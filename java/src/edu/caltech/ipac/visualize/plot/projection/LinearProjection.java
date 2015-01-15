/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
//import edu.caltech.ipac.util.SUTDebug;


public class LinearProjection{


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;

    static public ProjectionPt RevProject (double lon, double lat, ProjectionParams hdr)
    {
	double          fline, fsamp, rtwist, temp;
	double          x, y;

	double crpix1 = hdr.crpix1;
	double crpix2 = hdr.crpix2;
	double glong  = hdr.crval1;
	double glat   = hdr.crval2;
	double cdelt1 = hdr.cdelt1;
	double cdelt2 = hdr.cdelt2;
	double twist  = hdr.crota2;
	boolean using_cd = hdr.using_cd;
	double dc1_1 = hdr.dc1_1;
	double dc1_2 = hdr.dc1_2;
	double dc2_1 = hdr.dc2_1;
	double dc2_2 = hdr.dc2_2;

	/*
	if (SUTDebug.isDebug())
	{
	    System.out.println(
	    "RBH LinearProjection.RevProject input lon = " + lon + 
	    "  lat = " + lat  + "  glong = " + glong + "  glat = " + glat +
	    "  cdelt1 = " + cdelt1 + "  cdelt2 = " + cdelt2);
	}
	*/

	if (using_cd)
	{
	    fsamp = ((lon - glong) );
	    fline = ((lat - glat) );
	    temp = (dc1_1 * fsamp + dc1_2 * fline);
	    fline = (dc2_1 * fsamp + dc2_2 * fline);
	    fsamp = temp;
	}
	else
	{
	    fsamp = ((lon - glong) / cdelt1 );
	    fline = ((lat - glat) / cdelt2 );
	    /* do the twist */
	    rtwist = - twist * dtr;       /* convert to radians */
	    temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
	    fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
	    fsamp = temp;
	}


	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH LinearProjection.RevProject fsamp = " + fsamp + 
	    "  fline = " + fline);
	*/
	x = fsamp + crpix1 - 1;
	y = fline + crpix2 - 1;

	ProjectionPt image_pt = new ProjectionPt(x, y);
	return (image_pt);
    }

    static public Pt FwdProject( double x, double y, ProjectionParams hdr)
    {
	double fsamp, fline;
	double          lat, lon;
	double          rtwist, temp;
	double xx, yy;

	double crpix1 = hdr.crpix1;
	double crpix2 = hdr.crpix2;
	double glong  = hdr.crval1;
	double glat   = hdr.crval2;
	double cdelt1 = hdr.cdelt1;
	double cdelt2 = hdr.cdelt2;
	double twist  = hdr.crota2;
	boolean using_cd = hdr.using_cd;
	double cd1_1 = hdr.cd1_1;
	double cd1_2 = hdr.cd1_2;
	double cd2_1 = hdr.cd2_1;
	double cd2_2 = hdr.cd2_2;

	/*
	if (SUTDebug.isDebug())
	{
	    System.out.println(
	    "RBH LinearProjection.FwdProject: input x = " + x + "  y = " + y);
	    //Thread.currentThread().dumpStack();
	}
	*/


	fsamp = x - crpix1 + 1;
	fline = y - crpix2 + 1;

	if (using_cd)
	{
	    lon = (cd1_1 * fsamp + cd1_2 * fline);
	    lat = (cd2_1 * fsamp + cd2_2 * fline);
	    lon += glong;
	    lat += glat;
	}
	else
	{
	    rtwist = - twist * dtr;       /* convert to radians */
	    temp = fsamp * Math.cos(rtwist) - fline * Math.sin(rtwist); /* do twist */
	    fline = fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
	    fsamp = temp;

	    lon = glong + fsamp * cdelt1;
	    lat = glat + fline * cdelt2;
	}


	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH LinearProjection.FwdProject: output lon = " + lon + 
	    "  lat = " + lat);
	*/
	Pt _pt = new Pt(lon, lat);
	return (_pt);
    }

}



