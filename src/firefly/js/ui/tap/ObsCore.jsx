import Enum from 'enum';
import React, {useContext, useEffect, useState} from 'react';
import {getAppOptions} from 'firefly/api/ApiUtil';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {ListBoxInputField} from 'firefly/ui/ListBoxInputField';
import {
    makeFieldErrorList, getPanelPrefix, LableSaptail, makePanelStatusUpdater,
    SpatialWidth, Width_Column, DebugObsCore, makeCollapsibleCheckHeader
} from 'firefly/ui/tap/TableSearchHelpers';
import {tapHelpId} from 'firefly/ui/tap/TapUtil';
import {ValidationField} from 'firefly/ui/ValidationField';
import PropTypes from 'prop-types';
import {ColsShape} from '../../charts/ui/ColumnOrExpression';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {useFieldGroupRerender, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {ConstraintContext} from './Constraints.js';

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

    const {obsCoreCollection, obsCoreCalibrationLevel, obsCoreTypeSelection,
        obsCoreSubType, obsCoreInstrumentName}=fldObj;

    // const {obsCoreCollection, obsCoreCalibrationLevel, obsCoreTypeSelection, obsCoreSubType, obsCoreInstrumentName} = fields;
    errList.checkForError(obsCoreCollection);
    if (obsCoreCollection.value?.length > 0) {
        adqlConstraintsAry.push(`obs_collection = '${obsCoreCollection.value}'`);
        siaConstraints.push(`COLLECTION=${obsCoreCollection.value}`);
    }
    errList.checkForError(obsCoreCalibrationLevel);
    if (obsCoreCalibrationLevel.value) {
        const mcResult = multiConstraint(obsCoreCalibrationLevel.value, 'calib_level', 'CALIB', '');
        adqlConstraintsAry.push(mcResult.adqlConstraint);
        siaConstraints.push(...mcResult.siaConstraints);
    }
    errList.checkForError(obsCoreTypeSelection);
    if (obsCoreTypeSelection.value) {
        const mcResult = multiConstraint(obsCoreTypeSelection.value, 'dataproduct_type', 'DPTYPE', '\'', true);
        adqlConstraintsAry.push(mcResult.adqlConstraint);
        const siaErrorFields = ['visibility', 'event', 'null'];
        if (siaErrorFields.some((v) => obsCoreTypeSelection.value.indexOf(v) >= 0)){
            siaConstraintErrors.push(`values ${siaErrorFields} not valid SIA DPTYPE options`);
        } else {
            siaConstraints.push(...mcResult.siaConstraints);
        }
    }
    errList.checkForError(obsCoreInstrumentName);
    if (obsCoreInstrumentName.value?.length) {
        const mcResult = multiConstraint(obsCoreInstrumentName.value, 'instrument_name', 'INSTRUMENT', '\'');
        adqlConstraintsAry.push(mcResult.adqlConstraint);
        siaConstraints.push(...mcResult.siaConstraints);
    }
    if (hasSubType){
        errList.checkForError(obsCoreSubType);
        if (obsCoreSubType.value?.length > 0) {
            adqlConstraintsAry.push(`dataproduct_subtype = '${obsCoreSubType.value}'`);
            siaConstraintErrors.push('Not able to translate dataproduct_subtype to SIAV2 query');
        }
    }

    if (!obsCoreCollection?.value && !obsCoreCalibrationLevel?.value &&
        !obsCoreInstrumentName?.value && !obsCoreSubType?.value && !obsCoreTypeSelection?.value) {
        errList.addError('at least one field must be populated');
    }

    const errAry= errList.getErrors();
    return { valid: errAry.length===0, errAry, adqlConstraintsAry, siaConstraints, siaConstraintErrors };
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

const defCalLabels= [
    'Raw instrumental data',
    'Instrumental data in standard format (FITS, VOTable)',
    'Calibrated, science-ready data',
    'Enhanced data products',
    'Analysis data products',
];

const getCalibrationOptions= (obsCoreCalibrationLevelOptions) => defCalLabels.map((dl,idx) =>
    ( {value: idx+'', label: idx+'', title: obsCoreCalibrationLevelOptions.level?.[idx]?.title || dl}));


const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(panelValue));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;
const fldListAry= ['obsCoreCalibrationLevel', 'obsCoreTypeSelection', 'obsCoreInstrumentName', 'obsCoreCollection'];


export function ObsCoreSearch({cols, initArgs={}}) {
    const {urlApi={}}= initArgs;
    const {setConstraintFragment}= useContext(ConstraintContext);
    const tapObsCoreOps= getAppOptions().tapObsCore ?? {};
    const {makeFldObj}= useContext(FieldGroupCtx);
    const obsCoreCollectionOptions =  tapObsCoreOps.obsCoreCollection ?? {};
    const obsCoreCalibrationLevelOptions =  tapObsCoreOps.obsCoreCalibrationLevel ?? {};
    const obsCoreSubTypeOptions =  tapObsCoreOps.obsCoreSubType ?? {};
    const obsCoreInstrumentNameOptions =  tapObsCoreOps.obsCoreInstrumentName ?? {};

    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), panelValue);

    useFieldGroupWatch(fldListAry,
        (fldValAry,isInit) => {
            if (!isInit && fldValAry.some( (v) => v)) {
                checkHeaderCtl.setPanelActive(true);
            }
        });


    const calibrationOptions = getCalibrationOptions(obsCoreCalibrationLevelOptions);

    const typeOptions = () => {
        return ObsCoreTypeOptions.enums.reduce((p, enumItem) => {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    const hasSubType = cols.findIndex((v) => v.name === 'dataproduct_subtype') !== -1;

    useEffect(() => {
        updatePanelStatus(makeConstraints(hasSubType,makeFldObj(fldListAry)), constraintResult, setConstraintResult);
    });

    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''}
                                initialStateOpen={true} initialStateChecked={true}>
            <div style={{
                display: 'flex', flexDirection: 'column', flexWrap: 'no-wrap', width: SpatialWidth, marginTop: 5 }}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    <div style={{display: 'flex', flexDirection: 'column', marginTop: '5px'}}>
                        <CheckboxGroupInputField fieldKey='obsCoreCalibrationLevel'
                                                 options={calibrationOptions}
                                                 tooltip={obsCoreCalibrationLevelOptions.tooltip || 'Select ObsCore Calibration Level (calibration_level)'}
                                                 label={'Calibration Level:'}
                                                 labelWidth={LableSaptail}
                                                 multiple={true}
                                                 initialState={{value: urlApi.obsCoreCalibrationLevel || ''}}
                        />
                        {obsCoreCalibrationLevelOptions.helptext &&
                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                <i>{obsCoreCalibrationLevelOptions.helptext}</i>
                            </div>
                        }
                    </div>
                    <div style={{display: 'flex', flexDirection: 'column'}}>
                        <ListBoxInputField fieldKey='obsCoreTypeSelection'
                                           tooltip='Select ObsCore Data Product Type'
                                           label={'Data Product Type:'}
                                           labelWidth={LableSaptail}
                                           initialState={{value: urlApi.obsCoreTypeSelection || 'image'}}
                                           options={typeOptions()}
                                           wrapperStyle={{marginRight: 15, padding: '8px 0 5px 0', display: 'flex'}}
                                           multiple={true}
                        />
                    </div>
                    <div style={{marginTop: 5}}>
                        <ValidationField fieldKey='obsCoreInstrumentName'
                                         inputWidth={Width_Column}
                                         inputStyle={{overflow: 'auto', height: 16}}
                                         tooltip={obsCoreInstrumentNameOptions.tooltip || 'Select ObsCore Instrument Name'}
                                         placeholder={obsCoreInstrumentNameOptions.placeholder}
                                         label={'Instrument Name:'}
                                         labelWidth={LableSaptail}
                                         initialState={{ value: urlApi.obsCoreInstrumentName || '' }}
                        />
                        {obsCoreInstrumentNameOptions.helptext &&
                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                <i>{obsCoreInstrumentNameOptions.helptext}</i>
                            </div>
                        }
                    </div>
                    <div style={{marginTop: 5}}>
                        <ValidationField fieldKey='obsCoreCollection'
                                         inputWidth={Width_Column}
                                         inputStyle={{overflow: 'auto', height: 16}}
                                         tooltip={obsCoreCollectionOptions.tooltip || 'Select ObsCore Collection Name'}
                                         placeholder={obsCoreCollectionOptions.placeholder}
                                         label={'Collection:'}
                                         labelWidth={LableSaptail}
                                         initialState={{ value: urlApi.obsCoreCollection || ''
                                         }}
                        />
                        {obsCoreCollectionOptions.helptext &&
                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                <i>{obsCoreCollectionOptions.helptext}</i>
                            </div>
                        }
                    </div>
                    {hasSubType && <div style={{marginTop: 5}}>
                        <ValidationField fieldKey='obsCoreSubType'
                                         inputWidth={Width_Column}
                                         inputStyle={{overflow: 'auto', height: 16}}
                                         tooltip={obsCoreSubTypeOptions.tooltip || 'Select ObsCore Dataproduct Subtype Name'}
                                         placeholder={obsCoreSubTypeOptions.placeholder}
                                         label='Data Product Subtype:'
                                         labelWidth={LableSaptail}
                                         initialState={{ value: urlApi.obsCoreSubType || '' }}
                        />
                        {obsCoreSubTypeOptions.helptext &&
                            <div style={{marginLeft: LableSaptail, marginTop: 5, padding: 2}}>
                                <i>{obsCoreSubTypeOptions.helptext}</i>
                            </div>
                        }
                    </div>}
                    <DebugObsCore {...{constraintResult}}/>
                </ForceFieldGroupValid>
            </div>
        </CollapsibleCheckHeader>
    );
}

ObsCoreSearch.propTypes = {
    cols: ColsShape,
    initArgs: PropTypes.object,
    fields: PropTypes.object
};

