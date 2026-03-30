import {isString, isUndefined} from 'lodash';
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
    calculatePosition,
    calculatePositionFromLocalOffsets,
    computeCentralPointAndRadius,
    computeDistance,
    convertCelestial,
    getPointOnEllipse,
    getPositionAngle,
    normalizeRotation,
} from '../VisUtil.js';
import {changeHiPSProjectionCenter, getDevPixScaleDeg, isImage} from '../WebPlot.js';
import {BOX_CHOICE_KEY, CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './CommonUIKeys.js';
import {
    clearModalEndInfo, closeToolbarModalLayers, getModalEndInfo, setModalEndInfo
} from './ToolbarToolModalEnd.js';
import {dispatchActiveTarget} from '../../core/AppDataCntlr.js';
import {clampInRange} from 'firefly/util/MathUtil';


export const SEARCH_REFINEMENT_DIALOG_ID = 'SEARCH_REFINEMENT_DIALOG';
const SEARCH_SELECT_TOOL_SOURCE = SearchSelectTool.TYPE_ID;
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
    const cenDevPt= makeDevicePt( (dPt0.x+dPt1.x)/2, (dPt0.y+dPt1.y)/2 );
    const cenWpt= cc.getWorldCoords(cenDevPt); // in Equ
    const sideWPx= cc.getWorldCoords( makeDevicePt( dPt0.x,cenDevPt.y)); // in Equ
    const sideWPy= cc.getWorldCoords( makeDevicePt( cenDevPt.x,dPt0.y)); // in Equ
    if (!cenWpt || !sideWPx || !sideWPy) return {};
    const radiusInit= Math.min(computeDistance(sideWPx,cenWpt), computeDistance(sideWPy,cenWpt));
    const radius= plot.attributes[PlotAttribute.SELECTION_SOURCE]===SEARCH_SELECT_TOOL_SOURCE &&
                  plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG] ?
               plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG] : Math.trunc(radiusInit*3600)/3600;
    let corners, rectSizeX, rectSizeY, rectRotAngle, rectXAxisWpt, rectYAxisWpt;
    const rx= cenDevPt.x-dPt0.x;
    const ry= cenDevPt.y-dPt0.y;

    const cone= plot.attributes[PlotAttribute.SELECTION_TYPE]===SelectedShape.circle.key;

    if (cone) { // SelectedShape.circle
        corners= getCircumscribedCorners(cc,cenDevPt,rx,ry);
    }
    else { // SelectedShape.rect
        const ptX0Y1= cc.getWorldCoords(makeDevicePt(dPt0.x, dPt1.y)); // in Equ
        const ptX1Y0= cc.getWorldCoords(makeDevicePt(dPt1.x, dPt0.y)); // in Equ
        corners = [pt0,ptX0Y1,pt1,ptX1Y0].filter((pt) => pt); //poly UI params

        rectXAxisWpt = cc.getWorldCoords(makeDevicePt(dPt0.x, cenDevPt.y)); // in Equ
        rectYAxisWpt = cc.getWorldCoords(makeDevicePt(cenDevPt.x, dPt0.y)); // in Equ
        rectSizeX = computeDistance(cenWpt, rectXAxisWpt) * 2;
        rectSizeY = computeDistance(cenWpt, rectYAxisWpt) * 2;
        rectRotAngle = getPositionAngle(
            cenWpt.getLon(), cenWpt.getLat(),
            rectYAxisWpt.getLon(), rectYAxisWpt.getLat() // since rotation angle is E of N (where Y axis = N at 0 deg)
        );
    }

    let relativeDevCorners= corners
        .map( (pt) => {
            const dPt= cc.getDeviceCoords(pt);
            if (!dPt) return;
            return makeDevicePt( cenDevPt.x-dPt.x, cenDevPt.y-dPt.y );
        })
        .filter( (pt) => pt);

    if (relativeDevCorners?.length!==corners.length) relativeDevCorners= undefined;


    return {cenWpt, radius, corners, rectSizeX, rectSizeY, rectRotAxisWpt: rectYAxisWpt, rectRotAngle, relativeDevCorners, cone};
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
    // create a SearchSelect draw layer and attach it to the plot (if not already done)
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


function handleConeUIFromPlot({plot, plotId, isSelectAreaDLOnPlot, cenWpt, radius,
                                  setWpAndDispatch, doModalEnd, userEnterWorldPt, userEnterSearchRadius,
                                  setHiPSRadius, getHiPSRadius, minSize, maxSize}) {
    if (isSelectAreaDLOnPlot) {
        if (!cenWpt) return;
        const drawRadius = clampInRange(radius, minSize, maxSize);
        if (pointEquals(userEnterWorldPt(), cenWpt) && drawRadius === userEnterSearchRadius()) return;
        setWpAndDispatch(cenWpt);
        setUISize(radius, minSize, maxSize, userEnterSearchRadius(), setHiPSRadius);
        updatePlotOverlayFromUserInput({plotId, whichOverlay: CONE_CHOICE_KEY, wp: cenWpt, radius: drawRadius, polygonAry: undefined});
        setTimeout(doModalEnd, 10);
    } else {
        const wp = plot.attributes[PlotAttribute.USER_SEARCH_WP];
        if (!wp) return;
        const utWPt = userEnterWorldPt();
        if (Number(getHiPSRadius()) !== Number(plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG])) {
            setUISize(plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG], minSize, maxSize, userEnterSearchRadius(), setHiPSRadius);
        }
        if (!utWPt || (isValidPoint(utWPt) && !pointEquals(wp, utWPt))) {
            setWpAndDispatch(wp);
            if (plot.attributes[PlotAttribute.USER_SEARCH_RADIUS_DEG]) doModalEnd();
        }
    }
}

function handleBoxUIFromPlot({plot, plotId, isSelectAreaDLOnPlot, cenWpt, rectSizeX, rectSizeY, rectRotAngle, rectRotAxisWpt,
                                 setWpAndDispatch, doModalEnd, userEnterWorldPt, getBoxParams, setBoxParams, minSize, maxSize}) {
    if (isSelectAreaDLOnPlot) {
        if (!cenWpt) return;
        const drawSizeX = clampInRange(rectSizeX, minSize, maxSize);
        const drawSizeY = clampInRange(rectSizeY, minSize, maxSize);
        const rotation = rectRotAngle;
        if (pointEquals(userEnterWorldPt(), cenWpt) &&
            drawSizeX === getBoxParams()?.sizeX &&
            drawSizeY === getBoxParams()?.sizeY &&
            rotation === getBoxParams()?.rotation) return;
        setWpAndDispatch(cenWpt);
        const drawBox = {sizeX: drawSizeX, sizeY: drawSizeY, rotation: normalizeRotation(rotation)};
        setBoxParams(drawBox);
        dispatchAttributeChange({plotId, changes: {
            [PlotAttribute.USER_SEARCH_BOX]: drawBox,
            [PlotAttribute.USER_SEARCH_BOX_AXIS_WP]: rectRotAxisWpt
        }});
        setTimeout(doModalEnd, 10);
    } else {
        const wp = plot.attributes[PlotAttribute.USER_SEARCH_WP];
        if (!wp) return;
        const utWPt = userEnterWorldPt();
        const boxParams = plot.attributes[PlotAttribute.USER_SEARCH_BOX];
        if (getBoxParams()?.sizeX !== boxParams?.sizeX ||
            getBoxParams()?.sizeY !== boxParams?.sizeY ||
            getBoxParams()?.rotation !== boxParams?.rotation) {
            setBoxParams(boxParams);
        }
        if (!utWPt || (isValidPoint(utWPt) && !pointEquals(wp, utWPt))) {
            setWpAndDispatch(wp);
            if (boxParams?.sizeX && boxParams?.sizeY) doModalEnd();
        }
    }
}

function handlePolygonUIFromPlot({plot, plotId, isSelectAreaDLOnPlot, cenWpt, corners,
                                     setTargetWp, getTargetWp, doModalEnd, userEnterPolygon, setPolygon}) {
    let wpStr;
    if (isSelectAreaDLOnPlot) {
        if (isWpArysEquals(corners, userEnterPolygon())) return;
        wpStr = cenWpt.toString();
        const polyStr = convertWpAryToStr(corners, plot);
        setPolygon(polyStr);
        dispatchAttributeChange({plotId, changes: {[PlotAttribute.POLYGON_ARY]: convertStrToWpAry(polyStr)}});
        setTimeout(doModalEnd, 10);
    } else {
        const polyWpAry = plot.attributes[PlotAttribute.POLYGON_ARY];
        wpStr = plot.attributes[PlotAttribute.USER_SEARCH_WP];
        if (polyWpAry?.length) {
            if (isWpArysEquals(polyWpAry, userEnterPolygon())) return;
            setPolygon(convertWpAryToStr(polyWpAry, plot));
            doModalEnd();
        }
    }
    if (wpStr && wpStr !== getTargetWp()) setTargetWp(wpStr);
}

export function updateUIFromPlot({plotId, setWhichOverlay, whichOverlay, setTargetWp, getTargetWp, canUpdateModalEndInfo=true,
                                     setHiPSRadius, getHiPSRadius, setPolygon, getPolygon, getBoxParams, setBoxParams, minSize, maxSize }) {

    const userEnterWorldPt = () => parseWorldPt(getTargetWp());
    const userEnterSearchRadius = () => Number(getHiPSRadius());
    const userEnterPolygon = () => convertStrToWpAry(getPolygon());

    if (![CONE_CHOICE_KEY, POLY_CHOICE_KEY, BOX_CHOICE_KEY].includes(whichOverlay)) return;
    const plot = primePlot(visRoot(), plotId);
    if (!plot) return;
    let isCone = whichOverlay === CONE_CHOICE_KEY;
    const isBox = whichOverlay === BOX_CHOICE_KEY;
    const {cenWpt, radius, corners, rectSizeX, rectSizeY, rectRotAxisWpt, rectRotAngle} =
        plot.attributes[PlotAttribute.SELECTION] ? getDetailsFromSelection(plot) : {};

    const plotSelType= plot.attributes[PlotAttribute.SELECTION_TYPE];
    if (setWhichOverlay && plotSelType) {
        isCone= plotSelType!==SelectedShape.rect.key; // in future, if something not supported just default to cone
        if (plot.attributes[PlotAttribute.SELECTION_SOURCE]!==SEARCH_SELECT_TOOL_SOURCE) {
            setWhichOverlay(isCone ? CONE_CHOICE_KEY : (plot.attributes[PlotAttribute.USE_BOX]
                ? BOX_CHOICE_KEY : POLY_CHOICE_KEY));
        }
    }
    // SelectArea Drawing Layer is drawn on the plot
    const isSelectAreaDLOnPlot = plot.attributes[PlotAttribute.SELECTION] && plot.attributes[PlotAttribute.SELECTION_SOURCE]===SelectArea.TYPE_ID;

    const setWpAndDispatch = (wp) => {
        setTargetWp(wp.toString());
        dispatchActiveTarget(wp);
    };
    const doModalEnd = () => canUpdateModalEndInfo ? updateModalEndInfo(plot.plotId) : closeToolbarModalLayers();

    const handlerCtx = {plot, plotId, isSelectAreaDLOnPlot, cenWpt, setTargetWp, setWpAndDispatch, doModalEnd, userEnterWorldPt};
    if (isCone) {
        handleConeUIFromPlot({
            ...handlerCtx,
            radius, userEnterSearchRadius, setHiPSRadius, getHiPSRadius, minSize, maxSize
        });
    } else if (isBox) {
        handleBoxUIFromPlot({
            ...handlerCtx,
            rectSizeX, rectSizeY, rectRotAngle, rectRotAxisWpt, getBoxParams, setBoxParams, minSize, maxSize
        });
    } else {
        handlePolygonUIFromPlot({
            ...handlerCtx,
            corners, getTargetWp, userEnterPolygon, setPolygon
        });
    }
}

function setUISize(size, minSize, maxSize, uiCurrentValue, setter) {
    if (isUndefined(size) || isNaN(size)) return;
    const sizeToSet = clampInRange(size, minSize, maxSize);
    if (uiCurrentValue===sizeToSet) return;
    setter(sizeToSet+'');
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
        [PlotAttribute.SELECTION_SOURCE]: SEARCH_SELECT_TOOL_SOURCE,
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
        [PlotAttribute.SELECTION_SOURCE]: SEARCH_SELECT_TOOL_SOURCE,
    };
    
}

function convertBoxToSelection(plot, wp, boxParams) {
    if (!plot || !wp || !boxParams?.sizeX || !boxParams?.sizeY || !(boxParams?.rotation || boxParams?.rotation===0)) return {};

    const halfSizeXArcsec = boxParams.sizeX / 2 * 3600;
    const halfSizeYArcsec = boxParams.sizeY / 2 * 3600;
    const rotAxisOffset = {x: 0, y: halfSizeYArcsec}; // since the rotation axis is along the Y axis of box
    const boxCornerOffsets = [
        {x: +halfSizeXArcsec, y: +halfSizeYArcsec}, // upperRight
        {x: -halfSizeXArcsec, y: +halfSizeYArcsec}, // upperLeft
        {x: -halfSizeXArcsec, y: -halfSizeYArcsec}, // lowerLeft
        {x: +halfSizeXArcsec, y: -halfSizeYArcsec}, // lowerRight
    ];

    // wp is box center (0, 0) but Y and X axes of the box are not always aligned with North and East. So we can't do
    // simple arithmetic to find the corners from above offsets and instead use the following function which takes
    // (E of N) rotation of the local plane into account when applying offsets to find new positon.
    const wpAry = boxCornerOffsets.map((offset) =>
        calculatePositionFromLocalOffsets(wp, offset.x, offset.y, boxParams.rotation));
    const rotAxisWp = calculatePositionFromLocalOffsets(wp, rotAxisOffset.x, rotAxisOffset.y, boxParams.rotation);

    if (wpAry.some((pt) => !isValidPoint(pt))) return {};

    const sel = {
        pt0: wpAry[1], // upperLeft
        pt1: wpAry[3], // lowerRight
    };
    const imBoundSel = getImageBoundsSelection(sel, CsysConverter.make(plot), SelectedShape.rect.key,
        getPlotViewById(visRoot(), plot.plotId)?.rotation ?? 0);

    return {
        [PlotAttribute.POLYGON_ARY]: wpAry,
        [PlotAttribute.USER_SEARCH_BOX_AXIS_WP]: rotAxisWp,
        [PlotAttribute.SELECTION]: sel,
        [PlotAttribute.SELECTION_TYPE]: SelectedShape.rect.key,
        [PlotAttribute.IMAGE_BOUNDS_SELECTION]: imBoundSel,
        [PlotAttribute.SELECTION_SOURCE]: SEARCH_SELECT_TOOL_SOURCE,
    };
}


function convertToSelection(plot, wp, radius, polygonAry, whichOverlay, boxParams) {
    if (!plot) return {};
    if (whichOverlay === CONE_CHOICE_KEY) return convertConeToSelection(plot, wp, radius);
    if (whichOverlay === BOX_CHOICE_KEY) return convertBoxToSelection(plot, wp, boxParams);
    return convertPolygonToSelection(plot, polygonAry);
}

export function updatePlotOverlayFromUserInput({
    plotId, whichOverlay, wp, radius, polygonAry, boxParams, forceCenterOn = false, canGeneratePolygon = false
}) {
    const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
    if (!dl) return; // since the following changes are only made for SearchSelect drawing layer on plot

    const isCone = whichOverlay === CONE_CHOICE_KEY;
    const isBox = whichOverlay === BOX_CHOICE_KEY;
    const isPolygon = whichOverlay === POLY_CHOICE_KEY;
    const plot= primePlot(visRoot(),plotId);

    // update SearchSelect drawing layer's attributes in the store
    dispatchChangeDrawingDef(dl.drawLayerId,{...dl.drawingDef,color:'yellow'},plotId);
    dispatchModifyCustomField(dl.drawLayerId,{isInteractive: true},plotId);

    if (isPolygon && wp && radius && canGeneratePolygon) {
        // convert cone to a 8-pointed polygon that circumscribes it
        const cc= CsysConverter.make(plot);
        const cen= cc.getDeviceCoords(wp); // center of cone
        const ptOnCone= cc.getDeviceCoords(calculatePosition( convertCelestial(wp),radius*3600,radius*3600));
        const dist= Math.abs(cen.y-ptOnCone.y)*2; // diameter of cone
        polygonAry= getCircumscribedCorners(cc,cen,dist,dist); // rx=ry for circle, so dist is used for both
    }

    const changes= {
        [PlotAttribute.USER_SEARCH_WP]: wp,
        [PlotAttribute.USER_SEARCH_RADIUS_DEG]: isCone ? radius : undefined,
        [PlotAttribute.USER_SEARCH_BOX]: isBox && boxParams ? {...boxParams, rotation: normalizeRotation(boxParams?.rotation)} : undefined,
        [PlotAttribute.POLYGON_ARY]: isPolygon ? polygonAry : undefined,
        [PlotAttribute.RELATIVE_IMAGE_POLYGON_ARY]:
            isPolygon ? makeRelativePolygonAry(primePlot(visRoot(), plotId), polygonAry) : undefined,
        [PlotAttribute.USE_BOX]: isBox,
        [PlotAttribute.USE_POLYGON]: isPolygon,
        // add SearchSelectTool DL related plot attributes from the user input
        ...(whichOverlay ? convertToSelection(plot, wp, radius, polygonAry, whichOverlay, boxParams) : {})
    };
    dispatchAttributeChange({ plotId, changes });

    // force redrawing the SearchSelect drawing layer now that plot attributes have been updated
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);
    if (!plot || isImage(plot)) return;
    if (isPolygon && !polygonAry) return;
    if (isBox && !boxParams?.sizeX && !boxParams?.sizeY) return;

    const centerProjPt = isPolygon ? computeCentralPointAndRadius(polygonAry)?.centralPoint : wp;
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

    updatePlotOverlayFromUserInput({plotId, whichOverlay: isCone ? CONE_CHOICE_KEY : POLY_CHOICE_KEY, wp, radius, polygonAry});
    dispatchModifyCustomField(dl.drawLayerId, {isInteractive: false}, plotId);
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);
}
