import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {floatValidator, maximumPositiveFloatValidator, minimumPositiveFloatValidator} from '../../util/Validate.js';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {makeAdqlQueryRangeFragment, ConstraintContext, siaQueryRange} from './Constraints.js';
import {
    DebugObsCore, getPanelPrefix, LableSaptail, LeftInSearch, makeCollapsibleCheckHeader, makeFieldErrorList,
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
    const siaConstraintErrors= [];
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
        const exponent= getExponent(wlUnits.value);
        if (rangeType === 'contains') {
            errList.checkForError(wlContains);
            if (wlContains?.valid) {
                const range = wlContains.value;
                const rangeList = [[`${range}${exponent}`, `${range}${exponent}`]];
                adqlConstraintsAry.push(makeAdqlQueryRangeFragment('em_min', 'em_max', rangeList, true));
                siaConstraints.push(...siaQueryRange('BAND', rangeList));
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
    return { valid: errAry.length===0, errAry, adqlConstraintsAry, siaConstraints, siaConstraintErrors };

}

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;
const fldKeys= ['obsCoreWavelengthContains', 'obsCoreWavelengthMinRange','obsCoreWavelengthSelectionType',
                'obsCoreWavelengthMaxRange', 'obsCoreWavelengthUnits'];

export function ObsCoreWavelengthSearch({initArgs}) {
    const filterDefinitions = getAppOptions()?.tapObsCore?.filterDefinitions ?? [];
    const fdDefsKeys= filterDefinitions.length ? filterDefinitions.map((fd) =>'filter' +fd.name ) : [];

    const {getVal,makeFldObj}= useContext(FieldGroupCtx);
    const {setConstraintFragment}= useContext(ConstraintContext);
    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldKeys,...fdDefsKeys, ...collapsibleCheckHeaderKeys]); // force rerender on any change


    const rangeType= getVal('obsCoreWavelengthRangeType');
    const selectionType= getVal('obsCoreWavelengthSelectionType');
    const hasFilters = filterDefinitions?.length > 0;
    const useNumerical = !hasFilters || selectionType === 'numerical';
    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useEffect(() => {
        const fldObj= makeFldObj([...fdDefsKeys, ...fldKeys]);
        const constraints= makeWavelengthConstraints(selectionType,rangeType, filterDefinitions, fldObj);
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    useFieldGroupWatch([...fdDefsKeys,
    'obsCoreWavelengthContains', 'obsCoreWavelengthMinRange', 'obsCoreWavelengthMaxRange', 'obsCoreWavelengthUnits' ],
        (valAry,isInit) => {
            !isInit && valAry.some((v)=>v) && checkHeaderCtl.setPanelActive(true);
        });

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} initialStateOpen={false}>
            <div style={{display: 'flex', flexDirection: 'column', width: SpatialWidth, justifyContent: 'flex-start'}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    {hasFilters && <RadioGroupInputField
                        fieldKey={'obsCoreWavelengthSelectionType'}
                        options={[{label: 'By Filter Bands', value: 'filter'}, {label: 'By Wavelength', value: 'numerical'}]}
                        alignment={'horizontal'}
                        wrapperStyle={{marginTop: '10px'}}
                        label={'Query Type:'}
                        labelWidth={LableSaptail}
                    />}
                    {hasFilters && selectionType === 'filter' &&
                        <div style={{marginTop: 10}}>
                            <div style={{paddingTop: 4}}>Require coverage at the approximate center of these filters:</div>
                            <div style={{marginLeft: LeftInSearch}}>
                                {filterDefinitions.map((filterDefinition) => {
                                    return (
                                        <CheckboxGroupInputField
                                            key={'filter' + filterDefinition.name + 'Key'}
                                            fieldKey={'filter' + filterDefinition.name}
                                            options={filterDefinition.options}
                                            alignment='horizontal'
                                            wrapperStyle={{whiteSpace: 'normal', marginTop: '5px'}}
                                            label={filterDefinition.name}
                                            labelWidth={85}
                                        />);
                                })
                                }
                            </div>
                        </div>
                    }
                    {useNumerical &&
                        <div style={{marginTop: '10px'}}>
                            <div style={{display: 'flex'}}>
                                <ListBoxInputField fieldKey='obsCoreWavelengthRangeType'
                                                   options={ [
                                                       {label: 'contains', value: 'contains'},
                                                       {label: 'overlaps', value: 'overlaps'},
                                                   ]}
                                                   initialState={{ value: initArgs?.urlApi?.obsCoreWavelengthRangeType || 'contains' }}
                                                   label='Select observations whose wavelength coverage'
                                                   labelWidth={236}
                                                   multiple={false} />
                            </div>
                            <div style={{display: 'inline-flex', marginTop: '10px', marginLeft: LeftInSearch}}>
                                {rangeType === 'contains' &&
                                    <div style={{display: 'flex'}}>
                                        <ValidationField fieldKey='obsCoreWavelengthContains'
                                                         size={SmallFloatNumericWidth}
                                                         inputStyle={{overflow: 'auto', height: 16}}
                                                         validator={floatValidator(0, 100e15, 'Wavelength')}
                                                         initialState={{value: initArgs?.urlApi?.obsCoreWavelengthContains || ''}} />
                                    </div>
                                }
                                {rangeType === 'overlaps' &&
                                    <div style={{display: 'flex'}}>
                                        <ValidationField fieldKey='obsCoreWavelengthMinRange'
                                                         size={SmallFloatNumericWidth}
                                                         inputStyle={{overflow: 'auto', height: 16}}
                                                         validator={minimumPositiveFloatValidator('Min Wavelength')}
                                                         placeholder={'-Inf'}
                                                         initialState={{value: initArgs?.urlApi?.obsCoreWavelengthMinRange}} />
                                        <div style={{display: 'flex', marginTop: 5, marginRight: '16px', paddingRight: '3px'}}>to</div>
                                        <ValidationField fieldKey='obsCoreWavelengthMaxRange'
                                                         size={SmallFloatNumericWidth}
                                                         inputStyle={{overflow: 'auto', height: 16}}
                                                         validator={maximumPositiveFloatValidator('Max Wavelength')}
                                                         placeholder={'+Inf'}
                                                         initialState={{value: initArgs?.urlApi?.obsCoreWavelengthMaxRange}} />
                                    </div>
                                }
                                <ListBoxInputField fieldKey='obsCoreWavelengthUnits'
                                                   options={ [
                                                       {label: 'microns', value: 'um'},
                                                       {label: 'nanometers', value: 'nm'},
                                                       {label: 'angstroms', value: 'angstrom'},
                                                   ]}
                                                   initialState={{ value: initArgs?.urlApi?.obsCoreWavelengthUnits || 'nm' }}
                                                   multiple={false} />
                            </div>
                        </div>
                    }
                    <DebugObsCore {...{constraintResult}}/>
                </ForceFieldGroupValid>
            </div>
        </CollapsibleCheckHeader>
    );
}

ObsCoreWavelengthSearch.propTypes = {
    initArgs: PropTypes.object
};
