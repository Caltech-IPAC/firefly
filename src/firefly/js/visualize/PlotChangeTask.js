/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import ImagePlotCntlr, {visRoot,ActionScope} from './ImagePlotCntlr.js';
import {primePlot, getPlotViewById, operateOnOthersInGroup,getPlotStateAry} from './PlotViewUtil.js';
import {
    callChangeColor,
    callRotateNorth,
    callRotateToAngle,
    callRecomputeStretch} from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';
import RangeValues from './RangeValues.js';
import {isPlotNorth} from './VisUtil.js';
import WebPlot, {PlotAttribute} from './WebPlot.js';

export const RotateType= new Enum(['NORTH', 'ANGLE', 'UNROTATE']);


/**
 * color bar Action creator
 * @param rawAction
 * @return {Function}
 */
export function makeColorChangeAction(rawAction) {
    return (dispatcher) => {
        var {plotId,cbarId}= rawAction.payload;
        var pv= getPlotViewById(visRoot(),plotId);
        if (!pv) return;


        if (!primePlot(pv).plotState.isThreeColor()) {
            doColorChange(dispatcher,plotId,cbarId);
            operateOnOthersInGroup(visRoot(),pv, (pv) => doColorChange(dispatcher,pv.plotId,cbarId));
        }
        else {
            dispatcher( {
                type: ImagePlotCntlr.COLOR_CHANGE_FAIL,
                payload: {plotId, error:`can't change color bar for 3 color plots`} } );
        }
    };

}


/**
 * color bar Action creator
 * @param rawAction
 * @return {Function}
 */
export function makeStretchChangeAction(rawAction) {
    return (dispatcher) => {
        var {plotId,rangeValues}= rawAction.payload;
        var pv= getPlotViewById(visRoot(),plotId);
        if (!pv || !rangeValues) return;
        doStretch(dispatcher,plotId,rangeValues);
        operateOnOthersInGroup(visRoot(),pv, (pv) => doStretch(dispatcher,pv.plotId,rangeValues));
    };
}


/**
 * @param rawAction
 * @return {Function}
 */
export function makeRotateAction(rawAction) {
    return (dispatcher) => {
        var { plotId, angle, rotateType, newZoomLevel, actionScope }= rawAction.payload;
        var plotView= getPlotViewById(visRoot(),plotId);
        if (!plotView || !rotateType) return;
        var p= primePlot(plotView);
        if (!p) return;

        if (rotateType===RotateType.NORTH && isPlotNorth(p)) {
                return;
        }
        if (rotateType===RotateType.UNROTATE && !p.plotState.isRotated()) {
            return;
        }

        doRotate(dispatcher,plotView,rotateType,angle,newZoomLevel);
        if (actionScope===ActionScope.GROUP) {
            operateOnOthersInGroup(visRoot(),plotView, (pv) =>
                 doRotate(dispatcher,pv,rotateType,angle,newZoomLevel));
        }
    };
}





function doStretch(dispatcher,plotId,rangeValues) {

    var plot= primePlot(visRoot(),plotId);
    var stretchDataAry= plot.plotState.getBands().map( (band) => {
        return {
            band : band.key,
            rv :  RangeValues.serializeRV(rangeValues),
            bandVisible: true
        };
    } );
    callRecomputeStretch(plot.plotState,stretchDataAry)
        .then( (wpResult) => processStretchResult(dispatcher,plotId,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_FAIL, payload: {plotId, rangeValues, error:e} } );
            logError(`plot error, stretch change, plotId: ${plot.plotId}`, e);
        });
}



function doColorChange(dispatcher,plotId,cbarId) {

    var plot= primePlot(visRoot(),plotId);
    callChangeColor(plot.plotState,cbarId)
        .then( (wpResult) => processColorResult(dispatcher,plotId,cbarId,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_FAIL, payload: {plotId, cbarId, error:e} } );
            logError(`plot error, color change, plotId: ${plot.plotId}`, e);
        });
}

function doRotate(dispatcher,pv,rotateType,angle,newZoomLevel) {

    //var plot= primePlot(visRoot(),plotId);

    var p;

    switch (rotateType) {
        case RotateType.NORTH:
            p= callRotateNorth(getPlotStateAry(pv),true,newZoomLevel);
            break;
        case RotateType.ANGLE:
            p= callRotateToAngle(getPlotStateAry(pv), true, angle, newZoomLevel);
            break;
        case RotateType.UNROTATE:
            p= callRotateToAngle(getPlotStateAry(pv), false, NaN, 0);
            break;
    }


    p.then( (wpResult) => processRotateResult(dispatcher,wpResult,pv,rotateType) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.ROTATE_FAIL, payload: {plotId, error:e} } );
            logError(`plot error, rotate , plotId: ${pv.plotId}`, e);
        });
}



function processRotateResult(dispatcher, result, pv, rotateType) {
    var successSent = false;
    if (result.success) {
        var resultAry = result[WebPlotResult.RESULT_ARY];
        if (resultAry[0].success) {

            var plotAry = resultAry[0].data[WebPlotResult.PLOT_CREATE].map((wpInit) => makePlot(wpInit, pv));

            var overlayPlotViews = [];
            resultAry.forEach((r, i) => {
                if (i === 0) return;
                var plot = WebPlot.makeWebPlotData(pv.plotId, r.data[WebPlotResult.PLOT_CREATE], true);
                overlayPlotViews[i - 1] = {plot};
            });
            dispatcher({
                type: ImagePlotCntlr.ROTATE,
                payload: {plotId: pv.plotId, plotAry, overlayPlotViews, rotateType}
            });
            dispatcher({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotIdAry: [pv.plotId]}});
            successSent = true;

        }
        if (!successSent) {
            dispatcher({
                type: ImagePlotCntlr.ZOOM_IMAGE_FAIL,
                payload: {plotId: pv.plotId, error: Error('payload failed')}
            });
        }
    }
}


function makePlot(wpInit,pv) {
    var plot= WebPlot.makeWebPlotData(pv.plotId, wpInit);
    plot.title= primePlot(pv).title;
    plot.attributes= primePlot(pv).attributes;
    return plot;
}




/**
 *
 * @param dispatcher
 * @param plotId
 * @param cbarId
 * @param {object} result
 */
function processColorResult(dispatcher, plotId, cbarId, result) {
    var successSent= false;
    if (result.success) {
            dispatcher( {
                type: ImagePlotCntlr.COLOR_CHANGE,
                payload: {
                    plotId,
                     primaryStateJson : result[WebPlotResult.PLOT_STATE],
                     primaryTiles : result[WebPlotResult.PLOT_IMAGES]
                }});
            dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
            successSent= true;
    }
    if (!successSent) {
        dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_FAIL,
            payload: {plotId, cbarId, error:Error('payload failed, color change')} } );
    }
}

/**
 *
 * @param dispatcher
 * @param plotId
 * @param {object} result
 */
function processStretchResult(dispatcher, plotId, result) {
    var successSent= false;
    if (result.success) {
        dispatcher( {
            type: ImagePlotCntlr.STRETCH_CHANGE,
            payload: {
                plotId,
                primaryStateJson : result[WebPlotResult.PLOT_STATE],
                primaryTiles : result[WebPlotResult.PLOT_IMAGES]
            }});
        dispatcher( { type: ImagePlotCntlr.ANY_REPLOT, payload:{plotIdAry:[plotId]}} );
        successSent= true;
    }
    if (!successSent) {
        dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_FAIL,
            payload: {plotId, error:Error('payload failed, stretch change')} } );
    }
}

