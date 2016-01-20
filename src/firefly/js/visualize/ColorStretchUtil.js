/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import ImagePlotCntlr, {visRoot,ActionScope} from './ImagePlotCntlr.js';
import PlotViewUtil, {getPlotViewById} from './PlotViewUtil.js';
import PlotServicesJson from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';
import VisUtil from './VisUtil.js';
import RangeValues from './RangeValues.js';
import {Band} from './Band.js';






/**
 *
 * @param {string} plotId
 * @param {number} cbarId
 * @param {ActionScope} actionScope
 */
export function doDispatchColorChange(plotId, cbarId, actionScope=ActionScope.GROUP ) {

    flux.process({
        type: ImagePlotCntlr.COLOR_CHANGE,
        payload :{
            plotId, cbarId, actionScope
        }});
}

/**
 *
 * @param {string} plotId
 * @param {number} rangeValues
 * @param {ActionScope} actionScope
 */
export function doDispatchStretchChange(plotId, rangeValues, actionScope=ActionScope.GROUP ) {

    flux.process({
        type: ImagePlotCntlr.STRETCH_CHANGE,
        payload :{
            plotId, rangeValues, actionScope
        }});
}



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


        if (!pv.primaryPlot.plotState.isThreeColor()) {
            doColorChange(dispatcher,plotId,cbarId);
            PlotViewUtil.operateOnOthersInGroup(visRoot(),pv, (pv) => doColorChange(dispatcher,pv.plotId,cbarId));
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
        PlotViewUtil.operateOnOthersInGroup(visRoot(),pv, (pv) => doStretch(dispatcher,pv.plotId,rangeValues));
    };
}


function doStretch(dispatcher,plotId,rangeValues) {

    var pv= getPlotViewById(visRoot(),plotId);
    var stretchDataAry= pv.primaryPlot.plotState.getBands().map( (band) => {
        return {
            band : band.key,
            rv :  RangeValues.serializeRV(rangeValues),
            bandVisible: true
        };
    } );
    PlotServicesJson.recomputeStretch(pv.primaryPlot.plotState,stretchDataAry)
        .then( (wpResult) => processStretchResult(dispatcher,plotId,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.STRETCH_CHANGE_FAIL, payload: {plotId, rangeValues, error:e} } );
            logError(`plot error, stretch change, plotId: ${pv.plotId}`, e);
        });
}



function doColorChange(dispatcher,plotId,cbarId) {

    var pv= getPlotViewById(visRoot(),plotId);
    PlotServicesJson.changeColor(pv.primaryPlot.plotState,cbarId)
        .then( (wpResult) => processColorResult(dispatcher,plotId,cbarId,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_FAIL, payload: {plotId, cbarId, error:e} } );
            logError(`plot error, color change, plotId: ${pv.plotId}`, e);
        });
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

