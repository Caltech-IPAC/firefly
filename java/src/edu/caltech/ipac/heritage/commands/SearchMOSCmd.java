package edu.caltech.ipac.heritage.commands;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.commands.DownloadCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.MOSRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.Form;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MOSPanel;
import edu.caltech.ipac.firefly.ui.NaifTargetPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.ImageGridViewCreator;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.DatasetQueryCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.DrawingLayerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.SelectableTablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;
import edu.caltech.ipac.heritage.ui.DownloadSelectionDialog;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tatianag
 */
public class SearchMOSCmd extends HeritageRequestCmd {

    public static final String COMMAND_NAME = "MOSQuery";
    MOSPanel mosPanel;

    public SearchMOSCmd() {
        super(COMMAND_NAME);
    }

    @Override
    protected Form createForm() {
        mosPanel = new MOSPanel();
        Form form = new Form();
        form.setHelpId("searching.byPosition");
        form.add(mosPanel);
        form.setFocus(NaifTargetPanel.NAIF_NAME_KEY);
        return form;

    }

    @Override
    protected void processRequest(Request req, AsyncCallback<String> callback) {
        createTablePreviewDisplay(true);
        EventHub hub= getResultsPanel().getPreview().getEventHub();

        req.setRequestId("MOSQuery");
        req.setParam(MOSRequest.CATALOG, "spitzer_bcd");
        BaseTableConfig config = new BaseTableConfig<TableServerRequest>(req, "Precovery", "Precovery search results", "heritageDownload", "precovery-", "Precovery: ");

        TablePanel tablePanel = addTable(config);

        // add event workers

        /* from WISE
        <EventWorker id="movingTrackMOS" type="DataSetVisQuery">
        <QueryId>wiseMOSQuery_1b</QueryId>
        <Param key="searchProcessorId" value="WiseMOSQuery"/>
        <Param key="ExtraParams" value="queryId=wiseMOSQuery_1b"/>
        <Param key="Events" value="SearchResultEnd"/>
        <Param key="UniqueKeyColumns" value="frame_num,scan_id"/>

        <Param key="Type" value="MatchedPoint"/>
        <Param key="COLOR" value="orange"/>
        <Param key="SYMBOL" value="X"/>
        <!--<Param key="MatchColor" value="red"/>-->
        <Param key="Title" value="Observed Images"/>
        <Param key="Selection" value="True"/>
        </EventWorker>
         */
        Map<String,String> params=
                StringUtils.createStringMap(
                        EventWorker.QUERY_SOURCE, "shaGridMOSQuery",
                        EventWorker.ID, "movingTrackMOS",
                        CommonParams.SEARCH_PROCESSOR_ID, "MOSQuery",
                        CommonParams.ENABLE_DEFAULT_COLUMNS, "True",
                        CommonParams.UNIQUE_KEY_COLUMNS, "bcdid",
                        "Events", "SearchResultEnd",
                        CommonParams.TYPE,  "MatchedPoint",
                        CommonParams.COLOR, "orange",
                        CommonParams.SYMBOL, "X",
                        CommonParams.TITLE, "Observed Images",
                        "Selection", "True");

        final WidgetFactory factory= Application.getInstance().getWidgetFactory();
        EventWorker ew = factory.createEventWorker(DrawingLayerCreator.DATASET_VIS_QUERY, params);
        hub.bind(ew);
        ew.bind(hub);

        /*
        <EventWorker id="movingTrackMOSPath" type="DataSetVisQuery">
        <QueryId>wiseMOSQuery_1b</QueryId>
        <Param key="searchProcessorId" value="WiseMOSQuery"/>
        <Param key="ExtraParams" value="table_name=orbital_path_table,queryId=wiseMOSQuery_1b"/>
        <Param key="Events" value="SearchResultEnd"/>

        <Param key="Type" value="Track"/>
        <Param key="COLOR" value="blue"/>
        <Param key="SYMBOL" value="DOT"/>
        <Param key="Title" value="Moving Target Track"/>
        <Param key="Selection" value="False"/>
        <Param key="Decimation" value="3"/>
        </EventWorker>
        */
        params= StringUtils.createStringMap(
                EventWorker.QUERY_SOURCE, "shaGridMOSQuery",
                EventWorker.ID, "movingTrackMOSPath",
                CommonParams.SEARCH_PROCESSOR_ID, "MOSQuery",
                "table_name", "orbital_path_table",
                "Events", "SearchResultEnd",
                CommonParams.TYPE,  "Track",
                CommonParams.COLOR, "blue",
                CommonParams.SYMBOL, "DOT",
                CommonParams.TITLE, "Moving Target Track",
                "Selection", "False",
                "Decimation", "3");
        ew = factory.createEventWorker(DrawingLayerCreator.DATASET_VIS_QUERY, params);
        hub.bind(ew);
        ew.bind(hub);

        /*
        <EventWorker id="orbitalElementsMOS" type="DataSetQuery">
        <QueryId>wiseMOSQuery_1b</QueryId>
        <Param key="searchProcessorId" value="WiseMOSQuery"/>
        <Param key="ExtraParams" value="header_only=true,queryId=wiseMOSQuery_1b"/>
        <Param key="Events" value="SearchResultEnd"/>
        </EventWorker>
        */
        params= StringUtils.createStringMap(
                EventWorker.QUERY_SOURCE, "MOSQuery",
                EventWorker.ID, "orbitalElementsMOS",
                CommonParams.SEARCH_PROCESSOR_ID, "MOSQuery",
                CommonParams.EXTRA_PARAMS, "header_only=true",
                "Events", "SearchResultEnd");
        ew = factory.createEventWorker(DatasetQueryCreator.DATASET_QUERY, params);
        hub.bind(ew);
        ew.bind(hub);

        GWT.runAsync(new GwtUtil.DefAsync() {
            public void onSuccess() {

                // Add Orbital Path coverage preview
                final Map<String,String> obc_params = StringUtils.createStringMap(
                        "EVENT_WORKER_ID", "movingTrackMOSPath,movingTrackMOS,orbitalElementsMOS",
                        CommonParams.TITLE, "Orbital Path Coverage");

                TablePreview obc_preview = factory.createObserverUI(WidgetFactory.DATA_SOURCE_COVERAGE_VIEW, obc_params);
                getResultsPanel().getPreview().addView(obc_preview);
            }
        });

        //add image grid view
        List<Param> paramList = req.getParams();
        params = new HashMap<String, String>();
        for (Param p : paramList) {
            if (!p.getName().equals(ServerRequest.ID_KEY)) {
                params.put(p.getName(), p.getValue());
            }
        }
        params.put(ServerRequest.ID_KEY, "shaGridMOSQuery");
        params.put("Index", "-1"); // first view to display
        params.put(CommonParams.SEARCH_PROCESSOR_ID, "shaGridMOSQuery");
        params.put(CommonParams.PAGE_SIZE, "15");
        params.put(CommonParams.LOCK_RELATED, "true");
        params.put(CommonParams.PLOT_EVENT_WORKERS, "movingTrackMOS");

        TablePanel.View imageGridView = new ImageGridViewCreator().create(params);
        tablePanel.addView(imageGridView);
        imageGridView.bind(hub);

        loadAll();
        setResults(getResultsPanel());
        //NewTableResults tr = new NewTableResults(req, WidgetFactory.TABLE, "Precovery search");
        //WebEventManager.getAppEvManager().fireEvent(new WebEvent<NewTableResults>(this, Name.NEW_TABLE_RETRIEVED, tr));


    }

    @Override
    public void onLoaded(PrimaryTableUI tableUI) {
        super.onLoaded(tableUI);
        if (tableUI == null) return;

        if (tableUI.getDisplay() instanceof SelectableTablePanel) {
            SelectableTablePanel table = (SelectableTablePanel) tableUI.getDisplay();
            if (table.getDataset() != null && table.getDataset().getTotalRows() > 0) {
                TableConfig tconf = getConfig(table);
                 if (tconf instanceof BaseTableConfig) {
                    DownloadSelectionDialog dsd =
                        new DownloadSelectionDialog(DownloadSelectionDialog.DialogType.BCD,
                                new DownloadRequest(tconf.getSearchRequest(), "Precovery: ", "precovery-"),
                                table.getDataset());
                    ((BaseTableConfig)tconf).setDownloadSelectionIF(dsd);
                    DownloadCmd cmd = tconf.getDownloadCmd();
                    if (cmd != null) {
                        table.addToolButton(cmd, false);
                    }
                }
            }
        }
    }

}
