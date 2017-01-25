/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {cloneDeep, get, set, omit, slice, replace, isArray, isString} from 'lodash';
import {doUpload} from '../../ui/FileUpload.jsx';
import {loadXYPlot} from '../../charts/dataTypes/XYColsCDT.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {makeTblRequest,getTblById, getTblIdsByGroup, tableToText, makeFileRequest} from '../../tables/TableUtil.js';
import {dispatchTableReplace} from '../../tables/TablesCntlr.js';
import {LC, getValidValueFrom} from './LcManager.js';
import {getLayouInfo} from '../../core/LayoutCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';


const DEC_PHASE = 3;       // decimal digit

/**
 * @summary upload phase table with the column arranged as needed
 * @param {TableModel} tbl
 * @param {string} flux
 */
export function uploadPhaseTable(tbl, flux) {
    const {tableData, tbl_id, tableMeta, title} = tbl;

    const ipacTable = tableToText(tableData.columns, tableData.data, true, tableMeta);
    const file = new File([new Blob([ipacTable])], `${tbl_id}.tbl`);

    doUpload(file).then(({status, cacheKey}) => {
        const tReq = makeFileRequest(title, cacheKey, null, {tbl_id, sortInfo:sortInfoString(LC.PHASE_CNAME)});
        dispatchTableSearch(tReq, {removable: true});

        const xyPlotParams = {
            userSetBoundaries: {xMax: 2},
            x: {columnOrExpr: LC.PHASE_CNAME, options: 'grid'},
            y: {columnOrExpr: flux, options: 'grid,flip'}
        };
        loadXYPlot({chartId: tbl_id, tblId: tbl_id, xyPlotParams});
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
    const rawTable = getTblById(LC.RAW_TABLE);

    var phaseFoldedTable = rawTable && addPhaseToTable(rawTable, time, flux, tzero, period);
    phaseFoldedTable&&repeatDataCycle(time, parseFloat(period), phaseFoldedTable);

    return phaseFoldedTable;
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
    var p = q >= 0  ? (q - Math.floor(q)) : (q + Math.floor(-q));

    return p.toFixed(dec);
}

/**
 * @summary create a table model with phase column
 * @param {TableModel} tbl
 * @param {string} timeName
 * @param {string} fluxName
 * @param {string} tzero
 * @param {string} period
 * @returns {TableModel}
 */
function addPhaseToTable(tbl, timeName, fluxName, tzero, period) {
    var {columns} = tbl.tableData;
    var tIdx = timeName ? columns.findIndex((c) => (c.name === timeName)) : -1;

    if (tIdx < 0) return null;

    var tPF = Object.assign(cloneDeep(tbl), {tbl_id: LC.PHASE_FOLDED, title: 'Phase Folded'},
                                            {request: getPhaseFoldingRequest(period, timeName, fluxName, tbl)},
                                            {highlightedRow: 0});
    tPF = omit(tPF, ['hlRowIdx', 'isFetching']);

    var phaseC = {desc: 'number of period elapsed since starting time.',
                  name: LC.PHASE_CNAME, type: 'double', width: 6 };

    tPF.tableData.columns.push(phaseC);          // add phase column

    tPF.tableData.data.forEach((d, index) => {   // add phase value (in string) to each data

        tPF.tableData.data[index].push(getPhase(parseFloat(d[tIdx]), parseFloat(tzero), parseFloat(period)));
    });

    const fluxCols = get(getLayouInfo(), [LC.MISSION_DATA, LC.META_FLUX_NAMES]);

    locateTableColumns(tPF, [timeName, LC.PHASE_CNAME,...fluxCols], 0);
    return tPF;
}


/**
 * @summary change the content of each row of the table by relocating some columns
 *          to be after or replace certain column
 * @param {TableModel} tbl
 * @param {array|string} srcCols
 * @param {number|string} targetCol
 * @param {number} position 1: after the target columm, -1: before the target column, 0: at the target column
 */
function locateTableColumns(tbl, srcCols, targetCol, position = 1) {
    var {columns, data} = tbl.tableData;

    if (!isArray(srcCols)) {
        srcCols = [srcCols];
    }

    var colIdx = [];
    var cols = srcCols.reduce((prev, srcCol) => {
            var idx = columns.findIndex((col) => (col.name === srcCol));

            if (idx >= 0) {
                prev.push(columns[idx]);
                columns.splice(idx, 1);
                colIdx.push(idx);
            }
            return prev;
        }, []);


    var targetIdx = isString(targetCol) ? columns.findIndex((col) => (col.name === targetCol)) : targetCol;
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
 * @summary create table request object for phase folded table
 * @param {string} period period in string
 * @param {string} time  time column
 * @param {string} flux flux name
 * @param {TableModel} tbl
 * @returns {TableRequest}
 */
function getPhaseFoldingRequest(period, time, flux, tbl) {
    const cutoutSize = getValidValueFrom(FieldGroupUtils.getGroupFields(LC.FG_VIEWER_FINDER), 'cutoutSize');

    return makeTblRequest('PhaseFoldedProcessor', LC.PHASE_FOLDED, {
        period_days: period,
        table_name: 'folded_table',
        cutout_size: cutoutSize ? cutoutSize : undefined,
        flux,
        x: time,
        original_table: tbl.tableMeta.tblFilePath
    },  {tbl_id:LC.PHASE_FOLDED});

}

/**
 * @summary duplicate the phase cycle to the phase folded table
 * @param {string} timeCol
 * @param {number} period
 * @param {TableModel} phaseTable
 */
function repeatDataCycle(timeCol, period, phaseTable) {
    var tIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === timeCol));
    var fIdx = phaseTable.tableData.columns.findIndex((c) => (c.name === LC.PHASE_CNAME));
    var {totalRows, tbl_id, title} = phaseTable;


    slice(phaseTable.tableData.data, 0, totalRows).forEach((d) => {
        var newRow = slice(d);

        newRow[tIdx] = `${parseFloat(d[tIdx]) + period}`;
        newRow[fIdx] = `${parseFloat(d[fIdx]) + 1}`;
        phaseTable.tableData.data.push(newRow);
    });

    totalRows *= 2;

    set(phaseTable, 'totalRows', totalRows);
    set(phaseTable, ['tableMeta', 'RowsRetrieved'], `${totalRows}`);

    set(phaseTable, ['tableMeta', 'tbl_id'], tbl_id);
    set(phaseTable, ['tableMeta', 'title'], title);

    var col = phaseTable.tableData.columns.length;
    var sqlMeta = get(phaseTable, ['tableMeta', 'SQL']);
    if (sqlMeta) {
        set(phaseTable, ['tableMeta', 'SQL'], replace(sqlMeta, `${col-1}`, `${col}`));
    }
}
