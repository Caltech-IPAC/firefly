package edu.caltech.ipac.fftools.core;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
import edu.caltech.ipac.firefly.ui.TitleFlasher;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.task.VisTask;

import java.util.Arrays;
import java.util.List;

public class FFToolsImageCmd extends RequestCmd {

    public  static final String COMMAND = "FFToolsImageCmd";
    private final StandaloneUI aloneUI;
    private static final String IMAGE_CMD_PLOT_ID= "ImageCmdPlotID";
    private static int idCnt= 0;

    public FFToolsImageCmd(StandaloneUI aloneUI) {
        super(COMMAND, "Fits Viewer", "Fits Viewer", true);
        this.aloneUI= aloneUI;
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {

        TitleFlasher.flashTitle("!! New Image !!");

        String id=IMAGE_CMD_PLOT_ID+idCnt;
        idCnt++;
        WebPlotRequest workReq= null;
        CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);

        if (!req.containsParam(WebPlotRequest.THREE_COLOR_HINT)) {
            if (req.containsParam(CommonParams.DO_PLOT)) {
                workReq= new WebPlotRequest();
                for(Param p : req.getParams()){
                    if (!p.getName().equals(Request.ID_KEY)) {
                        workReq.setParam(p);
                    }
                }
            }

            final WebPlotRequest wpr= workReq;


            prepareRequest(id, wpr != null ? wpr : req);

            checkAndLoadMulti(req);
        }
        else {
            if (req.containsParam(WebPlotRequest.MULTI_PLOT_KEY)) {
                String key= req.getParam(WebPlotRequest.MULTI_PLOT_KEY);
                handle3ColorRequest(id, key);
            }
        }
    }


    private void checkAndLoadMulti(Request req) {
        if (req.containsParam(WebPlotRequest.MULTI_PLOT_KEY)) {
            String key= req.getParam(WebPlotRequest.MULTI_PLOT_KEY);
            VisTask.getInstance().getAllSavedRequest(key, new AsyncCallback<List<WebPlotRequest>>() {
                public void onSuccess(List<WebPlotRequest> reqList) {
                    for(WebPlotRequest wpr : reqList) {
                        Request request= new Request();
                        request.copyFrom(wpr);
                        request.setParam(CommonParams.DO_PLOT,"true");
                        FFToolsImageCmd.this.doExecute(request,null);
                    }
                }
                public void onFailure(Throwable caught) { /*do nothing*/ }
            });
        }
    }

    private void handle3ColorRequest(final String id, String keyBase) {
        final ThreeRetrieved threeRetrieved= new ThreeRetrieved();
        Band[] bandAry= {Band.RED,Band.GREEN,Band.BLUE};

        for(Band b: bandAry) {
            final Band band= b;
            VisTask.getInstance().getAllSavedRequest(keyBase+band, new AsyncCallback<List<WebPlotRequest>>() {
                public void onSuccess(List<WebPlotRequest> reqList) {
                    WebPlotRequest wpr= reqList.get(0);
                    threeRetrieved.markDone(band,wpr);
                    if (threeRetrieved.isAllDone()) {
                        prepare3ColorRequest(id,threeRetrieved.red, threeRetrieved.green, threeRetrieved.blue);
                    }

                }
                public void onFailure(Throwable caught) { /*do nothing*/ }
            });
        }
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


}

