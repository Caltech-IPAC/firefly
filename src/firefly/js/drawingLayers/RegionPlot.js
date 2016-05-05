/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {drawRegions} from '../visualize/region/RegionDrawer.js';
import {get} from 'lodash';

const ID= 'REGION_PLOT';
const TYPE_ID= 'REGION_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, null, getLayerChanges, null, null);
export default {factoryDef, TYPE_ID};

var idCnt=0;

/**
 * create region plot layer
 * @return {Function}
 */
function creator(initPayload) {

    var drawingDef= makeDrawingDef('green');

    idCnt++;
    var options= {
        canUseMouse:true,
        canUserChangeColor: ColorChangeType.DISABLE,
        isPointData:false,
        hasPerPlotData: true
    };

    return DrawLayer.makeDrawLayer( `${ID}-${idCnt}`, TYPE_ID, get(initPayload, 'title', 'Region Plot'),
                                     options, drawingDef );

}

/**
 * state update on the drawlayer change
 * @param drawLayer
 * @param action
 * @returns {*}
 */
function getLayerChanges(drawLayer, action) {

    switch (action.type) {
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            let drawData = {};
            const regions = get(action, 'payload.changes.regions');

            if (regions) {
                //drawLayer.regions = Object.assign({}, regions);
                drawData[DataTypes.DATA] = plotAllRegions(regions);
            }

            return {drawData};
    }

    return null;
}

/**
 * create DrawingObj for all regions
 * @param regionAry
 * @returns {*}
 */
function plotAllRegions(regionAry) {
    if (!regionAry) {
        return [];
    }

    return drawRegions(regionAry);

}
