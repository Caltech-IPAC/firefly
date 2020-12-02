/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil} from 'lodash';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {drawOneHiPSTile} from './HiPSSingleTileRender.js';
import {
    findTileCachedImage,
    addTileCachedImage,
    addFailedImage,
    isInFailTileCached,
} from './HiPSTileCache.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, makeTaskId, getTaskCount} from '../../core/AppDataCntlr.js';
import {createImageUrl, createEmptyTile} from './TileDrawHelper.jsx';

const emptyTileCanvas= createEmptyTile(512,512);

const colorId = (plot) => {
    const id= Number(plot.plotState.getColorTableId());
    return isNaN(id) ? -1 : id;
};

/**
 * The object that can render a HiPS to the screen.
 * @param {{plotView:PlotView, plot:WebPlot, targetCanvas:Canvas, offscreenCanvas:Canvas,
 *          opacity:number, offsetX:number, offsetY:number}} screenRenderParams
 * @param totalCnt
 * @param isBaseImage
 * @param tileProcessInfo
 * @param screenRenderEnabled
 * @param hipsColorOps
 * @return {{drawTile(*=, *=): undefined, drawTileImmediate(*=, *, *=): void, abort(): void}}
 */
export function makeHipsRenderer(screenRenderParams, totalCnt, isBaseImage, tileProcessInfo, screenRenderEnabled, hipsColorOps) {

    let renderedCnt=0;
    let abortRender= false;
    let firstRenderTime= 0;
    let renderComplete=  false;
    const {offscreenCanvas, plotView}= screenRenderParams;
    const offscreenCtx = offscreenCanvas.getContext('2d');
    const allImageCancelFuncs= [];
    let plotTaskId;



    //  ------------------------------------------------------------
    //  -------------------------  private functions
    //  ------------------------------------------------------------

    /**
     * draw a single tile async (retrieve image and draw tile)
     * @param src
     * @param {HiPSDeviceTileData} tile
     * @param colorTableId
     * @param bias
     * @param contrast
     */
    const drawTileAsync= (src, tile, colorTableId,bias,contrast) => {
        if (abortRender) return;
        let inCache;
        let tileData;
        let emptyTile;

        let cachedTile= findTileCachedImage(src,colorTableId,bias,contrast);
        if (colorTableId!==-1 && !cachedTile) {
            cachedTile= findTileCachedImage(src);
            if (cachedTile) {
                const coloredImage= hipsColorOps.changeHiPSColor(cachedTile.image,colorTableId,bias,contrast);
                addTileCachedImage(src, coloredImage,colorTableId,bias,contrast);
                cachedTile= findTileCachedImage(src,colorTableId,bias,contrast);
            }
        }

        if (!firstRenderTime) firstRenderTime= Date.now();
        if (cachedTile) {
            tileData=  cachedTile.image;
            emptyTile= cachedTile.emptyTile;
            inCache= true;
        }
        else {
            tileData=  src;
            emptyTile= false;
        }

        const {tileAttributes, shouldProcess, processor}= tileProcessInfo;

        let p;
        if (isInFailTileCached(tileData)) {
           p= Promise.reject();
        }
        else {
            const {promise, cancelImageLoad} = retrieveAndProcessImage(tileData, tileAttributes, shouldProcess, processor);
            allImageCancelFuncs.push(cancelImageLoad);
            p= promise;
        }

        const doRenderNow= () => {
            const now= Date.now();
            return (renderedCnt === totalCnt ||
                renderedCnt/totalCnt > .75 && now-firstRenderTime>1000 ||
                now-firstRenderTime>2000);
        };

        p.then((imageData) => {
            renderedCnt++;

            let image;
            if (!inCache && !emptyTile) {
                image= imageData.image;
                addTileCachedImage(src, image);
                if (colorTableId!==-1) {
                    image= hipsColorOps.changeHiPSColor(image,colorTableId,bias,contrast);
                    addTileCachedImage(src, image,colorTableId,bias,contrast);
                }

            }
            else {
                image= imageData instanceof HTMLCanvasElement ? imageData : imageData.image;
            }
            if (abortRender) return;

            const tileSize= tile.tileSize || image.width;
            drawOneHiPSTile(offscreenCtx, image, tile.devPtCorners, tileSize, {x:tile.dx,y:tile.dy}, tile.nside);


            if (doRenderNow()) renderToScreen();
            renderComplete= (renderedCnt === totalCnt);
            if (renderComplete) removeTask();
        }).catch(() => {
            renderedCnt++;
            if (abortRender) return;
            drawOneHiPSTile(offscreenCtx, emptyTileCanvas, tile.devPtCorners, 512, {x:tile.dx,y:tile.dy}, tile.nside);
            addFailedImage(src);
            if (doRenderNow()) {
                removeTask();
                renderComplete= true;
                renderToScreen();
            }
        });
    };

    /**
     * draw a tile async (retrieve image and draw tile).  Any retrieved tiles will the added to the cache.
     * @param {string} src - url of the image
     * @param {HiPSDeviceTileData} tile
     * @param allskyImage
     */
    const drawTileImmediate= (src, tile, allskyImage, colorTableId, bias, contrast) => {
        const image= allskyImage || findTileCachedImage(src, colorTableId, bias, contrast)?.image;
        if (image) {
            const tileSize= tile.tileSize || image.width;
            drawOneHiPSTile(offscreenCtx, image, tile.devPtCorners,
                tileSize, {x:tile.dx,y:tile.dy}, tile.nside);
        }
        renderedCnt++;
        if (renderedCnt === totalCnt) {
            renderComplete= true;
            renderToScreen();
        }
    };

    const drawAllSkyFromOneImage= (allSkyImage, tilesToLoad) => {

        const width= allSkyImage.width/27;
        let offset;
        for(let i=0; i<tilesToLoad.length; i++) { // do a classic for loop to increase the fps by 3 or 4
            offset= Math.floor(tilesToLoad[i].tileNumber/27);
            tilesToLoad[i].dy= width * offset;
            tilesToLoad[i].dx=  width * (tilesToLoad[i].tileNumber - 27*offset);
            tilesToLoad[i].tileSize= width;
            drawTileImmediate(null, tilesToLoad[i],allSkyImage);

        }
    };

    /**
     * Render the offscreen image to the screen
     */
    const renderToScreen= () => {
        if (!screenRenderEnabled) return;
        const {plotView, targetCanvas, offscreenCanvas, opacity}= screenRenderParams;
        const ctx= targetCanvas.getContext('2d');
        ctx.globalAlpha=opacity;
        if (!isNil(plotView.scrollX) && !isNil(plotView.scrollY)) {
            ctx.drawImage(offscreenCanvas, 0,0);
        }
    };

    const removeTask= () => {
        if (plotTaskId) {
            setTimeout( () => getTaskCount(plotView.plotId) && dispatchRemoveTaskCount(plotView.plotId,plotTaskId) ,0);
        }
    };


    //  ------------------------------------------------------------
    //  -------------------------  return public functions
    //  this object has not properties, just functions to render
    //  ------------------------------------------------------------
    return {

        /**
         *
         * draw all tiles async (check cache or retrieve image and draw tile).
         * Any retrieved tiles will the added to the cache.
         * @param {Array.<HiPSDeviceTileData>} tilesToLoad
         * @param {WebPlot} plot
         */
        drawAllTilesAsync(tilesToLoad, plot) {
            if (abortRender) return;
            plotTaskId= makeTaskId();
            setTimeout( () => {
                if (!abortRender && !renderComplete) dispatchAddTaskCount(plot.plotId,plotTaskId, true);
            }, 500);
            const {bias,contrast}= plot.rawData.bandData[0];
            const colorTableId= colorId(plot);
            tilesToLoad.forEach( (tile) => drawTileAsync(createImageUrl(plot,tile), tile, colorTableId,bias,contrast) );
        },

        /**
         *
         * draw all tiles immediately. Any tile not in cache will be ignored.
         * @param {Array.<HiPSDeviceTileData>} tilesToLoad
         * @param {WebPlot} plot
         */
        drawAllTilesImmediate(tilesToLoad, plot) {
            if (abortRender) return;
            const {bias,contrast}= plot.rawData.bandData[0];
            const colorTableId= colorId(plot);
            for(let i=0; i<tilesToLoad.length; i++) { // do a classic for loop to increase the fps by 3 or 4
                drawTileImmediate(createImageUrl(plot, tilesToLoad[i]), tilesToLoad[i], undefined, colorTableId, bias, contrast);
            }
        },

        /**
         *
         * @param {number} norder
         * @param {HiPSAllSkyCacheInfo} cachedAllSky
         * @param {Array.<HiPSDeviceTileData>} tilesToLoad
         */
        drawAllSky(norder, cachedAllSky, tilesToLoad) {
            if (abortRender) return;
            if (norder===3) {
                drawAllSkyFromOneImage(cachedAllSky.order3, tilesToLoad);
            }
            else {
                for(let i=0; i<tilesToLoad.length; i++) { // do a classic for loop to increase the fps by 3 or 4
                    drawTileImmediate(null, tilesToLoad[i],cachedAllSky.order2Array[tilesToLoad[i].tileNumber]);
                }
            }
        },

        /**
         * abort the last async draw, if last draw was sync, it is a noop
         */
        abort()  {
            if (abortRender) return;
            abortRender = true;
            if (isBaseImage && !renderComplete && renderedCnt>0) renderToScreen(screenRenderParams);
            if (!renderComplete) allImageCancelFuncs.forEach( (f) => f && f() );
            removeTask();
        }
    };
}

