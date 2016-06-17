/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.conv;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

import java.util.ArrayList;

@JsExport
@JsType
public class CoordConv {
    public static final int EQUATORIAL_J = 0;
    public static final int EQUATORIAL_B = 1;
    public static final int GALACTIC = 2;
    public static final int ECLIPTIC_B = 3;
    public static final int SUPERGALACTIC = 4;
    public static final int ECLIPTIC_J = 13;


    /**
     * do the conversion when there is no proper motion
     * in_sys and out_sys should be on of the final int defined in this class.
     */
    public static LonLat doConv(int in_sys, double in_equinox,
                                double in_lon, double in_lat,
                                int out_sys, double out_equinox,
                                double tobs) {
        double out_lon = 0.0;
        double out_lat = 0.0;
        Jcnvc2.Jcnvc2Retval ret = Jcnvc2.jcnvc2(in_sys, in_equinox,
                in_lon, in_lat,
                out_sys, out_equinox,
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
                                   double in_pmlon, double in_pmlat) {
        double out_lon = 0.0;
        double out_lat = 0.0;
        double out_pmlon = 0.0;
        double out_pmlat = 0.0;
        double in_equinox = 1950.0;
        double out_equinox = 1950.0;
        double in_p = 0.0;
        double in_v = 0.0;
        int in_ieflag = 1; // remove the E-terms of aberration if any
        // -1 if do not want to remove the E-terms
        Gtjul2.RaDecPMRetval ret;

        // convert from arcsec to seconds(AR8211, 9/2008), per year to per century
        double pmLon = (in_pmlon * 100.0) / (15.0 * Math.cos(in_lat * Math.PI / 180.0));
        //System.out.println("in_lat: " +in_lat +" in_arcsec: " + in_pmlon + "  in_sec: " + pmLon);
        if (fromB1950ToJ2000) {  // from B1950 to J2000
            ret = Gtjul2.gtjulp(in_equinox, in_lon, in_lat,
                    pmLon, in_pmlat * 100.0,
                    in_p, in_v, in_ieflag,
                    out_lon, out_lat,
                    out_pmlon, out_pmlat);
        } else {// from J2000 to B1950
            ret = Gtjul2.unjulp(in_lon, in_lat,
                    pmLon, in_pmlat * 100.0,
                    in_p, in_v, in_ieflag,
                    out_equinox,
                    out_lon, out_lat, out_pmlon, out_pmlat);
        }
        // convert from seconds to arcsec(AR8211, 9/2008), per century to per year,

        double pmLonOut = (ret._pmra / 100.0) * 15.0 * Math.cos(ret._dec * Math.PI / 180.0);
        //System.out.println("out_arcsec: " + pmLonOut + "  out_sec: " + ret._pmra);
        //System.out.printf("lon: %f lat: %f lonPM: %f latPM %f ",  ret._ra, ret._dec, pmLonOut, ret._pmdec/100.0);

        RaDecPM raDecPM = new RaDecPM(ret._ra, ret._dec,
                pmLonOut, ret._pmdec / 100.0);
        return raDecPM;
    }


    static private double[][] calculateLonLats(int inCoord, int outCoord, double inEquinox,double outEquinox, double tobs ){

        double deltaLon=12.0;
        double deltaLat=18.0;
        double[] lons = new double[31];
        double[] lats = new double[11];

        for (int i=0; i<31; i++){
            lons[i] = deltaLon*i;
        }

        for (int i=0; i<11; i++){
            lats[i] = -90.0+deltaLat*i;
        }

        ArrayList<Double>  retLons = new ArrayList<Double>();
        ArrayList<Double>  retLats = new ArrayList<Double>();
        for (int i=0; i<lons.length; i++){
            for (int j=0; j<lats.length; j++){
                LonLat lonLat= doConv(inCoord,inEquinox,
                        lons[i], lats[j],
                        outCoord, outEquinox, tobs);
                retLons.add(lonLat.getLon());
                retLats.add(lonLat.getLat());
            }
        }

       double[] lonResult= new double[retLons.size()];
       double[] latResult= new double[retLons.size()];
       for ( int i=0; i<retLons.size(); i++){
           lonResult[i]=retLons.get(i).doubleValue();
           latResult[i]=retLats.get(i).doubleValue();
       }

        double[][] ret =new double[retLons.size()][2];
        ret[0]=lonResult;
        ret[1]=latResult;
        return ret;

    }

    //for fixed lat and different lons
    static private double[][] calculateLonLats(int inCoord, int outCoord, double inEquinox,double outEquinox, double lat, double tobs ){

        double deltaLon=12.0;

        double[] lons = new double[31];


        for (int i=0; i<31; i++){
            lons[i] = deltaLon*i;
        }

        double[]  retLons = new double[31];
        double[]  retLats = new double[31];
        for (int i=0; i<lons.length; i++){
                LonLat lonLat= doConv(inCoord,inEquinox,
                        lons[i], lat,
                        outCoord, outEquinox, tobs);
                retLons[i]=lonLat.getLon();
                retLats[i]=lonLat.getLat();
        }


        double[][] ret =new double[31][2];
        ret[0]=retLons;
        ret[1]= retLats;
        return ret;

    }
    public static void main(String[] args) {


        int out_sys = 0;

        double in_lon;
        double in_lat;
        double in_pmlon;
        double in_pmlat;
        //double out_lon = 0.0;
        //double out_lat = 0.0;
        double tobs;

        int in_sys = 1;
        double in_equinox = 1950.0;
        in_lon = 188.733333;
        in_lat = 12.34;
        in_pmlon = 1.0;
        in_pmlat = 0.0;
        tobs = 1983.5;

        out_sys = 0;
        double out_equinox = 2000.0;
        //RaDecPM ret = doConvPM(true, in_lon, in_lat,
        //in_pmlon, in_pmlat);
        LonLat ret = doConv(in_sys, in_equinox,
                in_lon, in_lat,
                out_sys, out_equinox, tobs);
        System.out.println("Conv: converted result  RA = " + ret.getLon() +
                "  Dec = " + ret.getLat());

        System.out.println("result should be near 189.364540d +12.065127d");

        boolean fromB1950ToJ2000=false;//true;
        RaDecPM rd=doConvPM( fromB1950ToJ2000 ,in_lon,  in_lat,in_pmlon, in_pmlat) ;

        double ra =  rd.getRa();
        double dec = rd.getDec();
        double raPM= rd.getRaPM() ;
        double decPM=rd.getDecPM();

        double[] result={ra, dec, raPM, decPM};
        System.out.println("result="+result);

 /*       tobs=0.0;
        ret = doConv(EQUATORIAL_J, 2000.0,
                124.534666, -35.955853,
                GALACTIC, 2000.0, tobs);
        System.out.println("galactic conversion of : " + ret);*/


        /*his block prepare the testing data to convert from each of the coordinate in the array to the others
          the eqinox for inCoord and outCorord is the same 2000
          each time, one array element is used as inCoord = EQUATORIAL_J and this element is commented in the array
         */
        int[] coordianteSys = {
                EQUATORIAL_J,
               // EQUATORIAL_B,
                GALACTIC,
                ECLIPTIC_B,
                SUPERGALACTIC,
                ECLIPTIC_B,
                 ECLIPTIC_J
        };

       //generate conversion data for in_equinox=out_equinox=2000
        double eqinox2000=2000.0;
        double eqinox1950=1950.0;
        double inLon =   188.733333;//124.534666;
        double inLat =   12.34;//-35.955853;

        double[] lons = new double[coordianteSys.length];
        double[] lats = new double[coordianteSys.length];

        int inCoord=EQUATORIAL_B;
        for (int i=0; i<coordianteSys.length; i++){
            LonLat lonLat= doConv(inCoord,eqinox1950,
                        inLon, inLat,
                       coordianteSys[i], eqinox2000, tobs);
            lons[i]=lonLat.getLon();
            lats[i]=lonLat.getLat();
        }


        double[][] mResult = calculateLonLats(EQUATORIAL_J,ECLIPTIC_B, eqinox2000, eqinox1950, -90.0, tobs  );

        double[] newLons = mResult[0];
        double[] newLats = mResult[1];
        System.out.println("done");
    }


}


