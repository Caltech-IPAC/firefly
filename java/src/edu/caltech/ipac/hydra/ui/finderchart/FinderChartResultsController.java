package edu.caltech.ipac.hydra.ui.finderchart;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.DynResultsHandler;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DynRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FinderChartRequestUtil;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.SDSSRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.dyn.DynUtils;
import edu.caltech.ipac.firefly.data.dyn.xstream.DownloadTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.ParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.QueryTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchFormParamTag;
import edu.caltech.ipac.firefly.data.dyn.xstream.SearchTypeTag;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.fuse.data.ConverterStore;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.BadgeButton;
import edu.caltech.ipac.firefly.ui.DynDownloadSelectionDialog;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PackageTask;
import edu.caltech.ipac.firefly.ui.ShadowedPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseEventWorker;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.previews.MultiDataViewerPreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TableResultsDisplay;
import edu.caltech.ipac.firefly.ui.table.builder.PrimaryTableUILoader;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.Ref;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.PlotWidgetGroup;
import edu.caltech.ipac.firefly.visualize.PrintableUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.firefly.visualize.draw.WebLayerItem;
import edu.caltech.ipac.hydra.core.FinderChartDescResolver;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.FD_CAT_BY_BOUNDARY;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.FD_FILENAME;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.FD_OVERLAY_CAT;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.FD_SOURCES;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.FD_SUBSIZE;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.Radius;
import static edu.caltech.ipac.firefly.data.FinderChartRequestUtil.Source;

// convenience sharing of constants

/**
 * Date: 9/12/14
 *
 * @author loi
 * @version $Id: $
 */
public class FinderChartResultsController extends BaseEventWorker implements DynResultsHandler {
    private static final String FINDERCHART_GROUP_NAME = "FinderChartGroup";
    private static final String SUBGROUP_KEY = "subgroup";

    private ShadowedPanel sourceContainer;
    private TablePanel sourceTable;
    private ShadowedPanel gridContainer;
    private MultiDataViewerPreview imageGrid;
    private TabPane catalogTab;
    private SplitLayoutPanelFirefly layoutPanel;
    private TableResultsDisplay catalogsDisplay;
    private boolean isMultiPosition;

    public FinderChartResultsController() {

        super(ID);
        setEventsByName(Arrays.asList(EventHub.ON_TABLE_ADDED, EventHub.ON_ROWHIGHLIGHT_CHANGE, EventHub.ON_TABLE_REMOVED));
    }

    private void init() {
        layoutPanel = new SplitLayoutPanelFirefly();
        sourceContainer = new ShadowedPanel(null);
//        sourceContainer.setSize("100%", "100%");

        catalogsDisplay = new TableResultsDisplay(getEventHub());
        catalogTab = catalogsDisplay.getTabPane();
        catalogTab.setSize("100%", "100%");

        gridContainer = new ShadowedPanel(null);
//        gridContainer.setSize("100%", "100%");

        layoutPanel.addSouth(catalogTab, 200);
        layoutPanel.addWest(sourceContainer, 350);
        layoutPanel.add(gridContainer);
        layoutPanel.setSize("100%", "100%");
    }

    public Widget processRequest(final Request inputReq, AsyncCallback<String> callback, final EventHub hub, final Form form, PrimaryTableUILoader loader, final SearchTypeTag searchTypeTag) {

        isMultiPosition = !StringUtils.isEmpty(inputReq.getParam(FD_FILENAME));


        init();

        imageGrid = new MultiDataViewerPreview();
        imageGrid.getViewer().setPlotGroupNameRoot(FINDERCHART_GROUP_NAME);
        hub.bind(imageGrid);
        imageGrid.bind(hub);
        Widget grid = imageGrid.getDisplay();
        grid.setSize("100%", "100%");
        gridContainer.setContent(grid);

        // create source table.
        Map<String, String> tableParams = new HashMap<String, String>();
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
        sourceTable = (TablePanel) primary.getDisplay();
        sourceContainer.setContent(sourceTable);

        if (sourceTable.isInit()) {
            onResultsLoad(inputReq, searchTypeTag, hub, form);
        } else {
            sourceTable.getEventManager().addListener(TablePanel.ON_INIT, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    sourceTable.getEventManager().removeListener(TablePanel.ON_INIT, this);
                    onResultsLoad(inputReq, searchTypeTag, hub, form);
                }
            });
        }

        new NewTableEventHandler(hub, catalogTab);


        return layoutPanel;
    }

    private void onResultsLoad(Request inputReq, SearchTypeTag searchTypeTag, final EventHub hub, final Form form) {

        // -- setup active target overlay
        Map<String,String> params = new HashMap<String, String>();
        params.put(EventWorker.ID, "target");
        params.put(EventWorker.QUERY_SOURCE, "finderChart");
        params.put(CommonParams.TARGET_COLUMNS, "ra,dec");
        params.put(CommonParams.TARGET_TYPE, CommonParams.TABLE_ROW);
        EventWorker w = Application.getInstance().getWidgetFactory().createEventWorker("ActiveTarget", params);
        w.bind(hub);
        // -- end active target overlay

        // initiate artifacts
        ConverterStore.get("FINDER_CHART").initArtifactLayers(hub);

        if (sourceTable.getTable() != null) {
            sourceTable.clearToolButtons(true, false, false);
            sourceTable.getTable().showFilters(false);
        }
        if (!isMultiPosition) {
            sourceTable.onShow();
        }

        // add  catalogs
        processCatalog(inputReq);

        // do this after the initial query..
        // if it's a table upload, we can use the file with resolved targets coordinates.
        for (QueryTag qry : searchTypeTag.getQueries()) {
            if (qry.getId().equals("finderChart")) {
                final DownloadTag dlTag = qry.getDownload();
                final DynDownloadSelectionDialog dialog = DynUtils.makeDownloadDialog(dlTag, form);

                final GeneralCommand cmd = new GeneralCommand("FC Download", "Download", "Download", true) {
                    protected void doExecute() {
                        DownloadRequest dlreq = makeDownlaodRequest(sourceTable.getDataModel().getRequest(), dlTag, form);
                        dialog.setDownloadRequest(dlreq);
                        dialog.show();
                    }
                };
                cmd.setHighlighted(true);

                FocusWidget dlButton = new Button(cmd.getLabel());
                TablePanel.updateHighlighted(dlButton, cmd);
                GwtUtil.setStyle(dlButton, "margin", "4px");
                dlButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent ev) {
                        cmd.execute();
                    }
                });

                imageGrid.getViewer().addToolbarWidgetAtBeginning(dlButton);

                final Ref<Integer> dlCounter = new Ref<Integer>(0);
                BadgeButton pdfButton = GwtUtil.makeBadgeButton(
                        new Image(IconCreator.Creator.getInstance().getPdf()),
                        "Download PDF", true, new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                int counter = dlCounter.getSource();
                                Widget maskW = gridContainer;
                                Widget w = gridContainer;
                                int cX = w.getAbsoluteLeft() + w.getOffsetWidth() / 2;
                                int cY = w.getAbsoluteTop() + w.getOffsetHeight() / 2;
                                DownloadRequest dlreq = makeDownlaodRequest(sourceTable.getDataModel().getRequest(), dlTag, form);
                                dlreq.setParam("scope", "current"); // "current target" parameter for finder chart download.
                                dlreq.setBaseFileName(dlreq.getFilePrefix());
                                dlreq.setTitle(dlreq.getTitlePrefix() + "-pdf-" + counter++);
                                dlCounter.setSource(counter);
                                dlreq.setParam("file_type", "pdf");
                                PackageTask.preparePackage(maskW, cX, cY, dlreq);
                            }
                        });

                imageGrid.getViewer().addToolbarWidgetRightAtBeginning(pdfButton.getWidget());
            }
        }
    }

    private void processCatalog(ServerRequest tsReq) {
        boolean doOverlay = tsReq.getBooleanParam(FD_OVERLAY_CAT);
        if (doOverlay) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, catalogTab);
            String sources = tsReq.getParam(FD_SOURCES);
            if (sources.contains(Source.SDSS.name())) {
                addSDSSCatalog(FinderChartRequestUtil.ImageSet.SDSS, tsReq, Radius.sdss_radius.name(), "green");
            }
            if (sources.contains(Source.twomass.name())) {
                addCatalog(FinderChartRequestUtil.ImageSet.TWOMASS, tsReq, Radius.twomass_radius.name(), "red");
            }
            if (sources.contains(Source.WISE.name())) {
                addCatalog(FinderChartRequestUtil.ImageSet.WISE, tsReq, Radius.wise_radius.name(), "orange");
            }
            if (sources.contains(Source.IRIS.name())) {
                addCatalog(FinderChartRequestUtil.ImageSet.IRIS, tsReq, Radius.iras_radius.name(), "purple");
            }
        }
    }

    private void addSDSSCatalog(FinderChartRequestUtil.ImageSet imageSet, ServerRequest tsReq, String radiusFieldStr, String color) {
        double radiusArcSec = tsReq.getDoubleParam(radiusFieldStr);
        if (radiusArcSec == Double.NaN) {
            radiusArcSec = 50;
        }
        double radiusArcMin = radiusArcSec / 60.0;
        SDSSRequest req = new SDSSRequest();
        req.setRadiusArcmin(radiusArcMin);
        req.setMeta(MetaConst.CATALOG_HINTS, SUBGROUP_KEY + "=" + imageSet.subgroup);
        if (color!=null) req.setMeta(MetaConst.DEFAULT_COLOR,color);
        boolean one_to_one = tsReq.getBooleanParam(CatalogRequest.ONE_TO_ONE);
        req.setNearestOnly(one_to_one);

        String uploadFname = tsReq.getParam(FD_FILENAME);
        if (StringUtils.isEmpty(uploadFname)) {
            req.setUserTargetWorldPoint(tsReq.getParam(ReqConst.USER_TARGET_WORLD_PT));
            if (tsReq.getBooleanParam(FD_CAT_BY_BOUNDARY)) {
                radiusArcMin = tsReq.getDoubleParam(FD_SUBSIZE) * 60 / 2;
                req.setRadiusArcmin(radiusArcMin);
                req.setParam(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.BOX.getDesc());
            } else {
                req.setParam(CatalogRequest.SEARCH_METHOD, CatalogRequest.Method.CONE.getDesc());
            }
        } else {
            req.setFilename(uploadFname);
        }
        String radiusDesc = tsReq.getBooleanParam(FD_CAT_BY_BOUNDARY) ? "by image boundary" :  radiusArcSec + " arcsec";
        String title = imageSet.catalogTitle + ": " + FinderChartDescResolver.getSourceDesc(tsReq).replaceAll("Target= ", "") + "; " + radiusDesc;
        MonitorItem sourceMonItem = SearchAdmin.getInstance().submitSearch(req, title);

    }

    private void addCatalog(FinderChartRequestUtil.ImageSet imageSet, ServerRequest tsReq, String radiusFieldStr, String color) {

        double radiusArcSec = tsReq.getDoubleParam(radiusFieldStr);
        if (radiusArcSec == Double.NaN) {
            radiusArcSec = 50;
        }

        CatalogRequest gatorReq = new CatalogRequest(CatalogRequest.RequestType.GATOR_QUERY);
        gatorReq.setRadUnits(CatalogRequest.RadUnits.ARCSEC);
        gatorReq.setGatorHost("irsa");
        gatorReq.setDDOnList(true);
        gatorReq.setQueryCatName(imageSet.catalog);
        gatorReq.setRadius(radiusArcSec);
        gatorReq.setMeta(MetaConst.CATALOG_HINTS, SUBGROUP_KEY + "=" + imageSet.subgroup);
        if (color!=null) gatorReq.setMeta(MetaConst.DEFAULT_COLOR,color);
        if (tsReq.getBooleanParam(CatalogRequest.ONE_TO_ONE)) {
            gatorReq.setParam(CatalogRequest.ONE_TO_ONE, "1");
        }

        String uploadFname = tsReq.getParam(FD_FILENAME);
        if (StringUtils.isEmpty(uploadFname)) {
            gatorReq.setParam(ReqConst.USER_TARGET_WORLD_PT, tsReq.getParam(ReqConst.USER_TARGET_WORLD_PT));
            if (tsReq.getBooleanParam(FD_CAT_BY_BOUNDARY)) {
                gatorReq.setMethod(CatalogRequest.Method.BOX);
                double size = tsReq.getDoubleParam(FD_SUBSIZE) * 60 * 60;
                gatorReq.setSide(size);
            } else {
                gatorReq.setMethod(CatalogRequest.Method.CONE);
            }
        } else {
            gatorReq.setMethod(CatalogRequest.Method.TABLE);
            gatorReq.setFileName(uploadFname);
        }

        String radiusDesc = tsReq.getBooleanParam(FD_CAT_BY_BOUNDARY) ? "image boundary" :  radiusArcSec + " arcsec";
        String title = imageSet.catalogTitle + ": " + FinderChartDescResolver.getSourceDesc(tsReq).replaceAll("Target= ", "") + "; " + radiusDesc;

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
        if (sourceTable != null && isMultiPosition) {
            GwtUtil.SplitPanel.showWidget(layoutPanel, sourceContainer);
        } else {
            GwtUtil.SplitPanel.hideWidget(layoutPanel, sourceContainer);
        }
    }

    private DownloadRequest makeDownlaodRequest(TableServerRequest srcRequest, DownloadTag dlTag, Form form) {
        TableServerRequest searchReq = new TableServerRequest("FinderChartQuery", srcRequest);
        DownloadRequest dlreq = new DownloadRequest(searchReq, "Finder Chart Download Options", "FinderChartFiles");
        dlreq.setRequestId("FinderChartDownload");

        List<ParamTag> dlParams = dlTag.getParams();
        List<SearchFormParamTag> sfParams = dlTag.getSearchFormParams();
        for (SearchFormParamTag sfpt : sfParams) {
            DynUtils.evaluateSearchFormParam(form, sfpt, dlParams);
        }
        if (dlParams != null) {
            for (ParamTag p : dlParams) {
                dlreq.setParam(p.getKey(), p.getValue());
            }
        }

        // -------------------------------
        // record plot states and layers info
        List<PlotWidgetGroup> groups = AllPlots.getInstance().searchGroups(new CollectionUtil.SimpleFilter<PlotWidgetGroup>() {
                        public boolean accept(PlotWidgetGroup obj) {
                            return obj.getName().startsWith(FINDERCHART_GROUP_NAME);
                        }
                    });
        // there should only be 1.  if there's more than 1, the logic may need to change.
        if (groups.size() == 1) {
            Set<TableData.Row> srows = sourceTable.getTable().getSelectedRowValues();
            if (srows.size() > 0) {
                TableData.Row selRow = srows.iterator().next();
                List<String> cfilters = new ArrayList<String>();
                cfilters.add("ra = " + selRow.getValue("ra"));
                cfilters.add("dec = " + selRow.getValue("dec"));
                searchReq.setFilters(cfilters);
            }

            PlotWidgetGroup group = groups.get(0);
            String stateStr = "";
            String drawInfoListStr = "";
            ArrayList<String> queryItems = new ArrayList<String>(Arrays.asList("PlotStates,DrawInfoList,LayerInfo".split(",")));
            WebPlot currentPlot;
            Map<String, String> layerMap = new LinkedHashMap<String, String>();
            for (MiniPlotWidget mpw : group.getAllActive()) {
                if (mpw.getTitle().length() == 0) break;
                currentPlot = mpw.getCurrentPlot();
                if (queryItems.contains("PlotStates")) {
                    stateStr += getPlotStates(currentPlot);
                    stateStr += "&&";
                }
                if (queryItems.contains("DrawInfoList")) {
                    drawInfoListStr += getDrawInfoListString(currentPlot);
                    drawInfoListStr += "&&";
                }
                collectLayerInfo(layerMap, currentPlot);
            }
            if (queryItems.contains("PlotStates")) {
                dlreq.setParam("PlotStates", stateStr);
            }
            if (queryItems.contains("DrawInfoList")) {
                dlreq.setParam("DrawInfoList", drawInfoListStr);
            }
            if (queryItems.contains("LayerInfo") && layerMap.size() > 0) {
                String layerInfo = serializeLayerInfo(layerMap);
                dlreq.setParam("LayerInfo", layerInfo);
            }
        }
        // -------------------------------

        return dlreq;
    }

    private String getPlotStates(WebPlot plot) {
        String retval = "";

        PlotState state;
        if (plot != null) {
            state = plot.getPlotState();
            if (state != null) {
                retval = state.serialize();
            }
        } else {
            retval = Constants.SPLIT_TOKEN;
        }
        return retval;
    }

    private String getDrawInfoListString(WebPlot plot) {
        StringBuilder sb = new StringBuilder(350);

        if (plot != null) {
            for (StaticDrawInfo info : PrintableUtil.getDrawInfoList(plot)) {
//                if (info.getDrawType().equals(StaticDrawInfo.DrawType.SYMBOL) && !info.getLabel().contains("CatalogID")) {
//                    info.setList(new ArrayList<WorldPt>(0));
//                }
                sb.append(info.serialize()).append(Constants.SPLIT_TOKEN);
            }
        } else {
            sb.append(Constants.SPLIT_TOKEN);
        }
        return sb.toString();
    }

    private void collectLayerInfo(Map<String, String> layerMap, WebPlot plot) {
        String title, defaultColor;
        if (plot != null) {
            for (WebLayerItem layer : plot.getPlotView().getUserDrawerLayerSet()) {
                if (layer.getDrawer().isVisible()) {
                    defaultColor = layer.getColor();
                    title = layer.getTitle();
                    if (!layerMap.keySet().contains(title)) {
                        layerMap.put(title, defaultColor);
                    }
                }
            }
        }
    }

    private String serializeLayerInfo(Map<String, String> layerMap) {
        StringBuilder sb = new StringBuilder(350);
        if (layerMap != null && layerMap.size() > 0) {
            for (String key : layerMap.keySet()) {
                sb.append(key);
                sb.append("==");
                sb.append(layerMap.get(key));
                sb.append(Constants.SPLIT_TOKEN);
            }
        }
        return sb.toString();
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
