import {isString} from 'lodash';
import React from 'react';
import SELECT_NONE from 'images/icons-2014/28x28_Rect_DD.png';
import SearchSelectTool from '../../drawingLayers/SearchSelectTool.js';
import {SelectedShape} from '../../drawingLayers/SelectedShape.js';
import {sprintf} from '../../externalSource/sprintf.js';
import {splitByWhiteSpace} from '../../util/WebUtil.js';
import CsysConverter from '../CsysConverter.js';
import {
    dispatchAttachLayerToPlot, dispatchChangeDrawingDef, dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchForceDrawLayerUpdate, dispatchModifyCustomField, getDlAry
} from '../DrawLayerCntlr.js';
import {dispatchAttributeChange, dispatchChangeCenterOfProjection, visRoot} from '../ImagePlotCntlr.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {getDrawLayerByType, isDrawLayerAttached, primePlot} from '../PlotViewUtil.js';
import {isValidPoint, makeDevicePt, makeImagePt, makeWorldPt, parseWorldPt, pointEquals} from '../Point.js';
import {computeCentralPointAndRadius, computeDistance, getPointOnEllipse} from '../VisUtil.js';
import {isImage} from '../WebPlot.js';
import {CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './CommonUIKeys.js';
import {closeToolbarModalLayers} from './VisMiniToolbar.jsx';


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
    const {y:yDiagUp}= getPointOnEllipse(cen.x,cen.y,rx,ry,1*PI/8);
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
    if (!cenWpt) return {};
    const sideWPx= cc.getWorldCoords( makeDevicePt( dPt0.x,cen.y));
    const sideWPy= cc.getWorldCoords( makeDevicePt( cen.x,dPt0.y));
    const radius= Math.min(computeDistance(sideWPx,cenWpt), computeDistance(sideWPy,cenWpt));
    let corners;
    const rx= cen.x-dPt0.x;
    const ry= cen.y-dPt0.y;


    if (plot.attributes[PlotAttribute.SELECTION_TYPE]===SelectedShape.circle.key) {
        corners= getCircumscribedCorners(cc,cen,rx,ry);
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
    const ptStrAry = str?.split(',');
    if (!(ptStrAry?.length > 1)) return [];
    const wpAry = ptStrAry
        .map((s) => splitByWhiteSpace(s))
        .filter((sAry) => sAry.length === 2 && !isNaN(Number(sAry[0])) && !isNaN(Number(sAry[1])))
        .map((sAry) => makeWorldPt(sAry[0], sAry[1]));
    return wpAry;
}

export function convertWpAryToStr(wpAry, plot) {
    const cc = CsysConverter.make(plot);
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
    isDrawLayerAttached(dl, plotId) && dispatchDestroyDrawLayer(dl.drawLayerId);
}

export function updateUIFromPlot(plotId, whichOverlay, setTargetWp, getTargetWp, setHiPSRadius, getHiPSRadius, setPolygon, getPolygon, minSize, maxSize) {


    const userEnterWorldPt = () => parseWorldPt(getTargetWp());
    const userEnterSearchRadius = () => Number(getHiPSRadius());
    const userEnterPolygon = () => convertStrToWpAry(getPolygon());

    if (whichOverlay !== CONE_CHOICE_KEY && whichOverlay !== POLY_CHOICE_KEY) return;
    const plot = primePlot(visRoot(), plotId);
    if (!plot) return;
    const isCone = whichOverlay === CONE_CHOICE_KEY;
    const {cenWpt, radius, corners} = plot.attributes[PlotAttribute.SELECTION] ? getDetailsFromSelection(plot) : {};

    if (isCone) {
        if (plot.attributes[PlotAttribute.SELECTION]) {
            if (!cenWpt) return;
            const drawRadius = radius <= maxSize ? (radius >= minSize ? radius : minSize) : maxSize;
            if (pointEquals(userEnterWorldPt(), cenWpt) && drawRadius === userEnterSearchRadius()) return;
            setTargetWp(cenWpt.toString());
            setHiPSRadius(drawRadius + '');
            updatePlotOverlayFromUserInput(plotId, whichOverlay, cenWpt, drawRadius, undefined);
            setTimeout(() => closeToolbarModalLayers(), 10);
        } else {
            const wp = plot.attributes[PlotAttribute.USER_SEARCH_WP];
            if (!wp) return;
            const utWPt = userEnterWorldPt();
            if (!utWPt || (isValidPoint(utWPt) && !pointEquals(wp, utWPt))) {
                setTargetWp(wp.toString());
            }
        }
    } else {
        if (plot.attributes[PlotAttribute.SELECTION]) {
            if (isWpArysEquals(corners, userEnterPolygon())) return;
            setPolygon(convertWpAryToStr(corners, plot));
            setTimeout(() => closeToolbarModalLayers(), 10);
        } else {
            const polyWpAry = plot.attributes[PlotAttribute.POLYGON_ARY];
            if (polyWpAry?.length) {
                if (isWpArysEquals(polyWpAry, userEnterPolygon())) return;
                setPolygon(convertWpAryToStr(polyWpAry, plot));
            }
        }
    }
}

export function updatePlotOverlayFromUserInput(plotId, whichOverlay, wp, radius, polygonAry, forceCenterOn = false) {
    const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (!dl) return;
    const isCone = whichOverlay === CONE_CHOICE_KEY;

    dispatchChangeDrawingDef(dl.drawLayerId,{...dl.drawingDef,color:'yellow'},plotId);
    dispatchModifyCustomField(dl.drawLayerId,{isInteractive: true},plotId);

    dispatchAttributeChange({
        plotId,
        changes: {
            [PlotAttribute.USER_SEARCH_WP]: isCone ? wp : undefined,
            [PlotAttribute.USER_SEARCH_RADIUS_DEG]: isCone ? radius : undefined,
            [PlotAttribute.POLYGON_ARY]: isCone ? undefined : polygonAry,
            [PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY]: isCone ? undefined : makeRelativePolygonAry(primePlot(visRoot(), plotId), polygonAry),
            [PlotAttribute.USE_POLYGON]: !isCone,
        }
    });
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);
    const plot= primePlot(visRoot());
    if (!plot || isImage(plot)) return;
    if (!isCone && !polygonAry) return;

    const centerProjPt = isCone ? wp : computeCentralPointAndRadius(polygonAry)?.centralPoint;
    const cc = CsysConverter.make(plot);
    if (!centerProjPt || !cc) return;
    if (cc.pointInView(centerProjPt) && !forceCenterOn) return;
    dispatchChangeCenterOfProjection({plotId, centerProjPt});
}


export const HelpLines= ({whichOverlay}) =>
    (whichOverlay===CONE_CHOICE_KEY ?
        ( <div>
            <span>Click to choose a search center, or use the Selection Tools (</span>
            <img style={{width:15, height:15, verticalAlign:'text-top'}} src={SELECT_NONE}/>
            <span>) to choose a search center and radius.</span>
        </div> ) :
        ( <div>
            <span>Use the Selection Tools (</span>
            <img style={{width:15, height:15, verticalAlign:'text-top'}} src={SELECT_NONE}/>
            <span>)  to choose a search polygon. Click to change the center. </span>
        </div> ));
