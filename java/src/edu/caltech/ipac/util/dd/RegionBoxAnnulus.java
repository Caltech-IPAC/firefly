/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley, Trey Roby
 */
public class RegionBoxAnnulus extends Region {


    private RegionValue rotation;
    private RegionDimension[] dim;

    private RegionBoxAnnulus() { super(null); }

    public RegionBoxAnnulus(WorldPt pt, RegionValue rotation, RegionDimension... dim) {
        super(pt);
        this.rotation = rotation;
        this.dim = dim;
    }

    public RegionValue getRotation() { return rotation; }

    public RegionDimension[] getDim() { return dim; }

    @Override
    public String getDesc() { return "Box Annulus"; }
}

