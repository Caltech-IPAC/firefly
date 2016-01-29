/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.astro.conv.CoordConv;

import java.io.Serializable;


class CoordinateSys implements Serializable {

   // =====================================================================
   // --------------  Public Constans -------------------------------------
   // =====================================================================

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
      //---- they are for the convenience functions
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

    public String getEquinox() { return equinoxDesc;}

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
