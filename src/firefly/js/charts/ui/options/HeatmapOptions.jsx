import React from 'react';
import {get, isArray, isUndefined} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';

import {toBoolean} from '../../../util/WebUtil.js';
import {intValidator} from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {BasicOptionFields, basicFieldReducer, helpStyle, submitChanges} from './BasicOptions.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {ALL_COLORSCALE_NAMES, PlotlyCS} from '../../Colorscale.js';



const fieldProps = {labelWidth: 62, size: 15};

export class HeatmapOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace:cActiveTrace=0} = getChartData(chartId);
        // activeTrace is passed via property, when used from NewTracePanel
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        return {activeTrace};
    }

    render() {
        const {chartId, tbl_id:tblIdProp, showMultiTrace} = this.props;
        const {tablesources, activeTrace:cActiveTrace=0} = getChartData(chartId);
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        const groupKey = this.props.groupKey || `${chartId}-heatmap-${activeTrace}`;
        const tablesource = get(tablesources, [cActiveTrace], tblIdProp && {tbl_id: tblIdProp});

        return (
            <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={fieldReducer({chartId, activeTrace})}>
                {tablesource && <TableSourcesOptions {...{tablesource, activeTrace, groupKey}}/>}
                <br/>
                <BasicOptionFields {...{activeTrace, groupKey, noColor: true, showMultiTrace}}/>
            </FieldGroup>
        );
    }
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
                                   options={[{value:'Default'}].concat(ALL_COLORSCALE_NAMES.map((e)=>({value:e})))}/>
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

