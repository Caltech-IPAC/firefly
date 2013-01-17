package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.caltech.ipac.firefly.commands.CenterPlotOnQueryCmd;
import edu.caltech.ipac.firefly.commands.FitsDownloadCmd;
import edu.caltech.ipac.firefly.commands.FitsHeaderCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogCmd;
import edu.caltech.ipac.firefly.commands.LayerCmd;
import edu.caltech.ipac.firefly.commands.LockImageCmd;
import edu.caltech.ipac.firefly.commands.ReadoutSideCmd;
import edu.caltech.ipac.firefly.commands.RotateNorthCmd;
import edu.caltech.ipac.firefly.commands.SelectAreaCmd;
import edu.caltech.ipac.firefly.commands.ShowColorOpsCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.IconMenuItem;
import edu.caltech.ipac.firefly.ui.PopoutControlsUI;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * @author Trey Roby
 */
public class AllPlots {


    public enum BarPopup {Inline, PopupOut}
    public enum ToolbarRows {ONE, MULTI}

    public enum PopoutStatus {Enabled, Disabled}

    private static AllPlots _instance = null;
    private static final FireflyCss css = CssData.Creator.getInstance().getFireflyCss();
    private static final NumberFormat _nf = NumberFormat.getFormat("#.#");

    private List<MiniPlotWidget> _allMpwList = new ArrayList<MiniPlotWidget>(10);
    private List<PlotWidgetGroup> _groups = new ArrayList<PlotWidgetGroup>(5);
    private List<PopoutWidget> _additionalWidgets = new ArrayList<PopoutWidget>(4);

    private Map<String, GeneralCommand> _commandMap = new HashMap<String, GeneralCommand>(13);
    private Map<PopoutWidget, PopoutStatus> _statusMap = new HashMap<PopoutWidget, PopoutStatus>(3);
    private final WebEventManager _eventManager = new WebEventManager();

    private WebMouseReadout.Side _side = WebMouseReadout.Side.Right;
    private WebMouseReadout _mouseReadout;
    private PopupPane _menuBarPopup;
//    private MenuItem _zoomLevel;
    private MenuItem _zoomLevelPopup = null;
    private Toolbar.CmdButton _toolbarLayerButton = null;
    private boolean _layerButtonAdded = false;
    private ToolbarRows _rows= ToolbarRows.ONE;
    private VerticalPanel mbarVP = new VerticalPanel();
    private VerticalPanel mbarPopBottom = new VerticalPanel();
    private Label heightControl = new Label("");
    private boolean _usingBlankPlots = false;

    private MiniPlotWidget _primarySel = null;
    private int toolPopLeftOffset= 0;

    private PopoutWidget.ExpandUseType _defaultExpandUseType = PopoutWidget.ExpandUseType.GROUP;


    private boolean _initialized = false;
    private MPWListener _pvListener;

    private HTML _toolbarTitle = new HTML();

    private final CheckBox _lockCB = GwtUtil.makeCheckBox("Lock related", "Lock images of all bands for zooming, scrolling, etc", false);


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    private AllPlots() {
        WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_START, new TearDownListen());
        setDefaultReadoutSide(WebMouseReadout.Side.Right);
        PopoutWidget.setExpandBehavior(new ExpandBehavior());

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) { toolbarResizeCheck(); }
        });
    }


    public static AllPlots getInstance() {
        if (_instance == null) _instance = new AllPlots();
        return _instance;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//    public void setToolbarRows(ToolbarRows rows) {
//       _rows =  rows;
//    }

    public void setToolPopLeftOffset(int offset) {
        this.toolPopLeftOffset= offset;
    }


    public PlotWidgetGroup getGroup(MiniPlotWidget mpw) {
        PlotWidgetGroup retval = null;
        if (_groups != null) {
            for (PlotWidgetGroup g : _groups) {
                if (g.contains(mpw)) {
                    retval = g;
                    break;
                }
            }
        }
        return retval;
    }


    public PlotWidgetGroup getGroup(String groupName) {
        PlotWidgetGroup retval = null;
        if (_groups != null && groupName != null) {
            for (PlotWidgetGroup g : _groups) {
                if (g.getName().equals(groupName)) {
                    retval = g;
                    break;
                }
            }
        }
        return retval;
    }


    public PlotWidgetGroup getActiveGroup() {
        return getGroup(_primarySel);
    }

    public void tearDownPlots() {

        fireTearDown();
        _primarySel = null;
        _lockCB.setValue(false);
        List<PlotWidgetGroup> l = new ArrayList(_groups);
        for (PlotWidgetGroup g : l) g.autoTearDownPlots();

        _additionalWidgets.clear();
        _statusMap.clear();


        MiniPlotWidget newSelected = null;
        for (PlotWidgetGroup g : _groups) {
            for (MiniPlotWidget mpw : g) {
                if (mpw != null) {
                    newSelected = mpw;
                    break;
                }
            }
            if (newSelected != null) break;
        }

        if (newSelected != null) {
            setSelectedWidget(newSelected);
        } else {
            firePlotWidgetChange(null);
        }
        removeLayerButton();
    }


    public void setDefaultExpandUseType(PopoutWidget.ExpandUseType useType) {
        _defaultExpandUseType = useType;
    }

    public void setDefaultTiledTitle(String title) {
        PopoutControlsUI.setTitledTitle(title);
    }

    public PopoutWidget.ExpandUseType getDefaultExpandUseType() {
        return _defaultExpandUseType;
    }

    public WebEventManager getEventManager() {
        return _eventManager;
    }

    /**
     * add a new MiniPlotWidget.
     * don't call this method until MiniPlotWidget.getPlotView() will return a non-null value
     *
     * @param mpw the MiniPlotWidget to add
     */
    void addMiniPlotWidget(MiniPlotWidget mpw) {
        _allMpwList.add(mpw);
        _primarySel = mpw;

        if (!_groups.contains(mpw.getGroup())) _groups.add(mpw.getGroup());

        init();

        WebPlotView pv = mpw.getPlotView();
        pv.getEventManager().addListener(_pvListener);
        _mouseReadout.addPlotView(pv);
        fireAdded(mpw);
        updateVisibleWidgets();
    }

    private void addLayerButton() {

        if (!_layerButtonAdded && Application.getInstance().getToolBar() != null) {
            LayerCmd cmd = (LayerCmd) _commandMap.get(LayerCmd.CommandName);
            if (cmd != null && _toolbarLayerButton == null) {
                _toolbarLayerButton = new Toolbar.CmdButton("Plot Layers", "Plot Layers",
                                                            "Control layers on the plot", cmd);
            }
            Application.getInstance().getToolBar().addButton(_toolbarLayerButton);
            _layerButtonAdded = true;
        }
    }

    private void removeLayerButton() {
        if (_layerButtonAdded && Application.getInstance().getToolBar() != null) {
            Application.getInstance().getToolBar().removeButton(_toolbarLayerButton.getName());
            _layerButtonAdded = false;
        }
    }

    /**
     * remove the MiniPlotWidget. For efficiency does not choose a now selected widget. You should set the selected widget
     * after calling this method
     *
     * @param mpw the MiniPlotWidget to remove
     */
    void removeMiniPlotWidget(MiniPlotWidget mpw) {
        _allMpwList.remove(mpw);
        WebPlotView pv = mpw.getPlotView();
        if (pv != null) {
            pv.getEventManager().removeListener(_pvListener);
            _mouseReadout.removePlotView(pv);
        }
        PlotWidgetGroup group = mpw.getGroup();
        if (_groups.contains(group) && group.size() == 0) {
            _groups.remove(group);
        }
        fireRemoved(mpw);
    }


//    public Map<String, GeneralCommand> getCommandMap() { return _commandMap;}

    public GeneralCommand getCommand(String name) {
        return _commandMap.get(name);
    }


    public void setDefaultReadoutSide(WebMouseReadout.Side side) {
        ReadoutSideCmd cmd = (ReadoutSideCmd) _commandMap.get(ReadoutSideCmd.CommandName);
        _side = side;
        if (cmd != null) cmd.setReadoutSide(side);
    }

    /**
     * Get the WebPlotView object that show the plots
     * This will return null until the first plot request is completes
     *
     * @return the WebPlotView
     */
    public WebPlotView getPlotView() {
        Vis.assertInitialized();
        return _primarySel != null ? _primarySel.getPlotView() : null;
    }

    public MiniPlotWidget getMiniPlotWidget() {
        return _primarySel;
    }

    public boolean getGroupContainsSelection(PlotWidgetGroup group) {
        boolean retval = false;
        for (MiniPlotWidget mpw : group.getAllActive()) {
            if (mpw == _primarySel) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public List<PopoutWidget> getAllPopouts() {
        List<PopoutWidget> retval =
                new ArrayList<PopoutWidget>(_allMpwList.size() + _additionalWidgets.size());
        for (MiniPlotWidget mpw : _allMpwList) {
            if (_statusMap.get(mpw) != PopoutStatus.Disabled) {
                retval.add(mpw);
            }
        }
        for (PopoutWidget popout : _additionalWidgets) {
            if (_statusMap.get(popout) != PopoutStatus.Disabled) {
                retval.add(popout);
            }
        }
        return retval;
    }

    public List<PopoutWidget> getAdditionalPopoutList() {
        List<PopoutWidget> retval =
                new ArrayList<PopoutWidget>(_additionalWidgets.size());
        for (PopoutWidget popout : _additionalWidgets) {
            if (_statusMap.get(popout) != PopoutStatus.Disabled) {
                retval.add(popout);
            }
        }
        return retval;
    }

    public List<MiniPlotWidget> getAll() {
        List<MiniPlotWidget> retval = new ArrayList<MiniPlotWidget>(_allMpwList.size());
        for (MiniPlotWidget mpw : _allMpwList) {
            if (_statusMap.get(mpw) != PopoutStatus.Disabled) {
                retval.add(mpw);
            }
        }
        return retval;
    }

    public WebMouseReadout getMouseReadout() {
        return _mouseReadout;
    }

    public List<MiniPlotWidget> getActiveList() {
        return getActiveGroupList(true);
    }

    public List<MiniPlotWidget> getActiveGroupList(boolean ignoreUninitialized) {
        PlotWidgetGroup group = getActiveGroup();
        List<MiniPlotWidget> retval;
        if (group == null) {
            retval = Collections.emptyList();
        } else {
            if (group.getLockRelated()) {
                retval = ignoreUninitialized ? group.getAllActive() : group.getAll();
            } else {
                AllPlots.getInstance().getActiveGroup();
                retval = Arrays.asList(AllPlots.getInstance().getMiniPlotWidget());
            }
        }
        return retval;
    }

    void updateUISelectedLook() {
        for (MiniPlotWidget mpw : _allMpwList) {
            mpw.updateUISelectedLook();
        }
    }

    public List<MiniPlotWidget> getGroupListWith(MiniPlotWidget mpw) {
        PlotWidgetGroup group = getGroup(mpw);
        List<MiniPlotWidget> retval;
        if (group == null) {
            retval = Collections.emptyList();
        } else if (!getGroupContainsSelection(group)) {
            retval = Collections.emptyList();
        } else {
            if (group.getLockRelated()) {
                retval = group.getAllActive();
            } else {
                retval = Arrays.asList(getMiniPlotWidget());
            }
        }
        return retval;
    }

    public void setStatus(PopoutWidget popout, PopoutStatus status) {
        if (status == PopoutStatus.Disabled) {
            _statusMap.put(popout, status);
            if (popout instanceof MiniPlotWidget && popout == _primarySel) {
                findNewSelected();
            }
        } else if (_statusMap.containsKey(popout)) {
            _statusMap.remove(popout);
        }
    }


    public void registerPopout(PopoutWidget popout) {
        _additionalWidgets.add(popout);
    }

    public void deregisterPopout(PopoutWidget popout) {
        if (_additionalWidgets.contains(popout)) {
            _additionalWidgets.remove(popout);
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private void toolbarResizeCheck() {
        updateMbarLayout();
    }

    private void findNewSelected() {
        if (_statusMap.containsKey(_primarySel) && _statusMap.get(_primarySel) == PopoutStatus.Disabled) {
            PlotWidgetGroup badGroup = _primarySel.getGroup();
            MiniPlotWidget firstChoice = null;
            MiniPlotWidget secondChoice = null;
            for (MiniPlotWidget mpw : getAll()) {
                if (mpw.getGroup().size() > 1 && mpw.getGroup() != badGroup) {
                    firstChoice = mpw;
                    break;
                }
                secondChoice = mpw;
            }
            setSelectedWidget(firstChoice != null ? firstChoice : secondChoice);
        }
    }

    private void init() {
        if (!_initialized) {
            _initialized = true;
            _pvListener = new MPWListener();
            WebVisInit.loadSharedVisCommands(_commandMap);
            layout();
            setDefaultReadoutSide(_side);
        }
    }


    private void updateTitleFeedback() {
        MiniPlotWidget mpwPrim = getMiniPlotWidget();

        for (MiniPlotWidget mpwItem : getAll()) {
            if (mpwItem.getPlotView() != null) {
                WebPlot p = mpwItem.getPlotView().getPrimaryPlot();
                String val;
                if (p != null && !mpwItem.getHideTitleDetail()) {
                    val = ZoomUtil.convertZoomToString(p.getZoomFact());

                    if (p.isRotated()) {
                        if (p.getRotationType() == PlotState.RotateType.NORTH) {
                            val += ", North";
                        } else {
                            val += ", " + _nf.format(p.getRotationAngle()) + "&#176;";
                        }
                    }

                    if (mpwItem == mpwPrim) {
                        if (_zoomLevelPopup != null) _zoomLevelPopup.setHTML(val);
                    }

                    String span = "&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"color: #49a344;\">" + val;
                    if (mpwItem.getPlotView().isTaskWorking()) {
                        span += "&nbsp;&nbsp;&nbsp;<img style=\"width:10px;height:10px;\" src=\"" + GwtUtil.LOADING_ICON_URL + "\" >";
                    }
                    span += "</span>";
                    mpwItem.setSecondaryTitle(span);
                }
                else {
                    mpwItem.setSecondaryTitle("");
                }
            }
        }
        MiniPlotWidget.forceExpandedUIUpdate();
    }

    private boolean isFullControl() {
        return _allMpwList.size()>0 ? _allMpwList.get(0).isFullControl() : false;
    }

    private void layout() {



        _lockCB.setStyleName("groupLock");

        _lockCB.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                getActiveGroup().setLockRelated(_lockCB.getValue());
            }
        });
        _menuBarPopup = new PopupPane("Tools", null, PopupType.STANDARD, false, false, false, isFullControl()? PopupPane.HeaderType.NONE :PopupPane.HeaderType.SIDE) {
            @Override
            protected void onClose() {
                updateUISelectedLook();
                getEventManager().fireEvent(new WebEvent<Boolean>(this,Name.VIS_MENU_BAR_POP_SHOWING, false));
            }
        };
        _menuBarPopup.setAnimationEnabled(true);
        _menuBarPopup.setRolldownAnimation(true);
        _menuBarPopup.setAnimationDurration(300);
        _menuBarPopup.setAnimateDown(true);
        _mouseReadout = new WebMouseReadout();
        _mouseReadout.setDisplayMode(WebMouseReadout.DisplayMode.Group);
        _mouseReadout.setDisplaySide(WebMouseReadout.Side.Right);
        setupFloatPopupMbarType();


    }

    private void setupFloatPopupMbarType() {
        mbarPopBottom.add(_lockCB);
        mbarPopBottom.add(_toolbarTitle);
        mbarPopBottom.setWidth("100%");
        mbarPopBottom.setCellHorizontalAlignment(_lockCB, HasHorizontalAlignment.ALIGN_RIGHT);

        HorizontalPanel mbarHP = new HorizontalPanel();
        mbarHP.add(heightControl);
        mbarHP.add(mbarVP);


        GwtUtil.setStyles(_toolbarTitle, "fontSize", "9pt",
                          "padding", "0 5px 0 5px");


        GwtUtil.setStyle(_lockCB, "paddingRight", "3px");

        _lockCB.setText("Lock images of all bands for zooming, scrolling, etc");
        _menuBarPopup.setWidget(mbarHP);
        _menuBarPopup.setHeader("Visualization Tools");

        updateMbarLayout();
        mbarVP.addDomHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                _mouseReadout.suggestHideMouseReadout();
            }
        }, MouseOverEvent.getType());

    }


    private void updateMbarLayout() {

        MenuGenerator menuGen = MenuGenerator.create(_commandMap,true);

        MenuBar mbarHor;
        MenuBar mbarHor2= null;

        _rows= (Window.getClientWidth()>1280+toolPopLeftOffset) ? ToolbarRows.ONE : ToolbarRows.MULTI;

        mbarVP.clear();
        if (_rows==ToolbarRows.ONE) {
            mbarHor = menuGen.makeToolBarFromProp("VisMenuBar.all", new PopupMenubar(), false, true, true);
            mbarVP.add(mbarHor);
            _toolbarTitle.setWidth("500px");
            if (isFullControl()) heightControl.setHeight("0px");
        }
        else {
            mbarHor = menuGen.makeToolBarFromProp("VisMenuBar.row1", new PopupMenubar(), false, true, true);
            mbarHor2 = menuGen.makeToolBarFromProp("VisMenuBar.row2", new PopupMenubar(), false, true, true);
            mbarVP.add(mbarHor);
            mbarVP.add(mbarHor2);
            _toolbarTitle.setWidth("300px");
            if (isFullControl()) {
                heightControl.setHeight(_mouseReadout.getContentHeight()+ "px");
            }
        }

        mbarHor.addItem(makeHelp(BarPopup.PopupOut));

        GwtUtil.setStyles(mbarHor, "border", "none",
                                   "background", "transparent");
        if (mbarHor2!=null) {
            GwtUtil.setStyles(mbarHor2, "border", "none",
                                        "background", "transparent");
        }

        mbarVP.add(mbarPopBottom);
        if (_menuBarPopup.isVisible()) _menuBarPopup.internalResized();
        updateVisibleWidgets();
    }

    void updateVisibleWidgets() {

        PlotWidgetGroup group = getActiveGroup();
        GwtUtil.setHidden(_lockCB, group.getAllActive().size() < 2);
        _lockCB.setValue(group.getLockRelated());
        if (!GwtUtil.isOnDisplay(_menuBarPopup.getPopupPanel())) updateToolbarAlignment();

        MiniPlotWidget mpw = getMiniPlotWidget();
        if (mpw.getCurrentPlot()!=null) {
            setCommandHidden(!mpw.isImageSelection(),       ImageSelectCmd.CommandName);
            setCommandHidden(!mpw.isLockImage(),            LockImageCmd.CommandName);
            setCommandHidden(!mpw.isCatalogButtonEnabled(), IrsaCatalogCmd.CommandName);

            if (!_usingBlankPlots) _usingBlankPlots= mpw.getCurrentPlot().isBlankImage();
            if (_usingBlankPlots) {
                setupBlankCommands();
            }

        }

    }


    private void setCommandHidden(boolean hidden, String... cmdName) {
        for(String name : cmdName) {
            GeneralCommand c=  _commandMap.get(name);
            if (c!=null) c.setHidden(hidden);
        }
    }

    private void setupBlankCommands() {
        MiniPlotWidget mpw = getMiniPlotWidget();
        if (mpw.getCurrentPlot()!=null) {
            boolean hide= mpw.getCurrentPlot().isBlankImage();
            setCommandHidden(hide,
                             SelectAreaCmd.CommandName, FitsHeaderCmd.CommandName,
                             FitsDownloadCmd.CommandName,
                             WebVisInit.ColorTable.CommandName, WebVisInit.Stretch.CommandName,
                             RotateNorthCmd.CommandName, CenterPlotOnQueryCmd.CommandName,
                             ShowColorOpsCmd.COMMAND_NAME   );

        }

    }

    void fireRemoved(MiniPlotWidget mpw) {
        _eventManager.fireEvent(new WebEvent<MiniPlotWidget>(this, Name.FITS_VIEWER_REMOVED, mpw));
    }

    void fireAdded(MiniPlotWidget mpw) {
        _eventManager.fireEvent(new WebEvent<MiniPlotWidget>(this, Name.FITS_VIEWER_ADDED, mpw));
    }

    void fireTearDown() {
        _eventManager.fireEvent(new WebEvent<AllPlots>(this, Name.ALL_FITS_VIEWERS_TEARDOWN, this));
    }

    void firePlotWidgetChange(MiniPlotWidget mpw) {
        _eventManager.fireEvent(new WebEvent<MiniPlotWidget>(this, Name.FITS_VIEWER_CHANGE, mpw));
    }

    public void fireAllPlotTasksCompleted() {
        _eventManager.fireEvent(new WebEvent<MiniPlotWidget>(this, Name.ALL_PLOT_TASKS_COMPLETE));
    }

    public void setSelectedWidget(final MiniPlotWidget mpw) {
        if (mpw != null && mpw.isInit()) {
            Vis.init(new Vis.InitComplete() {
                public void done() {
                    setSelectedWidget(mpw, false);
                }
            });
        }
    }

    public void setSelectedWidget(MiniPlotWidget mpw, boolean toggleShowMenuBar) {
        setSelectedWidget(mpw, false, toggleShowMenuBar);
    }

    public void setSelectedWidget(MiniPlotWidget mpw, boolean force, boolean toggleShowMenuBar) {
        if (!force && mpw == _primarySel && _menuBarPopup.isVisible() && !mpw.isExpanded()) {
            if (_menuBarPopup.isVisible() && toggleShowMenuBar) toggleShowMenuBarPopup(mpw);
            return;
        }
        _primarySel = mpw;
        _primarySel.saveCorners();
        updateUISelectedLook();


        updateToolbarAlignment();
        if (toggleShowMenuBar) toggleShowMenuBarPopup(mpw);
        firePlotWidgetChange(mpw);
        updateTitleFeedback();
        updateVisibleWidgets();
        setPlotTitleToMenuBar();
    }


    private void setPlotTitleToMenuBar() {
        if (_primarySel.getTitle() != null) {
            _toolbarTitle.setHTML(_primarySel != null ? "<b>" + _primarySel.getTitle() + "</b>" : "");
        } else {
            _toolbarTitle.setHTML("");
        }
    }

    public void toggleShowMenuBarPopup(MiniPlotWidget mpw) {
        if (mpw != null) {
            if (_menuBarPopup.isVisible() && !mpw.isExpanded()) _menuBarPopup.hide();
            else showMenuBarPopup();
        }
    }

    public void hideMenuBarPopup() {
        _menuBarPopup.hide();
        getEventManager().fireEvent(new WebEvent<Boolean>(this,Name.VIS_MENU_BAR_POP_SHOWING, false));
    }

    public void showMenuBarPopup() {
        if (!GwtUtil.isOnDisplay(_menuBarPopup.getPopupPanel())) updateToolbarAlignment();
        _menuBarPopup.show();
        getEventManager().fireEvent(new WebEvent<Boolean>(this,Name.VIS_MENU_BAR_POP_SHOWING, true));
    }

    public boolean isMenuBarPopupVisible() { return _menuBarPopup.isVisible(); }

    public void setMenuBarPopupPersistent(boolean p) { _menuBarPopup.setDoRegionChangeHide(!p); }

    public PopupPane getMenuBarPopup() { return _menuBarPopup; }


    void updateToolbarAlignment() {
        if (Window.getClientWidth() > 1220+toolPopLeftOffset && Application.getInstance().getCreator().isApplication()) {
            _menuBarPopup.alignTo(RootPanel.get(), PopupPane.Align.TOP_LEFT, 130+toolPopLeftOffset, 0);
        } else {
            _menuBarPopup.alignTo(RootPanel.get(), PopupPane.Align.TOP_LEFT, toolPopLeftOffset, 0);
        }
    }



    private MenuItem makeHelp(BarPopup popType) {
        IconMenuItem help = new IconMenuItem(HelpManager.makeHelpImage(),
                                             new Command() {
                                                 public void execute() {
                                                     Application.getInstance().getHelpManager().showHelpAt("visualization.fitsViewer");
                                                 }
                                             }, false);
        help.setTitle("Help on FITS visualization");
        if (popType == BarPopup.PopupOut) {
            GwtUtil.setStyles(help.getElement(), "paddingLeft", "7px",
                              "paddingBottom", "3px",
                              "borderColor", "transparent");
        } else {
            GwtUtil.setStyles(help.getElement(),
                              "padding", "0 0 2px 10px",
                              "borderColor", "transparent");

        }
        return help;
    }


//======================================================================
//------------------ Package Methods                    ----------------
//------------------ should only be called by           ----------------
//------------------ by the ServerTask classes in this  ----------------
//------------------ package that are called for plotting --------------
//======================================================================

    void hideMouseReadout() {
        _mouseReadout.hideMouseReadout();
        DeferredCommand.addPause();
        DeferredCommand.addPause();
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                _mouseReadout.hideMouseReadout();
            }
        });


    }

    private class MPWListener implements WebEventListener {

        public void eventNotify(WebEvent ev) {
            Name n = ev.getName();
            if (n == Name.REPLOT) {
                ReplotDetails details = (ReplotDetails) ev.getData();
                if (details.getReplotReason() == ReplotDetails.Reason.IMAGE_RELOADED ||
                        details.getReplotReason() == ReplotDetails.Reason.ZOOM) {
                    updateTitleFeedback();
                }
                addLayerButton();
            } else if (n == Name.PLOT_ADDED || n == Name.PLOT_REMOVED || n == Name.PRIMARY_PLOT_CHANGE) {
                updateTitleFeedback();
            } else if (n == Name.PLOT_TASK_WORKING || n == Name.PLOT_TASK_COMPLETE) {
                updateTitleFeedback();
            }
            _eventManager.fireEvent(ev);
        }
    }


    public class TearDownListen implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            tearDownPlots();
        }
    }

    private class PopupMenubar extends MenuBar {


        private MenuItem _selected;

        public PopupMenubar() {
            super(false);

            addHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent event) {
                    setPlotTitleToMenuBar();
                    setHighlight(false);
                    _selected = null;
                }
            }, MouseOutEvent.getType());

            addDomHandler(new MouseDownHandler() {
                public void onMouseDown(MouseDownEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(true);
                }
            }, MouseDownEvent.getType());

            addDomHandler(new MouseUpHandler() {
                public void onMouseUp(MouseUpEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(false);
                }
            }, MouseUpEvent.getType());

            addDomHandler(new TouchStartHandler() {
                public void onTouchStart(TouchStartEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(true);
                }
            }, TouchStartEvent.getType());

            addDomHandler(new TouchEndHandler() {
                public void onTouchEnd(TouchEndEvent event) {
                    setHighlight(false);
                    _selected = getSelectedItem();
                    setHighlight(false);
                }
            }, TouchEndEvent.getType());

        }

        private void setHighlight(boolean highlight) {
            if (_selected != null) {
                DOM.setStyleAttribute(_selected.getElement(), "backgroundColor",
                                      highlight ? css.selectedColor() : "transparent");
            }
        }

        @Override
        public void onBrowserEvent(Event ev) {
            super.onBrowserEvent(ev);

            if (DOM.eventGetType(ev) == Event.ONMOUSEOVER) {
                _mouseReadout.suggestHideMouseReadout();
                setHighlight(false);
                String s = _primarySel != null ? _primarySel.getTitle() : "";
                _selected = getSelectedItem();
                if (_selected != null) s = _selected.getTitle();
                _toolbarTitle.setText(s);
            } else if (DOM.eventGetType(ev) == Event.ONCLICK) {
                setHighlight(false);
                _selected = getSelectedItem();
                setHighlight(true);
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

