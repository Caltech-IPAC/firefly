/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.chart;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * Date: Nov 5, 2008
 *
 * @author loi
 * @version $Id: Dojo2DChart.java,v 1.1 2008/11/11 23:45:02 loi Exp $
 */
public class Dojo2DChart extends Widget {

    enum Type {Lines, StackedLines, Bars, Columns, StackedBars, StackedColumns}

    JavaScriptObject chart;
    String hAxis = "x";
    String vAxis = "y";

    ArrayList<PlotModel> plots = new ArrayList<PlotModel>();


    public Dojo2DChart() {
        JavaScriptObject div = createDiv(String.valueOf(System.currentTimeMillis()));
        setElement(div.<Element>cast());
        setSize("800px", "400px");
    }

    public void addPlot(Config config, String serieName, float... data) {
        plots.add(new PlotModel(config, serieName, data));
    }

    public void drawPlot(PlotModel model) {
        JsArrayNumber ary = JsArrayNumber.createArray().cast();
        for(int i = 0; i < model.data.length; i++) {
            ary.set(i, model.data[i]);
        }
        addPlot(chart, model.config.name, model.config.type.name());
        addSeries(chart, model.config.name, model.name, ary);
    }


    private native void renderChart(JavaScriptObject chart) /*-{
    $wnd.alert("chart:" + chart);
            chart.render();
    }-*/;

    private native void addSeries(JavaScriptObject chart, String plotName, String series, JavaScriptObject data) /*-{
    $wnd.alert("series:" + series + "  data:" + data + " id:" + plotName);
            chart.addSeries(series, data, {plot: plotName});
    }-*/;

    private native void addPlot(JavaScriptObject chart, String name, String ctype) /*-{
    $wnd.alert("name:" + name + "  ctype:" + ctype);
            chart.addPlot(name, {type: ctype});
    }-*/;

    private native void addAxis(JavaScriptObject chart, String hAxis, String vAxis) /*-{
            chart.addAxis(hAxis);
            chart.addAxis(vAxis, {vertical: true});
    }-*/;

    private native JavaScriptObject createDiv(String idstr) /*-{
        var el = $wnd.dojo.doc.createElement("div");
        el.id = idstr;
        return el;
    }-*/;

    private native JavaScriptObject createChart(String idstr) /*-{
        var el = $wnd.dojo.doc.createElement("div");
        el.id = idstr;
        var chart = new $wnd.dojox.charting.Chart2D(idstr);
        return chart;
    }-*/;

    private native JavaScriptObject test(JavaScriptObject chart) /*-{
        chart.addPlot("default", {type: "Lines"});
        chart.addPlot("other", {type: "Areas"});
        chart.addAxis("x");
        chart.addAxis("y", {vertical: true});
        chart.addSeries("Series 1", [1, 2, 2, 3, 4, 5, 5, 7], {plot:"default"});
        chart.addSeries("Series 2", [1, 1, 4, 2, 1, 6, 4, 3],
            {plot: "other", stroke: {color:"blue"}, fill: "lightblue"});
        chart.render();
    }-*/;

    @Override
    protected void onLoad() {
        addPlot(new Config(Type.Lines, "default"), "Series 1", 1,2,3,4,5);
        render();
    }

    public void render() {
        chart = createChart(getElement().getId());
//        test(chart);
        for(PlotModel m : plots) {
            drawPlot(m);
        }
        addAxis(chart, hAxis, vAxis);
        renderChart(chart);
    }

    public static class Config {
        Type type;
        String name;
        boolean lines;
        String color;
        String fill;
        boolean markers;
        String tensions;
        Shadows shadows;

        public Config(Type type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    public static class BarsConfig extends Config {
        int gap;

        public BarsConfig(Type type, String name) {
            super(type, name);
        }
    }

    public static class Shadows {
        int offsetX;
        int offsetY;
        int width;

        public Shadows(int offsetX, int offsetY, int width) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.width = width;
        }
    }

    static class PlotModel {
        Config config;
        String name;
        float[] data;

        PlotModel(Config config, String name, float[] data) {
            this.config = config;
            this.name = name;
            this.data = data;
        }
    }
    
}
