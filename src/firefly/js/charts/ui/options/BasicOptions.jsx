import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {isArray, get, isUndefined, isEmpty, reverse, set} from 'lodash';

import {dispatchChartUpdate, dispatchChartAdd, getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import FieldGroupUtils, {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange, VALUE_CHANGE, MULTI_VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {FieldGroupCollapsible} from '../../../ui/panel/CollapsiblePanel.jsx';

import {showColorPickerDialog} from '../../../ui/ColorPicker.jsx';
import Validate from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';

import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {updateSet} from '../../../util/WebUtil.js';
import {hideColSelectPopup} from '../ColSelectView.jsx';
import {addColorbarChanges} from '../../dataTypes/FireflyHeatmap.js';
import {getColumnType, getTblById} from '../../../tables/TableUtil.js';
import {getColValStats} from '../../TableStatsCntlr.js';
import {getColValidator} from '../ColumnOrExpression.jsx';
import {uniqueChartId, TRACE_COLORS, toRGBA, colorsOnTypes} from '../../ChartUtil.js';
import {colorscaleNameToVal} from '../../Colorscale.js';

import MAGNIFYING_GLASS from 'html/images/icons-2014/magnifyingGlass.png';
import {ToolbarButton} from '../../../ui/ToolbarButton.jsx';

const fieldProps = {labelWidth: 50, size: 25};
const boundariesFieldProps = {labelWidth: 35, size: 10};
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

/**
 * @param type - Plotly chart type
 */
export function hasMarkerColor(type) {
    return type.startsWith('scatter') || ['histogram', 'box', 'bar', 'plotcloud'].includes(type);
}

/*
 * check if the trace is not 3d-like chart or pie and has x and y defined
*/
function hasNoXY(type, tablesource) {
    if (type.endsWith('3d') || ['pie', 'surface', 'bar', 'area'].includes(type)) return true;

    return (!get(tablesource, ['mappings', 'x']) || !get(tablesource, ['mappings', 'y']));
}


function isNonNumColumn(tbl_id, colExp) {
    const numTypes = ['double', 'd', 'long', 'l', 'int', 'i', 'float', 'f'];
    const colType = getColumnType(getTblById(tbl_id), colExp);

    if (colType) {
        return !numTypes.includes(colType);
    } else {
        const colValidator = getColValidator(getColValStats(tbl_id));
        const {valid} = colValidator(colExp);

        return !valid;
    }
}

export class BasicOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace=0} = getChartData(chartId);
        return {activeTrace};
    }

    render() {
        const {chartId, tbl_Id:tblIdProp, showMultiTrace} = this.props;
        const {activeTrace=0} = this.state;
        const {tablesources, data={}} = getChartData(chartId);
        const groupKey = `${chartId}-basic-${activeTrace}`;
        const tablesource = get(tablesources, [activeTrace], tblIdProp ? {tbl_id: tblIdProp} : undefined);
        const tbl_id = get(tablesource, 'tbl_id');
        const type = get(data, `${activeTrace}.type`, 'scatter');
        const noColor = !hasMarkerColor(type);
        const noXY = hasNoXY(type, tablesource);
        const isXNotNumeric = noXY ? undefined : isNonNumColumn(tbl_id, get(tablesource, ['mappings', 'x'], ''));
        const isYNotNumeric = noXY ? undefined : isNonNumColumn(tbl_id, get(tablesource, ['mappings', 'y'], ''));
        const xNoLog = type.includes('histogram') ? true : undefined;          // histogram2d or histogram2dcontour
        const yNoLog = type.includes('histogram') ? true : undefined;

        //<OptionTopBar {...{groupKey, activeTrace, chartId, tbl_id}}/>
        return (
            <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey}
                        reducerFunc={basicFieldReducer({chartId, activeTrace})}>
                <BasicOptionFields {...{activeTrace, groupKey, noColor, noXY, isXNotNumeric, isYNotNumeric, xNoLog, yNoLog,showMultiTrace}}/>
            </FieldGroup>
        );
    }
}
//TODO on Monday, check if showMultiTrace is needed, it works for all cases both chart and xyPlot
export function basicFieldReducer({chartId, activeTrace, showMultiTrace=true}) {

    const getFields = () => {
        const {data, layout, fireflyLayout = {}} = getChartData(chartId);
        let color = get(data, `${activeTrace}.marker.color`, '');
        color = Array.isArray(color) ? '' : color;

        const commonfields = {
            ['layout.showlegend']: {
                fieldKey: 'layout.showlegend',
                value: get(layout, 'showlegend', ''),
                tooltip: 'Show legend',
                label: 'Legend:',
                ...fieldProps
            },
            ['layout.xaxis.title']: {
                fieldKey: 'layout.xaxis.title',
                value: get(layout, 'xaxis.title'),
                tooltip: 'X axis label',
                label: 'X Label:',
                ...fieldProps
            },
            ['__xreset']: {  // invisible helper field
                fieldKey: '__xreset',
                value: ''
            },
            ['__xoptions']: {
                fieldKey: '__xoptions',
                value: getOptions('x', layout),
                tooltip: 'X axis options',
                label: 'Options:',
                ...fieldProps
            },
            ['layout.yaxis.title']: {
                fieldKey: 'layout.yaxis.title',
                value: get(layout, 'yaxis.title'),
                tooltip: 'Y axis label',
                label: 'Y Label:',
                ...fieldProps
            },
            ['__yreset']: { // invisible helper field
                fieldKey: '__yreset',
                value: ''
            },
            ['__yoptions']: {
                fieldKey: '__yoptions',
                value: getOptions('y', layout),
                tooltip: 'Y axis options',
                label: 'Options:',
                ...fieldProps
            },
            ['fireflyLayout.xaxis.min']: {
                fieldKey: 'fireflyLayout.xaxis.min',
                value: get(fireflyLayout, 'xaxis.min'),
                validator: (val) => Validate.isFloat('X Min', val),
                tooltip: 'Minimum X value',
                label: 'X Min:',
                ...boundariesFieldProps
            },
            ['fireflyLayout.xaxis.max']: {
                fieldKey: 'fireflyLayout.xaxis.max',
                value: get(fireflyLayout, 'xaxis.max'),
                validator: (val) => Validate.isFloat('X Max', val),
                tooltip: 'Maximum X value',
                label: 'X Max:',
                ...boundariesFieldProps
            },
            ['fireflyLayout.yaxis.min']: {
                fieldKey: 'fireflyLayout.yaxis.min',
                value: get(fireflyLayout, 'yaxis.min'),
                validator: (val) => Validate.isFloat('Y Min', val),
                tooltip: 'Minimum Y value',
                label: 'Y Min:',
                ...boundariesFieldProps
            },
            ['fireflyLayout.yaxis.max']: {
                fieldKey: 'fireflyLayout.yaxis.max',
                value: get(fireflyLayout, 'yaxis.max'),
                validator: (val) => Validate.isFloat('Y Max', val),
                tooltip: 'Maximum Y value',
                label: 'Y Max:',
                ...boundariesFieldProps
            },
            [xyratioFldName]: {
                fieldKey: xyratioFldName,
                value: get(fireflyLayout, 'xyratio'),
                validator: Validate.floatRange.bind(null, 0.1, 10, 1, 'X/Y ratio'),
                tooltip: 'X/Y ratio',
                label: 'X/Y ratio:',
                labelWidth: 50
            },
            ['fireflyLayout.stretch']: {
                fieldKey: 'fireflyLayout.stretch',
                value: get(fireflyLayout, 'stretch', 'fit'),
                tooltip: 'Should the plot fit into the available space or fill the available width?',
                label: 'Stretch to:',
                labelWidth: 50
            }
        };

        const extraFields = showMultiTrace ? {
            [`data.${activeTrace}.name`]: {
                fieldKey: `data.${activeTrace}.name`,
                value: get(data, `${activeTrace}.name`, ''),
                tooltip: 'The name of this new series',
                label: 'Name:',
                ...fieldProps
            },
            [`data.${activeTrace}.marker.color`]: {
                fieldKey: `data.${activeTrace}.marker.color`,
                value: color,
                tooltip: 'Set series color',
                label: 'Color:',
                ...fieldProps
            },
            ['layout.title']: {
                fieldKey: 'layout.title',
                value: get(layout, 'title'),
                tooltip: 'Plot title',
                label: 'Plot title:',
                ...fieldProps
            },
        } : {};
        return Object.assign({}, commonfields, extraFields);
    };
    return (inFields, action) => {
        if (!inFields) {
            return getFields();
        } else {
            let fieldKey = undefined;
            if (action.type === VALUE_CHANGE) {
                // when field changes, clear the label and unit
                fieldKey = get(action.payload, 'fieldKey');
                ['x','y'].forEach((a) => {
                    if (fieldKey === `_tables.data.${activeTrace}.${a}`) {
                        inFields = updateSet(inFields, [`layout.${a}axis.title`, 'value'], undefined);
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


export class BasicOptionFields extends Component {

    constructor(props) {
        super(props);
        this.state = {
            displayStretchOptions : Boolean(getFieldVal(props.groupKey, xyratioFldName))
        };
        this.setStretchOptionsVisibility = this.setStretchOptionsVisibility.bind(this);
    }

    setStretchOptionsVisibility(displayStretchOptions) {
        if (this.iAmMounted && displayStretchOptions  !== this.state.displayStretchOptions) {
            this.setState({displayStretchOptions});
        }
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.unbinder) this.unbinder();
        hideColSelectPopup();
    }

    componentDidMount() {
        this.unbinder = FieldGroupUtils.bindToStore(this.props.groupKey,
            (fields) => {
                this.setStretchOptionsVisibility(Boolean(get(fields, [xyratioFldName, 'value']) && get(fields, [xyratioFldName, 'valid'])));
            });
        this.iAmMounted= true;
    }

    render() {
        const {activeTrace, groupKey, align='vertical', noColor, noXY, xNoLog, yNoLog, isXNotNumeric, isYNotNumeric, showMultiTrace} = this.props;

        // TODO: need color input field
        const colorFldPath = `data.${activeTrace}.marker.color`;

        // do not show XY ratio and axes ranges for fireflyHistogram
        const showAll = showMultiTrace || !xNoLog;

        return (
            <FieldGroupCollapsible  header='Layout Options'
                                    initialState= {{ value:'closed' }}
                                    fieldKey='layoutOptions'>
                <div className={`FieldGroup__${align}`}>
                    {showMultiTrace && <ValidationField fieldKey={`data.${activeTrace}.name`}/>}
                    {showMultiTrace && !noColor && <div style={{whiteSpace: 'nowrap'}}>
                        <ValidationField inline={true} fieldKey={colorFldPath}/>
                        <div
                            style={{display: 'inline-block', paddingLeft: 2, verticalAlign: 'top'}}
                            title='Select trace color'
                            onClick={() => showColorPickerDialog(getFieldVal(groupKey, colorFldPath), true, false,
                                (ev) => {
                                    const {r, g, b, a} = ev.rgb;
                                    const rgbStr = `rgba(${r},${g},${b},${a})`;
                                    dispatchValueChange({fieldKey: colorFldPath, groupKey, value: rgbStr, valid: true});
                                }, groupKey)}>
                            <ToolbarButton icon={MAGNIFYING_GLASS}/>
                        </div>
                    </div>
                    }

                    {showMultiTrace &&  <div> <br/><ValidationField fieldKey={'layout.title'}/><br/></div>}


                    {/* checkboxgroup is not working right when there's only 1 .. will add in later
                     <CheckboxGroupInputField fieldKey={'layout.showlegend'}/>
                     */}

                    {!noXY && <div>

                        <ValidationField fieldKey={'layout.xaxis.title'}/>
                        <CheckboxGroupInputField fieldKey='__xoptions'
                                                 options={xNoLog || isXNotNumeric ? X_AXIS_OPTIONS_NOLOG : X_AXIS_OPTIONS}/>
                        <br/>
                        <ValidationField fieldKey={'layout.yaxis.title'}/>
                        <CheckboxGroupInputField fieldKey='__yoptions'
                                                 options={yNoLog || isYNotNumeric ? Y_AXIS_OPTIONS_NOLOG : Y_AXIS_OPTIONS}/>
                        {showAll &&<div>
                            <br/>

                            <div style={helpStyle}>
                                Set plot boundaries if different from data range.
                            </div>
                            {isXNotNumeric ? false :
                                <div style={{display: 'flex', flexDirection: 'row', padding: '5px 15px 0'}}>
                                    <div style={{paddingRight: 5}}>
                                        <ValidationField fieldKey={'fireflyLayout.xaxis.min'}/>
                                    </div>
                                    <div style={{paddingRight: 5}}>
                                        <ValidationField fieldKey={'fireflyLayout.xaxis.max'}/>
                                    </div>
                                </div>}
                            {isYNotNumeric ? false :
                                <div style={{display: 'flex', flexDirection: 'row', padding: '0px 15px 15px'}}>
                                    <div style={{paddingRight: 5}}>
                                        <ValidationField fieldKey={'fireflyLayout.yaxis.min'}/>
                                    </div>
                                    <div style={{paddingRight: 5}}>
                                        <ValidationField fieldKey={'fireflyLayout.yaxis.max'}/>
                                    </div>
                                </div>
                            }
                        </div>}
                    </div>}
                    {showAll && <div>
                        <div style={helpStyle}>
                            Enter display aspect ratio below.<br/>
                            Leave it blank to use all available space.<br/>
                        </div>
                        <div style={{display: 'flex', flexDirection: 'row', padding: '5px 5px 5px 0'}}>
                            <div style={{paddingRight: 5}}>
                                <ValidationField style={{width:15}} fieldKey={xyratioFldName}/>
                            </div>
                            {this.state.displayStretchOptions && <div style={{paddingRight: 5}}>
                                <RadioGroupInputField fieldKey={'fireflyLayout.stretch'}
                                                      alignment='horizontal'
                                                      options={[
                                                          {label: 'height', value: 'fit'},
                                                          {label: 'width', value: 'fill'}
                                                      ]}/>
                            </div>}
                        </div>
                    </div>}

                    <div style={{overflow: 'hidden', height: 0, width: 0}}>
                        <ValidationField fieldKey='__xreset'/>
                        <ValidationField fieldKey='__yreset'/>
                    </div>
                </div>
            </FieldGroupCollapsible>
        );
    }
}

BasicOptionFields.propTypes = {
    chartId: PropTypes.string,
    groupKey: PropTypes.string.isRequired,
    activeTrace: PropTypes.number.isRequired,
    align: PropTypes.oneOf(['vertical', 'horizontal']),
    noColor: PropTypes.bool,
    xNoLog: PropTypes.bool,
    yNoLog: PropTypes.bool,
    noXY: PropTypes.bool,
    isXNotNumeric: PropTypes.bool,
    isYNotNumeric: PropTypes.bool,
    showMultiTrace: PropTypes.bool
};


/**
 * This is a default implementation of an option pane's apply changes function.
 * It assume the fieldId is the 'path' to the chart data and the value of the field is the value you want to change.
 * For fields that are mapped to tables, it assumes that they starts with '_tables'.  In this case, it will prepend
 * 'tables::' to the value.
 * @param {pbject} p
 * @param {string} p.chartId
 * @param {object} p.fields
 * @param {string} p.tbl_id
 */
export function submitChanges({chartId, fields, tbl_id}) {
    if (!fields) return;                // fields failed validations..  quick/dirty.. may need to separate the logic later.
    if (!chartId) chartId = uniqueChartId();
    const {layout={}, data=[], fireflyData, activeTrace:traceNum=0} = getChartData(chartId, {});
    const changes = {showOptions: false};
    Object.entries(fields).forEach( ([k,v]) => {
        if (tbl_id && k.startsWith('_tables.')) {
            const [,activeTrace] = /^_tables.data.(\d)/.exec(k) || [];
            if (!isUndefined(activeTrace)) set(changes, [`data.${activeTrace}.tbl_id`], tbl_id);
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
                            changes[`layout.${a}axis.range`] = (range[0]<range[1]) ? reverse(range) : range;
                            if (range[0]<range[1]) { changes[`layout.${a}axis.autorange`] = false; }
                        } else {
                            changes[`layout.${a}axis.autorange`] = 'reversed';
                            changes[`layout.${a}axis.range`] = undefined;
                        }
                    } else {
                        if (range) {
                            changes[`layout.${a}axis.range`] = (range[1]<range[0]) ? reverse(range) : range;
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
                if (!fields[`_tables.data.${traceNum}.marker.color`]) {
                    changes[k] = toRGBA(TRACE_COLORS[traceNum]);
                }
            } else {
                const d = data[traceNum];
                if (d) {
                    const type = get(d, 'type');
                    const prevMarkerColor = get(d, 'marker.color');
                    if (prevMarkerColor && prevMarkerColor !== v && type && colorsOnTypes[type]) {
                        // when changing color, change all color attributes
                        colorsOnTypes[type][0].filter((att) => att.endsWith('color')).
                        forEach((att) => changes[`data.${traceNum}.${att}`] = v);
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
        if (!k.startsWith('__') && !changes[k]) {
            changes[k] = v;
        }

    });
    adjustAxesRange(layout, changes);

    if (isEmpty(getChartData(chartId))) {
        // chart dropdown scenario
        // create chart data from changes and add chart
        const newChartData = {chartId, groupId: tbl_id};
        Object.entries(changes).forEach(([k,v]) => set(newChartData, k, v));
        dispatchChartAdd({chartId, chartType: 'plot.ly', groupId: tbl_id, ...newChartData});
    } else {
        // update chart from options scenario
        dispatchChartUpdate({chartId, changes});
    }
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

