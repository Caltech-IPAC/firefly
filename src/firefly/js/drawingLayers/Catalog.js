/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {isEmpty,get, isArray} from 'lodash';
import Enum from 'enum';
import {primePlot,getAllDrawLayersForPlot} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchUseTableAutoScroll} from '../visualize/ImagePlotCntlr.js';
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
import {getUIComponent, TableSelectOptions} from './CatalogUI.jsx';
import {FilterInfo} from '../tables/FilterInfo.js';
import DrawUtil from '../visualize/draw/DrawUtil.js';
import SelectArea, {SelectedShape} from './SelectArea.js';
import {detachSelectArea} from '../visualize/ui/SelectAreaDropDownView.jsx';
import {CysConverter} from '../visualize/CsysConverter.js';
import {parseObsCoreRegion} from '../util/ObsCoreSRegionParser.js';
import {brighter, darker} from '../util/Color';
import {isDefined} from '../util/WebUtil';


const TYPE_ID= 'CATALOG_TYPE';
const CatalogType = new Enum(['X', 'BOX', 'REGION']);


const findColIdx= (columns,colId) => columns.findIndex( (c) => c.name===colId);
const factoryDef= makeFactoryDef(TYPE_ID,creator,getDrawData,getLayerChanges,null,getUIComponent);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID



//---------------------------------------------------------------------
//---------------------------------------------------------------------
//--- The following are functions are used to create and
//--- operate the drawing layer
//---------------------------------------------------------------------
//---------------------------------------------------------------------


function creator(initPayload, presetDefaults={}) {
    const {catalogId, tableData, tableMeta, title,
           selectInfo, columns, tableRequest, highlightedRow, color, angleInRadian=false,
           symbol, size, tblId,
           dataTooBigForSelection=false, catalog=true,
           dataType=CatalogType.X.key, tableSelection, isFromRegion=false,
           searchTarget, searchTargetSymbol= DrawSymbol.POINT_MARKER,
    }= initPayload;

    const drawingDef= {...makeDrawingDef(),
            size: size || 5,
            symbol: DrawSymbol.get(symbol) || DrawSymbol.SQUARE,
        ...presetDefaults};

    const pairs= { [MouseState.DOWN.key]: highlightChange };

    drawingDef.color= (color || get(tableMeta,MetaConst.DEFAULT_COLOR) || getNextColor());


    const searchTargetDrawingDef= {...makeDrawingDef(),
            size: 10,
            symbol: searchTargetSymbol,
            color: darker(drawingDef.color)
         };

    const helpText= `Click on ${(dataType == CatalogType.REGION.key) ? 'region' : 'point'} to highlight`;
    const options= {
        hasPerPlotData:false,
        isPointData: catalog,
        canUserDelete: true,
        canUseMouse:true,
        canHighlight: true,
        canSelect: catalog,
        canShowSelect: dataType === CatalogType.REGION.key,
        canFilter: dataType !== CatalogType.REGION.key,
        dataTooBigForSelection,
        helpLine : helpText,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        supportSubgroups: Boolean(tableMeta && tableMeta[SUBGROUP])
    };

    const catalogType = dataType.toUpperCase();
    const rawDl= DrawLayer.makeDrawLayer(catalogId,TYPE_ID,
                                      title || `Catalog: ${get(tableMeta,'title',catalogId)}`,
                                      options, drawingDef, null, pairs );
    return {...rawDl,
        catalogId,
        tableData,
        tableMeta,
        tableRequest,
        selectInfo,
        highlightedRow,
        columns,
        catalog,
        catalogType: Object.keys(CatalogType).includes(catalogType) ? CatalogType.get(catalogType) : CatalogType.X,
        angleInRadian,
        selectOption: tableSelection,
        isFromRegion,
        tblId: tblId ? tblId : catalogId,
        searchTarget,
        searchTargetVisible: true,
        searchTargetDrawingDef,
    };
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
                // for data representing the selected region from region data
                if (data[closestIdx].fromRow) closestIdx = data[closestIdx].fromRow;


                const vr= visRoot();
                if (vr.autoScrollToHighlightedTableRow && vr.useAutoScrollToHighlightedTableRow) {
                    dispatchUseTableAutoScroll(false);
                }
                if (tableMeta.decimate_key) {
                    const colIdx= tableData.columns.findIndex((c) => c.name==='rowidx');
                    dispatchTableHighlight(drawLayer.tblId,tableData.data[closestIdx][colIdx],tableRequest);
                }
                else {
                    dispatchTableHighlight(drawLayer.tblId,closestIdx,tableRequest);
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

    if (changes.tableData || changes.selectOption || changes.searchTargetDrawingDef ||
        isDefined(changes.searchTargetVisible) ||
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

    const{tableData, columns}= drawLayer;

    switch (dataType) {
        case DataTypes.DATA:
            return isEmpty(lastDataRet) ? computeDrawLayer(drawLayer, tableData, columns) : lastDataRet;
        case DataTypes.HIGHLIGHT_DATA:
            return isEmpty(lastDataRet) ? 
                          computeHighlightLayer(drawLayer, columns) : lastDataRet;
        case DataTypes.SELECTED_IDXS:
            if (drawLayer.catalog || drawLayer.catalogType === CatalogType.REGION) {
                return isEmpty(lastDataRet) ?
                    computeSelectedIdxAry(drawLayer) : lastDataRet;
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
    const {selectOption} = drawLayer;

    switch (drawLayer.catalogType) {
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
        case CatalogType.X:
            objs = computePointDrawLayer(drawLayer, tableData, columns);
            break;
    }

    return objs;
}

function toAngle(d, radianToDegree)  {
    const v= Number(d);
    return (!isNaN(v) && radianToDegree) ? v*180/Math.PI : v;
}

function computeSearchTarget(drawLayer) {
    if (!drawLayer.searchTarget || !drawLayer.searchTargetVisible) return;
    const {searchTargetDrawingDef:{symbol,size,color}}= drawLayer;
    const tSym= PointDataObj.make(drawLayer.searchTarget, size, symbol);
    tSym.color= color;
    return tSym;

}

function computePointDrawLayer(drawLayer, tableData, columns) {

    const lonIdx= findColIdx(tableData.columns, columns.lonCol);
    const latIdx= findColIdx(tableData.columns, columns.latCol);
    const {angleInRadian:rad}= drawLayer;
    if (lonIdx<0 || latIdx<0) return null;

    const drawData= tableData.data.map( (d) => {
        const wp= makeWorldPt( toAngle(d[lonIdx],rad), toAngle(d[latIdx],rad), columns.csys);
        return PointDataObj.make(wp);
    });
    drawLayer.searchTarget && drawData.push(computeSearchTarget(drawLayer));
    return drawData;
}



function computeBoxDrawLayer(drawLayer, tableData, columns) {
    const {angleInRadian:rad}= drawLayer;

    const drawData= tableData.data.map( (d) => {
        const fp= columns.map( (c) => {
            const lonIdx= findColIdx(tableData.columns, c.lonCol);
            const latIdx= findColIdx(tableData.columns, c.latCol);
            return makeWorldPt( toAngle(d[lonIdx],rad), toAngle(d[latIdx],rad), c.csys);
        });
        return FootprintObj.make([fp]);
    });
    drawLayer.searchTarget && drawData.push(computeSearchTarget(drawLayer));
    return drawData;
}

function computeRegionLayer(drawLayer, tableData, regionCols) {
    const regionColAry = isArray(regionCols) ? regionCols : [regionCols];

    const drawData= regionColAry.reduce((prev, oneRegionCol) => {
        const {unit='deg'} = oneRegionCol;

        const colObjs = tableData.data.map((oneRow) => {
            const regionInfo = parseObsCoreRegion(oneRow[oneRegionCol.regionIdx], unit);

            return regionInfo.valid ? regionInfo.drawObj : undefined;
        });
        prev.push(...colObjs);
        return prev;
    }, []);
    drawLayer.searchTarget && drawData.push(computeSearchTarget(drawLayer));
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
        case CatalogType.X:
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

    const tbl= getTblById(drawLayer.tblId);
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


function computeRegionHighlightLayer(drawLayer, columns) {
    const {tableData, highlightedRow=0}= drawLayer;
    const d= tableData.data[highlightedRow];
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
    if (!selectInfo || (selectOption && selectOption !== TableSelectOptions.all.key)) {
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


export function selectCatalog(pv,dlAry) {
    const p= primePlot(pv);
    const sel= p.attributes[PlotAttribute.SELECTION];
    if (!sel) return;
    const catDlAry= getLayers(pv,dlAry);
    const selectedShape = getSelectedShape(pv, dlAry);
    if (catDlAry.length) {
        const tooBig= catDlAry.some( (dl) => dl.canSelect && dl.dataTooBigForSelection);
        if (tooBig) {
            showInfoPopup('Your data set is too large to select. You must filter it down first.',
                `Can't Select`); // eslint-disable-line quotes
        }
        else {
            catDlAry.forEach( (dl) => {
                if (dl.canSelect) {
                    const selectInfoCls = SelectInfo.newInstance({rowCount: dl.drawData.data.length});
                    getSelectedPts(sel, p, dl.drawData.data, selectedShape)
                        .forEach((idx) => selectInfoCls.setRowSelect(idx, true));
                    dispatchTableSelect(dl.tblId, selectInfoCls.data);
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
                dispatchTableSelect(dl.tblId, selectInfoCls.data);
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

    const selectedShape = getSelectedShape(pv, dlAry);
    catDlAry.forEach((dl) => dl.canFilter && doFilter(dl,p,sel, selectedShape));
    detachSelectArea(pv);
}


function doClearFilter(dl) {
    if (dl.canFilter) {
        dispatchTableFilter({tbl_id: dl.tblId, filters: ''});
    }
}


function doFilter(dl,p,sel, selectedShape) {

    const tbl= getTblById(dl.tblId);
    if (!tbl) return;
    const filterInfo = get(tbl, 'request.filters');
    const filterInfoCls = FilterInfo.parse(filterInfo);
    let filter;
    let newRequest;

    const decimateIdx= findColIdx(dl.tableData.columns,'decimate_key');
    if (decimateIdx>1 && dl.tableMeta['decimate_key']) {
        const idxs= getSelectedPts(sel, p, dl.drawData.data, selectedShape)
            .map( (idx) => `'${dl.tableData.data[idx][decimateIdx]}'`);
        filter= `IN (${idxs.toString()})`;
        filterInfoCls.addFilter(dl.tableMeta['decimate_key'], filter);
        newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
        console.log(newRequest);
        console.log(idxs);
    }
    else {
        const rowidIdx= findColIdx(dl.tableData.columns,'ROW_IDX');
        let idxs= getSelectedPts(sel, p, dl.drawData.data, selectedShape);
        idxs = rowidIdx < 0 ? idxs : idxs.map( (idx) => get(dl,`tableData.data[${idx}][${rowidIdx}]`) );
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
 * @returns {*}
 */
export function getSelectedShape(pv, dlAry) {
    const selectAreaLayer = getAllDrawLayersForPlot(dlAry, pv.plotId,true)
                            .find( (dl) => dl.drawLayerTypeId===SelectArea.TYPE_ID);

    return get(selectAreaLayer, 'selectedShape', SelectedShape.rect.key);
}
