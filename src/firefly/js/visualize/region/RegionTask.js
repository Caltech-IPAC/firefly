/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getDS9Region} from '../../rpc/PlotServicesJson.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import DrawLayerCntrl, {dispatchCreateDrawLayer, dispatchDetachLayerFromPlot,
        dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {getDrawLayerById, getPlotViewIdListInGroup} from '../PlotViewUtil.js';
import RegionPlot, {createNewRegionLayerId, getRegionLayerTitle} from '../../drawingLayers/RegionPlot.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {logError} from '../../util/WebUtil.js';
import {has, isArray, isEmpty, get, isNil} from 'lodash';

const regionDrawLayerId = RegionPlot.TYPE_ID;

var [RegionIdErr, RegionErr, NoRegionErr, JSONErr] = [
    'region id is not specified',
    'invalid description in region file',
    'no region is specified',
    'get region json error'];

export const getPlotId = (plotId) => {
    //return (!plotId || (isArray(plotId)&&plotId.length === 0)) ? get(visRoot(), 'activePlotId') : plotId;
    if (!plotId || (isArray(plotId)&&plotId.length === 0)) {
        const pid = get(visRoot(), 'activePlotId');

        return getPlotViewIdListInGroup(visRoot(), pid, false);
    } else {
        return plotId;
    }
};


const layerId = (drawLayerId) => {
    return isNil(drawLayerId) ? createNewRegionLayerId() : drawLayerId;
};


/**
 * action creator of REGION_CREATE_LAYER, create drawing layer based on region file or array of region description
 * @param rawAction
 * @returns {Function}
 */
export function regionCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        const {drawLayerId, fileOnServer, layerTitle, regionAry, plotId,
               selectMode} = rawAction.payload;

        if (!drawLayerId) {
            reportError(RegionIdErr);
        } else if (fileOnServer) {   // region file is given, get region description array
            getDS9Region(fileOnServer).then((result) => {
                if (has(result, 'RegionData')) {
                    createRegionLayer(result.RegionData,  getRegionLayerTitle(layerTitle||result.title),
                                      drawLayerId, plotId,  selectMode, 'json');
                } else {
                    reportError(RegionErr);
                }
            }, () => { reportError(JSONErr); } );
        } else if (!isEmpty(regionAry)) {
            createRegionLayer(regionAry, getRegionLayerTitle(layerTitle),
                              drawLayerId, plotId, selectMode);
        } else {
            reportError(NoRegionErr);
        }
    };
}

/**
 * parse array of region description in json or ds9 format and create drawing layer based on parsed results
 * @param regionAry
 * @param title
 * @param drawLayerId
 * @param plotId
 * @param selectMode
 * @param dataFrom
 */
function createRegionLayer(regionAry, title, drawLayerId, plotId,
                           selectMode,
                           dataFrom = 'ds9') {
    // convert region description array to Region object array

    if (isEmpty(regionAry)) {
        return reportError(NoRegionErr);
    }
    if (!isArray(regionAry)) {
        regionAry = [regionAry];
    }

    const pId = getPlotId(plotId);

    drawLayerId = layerId(drawLayerId);
    const dl = getDrawLayerById(getDlAry(), drawLayerId);

    if (!dl) {
        dispatchCreateDrawLayer(regionDrawLayerId, {title, drawLayerId,
                                                    regionAry, dataFrom, plotId: pId,
                                                    selectMode});
    }

    if (pId) {
        dispatchAttachLayerToPlot(drawLayerId, pId, true);
    }
}


/**
 * action creator for REGION_DELETE_LAER, remove drawing layer of regions
 * @param rawAction
 * @returns {Function}
 */
export function regionDeleteLayerActionCreator(rawAction) {
    return (dispatcher) => {
        const {plotId, drawLayerId} = rawAction.payload;

        const pId = getPlotId(plotId);
        const dl = getDrawLayerById(getDlAry(), drawLayerId);

        if (dl && drawLayerId && pId) {
            dispatchDetachLayerFromPlot(drawLayerId, pId, true, dl.destroyWhenAllDetached);
        } else {
            reportError(`${RegionIdErr} for deleting region layer`);
        }
    };
}

/**
 * action creator for REGION_ADD_ENTRY or REGION_REMOVE_ENTRY, add or remove regions from drawing layer
 * @param rawAction
 * @returns {Function}
 */
export function regionUpdateEntryActionCreator(rawAction) {
    return (dispatcher) => {
        const {drawLayerId, regionChanges, layerTitle, plotId,
              selectMode} = rawAction.payload || {};

        if (isEmpty(regionChanges)) {
            return;
        }

        const changes = isArray(regionChanges) ? regionChanges : [regionChanges];
        const dl = drawLayerId ? getDrawLayerById(getDlAry(), drawLayerId) : null;

        // if drawlayer doesn't exist, create a new one
        if (!dl && rawAction.type === DrawLayerCntrl.REGION_ADD_ENTRY) {
            const title = getRegionLayerTitle(layerTitle);

            createRegionLayer(changes, title, drawLayerId, plotId, selectMode);
        } else {
            const payload = Object.assign(rawAction.payload, {regionChanges: changes});

            if (drawLayerId) {
                dispatcher(Object.assign(rawAction, {payload})); // add and remove entries
            } else {
                reportError(`${RegionIdErr} for ${rawAction.type}`);
            }
        }
    };
}

function reportError(errMsg) {
    logError(errMsg);
}
