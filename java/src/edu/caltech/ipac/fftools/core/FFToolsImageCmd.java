package edu.caltech.ipac.fftools.core;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

public class FFToolsImageCmd extends RequestCmd {

    public  static final String COMMAND = "FFToolsImageCmd";
    private final StandaloneUI aloneUI;
    private final TabPlotWidgetFactory factory;

    public FFToolsImageCmd(TabPlotWidgetFactory factory, StandaloneUI aloneUI) {
        super(COMMAND, "Fits Viewer", "Fits Viewer", true);
        this.factory= factory;
        this.aloneUI= aloneUI;
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {


        WebPlotRequest workReq= null;
        CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);

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
                aloneUI.eventEmptyAppQueryImage();
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


}

