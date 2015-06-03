/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.expr.Expression;

import java.util.Map;

/**
 * @author tatianag
 *         $Id: CustomMetaSource.java,v 1.2 2013/01/07 21:33:39 tatianag Exp $
 */
public class CustomMetaSource implements XYPlotMetaSource  {


    public static String XCOL_KEY = "xCol";
    public static String XCOL_EXPR_KEY = "xColExpr";
    public static String YCOL_KEY = "yCol";
    public static String YCOL_EXPR_KEY = "yColExpr";
    public static String XDEFUNITS_KEY = "xDefUnits";
    public static String YDEFUNITS_KEY = "yDefUnits";
    public static String ERRORCOL_KEY = "errorCol";
    public static String ORDERCOL_KEY = "orderCol";
    public static String PLOT_STYLE_KEY = "plotStyle";
    public static String SHOW_LEGEND_KEY = "showLegend";

    private static String[] PARAM_KEYS = {
            XCOL_KEY, XCOL_EXPR_KEY, YCOL_KEY, YCOL_EXPR_KEY, XDEFUNITS_KEY, YDEFUNITS_KEY, ERRORCOL_KEY, ORDERCOL_KEY, PLOT_STYLE_KEY, SHOW_LEGEND_KEY
    };


    private String [] xCols;
    private String xColExpr;
    private String [] yCols;
    private String yColExpr;
    private String [] errorCols;
    private String [] orderCols;
    private String xDefaultUnits;
    private String yDefaultUnits;
    private XYPlotMeta.PlotStyle plotStyle;
    private XYPlotMeta.ShowLegendRule showLegendRule;

    public CustomMetaSource(Map<String, String> params) {

        xCols = getStringArray(params.get(XCOL_KEY));
        xColExpr = params.get(XCOL_EXPR_KEY);
        yCols = getStringArray(params.get(YCOL_KEY));
        yColExpr = params.get(YCOL_EXPR_KEY);
        errorCols = getStringArray(params.get(ERRORCOL_KEY));
        orderCols = getStringArray(params.get(ORDERCOL_KEY));

        xDefaultUnits = params.get(XDEFUNITS_KEY);
        if (xDefaultUnits==null) xDefaultUnits = "";

        yDefaultUnits = params.get(YDEFUNITS_KEY);
        if (yDefaultUnits==null) yDefaultUnits = "";

        String plotStyleStr = params.get(PLOT_STYLE_KEY);
        if (plotStyleStr == null) {
            plotStyle = XYPlotMeta.PlotStyle.POINTS;
        } else {
            if (plotStyleStr.equalsIgnoreCase(XYPlotMeta.PlotStyle.LINE.key)) {
                plotStyle = XYPlotMeta.PlotStyle.LINE;
            } else if (plotStyleStr.equalsIgnoreCase(XYPlotMeta.PlotStyle.LINE_POINTS.key)) {
                plotStyle = XYPlotMeta.PlotStyle.LINE_POINTS;
            } else {
                plotStyle = XYPlotMeta.PlotStyle.POINTS;
            }
        }

        String showLegendStr = params.get(SHOW_LEGEND_KEY);
        if (!StringUtils.isEmpty(showLegendStr) && showLegendStr.equals(XYPlotMeta.ShowLegendRule.ALWAYS.key)) {
            showLegendRule = XYPlotMeta.ShowLegendRule.ALWAYS;
        } else {
            showLegendRule = XYPlotMeta.ShowLegendRule.ON_EXPAND;
        }
    }

    public static boolean isValidParam( String key) {
        for (String s : PARAM_KEYS) {
            if (key.equals(s)) {
                return true;
            }
        }
        return false;
    }
    private String []  getStringArray(String val) {
        return (val == null) ? new String[]{} : val.split(",");
    }


    public String getXName(XYPlotData data) {
        return data.getXCol(); // TODO return short desc, rather than name
    }

    public String getYName(XYPlotData data) {
        return data.getYCol(); // TODO return short desc, rather than name
    }

    public String getDefaultXUnits(XYPlotData data) {
        return xDefaultUnits;
    }

    public String getDefaultYUnits(XYPlotData data) {
        return yDefaultUnits;
    }

    public String[] getXColNames() {
        return xCols;
    }

    public String[] getYColNames() {
        return yCols;
    }

    public String[] getErrorColNames() {
        return errorCols;
    }

    public String[] getOrderColNames() {
        return orderCols;
    }

    public XYPlotMeta.PlotStyle getPlotStyle() {
        return plotStyle;
    }

    public XYPlotMeta.ShowLegendRule getShowLegendRule() {
            return showLegendRule;
        }

    public Expression getXColExpr() {
        if (xColExpr == null) return null;
        Expression expr = new Expression(xColExpr, null);
        return (expr.isValid() ? expr : null);
    }

    public Expression getYColExpr() {
        if (yColExpr == null) return null;
        Expression expr = new Expression(yColExpr, null);
        return (expr.isValid() ? expr : null);
    }
}
