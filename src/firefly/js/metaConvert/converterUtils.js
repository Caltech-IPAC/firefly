/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty} from 'lodash';
import {ServerRequest} from '../data/ServerRequest.js';
import {getDefaultImageColorTable, WebPlotRequest} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {getCellValue, getTblInfo} from '../tables/TableUtil.js';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {
    dispatchTableRemove,
    dispatchTableSearch,
    TBL_UI_EXPANDED
} from '../tables/TablesCntlr';
import {MetaConst} from '../data/MetaConst';
import {dispatchChartAdd, dispatchChartRemove, CHART_UI_EXPANDED} from '../charts/ChartsCntlr';
import {getTblById, onTableLoaded} from '../tables/TableUtil';
import {ChartType} from '../data/FileAnalysis';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga';
import {SET_LAYOUT_MODE, LO_MODE, LO_VIEW} from '../core/LayoutCntlr.js';

const getSetInSrByRow= (table,sr,rowNum) => (col) => {
    sr.setSafeParam(col.name, getCellValue(table,rowNum,col.name));
};


export function createTableActivate(source, titleStr, activateParams, tbl_index=0) {

    return createChartTableActivate(false, source, {titleStr, showChartTitle:true}, activateParams,undefined,tbl_index);
}

const makeCommaSeparated= (strAry) => strAry.reduce( (str,d) => str? `${str},${d}` : d,'');



/**
 * @global
 * @public
 * @typedef {Object} chartInfo
 *
 *
 * @prop {string} xAxis
 * @prop {string} yAxis
 * @prop {ChartParams} chartParams
 *
 */

const loadedTablesIds= new Map();

function isTableChartNormalViewAction(payload, type) {
    const {mode, view, tbl_id} = payload;
    if (type !== SET_LAYOUT_MODE) return false;
    const normal = mode === LO_MODE.expanded && (view === LO_VIEW.none || (view !== LO_VIEW.tables && view !== LO_VIEW.xyPlots));
    if (normal) return true;
    return (mode === LO_MODE.standard && loadedTablesIds.has(tbl_id));
}

function makeTableRequest(source, titleInfo, tbl_id, tbl_index, colNames, colUnits) {
    const colNamesStr= colNames && makeCommaSeparated(colNames);
    const colUnitsStr= colUnits && makeCommaSeparated(colUnits);
    const dataTableReq= makeFileRequest(titleInfo.titleStr, source, undefined,
        {
            tbl_id,
            tbl_index,
            startIdx : 0,
            pageSize : 100,
            META_INFO : {
                [MetaConst.DATA_SOURCE] : 'false',
                [MetaConst.CATALOG_OVERLAY_TYPE]:'false'
            }
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
        dispatchCharts && dispatchCharts.forEach( (c) => dispatchChartAdd(c));
    });

    return () => {
        dispatchCancelActionWatcher(noopId);
        dispatchTableRemove(tbl_id,false);
        dispatchCharts && dispatchCharts.forEach( (c) => dispatchChartRemove(c.chartId));
    };
}


/**
 * Activate a chart and table
 * @param {boolean} chartAndTable - true - both char and table, false - table only
 * @param {String} source
 * @param {Object} titleInfo an object that has a titleStr and showchartTile properties
 * @param {ActivateParams} activateParams
 * @param {ChartInfo} chartInfo
 * @param {Number} tbl_index
 * @param {Array.<String>} colNames - an array of column names
 * @param {Array.<String>} colUnits - an array of types names
 * @param {String} chartId
 * @param {String} tbl_id
 * @return {function} the activate function
 */
export function createChartTableActivate(chartAndTable,source, titleInfo, activateParams, chartInfo={}, tbl_index=0,
                                         colNames= undefined, colUnits= undefined,
                                         chartId='part-result-chart', tbl_id= 'part-result-tbl') {
    return () => {
        const dispatchCharts=  chartAndTable && makeChartObj(chartInfo, activateParams,titleInfo,chartId,tbl_id);
        const dataTableReq= makeTableRequest(source,titleInfo,tbl_id,tbl_index,colNames,colUnits);
        const savedRequest= loadedTablesIds.has(tbl_id) && JSON.stringify(loadedTablesIds.get(tbl_id)?.request);

        if (savedRequest!==JSON.stringify(dataTableReq)) {
            const allChartIds= chartAndTable ? dispatchCharts.map( (c) => c.chartId) : [];
            const noopId = 'noop-' + tbl_id;
            dispatchAddActionWatcher({
                id: noopId,
                actions:[TBL_UI_EXPANDED, SET_LAYOUT_MODE, CHART_UI_EXPANDED],
                callback: ({payload,type}) => {
                    const tableInfo= loadedTablesIds.get(tbl_id);
                    if (isTableChartNormalViewAction(payload,type) && loadedTablesIds.has(tbl_id)) {
                        tableInfo.defereCleanup=true;
                    } else if (type === CHART_UI_EXPANDED && allChartIds?.includes(payload.chartId)) {
                        tableInfo.doCleanup=false;
                        tableInfo.defereCleanup=false;
                    } else if (type === TBL_UI_EXPANDED && payload.tbl_id === tbl_id) {
                        tableInfo.doCleanup=false;
                        tableInfo.defereCleanup=false;
                    }
                }
            });

            const {tableGroupViewerId}= activateParams;
            const cleanupFunc= loadTableAndCharts(dataTableReq,tbl_id,tableGroupViewerId,dispatchCharts, noopId);
            loadedTablesIds.set(tbl_id, {request:dataTableReq, doCleanup:true, deferCleanup:false, cleanupFunc});
        }

        return () => {
            const tableInfo= loadedTablesIds.get(tbl_id);
            if (tableInfo.doCleanup) {
                tableInfo.cleanupFunc();
                loadedTablesIds.delete(tbl_id);
            }
            else if (tableInfo.defereCleanup) {
                tableInfo.doCleanup= true;
            }
        };
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
    const {chartParamsAry}= chartInfo;
    const {xAxis, yAxis}= chartInfo;

    /* The table and chart title is the part's desc field. When the HDU does not have extname defined, the desc in the
     part is not defined.  In such case, the title is defined in PartAnalyzer is 'table_'+ HDU-index.  In order to change
     the table name to this title and now show the same title in the chart,  the titleInfo object is introduced. It tells
     if the showChartTitle or not.  if showChartTitle is false, the chart title is removed. 
     */
    const chartTitle= titleInfo.showChartTitle ?titleInfo.titleStr:'';
    if (chartParamsAry) {
        let chartNum=1;
        return chartParamsAry
            .map((chartParams) => makeChartFromParams(tbl_id, chartParams, xAxis, yAxis, chartTitle))
            .map((dataLayout) => ({viewerId, groupId: viewerId, chartId: `${chartId}--${chartNum++}`, ...dataLayout}));
    }
    else {
        return [ {
            viewerId, groupId: viewerId, chartId,
            data: [{
                    tbl_id,
                    x: `tables::${xAxis}`,
                    y: `tables::${yAxis}`,
                    mode: 'lines+markers'
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
                                                  xAxis, yAxis, tblRow= 0,tbl_index=0,
                                    chartId='part-result-chart',tbl_id= 'part-result-tbl') {
    return () => {
        const {tableGroupViewerId, chartViewerId}= activateParams;
        const dataTableReq= makeFileRequest(titleStr, source, undefined,
            {
                tbl_id,
                tbl_index,
                startIdx : 0,
                pageSize : 100,
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
            const xAry= getCellValue(table, tblRow, xAxis);
            const yAry= getCellValue(table, tblRow, yAxis);

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




/**
 *
 * @param table table data
 * @param {Array.<string>} colToUse columns from table
 * @param {Array.<string>} headerParams meta data parameters
 * @param {RangeValues} rv rangeValues
 * @param {number} colorTableId color table id
 * @return {function} see below, function takes plotId, reqKey,title, rowNum, extranParams and returns a WebPlotRequest
 *
 */
export function makeServerRequestBuilder(table, colToUse, headerParams, rv=null, colorTableId= getDefaultImageColorTable()) {
    /**
     * @param plotId - the plot id for the request
     * @param reqKey - search processor request key
     * @param title - title of plot
     * @param rowNum - get the row number of data in the table
     * @param extraParams can be an object with single key or an array of objects with single key
     * @return {WebPlotRequest}
     */
    return (plotId, reqKey, title, rowNum, extraParams) => {
        const sr= new ServerRequest(reqKey);
        if (typeof extraParams === 'object') {
            if (!Array.isArray(extraParams)) extraParams= [extraParams];
            extraParams.forEach( (p) => sr.setParam(p));
        }
        const {columns}= table.tableData;
        const {tableMeta:meta}= table;
        const setInSr= getSetInSrByRow(table,sr,rowNum);

        if (!Array.isArray(colToUse) && typeof colToUse === 'string') colToUse= [colToUse];
        if (!Array.isArray(headerParams) && typeof headerParams=== 'string') headerParams= [headerParams];


        if (isEmpty(colToUse) || colToUse[0].toUpperCase()==='ALL') {
            columns.forEach(setInSr);
        }
        else {
            columns.filter((c) => colToUse.includes(c.name)).forEach( setInSr);
        }

        if (!isEmpty(headerParams)) {
            if (headerParams[0].toUpperCase()==='ALL') {
                Object.keys(meta).forEach( (metaKey) => sr.setSafeParam(metaKey, meta[metaKey]) );
                
            }
            else {
                Object.keys(meta).filter( (m) => headerParams.includes(m) )
                    .forEach( (metaKey) => sr.setSafeParam(metaKey, meta[metaKey]) );
            }
        }
        const wpReq= WebPlotRequest.makeProcessorRequest(sr,title);
        // wpReq.setZoomType(ZoomType.FULL_SCREEN);
        wpReq.setInitialColorTable(colorTableId);
        wpReq.setTitle(title);
        wpReq.setPlotId(plotId);
        wpReq.setZoomType(ZoomType.TO_WIDTH);
        if (rv) wpReq.setInitialRangeValues(rv);
        return wpReq;
    };
}


export const computePlotId= (plotIdRoot ,plotIdx) => `${plotIdRoot}-row-${plotIdx}`;

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


