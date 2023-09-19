/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil} from 'lodash';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {drawOneHiPSTile} from './HiPSSingleTileRender.js';
import {findTileCachedImage, addTileCachedImage, addFailedImage, isInFailTileCached} from './HiPSTileCache.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, makeTaskId, getTaskCount} from '../../core/AppDataCntlr.js';
import {createImageUrl, createEmptyTile} from './TileDrawHelper.jsx';

const emptyTileCanvas= createEmptyTile(512,512);

const colorId = (plot) => plot?.colorTableId ?? -1;

/**
 * The object that can render a HiPS to the screen.
 * @param {{plotView:PlotView, plot:WebPlot, targetCanvas:Canvas, offscreenCanvas:Canvas,
 *          opacity:number, offsetX:number, offsetY:number}} screenRenderParams
 * @param totalCnt
 * @param isBaseImage
 * @param screenRenderEnabled
 * @param hipsColorOps
 * @param {boolean} isMaxOrder - true if the order drawing is max order
 * @param {number} norder
 * @param {number} desiredNorder
 * @return {Object}
 */
export function makeHipsRenderer(screenRenderParams, totalCnt, isBaseImage, screenRenderEnabled,
                                 hipsColorOps, isMaxOrder, norder, desiredNorder) {

    let renderedCnt=0;
    let abortRender= false;
    let firstRenderTime= 0;
    let renderComplete=  false;
    const {offscreenCanvas, plotView}= screenRenderParams;
    const offscreenCtx = offscreenCanvas.getContext('2d');
    offscreenCtx.imageSmoothingEnabled = !isMaxOrder;
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

        let p;
        if (isInFailTileCached(tileData)) {
           p= Promise.reject();
        }
        else {
            const {promise, cancelImageLoad} = retrieveAndProcessImage(tileData);
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
            drawOneHiPSTile(offscreenCtx, image, tile.devPtCorners, tileSize, {x:tile.dx,y:tile.dy}, isMaxOrder, norder, desiredNorder);


            if (doRenderNow()) renderToScreen();
            renderComplete= (renderedCnt === totalCnt);
            if (renderComplete) removeTask();
        }).catch(() => {
            renderedCnt++;
            if (abortRender) return;
            if (tile.devPtCorners.filter( (t) => t).length ===4) {
                drawOneHiPSTile(offscreenCtx, emptyTileCanvas, tile.devPtCorners, 512, {x:tile.dx,y:tile.dy}, isMaxOrder, norder, desiredNorder);
            }
            else {
                console.log('********************* found one');
            }
            addFailedImage(src);
            if (doRenderNow()) {
                removeTask();
                renderComplete= true;
                renderToScreen();
            }
        });
    };

    /**
     * draw a tile when all
     * @param image
     * @param {HiPSDeviceTileData} tile
     * @param {HiPSAllSkyCacheInfo} [cachedAllSkyData]
     */
    const drawTileImmediate= (image, tile, cachedAllSkyData) => {
        if (image) {
            if (tile.coordsWrap && cachedAllSkyData) {
                tile.subCells?.forEach( (cell) => {
                    const subImage= cachedAllSkyData.order3Array[cell.tileNumber];
                    drawOneHiPSTile(offscreenCtx, subImage, cell.devPtCorners, subImage.width, {x:0,y:0}, isMaxOrder, norder, desiredNorder);
                });
            }
            else {
                const tileSize= tile.tileSize || image.width;
                drawOneHiPSTile(offscreenCtx, image, tile.devPtCorners, tileSize, {x:tile.dx,y:tile.dy}, isMaxOrder, norder, desiredNorder);
            }
        }
        renderedCnt++;
        if (renderedCnt === totalCnt) {
            renderComplete= true;
            renderToScreen();
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
    //  this object has no properties, just functions to render
    //  ------------------------------------------------------------
    return {
        renderToScreen,

        /**
         *
         * draw all tiles async (check cache or retrieve image and draw tile).
         * Any retrieved tiles will the added to the cache.
         * @param {Array.<HiPSDeviceTileData>} tilesToLoad
         * @param {WebPlot} plot
         */
        drawAllTilesAsync(tilesToLoad, plot) {
            if (abortRender) return;
            plotTaskId= makeTaskId('hips-render');
            setTimeout( () => {
                if (!abortRender && !renderComplete) dispatchAddTaskCount(plot.plotId,plotTaskId);
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
                const image= findTileCachedImage(createImageUrl(plot, tilesToLoad[i]), colorTableId, bias, contrast)?.image;
                drawTileImmediate(image, tilesToLoad[i]);
            }
        },

        /**
         *
         * @param {HiPSAllSkyCacheInfo} cachedAllSky
         * @param {Array.<HiPSDeviceTileData>} tilesToLoad
         */
        drawAllSky(cachedAllSky, tilesToLoad) {
            if (abortRender) return;
            const allSkyAry= norder===3 ? cachedAllSky.order3Array : cachedAllSky.order2Array;
            for(let i=0; i<tilesToLoad.length; i++) { // do a classic for loop to increase the fps by 3 or 4
                drawTileImmediate(allSkyAry[tilesToLoad[i].tileNumber], tilesToLoad[i], cachedAllSky);
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

