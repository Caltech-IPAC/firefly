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
public class RegionLines extends Region {

    private WorldPt ptAry[];


    @JsNoExport
    private RegionLines() { super(null);}

    public RegionLines(WorldPt... ptAry)  {
        super(ptAry[0]);
        this.ptAry= ptAry;
    }

    public WorldPt[] getPtAry() {
        return ptAry;
    }

    public boolean isPolygon() { return ptAry.length>2; }

    @Override
    public String getDesc() {
        return isPolygon() ? "Polygon" : "Line";
    }


    public static RegionLines makeRegionLines(String wpStrAry[]) {
        WorldPt ptAry[]= new WorldPt[wpStrAry.length];
        for(int i=0; (i<wpStrAry.length); i++) {
            ptAry[i]= WorldPt.parse(wpStrAry[i]);
        }
        return new RegionLines(ptAry);
    }



}

