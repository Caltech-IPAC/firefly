package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley
 */
public class RegionAnnulus extends Region {


    private RegionValue[] radii;

    private RegionAnnulus() { super(null); }

    public RegionAnnulus(WorldPt pt, RegionValue... radii) {
        super(pt);
        this.radii = radii;
    }

    public RegionValue[] getRadii() { return radii; }

    public boolean isCircle() { return radii.length==1; }

    @Override
    public String getDesc() { return  isCircle() ? "Circle" : "Annulus"; }
}

