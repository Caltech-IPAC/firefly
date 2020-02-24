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


    /** @deprecated replaced by IMAGE_SOURCE_ID */
    @Deprecated
    public final static String DATASET_CONVERTER = "datasetInfoConverterId";
    
    /** id string for types of image */
    public final static String IMAGE_SOURCE_ID = "ImageSourceId";

    /** the column name with access rights info;  true if (public, 1, or true), otherwise false  */
    public static final String DATARIGHTS_COL = "DATARIGHTS_COL";

    /** the column name with public release date info;  null is considered not public */
    public static final String  RELEASE_DATE_COL = "RELEASE_DATE_COL";

    // meta options
    public static final String HIGHLIGHTED_ROW = "highlightedRow";
    public static final String HIGHLIGHTED_ROW_BY_ROWIDX = "highlightedRowByRowIdx";
}

