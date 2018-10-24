/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get, set, has, isEmpty, isString,  isUndefined} from 'lodash';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot, getAllDrawLayersForPlot} from '../visualize/PlotViewUtil.js';
import DrawLayerCntlr, {RegionSelStyle, dlRoot, dispatchSelectRegion, dispatchModifyCustomField}
                                                                     from '../visualize/DrawLayerCntlr.js';
import {clone} from '../util/WebUtil.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import {convertConnectedObjsToDrawObjs, getImageCoordsOnFootprint, drawHighlightFootprintObj} from '../visualize/draw/ImageLineBasedObj.js';
import {getUIComponent} from './ImageLineFootPrintUI.jsx';
import {rateOpacity} from '../util/Color.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {dispatchTableHighlight,  dispatchTableSelect, dispatchTableFilter} from '../tables/TablesCntlr.js';
import {findIndex, getTblById, getCellValue, doFetchTable, getColumnIdx} from '../tables/TableUtil.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import {getSelectedShape} from './Catalog.js';
import {getSelectedPts} from '../visualize/VisUtil.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {detachSelectArea} from '../visualize/ui/SelectAreaDropDownView.jsx';
import {FilterInfo} from '../tables/FilterInfo.js';
import {cloneRequest, MAX_ROW} from '../tables/TableRequestUtil.js';

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
        canFilter: true,
        canSelect: true,
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
    Object.assign(dl, {selectInfo, highlightedRow, tbl_id, tableRequest});  // will be updated by table select, highlight, filter and sort
    dl.selectRowIdxs = {};   // map: row_idx / row_num
    return dl;
}


/**
 * get row_num based on row_idx. the method depends on if there is filtering done earlier.
 * @param tbl_id
 * @param cObj
 * @param dataList
 * @returns {*}
 */
function getHighlightedRow(tbl_id, cObj, dataList) {
    const tbl = getTblById(tbl_id);
    const {request} = tbl || {};

    const getRowNum = (dataRows, col) => {
        return dataRows.findIndex((oneData) => oneData[col] === cObj.tableRowIdx);
    };

    if (request.filters) {
        if (dataList) {
            const row_num = getRowNum(dataList, 0);
            return Promise.resolve(row_num);
        } else {
            const params = {
                startIdx: 0,
                pageSize: MAX_ROW,
                inclCols: '"ROW_IDX"'
            };
            const req = cloneRequest(tbl.request, params);

            return doFetchTable(req).then((filterTable) => {
                const {data} = get(filterTable, 'tableData') || {};

                if (data) {
                    const rowidx_col = getColumnIdx(filterTable, 'ROW_IDX');

                    if (rowidx_col >= 0) {
                        return getRowNum(data, rowidx_col);
                    }
                }
                return -1;
            });
        }
    } else {
        return findIndex(tbl_id,`ROW_IDX = ${cObj.tableRowIdx}`);
    }
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
                            getHighlightedRow(dl.tbl_id, closestObj).then((highlightedRow) => {
                            //findIndex(drawLayer.tbl_id, `ROW_IDX = ${closestObj.tableRowIdx}`).then((highlightedRow) => {
                                    if (highlightedRow >= 0) {
                                        dispatchTableHighlight(drawLayer.tbl_id, highlightedRow, tableRequest);
                                    }
                                });
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
                    const distInfo = dObj.connectObj.containPoint(tPt);

                    if (distInfo.inside) {
                        if (!closestInfo || closestInfo.dist > distInfo.dist) {
                            closestInfo = distInfo;
                            closestObj = dObj;
                            //console.log('one inside = ' + closestObj.id + ' dist: ' + closestInfo.dist);
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
            const {changes} = action.payload;
            const {fillStyle, selectInfo, highlightedRow, selectRowIdxs,
                   tableRequest, tableData, imageLineBasedFP} = changes;
            const pId = plotId ? plotId : (plotIdAry ? plotIdAry[0]: null);

            if (!pId) return null;

            if (fillStyle) {
                const style = fillStyle.includes('outline') ? Style.STANDARD : Style.FILL;
                const showText = fillStyle.includes('text');
                const drawingDef = clone(drawLayer.drawingDef, {style, showText});

                set(dd, [DataTypes.DATA, pId], null);
                return Object.assign({}, {drawingDef, drawData: dd});
            }

            const changesUpdate = {};

            if (tableData) {    // from watcher on TABLE_LOADED
                set(dd, [DataTypes.DATA, pId], null);
            }

            if (tableRequest) {  // from watcher TABLE_LOADED
                Object.assign(changesUpdate, {tableRequest});
            }


            if (imageLineBasedFP) {
                Object.assign(changesUpdate, {imageLineBasedFP});
            }


            if (selectInfo) {   // from dispatch TableSelect, watcher on TABLE_SELECT or TABLE_LOADED
                const selectInfoCls = SelectInfo.newInstance(selectInfo);
                const count = selectInfoCls.getSelectedCount();
                if (tableData && count === 0) {  // from table sort or table filter
                    Object.assign(changesUpdate, {selectRowIdxs: {}});
                } else {
                    const crtRowIdxs = updateSelectRowIdx(drawLayer, selectInfo);
                    Object.assign(changesUpdate, {selectRowIdxs: crtRowIdxs});
               }
               set(dd, [DataTypes.SELECTED_IDXS], null);    // no plotId involved

               Object.assign(changesUpdate, {selectInfo});
            }

            if (!isUndefined(highlightedRow)) {  // from watcher TABLE_UPDATE, TABLE_HIGHLIGHT, TABLE_LOADED
                set(dd, [DataTypes.HIGHLIGHT_DATA, pId], null);
                Object.assign(changesUpdate, {highlightedRow});
            }

            if (!isUndefined(selectRowIdxs)) {   // from selectFootprint
                Object.assign(changesUpdate, {selectRowIdxs});
            }

            return Object.assign({}, changesUpdate, {drawData: dd});

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
        default:
            return null;
    }
}

function updateSelectRowIdx(drawLayer, selectInfo) {
    const {tbl_id, selectRowIdxs} = drawLayer;
    const selectInfoCls = SelectInfo.newInstance(selectInfo);
    const selected = selectInfoCls.getSelected();
    const tbl = getTblById(tbl_id);
    const rowidxCol = getColumnIdx(tbl, 'ROW_IDX');
    const rownumCol = getColumnIdx(tbl, 'ROW_NUM');
    const rowidxMap = get(tbl, ['tableData', 'data']).reduce((prev, oneData) => {
        prev[oneData[rowidxCol]] = oneData[rownumCol];
        return prev;
    }, {});

    // remove de-select item from de-select
    const newRowIdxs = Object.keys(selectRowIdxs).reduce((prev, rowIdx) => {
          const row_num = has(rowidxMap, rowIdx) ? rowidxMap[rowIdx] : -1;

          if (row_num === -1 || selected.has(Number(row_num))) {
              prev[rowIdx] = row_num;
          }
          return prev;
    }, {});

    // add select row item from select
    selected.forEach((rownum) => {
        const row_idx = getCellValue(tbl, rownum, 'ROW_IDX');
        if (!has(newRowIdxs, row_idx)) {
            newRowIdxs[row_idx] = `${rownum}`;
        }
    });

    return newRowIdxs;
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
    const {tbl_id, selectRowIdxs} = dl;
    const {selectInfo} = getTblById(tbl_id) || {};
    if (!selectInfo) return null;

    const si = SelectInfo.newInstance(selectInfo);
    if (!si.getSelectedCount()) return null;

    const data = get(dl, ['drawData', 'data']);
    const pId =  data && Object.keys(data).find((pId) => data[pId]);
    const footprintObjs = pId ? data[pId] : null;
    const selRowIdxsList = Object.keys(selectRowIdxs);

    const isSelected =  (fObjs, selRowIdxs) => {
        return (idx) => {
            return fObjs && selRowIdxs && selRowIdxs.includes(fObjs[idx].tableRowIdx);
        };
    };
    return isSelected(footprintObjs, selRowIdxsList);
}


function getTableData(drawLayer) {
    const {tbl_id} = drawLayer || {};
    if (!tbl_id) return null;

    const tbl = getTblById(tbl_id);
    return tbl.tableData;
}

/**
 * create DrawingObj for highlighted row
 * @param drawLayer
 * @param highlightedRow
 * @param plotId
 * @param drawingDef
 */
function plotHighlightedRow(drawLayer, highlightedRow, plotId, drawingDef) {
    const tableData = getTableData(drawLayer);

    if (!tableData || !tableData.data || !tableData.columns) return null;

    const {columns} = tableData;
    const d = tableData.data[highlightedRow];
    if (!d) return null;


    const colIdx = columns.findIndex((c) => {
        return (c.name === 'id');
    });

    if (colIdx < 0) return null;

    const id = d[colIdx];
    const {connectedObjs} = get(drawLayer, 'imageLineBasedFP') || {};
    if (connectedObjs) {
        const footprintHighlight = connectedObjs.find((oneFootprintObj) => {
            return oneFootprintObj.id === id;
        });

        return plotHighlightRegion(drawLayer, footprintHighlight, plotId, drawingDef);
    } else {
        return null;
    }
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


function plotLayer(dl) {
    const {style=Style.FILL, showText, color} = dl.drawingDef || {};
    const {imageLineBasedFP, hideFPId} = dl || {};

    if (!imageLineBasedFP || !imageLineBasedFP.connectedObjs) return null;
    return convertConnectedObjsToDrawObjs(imageLineBasedFP, style,
                                          {fill: rateOpacity(color, 0.5), outline: color, hole: rateOpacity('red', 0.5)}, showText, null, hideFPId);
}

function getLayers(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .filter( (dl) => dl.drawLayerTypeId===TYPE_ID);
}

export function selectFootprint(pv, dlAry) {
    if (dlAry.length === 0) return;

    const footprintAry = getLayers(pv, dlAry);
    if (footprintAry.length === 0) return;

    const p= primePlot(pv);
    const sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;

    const setSelectInfo = (nums, selectInfoCls) => {
        nums.forEach((idx) => selectInfoCls.setRowSelect(idx, true));
    };

    const selectedShape = getSelectedShape(pv, dlAry);
    footprintAry.forEach( (dl, dlIndex) => {
        const connectObjs = get(dl.drawData.data, [pv.plotId]);

        const tbl = getTblById(dl.tbl_id);
        //const selectInfoCls = SelectInfo.newInstance(tbl.selectInfo);
        const dataRows = get(tbl, ['tableData', 'data', 'length'], 0);
        const selectInfoCls = SelectInfo.newInstance({rowCount: dataRows});
        const allCObjsIdxs = getSelectedPts(sel, p, connectObjs, selectedShape);
        const row_nums = [];
        const row_idxs = {}; // clone(dl.selectRowIdxs);

        if (isEmpty(allCObjsIdxs)) {    // none is slected
            setSelectInfo(row_nums, selectInfoCls);
            dispatchModifyCustomField(dl.tbl_id, {selectRowIdxs: row_idxs}, p.plotId);
            dispatchTableSelect(dl.drawLayerId, selectInfoCls.data);
            if (dlIndex === footprintAry.length - 1) {
                detachSelectArea(pv);
            }
        } else {
            const params = {
                startIdx: 0,
                pageSize: MAX_ROW,
                inclCols: '"ROW_IDX"'
            };
            const req = cloneRequest(tbl.request, params);

            // get the row_idx list first, then get the row_num from the list to form selectInfo
            doFetchTable(req).then((filterTable) => {
                const {data} = get(filterTable, 'tableData') || {};

                allCObjsIdxs.reduce((ps, cObjIdx, n) => {
                    ps = ps.then(() => {
                        const rowIdx = connectObjs[cObjIdx].tableRowIdx;

                        getHighlightedRow(dl.tbl_id, connectObjs[cObjIdx], data).then((row_num) => {
                            //findIndex(dl.tbl_id, `ROW_IDX = ${rowIdx}`).then((row_num) => {
                            if (row_num >= 0) {
                                row_nums.push(Number(row_num));
                            }
                            row_idxs[rowIdx] = row_num;

                            if (n === allCObjsIdxs.length - 1) {
                                setSelectInfo(row_nums, selectInfoCls);
                                dispatchModifyCustomField(dl.tbl_id, {selectRowIdxs: row_idxs}, p.plotId);
                                dispatchTableSelect(dl.drawLayerId, selectInfoCls.data);

                                if (dlIndex === footprintAry.length - 1) {
                                    detachSelectArea(pv);
                                }
                            }

                            return row_num;
                        });
                    });
                    return ps;
                }, Promise.resolve());
            });   // promise.then(/*for 1st obj*/).then(/* for 2nd obj */)......then(/* for last object & dispatch */)
        }
    });
}


export function unselectFootprint(pv, dlAry) {
    const p= primePlot(pv);
    getLayers(pv,dlAry)
        .forEach( (dl) => {
            const connectObjs = get(dl.drawData.data, [pv.plotId]);
            const selectInfoCls = SelectInfo.newInstance({rowCount: connectObjs ? connectObjs.length : 0});
            dispatchModifyCustomField(dl.tbl_id, {selectRowIdxs: {}}, p.plotId);
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