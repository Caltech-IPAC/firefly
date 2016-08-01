/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {union,get,isEmpty,difference} from 'lodash';
import {Band,allBandAry} from '../Band.js';
import {TABLE_NEW,TABLE_SELECT,TABLE_HIGHLIGHT,
        TABLE_REMOVE,TABLE_UPDATE, TBL_RESULTS_ACTIVE, dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchPlotImage, dispatchDeletePlotView,
                        dispatchPlotGroup, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {REINIT_RESULT_VIEW} from '../../core/AppDataCntlr.js';
import {getTblById,getTblInfo,getActiveTableId,isTblDataAvail} from '../../tables/TableUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import MultiViewCntlr, {dispatchReplaceImages, dispatchUpdateCustom, getViewerPlotIds,
                        getMultiViewRoot, getViewer, GRID, GRID_FULL, SINGLE} from '../MultiViewCntlr.js';
import {converterFactory, converters} from '../../metaConvert/ConverterFactory.js';
import {findGridTableRows,isMetaDataTable} from '../../metaConvert/converterUtils.js';
// import {getActiveTableId} from '../../core/LayoutCntlr.js';
import {PlotAttribute} from '../WebPlot.js';

const MAX_GRID_SIZE= 10; //todo when post is working, increase to 16 or 24

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
export function* watchImageMetaData({viewerId}) {

    var tbl_id;
    var paused= true;
    while (true) {
        const action= yield take([TABLE_NEW,TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
                                  TBL_RESULTS_ACTIVE,
                                  MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
                                  MultiViewCntlr.VIEWER_UNMOUNTED,
                                  MultiViewCntlr.CHANGE_LAYOUT, MultiViewCntlr.UPDATE_CUSTOM_DATA,
                                  ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW,
                                  REINIT_RESULT_VIEW]);
        const {payload}= action;

        if (payload.viewerId && payload.viewerId!==viewerId) continue;

        if (action.type===TABLE_REMOVE) {
            tbl_id= getActiveTableId();
        }
        else if (payload.tbl_id) {
            if (!isMetaDataTable(payload.tbl_id)) continue;
            tbl_id= payload.tbl_id; // otherwise use the last one
        }


        switch (action.type) {

            case TABLE_NEW:
            case TABLE_REMOVE:
            case TABLE_HIGHLIGHT:
            case TABLE_UPDATE:
            case TBL_RESULTS_ACTIVE:
                if (!paused) updateImagePlots(tbl_id, viewerId);
                break;

            case MultiViewCntlr.CHANGE_LAYOUT:
            case MultiViewCntlr.UPDATE_CUSTOM_DATA:
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

    if (!tbl_id) return [];
    var reqRet;
    const table= getTblById(tbl_id);
    // check to see if tableData is available in this range.
    if (!isTblDataAvail(table.highlightedRow, table.highlightedRow+1, table)) return [];
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    if (!table) return [];

    const converterData= converterFactory(table);
    const {dataId,converter}= converterData;
    var highlightPlotId;
    var threeColorOps;
    var threeReqAry;

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

    if (layoutChange && viewer.layout===SINGLE && !isEmpty(viewer.plotIdAry)) {
        return;
    }
    

    if (viewer.layout===SINGLE) {
        const {single}= converter.makeRequest(table,highlightedRow,true);
        reqAry= [single];
        single.setPlotId(`${dataId}-singleview`,0);
        single.setRelatedTableRow(highlightedRow);
        highlightPlotId= single.getPlotId();
    }
    else if (viewer.layout===GRID && viewer.layoutDetail===GRID_FULL) {
        const plotRows= findGridTableRows(table,Math.min(converter.maxPlots,MAX_GRID_SIZE),`${dataId}-gridfull`);
        var reqAry= plotRows.map( (pR) => {
            const {single}= converter.makeRequest(table,pR.row,true);
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
        threeReqAry= reqRet.threeColor;
    }

    replot(reqAry, threeReqAry, highlightPlotId, viewerId, dataId);
}


function replot(reqAry, threeReqAry, activeId, viewerId, dataId)  {
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
    const inViewerIds= getViewerPlotIds(getMultiViewRoot(), viewerId);
    const idUnionLen= union(plottingIds,inViewerIds).length;
    if (idUnionLen!== inViewerIds.length || plottingIds.length<inViewerIds.length) {
        dispatchReplaceImages(viewerId,plottingIds);
    }


    // clean up unused Ids
    const cleanUpIds= difference(inViewerIds,plottingIds);
    cleanUpIds.forEach( (plotId) => dispatchDeletePlotView({plotId}));


    // prepare stand plot
    const wpRequestAry= makePlottingList(reqAry);
    if (!isEmpty(wpRequestAry)) dispatchPlotGroup({wpRequestAry, pvOptions: {menuItemKeys:{imageSelect : false}}});
    if (activeId) dispatchChangeActivePlotView(activeId);


    // prepare three color Plot
    if (plottingThree)  {
        const plotThreeReqAry= make3ColorPlottingList(threeReqAry);
        if (!isEmpty(plotThreeReqAry)) {
            dispatchPlotImage(
                {
                    plotId:threeCPlotId, wpRequest:plotThreeReqAry, threeColor:true,
                               pvOptions: {menuItemKeys:{imageSelect : false}}
                });
        }
    }
    
}

function copyAndCleanReqString(r) {
    if (!r) return '';
    r= r.makeCopy();
    r.setZoomToWidth(1);
    r.setZoomToHeight(1);
    r.setProgressKey('');
    r.setInitialRangeValues();
    r.setInitialColorTable(0);
    return r.toString();
}

const isCleanReqEqual= (r1,r2) => copyAndCleanReqString(r1)===copyAndCleanReqString(r2);


function makePlottingList(reqAry) {
    return reqAry.filter( (r) => {
        const plot= primePlot(visRoot(),r.getPlotId());
        if (!plot) return true;
        return !isCleanReqEqual(plot.plotState.getWebPlotRequest(),r);
    });
}

function make3ColorPlottingList(req3cAry) {
    const r= req3cAry.find( (r) => Boolean(r));
    if (!r) return req3cAry;
    const p= primePlot(visRoot(),r.getPlotId());
    if (!p) return req3cAry;
    const plotState= p.plotState;
    if (!plotState) return req3cAry;
    const match= allBandAry.every( (b) => isCleanReqEqual(plotState.getWebPlotRequest(b),req3cAry[b.value]));
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
