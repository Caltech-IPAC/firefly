/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley
 */
public class RegionEllipse extends Region {

   private RegionValue radius1;
   private RegionValue radius2;
   private RegionValue angle;

    private RegionEllipse() {super(null); }

    public RegionEllipse(WorldPt pt, RegionValue radius1, RegionValue radius2, RegionValue angle) {
        super(pt);
        this.radius1 = radius1;
        this.radius2 = radius2;
        this.angle = angle;
    }

    public RegionValue getRadius1() { return radius1; }

    public RegionValue getRadius2() { return radius2; }

    public RegionValue getAngle() { return angle; }

    @Override
    public String getDesc() { return "Ellipse Annulus"; }
}

