/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil} from 'lodash';
import {createImageUrl,isTileVisible, initOffScreenCanvas, computeBounding} from './TileDrawHelper.jsx';
import {makeTransform} from '../PlotTransformUtils.js';
import {primePlot, findProcessedTile} from '../PlotViewUtil.js';
import {clone} from '../../util/WebUtil.js';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {dispatchAddProcessedTiles, visRoot} from '../ImagePlotCntlr.js';
import {isImage} from '../WebPlot.js';


/**
 * Return a function that should be called on every render to draw the image
 * @param targetCanvas
 * @return {*}
 */
export function initImageDrawer(targetCanvas) {
    let loadedImages= {
        serverData: [],
        cachedImageData: [],
        used: []
    };
    let abortLastDraw= null;


    return (plot, opacity,plotView, tileProcessInfo= {shouldProcess:false}) => {
        if (abortLastDraw) abortLastDraw();
        const {tileData} = plot;
        const scale = plot.zoomFactor/ plot.plotState.getZoomLevel(); // note plotState contains the zoom level that tile where produced for.

        if (tileData && loadedImages.serverData!==tileData.images) {
            loadedImages= {
                plotId: plotView.plotId,
                imageOverlayId: undefined,
                plotImageId: plot.plotImageId,
                tileZoomLevel: plot.plotState.getZoomLevel(),
                serverData: tileData.images,
                cachedImageData: [],
                used: []
            };
            if (plot.plotId!==plotView.plotId) { // this is a overlay plot
                loadedImages.imageOverlayId= plot.plotId;
            }
        }

        const {viewDim}= plotView;
        const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot

        const boundingBox= computeBounding(rootPlot,viewDim.width,viewDim.height);
        const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
        const offsetY= boundingBox.y>0 ? boundingBox.y : 0;

        const tilesToLoad= tileData.images.filter( (tile) =>
            isTileVisible(tile,boundingBox.x,boundingBox.y,boundingBox.w,boundingBox.h,scale));


        const drawer= makeImageDrawer(plotView, plot, targetCanvas, tilesToLoad.length, loadedImages,
            offsetX,offsetY, scale, opacity, tileProcessInfo);

        tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
        abortLastDraw= drawer.abort;
    };
}






const noOp= { drawerTile : () => undefined, abort : () => undefined };

/**
 * return a object with two functions drawTile and abort
 * @param {PlotView} plotView
 * @param {WebPlot} plot
 * @param {Object} targetCanvas
 * @param {number} totalCnt
 * @param {Object} loadedImages
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} scale
 * @param {number} opacity
 * @param {Object} tileProcessInfo
 * @return {function(*, *)}
 */
function makeImageDrawer(plotView, plot, targetCanvas, totalCnt, loadedImages,
                    offsetX,offsetY, scale, opacity, tileProcessInfo) {

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


function makeImageDrawTileObj(screenRenderParams, totalCnt, scale, loadedImages, isBaseImage, tileProcessInfo) {

    let renderedCnt=0;
    let abortRender= false;
    let renderComplete=  false;
    const {offscreenCanvas, offsetX, offsetY, plotView}= screenRenderParams;
    const offscreenCtx = offscreenCanvas.getContext('2d');


    return {
        drawTile(src, tile) {
            const x = Math.trunc((tile.x * scale) - offsetX);
            const y = Math.trunc((tile.y * scale) - offsetY);
            const w = Math.trunc(tile.width * scale);
            const h = Math.trunc(tile.height * scale);

            // if (isBaseImage) drawEmptyRecTile(x,y,w,h,offscreenCtx,plotView);

            let tileData;
            const tileIdx = loadedImages.serverData.findIndex((d) => d.url === tile.url);
            const storeTile= findProcessedTile(visRoot(), plotView.plotId, tile.url);

            if (loadedImages.cachedImageData[tileIdx]) tileData= loadedImages.cachedImageData[tileIdx];
            else if (storeTile) {
                tileData=storeTile;
            }
            else tileData=src;

            const {tileAttributes, shouldProcess, processor}= tileProcessInfo;
            const p = retrieveAndProcessImage(tileData, tileAttributes, shouldProcess, processor);
            p.then((imageData) => {
                loadedImages.cachedImageData[tileIdx] = clone(loadedImages.serverData[tileIdx],
                    {dataUrl:null, tileAttributes:imageData.tileAttributes, image:imageData.image});

                loadedImages.used[tileIdx] = Date.now();
                renderedCnt++;

                if (abortRender) {
                    purgeLoadedImages(loadedImages);
                    return;
                }
                offscreenCtx.drawImage(loadedImages.cachedImageData[tileIdx].image, x, y, w, h);

                if (renderedCnt === totalCnt) {
                    renderComplete= true;
                    renderToScreen(screenRenderParams);
                    purgeLoadedImages(loadedImages);
                }
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

        if (!isNil(plotView.scrollX) && !isNil(plotView.scrollY)) {
            if (isImage(plot)) {
                const affTrans= makeTransform(offsetX,offsetY, scrollX, scrollY, rotation, flipX, flipY, viewDim);
                ctx.setTransform(affTrans.a, affTrans.b, affTrans.c, affTrans.d, affTrans.e, affTrans.f);
            }
            ctx.drawImage(offscreenCanvas, 0,0);
        }
        ctx.restore();
    });
}




function purgeLoadedImages(loadedImages) {
    let i;
    const fiveSecAgo= Date.now()- (1000 * 5);
    const saveTileData= [];
    let saveCnt= 0;
    for(i=0; (i<loadedImages.serverData.length); i++) {
        if (loadedImages.used[i] && loadedImages.used[i] < fiveSecAgo) {
            if (loadedImages.cachedImageData[i]) {
                saveTileData[saveCnt++]= loadedImages.cachedImageData[i];
            }
            loadedImages.cachedImageData[i]= null;
        }
    }

    const dispatchData= saveTileData.filter( (d) => d.image instanceof HTMLCanvasElement)
        .map( (d) => clone(d, {image:undefined, dataUrl: d.image.toDataURL('image/png')}) );
    if (dispatchData.length) {
        const {plotId, plotImageId, imageOverlayId, tileZoomLevel}= loadedImages;
        dispatchAddProcessedTiles(plotId, imageOverlayId, plotImageId, tileZoomLevel, dispatchData);
    }
}

