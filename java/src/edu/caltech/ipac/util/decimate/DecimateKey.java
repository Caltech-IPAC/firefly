package edu.caltech.ipac.util.decimate;

import edu.caltech.ipac.util.StringUtils;

/**
 * This is a class, which holds the information
 * necessary to calculate a unique cell key in
 * server side decimation algorithm, which
 * divides xy plane into equal size cells,
 * and preserves one row per cell.
 * @author tatianag
 */
public class DecimateKey {
    public final static String DECIMATE_KEY = "decimate_key";
    public final static String XY_SEPARATOR = ":";

    double xMin;
    double yMin;

    int nX;
    int nY;

    double xUnit;
    double yUnit;

    String xColNameOrExpr;
    String yColNameOrExpr;

    public DecimateKey(double xMin, double yMin, int nX, int nY, double xUnit, double yUnit) {
        this.xMin = xMin;
        this.yMin= yMin;
        this.nX = nX;
        this.nY = nY;
        this.xUnit = xUnit;
        this.yUnit = yUnit;
        this.xColNameOrExpr = null;
        this.yColNameOrExpr = null;
    }

    public String getKey(double xval, double yval) {
        return getXIdx(xval) + XY_SEPARATOR + getYIdx(yval);
    }

    public int getXIdx(double xval) {
        return (int)((xval-xMin)/xUnit);
    }

    public int getYIdx(double yval) {
        return (int)((yval-yMin)/yUnit);
    }

    public double getCenterX(double xval) {
        return xMin+((int)((xval-xMin)/xUnit)+0.5)*xUnit;
    }

    public double getCenterY(double yval) {
        return yMin+((int)((yval-yMin)/yUnit)+0.5)*yUnit;
    }

    public void setCols(String xColNameOrExpr, String yColNameOrExpr) {
        this.xColNameOrExpr = xColNameOrExpr;
        this.yColNameOrExpr = yColNameOrExpr;
    }

    public String getXCol() {return xColNameOrExpr; }
    public String getYCol() {return yColNameOrExpr; }

    public int getNX() {return nX; }
    public int getNY() {return nY; }


    public double getXUnit() {return xUnit; }
    public double getYUnit() {return yUnit; }


    public static DecimateKey parse(String str) {
        if (StringUtils.isEmpty(str)) return null;
        try {
            String v = str.replace(DECIMATE_KEY, "");
            if (v.length() < 3) return null;
            v = v.substring(1,v.length()-1); // remove outer braces
            String [] parts = v.split(",");
            if (parts.length == 8) {
                String xColNameOrExpr = parts[0];
                String yColNameOrExpr = parts[1];
                double xMin = Double.parseDouble(parts[2]);
                double yMin = Double.parseDouble(parts[3]);
                int nX = Integer.parseInt(parts[4]);
                int nY = Integer.parseInt(parts[5]);
                double xUnit = Double.parseDouble(parts[6]);
                double yUnit = Double.parseDouble(parts[7]);
                DecimateKey key = new DecimateKey(xMin,yMin,nX,nY,xUnit,yUnit);
                key.setCols(xColNameOrExpr, yColNameOrExpr);
                return key;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return DECIMATE_KEY+"("+xColNameOrExpr+","+yColNameOrExpr+","+
                xMin+","+yMin+","+nX+","+nY+","+xUnit+","+yUnit+")";
    }
}
