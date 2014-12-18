package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.ProjectionException;


public class NCPProjection{


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;

    static public ProjectionPt RevProject (double ra, double dec,
	ProjectionParams hdr)
    {
	double          lat, lon;
	double          rpp1, rpp2, lat0, lon0;
	double          fline, fsamp, rtwist, temp;
	double          x, y;

	double crpix1 = hdr.crpix1;
	double crpix2 = hdr.crpix2;
	double glong  = hdr.crval1;
	double glat   = hdr.crval2;
	double cdelt1 = hdr.cdelt1;
	double cdelt2 = hdr.cdelt2;
	double twist  = hdr.crota2;

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH NCPProjection.RevProject input ra = " + ra +
//	    "  dec = " + dec);

	lon = ra * dtr;
	lat = dec * dtr;

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH NCPProjection.RevProject radians lon = " + lon +
//	    "  lat = " + lat + "  dtr = " + dtr);

	rpp1 = -cdelt1 * dtr;
	rpp2 = -cdelt2 * dtr;

	lon0 = glong * dtr;
	lat0 = glat * dtr;

	fsamp = -Math.cos(lat) * Math.sin(lon - lon0);
	fline =
    (-Math.cos(lat0) + Math.cos(lat) * Math.cos(lon - lon0)) / Math.sin(lat0);


	    /* do the twist */
	    rtwist = twist * dtr;       /* convert to radians */
	    temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
	    fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
	    fsamp = temp;

	    fsamp = (fsamp / rpp1);     /* now apply cdelt */
	    fline = (fline / rpp2);

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH NCPProjection.RevProject fsamp = " + fsamp +
//	    "  fline = " + fline);
	x = fsamp + crpix1 - 1;
	y = fline + crpix2 - 1;

	ProjectionPt image_pt = new ProjectionPt(x, y);
	return (image_pt);
    }

    static public Pt FwdProject( double x, double y, ProjectionParams hdr)
	throws ProjectionException
    {
	double rad;
	double fsamp, fline;
	double          lat, lon;
	double          rpp1, rpp2, glongr, glatr ;
	double          rtwist, temp;
	double xx, yy;

	double crpix1 = hdr.crpix1;
	double crpix2 = hdr.crpix2;
	double glong  = hdr.crval1;
	double glat   = hdr.crval2;
	double cdelt1 = hdr.cdelt1;
	double cdelt2 = hdr.cdelt2;
	double twist  = hdr.crota2;

//	if (SUTDebug.isDebug())
//	{
//	    System.out.println(
//	    "RBH NCPProjection.FwdProject: getProjectionName() returns "
//	    + hdr.getProjectionName());
//	    System.out.println(
//	    "RBH NCPProjection.FwdProject: input x = " + x + "  y = " + y);
//	    //Thread.currentThread().dumpStack();
//	}
	fsamp = x - crpix1 + 1;
	fline = y - crpix2 + 1;

	rpp1 = cdelt1 * dtr;        /* radians per pixel */
	rpp2 = cdelt2 * dtr;        /* radians per pixel */
	xx = fsamp * rpp1;
	yy = fline * rpp2;

	rtwist = twist * dtr;       /* convert to radians */
	temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
	yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
	xx = temp;


	glatr = glat * dtr;
	glongr = glong * dtr;

	lon = glongr + Math.atan(xx / (Math.cos(glatr) - yy * Math.sin(glatr)));
	lat = Math.abs( Math.acos((Math.cos(glatr) - yy * Math.sin(glatr)) /
	    Math.cos(lon - glongr)));
	if (glatr < 0)
	    lat = -lat;


	lat = lat * rtd;
	lon = lon * rtd;

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH NCPProjection.FwdProject: output lon = " + lon +
//	    "  lat = " + lat);
	Pt _pt = new Pt(lon, lat);
	return (_pt);
    }

}



