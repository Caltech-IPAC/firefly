/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {race,call} from 'redux-saga/effects';
import {get} from 'lodash';
import {visRoot} from '../ImagePlotCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {readoutRoot, dispatchReadoutData, makeValueReadoutItem, makePointReadoutItem,
        makeDescriptionItem, isLockByClick} from '../MouseReadoutCntlr.js';
import {callGetFileFlux} from '../../rpc/PlotServicesJson.js';
import {Band} from '../Band.js';
import {MouseState} from '../VisMouseSync.js';
import {isBlankImage} from '../WebPlot.js';
import {primePlot, getPlotStateAry, getPlotViewById} from '../PlotViewUtil.js';
import {CysConverter} from '../CsysConverter.js';
import {mouseUpdatePromise} from '../VisMouseSync.js';



const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));


export function* watchReadout() {

    var lockByClick;
    var mouseCtx;
    yield call(mouseUpdatePromise);

    mouseCtx = yield call(mouseUpdatePromise);

    var getNextWithFlux;
    var threeColor= false;
    var plotView;


    while (true) {

        getNextWithFlux= false;
        lockByClick= isLockByClick(readoutRoot());
        const {plotId,worldPt,screenPt,imagePt,mouseState}= mouseCtx;

        if (usePayload(mouseState,lockByClick)) {
            plotView= getPlotViewById(visRoot(), plotId);
            var plot= primePlot(plotView);

            if (plot) {
                var readout= makeReadoutWithFlux(makeReadout(plot,worldPt,screenPt,imagePt), plot, null, plot.plotState.threeColor);
                threeColor= plot.plotState.isThreeColor();
                dispatchReadoutData(plotId,readout, threeColor);

                getNextWithFlux= !isBlankImage(plot);
            }
        }
        else if (!lockByClick) {
            dispatchReadoutData(plotId, {});
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
            const readout= makeReadoutWithFlux(noFluxReadout,primePlot(plotView), fluxResult, threeColor);
            dispatchReadoutData(plotView.plotId,readout, threeColor);
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
            const readout= makeReadoutWithFlux(noFluxReadout,primePlot(plotView), raceWinner.fluxResult, threeColor);
            dispatchReadoutData(plotView.plotId,readout, threeColor);
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
 * @return {{worldPt: *, screenPt: *, imagePt: *, threeColor: (boolean), title: *, pixel: ({title, value, unit, precision}|{title: *, value: *, unit: *, precision: *})}}
 */
function makeReadout(plot, worldPt, screenPt, imagePt) {
    if (CysConverter.make(plot).pointInPlot(imagePt)) {
        return {
            worldPt: makePointReadoutItem('World Point', worldPt),
            screenPt: makePointReadoutItem('Screen Point', screenPt),
            imagePt: makePointReadoutItem('Image Point', imagePt),
            title: makeDescriptionItem(plot.title),
            pixel: makeValueReadoutItem('Pixel Size',plot.projection.getPixelScaleArcSec(),'arcsec', 3),
            screenPixel:makeValueReadoutItem('Screen Pixel Size',plot.projection.getPixelScaleArcSec()/plot.zoomFactor,'arcsec', 3)
        };
    }
    else {
        return {};
    }

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

    if (!plot) return '';

    var webFitsData = plot.webFitsData;
    if (!band) return '';
    var fluxUnits = webFitsData[band.value].fluxUnits;

    var start;
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
    var valStr = start.length > 0 ? 'Val: ' : 'Value: ';

    var fluxUnitInUpperCase = fluxUnits.toUpperCase();
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
        var fluxUnitStr = plot.webFitsData[Band.NO_BAND.value].fluxUnits;
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
            unitStr = get(plot.webFitsData, [bands[i].value, 'fluxUnits'], '');
            fnum = parseFloat(result[bandName]);
            fluxArray[i]= {bandName, value:fnum, unit:unitStr};
        }
    }
    Object.keys(result)
        .filter((k) => k.startsWith('overlay'))
        .forEach( (k) => {fluxArray.push({ imageOverlay : true, value : parseFloat(result[k]), unit : 'mask' });});
    return fluxArray;
}

