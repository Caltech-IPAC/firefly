/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getDrawLayerById} from '../visualize/PlotViewUtil.js';
import DrawLayerCntlr, {dispatchCreateDrawLayer, getDlAry, dispatchAttachLayerToPlot} from '../visualize/DrawLayerCntlr.js';
import {get, set, isEmpty, isString} from 'lodash';
import {clone} from '../util/WebUtil.js';
import ImageLineBasedObj, {convertConnectedObjsToRectObjs, convertConnectedObjsToPolygonObjs}
                                                                         from '../visualize/draw/ImageLineBasedObj.js';
import {getUIComponent} from './ImageLineFootPrintUI.jsx';
import {rateOpacity} from '../util/Color.js';

const ID= 'ImageLineBasedFP_PLOT';
const TYPE_ID= 'ImageLineBasedFP_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, getUIComponent, null);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = [ 'cyan', 'green', 'magenta', 'orange', 'lime', 'red', 'blue', 'yellow'];
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
                                      textLoc: TextLocation.CENTER});
    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    const id = get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', 'Lsst footprint '+id),
                                        options, drawingDef);

    dl.imageLineBasedFP = get(initPayload, 'imageLineBasedFP') || {};
    return dl;
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
        default:
            return null;
    }
    return null;
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    switch (dataType) {
        case DataTypes.DATA:    // based on the same drawObjAry to draw the region on each plot
            return isEmpty(lastDataRet) ? plotLayer(drawLayer) : lastDataRet;
    }
    return null;
}

function plotLayer(dl) {
    const {style=Style.FILL, showText} = dl.drawingDef || {};
    const {connectedObjs, pixelSys} = get(dl, 'imageLineBasedFP') || {};

    if (!connectedObjs) return null;
    const oneObjs = (style === Style.FILL) ?
                    convertConnectedObjsToRectObjs(connectedObjs, pixelSys, null, style, true) :
                    convertConnectedObjsToPolygonObjs(connectedObjs, pixelSys, Style.STANDARD, null, showText);

    const zeroObjs = convertConnectedObjsToRectObjs(connectedObjs, pixelSys, rateOpacity('red', 0.5), Style.FILL, false);
    const polyObjs = (style === Style.FILL) ?
                      convertConnectedObjsToPolygonObjs(connectedObjs, pixelSys, Style.STANDARD, 'blue', showText): [];

    return [...oneObjs,...zeroObjs,...polyObjs];
}