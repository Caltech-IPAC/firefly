/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsNoExport;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.util.HashMap;
import java.util.Map;


/**
 * This class contains plot information.
 * Publicly this class operations in many coordinate system.
 * Some include a Image coordinate system, a world coordinate system, and a screen
 * coordinate system.
 * <ul>
 * <li>The image coordinate system is the coordinate system of the data. 
 * <li>The world coordinate system is the system that the data represents
 *        (i.e. the coordinate system of the sky)
 * <li>Screen coordinates are the pixel values of the screen.
 * </ul>
 *
 * @author Trey Roby
 * @version $Id: WebPlot.java,v 1.68 2012/12/14 23:59:58 roby Exp $
 */
@JsExport
@JsType
public class WebPlot {

    public enum ZDir {UP,DOWN,ORIGINAL}

    // =======================================================================
    // ------------------    constants for Attributes -------------------------
    // =======================================================================





    public static final String MOVING_TARGET_CTX_ATTR=   "MOVING_TARGET_CTX_ATTR";

    /**
     * This will probably be a WebMouseReadoutHandler class
     * @see WebMouseReadoutHandler
     */
    public static final String READOUT_ATTR=             "READOUT_ATTR";

    /**
     * This will probably be an HashMap<Integer, String> class
     * @see java.util.HashMap
     */
    public static final String READOUT_ROW_PARAMS=             "READOUT_ROW_PARAMS";

    /**
     * This will probably be a ActiveTarget.PosEntry class
     * @see ActiveTarget.PosEntry
     */
    public static final String FIXED_TARGET=             "FIXED_TARGET";

    /**
     * This will probably be a double with the requested size of the plot
     * @see ActiveTarget.PosEntry
     */
    public static final String REQUESTED_SIZE=             "REQUESTED_SIZE";


    /**
     * This will probably be a RecSelection class
     * @see edu.caltech.ipac.firefly.visualize.draw.RecSelection
     */
    public static final String SELECTION=                "SELECTION";

    /**
     * This will probably be a LineSelection class
     * @see edu.caltech.ipac.firefly.visualize.draw.LineSelection
     */
    public static final String ACTIVE_DISTANCE=          "ACTIVE_DISTANCE";


    /**
     * This will probably be a PointSelection class
     * @see edu.caltech.ipac.firefly.visualize.draw.PointSelection
     */
    public static final String ACTIVE_POINT=          "ACTIVE_POINT";


    /**
     * This is a String describing why this plot can't be rotated.  If it is defined then
     * rotating is disabled.
     */
    public static final String DISABLE_ROTATE_REASON=          "DISABLE_ROTATE_HINT";

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    public static final String FLIP_ZOOM_BY_LEVEL= "FLIP_ZOOM_BY_LEVEL";

    /**
     * what should happen when multi-fits images are changed.  If set the zoom is set to the same level
     * eg 1x, 2x ect.  If not set then flipping should attempt to make the image the same arcsec/screen pixel.
     */
    public static final String FLIP_ZOOM_TO_FILL= "FLIP_ZOOM_TO_FILL";

    /**
     * if set, when expanded the image will be zoom to no bigger than this level;
     * this should be a subclass of Number
     */
    public static final String MAX_EXPANDED_ZOOM_LEVEL = "MAX_EXPANDED_ZOOM_LEVEL";

    /**
     * if set, this should be the last expanded single image zoom level.
     * this should be a subclass of Number
     */
    public static final String LAST_EXPANDED_ZOOM_LEVEL = "LAST_EXPANDED_ZOOM_LEVEL";

    /**
     * if set, must be one of the string values defined by the enum ZoomUtil.FullType
     * currently is is ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT
     */
    public static final String EXPANDED_TO_FIT_TYPE = "MAX_EXPANDED_ZOOM_LEVEL";

    /**
     * if true, the readout will be very small
     */
    public static final String MINIMAL_READOUT = "MINIMAL_READOUT";



    public static final String UNIQUE_KEY = "UNIQUE_KEY";


    private static final int  THREE_COLOR_LEN= 3;


    private final CoordinateSys _imageCoordSys;
    private final Projection    _projection;
    private final int           _dataWidth;
    private final int           _dataHeight;
    private final WebPlotGroup  _plotGroup;
    private       PlotState     _plotState;
    private final WebFitsData   _webFitsData[];
    private final Map<String,Object>  _attributes= new HashMap<String,Object>(3);
    private final TileDrawer    _tileDrawer;

    // Cache is estimated to never exceed 2 MB
    private HashMap<WorldPt,ImagePt> conversionCache= new HashMap<WorldPt, ImagePt>(3000,.80F);
    private final int MAX_CACHE_ENTRIES = 38000; // set to never allows the cache array over 48000 with a 80% load factor


    private final int       _offsetX= 0;  //if we ever use this we will change the final
    private final int       _offsetY= 0; //if we ever use this we will change the final
    private final String    _dataDesc;
    private String    _plotDesc     = "";
    private boolean   _alive    = true;
    private float     _initialZoomLevel= 1.0F;
    private float     _percentOpaque   = 1.0F;


    private int _viewPortX= 0;
    private int _viewPortY= 0;
    private Dimension _viewPortDim= new Dimension(42,42); // small dummy initialization

    private final ImageBoundsData imageBoundsData;

    public WebPlot(WebPlotInitializer wpInit, boolean asOverlay) {
        _plotGroup= new WebPlotGroup(this,wpInit.getPlotState().getZoomLevel());
        _plotState       = wpInit.getPlotState();
        _tileDrawer      = new TileDrawer(this,wpInit.getInitImages(), asOverlay);
        _imageCoordSys   = wpInit.getCoordinatesOfPlot();
        _projection      = wpInit.getProjection();
        _dataWidth       = wpInit.getDataWidth();
        _dataHeight      = wpInit.getDataHeight();
        _plotDesc        = wpInit.getPlotDesc();
        _dataDesc        = wpInit.getDataDesc();

        _webFitsData= new WebFitsData[THREE_COLOR_LEN];
        WebFitsData webFitsData[]= wpInit.getFitsData();
        for(int i= 0; (i<THREE_COLOR_LEN); i++) {
            if (webFitsData.length>=i+1) {
                _webFitsData[i]= webFitsData[i];
            }
        }

        if (!_projection.isWrappingProjection()) {
            imageBoundsData = new ImageBoundsData(
                    this,
                    getWorldCoords(new ImagePt(0,0)),
                    getWorldCoords(new ImagePt(0,_dataWidth)),
                    getWorldCoords(new ImagePt(_dataHeight,0)),
                    getWorldCoords(new ImagePt(_dataHeight,_dataWidth))
            );

        }
        else {
           imageBoundsData = null;
        }
    }

    public WebPlot(){
    	imageBoundsData = null;
    	_plotGroup= null;
        _plotState       = null;
        _tileDrawer      = null;
        _imageCoordSys   = null;
        _projection      = null;
        _dataWidth       = 0;
        _dataHeight      = 0;
        _plotDesc        = null;
        _dataDesc        = null;
    	_webFitsData=null;
    };

    public AbsolutePanel getWidget() { return _tileDrawer.getWidget(); }

    public void refreshWidget() { _tileDrawer.refreshWidget(); }

    @JsNoExport
    public void refreshWidget(PlotImages images) { _tileDrawer.refreshWidget(images); }

    public PlotState getPlotState() { return _plotState; }
    public void setPlotState(PlotState state) { _plotState= state; }

    public void drawTilesInArea(ScreenPt viewPortLocation, int width, int height) {
        _tileDrawer.drawTilesForArea(viewPortLocation,width,height);
    }

    public void setOpacity(float opacity) { _tileDrawer.setOpacity(opacity); }


    public WebPlotGroup getPlotGroup() { return _plotGroup; }
    public TileDrawer getTileDrawer() { return _tileDrawer; }
    public float getZoomFact() { return _plotGroup.getZoomFact(); }

    public WebFitsData getFitsDataByBand(Band band) {
        return _webFitsData[band.getIdx()];
    }


    public String getRangeValuesSerialized(Band band) {
        RangeValues rv = _plotState.getRangeValues(band);
        return rv!=null ? rv.serialize() : null;
    }



    public WebFitsData getFitsData(Band band) {
        return _webFitsData[band.getIdx()];
    }

    public void setFitsData(WebFitsData  data, Band band) {
        _webFitsData[band.getIdx()]= data;
    }

    public boolean isCube() {
        return _plotState.isMultiImageFile() && _plotState.getCubeCnt()>0;
    }

    public int getCubeCnt() {
        return _plotState.getCubeCnt();
    }

    public int getCubePlaneNumber() {
        return _plotState.getCubePlaneNumber();
    }

    public WebHistogramOps getHistogramOps(Band band) {
        WebHistogramOps retval;
        if (band== Band.NO_BAND) {
            retval= new WebHistogramOps(this, Band.NO_BAND);
        }
        else {
            threeColorOK(band);
            retval= new WebHistogramOps(this,band);
        }
        return retval;
    }

    /**
     * set the viewport location in terms of screen coordinates
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setViewPort(int x, int y, int width, int height) {
        _viewPortX= x;
        _viewPortY= y;
        _viewPortDim= new Dimension(width,height);
    }

    public int getViewPortX() { return _viewPortX; }
    public int getViewPortY() { return _viewPortY; }
    public Dimension getViewPortDimension() { return _viewPortDim; }

    public int getImageWidth()  { return _plotGroup.getImageSize().getWidth();   }
    public int getImageHeight() { return _plotGroup.getImageSize().getHeight();  }


    /**
     * returns the first used band. It is possible that this method will return null.  You should always check.
     * @return the first name used.
     */
    public Band getFirstBand() {
        return getBands().length>0 ?  getBands()[0] : null;
    }

    /**
     * Get an array of used band.  It is possible that this routine will return a array of length 0
     * @return the bands in use
     */
    public Band[] getBands() { return _plotState.getBands(); }
    public boolean isThreeColor()  { return _plotState.isThreeColor(); }

    public int getColorTableID() { return _plotState.getColorTableId(); }

    private void threeColorOK(Band band) {
        WebAssert.argTst( (band== Band.RED || band== Band.GREEN || band== Band.BLUE),
                       "band must be RED, GREEN, or BLUE");
    }

    /**
     * This method will return the width of the image in screen coordinates.
     * This number will change as the plot is zoomed up and down.
     * @return the width of the plot
     */
    public int     getScreenWidth()  { return _plotGroup.getScreenSize().getWidth(); }

    /**
     *  This method will return the height of the image in screen coordinates.
     *  This number will change as the plot is zoomed up and down.
     * @return the height of the plot
     */
    public int     getScreenHeight() { return _plotGroup.getScreenSize().getHeight();}

    /**
     * This method will return the width of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the width of the image data
     */
    public int     getImageDataWidth() { return _dataWidth; }

    /**
     * This method will return the height of the image data.
     * This number will not change as the plot is zoomed up and down.
     * @return the height of the image data
     */
    public int     getImageDataHeight() { return _dataHeight; }

    @JsNoExport
    public boolean isBlankImage() {
        if (isThreeColor()) return false;
        WebPlotRequest req=_plotState.getWebPlotRequest();
        return (req!=null && req.getRequestType()==RequestType.BLANK);
    }

    /**
     * This method will return the width of the image in the world coordinate
     * system (probably degrees on the sky).
     * @return the width of the image data in world coord system.
     */
    public double getWorldPlotWidth() {
        return _projection.getPixelWidthDegree() *_dataWidth;
    }

    public Projection getProjection() { return _projection; }

    /**
     * This method will return the height of the image in the world coordinate
     * system (probably degrees on the sky).
     * @return the height of the image data in world coord system.
     */
    public double  getWorldPlotHeight() {
        return _projection.getPixelHeightDegree() *_dataHeight;
    }

//========================================================================================
//----------------------------- pointIn Methods  -----------------------------------------
//========================================================================================

    /**
     * Determine if a world point is in data Area of the plot and is not null
     * @param iwPt the point to test.
     * @return boolean true if it is in the data boundaries, false if not.
     */
    @JsNoExport
    public boolean pointInData(ImageWorkSpacePt iwPt) {
        boolean retval= false;
        if (iwPt!=null && pointInPlot(iwPt)) {
            ImagePt ipt= getImageCoords(iwPt);
            double x= ipt.getX();
            double y= ipt.getY();
            retval= (x >= 0 && x <= _dataWidth && y >= 0 && y <= _dataHeight );
        }
        return retval;
    }


    /**
     * Determine if a image point is in the plot boundaries and is not null.
     * @param pt the point to test.
     * @return boolean true if it is in the boundaries, false if not.
     */
    public boolean pointInData( Pt pt) {
        boolean retval= false;
        if      (pt==null) {
            retval= false;
        }
        else if (pt instanceof WorldPt) {
            ImageWorkSpacePt ipt= getImageWorkSpaceCoords((WorldPt)pt);
            retval= pointInData(ipt);
        }
        else if (pt instanceof ImageWorkSpacePt) {
            retval= pointInData((ImageWorkSpacePt)pt);
        }
        else if (pt instanceof ImagePt) {
            ImageWorkSpacePt ipt= this.getImageWorkSpaceCoords((ImagePt)pt);
            retval= pointInData(ipt);
        }
        else if (pt instanceof ScreenPt) {
            ImageWorkSpacePt ipt= this.getImageWorkSpaceCoords((ScreenPt)pt);
            retval= pointInData(ipt);
        }
        else if (pt instanceof ViewPortPt) {
            ImageWorkSpacePt ipt= this.getImageWorkSpaceCoords((ViewPortPt)pt);
            retval= pointInData(ipt);
        }
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }

        return retval;
    }


    /**
     * This method returns false it the point is definitely not in plot.  It returns true if the point might be in the plot.
     * Used for tossing out points that we know that are not in plot without having to do all the math.  It is much faster.
     * @return true in we guess it might be in the bounds, false if we know that it is not in the bounds
     */
    public boolean pointInPlotRoughGuess(WorldPt wp) {
        return imageBoundsData==null || imageBoundsData.pointInPlotRoughGuess(wp);
    }


    /**
     * Determine if a image point is in the plot boundaries and is not null.
     * @param ipt the point to test.
     * @return boolean true if it is in the boundaries, false if not.
     */
    @JsNoExport
    public boolean pointInPlot( ImageWorkSpacePt ipt) {
        if (ipt==null) return false;
        return _plotGroup.pointInPlot(ipt);
    }

    /**
     * Determine if a image point is in the plot boundaries and is not null
     * @param pt the point to test.
     * @return boolean true if it is in the boundaries, false if not.
     */
    public boolean pointInPlot( Pt pt) {
        boolean retval= false;
        if (pt==null) {
            return false;
        }
        else if (pt instanceof WorldPt) {
            retval= pointInPlotRoughGuess((WorldPt) pt);
            if (retval) {
                ImageWorkSpacePt ipt= getImageWorkSpaceCoords((WorldPt)pt);
                retval= pointInPlot(ipt);
            }
        }
        else if (pt instanceof ImageWorkSpacePt) {
            retval= pointInPlot((ImageWorkSpacePt)pt);
        }
        else if (pt instanceof ImagePt) {
            ImageWorkSpacePt ipt= this.getImageWorkSpaceCoords((ImagePt)pt);
            retval= pointInPlot(ipt);
        }
        else if (pt instanceof ScreenPt) {
            ImageWorkSpacePt ipt= this.getImageWorkSpaceCoords((ScreenPt)pt);
            retval= pointInPlot(ipt);
        }
        else if (pt instanceof ViewPortPt) {
            ImageWorkSpacePt ipt= this.getImageWorkSpaceCoords((ViewPortPt)pt);
            retval= pointInPlot(ipt);
        }
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }

        return retval;
    }

    /**
     * test to see if the input is in the view port and is not null
     * @param vpt
     * @return
     */
    @JsNoExport
    public boolean pointInViewPort( ViewPortPt vpt) {
        if (vpt==null) return false;
        int x= vpt.getIX();
        int y= vpt.getIY();
        return (x>=0 && y>=0 && x<=_viewPortDim.getWidth() && y<=_viewPortDim.getHeight());
    }


    /**
     * Determine if a point is in the view port boundaries and is not null.
     * @param pt the point to test.
     * @return boolean true if it is in the boundaries, false if not.
     */
    public boolean pointInViewPort(Pt pt) {
        boolean retval= false;
        if (pt==null)  retval= false;
        else if (pt instanceof WorldPt)          retval= pointInViewPort(getViewPortCoords((WorldPt)pt));
        else if (pt instanceof ImageWorkSpacePt) retval= pointInViewPort(getViewPortCoords((ImageWorkSpacePt)pt));
        else if (pt instanceof ImagePt)          retval= pointInViewPort(getViewPortCoords((ImagePt)pt));
        else if (pt instanceof ScreenPt)         retval= pointInViewPort(getViewPortCoords((ScreenPt)pt));
        else if (pt instanceof ViewPortPt)       retval= pointInViewPort((ViewPortPt)pt);
        else  WebAssert.argTst(false, "unknown Pt type");
        return retval;
    }


//========================================================================================
//----------------------------- End pointIn Methods  -------------------------------------
//========================================================================================

    /**
     * get the coordinate system of the plot.
     * @return  CoordinateSys  the coordinate system.
     */
    public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }

    /**
     * get the flux of a given image point point on the plot.
     * @param pt the image point
     * @param callback the image point as String
     */

    public void getFluxLight(ImagePt pt, AsyncCallback <String[]> callback) {

        if (pointInData(pt)) {
            Band bands[]= getBands();
            FileAndHeaderInfo pahi[]= new FileAndHeaderInfo[bands.length];
            for(int i= 0; (i<bands.length); i++) {
                pahi[i]= _plotState.getFileAndHeaderInfo(bands[i]);
            }

            ImagePt deciModPt= new ImagePt( pt.getX(), pt.getY());
            VisTask.getInstance().getFlux(pahi, deciModPt, callback);
        }
        else {
            callback.onFailure(null);
        }
    }

    public void getFlux(ImagePt pt,
                        final AsyncCallback<double[]> callback) {
        PlotServiceAsync pserv=PlotService.App.getInstance();
//        int decimation= _plotState.getDecimationLevel(_plotState.firstBand());
        int decimation= 1;
        ImagePt deciModPt= new ImagePt( pt.getX()/decimation, pt.getY()/decimation);
        pserv.getFlux(_plotState, deciModPt, new AsyncCallback<WebPlotResult>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onSuccess(WebPlotResult result) {
                _plotState.setContextString(result.getContextStr());
                DataEntry.DoubleArray fluxResult =
                        (DataEntry.DoubleArray) result.getResult(WebPlotResult.FLUX_VALUE);
                if (fluxResult != null) {
                    double fluxvals[] = fluxResult.getArray();
                    callback.onSuccess(fluxvals);
                } else {
                    callback.onFailure(new NullPointerException("fluxval is null"));
                }
            }
        });
    }

    /**
     * get the scale (in arcseconds) that one image pixel of data represents.
     * @return double the scale of one pixel.
     */
    public double getImagePixelScaleInArcSec(){
        return _projection.getPixelScaleArcSec();
    }
    public double getImagePixelScaleInDeg(){
        return _projection.getPixelScaleArcSec()/3600.0;
    }


//    public double convertImagePixelSizeToScreenPixelSize(double iPixSize){
//        return iPixSize*getZoomFact();
//    }

//========================================================================================
//----------------------------- Conversion to ImageWorkSpacePt Methods  ------------------
//========================================================================================


    @JsNoExport
    public ImageWorkSpacePt getImageWorkSpaceCoords(ViewPortPt vpt) {
        if (vpt==null) return null;
        return getImageWorkSpaceCoords(getScreenCoords(vpt));
    }


    @JsNoExport
    public ImageWorkSpacePt getImageWorkSpaceCoords(ScreenPt pt) {
        if (pt==null) return null;
        return getImageWorkSpaceCoords(pt,getZoomFact());
    }

    @JsNoExport
    public ImageWorkSpacePt getImageWorkSpaceCoords(ScreenPt pt, float altZLevel) {
        if (pt==null) return null;
        return new ImageWorkSpacePt(pt.getX() / altZLevel,
                                    getImageHeight()-pt.getY()/altZLevel);
    }


    @JsNoExport
    public ImageWorkSpacePt getImageWorkSpaceCoords(ImagePt pt) {
        if (pt==null) return null;
        return new ImageWorkSpacePt(pt.getX()+_offsetX, pt.getY()+_offsetY);
    }

    @JsNoExport
    public ImageWorkSpacePt getImageWorkSpaceCoords( WorldPt wpt) {
        if (wpt==null) return null;
        ImageWorkSpacePt retval;

        //LZ check here TODO remove it after testing
        Pt checkedPt= convertToCorrect(wpt);
        if (checkedPt instanceof  WorldPt) {
            ImagePt ipt= getImageCoords(wpt);
            retval= getImageWorkSpaceCoords(ipt);
        }
        else {
            retval= getImageWorkSpaceCoords(checkedPt);
        }
        return retval;
    }
    /**
     * Return the ImageWorkSpacePt coordinates given Pt
     * @param pt the point to translate
     * @return WorldPt the world coordinates
     */
    @JsNoExport
    public ImageWorkSpacePt getImageWorkSpaceCoords(Pt pt) {
        ImageWorkSpacePt retval= null;

        if      (pt==null)                       retval= null;
        else if (pt instanceof ImageWorkSpacePt) retval= (ImageWorkSpacePt)pt;
        else if (pt instanceof WorldPt)          retval= getImageWorkSpaceCoords((WorldPt)pt);
        else if (pt instanceof ImagePt)          retval= getImageWorkSpaceCoords((ImagePt)pt);
        else if (pt instanceof ScreenPt)         retval= getImageWorkSpaceCoords((ScreenPt)pt);
        else if (pt instanceof ViewPortPt)       retval= getImageWorkSpaceCoords((ViewPortPt)pt);
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }

        return retval;
    }

//========================================================================================
//----------------------------- Conversion to ImageSpacePt Methods  ----------------------
//========================================================================================


    /**
     * Return the image coordinates given screen x & y.
     * @param pt screen coordinates to convert from
     * @return ImagePt the translated coordinates
     */
    @JsNoExport
    public ImagePt getImageCoords(ScreenPt pt) {
        if (pt==null) return null;
        return getImageCoords(getImageWorkSpaceCoords(pt));
    }

    @JsNoExport
    public ImagePt getImageCoords(ViewPortPt vpt) {
        if (vpt==null) return null;
        return getImageCoords(getScreenCoords(vpt));
    }

    /**
     * Return the ImagePt coordinates given Pt
     * @param pt the point to translate
     * @return WorldPt the world coordinates
     */
    @JsNoExport
    public ImagePt getImageCoords(Pt pt) {
        ImagePt retval= null;

        if      (pt==null)                       retval= null;
        else if (pt instanceof ImagePt)          retval= (ImagePt)pt;
        else if (pt instanceof WorldPt)          retval= getImageCoords((WorldPt)pt);
        else if (pt instanceof ImageWorkSpacePt) retval= getImageCoords((ImageWorkSpacePt)pt);
        else if (pt instanceof ScreenPt)         retval= getImageCoords((ScreenPt)pt);
        else if (pt instanceof ViewPortPt)       retval= getImageCoords((ViewPortPt)pt);
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }
        return retval;
    }

    /**
     * This will be overridden for ImagePlot where the plot might be an overlay
     * @param sipt the ImageWorkSpacePt point
     * @return ImagePt the converted point
     */

    @JsNoExport
   public ImagePt getImageCoords(ImageWorkSpacePt sipt) {
       if (sipt==null) return null;
       return new ImagePt(sipt.getX()-_offsetX, sipt.getY()-_offsetY);
   }

    /**
     * Return the image coordinates given a WorldPt class
     * @param wpt the class containing the point in sky coordinates
     * @return ImagePt the translated coordinates
     */
    @JsNoExport
    public ImagePt getImageCoords( WorldPt wpt) {
        ImagePt retval;
        if (wpt==null) return null;

        Pt checkedPt= convertToCorrect(wpt);
        if (checkedPt instanceof  WorldPt) {
            WorldPt originalWp= wpt;
 //
            retval= conversionCache.get(checkedPt);
            if (retval==null) {
                if (!_imageCoordSys.equals(wpt.getCoordSys())) {
                    wpt= VisUtil.convert(wpt,_imageCoordSys);
                }
                ProjectionPt projPt= _projection.getImageCoordsSilent(wpt.getLon(),wpt.getLat());
                retval= projPt==null ? null : new ImagePt( (projPt.getX())+ 0.5F ,  (projPt.getY())+ 0.5F);
                putInConversionCache(originalWp,retval);
            }
        }
        else {
            retval= getImageCoords(checkedPt);
        }
        return retval;
    }

//========================================================================================
//----------------------------- Conversion to ViewPortPt Methods  ------------------------
//========================================================================================

    public boolean getViewPortCoordsOptimize( WorldPt wpt, ViewPortPtMutable retPt) {

        boolean success= false;
        float zfact= _plotGroup.getZoomFact();

        ImagePt imagePt= conversionCache.get(wpt);

        if (imagePt==null) {
            CoordinateSys csys= wpt.getCoordSys();
            if (csys.equals(CoordinateSys.SCREEN_PIXEL)) {
                retPt.setX(wpt.getX()-_viewPortX);
                retPt.setY(wpt.getY()- _viewPortY);
                success= true;
            }
            else if (csys.equals(CoordinateSys.PIXEL)) {
                ScreenPt sp= getScreenCoords(new ImagePt(wpt.getX(), wpt.getY()));
                retPt.setX(sp.getX()-_viewPortX);
                retPt.setY(sp.getY()- _viewPortY);
                success= true;
            }
            else {
                WorldPt originalWp= wpt;
                if (!_imageCoordSys.equals(wpt.getCoordSys())) {
                    wpt= VisUtil.convert(wpt,_imageCoordSys);
                }

                ProjectionPt proj_pt= _projection.getImageCoordsSilent(wpt.getLon(),wpt.getLat());
                if (proj_pt!=null) {
                    double imageX= proj_pt.getX()  + 0.5;
                    double imageY= proj_pt.getY()  + 0.5;
                    putInConversionCache(originalWp, new ImagePt(imageX,imageY));
                    double imageWorkspaceX= imageX-_offsetX;
                    double imageWorkspaceY= imageY-_offsetY;

                    double sx= (imageWorkspaceX*zfact);
                    double sy= ((getImageHeight() - imageWorkspaceY) *zfact);

                    retPt.setX(sx-_viewPortX);
                    retPt.setY(sy-_viewPortY);
                    success= true;
                }
            }
        }
        else {
            double imageWorkspaceX= imagePt.getX()-_offsetX;
            double imageWorkspaceY= imagePt.getY()-_offsetY;

            double sx= (imageWorkspaceX*zfact);
            double sy= ((getImageHeight() - imageWorkspaceY) *zfact);

            retPt.setX(sx-_viewPortX);
            retPt.setY(sy-_viewPortY);
            success= true;

        }

        return success;
    }

    public void putInConversionCache(WorldPt wp, ImagePt imp) {
        if (conversionCache.size()<MAX_CACHE_ENTRIES) {
            conversionCache.put(wp,imp);
        }
    }


    @JsNoExport
    public ViewPortPt getViewPortCoords(ScreenPt spt)  {
        if (spt==null) return null;
        return new ViewPortPt( spt.getIX()-_viewPortX, spt.getIY()-_viewPortY);
    }

    @JsNoExport
    public ViewPortPt getViewPortCoords(ImagePt ipt)  {
        if (ipt==null) return null;
        return getViewPortCoords(getScreenCoords(ipt));
    }
    @JsNoExport
    public ViewPortPt getViewPortCoords(ImagePt ipt, float altZLevel)  {
        if (ipt==null) return null;
        return getViewPortCoords(getScreenCoords(ipt,altZLevel));
    }
    @JsNoExport
    public ViewPortPt getViewPortCoords(ImageWorkSpacePt ipt)  {
        if (ipt==null) return null;
        return getViewPortCoords(getScreenCoords(ipt));
    }
    @JsNoExport
    public ViewPortPt getViewPortCoords(ImageWorkSpacePt ipt, float altZLevel)  {
        if (ipt==null) return null;
        return getViewPortCoords(getScreenCoords(ipt,altZLevel));
    }
    @JsNoExport
    public ViewPortPt getViewPortCoords(WorldPt wpt)  {
        if (wpt==null) return null;
        ViewPortPt retval;
        Pt checkedPt= convertToCorrect(wpt);
        if (checkedPt instanceof  WorldPt) {
            retval= new ViewPortPtMutable();
            getViewPortCoordsOptimize(wpt,(ViewPortPtMutable)retval);
//            retval= getViewPortCoords(getScreenCoords(wpt));
        }
        else {
            retval= getViewPortCoords(getScreenCoords(checkedPt));
        }
        return retval;
    }

    @JsNoExport
    public ViewPortPt getViewPortCoords(WorldPt wpt, float altZLevel) {
        if (wpt==null) return null;
        ViewPortPt retval;
        Pt checkedPt= convertToCorrect(wpt);
        if (checkedPt instanceof  WorldPt) {
            retval= getViewPortCoords(getScreenCoords(wpt));
        }
        else {
            retval= getViewPortCoords(checkedPt);
        }
        return retval;
    }

    public ViewPortPt getViewPortCoords(Pt pt) {
        if (pt==null) return null;
        return getViewPortCoords(getScreenCoords(pt));
    }

//========================================================================================
//----------------------------- Conversion to ScreenPt Methods  --------------------------
//========================================================================================

    /**
     * Return the screen coordinates given Pt
     * @param pt the point to translate
     * @return ScreenPt the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(Pt pt) {
        ScreenPt retval= null;

        if      (pt==null)                       retval= null;
        else if (pt instanceof WorldPt)          retval= getScreenCoords((WorldPt)pt);
        else if (pt instanceof ImageWorkSpacePt) retval= getScreenCoords((ImageWorkSpacePt)pt);
        else if (pt instanceof ImagePt)          retval= getScreenCoords((ImagePt)pt);
        else if (pt instanceof ScreenPt)         retval= (ScreenPt)pt;
        else if (pt instanceof ViewPortPt)       retval= getScreenCoords((ViewPortPt)pt);
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }

        return retval;
    }

    /**
     * Return the screen coordinates given WorldPt
     * @param wpt the world point to translate
     * @return ScreenPt the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(WorldPt wpt) {
        if (wpt==null) return null;

        ScreenPt retval= null;
        Pt checkedPt= convertToCorrect(wpt);
        if (checkedPt!=null) {
            if (checkedPt instanceof  WorldPt) {
                ImageWorkSpacePt iwpt= getImageWorkSpaceCoords(wpt);
                retval= getScreenCoords(iwpt);
            }
            else {
                retval= getScreenCoords(checkedPt);
            }
        }
        return  retval;
    }

    /**
     * Return the screen coordinates given WorldPt and alternate zoom level.
     * Make sure you know what you are doing if you are using this method
     * @param wpt the world point to translate
     * @return Point2D the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(WorldPt wpt, float altZLevel) {
        if (wpt==null) return null;
        ImageWorkSpacePt iwpt= getImageWorkSpaceCoords(wpt);
        return  getScreenCoords(iwpt, altZLevel);
    }

    /**
     * Return the screen coordinates given ImagePt
     * @param ipt the image point to translate
     * @return Point2D the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(ImagePt ipt) {
        if (ipt==null) return null;
        return getScreenCoords(ipt,getZoomFact());
    }



    /**
     * Return the screen coordinates given ImagePt and alternate zoom level.
     * Make sure you know what you are doing if you are using this method
     * @param ipt the image point to translate
     * @param altZLevel use the passed zoom level instead of the level of the plot
     * @return Point2D the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(ImagePt ipt, float altZLevel) {
        if (ipt==null) return null;
        return getScreenCoords(getImageWorkSpaceCoords(ipt), altZLevel);
    }


    @JsNoExport
    public ScreenPt getScreenCoords(ViewPortPt vpt) {
        if (vpt==null) return null;
        return new ScreenPt(vpt.getIX()+_viewPortX, vpt.getIY()+_viewPortY);
    }

    /**
     * Return the screen coordinates given ImageWorkSpacePt
     * @param ipt the ImageWorkSpace point to translate
     * @return Point2D the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(ImageWorkSpacePt ipt) {
        if (ipt==null) return null;
        return getScreenCoords(ipt,getZoomFact());
    }


    /**
     * Return the screen coordinates given ImageWorkSpacePt
     * and alternate zoom level.
     * Make sure you know what you are doing if you are using this method
     * @param ipt the ImageWorkSpace point to translate
     * @param altZLevel use the passed zoom level instead of the level of the plot
     * @return Point2D the screen coordinates
     */
    @JsNoExport
    public ScreenPt getScreenCoords(ImageWorkSpacePt ipt, float altZLevel) {
        if (ipt==null) return null;
        return new ScreenPt(((ipt.getX())*altZLevel),
                            ((getImageHeight() - ipt.getY()) *altZLevel));
    }

//========================================================================================
//----------------------------- Conversion to WorldPt Methods  ---------------------------
//========================================================================================


    /**
     * Return the world coordinates given screen x & y.
     * @param pt the screen coordinates to convert to world coordinates
     * @param outputCoordSys the coordinate system you want this screen coordinates
     *                      translated into
     * @return WorldPt the translated coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords(ScreenPt pt, CoordinateSys outputCoordSys) {
        if (pt==null) return null;
        ImageWorkSpacePt iwspt = getImageWorkSpaceCoords(pt);
        return iwspt!=null ? getWorldCoords(iwspt,outputCoordSys) : null;
    }


    /**
     * Return the world coordinates given screen x & y.
     * @param pt the screen coordinates to convert to world coordinates
     * @return WorldPt the translated coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords(ScreenPt pt) {
        if (pt==null) return null;
        ImageWorkSpacePt iwspt = getImageWorkSpaceCoords(pt);
        return iwspt!=null ? getWorldCoords(iwspt) : null;
    }

    @JsNoExport
    public WorldPt getWorldCoords(ViewPortPt vpt) {
        if (vpt==null) return null;
        ScreenPt spt = getScreenCoords(vpt);
        return spt!=null ? getWorldCoords(spt) : null;
    }

    @JsNoExport
    public WorldPt getWorldCoords(ViewPortPt vpt, CoordinateSys outputCoordSys) {
        if (vpt==null) return null;
        ScreenPt spt = getScreenCoords(vpt);
        return spt!=null ? getWorldCoords(spt, outputCoordSys) : null;
    }

    /**
     * Return the screen coordinates given Pt
     * @param pt the point to translate
     * @return WorldPt the world coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords(Pt pt) {
        WorldPt retval= null;

        if      (pt==null)                       retval= null;
        else if (pt instanceof WorldPt)          retval= (WorldPt)pt;
        else if (pt instanceof ImageWorkSpacePt) retval= getWorldCoords((ImageWorkSpacePt)pt);
        else if (pt instanceof ImagePt)          retval= getWorldCoords((ImagePt)pt);
        else if (pt instanceof ScreenPt)         retval= getWorldCoords((ScreenPt)pt);
        else if (pt instanceof ViewPortPt)       retval= getWorldCoords((ViewPortPt)pt);
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }

        return retval;
    }

    /**
     * Return the J2000 sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImagePt class
     * @param pt the ImageWorkSpacePt
     * @return WorldPt the translated coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords(ImageWorkSpacePt pt) {
        if (pt==null) return null;
        return getWorldCoords(getImageCoords(pt));
    }
    /**
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImageWorkSpacePt class
     * @param ipt  the image point
     * @param outputCoordSys The coordinate system to return
     * @return WorldPt the translated coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords( ImageWorkSpacePt ipt, CoordinateSys outputCoordSys) {
        if (ipt==null) return null;
        return getWorldCoords(getImageCoords(ipt),outputCoordSys);
    }

    /**
     * Return the J2000 sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImagePt class
     * @param pt the ImageWorkSpacePt
     * @return WorldPt the translated coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords(ImagePt pt) {
        if (pt==null) return null;
        return getWorldCoords(pt, CoordinateSys.EQ_J2000);
    }

    /**
     * Return the sky coordinates given a image x (fsamp) and  y (fline)
     * package in a ImageWorkSpacePt class
     * @param ipt  the image point
     * @param outputCoordSys The coordinate system to return
     * @return WorldPt the translated coordinates
     */
    @JsNoExport
    public WorldPt getWorldCoords( ImagePt ipt, CoordinateSys outputCoordSys) {
        if (ipt==null) return null;
        double x= ipt.getX();
        double y= ipt.getY();
        WorldPt wpt = _projection.getWorldCoordsSilent(x - .5F ,y - .5F );
        if (wpt!=null && !outputCoordSys.equals(wpt.getCoordSys())) {
            wpt= VisUtil.convert(wpt, outputCoordSys);
        }
        return wpt;
    }


//========================================================================================
//----------------------------- Conversion Methods End -----------------------------------
//========================================================================================

    /**
     * Return the same point using the WorldPt object.  the x,y value is the same but a world point is return with the
     * proper coordinate system.  If a WorldPt is passed the same point is returned.
     * <i>Important</i>: This method should not be used to convert between coordinate systems.
     * Example- a ScreenPt with (1,2) will return as a WorldPt with (1,2)
     * @param pt the point to translate
     * @return WorldPt the World point with the coordinate system set
     */
    public static WorldPt getWorldPtRepresentation(Pt pt) {
        WorldPt retval= null;

        if      (pt==null)                       retval= null;
        else if (pt instanceof WorldPt)          retval= (WorldPt)pt;
        else if (pt instanceof ImageWorkSpacePt) retval= new WorldPt(pt.getX(),pt.getY(), CoordinateSys.PIXEL);
        else if (pt instanceof ImagePt)          retval= new WorldPt(pt.getX(),pt.getY(), CoordinateSys.PIXEL);
        else if (pt instanceof ScreenPt)         retval= new WorldPt(pt.getX(),pt.getY(), CoordinateSys.SCREEN_PIXEL);
        else if (pt instanceof ViewPortPt)       retval= new WorldPt(pt.getX(),pt.getY(), CoordinateSys.SCREEN_PIXEL);
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }
        return retval;
    }




    public static Pt makePt(Class<? extends Pt> cType,  double x, double y) {
        Pt retval= null;
        if      (cType==WorldPt.class)          retval= new WorldPt(x,y);
        else if (cType==ImageWorkSpacePt.class) retval= new ImageWorkSpacePt(x,y);
        else if (cType==ImagePt.class)          retval= new ImagePt(x,y);
        else if (cType==ScreenPt.class)         retval= new ScreenPt((int)x,(int)y);
        else if (cType==ViewPortPt.class)       retval= new ViewPortPt((int)x,(int)y);
        else {
            WebAssert.argTst(false, "unknown Pt type");
        }

        return retval;
    }

    /**
     * specifically release any resources held by this object
     * any subclasses who override this method should do a
     * super.freeResoureces()
     */
    public void freeResources() {
        _alive= false;
    }

    public boolean isAlive() { return _alive; }

    public void setAttribute(String key, Object attribute) {
        _attributes.put(key,attribute);
    }
    public void removeAttribute(String key) {
        _attributes.remove(key);
    }

    public Object getAttribute(String key) {
        return _attributes.get(key);
    }

    public boolean containsAttributeKey(String key) {
        return _attributes.containsKey(key);
    }

    /**
     * Get the PlotView.
     * A plot contains a reference to the PlotView that contains it.
     * A plot may be in only one PlotView.
     * @return PlotView the PlotView this plot is in.
     */
    public WebPlotView getPlotView() { return _plotGroup.getPlotView(); }

    /**
     * Set a description of this plot.
     * @param d the plot description
     */
    public void   setPlotDesc(String d) { _plotDesc= d; }

    /**
     * Get the description of this plot.
     * @return String the plot description
     */
    public String getPlotDesc()         { return _plotDesc; }

    /**
     * Get the description of this fits data for this plot.
     * @return String the plot description
     */
    public String getDataDesc()         { return _dataDesc; }


    public void setPercentOpaque(float percentOpaque) {
         _percentOpaque= percentOpaque;
    }

    public float getPercentOpaque() {
         return _percentOpaque;
    }

    public String toString() {
        return getPlotDesc();
    }

    public boolean isRotated() { return _plotState.isRotated(); }
    public PlotState.RotateType getRotationType() {
        return _plotState.getRotateType();
    }
    public double getRotationAngle() { return _plotState.getRotationAngle(); }


    public boolean isRotatable() {
        boolean retval= false;
        Projection proj= getProjection();
        if (proj!=null) {
            retval= !proj.isWrappingProjection();
            if (retval) {
                retval= !containsAttributeKey(DISABLE_ROTATE_REASON);
            }
        }
        return retval;
    }


    public String getNonRotatableReason() {
        String retval;
        if (!isRotatable()) {
            Projection p= getProjection();
            if (containsAttributeKey(DISABLE_ROTATE_REASON)) {
                if (getAttribute(DISABLE_ROTATE_REASON) instanceof String) {
                    retval= (String)getAttribute(DISABLE_ROTATE_REASON);
                }
                else {
                    retval= "FITS image can't be rotated";
                }
            }
            else {
                if (p.isWrappingProjection()) {
                    retval= "FITS image with projection of type " +
                            p.getProjectionName() +
                            " can't be rotated";

                }
                else {
                    retval= "FITS image can't be rotated";
                }
            }
        }
        else {
           retval= null;
        }
        return retval;
    }


    private Pt convertToCorrect(WorldPt wp) {
        if (wp==null) return null;
        CoordinateSys csys= wp.getCoordSys();
        Pt retPt= wp;
        if (csys.equals(CoordinateSys.SCREEN_PIXEL)) {
            retPt= new ScreenPt(wp.getX(), wp.getY());
        }
        else if (csys.equals(CoordinateSys.PIXEL)) {
            retPt= new ImagePt(wp.getX(), wp.getY());
        }
        return retPt;
    }


    private static final double    DtoR      = Math.PI/180.0;
    private static final double    RtoD      = 180.0/Math.PI;

    public boolean coordsWrap(WorldPt wp1, WorldPt wp2) {
        if (wp1==null || wp2==null) return false;

        boolean retval= false;
        if (_projection.isWrappingProjection()) {
            double worldDist= VisUtil.computeDistance(wp1, wp2);
            double pix= _projection.getPixelWidthDegree();
            double value1= worldDist/pix;

            ImageWorkSpacePt ip1= getImageWorkSpaceCoords(wp1);
            ImageWorkSpacePt ip2= getImageWorkSpaceCoords(wp2);
            if (ip1!=null && ip2!=null) {
                double xdiff= ip1.getX()-ip2.getX();
                double ydiff= ip1.getY()-ip2.getY();
                double imageDist= Math.sqrt(xdiff*xdiff + ydiff*ydiff);
                retval= ((imageDist / value1) > 3);
            }
            else {
                retval= false;
            }
        }
        return retval;
    }

    //=================================================================
    //-----------------------------------------------------------------
    // DON'T DELETE THE FOLLOWING 3 COMMENTED OUT METHODS
    // might be used someday
    //-----------------------------------------------------------------
    //=================================================================


//    public void setImagePixPadding(Dimension padDim) {
//        _padDim= padDim;
//        setOffsetX(_padDim.getWidth());
//        setOffsetY(_padDim.getHeight());
//        _plotGroup.computeMinMax();
//    }

//    public void setArcSecPadding(double padWidth, double padHeight) {
//        double scale= getImagePixelScaleInArcSec();
//        setImagePixPadding( new Dimension((int)(padWidth/scale),  (int)(padHeight/scale)));
//    }
//
//    /**
//     * This method will force total size to be a with and a height.  If the image is less
//     * than the size passed then the image is padded.  If the image size is greater then the method
//     * does nothing.
//     * @param imWidth new width in arcsec
//     * @param imHeight new height in arcsec
//     */
//    public void setImageArcSecSize(double imWidth, double imHeight) {
//        double scale= getImagePixelScaleInArcSec();
//        int newWidth= (int)(imWidth/scale);
//        int newHeight= (int)(imHeight/scale);
//        if (newWidth>=_dataWidth && newHeight>=_dataHeight) {
//            setImagePixPadding(new Dimension((newWidth-_dataWidth)/2, (newHeight-_dataHeight)/2 ));
//        }
//
//
//    }




    // =======================================================================
   // ------------------    Private / Protected / Package Methods   ---------
   // =======================================================================

//   void setOffsetX(int x) {_offsetX= x;}
//   int  getOffsetX() {return _offsetX;}
//   int  getOffsetY() {return _offsetY;}


    private static class ImageBoundsData {
//        WorldPt topLeft;
//        WorldPt topRight;
//        WorldPt bottomLeft;
//        WorldPt bottomRight;
        private final boolean wrapsRa;
        private final boolean northPole;
        private final boolean southPole;
        private final double minRa;
        private final double maxRa;
        private final double minDec;
        private final double maxDec;


        private ImageBoundsData(WebPlot plot, WorldPt topLeft, WorldPt topRight, WorldPt bottomLeft, WorldPt bottomRight) {
//            this.topLeft = topLeft;
//            this.topRight = topRight;
//            this.bottomLeft = bottomLeft;
//            this.bottomRight = bottomRight;

            double minRa= 5000;
            double maxRa= -5000;
            double minDec=5000;
            double maxDec= -5000;

            for(WorldPt wp : new WorldPt[] {topLeft, topRight, bottomLeft, bottomRight}) {
                if (wp.getLon() < minRa) minRa= wp.getLon();
                if (wp.getLon() > maxRa) maxRa= wp.getLon();
                if (wp.getLat() < minDec) minDec= wp.getLat();
                if (wp.getLat() > maxDec) maxDec= wp.getLat();
            }

            double scale= plot.getImagePixelScaleInDeg();
            Dimension dim= plot._viewPortDim;
            int wPad= dim.getWidth()/2;
            int hPad= dim.getHeight()/2;



            minRa-= (wPad *scale);
            minDec-= (hPad*scale);
            maxDec+= (hPad*scale);
            maxRa+= (wPad*scale);


            double imageSize= plot.getImagePixelScaleInDeg() * Math.max(plot.getImageDataHeight(),plot.getImageDataWidth());
            double checkDeltaTop=    90-(2*imageSize);
            double checkDeltaBottom= -90 + (2*imageSize);

            this.wrapsRa= (maxRa-minRa) > 90;
            this.northPole= minDec>checkDeltaTop;
            this.southPole= maxDec<checkDeltaBottom;
            this.minRa= minRa;
            this.maxRa= maxRa;
            this.minDec= minDec;
            this.maxDec= maxDec;
        }


        /**
         * This method returns false it the point is definitely not in plot.  It returns true if the point might be in the plot.
         * Used for tossing out points that we know that are not in plot without having to do all the math.  It is much faster.
         * @return true in we guess it might be in the bounds, false if we know that it is not in the bounds
         */
        public boolean pointInPlotRoughGuess(WorldPt wp) {

            if (!CoordinateSys.EQ_J2000.equals(wp.getCoordSys())) {
                wp= VisUtil.convert(wp,CoordinateSys.EQ_J2000);
            }
            double x= wp.getLon();
            double y= wp.getLat();


            boolean retval;
            if (northPole) { //if near the j2000 "north pole" then ignore ra check
                retval= y> minDec;
            }
            else if (southPole) { //if near the j2000 "south pole" then ignore ra check
                retval= y< maxDec;
            }
            else if (wrapsRa) { // if image wraps around 0 ra
                retval= y> minDec && y< maxDec;
                if (retval) {
                    retval= x> maxRa || x< minRa;
                }
            }
            else { // normal case
                retval= x> minRa && y> minDec && x< maxRa && y< maxDec;
            }
            return retval;
        }

    }

}
