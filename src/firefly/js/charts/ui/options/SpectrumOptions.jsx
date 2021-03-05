import React from 'react';
import {get, range} from 'lodash';

import {getChartData} from '../../ChartsCntlr.js';
import {getTblById} from '../../../tables/TableUtil.js';
import {getSpectrumDM} from '../../../util/VOAnalyzer.js';
import {getUnitInfo, getUnitConvExpr} from '../../dataTypes/SpectrumUnitConversion.js';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {errorFieldKey, errorMinusFieldKey} from './Errors.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {fieldReducer, submitChangesScatter, scatterInputs, ScatterCommonOptions} from './ScatterOptions.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {updateSet} from '../../../util/WebUtil.js';
import {isFluxAxisOrder, getChartProps} from '../../ChartUtil.js';


const fieldProps = {labelWidth: 155, size: 15};


export function SpectrumOptions ({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const [activeTrace=0] = useStoreConnector(() => pActiveTrace ?? getChartData(chartId)?.activeTrace);

    groupKey = groupKey || `${chartId}-ffsed-${activeTrace}`;
    const {tbl_id, hasXerrors, hasYerrors, hasYMax, hasYMin, hasXMax, hasXMin} = getChartProps(chartId, ptbl_id, activeTrace);

    const {Xunit, Yunit} = spectrumInputs({activeTrace, tbl_id, chartId, groupKey, fieldProps});
    const {UseSpectrum, X, Xmax, Xmin, Y, Ymax, Ymin, Yerrors, Xerrors, Mode} = scatterInputs({activeTrace, tbl_id, chartId, groupKey, fieldProps});

    return(
        <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={false} reducerFunc={spectrumReducer({chartId, tbl_id, activeTrace, groupKey})}>

            {!isFluxAxisOrder(chartId) && <UseSpectrum/>}

            <div className='FieldGroup__vertical'>
                <X label='Spectral axis column(X):' readonly={true}/>
                {hasXerrors && <Xerrors labelWidth={85} readonly={true}/>}
                {hasXMax && <Xmax readonly={true}/>}
                {hasXMin && <Xmin readonly={true}/>}
                <Xunit/>
                <br/>
                <Y label='Flux axis column(Y):' readonly={true}/>
                {hasYerrors && <Yerrors labelWidth={85} readonly={true}/>}
                {hasYMax && <Ymax readonly={true} label = 'Flux axis upper limit column:'/>}
                {hasYMin && <Ymin readonly={true} label = 'Flux axis lower limit column:'/>}
                <Yunit/>
                <br/>
                <Mode/>
            </div>

            <ScatterCommonOptions{...{chartId, groupKey, fieldProps: {labelWidth: 60, size: 15}}}/>

        </FieldGroup>
    );
}


export function spectrumReducer({chartId, activeTrace, tbl_id, groupKey}) {
    const scatterReducer = fieldReducer({chartId, activeTrace, groupKey});
    const {fireflyData, data, spectralAxis, fluxAxis} = getChartProps(chartId, tbl_id, activeTrace);

    return (inFields, action) => {
        if (!inFields) {
            return ;
            // return {... scatterReducer(null), ...Object.fromEntries( Object.values(spectrum).map((f) => f.field))};
        }

        inFields = scatterReducer(inFields, action);

        const {payload:{fieldKey='', value=''}, type} = action;

        if (type === VALUE_CHANGE) {
            if (fieldKey === `fireflyData.${activeTrace}.xUnit`) {
                inFields = unitUpdated(fireflyData, data, inFields, 'x', value, activeTrace, spectralAxis, true);
            }
            if (fieldKey === `fireflyData.${activeTrace}.yUnit`) {
                inFields = unitUpdated(fireflyData, data, inFields, 'y', value, activeTrace, fluxAxis, true);
            }
        }
        return inFields;

    };
}

const unitUpdated = (fireflyData, data, inFields, axisType, value, traceNum, axis, isInput=false) => {

    const path = (p) => isInput ? [p, 'value'] : [p];

    const layoutAxis = axisType === 'x' ? 'xaxis' : 'yaxis';

    const label = getUnitInfo(value, axisType === 'x').label;
    inFields = updateSet(inFields, path(`layout.${layoutAxis}.title.text`), label);

    const colOrExpr = getUnitConvExpr({cname: axis.value, from: axis.unit, to: value});
    inFields = updateSet(inFields, path(`_tables.data.${traceNum}.${axisType}`), colOrExpr);

    const errCname = get(data, errorFieldKey(traceNum, axisType).replace('_tables.data.', ''));
    if (errCname) {
        const convVal = getUnitConvExpr({cname: axis.statErrHigh || axis.statError, from: axis.unit, to: value});
        inFields = updateSet(inFields, path(errorFieldKey(traceNum, axisType)), convVal);
    }

    const errMinusCname =  get(data, errorMinusFieldKey(traceNum, axisType).replace('_tables.data.', ''));
    if (errMinusCname) {
        const convVal = getUnitConvExpr({cname: axis.statErrLow, from: axis.unit, to: value});
        inFields = updateSet(inFields, path(errorMinusFieldKey(traceNum, axisType)), convVal);
    }

    const upperLimit =  get(fireflyData, `${traceNum}.${axisType}Max`);
    if (upperLimit) {
        const convVal = getUnitConvExpr({cname: axis.upperLimit, from: axis.unit, to: value});
        inFields = updateSet(inFields, path(`_tables.fireflyData.${traceNum}.${axisType}Max`), convVal);
    }
    const lowerLimit =  get(fireflyData, `${traceNum}.${axisType}Min`);
    if (lowerLimit) {
        const convVal = getUnitConvExpr({cname: axis.lowerLimit, from: axis.unit, to: value});
        inFields = updateSet(inFields, path(`_tables.fireflyData.${traceNum}.${axisType}Min`), convVal);
    }

    return inFields;
};



export function submitChangesSpectrum({chartId, activeTrace, fields, tbl_id, renderTreeId}) {

    // when unit changes, apple it to the other traces as well
    if (isFluxAxisOrder(chartId)) {
        const {data, fireflyData} = getChartData(chartId);
        const {spectralAxis={}, fluxAxis={}} = getSpectrumDM(getTblById(tbl_id)) || {};

        const cxUnit = get(fireflyData, `${activeTrace}.xUnit`);
        const nxUnit = fields[`fireflyData.${activeTrace}.xUnit`];
        const cyUnit = get(fireflyData, `${activeTrace}.yUnit`);
        const nyUnit = fields[`fireflyData.${activeTrace}.yUnit`];
        if ( cxUnit !== nxUnit || cyUnit !== nyUnit) {
            range(data.length).forEach((idx) => {
                if (idx !== activeTrace) {
                    fields = updateSet(fields, [`fireflyData.${idx}.xUnit`], nxUnit);
                    fields = updateSet(fields, [`fireflyData.${idx}.yUnit`], nyUnit);
                    fields = unitUpdated(data, fields, 'x', nxUnit, idx, spectralAxis);
                    fields = unitUpdated(data, fields, 'y', nyUnit, idx, fluxAxis);
                }
            });
        }
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

