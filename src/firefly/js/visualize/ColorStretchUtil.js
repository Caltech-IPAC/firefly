/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import numeral from 'numeral';
import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import {PlotAttribute} from './WebPlot.js';
import ImagePlotCntlr, {visRoot,ActionScope} from './ImagePlotCntlr.js';
import PlotViewUtil, {getPlotViewById} from './PlotViewUtil.js';
import PlotServicesJson from '../rpc/PlotServicesJson.js';
import WebPlotResult from './WebPlotResult.js';
import VisUtil from './VisUtil.js';






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
 * zoom Action creator, todo: zoomScope, fit, fill, and much, much more
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
            var matchFunc= makeColorChangeMatcher(dispatcher,cbarId);
            PlotViewUtil.operateOnOthersInGroup(visRoot(),pv, matchFunc);
        }
        else {
            dispatcher( {
                type: ImagePlotCntlr.COLOR_CHANGE_FAIL,
                payload: {plotId, error:`can't change color bar for 3 color plots`} } );
        }
    };

}

function doColorChange(dispatcher,plotId,cbarId) {

    var pv= getPlotViewById(visRoot(),plotId);
    PlotServicesJson.changeColor(pv.primaryPlot.plotState,cbarId)
        .then( (wpResult) => processResult(dispatcher,plotId,cbarId,wpResult) )
        .catch ( (e) => {
            dispatcher( { type: ImagePlotCntlr.COLOR_CHANGE_FAIL, payload: {plotId, cbarId, error:e} } );
            logError(`plot error, color change, plotId: ${pv.plotId}`, e);
        });
}



function makeColorChangeMatcher(dispatcher, cbarId) {
    return (pv) => {
        doColorChange(dispatcher,pv.plotId,cbarId);
    };
}



/**
 *
 * @param dispatcher
 * @param plotId
 * @param cbarId
 * @param {object} result
 */
function processResult(dispatcher, plotId, cbarId, result) {
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

