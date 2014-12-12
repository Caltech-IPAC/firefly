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
