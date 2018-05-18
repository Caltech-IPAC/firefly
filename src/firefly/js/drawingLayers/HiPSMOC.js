/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import DrawLayerCntlr, {dispatchSelectRegion,
                        dlRoot} from '../visualize/DrawLayerCntlr.js';
import {get, set, isEmpty, isNil} from 'lodash';
import MocObj, {getMocCell} from '../visualize/draw/MocObj.js';
import {RegionSelStyle, defaultRegionSelectStyle} from '../visualize/DrawLayerCntlr.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {getUIComponent} from './HiPSMOCUI.jsx';

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
    const pairs = {
        [MouseState.DOWN.key]: highlightChange
    };

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
                                        options, drawingDef, actionTypes, pairs );

    dl.moc_nuniq_nums = initPayload.moc_nuniq_nums || [];
    dl.highlightedCell = get(initPayload, 'highlightedCell', null);
    dl.selectMode = get(initPayload, 'selectMode', {[RegionSelStyle]: 'DottedOverlay' });
    dl.fromPlot = get(initPayload, 'fromPlot');

    return dl;
}

/**
 * find the drawObj which is selected for highlight
 * @param mouseStatePayload
 * @returns {Function}
 */
function highlightChange(mouseStatePayload) {
    const {drawLayer,plotId,screenPt} = mouseStatePayload;
    let done = false;
    let closestInfo = null;
    let closestObjId = -1;
    const {data} = drawLayer.drawData;
    const plot = primePlot(visRoot(), plotId);
    const dataPlot = get(data, plotId);


    function* getDrawObjIndex() {
        let index = 0;
        const dataPlot = get(data, plotId);

        while (index < dataPlot.length) {
            yield index++;
        }
    }
    const gen = getDrawObjIndex();

    const sId = window.setInterval( () => {
        if (done) {
            window.clearInterval(sId);

            // set the highlight region on current drawLayer,
            // unset the highlight on other drawLayer if a highlight is found for current layer

            dlRoot().drawLayerAry.forEach( (dl) => {
                if (dl.drawLayerId === drawLayer.drawLayerId) {
                    dispatchSelectRegion(dl.drawLayerId, closestObjId >= 0 ? dl.moc_nuniq_nums[closestObjId] : null);
                }
            });
        }

        for (let i = 0; i < dataPlot.length + 1; i++ ) {
            const nextId = gen.next().value;
            const dObj = dataPlot[nextId];

            if (!isNil(nextId)) {
                const distInfo = DrawOp.isScreenPointInside(screenPt, dObj, plot);
                if (distInfo.inside) {
                   if (!closestInfo || closestInfo.dist > distInfo.dist) {
                       closestInfo = distInfo;
                       closestObjId = nextId;
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
    const {drawLayerId} = action.payload;

    if (drawLayerId && drawLayerId !== drawLayer.drawLayerId) return null;
    const dd = Object.assign({}, drawLayer.drawData);

    const reDrawData = () => {    // redraw all cells
            Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                set(dd[DataTypes.DATA], plotId, null);
            });
        };

    const setIsRendered = (obj, isRendered = 1) => {   // isRendered indicates if draw the obj or not while rendering
        obj.isRendered = isRendered;
    };

    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!drawLayer.mocObj && drawLayer.fromPlot) {
                const mocObj = createMocObj(drawLayer, drawLayer.fromPlot.plotId);

                return {mocObj};
            }
            break;
        case DrawLayerCntlr.REGION_SELECT:
            const {selectedRegion: highlightedCell} = action.payload;
            const style = get(drawLayer.selectMode, RegionSelStyle, defaultRegionSelectStyle).toLowerCase();
            let hlObj = null;

            Object.keys(dd[DataTypes.HIGHLIGHT_DATA]).forEach((plotId) => {   // reset all highlight
                set(dd[DataTypes.HIGHLIGHT_DATA], plotId, null);
            });


            if (highlightedCell !== drawLayer.highlightedCell) {    // de-highlight or highlight a new one
                if (highlightedCell) {
                    hlObj = getCellDrawObj(drawLayer, highlightedCell);

                    // render the highlight obj instead of the data obj if the style is with 'replace'
                    hlObj && style.includes('replace') && setIsRendered(hlObj, 0);
                }
                if (drawLayer.highlightedObj) {
                    setIsRendered(drawLayer.highlightedObj);
                }
                reDrawData();
                return Object.assign({}, {highlightedObj: hlObj, highlightedCell}, {drawData: dd});

            } else {
                return null;
            }
            break;
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {fillStyle, showLabel} = action.payload.changes;
            if (fillStyle) {
                const newDrawingDef = Object.assign({}, drawLayer.drawingDef,
                    {style: fillStyle.includes('outline') ? Style.STANDARD : Style.FILL});

                return {drawingDef: newDrawingDef};
            } else {
                reDrawData();

                return Object.assign({}, {showLabel, drawData: dd});
            }
        default:
            return null;
    }
    return null;
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {selectMode, highlightedCell} = drawLayer;

    switch (dataType) {
        case DataTypes.DATA:    // based on the same drawObjAry to draw the region on each plot
            return isEmpty(lastDataRet) ? createObjsOfMoc(drawLayer, plotId) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:      // create the region drawObj based on the original region for upright case.
            return isEmpty(lastDataRet) ?
                   plotHighlightRegion(highlightedCell, drawLayer, plotId, selectMode) : lastDataRet;
    }
    return null;
}

/**
 * @summary create DrawingObj for highlighted region
 * @param {Object} highlightedCell
 * @param {Object} dl
 * @param {string} plotId
 * @param {Object} selectMode
 * @returns {Object[]}
 */
function plotHighlightRegion(highlightedCell, dl, plotId, selectMode) {
    if (!highlightedCell) {
        return [];
    }
    let highlightedObj = getCellDrawObj(dl, highlightedCell);

    if (highlightedObj) {
        const {showLabel} = dl;

        highlightedObj = Object.assign({}, highlightedObj, {style: Style.STANDARD,
                                                            text: showLabel ? highlightedObj.text : ''});
    }
    return highlightedObj ? [DrawOp.makeHighlight(highlightedObj, primePlot(visRoot(), plotId), selectMode)] : [];
}

function getCellDrawObj(dl, cellNum) {
    const idx = getNuniqIndex(cellNum, dl);
    const mocObj = get(dl, 'mocObj');

    return mocObj ? getMocCell(mocObj, idx) : null;
}

/**
 * find the index of the nuniq number in the nuniq number list for the layer
 * @param nuniq
 * @param dl
 * @returns {number|Promise.<number>}
 */
function getNuniqIndex(nuniq, dl) {
    return get(dl, 'moc_nuniq_nums', []).findIndex((n) => n === nuniq);
}

/**
 * create MocObj base on cell nuniq numbers and the coordinate systems
 * @param dl
 * @param plotId
 * @returns {Object}
 */
function createMocObj(dl, plotId) {
    const {moc_nuniq_nums = [], mocObj} = dl;
    const pv = primePlot(visRoot(), plotId);
    const coordsys = pv ? pv.dataCoordSys : CoordinateSys.EQ_J2000;
    return  mocObj ? mocObj : MocObj.make(moc_nuniq_nums, coordsys,
                                          {});
}

/**
 * get polygon DrawObj for MOC cells
 * @param dl
 * @param plotId
 * @returns {null}
 */
function createObjsOfMoc(dl, plotId) {
    const mocObj = createMocObj(dl, plotId);
    const drawObjAry = mocObj&&mocObj.drawObjAry ? mocObj.drawObjAry : null;
    const {showLabel} = dl;

    return !drawObjAry ? drawObjAry
                       : drawObjAry.map((oneObj) => {
                                return Object.assign({}, oneObj, {text: showLabel ? oneObj.text : ''});
                            });
}