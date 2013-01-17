package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.firefly.data.DataEntry;

import java.io.Serializable;
/**
 * User: roby
 * Date: Sep 11, 2009
 * Time: 11:16:02 AM
 */



/**
 * @author Trey Roby
 */
public class InsertBandInitializer implements Serializable, DataEntry {

    private final static String SPLIT_TOKEN= "--InsertBandInitializer--";

    private PlotImages    _initImages;
    private PlotState     _plotState;
    private WebFitsData   _fitsData;
    private Band          _band;
    private String        _dataDesc;


    private InsertBandInitializer() {}


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

    public static InsertBandInitializer parse(String s) {
        if (s==null) return null;
        String sAry[]= s.split(SPLIT_TOKEN,6);
        InsertBandInitializer retval= null;
        if (sAry.length==5) {
            try {
                int i= 0;
                PlotImages  initImages= PlotImages.parse(sAry[i++]);
                PlotState   plotState= PlotState.parse(sAry[i++]);
                WebFitsData fitsData= WebFitsData.parse(sAry[i++]);
                Band        band= Band.parse(sAry[i++]);
                String      dataDesc= getString(sAry[i]);
                retval= new InsertBandInitializer(plotState,initImages,band,fitsData,dataDesc);
            } catch (NumberFormatException e) {
                retval= null;
            }
        }
        return retval;

    }

    private static String getString(String s) { return s.equals("null") ? null : s; }
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
