import React from 'react';

import {get} from 'lodash';
import {getChartData} from '../../ChartsCntlr.js';
import {getNewTraceDefaults, hasMarkerColor} from '../../ChartUtil.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {getFieldVal} from '../../../fieldGroup/FieldGroupUtils.js';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {ScatterOptions, submitChangesScatter} from './ScatterOptions.jsx';
import {HeatmapOptions, submitChangesHeatmap} from './HeatmapOptions.jsx';
import {submitChangesSpectrum} from './SpectrumOptions.jsx';
import {FireflyHistogramOptions, submitChangesFFHistogram} from './FireflyHistogramOptions.jsx';
import {LayoutOptions, basicFieldReducer, submitChanges} from './BasicOptions.jsx';
import {Stack} from '@mui/joy';
import {CollapsibleGroup} from 'firefly/ui/panel/CollapsiblePanel';


export function getSubmitChangesFunc(traceType, fireflyType) {
    const type = fireflyType || traceType;

    switch(type) {
        case 'scatterOrHeatmap':
            if (traceType === 'heatmap') {
                return submitChangesHeatmap;
            } else {
                return submitChangesScatter;
            }
        case 'spectrum':
            return submitChangesSpectrum;
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
                <FieldGroup keepState={false} groupKey={groupKey} reducerFunc={fieldReducer({chartId, activeTrace})}>
                    <Stack spacing={2} sx={{
                        '.MuiFormLabel-root': {width: '8rem'},
                    }}>
                        <ValidationField fieldKey={`_tables.data.${activeTrace}.x`}/>
                        <ValidationField fieldKey={`_tables.data.${activeTrace}.y`}/>
                        <CollapsibleGroup>
                            <LayoutOptions {...{chartId, activeTrace, groupKey, tbl_id, noColor}}/>
                        </CollapsibleGroup>
                    </Stack>
                </FieldGroup>
            );
    }
}

export function getNewTraceType() {
    return getFieldVal('new-trace', 'type') || 'scatter';
}

export function addNewTrace({chartId, tbl_id, fields, hideDialog, renderTreeId}) {
    const type = getNewTraceType();
    const submitChangesFunc =  getSubmitChangesFunc(type);
    const data = get(getChartData(chartId), 'data', []);
    const activeTrace = data.length;

    fields = Object.assign({activeTrace}, fields);  // make the newly added trace active
    //fields[`data.${activeTrace}.type`] = type; // make sure trace type is set

    // apply defaults settings
    Object.entries(getNewTraceDefaults(chartId, type, activeTrace))
        .forEach(([k,v]) => !fields[k] && (fields[k] = v));

    // need to hide before the changes are submitted to avoid React Internal error:
    //    too much recursion (mounting/unmouting fields)
    hideDialog();
    submitChangesFunc({chartId, activeTrace, fields, tbl_id, renderTreeId});
}


export function NewTracePanel({tbl_id, chartId, groupKey}) {
    const {activeTrace, traceType} = useStoreConnector(() => ({
        activeTrace: getChartData(chartId)?.data?.length ?? 0, // next available index in data array
        traceType: getFieldVal('new-trace', 'type') || 'scatter'}), [chartId]);

    return (
        <Stack spacing={2}>
            <FieldGroup keepState={true} groupKey='new-trace'>
                <ListBoxInputField fieldKey='type' tooltip='Select plot type' label='Plot Type:'
                                   options={[
                                       {label: 'Scatter', value: 'scatter'},
                                       {label: 'Heatmap', value: 'fireflyHeatmap'},
                                       {label: 'Histogram', value: 'fireflyHistogram'}
                                   ]}/>
            </FieldGroup>
            {getOptionsComponent({traceType, chartId, activeTrace, groupKey, tbl_id})}
        </Stack>
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
        },
        [`_tables.data.${activeTrace}.y`]: {
            fieldKey: `_tables.data.${activeTrace}.y`,
            value: '',
            tooltip: 'Y axis',
            label: 'Y:',
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

