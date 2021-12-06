/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get,isEmpty} from 'lodash';
import {Band} from '../Band.js';
import {TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE,TABLE_UPDATE, TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchDeletePlotView, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {REINIT_APP} from '../../core/AppDataCntlr.js';
import {getTblById,getTblInfo,getActiveTableId,isTblDataAvail} from '../../tables/TableUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import MultiViewCntlr, {getViewerItemIds, dispatchChangeViewerLayout,
                        getMultiViewRoot, getViewer, GRID, GRID_FULL, SINGLE} from '../MultiViewCntlr.js';
import {makeDataProductsConverter, initImage3ColorDisplayManagement} from '../../metaConvert/DataProductsFactory.js';
import {findGridTableRows} from '../../metaConvert/converterUtils.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {isMetaDataTable} from '../../util/VOAnalyzer.js';
import {resetImageFullGridActivePlot, changeActivePlotView} from '../../metaConvert/ImageDataProductsUtil.js';
import {
    dataProductRoot,
    dispatchUpdateDataProducts,
    getActivateParams,
    getDataProducts
} from '../../metaConvert/DataProductsCntlr';
import {dpdtMessage} from '../../metaConvert/DataProductsType';

const MAX_GRID_SIZE= 50;


/** type {TableWatcherDef} */
const DataProductsWatcherDef = {
    id : 'ImageMetaDataWatcher',
    watcher : watchDataProductsTable,
    testTable : (t) => isMetaDataTable(t.tbl_id),
    allowMultiples: false,
    actions: [TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
              TBL_RESULTS_ACTIVE, REINIT_APP,
              MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
              MultiViewCntlr.VIEWER_UNMOUNTED,
              MultiViewCntlr.CHANGE_VIEWER_LAYOUT, MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA,
              ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW,
              ImagePlotCntlr.ANY_REPLOT]
};

export function startDataProductsWatcher({dataTypeViewerId= 'DataProductsType', paused=true}) {
    dispatchAddTableTypeWatcherDef( { ...DataProductsWatcherDef, options:{dataTypeViewerId, paused} });
}



/**
 * Action watcher callback:
 * loads images on table change (only acts on image metadata tables),
 * pauses when the viewer is unmounted, resumes when it is mounted again
 * @callback actionWatcherCallback
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params
 * @param params.options.imageViewerId
 * @param params.paused
 */
function watchDataProductsTable(tbl_id, action, cancelSelf, params) {
    const {options:{dataTypeViewerId= 'DataProductsType'}} = params;
    const dpId= params.options.dpId || dataTypeViewerId;
    let {paused= true} = params;
    const {abortPromise:abortLastPromise} = params;
    const activateParams= getActivateParams(dataProductRoot(),dpId);
    const {imageViewerId}= activateParams;
    const mvRoot= getMultiViewRoot();
    const imView= getViewer(mvRoot, imageViewerId);
    const dpView= getViewer(mvRoot, dataTypeViewerId);

    if (!action) {
        if (paused) {
            paused= !Boolean(imView || dpView);
        }
        if (!paused && getActiveTableId()===tbl_id) updateDataProducts(tbl_id, activateParams);
        initImage3ColorDisplayManagement(imageViewerId); //todo: 3 color not working
        return {paused};
    }

    const {payload}= action;


    if (payload.viewerId && payload.viewerId!==imageViewerId) return params;



    if (payload.tbl_id) {
        if (payload.tbl_id!==tbl_id) return params;
        if (action.type===TABLE_REMOVE) {
            removeAllProducts(activateParams,tbl_id);
            // todo might need to remove other stuff as well: charts, images, jpeg
            cancelSelf();
            return;
        }
        if (paused && imView && dpView) paused= false;
    }
    else {
        if (getActiveTableId()!==tbl_id) return params;
    }


    let abortPromise= abortLastPromise;

    switch (action.type) {

        case TABLE_REMOVE:
        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
        case TBL_RESULTS_ACTIVE:
            if (!paused) abortPromise= updateDataProducts(tbl_id, activateParams, abortLastPromise);
            break;

        case MultiViewCntlr.CHANGE_VIEWER_LAYOUT:
        case MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA:
            if (!paused) abortPromise= updateDataProducts(tbl_id, activateParams, abortLastPromise, true);

            break;


        case MultiViewCntlr.ADD_VIEWER:
            if (payload.mounted) {
                abortPromise= updateDataProducts(tbl_id, activateParams, abortLastPromise);
                paused= false;
            }
            break;

        case MultiViewCntlr.VIEWER_MOUNTED:
            paused= false;
            abortPromise= updateDataProducts(tbl_id, activateParams, abortLastPromise);
            break;

        case MultiViewCntlr.VIEWER_UNMOUNTED:
            paused= true;
            break;

        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
            if (!paused) changeActivePlotView(action.payload.plotId,tbl_id, true);
            break;

        case ImagePlotCntlr.ANY_REPLOT:
            if (!paused) resetImageFullGridActivePlot(tbl_id, action.payload.plotIdAry);
            break;

        case REINIT_APP:
            cancelSelf();
            break;
    }
    return {paused, abortPromise};
}


const getKey= (threeOp, band) => Object.keys(threeOp).find( (k) => threeOp[k].color && threeOp[k].color.key===band.key);

/**
 * 
 * @param {string} tbl_id
 * @param {ActivateParams} activateParams
 * @param {function} abortLastPromise
 * @param {boolean} layoutChange
 * @return {function} a function to abort any unfinished promises
 */
function updateDataProducts(tbl_id, activateParams, abortLastPromise=undefined, layoutChange= false) {

    abortLastPromise && abortLastPromise();
    const {imageViewerId,dpId}= activateParams;
    let continuePromise= true;
    const abortPromiseFunc= () => continuePromise= false;
    const isPromiseAborted= ()=> !continuePromise;
    let viewer = getViewer(getMultiViewRoot(), imageViewerId);
    const foundGridFail= getDataProducts(dataProductRoot(),dpId).gridNotSupported;

    if (!viewer) return;

    const table = getTblById(tbl_id);
    // check to see if tableData is available in this range.
    if (!table || !isTblDataAvail(table.highlightedRow, table.highlightedRow + 1, table)) {
        removeAllProducts(activateParams, tbl_id);
        return;
    }

    const converter = makeDataProductsConverter(table);
    if (!converter) return;
    const {converterId}= converter;
    let threeColorOps;


    if ((!converter.canGrid && viewer.layout === GRID) ||
        (viewer.layout === GRID && viewer.layoutDetail !== GRID_FULL && !converter.hasRelatedBands)) {
        dispatchChangeViewerLayout(imageViewerId, SINGLE);
        viewer = getViewer(getMultiViewRoot(), imageViewerId);
    }


    if (converter.threeColor) {
        const showThreeColor= get(viewer.customData, [converterId,'threeColorVisible'], false);
        if (showThreeColor) {
            threeColorOps= [
                getKey(viewer.customData[converterId], Band.RED),
                getKey(viewer.customData[converterId], Band.GREEN),
                getKey(viewer.customData[converterId], Band.BLUE)
            ];
        }
    }

    const tableState= getTblInfo(table);
    const {highlightedRow}= tableState;

    // keep the plotId array for 'single' layout


    if (layoutChange && viewer.layout===SINGLE && !isEmpty(viewer.itemIdAry) && !foundGridFail) {
        if (viewer.itemIdAry[0].includes(GRID_FULL.toLowerCase())) {   // from full grid images
            const activePid = visRoot().activePlotId;

            viewer.itemIdAry.find((id) => {
                const plot = primePlot(visRoot(), id);
                if (plot && id !== activePid &&
                    get(plot.attributes, PlotAttribute.DATALINK_TABLE_ROW, -1) === highlightedRow) {
                    dispatchChangeActivePlotView(id);
                    return true;
                }
                return false;
            });
        }
        return;
    }

    let resultPromise;
    if (viewer.layout===SINGLE) {
        resultPromise= converter.getSingleDataProduct(table,highlightedRow,activateParams);
    }
    else if (viewer.layout===GRID && viewer.layoutDetail===GRID_FULL) { // keep this image only
        const plotRows= findGridTableRows(table,Math.min(converter.maxPlots,MAX_GRID_SIZE),`${converterId}-gridfull`);
        resultPromise= converter.getGridDataProduct(table,plotRows,activateParams);
    }
    else if (viewer.layout===GRID) {// keep this image only
        resultPromise= converter.getRelatedDataProduct(table,highlightedRow,threeColorOps,viewer.highlightPlotId,activateParams);
    }
    resultPromise && handleProductResult(resultPromise, dpId, isPromiseAborted, viewer);
    return abortPromiseFunc;
}

function handleProductResult(p, dpId, isPromiseAborted, imageViewer) {
    return p.then((displayTypeParams) => {
        if (isPromiseAborted()) return;
        if (displayTypeParams) {
            dispatchUpdateDataProducts(dpId,displayTypeParams);
        }
        else {
            dispatchUpdateDataProducts(dpId,dpdtMessage('Error- Search for Data product failed'));
            return;
        }
        if (displayTypeParams.gridNotSupported && imageViewer.layout===GRID) {
            dispatchChangeViewerLayout(imageViewer.viewerId, SINGLE);
        }
        if (displayTypeParams.displayType==='promise' && displayTypeParams.promise) {
            handleProductResult(displayTypeParams.promise,dpId, isPromiseAborted, imageViewer);
        }
    });
}

/**
 *
 * @param {ActivateParams} activateParams
 * @param tbl_id
 */
function removeAllProducts(activateParams, tbl_id) {
    const {dpId, imageViewerId}= activateParams;
    if (!imageViewerId) return;
    const activeTblId= getActiveTableId();
    if (activeTblId===tbl_id || !isMetaDataTable(activeTblId)) {
        const inViewerIds= getViewerItemIds(getMultiViewRoot(), imageViewerId);
        inViewerIds.forEach( (plotId) => dispatchDeletePlotView({plotId}));
        dispatchUpdateDataProducts(dpId,dpdtMessage('No Data Products', undefined, {noProductsAvailable:true}));
    }
}

