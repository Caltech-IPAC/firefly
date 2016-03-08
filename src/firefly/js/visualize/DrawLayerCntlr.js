/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {getPlotViewIdListInGroup, getDrawLayerById} from './PlotViewUtil.js';
import VisMouseCntlr from './VisMouseCntlr.js';
import ImagePlotCntlr, {visRoot}  from './ImagePlotCntlr.js';
import DrawLayerReducer from './reducer/DrawLayerReducer.js';
import without from 'lodash/without';
import union from 'lodash/union';




const RETRIEVE_DATA= 'DrawLayerCntlr.retrieveData';
const CREATE_DRAWING_LAYER= 'DrawLayerCntlr.createDrawLayer';
const DESTROY_DRAWING_LAYER= 'DrawLayerCntlr.destroyDrawLayer';
const CHANGE_VISIBILITY= 'DrawLayerCntlr.changeVisibility';
const CHANGE_DRAWING_DEF= 'DrawLayerCntlr.changeDrawingDef';
const ATTACH_LAYER_TO_PLOT= 'DrawLayerCntlr.attachLayerToPlot';
const DETACH_LAYER_FROM_PLOT= 'DrawLayerCntlr.detachLayerFromPlot';
const MODIFY_CUSTOM_FIELD= 'DrawLayerCntlr.modifyCustomField';
const FORCE_DRAW_LAYER_UPDATE= 'DrawLayerCntlr.forceDrawLayerUpdate';

// _- select
const SELECT_AREA_START= 'DrawLayerCntlr.SelectArea.selectAreaStart';
const SELECT_AREA_MOVE= 'DrawLayerCntlr.SelectArea.selectAreaMove';
const SELECT_AREA_END= 'DrawLayerCntlr.SelectArea.selectAreaEnd';
const SELECT_MOUSE_LOC= 'DrawLayerCntlr.SelectArea.selectMouseLoc';

const SELECT_POINT=  'DrawLayerCntlr.SelectPoint.selectPoint';


// _- Distance tool
const DT_START= 'DrawLayerCntlr.DistanceTool.distanceToolStart';
const DT_MOVE= 'DrawLayerCntlr.DistanceTool.distanceToolMove';
const DT_END= 'DrawLayerCntlr.DistanceTool.distanceToolEnd';



export const DRAWING_LAYER_KEY= 'drawLayers';





export function dlRoot() { return flux.getState()[DRAWING_LAYER_KEY]; }

/**
 * Return, from the store, the master array of all the drawing layers on all the plots
 * @return {Array<Object>}
 */
export function getDlAry() { return flux.getState()[DRAWING_LAYER_KEY].drawLayerAry; }


export default {
    CHANGE_VISIBILITY, RETRIEVE_DATA,
    ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT,CHANGE_DRAWING_DEF,
    CREATE_DRAWING_LAYER,DESTROY_DRAWING_LAYER, MODIFY_CUSTOM_FIELD,
    SELECT_AREA_START, SELECT_AREA_MOVE, SELECT_AREA_END, SELECT_MOUSE_LOC,
    SELECT_POINT,
    FORCE_DRAW_LAYER_UPDATE,
    DT_START, DT_MOVE, DT_END,
    makeReducer, dispatchRetrieveData, dispatchChangeVisibility,
    dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchAttachLayerToPlot, dispatchDetachLayerFromPlot
};

/**
 *
 * @param drawLayerId
 */
export function dispatchRetrieveData(drawLayerId) {
    flux.process({type: RETRIEVE_DATA , payload: {drawLayerId} });

}


/**
 *
 * @param drawLayerTypeId
 * @param params
 */
export function dispatchCreateDrawLayer(drawLayerTypeId, params={}) {
    var drawLayer= flux.createDrawLayer(drawLayerTypeId,params);
    flux.process({type: CREATE_DRAWING_LAYER, payload: {drawLayer}} );
}



/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param visible
 * @param plotId
 * @param useGroup
 */
export function dispatchChangeVisibility(id,visible, plotId, useGroup= true) {
    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: CHANGE_VISIBILITY, payload: {drawLayerId, visible, plotIdAry} });
        });
}

/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param drawingDef
 * @param plotId
 * @param useGroup
 */
export function dispatchChangeDrawingDef(id,drawingDef, plotId, useGroup= true) {
    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: CHANGE_DRAWING_DEF, payload: {drawLayerId, drawingDef, plotIdAry}});
        });
}


/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param changes
 * @param plotId
 * @param useGroup
 */
export function dispatchModifyCustomField(id,changes, plotId, useGroup= true) {

    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: MODIFY_CUSTOM_FIELD, payload: {drawLayerId, changes, plotIdAry}});
        });
}

export function dispatchForceDrawLayerUpdate(id,plotId, useGroup= true) {

    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: FORCE_DRAW_LAYER_UPDATE, payload: {drawLayerId, plotIdAry}});
        });
}



/**
 *
 * @param {string} id make the drawLayerId or drawLayerTypeId
 */
export function dispatchDestroyDrawLayer(id) {
    var drawLayerId= getDrawLayerId(dlRoot(),id);
    if (drawLayerId) {
        flux.process({type: DESTROY_DRAWING_LAYER, payload: {drawLayerId} });
    }
}

/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|[]} plotId to attach this may by a string or an array of strings
 * @param attachPlotGroup
 */
export function dispatchAttachLayerToPlot(id,plotId, attachPlotGroup=false) {
    var plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry= attachPlotGroup ? getPlotViewIdListInGroup(visRoot(), plotId) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(),id,false)
        .forEach( (drawLayerId) => {
            flux.process({type: ATTACH_LAYER_TO_PLOT, payload: {drawLayerId,plotIdAry} });
        });
}


/**
 *
 * @param {string|[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|[]} plotId to attach this may by a string or an array of strings
 * @param detachPlotGroup
 * @param useLayerGroup
 */
export function dispatchDetachLayerFromPlot(id,plotId, detachPlotGroup=false, useLayerGroup=true) {
    var plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry= detachPlotGroup ? getPlotViewIdListInGroup(visRoot(), plotId) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(),id,useLayerGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: DETACH_LAYER_FROM_PLOT, payload: {drawLayerId,plotIdAry} });
        });

}



function getDrawLayerId(dlRoot,id) {
    var drawLayer= dlRoot.drawLayerAry.find( (dl) => id===dl.drawLayerId);
    if (!drawLayer) {
        drawLayer= dlRoot.drawLayerAry.find( (dl) => id===dl.drawLayerTypeId);
    }
    return drawLayer ? drawLayer.drawLayerId : null;
}

//function getDrawLayerIdAry(dlRoot,id,useGroup) {
//    return dlRoot.drawLayerAry
//            .filter( (dl) => id===dl.drawLayerId || id===dl.drawLayerTypeId || (useGroup && id===dl.drawLayerGroupId)
//            .map(  (dl) => dl.drawLayerId);
//}

function getDrawLayerIdAry(dlRoot,id,useGroup) {
    const idAry= Array.isArray(id) ? id: [id];
    return dlRoot.drawLayerAry
        .filter( (dl) => idAry
            .filter( (id) => id===dl.drawLayerId || id===dl.drawLayerTypeId || (useGroup && id===dl.drawLayerGroupId))
            .length>0)
        .map(  (dl) => dl.drawLayerId);
}


//=============================================
//=============================================
//=============================================

export function makeDetachLayerActionCreator(factory) {
    return (action) => {
        return (dispatcher) => {
            var {drawLayerId}= action.payload;
            var drawLayer= getDrawLayerById(getDlAry(), drawLayerId);
            factory.onDetachAction(drawLayer,action);
            dispatcher(action);
        };
    };
}




//=============================================
//=============================================
//=============================================
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
            case CHANGE_DRAWING_DEF:
            case FORCE_DRAW_LAYER_UPDATE:
            case MODIFY_CUSTOM_FIELD:
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
    var allowedActions= union(state.allowedActions, drawLayer.actionTypeAry);

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
 *  a lot of checking for change.
 *  If nothing has changed it returns the original state.
 * @param state
 * @param {{type:string,payload:object}} action
 * @param dlReducer drawinglayer subreducer
 * @param force
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

    if (without(state.drawLayerAry,...newAry).length) {  // if there are changes
        return Object.assign({},state, {drawLayerAry:newAry});
    }
    else {
       return state;
    }
}






//function mouseStateChange(state,action) {
//    var {drawLayerId, plotId}= action.payload;
//
//
//}

const initState= function() {

    return {
        allowedActions: [ RETRIEVE_DATA, CREATE_DRAWING_LAYER, DESTROY_DRAWING_LAYER, CHANGE_VISIBILITY,
                          ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT, MODIFY_CUSTOM_FIELD,
                          CHANGE_DRAWING_DEF,FORCE_DRAW_LAYER_UPDATE,
                          ImagePlotCntlr.ANY_REPLOT
                        ],
        drawLayerAry : [],
        mouseGrabDrawLayerId : null

    };

};



