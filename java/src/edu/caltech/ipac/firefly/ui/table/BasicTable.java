package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SortableGrid;
import com.google.gwt.gen2.table.client.TableModelHelper;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Date: Sep 18, 2009
 *
 * @author loi
 * @version $Id: BasicTable.java,v 1.17 2012/10/03 19:56:12 loi Exp $
 */
public class BasicTable extends ScrollTable {

    private FixedWidthGrid dataTable;
    private DataSet data;
    private List<TableData.Row> displayedData;


    public BasicTable(DataSet data) {
        super(makeDataTable(data), makeHeaderTable(data), new Images());
        dataTable = getDataTable();
        dataTable.setColumnSorter(new CustomColumnSorter(dataTable.getColumnSorter()));
        for (int i = 0; i < data.getColumns().size(); i++) {
            TableDataView.Column c = data.getColumn(i);
            if (c.isHidden() || !c.isVisible()) {
                setColumnWidth(i, 0);
                setMaximumColumnWidth(i, 0);
            } else {
                setColumnWidth(i, c.getPrefWidth());
            }
        }

        loadTable(data);
    }
    
    public List<TableData.Row> getRows() {
        return displayedData;
    }

    public void loadTable(DataSet data) {

        this.data = data;
        final TableModelHelper.ColumnSortInfo sortInfo = dataTable.getColumnSortList().getPrimaryColumnSortInfo();

        int numRows = data.getModel().getRows().size();
        boolean clearRows = dataTable.getRowCount() > numRows;

        if (clearRows) {
            if (dataTable.getRowCount() > 0) {
                do {
                    dataTable.removeRow(0);
                } while (dataTable.getRowCount() > numRows);
            }
        }

        displayedData = new ArrayList<TableData.Row>(data.getModel().getRows());
        sortData(displayedData, sortInfo);
        updateTable(displayedData);
    }

    private void updateTable(List<TableData.Row> rows) {
        if (rows != null && rows.size() > 0) {
            int i = 0;
            TableDataView.Column col;
            for (TableData.Row row : rows) {

                if (dataTable.getRowCount() <= i) {
                    getDataTable().insertRow(i);
                }
                for (int c = 0; c < row.size(); c++){
                    col = data.getColumn(c);
                    if (!data.getColumn(c).isHidden()) {
                        getDataTable().setHTML(i, c, String.valueOf(row.getValue(c)));
//                        setColumnWidth(c, col.getWidth());
                    }

                }
                i++;
            }
        }
    }

    private void sortData(final List<TableData.Row> data, final TableModelHelper.ColumnSortInfo sortInfo) {
        if (sortInfo != null) {
            final int col = sortInfo.getColumn();
            Collections.sort(data, new Comparator<TableData.Row>(){
                public int compare(TableData.Row o1, TableData.Row o2) {
                    
                    String type = BasicTable.this.data.getColumn(col).getType().toLowerCase();
                    Object v1 = o1.getValue(col);
                    Object v2 = o2.getValue(col);
                    Comparable c1, c2;

                    if (Arrays.binarySearch(new String[]{"double", "float", "d", "f"}, type) >= 0) {
                        c1 = new Double(String.valueOf(v1));
                        c2 = new Double(String.valueOf(v2));
                    } else if (Arrays.binarySearch(new String[]{"int", "long", "i", "l"}, type) >= 0) {
                        c1 = new Long(String.valueOf(v1));
                        c2 = new Long(String.valueOf(v2));
                    } else {
                        c1 = String.valueOf(v1);
                        c2 = String.valueOf(v2);
                    }

                    return (sortInfo.isAscending()? 1 : -1) * c1.compareTo(c2);
                }
            });
        }

    }

    public void clearSort() {
        applySortedColumnIndicator(null, true);
    }


    protected static FixedWidthGrid makeDataTable(DataSet data) {
        FixedWidthGrid table = new FixedWidthGrid(0, data.getColumns().size()) {
            @Override
            public ColumnSorter getColumnSorter() {
                return super.getColumnSorter(true);
            }
        };

        table.unsinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEDOWN | Event.ONCLICK);
        table.setSelectionEnabled(false);

        return table;
    }

    protected static FixedWidthFlexTable makeHeaderTable(DataSet data) {

        final FixedWidthFlexTable header = new FixedWidthFlexTable();
        int i = 0;
        for (TableDataView.Column c : data.getColumns()) {
            if (!c.isHidden()) {
                header.setHTML(0,i, c.getTitle());
            }
            i++;
        }
        return header;
    }


    private class CustomColumnSorter extends SortableGrid.ColumnSorter {
        private SortableGrid.ColumnSorter sorter;
        private TableModelHelper.ColumnSortInfo prevSortInfo;

        private CustomColumnSorter(SortableGrid.ColumnSorter sorter) {
            this.sorter = sorter;
        }

        public void onSortColumn(SortableGrid grid, TableModelHelper.ColumnSortList sortList,
                                 SortableGrid.ColumnSorterCallback callback) {

            // Get the primary column and sort order
            int col = sortList.getPrimaryColumn();
            if (prevSortInfo != null) {
                int prevCol = prevSortInfo.getColumn();
                if (col == prevCol) {
                    if (!prevSortInfo.isAscending()) {
                        sortList.clear();
                        clearSort();
                        displayedData = new ArrayList<TableData.Row>(data.getModel().getRows());
                        prevSortInfo = null;
                        updateTable(displayedData);
                        callback.onSortingComplete();
                        return;
                    }
                }
            }

            prevSortInfo = sortList.getPrimaryColumnSortInfo();
            sortData(displayedData, prevSortInfo);
            updateTable(displayedData);
            callback.onSortingComplete();
        }
    }

    public static class Images implements ScrollTableImages {

        public AbstractImagePrototype scrollTableFillWidth() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getTransImage());
        }

        public AbstractImagePrototype scrollTableAscending() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getSortAsc());
        }

        public AbstractImagePrototype scrollTableDescending() {
            return AbstractImagePrototype.create(TableImages.Creator.getInstance().getSortDesc());
        }
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
