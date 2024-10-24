/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';

import {get, isEqual, isEmpty, filter, pick, uniqBy, flatten} from 'lodash';
import Enum from 'enum';
import {DATA_PRODUCT_ID_PREFIX, dataProductRoot} from '../metaConvert/DataProductsCntlr.js';
import {getBackgroundInfo} from './background/BackgroundUtil.js';
import {flux} from './ReduxFlux';
import {clone} from '../util/WebUtil.js';
import {
    smartMerge, getActiveTableId, getTblById, findGroupByTblId, getTblIdsByGroup,
} from '../tables/TableUtil.js';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import { TBL_RESULTS_ADDED, TBL_RESULTS_REMOVE, TABLE_REMOVE, TABLE_SPACE_PATH, TBL_RESULTS_ACTIVE, TABLE_LOADED
} from '../tables/TablesCntlr.js';
import {CHART_ADD, CHART_REMOVE, CHART_SPACE_PATH} from '../charts/ChartsCntlr.js';
import {
    DEFAULT_FITS_VIEWER_ID, getMultiViewRoot, getViewer, PINNED_CHART_VIEWER_ID, REPLACE_VIEWER_ITEMS
} from '../visualize/MultiViewCntlr.js';
import {COMMAND, getMenu, REINIT_APP} from './AppDataCntlr.js';
import {getDefaultChartProps} from '../charts/ChartUtil.js';
import {getPlotViewAry, getPlotViewById} from 'firefly/visualize/PlotViewUtil.js';
import {MetaConst} from 'firefly/data/MetaConst';

export const LAYOUT_PATH = 'layout';

// this enum is flaggable, therefore you can use any combination of the 3, i.e. 'tables | images'.
/**
 * @typedef LO_VIEW
 * @type {Enum}
 * @prop none
 * @prop tables
 * @prop images
 * @prop xyPlots
 * @prop tableImageMeta
 * @prop coverageImage
 * @prop {Function} get
 */

/** @type LO_VIEW */
export const LO_VIEW = new Enum(['none', 'tables', 'images', 'xyPlots', 'tableImageMeta', 'coverageImage'], { ignoreCase: true });

/**
 * @typedef LO_MODE
 * @type {Enum}
 * @prop expanded
 * @prop standard
 */
/** @type LO_MODE */
export const LO_MODE = new Enum(['expanded', 'standard']);
export const SPECIAL_VIEWER = new Enum(['tableImageMeta', 'coverageImage'], { ignoreCase: true });

/*---------------------------- Actions ----------------------------*/

export const UPDATE_LAYOUT     = `${LAYOUT_PATH}.updateLayout`;
export const UPDATE_GRID_VIEW  = `${LAYOUT_PATH}.updateGridView`;
export const SET_LAYOUT         = `${LAYOUT_PATH}.setLayout`;
export const SET_LAYOUT_MODE    = `${LAYOUT_PATH}.setLayoutMode`;
export const TRIVIEW_LAYOUT    = `${LAYOUT_PATH}.triviewLayout`;
export const SHOW_DROPDOWN      = `${LAYOUT_PATH}.showDropDown`;
export const ADD_CELL           = `${LAYOUT_PATH}.addCell`;
export const REMOVE_CELL        = `${LAYOUT_PATH}.removeCell`;
export const ENABLE_SPECIAL_VIEWER= `${LAYOUT_PATH}.enableSpecialViewer`;
export const MENU_UPDATE      = `${LAYOUT_PATH}.menuUpdate`;


export const TRIVIEW_ICov_Ch_T= 'TRIVIEW_ICov_Ch_T'; //top left: image/cov, top right: charts, bottom: tables
export const TRIVIEW_I_ChCov_T= 'TRIVIEW_I_ChCov_T';//top left: image, top right: charts/cov, bottom: tables
export const BIVIEW_ICov_Ch= 'BIVIEW_ICov_Ch'; //left: image/cov, right: charts
export const BIVIEW_I_ChCov= 'BIVIEW_I_ChCov'; //left: image, right: charts/cov
export const BIVIEW_T_IChCov= 'BIVIEW_T_IChCov'; //left: tables, right: image/charts/cov
export const BIVIEW_IChCov_T= 'BIVIEW_IChCov_T'; //left: image/charts/cov, right: tables




/*---------------------------- Reducers ----------------------------*/

export function reducer(state={}, action={}) {
    const {mode, view} = action.payload || {};

    switch (action.type) {
        case UPDATE_LAYOUT :
        case SET_LAYOUT :
            const dropDown = get(action, 'payload.dropDown');
            if( dropDown ) {
                action.payload.dropDown.view = getSelView(state, dropDown);
            }
            return action.type === SET_LAYOUT ? action.payload : smartMerge(state, action.payload);

        case UPDATE_GRID_VIEW :
            return updateGridView(state, action.payload);
        case SET_LAYOUT_MODE :
            return smartMerge(state, {mode: {[mode]: view}});
        case TRIVIEW_LAYOUT :
            return updateTriviewLayout(state, action);
        case SHOW_DROPDOWN :
            const {visible = !state.disableDefaultDropDown, menuItem, initArgs={}} = action.payload;
            const newState= {...state};
            if (newState?.dropDown?.initArgs) newState.dropDown.initArgs= undefined;
            return smartMerge(state, {dropDown: {visible, view: getSelView(state, action.payload), menuItem, initArgs}});
        case ADD_CELL :
            return addCell(state, action.payload);
        case REMOVE_CELL :
            return removeCell(state, action.payload);
        case ENABLE_SPECIAL_VIEWER :
            return enableSpecialViewer(state,action.payload);
        case REINIT_APP:
            return {};
        default:
            return state;
    }

}

/*---------------------------- Reducer helpers -----------------------------*/


function updateTriviewLayout(state,action) {
    const {triviewLayout=TRIVIEW_ICov_Ch_T}= action.payload;
    const triViewKey= 'images | tables | xyplots';
    const imgXyKey= 'images | xyplots';
    const tblXyKey= 'tables | xyplots';
    const xYTblKey= 'xyplots | tables';
    const LEFT= 'LEFT';
    const RIGHT= 'RIGHT';

    const newObj={};

    switch (triviewLayout) {
        case TRIVIEW_ICov_Ch_T:
            newObj.mode= {[LO_MODE.standard]: triViewKey};
            newObj.coverageSide= LEFT;
            break;
        case TRIVIEW_I_ChCov_T:
            newObj.mode= {[LO_MODE.standard]: triViewKey};
            newObj.coverageSide= RIGHT;
            break;
        case BIVIEW_ICov_Ch:
            newObj.mode= {[LO_MODE.standard]: imgXyKey};
            newObj.coverageSide= LEFT;
            break;
        case BIVIEW_I_ChCov:
            newObj.mode= {[LO_MODE.standard]: imgXyKey};
            newObj.coverageSide= RIGHT;
            break;
        case BIVIEW_T_IChCov:
            newObj.mode= {[LO_MODE.standard]: tblXyKey};
            newObj.coverageSide= RIGHT;
            break;
        case BIVIEW_IChCov_T:
            newObj.mode= {[LO_MODE.standard]: xYTblKey};
            newObj.coverageSide= RIGHT;
            break;
    }
    return smartMerge(state, newObj);
}

function enableSpecialViewer(state,payload) {
    const {viewerType, cellId}= payload;
    const vType= SPECIAL_VIEWER.get(viewerType);
    if (!vType || !cellId) return state;
    const newVal= state[vType.key] ? [...state[vType.key], cellId] : [cellId];
    return Object.assign({}, state, {[vType.key]: newVal});
}

function addCell(state,payload) {
    const {row=0, col=0, width=1, height=1, cellId, renderTreeId='DEFAULT' }= payload;
    const type= LO_VIEW.get(payload.type);
    if (!type || !cellId) return state; // row, col, type, cellId must be defined


    let {gridViewsData= {}}= state;

    let {gridView=[], gridColumns:cols= 1}= get(gridViewsData,renderTreeId, {});

    const c= gridView.find((entry) => entry.cellId===cellId);
    const newEntry= { cellId, row, col, width, height, type };
    if (isEqual(c,newEntry)) return state; // no changes

                     // either update or add the new cell
    gridView= c ? gridView.map( (entry) => entry.cellId===cellId ? newEntry : entry) :
                  [...gridView, newEntry];
    const dim= getGridDim(gridView);
    gridViewsData= clone(gridViewsData, {[renderTreeId] : {gridView, gridColumns: cols>dim.cols ? cols : dim.cols}});
    return clone(state, {gridViewsData});
}


function updateGridView(state,payload) {
    const {gridView=[], renderTreeId='DEFAULT' }= payload;
    let {gridColumns=1}= get(state.gridViewsData,renderTreeId, {});
    const gridViewsData= clone(state.gridViewsData, {[renderTreeId]:{gridView, gridColumns}});
    return clone(state, {gridViewsData});
}

function removeCell(state,payload) {
    const {cellId, renderTreeId='DEFAULT'}= payload;
    let {gridViewsData= {}}= state;
    let {gridView=[], gridColumns=1}= get(gridViewsData,renderTreeId, {});
    if (isEmpty(gridView) || !cellId) return state;

    gridView= gridView.filter( (g) => g.cellId!==cellId);
    gridViewsData= clone(gridViewsData, {[renderTreeId]: {gridView, gridColumns}});
    return clone(state, {gridViewsData});
}


/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * set the layout mode of the application.  see LO_MODE and LO_VIEW enums for options.
 * @param mode standard or expanded
 * @param view see LO_VIEW for options.
 */
export function dispatchSetLayoutMode(mode=LO_MODE.standard, view) {
    flux.process({type: SET_LAYOUT_MODE, payload: {mode, view}});
}

/**
 * change triview layout
 * @param {Object} payload
 * @param payload.triviewLayout - one of TRIVIEW_ICov_Ch_T, TRIVIEW_I_ChCov_T, BIVIEW_ICov_Ch, BIVIEW_I_ChCov, BIVIEW_T_IChCov, BIVIEW_IChCov_T,
 */
export function dispatchTriviewLayout({triviewLayout}) {
    flux.process({type: TRIVIEW_LAYOUT, payload: {triviewLayout}});
}

/**
 * update the layout info of the application.  data will be merged.
 * @param layoutInfo data to be updated
 */
export function dispatchUpdateLayoutInfo(layoutInfo) {
    flux.process({type: UPDATE_LAYOUT, payload: {...layoutInfo}});
}


/**
 * update the layout info of the application.  data will be merged.
 * @param {GridViewData} gridView  grid view to update
 * @param {string} renderTreeId
 */
export function dispatchUpdateGridView(gridView, renderTreeId='DEFAULT') {
    flux.process({type: UPDATE_GRID_VIEW, payload: {gridView,renderTreeId}});
}


/**
 * set the layout info of the application.
 * @param layoutInfo data to be updated
 */
export function dispatchSetLayoutInfo(layoutInfo) {
    flux.process({type: SET_LAYOUT, payload: layoutInfo});
}

/**
 * show the drop down container
 * @param {object} p     parameters
 * @param {string} p.view name of the component to display in the drop-down container
 * @param {string} [p.menuItem] the menuItem associated with this view
 * @param {Object} [p.initArgs] - init args to pass to the view
 */
export function dispatchShowDropDown({view, menuItem, initArgs}) {
    flux.process({type: SHOW_DROPDOWN, payload: {visible: true, view, menuItem, initArgs}});
}

/**
 * update menu with the new one
 * @param menu the new menu object
 */
export function dispatchUpdateMenu(menu) {
    flux.process({ type : MENU_UPDATE, payload: {menu} });
}

/**
 * hide the drop down container
 */
export function dispatchHideDropDown() {
    flux.process({type: SHOW_DROPDOWN, payload: {visible: false}});
}

/**
 * Add a cell definition to the LayoutInfo
 *
 * @param {Object} p
 * @param {number} p.row
 * @param {number} p.col
 * @param {number} p.width
 * @param {number} p.height
 * @param {LO_VIEW|string} p.type
 * @param {string} p.cellId
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 */
export function dispatchAddCell({row,col,width,height,type, cellId, renderTreeId='DEFAULT', dispatcher= flux.process}) {
    dispatcher({type: ADD_CELL,   payload: {row,col,width,height,type, cellId, renderTreeId}});
}

/**
 * remove a cell definition from the LayoutInfo
 *
 * @param {Object} p
 * @param {string} p.cellId
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 */
export function dispatchRemoveCell({cellId, renderTreeId='DEFAULT', dispatcher= flux.process}) {
    dispatcher({type: REMOVE_CELL,   payload: {cellId, renderTreeId}});
}

/**
 *
 * @param {Object} p
 * @param p.viewerType
 * @param p.cellId
 * @param p.dispatcher
 */
export function dispatchEnableSpecialViewer({viewerType, cellId, dispatcher= flux.process}) {
    dispatcher({type: ENABLE_SPECIAL_VIEWER,  payload: {viewerType, cellId}});
}

/*------------------------- Util functions -------------------------*/
export function getExpandedMode() {
    return get(flux.getState(), ['layout','mode','expanded']);
}

export function getStandardMode() {
    return get(flux.getState(), ['layout','mode','standard']);
}

export function getDropDownInfo() {
    return get(flux.getState(), 'layout.dropDown', {});
}

export function getGridCell(cellId, renderTreeId='DEFAULT') {
    let {gridViewsData= {}}= getLayoutRoot();
    let {gridView=[], gridColumns}= gridViewsData[renderTreeId];
    return gridView.find( (entry) => entry.cellId===cellId);
}

export function getLayoutRoot() {
    return get(flux.getState(), [LAYOUT_PATH]);
}

/**
 * @returns {LayoutInfo} returns the layout information of the application
 */
export function getLayouInfo() {
    const state= flux.getState() ?? {};
    const layout = state[LAYOUT_PATH] ?? {initLoadCompleted:false};
    const hasImages = getPlotViewAry(visRoot()).some( (pv) => pv.plotViewCtx.useForSearchResults);
    const hasTables = !isEmpty(state[TABLE_SPACE_PATH]?.results?.main?.tables);
    /*
      to make plot area disappear if it's not possible to create a plot use
         hasXyPlots = getChartIdsInGroup(getActiveTableId()).length > 0 ||
                      getChartIdsInGroup('default').length > 0 ||
                      (hasTables && !isEmpty(getDefaultChartProps(getActiveTableId())));
      the drawback is that the layout changes for tables with no numeric data or no data
    */
    // keep plot area in place if any table has a related chart

    const mainChartCnt= Object.values(state[CHART_SPACE_PATH]?.data ?? {})
        ?.filter( (c) => !c.groupId.startsWith(DATA_PRODUCT_ID_PREFIX))?.length ?? 0;
    const hasXyPlots =  mainChartCnt || (hasTables && !isEmpty(getDefaultChartProps(getActiveTableId())));
    const initLoadCompleted= layout.initLoadCompleted||hasImages||hasTables||hasXyPlots;

    // we should not make a new object unless something has changed
    return (hasImages===layout.hasImages && hasTables===layout.hasTables &&
            hasXyPlots===layout.hasXyPlots && layout.initLoadCompleted) ?
        layout : {...layout, hasImages, hasTables, hasXyPlots, initLoadCompleted};
}


/**
 * returns an array of drop down actions from menu items
 * @returns {*}
 */
export function getDropDownNames() {
    const menuItems = getMenu()?.menuItems;
    if (!Array.isArray(menuItems)) return [];
    return menuItems.filter((mi) => mi.type !== COMMAND)
        .map((mi) => mi.action);
}


function getSelView(state, dropDown) {
    var {visible=!state.disableDefaultDropDown, view} = dropDown || {};
    if (visible && !view) {
        return get(state, 'layout.dropDown.view') || getDropDownNames()[0];
    }
    return view;
}

/**
 *
 * @return {{bgTableCnt: number, tableCnt: number, haveResults: boolean, imageCnt: number, pinChartCnt: number)}
 */
export function getResultCounts() {
    const layoutInfo= getLayouInfo();
    const haveResults = filter(pick(layoutInfo, ['showTables', 'showXyPlots', 'showImages'])).length>0 ||
            !isEmpty(layoutInfo.gridViewsData) ;
    const tblIds= getTblIdsByGroup('main') ?? [];
    const tableCnt= tblIds?.length;
    const tableLoadingCnt= tblIds.filter( (id) => getTblById(id)?.isFetching).length;

    const imViewAry= dataProductRoot()
        .map( (entry) => entry.activateParams?.imageViewerId)
        .map( (viewId) => getViewer(getMultiViewRoot(), viewId)?.itemIdAry ?? []);


    const defPvIdAry= getViewer(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID)?.itemIdAry ?? [];
    const pvIdAry= [...defPvIdAry,  ...flatten(imViewAry)];


    const imageCnt= pvIdAry?.length;
    const imageLoadingCnt= pvIdAry.filter( (id) => getPlotViewById(visRoot(),id)?.serverCall==='working').length;
    const pinChartCnt= getViewer(getMultiViewRoot(), PINNED_CHART_VIEWER_ID)?.itemIdAry?.length ?? 0;
    const {jobs={}}= getBackgroundInfo() ?? {};
    const bgTableCnt= Object.values(jobs)
        .filter((job) => job.jobInfo?.monitored && job.jobInfo?.type !== 'PACKAGE')?.length ?? 0;
    return {haveResults,tableCnt,tableLoadingCnt,imageCnt,imageLoadingCnt,pinChartCnt,bgTableCnt};
}

/**
 * This handles the general use case of the drop-down panel.
 * It will collapse the drop-down panel when new tables or images are added.
 * It will expand the drop-down panel when there is no results to be shown.
 * @param {LayoutInfo} layoutInfo
 * @param {Action} action
 * @returns {LayoutInfo}  return new LayoutInfo if layout was affected.  Otherwise, return the given layoutInfo.
 */
export function dropDownHandler(layoutInfo, action) {
    // calculate dropDown when new UI elements are added or removed from results
    switch (action.type) {
        case CHART_ADD:
        case TBL_RESULTS_ADDED:
        case TBL_RESULTS_ACTIVE:
        case TABLE_LOADED:
            const tbl_id= action.type === CHART_ADD ? action.payload.groupId : action.payload.tbl_id;
            if (findGroupByTblId(tbl_id)!=='main' || getTblById(tbl_id)?.request?.META_INFO?.[MetaConst.UPLOAD_TABLE]) {
                return layoutInfo;
            }
            return smartMerge(layoutInfo, {dropDown: {visible: false}});
        case REPLACE_VIEWER_ITEMS :
        case ImagePlotCntlr.PLOT_IMAGE :
            return smartMerge(layoutInfo, {dropDown: {visible: false}});
        case ImagePlotCntlr.PLOT_IMAGE_START :
            const VISUALIZED_TABLE_IDS = action.payload?.attributes?.VISUALIZED_TABLE_IDS;
            if (VISUALIZED_TABLE_IDS?.length) {
                const lastId = VISUALIZED_TABLE_IDS[VISUALIZED_TABLE_IDS.length - 1];
                // Check if the last entry contains 'Upload_Tbl' - and return layoutInfo as is if it does
                const containsUploadTbl = lastId.includes('Upload_Tbl');
                if (containsUploadTbl) return layoutInfo;
            }
            const {useForSearchResults= true, useForCoverage}= action.payload.pvOptions;
            const visible= (!useForCoverage && !useForSearchResults) && layoutInfo.dropDown.visible;
            return smartMerge(layoutInfo, {dropDown: {visible}});
    }
    return layoutInfo;
}



/**
 * This handles the general use case of the drop-down panel.
 * It will collapse the drop-down panel when new tables or images are added.
 * It will expand the drop-down panel when there is no results to be shown.
 */
export function* dropDownManager() {
    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
            REPLACE_VIEWER_ITEMS,
            TABLE_REMOVE, TBL_RESULTS_ADDED, TBL_RESULTS_REMOVE,
            CHART_ADD, CHART_REMOVE,
            SHOW_DROPDOWN, SET_LAYOUT_MODE
        ]);

        /**
         * @type {LayoutInfo}
         * @prop {boolean}  layoutInfo.dropDown.visible  show or hide the drop-down panel
         */
        const layoutInfo = getLayouInfo();
        const newLayoutInfo = dropDownHandler(layoutInfo, action);
        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
    }
}

/**
 * get the Dimensions of the grid
 * @param {Array.<GridViewEntry>} gridView used only with the grid view, undefined for other views
 * @param {number} sizeFactor
 * @return {{rows: number, cols: number}}
 */
export function getGridDim(gridView, sizeFactor=1) {
    const maxRow= gridView.reduce( (largest,c) => largest > c.height+c.row ? largest : c.height+c.row, 0 );
    const maxCol= gridView.reduce( (largest,c) => largest > c.width+c.col ? largest : c.width+c.col, 0 );
    return {rows: maxRow*sizeFactor, cols: maxCol*sizeFactor};
}

export function getNextRow(gridView, col) {
    return gridView.filter( (g) => g.col===col)
        .reduce( (sum,c) => sum+ c.height, 0 );
}

export function getNextColumn(gridView, row) {
    return gridView.filter( (g) => g.row===row)
        .reduce( (sum,c) => sum+ c.width, 0 );
}

export function getNextCell(gridView, w, h) {
    const dim= getGridDim(gridView);
    if (dim.rows===0 && dim.cols===0) return {row:0, col:dim.cols};

    const rows= uniqBy(gridView, 'row').map( (g) => g.row);
    let fitCol;
    for(let i=0; (i<rows.length); i++) {
        fitCol= getColFitIdx(gridView,rows[i], i, dim.cols,w);
        if (fitCol>-1) return {row:rows[i], col:fitCol};
    }

    if (dim.rows===dim.cols) {
        return {row:0, col:dim.cols};
    }
    else {
        return {row:dim.rows, col:0};
    }

}


export function getGridView(layoutInfo, renderTreeId='DEFAULT') {
    let {gridViewsData= {[renderTreeId]:{}}}= layoutInfo;
    return get(gridViewsData, [renderTreeId,'gridView'],[]);
}

export function getGridViewColumns(layoutInfo, renderTreeId='DEFAULT') {
    let {gridViewsData= {[renderTreeId]:{}}}= layoutInfo;
    return get(gridViewsData, [renderTreeId,'gridColumns'], 1);
}


function getColFitIdx(gridView, row, testIdx, gridColumns, testWidth) {
    const rowData= gridView.filter( (g) => g.row===row);
    return rowData.reduce( (fitIdx,g,idx) =>  {
        if (fitIdx>-1) {
            return fitIdx;
        }
        else if (idx+1===rowData.length) {
            if ( gridColumns- (g.col+g.width) >= testWidth) {
               fitIdx= g.col+g.width;
            }
        }
        else {
            if ( rowData[idx+1].col -  (g.col+g.width) >= testWidth) {
                fitIdx= g.col+g.width;
            }
        }
        return fitIdx;
    }   ,-1);


}


