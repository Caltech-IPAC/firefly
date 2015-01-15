/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.FixedWidthGridBulkRenderer;
import com.google.gwt.gen2.table.client.PagingScrollTable;
import com.google.gwt.gen2.table.event.client.PageLoadEvent;
import com.google.gwt.gen2.table.event.client.PageLoadHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
    private FixedWidthFlexTable headers;
    public static final int LABEL_IDX = 0;
    public static final int FILTER_IDX = 1;
    private TableFilterSupport filterSupport;
    private DataSetTableModel dataModel;
    private HLTracker highlightTracker = null;


    public BasicPagingTable(String name, DataSetTableModel tableModel, DataTable dataTable,
                            DatasetTableDef tableDef) {
        super(tableModel, dataTable, new FixedWidthFlexTable(), tableDef, new Images());
        this.name = name;
        headers = getHeaderTable();
        showUnits = showUnits || tableDef.isShowUnits();
        dataModel = tableModel;
        dataModel.setTable(this);

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

        filterSupport = new TableFilterSupport(this);
        updateHeaderTable(false);
        lastColDefs = getTableDefinition().getVisibleColumnDefinitions();
        filterSupport.showFilters(false);
        GwtUtil.setStyle(this, "textAlign", "left");
    }

    public DataSetTableModel getDataModel() {
        return dataModel;
    }

    public void onShow() {
        updateHeaderTable(lastColDefs, false);
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

    public void refresh() {
        List<TableData.Row>   rows = new ArrayList<TableData.Row>( getRowValues() );
        setData( getAbsoluteFirstRowIndex(), rows.iterator() );
    }

//====================================================================
//  highlighting support
//====================================================================

    public void highlightRow(int idx) {
        highlightRow(true, idx);
    }

    void highlightRow(boolean doPageLoad, int idx) {

        final Integer cIdx = idx;
        if (getDataTable().getRowCount() > 0) {
            int rowIdx = getTableIdx(idx);
            if (rowIdx < 0 && doPageLoad) {
                if (highlightTracker == null) {
                    highlightTracker = new HLTracker();
                    this.addHandler(PageLoadEvent.TYPE, highlightTracker);
                }
                highlightTracker.setHlIdx(cIdx);

                gotoPage(idx / getPageSize(), false);
                return;
            }
            if (rowIdx >= 0) {
                getDataTable().selectRow(rowIdx, true);
            }
        }
    }

    private class HLTracker implements PageLoadHandler {
        int hlIdx;

        public void setHlIdx(int hlIdx) { this.hlIdx = hlIdx; }

        public void onPageLoad(PageLoadEvent event) {
            highlightRow(false, hlIdx);
        }
    }

    public void clearHighlighted() {
        if (getDataTable() != null) {
            getDataTable().deselectAllRows();
        }
    }

    public int getHighlightedRowIdx() {
        Set<Integer> srows = getDataTable().getSelectedRows();
        if (srows != null && srows.size() > 0) {
            return srows.iterator().next() + getAbsoluteFirstRowIndex();
        }

        return -1;
    }

    public TableData.Row getHighlightedRow() {
        int rowIdx = getHighlightedRowIdx();
        TableData.Row row = null;
        if (rowIdx >= 0) {
            int tIdx = getTableIdx(rowIdx);
            row = getRowValue(tIdx);
        }
        if (row == null && getDataModel().getTotalRows() > 0) {
            row = getDataModel().getCurrentData().getModel().getRow(0);
        }
        return row;
    }

    /**
     * return the relative index of this page given the absolute index
     * @param i the absolute index
     * @return
     */
    protected int getTableIdx(int i) {
        int rowIdx = i - getAbsoluteFirstRowIndex();
        rowIdx = rowIdx >= getRowCount() ? -1 : rowIdx;
        return rowIdx;
    }

    /**
     * return the absolute index given the relative index
     * @param i the relative index
     * @return
     */
    protected int getAbsIdx(int i) {
        int rowIdx = i + getAbsoluteFirstRowIndex();
        rowIdx = rowIdx >= getTableModel().getRowCount() ? -1 : rowIdx;
        return rowIdx;
    }


    //====================================================================
//  filters supports
//====================================================================
    public List<String> getFilters() {
        return getFilters(false);
    }

    /**
     * returns a list of filters.  returns null if validation fail.
     * @return
     */
    public List<String> getFilters(boolean includeSysFilters) {
        return filterSupport.getFilters(includeSysFilters);
    }

    public void setFilters(List<String> userFilters) {
        filterSupport.setFilters(userFilters);
    }

    public void showFilters(boolean flg) {
        filterSupport.showFilters(flg);
    }

    public boolean isShowFilters() {
        return filterSupport.isShowFilters();
    }

    public void setFilterChangeHandler(ChangeHandler filterChangeHandler) {
        filterSupport.setFilterChangeHandler(filterChangeHandler);
    }

    public void togglePopoutFilters(FilterToggle filterToggle, PopupPane.Align bottomLeft) {
        filterSupport.togglePopoutFilters(filterToggle, bottomLeft);
    }

//====================================================================
//
//====================================================================

    @Override
    protected void setData(int firstRow, Iterator<TableData.Row> rows) {
        super.setData(firstRow, rows);

        // Get the visible column definitions
        List<ColumnDefinition<TableData.Row, ?>> colDefs = getTableDefinition().getVisibleColumnDefinitions();
        updateHeaderTable(colDefs);

    }

    protected void updateHeaderTable(boolean  force) {
        updateHeaderTable(getTableDefinition().getVisibleColumnDefinitions(), force);
    }

    protected void updateHeaderTable(List<ColumnDefinition<TableData.Row, ?>> colDefs) {
        updateHeaderTable(colDefs, false);
    }

    protected void updateHeaderTable(List<ColumnDefinition<TableData.Row, ?>> colDefs, boolean force) {
        
        if (colDefs.equals(lastColDefs) && !force) {
            filterSupport.ensureFilterShow();
            return;    // same .. no need to update
        }

        lastColDefs = colDefs;
        int numColumns = colDefs.size();

        // clear the headers
        clearTable(headers, numColumns);

        // Add the column and group headers
        for (int i = 0; i < numColumns; i++) {
            // Add the name
            ColDef colDef = (ColDef) colDefs.get(i);

            if (colDef == null) continue;       // skip if colDef is null

            String title = colDef.isImmutable() ? "" : "<b>" + colDef.getTitle() + "</b>";
            HTML label = new HTML(title, false);
            label.setTitle(colDef.getShortDesc());
            label.setWidth("10px");
            DOM.setStyleAttribute(label.getElement(), "display", "inline");

            headers.setWidget(LABEL_IDX, i, label);
            setColumnWidth(i, colDef.getPreferredColumnWidth());
            headers.getFlexCellFormatter().setHorizontalAlignment(LABEL_IDX, i, HorizontalPanel.ALIGN_CENTER);

            if (isShowUnits()) {
                String u =  colDef.getColumn() == null || StringUtils.isEmpty(colDef.getColumn().getUnits()) ? "&nbsp;" : "(" + colDef.getColumn().getUnits() + ")";
                label.setHTML(label.getHTML() + "<br>" + u);
            }
            GwtUtil.setStyle(label, "display", "inline-table");
        }
        filterSupport.onUpdateHeaders(colDefs);
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
                        if (t != null && (t.equals("units") || t.equals("filter"))) {
                            return;
                        }
                    }
                }
        super.onBrowserEvent(event);
    }

    protected void clearTable(FixedWidthFlexTable table, int numCols) {
        if (numCols < table.getColumnCount()) {
            for(int c = numCols-1; c < table.getColumnCount(); c++) {
                for (int r = 0; r < table.getRowCount(); r++) {
                    table.setText(r, c, "");
                }
            }
        }
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

