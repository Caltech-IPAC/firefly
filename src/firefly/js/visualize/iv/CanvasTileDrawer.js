/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import CsysConverter from '../CsysConverter.js';
import {makeDevicePt} from '../Point.js';
import {createImageUrl,isTileVisible} from './TileDrawHelper.jsx';
import {makeTransform} from '../PlotTransformUtils.js';
import {primePlot, findProcessedTile} from '../PlotViewUtil.js';
import {getBoundingBox,toRadians} from '../VisUtil.js';
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


const noOp= { drawerTile : () => undefined,
              abort : () => undefined };

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

    if (!targetCanvas) return noOp;

    if (!totalCnt) {
        window.requestAnimationFrame(() => {
            targetCanvas.getContext('2d').clearRect(0,0,targetCanvas.width, targetCanvas.height);
        });
        return noOp;
    }


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
    let beginRender= false;
    let renderComplete=  false;
    
    offscreenCtx.font= 'italic 15pt Arial';
    offscreenCtx.fillStyle= 'rgba(0,0,0,.4)';
    offscreenCtx.strokeStyle='rgba(0,0,0,.2)';
    offscreenCtx.textAlign= 'center';
    offscreenCtx.lineWidth= 1;


    const renderImage= () => renderToScreen(plotView, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY);

    return {
        drawTile(src, tile) {
            const x = Math.trunc((tile.xoff * scale) - offsetX);
            const y = Math.trunc((tile.yoff * scale) - offsetY);
            const w = Math.trunc(tile.width * scale);
            const h = Math.trunc(tile.height * scale);

            if (opacity===1) drawEmptyTile(x,y,w,h,offscreenCtx,plotView);

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
                    renderImage();
                    purgeLoadedImages(loadedImages);
                }
            }).catch((e) => {
                console.log(e);
            });
        },

        abort()  {
            abortRender = true;
            if (opacity===1 && !renderComplete) renderImage();
        }
    };
}

function drawEmptyTile(x,y,w,h,ctx,plotView ) {
    if (w>150 && h>100) {
        ctx.save();
        const {flipY, rotation}= plotView;
        ctx.translate(x+w/2,y+h/2);
        ctx.rotate(toRadians(flipY ? rotation : -rotation));
        if (flipY)  ctx.scale(-1,1);
        ctx.fillText('Loading Tile...',0,0);
        ctx.restore();
        ctx.strokeRect(x, y, w-1, h-1);
    }

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

