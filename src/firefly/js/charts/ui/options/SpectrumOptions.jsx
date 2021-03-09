import React from 'react';
import PropTypes from 'prop-types';
import {get, isUndefined, pick} from 'lodash';

import {BasicOptionFields} from './BasicOptions.jsx';
import {getChartData} from '../../ChartsCntlr.js';
import {getTblById} from '../../../tables/TableUtil.js';
import {getSpectrumDM} from '../../../util/VOAnalyzer.js';
import {getUnitInfo, getUnitConvExpr} from '../../dataTypes/SpectrumUnitConversion.js';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {getColValStats} from '../../TableStatsCntlr.js';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {ColumnOrExpression} from '../ColumnOrExpression.jsx';
import {errorFieldKey, errorMinusFieldKey, Errors, errorTypeFieldKey} from './Errors.jsx';
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
        <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={false} reducerFunc={spectrumReducer({chartId, activeTrace, tbl_id})}>

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
    const {spectralAxis, fluxAxis} = getSpectrumDM(getTblById(tbl_id)) || {};

    const colValStats = getColValStats(tbl_id);
    if (!colValStats) { return null; }
    const xProps = {readonly: true, fldPath:`_tables.data.${activeTrace}.x`, label: 'Spectral axis column(X):', name: 'X', nullAllowed: false, colValStats, groupKey, ...fieldProps};
    const yProps = {readonly: true, fldPath:`_tables.data.${activeTrace}.y`, label: 'Flux axis column(Y):', name: 'Y', nullAllowed: false, colValStats, groupKey, ...fieldProps};
    const xMaxProps = {fldPath:`_tables.fireflyData.${activeTrace}.xMax`, label: 'Spectral axis upper limit column:', name: 'Upper Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};
    const xMinProps = {fldPath:`_tables.fireflyData.${activeTrace}.xMin`, label: 'Spectral axis lower limit column:', name: 'Lower Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};
    const yMaxProps = {fldPath:`_tables.fireflyData.${activeTrace}.yMax`, label: 'Flux axis upper limit column:', name: 'Upper Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};
    const yMinProps = {fldPath:`_tables.fireflyData.${activeTrace}.yMin`, label: 'Flux axis lower limit column:', name: 'Lower Limit', nullAllowed: true, colValStats, groupKey, ...fieldProps};

    const hasXerrors = spectralAxis.statError || spectralAxis.statErrLow || spectralAxis.statErrHigh;
    const hasYerrors = fluxAxis.statError || fluxAxis.statErrLow || fluxAxis.statErrHigh;

    return (
        <div className='FieldGroup__vertical'>
            <ColumnOrExpression {...xProps}/>
            {hasXerrors && <Errors {...{axis: 'x', readonly: true, groupKey, colValStats, activeTrace, ...fieldProps, labelWidth: 75}}/>}
            <ColumnOrExpression {...xMaxProps}/>
            <ColumnOrExpression {...xMinProps}/>
            <Units {...{activeTrace, spectralAxis}}/>
            <br/>
            <ColumnOrExpression {...yProps}/>
            {hasYerrors && <Errors {...{axis: 'y', readonly: true, groupKey, colValStats, activeTrace, ...fieldProps, labelWidth: 75}}/>}
            <ColumnOrExpression {...yMaxProps}/>
            <ColumnOrExpression {...yMinProps}/>
            <Units {...{activeTrace, fluxAxis}}/>
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


export function spectrumReducer({chartId, activeTrace, tbl_id}) {
    const scatterReducer = fieldReducer({chartId, activeTrace});

    const {spectralAxis={}, fluxAxis={}} = getSpectrumDM(getTblById(tbl_id)) || {};

    const getFields = () => {
        const {fireflyData, tablesources = {}} = getChartData(chartId);
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
            [`fireflyData.${activeTrace}.xUnit`]: {
                fieldKey: `fireflyData.${activeTrace}.xUnit`,
                value: get(fireflyData, `${activeTrace}.xUnit`) || spectralAxis.unit,
                ...fieldProps
            },
            [`fireflyData.${activeTrace}.yUnit`]: {
                fieldKey: `fireflyData.${activeTrace}.yUnit`,
                value: get(fireflyData, `${activeTrace}.yUnit`) || fluxAxis.unit,
                ...fieldProps
            },
            ...scatterReducer(null)
        };
        return fields;
    };

    const updateErrors = (inFields, axisType, value) => {
        const errCname = inFields[errorFieldKey(activeTrace, axisType)];
        const errMinusCname = inFields[errorMinusFieldKey(activeTrace, axisType)];
        const axis = axisType === 'x' ? spectralAxis : fluxAxis;

        if (errCname) {
            const convVal = getUnitConvExpr({cname: axis.statErrHigh || axis.statError, from: axis.unit, to: value});
            inFields = updateSet(inFields, [errorFieldKey(activeTrace, axisType), 'value'], convVal);
        }
        if (errMinusCname) {
            const convVal = getUnitConvExpr({cname: axis.statErrLow, from: axis.unit, to: value});
            inFields = updateSet(inFields, [errorMinusFieldKey(activeTrace, axisType), 'value'], convVal);
        }
        return inFields;
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

            if (fieldKey === `fireflyData.${activeTrace}.xUnit`) {
                const xLabel = getUnitInfo(value, true).label;
                const colOrExpr = getUnitConvExpr({cname: spectralAxis.value, from: spectralAxis.unit, to: value});
                inFields = updateSet(inFields, [`_tables.data.${activeTrace}.x`, 'value'], colOrExpr);
                inFields = updateSet(inFields, ['layout.xaxis.title.text', 'value'], xLabel);
                inFields = updateErrors(inFields, 'x', value);
            }
            if (fieldKey === `fireflyData.${activeTrace}.yUnit`) {
                const yLabel = getUnitInfo(value, false).label;
                const colOrExpr = getUnitConvExpr({cname: fluxAxis.value, from: fluxAxis.unit, to: value});
                inFields = updateSet(inFields, [`_tables.data.${activeTrace}.y`, 'value'], colOrExpr);
                inFields = updateSet(inFields, ['layout.yaxis.title.text', 'value'], yLabel);
                inFields = updateErrors(inFields, 'y', value);
            }
        }
        return inFields;

    };
}


function Units({activeTrace, spectralAxis, fluxAxis}) {

    const value = spectralAxis?.unit || fluxAxis?.unit;
    const unitProp = spectralAxis ? 'xUnit' : 'yUnit';
    const label = spectralAxis ? 'Spectral axis units:' : 'Flux axis units:';
    const options = getUnitInfo(value)?.options;
    if (!options) return null;

    return (
        <ListBoxInputField fieldKey={`fireflyData.${activeTrace}.${unitProp}`} label={label}  options={options}/>
    );

}