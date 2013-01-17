package edu.caltech.ipac.firefly.visualize.graph;

/**
 * @author tatianag
 *         $Id: CatalogMetaSource.java,v 1.2 2012/12/11 21:10:01 tatianag Exp $
 */
public class CatalogMetaSource implements XYPlotMetaSource  {

    private static CatalogMetaSource instance;

    private CatalogMetaSource() {

    }

    public static CatalogMetaSource getInstance() {
        if (instance == null) {
            instance = new CatalogMetaSource();
        }
        return instance;
    }


    public String getXName(XYPlotData data) {
        return data.getXCol(); // TODO return short desc, rather than name
    }

    public String getYName(XYPlotData data) {
        return data.getYCol(); // TODO return short desc, rather than name
    }

    public String getDefaultXUnits(XYPlotData data) {
        return "";
    }

    public String getDefaultYUnits(XYPlotData data) {
        return "";
    }

    public String[] getXColNames() {
        return new String[]{"ra"};
    }

    public String[] getYColNames() {
        return new String[]{"dec"};
    }

    public String[] getErrorColNames() {
        return new String[0];
    }

    public String[] getOrderColNames() {
        return new String[0];
    }

    public XYPlotMeta.PlotStyle getPlotStyle() {
        return XYPlotMeta.PlotStyle.POINTS;
    }
}
