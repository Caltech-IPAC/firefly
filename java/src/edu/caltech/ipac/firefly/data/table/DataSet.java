package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.PropertyChangeSupport;
import edu.caltech.ipac.util.CollectionUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;


/**
 * This is a utility class use to represent a set of data, similiar to ResultSet
 * which can be pass to to a client. All reference of index starts from 0. <p>
 * <p/>
 * <b>NOTE:</b> Note that this implementation is not synchronized. If multiple
 * threads access a DataSet instance concurrently, and at least one of the
 * threads modifies the list structurally, it must be synchronized externally.
 *
 * @author loi
 * @version $Id: DataSet.java,v 1.35 2012/01/12 18:22:53 loi Exp $
 */
public class DataSet implements TableDataView, Serializable {

    private ArrayList<Column> columns;
    private transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);
//    private transient TreeSet<Integer> highlightedRows = new TreeSet<Integer>();
    private int highlightedRow;
    private BaseTableData model;
    private int totalRows;
    private int firstRowIdx;
    private TableMeta meta;
//    private transient TreeSet<Integer> selectedRows = new TreeSet<Integer>();
    private SelectionInfo selectInfo = new SelectionInfo();


    public DataSet() {
        this(new Column[0]);
    }

    public DataSet(Column[] meta) {
        model = new BaseTableData(new String[0]);
        setColumns(meta);
    }

    public DataSet(BaseTableData model) {
        this.model = model;
        setColumns(createColumns(model));
    }

    private Column[] createColumns(TableData model) {
        List<String> colNames = model.getColumnNames();
        Column[] cols = new BaseTableColumn[colNames.size()];
        for(int i = 0; i < colNames.size(); i++) {
            cols[i] = new BaseTableColumn(colNames.get(i));
        }
        return cols;
    }

    public TableData getModel() {
        return model;
    }

    public void setModel(TableData model) {
        TableData old = this.model;
        this.model = (BaseTableData)model;
        this.model.setHasAccessCName(getMeta().getAttribute(TableMeta.HAS_ACCESS_CNAME));
        pcs.firePropertyChange(MODEL_LOADED, old, model);
    }

    public int getTotalRows() {
         return totalRows;
     }

     public void setTotalRows(int totalRows) {
         this.totalRows = totalRows;
         selectInfo.setRowCount(totalRows);
     }

     public int getStartingIdx() {
         return firstRowIdx;
     }

     public void setStartingIdx(int index) {
         this.firstRowIdx = index;
     }

    public TableMeta getMeta() {
        return meta;
    }

    /**
     * Sets the meta identification for this DataSet.
     */
    public void setMeta(TableMeta meta) {
        this.meta = meta;
    }

    /**
     * Returns all of the columns data for this DataSet
     * @return
     */
    public List<Column> getColumns() {
        return Collections.unmodifiableList((List<? extends Column>) columns.clone());
    }

    public void moveColumn(Column col, int toIdx) {
        if (columns.remove(col)) {
            columns.add(toIdx, col);
        }
    }

    public void addColumn(Column col) {
        columns.add(col);
        model.addColumn((columns.size() - 1), col.getName());
    }

    public void addColumn(int index, Column col) {
        columns.add(index, col);
    }

    public void removeColumn(Column col) {
        columns.remove(col);
    }

    /**
     * Returns the columns data for the given column.
     * @param colIdx    index of the column
     * @return
     */
    public Column getColumn(int colIdx) {
        return columns.get(colIdx);
    }

    public Column findColumn(String colName) {
        for(Column c : columns) {
            if(c.getName().equals(colName)) {
                return c;
            }
        }
        return null;
    }

    public int findColumnIdx(String colName) {
        Column c = findColumn(colName);
        if (c != null) {
            return columns.indexOf(c);
        } else {
            return -1;
        }
    }

    public void highlight(int rowIdx) {

        if (rowIdx >= 0 && rowIdx < this.getTotalRows()) {
            int oldv = highlightedRow;
            highlightedRow = rowIdx;
            pcs.firePropertyChange(ROW_HIGHLIGHTED, oldv, rowIdx);
        }
    }

    public void clearHighlighted() {
        int oldv = highlightedRow;
        highlightedRow = -1;
        pcs.firePropertyChange(ROW_CLEARHIGHLIGHTED, oldv, highlightedRow);
    }

    public int getHighlighted() {
        return highlightedRow;
    }

    public void select(Integer... rowIdx) {
        for(Integer i : rowIdx) {
            selectInfo.select(i);
        }
        pcs.firePropertyChange(ROW_SELECTED, selectInfo, rowIdx);
    }

    public void setSelectionInfo(SelectionInfo selectInfo) {
        this.selectInfo = selectInfo;
        pcs.firePropertyChange(ROW_SELECTED, selectInfo, selectInfo.getSelected().toArray(new Integer[0]));
    }

    public SelectionInfo getSelectionInfo() {
        return selectInfo;
    }

    public boolean isSelected(int rowIdx) {
        return selectInfo.isSelected(rowIdx);
    }

    public boolean isSelectAll() {
        return selectInfo.isSelectAll();
    }

    public void selectAll() {
        selectInfo.selectAll();
        pcs.firePropertyChange(ROW_SELECT_ALL, null, selectInfo);
    }

    public void deselect(Integer... rowIdx) {
        if (rowIdx != null && rowIdx.length > 0) {
            for(int i : rowIdx) {
                selectInfo.deselect(i);
            }
            pcs.firePropertyChange(ROW_DESELECTED, selectInfo, rowIdx);
        }
    }

    public List<Integer> getSelected() {
        SortedSet<Integer> ss = selectInfo.getSelected();
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.addAll(ss);
        return list;
    }

    public void deselectAll() {
        selectInfo.deselectAll();
        pcs.firePropertyChange(ROW_DESELECT_ALL, null, selectInfo);
    }

    public void addPropertyChangeListener(String type, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(type, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }


    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }




    /**
     * Defines the columns data information for this DataSet
     * @param meta
     */
    public void setColumns(Column[] meta) {
        this.columns = new ArrayList<Column>(meta.length);
        this.columns.addAll(Arrays.asList(meta));
    }

    /**
     * get a subset of the current DataSet.
     * Only the TableModel is cloned.  The rest are references.
     * @param fromIdx
     * @param toIdx
     * @return
     */
    public DataSet subset(int fromIdx, int toIdx) {

        DataSet newval= emptyCopy();
        newval.firstRowIdx = fromIdx;

        int beginIdx = fromIdx-firstRowIdx;
        int endIdx = Math.min(beginIdx + toIdx-fromIdx, getModel().size());

        if (beginIdx < endIdx) {
            ArrayList<BaseTableData.RowData> result = new ArrayList<BaseTableData.RowData>();
            for(int i = beginIdx; i < endIdx; i++) {
                result.add(model.getRow(i));
            }
            newval.model.getRows().addAll(result);
            newval.totalRows = result.size();
        }
        return newval;
    }



    /**
     * get a subset of the current DataSet.
     * Only the TableModel is cloned.  The rest are references.
     * @param filter interface to filter the correct rows for the new DataSet
     * @return the filtered DataSet
     */
    public DataSet subset(CollectionUtil.Filter<BaseTableData.RowData> filter) {
        return subset(filter, 40);
    }

    /**
     * get a subset of the current DataSet.
     * Only the TableModel is cloned.  The rest are references.
     * @param filter interface to filter the correct rows for the new DataSet
     * @param sizeGuess a approximation of the row count of the output dataset will be
     * @return the filtered DataSet
     */
    public DataSet subset(CollectionUtil.Filter<BaseTableData.RowData> filter, int sizeGuess) {
        List<BaseTableData.RowData> inRows= this.getModel().getRows();
        ArrayList<BaseTableData.RowData> outRows= new ArrayList<BaseTableData.RowData>(sizeGuess);
        CollectionUtil.filter(inRows, outRows, filter);

        DataSet newval= emptyCopy();
        newval.firstRowIdx = 0;
        newval.totalRows = outRows.size();
        newval.model.getRows().addAll(outRows);
        return newval;
    }


    private DataSet emptyCopy() {
        DataSet newval= new DataSet();
        newval.columns = columns;
        newval.model = model.clone();
        newval.model.getRows().clear();
        newval.meta = meta;
        return newval;
    }

    public int getSize() {
        return model == null ? 0 : model.getSize();
    }

    public boolean hasAccess(int index) {
        return model != null && model.hasAccess(index);
    }
}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/