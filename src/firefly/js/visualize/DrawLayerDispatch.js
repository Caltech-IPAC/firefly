import {flatten, uniqBy} from 'lodash';
import {flux} from '../core/ReduxFlux';
import {dlRoot, visRoot} from './VisStoreRoots';
import {toBoolean} from '../util/WebUtil';
import {
    getAllPlotViewIdByOverlayLock, getDrawLayerById, getPlotViewById, getPlotViewIdListInOverlayGroup
} from './PlotViewUtil';
import {
    ATTACH_LAYER_TO_PLOT, CHANGE_DRAWING_DEF, CHANGE_VISIBILITY, CREATE_DRAWING_LAYER, defaultRegionSelectColor,
    defaultRegionSelectStyle, RegionSelectStyle, DESTROY_DRAWING_LAYER, UPDATE_DRAWING_LAYER,
    DETACH_LAYER_FROM_PLOT, FOOTPRINT_CREATE, FORCE_DRAW_LAYER_UPDATE, IMAGELINEBASEDFP_CREATE, MARKER_CREATE,
    MODIFY_CUSTOM_FIELD, REGION_ADD_ENTRY, REGION_CREATE_LAYER, REGION_DELETE_LAYER, REGION_REMOVE_ENTRY, REGION_SELECT,
} from './VisConst';

/**
 * @summary create drawing layer
 * @param {string} drawLayerTypeId - id of drawing layer
 * @param {Object} params
 * @public
 * @memberof firefly.action
 * @function  dispatchCreateDrawLayer
 */
export function dispatchCreateDrawLayer(drawLayerTypeId, params = {}) {
    const drawLayer = flux.createDrawLayer(drawLayerTypeId, params);
    flux.process({type: CREATE_DRAWING_LAYER, payload: {drawLayer}});

    const plotIdAry = dlRoot().preAttachedTypes[drawLayerTypeId];
    if (plotIdAry) {
        dispatchAttachLayerToPlot(drawLayerTypeId, plotIdAry);
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
export function dispatchChangeVisibility({id, visible, plotId, useGroup = true, subGroupId, matchTitle = false}) {
    let plotIdAry = useGroup ? getPlotViewIdListInOverlayGroup(visRoot(), plotId) : [plotId];
    if (subGroupId) {
        const vr = visRoot();
        plotIdAry = plotIdAry.filter((plotId) => {
            const pv = getPlotViewById(vr, plotId);
            return (pv && subGroupId === pv.drawingSubGroupId);
        });
    }
    if (plotIdAry.length) {
        getDrawLayerIdAry(dlRoot(), id, matchTitle)
            .forEach((drawLayerId) => {
                flux.process({type: CHANGE_VISIBILITY, payload: {drawLayerId, visible, plotIdAry}});
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
export function dispatchChangeDrawingDef(id, drawingDef, plotId, matchTitle = false) {
    const plotIdAry = getPlotViewIdListInOverlayGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(), id, matchTitle)
        .forEach((drawLayerId) => {
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
export function dispatchModifyCustomField(id, changes, plotId) {

    const plotIdAry = getPlotViewIdListInOverlayGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(), id)
        .forEach((drawLayerId) => {
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
export function dispatchForceDrawLayerUpdate(id, plotId) {

    const plotIdAry = getPlotViewIdListInOverlayGroup(visRoot(), plotId);

    getDrawLayerIdAry(dlRoot(), id)
        .forEach((drawLayerId) => {
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
    const drawLayerId = getDrawLayerId(dlRoot(), id);
    if (drawLayerId) {
        flux.process({type: DESTROY_DRAWING_LAYER, payload: {drawLayerId}});
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
export function dispatchAttachLayerToPlot(id, plotId, attachAllPlot = false, visible = true, plotTypeMustMatch = false) {

    let plotIdAry;
    let layerVisible;

    if (visible === 'inherit') {
        layerVisible = getDrawLayerIdAry(dlRoot(), id, true).some((drawLayerId) =>
            getDrawLayerById(dlRoot(), drawLayerId).visiblePlotIdAry.length);
    } else {
        layerVisible = toBoolean(visible);
    }

    if (Array.isArray(plotId)) {
        plotIdAry = plotId;
    } else {
        plotIdAry = attachAllPlot ? getAllPlotViewIdByOverlayLock(visRoot(), plotId, false, plotTypeMustMatch) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(), id)
        .forEach((drawLayerId) => {
            flux.process({type: ATTACH_LAYER_TO_PLOT, payload: {drawLayerId, plotIdAry, visible: layerVisible}});
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
export function dispatchDetachLayerFromPlot(id, plotId, detachAllPlot = false, destroyWhenAllDetached = false) {
    let plotIdAry;

    if (Array.isArray(plotId)) {
        plotIdAry = plotId;
    } else {
        plotIdAry = detachAllPlot ? getAllPlotViewIdByOverlayLock(visRoot(), plotId) : [plotId];
    }

    getDrawLayerIdAry(dlRoot(), id)
        .forEach((drawLayerId) => {
            flux.process({type: DETACH_LAYER_FROM_PLOT, payload: {drawLayerId, plotIdAry, destroyWhenAllDetached}});
        });

}

function getRegionSelectStyle(style = defaultRegionSelectStyle) {
    const idx = RegionSelectStyle.findIndex((val) => {
        return val.toLowerCase() === style.toLowerCase();
    });

    return (idx < 0) ? defaultRegionSelectStyle : RegionSelectStyle[idx];
}


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
export function dispatchCreateRegionLayer(drawLayerId, layerTitle, fileOnServer = '', regionAry = [], plotId = '',
                                          selectMode = {},
                                          dispatcher = flux.process) {

    dispatcher({
        type: REGION_CREATE_LAYER, payload: {
            drawLayerId, fileOnServer, plotId, layerTitle, regionAry,
            selectMode: validateSelectMode(selectMode)
        }
    });
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
export function dispatchAddRegionEntry(drawLayerId, regionChanges, plotId = [], layerTitle = '',
                                       selectMode = {},
                                       dispatcher = flux.process) {

    dispatcher({
        type: REGION_ADD_ENTRY, payload: {
            drawLayerId, regionChanges, plotId, layerTitle,
            selectMode: validateSelectMode(selectMode)
        }
    });
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
export function dispatchCreateMarkerLayer(markerId, layerTitle, plotId = [], attachPlotGroup = true, dispatcher = flux.process) {
    dispatcher({type: MARKER_CREATE, payload: {plotId, markerId, layerTitle, attachPlotGroup}});
}

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
                                             {
                                                 footprint = null,
                                                 instrument = null,
                                                 relocateBy = 'origin',
                                                 fromFile = null,
                                                 fromRegionAry = null
                                             },
                                             plotId = [], attachPlotGroup = true, dispatcher = flux.process) {
    dispatcher({
        type: FOOTPRINT_CREATE,
        payload: {
            plotId,
            footprintId,
            layerTitle,
            footprint,
            instrument,
            relocateBy,
            attachPlotGroup,
            fromFile,
            fromRegionAry
        }
    });

}

export function dispatchCreateImageLineBasedFootprintLayer(drawLayerId, title, fpData, plotId = [],
                                                           footprintFile, footprintImageFile, tbl_index,
                                                           attachPlotGroup = true, dispatcher = flux.process) {
    dispatcher({
        type: IMAGELINEBASEDFP_CREATE,
        payload: {
            plotId,
            drawLayerId,
            title,
            footprintData: fpData,
            footprintFile,
            footprintImageFile,
            tbl_index,
            attachPlotGroup
        }
    });
}

function getDrawLayerId(dlRoot, id) {
    let drawLayer = dlRoot.drawLayerAry.find((dl) => id === dl.drawLayerId);
    if (!drawLayer) {
        drawLayer = dlRoot.drawLayerAry.find((dl) => id === dl.drawLayerTypeId);
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
function getDrawLayerIdAry(dlRoot, id, matchTitles = false) {
    const idAry = Array.isArray(id) ? id : [id];
    const dlAry = dlRoot.drawLayerAry
        .filter((dl) => idAry
            .filter((id) => new Set([dl.drawLayerId, dl.drawLayerTypeId, dl.displayGroupId]).has(id))
            .length > 0);

    let retDlAry = dlAry;
    if (matchTitles) { //look for any other DrawLayers with titles that match the already found list of layers
        const matchTitleAry =
            uniqBy(flatten(dlAry
                .map((dl) => dlRoot.drawLayerAry
                    .filter((nextDl) => dl.title === nextDl.title && nextDl !== dl))), 'drawLayerId');
        retDlAry = [...dlAry, ...matchTitleAry];
    }

    return retDlAry.map((dl) => dl.drawLayerId);
}

/**
 * check and create selectMode with valid property and value.
 * @param selectMode
 * @returns {{selectStyle, selectColor, lineWidth}}
 */
function validateSelectMode(selectMode) {
    const {selectStyle = defaultRegionSelectStyle, selectColor = defaultRegionSelectColor, lineWidth = 0} = selectMode;
    const regSelectStyle = getRegionSelectStyle(selectStyle);

    return {selectStyle: regSelectStyle, selectColor, lineWidth};
}
