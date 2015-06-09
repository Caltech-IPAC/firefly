/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.fftools.core;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.AnyDataSetCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.fftools.XYPlotJSInterface;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.PopupContainerForRegion;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.previews.CoveragePreview;
import edu.caltech.ipac.firefly.ui.previews.MultiDataViewer;
import edu.caltech.ipac.firefly.ui.previews.XYPlotter;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.ui.table.TableResultsDisplay;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.firefly.visualize.ui.DataVisGrid;

import java.util.HashMap;
import java.util.Map;

class StandaloneUI {

    public  static final String FITS_BUTTON = "FitsInput";
    public  static final String SEARCH_BUTTON = "Search";
    private static final String XYVIEW_TAB_NAME = "XY View";

    private SplitLayoutPanelFirefly main= new SplitLayoutPanelFirefly();
    private LayoutPanel             imageArea = new LayoutPanel();
    private DeckLayoutPanel         catalogDeck= new DeckLayoutPanel();
    private LayoutPanel searchResultWrapper = new LayoutPanel();
    private LayoutPanel xyPlotAreaStandalone = new LayoutPanel();
    private final TabPane<Widget>   xyPlots = new TabPane<Widget>();
//    private TabPane<Widget>         tableTabPane = new TabPane<Widget>();
    private TableResultsDisplay     searchResults= new TableResultsDisplay();
    private final TabPane<Widget>   imageTabPane= new TabPane<Widget>();
//    private boolean closeButtonClosesWindow= false;
    private boolean isInit= false;
    private TabPane.Tab<Widget> coverageTab= null;
//    private CrossDocumentMessage xOrMsg;
    private XYPlotter xyPlotter= new XYPlotter(FFToolEnv.getHub());
    private CoveragePreview covPrev= null;
    private boolean initialStart= true;
    private MultiDataViewer dynMultiViewer= new MultiDataViewer();
    private MultiDataViewer dsMultiViewer= new MultiDataViewer();
    private TabPane.Tab<Widget> dynMultiViewerTab = null;
    private TabPane.Tab<Widget> dsMultiViewerTab = null;
    private  TablePanel activeTable= null;
    private String defaultCmdName= AnyDataSetCmd.COMMAND_NAME;

    public StandaloneUI() {
//        this.factory= factory;
//        xOrMsg= new CrossDocumentMessage(FFToolEnv.getHost(GWT.getModuleBaseURL()), new RequestListener());
//        new NewTableEventHandler(FFToolEnv.getHub(), tableTabPane, false);

        if (xyPlotAreaStandalone != null) {
            xyPlotAreaStandalone.addStyleName("standalone-xyplot");
        }

        xyPlots.getEventManager().addListener(TabPane.TAB_ADDED, new WebEventListener<TabPane.Tab>() {
            public void eventNotify(WebEvent<TabPane.Tab> ev) {
                TabPane.Tab tab = ev.getData();
                if (!tab.getName().equals(XYVIEW_TAB_NAME)) {
                    XYPlotWidget w = (XYPlotWidget) tab.getContent();
                    AllPlots.getInstance().registerPopout(w);
                }
            }
        });
        xyPlots.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener<TabPane.Tab>(){
            public void eventNotify(WebEvent<TabPane.Tab> ev) {
                TabPane.Tab tab = ev.getData();
                if (!tab.getName().equals(XYVIEW_TAB_NAME)) {  // if not the tab with XYPlotter, containing table view
                    XYPlotWidget w = (XYPlotWidget) tab.getContent();
                    AllPlots.getInstance().deregisterPopout(w);
                }
            }
        });


        AllPlots.getInstance().addListener(Name.FITS_VIEWER_ADDED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                initialStart = false;
            }
        });

        if (dynMultiViewerTab ==null) {
            dynMultiViewerTab = imageTabPane.addTab(dynMultiViewer.getWidget(), "User Loaded FITS", "FITS Image", false);
        }

        dynMultiViewer.setMpwFactory(new MyMpwFactory());
        dynMultiViewer.setNoDataMessage("No FITS data loaded, Choose Add/Modify Image to load.");
        dynMultiViewer.setRefreshListener(new MultiDataViewer.RefreshListener() {
            public void preDataChange() {
                handleViewUpdates();
                imageTabPane.selectTab(dynMultiViewerTab);
            }

            public void imageDeleted() {
                handleViewUpdates();
            }

            public void viewerRefreshed() {
                if (AllPlots.getInstance().getAll().size() == 1 && hasOnlyPlotResults()) {
                    handleViewUpdates();
                }
            }
        });

        dsMultiViewer.bind(Application.getInstance().getEventHub());
    }


    public void setDefaultCmdName(String cmdName) {
        defaultCmdName= cmdName;
    }

    private void handleViewUpdates() {
        AllPlots ap= AllPlots.getInstance();
        if (ap.isExpanded())  ap.updateExpanded(PopoutWidget.getViewType());
        if (hasPlotResults()) {
            if (AllPlots.getInstance().getMiniPlotWidget()==null || !dynMultiViewer.hasContent()) {
                relayoutMainArea();
            }
            else if (!AllPlots.getInstance().isExpanded() && hasOnlyPlotResults()) {
                expandImage();
            }
        }
        else {
            if (hasTableResults()) {
                relayoutMainArea();
            }
            else {
                GeneralCommand cmd= Application.getInstance().getCommand(ImageSelectDropDownCmd.COMMAND_NAME);
                if (cmd!=null) cmd.execute();
            }

        }
    }

    public MultiDataViewer getMultiViewer() { return dynMultiViewer; }

    public boolean isInitialStart() { return initialStart; }
//    public void initStartComplete() { initialStart= false; }

    public void ensureDynImageTabShowing() {
        imageTabPane.selectTab(dynMultiViewerTab);
        dynMultiViewer.ensureMPWSelected();
    }

    public boolean hasResults() {
       return (hasPlotResults() || searchResults.getTabPane().getSelectedIndex()!=-1);
    }
    public boolean hasTableResults() { return (searchResults.getTabPane().getSelectedIndex()!=-1); }
    public boolean hasOnlyPlotResults() { return hasPlotResults() && !hasTableResults(); }

    public boolean hasCatalogResults() {
        boolean retval= false;
        if (activeTable!=null && activeTable.getDataset()!=null) {
            TableMeta meta= activeTable.getDataset().getMeta();
            retval= meta.contains(MetaConst.CATALOG_OVERLAY_TYPE);
        }
        return retval;
    }





//    public boolean hasPlotResults() { return (AllPlots.getInstance().getAll().size()>0); }
    public boolean hasPlotResults() {
        ImageSelectDropDownCmd cmd= (ImageSelectDropDownCmd)Application.getInstance().getCommand(ImageSelectDropDownCmd.COMMAND_NAME);
        PlotData dynData= ConverterStore.get(ConverterStore.DYNAMIC).getPlotData();
        return cmd.isInProcessOfPlotting() || dynData.hasPlotsDefined();
    }

    public void expandImage()  {
        ImageSelectDropDownCmd cmd= (ImageSelectDropDownCmd)Application.getInstance().getCommand(ImageSelectDropDownCmd.COMMAND_NAME);
        if (cmd!=null && cmd.isInProcessOfPlotting()) dynMultiViewer.forceExpand();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
                if (mpw!=null) AllPlots.getInstance().forceExpand(mpw);
            }
        });
    }
    public void collapseImage()  {
        if (AllPlots.getInstance().isExpanded()) {
            AllPlots.getInstance().forceCollapse();
            dynMultiViewer.cancelExpand();
        }
    }

    public void init() {
        if (isInit) return;


        imageArea.add(imageTabPane);
        TableMeta meta= new TableMeta();
        meta.setAttribute(MetaConst.DATASET_CONVERTER, "DYNAMIC");
        dynMultiViewer.addGridInfo(meta);
//        imageArea.add(factory.getPlotTabPane());

        main.setSize("10px", "10px");

        imageArea.addStyleName("fits-input-cmd-main-widget");
        main.addStyleName("main-setto-result-region");


        configureNewTableListening();
        searchResultWrapper.addStyleName("catalog-area");
        searchResultWrapper.add(searchResults.getDisplay());



//        catalogDeck.add(catalogLabel);
        catalogDeck.add(searchResultWrapper);
        catalogDeck.setWidget(searchResultWrapper);

//        reinitMainWidgets();
        relayoutMainArea();

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

    public void newXYPlot(Map<String,String> params) {
        final XYPlotWidget xyPlotWidget = XYPlotJSInterface.getXYPlotWidget(params);
        xyPlots.addTab(xyPlotWidget, "XY Data", "XY Plot", true);
        xyPlots.selectTab(xyPlots.getNTabs() - 1);

        relayoutMainArea();
        XYPlotJSInterface.loadData(xyPlotWidget, params);
    }

    public void relayoutMainArea() {
        Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).setDisplay(main);
        main.setSize("100%", "100%");
        main.clear();

        if (xyPlots.getNTabs() > 1 || (xyPlots.getNTabs() == 1 && xyPlots.getTab(XYVIEW_TAB_NAME)== null)) {
            if (xyPlots.getTab(XYVIEW_TAB_NAME) != null) {
                xyPlots.getTab(XYVIEW_TAB_NAME).setContent(xyPlotter.getWidget());
            }
            main.addSouth(xyPlots, 300);
            xyPlots.setSize("100%", "100%");
        } else if (hasCatalogResults() && xyPlotAreaStandalone !=null) {
            xyPlotAreaStandalone.clear();
            xyPlotAreaStandalone.add(xyPlotter.getWidget());
            main.addSouth(xyPlotAreaStandalone, 300);
            xyPlotAreaStandalone.setSize("100%", "100%");

        }

        if (dynMultiViewerTab ==null && hasPlotResults()) {
            dynMultiViewerTab = imageTabPane.addTab(dynMultiViewer.getWidget(), "FITS data", "FITS Image", false);
        }
        else if (!hasPlotResults() && dynMultiViewerTab !=null) {
            imageTabPane.removeTab(dynMultiViewerTab);
            dynMultiViewerTab = null;
        }

        if (coverageTab!=null || hasPlotResults()) {
            int eastSize= 400;
            int winWidth= Window.getClientWidth();
            if (winWidth>50) eastSize= (int)(.6*Window.getClientWidth());
            main.addEast(catalogDeck, eastSize);
            main.add(imageArea);
        }
        else {
            main.add(catalogDeck);
        }
        main.forceLayout();
    }



//    private MiniPlotWidget getCurrentMPW() {
//        MiniPlotWidget  mpw= null;
//        TabPane.Tab tab= factory.getPlotTabPane().getSelectedTab();
//        if (tab!=null) {
//            mpw= factory.getMPW(tab);
//        }
//        return mpw;
//    }

//    public boolean isCloseButtonClosesWindow() {
//        return closeButtonClosesWindow;
//    }


    private void configureNewTableListening() {

        WebEventManager evMan=FFToolEnv.getHub().getEventManager();

        evMan.addListener(EventHub.ON_TABLE_ADDED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                initialStart= false;
                activeTable= (TablePanel)ev.getData();
                TableMeta meta= activeTable.getDataset().getMeta();
                initialStart= false;
                catalogDeck.setWidget(searchResultWrapper);
                collapseImage();

                if (meta.contains(MetaConst.CATALOG_COORD_COLS) ||
                    meta.contains(MetaConst.CENTER_COLUMN) ||
                    meta.contains(MetaConst.ALL_CORNERS) ) {
                    if (coverageTab==null && !hasPlotResults())  addCoverageTab();
                }

                if (!meta.contains(MetaConst.CATALOG_OVERLAY_TYPE) || meta.contains(MetaConst.DATASET_CONVERTER)) {
                    if (dsMultiViewerTab==null) {
                        dsMultiViewerTab = imageTabPane.addTab(dsMultiViewer.getWidget(), "FITS Data Sets", "FITS Image", false);
                    }
                    imageTabPane.selectTab(dsMultiViewerTab);
                }

                if (hasCatalogResults()) {
                     if (xyPlots.getTab(XYVIEW_TAB_NAME) == null) {
                         xyPlots.addTab(0, xyPlotter.getWidget(), XYVIEW_TAB_NAME, "XY View of an active table", false, true);
                     }
                     xyPlots.selectTab(0);
                }

                relayoutMainArea();
            }
        });



        evMan.addListener(EventHub.ON_TABLE_SHOW, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                activeTable= (TablePanel)ev.getSource();
            }
        });
        evMan.addListener(EventHub.ON_DATA_LOAD, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                activeTable= (TablePanel)ev.getSource();
            }
        });
        evMan.addListener(EventHub.ON_TABLE_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (ev.getData()==activeTable) activeTable= null;
//                FFToolEnv.getHub().getCatalogDisplay().removeCatalog(activeTable);
            }
        });




        searchResults.getTabPane().getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (searchResults.getTabPane().getSelectedIndex()==-1) {
                    resetNoTableView();
                }
            }
        } );
        searchResults.getTabPane().getEventManager().addListener(TabPane.TAB_SELECTED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (imageTabPane.getSelectedTab()==dsMultiViewerTab) {
                   dsMultiViewer.forceGridUpdate();
                }
            }
        });

        imageTabPane.getEventManager().addListener(TabPane.TAB_SELECTED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (imageTabPane.getSelectedTab()==dsMultiViewerTab) {
                    dsMultiViewer.forceGridUpdate();
                }
            }
        });


    }

    private void resetNoTableView() {
//        tableTabPane.add(); //todo unbind the table

        if (!hasTableResults()) {
            if (coverageTab!=null) imageTabPane.removeTab(coverageTab);
            if (covPrev!=null) covPrev.cleanup();
            coverageTab= null;
            covPrev= null;
        }

        if (hasOnlyPlotResults()) {
            AllPlots.getInstance().forceExpand(AllPlots.getInstance().getMiniPlotWidget());
        }
        else if (!hasResults()) {
            GeneralCommand cmd= Application.getInstance().getCommand(defaultCmdName);
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



        coverageTab= imageTabPane.addTab(covPrev.getDisplay(), "Coverage", "Coverage of catalog",false);


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
        if (enable)  imageTabPane.showTab(imageTabPane.getTab(tp.getName()));
        else         imageTabPane.hideTab(imageTabPane.getTab(tp.getName()));
    }

    private TablePreview getPreviewAtTabIdx(int idx) {
        EventHub hub= FFToolEnv.getHub();
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


    public class MyMpwFactory implements DataVisGrid.MpwFactory  {
        public MiniPlotWidget make(String groupName) {
            return new MiniPlotWidget(groupName, makePopoutContainerForApp());
        }
        public void addAttributes(MiniPlotWidget mpw) {/*do nothing*/ }
    }


    public PopupContainerForRegion makePopoutContainerForApp()  {
       return new PopupContainerForApp();
    }


    public class PopupContainerForApp extends PopupContainerForRegion {
        @Override
        protected void dropDownCloseExecuted() {
            if (hasTableResults()) {
                relayoutMainArea();
            }
            else {
                GeneralCommand cmd= Application.getInstance().getCommand(ImageSelectDropDownCmd.COMMAND_NAME);
                if (cmd!=null) cmd.execute();
            }
            super.dropDownCloseExecuted();
        }


        public boolean isCloseShowing() {
            return FFToolEnv.getHub().getTables().size()>0;
        }
        public boolean isViewControlShowing() { return true; }
        public boolean isImageSelectionShowing() { return true; }
    }

//    private static native void doCloseBrowserWindow()    /*-{
//        $wnd.close();
//    }-*/;
//
//    private static native void doFocus()    /*-{
//        $wnd.focus();
//    }-*/;




}

