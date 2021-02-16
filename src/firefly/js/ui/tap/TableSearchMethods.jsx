import React,  {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../core/ReduxFlux.js';
import {get, set, isUndefined, has} from 'lodash';
import Enum from 'enum';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {findCenterColumnsByColumnsModel, posCol, UCDCoord} from '../../util/VOAnalyzer.js';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {ServerParams} from '../../data/ServerParams.js';
import {getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {convert} from '../../visualize/VisUtil.js';
import {primePlot, getActivePlotView} from '../../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {renderPolygonDataArea,  calcCornerString} from '../CatalogSearchMethodType.jsx';
import {clone} from '../../util/WebUtil.js';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {validateMJD, validateDateTime, convertISOToMJD, convertMJDToISO, DateTimePickerField, fMoment,
        tryConvertToMoment} from '../DateTimePickerField.jsx';
import {TimePanel, isShowHelp, formFeedback} from '../TimePanel.jsx';
import {showInfoPopup, showOptionsPopup, POPUP_DIALOG_ID} from '../PopupUtil.jsx';
import {isDialogVisible} from '../../core/ComponentCntlr.js';
import {getColumnAttribute, tapHelpId, HeaderFont, MJD, ISO} from './TapUtil.js';
import {HelpIcon} from '../HelpIcon.jsx';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression';
import {CheckboxGroupInputField} from '../CheckboxGroupInputField.jsx';

export const skey = 'TABLE_SEARCH_METHODS';
const Spatial = 'Spatial';
const SpatialPanel = 'spatialSearchPanel';
const SpatialCheck = 'spatialCheck';
const CenterColumns = 'centerColumns';
const CenterLonColumns = 'centerLonColumns';
const CenterLatColumns = 'centerLatColumns';
const SpatialMethod = 'spatialMethod';
const RadiusSize = 'coneSize';
const PolygonCorners = 'polygoncoords';
const ImageCornerCalc = 'imageCornerCalc';
const Temporal = 'Temporal';
const TemporalPanel = 'temporalSearchPanel';
const TemporalCheck = 'temporalCheck';
const TemporalColumns = 'temporalColumns';
const DatePickerOpenStatus = 'datePickerOpenStatus';
const TimePickerFrom = 'timePickerFrom';
const TimePickerTo = 'timePickerTo';
const MJDFrom = 'mjdfrom';
const MJDTo = 'mjdto';
const TimeFrom = 'timeFrom';
const TimeTo = 'timeTo';
const TimeOptions = 'timeOptions';
const Wavelength = 'Wavelength';
const WavelengthPanel = 'wavelengthSearchPanel';
const WavelengthCheck = 'wavelengthCheck';
const WavelengthColumns = 'wavelengthColumns';

const CrtColumnsModel = 'crtColumnsModel';

const SomeFieldInvalid = 'invalid input';
const PanelValid = 'panelValid';
const PanelMessage = 'panelMessage';

const LeftInSearch = 8;
const LabelWidth = 106;
const LableSaptail = 100;
const SpatialWidth = 440;

const fieldsMap = {[Spatial]: {
                        [SpatialPanel]: {label: Spatial},
                        [CenterColumns]: {label: 'Position Columns'},
                        [CenterLonColumns]: {label: 'Longitude Column'},
                        [CenterLatColumns]: {label: 'Latitude Column'},
                        [SpatialMethod]: {label: 'Search Method'},
                        [ServerParams.USER_TARGET_WORLD_PT]: {label: 'Coordinates or Object Name'},
                        [RadiusSize]: {label: 'Radius'},
                        [PolygonCorners]:  {label: 'Coordinates'}},
                   [Temporal]: {
                        [TemporalPanel]: {label:Temporal},
                        [TemporalColumns]: {label: 'Temporal Column'},
                        [TimeFrom]: {label: 'From'},
                        [TimeTo]: {label: 'To'},
                        [MJDFrom]: {label: 'MJD (From)'},
                        [MJDTo]: {label: 'MJD (To)'},
                        [TimePickerFrom]: {label: 'ISO (From)'},
                        [TimePickerTo]: {label: 'ISO (To)'}},
                   [Wavelength]: {
                        [WavelengthPanel]: {label: Wavelength},
                        [WavelengthColumns]: {label: 'Wavelength Column'}}};


const TapSpatialSearchMethod = new Enum({
    'Cone': 'Cone',
    'Polygon': 'Polygon'
});

import '../CatalogSearchMethodType.css';
import './react-datetime.css';

const  FROM = 0;
const  TO  = 1;
const  timeKeyMap = {
                     [TimeFrom]: {
                         [MJD]: MJDFrom,
                         [ISO]: TimePickerFrom
                     },
                     [TimeTo]: {
                         [MJD]: MJDTo,
                         [ISO]: TimePickerTo
                     }
                };

// size used
const Width_Column = 175;
const Width_Time_Wrapper = Width_Column + 30;
export const SpattialPanelWidth = Math.max(Width_Time_Wrapper*2, SpatialWidth) + LabelWidth + 10;


function Header({title, helpID='', checkID, message, enabled=false}) {
    const tooltip = title + ' search is included in the query if checked';
    return (
        <div style={{display: 'inline-flex', alignItems: 'center'}} title={title + ' search'}>
            <div onClick={(e) => e.stopPropagation()} title={tooltip}>
                <CheckboxGroupInputField
                    key={checkID}
                    fieldKey={checkID}
                    initialState={{
                        value: enabled ?title:'',
                        label: ''
                        }}
                    options={[{label:'', value: title}]}
                    alignment='horizontal'
                    wrapperStyle={{whiteSpace: 'norma'}}
                 />
            </div>
            <div style={{...HeaderFont, marginRight: 5}}>{title}</div>
            <HelpIcon helpId={helpID}/>
            <div style={{marginLeft: 10, color: 'saddlebrown', fontStyle: 'italic', fontWeight: 'normal'}}>{message}</div>
        </div>
    );
}
Header.propTypes = {
    title: PropTypes.string,
    helpID: PropTypes.string,
    checkID: PropTypes.string,
    message: PropTypes.string
};

function isFieldInPanel(fieldKey) {
    return Object.keys(fieldsMap).find((panel) => {
               return Object.keys(fieldsMap[panel]).includes(fieldKey);
           });
}

function isPanelChecked(searchItem, fields) {
    const checkKey = searchItem.toLowerCase() + 'Check';
    return get(fields, [ checkKey, 'value' ]) === searchItem;
}

function getLabel(key, trailing='') {
    const panel = isFieldInPanel(key);
    const l = panel ? get(fieldsMap, [panel, key, 'label'], '') : '';
    return l ? l+trailing  : l;
}

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

        const cols = getAvailableColumns(columnsModel);
        return (
            <FieldGroup style={{height: '100%', overflow: 'auto'}}
                        groupKey={skey} keepState={true} reducerFunc={tapSearchMethodReducer(columnsModel)}>
                <SpatialSearch {...{cols, groupKey, fields, initArgs:this.props.initArgs}} />
                <TemporalSearch {...{cols, groupKey, fields}} />
                {false && <WavelengthSearch {...{cols, groupKey, fields, initArgs:this.props.initArgs}} />}
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

function SpatialSearch({cols, groupKey, fields, initArgs={}}) {
    const {POSITION:worldPt, radiusInArcSec}= initArgs;
    const showCenterColumns = () => {
        return (
            <div style={{marginLeft: LeftInSearch}}>
                <ColumnFld fieldKey={CenterLonColumns}
                           groupKey={groupKey}
                           cols={cols}
                           name={getLabel(CenterLonColumns).toLowerCase()} // label that appears in column chooser
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                />
                <div style={{marginTop: 5}}>
                    <ColumnFld fieldKey={CenterLatColumns}
                               groupKey={groupKey}
                               cols={cols}
                               name={getLabel(CenterLatColumns).toLowerCase()} // label that appears in column chooser
                               inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                    />
                </div>
            </div>
        );
    };

    const doSpatialSearch = () => {
        return (
            <div style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap',
                         width: SpatialWidth, marginLeft: `${LabelWidth + LeftInSearch}px`, marginTop: 5}}>
                {selectSpatialSearchMethod(groupKey, fields)}
                {setSpatialSearchSize(fields, radiusInArcSec)}
            </div>
        );
    };

    const message = get(fields, [SpatialCheck, 'value']) === Spatial ? get(fields, [SpatialPanel, PanelMessage], '') : '';

    return (
        <FieldGroupCollapsible header={<Header title={Spatial}  helpID={tapHelpId('spatial')}
                                       checkID={SpatialCheck}   message={message} enabled={Boolean(worldPt)}/>}
                               initialState={{ value: 'open' }}
                               fieldKey={SpatialPanel}
                               wrapperStyle={{marginBottom: 15}}
                               headerStyle={HeaderFont}>
            <div style={{marginTop: 5}}>
                {showCenterColumns()}
                {doSpatialSearch()}
            </div>
        </FieldGroupCollapsible>
    );
}

SpatialSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object,
    initArgs: PropTypes.object
};


function getTimeValueInfo(timeMode, groupKey, timeFieldKey) {
    const timeKey = get(timeKeyMap, [timeFieldKey, timeMode]);
    const fields = FieldGroupUtils.getGroupFields(groupKey);
    const {value='', valid=true, message=''} = get(fields, timeKey);

    return {value, valid, message};
}

function TemporalSearch({cols, groupKey, fields}) {
    const showTemporalColumns = () => {
        return (
            <div style={{marginLeft: LeftInSearch}}>
                <ColumnFld fieldKey={TemporalColumns}
                           groupKey={groupKey}
                           cols={cols}
                           name={getLabel(TemporalColumns).toLowerCase()} // label that appears in column choosery
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
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
            <div style={{display: 'flex', marginLeft: LeftInSearch, marginTop: 5, width: SpattialPanelWidth}}>
                <RadioGroupInputField fieldKey={TimeOptions}
                                      options={timeOptions}
                                      alignment={'horizontal'}
                                      wrapperStyle={{width: LabelWidth, paddingRight:'4px'}}
                />
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeFrom}
                                 groupKey={skey}
                                 timeMode={crtTimeMode}
                                 icon={icon}
                                 onClickIcon={changeDatePickerOpenStatus(FROM)}
                                 feedbackStyle={{height: 100}}
                                 inputWidth={Width_Column}
                                 inputStyle={{overflow:'auto', height:16}}
                    />
                </div>
                <div style={{width: Width_Time_Wrapper}}>
                    <TimePanel fieldKey={TimeTo}
                               groupKey={skey}
                               timeMode={crtTimeMode}
                               icon={icon}
                               onClickIcon={changeDatePickerOpenStatus(TO)}
                               feedbackStyle={{height: 100}}
                               inputWidth={Width_Column}
                               inputStyle={{overflow:'auto', height:16}}
                    />
               </div>
            </div>
        );
    };

    const message = get(fields, [TemporalCheck, 'value']) === Temporal ?get(fields, [TemporalPanel, PanelMessage], '') :'';
    return (
        <FieldGroupCollapsible header={<Header title={Temporal} helpID={tapHelpId('temporal')}
                                        checkID={TemporalCheck} message={message}/>}
                               initialState={{ value: 'closed' }}
                               fieldKey={TemporalPanel}
                               wrapperStyle={{marginBottom: 15}}
                               headerStyle={HeaderFont}>
                <div style={{marginTop: 5, height: 100}}>
                    {showTemporalColumns()}
                    {showTimeRange()}
                </div>
        </FieldGroupCollapsible>
    );
}

TemporalSearch.propTypes = {
    cols: ColsShape,
    groupKey: PropTypes.string,
    fields: PropTypes.object
};



function WavelengthSearch({cols, groupKey, fields, initArgs={}}) {
    const showWavelengthColumns = () => {
        return (
            <div style={{marginLeft: LeftInSearch}}>
                <ColumnFld fieldKey={WavelengthColumns}
                           groupKey={groupKey}
                           cols={cols}
                           name={getLabel(WavelengthColumns).toLowerCase()} // label that appears in column chooser
                           label={getLabel(WavelengthColumns, ':')}
                           labelWidth={LabelWidth}
                           tooltip={'Column for wavelength search'}
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                />
            </div>
        );
    };
    return (
        <FieldGroupCollapsible header={<Header title={Wavelength} helpID={tapHelpId('wavelength')}
                                               checkID={WavelengthCheck}/>}
                               initialState={{ value: 'closed' }}
                               fieldKey={WavelengthPanel}
                               headerStyle={HeaderFont}>
            <div style={{width: 100, height: 50}}>
                {showWavelengthColumns()}
            </div>
        </FieldGroupCollapsible>
    );
}

WavelengthSearch.propTypes = {
    cols: ColsShape,
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
        <div style={{display:'flex', flexDirection:'column'}}>
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
 * @returns {*}
 */
function setSpatialSearchSize(fields, radiusInArcSec) {
    const border = '1px solid #a3aeb9';
    const searchType = get(fields, [SpatialMethod, 'value'], TapSpatialSearchMethod.Cone.value);

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
                {renderPolygonDataArea(imageCornerCalc)}
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
    return (
        <SizeInputFields fieldKey={RadiusSize} showFeedback={true}
                         wrapperStyle={{padding:5, margin: '5px 0px 5px 0px'}}
                         initialState={{
                                               unit: 'arcsec',
                                               labelWidth : 100,
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


const defaultSpatialConstraints = {valid: true, where: '', title: 'spatial search error'};
const defaultTemporalConstraints =  {valid: true, where: '', title: 'temporal search error'};

function makeSpatialConstraints(fields, columnsModel) {
    const NOConstraint = '';
    const retval = clone(defaultSpatialConstraints);
    const {centerLonColumns, centerLatColumns, spatialMethod} = fields;

    // find ucd coordinate in type of UCDCoord
    const getUCDCoord = (colName) => {
        const ucdVal = getColumnAttribute(columnsModel, colName, 'ucd');

        if (!ucdVal) {
            return UCDCoord.eq;
        } else {
            return UCDCoord.enums.find((enumItem) => (ucdVal.includes(enumItem.key))) || UCDCoord.eq;
        }
    };

    const ucdCoord = getUCDCoord(centerLonColumns.value);
    const worldSys = posCol[ucdCoord.key].coord;
    const adqlCoordSys = posCol[ucdCoord.key].adqlCoord;
    const point = `POINT('${adqlCoordSys}', ${centerLonColumns.value}, ${centerLatColumns.value})`;
    let   userArea;
    let   newWpt;

    if (spatialMethod.value === TapSpatialSearchMethod.Cone.value) {
        const {coneSize} = fields;
        const worldPt = parseWorldPt(get(fields, [ServerParams.USER_TARGET_WORLD_PT, 'value']));
        const size = coneSize?.value;

        if (!worldPt || !size) {
            return retval;
        }

        newWpt = convert(worldPt, worldSys);
        userArea = `CIRCLE('${adqlCoordSys}', ${newWpt.x}, ${newWpt.y}, ${size})`;

    } else if (spatialMethod.value === TapSpatialSearchMethod.Polygon.value) {
        const {polygoncoords} = fields;

        if (!polygoncoords?.value) {
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
                return retval;   // no content
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

function makeTemporalConstraints(fields, columnsModel) {
    const retval = clone(defaultTemporalConstraints);
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
            where = timeConstraints[FROM] + timeConstraints[TO];
        } else {
            where =`(${timeConstraints[FROM]} AND ${timeConstraints[TO]})`;
        }
    }
    retval.where = where;
    return retval;
}

/**
 * compose constraints for ADQL where clause
 * @param {object} columnsModel
 * @returns {AdqlFragment}
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

    const spatialConstraints = isPanelChecked(Spatial, fields) ? makeSpatialConstraints(fields, columnsModel)
                                                               : clone(defaultSpatialConstraints);
    const timeConstraints = isPanelChecked(Temporal, fields) ? makeTemporalConstraints(fields, columnsModel)
                                                             : clone(defaultTemporalConstraints);

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
                p.where += ' AND (' + oneCondition.where + ')';
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
            const {fieldKey, value, displayValue}= action.payload;
            const rFields = clone(inFields);

            const setSearchPanelChecked = (key) => {
                const panel = [ServerParams.USER_TARGET_WORLD_PT, RadiusSize, PolygonCorners].includes(key) ? Spatial :
                    ([TimeFrom, TimeTo, TimePickerFrom, TimePickerTo].includes(key) ? Temporal : null);

                if (panel && !isPanelChecked(panel, rFields)) {
                    const val = () => {
                        if (displayValue || typeof value !== 'string') {
                            return displayValue;
                        } else {
                            return value;
                        }
                    };

                    if (val()) {
                        const panelChk = panel.toLowerCase()+'Check';
                        set(rFields, [panelChk, 'value'], panel);
                    }
                }
            };

            const onChangePolygonCoordinates = () => {
                rFields.imageCornerCalc = clone(inFields.imageCornerCalc, {value: 'user'});
                setSearchPanelChecked(fieldKey);
                validateSpatialConstraints(inFields, rFields);
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
                validateSpatialConstraints(inFields, rFields);
            };

            const onChangeTimeField = () => {
                // only update picker & mjd when there is no pop-up picker (time input -> picker or mjd)
                if (!isDialogVisible(POPUP_DIALOG_ID)) {
                    const {valid, message} = get(inFields, fieldKey, {});
                    const timeMode = get(inFields, [TimeOptions, 'value']);
                    const mjdKey = get(timeKeyMap, [fieldKey, MJD]);
                    const isoKey = get(timeKeyMap, [fieldKey, ISO]);
                    const updateValue = timeMode === MJD ? value : (valid ? fMoment(tryConvertToMoment(value, true)): value);

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
                    setSearchPanelChecked(fieldKey);
                    validateTemporalConstraints(inFields, rFields);
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
                    let tooltip
                    // const tooltip = `'${idx === FROM ? 'from' : 'to'}' time in ${timeMode} mode`;

                    if (timeMode==='iso') {
                        tooltip= `${idx===FROM?'From:':'To:'} select ISO 8601 time format (e.g., 2021-03-20)`;
                    }
                    else {
                        tooltip= `${idx===FROM?'From:':'To:'} Select Modified Julian Date time format (e.g., 59293.1)`;
                    }

                    rFields[timeKey] = clone(inFields[timeKey], { value: timeInfos[idx][timeMode].value,
                        valid: timeInfos[idx][timeMode].valid,
                        message: timeInfos[idx][timeMode].message,
                        tooltip, showHelp, feedback, timeMode});
                });
                validateTemporalConstraints(inFields, rFields);
            };

            const onChangeDateTimePicker = () => {
                // update MJD & TimeFrom (TimeTo) when there is pop-up picker (picker -> time field & mjd)
                if (isDialogVisible(POPUP_DIALOG_ID)) {
                    const {valid, message} = get(inFields, fieldKey) || {};
                    const timeKey = (fieldKey === TimePickerFrom) ? TimeFrom : TimeTo;
                    const mjdKey = get(timeKeyMap, [timeKey, MJD]);

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

                    setSearchPanelChecked(fieldKey);
                    validateTemporalConstraints(inFields, rFields);
                }
            };

            const onChangeSearchCheckBox = (key) => {
                if (key === SpatialCheck) {
                    return validateSpatialConstraints(inFields, rFields, value !== Spatial );
                } else if (key === TemporalCheck) {
                    return validateTemporalConstraints(inFields, rFields, value !== Temporal );
                }
            };

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

                validateSpatialConstraints(inFields, rFields);
                validateTemporalConstraints(inFields, rFields);
            };


            switch (action.type) {
                case FieldGroupCntlr.VALUE_CHANGE:
                    if (fieldKey === PolygonCorners) {
                        onChangePolygonCoordinates(rFields);
                    } else if (fieldKey === SpatialMethod && value === TapSpatialSearchMethod.Polygon.key) {
                        onChangeToPolygonMethod();
                    } else if (fieldKey === ImageCornerCalc) {
                        onChangeToPolygonMethod();
                    } else if (fieldKey === TimeFrom || fieldKey === TimeTo) { // update mjd & picker
                        onChangeTimeField();
                    } else if (fieldKey === TimeOptions) {    // time mode changes => update timefrom and timeto value
                        onChangeTimeMode();
                    } else if (fieldKey === TimePickerFrom || fieldKey === TimePickerTo) { // update mjd & time fields
                        onChangeDateTimePicker();
                    } else if (fieldKey === SpatialCheck || fieldKey === TemporalCheck || fieldKey === WavelengthCheck) {
                        onChangeSearchCheckBox(fieldKey);
                    } else {
                        setSearchPanelChecked(fieldKey);

                        const panel = isFieldInPanel(fieldKey);

                        if (panel) {
                            if (panel === Spatial) {
                                validateSpatialConstraints(inFields, rFields);
                            } else if (panel === Temporal) {
                                validateTemporalConstraints(inFields, rFields);
                            }
                        }
                    }
                    break;
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    if (columnsModel.tbl_id !== get(inFields, [CrtColumnsModel, 'value'])) {
                        onResetColumnsTable();
                    }
                    break;
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
    const centerColObj = (centerCols && centerCols.lonCol && centerCols.latCol) ?
        {lon: centerCols.lonCol.column_name, lat: centerCols.latCol.column_name} : {lon: '', lat: ''};

    return centerColObj;
}

/**
 * temporal column validator
 * @param columnsTable
 * @returns {Function}
 */
/*
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
                retval = {valid: false, message: 'invalid column name'};
            }
        }
        return retval;
    };
}
*/

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

/**
 * validate all fields in spatial search panel
 * if newFields is not defined, return true/false valid depending if any entry with invalid value exists.
 * if newFields is defined, the validity of the relevant field is recalculated and updated depending on if
 * spatial search panel is checked or not
 * @param fields
 * @param newFields
 * @param nullAllowed
 * @returns {{valid: boolean}}
 */
function validateSpatialConstraints(fields, newFields, nullAllowed) {
    let allValid = true;
    const opFields = newFields || fields;

    if (isUndefined(nullAllowed) && newFields) {
        nullAllowed = !isPanelChecked(Spatial, newFields);
    }

    const checkField = (key) => {
        const retval = getFieldValidity(opFields, key, nullAllowed);
        if (newFields) {
            Object.assign(newFields[key], {valid: retval.valid, message: retval.message});

            if (has(newFields[key], 'nullAllowed')) {
                newFields[key].nullAllowed = nullAllowed;
            }

            allValid = allValid&&retval.valid;
        }
        return retval;
    };

    let retval;

    const updateRetMessage = (key) => {
        return updateMessage(retval, Spatial, key);
    };

    retval = checkField(CenterLonColumns);

    if (!newFields && !retval.valid) {
        return updateRetMessage(CenterLonColumns);
    }

    retval = checkField(CenterLatColumns);

    if (!newFields && !retval.valid) {
        return updateRetMessage(CenterLatColumns);
    }

    const searchMethod = get(opFields, [SpatialMethod, 'value']);

    if (searchMethod ===  TapSpatialSearchMethod.Cone.value) {
        retval = checkField(ServerParams.USER_TARGET_WORLD_PT);

        if (!newFields && !retval.valid) {
            return  updateRetMessage(ServerParams.USER_TARGET_WORLD_PT);
        }

        retval = checkField(RadiusSize);

        if (!newFields && !retval.valid) {
            return updateRetMessage(RadiusSize);
        }
    } else if (searchMethod === TapSpatialSearchMethod.Polygon.value) {
        const {polygoncoords} = opFields;

        // check any polygon pair is valid, skip the pair check in case this field is not added yet.
        if (polygoncoords) {
            const rVal = polygoncoords.value ? polygoncoords.value.trim() : '';
            if (rVal) {
                const pairs = rVal.split(',').filter((onePair) => onePair);

                if (pairs.length < 3) {
                    retval = {valid: false, message: 'not enough pairs'};
                } else {
                    const badCorner = pairs.findIndex((onePoint) => {
                        const corner = onePoint.trim().split(' ');

                        return (corner.length !== 2) || isNaN(Number(corner[0])) || isNaN(Number(corner[1]));
                    });


                    retval = badCorner >= 0 ? {valid: false, message: 'wrong corner pair'} : {valid: true, message: ''};
                }
            } else {
                retval = {valid: nullAllowed, message: nullAllowed ? '' : 'empty entry'};
            }

            if (newFields) {
                Object.assign(newFields.polygoncoords, {valid: retval.valid, message: retval.message});
                allValid = allValid&&retval.valid;
            }

            if (!newFields && !retval.valid) {
                return updateRetMessage(PolygonCorners);
            }
        }
    }
    if (newFields) {
        retval = {valid: allValid, message: allValid ? '' : SomeFieldInvalid};
        Object.assign(newFields[SpatialPanel], {[PanelValid]: retval.valid, [PanelMessage]: retval.message} );

        return retval;
    } else {
        return {valid: true};
    }
}

/**
 * validate all fields in temporal search panel
 * if newFields is not defined, return true/false valid depending if any entry with invalid value exists.
 * if newFields is defined, the validity of the relevant field is recalculated and updated depending on if
 * temporal search panel is checked or not
 * @param fields
 * @param newFields
 * @param nullAllowed
 * @returns {{valid: boolean}}
 */
function validateTemporalConstraints(fields, newFields, nullAllowed) {
    const allowEmptyFields = [TimeFrom, TimeTo, MJDFrom, MJDTo, TimePickerFrom, TimePickerTo];

    let allValid = true;
    if (isUndefined(nullAllowed) && newFields) {
        nullAllowed = !isPanelChecked(Temporal, newFields);
    }

    const checkField = (key) => {
        let retval;

        if (isUndefined(nullAllowed)) {
            retval = getFieldValidity(newFields || fields, key);
        } else {
            retval = getFieldValidity(newFields || fields, key,
                                      allowEmptyFields.includes(key) ? true : nullAllowed);
        }
        if (newFields) {
            Object.assign(newFields[key], {valid: retval.valid, message: retval.message});

            if (has(newFields[key], 'nullAllowed')) {
                newFields[key].nullAllowed = nullAllowed;
            }

            allValid = allValid&&retval.valid;
        }
        return retval;
    };


    let retval;
    const updateRetMessage = (key) => {
        return updateMessage(retval, Temporal, key);
    };

    retval = checkField(TemporalColumns);
    if (!newFields && !retval.valid) {
        return updateRetMessage(TemporalColumns);
    }

    retval = checkField(TimeFrom);
    if (!newFields && !retval.valid) {
        return updateRetMessage(TimeFrom);
    }

    retval = checkField(TimeTo);
    if (!newFields && !retval.valid) {
        return updateRetMessage(TimeTo);
    }

    // mark field invalid if either ISO or MJD is invalid
    // let mjdRet = checkField(MJDFrom);
    // let isoRet = checkField(TimePickerFrom);
    //
    // if (!newFields && (!mjdRet.valid || !isoRet.valid)) {
    //     retval.valid = false;
    //     retval.message = !isoRet.valid ? isoRet.message : mjdRet.message;
    //     return updateRetMessage(!isoRet.valid ? TimePickerFrom : MJDFrom);
    // }
    //
    // mjdRet = checkField(MJDTo);
    // isoRet = checkField(TimePickerTo);
    //
    // if (!newFields && (!mjdRet.valid || !isoRet.valid)) {
    //     retval.valid = false;
    //     retval.message = !isoRet.valid ? isoRet.message : mjdRet.message;
    //     return updateRetMessage(!!isoRet.valid  ? TimePickerTo : MJDTo);
    // }

    if (newFields) {
        retval = {valid: allValid, message: allValid ? '' : SomeFieldInvalid};
        Object.assign(newFields[TemporalPanel], {[PanelValid]: retval.valid, [PanelMessage]: retval.message} );

        return retval;
    } else {
        return {valid: true};
    }
}

/**
 * pass the validity of the constraint entries on the checked search panel before forming the ADQL query
 * @param fields
 * @param columnsModel - if passed the do even more validation
 * @returns {*}
 */
export function validateConstraints(fields, columnsModel=undefined) {
    // spatial constraints
    let retval = {valid: true};

    if (isPanelChecked(Spatial, fields)) {
        retval = validateSpatialConstraints(fields);

        if (columnsModel && retval.valid) {
            const spacRet= makeSpatialConstraints(fields, columnsModel);
            if (retval.valid) retval.valid= Boolean(spacRet.valid && spacRet.where);
        }
        if (!retval.valid) return retval;
    }

    if (isPanelChecked(Temporal, fields)) {
        retval = validateTemporalConstraints(fields);
        if (columnsModel && retval.valid) {
            const temporalRet= makeTemporalConstraints(fields, columnsModel);
            if (retval.valid) retval.valid= Boolean(temporalRet.valid && temporalRet.where);
        }
    }
    return retval;
}

function fieldInit(columnsTable) {
    const centerColObj = formCenterColumns(columnsTable);
    const cols = getAvailableColumns(columnsTable);

    return (
        {
            [CenterLonColumns]: {
                fieldKey: CenterLonColumns,
                value: centerColObj.lon,
                tooltip: 'Table column that specifies longitude position',
                label: getLabel(CenterLonColumns, ':'),
                labelWidth: LabelWidth,
                validator: getColValidator(cols, false, false)
            },
            [CenterLatColumns]: {
                fieldKey: CenterLatColumns,
                value: centerColObj.lat,
                tooltip: 'Table column that specifies latitude position',
                label: getLabel(CenterLatColumns, ':'),
                labelWidth: LabelWidth,
                validator: getColValidator(cols, false, false)
            },
            [TemporalColumns]: {
                fieldKey: TemporalColumns,
                value: '',
                tooltip: 'Column for temporal search',
                label: getLabel(TemporalColumns, ':'),
                labelWidth: LabelWidth,
                validator: getColValidator(cols, false, false)
            },
            [SpatialMethod]: {
                fieldKey: SpatialMethod,
                tooltip: 'Select spatial search method',
                label: getLabel(SpatialMethod, ':'),
                labelWidth: LableSaptail,
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
            [PolygonCorners]: {
                fieldKey: PolygonCorners,
                value: ''
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
                value: ISO,
                tooltip:  'Choose between:\nISO 8601 time format (e.g., 2021-03-20)\nor\nModified Julian Date time format (e.g., 59293.1)'
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
                validator: timeValidator,
                tooltip:  'From: Select ISO 8601 time format (e.g., 2021-03-20)',
            },
            [TimeTo]:{
                fieldKey: TimeTo,
                value: '',
                validator: timeValidator,
                tooltip: 'To: Select ISO 8601 time format (e.g., 2021-03-20)'
            }
        }
    );
}
