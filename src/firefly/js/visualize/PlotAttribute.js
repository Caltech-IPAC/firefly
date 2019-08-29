
export const PlotAttribute= {

    MOVING_TARGET_CTX_ATTR:   'MOVING_TARGET_CTX_ATTR',

    /**
     * This will probably be a WebMouseReadoutHandler class
     * @see WebMouseReadoutHandler
     */
    READOUT_ATTR: 'READOUT_ATTR',

    READOUT_ROW_PARAMS: 'READOUT_ROW_PARAMS',

    /**
     * This will probably be a WorldPt
     * Used to overlay a target associated with this image
     */
    FIXED_TARGET: 'FIXED_TARGET',

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

    IMAGE_BOUNDS_SELECTION: 'IMAGE_BOUNDS_SELECTION',

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

    SHOW_COMPASS: 'SHOW_COMPASS',

    /**
     * This will probably an object {pt: point}
     * @See ./Point.js
     */
    ACTIVE_POINT: 'ACTIVE_POINT',


    /**
     * This is a String describing why this plot can't be rotated.  If it is defined then
     * rotating is disabled.
     */
    DISABLE_ROTATE_REASON: 'DISABLE_ROTATE_HINT',

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    FLIP_ZOOM_BY_LEVEL: 'FLIP_ZOOM_BY_LEVEL',

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    FLIP_ZOOM_TO_FILL: 'FLIP_ZOOM_TO_FILL',

    /**
     * if set, when expanded the image will be zoom to no bigger than this level,
     * this should be a subclass of Number
     */
    MAX_EXPANDED_ZOOM_LEVEL : 'MAX_EXPANDED_ZOOM_LEVEL',

    /**
     * if set, this should be the last expanded single image zoom level.
     * this should be a subclass of Number
     */
    LAST_EXPANDED_ZOOM_LEVEL : 'LAST_EXPANDED_ZOOM_LEVEL',

    /**
     * if set, must be one of the string values defined by the enum ZoomUtil.FullType
     * currently is is ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT
     */
    EXPANDED_TO_FIT_TYPE : 'MAX_EXPANDED_ZOOM_LEVEL',


    /** A table row related to this plot */
    TABLE_ROW : 'TABLE_ROW',

    /** A table id related to this plot */
    TABLE_ID : 'TABLE_ID',

    DATA_HELP_URL: 'DATA_HELP_URL',
    PROJ_TYPE_DESC: 'PROJ_TYPE_DESC',
    WAVE_TYPE: 'WAVE_TYPE',
    WAVE_LENGTH: 'WAVE_LENGTH',
    WAVE_LENGTH_DESC: 'WAVE_LENGTH',

};
