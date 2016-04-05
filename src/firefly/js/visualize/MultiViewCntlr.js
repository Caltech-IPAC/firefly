/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {without,union,difference} from 'lodash';
import {flux} from '../Firefly.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';


export const IMAGE_MULTI_VIEW_KEY= 'imageMultiView';

const ADD_VIEWER= 'MultiViewCntlr.AddViewer';
const REMOVE_VIEWER= 'MultiViewCntlr.RemoveViewer';
const VIEWER_MOUNTED= 'MultiViewCntlr.viewMounted';
const VIEWER_UNMOUNTED= 'MultiViewCntlr.viewUnmounted';
const ADD_IMAGES= 'MultiViewCntlr.addImages';
const REMOVE_IMAGES= 'MultiViewCntlr.removeImages';
const REPLACE_IMAGES= 'MultiViewCntlr.replaceImages';
const IMAGE_VIEW_TYPE= 'MultiViewCntlr.imageViewType';
const CHANGE_LAYOUT= 'MultiViewCntlr.changeLayout';
const ADD_TO_AUTO_RECEIVER = 'MultiViewCntlr.addToAutoReceiver';


export function getMultiViewRoot() { 
    return flux.getState()[IMAGE_MULTI_VIEW_KEY]; 
}

const clone = (obj,params={}) => Object.assign({},obj,params);

export default {
    ADD_VIEWER, REMOVE_VIEWER,
    ADD_IMAGES, REMOVE_IMAGES, REPLACE_IMAGES,
    VIEWER_MOUNTED, VIEWER_UNMOUNTED,
    CHANGE_LAYOUT, reducer
};

export const SINGLE='single';
export const GRID='grid';
export const EXPANDED_MODE_RESERVED= 'EXPANDED_MODE_RESERVED';

export const GRID_RELATED='gridRelated';
export const GRID_FULL='gridFull';

function initState() {

    return [
        { viewerId:EXPANDED_MODE_RESERVED,  
            plotIdAry:[], 
            viewType:'single', 
            canReceiveNewPlots: true,
            reservedContainer:true
        }
    ];

    /*
       array  { viewerId : string
                canReceiveNewPlots : boolean // can this viewer support adding images.
                viewType : string // 'single', 'grid'
                plotIdAry : array of string plotId
     *
     */
}




//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 *
 * @param viewerId
 * @param canReceiveNewPlots
 * @param mounted
 */
export function dispatchAddViewer(viewerId, canReceiveNewPlots, mounted=false) {
    flux.process({type: ADD_VIEWER , payload: {viewerId, canReceiveNewPlots,mounted} });
}

/**
 *
 * @param viewerId
 */
export function dispatchRemoveViewer(viewerId) {
    flux.process({type: REMOVE_VIEWER , payload: {viewerId} });
}

/**
 *
 * @param viewerId
 * @param {[]} imageAry  array of {plotId : string, requestAry : array of WebPlotRequest}
 *
 */
export function dispatchAddImages(viewerId, imageAry) {
    flux.process({type: ADD_IMAGES , payload: {viewerId, imageAry} });
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
 * @param viewerId
 * @param layout single or grid
 * @param layoutDetail more detail about the type of layout, hint to UI
 */
export function dispatchChangeLayout(viewerId, layout, layoutDetail) {
    flux.process({type: CHANGE_LAYOUT , payload: {viewerId, layout, layoutDetail} });
}


/**
 *
 * @param viewerId
 * @param {[]} plotIdAry array of string of plotId
 */
export function dispatchRemoveImages(viewerId, plotIdAry) {
    flux.process({type: REMOVE_IMAGES , payload: {viewerId, plotIdAry} });
}

/**
 *
 * @param viewerId
 * @param {[]} imageAry  array of {plotId : string, requestAry : array of WebPlotRequest}
 */
export function dispatchReplaceImages(viewerId, imageAry) {
    flux.process({type: REPLACE_IMAGES , payload: {viewerId, imageAry} });
}

export function dispatchViewerMounted(viewerId) {
    flux.process({type: VIEWER_MOUNTED , payload: {viewerId} });
}

export function dispatchViewerUnmounted(viewerId) {
    flux.process({type: VIEWER_UNMOUNTED , payload: {viewerId} });
}


//======================================== Utilities =============================
//======================================== Utilities =============================
//======================================== Utilities =============================

export function hasViewerId(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return false;
    return multiViewRoot.find((entry) => entry.viewerId === viewerId) ? true : false;
}

export function getLayoutType(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return GRID;
    const v= multiViewRoot.find((entry) => entry.viewerId === viewerId);
    return v ? v.layout : GRID;
}

export function getExpandedViewerPlotIds(multiViewRoot) {
    return getViewerPlotIds(multiViewRoot,EXPANDED_MODE_RESERVED);
}

export function getViewerPlotIds(multiViewRoot,viewerId) {
    if (!multiViewRoot || !viewerId) return null;
    var viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
    return (viewerObj) ? viewerObj.plotIdAry : [];
}

/**
 * get the viewer for an id
 * @param multiViewRoot
 * @param viewerId
 * @return {*}
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

export function getAViewFromMultiView() {
    return  getMultiViewRoot().find((pv) => (!pv.viewerId.includes('RESERVED')));
}


//======================================== Action Creator =============================
//======================================== Action Creator =============================
//======================================== Action Creator =============================

function xxPLACEHOLDERxxxxxActionCreator(rawAction) {  // remember to export
    return (dispatcher) => {
    };
}

//=============================================
//=============================================
//=============================================


function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;

    var retState= state;
    switch (action.type) {
        case ADD_VIEWER:
            retState= addViewer(state,action);
            break;
        case REMOVE_VIEWER:
            retState= removeViewer(state,action);
            break;
        case ADD_IMAGES:
            retState= addImages(state,action);
            break;
        case ADD_TO_AUTO_RECEIVER:
            retState= addToAutoReceiver(state,action);
            break;
        case REMOVE_IMAGES:
            retState= removeImages(state,action);
            break;
        case REPLACE_IMAGES:
            retState= replaceImages(state,action);
            break;
        case CHANGE_LAYOUT:
            retState= changeLayout(state,action);
            break;
        case VIEWER_MOUNTED:
            retState= changeMount(state,action.payload.viewerId,true);
            break;
        case VIEWER_UNMOUNTED:
            retState= changeMount(state,action.payload.viewerId,false);
            break;
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
            retState= deletePlotView(state,action);
            break;
        default:
            break;

    }
    return retState;
}



function addViewer(state,action) {

    const {viewerId,layout=GRID,canReceiveNewPlots=false,mounted}= action.payload;
    if (hasViewerId(state,viewerId)) return state;

    const entry= { viewerId, canReceiveNewPlots, layout, mounted, plotIdAry: [] };
    return [...state,entry];
}


function removeViewer(state,action) {
    const {viewerId}= action.payload;
    return state.filter( (v) => v.viewId!==viewerId);
}


function addImages(state,action) {
    const {viewerId,imageAry}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) {
        state= addViewer(state,action);
        viewer= state.find( (entry) => entry.viewerId===viewerId);
    }
    var plotIdAry= union(viewer.plotIdAry,imageAry);
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {plotIdAry}) : entry);
}


function addToAutoReceiver(state,action) {
    const {imageAry}= action.payload;
    return state.map( (entry) => 
              entry.canReceiveNewPlots ? clone(entry, {plotIdAry: union(entry.plotIdAry,imageAry)}) : entry);
}





function removeImages(state,action) {
    var {viewerId,imageAry}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;

    var plotIdAry= difference(viewer.plotIdAry,imageAry);
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {plotIdAry}) : entry);
}


function deletePlotView(state,action) {
    var {plotId}= action.payload;
    return state.map( (entry) => {
        if (!entry.plotIdAry.includes( plotId)) return entry;
        return clone(entry, {plotIdAry: without(entry.plotIdAry, plotId)});
    });

}

function replaceImages(state,action) {
    const {viewerId,imageAry}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) state= addViewer(state,action);
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {plotIdAry:imageAry}) : entry);
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
