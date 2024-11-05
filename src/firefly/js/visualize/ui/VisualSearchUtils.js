import {isString} from 'lodash';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import SearchSelectTool from '../../drawingLayers/SearchSelectTool.js';
import SelectArea, {getImageBoundsSelection} from '../../drawingLayers/SelectArea.js';
import {SelectedShape} from '../../drawingLayers/SelectedShape.js';
import {sprintf} from '../../externalSource/sprintf.js';
import {AREA} from '../../ui/dynamic/DynamicDef.js';
import {splitByWhiteSpace} from '../../util/WebUtil.js';
import CsysConverter, {CysConverter} from '../CsysConverter.js';
import {
    dispatchAttachLayerToPlot, dispatchChangeDrawingDef, dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchForceDrawLayerUpdate, dispatchModifyCustomField, getDlAry
} from '../DrawLayerCntlr.js';
import {dispatchAttributeChange, dispatchChangeCenterOfProjection, visRoot} from '../ImagePlotCntlr.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {getDrawLayerByType, getPlotViewById, isDrawLayerAttached, primePlot} from '../PlotViewUtil.js';
import {isValidPoint, makeDevicePt, makeImagePt, makeWorldPt, parseWorldPt, pointEquals} from '../Point.js';
import {
    calculatePosition, computeCentralPointAndRadius, computeDistance, getPointOnEllipse
} from '../VisUtil.js';
import {changeHiPSProjectionCenter, getDevPixScaleDeg, isImage} from '../WebPlot.js';
import {CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './CommonUIKeys.js';
import {
    clearModalEndInfo, closeToolbarModalLayers, getModalEndInfo, setModalEndInfo
} from './ToolbarToolModalEnd.js';
import {dispatchActiveTarget} from '../../core/AppDataCntlr.js';


export const SEARCH_REFINEMENT_DIALOG_ID = 'SEARCH_REFINEMENT_DIALOG';
const SEARCH_REFINEMENT_SELECTION_SOURCE= 'SearchRefinementTool';
const radians= [Math.PI/4, 2*Math.PI/4, 3*Math.PI/4, 4*Math.PI/4, 5*Math.PI/4, 6*Math.PI/4, 7*Math.PI/4, 8*Math.PI/4];

function getInscribedCorners(cc,cen,rx,ry) {
    const corners= radians.map( (radian) => {
        const {x,y}= getPointOnEllipse(cen.x,cen.y,rx,ry,radian);
        return cc.getWorldCoords(makeDevicePt(x,y));
    });
    return corners;
}

function getCircumscribedCorners(cc,cen,rx,ry) {
    const PI= Math.PI;
    const {y:topY}= getPointOnEllipse(cen.x,cen.y,rx,ry,4*PI/8);
    const {x:xDiagLeft}= getPointOnEllipse(cen.x,cen.y,rx,ry,3*PI/8);
    const {x:xDiagRight}= getPointOnEllipse(cen.x,cen.y,rx,ry,5*PI/8);
    const {y:bottomY}= getPointOnEllipse(cen.x,cen.y,rx,ry,12*PI/8);

    const {x:leftX}= getPointOnEllipse(cen.x,cen.y,rx,ry,16*PI/8);
    const {x:rightX}= getPointOnEllipse(cen.x,cen.y,rx,ry,8*PI/8);
    const {y:yDiagUp}= getPointOnEllipse(cen.x,cen.y,rx,ry,PI/8);
    const {y:yDiagDown}= getPointOnEllipse(cen.x,cen.y,rx,ry,15*PI/8);

    return [
        cc.getWorldCoords(makeDevicePt(xDiagLeft,topY)),
        cc.getWorldCoords(makeDevicePt(xDiagRight,topY)),
        cc.getWorldCoords(makeDevicePt(rightX,yDiagUp)),
        cc.getWorldCoords(makeDevicePt(rightX,yDiagDown)),
        cc.getWorldCoords(makeDevicePt(xDiagRight,bottomY)),
        cc.getWorldCoords(makeDevicePt(xDiagLeft,bottomY)),
        cc.getWorldCoords(makeDevicePt(leftX,yDiagDown)),
        cc.getWorldCoords(makeDevicePt(leftX,yDiagUp)),
    ];
}

/**
 * @param {WebPlot|undefined} plot
 * @return {{}|{radius: number, cenWpt: WorldPt}}
 */
export function getDetailsFromSelection(plot) {
    if (!plot) return {};
    const {pt0,pt1}= plot.attributes[PlotAttribute.SELECTION] ?? {};
    if (!pt0 || !pt1) return {};
    const cc= CsysConverter.make(plot);
    const dPt0= cc.getDeviceCoords(pt0);
    const dPt1= cc.getDeviceCoords(pt1);
    if (!dPt0 || !dPt1) return {};
    const cen= makeDevicePt( (dPt0.x+dPt1.x)/2, (dPt0.y+dPt1.y)/2 );
    const cenWpt= cc.getWorldCoords(cen);
    const sideWPx= cc.getWorldCoords( makeDevicePt( dPt0.x,cen.y));
    const sideWPy= cc.getWorldCoords( makeDevicePt( cen.x,dPt0.y));
    if (!cenWpt || !sideWPx || !sideWPy) return {};
    const radiusInit= Math.min(computeDistance(sideWPx,cenWpt), computeDistance(sideWPy,cenWpt));
    const radius= plot.attributes[PlotAttribute.SELECTION_SOURCE]===SEARCH_REFINEMENT_SELECTION_SOURCE &&
                  plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG] ?
               plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG] : Math.trunc(radiusInit*3600)/3600;
    let corners;
    const rx= cen.x-dPt0.x;
    const ry= cen.y-dPt0.y;

    const cone= plot.attributes[PlotAttribute.SELECTION_TYPE]===SelectedShape.circle.key;

    if (cone) {
        corners= getCircumscribedCorners(cc,cen,rx,ry);
    }
    else {
        const ptCorner01= cc.getWorldCoords(makeDevicePt(dPt0.x, dPt1.y));
        const ptCorner10= cc.getWorldCoords(makeDevicePt(dPt1.x, dPt0.y));
        corners= [pt0,ptCorner01,pt1,ptCorner10].filter( (pt) => pt);
    }

    let relativeDevCorners= corners
        .map( (pt) => {
            const dPt= cc.getDeviceCoords(pt);
            if (!dPt) return;
            return makeDevicePt( cen.x-dPt.x, cen.y-dPt.y );
        })
        .filter( (pt) => pt);

    if (relativeDevCorners?.length!==corners.length) relativeDevCorners= undefined;


    return {cenWpt, radius, corners, relativeDevCorners, cone};
}

export function makeRelativePolygonAry(plot, polygonAry) {
    if (!polygonAry) return;
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

export function convertStrToWpAry(str) {
    if (!str) return;
    if (!isString(str)) return [];
   /* const ptStrAry = str?.split(',');
    if (!(ptStrAry?.length > 1)) return [];
    const wpAry = ptStrAry
        .map((s) => splitByWhiteSpace(s))
        .filter((sAry) => sAry.length === 2 && !isNaN(Number(sAry[0])) && !isNaN(Number(sAry[1])))
        .map((sAry) => makeWorldPt(sAry[0], sAry[1]));
    return wpAry;*/

    //this is the logic to handle polygon pairs with or without commas, just reduce the array into pairs
    const normalizedInput = str.replace(/,/g, ' '); //replace every comma with a space
    const ptStrAry = splitByWhiteSpace(normalizedInput);
    if (!(ptStrAry?.length > 2)) return [];

    const pairs = ptStrAry.reduce((acc, curr, index) => {
        if (index % 2 === 0) {
            acc.push([curr]);
        } else {
            acc[acc.length - 1].push(curr);
        }
        return acc;
    }, []);

    // Filter valid pairs and map to makeWorldPt
    const wpAry = pairs
        .filter((pair) => pair.length === 2 && !isNaN(Number(pair[0])) && !isNaN(Number(pair[1])))
        .map((pair) => makeWorldPt(pair[0], pair[1]));
    return wpAry;
}

export function convertWpAryToStr(wpAry, plot) {
    const cc = CsysConverter.make(plot);
    if (!cc || !wpAry?.length) return '';
    return wpAry.reduce((fullStr, pt, idx) => {
        const wpt = cc.getWorldCoords(pt);
        return wpt ? `${fullStr}${idx > 0 ? ', ' : ''}${sprintf('%.6f', wpt.x)} ${sprintf('%.6f', wpt.y)}` : fullStr;
    }, '');
}

export function isWpArysEquals(wpAry1, wpAry2) {
    if (wpAry1?.length !== wpAry2?.length) return false;
    return wpAry1.filter((wp, idx) => pointEquals(wp, wpAry2[idx])).length === wpAry1.length;
}

export function initSearchSelectTool(plotId) {
    const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    !dl && dispatchCreateDrawLayer(SearchSelectTool.TYPE_ID);
    !isDrawLayerAttached(dl, plotId) && dispatchAttachLayerToPlot(SearchSelectTool.TYPE_ID, plotId, false);
}
export function removeSearchSelectTool(plotId) {
    const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (dl) dispatchDestroyDrawLayer(dl.drawLayerId);
    dispatchAttributeChange({
        plotId,
        changes: {
            [PlotAttribute.USER_SEARCH_WP]: undefined,
            [PlotAttribute.USER_SEARCH_RADIUS_DEG]: undefined,
            [PlotAttribute.POLYGON_ARY]: undefined,
            [PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY]: undefined,
            [PlotAttribute.SELECTION] : undefined,
            [PlotAttribute.SELECTION_TYPE] : undefined,
            [PlotAttribute.SELECTION_SOURCE] : undefined,
            [PlotAttribute.IMAGE_BOUNDS_SELECTION]: undefined,
        }
    });
}

export function updateUIFromPlot({plotId, setWhichOverlay, whichOverlay, setTargetWp, getTargetWp, canUpdateModalEndInfo=true,
                                     setHiPSRadius, getHiPSRadius, setPolygon, getPolygon, minSize, maxSize }) {

    const userEnterWorldPt = () => parseWorldPt(getTargetWp());
    const userEnterSearchRadius = () => Number(getHiPSRadius());
    const userEnterPolygon = () => convertStrToWpAry(getPolygon());

    if (whichOverlay !== CONE_CHOICE_KEY && whichOverlay !== POLY_CHOICE_KEY) return;
    const plot = primePlot(visRoot(), plotId);
    if (!plot) return;
    let isCone = whichOverlay === CONE_CHOICE_KEY;
    const {cenWpt, radius, corners} = plot.attributes[PlotAttribute.SELECTION] ? getDetailsFromSelection(plot) : {};
    const plotSelType= plot.attributes[PlotAttribute.SELECTION_TYPE];
    if (setWhichOverlay && plotSelType) {
        isCone= plotSelType!==SelectedShape.rect.key; // if future, if something not supported just default to cone
        if (plot.attributes[PlotAttribute.SELECTION_SOURCE]!==SEARCH_REFINEMENT_SELECTION_SOURCE) {
            setWhichOverlay(isCone ? CONE_CHOICE_KEY : POLY_CHOICE_KEY);
        }
    }

    if (isCone) {
        if (plot.attributes[PlotAttribute.SELECTION] && plot.attributes[PlotAttribute.SELECTION_SOURCE]===SelectArea.TYPE_ID) {
            if (!cenWpt) return;
            const drawRadius = radius <= maxSize ? (radius >= minSize ? radius : minSize) : maxSize;
            if (pointEquals(userEnterWorldPt(), cenWpt) && drawRadius === userEnterSearchRadius()) return;
            setTargetWp(cenWpt.toString());
            dispatchActiveTarget(cenWpt);
            setHiPSRadius(drawRadius + '');
            updatePlotOverlayFromUserInput(plotId, CONE_CHOICE_KEY, cenWpt, drawRadius, undefined);
            setTimeout(() => {
                canUpdateModalEndInfo ? updateModalEndInfo(plot.plotId) : closeToolbarModalLayers();
            }, 10);
        }
        else {
            const wp = plot.attributes[PlotAttribute.USER_SEARCH_WP];
            if (!wp) return;
            const utWPt = userEnterWorldPt();
            if (!utWPt || (isValidPoint(utWPt) && !pointEquals(wp, utWPt))) {
                setTargetWp(wp.toString());
                dispatchActiveTarget(wp);
                if (plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG]) {
                    canUpdateModalEndInfo ? updateModalEndInfo(plot.plotId) : closeToolbarModalLayers();
                }
            }
        }
    }
    else {
        let wpStr;
        if (plot.attributes[PlotAttribute.SELECTION] && plot.attributes[PlotAttribute.SELECTION_SOURCE]===SelectArea.TYPE_ID) {
            if (isWpArysEquals(corners, userEnterPolygon())) return;
            wpStr= cenWpt.toString();
            setPolygon(convertWpAryToStr(corners, plot));
            setTimeout(() => {
                canUpdateModalEndInfo ? updateModalEndInfo(plot.plotId) : closeToolbarModalLayers();
            }, 10);
        } else {
            const polyWpAry = plot.attributes[PlotAttribute.POLYGON_ARY];
            wpStr = plot.attributes[PlotAttribute.USER_SEARCH_WP];
            if (polyWpAry?.length) {
                if (isWpArysEquals(polyWpAry, userEnterPolygon())) return;
                setPolygon(convertWpAryToStr(polyWpAry, plot));
                canUpdateModalEndInfo ? updateModalEndInfo(plot.plotId) : closeToolbarModalLayers();
            }
        }
        if (wpStr && wpStr !== getTargetWp()) setTargetWp(wpStr);
    }
}


function convertConeToSelection(plot,wp,radius) {
    if (!wp) return {};
    const tmpPlot= changeHiPSProjectionCenter(plot, wp);
    const dist= radius/(getDevPixScaleDeg(tmpPlot));
    const ccTmpPlot= CysConverter.make(tmpPlot);
    const cen= ccTmpPlot.getDeviceCoords(wp);
    if (!cen) return {};
    const pt0= ccTmpPlot.getWorldCoords(makeDevicePt(cen.x-dist, cen.y+dist));
    const pt1= ccTmpPlot.getWorldCoords(makeDevicePt(cen.x+dist, cen.y-dist));
    if (!pt0 || !pt1) return {};
    const sel= {pt0,pt1};
    const imBoundSel= getImageBoundsSelection(sel,ccTmpPlot, SelectedShape.circle.key,
        getPlotViewById(visRoot(),plot.plotId)?.rotation ?? 0);
    return {
        [PlotAttribute.SELECTION]: sel,
        [PlotAttribute.SELECTION_TYPE]: SelectedShape.circle.key,
        [PlotAttribute.IMAGE_BOUNDS_SELECTION]: imBoundSel,
        [PlotAttribute.SELECTION_SOURCE]: SEARCH_REFINEMENT_SELECTION_SOURCE,
    };
}


function convertPolygonToSelection(plot,polygonAry) {
    if (!polygonAry?.length) return {};
    const cc= CsysConverter.make(plot);
    const devAry= polygonAry.map( (pt) => cc.getDeviceCoords(pt)).filter( (pt) => pt);
    if (devAry.length<3) return {};
    const xAry= devAry.map( ({x}) => x);
    const yAry= devAry.map( ({y}) => y);
    const minX= Math.min(...xAry);
    const minY= Math.min(...yAry);
    const maxX= Math.max(...xAry);
    const maxY= Math.max(...yAry);
    const pt0= cc.getWorldCoords(makeDevicePt(minX,maxY));
    const pt1= cc.getWorldCoords(makeDevicePt(maxX,minY));
    if (!pt0 || !pt1) return {};
    const sel= {pt0,pt1};
    const imBoundSel= getImageBoundsSelection(sel,CsysConverter.make(plot), SelectedShape.rect.key,
        getPlotViewById(visRoot(),plot.plotId)?.rotation ?? 0);
    return {
        [PlotAttribute.SELECTION]: sel,
        [PlotAttribute.SELECTION_TYPE]: SelectedShape.rect.key,
        [PlotAttribute.IMAGE_BOUNDS_SELECTION]: imBoundSel,
        [PlotAttribute.SELECTION_SOURCE]: SEARCH_REFINEMENT_SELECTION_SOURCE,
    };
    
}


function convertToSelection(plot, wp,radius,polygonAry,whichOverlay) {
    if (!plot) return {};
    return (whichOverlay===CONE_CHOICE_KEY) ?
        convertConeToSelection(plot,wp,radius) :
        convertPolygonToSelection(plot,polygonAry);
}

export function updatePlotOverlayFromUserInput(plotId, whichOverlay, wp, radius, polygonAry, forceCenterOn = false, canGeneratePolygon= false) {
    const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (!dl) return;
    const isCone = whichOverlay === CONE_CHOICE_KEY;
    const plot= primePlot(visRoot(),plotId);

    dispatchChangeDrawingDef(dl.drawLayerId,{...dl.drawingDef,color:'yellow'},plotId);
    dispatchModifyCustomField(dl.drawLayerId,{isInteractive: true},plotId);

    if (!isCone && wp && radius && canGeneratePolygon) {
        const cc= CsysConverter.make(plot);
        const cen= cc.getDeviceCoords(wp);
        const ptOnCone= cc.getDeviceCoords(calculatePosition(wp,radius*3600,radius*3600));
        const dist= Math.abs(cen.y-ptOnCone.y)*2;
        polygonAry= getCircumscribedCorners(cc,cen,dist,dist);
    }

    let changes= {
        [PlotAttribute.USER_SEARCH_WP]: wp,
        [PlotAttribute.USER_SEARCH_RADIUS_DEG]: isCone ? radius : undefined,
        [PlotAttribute.POLYGON_ARY]: isCone ? undefined : polygonAry,
        [PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY]: isCone ? undefined : makeRelativePolygonAry(primePlot(visRoot(), plotId), polygonAry),
        [PlotAttribute.USE_POLYGON]: !isCone,
    };
    if (whichOverlay) {
        changes=  {...changes, ...convertToSelection(plot, wp,radius,polygonAry,whichOverlay)};
    }

    dispatchAttributeChange({ plotId, changes });
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);
    if (!plot || isImage(plot)) return;
    if (!isCone && !polygonAry) return;

    const centerProjPt = isCone ? wp : computeCentralPointAndRadius(polygonAry)?.centralPoint;
    const cc = CsysConverter.make(plot);
    if (!centerProjPt || !cc) return;
    if (cc.pointInView(centerProjPt) && !forceCenterOn) return;
    dispatchChangeCenterOfProjection({plotId, centerProjPt});
}

export function updateModalEndInfo(plotId) {
    const modalEndInfo = getModalEndInfo();
    if (modalEndInfo?.key!=='SearchRefinement') modalEndInfo?.closeLayer?.();
    setModalEndInfo({
        closeLayer:(key) => {
            if (key===SelectArea.TYPE_ID)  return;
            dispatchHideDialog(SEARCH_REFINEMENT_DIALOG_ID);
            removeSearchSelectTool(plotId);
            clearModalEndInfo();
        },
        closeText:'End Search Marker',
        key: 'SearchRefinement',
        callIfReplaced: true,
        plotIdAry:[plotId],
    });
}


/**
 *
 * @param {ClickToActionCommand} sa
 * @param plotId
 * @param obj
 * @param obj.wp
 * @param obj.radius
 * @param obj.polyStr
 */
export function markOutline(sa, plotId, {wp, radius, polyStr}) {
    initSearchSelectTool(plotId);
    const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (!dl) return;
    if ((!wp || !radius) && !polygonAry) return;
    let isCone = wp && radius;
    const polygonAry = convertStrToWpAry(polyStr);
    if (polygonAry && sa.searchType === AREA) isCone = false;
    updateModalEndInfo(plotId);

    updatePlotOverlayFromUserInput(plotId, isCone ? CONE_CHOICE_KEY : POLY_CHOICE_KEY, wp, radius, polygonAry);
    dispatchModifyCustomField(dl.drawLayerId, {isInteractive: false}, plotId);
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);
}