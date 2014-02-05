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
            if (values.length > 2) {
                dinfo.setMaxPoints(StringUtils.getInt(values[2], 0));
            }
            if (values.length > 3) {
                dinfo.setXyRatio(StringUtils.getFloat(values[3], 1));
            }
            return dinfo.isValid() ? dinfo : null;
        }
        return null;
    }

    @Override
    public String toString() {
        String s = DECIMATE_TAG + "=" + xColumnName + "," + yColumnName;
        s += maxPoints == 0 ? "" : "," + maxPoints;
        s += xyRatio == 1 ? "" : "," + xyRatio;
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
