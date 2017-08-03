/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {RegionFactory} from './RegionFactory.js';
import {getDS9Region} from '../../rpc/PlotServicesJson.js';
import {getDlAry, defaultRegionSelectColor, getRegionSelectStyle} from '../DrawLayerCntlr.js';
import DrawLayerCntrl, {dispatchCreateDrawLayer, dispatchDetachLayerFromPlot,
        dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {getDrawLayerById, getPlotViewIdListInGroup} from '../PlotViewUtil.js';
import RegionPlot from '../../drawingLayers/RegionPlot.js';
import {getPlotViewAry} from '../PlotViewUtil.js';
import {visRoot} from '../ImagePlotCntlr.js';
import {logError} from '../../util/WebUtil.js'
import {has, isArray, isEmpty, get, isNil} from 'lodash';

const regionDrawLayerId = RegionPlot.TYPE_ID;

var cnt = 0;
var [RegionIdErr, RegionErr, NoRegionErr, JSONErr] = [
    'region id is not specified',
    'invalid description in region file',
    'no region is specified',
    'get region json error'];

var getPlotId = (plotId) => {
    //return (!plotId || (isArray(plotId)&&plotId.length === 0)) ? get(visRoot(), 'activePlotId') : plotId;
    if (!plotId || (isArray(plotId)&&plotId.length === 0)) {
        var pid = get(visRoot(), 'activePlotId');

        return getPlotViewIdListInGroup(visRoot(), pid, false);
    } else {
        return plotId;
    }
};


var layerId = (drawLayerId, title) => {
    return isNil(drawLayerId) ? title.slice() : drawLayerId;
};

var getLayerTitle = (layerTitle, titleRef) => {
    return layerTitle ? layerTitle : (titleRef ? titleRef : `Region Plot - ${cnt++}`);
};

/**
 * action creator of REGION_CREATE_LAYER, create drawing layer based on region file or array of region description
 * @param rawAction
 * @returns {Function}
 */
export function regionCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {drawLayerId, fileOnServer, layerTitle, regionAry, plotId,
            selectMode} = rawAction.payload;
        var title;

        if (!drawLayerId) {
            reportError(RegionIdErr)
        } else if (fileOnServer) {   // region file is given, get region description array
            getDS9Region(fileOnServer).then((result) => {
                if (has(result, 'RegionData')) {
                    title = getLayerTitle(layerTitle, (result.Title || drawLayerId));
                    createRegionLayer(result.RegionData, title, drawLayerId, plotId,
                                      selectMode, 'json');
                } else {
                    reportError(RegionErr);
                }
            }, () => { reportError(JSONErr); } );
        } else if (!isEmpty(regionAry)) {
            title = getLayerTitle(layerTitle, drawLayerId);
            createRegionLayer(regionAry, title, drawLayerId, plotId, selectMode);
        } else {
            reportError(NoRegionErr)
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

    var pId = getPlotId(plotId);

    drawLayerId = layerId(drawLayerId, title);
    var dl = getDrawLayerById(getDlAry(), drawLayerId);

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
        var {plotId, drawLayerId} = rawAction.payload;

        var pId = getPlotId(plotId);
        var dl = getDrawLayerById(getDlAry(), drawLayerId);

        if (dl && drawLayerId && pId) {
            dispatchDetachLayerFromPlot(drawLayerId, pId, true, false, dl.destroyWhenAllDetached);
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
        var {drawLayerId, regionChanges, layerTitle, plotId,
             selectMode} = rawAction.payload || {};

        if (isEmpty(regionChanges)) {
            return;
        }

        var changes = isArray(regionChanges) ? regionChanges : [regionChanges];
        var dl = drawLayerId ? getDrawLayerById(getDlAry(), drawLayerId) : null;

        // if drawlayer doesn't exist, create a new one
        if (!dl && rawAction.type === DrawLayerCntrl.REGION_ADD_ENTRY) {
            var title = getLayerTitle(layerTitle, drawLayerId);

            createRegionLayer(changes, title, drawLayerId, plotId, selectMode);
        } else {
            var payload = Object.assign(rawAction.payload, {regionChanges: changes});

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
