/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {has, isArray} from 'lodash';
import {REINIT_APP} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {convertToIdentityObj} from '../util/WebUtil.js';
import {changePrime} from './ChangePrime.js';

import {dispatchReplaceViewerItems, EXPANDED_MODE_RESERVED, IMAGE} from './MultiViewCntlr.js';
import {getPlotGroupById} from './PlotGroup.js';
import {getOnePvOrGroup, getPlotViewById, isActivePlotView, isImageExpanded, primePlot} from './PlotViewUtil.js';
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
    flipActionCreator, processScrollActionCreator, recenterActionCreator, rotateActionCreator
} from './task/PlotChangeTask';
import {
    colorChangeActionCreator, cropActionCreator, requestLocalDataActionCreator, stretchChangeActionCreator
} from './task/PlotChangeTask.js';
import {makeAbortHiPSAction} from './task/PlotHipsTask';
import {makeChangeHiPSAction, makeImageOrHiPSAction, makePlotHiPSAction} from './task/PlotHipsTask.js';
import {makePlotImageAction} from './task/PlotImageTask.js';
import {wcsMatchActionCreator} from './task/WcsMatchTask.js';
import {zoomActionCreator} from './task/ZoomTask.js';
import {ZoomType} from './ZoomType.js';

import {UserZoomTypes} from './ZoomUtil.js';

/** @typedef ExpandType
 * enum can be one of
 * @prop COLLAPSE
 * @prop GRID
 * @prop SINGLE
 * @type {Enum}
 */

/** @type ExpandType*/
export const ExpandType= new Enum(['COLLAPSE', 'GRID', 'SINGLE']);

/**
 * @typedef {Object} WcsMatchType
 * enum can be one of
 * @prop Standard
 * @prop Target
 * @prop Pixel
 * @prop PixelCenter
 * @type {Enum}
 */

/** @type WcsMatchType */
export const WcsMatchType= new Enum(['Standard', 'Target', 'Pixel', 'PixelCenter']);



/**
 * @typedef ActionScope
 * enum can be one of
 * @prop GROUP
 * @prop SINGLE
 * @prop LIST'
 * @type {Enum}
 * @public
 * @global
 */

/** @type ActionScope */
export const ActionScope= new Enum(['GROUP','SINGLE', 'LIST']);

export const PLOTS_PREFIX= 'ImagePlotCntlr';

/** Action Type: plot of new image started */
const PLOT_IMAGE_START= `${PLOTS_PREFIX}.PlotImageStart`;
/** Action Type: plot of new image failed */
const PLOT_IMAGE_FAIL= `${PLOTS_PREFIX}.PlotImageFail`;
/** Action Type: plot of new image completed */
const PLOT_IMAGE= `${PLOTS_PREFIX}.PlotImage`;

/** Action Type: plot of new HiPS image */
const PLOT_HIPS= `${PLOTS_PREFIX}.PlotHiPS`;
const ABORT_HIPS= `${PLOTS_PREFIX}.AbortHiPS`;
const CHANGE_HIPS= `${PLOTS_PREFIX}.ChangeHiPS`;
const PLOT_HIPS_OR_IMAGE= `${PLOTS_PREFIX}.plotHiPSOrImage`;
const PLOT_HIPS_FAIL= `${PLOTS_PREFIX}.PlotHiPSFail`;



/** Action Type: A image replot occurred */
const ANY_REPLOT= `${PLOTS_PREFIX}.Replot`;

/** Action Type: start the zoom process.  The image will appear zoomed by scaling, the server has not updated yet */
const ZOOM_HIPS= `${PLOTS_PREFIX}.ZoomHiPS`;

/** Action Type: The zoom from the server has complete */
const ZOOM_IMAGE= `${PLOTS_PREFIX}.ZoomImage`;
const ZOOM_LOCKING= `${PLOTS_PREFIX}.ZoomEnableLocking`;


/** Action Type: image with new color table loaded */
const COLOR_CHANGE= `${PLOTS_PREFIX}.ColorChange`;

/** Action Type: image loaded with new stretch */
const STRETCH_CHANGE= `${PLOTS_PREFIX}.StretchChange`;

/** Action Type: image rotated */
const ROTATE= `${PLOTS_PREFIX}.Rotate`;

/** Action Type: image flipped */
const FLIP= `${PLOTS_PREFIX}.Flip`;


const CROP_START= `${PLOTS_PREFIX}.CropStart`;
/** Action Type: image cropped */
const CROP= `${PLOTS_PREFIX}.Crop`;
const CROP_FAIL= `${PLOTS_PREFIX}.CropFail`;

const UPDATE_VIEW_SIZE= `${PLOTS_PREFIX}.UpdateViewSize`;
const PROCESS_SCROLL= `${PLOTS_PREFIX}.ProcessScroll`;
const CHANGE_CENTER_OF_PROJECTION= `${PLOTS_PREFIX}.changeCenterOfProjection`;
/** Action Type: Recenter in image on the active target */
const RECENTER= `${PLOTS_PREFIX}.recenter`;
/** Action Type: replot the image with the original plot parameters */
const RESTORE_DEFAULTS= `${PLOTS_PREFIX}.restoreDefaults`;
const POSITION_LOCKING= `${PLOTS_PREFIX}.PositionLocking`;
const OVERLAY_COLOR_LOCKING= `${PLOTS_PREFIX}.OverlayColorLocking`;

const CHANGE_POINT_SELECTION= `${PLOTS_PREFIX}.ChangePointSelection`;

const CHANGE_ACTIVE_PLOT_VIEW= `${PLOTS_PREFIX}.ChangeActivePlotView`;
const CHANGE_SUBHIGHLIGHT_PLOT_VIEW= `${PLOTS_PREFIX}.ChangeSubHighlightPlotView`;
const CHANGE_PLOT_ATTRIBUTE= `${PLOTS_PREFIX}.ChangePlotAttribute`;

/** Action Type: display mode to or from expanded */
const CHANGE_EXPANDED_MODE= `${PLOTS_PREFIX}.changeExpandedMode`;
/** Action Type: turn on/off expanded auto-play */
const EXPANDED_AUTO_PLAY= `${PLOTS_PREFIX}.expandedAutoPlay`;
/** Action Type: change the primary plot for a multi image fits display */
const CHANGE_PRIME_PLOT= `${PLOTS_PREFIX}.changePrimePlot`;
const CHANGE_IMAGE_VISIBILITY= `${PLOTS_PREFIX}.changeImageVisibility`;

const CHANGE_MOUSE_READOUT_MODE=`${PLOTS_PREFIX}.changeMouseReadoutMode`;
/** Action Type: delete a plotView */
const DELETE_PLOT_VIEW=`${PLOTS_PREFIX}.deletePlotView`;

const BYTE_DATA_REFRESH= `${PLOTS_PREFIX}.byteDataRefresh`;

const PLOT_MASK_START= `${PLOTS_PREFIX}.plotMaskStart`;
/** Action Type: add a mask image*/
const PLOT_MASK=`${PLOTS_PREFIX}.plotMask`;
const PLOT_MASK_LAZY_LOAD=`${PLOTS_PREFIX}.plotMaskLazyLoad`;
const PLOT_MASK_FAIL= `${PLOTS_PREFIX}.plotMaskFail`;
const DELETE_OVERLAY_PLOT=`${PLOTS_PREFIX}.deleteOverlayPlot`;
const OVERLAY_PLOT_CHANGE_ATTRIBUTES=`${PLOTS_PREFIX}.overlayPlotChangeAttributes`;
const CHANGE_HIPS_IMAGE_CONVERSION=`${PLOTS_PREFIX}.changeHipsImageConversion`;
const CHANGE_TABLE_AUTO_SCROLL=`${PLOTS_PREFIX}.changeTableAutoScroll`;
const USE_TABLE_AUTO_SCROLL=`${PLOTS_PREFIX}.useTableAutoScroll`;
const REQUEST_LOCAL_DATA=`${PLOTS_PREFIX}.requestLocalData`;

const WCS_MATCH=`${PLOTS_PREFIX}.wcsMatch`;

const PLOT_PROGRESS_UPDATE= `${PLOTS_PREFIX}.PlotProgressUpdate`;
const API_TOOLS_VIEW= `${PLOTS_PREFIX}.apiToolsView`;

/** Action Type: enable/disable wcs matching*/
export const IMAGE_PLOT_KEY= 'allPlots';

export const MOUSE_CLICK_REASON= 'mouseClickReason';
export const OTHER_REASON= 'otherReason';

/**
 * @returns {VisRoot}
 *
 * @public
 * @function visRoot
 * @memberof firefly.action
 * */
export function visRoot() { return flux.getState()[IMAGE_PLOT_KEY]; }


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


export default {
    reducers, actionCreators,
    ANY_REPLOT, PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE, PLOT_HIPS, PLOT_HIPS_FAIL, CHANGE_HIPS,ABORT_HIPS,
    ZOOM_HIPS, ZOOM_IMAGE, ZOOM_LOCKING,
    CHANGE_CENTER_OF_PROJECTION, ROTATE, FLIP, CROP_START, CROP, CROP_FAIL,
    COLOR_CHANGE, STRETCH_CHANGE, CHANGE_POINT_SELECTION, CHANGE_EXPANDED_MODE,
    PLOT_PROGRESS_UPDATE, UPDATE_VIEW_SIZE, PROCESS_SCROLL, RECENTER, OVERLAY_COLOR_LOCKING, POSITION_LOCKING,
    RESTORE_DEFAULTS, CHANGE_PLOT_ATTRIBUTE,EXPANDED_AUTO_PLAY,
    DELETE_PLOT_VIEW, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_PRIME_PLOT, CHANGE_IMAGE_VISIBILITY,
    PLOT_MASK, PLOT_MASK_START, PLOT_MASK_FAIL, PLOT_MASK_LAZY_LOAD, DELETE_OVERLAY_PLOT, BYTE_DATA_REFRESH,
    OVERLAY_PLOT_CHANGE_ATTRIBUTES, WCS_MATCH, API_TOOLS_VIEW, CHANGE_MOUSE_READOUT_MODE,
    CHANGE_HIPS_IMAGE_CONVERSION, CHANGE_TABLE_AUTO_SCROLL, USE_TABLE_AUTO_SCROLL,REQUEST_LOCAL_DATA,
    CHANGE_SUBHIGHLIGHT_PLOT_VIEW
};

const KEY_ROOT= 'progress-';
let  keyCnt= 0;
export function makeUniqueRequestKey(prefix= KEY_ROOT) {
    const requestKey= `${prefix}-${keyCnt}-${Date.now()}`;
    keyCnt++;
    return requestKey;
}


//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 * Tweek how the API image view works
 * @param {boolean} apiToolsView
 *
 * @public
 * @function dispatchApiToolsView
 * @memberof firefly.action
 */
export function dispatchApiToolsView(apiToolsView) {
    flux.process({ type: API_TOOLS_VIEW , payload: { apiToolsView}});
}
/**
 *
 * @param {String} plotId -
 * @param {String} message - the message if working or failure
 * @param {boolean} done - true if completed, false if still working
 * @param {String} requestKey
 * @param {boolean} [callSuccess=true] - true if success, false otherwise, parameter ignored if done is false
 */
export function dispatchPlotProgressUpdate(plotId, message, done, requestKey, callSuccess= true ) {
    flux.process({ type: PLOT_PROGRESS_UPDATE, payload: { plotId, done, message, requestKey, callSuccess }});
}

/**
 * Notify that the size of the plot viewing area has changed
 *
 * @param {string} plotId
 * @param {number} [width]  this parameter should be the offsetWidth of the dom element
 * @param {number} [height] this parameter should be the offsetHeight of the dom element
 */
export function dispatchUpdateViewSize(plotId,width,height) {
    flux.process({type: UPDATE_VIEW_SIZE, payload: {plotId, width, height} });
}

/**
 * change overlay/color lock for color change and overlays
 *
 * @param {string} plotId is required
 * @param {boolean} overlayColorLock true to set group lockRelated on
 */
export function dispatchOverlayColorLocking(plotId,overlayColorLock) {
    flux.process({ type: OVERLAY_COLOR_LOCKING, payload :{ plotId, overlayColorLock}});
}

/**
 * change position lock for zoom and scrolling
 *
 * @param {string} plotId is required
 * @param {boolean} positionLock true to set group lockRelated on
 */
export function dispatchPositionLocking(plotId,positionLock) {
    flux.process({ type: POSITION_LOCKING, payload :{ plotId, positionLock }});
}

/**
 * Change the primary plot for a multi image fits display
 * Note - function parameter is a single object
 * @param {Object} p
 * @param {string} p.plotId
 * @param {number} p.primeIdx
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchChangePrimePlot({plotId, primeIdx, dispatcher= flux.process}) {
    dispatcher({ type: CHANGE_PRIME_PLOT , payload: { plotId, primeIdx }});
}

/**
 * set if the base image is visible on a PlotView or an OverlayPlotView
 * @param {Object} p
 * @param {string} p.plotId
 * @param {string} [p.imageOverlayId] if defined change the visibility on the OverlayPlotView match this idea
 * @param {boolean} p.visible - true if visible
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchChangeImageVisibility({plotId, imageOverlayId, visible, dispatcher= flux.process}) {
    dispatcher({ type: CHANGE_IMAGE_VISIBILITY, payload: { plotId, imageOverlayId, visible}});
}

/**
 * Show image with new color table loaded
 * Note - function parameter is a single object
 *
 *
 * @param {Object}  obj
 * @param {string} obj.plotId
 * @param {number} obj.cbarId must be in the range, 0 - 21, each number represents different color bar
 * @param {number} obj.bias bias between 0 - 1, .5 is no bias
 * @param {number} obj.contrast bias between 0 - 2, .1 is no contrast
 * @param {boolean} obj.useRed use this band, only use with 3 color
 * @param {boolean} obj.useGreen use this band, only use with 3 color
 * @param {boolean} obj.useBlue use this band, only use with 3 color
 *
 * @param {string|ActionScope} [obj.actionScope] default to group
 * @param {Function} [obj.dispatcher] only for special dispatching uses such as remote
 *
 *
 * @public
 * @function dispatchColorChange
 * @memberof firefly.action
 */
export function dispatchColorChange({plotId, cbarId, bias, contrast,
                                        useRed=true, useGreen=true, useBlue=true,
                                        actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({ type: COLOR_CHANGE, payload: { plotId, cbarId, bias, contrast, useRed, useGreen, useBlue, actionScope }});
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
 * action.dispatchStretchChange({plotId:’myPlot’, stretchData:stretchData });
 * @example
 * // Example of stretch -2 - 5 sigma, linear stretch
 * var rv= RangeValues.makeSimple(’sigma’, -2, 5, 'linear’);
 * const stretchData= [{ band : 'NO_BAND', rv :  rv, bandVisible: true }];
 * action.dispatchStretchChange({plotId:’myPlot’, stretchData:stretchData });
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
 * @param {WcsMatchType|string|boolean} p.matchType one of 'Standard', 'Off', or you may pass false
 * @param {boolean} [p.lockMatch]
 * @param {Function} [p.dispatcher]
 */
export function dispatchWcsMatch({plotId, matchType, lockMatch= true, dispatcher= flux.process} ) {
    dispatcher({ type: WCS_MATCH, payload: { plotId, matchType, lockMatch}});
}


/**
 * Rotate image, do it client side
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {RotateType} p.rotateType enum RotateType
 * @param {number} [p.angle] - rotation angle- rotation is always toward the east of north. That is east-left images will
 * rotate counter-clockwise, while east-right image will rotate clockwise
 * @param {string|ActionScope} [p.actionScope] enum ActionScope
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchRotate
 * @memberof firefly.action
 */
export function dispatchRotate({plotId, rotateType, angle=-1,
                                actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({ type: ROTATE, payload: { plotId, angle, rotateType, actionScope}});
}

/**
 * @summary Flip
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {boolean} [p.isY]
 * @param {boolean} [p.rematchAfterFlip]
 * @param {string|ActionScope} [p.actionScope] enum ActionScope
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchFlip
 * @memberof firefly.action
 */
export function dispatchFlip({plotId, isY=true, rematchAfterFlip= true,
                                     actionScope=ActionScope.GROUP, dispatcher= flux.process}) {
    dispatcher({ type: FLIP, payload: { plotId, isY, rematchAfterFlip, actionScope}});
}

/**
 * @summary Crop
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
 * @param {Object} [p.disableBoundCheck]
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchProcessScroll({plotId,scrollPt, disableBoundCheck=false, dispatcher= flux.process}) {
    dispatcher({type: PROCESS_SCROLL, payload: {plotId, scrollPt,disableBoundCheck} });
}

/**
 *
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {WorldPt} [p.centerProjPt] new center, if undefined just keep the same center
 * @param {boolean} [p.fullSky] if true AITOFF, false half sky, undefined then don't change
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchChangeCenterOfProjection({plotId,centerProjPt, fullSky, dispatcher= flux.process}) {
    dispatcher({type: CHANGE_CENTER_OF_PROJECTION, payload: {plotId, centerProjPt, fullSky} });
}

/**
 * @summary recenter the images on the plot center or the ACTIVE_TARGET
 *
 * Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} p.plotId
 * @param {Point} [p.centerPt] Point to center on
 * @param {boolean} [p.centerOnImage] only used if centerPt is not defined.  If true then the centering will be
 *                                  the center of the image.  If false, then the center point will be the
 *                                  FIXED_TARGET attribute, if defined. Otherwise it will be the center of the image.
 * @param {boolean} [p.updateFixedTarget] if true and centerPt is a worldPt, it will update the
 *                                  FIXED_TARGET attribute on the plot
 * @param {boolean} [p.updateWcsPrimId] if true update the make plotId the prime for wcs matching, defaults to true
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchRecenter
 * @memberof firefly.action
 */
export function dispatchRecenter({plotId, centerPt= undefined, centerOnImage=false,
                                     updateWcsPrimId=true,
                                     updateFixedTarget= false, dispatcher= flux.process}) {
    dispatcher({type: RECENTER, payload: {plotId, centerPt, centerOnImage, updateFixedTarget, updateWcsPrimId} });
}

/**
 * @summary replot the image with the original plot parameters
 *
 * Note - function parameter is a single object
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 * @function dispatchRestoreDefaults
 */
export function dispatchRestoreDefaults({plotId, dispatcher= flux.process}) {
    dispatcher({type: RESTORE_DEFAULTS, payload: {plotId} });
}


/**
 * @summary Plot an image.
 * @description Note - function parameter is a single object
 * @param {Object}  p
 * @param {string} [p.plotId] is required unless defined in the WebPlotRequest
 * @param {WebPlotParams|WebPlotRequest|Array} p.wpRequest -  plotting parameters, required or for 3 color pass an array of WebPlotParams or WebPlotRequest
 * @param {boolean} [p.threeColor] is a three color request, if true the wpRequest should be an array
 * @param {boolean} [p.useContextModifications=true] it true the request will be modified to use preferences, rotation, etc
 *                                 should only be false when it is doing a 'restore to defaults' type plot
 * @param {Object} [p.attributes] meta data that is added the plot
 * @param {HipsImageConversionSettings} [p.hipsImageConversion= undefined] if defined, use these parameter to
 *                                                convert between image and HiPS
 * @param {Object} [p.pvOptions] parameter specific to the  plotView, only read the first time per plot id
 * @param {boolean} [p.setNewPlotAsActive= true] the new plot will be active
 * @param {boolean} [p.holdWcsMatch= false] if wcs match is on, then modify the request to hold the wcs match
 * @param {boolean} [p.enableRestore= true] if true the original request is saved for restore
 * @param {string} [p.viewerId] - viewer that this plot should be put into, only optional if
 *                                you have added the plot id manually to a viewer.
 *                                otherwise, you need to specify the viewer.
 * @param {string} [p.renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 * @public
 * @function dispatchPlotImage
 * @memberof firefly.action
 */
export function dispatchPlotImage({plotId,wpRequest, threeColor=isArray(wpRequest),
                                  useContextModifications= true,
                                  dispatcher= flux.process,
                                  attributes={},
                                  pvOptions= {},
                                  hipsImageConversion= undefined,
                                  setNewPlotAsActive= true,
                                  holdWcsMatch= true,
                                  enableRestore= true,
                                  viewerId,
                                  renderTreeId} ) {

    dispatcher({ type: PLOT_IMAGE,
                   payload: {plotId,wpRequest, threeColor, pvOptions, hipsImageConversion, enableRestore,
                             attributes, holdWcsMatch, setNewPlotAsActive,
                             useContextModifications,viewerId, renderTreeId}});
}

/**
 * @summary Plot a group of images.
 * Note - function parameter is a single object
 * @param {Object}  p this function takes a single parameter
 * @param {WebPlotRequest[]} p.wpRequestAry
 * @param {string} p.viewerId
 * @param {PVCreateOptions} p.pvOptions PlotView init Options
 * @param {Object} [p.attributes] meta data that is added the plot
 * @param {boolean} [p.setNewPlotAsActive] the last completed plot will be active
 * @param {boolean} [p.holdWcsMatch= true] if wcs match is on, then modify the request to hold the wcs match
 * @param {boolean} [p.enableRestore= true] if true the original request is saved for restore
 * @param {string} [p.renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchPlotGroup({wpRequestAry, viewerId, pvOptions= {},
                                   attributes={}, setNewPlotAsActive= true, holdWcsMatch= true, renderTreeId,
                                   enableRestore= true,
                                   dispatcher= flux.process}) {
    dispatcher( { type: PLOT_IMAGE, payload: { wpRequestAry, pvOptions, attributes, setNewPlotAsActive,
                                               enableRestore, holdWcsMatch, viewerId, renderTreeId} });
}


/**
 * @summary Plot a HiPS display
 * @description Note - function parameter is a single object
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {WebPlotParams|WebPlotRequest} p.wpRequest
 * @param {HipsImageConversionSettings} [p.hipsImageConversion= undefined] if defined, use these parameter to
 * @param {string} [p.viewerId]
 * @param {PVCreateOptions} [p.pvOptions] PlotView init Options
 * @param {Object} [p.attributes] meta data that is added the plot
 * @param {boolean} [p.setNewPlotAsActive] the last completed plot will be active
 * @param {boolean} [p.enableRestore= true] if true the original request is saved for restore
 * @param {string} [p.renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchPlotHiPS
 * @memberof firefly.action
 */
export function dispatchPlotHiPS({plotId,wpRequest, viewerId, pvOptions= {}, attributes={},
                                   hipsImageConversion= undefined,
                                     enableRestore= true, renderTreeId,
                                     setNewPlotAsActive= true, dispatcher= flux.process }) {
    dispatcher( { type: PLOT_HIPS, payload: {wpRequest, plotId, pvOptions, attributes, enableRestore, renderTreeId,
                                             hipsImageConversion, setNewPlotAsActive, viewerId} });
}

export function dispatchAbortHiPS({plotId, dispatcher= flux.process }) {
    dispatcher( { type: ABORT_HIPS, payload: {plotId} });
}


/**
 * @summary Plot a HiPS or a image depending on the FOV size
 * @description Note - function parameter is a single object
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {WebPlotParams|WebPlotRequest} p.hipsRequest
 * @param {WebPlotParams|WebPlotRequest} p.imageRequest - must be a ServiceType request.
 * @param {WebPlotParams|WebPlotRequest} p.allSkyRequest - must be an allsky type request,  this is deprecated, parameter ignored
 * @param {boolean} [p.plotAllSkyFirst= false] - if there is an all sky set up then plot that first
 * @param {number} [p.fovDegFallOver] - the size in degrees that the image will switch between hips and a image cutout
 * @param {number} [p.fovMaxFitsSize] - the max size the fits image service can support
 * @param {boolean} [p.autoConvertOnZoom] - convert between images and FITS on zoom
 * @param {string} [p.viewerId]
 * @param {string} [p.renderTreeId] - used only with multiple rendered tree, like slate in jupyter lab
 * @param {PVCreateOptions} [p.pvOptions] PlotView init Options
 * @param {Object} [p.attributes] meta data that is added the plot
 * @param {boolean} [p.setNewPlotAsActive] the last completed plot will be active
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public 
 * @function dispatchPlotImageOrHiPS
 * @memberof firefly.action
 */
export function dispatchPlotImageOrHiPS({plotId,hipsRequest, imageRequest, allSkyRequest, viewerId, fovDegFallOver=.12,
                                            fovMaxFitsSize= .12, autoConvertOnZoom= false,
                                            pvOptions= {}, attributes={}, plotAllSkyFirst= false,
                                            setNewPlotAsActive= true, renderTreeId, dispatcher= flux.process }) {

    dispatcher( { type: PLOT_HIPS_OR_IMAGE,
        payload: {hipsRequest, imageRequest, allSkyRequest, plotId, fovDegFallOver, pvOptions, renderTreeId,
                 fovMaxFitsSize, autoConvertOnZoom, attributes, setNewPlotAsActive, viewerId, plotAllSkyFirst} });
}

/**
 *
 * @param {Object} p this function takes a single parameter
 * @param {string} p.plotId
 * @param {HipsImageConversionSettings} p.hipsImageConversionChanges  changes to HipsImageConversionSettings, newOptions can contain any key in
 * HipsImageConversionSettings
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchChangeHipsImageConversion({plotId, hipsImageConversionChanges,
                                                           dispatcher= flux.process}) {
    dispatcher( { type: CHANGE_HIPS_IMAGE_CONVERSION, payload: {plotId, hipsImageConversionChanges}});
}



/**
 *
 * @summary change the hips repository or some other attribute
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {string} [p.hipsUrlRoot]
 * @param {CoordinateSys} [p.coordSys]
 * @param {number} [p.cubeIdx]
 * @param {WorldPt} [p.centerProjPt]
 * @param {String} [p.blankColor] set the background color of the hips if is is a blank hips
 * @param {boolean} [p.applyToGroup] apply to the whole group it is locked
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchChangeHiPS({ plotId, hipsUrlRoot, coordSys, centerProjPt, cubeIdx, blankColor,
                                       applyToGroup=true, dispatcher= flux.process }) {
    dispatcher( { type: CHANGE_HIPS, payload: {plotId, hipsUrlRoot, coordSys, cubeIdx, applyToGroup, centerProjPt, blankColor} });
}




/**
 * @summary Add a mask
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {number} p.maskValue power of 2, e.g 4, 8, 32, 128, etc
 * @param {number} p.maskNumber 2, e.g 4, 8, 32, 128, etc
 * @param {string} p.imageOverlayId
 * @param {number} p.imageNumber hdu number of fits
 * @param {string} p.fileKey file on the server
 * @param {string} p.color - color is optional, if not specified, one is chosen
 * @param {string} p.title
 * @param {string} p.lazyLoad
 * @param {string} p.uiCanAugmentTitle
 * @param {string} [p.relatedDataId] pass a related data id if one exist
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchPlotMask
 * @memberof firefly.action
 */
export function dispatchPlotMask({plotId,imageOverlayId, maskValue, fileKey,
                                  imageNumber, maskNumber=-1, color, title,
                                  uiCanAugmentTitle,
                                  relatedDataId, lazyLoad, dispatcher= flux.process}) {

    dispatcher( { type: PLOT_MASK, payload: { plotId,imageOverlayId, fileKey, maskValue,
                                              uiCanAugmentTitle, imageNumber, maskNumber,
                                              color, title, relatedDataId, lazyLoad } });
}

export function dispatchPlotMaskLazyLoad(payload ) {
    flux.process( { type: PLOT_MASK_LAZY_LOAD, payload});
}

/**
 *
 *
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {string} [p.imageOverlayId] the id of the overlay optional if deleteAll is true
 * @param {string} [p.deleteAll] delete all the overlay plot on the given plotId, defaults to false
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 */
export function dispatchDeleteOverlayPlot({plotId,imageOverlayId, deleteAll= false, dispatcher= flux.process}) {
    dispatcher( { type: DELETE_OVERLAY_PLOT, payload: { plotId,imageOverlayId, deleteAll} });
}


/**
 *
 * @param {Object}  p this function takes a single parameter
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
 * @summary Zoom a image
 * Note - function parameter is a single object
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {string|UserZoomTypes} p.userZoomType (one of ['UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV')
 * @param {boolean} [p.maxCheck]
 * @param {boolean} [p.zoomLockingEnabled]
 * @param {number} [p.level] the level to zoom to, used only userZoomType 'LEVEL'
 * @param {number} [p.upDownPercent] value between 0 and 1 - 1 is 100% of the next step up or down
 * @param {string|ActionScope} [p.actionScope] default to group
 * @param {DevicePt} [p.devicePt]
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 *
 * @public
 * @function dispatchZoom
 * @memberof firefly.action
 *
 * @example
 * // Example of zoom to level
 *  action.dispatchZoom({plotId:’myPlot’, userZoomType:’LEVEL’, level: .75 });
 *
 * @example
 * // Example of zoom up
 * action.dispatchZoom({plotId:’myPlot’, userZoomType:’UP’ }};
 * @example
 * // Example of zoom to fit
 * action.dispatchZoom({plotId:’myPlot’, userZoomType:’FIT’ }};
 * @example
 * // Example of zoom to level, if you are connected to a widget that is changing  the level fast, zLevel is the variable with the zoom level
 * action.dispatchZoom({plotId:’myPlot’, userZoomType:’LEVEL’, level: zLevel }};
 */
export function dispatchZoom({plotId, userZoomType, maxCheck= true, upDownPercent=1,
                             zoomLockingEnabled=false, level, devicePt,
                             actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({
        type: ZOOM_IMAGE,
        payload :{ plotId, userZoomType, actionScope, maxCheck, zoomLockingEnabled, level, devicePt, upDownPercent}});
}

/**
 * @summary Delete a PlotView
 * Note - function parameter is a single object
 * @param {Object}  p this function takes a single parameter
 * @param {string} p.plotId
 * @param {boolean} [p.holdWcsMatch= true] if wcs match is on, then modify the request to hold the wcs match
 * @param {Function} [p.dispatcher] only for special dispatching uses such as remote
 * @public
 * @function dispatchDeletePlotView
 * @memberof firefly.action
 */
export function dispatchDeletePlotView({plotId, holdWcsMatch= true, dispatcher= flux.process}) {
    dispatcher({ type: DELETE_PLOT_VIEW, payload: {plotId, holdWcsMatch} });
}


/**
 *
 * @param {string} plotId
 * @param {boolean} zoomLockingEnabled
 * @param {UserZoomTypes|string} zoomLockingType should be 'FIT' or 'FILL'
 */
export function dispatchZoomLocking(plotId,zoomLockingEnabled, zoomLockingType) {
    flux.process({ type: ZOOM_LOCKING, payload :{ plotId, zoomLockingEnabled, zoomLockingType }});
}


/**
 * Set the plotId of the active plot view
 * @param {string} plotId
 * @param {string} reason
 */

export function dispatchChangeActivePlotView(plotId,reason=OTHER_REASON) {
    if (!isActivePlotView(visRoot(),plotId)) {
        flux.process({ type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId,reason} });
    }
}

/**
 * Set the plotId ot be subhighlight
 * @param {Object}  p this function takes a single parameter
 * @param {Array.<{plotId:String, subHighlight:boolean}>} p.subHighlightAry subHighlight updates
 */
export function dispatchChangeSubHighPlotView({subHighlightAry}) {
    flux.process({ type: CHANGE_SUBHIGHLIGHT_PLOT_VIEW, payload: {subHighlightAry} });
}

/**
 *
 * @param {Object}  p this function takes a single parameter
 * @param p.plotId
 * @param p.overlayColorScope
 * @param p.positionScope
 * @param p.attKey
 * @param p.attValue
 * @param {Object} p.changes attribute changes, use if more than one
 * @param p.toAllPlotsInPlotView if a multiImageFits apply to all the images
 */
export function dispatchAttributeChange({plotId,overlayColorScope=true,positionScope=false,
                                            changes, attKey,attValue,toAllPlotsInPlotView=true}) {
    flux.process({
        type: CHANGE_PLOT_ATTRIBUTE,
        payload: {plotId,attKey,attValue,changes,overlayColorScope,positionScope, toAllPlotsInPlotView}
    });
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

export function dispatchChangeTableAutoScroll(enabled) {
    flux.process({ type: CHANGE_TABLE_AUTO_SCROLL, payload: {enabled} });
}

export function dispatchUseTableAutoScroll(useAutoScroll) {
    flux.process({ type: USE_TABLE_AUTO_SCROLL, payload: {useAutoScroll} });
}

export function dispatchRequestLocalData({plotId, plotImageId, imageOverlayId, dataRequested=true}) {
    flux.process({ type: REQUEST_LOCAL_DATA, payload: {plotId,plotImageId, dataRequested, imageOverlayId} });
}

/**
 *
 * @param {ExpandType|boolean} expandedMode the mode to change to, it true the expand and match the last one,
 *          if false colapse
 *
 */
export function dispatchChangeExpandedMode(expandedMode) { //todo: this code should be in a action creator

    const vr= visRoot();

    if (!isImageExpanded(vr.expandedMode) && isImageExpanded(expandedMode)) { // if going from collapsed to expanded
        const plotId= vr.activePlotId;
        const pv= getPlotViewById(vr,plotId);
        if (pv) {
            const group= getPlotGroupById(vr,pv.plotGroupId);
            const plotIdAry= getOnePvOrGroup(vr.plotViewAry,plotId,group, true)
                .filter((pv) => pv.plotViewCtx.canBeExpanded)
                .map( (pv) => pv.plotId);
            dispatchReplaceViewerItems(EXPANDED_MODE_RESERVED,plotIdAry, IMAGE );
        }
    }


    flux.process({ type: CHANGE_EXPANDED_MODE, payload: {expandedMode} });


    const enable= expandedMode!==ExpandType.COLLAPSE;
    visRoot().plotViewAry.forEach( (pv) => {
        const p= primePlot(pv);
        const zlEnabled= enable && p &&
            p.plotState.getWebPlotRequest() &&
            p.plotState.getWebPlotRequest().getZoomType()!==ZoomType.LEVEL;
        dispatchZoomLocking(pv.plotId,zlEnabled,pv.plotViewCtx.zoomLockingType);
    });

    if (!enable) {
        visRoot().plotViewAry.forEach( (pv) => {
            const level= pv.plotViewCtx.lastCollapsedZoomLevel;
            if (level>0) {
                dispatchZoom({
                    plotId:pv.plotId,
                    userZoomType:UserZoomTypes.LEVEL,
                    level, maxCheck:false,
                    actionScope:ActionScope.SINGLE});
            }
        });
    }
}


/**
 * Turn Auto play on
 * @param {boolean} autoPlayOn
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
const changePrimeActionCreator= (rawAction) => (dispatcher, getState) => changePrime(rawAction,dispatcher,getState);



//======================================== Reducer =============================
//======================================== Reducer =============================
//======================================== Reducer =============================

const creationActions= convertToIdentityObj([
    PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE, PLOT_HIPS, PLOT_HIPS_FAIL, CROP_START,
    CROP_FAIL, CROP, PLOT_MASK, PLOT_MASK_START, PLOT_MASK_FAIL, DELETE_OVERLAY_PLOT
]);

const changeActions= convertToIdentityObj([
    ZOOM_LOCKING, ZOOM_HIPS, ZOOM_IMAGE, UPDATE_VIEW_SIZE, PROCESS_SCROLL,
    CHANGE_PLOT_ATTRIBUTE, COLOR_CHANGE, ROTATE, FLIP,
    STRETCH_CHANGE, RECENTER, OVERLAY_COLOR_LOCKING, POSITION_LOCKING,
    PLOT_PROGRESS_UPDATE, OVERLAY_PLOT_CHANGE_ATTRIBUTES, CHANGE_PRIME_PLOT, CHANGE_CENTER_OF_PROJECTION,
    CHANGE_HIPS, CHANGE_HIPS_IMAGE_CONVERSION, CHANGE_IMAGE_VISIBILITY, BYTE_DATA_REFRESH,
    REQUEST_LOCAL_DATA,CHANGE_SUBHIGHLIGHT_PLOT_VIEW
]);

const adminActions= convertToIdentityObj([
    API_TOOLS_VIEW, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_EXPANDED_MODE, CHANGE_MOUSE_READOUT_MODE,
    EXPANDED_AUTO_PLAY, CHANGE_POINT_SELECTION, DELETE_PLOT_VIEW, WCS_MATCH, CHANGE_TABLE_AUTO_SCROLL,
    USE_TABLE_AUTO_SCROLL
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

