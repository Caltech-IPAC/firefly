/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data.table;

/**
 * User: roby
 * Date: Apr 16, 2010
 * Time: 2:22:46 PM
 */


/**
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

    /** if defined to any value the the table is a catalog */
    public static final String CATALOG_OVERLAY_TYPE = "CatalogOverlayType";

    /** @deprecated replaced by IMAGE_SOURCE_ID */
    @Deprecated
    public final static String DATASET_CONVERTER = "datasetInfoConverterId";
    
    /** id string for types of image */
    public final static String IMAGE_SOURCE_ID = "ImageSourceId";
    /** id string for types of mission */
    public static final String MISSION = "mission";

}

