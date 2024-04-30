/* eslint prefer-template:0 */
import {get, isString, isFunction, isPlainObject, isArray, join, omit, pick, isObject, isUndefined} from 'lodash';
import Enum from 'enum';
import {ServerRequest} from '../data/ServerRequest.js';
import {RequestType} from './RequestType.js';
import {ZoomType} from './ZoomType.js';
import {parseResolver} from '../astro/net/Resolver.js';
import {RangeValues} from './RangeValues.js';
import {PlotAttribute} from './PlotAttribute.js';
import CoordinateSys from 'firefly/visualize/CoordSys.js';


const DEFAULT_IMAGE_OVERLAYS= ['ACTIVE_TARGET_TYPE','POINT_SELECTION_TYPE', 'NORTH_UP_COMPASS_TYPE',
    'WEB_GRID_TYPE', 'OVERLAY_MARKER_TYPE', 'OVERLAY_FOOTPRINT_TYPE', 'REGION_PLOT_TYPE',
    ];

const DEFAULT_HIPS_OVERLAYS= ['ACTIVE_TARGET_TYPE','POINT_SELECTION_TYPE', 'NORTH_UP_COMPASS_TYPE',
    'OVERLAY_MARKER_TYPE', 'OVERLAY_FOOTPRINT_TYPE', 'REGION_PLOT_TYPE', 'HIPS_GRID_TYPE', 'MOC_PLOT_TYPE'];

/**
 * @typedef ServiceType
 * @summary service type
 * @description can be 'IRIS', 'ISSA', 'DSS', 'SDSS', 'TWOMASS', 'MSX', 'WISE', 'ATLAS','ZTF', 'PTF', 'UNKNOWN'
 *
 * @prop IRIS
 * @prop ISSA
 * @prop DSS
 * @prop SDSS
 * @prop TWOMASS
 * @prop MSX
 * @prop WISE
 * @prop ATLAS
 * @prop ZTF
 * @prop PTF
 * @prop UNKNOWN
 *
 * @type {Enum}
 * @public
 * @global
 */
export const ServiceType= new Enum(['IRIS', 'ISSA', 'DSS', 'SDSS', 'TWOMASS', 'MSX', 'WISE', 'ATLAS', 'ZTF', 'PTF', 'UNKNOWN'],
                                              { ignoreCase: true });
/**
 * @typedef {Object} TitleOptions
 * @summary title options
 * @description can be 'CLEARED', 'PLOT_DESC', 'FILE_NAME', 'HEADER_KEY', 'PLOT_DESC_PLUS', 'SERVICE_OBS_DATE'
 *
 * @prop NONE
 * @prop CLEARED
 * @prop PLOT_DESC
 * @prop FILE_NAME
 * @prop HEADER_KEY
 * @prop PLOT_DESC_PLUS
 * @prop SERVICE_OBS_DATE'
 *
 * @type {Enum}
 * @public
 * @global
 */
export const TitleOptions= new Enum([
    'NONE',  // use what it in the title
    'PLOT_DESC', // use the plot description key
    'FILE_NAME', // use the file name or analyze the URL and make a title from that
    'HEADER_KEY', // use the header value
    'PLOT_DESC_PLUS', // ??
    'SERVICE_OBS_DATE'
], { ignoreCase: true });

/**
 * @typedef {Object} AnnotationOps
 * @prop INLINE
 * @prop INLINE_BRIEF
 *
 * @type {Enum}
 */

/** @type AnnotationOps*/
export const AnnotationOps= new Enum([
    'INLINE',    //default inline title full title and tools
    'INLINE_BRIEF',  // inline brief title, details
], { ignoreCase: true });

/**
 * @typedef {Object} GridOnStatus
 * @prop FALSE
 * @prop TRUE
 * @prop TRUE_LABELS_FALSE
 * @type {Enum}
 */
export const GridOnStatus= new Enum(['FALSE','TRUE','TRUE_LABELS_FALSE'], { ignoreCase: true });

export const DEFAULT_THUMBNAIL_SIZE= 70;
export const WEB_PLOT_REQUEST_CLASS= 'WebPlotRequest';

export const WPConst= {
    FILE : 'File',
    WORLD_PT : 'WorldPt',
    URLKEY : 'URL',
    SIZE_IN_DEG : 'SizeInDeg',
    SURVEY_KEY : 'SurveyKey',
    SURVEY_KEY_BAND : 'SurveyKeyBand',
    TYPE : 'Type',
    ZOOM_TYPE : 'ZoomType',
    SERVICE : 'Service',
    USER_DESC : 'UserDesc',
    INIT_ZOOM_LEVEL : 'InitZoomLevel',
    TITLE : 'Title',
    ROTATE_NORTH : 'RotateNorth',
    ROTATE : 'Rotate',
    ROTATION_ANGLE : 'RotationAngle',
    HEADER_KEY_FOR_TITLE : 'HeaderKeyForTitle',
    INIT_RANGE_VALUES : 'RangeValues',
    INIT_COLOR_TABLE : 'ColorTable',
    MULTI_IMAGE_IDX : 'MultiImageIdx',
    MULTI_IMAGE_EXTS: 'MultiImageExts',
    ZOOM_ARCSEC_PER_SCREEN_PIX : 'ZoomArcsecPerScreenPix',
    OBJECT_NAME : 'ObjectName',
    RESOLVER : 'Resolver',
    PROGRESS_KEY : 'ProgressKey',
    FLIP_Y : 'FlipY',
    THUMBNAIL_SIZE : 'thumbnailSize',
    URL_CHECK_FOR_NEWER: 'urlCheckForNewer',
    MASK_REQUIRED_WIDTH: 'MaskRequiredWidth',
    MASK_REQUIRED_HEIGHT: 'MaskRequiredHeight',
    FILTER: 'filter',

// keys - client side operations
    PREFERENCE_COLOR_KEY : PlotAttribute.PREFERENCE_COLOR_KEY,  //todo make this only a attribute
    GRID_ON : 'GridOn',
    OVERLAY_POSITION : 'OverlayPosition',
    PLOT_GROUP_ID: 'plotGroupId',
    GROUP_LOCKED: 'GroupLocked',
    DRAWING_SUB_GROUP_ID: 'DrawingSubgroupID',
    DOWNLOAD_FILENAME_ROOT : 'DownloadFileNameRoot',
    PLOT_ID : 'plotId',
    OVERLAY_IDS: 'PredefinedOverlayIds',
    HIPS_ROOT_URL: 'hipsRootUrl',
    HIPS_SURVEYS_ID: 'hipsSurveysId',
    HIPS_USE_AITOFF_PROJECTION: 'hipsUseAitoffProjection',
    HIPS_USE_COORDINATE_SYS: 'hipsCoordinateSys',
    HIPS_ADDITIONAL_MOC_LIST: 'hipsAdditionalMocList',

    ANNOTATION_OPS : 'AnnotationOps',
    TITLE_OPTIONS : 'TitleOptions',
    POST_TITLE: PlotAttribute.POST_TITLE,  //todo make this only a attribute
    PRE_TITLE: PlotAttribute.PRE_TITLE,   //todo make this only a attribute

    MASK_BITS: 'MaskBits',
    PLOT_AS_MASK: 'PlotAsMask',
    MASK_COLORS: 'MaskColors',

};


const plotAttKeys= Object.values(PlotAttribute);
const plotAttKeysEnum= new Enum([...plotAttKeys]);
const paramKeys= Object.values(WPConst);
const allKeys= new Enum([...paramKeys, ...plotAttKeys ], { ignoreCase: true });


const clientSideKeys = [WPConst.PREFERENCE_COLOR_KEY, WPConst.GRID_ON,
         WPConst.TITLE_OPTIONS, WPConst.POST_TITLE, WPConst.PRE_TITLE, WPConst.OVERLAY_POSITION,
         WPConst.PLOT_GROUP_ID, WPConst.DRAWING_SUB_GROUP_ID,
         WPConst.DOWNLOAD_FILENAME_ROOT, WPConst.PLOT_ID, WPConst.GROUP_LOCKED, WPConst.OVERLAY_IDS
        ];


let defColorTable= 0;

export const setDefaultImageColorTable= (ct) => defColorTable=ct;
export const getDefaultImageColorTable= () => defColorTable;

const defParams= {
    RequestClass: WEB_PLOT_REQUEST_CLASS,
    [WPConst.INIT_COLOR_TABLE]: getDefaultImageColorTable,
};

/**
 * @summary Web plot request. This object can be created by using the method of making survey request like *makeXXXRequest*
 * and the method of setting the parameters.
 * @param {RequestType} type request type
 * @param {string} userDesc description
 * @param {ServiceType} serviceType service type if type == RequestType.SERVICE
 *
 * @public
 * @global
 */
export class WebPlotRequest extends ServerRequest {
    constructor(type,userDesc,serviceType) {
        super(type);
        type && this.setRequestType(type);
        (serviceType && serviceType!==ServiceType.UNKNOWN) && this.setServiceType(serviceType);
        userDesc && this.setParam(WPConst.USER_DESC, userDesc);
        Object.entries(defParams).forEach(([k,v]) => this.setParam(k,isFunction(v) ? v():v));
    }

//======================================================================
//----------- Factory Methods for various types of request    ----------
//----------- Most of the time it is better to use these than ----------
//----------- the constructors                                ----------
//======================================================================
    static makeFromObj(obj) {
        if (!obj) return null;
        if (isString(obj)) {
            return WebPlotRequest.parse(obj);
        }
        else if (isPlainObject(obj)) {
            const wpr= new WebPlotRequest();

            wpr.setAttributes(obj.attributes);
            wpr.setParams(cleanupObj(obj));


            let typeGuess;
            if (obj.id) typeGuess= RequestType.PROCESSOR;
            else if (obj[WPConst.FILE]) typeGuess= RequestType.FILE;
            else if (obj[WPConst.SURVEY_KEY]) typeGuess= RequestType.SERVICE;
            else if (obj[WPConst.SERVICE])   typeGuess= RequestType.SERVICE;
            else if (obj[WPConst.URLKEY]) typeGuess= RequestType.URL;
            else if (obj[WPConst.HIPS_ROOT_URL]) typeGuess= RequestType.HiPS;

            if (typeGuess && !wpr.params[WPConst.TYPE]) wpr.setRequestType(typeGuess);

            // setting safe urls
            if (wpr.params[WPConst.URLKEY]) wpr.setURL(wpr.params[WPConst.URLKEY]);
            if (wpr.params[WPConst.HIPS_ROOT_URL]) wpr.setHipsRootUrl(wpr.params[WPConst.HIPS_ROOT_URL]);

            return wpr;
        }
        else if (obj.makeCopy) { // in this case I was probably passed a WebPlotRequest
           return obj.makeCopy();
        }
        else { // i don't know what I have, just return it
            return obj;
        }
    }

    static makeFilePlotRequest(fileName, initZoomLevel) {
        const req = new WebPlotRequest(RequestType.FILE, 'Fits file: ' + fileName);
        req.setParam(WPConst.FILE, fileName);
        req.setTitleOptions(TitleOptions.FILE_NAME);
        if (initZoomLevel) req.setParam(WPConst.INIT_ZOOM_LEVEL, initZoomLevel + '');
        return req;
    }

    static makeProcessorRequest(serverRequest, desc) {
        const req = new WebPlotRequest(RequestType.PROCESSOR, desc);
        req.setParams(serverRequest.getParams());
        return req;
    }

    static makeURLPlotRequest(url, userDesc) {
        const req = new WebPlotRequest(RequestType.URL, userDesc||'Fits from URL: ' + url);
        req.setTitleOptions(TitleOptions.FILE_NAME);
        req.setURL(url);
        return req;
    }

    static makeWorkspaceRequest(filePath, userDesc) {
        const req = new WebPlotRequest(RequestType.WORKSPACE, userDesc||filePath);
        req.setTitleOptions(TitleOptions.FILE_NAME);
        req.setFileName(filePath);
        return req;
    };

    //======================== ISSA =====================================
    /**
     *
     * @param worldPt
     * @param {string} survey  must be one of '12','25','60','100'
     * @param sizeInDeg less then 12.5
     * @return {WebPlotRequest}
     */
    static makeISSARequest(worldPt, survey, sizeInDeg, ) {
        return this.makePlotServiceReq(ServiceType.ISSA, worldPt, survey, sizeInDeg, 'ISSA '+survey, 'iras');
    }

    //======================== IRIS =====================================
    /**
     * 
     * @param worldPt
     * @param {string} survey  must be one of '12','25','60','100'
     * @param sizeInDeg less then 12.5
     * @return {WebPlotRequest}
     */
    static makeIRISRequest(worldPt, survey, sizeInDeg) {
        return this.makePlotServiceReq(ServiceType.IRIS, worldPt, survey, sizeInDeg, 'IRIS '+survey, 'iras');
    }


    //======================== 2MASS =====================================
    /**
     * @param wp
     * @param {string} survey  must be one of 'asky', 'askyw', 'sx', 'sxw', 'cal'
     * @param {string} band  must be one of 'j','h','k'
     * @param sizeInDeg less then .138 degrees (500 arcsec)
     * @return {WebPlotRequest}
     */
    static make2MASSRequest(wp, survey, band, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.TWOMASS, wp, survey, sizeInDeg, '2MASS '+band.toUpperCase(),'2mass');
        req.setParam(WPConst.SURVEY_KEY_BAND, band + '');
        return req;
    }

    //======================== MSX =====================================
    /**
     * @param wp
     * @param survey must be  '3','4','5','6'
     *          for 'A (8.28 microns)', 'C (12.13 microns)', 'D (14.65 microns)', 'E (21.3 microns)'
     *
     * @param sizeInDeg .1 to 1.5
     * @return {WebPlotRequest}
     */
    static makeMSXRequest(wp, survey, sizeInDeg) {
        return this.makePlotServiceReq(ServiceType.MSX, wp, survey, sizeInDeg, 'MSX '+survey, 'msx');
    }

    //======================== SDSS =====================================
    /**
     * @param wp
     * @param band  one of: 'u' 'g' 'r' 'i' 'z'
     * @param sizeInDeg .016 to .25
     * @return {WebPlotRequest}
     */
    static makeSloanDSSRequest(wp, band, sizeInDeg) {
        return this.makePlotServiceReq(ServiceType.SDSS, wp, band, sizeInDeg, 'SDSS '+band, 'sdss');
    }

    //======================== DSS =====================================
    /**
     *
     * @param wp
     * @param {string} survey must be one of : poss2ukstu_red poss2ukstu_ir poss2ukstu_blue poss1_red poss1_blue quickv phase2_gsc2 phase2_gsc1
     * @param sizeInDeg .016 to .25
     * @return {WebPlotRequest}
     */
    static makeDSSRequest(wp, survey, sizeInDeg) {
        return this.makePlotServiceReq(ServiceType.DSS, wp, survey, sizeInDeg, `DSS ${survey}`, 'dss');
    }

    //======================== Wise =====================================
    /**
     *
     * @param wp
     * @param survey can be Atlas, 3a, 1b
     * @param band 1,2,3,4
     * @param sizeInDeg  .01 to 3
     * @return {WebPlotRequest}
     */
    static makeWiseRequest(wp, survey, band, sizeInDeg) {
        const req = this.makePlotServiceReq(ServiceType.WISE, wp, survey, sizeInDeg,undefined, 'wise');
        req.setParam(WPConst.SURVEY_KEY_BAND, band + '');
        const sDesc= survey.toLowerCase()==='3a' ? 'Atlas' : survey;
        req.setTitle('WISE: '+sDesc+ ', B'+ band);
        return req;
    }

    //======================== ZTF =====================================
    /**
     *
     * @param wp
     * @param {string} survey  'ref'
     * @param {string} band  must be one of 'zg', 'zr'
     * @param sizeInDeg
     * @return {WebPlotRequest}
     */
    static makeZTFRequest(wp, survey, band, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.ZTF, wp, survey, sizeInDeg, 'ZTF '+band.toUpperCase(), 'ztf');
        req.setParam(WPConst.SURVEY_KEY_BAND, band + '');
        return req;
    }

    //======================== PTF =====================================
    /**
     *
     * @param wp
     * @param {string} survey 'level2'
     * @param {string} band  must be one of '1' '2'
     * @param sizeInDeg
     * @return {WebPlotRequest}
     */
    static makePTFRequest(wp, survey, band, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.PTF, wp, survey, sizeInDeg, 'PTF '+band.toUpperCase(), 'ptf');
        req.setParam(WPConst.SURVEY_KEY_BAND, band + '');
        return req;
    }

    //======================== Atlas =====================================
    /**
     *
     * @param wp
     * @param survey any atlas combination tables: schema.table, i.e. 'spitzer.seip_science'
     * @param band any atlas 'band_name' column, i.e 'IRAC2' (which is 2.4 microns channel from IRAC instrument)
     * @param filter extra filter for particular table, such as 'file_type = 'science' and fname like '%.mosaic.fits' or 'file_type = 'science' and principal=1'
     * @param sizeInDeg
     * @return {WebPlotRequest}
     */
    static makeAtlasRequest(wp, survey, band, filter, sizeInDeg) {
        const req = this.makePlotServiceReq(ServiceType.ATLAS, wp, survey, sizeInDeg, survey + ',' + band);
        req.setParam(WPConst.SURVEY_KEY, survey);
        req.setParam(WPConst.FILTER, filter); //Needed for the query but not for fetching the data (see QueryIBE metadata)
        req.setParam(WPConst.SURVEY_KEY_BAND, band + '');
        return req;
    }

    //======================== All Sky =====================================
    static makeAllSkyPlotRequest() { return new WebPlotRequest(RequestType.ALL_SKY, 'All Sky Image'); }

    //======================== HiPS =====================================
    static makeHiPSRequest(rootUrl, wp= undefined, sizeInDeg= 180) {
        const r= new WebPlotRequest(RequestType.HiPS, '');
        r.setHipsRootUrl(rootUrl);
        if (wp) r.setWorldPt(wp);
        r.setSizeInDeg(sizeInDeg);
        return r;
    }

//======================================================================
//----------------------- Title Settings -------------------------------
//======================================================================

    setTitle(title) { this.setParam(WPConst.TITLE, title); }

    getTitle() { return this.getParam(WPConst.TITLE); }

    getUserDesc() { return this.getParam(WPConst.USER_DESC); }

    /**
     * @param option TitleOptions
     */
    setTitleOptions(option) { this.setParam(WPConst.TITLE_OPTIONS,option.key); }

    /** @return {TitleOptions} */
    getTitleOptions() {
        return TitleOptions.get(this.getParam(WPConst.TITLE_OPTIONS)) || TitleOptions.NONE;
    }

    /**
     * @param option HeaderDecorationOps
     */
    setAnnotationOps(option) { this.setParam(WPConst.ANNOTATION_OPS,option.key); }

    /**
     * @return {AnnotationOps}
     */
    getAnnotationOps() {
        return AnnotationOps.get(this.getParam(WPConst.ANNOTATION_OPS)) || AnnotationOps.INLINE;
    }

//======================================================================
//----------------------- Overlay Settings ------------------------------
//======================================================================

    /**
     * @param {WorldPt|String} worldPt - the world point object or a serialized version
     */
    setOverlayPosition(worldPt) { this.setParam(WPConst.OVERLAY_POSITION, worldPt ? worldPt.toString() : false); }

    /**
     * @return {WorldPt}
     */
    getOverlayPosition() { return this.getWorldPtParam(WPConst.OVERLAY_POSITION); }


//======================================================================
//----------------------- Color Settings ------------------------------
//======================================================================

    /**
     * @param {int} id integer, color table id number
     */
    setInitialColorTable(id) { this.setParam(WPConst.INIT_COLOR_TABLE, id + ''); }

    /** * @return {int} color table id number */
    getInitialColorTable() { return this.getIntParam(WPConst.INIT_COLOR_TABLE,defColorTable); }

    /**
     *
     * @param rangeValues RangeValues
     */
    setInitialRangeValues(rangeValues) {
        if (rangeValues) {
            this.setParam(WPConst.INIT_RANGE_VALUES, rangeValues.toJSON());
        }
        else {
            this.removeParam(WPConst.INIT_RANGE_VALUES);
        }
    }

    /** @return {RangeValues} */
    getInitialRangeValues() {
        return this.containsParam(WPConst.INIT_RANGE_VALUES) ? RangeValues.parse(this.getParam(WPConst.INIT_RANGE_VALUES)) : null;
    }

//======================================================================
//----------------------- Zoom Settings ------------------------------
//======================================================================

    /**
     * set the initialize zoom level, this is used with ZoomType.LEVEL
     *
     * @param {number} zl the zoom level, float
     * @see {ZoomType}
     */
    setInitialZoomLevel(zl) {
        this.setParam(WPConst.INIT_ZOOM_LEVEL, zl + '');
    }

    /** @return {number}, the zoom level */
    getInitialZoomLevel() {
        return this.getFloatParam(WPConst.INIT_ZOOM_LEVEL,1);
    }

    /**
     * sets the zoom type, based on the ZoomType other zoom set methods may be required
     * Notes for ZoomType:
     * <ul>
     * <li>ZoomType.LEVEL is the default when there is not width and height, when set you may optionally call
     * setInitialZoomLevel the zoom will default to be 1x</li>
     * <li>if ZoomType.TO_WIDTH then you must call setZoomToWidth and set a width </li>
     * <li>if ZoomType.FULL_SCREEN then you must call setZoomToWidth with a width and
     * setZoomToHeight with a height</li>
     * <li>if ZoomType.ARCSEC_PER_SCREEN_PIX then you must call setZoomArcsecPerScreenPix</li>
     * </ul>
     *
     * @param {ZoomType} zoomType affect how the zoom is computed
     * @see ZoomType
     */
    setZoomType(zoomType) { if (zoomType) this.setParam(WPConst.ZOOM_TYPE, zoomType.key); }

    getZoomType() {
        return ZoomType.get(this.getParam(WPConst.ZOOM_TYPE)) || ZoomType.TO_WIDTH_HEIGHT;
    }

    /**
     * set the arcseconds per screen pixel that will be used to determine the zoom level.
     * Used with ZoomType.ARCSEC_PER_SCREEN_PIX
     *
     * @param {number} arcsecSize
     * @see ZoomType
     */
    setZoomArcsecPerScreenPix(arcsecSize) {
        this.setParam(WPConst.ZOOM_ARCSEC_PER_SCREEN_PIX, arcsecSize + '');
    }

    getZoomArcsecPerScreenPix() {
        return this.containsParam(WPConst.ZOOM_ARCSEC_PER_SCREEN_PIX) ?
            this.getFloatParam(WPConst.ZOOM_ARCSEC_PER_SCREEN_PIX) : 0;
    }

//======================================================================
//----------------------- Rotate  & Flip Settings ---------------------
//  these have become pure client settings. We strip then out when we
//  call the server
//======================================================================
    /**
     * Plot should come up rotated north
     *
     * @param {boolean} rotateNorth true to rotate
     */
    setRotateNorth(rotateNorth) { this.setParam(WPConst.ROTATE_NORTH, rotateNorth + ''); }

    /** @return {boolean} */
    getRotateNorth() { return this.getBooleanParam(WPConst.ROTATE_NORTH); }

    /**
     * set to rotate, if true, the angle should also be set
     * @param rotate boolean, true to rotate
     */
    setRotate(rotate) { this.setParam(WPConst.ROTATE, rotate + ''); }

    /**
     * @return boolean,  true if rotate, false otherwise
     */
    getRotate() { return this.getBooleanParam(WPConst.ROTATE); }

    /**
     * set the angle to rotate to
     * @param rotationAngle  number, the angle in degrees to rotate to
     */
    setRotationAngle(rotationAngle) { this.setParam(WPConst.ROTATION_ANGLE, rotationAngle + ''); }

    /**
     * @return number, the angle
     */
    getRotationAngle() { return this.getFloatParam(WPConst.ROTATION_ANGLE); }

    /**
     * set if this image should be flipped on the Y axis
     * todo- deprecate- this is done on the client now
     * @param flipY boolean, true to flip, false not to flip
     */
    setFlipY(flipY) { this.setParam(WPConst.FLIP_Y,flipY+''); }

//======================================================================
//----------------------- Retrieval Settings --------------------------------
//======================================================================

    /**
     * plot the file name that exist on the server
     *
     * @param fileName the file name on the server
     */
    setFileName(fileName) { this.setParam(WPConst.FILE, fileName); }

    getFileName() { return this.getParam(WPConst.FILE); }

    /**
     * retrieve and plot the file from the specified URL
     *
     * @param url the URL where the file resides
     */
    setURL(url) { this.setSafeParam(WPConst.URLKEY, url); }

    getURL() { return this.getSafeParam(WPConst.URLKEY); }

    setURLCheckForNewer(check) { this.setSafeParam(WPConst.URL_CHECK_FOR_NEWER, check+''); }
    getURLCheckForNewer() { return this.getBooleanParam(WPConst.URL_CHECK_FOR_NEWER); }

    /**
     *
     * @param service ServiceType
     */
    setServiceType(service) {
        if (!service) return;
        if (isString(service)) this.setParam(WPConst.SERVICE, service);
        else if (service.key) this.setParam(WPConst.SERVICE, service.key);
        else this.setParam(WPConst.SERVICE, service.toString());
    }

    /**
     *
     * @return ServiceType
     */
    getServiceType() {
        return ServiceType.get(this.getParam(WPConst.SERVICE)) ||ServiceType.UNKNOWN;
    }

    /**
     * should only be call if getServiceType returns UNKNOWN
     */
    getServiceTypeString() {
        return this.getParam(WPConst.SERVICE);
    }

    /**
     * @return RequestType
     */
    getRequestType() { return RequestType.get(this.getParam(WPConst.TYPE)) ||RequestType.FILE; }

    /**
     * Set the type of request. This parameter is required for every call.  The factory methods will always
     * set the Request type.
     *
     * @param type the RequestType
     * @see RequestType
     */
    setRequestType(type) { this.setParam(WPConst.TYPE, type.key); }


    /**
     * @param {string} key string
     */
    setSurveyKey(key) { this.setParam(WPConst.SURVEY_KEY, key); }

    /**
     * @return {string} key string
     */
    getSurveyKey() { return this.getParam(WPConst.SURVEY_KEY); }

    /**
     * @return {string} key string
     */
    getSurveyBand() { return this.getParam(WPConst.SURVEY_KEY_BAND); }

//======================================================================
//----------------------- Object & Area Settings -----------------------
//======================================================================

    /**
     * @param {string} objectName string astronomical object to search
     */
    setObjectName(objectName) { this.setParam(WPConst.OBJECT_NAME, objectName); }

    /**
     * @return {string} astronomical object, string
     */
    getObjectName() { return this.getParam(WPConst.OBJECT_NAME); }

    /**
     * @param resolver Resolver, name resolver type
     */
    setResolver(resolver) { this.setParam(WPConst.RESOLVER, resolver.toString()); }

    /**
     * @return Resolver, name resolver type
     */
    getResolver() { return parseResolver(this.getParam(WPConst.RESOLVER)); }

    /**
     *
     * @param {WorldPt|undefined} worldPt WorldPt
     */
    setWorldPt(worldPt) { if (worldPt) this.setParam(WPConst.WORLD_PT, worldPt.toString()); }

    /**
     * @return {WorldPt} WorldPt
     */
    getWorldPt() { return this.getWorldPtParam(WPConst.WORLD_PT); }

    setSizeInDeg(sizeInDeg) { this.setParam(WPConst.SIZE_IN_DEG, sizeInDeg + ''); }
    getSizeInDeg() { return this.getFloatParam(WPConst.SIZE_IN_DEG, NaN); }

//======================================================================
//----------------------- Other Settings --------------------------------
//======================================================================


    setMultiImageIdx(idx) { this.setParam(WPConst.MULTI_IMAGE_IDX, idx + ''); }

    /**
     * image extension list. ex: '3,4,5' for extension 3, 4, 5
     * @param idxS
     */
    setMultiImageExts(idxS) { this.setParam(WPConst.MULTI_IMAGE_EXTS, idxS); }

    /**
     * @param {GridOnStatus|boolean} gridOnStatus GridOnStatus
     */
    setGridOn(gridOnStatus= GridOnStatus.FALSE) {
        const stat= GridOnStatus.get( String(gridOnStatus) );
        this.setParam(WPConst.GRID_ON, stat.key);
    }

    /**
     * @return GridOnStatus
     */
    getGridOn() { return GridOnStatus.get(this.getParam(WPConst.GRID_ON)) ||GridOnStatus.FALSE; }

    /**
     * @param thumbnailSize int
     */
    setThumbnailSize(thumbnailSize) { this.setParam(WPConst.THUMBNAIL_SIZE, thumbnailSize+''); }

    /**
     * @return int
     */
    getThumbnailSize() { return this.getIntParam(WPConst.THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE); }

    setHeaderKeyForTitle(headerKey) { this.setParam(WPConst.HEADER_KEY_FOR_TITLE, headerKey); }
    getHeaderKeyForTitle() { return this.getParam(WPConst.HEADER_KEY_FOR_TITLE); }

    setRequestKey(key) { this.setParam(WPConst.PROGRESS_KEY,key); }  // alias of setProgressKey
    getRequestKey() { return this.getParam(WPConst.PROGRESS_KEY); } // alias of getProgressKey

    setDrawingSubGroupId(id) { this.setParam(WPConst.DRAWING_SUB_GROUP_ID,id); }
    getDrawingSubGroupId() { return this.getParam(WPConst.DRAWING_SUB_GROUP_ID); }

    setDownloadFileNameRoot(nameRoot) { this.setParam(WPConst.DOWNLOAD_FILENAME_ROOT, nameRoot); }
    getDownloadFileNameRoot() { return this.getParam(WPConst.DOWNLOAD_FILENAME_ROOT); }

    setPlotId(id) { this.setParam(WPConst.PLOT_ID,id); }
    getPlotId() { return this.getParam(WPConst.PLOT_ID); }

    setPlotGroupId(id) { this.setParam(WPConst.PLOT_GROUP_ID,id); }
    getPlotGroupId() { return this.getParam(WPConst.PLOT_GROUP_ID); }

    setGroupLocked(locked) { this.setParam(WPConst.GROUP_LOCKED,locked+''); }
    isGroupLocked() { return this.getBooleanParam(WPConst.GROUP_LOCKED,true); }


    setMaskBits(idx) { this.setParam(WPConst.MASK_BITS,idx+''); }
    getMaskBits() { return this.containsParam(WPConst.MASK_BITS) ? this.getIntParam(WPConst.MASK_BITS) : 0;}

    setPlotAsMask(plotAsMask) { this.setParam(WPConst.PLOT_AS_MASK, plotAsMask+''); }
    isPlotAsMask() { return this.getBooleanParam(WPConst.PLOT_AS_MASK); }


    setMaskColors(colors) {
        if (isArray(colors)) {
            this.setParam(WPConst.MASK_COLORS, join(colors, ';'));
        }
        else {
            this.setParam(WPConst.MASK_COLORS, colors);
        }
    }

    getMaskColors() {
        const retList= [];
        if (this.containsParam(WPConst.MASK_COLORS)) {
            return this.getParam(WPConst.MASK_COLORS).split(';');
        }
        return retList;
    }

    setMaskRequiredWidth(width) { this.setParam(WPConst.MASK_REQUIRED_WIDTH, width+''); }

    setMaskRequiredHeight(height) { this.setParam(WPConst.MASK_REQUIRED_HEIGHT, height+''); }

    setHipsRootUrl(url) { this.setSafeParam(WPConst.HIPS_ROOT_URL, url);}
    getHipsRootUrl() { return this.getSafeParam(WPConst.HIPS_ROOT_URL);}


    setHipsUseAitoffProjection(useAitoff) { this.setParam(WPConst.HIPS_USE_AITOFF_PROJECTION, Boolean(useAitoff));}
    getHipsUseAitoffProjection() { return this.getBooleanParam(WPConst.HIPS_USE_AITOFF_PROJECTION);}

    setHipsUseCoordinateSys(csys) {
        if (csys!==CoordinateSys.EQ_J2000 && csys!==CoordinateSys.GALACTIC) return;
        this.setParam(WPConst.HIPS_USE_COORDINATE_SYS, csys.toString());
    }

    getHipsUseCoordinateSys() {
        return CoordinateSys.parse(this.getParam(WPConst.HIPS_USE_COORDINATE_SYS));
    }

    /**
     *
     * @param overlayIdList
     */
    setOverlayIds(overlayIdList) { this.setParam(WPConst.OVERLAY_IDS, join(overlayIdList, ';')); }

    /**
     * @return {Array.<String>} array of string DrawLayerType IDs
     */
    getOverlayIds() {
        if (!isUndefined(this.params[WPConst.OVERLAY_IDS])) {
            return this.getParam(WPConst.OVERLAY_IDS).split(';').filter((s) => s);
        }
        else {
            return this.getRequestType()!==RequestType.HiPS ? DEFAULT_IMAGE_OVERLAYS : DEFAULT_HIPS_OVERLAYS;
        }
    }

    /**
     * Return the request area
     * i am using circle but it is really size not radius - todo: fix this
     *
     * @return an area to select
     */
    getRequestArea() {
        let retval = null;
        const wp= this.getWorldPt();
        const side = this.getSizeInDeg();
        if (wp) retval = {center:wp, radius:side};
        return retval;
    }

    toStringServerSideOnly() {
        const retReq= this.makeCopy();
        clientSideKeys.forEach( (k) => retReq.params[k]= undefined);
        return retReq.toString();
    }

    /**
     * Set an attribute that will be included in the WebPlot.attributes object
     * @param {object} attObj - an object with name and string values that will be added to the attributes
     * @see PlotAttribute a set of predefined attribute names
     */
    setAttributes(attObj) {
        if (!attObj || !isPlainObject(attObj)) return;
        // add each attribute in attObj
        //      - if it is know PlotAttribute key then convert case insensitive keys to proper form.
        Object.keys(attObj).forEach( (k) => {
            this.setParam(get(plotAttKeysEnum.get(k), 'key',k),attObj[k].toString());
        });
        this.attributeCache= null;
    }

    /**
     * @return {Object} an object with all the attributes
     * @see PlotAttribute a set of predefined attribute names
     */
    getAttributes() {
        // The return is computed as follows:
        //    - all keys that are not part of the stand param keys
        //    - and all keys the are plot attribute keys.
        //    - this allows some param keys to be treated at attributes
        if (!this.attributeCache) {
            this.attributeCache= {...omit(this.params, paramKeys), ...pick(this.params, plotAttKeys)};
        }
        return this.attributeCache;
    }


    /**
    * This method can only be used for those services that take the standard
    * ra, dec, radius approach (so far, iras, issa, 2mass, dss)
    *
    * @param {ServiceType} serviceType the network service type
    * @param {WorldPt} wp the center point WorldPt
    * @param {string} survey
    * @param {number} sizeInDeg size in degrees
    * @param {string} [title] the title of the plot
    * @param {string} [subGroup] - the subgroup Id
    *
    * @return {WebPlotRequest} the PlotRequest object that was constructed
    */
    static makePlotServiceReq(serviceType, wp, survey, sizeInDeg, title, subGroup) {
        const desc = `${serviceType.key}: ${survey}, ${sizeInDeg} Deg`;
        const req = new WebPlotRequest(RequestType.SERVICE, desc, serviceType);
        req.setSurveyKey(survey);
        req.setWorldPt(wp);
        req.setSizeInDeg(sizeInDeg);
        if (title) req.setTitle(title);
        if (subGroup) req.setDrawingSubGroupId(subGroup);
        return req;
    }

    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     *
     * @param {string} str the serialized WebPlotRequest
     * @return (WebPlotRequest) the deserialized WebPlotRequest
     */
    static parse(str) { return ServerRequest.parseAndAdd(str, new WebPlotRequest()); }

    static isWPR(o) {return isObject(o) && o.getRequestClass?.()===WEB_PLOT_REQUEST_CLASS;}

    /**
     * @param {Object} [overrideParams] params to replace in the copy
     * @return {WebPlotRequest}
     */
    makeCopy(overrideParams=undefined) {
        const retval = new WebPlotRequest();
        retval.copyFrom(this);
        if (overrideParams) retval.setParams(cleanupObj(overrideParams));
        return retval;
    }
}

export default WebPlotRequest;

/**
 * Create a new object with the keys more consistent with the keys defined in WebPlotRequest.
 * if a case insensitive version of the key exist then replace it with the proper one, otherwise
 * use the key. This will also remove the attributes object.
 * The loop looks up the key with case insensitive matching, if it does not exist it uses the original key
 * @param {Object} r - plain object
 * @return {Object}
 */
function cleanupObj(r) {
    const retVal= Object.keys(r).reduce( (obj,k) => {
        obj[get(allKeys.get(k), 'key',k)]= r[k]; // note - uses both lodash.get and enum.get
        return obj;
    },{});
    return omit(retVal, ['attributes']);
}

/**
 * find all the invalid WebPlotRequest keys.  
 * @param {Object} r an object literal to evaluate
 * @return {String[]} an array of invalid keys
 */
export function findInvalidWPRKeys(r) {
    return [...Object.keys(r),...plotAttKeys].filter( (k) => !Boolean(allKeys.get(k)));
}

/**
 * take a plain object plot request and clean it up has a minimum required 
 * to be valid
 * @param {Object|Array} request
 * @param global
 * @param fallbackGroupId
 * @param {Function} makePlotId make a plot id
 * @return {*}
 */
export function confirmPlotRequest(request,global={},fallbackGroupId,makePlotId) {
    if (isArray(request)) {
        let locked= true;
        const idx= request.findIndex( (r) => r.plotId);

        const plotId= (idx>=0) ? request[idx].plotId : makePlotId();
        let plotGroupId;

        if (idx>=0  && request[idx].plotGroupId) {
            plotGroupId= request[idx].plotGroupId;
            locked= true;
        }
        else {
            plotGroupId= fallbackGroupId;
            locked= false;
        }

        return request.map( (r) => ({...global,...r,plotId,plotGroupId,GroupLocked:locked}));
    }
    else {
        const r= {...global, ...request};
        if (!r.plotId) r.plotId= makePlotId();
        if (!r.plotGroupId) r.plotGroupId= fallbackGroupId;
        if (r.plotGroupId===fallbackGroupId) r.GroupLocked= false;
        return r;
    }
}

function makeDataOnlyRequestString(r) {
    if (!r) return '';
    r= r.makeCopy();
    r.setRequestKey('');
    r.setPlotId('');
    r.setPlotGroupId('');
    r.setInitialRangeValues();
    r.setInitialColorTable(getDefaultImageColorTable());
    r.setAnnotationOps(AnnotationOps.INLINE);
    r.setRotateNorth(false);
    r.setRotate(0);
    return r.toString();
}

/**
 * Make simplified versions of the WebPlotRequest and compare them.
 * This function takes out a lot of parameters that are not related to resolving the fits file.  It attempts see
 * if we are requesting the same fits file or fits file services.
 * @param {WebPlotRequest} r1
 * @param {WebPlotRequest} r2
 */
export const isImageDataRequestedEqual= (r1,r2) => makeDataOnlyRequestString(r1)===makeDataOnlyRequestString(r2);
