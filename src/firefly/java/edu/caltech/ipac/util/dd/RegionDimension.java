/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;

/**
 * @author Trey Roby
 */
public class RegionDimension {

    private RegionValue width;
    private RegionValue height;

    public RegionDimension(RegionValue width, RegionValue height) {
        this.width = width;
        this.height = height;
    }

    public RegionValue getWidth() { return width; }
    public RegionValue getHeight() { return height; }

    public static RegionDimension makeRegionDimention(RegionValue width, RegionValue height) {
        return new RegionDimension(width,height);
    }
}

