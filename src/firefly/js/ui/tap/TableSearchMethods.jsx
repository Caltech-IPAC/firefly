import React,  {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {get, set} from 'lodash';
import Enum from 'enum';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {getColumnValues} from '../../tables/TableUtil.js';
import {findCenterColumnsByColumnsModel, posCol, UCDCoord} from '../../util/VOAnalyzer.js';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {ServerParams} from '../../data/ServerParams.js';
import {getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {convert} from '../../visualize/VisUtil.js';
import {primePlot, getActivePlotView} from '../../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../../visualize/WebPlot.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {renderPolygonDataArea,  calcCornerString, initRadiusArcSec} from '../CatalogSearchMethodType.jsx';
import {clone} from '../../util/WebUtil.js';
import {ValidationField} from '../ValidationField.jsx';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {validateMJD, validateDateTime, convertISOToMJD, convertMJDToISO, DateTimePickerField, fMoment,
        tryConvertToMoment} from '../DateTimePickerField.jsx';
import {TimePanel, isShowHelp, formFeedback} from '../TimePanel.jsx';
import {showInfoPopup, showOptionsPopup, POPUP_DIALOG_ID} from '../PopupUtil.jsx';
import {isDialogVisible} from '../../core/ComponentCntlr.js';
import {HeaderFont, MJD, ISO} from './TapUtil.js';
import {HelpIcon} from '../HelpIcon.jsx';
import {tapHelpId} from './TableSelectViewPanel.jsx';

const skey = 'TABLE_SEARCH_METHODS';
const CenterColumns = 'centerColumns';
const SpatialMethod = 'spatialMethod';
const RadiusSize = 'coneSize';
const PolygonCorners = 'polygoncoords';
const ImageCornerCalc = 'imageCornerCalc';
const TemporalColumns = 'temporalColumns';
const WavelengthColumns = 'wavelengthColumns';
const CrtColumnsModel = 'crtColumnsModel';
const DatePickerOpenStatus = 'datePickerOpenStatus';
const TimePickerFrom = 'timePickerFrom';
const TimePickerTo = 'timePickerTo';
const MJDFrom = 'mjdfrom';
const MJDTo = 'mjdto';
const TimeFrom = 'timeFrom';
const TimeTo = 'timeTo';
const TimeOptions = 'timeOptions';
const LeftInSearch = 8;
const LabelWidth = 106;


const TapSpatialSearchMethod = new Enum({
    'Cone': 'Cone',
    'Polygon': 'Polygon',
    'All Sky': 'AllSky'
});

import '../CatalogSearchMethodType.css';
import './react-datetime.css';

const  FROM = 0;
const  TO  = 1;
const  DateTimePicker = 'datePicker';
const  timeKeyMap = {
                     [TimeFrom]: {
                         [MJD]: MJDFrom,
                         [ISO]: TimePickerFrom,
                         title: 'From'
                     },
                     [TimeTo]: {
                         [MJD]: MJDTo,
                         [ISO]: TimePickerTo,
                         title: 'To'
                     }
                };

// size used
const Width_Time = 175;
const Width_Time_Wrapper = Width_Time+30;


function Header({title, helpID=''}) {
    return (
        <div style={{display: 'inline-flex', alignItems: 'center'}}>
            <div style={{...HeaderFont, marginRight: 5}}>{title}</div>
            <HelpIcon helpId={helpID}/>
        </div>
    );
}
Header.propTypes = {
    title: PropTypes.string,
    helpID: PropTypes.string
};

export class TableSearchMethods extends PureComponent {
    constructor(props) {
        super(props);
        this.state = Object.assign(this.nextState(props));
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    storeUpdate() {
        if (this.iAmMounted) {
            this.setState(this.nextState());
        }
    }


    nextState(props) {
        const fields = FieldGroupUtils.getGroupFields(skey);
        const columnsModel =  props ? props.columnsModel : getTblById(get(fields, [CrtColumnsModel, 'value']));

        return {fields, columnsModel};
    }

    render() {
        const groupKey = skey;
        const {fields, columnsModel} = this.state;

        return (
            <FieldGroup style={{height: '100%', overflow: 'auto'}}
                groupKey={skey} keepState={true} reducerFunc={tapSearchMethodReducer(columnsModel)}>
                    <SpatialSearch {...{groupKey, fields}} />
                    <TemporalSearch {...{groupKey, fields}} />
                    <WavelengthSearch {...{groupKey, fields}} />
            </FieldGroup>
        );
    }
}


const showTimePicker = (loc, show) => {
    const timeKey = loc === FROM ? TimeFrom : TimeTo;
    const title = loc === FROM ? 'select "from" time' : 'select "to" time';
    const valueInfo = getTimeValueInfo(ISO, skey, timeKey);
    const pickerKey = get(timeKeyMap, [timeKey, ISO]);

    const content = (
            <DateTimePickerField fieldKey={pickerKey}
                                 groupKey={skey}
                                 showInput={false}
                                 openPicker={true}
                                 value={valueInfo.value}
                                 inputStyle={{marginBottom: 3}}
            />);

    showOptionsPopup({content, title, modal: true, show});
};



function changeDatePickerOpenStatus(loc) {
    return () => {
        const show = !isDialogVisible(POPUP_DIALOG_ID);

        showTimePicker(loc, show);
    };
}

function SpatialSearch({groupKey, fields}) {
    const showCenterColumns = () => {
        return (
            <div style={{marginLeft: LeftInSearch}}>
                <ValidationField fieldKey={CenterColumns}
                                 style={{overflow:'auto', height:12, width: 200}}
                />
            </div>
        );
    };

    const doSpatialSearch = () => {
        return (
            <div style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center', marginTop: 5}}>
                {selectSpatialSearchMethod(groupKey, fields)}
                {setSpatialSearchSize(fields)}
            </div>
        );
    };


    return (
        <FieldGroupCollapsible header={<Header title={'Spatial'}  helpID={tapHelpId('spatial')}/>}
                               initialState={{ value: 'open' }}
                               fieldKey='spatialSearchPanel'
                               headerStyle={HeaderFont}>
            <div style={{marginTop: 5}}>
                {showCenterColumns()}
                {doSpatialSearch()}
            </div>
        </FieldGroupCollapsible>
    );
}

SpatialSearch.propTypes = {
    groupKey: PropTypes.string,
    fields: PropTypes.object
};


function getTimeValueInfo(timeMode, groupKey, timeFieldKey) {
    const timeKey = get(timeKeyMap, [timeFieldKey, timeMode]);
    const fields = FieldGroupUtils.getGroupFields(groupKey);
    const {value='', valid=true, message=''} = get(fields, timeKey);

    return {value, valid, message};
}


function TemporalSearch({groupKey, fields}) {
    const showTemporalColumns = () => {
        return (
            <div style={{marginLeft: LeftInSearch}}>
                <ValidationField fieldKey={TemporalColumns}
                                 style={{overflow:'auto', height:12, width: 200}}
                />
            </div>
        );
    };

    const showTimeRange = () => {
        const timeOptions = [{label: 'ISO', value: ISO},
                             {label: 'MJD', value: MJD}];
        const crtTimeMode = get(fields, [TimeOptions, 'value'], ISO);
        const fromValInfo = getTimeValueInfo(crtTimeMode, skey, TimeFrom);
        const toValInfo = getTimeValueInfo(crtTimeMode, skey, TimeTo);
        const icon = crtTimeMode === ISO ? 'calendar' : '';

        //  radio field is styled with padding right in consistent with the label part of 'temporal columns' entry
        return (
            <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5}}>
                <RadioGroupInputField fieldKey={TimeOptions}
                                      options={timeOptions}
                                      alignment={'horizontal'}
                                      wrapperStyle={{width: LabelWidth, paddingRight:'4px'}}
                />
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeFrom}
                                 groupKey={skey}
                                 timeMode={crtTimeMode}
                                 value={fromValInfo.value}
                                 valid={fromValInfo.valid}
                                 message={fromValInfo.message}
                                 icon={icon}
                                 onClickIcon={changeDatePickerOpenStatus(FROM)}
                                 feedbackStyle={{height: 100}}
                                 tooltip={'select a from time'}
                                 inputWidth={Width_Time}
                                 inputStyle={{overflow:'auto', height:16}}
                    />
                </div>
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeTo}
                               groupKey={skey}
                               timeMode={crtTimeMode}
                               value={toValInfo.value}
                               valid={toValInfo.valid}
                               message={toValInfo.message}
                               icon={icon}
                               onClickIcon={changeDatePickerOpenStatus(TO)}
                               feedbackStyle={{height: 100}}
                               tooltip={'select a from time'}
                               inputWidth={Width_Time}
                               inputStyle={{overflow:'auto', height:16}}
                    />
               </div>
            </div>
        );
    };

    return (
        <FieldGroupCollapsible header={<Header title={'Temporal'} helpID={tapHelpId('temporal')}/>}
                               initialState={{ value: 'closed' }}
                               fieldKey='temporalSearchPanel'
                               headerStyle={HeaderFont}>
                <div style={{marginTop: 5, height: 100}}>
                    {showTemporalColumns()}
                    {showTimeRange()}
                </div>
        </FieldGroupCollapsible>
    );
}


TemporalSearch.propTypes = {
   groupKey: PropTypes.string,
   fields: PropTypes.object
};



function WavelengthSearch({groupKey, fields}) {
    const showWavelengthColumns = () => {
        return (
            <div style={{marginLeft: 8}}>
                <ValidationField fieldKey={WavelengthColumns}
                                 initialState={{ label: 'Wavelength Columns:',
                                                 labelWidth: LabelWidth,
                                                 tooltip: 'Columns for wavelength search'
                                           }}
                                 style={{overflow:'auto', height:12, width: 200}}
                />
            </div>
        );
    };


    return (
        <FieldGroupCollapsible header={<Header title={'Wavelength'} helpID={tapHelpId('wavelength')}/>}
                               initialState={{ value: 'closed' }}
                               fieldKey='wavelengthSearchPanel'
                               headerStyle={HeaderFont}>
            <div style={{width: 100, height: 50}}>
                {showWavelengthColumns()}
            </div>
        </FieldGroupCollapsible>
    );
}
WavelengthSearch.propTypes = {
    groupKey: PropTypes.string,
    fields: PropTypes.object
};


function selectSpatialSearchMethod(groupKey, fields) {
    const spatialOptions = () => {
        return TapSpatialSearchMethod.enums.reduce((p, enumItem)=> {
            p.push({label: enumItem.key, value: enumItem.value});
            return p;
        }, []);
    };

    const spatialSearchList = (
        <div style={{display:'flex', flexDirection:'column', alignItems:'center'}}>
            <ListBoxInputField
                fieldKey={SpatialMethod}
                options={ spatialOptions()}
                wrapperStyle={{marginRight:'15px', padding:'8px 0 5px 0'}}
                multiple={false}
            />
            {renderTargetPanel(groupKey, fields)}
        </div>
    );
    return spatialSearchList;
}

/**
 * render the target panel for cone search
 * @param groupKey
 * @param fields
 * @returns {null}
 */
function renderTargetPanel(groupKey, fields) {
    const searchType = get(fields, [SpatialMethod, 'value'], TapSpatialSearchMethod.Cone.value);
    const visible = (searchType === TapSpatialSearchMethod.Cone.value);
    const targetSelect = () => {
        return (
            <div style={{height: 70, display:'flex', justifyContent: 'flex-start', alignItems: 'center'}}>
                <TargetPanel labelWidth={100} groupKey={groupKey} feedbackStyle={{height: 40}}/>
            </div>
        );
    };
    return (visible) ? targetSelect() : null;
}


/**
 * render the size area for each spatial search type
 * @param fields
 * @returns {*}
 */
function setSpatialSearchSize(fields) {
    const border = '1px solid #a3aeb9';
    const searchType = get(fields, [SpatialMethod, 'value'], TapSpatialSearchMethod.Cone.value);

    if (searchType === TapSpatialSearchMethod.Cone.value) {
        return (
            <div style={{border}}>
                {radiusInField({})}
            </div>
        );
    } else if (searchType === TapSpatialSearchMethod.Polygon.value) {
        const imageCornerCalc = get(fields, [ImageCornerCalc, 'value'], 'image');

        return renderPolygonDataArea(imageCornerCalc);
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
 * @param label
 * @returns {XML}
 */
function radiusInField({label = 'Radius:' }) {
    return (
        <SizeInputFields fieldKey={RadiusSize} showFeedback={true}
                         wrapperStyle={{padding:5, margin: '5px 0px 5px 0px'}}
                         initialState={{
                                               unit: 'arcsec',
                                               labelWidth : 100,
                                               nullAllowed: false,
                                               value: initRadiusArcSec(3600),
                                               min: 1 / 3600,
                                               max: 100
                                           }}
                         label={label}/>
    );
}

radiusInField.propTypes = {
    label: PropTypes.string
};


function makeSpatialConstraints(fields, columnsModel) {
    const NOConstraint = '';
    const retval = {valid: true, where: '', title: 'spatial search error'};
    const {centerColumns, spatialMethod} = fields;

    // find column pairs [[pair_1_ra, pair_1_dec]...[pair_n_ra, pair_n_dec]]
    const makeColPairs = () => {
        return centerColumns.value.split('\n').reduce((p, onePair) => {
            const oneP = onePair.trim();
            const pair = oneP && oneP.split(',').reduce((prev, n) => {
                    if (n.trim()) prev.push(n.trim());
                    return prev;
                }, []);
            if (pair && pair.length === 2) {
                p.push([pair[0], pair[1]]);
            }
            return p;
        }, []);
    };

    // find ucd coordinate in type of UCDCoord
    const getUCDCoord = (colName) => {
        const nameIdx = getColumnIdx(columnsModel, 'column_name');
        const ucdIdx = getColumnIdx(columnsModel, 'ucd');
        if (nameIdx < 0 || ucdIdx < 0) {
            return UCDCoord.eq;
        }
        const targetRow = get(columnsModel, ['tableData', 'data'], []).find((oneRow) => {
                return (oneRow[nameIdx] === colName);
            });


        if (!targetRow) {
            return UCDCoord.eq;
        }

        const ucdVal = targetRow[ucdIdx];
        return UCDCoord.enums.find((enumItem) => (ucdVal.includes(enumItem.key))) || UCDCoord.eq;

    };

    const pairs = makeColPairs();
    if (pairs.length !== 1) {
        return retval;
    }

    const ucdCoord = getUCDCoord(pairs[0][0]);
    const worldSys = posCol[ucdCoord.key].coord;
    const adqlCoordSys = posCol[ucdCoord.key].adqlCoord;
    const point = `POINT('${adqlCoordSys}', ${pairs[0][0]}, ${pairs[0][1]})`;
    let   userArea;
    let   newWpt;

    if (spatialMethod.value === TapSpatialSearchMethod.Cone.value) {
        const {coneSize} = fields;
        const worldPt = parseWorldPt(get(fields, [ServerParams.USER_TARGET_WORLD_PT, 'value']));
        const size = coneSize.value;

        if (!worldPt || !size) {
            return retval;
        }

        newWpt = convert(worldPt, worldSys);
        userArea = `CIRCLE('${adqlCoordSys}', ${newWpt.x}, ${newWpt.y}, ${size})`;

    } else if (spatialMethod.value === TapSpatialSearchMethod.Polygon.value) {
        const {polygoncoords} = fields;

        if (!polygoncoords.value) {
            return retval;
        }

        const newCorners = polygoncoords.value.trim().split(',').reduce((p, onePoint) => {
            const corner = onePoint.trim().split(' ');

            if (p && corner.length === 2) {
                const pt = convert(makeWorldPt(Number(corner[0]), Number(corner[1])), worldSys);
                p.push(pt);
            } else {
                p = null;
            }
            return p;
        }, []);

        if (!newCorners || newCorners.length < 3 || newCorners.length > 15) {
            if (newCorners && newCorners.length >= 1) {
                retval.valid = false;
                retval.message = 'too few or too many corner specified';
                return retval;
            } else {
                return retval;
            }
        }

        const cornerStr = newCorners.reduce((p, oneCorner, idx) => {
            p += oneCorner.x + ', ' + oneCorner.y;
            if (idx < (newCorners.length -1)) {
                p += ', ';
            }
            return p;
        }, '');

        userArea = `POLYGON('${adqlCoordSys}', ${cornerStr})`;
    } else {
        userArea = NOConstraint;
    }

    retval.where = userArea ? `CONTAINS(${point},${userArea})=1`:userArea;
    return retval;
}

function makeTemporalConstraints(fields) {
    const retval = {valid: true, where: '', title: 'temporal search error'};
    const timeColumns = get(fields, [TemporalColumns, 'value'], '').trim().split(',').reduce((p, c) => {
        if (c.trim()) p.push(c.trim());
        return p;
    }, []);

    const mjdRange = [TimeFrom, TimeTo].map((timeKey) => {
        const mjdKey =  timeKeyMap[timeKey][MJD];
        return get(fields, [mjdKey, 'value'], '');
    });


    if (mjdRange[FROM] && mjdRange[TO]) {
        if (Number(mjdRange[FROM]) > Number(mjdRange[TO])) {
            retval.valid = false;
            retval.message = 'the start time is greater than the end time';
            return retval;
        }
    }

    let where = '';
    if (timeColumns.length === 1) {
        const mjdSqls = mjdRange.map((oneRange, idx) => {
            if (!oneRange) {
                return '';
            } else {
                return idx === FROM ? `${timeColumns[0]} >= ${oneRange}` : `${timeColumns[0]} <= ${oneRange}`;
            }
        });

        if (!mjdSqls[FROM] || !mjdSqls[TO]) {
            where = mjdSqls[FROM] + mjdSqls[TO];
        } else {
            where =`(${mjdSqls[FROM]} AND ${mjdSqls[TO]})`;
        }
    }
    retval.where = where;
    return retval;
}

/**
 * Get constraints as ADQL
 * @param {object} columnsModel
 * @returns {string}
 */
export function tableSearchMethodsConstraints(columnsModel) {
    // construct ADQL string here
    const fields = FieldGroupUtils.getGroupFields(skey);

    let   adql;
    const validInfo = validateConstraints(fields);
    if (!validInfo.valid) {
         adql = {valid: false, message: validInfo.message, title: 'constraints entry error'};
         showInfoPopup(adql.message, adql.title);
         return adql;
    }

    const spatialConstraints = makeSpatialConstraints(fields, columnsModel);
    const timeConstraints = makeTemporalConstraints(fields);

    adql = [spatialConstraints, timeConstraints].reduce((p, oneCondition) => {
        p.valid = p.valid && oneCondition.valid;
        if (!p.valid) {
            p.where = '';
            if (oneCondition.message && !p.message) {
                p.message = oneCondition.message;
                p.title = oneCondition.title;
            }
        } else {
            if (p.where && oneCondition.where) {
                p.where += ' AND ' + oneCondition.where;
            } else {
                p.where += oneCondition.where;
            }
        }

        return p;

    }, {where: '', valid: true, message: '', title: ''});

    if (!adql.valid) {
        showInfoPopup((adql.message || 'search constraints error'),  (adql.title ||'search error'));
    }

    return adql;
}

function tapSearchMethodReducer(columnsModel) {
    return (inFields, action) => {

        if (!inFields)  {
             return fieldInit(columnsModel);
        } else {
            const {fieldKey, value}= action.payload;
            const rFields = clone(inFields);

            const onChangePolygonCoordinates = () => {
                rFields.imageCornerCalc = clone(inFields.imageCornerCalc, {value: 'user'});
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

            const onChangeTimeField = () => {
                // only update picker & mjd when there is no pop-up picker (time input -> picker or mjd)
                if (!isDialogVisible(POPUP_DIALOG_ID)) {
                    const {valid, message} = get(inFields, fieldKey, {});
                    const timeMode = get(inFields, [TimeOptions, 'value']);
                    const mjdKey = get(timeKeyMap, [fieldKey, MJD]);
                    const isoKey = get(timeKeyMap, [fieldKey, ISO]);
                    const updateValue = timeMode === MJD ? value : (valid ? fMoment(tryConvertToMoment(value)): value);

                    // convert value for the other time mode
                    const crtFieldKey = timeMode === MJD ? mjdKey : isoKey;
                    const convertFunc = timeMode === MJD ? convertMJDToISO : convertISOToMJD;
                    const validateFunc = timeMode === MJD ? validateDateTime : validateMJD;
                    const secondFieldKey = timeMode === MJD ? isoKey : mjdKey;

                    const secondVal = convertFunc(updateValue);
                    const secondValInfo = validateFunc(secondVal);

                    rFields[crtFieldKey] = clone(inFields[crtFieldKey], {value: updateValue, valid, message});
                    rFields[secondFieldKey] = clone(inFields[secondFieldKey], {
                        value: secondValInfo.value,
                        valid: secondValInfo.valid,
                        message: secondValInfo.message
                    });
                }
            };

            const onChangeTimeMode = () => {
                // update Time fields (from and to) and the feedback below based on the selected mode, iso or mjd
                const timeInfos = [TimeFrom, TimeTo].map((tKey) => {
                    return [ISO, MJD].reduce((prev, option) => {
                        const valKey = get(timeKeyMap, [tKey, option]);
                        prev[option] = get(inFields, [valKey]);

                        return prev;
                    }, {});
                });
                const timeMode = value;

                [TimeFrom, TimeTo].forEach((timeKey, idx) => {
                    const showHelp = isShowHelp(timeInfos[idx][ISO].value, timeInfos[idx][MJD].value);
                    const feedback = formFeedback(timeInfos[idx][ISO].value, timeInfos[idx][MJD].value);

                    rFields[timeKey] = clone(inFields[timeKey], { value: timeInfos[idx][timeMode].value,
                        valid: timeInfos[idx][timeMode].valid,
                        message: timeInfos[idx][timeMode].message,
                        showHelp, feedback, timeMode});
                });
            };

            const onChangeDateTimePicker = () => {
                // update MJD & TimeFrom (TimeTo) when there is pop-up picker (picker -> time field & mjd)
                if (isDialogVisible(POPUP_DIALOG_ID)) {
                    const {valid, message} = get(inFields, fieldKey) || {};
                    const timeKey = (fieldKey === TimePickerFrom) ? TimeFrom : TimeTo;
                    const mjdKey = get(timeKeyMap, [timeKey, 'mjd']);

                    const mjdVal = convertISOToMJD(value);
                    const mjdInfo = validateMJD(mjdVal);

                    rFields[mjdKey] = clone(inFields[mjdKey], {
                        value: mjdInfo.value, valid: mjdInfo.valid,
                        message: mjdInfo.message
                    });
                    const showHelp = isShowHelp(value, mjdInfo.value);
                    const feedback = formFeedback(value, mjdInfo.value);

                    rFields[timeKey] = clone(inFields[timeKey], {value, message, valid, showHelp, feedback,
                        timeMode: ISO});
                }
            };

            switch (action.type) {
                case FieldGroupCntlr.VALUE_CHANGE:
                    if (fieldKey === PolygonCorners) {
                        onChangePolygonCoordinates(rFields);
                    } else if (fieldKey === SpatialMethod && value === TapSpatialSearchMethod.Polygon.key) {
                        onChangeToPolygonMethod();
                    } else if (fieldKey === TimeFrom || fieldKey === TimeTo) { // update mjd & picker
                        onChangeTimeField();
                    } else if (fieldKey === TimeOptions) {    // time mode changes => update timefrom and timeto value
                        onChangeTimeMode();
                    } else if (fieldKey === TimePickerFrom || fieldKey === TimePickerTo) { // update mjd & time fields
                        onChangeDateTimePicker();
                    }

                    break;
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    if (columnsModel.tbl_id !== get(inFields, [CrtColumnsModel, 'value'])) {
                        set(rFields, [CrtColumnsModel, 'value'], columnsModel.tbl_id );
                        const centerColStr = formCenterColumnStr(columnsModel);
                        set(rFields, [CenterColumns, 'validator'], centerColumnValidator(columnsModel));
                        set(rFields, [TemporalColumns, 'validator'], temporalColumnValidator(columnsModel));
                        set(rFields, [CenterColumns, 'value'], centerColStr);
                        set(rFields, [TemporalColumns, 'value'], '');

                    }
                    break;
            }

            return rFields;
        }
    };
}


function timeValidator(val) {
    const timeMode = getFieldVal(skey, TimeOptions) || ISO;
    return (timeMode === MJD) ? validateMJD(val) : validateDateTime(val);
}


function formCenterColumnStr(columnsTable) {
    const centerCols = findCenterColumnsByColumnsModel(columnsTable);
    const centerColStr = (centerCols && centerCols.lonCol && centerCols.latCol) ?
                         centerCols.lonCol.column_name + ',' + centerCols.latCol.column_name: '';

    return centerColStr;
}

/**
 * center column validator
 * @param columnsTable
 * @returns {Function}
 */
function centerColumnValidator(columnsTable) {
    return (val) => {
        let   retval = {valid: true, message: ''};
        const columnNames = getColumnValues(columnsTable, 'column_name');
        const names = val.trim().split(',').map((n) => n.trim());

        if (val.trim() && (names.length !== 2 || (!names[0] || !names[1]))) {
            retval = {valid: false,
                      message: "invalid format, please enter the column pair like '<column name 1>,<column name 2>'"};
        } else {
            const invalidName = names.find((oneName) => {
                return !columnNames.includes(oneName.trim());
            });
            if (invalidName) {
                retval = {valid: false, message: 'invalid column name: ' + invalidName};
            }
        }
        return retval;
    };
}


/**
 * temporal column validator
 * @param columnsTable
 * @returns {Function}
 */
function temporalColumnValidator(columnsTable) {
    return (val) => {
        let   retval = {valid: true, message: ''};
        const columnNames = getColumnValues(columnsTable, 'column_name');
        const names = val.trim().split(',').map((n) => n.trim());

        if (val.trim() && names.length >= 2) {
            retval = {valid: false,
                      message: 'please enter a single column name like <column name>'};
        } else {
            const invalidName = names.find((oneName) => {
                return oneName && !columnNames.includes(oneName);
            });
            if (invalidName) {
                retval = {valid: false, message: 'invalid column name: ' + invalidName};
            }
        }
        return retval;
    };
}

/**
 * validate the constraint entry
 * @param fields
 * @returns {*}
 */
function validateConstraints(fields) {
    // spatial constraints
    const checkField = (fieldKey) => {
       const {valid=true, message, feedback} = get(fields, fieldKey) || {};

        if (!valid) {
            return {valid, message: (message ||  feedback || 'entry error')};
        } else {
            return {valid};
        }
    };

    let retval = checkField(CenterColumns);
    if (!retval.valid) return retval;

    const searchMethod = get(fields, [SpatialMethod, 'value']);

    if (searchMethod ===  TapSpatialSearchMethod.Cone.value) {
        retval = checkField(ServerParams.USER_TARGET_WORLD_PT);
        if (!retval.valid) return retval;

        retval = checkField(RadiusSize);
        if (!retval.valid) return retval;
    } else if (searchMethod === TapSpatialSearchMethod.Polygon.value) {
        const {polygoncoords} = fields;

        // check any polygon pair is valid
        if (polygoncoords.value) {
            const badCorner = polygoncoords.value.trim().split(',').find((onePoint) => {
                const corner = onePoint.trim().split(' ');

                return (corner.length !== 2) || isNaN(Number(corner[0])) || isNaN(Number(corner[1]));
            });

            if (badCorner) {
                retval.valid = false;
                retval.message = 'wrong corner pair';
                return retval;
            }
        }
    }

    // check temporal
    retval = checkField(TemporalColumns);
    if (!retval.valid) return retval;

    retval = checkField(TimeFrom);
    if (!retval.valid) return retval;

    retval = checkField(TimeTo);
    if (!retval.valid) return retval;

    return {valid: true};
}

function fieldInit(columnsTable) {
    const centerColStr = formCenterColumnStr(columnsTable);

    return (
        {
            [CenterColumns]: {
                fieldKey: CenterColumns,
                value: centerColStr,
                tooltip: "Center columns for spatial search, the format is like '<column name 1>,<column name 2>'",
                label: 'Position Columns:',
                labelWidth: LabelWidth,
                validator: centerColumnValidator(columnsTable)
            },
            [TemporalColumns]: {
                fieldKey: TemporalColumns,
                value: '',
                tooltip: 'columns for temporal search, multiple column names are separated by ","',
                label: 'Temporal Column:',
                labelWidth: LabelWidth,
                validator: temporalColumnValidator(columnsTable)
            },
            [SpatialMethod]: {
                fieldKey: SpatialMethod,
                tooltip: 'Enter a search method',
                label: 'Search Method:',
                labelWidth: 80,
                value: TapSpatialSearchMethod.Cone.value
            },
            [ImageCornerCalc]: {
                fieldKey: ImageCornerCalc,
                value: 'image'
            },
            [CrtColumnsModel]: {
                fieldKey: CrtColumnsModel,
                value: columnsTable.tbl_id
            },
            [DatePickerOpenStatus]: {
                fieldKey: DatePickerOpenStatus,
                value: [false, false]
            },
            [TimePickerFrom]: {
                fieldKey: TimePickerFrom,
                value: ''
            },
            [TimePickerTo]: {
                fieldKey: TimePickerTo,
                value: ''
            },
            [TimeOptions]: {
                fieldKey: TimeOptions,
                value: DateTimePicker
            },
            [MJDFrom]: {
                fieldKey: MJDFrom,
                value: ''
            },
            [MJDTo]: {
                fieldKey: MJDTo,
                value: ''
            },
            [TimeFrom]:{
                fieldKey: TimeFrom,
                value: '',
                validator: timeValidator
            },
            [TimeTo]:{
                fieldKey: TimeTo,
                value: '',
                validator: timeValidator
            }
        }
    );
}
