/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get,has,isArray} from 'lodash';
import Enum from 'enum';
import {flux} from '../Firefly.js';
import {clone} from '../util/WebUtil.js';
import PlotImageTask from './task/PlotImageTask.js';
import {UserZoomTypes} from './ZoomUtil.js';
import {reducer as plotChangeReducer} from './reducer/HandlePlotChange.js';
import {reducer as plotCreationReducer} from './reducer/HandlePlotCreation.js';
import {getPlotGroupById} from './PlotGroup.js';
import {isActivePlotView,
        getPlotViewById,
        getOnePvOrGroup,
        applyToOnePvOrGroup,
        findPlotGroup,
        isDrawLayerAttached,
        getDrawLayerByType } from './PlotViewUtil.js';
import {changePrime} from './ChangePrime.js';

import PointSelection from '../drawingLayers/PointSelection.js';
import {dispatchAttachLayerToPlot,
        dispatchCreateDrawLayer,
        dispatchDetachLayerFromPlot,
        DRAWING_LAYER_KEY} from './DrawLayerCntlr.js';
import {dispatchReplaceViewerItems, getExpandedViewerItemIds,
         getMultiViewRoot, EXPANDED_MODE_RESERVED} from './MultiViewCntlr.js';

import {zoomActionCreator} from './ZoomUtil.js';
import {plotImageMaskActionCreator, overlayPlotChangeAttributeActionCreator} from './ImageOverlayTask.js';

import {colorChangeActionCreator,
        stretchChangeActionCreator,
        flipActionCreator,
        cropActionCreator,
        rotateActionCreator} from './task/PlotChangeTask.js';

import {wcsMatchActionCreator} from './task/WcsMatchTask.js';

/** can the 'COLLAPSE', 'GRID', 'SINGLE' */
export const ExpandType= new Enum(['COLLAPSE', 'GRID', 'SINGLE']);

/** can the 'NorthCenOnPt', 'NorthCenOnMoving', 'Standard', 'Off' */
export const WcsMatchType= new Enum(['NorthCenOnPt', 'NorthCenOnMoving', 'StandCenOnPt', 'StandCenOnMoving', 'Standard']);



/**
 * can the 'GROUP', 'SINGLE', 'LIST'
 * @public
 * @global
 */
export const ActionScope= new Enum(['GROUP','SINGLE', 'LIST']);

export const PLOTS_PREFIX= 'ImagePlotCntlr';

const ANY_CHANGE= '${PLOT_PREFIX}.AnyChange';

/** Action Type: plot of new image started */
const PLOT_IMAGE_START= `${PLOTS_PREFIX}.PlotImageStart`;
/** Action Type: plot of new image failed */
const PLOT_IMAGE_FAIL= `${PLOTS_PREFIX}.PlotImageFail`;
/** Action Type: plot of new image completed */
const PLOT_IMAGE= `${PLOTS_PREFIX}.PlotImage`;

/** Action Type: A image replot occurred */
const ANY_REPLOT= `${PLOTS_PREFIX}.Replot`;

/** Action Type: start the zoom process.  The image will appear zoomed by scaling, the server has not updated yet */
const ZOOM_IMAGE_START= `${PLOTS_PREFIX}.ZoomImageStart`;

/** Action Type: The zoom from the server has complete */
const ZOOM_IMAGE= `${PLOTS_PREFIX}.ZoomImage`;
const ZOOM_IMAGE_FAIL= `${PLOTS_PREFIX}.ZoomImageFail`;

const ZOOM_LOCKING= `${PLOTS_PREFIX}.ZoomEnableLocking`;


/** Action Type: image with new color table call started */
const COLOR_CHANGE_START= `${PLOTS_PREFIX}.ColorChangeStart`;
/** Action Type: image with new color table loaded */
const COLOR_CHANGE= `${PLOTS_PREFIX}.ColorChange`;
const COLOR_CHANGE_FAIL= `${PLOTS_PREFIX}.ColorChangeFail`;


/** Action Type: server image stretch call started */
const STRETCH_CHANGE_START= `${PLOTS_PREFIX}.StretchChangeStart`;
/** Action Type: image loaded with new stretch */
const STRETCH_CHANGE= `${PLOTS_PREFIX}.StretchChange`;
const STRETCH_CHANGE_FAIL= `${PLOTS_PREFIX}.StretchChangeFail`;


const ROTATE_START= `${PLOTS_PREFIX}.RotateChangeStart`;
/** Action Type: image rotated */
const ROTATE= `${PLOTS_PREFIX}.RotateChange`;
const ROTATE_FAIL= `${PLOTS_PREFIX}.RotateChangeFail`;


/** Action Type: server image flipped call started */
const FLIP_START= `${PLOTS_PREFIX}.FlipStart`;
/** Action Type: image flipped */
const FLIP= `${PLOTS_PREFIX}.Flip`;
const FLIP_FAIL= `${PLOTS_PREFIX}.FlipFail`;


const CROP_START= `${PLOTS_PREFIX}.CropStart`;
/** Action Type: image cropped */
const CROP= `${PLOTS_PREFIX}.Crop`;
const CROP_FAIL= `${PLOTS_PREFIX}.CropFail`;

const UPDATE_VIEW_SIZE= `${PLOTS_PREFIX}.UpdateViewSize`;
const PROCESS_SCROLL= `${PLOTS_PREFIX}.ProcessScroll`;
/** Action Type: Recenter in image on the active target */
const RECENTER= `${PLOTS_PREFIX}.recenter`;
/** Action Type: replot the image with the original plot parameters */
const RESTORE_DEFAULTS= `${PLOTS_PREFIX}.restoreDefaults`;
const GROUP_LOCKING= `${PLOTS_PREFIX}.GroupLocking`;

const CHANGE_POINT_SELECTION= `${PLOTS_PREFIX}.ChangePointSelection`;

const CHANGE_ACTIVE_PLOT_VIEW= `${PLOTS_PREFIX}.ChangeActivePlotView`;
const CHANGE_PLOT_ATTRIBUTE= `${PLOTS_PREFIX}.ChangePlotAttribute`;

/** Action Type: display mode to or from expanded */
const CHANGE_EXPANDED_MODE= `${PLOTS_PREFIX}.changeExpandedMode`;
/** Action Type: turn on/off expanded auto-play */
const EXPANDED_AUTO_PLAY= `${PLOTS_PREFIX}.expandedAutoPlay`;
/** Action Type: change the primary plot for a multi image fits display */
const CHANGE_PRIME_PLOT= `${PLOTS_PREFIX}.changePrimePlot`;

const CHANGE_MOUSE_READOUT_MODE=`${PLOTS_PREFIX}.changeMouseReadoutMode`;
/** Action Type: delete a plotView */
const DELETE_PLOT_VIEW=`${PLOTS_PREFIX}.deletePlotView`;


const PLOT_MASK_START= `${PLOTS_PREFIX}.plotMaskStart`;
/** Action Type: add a mask image*/
const PLOT_MASK=`${PLOTS_PREFIX}.plotMask`;
const PLOT_MASK_FAIL= `${PLOTS_PREFIX}.plotMaskFail`;
const DELETE_OVERLAY_PLOT=`${PLOTS_PREFIX}.deleteOverlayPlot`;
const OVERLAY_PLOT_CHANGE_ATTRIBUTES=`${PLOTS_PREFIX}.overlayPlotChangeAttributes`;

const WCS_MATCH=`${PLOTS_PREFIX}.wcsMatch`;

const PLOT_PROGRESS_UPDATE= `${PLOTS_PREFIX}.PlotProgressUpdate`;
const API_TOOLS_VIEW= `${PLOTS_PREFIX}.apiToolsView`;

/** Action Type: enable/disable wcs matching*/
export const IMAGE_PLOT_KEY= 'allPlots';


/** @returns {VisRoot} */
export function visRoot() { return flux.getState()[IMAGE_PLOT_KEY]; }




/**
 *
 * @returns {VisRoot}
 */
const initState= function() {

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
     * @prop {PlotGroup[]} plotGroupAry view array
     * @prop {object} plotRequestDefaults - can have multiple values
     * @prop {ExpandType} expandedMode status of expand mode
     * @prop {ExpandType} previousExpandedMode the value last time it was expanded
     * @prop {boolean} singleAutoPlay true if auto play on in expanded mode
     * @prop {boolean} apiToolsView true if working in api mode
     */
    return {
        activePlotId: null,
        plotViewAry : [],  //there is one plot view for every ImageViewer, a plotView will have a plotId
        plotGroupAry : [], // there is one for each group, a plot group may have multiple plotViews
        // plotHistoryRequest: [], //todo

        prevActivePlotId: null,
        plotRequestDefaults : {}, // object: if normal request;
        //                                         {plotId : {threeColor:boolean, wpRequest : object, }
        //                                   if 3 color:
        //                                         {plotId : {threeColor:boolean,
        //                                                    redReq : object,
        //                                                    greenReq : object,
        //                                                    blueReq : object }

        //-- expanded settings
        expandedMode: ExpandType.COLLAPSE,
        previousExpandedMode: ExpandType.GRID, //  must be SINGLE OR GRID
        singleAutoPlay : false,

        //--  misc
        pointSelEnableAry : [],
        apiToolsView: false,

        //-- wcs match parameters //todo this might have to be in a plotGroup, not sure at this point
        wcsMatchCenterWP: null,
        wcsMatchType: false,
        mpwWcsPrimId: null,
    };

};

//============ EXPORTS ===========
//============ EXPORTS ===========



/*---------------------------- REDUCERS -----------------------------*/

function reducers() {
    return {
        [IMAGE_PLOT_KEY]: reducer,
    };
}

function actionCreators() {
    return {
        [PLOT_IMAGE]: plotImageActionCreator,
        [PLOT_MASK]: plotImageMaskActionCreator,
        [OVERLAY_PLOT_CHANGE_ATTRIBUTES]: overlayPlotChangeAttributeActionCreator,
        [ZOOM_IMAGE]: zoomActionCreator,
        [COLOR_CHANGE]: colorChangeActionCreator,
        [STRETCH_CHANGE]: stretchChangeActionCreator,
        [ROTATE]: rotateActionCreator,
        [FLIP]: flipActionCreator,
        [CROP]: cropActionCreator,
        [CHANGE_PRIME_PLOT] : changePrimeActionCreator,
        [CHANGE_POINT_SELECTION]: changePointSelectionActionCreator,
        [RESTORE_DEFAULTS]: restoreDefaultsActionCreator,
        [EXPANDED_AUTO_PLAY]: autoPlayActionCreator,
        [WCS_MATCH]: wcsMatchActionCreator,
        [DELETE_PLOT_VIEW]: deletePlotViewActionCreator,
    };
}


export default {
    reducer,
    reducers, actionCreators,
    ANY_CHANGE,  // todo remove soon- only for interface with GWT
    ANY_REPLOT,
    PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE,
    ZOOM_IMAGE_START, ZOOM_IMAGE_FAIL, ZOOM_IMAGE,ZOOM_LOCKING,
    ROTATE_START, ROTATE, ROTATE_FAIL,
    FLIP_START, FLIP, FLIP_FAIL,
    CROP_START, CROP, CROP_FAIL,
    COLOR_CHANGE_START, COLOR_CHANGE, COLOR_CHANGE_FAIL,
    STRETCH_CHANGE_START, STRETCH_CHANGE, STRETCH_CHANGE_FAIL,
    CHANGE_POINT_SELECTION, CHANGE_EXPANDED_MODE,
    PLOT_PROGRESS_UPDATE, UPDATE_VIEW_SIZE, PROCESS_SCROLL, RECENTER, GROUP_LOCKING,
    RESTORE_DEFAULTS, CHANGE_PLOT_ATTRIBUTE,EXPANDED_AUTO_PLAY,
    DELETE_PLOT_VIEW, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_PRIME_PLOT,
    PLOT_MASK, PLOT_MASK_START, PLOT_MASK_FAIL, DELETE_OVERLAY_PLOT,
    OVERLAY_PLOT_CHANGE_ATTRIBUTES, WCS_MATCH
};




//============ EXPORTS ===========
//============ EXPORTS ===========

const KEY_ROOT= 'progress-';
var  keyCnt= 0;
export function makeUniqueRequestKey() {
    const progressKey= `${KEY_ROOT}-${keyCnt}-${Date.now()}`;
    keyCnt++;
    return progressKey;
}


//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 * 
 * @param apiToolsView
 */
export function dispatchApiToolsView(apiToolsView) {
    flux.process({ type: API_TOOLS_VIEW , payload: { apiToolsView}});
}
/**
 *
 * @param plotId
 * @param message
 * @param done
 */
export function dispatchPlotProgressUpdate(plotId, message, done ) {
    flux.process({ type: PLOT_PROGRESS_UPDATE, payload: { plotId, done, message }});
}

/**
 * Notify that the size of the plot viewing area has changed
 *
 * @param {string} plotId
 * @param {number} width  this parameter should be the offsetWidth of the dom element
 * @param {number} height this parameter should be the offsetHeight of the dom element
 */
export function dispatchUpdateViewSize(plotId,width,height) {
    flux.process({type: UPDATE_VIEW_SIZE,
        payload: {plotId, width, height}
    });
}

/**
 * change group lock for zoom and scrolling
 *
 * @param {string} plotId is required
 * @param {boolean} groupLocked  true to set group lockRelated on
 */
export function dispatchGroupLocking(plotId,groupLocked) {
    flux.process({ type: GROUP_LOCKING, payload :{ plotId, groupLocked }});
}


//--------------

/**
 * Change the primary plot for a multi image fits display 
 * Note - function parameter is a single object
 * @param {Object} p
 * @param {string} p.plotId
 * @param {number} p.primeIdx
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 */
export function dispatchChangePrimePlot({plotId, primeIdx, dispatcher= flux.process}) {
    dispatcher({ type: CHANGE_PRIME_PLOT , payload: { plotId, primeIdx }});
}



/**
 * Show image with new color table loaded
 * Note - function parameter is a single object
 *
 *
 * @param {Object}  obj
 * @param {string} obj.plotId
 * @param {number} obj.cbarId must be in the range, 0 - 21, each number represents different colorbar
 * @param {ActionScope} [obj.actionScope] default to group
 * @param {Function} [obj.dispatcher] only for special dispatching uses such as remote
 *
 *
 * @public
 * @function dispatchColorChange
 * @memberof firefly.action
 */
export function dispatchColorChange({plotId, cbarId, actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({ type: COLOR_CHANGE, payload: { plotId, cbarId, actionScope }});
}

/**
 * Change the image stretch
 * Note - function parameter is a single object
 * @param {Object} obj - object literal with dispatcher parameters
 * @param {string} obj.plotId
 * @param {Array.<Object.<band:Band,rv:RangeValues,bandVisible:boolean>>} obj.stretchData
 * @param {ActionScope} [obj.actionScope] default to group
 * @param {Function} [obj.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchStretchChange
 * @memberof firefly.action
 *
 * @example
 * // Example of stretch 2 - 98 percent, log stretch
 * var rv= RangeValues.makeSimple(‘percent’, 2, 98, ‘log’);
 * const stretchData= [{ band : 'NO_BAND', rv :  rv, bandVisible: true }];
 * action.dispatchStretchChange({plotId:’myplot’, strechData:stretchData });
 * @example
 * // Example of stretch -2 - 5 sigma, linear stretch
 * var rv= RangeValues.makeSimple(’sigma’, -2, 5, 'linear’);
 * const stretchData= [{ band : 'NO_BAND', rv :  rv, bandVisible: true }];
 * action.dispatchStretchChange({plotId:’myplot’, strechData:stretchData });
 *
 */
export function dispatchStretchChange({plotId, stretchData, 
                                       actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({ type: STRETCH_CHANGE, payload: { plotId, stretchData, actionScope }});
}

/**
 * Enable / Disable WCS Match
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {Enum|string} p.matchType one of 'NorthCenOnPt', 'NorthCenOnMoving', 'Standard', 'Off'
 * @param {Function} p.dispatcher
 */
export function dispatchWcsMatch({plotId, matchType, dispatcher= flux.process} ) {
    dispatcher({ type: WCS_MATCH, payload: { plotId, matchType}});
}

/**
 * Rotate image
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {Enum} p.rotateType enum RotateType
 * @param {number} p.angle
 * @param {number} p.newZoomLevel after rotating
 * @param {boolean} p.keepWcsLock it wcs lock is on then keep is on, the default is to unlock wcs
 * @param {ActionScope} p.pactionScope enum ActionScope
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchRotate
 * @memberof firefly.action
 */
export function dispatchRotate({plotId, rotateType, angle=-1, newZoomLevel=0,
                                keepWcsLock= false,
                                actionScope=ActionScope.GROUP,
                                dispatcher= flux.process} ) {
    dispatcher({ type: ROTATE,
        payload: { plotId, angle, rotateType, actionScope, newZoomLevel, keepWcsLock}});
}


/**
 * Flip
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {boolean} p.isY
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchFlip
 * @memberof firefly.action
 */
export function dispatchFlip({plotId, isY=true, dispatcher= flux.process}) {
    dispatcher({ type: FLIP, payload: { plotId, isY}});
}

/**
 * @summary Crop
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {Object} p.imagePt1 image point of corner 1
 * @param {Object} p.imagePt2 image point of corner 2
 * @param {boolean} p.cropMultiAll
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchCrop
 * @memberof firefly.action
 */
export function dispatchCrop({plotId, imagePt1, imagePt2, cropMultiAll, dispatcher= flux.process}) {
    dispatcher({ type: CROP, payload: { plotId, imagePt1, imagePt2, cropMultiAll}});
}


/**
 * @summary Move the scroll point on this plotId and possible others if it is grouped.
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {Object} p.scrollPt a new point to scroll
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 */
export function dispatchProcessScroll({plotId,scrollPt, dispatcher= flux.process}) {
    dispatcher({type: PROCESS_SCROLL, payload: {plotId, scrollPt} });
}


/**
 * @summary recenter the images on the plot center or the ACTIVE_TARGET
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {Point} p.centerPt
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchRecenter
 * @memberof firefly.action
 */
export function dispatchRecenter({plotId, centerPt, dispatcher= flux.process}) {
    dispatcher({type: RECENTER, payload: {plotId, centerPt} });
}

/**
 * replot the image with the original plot parameters
 *
 * Note - function parameter is a single object
 * @param {Object}  p this function take a single parameter
 * @param {string} p.plotId
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchRestoreDefaults({plotId, dispatcher= flux.process}) {
    dispatcher({type: RESTORE_DEFAULTS, payload: {plotId} });
}


/**
 *
 * @summary Plot an image.
 * Note this dispatch function only takes an object with the parameters
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} [p.plotId] is required unless defined in the WebPlotRequest
 * @param {WebPlotRequest|Array} p.wpRequest -  plotting parameters, required or for 3 color pass an array of WebPlotRequest
 * @param {boolean} [p.threeColor] is a three color request, if true the wpRequest should be an array
 * @param {boolean} [p.addToHistory=true] add this request to global history of plots, may be deprecated in the future
 * @param {boolean} [p.useContextModifications=true] it true the request will be modified to use preferences, rotation, etc
 *                                 should only be false when it is doing a 'restore to defaults' type plot
 * @param {boolean} [p.attributes] the are added to the plot
 * @param {Object} [p.pvOptions] parameter specific to the  plotView, only read the first time per plot id
 * @param {boolean} [p.setNewPlotAsActive= true] the new plot will be active
 * @param {boolean} [p.holdWcsMatch= false] if wcs match is on, then modify the request to hold the wcs match
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 * @param {string} [p.viewerId] - viewer that this plot should be put into, only optional if using the default viewer id
 *                                normally you need to specify the viewer
 * @public
 * @function dispatchPlotImage
 * @memberof firefly.action
 */
export function dispatchPlotImage({plotId,wpRequest, threeColor=isArray(wpRequest),
                                  addToHistory=false,
                                  useContextModifications= true,
                                  dispatcher= flux.process,
                                  attributes={},
                                  pvOptions= {},
                                  setNewPlotAsActive= true,
                                  holdWcsMatch= false,
                                  viewerId} ) {

    dispatcher({ type: PLOT_IMAGE,
                   payload: {plotId,wpRequest, threeColor, addToHistory, pvOptions,
                             attributes, holdWcsMatch, setNewPlotAsActive, useContextModifications,viewerId}});
}

/**
 *
 * Note - function parameter is a single object
 * @param {Object}  p this function take a single parameter
 * @param {WebPlotRequest[]} p.wpRequestAry
 * @param {string} p.viewerId
 * @param {Object} p.pvOptions PlotView init Options
 * @param {boolean} [p.setNewPlotAsActive] the last completed plot will be active
 * @param {Function} p.dispatcher only for special dispatching uses such as remote
 */
export function dispatchPlotGroup({wpRequestAry, viewerId, pvOptions= {},
                                   setNewPlotAsActive= true,
                                   dispatcher= flux.process}) {
    dispatcher( { type: PLOT_IMAGE, payload: { wpRequestAry, pvOptions, setNewPlotAsActive, viewerId} });
}


/**
 * @summary Add a mask
 * @param {Object}  p this function take a single parameter
 * @param {string} p.plotId
 * @param {number} p.maskValue power of 2, e.g 4, 8, 32, 128, etc
 * @param {number} p.maskNumber 2, e.g 4, 8, 32, 128, etc
 * @param {string} p.imageOverlayId
 * @param {number} p.imageNumber hdu number of fits
 * @param {string} p.fileKey file on the server
 * @param {string} p.color - color is optional, if not specified, one is chosen
 * @param {string} p.title
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchPlotImage
 * @memberof firefly.action
 */
export function dispatchPlotMask({plotId,imageOverlayId, maskValue, fileKey, imageNumber, maskNumber=-1, color, title, dispatcher= flux.process}) {
    dispatcher( { type: PLOT_MASK, payload: { plotId,imageOverlayId, fileKey, maskValue, imageNumber, maskNumber, color, title} });
}

/**
 *
 *
 * @param {Object}  p this function take a single parameter
 * @param {string} p.plotId
 * @param {string} p.imageOverlayId
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchDeleteOverlayPlot({plotId,imageOverlayId, dispatcher= flux.process}) {
    dispatcher( { type: DELETE_OVERLAY_PLOT, payload: { plotId,imageOverlayId} });
}


/**
 *
 * @param {Object}  p this function take a single parameter
 * @param {string} p.plotId
 * @param {string} p.imageOverlayId
 * @param {Object} p.attributes any attribute in OverlayPlotView
 * @param {boolean} p.doReplot if false don't do a replot just change attributes
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchOverlayPlotChangeAttributes({plotId,imageOverlayId, attributes, doReplot=false, dispatcher= flux.process}) {
    dispatcher( { type: OVERLAY_PLOT_CHANGE_ATTRIBUTES, payload: { plotId,imageOverlayId, attributes, doReplot} });
}



/**
 * Zoom a image
 * Note - function parameter is a single object
 * @param {Object}  p this function take a single parameter
 * @param {string} p.plotId
 * @param {UserZoomTypes} p.userZoomType (one of ['UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV')
 * @param {boolean} [p.maxCheck]
 * @param {boolean} [p.zoomLockingEnabled]
 * @param {boolean} [p.forceDelay]
 * @param {number} [p.level] the level to zoom to, used only userZoomType 'LEVEL'
 * @param {ActionScope} [p.actionScope] default to group
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchZoom
 * @memberof firefly.action
 *
 * @example
 * // Example of zoom to level
 *  action.dispatchZoom({plotId:’myplot’, userZoomType:’LEVEL’, level: .75 });
 *
 * @example
 * // Example of zoom up
 * action.dispatchZoom({plotId:’myplot’, userZoomType:’UP’ }};
 * @example
 * // Example of zoom to fit
 * action.dispatchZoom({plotId:’myplot’, userZoomType:’FIT’ }};
 * @example
 * // Example of zoom to level, if you are connected to a widget that is changing  the level fast, zlevel is the varible with the zoom level
 * action.dispatchZoom({plotId:’myplot’, userZoomType:’LEVEL’, level: zlevel, forceDelay: true }};
 */
export function dispatchZoom({plotId, userZoomType, maxCheck= true,
                             zoomLockingEnabled=false, forceDelay=false, level,
                             actionScope=ActionScope.GROUP,
                             dispatcher= flux.process} ) {
    dispatcher({
        type: ZOOM_IMAGE,
        payload :{
            plotId, userZoomType, actionScope, maxCheck, zoomLockingEnabled, forceDelay, level
        }});
}

/**
 *
 * Note - function parameter is a single object
 * @param {Object}  p this function take a single parameter
 * @param {string} p.plotId
 * @param {boolean} [p.holdWcsMatch= false] if wcs match is on, then modify the request to hold the wcs match
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 * @public
 * @function dispatchDeletePlotView
 * @memberof firefly.action
 */
export function dispatchDeletePlotView({plotId, holdWcsMatch= false, dispatcher= flux.process}) {
    dispatcher({ type: DELETE_PLOT_VIEW, payload: {plotId, holdWcsMatch} });
}


/**
 *
 * @param {string} plotId
 * @param {boolean} zoomLockingEnabled
 * @param {UserZoomTypes|string} zoomLockingType should be 'FIT' or 'FILL'
 */
export function dispatchZoomLocking(plotId,zoomLockingEnabled, zoomLockingType) {
    flux.process({
        type: ZOOM_LOCKING,
        payload :{
            plotId, zoomLockingEnabled, zoomLockingType
        }});
}


/**
 * Set the plotId of the active plot view
 * @param {string} plotId
 */

export function dispatchChangeActivePlotView(plotId) {
    if (!isActivePlotView(visRoot(),plotId)) {
        flux.process({ type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId} });
    }
}

/**
 * 
 * @param plotId
 * @param applyToGroup
 * @param attKey
 * @param attValue
 * @param toAll if a multiImageFits apply to all the images
 */
export function dispatchAttributeChange(plotId,applyToGroup,attKey,attValue,toAll=false) {
    flux.process({ type: CHANGE_PLOT_ATTRIBUTE, payload: {plotId,attKey,attValue,applyToGroup,toAll} });
}

/**
 *
 * @param requester a string id of the requester
 * @param enabled true will add the request to the list, false will remove, when all requests are removed
 *                Point selection will be turned off
 */
export function dispatchChangePointSelection(requester, enabled) {
    flux.process({ type: CHANGE_POINT_SELECTION, payload: {requester,enabled} });
}


/**
 *
 * @param {ExpandType|boolean} expandedMode the mode to change to, it true the expand and match the last one,
 *          if false colapse
 */
export function dispatchChangeExpandedMode(expandedMode) {

    const vr= visRoot();

    if (!isExpanded(vr.expandedMode) && isExpanded(expandedMode)) { // if going from collapsed to expanded
        const plotId= vr.activePlotId;
        const pv= getPlotViewById(vr,plotId);
        if (pv) {
            const group= getPlotGroupById(vr,pv.plotGroupId);
            const plotIdAry= getOnePvOrGroup(vr.plotViewAry,plotId,group).map( (pv) => pv.plotId);
            dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED,plotIdAry);
        }
    }


    flux.process({ type: CHANGE_EXPANDED_MODE, payload: {expandedMode} });


    const enable= expandedMode!==ExpandType.COLLAPSE;
    visRoot().plotViewAry.forEach( (pv) =>
               dispatchZoomLocking(pv.plotId,enable,pv.plotViewCtx.zoomLockingType) );

    if (!enable) {
        visRoot().plotViewAry.forEach( (pv) => {
            const level= pv.plotViewCtx.lastCollapsedZoomLevel;
            if (level>0) {
                dispatchZoom({
                    plotId:pv.plotId,
                    userZoomTypes:UserZoomTypes.LEVEL,
                    level, maxCheck:false,
                    actionScope:ActionScope.SINGLE});
            }
        });
    }
    
}


/**
 * 
 * @param autoPlayOn
 */
export function dispatchExpandedAutoPlay(autoPlayOn) {
    flux.process({ type: EXPANDED_AUTO_PLAY, payload: {autoPlayOn} });


}


//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================


/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function changePrimeActionCreator(rawAction) {
    return (dispatcher, getState) => changePrime(rawAction,dispatcher,getState);
}

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function deletePlotViewActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const vr= getState()[IMAGE_PLOT_KEY];
        if (vr.wcsMatchType && !rawAction.payload.holdWcsMatch) {
            dispatcher({ type: WCS_MATCH, payload: {wcsMatchType:false} });
        }
        dispatcher(rawAction);
    };
}

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function plotImageActionCreator(rawAction) {
    return PlotImageTask.makePlotImageAction(rawAction);
}


/**
 * @param {Action} rawAction
 * @returns {Function}
 */
export function restoreDefaultsActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const vr= getState()[IMAGE_PLOT_KEY];
        const {plotId}= rawAction.payload;
        const {plotGroupAry,plotViewAry}= vr;
        var pv= getPlotViewById(vr,plotId);
        var plotGroup= findPlotGroup(pv.plotGroupId,plotGroupAry);
        applyToOnePvOrGroup( plotViewAry, plotId, plotGroup,
            (pv)=> {
                if (vr.plotRequestDefaults[pv.plotId]) {
                    const def= vr.plotRequestDefaults[pv.plotId];
                    if (def.threeColor) {
                        dispatchPlotImage({plotId:pv.plotId, 
                                           wpRequest:[def.redReq,def.greenReq,def.blueReq],
                                           threeColor:true,
                                           useContextModifications:false});
                    }
                    else {
                        dispatchPlotImage({plotId:pv.plotId, wpRequest:def.wpRequest,
                                           useContextModifications:false});
                    }
                }
            });
    };
}


export function autoPlayActionCreator(rawAction) {
    return (dispatcher) => {
        var {autoPlayOn}= rawAction.payload;
        if (autoPlayOn) {
            if (!visRoot().singleAutoPlay) {
                dispatcher(rawAction);
                var id= window.setInterval( () => {
                    var {singleAutoPlay,activePlotId}= visRoot();
                    if (singleAutoPlay) {

                        const plotIdAry= getExpandedViewerItemIds(getMultiViewRoot());
                        const cIdx= plotIdAry.indexOf(activePlotId);
                        const nextIdx= cIdx===plotIdAry.length-1 ? 0 : cIdx+1;
                        dispatchChangeActivePlotView(plotIdAry[nextIdx]);
                    }
                    else {
                        window.clearInterval(id);
                    }
                },1100);
            }
        }
        else {
            dispatcher(rawAction);
        }
   };
}


const attachAll= (plotViewAry,dl) => plotViewAry.forEach( (pv) => {
                                if (!isDrawLayerAttached(dl,pv.plotId)) {
                                    dispatchAttachLayerToPlot(dl.drawLayerTypeId,pv.plotId,false);
                                }
                        });

const detachAll= (plotViewAry,dl) => plotViewAry.forEach( (pv) => {
                                if (isDrawLayerAttached(dl,pv.plotId)) {
                                    dispatchDetachLayerFromPlot(dl.drawLayerTypeId,pv.plotId,false);
                                }
});


export function changePointSelectionActionCreator(rawAction) {
    return (dispatcher,getState) => {
        var store= getState();
        var wasEnabled= store[IMAGE_PLOT_KEY].pointSelEnableAry.length ? true : false;
        var {plotViewAry}= store[IMAGE_PLOT_KEY];
        var typeId= PointSelection.TYPE_ID;

        dispatcher(rawAction);

        store= getState();
        var dl= getDrawLayerByType(store[DRAWING_LAYER_KEY], typeId);
        if (store[IMAGE_PLOT_KEY].pointSelEnableAry.length && !wasEnabled) {
            if (!dl) {
                dispatchCreateDrawLayer(typeId);
                dl= getDrawLayerByType(getState()[DRAWING_LAYER_KEY], typeId);
            }
            attachAll(plotViewAry,dl);
        }
        else if (wasEnabled) {
            detachAll(plotViewAry,dl);
        }
    };
}




//======================================== Reducer =============================
//======================================== Reducer =============================
//======================================== Reducer =============================

/**
 *
 * @param {VisRoot} state
 * @param {Action} action
 * @returns {VisRoot}
 */
function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;

    var retState= state;
    switch (action.type) {
        case PLOT_IMAGE_START  :
        case PLOT_IMAGE_FAIL  :
        case PLOT_IMAGE  :
        case ROTATE_START:
        case ROTATE_FAIL:
        case ROTATE:
        case FLIP_START:
        case FLIP_FAIL:
        case FLIP:
        case CROP_START:
        case CROP_FAIL:
        case CROP:
        case PLOT_MASK:
        case PLOT_MASK_START:
        case PLOT_MASK_FAIL:
        case DELETE_OVERLAY_PLOT:
            retState= plotCreationReducer(state,action);
            break;


        case ZOOM_LOCKING:
        case ZOOM_IMAGE_START  :
        case ZOOM_IMAGE_FAIL  :
        case ZOOM_IMAGE  :
        case UPDATE_VIEW_SIZE :
        case PROCESS_SCROLL  :
        case CHANGE_PLOT_ATTRIBUTE:
        case COLOR_CHANGE  :
        case COLOR_CHANGE_START  :
        case COLOR_CHANGE_FAIL  :
        case STRETCH_CHANGE_START  :
        case STRETCH_CHANGE  :
        case STRETCH_CHANGE_FAIL:
        case RECENTER:
        case GROUP_LOCKING:
        case PLOT_PROGRESS_UPDATE  :
        case OVERLAY_PLOT_CHANGE_ATTRIBUTES :
        case CHANGE_PRIME_PLOT  :
            retState= plotChangeReducer(state,action);
            break;

        case API_TOOLS_VIEW  :
            retState= clone(state,{apiToolsView:action.payload.apiToolsView});
            break;

        case CHANGE_ACTIVE_PLOT_VIEW:
            retState= changeActivePlotView(state,action);
            break;
        case CHANGE_EXPANDED_MODE:
            retState= changeExpandedMode(state,action);
            break;
        case CHANGE_MOUSE_READOUT_MODE:
             retState = changeMouseReadout(state, action);

            break;
        case EXPANDED_AUTO_PLAY:
            if (state.singleAutoPlay!==action.payload.autoPlayOn) {
                retState= clone(state,{singleAutoPlay:action.payload.autoPlayOn});
            }
            break;
        case CHANGE_POINT_SELECTION:
            retState= changePointSelection(state,action);
            break;
        case DELETE_PLOT_VIEW:
            retState= deletePlotView(state,action);
            break;

        case WCS_MATCH:
            const {wcsMatchCenterWP,wcsMatchType,mpwWcsPrimId}= action.payload;
            retState= clone(state,{wcsMatchCenterWP,wcsMatchType,mpwWcsPrimId});
            break;


        default:
            break;

    }
    validateState(retState,state,action);
    return retState;
}


//============ private functions =================================
//============ private functions =================================
//============ private functions =================================


function validateState(state,originalState,action) {
    if (has(state,'activePlotId') && has(state,'plotViewAry') && has(state,'plotGroupAry')) {
        return state;
    }
    if (console.group) console.group('state invalid after: ' + action.type);
    console.log(action.type);
    console.log('originalState',originalState);
    console.log('new (bad) state',state);
    console.log('action', action);
    if (console.groupEnd) console.groupEnd();
}


function changePointSelection(state,action) {
    var {requester,enabled}= action.payload;
    var {pointSelEnableAry}= state;
    if (enabled) {
        if (pointSelEnableAry.includes(requester)) return state;
        return clone(state,{pointSelEnableAry: [...pointSelEnableAry,requester]});
    }
    else {
        if (!pointSelEnableAry.includes(requester)) return state;
        return clone(state,{pointSelEnableAry: pointSelEnableAry.filter( (e) => e!==requester)});
    }
}

function changeMouseReadout(state, action) {

    var fieldKey=action.payload.readoutType;
    var payload = action.payload;
    var newRadioValue = payload.newRadioValue;
    var oldRadioValue = state[fieldKey];
    if (newRadioValue ===oldRadioValue) return state;
    return Object.assign({}, state, {[fieldKey]:newRadioValue});

}

function changeActivePlotView(state,action) {
    const {plotId}= action.payload;
    if (plotId===state.activePlotId) return state;
    const prevActivePlotId= state.activePlotId;
    if (!getPlotViewById(state,plotId)) return state;

    return clone(state, {prevActivePlotId, activePlotId:action.payload.plotId});
}


const isExpanded = (expandedMode) => expandedMode===true ||
                                     expandedMode===ExpandType.GRID ||
                                     expandedMode===ExpandType.SINGLE;

function changeExpandedMode(state,action) {
    var {expandedMode}= action.payload;

    if (expandedMode===true) expandedMode= state.previousExpandedMode;
    else if (!expandedMode) expandedMode= ExpandType.COLLAPSE;

    if (expandedMode===state.expandedMode) return state;

    const changes= {expandedMode,singleAutoPlay:false};


    if (isExpanded(expandedMode)) { // we are currently expanded, just changing modes, e.g. grid to single
        changes.previousExpandedMode= expandedMode;
        if (state.wcsMatchType) {
            changes.mpwWcsPrimId= state.activePlotId;
        }
    }

    return clone(state, changes);
}


function deletePlotView(state,action) {
    const {plotId}= action.payload;
    if (!state.plotViewAry.find( (pv) => pv.plotId===plotId)) return state;
    
    state= clone(state, {plotViewAry:state.plotViewAry.filter( (pv) => pv.plotId!==plotId)});
    if (state.activePlotId===plotId) {
        state.activePlotId= get(state,'plotViewAry.0.plotId',null);
    }
    if (state.prevActivePlotId===plotId || state.prevActivePlotId===state.activePlotId) {
        state.prevActivePlotId= null;
    }
    if (state.mpwWcsPrimId===plotId) {
        state.mpwWcsPrimId= state.prevActivePlotId;
    }
    return state;
}


//function updateHistory(plotHistoryRequest, action) {
//
//    var {addToHistory}= action;
//    if (addToHistory) {
//        //todo: add to history here -- need to figure out how
//    }
//}






//============ end private functions =================================
//============ end private functions =================================
//============ end private functions =================================

