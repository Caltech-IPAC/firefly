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
public class RegionText extends Region {




    @JsNoExport
    private RegionText() { super(null);}

    public RegionText(WorldPt pt) {
        super(pt);
    }

    @Override
    public String getDesc() {
        return "Text";
    }


    public static RegionText makeRegionText(String serializedWP) {
        WorldPt wp= WorldPt.parse(serializedWP);
        return new RegionText(wp);
    }
}

