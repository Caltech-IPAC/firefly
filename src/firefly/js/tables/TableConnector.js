/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, omitBy, isUndefined, cloneDeep, get, range, memoize} from 'lodash';
import * as TblCntlr from './TablesCntlr.js';
import {getTblById, getTblInfoById} from './TableUtil.js';
import {SelectInfo} from './SelectInfo.js';
import {getTableUiById} from './TableUtil';




export const makeConnector = memoize((tbl_id, tbl_ui_id) => {
    return {
        onSort: (sortInfoString) => onSort(tbl_id, sortInfoString),
        onFilter: (filterIntoString) => onFilter(tbl_id, filterIntoString),
        onFilterSelected: (selected) => onFilterSelected(tbl_id, selected),
        onPageSizeChange: (nPageSize) => onPageSizeChange(tbl_id, nPageSize),
        onGotoPage: (number) => onGotoPage(tbl_id, number),
        onRowHighlight: (rowIdx) => onRowHighlight(tbl_id, rowIdx),
        onSelectAll: (checked) => onSelectAll(tbl_id, checked),
        onRowSelect: (checked, rowIndex) => onRowSelect(tbl_id, checked, rowIndex),
        onToggleTextView: (textView) => onToggleTextView(tbl_ui_id, textView),
        onToggleOptionsz: (showOptions) => onToggleOptions(tbl_ui_id, showOptions),
        onOptionUpdate: (changes) => onOptionUpdate( {...changes, tbl_id, tbl_ui_id}),
        onOptionReset: (original) => onOptionReset(tbl_id, tbl_ui_id, original),
    };
});




export function onSort(tbl_id, sortInfoString) {
    TblCntlr.dispatchTableSort({tbl_id, sortInfo: sortInfoString});
}

export function onFilter(tbl_id, filterIntoString) {
    TblCntlr.dispatchTableFilter({tbl_id, filters: filterIntoString});
}

/**
 * filter on the selected rows
 * @param {string} tbl_id  table ID
 * @param {number[]} selected  array of selected row indices.
 */
export function onFilterSelected(tbl_id, selected) {
    if (isEmpty(selected)) return;
    const {request} = getTblInfoById(tbl_id);
    TblCntlr.dispatchTableFilterSelrow(request, selected);
}

export function onPageSizeChange(tbl_id, nPageSize) {
    nPageSize = Number.parseInt(nPageSize);
    const {pageSize, highlightedRow} = getTblInfoById(tbl_id);
    if (Number.isInteger(nPageSize) && nPageSize !== pageSize) {
        const request = {tbl_id, pageSize: nPageSize};
        TblCntlr.dispatchTableHighlight(tbl_id, highlightedRow, request);
    }
}

export function onGotoPage(tbl_id, number = '1') {
    const {currentPage, totalPages, pageSize} = getTblInfoById(tbl_id);
    number = Number.parseInt(number);
    if (Number.isInteger(number) && number !== currentPage && number > 0 && number <= totalPages) {
        const highlightedRow = (number - 1) * pageSize;
        TblCntlr.dispatchTableHighlight(tbl_id, highlightedRow);
    }
}

export function onRowHighlight(tbl_id, rowIdx) {
    const {hlRowIdx, startIdx} = getTblInfoById(tbl_id);
    if (rowIdx !== hlRowIdx) {
        const highlightedRow = startIdx + rowIdx;
        TblCntlr.dispatchTableHighlight(tbl_id, highlightedRow);
    }
}

export function onSelectAll(tbl_id, checked) {
    const {startIdx, tableModel} = getTblInfoById(tbl_id);
    const selectInfo = tableModel.selectInfo ? cloneDeep(tableModel.selectInfo) : {};
    const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
    selectInfoCls.setSelectAll(checked);
    TblCntlr.dispatchTableSelect(tbl_id, selectInfoCls.data);
}

export function onRowSelect(tbl_id, checked, rowIndex) {
    const {tableModel, startIdx} = getTblInfoById(tbl_id);
    const selectInfo = tableModel.selectInfo ? cloneDeep(tableModel.selectInfo) : {};
    const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
    selectInfoCls.setRowSelect(rowIndex, checked);
    TblCntlr.dispatchTableSelect(tbl_id, selectInfoCls.data);
}

export function onToggleTextView(tbl_ui_id, textView) {
    const changes = {tbl_ui_id, textView};
    TblCntlr.dispatchTableUiUpdate(changes);
}

export function onToggleOptions(tbl_ui_id, showOptions) {
    const changes = {tbl_ui_id, showOptions};
    TblCntlr.dispatchTableUiUpdate(changes);
}

export function onOptionUpdate({tbl_id, tbl_ui_id, pageSize, columns, showUnits, showTypes, showFilters, sortInfo, filterInfo, sqlFilter, resetColWidth}) {
    if (pageSize) {
        onPageSizeChange(tbl_id, pageSize);
    }
    if (!isUndefined(filterInfo)) {
        onFilter(tbl_id, filterInfo);
    }
    if (!isUndefined(sqlFilter)) {
        TblCntlr.dispatchTableFilter({tbl_id, sqlFilter});
    }

    const changes = omitBy({columns, showUnits, showTypes, showFilters, optSortInfo:sortInfo}, isUndefined);

    if (columns) {
        const {columns:prevColumns, columnWidths, scrollLeft} = getTableUiById(tbl_ui_id);
        const updColIdx = columns.findIndex((c, idx) => c.visibility !== get(prevColumns, [idx, 'visibility']));
        if (updColIdx >= 0) {
            const updColPos = range(0, updColIdx).reduce((pv, idx) => {
                const delta =  get(columns, [idx, 'visibility'], 'show') === 'show' ?  columnWidths[idx] : 0;
                return pv + delta;
            }, 0);
            if (updColPos < scrollLeft) {
                if (get(columns, [updColIdx, 'visibility'], 'show') === 'show') {
                    changes.scrollLeft = scrollLeft + columnWidths[updColIdx];
                } else {
                    changes.scrollLeft = scrollLeft - columnWidths[updColIdx];
                }
            }
        }
    }

    if (resetColWidth) changes.columnWidths = undefined;

    if (!isEmpty(changes)) {
        changes.tbl_ui_id = tbl_ui_id;
        TblCntlr.dispatchTableUiUpdate(changes);
    }
}

export function onOptionReset(tbl_id, tbl_ui_id, original) {
    const ctable = getTblById(tbl_id);
    const {showUnits, showFilters, showTypes} = original;

    const pageSize = ctable?.request?.pageSize !== original.pageSize ? original.pageSize : undefined;       // only set if it has changed
    onOptionUpdate({tbl_id, tbl_ui_id, pageSize, resetColWidth: true,
                    columns: cloneDeep(get(ctable, 'tableData.columns', [])),
                    showUnits, showTypes, showFilters});

    const {filters, sqlFilter}= ctable?.request;
    if (filters || sqlFilter) {
        TblCntlr.dispatchTableFilter({tbl_id, sqlFilter: '', filters: ''});
    }
}


