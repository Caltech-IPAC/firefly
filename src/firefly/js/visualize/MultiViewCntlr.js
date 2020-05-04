/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {without,union,difference, get, has} from 'lodash';
import {race,call} from 'redux-saga/effects';
import {take} from 'redux-saga/effects';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {flux} from '../Firefly.js';
import {clone} from '../util/WebUtil.js';
import ImagePlotCntlr, {dispatchRecenter,
                        visRoot, ExpandType, WcsMatchType} from './ImagePlotCntlr.js';
import {primePlot, getPlotViewById} from './PlotViewUtil.js';
import Enum from 'enum';
import {REINIT_APP} from '../core/AppDataCntlr.js';

export const META_VIEWER_ID = 'triViewImageMetaData';
export const IMAGE_MULTI_VIEW_KEY= 'imageMultiView';
export const IMAGE_MULTI_VIEW_PREFIX= 'MultiViewCntlr';

export const ADD_VIEWER= `${IMAGE_MULTI_VIEW_PREFIX}.AddViewer`;
export const REMOVE_VIEWER= `${IMAGE_MULTI_VIEW_PREFIX}.RemoveViewer`;
export const VIEWER_MOUNTED= `${IMAGE_MULTI_VIEW_PREFIX}.viewMounted`;
export const VIEWER_UNMOUNTED= `${IMAGE_MULTI_VIEW_PREFIX}.viewUnmounted`;
export const ADD_VIEWER_ITEMS= `${IMAGE_MULTI_VIEW_PREFIX}.addViewerItems`;
export const REMOVE_VIEWER_ITEMS= `${IMAGE_MULTI_VIEW_PREFIX}.removeViewerItems`;
export const REPLACE_VIEWER_ITEMS= `${IMAGE_MULTI_VIEW_PREFIX}.replaceViewerItems`;
export const CHANGE_VIEWER_LAYOUT= `${IMAGE_MULTI_VIEW_PREFIX}.changeViewerLayout`;
export const UPDATE_VIEWER_CUSTOM_DATA= `${IMAGE_MULTI_VIEW_PREFIX}.updateViewerCustomData`;
export const ADD_TO_AUTO_RECEIVER = `${IMAGE_MULTI_VIEW_PREFIX}.addToAutoReceiver`;




function reducers() {
    return {
        [IMAGE_MULTI_VIEW_KEY]: reducer,
    };
}

function actionCreators() {
    return {
        [CHANGE_VIEWER_LAYOUT]: changeViewerLayoutActionCreator,
    };
}



export function getMultiViewRoot() { 
    return flux.getState()[IMAGE_MULTI_VIEW_KEY];
}

export default {
    reducers, actionCreators,
    ADD_VIEWER, REMOVE_VIEWER,
    ADD_VIEWER_ITEMS, REMOVE_VIEWER_ITEMS, REPLACE_VIEWER_ITEMS,
    VIEWER_MOUNTED, VIEWER_UNMOUNTED, UPDATE_VIEWER_CUSTOM_DATA,
    CHANGE_VIEWER_LAYOUT, reducer
};



export const SINGLE='single';
export const GRID='grid';
export const IMAGE='image';
export const PLOT2D='plot2d';
export const WRAPPER='wrapper';
export const DEFAULT_FITS_VIEWER_ID= 'DEFAULT_FITS_VIEWER_ID';
export const DEFAULT_PLOT2D_VIEWER_ID= 'DEFAULT_PLOT2D_VIEWER_ID';
export const EXPANDED_MODE_RESERVED= 'EXPANDED_MODE_RESERVED';

export const GRID_RELATED='gridRelated';
export const GRID_FULL='gridFull';

export const NewPlotMode = new Enum(['create_replace', 'replace_only', 'none']);

function initState() {

    /**
     *
     * @typedef {Object} Viewer
     * @prop {string} viewerId:EXPANDED_MODE_RESERVED,
     * @prop {string[]} itemIdAry
     * @prop {string} layout must be 'single' or 'grid'
     * @prop {boolean} canReceiveNewPlots - NewPlotMode.create_replace.key,
     * @prop {boolean} reservedContainer
     * @prop {string} containerType - one of 'image', 'plot2d', 'wrapper'
     * @prop {boolean} mounted - if the react component using the store is mounted
     * @prop {Object|String} layoutDetail - may be any object, string, etc- Hint for the UI, can be any string but with 2 reserved  GRID_RELATED, GRID_FULL
     * @prop {boolean} internallyManaged - this viewer is managed by other viewers
     * @prop {object} customData: {}
     *
     * @global
     * @public
     */
    /**
     * @typedef {Viewer[]} MultiViewerRoot
     * @global
     * @public
     */
    return [
        {
            viewerId:EXPANDED_MODE_RESERVED,
            itemIdAry:[],
            viewType:SINGLE,
            layout: GRID,
            canReceiveNewPlots: NewPlotMode.create_replace.key,
            reservedContainer:true,
            mounted: false,
            containerType : IMAGE,
            layoutDetail : 'none',
            customData: {},
            renderTreeId: undefined
        },
        {
            viewerId:DEFAULT_FITS_VIEWER_ID,
            itemIdAry:[],
            viewType:GRID,
            layout: GRID,
            canReceiveNewPlots: NewPlotMode.create_replace.key,
            reservedContainer:false,
            mounted: false,
            containerType : IMAGE,
            layoutDetail : 'none',
            customData: {},
            renderTreeId: undefined,
            lastActiveItemId: ''
        },
        {
            viewerId:DEFAULT_PLOT2D_VIEWER_ID,
            itemIdAry:[],
            viewType:GRID,
            layout: GRID,
            canReceiveNewPlots: NewPlotMode.create_replace.key,
            reservedContainer:false,
            mounted: false,
            containerType : PLOT2D,
            layoutDetail : 'none',
            customData: {},
            renderTreeId: undefined,
            lastActiveItemId: ''
        },
        {
            viewerId:'some id',
            itemIdAry:[],
            viewType:SINGLE,
            layout: SINGLE,
            canReceiveNewPlots: NewPlotMode.none.key,
            mounted: false,
            containerType : WRAPPER,
            layoutDetail : 'none',
            customData: {},
            renderTreeId: undefined,
            lastActiveItemId: ''
        }
    ];
}




//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 *
 * @param {string} viewerId
 * @param {string} canReceiveNewPlots   a string representation of one of NewPlotMode.
 * @param {string} containerType a string with container type, IMAGE and PLOT2D are predefined
 * @param {boolean} mounted
 * @param {string} [renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 * @param {string} [layout] - layout type - SINGLE or GRID, defaults to GRID
 * @param {boolean} [reservedContainer]
 * @param {boolean} [internallyManaged]
 */
export function dispatchAddViewer(viewerId, canReceiveNewPlots, containerType, mounted=false, renderTreeId,
                                  layout=GRID, reservedContainer=false, internallyManaged=false) {
    flux.process({
        type: ADD_VIEWER,
        payload: {viewerId, canReceiveNewPlots, containerType, mounted,
            renderTreeId, lastActiveItemId:'', layout, reservedContainer, internallyManaged}
    });
}

/**
 *
 * @param {string} viewerId
 */
export function dispatchRemoveViewer(viewerId) {
    flux.process({type: REMOVE_VIEWER , payload: {viewerId} });
}

/**
 *
 * @param {string} viewerId
 * @param {string[]} itemIdAry  array of itemIds
 * @param {string} containerType a string with container type, IMAGE and PLOT2D are predefined
 * @param {string} [renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 *
 */
export function dispatchAddViewerItems(viewerId, itemIdAry, containerType, renderTreeId) {
    flux.process({type: ADD_VIEWER_ITEMS , payload: {viewerId, itemIdAry, containerType, renderTreeId} });
}


/**
 * 
 * @param {string} viewerId
 * @param {string} layout single or grid
 * @param {string} layoutDetail more detail about the type of layout, hint to UI, typically detail is with GRID
 */
export function dispatchChangeViewerLayout(viewerId, layout, layoutDetail=undefined) {
    flux.process({type: CHANGE_VIEWER_LAYOUT , payload: {viewerId, layout, layoutDetail} });
}


/**
 *
 * @param {string} viewerId
 * @param {string[]} itemIdAry array of string of itemId
 */
export function dispatchRemoveViewerItems(viewerId, itemIdAry) {
    flux.process({type: REMOVE_VIEWER_ITEMS , payload: {viewerId, itemIdAry} });
}

/**
 *
 * @param {string} viewerId
 * @param {string[]} itemIdAry array of string of itemId
 * @param {string} containerType a string with container type, IMAGE and PLOT2D are predefined
 */
export function dispatchReplaceViewerItems(viewerId, itemIdAry, containerType) {
    flux.process({type: REPLACE_VIEWER_ITEMS , payload: {viewerId, itemIdAry, containerType} });
}

/**
 *
 * @param {string} viewerId
 */
export function dispatchViewerMounted(viewerId) {
    flux.process({type: VIEWER_MOUNTED , payload: {viewerId} });
}

/**
 *
 * @param {string} viewerId
 */
export function dispatchViewerUnmounted(viewerId) {
    flux.process({type: VIEWER_UNMOUNTED , payload: {viewerId} });
}

/**
 *
 * @param {string} viewerId
 * @param {Object} customData
 */
export function dispatchUpdateCustom(viewerId, customData) {
    flux.process({type: UPDATE_VIEWER_CUSTOM_DATA , payload: {viewerId,customData} });
}


//======================================== ActionCreators =============================
//======================================== ActionCreators =============================

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));


export function* watchForResizing(options) {
    let remainingIdAry= options.plotIdAry.slice(0);
    let waitingForMore= true;

    if (!visRoot().wcsMatchType) return;

    while (waitingForMore) {
        const raceWinner = yield race({
            action: take([ImagePlotCntlr.UPDATE_VIEW_SIZE]),
            timer: call(delay, 1000)
        });
        const {action}= raceWinner;
        if (action && action.payload.plotId) {
            remainingIdAry= remainingIdAry.filter( (id) => id!==action.payload.plotId);
            waitingForMore= remainingIdAry.length>0;
        }
        else {
            waitingForMore= false;
            console.log('watchForResizing: hit timeout');
        }
    }

    const vr= visRoot();
    const pv= getPlotViewById(vr, vr.mpwWcsPrimId);
    if (pv && primePlot(vr, pv)) {
        if (vr.wcsMatchType) {
            const centerOnImage= (
                vr.wcsMatchType===WcsMatchType.Standard  ||
                vr.wcsMatchType===WcsMatchType.Pixel ||
                vr.wcsMatchType===WcsMatchType.PixelCenter);
            setTimeout(() => dispatchRecenter({ plotId: vr.activePlotId, centerOnImage}) , 100); }
    }
}



/**
 * @param {Action} rawAction
 * @returns {Function}
 */
function changeViewerLayoutActionCreator(rawAction) {
    return (dispatcher, getState) => {

        dispatcher(rawAction);
        const {viewerId}= rawAction.payload;
        const viewer= getViewer(getState()[IMAGE_MULTI_VIEW_KEY], viewerId);
        if (get(viewer, 'containerType')===IMAGE) {
            dispatchAddSaga(watchForResizing, {
                    plotIdAry:viewer.layout===GRID ? viewer.itemIdAry: [viewer.lastActiveItemId]});
        }
    };
}

//======================================== Utilities =============================
//======================================== Utilities =============================
//======================================== Utilities =============================

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {boolean}
 */
export function hasViewerId(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return false;
    return multiViewRoot.find((entry) => entry.viewerId === viewerId);
}

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {string} will be 'single' or 'grid'
 */
export function getLayoutType(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return GRID;
    const v= multiViewRoot.find((entry) => entry.viewerId === viewerId);
    return v ? v.layout : GRID;
}

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @return {string[]} an array of item ids
 */
export function getExpandedViewerItemIds(multiViewRoot) {
    return getViewerItemIds(multiViewRoot,EXPANDED_MODE_RESERVED);
}

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {string[]} an array of item ids
 */
export function getViewerItemIds(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return [];
    var viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
    return (viewerObj) ? viewerObj.itemIdAry : [];
}

/**
 * get the viewer for an id
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {Viewer}
 */
export function getViewer(multiViewRoot,viewerId) {
    if (!multiViewRoot || !viewerId) return null;
    return multiViewRoot.find( (entry) => entry.viewerId===viewerId);
}

/**
 *
 * @param {MultiViewRoot} multiViewRoot
 * @param {string} itemId
 * @param {string} containerType
 * @return {Viewer}
 */
export function findViewerWithItemId(multiViewRoot, itemId, containerType) {
    if (!multiViewRoot) return null;
    const v= multiViewRoot.find((entry) =>
               entry.viewerId!==EXPANDED_MODE_RESERVED &&
               entry.itemIdAry.includes(itemId) &&
               entry.containerType===containerType);
    return v ? v.viewerId : null;
}

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} containerType
 * @param {string} [renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 * @return {Viewer}
 */
export function getAViewFromMultiView(multiViewRoot, containerType, renderTreeId= undefined) {
    const viewer= multiViewRoot.find((entry) => (!entry.viewerId.includes('RESERVED')&&
                                            !entry.customData.independentLayout &&
                                            entry.containerType===containerType &&
                                            (get(entry, 'canReceiveNewPlots') === NewPlotMode.create_replace.key)));
    if (viewer.reservedContainer && renderTreeId) {
        const newId= `${viewer.viewerId}_${renderTreeId}`;
        const modViewer= getViewer(multiViewRoot, newId);
        if (modViewer) return modViewer;
        dispatchAddViewer(newId, NewPlotMode.create_replace.key,
                      containerType,false,renderTreeId);

        return getViewer(getMultiViewRoot(), newId);
    }
    else {
        return viewer;
    }
}

/**
 *
 * @param {MultiViewRoot} multiViewRoot
 * @param {VisRoot} visRoot
 * @param {string} plotId
 * @return {boolean}
 */
export function isImageViewerSingleLayout(multiViewRoot, visRoot, plotId) {
    if (visRoot.expandedMode!==ExpandType.COLLAPSE) {
        return visRoot.expandedMode!==ExpandType.GRID;
    }
    else {
        const viewerId= findViewerWithItemId(multiViewRoot, plotId, IMAGE);
        const viewer = viewerId ? getViewer(multiViewRoot, viewerId) : null;

        return viewer ? viewer.layout===SINGLE : true;
    }
}


//======================================== Action Creator =============================
//======================================== Action Creator =============================
//======================================== Action Creator =============================

// eslint-disable-next-line
function xxPLACEHOLDERxxxxxActionCreator(rawAction) {  // remember to export
    return (dispatcher) => { // eslint-disable-line
    };
}

//=============================================
//=============================================
//=============================================


function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;

    var retState= state;
    const {payload}= action;

    switch (action.type) {
        case ADD_VIEWER:
            retState= addViewer(state,payload);
            break;
        case REMOVE_VIEWER:
            retState= removeViewer(state,action);
            break;
        case ADD_VIEWER_ITEMS:
            retState= addItems(state,payload.viewerId,payload.itemIdAry, payload.containerType, payload.renderTreeId);
            break;
        case ADD_TO_AUTO_RECEIVER:
            retState= addToAutoReceiver(state,action);
            break;
        case REMOVE_VIEWER_ITEMS:
            retState= removeItems(state,action);
            break;
        case REPLACE_VIEWER_ITEMS:
            retState= replaceImages(state,payload.viewerId,payload.itemIdAry, payload.containerType);
            break;
        case CHANGE_VIEWER_LAYOUT:
            retState= changeLayout(state,action);
            break;
        case VIEWER_MOUNTED:
            retState= changeMount(state,payload.viewerId,true);
            break;
        case VIEWER_UNMOUNTED:
            retState= changeMount(state,payload.viewerId,false);
            break;
        case UPDATE_VIEWER_CUSTOM_DATA:
            retState= updateCustomData(state,action);
            break;
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
            retState= deleteSingleItem(state,payload.plotId, IMAGE);
            break;

        case ImagePlotCntlr.PLOT_HIPS:
        case ImagePlotCntlr.PLOT_IMAGE_START:
            const {viewerId, plotId, renderTreeId} = payload;
            if (imageViewerCanAdd(state,viewerId, plotId)) {
            //if (payload.viewerId && payload.plotId) {
                state= addItems(state,payload.viewerId,[payload.plotId], IMAGE, renderTreeId);
                retState= addItems(state,EXPANDED_MODE_RESERVED,[payload.plotId],IMAGE);
            }
            break;
        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
        case ImagePlotCntlr.PLOT_IMAGE:
            retState = changeActiveItem(state, payload, IMAGE);
            break;
        case REINIT_APP:
            retState= initState();
            break;
        default:
            break;

    }
    return retState;
}


function imageViewerCanAdd(state, viewerId, plotId) {
    if (!viewerId || !plotId) return false;
    if (!hasViewerId(state,viewerId)) return true;

    return !state.find( (viewer) => { // look for the plotId in all the normal image viewers
        if (viewer.containerType!==IMAGE || viewer.viewerId===EXPANDED_MODE_RESERVED) return false;
        return getViewerItemIds(state,viewer.viewerId).includes(plotId);
    });

}

function addViewer(state,payload) {

    const {viewerId,containerType, layout=GRID,canReceiveNewPlots=NewPlotMode.replace_only.key,
             mounted=false, renderTreeId, reservedContainer, internallyManaged}= payload;
    var   {lastActiveItemId} = payload;
    var entryInState = hasViewerId(state,viewerId);

    if (entryInState) {
        entryInState = Object.assign(entryInState, {canReceiveNewPlots, mounted, containerType});

        if (has(entryInState, 'lastActiveItemId')) {
            lastActiveItemId = entryInState.lastActiveItemId ? entryInState.lastActiveItemId : get(entryInState, ['itemIdAry', '0'], '');
            entryInState = Object.assign(entryInState, {lastActiveItemId});
        }
        return [...state];
    } else {
        // set default layout for the viewer with viewerId, META_VIEWER_ID, is full-grid type
        const layoutDetail = viewerId === META_VIEWER_ID ? GRID_FULL : undefined;
        const entry = {viewerId, containerType, canReceiveNewPlots, layout, mounted, itemIdAry: [], customData: {},
                       lastActiveItemId, layoutDetail, renderTreeId, reservedContainer, internallyManaged};
        return [...state, entry];
    }
}


function removeViewer(state,action) {
    const {viewerId}= action.payload;
    return state.filter( (v) => v.viewId!==viewerId);
}


/**
 *
 * @param {MultiViewerRoot} state
 * @param {string} viewerId
 * @param {string[]} itemIdAry
 * @param {string} containerType
 * @param {string} [renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab, only used here
 *                                 if the viewerId does not exist and it needs to make one.
 * @return {MultiViewerRoot}
 */
function addItems(state,viewerId,itemIdAry, containerType, renderTreeId) {

    if (renderTreeId) {
        itemIdAry.forEach( (id) => {
            const v= findViewerWithItemId(state, id, containerType);
            if (v && v.renderTreeId!==renderTreeId) state= deleteSingleItem(state,id,containerType);
        });
    }
    let viewer= state.find( (entry) => entry.viewerId===viewerId);

    if (!viewer) {
        state= addViewer(state,{viewerId,containerType, renderTreeId});
        viewer= state.find( (entry) => entry.viewerId===viewerId);
    }

    itemIdAry= union(viewer.itemIdAry,itemIdAry);
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {itemIdAry}) : entry);
}

/**
 *
 * @param {MultiViewerRoot} state
 * @param {string} viewerId
 * @param {string[]} itemIdAry
 * @param {string} containerType
 * @return {MultiViewerRoot}
 */
function replaceImages(state,viewerId,itemIdAry,containerType) {
    let viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) {
        state= addViewer(state,{viewerId,containerType});
    }
    const updateViewer = (entry) => {
        if (has(entry, 'lastActiveItemId')) {
            return {itemIdAry, lastActiveItemId: get(itemIdAry, '0', '')};
        } else {
            return {itemIdAry};
        }
    };

    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, updateViewer(entry)) : entry);
}


function addToAutoReceiver(state,action) {
    const {imageAry}= action.payload;
    return state.map( (entry) => 
              entry.canReceiveNewPlots === NewPlotMode.create_replace.key ? clone(entry, {itemIdAry: union(entry.itemIdAry,imageAry)}) : entry);
}


function removeItems(state,action) {
    var {viewerId,itemIdAry}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;

    var rmIdAry = itemIdAry.slice();
    itemIdAry= difference(viewer.itemIdAry,itemIdAry);

    var updateViewer = (entry) => {

        if (has(entry, 'lastActiveItemId')&&rmIdAry.includes(entry.lastActiveItemId)) {
            return {itemIdAry, lastActiveItemId: get(itemIdAry, '0', '')};
        } else {
            return {itemIdAry};
        }
    };

    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, updateViewer(entry)) : entry);
}

/**
 * Delete an item with only knowing the itemId and containerType but not the viewerId
 * @param {MultiViewRoot} state
 * @param {string} itemId
 * @param {string} containerType
 * @return {MultiViewRoot}
 */
function deleteSingleItem(state,itemId, containerType) {
    return state.map( (viewer) => {
        if (viewer.containerType!==containerType || !viewer.itemIdAry.includes( itemId)) return viewer;
        const v = clone(viewer, {itemIdAry: without(viewer.itemIdAry, itemId)});

        if (has(v, 'lastActiveItemId') && (v.lastActiveItemId === itemId)) {
            return clone(v, {lastActiveItemId:  get(v, 'itemIdAry.0', '')});
        } else {
            return v;
        }
    });
}



function changeLayout(state,action) {
    const {viewerId,layout,layoutDetail}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;
    if (viewer.layout===layout && viewer.layoutDetail===layoutDetail) return state;
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {layout,layoutDetail}) : entry);
}

function changeMount(state,viewerId,mounted) {
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;
    if (viewer.mounted===mounted) return state;
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {mounted}) : entry);
}


function updateCustomData(state,action) {
    const {viewerId,customData}= action.payload;
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {customData}) : entry);
}

function changeActiveItem(state, payload, containerType) {
    var {plotId, viewerId} = payload;

    return state.map((viewer) => {
        var isView = false;

        if (!has(viewer, 'lastActiveItemId')) return viewer;

        if (viewerId) {         // plot image action case
            if ((viewerId === viewer.viewerId) && viewer.itemIdAry.includes(plotId)) {
                isView = true;
            }
        } else {               // change active plot action case
            if ((viewer.containerType === containerType) && (viewer.itemIdAry.includes(plotId))) {
              isView = true;
            }
        }

        if (isView) {
            return clone(viewer, {lastActiveItemId: plotId});
        } else {
            return viewer;
        }
    });
}
