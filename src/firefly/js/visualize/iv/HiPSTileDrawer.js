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
import {isHiPS} from '../WebPlot';
import {getHipsColorOps} from './HipsColor.js';
import {makeDevicePt, makeWorldPt} from '../Point.js';
import {getColorModel} from '../rawData/rawAlgorithm/ColorTable.js';


const noOp= { drawerTile : () => undefined, abort : () => undefined };

const colorId = (plot) => {
    const id= Number(plot.plotState.getColorTableId());
    return isNaN(id) ? -1 : id;
};

export const DrawTiming= new Enum(['IMMEDIATE','ASYNC', 'DELAY']);

/**
 * Return a function that should be called on every render to draw the image
 * @param targetCanvas
 * @param GPU
 * @return {*}
 */
export function createHiPSDrawer(targetCanvas, GPU) {
    if (!targetCanvas) return () => undefined;
    let abortLastDraw= null;
    let lastDrawNorder= 0;
    let lastFov;
    const hipsColorOps= getHipsColorOps(GPU);


    return (plot, opacity,plotView, tileProcessInfo= {shouldProcess:false}) => {
        if (!isHiPS(plot)) return;
        abortLastDraw && abortLastDraw(); // stop any incomplete drawing

        const {viewDim}= plotView;
        let transitionNorder;

        const {norder, useAllSky}= getHiPSNorderlevel(plot, true);
        const {fov,centerWp}= getPointMaxSide(plot,viewDim);
        const tilesToLoad= findCellOnScreen(plot,viewDim,norder, fov, centerWp);
        let drawTiming= DrawTiming.ASYNC;
        const {bias,contrast}= plot.rawData.bandData[0];
        const colorTableId= colorId(plot);
        let doDrawTransitionalImage= false;

        if (useAllSky) {
            drawTiming= DrawTiming.IMMEDIATE;
        }
        else if (tilesToLoad.every( (tile)=> findTileCachedImage(createImageUrl(plot,tile))) ) { // any case with all the tiles
            if (colorTableId>=1) {
                if (tilesToLoad.every( (tile)=> findTileCachedImage(createImageUrl(plot,tile),colorTableId,bias,contrast))) {
                    drawTiming= DrawTiming.IMMEDIATE;
                }
                else {
                    drawTiming= DrawTiming.ASYNC;
                }
            }
            else {
                drawTiming= DrawTiming.IMMEDIATE;
            }
        }
        else if (fovEqual(fov, lastFov)) { // scroll case
            doDrawTransitionalImage= true;
            drawTiming= DrawTiming.ASYNC;
            transitionNorder= lastDrawNorder-1;// for scroll transitionNorder needs to be set one back
        }
        else { // zoom or resize case, in a zoom or resize case, slow down drawing, to allow to use to finish
            doDrawTransitionalImage= true;
            drawTiming= DrawTiming.DELAY;
            transitionNorder= lastDrawNorder;
        }

        if (doDrawTransitionalImage) {
            drawTransitionalImage(fov,centerWp,targetCanvas,plot, plotView,norder, transitionNorder, hipsColorOps,
                opacity, tileProcessInfo, tilesToLoad);
        }

        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,drawTiming!==DrawTiming.IMMEDIATE);
        abortLastDraw= drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, hipsColorOps, tilesToLoad, useAllSky,
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
 * @param hipsColorOps
 * @param opacity
 * @param tileProcessInfo
 * @param finalTileToLoad
 */
function drawTransitionalImage(fov, centerWp, targetCanvas, plot, plotView,
                               norder, transitionNorder, hipsColorOps, opacity, tileProcessInfo, finalTileToLoad) {
    const {viewDim}= plotView;
    let tilesToLoad;
    const {bias,contrast}= plot.rawData.bandData[0];
    const colorTableId= colorId(plot);
    if (norder<=3) { // norder is always 3, need to fix this if
          // draw the level 2 all sky and then draw on top what ever part of the full resolution level 3 tiles that are in cache
        const tilesToLoad2= findCellOnScreen(plot,viewDim,2, fov, centerWp);
        const tilesToLoad3= findCellOnScreen(plot,viewDim,3, fov, centerWp);
        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,false);
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, 2, hipsColorOps, tilesToLoad2, true,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE, false);
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, 3, hipsColorOps, tilesToLoad3, false,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
    }
    else {
        let lookMore= true;
        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,false);
        // find some lower resolution norder to draw, as long as there is at least one tile available in cache, us it.
        for( let testNorder= transitionNorder; (testNorder>=3 && lookMore); testNorder--) {
            tilesToLoad= findCellOnScreen(plot,viewDim,testNorder, fov, centerWp);
            const hasSomeTiles= tilesToLoad.some( (tile)=> findTileCachedImage(createImageUrl(plot,tile),colorTableId,bias,contrast));
            if (hasSomeTiles || testNorder===3) { // if there are tiles or we need to do the allsky
                drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, testNorder, hipsColorOps, tilesToLoad, testNorder===3,
                    opacity, tileProcessInfo, DrawTiming.IMMEDIATE, false);
                lookMore= false;
                // console.log(`draw transi: transitionNorder: ${transitionNorder}, testNorder: ${testNorder}`);
            }
        }
        // draw what ever part of the nornder tiles that are in cache on top
        drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, hipsColorOps, finalTileToLoad, false,
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
    let badCnt;
    let devPtCorners;
    const centerDevPt= cc.getDeviceCoords(centerWp);
    // this function is performance sensitive, use for loops instead of map and filter
    for(let i= 0; (i<cells.length); i++) {
        devPtCorners= [];
        badCnt=0;
        for(let j=0; (j<cells[i].wpCorners.length); j++)  {
            devPtCorners[j]= cc.getDeviceCoords(cells[i].wpCorners[j]);
            if (!devPtCorners[j]) {
                badCnt++;
                if (badCnt>2) break;
            }
        }
        if (badCnt===1) devPtCorners= shim1DevPtCorner(devPtCorners,centerDevPt, viewDim);
        else if (badCnt===2) devPtCorners= shim2DevPtCorner(devPtCorners,centerDevPt, viewDim);
        if (isQuadTileOnScreen(devPtCorners, viewDim)) {
            retCells.push({devPtCorners, tileNumber:cells[i].ipix, dx:0, dy:0, nside: norder});
        }
    }
    return retCells;
}


function shim1DevPtCorner(devPtCorners,centerDevPt, viewDim) {
    const {width,height}= viewDim;
    const cX= centerDevPt.x;
    const avgY= devPtCorners.reduce( (sum, pt) => pt ? sum+pt.y : sum,0)/(devPtCorners.length-1);
    const maxX= devPtCorners.reduce( (max, pt) => pt ? Math.abs(cX-pt.x) > Math.abs(cX-max) ? pt.x : max : max ,cX);
    let y= avgY;
    if (y<50) y=1;
    else if (y>height-50) y=height;
    let x= maxX;
    if (x<50) x=1;
    else if (x>width-50) x=width;
    return devPtCorners.map( (pt) => pt ? pt : makeDevicePt(x,y));
}

function shim2DevPtCorner(devPtCorners,centerDevPt, viewDim) {
    const {width,height}= viewDim;
    const cY= centerDevPt.y;

    const avgX= devPtCorners.reduce( (sum, pt) => pt ? sum+pt.x : sum,0)/(devPtCorners.length-2);
    const maxY= devPtCorners.reduce( (max, pt) => pt ? Math.abs(cY-pt.y) > Math.abs(cY-max) ? pt.y : max : max ,cY);

    let y= maxY;
    if (y<50) y=1;
    else if (y>height-50) y=height;
    let xToUse= avgX-10;
    return devPtCorners.map( (pt) => {
        if (pt) return pt;
        let x= xToUse;
        if (x<50) x=1;
        else if (x>width-50) x=width;
        const retPt= makeDevicePt(xToUse,y);
        xToUse+=20;
        return retPt;
    });
}


// ---------- does not work
// function shimCorners(cc,wpCorners, centerWp, devPtCorners) {
//     const aDiff= (c,a) => Math.min( Math.abs(c-a), (360 - (c-a)) % 360);
//     const cenRa= centerWp.x;
//     return devPtCorners.map( (pt,idx) => {
//         if (pt) return pt;
//         const wp= wpCorners[idx];
//
//         const ra= wp.x;
//         const diff= aDiff(cenRa,ra);
//         const adjust= diff- 90.1;
//         const newRa = Math.abs(cenRa-ra) > (360 - (cenRa-ra)) % 360 ? ra+adjust : ra-adjust;
//         const newWp= makeWorldPt(newRa,wp.y, wp.cSys);
//         const centerDevPt= cc.getDeviceCoords(newWp);
//         console.log(wp, centerDevPt, newWp);
//         return centerDevPt;
//     });
// }



/**
 *
 * @param targetCanvas
 * @param offscreenCanvas
 * @param plot
 * @param plotView
 * @param norder
 * @param hipsColorOps
 * @param tilesToLoad
 * @param useAllSky
 * @param opacity
 * @param tileProcessInfo
 * @param drawTiming
 * @param screenRenderEnabled
 */
function drawDisplay(targetCanvas, offscreenCanvas, plot, plotView, norder, hipsColorOps, tilesToLoad, useAllSky, opacity, tileProcessInfo,
                     drawTiming= DrawTiming.ASYNC, screenRenderEnabled= true) {
    const {viewDim}= plotView;
    const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot
    const boundingBox= computeBounding(rootPlot,viewDim.width,viewDim.height);
    const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
    const offsetY= boundingBox.y>0 ? boundingBox.y : 0;

    if (!targetCanvas) return noOp;
    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};
    const drawer= makeHipsRenderer(screenRenderParams, tilesToLoad.length, !plot.asOverlay,
                                   tileProcessInfo, screenRenderEnabled, hipsColorOps);
    
    if (useAllSky) {
        const allSkyURL= makeHiPSAllSkyUrlFromPlot(plot);
        let cachedAllSkyImage;
        //=======================
        //======================= color experiment
        //=======================
        const ctId= colorId(plot);
        const {bias,contrast}= plot.rawData.bandData[0];
        cachedAllSkyImage= findAllSkyCachedImage(allSkyURL,ctId,bias,contrast);
        if (ctId!==-1 && !cachedAllSkyImage) {
            cachedAllSkyImage= findAllSkyCachedImage(allSkyURL);
            if (cachedAllSkyImage) {
                const coloredImage= hipsColorOps.changeHiPSColor(cachedAllSkyImage.order3,ctId,bias,contrast);
                addAllSkyCachedImage(allSkyURL, coloredImage,ctId,bias,contrast);
                cachedAllSkyImage= findAllSkyCachedImage(allSkyURL,ctId,bias,contrast);
            }
        }
        //=======================
        //======================= end color experiment
        //=======================
        if (cachedAllSkyImage) {
            drawer.drawAllSky(norder, cachedAllSkyImage, tilesToLoad);
        }
        else {
            loadImage(makeHiPSAllSkyUrlFromPlot(plot))
                .then( (allSkyImage) =>
                {
                    addAllSkyCachedImage(allSkyURL, allSkyImage);
                    if (ctId!==-1) {
                        allSkyImage= hipsColorOps.changeHiPSColor(allSkyImage,ctId,bias,contrast);
                        addAllSkyCachedImage(allSkyURL, allSkyImage,ctId,bias,contrast);
                    }
                    cachedAllSkyImage= findAllSkyCachedImage(allSkyURL,ctId,bias,contrast);
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



    const colorTableId= colorId(plot);
    let backgroundColor= [0,0,0];
    if (colorTableId>-1) {
        const cm= getColorModel(colorTableId);
        backgroundColor=[Math.trunc(cm[0]*255),Math.trunc(cm[1]*255),Math.trunc(cm[2]*255)];
    }

    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);
    const offscreenCtx = offscreenCanvas.getContext('2d');

    const {viewDim:{width,height}}=  plotView;

    const [r,g,b]= backgroundColor;
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
    offscreenCtx.fillStyle = overlayTransparent ? 'rgba(0,0,0,0)'  : `rgba(${r},${g},${b},1)`;
    offscreenCtx.fillRect(0, 0, width, height);
    return offscreenCanvas;
}
