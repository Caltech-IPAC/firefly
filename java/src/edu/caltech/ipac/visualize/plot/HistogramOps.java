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

