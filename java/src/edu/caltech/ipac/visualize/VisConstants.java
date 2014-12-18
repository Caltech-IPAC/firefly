package edu.caltech.ipac.visualize;

import edu.caltech.ipac.util.action.ClassProperties;

public class VisConstants {
    private final static ClassProperties  _prop= new ClassProperties(
                                                       VisConstants.class);
    public final static String WORKING_DIR_PROP= "vis.lastWorkingDirectory"; 
    public final static String FITS_TYPES      = _prop.getName("fitsFilter");
    public final static String JPEG_TYPES      = _prop.getName("jpegFilter");
    public final static String GIF_TYPES       = _prop.getName("gifFilter");
    public final static String PNG_TYPES       = _prop.getName("pngFilter");
    public final static String BMP_TYPES       = _prop.getName("bmpFilter");
    public final static String GZ_FITS_TYPES   = _prop.getName("gzFitsFilter");
    public final static String TABLE_TYPES     = _prop.getName("tableFilter");

    public final static String EQ_J2000      = "EQ_J2000";
    public final static String EQ_B1950      = "EQ_B1950";
    public final static String EC_J2000      = "EC_J2000";
    public final static String EC_B1950      = "EC_B1950";
    public final static String GALACTIC      = "GALACTIC";
    public final static String SUPERGALACTIC = "SUPERGALACTIC";

    public final static String COORD_SYS_PROP= "Coordinate.Sys.Default";
    public final static String COORD_DEC_PROP= "Coordinate.doDecimal.Default";

    public final static double    ARCMIN_TO_DEG = .01666666667;
    public final static double    ARCSEC_TO_DEG = .00027777778;
    public final static double    DEG_TO_ARCMIN = 60.0;
    public final static double    DEG_TO_ARCSEC = 3600.0;




}



