/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {union,get,isEmpty} from 'lodash';
import {TABLE_NEW,TABLE_SELECT,TABLE_HIGHLIGHT,
        TABLE_REMOVE,TABLE_UPDATE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchPlotImage, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {REINIT_RESULT_VIEW} from '../../core/AppDataCntlr.js';
import {findTblById,gatherTableState} from '../../tables/TableUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import MultiViewCntlr, {dispatchReplaceImages, getViewerPlotIds, 
                        getMultiViewRoot, getViewer, GRID_FULL, SINGLE} from '../MultiViewCntlr.js';
import {ACTIVE_TABLE_CHANGED} from '../../core/LayoutCntlr.js';
import {converterFactory} from '../../metaConvert/ConverterFactory.js';
import {findGridTableRows,isMetaDataTable} from '../../metaConvert/converterUtils.js';


/**
 * this saga does the following:
 * <ul>
 *     <li>Then loops:
 *     <ul>
 *         <li>waits table change, and loads images- only acts with image meta data tables
 *         <li>waits for watches for viewer mounting and unmounted, paused with unmounted
 *     </ul>
 * </ul>
 */
export function* watchImageMetaData(params) {

    const {viewerId}= params;
    var tbl_id;
    var paused= true;
    while (true) {
        const action= yield take([TABLE_NEW,TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
                                  ACTIVE_TABLE_CHANGED,
                                  MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
                                  MultiViewCntlr.VIEWER_UNMOUNTED,
                                  MultiViewCntlr.CHANGE_LAYOUT,
                                  REINIT_RESULT_VIEW]);
        const {payload}= action;

        if (payload.tbl_id) {
            if (!isMetaDataTable(payload.tbl_id)) continue;
            tbl_id= payload.tbl_id; // otherwise us the last one
        }
        if (payload.viewerId && payload.viewerId!==viewerId) {
            continue;
        }

        switch (action.type) {

            case TABLE_NEW:
            case TABLE_REMOVE:
            case TABLE_HIGHLIGHT:
            case TABLE_UPDATE:
            case ACTIVE_TABLE_CHANGED:
                if (!paused) updateImagePlots(tbl_id, viewerId);
                break;

            case MultiViewCntlr.CHANGE_LAYOUT:
                if (!paused) updateImagePlots(tbl_id, viewerId, true);
                break;


            case MultiViewCntlr.ADD_VIEWER:
                if (payload.mounted) {
                    updateImagePlots(tbl_id, viewerId);
                    paused= false;
                }
                break;

            case MultiViewCntlr.VIEWER_MOUNTED:
                paused= false;
                updateImagePlots(tbl_id, viewerId);
                break;

            case MultiViewCntlr.VIEWER_UNMOUNTED:
                paused= true;
                break;

            case REINIT_RESULT_VIEW:
                return; // sega exit
                break;
        }
    }
}


/**
 * 
 * @param tbl_id
 * @param viewerId
 * @param layoutChange
 * @return {Array}
 */
function updateImagePlots(tbl_id, viewerId, layoutChange=false) {

    if (!tbl_id) return [];
    var reqRet;
    const table= findTblById(tbl_id);
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    if (!table) return [];

    const converterData= converterFactory(table);
    const {dataId,converter}= converterData;
    var highlightPlotId;


    const gridFull= (viewer.layoutDetail===GRID_FULL || !converter.hasRelatedBands );
    const tabState= gatherTableState(table);
    const {highlightedRow}= tabState;

    if (layoutChange && viewer.layout===SINGLE && !isEmpty(viewer.plotIdAry)) {
        return;
    }
    
    if (gridFull) {
        const plotRows= findGridTableRows(table,12,`${dataId}-grid`);
        var reqAry= plotRows.map( (pR) => {
            const {single}= converter.makeRequest(table,pR.row,true);
            single.setPlotId(pR.plotId);
            return single;
        } );
        const pR= plotRows.filter( (pR) => pR.highlight).find( (pR) => pR.plotId);
        highlightPlotId= pR && pR.plotId;
    }
    else {
        reqRet= converter.makeRequest(table,highlightedRow,false,true,false);
        highlightPlotId= reqRet.highlightPlotId;
        reqAry= reqRet.standard;
    }

    replot(reqAry, highlightPlotId, viewerId, dataId);
}


function replot(reqAry, activeId, viewerId, dataId)  {
    const groupId= `${viewerId}-${dataId}-standard`;
    const plottingIds= reqAry.map( (r) =>  r.getPlotId());
    reqAry.forEach( (r) => r.setPlotGroupId(groupId));
    const inViewerIds= getViewerPlotIds(getMultiViewRoot(), viewerId);
    const idUnionLen= union(plottingIds,inViewerIds).length;
    if (idUnionLen!== inViewerIds.length || plottingIds.length<inViewerIds.length) {
        dispatchReplaceImages(viewerId,plottingIds);
    }
    const plotReqAry= makePlottingList(reqAry);

    plotReqAry.forEach( (r) => dispatchPlotImage(r.getPlotId(),r,false) );
    if (activeId) dispatchChangeActivePlotView(activeId);
}


function makePlottingList(reqAry) {
    return reqAry.filter( (r) => {
        const plot= primePlot(visRoot(),r.getPlotId());
        if (!plot) return true;

        var oldR= plot.plotState.getWebPlotRequest().makeCopy();
        r= r.makeCopy();

        // for some fields to be the same before the equals
        oldR.setZoomToWidth(1);
        oldR.setZoomToHeight(1);
        r.setZoomToWidth(1);
        r.setZoomToHeight(1);
        // compare the toString version
        return oldR.toString()!==r.toString();
    });
}



//todo
function makePlottingList3Color(reqAry, lastPlottedIds) {
    return reqAry;

}
