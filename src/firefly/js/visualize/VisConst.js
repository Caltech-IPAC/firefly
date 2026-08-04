/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import Enum from 'enum';

// redux store object keys
export const IMAGE_PLOT_KEY = 'allPlots';
export const IMAGE_MULTI_VIEW_KEY = 'imageMultiView';
export const DRAWING_LAYER_KEY = 'drawLayers';


// for reason parameter of CHANGE_ACTIVE_PLOT_VIEW action
export const MOUSE_CLICK_REASON = 'mouseClickReason';
export const OTHER_REASON = 'otherReason';


// action name prefixes
export const PLOTS_PREFIX = 'ImagePlotCntlr';
export const IMAGE_MULTI_VIEW_PREFIX = 'MultiViewCntlr';
export const DRAWLAYER_PREFIX = 'DrawLayerCntlr';



// MultiViewCntlr container types for Viewer.containerType
export const IMAGE = 'image';
export const PLOT2D = 'plot2d';
export const WRAPPER = 'wrapper';


// use by DrawingLayerCntrl
export const SUBGROUP = 'subgroup';



// Reserved IDs for containers and plot groups
export const EXPANDED_MODE_RESERVED = 'EXPANDED_MODE_RESERVED';
export const DEFAULT_FITS_VIEWER_ID = 'DEFAULT_FITS_VIEWER_ID';
export const DEFAULT_PLOT2D_VIEWER_ID = 'DEFAULT_PLOT2D_VIEWER_ID';
export const PINNED_CHART_VIEWER_ID = 'PINNED_CHART_VIEWER_ID';
export const DEFAULT_COVERAGE_PLOT_ID = 'CoveragePlot';
export const DEFAULT_COVERAGE_VIEWER_ID = 'CoverageImages';
export const META_VIEWER_ID = 'triViewImageMetaData';


// type of Viewer.layout
export const SINGLE = 'single';
export const GRID = 'grid';

// types of grid layouts for Viewer.layoutDetail
export const GRID_RELATED = 'gridRelated';
export const GRID_FULL = 'gridFull';

// partial id constants
export const CANVAS_IMAGE_ID_START = 'image-';
export const CANVAS_DL_ID_START = 'dl-';

// constants use by drawing layers
export const RegionSelectStyle = ['UprightBox', 'DottedOverlay', 'SolidOverlay', 'DottedReplace', 'SolidReplace'];
export const defaultRegionSelectColor = '#DAA520';   // golden
export const defaultRegionSelectStyle = RegionSelectStyle[0];
export const RegionSelColor = 'selectColor';
export const RegionSelStyle = 'selectStyle';


// ----------------------------------------
// actions use by ImagePlotCntlr
// ----------------------------------------
/** Action Type: plot of new image started */
export const PLOT_IMAGE_START = `${PLOTS_PREFIX}.PlotImageStart`;
/** Action Type: plot of new image failed */
export const PLOT_IMAGE_FAIL = `${PLOTS_PREFIX}.PlotImageFail`;
/** Action Type: plot of new image completed */
export const PLOT_IMAGE = `${PLOTS_PREFIX}.PlotImage`;
export const PLOT_PROXY = `${PLOTS_PREFIX}.PlotProxy`;
export const REMOVE_PROXY = `${PLOTS_PREFIX}.RemoveProxy`;
/** Action Type: plot of new HiPS image */
export const PLOT_HIPS = `${PLOTS_PREFIX}.PlotHiPS`;
export const ABORT_HIPS = `${PLOTS_PREFIX}.AbortHiPS`;
export const CHANGE_HIPS = `${PLOTS_PREFIX}.ChangeHiPS`;
export const PLOT_HIPS_OR_IMAGE = `${PLOTS_PREFIX}.plotHiPSOrImage`;
export const PLOT_HIPS_FAIL = `${PLOTS_PREFIX}.PlotHiPSFail`;
/** Action Type: A image replot occurred */
export const ANY_REPLOT = `${PLOTS_PREFIX}.Replot`;
/** Action Type: start the zoom process.  The image will appear zoomed by scaling, the server has not updated yet */
export const ZOOM_HIPS = `${PLOTS_PREFIX}.ZoomHiPS`;
/** Action Type: The zoom from the server has complete */
export const ZOOM_IMAGE = `${PLOTS_PREFIX}.ZoomImage`;
export const ZOOM_LOCKING = `${PLOTS_PREFIX}.ZoomEnableLocking`;
/** Action Type: image with new color table loaded */
export const COLOR_CHANGE = `${PLOTS_PREFIX}.ColorChange`;
/** Action Type: image loaded with new stretch */
export const STRETCH_CHANGE = `${PLOTS_PREFIX}.StretchChange`;
/** Action Type: image rotated */
export const ROTATE = `${PLOTS_PREFIX}.Rotate`;
/** Action Type: image flipped */
export const FLIP = `${PLOTS_PREFIX}.Flip`;
export const CROP_START = `${PLOTS_PREFIX}.CropStart`;
/** Action Type: image cropped */
export const CROP = `${PLOTS_PREFIX}.Crop`;
export const CROP_FAIL = `${PLOTS_PREFIX}.CropFail`;
export const UPDATE_VIEW_SIZE = `${PLOTS_PREFIX}.UpdateViewSize`;
export const PROCESS_SCROLL = `${PLOTS_PREFIX}.ProcessScroll`;
export const CHANGE_CENTER_OF_PROJECTION = `${PLOTS_PREFIX}.changeCenterOfProjection`;
/** Action Type: Recenter in image on the active target */
export const RECENTER = `${PLOTS_PREFIX}.recenter`;
export const MARK_OUT_OF_MEMORY = `${PLOTS_PREFIX}.makeOutOfMemory`;
/** Action Type: replot the image with the original plot parameters */
export const RESTORE_DEFAULTS = `${PLOTS_PREFIX}.restoreDefaults`;
export const POSITION_LOCKING = `${PLOTS_PREFIX}.PositionLocking`;
export const OVERLAY_COLOR_LOCKING = `${PLOTS_PREFIX}.OverlayColorLocking`;
export const CHANGE_POINT_SELECTION = `${PLOTS_PREFIX}.ChangePointSelection`;
export const CHANGE_ACTIVE_PLOT_VIEW = `${PLOTS_PREFIX}.ChangeActivePlotView`;
export const CHANGE_SUBHIGHLIGHT_PLOT_VIEW = `${PLOTS_PREFIX}.ChangeSubHighlightPlotView`;
export const CHANGE_PLOT_ATTRIBUTE = `${PLOTS_PREFIX}.ChangePlotAttribute`;
/** Action Type: display mode to or from expanded */
export const CHANGE_EXPANDED_MODE = `${PLOTS_PREFIX}.changeExpandedMode`;
/** Action Type: turn on/off expanded auto-play */
export const EXPANDED_AUTO_PLAY = `${PLOTS_PREFIX}.expandedAutoPlay`;
/** Action Type: change the primary plot for a multi image fits display */
export const CHANGE_PRIME_PLOT = `${PLOTS_PREFIX}.changePrimePlot`;
export const CHANGE_IMAGE_VISIBILITY = `${PLOTS_PREFIX}.changeImageVisibility`;
export const CHANGE_MOUSE_READOUT_MODE = `${PLOTS_PREFIX}.changeMouseReadoutMode`;
/** Action Type: delete a plotView */
export const DELETE_PLOT_VIEW = `${PLOTS_PREFIX}.deletePlotView`;
export const BYTE_DATA_REFRESH = `${PLOTS_PREFIX}.byteDataRefresh`;
export const PLOT_MASK_START = `${PLOTS_PREFIX}.plotMaskStart`;
/** Action Type: add a mask image*/
export const PLOT_MASK = `${PLOTS_PREFIX}.plotMask`;
export const PLOT_MASK_LAZY_LOAD = `${PLOTS_PREFIX}.plotMaskLazyLoad`;
export const PLOT_MASK_FAIL = `${PLOTS_PREFIX}.plotMaskFail`;
export const DELETE_OVERLAY_PLOT = `${PLOTS_PREFIX}.deleteOverlayPlot`;
export const OVERLAY_PLOT_CHANGE_ATTRIBUTES = `${PLOTS_PREFIX}.overlayPlotChangeAttributes`;
export const CHANGE_HIPS_IMAGE_CONVERSION = `${PLOTS_PREFIX}.changeHipsImageConversion`;
export const CHANGE_TABLE_AUTO_SCROLL = `${PLOTS_PREFIX}.changeTableAutoScroll`;
export const USE_TABLE_AUTO_SCROLL = `${PLOTS_PREFIX}.useTableAutoScroll`;
export const REQUEST_LOCAL_DATA = `${PLOTS_PREFIX}.requestLocalData`;
/** Action Type: enable/disable wcs matching*/
export const WCS_MATCH = `${PLOTS_PREFIX}.wcsMatch`;
export const PLOT_PROGRESS_UPDATE = `${PLOTS_PREFIX}.PlotProgressUpdate`;
export const API_TOOLS_VIEW = `${PLOTS_PREFIX}.apiToolsView`;


// ----------------------------------------
// actions use by MultiViewCntlr
// ----------------------------------------

export const ADD_VIEWER = `${IMAGE_MULTI_VIEW_PREFIX}.AddViewer`;
export const REMOVE_VIEWER = `${IMAGE_MULTI_VIEW_PREFIX}.RemoveViewer`;
export const VIEWER_MOUNTED = `${IMAGE_MULTI_VIEW_PREFIX}.viewMounted`;
export const VIEWER_UNMOUNTED = `${IMAGE_MULTI_VIEW_PREFIX}.viewUnmounted`;
export const VIEWER_SCROLL = `${IMAGE_MULTI_VIEW_PREFIX}.viewScroll`;
export const BOTTOM_UI_COMPONENT = `${IMAGE_MULTI_VIEW_PREFIX}.bottomUIComponent`;
export const ADD_VIEWER_ITEMS = `${IMAGE_MULTI_VIEW_PREFIX}.addViewerItems`;
export const REMOVE_VIEWER_ITEMS = `${IMAGE_MULTI_VIEW_PREFIX}.removeViewerItems`;
export const REPLACE_VIEWER_ITEMS = `${IMAGE_MULTI_VIEW_PREFIX}.replaceViewerItems`;
export const CHANGE_VIEWER_LAYOUT = `${IMAGE_MULTI_VIEW_PREFIX}.changeViewerLayout`;
export const UPDATE_VIEWER_CUSTOM_DATA = `${IMAGE_MULTI_VIEW_PREFIX}.updateViewerCustomData`;
export const ADD_TO_AUTO_RECEIVER = `${IMAGE_MULTI_VIEW_PREFIX}.addToAutoReceiver`;


// ----------------------------------------
// actions use by DrawLayerCntlr
// ----------------------------------------
export const CREATE_DRAWING_LAYER = `${DRAWLAYER_PREFIX}.createDrawLayer`;
export const UPDATE_DRAWING_LAYER = `${DRAWLAYER_PREFIX}.updateDrawLayer`;
export const DESTROY_DRAWING_LAYER = `${DRAWLAYER_PREFIX}.destroyDrawLayer`;
export const CHANGE_VISIBILITY = `${DRAWLAYER_PREFIX}.changeVisibility`;
export const CHANGE_DRAWING_DEF = `${DRAWLAYER_PREFIX}.changeDrawingDef`;
export const ATTACH_LAYER_TO_PLOT = `${DRAWLAYER_PREFIX}.attachLayerToPlot`;
export const DETACH_LAYER_FROM_PLOT = `${DRAWLAYER_PREFIX}.detachLayerFromPlot`;
export const MODIFY_CUSTOM_FIELD = `${DRAWLAYER_PREFIX}.modifyCustomField`;
export const FORCE_DRAW_LAYER_UPDATE = `${DRAWLAYER_PREFIX}.forceDrawLayerUpdate`;

// PointSelection.js
export const SELECT_POINT = `${DRAWLAYER_PREFIX}.SelectPoint.selectPoint`;
export const EXTRACT_POINT = `${DRAWLAYER_PREFIX}.ExtractPoints.extractPoint`;

// Distance tool
export const DT_START = `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolStart`;
export const DT_MOVE = `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolMove`;
export const DT_END = `${DRAWLAYER_PREFIX}.DistanceTool.distanceToolEnd`;

// Extraction Line tool
export const ELT_START = `${DRAWLAYER_PREFIX}.ExtractLineTool.distanceToolStart`;
export const ELT_MOVE = `${DRAWLAYER_PREFIX}.ExtractLineTool.distanceToolMove`;
export const ELT_END = `${DRAWLAYER_PREFIX}.ExtractLineTool.distanceToolEnd`;

// select
export const SELECT_AREA_START = `${DRAWLAYER_PREFIX}.SelectArea.selectAreaStart`;
export const SELECT_AREA_MOVE = `${DRAWLAYER_PREFIX}.SelectArea.selectAreaMove`;
export const SELECT_AREA_END = `${DRAWLAYER_PREFIX}.SelectArea.selectAreaEnd`;
export const SELECT_MOUSE_LOC = `${DRAWLAYER_PREFIX}.SelectArea.selectMouseLoc`;

// marker and footprint
export const MARKER_START = `${DRAWLAYER_PREFIX}.MarkerTool.markerStart`;
export const MARKER_MOVE = `${DRAWLAYER_PREFIX}.MarkerTool.markerMove`;
export const MARKER_END = `${DRAWLAYER_PREFIX}.MarkerTool.markerEnd`;
export const MARKER_CREATE = `${DRAWLAYER_PREFIX}.MarkerTool.markerCreate`;
export const FOOTPRINT_CREATE = `${DRAWLAYER_PREFIX}.FootprintTool.footprintCreate`;
export const FOOTPRINT_START = `${DRAWLAYER_PREFIX}.FootprintTool.footprintStart`;
export const FOOTPRINT_END = `${DRAWLAYER_PREFIX}.FootprintTool.footprintEnd`;
export const FOOTPRINT_MOVE = `${DRAWLAYER_PREFIX}.FootprintTool.footprintMove`;

// region
export const REGION_CREATE_LAYER = `${DRAWLAYER_PREFIX}.RegionPlot.createLayer`;
export const REGION_DELETE_LAYER = `${DRAWLAYER_PREFIX}.RegionPlot.deleteLayer`;
export const REGION_ADD_ENTRY = `${DRAWLAYER_PREFIX}.RegionPlot.addRegion`;
export const REGION_REMOVE_ENTRY = `${DRAWLAYER_PREFIX}.RegionPlot.removeRegion`;
export const REGION_SELECT = `${DRAWLAYER_PREFIX}.RegionPlot.selectRegion`;

export const IMAGELINEBASEDFP_CREATE = `${DRAWLAYER_PREFIX}.ImageLineBasedFP.imagelineBasedFPCreate`;


/** @typedef GroupingScope
 * enum can be one of
 * @prop STANDARD
 * @prop SUBGROUP
 * @prop GROUP
 * @type {Enum}
 */

/** @type GroupingScope */
export const GroupingScope = new Enum(['STANDARD', 'SUBGROUP', 'SINGLE']);

/** @typedef ExpandType
 * enum can be one of
 * @prop COLLAPSE
 * @prop GRID
 * @prop SINGLE
 * @type {Enum}
 */

/** @type ExpandType */
export const ExpandType = new Enum(['COLLAPSE', 'GRID', 'SINGLE']);

/**
 * @typedef {Object} WcsMatchType
 * enum can be one of
 * @prop Standard
 * @prop Target
 * @prop Pixel
 * @prop PixelCenter
 * @type {Enum}
 */

/** @type WcsMatchType */
export const WcsMatchType = new Enum(['Standard', 'Target', 'Pixel', 'PixelCenter']);

/**
 * @typedef ActionScope
 * enum can be one of
 * @prop GROUP
 * @prop SINGLE
 * @prop LIST
 * @type {Enum}
 * @public
 * @global
 */

/** @type ActionScope */
export const ActionScope = new Enum(['GROUP', 'SINGLE', 'LIST']);


/**
 * @typedef NewPlotMode
 * enum one of
 * @prop create_replace
 * @prop replace_only
 * @prop none
 * @type {Enum}
 */

/** @type NewPlotMode */
export const NewPlotMode = new Enum(['create_replace', 'replace_only', 'none']);


// ----------------------------------------
// Enums use by Zoom functions
// ----------------------------------------

/**
 * @typedef ZoomType
 * @summary zoom type
 * @description can be 'STANDARD', 'LEVEL', 'FULL_SCREEN', 'TO_WIDTH_HEIGHT', 'TO_WIDTH', 'TO_HEIGHT', 'ARCSEC_PER_SCREEN_PIX'
 * @prop STANDARD
 * @prop LEVEL
 * @prop FULL_SCREEN
 * @prop TO_WIDTH_HEIGHT
 * @prop TO_WIDTH
 * @prop TO_HEIGHT
 * @prop ARCSEC_PER_SCREEN_PIX
 * @type {Enum}
 * @public
 * @global
 */


/** @type ZoomType */
export const ZoomType = new Enum([
    'STANDARD',       // use normal zoom, zoom to given zoom level or 1x if not specified
    'LEVEL',       // use normal zoom, zoom to given zoom level or 1x if not specified
    'FULL_SCREEN',       // requires width & height specified. deprecated, same as TO_WIDTH_HEIGHT
    'TO_WIDTH_HEIGHT',   // requires width & height specified
    'TO_WIDTH',          // requires width
    'TO_HEIGHT',         // requires height, not yet implemented
    'ARCSEC_PER_SCREEN_PIX' // arcsec
]);

/**
 * @typedef UserZoomTypes
 * can be 'UP','DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'
 * @prop UP,
 * @prop DOWN,
 * @prop FIT,
 * @prop FILL,
 * @prop ONE,
 * @prop LEVEL,
 * @prop WCS_MATCH_PREV,
 * @type {Enum}
 * @public
 * @global
 */

/** @type UserZoomTypes */
export const UserZoomTypes = new Enum(['UP', 'DOWN', 'FIT', 'FILL', 'ONE', 'LEVEL', 'WCS_MATCH_PREV'], {ignoreCase: true}); // _- Distance tool


/**
 * @typedef FullType
 * enum can be one of
 * @prop ONLY_WIDTH
 * @prop WIDTH_HEIGHT
 * @prop ONLY_HEIGHT
 * @prop {Function} has
 * @type {Enum}
 */


/** @type FullType */
export const FullType = new Enum(['ONLY_WIDTH', 'WIDTH_HEIGHT', 'ONLY_HEIGHT']);