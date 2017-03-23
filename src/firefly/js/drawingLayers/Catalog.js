/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty,get} from 'lodash';
import {primePlot,getAllDrawLayersForPlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import FootprintObj from '../visualize/draw/FootprintObj.js';
import {makeDrawingDef, getNextColor} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr, {SUBGROUP} from '../visualize/DrawLayerCntlr.js';
import {MouseState} from '../visualize/VisMouseSync.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import {makeWorldPt} from '../visualize/Point.js';
import {dispatchTableHighlight,dispatchTableFilter} from '../tables/TablesCntlr.js';
import {COLOR_HIGHLIGHTED_PT} from '../visualize/draw/DrawingDef.js';
import {MetaConst} from '../data/MetaConst.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getSelectedPts} from '../visualize/VisUtil.js';
import {dispatchTableSelect} from '../tables/TablesCntlr.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import {showInfoPopup} from '../ui/PopupUtil.jsx';
import {getTblById,getCellValue} from '../tables/TableUtil.js';
import {getUIComponent} from './CatalogUI.jsx';
import {FilterInfo} from '../tables/FilterInfo.js';
import DrawUtil from '../visualize/draw/DrawUtil.js';

const TYPE_ID= 'CATALOG_TYPE';

const helpText= 'Click on point to highlight';



const findColIdx= (columns,colId) => columns.findIndex( (c) => c.name===colId);

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,getUIComponent);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID



//---------------------------------------------------------------------
//---------------------------------------------------------------------
//--- The following are functions are used to create and
//--- operate the drawing layer
//---------------------------------------------------------------------
//---------------------------------------------------------------------


function creator(initPayload, presetDefaults) {
    const {catalogId, tableData, tableMeta, title,
           selectInfo, columns, tableRequest, highlightedRow, color, angleInRadian=false,
           symbol, size,
           dataTooBigForSelection=false, catalog=true,boxData=false }= initPayload;

    const drawingDef= Object.assign(makeDrawingDef(),
        {
            size: size || 5,
            symbol: DrawSymbol.get(symbol) || DrawSymbol.SQUARE
        },
        presetDefaults);

    const pairs= {
        [MouseState.DOWN.key]: highlightChange
    };

    drawingDef.color= (color || get(tableMeta,MetaConst.DEFAULT_COLOR) || getNextColor());

    const options= {
        hasPerPlotData:false,
        isPointData:!boxData,
        canUserDelete: true,
        canUseMouse:true,
        canHighlight: true,
        canSelect: catalog,
        canFilter: true,
        dataTooBigForSelection,
        helpLine : helpText,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        supportSubgroups: Boolean(tableMeta && tableMeta[SUBGROUP])
    };

    const dl= DrawLayer.makeDrawLayer(catalogId,TYPE_ID, 
                                      title || `Catalog: ${get(tableMeta,'title',catalogId)}`,
                                      options, drawingDef, null, pairs );
    dl.catalogId= catalogId;
    dl.tableData= tableData;
    dl.tableMeta= tableMeta;
    dl.tableRequest= tableRequest;
    dl.selectInfo= selectInfo;
    dl.highlightedRow= highlightedRow;
    dl.columns= columns;
    dl.catalog= catalog;
    dl.boxData= boxData;
    dl.angleInRadian= angleInRadian;

    return dl;
}


/**
 * This function is mapped to the mouse down key
 * @param mouseStatePayload
 */
function highlightChange(mouseStatePayload) {
    const {drawLayer,plotId,screenPt}= mouseStatePayload;
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
    let done= false;
    let idx= 0;
    const maxChunk= 1000;
    let minDist = 20;
    const {data}= drawLayer.drawData;
    const {tableRequest}= drawLayer;
    let closestIdx= -1;
    const plot= primePlot(visRoot(),plotId);
    const id= window.setInterval( () => {
        if (done) {
            window.clearInterval(id);
            const {tableMeta, tableData}= drawLayer;
            if (closestIdx > -1) {
                if (tableMeta.decimate_key) {
                    const colIdx= tableData.columns.findIndex((c) => c.name==='rowidx');
                    dispatchTableHighlight(drawLayer.drawLayerId,tableData.data[closestIdx][colIdx],tableRequest);
                }
                else {
                    dispatchTableHighlight(drawLayer.drawLayerId,closestIdx,tableRequest);
                }
            }
        }

        let dist;

        for(let i=0;(idx<data.length && i<maxChunk ); i++) {
            const obj= data[idx];
            if (obj) {
                dist = DrawOp.getScreenDist(obj, plot, screenPt);
                if (dist > -1 && dist < minDist) {
                    minDist = dist;
                    closestIdx= idx;
                }
            }
            idx++;
        }
        done= (idx===data.length);
    },0);
    return () => window.clearInterval(id);
}




function getLayerChanges(drawLayer, action) {
    if  (action.type!==DrawLayerCntlr.MODIFY_CUSTOM_FIELD) return null;

    const {changes}= action.payload;
    const dd= Object.assign({},drawLayer.drawData);
    dd[DataTypes.HIGHLIGHT_DATA]= null;
    if (changes.tableData) dd[DataTypes.DATA]= null;
    if (changes.selectInfo) dd[DataTypes.SELECTED_IDXS]= null;
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

    const{tableData, columns}= drawLayer;
    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? computeDrawLayer(drawLayer, tableData, columns) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? 
                          computeHighlightLayer(drawLayer, columns) : lastDataRet;
        case DataTypes.SELECTED_IDXS:
            if (!drawLayer.catalog) return null;
            return isEmpty(lastDataRet) ?
                computeSelectedIdxAry(drawLayer.selectInfo) : lastDataRet;
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
    return drawLayer.boxData ? computeBoxDrawLayer(drawLayer, tableData,columns) : computePointDrawLayer(drawLayer, tableData,columns);

}


function toAngle(d, radianToDegree)  {
    const v= Number(d);
    return (!isNaN(v) && radianToDegree) ? v*180/Math.PI : v;
}

function computePointDrawLayer(drawLayer, tableData, columns) {

    const lonIdx= findColIdx(tableData.columns, columns.lonCol);
    const latIdx= findColIdx(tableData.columns, columns.latCol);
    const {angleInRadian:rad}= drawLayer;
    if (lonIdx<0 || latIdx<0) return null;

    return tableData.data.map( (d) => {
        const wp= makeWorldPt( toAngle(d[lonIdx],rad), toAngle(d[latIdx],rad), columns.csys);
        return PointDataObj.make(wp);
    });
}

function computeBoxDrawLayer(drawLayer, tableData, columns) {
    const {angleInRadian:rad}= drawLayer;

    return tableData.data.map( (d) => {
        const fp= columns.map( (c) => {
            const lonIdx= findColIdx(tableData.columns, c.lonCol);
            const latIdx= findColIdx(tableData.columns, c.latCol);
            return makeWorldPt( toAngle(d[lonIdx],rad), toAngle(d[latIdx],rad), c.csys);
        });
        return FootprintObj.make([fp]);
    });
}

function computeHighlightLayer(drawLayer, columns) {
    return drawLayer.boxData ? computeBoxHighlightLayer(drawLayer,columns, drawLayer.highlightedRow) :
                               computePointHighlightLayer(drawLayer,columns);
}


/**
 *
 * @param drawLayer
 * @param columns
 * @returns {Object[]} return a array of PointDataObj that represents the highlighted object
 */
function computePointHighlightLayer(drawLayer, columns) {


    const tbl= getTblById(drawLayer.drawLayerId);
    if (!tbl) return null;
    const {angleInRadian:rad}= drawLayer;
    const raStr= getCellValue(tbl,drawLayer.highlightedRow, columns.lonCol);
    const decStr= getCellValue(tbl,drawLayer.highlightedRow, columns.latCol);
    if (!raStr || !decStr) return null;

    const wp= makeWorldPt( toAngle(raStr,rad), toAngle(decStr, rad), columns.csys);
    const s = drawLayer.drawingDef.size || 5;
    const s2 = DrawUtil.getSymbolSizeBasedOn(DrawSymbol.X, Object.assign({}, drawLayer.drawingDef, {size: s}));
    const obj= PointDataObj.make(wp, s, drawLayer.drawingDef.symbol);
    const obj2= PointDataObj.make(wp, s2, DrawSymbol.X);
    obj.color= COLOR_HIGHLIGHTED_PT;
    obj2.color= COLOR_HIGHLIGHTED_PT;
    return [obj,obj2];
}

function computeBoxHighlightLayer(drawLayer, columns, highlightedRow) {
    const {tableData}= drawLayer;
    const d= tableData.data[highlightedRow];
    if (!d) return null;
    const fp= columns.map( (c) => {
        const lonIdx= findColIdx(tableData.columns, c.lonCol);
        const latIdx= findColIdx(tableData.columns, c.latCol);
        return makeWorldPt( d[lonIdx], d[latIdx], c.csys);
    });
    const fpObj= FootprintObj.make([fp]);
    fpObj.lineWidth= 3;
    fpObj.color= COLOR_HIGHLIGHTED_PT;
    return [fpObj];
}

function computeSelectedIdxAry(selectInfo) {
    if (!selectInfo) return null;
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


export function selectCatalog(pv,dlAry) {
    const p= primePlot(pv);
    const sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const catDlAry= getLayers(pv,dlAry);
    if (catDlAry.length) {
        const tooBig= catDlAry.some( (dl) => dl.dataTooBigForSelection);
        if (tooBig) {
            showInfoPopup('Your data set is too large to select. You must filter it down first.',
                `Can't Select`); // eslint-disable-line quotes
        }
        else {
            catDlAry.forEach( (dl) => {
                const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
                getSelectedPts(sel,p,dl.drawData.data)
                    .forEach( (idx) => selectInfoCls.setRowSelect(idx, true));
                dispatchTableSelect(dl.drawLayerId, selectInfoCls.data);
            });
        }
    }
}

export function unselectCatalog(pv,dlAry) {
    getLayers(pv,dlAry)
        .forEach( (dl) => {
            const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
            dispatchTableSelect(dl.drawLayerId, selectInfoCls.data);
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
    catDlAry.forEach((dl) =>doFilter(dl,p,sel));
}


function doClearFilter(dl) {
    dispatchTableFilter({tbl_id: dl.drawLayerId, filters: ''});
}


function doFilter(dl,p,sel) {

    const tbl= getTblById(dl.drawLayerId);
    if (!tbl) return;
    const filterInfo = get(tbl, 'request.filters');
    const filterInfoCls = FilterInfo.parse(filterInfo);
    let filter;
    let newRequest;

    const decimateIdx= findColIdx(dl.tableData.columns,'decimate_key');
    if (decimateIdx>1 && dl.tableMeta['decimate_key']) {
        const idxs= getSelectedPts(sel, p, dl.drawData.data)
            .map( (idx) => dl.tableData.data[idx][decimateIdx]);
        filter= `IN (${idxs.toString()})`;
        filterInfoCls.addFilter(dl.tableMeta['decimate_key'], filter);
        newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
        console.log(newRequest);
        console.log(idxs);
    }
    else {
        const rowidIdx= findColIdx(dl.tableData.columns,'ROW_IDX');
        let idxs= getSelectedPts(sel, p, dl.drawData.data);
        idxs = rowidIdx < 0 ? idxs : idxs.map( (idx) => get(dl,`tableData.data[${idx}][${rowidIdx}]`) );
        filter= `IN (${idxs.toString()})`;
        // filterInfoCls.setFilter(filter);
        filterInfoCls.setFilter('ROW_IDX', filter);
        newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
    }

}

function getLayers(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .filter( (dl) => dl.drawLayerTypeId===TYPE_ID);
}


