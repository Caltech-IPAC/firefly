import React from 'react';
import {get} from 'lodash';

import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {LayoutOptions, basicFieldReducer, basicOptions} from './BasicOptions.jsx';
import {getChartData} from '../../ChartsCntlr.js';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {scatterInputs} from './ScatterOptions.jsx';
import {getChartProps} from '../../ChartUtil.js';
import {FieldGroupCollapsible} from '../../../ui/panel/CollapsiblePanel.jsx';

const fieldProps = {labelWidth: 62, size: 15};

/**
 * This are the options for Plotly histogram chart
 * Plotly histogram does not display well with firefly histogram, which is using Plotly bar chart
 * Known issues: shen shown with firefly histogram, Plotly histogram shows as lines or does not show at all
 */
export function HistogramOptions({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const [activeTrace] = useStoreConnector(() => {
        return pActiveTrace ?? getChartData(chartId)?.activeTrace;
    });

    groupKey = groupKey || `${chartId}-ffhist-${activeTrace}`;
    const {tbl_id, multiTrace, noColor} = getChartProps(chartId, ptbl_id, activeTrace);

    const {X} = scatterInputs({activeTrace, tbl_id, chartId, groupKey, fieldProps:{labelWidth: 62}});
    const {Name, Color} = basicOptions({activeTrace, tbl_id, chartId, groupKey, fieldProps:{labelWidth: 60}});
    const reducerFunc = fieldReducer({chartId, activeTrace});
    reducerFunc.ver = chartId+activeTrace;

    return (
        <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={reducerFunc}>
            <X/>
            <ListBoxInputField fieldKey={`data.${activeTrace}.histfunc`} options={[{value:'count'}, {value:'sum'}, {value:'avg'}, {value:'min'}, {value:'max'}]}/>
            <ValidationField fieldKey={`data.${activeTrace}.nbinsx`}/>
            <ValidationField fieldKey={`data.${activeTrace}.xbins.size`}/>
            <ValidationField fieldKey={`data.${activeTrace}.xbins.start`}/>
            <ValidationField fieldKey={`data.${activeTrace}.xbins.end`}/>
            <br/>

            <div style={{margin: '5px 0 0 -22px'}}>
                { (multiTrace || !noColor) &&
                <FieldGroupCollapsible  header='Trace Options' initialState= {{ value:'closed' }} fieldKey='traceOptions'>
                    {multiTrace && <Name/>}
                    {!noColor && <Color/>}
                </FieldGroupCollapsible>
                }
                <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey}}/>
            </div>

        </FieldGroup>
    );
}

export function fieldReducer({chartId, activeTrace}) {
    const basicReducer = basicFieldReducer({chartId, activeTrace});

    const getFields = () => {
        const {data, tablesources = {}} = getChartData(chartId);
        const tablesourceMappings = get(tablesources[activeTrace], 'mappings');

        const fields = {
            [`_tables.data.${activeTrace}.x`]: {
                fieldKey: `_tables.data.${activeTrace}.x`,
                value: get(tablesourceMappings, 'x', ''),
                tooltip: 'X axis',
                label: 'X:',
                ...fieldProps
            },
            [`data.${activeTrace}.histfunc`]: {
                fieldKey: `data.${activeTrace}.histfunc`,
                value: get(data, `${activeTrace}.histfunc`),
                tooltip: 'Binning function used for this histogram trace',
                label: 'Function:',
                ...fieldProps
            },
            [`data.${activeTrace}.nbinsx`]: {
                fieldKey: `data.${activeTrace}.nbinsx`,
                value: get(data, `${activeTrace}.nbinsx`),
                tooltip: 'Maximum number of desired bins',
                label: 'Num of bins:',
                ...fieldProps
            },
            [`data.${activeTrace}.xbins.size`]: {
                fieldKey: `data.${activeTrace}.xbins.size`,
                value: get(data, `${activeTrace}.xbins.size`),
                tooltip: 'Step in between value each x axis bin',
                label: 'Bin width:',
                ...fieldProps
            },
            [`data.${activeTrace}.xbins.start`]: {
                fieldKey: `data.${activeTrace}.xbins.start`,
                value: get(data, `${activeTrace}.xbins.start`),
                tooltip: 'Sets the starting value for the x axis bins',
                label: 'Min:',
                ...fieldProps
            },
            [`data.${activeTrace}.xbins.end`]: {
                fieldKey: `data.${activeTrace}.xbins.end`,
                value: get(data, `${activeTrace}.xbins.end`),
                tooltip: 'Sets the end value for the x axis bins.',
                label: 'Max:',
                ...fieldProps
            },
            ...basicReducer(null)
        };
        return fields;
    };
    return (inFields, action) => {
        if (!inFields) {
            return getFields();
        } else {
            return inFields;
        }
    };
}
