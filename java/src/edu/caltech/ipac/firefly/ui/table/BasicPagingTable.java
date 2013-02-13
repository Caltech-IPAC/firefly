package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.gen2.table.client.ColumnDefinition;
import com.google.gwt.gen2.table.client.DefaultTableDefinition;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.FixedWidthGridBulkRenderer;
import com.google.gwt.gen2.table.client.PagingScrollTable;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.filter.FilterDialog;
import edu.caltech.ipac.firefly.ui.table.filter.FilterPanel;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * An customized version of the {@link com.google.gwt.gen2.table.client.PagingScrollTable} that updated the header and footer tables to reflect the
 * currently visible rows.
 */
public class BasicPagingTable extends PagingScrollTable<TableData.Row> {

    private static String ttips = "Valid values are one of (=, >, <, !=, >=, <=) followed by a value separated by a space. \n" +
            "Or 'IN', followed by a list of values separated by commas. \n" +
            "Examples:  > 12345, < a_word, IN a,b,c,d";
    private static String SHOW_FILTERS_PREF = "TableShowFilters";
    
    private static final String OP_SEP = ">=|<=|=|<|>|;|IN ";

    /**
     * The previous list of visible column definitions.
     */
    private List<ColumnDefinition<TableData.Row, ?>> lastColDefs = null;
    private String name;
    private boolean showUnits;
    private FixedWidthFlexTable headers;
    public static final int FILTER_IDX = 0;
    public static final int LABEL_IDX = 1;
    public static final int UNIT_IDX = 2;
    private ChangeHandler filterChangeHandler;
    private ArrayList<FilterBox> filters = new ArrayList<FilterBox>();



//    /**
//     * Construct a new {@link BasicPagingTable}.
//     *
//     * @param tableModel      the underlying table model
//     * @param tableDataView the column definitions
//     */
//    public BasicPagingTable(String name, MutableTableModel<TableData.Row> tableModel,
//                   TableDataView tableDataView) {
//        this(name, tableModel, new TableDef(tableDataView));
//    }
//
//    public BasicPagingTable(String name, MutableTableModel<TableData.Row> tableModel,
//                   TableDefinition<TableData.Row> tableDef) {
//        this(name, tableModel, new DataTable(), tableDef);
//    }
//
    public BasicPagingTable(String name, DataSetTableModel tableModel, DataTable dataTable,
                            DatasetTableDef tableDef) {
        super(tableModel, dataTable, new FixedWidthFlexTable(), tableDef, new Images());
        this.name = name;
        headers = getHeaderTable();
        showUnits = showUnits || tableDef.isShowUnits();

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

        updateHeaderTable(false);
        lastColDefs = getTableDefinition().getVisibleColumnDefinitions();
    }

    /**
     * returns a list of filters.  returns null if validation fail.
     * @return
     */
    public List<String> getFilters() {

        if (!validateFilters()) return null;

        ArrayList<String> retval = new ArrayList<String>();
        for (int i = 0; i < filters.size(); i++) {
            FilterBox fbox = filters.get(i);
            String val = fbox.getValue().trim();
            if (!StringUtils.isEmpty(val)) {
                List<String> conds = parseConditions(val);
                for (String c : conds) {
                    if (!StringUtils.isEmpty(c)) {
                        retval.add( fbox.getName() + " " + c);
                    }
                }
            }
        }
        return retval;
    }
    
    private List<String> parseConditions(String value) {
        ArrayList<String> conds = new ArrayList<String>();
        if (StringUtils.isEmpty(value)) return conds;

        String op = null, val = null;
        String[] parts = GwtUtil.split(value, OP_SEP, true);
        for(int i = 0; i < parts.length; ) {
            String s = parts[i];
            if (s == null || s.equals(";")) {
                i++; continue;
            }
            if (s.matches(OP_SEP)) {
                if (val != null) {
                    conds.add(makeCond(op, val));
                    val = null;
                }
                op = s;
            } else {
                val = s;
            }
            i++;
        }
        if (val != null) {
            conds.add(makeCond(op, val));
            val = null;
        }
        return conds;
    }
    
    private String makeCond(String op, String val) {
        op = op == null ? "=" : op;
        if (op.equalsIgnoreCase("IN ")) {
            op = "IN";
            val = val.matches("\\(.+\\)") ? val : "(" + val.trim() + ")";
        }
        return op + " " + val;
    }

    public void setFilters(List<String> userFilters) {
        
        for (FilterBox fb : filters) {
            fb.setValue("");
        }
        
        for (String s : userFilters) {
            String[] parts = s.split("\\s+", 2);
            if (parts.length > 1) {
                FilterBox fb = getFilterBox(parts[0]);
                if (fb != null) {
                    String v = StringUtils.isEmpty(fb.getValue()) ? "" : fb.getValue() + "; ";
                    fb.setValue( v + parts[1] );
                }
            }
        }
        
    }
    
    private FilterBox getFilterBox(String name) {
        for(FilterBox fb : filters) {
            if (fb.getName().equals(name)) {
                return fb;
            }
        }
        return null;
    }

    public void onShow() {
        updateHeaderTable(lastColDefs, false);
    }

    public void showFilters(boolean flg) {
        headers.getRowFormatter().setVisible(FILTER_IDX, flg);
        redraw();
        Preferences.setBooleanPreference(SHOW_FILTERS_PREF, flg);
    }

    public boolean isShowFilters() {
        return headers.getRowFormatter().isVisible(FILTER_IDX);
    }

    private boolean validateFilters() {
        boolean retval = true;
        for (int i = 0; i < filters.size(); i++) {
            String val = filters.get(i).getValue().trim();
            if (!StringUtils.isEmpty(val)) {
                List<String> conds = parseConditions(val);
                if (conds == null || conds.size() == 0) {
                    retval = false;
                    filters.get(i).markInvalid();
                }
            }
        }
        return retval;
    }
    
    public void setFilterChangeHandler(ChangeHandler filterChangeHandler) {
        this.filterChangeHandler = filterChangeHandler;
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
    protected void updateHeaderTable(boolean  force) {
        updateHeaderTable(getTableDefinition().getVisibleColumnDefinitions(), force);
    }

    protected void updateHeaderTable(List<ColumnDefinition<TableData.Row, ?>> colDefs) {
        updateHeaderTable(colDefs, false);
    }

    protected void updateHeaderTable(List<ColumnDefinition<TableData.Row, ?>> colDefs, boolean force) {
        
        if (this.getRowCount() > 0) {
            showFilters(Preferences.getBoolean(SHOW_FILTERS_PREF, false));
        } else {
            showFilters(false);
        }

        if (colDefs.equals(lastColDefs) && !force) return;    // same .. no need to update

        lastColDefs = colDefs;
        int numColumns = colDefs.size();

        // Remove everything from the header
        int rowCount = headers.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            headers.removeRow(0);
        }
        
        filters.clear();
        // Add the column and group headers
        for (int i = 0; i < numColumns; i++) {
            // Add the name
            ColDef colDef = (ColDef) colDefs.get(i);
            if (colDef.isImmutable()) continue;

            Label label = new Label(colDef.getTitle(), false);
            label.setTitle(colDef.getShortDesc());
            label.setWidth("10px");
            DOM.setStyleAttribute(label.getElement(), "display", "inline");

            headers.setWidget(LABEL_IDX, i, label);
            setColumnWidth(i, colDef.getPreferredColumnWidth());

            String[] vals = colDef.getColumn() == null ? null : colDef.getColumn().getEnums();
            FocusWidget field = null;
            if (vals != null && vals.length > 0) {
                ListBox f = new ListBox(false);
                f.addItem("");
                for(String s : vals) {
                    f.addItem(s);
                }
                GwtUtil.setStyles(f, "fontSize", "11px");

                field = f;
            } else {
                field = new TextBox();
                field.setTitle(ttips);
            }

            final FilterBox fb = new FilterBox(colDef.getName(), field);
            fb.setWidth("90%");
            headers.setWidget(FILTER_IDX, i, fb);
            fb.getElement().getParentElement().setPropertyString("type", "filter");
            filters.add(fb);

            // add event listener to the textboxes
            fb.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    onFilterChanged(event);
                }
            });

            if (isShowUnits() && colDef.getColumn() != null) {
                String u = colDef.getColumn().getUnits();
                final Label unit = new Label(StringUtils.isEmpty(u) ? "" : u);
                unit.setTitle("units");
                headers.setWidget(UNIT_IDX, i, unit);
                headers.getCellFormatter().addStyleName(UNIT_IDX, i, "unit-cell");
                unit.getElement().getParentElement().setPropertyString("type", "units");
            }
            headers.getRowFormatter().setStyleName(FILTER_IDX, "filterRow");

        }
    }

    private void onFilterChanged(ChangeEvent event) {
        if (filterChangeHandler != null) {
            filterChangeHandler.onChange(event);
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
                        if (t != null && (t.equals("units") || t.equals("filter"))) {
                            return;
                        }
                    }
                }
        super.onBrowserEvent(event);
    }

    public void clearHiddenFilters() {
        for (FilterBox fb : filters) {
            if (!StringUtils.isEmpty(fb.getValue())) {
                DefaultTableDefinition<TableData.Row> tdef =
                        (DefaultTableDefinition<TableData.Row>) getTableDefinition();
                for(int i = 0; i < tdef.getColumnDefinitionCount(); i++) {
                    ColDef cd = (ColDef) tdef.getColumnDefinition(i);
                    if (cd.getName() != null && cd.getName().equals(fb.getName())) {
                        if ( !tdef.isColumnVisible(cd)) {
                            fb.setValue("");
                            onFilterChanged(null);
                        }
                    }
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

    private static class FilterBox extends Composite {
        private String name;
        private FocusWidget box;

        private FilterBox(String colName, FocusWidget box) {
            this.name = colName;
            this.box = box;
            initWidget(box);
        }

        public String getName() {
            return name;
        }

        public void addChangeHandler(ChangeHandler changeHandler) {
            if (box instanceof HasChangeHandlers) {
                ((HasChangeHandlers)box).addChangeHandler(changeHandler);
            }
        }
        
        public void setValue(String v) {
            if (box instanceof TextBox) {
                ((TextBox)box).setValue(v);
            } else if (box instanceof ListBox) {
                v = v.replaceAll("IN \\(", "").replace(")", "").trim();
                ListBox lbox = (ListBox) box;
                for (int i = 0; i < lbox.getItemCount(); i++) {
                    if (lbox.getItemText(i).equals(v)) {
                        lbox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
        
        public String getValue() {
            if (box instanceof TextBox) {
                return ((TextBox)box).getValue();
            } else if (box instanceof ListBox) {
                ListBox lbox = (ListBox) box;
                String v = lbox.getValue(lbox.getSelectedIndex());
                if (StringUtils.isEmpty(v)) {
                    return "";
                } else {
                    return "IN " + v;
                }
            }
            return null;
        }

        public void markInvalid() {
            box.setFocus(true);
            box.addStyleName("invalid");
            final Ref<HandlerRegistration> kpreg = new Ref<HandlerRegistration>();
            kpreg.setSource(box.addKeyPressHandler(new KeyPressHandler() {
                public void onKeyPress(KeyPressEvent event) {
                    box.removeStyleName("invalid");
                    kpreg.getSource().removeHandler();
                }
            }));
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
