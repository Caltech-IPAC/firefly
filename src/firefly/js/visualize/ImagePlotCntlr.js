/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import Enum from 'enum';
import {flux} from '../Firefly.js';
import PlotImageTask from './PlotImageTask.js';
import ZoomUtil from './ZoomUtil.js';
import HandlePlotChange from './reducer/HandlePlotChange.js';
import HandlePlotCreation from './reducer/HandlePlotCreation.js';
import PlotViewUtil from './PlotViewUtil.js';


const ExpandType= new Enum(['COLLAPSE', 'GRID', 'SINGLE']);
const WcsMatchMode= new Enum (['NorthAndCenter', 'ByUserPositionAndZoom']);

const ANY_CHANGE= 'ImagePlotCntlr/AnyChange';


/**
 * All PLOT_IMAGES actions should contain:
 * {string} plotId,
 * {WebPlotRequest} wpRequest,
 * or
 * {WebPlotRequest} redReq, blueReq, greenReq - must contain one
 * {boolean} addToHistory - optional
 * @type {string}
 */
const PLOT_IMAGE_START= 'ImagePlotCntlr/PlotImageStart';
const PLOT_IMAGE_FAIL= 'ImagePlotCntlr/PlotImageFail';
const PLOT_IMAGE= 'ImagePlotCntlr/PlotImage';
const ANY_REPLOT= 'ImagePlotCntlr/Replot';

const ZOOM_IMAGE_START= 'ImagePlotCntlr/ZoomImageStart';
const ZOOM_IMAGE= 'ImagePlotCntlr/ZoomImage';
const ZOOM_IMAGE_FAIL= 'ImagePlotCntlr/ZoomImageFail';


const FLIP_IMAGE_START= 'ImagePlotCntlr/FlipImageStart';
const FLIP_IMAGE= 'ImagePlotCntlr/FlipImage';
const FLIP_IMAGE_FAIL= 'ImagePlotCntlr/FlipImageFail';


const CROP_IMAGE_START= 'ImagePlotCntlr/CropImageStart';
const CROP_IMAGE= 'ImagePlotCntlr/CropImage';
const CROP_IMAGE_FAIL= 'ImagePlotCntlr/CropImageFail';

const UPDATE_VIEW_SIZE= 'ImagePlotCntlr/UpdateViewSize';
const PROCESS_SCROLL= 'ImagePlotCntlr/ProcessScroll';


const CHANGE_ACTIVE_PLOT_VIEW= 'ImagePlotCntlr/ChangeActivePlotView';
const CHANGE_PLOT_ATTRIBUTE= 'ImagePlotCntlr/ChangePlotAttribute';

/**
 * action should contain:
 * todo - add documentation
 */
const PLOT_PROGRESS_UPDATE= 'ImagePlotCntlr/PlotProgressUpdate';

const IMAGE_PLOT_KEY= 'allPlots';


const initState= function() {

    return {
        plotViewAry : [],
        plotGroupAry : [], // todo, list of list of plotView ids, that are in the same group
        plottingProgressInfo : [], //todo
        plotHistoryRequest: [], //todo
        plotRequestDefaults : {}, // keys are the plot id, values are object with {band : WebPlotRequest}
        activePlotId: null,

        expanded: ExpandType.COLLAPSE, //todo
        toolBarIsPopup: true,    //todo
        mouseReadoutWide: false, //todo

        //-- wcs match parameters
        matchWCS: false, //todo
        wcsMatchCenterWP: null, //todo
        wcsMatchMode: WcsMatchMode.ByUserPositionAndZoom, //todo
        mpwWcsPrimId: null //todo

    };

};

//============ EXPORTS ===========
//============ EXPORTS ===========

export default {
    reducer,
    dispatchUpdateViewSize, dispatchProcessScroll,
    dispatchPlotImage, dispatch3ColorPlotImage, dispatchZoom,
    zoomActionCreator, plotImageActionCreator,
    dispatchChangeActivePlotView,dispatchAttributeChange,
    ANY_CHANGE, IMAGE_PLOT_KEY,
    PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE,
    ZOOM_IMAGE_START, ZOOM_IMAGE_FAIL, ZOOM_IMAGE,
    PLOT_PROGRESS_UPDATE, UPDATE_VIEW_SIZE, PROCESS_SCROLL,
    CHANGE_PLOT_ATTRIBUTE,
    ANY_REPLOT
};


//============ EXPORTS ===========
//============ EXPORTS ===========





//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 * Move the scroll point on this plotId and possible others if it is grouped.
 *
 * @param {string} plotId
 * @param scrollScreenPt a new point to scroll to in screen coordinates
 */
function dispatchProcessScroll(plotId,scrollScreenPt) {
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
function dispatchUpdateViewSize(plotId,width,height,updateScroll=true,centerImagePt=null) {
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
function dispatchPlotImage(plotId,wpRequest, removeOldPlot= true, addToHistory=false, useContextModifications= true ) {
    if (plotId) wpRequest.setPlotId(plotId);
    var payload= initPlotImagePayload(plotId,wpRequest,false, removeOldPlot,addToHistory,useContextModifications);
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

    var req= redReq ||  blueReq ||  greenReq;
    var payload= initPlotImagePayload(plotId,req,false, removeOldPlot,addToHistory,useContextModifications);
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
 * @param {string} plotId
 * @param {UserZoomTypes} zoomType
 */
function dispatchZoom(plotId,zoomType ) { ZoomUtil.dispatchZoom(plotId, zoomType); }

/**
 * Set the plotId of the active plot view
 * @param {string} plotId
 */
function dispatchChangeActivePlotView(plotId) {
    if (!PlotViewUtil.isActivePlotView(plotId)) {
        flux.process({ type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId} });
    }
}

function dispatchAttributeChange(plotId,applyToGroup,attKey,attValue) {
    flux.process({ type: CHANGE_PLOT_ATTRIBUTE, payload: {plotId,attKey,attValue,applyToGroup} });
}


//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================

function plotImageActionCreator(rawAction) {
    return PlotImageTask.makePlotImageAction(rawAction);
}

function zoomActionCreator(rawAction) {
    return ZoomUtil.makeZoomAction(rawAction);
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
            retState= HandlePlotCreation.reducer(state,action);
            break;
        case ZOOM_IMAGE_START  :
        case ZOOM_IMAGE_FAIL  :
        case ZOOM_IMAGE  :
        case PLOT_PROGRESS_UPDATE  :
        case UPDATE_VIEW_SIZE :
        case PROCESS_SCROLL  :
        case CHANGE_PLOT_ATTRIBUTE:
            retState= HandlePlotChange.reducer(state,action);
            break;
        case CHANGE_ACTIVE_PLOT_VIEW:
            retState= changeActivePlotView(state,action);
            break;
            break;
        default:
            break;
    }
    return retState;
}


//============ private functions =================================
//============ private functions =================================
//============ private functions =================================

function changeActivePlotView(state,action) {
    if (action.payload.plotId===state.activePlotId) return state;

    return Object.assign({}, state, {activePlotId:action.payload.plotId});
}




//todo
//todo
//todo
//function updateHistory(plotHistoryRequest, action) {
//
//    var {addToHistory}= action;
//    if (addToHistory) {
//        var request= pv.primaryPlot.plotState.getPrimaryWebPlotRequest();
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

    var plotGroupId= req.getPlotGroupId();
    var groupLocked= req.isGroupLocked();

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
    var allPlots= ffgwt.Visualize.AllPlots.getInstance();
    allPlots.addListener({
        eventNotify(ev) {
            //console.log('ANY_CHANGE:' + ev.getName().getName());
            if (ev.getName().getName()==='Replot') {
                flux.process({type: ANY_CHANGE, payload: { } });
            }
        }
    });
}


