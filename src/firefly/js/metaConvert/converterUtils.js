/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty} from 'lodash';
import {ServerRequest} from '../data/ServerRequest.js';
import {WebPlotRequest} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {getCellValue, getTblInfo} from '../tables/TableUtil.js';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {
    dispatchTableFetch,
    dispatchTableLoaded,
    dispatchTableRemove,
    dispatchTableSearch,
    TABLE_FETCH
} from '../tables/TablesCntlr';
import {MetaConst} from '../data/MetaConst';
import {LC} from '../templates/lightcurve/LcManager';
import {dispatchChartAdd} from '../charts/ChartsCntlr';
import {getTblById, default as TblUtil, onTableLoaded} from '../tables/TableUtil';

const getSetInSrByRow= (table,sr,rowNum) => (col) => {
    sr.setSafeParam(col.name, getCellValue(table,rowNum,col.name));
};



/**
 *
 * @param {String} source - a file on the server or a url
 * @param {String} titleStr
 * @param {ActivateParams} activateParams
 * @param {number} [tbl_index]
 * @param {number} [activateOnce] if true the only create this table id if it does not exist
 * @param {String} [tbl_id]
 * @return {function}
 */
export function createTableActivate(source, titleStr, activateParams, tbl_index=0, activateOnce=false, tbl_id= 'part-result-tbl') {
    return () => {
        if (activateOnce && getTblById(tbl_id)) return;
        const {tableGroupViewerId}= activateParams;
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
        dispatchTableSearch(dataTableReq,
            {
                logHistory: false,
                removable:false,
                tbl_group: tableGroupViewerId,
                backgroundable: false,
                showFilters: true,
                showInfoButton: true
            });
        return () => {
            dispatchTableRemove(tbl_id,false);
        };
    };
}

export function createChartActivate(source, titleStr, activateParams, xAxis, yAxis, tbl_index=0,activateOnce=false,
                                    chartId='part-result-chart',tbl_id= 'part-result-tbl') {
    return () => {
        if (activateOnce && getTblById(tbl_id)) return;
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

        const dispatchParams= {
            viewerId: chartViewerId,
            groupId: chartViewerId,
            chartId,
            data: [{
                tbl_id,
                x: `tables::${xAxis}`,
                y: `tables::${yAxis}`,
                mode: 'lines+markers'
            }],
            layout: {
                title: {text: titleStr},
                xaxis: {showgrid: true},
                yaxis: {showgrid: true, range: [0, undefined]},
            }
        };
        onTableLoaded(tbl_id).then( () => {
            dispatchChartAdd(dispatchParams);
        });
        return () => {
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
export function makeServerRequestBuilder(table, colToUse, headerParams, rv=null, colorTableId=0) {
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


