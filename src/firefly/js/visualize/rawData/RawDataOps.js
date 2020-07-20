import {isArray, isArrayBuffer, uniqueId} from 'lodash';
import {allBandAry, Band} from '../Band.js';
import {contains, intersects} from '../VisUtil.js';
import {isThreeColor, primePlot} from '../PlotViewUtil.js';
import {addRawDataToCache, getEntry} from './RawDataCache.js';
import {getCmdSrvNoZipURL, getRootURL, isGPUAvailableInWorker, isImageBitmap} from '../../util/WebUtil.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {RawDataThreadActions} from '../../threadWorker/WorkerThreadActions.js';
import {PlotState} from '../PlotState.js';
import {getNextWorkerKey, postToWorker} from '../../threadWorker/WorkerAccess.js';
import {TILE_SIZE} from './RawDataConst.js';
import {getColorModel} from './rawAlgorithm/ColorTable.js';
import {getGPUOps} from './RawImageTilesGPU.js';
import {getGpuJs} from './GpuJsConfig.js';




function createTileFromImageData(buffer, width,height) {
    const c = document.createElement('canvas');
    c.width = width;
    c.height = height;
    if (isImageBitmap(buffer)) {
        c.getContext('2d').drawImage(buffer,0,0);
    }
    else if (buffer instanceof HTMLCanvasElement) {
        return buffer;
    }
    else {
        c.getContext('2d').putImageData(new ImageData(new Uint8ClampedArray(buffer),width,height),0,0);
    }
    return c;
}



function* rawTileGenerator(rawTileDataGroup, colorTableId, GPU, force) {
    const {rawTileDataAry}= rawTileDataGroup;
    const newRawTileDataAry= [];
    for(let i=0; (i<rawTileDataAry.length);i++) {
        const inData= rawTileDataAry[i];
        const {pixelData3C, pixelDataStandard,workerTmpTile, width,height, rawImageTile }= rawTileDataAry[i];
        let tile= rawImageTile;

        if (!rawImageTile || force) {
            if (isArrayBuffer(pixelDataStandard) || pixelData3C?.some( (a) => isArrayBuffer(a)) ) {
                tile= getGPUOps(GPU).createTileWithGPU(rawTileDataAry[i],getColorModel(colorTableId),isArray(pixelData3C));
            }
            else {
                tile= createTileFromImageData(workerTmpTile, width,height);
            }
        }
        newRawTileDataAry[i]= {...inData, workerTmpTile: undefined, rawImageTile:tile};
        if (i<rawTileDataAry.length-1) yield;
    }
    return {...rawTileDataGroup, rawTileDataAry:newRawTileDataAry, colorTableId};
}


async function populateTilesAsync(rawTileDataGroup, colorTableId, force) {
    const chunkSize= 5;
    const GPU= await getGpuJs();
    const gen= rawTileGenerator(rawTileDataGroup,colorTableId, GPU, force);
    return new Promise((resolve, reject) => {
        const id= setInterval( () => {

            let result= {done:false};
            for(let i=0; (i<chunkSize && !result.done); i++) {
                result= gen.next();
                if (!result.done) return;
            }
            clearInterval(id);
            result.value ? resolve(result.value) : reject();
        },0);
    });
}

// function populateTiles(rawTileDataGroup, colorTableId, force) {
//     const gen= rawTileGenerator(rawTileDataGroup,colorTableId, force);
//     let result;
//     for(result= gen.next();(!result.done);result= gen.next());
//     return result.value;
// }
//

/**
 *
 * @param plot
 * @param requiresTransparency
 * @param newZoomFactor
 * @return {Array.<{x:number,y:number,width:number,height:number,local:boolean}>}
 */
const getTileDefListFullScreen = (plot, requiresTransparency, newZoomFactor) =>
    (newZoomFactor<1) ?
        getLocalScreenTileDefList(plot,requiresTransparency, newZoomFactor) :
        [{x:0,y:0,
            width: Math.trunc(plot.dataWidth*newZoomFactor),
            height: Math.trunc(plot.dataHeight*newZoomFactor),local:true}];



const makeTileDefKey= (x,y,width,height) => `x:${x}-y:${y}-w:${width}-h:${height}---${uniqueId('tileid-')}`;

/**
 *
 * @param plot
 * @param [requiresTransparency]
 * @param [newZoomFactor]
 * @return {Array.<{x:number,y:number,width:number,height:number,local:boolean}>}
 */
function getLocalScreenTileDefList(plot, requiresTransparency=false, newZoomFactor= undefined) {
    const zf= newZoomFactor ?? plot.zoomFactor;
    const defTileSize= findScreenTileSize(zf);
    const {dataWidth,dataHeight}= plot;
    let width= defTileSize;
    let height;
    const screenWidth= Math.trunc(dataWidth*zf);
    const screenHeight=Math.trunc(dataHeight*zf);
    const retList= [];
    for(let x= 0; (x<screenWidth);  x+=width) {
        for(let y= 0; (y<screenHeight);  y+=height) {
            width= Math.trunc(((x+defTileSize) > screenWidth-defTileSize) ? screenWidth - x : defTileSize);
            height= Math.trunc(((y+defTileSize) > screenHeight-defTileSize) ? screenHeight - y : defTileSize);
            retList.push({x,y,width, height, local:true, key:makeTileDefKey(x,y,width,height)});
        }
    }
    return retList;
}


function findScreenTileSize(zfact) {
    if (zfact<1) return 500;
    const trySizes= [512,640,500,630,748,760,494,600,700,420,800,825,650];
    const intZoomLevel= Math.trunc(zfact);
    const foundSize= trySizes.find( (size) => ((size % intZoomLevel)===0));
    if (foundSize) return foundSize;

    if (intZoomLevel < 21) return intZoomLevel*35;
    else if (intZoomLevel < 31) return intZoomLevel*25;
    else if (intZoomLevel < 41) return intZoomLevel*15;
    else if (intZoomLevel < 51) return intZoomLevel*12;
    else return intZoomLevel*10;
}


export function getLocalScreenTile(plot, tileDef, overrideZoomFactor) {
    const {x,y,width,height}= tileDef;
    const {zoomFactor}= plot;
    const entry= getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {rawTileDataAry}= entry.rawTileDataGroup;

    // const {}= plot;
    const screenCanvasTile = document.createElement('canvas');
    screenCanvasTile.width = width;
    screenCanvasTile.height = height;
    const ctx= screenCanvasTile.getContext('2d');
    ctx.imageSmoothingEnabled = false;
    const zf= overrideZoomFactor ?? zoomFactor;
    const step= Math.trunc(TILE_SIZE*zf);
    let id, drawX,drawY;
    const len= rawTileDataAry.length;
    for(let i=0; (i<len);i++) { // use for loop for optimization
        if (needsTile(x/zf,y/zf,width/zf,height/zf,rawTileDataAry[i])) {
            id= rawTileDataAry[i];
            drawX= Math.trunc( ((id.x/TILE_SIZE)*step) - x);
            drawY= Math.trunc( ((id.y/TILE_SIZE)*step) - y);
            ctx.drawImage(id.rawImageTile,0,0,id.width,id.height,drawX,drawY, id.width*zf, id.height*zf);
        }
    }

    return screenCanvasTile;
}



export function drawScreenTileToMainCanvas(plot, tileDef, mainCanvas, toX,toY,toW,toH, overrideZoomFactor) {
    const {x,y,width,height}= tileDef;
    const {zoomFactor}= plot;
    const entry= getEntry(plot.plotImageId);
    if (!entry?.rawTileDataGroup) return;
    const {rawTileDataAry}= entry.rawTileDataGroup;

    const ctx= mainCanvas.getContext('2d');
    ctx.save();
    ctx.translate(toX,toY);
    const path= new Path2D();
    path.rect(0,0,toW,toH);
    ctx.clip(path);
    ctx.imageSmoothingEnabled = false;

    const zf= overrideZoomFactor ?? zoomFactor;
    const step= Math.trunc(TILE_SIZE*zf);
    let id, drawX,drawY;
    const len= rawTileDataAry.length;
    for(let i=0; (i<len);i++) { // use for loop for optimization
        if (needsTile(x/zf, y/zf, width/zf, height/zf, rawTileDataAry[i])) {
            id= rawTileDataAry[i];
            drawX= Math.trunc( ((id.x/TILE_SIZE)*step) - x);
            drawY= Math.trunc( ((id.y/TILE_SIZE)*step) - y);
            ctx.drawImage(id.rawImageTile,0,0,id.width,id.height,drawX,drawY, id.width*zf, id.height*zf);
        }
    }
    ctx.restore();
}


function makeThumbnailCanvas(plot) {
    const tSize= 70;
    const tZoomLevel= tSize /Math.max( plot.dataWidth, plot.dataHeight);
    const result= getLocalScreenTileAtZoom(plot,0,0,tSize,tSize,tZoomLevel);
    return result;
}

export function getLocalScreenTileAtZoom(plot, x,y,width,height,zoomLevel) {
    const tile= {x, y, width, height};
    return getLocalScreenTile(plot,tile,zoomLevel);
}




/**
 *
 * @param x
 * @param y
 * @param width
 * @param height
 * @param id
 * @return {boolean}
 */
function needsTile(x, y, width, height, {x:idX,y:idY,width:idWidth,height:idHeight}) {
    return  ( contains(x,y,width,height, idX, idY) ||
              contains(idX, idY, idWidth, idHeight, x,y) ||
              intersects(x,y,width,height, idX, idY, idWidth, idHeight));
}


/**
 * color change needs to do the following
 *   - confirm the stretch data exist.
 *   - clear the raw data tiles
 *   - set the color in the plot state
 * @param plot
 * @param colorTableId
 * @return {Object}
 */
export async function changeLocalRawDataColor(plot, colorTableId) {
    // const cc= uniqueId('C');
    // console.time(cc+'--colorchange');
    const entry = getEntry(plot.plotImageId);
    if (!entry || isThreeColor(plot)) return;

    let plotStateSerialized;
    let rawTileDataGroup;
    if (isGPUAvailableInWorker()) {
        const colorResult= await postToWorker(makeColorAction(plot,colorTableId,entry.workerKey));
        plotStateSerialized = colorResult.plotStateSerialized;
        rawTileDataGroup= colorResult.rawTileDataGroup;
    }
    else {
        const newPlotState = plot.plotState.copy();
        newPlotState.colorTableId = colorTableId;
        plotStateSerialized= newPlotState.toJson(false);
        rawTileDataGroup= entry.rawTileDataGroup;
    }
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, colorTableId, true);
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    const localScreenTileDefList = getLocalScreenTileDefList(plot);
    // console.timeEnd(cc+'--colorchange');
    return {plotState:PlotState.parse(plotStateSerialized), localScreenTileDefList};
}



/**
 * zoom change needs to
 *   - confirm the stretch data exist.
 *   - recompute the localScreenTileDefList
 *   - change zoom factor in plot state
 * @param plot
 * @param zoomFactor
 * @param isFullScreen
 * @return {Object}
 */
export function changeLocalRawDataZoom(plot, zoomFactor, isFullScreen) {
    const entry = getEntry(plot.plotImageId);
    if (!entry) return;
    const localScreenTileDefList = isFullScreen ? getTileDefListFullScreen(plot, false, zoomFactor) :
        getLocalScreenTileDefList(plot, false, zoomFactor);
    const newPlotState = plot.plotState.copy();
    newPlotState.zoomLevel = zoomFactor;
    return {localScreenTileDefList, plotState: newPlotState};
}


/**
 * @param {WebPlot} plot
 * @param {ImagePt} ipt
 * @param {Band} band
 * @param {String} workerKey
 * @return {Promise.<Number|undefined>}
 */
export async function getFluxDirect(plot, ipt, band = Band.NO_BAND, workerKey) {
    const entry = getEntry(plot.plotImageId);
   const action= {
        type: RawDataThreadActions.GET_FLUX,
       workerKey: entry.workerKey,
       payload: {
            plotImageId: plot.plotImageId,
            iptSerialized: ipt.toString(),
            plotStateSerialized: plot.plotState.toJson(false),
            band,
        }};
    const fluxResult= await postToWorker(action);
    return fluxResult.value;
}


export function hasLocalRawDataInStore(plot) {
    const entry = getEntry(plot?.plotImageId);
    if (!entry) return false;

    if (isThreeColor(plot)) {
        const {plotState} = plot;
        return allBandAry.every((band) => {
            if (!plotState.isBandUsed(band)) return true;
            return Boolean(entry[band.key]);
        });
    } else {
        return Boolean(entry[Band.NO_BAND.key]);
    }
}




function makeColorAction(plot, colorTableId, workerKey) {
    const {plotImageId, plotState} = plot;
    return {
        type: RawDataThreadActions.COLOR,
        workerKey,
        payload: {
            plotImageId,
            colorTableId,
            plotStateSerialized: plotState.toJson(false),
            threeColor: isThreeColor(plot),
            rootUrl: getRootURL()
        }
    };
}



function makeLoadAction(plot, band, workerKey) {
    const {plotImageId, plotState} = plot;
    const b= plot.plotState.firstBand();
    const {processHeader} = plot.rawData.bandData[b.value];
    const cleanProcessHeader = {...processHeader, imageCoordSys: processHeader.imageCoordSys.toString()};
    return {
        type: RawDataThreadActions.FETCH_DATA,
        workerKey,
        payload: {
            plotImageId,
            plotStateSerialized: plotState.toJson(false),
            band,
            processHeader: cleanProcessHeader ,
            cmdSrvUrl: getCmdSrvNoZipURL(),
            rootUrl: getRootURL()
        }
    };
}

function makeStretchAction(plot, band, workerKey, rvAry) {
    const {plotImageId, plotState, dataWidth, dataHeight} = plot;
    const b= plot.plotState.firstBand();
    const {datamin, datamax, processHeader} = plot.rawData.bandData[b.value];
    const cleanProcessHeader = {...processHeader, imageCoordSys: processHeader.imageCoordSys.toString()};
    const threeColor= isThreeColor(plot);
    let rvStrAry;
    if (rvAry) {
        rvStrAry = threeColor ?
            allBandAry.map((b) => plotState.isBandUsed(b) ? rvAry[b.value]?.toString() : undefined) :
            [rvAry[0].toString()];
    }
    else {
        rvStrAry = threeColor ?
            allBandAry.map((b) => plotState.isBandUsed(b) ? plotState.getRangeValues(b).toString() : undefined) :
            [plotState.getRangeValues().toString()];
    }
    return {
        type: RawDataThreadActions.STRETCH,
        workerKey,
        payload: {
            plotImageId,
            plotStateSerialized: plotState.toJson(false),
            dataWidth,
            dataHeight,
            datamin,
            datamax,
            band,
            rvStrAry,
            processHeader: cleanProcessHeader,
            threeColor,
            rootUrl: getRootURL()
        }
    };
}


export function loadRawData(plotOrAry, dispatcher) {
    const plotAry= isArray(plotOrAry) ? plotOrAry : [plotOrAry];

    plotAry.forEach(  (p) => {
        const workerKey= getNextWorkerKey();
        if (isThreeColor(p)) {
            load3ColorRawData(p,dispatcher, workerKey);
        } else {
            loadStandardRawData(p,dispatcher, workerKey);
        }
    });
}

export async function stretchRawData(plot, rvAry) {

    let stretchResult;
    const entry = getEntry(plot.plotImageId);
    stretchResult= await postToWorker(makeStretchAction(plot,Band.NO_BAND, entry.workerKey,rvAry));
    const {rawTileDataGroup, plotStateSerialized} = stretchResult;
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, plot.plotState.getColorTableId());
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    const localScreenTileDefList = getLocalScreenTileDefList(plot);
    return {plotState:PlotState.parse(plotStateSerialized), localScreenTileDefList};

}

async function load3ColorRawData( plot, dispatcher, workerKey) {
    const {plotState} = plot;
    const promiseAry = allBandAry.map((b) => plotState.isBandUsed(b) ? postToWorker(makeLoadAction(plot, b, workerKey)) : Promise.resolve());
    const loadResultAry= (await Promise.all(promiseAry)).map( (r) => r ? r : {success:false} );
    loadResultAry.forEach(({success}, idx) => {
        const band = Band.get(idx);
        if (band && success) {
            const {processHeader} = plot.rawData.bandData[idx];
            if (!processHeader) return;
            addRawDataToCache(plot.plotImageId, processHeader, workerKey, band);
        }
    });
    let latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    const stretchResult= await postToWorker(makeStretchAction(latestPlot, Band.NO_BAND,workerKey));
    latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    completeLoad(latestPlot,stretchResult,dispatcher);
}

async function loadStandardRawData( plot, dispatcher, workerKey) {
    const loadResult = await postToWorker(makeLoadAction(plot,Band.NO_BAND, workerKey));
    if (!loadResult.success) throw ('Error loading the data');
    let latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    const stretchResult = await postToWorker(makeStretchAction(latestPlot,Band.NO_BAND, workerKey));
    const {processHeader} = plot.rawData.bandData[0];
    latestPlot= primePlot(visRoot(),plot.plotId);
    if (!latestPlot) return;
    addRawDataToCache(plot.plotImageId, processHeader, workerKey, Band.NO_BAND);
    completeLoad(plot,stretchResult,dispatcher);
}

async function completeLoad(plot, stretchResult,dispatcher) {
    const {rawTileDataGroup, plotStateSerialized} = stretchResult;
    const newPlotState = PlotState.parse(plotStateSerialized);
    const entry = getEntry(plot.plotImageId);
    entry.rawTileDataGroup = await populateTilesAsync(rawTileDataGroup, newPlotState.getColorTableId());
    entry.thumbnailEncodedImage = makeThumbnailCanvas(plot);
    const localScreenTileDefList = getLocalScreenTileDefList(plot);
    dispatcher({type: ImagePlotCntlr.UPDATE_RAW_IMAGE_DATA,
        payload:{plotId:plot.plotId, plotImageId:plot.plotImageId, rawData:{...plot.rawData, plotState: newPlotState, localScreenTileDefList}}});

}