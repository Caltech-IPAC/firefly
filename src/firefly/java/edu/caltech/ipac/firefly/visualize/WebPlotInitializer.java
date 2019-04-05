/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import nom.tam.fits.Header;

import java.util.List;
/**
 * User: roby
 * Date: Sep 11, 2009
 * Time: 11:16:02 AM
 */


/**
 * @author Trey Roby
 */
public class WebPlotInitializer {

    private CoordinateSys _imageCoordSys;
    private int           _dataWidth;
    private int           _dataHeight;
    private int           _imageScaleFactor;
    private PlotImages    _initImages;
    private PlotState     _plotState;
    private WebFitsData   _fitsData[];
    private String        _desc;
    private String        _dataDesc;
    private Header headerAry[]; //passed with non-cube images, length 1 for normal images, up to 3 for 3 color images
    private transient List<RelatedData> relatedData;
//    private transient Projection _projection;


    public WebPlotInitializer(PlotState plotState,
                             PlotImages images,
                             CoordinateSys imageCoordSys,
                             Header headerAry[] ,
                             int dataWidth,
                             int dataHeight,
                             int imageScaleFactor,
                             WebFitsData  fitsData[],
                             String desc,
                             String dataDesc,
                             List<RelatedData> relatedData ) {

        _plotState= plotState;
        _initImages= images;
        _imageCoordSys= imageCoordSys;
//        _projection= projection;
        _dataWidth= dataWidth;
        _dataHeight= dataHeight;
        _imageScaleFactor= imageScaleFactor;
        _fitsData= fitsData;
        _desc= desc;
        _dataDesc= dataDesc;
        this.headerAry = headerAry;
        this.relatedData= relatedData;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    
    public PlotState getPlotState() { return _plotState; }
    public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }
    public PlotImages getInitImages() { return _initImages; }

//    public Projection getProjection() {
//        return _projection;
//    }
    public List<RelatedData> getRelatedData() { return  relatedData; }

    public int getDataWidth() { return _dataWidth; }
    public int getDataHeight() { return _dataHeight; }
    public int getImageScaleFactor() { return _imageScaleFactor; }
    public WebFitsData[] getFitsData()  { return _fitsData; }

    public String getPlotDesc() { return _desc; }
    public void setPlotDesc(String d) { _desc= d; }
    public String getDataDesc() { return _dataDesc; }
    public Header[] getHeaderAry() { return this.headerAry; }


    private static String getString(String s) { return s.equals("null") ? null : s; }
}

