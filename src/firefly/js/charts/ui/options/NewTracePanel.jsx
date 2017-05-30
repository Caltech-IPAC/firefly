import React from 'react';
import {get} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../../core/ComponentCntlr.js';
import {PopupPanel} from '../../../ui/PopupPanel.jsx';
import {updateSet} from '../../../util/WebUtil.js';
import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {TableSourcesOptions, submitChangesScatter} from './ScatterOptions.jsx';
import {submitChanges} from './BasicOptions.jsx';

const fieldProps = {labelWidth: 62, size: 15};

export class NewTracePanel extends SimpleComponent {

    getNextState(np) {
        const {tbl_id, chartId} = this.props;
        const {data} = getChartData(chartId);
        const activeTrace = data.length;        //setting activeTrace to next available index.
        const groupKey = `${chartId}-new-trace-${activeTrace}`;
        const type = getFieldVal(groupKey, `data.${activeTrace-1}.type`);
        return {groupKey, activeTrace, type};
    }

    render() {
        const {tbl_id, chartId} = this.props;
        const {groupKey, activeTrace, type} = this.state;
        const {data, layout, tablesources} = getChartData(chartId);
        const doAdd = (fields) => {
            const traceType = get(fields, `data.${activeTrace}.type`);
            const submitChangesFunc =  (traceType === 'scatter') ? submitChangesScatter : submitChanges;

            fields = Object.assign({activeTrace}, fields);  // make the newly added trace active
            submitChangesFunc({chartId, activeTrace, fields, tbl_id});
            dispatchHideDialog('ScatterNewTracePanel');
        };

        const ScatterOpt = () => (
            <div className='FieldGroup__vertical'>
                <ListBoxInputField fieldKey={`data.${activeTrace}.mode`} options={[{value:'markers'}, {value:'lines'}, {value:'lines+markers'}]}/>
                <TableSourcesOptions tablesource={{tbl_id}} activeTrace={activeTrace} groupKey={groupKey}/>
            </div>
        );
        const HistogramOpt = () => (
            <div className='FieldGroup__vertical'>
                <ValidationField fieldKey={`_tables.data.${activeTrace}.x`}/>
                <ValidationField fieldKey={`_tables.data.${activeTrace}.y`}/>
            </div>
        );
        const addtlParams = (type === 'histogram') ? HistogramOpt() : ScatterOpt();

        return (
            <div style={{padding: 10}}>
                <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={fieldReducer({data, layout, activeTrace, tablesources})}>
                    <ListBoxInputField fieldKey={`data.${activeTrace}.type`} options={[{value:'scatter'}, {value:'histogram'}]}/>
                    <ValidationField fieldKey={`data.${activeTrace}.name`}/>
                    <ValidationField fieldKey={`data.${activeTrace}.marker.color`}/>
                    {addtlParams}
                </FieldGroup>
                <div style={{display: 'inline-flex', marginTop: 10, justifyContent: 'space-between'}}>
                    <CompleteButton groupKey={groupKey}
                                    onSuccess={doAdd}
                                    onFail={() => alert('to be implemented')}
                                    text='ADD'
                    />
                    <button type='button' className='button std'
                            onClick={() => dispatchHideDialog('ScatterNewTracePanel')}>Cancel
                    </button>
                </div>
            </div>
        );
    }
}

export function NewTracePanelBtn({tbl_id, chartId}) {
    function showNewTracePanel() {
        const content= (
            <PopupPanel title={'Add a new series to existing chart'} >
                <NewTracePanel {...{tbl_id, chartId}}/>
            </PopupPanel>
        );
        DialogRootContainer.defineDialog('ScatterNewTracePanel', content);
        dispatchShowDialog('ScatterNewTracePanel');
    }
    return (
        <button type='button' className='button std' onClick={showNewTracePanel}>Add Series</button>
    );
}

function fieldReducer({data, layout, activeTrace, tablesources={}}) {
    const tablesourceMappings = get(tablesources[activeTrace], 'mappings');
    let color = get(data, `${activeTrace}.marker.color`, '');
    color = Array.isArray(color) ? '' : color;
    const fields = {
        [`data.${activeTrace}.type`]: {
            fieldKey: `data.${activeTrace}.type`,
            value: get(data, `${activeTrace}.type`),
            tooltip: 'Select plot type',
            label: 'Plot Type:',
            ...fieldProps,
        },
        [`data.${activeTrace}.name`]: {
            fieldKey: `data.${activeTrace}.name`,
            value: get(data, `${activeTrace}.name`, 'trace ' + activeTrace),
            tooltip: 'The name of this new series',
            label : 'Series name:',
            ...fieldProps,
        },
        [`data.${activeTrace}.marker.color`]: {
            fieldKey: `data.${activeTrace}.marker.color`,
            value: color,
            tooltip: 'Set series color',
            label : 'Color:',
            ...fieldProps,
        },
        [`data.${activeTrace}.mode`]: {
            fieldKey: `data.${activeTrace}.mode`,
            value: get(data, `${activeTrace}.mode`),
            tooltip: 'Select plot style',
            label: 'Plot Style:',
            ...fieldProps,
        },
        [`_tables.data.${activeTrace}.x`]: {
             fieldKey: `_tables.data.${activeTrace}.x`,
             value: get(tablesourceMappings, 'x', ''),
             //tooltip: 'X axis',
             label: 'X:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.y`]: {
             fieldKey: `_tables.data.${activeTrace}.y`,
             value: get(tablesourceMappings, 'y', ''),
             //tooltip: 'Y axis',
             label: 'Y:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.error_x.array`]: {
             fieldKey: `_tables.data.${activeTrace}.error_x.array`,
             value: get(tablesourceMappings, ['error_x.array'], ''),
             //tooltip: 'X error',
             label: 'X error\u2191:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.error_x.arrayminus`]: {
             fieldKey: `_tables.data.${activeTrace}.error_x.arrayminus`,
             value: get(tablesourceMappings, ['error_x.arrayminus'], ''),
             //tooltip: 'X error',
             label: 'X Err\u2193:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.error_y.array`]: {
             fieldKey: `_tables.data.${activeTrace}.error_y.array`,
             value: get(tablesourceMappings, ['error_y.array'], ''),
             //tooltip: '',
             label: 'Y error\u2191:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.error_y.arrayminus`]: {
             fieldKey: `_tables.data.${activeTrace}.error_y.arrayminus`,
             value: get(tablesourceMappings, ['error_y.arrayminus'], ''),
             //tooltip: 'Y error',
             label: 'Y error\u2193:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.marker.color`]: {
             fieldKey: `_tables.data.${activeTrace}.marker.color`,
             value: get(tablesourceMappings, 'marker.color', ''),
             //tooltip: 'Use a column for color map',
             label: 'Color Map:',
             ...fieldProps
         },
         [`_tables.data.${activeTrace}.marker.size`]: {
             fieldKey: `_tables.data.${activeTrace}.marker.size`,
             value: get(tablesourceMappings, 'marker.size', ''),
             //tooltip: 'Use a column for size map',
             label: 'Size Map:',
             ...fieldProps
         }
    };
    return (inFields, action) => {
        if (!inFields) {
            return fields;
        }

        const {payload:{fieldKey='', value=''}, type} = action;

        if (fieldKey.endsWith('marker.color') && type === VALUE_CHANGE && value.length === 1) {
            if (fieldKey.startsWith('_tables')) {
                const colorKey = Object.keys(inFields).find((k) => k.match(/data.+.marker.color/)) || '';
                if (colorKey) inFields = updateSet(inFields, [colorKey, 'value'], '');     // blanks out color when a color map is entered
            } else {
                const colorMapKey = Object.keys(inFields).find((k) => k.match(/_tables.+.marker.color/)) || '';
                if (colorMapKey) inFields = updateSet(inFields, [colorMapKey, 'value'], '');   // blanks out color map when a color is entered
            }
        }
        return inFields;

    };
}

