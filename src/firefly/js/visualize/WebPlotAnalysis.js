import pointInPolygon from 'point-in-polygon';
import {SelectedShape} from '../drawingLayers/SelectedShape';
import CoordinateSys from './CoordSys';
import CysConverter, {CCUtil} from './CsysConverter';
import DrawOp from './draw/DrawOp.js';
import {makeDevicePt, makeImagePt, makeImageWorkSpacePt, makeWorldPt} from './Point';
import {
    contains, containsEllipse, containsRec, findIntersectionPt, getAngleInDeg, getBoundingBox, getPositionAngle
} from './VisUtil';
import {getPixScaleDeg} from './WebPlot';


export function isPlotRotatedNorth(plot, csys = CoordinateSys.EQ_J2000) {
    if (!plot) return false;
    const cc = CysConverter.make(plot);
    const wpt1 = cc.getWorldCoords(makeImageWorkSpacePt(plot.dataWidth / 2, plot.dataHeight / 2), csys);
    if (!wpt1) return false;
    const cdelt1 = getPixScaleDeg(plot);
    const wpt2 = makeWorldPt(wpt1.getLon(), wpt1.getLat() + (Math.abs(cdelt1) / plot.zoomFactor) * (5), csys);
    if (!wpt2) return false;
    const dpt1 = cc.getDeviceCoords(wpt1);
    const dpt2 = cc.getDeviceCoords(wpt2);
    return Boolean(dpt1 && dpt2 && (Math.abs(dpt1.x - dpt2.x) < .9) && dpt1.y > dpt2.y);
}

/**
 * Is the image positioned so that north is up.
 * @param {WebPlot|undefined} plot
 * @param {CoordinateSys} csys
 * @return {boolean}
 */
export function isPlotNorth(plot, csys = CoordinateSys.EQ_J2000) {
    if (!plot) return false;
    const ix = plot.dataWidth / 2;
    const iy = plot.dataHeight / 2;
    const cc = CysConverter.make(plot);
    const wpt1 = cc.getWorldCoords(makeImageWorkSpacePt(ix, iy), csys);
    if (!wpt1) return false;
    const cdelt1 = getPixScaleDeg(plot);
    const wpt2 = makeWorldPt(wpt1.getLon(), wpt1.getLat() + (Math.abs(cdelt1) / plot.zoomFactor) * (5), csys);
    const spt1 = cc.getScreenCoords(wpt1);
    const spt2 = cc.getScreenCoords(wpt2);
    if (spt1 && spt2) return (spt1.x === spt2.x && spt1.y > spt2.y);
    return false;
}

/**
 * get the world point at the center of the plot
 * @param {WebPlot} plot
 * @return {WorldPt}
 */
export function getCenterPtOfPlot(plot) {
    if (!plot) return undefined;
    const ip = makeImagePt(plot.dataWidth / 2, plot.dataHeight / 2);
    return CCUtil.getWorldCoords(plot, ip);
}

export const getRotationAngle = (plot) => {
    const iWidth = plot.dataWidth;
    const iHeight = plot.dataHeight;
    const ix = iWidth / 2;
    const iy = iHeight / 2;
    const cc = CysConverter.make(plot);
    const wptC = cc.getWorldCoords(makeImageWorkSpacePt(ix, iy));
    const wpt2 = cc.getWorldCoords(makeImageWorkSpacePt(ix, iy + iHeight / 4));
    if (wptC && wpt2) return getPositionAngle(wptC.getLon(), wptC.getLat(), wpt2.getLon(), wpt2.getLat());
    return 0;
};

/**
 * Return true if east if left of north.  If east is right of north return false. This works regardless of the rotation
 * of the image.
 * @param {WebPlot|undefined} plot
 * @return {boolean} true if is is left of north.
 */
export function isEastLeftOfNorth(plot) {
    if (!plot?.projection) return true;
    if (!plot.projection.isSpecified() || !plot.projection.isImplemented() || !plot.projection?.coordSys?.isCelestial()) return true;

    const mx = plot.dataWidth / 2;
    const my = plot.dataHeight / 2;


    const worldOffset = plot.projection.getPixelScaleDegree() * 10;

    const cc = CysConverter.make(plot);
    const wptC = cc.getWorldCoords(makeImageWorkSpacePt(mx, my));
    if (!wptC) return true;
    const wptNorth = makeWorldPt(wptC.x, wptC.y + worldOffset);
    const wptE = makeWorldPt(wptC.x + worldOffset, wptC.y);
    if (!wptE) return true;

    const impNorth = cc.getImageCoords(wptNorth);
    const impE = cc.getImageCoords(wptE);


    const angleN = getAngleInDeg(mx, my, impNorth.x, impNorth.y);
    const angleE = getAngleInDeg(mx, my, impE.x, impE.y);

    return ((angleE - angleN) + 360) % 360 < 180;
}

/**
 *
 * @param {WebPlot|undefined} p1
 * @param {WebPlot|undefined} p2
 * @return {boolean}
 */
export const isCsysDirMatching = (p1, p2) => isEastLeftOfNorth(p1) === isEastLeftOfNorth(p2);


export function getMatchingPlotRotationAngle(masterPlot, plot, masterRotationAngle, isMasterFlipY) {
    if (!plot || !masterPlot) return 0;
    const masterRot = masterRotationAngle * (isMasterFlipY ? -1 : 1);
    const rot = getRotationAngle(plot);
    let targetRotation;
    if (isEastLeftOfNorth(masterPlot)) {
        targetRotation = ((getRotationAngle(masterPlot) + masterRot) - rot) * (isMasterFlipY ? 1 : -1);
    } else {
        targetRotation = ((getRotationAngle(masterPlot) + (360 - masterRot)) - rot) * (isMasterFlipY ? 1 : -1);

    }
    if (!isCsysDirMatching(plot, masterPlot)) targetRotation = 360 - targetRotation;
    if (targetRotation < 0) targetRotation += 360;
    if (targetRotation > 359) targetRotation %= 360;
    return targetRotation;
}



/**
 * @param {WebPlot|undefined} plot
 * @return {{x: number, y: number, w: number, h: number}|undefined}
 */
export function computeBoundingBoxInDeviceCoordsForPlot(plot) {
    if (!plot) return;
    const {dataWidth: w, dataHeight: h} = plot;
    const cc = CysConverter.make(plot);
    return getBoundingBox([
        cc.getDeviceCoords(makeImagePt(0, 0)),
        cc.getDeviceCoords(makeImagePt(w, 0)),
        cc.getDeviceCoords(makeImagePt(w, h)),
        cc.getDeviceCoords(makeImagePt(0, h))
    ]);
}

export function isFullyOnScreen(plot, viewDim) {
    const box = computeBoundingBoxInDeviceCoordsForPlot(plot);
    return Boolean(box) && containsRec(0, 0, viewDim.width + 3, viewDim.height + 3, box.x, box.y, box.w, box.h);
}

/**
 * return true if the image is completely covering the area passed. The width and height are in Device coordinate
 * system.
 * @param {WebPlot} plot
 * @param {Point} pt
 * @param {number} width in device coordinates
 * @param {number} height in device coordinates
 * @return {boolean} true if covering
 */
export function isImageCoveringArea(plot, pt, width, height) {
    if (!pt) return false;
    const cc = CysConverter.make(plot);
    pt = cc.getDeviceCoords(pt);
    const testPts = [
        makeDevicePt(pt.x, pt.y),
        makeDevicePt(pt.x + width, pt.y),
        makeDevicePt(pt.x + width, pt.y + height),
        makeDevicePt(pt.x, pt.y + height),
    ];

    const polyPts = [
        cc.getDeviceCoords(makeImagePt(1, 1)),
        cc.getDeviceCoords(makeImagePt(plot.dataWidth, 1)),
        cc.getDeviceCoords(makeImagePt(plot.dataWidth, plot.dataHeight)),
        cc.getDeviceCoords(makeImagePt(1, plot.dataHeight))
    ];


    const polyPtsAsArray = polyPts.map((p) => [p.x, p.y]);

    return testPts.every((p) => pointInPolygon([p.x, p.y], polyPtsAsArray));
}

/**
 * find a point on the plot that is top and left but is still in view and on the image.
 * If the image is off the screen the return undefined.
 * @param {WebPlot} plot
 * @param {object} viewDim
 * @param {number} xOff
 * @param {number} yOff
 * @return {DevicePt} the found point
 */
export function getTopmostVisiblePoint(plot, viewDim, xOff, yOff) {
    const cc = CysConverter.make(plot);
    const ipt = cc.getImageCoords(makeDevicePt(xOff, yOff));
    if (isImageCoveringArea(plot, ipt, 2, 2)) return ipt;


    const {dataWidth, dataHeight} = plot;

    const lineSegs = [
        {pt1: cc.getDeviceCoords(makeImagePt(0, 0)), pt2: cc.getDeviceCoords(makeImagePt(dataWidth, 0))},
        {
            pt1: cc.getDeviceCoords(makeImagePt(dataWidth, 0)),
            pt2: cc.getDeviceCoords(makeImagePt(dataWidth, dataHeight))
        },
        {
            pt1: cc.getDeviceCoords(makeImagePt(dataWidth, dataHeight)),
            pt2: cc.getDeviceCoords(makeImagePt(0, dataHeight))
        },
        {pt1: cc.getDeviceCoords(makeImagePt(0, dataHeight)), pt2: cc.getDeviceCoords(makeImagePt(0, 0))}
    ];

    const foundSegs = lineSegs
        .filter((lineSeg) => {
            const {pt1, pt2} = lineSeg;
            if (!pt1 || !pt2) return false;
            const iPt = findIntersectionPt(pt1.x, pt1.y, pt2.x, pt2.y, 0, 0, viewDim.width - 1, 0);
            return Boolean(iPt && iPt.onSeg1 && iPt.onSeg2);
        })
        .sort((l1, l2) => l1.pt1.x - l2.pt1.x);

    if (foundSegs[0]) {
        const pt = findIntersectionPt(foundSegs[0].pt1.x, foundSegs[0].pt1.y,
            foundSegs[0].pt2.x, foundSegs[0].pt2.y, 0, 0, viewDim.width - 1, 0);
        return makeDevicePt(pt.x + xOff, pt.y + yOff);
    }

    const zXoff = xOff / plot.zoomFactor;
    const zYoff = xOff / plot.zoomFactor;

    const tryPts = [
        makeImagePt(1 + zXoff, 1 + zXoff),
        makeImagePt(plot.dataWidth - zXoff, 1 + zYoff),
        makeImagePt(plot.dataWidth - zXoff, plot.dataHeight - zYoff),
        makeImagePt(1 + zXoff, plot.dataHeight - zYoff),
    ];


    const highPts = tryPts
        .map((p) => cc.getDeviceCoords(p))
        .filter((p) => cc.pointOnDisplay(p))
        .sort((p1, p2) => p1.y !== p2.y ? p1.y - p2.y : p1.x - p2.x);

    return highPts[0];
}

/**
 * @param {object} selection obj with two properties pt0 & pt1
 * @param {WebPlot} plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @param selectedShape shape of selected area
 * @return {Array} indexes from the objList array that are selected
 */
export function getSelectedPts(selection, plot, objList, selectedShape) {

    if (selectedShape === SelectedShape.circle.key) {
        return getSelectedPtsFromEllipse(selection, plot, objList);
    } else {
        return getSelectedPtsFromRect(selection, plot, objList);
    }
}

/**
 * get selected points from circular selected area
 * @param selection
 * @param plot
 * @param objList
 * @returns {Array}
 */
function getSelectedPtsFromEllipse(selection, plot, objList) {
    const selectedList = [];
    if (selection && plot && objList && objList.length) {
        const cc = CysConverter.make(plot);
        const pt0 = cc.getDeviceCoords(selection.pt0);
        const pt1 = cc.getDeviceCoords(selection.pt1);
        if (!pt0 || !pt1) return selectedList;

        const c_x = (pt0.x + pt1.x) / 2;
        const c_y = (pt0.y + pt1.y) / 2;
        const r1 = Math.abs(pt0.x - pt1.x) / 2;
        const r2 = Math.abs(pt0.y - pt1.y) / 2;

        objList.forEach((obj, idx) => {
            const testObj = cc.getDeviceCoords(DrawOp.getCenterPt(obj));

            if (testObj && containsEllipse(testObj.x, testObj.y, c_x, c_y, r1, r2)) {
                selectedList.push(idx);
            }
        });
    }
    return selectedList;

}

/**
 * get selected points from rectangular selected area
 * @param {object} selection obj with two properties pt0 & pt1
 * @param {WebPlot} plot web plot
 * @param objList array of DrawObj (must be an array and contain a getCenterPt() method)
 * @return {Array} indexes from the objList array that are selected
 */
function getSelectedPtsFromRect(selection, plot, objList) {
    const selectedList = [];
    if (selection && plot && objList && objList.length) {
        const cc = CysConverter.make(plot);
        const pt0 = cc.getDeviceCoords(selection.pt0);
        const pt1 = cc.getDeviceCoords(selection.pt1);
        if (!pt0 || !pt1) return selectedList;

        const x = Math.min(pt0.x, pt1.x);
        const y = Math.min(pt0.y, pt1.y);
        const width = Math.abs(pt0.x - pt1.x);
        const height = Math.abs(pt0.y - pt1.y);
        objList.forEach((obj, idx) => {
            const testObj = cc.getDeviceCoords(DrawOp.getCenterPt(obj));
            if (testObj && contains(x, y, width, height, testObj.x, testObj.y)) {
                selectedList.push(idx);
            }
        });
    }
    return selectedList;
}