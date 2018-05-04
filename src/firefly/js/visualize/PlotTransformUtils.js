/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isNil} from 'lodash';
import {Matrix} from 'transformation-matrix-js';
import {primePlot} from './PlotViewUtil.js';
import {clone, updateSet} from '../util/WebUtil.js';
import {getCenterOfProjection, getFoV} from './PlotViewUtil.js';

import {toRadians} from './VisUtil.js';
import {CysConverter}  from './CsysConverter.js';
import {makeScreenPt, makeDevicePt} from './Point.js';
import {isImage} from './WebPlot.js';

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
    if (isNil(scrollX) || isNil(scrollY)) return undefined;
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
 * Call the function when drag is beginning.  It returns a function that is call on each drag event.
 * call the returned function with each next screen location it returns the new scroll point.
 * @param originalDeviceX
 * @param originalDeviceY
 * @param {ScreenPt} originalScrollPt
 * @param mouseDownScreenPt
 * @param {PlotView} startingPlotView
 * @return {function({number}, {number})}
 */
export function plotMover(originalDeviceX ,originalDeviceY , originalScrollPt, mouseDownScreenPt, startingPlotView) {

    const startingPlot= primePlot(startingPlotView);
    const cc= CysConverter.make(startingPlot);
    const originalScreenPt= cc.getScreenCoords(makeDevicePt(originalDeviceX ,originalDeviceY));
    const startWp= cc.getWorldCoords(mouseDownScreenPt, startingPlot.imageCoordSys);
    const xDir= startingPlotView.flipY ? -1 : 1;
    const yDir= startingPlotView.flipX ? -1 : 1;
    let xdiff, ydiff;
    let lastDevPt= makeDevicePt(originalDeviceX ,originalDeviceY);
    let lastWorldPt= cc.getWorldCoords(lastDevPt,startingPlot.imageCoordSys);



    if (isImage(startingPlot)) {
        return (screenX, screenY) => {
            const newDevPt = makeDevicePt(screenX, screenY);
            const newScreenPt = cc.getScreenCoords(newDevPt);
            xdiff = (newScreenPt.x - originalScreenPt.x) * xDir;
            ydiff = (newScreenPt.y - originalScreenPt.y) * yDir;

            const newScrX = originalScrollPt.x - xdiff;
            const newScrY = originalScrollPt.y - ydiff;

            return makeScreenPt(newScrX, newScrY);
        };
    }
    else {
        return (screenX, screenY, pv) => {
            const newDevPt= makeDevicePt(screenX,screenY);
            const plot= primePlot(startingPlotView);
            if (!startWp) return null;

            xdiff= (newDevPt.x- lastDevPt.x);
            ydiff= (newDevPt.y- lastDevPt.y);

            const actionPlot= primePlot(pv);
            const activeCC= CysConverter.make(actionPlot);
            const centerOfProj= getCenterOfProjection(actionPlot);
            const originalCenterOfProjDev= activeCC.getDeviceCoords(centerOfProj);

            if (!originalCenterOfProjDev) {
                // console.log('originalCenterOfProjScreen null');
                return null;
            }


            if (lastWorldPt && Math.abs(lastWorldPt.y)>88 && getFoV(pv)>30) {
                // xdiff/=8;
                ydiff/=8;
                xdiff/=8;
            }

            

            const newCenterOfProjDev= makeDevicePt(originalCenterOfProjDev.x-xdiff, originalCenterOfProjDev.y-ydiff);
            const newWp= activeCC.getWorldCoords(newCenterOfProjDev,plot.imageCoordSys);

            if (!newWp) return null;


            if (newWp.y < -89.7) newWp.y= -89.7;
            if (newWp.y >  89.7) newWp.y=  89.7;

            lastDevPt= newDevPt;
            lastWorldPt= newWp;

            return newWp;

        };
    }
}

