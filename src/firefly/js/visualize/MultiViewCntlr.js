/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {difference, has, union, without} from 'lodash';
import {call, race, take} from 'redux-saga/effects';
import {REINIT_APP} from '../core/AppDataCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {flux} from '../core/ReduxFlux.js';
import ImagePlotCntlr, {dispatchRecenter, ExpandType, visRoot, WcsMatchType} from './ImagePlotCntlr.js';
import {PlotAttribute} from './PlotAttribute.js';
import {getPlotViewById, primePlot} from './PlotViewUtil.js';

export const META_VIEWER_ID = 'triViewImageMetaData';
export const IMAGE_MULTI_VIEW_KEY= 'imageMultiView';
export const IMAGE_MULTI_VIEW_PREFIX= 'MultiViewCntlr';

export const ADD_VIEWER= `${IMAGE_MULTI_VIEW_PREFIX}.AddViewer`;
export const REMOVE_VIEWER= `${IMAGE_MULTI_VIEW_PREFIX}.RemoveViewer`;
export const VIEWER_MOUNTED= `${IMAGE_MULTI_VIEW_PREFIX}.viewMounted`;
export const VIEWER_UNMOUNTED= `${IMAGE_MULTI_VIEW_PREFIX}.viewUnmounted`;
export const VIEWER_SCROLL= `${IMAGE_MULTI_VIEW_PREFIX}.viewScroll`;
export const ADD_VIEWER_ITEMS= `${IMAGE_MULTI_VIEW_PREFIX}.addViewerItems`;
export const REMOVE_VIEWER_ITEMS= `${IMAGE_MULTI_VIEW_PREFIX}.removeViewerItems`;
export const REPLACE_VIEWER_ITEMS= `${IMAGE_MULTI_VIEW_PREFIX}.replaceViewerItems`;
export const CHANGE_VIEWER_LAYOUT= `${IMAGE_MULTI_VIEW_PREFIX}.changeViewerLayout`;
export const UPDATE_VIEWER_CUSTOM_DATA= `${IMAGE_MULTI_VIEW_PREFIX}.updateViewerCustomData`;
export const ADD_TO_AUTO_RECEIVER = `${IMAGE_MULTI_VIEW_PREFIX}.addToAutoReceiver`;


/**
 * @return {MultiViewerRoot}
 */
export const getMultiViewRoot= () => flux.getState()[IMAGE_MULTI_VIEW_KEY];

export default {
    reducers: () => ({[IMAGE_MULTI_VIEW_KEY]: reducer}),
    actionCreators: () => ({ [CHANGE_VIEWER_LAYOUT]: changeViewerLayoutActionCreator }),
    ADD_VIEWER, REMOVE_VIEWER, ADD_VIEWER_ITEMS, REMOVE_VIEWER_ITEMS, REPLACE_VIEWER_ITEMS,
    VIEWER_MOUNTED, VIEWER_UNMOUNTED, UPDATE_VIEWER_CUSTOM_DATA, CHANGE_VIEWER_LAYOUT,
    reducer
};


export const SINGLE='single';
export const GRID='grid';
export const IMAGE='image';
export const PLOT2D='plot2d';
export const WRAPPER='wrapper';
export const DEFAULT_FITS_VIEWER_ID= 'DEFAULT_FITS_VIEWER_ID';
export const DEFAULT_PLOT2D_VIEWER_ID= 'DEFAULT_PLOT2D_VIEWER_ID';
export const PINNED_CHART_VIEWER_ID = 'PINNED_CHART_VIEWER_ID';
export const EXPANDED_MODE_RESERVED= 'EXPANDED_MODE_RESERVED';
export const GRID_RELATED='gridRelated';
export const GRID_FULL='gridFull';

/**
 * @typedef NewPlotMode
 * enum one of
 * @prop create_replace
 * @prop replace_only
 * @prop none
 * @type {Enum}
 */


/** @type NewPlotMode*/
export const NewPlotMode = new Enum(['create_replace', 'replace_only', 'none']);

function initState() {
    /**
     *
     * @typedef {Object} Viewer
     * @prop {string} viewerId:EXPANDED_MODE_RESERVED,
     * @prop {string[]} itemIdAry
     * @prop {string} layout must be 'single' or 'grid'
     * @prop {string|Object} layoutDetail
     * @prop {object} tblBasedLayout
     * @prop {boolean} canReceiveNewPlots - NewPlotMode.create_replace.key,
     * @prop {boolean} reservedContainer
     * @prop {string} containerType - one of 'image', 'plot2d', 'wrapper'
     * @prop {boolean} mounted - if the react component using the store is mounted
     * @prop {boolean} scroll - if true, the ui can build the grid view to scroll
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
            scroll: false,
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
            scroll: false,
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
            scroll: false,
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
            scroll: false,
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
 * @param {string} viewerId
 */
export const dispatchRemoveViewer= (viewerId) => flux.process({type: REMOVE_VIEWER , payload: {viewerId} });

/**
 *
 * @param {string} viewerId
 * @param {string[]} itemIdAry  array of itemIds
 * @param {string} containerType a string with container type, IMAGE and PLOT2D are predefined
 * @param {string} [renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 *
 */
export const dispatchAddViewerItems= (viewerId, itemIdAry, containerType, renderTreeId) =>
    flux.process({type: ADD_VIEWER_ITEMS , payload: {viewerId, itemIdAry, containerType, renderTreeId} });

/**
 * 
 * @param {string} viewerId
 * @param {string} layout single or grid
 * @param {string} layoutDetail more detail about the type of layout, hint to UI, typically detail is with GRID
 * @param {string} associatedTblId - a table id related to this layout
 */
export const dispatchChangeViewerLayout= (viewerId, layout, layoutDetail=undefined, associatedTblId) =>
    flux.process({type: CHANGE_VIEWER_LAYOUT , payload: {viewerId, layout, layoutDetail, associatedTblId} });

/**
 *
 * @param {string} viewerId
 * @param {string[]} itemIdAry array of string of itemId
 */
export const dispatchRemoveViewerItems= (viewerId, itemIdAry) =>
    flux.process({type: REMOVE_VIEWER_ITEMS , payload: {viewerId, itemIdAry} });


/**
 *
 * @param {string} viewerId
 * @param {string[]} itemIdAry array of string of itemId
 * @param {string} containerType a string with container type, IMAGE and PLOT2D are predefined
 */
export const dispatchReplaceViewerItems= (viewerId, itemIdAry, containerType) =>
    flux.process({type: REPLACE_VIEWER_ITEMS , payload: {viewerId, itemIdAry, containerType} });


/**
 * @param {string} viewerId
 */
export const dispatchViewerMounted= (viewerId) => flux.process({type: VIEWER_MOUNTED , payload: {viewerId} });

/**
 * @param {string} viewerId
 */
export const dispatchViewerUnmounted= (viewerId) => flux.process({type: VIEWER_UNMOUNTED , payload: {viewerId} });

export const dispatchViewerScroll= ({viewerId,scroll=false}) =>
    flux.process({type: VIEWER_SCROLL , payload: {viewerId,scroll} });

/**
 *
 * @param {string} viewerId
 * @param {Object} customData
 */
export const dispatchUpdateCustom= (viewerId, customData) =>
    flux.process({type: UPDATE_VIEWER_CUSTOM_DATA , payload: {viewerId,customData} });


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
    if (pv && primePlot(vr, pv) && vr.wcsMatchType) {
        const centerOnImage= (
            vr.wcsMatchType===WcsMatchType.Standard  ||
            vr.wcsMatchType===WcsMatchType.Pixel ||
            vr.wcsMatchType===WcsMatchType.PixelCenter);
        setTimeout(() => dispatchRecenter({ plotId: vr.activePlotId, centerOnImage}) , 100);
    }
}


/**
 * @param {Action} rawAction
 * @returns {Function}
 */
function changeViewerLayoutActionCreator(rawAction) {
    return (dispatcher, getState) => {
        dispatcher(rawAction);
        const {viewerId,associatedTblId}= rawAction.payload;
        const viewer= getViewer(getState()[IMAGE_MULTI_VIEW_KEY], viewerId);
        const layout= getLayoutType(getState()[IMAGE_MULTI_VIEW_KEY],viewerId,associatedTblId);
        if (viewer?.containerType===IMAGE) {
            dispatchAddSaga(watchForResizing, {
                    plotIdAry:layout===GRID ? viewer.itemIdAry: [viewer.lastActiveItemId]});
        }
    };
}

//======================================== Utilities =============================
//======================================== Utilities =============================
//======================================== Utilities =============================

/**
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {boolean}
 */
export function hasViewerId(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return false;
    return Boolean(multiViewRoot.find((entry) => entry.viewerId === viewerId));
}


/**
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {string} will be 'single' or 'grid'
 * @param {string} [associatedTblId] - a table id related to this layout
 */
export function getLayoutType(multiViewRoot, viewerId, associatedTblId) {
    if (!multiViewRoot || !viewerId) return GRID;
    const v= multiViewRoot.find((entry) => entry.viewerId === viewerId);
    if (v?.tblBasedLayout?.[associatedTblId]?.layout) return v.tblBasedLayout[associatedTblId].layout;
    return v?.layout ?? GRID;
}

/**
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {string|Object} details of this layout
 * @param {string} [associatedTblId] - a table id related to this layout
 */
export function getLayoutDetails(multiViewRoot, viewerId, associatedTblId) {
    if (!multiViewRoot || !viewerId) return undefined;
    const v= multiViewRoot.find((entry) => entry.viewerId === viewerId);
    if (v?.tblBasedLayout?.[associatedTblId]?.layoutDetail) return v.tblBasedLayout[associatedTblId].layoutDetail;
    return v?.layoutDetail;
}

/**
 * @param {MultiViewerRoot} multiViewRoot
 * @return {string[]} an array of item ids
 */
export const getExpandedViewerItemIds= (multiViewRoot) => getViewerItemIds(multiViewRoot,EXPANDED_MODE_RESERVED);

/**
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {string[]} an array of item ids
 */
export function getViewerItemIds(multiViewRoot, viewerId) {
    if (!multiViewRoot || !viewerId) return [];
    const viewerObj= multiViewRoot.find( (entry) => entry.viewerId===viewerId);
    return (viewerObj) ? viewerObj.itemIdAry : [];
}

/**
 * get the viewer for an id
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} viewerId
 * @return {Viewer} a Viewer or undefined if not found
 */
export const getViewer= (multiViewRoot,viewerId) => multiViewRoot?.find( (entry) => entry.viewerId===viewerId);

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
 * @param {string} itemId
 * @param {string} containerType
 * @return {String}
 */
export const findViewerWithItemId= (multiViewRoot, itemId, containerType) =>
    multiViewRoot?.find((entry) =>
        entry.viewerId!==EXPANDED_MODE_RESERVED &&
        entry.itemIdAry.includes(itemId) &&
        entry.containerType===containerType)?.viewerId;

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
                                            (entry?.canReceiveNewPlots===NewPlotMode.create_replace.key)));
    return viewer;
}

/**
 *
 * @param {MultiViewerRoot} multiViewRoot
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
        if (!viewer) return true;
        const plot= primePlot(visRoot,plotId);
        const tbl_id= plot?.attributes.tbl_id ?? plot?.attributes[PlotAttribute.RELATED_TABLE_ID];
        const layout= getLayoutType(multiViewRoot,viewerId,tbl_id);
        return layout===SINGLE;
    }
}

//=============================================
//=============================================
//=============================================


function reducer(state=initState(), action={}) {
    if (!action.payload || !action.type) return state;
    const {payload}= action;

    switch (action.type) {
        case ADD_VIEWER:
            return addViewer(state,payload);
        case REMOVE_VIEWER:
            return removeViewer(state,action);
        case ADD_VIEWER_ITEMS:
            return addItems(state,payload.viewerId,payload.itemIdAry, payload.containerType, payload.renderTreeId);
        case ADD_TO_AUTO_RECEIVER:
            return addToAutoReceiver(state,action);
        case REMOVE_VIEWER_ITEMS:
            return removeItems(state,action);
        case REPLACE_VIEWER_ITEMS:
            return replaceImages(state,payload.viewerId,payload.itemIdAry, payload.containerType);
        case CHANGE_VIEWER_LAYOUT:
            return changeLayout(state,action);
        case VIEWER_MOUNTED:
            return changeMount(state,payload.viewerId,true);
        case VIEWER_UNMOUNTED:
            return changeMount(state,payload.viewerId,false);
        case VIEWER_SCROLL:
            return changeScroll(state,payload.viewerId,payload.scroll);
        case UPDATE_VIEWER_CUSTOM_DATA:
            return updateCustomData(state,action);
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
            return deleteSingleItem(state,payload.plotId, IMAGE);
        case ImagePlotCntlr.PLOT_HIPS:
        case ImagePlotCntlr.PLOT_IMAGE_START:
            const {viewerId, plotId, renderTreeId, pvOptions:{canBeExpanded= true} } = payload;
            if (!imageViewerCanAdd(state,viewerId, plotId)) return state;
            state= addItems(state,payload.viewerId,[payload.plotId], IMAGE, renderTreeId);
            return canBeExpanded ? addItems(state,EXPANDED_MODE_RESERVED,[payload.plotId],IMAGE) : state;
        case ImagePlotCntlr.CHANGE_ACTIVE_PLOT_VIEW:
        case ImagePlotCntlr.PLOT_IMAGE:
            return changeActiveItem(state, payload, IMAGE);
        case REINIT_APP:
            return initState();
        default:
            return state;
    }
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
    const {lastActiveItemId} = payload;

    if (hasViewerId(state,viewerId)) {
        const foundViewer= getViewer(state,viewerId);
        const updatedViewer = {...foundViewer, canReceiveNewPlots, mounted, containerType};

        if (has(updatedViewer, 'lastActiveItemId')) {
            updatedViewer.lastActiveItemId = updatedViewer.lastActiveItemId ? updatedViewer.lastActiveItemId : (updatedViewer?.itemIdAry?.[0] ?? '');
        }
        return state.map( (v) => v.viewerId===updatedViewer.viewerId ? updatedViewer : v);
    } else {
        // set default layout for the viewer with viewerId, META_VIEWER_ID, is full-grid type
        const layoutDetail = viewerId === META_VIEWER_ID ? GRID_FULL : undefined;
        const entry = {viewerId, containerType, canReceiveNewPlots, layout, mounted, itemIdAry: [], customData: {},
                       scroll: false,
                       lastActiveItemId, layoutDetail, renderTreeId, reservedContainer, internallyManaged};
        return [...state, entry];
    }
}


const removeViewer= (state,action) => state.filter( (v) => v.viewId!==action.payload.viewerId);

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
    let viewer= state.find( (entry) => entry.viewerId===viewerId);

    if (!viewer) {
        state= addViewer(state,{viewerId,containerType, renderTreeId});
        viewer= state.find( (entry) => entry.viewerId===viewerId);
    }

    itemIdAry= union(viewer.itemIdAry,itemIdAry);
    return state.map( (entry) => entry.viewerId===viewerId ? {...entry, itemIdAry} : entry);
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
    const viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) {
        state= addViewer(state,{viewerId,containerType});
    }
    const updateViewer = (entry) => {
        if (has(entry, 'lastActiveItemId')) {
            return {itemIdAry, lastActiveItemId: itemIdAry?.[0] ?? ''};
        } else {
            return {itemIdAry};
        }
    };

    return state.map( (entry) => entry.viewerId===viewerId ? {...entry, ...updateViewer(entry)} : entry);
}


function addToAutoReceiver(state,action) {
    const {imageAry}= action.payload;
    return state.map( (entry) => 
              entry.canReceiveNewPlots === NewPlotMode.create_replace.key ? {...entry, itemIdAry: union(entry.itemIdAry,imageAry)} : entry);
}


function removeItems(state,action) {
    let {viewerId,itemIdAry}= action.payload;
    const viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;

    const rmIdAry = itemIdAry.slice();
    itemIdAry= difference(viewer.itemIdAry,itemIdAry);

    const updateViewer = (entry) => {
        if (has(entry, 'lastActiveItemId')&&rmIdAry.includes(entry.lastActiveItemId)) {
            return {itemIdAry, lastActiveItemId: itemIdAry?.[0] ?? ''};
        } else {
            return {itemIdAry};
        }
    };

    return state.map( (entry) => entry.viewerId===viewerId ? {...entry, ...updateViewer(entry)} : entry);
}

/**
 * Delete an item with only knowing the itemId and containerType but not the viewerId
 * @param {MultiViewerRoot} state
 * @param {string} itemId
 * @param {string} containerType
 * @return {MultiViewerRoot}
 */
function deleteSingleItem(state,itemId, containerType) {
    return state.map( (viewer) => {
        if (viewer.containerType!==containerType || !viewer.itemIdAry.includes( itemId)) return viewer;
        const v = {...viewer, itemIdAry: without(viewer.itemIdAry, itemId)};

        if (has(v, 'lastActiveItemId') && (v.lastActiveItemId === itemId)) {
            return {...v, lastActiveItemId: v.itemIdAry?.[0] ?? '' };
        } else {
            return v;
        }
    });
}

function changeLayout(state,action) {
    const {viewerId,layout,layoutDetail,associatedTblId}= action.payload;
    const viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;

    if (associatedTblId) {
        const {tblBasedLayout={}}= viewer;
        const {layout:l, layoutDetail:ld}= tblBasedLayout[associatedTblId] ?? {};
        if (l===layout && ld===layoutDetail) return state;

        return state.map( (entry) => entry.viewerId===viewerId ?
            {...entry, layout,layoutDetail,
                tblBasedLayout:{...tblBasedLayout, [associatedTblId]:{layout,layoutDetail}}
            } :
            entry);
    }
    else {
        if (viewer.layout===layout && viewer.layoutDetail===layoutDetail) return state;
        return state.map( (entry) => entry.viewerId===viewerId ? {...entry, layout,layoutDetail} : entry);
    }

}

function changeMount(state,viewerId,mounted) {
    const viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;
    if (viewer.mounted===mounted) return state;
    return state.map( (entry) => entry.viewerId===viewerId ? {...entry, mounted} : entry);
}

function changeScroll(state,viewerId,scroll) {
    const viewer= state.find( (entry) => entry.viewerId===viewerId);
    if (!viewer) return state;
    if (viewer.scroll===scroll) return state;
    return state.map( (entry) => entry.viewerId===viewerId ? {...entry, scroll} : entry);
}

function updateCustomData(state,action) {
    const {viewerId,customData}= action.payload;
    return state.map( (entry) => entry.viewerId===viewerId ? {...entry, customData} : entry);
}

function changeActiveItem(state, payload, containerType) {
    const {plotId, viewerId} = payload;

    return state.map((viewer) => {
        let isView = false;
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
        return isView ? {...viewer, lastActiveItemId: plotId} : viewer;
    });
}
