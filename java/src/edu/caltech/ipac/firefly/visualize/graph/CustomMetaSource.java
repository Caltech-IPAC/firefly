package edu.caltech.ipac.firefly.visualize.graph;

import java.util.Map;

/**
 * @author tatianag
 *         $Id: CustomMetaSource.java,v 1.2 2013/01/07 21:33:39 tatianag Exp $
 */
public class CustomMetaSource implements XYPlotMetaSource  {


    public static String XCOL_KEY = "xCol";
    public static String YCOL_KEY = "yCol";
    public static String XDEFUNITS_KEY = "xDefUnits";
    public static String YDEFUNITS_KEY = "yDefUnits";
    public static String ERRORCOL_KEY = "errorCol";
    public static String ORDERCOL_KEY = "orderCol";
    public static String PLOT_STYLE_KEY = "plotStyle";

    private static String[] PARAM_KEYS = {
            XCOL_KEY, YCOL_KEY, XDEFUNITS_KEY, YDEFUNITS_KEY, ERRORCOL_KEY, ORDERCOL_KEY, PLOT_STYLE_KEY
    };


    private String [] xCols;
    private String [] yCols;
    private String [] errorCols;
    private String [] orderCols;
    private String xDefaultUnits;
    private String yDefaultUnits;
    private XYPlotMeta.PlotStyle plotStyle;

    public CustomMetaSource(Map<String, String> params) {

        xCols = getStringArray(params.get(XCOL_KEY));
        yCols = getStringArray(params.get(YCOL_KEY));
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
            if (plotStyleStr.equalsIgnoreCase("line")) {
                plotStyle = XYPlotMeta.PlotStyle.LINE;
            } else if (plotStyleStr.equalsIgnoreCase("line_points")) {
                plotStyle = XYPlotMeta.PlotStyle.LINE_POINTS;
            } else {
                plotStyle = XYPlotMeta.PlotStyle.POINTS;
            }
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
}
