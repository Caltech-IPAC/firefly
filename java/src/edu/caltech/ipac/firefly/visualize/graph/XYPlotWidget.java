package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.ButtonType;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskMessgeWidget;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.PopupContainerForToolbar;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.table.BasicTable;
import edu.caltech.ipac.firefly.ui.table.DataSetTableModel;
import edu.caltech.ipac.firefly.ui.table.FilterToggle;
import edu.caltech.ipac.firefly.ui.table.ModelEventHandler;
import edu.caltech.ipac.firefly.ui.table.filter.FilterDialog;
import edu.caltech.ipac.firefly.ui.table.filter.FilterPanel;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author tatianag
 * $Id $
 */
public class XYPlotWidget extends PopoutWidget implements FilterToggle.FilterToggleSupport {


    // colors that color blind people can distinguish
    // http://safecolours.rigdenage.com/Comp10.jpg
    // plus the colors that are 3 stops darker
    // see http://www.w3schools.com/tags/ref_colorpicker.asp
    private static String [] colors = {"#333333", "#ff3333", "#00ccff","#336600",
              "#9900cc", "#ff9933", "#009999", "#66ff33", "#cc9999",
            "#333333", "#b22424", "#008fb2", "#244700",
        "#6b008f", "#b26b24", "#006b6b", "#47b224", "8F6B6B"};
    
    // CSS light colors
    private static String [] lightcolors = {"MediumPurple", "LightCoral", "LightBlue", "Olive",
              "Plum", "LightSalmon", "SandyBrown", "PaleTurquoise", "YellowGreen",
              "LightPink", "CornflowerBlue", "Khaki", "PaleGreen", "LightSteelBlue"};

    private static final String ZOOM_OUT_HELP = "&nbsp;Zoom out with original size button.&nbsp;";
    private static final String ZOOM_IN_HELP = "&nbsp;Rubber band zoom &mdash; click and drag an area to zoom in.&nbsp;";
    private static final String SELECT_HELP = "&nbsp;Click and drag an area to select points in it.&nbsp;";
    private static final String UNSELECT_HELP = "&nbsp;Click and drag an empty area to unselect points.&nbsp;";


    private static int MIN_SIZE_FOR_DOCKED_OPTIONS = 650;
    private static int OPTIONS_PANEL_WIDTH = 350;

    private static final int RESIZE_DELAY= 100;
    DockLayoutPanel _dockPanel = new DockLayoutPanel(Style.Unit.PX);
    ScrollPanel _panel= new ScrollPanel();
    //VerticalPanel _vertPanel = new VerticalPanel(); // for chart, options, etc.
    SimplePanel _cpanel= new SimplePanel(); // for chart
    HTML _statusMessage;
    private FilterToggle _filters;
    private Label filterSelectedLink;
    boolean rubberbandZooms = true;
    private final MaskMessgeWidget _maskMessge = new MaskMessgeWidget(false);
    private final MaskPane _maskPane=
            new MaskPane(_dockPanel, _maskMessge);
    private GChart _chart = null;
    private String _sourceFile = null;
    private String _suggestedName = null;
    private DataSet _dataSet;
    private XYPlotData _data = null;
    private XYPlotMeta _meta = null;
    private Widget _legend = null;
    private boolean _showLegend = false;
    private boolean _popoutWidgetSet;
    private int _xResizeFactor = 1;
    private int _yResizeFactor = 1;
    private int TICKS = 6; // 5 intervals
    private boolean _logScale = false;

    ArrayList<GChart.Curve> _mainCurves;
    ArrayList<SpecificPointUI> _specificPoints;
    String specificPointsDesc;
    GChart.Curve _selectionCurve;
    GChart.Curve _highlightedPoints;
    GChart.Curve _selectedPoints;
    boolean _selecting = false;
    Selection _savedSelection = null;
    //boolean preserveOutOfBoundPoints = false;
    HTML _actionHelp;
    private XYPlotOptionsPanel optionsPanel;
    private XYPlotOptionsDialog optionsDialog;
    private ShowColumnsDialog showColumnsDialog;
    private FilterDialog popoutFilters;
    private ResizeTimer _resizeTimer= new ResizeTimer();

    private DataSetTableModel _tableModel = null;
    private ModelEventHandler dsModelEventHandler;

    private List<NewDataListener> _listeners = new ArrayList<NewDataListener>();

    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();

    public XYPlotWidget(XYPlotMeta meta) {
        //super(150, 90); long labels will get wrapped
        super(new PopupContainerForToolbar(), 300, 180);
        _meta = meta;
        AllPlots.getInstance().registerPopout(this);
        GChart.setCanvasFactory(ChartingFactory.getInstance());
        _popoutWidgetSet = false;

        _actionHelp = new HTML();
        _actionHelp.setWidth("100%");
        _actionHelp.addStyleName(_ffCss.highlightText());
        /**
        final CheckBox outOfBoundCheck = GwtUtil.makeCheckBox("Connect Out of Bounds Points",
                "Take into account out of bounds points that are reasonably close", false);
        outOfBoundCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

            public void onValueChange(ValueChangeEvent valueChangeEvent) {
                if (_chart != null && _data != null) {
                    preserveOutOfBoundPoints = outOfBoundCheck.getValue();
                    if (preserveOutOfBoundPoints) {
                        _chart.getYAxis().setOutOfBoundsMultiplier(Double.NaN);
                        _chart.getY2Axis().setAxisMin(_chart.getY2Axis().getAxisMin());
                        _chart.update();
                    } else {
                        _chart.getYAxis().setOutOfBoundsMultiplier(0);
                        _chart.getY2Axis().setAxisMin(_chart.getY2Axis().getAxisMin());
                        _chart.update();
                    }
                }
            }
        });
         */
    }

    private void showOptionsDialog() {
        if (optionsDialog == null) {
            optionsDialog = new XYPlotOptionsDialog(XYPlotWidget.this);
        }
        optionsDialog.setVisible(true);

    }


    private void setupNewChart(String title) {
        _selecting = false;
        _savedSelection = null;

        if (!_popoutWidgetSet) {
            //_vertPanel.add(_cpanel);
            //_vertPanel.setWidth("100%");
            _cpanel.setWidth("100%");
            _panel.setWidth("100%");
            _dockPanel.setSize("100%", "100%");
            _dockPanel.addStyleName("component-background");
            _dockPanel.addNorth(getMenuBar(), 40);
            _dockPanel.addWest(getOptionsPanel(), OPTIONS_PANEL_WIDTH);
            _statusMessage = GwtUtil.makeFaddedHelp("&nbsp;");
            GwtUtil.setStyles(_statusMessage, "textAlign", "left", "paddingTop", "2px", "borderTop", "1px solid #bbbbbb");
            ScrollPanel statusPanel = new ScrollPanel();
            statusPanel.setSize("100%", "100%");
            statusPanel.add(_statusMessage);
            _dockPanel.addSouth(statusPanel, 20);
            _dockPanel.add(_panel);
            GwtUtil.DockLayout.hideWidget(_dockPanel, optionsPanel);
            setPopoutWidget(_dockPanel);
            _popoutWidgetSet = true;
        }
        setTitle(title);
        //removeCurrentChart();
        if (_chart == null) {
            _chart = new GChart(_meta.getXSize(), _meta.getYSize());
            _chart.setOptimizeForMemory(true);
            _chart.setPadding("5px");
            _chart.setLegendBorderWidth(0); // no border
            _chart.setBackgroundColor("white");
            _chart.setGridColor("#999999");
            _chart.setHoverParameterInterpreter(new XYHoverParameterInterpreter());
            _chart.setClipToPlotArea(true);
            _chart.setClipToDecoratedChart(true);
            Widget footnotes = GwtUtil.leftRightAlign(new Widget[]{_actionHelp}, new Widget[]{new HTML("&nbsp;"), HelpManager.makeHelpIcon("visualization.xyplotViewer")});
            footnotes.setWidth("100%");
            _chart.setChartFootnotes(footnotes);
            _chart.setChartFootnotesLeftJustified(true);
            addMouseListeners();
            _cpanel.setWidget(_chart);
        } else {
            //_chart.setChartSize(_meta.getXSize(), _meta.getYSize());
        }
        // if we are not showing legend, inform the chart
        _chart.setLegendVisible(_showLegend || _meta.alwaysShowLegend());
    }

    private Widget getMenuBar() {
        FlowPanel menuBar = new FlowPanel();
        GwtUtil.setStyle(menuBar, "borderBottom", "1px solid #bbbbbb");
        menuBar.setWidth("100%");

        HorizontalPanel left = new HorizontalPanel();
        left.setSpacing(10);
        GwtUtil.setStyle(left, "align", "left");

        HorizontalPanel right = new HorizontalPanel();
        right.setSpacing(10);
        GwtUtil.setStyle(right, "align", "center");
        GwtUtil.setStyle(right, "paddingRight", "20px");

        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        right.add(GwtUtil.makeImageButton(new Image(ic.getSave()), "Download data in IPAC table format", new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Frame f = Application.getInstance().getNullFrame();
                String url;
                if (_sourceFile.contains("://")) {
                    url = _sourceFile;
                } else {
                    Param[] params;
                    if (_suggestedName != null) {
                        params = new Param[3];
                        params[2] = new Param("return", _suggestedName);
                    } else {
                        params = new Param[2];
                    }
                    params[0] = new Param("file", _sourceFile);
                    params[1] = new Param("log", "true");
                    url = WebUtil.encodeUrl(GWT.getModuleBaseURL() + "servlet/Download", params);
                }
                f.setUrl(url);
            }
        }));

        right.add(GwtUtil.makeImageButton(new Image(ic.getZoomOriginal()), "Zoom out to original chart", new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (_data != null) {
                    _savedSelection = null;
                    setChartAxes();
                    _actionHelp.setHTML(rubberbandZooms?ZOOM_IN_HELP:SELECT_HELP);
                    _chart.update();
                }
            }
        }));

        final DeckPanel selectToggle = new DeckPanel();
        selectToggle.add(GwtUtil.makeImageButton(new Image(ic.getSelectAreaOff()), "Select enclosed points on rubberband", new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        if (_data != null) {
                            rubberbandZooms = false;
                            _actionHelp.setHTML(SELECT_HELP);
                            selectToggle.showWidget(1);
                        }
                    }
                }));
        selectToggle.add(GwtUtil.makeImageButton(new Image(ic.getSelectAreaOn()), "Turn off selection mode", new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (_data != null) {
                    rubberbandZooms = true;
                    _actionHelp.setHTML(ZOOM_IN_HELP);
                    selectToggle.showWidget(0);
                }
            }
        }));
        selectToggle.showWidget(0);
        right.add(selectToggle);

        right.add(GwtUtil.makeImageButton(new Image(ic.getFitsHeader()), "Show All Columns", new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                showColumns(RootPanel.get(), PopupPane.Align.CENTER);
            }
        }));



        Label text = new Label("Options");
        HorizontalPanel hp = new HorizontalPanel();
        hp.setSpacing(2);
        hp.add(new Image(ic.getSettings()));
        hp.add(text);
        GwtUtil.makeIntoLinkButton(hp);
        text.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                showOptions();
            }
        });
        left.add(hp);

        _filters = new FilterToggle(this);
        left.add(_filters);

        filterSelectedLink = getFilterSelectedLink();
        filterSelectedLink.setVisible(false);
        left.add(filterSelectedLink);

        menuBar.add(GwtUtil.leftRightAlign(new Widget[]{left}, new Widget[]{right}));

        return menuBar;
    }



    private XYPlotOptionsPanel getOptionsPanel() {
        if (optionsPanel == null) {
            optionsPanel = new XYPlotOptionsPanel(this);
            GwtUtil.setStyle(optionsPanel, "paddingTop", "10px");
        }
        return optionsPanel;
    }

    private void showOptions() {

        boolean show = !(optionsDialog!=null && optionsDialog.isVisible()) && GwtUtil.DockLayout.isHidden(optionsPanel);
        if (show) {
            if (_panel.asWidget().getOffsetWidth()>MIN_SIZE_FOR_DOCKED_OPTIONS) {
                GwtUtil.DockLayout.showWidget(_dockPanel, optionsPanel);
                onResize();
                //resize(_dockPanel.getOffsetWidth(), _dockPanel.getOffsetHeight());
            } else {
                showOptionsDialog();
            }
        } else {
            if (!GwtUtil.DockLayout.isHidden(optionsPanel)) {
                GwtUtil.DockLayout.hideWidget(_dockPanel, optionsPanel);
                onResize();
                //resize(_dockPanel.getOffsetWidth(), _dockPanel.getOffsetHeight());
            }
            if (optionsDialog != null && optionsDialog.isVisible()) {
                optionsDialog.setVisible(false);
            }
        }
    }

    public void makeNewChart(final DataSetTableModel tableModel, String title) {
        if (!tableModel.equals(_tableModel)) {
            if (_tableModel != null && dsModelEventHandler != null) {
               _tableModel.removeHandler(dsModelEventHandler);
            }
            _tableModel = tableModel;
            dsModelEventHandler = new ModelEventHandler(){

                public void onFailure(Throwable caught) {
                }

                public void onLoad(TableDataView result) {
                }

                public void onStatusUpdated(TableDataView result) {
                    updateStatusMessage();
                }

                public void onDataStale(DataSetTableModel model) {
                    doServerCall(getRequiredCols(), _meta.getMaxPoints());
                }
            };
            _tableModel.addHandler(dsModelEventHandler);
        }
        _maskPane.hide();
        setupNewChart(title);
        doServerCall(getRequiredCols(), _meta.getMaxPoints());
    }

    private void doServerCall(final List<String> requiredCols, final int maxPoints) {
        _maskPane.hide();
        _filters.reinit();
        _dataSet = null;
        _savedSelection = null; // do not preserve zoomed selection
        removeCurrentChart();
        GwtUtil.DockLayout.hideWidget(_dockPanel, _statusMessage);
        if (showColumnsDialog != null) { showColumnsDialog.setVisible(false); showColumnsDialog = null; }

        ServerTask task = new ServerTask<TableDataView>(_dockPanel, "Retrieving Data...", true) {
            public void onSuccess(TableDataView result) {
                try {
                    _dataSet = (DataSet)result;
                    //_dataSet = result.subset(0, tableDataView.getTotalRows());
                    addData(_dataSet, _tableModel.getRequest());
                    updateStatusMessage();
                    onResize();
                    //resize(_dockPanel.getOffsetWidth(), _dockPanel.getOffsetHeight());
                } catch (Exception e) {
                    showMask(e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                showMask(throwable.getMessage());
            }


            @Override
            public void doTask(AsyncCallback<TableDataView> passAlong) {
                _tableModel.getAdHocData(passAlong, requiredCols, 0, maxPoints);
            }
        };
        //task.setMaskingDelaySec(1);
        task.start();
    }

    private List<String> getRequiredCols() {
        final ArrayList<String> requiredCols = new ArrayList<String>();

        // Limit number of columns for bigger tables
        if (_tableModel.getTotalRows() > 10) {
            ArrayList<String> cols = new ArrayList<String>();
            List<TableDataView.Column> allCols = _tableModel.getCurrentData().getColumns();
            for (TableDataView.Column c : allCols) {
                // interested only in numeric columns
                if (!c.getType().startsWith("c")) {
                    cols.add(c.getName());
                }
            }
            XYPlotMeta.UserMeta userMeta = _meta.userMeta;
            String c;
            if (userMeta != null && userMeta.xColExpr != null) {
                Set<String> cSet = userMeta.xColExpr.getParsedVariables();
                for (String s : cSet) {
                    if (!StringUtils.isEmpty(s)) requiredCols.add(s);
                }
            } else {
                c = _meta.findXColName(cols);
                if (!StringUtils.isEmpty(c)) requiredCols.add(c);
            }
            if (userMeta != null && userMeta.yColExpr != null) {
                Set<String> cSet = userMeta.yColExpr.getParsedVariables();
                for (String s : cSet) {
                    if (!StringUtils.isEmpty(s) && !requiredCols.contains(s)) requiredCols.add(s);
                }
            } else {
                c = _meta.findYColName(cols);
                if (!StringUtils.isEmpty(c) && !requiredCols.contains(c)) requiredCols.add(c);
            }
            c = _meta.findErrorColName(cols);
            if (!StringUtils.isEmpty(c) && !requiredCols.contains(c)) requiredCols.add(c);
            c = _meta.findDefaultOrderColName(cols);
            if (!StringUtils.isEmpty(c) && !requiredCols.contains(c)) requiredCols.add(c);
        }
        return requiredCols;
    }

    private void addData(DataSet dataSet, TableServerRequest sreq) {
        // check if DOWNLOAD_SOURCE attribute present:
        String downloadSource;
        TableMeta tableMeta = dataSet.getMeta();
        if (!StringUtils.isEmpty(tableMeta))  {
            downloadSource = dataSet.getMeta().getAttribute("DOWNLOAD_SOURCE");

            if (StringUtils.isEmpty(downloadSource)) {
                // use TableServerRequest, if available, to get source table url
                if (sreq != null) {
                    _sourceFile = WebUtil.getTableSourceUrl(sreq);
                } else {
                    _sourceFile = dataSet.getMeta().getSource();
                }
            } else {
                _sourceFile = downloadSource;
            }
        }

        try {
            addData(_dataSet);
            _selectionCurve = getSelectionCurve();
            _panel.setWidget(_cpanel);
            if (optionsDialog != null && (optionsDialog.isVisible() || _meta.hasUserMeta())) {
                if (optionsDialog.setupError()) {
                    if (!optionsDialog.isVisible()) showOptionsDialog();
                }
            }
        } catch (Throwable e) {
            if (e.getMessage().indexOf("column is not found") > 0) {
                _chart.clearCurves();
                _panel.setWidget(_cpanel);
                showOptionsDialog();
            } else {
                showMask(e.getMessage());
            }
        }

    }

    private void updateStatusMessage() {
        _statusMessage.setHTML("&nbsp;&nbsp;"+getTableInfo());
    }

    public void removeCurrentChart() {
        if (_chart != null) {
            if (_highlightedPoints != null) _highlightedPoints.clearPoints();
            if (_selectedPoints != null) _selectedPoints.clearPoints();
            _chart.clearCurves();
            _panel.remove(_cpanel);
           //_chart = null;
        }
        _statusMessage.setHTML("");
    }

    public XYPlotMeta getPlotMeta() {
        return _meta;
    }


    public XYPlotData getPlotData() {
        return _data;
    }

    public void addListener(NewDataListener l) {
        _listeners.add(l);
    }

    private void addMouseListeners() {

        _chart.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                if (_chart != null && _data != null) {
                    clickEvent.preventDefault();
                    setHighlighted(_chart.getTouchedPoint());
                }
            }
        });

        _chart.addMouseDownHandler(new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent event) {
                /*
                  * Most browsers, by default, support the ability to
                  * to "drag-copy" any web page image to the desktop.
                  * But GChart's rendering makes extensive use of
                  * images, so we need to override this default.
                  *
                  */
                event.preventDefault();

                double x = _chart.getXAxis().getMouseCoordinate();
                double y = _chart.getYAxis().getMouseCoordinate();
                //if (_data.getXMinMax().isIn(x) && _data.getYMinMax().isIn(y)) {
                _selecting = true;
                _selectionCurve.clearPoints();
                _selectionCurve.addPoint(x, y);
                for (GChart.Curve mainCurve : _mainCurves) {
                    mainCurve.getSymbol().setHoverSelectionEnabled(false);
                    mainCurve.getSymbol().setHoverAnnotationEnabled(false);
                }
                //}
            }
        });

        _chart.addMouseMoveHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent event) {
                if (_selecting) {
                    event.preventDefault();

                    double x = _chart.getXAxis().getMouseCoordinate();
                    double y = _chart.getYAxis().getMouseCoordinate();

                    //if (_data.getXMinMax().isIn(x) && _data.getYMinMax().isIn(y)) {
                        GChart.Curve.Point p0 = _selectionCurve.getPoint(0);
                        double x0 = p0.getX();
                        double y0 = p0.getY();

                        _selectionCurve.clearPoints();
                        _selectionCurve.addPoint(x0, y0);
                        _selectionCurve.addPoint(x, y0);
                        _selectionCurve.addPoint(x, y);
                        _selectionCurve.addPoint(x0, y);
                        _selectionCurve.addPoint(x0,y0);
                        _selectionCurve.setVisible(true);
                        _chart.update();
                    //}
                }
            }
        });

        _chart.addMouseUpHandler(new MouseUpHandler() {
            public void onMouseUp(MouseUpEvent event) {
                if (_selecting) {
                    event.preventDefault();
                    _selectionCurve.setVisible(false);
                    for (GChart.Curve mainCurve : _mainCurves) {
                        mainCurve.getSymbol().setHoverSelectionEnabled(true);
                        mainCurve.getSymbol().setHoverAnnotationEnabled(true);
                    }
                    if (_selectionCurve.getNPoints() == 5 && _data != null) {
                        // diagonal points of the selection rectangle
                        GChart.Curve.Point p0 = _selectionCurve.getPoint(0);
                        GChart.Curve.Point p2 = _selectionCurve.getPoint(2);
                        double xMin = Math.min(p0.getX(), p2.getX());
                        double xMax = Math.max(p0.getX(), p2.getX());
                        double yMin = Math.min(getUnscaled(p0.getY()), getUnscaled(p2.getY()));
                        double yMax = Math.max(getUnscaled(p0.getY()), getUnscaled(p2.getY()));
                        MinMax xMinMax = new MinMax(xMin, xMax);
                        MinMax yMinMax = new MinMax(yMin, yMax);

                        if (rubberbandZooms)  {
                            setChartAxesForSelection(xMinMax, yMinMax);
                            _chart.update();
                        } else {
                            setSelected(xMinMax, yMinMax);
                        }
                    }
                    _selecting = false;
                }
            }
        });

    }

    private GChart.Curve getSelectionCurve() {
        _chart.addCurve();
        GChart.Curve selectionCurve = _chart.getCurve();
        GChart.Symbol symbol= selectionCurve.getSymbol();
        symbol.setBorderColor("black");
        symbol.setSymbolType(GChart.SymbolType.LINE);
        symbol.setFillThickness(2);
        symbol.setWidth(0);
        symbol.setHeight(0);
        symbol.setHoverSelectionEnabled(false);
        symbol.setHoverAnnotationEnabled(false);
        selectionCurve.setVisible(false);
        return selectionCurve;
    }

    private void showMask(String text) {
        _maskMessge.setHTML(text);
        _maskPane.show();
    }

    private Widget createLegend() {
        int nCurves = _mainCurves.size();
        int nPoints = _specificPoints.size();
        if  (_data == null || (nCurves<2 && nPoints<1)) return null;
        Grid result = new Grid(nCurves+(nPoints>0 ? (nPoints+1) : 0), 1);
        int cIdx = 0;
        for (final GChart.Curve c : _mainCurves) {
            c.getSymbol().getBorderColor();
            final CheckBox ch = GwtUtil.makeCheckBox(c.getLegendLabel(), "Deselect to hide", true);
            ch.getElement().getStyle().setProperty("color", c.getSymbol().getBorderColor());
            ch.addClickHandler(new ClickHandler() {

                public void onClick(ClickEvent event) {
                    boolean visible = ch.getValue();
                    c.setVisible(visible);
                    // 2 error curves are added for each main curve
                    // error curves are added before main curves
                    if (_meta.plotError() && _data.hasError()) {
                        int cIdx = _mainCurves.indexOf(c);
                        XYPlotData.Curve current = _data.getCurveData().get(cIdx);
                        int lowerErrIdx = current.getErrorLowerCurveIdx();
                        int upperErrIdx = current.getErrorUpperCurveIdx();

                        try {
                            for (int i=lowerErrIdx; i<=upperErrIdx; i++) {
                                _chart.getCurve(i).setVisible(visible);
                            }
                        } catch (Exception e) { _meta.setPlotError(false); }
                    }
                    _chart.update();
                }
            });
            result.setWidget(cIdx, 0, ch);
            cIdx++;
        }
        int pIdx = 0;

        if (_meta.plotSpecificPoints() && nPoints>0) {
            Label desc = new HTML("<br><b>"+specificPointsDesc.replaceAll(" ", "<br>")+"</b>");
            result.setWidget(cIdx, 0, desc);   //"&nbsp;"

            for (final SpecificPointUI pointUI : _specificPoints) {
                final CheckBox ch = GwtUtil.makeCheckBox(pointUI.p.getLabel(), "Deselect to hide", true);
                ch.getElement().getStyle().setProperty("color", lightcolors[pointUI.p.getId()%lightcolors.length]);
                ch.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent event) {
                        pointUI.setVisible(ch.getValue());
                        _chart.update();
                    }
                });
                result.setWidget(cIdx+pIdx+1, 0, ch);
                pIdx++;
            }
        }
        return result;
    }

    public void updateMeta(XYPlotMeta meta, boolean preserveZoomSelection) {
        try {
            _meta = meta;
            if (_chart != null) {
                _chart.clearCurves();
            }
            if (_dataSet != null) {
                List<String> requiredCols = null;
                //do we need server call to get a new dataset?
                boolean serverCallNeeded = _dataSet.getSize() < _tableModel.getTotalRows() && _meta.getMaxPoints() > _dataSet.getSize();
                if (!serverCallNeeded) {
                    requiredCols = getRequiredCols();
                    for (String c : requiredCols) {
                        if (_dataSet.findColumn(c) == null) {
                            serverCallNeeded = true;
                            break;
                        }
                    }
                }

                if (serverCallNeeded) {
                    if (requiredCols == null) {
                        requiredCols = getRequiredCols();
                    }
                    doServerCall(requiredCols, _meta.getMaxPoints());
                } else {
                    addData(_dataSet);
                    _selectionCurve = getSelectionCurve();
                    if (_savedSelection != null && preserveZoomSelection) {
                        setChartAxesForSelection(_savedSelection.xMinMax, _savedSelection.yMinMax);
                        _chart.update();
                    } else {
                        _savedSelection = null;
                    }
                }
            }
            //_meta.addUserColumnsToDefault();
        } catch (Throwable e) {
            if (_chart != null) {
                _chart.clearCurves();
            }
            //_panel.setWidget(_vertPanel);
            PopupUtil.showError("Error",e.getMessage());
        }
    }

    private void addData(DataSet dataSet) {

        _data = new XYPlotData(dataSet, _meta);

        _logScale = _meta.logScale();

        // call listeners
        for (NewDataListener l : _listeners) {
            l.newData(_data);
        }

        // error curves - should be plotted first,
        // so that main curves are plotted on top of them
        if (_meta.plotError() && _data.hasError()) {
            addErrorCurves();
        }

        // main curves
        addMainCurves();

        // add specific points
        addSpecificPoints();

        // set axes (specific points are added here too)
        setChartAxes();

        // set legend
        _legend = createLegend();
        if (_legend != null ) {
            _chart.setLegend(_legend);
            if (!_showLegend && !_meta.alwaysShowLegend()) {
                _legend.setVisible(false);
            }
        }

        //if (_chart.isLegendVisible()) { _chart.setLegend(_legend); }
        //else { _chart.setLegend(null); }

        if (_meta.getTitle() != null) {
            _chart.setChartTitle(_meta.getTitle());
        } else {
            _chart.setChartTitle("<b>Preview: "+_meta.getYName(_data)+" vs. "+_meta.getXName(_data)+"</b>");
        }
        _chart.setChartTitleThickness(60);
        _chart.update();
    }

    private void addMainCurves() {
        _mainCurves = new ArrayList<GChart.Curve>(_data.getCurveData().size());
        GChart.Curve curve;
        for (XYPlotData.Curve cd : _data.getCurveData() ) {
            _chart.addCurve();
            curve = _chart.getCurve();
            _mainCurves.add(curve);

            if (_data.hasOrder()) {
                curve.setLegendLabel("Order " + cd.getOrder());
            }
            GChart.Symbol symbol= curve.getSymbol();
            symbol.setBorderColor(colors[cd.getCurveId() % colors.length]);
            if (_meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.POINTS)) {
                symbol.setWidth(3);
                symbol.setHeight(3);
            } else if (_meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.LINE_POINTS)) {
                symbol.setSymbolType(GChart.SymbolType.LINE);
                symbol.setFillSpacing(0);
                symbol.setFillThickness(1);
                symbol.setWidth(3);
                symbol.setHeight(3);
            } else {
                symbol.setSymbolType(GChart.SymbolType.LINE);
                symbol.setFillSpacing(0);
                symbol.setFillThickness(2);
                symbol.setWidth(0);
                symbol.setHeight(0);
            }
            symbol.setBackgroundColor(symbol.getBorderColor()); // make center of the markers filled
            //symbol.setBrushHeight(2*_meta.getYSize());
            symbol.setBrushHeight(5);  // to facilitate selection
            symbol.setBrushWidth(5);
            symbol.setHoverSelectionWidth(4);
            symbol.setHoverSelectionHeight(4);
            symbol.setHoverSelectionBackgroundColor("yellow");
            symbol.setHoverSelectionBorderColor(symbol.getBorderColor());
            symbol.setHoverAnnotationSymbolType(GChart.SymbolType.ANCHOR_NORTHWEST);
            symbol.setHoverLocation(GChart.AnnotationLocation.NORTHEAST);
            symbol.setHoverYShift(5);
            symbol.setHoverSelectionEnabled(true);
            String xColUnits = getXColUnits();
            String yColUnits = getYColUnits();
            String template = _meta.getXName(_data)+" = ${x}" +
                    (xColUnits != null ? " "+xColUnits : "") +
                    "<br>"+_meta.getYName(_data)+" = ${y}" +
                    (yColUnits != null ?  " "+yColUnits : "");
            if (_data.hasError()) {
                template += "<br>"+_data.getErrorCol()+" = +/- ${err}";
                String errorColUnits = _data.getErrorColUnits();
                if (errorColUnits != null) template += " "+errorColUnits;
            }
            symbol.setHovertextTemplate(GChart.formatAsHovertext(template));

            cd.setCurveIdx(_chart.getCurveIndex(curve));
            if (_logScale) {
                for (XYPlotData.Point p : cd.getPoints()) {
                    curve.addPoint(p.getX(),Math.log10(p.getY()));
                }
            } else {
                for (XYPlotData.Point p : cd.getPoints()) {
                    curve.addPoint(p.getX(),p.getY());
                }
            }
        }
    }

    private void addErrorCurves() {
        GChart.Curve  errCurveLower, errCurveUpper, errBarCurve;
        for (XYPlotData.Curve cd : _data.getCurveData() ) {

            //lower error curve
            _chart.addCurve();
            errCurveLower = _chart.getCurve();

            GChart.Symbol errSymbolLower= errCurveLower.getSymbol();
            errSymbolLower.setBorderColor("lightgray");
            errSymbolLower.setBackgroundColor("lightgray");
            if (_meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.POINTS)) {
                errSymbolLower.setWidth(3);
                errSymbolLower.setHeight(1);
            } else {
                errSymbolLower.setSymbolType(GChart.SymbolType.LINE);
                errSymbolLower.setFillThickness(1);
                errSymbolLower.setFillSpacing(0);
                errSymbolLower.setWidth(0);
                errSymbolLower.setHeight(0);
            }

            errSymbolLower.setHoverAnnotationEnabled(false);
            double err;
            if (_logScale) {
            for (XYPlotData.Point p : cd.getPoints()) {
                err = p.getError();
                errCurveLower.addPoint(p.getX(), Double.isNaN(err) ? Double.NaN : Math.log10(p.getY()-err));
            }
            } else {
                for (XYPlotData.Point p : cd.getPoints()) {
                    err = p.getError();
                    errCurveLower.addPoint(p.getX(), Double.isNaN(err) ? Double.NaN : p.getY()-err);
                }
            }

            // add error bars
            if (_meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.POINTS)) {
                for (XYPlotData.Point p : cd.getPoints()) {
                    err = getScaled(p.getError());
                    if (!Double.isNaN(err)) {
                        _chart.addCurve();
                        errBarCurve = _chart.getCurve();
                        GChart.Symbol errSymbol= errBarCurve.getSymbol();
                        errSymbol.setBorderColor("lightgray");
                        errSymbol.setBackgroundColor("lightgray");
                        errSymbol.setWidth(1);
                        errSymbol.setModelHeight(2*err);
                        errBarCurve.addPoint(p.getX(), getScaled(p.getY()));
                    }
                }
            }

            //upper error curve
            _chart.addCurve();
            errCurveUpper = _chart.getCurve();

            GChart.Symbol errSymbolUpper= errCurveUpper.getSymbol();
            errSymbolUpper.setBorderColor("lightgray");
            errSymbolUpper.setBackgroundColor("lightgray");
            if (_meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.POINTS)) {
                errSymbolUpper.setWidth(3);
                errSymbolUpper.setHeight(1);
            } else {
                errSymbolUpper.setSymbolType(GChart.SymbolType.LINE);
                errSymbolUpper.setFillThickness(1);
                errSymbolUpper.setFillSpacing(0);
                errSymbolUpper.setWidth(0);
                errSymbolUpper.setHeight(0);
            }
            errSymbolUpper.setHoverAnnotationEnabled(false);
            for (XYPlotData.Point p : cd.getPoints()) {
                err = p.getError();
                errCurveUpper.addPoint(p.getX(), Double.isNaN(err) ? Double.NaN : (getScaled(p.getY()+err)));
            }

            cd.setErrorIdx(_chart.getCurveIndex(errCurveLower), _chart.getCurveIndex(errCurveUpper));
        }
    }

    private void addSpecificPoints() {
        _specificPoints = new ArrayList<SpecificPointUI>();
        int colIdx;
        if (_meta.plotSpecificPoints() && _data.hasSpecificPoints()) {
            MinMax xMinMax = _data.getXMinMax();
            MinMax yMinMax;
            if (_meta.plotError() && _data.hasError()) {
                yMinMax = _data.getWithErrorMinMax();
            }  else {
                yMinMax = _data.getYMinMax();
            }

            SpecificPoints specificPoints = _data.getSpecificPoints();
            specificPointsDesc = specificPoints.getDescription();
            for (int i=0; i<specificPoints.getNumPoints(); i++) {
                SpecificPoints.Point p = specificPoints.getPoint(i);
                colIdx = p.getId() % lightcolors.length;
                MinMax x =  p.getXMinMax();
                MinMax y = p.getYMinMax();
                if (xMinMax.isIn(x.getReference()) &&
                        yMinMax.isIn(y.getReference())) {

                    // simulate point with two lines vertical and horizontal
                    //dotted x-line                    
                    _chart.addCurve();
                    GChart.Curve xCurve = _chart.getCurve();
                    xCurve.setLegendLabel(p.getLabel());

                    GChart.Symbol symbol= xCurve.getSymbol();
                    symbol.setBorderColor(lightcolors[colIdx]);
                    symbol.setBackgroundColor(lightcolors[colIdx]);
                    //symbol.setSymbolType(GChart.SymbolType.LINE);
                    symbol.setFillThickness(1);
                    symbol.setFillSpacing(2);
                    //symbol.setWidth(0);
                    symbol.setHeight(0);
                    symbol.setHoverAnnotationEnabled(false);

                    xCurve.addPoint(x.getMin(), getScaled(y.getReference()));
                    xCurve.addPoint(x.getMax(), getScaled(y.getReference()));

                    //dotted y-line
                    _chart.addCurve();
                    GChart.Curve yCurve = _chart.getCurve();
                    symbol= yCurve.getSymbol();
                    symbol.setBorderColor("black");
                    symbol.setBackgroundColor("black");
                    symbol.setSymbolType(GChart.SymbolType.LINE);
                    symbol.setFillThickness(1);
                    symbol.setFillSpacing(1);
                    symbol.setWidth(0);
                    symbol.setHeight(0);
                    symbol.setHoverAnnotationEnabled(false);

                    yCurve.addPoint(x.getReference(), getScaled(y.getMin()));
                    yCurve.addPoint(x.getReference(), getScaled(y.getMax()));

                    _chart.addCurve();
                    GChart.Curve spCurve = _chart.getCurve();
                    spCurve.addPoint(x.getReference(), y.getReference());
                    spCurve.getPoint().setAnnotationLocation(GChart.AnnotationLocation.NORTHEAST);
                    spCurve.getPoint().setAnnotationFontColor(lightcolors[colIdx]);
                    spCurve.getPoint().setAnnotationFontSize(8);
                    spCurve.getPoint().setAnnotationXShift(-3);
                    spCurve.getPoint().setAnnotationYShift(3);
                    spCurve.getPoint().setAnnotationText(p.getLabel());

                    symbol= spCurve.getSymbol();
                    symbol.setBorderColor("Black");
                    symbol.setBackgroundColor(lightcolors[colIdx]);
                    symbol.setSymbolType(GChart.SymbolType.BOX_CENTER);
                    symbol.setHoverSelectionBackgroundColor("black");
                    symbol.setHoverSelectionBorderColor(lightcolors[colIdx]);
                    symbol.setHoverAnnotationSymbolType(GChart.SymbolType.ANCHOR_NORTHWEST);
                    symbol.setHoverLocation(GChart.AnnotationLocation.NORTHEAST);
                    symbol.setHoverYShift(5);
                    symbol.setHoverSelectionEnabled(true);
                    String template = p.getDesc();
                    symbol.setHovertextTemplate(GChart.formatAsHovertext(template));

                    _specificPoints.add(new SpecificPointUI(p, spCurve, xCurve, yCurve));

                }
            }
            _chart.getXAxis().setOutOfBoundsMultiplier(Double.NaN);
            _chart.getYAxis().setOutOfBoundsMultiplier(Double.NaN);
        }
    }

    private void setChartAxes() {
        MinMax xMinMax = _data.getXMinMax();
        MinMax yMinMax;
        if (_meta.plotError() && _data.hasError()) {
            yMinMax = _data.getWithErrorMinMax();
        }  else {
            yMinMax = _data.getYMinMax();
        }

        xMinMax = MinMax.ensureNonZeroRange(xMinMax);
        yMinMax = MinMax.ensureNonZeroRange(yMinMax);
        setChartAxes(xMinMax, yMinMax);

        // do not check for out of bounds points
        _chart.getXAxis().setOutOfBoundsMultiplier(Double.NaN);
        _chart.getYAxis().setOutOfBoundsMultiplier(Double.NaN);
        _actionHelp.setHTML(rubberbandZooms?ZOOM_IN_HELP:SELECT_HELP);
    }

    private void  setChartAxesForSelection(MinMax xMinMax, MinMax yMinMax) {
        int numPoints = _data.getNPoints(xMinMax, yMinMax);
        if (numPoints > 0) {
            setChartAxes(xMinMax, yMinMax);
            _savedSelection = new Selection(xMinMax, yMinMax);
            // do not render points that are out of bounds
            //_chart.getXAxis().setOutOfBoundsMultiplier(0);
            //if (preserveOutOfBoundPoints || numPoints == 1) {
            //    _chart.getYAxis().setOutOfBoundsMultiplier(Double.NaN);
            //} else {
            //    _chart.getYAxis().setOutOfBoundsMultiplier(0);
            //}
            _actionHelp.setHTML(ZOOM_OUT_HELP);
        }
    }

    private void setChartAxes(MinMax xMinMax, MinMax yMinMax) {
        // set axes min/max and ticks
        GChart.Axis xAxis= _chart.getXAxis();
        GChart.Axis yAxis= _chart.getYAxis();
        String xUnits = getXColUnits();
        xAxis.setAxisLabel(_meta.getXName(_data) + (StringUtils.isEmpty(xUnits) ? "" : ", " + xUnits));
        setLinearScaleAxis(xAxis, xMinMax, TICKS * _xResizeFactor);

        String yName = _meta.getYName(_data);
        Widget yLabel;
        int yLabelLines = 1;
        if (getYColUnits().length() > 0) {
            if  (yName.length()+getYColUnits().length() > 20)  yLabelLines++;
            yLabel =  new HTML(yName + (yLabelLines>1 ? "<br>" : ", ") + getYColUnits());
        } else {
            yLabel =  new HTML(yName);
        }
        yLabel.addStyleName(_ffCss.rotateLeft());
        yAxis.setAxisLabel(yLabel);
        yAxis.setAxisLabelThickness(yLabelLines*20);
        if (_logScale) {
            setLogScaleAxis(yAxis, yMinMax, TICKS*_yResizeFactor);
        } else {
            setLinearScaleAxis(yAxis, yMinMax, TICKS*_yResizeFactor);
        }
    }

    private void setLinearScaleAxis(GChart.Axis axis, MinMax minMax, int maxTicks) {
        NiceScale numScale = new NiceScale(minMax, maxTicks);
        double min = numScale.getNiceMin();
        double max = numScale.getNiceMax();
        int tickCount = (int)Math.round(Math.abs((max-min)/numScale.getTickSpacing()))+1;
        axis.setAxisMin(min);
        axis.setAxisMax(max);
        axis.setHasGridlines(true);

        if (tickCount > 0) { axis.setTickCount(tickCount); }
        String tickLabelFormat = numScale.getFormatString();
        axis.setTickLabelFormat(tickLabelFormat);
        axis.setTickLabelFontSize(10);
    }

    private void setLogScaleAxis(GChart.Axis axis, MinMax minMax, int maxTicks) {
        axis.clearTicks();
        axis.setTickLabelFormat("=10^#.##########");
        axis.setTickLabelFontSize(10);

        double lmin = Math.floor(Math.log10(minMax.getMin()));
        double lmax = Math.ceil(Math.log10(minMax.getMax()));
        axis.setAxisMin(lmin);
        axis.setAxisMax(lmax);
        axis.setHasGridlines(true);

        if (Math.abs(lmax-lmin) <= maxTicks) {
            //show conventional log scale ticks
            axis.addTick(lmin);
            for (double x=Math.pow(10,lmin); x < Math.pow(10,lmax); x*=10)  {
                for (int y = 2; y <= 10; y++) {
                    if (y==10) { axis.addTick(Math.log10(x*y)); }
                    else { axis.addTick(Math.log10(x*y), ""); }
                }
            }
        } else {
            int scale = (Math.abs(lmax-lmin)<=maxTicks) ? 1 : (int)Math.ceil(Math.abs(lmax-lmin)/maxTicks);
            for (double x = lmin; x<=lmax; x+=scale)  {
                axis.addTick(x);
            }
        }
    }

    private String getXColUnits() {
        if (_data == null) {
            return "";
        } else if (_meta.userMeta != null && !StringUtils.isEmpty(_meta.userMeta.xUnit)) {
            return _meta.userMeta.xUnit;
        }
        String xUnits = _data.getXUnits();
        if (xUnits == null || xUnits.trim().length()<1) {
            xUnits = _meta.getDefaultXUnits(_data);
        }
        return xUnits;
    }

    private String getYColUnits() {
        if (_data == null) {
            return "";
        } else if (_meta.userMeta != null && !StringUtils.isEmpty(_meta.userMeta.yUnit)) {
            return _meta.userMeta.yUnit;
        }
        String yUnits = _data.getYUnits();
        if (yUnits == null || yUnits.trim().length()<1) {
            yUnits = _meta.getDefaultYUnits(_data);
        }
        return yUnits;
    }


    private double getError(GChart.Curve.Point hoveredOver) {
        double error = Double.MIN_VALUE;
        if (_data.hasError()) {
            try {
                XYPlotData.Point point = getDataPoint(hoveredOver);
                if (point != null) {
                    error = point.getError();
                }
                //GChart.Curve curve = hoveredOver.getParent();
                //int pointIdx = curve.getPointIndex(hoveredOver);
                //int curveIdx = _chart.getCurveIndex(curve);
                //for (XYPlotData.Curve cd : _data.getCurveData()) {
                //    if (cd.getCurveIdx()== curveIdx) {
                //        error = cd.getPoints().get(pointIdx).getError();
                //    }
                //}
            } catch (Throwable ignored) {}
        }
        return error;
    }

    public List<TableDataView.Column> getColumns() {

        if (_tableModel != null) {
            try {
                if (_tableModel.getTotalRows()>0) {
                    return _tableModel.getCurrentData().getColumns();
                } else if (_dataSet != null) {
                    return _dataSet.getColumns();
                } else {
                    return new ArrayList<TableDataView.Column>(0);
                }
            } catch (Exception e) {
                return new ArrayList<TableDataView.Column>(0);
            }
        }
        return new ArrayList<TableDataView.Column>(0);
    }

    public String getTableInfo() {
        if (_tableModel != null) {
            try {
                boolean filtered = _tableModel.getFilters().size()>0;
               if (_tableModel.getTotalRows() > 0) {
                    boolean tableNotLoaded = !_tableModel.getCurrentData().getMeta().isLoaded();
                    int totalRows = _tableModel.getTotalRows();
                    boolean allPlotted = (totalRows <= _meta.getMaxPoints());
                    return "Data table contains "+_tableModel.getTotalRows()
                            +(tableNotLoaded ? "+" : "")
                            +(filtered ? " filtered":"")+" rows, "+
                            (allPlotted ? "all" : _meta.getMaxPoints()+"")+" plotted."+
                            (allPlotted ? "" : " Set max plotted points in options.");
                } else if (_dataSet != null) {
                    boolean tableNotLoaded = !_dataSet.getMeta().isLoaded();
                    return "Data table contains "+_dataSet.getTotalRows()
                            +(tableNotLoaded ? "+" : "")
                            +(filtered ? " filtered":"")+" rows";
                }
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }


    public void showColumns(Widget alignTo, PopupPane.Align alignAt) {
        if (_dataSet != null) {
            if (showColumnsDialog == null) {
                showColumnsDialog = new ShowColumnsDialog(alignTo, getColumns());
            }
            showColumnsDialog.alignTo(alignTo, alignAt);
            showColumnsDialog.setVisible(true);
        }
    }

    public void widgetResized(int width, int height) {
        _resizeTimer.cancel();
        _resizeTimer.setupCall(width, height);
        _resizeTimer.schedule(RESIZE_DELAY);
//        resize(width, height);
    }

    public void onPostExpandCollapse(boolean expanded) {
        if (_chart != null && !_meta.alwaysShowLegend() && _showLegend != expanded) {
            _showLegend = expanded;
            if (_legend != null) {
                _legend.setVisible(expanded);
            }
            _chart.setLegendVisible(expanded);
            _chart.update();
        }
    }

    private void resize(int width, int height) {
        if (_meta != null) {
            width = _dockPanel.getOffsetWidth();
            height = _dockPanel.getOffsetHeight();
            if (!GwtUtil.DockLayout.isHidden(optionsPanel)) {
                if (width < MIN_SIZE_FOR_DOCKED_OPTIONS) {
                    //hide options
                    GwtUtil.DockLayout.hideWidget(_dockPanel, optionsPanel);
                } else {
                    width = width-OPTIONS_PANEL_WIDTH;
                }
            }
            //int w= (int)((width-100) * .95F);
            //int h= (int)((height-180)* .95F);
            int w= width-100;
            int h = height-180;

            if (_chart != null) {
                h -= 60; // for menu bar and status
                if (_chart.isLegendVisible()) {
                    w -= 100;
                }
            }

            if (w < 150) w = 150;
            if (h < 100) h = 90;
            _xResizeFactor = (int)Math.ceil(w/330.0);
            _yResizeFactor = (int)Math.ceil(h/300.0);

            if (_chart != null && _data != null) {
                if (_savedSelection != null) {
                    setChartAxesForSelection(_savedSelection.xMinMax, _savedSelection.yMinMax);
                } else {
                    setLinearScaleAxis(_chart.getXAxis(), _data.getXMinMax(), TICKS*_xResizeFactor);
                    if (_logScale) {
                        setLogScaleAxis(_chart.getYAxis(), _data.getYMinMax(), TICKS*_yResizeFactor);
                    } else {
                        setLinearScaleAxis(_chart.getYAxis(), _data.getYMinMax(), TICKS*_yResizeFactor);
                    }
                }
            }
            h = (int)Math.min(w*0.6, h);
            //_meta.setChartSize(w, h);

            if (_chart != null) {
                _chart.setChartSize(w, h);
                _chart.update();
            }
        }
    }

    private double getScaled(double val) {
        return _logScale ? Math.log10(val) : val;
    }

    private double getUnscaled(double val) {
        return _logScale ? Math.pow(10, val) : val;
    }

    private XYPlotData.Point getDataPoint(GChart.Curve.Point p) {
        if (_data!=null && _mainCurves.size()>0) {
            int curveIdx = p.getParent().getParent().getCurveIndex(p.getParent());
            int pointIdx = p.getParent().getPointIndex(p);

            if (curveIdx < _mainCurves.size()) {
                return _data.getPoint(curveIdx, pointIdx);
            } else if (_highlightedPoints != null && curveIdx == _chart.getCurveIndex(_highlightedPoints)) {
                return (XYPlotData.Point)_highlightedPoints.getCurveData();
            } else if (_selectedPoints != null && curveIdx == _chart.getCurveIndex(_selectedPoints)) {
                List<XYPlotData.Point> dataPoints = ((SelectedData)_selectedPoints.getCurveData()).getDataPoints();
                return dataPoints.get(pointIdx);
            }
        }
        return null;
    }

    private void setHighlighted(GChart.Curve.Point p) {
        if (p == null) return;

        XYPlotData.Point point = getDataPoint(p);

        boolean doHighlight = true; // we want to unhighlight when clicking on a highlighted point
        if (_highlightedPoints == null || _chart.getCurveIndex(_highlightedPoints)<0) {
            _chart.addCurve();
            _highlightedPoints = _chart.getCurve();
            GChart.Symbol symbol= _highlightedPoints.getSymbol();
            symbol.setBorderColor("black");
            symbol.setBackgroundColor("yellow");
            symbol.setSymbolType(GChart.SymbolType.BOX_CENTER);
            symbol.setHoverSelectionEnabled(true);
            symbol.setHoverAnnotationEnabled(true);

            GChart.Symbol refSym = p.getParent().getSymbol();
            symbol.setBrushHeight(refSym.getBrushHeight());
            symbol.setBrushWidth(refSym.getBrushWidth());
            symbol.setHoverSelectionWidth(refSym.getHoverSelectionWidth());
            symbol.setHoverSelectionHeight(refSym.getHoverSelectionHeight());
            symbol.setHoverSelectionBackgroundColor(symbol.getBackgroundColor());
            symbol.setHoverSelectionBorderColor(refSym.getBorderColor());
            symbol.setHoverAnnotationSymbolType(refSym.getHoverAnnotationSymbolType());
            symbol.setHoverLocation(refSym.getHoverLocation());
            symbol.setHoverYShift(refSym.getHoverYShift());
            symbol.setHovertextTemplate(refSym.getHovertextTemplate());
        } else {
            if (_highlightedPoints.getNPoints() > 0) {
                GChart.Curve.Point currentHighlighted = _highlightedPoints.getPoint();
                //XYPlotData.Point currentPoint = (XYPlotData.Point)_highlightedPoints.getCurveData();

                if (p.getX() == currentHighlighted.getX() && p.getY() == currentHighlighted.getY()) {
                    doHighlight = false;  // unhighlight if a highlighted point is clicked again
                }

                // unhighlight
                _highlightedPoints.clearPoints();
            }
        }

        // highlight
        if (doHighlight && point != null) {
            _highlightedPoints.setCurveData(point);
            _highlightedPoints.addPoint(p.getX(), p.getY());
            //_highlightedPoints.getSymbol().setHovertextTemplate(p.getHovertext());
            if (_tableModel.getCurrentData()!=null) {
                _tableModel.getCurrentData().highlight(point.getRowIdx());
            }
        }
        _chart.update();
    }

    private void setSelected(MinMax xMinMax, MinMax yMinMax) {

        if (_mainCurves.size() < 1) return;
        if (_selectedPoints == null || _chart.getCurveIndex(_selectedPoints)<0) {
            _chart.addCurve();
            _selectedPoints = _chart.getCurve();
            GChart.Symbol symbol= _selectedPoints.getSymbol();
            symbol.setBorderColor("black");
            symbol.setBackgroundColor("#99ff33");
            symbol.setSymbolType(GChart.SymbolType.BOX_CENTER);
            symbol.setHoverSelectionEnabled(true);
            symbol.setHoverAnnotationEnabled(true);

            GChart.Symbol refSym = _mainCurves.get(0).getSymbol();
            symbol.setBrushHeight(refSym.getBrushHeight());
            symbol.setBrushWidth(refSym.getBrushWidth());
            symbol.setHoverSelectionWidth(refSym.getHoverSelectionWidth());
            symbol.setHoverSelectionHeight(refSym.getHoverSelectionHeight());
            symbol.setHoverSelectionBackgroundColor(symbol.getBackgroundColor());
            symbol.setHoverSelectionBorderColor(refSym.getBorderColor());
            symbol.setHoverAnnotationSymbolType(refSym.getHoverAnnotationSymbolType());
            symbol.setHoverLocation(refSym.getHoverLocation());
            symbol.setHoverYShift(refSym.getHoverYShift());
            symbol.setHovertextTemplate(refSym.getHovertextTemplate());
        } else {
            _selectedPoints.clearPoints();
        }
        _actionHelp.setHTML(SELECT_HELP);

        double xMin = xMinMax.getMin();
        double xMax = xMinMax.getMax();
        double yMin = yMinMax.getMin();
        double yMax = yMinMax.getMax();

        double x,y;
        List<XYPlotData.Point> dataPoints = new ArrayList<XYPlotData.Point>();
        for (XYPlotData.Curve c : _data.getCurveData()) {
            for (XYPlotData.Point p : c.getPoints()) {
                x = p.getX();
                y = p.getY();
                if (x > xMin && x < xMax && y > yMin && y < yMax) {
                    _selectedPoints.addPoint(x, y);
                    dataPoints.add(p);
                }
            }
        }
        _selectedPoints.setCurveData(new SelectedData(xMinMax, yMinMax, dataPoints));

      // set selected rows
        if (dataPoints.size() > 0) {
            Integer [] selected = new Integer[dataPoints.size()];
            int i = 0;
            for (XYPlotData.Point p : dataPoints) {
                selected[i] = p.getRowIdx();
                i++;
            }
            if (_tableModel.getCurrentData()!=null) {
                _tableModel.getCurrentData().select(selected);
            }
            _actionHelp.setHTML(UNSELECT_HELP);
            filterSelectedLink.setVisible(true);
        } else {
            filterSelectedLink.setVisible(false);
        }
        _chart.update();
    }

    class Selection {
        MinMax xMinMax;
        MinMax yMinMax;
        Selection(MinMax xMinMax, MinMax yMinMax) {
            this.xMinMax = xMinMax;
            this.yMinMax = yMinMax;
        }
    }

    class XYHoverParameterInterpreter implements
            HoverParameterInterpreter {
        public String getHoverParameter(String paramName,
                                        GChart.Curve.Point hoveredOver) {
            String result = null;

            XYPlotData.Point point = getDataPoint(hoveredOver);
            if (point != null) {
                if ("x".equals(paramName))
                    result = point.getXStr();
                else if ("y".equals(paramName))
                    result = point.getYStr();
                else if ("err".equals(paramName)) {
                    result = point.getErrorStr();
                }
            } else {
                if ("x".equals(paramName))
                    result = _chart.getXAxis().formatAsTickLabel(hoveredOver.getX());
                else if ("y".equals(paramName))
                    result = _chart.getYAxis().formatAsTickLabel(hoveredOver.getY());
                else if ("err".equals(paramName)) {
                    result = _chart.getYAxis().formatAsTickLabel(getError(hoveredOver));
                }
            }
            return result;

        }
    }

    class SpecificPointUI {
        SpecificPoints.Point p;
        GChart.Curve spCurve;
        GChart.Curve xCurve;
        GChart.Curve yCurve;

        SpecificPointUI(SpecificPoints.Point p, GChart.Curve spCurve, GChart.Curve xCurve, GChart.Curve yCurve) {
            this.p = p;
            this.spCurve = spCurve;
            this.xCurve = xCurve;
            this.yCurve = yCurve;
        }

        void setVisible(boolean visible) {
            this.xCurve.setVisible(visible);
            this.yCurve.setVisible(visible);
            this.spCurve.setVisible(visible);
        }

    }

    private class ResizeTimer extends Timer {
        private int w= 0;
        private int h= 0;

        public void setupCall(int w, int h) {
            this.w= w;
            this.h= h;
        }

        @Override
        public void run() { resize(w,h); }
    }

    private Label getFilterSelectedLink() {
        return GwtUtil.makeLinkButton("Filter Selected",
                "Filter the data to leave only the selected points",
                new ClickHandler() {
                    public void onClick(ClickEvent clickEvent) {
                        // can filter when there are some selected points and when both x and y are not expressions
                        if (_selectedPoints == null || _chart.getCurveIndex(_selectedPoints) < 0 || _selectedPoints.getNPoints()<1) {
                            PopupUtil.showError("Nothing to filter", "Nothing selected");
                            return;
                        } else if (_data == null || _data.getXCol().length()==0 || _data.getYCol().length()==0) {
                            PopupUtil.showError("Unable to filter", "X or Y column is an expression. Unable to filter expressions.");
                            return;
                        }
                        if (_selectedPoints != null && _chart.getCurveIndex(_selectedPoints)>=0 &&
                                _selectedPoints.getNPoints()>0 &&
                                _data != null && _data.getXCol().length()>0 && _data.getYCol().length()>0) {
                            SelectedData selectedData = (SelectedData)_selectedPoints.getCurveData();
                            String xCol = _data.getXCol();
                            String yCol = _data.getYCol();
                            MinMax xMinMax = selectedData.getXMinMax();
                            MinMax yMinMax = selectedData.getYMinMax();

                            List<String> currentFilters = _tableModel.getFilters();
                            currentFilters.add(xCol+" > "+XYPlotData.formatValue(xMinMax.getMin()));
                            currentFilters.add(xCol+" < "+XYPlotData.formatValue(xMinMax.getMax()));
                            currentFilters.add(yCol+" > "+XYPlotData.formatValue(yMinMax.getMin()));
                            currentFilters.add(yCol+" < "+XYPlotData.formatValue(yMinMax.getMax()));
                            _tableModel.fireDataStaleEvent();
                            filterSelectedLink.setVisible(false);
                        } else {
                            PopupUtil.showError("Unable to filter", "Unable to Filter");
                        }
                    }
                });
    }

    public void toggleFilters() {
        if (popoutFilters == null) {
            final FilterPanel filterPanel = new FilterPanel(getColumns());
            popoutFilters = new FilterDialog(_filters, filterPanel);
            popoutFilters.setApplyListener(new GeneralCommand("Apply") {
                        @Override
                        protected void doExecute() {
                            _tableModel.setFilters(filterPanel.getFilters());
                            _tableModel.fireDataStaleEvent();
                        }
                    });

        }
        if (popoutFilters.isVisible()) {
            popoutFilters.setVisible(false);
        } else {
            popoutFilters.getFilterPanel().setFilters(_tableModel.getFilters());
            popoutFilters.show(0, PopupPane.Align.BOTTOM_LEFT);
        }
    }

    public List<String> getFilters() {
        return _tableModel.getFilters();
    }

    public void clearFilters() {
        _tableModel.setFilters(null);
        _tableModel.fireDataStaleEvent();
    }


    private static class ShowColumnsDialog extends BaseDialog {

        public ShowColumnsDialog(Widget parent, List<TableDataView.Column> cols) {
            super(parent, ButtonType.REMOVE, "Available columns", "visualization.xyplotViewer");
            Button b = this.getButton(BaseDialog.ButtonID.REMOVE);
            b.setText("Close");

            BaseTableData defTD = new BaseTableData(new String[]{"Column", "Units", "Type", "Description"});
            for (TableDataView.Column c : cols) {
                String units = c.getUnits();
                defTD.addRow(new String[]{c.getName(), StringUtils.isEmpty(units)? "" : units, c.getType(), c.getShortDesc()});
            }
            DataSet defDS = new DataSet(defTD);
            BasicTable table = new BasicTable(defDS);
            table.setColumnWidth(0, 80);
            table.setColumnWidth(1, 50);
            table.setColumnWidth(2, 50);
            table.setColumnWidth(3, 100);
            table.addStyleName("expand-fully");
            InfoPanel infoPanel = new InfoPanel();
            //infoPanel.setSize("310px", "310px");
            infoPanel.setWidget(table);
            setWidget(infoPanel);
            setDefaultContentSize(330, 200);
        }
    }

    private static class InfoPanel extends SimplePanel implements RequiresResize {
        public void onResize() {
            String height = this.getParent().getOffsetHeight()+"px";
            String width = this.getParent().getOffsetWidth()+"px";
            this.setSize(width, height);
            Widget w = this.getWidget();
            if (w instanceof BasicTable) {
                resizeTable((BasicTable) w, getParent().getOffsetWidth(),getParent().getOffsetHeight());
            }
        }

        private void resizeTable(BasicTable t, int width, int height) {
            int colCount= t.getDataTable().getColumnCount();
            int beforeLastColumnWidth = 0;
            int lastColWidth;
            if (colCount > 1) {
                for (int i=0; i<colCount-1;i++) {
                    beforeLastColumnWidth += t.getColumnWidth(i);
                }
                lastColWidth = width - beforeLastColumnWidth;
                if (lastColWidth > 50) {
                    t.setColumnWidth(colCount-1, lastColWidth-50);
                }
            }
            t.setSize(width+"px", height+"px");
        }
    }

    private static class SelectedData {
        MinMax _xMinMax;
        MinMax _yMinMax;
        List<XYPlotData.Point> _dataPoints;
        SelectedData(MinMax xMinMax, MinMax yMinMax, List<XYPlotData.Point> dataPoints) {
            _xMinMax = xMinMax;
            _yMinMax = yMinMax;
            _dataPoints = dataPoints;
        }

        MinMax getXMinMax() {return _xMinMax;}
        MinMax getYMinMax() {return _yMinMax;}
        List<XYPlotData.Point> getDataPoints() { return _dataPoints; }
    }

    public static interface NewDataListener {
        public void newData(XYPlotData data);
    }
}