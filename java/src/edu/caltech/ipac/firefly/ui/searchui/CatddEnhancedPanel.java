package edu.caltech.ipac.firefly.ui.searchui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.table.*;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;


/**
 *
 */
public class CatddEnhancedPanel extends Composite implements RequiresResize, InputFieldGroup {

    public final static String SELECTED_COLS_KEY = CatalogRequest.SELECTED_COLUMNS;
    public final static String CONSTRAINTS_KEY = CatalogRequest.CONSTRAINTS;
    public final static String FORM_KEY = "Catdd.Form";


    private SelectableTableWithConstraintsPanel table;
    private ArrayList<TableDataView.Column> _columns = new ArrayList<TableDataView.Column>();
    private SimplePanel tableWrapper= new SimplePanel();
    private List<Param> reqParams;
    private ListBox lists;
    private String columns;
    private String reqColumns;
    private List<String> reqColumnsList;
    private String constraints;
    private boolean _defSelect;
    private String formToSelect = "short";


    public CatddEnhancedPanel(String catalogName,
                              final String cols,
                              final String reqCols,
                              final String cons,
                              final String ddform,
                              boolean defSelect) throws Exception {
        DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.PX);
        initWidget(mainPanel);

        FlowPanel topArea = new FlowPanel();
        mainPanel.addSouth(topArea, 30);
        GwtUtil.setPadding(topArea,5,0,0,0);
        mainPanel.add(tableWrapper);
        mainPanel.setSize("100%", "100%");
        GwtUtil.setPadding(tableWrapper,0,15,0,20);

        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_DD);
        req.setQueryCatName(catalogName);

        reqParams = req.getParams();
        columns = cols;
        reqColumns = reqCols;
        reqColumnsList = StringUtils.asList(reqCols, ",");
        constraints = cons;
        if (!StringUtils.isEmpty(ddform)) formToSelect = ddform;
        _defSelect = defSelect;
//        GwtUtil.setStyle(mainPanel, "paddingLeft", "20px");

        try {
            HorizontalPanel formType = new HorizontalPanel();
            formType.add(new HTML("<b>Please select Long or Short Form display:<b>&nbsp;&nbsp;&nbsp;"));
            formType.add(createListBox());
            formType.add(new HTML("<br><br>"));
            topArea.add(formType);
            buildPanel(true);

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                if (table!=null && GwtUtil.isOnDisplay(table)) {
                    table.onResize();
                }
            }
        });
    }


//======================================================================
//------------------ PrivateMethods -----------------------
//======================================================================

    private void buildPanel(boolean ddShort) {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_DD);
        req.setParams(reqParams);
        req.setDDShort(ddShort);

        table = loadCatalogTable(req);

        table.setSize("100%", "100%");
        table.addStyleName("left-floater");
        tableWrapper.clear();
        tableWrapper.add(table);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                table.onResize();
            }
        });
    }

    private void addListeners(final SelectableTableWithConstraintsPanel table) {
        table.getEventManager().addListener(TablePanel.ON_ROWSELECT_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (!reqColumns.isEmpty()) {
                    Object obj = ev.getSource();
                    if (obj instanceof SelectableTableWithConstraintsPanel) {
                        SelectableTableWithConstraintsPanel stp = (SelectableTableWithConstraintsPanel) obj;
                        SelectionTable st = stp.getSelectionTable();
                        int rowIdx = st.getHighlightedRowIdx();
                        SortedSet<Integer> selectedRows = st.getSelectedRows();

                        if (rowIdx >= 0) {
                            String name = (String) st.getRowValue(rowIdx).getValue("name");
                            if (reqColumnsList.contains(name) && !selectedRows.contains(rowIdx)) {
                                st.select(rowIdx);
                                PopupUtil.showInfo(name + " is a required column!");
                            }
                        }
                    }
                }
            }
        });

        table.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
            public void eventNotify(WebEvent ev) {

                int selidx = lists.getSelectedIndex();
                if (!lists.getValue(selidx).equals(formToSelect)) {
                    lists.setSelectedIndex(selidx == 0 ? 1 : 0);
                    changePanel(formToSelect);
                    return;
                }

                table.showToolBar(false);
                table.showPagingBar(false);
                table.showOptionsButton(false);
                table.getTable().showFilters(false); //true if you want to see filters

                // TODO: this does not work here
                table.getDataset().getColumn(5).setVisible(false);

                for (TableData.Row row : table.getDataset().getModel().getRows()) {
                    _columns.add(new BaseTableColumn((String) row.getValue("name")));
                }

                if (_defSelect) {
                    selectDefaultColumns();
                } else if (!StringUtils.isEmpty(columns)) {
                    selectColumns(columns);
                }

                if (!StringUtils.isEmpty(reqColumns)) {
                    selectColumns(reqColumns);
                }

                if (!StringUtils.isEmpty(constraints)) {
                    populateConstraints(constraints);
                }
                onResize();
            }
        });

    }

    public void onResize() {
        table.onResize();
    }



    private ListBox createListBox() {
        List<EnumFieldDef.Item> listItems = new ArrayList<EnumFieldDef.Item>();
        listItems.add(new EnumFieldDef.Item("short", "Short Form"));
        listItems.add(new EnumFieldDef.Item("long", "Long Form"));
        EnumFieldDef list = new EnumFieldDef("Lists");
        list.addItems(listItems);
        list.setNullAllow(false);
        list.setErrMsg("This field is required. Select one from list");
        lists = GwtUtil.createComboBox(list);
        lists.addChangeHandler((new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                formToSelect = lists.getValue(lists.getSelectedIndex());
                changePanel(formToSelect);
            }
        }));


        return lists;
    }

    private void changePanel(String list) {
        _columns.clear();
        buildPanel(list.equalsIgnoreCase("short"));
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                onResize();
            }
        });
    }

    private void selectDefaultColumns() {
        int curRow = 0;
        for (TableData.Row row : table.getDataset().getModel().getRows()) {
            if (((String) row.getValue("sel")).equalsIgnoreCase("y")) {
                table.getSelectionTable().select(curRow);
            }
            curRow++;
        }
    }

    private void selectColumns(String columns) {
        for (String value : columns.split(",")) {
            int curRow = 0;
            for (TableData.Row row : table.getDataset().getModel().getRows()) {
                if (row.getValue(0).equals(value)) {
                    table.getSelectionTable().select(curRow);
                }
                curRow++;
            }

        }
    }

    private void populateConstraints(String constraints) {
        SelectionTable selTable = table.getSelectionTable();
        if (selTable instanceof SelectionTableWithConstraints) {
            ((SelectionTableWithConstraints)selTable).setConstraints(constraints);
        }
    }


    private String getSelectedColumns() {
        String values = "";
        int currRow = 0;
        for (Integer row : table.getSelectionTable().getSelectedRows()) {
            if (currRow == 0) {
                values = table.getTable().getRowValue(row).getValue(0).toString();
            } else {
                values = values + "," + table.getTable().getRowValue(row).getValue(0);
            }
            currRow++;
        }
        return values;
    }

    private String getPopulatedConstraints() {
        SelectionTable selTable = table.getSelectionTable();
        if (selTable instanceof SelectionTableWithConstraints) {
            return ((SelectionTableWithConstraints)selTable).getConstraints();
        } else {
            return "";
        }
    }

    private SelectableTableWithConstraintsPanel loadCatalogTable(CatalogRequest req) {
        BaseTableConfig<TableServerRequest> tableConfig = new BaseTableConfig<TableServerRequest>(req,
                req.getQueryCatName(), req.getQueryCatName(), null, null, null);
        Loader<TableDataView> loader = tableConfig.getLoader();
        loader.setPageSize(300);
        final SelectableTableWithConstraintsPanel table = new SelectableTableWithConstraintsPanel(loader);
        table.setMaskDelayMillSec(1);
//        table.setSize("100%", "200px");


        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
//                table.getTable().showFilters(true);
                addListeners(table);
                table.init();
            }
        });



        return table;
    }

    /*
    *   InputFieldGroup
    */
    public List<Param> getFieldValues() {
        List<Param> params = new ArrayList<Param>(3);
        params.add(new Param(SELECTED_COLS_KEY, getSelectedColumns()));
        params.add(new Param(CONSTRAINTS_KEY, getPopulatedConstraints()));
        params.add(new Param(FORM_KEY, lists.getValue(lists.getSelectedIndex())));
        return params;
    }

    public void setFieldValues(List<Param> list) {
        String columnsToSelect = null;
        String constraintsToPopulate = null;
        String formToSelect = null;
        boolean actionRequired = false;
        for (Param p : list) {
            if (p.getName().equals(SELECTED_COLS_KEY)) {
                columnsToSelect = p.getValue();
                actionRequired = true;
            }
            if (p.getName().equals(CONSTRAINTS_KEY)) {
                constraintsToPopulate = p.getValue();
                actionRequired = true;
            }
            if (p.getName().equals(FORM_KEY)) {
                formToSelect = p.getValue();
                actionRequired = true;
            }

        }
        if (!actionRequired) return;

        boolean needsFormUpdate = !StringUtils.isEmpty(formToSelect) && !formToSelect.equals(lists.getValue(lists.getSelectedIndex()));
        if (table != null && table.getSelectionTable() != null) {
            if (needsFormUpdate) {
                if (!StringUtils.isEmpty(columnsToSelect)) columns = columnsToSelect;
                if (!StringUtils.isEmpty(constraintsToPopulate)) constraints = constraintsToPopulate;
                changePanel(formToSelect);
            } else {
                if (!StringUtils.isEmpty(columnsToSelect) && !columnsToSelect.equals(getSelectedColumns())) {
                    selectColumns(columnsToSelect);
                }
                if (!StringUtils.isEmpty(constraintsToPopulate) && !constraintsToPopulate.equals(getPopulatedConstraints())) {
                    populateConstraints(constraintsToPopulate);
                }
            }
        } else {
            if (!StringUtils.isEmpty(columnsToSelect)) columns = columnsToSelect;
            if (!StringUtils.isEmpty(constraintsToPopulate)) constraints = constraintsToPopulate;
            this.formToSelect = formToSelect;
        }
    }

    public boolean validate() {
        return true;
    }

//====================================================================
//  implementing HasWidget
//====================================================================

    public void add(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public void clear() {
        throw new UnsupportedOperationException("This operation is not allowed");
    }

    public Iterator<Widget> iterator() {
        return (new ArrayList<Widget>().iterator());
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException("This operation is not allowed");
    }
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
