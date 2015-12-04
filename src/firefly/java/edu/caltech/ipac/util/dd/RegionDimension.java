/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;
/**
 * User: roby
 * Date: 2/8/13
 * Time: 2:19 PM
 */


import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

/**
 * @author Trey Roby
 */
@JsExport
@JsType
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

