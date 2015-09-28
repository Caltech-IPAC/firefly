/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.Circle;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * User: roby
 * Date: Apr 2, 2009
 * Time: 9:18:47 AM
 */


/**
 * @author Trey Roby
 */
public class WebPlotRequest extends ServerRequest {

    public enum
            ServiceType {IRIS, ISSA, DSS, SDSS, TWOMASS, MSX, DSS_OR_IRIS, WISE, NONE}
    public enum TitleOptions {NONE,  // use what it in the title
                              PLOT_DESC, // use the plot description key
                              FILE_NAME, // use the file name or analyze the URL and make a title from that
                              HEADER_KEY, // use the header value
                              PLOT_DESC_PLUS, // ??
                              SERVICE_OBS_DATE,
                             }
    public enum ExpandedTitleOptions {
        REPLACE,// use expanded title when expanded
        PREFIX,// use expanded title as prefix to title
        SUFFIX,// use expanded title as sufix to title
    }
    public enum GridOnStatus {FALSE,TRUE,TRUE_LABELS_FALSE}
    public static final int DEFAULT_THUMBNAIL_SIZE= 70;

    public static final String WEB_PLOT_REQUEST_CLASS= "WebPlotRequest";

    //keys
    // note- if you add a new key make sure you put it in the _allKeys array
    public static final String FILE = "File";
    public static final String WORLD_PT = "WorldPt";
    public static final String URL = "URL";
    public static final String SIZE_IN_DEG = "SizeInDeg";
    public static final String SURVEY_KEY = "SurveyKey";
    public static final String SURVEY_KEY_ALT = "SurveyKeyAlt";
    public static final String SURVEY_KEY_BAND = "SurveyKeyBand";
    public static final String TYPE = "Type";
    public static final String ZOOM_TYPE = "ZoomType";
    public static final String SERVICE = "Service";
    public static final String USER_DESC = "UserDesc";
    public static final String INIT_ZOOM_LEVEL = "InitZoomLevel";
    public static final String TITLE = "Title";
    public static final String ROTATE_NORTH = "RotateNorth";
    public static final String ROTATE_NORTH_TYPE = "RotateNorthType";
    public static final String ROTATE = "Rotate";
    public static final String ROTATION_ANGLE = "RotationAngle";
    public static final String HEADER_KEY_FOR_TITLE = "HeaderKeyForTitle";
    public static final String INIT_RANGE_VALUES = "RangeValues";
    public static final String INIT_COLOR_TABLE = "ColorTable";
    public static final String MULTI_IMAGE_FITS = "MultiImageFits";
    public static final String MULTI_IMAGE_IDX = "MultiImageIdx";
    public static final String ZOOM_TO_WIDTH = "ZoomToWidth";
    public static final String ZOOM_TO_HEIGHT = "ZoomToHeight";
    public static final String ZOOM_ARCSEC_PER_SCREEN_PIX = "ZoomArcsecPerScreenPix";
    public static final String POST_CROP = "PostCrop";
    public static final String POST_CROP_AND_CENTER = "PostCropAndCenter";
    public static final String POST_CROP_AND_CENTER_TYPE = "PostCropAndCenterType";
    public static final String CROP_PT1 = "CropPt1";
    public static final String CROP_PT2 = "CropPt2";
    public static final String CROP_WORLD_PT1 = "CropWorldPt1";
    public static final String CROP_WORLD_PT2 = "CropWorldPt2";
    public static final String UNIQUE_KEY = "UniqueKey";
    public static final String CONTINUE_ON_FAIL = "ContinueOnFail";
    public static final String OBJECT_NAME = "ObjectName";
    public static final String RESOLVER = "Resolver";
    public static final String PLOT_DESC_APPEND = "PlotDescAppend";
    public static final String BLANK_ARCSEC_PER_PIX = "BlankArcsecPerScreenPix";  //todo: doc
    public static final String BLANK_PLOT_WIDTH = "BlankPlotWidth";               //todo: doc
    public static final String BLANK_PLOT_HEIGHT = "BlankPlotHeight";             //todo: doc
    public static final String PROGRESS_KEY = "ProgressKey";
    public static final String FLIP_Y = "FlipY";
    public static final String FLIP_X = "FlipX";
    public static final String HAS_MAX_ZOOM_LEVEL = "HasMaxZoomLevel";
    public static final String THUMBNAIL_SIZE = "thumbnailSize";
    public static final String PIPELINE_ORDER = "pipelineOrder"; // todo: convert, doc, add to allKeys

    public static final String MASK_BITS= "MaskBits";
    public static final String PLOT_AS_MASK= "PlotAsMask";
    public static final String MASK_COLORS= "MaskColors";
    public static final String MASK_REQUIRED_WIDTH= "MaskRequiredWidth";
    public static final String MASK_REQUIRED_HEIGHT= "MaskRequiredHeight";

    // keys - client side operations
    // note- if you add a new key make sure you put it in the _allKeys array
    public static final String PLOT_TO_DIV = "PlotToDiv";
    public static final String PREFERENCE_COLOR_KEY = "PreferenceColorKey";
    public static final String PREFERENCE_ZOOM_KEY = "PreferenceZoomKey";
    public static final String SHOW_TITLE_AREA = "ShowTitleArea";
    public static final String ROTATE_NORTH_SUGGESTION = "RotateNorthSuggestion";
    public static final String SAVE_CORNERS = "SaveCornersAfterPlot";
    public static final String SHOW_SCROLL_BARS = "showScrollBars";
    public static final String EXPANDED_TITLE = "ExpandedTitle";
    public static final String ALLOW_IMAGE_SELECTION = "AllowImageSelection";
    public static final String HAS_NEW_PLOT_CONTAINER = "HasNewPlotContainer";
    public static final String ADVERTISE = "Advertise";
    public static final String HIDE_TITLE_DETAIL = "HideTitleDetail";
    public static final String GRID_ON = "GridOn";
    public static final String TITLE_OPTIONS = "TitleOptions";
    public static final String EXPANDED_TITLE_OPTIONS = "ExpandedTitleOptions";
    public static final String POST_TITLE= "PostTitle";
    public static final String PRE_TITLE= "PreTitle";
    public static final String TITLE_FILENAME_MODE_PFX = "TitleFilenameModePfx";
    public static final String OVERLAY_POSITION = "OverlayPosition";
    public static final String MINIMAL_READOUT= "MinimalReadout";
    public static final String DRAWING_SUB_GROUP_ID= "DrawingSubgroupID";
    public static final String GRID_ID = "GRID_ID";
    public static final String DOWNLOAD_FILENAME_ROOT = "DownloadFileNameRoot";
    public static final String PLOT_ID = "PlotID";

    private static final String _allKeys[] = {FILE, WORLD_PT, URL, SIZE_IN_DEG, SURVEY_KEY,
                                              SURVEY_KEY_ALT, SURVEY_KEY_BAND, TYPE, ZOOM_TYPE,
                                              SERVICE, USER_DESC, INIT_ZOOM_LEVEL,
                                              TITLE, ROTATE_NORTH, ROTATE_NORTH_TYPE, ROTATE, ROTATION_ANGLE,
                                              HEADER_KEY_FOR_TITLE,
                                              INIT_RANGE_VALUES, INIT_COLOR_TABLE, MULTI_IMAGE_FITS,MULTI_IMAGE_IDX,
                                              ZOOM_TO_WIDTH, ZOOM_TO_HEIGHT,
                                              POST_CROP, POST_CROP_AND_CENTER, FLIP_X, FLIP_Y,
                                              HAS_MAX_ZOOM_LEVEL,
                                              POST_CROP_AND_CENTER_TYPE, CROP_PT1, CROP_PT2, CROP_WORLD_PT1, CROP_WORLD_PT2,
                                              ZOOM_ARCSEC_PER_SCREEN_PIX, CONTINUE_ON_FAIL, OBJECT_NAME, RESOLVER,
                                              BLANK_ARCSEC_PER_PIX, BLANK_PLOT_WIDTH, BLANK_PLOT_HEIGHT,

                                              UNIQUE_KEY,
                                              PLOT_TO_DIV, PREFERENCE_COLOR_KEY, PREFERENCE_ZOOM_KEY,
                                              SHOW_TITLE_AREA, ROTATE_NORTH_SUGGESTION, SAVE_CORNERS,
                                              SHOW_SCROLL_BARS, EXPANDED_TITLE, PLOT_DESC_APPEND, HIDE_TITLE_DETAIL,
                                              ALLOW_IMAGE_SELECTION, HAS_NEW_PLOT_CONTAINER,
                                              GRID_ON, TITLE_OPTIONS, EXPANDED_TITLE_OPTIONS,
                                              POST_TITLE, PRE_TITLE, OVERLAY_POSITION,
                                              TITLE_FILENAME_MODE_PFX, MINIMAL_READOUT, DRAWING_SUB_GROUP_ID, GRID_ID,
                                              DOWNLOAD_FILENAME_ROOT, PLOT_ID

    };

    private static final String _clientSideKeys[] = {UNIQUE_KEY,
                                                     PLOT_TO_DIV, PREFERENCE_COLOR_KEY, PREFERENCE_ZOOM_KEY,
                                                     SHOW_TITLE_AREA, ROTATE_NORTH_SUGGESTION, SAVE_CORNERS,
                                                     SHOW_SCROLL_BARS, EXPANDED_TITLE,
                                                     ALLOW_IMAGE_SELECTION, HAS_NEW_PLOT_CONTAINER,
                                                     ADVERTISE, HIDE_TITLE_DETAIL, GRID_ON,
                                                     TITLE_OPTIONS, EXPANDED_TITLE_OPTIONS,
                                                     POST_TITLE, PRE_TITLE, OVERLAY_POSITION,
                                                     TITLE_FILENAME_MODE_PFX, MINIMAL_READOUT,
                                                     DRAWING_SUB_GROUP_ID, GRID_ID,
                                                     DOWNLOAD_FILENAME_ROOT, PLOT_ID
    };


    private static final String _ignoreForEquals[] = {PROGRESS_KEY, ZOOM_TO_WIDTH, ZOOM_TO_HEIGHT, ZOOM_TYPE, HAS_NEW_PLOT_CONTAINER};


    public enum Order {FLIP_Y, FLIP_X, ROTATE, POST_CROP, POST_CROP_AND_CENTER}

    public static final String DEFAULT_PIPELINE_ORDER= Order.ROTATE+";"+
                                                       Order.FLIP_Y+";"+
                                                       Order.FLIP_X+";"+
                                                       Order.POST_CROP+";"+
                                                       Order.POST_CROP_AND_CENTER;
    private static List<Order> defOrder= makeOrderList(DEFAULT_PIPELINE_ORDER);


    public static final String MULTI_PLOT_KEY= "MultiPlotKey";
    public static final String THREE_COLOR_PLOT_KEY= "ThreeColorPlotKey";
    public static final String THREE_COLOR_HINT= "ThreeColorHint";
    public static final String RED_HINT= "RedHint";
    public static final String GREEN_HINT= "GreenHint";
    public static final String BLUE_HINT= "BlueHint";

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public WebPlotRequest() {
        setRequestClass(WEB_PLOT_REQUEST_CLASS);
    }

    private WebPlotRequest(RequestType type, String userDesc) {
        this(type, userDesc, ServiceType.NONE);
    }

    private WebPlotRequest(RequestType type, String userDesc, ServiceType service) {
        super(type.toString());
        setRequestClass(WEB_PLOT_REQUEST_CLASS);
        setRequestType(type);
        if (!service.equals(ServiceType.NONE)) setServiceType(service);
        setParam(USER_DESC, userDesc);
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
     * @param r the request
     * @return the new WebPlotRequest
     */
    public static WebPlotRequest makeRequest(ServerRequest r) {
        WebPlotRequest retval;
        if (r instanceof WebPlotRequest) {
            retval= (WebPlotRequest)r;
        }
        else {
            retval = new WebPlotRequest(RequestType.FILE, "Fits File");
            retval.setParams(r.getParams());
            retval.removeParam(ServerRequest.ID_KEY);
        }
        return retval;
    }


    public static WebPlotRequest makeFilePlotRequest(String fileName) {
        return makeFilePlotRequest(fileName, 1.0F);
    }

    public static WebPlotRequest makeFilePlotRequest(String fileName, float initZoomLevel) {
        WebPlotRequest req = new WebPlotRequest(RequestType.FILE, "Fits file: " + fileName);
        req.setParam(FILE, fileName);
        req.setParam(INIT_ZOOM_LEVEL, initZoomLevel + "");
        return req;
    }

    public static WebPlotRequest makeProcessorRequest(ServerRequest serverRequest, String desc) {
        WebPlotRequest req = new WebPlotRequest(RequestType.PROCESSOR, desc);
        req.setParams(serverRequest.getParams());
        return req;
    }

    public static WebPlotRequest makeRawDatasetProcessorRequest(TableServerRequest request, String desc) {
        ServerRequest sreq = new ServerRequest(request.getRequestId());
        sreq.setParam(ServerParams.REQUEST, request.toString());
        WebPlotRequest req = new WebPlotRequest(RequestType.RAWDATASET_PROCESSOR, desc);
        req.setParams(sreq.getParams());
        return req;
    }


    public static WebPlotRequest makeURLPlotRequest(String url) {
        WebPlotRequest req = new WebPlotRequest(RequestType.URL, "Fits from URL: " + url);
        req.setURL(url);
        return req;
    }

    public static WebPlotRequest makeURLPlotRequest(String url, String userDesc) {
        WebPlotRequest req = new WebPlotRequest(RequestType.URL, userDesc);
        req.setURL(url);
        return req;
    }

    public static WebPlotRequest makeTblFilePlotRequest(String fileName) {
        WebPlotRequest req = new WebPlotRequest(RequestType.FILE, "Table: " + fileName);
        req.setParam(FILE, fileName);
        return req;
    }


    public static WebPlotRequest makeTblURLPlotRequest(String url) {
        WebPlotRequest req = new WebPlotRequest(RequestType.URL, "Table from URL: " + url);
        req.setParam(URL, url);
        return req;
    }

    //======================== ISSA =====================================


    public static WebPlotRequest makeISSARequest(WorldPt wp,
                                                 String survey,
                                                 float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.ISSA, wp, survey, sizeInDeg);
        req.setTitle("ISSA: "+survey);
        return req;
    }

    //======================== IRIS =====================================


    public static WebPlotRequest makeIRISRequest(WorldPt wp,
                                                 String survey,
                                                 float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.IRIS, wp, survey, sizeInDeg);
        req.setTitle("IRIS: "+survey);
        return req;
    }


    //======================== 2MASS =====================================

    public static WebPlotRequest make2MASSRequest(WorldPt wp,
                                                  String survey,
                                                  float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.TWOMASS, wp, survey, sizeInDeg);
        req.setTitle("2MASS: "+survey.toUpperCase());
        return req;
    }

    //======================== MSX =====================================

    public static WebPlotRequest makeMSXRequest(WorldPt wp,
                                                String survey,
                                                float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.MSX, wp, survey, sizeInDeg);
        req.setTitle("MSX: "+survey);
        return req;
    }

    //======================== SDSS =====================================


    public static WebPlotRequest makeSloanDSSRequest(WorldPt wp,
                                                     String band,
                                                     float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.SDSS, wp, band, sizeInDeg);
        req.setTitle("SDSS: "+band);
        return req;
    }
    //======================== DSS =====================================


    public static WebPlotRequest makeDSSRequest(WorldPt wp,
                                                String survey,
                                                float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.DSS, wp, survey, sizeInDeg);
        req.setTitle("DSS: "+survey);
        return req;
    }

    //======================== Wise =====================================


    public static WebPlotRequest makeWiseRequest(WorldPt wp,
                                                 String survey,
                                                 String band,
                                                 float sizeInDeg) {
        WebPlotRequest req = makePlotServiceReq(ServiceType.WISE, wp, survey, sizeInDeg);
        req.setParam(SURVEY_KEY_BAND, band + "");
        String sDesc= survey.equalsIgnoreCase("3a")  ? "Atlas" : survey;
        req.setTitle("WISE: "+sDesc+ ", B"+ band);
        return req;
    }

    //======================== DSS or IRIS =====================================

    public static WebPlotRequest makeDSSOrIRISRequest(WorldPt wp,
                                                      String dssSurvey,
                                                      String IssaSurvey,
                                                      float sizeInDeg) {
        WebPlotRequest r = makePlotServiceReq(ServiceType.DSS_OR_IRIS, wp, dssSurvey, sizeInDeg);
        r.setSurveyKeyAlt(IssaSurvey);
        return r;
    }

    //======================== All Sky =====================================


    public static WebPlotRequest makeAllSkyPlotRequest() {
        return new WebPlotRequest(RequestType.ALL_SKY, "All Sky Image");
    }



    //======================== Blank =====================================
    public static WebPlotRequest makeBlankPlotRequest(WorldPt wp,
                                                      float arcsecSize,
                                                      int plotWidth,
                                                      int plotHeight ) {
        WebPlotRequest r= new WebPlotRequest(RequestType.BLANK, "");
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

    public void setTitle(String title) {
        setParam(TITLE, title);
    }

    public String getTitle() {
        return getParam(TITLE);
    }


    public void setExpandedTitle(String title) {
        setParam(EXPANDED_TITLE, title);
    }

    public String getExpandedTitle() {
        return getParam(EXPANDED_TITLE);
    }

    public void setShowTitleArea(boolean show) {
        setParam(SHOW_TITLE_AREA, show + "");
    }

    public boolean getShowTitleArea() {
        return getBooleanParam(SHOW_TITLE_AREA);
    }

    public String getUserDesc() {
        return getParam(USER_DESC);
    }

    public void setTitleOptions(TitleOptions option) {
        setParam(TITLE_OPTIONS,option.toString());
    }

    public TitleOptions getTitleOptions() {
        TitleOptions retval = TitleOptions.NONE;
        if (this.containsParam(TITLE_OPTIONS)) {
            retval = Enum.valueOf(TitleOptions.class, getParam(TITLE_OPTIONS));
        }
        return retval;
    }

    public void setExpandedTitleOptions(ExpandedTitleOptions option) {
        setParam(EXPANDED_TITLE_OPTIONS,option.toString());
    }

    public ExpandedTitleOptions getExpandedTitleOptions() {
        ExpandedTitleOptions retval = ExpandedTitleOptions.REPLACE;
        if (this.containsParam(EXPANDED_TITLE_OPTIONS)) {
            retval = Enum.valueOf(ExpandedTitleOptions.class, getParam(EXPANDED_TITLE_OPTIONS));
        }
        return retval;
    }




    public void setPreTitle(String preTitle) { setParam(PRE_TITLE, preTitle); }

    public String getPreTitle() { return getParam(PRE_TITLE); }


    public void setPostTitle(String postTitle) {
        setParam(POST_TITLE, postTitle);
    }

    public String getPostTitle() { return getParam(POST_TITLE); }

    public void setTitleFilenameModePfx(String pfx) {
        setParam(TITLE_FILENAME_MODE_PFX, pfx);
    }

    public String getTitleFilenameModePfx() { return getParam(TITLE_FILENAME_MODE_PFX); }


//======================================================================
//----------------------- Overlay Settings ------------------------------
//======================================================================

    public void setOverlayPosition(WorldPt wp) {
        setParam(OVERLAY_POSITION, wp.toString());
    }

    public WorldPt getOverlayPosition() { return getWorldPtParam(OVERLAY_POSITION); }

//======================================================================
//----------------------- Color Settings ------------------------------
//======================================================================

    public void setInitialColorTable(int id) {
        setParam(INIT_COLOR_TABLE, id + "");
    }

    public int getInitialColorTable() {
        return containsParam(INIT_COLOR_TABLE) ? getIntParam(INIT_COLOR_TABLE) : 0;
    }

    public void setInitialRangeValues(RangeValues rv) {
        if (rv!=null) {
            setParam(INIT_RANGE_VALUES, rv.serialize());
        }
        else {
            removeParam(INIT_RANGE_VALUES);
        }
    }

    public RangeValues getInitialRangeValues() {
        return containsParam(INIT_RANGE_VALUES) ? RangeValues.parse(getParam(INIT_RANGE_VALUES)) : null;
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
    public void setZoomToWidth(int width) {
        setParam(ZOOM_TO_WIDTH, width + "");
    }

    public int getZoomToWidth() {
        return containsParam(ZOOM_TO_WIDTH) ? getIntParam(ZOOM_TO_WIDTH) : 0;
    }

    /**
     * Certain zoom types require the height of the viewable area to determine the zoom level
     * used with ZoomType.FULL_SCREEN, ZoomType.TO_HEIGHT (to height, no yet implemented)
     *
     * @param height the height in pixels
     * @see ZoomType
     */
    public void setZoomToHeight(int height) {
        setParam(ZOOM_TO_HEIGHT, height + "");
    }

    public int getZoomToHeight() {
        return containsParam(ZOOM_TO_HEIGHT) ? getIntParam(ZOOM_TO_HEIGHT) : 0;
    }


    /**
     * set the initialize zoom level, this is used with ZoomType.STANDARD
     *
     * @param zl the zoom level
     * @see ZoomType
     */
    public void setInitialZoomLevel(float zl) {
        setParam(INIT_ZOOM_LEVEL, zl + "");
    }

    public float getInitialZoomLevel() {
        float retval = 1F;
        if (containsParam(INIT_ZOOM_LEVEL)) {
            retval = getFloatParam(INIT_ZOOM_LEVEL);
        }
        return retval;
    }

    public static boolean isSmartZoom(ZoomType type) {
        return type == ZoomType.SMART;
    }

    public boolean isSmartZoom() {
        return isSmartZoom(getZoomType());
    }

    public void setHasMaxZoomLevel(boolean b) {
        setParam(HAS_MAX_ZOOM_LEVEL, b +"");
    }

    public boolean hasMaxZoomLevel() {
        return getBooleanParam(HAS_MAX_ZOOM_LEVEL);
    }

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
    public void setZoomType(ZoomType zoomType) {
        if (zoomType != null) setParam(ZOOM_TYPE, zoomType.toString());
    }

    public ZoomType getZoomType() {
        ZoomType retval = ZoomType.STANDARD;
        if (this.containsParam(ZOOM_TYPE)) {
            retval = Enum.valueOf(ZoomType.class, getParam(ZOOM_TYPE));
        }
        return retval;
    }

    /**
     * set the arcseconds per screen pixel that will be used to determine the zoom level.
     * Used with ZoomType.ARCSEC_PER_SCREEN_PIX
     *
     * @param arcsecSize
     * @see ZoomType
     */
    public void setZoomArcsecPerScreenPix(float arcsecSize) {
        setParam(ZOOM_ARCSEC_PER_SCREEN_PIX, arcsecSize + "");
    }

    public float getZoomArcsecPerScreenPix() {
        return containsParam(ZOOM_ARCSEC_PER_SCREEN_PIX) ? getFloatParam(ZOOM_ARCSEC_PER_SCREEN_PIX) : 0F;
    }

//======================================================================
//----------------------- Rotate  & Flip Settings ----------------------
//======================================================================

    /**
     * Plot should come up rotated north
     *
     * @param rotateNorth true to rotate
     */
    public void setRotateNorth(boolean rotateNorth) {
        setParam(ROTATE_NORTH, rotateNorth + "");
    }

    public boolean getRotateNorth() {
        return getBooleanParam(ROTATE_NORTH);
    }

    /**
     * Plot should come up rotated north, unless the user has already set the rotation using the button
     *
     * @param rotateNorth true to rotate
     */
    public void setRotateNorthSuggestion(boolean rotateNorth) {
        setParam(ROTATE_NORTH_SUGGESTION, rotateNorth + "");
    }

    public boolean getRotateNorthSuggestion() {
        return getBooleanParam(ROTATE_NORTH_SUGGESTION);
    }

    /**
     * Set to coordinate system for rotate north, eq j2000 is the default
     *
     * @param rotateNorthType the CoordinateSys, default CoordinateSys.EQ_J2000
     */
    public void setRotateNorthType(CoordinateSys rotateNorthType) {
        setParam(ROTATE_NORTH_TYPE, rotateNorthType.toString());
    }

    public CoordinateSys getRotateNorthType() {
        String cStr = getParam(ROTATE_NORTH_TYPE);
        CoordinateSys retval = null;
        if (cStr != null) retval = CoordinateSys.parse(cStr);
        if (retval == null) retval = CoordinateSys.EQ_J2000;
        return retval;
    }

    /**
     * set to rotate, if true, the angle should also be set
     *
     * @param rotate true to rotate
     */
    public void setRotate(boolean rotate) {
        setParam(ROTATE, rotate + "");
    }

    public boolean getRotate() {
        return getBooleanParam(ROTATE);
    }


    /**
     * set the angle to rotate to
     *
     * @param rotationAngle the angle in degrees to rotate to
     */
    public void setRotationAngle(double rotationAngle) {
        setParam(ROTATION_ANGLE, rotationAngle + "");
    }

    public double getRotationAngle() {
        return getDoubleParam(ROTATION_ANGLE);
    }

    /**
     * set if this image should be flipped on the Y axis
     * @param flipY true to flip, false not to flip
     */
    public void setFlipY(boolean flipY) { setParam(FLIP_Y,flipY+""); }

    public boolean isFlipY() { return getBooleanParam(FLIP_Y); }

    /**
     * set if this image should be flipped on the Y axis
     * @param flipX true to flip, false not to flip
     */
    public void setFlipX(boolean flipX) { setParam(FLIP_X,flipX+""); }

    public boolean isFlipX() { return getBooleanParam(FLIP_X); }

//======================================================================
//----------------------- Crop Settings --------------------------------
//======================================================================

    /**
     * Crop the image before returning it.  If rotation is set then the crop will happen post rotation.
     * Note: setCropPt1 & setCropPt2 are required to crop
     *
     * @param postCrop do the post crop
     */
    public void setPostCrop(boolean postCrop) {
        setParam(POST_CROP, postCrop + "");
    }

    public boolean getPostCrop() {
        return getBooleanParam(POST_CROP);
    }




    public void setPostCropAndCenter(boolean postCrop) {
        setParam(POST_CROP_AND_CENTER, postCrop + "");
    }

    public boolean getPostCropAndCenter() {
        return getBooleanParam(POST_CROP_AND_CENTER);
    }

    /**
     * Set to coordinate system for crop and center, eq j2000 is the default
     * @param csys the CoordinateSys, default CoordinateSys.EQ_J2000
     */
    public void setPostCropAndCenterType(CoordinateSys csys) {
        setParam(POST_CROP_AND_CENTER_TYPE, csys.toString());
    }

    public CoordinateSys getPostCropAndCenterType() {
        String cStr= getParam(POST_CROP_AND_CENTER_TYPE);
        CoordinateSys retval= null;
        if (cStr!=null)  retval= CoordinateSys.parse(cStr);
        if (retval==null) retval= CoordinateSys.EQ_J2000;
        return retval;
    }

    public void setCropPt1(Pt pt1) {
        if (pt1 != null) {
            setParam((pt1 instanceof WorldPt) ? CROP_WORLD_PT1 : CROP_PT1, pt1.toString());
        }
    }

    public ImagePt getCropImagePt1() {
        ImagePt pt = null;
        String cpStr = getParam(CROP_PT1);
        if (cpStr != null) {
            try {
                pt = ImagePt.parse(cpStr);

            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return pt;
    }

    public void setCropPt2(Pt pt2) {
        if (pt2 != null) {
            setParam((pt2 instanceof WorldPt) ? CROP_WORLD_PT2 : CROP_PT2, pt2.toString());
        }
    }

    public ImagePt getCropImagePt2() {
        ImagePt pt = null;
        String cpStr = getParam(CROP_PT2);
        if (cpStr != null) {
            try {
                pt = ImagePt.parse(cpStr);

            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return pt;
    }


    public WorldPt getCropWorldPt1() {
        return getWorldPtParam(CROP_WORLD_PT1);
    }

    public WorldPt getCropWorldPt2() {
        return getWorldPtParam(CROP_WORLD_PT2);
    }



//======================================================================
//----------------------- Blank Image Settings -------------------------
//======================================================================

    /**
     * set the arc seconds per pixel that will be used for a blank image
     * Used with RequestType.BLANK
     *
     * @param arcsecSize the size of the pixels in arcsec
     * @see RequestType
     */
    public void setBlankArcsecPerPix(float arcsecSize) {
        setParam(BLANK_ARCSEC_PER_PIX, arcsecSize + "");
    }

    public float getBlankArcsecPerPix() {
        return containsParam(BLANK_ARCSEC_PER_PIX) ? getFloatParam(BLANK_ARCSEC_PER_PIX) : 0F;
    }

    public void setBlankPlotWidth(int width) {
        setParam(BLANK_PLOT_WIDTH, width + "");
    }

    public int getBlankPlotWidth() {
        return containsParam(BLANK_PLOT_WIDTH) ? getIntParam(BLANK_PLOT_WIDTH) : 0;
    }


    public void setBlankPlotHeight(int height) {
        setParam(BLANK_PLOT_HEIGHT, height + "");
    }

    public int getBlankPlotHeight() {
        return containsParam(BLANK_PLOT_HEIGHT) ? getIntParam(BLANK_PLOT_HEIGHT) : 0;
    }

//======================================================================
//----------------------- Retrieval Settings --------------------------------
//======================================================================

    /**
     * plot the file name that exist on the server
     *
     * @param fileName the file name on the server
     */
    public void setFileName(String fileName) {
        setParam(FILE, fileName);
    }

    public String getFileName() {
        return getParam(FILE);
    }

    /**
     * retrieve and plot the file from the specified URL
     *
     * @param url the URL where the file resides
     */
    public void setURL(String url) {
        setSafeParam(URL, url);
    }

    public String getURL() {
        return getSafeParam(URL);
    }

    public void setServiceType(ServiceType service) {
        setParam(SERVICE, service.toString());
    }

    public RequestType getRequestType() {
        RequestType retval = RequestType.FILE;
        if (containsParam(TYPE)) {
            retval = Enum.valueOf(RequestType.class, getParam(TYPE));
        }
        return retval;
    }

    /**
     * Set the type of request. This parameter is required for every call.  The factory methods will always
     * set the Request type.
     *
     *
     * @param type the RequestType
     * @see RequestType
     */
    public void setRequestType(RequestType type) {
        setParam(TYPE, type.toString());
    }

    public ServiceType getServiceType() {
        ServiceType retval = ServiceType.NONE;
        if (containsParam(SERVICE)) {
            retval = Enum.valueOf(ServiceType.class, getParam(SERVICE));
        }
        return retval;
    }

    public void setSurveyKey(String key) {
        setParam(SURVEY_KEY, key);
    }

    public String getSurveyKey() {
        return getParam(SURVEY_KEY);
    }

    public void setSurveyKeyAlt(String key) {
        setParam(SURVEY_KEY_ALT, key);
    }

    public String getSurveyKeyAlt() {
        return getParam(SURVEY_KEY_ALT);
    }

    public String getSurveyBand() {
        return getParam(SURVEY_KEY_BAND);
    }

//======================================================================
//----------------------- Object & Area Settings -----------------------
//======================================================================

    public void setObjectName(String objectName) { setParam(OBJECT_NAME, objectName); }

    public String getObjectName() { return getParam(OBJECT_NAME); }

    public void setResolver(Resolver resolver) { setParam(RESOLVER, resolver.toString()); }

    public Resolver getResolver() {
        Resolver retval = Resolver.NedThenSimbad;
        if (containsParam(RESOLVER)) {
            retval = Enum.valueOf(Resolver.class, getParam(RESOLVER));
        }
        return retval;
    }


    public void setWorldPt(WorldPt wp) {
        if (wp != null) setParam(WORLD_PT, wp.toString());
    }

    public WorldPt getWorldPt() {
        return getWorldPtParam(WORLD_PT);
    }

    public void setSizeInDeg(float sizeInDeg) { setParam(SIZE_IN_DEG, sizeInDeg + ""); }
    public float getSizeInDeg() { return getFloatParam(SIZE_IN_DEG); }

//======================================================================
//----------------------- Other Settings --------------------------------
//======================================================================

    public void setMultiImageSupport(boolean multi) {
        setParam(MULTI_IMAGE_FITS, multi + "");
    }

    public boolean getMultiImageSupport() {
        return getBooleanParam(MULTI_IMAGE_FITS);
    }

    public void setMultiImageIdx(int idx) { setParam(MULTI_IMAGE_IDX, idx + "");  }

    public int getMultiImageIdx() { return getIntParam(MULTI_IMAGE_IDX,0); }



    public void setPreferenceColorKey(String key) {
        setParam(PREFERENCE_COLOR_KEY, key);
    }

    public String getPreferenceColorKey() {
        return getParam(PREFERENCE_COLOR_KEY);
    }

    public void setPreferenceZoomKey(String key) {
        setParam(PREFERENCE_ZOOM_KEY, key);
    }

    public String getPreferenceZoomKey() {
        return getParam(PREFERENCE_ZOOM_KEY);
    }



    public void setSaveCorners(boolean save) {
        setParam(SAVE_CORNERS, save + "");
    }

    public boolean getSaveCorners() {
        return getBooleanParam(SAVE_CORNERS);
    }


    public void setAllowImageSelection(boolean allowImageSelection) {
        setParam(ALLOW_IMAGE_SELECTION, allowImageSelection + "");
    }

    public boolean isAllowImageSelection() {
        return getBooleanParam(ALLOW_IMAGE_SELECTION);
    }

    public void setHasNewPlotContainer(boolean allowImageSelectionCreateNew) {
        setParam(HAS_NEW_PLOT_CONTAINER, allowImageSelectionCreateNew + "");
    }

    public boolean getHasNewPlotContainer() {
        return getBooleanParam(HAS_NEW_PLOT_CONTAINER);
    }



    public void setAdvertise(boolean advertise)  {
        setParam(ADVERTISE, advertise + "");
    }

    public boolean isAdvertise() {
        return getBooleanParam(ADVERTISE);
    }

    public void setGridOn(GridOnStatus gridOnStatus) {
        setParam(GRID_ON, gridOnStatus.toString());
    }

    public GridOnStatus getGridOn() {
        GridOnStatus retval = GridOnStatus.FALSE;
        if (containsParam(GRID_ON)) {
            retval = Enum.valueOf(GridOnStatus.class, getParam(GRID_ON));
        }
        return retval;
    }

    public void setHideTitleDetail(boolean hideTitleZoomLevel)  {
        setParam(HIDE_TITLE_DETAIL, hideTitleZoomLevel + "");
    }

    public boolean getHideTitleDetail() {
        return getBooleanParam(HIDE_TITLE_DETAIL);
    }

    public void setThumbnailSize(int thumbnailSize) {
        setParam(THUMBNAIL_SIZE, thumbnailSize+"");
    }

    public int getThumbnailSize() {
        return containsParam(THUMBNAIL_SIZE) ? getIntParam(THUMBNAIL_SIZE) : DEFAULT_THUMBNAIL_SIZE;
    }

    /**
     * For 3 color, if this request fails then keep trying to make a plot with the other request
     *
     * @param continueOnFail
     */
    public void setContinueOnFail(boolean continueOnFail) {
        setParam(CONTINUE_ON_FAIL, continueOnFail + "");
    }

    public boolean isContinueOnFail() {
        return getBooleanParam(CONTINUE_ON_FAIL);
    }

    public void setUniqueKey(String key) {
        setParam(UNIQUE_KEY, key);
    }

    public String getUniqueKey() {
        return getParam(UNIQUE_KEY);
    }

    public void setPlotToDiv(String div) {
        setParam(PLOT_TO_DIV, div);
    }

    public String getPlotToDiv() {
        return getParam(PLOT_TO_DIV);
    }


    public void setHeaderKeyForTitle(String headerKey) {
        setParam(HEADER_KEY_FOR_TITLE, headerKey);
    }


    public String getHeaderKeyForTitle() {
        return getParam(HEADER_KEY_FOR_TITLE);
    }


    public void setShowScrollBars(boolean s) {
        setParam(SHOW_SCROLL_BARS, s + "");
    }

    public boolean getShowScrollBars() {
        return getBooleanParam(SHOW_SCROLL_BARS);
    }

    public void setProgressKey(String key) { setParam(PROGRESS_KEY,key); }

    public String getProgressKey() { return getParam(PROGRESS_KEY); }


    public void setMinimalReadout(boolean minimalReadout) {
        setParam(MINIMAL_READOUT,minimalReadout+"");
    }

    public boolean isMinimalReadout() { return getBooleanParam(MINIMAL_READOUT); }


    public void setDrawingSubGroupId(String id) { setParam(DRAWING_SUB_GROUP_ID,id); }

    public String getDrawingSubGroupId() { return getParam(DRAWING_SUB_GROUP_ID); }

    public void setGridId(String id) { setParam(GRID_ID,id); }

    public String getGridId() { return getParam(GRID_ID); }

    public void setDownloadFileNameRoot(String nameRoot) {
        setParam(DOWNLOAD_FILENAME_ROOT, nameRoot);
    }

    public String getDownloadFileNameRoot() { return getParam(DOWNLOAD_FILENAME_ROOT); }


    public void setPlotId(String id) { setParam(PLOT_ID,id); }

    public String getPlotId() { return getParam(PLOT_ID); }

    public void setMaskBits(int idx) { setParam(MASK_BITS,idx+""); }
    public int getMaskBits() { return containsParam(MASK_BITS) ? getIntParam(MASK_BITS) : 0;}

    public void setPlotAsMask(boolean plotAsMask) { setParam(PLOT_AS_MASK, plotAsMask+"");}
    public boolean isPlotAsMask() { return getBooleanParam(PLOT_AS_MASK);}


    public void setMaskColors(String colors[]) {
        setParam(MASK_COLORS, StringUtils.combineAry(";", colors));
    }

    public List<String> getMaskColors() {
        if (containsParam(MASK_COLORS)) {
            String data= getParam(MASK_COLORS);
            return StringUtils.parseStringList(data,";");
        }
        else {
            return Collections.emptyList();
        }
    }

    public void setMaskRequiredWidth(int width) { setParam(MASK_REQUIRED_WIDTH, width+""); }

    public int getMaskRequiredWidth() { return getIntParam(MASK_REQUIRED_WIDTH,0); }

    public void setMaskRequiredHeight(int height) { setParam(MASK_REQUIRED_HEIGHT, height+""); }

    public int getMaskRequiredHeight() { return getIntParam(MASK_REQUIRED_HEIGHT,0); }

    /**
     * Set the order that the image processing pipeline runs when it reads a fits file.
     * This is experimental.  Use at your own risk.
     * Warning- if you exclude an Order elements the pipeline will not execute that process
     * even is you have it set in the option.
     * @param orderList the order of the pipeline
     */
    public void setPipelineOrder(List<Order> orderList) {
        StringBuilder sb= new StringBuilder(orderList.size()*10);
        for(Order order : orderList) {
            sb.append(order.toString());
            sb.append(";");
        }
        sb.deleteCharAt(sb.length()-1);
        setParam(PIPELINE_ORDER, sb.toString());
    }

    public List<Order> getPipelineOrder() {
        List<Order> retList;
        if (containsParam(PIPELINE_ORDER)) {
            retList= makeOrderList(getParam(PIPELINE_ORDER));
            if (retList.size()<2) retList= defOrder;
        }
        else {
            retList= defOrder;
        }
        return retList;
    }

    private static List<Order> makeOrderList(String orderStr) {
        List<Order> retList= new ArrayList<Order>(5);
        String sAry[]= orderStr.split(";");
        for(String s : sAry) {
            try {
                Order order= Enum.valueOf(Order.class, s);
                if (!retList.contains(order)) {
                    retList.add(order);
                }
            } catch (Exception e) {
            }
        }
        return retList;
    }



    /**
     * Return the request area
     * i am using circle but it is really size not radius - todo: fix this
     *
     * @return an area to select
     */
    public Circle getRequestArea() {
        Circle retval = null;
        WorldPt wp= getWorldPt();
        float side = getSizeInDeg();
        if (wp != null) retval = new Circle(wp, side);
        return retval;
    }


    public String prettyString() {
        String s = "WebPlotRequest: ";
        switch (getRequestType()) {
            case SERVICE:
                switch (getServiceType()) {
                    case IRIS:
                    case DSS:
                    case TWOMASS:
                        if (containsParam(WORLD_PT)) {
                            s += getServiceType().toString() + "- " + getRequestArea();
                        }
                        else {
                            s += getServiceType().toString() + "- Obj name: " + getObjectName() +
                            ", radius: " +getParam(SIZE_IN_DEG);
                        }
                        break;
                }
                break;
            case FILE:
                s += " File: " + getFileName();
                break;
            case URL:
                s += " URL: " + getURL();
                break;
            case ALL_SKY:
                s += " AllSky";
                break;
            case PROCESSOR:
                s += "File Search Processor: "+ getRequestId();
                break;
        }
        return s;

    }

    public void setPlotDescAppend(String s) { setParam(PLOT_DESC_APPEND, s); }

    public String getPlotDescAppend() { return getParam(PLOT_DESC_APPEND); }

    public boolean hasID() {
        return containsParam(ID_KEY) && !getRequestId().equals(ID_NOT_DEFINED);
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
    private static WebPlotRequest makePlotServiceReq(ServiceType serviceType,
                                                     WorldPt wp,
                                                     String survey,
                                                     float sizeInDeg) {
        String desc = makeServiceReqDesc(serviceType, survey, sizeInDeg);
        WebPlotRequest req = new WebPlotRequest(RequestType.SERVICE, desc, serviceType );
        req.setSurveyKey(survey);
        req.setWorldPt(wp);
        req.setSizeInDeg(sizeInDeg);
        return req;
    }

    private static String makeServiceReqDesc(ServiceType serviceType,
                                             String survey,
                                             float sizeInDeg) {
        return serviceType.toString() + ": " + survey +
                ", " + sizeInDeg + " Deg";
    }


    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     *
     * @param str the serialized WebPlotRequest
     * @return the deserialized WebPlotRequest
     */
    public static WebPlotRequest parse(String str) {
        return (str==null || "null".equalsIgnoreCase(str)) ? null : ServerRequest.parse(str, new WebPlotRequest());
    }

    public WebPlotRequest makeCopy() {
        WebPlotRequest retval = new WebPlotRequest();
        retval.copyFrom(this);
        return retval;
    }

    public static WebPlotRequest makeCopy(WebPlotRequest r) {
        return r == null ? null : r.makeCopy();
    }

    public static String[] getAllKeys() {
        return _allKeys;
    }

    public static String[] getClientKeys() {
        return _clientSideKeys;
    }

    /**
     * Perform equals but ignore layout params, such as zoom type, width and height
     * @param obj
     * @return
     */
    public boolean equalsPlottingParams(Object obj) {
        boolean retval= false;
        if (obj instanceof WebPlotRequest) {
            WebPlotRequest wpr1= this.makeCopy();
            WebPlotRequest wpr2= ((WebPlotRequest)obj).makeCopy();
            for(String key : _ignoreForEquals) {
                wpr1.removeParam(key);
                wpr2.removeParam(key);
            }
            retval= wpr1.toString().equals(wpr2.toString());
        }
        return retval;
    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}

