/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, set, isEmpty} from 'lodash';

import * as TblCntlr from './TablesCntlr.js';
import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
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
        const {tbl_id} = this.tableModel;
        this.handleAction(TblCntlr.TABLE_REMOVE, {tbl_id});
    }

    onSort(sortInfoString) {
        const {request} = this.tableModel;
        request.sortInfo = sortInfoString;
        this.handleAction(TblCntlr.TABLE_FETCH, {request});
    }

    onFilter(filterIntoString) {
        const {request} = this.tableModel;
        request.filters = filterIntoString;
        this.handleAction(TblCntlr.TABLE_FETCH, {request});
    }

    onPageSizeChange(nPageSize) {
        nPageSize = Number.parseInt(nPageSize);
        const {tbl_id, pageSize, highlightedRow} = TblUtil.gatherTableState(this.tableModel);
        if (Number.isInteger(nPageSize) && nPageSize !== pageSize) {
            const request = {pageSize: nPageSize};
            this.handleAction(TblCntlr.TABLE_HIGHLIGHT, {tbl_id, highlightedRow, request});
        }
    }

    gotoPage(number=1) {
        const {tbl_id, currentPage, totalPages, pageSize} = TblUtil.gatherTableState(this.tableModel);
        number = Number.parseInt(number);
        if (Number.isInteger(number) && number !== currentPage && number > 0 && number <= totalPages) {
            const highlightedRow = (number-1) * pageSize;
            this.handleAction(TblCntlr.TABLE_HIGHLIGHT, {tbl_id, highlightedRow});
        }
    }

    onRowHighlight(rowIdx) {
        const {tbl_id, hlRowIdx, startIdx} = TblUtil.gatherTableState(this.tableModel);
        if (rowIdx !== hlRowIdx) {
            const highlightedRow = startIdx+rowIdx;
            this.handleAction(TblCntlr.TABLE_HIGHLIGHT, {tbl_id, highlightedRow});
        }
    }

    onSelectAll(checked) {
        const {tbl_id, startIdx} = TblUtil.gatherTableState(this.tableModel);
        const selectInfoCls = SelectInfo.newInstance(this.tableModel.selectInfo, startIdx);
        selectInfoCls.setSelectAll(checked);
        const selectInfo = selectInfoCls.data;
        this.handleAction(TblCntlr.TABLE_SELECT, {tbl_id, selectInfo});
    }

    onRowSelect(checked, rowIndex) {
        const {tbl_id, startIdx} = TblUtil.gatherTableState(this.tableModel);
        const selectInfoCls = SelectInfo.newInstance(this.tableModel.selectInfo, startIdx);
        selectInfoCls.setRowSelect(rowIndex, checked);
        const selectInfo = selectInfoCls.data;
        this.handleAction(TblCntlr.TABLE_SELECT, {tbl_id, selectInfo});
    }

    handleAction(type, payload) {
        switch (type) {
            case (TblCntlr.TABLE_FETCH_UPDATE)  :
                throw new Error('sorting and filtering is not implemented for localstore, yet.');
                break;

            default:
                var tableModel = Object.assign({}, this.tableModel, payload);
                this.setTableModel(tableModel);
        }


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
        super(TblUtil.findTblById(tbl_id),changeListener);
        this.removeListener= flux.addListener(() => this.updateFromFlux());
        this.tbl_id = tbl_id;
    }

    updateFromFlux() {
        var tableModel = TblUtil.findTblById(this.tbl_id);
        if ( tableModel !== this.tableModel) {
            this.setTableModel(tableModel);
        }
    }

    handleAction(type, payload) {
        switch (type) {
            case (TblCntlr.TABLE_REMOVE)  :
                this.removeListener && this.removeListener();
                flux.process({type, payload});
                break;

            default:
                flux.process({type, payload});
        }
    }

    /**
     * @returns {TableStore}
     */
    static newInstance(tbl_id, changeListener) {
        return new RemoteTableStore(tbl_id, changeListener);
    }
}