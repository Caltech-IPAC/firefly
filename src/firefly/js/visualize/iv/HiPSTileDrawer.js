/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {createImageUrl,initOffScreenCanvas, computeBounding} from './TileDrawHelper.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {getPointMaxSide, getHiPSNorderlevel, makeHiPSAllSkyUrlFromPlot, loadImageMultiCall} from '../HiPSUtil.js';
import {findAllSkyCachedImage, findTileCachedImage, addAllSkyCachedImage} from './HiPSTileCache.js';
import {makeHipsRenderer} from './HiPSRenderer.js';
import {isHiPS, isHiPSAitoff} from '../WebPlot';
import {getHipsColorOps} from './HipsColor.js';
import {getColorModel} from '../rawData/rawAlgorithm/ColorTable.js';
import {brighter, darker} from 'firefly/util/Color.js';
import {findCellOnScreen} from 'firefly/visualize/iv/HiPSCellFinder.js';


const noOp= { drawerTile : () => undefined, abort : () => undefined };

const colorId = (plot) => plot?.colorTableId ?? -1;

/**
 * @typedef DrawTiming
 * @type {Enum}
 * @prop IMMEDIATE
 * @prop ASYNC
 * @prop DELAY
 */
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


    return (plot, opacity,plotView,colorMode) => {
        if (!isHiPS(plot)) return;
        abortLastDraw?.(); // stop any incomplete drawing

        const {viewDim}= plotView;
        let transitionNorder;

        const {norder, useAllSky, isMaxOrder, desiredNorder}= getHiPSNorderlevel(plot, true);
        const {fov,centerWp}= getPointMaxSide(plot,viewDim);
        if (!centerWp) return;
        const tilesToLoad= findCellOnScreen(plot,viewDim,norder, fov, centerWp, desiredNorder);
        let drawTiming;
        const {bias,contrast}= plot.rawData.bandData[0];
        const colorTableId= colorId(plot);
        const {blank}= plot;

        const allTilesInCache= () => tilesToLoad.every( (tile)=> findTileCachedImage(createImageUrl(plot,tile)));
        const allColoredTilesInCache= () => tilesToLoad.every( (tile)=> findTileCachedImage(createImageUrl(plot,tile),colorTableId,bias,contrast));

        if (useAllSky || blank) {
            drawTiming= DrawTiming.IMMEDIATE;
        }
        else if (allTilesInCache()) {
            drawTiming= colorTableId<1 || allColoredTilesInCache() ? DrawTiming.IMMEDIATE : DrawTiming.ASYNC;
        }
        else if (fovEqual(fov, lastFov)) { // scroll case
            drawTiming= DrawTiming.ASYNC;
            transitionNorder= lastDrawNorder-1;// for scroll transitionNorder needs to be set one back
        }
        else { // zoom or resize case, in a zoom or resize case, slow down drawing, to allow to use to finish
            drawTiming= DrawTiming.DELAY;
            transitionNorder= lastDrawNorder;
        }

        if (drawTiming!==DrawTiming.IMMEDIATE) {
            drawTransitionalImage(fov,centerWp,targetCanvas,plot, plotView,norder, transitionNorder, hipsColorOps,
                opacity, tilesToLoad, true, colorMode);
        }

        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,drawTiming!==DrawTiming.IMMEDIATE, colorMode);
        abortLastDraw= drawDisplay({targetCanvas, offscreenCanvas, plot, plotView, norder, hipsColorOps,
            tilesToLoad, useAllSky, opacity, drawTiming, isMaxOrder, desiredNorder}
        );
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
 * @param finalTileToLoad
 * @param colorMode
 */
function drawTransitionalImage(fov, centerWp, targetCanvas, plot, plotView,
                               norder, transitionNorder, hipsColorOps, opacity, finalTileToLoad, colorMode) {
    const {viewDim}= plotView;
    let tilesToLoad;
    const {bias,contrast}= plot.rawData.bandData[0];
    const colorTableId= colorId(plot);
    if (norder<=3) { // norder is always 3, need to fix this if
          // draw the level 2 all sky and then draw on top what ever part of the full resolution level 3 tiles that are in cache
        const tilesToLoad2= findCellOnScreen(plot,viewDim,2, fov, centerWp);
        const tilesToLoad3= findCellOnScreen(plot,viewDim,3, fov, centerWp);
        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,false,colorMode);
        drawDisplay({targetCanvas, offscreenCanvas, plot, plotView,
            norder:2, hipsColorOps, tilesToLoad:tilesToLoad2, useAllSky:true, drawAllSkyOnlyIfCached: true,
            opacity, drawTiming:DrawTiming.IMMEDIATE, screenRenderEnabled:false});
        drawDisplay({targetCanvas, offscreenCanvas, plot, plotView, norder:3, hipsColorOps, tilesToLoad:tilesToLoad3,
            drawAllSkyOnlyIfCached: true,
            opacity, drawTiming:DrawTiming.IMMEDIATE});
    }
    else {
        let lookMore= true;
        const offscreenCanvas = makeOffScreenCanvas(plotView,plot,false,colorMode);
        // find some lower resolution norder to draw, as long as there is at least one tile available in cache, us it.
        for( let testNorder= transitionNorder; (testNorder>=3 && lookMore); testNorder--) {
            tilesToLoad= findCellOnScreen(plot,viewDim,testNorder, fov, centerWp);
            const hasSomeTiles= tilesToLoad.some( (tile)=> findTileCachedImage(createImageUrl(plot,tile),colorTableId,bias,contrast));
            if (hasSomeTiles || testNorder===3) { // if there are tiles or we need to do the allsky
                drawDisplay({targetCanvas, offscreenCanvas, plot, plotView, norder:testNorder, hipsColorOps,
                    tilesToLoad, useAllSky:testNorder===3,
                    drawAllSkyOnlyIfCached: true,
                    opacity, drawTiming:DrawTiming.IMMEDIATE, screenRenderEnabled:false});
                lookMore= false;
            }
        }
        // draw what ever part of the nornder tiles that are in cache on top
        drawDisplay({targetCanvas, offscreenCanvas, plot, plotView, norder, hipsColorOps, tilesToLoad:finalTileToLoad,
            opacity, drawTiming:DrawTiming.IMMEDIATE});
    }
}



const fovEqual= (fov1,fov2) => Math.trunc(fov1*10000) === Math.trunc(fov2*10000);


/**
 *
 * @param p
 * @param p.targetCanvas
 * @param p.offscreenCanvas
 * @param p.plot - note this could the main plot or an overlay plot
 * @param p.plotView
 * @param p.norder
 * @param p.hipsColorOps
 * @param p.tilesToLoad
 * @param p.useAllSky
 * @param p.drawAllSkyOnlyIfCached
 * @param p.drawAllSkyOnlyIfCached
 * @param p.opacity
 * @param p.drawTiming
 * @param p.screenRenderEnabled
 * @param {boolean} [p.isMaxOrder] true if this norder is the max order
 */
function drawDisplay({targetCanvas, offscreenCanvas, plot,
                         plotView, norder, hipsColorOps,
                         tilesToLoad, useAllSky=false, opacity,
                         drawAllSkyOnlyIfCached= false,
                         drawTiming= DrawTiming.ASYNC, screenRenderEnabled= true,
                         isMaxOrder=false, desiredNorder=0} ) {
    if (!targetCanvas) return noOp;
    const {viewDim}= plotView;
    const boundingBox= computeBounding(primePlot(plotView),viewDim.width,viewDim.height);// should use main plot not overlay plot
    const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
    const offsetY= boundingBox.y>0 ? boundingBox.y : 0;

    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};
    const drawer= makeHipsRenderer(screenRenderParams, tilesToLoad.length, !plot.asOverlay,
        screenRenderEnabled, hipsColorOps, isMaxOrder, norder, desiredNorder);

    if (plot.blank) {
        drawer.renderToScreen();
        return drawer.abort; // this abort function will any async promise calls stop before they draw
    }
    
    useAllSky ?
        drawDisplayUsingAllSky(drawer, plot, hipsColorOps, tilesToLoad, drawAllSkyOnlyIfCached) :
        drawDisplayUsingTiles(drawer,plot,tilesToLoad,drawTiming);
    return drawer.abort; // this abort function will any async promise calls stop before they draw
}


/**
 * first look for an allsky image with the correct color, it not found look at see if we have the base allsky image
 * if not found return, if we have the base one then change the color and that version to the cache, then return it
 * @param allSkyURL
 * @param {number} ctId
 * @param {number} bias
 * @param {number} contrast
 * @param hipsColorOps
 * @return {HiPSAllSkyCacheInfo|undefined}
 */
function findCachedAllSkyToFitColor(allSkyURL,ctId,bias,contrast, hipsColorOps) {
    const cachedAllSkyData= findAllSkyCachedImage(allSkyURL,ctId,bias,contrast);
    if (cachedAllSkyData) return cachedAllSkyData; // found and returned
    const cachedAllSkyOriginalData= findAllSkyCachedImage(allSkyURL);  //look for the based all sky image (without color change)
    if (!cachedAllSkyOriginalData || ctId<0) return cachedAllSkyOriginalData;
       // at this point we have a base all sky image that needs to have the color changed
    const coloredAllSkyCanvas= hipsColorOps.changeHiPSColor(cachedAllSkyOriginalData.order3,ctId,bias,contrast);
    addAllSkyCachedImage(allSkyURL, coloredAllSkyCanvas,ctId,bias,contrast);
    return findAllSkyCachedImage(allSkyURL,ctId,bias,contrast);
}

async function drawDisplayUsingAllSky(drawer, plot, hipsColorOps, tilesToLoad, drawOnlyIfCached=false) {
    const allSkyURL= makeHiPSAllSkyUrlFromPlot(plot);
    const ctId= colorId(plot);
    const {bias,contrast}= plot.rawData.bandData[0];
    const findAllSkyCached= () => findCachedAllSkyToFitColor(allSkyURL,ctId,bias,contrast, hipsColorOps);


    const cachedAllSkyData= findAllSkyCached();
    if (cachedAllSkyData) {
        drawer.drawAllSky(cachedAllSkyData, tilesToLoad);
        return;
    }
    if (drawOnlyIfCached) {
        const allSkyImage= await loadImageMultiCall(allSkyURL);
        if (!findAllSkyCached()) addAllSkyCachedImage(allSkyURL, allSkyImage);
        return;
    }
    try {
        const allSkyImage= await loadImageMultiCall(allSkyURL);
        if (!findAllSkyCached()) addAllSkyCachedImage(allSkyURL, allSkyImage);
        const processedAllSkyData= findAllSkyCached();
        drawer.drawAllSky(processedAllSkyData, tilesToLoad);
    } catch (e) {// should not happen - there is no all sky image, so we are using the full tiles.
        drawer.drawAllTilesAsync(tilesToLoad,plot);
    }
}


function drawDisplayUsingTiles(drawer, plot, tilesToLoad, drawTiming) {
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


function clipForFullScreen(ctx, width, height, centerDevPt, altDevRadius, aitoff, colorMode) {
    ctx.fillStyle = colorMode==='dark' ? 'rgba(50,50,50,1)' : 'rgba(255,255,255,1)';
    ctx.fillRect(0, 0, width, height);
    ctx.save();
    ctx.beginPath();
    ctx.lineWidth= 5;
    aitoff ?
        ctx.ellipse(centerDevPt.x, centerDevPt.y, altDevRadius*.98, altDevRadius/2, 0, 0, 2*Math.PI, false) :
        ctx.arc(centerDevPt.x, centerDevPt.y, altDevRadius, 0, 2*Math.PI, false);
    ctx.closePath();
    ctx.clip();
}

/**
 *
 * @param plotView
 * @param plot
 * @param overlayTransparent
 * @param {String} colorMode - light or dark
 */
function makeOffScreenCanvas(plotView, plot, overlayTransparent, colorMode) {
    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);
    const ctx = offscreenCanvas.getContext('2d');

    const {viewDim:{width,height}}=  plotView;

    const {fov, centerDevPt}= getPointMaxSide(plot,plotView.viewDim);

    const aitoff= isHiPSAitoff(plot);
    let altDevRadius= plotView.viewDim.width/2 + plotView.scrollX - 4;
    if (aitoff) altDevRadius*= 2.92;
    if (aitoff && fov>=360) {
        clipForFullScreen(ctx, width,height, centerDevPt, altDevRadius, true, colorMode);
    }
    else if (fov>=180) {
        clipForFullScreen(ctx, width,height, centerDevPt, altDevRadius, false, colorMode);
    }

    if (overlayTransparent) {
        ctx.fillStyle = 'rgba(0,0,0,0)';
    }
    else if (plot.blank) {
        ctx.fillStyle= plot.blankColor;
        if (fov>=180 || (isHiPSAitoff(plot) && fov>=360)) {
            const brightC = brighter(plot.blankColor);
            const darkerC = darker(plot.blankColor);
            const gradient = ctx.createRadialGradient(centerDevPt.x, centerDevPt.y, 5, centerDevPt.x, centerDevPt.y, altDevRadius);
            // gradient.addColorStop(0, plot.blankColor);
            gradient.addColorStop(0, brightC);
            gradient.addColorStop(.8, darkerC);
            gradient.addColorStop(1, darker(darkerC));
            ctx.fillStyle = gradient;
        }
    }
    else if (colorId(plot)===-1) {
        ctx.fillStyle = 'rgba(0,0,01)';
        // ctx.fillStyle = 'red'; //for testing
    }
    else {
        const cm= getColorModel(colorId(plot));
        const [r,g,b]=[Math.trunc(cm[0]*255),Math.trunc(cm[1]*255),Math.trunc(cm[2]*255)];
        ctx.fillStyle = `rgba(${r},${g},${b},1)`;
    }
    ctx.fillRect(0, 0, width, height);
    return offscreenCanvas;
}
