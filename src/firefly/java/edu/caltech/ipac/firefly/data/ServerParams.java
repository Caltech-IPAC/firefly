/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

/**
 * @author Trey Roby
 */
public class ServerParams {

    // params
    public static final String COMMAND = "cmd";
    public static final String DO_JSONP = "doJsonp";
    public static final String RED_REQUEST = "red";
    public static final String GREEN_REQUEST = "green";
    public static final String BLUE_REQUEST = "blue";
    public static final String NOBAND_REQUEST = "noband";
    public static final String REQUEST = "request";
    public static final String SAVE_KEY = "saveKey";
    public static final String CLIENT_REQUEST = "clientRequest";
    public static final String WAIT_MILS = "waitMils";
    public static final String STATE = "state";
    public static final String PROGRESS_KEY = "progressKey";
    public static final String LEVEL = "level";
    public static final String FULL_SCREEN = "fullScreen";
    public static final String IMAGE_PT1 = "ImagePt1";
    public static final String FILE_AND_HEADER = "fah";
    public static final String WPT = "wpt";
    public static final String PT = "pt";
    public static final String PT1 = "pt1";
    public static final String PT2 = "pt2";
    public static final String PT3 = "pt3";
    public static final String PT4 = "pt4";
    public static final String PTARY = "ptAry";
    public static final String WPT_ARY = "wptAry";
    public static final String WL_ARY = "wlAry";
    public static final String FLUX_UNIT= "fluxUnit";
    public static final String WL_UNIT= "wlUnit";
    public static final String PLANE = "plane";
    public static final String CRO_MULTI_ALL = "cropMultiAll";
    public static final String STRETCH_DATA = "stretchData";
    public static final String BAND = "band";
    public static final String IDX = "idx";
    public static final String COLOR_IDX = "idx";
    public static final String COLOR= "color";
    public static final String COL_NAME= "colName";
    public static final String NORTH = "north";
    public static final String ANGLE = "angle";
    public static final String ROTATE = "rotate";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String CTXSTR = "ctx";
    public static final String USER_AGENT = "userAgent";
    public static final String DRAW_INFO = "drawInfo";
    public static final String SOURCE = "source";
    public static final String ALT_SOURCE = "alt_source";
    public static final String OBJ_NAME = "objName";
    public static final String RESOLVER = "resolver";
    public static final String ID = "id";
    public static final String BID = "bid";
    public static final String CHANNEL_ID = "channelID";
    public static final String TRY_MS = "tryMS";
    public static final String PLOT_ID = "plotId";
    public static final String POLLING = "polling";
    public static final String EMAIL = "email";
    public static final String ATTRIBUTE = "attribute";
    public static final String FILE = "file";
    public static final String PLOT_EXTERNAL = "PlotExternal";
    public static final String FILE_KEY = "fileKey";
    public static final String REGION_DATA = "regionData";
    public static final String ROWS = "rows";
    public static final String REQUESTED_DATA_SET = "RequestedDataSet";
    public static final String DATA_TYPE= "DataType";
    public static final String DATA= "Data";
    public static final String DESC= "Desc";
    public static final String SPACIAL_TYPE= "SpacialType";
    public static final String URL= "URL";
    public static final String IP_ADDRESS= "ipAddress";
    public static final String SCROLL_X= "scrollX";
    public static final String SCROLL_Y= "scrollY";
    public static final String ZOOM_FACTOR= "zoomFactor";
    public static final String RANGE_VALUES= "rangeValues";

    public static final String EXT_TYPE= "extType";
    public static final String IMAGE= "image";
    public static final String TOOL_TIP=  "toolTip";
    public static final String DS9_REGION_DATA= "ds9RegionData";
    public static final String BIT_NUMBER= "bitNumber";
    public static final String IMAGE_NUMBER= "imageNumber";
    public static final String BIT_DESC= "bitDesc";

    public static final String DOWNLOAD_REQUEST = "downloadRequest";
    public static final String SELECTION_INFO = "selectionInfo";
    public static final String SORT_ORDER= "sortOrder";
    public static final String IMAGE_SOURCES= "imageSources";
    public static final String EXTERNAL = "external";
    public static final String IRSA = "irsa";
    public static final String LSST = "lsst";
    public static final String ALL = "all";
    public static final String CDS = "cds";
    public static final String HIPS_SOURCES = "hipsSources";
    public static final String HIPS_LIST_SOURCE= "hipsListSource";
    public static final String HIPS_LIST_SOURCE_NAME= "hipsListSourceName";
    public static final String ENSURE_SOURCE= "ensureSource";
    public static final String ADHOC_SOURCE = "adhocSource";
    public static final String HIPS_DATATYPES = "hipsDataTypes";
    public static final String HIPS_MERGE_PRIORITY = "mergedListPriority";
    public static final String HIPS_TABLE_TYPE= "hipsTableType";
    public static final String CUBE = "cube";
    public static final String CATALOG = "catalog";
    public static final String RADIX = "radix";

    public static final String GEOSHAPE = "shape";
    public static final String ROTATION = "rotation";
    public static final String NAIFID_FORMAT = "naifIdFormat";

    // commands
    public static final String FILE_FLUX = "CmdFileFlux";
    public static final String FILE_FLUX_JSON = "CmdFileFluxJson";
    public static final String CREATE_PLOT = "CmdCreatePlot";
    public static final String CREATE_PLOT_GROUP = "CmdCreatePlotGroup";
    public static final String FLIP_Y = "CmdFlipY";
    public static final String HISTOGRAM = "CmdHistogram";
    public static final String CROP = "CmdCrop";
    public static final String STAT = "CmdStat";
    public static final String GET_USER_INFO = "CmdGetUserInfo";
    public static final String GET_ALERTS = "CmdAlerts";

    public static final String JSON_DATA = "JsonData";
    public static final String RESOLVE_NAME= "CmdResolveName";
    public static final String RESOLVE_NAIFID = "CmdResolveNaifid";
    public static final String TABLE_SEARCH_SPATIAL_BINARY = "TableSearchSpatialBinary";


    // Background Job related
    public static final String JOB_ID= "jobId";
    public static final String ADD_JOB = "addBgJob";
    public static final String REMOVE_JOB = "removeBgJob";
    public static final String CANCEL= "cancel";
    public static final String SET_EMAIL= "setEmail";
    public static final String RESEND_EMAIL= "resendEmail";
    public static final String CREATE_DOWNLOAD_SCRIPT= "createDownoadScript";
    public static final String UWS_JOB_INFO= "uwsJobInfo";

    public static final String GET_CAPABILITIES= "getCapabilities";
    public static final String REPORT_USER_ACTION= "reportUserAction";
    public static final String DS9_REGION= "ds9Region";
    public static final String SAVE_DS9_REGION= "saveDS9Region";
    public static final String ADD_SAVED_REQUEST= "addSavedRequest";
//    public static final String GET_ALL_SAVED_REQUEST= "getAllSavedRequest";
    public static final String TITLE= "Title";
    public static final String JSON_DEEP= "jsonDeep";
    public static final String CLIENT_IS_NORTH= "clientIsNorth";
    public static final String CLIENT_ROT_ANGLE= "clientRotAngle";
    public static final String CLIENT_FlIP_Y= "clientFlipY";
    public static final String ACTION= "action";
    public static final String PROP= "prop";

    public static final String VIS_PUSH_ALIVE_CHECK= "pushAliveCheck";
    public static final String VIS_PUSH_ALIVE_COUNT= "pushAliveCount";
    public static final String VIS_PUSH_ACTION= "pushAction";
    public static final String GET_IMAGE_MASTER_DATA= "getImageMasterData";
    public static final String GET_FLOAT_DATA= "getFloatData";
    public static final String GET_BYTE_DATA= "getStretchedByteData";
    public static final String FITS_EXTRACTION= "fitsExtraction";

    public static final String USER_TARGET_WORLD_PT = "UserTargetWorldPt";

    public static final String PACKAGE_REQUEST = "packageRequest";
    public static final String TABLE_SEARCH = "tableSearch";
    public static final String QUERY_TABLE = "queryTable";
    public static final String SELECTED_VALUES = "selectedValues";
    public static final String TABLE_SAVE = "tableSave";
    public static final String UPLOAD = "upload";
    public static final String ADD_OR_UPDATE_COLUMN = "addOrUpdateColumn";
    public static final String DELETE_COLUMN = "deleteColumn";

    public static final String INIT_APP = "CmdInitApp";
    public static final String JSON_PROPERTY= "CmdJsonProperty";
    public static final String LOGOUT = "CmdLogout";
    public static final String TILE_SIZE = "tileSize";
    public static final String DATA_COMPRESS = "dataCompress";
    public static final String POINT_SIZE= "pointSize";
    public static final String POINT_SIZE_X= "pointSizeX";
    public static final String POINT_SIZE_Y= "pointSizeY";
    public static final String COMBINE_OP= "combineOp";
    public static final String AXIS= "axis";
    public static final String EXTRACTION_TYPE= "extractionType";
    public static final String EXTRACTION_FLOAT_SIZE= "extractionFloatSize";
    public static final String HDU_NUM= "hduNum";
    public static final String RELATED_HDUS= "relatedHDUs";
    public static final String BACK_TO_URL= "backToUrl";
    public static final String MASK_DATA= "maskData";
    public static final String MASK_BITS= "maskBits";

    //Workspaces
    public static final String WS_LIST = "wsList"; // Gets the list of content/files
    public static final String WS_GET_FILE = "wsGet"; // TODO
    public static final String WS_UPLOAD_FILE = "wsUpload";// to get the path of the file uploaded into firefly server from WS
    public static final String WS_PUT_IMAGE_FILE = "wsPut"; //FITS, PNG, comes from edu.caltech.ipac.firefly.server.servlets.AnyFileDownload
    public static final String WS_PUT_TABLE_FILE = "wsPutTable";
    public static final String WS_DELETE_FILE = "wsDel";
    public static final String WS_MOVE_FILE = "wsMove";
    public static final String WS_GET_METADATA = "wsGetMeta";
    public static final String WS_CREATE_FOLDER = "wsParent";
    public static final String SOURCE_FROM = "sourceFrom";
    public static final String IS_WS = "isWs";
    public static final String FILE_ANALYSIS = "fileAnalysis";
}

