import React from 'react';

import {getChartData} from '../../ChartsCntlr.js';
import {getNewTraceDefaults} from '../../ChartUtil.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import CompleteButton from '../../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../../core/ComponentCntlr.js';
import {PopupPanel} from '../../../ui/PopupPanel.jsx';
import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {ScatterOptions, submitChangesScatter} from './ScatterOptions.jsx';
import {HeatmapOptions, submitChangesHeatmap} from './HeatmapOptions.jsx';
import {FireflyHistogramOptions, submitChangesFFHistogram} from './FireflyHistogramOptions.jsx';
import {BasicOptionFields, basicFieldReducer, submitChanges, hasMarkerColor} from './BasicOptions.jsx';

const fieldProps = {labelWidth: 62, size: 15};
const dialogNameNewTrace = 'NewTracePanel';

function getSubmitChangesFunc(traceType) {
    switch(traceType) {
        case 'scatter':
        case 'scattergl':
            return submitChangesScatter;
        case 'heatmap':
            return submitChangesHeatmap;
        case 'fireflyHistogram':
            return submitChangesFFHistogram;
        default:
            return submitChanges;
    }
}

function getOptionsComponent({traceType, chartId, activeTrace, groupKey, tbl_id}) {
    const noColor = !hasMarkerColor(traceType);
    switch(traceType) {
        case 'scatter':
        case 'scattergl':
            return (<ScatterOptions {...{chartId, activeTrace, groupKey, tbl_id}}/>);
        case 'heatmap':
            return (<HeatmapOptions {...{chartId, activeTrace, groupKey, tbl_id}}/>);
        case 'fireflyHistogram':
            return (<FireflyHistogramOptions {...{chartId, activeTrace, groupKey, tbl_id}}/>);
        default:
            return (
                <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={fieldReducer({chartId, activeTrace})}>
                    <ValidationField fieldKey={`_tables.data.${activeTrace}.x`}/>
                    <ValidationField fieldKey={`_tables.data.${activeTrace}.y`}/>
                    <BasicOptionFields {...{activeTrace, groupKey, noColor}}/>
                </FieldGroup>
            );
    }
}

export class NewTracePanel extends SimpleComponent {

    getNextState(np) {
        const {chartId} = this.props;
        const {data=[]} = getChartData(chartId);
        const activeTrace = data.length;        //setting activeTrace to next available index.
        const type = getFieldVal('new-trace', 'type') || 'scatter';
        const groupKey = `${chartId}-new-trace-${type}`;
        return {groupKey, activeTrace, type};
    }

    render() {
        const {tbl_id, chartId, hideDialog=()=>dispatchHideDialog(dialogNameNewTrace)} = this.props;
        const {groupKey, activeTrace, type} = this.state;
        const doAdd = (fields) => {
            const traceType = type;
            const submitChangesFunc =  getSubmitChangesFunc(traceType);

            fields = Object.assign({activeTrace}, fields);  // make the newly added trace active
            fields[`data.${activeTrace}.type`] = type; // make sure trace type is set

            // apply defaults settings
            Object.entries(getNewTraceDefaults(type, activeTrace))
                    .forEach(([k,v]) => !fields[k] && (fields[k] = v));
            
            // need to hide before the changes are submitted to avoid React Internal error too much recursion (mounting/unmouting fields)
            hideDialog();
            submitChangesFunc({chartId, activeTrace, fields, tbl_id});
        };

        return (
            <div style={{padding: 10}}>
                <FieldGroup className='FieldGroup__vertical' style={{padding: 5}} keepState={true} groupKey='new-trace'>
                    <ListBoxInputField fieldKey='type' tooltip='Select plot type' label='Plot Type:'
                        options={[
                            {label: 'Scatter', value: 'scatter'},
                            {label: 'Heatmap', value: 'heatmap'},
                            {label: 'Histogram', value: 'fireflyHistogram'}
                        ]}
                        {...fieldProps} />
                </FieldGroup>
                <br/>
                {getOptionsComponent({traceType:type, chartId, activeTrace, groupKey, tbl_id})}
                <div style={{display: 'inline-flex', marginTop: 10, justifyContent: 'space-between'}}>
                    <CompleteButton groupKey={groupKey}
                                    onSuccess={doAdd}
                                    onFail={() => {}}    //invalid fields highlighted, anything else?
                                    text='ADD'
                    />
                    <button type='button' className='button std'
                            onClick={hideDialog}>Cancel
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
        DialogRootContainer.defineDialog(dialogNameNewTrace, content);
        dispatchShowDialog(dialogNameNewTrace);
    }
    return (
        <button type='button' className='button std' onClick={showNewTracePanel}>Add Series</button>
    );
}

function fieldReducer({chartId, activeTrace}) {
    const basicReducer = basicFieldReducer({chartId, activeTrace});
    const fields = {
        [`_tables.data.${activeTrace}.x`]: {
            fieldKey: `_tables.data.${activeTrace}.x`,
            value: '',
            tooltip: 'X axis',
            label: 'X:',
            ...fieldProps
        },
        [`_tables.data.${activeTrace}.y`]: {
            fieldKey: `_tables.data.${activeTrace}.y`,
            value: '',
            tooltip: 'Y axis',
            label: 'Y:',
            ...fieldProps
        },
        ...basicReducer(null)
    };
    return (inFields, action) => {
        if (!inFields) {
            return fields;
        }
        return inFields;

    };
}

