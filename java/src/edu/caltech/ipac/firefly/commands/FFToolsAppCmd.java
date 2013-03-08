package edu.caltech.ipac.firefly.commands;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HtmlRegionLoader;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.PopupContainerForToolbar;
import edu.caltech.ipac.firefly.ui.catalog.CatalogPanel;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.StringUtils;

public class FFToolsAppCmd extends RequestCmd {

    public  static final String COMMAND = "FFToolsAppCmd";
    public  static final String FITS_BUTTON = "FitsInput";
    public  static final String SEARCH_BUTTON = "Search";

    private static final int CONTROLS_HEIGHT=  40;
    private static final int RESIZE_DELAY= 500;
    private MySplitLayoutPanelFirefly _main= new MySplitLayoutPanelFirefly();
    private MiniPlotWidget mpw = null;
    private LayoutPanel    _imageArea= new LayoutPanel();
    private LayoutPanel         _tableArea= new LayoutPanel();
    private LayoutPanel         _xyPlotArea= new LayoutPanel();
    private DialogListener _listener= null;
    private final TablePreviewEventHub _hub = new TablePreviewEventHub();
    private boolean added= false;
    private boolean closeButtonClosesWindow= false;

    public FFToolsAppCmd() {
        super(COMMAND, "Fits Viewer", "Fits Viewer", true);
    }



    public boolean init() {
        mpw = new MiniPlotWidget(null, new PopupContainerForApp());
        mpw.addStyleName("standard-border");


        _imageArea.add(mpw);

        _main.setSize("100%", "100%");
        _imageArea.addStyleName("fits-input-cmd-main-widget");


        _main.addEast(_xyPlotArea, 500);
        _main.addSouth(_tableArea, 400);
        _main.add(_imageArea);

        _tableArea.add(new Label("Table Here"));
        _xyPlotArea.add(new Label("XY Plot Here"));


        mpw.setRemoveOldPlot(true);
//        mpw.setPopoutButtonVisible(false);
//        mpw.setLockToolbarVisible(true);
        mpw.setMinSize(50, 50);
//        mpw.setTitleAreaAlwaysHidden(true);
        mpw.setAutoTearDown(false);
        mpw.setSaveImageCornersAfterPlot(true);
        mpw.setImageSelection(true);
        CatalogPanel.setDefaultSearchMethod(CatalogRequest.Method.POLYGON);


        TabPane<Widget> tabs= new TabPane<Widget>();
        TabPane.Tab<Widget> tab= new TabPane.Tab<Widget>(tabs,_tableArea,false,"Catalogs", "Catalogs");
        tabs.addTab(tab);
        tab.setSize("100%", "100%");

        new NewTableEventHandler(FFToolEnv.getHub(),tabs);

        Toolbar.Button b= Application.getInstance().getToolBar().getButton(FITS_BUTTON);
        Toolbar toolbar= Application.getInstance().getToolBar();
        if (b!=null) b.setUseDropdown(false);
        toolbar.removeButton(SEARCH_BUTTON);

        _main.addStyleName("main-setto-result-region");
        Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).setDisplay(_main);

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                _main.onResize();
            }
        });

        return true;
    }


    protected void doExecute(final Request req, AsyncCallback<String> callback) {


        WebPlotRequest workReq= null;

        checkSmallIcon(req);

        if (req.containsParam(CommonParams.DO_PLOT)) {
            registerView(LayoutManager.SEARCH_TITLE_REGION, mpw.getTitleWidget());
            workReq= new WebPlotRequest();
            for(Param p : req.getParams()){
                if (!p.getName().equals(Request.ID_KEY)) {
                    workReq.setParam(p);
                }
            }
        }

        //  maybe put this back in later
//            Application.getInstance().getToolBar().setTitle(mpw.getTitleWidget());
//            Application.getInstance().getToolBar().setHeaderButtons(mpw.getToolPanel());
//            Toolbar toolbar= Application.getInstance().getToolBar();
//            toolbar.setContent(_main, false,this,null);

        callback.onSuccess("success!");

        final WebPlotRequest wpr= workReq;

        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(final PlotWidgetOps ops) {
                prepareRequest(ops, wpr != null ? wpr : req);
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

    private void prepareRequest(final PlotWidgetOps ops, final ServerRequest req) {
        if (_listener==null) {
            _listener= new DialogListener();
            if (!added) {
                _hub.getCatalogDisplay().addPlotView(mpw.getPlotView());
                added= true;
            }
            ops.getPlotView().getEventManager().addListener(Name.SELECT_DIALOG_CANCEL,_listener);
        }
        AllPlots ap= AllPlots.getInstance();
        ap.showMenuBarPopup();
        ap.setMenuBarPopupPersistent(true);

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
            ops.showImageSelectDialog();
        }
    }

    private void deferredPlot(PlotWidgetOps ops, ServerRequest req) {
        final WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);

        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) && req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }

        wpReq.setZoomType(ZoomType.FULL_SCREEN);
        final int w= mpw.getOffsetWidth()<400 ? 400 : mpw.getOffsetWidth();
        final int h= mpw.getOffsetHeight()<400 ? 400 : mpw.getOffsetHeight();
        wpReq.setZoomToWidth(w);
        wpReq.setZoomToHeight(h);


        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(final PlotWidgetOps widgetOps) {
                widgetOps.plotExpanded(wpReq, new WebPlotCallback(mpw, false));
            }
        });

    }

    class MySplitLayoutPanelFirefly extends SplitLayoutPanelFirefly {

        private OneResizeTimer _oneResizeTimer= new OneResizeTimer();

        public MySplitLayoutPanelFirefly() {
        }

        @Override
        public void onResize() {
            Widget p= getParent();
            int w= p.getOffsetWidth();
            int h= p.getOffsetHeight()- CONTROLS_HEIGHT;
            mpw.setPixelSize(w, h);
            mpw.onResize();
            super.onResize();    //To change body of overridden methods use File | Settings | File Templates.
            _oneResizeTimer.cancel();
            _oneResizeTimer.setupCall(w,h);
            _oneResizeTimer.schedule(RESIZE_DELAY);
        }
    }

    private void handleResize(final int w ,final int h) {
//        Vis.init(new Vis.InitComplete() {
//            public void done() {
//                WebPlot p = mpw.getCurrentPlot();
//                if (p!=null) {
//                    float zlevel=  ZoomUtil.getEstimatedFullZoomFactor(p, new Dimension(w, h),
//                                                                       VisUtil.FullType.WIDTH_HEIGHT);
//                    if (Math.abs(p.getPlotGroup().getZoomFact()-zlevel) > .01) {
//                        p.getPlotView().setZoomTo(zlevel, true);
//                    }
//                }
//            }
//        });

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
            if (w>0 && h>0 && mpw !=null && mpw.isInit()) {
                handleResize(w,h);
            }

        }
    }


    private static class WebPlotCallback implements AsyncCallback<WebPlot> {
        private final boolean lock;
        private final MiniPlotWidget mpw;
        public WebPlotCallback(MiniPlotWidget mpw, boolean lock) {
            this.lock= lock;
            this.mpw= mpw;
        }

        public void onSuccess(WebPlot plot) {
            if (lock) mpw.getGroup().setLockRelated(lock);
            FFToolEnv.getHub().getCatalogDisplay().addPlotView(plot.getPlotView());
        }
        public void onFailure(Throwable caught) { }
    }



    public class DialogListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            if (ev.getName().equals(Name.SELECT_DIALOG_CANCEL)) {
                Vis.init(mpw, new Vis.InitComplete() {
                    public void done() {
                        if (mpw.getCurrentPlot()==null) {
                            Toolbar toolbar= Application.getInstance().getToolBar();
                            if (toolbar.getOwner()==FFToolsAppCmd.this) {
                                toolbar.close();
                            }
                        }
                    }
                });
            }
        }
    }


    public class PopupContainerForApp extends PopupContainerForToolbar {
        @Override
        protected void dropDownCloseExecuted() {
            if (closeButtonClosesWindow) {
                doCloseBrowserWindow();
            }
            else {
                super.dropDownCloseExecuted();
            }
        }

        @Override
        protected String getDropDownCloseButtonText() {
            if (closeButtonClosesWindow) {
                return "Close Tab";
            }
            else {
                return "Collapse";
            }

        }
    }

    private static native void doCloseBrowserWindow()    /*-{
        $wnd.close();
    }-*/;
}

