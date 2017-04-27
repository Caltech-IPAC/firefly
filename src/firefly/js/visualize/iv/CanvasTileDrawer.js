/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import CsysConverter from '../CsysConverter.js';
import {makeDevicePt} from '../Point.js';
import {createImageUrl,isTileVisible} from './TileDrawHelper.jsx';
import {makeTransform} from '../PlotTransformUtils.js';
import {primePlot, findProcessedTile} from '../PlotViewUtil.js';
import {getBoundingBox} from '../VisUtil.js';
import {clone} from '../../util/WebUtil.js';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {dispatchAddProcessedTiles, visRoot} from '../ImagePlotCntlr.js';


export function initTileDrawer(targetCanvas) {
    if (!targetCanvas) return () => undefined;
    let loadedImages= {
        serverData: [],
        cachedImageData: [],
        used: []
    };
    let abortLastDraw= null;


    return (plot, opacity,plotView, tileAttributes, shouldProcess, processor) => {
        if (abortLastDraw) abortLastDraw();
        const tileData = plot.serverImages;
        const scale = plot.zoomFactor/ plot.plotState.getZoomLevel(); // note plotState contains the zoom level that tile where produced for.

        if (loadedImages.serverData!==tileData.images) {
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


        const tilesToLoad= tileData.images.filter( (tile) =>
            isTileVisible(tile,boundingBox.x,boundingBox.y,boundingBox.w,boundingBox.h,scale));

        const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
        const offsetY= boundingBox.y>0 ? boundingBox.y : 0;
        const drawer= makeDrawer(plotView, plot, targetCanvas, tilesToLoad.length, loadedImages,
            offsetX,offsetY, scale, opacity, tileAttributes, shouldProcess, processor);

        tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
        abortLastDraw= drawer.abort;
    };
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

/**
 *
 * @param {PlotView} plotView
 * @param {WebPlot} plot
 * @param {Object} targetCanvas
 * @param {number} totalCnt
 * @param {Object} loadedImages
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} scale
 * @param {number} opacity
 * @param {Object} tileAttributes
 * @param {Function} shouldProcess
 * @param {Function} processor
 * @return {function(*, *)}
 */
function makeDrawer(plotView, plot, targetCanvas, totalCnt, loadedImages,
                    offsetX,offsetY, scale, opacity, tileAttributes, shouldProcess, processor) {

    if (!targetCanvas) return;
    const offscreenCanvas = document.createElement('canvas');
    const offscreenCtx = offscreenCanvas.getContext('2d');
    const {viewDim:{width,height}}=  plotView;
    if (plotView.rotation) {
        const diagonal= Math.sqrt(width*width + height*height);
        offscreenCanvas.width = diagonal;
        offscreenCanvas.height = diagonal;
    }
    else {
        offscreenCanvas.width = width;
        offscreenCanvas.height = height;
    }


    offsetX= Math.trunc(offsetX);
    offsetY= Math.trunc(offsetY);
    let renderedCnt=0;
    let abortRender= false;

    return {
        drawTile(src, tile) {
            const x = Math.trunc((tile.xoff * scale) - offsetX);
            const y = Math.trunc((tile.yoff * scale) - offsetY);
            const w = Math.trunc(tile.width * scale);
            const h = Math.trunc(tile.height * scale);


            // todo: modify this line to look first in local, then in store, then finally used the server iage
            let tileData;
            const tileIdx = loadedImages.serverData.findIndex((d) => d.url === tile.url);
            const storeTile= findProcessedTile(visRoot(), plotView.plotId, tile.url);

            if (loadedImages.cachedImageData[tileIdx]) tileData= loadedImages.cachedImageData[tileIdx];
            else if (storeTile) {
                tileData=storeTile;
            }
            else tileData=src;
            



            const p = retrieveAndProcessImage(tileData, tileAttributes, shouldProcess, processor);
            p.then((imageData) => {
                offscreenCtx.drawImage(imageData.image, x, y, w, h);

                loadedImages.cachedImageData[tileIdx] = clone(loadedImages.serverData[tileIdx],
                                                    {dataUrl:null, tileAttributes:imageData.tileAttributes, image:imageData.image});

                loadedImages.used[tileIdx] = Date.now();
                renderedCnt++;

                if (abortRender) {
                    purgeLoadedImages(loadedImages);
                    return;
                }
                offscreenCtx.drawImage(loadedImages.cachedImageData[tileIdx].image, x, y, w, h);
                // if (debugTiles) {
                //     offscreenCtx.lineWidth= 1;
                //     offscreenCtx.strokeStyle= 'red';
                //     offscreenCtx.strokeRect(x, y, w-1, h-1);
                //     offscreenCtx.restore();
                // }

                if (renderedCnt === totalCnt) {
                    renderToScreen(plotView, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY);
                    purgeLoadedImages(loadedImages);
                }
            }).catch((e) => {
                console.log(e);
            });
        },

        abort()  {
            abortRender = true;
        }
    };
}



function renderToScreen(plotView, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY) {
    window.requestAnimationFrame(() => {
        const ctx= targetCanvas.getContext('2d');
        ctx.save();
        ctx.clearRect(0,0,targetCanvas.width, targetCanvas.height);
        ctx.globalAlpha=opacity;

        const {scrollX, scrollY, flipX,flipY, viewDim, rotation}= plotView;
        if (flipY) offsetX*=-1;

        const affTrans= makeTransform(offsetX,offsetY, scrollX, scrollY, rotation, flipX, flipY, viewDim);
        ctx.setTransform(affTrans.a, affTrans.b, affTrans.c, affTrans.d, affTrans.e, affTrans.f);
        ctx.drawImage(offscreenCanvas, 0,0);
        ctx.restore();
    });
}


function computeBounding(plot,w,h) {
    const ptAry= [];
    const cc= CsysConverter.make(plot);
    ptAry.push(cc.getScreenCoords(makeDevicePt(0,0)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(w,0)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(w,h)));
    ptAry.push(cc.getScreenCoords(makeDevicePt(0,h)));
    return getBoundingBox(ptAry);
}

