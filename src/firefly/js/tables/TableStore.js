/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set, isEmpty} from 'lodash';

import * as TblCntlr from './TablesCntlr.js';
import * as TblUiCntlr from './TablesUiCntlr.js';
import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import {Table} from './Table.js';

function makeColWidth(tableModel) {
    var columns = get(tableModel, 'tableData.columns');
    return !columns ? {} : columns.reduce((widths, col, cidx) => {
        const label = col.title || col.name;
        var nchar = col.prefWidth;
        if (!nchar) {
            nchar = Math.max(label.length, get(tableModel, `tableData.data.0.${cidx}.length`) || 0);
        }
        widths[col.name] = nchar * 8.5;
        return widths;
    }, {});
}

export class TableStore {
    constructor(component) {
        this.component = component;
        this.changeListener = undefined;
        this.isRemoteStore = false;
    }

    setChangeListener(callback) {
        this.changeListener = callback;
    }

    updateFromFlux() {
        var {tbl_id, tbl_ui_id, tbl_ui_gid} = this.component.state.tableUi;
        var tableModel = TblUtil.findById(tbl_id);
        if ( tableModel.tableData ) {
            var tableUi = TblUtil.findUiById(tbl_ui_id, tbl_ui_gid);
            if (tableModel !== this.component.state.tableModel || tableUi !== this.component.state.tableUi) {
                var nState = this.prepareState(tableModel, tableUi);
                nState && this.changeListener && this.changeListener(nState);
            }
        }
    }

    removeChangeListener() {
        if (this.isRemoteStore) {
            this.removeListener && this.removeListener();
        }
    }

    receiveProps({tableModel, tbl_id, tbl_ui_gid, pageSize}) {
        var {tableUi} = this.component.state;
        tableUi.pageSize = pageSize || tableUi.pageSize;
        if (tbl_id && tbl_id !== tableUi.tbl_id) {
            this.isRemoteStore = true;
            Object.assign(tableUi, {tbl_id, tbl_ui_gid});
            if (!this.removeListener) {
                this.removeListener= flux.addListener(() => this.updateFromFlux());
            }
            tableModel = TblUtil.findById(tbl_id);
        }
        if (tableModel) {
            if (tableModel !== this.component.state.tableModel) {
                tableUi.tbl_id = tableModel.tbl_id;
                this.component.setState(this.prepareState(tableModel, tableUi));
            }
        }
    }

    onResize(size) {
        if (size) {
            var {showToolbar} = this.component.props;
            var tableUi = Object.assign({},this.component.state.tableUi);
            const pagingBarHeight = showToolbar ? 26 : 0;
            tableUi.widthPx = size.width-9;
            tableUi.heightPx = size.height-pagingBarHeight-6;
            this.handleEvent(TblUiCntlr.TBL_UI_RESIZE, {tableUi});
        }
    }
    
    onColumnResize(columnWidths) {
        var tableUi = Object.assign({},this.component.state.tableUi);
        tableUi.columnWidths = Object.assign(tableUi.columnWidths, columnWidths);
        this.handleEvent(TblUiCntlr.TBL_UI_COL_RESIZE, {tableUi});
    }

    changePageSize(pageSize) {
        var tableUi = Object.assign({},this.component.state.tableUi);
        var tableModel = Object.assign({},this.component.state.tableModel);
        if (pageSize != tableUi.pageSize) {
            tableUi.pageSize = pageSize;
            tableUi.currentPage = 1;
            tableModel.highlightedRow = 0;
            this.handleEvent(TblUiCntlr.TBL_UI_GOTO_PAGE, {tableModel, tableUi});
        }
    }

    gotoPage(number=1, hlRowIdx=0) {
        var tableUi = Object.assign({},this.component.state.tableUi);
        var tableModel = Object.assign({},this.component.state.tableModel);
        if (number != tableUi.currentPage && number > 0 && number <= this.component.state.totalPages) {
            tableUi.currentPage = number;
            tableUi.hlRowIdx = hlRowIdx;
            tableModel.highlightedRow = this.toAbsIdx(hlRowIdx, tableUi);
            this.handleEvent(TblCntlr.TBL_HIGHLIGHT_ROW, {tableModel, tableUi});
        }
    }

    onRowHighlight(rowIdx) {
        if (rowIdx !== this.component.state.tableUi.hlRowIdx) {
            var tableModel = Object.assign({},this.component.state.tableModel);
            tableModel.highlightedRow = this.toAbsIdx(rowIdx);
            this.handleEvent(TblCntlr.TBL_HIGHLIGHT_ROW, {tableModel});
        }
    }

    onSelectAll(checked, selectInfo) {
        if (checked) {
            selectInfo.selectAll();
        } else {
            selectInfo.deselectAll();
        }
        var {tbl_id} = this.component.state.tableModel;
        var changes = { tbl_id, selectionInfo:selectInfo.data };
        this.handleEvent(TblCntlr.TBL_SELECT_ROW, {tableModel:changes});
    }

    onRowSelect(checked, rowIndex, selectInfo) {
        let absIdx = this.toAbsIdx(rowIndex);
        if (checked) {
            selectInfo.select(absIdx);
        } else {
            selectInfo.deselect(absIdx);
        }
        var {tbl_id} = this.component.state.tableModel;
        var changes = { tbl_id, selectionInfo:selectInfo.data };
        this.handleEvent(TblCntlr.TBL_SELECT_ROW, {tableModel:changes});
    }

    toAbsIdx(relIdx, tableUi) {
        var {currentPage, pageSize} = tableUi || this.component.state.tableUi;
        return (currentPage-1) * pageSize + relIdx;
    }

    toRelIdx(absIdx, pageSize=this.component.state.tableUi.pageSize) {
        return absIdx % pageSize;
    }

    prepareState(tableModel, tableUi) {
        tableModel = tableModel || Object.assign({}, this.component.state.tableModel);
        tableUi = tableUi || this.component.state.tableUi;

        if (!tableModel.tableData.columns) return {tableModel, tableUi};

        var {sortInfo, selectionInfo, filterInfo, highlightedRow} = tableModel;
        var {pageSize, currentPage, widthPx, heighPx, columns} = tableUi;

        if (isEmpty(columns) || tableUi.columns !== this.component.state.tableUi.columns) {
            tableUi.columns = get(tableModel, 'tableData.columns');
            tableUi.columnWidths = makeColWidth(tableModel);
        }

        tableUi.currentPage = highlightedRow >= 0 ? Math.floor(highlightedRow / pageSize)+1 : 1;
        tableUi.hlRowIdx = highlightedRow >= 0 ? this.toRelIdx(highlightedRow) : 0;
        const startIdx = (tableUi.currentPage-1) * pageSize;
        const endIdx = Math.min(startIdx+pageSize, tableModel.totalRows) || startIdx ;
        var totalPages = Math.ceil((tableModel.totalRows || 0)/pageSize);
        var data = [];
        if ( Table.newInstance(tableModel).has(startIdx, endIdx) ) {
            data = tableModel.tableData.data.slice(startIdx, endIdx);
        } else {
            Object.assign(tableModel.request, {startIdx, pageSize});
            TblCntlr.dispatchFetchTable(tableModel.request, highlightedRow);
        }
        var tableRowCount = data.length;

        return {tableModel, tableUi, totalPages, tableRowCount, data};
    }

    handleEvent(type, payload) {
        if (this.isRemoteStore) {
            switch (type) {
                case (TblCntlr.TBL_SELECT_ROW)  :
                case (TblCntlr.TBL_HIGHLIGHT_ROW)  :
                    flux.process({type, payload: payload.tableModel});
                    break;

                default:
                    flux.process({type, payload: payload.tableUi});
            }

        } else {
            var tableModel = Object.assign({}, this.component.state.tableModel, payload.tableModel);
            var tableUi = Object.assign({}, this.component.state.tableUi, payload.tableUi);
            var nState = this.prepareState(tableModel, tableUi);
            this.changeListener && this.changeListener(nState);
        }
    }

    /**
     * @returns {TableStore}
     */
    static newInstance(component) {
        return new TableStore(component);
    }
}
