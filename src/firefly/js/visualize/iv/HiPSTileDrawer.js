/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isNil, isString} from 'lodash';
import {createImageUrl,initOffScreenCanvas, computeBounding, isQuadTileOnScreen, drawEmptyRecTile} from './TileDrawHelper.jsx';
import {primePlot} from '../PlotViewUtil.js';
import {retrieveAndProcessImage} from './ImageProcessor.js';
import {drawOneHiPSTile} from './HiPSSingleTileRender.js';
import {getVisibleHiPSCells, getPointMaxSide, getBestHiPSlevel, makeHiPSAllSkyUrl} from '../HiPSUtil.js';
import {loadImage} from '../../util/WebUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {findAllSkyImage, findTileImage, addAllSkyImage, addTileImage} from './HiPSTileCache.js';


const noOp= { drawerTile : () => undefined, abort : () => undefined };




/**
 * Return a function that should be called on every render to draw the image
 * @param targetCanvas
 * @return {*}
 */
export function initHiPSDrawer(targetCanvas) {
    if (!targetCanvas) return () => undefined;
    // let cachedImages= [];
    // let cachedAllSkyImage= {url:null, image: null};
    let abortLastDraw= null;


    return (plot, opacity,plotView, tileProcessInfo= {shouldProcess:false}) => {
        if (abortLastDraw) abortLastDraw();
        const scale = plot.zoomFactor;

        const rootPlot= primePlot(plotView); // bounding box should us main plot not overlay plot

        const {viewDim}= plotView;
        const boundingBox= computeBounding(rootPlot,viewDim.width,viewDim.height);
        const offsetX= boundingBox.x>0 ? boundingBox.x : 0;
        const offsetY= boundingBox.y>0 ? boundingBox.y : 0;


        let tryHipsAllsky= false;
        let nside= getBestHiPSlevel(plot, true);
        if (nside==='allsky') {
            tryHipsAllsky= true;
            nside= 3;
        }

        const cc= CysConverter.make(plot);

        const {fov,centerWp}= getPointMaxSide(plot,viewDim);

        const cells= getVisibleHiPSCells(nside,centerWp, fov, plot.dataCoordSys);

        const tilesToLoad= cells
            .map( (c) => {
                const devPtCorners= c.wpCorners.map( (corner) => cc.getDeviceCoords(corner));
                if (!isQuadTileOnScreen(devPtCorners, viewDim)) return null;
                return {devPtCorners, tileNumber:c.ipix, dx:0, dy:0, nside};
            })
            .filter( (c) => c);

        const drawer= makeHipsDrawer(plotView, plot, targetCanvas, tilesToLoad.length,
            offsetX,offsetY, scale, opacity, tileProcessInfo);

        if (tryHipsAllsky) {
            const allSkyURL= makeHiPSAllSkyUrl(plot);
            const cachedAllSkyImage= findAllSkyImage(allSkyURL);
            if (cachedAllSkyImage) {
                drawAllSky(cachedAllSkyImage, tilesToLoad, drawer);
            }
            else {
                loadImage(makeHiPSAllSkyUrl(plot))
                    .then( (allSkyImage) =>
                    {
                        addAllSkyImage(allSkyURL, allSkyImage);
                        drawAllSky(allSkyImage, tilesToLoad, drawer);
                    });
            }
        }
        else {
            tilesToLoad.forEach( (tile) => drawer.drawTile(createImageUrl(plot,tile), tile) );
        }
        abortLastDraw= drawer.abort;
    };
}

function drawAllSky(allSkyImage, tilesToLoad, drawer) {
    const width= allSkyImage.width/27;
    tilesToLoad.forEach( (tile) => {
        tile.dy= width * Math.floor(tile.tileNumber/27);
        tile.dx=  width * (tile.tileNumber - 27*Math.floor(tile.tileNumber/27));
        tile.tileSize= width;
        drawer.drawTile(null, tile,allSkyImage);
    } );
}





/**
 * return a object with two functions drawTile and abort
 * @param {PlotView} plotView
 * @param {WebPlot} plot
 * @param {Object} targetCanvas
 * @param {number} totalCnt
 * @param {number} offsetX
 * @param {number} offsetY
 * @param {number} scale
 * @param {number} opacity
 * @param {Object} tileProcessInfo
 * @return {function(*, *)}
 */
function makeHipsDrawer(plotView, plot, targetCanvas, totalCnt,
                        offsetX,offsetY, scale, opacity, tileProcessInfo) {

    if (!targetCanvas) return noOp;

    const offscreenCanvas = initOffScreenCanvas(plotView.viewDim);
    const offscreenCtx = offscreenCanvas.getContext('2d');

    const {viewDim:{width,height}}=  plotView;

    offscreenCtx.fillStyle = 'rgba(227,227,227,1)';
    offscreenCtx.fillRect(0, 0, width, height);


    const {fov, centerDevPt, devRadius}= getPointMaxSide(plot,plotView.viewDim);
    if (fov>=180) {
        const altDevRadius= plotView.viewDim.width/2 + plotView.scrollX;
        offscreenCtx.fillStyle = 'rgba(227,227,227,1)';
        offscreenCtx.fillRect(0, 0, width, height);
        offscreenCtx.save();
        offscreenCtx.beginPath();
        offscreenCtx.lineWidth= 5;
        // offscreenCtx.arc(centerDevPt.x, centerDevPt.y, devRadius, 0, 2*Math.PI, false);
        offscreenCtx.arc(centerDevPt.x, centerDevPt.y, altDevRadius, 0, 2*Math.PI, false);
        offscreenCtx.closePath();
        offscreenCtx.clip();
    }
    offscreenCtx.fillStyle = 'rgba(0,0,0,1)';
    offscreenCtx.fillRect(0, 0, width, height);

    const screenRenderParams= {plotView, plot, targetCanvas, offscreenCanvas, opacity, offsetX, offsetY};

    return makeHipsDrawTileObj(screenRenderParams, totalCnt, scale, !plot.asOverlay, tileProcessInfo);

}

function makeHipsDrawTileObj(screenRenderParams, totalCnt, scale, isBaseImage, tileProcessInfo) {

    let renderedCnt=0;
    let abortRender= false;
    let renderComplete=  false;
    const {offscreenCanvas, plotView, plot}= screenRenderParams;
    const offscreenCtx = offscreenCanvas.getContext('2d');


    return {
        drawTile(src, tile, allskyImage) {
            if (isBaseImage) drawEmptyRecTile(tile.devPtCorners,offscreenCtx,plotView);
            let inCache= false;

            //todo: look at this code and enabled caching for HiPS
            let tileData=src;
            let emptyTile= false;

            if (!allskyImage) {
                if (isString(tileData)) {
                    const cachedTile= findTileImage(src);
                    if (cachedTile) {
                        tileData=  cachedTile.image;
                        emptyTile= cachedTile.emptyTile;
                        inCache= true;
                    }
                    else {
                        tileData=  src;
                        emptyTile= false;
                    }
                }
            }
            else {
                tileData= allskyImage;
            }

            const {tileAttributes, shouldProcess, processor}= tileProcessInfo;
            const p = retrieveAndProcessImage(tileData, tileAttributes, shouldProcess, processor);
            p.then((imageData) => {
                renderedCnt++;

                if (abortRender) return;

                if (emptyTile) {
                    drawEmptyTile(offscreenCtx,tile);
                }
                else {
                    const tileSize= tile.tileSize || imageData.image.width;
                    drawOneHiPSTile(offscreenCtx, imageData.image, tile.devPtCorners,
                        tileSize, {x:tile.dx,y:tile.dy}, true, tile.nside);
                }

                if (!inCache && !allskyImage) {
                    addTileImage(src, imageData);
                }


                if (renderedCnt === totalCnt) {
                    renderComplete= true;
                    renderToScreen(screenRenderParams);
                }
            }).catch((e) => {
                addTileImage(src, null, true);
                renderedCnt++;
                drawEmptyTile(offscreenCtx, tile);
                if (renderedCnt === totalCnt) {
                    renderComplete= true;
                    renderToScreen(screenRenderParams);
                }
            });
        },

        abort()  {
            abortRender = true;
            if (isBaseImage && !renderComplete) renderToScreen(screenRenderParams);
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
    window.requestAnimationFrame(() => {
        const {plotView, targetCanvas, offscreenCanvas, opacity}= screenRenderParams;
        const ctx= targetCanvas.getContext('2d');
        ctx.save();
        ctx.globalAlpha=opacity;
        if (!isNil(plotView.scrollX) && !isNil(plotView.scrollY)) {
            ctx.drawImage(offscreenCanvas, 0,0);
        }
        ctx.restore();
    });
}

