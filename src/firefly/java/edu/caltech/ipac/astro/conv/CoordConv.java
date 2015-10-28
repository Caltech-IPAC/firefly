/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

@JsExport
@JsType
public class CoordConv {
   public static final int EQUATORIAL_J = 0;
   public static final int EQUATORIAL_B = 1;
   public static final int GALACTIC     = 2;
   public static final int ECLIPTIC_B   = 3;
   public static final int SUPERGALACTIC   = 4;
   public static final int ECLIPTIC_J   = 13;


   /**
    * do the conversion when there is no proper motion
    * in_sys and out_sys should be on of the final int defined in this class.
    */
   public static LonLat doConv(int in_sys, double in_equinox, 
				double in_lon, double in_lat,
				int out_sys, double out_equinox,
				double tobs)  {
	  double out_lon = 0.0;
	  double out_lat = 0.0;
          Jcnvc2.Jcnvc2Retval ret = Jcnvc2.jcnvc2(in_sys, in_equinox,
	                               in_lon, in_lat,
                                       out_sys,  out_equinox,
	                               out_lon, out_lat,
                        	       tobs);
	  LonLat lonLat = new LonLat(ret._xnew, ret._ynew);
	  return lonLat;
       }


   /**
    * only handles the Proper Motion conversion from Equatorial B1950 to 
    * J2000, or vice versa.
    * proper motion unit: arcsec/year
    */
   public static RaDecPM doConvPM(boolean fromB1950ToJ2000, 
				   double in_lon, double in_lat,
				   double in_pmlon, double in_pmlat)  {
	  double out_lon = 0.0;
	  double out_lat = 0.0;
	  double out_pmlon = 0.0;
	  double out_pmlat = 0.0;
          double in_equinox = 1950.0;
          double out_equinox = 1950.0;
          double in_p = 0.0;
          double in_v = 0.0;
          int    in_ieflag = 1; // remove the E-terms of aberration if any 
                                // -1 if do not want to remove the E-terms
          Gtjul2.RaDecPMRetval ret;

       // convert from arcsec to seconds(AR8211, 9/2008), per year to per century
       double pmLon =    (in_pmlon*100.0)/(15.0*Math.cos(in_lat*Math.PI/180.0));
       //System.out.println("in_lat: " +in_lat +" in_arcsec: " + in_pmlon + "  in_sec: " + pmLon);
          if (fromB1950ToJ2000) {  // from B1950 to J2000
             ret = Gtjul2.gtjulp(in_equinox, in_lon, in_lat,
                                       pmLon, in_pmlat*100.0,
                                       in_p, in_v, in_ieflag,
	                               out_lon, out_lat,
	                               out_pmlon, out_pmlat);
          }
      else {// from J2000 to B1950
	     ret = Gtjul2.unjulp(in_lon, in_lat,
                             pmLon, in_pmlat*100.0,
	                         in_p, in_v, in_ieflag,
                                 out_equinox,
                                 out_lon, out_lat, out_pmlon, out_pmlat);
          }
       // convert from seconds to arcsec(AR8211, 9/2008), per century to per year, 

       double pmLonOut = (ret._pmra/100.0)*15.0*Math.cos(ret._dec*Math.PI/180.0);
       //System.out.println("out_arcsec: " + pmLonOut + "  out_sec: " + ret._pmra);
       //System.out.printf("lon: %f lat: %f lonPM: %f latPM %f ",  ret._ra, ret._dec, pmLonOut, ret._pmdec/100.0);

          RaDecPM raDecPM = new RaDecPM(ret._ra, ret._dec,
	                                pmLonOut, ret._pmdec/100.0);
       return raDecPM;
       }


   public static void main(String[] args) {
        int    in_sys;
	int    out_sys = 0;
	double in_equinox = 1950.0;
	double out_equinox = 2000;
	double in_lon;
	double in_lat;
	double in_pmlon;
	double in_pmlat;
	//double out_lon = 0.0;
	//double out_lat = 0.0;
	double tobs ;

       in_sys = 1;
       in_equinox = 1950.0;
       in_lon = 188.733333;
       in_lat = 12.34;
       in_pmlon= 1.0;
       in_pmlat= 0.0;
       tobs = 1983.5;

       out_sys = 0;
       out_equinox = 2000.0;
       //RaDecPM ret = doConvPM(true, in_lon, in_lat,
	                                //in_pmlon, in_pmlat);
       LonLat ret = doConv(in_sys,  in_equinox,  
		           in_lon, in_lat, 
		           out_sys,  out_equinox, tobs);
       System.out.println("Conv: converted result  RA = " + ret.getLon() +
       "  Dec = " + ret.getLat());
       /*
       System.out.println("           PMRA = " + ret.getRaPM() +
		    "  PMDec = " + ret.getDecPM());

*/
       System.out.println("result should be near 189.364540d +12.065127d");


       ret = doConv(EQUATORIAL_J ,  2000.0,
                           124.534666,-35.955853,
                           GALACTIC,  2000.0, 0.0);
       System.out.println("galactic conversion of : "+ ret);





       }
}


