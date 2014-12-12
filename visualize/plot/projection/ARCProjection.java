package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;


public class ARCProjection{


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;

    static public ProjectionPt RevProject (double ra, double dec, ProjectionParams hdr)  {
	int i, j;
	double correction;
	double          lat, lon;
	double          rpp1, rpp2, lat0, lon0;
	double          aa, ff1, ff2, rho;
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

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH ARCProjection.RevProject input ra = " + ra +
//	    "  dec = " + dec);

	lon = ra * dtr;
	lat = dec * dtr;

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH ARCProjection.RevProject radians lon = " + lon + 
	    "  lat = " + lat + "  dtr = " + dtr);
	*/

	rpp1 = -cdelt1 * dtr;
	rpp2 = -cdelt2 * dtr;

	lon0 = glong * dtr;
	lat0 = glat * dtr;

	fsamp = Math.sin(lon-lon0) * Math.cos (lat);
        fline = Math.sin (lat) * Math.sin(lat0) 
	    + Math.cos (lat) * Math.cos(lat0) * Math.cos(lon-lon0);
        if (fline <-1.0) fline = -1.0;
        if (fline >1.0) fline = 1.0;
        fline = Math.acos (fline);
        if (fline!=0)
            fline = fline / Math.sin(fline);
        else
            fline = 1.0;
        fsamp = fsamp * fline;
        fline = (Math.sin (lat) * Math.cos(lat0) 
	    - Math.cos (lat) * Math.sin(lat0) * Math.cos(lon-lon0)) * fline;

        fsamp = - fsamp;
        fline = - fline;

	/* do the twist */
	rtwist = twist * dtr;       /* convert to radians */
	temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
	fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
	fsamp = temp;

	fsamp = (fsamp / rpp1);     /* now apply cdelt */
	fline = (fline / rpp2);

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH ARCProjection.RevProject fsamp = " + fsamp +
//	    "  fline = " + fline);
	x = fsamp + crpix1 - 1;
	y = fline + crpix2 - 1;

	ProjectionPt image_pt = new ProjectionPt(x, y);
	return (image_pt);
    }

    static public Pt FwdProject( double x, double y, ProjectionParams hdr)
    {
	int i, j;
	double correction;
	double fsamp, fline;
	double          lat, lon;
	double          rpp1, rpp2, glongr, glatr ;
	double          rtwist, temp;
	double delta, beta;
	double xxx, yyy, xx, yy;
	double sins, coss, dt, da;

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

	fsamp = x - crpix1 + 1;
	fline = y - crpix2 + 1;

//	if (SUTDebug.isDebug())
//	{
//	    System.out.println(
//	    "RBH ARCProjection.FwdProject: getProjectionName() returns "
//	    + hdr.getProjectionName());
//	    System.out.println(
//	    "RBH ARCProjection.FwdProject: input x = " + x + "  y = " + y +
//	    "   fsamp = " + fsamp + "  fline = " + fline);
//	    //Thread.currentThread().dumpStack();
//	}

	    rpp1 = cdelt1 * dtr;        /* radians per pixel */
	    rpp2 = cdelt2 * dtr;        /* radians per pixel */
	    xx = -fsamp * rpp1;
	    yy = -fline * rpp2;

	    rtwist = twist * dtr;       /* convert to radians */
	    temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
	    yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
	    xx = temp;


	glatr = glat * dtr;
	glongr = glong * dtr;

	sins = xx * xx + yy * yy;

	sins = Math.sqrt(sins);
	coss = Math.cos (sins);
	if (sins!=0.0) 
	    sins = Math.sin (sins) / sins;
	else
	    sins = 1.0;
	dt = - yy * Math.cos(glatr) * sins + Math.sin(glatr) * coss;
	lat = Math.asin (dt);
	da = coss - dt * Math.sin(glatr);
	dt = - xx * sins * Math.cos(glatr);
	lon = glongr + Math.atan2 (dt, da);

	lat = lat * rtd;
	lon = lon * rtd;

//	if (SUTDebug.isDebug())
//	    System.out.println(
//	    "RBH ARCProjection.FwdProject: output lon = " + lon +
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
