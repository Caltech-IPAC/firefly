import {Box, Stack} from '@mui/joy';
import Enum from 'enum';
import React, {useContext, useEffect, useState} from 'react';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {ListBoxInputField} from 'firefly/ui/ListBoxInputField';
import {
    makeFieldErrorList, getPanelPrefix, LableSaptail, makePanelStatusUpdater,
    Width_Column, DebugObsCore, makeCollapsibleCheckHeader, SpatialWidth
} from 'firefly/ui/tap/TableSearchHelpers';
import {tapHelpId} from 'firefly/ui/tap/TapUtil';
import {ValidationField} from 'firefly/ui/ValidationField';
import PropTypes, {bool, string, object, shape, arrayOf} from 'prop-types';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {ConstraintContext} from './Constraints.js';
import {AutoCompleteInput} from 'firefly/ui/AutoCompleteInput';
import {omit} from 'lodash';
import {getDataServiceOption} from './DataServicesOptions';

const panelTitle = 'Observation Type and Source';
const panelValue = 'ObsCore';
const panelPrefix = getPanelPrefix(panelValue);


const multiConstraint = (value, columnName, siaName, quote, checkForNull) => {
    const multiConstraint = [];
    const siaConstraints = [];
    const valueList = value.split(',');
    valueList.forEach((value) => {
        if (checkForNull && value === 'null'){
            multiConstraint.push(`${columnName} IS NULL`);
        } else {
            multiConstraint.push(`${columnName} = ${quote}${value}${quote}`);
        }
        siaConstraints.push(`${siaName}=${value}`);
    });
    let adqlConstraint = multiConstraint.join(' OR ');
    if (multiConstraint.length > 1) adqlConstraint = `( ${adqlConstraint} )`;
    return { adqlConstraint, siaConstraints };
};


/**
 *
 * @param hasSubType
 * @param fldObj
 * @returns {InputConstraints}
 */
const makeConstraints = function(hasSubType, fldObj) {
    const errList= makeFieldErrorList();
    const adqlConstraintsAry = [];
    const siaConstraints = [];
    const siaConstraintErrors = [];

    const siaHasErrors = (value, siaErrorFields) => (siaErrorFields.some((v) => value.indexOf(v) >= 0));

    const {obsCoreCollection={}, obsCoreCalibrationLevel, obsCoreTypeSelection,
        obsCoreSubType, obsCoreInstrumentName, siaFacility}=fldObj;

    // const {obsCoreCollection, obsCoreCalibrationLevel, obsCoreTypeSelection, obsCoreSubType, obsCoreInstrumentName} = fields;
    errList.checkForError(obsCoreCollection);
    if (obsCoreCollection?.value?.length > 0) {
        const mcResult = multiConstraint(obsCoreCollection.value, 'obs_collection', 'COLLECTION', '\'');
        adqlConstraintsAry.push(mcResult.adqlConstraint);
        siaConstraints.push(...mcResult.siaConstraints);
    }

    errList.checkForError(obsCoreCalibrationLevel);
    if (obsCoreCalibrationLevel?.value) {
        const mcResult = multiConstraint(obsCoreCalibrationLevel.value, 'calib_level', 'CALIB', '');
        adqlConstraintsAry.push(mcResult.adqlConstraint);
        siaConstraints.push(...mcResult.siaConstraints);
    }


    if (siaFacility) {
        errList.checkForError(siaFacility);
        if (siaFacility?.value) {
            const mcResult = multiConstraint(siaFacility.value, 'facility', 'FACILITY', '');
            adqlConstraintsAry.push(mcResult.adqlConstraint);
            siaConstraints.push(...mcResult.siaConstraints);
        }
    }

    errList.checkForError(obsCoreTypeSelection);
    if (obsCoreTypeSelection?.value) {
        const mcResult = multiConstraint(obsCoreTypeSelection.value, 'dataproduct_type', 'DPTYPE', '\'', true);
        adqlConstraintsAry.push(mcResult.adqlConstraint);

        const siaErrorFields = ['visibility', 'event', 'null'];
        siaHasErrors(obsCoreTypeSelection.value, siaErrorFields)
            ? siaConstraintErrors.push(`values ${siaErrorFields} are not valid SIA DPTYPE options`)
            : siaConstraints.push(...mcResult.siaConstraints);
    }

    errList.checkForError(obsCoreInstrumentName);
    if (obsCoreInstrumentName?.value?.length) {
        const mcResult = multiConstraint(obsCoreInstrumentName.value, 'instrument_name', 'INSTRUMENT', '\'', true);
        adqlConstraintsAry.push(mcResult.adqlConstraint);
        siaHasErrors(obsCoreInstrumentName.value, ['null'])
            ? siaConstraintErrors.push('null is not a valid SIA INSTRUMENT option')
            : siaConstraints.push(...mcResult.siaConstraints);
    }

    if (hasSubType){
        errList.checkForError(obsCoreSubType);
        if (obsCoreSubType?.value?.length > 0) {
            const mcResult = multiConstraint(obsCoreSubType.value, 'dataproduct_subtype', '', '\'');
            adqlConstraintsAry.push(mcResult.adqlConstraint);
            siaConstraintErrors.push('Not able to translate dataproduct_subtype to SIAV2 query');
        }
    }

    if (!obsCoreCollection?.value && !obsCoreCalibrationLevel?.value && !siaFacility?.value &&
        !obsCoreInstrumentName?.value && !obsCoreSubType?.value && !obsCoreTypeSelection?.value) {
        errList.addError('at least one field must be populated');
    }

    const errAry= errList.getErrors();
    return { valid: errAry.length===0, errAry, adqlConstraintsAry, siaConstraints};
};

const ObsCoreTypeOptions = new Enum({
    'Image': 'image',
    'Cube': 'cube',
    'Spectrum': 'spectrum',
    'SED': 'sed',
    'Timeseries': 'timeseries',
    'Visibility': 'visibility',
    'Event': 'event',
    'Measurements': 'measurements',
    '(null)': 'null',
});


const getCalibrationOptions= (obsCoreCalibrationLevelOptions, obsCoreMetadataModel={}, useSIAv2) => {
    if (useSIAv2 && obsCoreMetadataModel.tableData?.data?.some((r) => r[0]==='calib')) {
        return obsCoreMetadataModel.tableData.data
            .filter( (r) => r[0]==='calib')
            .map( (r) => ({value:r[1], label:`${r[2]||r[1]} (${r[1]})`, title:r[2]}) );
    }

    const defCalLabels= [
        'Raw instrumental data',
        'Instrumental data in standard format (FITS, VOTable)',
        'Calibrated, science-ready data',
        'Enhanced data products',
        'Analysis data products',
    ];
    return defCalLabels.map((dl,idx) => {
        const label= obsCoreCalibrationLevelOptions.level?.[idx]?.title ?
            obsCoreCalibrationLevelOptions.level?.[idx]?.title + ` (${idx})` : idx+'';
        return ( {value: idx+'', label, title: obsCoreCalibrationLevelOptions.level?.[idx]?.title || dl});
    });
};


const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;
const fldListAry= ['obsCoreCalibrationLevel', 'obsCoreTypeSelection', 'obsCoreSubType', 'obsCoreInstrumentName', 'obsCoreCollection', 'siaFacility'];


export function ObsCoreSearch({sx, cols, obsCoreMetadataModel, serviceId,
                                  useCalibrationLevel=true, useProductType=true, useFacility=true,
                                  useSubtype= true,
                                  useInstrumentName=true, useCollection=true,
                                  initArgs={}, useSIAv2, slotProps={}}) {
    const {urlApi={}}= initArgs;
    const {setConstraintFragment}= useContext(ConstraintContext);
    const {makeFldObj}= useContext(FieldGroupCtx);
    const obsCoreCollectionOptions =  getDataServiceOption('obsCoreCollection',serviceId,{});
    const obsCoreCalibrationLevelOptions =  getDataServiceOption('obsCoreCalibrationLevel',serviceId,{});
    const obsCoreSubTypeOptions =  getDataServiceOption('obsCoreSubType',serviceId,{});
    const obsCoreInstrumentNameOptions =  getDataServiceOption('obsCoreInstrumentName',serviceId,{});

    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useFieldGroupWatch(fldListAry,
        (fldValAry,isInit) => {
            if (!isInit && fldValAry.some( (v) => v)) {
                checkHeaderCtl.setPanelActive(true);
            }
        });


    const calibrationOptions = getCalibrationOptions(obsCoreCalibrationLevelOptions, obsCoreMetadataModel, useSIAv2);

    const typeOptions = () => {
        return ObsCoreTypeOptions.enums.reduce((p, enumItem) => {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    const hasSubType = useSubtype && Boolean(cols && cols.findIndex((v) => v.name === 'dataproduct_subtype') !== -1);

    useEffect(() => {
        updatePanelStatus(makeConstraints(hasSubType,makeFldObj(fldListAry)), constraintResult, setConstraintResult,useSIAv2);
    });

    useEffect(() => {
        setConstraintFragment?.(panelPrefix, constraintResult);
        return () => setConstraintFragment?.(panelPrefix, '');
    }, [constraintResult]);

    const hasFacility= obsCoreMetadataModel?.tableData?.data?.some( (row) => row[0]==='facility');

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''} sx={sx}
                                initialStateOpen={false} initialStateChecked={false}>
            <Stack {...{width: SpatialWidth, mt: 1, ...slotProps?.innerStack }}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    <Stack {...{direction:'column', sx:{'& .ff-Input':{width: 300}}, spacing:1}}>
                        {useCalibrationLevel &&
                            <CheckboxGroupInputField fieldKey='obsCoreCalibrationLevel'
                                                     options={calibrationOptions}
                                                     tooltip={obsCoreCalibrationLevelOptions.tooltip || 'Select ObsCore Calibration Level (calibration_level)'}
                                                     label='Calibration Level'
                                                     multiple={true}
                                                     initialState={{value: urlApi.obsCoreCalibrationLevel || ''}}
                            />
                        }
                        {useCalibrationLevel && obsCoreCalibrationLevelOptions.helptext &&
                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                <i>{obsCoreCalibrationLevelOptions.helptext}</i>
                            </div>
                        }
                        <Stack direction='row' flexWrap='wrap'>
                            <Box pr={2} pt={1}>
                                {useSIAv2 ?
                                    useProductType && <ObsCoreInputField fieldKey='obsCoreTypeSelection'
                                                       tooltip='Select ObsCore Data Product Type'
                                                       placeholder={'Any'}
                                                       label='Data Product Type'
                                                       initialState={{ value: urlApi.obsCoreCollection || '' }}
                                                       columnName='dataproduct_type'
                                                       obsCoreMetadataModel={obsCoreMetadataModel}
                                    />
                                    :
                                    <ListBoxInputField fieldKey='obsCoreTypeSelection'
                                                       tooltip='Select ObsCore Data Product Type'
                                                       label='Data Product Type'
                                                       orientation='vertical'
                                                       placeholder={'<None Selected>'}
                                                       initialState={{value: urlApi.obsCoreTypeSelection || 'image'}}
                                                       options={typeOptions()}
                                                       multiple={true}
                                    />
                                }
                            </Box>

                            {hasFacility && useFacility &&
                                <Box pt={1}>
                                    <ObsCoreInputField fieldKey='siaFacility'
                                                       tooltip={'Select Facility'}
                                                       placeholder={'Any'}
                                                       label='Facility'
                                                       initialState={{ value: urlApi.obsCoreCollection || '' }}
                                                       columnName='facility'
                                                       obsCoreMetadataModel={obsCoreMetadataModel}
                                    />
                                </Box>

                            }
                        </Stack>
                            <Stack direction='row' flexWrap='wrap'>
                                {useInstrumentName &&
                                    <Stack pt={1} pr={2}>
                                        <ObsCoreInputField fieldKey='obsCoreInstrumentName'
                                                           tooltip={obsCoreInstrumentNameOptions.tooltip || 'Select ObsCore Instrument Name'}
                                                           label='Instrument Name'
                                                           initialState={{ value: urlApi.obsCoreInstrumentName || '' }}
                                                           placeholder={obsCoreInstrumentNameOptions.placeholder ?? 'Any'}
                                                           columnName='instrument_name'
                                                           obsCoreMetadataModel={obsCoreMetadataModel}
                                        />
                                        {obsCoreInstrumentNameOptions.helptext &&
                                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                                <i>{obsCoreInstrumentNameOptions.helptext}</i>
                                            </div>
                                        }
                                    </Stack>
                                }
                                {useCollection &&
                                    <Stack pt={1} >
                                        <ObsCoreInputField fieldKey='obsCoreCollection'
                                                           tooltip={obsCoreCollectionOptions.tooltip || 'Select ObsCore Collection Name'}
                                                           placeholder={obsCoreCollectionOptions.placeholder ?? 'Any'}
                                                           label='Collection'
                                                           initialState={{value: urlApi.obsCoreCollection || ''}}
                                                           columnName='obs_collection'
                                                           obsCoreMetadataModel={obsCoreMetadataModel}
                                        />
                                        {obsCoreCollectionOptions.helptext &&
                                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                                <i>{obsCoreCollectionOptions.helptext}</i>
                                            </div>
                                        }
                                    </Stack>
                                }
                            </Stack>
                    </Stack>
                    {hasSubType && <div style={{marginTop: 5}}>
                        <ObsCoreInputField fieldKey='obsCoreSubType'
                                           tooltip={obsCoreSubTypeOptions.tooltip || 'Select ObsCore Dataproduct Subtype Name'}
                                           placeholder={obsCoreSubTypeOptions.placeholder}
                                           label='Data Product Subtype:'
                                           initialState={{ value: urlApi.obsCoreSubType || '' }}
                                           columnName='dataproduct_subtype'
                                           obsCoreMetadataModel={obsCoreMetadataModel}
                        />
                        {obsCoreSubTypeOptions.helptext &&
                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                <i>{obsCoreSubTypeOptions.helptext}</i>
                            </div>
                        }
                    </div>}
                    <DebugObsCore {...{constraintResult, includeSia: true}}/>
                </ForceFieldGroupValid>
            </Stack>
        </CollapsibleCheckHeader>
    );
}

ObsCoreSearch.propTypes = {
    cols: arrayOf(PropTypes.shape({ name: string, units: string, type: string, desc: string })),
    useSIAv2: bool,
    initArgs: object,
    fields: object,
    sx: object,
    useCalibrationLevel: bool,
    useProductType: bool,
    useFacility: bool,
    useInstrumentName: bool,
    useCollection: bool,
    obsCoreMetadataModel: object,
    serviceId: string,
    slotProps: shape({
        innerStack: shape({
            sx: object
        }),
    })
};


const ObsCoreInputField = ({obsCoreMetadataModel, columnName, fieldKey, placeholder:inPlaceHolder, ...props}) => {

    const value= useFieldGroupValue(fieldKey)[0]();
    const loading = obsCoreMetadataModel?.loading ?? false;
    const options = (obsCoreMetadataModel?.tableData?.data ?? [])
        .filter((row) => row?.[0] === columnName).map((row) => row?.[1]);

    const fieldProps = omit(props, 'obsCoreMetadataModel', 'columnName');

    return (loading || (options && options?.length > 0))
        ? <AutoCompleteInput orientation='vertical'
                             fieldKey={fieldKey}
                             multiple={true}
                             placeholder={value ? undefined : inPlaceHolder}
                             options={labelValuePairs(options)}
                             loading={loading}
                             {...fieldProps}/>
        : <ValidationField inputWidth={Width_Column}
                           fieldKey={fieldKey}
                           placeholder={inPlaceHolder}
                           inputStyle={{overflow: 'auto', height: 16}}
                           {...fieldProps}/>;
};


const labelValuePairs = (options) => options.map((opt) => opt?.value
    ? opt
    : (opt===null ? {label: 'null', value: 'null'} : {label: opt, value: opt}));
