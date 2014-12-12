package edu.caltech.ipac.visualize.plot;

/**
 * Date: May 27, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class HistogramOps {

    private FitsRead  _fitsRead;
    private ImageDataGroup _imageData;


//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================
    public HistogramOps(FitsRead fitsRead, ImageDataGroup imageData) {
        _fitsRead= fitsRead;
        _imageData= imageData;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================


    public void recomputeStretch(RangeValues rangeValues) {
        recomputeStretch(rangeValues,false);
    }

    public void recomputeStretch(RangeValues rangeValues, boolean force) {
        _imageData.recomputeStretch(_fitsRead, rangeValues,force);
    }

    public FitsRead getFitsRead() { return _fitsRead; }

    public RangeValues getCurrentStretch() {
        return _fitsRead.getRangeValues();
    }

    public int [] getDataHistogram() {
        return _fitsRead.getHistogram().getHistogramArray();
    }

    public byte [] getDataHistogramColors() {
        return _fitsRead.getHistColors();
    }


    public int [] getColorHistogram() {
        return _fitsRead.getScreenHistogram();
    }

    public double getDataMin() {
        ImageHeader imageHeader= _fitsRead.getImageHeader();
        return _fitsRead.getHistogram().getDNMin() *
               imageHeader.bscale + imageHeader.bzero;
    }
    public double getDataMax() {
        ImageHeader imageHeader= _fitsRead.getImageHeader();
        return _fitsRead.getHistogram().getDNMax() *
               imageHeader.bscale + imageHeader.bzero;
    }
    /**
     * @param bin The bin index in the histogram
     * @return    The mean value in the image corresponding to the specified bin
     */
    public double getMeanValueFromBin(int bin) {
        ImageHeader imageHeader= _fitsRead.getImageHeader();
        return _fitsRead.getHistogram().getDNfromBin(bin) *
               imageHeader.bscale + imageHeader.bzero;
    }

    /**
     * @param  v the mean value for the bin
     * @return  bin  The histogram index corresponding to the DN value
     */
    public int getBinFromMeanValue(double v) {
        ImageHeader imageHeader= _fitsRead.getImageHeader();
        double dn= (v - imageHeader.bzero) / imageHeader.bscale;
        return _fitsRead.getHistogram().getBinfromDN(dn);
    }

    /**
     * @param pct The percentile on the histogram (99.0 signifies 99%)
     * @param round_up Use the upper edge of the bin (versus the lower edge)
     * @return    The histogram index corresponding to the percentile
     */
    public int getBINfromPercentile(double pct, boolean round_up) {
        return _fitsRead.getHistogram().getBINfromPercentile(pct, round_up);
    }

    /**
     * @param sigma The sigma multiplier
     * @param round_up Use the upper edge of the bin (versus the lower edge)
     * @return    The histogram index corresponding to the percentile
     */
    public int getBINfromSigma(double sigma, boolean round_up) {
        return _fitsRead.getHistogram().getBINfromSigma(sigma, round_up);
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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
