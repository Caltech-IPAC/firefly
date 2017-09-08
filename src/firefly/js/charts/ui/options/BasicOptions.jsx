import React, {Component} from 'react';
import PropTypes from 'prop-types';
import {get, isUndefined, reverse} from 'lodash';

import {dispatchChartUpdate, dispatchChartAdd, getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import FieldGroupUtils, {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange, VALUE_CHANGE, MULTI_VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {showColorPickerDialog} from '../../../ui/ColorPicker.jsx';
import Validate from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {RadioGroupInputField} from '../../../ui/RadioGroupInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import {NewTracePanelBtn} from './NewTracePanel.jsx';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {updateSet} from '../../../util/WebUtil.js';
import {hideColSelectPopup} from '../ColSelectView.jsx';
import {addColorbarChanges} from '../../dataTypes/FireflyHeatmap.js';
import {getColumnType, getTblById} from '../../../tables/TableUtil.js';

const fieldProps = {labelWidth: 50, size: 25};
const boundariesFieldProps = {labelWidth: 35, size: 10};
const helpStyle = {fontStyle: 'italic', color: '#808080', paddingBottom: 10};
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
    if (get(layout, `${a}axis.autorange`) === 'reversed' ||
        (get(layout, `${a}axis.range.1`) < get(layout, `${a}axis.range.0`))) {
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

export class BasicOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace} = getChartData(chartId);
        return {activeTrace};
    }

    render() {
        const {chartId} = this.props;
        const {activeTrace=0} = this.state;
        const {tablesources, data={}} = getChartData(chartId);
        const groupKey = `${chartId}-basic-${activeTrace}`;
        const tablesource = get(tablesources, [activeTrace]);
        const tbl_id = get(tablesource, 'tbl_id');
        const type = get(data, `${activeTrace}.type`, 'scatter');
        const noColor = !hasMarkerColor(type);
        const noXY = hasNoXY(type, tablesource);
        const xColType = noXY ? '' : getColumnType(getTblById(tbl_id), get(tablesource, ['mappings', 'x'], ''));
        const yColType = noXY ? '' : getColumnType(getTblById(tbl_id), get(tablesource, ['mappings', 'y'], ''));
        const xNoLog = type.includes('histogram') ? true : undefined;          // histogram2d or histogram2dcontour
        const yNoLog = type.includes('histogram') ? true : undefined;

        return (
            <div style={{minWidth: 250, padding:'0 5px 7px'}}>
                <OptionTopBar {...{groupKey, activeTrace, chartId, tbl_id}}/>
                <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey}
                            reducerFunc={basicFieldReducer({chartId, activeTrace})}>
                    <BasicOptionFields {...{activeTrace, groupKey, noColor, noXY, xColType, yColType, xNoLog, yNoLog}}/>
                </FieldGroup>
            </div>
        );
    }
}

export function basicFieldReducer({chartId, activeTrace}) {
    const {data, layout, fireflyLayout={}} = getChartData(chartId);
    let color = get(data, `${activeTrace}.marker.color`, '');
    color = Array.isArray(color) ? '' : color;
    const fields = {
        [`data.${activeTrace}.name`]: {
            fieldKey: `data.${activeTrace}.name`,
            value: get(data, `${activeTrace}.name`, 'trace ' + activeTrace),
            tooltip: 'The name of this new series',
            label : 'Name:',
            ...fieldProps
        },
        [`data.${activeTrace}.marker.color`]: {
            fieldKey: `data.${activeTrace}.marker.color`,
            value: color,
            tooltip: 'Set series color',
            label : 'Color:',
            ...fieldProps
        },
        ['layout.title']: {
            fieldKey: 'layout.title',
            value: get(layout, 'title'),
            tooltip: 'Plot title',
            label : 'Plot title:',
            ...fieldProps
        },
        ['layout.showlegend']: {
            fieldKey: 'layout.showlegend',
            value: get(layout, 'showlegend', ''),
            tooltip: 'Show legend',
            label : 'Legend:',
            ...fieldProps
        },
        ['layout.xaxis.title']: {
            fieldKey: 'layout.xaxis.title',
            value: get(layout, 'xaxis.title'),
            tooltip: 'X axis label',
            label : 'X Label:',
            ...fieldProps
        },
        ['__xoptions']: {
            fieldKey: '__xoptions',
            value: getOptions('x', layout),
            tooltip: 'X axis options',
            label : 'Options:',
            ...fieldProps
        },
        ['layout.yaxis.title']: {
            fieldKey: 'layout.yaxis.title',
            value: get(layout, 'yaxis.title'),
            tooltip: 'Y axis label',
            label : 'Y Label:',
            ...fieldProps
        },
        ['__yoptions']: {
            fieldKey: '__yoptions',
            value: getOptions('y', layout),
            tooltip: 'Y axis options',
            label : 'Options:',
            ...fieldProps
        },
        ['fireflyLayout.xaxis.min']: {
            fieldKey: 'fireflyLayout.xaxis.min',
            value: get(fireflyLayout, 'xaxis.min'),
            validator: (val)=>Validate.isFloat('X Min', val),
            tooltip: 'Minimum X value',
            label: 'X Min:',
            ...boundariesFieldProps
        },
        ['fireflyLayout.xaxis.max']: {
            fieldKey: 'fireflyLayout.xaxis.max',
            value: get(fireflyLayout, 'xaxis.max'),
            validator: (val)=>Validate.isFloat('X Max', val),
            tooltip: 'Maximum X value',
            label : 'X Max:',
            ...boundariesFieldProps
        },
        ['fireflyLayout.yaxis.min']: {
            fieldKey: 'fireflyLayout.yaxis.min',
            value: get(fireflyLayout, 'yaxis.min'),
            validator: (val)=>Validate.isFloat('Y Min', val),
            tooltip: 'Minimum Y value',
            label: 'Y Min:',
            ...boundariesFieldProps
        },
        ['fireflyLayout.yaxis.max']: {
            fieldKey: 'fireflyLayout.yaxis.max',
            value: get(fireflyLayout, 'yaxis.max'),
            validator: (val)=>Validate.isFloat('Y Max', val),
            tooltip: 'Maximum Y value',
            label : 'Y Max:',
            ...boundariesFieldProps
        },
        [xyratioFldName]: {
            fieldKey: xyratioFldName,
            value: get(fireflyLayout, 'xyratio'),
            validator: Validate.floatRange.bind(null, 0.1, 10, 1, 'X/Y ratio'),
            tooltip: 'X/Y ratio',
            label : 'X/Y ratio:',
            labelWidth: 50
        },
        ['fireflyLayout.stretch']: {
            fieldKey: 'fireflyLayout.stretch',
            value: get(fireflyLayout, 'stretch', 'fit'),
            tooltip: 'Should the plot fit into the available space or fill the available width?',
            label : 'Stretch to:',
            labelWidth: 50
        }
    };

    return (inFields, action) => {
        if (!inFields) {
            return fields;
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
        const {activeTrace, groupKey, align='vertical', noColor, noXY, xNoLog, yNoLog, xColType, yColType} = this.props;
        const strT = ['str', 'char', 's', 'c'];

        // TODO: need color input field
        const colorFldPath = `data.${activeTrace}.marker.color`;

        return (
            <div className={`FieldGroup__${align}`}
                 style={{padding: '15px 10px 0', border: '2px solid #a5a5a5', borderRadius: 10}}>
                <ValidationField fieldKey={`data.${activeTrace}.name`}/>
                {!noColor && <div style={{whiteSpace: 'nowrap'}}>
                    <ValidationField inline={true} fieldKey={colorFldPath}/>
                    <div
                        style={{display: 'inline-block', cursor:'pointer', paddingLeft: 3, verticalAlign: 'middle', fontSize: 'larger'}}
                        title='Select trace color'
                        onClick={() => showColorPickerDialog(getFieldVal(groupKey, colorFldPath), true, false,
                             (ev) => {
                                 const {r,g,b,a}= ev.rgb;
                                 const rgbStr= `rgba(${r},${g},${b},${a})`;
                                 dispatchValueChange({fieldKey: colorFldPath, groupKey, value: rgbStr, valid: true});
                             }, groupKey)}>
                        {'\ud83d\udd0e'}
                    </div>
                </div>}
                <br/>
                <ValidationField fieldKey={'layout.title'}/>
                <br/>
                {/* checkboxgroup is not working right when there's only 1 .. will add in later
                 <CheckboxGroupInputField fieldKey={'layout.showlegend'}/>
                 */}
                {!noXY && <div>
                    <ValidationField fieldKey={'layout.xaxis.title'}/>
                    <CheckboxGroupInputField fieldKey='__xoptions'
                                             options={xNoLog || strT.includes(xColType) ? X_AXIS_OPTIONS_NOLOG : X_AXIS_OPTIONS}/>
                    <br/>
                    <ValidationField fieldKey={'layout.yaxis.title'}/>
                    <CheckboxGroupInputField fieldKey='__yoptions'
                                             options={yNoLog || strT.includes(yColType) ? Y_AXIS_OPTIONS_NOLOG : Y_AXIS_OPTIONS}/>
                    <br/>
                    <div style={helpStyle}>
                        Set plot boundaries if different from data range.
                    </div>
                    {strT.includes(xColType) ? false :
                        <div style={{display: 'flex', flexDirection: 'row', padding: '5px 15px 0'}}>
                            <div style={{paddingRight: 5}}>
                                <ValidationField fieldKey={'fireflyLayout.xaxis.min'}/>
                            </div>
                            <div style={{paddingRight: 5}}>
                                <ValidationField fieldKey={'fireflyLayout.xaxis.max'}/>
                            </div>
                        </div>}
                    {strT.includes(yColType) ? false :
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
            </div>
        );
    }
}

BasicOptionFields.propTypes = {
    groupKey: PropTypes.string.isRequired,
    activeTrace: PropTypes.number.isRequired,
    align: PropTypes.oneOf(['vertical', 'horizontal']),
    noColor: PropTypes.bool,
    xNoLog: PropTypes.bool,
    yNoLog: PropTypes.bool,
    noXY: PropTypes.bool,
    xColType: PropTypes.string,
    yColType: PropTypes.string
};



export function OptionTopBar({groupKey, activeTrace, chartId, tbl_id, submitChangesFunc=submitChanges}) {
    return (
        <div style={{display: 'flex', flexDirection: 'row', padding: '5px 0 15px'}}>
            <CompleteButton style={{flexGrow: 0}}
                            groupKey={groupKey}
                            onSuccess={(fields) => submitChangesFunc({chartId, activeTrace, fields, tbl_id})}
                            onFail={() => alert('to be implemented')}
                            text = 'Apply'
            />
            <div style={{flexGrow: 1}}/>
            {tbl_id && <div style={{flexGrow: 0}}><NewTracePanelBtn {...{chartId, tbl_id}}/></div>}
            <div style={{flexGrow: 0}}>
                <button type='button' className='button std' onClick={() => resetChart(chartId)}>Reset</button>
            </div>
        </div>

    );
}

OptionTopBar.propTypes = {
    groupKey: PropTypes.string.isRequired,
    activeTrace: PropTypes.number.isRequired,
    chartId: PropTypes.string,
    tbl_id: PropTypes.string,
    submitChangesFunc: PropTypes.func
};

/**
 * This is a default implementation of an option pane's apply changes function.
 * It assume the fieldId is the 'path' to the chart data and the value of the field is the value you want to change.
 * For fields that are mapped to tables, it assumes that they starts with '_tables'.  In this case, it will prepend
 * 'tables::tbl_id,' to the value.
 * @param {pbject} p
 * @param {string} p.chartId
 * @param {object} p.fields
 * @param {string} p.tbl_id
 */
export function submitChanges({chartId, fields, tbl_id}) {
    if (!fields) return;                // fields failed validations..  quick/dirty.. may need to separate the logic later.
    const {layout={}, data} = getChartData(chartId);
    const changes = {showOptions: false};
    Object.entries(fields).forEach( ([k,v]) => {
        if (tbl_id && k.startsWith('_tables.')) {
            k = k.replace('_tables.', '');
            v = v ? `tables::${tbl_id},${v}` : undefined;
        } else if (k.startsWith('__')) {
            // handling __xoptions and __yoptions
            ['x','y'].forEach((a) => {
                if (k === `__${a}options`) {
                    const opts = v || '';
                    const range = get(layout, `${a}axis.range`);

                    if (opts.includes('flip')) {
                        if (range) {  
                            if (range[0]<range[1]) changes[`layout.${a}axis.range`] = reverse(range);
                        } else {
                            changes[`layout.${a}axis.autorange`] = 'reversed';
                        }
                    } else {
                        if (range) {
                            if (range[1]<range[0]) changes[`layout.${a}axis.range`] = reverse(range);
                        } else {
                            changes[`layout.${a}axis.autorange`] = true;
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
                }
            });
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
    dispatchChartUpdate({chartId, changes});
}

function adjustAxesRange(layout, changes) {
    ['x', 'y'].forEach((a) => {
        var minUser = parseFloat(get(changes, `fireflyLayout.${a}axis.min`));
        var maxUser = parseFloat(get(changes, `fireflyLayout.${a}axis.max`));
        if (!Number.isNaN(minUser) || !Number.isNaN(maxUser)) {
            if (Number.isNaN(minUser) || Number.isNaN(maxUser)) {
                // range values of a log axis are logs - convert them back
                const range = get(layout, `${a}axis.range`, []).map(get(layout, `${a}axis.type`) === 'log' ? (e)=>Math.pow(10, e) : (e)=>e);
                if (Number.isNaN(minUser)) {
                    minUser = Math.min(range[0], range[1]);
                } else if (Number.isNaN(maxUser)) {
                    maxUser = Math.max(range[0], range[1]);
                }
            }
            const range = changes[`layout.${a}axis.range`] || [];
            const reversed = (changes[`layout.${a}axis.autorange`] === 'reversed') || (range[1] < range[0]);
            changes[`layout.${a}axis.range`] = getRange(minUser, maxUser, changes[`layout.${a}axis.type`] === 'log', reversed);
            changes[`layout.${a}axis.autorange`] = false;
        } else {
            //changes[`layout.${a}axis.autorange`] = true;
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

function resetChart(chartId) {
    const {_original} = getChartData(chartId);
    _original && dispatchChartAdd(_original);
}
