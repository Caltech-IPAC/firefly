/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.Zscale;

class ImageStretchUtil {
    /**
     * The Bscale  keyword shall be used, along with the BZERO keyword, when the array pixel values are not the true  physical  values,
     * to transform the primary data array  values to the true physical values they represent, using Eq. 5.3. The value field shall contain a
     * floating point number representing the coefficient of the linear term in the scaling equation, the ratio of physical value to array value
     * at zero offset. The default value for this keyword is 1.0.BZERO Keyword
     * BZERO keyword shall be used, along with the BSCALE keyword, when the array pixel values are not the true  physical values, to transform
     * the primary data array values to the true values. The value field shall contain a floating point number representing the physical value corresponding to an array value of zero. The default value for this keyword is 0.0.
     * The transformation equation is as follows:
     * physical_values = BZERO + BSCALE × array_value	(5.3)
     * <p>
     * This method return the physical data value for the given raw value with scaling coefficient applied 
     *
     * @param raw_dn raw data value
     * @param imageHeader image header
     * @param rv range values
     * @return physical data value
     */
    static double getScaled(double  raw_dn, ImageHeader imageHeader, RangeValues rv) {
        if ((raw_dn == imageHeader.blank_value) || (Double.isNaN(raw_dn))) {
            return Double.NaN;
        } else {
            return rv.getScalingK()*(raw_dn * imageHeader.bscale + imageHeader.bzero);
        }
    }

    static double getShigh(RangeValues rangeValues, float[] float1d, Histogram hist, double bzero, double bscale,
                              int naxis1, int naxis2, double blank_value) {
        return switch (rangeValues.getUpperWhich()) {
            case RangeValues.ABSOLUTE -> (rangeValues.getUpperValue() - bzero) / bscale;
            case RangeValues.PERCENTAGE -> hist.get_pct(rangeValues.getUpperValue(), true);
            case RangeValues.SIGMA -> hist.get_sigma(rangeValues.getUpperValue(), true);
            case RangeValues.ZSCALE -> getZscaleValue(float1d, naxis1, naxis2, blank_value, rangeValues).z2();
            default -> 0;
        };
    }

    static Zscale.ZscaleRetval getZscaleValue(float[] float1d, int naxis1, int naxis2, double blank_value, RangeValues rangeValues) {
        double contrast = rangeValues.getZscaleContrast();
        int optSize = rangeValues.getZscaleSamples();
        int lenStdline = rangeValues.getZscaleSamplesPerLine();
        return Zscale.cdl_zscale(float1d, naxis1, naxis2, contrast / 100.0, optSize, lenStdline, blank_value );
    }

    static double getSlow(RangeValues rangeValues, float[] float1d, Histogram hist, double bzero, double bscale,
                          int naxis1, int naxis2, double blank_value) {
        return switch (rangeValues.getLowerWhich()) {
            case RangeValues.ABSOLUTE ->  (rangeValues.getLowerValue() - bzero) / bscale;
            case RangeValues.PERCENTAGE -> hist.get_pct(rangeValues.getLowerValue(), false);
            case RangeValues.SIGMA -> hist.get_sigma(rangeValues.getLowerValue(), false);
            case RangeValues.ZSCALE -> getZscaleValue(float1d, naxis1, naxis2, blank_value, rangeValues).z1();
            default -> 0;
        };
    }
}
