package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley
 */
public class RegionEllipseAnnulus extends Region {


    private RegionValue angle;
    private RegionValue[] radii;

    private RegionEllipseAnnulus() { super(null); }

    public RegionEllipseAnnulus(WorldPt pt, RegionValue angle, RegionValue... radii) {
        super(pt);
        this.angle = angle;
        this.radii = radii;
    }

    public RegionValue getAngle() {
        return angle;
    }

    public RegionValue[] getRadii() {
        return radii;
    }

    @Override
    public String getDesc() { return "Ellipse Annulus"; }
}

