/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set, isEmpty} from 'lodash';

import * as TblCntlr from './TablesCntlr.js';
import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import {Table} from './Table.js';
import {SelectInfo} from './SelectInfo.js';

export class TableStore {
    constructor(tableModel, changeListener) {
        this.tableModel = tableModel;
        this.changeListener = changeListener;
    }

    setTableModel(tableModel) {
        if (tableModel && tableModel !== this.tableModel) {
            this.tableModel = tableModel;
            this.changeListener && this.changeListener({tableModel});
        }
    }

    onUnmount() {

    }

    onPageSizeChange(nPageSize) {
        nPageSize = Number.parseInt(nPageSize);
        const {tbl_id, pageSize, highlightedRow} = TblUtil.gatherTableState(this.tableModel);
        if (Number.isInteger(nPageSize) && nPageSize !== pageSize) {
            const request = {pageSize: nPageSize};
            this.handleAction(TblCntlr.TBL_HIGHLIGHT_ROW, {tbl_id, highlightedRow, request});
        }
    }

    gotoPage(number=1) {
        const {tbl_id, currentPage, totalPages, pageSize} = TblUtil.gatherTableState(this.tableModel);
        number = Number.parseInt(number);
        if (Number.isInteger(number) && number !== currentPage && number > 0 && number <= totalPages) {
            const highlightedRow = (number-1) * pageSize;
            this.handleAction(TblCntlr.TBL_HIGHLIGHT_ROW, {tbl_id, highlightedRow});
        }
    }

    onRowHighlight(rowIdx) {
        const {tbl_id, hlRowIdx, startIdx} = TblUtil.gatherTableState(this.tableModel);
        if (rowIdx !== hlRowIdx) {
            const highlightedRow = startIdx+rowIdx;
            this.handleAction(TblCntlr.TBL_HIGHLIGHT_ROW, {tbl_id, highlightedRow});
        }
    }

    onSelectAll(checked) {
        const {tbl_id, startIdx} = TblUtil.gatherTableState(this.tableModel);
        const selectInfo = SelectInfo.newInstance(this.tableModel.selectionInfo, startIdx);
        selectInfo.setSelectAll(checked);
        const selectionInfo = selectInfo.data;
        this.handleAction(TblCntlr.TBL_SELECT_ROW, {tbl_id, selectionInfo});
    }

    onRowSelect(checked, rowIndex) {
        const {tbl_id, startIdx} = TblUtil.gatherTableState(this.tableModel);
        const selectInfo = SelectInfo.newInstance(this.tableModel.selectionInfo, startIdx);
        selectInfo.setRowSelect(rowIndex, checked);
        const selectionInfo = selectInfo.data;
        this.handleAction(TblCntlr.TBL_SELECT_ROW, {tbl_id, selectionInfo});
    }

    handleAction(type, payload) {
        var tableModel = Object.assign({}, this.tableModel, payload);
        this.setTableModel(tableModel);
    }

    /**
     * @returns {TableStore}
     */
    static newInstance(tableModel, changeListener) {
        return new TableStore(tableModel, changeListener);
    }
}

export class RemoteTableStore extends TableStore {
    constructor(tbl_id, changeListener) {
        super(TblUtil.findById(tbl_id),changeListener);
        this.removeListener= flux.addListener(() => this.updateFromFlux());
        this.tbl_id = tbl_id;
    }

    onUnmount() {
        this.removeListener && this.removeListener();
    }

    updateFromFlux() {
        var tableModel = TblUtil.findById(this.tbl_id);
        if ( tableModel !== this.tableModel) {
            this.setTableModel(tableModel);
        }
    }

    handleAction(type, payload) {
        switch (type) {
            case (TblCntlr.TBL_SELECT_ROW)  :
            case (TblCntlr.TBL_HIGHLIGHT_ROW)  :
                flux.process({type, payload});
                break;

            default:
        }
    }

    /**
     * @returns {TableStore}
     */
    static newInstance(tbl_id, changeListener) {
        return new RemoteTableStore(tbl_id, changeListener);
    }
}