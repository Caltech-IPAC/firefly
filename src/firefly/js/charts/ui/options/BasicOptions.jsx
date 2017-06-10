import React from 'react';
import {get} from 'lodash';

import {dispatchChartUpdate, dispatchChartAdd, getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import {NewTracePanelBtn} from './NewTracePanel.jsx';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import Validate from '../../../util/Validate.js';

const fieldProps = {labelWidth: 62, size: 15};

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
        ['layout.title']: {
            fieldKey: 'layout.title',
            value: get(layout, 'title'),
            tooltip: 'Plot title',
            label : 'Plot Title:',
            ...fieldProps
        },
        [`data.${activeTrace}.name`]: {
            fieldKey: `data.${activeTrace}.name`,
            value: get(data, `${activeTrace}.name`, 'trace ' + activeTrace),
            tooltip: 'The name of this new series',
            label : 'Series name:',
            ...fieldProps
        },
        [`data.${activeTrace}.marker.color`]: {
            fieldKey: `data.${activeTrace}.marker.color`,
            value: color,
            tooltip: 'Set series color',
            label : 'Color:',
            ...fieldProps
        },
        [`data.${activeTrace}.opacity`]: {
            fieldKey: `data.${activeTrace}.opacity`,
            value: get(data, `${activeTrace}.opacity`, ''),
            validator: Validate.floatRange.bind(null, 0.1, 1.0, 2,'opacity'),
            tooltip: 'Set trace opacity',
            label : 'Opacity:',
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
            tooltip: 'X axis title',
            label : 'Xaxis Title:',
            ...fieldProps
        },
        ['layout.yaxis.title']: {
            fieldKey: 'layout.yaxis.title',
            value: get(layout, 'yaxis.title'),
            tooltip: 'Y axis title',
            label : 'Yaxis Title:',
            ...fieldProps
        }
    };

    return (inFields, action) => {
        if (!inFields) {
            return fields;
        } else {
            return inFields;
        }
    };
}


export function BasicOptionFields({activeTrace, align='vertical'}) {
    // TODO: need color input field
    return (
        <div className={`FieldGroup__${align}`}>
            <ValidationField fieldKey={'layout.title'}/>
            <ValidationField fieldKey={`data.${activeTrace}.name`}/>
            <ValidationField fieldKey={`data.${activeTrace}.marker.color`}/>
            <ValidationField fieldKey={`data.${activeTrace}.opacity`}/>
{/* checkboxgroup is not working right when there's only 1 .. will add in later
            <CheckboxGroupInputField fieldKey={'layout.showlegend'}/>
*/}
            <ValidationField fieldKey={'layout.xaxis.title'}/>
            <ValidationField fieldKey={'layout.yaxis.title'}/>
            <br/>
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
    const changes = {showOptions: false};
    Object.entries(fields).forEach( ([k,v]) => {
        if (tbl_id && k.startsWith('_tables.')) {
            k = k.replace('_tables.', '');
            v = v ? `tables::${tbl_id},${v}` : undefined;
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
