/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;


/**
 * Date: Feb 22, 2007
 *
 * @author Trey
 * @version $Id: MathUtil.java,v 1.4 2008/11/17 23:37:53 roby Exp $
 */
public class MathUtil {

    public final static double    ARCMIN_TO_DEG = .01666666667;
    public final static double    ARCSEC_TO_DEG = .00027777778;
    public final static double    DEG_TO_ARCMIN = 60.0;
    public final static double    DEG_TO_ARCSEC = 3600.0;
    public final static double    ARCMIN_TO_ARCSEC = 60.0;
    public final static double    ARCSEC_TO_ARCMIN = .01666666667;

    public enum Units {DEGREE, ARCMIN, ARCSEC;
        public static Units parse(String s, Units defValue) {
            if (s == null) {return defValue;}
            String lc = s.toLowerCase();
            if (lc.startsWith("deg")) {return DEGREE;}
            else if (lc.startsWith("arcsec")) {return ARCSEC;}
            else if (lc.startsWith("arcmin")) {return ARCMIN;}
            else {return defValue;}
        }
    }

   public static native double atan2(double x, double y) /*-{
            var retval=  Math.atan2(x,y);
            return retval;
   }-*/;


    public static double convert(Units fromUnits, Units toUnits, double v) {
        double retval;
        if (fromUnits==toUnits) {
            retval= v;
        }
        else if (Double.isNaN(v)) {
            retval= Double.NaN;
        }
        else if (fromUnits==Units.DEGREE && toUnits==Units.ARCMIN) {
            retval= v*DEG_TO_ARCMIN;
        }
        else if (fromUnits==Units.DEGREE && toUnits==Units.ARCSEC) {
            retval= v*DEG_TO_ARCSEC;
        }

        else if (fromUnits==Units.ARCSEC && toUnits==Units.DEGREE) {
            retval= v*ARCSEC_TO_DEG;
        }

        else if (fromUnits==Units.ARCSEC && toUnits==Units.ARCMIN) {
            retval= v*ARCSEC_TO_ARCMIN;
        }

        else if (fromUnits==Units.ARCMIN && toUnits==Units.DEGREE) {
            retval= v*ARCMIN_TO_DEG;
        }

        else if (fromUnits==Units.ARCMIN && toUnits==Units.ARCSEC) {
            retval= v*ARCMIN_TO_ARCSEC;
        }
        else { // this should never happpen
            retval= v;
            WebAssert.tst(false);
        }
        return retval;
    }




}
