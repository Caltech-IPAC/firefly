package edu.caltech.ipac.firefly.ui.catalog;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableColumn;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.SelectableTablePanel;
import edu.caltech.ipac.firefly.ui.table.SelectionTable;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.ui.table.filter.FilterPanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.EnumFieldDef;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Aug 3, 2010.
 */
public class CatddPanel extends Composite implements RequiresResize, InputFieldGroup {

    public final static String SELECTED_COLS_KEY = "CatDDPanel.SelectedCols";
    public final static String CONSTRAINTS_KEY = "CatDDPanel.Constraints";
    public final static String FORM_KEY = "CatDDPanel.ShortForm";


    private SelectableTablePanel table;
    private ArrayList<TableDataView.Column> _columns = new ArrayList<TableDataView.Column>();
    private VerticalPanel vp = new VerticalPanel();
    private HTML html = new HTML("<br>Add Column Filters Below.<br><br>");
    private List<Param> reqParams;
    private FilterPanel panel;
    private CatColumnInfo _info;
    private ListBox lists;
    private String columns;
    private String reqColumns;
    private List<String> reqColumnsList;
    private String constraints;
    private boolean _defSelect;
    private String formToSelect = "short";
    private HorizontalPanel formType= new HorizontalPanel();


    public CatddPanel(CatColumnInfo info, List<Param> params, final String cols, final String cons, boolean defSelect) throws Exception {
        this(info, params, cols, "", cons, defSelect);
    }


    public CatddPanel(CatColumnInfo info, List<Param> params, final String cols, final String reqCols, final String cons, boolean defSelect) throws Exception {
        initWidget(vp);
        _info = info;
        reqParams = params;
        columns = cols;
        reqColumns = reqCols;
        reqColumnsList = StringUtils.asList(reqCols, ",");
        constraints = cons;
        _defSelect = defSelect;
        GwtUtil.setStyle(vp, "paddingLeft", "5px");

        try {
            formType.add(new HTML("<b>Please select Long or Short Form display:<b>&nbsp;&nbsp;&nbsp;"));
            formType.add(createListBox());
            formType.add(new HTML("<br><br>"));
            vp.add(formType);
            buildPanel(true);

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public void setColumns() {
       _info.setSelectedColumns(getSelectedColumns());
    }

    public void setConstraints() {
        _info.setSelectedConstraints(getPopulatedConstraints());
    }

//======================================================================
//------------------ PrivateMethods -----------------------
//======================================================================

    private void buildPanel(boolean ddShort) {
        CatalogRequest req = new CatalogRequest(CatalogRequest.RequestType.GATOR_DD);
        req.setParams(reqParams);
        req.setDDShort(ddShort);

        table = loadCatalogTable(req);

        addListeners();

        vp.add(table);
        vp.add(html);
    }

    private void addListeners() {
        table.getEventManager().addListener(TablePanel.ON_ROWSELECT_CHANGE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (!reqColumns.isEmpty()) {
                    Object obj = ev.getSource();
                    if (obj instanceof SelectableTablePanel) {
                        SelectableTablePanel stp = (SelectableTablePanel) obj;
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

                table.removePanels();
                // TODO: this does not work here
                table.getDataset().getColumn(5).setVisible(false);

                for (TableData.Row row : table.getDataset().getModel().getRows()) {
                    _columns.add(new BaseTableColumn((String) row.getValue("name")));
                }

                List<TableDataView.Column> list = _columns;
                panel = new FilterPanel(list, false);
                vp.add(panel);

                if (_defSelect) {
                    selectDefaultColumns();
                } else {
                    if (!StringUtils.isEmpty(columns)) {
                        selectColumns(columns);
                    } //else {
                    //selectAllColumns();
                    //}
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
        Widget parent= getParent();
        if (panel!=null && html!=null && formType!=null) {
            int fixedHeight= panel.getOffsetHeight() + html.getOffsetHeight() + formType.getOffsetHeight();
            int h= parent.getOffsetHeight() - fixedHeight;
            int w= (parent.getOffsetWidth() - 15);
            if (w > 0 && h > 0) {
                table.setSize(w+"px",h + "px");
            }
        }
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
        vp.remove(table);
        vp.remove(html);
        if (panel != null && vp.getWidgetIndex(panel) >= 0) {
            vp.remove(panel);
        }

        if (list.equalsIgnoreCase("short")) {
            buildPanel(true);
        } else {
            buildPanel(false);
        }
        DeferredCommand.addCommand(new Command() {
            public void execute() { onResize(); }
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
        List<String> filters = Arrays.asList(constraints.split(","));
        panel.setFilters(filters);

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
        String values = "";
        if (!panel.getFilters().isEmpty()) {
            int currRow = 0;
            for (String value : panel.getFilters()) {
                if (currRow == 0) {
                    values = value;
                } else {
                    values = values + "," + value;
                }
                currRow++;
            }
        }
        return values;
    }



    private SelectableTablePanel loadCatalogTable(CatalogRequest req) {
        BaseTableConfig<TableServerRequest> tableConfig = new BaseTableConfig<TableServerRequest>(req,
                req.getQueryCatName(), req.getQueryCatName(), null, null, null);
        Loader<TableDataView> loader = tableConfig.getLoader();
        loader.setPageSize(300);
        SelectableTablePanel table = new SelectableTablePanel(loader);
        table.setSize("600px", "200px");


        table.init();


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
