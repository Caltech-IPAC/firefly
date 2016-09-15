/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {drawRegions} from '../visualize/region/RegionDrawer.js';
import {getRegionIndex, addNewRegion, removeRegion} from '../visualize/region/RegionUtil.js';
import {RegionFactory} from '../visualize/region/RegionFactory.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import DrawLayerCntlr, {DRAWING_LAYER_KEY,
                        //defaultRegionSelectColor,
                        //RegionSelectStyle,
                        //getRegionSelectStyle,
                        //dispatchModifyCustomField,
                        //dispatchAddRegionEntry,
                        dispatchDeleteRegionLayer,
                        //dispatchRemoveRegionEntry,
                        dispatchSelectRegion,
                        dlRoot} from '../visualize/DrawLayerCntlr.js';
import {get, set, has, isEmpty, isString, isArray} from 'lodash';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {flux} from '../Firefly.js';

const ID= 'REGION_PLOT';
const TYPE_ID= 'REGION_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, null);
export default {factoryDef, TYPE_ID};

var idCnt=0;

function* regionsRemoveSaga({id, plotId}, dispatch, getState) {
        while (true) {
            var action = yield take([DrawLayerCntlr.REGION_REMOVE_ENTRY,
                                     DrawLayerCntlr.REGION_DELETE_LAYER,
                                     DrawLayerCntlr.DETACH_LAYER_FROM_PLOT]);

            if (action.payload.drawLayerId === id) {
                var dl;
                switch (action.type) {
                    case  DrawLayerCntlr.REGION_REMOVE_ENTRY :
                        dl = getDrawLayerById(getState()[DRAWING_LAYER_KEY], id);
                        if (dl && isEmpty(get(dl, 'drawObjAry'))) {
                            dispatchDeleteRegionLayer(id, plotId);
                        }
                        break;
                    case DrawLayerCntlr.REGION_DELETE_LAYER:
                    case DrawLayerCntlr.DETACH_LAYER_FROM_PLOT:
                        return;
                        break;
                }
            }
        }
}

/**
 * create region plot layer
 * @param initPayload in region plot layer, attribute regionAry: region description array
 *                                 dataFrom: regionAry is from 'json' object or 'ds9' string
 *                                 regions:   array of region object constructed by parsing regionAry
 *                                 regionObjAry: array of drawing object constructed from regions
 *                                 => regions and regionObjAry are updated as adding or removing regions occurs
 *                                 highlightedRegion: selected region
 * @return {DrawLayer}
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
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    var actionTypes = [DrawLayerCntlr.REGION_ADD_ENTRY,
                       DrawLayerCntlr.REGION_REMOVE_ENTRY,
                       DrawLayerCntlr.REGION_SELECT];

    const id = get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    var   dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', 'Region Plot'),
                                      options, drawingDef, actionTypes, pairs );

    dl.regionAry = get(initPayload, 'regionAry', null);
    dl.dataFrom = get(initPayload, 'dataFrom', 'ds9');
    dl.highlightedRegion = get(initPayload, 'highlightedRegion', null);
    dl.selectMode = get(initPayload, 'selectMode');

    dispatchAddSaga(regionsRemoveSaga, {id, plotId: get(initPayload, 'plotId')});
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
                    dispatchSelectRegion(dl.drawLayerId, closestObj);
                } else if (closestObj) {
                    dispatchSelectRegion(dl.drawLayerId, null);
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
    const {regionChanges, drawLayerId } = action.payload;

    if (drawLayerId && drawLayerId !== drawLayer.drawLayerId) return null;
    var dd = Object.assign({}, drawLayer.drawData);

    var deHighlight = (obj) => {
        //obj.highlight = 0;
        obj.isRendered = 1;
    };

    //  re-render data in case  border 'replace' style is used for region selected
    var reDrawData = (hiRegion) => {
        if (has(drawLayer, 'selectMode.selectStyle') && drawLayer.selectMode.selectStyle.includes('Replace')) {
            if (hiRegion) {
                hiRegion.isRendered = 0;  // de-render the selected region
            }

            drawLayer.drawObjAry = drawLayer.drawObjAry.slice();
            Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                set(dd[DataTypes.DATA], plotId, null);
            });
        }
     };

    switch (action.type) {
        case DrawLayerCntlr.REGION_SELECT:
            const {selectedRegion} = action.payload;

            Object.keys(dd[DataTypes.HIGHLIGHT_DATA]).forEach((plotId) => {
                set(dd[DataTypes.HIGHLIGHT_DATA], plotId, null);
            });

            var highlightedRegion = null;
            var selectRegionDesc = null;

            // no region is selected
            if (isEmpty(selectedRegion)) {        // nothing is selected, empty string, empty array or null
                if (drawLayer.highlightedRegion) {
                    deHighlight(drawLayer.highlightedRegion); // de-highlight the highlighted region if there is
                    drawLayer.highlightedRegion = null;
                    reDrawData();
                }
            } else {
                // test if selected region is string or array of string description
                if (isString(selectedRegion) || isArray(selectedRegion)) {    // region description in string or array of string
                    highlightedRegion = getSelectedRegionDrawObj(drawLayer, selectedRegion);
                    highlightedRegion = isEmpty(highlightedRegion) ? null : highlightedRegion[0];
                } else {    // a region drawObj
                    highlightedRegion = selectedRegion;
                }

                // selected region is valid
                if (highlightedRegion) {
                    if (drawLayer.highlightedRegion) {
                        deHighlight(drawLayer.highlightedRegion); // de-highlight the highlighted region if there is
                    }

                    reDrawData(highlightedRegion);

                    //highlightedRegion.highlight = 1;
                    selectRegionDesc = get(highlightedRegion, 'region.desc', null);
                }
            }
            return Object.assign({}, {highlightedRegion}, {drawData: dd},
                                     {selectRegionDesc});

        case DrawLayerCntlr.REGION_ADD_ENTRY:
            if (regionChanges) {
                var {layerTitle} = action.payload;

                if (layerTitle) {
                    drawLayer.title = layerTitle.slice();  // update title of the layer
                }
                addRegionsToData(drawLayer, regionChanges);

                Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                    set(dd[DataTypes.DATA], plotId, drawLayer.drawObjAry);
                });

            }
            return {drawData: dd};

        case DrawLayerCntlr.REGION_REMOVE_ENTRY:
            if (regionChanges) {
                removeRegionsFromData(drawLayer, regionChanges);

                Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                    set(dd[DataTypes.DATA], plotId, drawLayer.drawObjAry);
                });

            }

            return {drawData: dd};

        default:
            return null;
    }
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {highlightedRegion, drawObjAry, selectMode} = drawLayer;

    switch (dataType) {
        case DataTypes.DATA:    // based on the same drawObjAry to draw the region on each plot
            return isEmpty(lastDataRet) ? drawObjAry || plotAllRegions(drawLayer) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:      // create the region drawObj based on the original region for upright case.
            return isEmpty(lastDataRet) ?
                   plotHighlightRegion(highlightedRegion, plotId, selectMode) : lastDataRet;
    }
    return null;
}
/**
 * @summary create DrawingObj for all regions
 * @param {Object} dl    drawing layer
 * @returns {Object[]}
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
 * @summary create DrawingObj for highlighted region
 * @param {Object} highlightedObj
 * @param {string} plotId
 * @param {Object} selectMode
 * @returns {Object[]}
 */
function plotHighlightRegion(highlightedObj, plotId, selectMode) {
    if (!highlightedObj) {
        return [];
    }

    if (highlightedObj.region) highlightedObj.region.highlighted = 1;
    var hObj = [DrawOp.makeHighlight(highlightedObj, primePlot(visRoot(), plotId), selectMode)];

    hObj.forEach((oneObj) => {
        oneObj.highlight = 1;
    });

    return hObj;
}

/**
 * @summary add new DrawingObj into originally displayed DrawingObj set
 * @param {Object} drawLayer
 * @param {string|string[]} addedRegions
 * @returns {Object[]}
 */
function addRegionsToData(drawLayer, addedRegions) {
    var {regions, drawObjAry: lastData} = drawLayer;
    var resultRegions = regions ? regions.slice() : [];
    var allDrawobjs = lastData ? lastData.slice() : [];

    if (!isEmpty(addedRegions)) {
        var allRegions = isString(addedRegions) ? [addedRegions] : addedRegions;
        var rgObj = RegionFactory.parseRegionDS9(allRegions);

        resultRegions = rgObj.reduce ( (prev, aRegion) => {
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
 * @param {Object} drawLayer
 * @param {string|string[]} removedRegions
 * @returns {Object[]}
 */
function removeRegionsFromData(drawLayer, removedRegions) {
    var {regions, drawObjAry: lastData} = drawLayer;
    var resultRegions = regions ? regions.slice() : [];
    var allDrawObjs = lastData ? lastData.slice() : [];

    if (resultRegions.length === 0) {
        return [];     // no region to be removed
    }

    if (!isEmpty(removedRegions)) {
        var allRegions = isString(removedRegions) ? [removedRegions] : removedRegions;
        var rgObj = RegionFactory.parseRegionDS9(allRegions);

        resultRegions = rgObj.reduce((prev, rmRegion) => {
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

/**
 * @summary find the region drawObj based on region description
 * @param {object} drawLayer
 * @param {string|string[]} regionDes
 * @param {int} stopIndex maximum number of regions to be selected
 * @return {Object[]} if no region is found, an empty array is return.
 */
function getSelectedRegionDrawObj(drawLayer, regionDes, stopIndex = 1) {
    var {regions, drawObjAry} = drawLayer;

    var regs = RegionFactory.parseRegionDS9((isString(regionDes) ? [regionDes] : regionDes),
                                            true, stopIndex);
    var selDrawObj = [];

    if (!isEmpty(regs)) {
        selDrawObj = regs.reduce((prev, aRegion, index) => {
            if (index < stopIndex) {
                var idx = getRegionIndex(regions, aRegion);

                if (idx >= 0) {
                    prev.push(drawObjAry[idx]);
                }
            }
            return prev;
        }, []);
    }
    return selDrawObj.slice(0, stopIndex);
}

/**
 * @summary get the region description of the selected region from the specified drawing layer
 * @param {string} drawLayerId id of the drawing layer
 * @return {string} description of the selected region
 * @public
 * @function getSelectedRegion
 * @memberof firefly.util.image
 */
export function getSelectedRegion(drawLayerId) {
    var drawLayer = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], drawLayerId);

    return get(drawLayer, 'selectRegionDesc', '');
}