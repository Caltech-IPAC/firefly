package edu.caltech.ipac.fuse.ui;
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
import edu.caltech.ipac.firefly.data.DataSetInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.task.IrsaAllDataSetsTask;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.catalog.CatColumnInfo;
import edu.caltech.ipac.firefly.ui.catalog.Catagory;
import edu.caltech.ipac.firefly.ui.catalog.Catalog;
import edu.caltech.ipac.firefly.ui.catalog.CatalogItem;
import edu.caltech.ipac.firefly.ui.catalog.Proj;
import edu.caltech.ipac.firefly.ui.input.InputFieldGroup;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Trey Roby
 */
public class CatalogSelectUI implements InputFieldGroup {

    private static final WebClassProperties prop = new WebClassProperties(CatalogSelectUI.class);

    private DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);
    private Catagory selectedCategory= null;
    private DataSetInfo.DataTypes lastUserSetDataType= null;
    private SingleColumnTablePanel catTable = new SingleColumnTablePanel(prop.getTitle("catalog"),
                                                                          new FileteredDatasetLoader());
    private Catalog currentCatalog= null ;
    private final DataSetInfo dsInfo;
    private final SearchMaxChange searchMaxChange;
    private FlowPanel catDDContainerRight= new FlowPanel();
    private String SelectedColumns = "";
    private String SelectedConstraints = "";

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
            List<String> catList= new ArrayList<String>(proj.getCatagoryCount());
            for (Catagory category : proj) catList.add(category.getCatagoryName());
            final SimpleInputField catSelect= GwtUtil.createListBoxField("Catagory", "Choose Catatory",
                                                                         catList,catList.get(0));
            left.addNorth(catSelect,25);
            GwtUtil.setPadding(catSelect, 0, 0, 0, 10);
//            catSelect.addStyleName("left-floating");
            catSelect.getField().addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(ValueChangeEvent<String> event) {
                    selectedCategory= getCatalogCategory(catSelect.getValue());
                    catTable.reloadTable(0);
                }
            });
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
        wem.addListener(TablePanel.ON_ROWHIGHLIGHT_CHANGE, new CurrCatalogListener());
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

    private void updateDD() {
        catDDContainerRight.clear();

        CatddEnhancedPanel catDD= null;
        try {
            catDD = new CatddEnhancedPanel( new CatColumnInfo() {
                public void setSelectedColumns(String values) {
                    SelectedColumns = values;
                }

                public void setSelectedConstraints(String values) {
                    SelectedConstraints = values;
                }
            }, currentCatalog.getQueryCatName(), SelectedColumns, "", SelectedConstraints, true);
        } catch (Exception e) {
            WebAssert.argTst(false, "not sure what to do here");
        }

        catDDContainerRight.add(catDD);
    }


    private void postInit() {
        catTable.showToolBar(false);
        catTable.showColumnHeader(false);
        catTable.showPaggingBar(false);
        catTable.showOptionsButton(false);
//        table.getTable().setHeight("200px");

        catTable.getTable().setTableDefinition(new SingleColDefinition(
                new CatalogItem("Results", catTable.getDataset())));
        catTable.reloadTable(0);
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
// from InputFieldGroup interface
//====================================================================


    public List<Param> getFieldValues() {
        return null;  //Todo
    }

    public void setFieldValues(List<Param> list) {
        //Todo
    }

    public boolean validate() {
        return true;
    }

    public Iterator<Widget> iterator() {
        return new ArrayList<Widget>().iterator(); // todo decide what goes here
    }

    public void add(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }
    public void clear() { throw new UnsupportedOperationException("operation not allowed"); }
    public boolean remove(Widget w) { throw new UnsupportedOperationException("operation not allowed"); }






//====================================================================
// Inner classes
//====================================================================


    public class CurrCatalogListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            int idx = catTable.getTable().getHighlightedRowIdx();
            if (idx >= 0) {
                BaseTableData.RowData row = (BaseTableData.RowData) catTable.getTable().getRowValues().get(idx);
                currentCatalog = new Catalog(row);
                if (searchMaxChange!=null) searchMaxChange.onSearchMaxChange(currentCatalog.getMaxArcSec());
            }
            updateDD();
        }
    }

    public interface SearchMaxChange {
        void onSearchMaxChange(int maxArcSec);
    }


    private class FileteredDatasetLoader extends AbstractLoader<TableDataView> {

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
