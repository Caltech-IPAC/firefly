/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: 2/8/13
 * Time: 2:19 PM
 */


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
}

