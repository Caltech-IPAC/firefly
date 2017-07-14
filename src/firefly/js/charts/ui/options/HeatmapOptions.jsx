import React from 'react';
import {get, isUndefined} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';

import {intValidator} from '../../../util/Validate.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {CheckboxGroupInputField} from '../../../ui/CheckboxGroupInputField.jsx';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';
import {BasicOptionFields, OptionTopBar, basicFieldReducer, submitChanges} from './BasicOptions.jsx';
import {addColorbarChanges} from '../../dataTypes/FireflyHeatmap.js';
import {getColValStats} from '../../TableStatsCntlr.js';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';


const fieldProps = {labelWidth: 62, size: 15};

export class HeatmapOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace:cActiveTrace} = getChartData(chartId);
        // activeTrace is passed via property, when used from NewTracePanel
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        return {activeTrace};
    }

    render() {
        const {chartId} = this.props;
        const {tablesources, activeTrace:cActiveTrace=0} = getChartData(chartId);
        const activeTrace = isUndefined(this.props.activeTrace) ? cActiveTrace : this.props.activeTrace;
        const groupKey = this.props.groupKey || `${chartId}-heatmap-${activeTrace}`;
        const tablesource = get(tablesources, [cActiveTrace]);
        const tbl_id = get(tablesource, 'tbl_id');

        return (
            <div style={{padding:'0 5px 7px'}}>
                {isUndefined(this.props.activeTrace) && <OptionTopBar {...{groupKey, activeTrace, chartId, tbl_id, submitChangesFunc: submitChangesHeatmap}}/>}
                <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={fieldReducer({chartId, activeTrace})}>
                    {tablesource && <TableSourcesOptions {...{tablesource, activeTrace, groupKey}}/>}
                    <br/>
                    <BasicOptionFields {...{activeTrace, groupKey}}/>
                </FieldGroup>
            </div>
        );
    }
}

export function fieldReducer({chartId, activeTrace}) {
    const {data, fireflyData, tablesources={}} = getChartData(chartId);
    const tablesourceMappings = get(tablesources[activeTrace], 'mappings');
    const basicReducer = basicFieldReducer({chartId, activeTrace, tablesources});
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
        [`data.${activeTrace}.colorscale`]: {
            fieldKey: `data.${activeTrace}.colorscale`,
            value: get(data, `${activeTrace}.colorscale`),
            tooltip: 'Select colorscale for color map',
            label: 'Color Scale:',
            ...fieldProps
        },
        [`data.${activeTrace}.reversescale`]: {
            fieldKey: `data.${activeTrace}.reversescale`,
            value: String(get(data, `${activeTrace}.reversescale`, '')),
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
    return (inFields, action) => {
        if (!inFields) {
            return tablesourceMappings? Object.assign({}, fields, tblRelFields) : fields;
        }

        inFields = basicReducer(inFields, action);
        return inFields;

    };
}

export function TableSourcesOptions({tablesource={}, activeTrace, groupKey}) {
    // _tables.  is prefixed the fieldKey.  it will be replaced with 'tables::tbl_id,val' on submitChanges.
    const tbl_id = get(tablesource, 'tbl_id');
    const colValStats = getColValStats(tbl_id);
    const labelWidth = 30;
    const xProps = {fldPath:`_tables.data.${activeTrace}.x`, label: 'X:', name: 'X', nullAllowed: false, colValStats, groupKey, labelWidth};
    const yProps = {fldPath:`_tables.data.${activeTrace}.y`, label: 'Y:', name: 'Y', nullAllowed: false, colValStats, groupKey, labelWidth};

    return (
        <div className='FieldGroup__vertical'>
            <br/>
            <ColumnOrExpression {...xProps}/>
            <ColumnOrExpression {...yProps}/>
            <div style={{whiteSpace: 'nowrap'}}>
                <ListBoxInputField fieldKey={`data.${activeTrace}.colorscale`}
                                   inline={true}
                                   options={[{label:'Default', value:undefined}, {value:'Bluered'}, {value:'Blues'}, {value:'Earth'}, {value:'Electric'}, {value:'Greens'},
                                         {value:'Greys'}, {value:'Hot'}, {value:'Jet'}, {value:'Picnic'}, {value:'Portland'}, {value:'Rainbow'},
                                         {value:'RdBu'}, {value:'Reds'}, {value:'Viridis'}, {value:'YlGnBu'}, {value:'YlOrRd'}]}/>
                <CheckboxGroupInputField
                    fieldKey={`data.${activeTrace}.reversescale`}
                    wrapperStyle={{display: 'inline-block'}}
                    options={[
                            {label: 'reverse', value: 'true'}
                        ]}
                />
            </div>
            <ValidationField fieldKey={`fireflyData.${activeTrace}.nbins.x`}/>
            <ValidationField fieldKey={`fireflyData.${activeTrace}.nbins.y`}/>
        </div>
    );
}

export function submitChangesHeatmap({chartId, activeTrace, fields, tbl_id}) {
    const dataType = (!tbl_id) ? 'heatmap' : 'fireflyHeatmap';
    const changes = {
        [`fireflyData.${activeTrace}.type`] : 'heatmap',
        [`fireflyData.${activeTrace}.dataType`] : dataType
    };

    Object.assign(changes, fields);

    // reversescale is boolean
    changes[`data.${activeTrace}.reversescale`] = get(fields, `data.${activeTrace}.reversescale`, '').includes('true');

    submitChanges({chartId, fields: changes, tbl_id});
}

