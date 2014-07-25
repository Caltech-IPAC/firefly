package edu.caltech.ipac.firefly.data.table;

import edu.caltech.ipac.firefly.data.HasAccessInfos;
import edu.caltech.ipac.firefly.fuse.data.config.DatasetInfoConverter;
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

    public static final String ROWID = "ROWID";
    public enum Align {LEFT, RIGHT, CENTER}

    DatasetInfoConverter getDatasetInfoProvider();
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
