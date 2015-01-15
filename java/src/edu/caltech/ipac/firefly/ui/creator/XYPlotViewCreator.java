/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;


import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.VisibleListener;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotData;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.util.StringUtils;

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


    public static class XYPlotView implements TablePanel.View, VisibleListener {

        public static String INDEX_KEY = "Index";

        public static final Name NAME = new Name("XY Plot View", "Display the content as an XY plot");
        private int viewIndex = 5;
        private Map<String, String> params;
        private TablePanel tablePanel = null;
        private boolean isActive = false;
        private boolean isPlotUpdated = false;
        XYPlotWidget xyPlotWidget = null;
        //WebEventListener listener = null;
        private boolean isHidden = false;


        public XYPlotView(Map<String, String> params) {
            this.params = params;
            int index = StringUtils.getInt(params.get(INDEX_KEY), -2);
            if (index > -2) { setViewIndex(index); }
        }

        public XYPlotWidget getXYPlotWidget() {
            if (xyPlotWidget == null) {
                int xSize = (tablePanel != null && tablePanel.getOffsetWidth() > 350) ?  tablePanel.getOffsetWidth() - 50 : 300;
                int ySize = (tablePanel != null && tablePanel.getOffsetHeight() > 230) ? tablePanel.getOffsetHeight() - 50 :180;
                XYPlotMeta xyPlotMeta = new XYPlotMeta(null, xSize, ySize, new CustomMetaSource(params));
                xyPlotWidget= new XYPlotWidget(xyPlotMeta);
                xyPlotWidget.addListener(new XYPlotWidget.NewDataListener(){
                    public void newData(XYPlotData data) {
                        xyPlotWidget.onResize();
                    }
                });
                xyPlotWidget.setSize("100%", "100%");
            }
            return xyPlotWidget;
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
            return getXYPlotWidget();
        }

        public void onViewChange(TablePanel.View newView) {
            setActive(newView.equals(this));
        }

        private void setActive (boolean active) {
            if (active) {
                if (!isActive) {
                    isActive = true;
                    //set listener to update view whenever page loads (ex. on filter update)
                    //if (listener != null) {
                    //    tablePanel.getEventManager().addListener(TablePanel.ON_PAGE_LOAD, listener);
                    //    tablePanel.getEventManager().addListener(TablePanel.ON_STATUS_UPDATE, listener);
                    //}
                    tablePanel.showToolBar(false);
                    tablePanel.showOptionsButton(false);
                    tablePanel.showPopOutButton(false);
                    updatePlot();
                    onShow();
                }
            } else {
                if (isActive) {
                    isActive = false;
                    //remove listener
                    //if (listener != null) {
                    //    tablePanel.getEventManager().removeListener(TablePanel.ON_PAGE_LOAD, listener);
                    //    tablePanel.getEventManager().removeListener(TablePanel.ON_STATUS_UPDATE, listener);
                    //}
                    tablePanel.showToolBar(true);
                    tablePanel.showOptionsButton(true);
                    tablePanel.showPopOutButton(true);
                }
                onHide();
            }
        }

        private void updatePlot() {
            if (tablePanel != null && !isPlotUpdated) {
                DataSetTableModel tableModel = tablePanel.getDataModel();
                if (tableModel != null) {
                    String title = tablePanel.getShortDesc();
                    xyPlotWidget.makeNewChart(tableModel, StringUtils.isEmpty(title) ? "X,Y view of the selected table columns":title);
                    isPlotUpdated = true;
                }
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

            //listener = new WebEventListener(){
            //    public void eventNotify(WebEvent ev) {
            //        if (ev.getName().equals(TablePanel.ON_PAGE_LOAD)) {
            //            if (tablePanel != null)  {
            //                updatePlot();
            //            }
            //        }
                    //else if (ev.getName().equals(TablePanel.ON_STATUS_UPDATE) &&
                    //        ev.getData().equals(Boolean.TRUE)) {
                    //    getXYPlotWidget().updateTableInfo();
                    //}
            //    }
            //};


            if (table.isInit()) {
                if (tablePanel.isActiveView(XYPlotView.this.getName())) {
                    setActive(true);
                }
            } else {
                tablePanel.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (tablePanel.isActiveView(XYPlotView.this.getName())) {
                            setActive(true);
                        }
                        tablePanel.getEventManager().removeListener(this);
                    }
                });
            }
        }

        public void bind(EventHub hub) {
            // TODO - do I need to?
        }

        public boolean isHidden() {
            return isHidden;
        }

        public void setHidden(boolean flg) {
            isHidden = flg;
        }

        public void onShow() {
            getXYPlotWidget().setVisible(true);
            AllPlots.getInstance().registerPopout(getXYPlotWidget());
        }

        public void onHide() {
            getXYPlotWidget().setVisible(false);
            AllPlots.getInstance().deregisterPopout(getXYPlotWidget());
        }
    }


        /**
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
         */


    
}
