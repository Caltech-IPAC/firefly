/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

/**
 * Date: May 27, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class HistogramOps {

    private FitsRead fitsReadAry[];
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

    /**
     * @param bin The bin index in the histogram
     * @return    The mean value in the image corresponding to the specified bin
     */
    public double getMeanValueFromBin(Histogram hist, int bin) {
        FitsRead fr= fitsReadAry[band.getIdx()];
        return hist.getDNfromBin(bin) * fr.getBscale() + fr.getBzero();
    }
}
