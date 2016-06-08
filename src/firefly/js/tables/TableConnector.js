/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, omitBy, isUndefined, cloneDeep, get} from 'lodash';
import {flux} from '../Firefly.js';
import * as TblCntlr from './TablesCntlr.js';
import * as TblUtil from './TableUtil.js';
import {SelectInfo} from './SelectInfo.js';
import {FilterInfo} from './FilterInfo.js';
import {fetchUrl} from '../util/WebUtil.js';

export class TableConnector {
    
    constructor(tbl_id, tbl_ui_id, tableModel) {
        this.tbl_id = tbl_id;
        this.tbl_ui_id = tbl_ui_id;
        this.origTableModel = tableModel;
    }

    onSort(sortInfoString) {
        var {tableModel, request} = TblUtil.getTblInfoById(this.tbl_id);
        if (this.origTableModel) {
            tableModel = TblUtil.sortTable(this.origTableModel, sortInfoString);
            flux.process({type: TblCntlr.TABLE_REPLACE, payload: tableModel});
        } else {
            request = Object.assign({}, request, {sortInfo: sortInfoString});
            TblCntlr.dispatchTableSort(request);
        }
    }

    onFilter(filterIntoString) {
        var {tableModel, request} = TblUtil.getTblInfoById(this.tbl_id);
        if (this.origTableModel) {
            // not implemented yet
        } else {
            request = Object.assign({}, request, {filters: filterIntoString});
            TblCntlr.dispatchTableFetch(request);
        }
    }

    /**
     * filter on the selected rows
     * @param {number[]} selected  array of selected row indices.
     */
    onFilterSelected(selected) {
        var {tableModel, request} = TblUtil.getTblInfoById(this.tbl_id);
        if (this.origTableModel) {
            // not implemented yet
        } else {
            const filterInfoCls = FilterInfo.parse(request.filters);
            const filePath = get(tableModel, 'tableMeta.source');
            if (filePath) {
                getRowIdFor(filePath, selected).then( (selectedRowIdAry) => {
                    const value = selectedRowIdAry.reduce((rv, val, idx) => {
                            return rv + (idx ? ',':'') + val;
                        }, 'IN (') + ')';
                    filterInfoCls.addFilter('ROWID', value);
                    request = Object.assign({}, request, {filters: filterInfoCls.serialize()});
                    TblCntlr.dispatchTableFetch(request);
                });
            }
        }
    }

    onPageSizeChange(nPageSize) {
        nPageSize = Number.parseInt(nPageSize);
        const {pageSize, highlightedRow} = TblUtil.getTblInfoById(this.tbl_id);
        if (Number.isInteger(nPageSize) && nPageSize !== pageSize) {
            const request = {pageSize: nPageSize};
            TblCntlr.dispatchTableHighlight(this.tbl_id, highlightedRow, request);
        }
    }

    onGotoPage(number = '1') {
        const {currentPage, totalPages, pageSize} = TblUtil.getTblInfoById(this.tbl_id);
        number = Number.parseInt(number);
        if (Number.isInteger(number) && number !== currentPage && number > 0 && number <= totalPages) {
            const highlightedRow = (number - 1) * pageSize;
            TblCntlr.dispatchTableHighlight(this.tbl_id, highlightedRow);
        }
    }

    onRowHighlight(rowIdx) {
        const {hlRowIdx, startIdx} = TblUtil.getTblInfoById(this.tbl_id);
        if (rowIdx !== hlRowIdx) {
            const highlightedRow = startIdx + rowIdx;
            TblCntlr.dispatchTableHighlight(this.tbl_id, highlightedRow);
        }
    }

    onSelectAll(checked) {
        const {startIdx, tableModel} = TblUtil.getTblInfoById(this.tbl_id);
        const selectInfo = tableModel.selectInfo ? cloneDeep(tableModel.selectInfo) : {};
        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        selectInfoCls.setSelectAll(checked);
        TblCntlr.dispatchTableSelect(this.tbl_id, selectInfoCls.data);
    }

    onRowSelect(checked, rowIndex) {
        const {tableModel, startIdx} = TblUtil.getTblInfoById(this.tbl_id);
        const selectInfo = tableModel.selectInfo ? cloneDeep(tableModel.selectInfo) : {};
        const selectInfoCls = SelectInfo.newInstance(selectInfo, startIdx);
        selectInfoCls.setRowSelect(rowIndex, checked);
        TblCntlr.dispatchTableSelect(this.tbl_id, selectInfoCls.data);
    }

    onOptionUpdate({pageSize, columns, showUnits, showFilters, colSortDir}) {
        if (pageSize) {
            this.onPageSizeChange(pageSize);
        }
        const changes = omitBy({columns, showUnits, showFilters, colSortDir}, isUndefined);
        if (!isEmpty(changes)) {
            changes.tbl_ui_id = this.tbl_ui_id;
            TblCntlr.dispatchTableUiUpdate(changes);
        }
    }

    onToggleTextView(textView) {
        const changes = {tbl_ui_id:this.tbl_ui_id, textView};
        TblCntlr.dispatchTableUiUpdate(changes);
    }

    onToggleOptions(showOptions) {
        const changes = {tbl_ui_id:this.tbl_ui_id, showOptions};
        TblCntlr.dispatchTableUiUpdate(changes);
    }

    static newInstance(tbl_id, tbl_ui_id, tableModel) {
        return new TableConnector(tbl_id, tbl_ui_id, tableModel);
    }
}


function getRowIdFor(filePath, selected) {
    const params = {id: 'Table__SelectedValues', columnName: 'ROWID', filePath, selectedRows: String(selected)};

    return fetchUrl(TblUtil.SEARCH_SRV_PATH, {method: 'post', params}).then( (response) => {
        return response.json().then( (selectedRowIdAry) => {
            return selectedRowIdAry;
        });
    });

}