import React from 'react';
import {get} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {ValidationField} from '../../../ui/ValidationField.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {BasicOptionFields, OptionTopBar, basicFieldReducer} from './BasicOptions.jsx';
import {updateSet} from '../../../util/WebUtil.js';
import {SimpleComponent} from '../../../ui/SimpleComponent.jsx';

const fieldProps = {labelWidth: 62, size: 15};

export class ScatterOptions extends SimpleComponent {

    getNextState() {
        const {chartId} = this.props;
        const {activeTrace} = getChartData(chartId);
        return {activeTrace};
    }

    render() {
        const {chartId} = this.props;
        const {activeTrace=0} = this.state;
        const {tablesources, data, layout} = getChartData(chartId);
        const groupKey = `${chartId}-scatter-${activeTrace}`;
        const tablesource = get(tablesources, [activeTrace]);
        const tbl_id = get(tablesource, 'tbl_id');
        return (
            <div className='TablePanelOptions' style={{minWidth: 250, width: 'auto', border: 'solid 1px #bbb'}}>
                <OptionTopBar {...{groupKey, chartId, tbl_id}}/>
                <FieldGroup className='FieldGroup__vertical' keepState={false} groupKey={groupKey} reducerFunc={fieldReducer({data, layout, activeTrace, tablesources})}>
                    <BasicOptionFields {...{layout, data, activeTrace}}/>
                    <ListBoxInputField fieldKey={`data.${activeTrace}.mode`} options={[{value:'markers'}, {value:'lines'}, {value:'lines+markers'}]}/>
                    <ListBoxInputField fieldKey={`data.${activeTrace}.marker.symbol`}
                                       options={[{value:'circle'}, {value:'circle-open'}, {value:'square'}, {value:'square-open'}, {value:'diamond'}, {value:'diamond-open'},
                                                 {value:'cross'}, {value:'x'}, {value:'triangle-up'}, {value:'hexagon'}, , {value:'star'}]}/>
                    {tablesource && <TableSourcesOptions {...{tablesource, activeTrace}}/>}
                </FieldGroup>
            </div>
        );
    }
}

export function fieldReducer({data, layout, activeTrace, tablesources={}}) {
    const tablesource = tablesources[activeTrace];
    const basicReducer = basicFieldReducer({data, layout, activeTrace, tablesources});
    const fields = {
        [`data.${activeTrace}.mode`]: {
            fieldKey: `data.${activeTrace}.mode`,
            value: get(data, `${activeTrace}.mode`),
            tooltip: 'Select plot style',
            label: 'Plot Style:',
            ...fieldProps,
        },
        [`data.${activeTrace}.marker.symbol`]: {
            fieldKey: `data.${activeTrace}.marker.symbol`,
            value: get(data, `${activeTrace}.marker.symbol`),
            tooltip: 'Select marker symbol',
            label: 'Symbol:',
            ...fieldProps,
        },
        ...basicReducer(null)
    };
    const tblRelFields = {
        [`_tables.data.${activeTrace}.x`]: {
            fieldKey: `_tables.data.${activeTrace}.x`,
            value: get(tablesource, 'x', ''),
            tooltip: 'X axis',
            label : 'x:',
            ...fieldProps,
        },
        [`_tables.data.${activeTrace}.y`]: {
            fieldKey: `_tables.data.${activeTrace}.y`,
            value: get(tablesource, 'y', ''),
            tooltip: 'Y axis',
            label : 'y:',
            ...fieldProps,
        },
        [`_tables.data.${activeTrace}.error_x.array`]: {
            fieldKey: `_tables.data.${activeTrace}.error_x.array`,
            value: get(tablesource, ['error_x.array'], ''),
            tooltip: 'X error',
            label : 'error_x:',
            ...fieldProps,
        },
        [`_tables.data.${activeTrace}.error_y.array`]: {
            fieldKey: `_tables.data.${activeTrace}.error_y.array`,
            value: get(tablesource, ['error_y.array'], ''),
            tooltip: 'Y error',
            label : 'error_y:',
            ...fieldProps,
        },
        [`_tables.data.${activeTrace}.marker.color`]: {
            fieldKey: `_tables.data.${activeTrace}.marker.color`,
            value: get(tablesource, 'marker.color', ''),
            tooltip: 'Use a column for color map',
            label : 'Color Map:',
            ...fieldProps,
        },
        [`_tables.data.${activeTrace}.marker.size`]: {
            fieldKey: `_tables.data.${activeTrace}.marker.size`,
            value: get(tablesource, 'marker.size', ''),
            tooltip: 'Use a column for size map',
            label : 'Size Map:',
            ...fieldProps,
        }
    };
    return (inFields, action) => {
        if (!inFields) {
            return tablesource? Object.assign({}, fields, tblRelFields) : fields;
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

export function TableSourcesOptions({tablesource={}, activeTrace}) {
    // _tables.  is prefixed the fieldKey.  it will be replaced with 'tables::tbl_id,val' on submitChanges.
    return (
        <div className='FieldGroup__vertical'>

            <ValidationField fieldKey={`_tables.data.${activeTrace}.x`}/>
            <ValidationField fieldKey={`_tables.data.${activeTrace}.y`}/>
            <ValidationField fieldKey={`_tables.data.${activeTrace}.error_x.array`}/>
            <ValidationField fieldKey={`_tables.data.${activeTrace}.error_y.array`}/>
            <ValidationField fieldKey={`_tables.data.${activeTrace}.marker.color`}/>
            <ValidationField fieldKey={`_tables.data.${activeTrace}.marker.size`}/>
        </div>
    );

}