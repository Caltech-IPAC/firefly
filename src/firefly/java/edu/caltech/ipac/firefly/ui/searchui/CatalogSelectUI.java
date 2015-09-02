/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 2/5/14
 * Time: 1:44 PM
 */


import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.task.DataSetInfoFactory;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.catalog.Catagory;
import edu.caltech.ipac.firefly.ui.catalog.Catalog;
import edu.caltech.ipac.firefly.ui.catalog.CatalogItem;
import edu.caltech.ipac.firefly.ui.catalog.Proj;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.table.AbstractLoader;
import edu.caltech.ipac.firefly.ui.table.SingleColDefinition;
import edu.caltech.ipac.firefly.ui.table.SingleColumnTablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class CatalogSelectUI implements DataTypeSelectUI {

    private static final WebClassProperties prop = new WebClassProperties(CatalogSelectUI.class);

    private DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);
    private Catagory selectedCategory= null;
    private SingleColumnTablePanel catTable = new SingleColumnTablePanel(prop.getTitle("catalog"),
                                                                          new FilteredDatasetLoader());
    private Catalog currentCatalog= null ;
    private final DataSetInfo dsInfo;
    private final SearchMaxChange searchMaxChange;
    private FlowPanel catDDContainerRight= new FlowPanel();

    private CatddEnhancedPanel catDD;
    private String selectedColumns = "";
    private String selectedConstraints = "";
    private String selectedDDForm = "";
    private CurrCatalogListener catListener;
    private List<String> catList;
    private SimpleInputField catSelectField;

    public CatalogSelectUI(DataSetInfo dsInfo, SearchMaxChange searchMaxChange) {
        this.dsInfo= dsInfo;
        this.searchMaxChange= searchMaxChange;
    }

    public Widget makeUI() {
        mainPanel.setSize("100%", "100%");
        selectedCategory= null;

        Proj proj= dsInfo.getCatProjInfo();

        DockLayoutPanel left= new DockLayoutPanel(Style.Unit.PX);
        mainPanel.addWest(left, 450);
        mainPanel.add(catDDContainerRight);


        // create category selection
        selectedCategory= proj.get(0);
        if (proj.getCatagoryCount() > 1) {
            catList= new ArrayList<String>(proj.getCatagoryCount());
            for (Catagory category : proj) catList.add(category.getCatagoryName());
            catSelectField = GwtUtil.createListBoxField("Category", "Choose Category",
                                                                         catList,catList.get(0));
            catSelectField.getField().getFocusWidget().setWidth("300px");
            left.addNorth(catSelectField, 25);
            GwtUtil.setPadding(catSelectField, 0, 0, 0, 10);
//            catSelectField.addStyleName("left-floating");
            catSelectField.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent<String> event) {
                    setSelectedCategory(catSelectField.getValue());
                }
            });
        }
        else {
            catSelectField= null;
            catList= null;
        }

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                if (GwtUtil.isOnDisplay(mainPanel)) mainPanel.onResize();
            }
        });

        Widget catTableWrapper= GwtUtil.wrap(catTable,5,5,28,5);

        catTable.addStyleName("standard-border");
//        GwtUtil.setStyles(catTable, "position", "absolute",
//                          "left", "10px",
//                          "right", "20px",
//                          "top", "0",
//                          "bottom", "0",
//                          "width", "auto");
//        catTableWrapper.setSize("100%", "200px");
//        catTableWrapper.addStyleName("left-floating");
        GwtUtil.setStyle(catTableWrapper, "position", "relative");
        left.add(catTableWrapper);
//        GwtUtil.setMargin(catTable, 10, 0, 0, 20);


        WebEventManager wem = catTable.getEventManager();
        catListener = new CurrCatalogListener();
        wem.addListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, catListener);
        wem.addListener(TablePanel.ON_LOAD, catListener);
        wem.addListener(TablePanel.ON_INIT, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                postInit();
            }
        });

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                catTable.init();
            }
        });

        return mainPanel;
    }

    private void setSelectedCategory(String categoryStr) {
        selectedCategory= getCatalogCategory(categoryStr);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                catTable.reloadTable(0);
            }
        });
    }

    private void updateDD() {
        catDDContainerRight.clear();

        if (!StringUtils.isEmpty(currentCatalog.getDDSearchProcessor())) {
            try {
                if (currentCatalog.getDDSearchProcessor().startsWith("LSST")) {
                    selectedDDForm = "none"; // no short or long form selection
                }
                catDD = new CatddEnhancedPanel(currentCatalog.getDDSearchProcessor(),
                                               currentCatalog.getQueryCatName(), selectedColumns, "",
                                               selectedConstraints, selectedDDForm, StringUtils.isEmpty(selectedColumns));
            } catch (Exception e) {
                WebAssert.argTst(false, "not sure what to do here");
            }
            catDDContainerRight.add(catDD);
        }
        else {
            catDD= null;
        }

    }


    private void postInit() {
        catTable.showToolBar(false);
        catTable.showColumnHeader(false);
        catTable.showPagingBar(false);
        catTable.showOptionsButton(false);
//        table.getTable().setHeight("200px");

        catTable.getTable().setTableDefinition(new SingleColDefinition(
                new CatalogItem("Results", catTable.getDataset())));
        catTable.reloadTable(0);
        catTable.getTable().highlightRow(0);
    }

    private String getSelectedCatRow() {
        int idx = catTable.getTable().getHighlightedRowIdx();
        return String.valueOf(idx);
    }


    private Catagory getCatalogCategory(String desc) {
        Catagory retval= null;
        Proj proj= dsInfo.getCatProjInfo();
        for (Catagory category : proj) {
            if (category.getCatagoryName().equals(desc)) {
                retval= category;
                break;
            }
        }
        return retval;
    }


    private DataSet makeCatalogDataset() {
        DataSet dataset = null;
        DataSetInfoFactory factory= Application.getDataSetFactory();
        if (factory.isAllDataSetsRetrieved()) {
            dataset = factory.getOriginalDataSet();
            if (selectedCategory != null) {
                dataset = dataset.subset(new CollectionUtil.FilterImpl<BaseTableData.RowData>() {
                    public boolean accept(BaseTableData.RowData testRow) {
                        boolean retval = false;
                        for (Catalog cat : selectedCategory) {
                            if (testRow.equals(cat.getDataSetRow())) {
                                retval = true;
                                break;
                            }
                        }
                        return retval;
                    }
                });
            }
        }
        return dataset;
    }



    public String getCatName() {
        return currentCatalog.getQueryCatName();
    }

    public String getTitle() {
        return currentCatalog.getProjStr() + "-" + currentCatalog.getQueryCatName();
    }

//====================================================================
// from DataTypeSelectUI interface
//====================================================================


    public String getDataDesc() {
        return currentCatalog!=null ? currentCatalog.getQueryCatName() : "catalog data";
    }

    public String makeRequestID() {
        if (selectedCategory!=null) {
            return currentCatalog.getCatalogSearchProcessor();
        }
        else {
            return CatalogRequest.RequestType.GATOR_QUERY.getSearchProcessor();
        }
    }

    public List<Param> getFieldValues() {
        List<Param> list= new ArrayList<Param>(10);
        list.add(new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.CONE.getDesc()));
        list.add(new Param(CatalogRequest.CATALOG, currentCatalog.getQueryCatName()));
        list.add(new Param(ServerParams.REQUESTED_DATA_SET, currentCatalog.getQueryCatName()));
        if (catDD!=null) list.addAll(catDD.getFieldValues());
//      list.add(new Param(CatalogRequest.USE, CatalogRequest.Use.DATA_PRIMARY.toString()));
        list.add(new Param(CatalogRequest.USE, CatalogRequest.Use.CATALOG_OVERLAY.toString()));
        if (catList!=null) {
            list.add(new Param(CatalogRequest.CAT_INDEX, catList.indexOf(selectedCategory.getCatagoryName())+""));
        }
        list.add(new Param(CatalogRequest.CATALOG_PROJECT, dsInfo.getId()));
        return list;
    }

    public void setFieldValues(List<Param> list) {
        final ServerRequest r= new ServerRequest(null,list); // make a tmp request so I can use the request tools

        if (r.containsParam(CatalogRequest.CAT_INDEX)) {
            int idx= r.getIntParam(CatalogRequest.CAT_INDEX,0);
            if (catList!=null && catSelectField!=null && catList.size()>idx) {
                setSelectedCategory(catList.get(idx));
                catSelectField.setValue(catList.get(idx));
            }
        }

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                if (r.containsParam(CatalogRequest.CATALOG)) {
                    String tmpSelCol= r.getParam(CatalogRequest.SELECTED_COLUMNS);
                    String tmpCon= r.getParam(CatalogRequest.CONSTRAINTS);
                    String tmpDDForm = r.getParam(CatddEnhancedPanel.FORM_KEY);

                    if (!StringUtils.isEmpty(tmpSelCol) || !StringUtils.isEmpty(tmpCon)) {
                        selectedColumns= StringUtils.isEmpty(tmpSelCol) ? "" : tmpSelCol;
                        selectedConstraints= StringUtils.isEmpty(tmpCon) ? "" : tmpCon;
                        selectedDDForm=  StringUtils.isEmpty(tmpDDForm) ? "" : tmpDDForm;
                    }
                    selectCatalog(r.getParam(CatalogRequest.CATALOG));
                }
            }
        });
    }



    public boolean validate() {
        return true;
    }

    public Iterator<Widget> iterator() {
        return new ArrayList<Widget>().iterator();
    }

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }



    private void updateCurrentCatalog() {
        if (catTable == null || catTable.getTable() == null) return;
        int idx = catTable.getTable().getHighlightedRowIdx();
        if (idx >= 0) {
            BaseTableData.RowData row = (BaseTableData.RowData) catTable.getTable().getRowValues().get(idx);
            currentCatalog = new Catalog(row);
            if (searchMaxChange!=null) searchMaxChange.onSearchMaxChange(currentCatalog.getMaxArcSec());
            try {
                updateDD();
            } finally {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {
                        selectedColumns = "";
                        selectedConstraints = "";
                        selectedDDForm = "";
                    }
                });

            }
        }
    }

    private void selectCatalog(String catName) {
        Catalog c;
        int idx=0;
        if (catTable==null || catTable.getTable()==null) return;
        for (TableData.Row row  : catTable.getTable().getRowValues()) {
            c= new Catalog(((BaseTableData.RowData)row));
            if (c.getQueryCatName().equals(catName)) {
                catTable.highlightRow(true, idx);
                break;
            }
            idx++;
        }

    }



//====================================================================
// Inner classes
//====================================================================


    public class CurrCatalogListener implements WebEventListener {

        public void eventNotify(WebEvent ev) {
            // clear selected columns, and constraints,
            // when catalog is changed
            updateCurrentCatalog();
        }
    }

    public interface SearchMaxChange {
        void onSearchMaxChange(int maxArcSec);
    }


    private class FilteredDatasetLoader extends AbstractLoader<TableDataView> {

        @Override
        public void load(int offset, int pageSize, AsyncCallback<TableDataView> callback) {
            DataSet results = makeCatalogDataset();
            this.setCurrentData(results);
            callback.onSuccess(makeCatalogDataset());
        }

        public String getSourceUrl() {
            return null;
        }

        public TableServerRequest getRequest() {
            return null;
        }
    }




}

