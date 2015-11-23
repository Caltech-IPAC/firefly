


import Enum from 'enum';
import {flux} from '../Firefly.js';
import PlotImageTask from './PlotImageTask.js';
import PlotView from './PlotView.js';
import PlotViewUtil from './PlotViewUtil.js';
import PlotGroup from './PlotGroup.js';
import WebPlot from './WebPlot.js';
import ZoomUtil from './ZoomUtil.js';


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
    ANY_CHANGE, IMAGE_PLOT_KEY,
    PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE,
    ZOOM_IMAGE_START, ZOOM_IMAGE_FAIL, ZOOM_IMAGE,
    PLOT_PROGRESS_UPDATE, UPDATE_VIEW_SIZE, PROCESS_SCROLL
};


//============ EXPORTS ===========
//============ EXPORTS ===========





//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 * Move the scroll point on this plotId and possible others if it is grouped.
 *
 * @param plotId
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
 * @param plotId
 * @param {UserZoomTypes} zoomType
 */
function dispatchZoom(plotId,zoomType ) { ZoomUtil.dispatchZoom(plotId, zoomType); }



//======================================== End Dispatch Functions =============================

//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================

function plotImageActionCreator(rawAction) {
    return PlotImageTask.makePlotImageAction(rawAction);
}

function zoomActionCreator(rawAction) {
    return ZoomUtil.makeZoomAction(rawAction);
}


//======================================== End Action Creators =============================



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
            retState= processPlotCreation(state,action);
            break;
        case ZOOM_IMAGE_START  :
        case ZOOM_IMAGE_FAIL  :
        case ZOOM_IMAGE  :
            retState= processPlotChange(state,action);
            break;
        case PLOT_PROGRESS_UPDATE  :
            break;
        case UPDATE_VIEW_SIZE :
            retState= updateViewSize(state,action);
            break;
        case PROCESS_SCROLL  :
            retState= processScroll(state,action);
            break;
        default:
            break;
    }
    return retState;
}


//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================



function processPlotCreation(state, action) {

    var retState= state;
    var plotViewAry;
    var plotGroupAry;
    var plotRequestDefaults;
    switch (action.type) {
        case PLOT_IMAGE_START  :
            plotRequestDefaults= updateDefaults(state.plotRequestDefaults,action);
            plotGroupAry= confirmPlotGroup(state.plotGroupAry,action);
            plotViewAry= confirmPlotView(state.plotViewAry,action);
            break;
        case PLOT_IMAGE_FAIL  :
            break;
        case PLOT_IMAGE  :
            plotViewAry= addPlot(state.plotViewAry,action);
            // todo: also process adding to history
            break;
        default:
            break;
    }
    if (plotGroupAry || plotViewAry || plotRequestDefaults) {
        retState= Object.assign({},state);
        if (plotViewAry) retState.plotViewAry= plotViewAry;
        if (plotGroupAry) retState.plotGroupAry= plotGroupAry;
        if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
    }
    return retState;
}


function processPlotChange(state, action) {

    var retState= state;
    var plotViewAry;
    var plotGroupAry;
    var plotRequestDefaults;
    switch (action.type) {
        case ZOOM_IMAGE_START  :
            plotViewAry= scaleImage(state.plotViewAry, action);
            break;
        case ZOOM_IMAGE_FAIL  :
            break;
        case ZOOM_IMAGE  :
            plotViewAry= installZoomTiles(state.plotViewAry,action);
            // todo: also process adding to history
            break;
        default:
            break;
    }
    if (plotViewAry) {
        retState= Object.assign({},state, {plotViewAry});
    }
    return retState;
}




function scaleImage(plotViewAry, action) {
    const {plotId, zoomLevel}= action.payload;
    var pv=PlotViewUtil.findPlotView(plotId,plotViewAry);
    var plot= pv ? pv.primaryPlot : null;
    if (!plot) return plotViewAry;

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= PlotView.replacePrimary(pv,WebPlot.setZoomFactor(plot,zoomLevel));
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv) => {
        var p= WebPlot.setZoomFactor(oPv.plot,zoomLevel);
        return Object.assign({},oPv, {plot:p});
    });
    return PlotView.replacePlotView(plotViewAry,pv);
}

function installZoomTiles(plotViewAry, action) {
    const {plotId, primaryStateJson,primaryTiles,overlayStateJsonAry,overlayTilesAry }= action.payload;
    var pv=PlotViewUtil.findPlotView(plotId,plotViewAry);
    var plot= pv ? pv.primaryPlot : null;
    if (!plot) return plotViewAry;

    var centerImagePt= PlotView.findCurrentCenterPoint(pv,pv.scrollX,pv.scrollY);
    pv= PlotView.replacePrimary(pv,WebPlot.setPlotState(plot,primaryStateJson,primaryTiles));
    pv.overlayPlotViews= pv.overlayPlotViews.map( (oPv,idx) => {
        var p= WebPlot.setPlotState(oPv.plot,overlayStateJsonAry[idx],overlayTilesAry[idx]);
        return Object.assign({},oPv, {plot:p})
    });
    pv= PlotView.updatePlotViewScrollXY(pv, PlotView.findScrollPtForImagePt(pv,centerImagePt));
    return PlotView.replacePlotView(plotViewAry,pv);
}


const updateDefaults= function(plotRequestDefaults, action) {
    var retDef;
    var {plotId,wpRequest,redReq,greenReq, blueReq,threeColor}= action.payload;
    if (threeColor) {
        retDef= Object.assign({}, plotRequestDefaults, {[plotId]:{threeColor,redReq,greenReq, blueReq}});
    }
    else {
        retDef= Object.assign({}, plotRequestDefaults, {[plotId]:{threeColor,wpRequest}});
    }
    return retDef;
};

const addPlot= function(plotViewAry,action) {

    const {plotId, plotAry}= action.payload;
    var newPlotViewAry= plotViewAry.map( (pv) => {
        return pv.plotId===plotId ? PlotView.replacePlots(pv,plotAry) : pv;
    });
    return newPlotViewAry;

};


//todo
//todo
//todo
function updateHistory(plotHistoryRequest, action) {

    var {addToHistory}= action;
    if (addToHistory) {
        var request= pv.primaryPlot.plotState.getPrimaryWebPlotRequest();
        //todo: add to history here -- need to figure out how
    }
}





//============ private functions =================================
//============ private functions =================================
//============ private functions =================================

/**
/**
 *
 * @param plotViewAry
 * @param action
 * @return {[]|null} new PlotViewAry or null it nothing is created.
 */
function confirmPlotView(plotViewAry,action) {
    const {plotId}= action.payload;
    if (pvExist(plotId,plotViewAry)) return null;

    const payload= action.payload;
    var rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => payload[key] ? true : false);
    var pv= PlotView.makePlotView(plotId, payload[rKey] );
    return [...plotViewAry,pv];
}

/**
 *
 * @param plotGroupAry
 * @param action
 * @return {[]|null} new PlotGroupAry or null if nothing is created.
 */
function confirmPlotGroup(plotGroupAry,action) {
    const {plotGroupId,groupLocked}= action.payload;
    if (plotGroupExist(plotGroupId,plotGroupAry)) return null;
    var plotGroup= PlotGroup.makePlotGroup(plotGroupId, groupLocked);
    return [...plotGroupAry,plotGroup];
}


function pvExist(plotId, plotViewAry) {
    return (plotViewAry.some( (pv) => pv.plotId===plotId ));
}

function plotGroupExist(plotGroupId, plotGroupAry) {
    return (plotGroupAry.some( (pg) => pg.plotGroupId===plotGroupId ));
}


function processScroll(state,action) {
    const {plotId,scrollScreenPt}= action.payload;
    var plotViewAry= PlotView.updatePlotGroupScrollXY(plotId,state.plotViewAry,state.plotGroupAry,scrollScreenPt);
    return Object.assign({},state,{plotViewAry});
}

function updateViewSize(state,action) {
    const {plotId,width,height,updateScroll,centerImagePt}= action.payload;
    var plotViewAry= state.plotViewAry.map( (pv) => {
        return pv.plotId===plotId ? PlotView.updateViewDim(pv,{width, height}) : pv;
    });


    return Object.assign({},state,{plotViewAry});
}


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


