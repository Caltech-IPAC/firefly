import {Divider, FormControl, FormLabel, Stack, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import Validate, {maximumPositiveFloatValidator, minimumPositiveFloatValidator} from '../../util/Validate.js';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {checkExposureTime, convertISOToMJD} from '../TimeUIUtil.js';
import {ValidationField} from '../ValidationField.jsx';
import {ConstraintContext, makeAdqlQueryRangeFragment, siaQueryRange} from './Constraints.js';
import {TimeRangePanel} from './TimeRangePanel.jsx';
import {
    DebugObsCore, getPanelPrefix, makeCollapsibleCheckHeader, makeFieldErrorList,
    makePanelStatusUpdater,
    SmallFloatNumericWidth
} from './TableSearchHelpers.jsx';
import {tapHelpId} from './TapUtil.js';

const START_EXP_GREATER_MSG= 'exposure time max must be greater than time min';
const ONE_POPULATED= 'at least one field must be populated';

const panelTitle = 'Timing';
const panelValue = 'Exposure';
const panelPrefix = getPanelPrefix(panelValue);
const exposureRangeOptions = [
    {label: 'Completed in the Last...', value: 'since' },
    {label: 'Overlapping specified range', value: 'range'}
];


function getSinceMill(sinceVal, ops) {
    const v = parseFloat(sinceVal);
    switch (ops) {
        case 'minutes': return v * 60 * 1000;
        case 'hours': return v * 60 * 60 * 1000;
        case 'days': return v * 24 * 60 * 60 * 1000;
        case 'years': return v * 365 * 24 * 60 * 60 * 1000;
        default: return 0;
    }
}


function checkSinceTimeInMjd(sinceVal, sinceOp) {
    const sinceMillis = getSinceMill(parseFloat(sinceVal), sinceOp);
    const sinceString = new Date(Date.now() - sinceMillis).toISOString();
    return convertISOToMJD(sinceString);
}


function checkExposureDuration(expLenMin, expLenMax) {
    const minValue = !expLenMin?.length ? '-Inf' : expLenMin ?? '-Inf';
    const maxValue = !expLenMax?.length ? '+Inf' : expLenMax ?? '+Inf';
    const minGreaterThanMax= !minValue.endsWith('Inf') && !maxValue.endsWith('Inf') && Number(minValue) > Number(maxValue);
    return {minValue, maxValue,rangeList:[[minValue, maxValue]],minGreaterThanMax};
}

/**
 * @param rangeType
 * @param fldObj
 * @returns {InputConstraints}
 */
function makeExposureConstraints(rangeType, fldObj) {
    const errList= makeFieldErrorList();
    const siaConstraints= [];
    const adqlConstraintsAry = [];

    const {exposureSinceValue:expSince,exposureLengthMin:expLenMinField, exposureLengthMax:expLenMaxField,
        exposureMin, exposureMax, exposureSinceOptions}= fldObj;

    const expLenMin = expLenMinField?.value;
    const expLenMax = expLenMaxField?.value;

    let seenValue = false;
    if (rangeType === 'range') {
        if (exposureMin?.value || exposureMax?.value) {
            const {minValue, maxValue}=  checkExposureTime(exposureMin,exposureMax);
            errList.checkForError(exposureMin);
            errList.checkForError(exposureMax);
            if (exposureMin?.valid && exposureMax?.valid) {
                const rangeList = [[minValue, maxValue]];
                adqlConstraintsAry.push(makeAdqlQueryRangeFragment('t_min', 't_max', rangeList, false));
                siaConstraints.push(...siaQueryRange('TIME', rangeList));
            }
            seenValue = true;
        }
    } else if (rangeType === 'since' && !isNaN(parseFloat(expSince?.value))) {
        errList.checkForError(expSince);
        if (expSince?.valid) {
            const rangeList = [[`${checkSinceTimeInMjd(expSince?.value, exposureSinceOptions?.value)}`, '+Inf']];
            adqlConstraintsAry.push(makeAdqlQueryRangeFragment('t_min', 't_max', rangeList));
            siaConstraints.push(...siaQueryRange('TIME', rangeList));
        }
        seenValue = true;
    }
    if (expLenMin || expLenMax) {
        const {rangeList, minGreaterThanMax}= checkExposureDuration(expLenMin,expLenMax);
        if (!minGreaterThanMax) {
            adqlConstraintsAry.push(makeAdqlQueryRangeFragment('t_exptime', 't_exptime', rangeList, true));
            siaConstraints.push(...siaQueryRange('EXPTIME', rangeList));
        }
        seenValue = true;
        errList.checkForError(expLenMinField);
        errList.checkForError(expLenMaxField);
    }
    if (!seenValue) errList.addError(ONE_POPULATED);

    const errAry= errList.getErrors();
    return { valid: errAry.length===0 && seenValue, errAry, adqlConstraintsAry, siaConstraints};

}


const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= ['exposureSinceValue', 'exposureLengthMin', 'exposureLengthMax',
            'exposureMin', 'exposureMax', 'exposureSinceOptions', 'exposureRangeType'];

export function ExposureDurationSearch({initArgs, slotProps,useSIAv2}) {
    const {getVal,makeFldObj}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys] ); // force rerender on any change

    const turnOnPanel= () => checkHeaderCtl.setPanelActive(true);

    const isRange= getVal('exposureRangeType') === 'range';
    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useEffect(() => {
        const constraints= makeExposureConstraints(getVal('exposureRangeType'), makeFldObj( fldListAry));
        updatePanelStatus(constraints, constraintResult, setConstraintResult,useSIAv2);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    },[constraintResult]);

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <div style={{marginTop: 5}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    <Stack direction='column' spacing={2}>
                        <ListBoxInputField
                            {...{fieldKey:'exposureRangeType', options: exposureRangeOptions,
                                label:'Time of Observation',
                                initialState:{value: initArgs?.urlApi?.exposureRangeType || 'since'},
                                ...slotProps?.exposureRangeType
                            }} />
                        {isRange
                            ? <TimeRangePanel {...{initArgs, turnOnPanel, panelActive:checkHeaderCtl.isPanelActive(),
                                fromTip:"'Exposure start from' time (t_min)",
                                toTip:"'Exposure end to' time (t_min)",
                                ...slotProps?.exposureTimeRange}}/>
                            : <ExposureSince {...{initArgs, turnOnPanel, panelActive:checkHeaderCtl.isPanelActive(),
                                ...slotProps?.exposureSince}} />
                        }
                        <ExposureLength {...{initArgs, turnOnPanel, panelActive:checkHeaderCtl.isPanelActive()}}/>
                        <DebugObsCore {...{constraintResult}}/>
                    </Stack>
                </ForceFieldGroupValid>
            </div>
        </CollapsibleCheckHeader>
    );
}

ExposureDurationSearch.propTypes = {
    initArgs: PropTypes.object,
    useSIAv2: PropTypes.bool,
    slotProps: PropTypes.shape({
        exposureRangeType: PropTypes.object,
        exposureTimeRange: PropTypes.object,
        exposureSince: PropTypes.object,
    })
};


function ExposureSince({initArgs, turnOnPanel}) {

    useFieldGroupWatch(['exposureSinceValue'], ([expSince],isInit) => expSince && !isInit && turnOnPanel());

    return (
        <Stack direction='row'>
            <ValidationField {...{
                placeholder:'Enter time',
                fieldKey:'exposureSinceValue', // FIXME: Introduce SinceValue or similar
                validator: (val) => Validate.isFloat('Exposure since',val),
                initialState: {value: initArgs?.urlApi?.exposureSinceValue || ''},
                sx:{'& .MuiInput-root':{ 'paddingInlineEnd': 0, }},
                endDecorator:
                    (
                        <Stack direction='row' alignItems='center'>
                            <Divider orientation='vertical' />
                            <ListBoxInputField
                                fieldKey={'exposureSinceOptions'} // FIXME: Introduce SinceOptions
                                options={[
                                    {label: 'Minutes', value: 'minutes'},
                                    {label: 'Hours', value: 'hours'},
                                    {label: 'Days', value: 'days'},
                                    {label: 'Years', value: 'years'}
                                ]}
                                slotProps={{ input: {
                                        variant:'plain',
                                        sx:{minHeight:'unset'}
                                    } }}
                                initialState={{value: initArgs?.urlApi?.exposureSinceOptions || 'hours'}}/>

                        </Stack>),
            }} />
        </Stack>
    );
}

function ExposureLength({initArgs, panelActive, turnOnPanel}) {
    const inputStyle= {overflow: 'auto', height: 16};

    const [getExposureLengthMin] = useFieldGroupValue('exposureLengthMin');
    const [getExposureLengthMax, setExposureLengthMax] = useFieldGroupValue('exposureLengthMax');

    const minMaxCheck= () => {
        const {minGreaterThanMax}=  checkExposureDuration(getExposureLengthMin(), getExposureLengthMax());
        if (minGreaterThanMax) {
            setExposureLengthMax(getExposureLengthMax(), {valid: false, message: START_EXP_GREATER_MSG});
        }
    };

    useFieldGroupWatch(['exposureLengthMin', 'exposureLengthMax'],
        ([expLenMin,expLenMax],isInit) => {
            minMaxCheck();
            if (!isInit && (expLenMin || expLenMax)) turnOnPanel();
        });

    useEffect(() => {
        if (panelActive) minMaxCheck();
    }, [panelActive]);


    return (
        <FormControl {...{orientation:'vertical'}}>
            <FormLabel>Exposure Duration</FormLabel>
            <Stack direction='row' spacing={1} alignItems='center'>
                <ValidationField {...{
                    fieldKey: 'exposureLengthMin',
                    size: SmallFloatNumericWidth,
                    tooltip: 'Cumulative shutter-open exposure duration in seconds',
                    sx:{'& .MuiInput-root':{'width': 100}},
                    validator: minimumPositiveFloatValidator('Minimum Exposure Length'),
                    placeholder:'-Inf',
                    initialState: {value: initArgs?.urlApi?.exposureLengthMin},
                }} />
                <Typography level='body-md'>to</Typography>
                <ValidationField {...{
                    fieldKey: 'exposureLengthMax',
                    size: SmallFloatNumericWidth,
                    sx:{'& .MuiInput-root':{'width': 100}},
                    inputStyle,
                    tooltip:'Cumulative shutter-open exposure must be less than this amount',
                    validator:maximumPositiveFloatValidator('Maximum Exposure Length'),
                    placeholder:'+Inf',
                    initialState: {value: initArgs?.urlApi?.exposureLengthMax}
                }}/>
                <Typography level='body-md'>seconds</Typography>
            </Stack>
        </FormControl>

    );
}