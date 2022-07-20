/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNumber} from 'lodash';
import React, {useContext, useEffect, useState} from 'react';
import PropTypes, {arrayOf, shape, oneOf, bool, object, string, number} from 'prop-types';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import {SelectedShape} from '../../drawingLayers/SelectedShape.js';
import {sprintf} from '../../externalSource/sprintf.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';
import {ConnectionCtx} from '../../ui/ConnectionCtx.js';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {parseObsCoreRegion} from '../../util/ObsCoreSRegionParser.js';
import {splitByWhiteSpace} from '../../util/WebUtil.js';
import ShapeDataObj from '../draw/ShapeDataObj.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {
    dispatchAttributeChange, dispatchChangeCenterOfProjection,
    dispatchDeletePlotView, dispatchPlotHiPS, visRoot
} from '../ImagePlotCntlr.js';
import {NewPlotMode} from '../MultiViewCntlr.js';
import {WebPlotRequest} from '../WebPlotRequest.js';
import {PlotAttribute} from '../PlotAttribute.js';
import CsysConverter from '../CsysConverter.js';
import {CoordinateSys} from '../CoordSys.js';
import {onPlotComplete} from '../PlotCompleteMonitor.js';
import {isValidPoint, makeDevicePt, makeImagePt, makeWorldPt, parseWorldPt, pointEquals} from '../Point.js';
import {
    getDrawLayerByType, getDrawLayersByType, getPlotViewById, isDrawLayerAttached, primePlot
} from '../PlotViewUtil.js';
import {
    dispatchAttachLayerToPlot, dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchForceDrawLayerUpdate, dlRoot, getDlAry } from '../DrawLayerCntlr.js';
import {createHiPSMocLayer} from '../task/PlotHipsTask.js';
import {GroupKeyCtx} from '../../ui/FieldGroup.jsx';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {SizeInputFields} from '../../ui/SizeInputField.jsx';
import SearchSelectTool from '../../drawingLayers/SearchSelectTool.js';
import HiPSMOC from '../../drawingLayers/HiPSMOC.js';
import {ToolbarButton} from 'firefly/ui/ToolbarButton.jsx';
import DialogRootContainer from 'firefly/ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import {LayoutType, PopupPanel} from 'firefly/ui/PopupPanel.jsx';
import {
    computeCentralPointAndRadius, computeDistance, getEllipseArcEndPoints, getPointOnEllipse
} from 'firefly/visualize/VisUtil.js';
import {closeToolbarModalLayers} from 'firefly/visualize/ui/VisMiniToolbar.jsx';
import {HelpIcon} from './../../ui/HelpIcon.jsx';
import CLICK from 'html/images/20x20_click.png';
import SELECT_NONE from 'html/images/icons-2014/28x28_Rect_DD.png';


const DEFAULT_TARGET_KEY= 'UserTargetWorldPt';
const DIALOG_ID= 'HiPSPanelPopup';
const DEFAULT_HIPS= 'ivo://CDS/P/DSS2/color';
const DEFAULT_FOV= 340;
export const CONE_CHOICE_KEY= 'CONE';
export const POLY_CHOICE_KEY= 'POLY';

const sharedPropTypes= {
    hipsUrl: string,
    centerPt: object,
    hipsFOVInDeg: number,
    style: object,
    plotId:string,
    cleanup: bool,
    groupKey: string,
    minSize: number,
    maxSize: number,
    whichOverlay: string,
    coordinateSys: oneOf([CoordinateSys.GALACTIC, CoordinateSys.EQ_J2000]),
    mocList: arrayOf( shape({ mocUrl: string, title: string }) ),
};

export function VisualPolygonPanel({label, initValue, tooltip, fieldKey, style,
                                       labelStyle={}, labelWidth= 100, manageHiPS=true, ...restOfProps}) {

    const button= manageHiPS &&
        ( <HiPSPanelPopupButton {...{polygonKey:fieldKey, whichOverlay:POLY_CHOICE_KEY, ...restOfProps}} /> );
    return (
        <InputAreaFieldConnected {...{
            fieldKey, label, labelStyle, button, labelWidth, tooltip,
            wrapperStyle:{display:'flex', alignItems:'center'},
            style:{overflow:'auto', height:55, maxHeight:200, minWidth: 100, width:280, maxWidth:360, ...style},
            initialState:{value:initValue},
        }} />
    );
}

export function VisualTargetPanel({fieldKey, labelWidth= 100, ...restOfProps}) {
    const popupButton= (
        <div style={{paddingRight: 2}}>
            <HiPSPanelPopupButton {...{targetKey:fieldKey, whichOverlay:CONE_CHOICE_KEY, ...restOfProps}} />
        </div>
    );
    return ( <TargetPanel fieldKey={fieldKey} button={popupButton} labelWidth={labelWidth}/> );
}

VisualTargetPanel.propTypes= {
    labelWidth: number,
    ...sharedPropTypes
};



export function convertStrToWpAry(str) {
    const ptStrAry= str?.split(',');
    if (!(ptStrAry?.length>1)) return [];
    const wpAry= ptStrAry
        .map( (s) => splitByWhiteSpace(s))
        .filter( (sAry) => sAry.length===2 && !isNaN(Number(sAry[0])) && !isNaN(Number(sAry[1])))
        .map( (sAry) => makeWorldPt(sAry[0],sAry[1]));
    return wpAry;
}

function convertWpAryToStr(wpAry,plot) {
    const cc= CsysConverter.make(plot);
    return wpAry.reduce( (fullStr, pt, idx) => {
        const wpt= cc.getWorldCoords(pt);
        return wpt ? `${fullStr}${idx>0?', ':''}${sprintf('%.6f',wpt.x)} ${sprintf('%.6f',wpt.y)}` : fullStr;
    },'');
}


function isWpArysEquals(wpAry1, wpAry2) {
    if (wpAry1?.length!==wpAry2.length) return false;
    return wpAry1.filter( (wp,idx) => pointEquals(wp,wpAry2[idx])).length===wpAry1.length;
}


export const HiPSTargetView = ({style, hipsDisplayKey='none',
                                   hipsUrl=DEFAULT_HIPS, hipsFOVInDeg= DEFAULT_FOV, centerPt=makeWorldPt(0,0, CoordinateSys.GALACTIC),
                                   targetKey=DEFAULT_TARGET_KEY, sizeKey='none---Size', polygonKey='non---Polygon',
                                   whichOverlay= CONE_CHOICE_KEY, sRegion,
                                   coordinateSys, mocList, minSize=1/3600, maxSize=100,
                                   plotId='defaultHiPSTargetSearch', cleanup= false, groupKey}) => {

    const viewerId= plotId+'-viewer';

    const pv= useStoreConnector(() => getPlotViewById(visRoot(),plotId));
    const [getTargetWp,setTargetWp]= useFieldGroupValue(targetKey, groupKey);
    const [getHiPSRadius, setHiPSRadius]= useFieldGroupValue(sizeKey, groupKey);
    const [getPolygon, setPolygon]= useFieldGroupValue(polygonKey, groupKey);

    const userEnterWorldPt= () =>  parseWorldPt(getTargetWp());
    const userEnterSearchRadius= () =>  Number(getHiPSRadius());
    const userEnterPolygon= () => convertStrToWpAry(getPolygon());

    useEffect(() => { // show HiPS plot
        if (!pv || hipsUrl!==pv.request.getHipsRootUrl()) {
            initHiPSPlot({plotId,hipsUrl, viewerId,centerPt,hipsFOVInDeg, coordinateSys, mocList,
                userEnterWorldPt:userEnterWorldPt(), userEnterSearchRadius:userEnterSearchRadius(),
                whichOverlay, userEnterPolygon:userEnterPolygon(),
            });
        }
        else {
            updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(), userEnterSearchRadius(),
                userEnterPolygon(), true);
        }
        return () => {
            if (cleanup) dispatchDeletePlotView({plotId});
        };
    },[hipsDisplayKey]);

    useEffect(() => { // if plot view changes then update the target or polygon field
        if (whichOverlay!==CONE_CHOICE_KEY && whichOverlay!==POLY_CHOICE_KEY) return;
        const plot= primePlot(visRoot(),plotId);
        if (!plot) return;
        const isCone= whichOverlay===CONE_CHOICE_KEY;
        const {cenWpt, radius, corners}= plot.attributes[PlotAttribute.SELECTION] ?getDetailsFromSelection(plot) : {};

        if (isCone) {
            if (plot.attributes[PlotAttribute.SELECTION]) {
                if (!cenWpt) return;
                const drawRadius= radius<=maxSize ? (radius >= minSize ? radius : minSize) : maxSize;
                if (pointEquals(userEnterWorldPt(),cenWpt) && drawRadius===userEnterSearchRadius()) return;
                setTargetWp(cenWpt.toString());
                setHiPSRadius(drawRadius+'');
                updatePlotOverlayFromUserInput(plotId, whichOverlay, cenWpt, drawRadius, undefined);
                setTimeout(() => closeToolbarModalLayers(), 10);
            }
            else {
                const wp= plot.attributes[PlotAttribute.USER_SEARCH_WP];
                if (!wp) return;
                const utWPt= userEnterWorldPt();
                if (!utWPt || (isValidPoint(utWPt) && !pointEquals(wp,utWPt ))) {
                    setTargetWp(wp.toString());
                }
            }
        }
        else {
            if (plot.attributes[PlotAttribute.SELECTION]) {
                if (isWpArysEquals(corners,userEnterPolygon())) return;
                setPolygon(convertWpAryToStr(corners,plot));
                setTimeout(() => closeToolbarModalLayers(), 10);
            }
            else {
                const polyWpAry= plot.attributes[PlotAttribute.POLYGON_ARY];
                if (polyWpAry?.length) {
                    if (isWpArysEquals(polyWpAry,userEnterPolygon())) return;
                    setPolygon(convertWpAryToStr(polyWpAry,plot));
                }
            }
        }

    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        updatePlotOverlayFromUserInput(plotId, whichOverlay, userEnterWorldPt(),
            userEnterSearchRadius(), userEnterPolygon());
    }, [getTargetWp, getHiPSRadius, getPolygon, whichOverlay]);

    useEffect(() => {
        attachSRegion(sRegion,plotId);
    }, [sRegion]);

    const helpLines= whichOverlay===CONE_CHOICE_KEY ?
        ( <div>
                <span>Click to choose a search center, or use the Selection Tools (</span>
                <img style={{width:15, height:15, verticalAlign:'text-top'}} src={SELECT_NONE}/>
                <span>) to choose a search center and radius.</span>
            </div> ) :
        ( <div>
                <span>Use the Selection Tools (</span>
                <img style={{width:15, height:15, verticalAlign:'text-top'}} src={SELECT_NONE}/>
                <span>)  to choose a search polygon. Click to change the center. </span>
            </div> );

    return (
        <div style={{height:500, ...style, display:'flex', flexDirection:'column'}}>
            <div style={{padding: '5px 5px 10px 5px',
                alignSelf: 'stretch', marginBottom: 1,
                display: 'flex', flexDirection:'row',
                justifyContent: 'space-between',
                background: 'rgb(200,200,200',
                borderBottom: '1px solid rgba(0,0,0,.07)'}}>
                <div style={{fontSize:'9pt', paddingRight: 10}}>
                    {helpLines}
                </div>
                <HelpIcon style={{alignSelf:'center'}} helpId={'hips.VisualSelection'} />
            </div>
            <MultiImageViewer viewerId= {viewerId} insideFlex={true}
                              canReceiveNewPlots={NewPlotMode.create_replace.key}
                              showWhenExpanded={true}
                              Toolbar={MultiViewStandardToolbar}/>
        </div>
    );
};

HiPSTargetView.propTypes= {
    ...sharedPropTypes,
    sizeKey: string,
    targetKey: string,
};



export const TargetHiPSPanel = ({searchAreaInDeg, style,
                                    minValue:min= 1/3600, maxValue:max= 100, ...restOfProps}) => (
    <div style={{display:'flex', width: 700, height:700, flexDirection:'column', ...style}}>
        <HiPSTargetView {...{...restOfProps, targetKey:DEFAULT_TARGET_KEY, sizeKey:'HiPSPanelRadius',
                             minSize:min, maxSize:max, style:{height:400} }}/>
        <div style={{display:'flex', flexDirection:'column', marginLeft:100}}>
            <TargetPanel style={{paddingTop: 10}} labelWidth={100}/>
            <SizeInputFields fieldKey='HiPSPanelRadius' showFeedback={true} labelWidth= {100}  nullAllowed={false}
                             label='Search Area:'
                             labelStyle={{textAlign:'right', paddingRight:4}}
                             initialState={{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }} />
        </div>
    </div>
);

TargetHiPSPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    ...sharedPropTypes,
};

export const TargetHiPSRadiusPopupPanel = ({searchAreaInDeg, style,
                                               targetKey=DEFAULT_TARGET_KEY, sizeKey= 'HiPSPanelRadius', polygonKey,
                                              minValue:min= 1/3600, maxValue:max= 100,
                                               ...restOfProps}) => {
    const [controlConnected, setControlConnected] = useState(false);
    return (
        <ConnectionCtx.Provider value={{controlConnected, setControlConnected}}>
            <div style={{display:'flex', width: 700, paddingBottom: 20, flexDirection:'column', ...style}}>
                <div style={{display:'flex', flexDirection:'column'}}>
                    <VisualTargetPanel {...{style:{paddingTop: 10}, fieldKey:targetKey, sizeKey, polygonKey, labelWidth:100,
                        minSize:min, maxSize:max, ...restOfProps}} />
                    <SizeInputFields {...{
                        fieldKey:sizeKey, showFeedback:true, labelWidth:100, nullAllowed:false,
                        label:'Search Area:',
                        labelStyle:{textAlign:'right', paddingRight:4},
                        initialState:{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }
                    }} />
                </div>
            </div>
        </ConnectionCtx.Provider>
    );
};

TargetHiPSRadiusPopupPanel.propTypes= {
    searchAreaInDeg: PropTypes.number.isRequired,
    sizeKey: PropTypes.string,
    ...sharedPropTypes,
};

export const hideHiPSPopupPanel= () => dispatchHideDialog(DIALOG_ID);


function HiPSPanelPopupButton({groupKey:gk, polygonKey, whichOverlay=CONE_CHOICE_KEY, targetKey=DEFAULT_TARGET_KEY,
                                  hideHiPSPopupPanelOnDismount= true, centerPt,
                                  tip='Choose search area visually',
                                  hipsFOVInDeg, ...restOfProps}) {

    const context= useContext(GroupKeyCtx);
    const connectContext= useContext(ConnectionCtx);
    const groupKey= gk || context.groupKey;

    const showDialog= (element) => {
        const wp= parseWorldPt(getFieldVal(groupKey,targetKey));
        let workingCenterPt= centerPt;
        let newHipsFOVInDeg= wp ? 10 : hipsFOVInDeg;
        if (whichOverlay===CONE_CHOICE_KEY) {
            if (restOfProps.sizeKey && wp) {
                const sizeValue= Number(getFieldVal(groupKey,restOfProps.sizeKey));
                if (!isNaN(sizeValue) && sizeValue) newHipsFOVInDeg= sizeValue*10;
            }
        }
        else if (whichOverlay===POLY_CHOICE_KEY && polygonKey) {
            const wpAry= convertStrToWpAry(getFieldVal(groupKey,polygonKey));
            if (wpAry?.length>3) {
                const {centralPoint, maxRadius}= computeCentralPointAndRadius(wpAry);
                if (maxRadius>.0002) {
                    if (maxRadius<50) newHipsFOVInDeg= maxRadius*6;
                    else if (maxRadius<120) newHipsFOVInDeg= maxRadius*1.5;
                    else if (maxRadius<150) newHipsFOVInDeg= maxRadius*1.5;
                    else newHipsFOVInDeg= 360;
                }
                if (centralPoint) workingCenterPt= centralPoint;
            }
        }
        showHiPSPanelPopup({
            centerPt:workingCenterPt,
            popupClosing: () => connectContext?.setControlConnected(false),
            polygonKey, targetKey, element, groupKey, whichOverlay, ...restOfProps, hipsFOVInDeg: newHipsFOVInDeg});
        connectContext?.setControlConnected(true);
    };

    useEffect(() => {
        if (isDialogVisible(DIALOG_ID)) {
            showDialog();
        }
        return () => {
            if (hideHiPSPopupPanelOnDismount) {
                hideHiPSPopupPanel();
                connectContext?.setControlConnected(false);
            }
        };
    },[]);

    return (
        <ToolbarButton {...{
            icon:CLICK, tip, bgDark:true, horizontal:true, imageStyle:{height:18, width:18},
            onClick:(element) => showDialog(element)
        }}/>
    );
}




const defPopupPlotId= 'defaultHiPSPopupTargetSearch';

function showHiPSPanelPopup({popupClosing, element, plotId= defPopupPlotId,
                                whichOverlay= CONE_CHOICE_KEY, ...restOfProps}) {


    const doClose= () => {
        closeToolbarModalLayers();
        popupClosing();
    };

    const hipsPanel= (
        <PopupPanel title={'Choose Target'} layoutPosition={LayoutType.TOP_RIGHT_OF_BUTTON} element={element}
                    closeCallback={() => doClose()} >
            <div style={{
                padding: 3, display:'flex', flexDirection:'column', width: 500, height:400,
                background: 'rgb(200,200,200',
                alignItems:'center', resize:'both', overflow: 'hidden', zIndex:1}}>
                <HiPSTargetView {...{
                    plotId,
                    whichOverlay,
                    ...restOfProps,
                    style:{height:'100%', width:'100%', paddingBottom:4} }} />
            </div>
        </PopupPanel>
    );

    DialogRootContainer.defineDialog(DIALOG_ID, hipsPanel, element );
    dispatchShowDialog(DIALOG_ID);
}




let mocCnt=0;
const createMocTableId= () => `moc-table-${++mocCnt}`;

/**
 * plot the HiPS, set the center, FOV, user target selection layer, and the MOC layers.
 * Note this many options are disabled that do not make sense when using the HiPS for user input
 * @param obj
 * @param obj.plotId
 * @param obj.hipsUrl
 * @param obj.viewerId
 * @param obj.centerPt
 * @param obj.hipsFOVInDeg
 * @param obj.coordinateSys
 * @param obj.mocList
 * @param obj.userEnterWorldPt
 * @param obj.userEnterSearchRadius
 * @param obj.whichOverlay
 * @param obj.userEnterPolygon
 * @return {Promise<void>}
 */
async function initHiPSPlot({ hipsUrl, plotId, viewerId, centerPt, hipsFOVInDeg, coordinateSys, mocList,
                                userEnterWorldPt, userEnterSearchRadius, whichOverlay, userEnterPolygon}) {
    getDrawLayersByType(dlRoot(), HiPSMOC.TYPE_ID)
        .forEach( ({drawLayerId}) => dispatchDestroyDrawLayer(drawLayerId));// clean up any old moc layers
    const wpRequest= WebPlotRequest.makeHiPSRequest(hipsUrl, centerPt, hipsFOVInDeg);
    wpRequest.setPlotGroupId(plotId+'-group');
    wpRequest.setOverlayIds(['none']);
    wpRequest.setPlotId(plotId);
    if (coordinateSys) {
        wpRequest.setHipsUseCoordinateSys(coordinateSys);
    }
    else if (hipsFOVInDeg>200) {
        wpRequest.setHipsUseCoordinateSys(CoordinateSys.GALACTIC);
    }
    wpRequest.setHipsUseAitoffProjection(hipsFOVInDeg>130);
    dispatchPlotHiPS({plotId, wpRequest, viewerId,
        pvOptions: {canBeExpanded:false, useForSearchResults:false, displayFixedTarget:false, userCanDeletePlots: false,
            menuItemKeys: {
                zoomDropDownMenu: false, overlayColorLock: false, matchLockDropDown: false,
                recenter: false, selectArea: true,
            }
        }
    });
    await onPlotComplete(plotId);
    if (mocList) {
        const pList= mocList.map( ({mocUrl,title}) =>
            createHiPSMocLayer(createMocTableId(),title, mocUrl, primePlot(visRoot(),plotId), true, '') );
        await Promise.all(pList);
    }

    const dl= getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    !dl && dispatchCreateDrawLayer(SearchSelectTool.TYPE_ID);
    !isDrawLayerAttached(dl,plotId) && dispatchAttachLayerToPlot(SearchSelectTool.TYPE_ID,plotId,false);
    if (userEnterWorldPt || userEnterPolygon?.length) {
        updatePlotOverlayFromUserInput(plotId,whichOverlay, userEnterWorldPt, userEnterSearchRadius, userEnterPolygon, true);
    }
}



function attachSRegion(sRegion, plotId) {
    getDrawLayersByType(dlRoot(), ImageOutline.TYPE_ID)
        .forEach( ({drawLayerId}) => dispatchDestroyDrawLayer(drawLayerId));// clean up any old moc layers
    if (!sRegion) return;
    const {valid,drawObj}= parseObsCoreRegion(sRegion);
    if (!valid) return;
    const dl = dispatchCreateDrawLayer(ImageOutline.TYPE_ID,
        {drawObj, color: 'red', title:'s_region outline', destroyWhenAllDetached: true});
    if (!isDrawLayerAttached(dl, plotId)) dispatchAttachLayerToPlot(dl.drawLayerId, plotId, false);
}





function updatePlotOverlayFromUserInput(plotId, whichOverlay, wp, radius, polygonAry, forceCenterOn= false) {
    const dl= getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (!dl) return;
    const isCone= whichOverlay===CONE_CHOICE_KEY;

    dispatchAttributeChange( {plotId,
        changes:{
            [PlotAttribute.USER_SEARCH_WP]: isCone ? wp : undefined,
            [PlotAttribute.USER_SEARCH_RADIUS_DEG]: isCone ? radius : undefined,
            [PlotAttribute.POLYGON_ARY]: isCone ? undefined : polygonAry,
            [PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY]: isCone ? undefined : makeRelativePolygonAry(primePlot(visRoot(),plotId),polygonAry),
            [PlotAttribute.USE_POLYGON]: !isCone,
        }
    } );
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);

    const centerProjPt= isCone ? wp : computeCentralPointAndRadius(polygonAry)?.centralPoint;
    const cc= CsysConverter.make(primePlot(visRoot(),plotId));
    if (!centerProjPt || !cc) return;
    if (cc.pointInView(centerProjPt) && !forceCenterOn) return;
    dispatchChangeCenterOfProjection({plotId, centerProjPt});
}


const radians= [Math.PI/4, 2*Math.PI/4, 3*Math.PI/4, 4*Math.PI/4, 5*Math.PI/4, 6*Math.PI/4, 7*Math.PI/4, 8*Math.PI/4];

/**
 * @param {WebPlot} plot
 * @return {{}|{radius: number, cenWpt: WorldPt}}
 */
function getDetailsFromSelection(plot) {
    const {pt0,pt1}= plot.attributes[PlotAttribute.SELECTION];
    if (!pt0 || !pt1) return {};
    const cc= CsysConverter.make(plot);
    const dPt0= cc.getDeviceCoords(pt0);
    const dPt1= cc.getDeviceCoords(pt1);
    const cen= makeDevicePt( (dPt0.x+dPt1.x)/2, (dPt0.y+dPt1.y)/2 );
    const cenWpt= cc.getWorldCoords(cen);
    if (!cenWpt) return {};
    const sideWPx= cc.getWorldCoords( makeDevicePt( dPt0.x,cen.y));
    const sideWPy= cc.getWorldCoords( makeDevicePt( cen.x,dPt0.y));
    const radius= Math.min(computeDistance(sideWPx,cenWpt), computeDistance(sideWPy,cenWpt));
    let corners;
    const rx= cen.x-dPt0.x;
    const ry= cen.y-dPt0.y;


    if (plot.attributes[PlotAttribute.SELECTION_TYPE]===SelectedShape.circle.key) {
        corners= radians.map( (radian) => {
            const {x,y}= getPointOnEllipse(cen.x,cen.y,rx,ry,radian);
            return cc.getWorldCoords(makeDevicePt(x,y));
        });
    }
    else {
        const ptCorner01= cc.getWorldCoords(makeDevicePt(dPt0.x, dPt1.y));
        const ptCorner10= cc.getWorldCoords(makeDevicePt(dPt1.x, dPt0.y));
        corners= [pt0,ptCorner01,pt1,ptCorner10];
    }

    const relativeDevCorners= corners.map( (pt) => {
        const dPt= cc.getDeviceCoords(pt);
        return makeDevicePt( cen.x-dPt.x, cen.y-dPt.y );
    });


    return {cenWpt, radius, corners, relativeDevCorners};
}

function makeRelativePolygonAry(plot, polygonAry) {
    const cc= CsysConverter.make(plot);
    if (!cc) return;
    const dAry= polygonAry.map( (pt) => cc.getImageCoords(pt)).filter( (pt) => pt);
    if (dAry.length <3) return;
    const avgX= dAry.reduce( (sum,{x}) => sum+x,0)/dAry.length;
    const avgY= dAry.reduce( (sum,{y}) => sum+y,0)/dAry.length;
    const cen= makeImagePt(avgX,avgY);
    const relDevPtAry= dAry.map( (pt) => makeImagePt(cen.x-pt.x, cen.y-pt.y));
    return relDevPtAry;
}