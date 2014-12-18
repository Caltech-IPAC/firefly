package edu.caltech.ipac.firefly.data;

import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;

/**
 * Date: Jan 31, 2014
 *
 * @author loi
 * @version $Id: SortInfo.java,v 1.5 2010/10/21 22:15:53 loi Exp $
 */
public class DecimateInfo implements Serializable, Comparable {

    public static final String DECIMATE_TAG = "decimate";
    private String xColumnName;
    private String yColumnName;
    private int maxPoints = 0;          // 0 is server's default
    private float xyRatio = 1;          // 1 is a square sample area.

    private double xMin = Double.NaN;
    private double xMax = Double.NaN;
    private double yMin = Double.NaN;
    private double yMax = Double.NaN;

    public DecimateInfo() {
    }

    public DecimateInfo(String xColumnName, String yColumnName) {
        this(xColumnName, yColumnName, 0, 1);
    }

    public DecimateInfo(String xColumnName, String yColumnName, int maxPoints, float xyRatio) {
        this.xColumnName = xColumnName;
        this.yColumnName = yColumnName;
        this.maxPoints = maxPoints;
        this.xyRatio = xyRatio;
    }

    public String getxColumnName() {
        return xColumnName;
    }

    public void setxColumnName(String xColumnName) {
        this.xColumnName = xColumnName;
    }

    public String getyColumnName() {
        return yColumnName;
    }

    public void setyColumnName(String yColumnName) {
        this.yColumnName = yColumnName;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public float getXyRatio() {
        return xyRatio;
    }

    public void setXyRatio(float xyRatio) {
        this.xyRatio = xyRatio;
    }

    public double getXMin() {
        return this.xMin;
    }

    public void setXMin(double xMin) {
        this.xMin = xMin;
    }

    public double getXMax() {
        return this.xMax;
    }

    public void setXMax(double xMax) {
        this.xMax = xMax;
    }

    public double getYMin() {
        return this.yMin;
    }

    public void setYMin(double yMin) {
        this.yMin = yMin;
    }

    public double getYMax() {
        return this.yMax;
    }

    public void setYMax(double yMax) {
        this.yMax = yMax;
    }

    public boolean isValid() {
        return !StringUtils.isEmpty(xColumnName) && !StringUtils.isEmpty(yColumnName);
    }

    public static DecimateInfo parse(String str) {
        if (StringUtils.isEmpty(str)) return null;
        String[] kv = str.split("=", 2);
        if (kv != null && kv.length == 2 && kv[0].equals(DECIMATE_TAG)) {
            String[] values = kv[1].split(",");
            DecimateInfo dinfo = new DecimateInfo();

            if (values.length > 1) {
                dinfo.setxColumnName(values[0]);
                dinfo.setyColumnName(values[1]);
            }
            if (values.length > 2 && !StringUtils.isEmpty(values[2])) {
                dinfo.setMaxPoints(StringUtils.getInt(values[2], 0));
            }
            if (values.length > 3 && !StringUtils.isEmpty(values[3])) {
                dinfo.setXyRatio(StringUtils.getFloat(values[3], 1));
            }
            if (values.length > 4 && !StringUtils.isEmpty(values[4])) {
                dinfo.setXMin(StringUtils.getDouble(values[4]));
            }
            if (values.length > 5 && !StringUtils.isEmpty(values[5])) {
                dinfo.setXMax(StringUtils.getDouble(values[5]));
            }
            if (values.length > 6 && !StringUtils.isEmpty(values[6])) {
                dinfo.setYMin(StringUtils.getDouble(values[6]));
            }
            if (values.length > 7 && !StringUtils.isEmpty(values[7])) {
                dinfo.setYMax(StringUtils.getDouble(values[7]));
            }

            return dinfo.isValid() ? dinfo : null;
        }
        return null;
    }

    @Override
    public String toString() {
        String s = DECIMATE_TAG + "=" + xColumnName + "," + yColumnName;
        s += "," + (maxPoints == 0 ? "" : maxPoints);
        s += "," + (xyRatio == 1 ? "" : xyRatio);
        s += "," + (Double.isNaN(xMin) ? "" : xMin);
        s += "," + (Double.isNaN(xMax) ? "" : xMax);
        s += "," + (Double.isNaN(yMin) ? "" : yMin);
        s += "," + (Double.isNaN(yMax) ? "" : yMax);
        return s;
    }

//====================================================================
//  Implements Comparable
//====================================================================

    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof DecimateInfo &&
                obj.toString().equals(toString());
    }
}
