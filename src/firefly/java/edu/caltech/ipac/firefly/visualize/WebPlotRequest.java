/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.firefly.data.ServerRequest;
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
 * @author Trey Roby
 */
public class WebPlotRequest extends ServerRequest {

    public enum ServiceType {IRIS, SEIP, AKARI, ATLAS, ISSA, DSS, SDSS, TWOMASS, MSX, WISE, ZTF, PTF, UNKNOWN}

    public enum TitleOptions {NONE,  // use what it in the title
                              PLOT_DESC, // use the plot description key
                              FILE_NAME, // use the file name or analyze the URL and make a title from that
                              HEADER_KEY, // use the header value
                              PLOT_DESC_PLUS, // ??
                              SERVICE_OBS_DATE,
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
    public static final String SURVEY_KEY_BAND = "SurveyKeyBand";
    public static final String FILTER = "filter";
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
    public static final String MULTI_IMAGE_IDX = "MultiImageIdx";
    public static final String MULTI_IMAGE_EXTS = "MultiImageExts";
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
    public static final String OBJECT_NAME = "ObjectName";
    public static final String RESOLVER = "Resolver";
    public static final String PLOT_DESC_APPEND = "PlotDescAppend";
    public static final String PROGRESS_KEY = "ProgressKey";
    public static final String FLIP_Y = "FlipY";
    public static final String FLIP_X = "FlipX";
    public static final String THUMBNAIL_SIZE = "thumbnailSize";
    public static final String PIPELINE_ORDER = "pipelineOrder"; // todo: convert, doc, add to allKeys
    public static final String URL_CHECK_FOR_NEWER = "urlCheckForNewer"; // todo: convert, doc, add to allKeys

    public static final String MASK_BITS= "MaskBits";
    public static final String PLOT_AS_MASK= "PlotAsMask";
    public static final String MASK_COLORS= "MaskColors";
    public static final String MASK_REQUIRED_WIDTH= "MaskRequiredWidth";
    public static final String MASK_REQUIRED_HEIGHT= "MaskRequiredHeight";

    // keys - client side operations
    // note- if you add a new key make sure you put it in the _allKeys array
    public static final String PREFERENCE_COLOR_KEY = "PreferenceColorKey";
    public static final String HIDE_TITLE_DETAIL = "HideTitleDetail";
    public static final String GRID_ON = "GridOn";
    public static final String TITLE_OPTIONS = "TitleOptions";
    public static final String OVERLAY_POSITION = "OverlayPosition";
    public static final String DRAWING_SUB_GROUP_ID= "DrawingSubgroupID";
    public static final String DOWNLOAD_FILENAME_ROOT = "DownloadFileNameRoot";
    public static final String PLOT_ID = "plotId";

    public static final String DATA_HELP_URL= "DATA_HELP_URL";
    public static final String PROJ_TYPE_DESC= "PROJ_TYPE_DESC";
    public static final String WAVE_TYPE= "WAVE_TYPE";
    public static final String WAVE_LENGTH= "WAVE_LENGTH";
    public static final String WAVE_LENGTH_UM= "WAVE_LENGTH_UM";


    public enum Order {FLIP_Y, FLIP_X, ROTATE, POST_CROP, POST_CROP_AND_CENTER}

    public static final String DEFAULT_PIPELINE_ORDER= Order.FLIP_Y+";"+
                                                       Order.FLIP_X+";"+
                                                       Order.ROTATE+";"+
                                                       Order.POST_CROP+";"+
                                                       Order.POST_CROP_AND_CENTER;
    private final static List<Order> defOrder= makeOrderList(DEFAULT_PIPELINE_ORDER);


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public WebPlotRequest() {
        setRequestClass(WEB_PLOT_REQUEST_CLASS);
    }

    private WebPlotRequest(RequestType type, String userDesc) {
        this(type, userDesc, ServiceType.UNKNOWN);
    }

    private WebPlotRequest(RequestType type, String userDesc, ServiceType service) {
        super(type.toString());
        setRequestClass(WEB_PLOT_REQUEST_CLASS);
        setRequestType(type);
        if (!service.equals(ServiceType.UNKNOWN)) setServiceType(service);
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

    public static WebPlotRequest makeURLPlotRequest(String url) {
        WebPlotRequest req = new WebPlotRequest(RequestType.URL, "Fits from URL: " + url);
        req.setURL(url);
        return req;
    }

    public static WebPlotRequest makeWorkspaceRequest(String filePath, String userDesc) {
        WebPlotRequest req = new WebPlotRequest(RequestType.WORKSPACE, userDesc==null? filePath : userDesc);
        req.setTitleOptions(TitleOptions.FILE_NAME);
        req.setFileName(filePath);
        return req;
    };

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

    public static WebPlotRequest makeISSARequest(WorldPt wp, String survey, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.ISSA, wp, survey, sizeInDeg);
        req.setTitle("ISSA: "+survey);
        return req;
    }

    public static WebPlotRequest makeIRISRequest(WorldPt wp, String survey, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.IRIS, wp, survey, sizeInDeg);
        req.setTitle("IRIS: "+survey);
        return req;
    }

    public static WebPlotRequest make2MASSRequest(WorldPt wp, String survey, String band, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.TWOMASS, wp, survey, sizeInDeg);
        req.setParam(SURVEY_KEY_BAND, band + "");
        req.setTitle("2MASS: "+survey.toUpperCase());
        return req;
    }

    public static WebPlotRequest makeMSXRequest(WorldPt wp, String survey, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.MSX, wp, survey, sizeInDeg);
        req.setTitle("MSX: "+survey);
        return req;
    }

    public static WebPlotRequest makeSloanDSSRequest(WorldPt wp, String band, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.SDSS, wp, band, sizeInDeg);
        req.setTitle("SDSS: "+band);
        return req;
    }

    public static WebPlotRequest makeDSSRequest(WorldPt wp, String survey, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.DSS, wp, survey, sizeInDeg);
        req.setTitle("DSS: "+survey);
        return req;
    }

    public static WebPlotRequest makeWiseRequest(WorldPt wp, String survey, String band, float sizeInDeg) {
        WebPlotRequest req = makePlotServiceReq(ServiceType.WISE, wp, survey, sizeInDeg);
        req.setParam(SURVEY_KEY_BAND, band + "");
        String sDesc= survey.equalsIgnoreCase("3a")  ? "Atlas" : survey;
        req.setTitle("WISE: "+sDesc+ ", B"+ band);
        return req;
    }

    public static WebPlotRequest makeZTFRequest(WorldPt wp, String survey, String band, float sizeInDeg) {
        WebPlotRequest req= makePlotServiceReq(ServiceType.ZTF, wp, survey, sizeInDeg);
        req.setParam(SURVEY_KEY_BAND, band + "");
        req.setTitle("ZTF: "+survey.toUpperCase());
        return req;
    }


    /**
     * @param wp WorldPt
     * @param survey for atlas, survey is in form of 'schema.table'
     * @param band SEIP exmaple 'irac1'
     * @param filter for SEIP, it should loo like type=science and fname like %.mosaic.fits
     * @param sizeInDeg size
     * @return a request
     */
    public static WebPlotRequest makeAtlasRequest(WorldPt wp,
                                                  String survey,
                                                  String band,
                                                  String filter,
                                                  float sizeInDeg) {
        WebPlotRequest req = makePlotServiceReq(ServiceType.ATLAS, wp, survey, sizeInDeg);
        req.setParam(SURVEY_KEY, survey.split("\\.")[0]);
        req.setParam("dataset", survey.split("\\.")[0]);
        req.setParam("table", survey.split("\\.")[1]);
        req.setParam("filter", filter); //Needed for the query but not for fetching the data (see QueryIBE metadata)
        req.setParam(SURVEY_KEY_BAND, band + "");
        req.setTitle(survey + "," + band);
        // TODO drawingSubGroupId TO BE SET OUTSIDE here! ATLAS has many dataset and it will depend on the app to group those images, example: See ImageSelectPanelResult.js, Finderrchart...
        //req.setDrawingSubGroupId(survey.split(".")[1]); // 'spitzer.seip_science'
        return req;
    }

    //======================== All Sky =====================================


    public static WebPlotRequest makeAllSkyPlotRequest() {
        return new WebPlotRequest(RequestType.ALL_SKY, "All Sky Image");
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
        ZoomType retval = ZoomType.LEVEL;
        int w= getZoomToWidth();
        int h= getZoomToHeight();
        if (this.containsParam(ZOOM_TYPE)) {
            retval = Enum.valueOf(ZoomType.class, getParam(ZOOM_TYPE));
        }
        else if (w>0 && h>0){
            retval = ZoomType.TO_WIDTH_HEIGHT;
        }
        return retval;
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
            } catch (NumberFormatException ignore) { }
        }
        return pt;
    }


    public WorldPt getCropWorldPt1() { return getWorldPtParam(CROP_WORLD_PT1); }

    public WorldPt getCropWorldPt2() { return getWorldPtParam(CROP_WORLD_PT2); }




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

    public void setUrlCheckForNewer(boolean check) { setSafeParam(URL_CHECK_FOR_NEWER,check+"");}

    public boolean getUrlCheckForNewer() { return getBooleanParam(URL_CHECK_FOR_NEWER);}

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
        ServiceType retval = ServiceType.UNKNOWN;
        if (containsParam(SERVICE)) {
            retval = Enum.valueOf(ServiceType.class, getParam(SERVICE));
        }
        return retval;
    }


    /**
     * should only be call if getServiceType returns UNKNOWN
     */
    public String getServiceTypeString() { return getParam(SERVICE); }



    public void setSurveyKey(String key) {
        setParam(SURVEY_KEY, key);
    }

    public String getSurveyKey() {
        return getParam(SURVEY_KEY);
    }

    public String getSurveyBand() { return getParam(SURVEY_KEY_BAND); }

    public void setSurveyBand(String band) { setParam(SURVEY_KEY_BAND,band); }

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

    public void setMultiImageIdx(int idx) { setParam(MULTI_IMAGE_IDX, idx + "");  }

    public void setMultiImageExts(String idxs) { setParam(MULTI_IMAGE_EXTS, idxs);  }

    public int getMultiImageIdx() { return getIntParam(MULTI_IMAGE_IDX,0); }

    public String getMultiImageExts() { return getParam(MULTI_IMAGE_EXTS); }

    public void setPreferenceColorKey(String key) {
        setParam(PREFERENCE_COLOR_KEY, key);
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
    public void setHeaderKeyForTitle(String headerKey) {
        setParam(HEADER_KEY_FOR_TITLE, headerKey);
    }


    public String getHeaderKeyForTitle() {
        return getParam(HEADER_KEY_FOR_TITLE);
    }

    public void setProgressKey(String key) { setParam(PROGRESS_KEY,key); }

    public String getProgressKey() { return getParam(PROGRESS_KEY); }

    public void setDrawingSubGroupId(String id) { setParam(DRAWING_SUB_GROUP_ID,id); }

    public String getDrawingSubGroupId() { return getParam(DRAWING_SUB_GROUP_ID); }

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


    public void setMaskColors(String[] colors) { setParam(MASK_COLORS, StringUtils.combineAry(";", colors)); }

    public List<String> getMaskColors() {
        if (containsParam(MASK_COLORS)) {
            String data= getParam(MASK_COLORS);
            List<String> retval= StringUtils.parseStringList(data,";");
            if (retval.size()==0 && data.length()>0) {
                retval= new ArrayList<>(1);
                retval.add(data);
            }
            return retval;
        }
        else {
            return Collections.emptyList();
        }
    }

    public void setMaskRequiredWidth(int width) { setParam(MASK_REQUIRED_WIDTH, width+""); }

    public int getMaskRequiredWidth() { return getIntParam(MASK_REQUIRED_WIDTH,0); }

    public void setMaskRequiredHeight(int height) { setParam(MASK_REQUIRED_HEIGHT, height+""); }

    public int getMaskRequiredHeight() { return getIntParam(MASK_REQUIRED_HEIGHT,0); }


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
        List<Order> retList= new ArrayList<>(5);
        String[] sAry= orderStr.split(";");
        for(String s : sAry) {
            try {
                Order order= Enum.valueOf(Order.class, s);
                if (!retList.contains(order)) {
                    retList.add(order);
                }
            } catch (Exception ignore) { }
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

    private static String makeServiceReqDesc(ServiceType serviceType, String survey, float sizeInDeg) {
        return serviceType.toString() + ": " + survey + ", " + sizeInDeg + " Deg";
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
}

