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
     * Meta entry that defines how to compute the image point position
     * This is used in the case where this table is tied to a single FITS file
     * use with FITS_FILE_PATH
     */
    IMAGE_COLUMN: 'IMAGE_COLUMN',

    /**
     * For coverage or catalog, an overlay position to use for this table
     */
    OVERLAY_POSITION : 'OverlayPosition',

    /**
     * How this data was extracted from the FITS file, should be z-axis, line, or points
     */
    FITS_EXTRACTION_TYPE: 'FitsExtractionType',

    /**
     * An world point in a fits FILE that is associated with this table
     */
    FITS_WORLD_PT: 'FitsWorldPoint',

    /**
     * An image point in a fits FILE that is associated with this table
     */
    FITS_IM_PT: 'FitsImPoint',

    /**
     * A second image point in a fits FILE that is associated with this table
     */
    FITS_IM_PT2: 'FitsImPoint2',

    /**
     * Fits Image server file path, a path to a FITS file that is associated with this table
     * It is used in extraction.  An extracted tables set the fits file path from the server.
     * FITS_FILE_PATH in a table can match the plotState.getWorkingFitsFileStr() from a WebPlot
     */
    FITS_FILE_PATH: 'FitsFilePath',

    /**
     * Fits Image HDU, The HDU number of a FITS file that is associated with this table
     */
    FITS_IMAGE_HDU: 'FitsImageHDU',

    /**
     * Fits Image plane , The cube plane number
     */
    FITS_IMAGE_HDU_CUBE_PLANE: 'FitsImageHDUCubePlane',


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

    /**
     * default chart properties for this table
     * Overrides DEFAULT_CHART_X_COL and DEFAULT_CHART_Y_COL
     */
    DEFAULT_CHART_DEF : 'defaultChartDef',

    /**
     * default chart x column for this table - forces the chart to make the default chart with this x column
     * not used with DEFAULT_CHART_DEF
     */
    DEFAULT_CHART_X_COL: 'defaultChartXCol',

    /**
     * default chart x column for this table - forces the chart to make the default chart with this x column
     * not used with DEFAULT_CHART_DEF
     * */
    DEFAULT_CHART_Y_COL: 'defaultChartYCol',


    /** a string or html rgb/rgba color that can be set in metadata, use with catalog overlay */
    DEFAULT_COLOR : 'DEFAULT_COLOR',

    /** a string to represent the drawing symbol
     * @see ../visualize/draw/PointDataObj.js for a list of draw symbols, DrawSymbol
     */
    DEFAULT_SYMBOL : 'DEFAULT_SYMBOL',

    /** a string to represent the drawing style for MOC display, 'outline' or 'fill' */
    MOC_DEFAULT_STYLE: 'MOC_DEFAULT_STYLE',

    /**
     * a URL or IVO ID
     * If this table is a MOC or some other type of supported overlay, then load the PREFERRED_HIPS if there is
     * no existing HiPS loaded
     */
    PREFERRED_HIPS : 'PREFERRED_HIPS',

    /**
     *
     * a URL or IVO ID
     * If this table is a MOC or some other type of supported overlay, then always attempt to load the REQUIRED_HIPS
     * even it there are other HiPS loaded.
     */
    REQUIRED_HIPS : 'REQUIRED_HIPS',

    /** used by time series, defines the mission
     * @see LcManager.js
     * @see getConverterId
     */
    TS_DATASET : 'TSDatasetId',

    /*
     * comma separated names of the columns to assign to the table when reading a fits image as a table.
     */
    IMAGE_AS_TABLE_COL_NAMES: 'IMAGE_AS_TABLE_COL_NAMES',

    /*
     * comma separated names of the units to assign to the table when reading a fits image as a table.
     */
    IMAGE_AS_TABLE_UNITS: 'IMAGE_AS_TABLE_UNITS',

    /*
     * a string the default says what this data type might be such as 'spectrum', 'sed', 'timeseries', etc
     */
    DATA_TYPE_HINT: 'DATA_TYPE_HINT',

    /** the column name with the url or filename of the image data */
    DATA_SOURCE : 'DataSource',

    /** column name includes the link to the PNG image preview */
    IMAGE_PREVIEW : 'ImagePreview',

    /** a url for HiPS Image to use as the coverage - overrides firefly defaults */
    COVERAGE_HIPS : 'CoverageHiPS',

    /** if y,t,true,yes the treat then recognized this table to have simulated data */
    SIMULATED_TABLE: 'SIMULATED_TABLE',

    /**
     * if true, Show the coverage display even it this table does not have coverage information
     * if false, treat this table as it has no coverage
     * value must be true or false, it not defined or has some other value then evaluate the table for coverage as normal.
     */
    COVERAGE_SHOWING : 'CoverageShowing',

    /** the column name with access rights info;  true if (public, 1, or true), otherwise false  */
    DATARIGHTS_COL : 'DATARIGHTS_COL',

    /** the column name with public release date info;  null is considered not public */
    RELEASE_DATE_COL : 'RELEASE_DATE_COL',

    /**
     * if defined this table container a moving object, setting this will override any catalog evaluation
     * value can me true or false, if true then evaluate this table as a moving obj, if false then ignore
     */
    ORBITAL_PATH : 'ORBITAL_PATH',

    HIGHLIGHTED_ROW             : 'highlightedRow',                 // row to highlight on data fetch
    HIGHLIGHTED_ROW_BY_ROWIDX   : 'highlightedRowByRowIdx',         // row to highlight on data fetch based on original row index


    /**
     * The id of the file analyzer to call
     * example - analyzerId: Sofia
     */
    ANALYZER_ID : 'AnalyzerId',

    /**
     * a list of columns, separated by commas, whose values will be send as parameter to the related DataProductAnalyzer
     * The key will the the columns names, the values will the taken from the active row. Therefore the values
     * will change as different rows are selected.
     * example - analyzerColumns: Processing Level,Product Type,Bandpass
     *
     */
    ANALYZER_COLUMNS : 'AnalyzerColumns',

    /**
     * a list of parameters, separated by commas, that will be send to the data product analyzer. The key/value pairs will be the same
     * regardless of which row is selected.
     * example - analyzerIdParams: instrument=FLITECAM,A=123
     */
    ANALYZER_PARAMS : 'AnalyzerParams',

    /** if defined and true and a table is a MOC then the table will not be loaded as a MOC layer, just as a table */
    IGNORE_MOC : 'ignoreMOC',

    LOAD_TO_DATALINK_UI : 'LoadToServiceDescriptorUI',

    /**
     * determine how a datalink obscore table trys to show the data layout, must be 'single', 'gridRelated', 'gridFull';
     */
    DATALINK_INITIAL_LAYOUT : 'DatalinkInitialLayout',

    /**
     * If defined and true, dispatchTableSearch will be called but prevent going to results view directly
     */
    UPLOAD_TABLE: 'UploadTable',

    /** @deprecated use CENTER_COLUMN */
    CATALOG_COORD_COLS : 'CatalogCoordColumns',

    /** @deprecated use CENTER_COLUMN */
    POSITION_COORD_COLS : 'positionCoordColumns',

    /** @deprecated replaced by IMAGE_SOURCE_ID */
    DATASET_CONVERTER : 'datasetInfoConverterId'


};
