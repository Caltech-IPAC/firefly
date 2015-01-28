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
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.ui.TitleFlasher;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ui.DS9RegionLoadDialog;

import java.util.Arrays;

/**
 * @author Trey Roby
 */
public class PushReceiver {

    private static final String IMAGE_CMD_PLOT_ID= "ImagePushPlotID";
//    private final List<String> consumedItems= new ArrayList<String>(15);
    private int consumedCnt= 0;
    private final StandaloneUI aloneUI;
    private static int idCnt= 0;

    public PushReceiver(final MonitorItem monItem, StandaloneUI aloneUI) {
        this.aloneUI= aloneUI;

        monItem.addUpdateListener(new MonitorItem.UpdateListener() {
            @Override
            public void update(MonitorItem item) {
                consume(monItem.getStatus());
            }
        });

    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    protected void consume(BackgroundStatus bgStat) {
        for(PushItem item= getNextItem(bgStat); (item!=null); item= getNextItem(bgStat) ) {
            TitleFlasher.flashTitle("!! New Image !!");
            String id=IMAGE_CMD_PLOT_ID+idCnt;
            idCnt++;
            CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);
//            consumedItems.add(wpr.toString());
            switch (item.pushType) {
                case WEB_PLOT_REQUEST:
                    WebPlotRequest wpr= WebPlotRequest.parse(item.data);
                    prepareRequest(id, wpr);
                    break;
                case REGION_FILE_NAME:
                    String fileName= item.data;
                    DS9RegionLoadDialog.loadRegFile(fileName,null);
                    break;
            }
        }
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

//
//
//        WebPlotRequest wpr= bgStat.getWebPlotRequest();
//        if (consumedItems.contains(wpr.toString())) {
//            wpr= null;
//        }
//        return wpr;
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



    private void prepare3ColorRequest(final String id,
                                      final WebPlotRequest red,
                                      final WebPlotRequest green,
                                      final WebPlotRequest blue) {

        Timer t = new Timer() { // layout is slow sometime so delay a little (this is a hack)
            @Override
            public void run() {
                deferredPlot3Color(id, red, green, blue);
            }
        };
        t.schedule(100);
    }

    private void deferredPlot3Color(String id,
                                    WebPlotRequest red,
                                    WebPlotRequest green,
                                    WebPlotRequest blue) {

        aloneUI.getMultiViewer().forceExpand();
        PlotData dynData= ConverterStore.get(ConverterStore.DYNAMIC).getPlotData();
        dynData.set3ColorIDRequest(id, Arrays.asList(red, green, blue));
    }


    private static class ThreeRetrieved {
        boolean redDone= false;
        boolean greenDone= false;
        boolean blueDone= false;

        WebPlotRequest red= null;
        WebPlotRequest green= null;
        WebPlotRequest blue= null;

        public void markDone(Band band, WebPlotRequest r) {
            switch (band) {
                case RED:
                    redDone= true;
                    red= r;
                    break;
                case GREEN:
                    greenDone= true;
                    green= r;
                    break;
                case BLUE:
                    blueDone= true;
                    blue= r;
                    break;
                case NO_BAND:
                    break;
            }
        }

        public boolean isAllDone() {
            return redDone && blueDone && greenDone;
        }
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


    private static final class PushItem {
        String data;
        BackgroundStatus.PushType pushType;

        public PushItem(String data, BackgroundStatus.PushType pushType) {
            this.data = data;
            this.pushType = pushType;
        }
    }
}
