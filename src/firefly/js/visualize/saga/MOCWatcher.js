/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {once} from 'lodash';
import {dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {getTblById} from '../../tables/TableUtil.js';
import {addNewMocLayer, getAppHiPSForMoc, isTableMOC} from '../HiPSMocUtil.js';
import {getActivePlotView, getNextHiPSPlotId, getPlotViewAry, primePlot} from '../PlotViewUtil.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import {dispatchPlotHiPS} from '../ImagePlotCntlr';
import {MetaConst} from '../../data/MetaConst.js';
import {isHiPS} from '../WebPlot.js';
import {RequestType} from '../RequestType';
import {onPlotComplete} from '../PlotCompleteMonitor';


/** @type {TableWatcherDef} */
export const getMocWatcherDef= once(() => ({
    id : 'MOCWatcher',
    watcher : watchForMOC,
    testTable : isTableMOC,
    stopPropagation: true,
    allowMultiples: false,
    actions: []
}));


/**
 * type {TableWatchFunc}
 * this saga does the following:
 * <ul>
 *     <li>Waits until first fits image is plotted
 *     <li>loads all the table that are catalogs
 *     <li>Then loops:
 *     <ul>
 *         <li>waits for a table new table, update, highlight or select change and then updates the drawing layer
 *         <li>waits for a new plot and adds any catalog
 *     </ul>
 * </ul>
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params
 * @return {*}
 */
export function watchForMOC(tbl_id, action, cancelSelf, params) {


    if (!action) {
        loadMOC(tbl_id);
        cancelSelf();
    }
}

function loadMOC(tbl_id) {

    const table= getTblById(tbl_id);
    if (!table) return;
    const {REQUIRED_HIPS, PREFERRED_HIPS} = MetaConst;
    const {tableMeta}= table;
    let plotId;

    if (tableMeta[REQUIRED_HIPS]) {
        plotId= findHiPSWithSource(tableMeta[REQUIRED_HIPS]) || plotHiPS(tableMeta[REQUIRED_HIPS]);
    }
    else if (tableMeta[PREFERRED_HIPS]) {
        plotId= findHiPSWithSource(tableMeta[PREFERRED_HIPS]) || findAnyHiPS() || plotHiPS(tableMeta[PREFERRED_HIPS]);
    }
    else {
        plotId= findAnyHiPS();
    }

    onPlotComplete(plotId).then( (pv) => {
        if (!pv) return;
        const dl = addNewMocLayer({
            tbl_id: table.tbl_id, uniqColName: table.tableData.columns[0].name, tablePreloaded: true});
        if (dl) {
            dispatchAttachLayerToPlot(dl.drawLayerId, plotId, true, true, true);
        }
    });

}


/**
 * find the plotId of a hips with the passed URL
 * @param url
 * @return {String} plotId
 */
function findHiPSWithSource(url) {
    const pvAry= getPlotViewAry(visRoot());
    const pv= pvAry.find( (pv) => isHiPSDeferred(pv) && (pv.request.getURL() === url) );
    return pv && pv.plotId;
}

/**
 * find plotId of active or first HiPS
 * @return {String} plotId
 */
function findAnyHiPS() {
    const activePV= getActivePlotView(visRoot());
    if (isHiPSDeferred(activePV)) return activePV.plotId;
    const foundPv= getPlotViewAry(visRoot()).find( (pv) => isHiPSDeferred(pv));
    return foundPv && foundPv.plotId;
}

function plotHiPS(url) {
    const plotId = getNextHiPSPlotId();

    const hipsUrl = url || getAppHiPSForMoc();

    const wpRequest = WebPlotRequest.makeHiPSRequest(hipsUrl);
    const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};

    wpRequest.setPlotGroupId(viewerId);
    wpRequest.setPlotId(plotId);
    wpRequest && dispatchPlotHiPS({plotId, viewerId, wpRequest});

    return plotId;
}

function isHiPSDeferred(pv) {
    if (!pv) return false;
    const plot= primePlot(pv);
    if (plot) return isHiPS(plot);
    return pv.request.getRequestType() === RequestType.HiPS;
}


