package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.ProjectionException;


public class AitoffProjection{


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;

    static public ProjectionPt RevProject (double ra, double dec, ProjectionParams hdr)
    {
	double          lat, lon;
	double rho, theta;
	double asin_arg;
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

	lon = ra * dtr;
	lat = dec * dtr;

	rpp1 = cdelt1 * dtr;
	rpp2 = cdelt2 * dtr;

	lon0 = glong * dtr;
	lat0 = glat * dtr;


	/* get delta-lon in range -180 to +180 */
	if ((lon - lon0) > Math.PI)
	    lon -= 2 * Math.PI;
	if ((lon - lon0) < -Math.PI)
	    lon += 2 * Math.PI;
	
	rho = Math.acos(Math.cos(lat) * Math.cos((lon - lon0) / 2.));
	if ((rho < 0.0001) && (rho > -0.0001))
	{
	    theta = 0;
	}
	else
	{
	    asin_arg = Math.cos(lat) * Math.sin((lon - lon0) / 2.) / Math.sin(rho);
	    if (asin_arg > 1.0)
		asin_arg = 1.0;
	    if (asin_arg < -1.0)
		asin_arg = -1.0;
	    theta = Math.asin(asin_arg);
	}

	fsamp = 4./ rpp1 * Math.sin(rho / 2.) * Math.sin(theta);
	fline = 2./ rpp2 * Math.sin(rho / 2.) * Math.cos(theta);

	if (lat < 0.)
	    fline = -fline;


	x = fsamp + crpix1 - 1;
	y = fline + crpix2 - 1;

	ProjectionPt image_pt = new ProjectionPt(x, y);
	return (image_pt);
    }

    static public Pt FwdProject( double x, double y, ProjectionParams hdr, boolean useProjException)
	throws ProjectionException
    {
	double rad;
	double asin_arg;
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
//	    "RBH AitoffProjection.FwdProject: getProjectionName() returns "
//	    + hdr.getProjectionName());
//	    System.out.println(
//	    "RBH AitoffProjection.FwdProject: input x = " + x + "  y = " + y);
//	    //Thread.currentThread().dumpStack();
//	}
	fsamp = x - crpix1 + 1;
	fline = y - crpix2 + 1;

	rpp1 = cdelt1 * dtr;        /* radians per pixel */
	rpp2 = cdelt2 * dtr;        /* radians per pixel */
	xx = (fsamp / 2.) * rpp1;
	yy = (fline / 2.) * rpp2;

	if (((4.- xx * xx - 4.* yy * yy) < 2) || (glat != 0.0))
	{
	    if (useProjException) throw new ProjectionException("undefined location");
        else                  return null;
	}

	temp = Math.sqrt(4.- xx * xx - 4.* yy * yy);

	lat = Math.asin(temp * yy);
	asin_arg = (temp * xx / (2.* Math.cos(lat)));
	if (asin_arg > 1.0)
	    asin_arg = 1.0;
	if (asin_arg < -1.0)
	    asin_arg = -1.0;
	lon = 2 * Math.asin(asin_arg);

	lat = lat * rtd;
	lon = glong + lon * rtd;
	if (lon < 0.0)
	{
	    lon += 360.0;
	}


//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH AitoffProjection.FwdProject: output lon = " + lon +
//	    "  lat = " + lat);
	Pt _pt = new Pt(lon, lat);
	return (_pt);
    }

}



/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
