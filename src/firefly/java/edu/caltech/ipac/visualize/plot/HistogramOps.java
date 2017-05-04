/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.Band;

/**
 * Date: May 27, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class HistogramOps {

    private FitsRead  fitsReadAry[];
    private ImageDataGroup _imageData;
    private Band band;


//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================
    public HistogramOps(FitsRead fitsReadAry[], Band band, ImageDataGroup imageData) {
        this.fitsReadAry= fitsReadAry;
        this.band= band;
        _imageData= imageData;
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================


    public void recomputeStretch(RangeValues rangeValues) {
        recomputeStretch(rangeValues,false);
    }

    public void recomputeStretch(RangeValues rangeValues, boolean force) {
        _imageData.recomputeStretch(fitsReadAry, band.getIdx(), rangeValues,force);
    }

    public FitsRead getFitsRead() { return fitsReadAry[band.getIdx()]; }

    //5/13/16 LZ added this method so the beta can be passed to the client side
    public double getBeta(){ return fitsReadAry[band.getIdx()].getDefaultBeta();}

    public Histogram getDataHistogram() {
        return fitsReadAry[band.getIdx()].getHistogram();
    }

    public byte [] getDataHistogramColors(Histogram hist, RangeValues rangeValues) {
        return fitsReadAry[band.getIdx()].getHistColors(hist, rangeValues);
    }

    public double getDataMin(Histogram hist) {
        ImageHeader imageHeader= fitsReadAry[band.getIdx()].getImageHeader();
        return hist.getDNMin() * imageHeader.bscale + imageHeader.bzero;
    }
    public double getDataMax(Histogram hist) {
        ImageHeader imageHeader= fitsReadAry[band.getIdx()].getImageHeader();
        return hist.getDNMax() * imageHeader.bscale + imageHeader.bzero;
    }
    /**
     * @param bin The bin index in the histogram
     * @return    The mean value in the image corresponding to the specified bin
     */
    public double getMeanValueFromBin(Histogram hist, int bin) {
        ImageHeader imageHeader= fitsReadAry[band.getIdx()].getImageHeader();
        return hist.getDNfromBin(bin) * imageHeader.bscale + imageHeader.bzero;
    }

//    /**
//     * @param  v the mean value for the bin
//     * @return  bin  The histogram index corresponding to the DN value
//     */
//    public int getBinFromMeanValue(double v) {
//        ImageHeader imageHeader= _fitsRead.getImageHeader();
//        double dn= (v - imageHeader.bzero) / imageHeader.bscale;
//        return _fitsRead.getHistogram().getBinfromDN(dn);
//    }
//
//    /**
//     * @param pct The percentile on the histogram (99.0 signifies 99%)
//     * @param round_up Use the upper edge of the bin (versus the lower edge)
//     * @return    The histogram index corresponding to the percentile
//     */
//    public int getBINfromPercentile(double pct, boolean round_up) {
//        return _fitsRead.getHistogram().getBINfromPercentile(pct, round_up);
//    }
//
//    /**
//     * @param sigma The sigma multiplier
//     * @param round_up Use the upper edge of the bin (versus the lower edge)
//     * @return    The histogram index corresponding to the percentile
//     */
//    public int getBINfromSigma(double sigma, boolean round_up) {
//        return _fitsRead.getHistogram().getBINfromSigma(sigma, round_up);
//    }


}

