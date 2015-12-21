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
 * @author Booth Hartley
 */
@JsExport
@JsType
public class RegionAnnulus extends Region {


    private RegionValue[] radii;

    @JsNoExport
    private RegionAnnulus() { super(null); }

    public RegionAnnulus(WorldPt pt, RegionValue... radii) {
        super(pt);
        this.radii = radii;
    }

    public RegionValue[] getRadii() { return radii; }

    public boolean isCircle() { return radii.length==1; }

    @Override
    public String getDesc() { return  isCircle() ? "Circle" : "Annulus"; }


    public static RegionAnnulus makeRegionAnnulus(String wpStr, RegionValue radii[]) {
        WorldPt wp= WorldPt.parse(wpStr);
        return new RegionAnnulus(wp,radii);
    }


}

