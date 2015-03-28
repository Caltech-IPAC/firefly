/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 4:24 PM
 */


import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.TitleFlasher;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotCmdExtension;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.draw.ActionReporter;
import edu.caltech.ipac.firefly.visualize.ui.DS9RegionLoadDialog;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class PushReceiver {

    private static final String IMAGE_CMD_PLOT_ID= "ImagePushPlotID";
//    private final List<String> consumedItems= new ArrayList<String>(15);
    private int consumedCnt= 0;
    private final StandaloneUI aloneUI;
    private static int idCnt= 0;
    public static final String TABLE_SEARCH_PROC_ID = "IpacTableFromSource";

    public PushReceiver(final MonitorItem monItem, StandaloneUI aloneUI) {
        this.aloneUI= aloneUI;

        monItem.addUpdateListener(new MonitorItem.UpdateListener() {
            @Override
            public void update(MonitorItem item) {
                consume(monItem);
            }
        });

    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    protected void consume(MonitorItem monItem) {
        BackgroundStatus bgStat= monItem.getStatus();
        for(PushItem item= getNextItem(bgStat); (item!=null); item= getNextItem(bgStat) ) {
            TitleFlasher.flashTitle("!! New Image !!");
            String id=IMAGE_CMD_PLOT_ID+idCnt;
            idCnt++;
            CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);
//            consumedItems.add(wpr.toString());
            ActionReporter actionReporter= new ActionReporter();
            actionReporter.setMonitorItem(monItem);
            String fileName;
            switch (item.pushType) {
                case WEB_PLOT_REQUEST:
                    WebPlotRequest wpr= WebPlotRequest.parse(item.data);
                    prepareRequest(id, wpr);
                    break;
                case REGION_FILE_NAME:
                    fileName= item.data;
                    DS9RegionLoadDialog.loadRegFile(fileName,null,monItem);
                    break;
                case TABLE_FILE_NAME:
                    fileName= item.data;
                    loadTable(fileName,monItem);
                    //
                    break;
                case FITS_COMMAND_EXT:
                    PlotCmdExtension ext= parsePlotCmdExtension(item.data);
                    for(MiniPlotWidget mpw : AllPlots.getInstance().getAll()) {
                        if (mpw.getPlotView()!=null) {
                            List<PlotCmdExtension> list= (List)mpw.getPlotView().getAttribute(WebPlotView.EXTENSION_LIST);
                            if (list==null) {
                                list= new ArrayList<PlotCmdExtension>(5);
                                mpw.getPlotView().setAttribute(WebPlotView.EXTENSION_LIST, list);
                            }
                            list.add(ext);
                            mpw.recomputeUserOptions(actionReporter);
                        }
                    }

                    break;
            }
        }
    }


    private static PlotCmdExtension parsePlotCmdExtension(String in) {
        ServerRequest req= ServerRequest.parse(in,new ServerRequest());
        return new PlotCmdExtension(req.getRequestId(),
                                    StringUtils.getEnum(req.getParam(ServerParams.EXT_TYPE), PlotCmdExtension.ExtType.NONE),
                                    req.getParam(ServerParams.IMAGE),
                                    req.getParam(ServerParams.TITLE),
                                    req.getParam(ServerParams.TOOL_TIP) );
    }

    private PushItem getNextItem(BackgroundStatus bgStat ) {
        String inStr= null;
        int statusCnt= bgStat.getNumPushData();

        for(;(consumedCnt<statusCnt && inStr==null); consumedCnt++) {
            inStr= bgStat.getPushData(consumedCnt);
        }

        PushItem retval= null;
        if (inStr!=null) {
            consumedCnt--;
            retval= new PushItem(inStr, bgStat.getPushType(consumedCnt));
            clearEntry(bgStat.getID(), consumedCnt);
            consumedCnt++;
        }
        return retval;
    }


    private void prepareRequest(String id, final ServerRequest req) {

        deferredPlot(id,req);
    }

    private void deferredPlot(String id, ServerRequest req) {
        WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);

        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) && req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }

        aloneUI.getMultiViewer().forceExpand();
        PlotData dynData= ConverterStore.get(ConverterStore.DYNAMIC).getPlotData();
        dynData.setID(id,wpReq);

    }


    private void clearEntry(final String id, final int idx) {
        Timer t= new Timer() {
            @Override
            public void run() {
                SearchServices.App.getInstance().clearPushEntry(id,idx, new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) { }
                    @Override
                    public void onSuccess(Boolean result) {  }
                });
            }
        };
        t.schedule(120000);
    }


    protected void loadTable(final String fileName, MonitorItem monItem) {

        final TableServerRequest req = new TableServerRequest(TABLE_SEARCH_PROC_ID);
        req.setStartIndex(0);
        req.setPageSize(100);
        req.setParam("source",fileName);
        String title= findTitle(req);
        FFToolEnv.getHub().getCatalogDisplay().addMonitorItemForTable(title,monItem);
        SearchAdmin.getInstance().submitSearch(req, title);
    }

    private static String findTitle(TableServerRequest req) {
        String title= "Loaded Table";
        if (req.containsParam(ServerParams.TITLE)) {
            title= req.getParam(ServerParams.TITLE);
        }
        else if (req.containsParam(ServerParams.SOURCE)) { // find another way to make a title
            req.setParam(ServerParams.SOURCE, FFToolEnv.modifyURLToFull(req.getParam(ServerParams.SOURCE)));
            String url = req.getParam(ServerParams.SOURCE);
            int idx = url.lastIndexOf('/');
            if (idx<0) idx = url.lastIndexOf('\\');
            if (idx > 1) {
                title = url.substring(idx+1);
            } else {
                title = url;
            }
        }
        return title;

    }

    private static final class PushItem {
        String data;
        BackgroundStatus.PushType pushType;

        public PushItem(String data, BackgroundStatus.PushType pushType) {
            this.data = data;
            this.pushType = pushType;
        }
    }
}
