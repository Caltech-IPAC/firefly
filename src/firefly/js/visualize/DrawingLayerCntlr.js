/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */





import {flux} from '../Firefly.js';
import PlotViewUtil from './PlotViewUtil.js';
import VisMouseCntlr from './VisMouseCntlr.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';
import _ from 'lodash';




const RETRIEVE_DATA= 'DrawingLayerCntlr.retrieveData';
const CREATE_DRAWING_LAYER= 'DrawingLayerCntlr.addDrawingLayer';
const DESTROY_DRAWING_LAYER= 'DrawingLayerCntlr.addDrawingLayer';
const CHANGE_VISIBILITY= 'DrawingLayerCntlr.changeVisibility';
const ATTACH_LAYER_TO_PLOT= 'DrawingLayerCntlr.attachLayerToPlot';
const DETACH_LAYER_FROM_PLOT= 'DrawingLayerCntlr.detachLayerFromPlot';


// _- select
const SELECT_AREA_START= 'DrawingLayerCntlr.SelectArea.selectAreaStart';
const SELECT_AREA_MOVE= 'DrawingLayerCntlr.SelectArea.selectAreaMove';
const SELECT_AREA_END= 'DrawingLayerCntlr.SelectArea.selectAreaEnd';
const SELECT_MOUSE_LOC= 'DrawingLayerCntlr.SelectArea.selectMouseLoc';





const DRAWING_LAYER_KEY= 'drawingLayers';



export default {
    CHANGE_VISIBILITY, RETRIEVE_DATA, DRAWING_LAYER_KEY,
    ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT,
    CREATE_DRAWING_LAYER,DESTROY_DRAWING_LAYER,
    SELECT_AREA_START, SELECT_AREA_MOVE, SELECT_AREA_END, SELECT_MOUSE_LOC,
    reducer, dispatchRetrieveData, dispatchChangeVisibility,
    dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchAttachLayerToPlot, dispatchDetachLayerFromPlot,

};


function dispatchRetrieveData(drawLayerId) {
    flux.process({type: RETRIEVE_DATA ,
        payload: {drawLayerId}
    });

}

function dispatchChangeVisibility(drawLayerId,visible, plotId) {
    flux.process({type: CHANGE_VISIBILITY,
        payload: {drawLayerId, visible, plotId}
    });
}

/**
 *
 * @param drawLayerId the id of this drawing layer
 * @param drawLayerReducer reducer for this drawing layer
 */
function dispatchCreateDrawLayer(drawLayerId,drawLayerReducer) {
    flux.process({type: CREATE_DRAWING_LAYER,
        payload: {drawLayerId, drawLayerReducer}
    });
}

function dispatchDestroyDrawLayer(drawLayerId) {
    flux.process({type: DESTROY_DRAWING_LAYER,
        payload: {drawLayerId}
    });
}

/**
 *
 * @param drawLayerId
 * @param plotIdAry
 */
function dispatchAttachLayerToPlot(drawLayerId,plotIdAry) {
    flux.process({type: ATTACH_LAYER_TO_PLOT,
                  payload: {drawLayerId,plotIdAry}
    });
}


/**
 *
 * @param drawLayerId
 * @param plotIdAry
 */
function dispatchDetachLayerFromPlot(drawLayerId,plotIdAry) {
    flux.process({type: DETACH_LAYER_FROM_PLOT,
                  payload: {drawLayerId,plotIdAry}
    });
}


/**
 *
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {{allowedActions, dlContainerAry, mouseGrabDrawLayerId}}
 */
function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;
    if (!state.allowedActions.includes(action.type)) return state;

    var retState= state;
    switch (action.type) {
        case CHANGE_VISIBILITY:
            retState= deferToLayerReducer(state,action);
            break;
        case CREATE_DRAWING_LAYER:
            retState= createDrawingLayer(state,action);
            break;
        case DESTROY_DRAWING_LAYER:
            retState= destroyDrawingLayer(state,action);
            break;
        case ATTACH_LAYER_TO_PLOT:
            retState= deferToLayerReducer(state,action);
            break;
        case DETACH_LAYER_FROM_PLOT:
            retState= deferToLayerReducer(state,action);
            break;
        case ImagePlotCntlr.ANY_REPLOT:
            retState= determineAndCallLayerReducer(state,action,true);
            break;
        case VisMouseCntlr.MOUSE_STATE_CHANGE:
            retState= mouseStateChange(state,action);
            break;
        case RETRIEVE_DATA:
            // todo: for async data:
            // todo: get the data in action creator, update the retrieved data here
            // todo: the action creator will have to defer to the layer somehow
            break;
        default:
            retState= determineAndCallLayerReducer(state,action);
            break;
    }
    return retState;
}


/**
 * Destroy a drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function createDrawingLayer(state,action) {
    var {drawLayerId,drawLayerReducer}= action.payload;
    var drawLayer= drawLayerReducer(null,action);
    var container= makeDrawLayerContainer(drawLayerId,drawLayerReducer,drawLayer);


    var allowedActions= _.union(state.allowedActions, drawLayer.actionTypeAry);

    return Object.assign({}, state,
        {allowedActions, dlContainerAry: [container, ...state.dlContainerAry] });
}

/**
 * Destroy the drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function destroyDrawingLayer(state,action) {
    var {drawLayerId}= action.payload;
    return Object.assign({}, state,
        {dlContainerAry: state.dlContainerAry.filter( (c) => c.drawLayerId!==drawLayerId) });
}

/**
 * Call the reducer for the drawing layer defined by the action
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function deferToLayerReducer(state,action) {
    var {drawLayerId}= action.payload;

    var container= state.dlContainerAry.find( (c) => drawLayerId===c.drawingLayer.drawLayerId);
    if (container) {
        var drawingLayer= container.drawLayerReducer(container.drawingLayer,action);
        if (drawingLayer!==container.drawingLayer) {
            container= Object.assign({},container, {drawingLayer});
            return Object.assign({}, state,
                {dlContainerAry: state.dlContainerAry.map( (c) => c.drawLayerId===drawLayerId ? container : c) });
        }
    }
    return state;
}


/**
 * Call all the drawing layers that are interested in the action.  Since this function will be called often it does
 *  a lot of checking for change. * If nothing has changed it returns the original state.
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 */
function determineAndCallLayerReducer(state,action,force) {
    var newAry= state.dlContainerAry.map( (dlContainer) => {
        var {drawLayerReducer,drawingLayer}= dlContainer;
        if (force || (drawingLayer.actionTypeAry && drawingLayer.actionTypeAry.includes(action.type))) {
            var newdl= drawLayerReducer(drawingLayer,action);
            if (newdl===drawingLayer) return dlContainer;  // check to see if there was a change
            else return Object.assign({}, dlContainer, {drawingLayer:newdl});
        }
        else {
            return dlContainer;
        }
    } );

    if (_.without(state.dlContainerAry,newAry).length) {  // if there are changes
        return Object.assign({},state, {dlContainerAry:newAry});
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
        dlContainerAry : [],
        mouseGrabDrawLayerId : null

    };

};

function makeDrawLayerContainer(drawLayerId, drawLayerReducer, drawingLayer) {
    return {drawLayerId, drawLayerReducer, drawingLayer};
}


