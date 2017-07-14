/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';

import {get, isEqual, isEmpty, filter, pick, uniqBy} from 'lodash';
import Enum from 'enum';
import {flux} from '../Firefly.js';
import {clone} from '../util/WebUtil.js';
import {smartMerge} from '../tables/TableUtil.js';
import {getDropDownNames} from '../ui/Menu.jsx';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {TBL_RESULTS_ADDED, TABLE_REMOVE} from '../tables/TablesCntlr.js';
import {CHART_ADD, CHART_REMOVE} from '../charts/ChartsCntlr.js';
import {REPLACE_VIEWER_ITEMS} from '../visualize/MultiViewCntlr.js';

export const LAYOUT_PATH = 'layout';

// this enum is flaggable, therefore you can use any combination of the 3, i.e. 'tables | images'.
export const LO_VIEW = new Enum(['none', 'tables', 'images', 'xyPlots', 'tableImageMeta', 'coverageImage'], { ignoreCase: true });
export const LO_MODE = new Enum(['expanded', 'standard']);
export const SPECIAL_VIEWER = new Enum(['tableImageMeta', 'coverageImage'], { ignoreCase: true });

/*---------------------------- Actions ----------------------------*/

export const SET_LAYOUT         = `${LAYOUT_PATH}.setLayout`;
export const SET_LAYOUT_MODE    = `${LAYOUT_PATH}.setLayoutMode`;
export const SHOW_DROPDOWN      = `${LAYOUT_PATH}.showDropDown`;
export const ADD_CELL           = `${LAYOUT_PATH}.addCell`;
export const REMOVE_CELL        = `${LAYOUT_PATH}.removeCell`;
export const ENABLE_SPECIAL_VIEWER= `${LAYOUT_PATH}.enableSpecialViewer`;

/*---------------------------- Reducers ----------------------------*/

export function reducer(state={}, action={}) {
    const {mode, view} = action.payload || {};

    switch (action.type) {
        case SET_LAYOUT :
            const dropDown = get(action, 'payload.dropDown');
            if( dropDown ) {
                action.payload.dropDown.view = getSelView(state, dropDown);
            }
            return smartMerge(state, action.payload);

        case SET_LAYOUT_MODE :
            return smartMerge(state, {mode: {[mode]: view}});

        case SHOW_DROPDOWN :
            const {visible = !state.disableDefaultDropDown} = action.payload;
            return smartMerge(state, {dropDown: {visible, view: getSelView(state, action.payload)}});
        case ADD_CELL :
            return addCell(state, action.payload);
        case REMOVE_CELL :
            return removeCell(state, action.payload);
        case ENABLE_SPECIAL_VIEWER :
            return enableSpecialViewer(state,action.payload);
        default:
            return state;
    }

}

/*---------------------------- Reducer helpers -----------------------------*/

function enableSpecialViewer(state,payload) {
    const {viewerType, cellId}= payload;
    const vType= SPECIAL_VIEWER.get(viewerType);
    if (!vType || !cellId) return state;
    const newVal= state[vType.key] ? [...state[vType.key], cellId] : [cellId];
    return Object.assign({}, state, {[vType.key]: newVal});
}

function addCell(state,payload) {
    const {row=0, col=0, width=1, height=1, cellId}= payload;
    const type= LO_VIEW.get(payload.type);
    if (!type || !cellId) return state; // row, col, type, cellId must be defined

    let {gridView=[]}=  state;

    const c= gridView.find((entry) => entry.cellId===cellId);
    const newEntry= { cellId, row, col, width, height, type };
    if (isEqual(c,newEntry)) return state; // no changes

                     // either update or add the new cell
    gridView= c ? gridView.map( (entry) => entry.cellId===cellId ? newEntry : entry) :
                  [...gridView, newEntry];


    const dim= getGridDim(gridView);
    const cols= state.gridColumns || 1;


    return clone(state, {gridView, gridColumns: cols>dim.cols ? cols : dim.cols});
}

function removeCell(state,payload) {
    const {cellId}= payload;
    const {gridView}=  state;
    if (isEmpty(gridView) || !cellId) return state;
    return clone(state, {gridView: gridView.filter( (g) => g.cellId!==cellId)});
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
 * set the layout info of the application.  data will be merged.
 * @param layoutInfo data to be updated
 */
export function dispatchUpdateLayoutInfo(layoutInfo) {
    flux.process({type: SET_LAYOUT, payload: {...layoutInfo}});
}

/**
 * show the drop down container
 * @param view name of the component to display in the drop-down container
 */
export function dispatchShowDropDown({view}) {
    flux.process({type: SHOW_DROPDOWN, payload: {visible: true, view}});
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
export function dispatchAddCell({row,col,width,height,type, cellId, dispatcher= flux.process}) {
    dispatcher({type: ADD_CELL,   payload: {row,col,width,height,type, cellId}});
}

/**
 * remove a cell definition from the LayoutInfo
 *
 * @param {Object} p
 * @param {string} p.cellId
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 */
export function dispatchRemoveCell({cellId, dispatcher= flux.process}) {
    dispatcher({type: REMOVE_CELL,   payload: {cellId}});
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
    return get(flux.getState(), 'layout.dropDown', {visible: false});
}

export function getGridCell(cellId) {
    const {gridView=[]}= getLayoutRoot();
    return gridView.find( (entry) => entry.cellId===cellId);
}

export function getLayoutRoot() {
    return get(flux.getState(), [LAYOUT_PATH]);
}

/**
 * @returns {LayoutInfo} returns the layout information of the application
 */
export function getLayouInfo() {
    const layout = get(flux.getState(), 'layout', {});
    const hasImages = get(flux.getState(), 'allPlots.plotViewAry.length') > 0;
    const hasTables = !isEmpty(get(flux.getState(), 'table_space.results.main.tables', {}));
    const hasXyPlots = !isEmpty(get(flux.getState(), 'charts.data', {}));
    return clone(layout, {hasImages, hasTables, hasXyPlots});
}

function getSelView(state, dropDown) {
    var {visible=!state.disableDefaultDropDown, view} = dropDown || {};
    if (visible && !view) {
        return get(state, 'layout.dropDown.view') || getDropDownNames()[0];
    }
    return view;
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
    const count = filter(pick(layoutInfo, ['showTables', 'showXyPlots', 'showImages'])).length;
    switch (action.type) {
        case CHART_ADD:
        case TBL_RESULTS_ADDED:
        case REPLACE_VIEWER_ITEMS :
        case ImagePlotCntlr.PLOT_IMAGE :
        case ImagePlotCntlr.PLOT_IMAGE_START :
            return smartMerge(layoutInfo, {dropDown: {visible: false}});
            break;

        case SHOW_DROPDOWN:
        case TABLE_REMOVE:
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
            if (!get(layoutInfo, 'dropDown.visible', false)) {
                if (count===0) {
                    return smartMerge(layoutInfo, {dropDown: {visible: true}});
                }
            }
            break;
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
            TABLE_REMOVE, TBL_RESULTS_ADDED,
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


