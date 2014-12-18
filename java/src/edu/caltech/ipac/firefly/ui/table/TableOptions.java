package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.gen2.table.client.DefaultTableDefinition;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.override.client.FlexTable;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: May 21, 2009
 *
 * @author loi
 * @version $Id: TableOptions.java,v 1.18 2012/06/16 00:21:53 loi Exp $
 */
public class TableOptions extends Composite {

    private static final String VISI_COL_PREF = "-VisibleCols";
    private TablePanel table;
    private PopupPane main;
    private Map<ColDef, CheckBox> checkBoxes = new HashMap<ColDef, CheckBox>();
    private Widget popupContent;
    private String defVisibleCols;
    private CheckBox selectAllCheckBox = new CheckBox();
    private SimpleInputField pageSize;
//    private FilterPanel filterPanel;


    public TableOptions(final TablePanel table) {
        this.table = table;

        defVisibleCols = getVisibleColStr(table.getTable());
        applyPrefVisibleColumns();
        main = new PopupPane("Table Options");
        popupContent = makeContent();
        initWidget(popupContent);
        addStyleName("filterRow");
    }

    public static void applyPrefVisibleColumns(TablePanel table) {

        String visibleCols = Preferences.get(table.getName() + VISI_COL_PREF);
        if (!StringUtils.isEmpty(visibleCols)) {
            List<String> vcols = Arrays.asList(visibleCols.split(";"));
            DefaultTableDefinition<TableData.Row> tdef = (DefaultTableDefinition<TableData.Row>) table.getTable().getTableDefinition();
            for (int i = 0; i < tdef.getColumnDefinitionCount(); i++) {
                ColDef col = (ColDef) tdef.getColumnDefinition(i);
                if (col.getColumn() != null) {
                    boolean isVisible = vcols.contains(col.getName());
                    col.getColumn().setVisible(isVisible);
                    tdef.setColumnVisible(col, isVisible);
                }
            }
        }
    }

    public static void setPrefVisibleColumns(String tableName, String[] vcols) {
        String visibleCols = "";
        for (String vcol : vcols) {
            visibleCols += (visibleCols.length() == 0 ? "" : ";") + vcol;
        }
        if (!StringUtils.isEmpty(visibleCols)) {
            Preferences.set(tableName + VISI_COL_PREF, visibleCols);
        }
    }

    public static String [] getPrefVisibleColumns(String tableName) {
        String visibleCols = Preferences.get(tableName + VISI_COL_PREF);
        if (!StringUtils.isEmpty(visibleCols)) {
            return visibleCols.split(";");
        } else {
            return new String[]{};
        }
    }


    private void applyPrefVisibleColumns() {
        applyPrefVisibleColumns(table);
    }

    private String getVisibleColStr(BasicPagingTable table) {
        String visibleCols = "";
        DefaultTableDefinition<TableData.Row> tdef = (DefaultTableDefinition<TableData.Row>) table.getTableDefinition();
        for (int i = 0; i < tdef.getColumnDefinitionCount(); i++) {
            ColDef col = (ColDef) tdef.getColumnDefinition(i);
            if (col.getColumn() != null) {
                if (col.getColumn().isVisible()) {
                    visibleCols += (visibleCols.length() == 0 ? "" : ";") + col.getColumn().getName();
                }
            }
        }
        return visibleCols;
    }

    private void applyChanges() {
        ensureSelectAllCB();
        DefaultTableDefinition<TableData.Row> tdef =
                (DefaultTableDefinition<TableData.Row>) table.getTable().getTableDefinition();
        boolean reloadNeeded = false;

        for (ColDef col : checkBoxes.keySet()) {
            CheckBox cb = checkBoxes.get(col);
            if (tdef.isColumnVisible(col) != cb.getValue()) {
                col.getColumn().setVisible(cb.getValue());
                tdef.setColumnVisible(col, cb.getValue());
                reloadNeeded = true;
            }
        }

        if (reloadNeeded) {
            String vcols = getVisibleColStr(table.getTable());
            if (vcols.equals(defVisibleCols)) {
                Preferences.set(table.getName() + VISI_COL_PREF, null);
            } else {
                Preferences.set(table.getName() + VISI_COL_PREF, vcols);
            }
//            table.getTable().clearHiddenFilters();
            table.redrawTable();
        }
    }

    private Widget makeContent() {
        final ScrollTable colsTable = makeColsTable(table.getTable());
        colsTable.setSize("100%", "100%");

        Widget hide = GwtUtil.makeLinkButton("Hide", "Hide table options", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                table.showOptions(false);
            }
        });

        Widget reset = GwtUtil.makeLinkButton("Reset to Defaults", "Reset to the default column selections", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (!StringUtils.isEmpty(defVisibleCols)) {
                    List<String> vcols = Arrays.asList(defVisibleCols.split(";"));
                    for (Map.Entry<ColDef, CheckBox> entry : checkBoxes.entrySet()) {
                        entry.getValue().setValue(vcols.contains(entry.getKey().getName()));
                    }
                }
                applyChanges();
            }
        });
        GwtUtil.setStyle(reset, "paddingRight", "3px");

        pageSize = makePageSizeField();
        VerticalPanel content = new VerticalPanel();
        content.setSize("100%", "100%");
        content.add(pageSize);
        content.add(GwtUtil.getFiller(1,10));
        content.add(reset);
        content.add(GwtUtil.getFiller(1, 3));
        content.add(colsTable);
        content.setCellHeight(colsTable, "100%");
        content.setCellWidth(colsTable, "100%");
        content.setCellHorizontalAlignment(reset, HorizontalPanel.ALIGN_RIGHT);
        GwtUtil.setStyle(content, "border", "2px solid lightgray");

        SimplePanel doHide = new SimplePanel(hide);
        doHide.getElement().getStyle().setPosition(Style.Position.ABSOLUTE);
        doHide.getElement().getStyle().setRight(10, Style.Unit.PX);
        doHide.getElement().getStyle().setTop(5, Style.Unit.PX);
        content.add(doHide);
        
        main.setWidget(content);
        ensureSelectAllCB();
        return content;
    }

    private SimpleInputField makePageSizeField() {
        final SimpleInputField pageSize = SimpleInputField.createByProp("TablePanel.pagesize");
        pageSize.setValue(table.getDataModel().getPageSize()+"");
        ValidationInputField tbif = (ValidationInputField) pageSize.getField();
        tbif.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange(ValueChangeEvent<String> vce) {
                int newPS = StringUtils.getInt(vce.getValue());
                if ( newPS != table.getDataModel().getPageSize()) {
                    table.getPagingBar().reloadPageSize(newPS);
                    return;
                }
            }
        });
        return pageSize;
    }


    private ScrollTable makeColsTable(final BasicPagingTable table) {

        final FixedWidthFlexTable header = new FixedWidthFlexTable();
        header.setHTML(0, 0, "Column");
        header.setWidget(0, 1, selectAllCheckBox);
        selectAllCheckBox.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent ev) {
                boolean hasSel = false;
                for(Map.Entry<ColDef, CheckBox> entry : checkBoxes.entrySet()) {
                    if (entry.getValue().getValue()) {
                        hasSel = true;
                        break;
                    }
                }

                if (selectAllCheckBox.getValue() && !hasSel) {
                    for(Map.Entry<ColDef, CheckBox> entry : checkBoxes.entrySet()) {
                        entry.getValue().setValue(true);
                    }
                } else {
                    for(Map.Entry<ColDef, CheckBox> entry : checkBoxes.entrySet()) {
                        entry.getValue().setValue(false);
                    }
                    selectAllCheckBox.setValue(false);
                }
                applyChanges();
            }
        });

//        final SortableGrid.ColumnSorter[] origSorter = new SortableGrid.ColumnSorter[1];
        @SuppressWarnings("deprecation")
        final FixedWidthGrid data = new FixedWidthGrid(0, 2);
        data.unsinkEvents(Event.ONMOUSEOVER);
        data.setSelectionEnabled(false);

        final ScrollTable view = new ScrollTable(data, header, new BasicTable.Images());
        FlexTable.FlexCellFormatter formatter = header.getFlexCellFormatter();
        formatter.setHorizontalAlignment(0, 1,
                HasHorizontalAlignment.ALIGN_CENTER);
        view.setMaximumColumnWidth(1, 35);
        view.setMinimumColumnWidth(1, 35);
        view.setColumnSortable(1, false);

        final DefaultTableDefinition<TableData.Row> tdef = (DefaultTableDefinition<TableData.Row>) table.getTableDefinition();
        int cRowIdx = 0;
        for (int i = 0; i < tdef.getColumnDefinitionCount(); i++) {
            final ColDef col = (ColDef) tdef.getColumnDefinition(i);
            if (!col.isImmutable()) {
                data.insertRow(cRowIdx);
                data.setHTML(cRowIdx, 0, col.getTitle());

                CheckBox cb = new CheckBox();
                cb.setValue(tdef.isColumnVisible(col));
                checkBoxes.put(col, cb);
                data.setWidget(cRowIdx, 1, cb);
                data.getCellFormatter().setAlignment(cRowIdx, 1, HasHorizontalAlignment.ALIGN_CENTER, HasVerticalAlignment.ALIGN_MIDDLE);
                cRowIdx++;

                cb.addClickHandler(new ClickHandler(){
                    public void onClick(ClickEvent event) {
                        applyChanges();
                    }
                });
            }
        }

        return view;
    }

    public void syncOptions() {
        // sync table's column with option's checkboxes
        DefaultTableDefinition<TableData.Row> tdef = (DefaultTableDefinition<TableData.Row>) table.getTable().getTableDefinition();
        for (ColDef col : checkBoxes.keySet()) {
            CheckBox cb = checkBoxes.get(col);
            cb.setValue(tdef.isColumnVisible(col));
        }
        pageSize.setValue(String.valueOf(table.getDataModel().getPageSize()));

    }

    private void ensureSelectAllCB() {
        boolean selAll = true;

        for(Map.Entry<ColDef, CheckBox> entry : checkBoxes.entrySet()) {
            if (!entry.getValue().getValue()) {
                selAll = false;
                break;
            }
        }
        selectAllCheckBox.setValue(selAll);
    }
}
