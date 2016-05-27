/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {RegionFactory} from './RegionFactory.js';
import {getDS9Region} from '../../rpc/PlotServicesJson.js';
import {getDlAry} from '../DrawLayerCntlr.js';
import {dispatchCreateDrawLayer, dispatchDestroyDrawLayer,
        dispatchAttachLayerToPlot} from '../DrawLayerCntlr.js';
import {getDrawLayerById} from '../PlotViewUtil.js';
import RegionPlot from '../../drawingLayers/RegionPlot.js';
import {getPlotViewAry} from '../PlotViewUtil.js';
import {visRoot } from '../ImagePlotCntlr.js';
import {has} from 'lodash';

const regionDrawLayerId = RegionPlot.TYPE_ID;

export default {
    regionCreateLayerActionCreator, regionDeleteLayerActionCreator
};

var cnt = 0;
var [RegionErr, DrawObjErr, JSONErr] = [
    'invalid description in region file',
    'create drawing object error',
    'get region json error' ];

/**
 * action creator of REGION_CREATE_LAYER, create drawing layer based on region file or array of region description
 * @param rawAction
 * @returns {Function}
 */
export function regionCreateLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {regionId, fileOnServer, layerTitle, regionAry} = rawAction.payload;

        if (fileOnServer) {   // region file is given, get region description array
            getDS9Region(fileOnServer).then((result) => {
                if (has(result, 'RegionData')) {
                    createRegionLayer(result.RegionData, result.Title, regionId, 'json');
                } else {
                    reportError(RegionErr);
                }
            }, () => { reportError(JSONErr); } );
        } else if (regionAry && regionAry.length > 0) {              // region description array is given
            var title = layerTitle ? layerTitle : `Region Plot - ${cnt++}`;

            createRegionLayer(regionAry, title, regionId);
        } else {
            reportError(RegionErr);
        }
    };
}

/**
 * parse array of region description in json or ds9 format and create drawing layer based on parsed results
 * @param regionAry
 * @param title
 * @param regionId
 * @param dataFrom
 */
function createRegionLayer(regionAry, title, regionId, dataFrom = 'ds9') {
    // convert region description array to Region object array

    if (regionAry && regionAry.length > 0) {

        var rgAry = dataFrom === 'json' ? RegionFactory.parseRegionJson(regionAry) :
                                          RegionFactory.parseRegionDS9(regionAry);

        if (rgAry && rgAry.length > 0) {
            var dl = getDrawLayerById(getDlAry(), regionId);

            if (!dl) {
                dispatchCreateDrawLayer(regionDrawLayerId, {title, drawLayerId: regionId, regions: rgAry});
            }

            var plotIdAry = getPlotViewAry(visRoot()).map((pv) => pv.plotId);
            dispatchAttachLayerToPlot(regionId, plotIdAry, true);
        } else {
            reportError(DrawObjErr);
        }
    } else {
        reportError(RegionErr);
    }
}


/**
 * action creator for REGION_DELETE_LAER, remove drawing layer of regions
 * @param rawAction
 * @returns {Function}
 */
export function regionDeleteLayerActionCreator(rawAction) {
    return (dispatcher) => {
        var {regionId} = rawAction.payload;

        if (regionId) {
            dispatchDestroyDrawLayer(regionId);
        }
    };
}

function reportError(errMsg) {
    console.log(errMsg);
}

// temporarily showing some hard coded regions which are defined but currently not identified by server
/*
 result.RegionData = [
 'J2000;ellipse 202.55556, 47.188286 20p 40p 0i # color=#48f text={ellipse 1} width=10',
 'physical;ellipse 100 400 20p 40p 30p 60p 40p 80p 2i # color=green text={ellipseannulus 2}',
 'image;box 100 100 20p 40p 30p 50p 70p 100p 30 # color=red text={slanted box annulus 3}'];
 */