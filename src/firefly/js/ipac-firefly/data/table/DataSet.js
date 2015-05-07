/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

/*
 columns - array of BaseTableColumn objects
 highlightedRow - int
 model - BaseTableData
 totalRows - int
 firstRowIdx - int
 meta - TableMeta
 // private transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
 // private transient DatasetInfoConverter datasetInfoProvider;
 // private transient TreeSet<Integer> highlightedRows = new TreeSet<Integer>();
 // private transient TreeSet<Integer> selectedRows = new TreeSet<Integer>();
 // private SelectionInfo selectInfo = new SelectionInfo();
 */

import {TableMeta, HAS_ACCESS_CNAME} from "./TableMeta.js";
import {BaseTableColumn} from "./BaseTableColumn.js";

export class DataSet {

    constructor(model) {
        if (model) {
            this.model = model;
            this.setColumns(DataSet.createColumns(model));
        }
    }

    /*
     * @return {Array} of Columns
     */
    static createColumns(model) {
        var cols;
        if (model) {
            var colNames = model.getColumnNames();
            cols = new Array(colNames.length);
            colNames.forEach(function (value, i) {
                cols[i] = new BaseTableColumn(colNames[i]);
            });
        }
        return cols;
    }

    getModel() {
        return this.model;
    }

    setModel(model) {
        let old = this.model;
        this.model = model;
        this.model.setHasAccessCName(this.getMeta().getAttribute(HAS_ACCESS_CNAME));
        //TODO: pcs.firePropertyChange(MODEL_LOADED, old, model);
    }

    getTotalRows() {
        return this.totalRows;
    }

    setTotalRows(totalRows) {
        this.totalRows = totalRows;
        //TODO this.selectInfo.setRowCount(totalRows);
    }

    getStartingIdx() {
        return this.firstRowIdx;
    }

    setStartingIdx(index) {
        this.firstRowIdx = index;
    }

    /*
     * @return {TableMeta}
     */
    getMeta() {
        return this.meta;
    }

    /**
     * Sets the meta identification for this DataSet.
     * @param {TableMeta} meta
     */
    setMeta(meta) {
        this.meta = meta;
    }

    /**
     * Returns all of the columns data for this DataSet
     * @return {Array} of BaseTableColumn objects
     */
    getColumns() {
        if (this.columns) {
            return this.columns.slice(0);
        } else {
            return [];
        }
    }

    moveColumn(col, toIdx) {
        if (this.columns.remove(col)) {
            this.columns.add(toIdx, col);
        }
    }

    addColumn(col) {
        this.columns.add(col);
        this.model.addColumn((this.columns.length - 1), col.getName());
    }

    removeColumn(col) {
        this.columns.remove(col);
    }

    /**
     * Returns the columns data for the given column.
     * @param {Number} colIdx index of the column
     * @return {BaseTableColumn}
     */
    getColumn(colIdx) {
        return this.columns.get(colIdx);
    }

    /*
     * @return {BaseTableColumn}
     */
    findColumn(colName) {
        for (var i = 0; i < this.columns.length; i++) {
            if (this.columns[i].getName() === colName) {
                return this.columns[i];
            }
        }
        return null;
    }

    findColumnIdx(colName) {
        let c = this.findColumn(colName);
        if (c) {
            return this.columns.indexOf(c);
        } else {
            return -1;
        }
    }

    /**
     * Defines the columns data information for this DataSet
     * @param {Array} cols
     */
    setColumns(cols) {
        if (cols) {
            this.columns = cols.slice(0);
        }
    }

    /**
     * Get a subset of the current DataSet. Only the TableModel is cloned.  The rest are references.
     * @param {Number} fromIdx
     * @param {Number} toIdx
     * @return {DataSet}
     */
    subset(fromIdx, toIdx) {

        let newval = this.emptyCopy();
        newval.firstRowIdx = fromIdx;

        var beginIdx = fromIdx - this.firstRowIdx;
        var endIdx = Math.min(beginIdx + toIdx - fromIdx, this.getModel().size());

        if (beginIdx < endIdx) {
            let result = [];
            for (let i = beginIdx; i < endIdx; i++) {
                result.push(this.model.getRow(i));
            }
            newval.model.getRows().push(result);
            newval.totalRows = result.length;
        }
        return newval;
    }

    /*
     * @return {DataSet}
     */
    clone() {
        let newval = new DataSet();
        newval.columns = this.getColumns();
        newval.model = this.model.clone();
        newval.meta = this.meta.clone();
        return newval;
    }

    /*
     * @return {DataSet}
     */
    emptyCopy() {
        let newval = new DataSet();
        newval.columns = this.getColumns();
        newval.model = this.model.clone();
        newval.model.getRows().clear();
        newval.meta = this.meta;
        return newval;
    }

    getSize() {
        return (!this.model) ? 0 : this.model.getSize();
    }

    /*
     * @return {Boolean}
     */
    hasAccess(index) {
        return this.model && this.model.hasAccess(index);
    }


    /* -------------------------------------------------------------------
       TODO: methods below this line are to be implemented in the future

    highlight(rowIdx) {
        if (rowIdx >= 0 && rowIdx < this.getTotalRows()) {
            var oldv = this.highlightedRow;
            this.highlightedRow = rowIdx;
            this.pcs.firePropertyChange(ROW_HIGHLIGHTED, oldv, rowIdx);
        }
    }

    clearHighlighted() {
        var oldv = this.highlightedRow;
        this.highlightedRow = -1;
        this.pcs.firePropertyChange(ROW_CLEARHIGHLIGHTED, oldv, highlightedRow);
    }

    getHighlighted() {
        return this.highlightedRow;
    }

    /**
     * rowIdx is the absolute row index of the whole table.
     *
     * @param rowIdx
     *
    select() {
        for(var arg in arguments) {
            this.selectInfo.select(arg);
        }
        this.pcs.firePropertyChange(ROW_SELECTED, selectInfo, rowIdx);
    }


    setSelectionInfo(selectInfo) {
        this.selectInfo = selectInfo;
        this.pcs.firePropertyChange(ROW_SELECTED, selectInfo, selectInfo.getSelected());
    }

    getSelectionInfo() {
        return this.selectInfo;
    }

    /**
     * rowIdx is the absolute row index of the whole table.
     * @param rowIdx
     *
    isSelected(rowIdx) {
        return this.selectInfo.isSelected(rowIdx);
    }

    isSelectAll() {
        return this.selectInfo.isSelectAll();
    }

    selectAll() {
        this.selectInfo.selectAll();
        this.pcs.firePropertyChange(ROW_SELECT_ALL, null, selectInfo);
    }

    /**
     * @param ...rowIdx is the absolute row index of the whole table
     *
    deselect() {
        if (arguments.length > 0) {
            for (var arg in arguments) {
                this.selectInfo.deselect(arg);
            }
            this.pcs.firePropertyChange(ROW_DESELECTED, selectInfo, rowIdx);
        }
    }

    getSelected() {
        return this.selectInfo.getSelected().slice(0);
    }

    deselectAll() {
        this.selectInfo.deselectAll();
        this.pcs.firePropertyChange(ROW_DESELECT_ALL, null, selectInfo);
    }

    /*
     * @param {PropertyChangeListener} listener
     * @param {String} type
     *
    addPropertyChangeListener(listener, type) {
        this.pcs.addPropertyChangeListener(listener, type);
    }

    /*
     * @param {PropertyChangeListener} listener
     *
    removePropertyChangeListener(listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    /**
     * get a subset of the current DataSet. Only the TableModel is cloned.  The rest are references.
     *
     * @param filter interface to filter the correct rows for the new DataSet
     * @return the filtered DataSet
     *
    subset(filter) {
        var inRows = this.getModel().getRows();
        var outRows = [];
        filter(inRows, outRows, filter);

        let newval = emptyCopy();
        newval.firstRowIdx = 0;
        newval.totalRows = outRows.length;
        newval.model.getRows().push(outRows);
        return newval;
    }
    ---------------------------------------------------------------------------------------------*/


}
