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
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.task.IrsaAllDataSetsTask;
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

    CatddEnhancedPanel catDD;
    private String selectedColumns = "";
    private String selectedConstraints = "";
    CurrCatalogListener catListener;
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
            catSelectField = GwtUtil.createListBoxField("Catagory", "Choose Catatory",
                                                                         catList,catList.get(0));
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
        catTable.reloadTable(0);
    }

    private void updateDD(final boolean selectDefaultCols) {
        catDDContainerRight.clear();

        try {
            catDD = new CatddEnhancedPanel(currentCatalog.getQueryCatName(), selectedColumns, "", selectedConstraints, selectDefaultCols);
        } catch (Exception e) {
            WebAssert.argTst(false, "not sure what to do here");
        }

        catDDContainerRight.add(catDD);
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
        if (IrsaAllDataSetsTask.isIrsaAllDataSetsRetrieved()) {
            dataset = IrsaAllDataSetsTask.getOriginalDataSet();
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

    public String makeRequestID() { return CatalogRequest.RequestType.GATOR_QUERY.getSearchProcessor(); }

    public List<Param> getFieldValues() {
        List<Param> list= new ArrayList<Param>(10);
        list.add(new Param(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.CONE.getDesc()));
        list.add(new Param(CatalogRequest.CATALOG, currentCatalog.getQueryCatName()));
        list.addAll(catDD.getFieldValues());
//      list.add(new Param(CatalogRequest.USE, CatalogRequest.Use.DATA_PRIMARY.toString()));
        list.add(new Param(CatalogRequest.USE, CatalogRequest.Use.CATALOG_OVERLAY.toString()));
        if (catList!=null) {
            list.add(new Param(CatalogRequest.CAT_INDEX, catList.indexOf(selectedCategory.getCatagoryName())+""));
        }
        list.add(new Param(CatalogRequest.CATALOG_PROJECT, dsInfo.getId()));
        return list;
    }

    public void setFieldValues(List<Param> list) {
        ServerRequest r= new ServerRequest(null,list); // make a tmp request so I can use the request tools

        if (r.containsParam(CatalogRequest.CAT_INDEX)) {
            int idx= r.getIntParam(CatalogRequest.CAT_INDEX,0);
            if (catList!=null && catSelectField!=null && catList.size()>idx) {
                setSelectedCategory(catList.get(idx));
                catSelectField.setValue(catList.get(idx));
            }
        }

        if (r.containsParam(CatalogRequest.CATALOG)) {
            String tmpSelCol= r.getParam(CatalogRequest.SELECTED_COLUMNS);
            String tmpCon= r.getParam(CatalogRequest.CONSTRAINTS);

            if (!StringUtils.isEmpty(tmpSelCol) || !StringUtils.isEmpty(tmpCon)) {
                selectedColumns= StringUtils.isEmpty(tmpSelCol) ? "" : tmpSelCol;
                selectedConstraints= StringUtils.isEmpty(tmpCon) ? "" : tmpCon;
            }
        }

        if (r.containsParam(CatalogRequest.CATALOG)) {
            selectCatalog(r.getParam(CatalogRequest.CATALOG));
        }

        /*
        String tmpSelCol= r.getParam(CatalogRequest.SELECTED_COLUMNS);
        String tmpCon= r.getParam(CatalogRequest.CONSTRAINTS);

        if (!StringUtils.isEmpty(tmpSelCol) || !StringUtils.isEmpty(tmpCon)) {
            selectedColumns= StringUtils.isEmpty(tmpSelCol) ? "" : tmpSelCol;
            selectedConstraints= StringUtils.isEmpty(tmpCon) ? "" : tmpCon;
            updateDD(false);
        }
        */
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
                updateDD(catListener.useDefaultCols);
            } finally {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {
                        catListener.useDefaultCols=true;
                        selectedColumns = "";
                        selectedConstraints = "";
                    }
                });

            }
        }
    }

    private void selectCatalog(String catName) {
        Catalog c;
        int idx=0;
        for (TableData.Row row  : catTable.getTable().getRowValues()) {
            c= new Catalog(((BaseTableData.RowData)row));
            if (c.getQueryCatName().equals(catName)) {
                catListener.useDefaultCols = false;
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
        private boolean useDefaultCols = true;

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
