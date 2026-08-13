/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has} from 'lodash';

import {REINIT_APP} from '../core/CoreConst';
import {convertToIdentityObj} from '../util/WebUtil.js';
import {changePrime} from './ChangePrime.js';
import {reducer as plotAdminReducer} from './reducer/HandlePlotAdmin.js';
import {reducer as plotChangeReducer} from './reducer/HandlePlotChange.js';
import {reducer as plotCreationReducer} from './reducer/HandlePlotCreation.js';
import {
    overlayPlotChangeAttributeActionCreator, plotImageMaskActionCreator, plotImageMaskLazyActionCreator
} from './task/ImageOverlayTask.js';
import {
    autoPlayActionCreator, changePointSelectionActionCreator, deletePlotViewActionCreator, restoreDefaultsActionCreator
} from './task/PlotAdminTask.js';
import {
    flipActionCreator, processScrollActionCreator, recenterActionCreator, rotateActionCreator,
    colorChangeActionCreator, cropActionCreator, requestLocalDataActionCreator, stretchChangeActionCreator
} from './task/PlotChangeTask';
import {makeAbortHiPSAction, makeChangeHiPSAction, makeImageOrHiPSAction, makePlotHiPSAction} from './task/PlotHipsTask.js';
import {makePlotImageAction} from './task/PlotImageTask.js';
import {wcsMatchActionCreator} from './task/WcsMatchTask.js';
import {zoomActionCreator} from './task/ZoomTask.js';
import {
    ABORT_HIPS, API_TOOLS_VIEW, BYTE_DATA_REFRESH, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_CENTER_OF_PROJECTION,
    CHANGE_EXPANDED_MODE, CHANGE_HIPS, CHANGE_HIPS_IMAGE_CONVERSION, CHANGE_IMAGE_VISIBILITY, CHANGE_MOUSE_READOUT_MODE,
    CHANGE_PLOT_ATTRIBUTE, CHANGE_POINT_SELECTION, CHANGE_PRIME_PLOT, CHANGE_SUBHIGHLIGHT_PLOT_VIEW,
    CHANGE_TABLE_AUTO_SCROLL, COLOR_CHANGE, CROP, CROP_FAIL, CROP_START, DELETE_OVERLAY_PLOT, DELETE_PLOT_VIEW,
    EXPANDED_AUTO_PLAY, ExpandType, FLIP, IMAGE_PLOT_KEY, MARK_OUT_OF_MEMORY,
    OVERLAY_COLOR_LOCKING, OVERLAY_PLOT_CHANGE_ATTRIBUTES, PLOT_HIPS, PLOT_HIPS_FAIL, PLOT_HIPS_OR_IMAGE, PLOT_IMAGE,
    PLOT_IMAGE_FAIL, PLOT_IMAGE_START, PLOT_MASK, PLOT_MASK_FAIL, PLOT_MASK_LAZY_LOAD, PLOT_MASK_START,
    PLOT_PROGRESS_UPDATE, PLOT_PROXY, PLOTS_PREFIX, POSITION_LOCKING, PROCESS_SCROLL, RECENTER, REMOVE_PROXY,
    REQUEST_LOCAL_DATA, RESTORE_DEFAULTS, ROTATE, STRETCH_CHANGE, UPDATE_VIEW_SIZE, USE_TABLE_AUTO_SCROLL, WCS_MATCH,
    ZOOM_HIPS, ZOOM_IMAGE, ZOOM_LOCKING,
} from './VisConst.js';

/**
 *
 * @returns {VisRoot}
 */
const initState= () => {

    /**
     * @global
     * @public
     * @typedef {Object} VisRoot
     *
     * @summary The state of the Image visualization.
     * The state contains an array of PlotView each have a plotId and tie to an Image Viewer,
     * one might be active (PlotView.js)
     * A PlotView has an array of WebPlots, one is primary (WebPlot.js)
     * An ImageViewer shows the primary plot of a plotView. (ImageView.js)
     *
     * @prop {String} activePlotId the id of the active plot
     * @prop {PlotView[]} plotViewAry view array
     * @prop {Object[]} plotProxyAry view array
     * @prop {PlotGroup[]} plotGroupAry view array
     * @prop {object} plotRequestDefaults - can have multiple values
     * @prop {ExpandType} expandedMode status of expand mode
     * @prop {ExpandType} previousExpandedMode the value last time it was expanded
     * @prop {boolean} singleAutoPlay true if auto play on in expanded mode
     * @prop {boolean} apiToolsView true if working in api mode
     * @prop {boolean} positionLock plots are locked together for scrolling and rotation.
     * @prop {WorldPt} wcsMatchCenterWP: null, // the point to match to
     * @prop {WcsMatchType} wcsMatchType   one of 'Standard', 'Target', 'Pixel', 'PixelCenter', or false
     * @prop {String} mpwWcsPrimId  plotId of the prime wcs match image
     * @prop {boolean} autoScrollToHighlightedTableRow
     * @prop {boolean} useAutoScrollToHighlightedTableRow
     *
     */
    return {
        activePlotId: null,
        plotViewAry : [],  //there is one plot view for every ImageViewer, a plotView will have a plotId
        plotGroupAry : [], // there is one for each group, a plot group may have multiple plotViews
        plotProxyAry : [],  // a proxy for a plot view when we need a placeholder before a plot request is made

        prevActivePlotId: null, // previous active plot before current one
        plotRequestDefaults : {}, // object:
        //                           if normal request;
        //                              {plotId : {threeColor:boolean, wpRequest : object}
        //                           if 3 color:
        //                             {plotId : {threeColor:boolean, redReq : object, greenReq : object, blueReq : object }

        //-- expanded settings
        expandedMode: ExpandType.COLLAPSE,
        previousExpandedMode: ExpandType.GRID, //  must be SINGLE OR GRID
        singleAutoPlay : false,

        //--  misc
        pointSelEnableAry : [], // a list of keys who have enable point select, is array length is non-zero, then point select is enabled
        apiToolsView: false,  // this should be deprecated, it is not used for much and there are other ways to do it.
        autoScrollToHighlightedTableRow: true,
        useAutoScrollToHighlightedTableRow: true, // this is not an option, it is used to handle temporary disabling auto scroll`

        //-- wcs match parameters
        positionLock: false, // images are locked together
        wcsMatchCenterWP: null, // the point to match to
        wcsMatchType: false,  // one of 'Standard', 'Target', 'Pixel', 'PixelCenter', or false
        mpwWcsPrimId: null,   // the plotId others are match to
    };

};

/**
 * @global
 * @public
 * @typedef {Object} ProcessedTiles
 *
 * @prop {string} plotId
 * @prop {string} plotImageId
 * @prop {string} imageOverlayId
 * @prop {number} zoomFactor
 * @prop {Array.<ClientTile>} clientTileAry
 */

/**
 * @global
 * @public
 * @typedef {Object} ClientTile
 *
 * @prop {Object} tileAttributes
 * @prop {String} dataUrl
 * @prop {number} width - width of this tile
 * @prop {number} height - height of this tile
 * @prop {number} index - index of this tile
 * @prop {string} url - original file key to use in the service to retrieve this tile
 * @prop {number} x - pixel offset of this tile
 * @prop {number} y - pixel offset of this tile
 */




function reducers() {
    return {
        [IMAGE_PLOT_KEY]: reducer,
    };
}

function actionCreators() {
    return {
        [PLOT_HIPS_OR_IMAGE]: makeImageOrHiPSAction,
        [PLOT_HIPS]: makePlotHiPSAction,
        [ABORT_HIPS]: makeAbortHiPSAction,
        [CHANGE_HIPS]: makeChangeHiPSAction,
        [PLOT_IMAGE]: makePlotImageAction,
        [PLOT_MASK]: plotImageMaskActionCreator,
        [PLOT_MASK_LAZY_LOAD]: plotImageMaskLazyActionCreator,
        [OVERLAY_PLOT_CHANGE_ATTRIBUTES]: overlayPlotChangeAttributeActionCreator,
        [ZOOM_IMAGE]: zoomActionCreator,
        [COLOR_CHANGE]: colorChangeActionCreator,
        [STRETCH_CHANGE]: stretchChangeActionCreator,
        [CROP]: cropActionCreator,
        [CHANGE_PRIME_PLOT] : changePrimeActionCreator,
        [CHANGE_POINT_SELECTION]: changePointSelectionActionCreator,
        [RESTORE_DEFAULTS]: restoreDefaultsActionCreator,
        [EXPANDED_AUTO_PLAY]: autoPlayActionCreator,
        [WCS_MATCH]: wcsMatchActionCreator,
        [DELETE_PLOT_VIEW]: deletePlotViewActionCreator,
        [RECENTER]: recenterActionCreator,
        [PROCESS_SCROLL]: processScrollActionCreator,
        [FLIP]: flipActionCreator,
        [ROTATE]: rotateActionCreator,
        [REQUEST_LOCAL_DATA]: requestLocalDataActionCreator,
    };
}


export default { reducers, actionCreators};



//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================


/**
 * @param {Action} rawAction
 * @returns {Function}
 */
const changePrimeActionCreator= (rawAction) => (dispatcher, getState) => changePrime(rawAction,dispatcher,getState);



//======================================== Reducer =============================
//======================================== Reducer =============================
//======================================== Reducer =============================

const creationActions= convertToIdentityObj([
    PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE, PLOT_HIPS, PLOT_HIPS_FAIL, CROP_START,
    CROP_FAIL, CROP, PLOT_MASK, PLOT_MASK_START, PLOT_MASK_FAIL, DELETE_OVERLAY_PLOT, PLOT_PROXY, REMOVE_PROXY,
]);

const changeActions= convertToIdentityObj([
    ZOOM_LOCKING, ZOOM_HIPS, ZOOM_IMAGE, UPDATE_VIEW_SIZE, PROCESS_SCROLL,
    CHANGE_PLOT_ATTRIBUTE, COLOR_CHANGE, ROTATE, FLIP,
    STRETCH_CHANGE, RECENTER, OVERLAY_COLOR_LOCKING, POSITION_LOCKING,
    PLOT_PROGRESS_UPDATE, OVERLAY_PLOT_CHANGE_ATTRIBUTES, CHANGE_PRIME_PLOT, CHANGE_CENTER_OF_PROJECTION,
    CHANGE_HIPS, CHANGE_HIPS_IMAGE_CONVERSION, CHANGE_IMAGE_VISIBILITY, BYTE_DATA_REFRESH,
    REQUEST_LOCAL_DATA,CHANGE_SUBHIGHLIGHT_PLOT_VIEW, MARK_OUT_OF_MEMORY,
]);

const adminActions= convertToIdentityObj([
    API_TOOLS_VIEW, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_EXPANDED_MODE, CHANGE_MOUSE_READOUT_MODE,
    EXPANDED_AUTO_PLAY, CHANGE_POINT_SELECTION, DELETE_PLOT_VIEW, WCS_MATCH, CHANGE_TABLE_AUTO_SCROLL,
    USE_TABLE_AUTO_SCROLL,
]);



/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @returns {VisRoot}
 */
function reducer(state=initState(), action={}) {
    let retState= state;
    const {type}= action;

    if (!type || !type.startsWith(PLOTS_PREFIX)) return state;

    switch (type) {
        case REINIT_APP:
            return initState();

        case creationActions[type]:
            retState= plotCreationReducer(state,action);
            validateState(retState,state,action);
            break;

        case changeActions[type]:
            retState= plotChangeReducer(state,action);
            validateState(retState,state,action);
            break;

        case adminActions[type]:
            retState= plotAdminReducer(state,action);
            validateState(retState,state,action);
            break;
    }
    return retState;
}


function validateState(state,originalState,action) {
    if (has(state,'activePlotId') && has(state,'plotViewAry') && has(state,'plotGroupAry')) {
        return state;
    }
    if (console.group) console.group('ImagePlotCntlr state invalid after: ' + action.type);
    console.log(action.type);
    console.log('originalState',originalState);
    console.log('new (bad) state',state);
    console.log('action', action);
    if (console.groupEnd) console.groupEnd();
}

