/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, has, isEmpty, isString,  isUndefined, pickBy} from 'lodash';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {getSelectedPts} from '../visualize/WebPlotAnalysis';
import {primePlot, getAllDrawLayersForPlot} from '../visualize/PlotViewUtil.js';
import DrawLayerCntlr, {RegionSelStyle, dlRoot, dispatchSelectRegion} from '../visualize/DrawLayerCntlr.js';
import {clone} from '../util/WebUtil.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {convertConnectedObjsToDrawObjs, getImageCoordsOnFootprint, drawHighlightFootprintObj} from '../visualize/draw/ImageLineBasedObj.js';
import {getUIComponent} from './ImageLineFootPrintUI.jsx';
import {rateOpacity} from '../util/Color.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import {dispatchTableHighlight,  dispatchTableSelect, dispatchTableFilter} from '../tables/TablesCntlr.js';
import {getTblById} from '../tables/TableUtil.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {getSelectedShape} from './Catalog.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {detachSelectArea} from '../visualize/ui/SelectAreaDropDownView.jsx';
import {FilterInfo} from '../tables/FilterInfo.js';

const ID= 'ImageLineBasedFP_PLOT';
const TYPE_ID= 'ImageLineBasedFP_PLOT_TYPE';
const factoryDef= makeFactoryDef(TYPE_ID, creator, getDrawData, getLayerChanges, null, getUIComponent, null);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = ['rgba(74, 144, 226, 1.0)', 'blue', 'cyan', 'green', 'magenta', 'orange', 'lime', 'red',  'yellow'];
const colorN = colorList.length;


/**
 * create region plot layer
 * @param initPayload moc_nuniq_nums, highlightedCell, selectMode
 * @return {DrawLayer}
 */
function creator(initPayload) {

    const {selectInfo, highlightedRow, tbl_id, tableRequest} = initPayload;
    const {color=colorList[idCnt%colorN], style=Style.FILL, selectColor='yellow', highlightColor='orange',
           showText=false} = initPayload;
    const drawingDef= makeDrawingDef(color,
                                     {style,
                                      showText,
                                      canUseOptimization: true,
                                      textLoc: TextLocation.CENTER,
                                      selectedColor: selectColor,
                                      selectColor: highlightColor,
                                      size: 3,
                                      symbol: DrawSymbol.X});

    set(drawingDef, RegionSelStyle, 'SolidReplace');

    const pairs = {
        [MouseState.DOWN.key]: highlightChange
    };

    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canFilter: !!tbl_id,
        canSelect: !!tbl_id,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true,
        isPointData:true
    };

    const actionTypes = [DrawLayerCntlr.REGION_SELECT, DrawLayerCntlr.CHANGE_DRAWING_DEF];

    const id = get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', 'Lsst footprint '+id),
                                        options, drawingDef, actionTypes, pairs);

    dl.imageLineBasedFP = get(initPayload, 'imageLineBasedFP') || {};
    Object.assign(dl, {selectInfo, highlightedRow, tbl_id, tableRequest});  // will be updated by table select, highlight, filter and sort
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
    let  index = 0;

    const {connectedObjs, pixelSys} = get(drawLayer, 'imageLineBasedFP') || {};
    const plot = primePlot(visRoot(), plotId);
    const cc = CsysConverter.make(plot);
    const tPt = getImageCoordsOnFootprint(screenPt, cc, pixelSys);
    const {tableRequest} = drawLayer;

    const sId = window.setInterval( () => {
        if (done) {
            window.clearInterval(sId);

            // set the highlight region on current drawLayer,
            // unset the highlight on other drawLayer if a highlight is found for current layer

            dlRoot().drawLayerAry.forEach( (dl) => {
                if (dl.drawLayerId === drawLayer.drawLayerId) {
                    if (drawLayer.tbl_id) {
                        if (closestObj) {
                            const highlightedRow = Number(closestObj.tableRowNum);

                            if (!isUndefined(highlightedRow) && highlightedRow >= 0) {
                                dispatchTableHighlight(drawLayer.tbl_id, highlightedRow, tableRequest);
                            }
                        }
                    } else {
                        dispatchSelectRegion(dl.drawLayerId, closestObj);
                    }
                } else if (closestObj) {
                    if (!drawLayer.tbl_id) {
                        dispatchSelectRegion(dl.drawLayerId, null);
                    }
                }
            });
        } else {
            for (let i = 0; (index < connectedObjs.length && i < maxChunk); i++) {
                const dObj = connectedObjs[index++];

                if (dObj) {
                    const distInfo = dObj.connectObj.containPoint(tPt, pixelSys, cc);

                    if (distInfo.inside) {
                        if (!closestInfo || closestInfo.dist > distInfo.dist) {
                            closestInfo = distInfo;
                            closestObj = dObj;
                        }
                    }
                }
            }
            done = (index === connectedObjs.length);
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

    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!plotIdAry && !plotId) return null;

            const pIdAry = plotIdAry ? plotIdAry :[plotId];
            const tObj = getTitle(drawLayer, pIdAry);

            return {title: tObj};

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const pId = plotId ? plotId : (plotIdAry ? plotIdAry[0]: null);
            if (!pId) return null;

            const {changes} = action.payload;
            const {selectInfo, highlightedRow,
                   tableRequest, tableData, imageLineBasedFP, tbl_id, title} = changes;
            const {style, color, showText, selectColor, highlightColor} = changes;  // for drawingdef
            const drawingDefChanges = clone(drawLayer.drawingDef);

            const changesUpdate = {};

            if (title) {
                Object.assign(changesUpdate, {title});
            }
            if (style) {
                drawingDefChanges.style = (style.toLowerCase() === 'fill') ? Style.FILL : Style.STANDARD;
            }
            Object.assign(drawingDefChanges, pickBy({showText, selectedColor:selectColor,
                                                     selectColor:highlightColor, color}));

            // TABLE_LOADED: tableRequest, tbl_id, imageLineBasedFP, title, selectInfo
            // TABLE_SELECT, TABLE_LOADED, TABLE_SORT: selectInfo
            Object.assign(changesUpdate,
                          pickBy({title, tableRequest, tbl_id, imageLineBasedFP,
                                  selectInfo}));

            if (tableData) {    // from watcher on TABLE_LOADED
                set(dd, [DataTypes.DATA, pId], null);
            }

            if (selectInfo) {   // from dispatch TableSelect, watcher on TABLE_SELECT or TABLE_LOADED
                set(dd, [DataTypes.SELECTED_IDXS], null);    // no plotId involved
            }

            if (!isUndefined(highlightedRow)) {  // from watcher TABLE_UPDATE, TABLE_HIGHLIGHT, TABLE_LOADED
                set(dd, [DataTypes.HIGHLIGHT_DATA, pId], null);
                Object.assign(changesUpdate, {highlightedRow});
            }

            return Object.assign({}, changesUpdate, {drawingDef: drawingDefChanges}, {drawData: dd});

        case DrawLayerCntlr.REGION_SELECT:
            const {selectedRegion} = action.payload;
            let   hideFPId = '';

            Object.keys(dd[DataTypes.HIGHLIGHT_DATA]).forEach((plotId) => {   // reset all highlight
                set(dd[DataTypes.HIGHLIGHT_DATA], plotId, null);              // deHighlight previous selected one
            });


            if (selectedRegion) {
                if (has(drawLayer, 'selectMode.selectStyle') && drawLayer.selectMode.selectStyle.includes('Replace')) {
                    hideFPId = selectedRegion.id;

                    Object.keys(dd[DataTypes.DATA]).forEach((plotId) => {
                        set(dd[DataTypes.DATA], plotId, null);               // will update data objs
                    });
                }
            }

            return Object.assign({}, {highlightedFootprint: selectedRegion, drawData: dd, hideFPId});

        case DrawLayerCntlr.CHANGE_DRAWING_DEF:
            const ptId = plotId ? plotId : (plotIdAry ? plotIdAry[0]: null);
            if (!ptId) return null;

            set(dd, [DataTypes.DATA, ptId], plotLayer(drawLayer, action.payload.drawingDef));
            return Object.assign({}, {drawingDef: clone(drawLayer.drawingDef, action.payload.drawingDef),
                                      drawData: dd});

        default:
            return null;
    }
}

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    const {highlightedFootprint,  tbl_id, highlightedRow, drawingDef} = drawLayer;

    switch (dataType) {
        case DataTypes.DATA:    // based on the same drawObjAry to draw the region on each plot
            return isEmpty(lastDataRet) ? plotLayer(drawLayer) : lastDataRet;

        case DataTypes.HIGHLIGHT_DATA:      // create the region drawObj based on the original region for upright case.
            if (tbl_id) {
                return isEmpty(lastDataRet) ? plotHighlightedRow(drawLayer, highlightedRow, plotId, drawingDef) : lastDataRet;
            } else {
                return isEmpty(lastDataRet) ?  plotHighlightRegion(drawLayer, highlightedFootprint, plotId, drawingDef) : lastDataRet;
            }
        case DataTypes.SELECTED_IDXS:
            if (tbl_id) {
                return isEmpty(lastDataRet) ? computeSelectedIdxAry(drawLayer) :lastDataRet;
            }
            break;
    }
    return null;
}

function computeSelectedIdxAry(dl) {
    const {tbl_id} = dl;
    const {selectInfo} = getTblById(tbl_id) || {};

    if (!selectInfo) return null;
    const si= SelectInfo.newInstance(selectInfo);
    if (!si.getSelectedCount()) return null;

    const data = get(dl, ['drawData', 'data']);
    const pId =  data && Object.keys(data).find((pId) => data[pId]);
    const footprintObjs = pId ? data[pId] : null;

    const isSelected =  (fObjs) => {
        return (idx) => {
            return fObjs && si.isSelected(Number(fObjs[idx].tableRowNum));
        };
    };

    return isSelected(footprintObjs);
}

/**
 * create DrawingObj for highlighted row
 * @param drawLayer
 * @param highlightedRow
 * @param plotId
 * @param drawingDef
 */
function plotHighlightedRow(drawLayer, highlightedRow, plotId, drawingDef) {
    if (isUndefined(highlightedRow) || highlightedRow < 0) return null;

    const {connectedObjs} = get(drawLayer, 'imageLineBasedFP') || {};
    if (connectedObjs) {
        const cObjs = connectedObjs.find((oneObj) => Number(oneObj.tableRowNum) === highlightedRow);
        if (cObjs) {
            return plotHighlightRegion(drawLayer, cObjs, plotId, drawingDef);
        }
    }
    return null;
}


/**
 * @summary create DrawingObj for highlighted region
 * @param {object} drawLayer
 * @param {Object} highlightedFootprint object for ConnectedObj
 * @param {string} plotId
 * @param {Object} drawingDef
 * @returns {Object[]}
 */
function plotHighlightRegion(drawLayer, highlightedFootprint, plotId, drawingDef) {
    if (!highlightedFootprint || !drawLayer.imageLineBasedFP) {
        return [];
    }

    return drawHighlightFootprintObj(highlightedFootprint, primePlot(visRoot(), plotId), drawingDef);
}


function plotLayer(dl, newDrawingDef) {
    const {style=Style.FILL, showText, color} = newDrawingDef || dl.drawingDef || {};
    const {imageLineBasedFP, hideFPId} = dl || {};

    if (!imageLineBasedFP || !imageLineBasedFP.connectedObjs) return null;
    return convertConnectedObjsToDrawObjs(imageLineBasedFP, style,
                                          {fill: rateOpacity(color, 0.5), outline: color, hole: rateOpacity('red', 0.5)}, showText, null, hideFPId);
}

function getLayers(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .filter( (dl) => dl.drawLayerTypeId===TYPE_ID);
}

export function selectFootprint(pv, dlAry= dlRoot().drawLayerAry) {
    if (dlAry.length === 0) return;

    const footprintAry = getLayers(pv, dlAry);
    if (footprintAry.length === 0) return;

    const p= primePlot(pv);
    const sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const selectedShape = getSelectedShape(pv, dlAry);

    footprintAry.forEach( (dl) => {
        const connectObjs = get(dl.drawData.data, [pv.plotId]);
        const selectInfoCls = SelectInfo.newInstance({rowCount:  connectObjs ? connectObjs.length : 0});
        const allCObjsIdxs = getSelectedPts(sel, p, connectObjs, selectedShape);

        allCObjsIdxs.forEach((cObjIdx) => selectInfoCls.setRowSelect(Number(connectObjs[cObjIdx].tableRowNum), true));
        dispatchTableSelect(dl.drawLayerId, selectInfoCls.data);
    });
    detachSelectArea(pv);
}


export function unselectFootprint(pv, dlAry) {
    getLayers(pv,dlAry)
        .forEach( (dl) => {
            const connectObjs = get(dl.drawData.data, [pv.plotId]);
            const selectInfoCls = SelectInfo.newInstance({rowCount: connectObjs ? connectObjs.length : 0});
            dispatchTableSelect(dl.drawLayerId, selectInfoCls.data);
        });
}

export function  filterFootprint(pv, dlAry) {
    const p = primePlot(pv);
    const sel = p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;

    const footprintAry = getLayers(pv, dlAry);
    if (footprintAry.length === 0) return;

    const selectedShape =  getSelectedShape(pv, dlAry);
    footprintAry.forEach((dl) => {
        const tbl = getTblById(dl.tbl_id);
        const filterInfo = get(tbl, 'request.filters');
        const filterInfoCls = FilterInfo.parse(filterInfo);

        const connectObjs = get(dl.drawData.data, [pv.plotId]);
        const allCObjsIdx = getSelectedPts(sel, p, connectObjs, selectedShape);
        const idxs = allCObjsIdx.map((idx) => connectObjs[idx].tableRowIdx);

        const filter= `IN (${idxs.length === 0 ? -1 : idxs.toString()})`;     //  ROW_IDX is always positive.. use -1 to force no row selected
        filterInfoCls.setFilter('ROW_IDX', filter);

        const newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
    });
    detachSelectArea(pv);
}


export function  clearFilterFootprint(pv, dlAry) {
    getLayers(pv, dlAry).forEach((dl) => {
        if (dl.tbl_id) {
            dispatchTableFilter({tbl_id: dl.tbl_id, filters: ''});
        }
    });
}