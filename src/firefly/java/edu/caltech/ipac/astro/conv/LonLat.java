/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

@JsExport
@JsType
public class LonLat {
    public double lon;
    public double lat;

    public LonLat(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public double getLon() {return this.lon; }
    public double getLat() {return this.lat; }


    @Override
    public String toString() {
        return "lon: "+this.lon+", lat: "+this.lat;
    }
}


