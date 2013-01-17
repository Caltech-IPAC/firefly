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

    public enum Units {DEGREE, ARCMIN, ARCSEC}

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
