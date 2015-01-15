/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.ProjectionException;


public class GnomonicProjection{


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;

    static public ProjectionPt RevProject (double ra, double dec,
	ProjectionParams hdr, boolean useProjException) throws ProjectionException
    {
	int i, j;
	double fsamp_correction, fline_correction;
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

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH GnomonicProjection.RevProject input ra = " + ra + 
	    "  dec = " + dec);
	*/

	lon = ra * dtr;
	lat = dec * dtr;

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH GnomonicProjection.RevProject radians lon = " + lon + 
	    "  lat = " + lat + "  dtr = " + dtr);
	*/

	rpp1 = -cdelt1 * dtr;
	rpp2 = -cdelt2 * dtr;

	lon0 = glong * dtr;
	lat0 = glat * dtr;

	aa = Math.cos(lat) * Math.cos(lon - lon0);
	ff1 = 1./ (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));
	ff2 = 1./ (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));

	if (ff1 < 0)
	{
	    /* we're more than 90 degrees from projection center */
        if (useProjException) throw new ProjectionException("coordinates not on image");
        else return null;
	}
	else
	{
	    fline = -ff2 * (Math.cos(lat0) * Math.sin(lat) - aa * Math.sin(lat0));
	    fsamp = -ff1 * Math.cos(lat) * Math.sin(lon - lon0);

	    if (using_cd)
	    {
		temp = -(dc1_1 * fsamp + dc1_2 * fline) * rtd;
		fline = -(dc2_1 * fsamp + dc2_2 * fline) * rtd;
		fsamp = temp;
	    }
	    else
	    {
		/* do the twist */
		rtwist = twist * dtr;       /* convert to radians */
		temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
		fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
		fsamp = temp;

		fsamp = (fsamp / rpp1);     /* now apply cdelt */
		fline = (fline / rpp2);
	    }


	    if (hdr.map_distortion)
	    {
		/* apply SIRTF distortion corrections */
		fsamp_correction = 0.0;
		for (i = 0; i <= hdr.ap_order; i++)
		{
		    for (j = 0; j <= hdr.ap_order; j++)
		    {
			if (i + j <= hdr.ap_order)
			{
			    fsamp_correction += 
				hdr.ap[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
			}
		    }
		}

		/*
		if (SUTDebug.isDebug())
		System.out.println("deproject: fsamp correction = " + fsamp_correction);
		*/

		fline_correction = 0.0;
		for (i = 0; i <= hdr.bp_order; i++)
		{
		    for (j = 0; j <= hdr.bp_order; j++)
		    {
			if (i + j <= hdr.bp_order)
			{
			    fline_correction += 
				hdr.bp[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
			}
		    }
		}
		fsamp += fsamp_correction;
		fline += fline_correction;
		/*
		if (SUTDebug.isDebug())
		System.out.println("deproject: fline correction = " + fline_correction);
		*/

	    }
	}


	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH GnomonicProjection.RevProject fsamp = " + fsamp + 
	    "  fline = " + fline);
	*/
	x = fsamp + crpix1 - 1;
	y = fline + crpix2 - 1;

	ProjectionPt image_pt = new ProjectionPt(x, y);
	return (image_pt);
    }

    static public Pt FwdProject( double x, double y, ProjectionParams hdr)
    {
	int i, j;
	double fsamp_correction, fline_correction;
	double fsamp, fline;
	double          lat, lon;
	double          rpp1, rpp2, glongr, glatr ;
	double          rtwist, temp;
	double delta, beta;
	double xxx, yyy, xx, yy;

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

	/*
	if (SUTDebug.isDebug())
	{
	    System.out.println(
	    "RBH GnomonicProjection.FwdProject: getProjectionName() returns "
	    + hdr.getProjectionName());
	    System.out.println(
	    "RBH GnomonicProjection.FwdProject: input x = " + x + "  y = " + y +
	    "   fsamp = " + fsamp + "  fline = " + fline);
	    //Thread.currentThread().dumpStack();
	}
	*/

	if (hdr.map_distortion)
	{
	    /* apply SIRTF distortion corrections */
	    fsamp_correction = 0.0;
	    for (i = 0; i <= hdr.a_order; i++)
	    {
		for (j = 0; j <= hdr.a_order; j++)
		{
		    if (i + j <= hdr.a_order)
		    {
			fsamp_correction += 
			    hdr.a[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
		    }
		}
	    }

	    /*
	    if (SUTDebug.isDebug())
	    System.out.println("dimage_to_sky: fsamp correction = " + fsamp_correction);
	    */

	    fline_correction = 0.0;
	    for (i = 0; i <= hdr.b_order; i++)
	    {
		for (j = 0; j <= hdr.b_order; j++)
		{
		    if (i + j <= hdr.b_order)
		    {
			fline_correction += 
			    hdr.b[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
		    }
		}
	    }
	    fsamp += fsamp_correction;
	    fline += fline_correction;

	    /*
	    if (SUTDebug.isDebug())
	    System.out.println("dimage_to_sky: fline correction = " + fline_correction);
	    */

	}


	if (using_cd)
	{
	    xx = -(cd1_1 * fsamp + cd1_2 * fline) * dtr;
	    yy = -(cd2_1 * fsamp + cd2_2 * fline) * dtr;
	}
	else
	{
	    rpp1 = cdelt1 * dtr;        /* radians per pixel */
	    rpp2 = cdelt2 * dtr;        /* radians per pixel */
	    xx = -fsamp * rpp1;
	    yy = -fline * rpp2;

	    rtwist = twist * dtr;       /* convert to radians */
	    temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
	    yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
	    xx = temp;
	}

	delta = Math.atan(Math.sqrt(xx * xx + yy * yy));

	if ((xx == 0.0) && (yy == 0.0))
	    yy = 1.0;  /* avoid domain error in atan2 */
	beta = Math.atan2(-xx, yy);
	glatr = glat * dtr;
	glongr = glong * dtr;
	lat = Math.asin(-Math.sin(delta) * Math.cos(beta) * Math.cos(glatr) +
	    Math.cos(delta) * Math.sin(glatr));
	xxx = Math.sin(glatr) * Math.sin(delta) * Math.cos(beta) +
	    Math.cos(glatr) * Math.cos(delta);
	yyy = Math.sin(delta) * Math.sin(beta);
	lon = glongr + Math.atan2(yyy, xxx);

	lat = lat * rtd;
	lon = lon * rtd;

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH GnomonicProjection.FwdProject: output lon = " + lon + 
	    "  lat = " + lat);
	*/
	Pt _pt = new Pt(lon, lat);
	return (_pt);
    }

}



