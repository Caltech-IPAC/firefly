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
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
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
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.util.CrossDocumentMessage;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;

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
    private Label catalogLabel;
    private boolean isInit= false;
    private boolean mainIsFull= false;
    private TabPane.Tab<Widget> coverageTab= null;
    private CrossDocumentMessage xOrMsg;
    private XYPlotter xyPlotter= new XYPlotter();
    private CoveragePreview covPrev= null;
    private boolean initialStart= true;

    public StandaloneUI(TabPlotWidgetFactory factory) {
        this.factory= factory;
//        xOrMsg= new CrossDocumentMessage(FFToolEnv.getHost(GWT.getModuleBaseURL()), new RequestListener());
        new NewTableEventHandler(FFToolEnv.getHub(),tabsPane, false);

        if (xyPlotArea != null) {
            xyPlotArea.addStyleName("standalone-xyplot");
        }
        xyPlotArea.add(xyPlotter.getWidget());

        AllPlots.getInstance().addListener(Name.FITS_VIEWER_ADDED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                initialStart= false;
            }
        });
    }


    public boolean isInitialStart() { return initialStart; }
    public void initStartComplete() { initialStart= false; }

    public boolean hasResults() {
       return (AllPlots.getInstance().getAll().size()>0 || tabsPane.getSelectedIndex()!=-1);
    }

    public boolean hasTableResults() { return (tabsPane.getSelectedIndex()!=-1); }
    public boolean hasOnlyPlotResults() { return hasPlotResults() && !hasTableResults(); }
    public boolean hasPlotResults() { return (AllPlots.getInstance().getAll().size()>0); }

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
        if (xyPlotArea != null) {
            main.addSouth(xyPlotArea, 300);
        }
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


        FFToolEnv.getHub().getEventManager().addListener(EventHub.ON_TABLE_ADDED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {

                final TablePanel table= (TablePanel)ev.getData();
                TableMeta meta= table.getDataset().getMeta();
                initialStart= false;
                prepareMainArea();
                catalogDeck.setWidget(catalogArea);
//                eventCatalogAdded();
                collapseImage();

                boolean hasCoverage= false;
                if (meta.contains(MetaConst.CATALOG_OVERLAY_TYPE) && meta.contains(MetaConst.CATALOG_COORD_COLS)) {
                    if (coverageTab==null && !hasPlotResults())  addCoverageTab();
                    hasCoverage= true;
                }


                int w= Window.getClientWidth()-20;
                if (hasCoverage || hasPlotResults()) {
                    GwtUtil.DockLayout.setWidgetChildSize(catalogDeck, (int)(.6*w));
                }
                else {
                    GwtUtil.DockLayout.setWidgetChildSize(catalogDeck, w);
                }


                main.forceLayout();
                Application.getInstance().getToolBar().close();


            }
        });



//        WebEventManager.getAppEvManager().addListener(Name.NEW_TABLE_RETRIEVED, new WebEventListener() {
//            public void eventNotify(WebEvent ev) {
//
//
//
//
//
//                initialStart= false;
//                prepareMainArea();
//                catalogDeck.setWidget(catalogArea);
////                eventCatalogAdded();
//                collapseImage();
//                int w= Window.getClientWidth()-20;
//                GwtUtil.DockLayout.setWidgetChildSize(catalogDeck, (int)(.6*w));
//                main.forceLayout();
//                Application.getInstance().getToolBar().close();
//                if (coverageTab==null && !hasPlotResults())  addCoverageTab();
//
//            }
//        });
        tabsPane.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (tabsPane.getSelectedIndex()==-1) {
                    resetNoTableView();
//                    catalogDeck.setWidget(catalogLabel);
                }
            }
        } );
    }

    private void resetNoTableView() {
//        tabsPane.add(); //todo unbind the table

        if (!hasTableResults()) {
            if (coverageTab!=null) factory.removeTab(coverageTab);
            if (covPrev!=null) covPrev.cleanup();
            coverageTab= null;
            covPrev= null;
        }

        if (hasOnlyPlotResults()) {
            AllPlots.getInstance().forceExpand(AllPlots.getInstance().getMiniPlotWidget());
        }
        else if (!hasResults()) {
//            Mode old= mode;
//            mode= Mode.CATALOG_START;
//            modeChange(old,mode);
            GeneralCommand cmd= Application.getInstance().getCommand(IrsaCatalogDropDownCmd.COMMAND_NAME);
            cmd.execute();
        }
    }



    private void addCoverageTab() {

        Map<String,String> paramMap= new HashMap<String, String>(7);
        WidgetFactory widgetFactory= Application.getInstance().getWidgetFactory();
        paramMap.put(CommonParams.ENABLE_DEFAULT_COLUMNS, "true");
        paramMap.put(CommonParams.CATALOGS_AS_OVERLAYS, "false");
        paramMap.put("EVENT_WORKER_ID","target");

        covPrev= (CoveragePreview)widgetFactory.createObserverUI(WidgetFactory.COVERAGE_VIEW,paramMap);
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

        EventHub hub= FFToolEnv.getHub();
        hub.getEventManager().addListener(EventHub.ENABLE_PREVIEW,
                                               new WebEventListener(){
                                                   public void eventNotify(WebEvent ev) {
                                                       setPreviewEnabled((TablePreview) ev.getSource(), true);
                                                   }
                                               });

        hub.getEventManager().addListener(EventHub.DISABLE_PREVIEW,
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
        EventHub hub= FFToolEnv.getHub();
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


        public boolean isCloseShowing() {
            return FFToolEnv.getHub().getTables().size()>0;
        }
        public boolean isViewControlShowing() { return true; }
        public boolean isImageSelectionShowing() { return true; }
    }

    private static native void doCloseBrowserWindow()    /*-{
        $wnd.close();
    }-*/;

    private static native void doFocus()    /*-{
        $wnd.focus();
    }-*/;




}

