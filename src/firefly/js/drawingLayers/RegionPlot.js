/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
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
                        dispatchRemoveRegionEntry} from '../visualize/DrawLayerCntlr.js';

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
        //[MouseState.DOWN.key]: removeRegionDescription
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

    var actionTypes = [DrawLayerCntrl.REGION_ADD_ENTRY,
                       DrawLayerCntlr.REGION_REMOVE_ENTRY];

    var dl = DrawLayer.makeDrawLayer( get(initPayload, 'drawLayerId', `${ID}-${idCnt}`),
                                      TYPE_ID, get(initPayload, 'title', 'Region Plot'),
                                      options, drawingDef, actionTypes, pairs );

    dl.regionAry = get(initPayload, 'regionAry', null);
    dl.dataFrom = get(initPayload, 'dataFrom', 'ds9');

    /*
    if (regionAry) {
        dl.regions =  get(initPayload, 'dataFrom', 'ds9') === 'json' ? RegionFactory.parseRegionJson(regionAry) :
                                                                       RegionFactory.parseRegionDS9(regionAry);
    } else {
        dl.regions = null;
    }
    */
    dl.highlightedRegion = get(initPayload, 'highlightedRegion', null);

    idCnt++;
    return dl;
}

// for testing
function removeRegionDescription(mouseStatePayload) {
    //const {drawLayerId} = mouseStatePayload.drawLayer;

    const id= TYPE_ID + '-0';

    var removeRegionAry =  [
        'image;ellipse 100 100 20p 40p 30p 60p 40p 80p 20 # color=green text={ellipseannulus 2}',
        'J2000;ellipse 202.55556, 47.188286 20p 40p 0i # color=#48f text={ellipse 1} width=10',
        'image;box 130 100 20p 40p 30p 50p 70p 100p 30 # color=red text={slanted box annulus 3}'];
      dispatchRemoveRegionEntry(id, removeRegionAry);
}

// for testing
function addRegionDescription(mouseStatePayload) {
    //const {drawLayerId} = mouseStatePayload.drawLayer;

    const id= TYPE_ID + '-0';

    var regionAry =  [
        'image;ellipse 100 100 20p 40p 30p 60p 40p 80p 20 # color=green text={ellipseannulus 2}',
        'J2000;ellipse 202.55556, 47.188286 20p 40p 0i # color=#48f text={ellipse 1} width=10',
        'image;box 130 100 20p 40p 30p 50p 70p 100p 30 # color=red text={slanted box annulus 3}'];

    dispatchAddRegionEntry(id, regionAry);
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
    const {changes, regionId, regionChanges } = action.payload;
    var dd = Object.assign({}, drawLayer.drawData);

    switch (action.type) {
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            /*
            if (changes && changes.regions) {  // this should not happen
                dd[DataTypes.HIGHLIGHT_DATA] = null;
                dd[DataTypes.DATA] = null;
                if (drawLayer.highlightedRegion) drawLayer.highlightedRegion = null;
            } else */
             if (changes && changes.highlightedRegion) {
                dd[DataTypes.HIGHLIGHT_DATA] = null;
                if (drawLayer.highlightedRegion) drawLayer.highlightedRegion.highlight = 0;
                changes.highlightedRegion.highlight = 1;
            } else if (changes) {
                dd[DataTypes.HIGHLIGHT_DATA] = null;
                if (drawLayer.highlightedRegion) {
                    drawLayer.highlightedRegion.highlight = 0; // un-highlight the last one
                    drawLayer.highlightedRegion = null;
                }
            }

            return Object.assign({}, changes, {drawData: dd});
        case DrawLayerCntrl.REGION_ADD_ENTRY:
            if (regionId === drawLayer.drawLayerId && regionChanges) {
                dd[DataTypes.DATA] = addRegionsToData(drawLayer, dd[DataTypes.DATA], regionChanges);
            }
            return {drawData: dd};

        case DrawLayerCntrl.REGION_REMOVE_ENTRY:
            if (regionId === drawLayer.drawLayerId && regionChanges) {
                dd[DataTypes.DATA] = removeRegionsFromData(drawLayer, dd[DataTypes.DATA], regionChanges);
            }

            return {drawData: dd};

        default:
            return null;
    }
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {highlightedRegion, regionAry, dataFrom} = drawLayer;

    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? plotAllRegions(regionAry, dataFrom, drawLayer) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? plotHighlightRegion(highlightedRegion) : lastDataRet;
    }
    return null;
}
/**
 * create DrawingObj for all regions
 * @param regionAry array of region strings
 * @param dataFrom from 'json' (server) or 'ds9' (original ds9 description)
 * @param dl    drawing layer
 * @returns {*}
 */
function plotAllRegions(regionAry, dataFrom, dl) {
    if (!regionAry) {
        return [];
    }

    dl.regions =  (dataFrom === 'json') ? RegionFactory.parseRegionJson(regionAry) :
                                              RegionFactory.parseRegionDS9(regionAry);

    return drawRegions(dl.regions);

}

/**
 * create DrawingObj for highlighted region
 * @param highlightedObj
 * @returns {*}
 */
function plotHighlightRegion(highlightedObj) {
    if (!highlightedObj) {
        return [];
    }

    if (highlightedObj.region) highlightedObj.region.highlighted = 1;
    return [DrawOp.makeHighlight(highlightedObj, primePlot(visRoot()))];
}

/**
 * add new DrawingObj into originally displayed DrawingObj set
 * @param drawLayer
 * @param lastData
 * @param addedRegions
 * @returns {Array}
 */
function addRegionsToData(drawLayer, lastData, addedRegions) {
    var {regions} = drawLayer;
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
    return allDrawobjs;
}

/**
 * remove DrawingObj from originally displayed DrawingObj set
 * @param drawLayer
 * @param lastData
 * @param removedRegions
 * @returns {Array}
 */
function removeRegionsFromData(drawLayer, lastData, removedRegions) {
    var resultRegions;
    var allDrawObjs = lastData ? lastData.slice() : [];

    resultRegions = allDrawObjs.reduce( (prev, drawObj) => {
        prev.push(drawObj.region);
        return prev;
    }, []);

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
    return allDrawObjs;
}