/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.fftools;
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
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.TitleFlasher;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.Ext;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ui.DS9RegionLoadDialog;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class PushReceiver {

    public enum ExtType { AREA_SELECT, LINE_SELECT, POINT, NONE }
    private static final String IMAGE_CMD_PLOT_ID= "ImagePushPlotID";
    private int consumedCnt= 0;
    private static int idCnt= 0;
    public static final String TABLE_SEARCH_PROC_ID = "IpacTableFromSource";
    public final ExternalPlotController plotController;

    public PushReceiver(final MonitorItem monItem, ExternalPlotController plotController) {
        this.plotController= plotController;
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
            CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);
//            AllPlots.getInstance().getActionReporter().setMonitorItem(monItem);
            String fileName;
            switch (item.pushType) {
                case WEB_PLOT_REQUEST:
                    WebPlotRequest wpr= WebPlotRequest.parse(item.data);
                    String id;
                    if (wpr.getPlotId()!=null) {
                        id= wpr.getPlotId();
                    }
                    else {
                        id=IMAGE_CMD_PLOT_ID+idCnt;
                        idCnt++;
                        wpr.setPlotId(id);
                    }
                    prepareRequest(wpr);
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
                    Ext.Extension ext= parsePlotCmdExtension(item.data);
                    Ext.ExtensionInterface exI= Ext.makeExtensionInterface();
                    exI.fireExtAdd(ext);
                    break;
            }
        }
    }


    private static Ext.Extension parsePlotCmdExtension(String in) {
        ServerRequest req= ServerRequest.parse(in,new ServerRequest());

        return Ext.makeExtension(req.getRequestId(),
                                 req.getParam(ServerParams.PLOT_ID),
                                 StringUtils.getEnum(req.getParam(ServerParams.EXT_TYPE), ExtType.NONE).toString(),
                                 req.getParam(ServerParams.IMAGE),
                                 req.getParam(ServerParams.TITLE),
                                 req.getParam(ServerParams.TOOL_TIP));
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


    private void prepareRequest(ServerRequest req) { deferredPlot(req); }

    private void deferredPlot(ServerRequest req) {
        WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);

        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) && req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }

        plotController.update(wpReq);
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
        req.setParam("source", fileName);
        String title= findTitle(req);
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

    public interface ExternalPlotController {
        void update(WebPlotRequest wpr);
    }




}
