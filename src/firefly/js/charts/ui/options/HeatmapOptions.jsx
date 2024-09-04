import React, {useContext, useEffect} from 'react';
import {get} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {FieldGroup, FieldGroupCtx} from '../../../ui/FieldGroup.jsx';

import {toBoolean} from '../../../util/WebUtil.js';
import {intValidator} from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';
import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {basicFieldReducer, LayoutOptions, submitChanges, useBasicOptions} from './BasicOptions.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {ALL_COLORSCALE_NAMES, PlotlyCS} from '../../Colorscale.js';
import {getChartProps} from '../../ChartUtil.js';
import {CollapsibleGroup, FieldGroupCollapsibleItem} from '../../../ui/panel/CollapsiblePanel.jsx';
import {Stack, Typography} from '@mui/joy';


export function HeatmapOptions({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const activeTrace = useStoreConnector(() => pActiveTrace ?? getChartData(chartId)?.activeTrace);

    groupKey = groupKey || `${chartId}-heatmap-${activeTrace}`;
    const {tablesource, tbl_id, multiTrace} = getChartProps(chartId, ptbl_id, activeTrace);
    const {Name} = useBasicOptions({activeTrace, tbl_id, chartId, groupKey});

    const reducerFunc = fieldReducer({chartId, activeTrace});
    reducerFunc.ver = chartId+activeTrace;

    return (
        <FieldGroup keepState={false} groupKey={groupKey} reducerFunc={reducerFunc}>
            <Stack spacing={3}>
                {tablesource && <TableSourcesOptions {...{tablesource, activeTrace, groupKey, chartId}}/>}
                <CollapsibleGroup>
                    {(multiTrace) &&
                        <FieldGroupCollapsibleItem  header='Trace Options' initialState= {{ value:'closed' }} fieldKey='traceOptions'>
                            {multiTrace && <Name/>}
                        </FieldGroupCollapsibleItem>
                    }
                    <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey}}/>
                </CollapsibleGroup>
            </Stack>
        </FieldGroup>
    );
}

const getColorscaleName = ({data, fireflyData, activeTrace}) => {
    let colorscaleName = fireflyData?.[activeTrace]?.colorscale;
    if (!colorscaleName) {
        const colorscale = data?.[activeTrace]?.colorscale;
        if (colorscale && PlotlyCS.includes(colorscale)) {
            colorscaleName = colorscale;
        }
    }
    return colorscaleName;
};

export function fieldReducer({chartId, activeTrace}) {
    const basicReducer = basicFieldReducer({chartId, activeTrace});

    const getFields = () => {
        const {data, fireflyData} = getChartData(chartId);

        return {
            [`fireflyData.${activeTrace}.nbins.x`]: {
                fieldKey: `fireflyData.${activeTrace}.nbins.x`,
                value: get(fireflyData, `${activeTrace}.nbins.x`),
                validator: intValidator(2, 300, 'Number of X-Bins'), // need at least 2 bins to display dx correctly
                tooltip: 'Number of bins along X axis',
                label: 'Number of X-Bins:',
            },
            [`fireflyData.${activeTrace}.nbins.y`]: {
                fieldKey: `fireflyData.${activeTrace}.nbins.y`,
                value: get(fireflyData, `${activeTrace}.nbins.y`),
                validator: intValidator(2, 300, 'Number of Y-Bins'), // need at least 2 bins to display dy correctly
                tooltip: 'Number of bins along Y axis',
                label: 'Number of Y-Bins:',
            },
            [`fireflyData.${activeTrace}.colorscale`]: {
                fieldKey: `fireflyData.${activeTrace}.colorscale`,
                value: getColorscaleName({data, fireflyData, activeTrace}),
                tooltip: 'Select colorscale for color map',
                label: 'Color Scale:'
            },
            [`data.${activeTrace}.reversescale`]: {
                fieldKey: `data.${activeTrace}.reversescale`,
                value: get(data, `${activeTrace}.reversescale`) ? 'true' : undefined,
                tooltip: 'Reverse colorscale for color map',
                label: ' '
            },
            ...basicReducer(null)
        };
    };
    return (inFields, action) => {
        if (!inFields) {
            return getFields();
        }

        inFields = basicReducer(inFields, action);
        return inFields;

    };
}

export function TableSourcesOptions({tablesource={}, activeTrace, groupKey, chartId, orientation='horizontal'}) {
    // _tables.  is prefixed the fieldKey.  it will be replaced with 'tables::val' on submitChanges.
    const tbl_id = get(tablesource, 'tbl_id');
    const colValStats = getColValStats(tbl_id);

    const xyProps = (xOrY) => ({fldPath:`_tables.data.${activeTrace}.${xOrY}`, label: `${xOrY.toUpperCase()}:`,
        name: xOrY.toUpperCase(), nullAllowed: false, colValStats, groupKey, slotProps: {control: {orientation}},
        initValue: tablesource?.mappings?.[xOrY] ?? ''
    });

    const {setVal} = useContext(FieldGroupCtx);

    //on the first render, update store with the color scale value reduced from chart data
    //otherwise FieldGroupConnector is replacing it with stale state (1st option in ListBox)
    useEffect(()=>{
        const {data, fireflyData} = getChartData(chartId);
        const colorscaleName = getColorscaleName({data, fireflyData, activeTrace});
        colorscaleName && setVal(`fireflyData.${activeTrace}.colorscale`, colorscaleName);
    }, []);

    return (
        <Stack spacing={2} sx={{'.MuiFormLabel-root': {width: '5.5rem'}}}>
            <Typography level='body-sm'>
                For X and Y, enter a column or an expression<br/>
                ex. log(col); 100*col1/col2; col1-col2
            </Typography>
            {colValStats && <ColumnOrExpression {...xyProps('x')}/>}
            {colValStats && <ColumnOrExpression {...xyProps('y')}/>}
            <Stack direction='row' spacing={2}>
                <ListBoxInputField fieldKey={`fireflyData.${activeTrace}.colorscale`}
                                   options={[{value: 'Default'}].concat(ALL_COLORSCALE_NAMES.map((e)=>({value:e})))}
                                   orientation={orientation}
                                   sx={{'.MuiSelect-root': {minWidth: '8.5rem'}}}
                />
                <CheckboxGroupInputField
                    fieldKey={`data.${activeTrace}.reversescale`}
                    options={[{label: 'reverse', value: 'true'}]}
                />
            </Stack>
            {colValStats && <ValidationField fieldKey={`fireflyData.${activeTrace}.nbins.x`} orientation={orientation}/>}
            {colValStats && <ValidationField fieldKey={`fireflyData.${activeTrace}.nbins.y`} orientation={orientation}/>}
        </Stack>
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

