/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty,isArray} from 'lodash';
import Enum from 'enum';
import {makeTableColorTitle} from '../visualize/ui/DrawLayerUIComponents';
import {getSelectedPts} from '../visualize/WebPlotAnalysis';
import {primePlot, getAllDrawLayersForPlot, getCenterOfProjection} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchUseTableAutoScroll} from '../visualize/ImagePlotCntlr.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import {DrawSymbol} from '../visualize/draw/DrawSymbol.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import {makeDrawingDef, getNextColor} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr, {dlRoot, SUBGROUP} from '../visualize/DrawLayerCntlr.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import {makeImagePt, makeWorldPt, pointEquals} from '../visualize/Point.js';
import {
    dispatchTableHighlight, dispatchTableFilter, dispatchTableSelect, dispatchTableUiUpdate
} from '../tables/TablesCntlr.js';
import {COLOR_HIGHLIGHTED_PT} from '../visualize/draw/DrawingDef.js';
import {MetaConst} from '../data/MetaConst.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {showInfoPopup} from '../ui/PopupUtil.jsx';
import {getTblById, getCellValue, getTableUiByTblId} from '../tables/TableUtil.js';
import {getUIComponent, TableSelectOptions} from './CatalogUI.jsx';
import {FilterInfo} from '../tables/FilterInfo.js';
import DrawUtil from '../visualize/draw/DrawUtil.js';
import SelectArea from './SelectArea.js';
import {detachSelectArea} from '../visualize/ui/SelectAreaDropDownView.jsx';
import {CysConverter} from '../visualize/CsysConverter.js';
import {parseObsCoreRegion} from '../util/ObsCoreSRegionParser.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj';
import {getNumFilters} from '../tables/FilterInfo';
import {SelectedShape} from './SelectedShape';


const TYPE_ID= 'CATALOG_TYPE';
/**
 * @typedef {Object} CatalogType
 * enum can be one of
 * @prop POINT
 * @prop BOX
 * @prop REGION
 * @prop ORBITAL_PATH
 * @prop POINT_IMAGE_PT
 * @type {Enum}
 */

/** @type CatalogType */
export const CatalogType = new Enum(['POINT', 'BOX', 'REGION', 'ORBITAL_PATH', 'POINT_IMAGE_PT']);
let lastProjectionCenter= undefined;


const findColIdx= (columns,colId) => columns.findIndex( (c) => c.name===colId);
const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,layerRemoved,getUIComponent);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID



//---------------------------------------------------------------------
//---------------------------------------------------------------------
//--- The following are functions are used to create and
//--- operate the drawing layer
//---------------------------------------------------------------------
//---------------------------------------------------------------------

const pointBehavior= (catalogType) =>
        Boolean(
            catalogType===CatalogType.POINT ||
            catalogType===CatalogType.ORBITAL_PATH ||
            catalogType===CatalogType.POINT_IMAGE_PT );


function creator(initPayload, presetDefaults={}) {
    const {catalogId, tableData, tableMeta, title, catalogType= CatalogType.POINT,
           selectInfo, columns, tableRequest, highlightedRow, color, angleInRadian=false,
           tableCanControlColor:inTableCanControlColor,
           symbol, size, tbl_id, dataTooBigForSelection=false, tableSelection, layersPanelLayoutId,

    }= initPayload;

    const tableCanControlColor= inTableCanControlColor ?? catalogType === CatalogType.POINT;

    const drawingDef= {...makeDrawingDef(),
            size: size || 5,
            symbol: DrawSymbol.get(symbol) || DrawSymbol.get(tableMeta?.[MetaConst.DEFAULT_SYMBOL]) || DrawSymbol.SQUARE,
        ...presetDefaults};

    const pairs= {
        [MouseState.UP.key]: highlightChange,
        [MouseState.DOWN.key]: saveLastDown
    };

    drawingDef.color= (color || tableMeta?.[MetaConst.DEFAULT_COLOR] || getNextColor());



    const helpText= `Click on ${(catalogType===CatalogType.REGION) ? 'region' : 'point'} to highlight`;
    const options= {
        catalogType,
        layersPanelLayoutId,
        hasPerPlotData:false,
        isPointData: pointBehavior(catalogType),
        canUserDelete: true,
        canUseMouse:true,
        canHighlight: true,
        canSelect: pointBehavior(catalogType),
        canShowSelect: catalogType === CatalogType.REGION,
        canFilter: pointBehavior(catalogType),
        dataTooBigForSelection,
        helpLine : helpText,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        supportSubgroups: Boolean(tableMeta && tableMeta[SUBGROUP])
    };

    const rawDl= DrawLayer.makeDrawLayer(catalogId,TYPE_ID,
                                      title || `Catalog: ${tableMeta?.title?.catalogId}`,
                                      options, drawingDef, null, pairs );

    setTimeout(() => {
        if (tbl_id && tableCanControlColor) {
            const {tbl_ui_id} = getTableUiByTblId(tbl_id) ?? {};
            if (!tbl_ui_id) return;
            dispatchTableUiUpdate({tbl_ui_id,
                title:makeTableColorTitle(drawingDef.color,rawDl.drawLayerId,undefined,tbl_id) ,
                color: drawingDef.color
            });
        }
    });

    return {...rawDl,
        catalogId,
        tableData,
        tableMeta,
        tableRequest,
        selectInfo,
        highlightedRow,
        columns,
        angleInRadian,
        tableCanControlColor,
        selectOption: tableSelection,
        tbl_id: tbl_id || catalogId,
    };
}

// eslint-disable-next-line no-unused-vars
function layerRemoved(drawLayer,action) {
}

function saveLastDown(mouseStatePayload) {
    const {plotId}= mouseStatePayload;
    const plot= primePlot(visRoot(),plotId);
    lastProjectionCenter= {center:getCenterOfProjection(plot), plotId};
}

/**
 * This function is mapped to the mouse down key
 * @param mouseStatePayload
 */
function highlightChange(mouseStatePayload) {
    const {drawLayer,plotId,screenPt}= mouseStatePayload;
    const plot= primePlot(visRoot(),plotId);
    const center= getCenterOfProjection(plot);
    if (lastProjectionCenter && (!pointEquals(center, lastProjectionCenter?.center) || lastProjectionCenter.plotId!==plotId)) return;
    lastProjectionCenter= undefined;
    makeHighlightDeferred(drawLayer,plotId,screenPt);
}


/**
 * looks for a point to highlight. Use a interval to keep from locking up the event loop
 * Ends with a dispatch call
 * @param drawLayer
 * @param plotId
 * @param screenPt
 * @returns {function()}
 */
function makeHighlightDeferred(drawLayer,plotId,screenPt) {
    let done = false;
    let idx = 0;
    const maxChunk = 1000;
    let minDist = 20;
    let {data} = drawLayer.drawData;
    const {tableRequest} = drawLayer;
    let closestIdx = -1;
    const plot = primePlot(visRoot(), plotId);

    if (drawLayer.catalogType===CatalogType.ORBITAL_PATH) {
        if (tableRequest.sortInfo || getNumFilters(tableRequest)) return () => undefined;
        data = data.filter((d) => d.type !== ShapeDataObj.SHAPE_DATA_OBJ);
    }

    const id= window.setInterval( () => {
        if (done) {
            window.clearInterval(id);
            const {tableMeta, tableData}= drawLayer;
            if (closestIdx > -1) {
                // for data representing the selected region from region data
                if (data[closestIdx].fromRow) closestIdx = data[closestIdx].fromRow;


                const vr= visRoot();
                if (vr.autoScrollToHighlightedTableRow && vr.useAutoScrollToHighlightedTableRow) {
                    dispatchUseTableAutoScroll(false);
                }
                if (tableMeta.decimate_key) {
                    const colIdx= tableData.columns.findIndex((c) => c.name==='rowidx');
                    dispatchTableHighlight(drawLayer.tbl_id,tableData.data[closestIdx][colIdx],tableRequest);
                }
                else {
                    dispatchTableHighlight(drawLayer.tbl_id,closestIdx,tableRequest);
                }
            }
        }

        let dist;

        const cc= CysConverter.make(plot);
        for(let i=0;(idx<data.length && i<maxChunk ); i++) {
            const obj= data[idx];
            if (obj) {
                dist = DrawOp.getScreenDist(obj, cc, screenPt);
                if (dist > -1 && dist < minDist) {
                    minDist = dist;
                    closestIdx= idx;
                    if (minDist === 0) break;
                }
            }
            idx++;
        }
        done= (idx===data.length) || (minDist === 0);
    },0);
    return () => window.clearInterval(id);
}




function getLayerChanges(drawLayer, action) {
    if  (action.type!==DrawLayerCntlr.MODIFY_CUSTOM_FIELD) return null;

    const dd= Object.assign({},drawLayer.drawData);
    const {changes}= action.payload;
    const {selectOption} = drawLayer;

    dd[DataTypes.HIGHLIGHT_DATA]= null;

    if (changes.tableData || changes.selectOption ||
        (changes.selectInfo && selectOption === TableSelectOptions.selected.key)) {
        dd[DataTypes.DATA]= null;
    }
    if (changes.selectInfo || changes.selectOption) dd[DataTypes.SELECTED_IDXS]= null;

    return Object.assign({}, changes,{drawData:dd});
}


/**
 * 
 * @param dataType
 * @param plotId
 * @param drawLayer
 * @param action
 * @param lastDataRet
 * @returns {*}
 */
function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    const{tableData, columns, catalogType}= drawLayer;

    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? computeDrawLayer(drawLayer, tableData, columns) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? 
                          computeHighlightLayer(drawLayer, columns) : lastDataRet;
        case DataTypes.SELECTED_IDXS:
            if (catalogType===CatalogType.POINT ||
                catalogType===CatalogType.POINT_IMAGE_PT ||
                catalogType===CatalogType.ORBITAL_PATH ||
                catalogType === CatalogType.REGION) {
                if (drawLayer.dataTooBigForSelection) return [];
                return isEmpty(lastDataRet) ? computeSelectedIdxAry(drawLayer) : lastDataRet;
            }
    }
    return null;
}


/**
 * 
 * @param drawLayer
 * @param tableData
 * @param columns
 * @returns {Array} build and return an array of PointDataObj for drawing.
 */
function computeDrawLayer(drawLayer, tableData, columns) {
    let objs = null;
    const {selectOption, catalogType} = drawLayer;

    switch (catalogType) {
        case CatalogType.REGION:
            if (!selectOption || selectOption !== TableSelectOptions.highlighted.key) {
                objs = (!selectOption || selectOption === TableSelectOptions.all.key) ?
                    computeRegionLayer(drawLayer, tableData, columns) :
                    computeRegionSelected(drawLayer, tableData, columns);
            } else {
                objs = [];
            }
            break;
        case CatalogType.BOX:
            objs = computeBoxDrawLayer(drawLayer, tableData, columns);
            break;
        case CatalogType.POINT:
        case CatalogType.POINT_IMAGE_PT:
        case CatalogType.ORBITAL_PATH:
            objs = computePointDrawLayer(drawLayer, tableData, columns);
            break;
    }

    return objs;
}



function computePointDrawLayer(drawLayer, tableData, columns) {

    const {catalogType}= drawLayer;
    const useImagePts= catalogType===CatalogType.POINT_IMAGE_PT;
    const lonIdx= findColIdx(tableData.columns, useImagePts ? columns.xCol : columns.lonCol);
    const latIdx= findColIdx(tableData.columns, useImagePts ? columns.yCol : columns.latCol);
    const {angleInRadian}= drawLayer;
    if (lonIdx<0 || latIdx<0) return null;
    const dataArray= tableData?.data ?? [];


    const makeDrawPoint= (d) => {
        const lonVal= lonIdx!==latIdx ? d[lonIdx] : d[lonIdx][0];
        const latVal= lonIdx!==latIdx ? d[latIdx] : d[latIdx][1];
        return useImagePts ?
            makeImagePt(lonVal,latVal) :
            makeWorldPt(lonVal,latVal,columns.csys,true, angleInRadian);
    };
    let drawData= [];


    if (catalogType===CatalogType.ORBITAL_PATH) {
        const {tableRequest} = drawLayer;
        if (!tableRequest.sortInfo && !getNumFilters(tableRequest)) {
            drawData= dataArray.map( (d) => PointDataObj.make(makeDrawPoint(d),3, DrawSymbol.DOT));
        }
        let lastWp;
         const orbitalData= dataArray
            .map( (d) => {
                const wp= makeDrawPoint(d);
                let line;
                if (lastWp && Math.abs(lastWp.x-wp.x)<80 && !useImagePts) line= ShapeDataObj.makeLine(wp, lastWp);
                lastWp= wp;
                return line;
            })
            .filter( (l) => l);
        drawData= [...drawData, ...orbitalData];
    }
    else {
        drawData= dataArray.map( (d) => PointDataObj.make(makeDrawPoint(d)));
    }
    
    return drawData;
}



function computeBoxDrawLayer(drawLayer, tableData, columns) {
    const {angleInRadian=false}= drawLayer;

    const drawData= (tableData?.data ?? []).map( (d) => {
        if (!isArray(columns)) return;
        const fp= columns.map( (c) => {
            const lonIdx= findColIdx(tableData.columns, c.lonCol);
            const latIdx= findColIdx(tableData.columns, c.latCol);
            return makeWorldPt(d[lonIdx],d[latIdx],c.csys,true,angleInRadian);
        });
        return FootprintObj.make([fp]);
    }).filter( (fp) => fp);
    return drawData;
}

function computeRegionLayer(drawLayer, tableData, regionCols) {
    const regionColAry = isArray(regionCols) ? regionCols : [regionCols];

    const drawData= regionColAry.reduce((prev, oneRegionCol) => {
        const {unit='deg'} = oneRegionCol;

        const colObjs = (tableData?.data ?? []).map((oneRow) => {
            const regionInfo = parseObsCoreRegion(oneRow[oneRegionCol.regionIdx], unit);

            return regionInfo.valid ? regionInfo.drawObj : undefined;
        });
        prev.push(...colObjs);
        return prev;
    }, []);
    return drawData;
}

function computeRegionSelected(drawLayer, tableData, regionCols) {
    const regionColAry = isArray(regionCols) ? regionCols : [regionCols];
    const {selectedColor} = drawLayer.drawingDef;
    const {selectInfo} = drawLayer;

    const si= SelectInfo.newInstance(selectInfo);
    if (!si.getSelectedCount()) return [];
    const idxAry = [...si.getSelected()];

    const selectedData =  regionColAry.reduce((prev, oneRegionCol) => {
        const {unit='deg'} = oneRegionCol;
        const colObjs = idxAry.map((idx) => {
            const row = tableData.data[idx];

            const objInfo = parseObsCoreRegion(row[oneRegionCol.regionIdx], unit);
            const obj = objInfo.valid ? objInfo.drawObj : undefined;
            if (obj) {
                obj.color = selectedColor;
                obj.fromRow = idx;
            }

            return obj;

        });

        prev.push(...colObjs);
        return prev;
    }, []);

    return selectedData;
}

function computeHighlightLayer(drawLayer, columns) {
    let objs = null;

    switch (drawLayer.catalogType) {
        case CatalogType.REGION:
            objs = computeRegionHighlightLayer(drawLayer, columns);
            break;
        case CatalogType.BOX:
            objs = computeBoxHighlightLayer(drawLayer, columns, drawLayer.highlightedRow);
            break;
        case CatalogType.POINT:
        case CatalogType.POINT_IMAGE_PT:
        case CatalogType.ORBITAL_PATH:
            objs = computePointHighlightLayer(drawLayer, columns);
            break;
    }
    return objs;
}


/**
 *
 * @param drawLayer
 * @param columns
 * @returns {Object[]} return a array of PointDataObj that represents the highlighted object
 */
function computePointHighlightLayer(drawLayer, columns) {

    const {angleInRadian=false, catalogType}= drawLayer;
    const tbl= getTblById(drawLayer.tbl_id);
    if (!tbl) return undefined;

    if (catalogType===CatalogType.ORBITAL_PATH) {
        const {tableRequest} = drawLayer;
        if (tableRequest.sortInfo || getNumFilters(tableRequest)) return undefined;
    }


    const useImagePts= catalogType===CatalogType.POINT_IMAGE_PT;
    let ra, dec;
    if (!useImagePts && columns.lonCol===columns.latCol) {
        const valueAry= getCellValue(tbl,drawLayer.highlightedRow, columns.lonCol);
        ra= valueAry[0];
        dec= valueAry[1];
    }
    else {
        ra= getCellValue(tbl,drawLayer.highlightedRow, useImagePts ? columns.xCol :columns.lonCol);
        dec= getCellValue(tbl,drawLayer.highlightedRow, useImagePts ? columns.yCol :columns.latCol);
    }
    if (!ra || !dec) return null;

    const pt= useImagePts ? makeImagePt(ra,dec) : makeWorldPt( ra, dec, columns.csys, true, angleInRadian);
    const s = drawLayer.drawingDef.size || 5;
    const s2 = DrawUtil.getSymbolSizeBasedOn(DrawSymbol.X, Object.assign({}, drawLayer.drawingDef, {size: s}));
    const obj= PointDataObj.make(pt, s, drawLayer.drawingDef.symbol);
    const obj2= PointDataObj.make(pt, s2, DrawSymbol.X);
    obj.color= COLOR_HIGHLIGHTED_PT;
    obj2.color= COLOR_HIGHLIGHTED_PT;
    return [obj,obj2];
}

function computeBoxHighlightLayer(drawLayer, columns, highlightedRow) {
    const {tableData, angleInRadian=false}= drawLayer;
    const d= tableData.data[highlightedRow];
    if (!d || !isArray(columns)) return null;
    const fp= columns.map( (c) => {
        const lonIdx= findColIdx(tableData.columns, c.lonCol);
        const latIdx= findColIdx(tableData.columns, c.latCol);
        return makeWorldPt( d[lonIdx], d[latIdx], c.csys, true, angleInRadian);
    });
    const fpObj= FootprintObj.make([fp]);
    fpObj.lineWidth= 3;
    fpObj.color= COLOR_HIGHLIGHTED_PT;
    return [fpObj];
}


function computeRegionHighlightLayer(drawLayer, columns) {
    const {tableData}= drawLayer;
    const d= tableData?.data[drawLayer?.highlightedRow];
    if (!d) return null;

    const regionColAry = isArray(columns) ? columns : [columns];

    return regionColAry.reduce((prev, oneRegionCol) => {
        const {unit='deg'} = oneRegionCol;
        const fpObjInfo = parseObsCoreRegion(d[oneRegionCol.regionIdx], unit);
        const fpObj = fpObjInfo.valid ? fpObjInfo.drawObj : undefined;

        if (!isEmpty(fpObj)) {
            fpObj.lineWidth = 3;
            fpObj.color= COLOR_HIGHLIGHTED_PT;
        }
        prev.push(fpObj);
        return prev;
    }, []);
}


function computeSelectedIdxAry(drawLayer) {
    const {selectInfo} = drawLayer;
    const {selectOption} = drawLayer;
    if (!selectInfo || (selectOption && selectOption === TableSelectOptions.highlighted.key)) {
        return null;
    }
    const si= SelectInfo.newInstance(selectInfo);
    if (!si.getSelectedCount()) return null;
    return (idx) => si.isSelected(idx);
}


//---------------------------------------------------------------------
//---------------------------------------------------------------------
//--- The following are utility functions that that are called
//--- by the toolbar buttons to filter, select, unfilter, and unselect
//--- The are not used directly by the Catalog drawing layer but
//--- are aware of the data structure
//---------------------------------------------------------------------
//---------------------------------------------------------------------


export function selectCatalog(pv, dlAry= dlRoot().drawLayerAry) {
    const p= primePlot(pv);
    const sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const catDlAry= getLayers(pv,dlAry);
    const selectedShape = getSelectedShape(pv);
    if (catDlAry.length) {
        const tooBig= catDlAry.some( (dl) => dl.canSelect && dl.dataTooBigForSelection);
        if (tooBig) {
            showInfoPopup('Your data set is too large to select. You must filter it down first.', `Can't Select`);
        }
        else {
            catDlAry.forEach( (dl) => {
                if (dl.canSelect) {
                    const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
                    getSelectedPts(sel, p, dl.drawData.data, selectedShape)
                        .forEach((idx) => selectInfoCls.setRowSelect(idx, true));
                    dispatchTableSelect(dl.tbl_id, selectInfoCls.data);
                }
            });
            detachSelectArea(pv);
        }
    }
}

export function unselectCatalog(pv,dlAry) {
    getLayers(pv,dlAry)
        .forEach( (dl) => {
            if (dl.canSelect) {
                const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
                dispatchTableSelect(dl.tbl_id, selectInfoCls.data);
            }
        });
}

export function clearFilterCatalog(pv, dlAry) {
    const catDlAry= getLayers(pv,dlAry);
    if (!catDlAry.length) return;
    catDlAry.forEach(doClearFilter);
}

export function filterCatalog(pv,dlAry) {

    const p= primePlot(pv);
    const sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const catDlAry= getLayers(pv,dlAry);
    if (!catDlAry.length) return;

    const selectedShape = getSelectedShape(pv);
    catDlAry.forEach((dl) => dl.canFilter && doFilter(dl,p,sel, selectedShape));
    detachSelectArea(pv);
}


function doClearFilter(dl) {
    if (dl.canFilter) {
        dispatchTableFilter({tbl_id: dl.tbl_id, filters: ''});
    }
}


function doFilter(dl,p,sel, selectedShape) {

    const tbl= getTblById(dl.tbl_id);
    if (!tbl || !dl.tableData.data) return;
    const tableDataLength = dl.tableData.data.length;
    const filterInfo = tbl?.request?.filters;
    const filterInfoCls = FilterInfo.parse(filterInfo);
    let filter;
    let newRequest;

    const decimateIdx= findColIdx(dl.tableData.columns,'decimate_key');
    if (decimateIdx>1 && dl.tableMeta['decimate_key']) {
        const idxs= getSelectedPts(sel, p, dl.drawData.data, selectedShape)
            .filter((idx) => idx < tableDataLength)
            .map( (idx) => `'${dl.tableData.data[idx][decimateIdx]}'`);
        if (idxs.length > 0) {
            filter= `IN (${idxs.toString()})`;
            filterInfoCls.addFilter(dl.tableMeta['decimate_key'], filter);
        } else {
            // no points selected
            filterInfoCls.addFilter('ROW_IDX', '< 0');
        }

        newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
        //console.log(newRequest);
        //console.log(idxs);
    }
    else {
        const rowidIdx= findColIdx(dl.tableData.columns,'ROW_IDX');
        let idxs= getSelectedPts(sel, p, dl.drawData.data, selectedShape)
            .filter((idx) => idx < tableDataLength);
        if (rowidIdx >= 0) {
            idxs = idxs.map( (idx) => dl.tableData?.data?.[idx]?.[rowidIdx] );
        }
        filter= `IN (${idxs.length === 0 ? -1 : idxs.toString()})`;     //  ROW_IDX is always positive.. use -1 to force no row selected
        filterInfoCls.setFilter('ROW_IDX', filter);
        newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
    }

}

function getLayers(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .filter( (dl) => dl.drawLayerTypeId===TYPE_ID);
}

/**
 * get the selected area shape from the SelectArea layer
 * @param pv
 * @param dlAry
 * @returns {string}
 */
export function getSelectedShape(pv, dlAry= dlRoot().drawLayerAry) {
    const selectAreaLayer = getAllDrawLayersForPlot(dlAry, pv.plotId,true)
                            .find( (dl) => dl.drawLayerTypeId===SelectArea.TYPE_ID);
    return selectAreaLayer?.selectedShape ?? SelectedShape.rect.key;
}
