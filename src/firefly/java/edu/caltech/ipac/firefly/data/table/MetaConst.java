/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

/**
 * Meta info that are used on server-side as well as client-side.  Make sure the constants
 * are in synced with data/MetaConst.js
 * @author Trey Roby
 */
public class MetaConst {

    /**
     * Meta entry that defines how to compute the center position of table row
     * Form: lonCol;latCol;CoordinateSys.toString(), eg ra;dec;J2000
     * @see edu.caltech.ipac.table.TableMeta#setCenterCoordColumns
     */
    public static final String CENTER_COLUMN = "CENTER_COLUMN";

    /**
     * Meta entry that defines how to compute the image point position
     * This is used in the case where this table is tied to a single FITS file
     * use with FITS_FILE_PATH
     */
    public static final String IMAGE_COLUMN = "IMAGE_COLUMN";

    /**
     *
     * Meta entry that defines the corners of a object defined by a table row
     * like CENTER_COLUMN but each position separated by comma
     * eg - ra1;dec1;EQ_J2000,ra2;dec2;EQ_J2000,ra3;dec3;EQ_J2000,ra4;dec4;EQ_J2000
     * @see edu.caltech.ipac.table.TableMeta#setCorners
     */
    public static final String ALL_CORNERS   = "ALL_CORNERS";

    /*
     * comma separated names of the columns to assign to the table when reading a fits image as a table.
     */
    public static final String IMAGE_AS_TABLE_COL_NAMES= "IMAGE_AS_TABLE_COL_NAMES";

    /*
     * comma separated names of the units to assign to the table when reading a fits image as a table.
     */
    public static final String IMAGE_AS_TABLE_UNITS= "IMAGE_AS_TABLE_UNITS";

    /*
     * a string the default says what this data type might be such as 'spectrum', 'sed', 'timeseries', etc
     */
    public static final String DATA_TYPE_HINT = "DATA_TYPE_HINT";

    /**
     * If defined to any value (but 'false') the the table is a catalog
     * CatalogOverlayType is required to even guess if there is no VO information.
     * Also it can be defined to FALSE to disable it being a catalog altogether even with VO information.
     */
    public static final String CATALOG_OVERLAY_TYPE = "CatalogOverlayType";

    /**
     * if defined this table container a moving object, setting this will override any catalog evaluation
     * value can me true or false, if true then evaluate this table as a moving obj, if false then ignore
     */
    public static final String ORBITAL_PATH = "ORBITAL_PATH";

    /**
     * How this data was extracted from the FITS file, should be z-axis, line, or points
     */
    public static final String FITS_EXTRACTION_TYPE= "FitsExtractionType";

    /**
     * An world point in a fits FILE that is associated with this table
     */
    public static final String FITS_WORLD_PT= "FitsWorldPoint";

    /**
     * An image point in a fits FILE that is associated with this table
     */
    public static final String FITS_IM_PT= "FitsImPoint";

    /**
     * A second image point in a fits FILE that is associated with this table
     */
    public static final String FITS_IM_PT2= "FitsImPoint2";

    /**
     * Fits Image server file path, a path to a FITS file that is associated with this table
     * It is used in extraction.  An extracted tables set the fits file path from the server.
     * FITS_FILE_PATH in a table can match the plotState.getWorkingFitsFileStr() from a WebPlot
     */
    public static final String FITS_FILE_PATH= "FitsFilePath";

    /**
     * Fits Image HDU, The HDU number of a FITS file that is associated with this table
     */
    public static final String FITS_IMAGE_HDU= "FitsImageHDU";

    /**
     * Fits Image plane , The cube plane number
     */
    public static final String FITS_IMAGE_HDU_CUBE_PLANE= "FitsImageHDUCubePlane";


    /** @deprecated replaced by IMAGE_SOURCE_ID */
    @Deprecated
    public final static String DATASET_CONVERTER = "datasetInfoConverterId";
    
    /** id string for types of image */
    public final static String IMAGE_SOURCE_ID = "ImageSourceId";

    /**
     * default chart properties for this table
     * Overrides DEFAULT_CHART_X_COL and DEFAULT_CHART_Y_COL
     */
    public static final String DEFAULT_CHART_DEF= "defaultChartDef";

    /**
     * default chart x column for this table - forces the chart to make the default chart with this x column
     * not used with DEFAULT_CHART_DEF
     */
    public static final String DEFAULT_CHART_X_COL= "defaultChartXCol";

    /**
     * default chart x column for this table - forces the chart to make the default chart with this x column
     * not used with DEFAULT_CHART_DEF
     * */
    public static final String DEFAULT_CHART_Y_COL= "defaultChartYCol";

    /** the column name with access rights info;  true if (public, 1, or true), otherwise false  */
    public static final String DATARIGHTS_COL = "DATARIGHTS_COL";

    /** the column name with public release date info;  null is considered not public */
    public static final String  RELEASE_DATE_COL = "RELEASE_DATE_COL";

    // meta options
    public static final String HIGHLIGHTED_ROW = "highlightedRow";
    public static final String HIGHLIGHTED_ROW_BY_ROWIDX = "highlightedRowByRowIdx";

    /** if defined and true and a table is a MOC then the table will not be loaded as a MOC layer, just as a table */
    public static final String IGNORE_MOC= "ignoreMOC";
}

