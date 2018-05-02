


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {primePlot} from './PlotViewUtil.js';
import {visRoot} from './ImagePlotCntlr.js';
import {CysConverter} from './CsysConverter.js';
import {isHiPS} from './WebPlot.js';
import {getHealpixCornerTool} from './HiPSUtil.js';
import {getHealpixPixel} from './HiPSUtil';

export const MouseState= new Enum(['NONE', 'ENTER', 'EXIT', 'DOWN', 'UP',
    'DRAG_COMPONENT', 'DRAG', 'MOVE', 'CLICK',
    'DOUBLE_CLICK']);

var lastCtx = {
    mouseState : MouseState.NONE,
    plotId : null,
    screenPt : null,
    imagePt : null,
    worldPt : null,
    modifiers: {}
};

var listenerList= [];
var oneTimeCallList= [];


export function lastMouseCtx() { return lastCtx; }

export function addMouseListener(l) {
    listenerList.push(l);
    return () => {
        const idx= listenerList.indexOf(l);
        if (idx>-1) listenerList.splice(idx,1);
    };
}


export function addOneTimeMouseCall(l) { oneTimeCallList.push(l); }

export function mouseUpdatePromise() {
    return new Promise((resolve) => addOneTimeMouseCall( (s) => resolve(s)) );
}

export function fireMouseCtxChange(mouseCtx) {
    lastCtx= mouseCtx;
    listenerList.forEach((l) => l(mouseCtx));
    oneTimeCallList.forEach((l) => l(mouseCtx));
    oneTimeCallList=[];
}



/**
 *
 * @param {string} plotId
 * @param {Enum} mouseState
 * @param {Object} screenPt
 * @param {number} screenX
 * @param {number} screenY
 * @param shiftDown
 * @param controlDown
 * @param metaDown
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
