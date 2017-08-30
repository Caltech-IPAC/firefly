/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import Enum from 'enum';
import {getPlotViewIdListInGroup, getPlotViewById, getDrawLayerById,
          getConnectedPlotsIds, getAllPlotViewId} from './PlotViewUtil.js';
import ImagePlotCntlr, {visRoot}  from './ImagePlotCntlr.js';
import DrawLayerReducer from './reducer/DrawLayerReducer.js';
import {without,union,isEmpty} from 'lodash';
import {clone} from '../util/WebUtil.js';

import {selectAreaEndActionCreator} from '../drawingLayers/SelectArea.js';
import {distanceToolEndActionCreator} from '../drawingLayers/DistanceTool.js';
import {markerToolStartActionCreator,
        markerToolMoveActionCreator,
        markerToolEndActionCreator,
        markerToolCreateLayerActionCreator} from '../drawingLayers/MarkerTool.js';

import {regionCreateLayerActionCreator,
        regionDeleteLayerActionCreator,
        regionUpdateEntryActionCreator} from './region/RegionTask.js';

import {footprintCreateLayerActionCreator,
        footprintStartActionCreator,
        footprintMoveActionCreator,
        footprintEndActionCreator
} from '../drawingLayers/FootprintTool.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';

export const DRAWLAYER_PREFIX = 'DrawLayerCntlr';

export const SUBGROUP= 'subgroup';

/** {Enum} can be 'GROUP', 'SUBGROUP', 'SINGLE' */
export const GroupingScope= new Enum(['GROUP', 'SUBGROUP', 'SINGLE']);




const CREATE_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.createDrawLayer`;
const DESTROY_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.destroyDrawLayer`;
const CHANGE_VISIBILITY= `${DRAWLAYER_PREFIX}.changeVisibility`;
const CHANGE_DRAWING_DEF= `${DRAWLAYER_PREFIX}.changeDrawingDef`;
const ATTACH_LAYER_TO_PLOT= `${DRAWLAYER_PREFIX}.attachLayerToPlot`;
const PRE_ATTACH_LAYER_TO_PLOT= `${DRAWLAYER_PREFIX}.attachLayerToPlot`;
const DETACH_LAYER_FROM_PLOT= `${DRAWLAYER_PREFIX}.detachLayerFromPlot`;
const MODIFY_CUSTOM_FIELD= `${DRAWLAYER_PREFIX}.modifyCustomField`;
const FORCE_DRAW_LAYER_UPDATE= `${DRAWLAYER_PREFIX}.forceDrawLayerUpdate`;
const TABLE_TO_IGNORE= `${DRAWLAYER_PREFIX}.tableToIgnore`;

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
 * @returns {DrawLayer[]}
 * @memberof firefly.action
 * @function  getDlAry
 */
export function getDlAry() { return flux.getState()[DRAWING_LAYER_KEY].drawLayerAry; }


/**
 * Return the draw layer store
 * @returns {DrawLayerRoot}
 * @memberof firefly.action
 * @function  getDlRoot
 */
export function getDlRoot() { return flux.getState()[DRAWING_LAYER_KEY]; }





export function getDrawLayerCntlrDef(drawLayerFactory) {
    return {
        reducers() {return {[DRAWING_LAYER_KEY]: makeReducer(drawLayerFactory)}; },

        actionCreators() {
            return {
                [DETACH_LAYER_FROM_PLOT] :  makeDetachLayerActionCreator(drawLayerFactory),
                [SELECT_AREA_END] :  selectAreaEndActionCreator,
                [DT_END] :  distanceToolEndActionCreator,
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
                [REGION_REMOVE_ENTRY] :  regionUpdateEntryActionCreator
            };
        }
    };
}


export default {
    getDrawLayerCntlrDef,
    CHANGE_VISIBILITY,
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
    makeReducer, dispatchChangeVisibility,
    dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
    dispatchAttachLayerToPlot, dispatchDetachLayerFromPlot,
    dispatchCreateRegionLayer, dispatchDeleteRegionLayer,
    dispatchAddRegionEntry, dispatchRemoveRegionEntry,
    dispatchCreateMarkerLayer, dispatchCreateFootprintLayer
};




/**
 *
 * @param {string} drawLayerTypeId
 * @param {string} tableId
 * @public
 * @memberof firefly.action
 * @function dispatchTableToIgnore
 */
export function dispatchTableToIgnore(drawLayerTypeId, tableId) {
    flux.process({type: TABLE_TO_IGNORE , payload: {drawLayerTypeId,tableId} });
}

/**
 * @summary create drawing layer
 * @param {string} drawLayerTypeId - id of drawing layer
 * @param {Object} params
 * @public
 * @memberof firefly.action
 * @function  dispatchCreateDrawLayer
 */
export function dispatchCreateDrawLayer(drawLayerTypeId, params={}) {
    const drawLayer= flux.createDrawLayer(drawLayerTypeId,params);
    flux.process({type: CREATE_DRAWING_LAYER, payload: {drawLayer}} );

    const plotIdAry= dlRoot().preAttachedTypes[drawLayerTypeId];
    if (plotIdAry) {
        dispatchAttachLayerToPlot(drawLayerTypeId,plotIdAry);
    }
}


/**
 * @summary change the visibility of the drawing layer
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param visible
 * @param plotId
 * @param useGroup If true, get all the plotViews in the group of the plotId, if false use only the one
 * @param subGroupId if defined the list of PlotViews affected will be further filtered by the subGroupId
 *  @public
 *  @memberof firefly.action
 *  @function dispatchChangeVisibility
 */
export function dispatchChangeVisibility(id,visible, plotId, useGroup= true, subGroupId= undefined) {
    let plotIdAry= useGroup ? getPlotViewIdListInGroup(visRoot(), plotId) : [plotId];
    if (subGroupId) {
        const vr= visRoot();
        plotIdAry= plotIdAry.filter( (plotId) => {
            const pv= getPlotViewById(vr,plotId);
            return  (pv && subGroupId===pv.drawingSubGroupId);
        });
    }
    if (plotIdAry.length) {
        getDrawLayerIdAry(dlRoot(),id,useGroup)
            .forEach( (drawLayerId) => {
                flux.process({type: CHANGE_VISIBILITY, payload: {drawLayerId, visible, plotIdAry} });
            });
    }
}

/**
 * @summary change the drawing definition of the drawing layer
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param drawingDef
 * @param plotId
 * @param useGroup
 *  @public
 *  @memberof firefly.action
 *  @function dispatchChangeDrawingDef
 */
export function dispatchChangeDrawingDef(id,drawingDef, plotId, useGroup= true) {
    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: CHANGE_DRAWING_DEF, payload: {drawLayerId, drawingDef, plotIdAry}});
        });
}


/**
 * @summary create custom changes to the drawing layer
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param changes
 * @param plotId
 * @param useGroup
 * @public
 * @memberof firefly.action
 * @function dispatchModifyCustomField
 */
export function dispatchModifyCustomField(id,changes, plotId, useGroup= true) {

    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: MODIFY_CUSTOM_FIELD, payload: {drawLayerId, changes, plotIdAry}});
        });
}

/**
 * @summary force to update the drawing layer
 * @param id
 * @param plotId
 * @param useGroup
 * @public
 * @memberof firefly.action
 * @function dispatchForceDrawLayerUpdate
 */
export function dispatchForceDrawLayerUpdate(id,plotId, useGroup= true) {

    var plotIdAry= getPlotViewIdListInGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id,useGroup)
        .forEach( (drawLayerId) => {
            flux.process({type: FORCE_DRAW_LAYER_UPDATE, payload: {drawLayerId, plotIdAry}});
        });
}



/**
 * @summary destroy the drawing layer
 * @param {string} id make the drawLayerId or drawLayerTypeId
 * @public
 * @memberof firefly.action
 * @function dispatchDestroyDrawLayer
 */
export function dispatchDestroyDrawLayer(id) {
    var drawLayerId= getDrawLayerId(dlRoot(),id);
    if (drawLayerId) {
        flux.process({type: DESTROY_DRAWING_LAYER, payload: {drawLayerId} });
    }
}

/**
 * @summary attach drawing layer to plot
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|string[]} plotId to attach this may by a string or an array of strings
 * @param attachAllPlot
 * @memberof firefly.action
 * @public
 * @function  dispatchAttachLayerToPlot
 */
export function dispatchAttachLayerToPlot(id,plotId,  attachAllPlot=false) {
    var plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry = attachAllPlot ? getAllPlotViewId(visRoot(), plotId) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(),id,false)
        .forEach( (drawLayerId) => {
            flux.process({type: ATTACH_LAYER_TO_PLOT, payload: {drawLayerId, plotIdAry} });
        });
}


/**
 * @summary Detatch drawing layer from the plot
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|string[]} plotId to attach this may by a string or an array of string
 * @param detachAllPlot
 * @param useLayerGroup
 * @param destroyWhenAllDetached if all plots are detached then destroy this plot
 * @public
 * @memberof firefly.action
 * @function dispatchDetachLayerFromPlot
 */
export function dispatchDetachLayerFromPlot(id,plotId, detachAllPlot=false,
                                            useLayerGroup=true, destroyWhenAllDetached=false) {
    var plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry= detachAllPlot ? getAllPlotViewId(visRoot(), plotId) : [plotId];
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
 */
function validateSelectMode(selectMode) {
    var {selectStyle = defaultRegionSelectStyle, selectColor = defaultRegionSelectColor, lineWidth = 0 } = selectMode;

    selectStyle = getRegionSelectStyle(selectStyle);

    return {selectStyle, selectColor, lineWidth};
}

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
 * @summary Create drawing layer based on region file or region description
 * @param {string} drawLayerId - id of the drawing layer to be created, required
 * @param {string} layerTitle - if it is empty, it will be created internally
 * @param {string} fileOnServer - region file name on server
 * @param {string[]|string} regionAry - array or string of region description
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {RegionSelectMode} selectMode - rendering features for the selected region
 * @param {Function} dispatcher
 * @public
 * @function dispatchCreateRegionLayer
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
 * @param {Function} dispatcher
 * @public
 * @function dispatchDeleteRegionLayer
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
 * @param {RegionSelectMode} selectMode - rendering features for the selected region
 * @param {Function} dispatcher
 * @public
 * @function dispatchAddRegionEntry
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
 * @param {Function} dispatcher
 * @public
 * @function dispatchRemoveRegionEntry
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
 * @param {Function} dispatcher
 * @public
 * @function dispatchSelectRegion
 * @memberof firefly.action
 * @see {@link firefly.util.image.getSelectedRegion} to get the string describing the selected region
 */
export function dispatchSelectRegion(drawLayerId, selectedRegion, dispatcher = flux.process) {
    dispatcher({type: REGION_SELECT, payload: {drawLayerId, selectedRegion}});
}

/**
 * @summary create drawing layer with marker
 * @param {string} markerId - id of the drawing layer
 * @param {string} layerTitle - title of the drawing layer
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {bool} attachPlotGroup - attach all plots of the same plot group
 * @param dispatcher
 * @public
 * @function dispatchCreateMarkerLayer
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
 * @param {bool} attachPlotGroup - attach all plots of the same plot group
 * @param dispatcher
 * @public
 * @function dispatchCreateFootprintLayer
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

function makeDetachLayerActionCreator(factory) {
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
            case TABLE_TO_IGNORE:
                retState = addTableToIgnore(state,action);
                break;
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                retState = deletePlotView(state, action, dlReducer);
                break;
            case ImagePlotCntlr.ANY_REPLOT:
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
    var {drawLayer}= action.payload;
    var allowedActions= union(state.allowedActions, drawLayer.actionTypeAry);

    return Object.assign({}, state,
        {allowedActions, drawLayerAry: [...state.drawLayerAry, drawLayer] });
}

/**
 * Destroy the drawing layer
 * @param state
 * @param {{type:string,payload:object}} action
 * @returns {Object} the new state;
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
 * @returns {Object} the new state;
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
 * @returns {Object} the new state;
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


function addTableToIgnore(state,action) {
    const {drawLayerTypeId,tableId}= action.payload;
    const {ignoreTables}= state;
    if (!ignoreTables.some( ( obj) => obj.tableId===tableId && obj.drawLayerTypeId===drawLayerTypeId)) {
       return clone(state, {ignoreTables: [...ignoreTables, {drawLayerTypeId,tableId}]} );
    }
    return state;
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
     * @prop {Array.<{tableId:string, drawLayerTypeId:string}>} ignoreTables -  an array of object that
     *                              are tableId and a draw layer
     */
    return {
        allowedActions: [ CREATE_DRAWING_LAYER, DESTROY_DRAWING_LAYER, CHANGE_VISIBILITY,
                          ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT, MODIFY_CUSTOM_FIELD,
                          CHANGE_DRAWING_DEF,FORCE_DRAW_LAYER_UPDATE,TABLE_TO_IGNORE,
                          ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.DELETE_PLOT_VIEW
                        ],
        drawLayerAry : [],
        ignoreTables : [],
        preAttachedTypes : {}  // {futureDrawLayerTypeId : [string] }
                               //  i.e. an object: keys are futureDrawLayerTypeId, values: array of plot id

    };

};


