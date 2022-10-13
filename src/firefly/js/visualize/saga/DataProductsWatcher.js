/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isArray, isEmpty} from 'lodash';
import {Band} from '../Band.js';
import {TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE,TABLE_UPDATE, TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr, {visRoot, dispatchDeletePlotView, dispatchChangeActivePlotView} from '../ImagePlotCntlr.js';
import {REINIT_APP} from '../../core/AppDataCntlr.js';
import {getTblById,getTblInfo,getActiveTableId,isTblDataAvail} from '../../tables/TableUtil.js';
import {primePlot} from '../PlotViewUtil.js';
import MultiViewCntlr, {
    getViewerItemIds, dispatchChangeViewerLayout,
    getMultiViewRoot, getViewer, GRID, GRID_FULL, SINGLE, getLayoutType, getLayoutDetails
} from '../MultiViewCntlr.js';
import {makeDataProductsConverter, initImage3ColorDisplayManagement} from '../../metaConvert/DataProductsFactory.js';
import {findGridTableRows} from '../../metaConvert/converterUtils.js';
import {PlotAttribute} from '../PlotAttribute.js';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {isDataProductsTable} from '../../util/VOAnalyzer.js';
import {zoomPlotPerViewSize, resetImageFullGridActivePlot, changeActivePlotView} from '../../metaConvert/ImageDataProductsUtil.js';
import {
    dataProductRoot,
    dispatchUpdateDataProducts,
    getActivateParams,
    getDataProducts
} from '../../metaConvert/DataProductsCntlr';
import {dpdtMessage} from '../../metaConvert/DataProductsType';
import {UserZoomTypes} from 'firefly/visualize/ZoomUtil.js';

const MAX_GRID_SIZE= 50;


/** type {TableWatcherDef} */
const getDataProductsWatcherDef = () => ({
    id : 'ImageMetaDataWatcher',
    watcher : watchDataProductsTable,
    testTable : (t) => isDataProductsTable(t.tbl_id),
    allowMultiples: false,
    actions: [TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
              TBL_RESULTS_ACTIVE, REINIT_APP,
              MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
              MultiViewCntlr.VIEWER_UNMOUNTED,
              MultiViewCntlr.CHANGE_VIEWER_LAYOUT, MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA,
              ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW,
              ImagePlotCntlr.UPDATE_VIEW_SIZE, ImagePlotCntlr.ANY_REPLOT]
});

export function startDataProductsWatcher({dataTypeViewerId= 'DataProductsType', paused=true}) {
    dispatchAddTableTypeWatcherDef( { ...getDataProductsWatcherDef(), options:{dataTypeViewerId, paused} });
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
    let firstTime= params.firstTime ?? true;
    const dpId= params.options.dpId || dataTypeViewerId;
    let {paused= true, zoomOnNextViewSizeChange=false} = params;
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
        if (!paused && getActiveTableId()===tbl_id) {
            updateDataProducts(null, firstTime, tbl_id, activateParams);
            firstTime= false;
        }
        initImage3ColorDisplayManagement(imageViewerId);
        return {paused, firstTime};
    }

    const {payload}= action;


    if (payload.viewerId && payload.viewerId!==imageViewerId) return {...params,firstTime};



    if (payload.tbl_id) {
        if (payload.tbl_id!==tbl_id) return {...params,firstTime};
        if (action.type===TABLE_REMOVE) {
            removeAllProducts(activateParams,tbl_id);
            // todo might need to remove other stuff as well: charts, images, jpeg
            cancelSelf();
            return;
        }
        if (paused && imView && dpView) paused= false;
    }
    else {
        if (getActiveTableId()!==tbl_id) return {...params, firstTime};
    }


    let abortPromise= abortLastPromise;

    switch (action.type) {

        case TABLE_REMOVE:
        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
        case TBL_RESULTS_ACTIVE:
            if (!paused) {
                abortPromise= updateDataProducts(action, firstTime, tbl_id, activateParams, abortLastPromise);
                firstTime= false;
            }
            zoomOnNextViewSizeChange= false;
            break;

        case MultiViewCntlr.CHANGE_VIEWER_LAYOUT:
        case MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA:
            if (!paused) {
                abortPromise= updateDataProducts(action, firstTime, tbl_id, activateParams, abortLastPromise);
                firstTime= false;
            }
            zoomOnNextViewSizeChange= true;
            break;


        case MultiViewCntlr.ADD_VIEWER:
            if (payload.mounted) {
                abortPromise= updateDataProducts(action, firstTime, tbl_id, activateParams, abortLastPromise);
                firstTime= false;
                paused= false;
                zoomOnNextViewSizeChange= false;
            }
            break;

        case MultiViewCntlr.VIEWER_MOUNTED:
            paused= false;
            abortPromise= updateDataProducts(action, firstTime, tbl_id, activateParams, abortLastPromise);
            firstTime= false;
            zoomOnNextViewSizeChange= false;
            break;

        case MultiViewCntlr.VIEWER_UNMOUNTED:
            paused= true;
            zoomOnNextViewSizeChange= false;
            break;

        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
            if (!paused) changeActivePlotView(action.payload.plotId,tbl_id, true);
            break;

        case ImagePlotCntlr.ANY_REPLOT:
            if (!paused) resetImageFullGridActivePlot(tbl_id, action.payload.plotIdAry);
            zoomOnNextViewSizeChange= false;
            break;

        case ImagePlotCntlr.UPDATE_VIEW_SIZE:
            if (!paused && zoomOnNextViewSizeChange) {
                zoomPlotPerViewSize(payload.plotId, UserZoomTypes.FIT);
            }
            break;

        case REINIT_APP:
            cancelSelf();
            zoomOnNextViewSizeChange= false;
            break;
    }
    return {paused, abortPromise, firstTime, zoomOnNextViewSizeChange};
}


const getKey= (threeOp, band) =>
    Object.keys(threeOp).find( (k) =>
                   isArray(threeOp[k].color) ? threeOp[k].color.includes(band) : threeOp[k].color===band );

/**
 *
 * @param {Action} action
 * @param {boolean} firstTime
 * @param {string} tbl_id
 * @param {ActivateParams} activateParams
 * @param {function} abortLastPromise
 * @return {function} a function to abort any unfinished promises
 */
function updateDataProducts(action, firstTime, tbl_id, activateParams, abortLastPromise=undefined) {

    abortLastPromise && abortLastPromise();

    const layoutChange= action?.type===MultiViewCntlr.CHANGE_VIEWER_LAYOUT ||
        action?.type===MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA;


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
    const {converterId, initialLayout=SINGLE, canGrid= false, threeColor, hasRelatedBands}=  converter;
    let threeColorOps;

    if (firstTime) {
        if (canGrid && initialLayout!==SINGLE) {
            dispatchChangeViewerLayout(viewer.viewerId,GRID,initialLayout,tbl_id);
        }
        else if (initialLayout===SINGLE) {
            dispatchChangeViewerLayout(viewer.viewerId,SINGLE,undefined,tbl_id);
        }
        viewer = getViewer(getMultiViewRoot(), imageViewerId);
    }

    if (threeColor) {
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
    const layout= getLayoutType(getMultiViewRoot(), imageViewerId, tbl_id);
    const layoutDetail= getLayoutDetails(getMultiViewRoot(), imageViewerId, tbl_id);


    if (layoutChange && layout===SINGLE && !isEmpty(viewer.itemIdAry) && !foundGridFail) {
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
    if (layout===SINGLE) {
        resultPromise= converter.getSingleDataProduct(table,highlightedRow,activateParams);
    }
    else if (layout===GRID && layoutDetail===GRID_FULL) { // keep this image only
        const plotRows= findGridTableRows(table,Math.min(converter.maxPlots,MAX_GRID_SIZE),`${converterId}-gridfull`);
        resultPromise= converter.getGridDataProduct(table,plotRows,activateParams);
    }
    else if (layout===GRID) {// keep this image only
        resultPromise= converter.getRelatedDataProduct(table,highlightedRow,threeColorOps,viewer.highlightPlotId,activateParams);
    }
    resultPromise && handleProductResult(resultPromise, dpId, tbl_id, isPromiseAborted, viewer, layout);
    return abortPromiseFunc;
}

function handleProductResult(p, dpId, tbl_id, isPromiseAborted, imageViewer, layout) {
    return p.then((displayTypeParams) => {
        if (isPromiseAborted()) return;
        if (displayTypeParams) {
            dispatchUpdateDataProducts(dpId,displayTypeParams);
        }
        else {
            dispatchUpdateDataProducts(dpId,dpdtMessage('Error- Search for Data product failed'));
            return;
        }
        if (displayTypeParams.gridNotSupported && layout===GRID) {
            dispatchChangeViewerLayout(imageViewer.viewerId, SINGLE, undefined, tbl_id);
        }
        if (displayTypeParams.displayType==='promise' && displayTypeParams.promise) {
            handleProductResult(displayTypeParams.promise,dpId, tbl_id, isPromiseAborted, imageViewer, layout);
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
    if (activeTblId===tbl_id || !isDataProductsTable(activeTblId)) {
        const inViewerIds= getViewerItemIds(getMultiViewRoot(), imageViewerId);
        inViewerIds.forEach( (plotId) => dispatchDeletePlotView({plotId}));
        dispatchUpdateDataProducts(dpId,dpdtMessage('No Data Products', undefined, {noProductsAvailable:true}));
    }
}

