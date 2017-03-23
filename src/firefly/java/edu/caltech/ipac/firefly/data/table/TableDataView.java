/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.firefly.data.HasAccessInfos;
import edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;

import java.io.Serializable;
import java.util.List;

/**
 * Date: May 14, 2008
 *
 * @author loi
 * @version $Id: TableDataView.java,v 1.18 2012/01/12 18:22:53 loi Exp $
 */
public interface TableDataView extends HasAccessInfos, Serializable {

    public static final String ROW_HIGHLIGHTED = "rowHighlighted";
    public static final String ROW_CLEARHIGHLIGHTED = "rowClearHighlighted";
    public static final String ROW_SELECTED = "rowSelected";
    public static final String ROW_DESELECTED = "rowDeselected";
    public static final String ROW_SELECT_ALL = "rowSelectAll";
    public static final String ROW_DESELECT_ALL = "rowDeselectAll";
    public static final String MODEL_LOADED = "modelLoaded";

    public static final String ROWID = "ROW_IDX";
    public enum Align {LEFT, RIGHT, CENTER}

    DatasetInfoConverter getDatasetInfoConverter();
    TableMeta getMeta();
    void setMeta(TableMeta meta);
    TableData<TableData.Row> getModel();
    void setModel(TableData model);
    int getStartingIdx();
    void setStartingIdx(int idx);
    int getTotalRows();
    void setTotalRows(int totalRows);
    Column getColumn(int idx);
    Column findColumn(String colName);
    int findColumnIdx(String colName);
    List<Column> getColumns();
    void moveColumn(Column col, int toIdx);
    void addColumn(Column col);
    void addColumn(int index, Column col);
    void removeColumn(Column col);
    DataSet subset(int fromIdx, int toIdx);

//--------- highlighting ---------------//
    void highlight(int rowIdx);
    void clearHighlighted();
    int getHighlighted();
//--------- selecting (checkbox) ---------------//
    void setSelectionInfo(SelectionInfo selectInfo);
    SelectionInfo getSelectionInfo();
    void select(Integer... rowIdx);
    void deselect(Integer... rowIdx);
    List<Integer> getSelected();
    boolean isSelectAll();
    boolean isSelected(int rowIdx);
    void selectAll();
    void deselectAll();


    void addPropertyChangeListener(String e, PropertyChangeListener listener);
    void addPropertyChangeListener(PropertyChangeListener listener);
    void removePropertyChangeListener(PropertyChangeListener listener);

    public interface Column extends Serializable {
        String getTitle();
        String getShortDesc();
        void setShortDesc(String desc);
        void setTitle(String title);
        String getName();
        Align getAlign();
        void setAlign(Align align);
        int getWidth();
        void setWidth(int width);
        int getPrefWidth();
        void setPrefWidth(int width);
        String getUnits();
        void setUnits(String units);
        String getType();
        void setType(String type);

        /**
         * Returns true if this column is hidden.  A hidden column is not intended for user to see.
         * It is used by the application only
         */
        boolean isHidden();
        void setHidden(boolean isHidden);
        boolean isSortable();
        void setSortable(boolean isSortable);

        /**
         * Returns true if this column is visible.  Use setVisible(flag) to show or hide this column.
         */
        boolean isVisible();
        void setVisible(boolean isVisible);
        void setEnums(String[] enumVals);
        String[] getEnums();
        void setSortByCols(String[] cols);
        String[] getSortByCols();
    }

}
