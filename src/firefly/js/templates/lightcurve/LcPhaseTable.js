/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set, slice, isArray, isString, cloneDeep, pick, omit} from 'lodash';
import {doUpload} from '../../ui/FileUpload.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {tableToIpac, makeFileRequest, getColumnIdx} from '../../tables/TableUtil.js';
import {LC, getFullRawTable, getConverterId} from './LcManager.js';
import {getLayouInfo} from '../../core/LayoutCntlr.js';
import {getConverter} from './LcConverterFactory.js';

const DEC_PHASE = 3;       // decimal digit

/**
 * @summary upload phase table with the column arranged as needed
 * @param {TableModel} tbl
 * @param {string} flux
 */
export function uploadPhaseTable(tbl, flux) {
    const {tbl_id, title} = tbl;
    const ipacTable = tableToIpac(tbl);
    const blob = new Blob([ipacTable]);
    //const file = new File([new Blob([ipacTable])], `${tbl_id}.tbl`);

    doUpload(blob).then(({status, cacheKey}) => {
        const tReq = makeFileRequest(title, cacheKey, null,
                                     {tbl_id,
                                      sortInfo:sortInfoString(LC.PHASE_CNAME),
                                      pageSize: LC.TABLE_PAGESIZE
                                     });

        dispatchTableSearch(tReq, {removable: true});
        const converterId =getConverterId(getLayouInfo());
        const plotTitle = getConverter(converterId).showPlotTitle?getConverter(converterId).showPlotTitle(LC.PHASE_FOLDED):'';
        const xyPlotParams = {
            userSetBoundaries: {xMax: 2},
            x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'},
            y: {columnOrExpr: flux, options: 'grid,flip'},
            plotTitle:plotTitle
        };
        loadXYPlot({chartId: tbl_id, tblId: tbl_id, xyPlotParams, help_id: 'main1TSV.plot'});
    });



}

/**
 * @summary adding phase column to raw table
 * @param {string} flux flux column name
 * @param {string} time time column name
 * @param {string} period periodt  value
 * @param {string} tzero zero time value
 * @return {TableModel} phase table
 */
export function doPFCalculate(flux, time, period, tzero) {
    const fullRawTable = getFullRawTable();

    var phaseFoldedTable = fullRawTable && addPhaseToTable(fullRawTable, time, tzero, period);

     if (phaseFoldedTable) {
         phaseFoldedTable = repeatDataCycle(phaseFoldedTable);
         uploadPhaseTable(phaseFoldedTable, flux);
    }
}

/**
 * @summary calculate phase and return as text
 * @param {number} time
 * @param {number} timeZero
 * @param {number} period
 * @param {number} dec
 * @returns {string}  folded phase (positive or negative) with 'dec'  decimal digits
 */
export function getPhase(time, timeZero, period,  dec=DEC_PHASE) {
    var q = (time - timeZero)/period;
    var p = q >= 0  ? (q - Math.floor(q)) : (q + Math.floor(-q) + 1.0);

    return p.toFixed(dec);
}

/**
 * @summary create a table model with phase column
 * @param {TableModel} tbl
 * @param {string} timeName
 * @param {string} tzero
 * @param {string} period
 * @returns {TableModel}
 */
function addPhaseToTable(tbl, timeName, tzero, period) {
    var tIdx = timeName ? getColumnIdx(tbl, timeName) : -1;

    if (tIdx < 0) return null;

    const tbl_id =  LC.PHASE_FOLDED;
    const title = 'Phase Folded Data';

    var tPF = {tableData: cloneDeep(tbl.tableData),
               tableMeta: cloneDeep(tbl.tableMeta),
               tbl_id, title};
    tPF.tableMeta = omit(tPF.tableMeta, ['source', 'datasetID', 'sortInfo', 'isFullyLoaded']);

    var phaseC = {desc: 'number of period elapsed since starting time.',
                  name: LC.PHASE_CNAME, type: 'double', width: 6 };

    tPF.tableData.columns.push(phaseC);          // add phase column

    tPF.tableData.data.forEach((d, index) => {   // add phase value (in string) to each data

        tPF.tableData.data[index].push(getPhase(parseFloat(d[tIdx]), parseFloat(tzero), parseFloat(period)));
    });

    const fluxCol = get(getLayouInfo(), [LC.MISSION_DATA, LC.META_FLUX_CNAME]);
    const fluxCols = get(getLayouInfo(), [LC.MISSION_DATA, LC.META_FLUX_NAMES], [fluxCol]);

    locateTableColumns(tPF, [timeName, LC.PHASE_CNAME,...fluxCols], 0);


    // add reference to raw_table original row
    var raw_rowid = get(tPF, 'tableData.columns', []).find((el) => el.name === 'ROW_IDX');
    if (raw_rowid) {
        raw_rowid.name = 'RAW_ROWID';
        raw_rowid.visibility = 'hidden';
    } else {
        raw_rowid = {name: 'RAW_ROWID', type: 'int', visibility: 'hidden'};
        tPF.tableData.columns.push(raw_rowid);
        tPF.tableData.data.forEach((row, index) => {
            row.push(index);
        });
    }

    return tPF;
}


/**
 * @summary change the content of each row of the table by relocating some columns
 *          to be after or replace certain column
 * @param {TableModel} tbl
 * @param {array|string} srcCols -- a string or an array of strings
 * @param {number|string} targetCol -- a string indicating column name or a number indicating column index
 * @param {number} position 1: after the target columm, -1: before the target column, 0: at the target column
 */
function locateTableColumns(tbl, srcCols, targetCol, position = 1) {
    var {columns, data} = tbl.tableData;

    if (!isArray(srcCols)) {
        srcCols = [srcCols];
    }

    var colIdx = [];
    var cols = srcCols.reduce((prev, srcCol) => {
            var idx = getColumnIdx(tbl, srcCol);

            if (idx >= 0) {
                prev.push(columns[idx]);
                columns.splice(idx, 1);
                colIdx.push(idx);
            }
            return prev;
        }, []);


    var targetIdx = isString(targetCol) ? getColumnIdx(tbl, targetCol) : targetCol;
    if (targetIdx < 0) {
        targetIdx = 0;
        position = 1;
    }

    if (targetIdx >= 0) {
        var deleteCount;

        if (position > 0) {
            targetIdx += 1;
            deleteCount = 0;
        } else if (position < 0) {
            deleteCount = 0;
        } else {
            deleteCount = 1;
        }

        columns.splice( targetIdx, deleteCount,...cols);

        data.forEach((dataRow) => {
                var content = colIdx.reduce((prev, idx) => {
                    prev.push(dataRow[idx]);
                    dataRow.splice(idx, 1);
                    return prev;
                }, []);
                dataRow.splice(targetIdx, deleteCount,...content);
            });

        set(tbl.tableData, ['columns'], columns);
        set(tbl.tableData, ['data'], data);
    }

}

/**
 * @summary duplicate the phase cycle to the phase folded table
 * @param {TableModel} phaseTable
 */
function repeatDataCycle(phaseTable) {
    var {tableData, tbl_id, title, tableMeta} = phaseTable;
    var fIdx = getColumnIdx(phaseTable, LC.PHASE_CNAME);

    slice(tableData.data, 0).forEach((d) => {
        var newRow = slice(d);

        newRow[fIdx] = `${parseFloat(d[fIdx]) + 1}`;
        tableData.data.push(newRow);
    });

    var totalRows = tableData.data.length;
    /* TODO: investigate to pick the needed properties under tableMeta */
    /*
    var newTableMeta = pick(tableMeta, ['fixlen', 'QueryTime', 'ORIGIN',
                                        'DATETIME', 'DataTag','DATABASE',
                                        'EQUINOX', 'SKYAREA', 'StatusFile', 'SQL']);
    */
    set(phaseTable, ['tableMeta', 'RowsRetrieved'], `${totalRows}`);
    set(phaseTable, ['tableMeta', 'tbl_id'], tbl_id);
    set(phaseTable, ['tableMeta', 'title'], title);
    set(phaseTable, ['tableMeta', 'SQL'],`SELECT (${tableData.columns.length} column names follow in next row.)`);
    return phaseTable;
}
