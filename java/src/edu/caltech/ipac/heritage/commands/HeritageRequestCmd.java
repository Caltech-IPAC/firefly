package edu.caltech.ipac.heritage.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.CommonRequestCmd;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePrimaryDisplay;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.previews.CoveragePreview;
import edu.caltech.ipac.firefly.ui.previews.DataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.Loader;
import edu.caltech.ipac.firefly.ui.table.SelectableTablePanel;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TableGroupPreviewCombo;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.rpc.SearchServices;
import edu.caltech.ipac.heritage.searches.HeritageSearch;
import edu.caltech.ipac.heritage.ui.AorCoveragePreview;
import edu.caltech.ipac.heritage.ui.AorDetailPreview;
import edu.caltech.ipac.heritage.ui.HeritageCoverageData;
import edu.caltech.ipac.heritage.ui.HeritageDOCPreviewData;
import edu.caltech.ipac.heritage.ui.HeritagePreviewData;
import edu.caltech.ipac.heritage.ui.InstrumentPanel;
import edu.caltech.ipac.heritage.ui.MoreOptionsPanel;
import edu.caltech.ipac.heritage.ui.image.HeritageImages;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.google.gwt.user.client.ui.HTML;
//import edu.caltech.ipac.firefly.ui.table.TablePreview;

/**
 * Date: Jun 9, 2009
 *
 * @author loi
 * @version $Id: HeritageRequestCmd.java,v 1.39 2012/10/31 14:58:54 tatianag Exp $
 */
public abstract class HeritageRequestCmd extends CommonRequestCmd {

    public static final String ACTIVE_TARGET_ID= "ActiveTargetID";
    private TableGroupPreviewCombo combo;
    private HashMap<TableConfig, SelectableTablePanel> tables = new HashMap<TableConfig, SelectableTablePanel>();


    public HeritageRequestCmd(String command) {
        super(command);
    }

    public TableGroupPreviewCombo getResultsPanel() {
        return combo;
    }

    public void loadAll() {
        getTableUiLoader().loadAll();
    }

    public SelectableTablePanel addTable(TableConfig config) {
        SelectableTablePanel table = new SelectableTablePanel(config.getTitle(), config.getLoader());
        table.setShortDesc(config.getShortDesc());
        table.setStateId(config.getSearchRequest().getRequestId());
        combo.addTable(table);
        tables.put(config, table);
        getTableUiLoader().addTable(new TablePrimaryDisplay(table));
        return table;
    }

    public void addTab(Widget widget, String name, boolean closeable) {
        combo.addTab(widget, name, closeable);
    }

    //public void addPreview(TablePreview preview) {
    //    combo.getPreview().addView(preview);
    //}

    protected void createTablePreviewDisplay() {
        createTablePreviewDisplay(true);
    }

    protected void createTablePreviewDisplay(boolean addPreviews) {

        combo = addPreviews ? new TableGroupPreviewCombo() : new TableGroupPreviewCombo(null);
        combo.setStateId("Search");
        TabPane tpane = addPreviews ? combo.getPreview().getTabPane() : combo.getTabPane();
        tpane.setHelpId("results");

        Application.getInstance().getRequestHandler().registerComponent("SearchResults",
                                        RequestHandler.Context.INCL_SEARCH, combo);

        if (addPreviews) {
            //TODO: add event worker here
            if (getName().equals(SearchByPositionCmd.COMMAND_NAME)) {
                WidgetFactory factory= Application.getInstance().getWidgetFactory();
                TablePreviewEventHub hub= combo.getPreview().getEventHub();                
                Map<String,String> params=
                        StringUtils.createStringMap(
                                //CommonParams.TARGET_TYPE, "TableRow",
                                //CommonParams.TARGET_COLUMNS, "ra,dec,raj2000,decj2000,cra,cdec"
                                CommonParams.TARGET_TYPE, "QueryCenter",
                                CommonParams.INPUT_FORMAT, "GUESS",
                                EventWorker.QUERY_SOURCE, CommonParams.ALL,
                                EventWorker.ID, ACTIVE_TARGET_ID);
                EventWorker ew = factory.createEventWorker(CommonParams.ACTIVE_TARGET, params);
                hub.bind(ew);
                ew.bind(hub);
            }

            addPreviews();
        }
    }

    protected void addPreviews() {
//        final CatalogDisplay catDisplay= new CatalogDisplay(displayPanel.getDisplay().getPreview().getEventHub());
        GWT.runAsync(new GwtUtil.DefAsync() {
            public void onSuccess() {
                combo.getPreview().addView(new AorDetailPreview("Details"));
                combo.getPreview().addView(new DataViewerPreview(new HeritagePreviewData()));
                combo.getPreview().addView(new DataViewerPreview(new HeritageDOCPreviewData()));
                combo.getPreview().addView(new AorCoveragePreview());
                combo.getPreview().addView(new CoveragePreview(new HeritageCoverageData()));
            }
        });
    }


    @Override
    protected Image createCmdImage() {
        HeritageImages im= HeritageImages.Creator.getInstance();
        String iStr= getIconProperty();
        if (iStr!=null && iStr.equals(getName() + ".Icon"))  {
            return im.getQueryCampain().createImage();
        }
        return null;
    }

    @Override
    public void onComplete(int totalRows) {
        super.onComplete(totalRows);
        if (totalRows > 1) {
            new DrillDownNav(combo);
        }
    }

    @Override
    public void onLoaded(PrimaryTableUI tableUI) {
        super.onLoaded(tableUI);
        if (tableUI == null) return;

        if (tableUI.getDisplay() instanceof SelectableTablePanel) {
            SelectableTablePanel table = (SelectableTablePanel) tableUI.getDisplay();
            if (table.getDataset() != null && table.getDataset().getTotalRows() > 0) {
                HeritageSearch conf = (HeritageSearch)getConfig(table);
                conf.setTable(table);
                DownloadCmd cmd = conf.getDownloadCmd();
                if (cmd != null) {
                    table.addToolButton(cmd, false);
                }
            }
        }
    }

    private static class DrillDownNav implements StatefulWidget {
        private CheckBox drillDownSwitch;
        private DrillDownListener pbcdDDL;
        private DrillDownListener bcdDDL;

        private DrillDownNav(TableGroupPreviewCombo tab) {
            TablePanel aor = tab.getTablePanel(DataType.AOR.getTitle());
            TablePanel pbcd = tab.getTablePanel(DataType.PBCD.getTitle());
            TablePanel bcd = tab.getTablePanel(DataType.BCD.getTitle());

            drillDownSwitch = new CheckBox(" Restrict data in other tabs");
            drillDownSwitch.setTitle("If checked, PBCD will be " +
                    "restricted by selected AORs, BCD will be restricted by " +
                    "selected PBCDs, or AORs (if PBCD not selected)");
            GwtUtil.makeIntoLinkButton(drillDownSwitch);

            drillDownSwitch.setValue(true);

            Widget hicon = HelpManager.makeHelpIcon("results.restrictData");

            if (aor != null && (pbcd != null || bcd != null)) {
                aor.addToolButton(drillDownSwitch, false);
                aor.addToolWidget(hicon, false);
            }

            if (pbcd != null) {
                if (aor == null) {
                    pbcd.addToolButton(drillDownSwitch, false);
                    pbcd.addToolWidget(hicon, false);
                } else {
                    pbcdDDL = new DrillDownListener(pbcd, drillDownSwitch, new TableKeyMap(aor, "reqkey"));
                }
            }
            if (bcd != null) {
                ArrayList<TableKeyMap> limittedby = new ArrayList<TableKeyMap>();
                if (pbcd != null) {
                    limittedby.add(new TableKeyMap(pbcd, "pbcdid"));
                }
                if (aor != null) {
                    limittedby.add(new TableKeyMap(aor, "reqkey"));
                }
                if (limittedby.size() > 0) {
                    bcdDDL = new BcdDrillDownListener(bcd, drillDownSwitch, limittedby.toArray(new TableKeyMap[limittedby.size()]));
                }
            }
            Application.getInstance().getRequestHandler().registerComponent(getStateId(),
                                            RequestHandler.Context.INCL_SEARCH, this);
        }

//====================================================================
//  implements StatefulWidget
//====================================================================

        public String getStateId() {
            return "HeritageTableDisplay";
        }

        public void setStateId(String iod) {
        }

        public void recordCurrentState(Request request) {
            request.setParam("drilldown", String.valueOf(drillDownSwitch.getValue()));
            if (drillDownSwitch.getValue()) {
                if (pbcdDDL != null && !StringUtils.isEmpty(pbcdDDL.getCurFilterKey())) {
                    request.setParam("pbcdDDL", pbcdDDL.getCurFilterKey());
                }
                if (bcdDDL != null && !StringUtils.isEmpty(bcdDDL.getCurFilterKey())) {
                    request.setParam("bcdDDL", bcdDDL.getCurFilterKey());
                }
            }
        }

        public void moveToRequestState(Request request, AsyncCallback callback) {

            String isDD = request.getParam("drilldown");
            boolean isEnabled = StringUtils.isEmpty(isDD) || StringUtils.getBoolean(isDD);
            drillDownSwitch.setValue(isEnabled);
            drillDownSwitch.fireEvent(new ClickEvent(){});
            if (isEnabled) {
                if (pbcdDDL != null) {
                    String qstr = request.getParam("pbcdDDL");
                    if (!StringUtils.isEmpty(qstr)) {
                        pbcdDDL.setCurFilterKey(qstr);
                    }
                }

                if (bcdDDL != null) {
                    String qstr = request.getParam("bcdDDL");
                    if (!StringUtils.isEmpty(qstr)) {
                        bcdDDL.setCurFilterKey(qstr);
                    }
                }
            }

            callback.onSuccess(null);
        }

        public boolean isActive() {
            return true;
        }
//====================================================================

    }




    public static class BcdDrillDownListener extends DrillDownListener {

        public BcdDrillDownListener(TablePanel source, CheckBox isDrillDownSwitch, TableKeyMap... refTables) {
            super(source, isDrillDownSwitch, refTables);
        }

        public void eventNotify(WebEvent ev) {
            for (final TableKeyMap tkmap : getMappings()) {
                if (tkmap.getKey().equals("pbcdid")) {
                    TableDataView dataset = tkmap.getTable().getDataset();
                    if (dataset != null) {
                        List<Integer> selRowsList = dataset.getSelected();
                        if (selRowsList.size() > 0) {
                            handlePbcdFilter(tkmap);
                            break;
                        }
                    }
                } else {
                    super.eventNotify(ev);
                    break;
                }
            }
        }

        private void handlePbcdFilter(final TableKeyMap tkmap) {

            final String newFilterKey = makeRestrictedKey(tkmap);

            final List<String> filters = getSource().getTable().getFilters();
            if (getMySwitch().getValue()) {
                if (!newFilterKey.equals(getCurFilterKey())) {
                    if (newFilterKey.length() == 0) {
                        reload(filters, newFilterKey);
                    } else {
                        final TableDataView ds = tkmap.getTable().getDataset();
                        final List<Integer> selRowsList = tkmap.getTable().getDataset().getSelected();
                        this.setCurFilterKey(newFilterKey);
                        new ServerTask<List<Integer>>(getSource(), "Loading", false){
                            public void doTask(AsyncCallback<List<Integer>> passAlong) {
                                SearchServices.App.getInstance().getBcdIds(ds.getMeta().getSource(),
                                            selRowsList, passAlong);
                            }
                            public void onSuccess(List<Integer> result) {
                                filters.add(Loader.SYS_FILTER_CHAR + "bcdid IN " + StringUtils.toString(result, ","));
                                reload(filters, newFilterKey);
                            }
                        }.start();
                    }
                }
            } else {
                if (filters.size() != getSource().getLoader().getFilters().size()) {
                    reload(filters, "");
                }
            }
        }

    }

    protected TableConfig getConfig(SelectableTablePanel table) {
        for(Map.Entry<TableConfig, SelectableTablePanel> es : tables.entrySet()) {
            if (es.getValue().equals(table)) {
                return es.getKey();
            }
        }
        return null;
    }


//====================================================================
//
//====================================================================

    public static class DrillDownListener implements WebEventListener {
        private ArrayList<TableKeyMap> mappings = new ArrayList<TableKeyMap>();
        private TablePanel source;
        private CheckBox mySwitch;
        private String curFilterKey = "";

        public DrillDownListener(TablePanel source, final CheckBox masterSwitch, TableKeyMap... refTables) {
            this.source = source;

            for (TableKeyMap tkm : refTables) {
                tkm.getTable().getEventManager().addListener(TablePanel.ON_ROWSELECT_CHANGE, new WebEventListener(){
                        public void eventNotify(WebEvent ev) {
                            tableSelectionChanged((SelectableTablePanel)ev.getSource());
                        }
                    });
            }

            mySwitch = new CheckBox(masterSwitch.getText());
            GwtUtil.makeIntoLinkButton(mySwitch);

            Widget hicon = HelpManager.makeHelpIcon("results.restrictData");

            mySwitch.setTitle(masterSwitch.getTitle());
            mySwitch.setValue(masterSwitch.getValue());
            source.addToolButton(mySwitch, false);
            source.addToolWidget(hicon, false);


            masterSwitch.addClickHandler(new ClickHandler(){
                    public void onClick(ClickEvent event) {
//                        if (tableLoaded(masterSwitch)) {
                            mySwitch.setValue(masterSwitch.getValue());
//                        }
                    }
                });

            source.getEventManager().addListener(TablePanel.ON_SHOW, this);

            this.mySwitch.addClickHandler(new ClickHandler(){
                    public void onClick(ClickEvent event) {
//                        if (tableLoaded(mySwitch)) {
                            masterSwitch.setValue(mySwitch.getValue());
                            masterSwitch.fireEvent(new ClickEvent(){});
                            if (getSource().isVisible()) {
                                eventNotify(null);
                            }
//                        }
                    }
                });
            mappings.addAll(Arrays.asList(refTables));

        }

        protected String makeRestrictedKey(TableKeyMap tkmap) {
            final TableDataView ds = tkmap.getTable().getDataset();
            final List<Integer> selRowsList = ds.getSelected();
            List<String> filters = tkmap.getTable().getLoader().getFilters();
            SortInfo sortInfo = tkmap.getTable().getLoader().getSortInfo();
            String siStr = sortInfo == null ? "" : sortInfo.toString();
            String selRows = (ds.isSelectAll() && (filters == null || filters.size() ==0))
                                || selRowsList == null || selRowsList.size() == 0 ? ""
                                : StringUtils.toString(selRowsList, ",");

            if (selRows.length() == 0) {
                return "";
            } else {
                return StringUtils.toString(filters) + ":" + siStr + ":" + selRows;
            }
        }

        /*
        private boolean tableLoaded(CheckBox cb) {
            if (cb.getValue()) {
                if (!DrillDownListener.this.source.isTableLoaded()) {
                    DrillDownListener.this.source.showNotAllowWarning(new HTML(cb.getText() +
                            " is not allowed while the tables are loading"));
                    cb.setValue(false);
                }
            }
            return cb.getValue();
        }
        */

        private void tableSelectionChanged(SelectableTablePanel table) {

            TableKeyMap tkm = getMapping(table);
            if (tkm == null) return;

            String newFilterKey = makeRestrictedKey(tkm);
            if (!newFilterKey.equals(getCurFilterKey())) {
                setCurFilterKey("staled");
            }

            for(int i = 0; i < mappings.size(); i++) {
                if (mappings.get(i).getTable() == table) {
                    if (i > 0) {
                        for (int n = i-1; n >= 0; n--) {
                            mappings.get(n).getTable().getDataset().deselectAll();
                        }
                    }
                    return;
                }
            }
        }

        protected String getCurFilterKey() {
            return curFilterKey;
        }

        protected void setCurFilterKey(String curFilterKey) {
            this.curFilterKey = curFilterKey;
        }

        protected List<TableKeyMap> getMappings() {
            return mappings;
        }

        protected TableKeyMap getMapping(TablePanel table) {
            for(TableKeyMap tkm : mappings) {
                if (tkm.getTable() == table) {
                    return tkm;
                }
            }
            return null;
        }

        protected TablePanel getSource() {
            return source;
        }

        protected CheckBox getMySwitch() {
            return mySwitch;
        }

        protected void reload(List<String> filters, String newFilterKey) {
            getSource().getLoader().setFilters(filters);
            getSource().getDataset().deselectAll();
            setCurFilterKey(newFilterKey);
            getSource().gotoPage(0);
        }

        public void eventNotify(WebEvent ev) {
            if (!source.isVisible()) {
                return;
            }

            for (int i = 0; i < getMappings().size(); i++) {
                final TableKeyMap tkmap = getMappings().get(i);
                TableDataView dataset = tkmap.getTable().getDataset();
                if (dataset == null) {
                    continue;
                }

                final String newFilterKey = makeRestrictedKey(tkmap);

                if ( i != getMappings().size()-1 && newFilterKey.length() == 0) {
                    continue;
                }

                final List<String> filters = source.getTable().getFilters();
                if (getMySwitch().getValue()) {
                    if (!newFilterKey.equals(getCurFilterKey())) {
                        if (newFilterKey.length() > 0) {
                            setCurFilterKey(newFilterKey);

                            final List<Integer> selRowsList = tkmap.getTable().getDataset().getSelected();
                            new ServerTask<List<String>>(source, "Loading", false){
                                public void onSuccess(List<String> result) {
                                    filters.add(Loader.SYS_FILTER_CHAR + tkmap.getKey() + " IN " + StringUtils.toString(result, ","));
                                    reload(filters, newFilterKey);
                                }
                                public void doTask(AsyncCallback<List<String>> passAlong) {
                                    TablePanel table = tkmap.getTable();
                                    boolean needServerCall = false;
                                    int startIdx = table.getDataset().getStartingIdx();
                                    for(Integer i : selRowsList) {
                                        if (i < startIdx ||
                                                i >= table.getDataset().getModel().size() + startIdx) {
                                            needServerCall = true;
                                            break;
                                        }
                                    }

                                    if (needServerCall) {
                                        edu.caltech.ipac.firefly.rpc.SearchServices.App.getInstance().getDataFileValues(
                                                tkmap.getTable().getDataset().getMeta().getSource(),
                                                selRowsList, tkmap.getKey(), passAlong);
                                    } else {
                                        List<String> vals = new ArrayList<String>(selRowsList.size());
                                        for(Integer i : selRowsList) {
                                            TableData.Row row = table.getDataset().getModel().getRow(i);
                                            String v = row == null ? "" : String.valueOf(row.getValue(tkmap.getKey()));
                                            vals.add(v);
                                        }
                                        doSuccess(vals);
                                    }
                                }
                            }.start();
                        } else {
                            if (filters.size() != getSource().getLoader().getFilters().size()) {
                                reload(filters, "");
                            }
                        }
                        break;
                    }
                } else {
                    if (filters.size() != getSource().getLoader().getFilters().size()) {
                        reload(filters, "");
                        break;
                    }
                }
            }
        }
    }

    public static class TableKeyMap {
        private TablePanel table;
        private String key;

        public TableKeyMap(TablePanel table, String key) {
            this.table = table;
            this.key = key;
        }

        public TablePanel getTable() {
            return table;
        }

        public String getKey() {
            return key;
        }
    }

    @Override
    protected void doProcessRequest(final Request req, final AsyncCallback<String> callback) {

        boolean validated = true;

        // Request level validation
        if (MoreOptionsPanel.isSourceListRequested(req)) {
            double radius = req.getDoubleParam(SearchByPositionCmd.RADIUS_KEY);
            if (radius != Double.NaN) {
                if (req.getParam(SearchByPositionCmd.UPLOADED_FILE_PATH) != null) {
                    if ((int)(radius*3600.0) > SearchByPositionCmd.sourceListMaxUploadRadius) {
                        PopupUtil.showError("Validation Error", "Source List [multi-target] search radius cannot exceed "+SearchByPositionCmd.sourceListMaxUploadRadius+" arcsec.");
                        validated = false;
                    }
                } else {
                    if ((int)(radius*3600.0) > SearchByPositionCmd.sourceListMaxConeRadius) {
                        PopupUtil.showError("Validation Error", "Source List search radius cannot exceed "+SearchByPositionCmd.sourceListMaxConeRadius+" arcsec.");
                        validated = false;
                    }
                }
            }
        }
        if (MoreOptionsPanel.isIrsEnhancedRequested(req)) {
            double radius = req.getDoubleParam(SearchByPositionCmd.RADIUS_KEY);
            if (radius != Double.NaN) {
                if (req.getParam(SearchByPositionCmd.UPLOADED_FILE_PATH) != null) {
                    if ((int)(radius*3600.0) > SearchByPositionCmd.irsEnhancedMaxUploadRadius) {
                        PopupUtil.showError("Validation Error", "IRS Enhanced [multi-target] search radius cannot exceed "+SearchByPositionCmd.irsEnhancedMaxUploadRadius+" arcsec.");
                        validated = false;
                    }
                }
            }
            if (MoreOptionsPanel.isOnlyIrsEnhancedRequested(req) && !InstrumentPanel.isIrsEnhancedRequested(req)) {
                PopupUtil.showError("Validation Error", "Invalid input. No data requested.");
                validated = false;
            }
        }
        if (MoreOptionsPanel.isOnlySupermosaicRequested(req) && !InstrumentPanel.isSupermosaicRequested(req)) {
            PopupUtil.showError("Validation Error", "Invalid input. No data requested.");
            validated = false;
        }
        // CR 8780: PBCD are not available for IRAC subarray
        if (InstrumentPanel.isIracSubarrayExplicitlyRequested(req) && MoreOptionsPanel.isPbcdRequested(req)) {
            validated = false;
            String msg = "No Level 2 (PBCD) data products are produced for IRAC<br>" +
                    "subarray observations. Level 1 (BCD) data are available.";

            if (!InstrumentPanel.isIrsRequested(req) && !InstrumentPanel.isMipsRequested(req)) {
                PopupUtil.showError("Validation Error", msg);
            } else {
                PopupUtil.showWarning("Warning", msg,
                        new ClickHandler(){
                            public void onClick(ClickEvent event) {
                                validationComplete(req, callback);
                            }
                        });
            }
        }
        if (validated) validationComplete(req, callback);
    }

    private void validationComplete(Request req, AsyncCallback<String> callback) {
        super.doProcessRequest(req, callback);
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