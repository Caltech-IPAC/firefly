package edu.caltech.ipac.firefly.visualize.graph;

import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.util.expr.Expression;
import edu.caltech.ipac.util.StringUtils;

import java.util.List;

/**
 * Metadata for XYPlotWidget
 * @author tatianag
 * $Id: XYPlotMeta.java,v 1.18 2012/12/11 21:10:01 tatianag Exp $
 */
public class XYPlotMeta {

    public enum PlotStyle {LINE("line"), LINE_POINTS("linePoints"), POINTS("points");
        String key;
        PlotStyle(String key) {this.key = key;}
        static PlotStyle getPlotStyle(String style) {
            for (PlotStyle ps : values()) {
                if (ps.key.equals(style)) { return ps; }
            }
            return LINE;
        }
    }

    public enum ShowLegendRule {ALWAYS("always"), ON_EXPAND("onExpand");
        String key;
        ShowLegendRule(String key) {this.key = key;}
        static ShowLegendRule getShowLegendRule(String style) {
            for (ShowLegendRule rule : values()) {
                if (rule.key.equals(style)) { return rule; }
            }
            return ON_EXPAND;
        }
    }

    public static Scale LINEAR_SCALE = new LinearScale();
    public static Scale LOG_SCALE = new LogScale();

    String title;
    int maxPoints;
    boolean maxPointsSet;

    int xSize;
    int ySize;

    XYPlotMetaSource source;

    boolean plotError;
    boolean plotSpecificPoints;
    boolean noGrid;
    PlotStyle plotStyle;
    Scale xScale;
    Scale yScale;

    public UserMeta userMeta;

    public XYPlotMeta(String plotTitle, int xSize, int ySize, XYPlotMetaSource source) {
        this.title = plotTitle;
        this.maxPoints = Constants.MAX_ROWS_SUPPORTED;
        this.maxPointsSet = false; // using default
        this.xSize = xSize;
        this.ySize = ySize;
        this.source = source;
        plotError = false;
        plotStyle = source.getPlotStyle();
        plotSpecificPoints = true;
        noGrid = false;
        xScale = LINEAR_SCALE;
        yScale = LINEAR_SCALE;
        this.userMeta = new UserMeta();
    }
    
    public XYPlotMeta deepCopy() {
        XYPlotMeta copyMeta = new XYPlotMeta(title, xSize, ySize, source);
        copyMeta.maxPoints = maxPoints;
        copyMeta.maxPointsSet = maxPointsSet;
        copyMeta.source = source;
        copyMeta.plotError = plotError;
        copyMeta.plotStyle = plotStyle;
        copyMeta.plotSpecificPoints = plotSpecificPoints;
        copyMeta.noGrid = noGrid;
        copyMeta.xScale = xScale;
        copyMeta.yScale = yScale;
        copyMeta.userMeta = new UserMeta();
        copyMeta.userMeta.xLimits = userMeta.xLimits;
        copyMeta.userMeta.yLimits = userMeta.yLimits;
        copyMeta.userMeta.xCol = userMeta.xCol;
        copyMeta.userMeta.yCol = userMeta.yCol;
        copyMeta.userMeta.xName = userMeta.xName;
        copyMeta.userMeta.xUnit = userMeta.xUnit;
        copyMeta.userMeta.yCol = userMeta.yCol;
        copyMeta.userMeta.yName = userMeta.yName;
        copyMeta.userMeta.yUnit = userMeta.yUnit;            
        copyMeta.userMeta.errorCol = userMeta.errorCol;
        copyMeta.userMeta.orderCol = userMeta.orderCol;
        copyMeta.userMeta.xColExpr = userMeta.xColExpr;
        copyMeta.userMeta.yColExpr = userMeta.yColExpr;
        copyMeta.userMeta.samplingXBins = userMeta.samplingXBins;
        copyMeta.userMeta.samplingYBins = userMeta.samplingYBins;
        copyMeta.userMeta.aspectRatio = userMeta.aspectRatio;
        copyMeta.userMeta.stretchToFill = userMeta.stretchToFill;
        return copyMeta;
    }

    public String getTitle() { return title; }

    public int getMaxPoints() { return maxPoints; }

    public void  setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
        this.maxPointsSet = true;
    }

    public boolean isMaxPointsSet() { return this.maxPointsSet; }

    public int getXSize() { return xSize; }

    public int getYSize() { return ySize; }

    public String getXName(XYPlotData data) {
        String name = null;
        if (userMeta != null) {
            if (!StringUtils.isEmpty(userMeta.xName))  {
                name = userMeta.xName;
            } else if (userMeta.xColExpr != null) {
                name = userMeta.xColExpr.getInput();
            }
        }
        if (StringUtils.isEmpty(name)) {
            name = source.getXName(data);
        }
        return name;
    }

    public String getYName(XYPlotData data) {
        String name = null;
        if (userMeta != null) {
            if (!StringUtils.isEmpty(userMeta.yName))  {
                name = userMeta.yName;
            } else if (userMeta.yColExpr != null) {
                name = userMeta.yColExpr.getInput();
            }
        }
        if (StringUtils.isEmpty(name)) {
            name = source.getYName(data);
        }
        return name;
    }

    public String getDefaultXUnits(XYPlotData data) { return source.getDefaultXUnits(data); }

    public String getDefaultYUnits(XYPlotData data) { return source.getDefaultYUnits(data); }

    public boolean plotError() {
        return plotError;
    }

    public PlotStyle plotStyle() {
        return plotStyle;
    }

    public boolean alwaysShowLegend() {
        return source.getShowLegendRule().equals(ShowLegendRule.ALWAYS);
    }


    public boolean plotSpecificPoints() {
        return plotSpecificPoints;
    }

    public boolean noGrid() {
        return noGrid;
    }

    public Scale getXScale() { return xScale; }

    public Scale getYScale() { return yScale; }


    public void setXScale(Scale xScale) {
        this.xScale = xScale;
    }

    public void setYScale(Scale yScale) {
        this.yScale = yScale;
    }

    public void setUserMeta(UserMeta userMeta) {
        this.userMeta = userMeta;
    }

    public void setPlotError(boolean plotError) {
        this.plotError = plotError;
    }

    public void setPlotStyle(PlotStyle plotStyle) {
        this.plotStyle = plotStyle;
    }


    public void setPlotSpecificPoints(boolean plotSpecificPoints) {
        this.plotSpecificPoints = plotSpecificPoints;
    }

    public void setNoGrid(boolean noGrid) {
        this.noGrid = noGrid;
    }


    public String findXColName(List<String> colNames) {
        return findXColName(colNames, false);
    }

    public String findDefaultXColName(List<String> colNames) {
        return findXColName(colNames, true);
    }


    private String findXColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.xCol) || ignoreUserMeta) {
            return findName(source.getXColNames(), colNames);
        } else {
            return findName(userMeta.xCol, colNames);
        }
    }

    public String findYColName(List<String> colNames) {
        return findYColName(colNames, false);
    }

    public String findDefaultYColName(List<String> colNames) {
        return findYColName(colNames, true);
    }


    private String findYColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.yCol) || ignoreUserMeta) {
            return findName(source.getYColNames(), colNames);
        } else {
            return findName(userMeta.yCol, colNames);
        }
    }

    public String findErrorColName(List<String> colNames) {
        return findErrorColName(colNames, false);
    }

    public String findDefaultErrorColName(List<String> colNames) {
        return findErrorColName(colNames, true);
    }


    private String findErrorColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.errorCol) || ignoreUserMeta) {
            return findName(source.getErrorColNames(), colNames);
        } else {
            return findName(userMeta.errorCol, colNames);
        }
    }

    public String findOrderColName(List<String> colNames) {
        return findOrderColName(colNames, false);
    }

    public String findDefaultOrderColName(List<String> colNames) {
        return findOrderColName(colNames, true);
    }


    private String findOrderColName(List<String> colNames, boolean ignoreUserMeta) {
        if (StringUtils.isEmpty(userMeta.orderCol) || ignoreUserMeta) {
            return findName(source.getOrderColNames(), colNames);
        } else {
            return findName(userMeta.orderCol, colNames);
        }
    }

    /*
    public void addUserColumnsToDefault() {
        if (userMeta == null || !userMeta.addToDefault ) { return; }
        source.addColNamesToDefault(userMeta.getXCol(), userMeta.getYCol(), userMeta.getErrorCol(), userMeta.getOrderCol());
        userMeta.clearCols();
    }
    */

    public static String findName(String name, List<String> colNames) {
        // case insensitive
        return findName(new String[]{name}, colNames);
    }

    public static String findName(String [] names, List<String> colNames) {
        if (names != null) {
            for (String n : names) {
                for (String cn : colNames) {
                    if (n.equalsIgnoreCase(cn)) {
                        return cn;
                    }
                }
            }
        }
        return null;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setChartSize(int xSize, int ySize) {
        this.xSize = xSize;
        this.ySize = ySize;
    }

    public boolean hasUserMeta() {
        return userMeta != null && userMeta.wasSet();
    }

    public boolean isSpectrum() {
        return (source instanceof SpectrumMetaSource);
    }

    public static class UserMeta {
        MinMax xLimits = null;
        MinMax yLimits = null;
        Expression xColExpr;
        Expression yColExpr;
        String xCol = null;
        String xName = null;
        String xUnit = null;
        String yCol = null;
        String yName = null;
        String yUnit = null;
        String errorCol = null;
        String orderCol = null;
        boolean addToDefault = false;
        int samplingXBins;
        int samplingYBins;
        double aspectRatio;
        boolean stretchToFill;


        public UserMeta() {
            this.xLimits = null;
            this.yLimits = null;
            this.xCol = null;
            this.yCol = null;
            this.xName = null;
            this.xUnit = null;
            this.yCol = null;
            this.yName = null;
            this.yUnit = null;            
            this.errorCol = null;
            this.orderCol = null;
            this.xColExpr = null;
            this.yColExpr = null;
            this.samplingXBins = 0;
            this.samplingYBins = 0;
            this.aspectRatio = -1;
            this.stretchToFill = false;
        }

        public boolean wasSet() {
            return xLimits != null || yLimits != null || xCol != null || yCol != null ||
                    errorCol != null || orderCol != null || xColExpr != null || yColExpr != null ||
                    xName != null || xUnit != null || yName != null || yUnit != null ||
                    samplingXBins != 0 || samplingYBins != 0 ||
                    aspectRatio > 0 || stretchToFill==true;
        }

        public void setXLimits(MinMax xLimits) { this.xLimits = xLimits; }
        public void setYLimits(MinMax yLimits) { this.yLimits = yLimits; }
        public void setXCol(String xCol) { this.xCol = xCol; }
        public void setYCol(String yCol) { this.yCol = yCol; }
        public void setErrorCol(String errorCol) { this.errorCol = errorCol; }
        public void setOrderCol(String orderCol) { this.orderCol = orderCol; }
        public void setXName(String xName) { this.xName = xName; }
        public void setYName(String yName) { this.yName = yName; }

        public MinMax getXLimits() { return xLimits; }
        public MinMax getYLimits() { return yLimits; }
        public String getXCol() { return xCol; }
        public String getYCol() { return yCol; }
        public String getErrorCol() { return errorCol; }
        public String getOrderCol() { return orderCol; }

        public boolean hasXMin() { return xLimits != null && xLimits.getMin() != Double.NEGATIVE_INFINITY; }
        public boolean hasXMax() { return xLimits != null && xLimits.getMax() != Double.POSITIVE_INFINITY;}
        public boolean hasYMin() { return yLimits != null && yLimits.getMin() != Double.NEGATIVE_INFINITY;}
        public boolean hasYMax() { return yLimits != null && yLimits.getMax() != Double.POSITIVE_INFINITY;}
    }

}
