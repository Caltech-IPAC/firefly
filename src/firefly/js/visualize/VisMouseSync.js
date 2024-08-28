


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {primePlot} from './PlotViewUtil.js';
import {visRoot} from './ImagePlotCntlr.js';
import {CysConverter} from './CsysConverter.js';
import {isHiPS} from './WebPlot.js';
import {getHealpixPixel} from './HiPSUtil';
import {STANDARD_READOUT} from './MouseReadoutCntlr';

/**
 * @typedef MouseState
 * @prop NONE,
 * @prop ENTER,
 * @prop EXIT,
 * @prop DOWN,
 * @prop UP,
 * @prop DRAG_COMPONENT,
 * @prop DRAG,
 * @prop MOVE,
 * @prop CLICK,
 * @prop WHEEL_UP,
 * @prop WHEEL_DOWN,
 * @prop DOUBLE_CLICK,
 * @type {Enum}
 */
/** @type MouseState */
export const MouseState= new Enum(['NONE', 'ENTER', 'EXIT', 'DOWN', 'UP',
    'DRAG_COMPONENT', 'DRAG', 'MOVE', 'CLICK', 'WHEEL_UP', 'WHEEL_DOWN',
    'DOUBLE_CLICK']);

var lastCtx = {
    mouseState : MouseState.NONE,
    plotId : null,
    screenPt : null,
    imagePt : null,
    worldPt : null,
    modifiers: {}
};

var lastReadout= {};

var imageMouseListenerList= [];
var imageReadoutListenerList= [];
var oneTimeCallList= [];


export function lastMouseCtx() { return lastCtx; }
export function lastMouseImageReadout() { return lastReadout; }

export function addImageMouseListener(l) {
    imageMouseListenerList.push(l);
    return () => {
        const idx= imageMouseListenerList.indexOf(l);
        if (idx>-1) imageMouseListenerList.splice(idx,1);
    };
}

export function addImageReadoutUpdateListener(l) {
    imageReadoutListenerList.push(l);
    return () => {
        const idx= imageReadoutListenerList.indexOf(l);
        if (idx>-1) imageReadoutListenerList.splice(idx,1);
    };
}


export function addOneTimeMouseCall(l) { oneTimeCallList.push(l); }

export function mouseUpdatePromise() {
    return new Promise((resolve) => addOneTimeMouseCall( (s) => resolve(s)) );
}

export function fireMouseCtxChange(mouseCtx) {
    lastCtx= mouseCtx;
    imageMouseListenerList.forEach((l) => l(mouseCtx));
    oneTimeCallList.forEach((l) => l(mouseCtx));
    oneTimeCallList=[];
}

/**
 *
 * @param readoutData
 * @param {string} readoutData.readoutType
 * @param {string} readoutData.plotId
 * @param {Object} readoutData.readoutItems
 * @param {boolean} [readoutData.threeColor]
 */
export function fireMouseReadoutChange({readoutType= STANDARD_READOUT, plotId, readoutItems={}, threeColor=false}) {
    lastReadout=  {readoutType,plotId,readoutItems,threeColor};
    imageReadoutListenerList.forEach((l) => l(lastReadout));
}


/**
 *
 * @param {string} plotId
 * @param {Enum} mouseState
 * @param {Object} screenPt
 * @param {number} screenX
 * @param {number} screenY
 * @param {Object} kState
 * @param kState.shiftDown
 * @param kState.controlDown
 * @param kState.metaDown
 * @return {{plotId: string, mouseState: Enum, screenPt: object, imagePt: object, worldPt: object, screenX: number, screenY: number}}
 */
export function makeMouseStatePayload(plotId,mouseState,screenPt,screenX,screenY,
                               {shiftDown,controlDown,metaDown}= {}) {
    const payload={mouseState,screenPt,screenX,screenY, shiftDown,controlDown,metaDown};
    const plot= primePlot(visRoot(),plotId);
    const cc= CysConverter.make(plot);
    if (!plotId || !plot) return payload;
    payload.plotId= plotId;
    payload.imagePt= cc.getImageCoords(screenPt);
    const worldPt= cc.getWorldCoords(screenPt);
    payload.devicePt= cc.getDeviceCoords(screenPt);
    payload.worldPt= worldPt;
    if (isHiPS(plot) && worldPt) {
        const result= getHealpixPixel(plot,worldPt);
        if (result) {
            payload.healpixPixel= result.pixel;
            payload.norder= result.norder;
        }
    }
    return payload;
}
