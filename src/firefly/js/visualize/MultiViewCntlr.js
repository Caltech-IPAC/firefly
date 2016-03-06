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
const ADD_IMAGES= 'MultiViewCntlr.addImages';
const REMOVE_IMAGES= 'MultiViewCntlr.addImages';
const REPLACE_IMAGES= 'MultiViewCntlr.addImages';
const IMAGE_VIEW_TYPE= 'MultiViewCntlr.imageViewType';
const CHANGE_LAYOUT= 'MultiViewCntlr.changeLayout';


export function getMultiViewRoot() { return flux.getState()[IMAGE_MULTI_VIEW_KEY]; }

const clone = (obj,params={}) => Object.assign({},obj,params);

export default {
    ADD_VIEWER, REMOVE_VIEWER,
    ADD_IMAGES, REMOVE_IMAGES, REPLACE_IMAGES,
    CHANGE_LAYOUT, reducer
};

export const SINGLE='single';
export const GRID='grid';

function initState() {

    return [];

    /*
       array  { viewerId : string
                canAdd : boolean // can this viewer support adding images.
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
 * @param canAdd
 */
export function dispatchAddViewer(viewerId, canAdd) {
    flux.process({type: ADD_VIEWER , payload: {viewerId, canAdd} });
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
 * @param viewerId
 * @param {string} layout must be grid or single
 */
export function dispatchChangeLayout(viewerId, layout) {
    flux.process({type: CHANGE_LAYOUT , payload: {viewerId, layout} });
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

//======================================== Utilities =============================
//======================================== Utilities =============================
//======================================== Utilities =============================

export function hasViewerId(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return false;
    multiViewRoot.find((entry) => entry.viewerId === viewerId) ? true : false;
}

export function getLayoutType(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return GRID;
    const v= multiViewRoot.find((entry) => entry.viewerId === viewerId);
    return v ? v.layout : GRID;
}


export function getViewerPlotIds(multiViewRoot,viewerId) {
    if (!multiViewRoot || !viewerId) return null;
    var viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
    return (viewerObj) ? viewerObj.plotIdAry : null;
}

export function getViewer(multiViewRoot,viewerId) {
    if (!multiViewRoot || !viewerId) return null;
    return multiViewRoot.find( (entry) => entry.viewerId===viewerId);
}

export function getViewerPlotRequest(multiViewRoot,viewerId) {
    if (!multiViewRoot || !viewerId) return null;
    var viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
    if (!viewerObj) return null;
    return viewerObj.plotIdAry;
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
        case REMOVE_IMAGES:
            retState= removeImages(state,action);
            break;
        case REPLACE_IMAGES:
            retState= replaceImages(state,action);
            break;
        case CHANGE_LAYOUT:
            retState= changeLayout(state,action);
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

    const {viewerId,layout=GRID,canAdd=false}= action.payload;
    if (hasViewerId(state,viewerId)) return state;

    const entry= { viewerId, canAdd, layout, plotIdAry: [] };
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
    const {viewerId,layout}= action.payload;
    var viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;
    if (viewer.layout===layout) return state;
    return state.map( (entry) => entry.viewerId===viewerId ? clone(entry, {layout}) : entry);
}

