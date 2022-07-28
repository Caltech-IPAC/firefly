
import {union, get, isEmpty, difference, isArray} from 'lodash';
import {
    dispatchReplaceViewerItems, getViewerItemIds, getMultiViewRoot,
    getLayoutType, GRID, DEFAULT_FITS_VIEWER_ID, IMAGE
} from '../visualize/MultiViewCntlr.js';
import ImagePlotCntlr, {
    dispatchPlotImage, dispatchDeletePlotView, visRoot, dispatchZoom,
    dispatchPlotGroup, dispatchChangeActivePlotView } from '../visualize/ImagePlotCntlr.js';
import {AnnotationOps, isImageDataRequestedEqual} from '../visualize/WebPlotRequest.js';
import {
    getPlotViewAry, getPlotViewById, isImageExpanded, primePlot } from '../visualize/PlotViewUtil.js';
import {allBandAry} from '../visualize/Band.js';
import {getTblById} from '../tables/TableUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {dispatchTableHighlight} from '../tables/TablesCntlr.js';
import {DPtypes} from 'firefly/metaConvert/DataProductsType.js';
import {showPinMessage} from 'firefly/ui/PopupUtil.jsx';
import {dispatchAddActionWatcher} from 'firefly/core/MasterSaga.js';
import {logger} from 'firefly/util/Logger.js';




export function createRelatedDataGridActivate(reqRet, imageViewerId, tbl_id, highlightPlotId) {
    reqRet.highlightPlotId = highlightPlotId;
    if (isEmpty(reqRet.standard)) return;
    return () => replotImageDataProducts(highlightPlotId, imageViewerId, tbl_id, reqRet.standard, reqRet.threeColor);
}



/**
 *
 * @param {Array.<WebPlotRequest>} inReqAry
 * @param {string} imageViewerId
 * @param {string} tbl_id
 * @param {Array.<Object>} plotRows
 * @return {undefined|function(): void}
 */
export function createGridImagesActivate(inReqAry, imageViewerId, tbl_id, plotRows) {
    const reqAry= inReqAry
        .map( (r,idx) => {
            if (!r) return;
            if (tbl_id) {
                r.setAttributes({
                    [PlotAttribute.DATALINK_TABLE_ROW]: plotRows[idx].row+'',
                    [PlotAttribute.DATALINK_TABLE_ID]: tbl_id
                });
            }
            r.setPlotId(plotRows[idx].plotId);
            return r;
        } )
        .filter( (r) =>r);
    const pR= plotRows.filter( (pR) => pR.highlight).find( (pR) => pR.plotId);
    const highlightPlotId= pR && pR.plotId;
    return () => replotImageDataProducts(highlightPlotId, imageViewerId, tbl_id, reqAry);
}

/**
 *
 * @param {WebPlotRequest} request
 * @param {string} imageViewerId
 * @param {string} tbl_id
 * @param {string} highlightedRow
 * @return {undefined|function(): void}
 */
export function createSingleImageActivate(request, imageViewerId, tbl_id, highlightedRow) {
    if (!request) return undefined;
    if (!request.getPlotId()) request.setPlotId(`${tbl_id|'no-table'}-singleview`);
    if (tbl_id) {
        request.setAttributes({[PlotAttribute.DATALINK_TABLE_ROW]: highlightedRow+'',
            [PlotAttribute.DATALINK_TABLE_ID]: tbl_id});
    }
    return () => replotImageDataProducts(request.getPlotId(), imageViewerId, tbl_id, [request]);
}

let extractedPlotId= 1;


function copyRequest(inR) {
    const r= inR.makeCopy();
    const plotId= `extract-plotId-${extractedPlotId}`;
    r.setPlotId(plotId);
    r.setPlotGroupId('extract-group');
    extractedPlotId++;
    return r;
}

export function createSingleImageExtraction(request) {
    if (!request) return undefined;
    const wpRequest= isArray(request) ? request.map( (r) => copyRequest(r)) : copyRequest(request);
    return () => {
        dispatchPlotImage({
            plotId:isArray(wpRequest)?wpRequest.getPlotId():undefined,
            viewerId:DEFAULT_FITS_VIEWER_ID, wpRequest});
        showPinMessage('Pinning to Image Area');
    };
}


/** @type actionWatcherCallback */
function watchForCompletedPlot(action, cancelSelf, params) {
    const {afterComplete, plotId}= params;
    const {payload,type}= action;

    if (type===ImagePlotCntlr.PLOT_IMAGE_FAIL) {
        if (payload.plotId!==plotId) return;
        cancelSelf();
        return;
    }
    if (type===ImagePlotCntlr.PLOT_IMAGE) {
        if (!payload.pvNewPlotInfoAry.some( (n) => n.plotId===plotId)) {
            return;
        }
        afterComplete();
        cancelSelf();
        return;
    }
    logger.warn('watchForCompletedPlot: should never get here.');
    cancelSelf();
}

export function zoomPlotPerViewSize(plotId, zoomType) {
    const afterComplete= () => {
        if (!visRoot().wcsMatchType) {
            dispatchZoom({plotId, userZoomType: zoomType});
        }
    };
    const pv= getPlotViewById(visRoot(),plotId);
    if (pv.serverCall==='working') {
        dispatchAddActionWatcher( {
            callback: watchForCompletedPlot,
            params: {plotId, afterComplete},
            actions: [ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_FAIL]
        } );
    }
    else {
        afterComplete();
    }
}

///=========================

export function resetImageFullGridActivePlot(tbl_id, plotIdAry) {
    if (!tbl_id || isEmpty(plotIdAry)) return;

    const {highlightedRow = 0} = getTblById(tbl_id)||{};
    const vr = visRoot();

    plotIdAry.find((pId) => {
        const plot = primePlot(vr, pId);
        if (!plot) return false;

        if (Number(get(plot.attributes, PlotAttribute.DATALINK_TABLE_ROW, -1)) !== highlightedRow) return false;

        dispatchChangeActivePlotView(pId);
        return true;
    });
}

export function changeActivePlotView(plotId,tbl_id) {
    const plot= primePlot(visRoot(), plotId);
    if (!plot) return;
    const row= Number(get(plot.attributes, PlotAttribute.DATALINK_TABLE_ROW, -1));
    if (row<0) return;
    const table= getTblById(tbl_id);
    if (!table) return;
    if (table.highlightedRow===row) return;
    dispatchTableHighlight(tbl_id,row,table.request);
}


/**
 *
 * @param {string} activePlotId the new active plot id after the replot
 * @param {string} imageViewerId the id of the viewer
 * @param {string} tbl_id table id of the table with the data products
 * @param {Array.<WebPlotRequest>} reqAry an array of request to execute
 * @param {Array.<WebPlotRequest>} [threeReqAry] an array of request for a three color plot, optional, max 3 entries, r,g,b
 */
function replotImageDataProducts(activePlotId, imageViewerId, tbl_id, reqAry, threeReqAry)  {
    const groupId= `${imageViewerId}-${tbl_id||'no-table-group'}-standard`;
    reqAry= reqAry.filter( (r) => r);
    let plottingIds= reqAry.map( (r) =>  r && r.getPlotId()).filter( (id) => id);
    let threeCPlotId;
    let plottingThree= false;

    // determine with we have a valid three color plot request array and add it to the list
    if (!isEmpty(threeReqAry)) {
        const r= threeReqAry.find( (r) => Boolean(r));
        if (r) {
            plottingThree= true;
            threeCPlotId= r.getPlotId();
            plottingIds= [...plottingIds,threeCPlotId];
            threeReqAry.forEach( (r) => r && r.setPlotGroupId(groupId) );
        }
    }
    // prepare each request
    reqAry= reqAry.map( (r) => {
        const newR= r.makeCopy();
        newR.setPlotGroupId(groupId);
        newR.setAnnotationOps(AnnotationOps.INLINE);
        return newR;
    });


    // setup view for these plotting ids
    const inViewerIds= getViewerItemIds(getMultiViewRoot(), imageViewerId);
    const idUnionLen= union(plottingIds,inViewerIds).length;
    if (idUnionLen!== inViewerIds.length || plottingIds.length<inViewerIds.length) {
        dispatchReplaceViewerItems(imageViewerId,plottingIds,IMAGE);
    }


    // clean up unused Ids
    const cleanUpIds= difference(inViewerIds,plottingIds);
    cleanUpIds.forEach( (plotId) => dispatchDeletePlotView({plotId, holdWcsMatch:true}));


    // prepare standard plot
    const wpRequestAry= makePlottingList(reqAry);
    if (!isEmpty(wpRequestAry)) {
        dispatchPlotGroup({wpRequestAry, viewerId:imageViewerId, holdWcsMatch:true,
            setNewPlotAsActive: !activePlotId,
            pvOptions: { userCanDeletePlots: false, menuItemKeys:{imageSelect : false}, useSticky:true },
            attributes: { tbl_id }
        });
    }
    if (activePlotId) dispatchChangeActivePlotView(activePlotId);


    // prepare three color Plot
    if (plottingThree)  {
        const plotThreeReqAry= make3ColorPlottingList(threeReqAry);
        if (!isEmpty(plotThreeReqAry)) {
            dispatchPlotImage(
                {
                    plotId:threeCPlotId, viewerId:imageViewerId, wpRequest:plotThreeReqAry, threeColor:true,
                    pvOptions: {userCanDeletePlots: false, menuItemKeys:{imageSelect : false}},
                    attributes: { tbl_id }
                });
        }
    }
    // const layoutType= getLayoutType(getMultiViewRoot(),imageViewerId);

    return ({nextDisplayType, nextMetaDataTableId}) => { // return the cleanup function
        if (isImageExpanded(visRoot().expandedMode)) return;
        const layoutType= getLayoutType(getMultiViewRoot(),imageViewerId);
        if (nextDisplayType===DPtypes.IMAGE && layoutType===GRID && tbl_id===nextMetaDataTableId) {
            return;
        }
        // !isEmpty(wpRequestAry) && wpRequestAry.forEach( (wpR) => dispatchDeletePlotView({plotId:wpR.getPlotId()}) );
        getPlotViewAry(visRoot())
            .filter( (pv) => pv.plotGroupId===groupId)
            .forEach( (pv) => dispatchDeletePlotView({plotId:pv.plotId}) );
        plottingThree && dispatchDeletePlotView({plotId:threeCPlotId});
    };
}

function makePlottingList(reqAry) {
    return reqAry.filter( (r) => {
        const pv= getPlotViewById(visRoot(),r.getPlotId());
        const plot= primePlot(pv);
        if (plot) {
            return !isImageDataRequestedEqual(plot.plotState.getWebPlotRequest(), r);
        }
        else if (get(pv,'request')) {
            return !isImageDataRequestedEqual(pv.request,r);
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
    const match= allBandAry.every( (b) => isImageDataRequestedEqual(plotState.getWebPlotRequest(b),req3cAry[b.value]));
    return match ? [] : req3cAry;
}
