package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.astro.conv.CoordConv;

import java.io.Serializable;


public class CoordinateSys implements Serializable, Comparable {

   // =====================================================================
   // --------------  Public Constans -------------------------------------
   // =====================================================================
    public static final String EQ_J2000_STR = "EQ_J2000";
    public static final String EQ_B1950_STR = "EQ_B1950";
    public static final String EC_J2000_STR = "EC_J2000";
    public static final String EC_B1950_STR = "EC_B1950";
    public static final String GALACTIC_STR = "GALACTIC";

    public static final String  EQUATORIAL_NAME = "Equatorial";
    public static final String  GALACTIC_NAME   = "Galactic";
    public static final String  ECLIPTIC_NAME   = "Ecliptic";

    public static final String J2000_DESC = "J2000";
    public static final String B1950_DESC = "B1950";

   // =====================================================================
   // --------------  Public Constant Predefined Coordinate systems -------
   // =====================================================================
    public static final CoordinateSys EQ_J2000 = 
                             new CoordinateSys(CoordConv.EQUATORIAL_J, 2000.0);
    public static final CoordinateSys EQ_B1950 = 
                             new CoordinateSys(CoordConv.EQUATORIAL_B, 1950.0);
    public static final CoordinateSys GALACTIC = 
                             new CoordinateSys(CoordConv.GALACTIC, 2000.0);
    public static final CoordinateSys ECL_J2000 =
                             new CoordinateSys(CoordConv.ECLIPTIC_J, 2000.0);
    public static final CoordinateSys ECL_B1950 = 
                             new CoordinateSys(CoordConv.ECLIPTIC_B, 1950.0);

   // =====================================================================
   // --------------  Private Constants -----------------------------------
   // =====================================================================

    private static final String EQ_J_DESC = "Equatorial J";
    private static final String EQ_B_DESC = "Equatorial B";
    private static final String GAL_DESC  = "Galactic";
    private static final String ECL_J_DESC= "Ecliptic J";
    private static final String ECL_B_DESC= "Ecliptic B";


      //---- these are the two primary varibles that make up a CoodinateSys
    private final int jsys;  // one of the constants defined in CoordConv
    private final double equinoxYear;

      //---- these are all derived based on jsys & equinoxYear
      //---- they are for the convience functions
    private final String equinoxDesc;
    private final String coodSysName;
    private final String jsysDesc;
    private final boolean equatorial;
    private final boolean ecliptic;
    private final boolean galactic;

    private CoordinateSys(int i, double d)
    {
       String  eDesc= null;
       double  eYear= d;
       boolean gal= false;
       String  csysName= null;

       switch(i) {
            case CoordConv.EQUATORIAL_J:
                jsysDesc = EQ_J_DESC;
                equatorial = true;
                ecliptic = false;
                eDesc= J2000_DESC;
                csysName= EQUATORIAL_NAME;
                break;

            case CoordConv.EQUATORIAL_B:
                jsysDesc = EQ_B_DESC;
                equatorial = true;
                ecliptic = false;
                csysName= EQUATORIAL_NAME;
                eDesc= B1950_DESC;
                break;

            case CoordConv.GALACTIC:
                jsysDesc = GAL_DESC;
                equatorial = false;
                ecliptic = false;
                eYear= 2000D;
                csysName= GALACTIC_NAME;
                eDesc= null;
                gal  = true;
                break;

            case CoordConv.ECLIPTIC_J:
                jsysDesc = ECL_J_DESC;
                equatorial = false;
                ecliptic = true;
                eDesc= J2000_DESC;
                csysName= ECLIPTIC_NAME;
                break;

            case CoordConv.ECLIPTIC_B:
                jsysDesc = ECL_B_DESC;
                equatorial = false;
                ecliptic = true;
                eDesc= B1950_DESC;
                csysName= ECLIPTIC_NAME;
                break;

            default:
                jsysDesc = null;
                equatorial = false;
                ecliptic = false;
                eDesc       = null;
                csysName    = null;
                Assert.tst(false);
                break;
       }
       equinoxDesc = eDesc;
       equinoxYear = eYear;
       galactic = gal;
       jsys = i;
       coodSysName = csysName;
    }

    public int getJsys() { return jsys; }

    public double getEquinoxYear() { return equinoxYear; }

    public boolean isEquatorial() { return equatorial; }

    public boolean isEcliptic() { return ecliptic; }

    public boolean isGalactic() { return galactic; }

    public String getEquinox() { return equinoxDesc;}

    public String getCoordSysName() { return coodSysName;}

    public int compareTo(Object obj) {
       int retval = -1;
       if(obj == this) {
          retval = 0;
       } //end if
       else if (obj!=null && obj instanceof CoordinateSys) {
          if(obj.getClass() == getClass()) {
             CoordinateSys coordinatesys = (CoordinateSys)obj;
             if (jsys > coordinatesys.jsys)      retval= 1;
             else if (jsys < coordinatesys.jsys) retval= -1;
             else if (jsys == coordinatesys.jsys) {
                if (equinoxYear > coordinatesys.equinoxYear)      retval= 1;
                else if (equinoxYear < coordinatesys.equinoxYear) retval= -1;
                else if (equinoxYear == coordinatesys.equinoxYear)retval= 0;
             }
          }
       } // end else
       return retval;
    }

    public boolean equals(Object obj) {
        boolean retval = false;
        if(obj == this) {
            retval = true;
        }
        else if (obj!=null && obj instanceof CoordinateSys) {
            CoordinateSys coordinatesys = (CoordinateSys)obj;
            if(jsys == coordinatesys.jsys &&
               equinoxYear == coordinatesys.equinoxYear) {
                retval = true;
            }
        }
        return retval;
    }

    public String toString() {
        int i = (int) equinoxYear;
        String retval;
        if (galactic)  {
           retval= jsysDesc;
        }
        else {
           retval= jsysDesc + i;
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
