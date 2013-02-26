package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.resbundle.images.TableImages;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.ui.gwtclone.GwtPopupPanelFirefly;
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

    private static String ttips = "Valid values are one of (=, >, <, !, >=, <=, LIKE) followed by a value separated by a space. \n" +
            "Or 'IN', followed by a list of values separated by commas. \n" +
            "Examples:  > 12345, ! 3000, IN a,b,c,d";
    private static String SHOW_FILTERS_PREF = "TableShowFilters";
    
    private static final String OP_SEP = ">=|<=|=|!|<|>|;|IN |LIKE ";

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
    private FilterDialog popoutFilters;
    private DatasetTableDef tableDef;
    



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
        this.tableDef = tableDef;
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
        showFilters(false);
    }

    public void togglePopoutFilters(Widget alignTo, PopupPane.Align dir) {
        if (popoutFilters == null) {
            final FilterPanel fp = new FilterPanel(tableDef.getTableDataView().getColumns());
            Widget parent = alignTo == null ? this : alignTo;
            popoutFilters = new FilterDialog(parent, fp);
            popoutFilters.setApplyListener(new GeneralCommand("Apply") {
                        @Override
                        protected void doExecute() {
                            setFilters(fp.getFilters());
                            onFilterChanged();
                        }
                    });
            
        }
        if (popoutFilters.isVisible()) {
            popoutFilters.setVisible(false);
        } else {
            popoutFilters.getFilterPanel().setFilters(getFilters());
            popoutFilters.show(0, dir);
        }
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
        String[] parts = GwtUtil.split(value, OP_SEP, true, true);
        for(int i = 0; i < parts.length; ) {
            String s = parts[i];
            if (s == null || s.equals(";")) {
                i++; continue;
            }
//            s = s.trim();
            if (GwtUtil.matchesIgCase(s, OP_SEP) ) {
                if (val != null) {
                    conds.add(makeCond(op, val));
                    val = null;
                }
                op = s.trim().toUpperCase();
            } else {
                val = s.trim();
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
        if (StringUtils.isEmpty(op)) {
            if (val.indexOf(",") > 0) {
                op = "IN";
            } else {
                op = "LIKE";
            }
        }
        if (op.equalsIgnoreCase("IN")) {
            op = "IN";
            val = val.matches("\\(.+\\)") ? val : "(" + val.trim() + ")";
        }
        return op + " " + val;
    }

    public void setFilters(List<String> userFilters) {
        
        for (FilterBox fb : filters) {
            fb.setValue("");
        }
        
        if (userFilters == null) return;

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
        showFilters(flg, false);
    }

    private void showFilters(boolean flg, boolean softly) {
        headers.getRowFormatter().setVisible(FILTER_IDX, flg);
        redraw();
        if(!softly) {
            Preferences.set(SHOW_FILTERS_PREF, Boolean.toString(flg), true);
        }
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
        
        if (colDefs.equals(lastColDefs) && !force) {
            ensureFilterShow();
            return;    // same .. no need to update
        }

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
            Widget field = null;
            if (vals != null && vals.length > 0) {
                field = new EnumList(vals);
            } else {
                field = new TextBox();
                field.setTitle(ttips);
                field.setWidth("100%");
            }

            final FilterBox fb = new FilterBox(colDef.getName(), field);
            headers.setWidget(FILTER_IDX, i, fb);
            fb.getElement().getParentElement().setPropertyString("type", "filter");
            filters.add(fb);

            // add event listener to the textboxes
            fb.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    onFilterChanged();
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

            ensureFilterShow();
        }
    }
    private  void ensureFilterShow() {
        if (this.getRowCount() > 0) {
            showFilters(Preferences.getBoolean(SHOW_FILTERS_PREF, false));
        } else {
            showFilters(false, true);
        }
    }

    private void onFilterChanged() {
        if (filterChangeHandler != null) {
            filterChangeHandler.onChange(null);
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
                            onFilterChanged();
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
        private Widget box;

        private FilterBox(String colName, Widget box) {
            this.name = colName;
            this.box = box;
            SimplePanel w = new SimplePanel(box);
            GwtUtil.setStyle(w, "marginRight", "10px");
            initWidget(w);
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
            } else if (box instanceof EnumList) {
                ((EnumList)box).setValue(v);
            }
        }
        
        public String getValue() {
            if (box instanceof TextBox) {
                return ((TextBox)box).getValue();
            } else if (box instanceof EnumList) {
                EnumList lbox = (EnumList) box;
                return lbox.getValue();
            }
            return null;
        }

        public void markInvalid() {
            if (box instanceof TextBox) {
                ((TextBox)box).setFocus(true);
                box.addStyleName("invalid");
                final Ref<HandlerRegistration> kpreg = new Ref<HandlerRegistration>();
                kpreg.setSource(((TextBox)box).addKeyPressHandler(new KeyPressHandler() {
                    public void onKeyPress(KeyPressEvent event) {
                        box.removeStyleName("invalid");
                        kpreg.getSource().removeHandler();
                    }
                }));
            }
        }
    }

    public static class EnumList extends Composite implements HasChangeHandlers {
        private Label text = new Label("");
        private Image picker = new Image(TableImages.Creator.getInstance().getEnumList());
        private ListBox box;
        private PopupPane popup;
        private ChangeHandler chandler;
        private CheckBox allowMultiSelect;
        private List<Integer> selIdxs = new ArrayList<Integer>();

        public EnumList(String... enums) {

            allowMultiSelect = new CheckBox(" Select multiple");
            allowMultiSelect.setValue(false);

            box = new ListBox(true);
            for(String s : enums    ) {
                box.addItem(s);
            }

            Widget hide = GwtUtil.makeLinkButton("Apply", "Close this selection box and then apply the changes", new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    popup.hide();
                }
            });

            Widget clear = GwtUtil.makeLinkButton("Clear", "Remove all filter(s) from this field", new ClickHandler() {
                public void onClick(ClickEvent ev) {
                    for(int i = 0; i < box.getItemCount(); i++) {
                        box.setItemSelected(i, false);
                    }
                    popup.hide();
                }
            });

            box.setVisibleItemCount(box.getItemCount());
            FlowPanel fp = new FlowPanel();
            fp.add(picker);
            fp.add(text);
            GwtUtil.setStyle(picker, "marginRight", "3px");
            fp.getElement().getStyle().setFloat(Style.Float.NONE);
            picker.getElement().getStyle().setFloat(Style.Float.LEFT);
            picker.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (popup.isPopupShowing()) {
                        popup.hide();
                    } else {
                        popup.alignTo(picker, PopupPane.Align.BOTTOM_LEFT);
                        String s = text.getText().replaceFirst("IN |= ", "").replaceAll("\\(|\\)", "");
                        List<String> vals = Arrays.asList(s.split(","));
                        for (int i = 0; i < box.getItemCount(); i++) {
                            box.setItemSelected(i, vals.contains(box.getValue(i).trim()));
                        }
                        popup.show();
                    }
                }
            });

            SimplePanel bwrapper = new SimplePanel(box);
            bwrapper.setStyleName("multiselect-box");

            VerticalPanel content = new VerticalPanel();
            content.setStyleName("filterRow");
            content.add(allowMultiSelect);
            content.add(GwtUtil.getFiller(1, 3));
            content.add(bwrapper);
            GwtUtil.setStyle(content, "padding", "20px 5px 5px 5px");

            SimplePanel doHide = new SimplePanel(hide);
            doHide.getElement().getStyle().setPosition(Style.Position.ABSOLUTE);
            doHide.getElement().getStyle().setRight(12, Style.Unit.PX);
            doHide.getElement().getStyle().setTop(5, Style.Unit.PX);
            content.add(doHide);

            SimplePanel doClear = new SimplePanel(clear);
            doClear.getElement().getStyle().setPosition(Style.Position.ABSOLUTE);
            doClear.getElement().getStyle().setLeft(7, Style.Unit.PX);
            doClear.getElement().getStyle().setTop(5, Style.Unit.PX);
            content.add(doClear);

            popup = new PopupPane("", content, PopupType.STANDARD, false, false, true, PopupPane.HeaderType.NONE){};
            popup.setAnimationEnabled(true);

            initWidget(fp);

            box.addChangeHandler(new ChangeHandler() {
                public void onChange(ChangeEvent event) {
                    if (allowMultiSelect.getValue()) {
                        int sidx = box.getSelectedIndex();
                        if (selIdxs.contains(sidx)) {
                            selIdxs.remove(new Integer(sidx));
                        } else {
                            selIdxs.add(sidx);
                        }
                        for (int i = 0; i < box.getItemCount(); i++) {
                            box.setItemSelected(i, selIdxs.contains(i));
                        }
                    } else {
                        popup.hide();
                    }
                    selIdxs.clear();
                    for (int i = 0; i < box.getItemCount(); i++) {
                        if (box.isItemSelected(i)) {
                            selIdxs.add(i);
                        }
                    }
                }
            });


            popup.addCloseHandler(new CloseHandler<PopupPane>() {
                public void onClose(CloseEvent<PopupPane> pce) {
                    if (!popup.isPopupShowing()) {
                        applyChanges();
                    }
                }
            });



        }

        protected void applyChanges() {
            String v = "";
            for (int i = 0; i < box.getItemCount(); i++) {
                v += box.isItemSelected(i) ? "," + box.getValue(i) : "";
                
            }
            v = v.startsWith(",") ? v.substring(1) : v;
            v = v.indexOf(",") > 0 ? "IN (" + v + ")" : v;
            v = StringUtils.isEmpty(v) ? "" : v.startsWith("IN") ? v : "= " + v;
            if (!v.equalsIgnoreCase(text.getText())) {
                setValue(v);
                if (chandler != null) {
                    chandler.onChange(null);
                }
            }
        }
        
        public String getValue() {
            return text.getText().trim();
        }

        public void setValue(String v) {
            v = v == null ? "" : v;
            text.setText(v);
        }

        public HandlerRegistration addChangeHandler(ChangeHandler handler) {
            chandler = handler;
            HandlerRegistration hr = new HandlerRegistration() {
                        public void removeHandler() {
                            chandler = null;
                        }
                    };
            return hr;
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
