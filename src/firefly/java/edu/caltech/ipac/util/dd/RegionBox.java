/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsNoExport;
import com.google.gwt.core.client.js.JsType;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley, Trey Roby
 */
@JsExport
@JsType
public class RegionBox extends Region {

   private RegionDimension dim;
   private RegionValue angle;


    @JsNoExport
    private RegionBox() { super(null); }

    @JsNoExport
    public RegionBox(WorldPt pt, RegionDimension dim) {
        this(pt,dim, new RegionValue(0, RegionValue.Unit.SCREEN_PIXEL));
    }

    public RegionBox(WorldPt pt, RegionDimension dim, RegionValue angle) {
        super(pt);
        this.angle = angle;
        this.dim = dim;
    }

    public static RegionBox makeRegionBox(String wpStr, RegionDimension dim) {
        return new RegionBox(WorldPt.parse(wpStr),dim,angel);
    }

    public RegionDimension getDim() { return dim; }

    public RegionValue getAngle() { return angle; }

    @Override
    public String getDesc() { return "Box"; }
}

