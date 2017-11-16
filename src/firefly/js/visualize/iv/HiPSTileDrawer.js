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
    let lastNorder= 0;
    let lastUsedAllSky= false;


    return (plot, opacity,plotView, tileProcessInfo= {shouldProcess:false}) => {
        if (abortLastDraw) abortLastDraw(); // stop any incomplete drawing

        const {viewDim}= plotView;

        const {norder, useAllSky}= getBestHiPSlevel(plot, true);
        const {fov,centerWp}= getPointMaxSide(plot,viewDim);
        const tilesToLoad= findCellOnScreen(plot,viewDim,norder, fov, centerWp);
        let drawTiming= DrawTiming.ASYNC;


        if (useAllSky || tilesToLoad.every( (tile)=> findTileCachedImage(createImageUrl(plot,tile))) ) {
            drawTiming= DrawTiming.IMMEDIATE;
        }

        if (drawTiming===DrawTiming.ASYNC && showZoomChangeDisplay(useAllSky,lastUsedAllSky,norder,lastNorder)) {
            drawTransitionalImage(fov,centerWp,targetCanvas,plot, plotView,norder, lastNorder, opacity, tileProcessInfo);
            drawTiming= DrawTiming.DELAY;
        }

        abortLastDraw= drawDisplay(targetCanvas, plot, plotView, norder, tilesToLoad, useAllSky,
            opacity, tileProcessInfo, drawTiming);
        lastNorder= norder;
        lastUsedAllSky= useAllSky;
    };
}


/**
 *
 * @param fov
 * @param centerWp
 * @param targetCanvas
 * @param plot
 * @param plotView
 * @param norder
 * @param lastNorder
 * @param opacity
 * @param tileProcessInfo
 */
function drawTransitionalImage(fov, centerWp, targetCanvas, plot, plotView, norder, lastNorder, opacity, tileProcessInfo) {
    const {viewDim}= plotView;
    let tilesToLoad;
    if (norder<=3) {
        tilesToLoad= findCellOnScreen(plot,viewDim,3, fov, centerWp);
        drawDisplay(targetCanvas, plot, plotView, lastNorder, tilesToLoad, true,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
    }
    else if (norder > lastNorder) {
        let lookMore= true;
        for( let testNo= lastNorder; (testNo>=3 && lookMore); testNo--) {
            tilesToLoad= findCellOnScreen(plot,viewDim,testNo, fov, centerWp);
            if (tilesToLoad.some( (tile)=> findTileCachedImage(createImageUrl(plot,tile)))) {
                drawDisplay(targetCanvas, plot, plotView, testNo, tilesToLoad, false,
                    opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
                lookMore= false;
            }
            if (lookMore && testNo===3){
                drawDisplay(targetCanvas, plot, plotView, testNo, tilesToLoad, true,
                    opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
                lookMore= false;
            }
        }
    }
    else {
        tilesToLoad= findCellOnScreen(plot,viewDim,lastNorder, fov, centerWp);
        drawDisplay(targetCanvas, plot, plotView, lastNorder, tilesToLoad, false,
            opacity, tileProcessInfo, DrawTiming.IMMEDIATE);
    }

}





function showZoomChangeDisplay(useAlSky, lastUsedAllSky, norder, lastNorder) {
    if (!lastNorder || useAlSky) return false;
    if (useAlSky!==lastUsedAllSky) return true;
    if (lastNorder!==norder) return true;

}

function findCellOnScreen(plot, viewDim, norder, fov,centerWp) {
    const cells= getVisibleHiPSCells(norder,centerWp, fov, plot.dataCoordSys);
    const cc= CysConverter.make(plot);
    const retval= cells
        .map( (c) => {
            let all= true;
            const devPtCorners= c.wpCorners.map( (corner) => {
                if (!all) {
                    return false;
                }
                const devCoord= cc.getDeviceCoords(corner);
                if (!devCoord) all= false;
                return devCoord;
            });
            if (!all) return null;
            if (!isQuadTileOnScreen(devPtCorners, viewDim)) return null;
            return {devPtCorners, tileNumber:c.ipix, dx:0, dy:0, nside: norder};
        })
        .filter( (c) => c);
    return retval;
}


/**
 *
 * @param targetCanvas
 * @param plot
 * @param plotView
 * @param norder
 * @param tilesToLoad
 * @param useAllSky
 * @param opacity
 * @param tileProcessInfo
 * @param drawTiming
 */
function drawDisplay(targetCanvas, plot, plotView, norder, tilesToLoad, useAllSky, opacity, tileProcessInfo,
                     drawTiming= DrawTiming.ASYNC) {
    const {viewDim}= plotView;
    const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot
    const boundingBox= computeBounding(rootPlot,viewDim.width,viewDim.height);
    const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
    const offsetY= boundingBox.y>0 ? boundingBox.y : 0;

    const drawer= makeHipsDrawer(plotView, plot, targetCanvas, tilesToLoad.length,
        offsetX,offsetY, opacity, tileProcessInfo);

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
 * return a object with two functions drawTile and abort
 * @param {PlotView} plotView
 * @param {WebPlot} plot
 * @param {Object} targetCanvas
 * @param {number} totalCnt
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} opacity
 * @param {Object} tileProcessInfo
 * @return {function(*, *)}
 */
function makeHipsDrawer(plotView, plot, targetCanvas, totalCnt,
                        offsetX,offsetY, opacity, tileProcessInfo) {

    if (!targetCanvas) return noOp;

    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);
    const offscreenCtx = offscreenCanvas.getContext('2d');

    const {viewDim:{width,height}}=  plotView;

    offscreenCtx.fillStyle = 'rgba(227,227,227,1)';
    offscreenCtx.fillRect(0, 0, width, height);


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
    offscreenCtx.fillStyle = 'rgba(0,0,0,1)';
    offscreenCtx.fillRect(0, 0, width, height);

    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};

    return makeHipsDrawTileObj(screenRenderParams, totalCnt, !plot.asOverlay, tileProcessInfo);

}

function makeHipsDrawTileObj(screenRenderParams, totalCnt, isBaseImage, tileProcessInfo) {

    let renderedCnt=0;
    let abortRender= false;
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


                if (renderedCnt === totalCnt) {
                    renderComplete= true;
                    renderToScreen(screenRenderParams);
                }
            }).catch(() => {
                addTileCachedImage(src, null, true);
                renderedCnt++;
                if (abortRender) return;
                drawEmptyTile(offscreenCtx, tile);
                if (renderedCnt === totalCnt) {
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
            if (renderedCnt === totalCnt) {
                renderComplete= true;
                renderToScreen(screenRenderParams);
            }
        },

        abort()  {
            abortRender = true;
            if (isBaseImage && !renderComplete && renderedCnt>0) renderToScreen(screenRenderParams);
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

