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
import MultiViewCntlr, {dispatchReplaceViewerItems, getViewerItemIds,
                        dispatchChangeViewerLayout, dispatchUpdateCustom,
                        getMultiViewRoot, getViewer, GRID, GRID_FULL, SINGLE} from '../MultiViewCntlr.js';
import {makeDataProductsConverter, initImage3ColorDisplayManagement} from '../../metaConvert/DataProductsFactory.js';
import {findGridTableRows} from '../../metaConvert/converterUtils.js';
import {PlotAttribute} from '../WebPlot.js';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {isMetaDataTable} from '../../util/VOAnalyzer.js';
import {zoomPlotPerViewSize, resetImageFullGridActivePlot, changeActivePlotView} from '../../metaConvert/ImageDataProductsUtil.js';
import {removeTablesFromGroup} from '../../tables/TableUtil';
import {IMAGE} from '../MultiViewCntlr';

const MAX_GRID_SIZE= 50;


const DataProductsWatcherDef = {
    id : 'ImageMetaDataWatcher',
    watcher : watchDataProductsTable,
    testTable : (t) => isMetaDataTable(t.tbl_id),
    actions: [TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE,
              TBL_RESULTS_ACTIVE, REINIT_APP,
              MultiViewCntlr.ADD_VIEWER, MultiViewCntlr.VIEWER_MOUNTED,
              MultiViewCntlr.VIEWER_UNMOUNTED,
              MultiViewCntlr.CHANGE_VIEWER_LAYOUT, MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA,
              ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW,
              ImagePlotCntlr.UPDATE_VIEW_SIZE, ImagePlotCntlr.ANY_REPLOT]
};

export function startDataProductsWatcher({imageViewerId= 'ViewImageMetaData', chartViewerId= 'ViewChartMetaData',
                                                            tableGroupViewerId='TableDataProducts',
                                                          dataTypeViewerId= 'DataProductsType', paused=true}) {
    dispatchAddTableTypeWatcherDef( { ...DataProductsWatcherDef, options:{imageViewerId, chartViewerId, tableGroupViewerId, dataTypeViewerId, paused} });
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
    const {options:{imageViewerId='ImageMetaData', chartViewerId, tableGroupViewerId, dataTypeViewerId= 'DataProductsType'}} = params;
    const activateParams= {imageViewerId,converterId:undefined,tableGroupViewerId,chartViewerId};
    let {paused= true} = params;
    const {abortPromise:abortLastPromise} = params;

    if (!action) {
        if (paused) {
            const mvRoot= getMultiViewRoot();
            const imView= getViewer(mvRoot, imageViewerId);
            const dpView= getViewer(mvRoot, dataTypeViewerId);
            paused= !Boolean(imView || dpView);
        }
        if (!paused) updateDataProducts(tbl_id, activateParams, dataTypeViewerId);
        initImage3ColorDisplayManagement(imageViewerId); //todo: 3 color not working
        return {paused};
    }

    const {payload}= action;


    if (payload.viewerId && payload.viewerId!==imageViewerId) return params;



    if (payload.tbl_id) {
        if (payload.tbl_id!==tbl_id) return params;
        if (action.type===TABLE_REMOVE) {
            removeAllProducts(activateParams);
            // todo might need to remove other stuff as well: charts, images, jpeg
            cancelSelf();
            return;
        }
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
            if (!paused) abortPromise= updateDataProducts(tbl_id, activateParams, dataTypeViewerId, abortLastPromise);
            break;

        case MultiViewCntlr.CHANGE_VIEWER_LAYOUT:
        case MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA:
            if (!paused) abortPromise= updateDataProducts(tbl_id, activateParams, dataTypeViewerId, abortLastPromise, true);

            break;


        case MultiViewCntlr.ADD_VIEWER:
            if (payload.mounted) {
                abortPromise= updateDataProducts(tbl_id, activateParams, dataTypeViewerId, abortLastPromise);
                paused= false;
            }
            break;

        case MultiViewCntlr.VIEWER_MOUNTED:
            paused= false;
            abortPromise= updateDataProducts(tbl_id, activateParams, dataTypeViewerId, abortLastPromise);
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

        case ImagePlotCntlr.UPDATE_VIEW_SIZE:
            if (!paused) zoomPlotPerViewSize(payload.plotId);
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
 * @param {string} dataTypeViewerId
 * @param {function} abortLastPromise
 * @param {boolean} layoutChange
 * @return {function} a function to abort any unfinished promises
 */
function updateDataProducts(tbl_id, activateParams, dataTypeViewerId, abortLastPromise=undefined, layoutChange= false) {

    abortLastPromise && abortLastPromise();
    const {imageViewerId}= activateParams;
    let continuePromise= true;
    const abortPromise= () => continuePromise= false;
    let viewer = getViewer(getMultiViewRoot(), imageViewerId);

    if (!viewer) return;

    const table = getTblById(tbl_id);
    // check to see if tableData is available in this range.
    if (!table || !isTblDataAvail(table.highlightedRow, table.highlightedRow + 1, table)) {
        removeAllProducts(activateParams);
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
        const showThreeColor= get(viewer.customData, [converter.converterId,'threeColorVisible'], false);
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

    if (layoutChange && viewer.layout===SINGLE && !isEmpty(viewer.itemIdAry)) {
        if (viewer.itemIdAry[0].includes(GRID_FULL.toLowerCase())) {   // from full grid images
            const activePid = visRoot().activePlotId;

            viewer.itemIdAry.find((id) => {
                const plot = primePlot(visRoot(), id);
                if (plot && id !== activePid &&
                    get(plot.attributes, PlotAttribute.TABLE_ROW, -1) === highlightedRow) {
                    dispatchChangeActivePlotView(id);
                    return true;
                }
                return false;
            });
        }
        return;
    }

    const fullActivateParams= {...activateParams,converterId};
    if (viewer.layout===SINGLE) {
        converter.getSingleDataProduct(table,highlightedRow,fullActivateParams)
            .then( (displayTypeParams) => continuePromise && dispatchUpdateCustom(dataTypeViewerId, displayTypeParams)
        );
    }
    else if (viewer.layout===GRID && viewer.layoutDetail===GRID_FULL) { // keep this image only
        const plotRows= findGridTableRows(table,Math.min(converter.maxPlots,MAX_GRID_SIZE),`${converterId}-gridfull`);
        converter.getGridDataProduct(table,plotRows,fullActivateParams)
            .then( (displayTypeParams) => continuePromise && dispatchUpdateCustom(dataTypeViewerId, displayTypeParams)
        );
    }
    else if (viewer.layout===GRID) {// keep this image only
        converter.getRelatedDataProduct(table,highlightedRow,threeColorOps,viewer.highlightPlotId,fullActivateParams)
            .then( (displayTypeParams) => continuePromise && dispatchUpdateCustom(dataTypeViewerId, displayTypeParams)
        );
    }
    return abortPromise;
}

/**
 *
 * @param {ActivateParams} activateParams
 */
function removeAllProducts(activateParams) {
    const {imageViewerId, tableGroupViewerId}= activateParams;
    if (!imageViewerId) return;
    const inViewerIds= getViewerItemIds(getMultiViewRoot(), imageViewerId);
    removeTablesFromGroup(tableGroupViewerId);
    dispatchReplaceViewerItems(imageViewerId,[],IMAGE);
    inViewerIds.forEach( (plotId) => dispatchDeletePlotView({plotId}));
}

