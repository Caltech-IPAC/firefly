/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isArray} from 'lodash';
import ComponentCntlr from '../core/ComponentCntlr.js';
import {getHttpErrorMessage} from '../util/HttpErrorMessage.js';
import {getStatusFromFetchError} from '../util/WebUtil.js';
import {isDataProductsTable} from '../voAnalyzer/TableAnalysis.js';
import {Band} from '../visualize/Band.js';
import {TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_REMOVE, TABLE_UPDATE, TBL_RESULTS_ACTIVE} from '../tables/TablesCntlr.js';
import {dispatchDeletePlotView} from '../visualize/ImagePlotDispatch';
import {
    ADD_VIEWER, ANY_REPLOT, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_VIEWER_LAYOUT, GRID, GRID_FULL, GRID_RELATED,
    MOUSE_CLICK_REASON, SINGLE,
    UPDATE_VIEW_SIZE, UPDATE_VIEWER_CUSTOM_DATA, UserZoomTypes, VIEWER_MOUNTED, VIEWER_UNMOUNTED
} from '../visualize/VisConst';
import {REINIT_APP} from '../core/CoreConst';
import {getTblById,getTblInfo,getActiveTableId,isTblDataAvail} from '../tables/TableUtil.js';
import {isDefaultCoverageActive} from '../visualize/PlotViewUtil.js';
import {
    getViewerItemIds, dispatchChangeViewerLayout, getMultiViewRoot, getViewer, getLayoutType, getLayoutDetails
} from '../visualize/MultiViewCntlr.js';
import {
    makeDataProductsConverter, getFactoryTemplateOptions
} from './DataProductsFactory.js';
import {findGridTableRows} from './TableDataProductUtils.js';
import {dispatchAddTableTypeWatcherDef} from '../core/MasterSaga.js';
import {zoomPlotPerViewSize, resetImageFullGridActivePlot, changeTableHighlightToMatchPlotView} from './ImageDataProductsUtil.js';
import {
    dataProductRoot, dispatchUpdateDataProducts, getActivateParams
} from './DataProductsCntlr.js';
import {dpdtMessage, dpdtSimpleMsg, DPtypes} from './DataProductsType.js';
import {DEFAULT_DATA_PRODUCTS_COMPONENT_KEY} from 'firefly/metaConvert/DataProductConst';

const MAX_GRID_SIZE= 50;


/** type {TableWatcherDef} */
const getDataProductsWatcherDef = () => ({
    watcher : watchDataProductsTable,
    testTable : (t) => isDataProductsTable(t.tbl_id),
    allowMultiples: false,
    actions: [TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
              TBL_RESULTS_ACTIVE, REINIT_APP,
              ADD_VIEWER, VIEWER_MOUNTED, VIEWER_UNMOUNTED, CHANGE_VIEWER_LAYOUT, UPDATE_VIEWER_CUSTOM_DATA,
              ComponentCntlr.COMPONENT_STATE_CHANGE, CHANGE_ACTIVE_PLOT_VIEW, UPDATE_VIEW_SIZE, ANY_REPLOT]
});

export function startDataProductsWatcher({dataTypeViewerId= DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, factoryKey}) {
    dispatchAddTableTypeWatcherDef( { ...getDataProductsWatcherDef(), id: dataTypeViewerId, options:{dataTypeViewerId, factoryKey} });
}

/**
 * The first call, when the table loads: start paused unless a viewer is there, and show the products if this
 * table is the active one.
 * @param {string} tbl_id
 * @param {string} factoryKey
 * @param {ActivateParams} activateParams
 * @param {boolean} viewerExists - the image viewer or the data products viewer is there
 * @return {{paused: boolean, firstTime: boolean}} the watcher's starting state
 */
function startWatching(tbl_id, factoryKey, activateParams, viewerExists) {
    const paused= !viewerExists;
    if (paused || getActiveTableId()!==tbl_id) return {paused, firstTime: true};
    updateDataProducts(factoryKey, null, true, tbl_id, activateParams);
    return {paused, firstTime: false};
}

/**
 * Remove this table's products, if they are the ones showing, and stop the watcher.
 * @param {ActivateParams} activateParams
 * @param {string} tbl_id
 * @param {Function} cancelSelf
 */
function stopWatching(activateParams,tbl_id,cancelSelf) {
    removeAllProducts(activateParams,tbl_id);
    // todo might need to remove other stuff as well: charts, images, jpeg
    cancelSelf();
}

/**
 * Whether the watcher is paused after this action.
 * @param {boolean} paused - whether it was paused before the action
 * @param {Action} action
 * @param {Object} imView - the image viewer
 * @param {Object} dpView - the data products viewer
 * @return {boolean}
 */
function getPauseState(paused, action, imView, dpView) {
    if (action.type===VIEWER_UNMOUNTED) return true;
    if (!paused) return false;
    // an action for this table wakes the watcher once its viewers are there
    if (getPayloadTblId(action) && ((imView && dpView) || imView?.mounted)) return false;
    // so does the image viewer being added mounted, or mounting
    if (action.type===ADD_VIEWER && action.payload.mounted) return false;
    if (action.type===VIEWER_MOUNTED) return false;
    return true;
}

/**
 * @param {Action} action
 * @return {string|undefined} the table the action is for, if it names one
 */
function getPayloadTblId({type, payload}) {
    if (type===ComponentCntlr.COMPONENT_STATE_CHANGE) return payload?.componentState?.tbl_id;
    return payload.tbl_id ?? payload.associatedTblId;
}

/**
 * An action counts when it is for no viewer or for the image viewer, and either names this table or names no
 * table while this table is the active one.
 * @param {Action} action
 * @param {string} tbl_id
 * @param {string} imageViewerId
 * @return {boolean}
 */
function isActionForThisTable(action, tbl_id, imageViewerId) {
    const {viewerId}= action.payload;
    if (viewerId && viewerId!==imageViewerId) return false;
    const payloadTblId= getPayloadTblId(action);
    if (payloadTblId) return payloadTblId===tbl_id;
    return getActiveTableId()===tbl_id;
}

/**
 * Action watcher callback:
 * loads images on table change (only acts on image metadata tables),
 * pauses when the viewer is unmounted, resumes when it is mounted again
 * @callback actionWatcherCallback
 * @param tbl_id
 * @param action
 * @param cancelSelf
 * @param params - the state this function returned last time, plus the options
 * @param params.options.dataTypeViewerId
 * @param params.options.factoryKey
 * @param params.paused
 * @param params.firstTime
 * @param params.abortPromise
 * @param params.zoomOnNextViewSizeChange
 */
function watchDataProductsTable(tbl_id, action, cancelSelf, params) {
    const {options:{dataTypeViewerId= DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, factoryKey}} = params;
    const activateParams= getActivateParams(dataProductRoot(),dataTypeViewerId);
    const {imageViewerId}= activateParams;
    const mvRoot= getMultiViewRoot();
    const imView= getViewer(mvRoot, imageViewerId);
    const dpView= getViewer(mvRoot, dataTypeViewerId);

    if (!action) return startWatching(tbl_id, factoryKey, activateParams, Boolean(imView || dpView));
    if (!isActionForThisTable(action, tbl_id, imageViewerId)) return params;
    if (action.type===TABLE_REMOVE) return stopWatching(activateParams,tbl_id,cancelSelf);

    const {payload}= action;
    const {dataProductsComponentKey}= getFactoryTemplateOptions(factoryKey) ??{};
    let {firstTime= true, zoomOnNextViewSizeChange= false}= params;
    const {paused:currentlyPaused= true, abortPromise:abortLastPromise} = params;
    const paused= getPauseState(currentlyPaused,action,imView,dpView);

    let abortPromise= abortLastPromise;
    const updateProducts= () => {
        if (paused) return;
        abortPromise= updateDataProducts(factoryKey, action, firstTime, tbl_id, activateParams, abortLastPromise);
        firstTime= false;
    };

    switch (action.type) {

        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
        case TBL_RESULTS_ACTIVE:
            updateProducts();
            zoomOnNextViewSizeChange= false;
            break;

        case CHANGE_VIEWER_LAYOUT:
        case UPDATE_VIEWER_CUSTOM_DATA:
            updateProducts();
            zoomOnNextViewSizeChange= true;
            break;


        case ADD_VIEWER:
            if (payload.mounted) {
                updateProducts();
                zoomOnNextViewSizeChange= false;
            }
            break;

        case VIEWER_MOUNTED:
            updateProducts();
            zoomOnNextViewSizeChange= false;
            break;

        case VIEWER_UNMOUNTED:
            zoomOnNextViewSizeChange= false;
            break;

        case ComponentCntlr.COMPONENT_STATE_CHANGE:
            if (payload.componentId===dataProductsComponentKey) updateProducts();
            break;

        case CHANGE_ACTIVE_PLOT_VIEW:
            if (!paused && payload.reason===MOUSE_CLICK_REASON) {
                changeTableHighlightToMatchPlotView(payload.plotId,tbl_id);
            }
            break;

        case ANY_REPLOT:
            if (!paused) {
                const makeActive= !isDefaultCoverageActive(getMultiViewRoot());
                if (makeActive) resetImageFullGridActivePlot(tbl_id, payload.plotIdAry);
            }
            zoomOnNextViewSizeChange= false;
            break;

        case UPDATE_VIEW_SIZE:
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
 * The abort handle for a product request. The last request is aborted now, unless it is kept loading; then
 * aborting this request aborts that one as well.
 * @param {Function} [abortLast] - the last request's abort function
 * @param {boolean} keepLastLoading
 * @return {{abort: Function, isAborted: Function}}
 */
function makeAbortHandle(abortLast, keepLastLoading) {
    if (!keepLastLoading) abortLast?.();
    let aborted= false;
    return {
        abort: () => {
            aborted= true;
            if (keepLastLoading) abortLast?.();
        },
        isAborted: () => aborted,
    };
}

/**
 *
 * @param {String} factoryKey
 * @param {Action} action
 * @param {boolean} firstTime
 * @param {string} tbl_id
 * @param {ActivateParams} activateParams
 * @param {function} abortLastPromise
 * @return {function} a function to abort any unfinished promises
 */
function updateDataProducts(factoryKey, action, firstTime, tbl_id, activateParams, abortLastPromise=undefined) {

    // a layout change leaves the last product loading
    const keepLastLoading= action?.type===CHANGE_VIEWER_LAYOUT || action?.type===UPDATE_VIEWER_CUSTOM_DATA;
    const {abort, isAborted}= makeAbortHandle(abortLastPromise, keepLastLoading);
    const {imageViewerId,dpId}= activateParams;

    if (!getViewer(getMultiViewRoot(), imageViewerId)) return abort;

    const table = getTblById(tbl_id);
    // check to see if tableData is available in this range.
    if (!table || !isTblDataAvail(table.highlightedRow, table.highlightedRow + 1, table)) {
        removeAllProducts(activateParams, tbl_id);
        return abort;
    }

    const converter = makeDataProductsConverter(table, factoryKey);
    if (!converter) return abort;

    const {layout, layoutDetail}= resolveLayout(converter, firstTime, imageViewerId, tbl_id);
    const viewer= getViewer(getMultiViewRoot(), imageViewerId);
    const resultPromise= requestProduct(converter, layout, layoutDetail, table, viewer, activateParams);
    resultPromise && void handleProductResultPromise(resultPromise, dpId, tbl_id, isAborted, viewer, layout);
    return abort;
}

/**
 * Set this table's layout in the image viewer to one the converter can show: its initial layout the first time,
 * then single instead of a related grid it has no bands for, or instead of a grid it cannot make.
 * @param {DataProductsConvertType} converter
 * @param {boolean} firstTime
 * @param {string} imageViewerId
 * @param {string} tbl_id
 * @return {{layout: string, layoutDetail: string}} the table's layout after any change
 */
function resolveLayout(converter, firstTime, imageViewerId, tbl_id) {
    const {initialLayout=SINGLE, canGrid= false, hasRelatedBands}= converter;
    const getTblLayout= () => getLayoutType(getMultiViewRoot(), imageViewerId, tbl_id);
    const getTblLayoutDetail= () => getLayoutDetails(getMultiViewRoot(), imageViewerId, tbl_id);
    const setSingleLayout= () => dispatchChangeViewerLayout(imageViewerId,SINGLE,undefined,tbl_id);

    if (firstTime) {
        if (canGrid && initialLayout!==SINGLE) dispatchChangeViewerLayout(imageViewerId,GRID,initialLayout,tbl_id);
        else if (initialLayout===SINGLE) setSingleLayout();
    }
    // use this table's layout, not viewer.layout: changeLayout (MultiViewCntlr.js) skips the update when the table's
    // layout already matches, leaving viewer.layout stale (e.g. GRID_RELATED from another table). Checking viewer.layout
    // here would redispatch SINGLE in an endless loop, since each CHANGE_VIEWER_LAYOUT re-runs this check
    else if (getTblLayout()===GRID && getTblLayoutDetail()===GRID_RELATED && !hasRelatedBands) {
        setSingleLayout();
    }

    const layoutDetail= getTblLayoutDetail();
    if (getTblLayout()===GRID && !canGrid) setSingleLayout();
    return {layout: getTblLayout(), layoutDetail};
}

/**
 * Ask the converter for the product that fits the layout: one row, a full grid of rows, or the row's related images.
 * @param {DataProductsConvertType} converter
 * @param {string} layout
 * @param {string} layoutDetail
 * @param {TableModel} table
 * @param {Object} viewer - the image viewer
 * @param {ActivateParams} activateParams
 * @return {Promise|undefined} the product, or undefined for an unknown layout
 */
function requestProduct(converter, layout, layoutDetail, table, viewer, activateParams) {
    const {converterId, threeColor, options={}}=  converter;
    const {highlightedRow}= getTblInfo(table);
    if (layout===SINGLE) return converter.getSingleDataProduct(table,highlightedRow,activateParams, options);
    if (layout!==GRID) return undefined;
    if (layoutDetail===GRID_FULL) {
        const plotRows= findGridTableRows(table,Math.min(converter.maxPlots,MAX_GRID_SIZE),`${converterId}-gridfull`);
        return converter.getGridDataProduct(table,plotRows,activateParams, options);
    }
    else {
        const threeData= viewer.customData?.[converterId];
        const threeColorOps= (threeColor && threeData?.threeColorVisible) ?
            [getKey(threeData, Band.RED), getKey(threeData, Band.GREEN), getKey(threeData, Band.BLUE)] : undefined;
        return converter.getRelatedDataProduct(table,highlightedRow,threeColorOps,viewer.highlightPlotId,activateParams, options);
    }
}

async function handleProductResultPromise(p, dpId, tbl_id, isPromiseAborted, imageViewer, layout) {
    const displayTypeParams= await p;
    if (isPromiseAborted()) return;
    if (!displayTypeParams  || displayTypeParams.displayType===DPtypes.ERROR) {
        const {error,url}= displayTypeParams ?? {};
        const status= getStatusFromFetchError(error?.message ?? 'Unknown Error');
        const msg= !status
            ? dpdtSimpleMsg('Error- Search for Data product failed')
            : dpdtSimpleMsg(`Error- Search for Data product failed, status ${status} (${getHttpErrorMessage(status)}), url: ${url}`);
        dispatchUpdateDataProducts(dpId,msg);
        return;
    }
    dispatchUpdateDataProducts(dpId,displayTypeParams);
    if (displayTypeParams.gridNotSupported && layout===GRID) {
        dispatchChangeViewerLayout(imageViewer.viewerId, SINGLE, undefined, tbl_id);
    }
    if (displayTypeParams.displayType===DPtypes.PROMISE && displayTypeParams.promise) {
        void handleProductResultPromise(displayTypeParams.promise,dpId, tbl_id, isPromiseAborted, imageViewer, layout);
    }
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

