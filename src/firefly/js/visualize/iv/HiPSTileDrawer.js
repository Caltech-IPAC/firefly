/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {createImageUrl,initOffScreenCanvas, computeBounding, isQuadTileOnScreen} from './TileDrawHelper.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {getVisibleHiPSCells, getPointMaxSide, getHiPSNorderlevel, makeHiPSAllSkyUrlFromPlot} from '../HiPSUtil.js';
import {loadImage} from '../../util/WebUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {findAllSkyCachedImage, findTileCachedImage, addAllSkyCachedImage} from './HiPSTileCache.js';
import {makeHipsRenderer} from './HiPSRenderer.js';


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
        abortLastDraw && abortLastDraw(); // stop any incomplete drawing

        const {viewDim}= plotView;
        let transitionNorder;

        const {norder, useAllSky}= getHiPSNorderlevel(plot, true);
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
    };
}


/**
 * draw a transitional image when scrolling or zooming.  The transitional image will be overlaid with the final image.
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
          // draw the level 2 all sky and then draw on top what ever part of the full resolution level 3 tiles that are in cache
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
        // find some lower resolution norder to draw, as long as there is at least one tile available in cache, us it.
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
        // draw what ever part of the nornder tiles that are in cache on top
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, finalTileToLoad, false,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
    }
}



const fovEqual= (fov1,fov2) => Math.trunc(fov1*10000) === Math.trunc(fov2*10000);



/**
 * @global
 * @public
 * @typedef {Object} HiPSDeviceTileData
 *
 * @prop {number} tileNumber - HiPS pixel number
 * @prop {number} nside - healpix level
 * @prop {Array.<DevicePt>} devPtCorners - the target corners of the tile in device coordinates
 * @prop {number} dx - x offset into image
 * @prop {number} dy - y offset into image
 */


/**
 *
 * @param {WebPlot} plot
 * @param viewDim
 * @param {number} norder
 * @param {number} fov
 * @param {WorldPt} centerWp
 * @return {Array.<HiPSDeviceTileData>}
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
    const drawer= makeHipsRenderer(screenRenderParams, tilesToLoad.length, !plot.asOverlay,
                                   tileProcessInfo, screenRenderEnabled);
    
    if (useAllSky) {
        const allSkyURL= makeHiPSAllSkyUrlFromPlot(plot);
        let cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
        if (cachedAllSkyImage) {
            drawer.drawAllSky(norder, cachedAllSkyImage, tilesToLoad);
        }
        else {
            loadImage(makeHiPSAllSkyUrlFromPlot(plot))
                .then( (allSkyImage) =>
                {
                    addAllSkyCachedImage(allSkyURL, allSkyImage);
                    cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
                    drawer.drawAllSky(norder, cachedAllSkyImage, tilesToLoad);
                })
                .catch( () => {
                    // this should not happen - there is no all sky image so we are looking for the full tiles.
                    // tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
                    drawer.drawAllTilesAsync(tilesToLoad,plot);
                });
        }
    }
    else {
        switch (drawTiming) {
            case DrawTiming.IMMEDIATE:
                drawer.drawAllTilesImmediate(tilesToLoad,plot);
                break;
            case DrawTiming.ASYNC:
                drawer.drawAllTilesAsync(tilesToLoad,plot);
                break;
            case DrawTiming.DELAY:
                setTimeout( () => drawer.drawAllTilesAsync(tilesToLoad,plot), 250);
                break;
        }
    }
    return drawer.abort; // this abort function will any async promise calls stop before they draw

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
