/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, has, isEmpty, isString} from 'lodash';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import DrawLayerCntlr, {dispatchCreateDrawLayer, getDlAry, dlRoot, dispatchAttachLayerToPlot, RegionSelStyle,
                        RegionSelColor, dispatchSelectRegion} from '../visualize/DrawLayerCntlr.js';
import {clone} from '../util/WebUtil.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import ImageLineBasedObj, {convertConnectedObjsToRectObjs, convertConnectedObjsToPolygonObjs,
                          convertConnectedObjPeaksToPointObjs} from '../visualize/draw/ImageLineBasedObj.js';
import {getUIComponent} from './ImageLineFootPrintUI.jsx';
import {rateOpacity} from '../util/Color.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {DrawSymbol} from '../visualize/draw/PointDataObj.js';


const ID= 'ImageLineBasedFP_PLOT';
const TYPE_ID= 'ImageLineBasedFP_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, getUIComponent, null);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = ['rgba(74, 144, 226, 1.0)', 'blue', 'cyan', 'green', 'magenta', 'orange', 'lime', 'red',  'yellow'];
const colorN = colorList.length;

function logError(message) {
    console.log(message);
}

export function imageLineBasedfootprintActionCreator(action) {
    return (dispatcher) => {
        const {footprintData, plotId, drawLayerId, title, style='fill', color, showText} = action.payload;

        if (!drawLayerId) {
            logError('no lsst drawlayer id specified');
        } else {
            const imageLineBasedFP = ImageLineBasedObj.make(footprintData, style);
            if (imageLineBasedFP) {
                const dl = getDrawLayerById(getDlAry(), drawLayerId);
                if (!dl) {
                    dispatchCreateDrawLayer(TYPE_ID, {
                        drawLayerId, title, imageLineBasedFP, color, showText,
                        style: (style&&(style.toLowerCase() === 'fill')) ? Style.FILL : Style.STANDARD
                    });
                }
                if (plotId) {
                    dispatchAttachLayerToPlot(drawLayerId, plotId, true);
                }
            }
        }
    };
}
/**
 * create region plot layer
 * @param initPayload moc_nuniq_nums, highlightedCell, selectMode
 * @return {DrawLayer}
 */
function creator(initPayload) {

    const drawingDef= makeDrawingDef(get(initPayload, 'color', colorList[idCnt%colorN]),
                                     {style: get(initPayload, 'style', Style.FILL),
                                      showText: get(initPayload, 'showText', false),
                                      canUseOptimization: true,
                                      textLoc: TextLocation.CENTER,
                                      size: 6,
                                      symbol: DrawSymbol.X});

    set(drawingDef, RegionSelStyle, 'SolidReplace');
    set(drawingDef, RegionSelColor, 'orange');
    const pairs = {
        [MouseState.DOWN.key]: highlightChange
    };

    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true,
        isPointData:true
    };

    const actionTypes = [DrawLayerCntlr.REGION_SELECT];

    const id = get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', 'Lsst footprint '+id),
                                        options, drawingDef, actionTypes, pairs);

    dl.imageLineBasedFP = get(initPayload, 'imageLineBasedFP') || {};
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
    const maxChunk = 500;

    const {polygonObjs=[]} = get(drawLayer, ['imageLineBasedFP']);
    const plot = primePlot(visRoot(), plotId);


    function* getDrawObj() {
        let index = 0;

        while (index < polygonObjs.length) {
            yield polygonObjs[index++];
        }
    }
    var gen = getDrawObj();

    const isInsidePolygon  = (polyObj) => {
        if (polyObj.parentConnectObj) {
            const cc = CsysConverter.make(plot);
            const tPt = cc.getZeroBasedImagePtFromInternal(cc.getImageCoords(screenPt));

            if (polyObj.parentConnectObj.containPoint(tPt)) {

                const deltaXSq = (polyObj.centerPt.x - tPt.x) ** 2;
                const  deltaYSq = (polyObj.centerPt.y - tPt.y) ** 2;
                return {inside: true, dist: Math.sqrt(deltaXSq + deltaYSq)};
            } else {
                return {inside: false};
            }
        } else {
            return DrawOp.isScreenPointInside(screenPt, polyObj, plot);
        }
    };



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
                const distInfo = isInsidePolygon(dObj);

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

function getTitle(dl, pIdAry) {
    const {drawLayerId, title} = dl;

    const tObj = isString(title) ? {} : Object.assign({}, title);
    const mTitle = 'lsst footprint ' + drawLayerId;
    pIdAry.forEach((pId) => tObj[pId] = mTitle);

    return tObj;
}


/**
 * state update on the drawlayer change
 * @param drawLayer
 * @param action
 * @returns {*}
 */
function getLayerChanges(drawLayer, action) {
    const {drawLayerId, plotId, plotIdAry} = action.payload;
    if (drawLayerId && drawLayerId !== drawLayer.drawLayerId) return null;

    const dd = Object.assign({}, drawLayer.drawData);

    console.log('action: '+action.type);

    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!plotIdAry && !plotId) return null;

            const pIdAry = plotIdAry ? plotIdAry :[plotId];
            const tObj = getTitle(drawLayer, pIdAry);

            return {title: tObj};

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {fillStyle, targetPlotId} = action.payload.changes;

            if (fillStyle && targetPlotId) {
                const style = fillStyle.includes('outline') ? Style.STANDARD : Style.FILL;
                const showText = fillStyle.includes('text');
                const drawingDef = clone(drawLayer.drawingDef, {style, showText});

                set(dd, [DataTypes.DATA, targetPlotId], null);
                return Object.assign({}, {drawingDef, drawData: dd});
            }
            break;

        case DrawLayerCntlr.REGION_SELECT:
            const {selectedRegion} = action.payload;

            Object.keys(dd[DataTypes.HIGHLIGHT_DATA]).forEach((plotId) => {   // reset all highlight
                set(dd[DataTypes.HIGHLIGHT_DATA], plotId, null);              // deHighlight previous selected one
            });

            if (drawLayer.highlightedFootprint) {
                if (!selectedRegion || selectedRegion !== drawLayer.highlightedFootprint) {
                    drawLayer.highlightedFootprint.isRendered = 1;
                }
            }

            if (selectedRegion) {
                if (has(drawLayer, 'selectMode.selectStyle') && drawLayer.selectMode.selectStyle.includes('Replace')) {
                    selectedRegion.isRendered = 0;     // not show, show highlighted one instead

                    Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                        set(dd[DataTypes.DATA], plotId, null);               // will update data objs
                    });
                }
            }

            return Object.assign({}, {highlightedFootprint: selectedRegion, drawData: dd});
        default:
            return null;
    }
    return null;
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {highlightedFootprint, drawingDef} = drawLayer;

    switch (dataType) {
        case DataTypes.DATA:    // based on the same drawObjAry to draw the region on each plot
            return isEmpty(lastDataRet) ? plotLayer(drawLayer, plotId) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:      // create the region drawObj based on the original region for upright case.
            return isEmpty(lastDataRet) ?
                plotHighlightRegion(drawLayer, highlightedFootprint, plotId, drawingDef) : lastDataRet;
    }
    return null;
}

/**
 * @summary create DrawingObj for highlighted region
 * @param {object} drawLayer
 * @param {Object} highlightedFootprint
 * @param {string} plotId
 * @param {Object} drawingDef
 * @returns {Object[]}
 */
function plotHighlightRegion(drawLayer, highlightedFootprint, plotId, drawingDef) {
    if (!highlightedFootprint || !drawLayer.imageLineBasedFP) {
        return [];
    }

    const footprintAry =  get(drawLayer, ['imageLineBasedFP', 'polygonObjs']);
    const pointAry =  get(drawLayer, ['imageLineBasedFP', 'peakPointObjs']);
    if (!footprintAry) return [];
    const plot = primePlot(visRoot(), plotId);
    const {lineWidth = 1} = drawingDef;
    const lw = lineWidth+1;

    const polyHighlight = footprintAry.filter((oneObj) => oneObj.id === highlightedFootprint.id)
                        .reduce((prev, oneFP) => {
                             const newhObj = DrawOp.makeHighlight(oneFP, plot, drawingDef);
                             newhObj.highlight = 1;
                             newhObj.text = newhObj.id;
                             newhObj.style = Style.STANDARD;
                             newhObj.lineWidth = lw;

                             prev.push(newhObj);
                             return prev;
                        }, []);
    const pointHighlight = pointAry.filter((oneObj) => oneObj.id === highlightedFootprint.id)
                                    .reduce((prev, onePoint) => {
                                    const pointObj = clone(onePoint, {symbol: (drawingDef.symbol || DrawSymbol.X)});
                                    const newhObj = DrawOp.makeHighlight(pointObj, plot, drawingDef);
                                    newhObj.highlight = 1;
                                    newhObj.lineWidth = lw;

                                    prev.push(newhObj);
                                    return prev;
                            }, []);
    return [...polyHighlight,...pointHighlight];

}

function plotLayer(dl, plotId) {
    const {style=Style.FILL, showText, color} = dl.drawingDef || {};
    const {imageLineBasedFP} = dl || {};

    if (!imageLineBasedFP || !imageLineBasedFP.connectedObjs) return null;
    const oneObjs = (style === Style.FILL) ?
                    convertConnectedObjsToRectObjs(imageLineBasedFP, true,  false, rateOpacity(color, 0.5), style) : [];
    const zeroObjs = convertConnectedObjsToRectObjs(imageLineBasedFP, false, false, rateOpacity('red', 0.5), Style.FILL);

    //for 'outline' mode and 'fill' mode, add polygon outline to 'fill' display
    const polyColor = (style === Style.FILL) ? color : null;
    const polyObjs = convertConnectedObjsToPolygonObjs(imageLineBasedFP, false, showText, polyColor, Style.STANDARD);
    const pointObjs = convertConnectedObjPeaksToPointObjs(imageLineBasedFP, false);

    const outputObjs = [...oneObjs,...zeroObjs,...polyObjs,...pointObjs];

    return outputObjs;
}
