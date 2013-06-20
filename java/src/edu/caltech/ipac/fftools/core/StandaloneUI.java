package edu.caltech.ipac.fftools.core;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupContainerForToolbar;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.previews.CoveragePreview;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.util.CrossDocumentMessage;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.HashMap;
import java.util.Map;

public class StandaloneUI {

    public  static final String FITS_BUTTON = "FitsInput";
    public  static final String SEARCH_BUTTON = "Search";

    public enum Mode {INIT, IMAGE_ONLY, IMAGE_WITH_CATALOG,
                      CATALOG_PLUS_BACKGROUND,
                      CATALOG_START}

    private SplitLayoutPanelFirefly main= new SplitLayoutPanelFirefly();
    private LayoutPanel             imageArea = new LayoutPanel();
    private DeckLayoutPanel         catalogDeck= new DeckLayoutPanel();
    private LayoutPanel             catalogArea = new LayoutPanel();
    private LayoutPanel             xyPlotArea = new LayoutPanel();
    private TabPane<Widget>         tabsPane= new TabPane<Widget>();
    private final TabPlotWidgetFactory factory;
    private boolean closeButtonClosesWindow= false;
    private Mode mode= Mode.INIT;
//    private HTML catalogLabel= new HTML("Catalogs");
    private Label catalogLabel;
    private boolean isInit= false;
    private boolean mainIsFull= false;
    private TabPane.Tab<Widget> coverageTab= null;
    private CrossDocumentMessage xOrMsg;

    public StandaloneUI(TabPlotWidgetFactory factory) {
        this.factory= factory;
//        xOrMsg= new CrossDocumentMessage(FFToolEnv.getHost(GWT.getModuleBaseURL()), new RequestListener());
        new NewTableEventHandler(FFToolEnv.getHub(),tabsPane, false);
    }

    //----------------------------------------------
    //------ Mode stuff
    //----------------------------------------------
    public Mode getMode() { return mode; }

    public void eventAddedImage() {
        Mode oldMode= mode;
        switch (mode) {
            case INIT:                    mode= Mode.IMAGE_ONLY; break;
            case IMAGE_ONLY:              mode= Mode.IMAGE_ONLY; break;
            case IMAGE_WITH_CATALOG:      mode= Mode.IMAGE_WITH_CATALOG; break;
            case CATALOG_PLUS_BACKGROUND: mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case CATALOG_START:           mode= Mode.IMAGE_ONLY; break;
        }
        modeChange(mode,oldMode);

    }

    public void eventEmptyAppQueryImage() {
        Mode oldMode= mode;
        switch (mode) {
            case IMAGE_ONLY:              mode= Mode.IMAGE_ONLY; break;
            case IMAGE_WITH_CATALOG:      mode= Mode.IMAGE_WITH_CATALOG; break;
            case CATALOG_PLUS_BACKGROUND: mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case CATALOG_START:           mode= Mode.IMAGE_ONLY; break;
        }
        modeChange(mode,oldMode);
    }

    public void eventCatalogAdded() {
        Mode oldMode= mode;
        switch (mode) {
            case INIT:                    mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case IMAGE_ONLY:              mode= Mode.IMAGE_WITH_CATALOG; break;
            case IMAGE_WITH_CATALOG:      mode= Mode.IMAGE_WITH_CATALOG; break;
            case CATALOG_PLUS_BACKGROUND: mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case CATALOG_START:           mode= Mode.CATALOG_PLUS_BACKGROUND; break;
        }
        modeChange(mode,oldMode);
        if (Application.getInstance().getToolBar().isOpen())Application.getInstance().getToolBar().close();
//        collapseImage();
    }

    public void eventSearchingCatalog() {
        Mode oldMode= mode;
        switch (mode) {
            case INIT:                    mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case IMAGE_ONLY:              mode= Mode.IMAGE_WITH_CATALOG; break;
            case IMAGE_WITH_CATALOG:      mode= Mode.IMAGE_WITH_CATALOG; break;
            case CATALOG_PLUS_BACKGROUND: mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case CATALOG_START:           mode= Mode.CATALOG_PLUS_BACKGROUND; break;
        }
        modeChange(mode,oldMode);
    }

    public void eventOpenCatalog() {
        Mode oldMode= mode;
        switch (mode) {
            case INIT:                    mode= Mode.CATALOG_START; break;
            case IMAGE_ONLY:              mode= Mode.IMAGE_ONLY; break;
            case IMAGE_WITH_CATALOG:      mode= Mode.IMAGE_WITH_CATALOG; break;
            case CATALOG_PLUS_BACKGROUND: mode= Mode.CATALOG_PLUS_BACKGROUND; break;
            case CATALOG_START:           mode= Mode.CATALOG_START; break;
        }
        modeChange(mode,oldMode);
    }

    //----------------------------------------------
    //----------------------------------------------
    //----------------------------------------------
    //----------------------------------------------

    private void modeChange(Mode mode, Mode oldMode) {
        if (mode!=oldMode) {
            if (mode==Mode.IMAGE_WITH_CATALOG ||
                mode==Mode.CATALOG_PLUS_BACKGROUND ||
                mode==Mode.CATALOG_START) {
                prepareMainArea();
                factory.setSharingView(true);
            }
        }
    }

    public void expandImage()  {
        MiniPlotWidget mpw= getCurrentMPW();
        if (mpw!=null) AllPlots.getInstance().forceExpand(mpw);
    }
    public void collapseImage()  {
        MiniPlotWidget mpw= getCurrentMPW();
        if (mpw!=null) mpw.forceCollapse();
    }

    public void init() {
        if (isInit) return;

        imageArea.add(factory.getPlotTabPane());

        main.setSize("10px", "10px");

        imageArea.addStyleName("fits-input-cmd-main-widget");
        main.addStyleName("main-setto-result-region");



        xyPlotArea.add(new Label("XY Plot Here"));



        configureCatalogListening();
        catalogArea.addStyleName("catalog-area");
        catalogArea.add(tabsPane);


        catalogLabel= GwtUtil.makeLinkButton("Load Catalogs", "Load a catalog", new ClickHandler() {
            public void onClick(ClickEvent event) {
                ((FFToolsStandaloneCreator)Application.getInstance().getCreator()).activateToolbarCatalog();
            }
        });


        catalogDeck.add(catalogLabel);
        catalogDeck.add(catalogArea);
        catalogDeck.setWidget(catalogLabel);

        reinitMainWidgets();

        GwtUtil.setStyles(catalogLabel, "padding",  "220px 0 0 125px",
                                        "fontSize", "20pt");



        Toolbar.Button b= Application.getInstance().getToolBar().getButton(FITS_BUTTON);
        Toolbar toolbar= Application.getInstance().getToolBar();
        if (b!=null) b.setUseDropdown(false);
        toolbar.removeButton(SEARCH_BUTTON);

        Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).setDisplay(main);

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                main.onResize();
            }
        });


        Map<String,String> params= new HashMap<String, String>(5);
        params.put(EventWorker.ID, "target");
        params.put(CommonParams.TARGET_TYPE, ActiveTargetCreator.TargetType.PlotFixedTarget.toString());
        EventWorker targetLayer= new WidgetFactory().createEventWorker(CommonParams.ACTIVE_TARGET, params);
        targetLayer.bind(FFToolEnv.getHub());


        isInit= true;
    }

    void reinitMainWidgets() {
        main.clear();
        main.addEast(catalogDeck, 400);
        main.add(imageArea);
        main.forceLayout();
    }

    public void prepareMainArea() {
        if (!mainIsFull) {
            main.setSize("100%", "100%");
            mainIsFull= true;
            reinitMainWidgets();
        }
    }


    private MiniPlotWidget getCurrentMPW() {
        MiniPlotWidget  mpw= null;
        TabPane.Tab tab= factory.getPlotTabPane().getSelectedTab();
        if (tab!=null) {
            mpw= factory.getMPW(tab);
        }
        return mpw;
    }

    public boolean isCloseButtonClosesWindow() {
        return closeButtonClosesWindow;
    }


    private void configureCatalogListening() {
        WebEventManager.getAppEvManager().addListener(Name.NEW_TABLE_RETRIEVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                prepareMainArea();
                catalogDeck.setWidget(catalogArea);
                eventCatalogAdded();
                collapseImage();
                int w= Window.getClientWidth()-20;
                GwtUtil.DockLayout.setWidgetChildSize(catalogDeck, (int)(.6*w));
                main.forceLayout();
                if (coverageTab==null && mode==Mode.CATALOG_PLUS_BACKGROUND) {
                   addCoverageTab();
                }
            }
        });
        tabsPane.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (tabsPane.getSelectedIndex()==-1) {
                    catalogDeck.setWidget(catalogLabel);
                }
            }
        } );
    }


    private void addCoverageTab() {

        Map<String,String> paramMap= new HashMap<String, String>(7);
        WidgetFactory widgetFactory= Application.getInstance().getWidgetFactory();
        paramMap.put(CommonParams.ENABLE_DEFAULT_COLUMNS, "true");
        paramMap.put(CommonParams.CATALOGS_AS_OVERLAYS, "false");

        CoveragePreview covPrev= (CoveragePreview)widgetFactory.createObserverUI(WidgetFactory.COVERAGE_VIEW,paramMap);
        covPrev.bind(FFToolEnv.getHub());


        MiniPlotWidget mpw= covPrev.getMPW();
        mpw.addStyleName("standard-border");
        mpw.setMinSize(50, 50);
        mpw.setAutoTearDown(false);
        mpw.setSaveImageCornersAfterPlot(true);
        mpw.setTitleAreaAlwaysHidden(true);
        mpw.setInlineToolbar(true);
        mpw.setUseToolsButton(false);



        coverageTab= factory.addTab(covPrev.getDisplay(), "Coverage", "Coverage of catalog");
        TabPane<Widget> imageTabPane= factory.getTabPane();


        imageTabPane.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            public void onBeforeSelection(BeforeSelectionEvent<Integer> ev) {
                TablePreview cview = getPreviewAtTabIdx(ev.getItem());
                if (cview != null)  cview.onHide();

            }
        });

        imageTabPane.addSelectionHandler(new SelectionHandler<Integer>(){
            public void onSelection(SelectionEvent<Integer> ev) {
                TablePreview cview = getPreviewAtTabIdx(ev.getSelectedItem());
                if (cview != null)  cview.onShow();

            }
        });

        TablePreviewEventHub hub= FFToolEnv.getHub();
        hub.getEventManager().addListener(TablePreviewEventHub.ENABLE_PREVIEW,
                                               new WebEventListener(){
                                                   public void eventNotify(WebEvent ev) {
                                                       setPreviewEnabled((TablePreview) ev.getSource(), true);
                                                   }
                                               });

        hub.getEventManager().addListener(TablePreviewEventHub.DISABLE_PREVIEW,
                                               new WebEventListener(){
                                                   public void eventNotify(WebEvent ev) {
                                                       setPreviewEnabled((TablePreview) ev.getSource(), false);
                                                   }
                                               });
    }

    private void setPreviewEnabled(TablePreview tp, boolean enable) {
        TabPane<Widget> imageTabPane= factory.getTabPane();
        if (enable)  imageTabPane.showTab(imageTabPane.getTab(tp.getName()));
        else         imageTabPane.hideTab(imageTabPane.getTab(tp.getName()));

    }

    private TablePreview getPreviewAtTabIdx(int idx) {
        TablePreviewEventHub hub= FFToolEnv.getHub();
        TabPane<Widget> imageTabPane= factory.getTabPane();
        if  (idx==-1) return null;
        TabPane.Tab t = imageTabPane.getVisibleTab(idx);
        if (t != null) {
            for (TablePreview tp : hub.getPreviews()) {
                if (tp.getName().equals(t.getName())) {
                    return tp;
                }
            }
        }
        return null;
    }


    public PopupContainerForToolbar makePopoutContainerForApp()  {
       return new PopupContainerForApp();
    }


    public class PopupContainerForApp extends PopupContainerForToolbar {
        @Override
        protected void dropDownCloseExecuted() {

            if (isCloseButtonClosesWindow()) {
                doCloseBrowserWindow();
            }
            else {
                prepareMainArea();
                super.dropDownCloseExecuted();
            }
        }

        @Override
        protected String getDropDownCloseButtonText() {

            if (isCloseButtonClosesWindow()) {
                return "Close Tab";
            }
            else {
                return "Collapse";
            }

        }

        @Override
        protected void expand() {
            super.expand();
        }

        @Override
        protected void collapse() {
            super.collapse();
        }


        public boolean isCloseShowing() { return FFToolEnv.getHub().getTables().size()>0; }
        public boolean isViewControlShowing() { return true; }
        public boolean isImageSelectionShowing() { return true; }
    }

    private static native void doCloseBrowserWindow()    /*-{
        $wnd.close();
    }-*/;

    private static native void doFocus()    /*-{
        $wnd.focus();
    }-*/;


    private class RequestListener implements CrossDocumentMessage.MessageListener {
        public void message(String msg) {
            Request r= ServerRequest.parse(msg, new Request());
            if (r.getRequestClass().equals(WebPlotRequest.WEB_PLOT_REQUEST_CLASS)) {
                // do image loading
//                WebPlotRequest wpr= WebPlotRequest.parse(msg)
                r.setParam(CommonParams.DO_PLOT,true+"");
                r.setRequestId(FFToolsImageCmd.COMMAND);
                Application.getInstance().getRequestHandler().processRequest(r);
//                Window.alert("New Image loaded");
               doFocus();
            }
            else {
               // to some catalog loading
            }
        }
    }



//    public class NewTableEventHandler_NEW_ATTEMPT implements WebEventListener {
//
//        private TabPane tab;
//
//        public NewTableEventHandler_NEW_ATTEMPT(TabPane tab) {
//            this.tab = tab;
//
//            WebEventManager.getAppEvManager().addListener(Name.NEW_TABLE_RETRIEVED, this);
//
//            tab.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener(){
//                public void eventNotify(WebEvent ev) {
//                    TabPane.Tab<TablePanel> tab = (TabPane.Tab<TablePanel>) ev.getData();
//                    FFToolEnv.getHub().unbind(tab.getContent());
//                    WebEventManager.getAppEvManager().removeListener(Name.NEW_TABLE_RETRIEVED, NewTableEventHandler_NEW_ATTEMPT.this);
//                }
//            });
//        }
//
//        public void eventNotify(WebEvent ev) {
//            NewTableResults tableInfo = (NewTableResults) ev.getData();
//
//            if (tableInfo != null && tableInfo.getConfig() != null) {
//                final TableConfig tconfig = tableInfo.getConfig();
//                WidgetFactory factory = Application.getInstance().getWidgetFactory();
//                Map<String, String> params = new HashMap<String, String>(2);
//                params.put(TablePanelCreator.TITLE, tconfig.getTitle());
//                params.put(TablePanelCreator.SHORT_DESC, tconfig.getShortDesc());
//                final PrimaryTableUI table = factory.createPrimaryUI(tableInfo.getTableType(),
//                                                                     tconfig.getSearchRequest(), params);
//                final TabPane.Tab tabItem = tab.addTab(table.getDisplay(), tconfig.getTitle(), tconfig.getShortDesc(), true);
//                table.bind(FFToolEnv.getHub());
//                table.load(new BaseCallback<Integer>(){
//                    public void doSuccess(Integer result) {
//                        DownloadRequest dlreq = tconfig.getDownloadRequest();
//                        if (dlreq!=null) {
//                            table.addDownloadButton(tconfig.getDownloadSelectionIF(), dlreq.getRequestId(),
//                                                    dlreq.getFilePrefix(), dlreq.getTitlePrefix(), null);
//                        }
//                        tab.selectTab(tabItem);
//                        if (table instanceof TablePrimaryDisplay) {
//                            TablePanel tp= ((TablePrimaryDisplay)table).getTable();
//                            tp.getTable().setShowUnits(true);
//                            tp.getTable().showFilters(true);
//                        }
//                    }
//                });
//            }
//        }
//    }



}

