/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * List of constants that are used in meta data of tables
 */
export const MetaConst = {


    /**
     * Meta entry that defines how to compute the center position of table row
     * 3 entries separated by ; the first two are column names
     * Form: lonCol;latCol;CoordinateSys.toString(),
     * eg if table has a ra column and a dec columns then: 'ra;dec;J2000'
     * @see CoordSys.js
     */
    CENTER_COLUMN : 'CENTER_COLUMN',


    /**
     *
     * Meta entry that defines the corners of a object defined by a table row
     * like CENTER_COLUMN but each position separated by comma
     * eg - 'ra1;dec1;EQ_J2000,ra2;dec2;EQ_J2000,ra3;dec3;EQ_J2000,ra4;dec4;EQ_J2000'
     */
    ALL_CORNERS   : 'ALL_CORNERS',

    /**
     * If defined to any value (but 'false') the the table is a catalog
     * CatalogOverlayType is required to even guess if there is no VO information.
     * @see isCatalog
     */
    CATALOG_OVERLAY_TYPE : 'CatalogOverlayType',

    /**
     * a position for the whole table. eq j2000- 1.2;3.4;EQ_J2000 or Galactic  7.7;9.2;GAL
     */
    POSITION_COORD: 'positionCoord',


    /** id string for types of image */
    IMAGE_SOURCE_ID : 'ImageSourceId',

    /** default chart properties for this table */
    DEFAULT_CHART_DEF : 'defaultChartDef',

    /** a string or html rgb color that can be set in metadata, use with catalog overlay */
    DEFAULT_COLOR : 'DEFAULT_COLOR',

    /** a string to represent the drawing symbol
     * @see ../visualize/draw/PointDataObj.js for a list of draw symbols, DrawSymbol
     */
    DEFAULT_SYMBOL : 'DEFAULT_SYMBOL',

    /** used by time series, defines the mission
     * @see LcManager.js
     * @see getConverterId
     */
    TS_DATASET : 'TSDatasetId',

    /** the column name with the url or filename of the image data */
    DATA_SOURCE : 'DataSource',

    /** a url for HiPS Image to use as the coverage - overrides firefly defaults */
    COVERAGE_HIPS : 'CoverageHiPS',

    /** the column name with access rights info;  true if (public, 1, or true), otherwise false  */
    DATARIGHTS_COL : 'DATARIGHTS_COL',

    /** the column name with public release date info;  null is considered not public */
    RELEASE_DATE_COL : 'RELEASE_DATE_COL',

    HIGHLIGHTED_ROW             : 'highlightedRow',                 // row to highlight on data fetch
    HIGHLIGHTED_ROW_BY_ROWIDX   : 'highlightedRowByRowIdx',         // row to highlight on data fetch based on original row index

    /** @deprecated use CENTER_COLUMN */
    CATALOG_COORD_COLS : 'CatalogCoordColumns',

    /** @deprecated use CENTER_COLUMN */
    POSITION_COORD_COLS : 'positionCoordColumns',

    /** @deprecated replaced by IMAGE_SOURCE_ID */
    DATASET_CONVERTER : 'datasetInfoConverterId'

};
