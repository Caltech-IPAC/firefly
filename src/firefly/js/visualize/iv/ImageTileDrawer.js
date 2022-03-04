/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNumber} from 'lodash';
import {initOffScreenCanvas, computeBounding} from './TileDrawHelper.jsx';
import {makeTransform} from '../PlotTransformUtils.js';
import {primePlot, hasLocalStretchByteData} from '../PlotViewUtil.js';
import {isImage} from '../WebPlot.js';
import {drawScreenTileToMainCanvas} from 'firefly/visualize/rawData/RawTileDrawer.js';

/**
 * Return a function that should be called on every render to draw the image
 * @param {HTMLCanvasElement} targetCanvas
 * @return {*}
 */
export function initImageDrawer(targetCanvas) {
    return (plot, opacity,plotView) => {
        if (!isImage(plot) || !plot.affTrans || !hasLocalStretchByteData(plot)) return;
        const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot
        const boundingBox= computeBounding(rootPlot,plotView.viewDim.width,plotView.viewDim.height);
        const {x,y,w,h}= boundingBox;
        const offsetX= x>0 ? x : 0;
        const offsetY= y>0 ? y : 0;
        drawImage(plotView, plot, targetCanvas, offsetX,offsetY, opacity,{x, y, width:w, height:h});
    };
}
/**
 * return a object with two functions drawTile
 * @param {PlotView} plotView
 * @param {WebPlot} plot
 * @param {HTMLCanvasElement} targetCanvas
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} opacity
 * @param {{x:number,y:number,width:number,height:number}} tile
 * @return {{drawTileLocal:Function}}
 */
function drawImage(plotView, plot, targetCanvas, offsetX,offsetY, opacity, tile) {

    if (!targetCanvas) return;
    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);

    if (plotView.rotation) {
        const {viewDim:{width,height}}=  plotView;
        const diagonal= Math.sqrt(width*width + height*height);
        offscreenCanvas.width = diagonal;
        offscreenCanvas.height = diagonal;
    }

    offsetX= Math.trunc(offsetX);
    offsetY= Math.trunc(offsetY);
    const x = Math.trunc((tile.x) - offsetX);
    const y = Math.trunc((tile.y) - offsetY);
    drawScreenTileToMainCanvas(plot,tile,offscreenCanvas,x,y,Math.trunc(tile.width), Math.trunc(tile.height));
    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};
    renderToScreen(screenRenderParams);
}

function renderToScreen(screenRenderParams) {
    window.requestAnimationFrame(() => {
        const {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetY}= screenRenderParams;
        let {offsetX}= screenRenderParams;

        const ctx= targetCanvas.getContext('2d');
        ctx.save();
        if (isImage(plot)) {
            ctx.clearRect(0,0,targetCanvas.width, targetCanvas.height);
        }
        ctx.globalAlpha=opacity;

        const {scrollX, scrollY, flipX,flipY, viewDim, rotation}= plotView;
        if (flipY) offsetX*=-1;

        if (isNumber(scrollX) && isNumber(scrollY)) {
            if (isImage(plot)) {
                const affTrans= makeTransform(offsetX,offsetY, scrollX, scrollY, rotation, flipX, flipY, viewDim);
                ctx.setTransform(affTrans.a, affTrans.b, affTrans.c, affTrans.d, affTrans.e, affTrans.f);
            }
            ctx.drawImage(offscreenCanvas, 0,0);
        }
        ctx.restore();
    });
}
