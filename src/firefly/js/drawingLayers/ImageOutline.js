/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isArray} from 'lodash';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {primePlot, isDrawLayerVisible} from '../visualize/PlotViewUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';
import {makeDrawingDef} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {makeImagePt, makeDevicePt} from '../visualize/Point.js';
import CysConverter from '../visualize/CsysConverter';
import DrawOp from '../visualize/draw/DrawOp.js';

const ID= 'IMAGE_OUTLINE';
const TYPE_ID= 'IMAGE_OUTLINE_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,null);

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

let idCnt=0;

function creator(initPayload, presetDefaults) {

    let drawingDef= makeDrawingDef(get(initPayload, 'color', 'blue'), {lineWidth:1} );
    drawingDef= Object.assign(drawingDef,presetDefaults);

    idCnt++;

    const options= {
        hasPerPlotData:true,
        isPointData:false,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        destroyWhenAllDetached: get(initPayload, 'destroyWhenAllDetached', false)
    };

    const dl = DrawLayer.makeDrawLayer(`${ID}-${idCnt}`,TYPE_ID, get(initPayload, 'title', {}), options, drawingDef);

    dl.drawObj = get(initPayload, 'drawObj');
    dl.textLoc = get(initPayload, 'textLoc');
    dl.text = get(initPayload, 'title');
    return dl;
}


function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    if (!isDrawLayerVisible(drawLayer, plotId)) return null;
    if (dataType!==DataTypes.DATA) return null;
    // return isEmpty(lastDataRet) ? computeDrawData(plotId) : lastDataRet;
    return  computeDrawData(drawLayer, plotId);
}

function getLayerChanges(drawLayer, action) {
    switch (action.type) {
        case ImagePlotCntlr.ANY_REPLOT:
            break;
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            const {plotId}= action.payload;
            let {plotIdAry}= action.payload;
            if (!plotIdAry && !plotId) return null;
            if (!plotIdAry) plotIdAry= [plotId];

            let title;

            if (typeof drawLayer.title === 'string') {
                title = drawLayer.title;
            } else {
                title = Object.assign({}, drawLayer.title);
                plotIdAry.forEach((id) => title[id] = getTitle(id));
            }
            return {title};

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            break;
    }
    return null;
}


function getTitle(plotId) {
    const plot= primePlot(visRoot(),plotId);
    const lastTitle= plot.attributes[PlotAttribute.OUTLINEIMAGE_TITLE];
    return `${lastTitle?lastTitle:'Image'} outline`;
}

function computeDrawData(drawLayer, plotId) {
    if (!plotId) return null;
    const plot= primePlot(visRoot(),plotId);
    if (!plot) return [];

    return computeDrawobj(drawLayer, plot);
}


/**
 * create drawing object and title object based on the statical or dynamical settings.
 * drawObj could be either statically set by dl.drawObj or dynamically set by plot attribute,
 * i.e. PlotAttribute.OUTLINEIMAGE_DRAWOBJ or PlotAttribute.OUTLINEIMAGE_BOUNDS
 * title could be either statically set by dl.text or dynamically set by attribute
 * PlotAttribute.OUTLINEIMAGE_TITLE
 * text location could be either statically set by dl.textLoc or dynamically set by attribute
 * PlotAttribute.OUTLINEIMAGE_TITLELOC
 * @param dl
 * @param plot
 * @returns {*}
 */
function computeDrawobj(dl, plot) {

    let   drawObj = dl.drawObj;
    let   textLoc = dl.textLoc || plot.attributes[PlotAttribute.OUTLINEIMAGE_TITLELOC];
    const text = dl.text || plot.attributes[PlotAttribute.OUTLINEIMAGE_TITLE];
    const cc = CysConverter.make(plot);

    const drawObjTextLoc = (dObj) => {
        if (isArray(dObj)) {
            textLoc = drawObj.map((oneObj) => DrawOp.getCenterPt(oneObj))
                .reduce((prev, oneLoc) => {
                    const loc = cc.getDeviceCoords(oneLoc);
                    prev = [prev[0] + loc.x, prev[1] + loc.y];
                    return prev;
                }, [0, 0])
                .map((loc) => loc/drawObj.length);

            textLoc = cc.getWorldCoords(makeDevicePt(textLoc[0], textLoc[0]));
        } else {
            textLoc = DrawOp.getCenterPt(dObj);
        }
    };

    if (drawObj) {
        if (text && !textLoc) {
            textLoc = drawObjTextLoc(drawObj);
        }
    } else {  // dynamic get data from attribute
        if (plot.attributes[PlotAttribute.OUTLINEIMAGE_DRAWOBJ]) {
            drawObj = plot.attributes[PlotAttribute.OUTLINEIMAGE_DRAWOBJ];

            if (text && !textLoc && drawObj) {
                textLoc = drawObjTextLoc(drawObj);
            }

        } else if (plot.attributes[PlotAttribute.OUTLINEIMAGE_BOUNDS]) {
            const wpAry = plot.attributes[PlotAttribute.OUTLINEIMAGE_BOUNDS];

            if (wpAry && wpAry.length > 2) {
                drawObj = FootprintObj.make([wpAry]);
                if (text && !textLoc) {
                    const s1 = cc.getDeviceCoords(wpAry[0]);
                    const s2 = cc.getDeviceCoords(wpAry[wpAry.length / 2]);

                    if (s1 && s2) {
                        textLoc = cc.getWorldCoords(makeDevicePt((s1.x + s2.x) / 2, (s1.y + s2.y) / 2));
                    }
                }
            }
        }
    }

    if (!drawObj) return [];

    const retObj = isArray(drawObj) ? drawObj : [drawObj];
    if (text && textLoc) {
        const textObj = ShapeDataObj.makeTextWithOffset(makeImagePt(-10, -6), textLoc, text);
        retObj.push(textObj);
    }

    return retObj;
}
