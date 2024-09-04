import React, {useCallback} from 'react';
import {get, range, isEqual} from 'lodash';
import {getSpectrumDM, REF_POS} from '../../../voAnalyzer/SpectrumDM.js';

import {getChartData} from '../../ChartsCntlr.js';
import {getTblById} from '../../../tables/TableUtil.js';
import {canUnitConv, getUnitInfo, getUnitConvExpr, getXLabel} from '../../dataTypes/SpectrumUnitConversion.js';

import {useStoreConnector} from '../../../ui/SimpleComponent.jsx';
import {FieldGroup} from '../../../ui/FieldGroup.jsx';
import {errorFieldKey, errorMinusFieldKey, errorShowFieldKey} from './Errors.jsx';
import {ListBoxInputField} from '../../../ui/ListBoxInputField.jsx';
import {fieldReducer, submitChangesScatter, ScatterCommonOptions, useScatterInputs} from './ScatterOptions.jsx';
import {VALUE_CHANGE} from '../../../fieldGroup/FieldGroupCntlr.js';
import {updateSet, toBoolean} from '../../../util/WebUtil.js';
import {isSpectralOrder, getChartProps} from '../../ChartUtil.js';
import {LayoutOptions, useBasicOptions} from './BasicOptions.jsx';
import {getSpectrumProps} from '../../dataTypes/FireflySpectrum.js';
import {getFieldVal, revalidateFields} from 'firefly/fieldGroup/FieldGroupUtils';
import {isFloat} from 'firefly/util/Validate';
import {ValidationField} from 'firefly/ui/ValidationField';
import {sprintf} from 'firefly/externalSource/sprintf';
import {RadioGroupInputField} from 'firefly/ui/RadioGroupInputField';
import {Box, Stack} from '@mui/joy';
import {CollapsibleGroup} from 'firefly/ui/panel/CollapsiblePanel';


export function SpectrumOptions ({activeTrace:pActiveTrace, tbl_id:ptbl_id, chartId, groupKey}) {

    const activeTrace = useStoreConnector(() => pActiveTrace ?? getChartData(chartId)?.activeTrace ?? 0);

    groupKey = groupKey || `${chartId}-ffsed-${activeTrace}`;

    const {tbl_id} = getChartProps(chartId, ptbl_id, activeTrace);
    const {xErrArray, yErrArray, xMax, xMin, yMax, yMin} = getSpectrumProps(tbl_id);

    const {Xunit, Yunit, SpectralFrame} = useSpectrumInputs({activeTrace, tbl_id, chartId, groupKey});
    const {UseSpectrum, X, Xmax, Xmin, Y, Ymax, Ymin, Yerrors, Xerrors, Mode} = useScatterInputs({activeTrace, tbl_id, chartId, groupKey});
    const {XaxisTitle, YaxisTitle} = useBasicOptions({activeTrace, tbl_id, chartId, groupKey});

    const reducerFunc = spectrumReducer({chartId, activeTrace, tbl_id});
    reducerFunc.ver = chartId+activeTrace+tbl_id;

    const labelWidth = '11rem';

    return(
        <FieldGroup groupKey={groupKey} validatorFunc={null} keepState={false} reducerFunc={reducerFunc}>
            <Stack spacing={3}>
                <Stack spacing={2} sx={{
                    '.MuiFormLabel-root': {width: labelWidth},
                    // '.MuiFormControl-root > *:not(.MuiFormLabel-root)': {width: `calc(100% - ${labelWidth})`} //TODO: whether to make inputs equally wide?
                }}>
                    {!isSpectralOrder(chartId) && <UseSpectrum/>}
                    <Stack spacing={1}>
                        <X label='Spectral axis column(X):' readonly={true} sx={{'.MuiInput-root': {width: '25rem'}}}/>
                        {xErrArray && <Xerrors readonly={true}/>}
                        {xMax && <Xmax readonly={true}/>}
                        {xMin && <Xmin readonly={true}/>}
                        <Xunit/>
                        <SpectralFrame labelWidth={labelWidth}/>
                    </Stack>
                    <Stack spacing={1}>
                        <Y label='Flux axis column(Y):' readonly={true}/>
                        {yErrArray && <Yerrors readonly={true}/>}
                        {yMax && <Ymax readonly={true} label = 'Flux axis upper limit column:'/>}
                        {yMin && <Ymin readonly={true} label = 'Flux axis lower limit column:'/>}
                        <Yunit/>
                    </Stack>
                    <Mode/>
                </Stack>
                <CollapsibleGroup>
                    <ScatterCommonOptions{...{activeTrace, tbl_id, chartId, groupKey}}/>
                    <LayoutOptions {...{activeTrace, tbl_id, chartId, groupKey}}
                                   XaxisTitle={() => <XaxisTitle readonly={true}/>}
                                   YaxisTitle={() => <YaxisTitle readonly={true}/>}
                    />
                </CollapsibleGroup>
            </Stack>
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

            if (Object.values(SFOptionFieldKeys(activeTrace)).includes(fieldKey)) {
                inFields = applyRedshiftCorrection({fireflyData, inFields, spectralAxis, activeTrace, isInput: true});
                inFields = updateSet(inFields, ['__xreset', 'value'], 'true');
            }
        }
        return inFields;

    };
}


const getRedshiftCorrectedExpr = ({cname, spectralFrame, sfOption, redshift=undefined}) => {
    const {refPos, redshift: customRedshift} = spectralFrame;
    const multiplyBy = refPos.toUpperCase() === REF_POS.CUSTOM ? ` * (1 + ${customRedshift ?? '0'})` : '';
    const divideBy = sfOption === 'rest' && redshift ? ` / (1 + ${redshift})` : '';
    const expr = customRedshift!==redshift ? `"%s"${multiplyBy}${divideBy}` : '"%s"';
    return sprintf(expr, cname);
};

const getCombinedExpr = (cname, redshiftCorrParams, unitConvParams) => {
    const redshiftCorrExp = getRedshiftCorrectedExpr({...redshiftCorrParams, cname});
    const unitConvExp = getUnitConvExpr({...unitConvParams, cname});

    const unitConvFactor = unitConvExp.split(`"${cname}"`)?.[1];
    return unitConvFactor ? `(${redshiftCorrExp})${unitConvFactor}` : redshiftCorrExp;
};


const getRedshiftInfo = (inFields, path, fireflyData, activeTrace) => {
    // reduce the 3 spectral frame fields to obtain the info needed for redshift correction expression and for axis label
    const [sfOption, redshiftOption, userSpecifiedRedshift] = Object.values(SFOptionFieldKeys(activeTrace))
        .map((fieldKey) => get(inFields, path(fieldKey)));

    let sfLabel = 'Observed Frame';
    let redshift, redshiftLabel='';

    if(sfOption==='rest') {
        sfLabel = 'Rest Frame';
        redshift = redshiftOption;
        redshiftLabel = getRedshiftOptions(fireflyData[activeTrace]).find((opt)=> opt.value===redshift)
            ?.label.replace(/ with confidence.*$/, ''); //don't need to show confidence in axis label

        if(redshiftOption==='userSpecified') {
            redshift = userSpecifiedRedshift;
            redshiftLabel = `Redshift = ${userSpecifiedRedshift}`;
        }
    }
    else if(sfOption!=='observed') sfLabel = `${sfOption} Spectral Frame`;

    return {sfOption, sfLabel, redshift, redshiftLabel};
};

const applyRedshiftCorrection = ({fireflyData, inFields, spectralAxis, activeTrace, isInput=false}) => {
    const path = (p) => isInput ? [p, 'value'] : [p];
    const {sfOption, sfLabel, redshift, redshiftLabel} = getRedshiftInfo(inFields, path, fireflyData, activeTrace);
    const {spectralFrame} = fireflyData[activeTrace];

    // take unit conversion into account too
    const xUnit = get(inFields, path(`fireflyData.${activeTrace}.xUnit`));
    const colOrExpr = getCombinedExpr(spectralAxis.value,
        {spectralFrame, sfOption, redshift},
        {from: spectralAxis.unit, to: xUnit});
    const xLabel = getXLabel(spectralAxis.value, xUnit, sfLabel, redshiftLabel);

    inFields = updateSet(inFields, path(`_tables.data.${activeTrace}.x`), colOrExpr);
    inFields = updateSet(inFields, path('layout.xaxis.title.text'), xLabel);
    return revalidateFields(inFields); //revalidate otherwise validation state of x would be stale
};


export const applyUnitConversion = ({fireflyData, data, inFields, axisType, newUnit, traceNum, axis, isInput=false}) => {
    const path = (p) => isInput ? [p, 'value'] : [p];
    const layoutAxis = axisType === 'x' ? 'xaxis' : 'yaxis';

    let label = getUnitInfo(newUnit, axis.value).label;
    let colOrExpr = getUnitConvExpr({cname: axis.value, from: axis.unit, to: newUnit});
    if (axisType==='x') {
        // take redshift correction into account too
        const {sfOption, sfLabel, redshift, redshiftLabel} = getRedshiftInfo(inFields, path, fireflyData, traceNum);
        colOrExpr = getCombinedExpr(axis.value,
            {spectralFrame: fireflyData[traceNum].spectralFrame, sfOption, redshift},
            {from: axis.unit, to: newUnit});
        label = getXLabel(axis.value, newUnit, sfLabel, redshiftLabel);
    }
    inFields = updateSet(inFields, path(`layout.${layoutAxis}.title.text`), label);
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
    const {data, fireflyData} = getChartData(chartId);
    const {spectralAxis={}, fluxAxis={}} = getSpectrumDM(getTblById(tbl_id)) || {};

    // get units and spectral frame options from the fields of active trace
    const xUnit = fields[`fireflyData.${activeTrace}.xUnit`];
    const yUnit = fields[`fireflyData.${activeTrace}.yUnit`]; // undefined if no field for yUnit
    const sfOptionFields = Object.fromEntries(Object.entries(SFOptionFieldKeys(activeTrace))
        .map(([subKey, fieldKey])=>[subKey, fields[fieldKey]]));

    // when units or spectral frame options change, apply it to the other/inactive traces as well
    range(data.length).forEach((idx) => {
        if (idx !== activeTrace) {
            // get units and spectral frame options from the chart data of inactive trace
            const xUnitTrace = fireflyData?.[idx]?.xUnit;
            const yUnitTrace = fireflyData?.[idx]?.yUnit;
            const sfOptionTrace = Object.fromEntries(Object.keys(SFOptionFieldKeys(idx))
                .map((key)=>[key, fireflyData?.[idx]?.spectralFrameOption?.[key]]));

            // set the fields of inactive trace same as that of active trace, if they don't match
            if (!isEqual(sfOptionTrace, sfOptionFields) || (xUnitTrace!==xUnit && canUnitConv({from: xUnitTrace, to: xUnit}))) {
                fields = updateSet(fields, [`fireflyData.${idx}.xUnit`], xUnit);
                Object.entries(SFOptionFieldKeys(idx)).forEach(([subKey, fieldKey])=>{
                    fields = updateSet(fields, [fieldKey], sfOptionFields[subKey]);
                });

                // applying unit conversion on X axis, will also apply redshift correction so no need to call it separately
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

    // propagate all of the above field changes to change the state (i.e. chart data in store)
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

/*
 * This function returns a collection of components using `useCallback`, ensuring they are not recreated between re-renders.
 * To modify this behavior, you can set the `deps` parameter accordingly.
 */
export const useSpectrumInputs = ({chartId, groupKey}, deps=[]) => {

    const {activeTrace=0, fireflyData={}} = getChartData(chartId);

    return {
        Xunit: useCallback((props={}) => <Units {...{activeTrace, axis: 'x', value: get(fireflyData, `${activeTrace}.xUnit`), ...props}}/>, deps),
        Yunit: useCallback((props={}) => <Units {...{activeTrace, axis: 'y', value: get(fireflyData, `${activeTrace}.yUnit`), ...props}}/>, deps),
        SpectralFrame: useCallback((props={}) => {
            const allProps = {label: 'Spectral Frame:', ...props};
            const sfRefPos = fireflyData[activeTrace].spectralFrame.refPos.toUpperCase();
            return Object.values(REF_POS).includes(sfRefPos) //only show options when TOPOCENTER or CUSTOM
                ? <SpectralFrameOptions groupKey={groupKey} activeTrace={activeTrace} refPos={sfRefPos} fireflyData={fireflyData} {...allProps}/>
                : <ValidationField fieldKey={SFOptionFieldKeys(activeTrace).value} initialState={{value: sfRefPos}} readonly={true} {...allProps}/>;
        }, deps),
    };
};

const SFOptionFieldKeys = (activeTrace) => {
    const baseKey = `fireflyData.${activeTrace}.spectralFrameOption`;
    return Object.fromEntries(['value', 'redshift', 'userSpecified'].map((subKey)=>[subKey, `${baseKey}.${subKey}`]));
};

function getRedshiftOptions({target, derivedRedshift, spectralFrame}){ //TODO: memoize it?
    const refPos = spectralFrame.refPos.toUpperCase();
    let options = [];

    if (target?.redshift) {
        const targetNameStr = target?.name ? ` for Target ${target.name}` : '';
        options.push({
            label: `Target Redshift = ${target.redshift}${targetNameStr}`,
            value: target.redshift.toString() //TODO: format with precision=8?
        });
    }

    if (derivedRedshift?.value) {
        const statErrorStr = derivedRedshift?.statError ? ` ± ${derivedRedshift.statError}` : '';
        const confidenceStr = derivedRedshift?.confidence ? ` with confidence = ${derivedRedshift.confidence}` : '';
        options.push({
            label: `Derived Redshift = ${derivedRedshift.value}${statErrorStr}${confidenceStr}`,
            value: derivedRedshift.value.toString()
        });
    }

    options.push({
        label: 'Enter Redshift: ',
        value: 'userSpecified'
    });

    if (refPos === REF_POS.CUSTOM) {
        const customRedshiftOption = spectralFrame?.redshift
            ? {label: `Custom Redshift = ${spectralFrame.redshift}`, value: spectralFrame.redshift}
            : {label: 'Unknown Custom Redshift', value: '0'}; //TODO: corner case: need a way to deal with math if not '0'
        options = [customRedshiftOption, ...options];
    }

    return options;
}

function SpectralFrameOptions ({groupKey, activeTrace, refPos, fireflyData, labelWidth, ...props}) {
    const {spectralFrameOption} = fireflyData[activeTrace];
    const spectralFrameOptions = [{label: 'Observed Frame', value: 'observed'}, {label: 'Rest Frame', value: 'rest'}];
    const redshiftOptions = getRedshiftOptions(fireflyData[activeTrace]);
    const defaultSFOption = refPos===REF_POS.TOPOCENTER ? 'observed' : 'rest';

    const isRestFrameOption = useStoreConnector(()=>
        getFieldVal(groupKey, SFOptionFieldKeys(activeTrace).value)==='rest');

    const isUserSpecifiedOption = useStoreConnector(()=>
        getFieldVal(groupKey, SFOptionFieldKeys(activeTrace).redshift)==='userSpecified');

    return (
        <Stack spacing={0.5}>
            <ListBoxInputField fieldKey={SFOptionFieldKeys(activeTrace).value}
                               options={spectralFrameOptions}
                               initialState={{value: spectralFrameOption?.value ?? defaultSFOption}}
                               {...props}/>
            <Box sx={{
                position: 'relative',
                display: isRestFrameOption ? 'block' : 'none' //to keep the contained fields mounted because they are needed in spectrumReducer
                }}>
                    <RadioGroupInputField fieldKey={SFOptionFieldKeys(activeTrace).redshift}
                                          options={redshiftOptions}
                                          initialState={{value: spectralFrameOption?.redshift}} //will select 1st option if undefined
                                          orientation={'vertical'}
                                          sx={{ml: `calc(${labelWidth} + 1rem)`}} //add 1rem to offset right margin of labels
                    />
                    <ValidationField fieldKey={SFOptionFieldKeys(activeTrace).userSpecified}
                                     sx={{position: 'absolute', bottom: 0, left: `calc(${labelWidth} + 9rem)`, width: '9rem', zIndex: 1}} //to align it with the last radio group option
                                     initialState={{value: spectralFrameOption?.userSpecified ?? '0'}}
                                     validator={(val) => isFloat('Redshift', val)}
                                     readonly={!isUserSpecifiedOption}
                                     tooltip='Rest Frame Redshift'/>
            </Box>
        </Stack>
    );
}
