/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {drawRegions} from '../visualize/region/RegionDrawer.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {get, isEmpty} from 'lodash';

const ID= 'REGION_PLOT';
const TYPE_ID= 'REGION_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, null);
export default {factoryDef, TYPE_ID};

var idCnt=0;

/**
 * create region plot layer
 * @return {Function}
 */
function creator(initPayload) {

    var drawingDef= makeDrawingDef('green');
    var pairs = {
        [MouseState.DOWN.key]: highlightChange
    };

    idCnt++;
    var options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DISABLE,
        isPointData:false,
        hasPerPlotData: false,
        destroyWhenAllDetached: true
    };

    var dl = DrawLayer.makeDrawLayer( get(initPayload, 'drawLayerId', `${ID}-${idCnt}`),
                                      TYPE_ID, get(initPayload, 'title', 'Region Plot'),
                                      options, drawingDef, null, pairs );
    dl.regions = get(initPayload, 'regions', null);
    dl.highlightedRegion = get(initPayload, 'highlightedRegion', null);

    return dl;
}


/**
 * find the drawObj which is selected for highlight
 * @param mouseStatePayload
 * @returns {Function}
 */
function highlightChange(mouseStatePayload) {
    const {drawLayer,plotId,screenPt} = mouseStatePayload;
    var done = false;
    var closestInfo = null;
    var closestObj = null;
    const maxChunk = 1000;
    const {data} = drawLayer.drawData;
    const plot = primePlot(visRoot(), plotId);


    function* getDrawObj() {
        var index = 0;

        while (index < data.length) {
            yield data[index++];
        }
    }
    var gen = getDrawObj();

    const sId = window.setInterval( () => {
        if (done) {
            window.clearInterval(sId);
            dispatchModifyCustomField(TYPE_ID, {highlightedRegion: closestObj}, false);
        }

        for (let i = 0; i < maxChunk; i++ ) {
            var dObj = gen.next().value;

            if (dObj) {
                var distInfo = DrawOp.isScreenPointInside(screenPt, dObj, plot);

                if (distInfo.inside) {
                   if (!closestInfo || closestInfo.dist > distInfo.dist) {
                       closestInfo = distInfo;
                       closestObj = dObj;
                   }
                }
            } else {
                done = true;
                break;
            }
        }
    }, 0);

    return () => window.clearInterval(sId);
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
            const {changes} = action.payload;
            var dd = Object.assign({}, drawLayer.drawData);

            if (changes.regions) {
                dd[DataTypes.HIGHLIGHT_DATA] = null;
                dd[DataTypes.DATA] = null;
                if (drawLayer.highlightedRegion) drawLayer.highlightedRegion = null;
            } else if (changes.highlightedRegion) {
                dd[DataTypes.HIGHLIGHT_DATA] = null;
                if (drawLayer.highlightedRegion) drawLayer.highlightedRegion.highlight = 0;
                changes.highlightedRegion.highlight = 1;
            } else {
                dd[DataTypes.HIGHLIGHT_DATA] = null;
                if (drawLayer.highlightedRegion) {
                    drawLayer.highlightedRegion.highlight = 0; // un-highlight the last one
                    drawLayer.highlightedRegion = null;
                }
            }

            return Object.assign({}, changes, {drawData: dd});
        default:
            return null;
    }
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {regions, highlightedRegion} = drawLayer;
    plotId = get(visRoot(), 'activePlotId');

    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? plotAllRegions(regions) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? plotHighlightRegion(highlightedRegion, plotId) : lastDataRet;
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

function plotHighlightRegion(highlightedObj, plotId) {
    if (!highlightedObj) {
        return [];
    }

    var plot =  primePlot(visRoot(),plotId);

    return [DrawOp.makeHighlight(highlightedObj, plot)];
}

