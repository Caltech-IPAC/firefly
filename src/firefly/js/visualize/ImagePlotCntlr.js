/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get,isArray} from 'lodash';
import Enum from 'enum';
import {flux} from '../Firefly.js';
import {clone} from '../util/WebUtil.js';
import PlotImageTask from './PlotImageTask.js';
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
import {dispatchReplaceImages, getExpandedViewerPlotIds,
         getMultiViewRoot, EXPANDED_MODE_RESERVED} from './MultiViewCntlr.js';

export {zoomActionCreator} from './ZoomUtil.js';

export {colorChangeActionCreator,
        stretchChangeActionCreator,
        flipActionCreator,
        cropActionCreator,
        rotateActionCreator} from './PlotChangeTask.js';



export const ExpandType= new Enum(['COLLAPSE', 'GRID', 'SINGLE']);
const WcsMatchMode= new Enum (['NorthAndCenter', 'ByUserPositionAndZoom']);


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

const PLOT_PROGRESS_UPDATE= `${PLOTS_PREFIX}.PlotProgressUpdate`;
const API_TOOLS_VIEW= `${PLOTS_PREFIX}.apiToolsView`;

export const IMAGE_PLOT_KEY= 'allPlots';

export const ActionScope= new Enum(['GROUP','SINGLE', 'LIST']);
export function visRoot() { return flux.getState()[IMAGE_PLOT_KEY]; }

/**
 * The state is best thought of at the following:
 * The state contains an array of PlotView each have a plotId and tie to an Image Viewer,
 * one might be active (PlotView.js)
 * A PlotView has an array of WebPlots, one is primary (WebPlot.js)
 * An ImageViewer shows the primary plot of a plotView. (ImageView.js)
 */
const initState= function() {

    return {
        plotViewAry : [],  //there is one plot view for every ImageViewer, a plotView will have a plotId
        plotGroupAry : [], // there is one for each group, a plot group may have multiple plotViews
        plotHistoryRequest: [], //todo
        activePlotId: null,

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
        matchWCS: false, //todo
        wcsMatchCenterWP: null, //todo
        wcsMatchMode: WcsMatchMode.ByUserPositionAndZoom, //todo
        mpwWcsPrimId: null,//todo

        //-- mouse readout settings - todo move to MouseReadoutCntlr
        mouseReadout1:'eqj2000hms',
        mouseReadout2: 'fitsIP',
        pixelSize: 'pixelSize'


    };

};

//============ EXPORTS ===========
//============ EXPORTS ===========

export default {
    reducer,
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
    DELETE_PLOT_VIEW, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_PRIME_PLOT
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
 * @param {boolean} [updateScroll]
 * @param {object} [centerImagePt] image point to center on
 */
export function dispatchUpdateViewSize(plotId,width,height,updateScroll=true,centerImagePt=null) {
    flux.process({type: UPDATE_VIEW_SIZE,
        payload: {plotId, width, height,updateScroll,centerImagePt}
    });
}

/**
 * change group lock for zoom and scrolling
 *
 * @param {string} plotId is required
 * @param {boolean} groupLocked,  true to set group lockRelated on
 */
export function dispatchGroupLocking(plotId,groupLocked) {
    flux.process({ type: GROUP_LOCKING, payload :{ plotId, groupLocked }});
}


//--------------

/**
 * Change the primary plot for a multi image fits display 
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {number} primeIdx
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchChangePrimePlot({plotId, primeIdx, dispatcher= flux.process}) {
    dispatcher({ type: CHANGE_PRIME_PLOT , payload: { plotId, primeIdx }});
}



/**
 * Show image with new color table loaded
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {number} cbarId
 * @param {ActionScope} actionScope
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchColorChange({plotId, cbarId, actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({ type: COLOR_CHANGE, payload: { plotId, cbarId, actionScope }});
}

/**
 *
 * Change the image stretch
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {[{band:object,rv:object,bandVisible:boolean}]} stretchData
 * @param {ActionScope} actionScope
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchStretchChange({plotId, stretchData, 
                                       actionScope=ActionScope.GROUP, dispatcher= flux.process} ) {
    dispatcher({ type: STRETCH_CHANGE, payload: { plotId, stretchData, actionScope }});
}


/**
 * Rotate image
 *
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {object} rotateType enum RotateType
 * @param {number} angle
 * @param actionScope enum ActionScope
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchRotate({plotId, rotateType, angle=-1, actionScope=ActionScope.GROUP,
                                dispatcher= flux.process} ) {
    dispatcher({ type: ROTATE,
        payload: { plotId, angle, rotateType, actionScope, newZoomLevel:0 }});
}


/**
 * Flip
 *
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {boolean} isY
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchFlip({plotId, isY=true, dispatcher= flux.process}) {
    dispatcher({ type: FLIP, payload: { plotId, isY}});
}

/**
 * Crop
 *
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {Object} imagePt1 image point of corner 1
 * @param {Object} imagePt2 image point of corner 2
 * @param {boolean} cropMultiAll
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchCrop({plotId, imagePt1, imagePt2, cropMultiAll, dispatcher= flux.process}) {
    dispatcher({ type: CROP, payload: { plotId, imagePt1, imagePt2, cropMultiAll}});
}


/**
 * Move the scroll point on this plotId and possible others if it is grouped.
 *
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {object} scrollPt a new point to scroll
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchProcessScroll({plotId,scrollPt, dispatcher= flux.process}) {
    dispatcher({type: PROCESS_SCROLL, payload: {plotId, scrollPt} });
}


/**
 * recenter the images on the plot center or the ACTIVE_TARGET
 *
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchRecenter({plotId, dispatcher= flux.process}) {
    dispatcher({type: RECENTER, payload: {plotId} });
}

/**
 * replot the image with the original plot parameters
 *
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchRestoreDefaults({plotId, dispatcher= flux.process}) {
    dispatcher({type: RESTORE_DEFAULTS, payload: {plotId} });
}


/**
 *
 * Plot an image.  Note this dispatch function only takes an object with the parameters
 * Note - function parameter is a single object
 * @param {string} plotId is required unless defined in the WebPlotRequest
 * @param {WebPlotRequest|Array} wpRequest, plotting parameters, required or for 3 color pass an array of WebPlotRequest
 * @param {boolean} threeColor is a three color request, if true the wpRequest should be an array
 * @param {boolean} addToHistory add this request to global history of plots, may be deprecated in the future
 * @param {boolean} useContextModifications it true the request will be modified to use preferences, rotation, etc
 *                                 should only be false when it is doing a 'restore to defaults' type plot
 * @param {function} dispatcher only for special dispatching uses such as remote
 * @param viewerId
 */
export function dispatchPlotImage({plotId,wpRequest, threeColor=isArray(wpRequest),
                                  addToHistory=false,
                                  useContextModifications= true,
                                  dispatcher= flux.process,
                                  viewerId} ) {

    dispatcher({ type: PLOT_IMAGE,
                   payload: {plotId,wpRequest, threeColor, addToHistory, useContextModifications,viewerId}});
}

/**
 *
 * Note - function parameter is a single object
 * @param wpRequestAry
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchPlotGroup({wpRequestAry, dispatcher= flux.process}) {
    dispatcher( { type: PLOT_IMAGE, payload: { wpRequestAry} });
}





/**
 * Zoom a image
 * Note - function parameter is a single object
 * @param {string} plotId
 * @param {UserZoomTypes} userZoomType
 * @param {boolean} maxCheck
 * @param {boolean} zoomLockingEnabled
 * @param {boolean} forceDelay
 * @param {number} level
 * @param {ActionScope} actionScope
 * @param {function} dispatcher only for special dispatching uses such as remote
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
 * @param plotId
 * @param {function} dispatcher only for special dispatching uses such as remote
 */
export function dispatchDeletePlotView({plotId, dispatcher= flux.process}) {
    dispatcher({ type: DELETE_PLOT_VIEW, payload: {plotId} });
}

//--------------

/**
 *
 * @param plotId
 * @param zoomLockingEnabled
 * @param zoomLockingType
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
            dispatchReplaceImages(EXPANDED_MODE_RESERVED,plotIdAry);
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
 * @param readoutType
 * @param newRadioValue
 */
export function dispatchChangeMouseReadout(readoutType, newRadioValue) {
    flux.process({ type: CHANGE_MOUSE_READOUT_MODE, payload: {readoutType, newRadioValue} });
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
 * @param rawAction
 * @return {Function}
 */
export function changePrimeActionCreator(rawAction) {
    return (dispatcher, getState) => changePrime(rawAction,dispatcher,getState);
}


export function plotImageActionCreator(rawAction) {
    return PlotImageTask.makePlotImageAction(rawAction);
}

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

                        const plotIdAry= getExpandedViewerPlotIds(getMultiViewRoot());
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



        default:
            break;

    }
    return retState;
}


//============ private functions =================================
//============ private functions =================================
//============ private functions =================================


function changePointSelection(state,action) {
    var {requester,enabled}= action.payload;
    var {pointSelEnableAry}= state;
    if (enabled) {
        if (pointSelEnableAry.includes(requester)) return state;
        return clone(state,{pointSelEnableAry: [...pointSelEnableAry,requester]});
    }
    else {
        if (!pointSelEnableAry.includes(requester)) return state;
        return clone(state,{pointSelEnableAry: pointSelEnableAry.filter( (e) => e!=requester)});
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
    if (!getPlotViewById(state,plotId)) return state;

    return clone(state, {activePlotId:action.payload.plotId});
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
    }

    return clone(state, changes);
}


function deletePlotView(state,action) {
    const {plotId}= action.payload;
    if (!state.plotViewAry.find( (pv) => pv.plotId===plotId)) return state;
    
    state= clone(state, {plotViewAry:state.plotViewAry.filter( (pv) => pv.plotId!=plotId)});
    if (state.activePlotId===plotId) state.activePlotId= get(state,'plotViewAry.0.plotId',null);
    return state;
}


//todo
//todo
//todo
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




//============ TEMPORARY interface with GWT=================================

/*globals ffgwt*/

if (window.ffgwt) {
    const allPlots= ffgwt.Visualize.AllPlots.getInstance();
    allPlots.addListener({
        eventNotify(ev) {
            if (ev.getName().getName()==='Replot') {
                flux.process({type: ANY_CHANGE, payload: { } });
            }
        }
    });
}
