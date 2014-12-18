package edu.caltech.ipac.firefly.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.VisUtil;

import java.util.Arrays;
import java.util.List;

/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * Create a widget that will fill the size of the container and will popout into a dialog
 * NOTE - you should never call setWidth, setHeight, or setPixelSize on this widget
 *
 * @author Trey Roby
 */
public abstract class PopoutWidget extends Composite implements RequiresResize {

//    private static WebClassProperties _prop = new WebClassProperties(PopoutWidget.class);


    public enum PopoutType {TOOLBAR,STAND_ALONE,REGION}
    public enum ViewType {UNKNOWN, GRID, ONE}
    public enum FillType {OFF,FILL,FIT,CONTEXT}

    public enum ExpandUseType {ALL, GROUP}

    public static final int DEF_MIN_WIDTH = 270;
    public static final int DEF_MIN_HEIGHT = 150;
    private int defTitleHeight = 30;
    static final int CONTROLS_HEIGHT = 40;
    static final int CONTROLS_HEIGHT_LARGE = 70;
    private static Behavior _behavior = new Behavior();
    private static boolean menuBarPermLockVisible= false;


    private ResizablePanel _movablePanel = new ResizablePanel();
    private Widget popoutWidget= null;
    private final SimplePanel _stagePanel = new SimplePanel();
    protected final FocusPanel _clickTitlePanel = new FocusPanel();
    protected final DockLayoutPanel _titlePanel = new DockLayoutPanel(Style.Unit.PX);
    private boolean _expanded;
    private String _title = "";
    private String _secondaryTitle = "";
    private final HTML _titleLbl = new HTML();
    private final HorizontalPanel _titleContainer = new HorizontalPanel();
    private CheckBox _titleCheckBox = null;
    private int toolbarWidth = 7;
    private DockLayoutPanel _expandRoot = new MyDockLayoutPanel();
    private List<PopoutWidget> _expandedList;
    private List<PopoutWidget> _originalExpandedList;
    private static ViewType _lastViewType = ViewType.ONE;
    private PopoutControlsUI _popoutUI;
    private final PopoutToolbar _toolPanel;
    private Dimension _minDim = new Dimension(DEF_MIN_WIDTH, DEF_MIN_HEIGHT);
    private boolean _lockVisible = false;
    private boolean _enableChecking = false;
    private int _titleHeight = defTitleHeight;
    private boolean _startingExpanded = false;
    private boolean _canCollapse = true;
    private boolean _widgetChecked = false;
    private boolean _supportTabs= false;
    private boolean _expandButtonAlwaysSingleView= false;
    private TabPane _tabPane= null;
    private FillType oneFillType = FillType.CONTEXT;
    private FillType gridFillType = FillType.CONTEXT;
    private boolean expansionToolbarHiding= false;

    private final PopoutContainer _expandPopout;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PopoutWidget(int minWidth, int minHeight) {
        this(choosePopoutType(false), minWidth,minHeight);
    }

    public PopoutWidget(PopoutContainer expandPopout,
                        int minWidth,
                        int minHeight) {
        initWidget(_stagePanel);
        _stagePanel.setSize("100%", "100%");
        _stagePanel.addStyleName("popout-stage-panel");
        _expandRoot.addStyleName("popout-expand-root");
        _movablePanel.addStyleName("popout-movable-panel");
        _expandPopout = expandPopout;
        _expandPopout.setPopoutWidget(this);
        _stagePanel.setWidget(_movablePanel);
        setUnexpandedStyle();
        GwtUtil.setStyle(_stagePanel, "position", "relative");
        setMinSize(minWidth, minHeight);
//        _movablePanel.setWidth("99%");
//        _movablePanel.setHeight("100%");
        super.setWidth("100%");
        super.setHeight("100%");


        _toolPanel = new PopoutToolbar(new ClickHandler() {
            public void onClick(ClickEvent ev) {
                toggleExpand();
                if (_expandButtonAlwaysSingleView) forceSwitchToOne();
            }
        },false);

        if (enableToolbar()) {
            if (!BrowserUtil.isTouchInput()) {
                _clickTitlePanel.addMouseOverHandler(new MouseOverHandler() {
                    public void onMouseOver(MouseOverEvent event) {
                        showToolbar(true);
                    }
                });


                _clickTitlePanel.addMouseOutHandler(new MouseOutHandler() {
                    public void onMouseOut(MouseOutEvent event) {
                        showToolbar(false);
                    }
                });

                _clickTitlePanel.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        showToolbar(true);
                    }
                });

                _movablePanel.addDomHandler(new MouseMoveHandler() {
                    public void onMouseMove(MouseMoveEvent event) {
                        showToolbar(true);
                    }
                }, MouseMoveEvent.getType());

                GwtUtil.setHidden(_toolPanel, true);
            }
        }



        _titleLbl.addStyleName("preview-title");

        toolbarWidth += 24;
        _titleContainer.add(_titleLbl);
        if (enableToolbar()) {
            _titlePanel.addEast(_toolPanel, toolbarWidth);
        }
        _titlePanel.add(_titleContainer);
        _titlePanel.addStyleName("popout-title-panel");


        _clickTitlePanel.setWidget(_titlePanel);
        _titlePanel.setWidth("100%");
        _titlePanel.setHeight("100%");


        _movablePanel.addNorth(_clickTitlePanel, _titleHeight);


        AllPlots.getInstance().addListener(Name.ZOOM_LEVEL_BUTTON_PUSHED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (isExpanded() && isPrimaryExpanded()) {
                    if (getViewType() == ViewType.ONE || _expandedList.size() == 1) {
                        oneFillType = FillType.OFF;
                        _behavior.setOnePlotFillStyle(oneFillType);
                    }
                    else {
                        gridFillType = FillType.OFF;
                        _behavior.setGridPlotFillStyle(gridFillType);
                    }
                }
            }
        });

        if (menuBarPermLockVisible) setLockToolbarVisible(true);

    }

    public static void setMenuBarPermLockVisible(boolean show) {
        menuBarPermLockVisible= show;
        PopoutToolbar.setAllToolbarsAlwaysVisible(show);
    }

    public void freeResources() {
        setVisible(false);
        if (_popoutUI!=null) {
            _popoutUI.freeResources();
        }
    }

    public void setExpandButtonAlwaysSingleView(boolean single) {
        _expandButtonAlwaysSingleView = single;
    }

    public boolean isPrimaryExpanded() {
        return _popoutUI != null;
    }

    public PopoutControlsUI getPopoutControlsUI() {
        return _popoutUI;
    }

    public int getToolbarWidth() { return toolbarWidth; }

    public static void forceExpandedTitleUpdate(PopoutWidget popout, List<PopoutWidget> searchList) {
        for (PopoutWidget p : searchList) {
            p.forceTitleUpdate();
            if (p._popoutUI != null) {
                p._popoutUI.updateExpandedTitle(popout);
                break;
            }
        }
    }

    public void supportTabs() {
        if (!_supportTabs) {
            _supportTabs= true;
            WebEventManager.getAppEvManager().addListener(Name.CATALOG_SEARCH_IN_PROCESS,new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    _tabPane = new TabPane();
                    _tabPane.setSize("100%", "100%");
                    _tabPane.setTabPaneName("ExpandTabView");
                    _tabPane.addTab(_expandRoot, "Plot", "Fits Viewer", false, true);
                    new NewTableEventHandler(FFToolEnv.getHub(), _tabPane);
                    WebEventManager.getAppEvManager().removeListener(Name.CATALOG_SEARCH_IN_PROCESS,this);
                    if (_expanded) _expandPopout.show();
                    DeferredCommand.addCommand(new Command() {
                        public void execute() { _tabPane.forceLayout(); }
                    });
                }
            });
        }

    }

    public Widget getToplevelExpandRoot() {
        return _tabPane==null ? _expandRoot : _tabPane;
    }

    public DockLayoutPanel getExpandRoot() {
        return _expandRoot;
    }

    public ResizablePanel getMovablePanel() {
        return _movablePanel;
    }

    public void setTitleAreaAlwaysHidden(boolean hidden) {
        _titleHeight = hidden ? 0 : defTitleHeight;
        GwtUtil.DockLayout.setWidgetChildSize(_clickTitlePanel, _titleHeight);
        _movablePanel.forceLayout();
    }

    public PopoutContainer getPopoutContainer() {
        return _expandPopout;
    }



//    public void setTitleHeight(int height) {
//        GwtUtil.DockLayout.setWidgetChildSize(_clickTitlePanel,height);
//        _movablePanel.forceLayout();
//    }

    public static void setExpandBehavior(Behavior behavior) {
        _behavior = behavior;
    }

    public void setOneFillType(FillType oneFillType) {
        if (isPrimaryExpanded()) {
            this.oneFillType = oneFillType;
            if (isExpanded()) {
                _behavior.setOnePlotFillStyle(oneFillType);
                if (isExpandedSingleView()) {
                    if (GwtUtil.isOnDisplay(_expandRoot)) {
                        int w = _expandRoot.getOffsetWidth();
                        int h = _expandRoot.getOffsetHeight();
                        int curr = _popoutUI.getVisibleIdxInOneMode();
                        PopoutWidget currW = _expandedList.get(curr);
                        _behavior.onSingleResize(currW, new Dimension(w, h), true);
                    }
                }
                else {
                   //todo: so something
                }
            }
        }
    }

    public void setGridFillType(FillType gridFillType) {
        if (isPrimaryExpanded()) {
            this.gridFillType = gridFillType;
            if (isExpanded()) {
                _behavior.setGridPlotFillStyle(gridFillType);
                if (isExpandedGridView()) {
                    if (GwtUtil.isOnDisplay(_expandRoot)) {
                        Dimension dim= _popoutUI.getGridDimension();
                        if (dim!=null)  _behavior.onGridResize(_expandedList, dim, true);
                    }
                }
                else {
                    //todo: so something
                }
            }
        }
    }

    public void setStartingExpanded(boolean startingExpanded) {
        _startingExpanded = startingExpanded;
    }

    public boolean isStartingExpanded() {
        return _startingExpanded;
    }

    public void setCanCollapse(boolean canCollapse) {
        _canCollapse= canCollapse;
    }
//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//    private Label pos= new Label();


    public void setEnableChecking(boolean enable) {
        if (enable == _enableChecking) return;

        _enableChecking = enable;

        ensureCheckBoxCreated();

        if (enable) {
            if (_titleContainer.getWidgetIndex(_titleCheckBox) == -1) {
                _titleContainer.insert(_titleCheckBox, 0);
                _titleContainer.setCellVerticalAlignment(_titleCheckBox, HasVerticalAlignment.ALIGN_MIDDLE);
                GwtUtil.setStyle(_titleCheckBox, "paddingLeft", "2px");
            }
        } else {
            if (_titleContainer.getWidgetIndex(_titleCheckBox) > -1) {
                _titleContainer.remove(_titleCheckBox);
            }
        }
    }

    public boolean getEnableChecking() {
        return _enableChecking;
    }

    public boolean isChecked() {
        return _titleCheckBox != null ? _titleCheckBox.getValue() : _widgetChecked;
    }

    public void setChecked(boolean checked) {
        _widgetChecked = checked;
        if (checked && _enableChecking) ensureCheckBoxCreated();
        if (_titleCheckBox != null) _titleCheckBox.setValue(checked);
    }


    public void setPopoutButtonVisible(boolean v) {
        _toolPanel.setPopoutButtonVisible(v);
    }

    public void setLockToolbarVisible(boolean lockVisible) {
        _lockVisible = lockVisible || menuBarPermLockVisible;
        if (_lockVisible) _toolPanel.showToolbar(true);
    }


    protected void showToolbar(boolean show) {
        if (enableToolbar()) {
            if (!_lockVisible) _toolPanel.showToolbar(show);
        }
    }

    public Widget getToolPanel() {
        return _toolPanel;
    }

    protected boolean enableToolbar() {
        return true;
    }

    public PopoutToolbar getPopoutToolbar() {  return _toolPanel; }
    public void setDefaultToolbarHeight(int h) {
        defTitleHeight= h;
        if (!_expanded) {
            _titleHeight= defTitleHeight;
            GwtUtil.DockLayout.setWidgetChildSize(_clickTitlePanel, _titleHeight);
            _movablePanel.forceLayout();
        }
    }

    public void enableExpansionToolbarHiding() { expansionToolbarHiding= true; }

    protected void clearToolbar() {
        _titlePanel.remove(_titleContainer);
        _titlePanel.remove(_toolPanel);
        _titlePanel.add(_titleContainer);
    }

    protected void addToolbarButton(Widget w, int width) {

        if (enableToolbar()) {
            if (_titlePanel.getWidgetIndex(_toolPanel) > -1) {
                _titlePanel.remove(_toolPanel);
                toolbarWidth += 4;
            }
            toolbarWidth += width;
            _toolPanel.addToolbarButton(w);
            _titlePanel.remove(_titleContainer);
            _titlePanel.addEast(_toolPanel, toolbarWidth);
            _titlePanel.add(_titleContainer);
        }
    }


    public void setMinSize(int minWidth, int minHeight) {
        _minDim = new Dimension(minWidth, minHeight);
        updateMinSizeDim(_minDim);
    }

    private void updateMinSizeDim(Dimension dim) {
        GwtUtil.setStyles(_movablePanel, "minWidth", dim.getWidth() + "px",
                                         "minHeight", dim.getHeight() + "px");
    }

    private void ensureCheckBoxCreated() {
        if (_titleCheckBox == null && _enableChecking) {
            _titleCheckBox = new CheckBox();
            _titleCheckBox.setValue(_widgetChecked);
            _titleCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                public void onValueChange(ValueChangeEvent<Boolean> ev) {
                    WebEvent<Boolean> wev = new WebEvent<Boolean>(PopoutWidget.this, Name.CHECKED_PLOT_CHANGE, ev.getValue());
                    AllPlots.getInstance().fireEvent(wev);
                    if (PopoutWidget.this instanceof MiniPlotWidget) {
                        MiniPlotWidget mpw = (MiniPlotWidget) PopoutWidget.this;
                        if (mpw.getGroup().isAllChecked() && !ev.getValue()) {
                            mpw.getGroup().checkboxForcedOffAllChecked();
                        }
                    }
                }
            });
        }
    }

    public void setPopoutWidget(Widget w) {
//        w.setWidth("100%");
//        w.setHeight("100%");

        popoutWidget= w;
        if (_movablePanel.getCenter() != null) {
            _movablePanel.remove(_movablePanel.getCenter());
        }
        _movablePanel.add(w);
        _movablePanel.addStyleName("popout-movable-panel");
        onResize();
    }

    public Widget getTitleWidget() {
        return _titleContainer;
    }

    public boolean isExpanded() {
        return _expanded;
    }

    public boolean isExpandedSingleView() {
        PopoutWidget w= AllPlots.getInstance().getExpandedController();
        if (w!=null) return w._expanded && (getViewType() == ViewType.ONE || w._expandedList.size() == 1);
        else return false;
    }


    public PopoutWidget getExpandedSingleViewWidget() {
        PopoutWidget retval= null;
        if (isExpandedSingleView()) {
            int curr = _popoutUI.getVisibleIdxInOneMode();
            if (curr>-1 && curr<_expandedList.size()) {
                retval= _expandedList.get(curr);
            }
        }
        return retval;
    }

    public boolean isExpandedGridView() {
        PopoutWidget w= AllPlots.getInstance().getExpandedController();
        if (w!=null) return w._expanded && getViewType()==ViewType.GRID && w._expandedList.size()>1;
        else return false;
    }


    void showGridView() {
        if (gridFillType==FillType.OFF) gridFillType= FillType.CONTEXT;
        _behavior.setGridPlotFillStyle(gridFillType);
        int size = _expandedList.size();
        if (size > 1) {
            PopoutWidget currPopout = _behavior.chooseCurrentInExpandMode();
            if (currPopout != null) {
                _popoutUI.updateExpandedTitle(currPopout);
            }

            int rows = 1;
            int cols = 1;
            VisUtil.FullType fullType = VisUtil.FullType.ONLY_WIDTH;
            if (currPopout instanceof MiniPlotWidget) {
                cols = ((MiniPlotWidget)currPopout).getGroup().getGridPopoutColumns();
                fullType = ((MiniPlotWidget)currPopout).getGroup().getGridPopoutZoomType();
            }

            if (fullType.equals(VisUtil.FullType.SMART)) {
                rows = (int)(Math.ceil(size / (float)cols));
            } else {
                if (size >= 7) {
                    rows = size / 4 + (size % 4);
                    cols = 4;
                } else if (size == 5 || size == 6) {
                    rows = 2;
                    cols = 3;
                } else if (size == 4) {
                    rows = 2;
                    cols = 2;
                } else if (size == 3) {
                    rows = 1;
                    cols = 3;
                } else if (size == 2) {
                    rows = 1;
                    cols = 2;
                }
            }
            int w = _expandRoot.getOffsetWidth() / cols;
            int h = _expandRoot.getOffsetHeight() / rows;
//            _expandRoot.clear();
//            _expandDeck.clear();
//            if (_topBar!=null) _expandRoot.addNorth(_topBar, CONTROLS_HEIGHT);
//            _expandRoot.add(_expandGrid);

            _popoutUI.reinit(ViewType.GRID, _expandRoot);


            _popoutUI.resizeGrid(rows, cols);

            updateExpandRoot(_expandPopout.getAvailableSize());


            int col = 0;
            int row = 0;
            for (PopoutWidget popout : _expandedList) {
                GwtUtil.DockLayout.setWidgetChildSize(popout._clickTitlePanel, 0);
                _popoutUI.setGridWidget(row, col, popout._movablePanel);
                GwtUtil.setStyle(popout._movablePanel, "display", "block");
                DOM.setStyleAttribute(popout._movablePanel.getElement(), "position", "relative");
                popout._movablePanel.setPixelSize(w, h);
                popout._movablePanel.forceLayout();

                col = (col < cols - 1) ? col + 1 : 0;
                if (col == 0) row++;
            }
//            _behavior.onGridResize(_expandedList, new Dimension(w, h), true);
            updateGridBorderStyle();
            _expandRoot.forceLayout();
            if (_expandedList.size() > 1) setViewType(ViewType.GRID);
            _popoutUI.updateOneImageNavigationPanel();
            AllPlots.getInstance().updateUISelectedLook();
        }
    }

    private void updateExpandRoot(Dimension dim) {
        _expandRoot.setPixelSize(dim.getWidth() - 10, dim.getHeight() - 10);
    }


    void showOneView(int showIdx) {

        if (showIdx > _expandedList.size() - 1) showIdx = 0;

        _popoutUI.reinit(ViewType.ONE, _expandRoot);
        DeckLayoutPanel expandDeck = _popoutUI.getDeck();

        updateExpandRoot(_expandPopout.getAvailableSize());

        for (PopoutWidget popout : _expandedList) {
            GwtUtil.DockLayout.setWidgetChildSize(popout._clickTitlePanel, 0);
            popout._movablePanel.forceLayout();
            popout._stagePanel.setWidget(null);
            popout._movablePanel.setWidth("100%");
            popout._movablePanel.setHeight("100%");
            expandDeck.add(popout._movablePanel);
            GwtUtil.setStyle(popout._movablePanel, "border", "none");
        }

        _expandRoot.forceLayout();
        if (expandDeck.getWidgetCount() > 0) expandDeck.showWidget(showIdx);
        onResize();
        if (_expandedList.size() > 1) setViewType(ViewType.ONE);
        _popoutUI.updateOneImageNavigationPanel();
        AllPlots.getInstance().updateUISelectedLook();
    }

    public void forceExpand() {
       forceExpand(false);
    }

    public void forceExpand(boolean evenIfExpanded) {
        if (!_expanded || evenIfExpanded) {
            _expanded= false;
            toggleExpand();
        }
//        else {
//            getPopoutContainer().show();
//        }
    }

    public void forceCollapse() { if (_expanded) toggleExpand(); }

    public void forceSwitchToOne() {
        if (_expanded && isPrimaryExpanded()) {
            _popoutUI.switchToOne();
        }
    }
    public void forceSwitchToGrid() {
        if (_expanded && isPrimaryExpanded()) {
            _popoutUI.switchToGrid();
        }
    }

    public void updateExpanded(ViewType viewType) {
        if (_expanded) {
            initFillStyle();
            PopoutChoice pc = _behavior.getPopoutWidgetList(this);
            _expandedList = pc.getSelectedList();
            _originalExpandedList = pc.getFullList();
            _popoutUI.updateList(_expandedList, _originalExpandedList);
            for (PopoutWidget popout : _originalExpandedList) {
                _behavior.onPostExpandCollapse(popout, true, this);
            }
            setViewType(viewType);
            if (getViewType() == ViewType.ONE || _expandedList.size() == 1) {
                int showIdx = _expandedList.indexOf(this);
                if (showIdx == -1) showIdx = 0;
                showOneView(showIdx);
            } else if (getViewType() == ViewType.GRID) {
                showGridView();
            } else {
                WebAssert.argTst(false, "don't know this case");
            }
        }
    }


    private void initFillStyle() {
        oneFillType= FillType.FIT;
        _behavior.setOnePlotFillStyle(oneFillType);
        if (gridFillType==FillType.OFF) gridFillType= FillType.CONTEXT;
        _behavior.setGridPlotFillStyle(gridFillType);
    }

    public void toggleExpand() {

        _expanded = !_expanded;                        // makes sure they are all set the same way

        if (_popoutUI!=null) { // either expanded state, this should be cleaned up
            _popoutUI.removeHeaderBar();
            _popoutUI.freeResources();
            _popoutUI = null;
        }

        if (_expanded) {
            initFillStyle();
            PopoutChoice pc = _behavior.getPopoutWidgetList(this);
            _expandedList = pc.getSelectedList();
            _originalExpandedList = pc.getFullList();
            _popoutUI = new PopoutControlsUI(this, _behavior, _expandedList, _originalExpandedList);
            _popoutUI.addHeaderBar();
        } else {
//            if (isExpanded()) _expandPopout.hide();
            _expandPopout.hideOnlyDisplay();
        }

        for (PopoutWidget popout : _originalExpandedList) {
            popout._expanded = _expanded;                        // makes sure they are all set the same way
            _behavior.onPreExpandCollapse(popout, _expanded, this);


            if (_expanded) {
                _behavior.onPostExpandCollapse(popout, _expanded, this);
                popout.updateMinSizeDim(new Dimension(20, 20));
            } else {
                if (popout._canCollapse) {
                    popout.setUnexpandedStyle();
                    popout.updateMinSizeDim(popout._minDim);
                    GwtUtil.setStyle(popout._movablePanel, "border", "none");
                    popout._stagePanel.setWidget(popout._movablePanel);
                    _behavior.onPostExpandCollapse(popout, _expanded, this);

                    GwtUtil.DockLayout.setWidgetChildSize(popout._clickTitlePanel, popout._titleHeight);
                    popout._movablePanel.forceLayout();
                } else {
                    _behavior.onPostExpandCollapse(popout, _expanded, this);
                }
            }
        }

        if (_expanded) {
            if (!_expandPopout.isExpanded())_expandPopout.show();
            if (getViewType() == ViewType.ONE || _expandedList.size() == 1) {
                int showIdx = _expandedList.indexOf(this);
                if (showIdx == -1) showIdx = 0;
                showOneView(showIdx);
            } else if (getViewType() == ViewType.GRID) {
                showGridView();
            } else {
                WebAssert.argTst(false, "don't know this case");
            }
        }
        onResize();

        if (expansionToolbarHiding) _toolPanel.setVisible(!_expanded);
        AllPlots.getInstance().updateUISelectedLook();
    }


    public void updateGridBorderStyle() {
//        if (AllPlots.getInstance().isExpanded() && _expandedList != null && getViewType() == ViewType.GRID) {
//            for (PopoutWidget popout : _expandedList) {
//                GwtUtil.setStyle(popout._movablePanel, "border", _behavior.getGridBorderStyle(popout));
//            }
//        }

    }


    public void setTitle(String title) {
        _title = title;
//        if (_primaryExpanded) _expandedTitle= title;
        setTitleLabel(_title, _secondaryTitle);
    }

    public void setSecondaryTitle(String secondaryTitle) {
        _secondaryTitle = secondaryTitle;
        setTitleLabel(_title, _secondaryTitle);
    }
    public void forceTitleUpdate() { setTitleLabel(_title,_secondaryTitle);  }


    private void setTitleLabel(String p, String s) {
        if (p == null) p = "";
        if (s == null) s = "";
        _titleLbl.setHTML(p + s);
    }

    public String getTitleLabelHTML() {
        String p = "";
        String s = "";
        if (_title != null) p = _title;
        if (_secondaryTitle != null) s = _secondaryTitle;
        return p + s;
    }

    public String getExpandedTitle(boolean allowHtml) {
        return _title;
    }

    public String getTitle() {
        return _title;
    }

    public String getSecondaryTitle() {
        return _secondaryTitle;
    }


    private void setUnexpandedStyle() {
        GwtUtil.setStyles(_movablePanel, "position", "absolute",
                          "left",  "0px",
                          "right", "0px",
                          "top", "0px",
                          "bottom", "0px",
                          "width", "auto",
                          "height", "auto",
                          "display", "block");

    }

    public abstract void widgetResized(int width, int height);


//=======================================================================
//-------------- Method from ResizableWidget Interface ------------------
//=======================================================================


    public void onResize() {
        if (GwtUtil.isOnDisplay(_movablePanel)) {
            widgetResized(_movablePanel.getOffsetWidth(),
                          _movablePanel.getOffsetHeight());
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    @Override
    protected void onUnload() {
        super.onUnload();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        onResize();
    }

//    Widget getPanelToMask() {
//        return _movablePanel;
//    }

//======================================================================
//------------------ Inner Classes -------------------------------------
//======================================================================

    public static class PopoutChoice {
        private final List<PopoutWidget> _selectedList;
        private final List<PopoutWidget> _fullList;


        public PopoutChoice(List<PopoutWidget> selectedList, List<PopoutWidget> fullList) {
            _selectedList = selectedList;
            _fullList = fullList;
        }

        public List<PopoutWidget> getSelectedList() {
            return _selectedList;
        }

        public List<PopoutWidget> getFullList() {
            return _fullList;
        }
    }


    public class ResizablePanel extends DockLayoutPanel implements RequiresResize {
        public ResizablePanel() {
            super(Style.Unit.PX);
        }

        public Widget getCenter() {
            return super.getCenter();
        }

        @Override
        public void onResize() {
            super.onResize();    //To change body of overridden methods use File | Settings | File Templates.
            PopoutWidget.this.onResize();
        }
    }


    public static void setViewType(ViewType viewType) {
        setViewType(viewType, false);
    }


    public static void setViewType(ViewType viewType, boolean forInit) {
        _lastViewType = viewType;
        if (!forInit) {
            Preferences.set("Popout.ViewType.Value", viewType.toString());
        }
    }

    public static ViewType getViewType() {
        if (_lastViewType == ViewType.UNKNOWN) {
            _lastViewType = ViewType.ONE;
        }
        return _lastViewType;
    }



    public class MyDockLayoutPanel extends DockLayoutPanel implements VisibleListener{
        public MyDockLayoutPanel() { super(Style.Unit.PX); }

        public void onShow() {
            if (_expandPopout instanceof PopupContainerForStandAlone ||
                _expandPopout instanceof PopupContainerForRegion)  {
                DeferredCommand.add(new Command() {
                    public void execute() {
                        _popoutUI.setResizeZoomEnabled(false);
                        MyDockLayoutPanel.this.forceLayout();
                        _popoutUI.setResizeZoomEnabled(true);
                    }
                });
            }
        }

        public void onHide() {
        }
    }


    public void updateUISelectedLook() {
        boolean selected= AllPlots.getInstance().getSelectPopoutWidget()==this;

        if (popoutWidget == null) return;

        if (isExpandedGridView()) {
            if (selected) {
                GwtUtil.setStyles(popoutWidget, "borderStyle", "ridge",
                                  "borderWidth", "3px 2px 2px 2px",
                                  "borderColor", "orange");
            }
            else {
                GwtUtil.setStyles(popoutWidget, "borderStyle", "ridge",
                                  "borderWidth", "3px 2px 2px 2px",
                                  "borderColor", "rgba(0,0,0,.4)");
            }
        }
        else {
            GwtUtil.setStyle(popoutWidget, "border", "none");
        }

    }




    protected static PopoutContainer choosePopoutType(boolean fullControl) {
        PopoutType ptype= Application.getInstance().getCreator().isApplication() ?
                                             PopoutType.REGION : PopoutType.STAND_ALONE;
        PopoutContainer retval= null;
        switch (ptype) {
//            case TOOLBAR:     retval= new PopupContainerForToolbar(); break;
            case REGION:     retval= new PopupContainerForRegion(); break;
            case STAND_ALONE: retval= new PopupContainerForStandAlone(fullControl); break;
            default: WebAssert.argTst(false, "Don't know this Type"); break;
        }
        return retval;
    }

    public static class Behavior {

        public PopoutChoice getPopoutWidgetList(PopoutWidget activatingPopout) {
            return new PopoutChoice(Arrays.asList(activatingPopout), Arrays.asList(activatingPopout));
        }
        public PopoutWidget chooseCurrentInExpandMode() { return null; }
        public String getGridBorderStyle(PopoutWidget popout) { return "1px solid transparent"; }
        public void onPreExpandCollapse(PopoutWidget popout, boolean expanded, PopoutWidget activatingPopout) { }
        public void onPostExpandCollapse(PopoutWidget popout, boolean expanded, PopoutWidget activatingPopout) { }
        public void onPrePageInExpandedMode(PopoutWidget oldPopout, PopoutWidget newPopout, Dimension dimension) { }
        public void onPostPageInExpandedMode(PopoutWidget oldPopout, PopoutWidget newPopout, Dimension dimension) { }
        public void onSingleResize(PopoutWidget popout, Dimension d, boolean adjustZoom) { }
        public void onGridResize(List<PopoutWidget> popout, Dimension d, boolean adjustZoom) { }
        public void setOnePlotFillStyle(FillType fillStyle) { }
        public void setGridPlotFillStyle(FillType fillStyle) { } }


}
