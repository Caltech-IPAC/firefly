/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley
 */
public class RegionPoint extends Region {

    public enum PointType {Circle,Box,Diamond,Cross,X,Arrow,BoxCircle}

    private PointType pointType;

    private RegionPoint() { super(null); }

    private int pointSize= -1;

    public RegionPoint(WorldPt pt, PointType pointType, int pointSize) {
        super(pt);
        this.pointType = pointType;
        this.pointSize= pointSize;
    }

    public PointType getPointType() { return pointType; }

    public int getPointSize() { return pointSize; }

    @Override
    public String getDesc() {
        return pointType.toString();
    }
}

