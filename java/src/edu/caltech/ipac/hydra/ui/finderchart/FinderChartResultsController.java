package edu.caltech.ipac.hydra.ui.finderchart;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DynResultsHandler;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.SDSSRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.fuse.data.ConverterStore;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseEventWorker;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.previews.MultiDataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TableResultsDisplay;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
* Date: 9/12/14
*
* @author loi
* @version $Id: $
*/
public class FinderChartResultsController extends BaseEventWorker implements DynResultsHandler {
    private TabPane sourceTab;
    private TablePanel sourceTable;
    private TabPane imageTab;
    private MultiDataViewerPreview imageGrid;
    private TabPane catalogTab;
    private SplitLayoutPanelFirefly layoutPanel;
    private TableResultsDisplay catalogsDisplay;
    private boolean isInit = false;

    public FinderChartResultsController() {

        super(ID);
        setEventsByName(Arrays.asList(EventHub.ON_TABLE_ADDED, EventHub.ON_ROWHIGHLIGHT_CHANGE, EventHub.ON_TABLE_REMOVED));
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);
        init();
    }

    private void init() {
        layoutPanel = new SplitLayoutPanelFirefly();
        sourceTab = new TabPane();
        sourceTab.setSize("100%", "100%");
        sourceTab.setTabPaneName("Targets");

        catalogsDisplay = new TableResultsDisplay(getEventHub());
        catalogTab = catalogsDisplay.getTabPane();
        catalogTab.setSize("100%", "100%");

        imageTab = new TabPane();
        imageTab.setSize("100%", "100%");

        layoutPanel.addSouth(catalogTab, 350);
        layoutPanel.addWest(sourceTab, 350);
        layoutPanel.add(imageTab);
        layoutPanel.setSize("100%", "100%");
    }

    public Widget processRequest(final Request inputReq, AsyncCallback<String> callback, final EventHub hub, PrimaryTableUILoader loader, SearchTypeTag searchTypeTag) {

        imageGrid = new MultiDataViewerPreview();
        hub.bind(imageGrid);
        imageGrid.bind(hub);
        imageTab.addTab(imageGrid.getDisplay(), "Finder Chart");

        // create source table.
        Map <String, String> tableParams = new HashMap<String, String>();
        tableParams.put(TablePanelCreator.TITLE, "Targets");
        tableParams.put(TablePanelCreator.SHORT_DESC, "List of search targets");
        tableParams.put(TablePanelCreator.QUERY_SOURCE, "finderChart");
        tableParams.put("QUERY_ID", "QueryFinderChartWeb");

        TableServerRequest tsReq = new TableServerRequest("QueryFinderChartWeb", inputReq);
        tsReq.setParam("maxSearchTargets", "1000");
        tsReq.setParam(DynUtils.QUERY_ID, "finderChart");
        final PrimaryTableUI primary = Application.getInstance().getWidgetFactory().createPrimaryUI(WidgetFactory.TABLE, tsReq, tableParams);
        primary.bind(hub);
        loader.addTable(primary);
        loader.loadAll();
        sourceTab.addTab(primary.getDisplay(), "Targets");
        sourceTable = (TablePanel) sourceTab.getTab("Targets").getContent();

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                ConverterStore.get("FINDER_CHART").initArtifactLayers(hub); //TODO - uncomment to enable artifacts
            }
        });


        sourceTable.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                sourceTable.getEventManager().removeListener(TablePanel.ON_INIT, this);
                isInit = true;
                processCatalog(inputReq);
                // do this after the initial query..
                // if it's a table upload, we can use the file with resolved targets coordinates.
            }
        });

        new NewTableEventHandler(hub, catalogTab);


        return layoutPanel;
    }

    private void processCatalog(ServerRequest tsReq) {
        boolean doOverlay = tsReq.getBooleanParam("overlay_catalog");
        if (doOverlay) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, catalogTab);
            String sources = tsReq.getParam("sources");
            if (sources.contains("SDSS")) {
                addSDSSCatalog("SDSS", tsReq, "sdss_radius");
            }
            if (sources.contains("twomass")) {
                addCatalog("2MASS", tsReq, "fp_psc", "2mass_radius");
            }
            if (sources.contains("WISE")) {
                addCatalog("WISE", tsReq,"wise_allwise_p3as_psd", "wise_radius");
            }
            if (sources.contains("IRIS")) {
                addCatalog("IRIS", tsReq,"iraspsc", "iras_radius");
            }
        }
    }

    private void addSDSSCatalog(String title, ServerRequest tsReq, String radiusFieldStr) {
        double radiusArcSec = tsReq.getDoubleParam(radiusFieldStr);
        if (radiusArcSec == Double.NaN) {
            radiusArcSec = 50;
        }
        double radiusArcMin = radiusArcSec/60.0;
        SDSSRequest req = new SDSSRequest();
        req.setRadiusArcmin(radiusArcMin);
        boolean one_to_one = tsReq.getBooleanParam(CatalogRequest.ONE_TO_ONE);
        req.setNearestOnly(one_to_one);

        String uploadFname = tsReq.getParam("filename");
        if (StringUtils.isEmpty(uploadFname)) {
            req.setUserTargetWorldPoint(tsReq.getParam(ReqConst.USER_TARGET_WORLD_PT));
            if (tsReq.getBooleanParam("catalog_by_img_boundary")) {
                radiusArcMin = tsReq.getDoubleParam("subsize") * 60 /2;
                req.setRadiusArcmin(radiusArcMin);
                req.setParam(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.BOX.getDesc());
            } else {
                req.setParam(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.CONE.getDesc());
            }
        } else {
            req.setFilename(uploadFname);
        }
        req.setMeta("CatalogHints", "subgroup=" + title);
        MonitorItem sourceMonItem = SearchAdmin.getInstance().submitSearch(req, title);

    }

    private void addCatalog(String title, ServerRequest tsReq, String catalog, String radiusFieldStr) {

        double radiusArcSec = tsReq.getDoubleParam(radiusFieldStr);
        if (radiusArcSec == Double.NaN) {
            radiusArcSec = 50;
        }

        CatalogRequest gatorReq = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        gatorReq.setRadUnits(CatalogRequest.RadUnits.ARCSEC);
        gatorReq.setGatorHost("irsa");
        gatorReq.setDDOnList(true);
        gatorReq.setQueryCatName(catalog);
        gatorReq.setRadius(radiusArcSec);
        gatorReq.setMeta("CatalogHints", "subgroup=" + title);
        if (tsReq.getBooleanParam(CatalogRequest.ONE_TO_ONE)) {
            gatorReq.setParam(CatalogRequest.ONE_TO_ONE, "1");
        }

        String uploadFname = tsReq.getParam("filename");
        if (StringUtils.isEmpty(uploadFname)) {
            gatorReq.setParam(ReqConst.USER_TARGET_WORLD_PT, tsReq.getParam(ReqConst.USER_TARGET_WORLD_PT));
            if (tsReq.getBooleanParam("catalog_by_img_boundary")) {
                gatorReq.setMethod(CatalogRequest.Method.BOX);
                double size = tsReq.getDoubleParam("subsize") * 60 * 60;
                gatorReq.setSide(size);
            } else {
                gatorReq.setMethod(CatalogRequest.Method.CONE);
            }
        } else {
            gatorReq.setMethod(CatalogRequest.Method.TABLE);
            gatorReq.setFileName(uploadFname);
        }

        MonitorItem sourceMonItem = SearchAdmin.getInstance().submitSearch(gatorReq, title);
    }


    protected void handleEvent(WebEvent ev) {
        if (sourceTable.isInit()) {
            handleSourceTable();
        } else {
            sourceTable.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    handleSourceTable();
                    sourceTable.getEventManager().removeListener(TablePanel.ON_INIT, this);
                }
            });
        }

        if (catalogTab != null && catalogTab.getWidgetCount() > 0) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, catalogTab);
        } else {
            GwtUtil.SplitPanel.hideWidget(layoutPanel, catalogTab);
        }
    }

    private void handleSourceTable() {
        sourceTable.clearToolButtons(true, false, false);
        sourceTable.getTable().showFilters(false);
        if (sourceTable != null && sourceTable.getDataModel().getTotalRows() > 1) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, sourceTab);
        } else {
            GwtUtil.SplitPanel.hideWidget(layoutPanel, sourceTab);
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
