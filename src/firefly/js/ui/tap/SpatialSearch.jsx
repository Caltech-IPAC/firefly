import PropTypes from 'prop-types';
import React, {useContext, useEffect, useState} from 'react';
import {ColsShape, ColumnFld, getColValidator} from '../../charts/ui/ColumnOrExpression.jsx';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {ServerParams} from '../../data/ServerParams.js';
import {findCenterColumnsByColumnsModel, findTableCenterColumns, posCol, UCDCoord} from '../../util/VOAnalyzer.js';
import {getSizeAsString} from '../../util/WebUtil.js';
import CoordinateSys from '../../visualize/CoordSys.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {getActivePlotView, primePlot} from '../../visualize/PlotViewUtil.js';
import {makeWorldPt, parseWorldPt} from '../../visualize/Point.js';
import {VisualTargetPanel} from '../../visualize/ui/TargetHiPSPanel.jsx';
import {convert} from '../../visualize/VisUtil.js';
import {calcCornerString, renderPolygonDataArea} from '../CatalogSearchMethodType.jsx';
import {FieldGroupCtx, ForceFieldGroupValid} from '../FieldGroup.jsx';
import {ExtraButton} from '../FormPanel.jsx';
import {ListBoxInputField} from '../ListBoxInputField.jsx';
import {FieldGroupCollapsible} from '../panel/CollapsiblePanel.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField.jsx';
import {useFieldGroupRerender, useFieldGroupValue, useFieldGroupWatch} from '../SimpleComponent.jsx';
import {SizeInputFields} from '../SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY} from '../TargetPanel.jsx';
import {ConstraintContext} from './Constraints.js';
import {
    DebugObsCore, getPanelPrefix, LableSaptail, makeCollapsibleCheckHeader, makeFieldErrorList, makePanelStatusUpdater,
    SpatialWidth} from './TableSearchHelpers.jsx';
import {showUploadTableChooser} from '../UploadTableChooser.js';
import {
    getAsEntryForTableName, getColumnAttribute, getTapServices, makeUploadSchema, maybeQuote, tapHelpId
} from './TapUtil.js';

const CenterLonColumns = 'centerLonColumns';
const CenterLatColumns = 'centerLatColumns';
const UploadCenterLonColumns = 'uploadCenterLonColumns';
const UploadCenterLatColumns = 'uploadCenterLatColumns';
const Spatial = 'Spatial';
const SPATIAL_TYPE= 'SPATIAL_TYPE';
const RadiusSize = 'coneSize';
const SpatialMethod = 'spatialMethod';
const PolygonCorners = 'polygoncoords';
const cornerCalcType= 'imageCornerCalc';
const SpatialRegOp= 'spatialRegionOperation';
const SINGLE= 'single';
const MULTI= 'multi';

const SpatialLabelSpatial = LableSaptail + 45 /* padding of target */ - 4 /* padding of label */;
const ICRS = 'ICRS';

const TAB_COLUMNS_MSG='These are the recommended columns to use for a spatial search on this table; changing them could cause the query to fail';

const spacialTypeOps = [{label: 'Single Shape', value: SINGLE}, {label: 'Multi-object', value: MULTI}];


function formCenterColumns(columnsTable) {
    const centerCols = findCenterColumnsByColumnsModel(columnsTable);
    return (centerCols && centerCols.lonCol && centerCols.latCol) ?
        {lon: centerCols.lonCol.column_name, lat: centerCols.latCol.column_name} : {lon: '', lat: ''};
}



const checkHeaderCtl= makeCollapsibleCheckHeader(getPanelPrefix(Spatial));
const {CollapsibleCheckHeader, collapsibleCheckHeaderKeys}= checkHeaderCtl;

const fldListAry= [ServerParams.USER_TARGET_WORLD_PT,SpatialRegOp,SPATIAL_TYPE,
            SpatialMethod,RadiusSize, PolygonCorners,CenterLonColumns,CenterLatColumns,
    UploadCenterLonColumns, UploadCenterLatColumns, cornerCalcType];

export function SpatialSearch({cols, serviceUrl, columnsModel, tableName, initArgs={}, obsCoreEnabled:requestObsCore, capabilities}) {
    const {searchParams={}}= initArgs ?? {};
    const obsCoreEnabled= requestObsCore && canSupportAtLeastOneObsCoreOption(capabilities);
    const panelTitle = !obsCoreEnabled ? Spatial : 'Location';
    const panelPrefix = getPanelPrefix(panelTitle);
    const posOpenKey= 'pos-columns';
    const {hipsUrl,centerWP,fovDeg}= getTapServices().find( ({value}) => value===serviceUrl) ?? {};
    const {canUpload=false}= capabilities ?? {};

    const {setConstraintFragment}= useContext(ConstraintContext);
    const {setVal,getVal,makeFldObj,groupKey}= useContext(FieldGroupCtx);
    const [constraintResult, setConstraintResult] = useState({});
    const [getUploadInfo, setUploadInfo]= useFieldGroupValue('uploadInfo');
    const [posOpenMsg, setPosOpenMsg]= useState(TAB_COLUMNS_MSG);

    useFieldGroupRerender([...fldListAry, ...collapsibleCheckHeaderKeys]); // force rerender on any change

    const uploadInfo= getUploadInfo() || undefined;



    const updatePanelStatus= makePanelStatusUpdater(checkHeaderCtl.isPanelActive(), Spatial);

    useEffect(() => {
        if (canUpload) return;
        setUploadInfo(false);
        setVal(SPATIAL_TYPE,SINGLE);
    }, [serviceUrl,canUpload]);

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
        const noDefaults= !lon || !lat;
        setVal(posOpenKey, (noDefaults) ? 'open' : 'closed');
        setPosOpenMsg(noDefaults?'':TAB_COLUMNS_MSG);
    }, [columnsModel, obsCoreEnabled]);

    useFieldGroupWatch([PolygonCorners,DEF_TARGET_PANEL_KEY,RadiusSize,SPATIAL_TYPE],
        ([corners,target,radius],isInit) => {
            if (isInit) return;
            if (corners||target||radius) checkHeaderCtl.setPanelActive(true);
        }
    );

    const onChangeToPolygonMethod = () => {
        const pv = getActivePlotView(visRoot());
        const plot = primePlot(pv);
        if (!plot) return;
        const cornerCalcV = getVal(cornerCalcType);
        if ((!cornerCalcV || cornerCalcV === 'image' || cornerCalcV === 'viewport' || cornerCalcV === 'area-selection')) {
            const sel = plot.attributes[PlotAttribute.SELECTION] ?? plot.attributes[PlotAttribute.POLYGON_ARY];
            if (!sel && cornerCalcV === 'area-selection') setVal(cornerCalcType,'image');
            if (!cornerCalcV) {
                if (sel) setVal(cornerCalcType,'area-selection');
            }
            setTimeout( () => setVal(PolygonCorners,calcCornerString(pv, cornerCalcV)), 5);
        }
    };

    useFieldGroupWatch([SpatialMethod],
        ([spatialMethod]) => spatialMethod==='Polygon' && onChangeToPolygonMethod()
    );

    useFieldGroupWatch([SPATIAL_TYPE],
        ([spacialType]) => (spacialType===MULTI) && !uploadInfo?.fileName && showUploadTableChooser(setUploadInfo),
        [uploadInfo]);

    useFieldGroupWatch([cornerCalcType], () => onChangeToPolygonMethod());

    useEffect(() => {
        const constraints= makeSpatialConstraints(columnsModel, obsCoreEnabled, makeFldObj(fldListAry), uploadInfo, tableName);
        updatePanelStatus(constraints, constraintResult, setConstraintResult);
    });
    
    useEffect(() => {
        setConstraintFragment(panelPrefix, constraintResult);
        return () => setConstraintFragment(panelPrefix, '');
    }, [constraintResult]);


    return (
        <CollapsibleCheckHeader title={panelTitle} helpID={tapHelpId(panelPrefix)}
                                message={constraintResult?.simpleError??''}
                                initialStateOpen={true} initialStateChecked={true}>
            <div style={{marginTop: 5}}>
                <ForceFieldGroupValid forceValid={!checkHeaderCtl.isPanelActive()}>

                    {canUpload &&
                        <RadioGroupInputField
                            fieldKey={SPATIAL_TYPE} options={spacialTypeOps} alignment={'horizontal'}
                            wrapperStyle={{marginTop: 5}}
                            label='Spatial type:'
                            tooltip={'Choose spatial type: either a single area (cone/polygon) or multi-position using an uploaded table'}
                            labelWidth={SpatialLabelSpatial}
                            initialState={{value: SINGLE}}
                        /> }
                    <SpatialSearchLayout {...{obsCoreEnabled, initArgs, uploadInfo, setUploadInfo,
                        hipsUrl, centerWP, fovDeg, capabilities}} />
                    {!obsCoreEnabled &&
                        <CenterColumns {...{lonCol: getVal(CenterLonColumns), latCol: getVal(CenterLatColumns),
                            headerTitle:'Position Columns:', openKey:posOpenKey,
                            headerPostTitle:'(from the selected table on the right)',
                            openPreMessage:posOpenMsg,
                            cols, lonKey:CenterLonColumns, latKey:CenterLatColumns}} />}
                </ForceFieldGroupValid>
            </div>
            <DebugObsCore {...{constraintResult}}/>
        </CollapsibleCheckHeader>
    );
}

SpatialSearch.propTypes = {
    cols: ColsShape,
    initArgs: PropTypes.object,
    capabilities: PropTypes.object,
    obsCoreEnabled: PropTypes.bool,
    serviceUrl: PropTypes.string,
    columnsModel: PropTypes.object,
};

function UploadTableSelector({uploadInfo, setUploadInfo}) {

    const {groupKey,setVal}= useContext(FieldGroupCtx);
    const [getLon,setLon]= useFieldGroupValue(UploadCenterLonColumns);
    const [getLat,setLat]= useFieldGroupValue(UploadCenterLatColumns);

    const {fileName,columns,totalRows,fileSize}= uploadInfo ?? {};
    const columnsUsed= columns?.filter( ({use}) => use)?.length ?? 0;
    const openKey= 'upload-pos-columns';

    useEffect(() => {
        if (!columns) return;
        const centerCols = findTableCenterColumns({tableData:{columns}}) ?? {};
        const {lonCol='', latCol=''}= centerCols;
        if (!getLon()) setLon(lonCol);
        if (!getLat()) setLat(latCol);
        setVal(openKey, (!lonCol || !latCol) ? 'open' : 'closed');
    },[columns]);

    const preSetUploadInfo= (ui) => {
        setLon('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setLat('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setUploadInfo(ui);
    };

    const haveTable= Boolean(fileName && columns);

    return (
        <div style={{margin: '10px 0 0 0'}}>
            <div style={{display:'flex', alignItems:'center'}}>
                <div style={{whiteSpace:'nowrap'}}>Upload Table:</div>
                <div style={{display:'flex', alignItems:'center'}}>
                    <ExtraButton text={fileName ? 'Change...' : 'Add...'}
                                 onClick={() => showUploadTableChooser(preSetUploadInfo)} style={{marginLeft: 42}} />
                    {haveTable &&
                        <div style={{width:200, overflow:'hidden', whiteSpace:'nowrap', fontSize:'larger',
                            textOverflow:'ellipsis', lineHeight:'2em', paddingLeft:15}}>
                            {`${fileName}`}
                        </div>
                    }
                </div>
            </div>
            {haveTable &&
                <div style={{display:'flex', flexDirection:'row', marginLeft: 195, justifyContent:'flex-start'}}>
                    <div style={{whiteSpace:'nowrap'}}>
                        <span>Rows: </span>
                        <span>{totalRows},</span>
                    </div>
                    <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        <span>Columns: </span>
                        <span>{columns.length} (using {columnsUsed}),</span>
                    </div>
                    <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        <span>Size: </span>
                        <span>{getSizeAsString(fileSize)}</span>
                    </div>
                </div>
            }
            {haveTable &&
                <CenterColumns {...{lonCol: getLon(), latCol: getLat(), cols:columns,
                    headerTitle:'Position Columns:', openKey,
                    headerPostTitle:'(from the upload table)',
                    headerStyle:{paddingLeft:1},
                    style:{margin:'0 0 10px 195px'},
                    labelStyle:{paddingRight:10},
                    lonKey:UploadCenterLonColumns, latKey:UploadCenterLatColumns}} />}
        </div>
    );
}

function CenterColumns({lonCol,latCol, style={},cols, lonKey, latKey, openKey, labelStyle,
                           headerTitle, headerPostTitle = '', openPreMessage='', headerStyle}) {


    const posHeader= (
        <div style={{marginLeft:-8}}>
            <span style={{fontWeight:'bold', ...headerStyle}}>
                {(lonCol || latCol) ? `${lonCol || 'unset'}, ${latCol || 'unset'}` : 'unset'}
            </span>
            <span style={{paddingLeft:12, whiteSpace:'nowrap'}}>
                {headerPostTitle}
            </span>
        </div>
    );

    return (
        <div style={{margin: '5px 0 0 0',...style}}>
            <div style={{display:'flex'}}>
                <div style={{width:140, marginTop:10, whiteSpace:'nowrap', ...labelStyle}}>
                    {headerTitle}
                </div>
                <FieldGroupCollapsible header={posHeader} headerStyle={{paddingLeft:0}} contentStyle={{marginLeft:4}}
                                       initialState={{value:'closed'}} fieldKey={openKey}>
                    {openPreMessage && <div style={{padding:'0 0 10px 0'}}>
                        {openPreMessage}
                    </div>}
                    <ColumnFld fieldKey={lonKey} cols={cols}
                               name='longitude column'  // label that appears in column chooser
                               inputStyle={{overflow:'auto', height:12, width: 100}}
                               tooltip={'Center longitude column for spatial search'}
                               label='Lon Column:'
                               labelWidth={62}
                               validator={getColValidator(cols, true, false)} />
                    <div style={{marginTop: 5}}>
                        <ColumnFld fieldKey={latKey} cols={cols}
                                   name='latitude column' // label that appears in column chooser
                                   inputStyle={{overflow:'auto', height:12, width: 100}}
                                   tooltip={'Center latitude column for spatial search'}
                                   label='Lat Column:'
                                   labelWidth={62}
                                   validator={getColValidator(cols, true, false)} />
                    </div>
                </FieldGroupCollapsible>

            </div>
        </div>
    );
}

const OBSCORE_UPLOAD_LAYOUT= 0;
const OBSCORE_SINGLE_LAYOUT= 1;
const NORMAL_UPLOAD_LAYOUT= 2;
const NORMAL_SINGLE_LAYOUT= 3;

function getSpacialLayoutMode(spacialType, obsCoreEnabled) {
    const upload= spacialType===MULTI;
    if (!upload && obsCoreEnabled) return OBSCORE_SINGLE_LAYOUT;
    if (!upload && !obsCoreEnabled) return NORMAL_SINGLE_LAYOUT;
    if (upload && !obsCoreEnabled) return  NORMAL_UPLOAD_LAYOUT;
    if (upload && obsCoreEnabled) return  OBSCORE_UPLOAD_LAYOUT;
}


const SpatialSearchLayout = ({initArgs, obsCoreEnabled, uploadInfo, setUploadInfo,
                                 hipsUrl, centerWP, fovDeg, capabilities}) => {

    const {getVal}= useContext(FieldGroupCtx);

    const spacialType= getVal(SPATIAL_TYPE) ?? SINGLE;
    const spatialMethod= getVal(SpatialMethod)??'Cone';
    const cornerCalcTypeValue= getVal(cornerCalcType)??'image';
    const spatialRegOpValue= getVal(SpatialRegOp) ?? 'contains_point';
    const polygonLabelWidth= 105;
    const layoutMode= getSpacialLayoutMode(spacialType,obsCoreEnabled);
    const isCone= spatialMethod === 'Cone';
    const containsPoint= spatialRegOpValue === 'contains_point';
    const style= {display:'flex', flexDirection:'column', flexWrap:'no-wrap', width: SpatialWidth, marginTop: 5};

    const radiusField= <RadiusField {...{radiusInArcSec:initArgs?.urlApi?.radiusInArcSec}}/>;

    const radiusOrPolygon= isCone ?
        radiusField :
        renderPolygonDataArea({ cornerCalcTypeValue, hipsUrl, centerWP, fovDeg, labelWidth: polygonLabelWidth });


    switch (layoutMode) {
        case OBSCORE_SINGLE_LAYOUT:
            return (
                <div {...{style}}>
                    <RegionOpField {...{initArgs, capabilities}}/>
                    {!containsPoint && <ConeOrAreaField/>}
                    { (isCone || containsPoint) && <TargetPanelForSpacial {...{hipsUrl, centerWP, fovDeg}}/>}
                    {!containsPoint && radiusOrPolygon}
                </div>
            );
        case OBSCORE_UPLOAD_LAYOUT:
            return (
                <div {...{style}}>
                    <RegionOpField {...{initArgs, capabilities}}/>
                    <UploadTableSelector {...{uploadInfo, setUploadInfo}}/>
                    {!containsPoint && radiusOrPolygon}
                </div>
            );
        case NORMAL_SINGLE_LAYOUT:
            return (
                <div {...{style}}>
                    <ConeOrAreaField/>
                    {isCone && <TargetPanelForSpacial {...{hipsUrl, centerWP, fovDeg}}/>}
                    {radiusOrPolygon}
                </div>
            );
        case NORMAL_UPLOAD_LAYOUT:
            return (
                <div {...{style}}>
                    <UploadTableSelector {...{uploadInfo, setUploadInfo}}/>
                    {radiusField}
                </div>
            );
    }
};


const ConeOrAreaField= () => (
    <div style={{display: 'flex', flexDirection: 'column'}}>
        <ListBoxInputField
            fieldKey={SpatialMethod}
            options={[{label:'Cone', value:'Cone'}, {label: 'Polygon', value: 'Polygon'}]}
            wrapperStyle={{marginRight: '15px', padding: '8px 0 5px 0'}} multiple={false}
            tooltip={'Select spatial search method'} label={'Shape Type:'} labelWidth={SpatialLabelSpatial}
            initialState={{ value: 'Cone' }} />
    </div>
);


function buildOptions(capabilities, initArgs) {
    const ops= [];
    let defVal;
    const { canUsePoint, canUseCircle, canUsePolygon, canUseContains, canUseIntersects} = capabilities ?? {};
    const apiDefVal= initArgs?.urlApi?.[SpatialRegOp];
    if (canUseContains && canUsePoint) {
        ops.push({label: 'Observation boundary contains point', value: 'contains_point'});
        defVal= 'contains_point';
    }
    if (canUseContains && (canUseCircle || canUsePolygon)) {
        ops.push({label: 'Observation boundary contains shape', value: 'contains_shape'});
        ops.push({label: 'Observation boundary is contained by shape', value: 'contained_by_shape'});
        if (!defVal) defVal= 'contains_shape';
    }
    if (canUseIntersects && (canUseCircle || canUsePolygon)) {
        ops.push({label: 'Observation boundary intersects shape', value: 'intersects'});
        if (!defVal) defVal= 'intersects';
    }
    if (canUseContains && canUsePoint && (canUseCircle || canUsePolygon)) {
        ops.push({label: 'Central point (s_ra, s_dec) is contained by shape', value: 'center_contained'});
        if (!defVal) defVal= 'center_contained';
    }

    if (apiDefVal && ops.map(({value}) => value).includes(apiDefVal)) {
        defVal= apiDefVal;
    }
    return {ops,defVal};
}

function canSupportAtLeastOneObsCoreOption(capabilities) {
    const {ops}= buildOptions(capabilities);
    return ops.length>0;
}


const RegionOpField= ({initArgs, capabilities}) => {

    const {ops,defVal}= buildOptions(capabilities,initArgs);
    const {setFld}= useContext(FieldGroupCtx);

    useEffect(() => {
        const {ops}= buildOptions(capabilities,initArgs);
        setFld({options:ops});
    }, [capabilities]);

    return (
        <div style={{marginTop: '5px'}}>
            <ListBoxInputField
                fieldKey={SpatialRegOp} multiple={false} label={'Query Type:'} labelWidth={LableSaptail+40}
                options={ops} initialState={{ value: defVal}}
            />
        </div>
    );
};

function TargetPanelForSpacial({hasRadius=true,
                                   hipsUrl= getAppOptions().coverage?.hipsSourceURL  ??  'ivo://CDS/P/2MASS/color',
                                   centerWP, fovDeg=240}) {
    return (
        <div style={{height: 70, display:'flex', justifyContent: 'flex-start', alignItems: 'center', marginTop: '5px'}}>
            <VisualTargetPanel labelWidth={LableSaptail} feedbackStyle={{height: 40, marginLeft:105, width:460}}
                               sizeKey={hasRadius? RadiusSize : undefined}
                               hipsDisplayKey={fovDeg}
                               hipsUrl={hipsUrl} hipsFOVInDeg={fovDeg} centerPt={parseWorldPt(centerWP)} />
        </div>
    );
}

function RadiusField({label = 'Radius:', radiusInArcSec=undefined }) {
    const marginSides = 5;
    return (
        <SizeInputFields fieldKey={RadiusSize} showFeedback={true}
                         style={{margin: `${marginSides}px 0px ${marginSides}px 0px`}}
                         initialState={{
                             unit: 'arcsec',
                             labelWidth : SpatialLabelSpatial - 1 /* box border width */,
                             nullAllowed: true,
                             value: `${(radiusInArcSec||10)/3600}`,
                             min: 1 / 3600,
                             max: 100
                         }}
                         label={label}/>
    );
}

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


function getUploadConeUserArea(tab, upLon, upLat, upColumns, radiusField, adqlCoordSys, errList) {
    
    errList.checkForError(radiusField);
    const size = radiusField.value;
    const valid= upColumns.some( ({name}) => name===upLon) && upColumns.some( ({name}) => name===upLat) ;
    const lonCol= `${tab}.${upLon}`;
    const latCol= `${tab}.${upLat}`;
    if (!lonCol || !latCol || !radiusField.value) return {valid:false, userArea: ''};
    const userArea = valid ? `CIRCLE('${adqlCoordSys}', ${lonCol}, ${latCol}, ${size})` : '';
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


function makeSpatialConstraints(columnsModel, obsCoreEnabled, fldObj, uploadInfo, tableName) {
    const {fileName,serverFile, columns:uploadColumns, totalRows, fileSize}= uploadInfo ?? {};
    const {[CenterLonColumns]:cenLonField, [CenterLatColumns]:cenLatField,
        [ServerParams.USER_TARGET_WORLD_PT]:wpField, [RadiusSize]:radiusSizeField,
        [SPATIAL_TYPE]:spatialTypeField,
        [UploadCenterLonColumns]:uploadCenLonColumns,
        [UploadCenterLatColumns]:uploadCenLatColumns }= fldObj;
    const regionOp= fldObj[SpatialRegOp]?.value ?? 'contains_point';
    const spatialMethod= fldObj[SpatialMethod]?.value;
    const polygonCornersStr= fldObj[PolygonCorners]?.value;
    const upLonCol= uploadCenLonColumns?.value;
    const upLatCol= uploadCenLatColumns?.value;
    const spatialType= spatialTypeField?.value ?? SINGLE;

    let adqlConstraint = '';
    const errList= makeFieldErrorList();
    const tabAs= 'ut';
    const validUpload= Boolean(serverFile && upLonCol && upLatCol);
    const preFix= validUpload ? `${getAsEntryForTableName(tableName)}.` : '';
    if (!validUpload && spatialType===MULTI) {
        if (!serverFile) errList.addError('Upload file is not been specified');
        if (!upLonCol && !upLatCol) errList.addError('Upload columns have not been specified');
        if (!upLonCol) errList.addError('Upload Longitude column have not been specified');
        if (!upLatCol) errList.addError('Upload Latitude column have not been specified');
    }
    
    if (!obsCoreEnabled) {
        const cenLon= cenLonField?.value;
        const cenLat= cenLatField?.value;
        errList.checkForError(cenLonField);
        errList.checkForError(cenLatField);
        const ucdCoord = getUCDCoord(columnsModel, cenLon);
        const worldSys = posCol[ucdCoord.key].coord;
        const adqlCoordSys = posCol[ucdCoord.key].adqlCoord;
        const point = `POINT('${adqlCoordSys}', ${maybeQuote(preFix+cenLon)}, ${maybeQuote(preFix+cenLat)})`;


        if (spatialType===SINGLE) {
            if (!radiusSizeField.value && spatialMethod === 'Cone') errList.addError('Missing radius input');
            const { valid, userArea}=
                checkUserArea(spatialMethod, wpField,radiusSizeField, polygonCornersStr, worldSys, ICRS, errList);
            if (valid)  adqlConstraint = `CONTAINS(${point},${userArea})=1`;
            else errList.addError('Spacial input not complete');
        }
        else if (validUpload) {
            if (!radiusSizeField.value) errList.addError('Missing radius input');
            const {valid, userArea}=
                getUploadConeUserArea(tabAs, upLonCol, upLatCol, uploadColumns,
                    radiusSizeField, adqlCoordSys, errList);
            if (valid) adqlConstraint= `CONTAINS(${point}, ${userArea})=1`;
            else errList.addError('Spacial input not complete');
        }

    } else {
        const worldSys = CoordinateSys.EQ_J2000;

        if (regionOp === 'contains_point') {
            if (spatialType===SINGLE) {
                const {valid, x, y} = checkPoint(worldSys, ICRS, wpField, errList);
                if (valid) adqlConstraint = `CONTAINS(POINT('${ICRS}', ${x}, ${y}), s_region)=1`;
                else errList.addError('Spacial input not complete');
            }
            else if (validUpload)  {
                adqlConstraint = `CONTAINS(POINT('${ICRS}', ${tabAs}.${upLonCol}, ${tabAs}.${upLatCol}), s_region)=1`;
            }

        } else {
            if (spatialType===SINGLE) {
                const {valid, userArea} =
                    checkUserArea(spatialMethod, wpField,radiusSizeField, polygonCornersStr, worldSys, ICRS, errList);
                if (valid) adqlConstraint= makeUserAreaConstraint(regionOp,userArea, ICRS );
                else errList.addError('Spacial input not complete');
            }
            else if (validUpload)  {
                const {valid, userArea}=
                    getUploadConeUserArea(tabAs, upLonCol, upLatCol, uploadColumns, radiusSizeField, ICRS, errList);
                if (valid) adqlConstraint= makeUserAreaConstraint(regionOp,userArea, ICRS );
                else errList.addError('Spacial input not complete');
            }
        }
    }

    const errAry= errList.getErrors();
    const retObj= {
        valid:  errAry.length===0, errAry,
        adqlConstraintsAry: adqlConstraint ? [adqlConstraint] : [],
        siaConstraints:[],
        siaConstraintErrors:[],
    };
    if (spatialType===MULTI) {
        retObj.TAP_UPLOAD= makeUploadSchema(fileName,serverFile,uploadColumns, totalRows, fileSize);
        retObj.uploadFile= fileName;
    }
    return retObj;
}
