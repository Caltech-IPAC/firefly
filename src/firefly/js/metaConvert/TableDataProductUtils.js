/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getDefaultChartProps} from 'firefly/charts/ChartUtil.js';
import {showPinMessage} from 'firefly/ui/PopupUtil.jsx';
import {isString} from 'lodash';
import {CHART_UI_EXPANDED, dispatchChartAdd, dispatchChartRemove} from '../charts/ChartsCntlr';
import ComponentCntlr from '../core/ComponentCntlr';
import {LO_MODE, LO_VIEW, SET_LAYOUT_MODE} from '../core/LayoutCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga';
import {ChartType, TableDataType} from '../data/FileAnalysis';
import {MetaConst} from '../data/MetaConst';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {
    dispatchActiveTableChanged, dispatchTableRemove, dispatchTableSearch, TBL_RESULTS_ACTIVE, TBL_UI_EXPANDED
} from '../tables/TablesCntlr';
import {getActiveTableId, getTblById, onTableLoaded} from '../tables/TableUtil';
import {getCellValue, getTblInfo} from '../tables/TableUtil.js';
import MultiViewCntlr, {dispatchUpdateCustom, getMultiViewRoot, getViewer} from '../visualize/MultiViewCntlr.js';
import {
    getObsCoreAccessURL, getObsCoreSRegion, getSearchTarget, isFormatDataLink, makeWorldPtUsingCenterColumns
} from '../voAnalyzer/TableAnalysis';
import {getServiceDescriptors} from '../voAnalyzer/VoDataLinkServDef';
import {makeDlUrl} from './vo/DatalinkProducts';
import {findDataLinkServeDescs} from './vo/ServDescConverter';
import {ensureDefaultChart} from 'firefly/charts/ui/ChartsContainer';
import {pinChart} from 'firefly/charts/ui/PinnedChartContainer';


export function createTableActivate(source, titleInfo, activateParams, contentType= '', tbl_index=0) {
    const tableDataType= contentType?.toLowerCase()===TableDataType.Spectrum.toLowerCase()?TableDataType.Spectrum:undefined;
    return createChartTableActivate({ source, titleInfo, activateParams, tbl_index,
        chartInfo:{useChartChooser:true, tableDataType},
    });
}

const makeCommaSeparated= (strAry) => strAry.reduce( (str,d) => str? `${str},${d}` : d,'');

/**
 * @typedef {Object} ImageAsTableInfo
 * @prop {Array.<String>|undefined} p.colNames - an array of column names
 * @prop {Array.<String>|undefined} p.colUnits - an array of types names
 * @prop {number} [p.cubePlane] - plane of cube - ignored for non-cubes
 */


const loadedTablesIds= new Map();

function isTableChartNormalViewAction(payload, type) {
    const {mode, view, tbl_id} = payload;
    if (type !== SET_LAYOUT_MODE) return false;
    const normal = mode === LO_MODE.expanded && (view === LO_VIEW.none || (view !== LO_VIEW.tables && view !== LO_VIEW.xyPlots));
    if (normal) return true;
    return (mode === LO_MODE.standard && loadedTablesIds.has(tbl_id));
}

/**
 *
 * @param {String} source
 * @param {Object|String} titleInfo
 * @param {String} tbl_id
 * @param {number} tbl_index
 * @param {ImageAsTableInfo} imageAsTableInfo
 * @param cubePlane
 * @param {String} dataTypeHint
 * @param {boolean} extraction
 * @return {TableRequest}
 */
function makeTableRequest(source, titleInfo, tbl_id, tbl_index, imageAsTableInfo={}, cubePlane, dataTypeHint, extraction=false) {
    const {colNames,colUnits}= imageAsTableInfo;

    const title= isString(titleInfo) ? titleInfo : titleInfo.titleStr;
    const colNamesStr= colNames && makeCommaSeparated(colNames);
    const colUnitsStr= colUnits && makeCommaSeparated(colUnits);
    const META_INFO= !extraction ?
        {
            [MetaConst.DATA_SOURCE] : 'false',
            [MetaConst.CATALOG_OVERLAY_TYPE]:'false'
        } : {};
    if (dataTypeHint) META_INFO[MetaConst.DATA_TYPE_HINT]= dataTypeHint;
    const dataTableReq= makeFileRequest(title, source, undefined,
        {
            tbl_id : !extraction ? tbl_id : undefined,
            tbl_index,
            cubePlane,
            startIdx : 0,
            META_INFO,
        });
    if (colNamesStr) dataTableReq.META_INFO[MetaConst.IMAGE_AS_TABLE_COL_NAMES]=  colNamesStr;
    if (colUnitsStr) dataTableReq.META_INFO[MetaConst.IMAGE_AS_TABLE_UNITS]=  colUnitsStr;

    return dataTableReq;
}

function loadTableAndCharts(dataTableReq, tbl_id, tableGroupViewerId, dispatchCharts, noopId) {
    dispatchTableSearch(dataTableReq,
        {
            logHistory: false,
            removable:false,
            tbl_group: tableGroupViewerId,
            backgroundable: false,
            showFilters: true,
            showInfoButton: true
        });

    onTableLoaded(tbl_id).then( () => {
        if (dispatchCharts) {
            dispatchCharts.forEach( (c) => {
                if (c.useChartChooser) {
                    const {xAxis,yAxis,title,...dParams} = c;
                    const chartProps= getDefaultChartProps(tbl_id,xAxis,yAxis);
                    if (chartProps?.layout && title) chartProps.layout.title= title;
                    dispatchChartAdd({...dParams,...chartProps});
                }
                else {
                    dispatchChartAdd(c);
                }
            });

        }
    });

    return () => {
        const table= getTblById(tbl_id);
        if (!table || table.isFetching) return;
        dispatchCancelActionWatcher(noopId);
        dispatchTableRemove(tbl_id,false);
        dispatchCharts && dispatchCharts.forEach( (c) => dispatchChartRemove(c.chartId));
    };
}

/**
 *
 * @param source
 * @param {Object|String} titleInfo
 * @param [tbl_index]
 * @param {ImageAsTableInfo} imageAsTableInfo
 * @param cubePlane
 * @param [dataTypeHint]
 */
export function createTableExtraction(source,titleInfo,tbl_index=0,imageAsTableInfo,cubePlane=0,dataTypeHint='') {
    return () => {
        const dataTableReq= makeTableRequest(source,titleInfo,undefined,tbl_index,imageAsTableInfo,cubePlane,dataTypeHint, true);
        dispatchTableSearch(dataTableReq,
            { setAsActive: false, logHistory: false, showFilters: true, showInfoButton: true });
        let pinMessage = 'Pinning to Table Area';
        if (dataTypeHint === TableDataType.Spectrum || dataTypeHint === TableDataType.LightCurve) {
            pinMessage += ' and Pinned Chart tab';
            onTableLoaded(dataTableReq?.tbl_id).then(() => {
                const chartId = ensureDefaultChart(dataTableReq?.tbl_id);
                if (chartId) pinChart({chartId, displayPinMessage: false});
            });
        }
        showPinMessage(pinMessage);
    };
}


export function getExtractionText(tableDataType) {
    if (tableDataType === TableDataType.Spectrum) return 'Pin Table/Spectrum';
    if (tableDataType === TableDataType.LightCurve) return 'Pin Table/LightCurve';
    return 'Pin Table';
}

export function extractDatalinkTable(table,row,title,setAsActive=true) {
    let url;
    if (isFormatDataLink(table, row)) {
        url= getObsCoreAccessURL(table,row);
    }
    else {
        const serDefAry= getServiceDescriptors(table);
        if (!serDefAry || !serDefAry.length) return;
        const dlSerDef= findDataLinkServeDescs(serDefAry);
        if (!dlSerDef) return;
        url= makeDlUrl(dlSerDef[0],table,row);
    }
    if (!url) return;

    const positionWP = getSearchTarget(table?.request, table);
    const sRegion= getObsCoreSRegion(table,row);
    const rowWP=  makeWorldPtUsingCenterColumns(table, row);


    const dataTableReq= makeTableRequest(url,{titleStr:title},undefined,0,undefined,undefined,undefined,true);
    if (positionWP) dataTableReq.META_INFO[MetaConst.SEARCH_TARGET]= positionWP.toString();
    if (sRegion) dataTableReq.META_INFO[MetaConst.S_REGION]= sRegion;
    if (rowWP) dataTableReq.META_INFO[MetaConst.ROW_TARGET]= rowWP.toString();

    dispatchTableSearch(dataTableReq, {setAsActive, logHistory: false, showFilters: true, showInfoButton: true});
    showPinMessage('Pinning to Table Area');
}


function makeTableCleanupFunc(tbl_id) {
    return () => {
        const tableInfo= loadedTablesIds.get(tbl_id);
        if (tableInfo.doCleanup) {
            tableInfo.cleanupFunc();
            loadedTablesIds.delete(tbl_id);
        }
        else if (tableInfo.deferCleanup) {
            tableInfo.doCleanup= true;
        }
    };
}


/**
 * Activate a chart and table
 * @param {Object} p
 * @param {boolean} [p.chartAndTable] - true - both char and table, false - table only
 * @param {String} p.source
 * @param {{titleString:String,showChartTitle:boolean}|String} p.titleInfo an object that has a titleStr and showchartTile properties
 * @param {ActivateParams} p.activateParams
 * @param {ChartInfo} [p.chartInfo]
 * @param {Number} p.tbl_index
 * @param {ImageAsTableInfo} [p.imageAsTableInfo]
 * @param {String|undefined} [p.chartId]
 * @param {String} [p.tbl_id]
 * @param {String} [p.statefulTabComponentKey]
 * @return {function} the activate function
 */
export function createChartTableActivate({chartAndTable=false, source, titleInfo, activateParams, chartInfo={},
                                         tbl_index=0, imageAsTableInfo, statefulTabComponentKey,
                                         chartId='part-result-chart', tbl_id= 'part-result-tbl'}) {


    return () => {
        const dataTypeHint= chartInfo?.tableDataType ?? '';
        const dispatchCharts=  chartAndTable && makeChartObj(chartInfo, activateParams,titleInfo,chartId,tbl_id);
        const dataTableReq= makeTableRequest(source,titleInfo,tbl_id,tbl_index,imageAsTableInfo,0,dataTypeHint, false);
        const savedRequest= loadedTablesIds.has(tbl_id) && JSON.stringify(loadedTablesIds.get(tbl_id)?.request);

        const tableInfo= loadedTablesIds.get(tbl_id);
        if (tableInfo && source===tableInfo.source) {
            tableInfo.deferCleanup=true;
            return makeTableCleanupFunc(tbl_id);
        }


        if (savedRequest!==JSON.stringify(dataTableReq)) {
            const allChartIds= chartAndTable ? dispatchCharts.map( (c) => c.chartId) : [];
            const noopId = 'noop-' + tbl_id;
            dispatchAddActionWatcher({
                id: noopId,
                actions:[TBL_UI_EXPANDED, SET_LAYOUT_MODE, CHART_UI_EXPANDED, ComponentCntlr.COMPONENT_STATE_CHANGE],
                callback: ({payload,type}) => {
                    const tableInfo= loadedTablesIds.get(tbl_id);
                    if (isTableChartNormalViewAction(payload,type) && loadedTablesIds.has(tbl_id)) {
                        tableInfo.deferCleanup=true;
                    } else if (type === CHART_UI_EXPANDED && allChartIds?.includes(payload.chartId)) {
                        tableInfo.doCleanup=false;
                        tableInfo.deferCleanup=false;
                    } else if (type === TBL_UI_EXPANDED && payload.tbl_id === tbl_id) {
                        tableInfo.doCleanup=false;
                        tableInfo.deferCleanup=false;
                    } else if (type === ComponentCntlr.COMPONENT_STATE_CHANGE && payload.componentId===statefulTabComponentKey) {
                        tableInfo.doCleanup=false;
                        tableInfo.deferCleanup=false;
                    }
                }
            });

            const {tableGroupViewerId}= activateParams;
            const cleanupFunc= loadTableAndCharts(dataTableReq,tbl_id,tableGroupViewerId,dispatchCharts, noopId);
            loadedTablesIds.set(tbl_id, {request:dataTableReq, source,
                doCleanup:true, deferCleanup:false, cleanupFunc});
        }

        return makeTableCleanupFunc(tbl_id);
    };
}




function makeChartFromParams(tbl_id, chartParams, computeXAxis, computeYAxis,titleStr) {
    const {layoutAdds=true, simpleChartType= ChartType.XYChart, simpleData=true, numBins=30, traces,
        layout:passedLayout, xAxisColName, yAxisColName}= chartParams;


    const defLayout= {
        title: {text: titleStr}
    };


    if (simpleData && simpleChartType===ChartType.XYChart) {
        defLayout.xaxis= {showgrid: true};
        defLayout.yaxis= {showgrid: true, range: [0, undefined]};
    }

    let mode= 'lines+markers';
    let layout= defLayout;
    if (passedLayout) layout= layoutAdds ? {...defLayout, ...passedLayout} : passedLayout;
    let data;

    if (simpleData || !traces) {
        if (simpleChartType===ChartType.XYChart) {
            const x= xAxisColName ? `tables::${xAxisColName}` :`tables::${computeXAxis}`;
            const y= yAxisColName ? `tables::${yAxisColName}`: `tables::${computeYAxis}`;
            mode= chartParams.mode || mode;
            data= [{tbl_id, x, y, mode}];
        }
        else if (simpleChartType===ChartType.Histogram) {
            const columnOrExpr= yAxisColName || yAxisColName || computeYAxis || computeXAxis;
            data= [{
                type: 'fireflyHistogram',
                firefly: {
                    tbl_id,
                    options: {
                        algorithm: 'fixedSizeBins',
                        fixedBinSizeSelection: 'numBins',
                        numBins,
                        columnOrExpr,
                    }
                },
            }];
        }
    }
    else {
        data= traces;
        data.forEach((t) => t.tbl_id= tbl_id);
    }

    return { data, layout};
}



function makeChartObj(chartInfo,  activateParams, titleInfo, chartId, tbl_id ) {

    const {chartViewerId:viewerId}= activateParams;
    const {xAxis, yAxis, useChartChooser, chartParamsAry,connectPoints,showChartTitle=true}= chartInfo;

    /* The table and chart title is the part's desc field. When the HDU does not have extname defined, the desc in the
     part is not defined.  In such case, the title is defined in PartAnalyzer is 'table_'+ HDU-index.  In order to change
     the table name to this title and now show the same title in the chart,  the titleInfo object is introduced. It tells
     if the showChartTitle or not.  if chartInfo.showChartTitle is false, the chart title is removed.
     */
    const chartTitle= (showChartTitle && titleInfo) ? isString(titleInfo) ? titleInfo : titleInfo.titleStr : '';
    if (chartParamsAry) {
        let chartNum=1;
        return chartParamsAry
            .map((chartParams) => makeChartFromParams(tbl_id, chartParams, xAxis, yAxis, chartTitle))
            .map((dataLayout) => ({viewerId, groupId: viewerId, chartId: `${chartId}--${chartNum++}`, ...dataLayout}));
    }
    else if (useChartChooser) {
        const obj= [{ viewerId, groupId: viewerId, chartId,xAxis, yAxis, useChartChooser: true }];
        if (chartTitle) obj[0].title= {text: chartTitle};
        return obj;
    }
    else {
        return [ {
            viewerId, groupId: viewerId, chartId,
            data: [{
                    tbl_id,
                    x: `tables::${xAxis}`,
                    y: `tables::${yAxis}`,
                    mode: connectPoints ? 'lines+markers' : 'markers',
                }],
            layout: {
                title: {text: chartTitle},
                xaxis: {showgrid: true},
                yaxis: {showgrid: true, range: [0, undefined]},
            }
        }];
    }

}


export function createChartSingleRowArrayActivate(source, titleStr, activateParams,
                                                  chartInfo, tblRow= 0,tbl_index=0,
                                    chartId='part-result-chart',tbl_id= 'part-result-tbl') {
    return () => {
        const {tableGroupViewerId, chartViewerId}= activateParams;
        const dataTableReq= makeFileRequest(titleStr, source, undefined,
            {
                tbl_id,
                tbl_index,
                startIdx : 0,
                META_INFO : {
                    [MetaConst.DATA_SOURCE] : 'false',
                    [MetaConst.CATALOG_OVERLAY_TYPE]:'false'
                }
            });
        // dispatchTableFetch(dataTableReq);
        dispatchTableSearch(dataTableReq,
            {
                logHistory: false,
                removable:false,
                tbl_group: tableGroupViewerId,
                backgroundable: false,
                showFilters: true,
                showInfoButton: true
            });

        onTableLoaded(tbl_id).then( () => {
            const table= getTblById(tbl_id);
            const xAry= getCellValue(table, tblRow, chartInfo.xAxis);
            const yAry= getCellValue(table, tblRow, chartInfo.yAxis);

            const dispatchParams= {
                viewerId: chartViewerId,
                groupId: chartViewerId,
                chartId,
                data: [{
                    x: xAry,
                    y: yAry,
                    mode: 'lines+markers'
                }],
                layout: {
                    title: {text: titleStr},
                    xaxis: {showgrid: true},
                    yaxis: {showgrid: true, range: [0, undefined]},
                }
            };
            dispatchChartAdd(dispatchParams);
        });
        return () => {
            dispatchChartRemove(chartId);
            dispatchTableRemove(tbl_id,false);
        };
    };

}




const computePlotId= (plotIdRoot ,plotIdx) => `${plotIdRoot}-row-${plotIdx}`;

/**
 * helper function to make a list of table row that should be used to plot in grid mode
 * @param table
 * @param maxRows
 * @param plotIdRoot
 * @return {Array<{plotId:String,row:number,highlight:boolean}>}
 */
export function findGridTableRows(table,maxRows, plotIdRoot) {

    const {startIdx, endIdx, hlRowIdx}= getTblInfo(table, maxRows);

    let j= 0;
    const retval= [];

    for(let i=startIdx; (i<endIdx );i++) {
        retval[j] = {plotId: computePlotId(plotIdRoot, i), row: i, highlight: j === hlRowIdx};
        j++;
    }
    return retval;
}


let lastTblId;

/**
 * passing an object with tbl_id as key and activate function as value. Make a new activate function
 * @param {Object} activateObj
 * @param {ActivateParams} activateParams
 * @return {function(): function(): void}
 */
export function makeMultiTableActivate(activateObj,activateParams) {
    return () => {
        const deActivateAry= Object.values(activateObj).map( ({activate}) => activate());
        const tblIdList= Object.keys(activateObj);
        const chartList= Object.values(activateObj).map( ({chartId}) => chartId);

        Promise.all(tblIdList.map( async (tbl_id) => await onTableLoaded(tbl_id)))
            .then( () => {
                if (lastTblId) dispatchActiveTableChanged(lastTblId,activateParams.tableGroupViewerId);
            });

        const id= tblIdList.join('-');

        const watcher= (action) => {
            let idx;
            switch (action.type) {
                case MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA:
                    const chartId= action.payload.customData.activeItemId;
                    if (!chartList.includes(chartId)) return;
                    idx= chartList.indexOf(chartId);
                    const newTblId= tblIdList[idx];
                    if (newTblId && getActiveTableId(activateParams.tableGroupViewerId) !== newTblId) {
                        dispatchActiveTableChanged(newTblId,activateParams.tableGroupViewerId);
                    }
                    break;
                case TBL_RESULTS_ACTIVE:
                    if (action.payload.tbl_group!==activateParams.tableGroupViewerId) return;
                    const tbl_id= action.payload.tbl_id;
                    idx= tblIdList.indexOf(tbl_id);
                    const newChartId= chartList[idx];
                    if (getViewer(getMultiViewRoot(),activateParams.chartViewerId)?.customData.activeItemId!==newChartId) {
                        dispatchUpdateCustom(activateParams.chartViewerId, {activeItemId: chartList[idx]});
                    }
                    break;
            }
        };

        dispatchAddActionWatcher({id, actions:[MultiViewCntlr.UPDATE_VIEWER_CUSTOM_DATA,TBL_RESULTS_ACTIVE], callback:watcher});
        return () => {
            const tbl_id= getActiveTableId(activateParams.tableGroupViewerId);
            if (tbl_id) {
                const table= getTblById(tbl_id);
                lastTblId= table?.tbl_id;
            }
            dispatchCancelActionWatcher(id);
            deActivateAry.forEach((d) => d?.());
        };
    };
}

/**
 * passing an object with tbl_id as key and extraction function as value. Make a new extraction function
 * @param {Object} extractionObj
 * @param {ActivateParams} activateParams
 * @return {function(): function(): void}
 */
export function makeMultiTableExtraction(extractionObj,activateParams) {
    return () => {
        extractionObj[getActiveTableId(activateParams.tableGroupViewerId)]?.();
    };
}
