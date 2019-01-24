import React,  {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../../Firefly.js';
import {get, set} from 'lodash';
import Enum from 'enum';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {getColumnValues} from '../../tables/TableUtil.js';
import {findCenterColumnsByColumnsModel, posCol, UCDCoord} from '../../util/VOAnalyzer.js';
import FieldGroupCntlr from '../../fieldGroup/FieldGroupCntlr.js';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {ServerParams} from '../../data/ServerParams.js';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {convert} from '../../visualize/VisUtil.js';
import {primePlot, getActivePlotView} from '../../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../../visualize/WebPlot.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {renderPolygonDataArea,  calcCornerString, initRadiusArcSec} from '../CatalogSearchMethodType.jsx';
import {clone} from '../../util/WebUtil.js';
import {ValidationField} from '../ValidationField.jsx';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';

const skey = 'TABLE_SEARCH_METHODS';
const ColWithUCD = 'colWithUCD';
// field key under skey
//const ColUCDWithPOS = 'poscolsWithUCD';
const CenterColumns = 'centerColumns';
const SpatialMethod = 'spatialMethod';
const RadiusSize = 'coneSize';
const PolygonCorners = 'polygoncoords';
const ImageCornerCalc = 'imageCornerCalc';
const TemporalColumns = 'temporalColumns';
const WavelengthColumns = 'wavelengthColumns';
const CrtColumnsModel = 'crtColumnsModel';

const TapSpatialSearchMethod = new Enum({
    'Cone': 'Cone',
    'Polygon': 'Polygon',
    'All Sky': 'AllSky'
});


import '../CatalogSearchMethodType.css';

export class TableSearchMethods extends PureComponent {
    constructor(props) {
        super(props);
        this.state = this.nextState(props);
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
            this.setState(this.nextState(this.props));
        }
    }

    nextState(props) {
        const {columnsModel} = props;
        const ucds = getColumnValues(columnsModel, 'ucd');
        const column_names = getColumnValues(columnsModel, 'column_name');
        const options = ucds.reduce((p,c,i) => {
            if (c) { p.push({value: column_names[i], label: `[${column_names[i]}] ${c}`}); }
            return p;
        }, []);
        const posCols = ucds.reduce((p,c,i) => {
            if (c && c.includes('pos.')) { p.push({name: column_names[i], ucd: c}); }
            return p;
        }, []);
        const fields = FieldGroupUtils.getGroupFields(skey);

        return {ucds, column_names, options, posCols,fields};
    }

    render() {
        const groupKey = skey;
        const {fields, options} = this.state;
        const {columnsModel} = this.props;

        return (
            <FieldGroup groupKey={skey} keepState={true} reducerFunc={tapSearchMethodReducer(columnsModel)}>
                <div>
                    <UCDColumnList {...{options}} />
                </div>
                <div style={{height: 250, overflow: 'auto'}}>
                    <div>
                        <SpatialSearch {...{groupKey, fields}} />
                    </div>
                    <div>
                        <TemporalSearch {...{groupKey}} />
                    </div>
                    <div>
                        <WavelengthSearch {...{groupKey}} />
                    </div>
                </div>
            </FieldGroup>
        );
    }
}

function UCDColumnList({options}) {
    return (
            <ListBoxInputField key='ucdList'
                               fieldKey= {ColWithUCD}
                               options={options}
                               multiple={false}
                               label='Columns with UCDs:'
                               tooltip='List of UCDs for the selected table columns'
                               labelWidth={100}
                               wrapperStyle={{paddingBottom: 2}}
            />
    );
}

UCDColumnList.propTypes = {
    options: PropTypes.array
};
/*
// parse the selected position column pairs shown in center columns text area
function getPosColumns(groupKey, fields, posCols)  {
    return () => {
        let cols = FieldGroupUtils.getFldValue(fields, ColUCDWithPOS);
        const colAry = cols && cols.split(',');
        const reOrderPair = (ra_dec_cols, ucd1 = 'ra', ucd2 = 'dec') => {
            if (ra_dec_cols[0].ucd.includes(ucd1) && ra_dec_cols[1].ucd.includes(ucd2) &&
                ra_dec_cols[0].ucd.replace(ucd1, ucd2) === ra_dec_cols[1].ucd) {
                return [ra_dec_cols[0], ra_dec_cols[1]];
            } else if (ra_dec_cols[0].ucd.includes(ucd2) && ra_dec_cols[1].ucd.includes(ucd1) &&
                ra_dec_cols[0].ucd.replace(ucd2, ucd1) === ra_dec_cols[1].ucd) {
                return [ra_dec_cols[1], ra_dec_cols[0]];
            }  else {
                return null;
            }
        };

        if (!cols || !colAry || colAry.length !== 2) return;

        let centerColContent = FieldGroupUtils.getFldValue(fields, CenterColumns, '');
        let ra_dec_cols = posCols.reduce((p, oneCol) => {
            if (colAry.includes(oneCol.name)) {
                p.push(oneCol);
            }
            return p;
        }, []);

        if (ra_dec_cols.length === 2) {
            ra_dec_cols = reOrderPair(ra_dec_cols) || reOrderPair(ra_dec_cols, 'lon', 'lat');

            if (ra_dec_cols.length === 0) return;

            cols = ra_dec_cols[0].name + ',' + ra_dec_cols[1].name;

            if (!centerColContent.includes(cols)) {
                centerColContent += cols + '\n';
                dispatchValueChange({fieldKey: CenterColumns, groupKey, value: centerColContent});
            }
        }
    };
}
*/

function SpatialSearch({groupKey, fields}) {
    const showCenterColumns = () => {
        return (
            <div style={{marginLeft: 8}}>
                <ValidationField fieldKey={CenterColumns}
                                 style={{overflow:'auto', height:12, width: 200}}
                />
            </div>
        );
    };

    const doSpatialSearch = () => {
        return (
            <div style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap', alignItems:'center'}}>
                {selectSpatialSearchMethod(groupKey, fields)}
                {setSpatialSearchSize(fields)}
            </div>
        );
    };


    /* the part is kept as comment for future reference if needed
        // show selected center column pairs
        const areaForCenterColumns = () => {
            return (
                <div style={{paddingLeft: 10,  display: 'flex', alignItems: 'flex-end'}}>
                    <button type='button' className='button std'
                            onClick={getPosColumns(groupKey, fields, posCols)}>Add position columns</button>
                    <InputAreaFieldConnected fieldKey={CenterColumns}
                                             style={{overflow:'auto',height: SpatialUCDH, width: 150}}
                                             initialState={{
                                                        tooltip:'Enter center columns'
                                                  }}
                                             label=''
                    />
                </div>
            );
        };

        // show position column candidates
        const showPosCandidate = () => {
            const options = posCols.reduce((p, pos) => {
                if (!isEmpty(pos)) { p.push({value: pos.name, label: `[${pos.name}] ${pos.ucd}`}); }
                return p;
            }, []);

            return (
                <div style={{display:'flex', alignItems:'flex-end'}}>
                    <ListBoxInputField   fieldKey={ColUCDWithPOS}
                                         options={options}
                                         multiple={true}
                                         label='Position Columns with UCDs'
                                         tooltip='List of UCDs of position columns for the selected table columns'
                                         labelWidth={LableWidth}
                                         selectStyle={{height: SpatialUCDH}}
                    />
                    <div style={{whiteSpace: 'nowrap', display: 'block'}}>
                         {!withPositionCols() ? null : areaForCenterColumns()}
                    </div>
               </div>
            );
        };
     */

    return (
        <FieldGroupCollapsible header='Spatial'
                               initialState={{ value: 'open' }}
                               fieldKey='spatialSearchPanel' >
            <div style={{marginTop: 5}}>
                {showCenterColumns()}
            </div>
            {doSpatialSearch()}
        </FieldGroupCollapsible>
    );
}

SpatialSearch.propTypes = {
    groupKey: PropTypes.string,
    posCols: PropTypes.array,
    fields: PropTypes.object
};

function TemporalSearch({groupKey}) {
    const showTemporalColumns = () => {
        return (
            <div style={{marginLeft: 8}}>
                <ValidationField fieldKey={TemporalColumns}
                                 initialState={{ label: 'Temporal Columns:',
                                                 labelWidth: 100,
                                                 tooltip: 'Columns for temporal search'
                                           }}
                                 style={{overflow:'auto', height:12, width: 200}}
                />
            </div>
        );
    };


    return (
        <FieldGroupCollapsible header='Temporal'
                               initialState={{ value: 'closed' }}
                               fieldKey='temporalSearchPanel' >
            <div style={{width: 100, height: 50}}>
                {showTemporalColumns()}
            </div>
        </FieldGroupCollapsible>
    );
}
TemporalSearch.propTypes = {
   groupKey: PropTypes.string
};

function WavelengthSearch({groupKey}) {
    const showWavelengthColumns = () => {
        return (
            <div style={{marginLeft: 8}}>
                <ValidationField fieldKey={WavelengthColumns}
                                 initialState={{ label: 'Wavelength Columns:',
                                                 labelWidth: 106,
                                                 tooltip: 'Columns for wavelength search'
                                           }}
                                 style={{overflow:'auto', height:12, width: 200}}
                />
            </div>
        );
    };


    return (
        <FieldGroupCollapsible header='Wavelength'
                               initialState={{ value: 'closed' }}
                               fieldKey='wavelengthSearchPanel' >
            <div style={{width: 100, height: 50}}>
                {showWavelengthColumns()}
            </div>
        </FieldGroupCollapsible>
    );
}
WavelengthSearch.propTypes = {
    groupKey: PropTypes.string
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
    const {centerColumns, spatialMethod} = fields;

    // find column pairs [[pair_1_ra, pair_1_dec]...[pair_n_ra, pair_n_dec]]
    const makeColPairs = () => {
        return centerColumns.value.split('\n').reduce((p, onePair) => {
            const oneP = onePair.trim();
            const pair = oneP && oneP.split(',');
            if (pair && pair.length === 2) {
                p.push([pair[0].trim(), pair[1].trim()]);
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
    if (pairs.length !== 1) return NOConstraint;

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

        if (!worldPt || !size) return NOConstraint;

        newWpt = convert(worldPt, worldSys);
        userArea = `CIRCLE('${adqlCoordSys}', ${newWpt.x}, ${newWpt.y}, ${size})`;

    } else if (spatialMethod.value === TapSpatialSearchMethod.Polygon.value) {
        const {polygoncoords} = fields;

        if (!polygoncoords.value) return '';

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

        if (!newCorners || newCorners.length < 3 || newCorners.length > 15) return '';

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

    return userArea ? `CONTAINS(${point},${userArea})=1` : userArea;
}

/**
 * Get constraints as ADQL
 * @param {object} columnsModel
 * @returns {string}
 */
export function tableSearchMethodsConstraints(columnsModel) {
    // construct ADQL string here
    const fields = FieldGroupUtils.getGroupFields(skey);
    const adqlConstraints = makeSpatialConstraints(fields, columnsModel);

    return adqlConstraints;
}

function tapSearchMethodReducer(columnsModel) {
    return (inFields, action) => {

        if (!inFields)  {
             return fieldInit(columnsModel);
        } else {
            const {fieldKey}= action.payload;
            const rFields = clone(inFields);

            switch (action.type) {
                case FieldGroupCntlr.VALUE_CHANGE:
                    if (fieldKey === PolygonCorners) {
                        rFields.imageCornerCalc = clone(inFields.imageCornerCalc, {value: 'user'});
                    } else if (fieldKey === SpatialMethod &&
                        get(inFields, [SpatialMethod, 'value']) === TapSpatialSearchMethod.Polygon.key) {
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
                    }
                    break;
                case FieldGroupCntlr.MOUNT_FIELD_GROUP:
                    if (columnsModel.tbl_id !== get(inFields, [CrtColumnsModel, 'value'])) {
                        const centerColStr = formCenterColumnStr(columnsModel);
                        set(rFields, [CenterColumns, 'validator'], centerColumnValidator(columnsModel));
                        set(rFields, [CenterColumns, 'value'], centerColStr);
                        set(rFields, [CrtColumnsModel, 'value'], columnsModel.tbl_id );
                    }
                    break;
            }

            return rFields;
        }
    };
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
        const names = val.trim().split(',');

        if (names.length !== 2) {
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


function fieldInit(columnsTable) {
    const centerColStr = formCenterColumnStr(columnsTable);

    return (
        {
            [CenterColumns]: {
               fieldKey: CenterColumns,
               value: centerColStr,
               tooltip:"Center columns for spatial search, the format is like '<column name 1>,<column name 2>'",
               label: 'Position Columns:',
               labelWidth: 100,
               validator: centerColumnValidator(columnsTable)
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
            }
        }
    );
}

