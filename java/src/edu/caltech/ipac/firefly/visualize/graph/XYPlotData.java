package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.firefly.util.expr.Expression;
import edu.caltech.ipac.util.StringUtils;

import java.util.*;


/**
 * @author tatianag
 * $Id: XYPlotData.java,v 1.22 2012/10/11 19:31:43 tatianag Exp $
 */
public class XYPlotData {
    

    // column names
    private String xCol;
    private String yCol;
    private String orderCol;
    private String errorCol;

    // units
    private String xColUnits;
    private String yColUnits;
    private String errorColUnits;

    //private String xName = null;
    //private String yName = null;

    MinMax xMinMax;
    MinMax yMinMax;
    MinMax xDatasetMinMax;
    MinMax yDatasetMinMax;
    MinMax withErrorMinMax = null;


    // order column defines which points should be placed in the same set (plotted by the same curve)
    private boolean hasOrder;

    // is error column defined
    private boolean hasError;


    /**
     *  One set per order;
     *  if table has no order column, there will be one item in this list
     */
    private List<Curve> curves;

    private SpecificPoints adjustedSpecificPoints = null;

    private static NumberFormat _nf = NumberFormat.getFormat("#.######");
    private static NumberFormat _nfExp = NumberFormat.getFormat("#.######E0");

    private int numPointsInSample;
    private int numPointsRepresented;

    // if I could figure out how to make them local, I would
    private double xDatasetMin=Double.POSITIVE_INFINITY, xDatasetMax=Double.NEGATIVE_INFINITY;
    private double yDatasetMin=Double.POSITIVE_INFINITY, yDatasetMax=Double.NEGATIVE_INFINITY;

    XYPlotData(final DataSet dataSet, final XYPlotMeta meta) {

        TableData model = dataSet.getModel();
        int orderColIdx=-1, errorColIdx=-1, xColIdx=0, yColIdx=0;
        List<String> colNames = model.getColumnNames();

        final boolean xExpr = meta.userMeta != null && meta.userMeta.xColExpr != null;
        final Expression xColExpr = xExpr ? meta.userMeta.xColExpr : null;
        if (xExpr) {
            xCol = "";
            xColUnits = "";
        } else {
            xCol = meta.findXColName(colNames);
            if (xCol == null) {
                // find first numeric column
                List<TableDataView.Column> cols = dataSet.getColumns();
                for (TableDataView.Column c : cols) {
                    if (isNumeric(c)) {
                        xCol = c.getName();
                        break;
                    }
                }
                if (xCol == null) {
                    throw new IllegalArgumentException("X column is not found in the data set.");
                }
            }
            xColIdx = model.getColumnIndex(xCol);
            xColUnits = dataSet.getColumn(xColIdx).getUnits();

        }


        final boolean yExpr = meta.userMeta != null && meta.userMeta.yColExpr != null;
        final Expression yColExpr = yExpr ? meta.userMeta.yColExpr : null;
        if (yExpr) {
            yCol = "";
            yColUnits = "";
        } else {
            yCol = meta.findYColName(colNames);
            if (yCol == null) {
                //find first numeric column that is not xCol
                List<TableDataView.Column> cols = dataSet.getColumns();
                for (TableDataView.Column c : cols) {
                    if (isNumeric(c) && !c.getName().equals(xCol)) {
                        yCol = c.getName();
                        break;
                    }
                }
                if (yCol == null) {
                    throw new IllegalArgumentException("Y column is not found in the data set.");
                }
            }
            yColIdx = model.getColumnIndex(yCol);
            yColUnits = dataSet.getColumn(yColIdx).getUnits();
        }

        orderCol = meta.findOrderColName(colNames);
        if (orderCol == null) {
            hasOrder = false;
        } else {
            hasOrder = true;
            orderColIdx = model.getColumnIndex(orderCol);
        }
        errorCol = meta.findErrorColName(colNames);
        if (errorCol == null) {
            hasError = false;
        } else {
            hasError = true;
            errorColIdx = model.getColumnIndex(errorCol);
            errorColUnits = dataSet.getColumn(errorColIdx).getUnits();
            if (yColUnits != null && !yColUnits.equals(errorColUnits)) hasError = false;
            else if (yColUnits == null && errorColUnits != null) hasError = false;
        }
        curves = new ArrayList<Curve>();
        HashMap<String, Curve> curvesByOrder = new HashMap<String, Curve>();
        HashMap<String, Integer> curveIdByOrder = new HashMap<String, Integer>();

        String order="0";
        int curveId = 0;
        curveIdByOrder.put(order, curveId);

        double x, y, error=-1;
        String xStr, yStr, errorStr=null;
        int rowIdx; // for the connection with the table


        double withErrorMin=Double.POSITIVE_INFINITY, withErrorMax=Double.NEGATIVE_INFINITY;

        final int xColIdxF=xColIdx, yColIdxF= yColIdx;
        Sampler sampler = new Sampler(new Sampler.SamplePointGetter() {

            public Sampler.SamplePoint getValue(int rowIdx, TableData.Row row) {
                double x,y;
                try {
                    if (xExpr) {
                        for (String v : xColExpr.getParsedVariables()) {
                            xColExpr.setVariableValue(v, Double.parseDouble(row.getValue(v).toString()));
                        }
                        x = xColExpr.getValue();
                    } else {
                        x = Double.parseDouble(row.getValue(xColIdxF).toString());
                    }
                } catch (Exception e) {
                    return null;
                }
                try {
                    if (yExpr) {
                        for (String v : yColExpr.getParsedVariables()) {
                            yColExpr.setVariableValue(v, Double.parseDouble(row.getValue(v).toString()));
                        }
                        y = yColExpr.getValue();
                    } else {
                        y = Double.parseDouble(row.getValue(yColIdxF).toString());
                    }
                } catch (Exception e) {
                    return null;
                }

                if (x < xDatasetMin) xDatasetMin = x;
                if (x > xDatasetMax) xDatasetMax = x;

                if (y < yDatasetMin) yDatasetMin = y;
                if (y > yDatasetMax) yDatasetMax = y;

                if (withinLimits(x, y, meta)) {
                    return new Sampler.SamplePoint(x,y,rowIdx);
                } else {
                    return null;
                }
            }
        });

        TableData.Row row;
        List<TableData.Row> rows = model.getRows();
        for (Sampler.SamplePoint sp : sampler.sample(rows)) {
            rowIdx = sp.getRowIdx();
            row = rows.get(rowIdx);  // row.getRowIdx() returns absolute index
            x = sp.getX();
            y = sp.getY();

            if (xExpr) {
                xStr = formatValue(sp.getX());
            } else {
                xStr = row.getValue(xColIdx).toString();
            }
            if (yExpr) {
                yStr = formatValue(sp.getY());
            } else {
                yStr = row.getValue(yColIdx).toString();
            }

            if (hasOrder) {
                order = row.getValue(orderColIdx).toString();
                if (!curveIdByOrder.containsKey(order)) {
                    curveId++;
                    curveIdByOrder.put(order, curveId);
                }
            }

            if (hasError) {
                try {
                    errorStr = row.getValue(errorColIdx).toString();
                    error = Double.parseDouble(errorStr);
                    if (error >= 0) {
                        if (y-error < withErrorMin) withErrorMin = y-error;
                        if (y+error > withErrorMax) withErrorMax = y+error;
                    } else {
                        error = Double.NaN;
                    }
                } catch (Throwable th) {
                    error = Double.NaN;
                }
            }


            Curve aCurve;
            if (curvesByOrder.containsKey(order)) {
                aCurve = curvesByOrder.get(order);
            } else {
                aCurve = new Curve(hasError, order, curveIdByOrder.get(order), !meta.plotDataPoints().equals(XYPlotMeta.PlotStyle.POINTS));
                curves.add(aCurve);
                curvesByOrder.put(order, aCurve);
            }
            aCurve.addPoint(new Point(rowIdx, x, xStr, y, yStr, error, errorStr, sp.getRepresentedRows()));
        }

        numPointsInSample = sampler.getNumPointsInSample();
        numPointsRepresented = sampler.getNumPointsRepresented();

        xMinMax = sampler.getXMinMax();
        yMinMax = sampler.getYMinMax();
        xDatasetMinMax = new MinMax(xDatasetMin, xDatasetMax);
        yDatasetMinMax = new MinMax(yDatasetMin, yDatasetMax);
        if (hasError) withErrorMinMax = new MinMax(withErrorMin, withErrorMax);

        // check if specific points are present
        TableMeta tblMeta = dataSet.getMeta();
        if (tblMeta.contains(SpecificPoints.SERIALIZATION_KEY)) {
            String serializedValue = tblMeta.getAttribute(SpecificPoints.SERIALIZATION_KEY);
            /*
            Specific points to be plotted might be present in metadata
            */
            SpecificPoints specificPoints;
            try {
                if (!StringUtils.isEmpty(serializedValue)) {
                    specificPoints = SpecificPoints.parse(serializedValue);

                    if (xExpr || yExpr) {
                        adjustedSpecificPoints = new SpecificPoints();
                        adjustedSpecificPoints.setDescription(specificPoints.getDescription());
                        String defaultXName = meta.findDefaultXColName(colNames);
                        String defaultYName = meta.findDefaultYColName(colNames);
                        double adjustedRef;
                        boolean failure = false;
                        MinMax adjustedXMinMax, adjustedYMinMax;
                        for (int si=0; si< specificPoints.getNumPoints(); si++) {
                            SpecificPoints.Point sp = specificPoints.getPoint(si);
                            MinMax spXMinMax = sp.getXMinMax();
                            MinMax spYMinMax = sp.getYMinMax();
                            if (xExpr) {
                                for (String v : xColExpr.getParsedVariables()) {
                                    // can only adjust, when default x and y are referenced
                                    if (v.equals(defaultXName)) {
                                        xColExpr.setVariableValue(v, spXMinMax.getReference());
                                    } else if (v.equals(defaultYName)) {
                                        xColExpr.setVariableValue(v, spYMinMax.getReference());
                                    } else {
                                        failure = true;
                                        break;
                                    }
                                }
                                if (failure) break;
                                adjustedRef = xColExpr.getValue();
                                adjustedXMinMax = new MinMax(adjustedRef, adjustedRef);
                            } else {
                                adjustedXMinMax = spXMinMax;
                            }
                            if (yExpr) {
                                for (String v : yColExpr.getParsedVariables()) {
                                    // can only adjust, when default x and y are referenced
                                    if (v.equals(defaultXName)) {
                                        yColExpr.setVariableValue(v, spXMinMax.getReference());
                                    } else if (v.equals(defaultYName)) {
                                        yColExpr.setVariableValue(v, spYMinMax.getReference());
                                    } else {
                                        failure = true;
                                        break;
                                    }
                                }
                                if (failure) break;
                                adjustedRef = yColExpr.getValue();
                                adjustedYMinMax = new MinMax(adjustedRef, adjustedRef);
                            } else {
                                adjustedYMinMax = spYMinMax;
                            }
                            if (!failure ) {
                                adjustedSpecificPoints.addPoint(sp.getId(),sp.getLabel(),sp.getDesc(), adjustedXMinMax, adjustedYMinMax);
                            } else {
                                PopupUtil.showError("Error","Can not calculate specific XY points for the given expressions");
                                break;
                            }
                        }
                    } else {
                        adjustedSpecificPoints = specificPoints;
                    }
                }
            } catch (Exception e) {
                adjustedSpecificPoints = null;
            }
        }
    }

    /**
     *  @return true if data are sampled: one point represents several data points or rows
     */
    public boolean isSampled() { return numPointsInSample != numPointsRepresented; }
    public int getNumPointsInSample() { return numPointsInSample; }
    public int getNumPointsRepresented() { return numPointsRepresented; }
    public static boolean shouldSample(int numRows) { return Sampler.shouldSample(numRows);}

    public static String formatValue(double value) {
        String fstr;
        double absV= Math.abs(value);
        if (absV < 0.01 || absV >= 1000.) {
            fstr= _nfExp.format(value);
        }
        else {
            fstr= _nf.format(value);
        }
        return fstr;

    }

    private boolean isNumeric(TableDataView.Column c) {
        String type = c.getType();
        return !(type == null || type.startsWith("c") || type.equals("date"));
    }

    private static boolean withinLimits(double x, double y, XYPlotMeta meta) {
        MinMax xLimits = meta.userMeta.getXLimits();
        MinMax yLimits = meta.userMeta.getYLimits();
        return  (xLimits == null || ((x >= xLimits.getMin()) && (x <=  xLimits.getMax()))) &&
                (yLimits == null || ((y >= yLimits.getMin()) && (y <=  yLimits.getMax())));
    }

    public boolean hasError() {return hasError;}
    public boolean hasOrder() {return hasOrder;}
    public boolean hasSpecificPoints() {return adjustedSpecificPoints != null && adjustedSpecificPoints.getNumPoints() > 0; }
    public List<Curve> getCurveData() {return curves;}
    public SpecificPoints getSpecificPoints() { return adjustedSpecificPoints; }
    public String getXCol() {return xCol;}
    public String getYCol() {return yCol;}
    public String getErrorCol() {return errorCol; }
    public String getOrderCol() {return orderCol; }
    public String getXUnits() {return xColUnits; }
    public String getYUnits() {return yColUnits; }
    public String getErrorColUnits() {return errorColUnits; }
    public MinMax getXMinMax() {return xMinMax; }
    public MinMax getYMinMax() {return yMinMax; }
    public MinMax getXDatasetMinMax() {return xDatasetMinMax; }
    public MinMax getYDatasetMinMax() {return yDatasetMinMax; }
    public MinMax getWithErrorMinMax() {return withErrorMinMax; }


    public int getNPoints(MinMax xMinMax, MinMax yMinMax) {
        int nPoints = 0;
        double xMin = xMinMax.getMin();
        double xMax = xMinMax.getMax();
        double yMin = yMinMax.getMin();
        double yMax = yMinMax.getMax();

        double x,y;
        for (Curve c : curves) {
            for (Point p : c.getPoints()) {
                x = p.getX();
                y = p.getY();
                if (x > xMin && x < xMax && y > yMin && y < yMax) {
                    nPoints++;
                }
            }
        }
        return nPoints;
    }

    public Point getPoint(int curveIdx, int pointIdx) {
        Point ret = null;
        for (Curve curve : curves) {
            if (curve.getCurveIdx() == curveIdx) {
                ret =  curve.getPoints().get(pointIdx);
            }
        }
        return ret;
    }

    public Integer [] getRepresentedRowIds(List<Point> samplePoints) {
        HashSet<Integer> rowIdx = new HashSet<Integer>();

        for (XYPlotData.Point p : samplePoints) {
            List<Integer> representedRows = p.getRepresentedRows();
            if (representedRows != null && representedRows.size()>0) {
                rowIdx.addAll(representedRows);
            } else {
                rowIdx.add(p.getRowIdx());
            }
        }
        return rowIdx.toArray(new Integer[rowIdx.size()]);
    }



    public static class Curve {
        String orderVal;

        List<Point> points;
        boolean hasError;

        int id; // to preserve display of a given curve even i its curveIdx changes

        // set when data are plotted
        int curveIdx;
        int errorUpperCurveIdx;
        int errorLowerCurveIdx;

        boolean needsSorting;


        public Curve(boolean hasError, String order, int id, boolean needsSorting) {

            this.points = new ArrayList<Point>();
            this.hasError = hasError;
            this.orderVal = order;
            this.id = id;
            this.needsSorting = needsSorting;
        }

        public void addPoint(Point point) {
            this.points.add(point);
        }

        public void setCurveIdx(int curveIdx) {
            this.curveIdx = curveIdx;
        }

        public void setErrorIdx(int lowerCurveIdx, int upperCurveIdx) {
            this.errorLowerCurveIdx = lowerCurveIdx;
            this.errorUpperCurveIdx = upperCurveIdx;
        }

        public int getCurveId() {return id; }
        public int getCurveIdx() { return curveIdx; }
        public int getErrorLowerCurveIdx() { return errorLowerCurveIdx; }
        public int getErrorUpperCurveIdx() {return errorUpperCurveIdx; }

        public List<Point> getPoints() {
            if (needsSorting) {
                Collections.sort(points, new Comparator<Point>() {
                    public int compare(Point p1, Point p2) {
                        return new Double(p1.getX()).compareTo(p2.getX());
                    }
                });
                needsSorting = false;
            }
            return points;
        }

        public String getOrder() { return orderVal; }

        public Point getRepresentativeSamplePoint(int rowIdx) {
            if (rowIdx < 0) return null;
            for (Point pt : getPoints()) {
                if (pt.getRowIdx() == rowIdx) {
                    return pt;
                } else {
                    List<Integer> representedRows = pt.getRepresentedRows();
                    if (representedRows != null && representedRows.size()>1) {
                        if (Collections.binarySearch(representedRows, rowIdx) >= 0) {
                            return pt;
                        }
                    }
                }
            }
            return null;
        }
    }

    public static class Point {
        int rowIdx;

        double x;
        double y;
        double error; // standard deviation - should not be less than 0

        String xStr;
        String yStr;
        String errorStr;

        List<Integer> representedRows; // row indexes that this point represents

        public Point(int rowIdx, double x, String xStr, double y, String yStr, double error, String errorStr, List<Integer>representedRows) {
            this.rowIdx = rowIdx;
            this.x = x;
            this.y = y;
            this.error = error;
            this.xStr = xStr;
            this.yStr = yStr;
            this.errorStr = errorStr;
            this.representedRows = representedRows;
        }

        // returns data set absolute index (not original index)
        public int getRowIdx() {return rowIdx;}
        // returns data set absolute indexes  of the represented rows
        public List<Integer> getRepresentedRows() {return representedRows;}

        public double getX() {return x;}
        public double getY() {return y;}
        public double getError() {return error;}
        public String getXStr() {return xStr;}
        public String getYStr() {return yStr;}
        public String getErrorStr() {return errorStr == null ? "" : errorStr;}

    }

}
