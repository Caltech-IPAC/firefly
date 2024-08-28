/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../core/ReduxFlux.js';
import Enum from 'enum';
import {
    getAllPlotViewIdByOverlayLock,
    getConnectedPlotsIds,
    getDrawLayerById,
    getPlotViewById,
    getPlotViewIdListInOverlayGroup
} from './PlotViewUtil.js';
import ImagePlotCntlr, {visRoot} from './ImagePlotCntlr.js';
import DrawLayerReducer from './reducer/DrawLayerReducer.js';
import {flatten, isEmpty, union, uniqBy, without} from 'lodash';
import {clone, toBoolean} from '../util/WebUtil.js';

import {selectAreaEndActionCreator} from '../drawingLayers/SelectArea.js';
import {distanceToolEndActionCreator} from '../drawingLayers/DistanceTool.js';
import {
    extractLineToolEndActionCreator,
    extractLineToolStartActionCreator
} from 'firefly/drawingLayers/ExtractLineTool.js';
import {
    markerToolCreateLayerActionCreator,
    markerToolEndActionCreator,
    markerToolMoveActionCreator,
    markerToolStartActionCreator
} from '../drawingLayers/MarkerTool.js';

import {
    regionCreateLayerActionCreator,
    regionDeleteLayerActionCreator,
    regionUpdateEntryActionCreator
} from './region/RegionTask.js';

import {
    footprintCreateLayerActionCreator,
    footprintEndActionCreator,
    footprintMoveActionCreator,
    footprintStartActionCreator
} from '../drawingLayers/FootprintTool.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {imageLineBasedfootprintActionCreator} from './task/LSSTFootprintTask.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';

export const DRAWLAYER_PREFIX = 'DrawLayerCntlr';

export const SUBGROUP= 'subgroup';

/** {Enum} can be 'GROUP', 'SUBGROUP', 'SINGLE' */
export const GroupingScope= new Enum(['STANDARD', 'SUBGROUP', 'SINGLE']);




const CREATE_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.createDrawLayer`;
const UPDATE_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.updateDrawLayer`;
const DESTROY_DRAWING_LAYER= `${DRAWLAYER_PREFIX}.destroyDrawLayer`;
const CHANGE_VISIBILITY= `${DRAWLAYER_PREFIX}.changeVisibility`;
const CHANGE_DRAWING_DEF= `${DRAWLAYER_PREFIX}.changeDrawingDef`;
export const ATTACH_LAYER_TO_PLOT= `${DRAWLAYER_PREFIX}.attachLayerToPlot`;
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
const EXTRACT_POINT=  `${DRAWLAYER_PREFIX}.ExtractPoints.extractPoint`;


// _- Distance tool
const DT_START= `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolStart`;
const DT_MOVE= `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolMove`;
const DT_END= `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolEnd`;

// _- Distance tool
const ELT_START= `${DRAWLAYER_PREFIX}.ExtractLineTool.distanceToolStart`;
const ELT_MOVE= `${DRAWLAYER_PREFIX}.ExtractLineTool.distanceToolMove`;
const ELT_END= `${DRAWLAYER_PREFIX}.ExtractLineTool.distanceToolEnd`;

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

const IMAGELINEBASEDFP_CREATE = `${DRAWLAYER_PREFIX}.ImageLineBasedFP.imagelineBasedFPCreate`;

export const DRAWING_LAYER_KEY= 'drawLayers';
export function dlRoot() { return flux.getState()[DRAWING_LAYER_KEY]; }

export const RegionSelectStyle = ['UprightBox', 'DottedOverlay', 'SolidOverlay',
                                  'DottedReplace', 'SolidReplace'];
export const defaultRegionSelectColor = '#DAA520';   // golden
export const defaultRegionSelectStyle = RegionSelectStyle[0];
export const RegionSelColor = 'selectColor';
export const RegionSelStyle = 'selectStyle';


export function getRegionSelectStyle(style = defaultRegionSelectStyle) {
    const idx = RegionSelectStyle.findIndex((val) => {
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

    setTimeout( () => {
        dispatchAddActionWatcher({
            actions:[CHANGE_VISIBILITY, CHANGE_DRAWING_DEF, ATTACH_LAYER_TO_PLOT,
                DETACH_LAYER_FROM_PLOT, FORCE_DRAW_LAYER_UPDATE, MODIFY_CUSTOM_FIELD,
                ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.CHANGE_HIPS,
                ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION
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
    CHANGE_VISIBILITY,
    ATTACH_LAYER_TO_PLOT, DETACH_LAYER_FROM_PLOT,CHANGE_DRAWING_DEF,
    CREATE_DRAWING_LAYER,DESTROY_DRAWING_LAYER, MODIFY_CUSTOM_FIELD,
    SELECT_AREA_START, SELECT_AREA_MOVE, SELECT_AREA_END, SELECT_MOUSE_LOC,
    SELECT_POINT, EXTRACT_POINT,
    FORCE_DRAW_LAYER_UPDATE,
    DT_START, DT_MOVE, DT_END,
    ELT_START, ELT_MOVE, ELT_END,
    REGION_CREATE_LAYER, REGION_DELETE_LAYER,  REGION_ADD_ENTRY, REGION_REMOVE_ENTRY,
    REGION_SELECT,
    MARKER_START, MARKER_MOVE, MARKER_END, MARKER_CREATE,
    FOOTPRINT_CREATE, FOOTPRINT_START, FOOTPRINT_END, FOOTPRINT_MOVE,
    IMAGELINEBASEDFP_CREATE,
    dispatchCreateDrawLayer,
    dispatchCreateImageLineBasedFootprintLayer
};



export function deleteAllDrawLayers() {
    (getDlAry() || [])
        .filter((l) => l.drawLayerId)
        .forEach((id) => dispatchDestroyDrawLayer(id));
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
    return drawLayer;
}


/**
 * @summary change the visibility of one more drawing layers on a set of WebPlots.
 * This function can match the drawing layers using drawLayerId, drawLayerTypeId and further
 * match using the title of the draw layer. it can match hhe plots by plotId, then all the plots in the group, and then
 * can limit the plots in the group using subgroupId
 * @param {Object} p
 * @param {string|string[]} p.id - make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {boolean} p.visible
 * @param {string} p.plotId - the plotId to change the visibility on, if used group is defined then visibility will be
 * change for all the plotIds in the PlotGroup
 * @param {boolean} [p.useGroup] - If true, get all the plotViews in the group of the plotId, if false use only the one
 * @param {string} [p.subGroupId] - if defined the list of PlotViews affected will be filtered by the subGroupId. In other words
 * it will only change the visibility on PlotView that have a matching subGroupId.
 * @param {boolean} [p.matchTitle] -  matches any draw layers that have the same title as the one specified by the id
 *  @public
 *  @memberof firefly.action
 *  @function dispatchChangeVisibility
 */
export function dispatchChangeVisibility({id,visible, plotId, useGroup= true, subGroupId, matchTitle= false}) {
    let plotIdAry= useGroup ? getPlotViewIdListInOverlayGroup(visRoot(), plotId) : [plotId];
    if (subGroupId) {
        const vr= visRoot();
        plotIdAry= plotIdAry.filter( (plotId) => {
            const pv= getPlotViewById(vr,plotId);
            return  (pv && subGroupId===pv.drawingSubGroupId);
        });
    }
    if (plotIdAry.length) {
        getDrawLayerIdAry(dlRoot(),id, matchTitle)
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
 * @param {boolean} [matchTitle] -  matches any draw layers that have the same title as the one specified by the id
 *  @public
 *  @memberof firefly.action
 *  @function dispatchChangeDrawingDef
 */
export function dispatchChangeDrawingDef(id,drawingDef, plotId, matchTitle= false) {
    const plotIdAry= getPlotViewIdListInOverlayGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id, matchTitle)
        .forEach( (drawLayerId) => {
            flux.process({type: CHANGE_DRAWING_DEF, payload: {drawLayerId, drawingDef, plotIdAry}});
        });
}


/**
 * @summary create custom changes to the drawing layer
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {Object} changes any object of changes
 * @param {string} [plotId] a plotId
 * @public
 * @memberof firefly.action
 * @function dispatchModifyCustomField
 */
export function dispatchModifyCustomField(id,changes, plotId) {

    const plotIdAry= getPlotViewIdListInOverlayGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id)
        .forEach( (drawLayerId) => {
            flux.process({type: MODIFY_CUSTOM_FIELD, payload: {drawLayerId, changes, plotIdAry}});
        });
}

/**
 *
 * @param {DrawLayer|Object} drawLayer
 */
export function dispatchUpdateDrawLayer(drawLayer) {
    flux.process({type: UPDATE_DRAWING_LAYER, payload: {drawLayer}});
}

/**
 * @summary force to update the drawing layer
 * @param id
 * @param plotId
 * @public
 * @memberof firefly.action
 * @function dispatchForceDrawLayerUpdate
 */
export function dispatchForceDrawLayerUpdate(id,plotId) {

    const plotIdAry= getPlotViewIdListInOverlayGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(),id)
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
    const drawLayerId= getDrawLayerId(dlRoot(),id);
    if (drawLayerId) {
        flux.process({type: DESTROY_DRAWING_LAYER, payload: {drawLayerId} });
    }
}

/**
 * @summary attach drawing layer to plot
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|string[]} plotId to attach this may by a string or an array of strings
 * @param {boolean} attachAllPlot
 * @param {boolean|string} visible - Can have three values: true: layer is attach visible, false: attach not-visible,
 * value (string) 'inherit' layer is visible
 * @param {boolean} plotTypeMustMatch
 * @memberof firefly.action
 * @public
 * @function  dispatchAttachLayerToPlot
 */
export function dispatchAttachLayerToPlot(id,plotId,  attachAllPlot=false, visible= true, plotTypeMustMatch=false) {

    let plotIdAry;
    let layerVisible;

    if (visible==='inherit') {
        layerVisible= getDrawLayerIdAry(dlRoot(),id, true).some( (drawLayerId) =>
                                   getDrawLayerById(dlRoot(),drawLayerId).visiblePlotIdAry.length);
    }
    else {
        layerVisible= toBoolean(visible);
    }

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry = attachAllPlot ? getAllPlotViewIdByOverlayLock(visRoot(), plotId, false, plotTypeMustMatch) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(),id)
        .forEach( (drawLayerId) => {
            flux.process({type: ATTACH_LAYER_TO_PLOT, payload: {drawLayerId, plotIdAry, visible:layerVisible} });
        });
}


/**
 * @summary Detach drawing layer from the plot
 * @param {string|string[]} id make the drawLayerId or drawLayerTypeId, this may be an array
 * @param {string|string[]} plotId to attach this may by a string or an array of string
 * @param detachAllPlot
 * @param destroyWhenAllDetached if all plots are detached then destroy this plot
 * @public
 * @memberof firefly.action
 * @function dispatchDetachLayerFromPlot
 */
export function dispatchDetachLayerFromPlot(id,plotId, detachAllPlot=false, destroyWhenAllDetached=false) {
    let plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry= plotId;
    }
    else {
        plotIdAry= detachAllPlot ? getAllPlotViewIdByOverlayLock(visRoot(), plotId) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(),id)
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
    const {selectStyle = defaultRegionSelectStyle, selectColor = defaultRegionSelectColor, lineWidth = 0 } = selectMode;
    const regSelectStyle = getRegionSelectStyle(selectStyle);

    return {selectStyle:regSelectStyle , selectColor, lineWidth};
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

/**
 * @summary create drawing layer with footprint
 * @param {string} footprintId - id of the drawing layer
 * @param {string} layerTitle - title of the drawing layer
 * @param {footprintInfo} footprintData footprint information for footprint layer,
 *                        relocateBy: 'origin' means relocating footprint origin to the target location
 *                                    'center' means relocating footprint center to the target location
 * @param {string[]|string} plotId - array or string of plot id. If plotId is empty, all plots of the active group are applied
 * @param {bool} attachPlotGroup - attach all plots of the same plot group
 * @param dispatcher
 * @public
 * @function dispatchCreateFootprintLayer
 * @memberof firefly.action
 */
export function dispatchCreateFootprintLayer(footprintId, layerTitle,
                                             {footprint=null, instrument=null, relocateBy='origin',  fromFile=null, fromRegionAry=null},
                                             plotId = [], attachPlotGroup=true, dispatcher = flux.process) {
    dispatcher({type: FOOTPRINT_CREATE, payload: {plotId, footprintId, layerTitle, footprint, instrument, relocateBy, attachPlotGroup, fromFile, fromRegionAry}});

}

export function dispatchCreateImageLineBasedFootprintLayer(drawLayerId, title, fpData, plotId = [],
                                                                     footprintFile, footprintImageFile, tbl_index,
                                                                     attachPlotGroup=true, dispatcher = flux.process) {
    dispatcher({
        type: IMAGELINEBASEDFP_CREATE,
        payload: {plotId, drawLayerId, title, footprintData: fpData, footprintFile, footprintImageFile, tbl_index, attachPlotGroup}
    });
}


function getDrawLayerId(dlRoot,id) {
    let drawLayer= dlRoot.drawLayerAry.find( (dl) => id===dl.drawLayerId);
    if (!drawLayer) {
        drawLayer= dlRoot.drawLayerAry.find( (dl) => id===dl.drawLayerTypeId);
    }
    return drawLayer ? drawLayer.drawLayerId : null;
}

/**
 *
 * @param dlRoot
 * @param id - drawLayerId or drawLayerTypeId or displayGroupId
 * @param matchTitles
 * @return {Array.<String>} the list of drawLayerIds
 */
function getDrawLayerIdAry(dlRoot,id, matchTitles= false) {
    const idAry= Array.isArray(id) ? id: [id];
    const dlAry= dlRoot.drawLayerAry
        .filter( (dl) => idAry
            .filter( (id) => new Set([dl.drawLayerId,dl.drawLayerTypeId,dl.displayGroupId]).has(id))
            .length>0);

    let retDlAry= dlAry;
    if (matchTitles) { //look for any other DrawLayers with titles that match the already found list of layers
        const matchTitleAry=
            uniqBy(flatten(dlAry
                .map( (dl) => dlRoot.drawLayerAry
                    .filter( (nextDl) => dl.title===nextDl.title && nextDl!==dl) )), 'drawLayerId');
        retDlAry= [...dlAry, ...matchTitleAry];
    }

    return retDlAry.map(  (dl) => dl.drawLayerId);
}


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
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                retState = deletePlotView(state, action, dlReducer);
                break;
            case ImagePlotCntlr.CHANGE_HIPS:
            case ImagePlotCntlr.ANY_REPLOT:
            case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
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

    const preAttachedTypes=  clone( state.preAttachedTypes, {[drawLayerTypeId]: union(currentAry,plotIdAry)});
    return clone(state, {preAttachedTypes});
}


function deletePlotView(state,action, dlReducer) {
    const {plotId} = action.payload;

    const drawLayerAry= state.drawLayerAry
        .map( (dl) => dlReducer(dl, {type:DETACH_LAYER_FROM_PLOT, payload:{plotIdAry:[plotId]}}))
        .filter( (dl) => !(dl.destroyWhenAllDetached && isEmpty(dl.plotIdAry)));


    return clone(state, {drawLayerAry});
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
                          ImagePlotCntlr.ANY_REPLOT, ImagePlotCntlr.DELETE_PLOT_VIEW,
                          ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION,
                          ImagePlotCntlr.CHANGE_HIPS, UPDATE_DRAWING_LAYER
                        ],
        drawLayerAry : [],
        preAttachedTypes : {}  // {futureDrawLayerTypeId : [string] }
                               //  i.e. an object: keys are futureDrawLayerTypeId, values: array of plot id

    };

};


