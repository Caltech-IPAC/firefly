/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * User: roby
 * Date: 2/7/12
 * Time: 10:14 AM
 */


/**
 * @author Trey Roby
 */

/**
 * List of constants that are used as the names of parameters passed to the server.
 */
const ServerParams = {
        COMMAND : 'cmd',
        DO_JSONP : 'doJsonp',
        RED_REQUEST : 'red',
        GREEN_REQUEST : 'green',
        BLUE_REQUEST : 'blue',
        NOBAND_REQUEST : 'noband',
        REQUEST : 'request',
        SAVE_KEY : 'saveKey',
        CLIENT_REQUEST : 'clientRequest',
        WAIT_MILS : 'waitMils',
        STATE : 'state',
        PROGRESS_KEY : 'progressKey',
        LEVEL : 'level',
        FULL_SCREEN : 'fullScreen',
        IMAGE_PT1 : 'ImagePt1',
        FILE_AND_HEADER : 'fah',
        PT : 'pt',
        PT1 : 'pt1',
        PT2 : 'pt2',
        PT3 : 'pt3',
        PT4 : 'pt4',
        CRO_MULTI_ALL : 'cropMultiAll',
        STRETCH_DATA : 'stretchData',
        BAND : 'band',
        IDX : 'idx',
        COLOR_IDX : 'idx',
        COL_NAME: 'colName',
        NORTH : 'north',
        ANGLE : 'angle',
        ROTATE : 'rotate',
        WIDTH : 'width',
        HEIGHT : 'height',
        CTXSTR : 'ctx',
        USER_AGENT : 'userAgent',
        DRAW_INFO : 'drawInfo',
        SOURCE : 'source',
        ALT_SOURCE : 'alt_source',
        OBJ_NAME : 'objName',
        RESOLVER : 'resolver',
        ID : 'id',
        BID : 'bid',
        CHANNEL_ID : 'channelID',
        PLOT_ID : 'plotId',
        POLLING : 'polling',
        EMAIL : 'email',
        ATTRIBUTE : 'attribute',
        FILE : 'file',
        PLOT_EXTERNAL : 'PlotExternal',
        FILE_KEY : 'fileKey',
        REGION_DATA : 'regionData',
        ROWS : 'rows',
        STATIC_JSON_DATA : 'staticJsonData',
        REQUESTED_DATA_SET : 'RequestedDataSet',
        DATA_TYPE: 'DataType',
        DATA: 'Data',
        DESC: 'Desc',
        SPACIAL_TYPE: 'SpacialType',
        URL: 'URL',
        IP_ADDRESS: 'ipAddress',
        SCROLL_X: 'scrollX',
        SCROLL_Y: 'scrollY',
        ZOOM_FACTOR: 'zoomFactor',
        RANGE_VALUES: 'rangeValues',

        EXT_TYPE: 'extType',
        IMAGE: 'image',
        TOOL_TIP:  'toolTip',
        DS9_REGION_DATA: 'ds9RegionData',




        // commands
        FILE_FLUX : 'CmdFileFlux',
        FILE_FLUX_JSON : 'CmdFileFluxJson',
        CREATE_PLOT : 'CmdCreatePlot',
        CREATE_PLOT_GROUP : 'CmdCreatePlotGroup',
        ZOOM : 'CmdZoom',
        STRETCH : 'CmdStretch',
        ADD_BAND : 'CmdAddBand',
        REMOVE_BAND : 'CmdRemoveBand',
        CHANGE_COLOR : 'CmdChangeColor',
        ROTATE_NORTH : 'CmdRotateNorth',
        ROTATE_ANGLE : 'CmdRotateAngle',
        FLIP_Y : 'CmdFlipY',
        HISTOGRAM : 'CmdHistogram',
        IMAGE_PNG : 'CmdImagePng',
        CROP : 'CmdCrop',
        STAT : 'CmdStat',
        HEADER : 'CmdHeader',
        DELETE : 'CmdDelete',
        USER_KEY : 'CmdUserKey',
        VERSION : 'CmdVersion',
        RAW_DATA_SET : 'RawDataSet',
        JSON_DATA : 'JsonData',
        CHK_FILE_STATUS : 'CmdChkFileStatus',
        GET_ENUM_VALUES : 'getEnumValues',
        RESOLVE_NAME: 'CmdResolveName',
        PROGRESS : 'CmdProgress',
        SUB_BACKGROUND_SEARCH: 'subBackgroundSearch',
        GET_STATUS: 'status',
        CANCEL: 'cancel',
        ADD_ID_TO_CRITERIA: 'addIDToCriteria',
        CLEAN_UP: 'cleanup',
        DOWNLOAD_PROGRESS: 'downloadProgress',
        GET_DATA_FILE_VALUES: 'getDataFileValues',
        SET_EMAIL: 'setEmail',
        SET_ATTR: 'setAttribute',
        GET_EMAIL: 'getEmail',
        RESEND_EMAIL: 'resendEmail',
        CLEAR_PUSH_ENTRY: 'clearPushEntry',
        REPORT_USER_ACTION: 'reportUserAction',
        CREATE_DOWNLOAD_SCRIPT: 'createDownoadScript',
        DS9_REGION: 'ds9Region',
        SAVE_DS9_REGION: 'saveDS9Region',
        ADD_SAVED_REQUEST: 'addSavedRequest',
        GET_ALL_SAVED_REQUEST: 'getAllSavedRequest',
        TITLE: 'Title',
        JSON_DEEP: 'jsonDeep',

        VIS_PUSH_CREATE_ID: 'createID',
        VIS_PUSH_FITS: 'pushFits',
        VIS_PUSH_REG: 'pushRegion',
        VIS_PUSH_REMOVE_REG: 'pushRemoveRegion',
        VIS_PUSH_REG_DATA: 'pushRegionData',
        VIS_PUSH_REMOVE_REG_DATA: 'pushRemoveRegionData',
        VIS_PUSH_TABLE: 'pushTable',
        VIS_PUSH_EXT: 'pushExt',
        VIS_QUERY_ACTION: 'queryAction',
        VIS_PUSH_WPR: 'pushWPR',
        VIS_PUSH_ALIVE_CHECK: 'pushAliveCheck',
        VIS_PUSH_PAN: 'pushPan',
        VIS_PUSH_ZOOM: 'pushZoom',
        VIS_PUSH_RANGE_VALUES: 'pushRangeValues',


        USER_TARGET_WORLD_PT : 'UserTargetWorldPt'
};

export default ServerParams;
