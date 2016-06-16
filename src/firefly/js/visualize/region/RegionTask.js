/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {RegionFactory} from './RegionFactory.js';
import {getDS9Region} from '../../rpc/PlotServicesJson.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {dispatchCreateDrawLayer, dispatchDetachLayerFromPlot,
        dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {getDrawLayerById} from '../PlotViewUtil.js';
import RegionPlot from '../../drawingLayers/RegionPlot.js';
import {getPlotViewAry} from '../PlotViewUtil.js';
import {visRoot } from '../ImagePlotCntlr.js';
import {has, isArray, get} from 'lodash';

const regionDrawLayerId = RegionPlot.TYPE_ID;

var cnt = 0;
var [RegionIdErr, RegionErr, DrawObjErr, JSONErr] = [
    'region id is not specified',
    'invalid description in region file',
    'create drawing object error',
    'get region json error'];

var getPlotId = (plotId) => {
    return (!plotId || (isArray(plotId)&&plotId.length === 0)) ? get(visRoot(), 'activePlotId') : plotId;

};

/**
 * action creator of REGION_CREATE_LAYER, create drawing layer based on region file or array of region description
 * @param rawAction
 * @returns {Function}
 */
export function regionCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {regionId, fileOnServer, layerTitle, regionAry, plotId} = rawAction.payload;

        if (regionId && fileOnServer) {   // region file is given, get region description array
            getDS9Region(fileOnServer).then((result) => {
                if (has(result, 'RegionData')) {
                    createRegionLayer(result.RegionData, result.Title, regionId, plotId, 'json');
                } else {
                    reportError(RegionErr);
                }
            }, () => { reportError(JSONErr); } );
        } else if (regionId && regionAry) {              // region description array or a string is given
            if (!isArray(regionAry)) {
                regionAry = [regionAry];
            }
            var title = layerTitle ? layerTitle : `Region Plot - ${cnt++}`;

            createRegionLayer(regionAry, title, regionId, plotId);
        } else {
            regionId ? reportError(RegionErr) : reportError(RegionIdErr);
        }
    };
}

/**
 * parse array of region description in json or ds9 format and create drawing layer based on parsed results
 * @param regionAry
 * @param title
 * @param regionId
 * @param plotId
 * @param dataFrom
 */
function createRegionLayer(regionAry, title, regionId, plotId, dataFrom = 'ds9') {
    // convert region description array to Region object array

    if (regionAry && regionAry.length > 0) {

        var rgAry = dataFrom === 'json' ? RegionFactory.parseRegionJson(regionAry) :
                                          RegionFactory.parseRegionDS9(regionAry);

        if (rgAry && rgAry.length > 0) {
            var dl = getDrawLayerById(getDlAry(), regionId);

            if (!dl) {
                dispatchCreateDrawLayer(regionDrawLayerId, {title, drawLayerId: regionId, regions: rgAry});
            }

            var pId = getPlotId(plotId);
            if (pId) {
                dispatchAttachLayerToPlot(regionId, pId, true);
            }
        } else {
            reportError(DrawObjErr);
        }
    } else {
        reportError(`${RegionIdErr} for creating region layer`);
    }
}


/**
 * action creator for REGION_DELETE_LAER, remove drawing layer of regions
 * @param rawAction
 * @returns {Function}
 */
export function regionDeleteLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {plotId, regionId} = rawAction.payload;

        var pId = getPlotId(plotId);

        if (regionId && pId) {
            dispatchDetachLayerFromPlot(regionId, pId, true, false);
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
        var {regionId, regionChanges} = rawAction.payload || {};
        var changes = [];

        if (regionChanges) {
            changes = isArray(regionChanges) ? regionChanges : [regionChanges];
        }

        var payload = Object.assign(rawAction.payload, {regionChanges: changes});

        if (regionId) {
            dispatcher(Object.assign(rawAction, {payload})); // add and remove entries
        } else {
            reportError(`${RegionIdErr} for ${rawAction.type}`);
        }
    };
}

function reportError(errMsg) {
    console.log(errMsg);
}
