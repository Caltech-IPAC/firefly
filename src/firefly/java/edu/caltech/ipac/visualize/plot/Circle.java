/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.ComparisonUtil;

/* This class contains a circle */
public record Circle(WorldPt center, double radius) {

    public String toString() { return "ra: " + center.getLon() + ", dec: " + center.getLat() + ", radius: " + radius; }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        else if (other instanceof Circle r) return radius == r.radius && ComparisonUtil.equals(center, r.center);
        return false;
    }
}
