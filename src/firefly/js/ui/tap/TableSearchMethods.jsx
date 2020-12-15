import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {get, has, isUndefined, set} from 'lodash';
import Enum from 'enum';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {findCenterColumnsByColumnsModel, posCol, UCDCoord} from '../../util/VOAnalyzer.js';
import FieldGroupCntlr, {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {ServerParams} from '../../data/ServerParams.js';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {convert} from '../../visualize/VisUtil.js';
import {getActivePlotView, primePlot} from '../../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {calcCornerString, renderPolygonDataArea} from '../CatalogSearchMethodType.jsx';
import {clone} from '../../util/WebUtil.js';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {convertMJDToISO, validateDateTime, validateMJD} from '../DateTimePickerField.jsx';
import {TimePanel} from '../TimePanel.jsx';
import {getColumnAttribute, HeaderFont, ISO, MJD, tapHelpId} from './TapUtil.js';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression';
import {
    changeDatePickerOpenStatus,
    FROM,
    getPanelPrefix,
    Header,
    isPanelChecked,
    LabelWidth,
    LableSaptail,
    LeftInSearch,
    onChangeDateTimePicker,
    onChangeTimeField,
    onChangeTimeMode,
    SpatialWidth,
    TO,
    updatePanelFields,
    Width_Column,
    Width_Time_Wrapper
} from 'firefly/ui/tap/TableSearchHelpers';
import {ExposureDurationSearch, ObsCoreSearch, ObsCoreWavelengthSearch} from 'firefly/ui/tap/ObsCore';
import {getAppOptions} from 'firefly/api/ApiUtil';
import CoordinateSys from 'firefly/visualize/CoordSys';
import {Logger} from 'firefly/util/Logger';

const logger = Logger('TableSearchMethods');

export const skey = 'TABLE_SEARCH_METHODS';
const Spatial = 'Spatial';
const CenterColumns = 'centerColumns';
const CenterLonColumns = 'centerLonColumns';
const CenterLatColumns = 'centerLatColumns';
const SpatialMethod = 'spatialMethod';
const RadiusSize = 'coneSize';
const PolygonCorners = 'polygoncoords';
const ImageCornerCalc = 'imageCornerCalc';
const Temporal = 'Temporal';
const TemporalCheck = 'temporalCheck';
const TemporalColumns = 'temporalColumns';
const DatePickerOpenStatus = 'datePickerOpenStatus';
const ExposureColumns = 'exposureColumns';
const TimePickerFrom = 'timePickerFrom';
const TimePickerTo = 'timePickerTo';
const MJDFrom = 'mjdfrom';
const MJDTo = 'mjdto';
const TimeFrom = 'timeFrom';
const TimeTo = 'timeTo';
const TimeOptions = 'timeOptions';

const ConstraintResults = 'constraintResults';

// Aligns with padding of Target
const SpatialLableSaptail = LableSaptail + 45 /* padding of target */ - 4 /* padding of label */;


const CrtColumnsModel = 'crtColumnsModel';

const PanelMessage = 'panelMessage';

const fieldsMap = {[Spatial]: {
                        [CenterColumns]: {label: 'Position Columns'},
                        [CenterLonColumns]: {label: 'Longitude Column'},
                        [CenterLatColumns]: {label: 'Latitude Column'},
                        [ServerParams.USER_TARGET_WORLD_PT]: {label: 'Coordinates or Object Name'},
                        [RadiusSize]: {label: 'Radius'},
                        [PolygonCorners]:  {label: 'Coordinates'}},
                   [Temporal]: {
                        [TemporalColumns]: {label: 'Temporal Column'},
                        [ExposureColumns]: {label: 'Exposure Duration Column'},
                        [TimeFrom]: {label: 'From'},
                        [TimeTo]: {label: 'To'},
                        [MJDFrom]: {label: 'MJD (From)'},
                        [MJDTo]: {label: 'MJD (To)'},
                        [TimePickerFrom]: {label: 'ISO (From)'},
                        [TimePickerTo]: {label: 'ISO (To)'}},
};


const TapSpatialSearchMethod = new Enum({
    'Cone': 'Cone',
    'Polygon': 'Polygon'
});

// size used
export const SpatialPanelWidth = Math.max(Width_Time_Wrapper*2, SpatialWidth) + LabelWidth + 10;

function isFieldInPanel(fieldKey) {
    return Object.keys(fieldsMap).find((panel) => {
               return Object.keys(fieldsMap[panel]).includes(fieldKey);
           });
}

function getLabel(key, trailing='') {
    const panel = isFieldInPanel(key);
    const l = panel ? get(fieldsMap, [panel, key, 'label'], '') : '';
    return l ? l+trailing  : l;
}


const constraintReducers = {};
constraintReducers.adqlReducerMap = new Map();
const chainedReducers = new Map();

/**
 * Pseudo-hook for registering a constraint reducer.
 * A constraints reducer takes in (fields, newFields), may modify
 * newFields, and returns an object of the form:
 * {adqlConstraints, adqlConstraintErrors, siaConstraints, siaConstraintErrors}
 * adqlConstraints can usually be joined with AND, siaConstraints can be joined with &.
 * @param component
 * @param constraintReducer
 * @param watchFields
 * @returns {unknown}
 */
function useConstraintReducer(component, constraintReducer, watchFields) {
    const [constraintResult, setConstraintResult] = useState(null);
    const fieldName = `${ConstraintResults}.${component}`;

    useEffect(() => {
        function reduceQuery(fields, newFields) {
            const constraintResult = constraintReducer(fields, newFields);
            newFields[fieldName] = constraintResult;
            setConstraintResult(constraintResult);
            return constraintResult;
        }
        constraintReducers.adqlReducerMap.set(component, reduceQuery);
        return () => {
            constraintReducers.adqlReducerMap.delete(component);
        };
    });

    useEffect(() => {
        return FieldGroupUtils.bindToStore(skey, (fields) => {
            const constraintResults = get(fields, [fieldName]);
            setConstraintResult(constraintResults);
        });
    }, []);

    const deps = watchFields || [];
    useEffect(() => {
        // Reset generated value when one of the deps change.
        dispatchValueChange({...{value: ''}, fieldKey: fieldName, groupKey: skey});
    }, [...deps, fieldName]);

    return constraintResult;
}

function useFieldGroupReducer(component, fieldGroupReducer) {
    useEffect(() => {
        chainedReducers.set(component, fieldGroupReducer);
        return () => {
            chainedReducers.delete(component);
        };
    });
}

const FunctionalTableSearchMethods = (props) => {

    const [fields, setFields] = useState(FieldGroupUtils.getGroupFields(skey));
    //const [queryState, dispatchQueryState] = useReducer();
    // FIXME: Rationalize use of state, props, etc...
    //const [columnsModel, setColumnsModel] = useState(props ? props.columnsModel : getTblById(get(fields, [CrtColumnsModel, 'value'])));
    const columnsModel = props.columnsModel;
    const groupKey = skey;
    const [constraintResults, setConstraintResults] = useState();

    useEffect(() => {
        setFields(FieldGroupUtils.getGroupFields(skey));
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            const {constraintResults} = fields;
            setConstraintResults(constraintResults);
        });
    }, []);

    useEffect(() => {
        // We effectively want to invalidate the computed ADQL results when the
        // constraint reducers change. Dispatching an update will trigger the reducers.
        dispatchValueChange({...{value: ''}, fieldKey: ConstraintResults, groupKey});
    }, [constraintReducers]);

    const obsCoreEnabled = props.obsCoreEnabled;
    const cols = getAvailableColumns(columnsModel);
    const DEBUG_OBSCORE = getAppOptions().tapObsCore?.debug ?? false;

    // We create a new reducer for this FieldGroup
    return (
        <FieldGroup style={{height: '100%', overflow: 'auto'}}
                    groupKey={groupKey} keepState={true} reducerFunc={buildTapSearchMethodReducer(columnsModel)}>
            {obsCoreEnabled && <ObsCoreSearch {...{cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}} />}
            <SpatialSearch {...{cols, columnsModel, groupKey, fields, initArgs:props.initArgs, obsCoreEnabled, useConstraintReducer, useFieldGroupReducer}} />
            {obsCoreEnabled && <ExposureDurationSearch {...{cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}} />}
            {!obsCoreEnabled && <TemporalSearch {...{cols, columnsModel, groupKey, fields, obsCoreEnabled, useConstraintReducer, useFieldGroupReducer}} />}
            {obsCoreEnabled && <ObsCoreWavelengthSearch {...{cols, groupKey, fields, useConstraintReducer, useFieldGroupReducer}} />}
            {DEBUG_OBSCORE && <div>
                adql WHERE: <br/>
                {constraintResults?.adqlConstraints?.join(' AND ')}
                <br/>
                adql errors: <br/>
                {constraintResults?.adqlConstraintErrors?.map((elem) => {elem.join(', ');})}
                <br/>
                sia ?: <br/>
                {constraintResults?.siaConstraints?.join('&')}
                <br/>
                sia errors: <br/>
                {constraintResults?.siaConstraintErrors?.map((elem) => elem.join(', ')).join('<br/>')}
                <br/>
            </div>}
        </FieldGroup>
    );
};

export const TableSearchMethods = FunctionalTableSearchMethods;

function SpatialSearch({cols, columnsModel, groupKey, fields, initArgs={}, obsCoreEnabled, useConstraintReducer, useFieldGroupReducer}) {
    const panelTitle = !obsCoreEnabled ? Spatial : 'Location';
    const panelValue = Spatial;
    const panelPrefix = getPanelPrefix(Spatial);
    const {POSITION:worldPt, radiusInArcSec}= initArgs;
    const [spatialMethod, setSpatialMethod] = useState(TapSpatialSearchMethod.Cone.value);
    const [spatialRegionOperation, setSpatialRegionOperation] = useState('contains_shape');
    const centerColObj = formCenterColumns(columnsModel);
    const [message, setMessage] = useState();

    // useEffect(() => {
    //     setPanelTitle(!obsCoreEnabled ? Spatial : 'Location');
    // }, [obsCoreEnabled]);

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            const panelActive = isPanelChecked(panelValue, panelPrefix, fields);
            setMessage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, 'panelMessage'], '') : '');
            setSpatialMethod(getFieldVal(groupKey, SpatialMethod, spatialMethod));
            setSpatialRegionOperation(getFieldVal(groupKey, 'spatialRegionOperation', spatialRegionOperation));
        });
    }, []);

    const onChange = (inFields, action, rFields) => {
        const {fieldKey, value} = action.payload;

        const onChangePolygonCoordinates = () => {
            rFields.imageCornerCalc = clone(inFields.imageCornerCalc, {value: 'user'});
            if(!isPanelChecked(panelValue, panelPrefix, rFields)){
                const panelChk = `${panelValue}Check`;
                set(rFields, [panelChk, 'value'], panelValue);
            }
        };

        const onChangeToPolygonMethod = () => {
            const cornerCalcV = get(inFields.imageCornerCalc, 'value', 'user');
            const pv = getActivePlotView(visRoot());


            if (pv && (cornerCalcV === 'image' || cornerCalcV === 'viewport' || cornerCalcV === 'area-selection')) {
                const plot = primePlot(pv);

                if (plot) {
                    const sel = plot.attributes[PlotAttribute.SELECTION];
                    if (!sel && cornerCalcV === 'area-selection') {
                        rFields.imageCornerCalc = clone(inFields.imageCornerCalc, {value: 'image'});
                    }
                    const {value:cornerCalcV2}= rFields.imageCornerCalc;
                    const v = calcCornerString(pv, cornerCalcV2);
                    rFields.polygoncoords = clone(inFields.polygoncoords, {value: v});
                }
            }
        };

        if (fieldKey === PolygonCorners) {
            onChangePolygonCoordinates(rFields);
        } else if (fieldKey === SpatialMethod && value === TapSpatialSearchMethod.Polygon.key || fieldKey === ImageCornerCalc) {
            onChangeToPolygonMethod();
        }
    };

    const constraintReducer = function(fields, newFields) {
        const siaConstraints = [];
        const siaConstraintErrors = new Map();
        let adqlConstraint = '';
        const adqlConstraintErrors = [];
        const constraintsResult = makeSpatialConstraints(fields, columnsModel, newFields);
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

    const DEBUG_OBSCORE = getAppOptions().tapObsCore?.debug ?? false;
    const constraintResult = useConstraintReducer('spatial', constraintReducer, [obsCoreEnabled, spatialRegionOperation]);

    useFieldGroupReducer('spatial', onChange);

    const doObsCoreSearch = () => {

        return (
            <div style={{marginTop: '5px'}}>
                <ListBoxInputField
                    fieldKey={'spatialRegionOperation'}
                    options={
                        [
                            {label: 'Observation boundary contains point', value: 'contains_point'},
                            {label: 'Observation boundary contains shape', value: 'contains_shape'},
                            {label: 'Observation boundary is contained by shape', value: 'contained_by_shape'},
                            {label: 'Observation boundary intersects shape', value: 'intersects'},
                            {label: 'central point (s_ra, s_dec) is contained by shape', value: 'center_contained'},
                        ]}
                    initialState={{
                        value: 'contains_shape'
                    }}
                    multiple={false}
                    label={'Query type:'}
                    labelWidth={LableSaptail}
                />
                <div style={{marginTop: '5px'}}>
                    {spatialRegionOperation !== 'contains_point' && doSpatialSearch()}
                    {spatialRegionOperation === 'contains_point' &&
                        <div style={{marginLeft: LeftInSearch}}>
                            {renderTargetPanel(groupKey, fields, true)}
                        </div>
                    }
                </div>
            </div>
        );
    };

    const showCenterColumns = () => {
        return (
            <div style={{marginTop: '5px'}}>
                <ColumnFld fieldKey={CenterLonColumns}
                           groupKey={groupKey}
                           cols={cols}
                           name={getLabel(CenterLonColumns).toLowerCase()} // label that appears in column chooser
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                           initValue={centerColObj.lon}
                           tooltip={'Center longitude column for spatial search'}
                           label={getLabel(CenterLonColumns, ':')}
                           labelWidth={SpatialLableSaptail}
                           validator={getColValidator(cols, false, false)}
                />
                <div style={{marginTop: 5}}>
                    <ColumnFld fieldKey={CenterLatColumns}
                               groupKey={groupKey}
                               cols={cols}
                               name={getLabel(CenterLatColumns).toLowerCase()} // label that appears in column chooser
                               inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                               initValue={centerColObj.lat}
                               tooltip={'Center latitude column for spatial search'}
                               label={getLabel(CenterLatColumns, ':')}
                               labelWidth={SpatialLableSaptail}
                               validator={getColValidator(cols, false, false)}

                    />
                </div>
            </div>
        );
    };

    const doSpatialSearch = () => {
        return (
            <div style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap',
                         width: SpatialWidth, marginLeft: LeftInSearch, marginTop: 5}}>
                {selectSpatialSearchMethod(groupKey, fields, spatialMethod)}
                {setSpatialSearchSize(fields, radiusInArcSec, spatialMethod)}
            </div>
        );
    };

    return (
        <FieldGroupCollapsible header={<Header title={panelTitle}  helpID={tapHelpId(panelPrefix)}
                                       checkID={`${panelPrefix}Check`}  panelValue={panelValue} message={message} enabled={Boolean(worldPt)}/>}
                               initialState={{ value: 'open' }}
                               fieldKey={`${panelPrefix}SearchPanel`}
                               wrapperStyle={{marginBottom: 15}}
                               label={'Spatial'}
                               headerStyle={HeaderFont}>
            <div style={{marginTop: '5px'}}>
                {!obsCoreEnabled && showCenterColumns()}
                {!obsCoreEnabled && doSpatialSearch()}
                {obsCoreEnabled && doObsCoreSearch()}
            </div>
            {DEBUG_OBSCORE && <div>
                adql fragment: {constraintResult?.adqlConstraint} <br/>
                sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints}
            </div>}
        </FieldGroupCollapsible>
    );
}

SpatialSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object,
    initArgs: PropTypes.object,
    obsCoreEnabled: PropTypes.bool,
};

/**
 * Temporal Search is not relevant to SIA/ObsCore, so we don't do SIA reduction.
 */
function TemporalSearch({cols, columnsModel, groupKey, fields, useConstraintReducer, useFieldGroupReducer}) {
    const panelTitle = Temporal;
    const panelPrefix = getPanelPrefix(panelTitle);
    const [message, setMesage] = useState();

    useEffect(() => {
        return FieldGroupUtils.bindToStore(groupKey, (fields) => {
            const panelActive = getFieldVal(groupKey, `${panelPrefix}Check`) === panelTitle;
            setMesage(panelActive ? get(fields, [`${panelPrefix}SearchPanel`, PanelMessage], '') : '');
        });
    }, []);

    const showTemporalColumns = () => {
        return (
            <div style={{marginLeft: LeftInSearch}}>
                <ColumnFld fieldKey={TemporalColumns}
                           groupKey={groupKey}
                           cols={cols}
                           name={getLabel(TemporalColumns).toLowerCase()} // label that appears in column choosery
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                           tooltip={'Column for temporal search'}
                           label={getLabel(TemporalColumns, ':')}
                           labelWidth={LabelWidth}
                           validator={getColValidator(cols, false, false)}
                />
            </div>
        );
    };

    const showTimeRange = () => {
        const timeOptions = [{label: 'ISO', value: ISO},
                             {label: 'MJD', value: MJD}];
        const crtTimeMode = get(fields, [TimeOptions, 'value'], ISO);
        const icon = crtTimeMode === ISO ? 'calendar' : '';

        //  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
        return (
            <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5, width: SpatialPanelWidth}}>
                <RadioGroupInputField fieldKey={TimeOptions}
                                      options={timeOptions}
                                      alignment={'horizontal'}
                                      wrapperStyle={{width: LabelWidth, paddingRight:'4px'}}
                                      initialState={{
                                          value: ISO
                                      }}
                                      tooltip='Choose between:\nISO 8601 time format (e.g., 2021-03-20)\nor\nModified Julian Date time format (e.g., 59293.1)'
                />
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeFrom}
                                 groupKey={skey}
                                 timeMode={crtTimeMode}
                                 icon={icon}
                                 onClickIcon={changeDatePickerOpenStatus(FROM, TimeFrom)}
                                 feedbackStyle={{height: 100}}
                                 inputWidth={Width_Column}
                                 inputStyle={{overflow:'auto', height:16}}
                                 validator={timeValidator}
                                 tooltip={"'from' time"}

                    />
                </div>
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeTo}
                               groupKey={skey}
                               timeMode={crtTimeMode}
                               icon={icon}
                               onClickIcon={changeDatePickerOpenStatus(TO, TimeTo)}
                               feedbackStyle={{height: 100}}
                               inputWidth={Width_Column}
                               inputStyle={{overflow:'auto', height:16}}
                               validator={timeValidator}
                               tooltip={"'to' time"}
                    />
               </div>
            </div>
        );
    };

    const onChange = (inFields, action, rFields) => {

        const {fieldKey, value} = action.payload;
        if (fieldKey === TimeFrom || fieldKey === TimeTo) { // update mjd & picker
            onChangeTimeField(value, inFields, rFields, fieldKey, TimeOptions);
        } else if (fieldKey === TimeOptions) {    // time mode changes => update timefrom and timeto value
            onChangeTimeMode(value, inFields, rFields, [TimeFrom, TimeTo]);
        } else if ( (fieldKey.startsWith(TimeFrom) || fieldKey.startsWith(TimeTo)) && fieldKey.endsWith('Picker')) { // update mjd & time fields
            // Picker Key is `${fieldKey}Picker`
            const {timeOptions} = inFields;
            const targetKey = fieldKey.substring(0, fieldKey.indexOf('Picker'));
            onChangeDateTimePicker(value, inFields, rFields, targetKey, fieldKey, timeOptions);
            if(!isPanelChecked(Temporal, panelPrefix, rFields)){
                set(rFields, [TemporalCheck, 'value'], Temporal);
            }
        } else if (fieldKey === TemporalColumns) {
            if (inFields[TemporalColumns]?.value?.length){
                if(!isPanelChecked(Temporal, panelPrefix, rFields)){
                    set(rFields, [TemporalCheck, 'value'], Temporal);
                }
            }
        }
    };

    useFieldGroupReducer('temporal', onChange);

    const constraintReducer = function(fields, newFields) {
        let adqlConstraint = '';
        const adqlConstraintErrors = [];

        const siaConstraints = [];
        const siaConstraintErrors = [];
        // Process constraints because we also do validation
        const constraintsResult = makeTemporalConstraints(fields, columnsModel, newFields);
        updatePanelFields(constraintsResult.fieldsValidity, constraintsResult.valid, fields, newFields, panelTitle, panelPrefix);
        if (isPanelChecked(panelTitle, panelPrefix, newFields)) {
            if (constraintsResult.valid){
                if (constraintsResult.adqlConstraint?.length > 0){
                    adqlConstraint = constraintsResult.adqlConstraint;
                } else {
                    adqlConstraintErrors.push(`Unknown error processing ${panelTitle} constraints`);
                }
                if  (constraintsResult.siaConstraints?.length > 0){
                    siaConstraints.push(...constraintsResult.siaConstraints);
                }
            } else if (!constraintsResult.adqlConstraint) {
                logger.warn(`invalid ${panelTitle} adql constraints`);
            }
        }
        return {
            adqlConstraint,
            adqlConstraintErrors,
            siaConstraints,
            siaConstraintErrors
        };
    };


    const DEBUG_OBSCORE = getAppOptions().tapObsCore?.debug ?? false;
    const constraintResult = useConstraintReducer('temporal', constraintReducer);

    return (
        <FieldGroupCollapsible header={<Header title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                        checkID={`${panelPrefix}Check`} message={message}/>}
                               initialState={{ value: 'closed' }}
                               fieldKey={`${panelPrefix}SearchPanel`}
                               wrapperStyle={{marginBottom: 15}}
                               label={Temporal}
                               headerStyle={HeaderFont}>
                <div style={{marginTop: 5, height: 100}}>
                    {showTemporalColumns()}
                    {showTimeRange()}
                </div>
            {DEBUG_OBSCORE && <div>
                adql fragment: {constraintResult?.adqlConstraint} <br/>
                sia fragment: {constraintResult?.siaConstraintErrors?.length ? `Error: ${constraintResult.siaConstraintErrors.join(' ')}` : constraintResult?.siaConstraints}
            </div>}
        </FieldGroupCollapsible>
    );
}

TemporalSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object,
    obsCoreEnabled: PropTypes.bool
};


function selectSpatialSearchMethod(groupKey, fields, spatialMethod) {
    const spatialOptions = () => {
        return TapSpatialSearchMethod.enums.reduce((p, enumItem)=> {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    return (
        <div style={{display: 'flex', flexDirection: 'column'}}>
            <ListBoxInputField
                fieldKey={SpatialMethod}
                options={spatialOptions()}
                wrapperStyle={{marginRight: '15px', padding: '8px 0 5px 0'}}
                multiple={false}
                tooltip={'Select spatial search method'}
                label={'Shape Type:'}
                labelWidth={SpatialLableSaptail}
                initialState={{
                    value: TapSpatialSearchMethod.Cone.value
                }}
            />
            {renderTargetPanel(groupKey, fields, spatialMethod === TapSpatialSearchMethod.Cone.value)}
        </div>
    );
}

ObsCoreSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};

/**
 * render the target panel for cone search
 * @param groupKey
 * @param fields
 * @param visible
 * @returns {null}
 */
function renderTargetPanel(groupKey, fields, visible) {
    const targetSelect = () => {
        return (
            <div style={{height: 70, display:'flex', justifyContent: 'flex-start', alignItems: 'center', marginTop: '5px'}}>
                <TargetPanel labelWidth={LableSaptail} groupKey={groupKey} feedbackStyle={{height: 40}}/>
            </div>
        );
    };
    return (visible) ? targetSelect() : null;
}


/**
 * render the size area for each spatial search type
 * @param fields
 * @param radiusInArcSec
 * @param spatialMethod
 * @returns {*}
 */
function setSpatialSearchSize(fields, radiusInArcSec, spatialMethod) {
    const border = '1px solid #a3aeb9';
    const searchType = spatialMethod;

    if (searchType === TapSpatialSearchMethod.Cone.value) {
        return (
            <div style={{border}}>
                {radiusInField({radiusInArcSec})}
            </div>
        );
    } else if (searchType === TapSpatialSearchMethod.Polygon.value) {
        const imageCornerCalc = get(fields, [ImageCornerCalc, 'value'], 'image');

        return (
            <div style={{marginTop: 5}}>
                {renderPolygonDataArea(imageCornerCalc, SpatialLableSaptail - 1 /* box border */ - 5 /* box padding */ - 5 /* label padding */)}
            </div>
        );
    } else {
        return (
            <div style={{border, padding:'30px 30px', whiteSpace: 'pre-line'}}>
                Search the catalog with no spatial constraints
            </div>
        );
    }
}

/**
 * render size area for cone search
 * @param p
 * @param [p.label]
 * @param [p.radiusInArcSec]
 * @returns {XML}
 */
function radiusInField({label = getLabel(RadiusSize), radiusInArcSec=undefined }) {
    const marginSides = 5;
    return (
        <SizeInputFields fieldKey={RadiusSize} showFeedback={true}
                         wrapperStyle={{padding:5, margin: `${marginSides}px 0px ${marginSides}px 0px`}}
                         initialState={{
                                               unit: 'arcsec',
                             labelWidth : SpatialLableSaptail - marginSides - 1 /* box border width */,
                                               nullAllowed: true,
                                               value: `${(radiusInArcSec||10)/3600}`,
                                               min: 1 / 3600,
                                               max: 100
                                           }}
                         label={label}/>
    );
}

radiusInField.propTypes = {
    label: PropTypes.string,
    radiusInArcSec: PropTypes.number
};


function makeSpatialConstraints(fields, columnsModel, newFields) {
    const opFields = newFields || fields;
    const retval = {valid: true, title: 'spatial search error'};
    let adqlConstraint = '';
    const {centerLonColumns, centerLatColumns, spatialMethod} = fields;
    const fieldsValidity = new Map();

    const checkField = (key, fieldsValidity) => {
        const retval = getFieldValidity(opFields, key, false);
        const field = get(opFields, key);
        const validity =  {valid: retval.valid, message: retval.message};
        if (!retval.valid){
            updateMessage(retval, Spatial, key);
        }
        if (has(field, 'nullAllowed')) {
            validity.nullAllowed = false;
        }
        fieldsValidity.set(key, validity);
        return validity;
    };

    const checkPoint = function(fields, worldSys, adqlCoordSys, fieldsValidity) {
        checkField(ServerParams.USER_TARGET_WORLD_PT, fieldsValidity);
        const fieldValidity = fieldsValidity.get(ServerParams.USER_TARGET_WORLD_PT);
        const worldPt = parseWorldPt(get(fields, [ServerParams.USER_TARGET_WORLD_PT, 'value']));
        let newWpt = {};
        if (worldPt){
            newWpt = convert(worldPt, worldSys);
        } else {
            // Point wasn't actually valid
            fieldValidity.valid = false;
            fieldValidity.message = 'no target found';
        }
        return {...newWpt, ...fieldValidity};
    };

    const checkUserArea = function(spatialMethod, fields, worldSys, adqlCoordSys) {
        const fieldsValidity = new Map();
        const retval = {valid: true, empty: true};
        let userArea;
        const {polygoncoords} = fields;
        if (spatialMethod.value === TapSpatialSearchMethod.Cone.value) {
            const pointResult = checkPoint(fields, worldSys, adqlCoordSys, fieldsValidity);
            checkField(RadiusSize, fieldsValidity);
            const {coneSize} = fields;
            const size = coneSize?.value;
            if (pointResult.valid && size) {
                userArea = `CIRCLE('${adqlCoordSys}', ${pointResult.x}, ${pointResult.y}, ${size})`;
            }
        } else if (spatialMethod.value === TapSpatialSearchMethod.Polygon.value) {
            let polygonValidity = {valid: true, message: ''};
            const splitPairs = polygoncoords?.value.trim().split(',') ?? [];
            const newCorners = splitPairs.reduce((p, onePoint) => {
                const corner = onePoint.trim().split(' ');
                if ((corner.length !== 2) || isNaN(Number(corner[0])) || isNaN(Number(corner[1]))) {
                    polygonValidity = {valid: false, message: 'bad polygon pair'};
                } else {
                    if (p) {
                        const pt = convert(makeWorldPt(Number(corner[0]), Number(corner[1])), worldSys);
                        p.push(pt);
                    } else {
                        p = null;
                    }
                }
                return p;
            }, []);

            if (!newCorners || newCorners.length < 3 || newCorners.length > 15) {
                if (newCorners?.length >= 1) {
                    polygonValidity = {valid: false, message: 'too few or too many corner specified'};
                }
            }

            const cornerStr = newCorners.reduce((p, oneCorner, idx) => {
                p += oneCorner.x + ', ' + oneCorner.y;
                if (idx < (newCorners.length - 1)) {
                    p += ', ';
                }
                return p;
            }, '');
            if (polygonValidity.valid) {
                userArea = `POLYGON('${adqlCoordSys}', ${cornerStr})`;
            }
            fieldsValidity.set('polygoncoords', polygonValidity);
        }
        retval.valid = Array.from(fieldsValidity.values()).every((v) => v.valid);
        retval.userArea = userArea;
        retval.fieldsValidity = fieldsValidity;
        return retval;
    };

    // find ucd coordinate in type of UCDCoord
    const getUCDCoord = (colName) => {
        const ucdVal = getColumnAttribute(columnsModel, colName, 'ucd');

        if (!ucdVal) {
            return UCDCoord.eq;
        } else {
            return UCDCoord.enums.find((enumItem) => (ucdVal.includes(enumItem.key))) || UCDCoord.eq;
        }
    };

    const {spatialRegionOperation} = fields;
    if (centerLatColumns?.mounted) {
        checkField(CenterLonColumns, fieldsValidity);
        checkField(CenterLatColumns, fieldsValidity);
        const ucdCoord = getUCDCoord(centerLonColumns.value);
        const worldSys = posCol[ucdCoord.key].coord;
        const adqlCoordSys = posCol[ucdCoord.key].adqlCoord;
        const point = `POINT('${adqlCoordSys}', ${centerLonColumns.value}, ${centerLatColumns.value})`;
        const userAreaResult = checkUserArea(spatialMethod, fields, worldSys, adqlCoordSys);
        userAreaResult.fieldsValidity.forEach((value, key) => fieldsValidity.set(key, value));
        if (userAreaResult.valid) {
            adqlConstraint = `CONTAINS(${point},${userAreaResult.userArea})=1`;
        }
    } else if (spatialRegionOperation?.mounted){
        const worldSys = CoordinateSys.EQ_J2000;
        const adqlCoordSys = 'ICRS';
        if (spatialRegionOperation.value === 'contains_point') {
            const pointResult = checkPoint(fields, worldSys, adqlCoordSys, fieldsValidity);
            if (pointResult.valid){
                const point = `POINT('${adqlCoordSys}', ${pointResult.x}, ${pointResult.y})`;
                // can only be 'contains'
                adqlConstraint = `CONTAINS(${point}, s_region)=1`;
            }
        } else if (spatialRegionOperation.value === 'contains_shape' || spatialRegionOperation.value === 'contained_by_shape') {
            const contains = spatialRegionOperation.value === 'contains_shape';
            const userAreaResult = checkUserArea(spatialMethod, fields, worldSys, adqlCoordSys);
            userAreaResult.fieldsValidity.forEach((value, key) => fieldsValidity.set(key, value));
            if (userAreaResult.valid) {
                const containedBy = contains ? 's_region' : userAreaResult.userArea;
                const region = contains ? userAreaResult.userArea : 's_region';
                adqlConstraint = `CONTAINS(${region}, ${containedBy})=1`;
            }
        } else if (spatialRegionOperation.value === 'intersects'){
            const userAreaResult = checkUserArea(spatialMethod, fields, worldSys, adqlCoordSys);
            userAreaResult.fieldsValidity.forEach((value, key) => fieldsValidity.set(key, value));
            if (userAreaResult.valid) {
                adqlConstraint = `INTERSECTS(${userAreaResult.userArea}, s_region)=1`;
            }
        } else if (spatialRegionOperation.value === 'center_contained') {
            // Same as non-ObsCore, but with fixed s_ra/s_dec columns
            const point = `POINT('${adqlCoordSys}', s_ra, s_dec)`;
            const userAreaResult = checkUserArea(spatialMethod, fields, worldSys, adqlCoordSys);
            userAreaResult.fieldsValidity.forEach((value, key) => fieldsValidity.set(key, value));
            if (userAreaResult.valid) {
                adqlConstraint = `CONTAINS(${point},${userAreaResult.userArea})=1`;
            }
        }
    }
    // We don't want to say we are valid unless enough components were mounted and checked.
    const enoughMounted = centerLatColumns?.mounted || spatialRegionOperation?.mounted || false;
    retval.fieldsValidity = fieldsValidity;
    retval.valid = Array.from(fieldsValidity.values()).every((validity) => validity.valid) && enoughMounted;
    retval.adqlConstraint = adqlConstraint;
    return retval;
}

function makeTemporalConstraints(fields, columnsModel, newFields) {
    const opFields = newFields || fields;
    const fieldsValidity = new Map();

    const checkField = (key, fieldsValidity, nullAllowed) => {
        const retval = getFieldValidity(opFields, key, nullAllowed);
        const field = get(opFields, key);
        const validity =  {valid: retval.valid, message: retval.message};
        if (!retval.valid){
            updateMessage(retval, Temporal, key);
        }
        if (has(field, 'nullAllowed')) {
            validity.nullAllowed = nullAllowed;
        }
        fieldsValidity.set(key, validity);
        return validity;
    };

    let adqlConstraint = '';
    const columnsValidity = checkField(TemporalColumns, fieldsValidity, false);
    const fromValidity = checkField(TimeFrom, fieldsValidity, true);
    const toValidity = checkField(TimeTo, fieldsValidity, true);

    if (columnsValidity.valid && fromValidity.valid && toValidity.valid){
        const timeColumns = get(opFields, [TemporalColumns, 'value'], '').trim().split(',').reduce((p, c) => {
            if (c.trim()) p.push(c.trim());
            return p;
        }, []);

        const mjdRange = [TimeFrom, TimeTo].map((timeKey) => {
            return get(opFields, [timeKey, MJD, 'value'], '');
        });

        if (mjdRange[FROM] && mjdRange[TO]) {
            if (Number(mjdRange[FROM]) > Number(mjdRange[TO])) {
                toValidity.valid = false;
                toValidity.message = 'the start time is greater than the end time';
            }
        }

        if (timeColumns.length === 1) {
            const timeColumn = timeColumns[0];

            // use MJD if time column has a numeric type, ISO otherwise
            const datatype = getColumnAttribute(columnsModel, timeColumn, 'datatype').toLowerCase();
            // ISO types: char, varchar, timestamp, ?
            const useISO = !['double', 'float', 'real', 'int', 'long', 'short'].some((e) => datatype.includes(e));

            let timeRange = mjdRange;
            if (useISO) {
                timeRange = mjdRange.map((value) => convertMJDToISO(value));
            }

            const timeConstraints = timeRange.map((oneRange, idx) => {
                if (!oneRange) {
                    return '';
                } else {
                    const limit  = useISO ? `'${oneRange}'` : oneRange;
                    return idx === FROM ? `${timeColumn} >= ${limit}` : `${timeColumn} <= ${limit}`;
                }
            });

            if (!timeConstraints[FROM] || !timeConstraints[TO]) {
                adqlConstraint = timeConstraints[FROM] + timeConstraints[TO];
            } else {
                adqlConstraint =`(${timeConstraints[FROM]} AND ${timeConstraints[TO]})`;
            }
        }
    }
    const allValid = Array.from(fieldsValidity.values()).every((validity) => {return validity.valid;});
    return {
        valid: allValid,
        fieldsValidity,
        adqlConstraint,
    };
}

/**
 * compose constraints for ADQL where clause
 * @param {object} columnsModel
 * @returns {AdqlFragment}
 */
export function tableSearchMethodsConstraints(columnsModel) {
    // construct ADQL string here
    const fields = FieldGroupUtils.getGroupFields(skey);
    const {constraintResults} = fields;
    if (constraintResults){
        return {valid: true, where: constraintResults.adqlConstraints.join(' AND ')};
    }
    return {valid: true};
}

function buildTapSearchMethodReducer(columnsModel) {
    return (inFields, action) => {

        if (!inFields)  {
             return fieldInit(columnsModel);
        } else {
            const rFields = clone(inFields);

            const onResetColumnsTable = () => {
                const cols = getAvailableColumns(columnsModel);
                set(rFields, [CrtColumnsModel, 'value'], columnsModel.tbl_id );
                const centerColObj = formCenterColumns(columnsModel);
                Object.assign(rFields[CenterLonColumns],
                                       {validator: getColValidator(cols, false, false), value: centerColObj.lon, valid: true});
                Object.assign(rFields[CenterLatColumns],
                                       {validator: getColValidator(cols, false, false), value: centerColObj.lat, valid: true});

                Object.assign(rFields[TemporalColumns],
                                        {validator: getColValidator(cols, false, false), value: '', valid: true});

                // validateTemporalConstraints(inFields, rFields);
            };


            switch (action.type) {

                case FieldGroupCntlr.VALUE_CHANGE:
                    Array.from(chainedReducers.values()).forEach((chainedReducer) => {
                        chainedReducer(inFields, action, rFields);
                    });
                    const adqlConstraints = [];
                    const adqlConstraintErrorsArray = [];
                    const siaConstraints = [];
                    const siaConstraintErrorsArray = [];
                    // adqlComponents can apparently be modified during iteration in the forEach...
                    Array.from(constraintReducers.adqlReducerMap.values()).forEach((constraintReducer) => {
                        const constraintResult = constraintReducer(inFields, rFields);
                        if (!constraintResult.adqlConstraintErrors?.length) {
                            if (constraintResult.adqlConstraint) {
                                adqlConstraints.push(constraintResult.adqlConstraint);
                            }
                        } else {
                            adqlConstraintErrorsArray.push(constraintResult.adqlConstraintErrors);
                        }
                        if (!constraintResult.siaConstraintErrors?.length) {
                            if (constraintResult.siaConstraints?.length > 0) {
                                siaConstraints.push(...constraintResult.siaConstraints);
                            }
                        } else {
                            siaConstraintErrorsArray.push(constraintResult.siaConstraintErrors);
                        }
                    });
                    const constraintResults = {
                        adqlConstraints,
                        adqlConstraintErrors: adqlConstraintErrorsArray,
                        siaConstraints,
                        siaConstraintErrors: siaConstraintErrorsArray
                    };
                    rFields[ConstraintResults] = constraintResults;
                    break;
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    if (columnsModel.tbl_id !== get(inFields, [CrtColumnsModel, 'value'])) {
                        onResetColumnsTable();
                    }
                    break;
                default:
                    logger.warn(`Unhandled field group action: ${action.type}`);
            }
            return rFields;
        }
    };
}


/**
 * Assembles an array of objects with column attributes in the format that ColumnFld accepts
 * @param columnsModel
 */
function getAvailableColumns(columnsModel){
    const attrIdx  = [
        //[<column name in columnModel>, <column attribute name ColumnFld wants>]
        ['column_name', 'name'],
        ['unit', 'units'],
        ['datatype', 'type'],
        ['ucd', 'ucd'],
        ['description', 'desc']
    ].map(([c,k])=>[k,getColumnIdx(columnsModel, c)]);

    return get(columnsModel, ['tableData', 'data'], []).map((r) => {
        const col = {};
        attrIdx.forEach(([k,i]) => { col[k] = r[i]; });
        return col;
    });
}

function timeValidator(val) {
    const timeMode = getFieldVal(skey, TimeOptions) || ISO;
    return (timeMode === MJD) ? validateMJD(val) : validateDateTime(val);
}

function formCenterColumns(columnsTable) {
    const centerCols = findCenterColumnsByColumnsModel(columnsTable);
    return (centerCols && centerCols.lonCol && centerCols.latCol) ?
        {lon: centerCols.lonCol.column_name, lat: centerCols.latCol.column_name} : {lon: '', lat: ''};
}

const getFieldValidity  = (fields, fieldKey, nullAllowed) => {
    const {valid=true, message, value, displayValue} = get(fields, fieldKey) || {};
    const val = displayValue || value;
    const rVal = val&&(typeof val === 'string') ? val.trim() : val;


    // if nullAllowed is undefined, just pass valid & message as assigned
    if (isUndefined(nullAllowed) || rVal) {
        return {valid, message: (valid ? '' : (message || 'entry error'))};
    } else if (!rVal) {
        return {valid: nullAllowed, message: !nullAllowed ? 'empty entry' : ''};
    }
};

const updateMessage = (retval, panel, key) => {
    const keyInfo = get(fieldsMap, [panel, key], null);

    if (keyInfo) {
        retval.message = `field '${keyInfo.label}': ${retval.message}`;
    }
    return retval;
};

function fieldInit(columnsTable) {
    return (
        {
            [ImageCornerCalc]: {
                fieldKey: ImageCornerCalc,
                value: 'image'
            },
            [CrtColumnsModel]: {
                fieldKey: CrtColumnsModel,
                value: columnsTable.tbl_id
            },
            [PolygonCorners]: {
                fieldKey: PolygonCorners,
                value: ''
            },
            [DatePickerOpenStatus]: {
                fieldKey: DatePickerOpenStatus,
                value: [false, false]
            },
        }
    );
}
