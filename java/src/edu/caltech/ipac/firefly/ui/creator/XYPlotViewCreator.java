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
        public static String SEARCH_PROCESSOR_ID_KEY = "searchProcessorId";

        public static final Name NAME = new Name("XY Plot View", "Display the content as an XY plot");
        private int viewIndex = 5;
        private Map<String, String> params;
        private TablePanel tablePanel = null;
        private boolean isActive = false;
        private String searchProcessorId = null;
        XYPlotViewPanel viewPanel = null;


        public XYPlotView(Map<String, String> params) {
            this.params = params;
            int index = StringUtils.getInt(params.get(INDEX_KEY), -2);
            if (index > -2) { setViewIndex(index); }
            searchProcessorId = params.get(SEARCH_PROCESSOR_ID_KEY);
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

        private TableServerRequest makeRequest(int startIdx) {
            TableServerRequest tableRequest = tablePanel.getLoader().getRequest();
            TableServerRequest req = new TableServerRequest(searchProcessorId == null ? tableRequest.getRequestId() : searchProcessorId,
                    tableRequest);
            //for(String key : params.keySet()) {
            //    req.setParam( new Param(key, params.get(key)));
            //}
            req.setFilters(tablePanel.getLoader().getFilters());
            req.setSortInfo(tablePanel.getLoader().getSortInfo());
            req.setStartIndex(startIdx);

            return req;
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
            if (newView.equals(this)) {
                isActive = true;
                tablePanel.showToolBar(false);
                tablePanel.showOptions(false);
                tablePanel.showPopOutButton(false);
                viewPanel.updatePlot(this);
                onShow();
            } else {
                if (isActive) {
                    tablePanel.showToolBar(true);
                    tablePanel.showOptions(true);
                    tablePanel.showPopOutButton(true);                                    
                    isActive = false;
                }
                onHide();
            }

        }

        public TablePanel getTablePanel() {
            return null;
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
            if (table.isInit()) {
                getViewPanel().bind(tablePanel);
                if (getViewIdx() == 0) {
                    getViewPanel().updatePlot(this);
                }
            } else {
                tablePanel.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        getViewPanel().bind(tablePanel);
                        if (getViewIdx() == 0) {
                            getViewPanel().updatePlot(XYPlotView.this);
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

        public static int MAX_POINTS_FOR_UNRESTRICTED_COLUMNS = 1000;

        public static String NUM_POINTS_KEY = "xyplot.numPoints";

        SplitLayoutPanel container;
        InputField numPoints;
        HTML tableInfo;
        ListBox xColList;
        ListBox yColList;
        XYPlotMeta xyPlotMeta;
        XYPlotWidget xyPlotWidget;
        List<String> numericCols;
        TablePanel tablePanel = null;

        String currentBaseTableReq = null;
        boolean plotUpdateNeeded = false;

        public XYPlotViewPanel(final XYPlotView view, Map<String, String> params) {
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
            ftPanel.setWidget(row, 1, numPoints);
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

        public void bind(TablePanel tablePanel) {
            this.tablePanel = tablePanel;
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