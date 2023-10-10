import React from 'react';
import {get, range} from 'lodash';
import {getSpectrumDM} from '../../../voAnalyzer/SpectrumDM.js';

import {getChartData} from '../../ChartsCntlr.js';
import {getTblById} from '../../../tables/TableUtil.js';
import {canUnitConv, getUnitInfo, getUnitConvExpr} from '../../dataTypes/SpectrumUnitConversion.js';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {errorFieldKey, errorMinusFieldKey, errorShowFieldKey} from './Errors.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {fieldReducer, submitChangesScatter, scatterInputs, ScatterCommonOptions} from './ScatterOptions.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {updateSet, toBoolean} from '../../../util/WebUtil.js';
import {isSpectralOrder, getChartProps} from '../../ChartUtil.js';
import {basicOptions, LayoutOptions} from './BasicOptions.jsx';
import {getSpectrumProps} from '../../dataTypes/FireflySpectrum.js';


export function SpectrumOptions ({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const activeTrace = useStoreConnector(() => pActiveTrace ?? getChartData(chartId)?.activeTrace ?? 0);

    groupKey = groupKey || `${chartId}-ffsed-${activeTrace}`;

    const {tbl_id} = getChartProps(chartId, ptbl_id, activeTrace);
    const {xErrArray, yErrArray, xMax, xMin, yMax, yMin} = getSpectrumProps(tbl_id);

    const wideLabel = xMax || xMin || yMax || yMin;
    const fieldProps = {labelWidth: (wideLabel ? 155 : 116), size: 20};

    const {Xunit, Yunit} = spectrumInputs({activeTrace, tbl_id, chartId, groupKey, fieldProps});
    const {UseSpectrum, X, Xmax, Xmin, Y, Ymax, Ymin, Yerrors, Xerrors, Mode} = scatterInputs({activeTrace, tbl_id, chartId, groupKey, fieldProps});
    const {XaxisTitle, YaxisTitle} = basicOptions({activeTrace, tbl_id, chartId, groupKey, fieldProps});

    const reducerFunc = spectrumReducer({chartId, activeTrace, tbl_id});
    reducerFunc.ver = chartId+activeTrace+tbl_id;

    return(
        <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={false} reducerFunc={reducerFunc}>

            {!isSpectralOrder(chartId) && <UseSpectrum/>}

            <div className='FieldGroup__vertical'>
                <X label='Spectral axis column(X):' readonly={true}/>
                {xErrArray && <Xerrors labelWidth={wideLabel ? 85 : 46} readonly={true}/>}
                {xMax && <Xmax readonly={true}/>}
                {xMin && <Xmin readonly={true}/>}
                <Xunit/>
                <br/>
                <Y label='Flux axis column(Y):' readonly={true}/>
                {yErrArray && <Yerrors labelWidth={wideLabel ? 85 : 46} readonly={true}/>}
                {yMax && <Ymax readonly={true} label = 'Flux axis upper limit column:'/>}
                {yMin && <Ymin readonly={true} label = 'Flux axis lower limit column:'/>}
                <Yunit/>
                <br/>
                <Mode/>
            </div>

            <div style={{margin: '5px 0 0 -22px'}}>
                <ScatterCommonOptions{...{activeTrace, tbl_id, chartId, groupKey, fieldProps: {labelWidth: 60, size: 20}}}/>
                <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey}}
                               XaxisTitle={() => <XaxisTitle readonly={true} labelWidth={60}/>}
                               YaxisTitle={() => <YaxisTitle readonly={true} labelWidth={60}/>}
                />
            </div>

        </FieldGroup>
    );
}


export function spectrumReducer({chartId, activeTrace, tbl_id}) {
    const scatterReducer = fieldReducer({chartId, activeTrace, tbl_id});
    const {fireflyData, data, spectralAxis, fluxAxis} = getChartProps(chartId, tbl_id, activeTrace);

    return (inFields, action) => {
        if (!inFields) {
            return ;
            // return {... scatterReducer(null), ...Object.fromEntries( Object.values(spectrum).map((f) => f.field))};
        }

        inFields = scatterReducer(inFields, action);

        const {payload:{fieldKey='', value=''}, type} = action;

        if (type === VALUE_CHANGE) {
            ['x', 'y'].forEach((v) => {
                if (fieldKey === `fireflyData.${activeTrace}.${v}Unit`) {
                    const axis = v === 'x' ? spectralAxis : fluxAxis;
                    inFields = applyUnitConversion({fireflyData, data, inFields, axisType:v, newUnit:value, traceNum:activeTrace, axis, isInput:true});
                    inFields = updateSet(inFields, [`fireflyLayout.${v}axis.min`, 'value'], undefined);
                    inFields = updateSet(inFields, [`fireflyLayout.${v}axis.max`, 'value'], undefined);
                    inFields = updateSet(inFields, [`__${v}reset`, 'value'], 'true');
                }
            });
        }
        return inFields;

    };
}

export const applyUnitConversion = ({fireflyData, data, inFields, axisType, newUnit, traceNum, axis, isInput=false}) => {

    const path = (p) => isInput ? [p, 'value'] : [p];

    const layoutAxis = axisType === 'x' ? 'xaxis' : 'yaxis';

    const label = getUnitInfo(newUnit, axis.value).label;
    inFields = updateSet(inFields, path(`layout.${layoutAxis}.title.text`), label);

    const colOrExpr = getUnitConvExpr({cname: axis.value, from: axis.unit, to: newUnit});
    inFields = updateSet(inFields, path(`_tables.data.${traceNum}.${axisType}`), colOrExpr);

    const errCname = get(data, errorFieldKey(traceNum, axisType).replace('_tables.data.', ''));
    if (errCname) {
        const convVal = getUnitConvExpr({cname: axis.statErrHigh || axis.statError, from: axis.unit, to: newUnit});
        inFields = updateSet(inFields, path(errorFieldKey(traceNum, axisType)), convVal);
    }

    const errMinusCname =  get(data, errorMinusFieldKey(traceNum, axisType).replace('_tables.data.', ''));
    if (errMinusCname) {
        const convVal = getUnitConvExpr({cname: axis.statErrLow, from: axis.unit, to: newUnit});
        inFields = updateSet(inFields, path(errorMinusFieldKey(traceNum, axisType)), convVal);
    }

    const upperLimit =  get(fireflyData, `${traceNum}.${axisType}Max`);
    if (upperLimit) {
        const convVal = getUnitConvExpr({cname: axis.upperLimit, from: axis.unit, to: newUnit});
        inFields = updateSet(inFields, path(`_tables.fireflyData.${traceNum}.${axisType}Max`), convVal);
    }
    const lowerLimit =  get(fireflyData, `${traceNum}.${axisType}Min`);
    if (lowerLimit) {
        const convVal = getUnitConvExpr({cname: axis.lowerLimit, from: axis.unit, to: newUnit});
        inFields = updateSet(inFields, path(`_tables.fireflyData.${traceNum}.${axisType}Min`), convVal);
    }

    return inFields;
};



export function submitChangesSpectrum({chartId, activeTrace, fields, tbl_id, renderTreeId}) {

    // when unit changes, apply it to the other traces as well
    const {data, fireflyData} = getChartData(chartId);
    const {spectralAxis={}, fluxAxis={}} = getSpectrumDM(getTblById(tbl_id)) || {};

    const xUnit = fields[`fireflyData.${activeTrace}.xUnit`];
    const yUnit = fields[`fireflyData.${activeTrace}.yUnit`]; // undefined if no field for yUnit
    range(data.length).forEach((idx) => {
        if (idx !== activeTrace) {
            const xUnitTrace = fireflyData?.[idx]?.xUnit;
            const yUnitTrace = fireflyData?.[idx]?.yUnit;
            // resetting unit resets the field,
            if (canUnitConv({from: xUnitTrace, to: xUnit})) {
                fields = updateSet(fields, [`fireflyData.${idx}.xUnit`], xUnit);
                fields = applyUnitConversion({fireflyData, data, inFields:fields, axisType:'x', newUnit:xUnit, traceNum:idx, axis:spectralAxis});
            }
            if (canUnitConv({from: yUnitTrace, to: yUnit})) {
                fields = updateSet(fields, [`fireflyData.${idx}.yUnit`], yUnit || yUnitTrace);
                fields = applyUnitConversion({fireflyData, data, inFields:fields, axisType:'y', newUnit:yUnit || yUnitTrace, traceNum:idx, axis:fluxAxis});
            }
        }
    });

    // when show/hide error changes for spectrum with 'order', apply it to other traces as well
    if (isSpectralOrder(chartId)) {
        const xShowError = fields[errorShowFieldKey(activeTrace, 'x')] || 'false';
        const yShowError = fields[errorShowFieldKey(activeTrace, 'y')] || 'false';
        range(data.length).forEach((idx) => {
            if (idx !== activeTrace) {
                const xShowErrorTrace = get({fireflyData}, errorShowFieldKey(idx, 'x')) || 'false';
                const yShowErrorTrace = get({fireflyData}, errorShowFieldKey(idx, 'y')) || 'false';
                if (xShowError !== xShowErrorTrace) {
                    fields = updateSet(fields, [errorShowFieldKey(idx, 'x')], xShowError);
                    fields = updateSet(fields, [`data.${idx}.error_x.visible`], toBoolean(xShowError));
                }
                if (yShowError !== yShowErrorTrace) {
                    fields = updateSet(fields, [errorShowFieldKey(idx, 'y')], yShowError);
                    fields = updateSet(fields, [`data.${idx}.error_y.visible`], toBoolean(yShowError));
                }
            }
        });
    }

    // Object.assign(changes, fields);
    submitChangesScatter({chartId, activeTrace, fields, tbl_id, renderTreeId});
}


function Units({activeTrace, value, axis, ...rest}) {

    const unitProp = axis === 'x' ? 'xUnit' : 'yUnit';
    const label = axis === 'x' ? 'Spectral axis units:' : 'Flux axis units:';
    const options = getUnitInfo(value)?.options;
    if (!options) return null;

    return (
        <ListBoxInputField fieldKey={`fireflyData.${activeTrace}.${unitProp}`} initialState={{value}} {...{label, options, ...rest}}/>
    );

}


export function spectrumInputs ({chartId, groupKey, fieldProps={}}) {

    const {activeTrace=0, fireflyData={}} = getChartData(chartId);

    return {
        Xunit: (props={}) => <Units {...{activeTrace, axis: 'x', value: get(fireflyData, `${activeTrace}.xUnit`), ...fieldProps, ...props}}/>,
        Yunit: (props={}) => <Units {...{activeTrace, axis: 'y', value: get(fireflyData, `${activeTrace}.yUnit`), ...fieldProps, ...props}}/>,
    };
}

