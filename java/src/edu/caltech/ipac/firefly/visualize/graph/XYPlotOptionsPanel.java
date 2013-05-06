package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.form.DoubleFieldDef;
import edu.caltech.ipac.firefly.data.table.TableDataView;
//import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.firefly.ui.input.InputField;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.ui.input.SuggestBoxInputField;
import edu.caltech.ipac.firefly.ui.input.ValidationInputField;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.firefly.util.expr.Expression;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDef;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tatianag
 *         $Id: $
 */
public class XYPlotOptionsPanel extends Composite {
    private static WebClassProperties _prop= new WebClassProperties(XYPlotOptionsDialog.class);
    private final XYPlotWidget _xyPlotWidget;

    private MinMaxPanel xMinMaxPanel;
    private MinMaxPanel yMinMaxPanel;
    private HTML xMinMaxPanelDesc;
    private HTML yMinMaxPanelDesc;
    private SimpleInputField plotDataPoints;
    private CheckBox plotError;
    private CheckBox plotSpecificPoints;
    private CheckBox logScale;
    private InputField xColFld;
    private InputField yColFld;
    private Expression xColExpr;
    private Expression yColExpr;
    private InputField xNameFld;
    private InputField xUnitFld;
    private InputField yNameFld;
    private InputField yUnitFld;
    private List<String> numericCols;
    private SimpleInputField maxPoints;
    private boolean setupOK = true;

    ScrollPanel _mainPanel = new ScrollPanel();
    private static boolean suspendEvents = false;

    public XYPlotOptionsPanel(XYPlotWidget widget) {
        _xyPlotWidget = widget;
        layout(widget.getPlotData());
        _xyPlotWidget.addListener(new XYPlotWidget.NewDataListener() {
             public void newData(XYPlotData data) {
                 suspendEvents = true;
                 setup();
                 suspendEvents = false;
             }
         });

        this.initWidget(_mainPanel);
    }

    public void setVisible(boolean v) {
        if (v) {
            setup();
        }
        super.setVisible(v);
    }


    private void layout(final XYPlotData data) {

        // Plot Error
        plotError = GwtUtil.makeCheckBox("XYPlotOptionsDialog.plotError");
        plotError.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (plotError.getValue() && !plotError.isEnabled()) {
                    // should not happen
                } else {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                    meta.setPlotError(plotError.getValue());
                    _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                }
            }
        });

        // Plot Specific Points
        plotSpecificPoints = GwtUtil.makeCheckBox("XYPlotOptionsDialog.plotSpecificPoints");
        plotSpecificPoints.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (plotSpecificPoints.getValue() && !plotSpecificPoints.isEnabled()) {
                    //should not happen
                } else {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                    meta.setPlotSpecificPoints(plotSpecificPoints.getValue());
                    _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                }
            }
        });

        // Alternative Columns
        HTML colPanelDesc = GwtUtil.makeFaddedHelp(
                "For X and Y, enter a column or an expression<br>"+
                "ex. log(col); 100*col1/col2; col1-col2");

        ColExpressionOracle oracle = new ColExpressionOracle();
        FieldDef xColFD = FieldDefCreator.makeFieldDef("XYPlotOptionsDialog.x.col");
        xColFld = new ValidationInputField(new SuggestBoxInputField(xColFD, oracle));
        FieldDef yColFD = FieldDefCreator.makeFieldDef("XYPlotOptionsDialog.y.col");
        yColFld = new ValidationInputField(new SuggestBoxInputField(yColFD, oracle));

        FormBuilder.Config config = new FormBuilder.Config(FormBuilder.Config.Direction.VERTICAL,
                50, 0, HorizontalPanel.ALIGN_LEFT);
        xNameFld = FormBuilder.createField("XYPlotOptionsDialog.x.name");
        xUnitFld = FormBuilder.createField("XYPlotOptionsDialog.x.unit");
        Widget xNameUnit = FormBuilder.createPanel(config, xNameFld, xUnitFld);
        CollapsiblePanel xNameUnitCP = new CollapsiblePanel("X Name/Unit", xNameUnit, false);

        yNameFld = FormBuilder.createField("XYPlotOptionsDialog.y.name");
        yUnitFld = FormBuilder.createField("XYPlotOptionsDialog.y.unit");
        Widget yNameUnit = FormBuilder.createPanel(config, yNameFld, yUnitFld);
        CollapsiblePanel yNameUnitCP = new CollapsiblePanel("Y Name/Unit", yNameUnit, false);

        FlexTable colPanel = new FlexTable();
        DOM.setStyleAttribute(colPanel.getElement(), "padding", "5px");
        colPanel.setCellSpacing(10);

        colPanel.setHTML(0, 0, "X: ");
        colPanel.setWidget(0, 1, xColFld);
        colPanel.setWidget(1, 1, xNameUnitCP);
        colPanel.setHTML(2, 0, "Y: ");
        colPanel.setWidget(2, 1, yColFld);
        colPanel.setWidget(3, 1, yNameUnitCP);
        logScale = GwtUtil.makeCheckBox("XYPlotOptionsDialog.logScale");
        logScale.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (logScale.getValue() && !logScale.isEnabled()) {
                    // should not happen
                } else {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                    meta.setLogScale(logScale.getValue());
                    _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                }
            }
        });
        colPanel.setWidget(2, 2, logScale);

        // Plot Style
        plotDataPoints = SimpleInputField.createByProp("XYPlotOptionsDialog.plotDataPoints");
        plotDataPoints.getField().addValueChangeHandler(new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                if (!suspendEvents) {
                    String value = plotDataPoints.getValue();
                    if (value != null) {
                        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                        meta.setPlotDataPoints(XYPlotMeta.PlotStyle.getPlotStyle(value));
                        _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                    }
                }
            }
        });


        // Y MIN and MAX
        xMinMaxPanelDesc = GwtUtil.makeFaddedHelp(getXMinMaxDescHTML(data == null ? null: data.getXDatasetMinMax()));
        yMinMaxPanelDesc = GwtUtil.makeFaddedHelp(getYMinMaxDescHTML(data == null ? null: data.getYDatasetMinMax()));

        FormBuilder.Config cX = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                                      50, 5, HorizontalPanel.ALIGN_LEFT);

        xMinMaxPanel = new MinMaxPanel("XYPlotOptionsDialog.x.min", "XYPlotOptionsDialog.x.max", cX);

        FormBuilder.Config cY = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                                      50, 5, HorizontalPanel.ALIGN_LEFT);

        yMinMaxPanel = new MinMaxPanel("XYPlotOptionsDialog.y.min", "XYPlotOptionsDialog.y.max", cY);

        maxPoints = SimpleInputField.createByProp("XYPlotOptionsDialog.maxPoints");


        //VisIconCreator ic= VisIconCreator.Creator.getInstance();
        //Widget colInfo = GwtUtil.makeImageButton(new Image(ic.getFitsHeader()), "Show available columns", new ClickHandler() {
        Widget colInfo = GwtUtil.makeLinkButton("Show all columns", "Show available columns", new ClickHandler() {
                    public void onClick(ClickEvent event) {
               _xyPlotWidget.showColumns(RootPanel.get(), PopupPane.Align.CENTER);
            }
        });

        String bprop = _prop.makeBase("apply");
        String bname = WebProp.getName(bprop);
        String btip = WebProp.getTip(bprop);

        Button apply = new Button(bname, new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (xMinMaxPanel.validate() && yMinMaxPanel.validate() && maxPoints.validate() && validateColumns()) {

                    // current list of column names
                    List<TableDataView.Column> columnLst = _xyPlotWidget.getColumns();
                    List<String> cols = new ArrayList<String>(columnLst.size());
                    for (TableDataView.Column c : columnLst) {
                        cols.add(c.getName());
                    }

                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();

                    meta.userMeta.setXLimits(getMinMaxValues(xMinMaxPanel));
                    meta.userMeta.setYLimits(getMinMaxValues(yMinMaxPanel));

                    // Columns
                    if (xColExpr != null) {
                        meta.userMeta.xColExpr = xColExpr;
                        meta.userMeta.setXCol(null);
                    } else {
                        String xCol = xColFld.getValue();
                        if (StringUtils.isEmpty(xCol) || xCol.equals(meta.findDefaultXColName(cols))) {
                            xCol = null;
                        }
                        meta.userMeta.setXCol(xCol);
                        meta.userMeta.xColExpr = null;
                    }

                    if (yColExpr != null) {
                        meta.userMeta.yColExpr = yColExpr;
                        meta.userMeta.setYCol(null);
                        nonDefaultYColumn(meta);
                    } else {
                        String yCol = yColFld.getValue();
                        String errorCol;
                        boolean defaultYCol = yCol.equals(meta.findDefaultYColName(cols));
                        if (StringUtils.isEmpty(yCol) || defaultYCol) {
                            yCol = null;
                            errorCol = null;
                            plotError.setEnabled(true);
                            plotSpecificPoints.setEnabled(true);
                        } else {
                            nonDefaultYColumn(meta);
                            errorCol = "_"; // no error column for non-default y column
                        }
                        meta.userMeta.setYCol(yCol);
                        meta.userMeta.yColExpr = null;
                        meta.userMeta.setErrorCol(errorCol);
                    }
                    if (!StringUtils.isEmpty(xNameFld.getValue())) {
                        meta.userMeta.xName = xNameFld.getValue();
                    }
                    if (!StringUtils.isEmpty(xUnitFld.getValue())) {
                        meta.userMeta.xUnit = xUnitFld.getValue();
                    }
                    if (!StringUtils.isEmpty(yNameFld.getValue())) {
                        meta.userMeta.yName = yNameFld.getValue();
                    }
                    if (!StringUtils.isEmpty(yUnitFld.getValue())) {
                        meta.userMeta.yUnit = yUnitFld.getValue();
                    }

                    meta.setMaxPoints(Integer.parseInt(maxPoints.getValue()));

                    try {
                        _xyPlotWidget.updateMeta(meta, false);
                    } catch (Exception e) {
                        PopupUtil.showError("Update failed", e.getMessage());
                    }
                }
            }
        });
        apply.setTitle(btip);


        Button cancel = new Button("Restore Default", new ClickHandler() {
            public void onClick(ClickEvent ev) {
                clearOptions();
            }
        });
        cancel.setTitle("Restore default values");


        VerticalPanel vbox = new VerticalPanel();
        vbox.setSpacing(5);
        vbox.add(plotError);
        vbox.add(plotSpecificPoints);
        vbox.add(colInfo);

        vbox.add(colPanelDesc);
        vbox.add(colPanel);

        vbox.add(plotDataPoints);

        VerticalPanel vbox1 = new VerticalPanel();
        vbox1.add(xMinMaxPanelDesc);
        vbox1.add(xMinMaxPanel);
        vbox1.add(yMinMaxPanelDesc);
        vbox1.add(yMinMaxPanel);
        Label spacer1 = new Label();
        spacer1.setWidth("30px");
        vbox1.add(spacer1);
        vbox1.add(maxPoints);
        CollapsiblePanel cpanel = new CollapsiblePanel("More Options", vbox1, false);

        vbox.add(cpanel);

        //vbox.add(addToDefault);
        Widget buttons = GwtUtil.leftRightAlign(new Widget[]{cancel}, new Widget[]{apply, HelpManager.makeHelpIcon("visualization.chartoptions")});
        buttons.addStyleName("base-dialog-buttons");
        vbox.add(buttons);

        _mainPanel.setWidget(vbox);
    }



    public boolean setupError() {
        return !setupOK;
    }


    private void clearOptions() {
        // error, specific points, plot style (line or unconnected points) are specific to the table being plotted
        plotError.setEnabled(true);
        plotSpecificPoints.setEnabled(true);
        logScale.setEnabled(false);
        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
        meta.setPlotError(false);
        meta.setPlotSpecificPoints(true);
        meta.setLogScale(false);
        //meta.setPlotDataPoints(XYPlotMeta.PlotStyle.LINE);
        meta.setUserMeta(new XYPlotMeta.UserMeta());
        _xyPlotWidget.updateMeta(meta, false); // don't preserve zoom selection
        setup();
    }

    /*
        Sync the form with current meta and data
     */
    private void setup() {
        setupOK = true;
        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
        plotDataPoints.setValue(meta.plotDataPoints().key);
        plotError.setValue(meta.plotError());
        plotSpecificPoints.setValue(meta.plotSpecificPoints());
        XYPlotData data = _xyPlotWidget.getPlotData();
        if (data != null) {
            if (data.hasError() && plotError.isEnabled()) plotError.setVisible(true);
            else plotError.setVisible(false);

            if (data.hasSpecificPoints() && plotSpecificPoints.isEnabled()) {
                String desc = data.getSpecificPoints().getDescription();
                if (StringUtils.isEmpty(desc)) { desc = "Specific Points"; }
                plotSpecificPoints.setHTML("Plot "+desc);
                plotSpecificPoints.setVisible(true);
            } else plotSpecificPoints.setVisible(false);

            MinMax minMax = plotError.getValue() ? data.getWithErrorMinMax() :data.getYMinMax();
            if (minMax.getMin()>0 && minMax.getMax()/minMax.getMin()>4) {
                logScale.setEnabled(true);
                logScale.setVisible(true);
            } else {
                logScale.setEnabled(false);
                logScale.setVisible(false);
            }
            logScale.setValue(meta.logScale() && logScale.isEnabled());


            MinMax yMinMax = data.getYDatasetMinMax();
            DoubleFieldDef yminFD = (DoubleFieldDef)yMinMaxPanel.getMinField().getFieldDef();
            yminFD.setMinValue(Double.NEGATIVE_INFINITY);
            yminFD.setMaxValue(yMinMax.getMax());
            NumberFormat nf_y = NumberFormat.getFormat(MinMax.getFormatString(yMinMax, 3));
            yminFD.setErrMsg("Must be numerical value less than "+nf_y.format(yMinMax.getMax()));
            DoubleFieldDef ymaxFD = (DoubleFieldDef)yMinMaxPanel.getMaxField().getFieldDef();
            ymaxFD.setMinValue(yMinMax.getMin());
            ymaxFD.setMaxValue(Double.POSITIVE_INFINITY);
            ymaxFD.setErrMsg("Must be numerical value greater than "+nf_y.format(yMinMax.getMin()));


            MinMax xMinMax = data.getXDatasetMinMax();
            DoubleFieldDef xminFD = (DoubleFieldDef)xMinMaxPanel.getMinField().getFieldDef();
            xminFD.setMinValue(Double.NEGATIVE_INFINITY);
            xminFD.setMaxValue(xMinMax.getMax());
            NumberFormat nf_x = NumberFormat.getFormat(MinMax.getFormatString(xMinMax, 3));
            xminFD.setErrMsg("Must be numerical value less than "+nf_x.format(xMinMax.getMax()));
            DoubleFieldDef xmaxFD = (DoubleFieldDef)xMinMaxPanel.getMaxField().getFieldDef();
            xmaxFD.setMinValue(xMinMax.getMin());
            xmaxFD.setMaxValue(Double.POSITIVE_INFINITY);
            xmaxFD.setErrMsg("Must be numerical value greater than "+nf_x.format(xMinMax.getMin()));
        }
        MinMax xLimits = meta.userMeta.getXLimits();
        if (xLimits != null) {
            NumberFormat nf = NumberFormat.getFormat(MinMax.getFormatString(xLimits, 3));
            if (xLimits.getMin() != Double.NEGATIVE_INFINITY) {
                xMinMaxPanel.getMinField().setValue(nf.format(xLimits.getMin()));
            } else {
                xMinMaxPanel.getMinField().reset();
            }
            if (xLimits.getMax() != Double.POSITIVE_INFINITY) {
                xMinMaxPanel.getMaxField().setValue(nf.format(xLimits.getMax()));
            } else {
                xMinMaxPanel.getMaxField().reset();
            }
        } else {
            xMinMaxPanel.getMinField().reset();
            xMinMaxPanel.getMaxField().reset();
        }
        MinMax yLimits = meta.userMeta.getYLimits();
        if (yLimits != null) {
            NumberFormat nf = NumberFormat.getFormat(MinMax.getFormatString(yLimits, 3));
            if (yLimits.getMin() != Double.NEGATIVE_INFINITY) {
                yMinMaxPanel.getMinField().setValue(nf.format(yLimits.getMin()));
            } else {
                yMinMaxPanel.getMinField().reset();
            }
            if (yLimits.getMax() != Double.POSITIVE_INFINITY) {
                yMinMaxPanel.getMaxField().setValue(nf.format(yLimits.getMax()));
            } else {
                yMinMaxPanel.getMaxField().reset();
            }
        } else {
            yMinMaxPanel.getMinField().reset();
            yMinMaxPanel.getMaxField().reset();
        }
        xMinMaxPanelDesc.setHTML(getXMinMaxDescHTML(data == null ? null: data.getXDatasetMinMax()));
        yMinMaxPanelDesc.setHTML(getYMinMaxDescHTML(data == null ? null: data.getYDatasetMinMax()));

        if (meta.getMaxPoints() > 0) {
            maxPoints.setValue(meta.getMaxPoints()+"");
        }
        setupXYColumnFields();

        setFldValue(xNameFld, meta.userMeta.xName);
        setFldValue(xUnitFld, meta.userMeta.xUnit);
        setFldValue(yNameFld, meta.userMeta.yName);
        setFldValue(yUnitFld, meta.userMeta.yUnit);

        setupOK = (xMinMaxPanel.validate() && yMinMaxPanel.validate() && maxPoints.validate() && validateColumns());
    }

    private void nonDefaultYColumn(XYPlotMeta meta) {

        if (plotError.getValue()) {
            plotError.setValue(false);
            meta.setPlotError(false);
        }
        if (plotSpecificPoints.getValue()) {
            plotSpecificPoints.setValue(false);
            meta.setPlotSpecificPoints(false);
        }
        // error and specific points only make sense for default y column
        plotError.setEnabled(false);
        plotSpecificPoints.setEnabled(false);
        meta.userMeta.setErrorCol("_");  // no error column for non-default y column
    }

    private void setFldValue(InputField fld, String value) {
        if (StringUtils.isEmpty(value)) {
            fld.reset();
        } else {
            fld.setValue(value);
        }

    }

    private void setupXYColumnFields() {
        xColFld.reset();
        yColFld.reset();
        numericCols = new ArrayList<String>();

        if (_xyPlotWidget != null && _xyPlotWidget.getPlotData() != null) {
            List<TableDataView.Column> columnLst = _xyPlotWidget.getColumns();
            for (TableDataView.Column c : columnLst) {
                if (!c.getType().equals("char")) {
                    numericCols.add(c.getName());
                }
            }
            XYPlotData data = _xyPlotWidget.getPlotData();

            XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
            if (meta.userMeta != null && meta.userMeta.xColExpr != null) {
                xColFld.setValue(meta.userMeta.xColExpr.getInput());
            } else {
                String xCol = data.getXCol();
                if (numericCols.indexOf(xCol) > -1) xColFld.setValue(xCol);
            }
            if (meta.userMeta != null && meta.userMeta.yColExpr != null) {
                yColFld.setValue(meta.userMeta.yColExpr.getInput());
            } else {
                String yCol = data.getYCol();
                if (numericCols.indexOf(yCol) > -1) yColFld.setValue(yCol);
            }
       }
    }



    private MinMax getMinMaxValues(MinMaxPanel panel) {

        DoubleFieldDef minFD = (DoubleFieldDef)panel.getMinField().getFieldDef();

        String minStr = panel.getMinField().getValue();
        double min = Double.NEGATIVE_INFINITY;
        if (!StringUtils.isEmpty(minStr)) {
            min = minFD.getDoubleValue(minStr);
        }
        String maxStr = panel.getMaxField().getValue();
        double max = Double.POSITIVE_INFINITY;
        if (!StringUtils.isEmpty(maxStr)) {
            max = minFD.getDoubleValue(maxStr);
        }
        return new MinMax(min, max);
    }

    private String getXMinMaxDescHTML(MinMax xMinMax) {
        String desc = "Remove out-of-bound points by defining a new X range.<br>";
        if (xMinMax != null) {
            NumberFormat nf_x = NumberFormat.getFormat(MinMax.getFormatString(xMinMax, 3));
            desc += "Dataset min X: "+nf_x.format(xMinMax.getMin())+", max X: "+nf_x.format(xMinMax.getMax());
        }
        return desc;
    }

    private String getYMinMaxDescHTML(MinMax yMinMax) {
        String desc = "Remove out-of-bound points by defining a new Y range.<br>";
        if (yMinMax != null) {
            NumberFormat nf_y = NumberFormat.getFormat(MinMax.getFormatString(yMinMax, 3));
            desc += "Dataset min Y: "+nf_y.format(yMinMax.getMin())+", max Y: "+nf_y.format(yMinMax.getMax());
        }
        return desc;
    }

    private boolean validateColumns() {
        boolean valid = xColFld.validate() && yColFld.validate();

        if (!valid) return false;

        String xCol = xColFld.getValue();
        if (!numericCols.contains(xCol)) {
            //check for expression
            xColExpr = validateAndSetExpression(xColFld);
            valid = (xColExpr != null);
        }
        String yCol = yColFld.getValue();
        if (!numericCols.contains(yCol)) {
            // check for expression
            yColExpr = validateAndSetExpression(yColFld);
            valid = valid && (yColExpr != null);
        }
        return valid;
    }

    private Expression validateAndSetExpression(InputField fld) {
        // check that the expression is parsable
        Expression expr = new Expression(fld.getValue(),numericCols);
        if (!expr.isValid()) {
            fld.forceInvalid(expr.getErrorMessage());
            return null;
        } else {
            return expr;
        }

    }



    public class ColExpressionOracle extends SuggestOracle {

        @Override
        public void requestSuggestions(Request request, Callback callback) {
            String text= request.getQuery();
            resolveCol(text, request, callback);
        }

        @Override
        public boolean isDisplayStringHTML() {
            return true;
        }
    }

    public class ColExpressionOracleCallback implements AsyncCallback<List<String>> {
        private String prior;
        private SuggestOracle.Callback cb;
        private SuggestOracle.Request request;
        private boolean completed= false;

        ColExpressionOracleCallback(String prior, SuggestOracle.Request request, SuggestOracle.Callback cb) {
            this.prior= prior;
            this.request= request;
            this.cb= cb;
        }

        public void onFailure(Throwable caught) {
            if (!completed) {
                List<ColSuggestion> sugList= new ArrayList<ColSuggestion>(0);
                SuggestOracle.Response response= new SuggestOracle.Response(sugList);
                cb.onSuggestionsReady(request,response);
            }
            completed= true;
        }

        public void onSuccess(List<String> result) {
            if (!completed) {
                List<ColSuggestion> sugList= new ArrayList<ColSuggestion>(result.size());
                for(String col : result)  sugList.add(new ColSuggestion(prior, col));
                SuggestOracle.Response response= new SuggestOracle.Response(sugList);
                cb.onSuggestionsReady(request,response);
            }
            completed= true;
        }
    }

    public class ColSuggestion implements SuggestOracle.Suggestion {
        private String prior;
        private String col;

        ColSuggestion(String prior, String col) {
            this.prior = prior;
            this.col= col;
        }

        public String getDisplayString() {
            return format(col);
        }

        public String getReplacementString() {
            return prior+col;
        }

        private String format(String s) { return "&nbsp;"+ s +"&nbsp;&nbsp;"; }

    }

    private void resolveCol(String text, SuggestOracle.Request request, SuggestOracle.Callback callback) {
        if (!StringUtils.isEmpty(text)) {
            String prior = "";
            String token = "";
            int priorIdx = -1;
            for (int i = text.length()-1; i>=0; i--) {
                Character c = text.charAt(i);
                if (!Character.isLetterOrDigit(c) && !c.equals('_')) {
                    priorIdx = i;
                    break;
                }
            }
            if (priorIdx > 0) prior = text.substring(0, priorIdx+1);
            if (priorIdx < text.length()) token = text.substring(priorIdx+1);

            AsyncCallback<List<String>> cb = new ColExpressionOracleCallback(prior, request, callback);
            List<String> matchingCols = getMatchingCols(token);
            if (matchingCols == null || matchingCols.size()==0) {
                matchingCols = numericCols;
            }
            cb.onSuccess(matchingCols);
        }
    }

    private List<String> getMatchingCols(String token) {
        if (_xyPlotWidget != null && numericCols != null && numericCols.size()>1) {
            if (!StringUtils.isEmpty(token)) {
                ArrayList<String> matchingCols = new ArrayList<String>();
                for (String c : numericCols) {
                    if (c.startsWith(token)) {
                        matchingCols.add(c);
                    }
                }
                return matchingCols;
            }
        }
        return null;
    }

}
