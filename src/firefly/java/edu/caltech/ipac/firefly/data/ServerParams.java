/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 2/7/12
 * Time: 10:14 AM
 */


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
    public static final String PT = "pt";
    public static final String PT1 = "pt1";
    public static final String PT2 = "pt2";
    public static final String PT3 = "pt3";
    public static final String PT4 = "pt4";
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
    public static final String STATIC_JSON_DATA = "staticJsonData";
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






    // commands
    public static final String FILE_FLUX = "CmdFileFlux";
    public static final String FILE_FLUX_JSON = "CmdFileFluxJson";
    public static final String CREATE_PLOT = "CmdCreatePlot";
    public static final String CREATE_PLOT_GROUP = "CmdCreatePlotGroup";
    public static final String ZOOM = "CmdZoom";
    public static final String STRETCH = "CmdStretch";
    public static final String GET_BETA = "CmdGetBeta";
    public static final String REMOVE_BAND = "CmdRemoveBand";
    public static final String CHANGE_COLOR = "CmdChangeColor";
    public static final String ROTATE_NORTH = "CmdRotateNorth";
    public static final String ROTATE_ANGLE = "CmdRotateAngle";
    public static final String FLIP_Y = "CmdFlipY";
    public static final String HISTOGRAM = "CmdHistogram";
    public static final String IMAGE_PNG = "CmdImagePng";
    public static final String IMAGE_PNG_REG = "CmdImagePngReg";
    public static final String CROP = "CmdCrop";
    public static final String STAT = "CmdStat";
    public static final String HEADER = "CmdHeader";
    public static final String FITS_HEADER = "CmdFitsHeader";  //LZ 3/21/16  DM-4494

    public static final String DELETE = "CmdDelete";
    public static final String USER_KEY = "CmdUserKey";
    public static final String VERSION = "CmdVersion";
    public static final String RAW_DATA_SET = "RawDataSet";
    public static final String JSON_DATA = "JsonData";
    public static final String CHK_FILE_STATUS = "CmdChkFileStatus";
    public static final String GET_ENUM_VALUES = "getEnumValues";
    public static final String RESOLVE_NAME= "CmdResolveName";
    public static final String PROGRESS = "CmdProgress";
    public static final String SUB_BACKGROUND_SEARCH= "subBackgroundSearch";
    public static final String GET_STATUS= "status";
    public static final String ADD_JOB = "addBgJob";
    public static final String REMOVE_JOB = "removeBgJob";
    public static final String CANCEL= "cancel";
    public static final String ADD_ID_TO_CRITERIA= "addIDToCriteria";
    public static final String CLEAN_UP= "cleanup";
    public static final String DOWNLOAD_PROGRESS= "downloadProgress";
    public static final String SET_EMAIL= "setEmail";
    public static final String SET_ATTR= "setAttribute";
    public static final String GET_EMAIL= "getEmail";
    public static final String RESEND_EMAIL= "resendEmail";
    public static final String CLEAR_PUSH_ENTRY= "clearPushEntry";
    public static final String REPORT_USER_ACTION= "reportUserAction";
    public static final String CREATE_DOWNLOAD_SCRIPT= "createDownoadScript";
    public static final String DS9_REGION= "ds9Region";
    public static final String SAVE_DS9_REGION= "saveDS9Region";
    public static final String ADD_SAVED_REQUEST= "addSavedRequest";
    public static final String GET_ALL_SAVED_REQUEST= "getAllSavedRequest";
    public static final String TITLE= "Title";
    public static final String JSON_DEEP= "jsonDeep";
    public static final String CLIENT_IS_NORTH= "clientIsNorth";
    public static final String CLIENT_ROT_ANGLE= "clientRotAngle";
    public static final String CLIENT_FlIP_Y= "clientFlipY";
    public static final String ACTION= "action";

    public static final String VIS_PUSH_ALIVE_CHECK= "pushAliveCheck";
    public static final String VIS_PUSH_ALIVE_COUNT= "pushAliveCount";
    public static final String VIS_PUSH_ACTION= "pushAction";
    public static final String GET_IMAGE_MASTER_DATA= "getImageMasterData";


    public static final String USER_TARGET_WORLD_PT = "UserTargetWorldPt";

    public static final String PACKAGE_REQUEST = "packageRequest";
    public static final String TABLE_SEARCH = "tableSearch";
    public static final String QUERY_TABLE = "queryTable";
    public static final String SELECTED_VALUES = "selectedValues";
    public static final String TABLE_SAVE = "tableSave";
    public static final String UPLOAD = "upload";
    public static final String JSON_SEARCH = "jsonSearch";

    public static final String INIT_APP = "CmdInitApp";
    public static final String LOGOUT = "CmdLogout";

    //Workspaces
    public static final String WS_LIST = "wsList";
    public static final String WS_GET_FILE = "wsGet";
    public static final String WS_PUT_FILE = "wsPut";
}

