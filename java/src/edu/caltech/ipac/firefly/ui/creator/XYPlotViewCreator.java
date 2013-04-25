package edu.caltech.ipac.firefly.ui.creator;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.FormBuilder;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.table.FilterToggle;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.graph.*;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tatiana
 * @version $Id: XYPlotViewCreator.java,v 1.10 2012/12/11 21:10:01 tatianag Exp $
 */
public class XYPlotViewCreator implements TableViewCreator {


    public TablePanel.View create(Map<String, String> params) {
        return new XYPlotView(params);
    }


    public static class XYPlotView implements TablePanel.View {

        public static String INDEX_KEY = "Index";

        public static final Name NAME = new Name("XY Plot View", "Display the content as an XY plot");
        private int viewIndex = 5;
        private Map<String, String> params;
        private TablePanel tablePanel = null;
        private boolean isActive = false;
        XYPlotViewPanel viewPanel = null;
        WebEventListener listener = null;


        public XYPlotView(Map<String, String> params) {
            this.params = params;
            int index = StringUtils.getInt(params.get(INDEX_KEY), -2);
            if (index > -2) { setViewIndex(index); }
        }

        public XYPlotViewPanel getViewPanel() {
            if (viewPanel == null) {
                viewPanel = new XYPlotViewPanel(this, params);
            }
            return viewPanel;
        }


        public void setViewIndex(int viewIndex) {
            this.viewIndex = viewIndex;
        }


//====================================================================
//  implements TablePanel.View
//====================================================================

        public int getViewIdx() {
            return viewIndex;
        }

        public Name getName() {
            return NAME;
        }

        public String getShortDesc() {
            return NAME.getDesc();
        }

        public Widget getDisplay() {
            return getViewPanel();
        }

        public void onViewChange(TablePanel.View newView) {
            setActive(newView.equals(this));
        }

        private void setActive (boolean active) {
            if (active) {
                if (!isActive) {
                    isActive = true;
                    //set listener to update view whenever page loads (ex. on filter update)
                    if (listener != null) {
                        tablePanel.getEventManager().addListener(TablePanel.ON_PAGE_LOAD, listener);
                        tablePanel.getEventManager().addListener(TablePanel.ON_STATUS_UPDATE, listener);
                    }
                    tablePanel.showToolBar(false);
                    tablePanel.showOptionsButton(false);
                    tablePanel.showPopOutButton(false);
                    getViewPanel().update();
                    onShow();
                }
            } else {
                if (isActive) {
                    isActive = false;
                    //remove listener
                    if (listener != null) {
                        tablePanel.getEventManager().removeListener(TablePanel.ON_PAGE_LOAD, listener);
                        tablePanel.getEventManager().removeListener(TablePanel.ON_STATUS_UPDATE, listener);
                    }
                    tablePanel.showToolBar(true);
                    tablePanel.showOptionsButton(true);
                    tablePanel.showPopOutButton(true);
                }
                onHide();
            }
        }

        public TablePanel getTablePanel() {
            return tablePanel;
        }

        public void onMaximize() {
        }

        public void onMinimize() {
        }

        public ImageResource getIcon() {
            return IconCreator.Creator.getInstance().getXYPlotView();
        }

        public void bind(TablePanel table) {
            tablePanel = table;

            listener = new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    if (ev.getName().equals(TablePanel.ON_PAGE_LOAD)) {
                    getViewPanel().update();
                    } else if (ev.getName().equals(TablePanel.ON_STATUS_UPDATE) &&
                            ev.getData().equals(Boolean.TRUE)) {
                        getViewPanel().updateTableInfo();
                    }
                }
            };


            if (table.isInit()) {
                getViewPanel().bind(tablePanel);
                if (tablePanel.isActiveView(XYPlotView.this.getName())) {
                    setActive(true);
                }
            } else {
                tablePanel.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        getViewPanel().bind(tablePanel);
                        if (tablePanel.isActiveView(XYPlotView.this.getName())) {
                            setActive(true);
                        }
                        tablePanel.getEventManager().removeListener(this);
                    }
                });
            }
        }

        public void bind(TablePreviewEventHub hub) {
            // TODO - do I need to?
        }

        public boolean isHidden() {
            return false;
        }


        public void onShow() {
            getViewPanel().setVisible(true);
        }

        public void onHide() {
            getViewPanel().setVisible(false);
        }
         
    }

    public static class XYPlotViewPanel extends ResizeComposite {

        public static int MAX_POINTS_FOR_UNRESTRICTED_COLUMNS = 10000;

        SplitLayoutPanel container;
        InputField numPoints;
        HTML tableInfo;
        SimplePanel filterPanel;
        FilterToggle filterToggle;
        ListBox xColList;
        ListBox yColList;
        XYPlotMeta xyPlotMeta;
        XYPlotWidget xyPlotWidget;
        List<String> numericCols;
        TablePanel tablePanel = null;
        XYPlotView view = null;

        String currentBaseTableReq = null;
        boolean plotUpdateNeeded = false;

        public XYPlotViewPanel(final XYPlotView view, Map<String, String> params) {
            this.view = view;
            FlexTable ftPanel = new FlexTable();
            //DOM.setStyleAttribute(ftPanel.getElement(), "padding", "5px");
            ftPanel.setCellSpacing(10);
            numPoints = FormBuilder.createField("XYPlotViewPanel.maxPoints");
            xColList = new ListBox();
            xColList.setWidth("160px");
            yColList = new ListBox();
            yColList.setWidth("160px");
            int row = 0;
            ftPanel.setHTML(row, 0, "Max Points: ");
            filterPanel = new SimplePanel();
            ftPanel.setWidget(row, 1, GwtUtil.leftRightAlign(
                    new Widget[]{numPoints}, new Widget[]{filterPanel}));
            row++;
            ftPanel.setHTML(row, 0, "X Column: ");
            ftPanel.setWidget(row, 1, xColList);
            row++;
            ftPanel.setHTML(row, 0, "Y Column: ");
            ftPanel.setWidget(row, 1, yColList);

            xyPlotMeta = new XYPlotMeta(null, 300, 180, new CustomMetaSource(params));
            xyPlotWidget= new XYPlotWidget(xyPlotMeta);
            xyPlotWidget.addListener(new XYPlotWidget.NewDataListener(){
                public void newData(XYPlotData data) {
                    if (data!= null && numericCols!=null && numericCols.size()>0) {
                        int xColIndex = numericCols.indexOf(data.getXCol());
                        int yColIndex = numericCols.indexOf(data.getYCol());
                        if (xColIndex>=0) xColList.setSelectedIndex(xColIndex);
                        if (yColIndex>=0) yColList.setSelectedIndex(yColIndex);
                        xyPlotWidget.onResize();
                    }
                }
            });

            VerticalPanel controlPanel = new VerticalPanel();
            controlPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
            tableInfo = GwtUtil.makeFaddedHelp("No table info yet");
            controlPanel.add(tableInfo);
            controlPanel.add(ftPanel);
            controlPanel.add(GwtUtil.makeButton("Plot", "Plot the selected columns", new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    updatePlot(view);
                }
            }));
            controlPanel.addStyleName("content-panel");

            container = new SplitLayoutPanel();
            container.addWest(controlPanel, 300);
            container.add(xyPlotWidget);
            container.setSize("100%", "100%");
            
            initWidget(container);
        }

        public void bind(final TablePanel tablePanel) {
            this.tablePanel = tablePanel;
            this.filterToggle = new FilterToggle(tablePanel);
            filterPanel.setWidget(filterToggle);

           // populate column names
            List<TableDataView.Column> columnLst = tablePanel.getDataset().getColumns();
            numericCols = new ArrayList<String>();
            xColList.clear();
            yColList.clear();
            int nCols=0, cCols=0;
            String item;
            for (TableDataView.Column c : columnLst) {
                if (c.getType() != null) {
                    // "c", "char", "date" are nonnumeric types
                    if (c.getType().startsWith("c") || c.getType().equals("date")) {
                        cCols++;
                    } else {
                        nCols++;
                        numericCols.add(c.getName());
                        // item text: colname (units)
                        item = c.getUnits();
                        if (StringUtils.isEmpty(item) || item.equals("null")) {
                            item = c.getTitle();
                        } else {
                            item = c.getTitle()+" ("+item+")";
                        }
                        xColList.addItem(item);
                        yColList.addItem(item);
                    }
                }
            }
            if (nCols>0) {
                String xCol = xyPlotMeta.findXColName(numericCols);
                String yCol = xyPlotMeta.findYColName(numericCols);
                int xIdx = 0;
                int yIdx = 0;
                if (!StringUtils.isEmpty(xCol)) {
                    xIdx = numericCols.indexOf(xCol);
                }
                if (xIdx+1 < numericCols.size()) {
                    yIdx = xIdx+1;
                }
                xColList.setSelectedIndex(xIdx);

                if (!StringUtils.isEmpty(yCol)) {
                    yIdx = numericCols.indexOf(yCol);
                }
                yColList.setSelectedIndex(yIdx);
            }
            tableInfo.setHTML("TABLE INFORMATION<br>"+tablePanel.getDataset().getTotalRows()+" rows, "+nCols+"/"+(cCols+nCols)+" columns (numeric/all)");

        }

        private void update() {
            if (tablePanel != null) {
                filterToggle.reinit();
                updatePlot(view);
            }
        }

        private void updateTableInfo() {
            String currentHtml = tableInfo.getHTML();
            // total rows could change due to filtering
            if (! StringUtils.isEmpty(currentHtml)) {
                String [] parts = currentHtml.split(",");
                if (parts.length > 1) {
                    boolean tableNotLoaded = !tablePanel.getDataset().getMeta().isLoaded();
                    boolean filtered = tablePanel.getDataModel().getFilters().size()>0;
                    String newHtml = "TABLE INFORMATION<br>"+tablePanel.getDataset().getTotalRows()
                            +(tableNotLoaded ? "+" : "")
                            +(filtered ? " filtered":"")+" rows, "+parts[1];
                    tableInfo.setHTML(newHtml);
                }
            }
        }

        public void updatePlot(XYPlotView view) {
            boolean serverCallNeeded;
            if (numPoints.validate()) {
                TableServerRequest req = view.makeRequest(0);
                String newBaseTableReq = req.toString(); // without page and start idx
                if (newBaseTableReq.equals(currentBaseTableReq)) {
                    serverCallNeeded = false;
                } else {
                    currentBaseTableReq = newBaseTableReq;
                    serverCallNeeded = true;
                    updateTableInfo();
                }
                int nPointsRequested = Integer.parseInt(numPoints.getValue());
                req.setPageSize(nPointsRequested);
                String xCol = numericCols.get(xColList.getSelectedIndex());
                String yCol = numericCols.get(yColList.getSelectedIndex());
                xyPlotMeta.userMeta.setXCol(xCol);
                xyPlotMeta.userMeta.setYCol(yCol);

                serverCallNeeded = serverCallNeeded || isServerCallNeeded(nPointsRequested);

                if (nPointsRequested>MAX_POINTS_FOR_UNRESTRICTED_COLUMNS &&
                        getTotalRows()>MAX_POINTS_FOR_UNRESTRICTED_COLUMNS) {
                    // TODO: this works only for catalogs
                    req.setParam("selcols", xCol+","+yCol);
                }

                // check if server call is needed
                if (serverCallNeeded) {
                    WebPlotRequest plotRequest = WebPlotRequest.makeRawDatasetProcessorRequest(req, "Table to plot");
                    //TODO: what should be in the title?
                    xyPlotWidget.makeNewChart(plotRequest, "X,Y view of the selected table columns");
                } else {
                    if (plotUpdateNeeded) {
                        xyPlotWidget.updateMeta(xyPlotMeta, false);
                    }
                }
            }  else {
                xyPlotWidget.removeCurrentChart();
            }
        }

        private int getTotalRows() {
            if (tablePanel != null && tablePanel.getDataset() != null) {
                return tablePanel.getDataset().getTotalRows();
            } else {
                return 0;
            }
        }

        /*
            Server call is needed if more points or columns that are not present are requested
            @param nPointsRequested new number of points requested
         */
        private boolean isServerCallNeeded(int nPointsRequested) {
            int nDataPoints = xyPlotWidget.getDataSetSize();
            if (nDataPoints == nPointsRequested || (nDataPoints == getTotalRows() && nDataPoints<=nPointsRequested)) {
                if (xyPlotMeta.userMeta.getXCol().equals(xyPlotWidget.getPlotData().getXCol()) &&
                    xyPlotMeta.userMeta.getYCol().equals(xyPlotWidget.getPlotData().getYCol())) {
                    // plot is up to date, no update is needed
                    plotUpdateNeeded = false;
                    return false;
                } else {
                    plotUpdateNeeded = true;
                }
                List<TableDataView.Column> cols = xyPlotWidget.getColumns();
                List<String> colLst = new ArrayList<String>();
                for (TableDataView.Column c : cols) {
                    colLst.add(c.getName());
                }
                return !(colLst.contains(xyPlotMeta.userMeta.getXCol()) &&
                        colLst.contains(xyPlotMeta.userMeta.getYCol()));
            } else {
                return true;
            }

        }

        @Override
        public void onResize() {
            container.onResize();
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