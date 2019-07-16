/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, omitBy, isUndefined, cloneDeep, get, set, range} from 'lodash';
import * as TblCntlr from './TablesCntlr.js';
import {getTableUiByTblId, getTblById, getTblInfoById} from './TableUtil.js';
import {SelectInfo} from './SelectInfo.js';
import {getTableUiById} from './TableUtil';

export class TableConnector {
    
    constructor(tbl_id, tbl_ui_id, tableModel, options) {

        this.tbl_id = tbl_id || tableModel.tbl_id;
        this.tbl_ui_id = tbl_ui_id;
        this.tableModel = tableModel;
        this.options = options;

        const {pageSize, showUnits=true, showFilters=false, showTypes=false} = options || {};
        this.orig = {showUnits, showTypes, showFilters, pageSize};
    }

    onMount() {
        const {tbl_ui_id, tbl_id, tableModel} = this;
        if (!getTableUiByTblId(tbl_id)) {
            TblCntlr.dispatchTableUiUpdate({tbl_ui_id, tbl_id, ...this.options});
            if (tableModel && !tableModel.origTableModel) {
                set(tableModel, 'request.tbl_id', tbl_id);
                TblCntlr.dispatchTableAddLocal(tableModel, undefined, false);
            }
        }
    }

    onSort(sortInfoString) {
        TblCntlr.dispatchTableSort({tbl_id: this.tbl_id, sortInfo: sortInfoString});
    }

    onFilter(filterIntoString) {
        TblCntlr.dispatchTableFilter({tbl_id: this.tbl_id, filters: filterIntoString});
    }

    /**
     * filter on the selected rows
     * @param {number[]} selected  array of selected row indices.
     */
    onFilterSelected(selected) {
        if (isEmpty(selected)) return;
        const {request} = getTblInfoById(this.tbl_id);
        TblCntlr.dispatchTableFilterSelrow(request, selected);
    }

    onPageSizeChange(nPageSize) {
        nPageSize = Number.parseInt(nPageSize);
        const {pageSize, highlightedRow} = getTblInfoById(this.tbl_id);
        if (Number.isInteger(nPageSize) && nPageSize !== pageSize) {
            const request = {pageSize: nPageSize};
            TblCntlr.dispatchTableHighlight(this.tbl_id, highlightedRow, request);
        }
    }

    onGotoPage(number = '1') {
        const {currentPage, totalPages, pageSize} = getTblInfoById(this.tbl_id);
        number = Number.parseInt(number);
        if (Number.isInteger(number) && number !== currentPage && number > 0 && number <= totalPages) {
            const highlightedRow = (number - 1) * pageSize;
            TblCntlr.dispatchTableHighlight(this.tbl_id, highlightedRow);
        }
    }

    onRowHighlight(rowIdx) {
        const {hlRowIdx, startIdx} = getTblInfoById(this.tbl_id);
        if (rowIdx !== hlRowIdx) {
            const highlightedRow = startIdx + rowIdx;
            TblCntlr.dispatchTableHighlight(this.tbl_id, highlightedRow);
        }
    }

    onSelectAll(checked) {
        const {startIdx, tableModel} = getTblInfoById(this.tbl_id);
        const selectInfo = tableModel.selectInfo ? cloneDeep(tableModel.selectInfo) : {};
        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        selectInfoCls.setSelectAll(checked);
        TblCntlr.dispatchTableSelect(this.tbl_id, selectInfoCls.data);
    }

    onRowSelect(checked, rowIndex) {
        const {tableModel, startIdx} = getTblInfoById(this.tbl_id);
        const selectInfo = tableModel.selectInfo ? cloneDeep(tableModel.selectInfo) : {};
        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        selectInfoCls.setRowSelect(rowIndex, checked);
        TblCntlr.dispatchTableSelect(this.tbl_id, selectInfoCls.data);
    }

    onToggleTextView(textView) {
        const changes = {tbl_ui_id:this.tbl_ui_id, textView};
        TblCntlr.dispatchTableUiUpdate(changes);
    }

    onToggleOptions(showOptions) {
        const changes = {tbl_ui_id:this.tbl_ui_id, showOptions};
        TblCntlr.dispatchTableUiUpdate(changes);
    }

    onOptionUpdate({pageSize, columns, showUnits, showTypes, showFilters, sortInfo, filterInfo}) {
        if (pageSize) {
            this.onPageSizeChange(pageSize);
        }
        if (!isUndefined(filterInfo)) {
            this.onFilter(filterInfo);
        }

        const changes = omitBy({columns, showUnits, showTypes, showFilters, optSortInfo:sortInfo}, isUndefined);

        if (columns) {
            const {columns:prevColumns, columnWidths, scrollLeft} = getTableUiById(this.tbl_ui_id);
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

        if (!isEmpty(changes)) {
            changes.tbl_ui_id = this.tbl_ui_id;
            TblCntlr.dispatchTableUiUpdate(changes);
        }
    }

    onOptionReset() {
        const ctable = getTblById(this.tbl_id);
        var filterInfo = get(ctable, 'request.filters', '').trim();
        filterInfo = filterInfo !== '' ? '' : undefined;
        var {showUnits, showTypes, showFilters, pageSize} = this.orig || {};
        
        pageSize = get(ctable, 'request.pageSize') !== pageSize ? pageSize : undefined;
        this.onOptionUpdate({filterInfo, pageSize,
                        columns: cloneDeep(get(ctable, 'tableData.columns', [])),
                        showUnits, showTypes, showFilters});
    }

    static newInstance(tbl_id, tbl_ui_id, tableModel, options) {
        return new TableConnector(tbl_id, tbl_ui_id, tableModel, options);
    }
}
