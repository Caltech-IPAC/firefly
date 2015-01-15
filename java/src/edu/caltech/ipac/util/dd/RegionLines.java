/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley, Trey Roby
 */
public class RegionLines extends Region {

    private WorldPt ptAry[];


    private RegionLines() { super(null);}

    public RegionLines(WorldPt... ptAry)  {
        super(ptAry[0]);
        this.ptAry= ptAry;
    }

    public WorldPt[] getPtAry() {
        return ptAry;
    }

    public boolean isPolygon() { return ptAry.length>2; }

    @Override
    public String getDesc() {
        return isPolygon() ? "Polygon" : "Line";
    }
}

