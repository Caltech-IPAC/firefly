import {getPreference} from '../../core/AppDataCntlr';
import {dispatchAddActionWatcher} from '../../core/MasterSaga';
import {MetaConst} from '../../data/MetaConst';
import {
    getHpxIndexData, getIpixForWp, getTile, hasOrderDataReady, makeHpxWpt, MIN_NORDER
} from '../../tables/HpxIndexCntlr';
import {
    dispatchTableHighlight, dispatchTableUiUpdate, TABLE_HIGHLIGHT, TABLE_REMOVE, TABLE_SELECT, TABLE_UPDATE
} from '../../tables/TablesCntlr';
import {getMetaEntry, getTableUiByTblId, getTblById} from '../../tables/TableUtil';
import CysConverter from '../../visualize/CsysConverter';
import {COLOR_HIGHLIGHTED_PT, getNextColor, makeDrawingDef} from '../../visualize/draw/DrawingDef';
import DrawLayer, {ColorChangeType, DataTypes} from '../../visualize/draw/DrawLayer';
import {makeFactoryDef} from '../../visualize/draw/DrawLayerFactory';
import DrawOp from '../../visualize/draw/DrawOp';
import {DrawSymbol} from '../../visualize/draw/DrawSymbol';
import DrawUtil from '../../visualize/draw/DrawUtil';
import PointDataObj from '../../visualize/draw/PointDataObj';
import DrawLayerCntlr, {
    dispatchForceDrawLayerUpdate, dispatchModifyCustomField, dispatchUpdateDrawLayer, dlRoot, getDlAry, SUBGROUP
} from '../../visualize/DrawLayerCntlr';
import ImagePlotCntlr, {dispatchUseTableAutoScroll, visRoot} from '../../visualize/ImagePlotCntlr';
import {
    getCenterOfProjection, getConnectedPlotsIds, getDrawLayerById, getPlotViewIdListByPositionLock, primePlot
} from '../../visualize/PlotViewUtil';
import {pointEquals} from '../../visualize/Point';
import {makeTableColorTitle} from '../../visualize/ui/DrawLayerUIComponents';
import {MouseState} from '../../visualize/VisMouseSync';
import {isHiPS, isImage} from '../../visualize/WebPlot';
import {CatalogType} from '../Catalog';
import {getUIComponent} from '../CatalogUI';
import {
    HPX_GROUP_TYPE_PREF, DEFAULT_MIN_HPX_GROUP, HPX_MIN_GROUP_PREF, TYPE_ID, DEFAULT_HPX_GROUP_TYPE, HPX_GRID_SIZE_PREF,
    DEFAULT_HPX_GRID_SIZE, HPX_HEATMAP_LABEL_PREF, DEFAULT_HEATMAP_LABELS, HPX_HEATMAP_STRETCH_PREF, DEFAULT_HPX_STRETCH
} from './HpxCatalogUtil';
import {createTileDataMaker} from './TileDataMaker';

const getLayerActions= () => [
    DrawLayerCntlr.ATTACH_LAYER_TO_PLOT, DrawLayerCntlr.DESTROY_DRAWING_LAYER,
    DrawLayerCntlr.MODIFY_CUSTOM_FIELD, DrawLayerCntlr.CHANGE_DRAWING_DEF
];
const getTableActions= () => [ TABLE_HIGHLIGHT,TABLE_UPDATE, TABLE_SELECT ];


const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,getUIComponent, undefined);
let lastProjectionCenter= undefined;

export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

function creator(initPayload, presetDefaults={}) {

    const {catalogId,
        highlightedRow, color, title,
        tableCanControlColor= true,
        symbol, size, tbl_id,
        layersPanelLayoutId,
    }= initPayload;

    const table= getTblById(tbl_id);

    const drawingDef= {...makeDrawingDef(),
        size: size || 3,
        symbol: DrawSymbol.get(symbol) || DrawSymbol.get(table.tableMeta?.[MetaConst.DEFAULT_SYMBOL]) || DrawSymbol.SQUARE,
        ...presetDefaults};

    const pairs= { [MouseState.UP.key]: highlightChange, [MouseState.DOWN.key]: saveLastDown };
    drawingDef.color= color || getMetaEntry(tbl_id,MetaConst.DEFAULT_COLOR) || getNextColor();


    const options= {
        catalogType: CatalogType.POINT,
        canUseMouse:true,
        canUserDelete: true,
        canHighlight:true,
        canSelect: true,
        canFilter: true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        isPointData:true,
        supportSubgroups: Boolean(table.tableMeta && table.tableMeta[SUBGROUP]),
        layersPanelLayoutId,
    };

    const rawDl= DrawLayer.makeDrawLayer(catalogId,TYPE_ID,
        title || `Catalog: ${table.tableMeta?.titled}`,
        options, drawingDef, null, pairs );

    setTimeout(() => void asyncCreateStep(catalogId,tbl_id),0);

    dispatchAddActionWatcher({
        actions:[
            ...getLayerActions(), ...getTableActions(),
            ImagePlotCntlr.ANY_REPLOT,
            ImagePlotCntlr.ZOOM_IMAGE, ImagePlotCntlr.RECENTER, ImagePlotCntlr.PROCESS_SCROLL,
            ImagePlotCntlr.CHANGE_HIPS, ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION, ImagePlotCntlr.UPDATE_VIEW_SIZE,
        ],
        callback: watchDisplay,
        params: {tbl_id,catalogId}
    });

    return {...rawDl,
        catalogId,
        tbl_id,
        table,
        highlightedRow,
        tableCanControlColor,
        minGroupSize:getPreference(HPX_MIN_GROUP_PREF,DEFAULT_MIN_HPX_GROUP),
        groupType: getPreference(HPX_GROUP_TYPE_PREF,DEFAULT_HPX_GROUP_TYPE),
        gridSize: getPreference(HPX_GRID_SIZE_PREF,DEFAULT_HPX_GRID_SIZE),
        heatMapLabels: getPreference(HPX_HEATMAP_LABEL_PREF,DEFAULT_HEATMAP_LABELS),
        heatMapStretch: getPreference(HPX_HEATMAP_STRETCH_PREF,DEFAULT_HPX_STRETCH),
        dataTooBigForSelection: false,
    };
}

async function asyncCreateStep(catalogId, tbl_id) {
    if (!tbl_id) return;
    const {tbl_ui_id} = getTableUiByTblId(tbl_id) ?? {};
    const dl = getDrawLayerById(getDlAry(), catalogId);
    if (!dl || !tbl_ui_id) return;

    dispatchTableUiUpdate({
        tbl_ui_id,
        title: makeTableColorTitle(dl.drawingDef.color, dl.drawLayerId, undefined, tbl_id),
        color: dl.drawingDef.color
    });
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
    const {drawLayer,plotId,screenPt,worldPt}= mouseStatePayload;
    const plot= primePlot(visRoot(),plotId);
    const center= getCenterOfProjection(plot);
    if (lastProjectionCenter && (!pointEquals(center, lastProjectionCenter?.center) || lastProjectionCenter.plotId!==plotId)) return;
    lastProjectionCenter= undefined;
    makeHighlightDeferred(drawLayer,plotId,screenPt,worldPt);
}


function makeHighlightDeferred(drawLayer,plotId,screenPt,worldPt) {
    let done = false;
    let idx = 0;
    const maxChunk = 1000;
    let minDist = 20;
    const data = drawLayer.drawData?.data?.[plotId];
    if (!data) return;
    const {tableRequest} = drawLayer;
    let closestIdx = -1;
    const plot = primePlot(visRoot(), plotId);


    let dataNorder;
    let clickIpix;


    const id= window.setInterval( () => {

        if (done) {
            window.clearInterval(id);
            if (closestIdx > -1) {
                if (data[closestIdx].norder && data[closestIdx].ipix) {
                    const norder= data[closestIdx].norder;
                    if (norder<MIN_NORDER) return;
                    const updatedDl= getDrawLayerById(dlRoot(),drawLayer.drawLayerId);
                    let expandedTiles= updatedDl.expandedTiles ?? {};
                    if (!expandedTiles[plotId]) {
                        expandedTiles= {...expandedTiles, [plotId]:{norder, ipixAry: []}};
                    }
                    expandedTiles[plotId].ipixAry?.unshift(data[closestIdx].ipix);
                    const max=getMaxExpandedTiles(norder,drawLayer.tbl_id, expandedTiles[plotId].ipixAry);
                    if (expandedTiles[plotId].ipixAry.length>max) expandedTiles[plotId].ipixAry.length=max;
                    void makeTileDataAndUpdate(updatedDl,plotId,updatedDl.tbl_id,false, expandedTiles);
                }
                else if (!isNaN(data[closestIdx].fromRow)) {
                    const vr= visRoot();
                    if (vr.autoScrollToHighlightedTableRow && vr.useAutoScrollToHighlightedTableRow) {
                        dispatchUseTableAutoScroll(false);
                    }
                    dispatchTableHighlight(drawLayer.tbl_id,data[closestIdx].fromRow,tableRequest);
                }
            }
        }

        let dist;

        const cc= CysConverter.make(plot);
        for(let i=0;(idx<data.length && i<maxChunk ); i++) {
            const obj= data[idx];
            if (obj) {
                if (obj.norder && obj.ipix && !dataNorder) {
                    dataNorder= obj.norder;
                    clickIpix= getIpixForWp(worldPt,2**dataNorder);
                }
                if (obj.ipix) {
                    if (obj.ipix===clickIpix) {
                        closestIdx= idx;
                        minDist= 0;
                        break;
                    }
                }
                else {
                    dist = DrawOp.getScreenDist(obj, cc, screenPt);
                    if (dist > -1 && dist < minDist) {
                        minDist = dist;
                        closestIdx= idx;
                        if (minDist === 0) break;
                    }
                }
            }
            idx++;
        }
        done= (idx===data.length) || (minDist === 0);
    },0);
    return () => window.clearInterval(id);
}

function getMaxExpandedTiles(norder,tbl_id,ipixAry) {

    let totalPts= 0;
    if (hasOrderDataReady(tbl_id) && norder<11) {
        const idxData = getHpxIndexData(tbl_id);
        totalPts= ipixAry.reduce( (sum, ipix) => sum+ getTile(idxData.orderData,norder,ipix).count,0);
    }
    if (norder<7) return totalPts < 50000 ? 4 : 1;
    if (norder<8) return totalPts < 50000 ? 6 : 2;
    if (norder<10) return totalPts < 50000 ? 18 : 4;
    if (norder<11) return totalPts < 50000 ? 30 : 4;
    else return 400;
}

function getLayerChanges(drawLayer, action) {
    if  (action.type!==DrawLayerCntlr.MODIFY_CUSTOM_FIELD) return null;
    const {changes}= action.payload;
    return changes;
}





function getDrawData(dataType, plotId, drawLayer, action, lastDataRet) {
    switch (dataType) {
        case DataTypes.DATA:
            return drawLayer.drawData?.data?.[plotId];
        case DataTypes.HIGHLIGHT_DATA:
            return drawLayer.highlightedRow>-1 ? getHighlightData(drawLayer) : [];
        case DataTypes.SELECTED_IDXS:
            return [];  // selected indexes is handled at DATA layer
    }
}

function getHighlightData(dl) {
    if (isNaN(dl.highlightedRow)) return [];
    const idxData= getHpxIndexData(dl.tbl_id);
    if (!idxData) return [];

    const wpt= makeHpxWpt(idxData,dl.highlightedRow);
    if (!wpt) return [];
    const s = dl.drawingDef.size+2 || 5;
    const s2 = DrawUtil.getSymbolSizeBasedOn(DrawSymbol.X, Object.assign({}, dl.drawingDef, {size: s}));
    const obj= PointDataObj.make(wpt, s, dl.drawingDef.symbol);
    const obj2= PointDataObj.make(wpt, s2, DrawSymbol.X);
    obj.color= COLOR_HIGHLIGHTED_PT;
    obj2.color= COLOR_HIGHLIGHTED_PT;
    return [obj,obj2];
}


function watchDisplay(action={}, cancelSelf, params) {
    const {type}= action;
    if (getLayerActions().includes(type)) handleLayerActions(action,cancelSelf,params);
    if (getTableActions().includes(type)) handleTableActions(action,cancelSelf,params);
    else handlePlotActions(action,cancelSelf,params);
}

function handleLayerActions(action,cancelSelf,params) {
    const {catalogId,tbl_id}= params;
    if (action.payload.drawLayerId !== catalogId) return;
    const dl = getDrawLayerById(getDlAry(), catalogId);
    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            action.payload.plotIdAry.forEach((plotId) => void makeTileDataAndUpdate(dl, plotId, tbl_id));
            break;
        case DrawLayerCntlr.CHANGE_DRAWING_DEF:
            dl?.visiblePlotIdAry?.forEach((plotId) => void makeTileDataAndUpdate(dl, plotId, tbl_id, false));
            break;
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            dl?.visiblePlotIdAry?.forEach((plotId) => void makeTileDataAndUpdate(dl, plotId, tbl_id, false));
            break;

        case DrawLayerCntlr.DESTROY_DRAWING_LAYER:
        case TABLE_REMOVE:
            cancelSelf();
            break;
    }
}



function handleTableActions(action,cancelSelf,params) {
    const {catalogId,tbl_id}= params;
    if (action.payload.tbl_id !== tbl_id) return;
    const dl = getDrawLayerById(getDlAry(), catalogId);
    switch (action.type) {
        case TABLE_SELECT:
            setTimeout(() =>
                dl.visiblePlotIdAry.forEach((plotId) => void makeTileDataAndUpdate(dl, plotId, tbl_id)), 1 );
            break;
        case TABLE_HIGHLIGHT:
        case TABLE_UPDATE:
            dispatchModifyCustomField(catalogId, {highlightedRow:action.payload.highlightedRow});
            break;
    }
}



function handlePlotActions(action, cancelSelf, params) {
    const {catalogId,tbl_id}= params;
    const dl = getDrawLayerById(getDlAry(), catalogId);
    const connectedIds= getConnectedPlotsIds(dlRoot(),catalogId,true);
    if (!connectedIds.length) return;
    const {plotId}= action.payload;
    const plot= plotId && primePlot(visRoot(),plotId);

    switch (action.type) {
        case ImagePlotCntlr.ZOOM_IMAGE:
        case ImagePlotCntlr.PROCESS_SCROLL:
            const plotIdAry= getPlotViewIdListByPositionLock(visRoot(),plotId);
            plotIdAry.forEach(
                (plotId) => {
                    const plot= primePlot(visRoot(),plotId);
                    if (!isImage(plot) || !connectedIds.includes(plotId)) return;
                    void makeTileDataAndUpdate(dl,plotId,tbl_id, action.type!==ImagePlotCntlr.PROCESS_SCROLL);
                });
            break;

        case ImagePlotCntlr.UPDATE_VIEW_SIZE:
            if (!connectedIds.includes(plotId)) return;
            void makeTileDataAndUpdate(dl,plotId,tbl_id, action.type!==ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION);
            break;
        case ImagePlotCntlr.RECENTER:
        case ImagePlotCntlr.ANY_REPLOT:
        case ImagePlotCntlr.CHANGE_HIPS:
        case ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION:
            if (!isHiPS(plot)) return;
            if (!connectedIds.includes(plotId)) return;
            void makeTileDataAndUpdate(dl,plotId,tbl_id, action.type!==ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION);
            break;
    }
    return params;
}

const tableAborts= {};

async function makeTileDataAndUpdate(dl, plotId, tbl_id, clearExpanded=true, newExpanded) {
    const abortId= `${dl.drawLayerId}-${tbl_id}-${plotId}`;
    tableAborts[abortId]?.();
    const {makeTileData, abort}= createTileDataMaker();
    tableAborts[abortId]= abort;
    const newDrawData= await makeTileData(dl, plotId, tbl_id, clearExpanded ? {} : newExpanded ?? dl.expandedTiles);
    if (!newDrawData) return;

    const updatedDl= getDrawLayerById(dlRoot(),dl.drawLayerId);
    const newDl= {...updatedDl};
    if (!newDl.drawData) newDl.drawData={data:{}};
    newDl.drawData.data[plotId]= newDrawData;
    if (newExpanded) newDl.expandedTiles= newExpanded;
    if (clearExpanded) newDl.expandedTiles={};
    dispatchUpdateDrawLayer(newDl);
    dispatchForceDrawLayerUpdate(dl.drawLayerId, plotId);
}