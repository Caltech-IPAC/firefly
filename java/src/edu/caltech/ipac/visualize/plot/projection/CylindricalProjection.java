/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.ProjectionException;

public class CylindricalProjection{


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;
    private static double  WCSTRIG_TOL = 1e-10;
    private static double celref[] = new double[4];
    private static double euler[] = new double[5];

    static public ProjectionPt RevProject (double lon, double lat,
	ProjectionParams hdr, boolean useProjException) throws ProjectionException
    {
	double          fline, fsamp, rtwist, temp, rlat;
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
	double xx, yy;
	double result[];

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH CylindricalProjection.RevProject input lon = " + lon + 
	    "  lat = " + lat);
	*/

	/* Initialize projection parameters. */
	/* Set reference angles for the native grid. */
	celref[0] =   glong;
	celref[1] =   glat;
	celref[2] =   999.0;
	celref[3] =   999.0;

    boolean celsetSuccess= celset(useProjException);
    if (!celsetSuccess && !useProjException)  return null;

	result = sphfwd(lon, lat);
	xx = result[0];
	//yy = result[1];
	
	yy = Math.sin(lat * dtr) * 180.0 / Math.PI;

	if (using_cd)
	{
	    fsamp = dc1_1 * xx + dc1_2 * yy;
	    fline = dc2_1 * xx + dc2_2 * yy;
	}
	else
	{
	    fsamp = xx / cdelt1;
	    fline = yy / cdelt2;
	}


	/*
	rlat = lat * dtr;
	fsamp = ((lon - glong) / cdelt1) * Math.cos(rlat);
	fline = ((lat - glat) / cdelt2);
	*/


	    /* do the twist */
	    rtwist = - twist * dtr;       /* convert to radians */
	    temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
	    fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
	    fsamp = temp;


	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH CylindricalProjection.RevProject fsamp = " + fsamp + 
	    "  fline = " + fline);
	*/
	x = fsamp + crpix1 - 1;
	y = fline + crpix2 - 1;

	ProjectionPt image_pt = new ProjectionPt(x, y);
	return (image_pt);
    }

    static public Pt FwdProject( double x, double y, ProjectionParams hdr, boolean useProjException)
            throws ProjectionException
    {
	double fsamp, fline;
	double          lat, lon;
	double          rtwist, temp, rlat;
	double xx, yy;
	double result[];

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
	    "RBH CylindricalProjection.FwdProject: input x = " + x + "  y = " + y);
	    //Thread.currentThread().dumpStack();
	}
	*/
	fsamp = x - crpix1 + 1;
	fline = y - crpix2 + 1;


	rtwist = - twist * dtr;       /* convert to radians */
	temp = fsamp * Math.cos(rtwist) - fline * Math.sin(rtwist); /* do twist */
	fline = fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
	fsamp = temp;

	if (using_cd)
	{
	    xx = (cd1_1 * fsamp + cd1_2 * fline);
	    yy = (cd2_1 * fsamp + cd2_2 * fline);
	}
	else
	{
	    xx = fsamp * cdelt1;
	    yy = fline * cdelt2;
	}
	/* Initialize projection parameters. */
	/* Set reference angles for the native grid. */
	celref[0] =   glong;
	celref[1] =   glat;
	celref[2] =   999.0;
	celref[3] =   999.0;

    boolean celsetSuccess= celset(useProjException);
    if (!celsetSuccess && !useProjException)  return null;

	yy = Math.asin(yy * dtr) * rtd;

	result = sphrev(xx, yy);
	lon = result[0];
	lat = result[1];

	if (lon < 0.0)
	{
	    lon += 360.0;
	}

	/*
	if (SUTDebug.isDebug())
	    System.out.println(
	    "RBH CylindricalProjection.FwdProject: output lon = " + lon + 
	    "  lat = " + lat);
	*/
	Pt _pt = new Pt(lon, lat);
	return (_pt);
    }


   private static boolean celset(boolean useProjException)  throws ProjectionException
{
   double tol = 1.0e-10;
   double clat0, cphip, cthe0, slat0, sphip, sthe0;
   double latp, latp1, latp2;
   double u, v, x, y, z;



   /* Compute celestial coordinates of the native pole. */

      /* Reference point away from the native pole. */

      /* Set default for longitude of the celestial pole. */
     if (celref[1] < 0.0)
	celref[2] = 180.0;
     else
	celref[2] = 0.0;
	


      clat0 = cosd(celref[1]);
      slat0 = sind(celref[1]);
      cphip = cosd(celref[2]);
      sphip = sind(celref[2]);
      cthe0 = 1.0;
      sthe0 = 0.0;

      x = cthe0*cphip;
      y = sthe0;
      z = Math.sqrt(x*x + y*y);
      if (z == 0.0) {
          if (slat0 != 0.0) {
              if (useProjException) throw (new ProjectionException("failure in CAR projection"));
              else return false;
          }

         /* latp determined by LATPOLE in this case. */
         latp = celref[3];
      } else {
          if (Math.abs(slat0/z) > 1.0) {
              if (useProjException) throw (new ProjectionException("failure in CAR projection"));
              else return false;
          }

         u = atan2d(y,x);
         v = acosd(slat0/z);

         latp1 = u + v;
         if (latp1 > 180.0) {
            latp1 -= 360.0;
         } else if (latp1 < -180.0) {
            latp1 += 360.0;
         }

         latp2 = u - v;
         if (latp2 > 180.0) {
            latp2 -= 360.0;
         } else if (latp2 < -180.0) {
            latp2 += 360.0;
         }

         if (Math.abs(celref[3]-latp1) < Math.abs(celref[3]-latp2)) {
            if (Math.abs(latp1) < 90.0+tol) {
               latp = latp1;
            } else {
               latp = latp2;
            }
         } else {
            if (Math.abs(latp2) < 90.0+tol) {
               latp = latp2;
            } else {
               latp = latp1;
            }
         }

         celref[3] = latp;
      }

      euler[1] = 90.0 - latp;

      z = cosd(latp)*clat0;
      if (Math.abs(z) < tol) {
         if (Math.abs(clat0) < tol) {
            /* Celestial pole at the reference point. */
            euler[0] = celref[0];
            euler[1] = 90.0;
         } else if (latp > 0.0) {
            /* Celestial pole at the native north pole.*/
            euler[0] = celref[0] + celref[2] - 180.0;
            euler[1] = 0.0;
         } else if (latp < 0.0) {
            /* Celestial pole at the native south pole. */
            euler[0] = celref[0] - celref[2];
            euler[1] = 180.0;
         }
      } else {
         x = (sthe0 - sind(latp)*slat0)/z;
         y =  sphip*cthe0/clat0;
          if (x == 0.0 && y == 0.0) {
              if (useProjException) throw (new ProjectionException("failure in CAR projection"));
              else return false;
          }
         euler[0] = celref[0] - atan2d(y,x);
      }

   euler[2] = celref[2];
   euler[3] = cosd(euler[1]);
   euler[4] = sind(euler[1]);

   /* Check for ill-conditioned parameters. */
    if (Math.abs(latp) > 90.0+tol) {
        if (useProjException) throw new ProjectionException("ill-conditioned parameters in CAR projection");
        else return false;
    }
    return true;
}



private static double[]  sphfwd (double lng, double lat)
{
    double tol = 1.0e-5;
    double phi, theta;
   double result[] = new double[2];
   double coslat, coslng, dlng, dphi, sinlat, sinlng, x, y, z;

   coslat = cosd(lat);
   sinlat = sind(lat);

   dlng = lng - euler[0];
   coslng = cosd(dlng);
   sinlng = sind(dlng);

   /* Compute native coordinates. */
   x = sinlat*euler[4] - coslat*euler[3]*coslng;
   if (Math.abs(x) < tol) {
      /* Rearrange formula to reduce roundoff errors. */
      x = -cosd(lat+euler[1]) + coslat*euler[3]*(1.0 - coslng);
   }
   y = -coslat*sinlng;
   if (x != 0.0 || y != 0.0) {
      dphi = atan2d(y, x);
   } else {
      /* Change of origin of longitude. */
      dphi = dlng - 180.0;
   }
   phi = euler[2] + dphi;

   /* Normalize. */
   if (phi > 180.0) {
      phi -= 360.0;
   } else if (phi < -180.0) {
      phi += 360.0;
   }

   if (dlng % 180.0 == 0.0) {
      theta = lat + coslng*euler[1];
      if (theta >  90.0) theta =  180.0 - theta;
      if (theta < -90.0) theta = -180.0 - theta;
   } else {
      z = sinlat*euler[3] + coslat*euler[4]*coslng;
      if (Math.abs(z) > 0.99) {
         /* Use an alternative formula for greater numerical accuracy. */
         theta = acosd(Math.sqrt(x*x+y*y));
	 if (z < 0.0)
	    theta = -Math.abs(theta);
	 else
	    theta = Math.abs(theta);
      } else {
         theta = asind(z);
      }
   }

   result[0] = phi;
   result[1] = theta;
   return(result);
}


private static double[]  sphrev (double phi, double theta)
{
    double tol = 1.0e-5;
    double lng, lat;
   double retval[] = new double[2];
   double cosphi, costhe, dlng, dphi, sinphi, sinthe, x, y, z;

   costhe = cosd(theta);
   sinthe = sind(theta);

   dphi = phi - euler[2];
   cosphi = cosd(dphi);
   sinphi = sind(dphi);

   /* Compute celestial coordinates. */
   x = sinthe*euler[4] - costhe*euler[3]*cosphi;
   if (Math.abs(x) < tol) {
      /* Rearrange formula to reduce roundoff errors. */
      x = -cosd(theta+euler[1]) + costhe*euler[3]*(1.0 - cosphi);
   }
   y = -costhe*sinphi;
   if (x != 0.0 || y != 0.0) {
      dlng = atan2d(y, x);
   } else {
      /* Change of origin of longitude. */
      dlng = dphi + 180.0;
   }
   lng = euler[0] + dlng;

   /* Normalize the celestial longitude. */
   if (euler[0] >= 0.0) {
      if (lng < 0.0) lng += 360.0;
   } else {
      if (lng > 0.0) lng -= 360.0;
   }
   
   /* Normalize. */
   if (lng > 360.0) {
      lng -= 360.0;
   } else if (lng < -360.0) {
      lng += 360.0;
   }

   if (dphi % 180.0 == 0.0) {
      lat = theta + cosphi*euler[1];
      if (lat >  90.0) lat =  180.0 - lat;
      if (lat < -90.0) lat = -180.0 - lat;
   } else {
      z = sinthe*euler[3] + costhe*euler[4]*cosphi;
      if (Math.abs(z) > 0.99) {
         /* Use an alternative formula for greater numerical accuracy. */
         lat = acosd(Math.sqrt(x*x+y*y));
	 if (z < 0.0)
	    lat = -Math.abs(lat);
	 else
	    lat = Math.abs(lat);
      } else {
         lat = asind(z);
      }
   }

   retval[0] = lng;
   retval[1] = lat;
   return(retval);
}


private static double cosd(double angle)
{
   return Math.cos(angle*dtr);
}


private static double sind(double angle)
{
   return Math.sin(angle*dtr);
}


private static double tand(double angle)
{
   return Math.tan(angle*dtr);
}


private static double acosd(double v)
{
   if (v >= 1.0) {
      if (v-1.0 <  WCSTRIG_TOL) return 0.0;
   } else if (v == 0.0) {
      return 90.0;
   } else if (v <= -1.0) {
      if (v+1.0 > -WCSTRIG_TOL) return 180.0;
   }

   return Math.acos(v)*rtd;
}


private static double asind(double v)
{
   if (v <= -1.0) {
      if (v+1.0 > -WCSTRIG_TOL) return -90.0;
   } else if (v == 0.0) {
      return 0.0;
   } else if (v >= 1.0) {
      if (v-1.0 <  WCSTRIG_TOL) return 90.0;
   }

   return Math.asin(v)*rtd;
}


private static double atand(double v)
{
   if (v == -1.0) {
      return -45.0;
   } else if (v == 0.0) {
      return 0.0;
   } else if (v == 1.0) {
      return 45.0;
   }

   return Math.atan(v)*rtd;
}


private static double atan2d(double y, double x)
{
   if (y == 0.0) {
      if (x >= 0.0) {
         return 0.0;
      } else if (x < 0.0) {
         return 180.0;
      }
   } else if (x == 0.0) {
      if (y > 0.0) {
         return 90.0;
      } else if (y < 0.0) {
         return -90.0;
      }
   }

   return Math.atan2(y,x)*rtd;
}

}



