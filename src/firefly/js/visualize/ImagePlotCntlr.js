/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import update from 'react-addons-update';
import {flux} from '../Firefly.js';
import PlotImageTask from './PlotImageTask.js';
import {makeZoomAction as zoomActionCreator,doDispatchZoom} from './ZoomUtil.js';
import {makeColorChangeAction as colorChangeActionCreator,
        makeStretchChangeAction as stretchChangeActionCreator,
        makeRotateAction as rotateActionCreator} from './PlotChangeTask.js';
import {getPlotGroupById} from './PlotGroup.js';
import HandlePlotChange from './reducer/HandlePlotChange.js';
import HandlePlotCreation from './reducer/HandlePlotCreation.js';
import {
    isActivePlotView,
    getPlotViewById,
    expandedPlotViewAry,
    getActivePlotView,
    applyToOnePvOrGroup } from './PlotViewUtil.js';

import {doDispatchZoomLocking} from './ZoomUtil.js';
import CoordinateSys from './CoordSys.js';

export const ExpandType= new Enum(['COLLAPSE', 'GRID', 'SINGLE']);
const WcsMatchMode= new Enum (['NorthAndCenter', 'ByUserPositionAndZoom']);

const ANY_CHANGE= 'ImagePlotCntlr/AnyChange';


/**
 * All PLOT_IMAGES actions should contain:
 * {string} plotId,
 * {WebPlotRequest} wpRequest,
 *  or
 * {WebPlotRequest} redReq, blueReq, greenReq - must contain one
 * {boolean} addToHistory - optional
 * @type {string}
 */
const PLOT_IMAGE_START= 'ImagePlotCntlr.PlotImageStart';
const PLOT_IMAGE_FAIL= 'ImagePlotCntlr.PlotImageFail';
const PLOT_IMAGE= 'ImagePlotCntlr.PlotImage';
const ANY_REPLOT= 'ImagePlotCntlr.Replot';

const ZOOM_IMAGE_START= 'ImagePlotCntlr.ZoomImageStart';
const ZOOM_IMAGE= 'ImagePlotCntlr.ZoomImage';
const ZOOM_IMAGE_FAIL= 'ImagePlotCntlr.ZoomImageFail';

const ZOOM_LOCKING= 'ImagePlotCntlr.ZoomEnableLocking';


const COLOR_CHANGE_START= 'ImagePlotCntlr.ColorChangeStart';
const COLOR_CHANGE= 'ImagePlotCntlr.ColorChange';
const COLOR_CHANGE_FAIL= 'ImagePlotCntlr.ColorChangeFail';


const STRETCH_CHANGE_START= 'ImagePlotCntlr.StretchChangeStart';
const STRETCH_CHANGE= 'ImagePlotCntlr.StretchChange';
const STRETCH_CHANGE_FAIL= 'ImagePlotCntlr.StretchChangeFail';


const ROTATE_START= 'ImagePlotCntlr.RotateChangeStart';
const ROTATE= 'ImagePlotCntlr.RotateChange';
const ROTATE_FAIL= 'ImagePlotCntlr.RotateChangeFail';


const FLIP_IMAGE_START= 'ImagePlotCntlr.FlipImageStart';
const FLIP_IMAGE= 'ImagePlotCntlr.FlipImage';
const FLIP_IMAGE_FAIL= 'ImagePlotCntlr.FlipImageFail';


const CROP_IMAGE_START= 'ImagePlotCntlr.CropImageStart';
const CROP_IMAGE= 'ImagePlotCntlr.CropImage';
const CROP_IMAGE_FAIL= 'ImagePlotCntlr.CropImageFail';

const UPDATE_VIEW_SIZE= 'ImagePlotCntlr.UpdateViewSize';
const PROCESS_SCROLL= 'ImagePlotCntlr.ProcessScroll';


const CHANGE_ACTIVE_PLOT_VIEW= 'ImagePlotCntlr.ChangeActivePlotView';

const CHANGE_PLOT_ATTRIBUTE= 'ImagePlotCntlr.ChangePlotAttribute';

const CHANGE_EXPANDED_MODE= 'ImagePlotCntlr.changeExpandedMode';
const EXPANDED_AUTO_PLAY= 'ImagePlotCntlr.expandedAutoPlay';
const EXPANDED_LIST= 'ImagePlotCntlr.expandedList';

//LZ add those three constant on 2/1/16
//const CHANGE_MOUSE_READOUT_READOUT1 =  ()=> {
//    return {
//      type: CoordinateSys,
//      text:'ImagePlotCntlr.changeMouseReadoutModeReadout1'
//   };
//} ;
//const CHANGE_MOUSE_READOUT_READOUT2= ()=>{
//    return {
//        type: CoordinateSys,
//        text: 'ImagePlotCntlr.changeMouseReadoutModeReadout2'
//    };
//};
const CHANGE_MOUSE_READOUT_READOUT1='ImagePlotCntlr.changeMouseReadoutModeReadout1';
const CHANGE_MOUSE_READOUT_READOUT2='ImagePlotCntlr.changeMouseReadoutModeReadout2';
const CHANGE_MOUSE_READOUT_PIXEL= 'ImagePlotCntlr.changeMouseReadoutModeReadoutPixel';

/**
 * action should contain:
 * todo - add documentation
 */
const PLOT_PROGRESS_UPDATE= 'ImagePlotCntlr.PlotProgressUpdate';

const IMAGE_PLOT_KEY= 'allPlots';



const clone = (obj,params={}) => Object.assign({},obj,params);

export const ActionScope= new Enum(['GROUP','SINGLE', 'LIST']);
export function visRoot() { return flux.getState()[IMAGE_PLOT_KEY]; }

/**
 * The state is best thought of at the following:
 * The state contains an array of PlotView each have a plotId and tie to an Image Viewer, one might be active (PlotView.js)
 * A PlotView has an array of WebPlots, one is primary (WebPlot.js)
 * An ImageViewer shows the primary plot of a plotView. (ImageView.js)
 */
const initState= function() {

    return {
        plotViewAry : [],  //there is one plot view for every ImageViewer, a plotView will have a plotId
        plotGroupAry : [], // there is one for each group, a plot group may have multiple plotViews
        plottingProgressInfo : [], //todo
        plotHistoryRequest: [], //todo
        plotRequestDefaults : {}, // keys are the plot id, values are object with {band : WebPlotRequest}
        activePlotId: null,

        // expanded stuff
        expandedMode: ExpandType.COLLAPSE,
        previousExpandedMode: ExpandType.SINGLE, //  must be SINGLE OR GRID
        singleAutoPlay : false,

        //  misc
        toolBarIsPopup: false,    //todo
        mouseReadoutWide: false, //todo

        //-- wcs match parameters //todo this might have to be in a plotGroup, not sure at this point
        matchWCS: false, //todo
        wcsMatchCenterWP: null, //todo
        wcsMatchMode: WcsMatchMode.ByUserPositionAndZoom, //todo
        mpwWcsPrimId: null,//todo
        mouseReadout1:'eqj2000Dhms',
        mouseReadout2: 'fitsIP',
        pixelSize: 'pixelSize',
        flux: 'Flux'

    };

};

//============ EXPORTS ===========
//============ EXPORTS ===========

export default {
    reducer,
    dispatchProcessScroll,
    dispatch3ColorPlotImage,
    zoomActionCreator, colorChangeActionCreator,
    stretchChangeActionCreator, rotateActionCreator,
    plotImageActionCreator, autoPlayActionCreator,
    dispatchChangeActivePlotView,dispatchAttributeChange,
    ANY_CHANGE, IMAGE_PLOT_KEY,
    PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE,
    ZOOM_IMAGE_START, ZOOM_IMAGE_FAIL, ZOOM_IMAGE,ZOOM_LOCKING,
    ROTATE_START, ROTATE, ROTATE_FAIL,
    COLOR_CHANGE_START, COLOR_CHANGE, COLOR_CHANGE_FAIL,
    STRETCH_CHANGE_START, STRETCH_CHANGE, STRETCH_CHANGE_FAIL,
    PLOT_PROGRESS_UPDATE, UPDATE_VIEW_SIZE, PROCESS_SCROLL,
    CHANGE_PLOT_ATTRIBUTE,EXPANDED_AUTO_PLAY,EXPANDED_LIST,
    ANY_REPLOT
};




//============ EXPORTS ===========
//============ EXPORTS ===========


//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================


/**
 *
 * @param {string} plotId
 * @param {number} cbarId
 * @param {ActionScope} actionScope
 */
export function dispatchColorChange(plotId, cbarId, actionScope=ActionScope.GROUP ) {
    flux.process({ type: COLOR_CHANGE,
        payload: { plotId, cbarId, actionScope }});
}

/**
 *
 * @param {string} plotId
 * @param {number} rangeValues
 * @param {ActionScope} actionScope
 */
export function dispatchStretchChange(plotId, rangeValues, actionScope=ActionScope.GROUP ) {
    flux.process({ type: STRETCH_CHANGE,
        payload: { plotId, rangeValues, actionScope }});
}


/**
 * Rotate
 *
 * @param {string} plotId
 * @param {object} rotateType enum RotateType
 * @param {number} angle
 * @param actionScope enum ActionScope
 */
export function dispatchRotate(plotId, rotateType, angle, actionScope=ActionScope.GROUP ) {
    flux.process({ type: ROTATE,
        payload: { plotId, angle, rotateType, actionScope, newZoomLevel:0 }});
}

/**
 * Move the scroll point on this plotId and possible others if it is grouped.
 *
 * @param {string} plotId
 * @param scrollScreenPt a new point to scroll to in screen coordinates
 */
export function dispatchProcessScroll(plotId,scrollScreenPt) {
    flux.process({type: PROCESS_SCROLL,
        payload: {plotId, scrollScreenPt}
    });
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
 *
 * @param {string} plotId is required unless defined in the WebPlotRequest
 * @param {WebPlotRequest} wpRequest, plotting parameters, required
 * @param {boolean} removeOldPlot Remove the old plot from the plotview and tell the server to delete the context.
 *                                This parameter is almost always true
 * @param {boolean} addToHistory add this request to global history of plots
 * @param {boolean} useContextModifications it true the request will be modified to use preferences, rotation, etc
 *                                 should only be false when it is doing a 'restore to defaults' type plot
 */
export function dispatchPlotImage(plotId,wpRequest, removeOldPlot= true, addToHistory=false, useContextModifications= true ) {
    if (plotId) wpRequest.setPlotId(plotId);
    const payload= initPlotImagePayload(plotId,wpRequest,false, removeOldPlot,addToHistory,useContextModifications);
    payload.wpRequest= wpRequest;
    flux.process({ type: PLOT_IMAGE, payload});
}


/**
 *
 * @param {string} plotId is required unless defined in the WebPlotRequest
 * @param {WebPlotRequest} redReq, red plotting parameters, 1 of red or green or blue is required
 * @param {WebPlotRequest} greenReq, blue plotting parameters, 1 of red or green or blue is required
 * @param {WebPlotRequest} blueReq, green plotting parameters, 1 of red or green or blue is required
 * @param {boolean} removeOldPlot Remove the old plot from the plotview and tell the server to delete the context.
 *                                This parameter is almost always true
 * @param {boolean} addToHistory add this request to global history of plots
 * @param {boolean} useContextModifications it true the request will be modified to use preferences, rotation, etc
 *                                 should only be false when it is doing a 'restore to defaults' type plot
 */
function dispatch3ColorPlotImage(plotId,redReq,blueReq,greenReq,
                                 removeOldPlot= true, addToHistory= false,
                                 useContextModifications= true) {

    if (plotId) {
        [redReq,blueReq,greenReq].forEach( (r) => {if (r) r.setPlotId(plotId);});
    }

    const req= redReq ||  blueReq ||  greenReq;
    const payload= initPlotImagePayload(plotId,req,false, removeOldPlot,addToHistory,useContextModifications);
    payload.redReq= redReq;
    payload.greenReq= greenReq;
    payload.blueReq= blueReq;

    if (payload.plotId) {
        flux.process({ type: PLOT_IMAGE, payload});
    }
    else {
        var error= Error('plotId is required');
        flux.process({ type: PLOT_IMAGE_FAIL, payload: {plotId, error} });
    }
}


/**
 *
 * @param plotId
 * @param {UserZoomTypes} zoomType
 * @param maxCheck
 * @param forceDelay
 * @param zoomLockingEnabled
 */
export function dispatchZoom(plotId,zoomType,maxCheck=true, forceDelay= false, zoomLockingEnabled=false) {
    doDispatchZoom(plotId, zoomType, maxCheck, zoomLockingEnabled, forceDelay);
}

/**
 *
 * @param plotId
 * @param zoomLockingEnabled
 * @param zoomLockingType
 */
export function dispatchZoomLocking(plotId,zoomLockingEnabled, zoomLockingType) {
    doDispatchZoomLocking(plotId,zoomLockingEnabled, zoomLockingType);
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

export function dispatchAttributeChange(plotId,applyToGroup,attKey,attValue) {
    flux.process({ type: CHANGE_PLOT_ATTRIBUTE, payload: {plotId,attKey,attValue,applyToGroup} });
}


/**
 *
 * @param {ExpandType|boolean} expandedMode the mode to change to, it true the expand and match the last one, if false colapse
 */
export function dispatchChangeExpandedMode(expandedMode) {
    flux.process({ type: CHANGE_EXPANDED_MODE, payload: {expandedMode} });


    const enable= expandedMode!==ExpandType.COLLAPSE;
    visRoot().plotViewAry.forEach( (pv) =>
               dispatchZoomLocking(pv.plotId,enable,pv.plotViewCtx.zoomLockingType) );
}



export function dispatchChangeMouseReadoutReadout1( newRadioValue) {

    flux.process({type: CHANGE_MOUSE_READOUT_READOUT1, payload: {newRadioValue }});
}


export function dispatchChangeMouseReadoutReadout2(newRadioValue) {
    flux.process({ type: CHANGE_MOUSE_READOUT_READOUT2, payload: {newRadioValue} });

}


export function dispatchChangeMouseReadoutPixel(readoutPixel) {
    flux.process({ type: CHANGE_MOUSE_READOUT_PIXEL, payload: {readoutPixel} });

}
export function dispatchExpandedAutoPlay(autoPlayOn) {
    flux.process({ type: EXPANDED_AUTO_PLAY, payload: {autoPlayOn} });


}


export function dispatchExpandedList(plotIdAry) {
    flux.process({ type: EXPANDED_LIST, payload: {plotIdAry} });
}

//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================

function plotImageActionCreator(rawAction) {
    return PlotImageTask.makePlotImageAction(rawAction);
}


function autoPlayActionCreator(rawAction) {
    return (dispatcher) => {
        var {autoPlayOn}= rawAction.payload;
        if (autoPlayOn) {
            if (!visRoot().singleAutoPlay) {
                dispatcher(rawAction);
                var id= window.setInterval( () => {
                    var {singleAutoPlay,plotViewAry,activePlotId}= visRoot();
                    if (singleAutoPlay) {
                        const pvAry= expandedPlotViewAry(plotViewAry,activePlotId);
                        const cIdx= pvAry.findIndex( (pv) => pv.plotId===activePlotId);
                        const nextIdx= cIdx===pvAry.length-1 ? 0 : cIdx+1;
                        dispatchChangeActivePlotView(pvAry[nextIdx].plotId);
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
            retState= HandlePlotCreation.reducer(state,action);
            break;


        case ZOOM_LOCKING:
        case ZOOM_IMAGE_START  :
        case ZOOM_IMAGE_FAIL  :
        case ZOOM_IMAGE  :
        case PLOT_PROGRESS_UPDATE  :
        case UPDATE_VIEW_SIZE :
        case PROCESS_SCROLL  :
        case CHANGE_PLOT_ATTRIBUTE:
        case COLOR_CHANGE  :
        case COLOR_CHANGE_FAIL  :
        case STRETCH_CHANGE  :
        case STRETCH_CHANGE_FAIL:
        case EXPANDED_LIST:
            retState= HandlePlotChange.reducer(state,action);
            break;


        case CHANGE_ACTIVE_PLOT_VIEW:
            retState= changeActivePlotView(state,action);
            break;
        case CHANGE_EXPANDED_MODE:
            retState= changeExpandedMode(state,action);
            break;
        case CHANGE_MOUSE_READOUT_READOUT1:
             retState = changeMouseReadoutReadout1(state, action);
            break;
        case CHANGE_MOUSE_READOUT_READOUT2:
            retState = changeMouseReadoutReadout2(state, action);
            break;
        case CHANGE_MOUSE_READOUT_PIXEL:
            retState = changeMouseReadoutPixel(state, action);
            break;
        case EXPANDED_AUTO_PLAY:
            if (state.singleAutoPlay!==action.payload.autoPlayOn) {
                retState= clone(state,{singleAutoPlay:action.payload.autoPlayOn});
            }
            break;
        default:
            break;

    }
    return retState;
}


//============ private functions =================================
//============ private functions =================================
//============ private functions =================================

function changeMouseReadoutReadout1(state, action) {

    var payload = action.payload;
    var newRadioValue = payload.newRadioValue;
    var oldRadioValue = state.mouseReadout1;
    if (newRadioValue ===oldRadioValue) return state;
    return Object.assign({}, state, {mouseReadout1:newRadioValue});

}

function changeMouseReadoutReadout2(state, action){
    var payload = action.payload;
    var newRadioValue = payload.newRadioValue;
    var oldRadioValue = state.mouseReadout2;
    if (newRadioValue ===oldRadioValue) return state;
    return Object.assign({}, state, {mouseReadout2:newRadioValue});

}

function changeMouseReadoutPixel(state, action){
    var payload = action.payload;
    var newReadoutPixel = payload.readoutPixel;
    var oldReadoutPixel  = state. pixelSize;
    if (newReadoutPixel  ===oldReadoutPixel ) return state;
    return Object.assign({}, state, {pixelSize:newReadoutPixel});

}
function changeActivePlotView(state,action) {
    if (action.payload.plotId===state.activePlotId) return state;

    return clone(state, {activePlotId:action.payload.plotId});
}

const includeInExpandedList = (pv,enable) => update(pv, {plotViewCtx : {$merge :{inExpandedList:enable}}});

const isExpanded = (expandedMode) => expandedMode===ExpandType.GRID || expandedMode===ExpandType.SINGLE;

function changeExpandedMode(state,action) {
    var {expandedMode}= action.payload;

    if (expandedMode===true) expandedMode= state.previousExpandedMode;
    else if (!expandedMode) expandedMode= ExpandType.COLLAPSE;

    if (expandedMode===state.expandedMode) return state;

    const {plotViewAry}= state;
    const changes= {expandedMode,singleAutoPlay:false};
    if (isExpanded(expandedMode)) {
        changes.previousExpandedMode= expandedMode;
    }

    if (!isExpanded(expandedMode)) {  // if we are collapsing
        changes.plotViewAry= plotViewAry.map( (pv) =>
                  pv.plotViewCtx.inExpandedList ? includeInExpandedList(pv,false) : pv
        );
    }
    else if (!isExpanded(state.expandedMode)) { // if we are expanding
        const plotId= state.activePlotId;
        const pv= getPlotViewById(state,plotId);
        if (pv) {
            const group= getPlotGroupById(state,pv.plotGroupId);
            changes.plotViewAry= applyToOnePvOrGroup(plotViewAry,plotId,group, (pv) =>includeInExpandedList(pv,true));
        }
    }

    return clone(state, changes);
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





/*

/**
 *
 * @param plotId
 * @param req
 * @param threeColor
 * @param removeOldPlot
 * @param addToHistory
 * @param useContextModifications
 * @return {{plotId: *, plotGroupId: *, removeOldPlot: boolean, addToHistory: boolean, useContextModifications: boolean, groupLocked: *, threeColor: *}}
 */
function initPlotImagePayload(plotId,req, threeColor, removeOldPlot= true, addToHistory=false, useContextModifications= true) {
    if (!plotId) plotId= req.getPlotId();

    const plotGroupId= req.getPlotGroupId();
    const groupLocked= req.isGroupLocked();

    return {plotId, plotGroupId, removeOldPlot,
        addToHistory, useContextModifications,
        groupLocked, threeColor};
}

//============ end private functions =================================
//============ end private functions =================================
//============ end private functions =================================




//============ TEMPORARY interface with GWT=================================

/*globals ffgwt*/

if (window.ffgwt) {
    const allPlots= ffgwt.Visualize.AllPlots.getInstance();
    allPlots.addListener({
        eventNotify(ev) {
            //console.log('ANY_CHANGE:' + ev.getName().getName());
            if (ev.getName().getName()==='Replot') {
                flux.process({type: ANY_CHANGE, payload: { } });
            }
        }
    });
}
