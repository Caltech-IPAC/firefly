/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {union,get,isEmpty,difference} from 'lodash';
import {Band,allBandAry} from '../Band.js';
import {TABLE_SELECT,TABLE_HIGHLIGHT,
        TABLE_REMOVE,TABLE_UPDATE, TBL_RESULTS_ACTIVE, dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchPlotImage, dispatchDeletePlotView,
                        dispatchPlotGroup, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {REINIT_RESULT_VIEW} from '../../core/AppDataCntlr.js';
import {getTblById,getTblInfo,getActiveTableId,isTblDataAvail} from '../../tables/TableUtil.js';
import {primePlot, getPlotViewById} from '../PlotViewUtil.js';
import MultiViewCntlr, {dispatchReplaceViewerItems, dispatchUpdateCustom, getViewerItemIds,
                        dispatchChangeViewerLayout,
                        getMultiViewRoot, getViewer, GRID, GRID_FULL, SINGLE} from '../MultiViewCntlr.js';
import {converterFactory, converters} from '../../metaConvert/ConverterFactory.js';
import {findGridTableRows,isMetaDataTable} from '../../metaConvert/converterUtils.js';
import {PlotAttribute} from '../WebPlot.js';
import {isImageDataRequeestedEqual} from '../WebPlotRequest.js';

const MAX_GRID_SIZE= 50;

/**
 * this saga does the following:
 * <ul>
 *     <li>Then loops:
 *     <ul>
 *         <li>waits table change, and loads images- only acts with image meta data tables
 *         <li>waits for watches for viewer mounting and unmounted, paused with unmounted
 *     </ul>
 * </ul>
 * @param viewerId
 */
export function* watchImageMetaData({viewerId, paused=true}) {

    var tbl_id;

    if (!paused) {
        tbl_id= getActiveTableId();
        if (tbl_id) updateImagePlots(tbl_id, viewerId);
    }



    while (true) {
        const action= yield take([TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
                                  TBL_RESULTS_ACTIVE,
                                  MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
                                  MultiViewCntlr.VIEWER_UNMOUNTED,
                                  MultiViewCntlr.CHANGE_VIEWER_LAYOUT, MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA,
                                  ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW,
                                  REINIT_RESULT_VIEW]);
        const {payload}= action;

        if (payload.viewerId && payload.viewerId!==viewerId) continue;

        if (action.type===TABLE_REMOVE) {
            tbl_id= getActiveTableId();
            if (!tbl_id) removeAllPlotsInViewer(viewerId);
        }
        else if (payload.tbl_id) {
            if (!isMetaDataTable(payload.tbl_id)) continue;
            tbl_id= payload.tbl_id; // otherwise use the last one
        }


        switch (action.type) {

            case TABLE_REMOVE:
            case TABLE_HIGHLIGHT:
            case TABLE_UPDATE:
            case TBL_RESULTS_ACTIVE:
                if (!paused) updateImagePlots(tbl_id, viewerId);
                break;

            case MultiViewCntlr.CHANGE_VIEWER_LAYOUT:
            case MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA:
                if (!paused) updateImagePlots(tbl_id, viewerId, true);
                break;


            case MultiViewCntlr.ADD_VIEWER:
                init3Color(viewerId);
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

            case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
                if (!paused) changeActivePlotView(action.payload.plotId,tbl_id);
                break;

            case REINIT_RESULT_VIEW:
                return; // sega exit
                break;
        }
    }
}


const getKey= (threeOp, band) => Object.keys(threeOp).find( (k) => threeOp[k].color && threeOp[k].color.key===band.key);

/**
 * 
 * @param tbl_id
 * @param viewerId
 * @param layoutChange
 * @return {Array}
 */
function updateImagePlots(tbl_id, viewerId, layoutChange=false) {

    var viewer = getViewer(getMultiViewRoot(), viewerId);


    const table = getTblById(tbl_id);
    // check to see if tableData is available in this range.
    if (!table || !isTblDataAvail(table.highlightedRow, table.highlightedRow + 1, table)) {
        removeAllPlotsInViewer(viewerId);
        return [];
    }

    var reqRet;
    const converterData = converterFactory(table);
    if (!converterData) return [];
    const {dataId, converter}= converterData;
    var highlightPlotId;
    var threeColorOps;
    var threeReqAry;


    if (viewer.layout === GRID && viewer.layoutDetail !== GRID_FULL && !converter.hasRelatedBands) {
        dispatchChangeViewerLayout(viewerId, SINGLE, null);
        viewer = getViewer(getMultiViewRoot(), viewerId);
    }


    if (converter.threeColor) {
        const showThreeColor= get(viewer.customData, [dataId,'threeColorVisible'], false);
        if (showThreeColor) {
            threeColorOps= [
                getKey(viewer.customData[dataId], Band.RED),
                getKey(viewer.customData[dataId], Band.GREEN),
                getKey(viewer.customData[dataId], Band.BLUE)
            ];
        }
    }

    const tabState= getTblInfo(table);
    const {highlightedRow}= tabState;

    if (layoutChange && viewer.layout===SINGLE && !isEmpty(viewer.itemIdAry)) {
        return;
    }
    

    if (viewer.layout===SINGLE) {
        const {single}= converter.makeRequest(table,highlightedRow,true);
        if (!single) return [];
        reqAry= [single];
        single.setPlotId(`${dataId}-singleview`,0);
        single.setRelatedTableRow(highlightedRow);
        highlightPlotId= single.getPlotId();
    }
    else if (viewer.layout===GRID && viewer.layoutDetail===GRID_FULL) {
        const plotRows= findGridTableRows(table,Math.min(converter.maxPlots,MAX_GRID_SIZE),`${dataId}-gridfull`);
        var reqAry= plotRows.map( (pR) => {
            const {single}= converter.makeRequest(table,pR.row,true);
            if (!single) return [];
            single.setRelatedTableRow(pR.row);
            single.setPlotId(pR.plotId);
            return single;
        } );
        const pR= plotRows.filter( (pR) => pR.highlight).find( (pR) => pR.plotId);
        highlightPlotId= pR && pR.plotId;
    }
    else if (viewer.layout===GRID) {
        reqRet= converter.makeRequest(table,highlightedRow,false,true,threeColorOps);
        highlightPlotId= reqRet.highlightPlotId;
        reqAry= reqRet.standard;
        if (isEmpty(reqAry)) return [];
        threeReqAry= reqRet.threeColor;
    }

    replot(reqAry, threeReqAry, highlightPlotId, viewerId, dataId, tbl_id);
}


function removeAllPlotsInViewer(viewerId) {
    if (!viewerId) return;
    const inViewerIds= getViewerItemIds(getMultiViewRoot(), viewerId);
    dispatchReplaceViewerItems(viewerId,[]);
    inViewerIds.forEach( (plotId) => dispatchDeletePlotView({plotId}));
}

function replot(reqAry, threeReqAry, activeId, viewerId, dataId, tbl_id)  {
    const groupId= `${viewerId}-${dataId}-standard`;
    var plottingIds= reqAry.map( (r) =>  r.getPlotId());
    var threeCPlotId;
    var plottingThree= false;
    if (!isEmpty(threeReqAry)) {
        const r= threeReqAry.find( (r) => Boolean(r));
        if (r) {
            plottingThree= true;
            threeCPlotId= r.getPlotId();
            plottingIds= [...plottingIds,threeCPlotId];
            threeReqAry.forEach( (r) => r && r.setPlotGroupId(groupId) );
        }
    }
    reqAry.forEach( (r) => r.setPlotGroupId(groupId));
    
    
    // setup view for these plotting ids
    const inViewerIds= getViewerItemIds(getMultiViewRoot(), viewerId);
    const idUnionLen= union(plottingIds,inViewerIds).length;
    if (idUnionLen!== inViewerIds.length || plottingIds.length<inViewerIds.length) {
        dispatchReplaceViewerItems(viewerId,plottingIds);
    }


    // clean up unused Ids
    const cleanUpIds= difference(inViewerIds,plottingIds);
    cleanUpIds.forEach( (plotId) => dispatchDeletePlotView({plotId, holdWcsMatch:true}));


    // prepare stand plot
    const wpRequestAry= makePlottingList(reqAry);
    if (!isEmpty(wpRequestAry)) {
        dispatchPlotGroup({wpRequestAry, viewerId, holdWcsMatch:true,
                           pvOptions: { userCanDeletePlots: false, menuItemKeys:{imageSelect : false} },
                           attributes: { tbl_id },
        });
    }
    if (activeId) dispatchChangeActivePlotView(activeId);


    // prepare three color Plot
    if (plottingThree)  {
        const plotThreeReqAry= make3ColorPlottingList(threeReqAry);
        if (!isEmpty(plotThreeReqAry)) {
            dispatchPlotImage(
                {
                    plotId:threeCPlotId, viewerId, wpRequest:plotThreeReqAry, threeColor:true,
                               pvOptions: {userCanDeletePlots: true, menuItemKeys:{imageSelect : false}},
                    attributes: { tbl_id },
                });
        }
    }
    
}


function makePlottingList(reqAry) {
    return reqAry.filter( (r) => {
        const pv= getPlotViewById(visRoot(),r.getPlotId());
        const plot= primePlot(pv);
        if (plot) {
            return !isImageDataRequeestedEqual(plot.plotState.getWebPlotRequest(), r);
        }
        else if (get(pv,'request')) {
            return !isImageDataRequeestedEqual(pv.request,r);
        }
        else {
            return true;
        }
    });
}

function make3ColorPlottingList(req3cAry) {
    const r= req3cAry.find( (r) => Boolean(r));
    if (!r) return req3cAry;
    const p= primePlot(visRoot(),r.getPlotId());
    if (!p) return req3cAry;
    const plotState= p.plotState;
    if (!plotState) return req3cAry;
    const match= allBandAry.every( (b) => isImageDataRequeestedEqual(plotState.getWebPlotRequest(b),req3cAry[b.value]));
    return match ? [] : req3cAry;
}



function init3Color(viewerId) {
    var customEntry= Object.keys(converters).reduce( (newObj,key) => {
        if (!converters[key].threeColor) return newObj;
        newObj[key]= Object.assign({}, converters[key].threeColorBands, {threeColorVisible:false});
        return newObj;
    }, {});
    dispatchUpdateCustom(viewerId, customEntry);
}


function changeActivePlotView(plotId,tbl_id) {
    const plot= primePlot(visRoot(), plotId);
    if (!plot) return;
    const row= get(plot.attributes, PlotAttribute.TABLE_ROW, -1);
    if (row<0) return;
    const table= getTblById(tbl_id);
    if (!table) return;
    if (table.highlightedRow===row) return;
    dispatchTableHighlight(tbl_id,row,table.request);
}
