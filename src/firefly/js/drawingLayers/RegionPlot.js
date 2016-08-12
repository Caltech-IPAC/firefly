/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr, {dispatchDeleteRegionLayer} from '../visualize/DrawLayerCntlr.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {drawRegions} from '../visualize/region/RegionDrawer.js';
import {addNewRegion, removeRegion} from '../visualize/region/RegionUtil.js';
import {RegionFactory} from '../visualize/region/RegionFactory.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import DrawLayerCntrl, {dispatchModifyCustomField,
                        dispatchAddRegionEntry,
                        dispatchRemoveRegionEntry, dlRoot} from '../visualize/DrawLayerCntlr.js';

import {get, set, isEmpty} from 'lodash';

const ID= 'REGION_PLOT';
const TYPE_ID= 'REGION_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, null);
export default {factoryDef, TYPE_ID};

var idCnt=0;

/**
 * create region plot layer
 * in region plot layer, attribute regionAry: region description array
 *                                 dataFrom: regionAry is from 'json' object or 'ds9' string
 *                                 regions:   array of region object constructed by parsing regionAry
 *                                 regionObjAry: array of drawing object constructed from regions
 *                                 => regions and regionObjAry are updated as adding or removing regions occurs
 *                                 highlightedRegion: selected region
 * @return {Function}
 */
function creator(initPayload) {

    var drawingDef= makeDrawingDef('green');
    var pairs = {
        [MouseState.DOWN.key]: highlightChange
        //[MouseState.DOWN.key]: removeRegionDescription
    };

    idCnt++;
    var options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DISABLE,
        isPointData:false,
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    var actionTypes = [DrawLayerCntrl.REGION_ADD_ENTRY,
                       DrawLayerCntlr.REGION_REMOVE_ENTRY];

    var id = get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    var dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', 'Region Plot'),
                                      options, drawingDef, actionTypes, pairs );

    dl.regionAry = get(initPayload, 'regionAry', null);
    dl.dataFrom = get(initPayload, 'dataFrom', 'ds9');
    dl.highlightedRegion = get(initPayload, 'highlightedRegion', null);

    idCnt++;
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
        var dataPlot = get(data, plotId);

        while (index < dataPlot.length) {
            yield dataPlot[index++];
        }
    }
    var gen = getDrawObj();

    const sId = window.setInterval( () => {
        if (done) {
            window.clearInterval(sId);

            // set the highlight region on current drawLayer,
            // unset the highlight on other drawLayer if a highlight is found for current layer

            dlRoot().drawLayerAry.forEach( (dl) => {
                if (dl.drawLayerId === drawLayer.drawLayerId) {
                    dispatchModifyCustomField(dl.drawLayerId, {highlightedRegion: closestObj}, plotId, false);
                } else if (closestObj) {
                    dispatchModifyCustomField(dl.drawLayerId, {highlightedRegion: null}, plotId, false);
                }
            });
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
    const {changes, regionChanges, drawLayerId } = action.payload;

    if (drawLayerId && drawLayerId !== drawLayer.drawLayerId) return null;
    var dd = Object.assign({}, drawLayer.drawData);

    switch (action.type) {
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
             if (changes) {
                 dd[DataTypes.HIGHLIGHT_DATA] = null;
                 if (!changes.highlightedRegion) {
                     if (drawLayer.highlightedRegion) {
                         drawLayer.highlightedRegion.highlight = 0; // un-highlight the last selected region
                         drawLayer.highlightedRegion = null;
                     }
                 } else {
                     if (drawLayer.highlightedRegion) drawLayer.highlightedRegion.highlight = 0;
                     changes.highlightedRegion.highlight = 1;

                 }
             }
             return Object.assign({}, changes, {drawData: dd});
        case DrawLayerCntrl.REGION_ADD_ENTRY:
            if (regionChanges) {
                var {layerTitle} = action.payload;

                if (layerTitle) {
                    drawLayer.title = layerTitle.slice();  // update title of the layer
                }
                addRegionsToData(drawLayer, regionChanges);
                Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                    set(dd[DataTypes.DATA], plotId, drawLayer.drawObjAry)
                });
            }
            return {drawData: dd};

        case DrawLayerCntrl.REGION_REMOVE_ENTRY:
            if (regionChanges) {
                removeRegionsFromData(drawLayer, regionChanges);
                Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                    set(dd[DataTypes.DATA], plotId, drawLayer.drawObjAry)
                });
            }

            return {drawData: dd};

        default:
            return null;
    }
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {highlightedRegion, drawObjAry} = drawLayer;
    var preData = get(lastDataRet, plotId, null) || lastDataRet;

    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(preData) ? drawObjAry || plotAllRegions(drawLayer) : preData;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(preData) ? plotHighlightRegion(highlightedRegion, plotId) : preData;
    }
    return null;
}
/**
 * create DrawingObj for all regions
 * @param dl    drawing layer
 * @returns {*}
 */
function plotAllRegions(dl) {
    var {dataFrom, regionAry} = dl; //regionAry: array of region strings
                                    //dataFrom: from 'json' (server) or 'ds9' (original ds9 description)
    if (!regionAry) {
        return [];
    }

    dl.regions =  (dataFrom === 'json') ? RegionFactory.parseRegionJson(regionAry) :
                                          RegionFactory.parseRegionDS9(regionAry);

    dl.drawObjAry = drawRegions(dl.regions);   //no need regionAry anymore
    return dl.drawObjAry;
}

/**
 * create DrawingObj for highlighted region
 * @param highlightedObj
 * @param plotId
 * @returns {*}
 */
function plotHighlightRegion(highlightedObj, plotId) {
    if (!highlightedObj) {
        return [];
    }

    if (highlightedObj.region) highlightedObj.region.highlighted = 1;
    return [DrawOp.makeHighlight(highlightedObj, primePlot(visRoot(), plotId))];
}

/**
 * add new DrawingObj into originally displayed DrawingObj set
 * @param drawLayer
 * @param addedRegions
 * @returns {Array}
 */
function addRegionsToData(drawLayer, addedRegions) {
    var {regions, drawObjAry: lastData} = drawLayer;
    var resultRegions = regions ? regions.slice() : [];
    var allDrawobjs = lastData ? lastData.slice() : [];

    if (addedRegions) {
        resultRegions = addedRegions.reduce ( (prev, aRegionDes) => {
            var aRegion = RegionFactory.parsePart(aRegionDes);
            var newDrawobj = addNewRegion(prev, aRegion);

            if (newDrawobj) {
                prev.push(aRegion);
                allDrawobjs.push(newDrawobj);
            }
            return prev;
        }, resultRegions);
    }

    drawLayer.regions = resultRegions;
    drawLayer.drawObjAry = allDrawobjs;
    return allDrawobjs;
}

/**
 * remove DrawingObj from originally displayed DrawingObj set
 * @param drawLayer
 * @param removedRegions
 * @returns {Array}
 */
function removeRegionsFromData(drawLayer, removedRegions) {
    var {regions, drawObjAry: lastData} = drawLayer;
    var resultRegions = regions ? regions.slice() : [];
    var allDrawObjs = lastData ? lastData.slice() : [];

    if (resultRegions.length === 0) {
        return [];     // no region to be removed
    }

    if (removedRegions) {
        resultRegions = removedRegions.reduce((prev, aRegionDes) => {
            var rmRegion = RegionFactory.parsePart(aRegionDes);
            var {index, regions} = removeRegion( prev, rmRegion );

            if (index >= 0) {
                allDrawObjs.splice(index, 1);
                prev = regions;
            }
            return prev;
          }, resultRegions);
    }

    drawLayer.regions = resultRegions;
    drawLayer.drawObjAry = allDrawObjs;
    return allDrawObjs;
}