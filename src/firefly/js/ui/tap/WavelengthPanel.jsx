import {Divider, Stack, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {floatValidator, maximumPositiveFloatValidator, minimumPositiveFloatValidator} from '../../util/Validate.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {makeAdqlQueryRangeFragment, ConstraintContext, siaQueryRange} from './Constraints.js';
import {
    DebugObsCore, getPanelPrefix, getTapObsCoreOptions, LeftInSearch, makeCollapsibleCheckHeader,
    makeFieldErrorList,
    makePanelStatusUpdater, SmallFloatNumericWidth, SpatialWidth,
} from './TableSearchHelpers.jsx';
import {tapHelpId} from './TapUtil.js';

const panelTitle = 'Spectral Coverage';
const panelValue = 'Wavelength';
const panelPrefix = getPanelPrefix(panelValue);

function getExponent(units) {
    switch (units) {
        case 'nm': return 'e-9';
        case 'angstrom': return 'e-10';
        case 'um': return 'e-6';
        default: return '';
    }
}


function makeWavelengthConstraints(wavelengthSelection, rangeType, filterDefinitions, fldObj) {
    const errList= makeFieldErrorList();
    const siaConstraints= [];
    const adqlConstraintsAry = [];

    const {obsCoreWavelengthContains:wlContains, obsCoreWavelengthMinRange:wlMinRange,
        obsCoreWavelengthMaxRange:wlMaxRange, obsCoreWavelengthUnits:wlUnits}= fldObj;


    // pull out the fields we care about
    if (wavelengthSelection === 'filter') {
        const rangeList = [];
        filterDefinitions.forEach((filterDefinition) => {
            const fieldKey = 'filter' + filterDefinition.name;
            const field = fldObj[fieldKey];
            errList.checkForError(field);
            if (field?.value?.length) {
                // it's valid but we do this so we can get a field validity later
                const values = field.value.split(',');
                values.forEach((value) => {
                    // field values are in nanometers, but service expects meters
                    // We parse value as int, then
                    const iValue = parseInt(value);
                    const fValueString = `${iValue}e-9`;
                    rangeList.push([fValueString, fValueString]);
                });
            }
        });
        if (rangeList.length) {
            adqlConstraintsAry.push(makeAdqlQueryRangeFragment('em_min', 'em_max', rangeList, true));
            siaConstraints.push(...siaQueryRange('BAND', rangeList));
        } else {
            // Need at least one field to be non-empty
            errList.addError('at least one filter must be checked');
        }
    } else if (wavelengthSelection === 'numerical') {
        const exponent= getExponent(wlUnits?.value);
        if (rangeType === 'contains') {
            errList.checkForError(wlContains);
            if (wlContains?.valid) {
                const range = wlContains.value;
                if (range) {
                    const rangeList = [[`${range}${exponent}`, `${range}${exponent}`]];
                    adqlConstraintsAry.push(makeAdqlQueryRangeFragment('em_min', 'em_max', rangeList, true));
                    siaConstraints.push(...siaQueryRange('BAND', rangeList));
                }
                else {
                    errList.addError('Wavelength empty');
                }
            }
        }
        if (rangeType === 'overlaps') {
            errList.checkForError(wlMinRange);
            errList.checkForError(wlMaxRange);
            const anyHasValue = wlMinRange?.value || wlMaxRange?.value;
            if (anyHasValue) {
                const minValue = wlMinRange?.value?.length === 0 ? '-Inf' : wlMinRange?.value ?? '-Inf';
                const maxValue = wlMaxRange?.value?.length === 0 ? '+Inf' : wlMaxRange?.value ?? '+Inf';
                const lowerValue = minValue === '-Inf' ? minValue : `${minValue}${exponent}`;
                const upperValue = maxValue === '+Inf' ? maxValue : `${maxValue}${exponent}`;
                const rangeList = [[lowerValue, upperValue]];
                if (!lowerValue.endsWith('Inf') && !upperValue.endsWith('Inf') && Number(lowerValue) > Number(upperValue)) {
                    errList.addError('the max wavelength is smaller than the min wavelength');
                } else {
                    adqlConstraintsAry.push(makeAdqlQueryRangeFragment('em_min', 'em_max', rangeList));
                    siaConstraints.push(...siaQueryRange('BAND', rangeList));
                }
            } else {
                errList.addError('at least one field must be populated');
            }
        }
    }
    const errAry= errList.getErrors();
    return { valid: errAry.length===0, errAry, adqlConstraintsAry, siaConstraints};

}

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;
const fldKeys= ['obsCoreWavelengthContains', 'obsCoreWavelengthMinRange','obsCoreWavelengthSelectionType',
                'obsCoreWavelengthRangeType', 'obsCoreWavelengthMaxRange', 'obsCoreWavelengthUnits'];

export function ObsCoreWavelengthSearch({initArgs, serviceLabel, slotProps,useSIAv2}) {
    const filterDefinitions = getTapObsCoreOptions(serviceLabel).filterDefinitions ?? [];
    const fdDefsKeys= filterDefinitions.length ? filterDefinitions.map((fd) =>'filter' +fd.name ) : [];

    const {getVal,makeFldObj}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldKeys,...fdDefsKeys, ...collapsibleCheckHeaderKeys]); // force rerender on any change


    const rangeType= getVal('obsCoreWavelengthRangeType');
    const selectionType= getVal('obsCoreWavelengthSelectionType') ?? 'numerical';
    const hasFilters = filterDefinitions?.length > 0;
    const useNumerical = !hasFilters || selectionType === 'numerical';
    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useEffect(() => {
        const fldObj= makeFldObj([...fdDefsKeys, ...fldKeys]);
        const constraints= makeWavelengthConstraints(selectionType,rangeType, filterDefinitions, fldObj);
        updatePanelStatus(constraints, constraintResult, setConstraintResult,useSIAv2);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    useFieldGroupWatch([...fdDefsKeys,
    // 'obsCoreWavelengthContains', 'obsCoreWavelengthMinRange', 'obsCoreWavelengthMaxRange', 'obsCoreWavelengthUnits' ],
            'obsCoreWavelengthContains', 'obsCoreWavelengthMinRange', 'obsCoreWavelengthMaxRange' ],
        (valAry,isInit) => {
            !isInit && valAry.some((v)=>v) && checkHeaderCtl.setPanelActive(true);
        });

    const units= (
        <Stack direction='row' alignItems='center'>
            <Divider orientation='vertical' />
            <ListBoxInputField fieldKey='obsCoreWavelengthUnits'
                               options={ [
                                   {label: 'microns', value: 'um'},
                                   {label: 'nanometers', value: 'nm'},
                                   {label: 'angstroms', value: 'angstrom'},
                               ]}
                               slotProps={{ input: {
                                       variant:'plain',
                                       sx:{minHeight:'unset'}
                                   } }}
                               initialState={{ value: initArgs?.urlApi?.obsCoreWavelengthUnits || 'nm' }}
                               multiple={false}
                               {...slotProps?.obsCoreWavelengthUnits} />
        </Stack>
    );



    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <Stack {...{spacing: 2, width: SpatialWidth, justifyContent: 'flex-start'}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    {hasFilters && <RadioGroupInputField
                        fieldKey={'obsCoreWavelengthSelectionType'}
                        options={[{label: 'By Filter Bands', value: 'filter'}, {label: 'By Wavelength', value: 'numerical'}]}
                        orientation='horizontal'
                        label={'Query Type:'}
                        {...slotProps?.obsCoreWavelengthSelectionType}
                    />}
                    {hasFilters && selectionType === 'filter' &&
                        <Stack spacing={1}>
                            <Typography>Require coverage at the approximate center of these filters:</Typography>
                            <Stack spacing={.5} style={{marginLeft: LeftInSearch}}>
                                {filterDefinitions.map((filterDefinition) => {
                                    return (
                                        <CheckboxGroupInputField
                                            key={'filter' + filterDefinition.name + 'Key'}
                                            fieldKey={'filter' + filterDefinition.name}
                                            options={filterDefinition.options}
                                            alignment='horizontal'
                                            label={filterDefinition.name}
                                            {...slotProps?.obsCoreFilterDefinitions}
                                        />);
                                })}
                            </Stack>
                        </Stack>
                    }
                    {useNumerical &&
                        <Stack spacing={1}>
                            <div style={{display: 'flex'}}>
                                <ListBoxInputField fieldKey='obsCoreWavelengthRangeType'
                                                   options={[
                                                       {label: 'contains', value: 'contains'},
                                                       {label: 'overlaps', value: 'overlaps'},
                                                   ]}
                                                   initialState={{value: initArgs?.urlApi?.obsCoreWavelengthRangeType || 'contains'}}
                                                   label='Select observations whose wavelength coverage'
                                                   orientation='vertical'
                                                   multiple={false}
                                                   {...slotProps?.obsCoreWavelengthRangeType}
                                />
                            </div>
                            {rangeType === 'contains' &&
                                <div style={{display: 'flex'}}>
                                    <ValidationField fieldKey='obsCoreWavelengthContains'
                                                     size={SmallFloatNumericWidth}
                                                     inputStyle={{overflow: 'auto', height: 16}}
                                                     placeholder='enter wavelength'
                                                     sx={{'& .MuiInput-root': {'paddingInlineEnd': 0,}}}
                                                     validator={floatValidator(0, 100e15, 'Wavelength')}
                                                     endDecorator={units}
                                                     initialState={{value: initArgs?.urlApi?.obsCoreWavelengthContains || ''}}/>
                                </div>
                            }
                            {rangeType === 'overlaps' &&
                                <Stack direction='row' spacing={1} alignItems='center'>
                                    <ValidationField {...{
                                        fieldKey: 'obsCoreWavelengthMinRange',
                                        sx: {'& .MuiInput-root': {'width': 100}},
                                        validator: minimumPositiveFloatValidator('Min Wavelength'),
                                        placeholder: '-Inf',
                                        initialState: {value: initArgs?.urlApi?.obsCoreWavelengthMinRange},
                                    }}/>
                                    <Typography level='body-md'>to</Typography>
                                    <ValidationField {...{
                                        fieldKey: 'obsCoreWavelengthMaxRange',
                                        sx: {'& .MuiInput-root': {'width': 100}},
                                        validator: maximumPositiveFloatValidator('Max Wavelength'),
                                        placeholder: '+Inf',
                                        initialState: {value: initArgs?.urlApi?.obsCoreWavelengthMaxRange}
                                    }}/>
                                    {units}
                                </Stack>
                            }
                        </Stack>
                    }
                    <DebugObsCore {...{constraintResult}}/>
                </ForceFieldGroupValid>
            </Stack>
        </CollapsibleCheckHeader>
    );
}

ObsCoreWavelengthSearch.propTypes = {
    initArgs: PropTypes.object,
    serviceLabel: PropTypes.string,
    useSIAv2: PropTypes.bool,
    slotProps: PropTypes.shape({
        obsCoreWavelengthUnits: PropTypes.object,
        obsCoreFilterDefinitions: PropTypes.object,
        obsCoreWavelengthSelectionType: PropTypes.object,
        obsCoreWavelengthRangeType: PropTypes.object,
    })
};
