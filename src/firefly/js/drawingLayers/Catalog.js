/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty,get} from 'lodash';
import {primePlot,getAllDrawLayersForPlot} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef, COLOR_PT_1, COLOR_PT_2, 
    COLOR_PT_3, COLOR_PT_5, COLOR_PT_6} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes,ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr from '../visualize/DrawLayerCntlr.js';
import {MouseState} from '../visualize/VisMouseCntlr.js';
import DrawOp from '../visualize/draw/DrawOp.js';
import {makeWorldPt} from '../visualize/Point.js';
import {dispatchTableHighlight,dispatchTableFetch} from '../tables/TablesCntlr.js';
import {COLOR_HIGHLIGHTED_PT} from '../visualize/draw/DrawingDef.js';
import {MetaConst} from '../data/MetaConst.js';
import {SelectInfo} from '../tables/SelectInfo.js';
import {getSelectedPts} from '../visualize/VisUtil.js';
import {dispatchTableSelect} from '../tables/TablesCntlr.js';
import {PlotAttribute} from '../visualize/WebPlot.js';
import {showInfoPopup} from '../ui/PopupUtil.jsx';
import {findTblById,getCellValue} from '../tables/TableUtil.js';
import {FilterInfo} from '../tables/FilterInfo.js';

const TYPE_ID= 'CATALOG_TYPE';

const helpText=
    `Click on point to highlight`;



const findColIdx= (columns,colId) => columns.findIndex( (c) => c.name===colId);

const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,null);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

var createCnt= 0;
const defColors= [COLOR_PT_1, COLOR_PT_2, COLOR_PT_3, COLOR_PT_5, COLOR_PT_6];



//---------------------------------------------------------------------
//---------------------------------------------------------------------
//--- The following are functions are used to create and
//--- operate the drawing layer
//---------------------------------------------------------------------
//---------------------------------------------------------------------


function creator(initPayload) {
    const {catalogId, tableData, tableMeta, selectInfo, columns, 
        tableRequest, highlightedRow,dataTooBigForSelection }= initPayload;
    var drawingDef= makeDrawingDef();
    drawingDef.symbol= DrawSymbol.SQUARE;

    var pairs= {
        [MouseState.DOWN.key]: highlightChange
    };

    drawingDef.color= tableMeta[MetaConst.DEFAULT_COLOR] || defColors[createCnt % defColors.length];

    var options= {
        hasPerPlotData:false,
        isPointData:true,
        canUserDelete: true,
        canUseMouse:true,
        canHighlight: true,
        canSelect: true,
        canFilter: true,
        dataTooBigForSelection,
        helpLine : helpText,
        canUserChangeColor: ColorChangeType.STATIC
    };
    // todo: get the real title
    const dl= DrawLayer.makeDrawLayer(catalogId,TYPE_ID, `Catalog: ${tableMeta.title || catalogId}`,
        options, drawingDef, null, pairs );
    dl.catalogId= catalogId;
    dl.tableData= tableData;
    dl.tableMeta= tableMeta;
    dl.tableRequest= tableRequest;
    dl.selectInfo= selectInfo;
    dl.highlightedRow= highlightedRow;
    dl.columns= columns;


    createCnt++;
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
 * @return {function()}
 */
function makeHighlightDeferred(drawLayer,plotId,screenPt) {
    var done= false;
    var idx= 0;
    const maxChunk= 1000;
    var minDist = Number.MAX_SAFE_INTEGER;
    const {data}= drawLayer.drawData;
    const {tableRequest}= drawLayer;
    var closestIdx;
    const plot= primePlot(visRoot(),plotId);
    const id= window.setInterval( () => {
        if (done) {
            window.clearInterval(id);
            dispatchTableHighlight(drawLayer.drawLayerId,closestIdx,tableRequest);
        }

        var dist;

        for(var i=0;(idx<data.length && i<maxChunk ); i++) {
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
    var dd= Object.assign({},drawLayer.drawData);
    dd[DataTypes.HIGHLIGHT_DATA]= null;
    if (changes.tableData) dd[DataTypes.DATA]= null;
    if (changes.selectInfo) dd[DataTypes.SELECTED_IDXS]= null;
    return Object.assign({}, changes,{drawData:dd});
}



function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    const{tableData, columns}= drawLayer;
    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? computeDrawLayer(tableData, columns) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? 
                          computeHighlightLayer(drawLayer, columns) : lastDataRet;
        case DataTypes.SELECTED_IDXS:
            return isEmpty(lastDataRet) ?
                computeSelectedIdxAry(drawLayer.selectInfo) : lastDataRet;
    }
    return null;
}



function computeDrawLayer(tableData, columns) {

    const lonIdx= findColIdx(tableData.columns, columns.lonCol);
    const latIdx= findColIdx(tableData.columns, columns.latCol);
    if (lonIdx<0 || latIdx<0) return null;

    const ptAry= tableData.data.map( (d) => {
        const wp= makeWorldPt( d[lonIdx], d[latIdx], columns.csys);
        return PointDataObj.make(wp, 5, DrawSymbol.SQUARE);
    });

    return ptAry;
}

function computeHighlightLayer(drawLayer, columns) {


    const tbl= findTblById(drawLayer.drawLayerId);
    if (!tbl) return null;
    const raStr= getCellValue(tbl,drawLayer.highlightedRow, columns.lonCol);
    const decStr= getCellValue(tbl,drawLayer.highlightedRow, columns.latCol);
    if (!raStr || !decStr) return null;

    const wp= makeWorldPt( raStr, decStr, columns.csys);
    const obj= PointDataObj.make(wp, 5, DrawSymbol.SQUARE);
    const obj2= PointDataObj.make(wp, 5, DrawSymbol.X);
    obj.color= COLOR_HIGHLIGHTED_PT;
    obj2.color= COLOR_HIGHLIGHTED_PT;

    return [obj,obj2];
}

function computeSelectedIdxAry(selectInfo) {
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
    var p= primePlot(pv);
    var sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const catDlAry= getLayers(pv,dlAry);
    if (catDlAry.length) {
        const tooBig= catDlAry.some( (dl) => dl.dataTooBigForSelection);
        if (tooBig) {
            showInfoPopup('Your data set is too large to select. You must filter it down first.',
                `Can't Select`);
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

    var p= primePlot(pv);
    var sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const catDlAry= getLayers(pv,dlAry);
    if (!catDlAry.length) return;
    catDlAry.forEach((dl) =>doFilter(dl,p,sel));
}


function doClearFilter(dl) {
    const tbl= findTblById(dl.drawLayerId);
    if (!tbl) return;
    const newRequest = Object.assign({}, tbl.request, {filters: ''});
    dispatchTableFetch(newRequest);
}


function doFilter(dl,p,sel) {

    const tbl= findTblById(dl.drawLayerId);
    if (!tbl) return;
    const filterInfo = get(tbl, 'request.filters');
    const filterInfoCls = FilterInfo.parse(filterInfo);
    var filter;
    var newRequest;

    const decimateIdx= findColIdx(dl.tableData.columns,'decimate_key');
    if (decimateIdx>1 && dl.tableMeta['decimate_key']) {
        const idxs= getSelectedPts(sel, p, dl.drawData.data)
            .map( (idx) => dl.tableData.data[idx][decimateIdx]);
        filter= `IN (${idxs.toString()})`;
        filterInfoCls.addFilter(dl.tableMeta['decimate_key'], filter);
        newRequest = Object.assign({}, tbl.request, {filters: filterInfoCls.serialize()});
        dispatchTableFetch(newRequest);
        console.log(newRequest);
        console.log(idxs);
    }
    else {
        const idxs= getSelectedPts(sel, p, dl.drawData.data);
        filter= `IN (${idxs.toString()})`;
        // filterInfoCls.setFilter(filter);
        filterInfoCls.setFilter('ROWID', filter);
        newRequest = Object.assign({}, tbl.request, {filters: filterInfoCls.serialize()});
        dispatchTableFetch(newRequest);
    }

}

function getLayers(pv,dlAry) {
    return getAllDrawLayersForPlot(dlAry, pv.plotId,true)
        .filter( (dl) => dl.drawLayerTypeId===TYPE_ID);
}


