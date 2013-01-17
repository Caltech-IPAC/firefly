package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.DefaultTableDefinition;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.FixedWidthGridBulkRenderer;
import com.google.gwt.gen2.table.client.MutableTableModel;
import com.google.gwt.gen2.table.client.PagingScrollTable;
import com.google.gwt.gen2.table.client.TableDefinition;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * An customized version of the {@link com.google.gwt.gen2.table.client.PagingScrollTable} that updated the header and footer tables to reflect the
 * currently visible rows.
 */
public class BasicPagingTable extends PagingScrollTable<TableData.Row> {

    /**
     * The previous list of visible column definitions.
     */
    private List<ColumnDefinition<TableData.Row, ?>> lastColDefs = null;
    private String name;
    private boolean showUnits;

    /**
     * Construct a new {@link BasicPagingTable}.
     *
     * @param tableModel      the underlying table model
     * @param tableDataView the column definitions
     */
    public BasicPagingTable(String name, MutableTableModel<TableData.Row> tableModel,
                   TableDataView tableDataView) {
        this(name, tableModel, new TableDef(tableDataView));
    }

    public BasicPagingTable(String name, MutableTableModel<TableData.Row> tableModel,
                   TableDefinition<TableData.Row> tableDef) {
        this(name, tableModel, new DataTable(), tableDef);
    }

    public BasicPagingTable(String name, MutableTableModel<TableData.Row> tableModel, DataTable dataTable,
                   TableDefinition<TableData.Row> tableDef) {
        super(tableModel, dataTable, new FixedWidthFlexTable(), tableDef, new Images());
        this.name = name;
        if (tableDef instanceof TableDef) {
            showUnits = showUnits || ((TableDef) tableDef).isShowUnits();
        }
        
        // Setup the bulk renderer
        FixedWidthGridBulkRenderer<TableData.Row> bulkRenderer = new FixedWidthGridBulkRenderer<TableData.Row>(
                getDataTable(), this);
        setBulkRenderer(bulkRenderer);

        Label coverUp = new Label();
        coverUp.setSize("18px", "18px");
        Element optionsEl = coverUp.getElement();
//        DOM.setStyleAttribute(optionsEl, "cursor", "pointer");
        DOM.setStyleAttribute(optionsEl, "position", "absolute");
        DOM.setStyleAttribute(optionsEl, "top", "0px");
        DOM.setStyleAttribute(optionsEl, "right", "0px");
        DOM.setStyleAttribute(optionsEl, "zIndex", "1");
        add(coverUp, getElement());

        updateHeaderTable(getTableDefinition().getVisibleColumnDefinitions());
        lastColDefs = getTableDefinition().getVisibleColumnDefinitions();


//        this.addDomHandler(KeyPressEvent.TYPE, new KeyPressHandler(){
//                    public void onKeyPress(KeyPressEvent event) {sele
//                        if(event.getNativeEvent().getKeyCode() == 38 ) {
//                            // up arrow
//                            adjustHighligtedIdx(-1);
//                        } else if (event.getNativeEvent().getKeyCode() == 40) {
//                            // down arrow
//                            adjustHighligtedIdx(1);
//                        }
//
//                    }
//                });
    }

    public void addDoubleClickListener(DoubleClickHandler dch){
        this.addDomHandler(dch, DoubleClickEvent.getType());
    }

    public void setShowUnits(boolean showUnits) {
        this.showUnits = showUnits;
        updateHeaderTable(lastColDefs, true);
    }

    public boolean isShowUnits() {
        return showUnits;
    }

    public String getName() {
        return name;
    }

    public int getRowCount() {
        return ((DataTable)getDataTable()).getDOMRowCount();
    }

    @Override
    public ColDef getColumnDefinition(int colIndex) {
        return (ColDef) super.getColumnDefinition(colIndex);
    }

    @Override
    public List<TableData.Row> getRowValues() {
        return super.getRowValues();
    }

    /**
     * Scroll the first highlighted row into the table view.
     */
    public void scrollHighlightedIntoView() {
        ((DataTable)getDataTable()).scrollSelectedIntoToView();
    }

    public void setSortIndicator(String colName, SortInfo.Direction dir) {
        Element el = findElement(getHeaderTable().getElement(), colName);
        applySortedColumnIndicator(el, dir.equals(SortInfo.Direction.ASC));
    }

    private Element findElement(Element el, String value) {
        NodeList<com.google.gwt.dom.client.Element> nl = el.getElementsByTagName("td");
        for(int i=0; i < nl.getLength(); i++) {
            if (nl.getItem(i).getInnerText().equals(value)) {
                return (Element) nl.getItem(i);
            }
        }
        return null;
    }

    public void clearSortIndicator() {
        applySortedColumnIndicator(null, true);
    }
//====================================================================
//  highlighting support
//====================================================================

    public void setHighlightRows(int... idxs) {
        if (getDataTable().getRowCount() > 0) {
            for(int i : idxs) {
                getDataTable().selectRow(i, true);
            }
        }
    }

    public Integer getFirstHighlightRowIdx() {
        Integer[] idxs = getHighlightRowIdxs();
        if (idxs != null && idxs.length > 0) {
            return idxs[0];
        }
        return -1;
    }

    public Integer[] getHighlightRowIdxs() {
        return getDataTable().getSelectedRows().toArray(
                new Integer[getDataTable().getSelectedRows().size()]);
    }

    public TableData.Row[] getHighlightRows() {
        Integer[] selrows = getHighlightRowIdxs();
        TableData.Row[] rows = new TableData.Row[selrows.length];
        int idx = 0;
        for(int i : selrows) {
            rows[idx++] = getRowValue(i);
        }
        return rows;
    }

    @Override
    protected void setData(int firstRow, Iterator<TableData.Row> rows) {
        super.setData(firstRow, rows);

        // Get the visible column definitions
        List<ColumnDefinition<TableData.Row, ?>> colDefs = getTableDefinition().getVisibleColumnDefinitions();
        updateHeaderTable(colDefs);

    }

//====================================================================
//
//====================================================================
    protected void updateHeaderTable(List<ColumnDefinition<TableData.Row, ?>> colDefs) {
        updateHeaderTable(colDefs, false);
    }

    protected void updateHeaderTable(List<ColumnDefinition<TableData.Row, ?>> colDefs, boolean force) {

        if (colDefs.equals(lastColDefs) && !force) return;    // same .. no need to update

        lastColDefs = colDefs;
        int numColumns = colDefs.size();

        // Remove everything from the header
        FixedWidthFlexTable headerTable = getHeaderTable();
        int rowCount = headerTable.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            headerTable.removeRow(0);
        }

        // Add the column and group headers
        for (int i = 0; i < numColumns; i++) {
            // Add the name
            ColDef colDef = (ColDef) colDefs.get(i);
            if (colDef.isImmutable()) continue;

            Label label = new Label(colDef.getTitle(), false);
            label.setTitle(colDef.getShortDesc());
            label.setWidth("10px");
            DOM.setStyleAttribute(label.getElement(), "display", "inline");

            headerTable.setWidget(0, i, label);
//            headerTable.setText(0, i, colDef.getName());
            setColumnWidth(i, colDef.getPreferredColumnWidth());

            if (isShowUnits() && colDef.getColumn() != null) {
                String u = colDef.getColumn().getUnits();
                final Label unit = new Label(StringUtils.isEmpty(u) ? "" : u);
                unit.setTitle("units");
                headerTable.setWidget(1, i, unit);
                headerTable.getCellFormatter().addStyleName(1, i, "unit-cell");
                unit.getElement().getParentElement().setPropertyString("type", "units");
            }

        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        switch (DOM.eventGetType(event)) {
            case Event.ONMOUSEUP:
                if (DOM.eventGetButton(event) != Event.BUTTON_LEFT) {
                    return;
                }
                    // Get the actual column index
                    Element cellElem = this.getHeaderTable().getEventTargetCell(event);
                    if (cellElem != null) {
                        // Check the colSpan
                        String t = cellElem.getPropertyString("type");
                        if (t != null && t.equals("units")) {
                            return;
                        }
                    }
                }
        super.onBrowserEvent(event);
    }


//====================================================================
//
//====================================================================

    @SuppressWarnings("deprecation")
    public static class DataTable extends FixedWidthGrid {

        public DataTable() {
            setSelectionPolicy(SelectionPolicy.ONE_ROW);
        }

        @Override
        public int getDOMRowCount() {
            return super.getDOMRowCount();
        }

        boolean scrollSelectedIntoToView() {
            Map<Integer, Element> map = getSelectedRowsMap();
            if (map.size() > 0) {
                for(int idx : map.keySet()) {
                    if (idx > -1 && idx < getRowCount()) {
                        Element el = map.get(idx);
                        if (el.getPropertyInt("clientHeight") > 0) {
                            com.google.gwt.dom.client.Element scroll = el.getParentElement().getParentElement().getParentElement();
                            GwtUtil.scrollIntoView(scroll, el, true, false);
//                            try {
//                                el.getParentElement().getParentElement().getParentElement().setScrollLeft(oldv);
//                            } catch (NullPointerException ex) {};
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

//====================================================================
//
//====================================================================
    public static class TableDef extends DefaultTableDefinition<TableData.Row> {
        boolean showUnits;

        public TableDef(TableDataView def) {
            if (def.getMeta() != null) {
                showUnits = Boolean.parseBoolean(def.getMeta().getAttribute(TableMeta.SHOW_UNITS));
            }
            for(TableDataView.Column c : def.getColumns()) {
                if (c.isVisible()) {
                    ColDef cd = new ColDef(c);
                    addColumnDefinition(cd);
                    setColumnVisible(cd, !c.isHidden());
                }
            }
        }

    public boolean isShowUnits() {
        return showUnits;
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
