package edu.caltech.ipac.firefly.visualize.graph;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.SpecificPoints;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.util.MinMax;
import edu.caltech.ipac.util.decimate.DecimateKey;
import edu.caltech.ipac.util.expr.Expression;
import edu.caltech.ipac.util.StringUtils;

import java.util.*;
import java.util.logging.Level;


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

    // related to sampling
    int minWeight = Integer.MAX_VALUE, maxWeight = Integer.MIN_VALUE;
    int xSampleUnits = 0, ySampleUnits = 0;
    double xSampleUnitSize = 0d, ySampleUnitSize = 0d;


    // order column defines which points should be placed in the same set (plotted by the same curve)
    private boolean hasOrder;

    // the order is based on the number of represented rows (weight)
    private boolean hasWeightBasedOrder;



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

    HashMap<Integer, Integer> decimatedToFullRowIdx = new HashMap<Integer,Integer>();

    DecimateKey decimateKey = null;

    XYPlotData(final DataSet dataSet, final XYPlotMeta meta) {

        TableData model = dataSet.getModel();
        int orderColIdx=-1, errorColIdx=-1, xColIdx=0, yColIdx=0;
        List<String> colNames = model.getColumnNames();

        // if table is decimated it should have decimate_key (attribute and column)
        decimateKey= dataSet.getMeta().getDecimateKey();

        boolean xExpr = meta.userMeta != null && meta.userMeta.xColExpr != null;
        final Expression xColExpr = xExpr ? meta.userMeta.xColExpr : null;
        if (xExpr) {
            // check if the value of expression was already calculated by server
            xCol = dataSet.getMeta().getAttribute(DecimateInfo.DECIMATE_TAG + ".X-COL");
            if (StringUtils.isEmpty(xCol)) {
                xCol = "";
                xColUnits = "";
            } else {
                xExpr = false;
                xColIdx = model.getColumnIndex(xCol);
                xColUnits = dataSet.getColumn(xColIdx).getUnits();
            }
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


        boolean yExpr = meta.userMeta != null && meta.userMeta.yColExpr != null;
        final Expression yColExpr = yExpr ? meta.userMeta.yColExpr : null;
        if (yExpr) {
            // check if the value of expression was already calculated by server
            yCol = dataSet.getMeta().getAttribute(DecimateInfo.DECIMATE_TAG + ".Y-COL");
            if (StringUtils.isEmpty(yCol)) {
                yCol = "";
                yColUnits = "";
            } else {
                yExpr = false;
                yColIdx = model.getColumnIndex(yCol);
                yColUnits = dataSet.getColumn(yColIdx).getUnits();
            }
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

        final boolean hasRowIdx = colNames.contains("rowidx");
        final int rowIdxColIdx = hasRowIdx ? model.getColumnIndex("rowidx") : -1;

        final boolean hasWeight = isDecimatedTable() && colNames.contains("weight");
        final int weightColIdx = hasWeight ? model.getColumnIndex("weight") : -1;

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
        final boolean xExprF = xExpr, yExprF = yExpr;
        Sampler sampler = new Sampler(new Sampler.SamplePointGetter() {

            public Sampler.SamplePoint getValue(int rowIdx, TableData.Row row) {
                double x,y;
                try {
                    if (xExprF) {
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
                    if (yExprF) {
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
                    if (isDecimatedTable()) {
                        try {
                            int fullTableRowIdx = rowIdx;

                            if (hasRowIdx) {
                                fullTableRowIdx = Integer.parseInt(row.getValue(rowIdxColIdx).toString());
                                // TODO: don't need both decimatedToFullRowIdx and fullTableRowIdx fld
                                if (fullTableRowIdx != rowIdx) {
                                    decimatedToFullRowIdx.put(rowIdx, fullTableRowIdx);
                                }
                            }

                            return new Sampler.SamplePointInDecimatedTable(x,y,rowIdx,fullTableRowIdx,
                                    hasWeight ? Integer.parseInt(row.getValue(weightColIdx).toString()) : 1);
                        } catch (Exception e) { return null; }
                    } else {
                        return new Sampler.SamplePoint(x,y,rowIdx);
                    }
                } else {
                    return null;
                }
            }
        });
        if (meta.getXSize()>0&&meta.getYSize()>0) {
            sampler.setXYRatio(meta.getXSize()/meta.getYSize());
            int maxPoints = (int)(meta.getXSize()*meta.getYSize()/25.0); // assuming 5 px symbol
            if (maxPoints < 4) maxPoints = 4;
            if (maxPoints > 6400) maxPoints = 6400;
            sampler.setMaxPoints(maxPoints);
        }

        List<TableData.Row> rows = model.getRows();
        List<Sampler.SamplePoint> samplePoints = sampler.sample(rows);
        numPointsInSample = sampler.getNumPointsInSample();
        numPointsRepresented = sampler.getNumPointsRepresented();
        xSampleUnits = sampler.getXSampleUnits();
        ySampleUnits = sampler.getYSampleUnits();
        xSampleUnitSize = sampler.getXSampleUnitSize();
        ySampleUnitSize = sampler.getYSampleUnitSize();
        xMinMax = sampler.getXMinMax();
        yMinMax = sampler.getYMinMax();
        minWeight = sampler.getMinWeight();
        maxWeight = sampler.getMaxWeight();
        hasWeightBasedOrder = minWeight!=maxWeight;

        TableData.Row row;
        int weight;
        for (Sampler.SamplePoint sp : samplePoints) {

            rowIdx = sp.getRowIdx();
            row = rows.get(rowIdx);  // row.getRowIdx() returns row id
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

            if (hasOrder || hasWeightBasedOrder) {
                if (hasOrder) {
                    order = row.getValue(orderColIdx).toString();
                } else {
                    weight = sp.getWeight();
                    order = getWeightBasedOrder(weight, minWeight, maxWeight);
                }
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
                aCurve = new Curve(hasError, order, curveIdByOrder.get(order), !meta.plotStyle().equals(XYPlotMeta.PlotStyle.POINTS));
                curves.add(aCurve);
                curvesByOrder.put(order, aCurve);
            }
            Point p;
            if (hasError) {
                p = new PointWithError(rowIdx, x, xStr, y, yStr, error, errorStr);
            } else if (isDecimatedTable()) {
                p = new PointInDecimatedSample(rowIdx, x, xStr, y, yStr, sp.getRepresentedRows(), sp.getFullTableRowIdx(), sp.getWeight());
            } else if (isSampled()) {
                p = new PointInSample(rowIdx, x, xStr, y, yStr, sp.getRepresentedRows());
            } else {
                p = new Point(rowIdx, x, xStr, y, yStr);
            }
            aCurve.addPoint(p);
        }
        // sort curves by order
        Collections.sort(curves, new Comparator<Curve>() {
            public int compare(Curve c1, Curve c2) {
                return c1.getOrder().compareTo(c2.getOrder());
            }
        });

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
                    adjustedSpecificPoints = new SpecificPoints();
                    adjustedSpecificPoints.setDescription(specificPoints.getDescription());

                    if (xExpr || yExpr) {
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
                            if (!failure && withinLimits(adjustedXMinMax.getReference(), adjustedYMinMax.getReference(), meta)) {
                                adjustedSpecificPoints.addPoint(sp.getId(),sp.getLabel(),sp.getDesc(), adjustedXMinMax, adjustedYMinMax);
                            } else {
                                PopupUtil.showError("Error","Can not calculate specific XY points for the given expressions");
                                break;
                            }
                        }
                    } else {
                        // discard specific points that are not within limits
                        for (int si=0; si< specificPoints.getNumPoints(); si++) {
                            SpecificPoints.Point sp = specificPoints.getPoint(si);
                            MinMax spXMinMax = sp.getXMinMax();
                            MinMax spYMinMax = sp.getYMinMax();
                            if (withinLimits(spXMinMax.getReference(), spYMinMax.getReference(), meta)) {
                                adjustedSpecificPoints.addPoint(sp.getId(),sp.getLabel(),sp.getDesc(), spXMinMax, spYMinMax);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                adjustedSpecificPoints = null;
            }
        }
        adjustMinMax(meta, dataSet);
    }

    public Point getPoint(XYPlotMeta meta, TableData.Row row) {

        double x,y;
        String xStr, yStr;
        try {
            boolean xExpr = meta.userMeta != null && meta.userMeta.xColExpr != null;
            final Expression xColExpr = xExpr ? meta.userMeta.xColExpr : null;
            if (xExpr) {
                for (String v : xColExpr.getParsedVariables()) {
                    xColExpr.setVariableValue(v, Double.parseDouble(row.getValue(v).toString()));
                }
                x = xColExpr.getValue();
                xStr = formatValue(x);
            } else {
                xStr = row.getValue(xCol).toString();
                x = Double.parseDouble(xStr);
            }

            boolean yExpr = meta.userMeta != null && meta.userMeta.yColExpr != null;
            final Expression yColExpr = yExpr ? meta.userMeta.yColExpr : null;
            if (yExpr) {
                for (String v : yColExpr.getParsedVariables()) {
                    yColExpr.setVariableValue(v, Double.parseDouble(row.getValue(v).toString()));
                }
                y = yColExpr.getValue();
                yStr = formatValue(y);
            } else {
                yStr = row.getValue(yCol).toString();
                y = Double.parseDouble(yStr);
            }
            if (withinLimits(x, y, meta)) {
                return new Point(-1, x, xStr, y, yStr);
            } else {
                return null;
            }
        } catch (Exception e) {
            GwtUtil.getClientLogger().log(Level.WARNING, "XYPlotData.getPoint: "+e.getMessage());
            return null;
        }
    }

    private static String getWeightBasedOrder(int weight, int minWeight, int maxWeight) {
        if (weight == 1) return getCharForNumber(1)+". 1pt";
        else {
            int range =  maxWeight-minWeight-1;
            int n=2;
            int min, max;
            // 10 orders incr=0.10, 5 orders incr=0.20
            for (double incr = 0.20; incr <=1; incr += 0.20) {
                min = (int)Math.round(minWeight+1+(incr-0.20)*range);
                max = (int)Math.round(minWeight+1+incr*range);
                if (weight <= max) {
                    return getCharForNumber(n)+". "+(min==max ? min : (min+"-"+max))+"pts";
                }
                n++;
            }
        }
        return "Z."; // should not happen
    }

    /*
     * @param i number from 1 to 27
     * @return letter of the alphabet for i from 1 to 27, null otherwise
     */
    private static String getCharForNumber(int i) {
        return i > 0 && i < 27 ? String.valueOf((char)(i + 64)) : null;
    }

    public DecimateKey getDecimateKey() { return decimateKey; }

    public boolean isDecimatedTable() {return decimateKey != null; }

    public int getFullTableRowIdx(int dataSetRowIdx) {
        // for decimated tables row idx might be different from full table row idx
        if (decimatedToFullRowIdx.containsKey(dataSetRowIdx)) {
            return decimatedToFullRowIdx.get(dataSetRowIdx);
        } else {
            return dataSetRowIdx;
        }
    }

    /**
     *  @return true if data are sampled: one point represents several data points or rows
     */
    public boolean isSampled() { return numPointsInSample != numPointsRepresented; }
    public int getNumPointsInSample() { return numPointsInSample; }
    public int getNumPointsRepresented() { return numPointsRepresented; }
    public int getXSampleUnits() { return xSampleUnits; }
    public int getYSampleUnits() { return ySampleUnits; }
    public double getXSampleUnitSize() { return xSampleUnitSize; }
    public double getYSampleUnitSize() { return ySampleUnitSize; }
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

    //Adjust for specific points and user's limits
    private void adjustMinMax(XYPlotMeta meta, DataSet dataSet) {

        // adjust for decimated and decimated/zoomed
        if (decimateKey != null) {
            double xMinD = xDatasetMinMax.getMin();
            double xMaxD = xDatasetMinMax.getMax();
            double yMinD = yDatasetMinMax.getMin();
            double yMaxD = yDatasetMinMax.getMax();

            double val;
            String strVal = dataSet.getMeta().getAttribute(DecimateInfo.DECIMATE_TAG + ".X-MIN");
            if (!StringUtils.isEmpty(strVal)) {
                val = StringUtils.parseDouble(strVal);
                xMinD = Math.min(xMinD, val);
            }
            strVal = dataSet.getMeta().getAttribute(DecimateInfo.DECIMATE_TAG + ".X-MAX");
            if (!StringUtils.isEmpty(strVal)) {
                val = StringUtils.parseDouble(strVal);
                xMaxD = Math.max(xMaxD, val);
            }
            strVal = dataSet.getMeta().getAttribute(DecimateInfo.DECIMATE_TAG + ".Y-MIN");
            if (!StringUtils.isEmpty(strVal)) {
                val = StringUtils.parseDouble(strVal);
                yMinD = Math.min(yMinD, val);
            }
            strVal = dataSet.getMeta().getAttribute(DecimateInfo.DECIMATE_TAG + ".Y-MAX");
            if (!StringUtils.isEmpty(strVal)) {
                val = StringUtils.parseDouble(strVal);
                yMaxD = Math.max(yMaxD, val);
            }
            xDatasetMinMax = new MinMax(xMinD, xMaxD);
            yDatasetMinMax = new MinMax(yMinD, yMaxD);
        }

        if (adjustedSpecificPoints != null) {
            // adjust min/max for specific points
            double xMin = xMinMax.getMin();
            double xMax = xMinMax.getMax();
            double yMin = yMinMax.getMin();
            double yMax = yMinMax.getMax();
            double xMinD = xDatasetMinMax.getMin();
            double xMaxD = xDatasetMinMax.getMax();
            double yMinD = yDatasetMinMax.getMin();
            double yMaxD = yDatasetMinMax.getMax();


            for (int si=0; si< adjustedSpecificPoints.getNumPoints(); si++) {
                SpecificPoints.Point sp = adjustedSpecificPoints.getPoint(si);
                MinMax spXMinMax = sp.getXMinMax();
                MinMax spYMinMax = sp.getYMinMax();

                xMin = Math.min(xMin, spXMinMax.getMin());
                xMax = Math.max(xMax, spXMinMax.getMax());
                yMin = Math.min(yMin, spYMinMax.getMin());
                yMax = Math.max(yMax, spYMinMax.getMax());

                xMinD = Math.min(xMinD, spXMinMax.getMin());
                xMaxD = Math.max(xMaxD, spXMinMax.getMax());
                yMinD = Math.min(yMinD, spYMinMax.getMin());
                yMaxD = Math.max(yMaxD, spYMinMax.getMax());
            }
            xMinMax = new MinMax(xMin, xMax);
            yMinMax = new MinMax(yMin, yMax);
            xDatasetMinMax = new MinMax(xMinD, xMaxD);
            yDatasetMinMax = new MinMax(yMinD, yMaxD);
        }

        // if user wants larger range of x, y  - adjust
        if (meta.userMeta.getXLimits() != null) {
            double xMin = xMinMax.getMin(), xMax = xMinMax.getMax();
            if (meta.userMeta.hasXMin()) { xMin = Math.min(meta.userMeta.getXLimits().getMin(), xMin); }
            if (meta.userMeta.hasXMax()) { xMax = Math.max(meta.userMeta.getXLimits().getMax(), xMax); }
            xMinMax = new MinMax(xMin, xMax);
        }
        if (meta.userMeta.getYLimits() != null) {
            double yMin = yMinMax.getMin(), yMax = yMinMax.getMax();
            if (meta.userMeta.hasYMin()) { yMin = Math.min(meta.userMeta.getYLimits().getMin(), yMin); }
            if (meta.userMeta.hasYMax()) { yMax = Math.max(meta.userMeta.getYLimits().getMax(), yMax); }
            yMinMax = new MinMax(yMin, yMax);
        }
    }

    public boolean hasError() {return hasError;}
    public boolean hasOrder() {return hasOrder;}
    public boolean hasWeightBasedOrder() {return hasWeightBasedOrder;}
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

        if (adjustedSpecificPoints != null) {
            for (int si=0; si< adjustedSpecificPoints.getNumPoints(); si++) {
                SpecificPoints.Point sp = adjustedSpecificPoints.getPoint(si);
                x = sp.getXMinMax().getReference();
                y = sp.getYMinMax().getReference();
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

    public Integer [] getRepresentedRows(List<Point> samplePoints) {
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

    }


    public static class Point {
        int rowIdx;

        double x;
        double y;

        String xStr;
        String yStr;

        public Point(int rowIdx, double x, String xStr, double y, String yStr) {
            this.rowIdx = rowIdx;
            this.x = x;
            this.y = y;
            this.xStr = xStr;
            this.yStr = yStr;
        }

        // returns data set absolute index (not original index)
        public int getRowIdx() {return rowIdx;}
        public int getFullTableRowIdx() {return rowIdx;}

        public double getX() {return x;}
        public double getY() {return y;}
        public double getError() {return Double.NaN;}
        public String getXStr() {return xStr;}
        public String getYStr() {return yStr;}
        public String getErrorStr() {return "";}
        public List<Integer> getRepresentedRows() {return null;}
        public int getWeight() {return 1;}

    }

    public static class PointWithError extends Point {
        double error; // standard deviation - should not be less than 0
        String errorStr;

        public PointWithError(int rowIdx, double x, String xStr, double y, String yStr, double error, String errorStr) {
            super(rowIdx, x, xStr, y, yStr);
            this.error = error;
            this.errorStr = errorStr;
        }
        @Override
        public double getError() {return error;}

        @Override
        public String getErrorStr() {return errorStr == null ? "" : errorStr;}
    }

    public static class PointInSample extends Point {

        List<Integer> representedRows; // row indexes that this point represents

        public PointInSample(int rowIdx, double x, String xStr, double y, String yStr, List<Integer>representedRows) {
            super(rowIdx, x, xStr, y, yStr);
            this.representedRows = representedRows;
        }

        @Override
        public List<Integer> getRepresentedRows() {return representedRows;}

        @Override
        public int getWeight() {return representedRows == null ? 1 : representedRows.size();}

    }

    // data are based on decimated table (subset of the full table)
    public static class PointInDecimatedSample extends PointInSample {

        int fullTableIdx;
        int weight;

        public PointInDecimatedSample(int rowIdx, double x, String xStr, double y, String yStr, List<Integer>representedRows, int fullTableIdx, int weight) {
            super(rowIdx, x, xStr, y, yStr, representedRows);
            this.fullTableIdx = fullTableIdx;
            this.weight = weight;
        }

        @Override
        public int getFullTableRowIdx() {return fullTableIdx;}

        @Override
        public int getWeight() {return weight; }

    }


}
