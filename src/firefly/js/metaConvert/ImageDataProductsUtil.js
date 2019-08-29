
import {union,get,isEmpty,difference} from 'lodash';
import {dispatchReplaceViewerItems, getViewerItemIds, getMultiViewRoot, isImageViewerSingleLayout} from '../visualize/MultiViewCntlr.js';
import {dispatchPlotImage, dispatchDeletePlotView, visRoot, dispatchZoom,
    dispatchPlotGroup, dispatchChangeActivePlotView} from '../visualize/ImagePlotCntlr.js';
import {isImageDataRequestedEqual} from '../visualize/WebPlotRequest.js';
import {getPlotViewById, primePlot} from '../visualize/PlotViewUtil.js';
import {UserZoomTypes} from '../visualize/ZoomUtil.js';
import {allBandAry} from '../visualize/Band.js';
import {getTblById} from '../tables/TableUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {dispatchTableHighlight} from '../tables/TablesCntlr.js';




export function createRelatedDataGridActivate(reqRet, imageViewerId, dataId, tbl_id, highlightPlotId) {
    reqRet.highlightPlotId = highlightPlotId;
    if (isEmpty(reqRet.standard)) return;
    return () => replotImageDataProducts(highlightPlotId, imageViewerId, dataId, tbl_id, reqRet.standard, reqRet.threeColor);
}


/**
 *
 * @param {Array.<WebPlotRequest>} inReqAry
 * @param {string} imageViewerId
 * @param {string} dataId
 * @param {string} tbl_id
 * @param {Array.<Object>} plotRows
 * @return {function(): void}
 */
export function createGridImagesActivate(inReqAry, imageViewerId, dataId, tbl_id, plotRows) {
    const reqAry= inReqAry.map( (r,idx) => {
        if (!r) return;
        r.setAttributes({[PlotAttribute.TABLE_ROW]: plotRows[idx].row,
                         [PlotAttribute.TABLE_ID]: tbl_id});
        r.setPlotId(plotRows[idx].plotId);
        return r;
    } );
    const pR= plotRows.filter( (pR) => pR.highlight).find( (pR) => pR.plotId);
    const highlightPlotId= pR && pR.plotId;
    return () => replotImageDataProducts(highlightPlotId, imageViewerId, dataId, tbl_id, reqAry);
}


/**
 *
 * @param {WebPlotRequest} request
 * @param {string} imageViewerId
 * @param {string} dataId
 * @param {string} tbl_id
 * @param {string} highlightedRow
 * @return {function(): void}
 */
export function createSingleImageActivate(request, imageViewerId, dataId, tbl_id, highlightedRow) {
    if (!request) return;
    request.setPlotId(`${dataId}-singleview`);
    request.setAttributes({[PlotAttribute.TABLE_ROW]: highlightedRow,
                           [PlotAttribute.TABLE_ID]: tbl_id});
    return () => replotImageDataProducts(request.getPlotId(), imageViewerId, dataId, tbl_id, [request]);
}



export function zoomPlotPerViewSize(plotId) {
    const vr= visRoot();
    if (!vr.wcsMatchType &&  isImageViewerSingleLayout(getMultiViewRoot(), vr, plotId)) {
        dispatchZoom({plotId, userZoomType: UserZoomTypes.FILL});
    }
}


export function resetImageFullGridActivePlot(tbl_id, plotIdAry) {
    if (!tbl_id || isEmpty(plotIdAry)) return;
    const {highlightedRow = 0} = getTblById(tbl_id)||{};
    const vr = visRoot();

    plotIdAry.find((pId) => {
        const plot = primePlot(vr, pId);
        if (!plot) return false;

        if (get(plot.attributes, PlotAttribute.TABLE_ROW, -1) !== highlightedRow) return false;

        dispatchChangeActivePlotView(pId);
        return true;
    });
}

export function changeActivePlotView(plotId,tbl_id) {
    const plot= primePlot(visRoot(), plotId);
    if (!plot) return;
    const row= get(plot.attributes, PlotAttribute.TABLE_ROW, -1);
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
 * @param {string} dataId the id of table table type return by makeDataProductsConverter
 * @param {string} tbl_id table id of the table with the data products
 * @param {Array.<WebPlotRequest>} reqAry an array of request to execute
 * @param {Array.<WebPlotRequest>} [threeReqAry] an array of request for a three color plot, optional, max 3 entries, r,g,b
 */
function replotImageDataProducts(activePlotId, imageViewerId, dataId, tbl_id, reqAry, threeReqAry)  {
    const groupId= `${imageViewerId}-${dataId}-standard`;
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
    reqAry.forEach( (r) => r.setPlotGroupId(groupId));


    // setup view for these plotting ids
    const inViewerIds= getViewerItemIds(getMultiViewRoot(), imageViewerId);
    const idUnionLen= union(plottingIds,inViewerIds).length;
    if (idUnionLen!== inViewerIds.length || plottingIds.length<inViewerIds.length) {
        dispatchReplaceViewerItems(imageViewerId,plottingIds);
    }


    // clean up unused Ids
    const cleanUpIds= difference(inViewerIds,plottingIds);
    cleanUpIds.forEach( (plotId) => dispatchDeletePlotView({plotId, holdWcsMatch:true}));


    // prepare standard plot
    const wpRequestAry= makePlottingList(reqAry);
    if (!isEmpty(wpRequestAry)) {
        dispatchPlotGroup({wpRequestAry, viewerId:imageViewerId, holdWcsMatch:true,
            pvOptions: { userCanDeletePlots: false, menuItemKeys:{imageSelect : false} },
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
                    pvOptions: {userCanDeletePlots: true, menuItemKeys:{imageSelect : false}},
                    attributes: { tbl_id }
                });
        }
    }

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
