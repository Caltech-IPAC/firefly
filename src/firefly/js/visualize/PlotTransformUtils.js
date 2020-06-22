/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isNil} from 'lodash';
import {Matrix} from '../externalSource/transformation-matrix-js/matrix';
import {primePlot} from './PlotViewUtil.js';
import {memorizeLastCall, updateSet} from '../util/WebUtil.js';
import {toRadians} from './VisUtil.js';

/**
 * should be call if the viewport changes, scroll changes, rotation changes, viewDim (offsetWidth, offsetHeight) changes
 * @param {PlotView} pv
 * @return {PlotView}
 */
export function updateTransform(pv) {
    const plot= primePlot(pv);
    if (!plot) return pv;
    const {scrollX, scrollY, rotation, viewDim}= pv;
    if (isNil(scrollX) || isNil(scrollY)) {
        return pv;
    }
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
    if (isNil(scrollX) || isNil(scrollY) || !viewDim) return undefined;
    return makeTransformImpl(offsetX,offsetY,scrollX,scrollY, rotation, flipX, flipY,viewDim.width,viewDim.height);
}

const makeTransformImpl= memorizeLastCall( (offsetX,offsetY,scrollX,scrollY, rotation, flipX, flipY, w,h) =>{
    const left= offsetX-scrollX;
    const top= offsetY-scrollY;

    const affTrans= new Matrix();
    affTrans.translate(w/2,h/2);
    affTrans.rotate(toRadians(rotation));
    affTrans.translate(w/-2+left, h/-2+top);
    if (flipY)  {
        affTrans.translateX(w);
        affTrans.scale(-1,1);
    }
    return affTrans;
},10);





export function makeThumbnailTransformCSS(rotation, flipX, flipY) {
    let transFormCss= '';
    if (rotation) transFormCss+= `rotate(${rotation}deg) `;
    if (flipY) transFormCss+= 'scaleX(-1) ';
    return transFormCss;
}

function insertTransform(inPlotView, affTrans) {
    const plot= primePlot(inPlotView);
    const pv= {...inPlotView};
    const {plots}= pv;
    pv.affTrans=  {...affTrans};
    pv.plots= plots.map( (p) => p===plot ? {...plot,affTrans:pv.affTrans, viewDim:pv.viewDim} : p);
    pv.overlayPlotViews= pv.overlayPlotViews.map( (opv) =>
                                       opv?.plot ? updateSet(opv, 'plot.affTrans', pv.affTrans) :opv);
    return pv;
}


