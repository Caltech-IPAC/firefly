package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.core.client.GWT;
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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.BaseTableData;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.rpc.PlotService;
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
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tatianag
 * $Id $
 */
public class XYPlotWidget extends PopoutWidget {


    // colors that color blind people can distinguish
    // http://safecolours.rigdenage.com/Comp10.jpg
    // plus the colors that are 3 stops darker
    // see http://www.w3schools.com/tags/ref_colorpicker.asp
    private static String [] colors = {"#000000", "#ff3333", "#00ccff","#336600",
              "#9900cc", "#ff9933", "#009999", "#66ff33", "#cc9999",
            "#333333", "#b22424", "#008fb2", "#244700",
        "#6b008f", "#b26b24", "#006b6b", "#47b224", "8F6B6B"};
    
    // CSS light colors
    private static String [] lightcolors = {"MediumPurple", "LightCoral", "LightBlue", "Olive",
              "Plum", "LightSalmon", "SandyBrown", "PaleTurquoise", "YellowGreen",
              "LightPink", "CornflowerBlue", "Khaki", "PaleGreen", "LightSteelBlue"};


    private static final int RESIZE_DELAY= 100;
    ScrollPanel _panel= new ScrollPanel();
    VerticalPanel _vertPanel = new VerticalPanel(); // for chart, options, etc.
    SimplePanel _cpanel= new SimplePanel(); // for chart
    private final MaskMessgeWidget _maskMessge = new MaskMessgeWidget(false);
    private final MaskPane _maskPane=
            new MaskPane(_panel, _maskMessge);
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

    ArrayList<GChart.Curve> _mainCurves;
    ArrayList<SpecificPointUI> _specificPoints;
    String specificPointsDesc;
    GChart.Curve _selectionCurve;
    boolean _selecting = false;
    Selection _savedSelection = null;
    //boolean preserveOutOfBoundPoints = false;
    private VerticalPanel footnotesZoomOut;
    private VerticalPanel footnotesZoomIn;
    private XYPlotOptionsDialog optionsDialog;
    private ShowColumnsDialog showColumnsDialog;
    private ResizeTimer _resizeTimer= new ResizeTimer();

    private List<NewDataListener> _listeners = new ArrayList<NewDataListener>();

    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();

    public XYPlotWidget(XYPlotMeta meta) {
        //super(150, 90); long labels will get wrapped
        super(new PopupContainerForToolbar(), 300, 180);
        _meta = meta;
        AllPlots.getInstance().registerPopout(this);
        GChart.setCanvasFactory(ChartingFactory.getInstance());
        layout();
        _popoutWidgetSet = false;

        // set zoom-out widget
        HorizontalPanel zoomOut = new HorizontalPanel();
        zoomOut.setWidth("100%");
        Widget zoomOutLink = GwtUtil.makeLinkButton("Zoom out to original chart", "Zoom out to include all data points", new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (_data != null) {
                    setChartAxes();
                    _savedSelection = null;
                    _chart.update();
                }
            }
        });
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
        zoomOut.add(zoomOutLink);
        //Label spacer1 = new Label();
        //spacer1.setWidth("30px");
        //zoomOut.add(spacer1);
        //zoomOut.add(outOfBoundCheck);
        footnotesZoomOut = new VerticalPanel();
        footnotesZoomOut.add(zoomOut);

        // set zoom help
        Widget zoomHelp = new HTML("Rubber band zoom &mdash; click and drag an area to zoom in.");
        zoomHelp.addStyleName(_ffCss.highlightText());

        // set zoom-out widget
        HorizontalPanel zoomIn = new HorizontalPanel();
        zoomIn.setWidth("100%");
        zoomIn.add(zoomHelp);

        footnotesZoomIn = new VerticalPanel();
        footnotesZoomIn.add(zoomIn);
    }

    private Widget makeOptionsWidget() {
        return GwtUtil.makeLinkButton("Chart Options", "Chart Options", new ClickHandler() {
            public void onClick(ClickEvent event) {
                showOptionsDialog();
            }
        });
    }

    private void showOptionsDialog() {
        if (optionsDialog == null) {
            optionsDialog = new XYPlotOptionsDialog(XYPlotWidget.this);
        }
        optionsDialog.setVisible(true);

    }

    private Widget makeDownloadWidget() {
        return GwtUtil.makeLinkButton("Download Data Table", "Download data in IPAC table format", new ClickHandler() {
            public void onClick(ClickEvent event) {
                Frame f= Application.getInstance().getNullFrame();
                String url;
                if (_sourceFile.contains("://")) {
                    url = _sourceFile;
                } else {
                    Param [] params;
                    if (_suggestedName != null) {
                        params = new Param[3];
                        params[2] = new Param("return", _suggestedName);
                    } else {
                        params = new Param[2];
                    }
                    params[0] = new Param("file", _sourceFile);
                    params[1] = new Param("log", "true");
                    url= WebUtil.encodeUrl(GWT.getModuleBaseURL()+ "servlet/Download", params);
                }
                f.setUrl(url);
            }
        });
    }

    public void makeNewChart(WebPlotRequest request, String title) {
        _selecting = false;
        _savedSelection = null;

        if (!_popoutWidgetSet) {
            Widget bottomWidget = GwtUtil.leftRightAlign(new Widget[]{makeOptionsWidget()},
                new Widget[]{new HTML("&nbsp;"), makeDownloadWidget(), new HTML("&nbsp;"), HelpManager.makeHelpIcon("visualization.xyplotViewer")});
            bottomWidget.setWidth("100%");
            _vertPanel.setSpacing(5);
            _vertPanel.add(_cpanel);
            _vertPanel.add(bottomWidget);
            _vertPanel.setWidth("100%");
            //_panel.setWidget(_vertPanel);
            setPopoutWidget(_panel);
            _popoutWidgetSet = true;
        }
        setTitle(title);
        removeCurrentChart();
        if (_chart == null) {
            _chart = new GChart(_meta.getXSize(), _meta.getYSize());
            _chart.setOptimizeForMemory(true);
            _chart.setPadding("5px");
            _chart.setLegendBorderWidth(0); // no border
            _chart.setHoverParameterInterpreter(new XYHoverParameterInterpreter());
            _chart.setBackgroundColor("white");
            _chart.setClipToPlotArea(true);
            _chart.setClipToDecoratedChart(true);
            _chart.setChartFootnotesLeftJustified(true);
            addMouseListeners();
            _cpanel.setWidget(_chart);
        } else {
            _chart.setChartSize(_meta.getXSize(), _meta.getYSize());
        }
        _chart.setOptimizeForMemory(true);
        _chart.setPadding("5px");
        // if we are not showing legend, inform the chart
        _chart.setLegendVisible(_showLegend || _meta.alwaysShowLegend());
        _chart.setHoverParameterInterpreter(new XYHoverParameterInterpreter());
        _chart.setBackgroundColor("white");
        addData(request);
    }

    public void removeCurrentChart() {
        if (_chart != null) {
            _chart.clearCurves();
            _panel.remove(_vertPanel);
           //_chart = null;
        }
    }

    public XYPlotMeta getPlotMeta() {
        return _meta;
    }


    public XYPlotData getPlotData() {
        return _data;
    }

    public int getDataSetSize() {
        if (_dataSet != null) {
            return _dataSet.getSize();
        } else {
            return 0;
        }
    }

    public void addListener(NewDataListener l) {
        _listeners.add(l);
    }

    private void addMouseListeners() {

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
                    _selectionCurve.addPoint(x,y);
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
                    setChartAxesForSelection(_selectionCurve);
                    _chart.update();
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
        if (!Drawer.isModernDrawing()) symbol.setFillSpacing(2); // to use old HTML style lines without canvas
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
                        //int cIdxChart = _chart.getCurveIndex(c);
                        //int lowerErrIdx = cIdxChart - cIdx - _mainCurves.size()*2 + cIdx*2;
                        //int upperErrIdx = lowerErrIdx+1;
                        XYPlotData.Curve current = _data.getCurveData().get(cIdx);
                        int lowerErrIdx = current.getErrorLowerCurveIdx();
                        int upperErrIdx = current.getErrorUpperCurveIdx();

                        try {
                            for (int i=lowerErrIdx; i<=upperErrIdx; i++) {
                                _chart.getCurve(i).setVisible(visible);
                            }
                            //_chart.getCurve(lowerErrIdx).setVisible(visible);
                            //_chart.getCurve(upperErrIdx).setVisible(visible);
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

    private void addData(final WebPlotRequest request) {
        _maskPane.hide();
        _dataSet = null;
        if (showColumnsDialog != null) { showColumnsDialog.setVisible(false); showColumnsDialog = null; }
        ServerTask task = new ServerTask<WebPlotResult>(_panel, "Retrieving Data...", true) {

            public void onSuccess(WebPlotResult result) {
                try {
                    if (result.isSuccess()) {
                        DataEntry re =  result.getResult(WebPlotResult.RAW_DATA_SET);
                        if (re instanceof DataEntry.RawDataSetResult) {
                            _chart.clearCurves();
                            RawDataSet rawDataSet = ((DataEntry.RawDataSetResult)re).getRawDataSet();
                            _dataSet = DataSetParser.parse(rawDataSet);
                            // check if DOWNLOAD_SOURCE attribute present:
                            String downloadSource = _dataSet.getMeta().getAttribute("DOWNLOAD_SOURCE");
                            if (StringUtils.isEmpty(downloadSource)) {
                                // use TableServerRequest, if available, to get source table url 
                                String reqStr = request.getParam(ServerParams.REQUEST);
                                if (!StringUtils.isEmpty(reqStr)) {
                                    TableServerRequest sreq = TableServerRequest.parse(reqStr);
                                    _sourceFile = WebUtil.getTableSourceUrl(sreq);
                                } else {
                                    _sourceFile = rawDataSet.getMeta().getSource();
                                }
                            } else {
                                _sourceFile = downloadSource;
                            }

                            // Not sure how to pass suggested file name
                            // using base name for URLs, otherwise nothing
                            if (request.getURL() != null) {
                                _suggestedName = getBaseName(request.getURL());
                            } else {
                                _suggestedName = null;
                            }
                            addData(_dataSet);
                            _selectionCurve = getSelectionCurve();
                            _panel.setWidget(_vertPanel);
                            if (optionsDialog != null && (optionsDialog.isVisible() || _meta.hasUserMeta())) {
                                if (!optionsDialog.setup()) {
                                    if (!optionsDialog.isVisible()) showOptionsDialog();
                                }
                            }
                        } else {
                            showMask("Failed to get IPAC Table");
                        }
                    } else {
                        showMask("Failed to get data");
                    }
                } catch (Throwable e) {
                    if (e.getMessage().indexOf("column is not found") > 0) {
                        _chart.clearCurves();
                        _panel.setWidget(_vertPanel);
                        showOptionsDialog();
                    } else {
                        showMask(e.getMessage());
                    }
                }
            }

            public void onFailure(Throwable e) {
                showMask(e.getMessage());
            }

            public void doTask(AsyncCallback<WebPlotResult> passAlong) {
                PlotService.App.getInstance().getTableData(request, passAlong);
            }
        };
        task.start();
   }

    private String getBaseName(String filePath) {
        String basename = "xysource.tbl";
        if (!StringUtils.isEmpty(filePath)) {
            String[] parts = filePath.split("/");
            if (parts.length > 0) {
                basename =  parts[parts.length-1];
            }
        }
        return basename;
    }


    public void updateMeta(XYPlotMeta meta, boolean preserveZoomSelection) {
        try {
            _meta = meta;
            if (_chart != null) {
                _chart.clearCurves();
            }
            if (_dataSet != null) {
                addData(_dataSet);
                _selectionCurve = getSelectionCurve();
                if (_savedSelection != null && preserveZoomSelection) {
                    setChartAxesForSelection(_savedSelection.xMinMax, _savedSelection.yMinMax);
                    _chart.update();
                } else {
                    _savedSelection = null;
                }
            }
            //_meta.addUserColumnsToDefault();
        } catch (Throwable e) {
            if (_chart != null) _chart.clearCurves();
            //_panel.setWidget(_vertPanel);
            PopupUtil.showError("Error",e.getMessage());
        }
    }

    private void addData(DataSet dataSet) {

        _data = new XYPlotData(dataSet, _meta);

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
        _chart.setChartTitleThickness(70);
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
                if (!Drawer.isModernDrawing()) symbol.setFillSpacing(1); // to use old HTML style lines without canvas
                symbol.setFillThickness(1);
                symbol.setWidth(3);
                symbol.setHeight(3);
            } else {
                symbol.setSymbolType(GChart.SymbolType.LINE);
                if (!Drawer.isModernDrawing()) symbol.setFillSpacing(2); // to use old HTML style lines without canvas
                symbol.setFillThickness(2);
                symbol.setWidth(0);
                symbol.setHeight(0);
            }
            symbol.setBackgroundColor(symbol.getBorderColor()); // make center of the markers filled
            symbol.setBrushHeight(2*_meta.getYSize());
            symbol.setBrushWidth(10);


            symbol.setHoverSelectionWidth(4);
            symbol.setHoverSelectionHeight(4);
            symbol.setHoverSelectionBackgroundColor("black");
            symbol.setHoverSelectionBorderColor(symbol.getBorderColor());
            // annotation on top of grid line (above chart)
            //symbol.setHoverAnnotationSymbolType(GChart.SymbolType.XGRIDLINE);
            //symbol.setHoverLocation(GChart.AnnotationLocation.NORTH);
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
            for (XYPlotData.Point p : cd.getPoints()) {
                curve.addPoint(p.getX(), p.getY());
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
                if (!Drawer.isModernDrawing()) errSymbolLower.setFillSpacing(1); // to use old HTML style lines without canvas
                errSymbolLower.setFillThickness(1);
                errSymbolLower.setWidth(0);
                errSymbolLower.setHeight(0);
            }

            errSymbolLower.setHoverAnnotationEnabled(false);
            double err;
            for (XYPlotData.Point p : cd.getPoints()) {
                err = p.getError();
                errCurveLower.addPoint(p.getX(), Double.isNaN(err) ? Double.NaN : (p.getY()-err));
            }

            // add error bars
            if (_meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.POINTS)) {
                for (XYPlotData.Point p : cd.getPoints()) {
                    err = p.getError();
                    if (!Double.isNaN(err)) {
                        _chart.addCurve();
                        errBarCurve = _chart.getCurve();
                        GChart.Symbol errSymbol= errBarCurve.getSymbol();
                        errSymbol.setBorderColor("lightgray");
                        errSymbol.setBackgroundColor("lightgray");
                        errSymbol.setWidth(1);
                        errSymbol.setModelHeight(2*err);
                        errBarCurve.addPoint(p.getX(), p.getY());
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
                if (!Drawer.isModernDrawing()) errSymbolUpper.setFillSpacing(1); // to use old HTML style lines without canvas
                errSymbolUpper.setFillThickness(1);
                errSymbolUpper.setWidth(0);
                errSymbolUpper.setHeight(0);
            }
            errSymbolUpper.setHoverAnnotationEnabled(false);
            for (XYPlotData.Point p : cd.getPoints()) {
                err = p.getError();
                errCurveUpper.addPoint(p.getX(), Double.isNaN(err) ? Double.NaN : (p.getY()+err));
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
                    if (!Drawer.isModernDrawing()) symbol.setFillSpacing(3); // to use old HTML style lines without canvas
                    symbol.setFillThickness(1);
                    symbol.setFillSpacing(3);
                    //symbol.setWidth(0);
                    symbol.setHeight(0);
                    symbol.setHoverAnnotationEnabled(false);

                    xCurve.addPoint(x.getMin(), y.getReference());
                    xCurve.addPoint(x.getMax(), y.getReference());

                    //dotted y-line
                    _chart.addCurve();
                    GChart.Curve yCurve = _chart.getCurve();
                    symbol= yCurve.getSymbol();
                    symbol.setBorderColor("black");
                    symbol.setBackgroundColor("black");
                    symbol.setSymbolType(GChart.SymbolType.LINE);
                    if (!Drawer.isModernDrawing()) symbol.setFillSpacing(1); // to use old HTML style lines without canvas
                    symbol.setFillThickness(1);
                    symbol.setFillSpacing(1);
                    symbol.setWidth(0);
                    symbol.setHeight(0);
                    symbol.setHoverAnnotationEnabled(false);

                    yCurve.addPoint(x.getReference(), y.getMin());
                    yCurve.addPoint(x.getReference(), y.getMax());

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
                    symbol.setHoverLocation(GChart.AnnotationLocation.NORTH);
                    symbol.setHoverXShift(50);
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
        _chart.setChartFootnotes(footnotesZoomIn);
    }

    private void setChartAxesForSelection(GChart.Curve selectionCurve) {
        if (selectionCurve.getNPoints() < 4 || _data == null) return;
        // diagonal points of the selection rectangle
        GChart.Curve.Point p0 = selectionCurve.getPoint(0);
        GChart.Curve.Point p2 = selectionCurve.getPoint(2);
        double xMin = Math.min(p0.getX(), p2.getX());
        double xMax = Math.max(p0.getX(), p2.getX());
        double yMin = Math.min(p0.getY(), p2.getY());
        double yMax = Math.max(p0.getY(), p2.getY());
        MinMax xMinMax = new MinMax(xMin, xMax);
        MinMax yMinMax = new MinMax(yMin, yMax);

        setChartAxesForSelection(xMinMax, yMinMax);
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
            _chart.setChartFootnotes(footnotesZoomOut);
        }
    }

    private void setChartAxes(MinMax xMinMax, MinMax yMinMax) {
        // set axes min/max and ticks
        GChart.Axis xAxis= _chart.getXAxis();
        GChart.Axis yAxis= _chart.getYAxis();
        String xUnits = getXColUnits();
        xAxis.setAxisLabel(_meta.getXName(_data)+(StringUtils.isEmpty(xUnits) ? "" : ", "+xUnits));
        setAxis(xAxis, xMinMax, TICKS*_xResizeFactor);

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
        setAxis(yAxis, yMinMax, TICKS*_yResizeFactor);

    }

    private void setAxis(GChart.Axis axis, MinMax minMax, int maxTicks) {
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

    private void layout() {
    }

    private String getXColUnits() {
        if (_data == null) return "";
        String xUnits = _data.getXUnits();
        if (xUnits == null || xUnits.trim().length()<1) {
            xUnits = _meta.getDefaultXUnits(_data);
        }
        return xUnits;
    }

    private String getYColUnits() {
        if (_data == null) return "";
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
                GChart.Curve curve = hoveredOver.getParent();
                int pointIdx = curve.getPointIndex(hoveredOver);
                int curveIdx = _chart.getCurveIndex(curve);
                for (XYPlotData.Curve cd : _data.getCurveData()) {
                    if (cd.getCurveIdx()== curveIdx) {
                        error = cd.getPoints().get(pointIdx).getError();
                    }
                }
            } catch (Throwable ignored) {}
        }
        return error;
    }

    public List<TableDataView.Column> getColumns() {
        if (_dataSet == null) return new ArrayList<TableDataView.Column>(0);
        return _dataSet.getColumns();
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
        _resizeTimer.setupCall(width,height);
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
            int w= (int)((width-100) * .95F);
            int h= (int)((height-180)* .95F);

            if (_chart != null) {
                h -= 60; // for options line
                if (_chart.isLegendVisible()) {
                    w -= 100;
                }
            }

            if (w < 150) w = 150;
            if (h < 90) h = 90;
            _xResizeFactor = (int)Math.ceil(w/300.0);
            _yResizeFactor = (int)Math.ceil(h/300.0);

            if (_chart != null && _data != null) {
                if (_savedSelection != null) {
                    setChartAxesForSelection(_savedSelection.xMinMax, _savedSelection.yMinMax);
                } else {
                    setAxis(_chart.getXAxis(), _data.getXMinMax(), TICKS*_xResizeFactor);
                    setAxis(_chart.getYAxis(), _data.getYMinMax(), TICKS*_yResizeFactor);
                }
            }
            h = (int)Math.min(w*0.6, h);
            _meta.setChartSize(w, h);

            if (_chart != null) {
                _chart.setXChartSize(w);
                _chart.setYChartSize(h);
                _chart.update();
            }
        }
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

            XYPlotData.Point point = null;
            if (_data!=null) {
                int curveIdx = hoveredOver.getParent().getParent().getCurveIndex(hoveredOver.getParent());
                int pointIdx = hoveredOver.getParent().getPointIndex(hoveredOver);
                point = _data.getPoint(curveIdx, pointIdx);
            }
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

    private class ShowColumnsDialog extends BaseDialog {

        public ShowColumnsDialog(Widget parent, List<TableDataView.Column> cols) {
            super(parent, ButtonType.REMOVE, "Available columns", "visualization.xyplotViewer");
            Button b = this.getButton(BaseDialog.ButtonID.REMOVE);
            b.setText("Close");

            BaseTableData defTD = new BaseTableData(new String[]{"Column", "Units", "Type"});
            for (TableDataView.Column c : cols) {
                String units = c.getUnits();
                defTD.addRow(new String[]{c.getName(), StringUtils.isEmpty(units)? "" : units, c.getType()});
            }
            DataSet defDS = new DataSet(defTD);
            defDS.getColumn(0).setPrefWidth(100);
            defDS.getColumn(1).setPrefWidth(50);
            defDS.getColumn(2).setPrefWidth(50);


            BasicTable table = new BasicTable(defDS);
            table.setSize("260px", "160px");

            setWidget(table);
        }
    }

    public static interface NewDataListener {
        public void newData(XYPlotData data);
    }
}