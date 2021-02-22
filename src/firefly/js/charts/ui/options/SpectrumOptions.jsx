import React from 'react';
import PropTypes from 'prop-types';
import {get, isUndefined, pick} from 'lodash';

import {BasicOptionFields} from './BasicOptions.jsx';
import {getChartData, getTraceSymbol} from '../../ChartsCntlr.js';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {errorFieldKey, errorMinusFieldKey, Errors, errorTypeFieldKey, getDefaultErrorType} from './Errors.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {fieldReducer} from './ScatterOptions.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {updateSet} from '../../../util/WebUtil.js';


const fieldProps = {labelWidth: 155, size: 15};


export function SedOptions ({chartId, tbl_id, activeTrace, showMultiTrace, groupKey}) {

    const [{tablesources, activeTrace:cActiveTrace=0}] = useStoreConnector(() => pick(getChartData(chartId), ['tablesources', 'activeTrace']));

    activeTrace = isUndefined(activeTrace) ? cActiveTrace : activeTrace;
    groupKey = groupKey || `${chartId}-ffsed-${activeTrace}`;
    const tablesource = get(tablesources, [cActiveTrace]);
    tbl_id = get(tablesource, 'tbl_id') || tbl_id;

    return(
        <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={false} reducerFunc={spectrumReducer({chartId, activeTrace})}>

            <div className='FieldGroup__vertical'>
                <ListBoxInputField fieldKey={`data.${activeTrace}.mode`} options={[{label: 'points', value:'markers'}, {label: 'connected points', value:'lines+markers'}]}/>
                <ListBoxInputField fieldKey={`data.${activeTrace}.marker.symbol`}
                                   options={[{value:'circle'}, {value:'circle-open'}, {value:'square'}, {value:'square-open'}, {value:'diamond'}, {value:'diamond-open'},
                                             {value:'cross'}, {value:'x'}, {value:'triangle-up'}, {value:'hexagon'}, {value:'star'}]}/>
            </div>
            <p/>
            <SpectrumOptions {...{chartId, tablesource, activeTrace, groupKey}}/>
            <div style={{marginLeft: -15}}>
                <BasicOptionFields {...{activeTrace, groupKey, xNoLog: true, showMultiTrace}}/>
            </div>
        </FieldGroup>
    );
}



export function SpectrumOptions({chartId, tablesource={}, activeTrace, groupKey}) {
    // _tables.  is prefixed the fieldKey.  it will be replaced with 'tables::val' on submitChanges.
    const tbl_id = get(tablesource, 'tbl_id');
    const colValStats = getColValStats(tbl_id);
    if (!colValStats) { return null; }
    const xProps = {fldPath:`_tables.data.${activeTrace}.x`, label: 'Spectral axis column(X):', name: 'X', nullAllowed: false, colValStats, groupKey, ...fieldProps};
    const yProps = {fldPath:`_tables.data.${activeTrace}.y`, label: 'Flux axis column(Y):', name: 'Y', nullAllowed: false, colValStats, groupKey, ...fieldProps};
    const xMaxProps = {fldPath:`_tables.fireflyData.${activeTrace}.xMax`, label: 'Spectral axis upper limit column:', name: 'Upper Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};
    const xMinProps = {fldPath:`_tables.fireflyData.${activeTrace}.xMin`, label: 'Spectral axis lower limit column:', name: 'Lower Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};
    const yMaxProps = {fldPath:`_tables.fireflyData.${activeTrace}.yMax`, label: 'Flux axis upper limit column:', name: 'Upper Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};
    const yMinProps = {fldPath:`_tables.fireflyData.${activeTrace}.yMin`, label: 'Flux axis lower limit column:', name: 'Lower Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};

    return (
        <div className='FieldGroup__vertical'>
            <ColumnOrExpression {...xProps}/>
            <Errors {...{axis: 'x', groupKey, colValStats, activeTrace, ...fieldProps, labelWidth: 75}}/>
            <ColumnOrExpression {...xMaxProps}/>
            <ColumnOrExpression {...xMinProps}/>
            <br/>
            <ColumnOrExpression {...yProps}/>
            <Errors {...{axis:'y', groupKey, colValStats, activeTrace, ...fieldProps, labelWidth: 75}}/>
            <ColumnOrExpression {...yMaxProps}/>
            <ColumnOrExpression {...yMinProps}/>
        </div>
    );
}

SpectrumOptions.propTypes = {
    chartId: PropTypes.string,
    tablesource: PropTypes.object,
    activeTrace: PropTypes.number,
    groupKey: PropTypes.string,
    showMultiTrace: PropTypes.bool
};


export function spectrumReducer({chartId, activeTrace}) {
    const scatterReducer = fieldReducer({chartId, activeTrace});

    const getFields = () => {
        const chartData = getChartData(chartId);
        const {tablesources={}} = chartData;
        const tablesourceMappings = get(tablesources[activeTrace], 'mappings');

        const fields = {
            [`_tables.fireflyData.${activeTrace}.xMax`]: {
                fieldKey: `_tables.fireflyData.${activeTrace}.xMax`,
                value: get(tablesourceMappings, `fireflyData.${activeTrace}.xMax`, ''),
                ...fieldProps
            },
            [`_tables.fireflyData.${activeTrace}.xMin`]: {
                fieldKey: `_tables.fireflyData.${activeTrace}.xMin`,
                value: get(tablesourceMappings, `fireflyData.${activeTrace}.xMin`, ''),
                ...fieldProps
            },
            ...scatterReducer(null)
        };
        return fields;
    };

    return (inFields, action) => {
        if (!inFields) {
            return getFields();
        }

        inFields = scatterReducer(inFields, action);

        const {payload:{fieldKey='', value=''}, type} = action;

        if (type === VALUE_CHANGE) {
            // when field changes, clear error fields
            ['x','y'].forEach((a) => {
                if (fieldKey === `_tables.data.${activeTrace}.${a}`) {
                    inFields = updateSet(inFields, [errorTypeFieldKey(activeTrace, `${a}`), 'value'], 'none');
                    inFields = updateSet(inFields, [errorFieldKey(activeTrace, `${a}`), 'value'], undefined);
                    inFields = updateSet(inFields, [errorMinusFieldKey(activeTrace, `${a}`), 'value'], undefined);
                }
            });
        }
        return inFields;

    };
}
