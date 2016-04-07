/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty, cloneDeep, isEqual, omitBy, isUndefined} from 'lodash';

import * as TblCntlr from './TablesCntlr.js';
import * as TblUiCntlr from './TablesUiCntlr.js';
import {flux} from '../Firefly.js';
import * as TblUtil from './TableUtil.js';
import {SelectInfo} from './SelectInfo.js';

export class TableStore {

    /**
     * 
     * @param props props from Table
     * @param changeListener function called when data changed
     */
    constructor(props, changeListener) {
        this.init(props);
        this.changeListener = changeListener;
    }

    init(props) {
        const defs =  {
            showOptions: false,
            showUnits: false,
            showFilters: false,
            textView: false
        };
        const columns = ensureColumns(props);
        this.cState = Object.assign({}, defs, props, {columns})
    }

    updateState(state) {
        const {cState, changeListener} = this;
        const hasChanged = ! Object.keys(state).reduce( (rval, key) => {
            return rval && state[key] === cState[key];
        }  , true);
        if (hasChanged) {
            this.cState = Object.assign({}, cState, state);
            this.cState.columns = ensureColumns(this.cState);
            changeListener && changeListener(this.cState);
        }
    }

    onUnmount() {}

    onSort(sortInfoString) {
        var {request} = this.cState.tableModel;
        request = Object.assign({}, request, {sortInfo: sortInfoString});
        this.handleAction(TblCntlr.TABLE_FETCH, {request});
    }

    onFilter(filterIntoString) {
        var {request} = this.cState.tableModel;
        request = Object.assign({}, request, {filters: filterIntoString});
        this.handleAction(TblCntlr.TABLE_FETCH, {request});
    }

    onPageSizeChange(nPageSize) {
        nPageSize = Number.parseInt(nPageSize);
        const {tbl_id, pageSize, highlightedRow} = TblUtil.gatherTableState(this.cState.tableModel);
        if (Number.isInteger(nPageSize) && nPageSize !== pageSize) {
            const request = {pageSize: nPageSize};
            this.handleAction(TblCntlr.TABLE_HIGHLIGHT, {tbl_id, highlightedRow, request});
        }
    }

    gotoPage(number='1') {
        const {tbl_id, currentPage, totalPages, pageSize} = TblUtil.gatherTableState(this.cState.tableModel);
        number = Number.parseInt(number);
        if (Number.isInteger(number) && number !== currentPage && number > 0 && number <= totalPages) {
            const highlightedRow = (number-1) * pageSize;
            this.handleAction(TblCntlr.TABLE_HIGHLIGHT, {tbl_id, highlightedRow});
        }
    }

    onRowHighlight(rowIdx) {
        const {tbl_id, hlRowIdx, startIdx} = TblUtil.gatherTableState(this.cState.tableModel);
        if (rowIdx !== hlRowIdx) {
            const highlightedRow = startIdx+rowIdx;
            this.handleAction(TblCntlr.TABLE_HIGHLIGHT, {tbl_id, highlightedRow});
        }
    }

    onSelectAll(checked) {
        const {tbl_id, startIdx} = TblUtil.gatherTableState(this.cState.tableModel);
        const selectInfoCls = SelectInfo.newInstance(this.cState.tableModel.selectInfo, startIdx);
        selectInfoCls.setSelectAll(checked);
        const selectInfo = selectInfoCls.data;
        this.handleAction(TblCntlr.TABLE_SELECT, {tbl_id, selectInfo});
    }

    onRowSelect(checked, rowIndex) {
        const {tbl_id, startIdx} = TblUtil.gatherTableState(this.cState.tableModel);
        const selectInfoCls = SelectInfo.newInstance(this.cState.tableModel.selectInfo, startIdx);
        selectInfoCls.setRowSelect(rowIndex, checked);
        const selectInfo = selectInfoCls.data;
        this.handleAction(TblCntlr.TABLE_SELECT, {tbl_id, selectInfo});
    }

    onOptionUpdate({pageSize, columns, showUnits, showFilters, colSortDir}) {
        if (pageSize) {
            this.onPageSizeChange(pageSize);
        }
        const changes = omitBy({columns, showUnits, showFilters, colSortDir}, isUndefined);
        if ( !isEmpty(changes) ) {
            changes.tbl_ui_id = this.cState.tbl_ui_id;
            this.handleAction(TblUiCntlr.TBL_UI_STATE_UPDATE, changes);
        }
    }

    toggleTextView() {
        const changes = {tbl_ui_id: this.cState.tbl_ui_id, textView: ! this.cState.textView};
        this.handleAction(TblUiCntlr.TBL_UI_STATE_UPDATE, changes);
    }

    toggleOptions() {
        const changes = {tbl_ui_id: this.cState.tbl_ui_id, showOptions: !this.cState.showOptions};
        this.handleAction(TblUiCntlr.TBL_UI_STATE_UPDATE, changes);
    }

    handleAction(type, payload) {
        var {tableModel, origTableModel} = this.cState;
        if (! origTableModel && tableModel) {
            // for localStore
            // need to save orig tableModel when user wants to reset.
            this.cState.origTableModel = cloneDeep(tableModel);
            origTableModel = this.cState.origTableModel;
        }
        switch (type) {
            case (TblCntlr.TABLE_FETCH)  :
                if (get(payload, ['request', 'sortInfo']) !== get(tableModel, ['request', 'sortInfo'])) {
                    tableModel = TblUtil.sortTable(origTableModel, payload.request.sortInfo);
                }
                this.updateState({tableModel});
                break;

            default:
                tableModel = Object.assign({}, this.cState.tableModel, payload);
                this.updateState({tableModel});
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
    constructor(props, changeListener) {
        const tableModel = TblUtil.findTblById(props.tbl_id);
        var tpState = TblUtil.findTablePanelStateById(props.tbl_ui_id);
        super(Object.assign({}, props, tpState, {tableModel}), changeListener);
        this.removeListener= flux.addListener(() => this.updateFromFlux());
    }

    onUnmount() {
        this.removeListener && this.removeListener();
    }

    updateFromFlux() {
        var tableModel = TblUtil.findTblById(this.cState.tbl_id);
        var tpState = TblUtil.findTablePanelStateById(this.cState.tbl_ui_id);
        this.updateState({tableModel,...tpState});
    }

    handleAction(type, payload) {
        switch (type) {

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


export const ensureColumns = ({tableModel, columns}) => {
    if (isEmpty(columns)) {
        return cloneDeep(get(tableModel, 'tableData.columns', []));
    } else {
        return columns;
    }
};

