/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {Matrix} from 'transformation-matrix-js';
import {primePlot} from './PlotViewUtil.js';
import {clone, updateSet} from '../util/WebUtil.js';

import {toRadians} from './VisUtil.js';
import {CysConverter}  from './CsysConverter.js';
import {makeScreenPt, makeDevicePt} from './Point.js';

/**
 *
 * @param {PlotView} pvOrPlot
 */
export function getTransform(pvOrPlot) {
    return Matrix.from(pvOrPlot.affTrans);
}

/**
 *
 * @param {PlotView} pv
 */
export const getInverseTransform= (pv) => getTransform(pv).inverse();


/**
 * should be call if the viewport changes, scroll changes, rotation changes, viewDim (offsetWidth, offsetHeight) changes
 * @param {PlotView} pv
 * @return {PlotView}
 */
export function updateTransform(pv) {
    const plot= primePlot(pv);
    if (!plot) return pv;
    const {scrollX, scrollY, rotation, viewDim}= pv;
    const affTrans= makeTransform(0,0, scrollX, scrollY, rotation, pv.flipX, pv.flipY, viewDim);

    return insertTransform(pv, affTrans, viewDim);
}

/**
 *
 * @param offsetX
 * @param offsetY
 * @param scrollX
 * @param scrollY
 * @param rotation
 * @param flipX
 * @param flipY
 * @param viewDim
 * @return {Matrix}
 */
export function makeTransform(offsetX,offsetY,scrollX,scrollY, rotation, flipX, flipY, viewDim) {
    const left= offsetX-scrollX;
    const top= offsetY-scrollY;
    const {width:w, height:h}= viewDim;

    const affTrans= new Matrix();
    affTrans.translate(w/2,h/2);
    affTrans.rotate(toRadians(rotation));
    affTrans.translate(w/-2+left, h/-2+top);
    if (flipY)  {
        affTrans.translateX(w);
        affTrans.scale(-1,1);
    }
    return affTrans;
}

export function makeThumbnailTransformCSS(rotation, flipX, flipY) {
    let transFormCss= '';
    if (rotation) transFormCss+= `rotate(${rotation}deg) `;
    if (flipY) transFormCss+= 'scaleX(-1) ';
    return transFormCss;
}

function insertTransform(inPlotView, affTrans) {
    const pv= clone(inPlotView);
    const {plots}= pv;
    const plot= primePlot(pv);
    pv.affTrans=  clone(affTrans);
    pv.plots= plots.map( (p) => p===plot ? clone(plot,{affTrans:pv.affTrans, viewDim:pv.viewDim}) : p);
    pv.overlayPlotViews= pv.overlayPlotViews.map( (opv) =>
                                       get(opv,'plot') ? updateSet(opv, 'plot.affTrans', pv.affTrans) :opv);
    return pv;
}


/**
 *
 * @param originalDeviceX
 * @param originalDeviceY
 * @param {ScreenPt} originalScrollPt
 * @param {PlotView} plotView
 * @return {function(*, *)}
 */
export function plotMover(originalDeviceX ,originalDeviceY , originalScrollPt,plotView) {

    const cc= CysConverter.make(primePlot(plotView));
    const originalScreenPt= cc.getScreenCoords(makeDevicePt(originalDeviceX ,originalDeviceY));
    const xDir= plotView.flipY ? -1 : 1;
    const yDir= plotView.flipX ? -1 : 1;

    return (screenX, screenY) => {

        const newScreenPt= cc.getScreenCoords(makeDevicePt(screenX,screenY));

        const xdiff= (newScreenPt.x- originalScreenPt.x) * xDir;
        const ydiff= (newScreenPt.y- originalScreenPt.y) * yDir;

        const newScrX= originalScrollPt.x -xdiff;
        const newScrY= originalScrollPt.y -ydiff;

        return makeScreenPt(newScrX,newScrY);
    };
}










