/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNumber} from 'lodash';
import {createImageUrl,isTileVisible, initOffScreenCanvas, computeBounding} from './TileDrawHelper.jsx';
import {makeTransform} from '../PlotTransformUtils.js';
import {primePlot, hasLocalStretchByteData} from '../PlotViewUtil.js';
import {clone} from '../../util/WebUtil.js';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {isImage} from '../WebPlot.js';
import {drawScreenTileToMainCanvas} from '../rawData/RawDataOps.js';


/**
 * Return a function that should be called on every render to draw the image
 * @param targetCanvas
 * @return {*}
 */
export function initImageDrawer(targetCanvas) {
    let loadedImages= { tileDefAry: [], cachedImageData: [], used: [] };
    let abortLastDraw= null;


    return (plot, opacity,plotView, tileProcessInfo= {shouldProcess:false}) => {
        if (!isImage(plot) || !plot.affTrans) return;
        abortLastDraw?.();
        const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot
        const boundingBox= computeBounding(rootPlot,plotView.viewDim.width,plotView.viewDim.height);
        const {x,y,w,h}= boundingBox;
        const offsetX= x>0 ? x : 0;
        const offsetY= y>0 ? y : 0;

        if (hasLocalStretchByteData(plot)) {
            const {drawTileLocal}= makeImageDrawer(plotView, plot, targetCanvas, offsetX,offsetY, opacity);
            const localDrawer= (imageTile, canvas,x,y,w,h) => drawScreenTileToMainCanvas(plot,imageTile,canvas,x,y,w,h);
            drawTileLocal( {x, y, width:w, height:h, local:true}, localDrawer);
            abortLastDraw= null; // no need to abort for local, no async involved
        }
        else {
            const tileDefAry= plot.tileData?.images;
            if (!tileDefAry) return;
            if (loadedImages.tileDefAry!==tileDefAry) {
                loadedImages= {
                    plotId: plotView.plotId,
                    imageOverlayId: undefined,
                    plotImageId: plot.plotImageId,
                    tileZoomLevel: plot.plotState.getZoomLevel(),
                    tileDefAry,
                    cachedImageData: [],
                    used: []
                };
                if (plot.plotId!==plotView.plotId) { // this is a overlay plot
                    loadedImages.imageOverlayId= plot.plotId;
                }
            }
            const scale = plot.zoomFactor/ plot.plotState.getZoomLevel(); // note plotState contains the zoom level that tile where produced for.
            const tilesToLoad= tileDefAry.filter( (tile) => isTileVisible(tile,x,y,w,h,scale));
            const {drawTile,abort}= makeImageDrawer(plotView, plot, targetCanvas, offsetX,offsetY, opacity,
                tilesToLoad.length, scale, loadedImages, tileProcessInfo);
            tilesToLoad.forEach( (tile) => drawTile(createImageUrl(plot,tile), tile) );
            abortLastDraw= abort;
        }
    };
}






const noOp= { drawerTile : () => undefined, abort : () => undefined };

/**
 * return a object with two functions drawTile and abort
 * @param {PlotView} plotView
 * @param {WebPlot} plot
 * @param {Object} targetCanvas
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} opacity
 * @param {number} [totalCnt]
 * @param {number} [scale]
 * @param {Object} [loadedImages]
 * @param {Object} [tileProcessInfo]
 * @return {{drawTileLocal:Function, drawTileLocal:Function, abort:Function}}
 */
function makeImageDrawer(plotView, plot, targetCanvas, offsetX,offsetY, opacity,
                         totalCnt=1, scale=1, loadedImages,tileProcessInfo) {

    if (!targetCanvas) return noOp;

    if (!totalCnt) {
        window.requestAnimationFrame(() => {
            targetCanvas.getContext('2d').clearRect(0,0,targetCanvas.width, targetCanvas.height);
        });
        return noOp;
    }

    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);

    if (plotView.rotation) {
        const {viewDim:{width,height}}=  plotView;
        const diagonal= Math.sqrt(width*width + height*height);
        offscreenCanvas.width = diagonal;
        offscreenCanvas.height = diagonal;
    }

    offsetX= Math.trunc(offsetX);
    offsetY= Math.trunc(offsetY);

    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};

    return makeImageDrawTileObj(screenRenderParams, totalCnt, scale, loadedImages, !plot.asOverlay, tileProcessInfo);

}


/**
 *
 * @param screenRenderParams
 * @param {number} totalCnt
 * @param {number} scale
 * @param loadedImages
 * @param {boolean} isBaseImage
 * @param tileProcessInfo
 * @return {{drawTileLocal:Function, drawTileLocal:Function, abort:Function}}
 */
function makeImageDrawTileObj(screenRenderParams, totalCnt, scale, loadedImages,
                              isBaseImage, tileProcessInfo) {

    let renderedCnt=0;
    let abortRender= false;
    let renderComplete=  false;
    const {offscreenCanvas, offsetX, offsetY}= screenRenderParams;
    const offscreenCtx = offscreenCanvas.getContext('2d');

    function drawToMainCanvas(image,x,y,w,h,tile,localImageRetriever) {
        renderedCnt++;

        if (abortRender) {
            // purgeLoadedImages(loadedImages);
            return;
        }
        if (!image && tile?.local && localImageRetriever) {
            localImageRetriever(tile,offscreenCanvas,x,y,w,h);
        }
        else {
            offscreenCtx.drawImage(image, x, y, w, h);
        }

        if (renderedCnt === totalCnt) {
            renderComplete= true;
            renderToScreen(screenRenderParams);
            // purgeLoadedImages(loadedImages);
        }

    }

    return {

        drawTileLocal(tile, localImageRetriever) {
            const x = Math.trunc((tile.x) - offsetX);
            const y = Math.trunc((tile.y) - offsetY);
            drawToMainCanvas(undefined, x, y,
                Math.trunc(tile.width), Math.trunc(tile.height), tile, localImageRetriever);
        },

        drawTile(src, tile) {
            const x = Math.trunc((tile.x * scale) - offsetX);
            const y = Math.trunc((tile.y * scale) - offsetY);
            const w = Math.trunc(tile.width * scale);
            const h = Math.trunc(tile.height * scale);

            const tileIdx = tile.local ? -1 : loadedImages.tileDefAry.findIndex((d) => d.local ? d.key===tile.key : d.url === tile.url);
            // const storeTile= findProcessedTile(visRoot(), plotView.plotId, tile.url);

            let tileData;
            if (loadedImages.cachedImageData[tileIdx]) tileData= loadedImages.cachedImageData[tileIdx];
            else tileData=src;
            

            const {tileAttributes, shouldProcess, processor}= tileProcessInfo;
            const {promise} = retrieveAndProcessImage(tileData, tileAttributes, shouldProcess, processor);
            promise.then((imageData) => {
                loadedImages.cachedImageData[tileIdx] = clone(loadedImages.tileDefAry[tileIdx],
                    {dataUrl:null, tileAttributes:imageData.tileAttributes, image:imageData.image});
                loadedImages.used[tileIdx] = Date.now();
                drawToMainCanvas(imageData.image,x,y,w,h);
            }).catch((e) => {
                console.log(e);
            });
        },

        abort()  {
            abortRender = true;
            if (isBaseImage && !renderComplete) renderToScreen(screenRenderParams);
        }
    };
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

