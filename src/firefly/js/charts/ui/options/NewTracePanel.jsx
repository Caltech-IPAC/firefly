import React from 'react';

import {get} from 'lodash';
import {getChartData} from '../../ChartsCntlr.js';
import {getNewTraceDefaults} from '../../ChartUtil.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {ScatterOptions, submitChangesScatter} from './ScatterOptions.jsx';
import {HeatmapOptions, submitChangesHeatmap} from './HeatmapOptions.jsx';
import {FireflyHistogramOptions, submitChangesFFHistogram} from './FireflyHistogramOptions.jsx';
import {BasicOptionFields, basicFieldReducer, submitChanges, hasMarkerColor} from './BasicOptions.jsx';

const fieldProps = {labelWidth: 62, size: 15};

export function getSubmitChangesFunc(traceType, fireflyType) {
    const type = fireflyType || traceType;
    switch(type) {
        case 'scatter':
        case 'scattergl':
            return submitChangesScatter;
        case 'fireflyHeatmap':
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
        case 'fireflyHeatmap':
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

export function getNewTraceType() {
    return getFieldVal('new-trace', 'type') || 'scatter';
}

export function addNewTrace({chartId, tbl_id, fields, hideDialog}) {
    const type = getNewTraceType();
    const submitChangesFunc =  getSubmitChangesFunc(type);
    const data = get(getChartData(chartId), 'data', []);
    const activeTrace = data.length;

    fields = Object.assign({activeTrace}, fields);  // make the newly added trace active
    fields[`data.${activeTrace}.type`] = type; // make sure trace type is set

    // apply defaults settings
    Object.entries(getNewTraceDefaults(chartId, type, activeTrace))
        .forEach(([k,v]) => !fields[k] && (fields[k] = v));

    // need to hide before the changes are submitted to avoid React Internal error:
    //    too much recursion (mounting/unmouting fields)
    hideDialog();
    submitChangesFunc({chartId, activeTrace, fields, tbl_id});
}

export class NewTracePanel extends SimpleComponent {

    getNextState(np) {
        const {chartId} = np || this.props;
        const {data=[]} = getChartData(chartId);
        const activeTrace = data.length;        //setting activeTrace to next available index.
        const type = getFieldVal('new-trace', 'type') || 'scatter';
        return {activeTrace, type};
    }

    render() {
        const {tbl_id, chartId, groupKey} = this.props;
        const {activeTrace, type} = this.state;

        return (
            <div style={{padding: 10}}>
                <FieldGroup className='FieldGroup__vertical' style={{padding: 5}} keepState={true} groupKey='new-trace'>
                    <ListBoxInputField fieldKey='type' tooltip='Select plot type' label='Plot Type:'
                        options={[
                            {label: 'Scatter', value: 'scatter'},
                            {label: 'Heatmap', value: 'fireflyHeatmap'},
                            {label: 'Histogram', value: 'fireflyHistogram'}
                        ]}
                        {...fieldProps} />
                </FieldGroup>
                <br/>
                {getOptionsComponent({traceType:type, chartId, activeTrace, groupKey, tbl_id})}

            </div>
        );
    }
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

