import React from 'react';
import {get, isUndefined, reverse} from 'lodash';

import {dispatchChartUpdate, dispatchChartAdd, getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import {NewTracePanelBtn} from './NewTracePanel.jsx';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';

const fieldProps = {labelWidth: 50, size: 25};

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

export class BasicOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace} = getChartData(chartId);
        return {activeTrace};
    }

    render() {
        const {chartId} = this.props;
        const {activeTrace=0} = this.state;
        const {tablesources, data, layout} = getChartData(chartId);
        const groupKey = `${chartId}-basic-${activeTrace}`;
        const tablesource = get(tablesources, [activeTrace]);
        const tbl_id = get(tablesource, 'tbl_id');
        return (
            <div style={{minWidth: 250, padding:'0 5px 7px'}}>
                <OptionTopBar {...{groupKey, activeTrace, chartId, tbl_id}}/>
                <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey}
                            reducerFunc={basicFieldReducer({data, layout, activeTrace, tablesources})}>
                    <BasicOptionFields {...{layout, data, activeTrace}}/>
                </FieldGroup>
            </div>
        );
    }
}

export function basicFieldReducer({data, layout, activeTrace, tablesources}) {
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
        ['_xoptions']: {
            fieldKey: '_xoptions',
            value: getOptions('x', layout),
            tooltip: 'X axis options',
            label : 'X Options:',
            ...fieldProps
        },
        ['layout.yaxis.title']: {
            fieldKey: 'layout.yaxis.title',
            value: get(layout, 'yaxis.title'),
            tooltip: 'Y axis label',
            label : 'Y Label:',
            ...fieldProps
        },
        ['_yoptions']: {
            fieldKey: '_yoptions',
            value: getOptions('y', layout),
            tooltip: 'Y axis options',
            label : 'Y Options:',
            ...fieldProps
        },

    };

    return (inFields, action) => {
        if (!inFields) {
            return fields;
        } else {
            return inFields;
        }
    };
}


export function BasicOptionFields({activeTrace, align='vertical', xNoLog}) {
    // TODO: need color input field
    return (
        <div className={`FieldGroup__${align}`} style={{padding: 5, border: '2px solid #a5a5a5', borderRadius: 10}}>
            <ValidationField fieldKey={`data.${activeTrace}.name`}/>
            <ValidationField fieldKey={`data.${activeTrace}.marker.color`}/>

            {/* checkboxgroup is not working right when there's only 1 .. will add in later
             <CheckboxGroupInputField fieldKey={'layout.showlegend'}/>
             */}

            <br/>
            <ValidationField fieldKey={'layout.xaxis.title'}/>
            <CheckboxGroupInputField fieldKey='_xoptions'
                                     options={xNoLog ? X_AXIS_OPTIONS_NOLOG : X_AXIS_OPTIONS}/>
            <br/>
            <ValidationField fieldKey={'layout.yaxis.title'}/>
            <CheckboxGroupInputField fieldKey='_yoptions' options={Y_AXIS_OPTIONS}/>
            <br/>
            <ValidationField fieldKey={'layout.title'}/>

        </div>
    );
}

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
    const {layout={}} = getChartData(chartId);
    const changes = {showOptions: false};
    Object.entries(fields).forEach( ([k,v]) => {
        if (tbl_id && k.startsWith('_tables.')) {
            k = k.replace('_tables.', '');
            v = v ? `tables::${tbl_id},${v}` : undefined;
        } else if (k.startsWith('_')) {
            // handling _xoptions and _yoptions
            ['x','y'].forEach((a) => {
                if (k === `_${a}options`) {
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
                    changes[`layout.${a}axis.type`]  = opts.includes('log') ? 'log' : 'linear';
                }
            });
        }
        if (!changes[k]) {
            changes[k] = v;
        }

    });
    dispatchChartUpdate({chartId, changes});
}

function resetChart(chartId) {
    const {_original} = getChartData(chartId);
    _original && dispatchChartAdd(_original);
}
