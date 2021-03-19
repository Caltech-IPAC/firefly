/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.Zscale;

import java.util.Arrays;

import static edu.caltech.ipac.visualize.plot.plotdata.ImageStretchUtil.getScaled;
import static edu.caltech.ipac.visualize.plot.plotdata.ImageStretchUtil.getSlow;
import static edu.caltech.ipac.visualize.plot.plotdata.ImageStretchUtil.getZscaleValue;

/**
 * This class is used to store intensity-related info for RGB hue preserving algorithm.
 * It accumulates range values and calculates the statistics for the intensity, when the range value info
 * for all three bands is available.
 * Intensity and the related values are recalculated only when the black points (as reflected by range values) are changing.
 * @author tatianag
 */
public class RGBIntensity {

    private RangeValues[] _rangeValuesAry;

    private String _intensityCacheKey; // derived from range values: when black points change, intensity needs to be recalculated

    // the values below are good for the intensity produced with the intensityCacheKey
    private float _intensityDataLow; // minimum data value for intensity
    private float _intensityDataHigh; // maximum data value for intensity
    private double _intensityLow; // lower range for intensity
    private double _intensityHigh; // upper range for intensity

    public RGBIntensity() {
        _rangeValuesAry = null;
        _intensityCacheKey = "";
        _intensityDataLow = Float.NaN;
        _intensityDataHigh = Float.NaN;
        _intensityLow = Double.NaN;
        _intensityHigh = Double.NaN;
    }

    public void addRangeValues(FitsRead[] fitsReadAry, int idx, RangeValues rv) {
        if (_rangeValuesAry == null) {
            _rangeValuesAry = new RangeValues[3];
        }
        _rangeValuesAry[idx] = rv;
        if (idx == 2 && !_intensityCacheKey.equals(getCacheKey(_rangeValuesAry))) {
            computeRGBIntensityStats(fitsReadAry, _rangeValuesAry);
            _rangeValuesAry = null;
        }
    }

    /**
     * Intensity and the limits needs to be recalculated when the black points of the bands change.
     * If the key is the same, no need to recalculate.
     * @param rangeValuesAry and array of red, green, and blue range values
     * @return a string describing black points, derived from range values
     */
    private static String getCacheKey(RangeValues[] rangeValuesAry) {
        StringBuilder key = new StringBuilder("");
        if (rangeValuesAry==null) { return key.toString(); }
        for (RangeValues rv : rangeValuesAry)  {
            key.append(rv.getScalingK()).append(",").append(rv.getLowerWhich()).append(",").append(rv.getLowerValue()).append(";");
        }
        return key.toString();
    }

    private void computeRGBIntensityStats(FitsRead[] fitsReadAry, RangeValues[] rangeValuesAry) {

        if (Arrays.asList(fitsReadAry).contains(null)) {
            throw new IllegalArgumentException("fitsReadAry should contain 3 non-null values for hue-preserving RGB.");
        }

        float[][] float1dAry= new float[3][];
        ImageHeader imageHeaderAry[]= new ImageHeader[3];
        double blankPxValAry[] = new double[3];
        Histogram[] histAry= new Histogram[3];
        for(int i=0; i<3; i++) {
            float1dAry[i]= fitsReadAry[i].getRawFloatAry();
            imageHeaderAry[i]= new ImageHeader(fitsReadAry[i].getHeader());
            blankPxValAry[i]= imageHeaderAry[i].blank_value;
            histAry[i]= fitsReadAry[i].getHistogram();
        }
        for(int i=0; (i<float1dAry.length); i++) {
            if (float1dAry[i]==null || imageHeaderAry[i]==null || histAry[i]==null) {
                throw new IllegalArgumentException("3 bands are required for hue preserving stretch.");
            }
        }
        if (imageHeaderAry[0].naxis1!=imageHeaderAry[1].naxis1 || imageHeaderAry[1].naxis1!=imageHeaderAry[2].naxis1 ||
                imageHeaderAry[0].naxis2!=imageHeaderAry[1].naxis2 || imageHeaderAry[1].naxis2!=imageHeaderAry[2].naxis2) {
            throw new IllegalArgumentException("Hue-preserving stretch: naxis1 and naxis2 must match. "+
                    "r: ("+imageHeaderAry[0].naxis1+","+imageHeaderAry[0].naxis2+") "+
                    "g: ("+imageHeaderAry[1].naxis1+","+imageHeaderAry[1].naxis2+") "+
                    "b: ("+imageHeaderAry[2].naxis1+","+imageHeaderAry[2].naxis2+")");
        }

        // green and blue images might be reprojected, hence it is important to work with scaled values

        double [] slowAry = new double[3];
        for(int i=0; i<3; i++) {
            blankPxValAry[i]= imageHeaderAry[i].blank_value;
            ImageHeader iH= imageHeaderAry[i];
            slowAry[i] = getSlow(rangeValuesAry[i], float1dAry[i], histAry[i], iH.bzero, iH.bscale, iH.naxis1,
                                               iH.naxis2, iH.bitpix, iH.blank_value);
            slowAry[i] = getScaled(slowAry[i], iH, rangeValuesAry[i]);
        }
        float [] intensity = new float[float1dAry[0].length];
        Arrays.fill(intensity, Float.NaN);
        float val, minVal = Float.MAX_VALUE, maxVal=Float.MIN_VALUE;
        for (int i=0; i<float1dAry[0].length; i++) {
            // check for blank pixel values
            if (float1dAry[0][i] == blankPxValAry[0] || float1dAry[1][i] == blankPxValAry[1] || float1dAry[2][i] == blankPxValAry[1]) {
                continue;
            }

            val = computeIntensity(i, float1dAry, imageHeaderAry, slowAry, rangeValuesAry);

            // save min and max
            if (val < minVal) { minVal = val; }
            if (val > maxVal) { maxVal = val; }

            intensity[i] = val;
        }
        _intensityDataLow = (minVal != Float.MAX_VALUE) ? minVal : Float.NaN;
        _intensityDataHigh = (maxVal != Float.MAX_VALUE) ? maxVal : Float.NaN;

        // zscale settings are shared between bands
        boolean useZ = rangeValuesAry[0].getLowerWhich()==RangeValues.ZSCALE;
        if (useZ || !Double.isFinite(rangeValuesAry[0].getAsinhStretch())) {
            // for zscale only: calculate z1 and z2 for intensity
            // use the last image header, because after reprojection, bzero and bscale are removed in green and blue
            // zscale parameters are shared between range values, no matter range values which to use
            ImageHeader ih= imageHeaderAry[2];
            Zscale.ZscaleRetval zscale_retval = getZscaleValue(intensity, ih.naxis1, ih.naxis2,
                    ih.bitpix, ih.blank_value,rangeValuesAry[0]);
            _intensityLow = zscale_retval.getZ1();
            _intensityHigh = zscale_retval.getZ2();
        } else {
            _intensityLow = Double.NaN;
            _intensityHigh = Double.NaN;
        }
        _intensityCacheKey = getCacheKey(rangeValuesAry);
    }

    float getIntensityDataLow() { return _intensityDataLow; }
    float getIntensityDataHigh() { return _intensityDataHigh; }
    float getIntensityLow() { return (float)_intensityLow; }
    float getIntensityHigh() { return (float)_intensityHigh; }

    static float computeIntensity(int i, float[][] float1dAry, ImageHeader[] imageHeaderAry, double[] slowAry, RangeValues[] rangeValuesAry) {
        double val = (getScaled(float1dAry[0][i], imageHeaderAry[0], rangeValuesAry[0])-slowAry[0]+
                            getScaled(float1dAry[1][i], imageHeaderAry[1], rangeValuesAry[1])-slowAry[1]+
                            getScaled(float1dAry[2][i], imageHeaderAry[2],rangeValuesAry[2])-slowAry[2])/3.0;
        return val > 0 ? (float)val : 0f;
    }


}
