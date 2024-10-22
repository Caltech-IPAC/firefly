export const PlotAttribute= {



    // -----    no longer used
    // READOUT_ATTR: 'READOUT_ATTR',
    // MOVING_TARGET_CTX_ATTR:   'MOVING_TARGET_CTX_ATTR',
    // READOUT_ROW_PARAMS: 'READOUT_ROW_PARAMS',
    // SHOW_COMPASS: 'SHOW_COMPASS',
    // DISABLE_ROTATE_REASON: 'DISABLE_ROTATE_HINT',
    // FLIP_ZOOM_BY_LEVEL: 'FLIP_ZOOM_BY_LEVEL',
    // FLIP_ZOOM_TO_FILL: 'FLIP_ZOOM_TO_FILL',
    // MAX_EXPANDED_ZOOM_LEVEL : 'MAX_EXPANDED_ZOOM_LEVEL',
    // LAST_EXPANDED_ZOOM_LEVEL : 'LAST_EXPANDED_ZOOM_LEVEL',
    // COVERAGE_CREATED: 'COVERAGE_CREATED',

    /**
     * This will probably be a WorldPt
     * Used to overlay a target associated with this image
     */
    FIXED_TARGET: 'FIXED_TARGET',

    /**
     * This may be a number or a string. If the string ends with a % it uses the percentage to get the initial plane
     * i.e  4 or '56' or '20%'
     */
    CUBE_FIRST_FRAME: 'CUBE_FIRST_FRAME',

    /**
     * a WorldPt
     * if defined overrides FIXED_TARGET.
     */
    CENTER_ON_FIXED_TARGET: 'CENTER_ON_FIXED_TARGET',

    /**
     * a position to center a new plot on,the point object of the position to scroll image to when loaded
     */
    INIT_CENTER: 'INIT_CENTER',

    /**
     * This will probably be a double with the requested size of the plot
     */
    REQUESTED_SIZE: 'REQUESTED_SIZE',


    /**
     * This will probably an object represent a rectangle {pt0: point,pt1: point}
     * @See ./Point.js
     */
    SELECTION: 'SELECTION',

    /** the selection type, a string - 'rect' or 'circle' */
    SELECTION_TYPE: 'SELECTION_TYPE',

    /** the component who created the selection eg ('SelectArea' or 'SearchRefinementTool') */
    SELECTION_SOURCE: 'SELECTION_SOURCE',


    IMAGE_BOUNDS_SELECTION: 'IMAGE_BOUNDS_SELECTION',

    /**
     * A polygon that is drawn on the image or HiPS, it should be an arrays of points [pt,pt,pt,pt,pt]
     * probably not more than 15 points
     */
    POLYGON_ARY: 'POLYGON_ARY',

    RELATIVE_IMAGE_POLYGON_ARY: 'RELATIVE_IMAGE_POLYGON_ARY',

    /**
     * boolean
     */
    USE_POLYGON: 'USE_POLYGON',

    SELECT_ACTIVE_CHART_PT: 'selectActiveChartPt',

    /**
     * setting for outline image, bounds (for FootprintObj) or drawObj, text, textLoc,
     */
    OUTLINEIMAGE_BOUNDS: 'OUTLINEIMAGE_BOUNDS',
    OUTLINEIMAGE_TITLE: 'OUTLINEIMAGE_TITLE',
    OUTLINEIMAGE_TITLELOC: 'OUTLINEIMAGE_TITLELOC',
    OUTLINEIMAGE_DRAWOBJ: 'OUTLINE_OBJ',
    /**
     * This will probably an object to represent a line {pt0: point,pt1: point}
     * @See ./Point.js
     */
    ACTIVE_DISTANCE: 'ACTIVE_DISTANCE',

    /** point used for extractions, it should be a (possibly large) array of points [pt,pt,pt,pt,pt,...,pt] */
    PT_ARY: 'PT_ARY',

    /** true if there is extraction data */
    EXTRACTION_DATA: 'EXTRACTION_DATA',

    /**
     * This will probably an object {pt: point}
     * @See ./Point.js
     */
    ACTIVE_POINT: 'ACTIVE_POINT',


    /**
     * if set, must be one of the string values defined by the enum ZoomUtil.FullType
     * currently it is ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT
     */
    EXPANDED_TO_FIT_TYPE : 'MAX_EXPANDED_ZOOM_LEVEL',

    /** A title to be post-pended to the title */
    POST_TITLE: 'PostTitle',

    /** A title to be prepended to the title */
    PRE_TITLE: 'PreTitle',

    /** A header from the FITS file whose value is used in the per HDU title line */
    HDU_TITLE_HEADER: 'HDU_TITLE_HEADER',

    /** a description for the HDU_TITLE_HEADER that is use in the per HDU title line*/
    HDU_TITLE_DESC: 'HDU_TITLE_DESC',

    /** a key for saving color table selection to preferences */
    PREFERENCE_COLOR_KEY : 'PreferenceColorKey',

    /** an url for help about the dataset for this image */
    DATA_HELP_URL: 'DATA_HELP_URL',

    /** a description of the projection - a string*/
    PROJ_TYPE_DESC: 'PROJ_TYPE_DESC',

    /** wavelength type */
    WAVE_TYPE: 'WAVE_TYPE',

    /** a description of the wavelength - a string*/
    WAVE_LENGTH: 'WAVE_LENGTH',

    /** the wavelength - a number */
    WAVE_LENGTH_UM: 'WAVE_LENGTH_UM',


    /** A source datalink (or similar) table row that was used to construct this plot */
    RELATED_TABLE_ROW : 'DATALINK_TABLE_ROW',

    /** A source datalink (or similar) table id related that was used to construct this plot */
    RELATED_TABLE_ID : 'DATALINK_TABLE_ID',

    /** an array of table ids used by coverage or other representation of these tables */
    VISUALIZED_TABLE_IDS : 'VISUALIZED_TABLE_IDS',

    /** hint on how the plot happened - it was just replotted with a different center */
    REPLOT_WITH_NEW_CENTER: 'REPLOT_WITH_NEW_CENTER',

    /** a world point that a user selected for a search */
    USER_SEARCH_WP: 'USER_SEARCH_WP',

    /** the radius of the search */
    USER_SEARCH_RADIUS_DEG: 'USER_SEARCH_RADIUS_DEG',

    /** an object warnings: {key: string, warning:string} */
    USER_WARNINGS: 'USER_WARNINGS'
};
