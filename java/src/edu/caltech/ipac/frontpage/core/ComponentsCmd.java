package edu.caltech.ipac.frontpage.core;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.fftools.core.StandaloneUI;
import edu.caltech.ipac.fftools.core.TabPlotWidgetFactory;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.TitleFlasher;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.task.VisTask;

import java.util.List;

public class ComponentsCmd extends RequestCmd {

    public  static final String COMMAND = "FFToolsImageCmd";
    private final StandaloneUI aloneUI;
    private final TabPlotWidgetFactory factory;

    public ComponentsCmd(TabPlotWidgetFactory factory, StandaloneUI aloneUI) {
        super(COMMAND, "Fits Viewer", "Fits Viewer", true);
        this.factory= factory;
        this.aloneUI= aloneUI;
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {

        TitleFlasher.flashTitle("!! New Image !!");

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

            final MiniPlotWidget mpw= factory.create();

            mpw.getOps(new MiniPlotWidget.OpsAsync() {
                public void ops(final PlotWidgetOps ops) {
                    prepareRequest(mpw, ops, wpr != null ? wpr : req);
                }
            });

            checkAndLoadMulti(req);
        }
        else {
            if (req.containsParam(WebPlotRequest.MULTI_PLOT_KEY)) {
                final MiniPlotWidget mpw= factory.create();
                mpw.getOps(new MiniPlotWidget.OpsAsync() {
                    public void ops(final PlotWidgetOps ops) {
                        String key= req.getParam(WebPlotRequest.MULTI_PLOT_KEY);
                        handle3ColorRequest(mpw, ops, key);
                    }
                });
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
                        ComponentsCmd.this.doExecute(request,null);
                    }
                }
                public void onFailure(Throwable caught) { /*do nothing*/ }
            });
        }
    }

    private void handle3ColorRequest(final MiniPlotWidget mpw, final PlotWidgetOps ops, String keyBase) {
        final ThreeRetrieved threeRetrieved= new ThreeRetrieved();
        Band[] bandAry= {Band.RED,Band.GREEN,Band.BLUE};

        for(Band b: bandAry) {
            final Band band= b;
            VisTask.getInstance().getAllSavedRequest(keyBase+band, new AsyncCallback<List<WebPlotRequest>>() {
                public void onSuccess(List<WebPlotRequest> reqList) {
                    WebPlotRequest wpr= reqList.get(0);
                    threeRetrieved.markDone(band,wpr);
                    if (threeRetrieved.isAllDone()) {
                        prepare3ColorRequest(mpw,ops, threeRetrieved.red, threeRetrieved.green, threeRetrieved.blue);
                    }

                }
                public void onFailure(Throwable caught) { /*do nothing*/ }
            });
        }
    }

    private void prepareRequest(final MiniPlotWidget mpw, final PlotWidgetOps ops, final ServerRequest req) {

        boolean empty= !ops.isPlotShowing();

        if (empty) {
            if (req.containsParam(CommonParams.DO_PLOT) && req instanceof WebPlotRequest) {
                Timer t = new Timer() { // layout is slow sometime so delay a little (this is a hack)
                    @Override
                    public void run() {
                        factory.prepare(mpw, new Vis.InitComplete() {
                                public void done() { deferredPlot(mpw, req);  } });
                    }
                };
                t.schedule(100);
            }
            else {
                factory.removeCurrentTab();
//                aloneUI.eventEmptyAppQueryImage();
                ops.showImageSelectDialog();
            }
        }
        else {
            ops.showImageSelectDialog();
        }
    }

    private void deferredPlot(final MiniPlotWidget mpw, ServerRequest req) {
        WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);

        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) && req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }

        final WebPlotRequest wpReqFinal = factory.customizeRequest(mpw,wpReq);

        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(final PlotWidgetOps widgetOps) {
                widgetOps.plotExpanded(wpReqFinal, true, null);
            }
        });

    }



    private void prepare3ColorRequest(final MiniPlotWidget mpw,
                                      final PlotWidgetOps ops,
                                      final WebPlotRequest red,
                                      final WebPlotRequest green,
                                      final WebPlotRequest blue) {

        Timer t = new Timer() { // layout is slow sometime so delay a little (this is a hack)
            @Override
            public void run() {
                factory.prepare(mpw, new Vis.InitComplete() {
                    public void done() { deferredPlot3Color(mpw, red, green, blue);  } });
            }
        };
        t.schedule(100);
    }

    private void deferredPlot3Color(final MiniPlotWidget mpw,
                                    WebPlotRequest red,
                                    WebPlotRequest green,
                                    WebPlotRequest blue) {

        final WebPlotRequest redFinal = factory.customizeRequest(mpw,red);
        final WebPlotRequest greenFinal = factory.customizeRequest(mpw,green);
        final WebPlotRequest blueFinal = factory.customizeRequest(mpw,blue);

        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(final PlotWidgetOps widgetOps) {
                widgetOps.plot3Expanded(redFinal,greenFinal,blueFinal, true, null);
            }
        });

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

