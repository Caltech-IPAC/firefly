/**
 * User: roby
 * Date: Apr 2, 2009
 * Time: 9:18:47 AM
 */
/* eslint prefer-template:0 */
import {get, isString, isPlainObject, isArray, join, has} from 'lodash';
import {ServerRequest, ID_NOT_DEFINED} from '../data/ServerRequest.js';
import {RequestType} from './RequestType.js';
import {ZoomType} from './ZoomType.js';
import Enum from 'enum';
import CoordinateSys from './CoordSys.js';
import Point, {parseImagePt} from './Point.js';
import {parseResolver} from '../astro/net/Resolver.js';
import {RangeValues} from './RangeValues.js';


export const ServiceType= new Enum(['IRIS', 'ISSA', 'DSS', 'SDSS', 'TWOMASS', 'MSX', 'DSS_OR_IRIS', 'WISE', 'NONE'],
                                              { ignoreCase: true });
export const TitleOptions= new Enum([
    'NONE',  // use what it in the title
    'PLOT_DESC', // use the plot description key
    'FILE_NAME', // use the file name or analyze the URL and make a title from that
    'HEADER_KEY', // use the header value
    'PLOT_DESC_PLUS', // ??
    'SERVICE_OBS_DATE'
], { ignoreCase: true });

export const AnnotationOps= new Enum([
    'INLINE',    //default inline title full title and tools
    'INLINE_BRIEF',  // inline brief title, no tools
    'INLINE_BRIEF_TOOLS', // brief title w/ tools
    'TITLE_BAR', // title full title and tools
    'TITLE_BAR_BRIEF', // title bar brief title, no tools
    'TITLE_BAR_BRIEF_TOOLS', // title bar brief with w/ tools
    'TITLE_BAR_BRIEF_CHECK_BOX' // title bar brief title with a check box (used in planck)
], { ignoreCase: true });



export const ExpandedTitleOptions= new Enum([ 'REPLACE',// use expanded title when expanded
                                     'PREFIX',// use expanded title as prefix to title
                                     'SUFFIX'// use expanded title as suffix to title
                                   ], { ignoreCase: true });
export const GridOnStatus= new Enum(['FALSE','TRUE','TRUE_LABELS_FALSE'], { ignoreCase: true });

export const DEFAULT_THUMBNAIL_SIZE= 70;
const WEB_PLOT_REQUEST_CLASS= 'WebPlotRequest';
const Order= new Enum(['FLIP_Y', 'FLIP_X', 'ROTATE', 'POST_CROP', 'POST_CROP_AND_CENTER'], { ignoreCase: true });


const C= {
    FILE : 'File',
    WORLD_PT : 'WorldPt',
    URLKEY : 'URL',
    SIZE_IN_DEG : 'SizeInDeg',
    SURVEY_KEY : 'SurveyKey',
    SURVEY_KEY_ALT : 'SurveyKeyAlt',
    SURVEY_KEY_BAND : 'SurveyKeyBand',
    TYPE : 'Type',
    ZOOM_TYPE : 'ZoomType',
    SERVICE : 'Service',
    USER_DESC : 'UserDesc',
    INIT_ZOOM_LEVEL : 'InitZoomLevel',
    TITLE : 'Title',
    ROTATE_NORTH : 'RotateNorth',
    ROTATE_NORTH_TYPE : 'RotateNorthType',
    ROTATE : 'Rotate',
    ROTATE_FROM_NORTH : 'RotateFromNorth',
    ROTATION_ANGLE : 'RotationAngle',
    HEADER_KEY_FOR_TITLE : 'HeaderKeyForTitle',
    INIT_RANGE_VALUES : 'RangeValues',
    INIT_COLOR_TABLE : 'ColorTable',
    MULTI_IMAGE_IDX : 'MultiImageIdx',
    MULTI_IMAGE_EXTS: 'MultiImageExts',
    ZOOM_TO_WIDTH : 'ZoomToWidth',
    ZOOM_TO_HEIGHT : 'ZoomToHeight',
    ZOOM_ARCSEC_PER_SCREEN_PIX : 'ZoomArcsecPerScreenPix',
    POST_CROP : 'PostCrop',
    POST_CROP_AND_CENTER : 'PostCropAndCenter',
    POST_CROP_AND_CENTER_TYPE : 'PostCropAndCenterType',
    CROP_PT1 : 'CropPt1',
    CROP_PT2 : 'CropPt2',
    CROP_WORLD_PT1 : 'CropWorldPt1',
    CROP_WORLD_PT2 : 'CropWorldPt2',
    UNIQUE_KEY : 'UniqueKey',
    CONTINUE_ON_FAIL : 'ContinueOnFail',
    OBJECT_NAME : 'ObjectName',
    RESOLVER : 'Resolver',
    PLOT_DESC_APPEND : 'PlotDescAppend',
    BLANK_ARCSEC_PER_PIX : 'BlankArcsecPerScreenPix',  //todo: doc
    BLANK_PLOT_WIDTH : 'BlankPlotWidth',               //todo: doc
    BLANK_PLOT_HEIGHT : 'BlankPlotHeight',             //todo: doc
    PROGRESS_KEY : 'ProgressKey',
    FLIP_X : 'FlipX',
    FLIP_Y : 'FlipY',
    HAS_MAX_ZOOM_LEVEL : 'HasMaxZoomLevel',
    THUMBNAIL_SIZE : 'thumbnailSize',
    PIPELINE_ORDER : 'pipelineOrder',
    URL_CHECK_FOR_NEWER: 'urlCheckForNewer',

    MULTI_PLOT_KEY: 'MultiPlotKey',
    THREE_COLOR_PLOT_KEY: 'ThreeColorPlotKey',
    THREE_COLOR_HINT: 'ThreeColorHint',
    RED_HINT: 'RedHint',
    GREEN_HINT: 'GreenHint',
    BLUE_HINT: 'BlueHint',

// keys - client side operations
// note- if you add a new key make sure you put it in the _allKeys array
    PLOT_TO_DIV : 'PlotToDiv',
    PREFERENCE_COLOR_KEY : 'PreferenceColorKey',
    PREFERENCE_ZOOM_KEY : 'PreferenceZoomKey',
    ROTATE_NORTH_SUGGESTION : 'RotateNorthSuggestion',
    SAVE_CORNERS : 'SaveCornersAfterPlot',
    SHOW_SCROLL_BARS : 'showScrollBars',  // todo deprecate
    ALLOW_IMAGE_SELECTION : 'AllowImageSelection',
    HAS_NEW_PLOT_CONTAINER : 'HasNewPlotContainer',
    GRID_ON : 'GridOn',
    OVERLAY_POSITION : 'OverlayPosition',
    MINIMAL_READOUT: 'MinimalReadout',
    PLOT_GROUP_ID: 'plotGroupId',
    GROUP_LOCKED: 'GroupLocked',
    DRAWING_SUB_GROUP_ID: 'DrawingSubgroupID',
    //GRID_ID : 'GRID_ID', - deprecated
    DOWNLOAD_FILENAME_ROOT : 'DownloadFileNameRoot',
    PLOT_ID : 'plotId',
    OVERLAY_IDS: 'PredefinedOverlayIds',
    RELATED_TABLE_ROW : 'RELATED_TABLE_ROW',



    //SHOW_TITLE_AREA : 'ShowTitleArea',  // deprecate
    //HIDE_TITLE_DETAIL : 'HideTitleDetail',// deprecate
    //TITLE_FILENAME_MODE_PFX : 'TitleFilenameModePfx', // deprecate
    ANNOTATION_OPS : 'AnnotationOps',
    TITLE_OPTIONS : 'TitleOptions',
    EXPANDED_TITLE_OPTIONS : 'ExpandedTitleOptions',
    EXPANDED_TITLE : 'ExpandedTitle',
    POST_TITLE: 'PostTitle',
    PRE_TITLE: 'PreTitle',

    MASK_BITS: 'MaskBits',
    PLOT_AS_MASK: 'PlotAsMask',
    MASK_COLORS: 'MaskColors',
    MASK_REQUIRED_WIDTH: 'MaskRequiredWidth',
    MASK_REQUIRED_HEIGHT: 'MaskRequiredHeight'

};

const allKeys= new Enum(Object.keys(C).map( (k) => C[k]), { ignoreCase: true });


const clientSideKeys =
        [C.UNIQUE_KEY,
         C.PLOT_TO_DIV, C.PREFERENCE_COLOR_KEY, C.PREFERENCE_ZOOM_KEY,
         C.ROTATE_NORTH_SUGGESTION, C.SAVE_CORNERS,
         C.SHOW_SCROLL_BARS, C.EXPANDED_TITLE,
         C.ALLOW_IMAGE_SELECTION, C.HAS_NEW_PLOT_CONTAINER,
         C.GRID_ON,
         C.TITLE_OPTIONS, C.EXPANDED_TITLE_OPTIONS,
         C.POST_TITLE, C.PRE_TITLE, C.OVERLAY_POSITION,
         C.MINIMAL_READOUT,
         C.PLOT_GROUP_ID, C.DRAWING_SUB_GROUP_ID,  C.RELATED_TABLE_ROW,
         C.DOWNLOAD_FILENAME_ROOT, C.PLOT_ID, C.GROUP_LOCKED,
         C.OVERLAY_IDS
        ];

const ignoreForEquals = [C.PROGRESS_KEY, C.ZOOM_TO_WIDTH, C.ZOOM_TO_HEIGHT,
                          C.ZOOM_TYPE, C.HAS_NEW_PLOT_CONTAINER];

const DEFAULT_PIPELINE_ORDER= Order.ROTATE.key+';'+
                              Order.FLIP_Y.key+';'+
                              Order.FLIP_X.key+';'+
                              Order.POST_CROP.key+';'+
                              Order.POST_CROP_AND_CENTER.key;

const makeOrderList = (orderStr) => orderStr ? orderStr.split(';').map( (v) => Order.get(v)) : [];

const DEF_ORDER= makeOrderList(DEFAULT_PIPELINE_ORDER);


/**
 * @author Trey Roby
 */
export class WebPlotRequest extends ServerRequest {
//class WebPlotRequest {
    constructor(type,userDesc,serviceType) {
        super(type);
        if (type) this.setRequestType(type);

        this.setRequestClass(WEB_PLOT_REQUEST_CLASS);

        if (serviceType && serviceType!==ServiceType.NONE) this.setServiceType(serviceType);
        if (userDesc) this.setParam(C.USER_DESC, userDesc);
    }


//======================================================================
//----------- Factory Methods for various types of request    ----------
//----------- Most of the time it is better to use these than ----------
//----------- the constructors                                ----------
//======================================================================
    
    static makeFromObj(obj) {
        if (isString(obj)) {
            return WebPlotRequest.parse(obj);
        }
        else if (isPlainObject(obj)) {
            const wpr= new WebPlotRequest();
            wpr.setParams(cleanupObj(obj));


            let typeGuess;
            if (obj.id) typeGuess= RequestType.PROCESSOR;
            else if (obj[C.FILE]) typeGuess= RequestType.FILE;
            else if (obj[C.URLKEY]) typeGuess= RequestType.URL;
            else if (obj[C.SURVEY_KEY]) typeGuess= RequestType.SERVICE;


            if (obj[C.BLANK_ARCSEC_PER_PIX] && obj[C.BLANK_PLOT_WIDTH] &&
                obj[C.BLANK_PLOT_HEIGHT]) {
                typeGuess= RequestType.BLANK;
            }
            if (typeGuess && !wpr.params[C.TYPE]) wpr.setRequestType(typeGuess);

            if (has(wpr.params, C.URLKEY)) {
                wpr.setURL(wpr.params[C.URLKEY]);
            }

            return wpr;
        }
        return obj;
    }

    /**
     * Makes a WebPlotRequest from another request.
     * There are two things to note: The ServerRequest.ID_KEY is not set.
     * If this is a RequestType.PROCESSOR, then you should do the follow:
     * <ul>
     * <li>call setRequestType(RequestType.PROCESSOR)</li>
     * <li>call setParam(ServerRequest.ID_KEY, <i>someID</i>)</li>
     * </ul>
     * or if you can create the WebPlotRequest by calling makeProcessorRequest instead
     *
     * @param serverReq the request
     * @return {ServerRequest} the new WebPlotRequest
     */
    makeRequest(serverReq) {
        let retval;
        if (serverReq instanceof WebPlotRequest) {
            retval= serverReq;
        }
        else {
            retval = new WebPlotRequest(RequestType.FILE, 'Fits File');
            retval.setParams(serverReq.getParams());
            retval.removeParam(this.ID_KEY);
        }
        return retval;
    }



    static makeFilePlotRequest(fileName, initZoomLevel) {
        const req = new WebPlotRequest(RequestType.FILE, 'Fits file: ' + fileName);
        req.setParam(C.FILE, fileName);
        req.setTitleOptions(TitleOptions.FILE_NAME);
        if (initZoomLevel) {
            req.setParam(C.INIT_ZOOM_LEVEL, initZoomLevel + '');
        }
        else {
            req.setParam(C.INIT_ZOOM_LEVEL, 1);
            // req.setZoomType(ZoomType.TO_WIDTH); //todo fix when we can auto zoom to with: dm-4759
        }
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


    static makeTblFilePlotRequest(fileName) {
        const req = new WebPlotRequest(RequestType.FILE, 'Table: ' + fileName);
        req.setParam(C.FILE, fileName);
        return req;
    }


    static makeTblURLPlotRequest(url) {
        const req = new WebPlotRequest(RequestType.URL, 'Table from URL: ' + url);
        req.setParam(C.URLKEY, url);
        return req;
    }

    //======================== ISSA =====================================


    /**
     *
     * @param worldPt
     * @param {string} survey  must be one of '12','25','60','100'
     * @param sizeInDeg less then 12.5
     * @return {WebPlotRequest}
     */
    static makeISSARequest(worldPt, survey, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.ISSA, worldPt, survey, sizeInDeg);
        req.setTitle('ISSA '+survey);
        req.setDrawingSubGroupId('iras');
        return req;
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
        const req= this.makePlotServiceReq(ServiceType.IRIS, worldPt, survey, sizeInDeg);
        req.setTitle('IRIS '+survey);
        req.setDrawingSubGroupId('iras');
        return req;
    }


    //======================== 2MASS =====================================

    /**
     *
     * @param wp
     * @param {string} survey  must be one of 'j','h','k'
     * @param sizeInDeg less then .138 degrees (500 arcsec)
     * @return {WebPlotRequest}
     */
    static make2MASSRequest(wp, survey, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.TWOMASS, wp, survey, sizeInDeg);
        req.setTitle('2MASS '+survey.toUpperCase());
        req.setDrawingSubGroupId('2mass');
        return req;
    }

    //======================== MSX =====================================


    /**
     *
     * @param wp
     * @param survey must be  '3','4','5','6'
     *          for 'A (8.28 microns)', 'C (12.13 microns)', 'D (14.65 microns)', 'E (21.3 microns)'
     *
     * @param sizeInDeg .1 to 1.5
     * @return {WebPlotRequest}
     */
    static makeMSXRequest(wp, survey, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.MSX, wp, survey, sizeInDeg);
        req.setTitle('MSX '+survey);
        req.setDrawingSubGroupId('msx');
        return req;
    }

    //======================== SDSS =====================================

    /**
     *
     * @param wp
     * @param band  one of: 'u' 'g' 'r' 'i' 'z'
     * @param sizeInDeg .016 to .25
     * @return {WebPlotRequest}
     */
    static makeSloanDSSRequest(wp, band, sizeInDeg) {
        const req= this.makePlotServiceReq(ServiceType.SDSS, wp, band, sizeInDeg);
        req.setTitle('SDSS '+band);
        req.setDrawingSubGroupId('sdss');
        return req;
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
        const req = this.makePlotServiceReq(ServiceType.DSS, wp, survey, sizeInDeg);
        req.setTitle(`DSS ${survey}`);
        req.setDrawingSubGroupId('dss');
        return req;

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
        const req = this.makePlotServiceReq(ServiceType.WISE, wp, survey, sizeInDeg);
        req.setParam(C.SURVEY_KEY_BAND, band + '');
        const sDesc= survey.toLowerCase()==='3a' ? 'Atlas' : survey;
        req.setTitle('WISE: '+sDesc+ ', B'+ band);
        req.setDrawingSubGroupId('wise');
        return req;
    }

    //======================== DSS or IRIS =====================================

    static makeDSSOrIRISRequest(wp, dssSurvey, IssaSurvey, sizeInDeg) {
        const r = this.makePlotServiceReq(ServiceType.DSS_OR_IRIS, wp, dssSurvey, sizeInDeg);
        r.setSurveyKeyAlt(IssaSurvey);
        return r;
    }

    //======================== All Sky =====================================


    static makeAllSkyPlotRequest() {
        return new WebPlotRequest(RequestType.ALL_SKY, 'All Sky Image');
    }



    //======================== Blank =====================================
    static makeBlankPlotRequest(wp, arcsecSize, plotWidth, plotHeight ) {
        const r= new WebPlotRequest(RequestType.BLANK, '');
        r.setWorldPt(wp);
        r.setBlankArcsecPerPix(arcsecSize);
        r.setBlankPlotWidth(plotWidth);
        r.setBlankPlotHeight(plotHeight);
        r.setGridOn(GridOnStatus.TRUE);
        return r;
    }

//======================================================================
//----------------------- Title Settings -------------------------------
//======================================================================

    setTitle(title) { this.setParam(C.TITLE, title); }

    getTitle() { return this.getParam(C.TITLE); }


    setExpandedTitle(title) { this.setParam(C.EXPANDED_TITLE, title); }

    getExpandedTitle() { return this.getParam(C.EXPANDED_TITLE); }

    getUserDesc() { return this.getParam(C.USER_DESC); }

    /**
     *
     * @param option TitleOptions
     */
    setTitleOptions(option) { this.setParam(C.TITLE_OPTIONS,option.key); }

    /**
     *
     * @return {TitleOptions}
     */
    getTitleOptions() {
        return TitleOptions.get(this.getParam(C.TITLE_OPTIONS)) || TitleOptions.NONE;
    }



    /**
     *
     * @param option HeaderDecorationOps
     */
    setAnnotationOps(option) { this.setParam(C.ANNOTATION_OPS,option.key); }

    /**
     *
     * @return {AnnotationOps}
     */
    getAnnotationOps() {
        return AnnotationOps.get(this.getParam(C.ANNOTATION_OPS)) || AnnotationOps.INLINE;
    }




    /**
     *
     * @param {ExpandedTitleOptions} option
     */
    setExpandedTitleOptions(option) { this.setParam(C.EXPANDED_TITLE_OPTIONS,option.key); }

    /**
     *
     * @return {ExpandedTitleOptions}
     */
    getExpandedTitleOptions() {
        return ExpandedTitleOptions.get(this.getParam(C.EXPANDED_TITLE_OPTIONS)) || ExpandedTitleOptions.REPLACE;
    }




    setPreTitle(preTitle) { this.setParam(C.PRE_TITLE, preTitle); }
    getPreTitle() { return this.getParam(C.PRE_TITLE); }


    setPostTitle(postTitle) { this.setParam(C.POST_TITLE, postTitle); }
    getPostTitle() { return this.getParam(C.POST_TITLE); }

//======================================================================
//----------------------- Overlay Settings ------------------------------
//======================================================================

    /**
     *
     * @param {WorldPt|String} worldPt - the world point object or a serialized version
     */
    setOverlayPosition(worldPt) {
        this.setParam(C.OVERLAY_POSITION, worldPt ? worldPt.toString() : false);
    }

    /**
     *
     * @return {WorldPt}
     */
    getOverlayPosition() { return this.getWorldPtParam(C.OVERLAY_POSITION); }

//======================================================================
//----------------------- Color Settings ------------------------------
//======================================================================

    /**
     *
     * @param {int} id integer, color table id number
     */
    setInitialColorTable(id) { this.setParam(C.INIT_COLOR_TABLE, id + ''); }

    /**
     *
     * @return {int} color table id number
     */
    getInitialColorTable() {
        return this.getIntParam(C.INIT_COLOR_TABLE,0);
    }

    /**
     *
     * @param rangeValues RangeValues
     */
    setInitialRangeValues(rangeValues) {
        if (rangeValues) {
            this.setParam(C.INIT_RANGE_VALUES, rangeValues.toJSON());
        }
        else {
            this.removeParam(C.INIT_RANGE_VALUES);
        }
    }

    /**
     *
     * @return {RangeValues}
     */
    getInitialRangeValues() {
        return this.containsParam(C.INIT_RANGE_VALUES) ? RangeValues.parse(this.getParam(C.INIT_RANGE_VALUES)) : null;
    }

//======================================================================
//----------------------- Zoom Settings ------------------------------
//======================================================================

    /**
     * Certain zoom types require the width of the viewable area to determine the zoom level
     * used with ZoomType.FULL_SCREEN, ZoomType.TO_WIDTH
     *
     * @param {number} width the width in pixels
     * @see {ZoomType}
     */
    setZoomToWidth(width) { this.setParam(C.ZOOM_TO_WIDTH, width + ''); }

    getZoomToWidth() {
        return this.containsParam(C.ZOOM_TO_WIDTH) ? this.getIntParam(C.ZOOM_TO_WIDTH) : 0;
    }

    /**
     * Certain zoom types require the height of the viewable area to determine the zoom level
     * used with ZoomType.FULL_SCREEN, ZoomType.TO_HEIGHT (to height, no yet implemented)
     *
     * @param height the height in pixels
     * @see {ZoomType}
     */
    setZoomToHeight(height) {
        this.setParam(C.ZOOM_TO_HEIGHT, height + '');
    }

    getZoomToHeight() {
        return this.containsParam(C.ZOOM_TO_HEIGHT) ? this.getIntParam(C.ZOOM_TO_HEIGHT) : 0;
    }


    /**
     * set the initialize zoom level, this is used with ZoomType.LEVEL
     *
     * @param {number} zl the zoom level, float
     * @see {ZoomType}
     */
    setInitialZoomLevel(zl) {
        this.setParam(C.INIT_ZOOM_LEVEL, zl + '');
    }

    /**
     *
     * @return {number}, the zoom level
     */
    getInitialZoomLevel() {
        return this.getFloatParam(C.INIT_ZOOM_LEVEL,1);
    }

    /**
     *
     * @param {boolean} hasMax
     */
    setHasMaxZoomLevel(hasMax) { this.setParam(C.HAS_MAX_ZOOM_LEVEL, hasMax +''); }

    /**
     *
     * @return {boolean}
     */
    hasMaxZoomLevel() { return this.getBooleanParam(C.HAS_MAX_ZOOM_LEVEL); }

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
    setZoomType(zoomType) {
        if (zoomType) this.setParam(C.ZOOM_TYPE, zoomType.key);
    }

    getZoomType() {
        const w= this.getZoomToWidth();
        const h= this.getZoomToHeight();
        const defaultType= (w && h) ?  ZoomType.TO_WIDTH_HEIGHT : ZoomType.LEVEL;
        return ZoomType.get(this.getParam(C.ZOOM_TYPE)) || defaultType;
    }

    /**
     * set the arcseconds per screen pixel that will be used to determine the zoom level.
     * Used with ZoomType.ARCSEC_PER_SCREEN_PIX
     *
     * @param {number} arcsecSize
     * @see ZoomType
     */
    setZoomArcsecPerScreenPix(arcsecSize) {
        this.setParam(C.ZOOM_ARCSEC_PER_SCREEN_PIX, arcsecSize + '');
    }

    /**
     *
     * @return {number}
     */
    getZoomArcsecPerScreenPix() {
        return this.getFloatParam(C.ZOOM_ARCSEC_PER_SCREEN_PIX,0);
    }

//======================================================================
//----------------------- Rotate  & Flip Settings ----------------------
//======================================================================

    /**
     * Plot should come up rotated north
     *
     * @param {boolean} rotateNorth true to rotate
     */
    setRotateNorth(rotateNorth) { this.setParam(C.ROTATE_NORTH, rotateNorth + ''); }

    /**
     *
     * @return {boolean}
     */
    getRotateNorth() { return this.getBooleanParam(C.ROTATE_NORTH); }

    /**
     * Plot should come up rotated north, unless the user has already set the rotation using the button
     *
     * @param {boolean} rotateNorth true to rotate
     */
    setRotateNorthSuggestion(rotateNorth) {
        this.setParam(C.ROTATE_NORTH_SUGGESTION, rotateNorth + '');
    }

    /**
     *
     * @return {boolean}
     */
    getRotateNorthSuggestion() { return this.getBooleanParam(C.ROTATE_NORTH_SUGGESTION); }

    /**
     * Set to coordinate system for rotate north, eq j2000 is the default
     *
     * @param {boolean} rotateNorthType CoordinateSys, default CoordinateSys.EQ_J2000
     */
    setRotateNorthType(rotateNorthType) {
        this.setParam(C.ROTATE_NORTH_TYPE, rotateNorthType.toString());
    }

    /**
     *
     * @return CoordinateSys
     */
    getRotateNorthType() {
        const cStr = this.getParam(C.ROTATE_NORTH_TYPE);
        let retval = null;
        if (cStr !== null) retval = CoordinateSys.parse(cStr);
        if (retval === null) retval = CoordinateSys.EQ_J2000;
        return retval;
    }

    /**
     * set to rotate, if true, the angle should also be set
     *
     * @param rotate boolean, true to rotate
     */
    setRotate(rotate) { this.setParam(C.ROTATE, rotate + ''); }

    /**
     * @return boolean,  true if rotate, false otherwise
     */
    getRotate() { return this.getBooleanParam(C.ROTATE); }


    /**
     * set the angle to rotate to
     *
     * @param rotationAngle  number, the angle in degrees to rotate to
     */
    setRotationAngle(rotationAngle) { this.setParam(C.ROTATION_ANGLE, rotationAngle + ''); }

    /**
     * @return number, the angle
     */
    getRotationAngle() { return this.getFloatParam(C.ROTATION_ANGLE); }

    setRotateFromNorth(fromNorth) { this.setParam(C.ROTATE_FROM_NORTH, fromNorth+ ''); }

    getRotateFromNorth() { this.getBooleanParam(C.ROTATE_FROM_NORTH,true); }

    /**
     * set if this image should be flipped on the Y axis
     * @param flipY boolean, true to flip, false not to flip
     */
    setFlipY(flipY) { this.setParam(C.FLIP_Y,flipY+''); }


    /**
     * @return boolean, flip true or false
     */
    isFlipY() { return this.getBooleanParam(C.FLIP_Y); }

    /**
     * set if this image should be flipped on the X axis
     * @param flipX boolean, true to flip, false not to flip
     */
    setFlipX(flipX) { this.setParam(C.FLIP_X,flipX+''); }


    /**
     * @return boolean, flip true or false
     */
    isFlipX() { return this.getBooleanParam(C.FLIP_X); }

//======================================================================
//----------------------- Crop Settings --------------------------------
//======================================================================

    /**
     * Crop the image before returning it.  If rotation is set then the crop will happen post rotation.
     * Note: setCropPt1 & setCropPt2 are required to crop
     *
     * @param postCrop boolean, do the post crop
     */
    setPostCrop(postCrop) { this.setParam(C.POST_CROP, postCrop + ''); }

    /**
     * @return boolean, do the post crop
     */
    getPostCrop() { return this.getBooleanParam(C.POST_CROP); }


    /**
     * set the post crop
     * @param postCrop boolean
     */
    setPostCropAndCenter(postCrop) { this.setParam(C.POST_CROP_AND_CENTER, postCrop + ''); }

    /**
     * @return boolean, do the post crop and center
     */
    getPostCropAndCenter() { return this.getBooleanParam(C.POST_CROP_AND_CENTER); }

    /**
     * Set to coordinate system for crop and center, eq j2000 is the default
     * @param csys CoordinateSys, the CoordinateSys, default CoordinateSys.EQ_J2000
     */
    setPostCropAndCenterType(csys) { this.setParam(C.POST_CROP_AND_CENTER_TYPE, csys.toString()); }

    /**
     * @return CoordinatesSys
     */
    getPostCropAndCenterType() {
        const cStr= this.getParam(C.POST_CROP_AND_CENTER_TYPE);
        let retval= null;
        if (cStr!==null) retval= CoordinateSys.parse(cStr);
        if (retval===null) retval= CoordinateSys.EQ_J2000;
        return retval;
    }

    setCropPt1(pt1) {
        if (pt1) this.setParam((pt1.type===Point.W_PT) ? C.CROP_WORLD_PT1 : C.CROP_PT1, pt1.toString());
    }

    /**
     *  @return imagePt
     */
    getCropImagePt1() { return parseImagePt(this.getParam(C.CROP_PT1)); }

    setCropPt2(pt2) {
        if (pt2) this.setParam((pt2.type===Point.W_PT) ? C.CROP_WORLD_PT2 : C.CROP_PT2, pt2.toString());
    }

    /**
     *  @return imagePt
     */
    getCropImagePt2() { return parseImagePt(this.getParam(C.CROP_PT2)); }


    /**
     *  @return WorldPt
     */
    getCropWorldPt1() { return this.getWorldPtParam(C.CROP_WORLD_PT1); }

    /**
     *  @return WorldPt
     */
    getCropWorldPt2() { return this.getWorldPtParam(C.CROP_WORLD_PT2); }



//======================================================================
//----------------------- Blank Image Settings -------------------------
//======================================================================

    /**
     * set the arc seconds per pixel that will be used for a blank image
     * Used with RequestType.BLANK
     *
     * @param {number} arcsecSize float,  the size of the pixels in arcsec
     * @see RequestType
     */
    setBlankArcsecPerPix(arcsecSize) { this.setParam(C.BLANK_ARCSEC_PER_PIX, arcsecSize + ''); }

    /**
     * @return {number} float
     */
    getBlankArcsecPerPix() { return this.getFloatParam(C.BLANK_ARCSEC_PER_PIX,0); }

    /**
     * @param {number} width int
     */
    setBlankPlotWidth(width) { this.setParam(C.BLANK_PLOT_WIDTH, width + ''); }

    /**
     * @return {number} width int
     */
    getBlankPlotWidth() {
        return this.getIntParam(C.BLANK_PLOT_WIDTH,0);
    }


    /**
     * @param height int
     */
    setBlankPlotHeight(height) { this.setParam(C.BLANK_PLOT_HEIGHT, height + ''); }

    getBlankPlotHeight() { return this.getIntParam(C.BLANK_PLOT_HEIGHT,0); }

//======================================================================
//----------------------- Retrieval Settings --------------------------------
//======================================================================

    /**
     * plot the file name that exist on the server
     *
     * @param fileName the file name on the server
     */
    setFileName(fileName) { this.setParam(C.FILE, fileName); }

    getFileName() { return this.getParam(C.FILE); }

    /**
     * retrieve and plot the file from the specified URL
     *
     * @param url the URL where the file resides
     */
    setURL(url) { this.setSafeParam(C.URLKEY, url); }

    getURL() { return this.getSafeParam(C.URLKEY); }

    setURLCheckForNewer(check) { this.setSafeParam(C.URL_CHECK_FOR_NEWER, check+''); }

    getURLCheckForNewer() { return this.getBooleanParam(C.URL_CHECK_FOR_NEWER); }

    /**
     *
     * @param service ServiceType
     */
    setServiceType(service) { this.setParam(C.SERVICE, service.key); }

    /**
     * @return RequestType
     */
    getRequestType() {
        return RequestType.get(this.getParam(C.TYPE)) ||RequestType.FILE;
    }

    /**
     * Set the type of request. This parameter is required for every call.  The factory methods will always
     * set the Request type.
     *
     *
     * @param type the RequestType
     * @see RequestType
     */
    setRequestType(type) { this.setParam(C.TYPE, type.key); }

    /**
     *
     * @return ServiceType
     */
    getServiceType() {
        return ServiceType.get(this.getParam(C.SERVICE)) ||ServiceType.NONE;
    }

    /**
     * @param {string} key string
     */
    setSurveyKey(key) { this.setParam(C.SURVEY_KEY, key); }

    /**
     * @return {string} key string
     */
    getSurveyKey() { return this.getParam(C.SURVEY_KEY); }

    /**
     * @param {string} key string
     */
    setSurveyKeyAlt(key) { this.setParam(C.SURVEY_KEY_ALT, key); }

    /**
     * @return {string} key string
     */
    getSurveyKeyAlt() { return this.getParam(C.SURVEY_KEY_ALT); }

    /**
     * @return {string} key string
     */
    getSurveyBand() { return this.getParam(C.SURVEY_KEY_BAND); }

//======================================================================
//----------------------- Object & Area Settings -----------------------
//======================================================================

    /**
     * @param {string} objectName string astronomical object to search
     */
    setObjectName(objectName) { this.setParam(C.OBJECT_NAME, objectName); }

    /**
     * @return {string} astronomical object, string
     */
    getObjectName() { return this.getParam(C.OBJECT_NAME); }


    /**
     *
     * @param resolver Resolver, name resolver type
     */
    setResolver(resolver) { this.setParam(C.RESOLVER, resolver.toString()); }

    /**
     * @return Resolver, name resolver type
     */
    getResolver() { return parseResolver(this.getParam(C.RESOLVER)); }

    /**
     *
     * @param {WorldPt} worldPt WorldPt
     */
    setWorldPt(worldPt) {
        if (worldPt) this.setParam(C.WORLD_PT, worldPt.toString());
    }

    /**
     * @return {WorldPt} WorldPt
     */
    getWorldPt() { return this.getWorldPtParam(C.WORLD_PT); }


    setSizeInDeg(sizeInDeg) { this.setParam(C.SIZE_IN_DEG, sizeInDeg + ''); }
    getSizeInDeg() { return this.getFloatParam(C.SIZE_IN_DEG, NaN); }

//======================================================================
//----------------------- Other Settings --------------------------------
//======================================================================


    setMultiImageIdx(idx) { this.setParam(C.MULTI_IMAGE_IDX, idx + ''); }

    /**
     * @return number index of image
     */
    getMultiImageIdx() { return this.getIntParam(C.MULTI_IMAGE_IDX,0); }


    /**
     * image extension list. ex: '3,4,5' for extension 3, 4, 5
     * @param idxS
     */
    setMultiImageExts(idxS) { this.setParam(C.MULTI_IMAGE_EXTS, idxS); }

    /**
     * return image extension list
     * @returns {*}
     */
    getMultiImageExts() { return this.getParam(C.MULTI_IMAGE_EXTS); }

    /**
     * key to store preferences in local cache
     * @param key String
     */
    setPreferenceColorKey(key) { this.setParam(C.PREFERENCE_COLOR_KEY, key); }

    getPreferenceColorKey() { return this.getParam(C.PREFERENCE_COLOR_KEY); }

    setPreferenceZoomKey(key) { this.setParam(C.PREFERENCE_ZOOM_KEY, key); }

    getPreferenceZoomKey() { return this.getParam(C.PREFERENCE_ZOOM_KEY); }


    /**
     * @param save boolean
     */
    setSaveCorners(save) { this.setParam(C.SAVE_CORNERS, save + ''); }

    /**
     * @return boolean
     */
    getSaveCorners() { return this.getBooleanParam(C.SAVE_CORNERS); }


    /**
     * @param allowImageSelection boolean
     */
    setAllowImageSelection(allowImageSelection) {
        this.setParam(C.ALLOW_IMAGE_SELECTION, allowImageSelection + '');
    }

    /**
     * @return boolean
     */
    isAllowImageSelection() { return this.getBooleanParam(C.ALLOW_IMAGE_SELECTION); }

    /**
     * @param allowImageSelectionCreateNew boolean
     */
    setHasNewPlotContainer(allowImageSelectionCreateNew) {
        this.setParam(C.HAS_NEW_PLOT_CONTAINER, allowImageSelectionCreateNew + '');
    }

    getHasNewPlotContainer() { this.getBooleanParam(C.HAS_NEW_PLOT_CONTAINER); }


    /**
     *
     * @param gridOnStatus GridOnStatus
     */

    setGridOn(gridOnStatus= GridOnStatus.FALSE) {
        const stat= GridOnStatus.get( String(gridOnStatus) );
        this.setParam(C.GRID_ON, stat.key);
    }

    /**
     *
     * @return GridOnStatus
     */
    getGridOn() {
        return GridOnStatus.get(this.getParam(C.GRID_ON)) ||GridOnStatus.FALSE;
    }

    ///**
    // * @param hideTitleZoomLevel boolean
    // */
    //setHideTitleDetail(hideTitleZoomLevel) { this.setParam(C.HIDE_TITLE_DETAIL, hideTitleZoomLevel + ''); }
    //
    ///**
    // * @return boolean
    // */
    //getHideTitleDetail() { return this.getBooleanParam(C.HIDE_TITLE_DETAIL); }

    /**
     * @param thumbnailSize int
     */
    setThumbnailSize(thumbnailSize) { this.setParam(C.THUMBNAIL_SIZE, thumbnailSize+''); }

    /**
     * @return int
     */
    getThumbnailSize() {
        return this.getIntParam(C.THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE);
    }

    /**
     * For 3 color, if this request fails then keep trying to make a plot with the other request
     *
     * @param continueOnFail boolean
     */
    setContinueOnFail(continueOnFail) { this.setParam(C.CONTINUE_ON_FAIL, continueOnFail + ''); }

    isContinueOnFail() { return this.getBooleanParam(C.CONTINUE_ON_FAIL); }

    setUniqueKey(key) { this.setParam(C.UNIQUE_KEY, key); }

    getUniqueKey() { return this.getParam(C.UNIQUE_KEY); }

    setPlotToDiv(div) { this.setParam(C.PLOT_TO_DIV, div); }

    getPlotToDiv() { return this.getParam(C.PLOT_TO_DIV); }


    setHeaderKeyForTitle(headerKey) { this.setParam(C.HEADER_KEY_FOR_TITLE, headerKey); }

    getHeaderKeyForTitle() { return this.getParam(C.HEADER_KEY_FOR_TITLE); }


    /**
     * @param {boolean} showBars boolean
     */
    setShowScrollBars(showBars) { this.setParam(C.SHOW_SCROLL_BARS, showBars + ''); }

    /**
     * @return boolean
     */
    getShowScrollBars() { return this.getBooleanParam(C.SHOW_SCROLL_BARS); }

    setProgressKey(key) { this.setParam(C.PROGRESS_KEY,key); }

    getProgressKey() { return this.getParam(C.PROGRESS_KEY); }

    setRequestKey(key) { this.setParam(C.PROGRESS_KEY,key); }  // alias of setProgressKey

    getRequestKey() { return this.getParam(C.PROGRESS_KEY); } // alias of getProgressKey

    /**
     * @param minimalReadout boolean
     */
    setMinimalReadout(minimalReadout) { this.setParam(C.MINIMAL_READOUT,minimalReadout+''); }

    /**
     * @return boolean
     */
    isMinimalReadout() { return this.getBooleanParam(C.MINIMAL_READOUT); }

    setDrawingSubGroupId(id) { this.setParam(C.DRAWING_SUB_GROUP_ID,id); }

    getDrawingSubGroupId() { return this.getParam(C.DRAWING_SUB_GROUP_ID); }

    setRelatedTableRow(id) { this.setParam(C.RELATED_TABLE_ROW,id); }

    getRelatedTableRow() { return this.getIntParam(C.RELATED_TABLE_ROW,-1); }

    setDownloadFileNameRoot(nameRoot) { this.setParam(C.DOWNLOAD_FILENAME_ROOT, nameRoot); }

    getDownloadFileNameRoot() { return this.getParam(C.DOWNLOAD_FILENAME_ROOT); }

    setPlotId(id) { this.setParam(C.PLOT_ID,id); }

    getPlotId() { return this.getParam(C.PLOT_ID); }

    setPlotGroupId(id) { this.setParam(C.PLOT_GROUP_ID,id); }

    getPlotGroupId() { return this.getParam(C.PLOT_GROUP_ID); }

    setGroupLocked(locked) { this.setParam(C.GROUP_LOCKED,locked); }

    isGroupLocked() { return this.getBooleanParam(C.GROUP_LOCKED,true); }

    /**
     * Set the order that the image processing pipeline runs when it reads a fits file.
     * This is experimental.  Use at your own risk.
     * Warning- if you exclude an Order element, the pipeline will not execute that process
     * even is you have it set in the options.
     * @param orderList array of Order enums, the order of the pipeline
     */
    setPipelineOrder(orderList) {
        this.setParam(C.PIPELINE_ORDER, join(orderList,';'));
    }

    /**
     * @return {Array} array of Order enums
     */
    getPipelineOrder() {
        let retList= DEF_ORDER;
        if (this.containsParam(C.PIPELINE_ORDER)) {
            retList= makeOrderList(this.getParam(C.PIPELINE_ORDER));
            if (retList.length<2) retList= DEF_ORDER;
        }
        return retList;
    }


    setMaskBits(idx) { this.setParam(C.MASK_BITS,idx+''); }
    getMaskBits() { return this.containsParam(C.MASK_BITS) ? this.getIntParam(C.MASK_BITS) : 0;}

    setPlotAsMask(plotAsMask) { this.setParam(C.PLOT_AS_MASK, plotAsMask+''); }
    isPlotAsMask() { return this.getBooleanParam(C.PLOT_AS_MASK); }


    setMaskColors(colors) {
        if (isArray(colors)) {
            this.setParam(C.MASK_COLORS, join(colors, ';'));
        }
        else {
            this.setParam(C.MASK_COLORS, colors);
        }
    }

    getMaskColors() {
        const retList= [];
        if (this.containsParam(C.MASK_COLORS)) {
            return this.getParam(C.MASK_COLORS).split(';');
        }
        return retList;
    }

    setMaskRequiredWidth(width) { this.setParam(C.MASK_REQUIRED_WIDTH, width+''); }

    getMaskRequiredWidth() { return this.getIntParam(C.MASK_REQUIRED_WIDTH,0); }

    setMaskRequiredHeight(height) { this.setParam(C.MASK_REQUIRED_HEIGHT, height+''); }

    getMaskRequiredHeight() { return this.getIntParam(C.ASK_REQUIRED_HEIGHT,0); }





    /**
     *
     * @param overlayIdList
     */
    setOverlayIds(overlayIdList) {
        this.setParam(C.OVERLAY_IDS, join(overlayIdList, ';'));
    }

    /**
     * @return {Array} array of Order enums
     */
    getOverlayIds() {
        return this.containsParam(C.OVERLAY_IDS) ?
            this.getParam(C.OVERLAY_IDS).split(';') :
            ['ACTIVE_TARGET_TYPE','POINT_SELECTION_TYPE', 'DISTANCE_TOOL_TYPE', 'NORTH_UP_COMPASS_TYPE',
             'SELECT_AREA_TYPE', 'WEB_GRID_TYPE', 'OVERLAY_MARKER_TYPE', 'OVERLAY_FOOTPRINT_TYPE', 'REGION_PLOT_TYPE'];
            //[ActiveTarget.TYPE_ID,'OTHER'];
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


    prettyString() {
        let s = 'WebPlotRequest: ';
        switch (this.getRequestType()) {
            case RequestType.SERVICE:
                switch (this.getServiceType()) {
                    case ServiceType.IRIS:
                    case ServiceType.DSS:
                    case ServiceType.TWOMASS:
                        if (this.containsParam(C.WORLD_PT)) {
                            s += this.getServiceType().key + '- ' + this.getRequestArea();
                        }
                        else {
                            s += this.getServiceType().key + '- Obj name: ' + this.getObjectName() +
                            ', radius: ' +this.getParam(C.SIZE_IN_DEG);
                        }
                        break;
                }
                break;
            case RequestType.FILE:
                s += ' File: ' + this.getFileName();
                break;
            case RequestType.URL:
                s += ' URL: ' + this.getURL();
                break;
            case RequestType.ALL_SKY:
                s += ' AllSky';
                break;
            case RequestType.PROCESSOR:
                s += 'File Search Processor: '+ this.getRequestId();
                break;
        }
        return s;
    }

    setPlotDescAppend(s) { this.setParam(C.PLOT_DESC_APPEND, s); }

    getPlotDescAppend() { return this.getParam(C.PLOT_DESC_APPEND); }

    /**
     * @return boolean
     */
    hasID() {
        return this.containsParam(this.ID_KEY) && this.getRequestId()!==ID_NOT_DEFINED;
    }
//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    /**
    * This method can only be used for those services that take the standard
    * ra, dec, radius approach (so far, iras, issa, 2mass, dss)
    *
    * @param {ServiceType} serviceType the network service type
    * @param {WorldPt} wp the center point WorldPt
     * @param {string} survey
    * @param {number} sizeInDeg size in degrees
    *
    * @return {WebPlotRequest} the PlotRequest object that was constructed
    */
    static makePlotServiceReq(serviceType, wp, survey, sizeInDeg) {
        const desc = this.makeServiceReqDesc(serviceType, survey, sizeInDeg);
        const req = new WebPlotRequest(RequestType.SERVICE, desc, serviceType);
        req.setSurveyKey(survey);
        req.setWorldPt(wp);
        req.setSizeInDeg(sizeInDeg);
        return req;
    }

    static makeServiceReqDesc(serviceType, survey, sizeInDeg) {
        return serviceType.key + ': ' + survey + ', ' + sizeInDeg + ' Deg';
    }


    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     *
     * @param {string} str the serialized WebPlotRequest
     * @return (WebPlotRequest) the deserialized WebPlotRequest
     */
    static parse(str) {
        return ServerRequest.parse(str, new WebPlotRequest());
    }

    makeCopy() {
        const retval = new WebPlotRequest();
        retval.copyFrom(this);
        return retval;
    }

    static makeCopyOfRequest(r) {
        return r ? null : r.makeCopy();
    }

    static getAllKeys() { return allKeys; }

    static getClientKeys() { return clientSideKeys; }

    /**
     * Perform equals but ignore layout params, such as zoom type, width and height
     * @param obj
     * @return {boolean}
     */
    equalsPlottingParams(obj) {
        let retval= false;
        if (obj instanceof WebPlotRequest) {
            const wpr1= this.makeCopy();
            const wpr2= obj.makeCopy();
            ignoreForEquals.forEach((key)=> {
                wpr1.removeParam(key);
                wpr2.removeParam(key);
            });
            retval= wpr1.toString()===wpr2.toString();
        }
        return retval;
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}


export const WPConst= C;
export default WebPlotRequest;

/**
 * Create a new object with the keys more consistent with the keys defined in WebPlotRequest.
 * if a case insensitive version of the key exist then replace it with the proper one, otherwise
 * use the key.
 * The loop looks up the key with case insensitive matching, if it does not exist it uses the original key
 * @param {Object} r - plain object
 * @return {Object}
 */
function cleanupObj(r) {
    return Object.keys(r).reduce( (obj,k) => {
        obj[get(allKeys.get(k), 'key',k)]= r[k]; // note - uses both lodash.get and enum.get
        return obj;
    },{});
}

/**
 * find all the invalid WebPlotRequest keys.  
 * @param {Object} r an object literal to evaluate
 * @return {String[]} an array of invalid keys
 */
export function findInvalidWPRKeys(r) {
    return Object.keys(r).filter( (k) => !Boolean(allKeys.get(k)));
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
export function confirmPlotRequest(request,global,fallbackGroupId,makePlotId) {
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

        return request.map( (r) => Object.assign({},global,r,{plotId,plotGroupId,GroupLocked:locked}));
    }
    else {
        const r= Object.assign({}, global, request);
        if (!r.plotId) r.plotId= makePlotId();
        if (!r.plotGroupId) r.plotGroupId= fallbackGroupId;
        if (r.plotGroupId===fallbackGroupId) r.GroupLocked= false;
        return r;
    }
}


function makeDataOnlyRequestString(r) {
    if (!r) return '';
    r= r.makeCopy();
    r.setZoomToWidth(1);
    r.setZoomToHeight(1);
    r.setRequestKey('');
    r.setInitialRangeValues();
    r.setInitialColorTable(0);
    return r.toString();
}

/**
 * Make simplified versions of the WebPlotRequest and compare them.
 * This function takes out a lot of parameters that are not related to resolving the fits file.  It attempts see
 * if we are requesting the same fits file or fits file services.
 * @param {WebPlotRequest} r1
 * @param {WebPlotRequest} r2
 */
export const isImageDataRequeestedEqual= (r1,r2) => makeDataOnlyRequestString(r1)===makeDataOnlyRequestString(r2);
