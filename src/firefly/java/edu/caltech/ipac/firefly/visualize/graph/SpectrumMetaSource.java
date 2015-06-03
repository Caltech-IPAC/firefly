/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.graph;

//import edu.caltech.ipac.util.StringUtils;

//import java.util.Arrays;
//import java.util.List;


import edu.caltech.ipac.util.expr.Expression;

/**
 * @author tatianag
 *         $Id: SpectrumMetaSource.java,v 1.6 2012/12/11 21:10:01 tatianag Exp $
 */
public class SpectrumMetaSource implements XYPlotMetaSource {

    /*
    public static String SPECTRUM_XCOLS_KEY = "SpectrumXCols";
    public static String SPECTRUM_YCOLS_KEY = "SpectrumYCols";
    public static String SPECTRUM_ERROR_COLS_KEY = "SpectrumErrorCols";
    public static String SPECTRUM_ORDER_COLS_KEY = "SpectrumOrderCols";
    */

    private static String DEFAULT_SPECTRUM_XCOLS = "wavelength,wavelenth,wavelen,wave,lambda";
    private static String DEFAULT_SPECTRUM_YCOLS = "flux_density,flux-density,flux density,fluxdensity,flux,specific flux,fullap";
    private static String DEFAULT_SPECTRUM_ERROR_COLS = "error,sigma_flux,flux_uncertainty,f_er,fullap_error";
    private static String DEFAULT_SPECTRUM_ORDER_COLS = "order,order_bit,orderflag,module";

    private static SpectrumMetaSource instance;
    private String [] xColNames = null;
    private String [] yColNames = null;
    private String [] errorColNames = null;
    private String [] orderColNames = null;

    // regular expression for separator
    private String SEPARATOR = ",";

    private SpectrumMetaSource() {

    }

    public static SpectrumMetaSource getInstance() {
        if (instance == null) {
            instance = new SpectrumMetaSource();
        }
        return instance;
    }

    public String getXName(XYPlotData data) {
        return data.getXCol();
    }

    public String getYName(XYPlotData data) {
        if (data.getYCol().equalsIgnoreCase("flux") &&
                (data.getYUnits() != null && data.getYUnits().endsWith("Jy"))) {
            return "flux density";
        } else {
            return data.getYCol();
        }
    }

    public String getDefaultXUnits(XYPlotData data) {
        if (data.getXCol().equalsIgnoreCase("wavelength")) {
            return "microns";
        } else {
            return "";
        }
    }

    public String getDefaultYUnits(XYPlotData data) {
        return "";
    }

    public String[] getXColNames() {
        if (xColNames == null) {
            xColNames = DEFAULT_SPECTRUM_XCOLS.split(SEPARATOR);
        }
        return xColNames;
    }

    public String[] getYColNames() {
        if (yColNames == null) {
           yColNames = DEFAULT_SPECTRUM_YCOLS.split(SEPARATOR);
        }
        return yColNames;
    }

    public String[] getErrorColNames() {
        if (errorColNames == null) {
           errorColNames = DEFAULT_SPECTRUM_ERROR_COLS.split(SEPARATOR);
        }
        return errorColNames;
    }

    public String[] getOrderColNames() {
        if (orderColNames == null) {
            orderColNames = DEFAULT_SPECTRUM_ORDER_COLS.split(SEPARATOR);
        }
        return orderColNames;
    }

    public XYPlotMeta.PlotStyle getPlotStyle() {
        return XYPlotMeta.PlotStyle.LINE;
    }

    public XYPlotMeta.ShowLegendRule getShowLegendRule() {
        return XYPlotMeta.ShowLegendRule.ON_EXPAND;
    }


    public Expression getXColExpr() {
        return null;
    }

    public Expression getYColExpr() {
        return null;
    }

    /*
    public void addColNamesToDefault(String xCol, String yCol, String errorCol, String orderCol) {
        if (!StringUtils.isEmpty(xCol)) {
            updateColList(xCol, getXColNames(), SPECTRUM_XCOLS_KEY);
        }
        if (!StringUtils.isEmpty(yCol)) {
            updateColList(yCol, getYColNames(), SPECTRUM_YCOLS_KEY);
        }
        if (!StringUtils.isEmpty(errorCol)) {
            updateColList(errorCol, getErrorColNames(), SPECTRUM_ERROR_COLS_KEY);
        }
        if (!StringUtils.isEmpty(orderCol)) {
            updateColList(orderCol, getOrderColNames(), SPECTRUM_ORDER_COLS_KEY);
        }

    }

    private void updateColList(String col, String [] cols, String designator) {
        if (!StringUtils.isEmpty(col)) {
            String name = col.toLowerCase();
            List<String> list = Arrays.asList(cols);

            int idx = list.indexOf(name);
            if (idx != 0) {
                String [] newcols;
                if (idx > 0) {
                    newcols = new String[list.size()];
                    newcols[0] = name;
                    for (int i=0; i<idx; i++){
                        newcols[i+1] = list.get(i);
                    }
                    for (int i=idx+1; i<list.size(); i++) {
                        newcols[i] = list.get(i);
                    }
                } else {
                    newcols = new String[list.size()+1];
                    newcols[0] = name;
                    for (int i=0; i<list.size(); i++) {
                        newcols[i+1] = list.get(i);
                    }
                }
                if (designator.equals(SPECTRUM_XCOLS_KEY)) {
                    xColNames = newcols;
                }
                if (designator.equals(SPECTRUM_YCOLS_KEY)) {
                    yColNames = newcols;
                }
                if (designator.equals(SPECTRUM_ERROR_COLS_KEY)) {
                    errorColNames = newcols;
                }
                if (designator.equals(SPECTRUM_ORDER_COLS_KEY)) {
                    orderColNames = newcols;
                }
            }
        }
    }
    */
}
