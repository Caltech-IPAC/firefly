/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley, Trey Roby
 */
public class RegionText extends Region {



    private RegionText() { super(null);}

    public RegionText(WorldPt pt) {
        super(pt);
    }

    @Override
    public String getDesc() {
        return "Text";
    }
}

