/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getDefaultChartProps} from 'firefly/charts/ChartUtil.js';
import {showPinMessage} from 'firefly/ui/PopupUtil.jsx';
import {isString} from 'lodash';
import {CHART_UI_EXPANDED, dispatchChartAdd, dispatchChartRemove} from '../charts/ChartsCntlr';
import {LO_MODE, LO_VIEW, SET_LAYOUT_MODE} from '../core/LayoutCntlr.js';
import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../core/MasterSaga';
import {ChartType} from '../data/FileAnalysis';
import {MetaConst} from '../data/MetaConst';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {
    dispatchActiveTableChanged, dispatchTableRemove, dispatchTableSearch, TBL_RESULTS_ACTIVE, TBL_UI_EXPANDED
} from '../tables/TablesCntlr';
import {getActiveTableId, getTblById, onTableLoaded} from '../tables/TableUtil';
import {getCellValue, getTblInfo} from '../tables/TableUtil.js';
import MultiViewCntlr, {dispatchUpdateCustom, getMultiViewRoot, getViewer} from '../visualize/MultiViewCntlr.js';


export function createTableActivate(source, titleStr, activateParams, dataTypeHint= '', tbl_index=0) {

    return createChartTableActivate({source, titleInfo:{titleStr, showChartTitle:true},
        activateParams,undefined,tbl_index, dataTypeHint});
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

/**
 *
 * @param {String} source
 * @param {Object} titleInfo
 * @param {String} tbl_id
 * @param {number} tbl_index
 * @param {Array.<string>} colNames
 * @param {Array.<string>} colUnits
 * @param {number} cubePlane - plane of cube - ignored for non-cubes
 * @param {String} dataTypeHint
 * @param {boolean} extraction
 * @return {TableRequest}
 */
function makeTableRequest(source, titleInfo, tbl_id, tbl_index, colNames, colUnits, cubePlane, dataTypeHint, extraction=false) {
    const colNamesStr= colNames && makeCommaSeparated(colNames);
    const colUnitsStr= colUnits && makeCommaSeparated(colUnits);
    const META_INFO= !extraction ?
        {
            [MetaConst.DATA_SOURCE] : 'false',
            [MetaConst.CATALOG_OVERLAY_TYPE]:'false'
        } : {};
    if (dataTypeHint) META_INFO[MetaConst.DATA_TYPE_HINT]= dataTypeHint;
    const dataTableReq= makeFileRequest(titleInfo.titleStr, source, undefined,
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
        dispatchCancelActionWatcher(noopId);
        dispatchTableRemove(tbl_id,false);
        dispatchCharts && dispatchCharts.forEach( (c) => dispatchChartRemove(c.chartId));
    };
}

export function createTableExtraction(source,titleInfo,tbl_index,colNames,colUnits,cubePlane=0,dataTypeHint) {
    return () => {
        const ti= isString(titleInfo) ? {titleStr:titleInfo} : titleInfo;
        const dataTableReq= makeTableRequest(source,ti,undefined,tbl_index,colNames,colUnits,cubePlane,dataTypeHint, true);
        dispatchTableSearch(dataTableReq,
            { setAsActive: false, logHistory: false, showFilters: true, showInfoButton: true });
        showPinMessage('Pinning to Table Area');
    };
}



/**
 * Activate a chart and table
 * @param {Object} p
 * @param {boolean} [p.chartAndTable] - true - both char and table, false - table only
 * @param {String} p.source
 * @param {{titleString:String,showChartTitle:boolean}} p.titleInfo an object that has a titleStr and showchartTile properties
 * @param {ActivateParams} p.activateParams
 * @param {ChartInfo} [p.chartInfo]
 * @param {Number} p.tbl_index
 * @param {String} p.dataTypeHint  stuff like 'spectrum', 'image', 'cube', etc
 * @param {Array.<String>} p.colNames - an array of column names
 * @param {Array.<String>} p.colUnits - an array of types names
 * @param {boolean} [p.connectPoints] if a default scatter chart then connect the points
 * @param {number} [p.cubePlane] - plane of cube - ignored for non-cubes
 * @param {String} [p.chartId]
 * @param {String} [p.tbl_id]
 * @return {function} the activate function
 */
export function createChartTableActivate({chartAndTable=false,
                                             source, titleInfo, activateParams, chartInfo={},
                                         tbl_index=0, dataTypeHint,cubePlane=0,
                                         colNames= undefined, colUnits= undefined, connectPoints=true,
                                         chartId='part-result-chart', tbl_id= 'part-result-tbl'}) {
    return () => {
        const dispatchCharts=  chartAndTable && makeChartObj(chartInfo, activateParams,titleInfo,connectPoints,chartId,tbl_id);
        const dataTableReq= makeTableRequest(source,titleInfo,tbl_id,tbl_index,colNames,colUnits,cubePlane,dataTypeHint, false);
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



function makeChartObj(chartInfo,  activateParams, titleInfo, connectPoints, chartId, tbl_id ) {

    const {chartViewerId:viewerId}= activateParams;
    const {chartParamsAry}= chartInfo;
    const {xAxis, yAxis, useChartChooser}= chartInfo;

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
    else if (useChartChooser) {
        const obj= [{ viewerId, groupId: viewerId, chartId,xAxis, yAxis, useChartChooser: true }];
        if (chartTitle) obj[0].title= {text: titleInfo.titleStr};
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
                                                  xAxis, yAxis, tblRow= 0,tbl_index=0,
                                                  dataTypeHint,
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
                lastTblId= table.tbl_id;
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
