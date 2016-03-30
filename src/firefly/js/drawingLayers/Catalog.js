/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty} from 'lodash';
import {primePlot} from '../visualize/PlotViewUtil.js';
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
import {dispatchTableHighlight} from '../tables/TablesCntlr.js';
import {COLOR_HIGHLIGHTED_PT} from '../visualize/draw/DrawingDef.js';
import {MetaConst} from '../data/MetaConst.js';

const TYPE_ID= 'CATALOG_TYPE';

const helpText=
    `Click on point to highlight`;




const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,null);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID





var createCnt= 0;
const defColors= [COLOR_PT_1, COLOR_PT_2, COLOR_PT_3, COLOR_PT_5, COLOR_PT_6];


function creator(initPayload) {
    const {catalogId, tableData, tableMeta, columns, tableRequest, highlightedRow}= initPayload;
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
    dl.highlightedRow= highlightedRow;
    dl.columns= columns;


    createCnt++;
    return dl;
}



function highlightChange(mouseStatePayload) {
    const {drawLayer,plotId,screenPt}= mouseStatePayload;
    makeHighlightDeferred(drawLayer,plotId,screenPt);
}


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

function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {

    const{tableData, columns}= drawLayer;
    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? computeDrawLayer(tableData, columns) : lastDataRet;
            break;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? 
                          computeHighlightLayer(tableData, drawLayer.highlightedRow, columns) : lastDataRet;
            break;
        case DataTypes.SELECTED_IDX_ARY:
            return null;
            break;
    }
    return null;
}


function getLayerChanges(drawLayer, action) {
    if  (action.type!==DrawLayerCntlr.MODIFY_CUSTOM_FIELD) return null;

    const {changes}= action.payload;
    var dd= Object.assign({},drawLayer.drawData);
    dd[DataTypes.HIGHLIGHT_DATA]= null;
    if (changes.tableData) dd[DataTypes.DATA]= null;
    return Object.assign({}, changes,{drawData:dd});
}


const findColIdx= (columns,colId) => columns.findIndex( (c) => c.name===colId);

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


function computeHighlightLayer(tableData, highlightedRow, columns) {

    const lonIdx= findColIdx(tableData.columns, columns.lonCol);
    const latIdx= findColIdx(tableData.columns, columns.latCol);
    if (lonIdx<0 || latIdx<0) return null;

    const d= tableData.data[highlightedRow];

    const wp= makeWorldPt( d[lonIdx], d[latIdx], columns.csys);
    const obj= PointDataObj.make(wp, 5, DrawSymbol.SQUARE);
    const obj2= PointDataObj.make(wp, 5, DrawSymbol.X);
    obj.color= COLOR_HIGHLIGHTED_PT;
    obj2.color= COLOR_HIGHLIGHTED_PT;

    return [obj,obj2];
}

