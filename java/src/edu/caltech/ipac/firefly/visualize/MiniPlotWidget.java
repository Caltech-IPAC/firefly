package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.GridCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopoutContainer;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.PopupContainerForStandAlone;
import edu.caltech.ipac.firefly.ui.PopupContainerForToolbar;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.firefly.visualize.task.PlotFileTask;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * Create a Widget to manager WebPlots.  All WebPlots should exist in a MiniPlotWidget
 * NOTE - you should never call setWidth, setHeight, or setPixelSize on this widget
 * @author Trey Roby
 */
public class MiniPlotWidget extends PopoutWidget {

    public enum PopoutType {TOOLBAR,STAND_ALONE}


    public static final String DISABLED= "DISABLED";
    public static final String DEF_WORKING_MSG= "Plotting ";
    public static final int MIN_WIDTH=   320;
    public static final int MIN_HEIGHT=  350;

    private static final FireflyCss fireflyCss= CssData.Creator.getInstance().getFireflyCss();
    private static final int TOOLBAR_SIZE= 32;

    private final HiddableLayoutPanel _panel= new HiddableLayoutPanel(Style.Unit.PX);
    private       InlineTitleLayoutPanel _plotPanel= null;
    private final FlexTable _selectionMbarDisplay= new FlexTable();
    private final FlexTable _flipMbarDisplay= new FlexTable();

    private final Map<String, String> _reqMods= new HashMap<String, String>(5);
    private final PlotWidgetGroup _group;

    private PlotFileTask _plotTask;
    private MenuItem     _flipFrame;
    private PlotError    _plotError       = new DefaultPlotError();

    private boolean      _firstPlot       = true;
    private boolean      _initialized     = false;
    private boolean      _showInlineTitle = false;

    // many options
    private String _workingMsg= DEF_WORKING_MSG;
    private final boolean _showTitle= true;       // if true show the title area, currently always true, may change later
    private boolean      _removeOldPlot   = true; // if true keep the last plot for flipping
    private boolean      _allowImageSelect= false; // show the image selection button in the toolbar, user can change image
    private boolean      _allowImageLock  = true; // show the image selection button in the toolbar, user can change image
    private boolean      _rotateNorth     = false; // rotate this plot north when plotting
    private boolean      _turnOnGridAfterPlot= false; // turn on the grid after plot
    private boolean      _userModifiedRotate= false; // the user modified the rotate status
    private boolean      _showScrollBars  = false;  // if true show the scroll bar otherwise just use google maps type scrolling
    private boolean      _rememberZoom    = false; // remember the last zoom level and display the next plot at that level
    private boolean      _autoTearDown    = true;  // tear down when there is a new search
    private boolean      _saveCorners     = false; // save the four corners of the plot to the ActiveTarget singleton
    private boolean      _active          = true;  // this is the active MiniPlotWidget
    private boolean      _boxSelection    = false; // type of highlighting used when user selects this widget
    private boolean      _showAd          = false; // show home add
    private boolean      _catalogButton   = false; // show the catalog select button
    private boolean      _hideTitleDetail = false; // hide the zoom level and rotation shown in the title
    private final boolean _fullControl;


    //preference controls
    private String       _preferenceColorKey= DISABLED;
    private String       _preferenceZoomKey = DISABLED;

    // all the following are initialized in initAsync
    private WebPlotView              _plotView      = null;
    private PlotWidgetOps            _ops           = null;
    private PlotWidgetColorPrefs     _colorPrefs    = null;
    private PlotWidgetZoomPrefs      _zoomPrefs     = null;

    private String _expandedTitle = null;
    private String _addText =  "<a target=\"_blank\" class=\"link-color\" style=\"font-size:7pt; \" href=\"http://irsa.ipac.caltech.edu\">Powered by IRSA @ IPAC</a>";

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public MiniPlotWidget() {  this(null); }

    public MiniPlotWidget(String groupName) {
        this(Application.getInstance().getCreator().isApplication() ?
                               PopoutType.TOOLBAR : PopoutType.STAND_ALONE,
             groupName, false);
    }

    public MiniPlotWidget(PopoutType pType, String groupName, boolean fullControl) {
        super(choosePopoutType(pType,fullControl), MIN_WIDTH,MIN_HEIGHT);
        updateUISelectedLook();
        _fullControl= fullControl;
        setPopoutWidget(_panel);
        _panel.addStyleName("mpw-popout-panel");
        _group= (groupName==null) ? PlotWidgetGroup.makeSingleUse() : PlotWidgetGroup.getShared(groupName);
        _group.addMiniPlotWidget(this);

        Image im= new Image(IconCreator.Creator.getInstance().getToolsIcon());
        Widget toolsButton= GwtUtil.makeImageButton(im,"Show tools for more image operations",new ClickHandler() {
            public void onClick(ClickEvent event) {
                AllPlots.getInstance().setSelectedWidget(MiniPlotWidget.this, true);
            }
        });
        addToolbarButton(toolsButton,24);
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    private static PopoutContainer choosePopoutType(PopoutType ptype, boolean fullControl) {
        PopoutContainer retval= null;
        switch (ptype) {
            case TOOLBAR:     retval= new PopupContainerForToolbar(); break;
            case STAND_ALONE: retval= new PopupContainerForStandAlone(fullControl); break;
            default: WebAssert.argTst(false, "Don't know this Type"); break;
        }
        return retval;
    }

    public void notifyWidgetShowing() {
        Vis.init(this, new Vis.InitComplete()  {
            public void done() {
                if (_plotView!=null) _plotView.notifyWidgetShowing();
            }
        });

    }

    boolean isFullControl() { return _fullControl;}
    public void addRequestMod(String key, String value)  { _reqMods.put(key,value);  }

    public void setPreferenceColorKey(String name) { _preferenceColorKey = name;}
    public void setPreferenceZoomKey(String name) { _preferenceZoomKey = name;}

    public PlotWidgetOps getOps() {
        Vis.assertInitialized();
        if (_ops==null) _ops= new PlotWidgetOps(MiniPlotWidget.this);
        return _ops;
    }

    public void getOps(final OpsAsync async) {
        Vis.init(this, new Vis.InitComplete() {
            public void done() {
                async.ops(getOps());
            }
        });
    }

    public PlotWidgetGroup getGroup() { return _group;}

    public void setRemoveOldPlot(boolean remove) { _removeOldPlot= remove; }

    /**
     * New plots pick up the zoom factor of the last plot.  This will override any zoom settings in the WebPlotRequest.
     * @param remember true to remember the last zoom level, otherwise false
     */
    public void setRememberZoom(boolean remember) { _rememberZoom= remember; }

    public void forcePlotPrefUpdate() {
        _colorPrefs.setKey(_preferenceColorKey);
        _zoomPrefs.setKey(_preferenceZoomKey);
        _colorPrefs.saveState();
        if (!isExpanded()) _zoomPrefs.saveState();
    }

    public Panel makeFixedSizeContainer(int width, int height, boolean decorated) {

        SimplePanel panel= new SimplePanel();
        panel.setWidget(this);
        panel.setPixelSize(width,height);

        Panel retval= panel;

        if (decorated) {
            DecoratorPanel dp= new DecoratorPanel();
            dp.setWidget(panel);
            retval= dp;
        }
        return retval;

    }

    public Panel makeFailureMessage(String message, int width, int height, boolean decorated) {
        VerticalPanel panel= new VerticalPanel();
        HTML header = new HTML(getTitle());
        Widget msg = GwtUtil.centerAlign(new HTML(message));
        header.addStyleName("preview-title");
        GwtUtil.setStyle(msg, "padding", "5px");
        panel.add(header);
        panel.add(msg);
        panel.setPixelSize(width, height);

        Panel retval= panel;

        if (decorated) {
            DecoratorPanel dp= new DecoratorPanel();
            dp.setWidget(panel);
            retval= dp;
        }
        return retval;
    }

    public void setActive(boolean active) {
        _active= active;
        AllPlots.getInstance().setStatus(this, active ?  AllPlots.PopoutStatus.Enabled : AllPlots.PopoutStatus.Disabled);

        if (active) {
            Vis.init(this, new Vis.InitComplete() {
                public void done() {
                    Timer t= new Timer() {
                        @Override
                        public void run() {
                            _plotView.refreshDisplay();
                        }
                    };
                    t.schedule(1000);
                }
            });
        }

    }
    public boolean isActive() { return _active; }

    public void dispose() { _plotView.clearAllPlots(); }

    public void setBoxSelection(boolean boxSelection ) {
        _boxSelection= boxSelection;
    }

    public void setHideTitleDetail(boolean hide) {
        _hideTitleDetail = hide;
    }
    public boolean getHideTitleDetail() { return _hideTitleDetail; }

    public void setSaveImageCornersAfterPlot(boolean save) {_saveCorners= save;}
    public void setImageSelection(boolean sel) { _allowImageSelect= sel; }
    public void setCatalogButtonEnable(boolean catalogButtonEnable) { _catalogButton= catalogButtonEnable; }

    public boolean isImageSelection() { return _allowImageSelect; }
    public boolean isLockImage() { return _allowImageSelect && _allowImageLock; }
    public boolean isCatalogButtonEnabled() { return _catalogButton; }

    public void setLockImage(boolean lock) { _allowImageLock= lock; }

    public void setShowScrollBars(boolean show) {
        Vis.assertInitialized();
        if (isInit()) {
            _plotView.setScrollBarsEnabled(_showScrollBars || super.isExpanded());
        }
        _showScrollBars= show;
    }

    boolean getShowScrollBars() { return _showScrollBars; }
    public void setWorkingMsg(String workingMsg)  {_workingMsg= workingMsg;}

    public void setErrorDisplayHandler(PlotError plotError) { _plotError= plotError; }


    public void setIsCollapsible(boolean collapsible) {
        super.setIsCollapsible(collapsible);
        if (!collapsible) {
            // setup time panels here
        }
    }



    void showImageSelectDialog() {
        Vis.init(this, new Vis.InitComplete() {
            public void done() {
                GeneralCommand cmd= AllPlots.getInstance().getCommand(ImageSelectCmd.CommandName);
                if (cmd!=null) cmd.execute();
            }
        });
    }


    public boolean isPlotShowing()         { return _plotView!=null && _plotView.getPrimaryPlot()!=null; }
    public WebPlot getCurrentPlot()         { return _plotView!=null ? _plotView.getPrimaryPlot() : null; }

    /**
     * Get the WebPlotView object that show the plots
     * This will return null until the first plot request is completes
     * @return the WebPlotView
     */
    public WebPlotView getPlotView()         {
        Vis.assertInitialized();
        return _plotView;
    }

    public void recallScrollPos() {
        if (_plotView==null) return;
        Vis.init(this, new Vis.InitComplete() {
            public void done() {
                _plotView.recallScrollPos();
            }
        });
    }

    public boolean contains(WebPlot p) { return _plotView!=null && _plotView.contains(p); }
    public Widget getMaskWidget() {
        Widget retval= null;
        if (_plotView==null || !GwtUtil.isOnDisplay(_plotView) ) {
            for(Widget w=_panel; (w!=null); w= w.getParent() ) {
                if (GwtUtil.isOnDisplay(w)) {
                    retval= w;
                    break;
                }
            }
        }
        else {
            retval= _plotView.getMaskWidget();
        }
        return retval;
    }

    public void setRotateNorth(boolean rotate) {
        _userModifiedRotate= true;
        checkAndDoRotate(rotate);
    }


    private boolean isRotatablePlot() { return getCurrentPlot()!=null && getCurrentPlot().isRotatable(); }

    public void setSelectionBarVisible(boolean visible) { _panel.setSelMBarVisible(visible); }
    public void setFlipBarVisible(boolean visible) { _panel.setFlipMBarVisible(visible); }

    public void setShowInlineTitle(boolean show) {
        _showInlineTitle= show;
        _plotPanel.setTitleIsAd(false);
        if (_showAd) {
            _plotPanel.setShowInlineTitle(true);
            if (show) {
                _plotPanel.updateInLineTitle(getTitleLabelHTML());
            }
            else {
                _plotPanel.setTitleIsAd(true);
                _plotPanel.updateInLineTitle(_addText);
            }
        }
        else {
            _plotPanel.setShowInlineTitle(show);
        }

    }

    public void setAutoTearDown(boolean autoTearDown) { _autoTearDown= autoTearDown; }
    public boolean isAutoTearDown() { return _autoTearDown; }

    @Override
    public String getTitleLabelHTML() {
        //todo: what happen if called in non-expanded view mode?
        String p= "";
        String s= "";
        if (getExpandedTitle()!=null) p= getExpandedTitle();
        if (getSecondaryTitle()!=null) s= getSecondaryTitle();
        return p+s;
    }

//=======================================================================
//-------------- public plotting methods --------------------------------
//=======================================================================




    void initAndPlot(final WebPlotRequest r1,
                     final WebPlotRequest r2,
                     final WebPlotRequest r3,
                     final boolean threeColor,
                     final boolean addToHistory,
                     final boolean enableMods,
                     final AsyncCallback<WebPlot> notify) {

        Vis.init(this, new Vis.InitComplete() {
            public void done() {
                if (_showTitle) {
                    setTitle(findTitle(r1, r2, r3));
                    setExpandedTitle(findExpandedTitle(r1, r2, r3));
                }

                if (_removeOldPlot && _plotTask != null && !_plotTask.isFinish()) {
                    _plotTask.cancel();
                }

                setClientSideRequestOptions(r1, r2, r3);

                WebPlotRequest modR1= modifyRequest(r1, threeColor ? Band.RED : Band.NO_BAND,enableMods);
                WebPlotRequest modR2= modifyRequest(r2, Band.GREEN,enableMods);
                WebPlotRequest modR3= modifyRequest(r3, Band.BLUE,enableMods);

                if (enableMods) {
                    _reqMods.clear();
                }
                else {
                    _rotateNorth = false;
                }

                _plotTask = VisTask.getInstance().plot(modR1, modR2, modR3,
                                                       threeColor, _workingMsg,
                                                       _removeOldPlot, addToHistory, notify,
                                                       MiniPlotWidget.this);
            }
        });
    }

    private void setClientSideRequestOptions(WebPlotRequest... rAry) {
        for (WebPlotRequest r : rAry) {
            if (r!=null) {
                if (r.containsParam(WebPlotRequest.PREFERENCE_COLOR_KEY)) {
                    setPreferenceColorKey(r.getPreferenceColorKey());
                }
                if (r.containsParam(WebPlotRequest.PREFERENCE_ZOOM_KEY)) {
                    setPreferenceZoomKey(r.getPreferenceZoomKey());
                }
                if (r.containsParam(WebPlotRequest.SHOW_TITLE_AREA)) {
                    setTitleAreaAlwaysHidden(!r.getShowTitleArea());
                }
                if (r.containsParam(WebPlotRequest.SAVE_CORNERS)) {
                    setSaveImageCornersAfterPlot(r.getSaveCorners());
                }
                if (r.getRotateNorthSuggestion() && !_userModifiedRotate) {
                    r.setRotateNorth(true);
                }
                if (r.containsParam(WebPlotRequest.SHOW_SCROLL_BARS)) {
                    setShowScrollBars(r.getShowScrollBars());
                }
                if (r.containsParam(WebPlotRequest.ALLOW_IMAGE_SELECTION)) {
                    setImageSelection(r.isAllowImageSelection());
                }
                if (r.containsParam(WebPlotRequest.HIDE_TITLE_DETAIL)) {
                    setHideTitleDetail(r.getHideTitleDetail());
                }
                if (r.containsParam(WebPlotRequest.ADVERTISE) && r.isAdvertise()) {
                    _showAd= true;
                    _plotPanel.setShowInlineTitle(true);
                    _plotPanel.updateInLineTitle(_addText);
                    _plotPanel.setTitleIsAd(true);
                }
                else {
                    _showAd= false;
                    _plotPanel.setShowInlineTitle(false);
                    _plotPanel.updateInLineTitle(getTitleLabelHTML());
                    _plotPanel.setTitleIsAd(false);
                }
                if (r.getRequestType()==RequestType.URL &&
                        Application.getInstance().getNetworkMode()== NetworkMode.JSONP &&
                        !Application.getInstance().getCreator().isApplication()) {
                   r.setURL(FFToolEnv.modifyURLToFull(r.getURL()));
                }
                if (r.containsParam(WebPlotRequest.GRID_ON)) {
                    _turnOnGridAfterPlot= r.getGridOn();
                }
            }
        }
    }

//    private String modifyURLToFull(String url) {
//        String retURL= url;
//        if (!StringUtils.isEmpty(url)) {
//            url= url.toLowerCase();
//            if (!url.startsWith("http") && !url.startsWith("file")) {
//                String docUrl= Document.get().getURL();
//                int lastSlash= docUrl.lastIndexOf("/");
//                if (lastSlash>-1) {
//                    String rootURL= docUrl.substring(0,lastSlash+1);
//                    retURL= rootURL+url;
//                }
//                else {
//                    retURL= docUrl+"/"+url;
//                }
//            }
//        }
//        return retURL;
//    }


    private WebPlotRequest modifyRequest(WebPlotRequest r, Band band, boolean enableMods) {

        if (r==null || !enableMods) {
            return WebPlotRequest.makeCopy(r);
        }

        WebPlotRequest retval= r.makeCopy();

        if (_rotateNorth) retval.setRotateNorth(true);

        if (_rememberZoom && _plotView.getPrimaryPlot()!=null) {
            float zFact= _plotView.getPrimaryPlot().getZoomFact();
            retval.setZoomType(ZoomType.STANDARD);
            retval.setInitialZoomLevel(zFact);
        }


        _colorPrefs.setKey(_preferenceColorKey);
        _zoomPrefs.setKey(_preferenceZoomKey);

        if (isUsingColorPrefs()) {
            if (_colorPrefs.getRangeValues(band)!=null) retval.setInitialRangeValues(_colorPrefs.getRangeValues(band));
            retval.setInitialColorTable(_colorPrefs.getColorTableId());
        }

        if (isUsingZoomPrefs()) {
            retval.setInitialZoomLevel(_zoomPrefs.getZoomLevel());
        }

        for(Map.Entry<String,String> entry : _reqMods.entrySet()) {
            retval.setParam(new Param(entry.getKey(), entry.getValue()));
        }
        return retval;

    }

    private boolean isUsingColorPrefs() {
        return _preferenceColorKey!=null && !DISABLED.equals(_preferenceColorKey) && _colorPrefs.isPrefsAvailable();
    }

    private boolean isUsingZoomPrefs() {
        return _preferenceZoomKey!=null && !DISABLED.equals(_preferenceZoomKey) && _zoomPrefs.isPrefsAvailable();
    }

    private static String findTitle(WebPlotRequest r1,
                                    WebPlotRequest r2,
                                    WebPlotRequest r3) {
        String retval;
        if      (r1!=null) retval= r1.getTitle();
        else if (r2!=null) retval= r2.getTitle();
        else if (r3!=null) retval= r3.getTitle();
        else               retval= "";
        return retval;
    }

    private static String findExpandedTitle(WebPlotRequest r1,
                                    WebPlotRequest r2,
                                    WebPlotRequest r3) {
        String retval;
        if      (r1!=null) retval= r1.getExpandedTitle();
        else if (r2!=null) retval= r2.getExpandedTitle();
        else if (r3!=null) retval= r3.getExpandedTitle();
        else               retval= "";
        return retval;
    }

    @Override
    public String getExpandedTitle() {
        String retval = _expandedTitle;
        if (retval==null || retval.length()==0) {
            retval = getTitle();
        }
        return retval;
    }

    public void setExpandedTitle(String s) {
        _expandedTitle = s;
    }
    /**
     * Will return true after the first plot has completed or is init is called directly
     * @return true, is initialized, false, if not
     */
    public boolean isInit() { return _initialized && Drawer.isJSLoaded(); }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void checkAndDoRotate(boolean rotateNorth) {
        WebPlot plot= _plotView.getPrimaryPlot();
        if (!isRotatablePlot() || plot==null || plot.getFirstBand()==null) return;


        boolean performRotation= false;
        boolean imageAlreadyNorth=  plot.getRotationType()== PlotState.RotateType.NORTH || VisUtil.isPlotNorth(plot);

        if (rotateNorth && !imageAlreadyNorth) {
            performRotation= true;
        }
        else if (!rotateNorth && plot.isRotated()) {
            performRotation= true;
        }

        if (performRotation) VisTask.getInstance().rotateNorth(plot, rotateNorth, this);
    }


    private Dimension getMaxWinZoomSize() {
        WebPlot plot= _plotView.getPrimaryPlot();
        Dimension retval= null;
        if (plot!=null) {
            int dWidth= plot.getImageDataWidth();
            int dHeight= plot.getImageDataHeight();
            float futureZoom= getFullScreenZoomSize();
            retval= new Dimension( (int)(dWidth*futureZoom), (int)(dHeight*futureZoom));
        }
        return retval;

    }

    private float getFullScreenZoomSize() {
        return ZoomUtil.getEstimatedFullZoomFactor(_plotView.getPrimaryPlot(),
                                                   getPopoutContainer().getAvailableSize()
        );
    }

    public void widgetResized(int width, int height) { resize(); }

    @Override
    protected boolean enableToolbar() {
        return _group==null ? true : !_group.isFloatVisBar();
    }

    void clearPlotToolbar() {
        super.clearToolbar();
    }

    void initAsync(final Vis.InitComplete ic) {
        if (!_initialized) {
            _initialized= true;
            _plotView= new WebPlotView();
            _plotView.setMiniPlotWidget(MiniPlotWidget.this);
            _group.initMiniPlotWidget(MiniPlotWidget.this);
            Map<String, GeneralCommand> privateCommandMap = new HashMap<String, GeneralCommand>(7);
            WebVisInit.loadPrivateVisCommands(privateCommandMap, MiniPlotWidget.this);
            layout(privateCommandMap);
            _plotView.setMaskWidget(MiniPlotWidget.this);
            addPlotListeners();
            _colorPrefs= new PlotWidgetColorPrefs(this);
            _zoomPrefs= new PlotWidgetZoomPrefs(this,false);
        }
        else {
        }
        if (ic!=null) ic.done();
    }





    private final boolean forceIE6Layout = BrowserUtil.isBrowser(Browser.IE,6) || BrowserUtil.isBrowser(Browser.IE,7);

    private void resize() {
        if (_panel!=null) {
            _panel.onResize();
            if (_panel.isAttached() && forceIE6Layout) {
                _panel.forceLayout();
            }
        }
    }



    private void addPlotListeners() {
        _plotView.getEventManager().addListener(Name.AREA_SELECTION,
                                                new WebEventListener() {
                                                    public void eventNotify(WebEvent ev) {
                                                        resize();
                                                        if (_plotView.getPrimaryPlot() != null) {
                                                            RecSelection sel = (RecSelection) _plotView.getAttribute(WebPlot.SELECTION);
                                                            setSelectionBarVisible(sel != null);
                                                        }
                                                    }
                                                });


        _plotView.getEventManager().addListener(
                new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        Name n = ev.getName();
                        if (n == Name.PRIMARY_PLOT_CHANGE) {
                            WebPlot p = _plotView.getPrimaryPlot();
                            if (p != null) {
                                if (StringUtils.isEmpty(p.getPlotDesc())) {
                                    int idx = _plotView.indexOf(p);
                                    String title = "Frame- " + idx;
                                    _flipFrame.setHTML(title);
                                } else {
                                    _flipFrame.setHTML(p.getPlotDesc());
                                }
                            } else {
                                _flipFrame.setHTML("Frame: " + _plotView.indexOf(p));
                            }
                        }
                    }


                });

        WebPlotView.MouseAll ma= new WebPlotView.DefMouseAll() {
            @Override
            public void onMouseOver(WebPlotView pv, ScreenPt spt) {
                showToolbar(true);

            }

            @Override
            public void onMouseOut(WebPlotView pv) {
                showToolbar(false);
            }

//            @Override
//            public void onMouseMove(WebPlotView pv, ScreenPt spt) { }

            @Override
            public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
                AllPlots.getInstance().setSelectedWidget(MiniPlotWidget.this, isExpanded());
                forceExpandedUIUpdate();
                updateGridBorderStyle();
            }

            @Override
            public void onTouchStart(WebPlotView pv, ScreenPt spt, TouchStartEvent ev) {
                AllPlots.getInstance().setSelectedWidget(MiniPlotWidget.this, isExpanded());
                forceExpandedUIUpdate();
                updateGridBorderStyle();
            }

        };

        _clickTitlePanel.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                AllPlots.getInstance().setSelectedWidget(MiniPlotWidget.this, isExpanded());
            }
        });

        _plotView.addPersistentMouseInfo(new WebPlotView.MouseInfo(ma, ""));


    }


    private void layout(Map<String, GeneralCommand> privateCommandMap) {

        MenuGenerator privateMenugen= MenuGenerator.create(privateCommandMap);
        _flipFrame= new MenuItem("Frame: 0",new Command() { public void execute() { } });


        DOM.setStyleAttribute(_flipFrame.getElement(),"borderColor", "transparent");
        DOM.setStyleAttribute(_flipFrame.getElement(), "background", "transparent");

        DOM.setStyleAttribute(_flipFrame.getElement(), "paddingLeft", "25px");
        DOM.setStyleAttribute(_flipFrame.getElement(), "color", "#49a344");
        DOM.setStyleAttribute(_flipFrame.getElement(), "paddingBottom", "7px");

        MenuBar selectionMbar= privateMenugen.makeToolBarFromProp("VisSelectionMenuBar");
        MenuBar flipMbar= privateMenugen.makeToolBarFromProp("VisFlipMenuBar");


        _plotPanel= new InlineTitleLayoutPanel(this);

        flipMbar.addItem(_flipFrame);

        _panel.addNorth(_selectionMbarDisplay, TOOLBAR_SIZE);
        _panel.addNorth(_flipMbarDisplay, TOOLBAR_SIZE);
        _panel.add(_plotPanel);

        setSelectionBarVisible(false);
        setFlipBarVisible(false);

        _selectionMbarDisplay.setHTML(0,0,"<i>Options: </i>");
        _selectionMbarDisplay.setWidget(0,1,selectionMbar);


        _flipMbarDisplay.setHTML(0,0,"<i>Change Image: </i>");
        _flipMbarDisplay.setWidget(0,1,flipMbar);


        FlexTable.FlexCellFormatter fm= _selectionMbarDisplay.getFlexCellFormatter();
        fm.setColSpan(0,1,5);
        fm.setColSpan(0,0,1);

        fm= _flipMbarDisplay.getFlexCellFormatter();
        fm.setColSpan(0,1,5);
        fm.setColSpan(0,0,1);


        PlotMover plotMover= new PlotMover(_plotView);
        plotMover.disableMouseReadoutOnMove(AllPlots.getInstance().getMouseReadout());
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (_showInlineTitle) _plotPanel.updateInLineTitle(getTitleLabelHTML());
    }

    @Override
    public void setSecondaryTitle(String secondaryTitle) {
        super.setSecondaryTitle(secondaryTitle);
        if (_showInlineTitle) _plotPanel.updateInLineTitle(getTitleLabelHTML());
    }

    //======================================================================
//------------------ Package Methods                    ----------------
//------------------ should only be called by           ----------------
//------------------ by the ServerTask classes in this  ----------------
//------------------ package that are called for plotting --------------
//======================================================================

    public void hideMouseReadout() { AllPlots.getInstance().hideMouseReadout(); }

    /**
     * should only be call from the one of the plotting task classes
     */
    public void prePlotTask() {
        hideMouseReadout();
        AllPlots.getInstance().getMouseReadout().setEnabled(false);
    }

    /**
     * should only be call from the one of the plotting task classes
     * @param widgetTitle the title of the new plot
     * @param plot the new plot
     * @param notify the callback class
     */
    public void postPlotTask(String widgetTitle,
                             WebPlot plot,
                             AsyncCallback<WebPlot> notify) {
        setTitle(widgetTitle != null ? widgetTitle : "");
        postPlotTask(plot, notify);
    }


    public void postPlotTask(WebPlot plot, AsyncCallback<WebPlot> notify) {
        if (notify!=null) notify.onSuccess(plot);
        hideMouseReadout();
        if (_firstPlot)  _firstPlot= false;
        AllPlots.getInstance().getMouseReadout().setEnabled(true);
        _plotView.setScrollBarsEnabled(_showScrollBars || super.isExpandedAsOne());
        _rotateNorth= (plot.getRotationType()== PlotState.RotateType.NORTH);
        if (_turnOnGridAfterPlot) {
            GridCmd cmd= (GridCmd)AllPlots.getInstance().getCommand(GridCmd.CommandName);
            cmd.setGridEnable(this,true,false);
            _turnOnGridAfterPlot= false;
        }
        saveCorners();
    }

    void saveCorners() {
       saveCorners(getCurrentPlot());
    }

    private void saveCorners(WebPlot plot) {
        if (_saveCorners && plot!=null) {
            try {
                int w= plot.getImageDataWidth();
                int h= plot.getImageDataHeight();
                WorldPt pt1= plot.getWorldCoords(new ImagePt(0, 0));
                WorldPt pt2= plot.getWorldCoords(new ImagePt(w, 0));
                WorldPt pt3= plot.getWorldCoords(new ImagePt(w,h));
                WorldPt pt4= plot.getWorldCoords(new ImagePt(0, h));
                ActiveTarget.getInstance().setImageCorners(pt1,pt2,pt3,pt4);
            } catch (ProjectionException e) {
                // do nothing
            }
        }
    }


    public Widget getLayoutPanel() { return _panel; }


    public void processError(WebPlot wp, String briefDesc, String desc, Exception e) {
        processError(wp,briefDesc,desc,null,e);
    }

    public void processError(WebPlot wp, String briefDesc, String desc, String details, Exception e) {
        if (_plotError!=null) _plotError.onError(wp, briefDesc, desc, details, e);
    }

    void updateUISelectedLook() {
        Vis.init(new Vis.InitComplete() {
            public void done() { updateUISelectedLookAsync(); }
        });

    }

    void updateUISelectedLookAsync() {
        boolean selected= AllPlots.getInstance().getMiniPlotWidget()==this;
        boolean locked= _group!=null ? _group.getLockRelated() : false;
        Widget pv= getPlotView();

        if (selected) {
            if (isExpanded()) {
                if (pv!=null) GwtUtil.setStyle(pv, "border", "none");
                GwtUtil.setStyle(_panel, "border", "none");
            }
            else if (_boxSelection) {
                GwtUtil.setStyle(_panel, "border", "none");
                if (pv!=null) GwtUtil.setStyle(pv, "border", "2px ridge orange");
            }
            else {
//                GwtUtil.setStyle(_panel, "border", "1px solid transparent");
//                GwtUtil.setStyle(_panel, "borderTop", "2px solid "+fireflyCss.selectedColor());
//                GwtUtil.setStyle(_panel, "borderTop", "2px dashed green");
                if (pv!=null) GwtUtil.setStyle(pv, "border", "none");
                GwtUtil.setStyle(_panel, "borderTop", "2px ridge orange");
            }
        }
        else {
            if (isExpanded()) {
                if (pv!=null) GwtUtil.setStyle(pv, "border", "none");
                GwtUtil.setStyle(_panel, "border", "none");
            }
            else if (_boxSelection) {
                GwtUtil.setStyle(_panel, "border", "none");
                if (pv!=null) GwtUtil.setStyle(pv, "border", "2px solid transparent");
            }
            else {
                if (pv!=null) GwtUtil.setStyle(pv, "border", "none");
                if (locked) GwtUtil.setStyle(_panel, "borderTop", "2px groove "+fireflyCss.highlightColor());
                else        GwtUtil.setStyle(_panel, "borderTop", "2px solid transparent");
            }


        }

    }


    static void forceExpandedUIUpdate() {
        AllPlots _allPlots= AllPlots.getInstance();
        MiniPlotWidget mpw= _allPlots.getMiniPlotWidget();
        List<PopoutWidget> list= new ArrayList<PopoutWidget>(_allPlots.getAllPopouts());
        if (mpw.isExpanded())  {
            PopoutWidget.forceExpandedTitleUpdate(mpw, list);
            for (PopoutWidget popout : list) {
                if (popout.isPrimaryExpanded()) popout.updateGridBorderStyle();
            }

        }
    }


    public interface OpsAsync {
        public void ops(PlotWidgetOps widgetOps);
    }


    public interface PlotError {
        public void onError(WebPlot wp, String briefDesc, String desc, String details, Exception e);
    }


    public class DefaultPlotError implements PlotError {
        public void onError(WebPlot wp, String briefDesc, String desc, String details, Exception e) {
            PopupUtil.showError("Plot Error", desc, details);
        }
    }


    public class HiddableLayoutPanel extends DockLayoutPanel {
        public HiddableLayoutPanel(Style.Unit unit) {
            super(unit);
        }

        void setSelMBarVisible(boolean visible) {
            LayoutData data = (LayoutData) _selectionMbarDisplay.getLayoutData();
            if (data!=null) {
                if (visible) data.size= TOOLBAR_SIZE;
                else         data.size= 1;
                forceLayout();
            }
        }

        void setFlipMBarVisible(boolean visible) {
            LayoutData data = (LayoutData) _flipMbarDisplay.getLayoutData();
            if (data!=null) {
                if (visible)  data.size= TOOLBAR_SIZE;
                else          data.size= 1;
                forceLayout();
            }
        }



    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
