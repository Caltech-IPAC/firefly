import React,  {useState, useEffect} from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import {get} from 'lodash';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';
import {HeaderFont, ISO, MJD, tapHelpId} from 'firefly/ui/tap/TapUtil';
import {ValidationField} from 'firefly/ui/ValidationField';
import {ListBoxInputField} from 'firefly/ui/ListBoxInputField';
import {
    Header,
    FROM,
    TO,
    skey,
    LeftInSearch,
    LabelWidth,
    LableSaptail,
    SpatialWidth,
    Width_Column,
    Width_Time_Wrapper,
    changeDatePickerOpenStatusNew,
    onChangeTimeMode,
    getTimeInfo,
    checkField,
    updatePanelFields, isPanelChecked, getPanelPrefix, SmallFloatNumericWidth, SpatialLableSaptail
} from 'firefly/ui/tap/TableSearchHelpers';
import {ColsShape} from '../../charts/ui/ColumnOrExpression';
import {convertISOToMJD, validateDateTime, validateMJD} from 'firefly/ui/DateTimePickerField';
import {TimePanel} from 'firefly/ui/TimePanel';
import {RadioGroupInputField} from 'firefly/ui/RadioGroupInputField';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {getAppOptions} from 'firefly/api/ApiUtil';
import {floatValidator, minimumPositiveFloatValidator, maximumPositiveFloatValidator} from 'firefly/util/Validate';
import toFloat from 'validator/es/lib/toFloat';
import {dispatchValueChange} from 'firefly/fieldGroup/FieldGroupCntlr';
import {Logger} from 'firefly/util/Logger';

const logger = Logger('ObsCore');

const fakeValidator = () => ({valid: true, message: '' });

function timeValidator(fieldKey) {
    return (val) => {
        const timeMode = getFieldVal(skey, fieldKey) || ISO;
        return (timeMode === MJD) ? validateMJD(val) : validateDateTime(val);
    };
}

export function ObsCoreSearch({cols, groupKey, fields, useConstraintReducer}) {
    const panelTitle = 'Observation Type and Source';
    const panelValue = 'ObsCore';
    const panelPrefix = getPanelPrefix(panelValue);

    const [message, setMesage] = useState();

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelValue;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
        });
    }, []);

    const DEBUG_OBSCORE = getAppOptions().tapObsCore?.debug ?? false;
    const obsCoreCollectionOptions =  getAppOptions().tapObsCore?.obsCoreCollection ?? {};
    const obsCoreCalibrationLevelOptions =  getAppOptions().tapObsCore?.obsCoreCalibrationLevel ?? {};
    const obsCoreSubTypeOptions =  getAppOptions().tapObsCore?.obsCoreSubType ?? {};
    const obsCoreInstrumentNameOptions =  getAppOptions().tapObsCore?.obsCoreInstrumentName ?? {};

    const calibrationOptions = [
        {value: '0', label: '0', title: obsCoreCalibrationLevelOptions.level?.[0]?.title || 'Raw instrumental data'},
        {value: '1', label: '1', title: obsCoreCalibrationLevelOptions.level?.[1]?.title ||'Instrumental data in standard format (FITS, VOTable)'},
        {value: '2', label: '2', title: obsCoreCalibrationLevelOptions.level?.[2]?.title || 'Calibrated, science-ready data'},
        {value: '3', label: '3', title: obsCoreCalibrationLevelOptions.level?.[3]?.title || 'Enhanced data products'},
        {value: '4', label: '4', title: obsCoreCalibrationLevelOptions.level?.[4]?.title || 'Analysis data products'},
    ];

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

    const typeOptions = () => {
        return ObsCoreTypeOptions.enums.reduce((p, enumItem) => {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    const hasSubType = cols.findIndex((v) => v.name === 'dataproduct_subtype') !== -1;

    const makeConstraints = function(fields, fieldsValidity) {
        const adqlConstraints = [];
        const siaConstraints = [];
        const siaConstraintErrors = [];

        const checkObsCoreField = (key, nullAllowed) => {
            return checkField(key, fields, true, fieldsValidity);
        };

        const multiConstraint = (value, columnName, siaName, quote, checkForNull) => {
            const multiConstraint = [];
            const _siaConstraints = [];
            const valueList = value.split(',');
            valueList.forEach((value) => {
                if (checkForNull && value === 'null'){
                    multiConstraint.push(`${columnName} IS NULL`);
                } else {
                    multiConstraint.push(`${columnName} = ${quote}${value}${quote}`);
                }
                _siaConstraints.push(`${siaName}=${value}`);
            });
            let adqlConstraint = multiConstraint.join(' OR ');
            if (multiConstraint.length > 1){
                adqlConstraint = `( ${adqlConstraint} )`;
            }
            return {
                adqlConstraint,
                siaConstraints: _siaConstraints
            };
        };

        // pull out the fields we care about
        const {obsCoreCollection, obsCoreCalibrationLevel, obsCoreTypeSelection, obsCoreSubType, obsCoreInstrumentName} = fields;
        checkObsCoreField('obsCoreCollection', true);
        if (obsCoreCollection.value?.length > 0) {
            adqlConstraints.push(`obs_collection = '${obsCoreCollection.value}'`);
            siaConstraints.push(`COLLECTION=${obsCoreCollection.value}`);
        }
        checkObsCoreField('obsCoreCalibrationLevel', true);
        if (obsCoreCalibrationLevel.value) {
            const mcResult = multiConstraint(obsCoreCalibrationLevel.value, 'calib_level', 'CALIB', '');
            adqlConstraints.push(mcResult.adqlConstraint);
            siaConstraints.push(...mcResult.siaConstraints);
        }
        checkObsCoreField('obsCoreTypeSelection', true);
        if (obsCoreTypeSelection.value !== '') {
            const mcResult = multiConstraint(obsCoreTypeSelection.value, 'dataproduct_type', 'DPTYPE', '\'', true);
            adqlConstraints.push(mcResult.adqlConstraint);
            const siaErrorFields = ['visibility', 'event', 'null'];
            if (siaErrorFields.some((v) => obsCoreTypeSelection.value.indexOf(v) >= 0)){
                siaConstraintErrors.push(`values ${siaErrorFields} not valid SIA DPTYPE options`);
            } else {
                siaConstraints.push(...mcResult.siaConstraints);
            }
        }
        checkObsCoreField('obsCoreInstrumentName', true);
        if (obsCoreInstrumentName.value?.length > 0) {
            const mcResult = multiConstraint(obsCoreInstrumentName.value, 'instrument_name', 'INSTRUMENT', '\'');
            adqlConstraints.push(mcResult.adqlConstraint);
            siaConstraints.push(...mcResult.siaConstraints);
        }
        if (hasSubType){
            checkObsCoreField('obsCoreSubType', true);
            if (obsCoreSubType.value?.length > 0) {
                adqlConstraints.push(`dataproduct_subtype = '${obsCoreSubType.value}'`);
                siaConstraintErrors.push('Not able to translate dataproduct_subtype to SIAV2 query');
            }
        }
        const adqlConstraint = adqlConstraints.join(' AND ');
        const allValid = Array.from(fieldsValidity.values()).every((v) => v.valid);
        return {
            valid: allValid,
            adqlConstraint,
            fieldsValidity,
            adqlFieldsValidity: Array.from(fieldsValidity.values()).filter((v) => !v.valid),
            siaConstraints,
            siaConstraintErrors
        };
    };

    const constraintReducer = (fields, newFields) => {
        const fieldsValidity = new Map();
        const panelActive = isPanelChecked(panelValue, panelPrefix, fields);
        const siaConstraints = [];
        const siaConstraintErrors = [];
        let adqlConstraint = '';
        const adqlConstraintErrors = [];
        const constraintsResult = makeConstraints(fields, fieldsValidity, panelActive);
        updatePanelFields(constraintsResult.fieldsValidity, constraintsResult.valid, fields, newFields, panelValue, panelPrefix);
        if (isPanelChecked(panelValue, panelPrefix, newFields)) {
            if (constraintsResult.valid){
                if (constraintsResult.adqlConstraint?.length > 0){
                    adqlConstraint = constraintsResult.adqlConstraint;
                } else {
                    adqlConstraintErrors.push(`Unknown error processing ${panelValue} constraints`);
                }
                if  (constraintsResult.siaConstraints?.length > 0){
                    siaConstraints.push(...constraintsResult.siaConstraints);
                }
                if  (constraintsResult.siaConstraintErrors?.length > 0){
                    siaConstraintErrors.push(...constraintsResult.siaConstraintErrors);
                }
            } else if (!constraintsResult.adqlConstraint) {
                logger.warn(`invalid ${panelValue} adql constraints`);
            }
        }
        return {
            adqlConstraint,
            adqlConstraintErrors,
            siaConstraints,
            siaConstraintErrors
        };
    };

    const constraintResult = useConstraintReducer(panelPrefix, constraintReducer);

    return (
        <FieldGroupCollapsible header={<Header title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                               checkID={`${panelPrefix}Check`} panelValue={panelValue} message={message}/>}
                               initialState={{value: 'closed'}}
                               fieldKey={`${panelPrefix}SearchPanel`}
                               wrapperStyle={{marginBottom: 15}}
                               headerStyle={HeaderFont}
        >

            <div style={{
                display: 'flex', flexDirection: 'column', flexWrap: 'no-wrap',
                width: SpatialWidth, marginTop: 5
            }}>
                <div style={{display: 'flex', flexDirection: 'column', marginTop: '5px'}}>
                    <CheckboxGroupInputField
                        fieldKey={'obsCoreCalibrationLevel'}
                        options={calibrationOptions}
                        tooltip={obsCoreCalibrationLevelOptions.tooltip || 'Select ObsCore Calibration Level (calibration_level)'}
                        label={'Calibration Level:'}
                        labelWidth={LableSaptail}
                        multiple={true}
                    />
                    {obsCoreCalibrationLevelOptions.helptext &&
                    <div style={{marginLeft: LableSaptail, marginTop: '5px', padding: '2px'}}>
                        <i>{obsCoreCalibrationLevelOptions.helptext}</i>
                    </div>
                    }
                </div>
                <div style={{display: 'flex', flexDirection: 'column'}}>
                    <ListBoxInputField
                        fieldKey={'obsCoreTypeSelection'}
                        tooltip={'Select ObsCore Data Product Type'}
                        label={'Data Product Type:'}
                        labelWidth={LableSaptail}
                        initialState={{value: 'image'}}
                        options={typeOptions()}
                        wrapperStyle={{marginRight: '15px', padding: '8px 0 5px 0', display: 'flex'}}
                        multiple={true}
                    />
                </div>
                <div style={{marginTop: '5px'}}>
                    <ValidationField
                        fieldKey={'obsCoreInstrumentName'}
                        groupKey={skey}
                        inputWidth={Width_Column}
                        inputStyle={{overflow: 'auto', height: 16}}
                        tooltip={obsCoreInstrumentNameOptions.tooltip || 'Select ObsCore Instrument Name'}
                        placeholder={obsCoreInstrumentNameOptions.placeholder}
                        label={'Instrument Name:'}
                        labelWidth={LableSaptail}
                        validator={fakeValidator}
                    />
                    {obsCoreInstrumentNameOptions.helptext &&
                    <div style={{marginLeft: LableSaptail, marginTop: '5px', padding: '2px'}}>
                        <i>{obsCoreInstrumentNameOptions.helptext}</i>
                    </div>
                    }
                </div>
                <div style={{marginTop: '5px'}}>
                    <ValidationField
                        fieldKey={'obsCoreCollection'}
                        groupKey={skey}
                        inputWidth={Width_Column}
                        inputStyle={{overflow: 'auto', height: 16}}
                        tooltip={obsCoreCollectionOptions.tooltip || 'Select ObsCore Collection Name'}
                        placeholder={obsCoreCollectionOptions.placeholder}
                        label={'Collection:'}
                        labelWidth={LableSaptail}
                        validator={fakeValidator}
                    />
                    {obsCoreCollectionOptions.helptext &&
                    <div style={{marginLeft: LableSaptail, marginTop: '5px', padding: '2px'}}>
                        <i>{obsCoreCollectionOptions.helptext}</i>
                    </div>
                    }
                </div>
                {hasSubType && <div style={{marginTop: '5px'}}>
                    <ValidationField
                        fieldKey={'obsCoreSubType'}
                        groupKey={skey}
                        inputWidth={Width_Column}
                        inputStyle={{overflow: 'auto', height: 16}}
                        tooltip={obsCoreSubTypeOptions.tooltip || 'Select ObsCore Dataproduct Subtype Name'}
                        placeholder={obsCoreSubTypeOptions.placeholder}
                        label={'Data Product Subtype:'}
                        labelWidth={LableSaptail}
                        validator={fakeValidator}
                    />
                    {obsCoreSubTypeOptions.helptext &&
                    <div style={{marginLeft: LableSaptail, marginTop: '5px', padding: '2px'}}>
                        <i>{obsCoreSubTypeOptions.helptext}</i>
                    </div>
                    }
                </div>}
                {DEBUG_OBSCORE && <div>
                    adql fragment: {constraintResult?.adqlConstraint} <br/>
                    sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints.join('&')}
                </div>}
            </div>
        </FieldGroupCollapsible>
    );
}

ObsCoreSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};


export function ExposureDurationSearch({cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}) {
    const panelTitle = 'Timing';
    const panelValue = 'Exposure';
    const panelPrefix = getPanelPrefix(panelValue);

    const [rangeType, setRangeType] = useState('since');
    const [expTimeMode, setExpTimeMode] = useState(ISO);
    const [expMin, setExpMin] = useState();
    const [expMax, setExpMax] = useState();
    const [expLength, setExpLength] = useState();
    const [message, setMesage] = useState();

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            setRangeType(getFieldVal(groupKey, 'exposureRangeType', rangeType));
            setExpTimeMode(getFieldVal(groupKey, 'exposureTimeMode', expTimeMode));
            setExpMin(getFieldVal(groupKey, 'exposureMin', expMin));
            setExpMax(getFieldVal(groupKey, 'exposureMax', expMax));
            setExpLength(getFieldVal(groupKey, 'exposureLength', expLength));
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelValue;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
        });
    }, []);

    const DEBUG_OBSCORE = getAppOptions().tapObsCore?.debug ?? false;

    const constraintReducer = (fields, newFields) => {
        let constraintsResult = {
            siaConstraints: [],
            siaConstraintErrors: [],
        };
        const adqlConstraints = [];
        const adqlConstraintErrors = [];

        const fieldsValidity = new Map();
        let enoughMounted = true;
        let seenValue = false;
        if (fields) {
            const {exposureRangeType} = fields;
            if (exposureRangeType?.value === 'range') {
                const {exposureMin, exposureMax} = fields;
                if (exposureMin?.mounted && exposureMax?.mounted) {
                    const minValidity = checkField('exposureMin', fields, true, fieldsValidity);
                    const maxValidity = checkField('exposureMax', fields, true, fieldsValidity);
                    if (exposureMin?.value || exposureMax?.value) {
                        // We don't care what exposureTimeMode is - we just care about getting the time from the components
                        const expMinTimeInfo = getTimeInfo(exposureMin.timeMode, exposureMin.value, exposureMin.valid, exposureMin.message);
                        const expMaxTimeInfo = getTimeInfo(exposureMax.timeMode, exposureMax.value, exposureMax.valid, exposureMax.message);
                        const minValue = expMinTimeInfo[MJD].value.length ? expMinTimeInfo[MJD].value : '-Inf';
                        const maxValue = expMaxTimeInfo[MJD].value.length ? expMaxTimeInfo[MJD].value : '+Inf';
                        if (minValue && maxValue) {
                            if (Number(minValue) > Number(maxValue)) {
                                maxValidity.valid = false;
                                maxValidity.message = 'the start time is greater than the end time';
                            }
                        }
                        if (minValidity.valid && maxValidity.valid) {
                            const rangeList = [[minValue, maxValue]];
                            adqlConstraints.push(adqlQueryRange('t_min', 't_max', rangeList, false));
                            constraintsResult.siaConstraints.push(...siaQueryRange('TIME', rangeList));
                        }
                        seenValue = true;
                    }
                } else {
                    enoughMounted = false;
                }
            } else if (exposureRangeType?.value === 'since') {
                const {exposureSinceValue, exposureSinceOptions} = fields;
                if (exposureSinceValue?.mounted) {
                    if (checkField('exposureSinceValue', fields, false, fieldsValidity).valid) {
                        let sinceMillis;
                        switch (exposureSinceOptions.value) {
                            case 'minutes':
                                sinceMillis = toFloat(exposureSinceValue.value) * 60 * 1000;
                                break;
                            case 'hours':
                                sinceMillis = toFloat(exposureSinceValue.value) * 60 * 60 * 1000;
                                break;
                            case 'days':
                                sinceMillis = toFloat(exposureSinceValue.value) * 24 * 60 * 60 * 1000;
                                break;
                            case 'years':
                                sinceMillis = toFloat(exposureSinceValue.value) * 365 * 24 * 60 * 60 * 1000;
                                break;
                        }
                        const sinceString = new Date(Date.now() - sinceMillis).toISOString();
                        const mjdTime = convertISOToMJD(sinceString);
                        const rangeList = [[`${mjdTime}`, '+Inf']];
                        adqlConstraints.push(adqlQueryRange('t_min', 't_max', rangeList));
                        constraintsResult.siaConstraints.push(...siaQueryRange('TIME', rangeList));
                    }
                    seenValue = true;
                } else {
                    enoughMounted = false;
                }
            }
            const {exposureLengthMin, exposureLengthMax} = fields;
            if (enoughMounted && exposureLengthMin?.mounted) {
                checkField('exposureLengthMin', fields, true, fieldsValidity);
                const maxValidity = checkField('exposureLengthMax', fields, true, fieldsValidity);
                if (exposureLengthMin?.value || exposureLengthMax?.value) {
                    const minValue = exposureLengthMin?.value?.length === 0 ? '-Inf' : exposureLengthMin?.value ?? '-Inf';
                    const maxValue = exposureLengthMax?.value?.length === 0 ? '+Inf' : exposureLengthMax?.value ?? '+Inf';
                    const rangeList = [[minValue, maxValue]];
                    if (!minValue.endsWith('Inf') && !maxValue.endsWith('Inf') && Number(minValue) > Number(maxValue)) {
                        maxValidity.valid = false;
                        maxValidity.message = 'exposure time max must be greater than time min';
                    } else {
                        adqlConstraints.push(adqlQueryRange('t_exptime', 't_exptime', rangeList, true));
                        constraintsResult.siaConstraints.push(...siaQueryRange('EXPTIME', rangeList));
                    }
                    seenValue = true;
                }
            } else {
                enoughMounted = false;
            }
        }
        constraintsResult = {
            ...constraintsResult,
            valid: Array.from(fieldsValidity.values()).every((v) => v.valid) && enoughMounted && seenValue,
            adqlConstraint: adqlConstraints.join(' AND '),
        };
        let adqlConstraint;
        const siaConstraints = [];
        const siaConstraintErrors = [];
        updatePanelFields(fieldsValidity, constraintsResult.valid, fields, newFields, panelValue, panelPrefix, 'at least one field must be populated');
        if (isPanelChecked(panelValue, panelPrefix, newFields)) {
            if (constraintsResult.valid) {
                if (constraintsResult.adqlConstraint?.length > 0) {
                    adqlConstraint = constraintsResult.adqlConstraint;
                } else {
                    adqlConstraintErrors.push(`Unknown error processing ${panelValue} constraints`);
                }
                if (constraintsResult.siaConstraints?.length > 0) {
                    siaConstraints.push(...constraintsResult.siaConstraints);
                }
            } else if (!constraintsResult.adqlConstraint) {
                logger.warn(`invalid ${panelValue} adql constraints`);
            }
        }
        return {
            adqlConstraint,
            adqlConstraintErrors,
            siaConstraints,
            siaConstraintErrors
        };
    };

    const onChange = (inFields, action, rFields) => {
        const {fieldKey, value} = action.payload;
        if (fieldKey === 'exposureTimeMode') {    // time mode changes => update timefrom and timeto value
            onChangeTimeMode(value, inFields, rFields, ['exposureMin', 'exposureMax']);
        }
    };

    useFieldGroupReducer('exposure', onChange);

    const showExposureRange = () => {
        const timeOptions = [{label: 'UTC date/times (ISO format)', value: ISO},
            {label: 'MJD values', value: MJD}];
        const exposureRangeOptions = [{label: 'Completed in the Last...', value: 'since'}, {label: 'Overlapping specified range', value: 'range'}];
        const icon = 'calendar';

        //  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
        return (
            <div style={{display: 'block', marginTop: '5px'}}>
                <ListBoxInputField
                    fieldKey={'exposureRangeType'}
                    options={exposureRangeOptions}
                    alignment={'horizontal'}
                    label={'Time of Observation:'}
                    labelWidth={LableSaptail}
                />
                <div>
                    {rangeType === 'range' &&
                    <div style={{display: 'block', marginLeft: LeftInSearch, marginTop: 10}}>
                        <RadioGroupInputField
                            fieldKey={'exposureTimeMode'}
                            options={timeOptions}
                            alignment={'horizontal'}
                            wrapperStyle={{width: LabelWidth, marginTop: 5, marginLeft: 0}}
                            initialState={{value: ISO}}
                            label={'Use:'}
                            labelWidth={32 /* FIXME: Not sure if this is best */}
                            tooltip='Select time mode'
                        />
                        <div style={{display: 'flex', marginTop: 10}}>
                            <div title='Start Time'
                                 style={{display: 'inline-block', paddingRight: '4px', width: SpatialLableSaptail}}>Start Time
                            </div>
                            <div style={{width: Width_Time_Wrapper}}>
                                <TimePanel
                                    fieldKey={'exposureMin'}
                                    groupKey={skey}
                                    timeMode={expTimeMode}
                                    icon={icon}
                                    onClickIcon={changeDatePickerOpenStatusNew(FROM, 'exposureMin', expMin, expTimeMode, (value) => {
                                        /* NOTE: if we don't do timeMode: expTimeMode - we can't see the current time mode for this field (when new) */
                                        dispatchValueChange({
                                            ...{value},
                                            timeMode: expTimeMode,
                                            fieldKey: 'exposureMin',
                                            groupKey
                                        });
                                    })}
                                    feedbackStyle={{height: 100}}
                                    inputWidth={Width_Column}
                                    inputStyle={{overflow: 'auto', height: 16}}
                                    validator={timeValidator('exposureMin')}
                                    tooltip={"'Exposure start from' time (t_min)"}
                                    value={expMin}
                                />
                            </div>
                        </div>
                        <div style={{display: 'flex', marginTop: 5}}>
                            <div title='End Time'
                                 style={{display: 'inline-block', paddingRight: '4px', width: SpatialLableSaptail}}>End Time
                            </div>
                            <div style={{width: Width_Time_Wrapper}}>
                                <TimePanel
                                    fieldKey={'exposureMax'}
                                    groupKey={skey}
                                    timeMode={expTimeMode}
                                    icon={icon}
                                    onClickIcon={changeDatePickerOpenStatusNew(TO, 'exposureMax', expMax, expTimeMode, (value) => {
                                        dispatchValueChange({
                                            ...{value},
                                            timeMode: expTimeMode,
                                            fieldKey: 'exposureMax',
                                            groupKey
                                        });
                                    })}
                                    feedbackStyle={{height: 100}}
                                    inputWidth={Width_Column}
                                    inputStyle={{overflow: 'auto', height: 16}}
                                    validator={timeValidator('exposureMax')}
                                    tooltip={"'Exposure end to' time (t_max)"}
                                    value={expMax}
                                />
                            </div>
                        </div>
                    </div>
                    }
                    {rangeType === 'since' &&
                    <div style={{display: 'flex', marginTop: 10}}>
                        <ValidationField
                            fieldKey={'exposureSinceValue'} // FIXME: Introduce SinceValue or similar
                            groupKey={skey}
                            size={SmallFloatNumericWidth}
                            inputStyle={{overflow: 'auto', height: 16}}
                            validator={fakeValidator}
                            wrapperStyle={{marginLeft: LableSaptail, paddingLeft: 4 /* Extra padding because there's no label */, paddingBottom: 5}}
                        />
                        <ListBoxInputField
                            fieldKey={'exposureSinceOptions'} // FIXME: Introduce SinceOptions
                            options={[
                                {label: 'Minutes', value: 'minutes'},
                                {label: 'Hours', value: 'hours'},
                                {label: 'Days', value: 'days'},
                                {label: 'Years',value: 'years'}
                            ]}
                            initialState={{value: 'hours'}}
                        />
                    </div>
                    }
                    <div style={{display: 'flex', marginTop: 5}}>
                        <ValidationField
                            fieldKey={'exposureLengthMin'}
                            groupKey={skey}
                            size={SmallFloatNumericWidth}
                            inputStyle={{overflow: 'auto', height: 16}}
                            label={'Exposure Duration:'}
                            tooltip={'Cumulative shutter-open exposure duration in seconds'}
                            labelWidth={LableSaptail}
                            validator={minimumPositiveFloatValidator('Minimum Exposure Length')}
                            placeholder={'-Inf'}
                        />
                        <div style={{display: 'flex', marginTop: 5, marginRight: '16px', paddingRight: '3px'}}>to</div>
                        <ValidationField
                            fieldKey={'exposureLengthMax'}
                            groupKey={skey}
                            size={SmallFloatNumericWidth}
                            inputStyle={{overflow: 'auto', height: 16}}
                            tooltip={'Cumulative shutter-open exposure must be less than this amount'}
                            validator={maximumPositiveFloatValidator('Maximum Exposure Length')}
                            placeholder={'+Inf'}
                        />
                        <div style={{display: 'flex', marginTop: 5}}>seconds</div>
                    </div>
                </div>
                {DEBUG_OBSCORE && <div>
                    adql fragment: {constraintResult?.adqlConstraint} <br/>
                    sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints.join('&')}
                </div>}
            </div>
        );
    };

    const constraintResult = useConstraintReducer('exposure', constraintReducer, [rangeType]);

    return (
        <FieldGroupCollapsible
            header={<Header title={panelTitle} helpID={tapHelpId(`${panelPrefix}`)}
                            checkID={`${panelPrefix}Check`}
                            panelValue={panelValue}
                            message={message}
            />}
            initialState={{value: 'closed'}}
            fieldKey={`${panelPrefix}SearchPanel`}
            wrapperStyle={{marginBottom: 15}}
            headerStyle={HeaderFont}>
            <div style={{marginTop: 5}}>
                {showExposureRange()}
            </div>
        </FieldGroupCollapsible>
    );
}

ExposureDurationSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};

function adqlQueryRange(lowerBound, upperBound, rangeList, contains=false) {
    const adqlFragmentList = [];
    rangeList.forEach((rangePair) =>{
        const [lowerValue, upperValue] = rangePair;
        const query = [];
        if (contains && lowerValue !== '-Inf' && !upperValue.endsWith('Inf')) {
            if (lowerValue === upperValue){
                query.push(lowerValue,'BETWEEN', lowerBound,'AND',upperBound);
            } else if (lowerBound === upperBound) {
                query.push(lowerBound, 'BETWEEN', lowerValue, 'AND', upperValue);
            } else {
                query.push(
                    lowerValue, 'BETWEEN', lowerBound, 'AND', upperBound,
                    'AND',
                    upperValue, 'BETWEEN', lowerBound, 'AND', upperBound,
                );
            }
        } else {
            if (!upperValue.endsWith('Inf')) {
                query.push(lowerBound,'<=',upperValue);
            }
            if (!lowerValue.endsWith('Inf') && !upperValue.endsWith('Inf')) {
                query.push('AND');
            }
            if (!lowerValue.endsWith('Inf')) {
                query.push(lowerValue, '<=', upperBound);
            }
        }
        if (query.length > 1){
            adqlFragmentList.push('( ' + query.join(' ') + ' )');
        }
    });
    const adqlFragment = adqlFragmentList.join(' OR ');
    return adqlFragmentList.length > 1 ? `( ${adqlFragment} )` : adqlFragment;
}

/*
 * Takes a ranges list and returns a list of sia constraints (query params)
 */
function siaQueryRange(keyword, rangeList) {
    const siaFragmentList = [];
    rangeList.forEach((rangePair) =>{
        const [lowerValue, upperValue] = rangePair;
        if (lowerValue === upperValue){
            siaFragmentList.push(`${keyword}=${lowerValue}`);
        } else {
            siaFragmentList.push(`${keyword}=${lowerValue} ${upperValue}`);
        }
    });
    return siaFragmentList;
}

export function ObsCoreWavelengthSearch({cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}) {
    const panelTitle = 'Spectral Coverage';
    const panelValue = 'Wavelength';
    const panelPrefix = getPanelPrefix(panelValue);
    const [selectionType, setSelectionType] = useState('filter');
    const [rangeType, setRangeType] = useState('contains');
    const [message, setMesage] = useState();

    const DEBUG_OBSCORE = getAppOptions().tapObsCore?.debug ?? false;

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            setSelectionType(getFieldVal(groupKey, 'obsCoreWavelengthSelectionType', selectionType));
            setRangeType(getFieldVal(groupKey, 'obsCoreWavelengthRangeType', rangeType));
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelValue;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
        });
    }, []);


    const filterDefinitions = getAppOptions()?.tapObsCore?.filterDefinitions ?? [];

    const constraintReducer = (fields, newFields) => {
        const adqlConstraints = [];
        const adqlConstraintErrors = [];
        const siaConstraints = [];
        const siaConstraintErrors = [];
        const fieldsValidity = new Map();
        // The reducer was added when the component was mounted but not before all parts of the
        // But fields show up later if we change between filter/numerical.
        // We can use these two fields to verify the other parts are mounted.
        const {obsCoreWavelengthSelectionType, obsCoreWavelengthRangeType} = fields;
        let enoughMounted = true;
        if (fields) {
            // pull out the fields we care about
            if (obsCoreWavelengthSelectionType?.value === 'filter') {
                const rangeList = [];
                filterDefinitions.forEach((filterDefinition) => {
                    const fieldKey = 'filter' + filterDefinition.name;
                    const field = get(fields, fieldKey);
                    if (field.mounted) {
                        checkField(fieldKey, fields, true, fieldsValidity);
                        if (field.value?.length) {
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
                    } else {
                        enoughMounted = false;
                    }
                });
                if (enoughMounted) {
                    if (rangeList.length) {
                        adqlConstraints.push(adqlQueryRange('em_min', 'em_max', rangeList, true));
                        siaConstraints.push(...siaQueryRange('BAND', rangeList));
                    } else {
                        // Need at least one field to be non-empty
                        [...fieldsValidity.values()][0].valid = false;
                        [...fieldsValidity.values()][0].message = 'at least one filter must be checked';
                    }
                }
            } else if (obsCoreWavelengthSelectionType?.value === 'numerical' || obsCoreWavelengthRangeType?.mounted) {
                const {
                    obsCoreWavelengthContains,
                    obsCoreWavelengthMinRange,
                    obsCoreWavelengthMaxRange,
                    obsCoreWavelengthUnits,
                } = fields;
                if (obsCoreWavelengthUnits?.mounted) {
                    let exponent;
                    switch (obsCoreWavelengthUnits.value) {
                        case 'nm':
                            exponent = 'e-9';
                            break;
                        case 'angstrom':
                            exponent = 'e-10';
                            break;
                        case 'um':
                            exponent = 'e-6';
                            break;
                    }
                    if (obsCoreWavelengthRangeType?.value === 'contains') {
                        if (obsCoreWavelengthContains.mounted) {
                            if (checkField('obsCoreWavelengthContains', fields, false, fieldsValidity).valid) {
                                const range = obsCoreWavelengthContains.value;
                                const rangeList = [[`${range}${exponent}`, `${range}${exponent}`]];
                                adqlConstraints.push(adqlQueryRange('em_min', 'em_max', rangeList, true));
                                siaConstraints.push(...siaQueryRange('BAND', rangeList));
                            }
                        } else {
                            enoughMounted = false;
                        }
                    }
                    if (obsCoreWavelengthRangeType?.value === 'overlaps') {
                        if (obsCoreWavelengthMinRange?.mounted) {
                            const minValidity = checkField('obsCoreWavelengthMinRange', fields, true, fieldsValidity);
                            const maxValidity = checkField('obsCoreWavelengthMaxRange', fields, true, fieldsValidity);
                            const anyHasValue = obsCoreWavelengthMinRange?.value || obsCoreWavelengthMaxRange?.value;
                            if (anyHasValue) {
                                const minValue = obsCoreWavelengthMinRange?.value?.length === 0 ? '-Inf' : obsCoreWavelengthMinRange?.value ?? '-Inf';
                                const maxValue = obsCoreWavelengthMaxRange?.value?.length === 0 ? '+Inf' : obsCoreWavelengthMaxRange?.value ?? '+Inf';
                                const lowerValue = minValue === '-Inf' ? minValue : `${minValue}${exponent}`;
                                const upperValue = maxValue === '+Inf' ? maxValue : `${maxValue}${exponent}`;
                                const rangeList = [[lowerValue, upperValue]];
                                if (!lowerValue.endsWith('Inf') && !upperValue.endsWith('Inf') && Number(lowerValue) > Number(upperValue)) {
                                    maxValidity.valid = false;
                                    maxValidity.message = 'the max wavelength is smaller than the min wavelength';
                                } else {
                                    adqlConstraints.push(adqlQueryRange('em_min', 'em_max', rangeList));
                                    siaConstraints.push(...siaQueryRange('BAND', rangeList));
                                }
                            } else {
                                minValidity.valid = false;
                                minValidity.message = 'at least one field must be populated';
                            }
                        } else {
                            enoughMounted = false;
                        }
                    }
                } else {
                    enoughMounted = false;
                }
            }
        }
        const constraintsResult = {
            valid: Array.from(fieldsValidity.values()).every((v) => v.valid) && enoughMounted,
            adqlConstraint: adqlConstraints.join(' AND '),
            siaConstraints,
            siaConstraintErrors
        };
        let adqlConstraint;
        updatePanelFields(fieldsValidity, constraintsResult.valid, fields, newFields, panelValue, panelPrefix);
        if (isPanelChecked(panelValue, panelPrefix, newFields)) {
            if (constraintsResult.valid) {
                if (constraintsResult.adqlConstraint?.length > 0) {
                    adqlConstraint = constraintsResult.adqlConstraint;
                } else {
                    adqlConstraintErrors.push(`Unknown error processing ${panelValue} constraints`);
                }
                if (constraintsResult.siaConstraints?.length > 0) {
                    siaConstraints.push(...constraintsResult.siaConstraints);
                }
            } else if (!constraintsResult.adqlConstraint) {
                logger.warn(`invalid ${panelValue} adql constraints`);
            }
        }
        return {
            adqlConstraint,
            adqlConstraintErrors,
            siaConstraints,
            siaConstraintErrors
        };
    };

    const constraintResult = useConstraintReducer('wavelength', constraintReducer, [selectionType, rangeType]);
    const hasFilters = filterDefinitions?.length > 0;
    const useNumerical = !hasFilters || selectionType === 'numerical';

    return (
        <FieldGroupCollapsible header={<Header title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                               checkID={`${panelPrefix}Check`}
                                               panelValue={panelValue}
                                               message={message}/>}
                               initialState={{value: 'closed'}}
                               fieldKey={`${panelPrefix}SearchPanel`}
                               headerStyle={HeaderFont}>
            <div style={{display: 'flex', flexDirection: 'column', width: SpatialWidth, justifyContent: 'flex-start'}}>
                {hasFilters && <RadioGroupInputField
                    fieldKey={'obsCoreWavelengthSelectionType'}
                    options={[{label: 'By Filter Bands', value: 'filter'}, {label: 'By Wavelength', value: 'numerical'}]}
                    alignment={'horizontal'}
                    wrapperStyle={{marginTop: '10px'}}
                    label={'Query Type:'}
                    labelWidth={LableSaptail}
                />}
                {hasFilters && selectionType === 'filter' &&
                <div style={{marginTop: '10px'}}>
                    <div style={{paddingTop: '4px'}}>Require coverage at the approximate center of these filters:</div>
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
                        <ListBoxInputField
                            fieldKey={'obsCoreWavelengthRangeType'}
                            options={
                                [
                                    {label: 'contains', value: 'contains'},
                                    {label: 'overlaps', value: 'overlaps'},
                                ]}
                            initialState={{
                                value: 'contains'
                            }}
                            label={'Select observations whose wavelength coverage'}
                            labelWidth={236}
                            multiple={false}
                        />
                    </div>
                    <div style={{display: 'inline-flex', marginTop: '10px', marginLeft: LeftInSearch}}>
                        {rangeType === 'contains' &&
                        <div style={{display: 'flex'}}>
                            <ValidationField
                                fieldKey={'obsCoreWavelengthContains'}
                                groupKey={skey}
                                size={SmallFloatNumericWidth}
                                inputStyle={{overflow: 'auto', height: 16}}
                                validator={floatValidator(0, 100e15, 'Wavelength')}
                            />
                        </div>
                        }
                        {rangeType === 'overlaps' &&
                        <div style={{display: 'flex'}}>
                            <ValidationField
                                fieldKey={'obsCoreWavelengthMinRange'}
                                groupKey={skey}
                                size={SmallFloatNumericWidth}
                                inputStyle={{overflow: 'auto', height: 16}}
                                validator={minimumPositiveFloatValidator('Min Wavelength')}
                                placeholder={'-Inf'}
                            />
                            <div style={{display: 'flex', marginTop: 5, marginRight: '16px', paddingRight: '3px'}}>to</div>
                            <ValidationField
                                fieldKey={'obsCoreWavelengthMaxRange'}
                                groupKey={skey}
                                size={SmallFloatNumericWidth}
                                inputStyle={{overflow: 'auto', height: 16}}
                                validator={maximumPositiveFloatValidator('Max Wavelength')}
                                placeholder={'+Inf'}
                            />
                        </div>
                        }
                        <ListBoxInputField
                            fieldKey={'obsCoreWavelengthUnits'}
                            options={
                                [
                                    {label: 'microns', value: 'um'},
                                    {label: 'nanometers', value: 'nm'},
                                    {label: 'angstroms', value: 'angstrom'},
                                ]}
                            initialState={{
                                value: 'nm'
                            }}
                            multiple={false}
                        />
                    </div>
                </div>
                }
                {DEBUG_OBSCORE && <div>
                    adql fragment: {constraintResult?.adqlConstraint} <br/>
                    sia
                    fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints.join('&')}
                </div>}
            </div>
        </FieldGroupCollapsible>
    );
}

ObsCoreWavelengthSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};
