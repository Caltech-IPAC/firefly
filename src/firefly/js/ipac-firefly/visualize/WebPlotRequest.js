/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/
/**
 * User: roby
 * Date: Apr 2, 2009
 * Time: 9:18:47 AM
 */
"use strict";

import {ServerRequest, ID_NOT_DEFINED}  from "ipac-firefly/data/ServerRequest.js";
//var ServerRequest=  require("ipac-firefly/data/ServerRequest.js");
import RequestType from "./RequestType.js";
import ZoomType from "./ZoomType.js";
import Enum from "enum";
import CoordinateSys from "./CoordSys.js";
import {WorldPt, ImagePt, Pt} from "./Point.js";
import Resolver from "ipac-firefly/astro/net/Resolver.js";
import RangeValues from "./RangeValues.js";
import _ from "underscore";

//var WorldPt= PtPackage.WordlPt;
//var ImagePt= PtPackage.ImagePt;
//var Pt= PtPackage.Pt;


const ServiceType= new Enum(["IRIS", "ISSA", "DSS", "SDSS", "TWOMASS", "MSX", "DSS_OR_IRIS", "WISE", "NONE"]);
const TitleOptions= new Enum(["NONE",  // use what it in the title
                            "PLOT_DESC", // use the plot description key
                            "FILE_NAME", // use the file name or analyze the URL and make a title from that
                            "HEADER_KEY", // use the header value
                            "PLOT_DESC_PLUS", // ??
                            "SERVICE_OBS_DATE"
                           ]);
const ExpandedTitleOptions= new Enum([ "REPLACE",// use expanded title when expanded
                                     "PREFIX",// use expanded title as prefix to title
                                     "SUFFIX"// use expanded title as sufix to title
                                   ]);
const GridOnStatus= new Enum(["FALSE","TRUE","TRUE_LABELS_FALSE"]);

const DEFAULT_THUMBNAIL_SIZE= 70;
const WEB_PLOT_REQUEST_CLASS= "WebPlotRequest";
const Order= new Enum(["FLIP_Y", "FLIP_X", "ROTATE", "POST_CROP", "POST_CROP_AND_CENTER"]);

//keys
// note- if you add a new key make sure you put it in the _allKeys array

const C= {
    FILE : "File",
    WORLD_PT : "WorldPt",
    URLKEY : "URL",
    SIZE_IN_DEG : "SizeInDeg",
    SURVEY_KEY : "SurveyKey",
    SURVEY_KEY_ALT : "SurveyKeyAlt",
    SURVEY_KEY_BAND : "SurveyKeyBand",
    TYPE : "Type",
    ZOOM_TYPE : "ZoomType",
    SERVICE : "Service",
    USER_DESC : "UserDesc",
    INIT_ZOOM_LEVEL : "InitZoomLevel",
    TITLE : "Title",
    ROTATE_NORTH : "RotateNorth",
    ROTATE_NORTH_TYPE : "RotateNorthType",
    ROTATE : "Rotate",
    ROTATION_ANGLE : "RotationAngle",
    HEADER_KEY_FOR_TITLE : "HeaderKeyForTitle",
    INIT_RANGE_VALUES : "RangeValues",
    INIT_COLOR_TABLE : "ColorTable",
    MULTI_IMAGE_FITS : "MultiImageFits",
    MULTI_IMAGE_IDX : "MultiImageIdx",
    ZOOM_TO_WIDTH : "ZoomToWidth",
    ZOOM_TO_HEIGHT : "ZoomToHeight",
    ZOOM_ARCSEC_PER_SCREEN_PIX : "ZoomArcsecPerScreenPix",
    POST_CROP : "PostCrop",
    POST_CROP_AND_CENTER : "PostCropAndCenter",
    POST_CROP_AND_CENTER_TYPE : "PostCropAndCenterType",
    CROP_PT1 : "CropPt1",
    CROP_PT2 : "CropPt2",
    CROP_WORLD_PT1 : "CropWorldPt1",
    CROP_WORLD_PT2 : "CropWorldPt2",
    UNIQUE_KEY : "UniqueKey",
    CONTINUE_ON_FAIL : "ContinueOnFail",
    OBJECT_NAME : "ObjectName",
    RESOLVER : "Resolver",
    PLOT_DESC_APPEND : "PlotDescAppend",
    BLANK_ARCSEC_PER_PIX : "BlankArcsecPerScreenPix",  //todo: doc
    BLANK_PLOT_WIDTH : "BlankPlotWidth",               //todo: doc
    BLANK_PLOT_HEIGHT : "BlankPlotHeight",             //todo: doc
    PROGRESS_KEY : "ProgressKey",
    FLIP_X : "FlipX",
    FLIP_Y : "FlipY",
    HAS_MAX_ZOOM_LEVEL : "HasMaxZoomLevel",
    THUMBNAIL_SIZE : "thumbnailSize",
    PIPELINE_ORDER : "pipelineOrder",

    MULTI_PLOT_KEY: "MultiPlotKey",
    THREE_COLOR_PLOT_KEY: "ThreeColorPlotKey",
    THREE_COLOR_HINT: "ThreeColorHint",
    RED_HINT: "RedHint",
    GREEN_HINT: "GreenHint",
    BLUE_HINT: "BlueHint",

// keys - client side operations
// note- if you add a new key make sure you put it in the _allKeys array
    PLOT_TO_DIV : "PlotToDiv",
    PREFERENCE_COLOR_KEY : "PreferenceColorKey",
    PREFERENCE_ZOOM_KEY : "PreferenceZoomKey",
    SHOW_TITLE_AREA : "ShowTitleArea",
    ROTATE_NORTH_SUGGESTION : "RotateNorthSuggestion",
    SAVE_CORNERS : "SaveCornersAfterPlot",
    SHOW_SCROLL_BARS : "showScrollBars",
    EXPANDED_TITLE : "ExpandedTitle",
    ALLOW_IMAGE_SELECTION : "AllowImageSelection",
    HAS_NEW_PLOT_CONTAINER : "HasNewPlotContainer",
    ADVERTISE : "Advertise",
    HIDE_TITLE_DETAIL : "HideTitleDetail",
    GRID_ON : "GridOn",
    TITLE_OPTIONS : "TitleOptions",
    EXPANDED_TITLE_OPTIONS : "ExpandedTitleOptions",
    POST_TITLE: "PostTitle",
    PRE_TITLE: "PreTitle",
    TITLE_FILENAME_MODE_PFX : "TitleFilenameModePfx",
    OVERLAY_POSITION : "OverlayPosition",
    MINIMAL_READOUT: "MinimalReadout",
    DRAWING_SUB_GROUP_ID: "DrawingSubgroupID",
    GRID_ID : "GRID_ID",
    DOWNLOAD_FILENAME_ROOT : "DownloadFileNameRoot"

};

const _allKeys =
        [C.FILE, C.WORLD_PT, C.URLKEY, C.SIZE_IN_DEG, C.SURVEY_KEY,
         C.SURVEY_KEY_ALT, C.SURVEY_KEY_BAND, C.TYPE, C.ZOOM_TYPE,
         C.SERVICE, C.USER_DESC, C.INIT_ZOOM_LEVEL,
         C.TITLE, C.ROTATE_NORTH, C.ROTATE_NORTH_TYPE, C.ROTATE, C.ROTATION_ANGLE,
         C.HEADER_KEY_FOR_TITLE,
         C.INIT_RANGE_VALUES, C.INIT_COLOR_TABLE, C.MULTI_XMAGE_FITS, C.MULTI_IMAGE_IDX,
         C.ZOOM_TO_WIDTH, C.ZOOM_TO_HEIGHT,
         C.POST_CROP, C.POST_CROP_AND_CENTER, C.FLIP_X, C.FLIP_Y,
         C.HAS_MAX_ZOOM_LEVEL,
         C.POST_CROP_AND_CENTER_TYPE, C.CROP_PT1, C.CROP_PT2, C.CROP_WORLD_PT1, C.CROP_WORLD_PT2,
         C.ZOOM_ARCSEC_PER_SCREEN_PIX, C.CONTINUE_ON_FAIL, C.OBJECT_NAME, C.RESOLVER,
         C.BLANK_ARCSEC_PER_PIX, C.BLANK_PLOT_WIDTH, C.BLANK_PLOT_HEIGHT,

         C.UNIQUE_KEY,
         C.PLOT_TO_DIV, C.PREFERENCE_COLOR_KEY, C.PREFERENCE_ZOOM_KEY,
         C.SHOW_TITLE_AREA, C.ROTATE_NORTH_SUGGESTION, C.SAVE_CORNERS,
         C.SHOW_SCROLL_BARS, C.EXPANDED_TITLE, C.PLOT_DESC_APPEND, C.HIDE_TITLE_DETAIL,
         C.ALLOW_IMAGE_SELECTION, C.HAS_NEW_PLOT_CONTAINER,
         C.GRID_ON, C.TITLE_OPTIONS, C.EXPANDED_TITLE_OPTIONS,
         C.POST_TITLE, C.PRE_TITLE, C.OVERLAY_POSITION,
         C.TITLE_FILENAME_MODE_PFX, C.MINIMAL_READOUT, C.DRAWING_SUB_GROUP_ID, C.GRID_ID,
         C.DOWNLOAD_FILENAME_ROOT
        ];

const _clientSideKeys =
        [C.UNIQUE_KEY,
         C.PLOT_TO_DIV, C.PREFERENCE_COLOR_KEY, C.PREFERENCE_ZOOM_KEY,
         C.SHOW_TITLE_AREA, C.ROTATE_NORTH_SUGGESTION, C.SAVE_CORNERS,
         C.SHOW_SCROLL_BARS, C.EXPANDED_TITLE,
         C.ALLOW_IMAGE_SELECTION, C.HAS_NEW_PLOT_CONTAINER,
         C.ADVERTISE, C.HIDE_TITLE_DETAIL, C.GRID_ON,
         C.TITLE_OPTIONS, C.EXPANDED_TITLE_OPTIONS,
         C.POST_TITLE, C.PRE_TITLE, C.OVERLAY_POSITION,
         C.TITLE_FILENAME_MODE_PFX, C.MINIMAL_READOUT,
         C.DRAWING_SUB_GROUP_ID, C.GRID_ID, C.DOWNLOAD_FILENAME_ROOT
        ];

const _ignoreForEquals = [C.PROGRESS_KEY, C.ZOOM_TO_WIDTH, C.ZOOM_TO_HEIGHT,
                          C.ZOOM_TYPE, C.HAS_NEW_PLOT_CONTAINER];

const DEFAULT_PIPELINE_ORDER= Order.ROTATE.value+";"+
                              Order.FLIP_Y.value+";"+
                              Order.FLIP_X.value+";"+
                              Order.POST_CROP.value+";"+
                              Order.POST_CROP_AND_CENTER.value;

function makeOrderList(orderStr) {
    var retList= [];
    if (!orderStr) return retList;
    var sAry= orderStr.split(";");
    sAry.forEach(v => {
        if (Order.get(v)) retList.push(Order.get(v));
    });
    return retList;
}

const DEF_ORDER= makeOrderList(DEFAULT_PIPELINE_ORDER);



/**
 * @author Trey Roby
 */
class WebPlotRequest extends ServerRequest {
//class WebPlotRequest {
    constructor(type,userDesc,serviceType) {
        super(type);
        this.WebPlotRequestConst= C;
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
     * @return the new WebPlotRequest
     */
    makeRequest(serverReq) {
        var retval;
        if (serverReq instanceof WebPlotRequest) {
            retval= serverReq;
        }
        else {
            retval = new WebPlotRequest(RequestType.FILE, "Fits File");
            retval.setParams(serverReq.getParams());
            retval.removeParam(this.ID_KEY);
        }
        return retval;
    }



    static makeFilePlotRequest(fileName, initZoomLevel) {
        var req = new WebPlotRequest(RequestType.FILE, "Fits file: " + fileName);
        req.setParam(C.FILE, fileName);
        if (initZoomLevel) {
            req.setParam(C.INIT_ZOOM_LEVEL, initZoomLevel + "");
        }
        else {
            req.setZoomType(ZoomType.TO_WIDTH);
        }
        return req;
    }

    static makeProcessorRequest(serverRequest, desc) {
        var req = new WebPlotRequest(RequestType.PROCESSOR, desc);
        req.setParams(serverRequest.getParams());
        return req;
    }


    static makeURLPlotRequest(url, userDesc) {
        var req = new WebPlotRequest(RequestType.URL, userDesc||"Fits from URL: " + url);
        req.setURL(url);
        return req;
    }


    static makeTblFilePlotRequest(fileName) {
        var req = new WebPlotRequest(RequestType.FILE, "Table: " + fileName);
        req.setParam(C.FILE, fileName);
        return req;
    }


    static makeTblURLPlotRequest(url) {
        var req = new WebPlotRequest(RequestType.URL, "Table from URL: " + url);
        req.setParam(C.URLKEY, url);
        return req;
    }

    //======================== ISSA =====================================


    static makeISSARequest(worldPt, survey, sizeInDeg) {
        var req= this.makePlotServiceReq(ServiceType.ISSA, worldPt, survey, sizeInDeg);
        req.setTitle("ISSA "+survey);
        return req;
    }

    //======================== IRIS =====================================


    static makeIRISRequest(worldPt, survey, sizeInDeg) {
        var req= this.makePlotServiceReq(ServiceType.IRIS, worldPt, survey, sizeInDeg);
        req.setTitle("IRIS "+survey);
        return req;
    }


    //======================== 2MASS =====================================

    static make2MASSRequest(wp, survey, sizeInDeg) {
        var req= this.makePlotServiceReq(ServiceType.TWOMASS, wp, survey, sizeInDeg);
        req.setTitle("2MASS "+survey);
        return req;
    }

    //======================== MSX =====================================


    static makeMSXRequest(wp, survey, sizeInDeg) {
        var req= this.makePlotServiceReq(ServiceType.MSX, wp, survey, sizeInDeg);
        req.setTitle("MSX "+survey);
        return req;
    }

    //======================== SDSS =====================================

    static makeSloanDSSRequest(wp, band, sizeInDeg) {
        var req= this.makePlotServiceReq(ServiceType.SDSS, wp, band, sizeInDeg);
        req.setTitle("SDSS "+band);
        return req;
    }
    //======================== DSS =====================================


    static makeDSSRequest(wp, survey, sizeInDeg) {
        return this.makePlotServiceReq(ServiceType.DSS, wp, survey, sizeInDeg);
    }

    //======================== Wise =====================================


    static makeWiseRequest(wp, survey, band, sizeInDeg) {
        var req = this.makePlotServiceReq(ServiceType.WISE, wp, survey, sizeInDeg);
        req.setParam(C.SURVEY_KEY_BAND, band + "");
        var sDesc= survey.toLowerCase()==="3a"  ? "Atlas" : survey;
        req.setTitle("Wise: "+sDesc+ ", B"+ band);
        return req;
    }

    //======================== DSS or IRIS =====================================

    static makeDSSOrIRISRequest(wp, dssSurvey, IssaSurvey, sizeInDeg) {
        var r = this.makePlotServiceReq(ServiceType.DSS_OR_IRIS, wp, dssSurvey, sizeInDeg);
        r.setSurveyKeyAlt(IssaSurvey);
        return r;
    }

    //======================== All Sky =====================================


    static makeAllSkyPlotRequest() {
        return new WebPlotRequest(RequestType.ALL_SKY, "All Sky Image");
    }



    //======================== Blank =====================================
    static makeBlankPlotRequest(wp, arcsecSize, plotWidth, plotHeight ) {
        var r= new WebPlotRequest(RequestType.BLANK, "");
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

    setShowTitleArea(show) { this.setParam(C.SHOW_TITLE_AREA, show + ""); }

    getShowTitleArea() { return this.getBooleanParam(C.SHOW_TITLE_AREA); }

    getUserDesc() { return this.getParam(C.USER_DESC); }

    /**
     *
     * @param option TitleOptions
     */
    setTitleOptions(option) { this.setParam(C.TITLE_OPTIONS,option.value); }

    /**
     *
     * @return TitleOptions
     */
    getTitleOptions() {
        return TitleOptions.get(this.getParam(C.ZOOM_TYPE)) || TitleOptions.NONE;
    }

    /**
     *
     * @param option ExpandedTitleOptions
     */
    setExpandedTitleOptions(option) { this.setParam(C.EXPANDED_TITLE_OPTIONS,option.value); }

    /**
     *
     * @return ExpandedTitleOptions
     */
    getExpandedTitleOptions() {
        return ExpandedTitleOptions.get(this.getParam(C.ZOOM_TYPE)) || ExpandedTitleOptions.REPLACE;
    }




    setPreTitle(preTitle) { this.setParam(C.PRE_TITLE, preTitle); }
    getPreTitle() { return this.getParam(C.PRE_TITLE); }


    setPostTitle(postTitle) { this.setParam(C.POST_TITLE, postTitle); }
    getPostTitle() { return this.getParam(C.POST_TITLE); }

    setTitleFilenameModePfx(pfx) { this.setParam(C.TITLE_FILENAME_MODE_PFX, pfx); }
    getTitleFilenameModePfx() { return this.getParam(C.TITLE_FILENAME_MODE_PFX); }


//======================================================================
//----------------------- Overlay Settings ------------------------------
//======================================================================

    /**
     *
     * @param worldPt WorldPt
     */
    setOverlayPosition(worldPt) {
        this.setParam(C.OVERLAY_POSITION, worldPt.toString());
    }

    /**
     *
     * @return WorldPt
     */
    getOverlayPosition() { return this.getWorldPtParam(C.OVERLAY_POSITION); }

//======================================================================
//----------------------- Color Settings ------------------------------
//======================================================================

    /**
     *
     * @param id integer, color table id number
     */
    setInitialColorTable(id) { this.setParam(C.INIT_COLOR_TABLE, id + ""); }

    /**
     *
     * @return integer, color table id number
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
            this.setParam(C.INIT_RANGE_VALUES, rangeValues.serialize());
        }
        else {
            this.removeParam(C.INIT_RANGE_VALUES);
        }
    }

    /**
     *
     * @return RangeValues
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
     * @param width the width in pixels
     * @see ZoomType
     */
    setZoomToWidth(width) { this.setParam(C.ZOOM_TO_WIDTH, width + ""); }

    getZoomToWidth() {
        return this.containsParam(C.ZOOM_TO_WIDTH) ? this.getIntParam(C.ZOOM_TO_WIDTH) : 0;
    }

    /**
     * Certain zoom types require the height of the viewable area to determine the zoom level
     * used with ZoomType.FULL_SCREEN, ZoomType.TO_HEIGHT (to height, no yet implemented)
     *
     * @param height the height in pixels
     * @see ZoomType
     */
    setZoomToHeight(height) {
        this.setParam(C.ZOOM_TO_HEIGHT, height + "");
    }

    getZoomToHeight() {
        return this.containsParam(C.ZOOM_TO_HEIGHT) ? this.getIntParam(C.ZOOM_TO_HEIGHT) : 0;
    }


    /**
     * set the initialize zoom level, this is used with ZoomType.STANDARD
     *
     * @param zl the zoom level, float
     * @see ZoomType
     */
    setInitialZoomLevel(zl) {
        this.setParam(C.INIT_ZOOM_LEVEL, zl + "");
    }

    /**
     *
     * @return number, the zoom level
     */
    getInitialZoomLevel() {
        return this.getFloatParam(C.INIT_ZOOM_LEVEL,1);
    }

    /**
     * @deprecated
     * Check if the zoom type is a smart type, the parameter is optional. if not passed the
     * function will look at the object for the value
     * @param testType optional, ZoomType
     * @return boolean, true is the type is smart
     */
    isSmartZoom(testType) {
        var type= testType||this.getZoomType();
        return (ZoomType.SMART.is(type) ||
                ZoomType.SMART_SMALL.is(type) ||
                ZoomType.SMART_LARGE.is(type));
    }

    /**
     *
     * @param hasMax boolean
     */
    setHasMaxZoomLevel(hasMax) { this.setParam(C.HAS_MAX_ZOOM_LEVEL, hasMax +""); }

    hasMaxZoomLevel() { return this.getBooleanParam(C.HAS_MAX_ZOOM_LEVEL); }

    /**
     * sets the zoom type, based on the ZoomType other zoom set methods may be required
     * Notes for ZoomType:
     * <ul>
     * <li>ZoomType.STANDARD is the default, when set you may optionally call
     * setInitialZoomLevel the zoom will default to be 1x</li>
     * <li>if ZoomType.TO_WIDTH then you must call setZoomToWidth and set a width </li>
     * <li>if ZoomType.FULL_SCREEN then you must call setZoomToWidth with a width and
     * setZoomToHeight with a height</li>
     * <li>if ZoomType.ARCSEC_PER_SCREEN_PIX then you must call setZoomArcsecPerScreenPix</li>
     * </ul>
     *
     * @param zoomType affect how the zoom is computed
     * @see ZoomType
     */
    setZoomType(zoomType) {
        if (zoomType) this.setParam(C.ZOOM_TYPE, zoomType.value);
    }

    getZoomType() {
        return ZoomType.get(this.getParam(C.ZOOM_TYPE)) ||ZoomType.STANDARD;
    }

    /**
     * set the arcseconds per screen pixel that will be used to determine the zoom level.
     * Used with ZoomType.ARCSEC_PER_SCREEN_PIX
     *
     * @param arcsecSize, number
     * @see ZoomType
     */
    setZoomArcsecPerScreenPix(arcsecSize) {
        this.setParam(C.ZOOM_ARCSEC_PER_SCREEN_PIX, arcsecSize + "");
    }

    /**
     *
     * @return number
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
     * @param rotateNorth, boolean, true to rotate
     */
    setRotateNorth(rotateNorth) { this.setParam(C.ROTATE_NORTH, rotateNorth + ""); }

    /**
     *
     * @return boolean
     */
    getRotateNorth() { return this.getBooleanParam(C.ROTATE_NORTH); }

    /**
     * Plot should come up rotated north, unless the user has already set the rotation using the button
     *
     * @param rotateNorth true to rotate
     */
    setRotateNorthSuggestion(rotateNorth) {
        this.setParam(C.ROTATE_NORTH_SUGGESTION, rotateNorth + "");
    }

    /**
     *
     * @return boolean
     */
    getRotateNorthSuggestion() { return this.getBooleanParam(C.ROTATE_NORTH_SUGGESTION); }

    /**
     * Set to coordinate system for rotate north, eq j2000 is the default
     *
     * @param rotateNorthType CoordinateSys, default CoordinateSys.EQ_J2000
     */
    setRotateNorthType(rotateNorthType) {
        this.setParam(C.ROTATE_NORTH_TYPE, rotateNorthType.toString());
    }

    /**
     *
     * @return CoordinateSys
     */
    getRotateNorthType() {
        var cStr = this.getParam(C.ROTATE_NORTH_TYPE);
        var retval = null;
        if (cStr !== null) retval = CoordinateSys.parse(cStr);
        if (retval === null) retval = CoordinateSys.EQ_J2000;
        return retval;
    }

    /**
     * set to rotate, if true, the angle should also be set
     *
     * @param rotate boolean, true to rotate
     */
    setRotate(rotate) { this.setParam(C.ROTATE, rotate + ""); }

    /**
     * @return boolean,  true if rotate, false otherwise
     */
    getRotate() { return this.getBooleanParam(C.ROTATE); }


    /**
     * set the angle to rotate to
     *
     * @param rotationAngle  number, the angle in degrees to rotate to
     */
    setRotationAngle(rotationAngle) { this.setParam(C.ROTATION_ANGLE, rotationAngle + ""); }

    /**
     * @return number, the angle
     */
    getRotationAngle() { return this.getFloatParam(C.ROTATION_ANGLE); }

    /**
     * set if this image should be flipped on the Y axis
     * @param flipY boolean, true to flip, false not to flip
     */
    setFlipY(flipY) { this.setParam(C.FLIP_Y,flipY+""); }


    /**
     * @return boolean, flip true or false
     */
    isFlipY() { return this.getBooleanParam(C.FLIP_Y); }

    /**
     * set if this image should be flipped on the X axis
     * @param flipX boolean, true to flip, false not to flip
     */
    setFlipX(flipX) { this.setParam(C.FLIP_X,flipX+""); }


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
    setPostCrop(postCrop) { this.setParam(C.POST_CROP, postCrop + ""); }

    /**
     * @return boolean, do the post crop
     */
    getPostCrop() { return this.getBooleanParam(C.POST_CROP); }


    /**
     * set the post crop
     * @param postCrop boolean
     */
    setPostCropAndCenter(postCrop) { this.setParam(C.POST_CROP_AND_CENTER, postCrop + ""); }

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
        var cStr= this.getParam(C.POST_CROP_AND_CENTER_TYPE);
        var retval= null;
        if (cStr!==null)  retval= CoordinateSys.parse(cStr);
        if (retval===null) retval= CoordinateSys.EQ_J2000;
        return retval;
    }

    setCropPt1(pt1) {
        if (pt1) this.setParam((pt1 instanceof WorldPt) ? C.CROP_WORLD_PT1 : C.CROP_PT1, pt1.toString());
    }

    /**
     *  @return imagePt
     */
    getCropImagePt1() { return ImagePt.parse(this.getParam(C.CROP_PT1)); }

    setCropPt2(pt2) {
        if (pt2) this.setParam((pt2 instanceof WorldPt) ? C.CROP_WORLD_PT2 : C.CROP_PT2, pt2.toString());
    }

    /**
     *  @return imagePt
     */
    getCropImagePt2() { return ImagePt.parse(this.getParam(C.CROP_PT2)); }


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
     * @param arcsecSize float,  the size of the pixels in arcsec
     * @see RequestType
     */
    setBlankArcsecPerPix(arcsecSize) { this.setParam(C.BLANK_ARCSEC_PER_PIX, arcsecSize + ""); }

    /**
     * @return float
     */
    getBlankArcsecPerPix() { return this.getFloatParam(C.BLANK_ARCSEC_PER_PIX,0); }

    /**
     * @param width int
     */
    setBlankPlotWidth(width) { this.setParam(C.BLANK_PLOT_WIDTH, width + ""); }

    /**
     * @return width int
     */
    getBlankPlotWidth() {
        return this.getIntParam(C.BLANK_PLOT_WIDTH,0);
    }


    /**
     * @param height int
     */
    setBlankPlotHeight(height) { this.setParam(C.BLANK_PLOT_HEIGHT, height + ""); }

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

    /**
     *
     * @param service ServiceType
     */
    setServiceType(service) { this.setParam(C.SERVICE, service.value); }

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
    setRequestType(type) { this.setParam(C.TYPE, type.value); }

    /**
     *
     * @return ServiceType
     */
    getServiceType() {
        return ServiceType.get(this.getParam(C.SERVICE)) ||ServiceType.NONE;
    }

    /**
     * @param key string
     */
    setSurveyKey(key) { this.setParam(C.SURVEY_KEY, key); }

    /**
     * @return key string
     */
    getSurveyKey() { return this.getParam(C.SURVEY_KEY); }

    /**
     * @param key string
     */
    setSurveyKeyAlt(key) { this.setParam(C.SURVEY_KEY_ALT, key); }

    /**
     * @return key string
     */
    getSurveyKeyAlt() { return this.getParam(C.SURVEY_KEY_ALT); }

    /**
     * @return key string
     */
    getSurveyBand() { return this.getParam(C.SURVEY_KEY_BAND); }

//======================================================================
//----------------------- Object & Area Settings -----------------------
//======================================================================

    /**
     * @return objectName, string, astronomical object to search
     */
    setObjectName(objectName) { this.setParam(C.OBJECT_NAME, objectName); }

    /**
     * @return astronomical object, string
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
    getResolver() { return Resolver.parse(this.getParam(C.RESOLVER)); }

    /**
     *
     * @param worldPt WorldPt
     */
    setWorldPt(worldPt) {
        if (worldPt) this.setParam(C.WORLD_PT, worldPt.toString());
    }

    /**
     * @return WorldPt
     */
    getWorldPt() { return this.getWorldPtParam(C.WORLD_PT); }


    setSizeInDeg(sizeInDeg) { this.setParam(C.SIZE_IN_DEG, sizeInDeg + ""); }
    getSizeInDeg() { return this.getFloatParam(C.SIZE_IN_DEG, NaN); }

//======================================================================
//----------------------- Other Settings --------------------------------
//======================================================================

    /**
     * @param multi boolean
     */
    setMultiImageSupport(multi) { this.setParam(C.MULTI_IMAGE_FITS, multi + ""); }

    getMultiImageSupport() { return this.getBooleanParam(C.MULTI_IMAGE_FITS); }

    setMultiImageIdx(idx) { this.setParam(C.MULTI_IMAGE_IDX, idx + ""); }

    /**
     * @return number index of image
     */
    getMultiImageIdx() { return this.getIntParam(C.MULTI_IMAGE_IDX,0); }

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
    setSaveCorners(save) { this.setParam(C.SAVE_CORNERS, save + ""); }

    /**
     * @return boolean
     */
    getSaveCorners() { return this.getBooleanParam(C.SAVE_CORNERS); }


    /**
     * @param allowImageSelection boolean
     */
    setAllowImageSelection(allowImageSelection) {
        this.setParam(C.ALLOW_IMAGE_SELECTION, allowImageSelection + "");
    }

    /**
     * @return boolean
     */
    isAllowImageSelection() { return this.getBooleanParam(C.ALLOW_IMAGE_SELECTION); }

    /**
     * @param allowImageSelectionCreateNew boolean
     */
    setHasNewPlotContainer(allowImageSelectionCreateNew) {
        this.setParam(C.HAS_NEW_PLOT_CONTAINER, allowImageSelectionCreateNew + "");
    }

    getHasNewPlotContainer() { this.getBooleanParam(C.HAS_NEW_PLOT_CONTAINER); }



    setAdvertise(advertise)  { this.setParam(C.ADVERTISE, advertise + ""); }

    isAdvertise() { return this.getBooleanParam(C.ADVERTISE); }

    /**
     *
     * @param gridOnStatus GridOnStatus
     */
    setGridOn(gridOnStatus) { this.setParam(C.GRID_ON, gridOnStatus.value); }

    /**
     *
     * @return GridOnStatus
     */
    getGridOn() {
        return GridOnStatus.get(this.getParam(C.GRID_ON)) ||GridOnStatus.FALSE;
    }

    /**
     * @param hideTitleZoomLevel boolean
     */
    setHideTitleDetail(hideTitleZoomLevel)  { this.setParam(C.HIDE_TITLE_DETAIL, hideTitleZoomLevel + ""); }

    /**
     * @return boolean
     */
    getHideTitleDetail() { return this.getBooleanParam(C.HIDE_TITLE_DETAIL); }

    /**
     * @param thumbnailSize int
     */
    setThumbnailSize(thumbnailSize) { this.setParam(C.THUMBNAIL_SIZE, thumbnailSize+""); }

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
    setContinueOnFail(continueOnFail) { this.setParam(C.CONTINUE_ON_FAIL, continueOnFail + ""); }

    isContinueOnFail() { return this.getBooleanParam(C.CONTINUE_ON_FAIL); }

    setUniqueKey(key) { this.setParam(C.UNIQUE_KEY, key); }

    getUniqueKey() { return this.getParam(C.UNIQUE_KEY); }

    setPlotToDiv(div) { this.setParam(C.PLOT_TO_DIV, div); }

    getPlotToDiv() { return this.getParam(C.PLOT_TO_DIV); }


    setHeaderKeyForTitle(headerKey) { this.setParam(C.HEADER_KEY_FOR_TITLE, headerKey); }

    getHeaderKeyForTitle() { return this.getParam(C.HEADER_KEY_FOR_TITLE); }


    /**
     * @param showBars boolean
     */
    setShowScrollBars(showBars) { this.setParam(C.SHOW_SCROLL_BARS, showBars + ""); }

    /**
     * @return boolean
     */
    getShowScrollBars() { return this.getBooleanParam(C.SHOW_SCROLL_BARS); }

    setProgressKey(key) { this.setParam(C.PROGRESS_KEY,key); }

    getProgressKey() { return this.getParam(C.PROGRESS_KEY); }

    /**
     * @param minimalReadout boolean
     */
    setMinimalReadout(minimalReadout) { this.setParam(C.MINIMAL_READOUT,minimalReadout+""); }

    /**
     * @return boolean
     */
    isMinimalReadout() { return this.getBooleanParam(C.MINIMAL_READOUT); }

    setDrawingSubGroupId(id) { this.setParam(C.DRAWING_SUB_GROUP_ID,id); }

    getDrawingSubGroupId() { return this.getParam(C.DRAWING_SUB_GROUP_ID); }

    setGridId(id) { this.setParam(C.GRID_ID,id); }

    getGridId() { return this.getParam(C.GRID_ID); }

    setDownloadFileNameRoot(nameRoot) { this.setParam(C.DOWNLOAD_FILENAME_ROOT, nameRoot); }

    getDownloadFileNameRoot() { return this.getParam(C.DOWNLOAD_FILENAME_ROOT); }

    /**
     * Set the order that the image processing pipeline runs when it reads a fits file.
     * This is experimental.  Use at your own risk.
     * Warning- if you exclude an Order elements the pipeline will not execute that process
     * even is you have it set in the option.
     * @param orderList array of Order enums, the order of the pipeline
     */
    setPipelineOrder(orderList) {
        var out= orderList.reduce((str,v,idx,ary)=> {
            return   str+ v.value + (idx===ary.length-1 ? "": ";");
        },"");
        this.setParam(C.PIPELINE_ORDER, out);
    }

    /**
     * @return array of Order enums
     */
    getPipelineOrder() {
        var retList;
        if (this.containsParam(C.PIPELINE_ORDER)) {
            retList= makeOrderList(this.getParam(C.PIPELINE_ORDER));
            if (retList.size()<2) retList= DEF_ORDER;
        }
        else {
            retList= DEF_ORDER;
        }
        return retList;
    }


    /**
     * Return the request area
     * i am using circle but it is really size not radius - todo: fix this
     *
     * @return an area to select
     */
    getRequestArea() {
        var retval = null;
        var wp= this.getWorldPt();
        var side = this.getSizeInDeg();
        if (wp) retval = {center:wp, radius:side};
        return retval;
    }


    prettyString() {
        var s = "WebPlotRequest: ";
        switch (this.getRequestType()) {
            case RequestType.SERVICE:
                switch (this.getServiceType()) {
                    case ServiceType.IRIS:
                    case ServiceType.DSS:
                    case ServiceType.TWOMASS:
                        if (this.containsParam(C.WORLD_PT)) {
                            s += this.getServiceType().value + "- " + this.getRequestArea();
                        }
                        else {
                            s += this.getServiceType().value + "- Obj name: " + this.getObjectName() +
                            ", radius: " +this.getParam(C.SIZE_IN_DEG);
                        }
                        break;
                }
                break;
            case RequestType.FILE:
                s += " File: " + this.getFileName();
                break;
            case RequestType.URL:
                s += " URL: " + this.getURL();
                break;
            case RequestType.ALL_SKY:
                s += " AllSky";
                break;
            case RequestType.PROCESSOR:
                s += "File Search Processor: "+ this.getRequestId();
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

    /*
    * This method can only be used for those services that take the standard
    * ra, dec, radius approach (so far, iras, issa, 2mass, dss)
    * @param ctxStr the context  string
    * @param serviceType the network service type
    * @param wp the center point WorldPt
    * @param sizeInDeg size in degrees
    * @return the PlotRequest object that was constructed
    */
    makePlotServiceReq(serviceType, wp, survey, sizeInDeg) {
        var desc = this.makeServiceReqDesc(serviceType, survey, sizeInDeg);
        var req = new WebPlotRequest(RequestType.SERVICE, serviceType, desc);
        req.setSurveyKey(survey);
        req.setWorldPt(wp);
        req.setSizeInDeg(sizeInDeg);
        return req;
    }

    static makeServiceReqDesc(serviceType, survey, sizeInDeg) {
        return serviceType.value + ": " + survey + ", " + sizeInDeg + " Deg";
    }


    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     *
     * @param str the serialized WebPlotRequest
     * @return the deserialized WebPlotRequest
     */
    static parse(str) {
        return ServerRequest.parse(str, new WebPlotRequest());
    }

    makeCopy() {
        var retval = new WebPlotRequest();
        retval.copyFrom(this);
        return retval;
    }

    static makeCopyOfRequest(r) {
        return r ? null : r.makeCopy();
    }

    static getAllKeys() { return _allKeys; }

    static getClientKeys() { return _clientSideKeys; }

    /**
     * Perform equals but ignore layout params, such as zoom type, width and height
     * @param obj
     * @return boolean
     */
    equalsPlottingParams(obj) {
        var retval= false;
        if (obj instanceof WebPlotRequest) {
            var wpr1= this.makeCopy();
            var wpr2= obj.makeCopy();
            _ignoreForEquals.forEach(key=> {
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

export default WebPlotRequest;
