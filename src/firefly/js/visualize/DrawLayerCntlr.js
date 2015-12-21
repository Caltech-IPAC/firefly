/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import PlotViewUtil from './PlotViewUtil.js';
import VisMouseCntlr from './VisMouseCntlr.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';
import DrawLayerReducer from './reducer/DrawLayerReducer.js';
import DrawLayerFactory from './draw/DrawLayerFactory.js';
import _ from 'lodash';




const RETRIEVE_DATA= 'DrawLayerCntlr.retrieveData';
const CREATE_DRAWING_LAYER= 'DrawLayerCntlr.createDrawLayer';
const DESTROY_DRAWING_LAYER= 'DrawLayerCntlr.destroyDrawLayer';
const CHANGE_VISIBILITY= 'DrawLayerCntlr.changeVisibility';
const ATTACH_LAYER_TO_PLOT= 'DrawLayerCntlr.attachLayerToPlot';
const DETACH_LAYER_FROM_PLOT= 'DrawLayerCntlr.detachLayerFromPlot';


// _- select
const SELECT_AREA_START= 'DrawLayerCntlr.SelectArea.selectAreaStart';
const SELECT_AREA_MOVE= 'DrawLayerCntlr.SelectArea.selectAreaMove';
const SELECT_AREA_END= 'DrawLayerCntlr.SelectArea.selectAreaEnd';
const SELECT_MOUSE_LOC= 'DrawLayerCntlr.SelectArea.selectMouseLoc';


// _- Distance tool
const DT_START= 'DrawLayerCntlr.DistanceTool.distanceToolStart';
const DT_MOVE= 'DrawLayerCntlr.DistanceTool.distanceToolMove';
const DT_END= 'DrawLayerCntlr.DistanceTool.distanceToolEnd';



const DRAWING_LAYER_KEY= 'drawLayers';







export default {
    CHANGE_VISIBILITY, RETRIEVE_DATA, DRAWING_LAYER_KEY,
    ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT,
    CREATE_DRAWING_LAYER,DESTROY_DRAWING_LAYER,
    SELECT_AREA_START, SELECT_AREA_MOVE, SELECT_AREA_END, SELECT_MOUSE_LOC,
    DT_START, DT_MOVE, DT_END,
    makeReducer, dispatchRetrieveData, dispatchChangeVisibility,
    dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchAttachLayerToPlot, dispatchDetachLayerFromPlot,

};


function dispatchRetrieveData(drawLayerId) {
    flux.process({type: RETRIEVE_DATA , payload: {drawLayerId} });

}

function dispatchChangeVisibility(drawLayerId,visible, plotId) {
    flux.process({type: CHANGE_VISIBILITY, payload: {drawLayerId, visible, plotId} });
}

/**
 *
 * @param drawLayerTypeId
 * @param params
 */
function dispatchCreateDrawLayer(drawLayerTypeId, params={}) {
    var drawLayer= flux.createDrawLayer(drawLayerTypeId,params);
    flux.process({type: CREATE_DRAWING_LAYER, payload: {drawLayer}} );
}

/**
 *
 * @param {string} id make the drawLayerId or drawLayerTypeId
 */
function dispatchDestroyDrawLayer(id) {
    var drawLayerId= getDrawLayerId(id);
    if (drawLayerId) {
        flux.process({type: DESTROY_DRAWING_LAYER, payload: {drawLayerId} });
    }
}

/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|[]} plotId to attach this may by a string or an array of strings
 */
function dispatchAttachLayerToPlot(id,plotId) {
    const idAry= Array.isArray(id) ? id : [id];
    const plotIdAry= Array.isArray(plotId) ? plotId : [plotId];

    idAry.forEach( (idItem) => {
        const drawLayerId= getDrawLayerId(idItem);
        if (drawLayerId) {
            flux.process({type: ATTACH_LAYER_TO_PLOT, payload: {drawLayerId,plotIdAry} });
        }
    });
}


/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|[]} plotId to attach this may by a string or an array of strings
 */
function dispatchDetachLayerFromPlot(id,plotId) {
    const idAry= Array.isArray(id) ? id : [id];
    const plotIdAry= Array.isArray(plotId) ? plotId : [plotId];
    idAry.forEach( (idItem) => {
        const drawLayerId= getDrawLayerId(idItem);
        if (drawLayerId) {
            flux.process({type: DETACH_LAYER_FROM_PLOT, payload: {drawLayerId,plotIdAry} });
        }
    });
}

function getDrawLayerId(id) {
    var drawLayerAry= PlotViewUtil.getAllDrawLayersStore();
    var drawLayer= drawLayerAry.find( (dl) => id===dl.drawLayerId);
    if (!drawLayer) {
        drawLayer= drawLayerAry.find( (dl) => id===dl.drawLayerTypeId);
    }
    return drawLayer ? drawLayer.drawLayerId : null;
}



/**
 *
 * @param factory
 */
function makeReducer(factory) {
    const dlReducer= DrawLayerReducer.makeReducer(factory);
    return (state=initState(), action={}) => {
        if (!action.payload || !action.type) return state;
        if (!state.allowedActions.includes(action.type)) return state;

        var retState = state;
        switch (action.type) {
            case CHANGE_VISIBILITY:
                retState = deferToLayerReducer(state, action, dlReducer);
                break;
            case CREATE_DRAWING_LAYER:
                retState = createDrawLayer(state, action);
                break;
            case DESTROY_DRAWING_LAYER:
                retState = destroyDrawLayer(state, action);
                break;
            case ATTACH_LAYER_TO_PLOT:
                retState = deferToLayerReducer(state, action, dlReducer);
                break;
            case DETACH_LAYER_FROM_PLOT:
                retState = deferToLayerReducer(state, action, dlReducer);
                break;
            case ImagePlotCntlr.ANY_REPLOT:
                retState = determineAndCallLayerReducer(state, action, dlReducer, true);
                break;
            case VisMouseCntlr.MOUSE_STATE_CHANGE:
                retState= state;
                //retState = mouseStateChange(state, action);
                break;
            case RETRIEVE_DATA:
                // todo: for async data:
                // todo: get the data in action creator, update the retrieved data here
                // todo: the action creator will have to defer to the layer somehow
                break;
            default:
                retState = determineAndCallLayerReducer(state, action, dlReducer);
                break;
        }
        return retState;
    };
}


/**
 * Create a drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function createDrawLayer(state,action) {
    var {drawLayer}= action.payload;
    var allowedActions= _.union(state.allowedActions, drawLayer.actionTypeAry);

    return Object.assign({}, state,
        {allowedActions, drawLayerAry: [drawLayer, ...state.drawLayerAry] });
}

/**
 * Destroy the drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function destroyDrawLayer(state,action) {
    var {drawLayerId}= action.payload;
    return Object.assign({}, state,
        {drawLayerAry: state.drawLayerAry.filter( (c) => c.drawLayerId!==drawLayerId) });
}

/**
 * Call the reducer for the drawing layer defined by the action
 * @param state
 * @param {{type:string,payload:object}} action
 * @param dlReducer drawinglayer subreducer
 * @return {object} the new state;
 */
function deferToLayerReducer(state,action,dlReducer) {
    var {drawLayerId}= action.payload;
    var drawLayer= state.drawLayerAry.find( (dl) => drawLayerId===dl.drawLayerId);

    if (drawLayer) {
        var newDl= dlReducer(drawLayer,action);
        if (newDl!==drawLayer) {
            return Object.assign({}, state,
                {drawLayerAry: state.drawLayerAry.map( (dl) => dl.drawLayerId===drawLayerId ? newDl : dl) });
        }
    }
    return state;
}


/**
 * Call all the drawing layers that are interested in the action.  Since this function will be called often it does
 *  a lot of checking for change. * If nothing has changed it returns the original state.
 * @param state
 * @param {{type:string,payload:object}} action
 * @param dlReducer drawinglayer subreducer
 * @return {object} the new state;
 */
function determineAndCallLayerReducer(state,action,dlReducer,force) {
    var newAry= state.drawLayerAry.map( (dl) => {
        if (force || (dl.actionTypeAry && dl.actionTypeAry.includes(action.type))) {
            var newdl= dlReducer(dl,action);
            return (newdl===dl) ? dl : newdl;  // check to see if there was a change
        }
        else {
            return dl;
        }
    } );

    if (_.without(state.drawLayerAry,newAry).length) {  // if there are changes
        return Object.assign({},state, {drawLayerAry:newAry});
    }
    else {
       return state;
    }
}






/**
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function mouseStateChange(state,action) {
    var {drawLayerId, plotId}= action.payload;

    //todo: determine which drawing layer this should be passed to
    //todo: determine it there is a mouse grab active layer to take priority and the it is on the plotId
    //todo: call all the drawing layers that quality using newState= deferToLayerReducer()
    //todo: if non qualify then just return the state


}

const initState= function() {

    return {
        allowedActions: [ RETRIEVE_DATA, CREATE_DRAWING_LAYER, DESTROY_DRAWING_LAYER, CHANGE_VISIBILITY,
                          ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT, ImagePlotCntlr.ANY_REPLOT
                        ],
        drawLayerAry : [],
        mouseGrabDrawLayerId : null

    };

};



