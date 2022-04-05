import React from 'react';
import {get} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';

import {toBoolean} from '../../../util/WebUtil.js';
import {intValidator} from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {LayoutOptions, basicFieldReducer, helpStyle, submitChanges, basicOptions} from './BasicOptions.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {ALL_COLORSCALE_NAMES, PlotlyCS} from '../../Colorscale.js';
import {getChartProps} from '../../ChartUtil.js';
import {FieldGroupCollapsible} from '../../../ui/panel/CollapsiblePanel.jsx';



const fieldProps = {labelWidth: 62, size: 15};

export function HeatmapOptions({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const activeTrace = useStoreConnector(() => pActiveTrace ?? getChartData(chartId)?.activeTrace);

    groupKey = groupKey || `${chartId}-heatmap-${activeTrace}`;
    const {tablesource, tbl_id, multiTrace} = getChartProps(chartId, ptbl_id, activeTrace);
    const {Name} = basicOptions({activeTrace, tbl_id, chartId, groupKey, fieldProps:{labelWidth: 60}});

    const reducerFunc = fieldReducer({chartId, activeTrace});
    reducerFunc.ver = chartId+activeTrace;

    return (
        <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={reducerFunc}>
            {tablesource && <TableSourcesOptions {...{tablesource, activeTrace, groupKey}}/>}
            <br/>
            <div style={{margin: '5px 0 0 -22px'}}>
                { (multiTrace) &&
                <FieldGroupCollapsible  header='Trace Options' initialState= {{ value:'closed' }} fieldKey='traceOptions'>
                    {multiTrace && <Name/>}
                </FieldGroupCollapsible>
                }
                <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey, noColor: true}}/>
            </div>
        </FieldGroup>
    );
}

export function fieldReducer({chartId, activeTrace}) {
    const basicReducer = basicFieldReducer({chartId, activeTrace});

    const getFields = () => {
        const {data, fireflyData, tablesources = {}} = getChartData(chartId);
        const tablesourceMappings = get(tablesources[activeTrace], 'mappings');
        let colorscaleName = get(fireflyData, `${activeTrace}.colorscale`);
        if (!colorscaleName) {
            const colorscale = get(data, `${activeTrace}.colorscale`);
            if (colorscale && PlotlyCS.includes(colorscale)) {
                colorscaleName = colorscale;
            }
        }

        const fields = {

            [`fireflyData.${activeTrace}.nbins.x`]: {
                fieldKey: `fireflyData.${activeTrace}.nbins.x`,
                value: get(fireflyData, `${activeTrace}.nbins.x`),
                validator: intValidator(2, 300, 'Number of X-Bins'), // need at least 2 bins to display dx correctly
                tooltip: 'Number of bins along X axis',
                label: 'Number of X-Bins:',
                labelWidth: 95,
                size: 5
            },
            [`fireflyData.${activeTrace}.nbins.y`]: {
                fieldKey: `fireflyData.${activeTrace}.nbins.y`,
                value: get(fireflyData, `${activeTrace}.nbins.y`),
                validator: intValidator(2, 300, 'Number of Y-Bins'), // need at least 2 bins to display dy correctly
                tooltip: 'Number of bins along Y axis',
                label: 'Number of Y-Bins:',
                labelWidth: 95,
                size: 5
            },
            [`fireflyData.${activeTrace}.colorscale`]: {
                fieldKey: `fireflyData.${activeTrace}.colorscale`,
                value: colorscaleName,
                tooltip: 'Select colorscale for color map',
                label: 'Color Scale:',
                ...fieldProps
            },
            [`data.${activeTrace}.reversescale`]: {
                fieldKey: `data.${activeTrace}.reversescale`,
                value: get(data, `${activeTrace}.reversescale`) ? 'true' : undefined,
                tooltip: 'Reverse colorscale for color map',
                label: ' ',
                labelWidth: 10
            },
            ...basicReducer(null)
        };
        const tblRelFields = {
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
            }
        };
        return tablesourceMappings? Object.assign({}, fields, tblRelFields) : fields;
    };
    return (inFields, action) => {
        if (!inFields) {
            return getFields();
        }

        inFields = basicReducer(inFields, action);
        return inFields;

    };
}

export function TableSourcesOptions({tablesource={}, activeTrace, groupKey}) {
    // _tables.  is prefixed the fieldKey.  it will be replaced with 'tables::val' on submitChanges.
    const tbl_id = get(tablesource, 'tbl_id');
    const colValStats = getColValStats(tbl_id);
    const labelWidth = 30;
    const xProps = {fldPath:`_tables.data.${activeTrace}.x`, label: 'X:', name: 'X', nullAllowed: false, colValStats, groupKey, labelWidth};
    const yProps = {fldPath:`_tables.data.${activeTrace}.y`, label: 'Y:', name: 'Y', nullAllowed: false, colValStats, groupKey, labelWidth};

    return (
        <div className='FieldGroup__vertical'>
            <br/>
            <div style={helpStyle}>
                For X and Y, enter a column or an expression<br/>
                ex. log(col); 100*col1/col2; col1-col2
            </div>
            {colValStats && <ColumnOrExpression {...xProps}/>}
            {colValStats && <ColumnOrExpression {...yProps}/>}
            <div style={{whiteSpace: 'nowrap'}}>
                <ListBoxInputField fieldKey={`fireflyData.${activeTrace}.colorscale`}
                                   inline={true}
                                   options={[{label:'Default',value:undefined}].concat(ALL_COLORSCALE_NAMES.map((e)=>({value:e})))}/>
                <CheckboxGroupInputField
                    fieldKey={`data.${activeTrace}.reversescale`}
                    wrapperStyle={{display: 'inline-block'}}
                    options={[
                            {label: 'reverse', value: 'true'}
                        ]}
                />
            </div>
            {colValStats && <ValidationField fieldKey={`fireflyData.${activeTrace}.nbins.x`}/>}
            {colValStats && <ValidationField fieldKey={`fireflyData.${activeTrace}.nbins.y`}/>}
        </div>
    );
}

export function submitChangesHeatmap({chartId, activeTrace, fields, tbl_id, renderTreeId}) {
    const dataType = (!tbl_id) ? 'heatmap' : 'fireflyHeatmap';
    const changes = {
        [`data.${activeTrace}.type`] : 'heatmap',
        [`fireflyData.${activeTrace}.dataType`] : dataType
    };

    Object.assign(changes, fields);

    // reversescale is boolean
    changes[`data.${activeTrace}.reversescale`] = toBoolean(get(fields, `data.${activeTrace}.reversescale`));

    submitChanges({chartId, fields: changes, tbl_id, renderTreeId});
}

