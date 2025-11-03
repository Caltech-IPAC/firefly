/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.ProjectionException;

public class SansonFlamsteedProjection {


    static double dtr = Projection.dtr;

    static public ProjectionPt RevProject(double lon, double lat,
                                          ProjectionParams hdr, boolean useProjException) throws ProjectionException {
        double fline, fsamp, rtwist, temp, rlat;
        double x, y;

        double crpix1 = hdr.crpix1;
        double crpix2 = hdr.crpix2;
        double glong = hdr.crval1;
        double glat = hdr.crval2;
        double cdelt1 = hdr.cdelt1;
        double cdelt2 = hdr.cdelt2;
        double twist = hdr.crota2;
        boolean using_cd = hdr.using_cd;
        double dc1_1 = hdr.dc1_1;
        double dc1_2 = hdr.dc1_2;
        double dc2_1 = hdr.dc2_1;
        double dc2_2 = hdr.dc2_2;
        double xx, yy;

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH SansonFlamsteedProjection.RevProject input lon = " + lon +
	    "  lat = " + lat);
	*/

        LonLat result = ProjectionUtil.celestialToNative(lon, lat, glong, glat);
        if (result == null) {
            if (useProjException) throw new ProjectionException("Failed to convert to native coordinates");
            return null;
        }
        xx = result.lon();
        yy = result.lat();

        // SFL projection-specific: scale x by cos(y)
        xx = xx * Math.cos(yy * dtr);

        if (using_cd) {
            fsamp = dc1_1 * xx + dc1_2 * yy;
            fline = dc2_1 * xx + dc2_2 * yy;
        } else {
            fsamp = xx / cdelt1;
            fline = yy / cdelt2;
        }


	/*
	rlat = lat * dtr;
	fsamp = ((lon - glong) / cdelt1) * Math.cos(rlat);
	fline = ((lat - glat) / cdelt2);
	*/


        /* do the twist */
        rtwist = -twist * dtr;       /* convert to radians */
        temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
        fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
        fsamp = temp;


	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH SansonFlamsteedProjection.RevProject fsamp = " + fsamp + 
	    "  fline = " + fline);
	*/
        x = fsamp + crpix1 - 1;
        y = fline + crpix2 - 1;

        ProjectionPt image_pt = new ProjectionPt(x, y);
        return (image_pt);
    }

    static public Pt FwdProject(double x, double y, ProjectionParams hdr, boolean useProjException)
            throws ProjectionException {
        double fsamp, fline;
        double lat, lon;
        double rtwist, temp;
        double xx, yy;

        double crpix1 = hdr.crpix1;
        double crpix2 = hdr.crpix2;
        double glong = hdr.crval1;
        double glat = hdr.crval2;
        double cdelt1 = hdr.cdelt1;
        double cdelt2 = hdr.cdelt2;
        double twist = hdr.crota2;
        boolean using_cd = hdr.using_cd;
        double cd1_1 = hdr.cd1_1;
        double cd1_2 = hdr.cd1_2;
        double cd2_1 = hdr.cd2_1;
        double cd2_2 = hdr.cd2_2;

        /*
        if (SUTDebug.isDebug())
        {
            System.out.println(
            "RBH SansonFlamsteedProjection.FwdProject: " + hdr.maptype);
            System.out.println(
            "RBH SansonFlamsteedProjection.FwdProject: input x = " + x + "  y = " + y);
            //Thread.currentThread().dumpStack();
        }
        */
        fsamp = x - crpix1 + 1;
        fline = y - crpix2 + 1;


        rtwist = -twist * dtr;       /* convert to radians */
        temp = fsamp * Math.cos(rtwist) - fline * Math.sin(rtwist); /* do twist */
        fline = fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
        fsamp = temp;


        if (using_cd) {
            xx = (cd1_1 * fsamp + cd1_2 * fline);
            yy = (cd2_1 * fsamp + cd2_2 * fline);
        } else {
            xx = fsamp * cdelt1;
            yy = fline * cdelt2;
        }
        // SFL projection-specific: unscale x by dividing by cos(y)
        if (Math.cos(yy * dtr) == 0) {
            xx = 0;
        } else {
            xx = xx / Math.cos(yy * dtr);
        }

        LonLat result = ProjectionUtil.nativeToCelestial(xx, yy, glong, glat);
        if (result == null) {
            if (useProjException) throw new ProjectionException("Failed to convert to celestial coordinates");
            return null;
        }
        lon = result.lon();
        lat = result.lat();

        /*
        if (SUTDebug.isDebug())
            System.out.println(
            "RBH SansonFlamsteedProjection.FwdProject: output lon = " + lon +
            "  lat = " + lat);
        */
        Pt _pt = new Pt(lon, lat);
        return (_pt);
    }

}
