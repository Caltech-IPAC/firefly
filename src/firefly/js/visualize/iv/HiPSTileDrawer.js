/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil, get} from 'lodash';
import Enum from 'enum';
import {createImageUrl,initOffScreenCanvas, computeBounding, isQuadTileOnScreen, drawEmptyRecTile} from './TileDrawHelper.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {drawOneHiPSTile} from './HiPSSingleTileRender.js';
import {getVisibleHiPSCells, getPointMaxSide, getBestHiPSlevel, makeHiPSAllSkyUrlFromPlot} from '../HiPSUtil.js';
import {loadImage} from '../../util/WebUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {findAllSkyCachedImage, findTileCachedImage, addAllSkyCachedImage, addTileCachedImage} from './HiPSTileCache.js';


const noOp= { drawerTile : () => undefined, abort : () => undefined };



export const DrawTiming= new Enum(['IMMEDIATE','ASYNC', 'DELAY']);

/**
 * Return a function that should be called on every render to draw the image
 * @param targetCanvas
 * @return {*}
 */
export function createHiPSDrawer(targetCanvas) {
    if (!targetCanvas) return () => undefined;
    let abortLastDraw= null;
    let lastDrawNorder= 0;
    let lastFov;


    return (plot, opacity,plotView, tileProcessInfo= {shouldProcess:false}) => {
        if (abortLastDraw) abortLastDraw(); // stop any incomplete drawing

        const {viewDim}= plotView;
        let transitionNorder;

        const {norder, useAllSky}= getBestHiPSlevel(plot, true);
        const {fov,centerWp}= getPointMaxSide(plot,viewDim);
        const tilesToLoad= findCellOnScreen(plot,viewDim,norder, fov, centerWp);
        let drawTiming= DrawTiming.ASYNC;

        if (useAllSky || tilesToLoad.every( (tile)=> findTileCachedImage(createImageUrl(plot,tile))) ) { // any case with all the tiles
            drawTiming= DrawTiming.IMMEDIATE;
        }
        else if (fovEqual(fov, lastFov)) { // scroll case
            drawTiming= DrawTiming.ASYNC; // in a zoom case, slow down drawing a little, to allow to the user click multiple times
            transitionNorder= lastDrawNorder-1;// for scroll transitionNorder needs to be set one back
        }
        else { // zoom or resize case, in a zoom or resize case, slow down drawing, to allow to use to finish
            drawTiming= DrawTiming.DELAY;
            transitionNorder= lastDrawNorder;
        }

        if (drawTiming!==DrawTiming.IMMEDIATE) {
            drawTransitionalImage(fov,centerWp,targetCanvas,plot, plotView,norder, transitionNorder,
                opacity, tileProcessInfo, tilesToLoad);
        }

        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,drawTiming!==DrawTiming.IMMEDIATE);
        abortLastDraw= drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, tilesToLoad, useAllSky,
            opacity, tileProcessInfo, drawTiming);
        lastDrawNorder= norder;
        lastFov= fov;
    }
}


/**
 * this is used for temporary image when zooming
 * @param fov
 * @param centerWp
 * @param targetCanvas
 * @param plot
 * @param plotView
 * @param norder
 * @param transitionNorder
 * @param opacity
 * @param tileProcessInfo
 * @param finalTileToLoad
 */
function drawTransitionalImage(fov, centerWp, targetCanvas, plot, plotView,
                               norder, transitionNorder, opacity, tileProcessInfo, finalTileToLoad) {
    const {viewDim}= plotView;
    let tilesToLoad;
    if (norder<=3) { // norder is always 3, need to fix this if
        const tilesToLoad2= findCellOnScreen(plot,viewDim,2, fov, centerWp);
        const tilesToLoad3= findCellOnScreen(plot,viewDim,3, fov, centerWp);
        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,false);
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, 2, tilesToLoad2, true,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE, false);
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, 3, tilesToLoad3, false,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
    }
    else {
        let lookMore= true;
        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,false);
        for( let testNorder= transitionNorder; (testNorder>=3 && lookMore); testNorder--) {
            tilesToLoad= findCellOnScreen(plot,viewDim,testNorder, fov, centerWp);
            const hasSomeTiles= tilesToLoad.some( (tile)=> findTileCachedImage(createImageUrl(plot,tile)));
            if (hasSomeTiles || testNorder===3) { // if there are tiles or we need to do the allsky
                drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, testNorder, tilesToLoad, testNorder===3,
                    opacity, tileProcessInfo, DrawTiming.IMMEDIATE, false);
                lookMore= false;
                // console.log(`draw transi: transitionNorder: ${transitionNorder}, testNorder: ${testNorder}`);
            }
        }
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, finalTileToLoad, false,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
    }
}



const fovEqual= (fov1,fov2) => Math.trunc(fov1*10000) === Math.trunc(fov2*10000);


/**
 *
 * @param {WebPlot} plot
 * @param viewDim
 * @param {number} norder
 * @param {number} fov
 * @param {WorldPt} centerWp
 * @return {Array}
 */
function findCellOnScreen(plot, viewDim, norder, fov,centerWp) {
    const cells= getVisibleHiPSCells(norder,centerWp, fov, plot.dataCoordSys);
    const cc= CysConverter.make(plot);

    const retCells= [];
    let devPtCorners;
                   // this function is performance sensitive, use for loops instead of map and filter
    for(let i= 0; (i<cells.length); i++) {
        devPtCorners= [];
        for(let j=0; (j<cells[i].wpCorners.length); j++)  {
            devPtCorners[j]= cc.getDeviceCoords(cells[i].wpCorners[j]);
            if (!devPtCorners[j]) break;
        }
        if (isQuadTileOnScreen(devPtCorners, viewDim)) {
            retCells.push({devPtCorners, tileNumber:cells[i].ipix, dx:0, dy:0, nside: norder});
        }
    }
    return retCells;
}




/**
 *
 * @param targetCanvas
 * @param offscreenCanvas
 * @param plot
 * @param plotView
 * @param norder
 * @param tilesToLoad
 * @param useAllSky
 * @param opacity
 * @param tileProcessInfo
 * @param drawTiming
 * @param screenRenderEnabled
 */
function drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, tilesToLoad, useAllSky, opacity, tileProcessInfo,
                     drawTiming= DrawTiming.ASYNC, screenRenderEnabled= true) {
    const {viewDim}= plotView;
    const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot
    const boundingBox= computeBounding(rootPlot,viewDim.width,viewDim.height);
    const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
    const offsetY= boundingBox.y>0 ? boundingBox.y : 0;

    if (!targetCanvas) return noOp;
    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};
    const drawer= makeHipsDrawer(screenRenderParams, tilesToLoad.length, !plot.asOverlay,
                                      tileProcessInfo, screenRenderEnabled);
    
    if (useAllSky) {
        const allSkyURL= makeHiPSAllSkyUrlFromPlot(plot);
        let cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
        if (cachedAllSkyImage) {
            drawAllSky(norder, cachedAllSkyImage, tilesToLoad, drawer);
        }
        else {
            loadImage(makeHiPSAllSkyUrlFromPlot(plot))
                .then( (allSkyImage) =>
                {
                    addAllSkyCachedImage(allSkyURL, allSkyImage);
                    cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
                    drawAllSky(norder, cachedAllSkyImage, tilesToLoad, drawer);
                })
                .catch( () => {
                    // this should not happen - there is not all sky image so we are looking for the full tiles.
                    tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
                });
        }
    }
    else {
        switch (drawTiming) {
            case DrawTiming.IMMEDIATE:
                tilesToLoad.forEach( (tile) => drawer.drawTileImmediate(createImageUrl(plot,tile), tile) );
                break;
            case DrawTiming.ASYNC:
                tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
                break;
            case DrawTiming.DELAY:
                setTimeout( () => {
                    tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
                }, 250);
                break;
        }
    }
    return drawer.abort; // this abort function will any async promise calls stop before they draw

}

function drawAllSky(norder, cachedAllSky, tilesToLoad, drawer) {
    if (norder===3) {
        drawAllSkyFromOneImage(cachedAllSky.order3, tilesToLoad, drawer);
    }
    else {
        for(let i=0; i<tilesToLoad.length; i++) { // do a classic for loop to increase the fps by 3 or 4
            drawer.drawTileImmediate(null, tilesToLoad[i],cachedAllSky.order2Array[tilesToLoad[i].tileNumber]);
        }
    }
}


function drawAllSkyFromOneImage(allSkyImage, tilesToLoad, drawer) {

    const width= allSkyImage.width/27;
    let offset;
    for(let i=0; i<tilesToLoad.length; i++) { // do a classic for loop to increase the fps by 3 or 4
        offset= Math.floor(tilesToLoad[i].tileNumber/27);
        tilesToLoad[i].dy= width * offset;
        tilesToLoad[i].dx=  width * (tilesToLoad[i].tileNumber - 27*offset);
        tilesToLoad[i].tileSize= width;
        drawer.drawTileImmediate(null, tilesToLoad[i],allSkyImage);

    }
}


/**
 *
 * @param plotView
 * @param plot
 * @param overlayTransparent
 */
function makeOffScreenCanvas(plotView, plot, overlayTransparent) {

    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);
    const offscreenCtx = offscreenCanvas.getContext('2d');

    const {viewDim:{width,height}}=  plotView;

    offscreenCtx.fillStyle = overlayTransparent ? 'rgba(0,0,0,0)'  : 'rgba(227,227,227,1)';
    // offscreenCtx.fillRect(0, 0, width, height);


    const {fov, centerDevPt}= getPointMaxSide(plot,plotView.viewDim);
    if (fov>=180) {
        const altDevRadius= plotView.viewDim.width/2 + plotView.scrollX;
        offscreenCtx.fillStyle = 'rgba(227,227,227,1)';
        offscreenCtx.fillRect(0, 0, width, height);
        offscreenCtx.save();
        offscreenCtx.beginPath();
        offscreenCtx.lineWidth= 5;
        offscreenCtx.arc(centerDevPt.x, centerDevPt.y, altDevRadius-4, 0, 2*Math.PI, false);
        offscreenCtx.closePath();
        offscreenCtx.clip();
    }
    offscreenCtx.fillStyle = overlayTransparent ? 'rgba(0,0,0,0)'  : 'rgba(0,0,0,1)';
    offscreenCtx.fillRect(0, 0, width, height);
    return offscreenCanvas;
}


/**
 *
 * @param {{plotView:PlotView, plot:WebPlot, targetCanvas:Canvas, offscreenCanvas:Canvas,
 *          opacity:number, offsetX:number, offsetY:number}} screenRenderParams
 * @param totalCnt
 * @param isBaseImage
 * @param tileProcessInfo
 * @param screenRenderEnabled
 * @return {{drawTile(*=, *=): undefined, drawTileImmediate(*=, *, *=): void, abort(): void}}
 */
function makeHipsDrawer(screenRenderParams, totalCnt, isBaseImage, tileProcessInfo, screenRenderEnabled) {

    let renderedCnt=0;
    let abortRender= false;
    let firstRenderTime= 0;
    let renderComplete=  false;
    const {offscreenCanvas, plotView}= screenRenderParams;
    const offscreenCtx = offscreenCanvas.getContext('2d');


    return {
        drawTile(src, tile) {
            if (abortRender) return;
            if (isBaseImage) drawEmptyRecTile(tile.devPtCorners,offscreenCtx,plotView);
            let inCache;
            let tileData;
            let emptyTile;

            const cachedTile= findTileCachedImage(src);
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
            const p = retrieveAndProcessImage(tileData, tileAttributes, shouldProcess, processor);
            p.then((imageData) => {
                renderedCnt++;

                if (!inCache) addTileCachedImage(src, imageData);
                if (abortRender) return;

                if (emptyTile) {
                    drawEmptyTile(offscreenCtx,tile);
                }
                else {
                    const tileSize= tile.tileSize || imageData.image.width;
                    drawOneHiPSTile(offscreenCtx, imageData.image, tile.devPtCorners,
                        tileSize, {x:tile.dx,y:tile.dy}, true, tile.nside);
                }


                const now= Date.now();
                const renderNow= (renderedCnt === totalCnt ||
                                  renderedCnt/totalCnt > .75 && now-firstRenderTime>1000 ||
                                  now-firstRenderTime>2000);
                // console.log(`${renderedCnt} of ${totalCnt}, renderNow: ${renderNow}, time diff ${(now-firstRenderTime)/1000}`);
                if (renderNow && screenRenderEnabled) renderToScreen(screenRenderParams);
                renderComplete= (renderedCnt === totalCnt);
            }).catch(() => {
                addTileCachedImage(src, null, true);
                renderedCnt++;
                if (abortRender) return;
                drawEmptyTile(offscreenCtx, tile);
                if (renderedCnt === totalCnt && screenRenderEnabled) {
                    renderComplete= true;
                    renderToScreen(screenRenderParams);
                }
            });
        },



        drawTileImmediate(src, tile, allskyImage) {
            const image= allskyImage || get(findTileCachedImage(src),'image.image');
            if (image) {
                const tileSize= tile.tileSize || image.width;
                drawOneHiPSTile(offscreenCtx, image, tile.devPtCorners,
                    tileSize, {x:tile.dx,y:tile.dy}, true, tile.nside);
            }
            renderedCnt++;
            if (renderedCnt === totalCnt && screenRenderEnabled) {
                renderComplete= true;
                renderToScreen(screenRenderParams);
            }
        },

        abort()  {
            abortRender = true;
            if (isBaseImage && !renderComplete && renderedCnt>0 && screenRenderEnabled) renderToScreen(screenRenderParams);
        }
    };
}


function drawEmptyTile(offscreenCtx, tile) {

    const DRAW_EMPTY= true;
    if (DRAW_EMPTY) {

        offscreenCtx.fillStyle = 'rgba(40,40,40,1)';
        offscreenCtx.save();
        offscreenCtx.beginPath();
        const {devPtCorners}= tile;

        offscreenCtx.moveTo(devPtCorners[0].x, devPtCorners[0].y);
        for(let i= 1; i<devPtCorners.length; i++) {
            offscreenCtx.lineTo(devPtCorners[i].x, devPtCorners[i].y);
        }
        offscreenCtx.closePath();
        offscreenCtx.fill();
        offscreenCtx.restore();
    }

}


function renderToScreen(screenRenderParams) {
    // window.requestAnimationFrame(() => {
        const {plotView, targetCanvas, offscreenCanvas, opacity}= screenRenderParams;
        const ctx= targetCanvas.getContext('2d');
        // ctx.save();
        ctx.globalAlpha=opacity;
        if (!isNil(plotView.scrollX) && !isNil(plotView.scrollY)) {
            ctx.drawImage(offscreenCanvas, 0,0);
        }
        // ctx.restore();
    // });
}

