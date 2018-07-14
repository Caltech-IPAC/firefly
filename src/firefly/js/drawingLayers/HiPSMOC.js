/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {get, set, isEmpty, cloneDeep, clone} from 'lodash';
import MocObj, {createDrawObjsInMoc, getMaxDisplayOrder, setMocDisplayOrder} from '../visualize/draw/MocObj.js';
import {getUIComponent} from './HiPSMOCUI.jsx';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';

const ID= 'MOC_PLOT';
const TYPE_ID= 'MOC_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, getUIComponent);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = ['green', 'cyan', 'magenta', 'orange', 'lime', 'red', 'blue', 'yellow'];
const colorN = colorList.length;

/**
 * create region plot layer
 * @param initPayload moc_nuniq_nums, highlightedCell, selectMode
 * @return {DrawLayer}
 */
function creator(initPayload) {

    const drawingDef= makeDrawingDef(colorList[idCnt%colorN], {style: Style.STANDARD});
    drawingDef.textLoc = TextLocation.CENTER;


    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    const actionTypes = [DrawLayerCntlr.REGION_SELECT];
    const id = get(initPayload, 'tbl_id') || get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', 'MOC Plot - '+id.replace('_moc', '')),
                                        options, drawingDef, actionTypes);
    dl.moc_nuniq_nums = initPayload.moc_nuniq_nums || [];
    dl.fromPlotId = get(initPayload, 'fromPlotId');

    return dl;
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

    let pId;
    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!drawLayer.mocObj && drawLayer.fromPlotId) {
                const mocObj = createMocObj(drawLayer, drawLayer.fromPlotId);

                return {mocObj};
            }
            break;

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {fillStyle, targetPlotId} = action.payload.changes;

            if (fillStyle && targetPlotId) {
                const {mocStyle={}} = drawLayer;
                const style = fillStyle.includes('outline') ? Style.STANDARD : Style.FILL;
                const mocObj = get(dd, [DataTypes.DATA, targetPlotId, 0]);
                const newMocObj = mocObj ? Object.assign({}, mocObj, {style}) : null;

                set(dd, [DataTypes.DATA, targetPlotId], [newMocObj]);
                set(mocStyle, [targetPlotId], style);
                return Object.assign({}, {mocStyle, drawData: dd});
            }
            break;

        case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
        case ImagePlotCntlr.ANY_REPLOT:
            pId = plotIdAry ? plotIdAry[0] : plotId;
            const {visiblePlotIdAry=[]} = drawLayer;

            if (pId && visiblePlotIdAry.includes(pId)) {
                set(dd[DataTypes.DATA], [pId], null);
                return Object.assign({}, {drawData: dd});
            }
            break;
        case DrawLayerCntlr.CHANGE_VISIBILITY:
            if (action.payload.visible) {
                pId = plotIdAry ? plotIdAry[0] : plotId;
                if (pId) {
                    set(dd[DataTypes.DATA], [pId], null);
                    return Object.assign({}, {drawData: dd});
                }
            }
            break;
        default:
            return null;
    }
    return null;
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {visiblePlotIdAry=[]} = drawLayer;
    const showMoc = (action.type !== DrawLayerCntlr.ATTACH_LAYER_TO_PLOT) && visiblePlotIdAry.includes(plotId);
    switch (dataType) {
        case DataTypes.DATA:    // based on the same drawObjAry to draw the region on each plot
            return (isEmpty(lastDataRet) && showMoc)
                                              ? [createMocData(drawLayer, plotId, action)] : lastDataRet;
    }
    return null;
}

/**
 * create MocObj base on cell nuniq numbers and the coordinate systems
 * @param dl
 * @returns {Object}
 */
function createMocObj(dl) {
    const {moc_nuniq_nums = [], mocObj, drawingDef} = dl;

    return mocObj ? cloneDeep(mocObj) : MocObj.make(moc_nuniq_nums, drawingDef);
}


function createMocData(dl, plotId) {
    const {moc_nuniq_nums = [], mocObj, mocStyle} = dl;
    const plot = primePlot(visRoot(), plotId);
    const {mocGroup} = mocObj || {};
    let   newMocObj =  mocObj ? clone(mocObj) : MocObj.make(moc_nuniq_nums, {}, plot);  // keep original mocGroup
    const {minOrder=0, maxOrder=0} = mocGroup || {};
    const {displayOrder, hipsOrderLevel} = getMaxDisplayOrder(minOrder, maxOrder, plot);

    newMocObj = setMocDisplayOrder(newMocObj, plot, displayOrder, hipsOrderLevel);
    newMocObj.style =  get(mocStyle, plotId, Style.STANDARD);
    createDrawObjsInMoc(newMocObj, plot);
    return newMocObj;
}