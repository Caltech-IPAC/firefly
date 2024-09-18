import React, {useCallback, useEffect} from 'react';
import PropTypes from 'prop-types';
import {get, isArray, isEmpty, isString, isUndefined, memoize, reverse, set} from 'lodash';

import {dispatchChartAdd, dispatchChartUpdate, getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {getField, getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange, MULTI_VALUE_CHANGE, VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {
    CollapsibleGroup,
    CollapsibleItem
} from '../../../ui/panel/CollapsiblePanel.jsx';

import {showColorPickerDialog} from '../../../ui/ColorPicker.jsx';
import Validate from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {updateSet} from '../../../util/WebUtil.js';
import {hideColSelectPopup} from '../ColSelectView.jsx';
import {addColorbarChanges} from '../../dataTypes/FireflyHeatmap.js';
import {colorsOnTypes, getChartProps, toRGBA, TRACE_COLORS, uniqueChartId} from '../../ChartUtil.js';
import {colorscaleNameToVal} from '../../Colorscale.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../../visualize/MultiViewCntlr.js';

import MAGNIFYING_GLASS from 'html/images/icons-2014/magnifyingGlass.png';
import {ToolbarButton} from '../../../ui/ToolbarButton.jsx';
import {ActiveTraceSelect} from '../PlotlyToolbar.jsx';
import {FormControl, FormLabel, Stack, Typography} from '@mui/joy';

export const helpStyle = {fontStyle: 'italic', color: '#808080', paddingBottom: 10};
const xyratioFldName = 'fireflyLayout.xyratio';

const X_AXIS_OPTIONS = [
    {label: 'grid', value: 'grid'},
    {label: 'reverse', value: 'flip'},
    {label: 'top', value: 'opposite'},
    {label: 'log', value: 'log'}
];
const X_AXIS_OPTIONS_NOLOG = X_AXIS_OPTIONS.filter((el) => {return el.label !== 'log';});

const Y_AXIS_OPTIONS = [
    {label: 'grid', value: 'grid'},
    {label: 'reverse', value: 'flip'},
    {label: 'right', value: 'opposite'},
    {label: 'log', value: 'log'}
];
const Y_AXIS_OPTIONS_NOLOG = Y_AXIS_OPTIONS.filter((el) => {return el.label !== 'log';});

function getOptions(a, layout) {
    const opts = [];
    const showgrid = get(layout, `${a}axis.showgrid`);
    if ( (isUndefined(showgrid) && get(layout, `${a}axis.gridwidth`)) || showgrid) {
        opts.push('grid');
    }
    const range = get(layout, `${a}axis.range`) || [];
    const autorange = get(layout, `${a}axis.autorange`);
    const reversed = (autorange === 'reversed') || (range[1] < range[0]);
    if (reversed) {
        opts.push('flip');
    }
    if (get(layout, `${a}axis.side`) === (a==='x'?'top':'right')) {
        opts.push('opposite');
    }
    if (get(layout, `${a}axis.type`) === 'log') {
        opts.push('log');
    }
    return opts.toString();
}

function findViewerId(viewerId= DEFAULT_PLOT2D_VIEWER_ID, renderTreeId= undefined) {
    if (viewerId===DEFAULT_PLOT2D_VIEWER_ID && renderTreeId) return `${viewerId}_${renderTreeId}`;
    return viewerId;
}

export function BasicOptions({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {
    
    const activeTrace = useStoreConnector(() =>  pActiveTrace ?? getChartData(chartId)?.activeTrace ?? 0);
    useEffect(() => {
       return () => hideColSelectPopup();
    }, []);

    const {noColor, tbl_id, multiTrace} = getChartProps(chartId, ptbl_id, activeTrace);
    const {Name, Color} = useBasicOptions({activeTrace, tbl_id, chartId, groupKey});
    const reducerFunc = basicFieldReducer({chartId, activeTrace});
    reducerFunc.ver = chartId+activeTrace;

    return (
        <FieldGroup keepState={false} groupKey={groupKey}
                    reducerFunc={reducerFunc}>
            <CollapsibleGroup>
                {(multiTrace || !noColor) &&
                    //TODO: expose it as Trace Options and replace all usages of following
                    <CollapsibleItem header='Trace Options' componentKey='chart-trace-options'>
                        <Stack spacing={1} sx={{'.MuiFormLabel-root': {width: '6rem'}}}>
                            {multiTrace && <Name/>}
                            {!noColor && <Color/>}
                        </Stack>
                    </CollapsibleItem>
                }
                <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey}}/>
            </CollapsibleGroup>
        </FieldGroup>
    );
}

export function basicFieldReducer({chartId, activeTrace}) {

    return (inFields, action) => {
        if (!inFields) {
            return ;
            // return Object.fromEntries( Object.values(options).map((f) => f.field));
        } else {
            let fieldKey = undefined;
            if (action.type === VALUE_CHANGE) {
                // when field changes, clear the label and unit
                fieldKey = get(action.payload, 'fieldKey');
                ['x','y'].forEach((a) => {
                    if (fieldKey === `_tables.data.${activeTrace}.${a}`) {
                        inFields = updateSet(inFields, [`layout.${a}axis.title.text`, 'value'], undefined);
                        inFields = updateSet(inFields, [`fireflyLayout.${a}axis.min`, 'value'], undefined);
                        inFields = updateSet(inFields, [`fireflyLayout.${a}axis.max`, 'value'], undefined);
                        inFields = updateSet(inFields, [`__${a}reset`, 'value'], 'true');
                        const optFldName = `__${a}options`;
                        const currOptions = get(inFields, [optFldName, 'value']);
                        // do not reset grid selection
                        inFields = updateSet(inFields, [optFldName, 'value'], filterOptions(currOptions, ['grid', 'opposite']));
                    }
                });
            }
            if (action.type === MULTI_VALUE_CHANGE || action.type === VALUE_CHANGE) {
                // validate min/max/log relationship
                const fldsets = [
                    {min: 'fireflyLayout.xaxis.min', max: 'fireflyLayout.xaxis.max', options: '__xoptions'},
                    {min: 'fireflyLayout.yaxis.min', max: 'fireflyLayout.yaxis.max', options: '__yoptions'}
                ];
                fldsets.forEach(
                    (v) => {
                        if (!fieldKey || fieldKey === v.min || fieldKey === v.max || fieldKey === v.options) {
                            const valMin = Number.parseFloat(get(inFields, [v.min, 'value']));
                            const options = get(inFields, [v.options, 'value']);
                            const logVal = Boolean(options && options.includes('log'));
                            if (Number.isFinite(valMin)) {
                                if (logVal && valMin <= 0) {
                                    inFields = updateSet(inFields, [v.min, 'valid'], false);
                                    inFields = updateSet(inFields, [v.min, 'message'], 'The minimum of a log axis can not be 0 or less');
                                } else {
                                    const valMax = Number.parseFloat(get(inFields, [v.max, 'value']));
                                    if (Number.isFinite(valMax) && valMin > valMax) {
                                        inFields = updateSet(inFields, [fieldKey || v.min, 'valid'], false);
                                        inFields = updateSet(inFields, [fieldKey || v.min, 'message'], 'Min value greater than max');
                                    }
                                }
                            }
                        }
                    });
            }
            return inFields;
        }
    };
}


export function LayoutOptions({activeTrace, tbl_id, chartId, groupKey, noXY, xNoLog, noTitle,
                                  XaxisTitle, YaxisTitle, ...more}) {

    const {Title, XaxisTitle:pXaxisTitle, YaxisTitle:pYaxisTitle, Xoptions, Yoptions, Xyratio, Stretch, Xreset, Yreset} = useBasicOptions({activeTrace, tbl_id, chartId, groupKey, noXY, xNoLog, ...more});
    XaxisTitle = XaxisTitle || pXaxisTitle;
    YaxisTitle = YaxisTitle || pYaxisTitle;

    return (
        <CollapsibleItem  header='Chart Options' componentKey='chart-layout-options'
                                    {...more /* to allow AccordionGroup styling props inserted by joy UI, propagate to Accordion */}>
            <Stack spacing={2} sx={{'.MuiFormLabel-root': {width: '6rem'}}}>
                <Title/>
                {!noXY && <>
                    <Stack spacing={1}>
                        <XaxisTitle/>
                        <Xoptions/>
                    </Stack>
                    <Stack spacing={1}>
                        <YaxisTitle/>
                        <Yoptions/>
                    </Stack>
                    <PlotBoundaries {...{chartId, groupKey, activeTrace, tbl_id}}/>
                </>}
                <XyRatio {...{groupKey, Xyratio, Stretch, xNoLog}}/>
                <div style={
                    {display: 'none'} //hidden because following are just helper fields for tracking state
                }>
                    <Xreset/>
                    <Yreset/>
                </div>
            </Stack>
        </CollapsibleItem>
    );
}

LayoutOptions.propTypes = {
    activeTrace: PropTypes.number,
    tbl_id: PropTypes.string,
    chartId: PropTypes.string,
    groupKey: PropTypes.string.isRequired,
    noXY: PropTypes.bool,               // don't show XY fields.
    xNoLog: PropTypes.bool,             // no Log option for x axis
    XaxisTitle: PropTypes.func,         // x axis title override
    YaxisTitle: PropTypes.func          // y axis title override
};

export function PlotBoundaries({activeTrace:pActiveTrace, tbl_id, chartId, groupKey, isXNotNumeric, isYNotNumeric}) {
    const {activeTrace, ...rest} = getChartProps(chartId, tbl_id, pActiveTrace);
    isXNotNumeric = isXNotNumeric ?? rest.isXNotNumeric;
    isYNotNumeric = isYNotNumeric ?? rest.isYNotNumeric;
    const {XaxisMin, XaxisMax, YaxisMin, YaxisMax} = useBasicOptions({activeTrace, chartId, groupKey, isXNotNumeric, isYNotNumeric});
    return (
        <Stack spacing={.5} sx={{
            '.MuiFormLabel-root': {width: '3rem'},
            '.MuiInput-root': {width: '7rem'}
        }}>
            <Typography level='body-sm'>
                Set plot boundaries if different from data range.
            </Typography>
            {isXNotNumeric ? false :
                <Stack spacing={3} direction={'row'}>
                        <XaxisMin/>
                        <XaxisMax/>
                </Stack>}
            {isYNotNumeric ? false :
                <Stack spacing={3} direction={'row'}>
                        <YaxisMin/>
                        <YaxisMax/>
                </Stack>
            }
        </Stack>
    );
}

function XyRatio({groupKey, Xyratio, Stretch, xNoLog}) {

    const showStretch = useStoreConnector(() => {
        const {value, valid} = getField(groupKey, xyratioFldName) || {};
        return !!value && valid;
    });

    if (xNoLog) return null;
    return (
        <Stack spacing={.5}>
            <Typography level='body-sm'>
                Enter display aspect ratio below.<br/>
                Leave it blank to use all available space.
            </Typography>
            <Stack spacing={2} direction={'row'}>
                <Xyratio/>
                {showStretch && <Stretch/>}
            </Stack>
        </Stack>
    );
}


/**
 * This is a default implementation of an option pane's apply changes function.
 * It assume the fieldId is the 'path' to the chart data and the value of the field is the value you want to change.
 * For fields that are mapped to tables, it assumes that they starts with '_tables'.  In this case, it will prepend
 * 'tables::' to the value.
 * @param {object} p
 * @param {string} p.chartId
 * @param {object} p.fields
 * @param {string} p.tbl_id
 * @param {string} p.renderTreeId
 */
export function submitChanges({chartId, fields, tbl_id, renderTreeId}) {
    if (!fields) return;                // fields failed validations..  quick/dirty.. may need to separate the logic later.
    if (!chartId) chartId = uniqueChartId();

    const changes = evalChangesFromFields(chartId, tbl_id, fields);

    if (isEmpty(getChartData(chartId))) {
        // chart dropdown scenario
        // create chart data from changes and add chart
        const newChartData = {chartId, groupId: tbl_id};
        Object.entries(changes).forEach(([k,v]) => set(newChartData, k, v));
        dispatchChartAdd({chartId, chartType: 'plot.ly', groupId: tbl_id, renderTreeId,
            viewerId: findViewerId(DEFAULT_PLOT2D_VIEWER_ID,renderTreeId),
            ...newChartData});
    } else {
        // update chart from options scenario
        dispatchChartUpdate({chartId, changes});
    }
}

export function evalChangesFromFields(chartId, tbl_id, fields) {

    const {layout={}, data=[], fireflyData, activeTrace:traceNum=0} = getChartData(chartId, {});

    const changes = {showOptions: false};
    Object.entries(fields).forEach( ([k,v]) => {
        if (tbl_id && k.startsWith('_tables.')) {
            const [,activeTrace] = /^_tables.data.(\d)/.exec(k) || [];
            if (!isUndefined(activeTrace)) {
                // table id must be set for a data change
                changes[`data.${activeTrace}.tbl_id`] = data[activeTrace]?.tbl_id || tbl_id;
            }
            k = k.replace('_tables.', '');
            v = v ? `tables::${v}` : undefined;
        } else if (k.startsWith('__')) {
            // handling __xoptions and __yoptions
            ['x','y'].forEach((a) => {
                if (k === `__${a}options`) {
                    const opts = v || '';

                    // helper hidden fields __xreset and __yreset keep track of whether x and y have changed
                    // if this field is set, we'd like to clear range
                    const range = !fields[`__${a}reset`] && get(layout, `${a}axis.range`);

                    if (opts.includes('flip')) {
                        if (range) {
                            // reverse mutates the original array
                            changes[`layout.${a}axis.range`] = (range[0]<range[1]) ? reverse([range[0],range[1]]) : range;
                            if (range[0]<range[1]) { changes[`layout.${a}axis.autorange`] = false; }
                        } else {
                            changes[`layout.${a}axis.autorange`] = 'reversed';
                            changes[`layout.${a}axis.range`] = undefined;
                        }
                    } else {
                        if (range) {
                            // reverse mutates the original array
                            changes[`layout.${a}axis.range`] = (range[1]<range[0]) ? reverse([range[0],range[1]]) : range;
                            if (range[1]<range[0]) { changes[`layout.${a}axis.autorange`] = false; }
                        } else {
                            changes[`layout.${a}axis.autorange`] = true;
                            changes[`layout.${a}axis.range`] = undefined;
                        }
                    }
                    if (opts.includes('opposite')) {
                        changes[`layout.${a}axis.side`] = (a==='x'?'top':'right');
                    } else {
                        changes[`layout.${a}axis.side`] = (a==='x'?'bottom':'left');
                    }

                    changes[`layout.${a}axis.showgrid`] = opts.includes('grid');
                    if (opts.includes('log')) {
                        changes[`layout.${a}axis.type`] = 'log';
                    } else if (get(layout, `${a}axis.type`, '') === 'log') {
                        changes[`layout.${a}axis.type`] = 'linear';
                    }
                } else if (k === `__${a}reset` && v) {
                    changes[`layout.${a}axis.range`] = undefined;
                }
            });
        } else if (k===`data.${traceNum}.marker.color`) {
            if (!v) {
                // make sure color is set to default - otherwise active trace change changes the color of the trace
                // after using colormap option, the color might not be set
                v = fields[`_tables.data.${traceNum}.marker.color`] ? undefined : toRGBA(TRACE_COLORS[traceNum]);
            } else {
                if (fields[`_tables.data.${traceNum}.marker.color`]) {
                    // do not set color, if using color map
                    v = undefined;
                } else {
                    const d = data[traceNum];
                    if (d) {
                        const type = get(d, 'type');
                        const prevMarkerColor = get(d, 'marker.color');
                        if (prevMarkerColor && prevMarkerColor !== v && type && colorsOnTypes[type]) {
                            // when changing color, change all color attributes
                            colorsOnTypes[type][0].filter((att) => att.endsWith('color')).forEach((att) => changes[`data.${traceNum}.${att}`] = v);
                            // change annotation color
                            const annotations = get(fireflyData, `${traceNum}.annotations`);
                            if (isArray(annotations)) {
                                annotations.forEach((a, i) => {
                                    if (a) {
                                        changes[`fireflyData.${traceNum}.annotations.${i}.arrowcolor`] = v;
                                    }
                                });
                            }
                        }
                    }
                }
            }
        } else if (k.match(/^fireflyData.+colorscale$/)) {
            // colorscale name is saved in fireflyData
            // the corresponding colorscale value must be set in data
            const colorscale = colorscaleNameToVal(v);
            if (colorscale) {
                changes[k.replace(/^fireflyData/, 'data')] = colorscale;
            }
        }

        // move colorbar to the other side of the chart
        const yOpposite = get(fields, '__yoptions', '').includes('opposite');
        data.forEach( (d, i) => {
            if (get(d, 'colorbar') && get(d, 'showscale', true)) {
                addColorbarChanges(changes, yOpposite, i, get(d, 'colorbar.x'));
            }
        });

        // omit fields, that start with '__'
        if (!k.startsWith('__') && isUndefined(changes[k])) {
            if (layout.title || k!=='layout.title.text' || v) { // if no title set then don't set it in the changes
                changes[k] = v;
            }
        }

    });
    adjustAxesRange(layout, changes);

    return changes;
}

function adjustAxesRange(layout, changes) {
    ['x', 'y'].forEach((a) => {
        let minUser = parseFloat(get(changes, `fireflyLayout.${a}axis.min`));
        let maxUser = parseFloat(get(changes, `fireflyLayout.${a}axis.max`));

        if (!Number.isNaN(minUser) || !Number.isNaN(maxUser) || changes[`layout.${a}axis.type`]) {
            // range values of a log axis are logs - convert them back
            const range = changes[`layout.${a}axis.range`] &&
                (get(layout, `${a}axis.range`, []).map(get(layout, `${a}axis.type`) === 'log' ? (e)=>Math.pow(10, e) : (e)=>e));
            if (Number.isNaN(minUser) && range) {
                minUser = Math.min(range[0], range[1]);
            }
            if (Number.isNaN(maxUser) && range) {
                maxUser = Math.max(range[0], range[1]);
            }

            if (!Number.isNaN(minUser) || !Number.isNaN(maxUser)) {
                const autorange = changes[`layout.${a}axis.autorange`];
                const reversed = (autorange === 'reversed') || (!autorange && range[1] < range[0]);

                changes[`layout.${a}axis.range`] = getRange(minUser, maxUser, changes[`layout.${a}axis.type`] === 'log', reversed);
                changes[`layout.${a}axis.autorange`] = false;
            }
        }
    });
}


/**
 * Get range for a plotly axis
 * Plotly requires range to be reversed if the axis is reversed,
 * and limits to be log if axis scale is log
 * @param min - minimum value
 * @param max - maximum value
 * @param isLog - true, if an axis uses log scale
 * @param isReversed - true, if the axis should be reversed
 * @returns {Array<number>} an array for axis range property in plotly layout
 */
function getRange(min, max, isLog, isReversed) {
    const [r1, r2] = isReversed ? [max, min] : [min, max];
    return isLog ? [Math.log10(r1), Math.log10(r2)] : [r1, r2];
}

/**
 * Filter options string, so that it contains only the listed options
 * @param options - original options
 * @param opts - array of options that are OK to leave
 * @returns {*} - filtered options
 */
function filterOptions(options, opts) {
    if (!options) return undefined;

    return opts.filter((opt) => options.includes(opt) || options.includes('_all_')).toString();
}

/*
 * This function returns a collection of components using `useCallback`, ensuring they are not recreated between re-renders.
 * To modify this behavior, you can set the `deps` parameter accordingly.
 */
export const useBasicOptions = ({activeTrace:pActiveTrace, chartId, tbl_id, groupKey, isXNotNumeric,
                                  isYNotNumeric, xNoLog, yNoLog, orientation='horizontal'}, deps=[]) => {
    const {activeTrace, data, layout, fireflyLayout, color, ...rest} = getChartProps(chartId, tbl_id, pActiveTrace);
    xNoLog = xNoLog ?? rest.xNoLog;
    yNoLog = yNoLog ?? rest.yNoLog;
    isXNotNumeric = isXNotNumeric ?? rest.isXNotNumeric;
    isYNotNumeric = isYNotNumeric ?? rest.isYNotNumeric;
    const initTitle= isString(layout?.title?.text) ?
        layout.title.text :
        isString(layout?.title) ? layout.title : '';

    return {
        ShowLegend: useCallback((props) => (<CheckboxGroupInputField fieldKey={'layout.showlegend'}
                                         initialState= {{value: get(layout, 'showlegend', 'true')}}
                                         tooltip='Show legend'
                                         label='Legend:'
                                         options={[{label: 'Show legend:', value: 'true'}]}
                                         alignment={orientation} {...props}/>),deps),
        XaxisTitle: useCallback((props) => (<ValidationField fieldKey={'layout.xaxis.title.text'}
                                         initialState= {{value: get(layout, 'xaxis.title.text')}}
                                         tooltip='X axis label'
                                         label='X Label:'
                                         orientation={orientation} {...props}/>), deps),
        Xreset: useCallback((props) => (<ValidationField fieldKey='__xreset' initialState={{value:''}}
                                                orientation={orientation} {...props}/>), deps),
        Xoptions: useCallback((props) => (<CheckboxGroupInputField fieldKey='__xoptions'
                                 initialState= {{value: getOptions('x', layout)}}
                                 tooltip='X axis options'
                                 label='Options:'
                                 options={xNoLog || isXNotNumeric ? X_AXIS_OPTIONS_NOLOG : X_AXIS_OPTIONS}
                                 alignment={orientation} {...props}/>), deps),
        YaxisTitle: useCallback((props) => (<ValidationField fieldKey={'layout.yaxis.title.text'}
                                     initialState= {{value: get(layout, 'yaxis.title.text')}}
                                     tooltip='Y axis label'
                                     label='Y Label:'
                                     orientation={orientation} {...props}/>), deps),
        Yreset: useCallback((props) => (<ValidationField fieldKey='__yreset' initialState={{value:''}}
                                                orientation={orientation} {...props}/>), deps),
        Yoptions: useCallback((props) => (<CheckboxGroupInputField fieldKey='__yoptions'
                                    initialState= {{value: getOptions('y', layout)}}
                                    tooltip='Y axis options'
                                    label='Options:'
                                    options={yNoLog || isYNotNumeric ? Y_AXIS_OPTIONS_NOLOG : Y_AXIS_OPTIONS}
                                    alignment={orientation} {...props}/>), deps),
        XaxisMin: useCallback((props) => (<ValidationField fieldKey={'fireflyLayout.xaxis.min'}
                                    initialState= {{value: get(fireflyLayout, 'xaxis.min')}}
                                    validator={(val) => Validate.isFloat('X Min', val)}
                                    tooltip='Minimum X value'
                                    label='X Min:'
                                    orientation={orientation} {...props}/>), deps),
        XaxisMax: useCallback((props) => (<ValidationField fieldKey={'fireflyLayout.xaxis.max'}
                                    initialState= {{value: get(fireflyLayout, 'xaxis.max')}}
                                    validator={(val) => Validate.isFloat('X Max', val)}
                                    tooltip='Maximum X value'
                                    label='X Max:'
                                    orientation={orientation} {...props}/>), deps),
        YaxisMin:  useCallback((props) => (<ValidationField fieldKey={'fireflyLayout.yaxis.min'}
                                    initialState= {{value: get(fireflyLayout, 'yaxis.min')}}
                                    validator={(val) => Validate.isFloat('Y Min', val)}
                                    tooltip='Minimum Y value'
                                    label='Y Min:'
                                    orientation={orientation} {...props}/>), deps),
        YaxisMax:  useCallback((props) => (<ValidationField fieldKey={'fireflyLayout.yaxis.max'}
                                    initialState= {{value: get(fireflyLayout, 'yaxis.max')}}
                                    validator={(val) => Validate.isFloat('Y Max', val)}
                                    tooltip='Maximum Y value'
                                    label='Y Max:'
                                    orientation={orientation} {...props}/>), deps),
        Xyratio:  useCallback((props) => (<ValidationField style={{width: 15}} fieldKey={xyratioFldName}
                                     initialState= {{value: get(fireflyLayout, 'xyratio')}}
                                     validator={Validate.floatRange.bind(null, 0.1, 10, 1, 'X/Y ratio')}
                                     tooltip='X/Y ratio'
                                     label='X/Y ratio:'
                                     orientation={orientation} {...props}/>), deps),
        Stretch:  useCallback((props) => (<RadioGroupInputField
                                    fieldKey={'fireflyLayout.stretch'}
                                    alignment='horizontal'
                                    initialState= {{value: get(fireflyLayout, 'stretch', 'fit')}}
                                    tooltip='Should the plot fit into the available space or fill the available width?'
                                    label='Stretch to:'
                                    options={[ {label: 'height', value: 'fit'}, {label: 'width', value: 'fill'}]}
                                    orientation={orientation} {...props}/>), deps),
        Name:  useCallback((props) => (<ValidationField fieldKey={`data.${activeTrace}.name`}
                                     initialState= {{value: get(data, `${activeTrace}.name`, '')}}
                                     tooltip='The name of this new series'
                                     label='Name:'
                                     orientation={orientation} {...props}/>), deps),
        Color:  useCallback((props) => {
            const colorPicker = (
                <div style={{display: 'inline-block', paddingLeft: 2, verticalAlign: 'top'}}
                     title='Select trace color'
                     onClick={() => showColorPickerDialog(getFieldVal(groupKey, `data.${activeTrace}.marker.color`), true, false,
                         (ev) => {
                             if (ev) {
                                 const {r, g, b, a} = ev.rgb;
                                 const rgbStr = `rgba(${r},${g},${b},${a})`;
                                 dispatchValueChange({fieldKey: `data.${activeTrace}.marker.color`, groupKey, value: rgbStr, valid: true});
                             }
                         }, groupKey, 'plots.colorpicker', .5)}>
                    <ToolbarButton icon={MAGNIFYING_GLASS}/>
                </div>
            );
            return (
                <ValidationField fieldKey={`data.${activeTrace}.marker.color`}
                                 initialState={{value: color}}
                                 tooltip='Set series color'
                                 label='Color:'
                                 endDecorator={colorPicker}
                                 orientation={orientation} {...props}/>
            );}, deps),
        Title:  useCallback((props) => ( <ValidationField fieldKey={'layout.title.text'}
                                                 initialState= {{value: initTitle}}
                                                 tooltip='Chart title'
                                                 label='Chart title:'
                                                 orientation={orientation} {...props}/>), deps),
        ChooseTrace: useCallback(({sx, ...props}) => {
            if (!data || data.length < 2) return null;
            return (
                <FormControl sx={sx} orientation={orientation}>
                    <FormLabel>Choose Trace: </FormLabel>
                    <ActiveTraceSelect chartId={chartId} activeTrace={activeTrace} {...props}/>
                </FormControl>
            );
        }, deps),
    };
};
