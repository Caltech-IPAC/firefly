import {Stack, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {makeAdqlQueryRangeFragment, ConstraintContext, siaQueryRange} from './Constraints.js';
import {getDataServiceOption} from './DataServicesOptions';
import {
    DebugObsCore, getPanelPrefix, LeftInSearch, makeCollapsibleCheckHeader,
    makeFieldErrorList,
    makePanelStatusUpdater, SpatialWidth,
} from './TableSearchHelpers.jsx';
import {tapHelpId} from './TapUtil.js';
import {
    BASE_UNIT,
    convertWavelengthStr,
    WavelengthInputField,
    WavelengthRangeInput
} from 'firefly/ui/WavelengthInputField';

const panelTitle = 'Spectral Coverage';
const panelValue = 'Wavelength';
const panelPrefix = getPanelPrefix(panelValue);

const obsCoreWvlFieldKeys = {
    selectionType: 'obsCoreWavelengthSelectionType',
    rangeType: 'obsCoreWavelengthRangeType',
    wvlContains: 'obsCoreWavelengthContains',
    wvlMin: 'obsCoreWavelengthMinRange',
    wvlMax: 'obsCoreWavelengthMaxRange',
};

function makeWavelengthConstraints(filterDefinitions, fldObj) {
    const errList= makeFieldErrorList();
    const siaConstraints= [];
    const adqlConstraintsAry = [];

    const {[obsCoreWvlFieldKeys.selectionType]:wavelengthSelection, [obsCoreWvlFieldKeys.rangeType]:rangeType,
        [obsCoreWvlFieldKeys.wvlContains]:wlContains, [obsCoreWvlFieldKeys.wvlMin]:wlMinRange,
        [obsCoreWvlFieldKeys.wvlMax]:wlMaxRange} = fldObj;


    // pull out the fields we care about
    if (wavelengthSelection?.value === 'filter') {
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
    } else { //wavelengthSelection fld is undefined (because of no filters), or 'numerical' (radio option)
        if (rangeType?.value === 'contains') {
            errList.checkForError(wlContains);
            if (wlContains?.valid) {
                // value is represented in BASE_UNIT but 'em_min' and 'em_max' in query fragment are in m (meters) so convert to m
                const rangeBound = convertWavelengthStr(wlContains.value, BASE_UNIT, 'm');
                if (rangeBound) {
                    const rangeList = [[rangeBound, rangeBound]];
                    adqlConstraintsAry.push(makeAdqlQueryRangeFragment('em_min', 'em_max', rangeList, true));
                    siaConstraints.push(...siaQueryRange('BAND', rangeList));
                }
                else {
                    errList.addError('Wavelength empty');
                }
            }
        }
        if (rangeType?.value === 'overlaps') {
            errList.checkForError(wlMinRange);
            errList.checkForError(wlMaxRange);
            const anyHasValue = wlMinRange?.value || wlMaxRange?.value;
            if (anyHasValue) {
                // value is represented in BASE_UNIT but 'em_min' and 'em_max' in query fragment are in m (meters) so convert to m
                const lowerValue = convertWavelengthStr(wlMinRange?.value, BASE_UNIT, 'm') || '-Inf';
                const upperValue = convertWavelengthStr(wlMaxRange?.value, BASE_UNIT, 'm') || 'Inf';
                const rangeList = [[lowerValue, upperValue]];
                adqlConstraintsAry.push(makeAdqlQueryRangeFragment('em_min', 'em_max', rangeList));
                siaConstraints.push(...siaQueryRange('BAND', rangeList));
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

export function WavelengthOptions({initArgs, fieldKeys, filterDefinitionsLabel, filterDefinitions, fixedRangeType, slotProps }) {
    const [getSelectionType,] = useFieldGroupValue(fieldKeys.selectionType);
    const [getRangeType,] = useFieldGroupValue(fieldKeys.rangeType);

    const hasFilters = filterDefinitions?.length > 0;
    const useNumerical = !hasFilters || getSelectionType() === 'numerical';
    const useRangeContains = fixedRangeType ? fixedRangeType === 'contains' : getRangeType() === 'contains';
    const useRangeOverlaps = fixedRangeType ? fixedRangeType === 'overlaps' : getRangeType() === 'overlaps';

    return (
        <Stack spacing={2}>
            {hasFilters && (
                <RadioGroupInputField
                    fieldKey={fieldKeys.selectionType}
                    options={[{ label: 'By Filter Bands', value: 'filter' }, { label: 'By Wavelength', value: 'numerical' }]}
                    orientation='horizontal'
                    label={'Query Type:'}
                    {...slotProps?.selectionType}
                />
            )}

            {hasFilters && getSelectionType() === 'filter' && (
                <Stack spacing={1} {...slotProps?.filterBandsWvlOptions}>
                    {filterDefinitionsLabel && (
                        <Typography level='title-sm'>{filterDefinitionsLabel}</Typography>
                    )}
                    <Stack spacing={.5} style={{ marginLeft: filterDefinitionsLabel ? LeftInSearch : 0 }}>
                        {filterDefinitions.map((filterDefinition) => (
                            <CheckboxGroupInputField
                                key={'filter' + filterDefinition.name + 'Key'}
                                fieldKey={'filter' + filterDefinition.name}
                                options={filterDefinition.options}
                                alignment='horizontal'
                                label={filterDefinition.name}
                                {...slotProps?.filterDefOptionsGroup}
                            />
                        ))}
                    </Stack>
                </Stack>
            )}

            {useNumerical && (
                <Stack spacing={1} {...slotProps?.numericalWvlOptions}>
                    {!fixedRangeType && (
                        <div style={{display: 'flex'}}>
                            <ListBoxInputField
                                fieldKey={fieldKeys.rangeType}
                                options={[
                                    {label: 'contains', value: 'contains'},
                                    {label: 'overlaps', value: 'overlaps'},
                                ]}
                                initialState={{value: initArgs?.urlApi?.rangeType || 'contains'}}
                                label='Select observations whose wavelength coverage'
                                orientation='vertical'
                                multiple={false}
                                {...slotProps?.rangeType}
                            />
                        </div>
                    )}
                    {useRangeContains && (
                        <div style={{ display: 'flex' }}>
                            <WavelengthInputField fieldKey={fieldKeys.wvlContains}
                                                  inputStyle={{ overflow: 'auto', height: 16 }}
                                                  placeholder='enter wavelength'
                                                  initialState={{
                                                      value: initArgs?.urlApi?.wvlContains || '',
                                                      unit: initArgs?.urlApi?.wvlUnits || 'nm'
                                                  }}
                                                  {...slotProps?.wvlContains} />
                        </div>
                    )}
                    {useRangeOverlaps && (
                        <WavelengthRangeInput minFieldKey={fieldKeys.wvlMin} maxFieldKey={fieldKeys.wvlMax}
                                              slotProps={{
                                                  wvlMin: {
                                                      initialState: {
                                                          value: initArgs?.urlApi?.wvlMin || '',
                                                          unit: initArgs?.urlApi?.wvlUnits || 'nm'
                                                      },
                                                      ...slotProps?.wvlMin
                                                  },
                                                  wvlMax: {
                                                      initialState: {
                                                          value: initArgs?.urlApi?.wvlMax || '',
                                                          unit: initArgs?.urlApi?.wvlUnits || 'nm'
                                                      },
                                                      ...slotProps?.wvlMax
                                                  }
                                              }}/>
                    )}
                </Stack>
            )}
        </Stack>
    );
}

WavelengthOptions.propTypes = {
    initArgs: PropTypes.object,
    fieldKeys: PropTypes.shape({
        selectionType: PropTypes.string,
        rangeType: PropTypes.string,
        wvlContains: PropTypes.string,
        wvlMin: PropTypes.string,
        wvlMax: PropTypes.string,
    }).isRequired,
    filterDefinitionsLabel: PropTypes.string,
    filterDefinitions: PropTypes.arrayOf(PropTypes.shape({
        name: PropTypes.string,
        options: PropTypes.arrayOf(PropTypes.shape({ value: PropTypes.string, label: PropTypes.string }))
    })),
    fixedRangeType: PropTypes.oneOf(['contains', 'overlaps']),
    slotProps: PropTypes.shape({
        selectionType: PropTypes.object,
        rangeType: PropTypes.object,
        wvlContains: PropTypes.object,
        wvlMin: PropTypes.object,
        wvlMax: PropTypes.object,
        wvlUnits: PropTypes.object,
        filterDefOptionsGroup: PropTypes.object,
        numericalWvlOptions: PropTypes.object,
        filterBandsWvlOptions: PropTypes.object,
    })
};


export function ObsCoreWavelengthSearch({ initArgs, serviceId, slotProps, useSIAv2 }) {
    const filterDefinitions = getDataServiceOption('filterDefinitions', serviceId, []);
    const fdDefsKeys = filterDefinitions.length ? filterDefinitions.map((fd) => 'filter' + fd.name) : [];
    const fldKeys = Object.values(obsCoreWvlFieldKeys);

    const { makeFldObj } = useContext(FieldGroupCtx);
    const { setConstraintFragment } = useContext(ConstraintContext);
    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldKeys, ...fdDefsKeys, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const updatePanelStatus = makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useEffect(() => {
        const fldObj = makeFldObj([...fdDefsKeys, ...fldKeys]);
        const constraints = makeWavelengthConstraints(filterDefinitions, fldObj);
        updatePanelStatus(constraints, constraintResult, setConstraintResult, useSIAv2);
    });

    useEffect(() => {
        setConstraintFragment?.(panelPrefix, constraintResult);
        return () => setConstraintFragment?.(panelPrefix, '');
    }, [constraintResult]);

    useFieldGroupWatch([...fdDefsKeys, obsCoreWvlFieldKeys.wvlContains, obsCoreWvlFieldKeys.wvlMin, obsCoreWvlFieldKeys.wvlMax],
        (valAry, isInit) => {
        !isInit && valAry.some((v) => v) && checkHeaderCtl.setPanelActive(true);
    });

    return (
        <CollapsibleCheckHeader
            title={panelTitle}
            helpID={tapHelpId(panelPrefix)}
            message={constraintResult?.simpleError ?? ''}
            initialStateOpen={false}
        >
            <Stack {...{ spacing: 2, width: SpatialWidth, justifyContent: 'flex-start', ...slotProps?.root }}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    <WavelengthOptions {...{initArgs,
                        fieldKeys: obsCoreWvlFieldKeys,
                        filterDefinitionsLabel: 'Require coverage at the approximate center of these filters:',
                        filterDefinitions,
                        slotProps: slotProps?.wavelengthOptions}}/>
                    <DebugObsCore {...{ constraintResult }} />
                </ForceFieldGroupValid>
            </Stack>
        </CollapsibleCheckHeader>
    );
}

ObsCoreWavelengthSearch.propTypes = {
    initArgs: PropTypes.object,
    serviceId: PropTypes.string,
    useSIAv2: PropTypes.bool,
    slotProps: PropTypes.shape({
        root: PropTypes.object,
        wavelengthOptions: WavelengthOptions.propTypes.slotProps
    })
};
