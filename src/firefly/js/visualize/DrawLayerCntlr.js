/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {getPlotViewIdListInGroup, getDrawLayerById, getConnectedPlotsIds} from './PlotViewUtil.js';
import ImagePlotCntlr, {visRoot}  from './ImagePlotCntlr.js';
import DrawLayerReducer from './reducer/DrawLayerReducer.js';
import {without,union,omit,isEmpty,get} from 'lodash';
import {clone} from '../util/WebUtil.js';
import Enum from 'enum';


export {selectAreaEndActionCreator} from '../drawingLayers/SelectArea.js';
export {distanceToolEndActionCreator} from '../drawingLayers/DistanceTool.js';
export {markerToolStartActionCreator,
        markerToolMoveActionCreator,
        markerToolEndActionCreator,
        markerToolCreateLayerActionCreator} from '../drawingLayers/MarkerTool.js';

export {regionCreateLayerActionCreator,
        regionDeleteLayerActionCreator,
        regionUpdateEntryActionCreator} from './region/RegionTask.js';

export {footprintCreateLayerActionCreator,
        footprintStartActionCreator,
        footprintMoveActionCreator,
        footprintEndActionCreator
} from '../drawingLayers/FootprintTool.js';

export const DRAWLAYER_PREFIX = 'DrawLayerCntlr';

const RETRIEVE_DATA= `${DRAWLAYER_PREFIX}.retrieveData`;
const CREATE_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.createDrawLayer`;
const DESTROY_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.destroyDrawLayer`;
const CHANGE_VISIBILITY= `${DRAWLAYER_PREFIX}.changeVisibility`;
const CHANGE_DRAWING_DEF= `${DRAWLAYER_PREFIX}.changeDrawingDef`;
const ATTACH_LAYER_TO_PLOT= `${DRAWLAYER_PREFIX}.attachLayerToPlot`;
const PRE_ATTACH_LAYER_TO_PLOT= `${DRAWLAYER_PREFIX}.attachLayerToPlot`;
const DETACH_LAYER_FROM_PLOT= `${DRAWLAYER_PREFIX}.detachLayerFromPlot`;
const MODIFY_CUSTOM_FIELD= `${DRAWLAYER_PREFIX}.modifyCustomField`;
const FORCE_DRAW_LAYER_UPDATE= `${DRAWLAYER_PREFIX}.forceDrawLayerUpdate`;

// _- select
const SELECT_AREA_START= `${DRAWLAYER_PREFIX}.SelectArea.selectAreaStart`;
const SELECT_AREA_MOVE= `${DRAWLAYER_PREFIX}.SelectArea.selectAreaMove`;
const SELECT_AREA_END= `${DRAWLAYER_PREFIX}.SelectArea.selectAreaEnd`;
const SELECT_MOUSE_LOC= `${DRAWLAYER_PREFIX}.SelectArea.selectMouseLoc`;

const SELECT_POINT=  `${DRAWLAYER_PREFIX}.SelectPoint.selectPoint`;


// _- Distance tool
const DT_START= `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolStart`;
const DT_MOVE= `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolMove`;
const DT_END= `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolEnd`;

// region
const REGION_CREATE_LAYER = `${DRAWLAYER_PREFIX}.RegionPlot.createLayer`;
const REGION_DELETE_LAYER = `${DRAWLAYER_PREFIX}.RegionPlot.deleteLayer`;
const REGION_ADD_ENTRY = `${DRAWLAYER_PREFIX}.RegionPlot.addRegion`;
const REGION_REMOVE_ENTRY = `${DRAWLAYER_PREFIX}.RegionPlot.removeRegion`;
const REGION_SELECT = `${DRAWLAYER_PREFIX}.RegionPlot.selectRegion`;

// marker and footprint
const MARKER_START = `${DRAWLAYER_PREFIX}.MarkerTool.markerStart`;
const MARKER_MOVE = `${DRAWLAYER_PREFIX}.MarkerTool.markerMove`;
const MARKER_END = `${DRAWLAYER_PREFIX}.MarkerTool.markerEnd`;
const MARKER_CREATE= `${DRAWLAYER_PREFIX}.MarkerTool.markerCreate`;
const FOOTPRINT_CREATE = `${DRAWLAYER_PREFIX}.FootprintTool.footprintCreate`;
const FOOTPRINT_START = `${DRAWLAYER_PREFIX}.FootprintTool.footprintStart`;
const FOOTPRINT_END = `${DRAWLAYER_PREFIX}.FootprintTool.footprintEnd`;
const FOOTPRINT_MOVE = `${DRAWLAYER_PREFIX}.FootprintTool.footprintMove`;

export const DRAWING_LAYER_KEY= 'drawLayers';
export function dlRoot() { return flux.getState()[DRAWING_LAYER_KEY]; }

export const RegionSelectStyle = ['UprightBox', 'DottedOverlay', 'SolidOverlay',
                                  'DottedReplace', 'SolidReplace'];
export const  defaultRegionSelectColor = '#DAA520';   // golden
export const  defaultRegionSelectStyle = RegionSelectStyle[0];

export function getRegionSelectStyle(style = defaultRegionSelectStyle) {
    var idx = RegionSelectStyle.findIndex((val) => {
        return val.toLowerCase() === style.toLowerCase();
    });

    return (idx < 0) ? defaultRegionSelectStyle : RegionSelectStyle[idx];
}

/**
 * Return, from the store, the master array of all the drawing layers on all the plots
 * @return {Array<Object>}
 * @memberof firefly.action
 * @func  getDlAry
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
    REGION_CREATE_LAYER, REGION_DELETE_LAYER,  REGION_ADD_ENTRY, REGION_REMOVE_ENTRY,
    REGION_SELECT,
    MARKER_START, MARKER_MOVE, MARKER_END, MARKER_CREATE,
    FOOTPRINT_CREATE, FOOTPRINT_START, FOOTPRINT_END, FOOTPRINT_MOVE,
    makeReducer, dispatchRetrieveData, dispatchChangeVisibility,
    dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchAttachLayerToPlot, dispatchDetachLayerFromPlot,
    dispatchCreateRegionLayer, dispatchDeleteRegionLayer,
    dispatchAddRegionEntry, dispatchRemoveRegionEntry,
    dispatchCreateMarkerLayer, dispatchCreateFootprintLayer
};

/**
 *
 * @param drawLayerId
 * @public
 * @memberof firefly.action
 * @func dispatchRetrieveData
 */
export function dispatchRetrieveData(drawLayerId) {
    flux.process({type: RETRIEVE_DATA , payload: {drawLayerId} });

}


/**
 *
 * @param drawLayerTypeId
 * @param params
 * @public
 * @memberof firefly.action
 * @func  dispatchCreateDrawLayer
 */
export function dispatchCreateDrawLayer(drawLayerTypeId, params={}) {
    var drawLayer= flux.createDrawLayer(drawLayerTypeId,params);
    flux.process({type: CREATE_DRAWING_LAYER, payload: {drawLayer}} );

    const plotIdAry= dlRoot().preAttachedTypes[drawLayerTypeId];
    if (plotIdAry) {
        dispatchAttachLayerToPlot(drawLayerTypeId,plotIdAry);
    }
}


/**
 *
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param visible
 * @param plotId
 * @param useGroup
 *  @public
 *  @memberof firefly.action
 *  @func dispatchChangeVisibility
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
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param drawingDef
 * @param plotId
 * @param useGroup
 *  @public
 *  @memberof firefly.action
 *  @func dispatchChangeDrawingDef
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
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param changes
 * @param plotId
 * @param useGroup
 * @public
 * @memberof firefly.action
 * @func dispatchModifyCustomField
 */
export function dispatchModifyCustomField(id,changes, plotId, useGroup= true) {

    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: MODIFY_CUSTOM_FIELD, payload: {drawLayerId, changes, plotIdAry}});
        });
}

/**
 *
 *
 * @param id
 * @param plotId
 * @param useGroup
 *  @public
 * @memberof firefly.action
 * @func dispatchForceDrawLayerUpdate
 */
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
 * @public
 * @memberof firefly.action
 * @func dispatchDestroyDrawLayer
 */
export function dispatchDestroyDrawLayer(id) {
    var drawLayerId= getDrawLayerId(dlRoot(),id);
    if (drawLayerId) {
        flux.process({type: DESTROY_DRAWING_LAYER, payload: {drawLayerId} });
    }
}

/**
 *
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|string[]} plotId to attach this may by a string or an array of strings
 * @param attachPlotGroup
 * @memberof firefly.action
 * @public
 * @func  dispatchAttachLayerToPlot
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
 * @summary Detatch drawing layer from the plot
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|string[]} plotId to attach this may by a string or an array of string
 * @param detachPlotGroup
 * @param useLayerGroup
 * @param destroyWhenAllDetached if all plots are detached then destroy this plot
 * @public
 * @memberof firefly.action
 * @func dispatchDetachLayerFromPlot
 */
export function dispatchDetachLayerFromPlot(id,plotId, detachPlotGroup=false,
                                            useLayerGroup=true, destroyWhenAllDetached=false) {
    var plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry= detachPlotGroup ? getPlotViewIdListInGroup(visRoot(), plotId) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(),id,useLayerGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: DETACH_LAYER_FROM_PLOT, payload: {drawLayerId,plotIdAry, destroyWhenAllDetached} });
        });

}

/**
 * check and create selectMode with valid property and value.
 * @param selectMode
 * @returns {{selectStyle, selectColor, lineWidth}}
 * @ignore
 */
function validateSelectMode(selectMode) {
    var {selectStyle = defaultRegionSelectStyle, selectColor = defaultRegionSelectColor, lineWidth = 0 } = selectMode;

    selectStyle = getRegionSelectStyle(selectStyle);

    return {selectStyle, selectColor, lineWidth};
}

/**
 * @global
 * @public
 * @typedef {Object} regionSelectMode
 * @summary shallow object with the rendering parameters for selected region
 * @prop {string}  [selectStyle] - rendering style for the selected region including 'UprightBox' (default), 'DottedOverlay',
 * 'SolidOverlay', 'DottedReplace', and 'SolidReplace'
 * @prop {string}  [selectColor] - rendering color for the selected region, ex: '#DAA520'(default), 'red'
 * @prop {int}     [lineWidth] - rendering line width for the selected region. 0 (default) or less means the line width
 * is the same as that of the selected region
 */

/**
 * @summary Create drawing layer based on region file or region description
 * @param {string} drawLayerId - id of the drawing layer to be created, required
 * @param {string} layerTitle - if it is empty, it will be created internally
 * @param {string} fileOnServer - region file name on server
 * @param {string[]|string} regionAry - array or string of region description
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {regionSelectMode} selectMode - rendering features for the selected region
 * @param {function} dispatcher
 * @public
 * @func dispatchCreateRegionLayer
 * @memberof firefly.action
 */
export function dispatchCreateRegionLayer(drawLayerId, layerTitle, fileOnServer='', regionAry=[], plotId='',
                                           selectMode = {},
                                           dispatcher = flux.process) {

    dispatcher({type: REGION_CREATE_LAYER, payload: {drawLayerId, fileOnServer, plotId, layerTitle, regionAry,
                                                     selectMode: validateSelectMode(selectMode)}});
}

/**
 * @summary Delete the region drawing layer
 * @param {string} drawLayerId - id of the drawing layer to be deleted, required
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {function} dispatcher
 * @public
 * @func dispatchDeleteRegionLayer
 * @memberof firefly.action
 */
export function dispatchDeleteRegionLayer(drawLayerId, plotId, dispatcher = flux.process) {
    dispatcher({type: REGION_DELETE_LAYER, payload: {drawLayerId, plotId}});
}


/**
 * @summary Add regions to drawing layer
 * @param {string} drawLayerId - id of the drawing layer where the region(s) are added to
 * if the layer doesn't exist, a new drawing layer is created by either using the specified drawLayerId or
 * creating a new id based on the setting of 'layerTitle' in case drawLayerId is undefined
 * @param {string[]|string} regionChanges - array or string of region description
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {string} layerTitle - will replace the original title if the drawing layer exists and layerTitle is non-empty
 * @param {regionSelectMode} selectMode - rendering features for the selected region
 * @param {function} dispatcher
 * @public
 * @func dispatchAddRegionEntry
 * @memberof firefly.action
 */
export function dispatchAddRegionEntry(drawLayerId, regionChanges, plotId=[], layerTitle='',
                                       selectMode = {},
                                       dispatcher = flux.process) {

    dispatcher({type: REGION_ADD_ENTRY, payload: {drawLayerId, regionChanges, plotId, layerTitle,
                                                  selectMode: validateSelectMode(selectMode)}});
}

/**
 * @summary remove region(s) from the drawing layer
 * @param {string} drawLayerId - id of the drawing layer where the region(s) are removed from, required
 * @param {string[]|string} regionChanges - array or string of region description
 * @param {function} dispatcher
 * @public
 * @func dispatchRemoveRegionEntry
 * @memberof firefly.action
 */
export function dispatchRemoveRegionEntry(drawLayerId, regionChanges, dispatcher = flux.process) {
    dispatcher({type: REGION_REMOVE_ENTRY, payload: {drawLayerId, regionChanges}});
}


/**
 * @summary select region from a drawing layer containing regions
 * @param {string} drawLayerId - id of drawing layer where the region is selected from, required
 * @param {string[]|string|Object} selectedRegion - array or string of region description or region object (drawObj)
 * currently only single region is allowed to be selected if the array contains the description of multiple regions.
 * If 'null' or empty array is passed, the function works as de-select the region.
 * @param {function} dispatcher
 * @public
 * @func dispatchSelectRegion
 * @memberof firefly.action
 */
export function dispatchSelectRegion(drawLayerId, selectedRegion, dispatcher = flux.process) {
    dispatcher({type: REGION_SELECT, payload: {drawLayerId, selectedRegion}});
}

/**
 *
 */
/**
 * @summary create drawing layer with marker
 * @param {string} markerId - id of the drawing layer
 * @param {string} layerTitle - title of the drawing layer
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {bool} attachPlotGroup - attach all plots of the same plot group
 * @param dispatcher
 * @public
 * @func dispatchCreateMarkerLayer
 * @memberof firefly.action
 */
export function dispatchCreateMarkerLayer(markerId, layerTitle, plotId = [], attachPlotGroup=true, dispatcher = flux.process) {
    dispatcher({type: MARKER_CREATE, payload: {plotId, markerId, layerTitle, attachPlotGroup}});
}
/**
 * @summary create drawing layer with footprint
 * @param {string} footprintId - id of the drawing layer
 * @param {string} layerTitle - title of the drawiing layer
 * @param {string} footprint - name of footprint project, such as 'HST', 'WFIRST', etc.
 * @param {string} instrument - name of instrument for the footprint
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {bool} attachPlotGroup - - attach all plots of the same plot group
 * @param dispatcher
 * @public
 * @func dispatchCreateFootprintLayer
 * @memberof firefly.action
 */
export function dispatchCreateFootprintLayer(footprintId, layerTitle, footprint, instrument, plotId = [],
                                                                      attachPlotGroup=true, dispatcher = flux.process) {
    dispatcher({type: FOOTPRINT_CREATE, payload: {plotId, footprintId, layerTitle, footprint, instrument, attachPlotGroup}});

}

function getDrawLayerId(dlRoot,id) {
    var drawLayer= dlRoot.drawLayerAry.find( (dl) => id===dl.drawLayerId);
    if (!drawLayer) {
        drawLayer= dlRoot.drawLayerAry.find( (dl) => id===dl.drawLayerTypeId);
    }
    return drawLayer ? drawLayer.drawLayerId : null;
}

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
 * @ignore
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
                const {payload}= action;
                if (payload.destroyWhenAllDetached &&
                    isEmpty(getConnectedPlotsIds(retState,payload.drawLayerId))) {
                    retState = destroyDrawLayer(retState, action);
                }
                break;
            case PRE_ATTACH_LAYER_TO_PLOT:
                retState = preattachLayerToPlot(state,action);
                break;
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                retState = deletePlotView(state, action, dlReducer);
                break;
            case ImagePlotCntlr.ANY_REPLOT:
                retState = determineAndCallLayerReducer(state, action, dlReducer, true);
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
 * @ignore
 */
function createDrawLayer(state,action) {
    var {drawLayer}= action.payload;
    var allowedActions= union(state.allowedActions, drawLayer.actionTypeAry);

    return Object.assign({}, state,
        {allowedActions, drawLayerAry: [...state.drawLayerAry, drawLayer] });
}

/**
 * Destroy the drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @return {object} the new state;
 * @ignore
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
 * @param dlReducer drawinglayer subreducer{string|string[]}
 * @return {object} the new state;
 * @ignore
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
 * @ignore
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

/*
function clearPreattachLayer(state,action) {
    var {drawLayerId}= action.payload;
    var drawLayer= state.drawLayerAry.find( (dl) => drawLayerId===dl.drawLayerId);
    if (drawLayer) return state;
    if (!state.preAttachedTypes[drawLayer.drawLayerTypeId]) return state;
    const preAttachedTypes= omit(state.preAttachedTypes,drawLayer.drawLayerTypeId);
    return clone(state, {preAttachedTypes});
}
*/

function preattachLayerToPlot(state,action) {
    const {drawLayerTypeId,plotIdAry}= action.payload;
    const currentAry= state.preAttachedTypes[drawLayerTypeId] || [];

    const preAttachedTypes=  clone( state.preAttachedTypes, {[drawLayerTypeId]: union(currentAry,plotIdAry)});
    return clone(state, {preAttachedTypes});
}


function deletePlotView(state,action, dlReducer) {
    const {plotId} = action.payload;


    const drawLayerAry= state.drawLayerAry.map( (dl) => {
        return dlReducer(dl, {type:DETACH_LAYER_FROM_PLOT, payload:{plotIdAry:[plotId]}});
    } );

    return Object.assign({},state, {drawLayerAry});
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
                          ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.DELETE_PLOT_VIEW
                        ],
        drawLayerAry : [],
        mouseGrabDrawLayerId : null,
        preAttachedTypes : {}  // {futureDrawLayerTypeId : [string] }
                               //  i.e. an object: keys are futureDrawLayerTypeId, values: array of plot id

    };

};


