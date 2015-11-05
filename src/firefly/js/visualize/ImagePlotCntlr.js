


import Enum from 'enum';
import {flux} from '../Firefly.js';
import PlotImageTask from './PlotImageTask.js';
import PlotView from './PlotView.js';


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
    reducer, plotImageActionCreator,
    dispatchUpdateViewSize, dispatchProcessScroll, dispatchPlotImage,
    ANY_CHANGE, IMAGE_PLOT_KEY, PLOT_IMAGE_START, PLOT_IMAGE_FAIL, PLOT_IMAGE,
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

    if (!plotId) plotId= wpRequest.getPlotId();
    else wpRequest.setPlotId(plotId);


    if (plotId) {
        flux.process({
            type: PLOT_IMAGE,
            payload: {plotId, wpRequest, removeOldPlot, addToHistory, useContextModifications, threeColor: false}
        });
    }
    else {
        var error= Error('plotId is required');
        flux.process({ type: PLOT_IMAGE_FAIL, payload: {plotId, error} });
    }
}

//======================================== End Dispatch Functions =============================

//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================

function plotImageActionCreator(rawAction) {
    return PlotImageTask.makePlotImageAction(rawAction);
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
            retState= processPlotView(state,action);
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



function processPlotView(state, action) {

    var retState= state;
    var plotViewAry;
    var plotRequestDefaults;
    switch (action.type) {
        case PLOT_IMAGE_START  :
            plotRequestDefaults= updateDefaults(state.plotRequestDefaults,action);
            plotViewAry= makePlotView(state.plotViewAry,action);
            break;
        case PLOT_IMAGE_FAIL  :
            break;
        case PLOT_IMAGE  :
            plotViewAry= addPlot(state.plotViewAry,action);
            // todo: also process adding to history
            break;
        case PLOT_PROGRESS_UPDATE  :
            break;
        default:
            break;
    }
    if (plotViewAry || plotRequestDefaults) {
        retState= Object.assign({},state);
        if (plotViewAry) retState.plotViewAry= plotViewAry;
        if (plotRequestDefaults) retState.plotRequestDefaults= plotRequestDefaults;
    }
    return retState;
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
 *
 * @param plotViewAry
 * @param action
 * @return null if the plotview already exist, other return the a new plotViewAry with the new PlotView
 */
function makePlotView(plotViewAry,action) {
    const {plotId}= action.payload;
    if (pvExist(plotId,plotViewAry)) return null;

    const payload= action.payload;
    var rKey= ['wpRequest','redReq','blueReq','greenReq'].find( (key) => payload[key] ? true : false);
    var pv= PlotView.makePlotView(plotId, payload[rKey] );
    var newPlotViewAry= plotViewAry.slice();
    newPlotViewAry.push(pv);
    return newPlotViewAry;
}


function pvExist(plotId, plotViewAry) {
    return (plotViewAry.some( (pv) => pv.plotId===plotId ));
}


function processScroll(state,action) {
    const {plotId,scrollScreenPt}= action.payload;
    var plotViewAry= state.plotViewAry.map( (pv) => {
        return pv.plotId===plotId ? PlotView.updateScrollXY(pv,scrollScreenPt) : pv;
    });
    return Object.assign({},state,{plotViewAry});
}

function updateViewSize(state,action) {
    const {plotId,width,height,updateScroll,centerImagePt}= action.payload;
    var plotViewAry= state.plotViewAry.map( (pv) => {
        return pv.plotId===plotId ? PlotView.updateViewDim(pv,{width, height}) : pv;
    });


    return Object.assign({},state,{plotViewAry});
}


/**
 *
 * @param plotGroupId
 * @return {{plotGroupId: *, lockRelated: boolean, enableSelecting: boolean, allSelected: boolean}}
 */
function makePlotGroup(plotGroupId) {
    return {
        plotGroupId,
        lockRelated  : false,
        enableSelecting :false,    //todo
        allSelected :false    //todo
    };
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


