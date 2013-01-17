package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.form.DoubleFieldDef;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.resbundle.images.VisIconCreator;
import edu.caltech.ipac.firefly.ui.*;
import edu.caltech.ipac.firefly.ui.input.SimpleInputField;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.WebClassProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.List;                                                         

/**
 * @author tatianag
 *         $Id: XYPlotOptionsDialog.java,v 1.22 2012/09/20 23:36:03 xiuqin Exp $
 */
public class XYPlotOptionsDialog {
    private static WebClassProperties _prop= new WebClassProperties(XYPlotOptionsDialog.class);
    private final PopupPane _popup;
    private final XYPlotWidget _xyPlotWidget;

    private MinMaxPanel xMinMaxPanel;
    private MinMaxPanel yMinMaxPanel;
    private HTML xMinMaxPanelDesc;
    private HTML yMinMaxPanelDesc;
    private SimpleInputField plotDataPoints;
    private CheckBox plotError;
    private CheckBox plotSpecificPoints;
    //private CheckBox addToDefault;
    private ListBox xColList;
    private ListBox yColList;
    private List<String> numericCols;

    XYPlotOptionsDialog(XYPlotWidget widget) {
        _popup= new PopupPane(_prop.getTitle(),null, PopupType.STANDARD, false, false);
        _xyPlotWidget = widget;
        layout(widget.getPlotData());
    }

    public void setVisible(boolean v) {
        if (v) {
            setup();
            _popup.alignTo(RootPanel.get(), PopupPane.Align.TOP_CENTER, 0, 0);
            _popup.show();
        }
        else {
            _popup.hide();
        }
    }

    public boolean isVisible() {
        return _popup.isVisible();
    }

    private void clearOptions() {
        // TODO: error, specific points, plot style (line or unconnected points) are apecific to the table being plotted
        plotError.setEnabled(true);
        plotSpecificPoints.setEnabled(true);
        XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
        meta.setPlotError(false);
        meta.setPlotSpecificPoints(true);
        //meta.setPlotDataPoints(XYPlotMeta.PlotStyle.LINE);
        meta.setUserMeta(new XYPlotMeta.UserMeta());
        _xyPlotWidget.updateMeta(meta, false); // don't preserve zoom selection
        setup();
    }

    private void layout(final XYPlotData data) {

        // Plot Style
        HTML styleDesc = GwtUtil.makeFaddedHelp("<br>Select how data should be presented");
        plotDataPoints = SimpleInputField.createByProp("XYPlotOptionsDialog.plotDataPoints");
        plotDataPoints.getField().addValueChangeHandler(new ValueChangeHandler<String>(){
            public void onValueChange(ValueChangeEvent<String> ev) {
                String value = plotDataPoints.getValue();
                if (value != null) {
                    XYPlotMeta meta = _xyPlotWidget.getPlotMeta();
                       meta.setPlotDataPoints(XYPlotMeta.PlotStyle.getPlotStyle(value));
                       _xyPlotWidget.updateMeta(meta, true); // preserve zoom
                }
            }
        });

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

        // Y MIN and MAX
        xMinMaxPanelDesc = GwtUtil.makeFaddedHelp(getXMinMaxDescHTML(data == null ? null: data.getXDatasetMinMax()));
        yMinMaxPanelDesc = GwtUtil.makeFaddedHelp(getYMinMaxDescHTML(data == null ? null: data.getYDatasetMinMax()));

        FormBuilder.Config cX = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                                      50, 5, HorizontalPanel.ALIGN_LEFT);

        xMinMaxPanel = new MinMaxPanel("XYPlotOptionsDialog.x.min", "XYPlotOptionsDialog.x.max", cX);

        FormBuilder.Config cY = new FormBuilder.Config(FormBuilder.Config.Direction.HORIZONTAL,
                                                      50, 5, HorizontalPanel.ALIGN_LEFT);

        yMinMaxPanel = new MinMaxPanel("XYPlotOptionsDialog.y.min", "XYPlotOptionsDialog.y.max", cY);

        // Alternative Columns
        HTML colPanelDesc = GwtUtil.makeFaddedHelp("Change what is being plotted");

        FlexTable colPanel = new FlexTable();
        DOM.setStyleAttribute(colPanel.getElement(), "padding", "10px");
        colPanel.setCellSpacing(10);
        xColList = new ListBox();
        xColList.setWidth("200px");
        yColList = new ListBox();
        yColList.setWidth("200px");
        colPanel.setHTML(0, 0, "X Column: ");
        colPanel.setWidget(0, 1, xColList);
        colPanel.setHTML(1, 0, "Y Column: ");
        colPanel.setWidget(1, 1, yColList);


        final HorizontalPanel colPanelPlus = new HorizontalPanel();


        VisIconCreator ic= VisIconCreator.Creator.getInstance();
        Widget cols = GwtUtil.makeImageButton(new Image(ic.getFitsHeader()), "Show available columns", new ClickHandler() {

            public void onClick(ClickEvent event) {
                _xyPlotWidget.showColumns(RootPanel.get(), PopupPane.Align.CENTER);
            }
        });

        colPanelPlus.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        colPanelPlus.add(colPanel);
        colPanelPlus.add(cols);

        String bprop = _prop.makeBase("apply");
        String bname = WebProp.getName(bprop);
        String btip = WebProp.getTip(bprop);

        Button apply = new Button(bname, new ClickHandler() {
            public void onClick(ClickEvent ev) {
                if (xMinMaxPanel.validate() && yMinMaxPanel.validate() && validateColumns()) {

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
                    String xCol = xColList.getSelectedIndex() >= 0 ?
                        numericCols.get(xColList.getSelectedIndex()) : "";
                    if (StringUtils.isEmpty(xCol) || xCol.equals(meta.findDefaultXColName(cols))) {
                        xCol = null;
                    }
                    meta.userMeta.setXCol(xCol);

                    String yCol = yColList.getSelectedIndex() >= 0 ?
                        numericCols.get(yColList.getSelectedIndex()) : "";
                    boolean defaultYCol = yCol.equals(meta.findDefaultYColName(cols));
                    if (StringUtils.isEmpty(yCol) || defaultYCol) {
                        yCol = null;
                        plotError.setEnabled(true);
                        plotSpecificPoints.setEnabled(true);
                    } else {
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
                    }
                    meta.userMeta.setYCol(yCol);

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


        VerticalPanel vbox= new VerticalPanel();
        vbox.setSpacing(5);
        vbox.add(plotError);
        vbox.add(plotSpecificPoints);

        vbox.add(styleDesc);
        vbox.add(plotDataPoints);

        vbox.add(xMinMaxPanelDesc);
        vbox.add(xMinMaxPanel);
        vbox.add(yMinMaxPanelDesc);
        vbox.add(yMinMaxPanel);

        vbox.add(colPanelDesc);
        vbox.add(colPanelPlus);
        //vbox.add(addToDefault);
        Widget buttons = GwtUtil.leftRightAlign(new Widget[]{cancel}, new Widget[]{apply, HelpManager.makeHelpIcon("visualization.chartoptions")});
        buttons.addStyleName("base-dialog-buttons");
        vbox.add(buttons);           
        _popup.setWidget(vbox);
    }

    private void setupXYColumnFields() {
        xColList.clear();
        yColList.clear();
        numericCols = new ArrayList<String>();

        if (_xyPlotWidget != null && _xyPlotWidget.getPlotData() != null) {
            List<TableDataView.Column> columnLst = _xyPlotWidget.getColumns();
            for (TableDataView.Column c : columnLst) {
                if (!c.getType().equals("char")) {
                    numericCols.add(c.getName());
                    xColList.addItem(c.getTitle()+(StringUtils.isEmpty(c.getUnits()) ? "" : " ("+c.getUnits()+")"));
                    yColList.addItem(c.getTitle()+(StringUtils.isEmpty(c.getUnits()) ? "" : " ("+c.getUnits()+")"));
                }
            }
            XYPlotData data = _xyPlotWidget.getPlotData();
            int xSelIdx = numericCols.indexOf(data.getXCol());
            if (xSelIdx > -1) xColList.setSelectedIndex(xSelIdx);
            int ySelIdx = numericCols.indexOf(data.getYCol());
            if (ySelIdx > -1) yColList.setSelectedIndex(ySelIdx);
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


    /*
        Sync the form with current meta and data
     */
    public boolean setup() {
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

        setupXYColumnFields();
        return (xMinMaxPanel.validate() && yMinMaxPanel.validate() && validateColumns());
    }

    private boolean validateColumns() {
        return (xColList.getSelectedIndex() > -1 && yColList.getSelectedIndex() > -1);

    }


}
