package edu.caltech.ipac.firefly.commands;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.ZoomUtil;
import edu.caltech.ipac.util.StringUtils;

public class FitsInputCmd extends RequestCmd {

    public  static final String COMMAND = "FitsInput";
    public  static final String FITS_BUTTON = "FitsInput";
    public  static final String SEARCH_BUTTON = "Search";

    private static final int CONTROLS_HEIGHT=  40;
    private static final int RESIZE_DELAY= 500;
    private MyDockLayoutPanel _main= new MyDockLayoutPanel();
    private MiniPlotWidget _mpw = null;
    private DialogListener _listener= null;
    private boolean _standAlone = false;
    private final EventHub _hub = new EventHub();
    private boolean added= false;

    public FitsInputCmd(String title, String desc, boolean enabled) {
        super(COMMAND, title, desc, enabled);
    }




    public boolean init() {
        _mpw = new MiniPlotWidget();
        _mpw.addStyleName("standard-border");
        _main.add(_mpw);
        _main.setSize("100%", "100%");
        _main.addStyleName("fits-input-cmd-main-widget");
        _mpw.setRemoveOldPlot(true);
        _mpw.setPopoutButtonVisible(false);
        _mpw.setLockToolbarVisible(true);
        _mpw.setMinSize(50, 50);
        _mpw.setTitleAreaAlwaysHidden(true);
        _mpw.setAutoTearDown(false);
        _mpw.setSaveImageCornersAfterPlot(true);
        _mpw.setImageSelection(true);
        CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);
        return true;
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {

        if (!_standAlone)  {
            _standAlone = req.containsParam(CommonParams.VIS_ONLY);
        }


        WebPlotRequest workReq= null;
        if (_standAlone && !added) {
            TabPane<Widget> tabs= new TabPane<Widget>();
            TabPane.Tab<Widget> tab= new TabPane.Tab<Widget>(tabs,_main,false,"FITS Image", "FITS Image");
            tabs.addTab(tab);
            tab.setSize("100%", "100%");
            new NewTableEventHandler(_hub,tabs);
            checkSmallIcon(req);
            Toolbar.Button b= Application.getInstance().getToolBar().getButton(FITS_BUTTON);
            Toolbar toolbar= Application.getInstance().getToolBar();
            if (b!=null) b.setUseDropdown(false);
            toolbar.removeButton(SEARCH_BUTTON);

            Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).setDisplay(tabs);


            if (req.containsParam(WebPlotRequest.TITLE)) {
                registerView(LayoutManager.SEARCH_TITLE_REGION, _mpw.getTitleWidget());
            }

            if (req.containsParam(ServerParams.REQUEST)) {
               workReq= WebPlotRequest.parse(req.getParam(ServerParams.REQUEST));
            }

            Window.addResizeHandler(new ResizeHandler() {
                public void onResize(ResizeEvent event) {
                    _main.onResize();
                }
            });


        }
        else if (!_standAlone) {
            Application.getInstance().getToolBar().setTitle(_mpw.getTitleWidget());
            Application.getInstance().getToolBar().setHeaderButtons(_mpw.getToolPanel());
            Toolbar toolbar= Application.getInstance().getToolBar();
            toolbar.setContent(_main, false,this,null);
        }

        callback.onSuccess("success!");

        final WebPlotRequest wpr= workReq;

        _mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(final PlotWidgetOps ops) {
                analyze(ops, wpr!=null ? wpr : req);
            }
        });
    }


    private void checkSmallIcon(Request req) {
        HtmlRegionLoader f = new HtmlRegionLoader();
        String smIconRegion = req.getParam(LayoutManager.SMALL_ICON_REGION);
        if (!StringUtils.isEmpty(smIconRegion)) {
            f.load(smIconRegion, LayoutManager.SMALL_ICON_REGION);
        } else {
            f.unload(LayoutManager.SMALL_ICON_REGION);
        }
    }

    private void analyze(final PlotWidgetOps ops, final ServerRequest req) {
        if (_listener==null) {
            _listener= new DialogListener();
            if (!added) {
                _hub.getCatalogDisplay().addPlotView(_mpw.getPlotView());
                added= true;
            }
            AllPlots.getInstance().addListener(Name.SELECT_DIALOG_CANCEL,_listener);
        }
        AllPlots ap= AllPlots.getInstance();
        ap.showMenuBarPopup();
        if (_standAlone) ap.setMenuBarPopupPersistent(true);

        boolean empty= !ops.isPlotShowing();

        if (empty) {
            if (req.containsParam(CommonParams.DO_PLOT) && req instanceof WebPlotRequest) {
                Timer t = new Timer() { // layout is slow sometime so delay a little (this is a hack)
                    @Override
                    public void run() { deferredPlot(ops, req); }
                };
                t.schedule(100);
            }
            else {
                ops.showImageSelectDialog();
            }
        }
        else {
            if (_standAlone) ops.showImageSelectDialog();
        }
    }

    private void deferredPlot(PlotWidgetOps ops, ServerRequest req) {
        WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);
        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) &&
                req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }
        wpReq.setZoomType(ZoomType.FULL_SCREEN);
        final int w= _mpw.getOffsetWidth()<400 ? 400 : _mpw.getOffsetWidth();
        final int h= _mpw.getOffsetHeight()<400 ? 400 : _mpw.getOffsetHeight();
        wpReq.setZoomToWidth(w);
        wpReq.setZoomToHeight(h);
        ops.plot(wpReq, false, new AsyncCallback<WebPlot>() {
            public void onFailure(Throwable caught) {  }
            public void onSuccess(WebPlot result) {
                if (_mpw.getOffsetHeight()!=h || _mpw.getOffsetWidth()!=w) {
                    handleResize(_mpw.getOffsetWidth(),_mpw.getOffsetHeight());
                }

            }
        });
    }

    class MyDockLayoutPanel extends DockLayoutPanel {

        private OneResizeTimer _oneResizeTimer= new OneResizeTimer();

        public MyDockLayoutPanel() {
            super(Style.Unit.PX);
        }

        @Override
        public void onResize() {
            Widget p= getParent();
            int w= p.getOffsetWidth();
            int h= (p.getOffsetHeight()- (_standAlone ? 5 : CONTROLS_HEIGHT));
            if (!_standAlone) _mpw.setPixelSize(w,h);
            _mpw.onResize();
            super.onResize();    //To change body of overridden methods use File | Settings | File Templates.
            _oneResizeTimer.cancel();
            _oneResizeTimer.setupCall(w,h);
            _oneResizeTimer.schedule(RESIZE_DELAY);
        }
    }

    private void handleResize(final int w ,final int h) {
        Vis.init(new Vis.InitComplete() {
            public void done() {
                WebPlot p = _mpw.getCurrentPlot();
                if (p!=null) {
                    float zlevel=  ZoomUtil.getEstimatedFullZoomFactor(p, new Dimension(w, h),
                                                                       VisUtil.FullType.WIDTH_HEIGHT);
                    if (Math.abs(p.getPlotGroup().getZoomFact()-zlevel) > .01) {
                        p.getPlotView().setZoomTo(zlevel, true, true);
                    }
                }
            }
        });

    }

    private class OneResizeTimer extends Timer {
        private int w= 0;
        private int h= 0;

        public void setupCall(int w, int h) {
            this.w= w;
            this.h= h;
        }

        @Override
        public void run() {
            if (w>0 && h>0 && _mpw !=null && _mpw.isInit()) {
                handleResize(w,h);
            }

        }
    }

    public class DialogListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            if (ev.getName().equals(Name.SELECT_DIALOG_CANCEL)) {
                Vis.init(_mpw, new Vis.InitComplete() {
                    public void done() {
                        if (_mpw.getCurrentPlot()==null) {
                            Toolbar toolbar= Application.getInstance().getToolBar();
                            if (toolbar.getOwner()==FitsInputCmd.this) {
                                toolbar.close();
                            }
                        }
                    }
                });
            }
        }
    }

}

