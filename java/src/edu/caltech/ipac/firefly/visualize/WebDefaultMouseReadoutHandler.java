package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.conv.CoordUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: roby
 * Date: Apr 25, 2007
 * Time: 12:34:33 PM
 */


/**
 * @author Trey Roby
 */
public class WebDefaultMouseReadoutHandler implements WebMouseReadoutHandler {

    //row parameter keys
    public static final String TITLE= "TITLE";
    public static final String EQ_J2000= "EQ_J2000";
    public static final String EQ_J2000_DEG = "EQ_J2000_DEG";
    public static final String IMAGE_PIXEL = "IMAGE_PIXEL";
    public static final String GALACTIC= "GALACTIC";
    public static final String EQ_B1950= "EQ_B1950";
    public static final Result EMPTY= new Result("","");
    //public static final String FIRST_FLUX= "FIRST_FLUX";
    //public static final String PIXEL_SIZE= "PIXEL_SIZE";

    private static final int MAX_TITLE_LEN= 25;

//    private static WebClassProperties _prop= new WebClassProperties(WebDefaultMouseReadoutHandler.class);
    public final static String R1_PROP="XYPrefsDialog.r1Type.RadioValue";
    public final static String R2_PROP="XYPrefsDialog.r2Type.RadioValue";
    
    private static final int MAX_FLUXES= 100;
    private static final int MOUSE_DELAY_MS= 200;
    private static final NumberFormat _nfExp= NumberFormat.getScientificFormat();
    private static final NumberFormat _nfExpFlux= NumberFormat.getFormat("#.######E0");
    private static final NumberFormat _nf   = NumberFormat.getFormat("#.######");
    private static final NumberFormat _nfPix   = NumberFormat.getFormat("#.####");
    private static final int BASE_ROWS = 6;

//    private static String PIXEL_DESC = _prop.getName("pixelDesc");
//    private static String SCREEN_PIXEL_DESC = _prop.getName("screenPixelDesc");

    private static final int TITLE_ROW= 0;
    private static final int FIRST_FLUX_ROW = 6;
    private static final int PIXEL_SIZE_OFFSET = 1;
    private static HashMap<Integer, String > DEFAULT_ROW_PARAMS= makeDefaultRowParams();
    public enum ReadoutMode {HMS, DECIMAL }
    public enum WhichReadout {LEFT, RIGHT }
    public enum WhichDir {LON, LAT, BOTH}

    private ReadoutMode _leftMode= ReadoutMode.HMS;
    private CoordinateSys _leftCoordSys= CoordinateSys.EQ_J2000;

    private ReadoutMode _rightMode= ReadoutMode.HMS;
//    private CoordinateSys _rightCoordSys= CoordinateSys.EQ_J2000;
    private CoordinateSys _rightCoordSys= CoordinateSys.PIXEL;

    private FluxTimer _fluxTimer= new FluxTimer();
    private List<FluxCache> _savedFluxes= new ArrayList<FluxCache>(10);

    public static final Result NO_RESULT=  new Result("","");
    private boolean _attempingCtxUpdate= false;
    private WebPlot _lastPlot= null;
    private long _lastCallID= 0;
    private int _lastFluxRow;
    private int _pixelSizeRow;
    private int _screenPixelSizeRow;

    private HashMap<Integer, String > _rowParams = null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public WebDefaultMouseReadoutHandler() {
        this(DEFAULT_ROW_PARAMS);
    }

    public WebDefaultMouseReadoutHandler(HashMap<Integer, String> rowParams) {
        setReadOutRowParams(rowParams);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    public void setReadOutRowParams(HashMap<Integer, String> rowParams) {
        _rowParams = rowParams;
    }

    public void useDefaultReadOutRowRParams() {
        setReadOutRowParams(DEFAULT_ROW_PARAMS);
    }
    
    public void setMode(WhichReadout which,
                        ReadoutMode mode) {
        if (which==WhichReadout.LEFT ) {
            _leftMode= mode;
        }
        else if (which==WhichReadout.RIGHT ) {
            _rightMode= mode;
        }
        else {
            WebAssert.tst(false);
        }
    }

    public ReadoutMode getMode(WhichReadout which) {
        return (which==WhichReadout.LEFT ) ? _leftMode : _rightMode;
    }

    public void setCoordSystem(WhichReadout which,
                               CoordinateSys coordSys) {
        if (which==WhichReadout.LEFT ) {
            _leftCoordSys= coordSys;
        }
        else if (which==WhichReadout.RIGHT ) {
            _rightCoordSys= coordSys;
        }
        else {
            WebAssert.tst(false);
        }
    }

    public CoordinateSys getCoordSystem(WhichReadout which) {
        return (which==WhichReadout.LEFT ) ? _leftCoordSys : _rightCoordSys;
    }

//=======================================================================
//-------------- Method from MouseReadoutHandler Interface --------------
//=======================================================================

    public int getRows(WebPlot plot) {
        int bands= 1;
        if (plot!=null) {
            bands= plot.getBands().length;
            _lastFluxRow= FIRST_FLUX_ROW+bands-1;
            _pixelSizeRow= _lastFluxRow+PIXEL_SIZE_OFFSET;
            _screenPixelSizeRow= _pixelSizeRow+1;
            //if (_rowParams!=null && !_rowParams.containsKey(PIXEL_SIZE))
            //    _rowParams.put(PIXEL_SIZE, _pixelSizeRow);
        }
        return BASE_ROWS +bands;
    }



    public int getColumns(WebPlot plot) { return 1; }

    public int getColumnSize(int colIdx) {
        return 20;
    }

    public void computeMouseValue(WebPlot plot,
                                    WebMouseReadout readout,
                                    int row,
                                    int column,
                                    ImagePt ipt,
                                    ScreenPt screenPt,
                                    long callID) {
        Result retval= null;
        checkPlotChange(plot);

        useDefaultReadOutRowRParams();
        if (plot.getPlotView() != null &&
                plot.getPlotView().containsAttributeKey(WebPlot.READOUT_ROW_PARAMS)) {
            Object o = plot.getPlotView().getAttribute(WebPlot.READOUT_ROW_PARAMS); 
            if (o!=null && o instanceof HashMap) 
                setReadOutRowParams((HashMap<Integer, String>)o);
        }
        if (_rowParams.containsKey(row)) {
            if (_rowParams.get(row).equals(TITLE)) {
                showTitle(readout,row,column,VisUtil.getBestTitle(plot));
            }
            else if (_rowParams.get(row).equals(EQ_J2000)) {
                Projection proj= plot.getProjection();
                if (!proj.isSpecified()) {
                    retval= new Result("Projection:", "none in image");
                }
                else if (!proj.isImplemented()) {
                    retval= new Result("Projection:", "not recognized");
                }
                else {
                    retval= getBoth1(plot, ipt, screenPt);
                }
            }
            else if (_rowParams.get(row).equals(EQ_J2000_DEG)) {
                retval= getReadoutByImagePt(plot,
                                          ipt,
                                          screenPt,
                                          WhichDir.BOTH,
                                          ReadoutMode.DECIMAL,
                                          CoordinateSys.EQ_J2000);
            }
            else if (_rowParams.get(row).equals(IMAGE_PIXEL)) {
                if (!plot.isBlankImage()) retval= getBoth2(plot, ipt, screenPt);
                else                      retval= EMPTY;
            }
            else if (_rowParams.get(row).equals(GALACTIC)) {
                retval= getReadoutByImagePt(plot,
                                            ipt,
                                            screenPt,
                                            WhichDir.BOTH,
                                            ReadoutMode.DECIMAL,
                                            CoordinateSys.GALACTIC);
            }
            else if (_rowParams.get(row).equals(EQ_B1950)) {
                retval= getReadoutByImagePt(plot,
                                            ipt,
                                            screenPt,
                                            WhichDir.BOTH,
                                            ReadoutMode.HMS,
                                            CoordinateSys.EQ_B1950);
            }
        } else if (row>= FIRST_FLUX_ROW && row<=_lastFluxRow) {
            if (!plot.isBlankImage()) {
                if (_lastCallID!=callID) {
                    _lastCallID= callID;
                    Result fluxRes[]= getFlux(plot, readout, ipt);
                    Band bands[]= plot.getBands();
                    int i= 0;
                    for(Result r : fluxRes) {
                        readout.setValue(getBandOffset(plot,bands[i++]),column,r._label,r._value,r._style);
                    }
                }
                retval=null;
            }
            else {
                retval= EMPTY;
            }
        }
        else if (row==_pixelSizeRow) {
            retval= getPixelSize(plot);
        }
        else if (row==_screenPixelSizeRow) {
            retval= getScreenPixelSize(plot);
        }
//            else if (row==8) {
//                retval= getReadoutByImagePt(plot,
//                                            ipt,
//                                            screenPt,
//                                            WhichDir.BOTH,
//                                            ReadoutMode.HMS,
//                                            CoordinateSys.SCREEN_PIXEL);
//            }
        else if (row> _pixelSizeRow) {
            WebAssert.tst(false, "row should be 0-5 it is: " + row);
        }

        if (retval!=null) {
            readout.setValue(row,column,retval._label,retval._value);
        }
    }


    public void computeMouseExitValue(WebPlot plot,
                                      WebMouseReadout readout,
                                      int row,
                                      int column) {
         readout.setValue(row,column,"", "");
    }



    public Result getZoom(WebPlot plot) {
        return new Result("Zoom Level: ", plot.getZoomFact()+"");
    }

    public Result getFileSize(WebPlot plot) {
        long size= plot.getFitsData(Band.NO_BAND).getFitsFileSize();
        return new Result("File Size: ", StringUtils.getSizeAsString(size,true) );
    }

    public Result[] getFlux(WebPlot plot, final WebMouseReadout readout, ImagePt ipt) {
        Result retval[];
        _fluxTimer.cancel();

        int size= _lastFluxRow-FIRST_FLUX_ROW + 1;
        retval= new Result[size];
        FluxCache fc= getSavedFlux(plot,ipt);
        Band bands[]= plot.getBands();
        if (fc==null) {
            _fluxTimer.setupCall(ipt, plot,readout);
            _fluxTimer.schedule(MOUSE_DELAY_MS);
            for(int i=0; (i<retval.length);i++) {
                retval[i]= new Result(getFluxLabel(plot, bands[i]), "                ");
            }
        }
        else {
            for(int i=0; (i<retval.length);i++) {
                retval[i]= makeFluxResult(fc.getFlux().get(bands[i]),plot,bands[i]);
            }
        }
        return retval;
    }


    public void setFluxLater(double zValue,
                             ImagePt ipt,
                             WebMouseReadout readout,
                             WebPlot plot,
                             Band band) {

        Result result= makeFluxResult(zValue,plot,band);
        readout.setValue(getBandOffset(plot, band),0,result._label,result._value, getColorStyle(band));
        addFlux(plot,ipt,band,zValue);
    }


    private int getBandOffset(WebPlot plot, Band band) {
        Band bands[]= plot.getBands();
        int i= FIRST_FLUX_ROW;
        for(Band b : bands) {
            if (b==band) {
                break;
            }
            else {
                i++;
            }
        }
        return i;
    }


    private void showTitle(WebMouseReadout readout,
                           int row,
                           int column,
                           String title) {
        if (title!=null) {
            if (title.length()> MAX_TITLE_LEN) {
                title= title.substring(0,MAX_TITLE_LEN) + "...";
            }
            readout.setTitle(row,column,"", "<b>"+title+"</b>",true);
        }
    }

    private String getColorStyle(Band band) {
        String color;
        switch (band) {
            case RED : color= "red-color"; break;
            case GREEN : color= "green-color"; break;
            case BLUE : color= "blue-color"; break;
            case NO_BAND : color= null; break;
            default : color= null; break;
        }
        return color;

    }


    private Result makeFluxResult(double zValue, WebPlot plot, Band band) {
        Result retval;
        String colorStyle= getColorStyle(band);


        if (!Double.isNaN(zValue)) {
            String fstr= formatFlux(zValue,plot, band);
            retval= new Result(getFluxLabel(plot,band), fstr, colorStyle);
        }
        else {
            retval= new Result(getFluxLabel(plot,band), "None", colorStyle);
        }
        return retval;
    }


    private String getFluxLabel(WebPlot plot, Band band)  {
        String label;
        String fluxUnits= plot.getFitsData(band).getFluxUnits();


        String start;
        switch (band) {
            case RED : start= "Red "; break;
            case GREEN : start= "Green "; break;
            case BLUE : start= "Blue "; break;
            case NO_BAND : start= ""; break;
            default : start= ""; break;
        }

        String valStr= start.length()>0 ? "Val: " : "Value: ";

        if (fluxUnits.equalsIgnoreCase("dn")) {
            label= start + valStr;
        }
        else if (fluxUnits.equalsIgnoreCase("frames")) {
            label= start + valStr;
        }
        else if (fluxUnits.equalsIgnoreCase("")) {
            label= start + valStr;
        }
        else {
            label= start + "Flux: ";
        }
        return label;
    }

    public Result getLon1(WebPlot plot, ImagePt ipt, ScreenPt screenPt) {
        return getCoord(plot,ipt,screenPt,
                        WhichDir.LON, WhichReadout.LEFT);
    }

    public Result getLon2(WebPlot plot, ImagePt ipt, ScreenPt screenPt) {
        return getCoord(plot,ipt,screenPt,
                        WhichDir.LON, WhichReadout.RIGHT);
    }

    public static Result getPixelSize(WebPlot plot) {
        Result retval;
        if (plot != null) {
            retval= new Result("1 File Pixel: ", _nfPix.format(plot.getImagePixelScaleInArcSec())  + "\"");
        }
        else {
            retval= NO_RESULT;
        }
        return retval;
    }

    public static Result getScreenPixelSize(WebPlot plot) {
        Result retval;
        if (plot != null) {
            float size= (float)plot.getImagePixelScaleInArcSec() / plot.getZoomFact();
            retval= new Result("1 Screen Pixel: ", _nfPix.format(size)  + "\"");
        }
        else {
            retval= NO_RESULT;
        }
        return retval;
    }

    public Result getLat1(WebPlot plot, ImagePt ipt, ScreenPt screenPt) {
        return getCoord(plot,ipt,screenPt,
                        WhichDir.LAT, WhichReadout.LEFT);
    }
    public Result getLat2(WebPlot plot, ImagePt ipt, ScreenPt screenPt) {
        return getCoord(plot,ipt,screenPt,
                        WhichDir.LAT, WhichReadout.RIGHT);
    }


    public Result getBoth2(WebPlot plot, ImagePt ipt, ScreenPt screenPt) {
        return getCoord(plot,ipt,screenPt,
                WhichDir.BOTH, WhichReadout.RIGHT);
    }

    public Result getBoth1(WebPlot plot, ImagePt ipt, ScreenPt screenPt) {
        return getCoord(plot,ipt,screenPt,
                WhichDir.BOTH, WhichReadout.LEFT);
    }

    private Result getCoord(WebPlot plot,
                            ImagePt ipt,
                            ScreenPt screenPt,
                            WhichDir dir,
                            WhichReadout readout) {

//        double x= screenPt.getIX();
//        double y= screenPt.getIY();
        Result retval= NO_RESULT;
        CoordinateSys coordSys;
        ReadoutMode mode;

        if (readout==WhichReadout.LEFT) {
            coordSys= _leftCoordSys;
            mode= _leftMode;
        }
        else if (readout==WhichReadout.RIGHT) {
            coordSys= _rightCoordSys;
            mode= _rightMode;
        }
        else {
            WebAssert.tst(false);
            coordSys= null;
            mode= null;
        }


        if (coordSys == null)  coordSys= plot.getCoordinatesOfPlot();
        if (plot == null) return NO_RESULT;
//        if (x > plot.getScreenWidth() || y > plot.getScreenHeight()) {
//            retStr= "";
//        }
//        else {
            if (coordSys.equals(CoordinateSys.SCREEN_PIXEL)) {
                if (dir==WhichDir.LON) {
                    retval= getReadoutByPixel(plot, dir, screenPt.getIX());
                }
                else if (dir==WhichDir.LAT) {
                    retval= getReadoutByPixel(plot, dir, screenPt.getIY());
                }
                else {
                    WebAssert.tst(false);
                }
            }
            else if (ipt != null) {
                retval= getReadoutByImagePt(plot, ipt, screenPt, dir,
                                            mode, coordSys);
            }
            else {
                retval= NO_RESULT;
            }
//        } // end else
        return retval;
    }

    public static HashMap<Integer, String> makeDefaultRowParams() {
        HashMap<Integer, String> retval = new HashMap<Integer,String> (3);
        retval.put(0, TITLE);
        retval.put(1, EQ_J2000);
        retval.put(2, EQ_J2000_DEG);
        retval.put(3, IMAGE_PIXEL);
        retval.put(4, GALACTIC);
        retval.put(5, EQ_B1950);

        return retval;
    }

    public static Result getReadoutByImagePt(WebPlot plot,
                                       ImagePt ip,
                                       ScreenPt screenPt,
                                       WhichDir dir,
                                       ReadoutMode mode,
                                       CoordinateSys coordSys) {
        Result retval;
        if (plot == null) {
            retval= NO_RESULT;
        }
        else if (coordSys.equals(CoordinateSys.PIXEL)) {
            if (dir==WhichDir.BOTH) {
                retval= getDecimalBoth(ip.getX()-0.5, ip.getY()-0.5 , coordSys.getShortDesc());
            }
            else {
                retval= getDecimalXY(getValue(ip,dir)-0.5 , dir, coordSys);
            }
        }
        else if (coordSys.equals(CoordinateSys.SCREEN_PIXEL)) {
            if (dir==WhichDir.BOTH) {
                retval= getDecimalBoth(screenPt.getIX(), screenPt.getIY(), coordSys.getShortDesc());
            }
            else {
                retval= getDecimalXY(getValue(screenPt,dir), dir,
                        CoordinateSys.SCREEN_PIXEL);
            }
        }
        else {
            try {
                WorldPt degPt= plot.getWorldCoords(ip, coordSys);
                if (mode == ReadoutMode.HMS) {
                    if (dir==WhichDir.BOTH) {
                        retval= getHmsBoth(degPt.getLon(),degPt.getLat(), coordSys);
                    }
                    else {
                        retval= getHmsXY(getValue(degPt,dir), dir, coordSys);
                    }
                }
                else if (mode == ReadoutMode.DECIMAL) {
                    if (dir==WhichDir.BOTH) {
                        retval= getDecimalBoth(degPt.getX(),degPt.getY(),  coordSys.getShortDesc());
                    }
                    else {
                        retval= getDecimalXY(getValue(degPt,dir), dir, coordSys);
                    }
                }
                else {
                    WebAssert.tst(false);
                    retval= NO_RESULT;
                }
            } catch (ProjectionException pe) {
                retval= NO_RESULT;
            }
        }
        return retval;
    }

    private static Result getHmsXY(double val, WhichDir which, CoordinateSys coordSys) {
        Result retval;
        try {
            if (which==WhichDir.LON) {
                retval= new Result(coordSys.getlonShortDesc() ,
                        CoordUtil.convertLonToString(val, coordSys.isEquatorial()));
            }
            else if (which==WhichDir.LAT) {
                retval= new Result(coordSys.getlatShortDesc() ,
                        CoordUtil.convertLatToString(val, coordSys.isEquatorial()));
            }
            else {
                WebAssert.tst(false);
                retval= NO_RESULT;
            }
        } catch (CoordException ce) {
            retval= NO_RESULT;
        }
        return retval;
    }

    public static Result getHmsBoth(double lon, double lat, CoordinateSys coordSys) {
        Result retval;
        try {
            String lonStr= CoordUtil.convertLonToString(lon, coordSys.isEquatorial());
            String latStr= CoordUtil.convertLatToString(lat, coordSys.isEquatorial());
            retval= new Result(coordSys.getShortDesc(), lonStr +", "+latStr);
        } catch (CoordException ce) {
            retval= NO_RESULT;
        }
        return retval;
    }

    private static Result getReadoutByPixel(WebPlot plot, WhichDir which, int val) {
        Result retval;
        if (plot == null) {
            retval= NO_RESULT;
        }
        else {
            retval= getDecimalXY(val, which ,CoordinateSys.SCREEN_PIXEL);
        }
        return retval;
    }


    private static Result getDecimalXY(double val,
                                       WhichDir dir,
                                       CoordinateSys coordSys) {

        String desc= null;
        if (dir==WhichDir.LON) {
            desc= coordSys.getlonShortDesc();
        }
        else if (dir==WhichDir.LAT) {
            desc= coordSys.getlatShortDesc();
        }
        else {
            WebAssert.tst(false);
        }
        return new Result(desc , _nf.format(val));
    }

    private static Result getDecimalBoth(double x,
                                         double y,
                                         String desc) {

        return new Result(desc , _nf.format(x) +", " + _nf.format(y));
    }


    private static double getValue(ScreenPt pt, WhichDir dir) {
        double val;
        if (dir==WhichDir.LON) {
            val= pt.getIX();
        }
        else if (dir==WhichDir.LAT) {
            val= pt.getIY();
        }
        else {
            WebAssert.tst(false);
            val= 0;
        }
        return val;
    }

    private static double getValue(Pt pt, WhichDir dir) {
        double val;
        if (dir==WhichDir.LON) {
            val= pt.getX();
        }
        else if (dir==WhichDir.LAT) {
            val= pt.getY();
        }
        else {
            WebAssert.tst(false);
            val= 0;
        }
        return val;
    }

//    public static String formatReadoutByImagePt(WebDefaultMouseReadoutHandler.WhichReadout which,
//				 WebPlot plot, ImageWorkSpacePt ipt, String separator) {
//        String retval;
//        try {
//            ScreenPt screenPt = null;
//            ReadoutMode readoutMode = ReadoutMode.HMS;
//            CoordinateSys coordSys= CoordinateSys.EQ_J2000;
//            if (coordSys == null)  coordSys= plot.getCoordinatesOfPlot();
//            if (coordSys.equals(CoordinateSys.SCREEN_PIXEL)) {
//                screenPt = plot.getScreenCoords(ipt);
//            }
//	    ImagePt ip = new ImagePt(ipt.getX(), ipt.getY());
//            retval = getReadoutByImagePt(plot, ip, screenPt, WhichDir.LON, readoutMode, coordSys) +
//                     separator +
//                     getReadoutByImagePt(plot, ip, screenPt, WhichDir.LAT, readoutMode, coordSys);
//            return retval;
//        } catch (Exception e) {
//            return " ";
//        }
//    }



    private void findFluxTry2(final WebPlot plot,
                              final WebMouseReadout readout,
                              final ImagePt pt)  {
        if (!_attempingCtxUpdate) {
            _attempingCtxUpdate= true;
            plot.getFlux( pt,new AsyncCallback<double[]>() {
                public void onFailure(Throwable throwable) {
                    _attempingCtxUpdate= false;
                }

                public void onSuccess(double flux[]) {


                    Band bands[]= plot.getBands();
                    _attempingCtxUpdate= false;
                    for(int i= 0; (i<bands.length); i++) {
                        setFluxLater(flux[i],pt, readout,plot, bands[i]);
                    }
                }
            });
        }
    }



    private void findFlux(final WebPlot plot,
                          final WebMouseReadout readout,
                          final ImagePt pt)  {
        plot.getFluxLight( pt,new AsyncCallback<String[]>() {
            public void onFailure(Throwable throwable) {
                for (Band band : plot.getBands()) {
                    setFluxLater(Double.NaN,pt,
                                 readout,plot,band);
                }
            }

            public void onSuccess(String[] strFlux) {
                if (strFlux!=null && strFlux.length>0 && !PlotState.NO_CONTEXT.equals(strFlux[0])) {
                    Band bands[]= plot.getBands();
                    for(int i= 0; (i<bands.length); i++) {
                        double val= Double.parseDouble(strFlux[i]);
                        setFluxLater(val,pt,
                                     readout,plot,bands[i]);
                    }
                }
                else {
                    Band bands[]= plot.getBands();
                    for (int i=0; (i<bands.length); i++) {
                        readout.setValue(FIRST_FLUX_ROW+i,0,getFluxLabel(plot,bands[i]),"Reloading...",
                                         getColorStyle(bands[i]));
                    }
                    findFluxTry2(plot,readout,pt);
                }
            }
        });
    }

    private void checkPlotChange(WebPlot currPlot) {
        if (currPlot!=_lastPlot) {
            _lastPlot= currPlot;
            _savedFluxes.clear();

        }
    }



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private FluxCache getSavedFlux(WebPlot p, ImagePt pt) {
        FluxCache retval= null;
        for(FluxCache fc : _savedFluxes) {
            if (fc.testMatch(p,pt)) {
                retval= fc;
                break;
            }
        }
        return retval;
    }

    private void addFlux(WebPlot p, ImagePt pt, Band band, double flux) {
        // purge old
        FluxCache min= null;
        if (_savedFluxes.size() >= MAX_FLUXES) {
            for(FluxCache fc : _savedFluxes) {
                if (min==null || min.getTime()>=fc.getTime()) {
                    min= fc;
                }
            }
            WebAssert.tst(min!=null,
                          "Did not find a saved flux to delete");
            _savedFluxes.remove(min);
        }


        // add new
        FluxCache newFC= getSavedFlux(p,pt);
        if (newFC==null) {
            newFC= new FluxCache(p,pt);
            _savedFluxes.add(newFC);
        }
        newFC._fluxMap.put(band,flux);


    }

// =====================================================================
// -------------------- Inner classes --------------------------------
// =====================================================================

    private class FluxTimer extends Timer {
        private ImagePt _pt;
        private WebPlot _plot;
        private WebMouseReadout _readout;

        public void run() { findFlux(_plot, _readout, _pt); }

        public void setupCall(ImagePt pt,
                              WebPlot plot,
                              WebMouseReadout readout) {
            _pt= pt;
            _plot= plot;
            _readout= readout;
        }
    }

    private static class FluxCache {
        private final WebPlot _plot;
        private final Map<Band,Double> _fluxMap= new HashMap<Band,Double>(3);
        private final ImagePt _pt;
        private final long _date;

        public FluxCache(WebPlot plot, ImagePt pt) {
            _plot= plot;
            _pt= pt;
            _date= new Date().getTime();
        }
        public boolean equals(Object o) {
            boolean retval= false;
            if (o instanceof FluxCache) {
                FluxCache other= (FluxCache)o;
                retval= (_plot==other._plot) && other._pt.equals(_pt);
            }
            return retval;
        }


        public boolean testMatch(WebPlot plot, ImagePt pt) {
            return (_plot==plot) && pt.equals(_pt);
        }

        public Map<Band,Double> getFlux() {return _fluxMap; }
        public long getTime() { return _date;}
    }


    /**
     * Return the formatted value for this flux value
     * @param value the flux value
     * @param plot the plot the value is associated with
     * @param band the  color band
     * @return formatted flux with units
     */
    public static String formatFlux(double value, WebPlot plot, Band band) {
        String fluxUnits= plot.getFitsData(band).getFluxUnits();
        return formatFlux(value)+ " " + fluxUnits;

    }

    /**
     * Return the formatted value for this flux value
     * @param value the flux value
     * @return formatted flux
     */
    public static String formatFlux(double value) {
        String fstr;
        double absV= Math.abs(value);
        if (absV < 0.01 || absV >= 1000.) {
            fstr= _nfExpFlux.format(value);
        }
        else {
            fstr= _nf.format(value);
        }
        return fstr;

    }


    public static class Result {
        public  final String _label;
        public  final String _value;
        private final String _style;

        public Result( String label, String value) {
            this(label,value,null);
        }

        public Result( String label, String value, String style) {
            _label= label;
            _value= value;
            _style = style;
        }

    }



}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
