/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {race,call} from 'redux-saga/effects';
import {get} from 'lodash';
import {visRoot} from '../ImagePlotCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {readoutRoot, dispatchReadoutData, makeValueReadoutItem, makePointReadoutItem,makeHealpixReadoutItem,
        makeDescriptionItem, isLockByClick} from '../MouseReadoutCntlr.js';
import {callGetFileFlux} from '../../rpc/PlotServicesJson.js';
import {Band} from '../Band.js';
import {MouseState} from '../VisMouseSync.js';
import {primePlot, getPlotStateAry, getPlotViewById} from '../PlotViewUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {mouseUpdatePromise} from '../VisMouseSync.js';
import {getPixScaleArcSec, getScreenPixScaleArcSec, isImage, isHiPS, getFluxUnits} from '../WebPlot.js';
import {getPlotTilePixelAngSize} from '../HiPSUtil.js';


const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));


export function* watchReadout() {

    let mouseCtx;
    yield call(mouseUpdatePromise);

    mouseCtx = yield call(mouseUpdatePromise);

    while (true) {

        let getNextWithFlux= false;
        const lockByClick= isLockByClick(readoutRoot());
        const {plotId,worldPt,screenPt,imagePt,mouseState, healpixPixel, norder}= mouseCtx;

        let readout= undefined;
        const plotView= getPlotViewById(visRoot(), plotId);
        const plot= primePlot(plotView);
        const threeColor= plot.plotState.threeColor;
        if (usePayload(mouseState,lockByClick)) {

            if (plot) {
                if (isImage(plot)) {
                    readout= makeReadoutWithFlux(makeReadout(plot,worldPt,screenPt,imagePt), plot, null, threeColor);
                    dispatchReadoutData({plotId,readoutItems:readout, threeColor});
                    getNextWithFlux= true;
                }
                else {
                    dispatchReadoutData({plotId,readoutItems:makeReadout(plot,worldPt,screenPt,imagePt, healpixPixel, norder),
                                         isHiPS:true});
                }
            }
        }
        else if (!lockByClick) {
            dispatchReadoutData({plotId, readoutItems:{}, isHiPS:isHiPS(plot)});
        }

        if (getNextWithFlux) { // get the next mouse event or the flux
            mouseCtx= lockByClick ? yield call(processImmediateFlux,readout,plotView,imagePt,threeColor) :
                                    yield call(processDelayedFlux,readout,plotView,imagePt,threeColor);
        }
        else { // get the next mouse event
            mouseCtx = yield call(mouseUpdatePromise);
        }

    }
}

function* processImmediateFlux(noFluxReadout,plotView,imagePt, threeColor) {
    try {
        const fluxResult= yield call(doFluxCall, plotView, imagePt);
        if (fluxResult) {
            const plot= primePlot(plotView);
            const readout= makeReadoutWithFlux(noFluxReadout,plot, fluxResult, threeColor);
            dispatchReadoutData({plotId:plotView.plotId,readoutItems:readout, isHiPS:isHiPS(plot), threeColor});
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


function* processDelayedFlux(noFluxReadout,plotView,imagePt, threeColor) {
    var raceWinner = yield race({
        mouseCtx: call(mouseUpdatePromise),
        timer: call(delay, 200)
    });

    if (raceWinner.mouseCtx) return raceWinner.mouseCtx;


    try {
        raceWinner = yield race({
            mouseCtx: call(mouseUpdatePromise),
            fluxResult: call(doFluxCall, plotView, imagePt,200)
        });
        if (raceWinner.mouseCtx) return raceWinner.mouseCtx;

        if (raceWinner.fluxResult) {
            const plot= primePlot(plotView);
            const readout= makeReadoutWithFlux(noFluxReadout,primePlot(plotView), raceWinner.fluxResult, threeColor);
            dispatchReadoutData({plotId:plotView.plotId,readoutItems:readout, isHiPS:isHiPS(plot), threeColor});
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



function usePayload(mouseState, lockByClick) {
    if (lockByClick) {
        return mouseState===MouseState.CLICK;
    }
    else {
        return mouseState!==MouseState.EXIT;
    }

}


/**
 *
 * @param {WebPlot} plot
 * @param {WorldPt} worldPt
 * @param {ScreenPt} screenPt
 * @param {ImagePt} imagePt
 * @param {number} [healpixPixel] the healpix pixel for the current tile, only passed with HiPS
 * @param {number} [norder] the healpix pixel norder
 * @return {{worldPt: *, screenPt: *, imagePt: *, threeColor: (boolean), title: *, pixel: ({title, value, unit, precision}|{title: *, value: *, unit: *, precision: *})}}
 */
function makeReadout(plot, worldPt, screenPt, imagePt, healpixPixel, norder) {
    if (CysConverter.make(plot).pointInPlot(imagePt)) {
        if (isImage(plot)) {
            return {
                worldPt: makePointReadoutItem('World Point', worldPt),
                screenPt: makePointReadoutItem('Screen Point', screenPt),
                imagePt: makePointReadoutItem('Image Point', imagePt),
                title: makeDescriptionItem(plot.title),
                pixel: makeValueReadoutItem('Pixel Size',getPixScaleArcSec(plot),'arcsec', 3),
                screenPixel:makeValueReadoutItem('Screen Pixel Size',getScreenPixScaleArcSec(plot),'arcsec', 3)
            };
        }
        else {
            return {
                worldPt: makePointReadoutItem('World Point', worldPt),
                screenPt: makePointReadoutItem('Screen Point', screenPt),
                imagePt: makePointReadoutItem('Image Point', imagePt),
                title: makeDescriptionItem(plot.title),
                pixel: makeHiPSPixelReadoutItem(plot),
                screenPixel:makeValueReadoutItem('Screen Pixel Size',getScreenPixScaleArcSec(plot),'arcsec', 3),
                healpixPixel:makeValueReadoutItem('Healpix Pixel', healpixPixel, 'pixel', 0),
                healpixNorder:makeValueReadoutItem('Healpix norder', norder,'norder', 0),
            };

        }
    }
    else {
        return {};
    }

}

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
 * @param readout
 * @param {WebPlot} plot
 * @param fluxResult
 * @param threeColor
 * @return {*}
 */
function makeReadoutWithFlux(readout, plot, fluxResult,threeColor) {
    readout= clone(readout);
    const fluxData= fluxResult ? getFlux(fluxResult,plot) : null;
    const labels= getFluxLabels(plot);
    if (threeColor) {
        const bands = plot.plotState.getBands();
        bands.forEach( (b,idx) => readout[b.key+'Flux']=
            makeValueReadoutItem(labels[idx], get(fluxData,[idx,'value']),get(fluxData,[idx,'unit']), 6 ));
    }
    else {
        readout.nobandFlux= makeValueReadoutItem(labels[0], get(fluxData,[0,'value']),get(fluxData,[0,'unit']), 6);
    }
    if (fluxData) {
        const oIdx= fluxData.findIndex( (d) => d.imageOverlay);
        if (oIdx>-1) {
            readout.imageOverlay= makeValueReadoutItem('mask', fluxData[oIdx].value, fluxData[oIdx].unit, 0);
        }
    }
    return readout;
}


function getFluxLabels(plot) {

    if (!plot) return '';
    var bands = plot.plotState.getBands();
    var fluxLabels = ['', '', ''];
    for (var i = 0; i < bands.length; i++) {
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







function doFluxCall(plotView,iPt) {
    const plot= primePlot(plotView);
    if (CysConverter.make(plot).pointInPlot(iPt)) {
        const plotStateAry= getPlotStateAry(plotView);
        return callGetFileFlux(plotStateAry, iPt)
            .then((result) => {
                return result;
            })
            .catch((e) => {
                console.log(`flux error: ${plotView.plotId}`, e);
                return ['', '', ''];
            });
    }
    else {
        return Promise.resolve(['', '', '']);
    }
}


function getFlux(result, plot) {
    var fluxArray = [];
    if (result.NO_BAND) {
        var fluxUnitStr = getFluxUnits(plot,Band.NO_BAND);
        var fValue = parseFloat(result.NO_BAND);
        fluxArray[0]= {value: fValue, unit: fluxUnitStr};
    }
    else {
        const bands = plot.plotState.getBands();
        var bandName, unitStr, fnum;
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
            unitStr = getFluxUnits(plot,bands[i]);
            fnum = parseFloat(result[bandName]);
            fluxArray[i]= {bandName, value:fnum, unit:unitStr};
        }
    }
    Object.keys(result)
        .filter((k) => k.startsWith('overlay'))
        .forEach( (k) => {fluxArray.push({ imageOverlay : true, value : parseFloat(result[k]), unit : 'mask' });});
    return fluxArray;
}

