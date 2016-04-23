/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, get} from 'lodash';
import {getCellValue} from '../tables/TableUtil.js';
import {ServerRequest} from '../data/ServerRequest.js';
import {WebPlotRequest} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {findTblById,getTblInfo} from '../tables/TableUtil.js';
import {converterFactory} from './ConverterFactory.js';
import {MetaConst} from '../data/MetaConst.js';


const getSetInSrByRow= (table,sr,rowNum) => (col) => {
    sr.setSafeParam(col.name, getCellValue(table,rowNum,col.name));
};


/**
 *
 * @param table table data
 * @param colToUse columns from table
 * @param headerParams meta data parameters
 * @param rv rangeValues
 * @param colorTableId color table id
 * @return {function}
 */
export function makeServerRequestBuilder(table, colToUse, headerParams, rv=null, colorTableId=0) {
    /**
     * @param plotId
     * @param reqKey
     * @param title
     * @param rowNum
     * @param extraParams can be an object with single key or an array of objects with single key
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
 * @return {Array}
 */
export function findGridTableRows(table,maxRows, plotIdRoot) {

    const {startIdx, endIdx, highlightedRow}= getTblInfo(table, maxRows);

    var j= 0;
    const retval= [];

    for(var i=startIdx; (i<endIdx );i++) {
        retval[j++] = {plotId: computePlotId(plotIdRoot, i), row: i, highlight: i === highlightedRow};
    }
    return retval;
}

/**
 * Guess if this table contains image meta data
 * @param tbl_id
 * @return {boolean} true if there is image meta data
 */
export function isMetaDataTable(tbl_id) {
    const table= findTblById(tbl_id);
    const tableMeta= get(table, 'tableMeta');
    if (!tableMeta) return false;

    if (tableMeta[MetaConst.CATALOG_OVERLAY_TYPE] || tableMeta[MetaConst.CATALOG_COORD_COLS])  return false;
    const converter= converterFactory(table);
    return Boolean(converter);

}

