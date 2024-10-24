/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {race,call} from 'redux-saga/effects';
import {visRoot} from '../ImagePlotCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {
    readoutRoot, makeValueReadoutItem, makePointReadoutItem,
    makeDescriptionItem, isLockByClick, STANDARD_READOUT, HIPS_STANDARD_READOUT, makeFileValueReadoutItem, STATUS_VALUE,
    TYPE_EMPTY
} from '../MouseReadoutCntlr.js';
import {callGetFileFlux} from '../../rpc/PlotServicesJson.js';
import {Band} from '../Band.js';
import {MouseState} from '../VisMouseSync.js';
import CsysConverter, {CysConverter} from '../CsysConverter.js';
import {getPixScale, getScreenPixScale, getScreenPixScaleArcSec, isImage, isHiPS, getFluxUnits} from '../WebPlot.js';
import {getPlotTilePixelAngSize} from '../HiPSUtil.js';
import {mouseUpdatePromise, fireMouseReadoutChange} from '../VisMouseSync';
import {
    primePlot, getPlotStateAry, getPlotViewById, getImageCubeIdx, getPtWavelength,
    getWavelengthParseFailReason, getWaveLengthUnits, hasPixelLevelWLInfo, hasPlaneOnlyWLInfo,
    isImageCube, wavelengthInfoParsedSuccessfully, } from '../PlotViewUtil';
import {getBixPix} from '../FitsHeaderUtil.js';


const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const PAUSE_DELAY= 30;


/**
 * Readout watcher defined the algorythm to drive the mouse readout. It does the following:
 *   - waits for a promise of a mouse event
 *   - Has two modes lockByClick, on or off
 *   - lockByClick off:
 *       - on mouse move compute and fire a mouse readout object
 *       - if mouse pauses (200 ms) and readout uses async, call the server for more data (the image pixel flux value)
 *       - if mouse still pause, fire a updated mouse readout object
 *   - lockByClick on:
 *       - on mouse click compute and fire a mouse readout object
 *       - if readout uses async, call the server for more data
 *       - if the position has not changed, fire a updated mouse readout object
 *
 */
export function* watchReadout() {

    let mouseCtx;
    let savedWP;
    yield call(mouseUpdatePromise);

    mouseCtx = yield call(mouseUpdatePromise);

    while (true) {

        let getNextWithWithAsync= false;
        const lockByClick= isLockByClick(readoutRoot());
        let {worldPt,screenPt,imagePt}= mouseCtx;
        const {plotId,mouseState, healpixPixel, norder, shiftDown}= mouseCtx;
        if (!lockByClick) savedWP= undefined;
        if (lockByClick && worldPt && !shiftDown && mouseState===MouseState.CLICK) savedWP= worldPt;

        const plotView= getPlotViewById(visRoot(), plotId);
        const plot= primePlot(plotView);
        const threeColor= plot?.plotState?.threeColor;

        if (plot && lockByClick && savedWP && shiftDown && mouseState===MouseState.CLICK) {
            worldPt= savedWP;
            const cc= CsysConverter.make(plot);
            imagePt= cc.getImageCoords(savedWP);
            screenPt= cc.getScreenCoords(imagePt);
        }


        if (isPayloadNeeded(mouseState,lockByClick)) {
            if (plot) {
                const readoutItems= makeImmediateReadout(plot, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder);
                fireMouseReadoutChange({plotId, readoutItems, threeColor, readoutType:getReadoutKey(plot)});
                getNextWithWithAsync= hasAsyncReadout(plot);
            }
        }
        else if (!lockByClick) {
            fireMouseReadoutChange({plotId, readoutItems:{}, readoutType:getReadoutKey(plot)});
        }

        if (getNextWithWithAsync) { // get the next mouse event or the flux
            mouseCtx= lockByClick  ?
                yield call(processAsyncDataImmediate,plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder) :
                yield call(processAsyncDataDelayed,plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder);
        }
        else { // get the next mouse event
            mouseCtx = yield call(mouseUpdatePromise);
        }

    }
}

function* processAsyncDataImmediate(plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder) {
    try {
        const readoutItems= yield call(makeAsyncReadout,plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder);
        if (readoutItems) {
            const plot= primePlot(plotView);
            // dispatchReadoutData({plotId:plotView.plotId,readoutItems, threeColor, readoutKey:getReadoutKey(plot)});
            fireMouseReadoutChange({plotId:plotView.plotId,readoutItems, threeColor, readoutType:getReadoutKey(plot)});
            const mouseCtx = yield call(mouseUpdatePromise);
            return mouseCtx;
        }
    }
    catch(error) {
        const mouseCtx = yield call(mouseUpdatePromise);
        return mouseCtx;
    }
    return null;
}


function* processAsyncDataDelayed(plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder) {
    const plot= primePlot(plotView);
    const mousePausedRaceWinner = yield race({ mouseCtx: call(mouseUpdatePromise), timer: call(delay, PAUSE_DELAY) });

    if (mousePausedRaceWinner.mouseCtx) return mousePausedRaceWinner.mouseCtx;

    try {
        const mouseMoveRaceWinner = yield race({
                mouseCtx: call(mouseUpdatePromise),
                readoutItems: call(makeAsyncReadout,plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder)
            });
        if (mouseMoveRaceWinner.mouseCtx) return mouseMoveRaceWinner.mouseCtx;

        if (mouseMoveRaceWinner.readoutItems) {
            fireMouseReadoutChange({plotId:plotView.plotId,readoutItems:mouseMoveRaceWinner.readoutItems,
                threeColor, readoutType:getReadoutKey(plot)});
            const mouseCtx = yield call(mouseUpdatePromise);
            return mouseCtx;
        }
    }
    catch(error) {
        console.log('flux error= just ignore');
        const mouseCtx = yield call(mouseUpdatePromise);
        return mouseCtx;
    }
}


function isPayloadNeeded(mouseState, lockByClick) {
    if (lockByClick) {
        return mouseState===MouseState.CLICK;
    }
    else {
        return mouseState!==MouseState.EXIT;
    }
}



//-------------------------------------------------------------------------------
//------- Interface Functions between the readout watcher and the factory
//-------------------------------------------------------------------------------

const getReadoutType= (plot) => readoutTypes.find( (r) => r.matches(plot));

function makeImmediateReadout(plot,worldPt,screenPt,imagePt, threeColor, healpixPixel, norder) {
    const rt= getReadoutType(plot);
    return rt && rt.createImmediateReadout(plot,worldPt,screenPt,imagePt, threeColor, healpixPixel, norder);
}

function makeAsyncReadout(plotView,worldPt,screenPt,imagePt, threeColor) {
    const rt= getReadoutType(primePlot(plotView));
    return Promise.resolve(rt && rt.createAsyncReadout(plotView,worldPt,screenPt,imagePt, threeColor));
}

function hasAsyncReadout(plot) {
    const rt= getReadoutType(plot);
    return rt && rt.hasAsyncReadout(plot);
}

function getReadoutKey(plot) {
    const rt= getReadoutType(plot);
    return rt && rt.readoutKey;
}


//-------------------------------------
//------- Factory ---------------------
//-------------------------------------

/**
 * @global
 * @public
 * @typedef {Object} MouseReadoutType
 *
 * @prop {String} readoutKey: unique key represent this readout type
 *
 * @prop {function(WebPlot:plot):boolean} matches: function to test if this readout should be used form: tableMatches(WebPlot): boolean
 *
 * @prop {function(WebPlot:plot, WorldPt:worldPt, ScreenPt:screenPt,
 * ImagePt:imagePt, Boolean:threeColor, number:healpixPixel, number:norder):Object} createImmediateReadout:
 *            function to create a readout object
 *
 * @prop {function(WebPlot:plot, WorldPt:worldPt, ScreenPt:screenPt, ImagePt:imagePt, Boolean:threeColor):Promise<Object>} createAsyncReadout:
 *            function to create a readout by calling the server to get data, returns a promise with a readout
 *
 * @prop {function(WebPlot:plot):boolean} hasAsyncReadout: function to test readout should be used make async calls: hasAsyncReadout(WebPlot): boolean
 *
 */





/**
 * @type {Array.<MouseReadoutType>}
 */
const readoutTypes= [
    {  // this first one is not used yet.
        readoutKey: 'spectral-cube',
        matches: (plot) => false,
        createImmediateReadout: () => {throw Error('not implemented');},
        createAsyncReadout: () => {throw Error('not implemented');},
        hasAsyncReadout: (plot) => true,
    },
    {
        readoutKey: STANDARD_READOUT,
        matches: (plot) => isImage(plot),
        createImmediateReadout: makeImagePlotImmediateReadout,
        createAsyncReadout: makeImagePlotAsyncReadout,
        hasAsyncReadout: (plot) => true,
    },
    {
        readoutKey: HIPS_STANDARD_READOUT,
        matches: (plot) => isHiPS(plot),
        createImmediateReadout: makeHiPSReadout,
        createAsyncReadout: () => {throw Error('HiPS should not do async');},
        hasAsyncReadout: (plot) => false,

    },
];




//-------------------------------------
//------- Standard Image Plot and routines to get flux
//-------------------------------------

function makeImagePlotImmediateReadout(plot, worldPt, screenPt, imagePt, threeColor) {
    return makeReadoutWithFlux(makeReadout(plot,worldPt,screenPt,imagePt), plot, null, 10, threeColor);
}

function makeImagePlotAsyncReadout(plotView, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder) {

    const plot= primePlot(plotView);
    const readoutItems= makeImmediateReadout(plot, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder);
    const {readoutPref}= readoutRoot();
    const radix= Number(getBixPix(plot)>0 ? readoutPref.intFluxValueRadix : readoutPref.floatFluxValueRadix);
    return doFluxCall(plotView,imagePt).then( (fluxResult) => {
        return makeReadoutWithFlux(readoutItems,primePlot(plotView), fluxResult, radix, threeColor);
    });
}

/**
 *
 * @param readout
 * @param {WebPlot|undefined} plot
 * @param fluxResult
 * @param radix
 * @param threeColor
 * @return {*}
 */
function makeReadoutWithFlux(readout, plot, fluxResult, radix, threeColor) {
    readout= clone(readout);
    const fluxData= fluxResult ? getFlux(fluxResult,plot) : undefined;
    const labels= getFluxLabels(plot);
    if (threeColor) {
        plot.plotState.getBands().forEach( (b,idx) => {
            const {valueBase10, valueBase16,type,status,unit}= fluxData?.[idx] ?? {status:STATUS_VALUE, type:TYPE_EMPTY};
            readout[b.key+'Flux']= makeFileValueReadoutItem(labels[idx], valueBase10, valueBase16,radix, status,type,unit, 6);
        });
    }
    else {
        const {valueBase10, valueBase16,type,status,unit}= fluxData?.[0] ?? {status:STATUS_VALUE, type:TYPE_EMPTY};
        readout.nobandFlux= makeFileValueReadoutItem(labels[0], valueBase10, valueBase16,radix, status,type, unit, 6);
    }
    if (fluxData) {
        const oIdx= fluxData.findIndex( (d) => d.imageOverlay);
        if (oIdx>-1) {
            const {valueBase10, valueBase16,type,status,unit}= fluxData?.[oIdx] ?? {};
            readout.imageOverlay= makeFileValueReadoutItem('mask', valueBase10, valueBase16, radix, status, type, unit, 0);
        }
    }
    return readout;

}

function doFluxCall(plotView,iPt) {
    const plot= primePlot(plotView);
    if (CysConverter.make(plot).pointInPlot(iPt)) {
        const plotStateAry= getPlotStateAry(plotView);
        const passAry=[plotStateAry[0]];
        if (plotStateAry[1]) passAry.push(plotStateAry[1]);
        return callGetFileFlux(passAry, iPt)
            .then((result) => {
                return result;
            })
            .catch((e) => {
                console.log(`flux error: ${plotView.plotId}`, e);
                return [{}, {}, {}];
            });
    }
    else {
        return Promise.resolve();
    }
}

function getFlux(result, plot) {
    const fluxArray = [];
    if (result.NO_BAND) {
        fluxArray[0]= {...result.NO_BAND, unit: getFluxUnits(plot,Band.NO_BAND)};
    }
    else {
        const bands = plot.plotState.getBands();
        let bandName;
        for (let i = 0; i < bands.length; i++) {
            switch (bands[i].key) {
                case 'RED':
                    bandName = 'Red';
                    break;
                case 'GREEN':
                    bandName = 'Green';
                    break;
                case 'BLUE':
                    bandName = 'Blue';
                    break;
            }
            const r= result[bandName] ?? result[bands[i].key] ?? {}; //todo which one is it
            fluxArray[i]= {...r, unit:getFluxUnits(plot,bands[i])};
        }
    }
    Object.keys(result)
        .filter((k) => k.startsWith('overlay'))
        .forEach( (k) => {fluxArray.push({ imageOverlay : true, ...result[k], unit : 'mask' });});
    return fluxArray;
}

function getFluxLabels(plot) {

    if (!plot) return '';
    const bands = plot.plotState.getBands();
    const fluxLabels = ['', '', ''];
    for (let i = 0; i < bands.length; i++) {
        fluxLabels[i] = showSingleBandFluxLabel(plot, bands[i]);
    }
    return fluxLabels;

}

function showSingleBandFluxLabel(plot, band) {

    if (!plot || !band) return '';
    const fluxUnits= getFluxUnits(plot,band);

    let start;
    switch (band) {
        case Band.RED :
            start = 'Red ';
            break;
        case Band.GREEN :
            start = 'Green ';
            break;
        case Band.BLUE :
            start = 'Blue ';
            break;
        case Band.NO_BAND :
            start = '';
            break;
        default :
            start = '';
            break;
    }
    const valStr = start.length > 0 ? 'Val: ' : 'Value: ';

    const fluxUnitInUpperCase = fluxUnits.toUpperCase();
    if (fluxUnitInUpperCase === 'DN' || fluxUnitInUpperCase === 'FRAMES' || fluxUnitInUpperCase === '') {
        return start + valStr;
    }
    else {
        return start + 'Flux: ';
    }

}

function makeWLResult(plot,imagePt= undefined) {
               // do wavelength readout if it has pixel level wl or if it is a not a cube image with plane wl info
    if ((hasPixelLevelWLInfo(plot) || (hasPlaneOnlyWLInfo(plot) && !isImageCube(plot)))) {
        if (wavelengthInfoParsedSuccessfully(plot)) {
            if (!imagePt) return;
            const cubeIdx= (isImageCube(plot) && getImageCubeIdx(plot)) || 0;
            const wlValue= getPtWavelength(plot, imagePt, cubeIdx);
            return makeValueReadoutItem('Wavelength', wlValue, getWaveLengthUnits(plot), 4);
        }
        else {
            const item=  makeValueReadoutItem('Wavelength', 'Failed', '', 4);
            item.failReason= getWavelengthParseFailReason(plot);
            return item;
        }
    }
    else {
        return undefined;
    }
}



/**
 *
 * @param {WebPlot} plot
 * @param {WorldPt} worldPt
 * @param {ScreenPt} screenPt
 * @param {ImagePt} imagePt
 * @return {Object}
 */
function makeReadout(plot, worldPt, screenPt, imagePt) {
    const csys= CysConverter.make(plot);
    if (csys.pointInPlot(imagePt)) {
        const pixScale = getPixScale(plot);
        const screenPixScale = getScreenPixScale(plot);
        return {
            worldPt: makePointReadoutItem('World Point', worldPt),
            screenPt: makePointReadoutItem('Screen Point', screenPt),
            imagePt: makePointReadoutItem('Image Point', imagePt),
            devPt: makePointReadoutItem('Dev Point', csys.getDeviceCoords(screenPt)),
            fitsImagePt: makePointReadoutItem('FITS Standard Image Point', csys.getFitsStandardImagePtFromInternal(imagePt)),
            zeroBasedImagePt: makePointReadoutItem('FITS Standard Image Point', csys.getZeroBasedImagePtFromInternal(imagePt)),
            title: makeDescriptionItem(plot.title),
            pixel: makeValueReadoutItem('Pixel Size', pixScale.value, pixScale.unit, 3),
            screenPixel:makeValueReadoutItem('Screen Pixel Size', screenPixScale.value, screenPixScale.unit, 3),
            wl: makeWLResult(plot,imagePt)
        };
    }
    else {
        return {
            wl: makeWLResult(plot)
        };
    }

}

//-------------------------------------
//------- HiPS Image Plot
//-------------------------------------


function makeHiPSPixelReadoutItem(plot) {
    const pixDeg= getPlotTilePixelAngSize(plot);
    let unit= 'degree', value= pixDeg;
    if (pixDeg*3600 < 60) {
        unit= 'arcsec';
        value= pixDeg*3600;
    }
    else if (pixDeg*60 < 60) {
        unit= 'arcmin';
        value= pixDeg*60;
    }
    return makeValueReadoutItem('Pixel Size',value, unit, 3);
}

/**
 *
 * @param {WebPlot} plot
 * @param {WorldPt} worldPt
 * @param {ScreenPt} screenPt
 * @param {ImagePt} imagePt
 * @param {boolean} threeColor
 * @param {number} [healpixPixel] the healpix pixel for the current tile, only passed with HiPS
 * @param {number} [norder] the healpix pixel norder
 * @return {Object}
 */
function makeHiPSReadout(plot, worldPt, screenPt, imagePt, threeColor, healpixPixel, norder) {
    const csys= CysConverter.make(plot);
    if (csys.pointInView(imagePt)) {
            return {
                worldPt: makePointReadoutItem('World Point', worldPt),
                screenPt: makePointReadoutItem('Screen Point', screenPt),
                imagePt: makePointReadoutItem('Image Point', imagePt),
                devPt: makePointReadoutItem('Dev Point', csys.getDeviceCoords(screenPt)),
                fitsImagePt: makePointReadoutItem('FITS Standard Image Point', csys.getFitsStandardImagePtFromInternal(imagePt)),
                title: makeDescriptionItem(plot.title),
                pixel: makeHiPSPixelReadoutItem(plot),
                screenPixel:makeValueReadoutItem('Screen Pixel Size',getScreenPixScaleArcSec(plot),'arcsec', 3),
                healpixPixel:makeValueReadoutItem('Healpix Pixel', healpixPixel, 'pixel', 0),
                healpixNorder:makeValueReadoutItem('Healpix norder', norder,'norder', 0),
            };
    }
    else {
        return {};
    }

}
