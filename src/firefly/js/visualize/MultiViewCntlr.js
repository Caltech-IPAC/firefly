/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {without,union,difference, get} from 'lodash';
import {flux} from '../Firefly.js';
import {clone} from '../util/WebUtil.js';
import ImagePlotCntlr, {ExpandType} from './ImagePlotCntlr.js';
import Enum from 'enum';


export const IMAGE_MULTI_VIEW_KEY= 'imageMultiView';
export const IMAGE_MULTI_VIEW_PREFIX= 'MultiViewCntlr';

export const ADD_VIEWER= `${IMAGE_MULTI_VIEW_PREFIX}.AddViewer`;
export const REMOVE_VIEWER= `${IMAGE_MULTI_VIEW_PREFIX}.RemoveViewer`;
export const VIEWER_MOUNTED= `${IMAGE_MULTI_VIEW_PREFIX}.viewMounted`;
export const VIEWER_UNMOUNTED= `${IMAGE_MULTI_VIEW_PREFIX}.viewUnmounted`;
export const ADD_IMAGES= `${IMAGE_MULTI_VIEW_PREFIX}.addImages`;
export const REMOVE_IMAGES= `${IMAGE_MULTI_VIEW_PREFIX}.removeImages`;
export const REPLACE_IMAGES= `${IMAGE_MULTI_VIEW_PREFIX}.replaceImages`;
export const CHANGE_LAYOUT= `${IMAGE_MULTI_VIEW_PREFIX}.changeLayout`;
export const UPDATE_CUSTOM_DATA= `${IMAGE_MULTI_VIEW_PREFIX}.updateCustomData`;
export const ADD_TO_AUTO_RECEIVER = `${IMAGE_MULTI_VIEW_PREFIX}.addToAutoReceiver`;


export function getMultiViewRoot() { 
    return flux.getState()[IMAGE_MULTI_VIEW_KEY]; 
}

export default {
    ADD_VIEWER, REMOVE_VIEWER,
    ADD_IMAGES, REMOVE_IMAGES, REPLACE_IMAGES,
    VIEWER_MOUNTED, VIEWER_UNMOUNTED, UPDATE_CUSTOM_DATA,
    CHANGE_LAYOUT, reducer
};

export const SINGLE='single';
export const GRID='grid';
export const DEFAULT_FITS_VIEWER_ID= 'DEFAULT_FITS_VIEWER_ID';
export const EXPANDED_MODE_RESERVED= 'EXPANDED_MODE_RESERVED';
export const ANY_VIEWER_RESERVED= 'ANY_VIEWER_RESERVED';

export const GRID_RELATED='gridRelated';
export const GRID_FULL='gridFull';

export const NewPlotMode = new Enum(['create_replace', 'replace_only', 'none']);

function initState() {

    /**
     *
     * @typedef {Object} Viewer
     * @prop {string} viewerId:EXPANDED_MODE_RESERVED,
     * @prop {string[]} plotIdAry
     * @prop {string} must be 'single' or 'grid'
     * @prop {boolean} canReceiveNewPlots: NewPlotMode.create_replace.key,
     * @prop {boolean} reservedContainer:true,
     * @prop {object} customData: {}
     *
     */
    /**
     * @typedef {Viewer[]} MultiViewerRoot
     */
    return [
        {
            viewerId:EXPANDED_MODE_RESERVED,
            plotIdAry:[], 
            viewType:SINGLE,
            canReceiveNewPlots: NewPlotMode.create_replace.key,
            reservedContainer:true,
            customData: {}
        },
        {
            viewerId:DEFAULT_FITS_VIEWER_ID,
            plotIdAry:[],
            viewType:GRID,
            canReceiveNewPlots: NewPlotMode.create_replace.key,
            reservedContainer:true,
            mounted: false,
            customData: {}
        }
    ];

    /*
       array  { viewerId : string
                canReceiveNewPlots : boolean // can this viewer support adding images.
                viewType : string // 'single', 'grid'
                plotIdAry : array of string plotId
                mounted : boolean, if the react component using the store is mounted
                layout :  string, one of GRID, SINGLE
                layoutDetail :  hint for the UI, can be any string but with 2 reserved  GRID_RELATED, GRID_FULL
     *
     */
}




//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 *
 * @param {string} viewerId
 * @param {boolean} canReceiveNewPlots
 * @param {boolean} mounted
 */
export function dispatchAddViewer(viewerId, canReceiveNewPlots, mounted=false) {
    flux.process({type: ADD_VIEWER , payload: {viewerId, canReceiveNewPlots,mounted} });
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
 * @param {string[]} plotIdAry  array of plotIds
 *
 */
export function dispatchAddImages(viewerId, plotIdAry) {
    flux.process({type: ADD_IMAGES , payload: {viewerId, plotIdAry} });
}

/**
 *
 * @param {[]} imageAry  array of {plotId : string, requestAry : array of WebPlotRequest}
 *
 */
export function dispatchAddToAutoReceiver(imageAry) {
    flux.process({type: ADD_TO_AUTO_RECEIVER , payload: {imageAry} });
}

/**
 * 
 * @param {string} viewerId
 * @param {string} layout single or grid
 * @param {string} layoutDetail more detail about the type of layout, hint to UI
 */
export function dispatchChangeLayout(viewerId, layout, layoutDetail) {
    flux.process({type: CHANGE_LAYOUT , payload: {viewerId, layout, layoutDetail} });
}


/**
 *
 * @param {string} viewerId
 * @param {[]} plotIdAry array of string of plotId
 */
export function dispatchRemoveImages(viewerId, plotIdAry) {
    flux.process({type: REMOVE_IMAGES , payload: {viewerId, plotIdAry} });
}

/**
 *
 * @param {string} viewerId
 * @param {string[]} plotIdAry  array of {plotId : string, requestAry : array of WebPlotRequest}
 */
export function dispatchReplaceImages(viewerId, plotIdAry) {
    flux.process({type: REPLACE_IMAGES , payload: {viewerId, plotIdAry} });
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
    flux.process({type: UPDATE_CUSTOM_DATA , payload: {viewerId,customData} });
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
 * @return {string[]} an array of plot ids
 */
export function getExpandedViewerPlotIds(multiViewRoot) {
    return getViewerPlotIds(multiViewRoot,EXPANDED_MODE_RESERVED);
}

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {string[]} an array of plot ids
 */
export function getViewerPlotIds(multiViewRoot,viewerId) {
    if (!multiViewRoot || !viewerId) return [];
    var viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
    return (viewerObj) ? viewerObj.plotIdAry : [];
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

// export function getViewerPlotRequest(multiViewRoot,viewerId) {
//     if (!multiViewRoot || !viewerId) return null;
//     var viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
//     if (!viewerObj) return null;
//     return viewerObj.plotIdAry;
// }


export function findViewerWithPlotId(multiViewRoot, plotId) {
    if (!multiViewRoot) return null;
    const v= multiViewRoot.find((entry) =>
               entry.viewerId!==EXPANDED_MODE_RESERVED && entry.plotIdAry.includes(plotId));
    return v ? v.viewerId : null;
}

// get an available view from multiple views

/**
 *
 * @param {MultiViewRoot} multiViewRoot
 * @return {Viewer}
 */
export function getAViewFromMultiView(multiViewRoot) {
    return  multiViewRoot.find((entry) => (!entry.viewerId.includes('RESERVED')&&
                                          (get(entry, 'canReceiveNewPlots') === NewPlotMode.create_replace.key)));
}

/**
 *
 * @param {MultiViewRoot} multiViewRoot
 * @param {VisRoot} visRoot
 * @param {string} plotId
 * @return {boolean}
 */
export function isViewerSingleLayout(multiViewRoot, visRoot, plotId) {
    var viewer;
    if (visRoot.expandedMode!==ExpandType.COLLAPSE) {
        return visRoot.expandedMode!==ExpandType.GRID;
    }
    else {
        const viewerId= findViewerWithPlotId(multiViewRoot, plotId);
        return viewer ? getViewer(multiViewRoot,viewerId).viewType===SINGLE : true;
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
        case ADD_IMAGES:
            retState= addImages(state,payload.viewerId,payload.plotIdAry);
            break;
        case ADD_TO_AUTO_RECEIVER:
            retState= addToAutoReceiver(state,action);
            break;
        case REMOVE_IMAGES:
            retState= removeImages(state,action);
            break;
        case REPLACE_IMAGES:
            retState= replaceImages(state,payload.viewerId,payload.plotIdAry);
            break;
        case CHANGE_LAYOUT:
            retState= changeLayout(state,action);
            break;
        case VIEWER_MOUNTED:
            retState= changeMount(state,payload.viewerId,true);
            break;
        case VIEWER_UNMOUNTED:
            retState= changeMount(state,payload.viewerId,false);
            break;
        case UPDATE_CUSTOM_DATA:
            retState= updateCustomData(state,action);
            break;
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
            retState= deletePlotView(state,action);
            break;

        case ImagePlotCntlr.PLOT_IMAGE_START:
            if (payload.viewerId) {
                if (payload.plotId) {
                    state= addImages(state,payload.viewerId,[payload.plotId]);
                    retState= addImages(state,EXPANDED_MODE_RESERVED,[payload.plotId]);
                }
            }
            break;
        default:
            break;

    }
    return retState;
}



function addViewer(state,payload) {

    const {viewerId,layout=GRID,canReceiveNewPlots=NewPlotMode.replace_only.key, mounted=false}= payload;
    var entryInState = hasViewerId(state,viewerId);

    if (entryInState) {
        entryInState = Object.assign(entryInState, {canReceiveNewPlots, mounted});
        return [...state];
    } else {
        const entry = {viewerId, canReceiveNewPlots, layout, mounted, plotIdAry: [], customData: {}};
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
 * @param {string[]} plotIdAry
 * @return {MultiViewerRoot}
 */
function addImages(state,viewerId,plotIdAry) {
    var viewer;
    if (viewerId===ANY_VIEWER_RESERVED) {
        viewer= getAViewFromMultiView(state);
    }
    else {
        viewer= state.find( (entry) => entry.viewerId===viewerId);
        if (!viewer) {
            state= addViewer(state,{viewerId});
            viewer= state.find( (entry) => entry.viewerId===viewerId);
        }
    }

    plotIdAry= union(viewer.plotIdAry,plotIdAry);
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {plotIdAry}) : entry);
}

/**
 *
 * @param {MultiViewerRoot} state
 * @param {string} viewerId
 * @param {string[]} plotIdAry
 * @return {MultiViewerRoot}
 */
function replaceImages(state,viewerId,plotIdAry) {
    var viewer;
    if (viewerId===ANY_VIEWER_RESERVED) {
        viewer= getAViewFromMultiView(state);
        viewerId= viewer.viewerId;
        if (!viewerId) return state;
    }
    else {
        viewer= state.find( (entry) => entry.viewerId===viewerId);
        if (!viewer) {
            state= addViewer(state,{viewerId});
        }
    }

    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {plotIdAry}) : entry);
}


function addToAutoReceiver(state,action) {
    const {imageAry}= action.payload;
    return state.map( (entry) => 
              entry.canReceiveNewPlots === NewPlotMode.create_replace.key ? clone(entry, {plotIdAry: union(entry.plotIdAry,imageAry)}) : entry);
}


function removeImages(state,action) {
    var {viewerId,plotIdAry}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;

    plotIdAry= difference(viewer.plotIdAry,plotIdAry);
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {plotIdAry}) : entry);
}


function deletePlotView(state,action) {
    var {plotId}= action.payload;
    return state.map( (entry) => {
        if (!entry.plotIdAry.includes( plotId)) return entry;
        return clone(entry, {plotIdAry: without(entry.plotIdAry, plotId)});
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



