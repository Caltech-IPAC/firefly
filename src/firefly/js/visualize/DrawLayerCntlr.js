/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {
    extractLineToolEndActionCreator, extractLineToolStartActionCreator
} from 'firefly/drawingLayers/ExtractLineTool.js';
import {isEmpty, union, without} from 'lodash';

import {REINIT_APP} from '../core/CoreConst';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {distanceToolEndActionCreator} from '../drawingLayers/DistanceTool.js';

import {
    footprintCreateLayerActionCreator, footprintEndActionCreator, footprintMoveActionCreator,
    footprintStartActionCreator
} from '../drawingLayers/FootprintTool.js';
import {
    markerToolCreateLayerActionCreator, markerToolEndActionCreator, markerToolMoveActionCreator,
    markerToolStartActionCreator
} from '../drawingLayers/MarkerTool.js';

import {selectAreaEndActionCreator} from '../drawingLayers/SelectArea.js';
import {getConnectedPlotsIds, getDrawLayerById} from './PlotViewUtil.js';
import DrawLayerReducer from './reducer/DrawLayerReducer.js';

import {
    regionCreateLayerActionCreator, regionDeleteLayerActionCreator, regionUpdateEntryActionCreator
} from './region/RegionTask.js';
import {imageLineBasedfootprintActionCreator} from './task/LSSTFootprintTask.js';
import {
    ANY_REPLOT, ATTACH_LAYER_TO_PLOT, CHANGE_CENTER_OF_PROJECTION, CHANGE_DRAWING_DEF, CHANGE_HIPS, CHANGE_VISIBILITY,
    CREATE_DRAWING_LAYER, DELETE_PLOT_VIEW, DESTROY_DRAWING_LAYER, DETACH_LAYER_FROM_PLOT, DRAWING_LAYER_KEY,
    DRAWLAYER_PREFIX, DT_END, ELT_END, ELT_START, FOOTPRINT_CREATE, FOOTPRINT_END, FOOTPRINT_MOVE, FOOTPRINT_START,
    FORCE_DRAW_LAYER_UPDATE, IMAGELINEBASEDFP_CREATE, MARKER_CREATE, MARKER_END, MARKER_MOVE, MARKER_START,
    MODIFY_CUSTOM_FIELD, REGION_ADD_ENTRY, REGION_CREATE_LAYER, REGION_DELETE_LAYER, REGION_REMOVE_ENTRY,
    SELECT_AREA_END, UPDATE_DRAWING_LAYER
} from './VisConst';
import {getDlAry} from './VisStoreRoots';


const PRE_ATTACH_LAYER_TO_PLOT= `${DRAWLAYER_PREFIX}.attachLayerToPlot`;


export function getDrawLayerCntlrDef(drawLayerFactory) {

    setTimeout( () => {
        dispatchAddActionWatcher({
            actions:[CHANGE_VISIBILITY, CHANGE_DRAWING_DEF, ATTACH_LAYER_TO_PLOT,
                DETACH_LAYER_FROM_PLOT, FORCE_DRAW_LAYER_UPDATE, MODIFY_CUSTOM_FIELD,
                ANY_REPLOT, CHANGE_HIPS, CHANGE_CENTER_OF_PROJECTION
            ],
            callback: asyncDrawDataWatcher,
            params: {drawLayerFactory}
        });
    },10);
    
    return {
        reducers() {return {[DRAWING_LAYER_KEY]: makeReducer(drawLayerFactory)}; },

        actionCreators() {
            return {
                [DETACH_LAYER_FROM_PLOT] :  makeDetachLayerActionCreator(drawLayerFactory),
                [DESTROY_DRAWING_LAYER] :  makeDestoyLayerActionCreator(drawLayerFactory),
                [CHANGE_VISIBILITY] :  makeChangeVisibilityActionCreator(drawLayerFactory),
                [SELECT_AREA_END] :  selectAreaEndActionCreator,
                [DT_END] :  distanceToolEndActionCreator,
                [ELT_END] :  extractLineToolEndActionCreator,
                [ELT_START] :  extractLineToolStartActionCreator,
                [MARKER_START] :  markerToolStartActionCreator,
                [MARKER_MOVE] :  markerToolMoveActionCreator,
                [MARKER_END] :  markerToolEndActionCreator,
                [MARKER_CREATE] :  markerToolCreateLayerActionCreator,
                [FOOTPRINT_CREATE] :  footprintCreateLayerActionCreator,
                [FOOTPRINT_START] :  footprintStartActionCreator,
                [FOOTPRINT_END] :  footprintEndActionCreator,
                [FOOTPRINT_MOVE] :  footprintMoveActionCreator,
                [REGION_CREATE_LAYER] :  regionCreateLayerActionCreator,
                [REGION_DELETE_LAYER] :  regionDeleteLayerActionCreator,
                [REGION_ADD_ENTRY] :  regionUpdateEntryActionCreator,
                [REGION_REMOVE_ENTRY] :  regionUpdateEntryActionCreator,
                [IMAGELINEBASEDFP_CREATE] : imageLineBasedfootprintActionCreator
            };
        }
    };
}


export default {
    getDrawLayerCntlrDef,
};



/**
 * @global
 * @public
 * @typedef {Object} RegionSelectMode
 * @summary shallow object with the rendering parameters for selected region
 * @prop {string}  [selectStyle='UprightBox'] - rendering style for the selected region including 'UprightBox', 'DottedOverlay',
 * 'SolidOverlay', 'DottedReplace', and 'SolidReplace'
 * @prop {string}  [selectColor='#DAA520'] - rendering color for the selected region, CSS color values, such as '#DAA520' 'red'.
 * are valid for rendering.
 * @prop {int}     [lineWidth=0] - rendering line width for the selected region. 0 or less means the line width
 * is the same as that of the selected region
 */


/**
 * Footprint Info.  The data object containing footprint info.
 * @typedef {object} footprintInfo
 * @prop {string} footprint - name of footprint project, such as 'HST', 'Roman', etc. or footprint file at the server
 * @prop {string} instrument - name of instrument for the footprint
 * @prop {string} relocateBy - name of instrument for the footprint from the server, method of relocation for the uploaded footprint
 * @prop {string} fromFile - filename, not including the extension, of the uploaded file
 * @prop {string[]} fromRegionAry - array or string of region description
 *
 * @public
 */


//=============================================
//=============================================
//=============================================

function makeDestoyLayerActionCreator(factory) {
    return (action) => {
        return (dispatcher) => {
            const {drawLayerId}= action.payload;
            const drawLayer= getDrawLayerById(getDlAry(), drawLayerId);
            factory.onDetachAction(drawLayer,action);
            dispatcher(action);
        };
    };
}

function makeDetachLayerActionCreator(factory) {
    return (action) => {
        return (dispatcher) => {
            const {drawLayerId}= action.payload;
            const drawLayer= getDrawLayerById(getDlAry(), drawLayerId);
            factory.onDetachAction(drawLayer,action);
            dispatcher(action);
        };
    };
}

function makeChangeVisibilityActionCreator(factory) {
    return (action) => {
        return (dispatcher) => {
            const {drawLayerId}= action.payload;
            const drawLayer= getDrawLayerById(getDlAry(), drawLayerId);
            dispatcher(action);
            factory.onVisibilityChange(drawLayer,action);
        };
    };
}


function asyncDrawDataWatcher(action, cancelSelf, params) {
        const {drawLayerId, plotId}= action.payload;
        const drawLayerAry= getDlAry();
        const drawLayer= getDrawLayerById(drawLayerAry, drawLayerId);
        const {drawLayerFactory}=  params;
        if (drawLayer) {
            drawLayerFactory.asyncComputeDrawData(drawLayer,action);
        }
        else if (plotId) {
            drawLayerAry
                .filter( (dl) => dl.visiblePlotIdAry
                    .find( (testPlotId) => testPlotId===plotId))
                .forEach( (dl) => drawLayerFactory.asyncComputeDrawData(dl,action));
        }
}


//=============================================
//=============================================
//=============================================
/**
 *
 * @param factory
 * @ignore
 */
function makeReducer(factory) {
    const dlReducer= DrawLayerReducer.makeReducer(factory);
    return (state=initState(), action={}) => {


        if (action.type===REINIT_APP) return initState();

        if (!action.payload || !action.type) return state;
        if (!state.allowedActions.includes(action.type)) return state;

        let retState = state;
        switch (action.type) {
            case CHANGE_VISIBILITY:
            case CHANGE_DRAWING_DEF:
            case FORCE_DRAW_LAYER_UPDATE:
            case MODIFY_CUSTOM_FIELD:
                retState = deferToLayerReducer(state, action, dlReducer);
                break;
            case UPDATE_DRAWING_LAYER:
                retState = doUpdateDrawLayer(state, action);
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
                const {payload}= action;
                if (payload.destroyWhenAllDetached &&
                    isEmpty(getConnectedPlotsIds(retState,payload.drawLayerId))) {
                    retState = destroyDrawLayer(retState, action);
                }
                break;
            case PRE_ATTACH_LAYER_TO_PLOT:
                retState = preattachLayerToPlot(state,action);
                break;
            case DELETE_PLOT_VIEW:
                retState = deletePlotView(state, action, dlReducer);
                break;
            case CHANGE_HIPS:
            case ANY_REPLOT:
            case CHANGE_CENTER_OF_PROJECTION:
                retState = determineAndCallLayerReducer(state, action, dlReducer, true);
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
 * @returns {Object} the new state;
 * @ignore
 */
function createDrawLayer(state,action) {
    const {drawLayer}= action.payload;
    const allowedActions= union(state.allowedActions, drawLayer.actionTypeAry);

    return Object.assign({}, state,
        {allowedActions, drawLayerAry: [...state.drawLayerAry, drawLayer] });
}

function doUpdateDrawLayer(state,action) {
    const {drawLayer}= action.payload;
    const drawLayerAry= state.drawLayerAry.map( (dl) => dl.drawLayerId===drawLayer.drawLayerId ? drawLayer : dl);
    return Object.assign({}, state, {drawLayerAry} );
}

/**
 * Destroy the drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @returns {Object} the new state;
 * @ignore
 */
function destroyDrawLayer(state,action) {
    const {drawLayerId}= action.payload;
    return Object.assign({}, state,
        {drawLayerAry: state.drawLayerAry.filter( (c) => c.drawLayerId!==drawLayerId) });
}

/**
 * Call the reducer for the drawing layer defined by the action
 * @param state
 * @param {{type:string,payload:object}} action
 * @param dlReducer drawinglayer subreducer{string|string[]}
 * @returns {Object} the new state;
 * @ignore
 */
function deferToLayerReducer(state,action,dlReducer) {
    const {drawLayerId}= action.payload;
    const drawLayer= state.drawLayerAry.find( (dl) => drawLayerId===dl.drawLayerId);

    if (drawLayer) {
        const newDl= dlReducer(drawLayer,action);
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
 * @returns {Object} the new state;
 * @ignore
 */
function determineAndCallLayerReducer(state,action,dlReducer,force) {
    const newAry= state.drawLayerAry.map( (dl) => {
        if (force || (dl.actionTypeAry && dl.actionTypeAry.includes(action.type))) {
            const newdl= dlReducer(dl,action);
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


function preattachLayerToPlot(state,action) {
    const {drawLayerTypeId,plotIdAry}= action.payload;
    const currentAry= state.preAttachedTypes[drawLayerTypeId] || [];

    const preAttachedTypes=  {...state.preAttachedTypes, [drawLayerTypeId]: union(currentAry,plotIdAry)};
    return {...state, preAttachedTypes};
}


function deletePlotView(state,action, dlReducer) {
    const {plotId} = action.payload;
    const drawLayerAry= state.drawLayerAry
        .map( (dl) => dlReducer(dl, {type:DETACH_LAYER_FROM_PLOT, payload:{plotIdAry:[plotId]}}))
        .filter( (dl) => !(dl.destroyWhenAllDetached && isEmpty(dl.plotIdAry)));
    return {...state, drawLayerAry};
}

/**
 *
 * @return {DrawLayerRoot}
 */
const initState= function() {

    /**
     * @global
     * @public
     * @typedef {Object} DrawLayerRoot
     *
     * @summary The state of the Drawing layers store.
     * @prop {DrawLayer[]} drawLayerAry the array of all the drawing layers
     * @prop {string[]} allowedActions the actions the go to the drawing layers by default
     */
    return {
        allowedActions: [ CREATE_DRAWING_LAYER, DESTROY_DRAWING_LAYER, CHANGE_VISIBILITY,
                          ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT, MODIFY_CUSTOM_FIELD,
                          CHANGE_DRAWING_DEF,FORCE_DRAW_LAYER_UPDATE,
                          ANY_REPLOT, DELETE_PLOT_VIEW, CHANGE_CENTER_OF_PROJECTION, CHANGE_HIPS, UPDATE_DRAWING_LAYER
                        ],
        drawLayerAry : [],
        preAttachedTypes : {}  // {futureDrawLayerTypeId : [string] }
                               //  i.e. an object: keys are futureDrawLayerTypeId, values: array of plot id

    };

};


