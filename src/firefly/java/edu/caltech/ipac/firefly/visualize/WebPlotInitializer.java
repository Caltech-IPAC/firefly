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

    private final CoordinateSys _imageCoordSys;
    private final int           _dataWidth;
    private final int           _dataHeight;
    private final PlotImages    _initImages;
    private final PlotState     _plotState;
    private final WebFitsData[] _fitsData;
    private final String        _desc;
    private final String        _dataDesc;
    private final Header[] headerAry; //passed with non-cube images, length 1 for normal images, up to 3 for 3 color images
    private final Header[] zeroHeaderAry; //passed with non-cube images, length 1 for normal images, up to 3 for 3 color images
    private final transient List<RelatedData> relatedData;


    public WebPlotInitializer(PlotState plotState,
                             PlotImages images,
                             CoordinateSys imageCoordSys,
                             Header[] headerAry,
                             Header[] zeroHeaderAry,
                             int dataWidth,
                             int dataHeight,
                             WebFitsData[] fitsData,
                             String desc,
                             String dataDesc,
                             List<RelatedData> relatedData ) {

        _plotState= plotState;
        _initImages= images;
        _imageCoordSys= imageCoordSys;
//        _projection= projection;
        _dataWidth= dataWidth;
        _dataHeight= dataHeight;
        _fitsData= fitsData;
        _desc= desc;
        _dataDesc= dataDesc;
        this.headerAry = headerAry;
        this.zeroHeaderAry = zeroHeaderAry;
        this.relatedData= relatedData;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================
    
    public PlotState getPlotState() { return _plotState; }
    public CoordinateSys getCoordinatesOfPlot() { return _imageCoordSys; }
    public PlotImages getInitImages() { return _initImages; }

    public List<RelatedData> getRelatedData() { return  relatedData; }

    public int getDataWidth() { return _dataWidth; }
    public int getDataHeight() { return _dataHeight; }
    public WebFitsData[] getFitsData()  { return _fitsData; }

    public String getPlotDesc() { return _desc; }
    public String getDataDesc() { return _dataDesc; }
    public Header[] getHeaderAry() { return this.headerAry; }
    public Header[] getZeroHeaderAry() { return this.zeroHeaderAry; }


    private static String getString(String s) { return s.equals("null") ? null : s; }
}

