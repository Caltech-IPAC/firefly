/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

/**
 * @author tatianag
 *         $Id: XYPlotMetaSource.java,v 1.4 2012/12/11 21:10:01 tatianag Exp $
 */
public interface XYPlotMetaSource {
    /**
     * @return the name for x axis
     */
    public String getXName(XYPlotData data);

    /**
     * @return the name for y axis
     */
    public String getYName(XYPlotData data);

    /**
     * @return default x units for cases when no units specified in data
     */
    public String getDefaultXUnits(XYPlotData data);

    /**
     * @return default y units for cases when no units are specified in data
     */
    public String getDefaultYUnits(XYPlotData data);

    public String [] getXColNames();
    public String [] getYColNames();
    public String [] getErrorColNames();
    public String [] getOrderColNames();

    public XYPlotMeta.PlotStyle getPlotStyle();
    public XYPlotMeta.ShowLegendRule getShowLegendRule();
    //public void addColNamesToDefault(String xCol, String yCol, String errorCol, String orderCol);
}
