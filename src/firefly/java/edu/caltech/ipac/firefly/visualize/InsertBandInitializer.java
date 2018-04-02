/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.io.Serializable;
/**
 * User: roby
 * Date: Sep 11, 2009
 * Time: 11:16:02 AM
 */



/**
 * @author Trey Roby
 */
public class InsertBandInitializer implements Serializable {

    private final static String SPLIT_TOKEN= "--InsertBandInitializer--";

    private PlotImages    _initImages;
    private PlotState     _plotState;
    private WebFitsData   _fitsData;
    private Band          _band;
    private String        _dataDesc;


    public InsertBandInitializer(PlotState plotState,
                             PlotImages    images,
                             Band          band,
                             WebFitsData   fitsData,
                             String        dataDesc) {

        _plotState= plotState;
        _initImages= images;
        _band= band;
        _fitsData= fitsData;
        _dataDesc= dataDesc;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public PlotState getPlotState() { return _plotState; }
    public PlotImages getImages() { return _initImages; }
    public WebFitsData getFitsData()  { return _fitsData; }
    public Band getBand()  { return _band; }
    public String getDataDesc()  { return _dataDesc; }

    public String toString() {
        return _initImages +SPLIT_TOKEN+
               _plotState  +SPLIT_TOKEN+
               _fitsData   +SPLIT_TOKEN+
               _band       +SPLIT_TOKEN+
               _dataDesc;
    }


    private static String getString(String s) { return s.equals("null") ? null : s; }
}
