/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.creator;

import com.google.gwt.dom.client.Style;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.VisibleListener;
import edu.caltech.ipac.firefly.ui.imageGrid.BasicImageGrid;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: Dec 21, 2011
 *
 * @author loi
 * @version $Id: ImageGridViewCreator.java,v 1.21 2012/09/20 03:26:06 tlau Exp $
 */
public class ImageGridViewCreator implements TableViewCreator {



    public TablePanel.View create(Map<String, String> params) {
        ImageGridView view = new ImageGridView(params);
        return view;
    }


    public static class ImageGridView extends Composite implements TablePanel.View, VisibleListener {
        public static final Name NAME = new Name("Grid View", "Display the content in image grid");
        private BasicImageGrid grid;
        private DockLayoutPanel container = new DockLayoutPanel(Style.Unit.PX);
        private int viewIndex = -1;
        private Map<String, String> params;
//        private DataSet lastResults;
        private int lastStartIdx = -1;
        private TablePanel tablePanel;
        private int initialPS = -1;
        private boolean isActive = false;
        private int gridPageSize;
        private boolean ignoreEvents = false;
        private PreviewImageGridCreator.ImageGridPreviewData previewData = null;
        private boolean isHidden = false;

        private String lastResults = "";

        public ImageGridView(Map<String, String> params) {
            this.params = params;
            int index = StringUtils.getInt(params.get("Index"), -1);
            setViewIndex(index);
            previewData = PreviewImageGridCreator.makePreviewData(params);
            grid = new BasicImageGrid(previewData);
            grid.setEnableChecking(previewData.getEnableChecking()); //default is true.
            grid.setEnablePdfDownload(previewData.getEnablePdfDownload());
            grid.setOnlyShowingFilteredResults(previewData.getOnlyShowingFilteredResults());
            grid.setShowDrawingLayers(previewData.getShowDrawingLayers());
            container.setSize("100%", "100%");
            container.add(grid);
            grid.setSize("100%", "100%");
            initWidget(container);
            gridPageSize = StringUtils.getInt(params.get(CommonParams.PAGE_SIZE), 20);

        }


        public void setViewIndex(int viewIndex) {
            this.viewIndex = viewIndex;
        }

        protected void loadTable(DataSet data) {
            lastStartIdx = tablePanel.getTable().getAbsoluteFirstRowIndex();
            grid.updateDisplay();
            tablePanel.getPagingBar().setIsLoading(true);
            grid.loadTable(data);
            tablePanel.showOptionsButton(false);
            tablePanel.showPopOutButton(false);
            syncSelectedWithTable();
            syncHighlightedWithTable();
            if (previewData.getSelectAllPlots()) {
                tablePanel.getDataset().selectAll();
            }
        }

        private void syncHighlightedWithTable() {

            ignoreEvents = true;
            try {
                int hlIdx = tablePanel.getTable().getHighlightedRowIdx();
                if (hlIdx >= 0) {
                    hlIdx += tablePanel.getTable().getAbsoluteFirstRowIndex();
                    if (hlIdx >= 0) {
                        int hl = hlIdx % gridPageSize;
                        grid.setSelectedPlotIdx(hl);
                    }
                }
            } finally {
                ignoreEvents = false;
            }
        }

        private TableServerRequest makeRequest(int startIdx) {
            String searchId = params.get(CommonParams.SEARCH_PROCESSOR_ID);
            TableServerRequest req = new TableServerRequest(searchId);
            for(String key : params.keySet()) {
                req.setParam( new Param(key, params.get(key)));
            }
            req.setFilters(tablePanel.getDataModel().getFilters());
            req.setSortInfo(tablePanel.getDataModel().getSortInfo());
            req.setPageSize(gridPageSize);
            req.setStartIndex(startIdx);
            req.setParam(Request.ID_KEY, searchId);

            return req;
        }

        private void callServer(final TableServerRequest req) {
            ServerTask task = new ServerTask<RawDataSet>() {
                public void onSuccess(RawDataSet result) {
                    String newResults = result.getDataSetString();
                    if (!newResults.equals(lastResults)) {
                        DataSet data = DataSetParser.parse(result);
                        if (data != null) {
                            loadTable(data);
                        }
                        lastResults = newResults;
                    } else {
                        syncSelectedWithTable();
                        syncHighlightedWithTable();
                    }
                    ignoreEvents = false;
                }

                @Override
                protected void onCancel(boolean byUser) {
                    super.onCancel(byUser);
                    ignoreEvents = false;
                }

                @Override
                protected void onFailure(Throwable caught) {
                    super.onFailure(caught);
                    ignoreEvents = false;
                }

                public void doTask(AsyncCallback<RawDataSet> passAlong) {
                    ignoreEvents = true;
                    SearchServices.App.getInstance().getRawDataSet(req, passAlong);

                }
            };
            task.start();
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
            return this;
        }

        public void onViewChange(TablePanel.View newView) {
            if (newView.equals(this)) {
                isActive = true;
                loadGrid(false);
                onShow();
            } else {
                tablePanel.showOptionsButton(true);
                tablePanel.showPopOutButton(true);
                if (isActive) {
                    isActive = false;
                    if (gridPageSize != initialPS) {
                        int startIdx = tablePanel.getTable().getAbsoluteFirstRowIndex();
                        int pnum = startIdx/initialPS;
                        int hl = tablePanel.getDataset().getHighlighted();
                        tablePanel.reloadTable(pnum, initialPS, hl);
                    }
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
            return IconCreator.Creator.getInstance().getGridView();
        }

        public void bind(TablePanel table) {
            tablePanel = table;
            tablePanel.getEventManager().addListener(TablePanel.ON_PAGE_LOAD, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    loadGrid(true);
                }
            });
            new TableGridConnector(tablePanel, grid);
        }

        public void bind(EventHub hub) {
            grid.bind(hub);
        }

        public boolean isHidden() {
            return isHidden;
        }

        public void setHidden(boolean flg) {
            isHidden = flg;
        }

        private void loadGrid(boolean forceload) {
            if (isActive) {
                int cPS = tablePanel.getDataModel().getPageSize();
                int startIdx = tablePanel.getTable().getAbsoluteFirstRowIndex();
                if (gridPageSize != cPS) {
                    initialPS = cPS;
                    int hlrow = tablePanel.getTable().getHighlightedRowIdx();
                    int hlIdx = hlrow < 0 ? 0 : hlrow;
                    int pnum = hlIdx/gridPageSize;
                    tablePanel.reloadTable(pnum, gridPageSize, hlIdx);
                } else {
                    if (forceload || startIdx != lastStartIdx) {
                        callServer(makeRequest(startIdx));
                    }
                }
            }
        }

        private void syncSelectedWithTable() {
            ignoreEvents = true;
            try {
                if (tablePanel.getDataset().isSelectAll()) {
                    grid.setAllChecked(true);
                } else {
                    grid.setAllChecked(false);
                    List<Integer> gsels = new ArrayList<Integer>();
                    for (int i = 0; i <tablePanel.getTable().getRowCount(); i++) {
                        int absIdx = i + tablePanel.getTable().getAbsoluteFirstRowIndex();
                        if (tablePanel.getDataset().isSelected(absIdx)) {
                            gsels.add(i);
                        }
                    }
                    if (gsels.size() > 0) {
                        grid.setCheckedPlotIdx(gsels);
                    }
                }
            } finally {
                ignoreEvents = false;
            }
        }

        private void syncSelectedWithGrid() {
            ignoreEvents = true;
            try {
                if (grid.isAllChecked()) {
                    tablePanel.getDataset().selectAll();
                } else {
                    ArrayList<Integer> idxs = grid.getCheckedPlotIdx();
                    for (int i = 0; i <tablePanel.getTable().getRowCount(); i++) {
                        int absIdx = i + tablePanel.getTable().getAbsoluteFirstRowIndex();
                        if (idxs.contains(i)) {
                            tablePanel.getDataset().select(absIdx);
                        } else {
                            tablePanel.getDataset().deselect(absIdx);
                        }
                    }
                }
            } finally {
                ignoreEvents = false;
            }
        }

        public void onShow() {
            if (grid != null) grid.onShow();
        }

        public void onHide() {
            if (grid != null) grid.onHide();
        }

        private class TableGridConnector {
            private TablePanel tablePanel;
            private BasicImageGrid grid;

            public TableGridConnector(TablePanel tablePanel, BasicImageGrid grid) {
                this.tablePanel = tablePanel;
                this.grid = grid;
                this.tablePanel.getEventManager().addListener(new TableListener());
                this.grid.getEventManager().addListener(new GridListener());
            }

            class GridListener implements WebEventListener {
                public void eventNotify(WebEvent ev) {
                    if (ignoreEvents) { return; }

                    Name event = ev.getName();
                    if (event.equals(BasicImageGrid.ON_CHECKED_PLOT_CHANGE)) {
                        if (grid.getCheckedPlotIdx().size() > 0) {
                            syncSelectedWithGrid();
                        }
                    } else if (event.equals(BasicImageGrid.ON_SELECTED_PLOT_CHANGE)) {
                        ignoreEvents = true;
                        tablePanel.highlightRow(true, tablePanel.getTable().getAbsoluteFirstRowIndex() + grid.getSelectedPlotIdx());
                        ignoreEvents = false;
                    } else if (event.equals(BasicImageGrid.ON_ALL_CHECKED_PLOT_CHANGE)) {
                        ignoreEvents = true;
                        if (grid.isAllChecked()) {
                            tablePanel.getDataset().selectAll();
                        } else {
                            tablePanel.getDataset().deselectAll();
                        }
                        ignoreEvents = false;
                    } else if (event.equals(BasicImageGrid.ON_ALL_PLOTS_DONE)) {
                        tablePanel.getPagingBar().setIsLoading(false);
                    }
                }
            }
            class TableListener implements WebEventListener {
                public void eventNotify(WebEvent ev) {
                    if (ignoreEvents || !isActive) { return; }

                    Name event = ev.getName();
                    if (event.equals(TablePanel.ON_ROWHIGHLIGHT_CHANGE)) {
                        int hlIdx = tablePanel.getDataModel().getCurrentData().getHighlighted();
                        if (hlIdx >= 0) {
                            ignoreEvents = true;
                            int hl = hlIdx % gridPageSize;
                            grid.setSelectedPlotIdx(hl);
                            ignoreEvents = false;
                        }
                    } else if (event.equals(TablePanel.ON_ROWSELECT_CHANGE)) {
                        syncSelectedWithTable();
                    }
                }
            }
        }

    }
}
