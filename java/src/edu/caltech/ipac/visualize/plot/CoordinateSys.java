package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.conv.CoordConv;

import java.io.Serializable;


public class CoordinateSys implements Serializable {

    // =====================================================================
    // --------------- Private  final variables-----------------------------
    // =====================================================================

//    private final static ClassProperties _prop= new ClassProperties(
//                                                       CoordinateSys.class);

    // =====================================================================
    // --------------- Constants for the parse Method ----------
    // =====================================================================

    public final static String EQ_J2000_STR = "EQ_J2000";
    public final static String EQ_B1950_STR = "EQ_B1950";
    public final static String EQ_B2000_STR = "EQ_B2000";
    public final static String EC_J2000_STR = "EC_J2000";
    public final static String EC_B1950_STR = "EC_B1950";
    public final static String GALACTIC_STR = "GALACTIC";
    public final static String SUPERGALACTIC_STR = "SUPERGALACTIC";
    public final static String PIXEL_STR = "PIXEL";
    public final static String SCREEN_PIXEL_STR = "SCREEN_PIXEL";
    public final static String UNDEFINED_STR = "UNDFINED";
    // =====================================================================
    // --------------  Constants Strings describing sys --------------------
    // =====================================================================

    private static final String EQ_J_DESC = "Equatorial J";
    private static final String EQ_B_DESC = "Equatorial B";
    private static final String EC_J_DESC = "Ecliptic J";
    private static final String EC_B_DESC = "Ecliptic B";
    private static final String GAL_DESC = "Galactic";
    private static final String SGAL_DESC = "Super Galactic";
    private static final String PIXEL_DESC = "Image Pixel";
    private static final String SCREEN_PIXEL_DESC = "Screen Pixel";
    private static final String UNDEF_DESC = "Undefined";

//    private static final String EQ_J_DESC  = _prop.getName("Equatorial.J");
//    private static final String EQ_B_DESC  = _prop.getName("Equatorial.B");
//    private static final String EC_J_DESC  = _prop.getName("Ecliptic.J");
//    private static final String EC_B_DESC  = _prop.getName("Ecliptic.B");
//    private static final String GAL_DESC   = _prop.getName("Galactic");
//    private static final String SGAL_DESC  = _prop.getName("SuperGalactic");
//    private static final String PIXEL_DESC = _prop.getName("Pixel");
//    private static final String SCREEN_PIXEL_DESC = _prop.getName("ScreenPixel");
//    private static final String UNDEF_DESC = _prop.getName("Undefined");

    // =====================================================================
    // --------------  Constant Predefined Coordinate systems --------------
    // =====================================================================

    public static final CoordinateSys EQ_J2000 =
            new CoordinateSys(
                    CoordConv.EQUATORIAL_J, 2000.0,
                    EQ_J_DESC,
                    "Eq-J2000 RA: ", "Eq-J2000 Dec: ", "Eq-J2000: "
            );
    public static final CoordinateSys EQ_B2000 =
            new CoordinateSys(
                    CoordConv.EQUATORIAL_B, 2000.0,
                    EQ_B_DESC,
                    "Eq-B2000 RA: ", "Eq-B2000 Dec: ", "Eq-B2000: "
            );
    public static final CoordinateSys EQ_B1950 =
            new CoordinateSys(
                    CoordConv.EQUATORIAL_B, 1950.0,
                    EQ_B_DESC,
                    "Eq-B1950 RA: ", "Eq-B1950 Dec: ", "Eq-B1950: "
            );
    public static final CoordinateSys GALACTIC =
            new CoordinateSys(
                    CoordConv.GALACTIC, 2000.0,
                    GAL_DESC,
                    "Gal. Lon: ", "Gal. Lat: ", "Gal: "
            );
    public static final CoordinateSys SUPERGALACTIC =
            new CoordinateSys(
                    CoordConv.SUPERGALACTIC, 2000.0,
                    SGAL_DESC,
                    "SGal. Lon: ", "SGal. Lat: ", "SGal: "
            );
    public static final CoordinateSys ECL_J2000 =
            new CoordinateSys(
                    CoordConv.ECLIPTIC_J, 2000.0,
                    EC_J_DESC,
                    "Ecl-J2000 RA: ", "Ecl-J2000 Dec: ", "Ecl-J2000: "
            );
    public static final CoordinateSys ECL_B1950 =
            new CoordinateSys(
                    CoordConv.ECLIPTIC_B, 1950.0,
                    EC_B_DESC,
                    "Ecl-B1950 RA: ", "Ecl-B1950 Dec: ", "Ecl-B1950: "
            );
    public static final CoordinateSys PIXEL =
            new CoordinateSys(
                    -999, 0.0,
                    PIXEL_DESC,
                    "X: ", "Y: ", "Image Pixel: "
            );
    public static final CoordinateSys SCREEN_PIXEL =
            new CoordinateSys(
                    -998, 0.0,
                    PIXEL_DESC,
                    "screen pixel X: ", "screen pixel Y: ", "screen pixel: "
            );
    public static final CoordinateSys UNDEFINED =
            new CoordinateSys(
                    -997, 0.0,
                    UNDEF_DESC,
                    "X: ", "Y: ", "coord: "
            );


    // =====================================================================
    // ------------------------ Private Variables --------------------------
    // =====================================================================

    private int _jsys;
    private double _equinox;
    private String _jsysDesc;
    private String _lonShortDesc;
    private String _latShortDesc;
    private String _shortDesc;


    private CoordinateSys() {
    }


    public CoordinateSys(int jsys,
                         double equinox,
                         String jsysDesc,
                         String lonShortDesc,
                         String latShortDesc,
                         String shortDesc) {
        _jsys = jsys;
        _equinox = equinox;
        _jsysDesc = jsysDesc;
        _lonShortDesc = lonShortDesc;
        _latShortDesc = latShortDesc;
        _shortDesc = shortDesc;
    }

    public int getJsys() {
        return _jsys;
    }

    public double getEquinox() {
        return _equinox;
    }

    public String getlonShortDesc() {
        return _lonShortDesc;
    }

    public String getlatShortDesc() {
        return _latShortDesc;
    }

    public String getShortDesc() {
        return _shortDesc;
    }

    public boolean equals(Object o) {
        boolean retval = false;
        if (o == this) {
            retval = true;
        } else if (o != null && o instanceof CoordinateSys) {
            CoordinateSys cs = (CoordinateSys) o;
            if (_jsys == cs._jsys && _equinox == cs._equinox) {
                retval = true;
            }
        }
        return retval;
    }

    public String toString() {
        String retval;
        if (this.equals(EQ_J2000)) retval = EQ_J2000_STR;
        else if (this.equals(EQ_B2000)) retval = EQ_B2000_STR;
        else if (this.equals(EQ_B1950)) retval = EQ_B1950_STR;
        else if (this.equals(GALACTIC)) retval = GALACTIC_STR;
        else if (this.equals(SUPERGALACTIC)) retval = SUPERGALACTIC_STR;
        else if (this.equals(ECL_J2000)) retval = EC_J2000_STR;
        else if (this.equals(ECL_B1950)) retval = EC_B1950_STR;
        else if (this.equals(PIXEL)) retval = PIXEL_STR;
        else if (this.equals(SCREEN_PIXEL)) retval = SCREEN_PIXEL_STR;
        else if (this.equals(UNDEFINED)) retval = UNDEFINED_STR;
        else {
            int outE = (int) _equinox;
            retval = _jsysDesc + " " + outE;
        }
        return retval;
    }


    public static CoordinateSys parse(String desc) {
        CoordinateSys coordSys;
        if (desc.equalsIgnoreCase(EQ_J2000_STR) ||
                desc.equalsIgnoreCase("EQJ") || desc.equalsIgnoreCase("J2000") ) {
            coordSys = CoordinateSys.EQ_J2000;
        } else if (desc.equalsIgnoreCase(EQ_B2000_STR))  {
            coordSys = CoordinateSys.EQ_B2000;
        } else if (desc.equalsIgnoreCase(EQ_B1950_STR) ||
                desc.equalsIgnoreCase("EQB") || desc.equalsIgnoreCase("B1950") ) {
            coordSys = CoordinateSys.EQ_B1950;
        } else if (desc.equalsIgnoreCase(GALACTIC_STR)|| desc.equalsIgnoreCase("GAL")) {
            coordSys = CoordinateSys.GALACTIC;
        } else if (desc.equalsIgnoreCase(SUPERGALACTIC_STR)) {
            coordSys = CoordinateSys.SUPERGALACTIC;
        } else if (desc.equalsIgnoreCase(EC_J2000_STR) || desc.equalsIgnoreCase("ECJ")) {
            coordSys = CoordinateSys.ECL_J2000;
        } else if (desc.equalsIgnoreCase(EC_B1950_STR) || desc.equalsIgnoreCase("ECB")) {
            coordSys = CoordinateSys.ECL_B1950;
        } else {
            coordSys = null;
        }

        return coordSys;
    }

    public static CoordinateSys makeCoordinateSys(int sys, double equ) {
        CoordinateSys coordSys = null;
        if ((equ == 2000.0) && (sys == CoordConv.EQUATORIAL_J)) {
            coordSys = CoordinateSys.EQ_J2000;
        } else if ((equ == 2000.0) && (sys == CoordConv.EQUATORIAL_B)) {
            coordSys = CoordinateSys.EQ_B2000;
        } else if ((equ == 1950.0) && (sys == CoordConv.EQUATORIAL_B)) {
            coordSys = CoordinateSys.EQ_B1950;
        } else if ((equ == 2000.0) && (sys == CoordConv.GALACTIC)) {
            coordSys = CoordinateSys.GALACTIC;
        } else if ((equ == 2000.0) && (sys == CoordConv.SUPERGALACTIC)) {
            coordSys = CoordinateSys.SUPERGALACTIC;
        } else if ((equ == 2000.0) && (sys == CoordConv.ECLIPTIC_J)) {
            coordSys = CoordinateSys.ECL_J2000;
        } else if ((equ == 1950.0) && (sys == CoordConv.ECLIPTIC_B)) {
            coordSys = CoordinateSys.ECL_B1950;
        } else {
            if (sys == CoordConv.EQUATORIAL_J)
                coordSys = new CoordinateSys(sys, equ, EQ_J_DESC,
                                             EQ_J_DESC + " RA: ",
                                             EQ_J_DESC + " Dec: ",
                                             EQ_J_DESC);
            else if (sys == CoordConv.EQUATORIAL_B)
                coordSys = new CoordinateSys(sys, equ, EQ_B_DESC,
                                             EQ_B_DESC + " RA: ",
                                             EQ_B_DESC + " Dec: ",
                                             EQ_B_DESC);
            else if (sys == CoordConv.ECLIPTIC_J)
                coordSys = new CoordinateSys(sys, equ, EC_J_DESC,
                                             EC_J_DESC + " RA: ",
                                             EC_J_DESC + " Dec: ",
                                             EC_J_DESC);
            else if (sys == CoordConv.ECLIPTIC_B)
                coordSys = new CoordinateSys(sys, equ, EC_B_DESC,
                                             EC_B_DESC + " RA: ",
                                             EC_B_DESC + " Dec: ",
                                             EC_B_DESC);
            else if (sys == CoordConv.GALACTIC)
                coordSys = new CoordinateSys(sys, equ, GAL_DESC,
                                             GAL_DESC + " Lon: ",
                                             GAL_DESC + " Lat: ",
                                             GAL_DESC);
            else if (sys == CoordConv.SUPERGALACTIC)
                coordSys = new CoordinateSys(sys, equ, SGAL_DESC,
                                             SGAL_DESC + " Lon: ",
                                             SGAL_DESC + " Lat: ",
                                             SGAL_DESC);

            else
                coordSys = UNDEFINED;
        }
        return coordSys;
    }

    public boolean isEquatorial() {
        return (_jsys == CoordConv.EQUATORIAL_J ||
                _jsys == CoordConv.EQUATORIAL_B);
    }
}


