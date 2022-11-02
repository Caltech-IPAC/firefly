import Enum from 'enum';
import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression.jsx';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {findCenterColumnsByColumnsModel, posCol, UCDCoord} from '../../util/VOAnalyzer.js';
import CoordinateSys from '../../visualize/CoordSys.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {getActivePlotView, primePlot} from '../../visualize/PlotViewUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {VisualTargetPanel} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {convert} from '../../visualize/VisUtil.js';
import {calcCornerString, renderPolygonDataArea} from '../CatalogSearchMethodType.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {useFieldGroupRerender, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY} from '../TargetPanel.jsx';
import {ConstraintContext} from './Constraints.js';
import {
    DebugObsCore, getPanelPrefix, LableSaptail, LeftInSearch, makeCollapsibleCheckHeader, makeFieldErrorList,
    makePanelStatusUpdater, SpatialWidth, Width_Column
} from './TableSearchHelpers.jsx';
import {getColumnAttribute, getTapServices, maybeQuote, tapHelpId} from './TapUtil.js';

const CenterLonColumns = 'centerLonColumns';
const CenterLatColumns = 'centerLatColumns';
const Spatial = 'Spatial';
const RadiusSize = 'coneSize';
const SpatialMethod = 'spatialMethod';
const PolygonCorners = 'polygoncoords';
const SpatialRegOp= 'spatialRegionOperation';

const SpatialLableSaptail = LableSaptail + 45 /* padding of target */ - 4 /* padding of label */;
const ICRS = 'ICRS';


const TapSpatialSearchMethod = new Enum({
    'Cone': 'Cone',
    'Polygon': 'Polygon'
});

function formCenterColumns(columnsTable) {
    const centerCols = findCenterColumnsByColumnsModel(columnsTable);
    return (centerCols && centerCols.lonCol && centerCols.latCol) ?
        {lon: centerCols.lonCol.column_name, lat: centerCols.latCol.column_name} : {lon: '', lat: ''};
}


/**
 * render the target panel for cone search
 * @param visible
 * @param {boolean} hasRadius
 * @param hipsUrl
 * @param centerWP
 * @param fovDeg
 * @returns {null}
 */
function renderTargetPanel(visible, hasRadius,
                           hipsUrl= getAppOptions().coverage?.hipsSourceURL  ??  'ivo://CDS/P/2MASS/color',
                           centerWP, fovDeg=240) {
    const targetSelect = () => {
        const wp= parseWorldPt(centerWP);
        return (
            <div style={{height: 70, display:'flex', justifyContent: 'flex-start', alignItems: 'center', marginTop: '5px'}}>
                <VisualTargetPanel labelWidth={LableSaptail} feedbackStyle={{height: 40}}
                                   sizeKey={hasRadius? RadiusSize : undefined}
                                   hipsDisplayKey={fovDeg}
                                   hipsUrl={hipsUrl} hipsFOVInDeg={fovDeg} centerPt={wp} />
            </div>
        );
    };
    return (visible) ? targetSelect() : null;
}

const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(Spatial));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= [ServerParams.USER_TARGET_WORLD_PT,SpatialRegOp,
            SpatialMethod,RadiusSize, PolygonCorners,CenterLonColumns,CenterLatColumns];

export function SpatialSearch({cols, serviceUrl, columnsModel, initArgs={}, obsCoreEnabled}) {
    const {searchParams={}}= initArgs ?? {};
    const {radiusInArcSec}= initArgs?.urlApi ?? {};
    const panelTitle = !obsCoreEnabled ? Spatial : 'Location';
    const panelPrefix = getPanelPrefix(panelTitle);
    const {hipsUrl,centerWP,fovDeg}= getTapServices().find( ({value}) => value===serviceUrl) ?? {};

    const {setConstraintFragment}= useContext(ConstraintContext);
    const {setVal,getVal,makeFldObj}= useContext(FieldGroupCtx);
    const [constraintResult, setConstraintResult] = useState({});
    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change



    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), Spatial);


    useEffect(() => {
        searchParams.radiusInArcSec && setVal(RadiusSize,searchParams.radiusInArcSec);
        if (searchParams.wp) {
            setVal(SpatialMethod,'Cone');
            setVal(SpatialRegOp,'contains_point');
            setVal(DEF_TARGET_PANEL_KEY,searchParams.wp);
            checkHeaderCtl.setPanelActive(true);
        }
        if (searchParams.corners) {
            setVal(SpatialMethod,'Polygon');
            setVal(SpatialRegOp,'center_contained');
            setVal(PolygonCorners,searchParams.corners);
            checkHeaderCtl.setPanelActive(true);
        }
    }, [searchParams.radiusInArcSec, searchParams.wp, searchParams.corners]);


    useEffect(() => {
        const {lon,lat} = formCenterColumns(columnsModel);
        setVal(CenterLonColumns, lon, {validator: getColValidator(cols, true, false), valid: true});
        setVal(CenterLatColumns, lat, {validator: getColValidator(cols, true, false), valid: true});
    }, [columnsModel, obsCoreEnabled]);

    useFieldGroupWatch([PolygonCorners,DEF_TARGET_PANEL_KEY,RadiusSize],
        ([corners,target,radius],isInit) => {
            if (isInit) return;
            if (corners||target||radius) checkHeaderCtl.setPanelActive(true);
        }
    );

    const onChangeToPolygonMethod = () => {
        const pv = getActivePlotView(visRoot());
        const plot = primePlot(pv);
        if (!plot) return;
        const cornerCalcV = getVal('imageCornerCalc') || 'image';
        if ((cornerCalcV === 'image' || cornerCalcV === 'viewport' || cornerCalcV === 'area-selection')) {
            const sel = plot.attributes[PlotAttribute.SELECTION];
            if (!sel && cornerCalcV === 'area-selection') setVal('imageCornerCalc','image');
            setTimeout( () => setVal(PolygonCorners,calcCornerString(pv, cornerCalcV)), 5);
        }
    };

    useFieldGroupWatch([SpatialMethod],
        ([spatialMethod]) => spatialMethod==='Polygon' && onChangeToPolygonMethod()
    );

    useFieldGroupWatch(['imageCornerCalc'], () => onChangeToPolygonMethod());

    useEffect(() => {
        const constraints= makeSpatialConstraints(columnsModel, obsCoreEnabled, makeFldObj(fldListAry));
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });
    
    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);

    const doObsCoreSearch = () => {

        return (
            <div style={{marginTop: '5px'}}>
                <ListBoxInputField
                    fieldKey={SpatialRegOp}
                    options={
                        [
                            {label: 'Observation boundary contains point', value: 'contains_point'},
                            {label: 'Observation boundary contains shape', value: 'contains_shape'},
                            {label: 'Observation boundary is contained by shape', value: 'contained_by_shape'},
                            {label: 'Observation boundary intersects shape', value: 'intersects'},
                            {label: 'Central point (s_ra, s_dec) is contained by shape', value: 'center_contained'},
                        ]}
                    initialState={{
                        value: initArgs?.urlApi?.[SpatialRegOp] || 'contains_point'
                    }}
                    multiple={false}
                    label={'Query type:'}
                    labelWidth={LableSaptail}
                />
                <div style={{marginTop: '5px'}}>
                    {getVal(SpatialRegOp) !== 'contains_point' && doSpatialSearch(true)}
                    {getVal(SpatialRegOp) === 'contains_point' &&
                        <div style={{marginLeft: LeftInSearch}}>
                            {renderTargetPanel(true, false,hipsUrl, centerWP, fovDeg)}
                        </div>
                    }
                </div>
            </div>
        );
    };

    const centerColumns= (
            <div style={{marginTop: '5px'}}>
                <ColumnFld fieldKey={CenterLonColumns} cols={cols}
                           name='longitude column'  // label that appears in column chooser
                           inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                           tooltip={'Center longitude column for spatial search'}
                           label='Longitude Column :'
                           labelWidth={SpatialLableSaptail}
                           validator={getColValidator(cols, true, false)} />
                <div style={{marginTop: 5}}>
                    <ColumnFld fieldKey={CenterLatColumns} cols={cols}
                               name='latitude column' // label that appears in column chooser
                               inputStyle={{overflow:'auto', height:12, width: Width_Column}}
                               tooltip={'Center latitude column for spatial search'}
                               label='Latitude Column:'
                               labelWidth={SpatialLableSaptail}
                               validator={getColValidator(cols, true, false)} />
                </div>
            </div>
        );

    const doSpatialSearch = (hasRadius) => {
        return (
            <div style={{display:'flex', flexDirection:'column', flexWrap:'no-wrap',
                width: SpatialWidth, marginLeft: LeftInSearch, marginTop: 5}}>
                {selectSpatialSearchMethod(getVal(SpatialMethod)??'Cone', hasRadius, hipsUrl, centerWP, fovDeg)}
                {setSpatialSearchSize(radiusInArcSec, getVal(SpatialMethod)??'Cone', getVal('imageCornerCalc')??'image',
                    hipsUrl,centerWP,fovDeg)}
            </div>
        );
    };

    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''}
                                initialStateOpen={true} initialStateChecked={true}>
            <div style={{marginTop: '5px'}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>
                    {!obsCoreEnabled && centerColumns}
                    {!obsCoreEnabled && doSpatialSearch(true)}
                    {obsCoreEnabled && doObsCoreSearch()}
                </ForceFieldGroupValid>
            </div>
            <DebugObsCore {...{constraintResult}}/>
        </CollapsibleCheckHeader>
    );
}

SpatialSearch.propTypes = {
    cols: ColsShape,
    initArgs: PropTypes.object,
    obsCoreEnabled: PropTypes.bool,
    serviceUrl: PropTypes.string,
    columnsModel: PropTypes.object,
};

function selectSpatialSearchMethod(spatialMethod, hasRadius, hipsUrl, centerWP, fovDeg) {
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
                initialState={{ value: 'Cone' }}
            />
            {renderTargetPanel(spatialMethod === 'Cone', hasRadius, hipsUrl, centerWP, fovDeg)}
        </div>
    );
}

/**
 * render the size area for each spatial search type
 * @param radiusInArcSec
 * @param spatialMethod
 * @param imageCornerCalc
 * @param hipsUrl
 * @param centerWP
 * @param fovDeg
 * @returns {*}
 */
function setSpatialSearchSize(radiusInArcSec, spatialMethod, imageCornerCalc, hipsUrl,centerWP,fovDeg) {
    const border = '1px solid #a3aeb9';

    if (spatialMethod === 'Cone') {
        return (
            <div style={{border}}>
                {radiusInField({radiusInArcSec})}
            </div>
        );
    } else if (spatialMethod === 'Polygon') {
        const labelWidth= SpatialLableSaptail - 1 /* box border */ - 5 /* box padding */ - 5 /* label padding */;
        return (
            <div style={{marginTop: 5}}>
                {renderPolygonDataArea({ imageCornerCalc, hipsUrl, centerWP, fovDeg, labelWidth })}
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
 * @returns {*}
 */
function radiusInField({label = 'Radius', radiusInArcSec=undefined }) {
    const marginSides = 5;
    return (
        <SizeInputFields fieldKey={RadiusSize} showFeedback={true}
                         style={{padding:5, margin: `${marginSides}px 0px ${marginSides}px 0px`}}
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


// find ucd coordinate in type of UCDCoord
const getUCDCoord = (columnsModel, colName) => {
    const ucdVal = getColumnAttribute(columnsModel, colName, 'ucd');
    return (!ucdVal) ?
        UCDCoord.eq : UCDCoord.enums.find((enumItem) => (ucdVal.includes(enumItem.key))) || UCDCoord.eq;
};

/**
 *
 * @param worldSys
 * @param adqlCoordSys
 * @param wpField
 * @param {FieldErrorList} errList
 * @returns {*}
 */
function checkPoint(worldSys, adqlCoordSys, wpField, errList) {
    errList.checkForError(wpField);
    const worldPt = parseWorldPt(wpField.value);
    let newWpt = {};
    let valid= false;
    if (wpField.valid){
        if (worldPt){
            newWpt = convert(worldPt, worldSys);
            valid= true;
        } else {
            errList.addError('no target found');
        }
    }
    return {...newWpt, valid};
}

/**
 *
 * @param polygonCornersStr
 * @param adqlCoordSys
 * @param worldSys
 * @param {FieldErrorList} errList
 * @returns {Object}
 */
function getPolygonUserArea(polygonCornersStr='', adqlCoordSys, worldSys, errList) {
    const splitPairs = polygonCornersStr ? polygonCornersStr.trim()?.split(',') : [];
    let valid= true;
    const newCorners = splitPairs.reduce((p, onePoint) => {
        const corner = onePoint.trim().split(' ');
        if ((corner.length !== 2) || isNaN(parseFloat(corner[0])) || isNaN(parseFloat(corner[1]))) {
            errList.addError('bad polygon pair');
            valid= false;
            return undefined;
        } else {
            if (!p) return undefined;
            p.push(convert(makeWorldPt(corner[0], corner[1]), worldSys));
        }
        return p;
    }, []);

    if (newCorners?.length < 3 || newCorners?.length > 15) {
        errList.addError('too few or too many corner specified');
        valid= false;
    }

    const cornerStr = newCorners?.reduce((p, oneCorner, idx) => {
        if (!oneCorner) return p;
        p += oneCorner.x + ', ' + oneCorner.y;
        if (idx < (newCorners.length - 1)) {
            p += ', ';
        }
        return p;
    }, '') ?? '';
    return { userArea:  valid ? `POLYGON('${adqlCoordSys}', ${cornerStr})` : '', valid};
}

function getConeUserArea(wpField,radiusField, worldSys, adqlCoordSys, errList) {
    const {valid:ptValid,x,y} = checkPoint(worldSys, adqlCoordSys, wpField, errList);
    errList.checkForError(radiusField);
    const size = radiusField.value;
    const valid= ptValid && size && radiusField.valid;
    const userArea = valid ? `CIRCLE('${adqlCoordSys}', ${x}, ${y}, ${size})` : '';
    return {userArea, valid};
}


/**
 *
 * @param spatialMethod
 * @param wpField
 * @param radiusSizeField
 * @param polygonCornersStr
 * @param worldSys
 * @param adqlCoordSys
 * @param {FieldErrorList} errList
 * @returns {Object}
 */
function checkUserArea(spatialMethod, wpField, radiusSizeField, polygonCornersStr, worldSys, adqlCoordSys, errList) {
    if (spatialMethod === 'Cone') {
        return getConeUserArea(wpField, radiusSizeField, worldSys, adqlCoordSys,errList);

    } else if (spatialMethod === 'Polygon') {
        return getPolygonUserArea(polygonCornersStr, adqlCoordSys, worldSys, errList);
    }
    return {valid:false, userArea: undefined};
}


function makeUserAreaConstraint(regionOp, userArea, adqlCoordSys ) {
    if (regionOp === 'contains_shape' || regionOp === 'contained_by_shape') {
        const contains = regionOp === 'contains_shape';
        const containedBy = contains ? 's_region' : userArea;
        const region = contains ? userArea : 's_region';
        return `CONTAINS(${region}, ${containedBy})=1`;

    } else if (regionOp === 'intersects'){
        return `INTERSECTS(${userArea}, s_region)=1`;

    } else if (regionOp === 'center_contained') { // Same as non-ObsCore, but with fixed s_ra/s_dec columns
        return `CONTAINS(POINT('${adqlCoordSys}', s_ra, s_dec),${userArea})=1`;
    }
}


function makeSpatialConstraints(columnsModel, obsCoreEnabled, fldObj) {
    const {[CenterLonColumns]:cenLonField, [CenterLatColumns]:cenLatField,
        [ServerParams.USER_TARGET_WORLD_PT]:wpField, [RadiusSize]:radiusSizeField}= fldObj;
    const regionOp= fldObj[SpatialRegOp]?.value ?? 'contains_point';
    const spatialMethod= fldObj[SpatialMethod]?.value;
    const polygonCornersStr= fldObj[PolygonCorners]?.value;

    let adqlConstraint = '';
    const errList= makeFieldErrorList();
    
    if (!obsCoreEnabled) {
        const cenLon= cenLonField?.value;
        const cenLat= cenLatField?.value;
        errList.checkForError(cenLonField);
        errList.checkForError(cenLatField);
        const ucdCoord = getUCDCoord(columnsModel, cenLon);
        const worldSys = posCol[ucdCoord.key].coord;
        const adqlCoordSys = posCol[ucdCoord.key].adqlCoord;
        const point = `POINT('${adqlCoordSys}', ${maybeQuote(cenLon)}, ${maybeQuote(cenLat)})`;

        const { valid, userArea}=
            checkUserArea(spatialMethod, wpField,radiusSizeField, polygonCornersStr, worldSys, ICRS, errList);
        if (valid)  adqlConstraint = `CONTAINS(${point},${userArea})=1`;

    } else {
        const worldSys = CoordinateSys.EQ_J2000;

        if (regionOp === 'contains_point') {
            const {valid,x,y} = checkPoint(worldSys, ICRS, wpField, errList);
            if (valid) adqlConstraint = `CONTAINS(POINT('${ICRS}', ${x}, ${y}), s_region)=1`;

        } else {
            const {valid, userArea} =
                checkUserArea(spatialMethod, wpField,radiusSizeField, polygonCornersStr, worldSys, ICRS, errList);
            if (valid) adqlConstraint= makeUserAreaConstraint(regionOp,userArea, ICRS );
        }
    }

    const errAry= errList.getErrors();
    return {
        valid:  errAry.length===0, errAry,
        adqlConstraintsAry: adqlConstraint ? [adqlConstraint] : [],
        siaConstraints:[],
        siaConstraintErrors:[],
    };
}
