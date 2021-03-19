/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.util.Assert;
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
     *
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
                              int naxis1, int naxis2, int bitpix, double blank_value) {
        double shigh = 0.0;
        switch (rangeValues.getUpperWhich()) {
            case RangeValues.ABSOLUTE:
                shigh = (rangeValues.getUpperValue() - bzero) / bscale;
                break;
            case RangeValues.PERCENTAGE:
                shigh = hist.get_pct(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.SIGMA:
                shigh = hist.get_sigma(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.ZSCALE:
                Zscale.ZscaleRetval zscale_retval = getZscaleValue(float1d, naxis1, naxis2, bitpix, blank_value, rangeValues);
                shigh = zscale_retval.getZ2();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getUpperWhich()");
        }
        return shigh;
    }

    static Zscale.ZscaleRetval getZscaleValue(float[] float1d, int naxis1, int naxis2, int bitpix, double blank_value, RangeValues rangeValues) {

        double contrast = rangeValues.getZscaleContrast();
        int optSize = rangeValues.getZscaleSamples();

        int lenStdline = rangeValues.getZscaleSamplesPerLine();

        return Zscale.cdl_zscale(float1d, naxis1, naxis2, bitpix, contrast / 100.0, optSize, lenStdline, blank_value );
    }

    static double getSlow(RangeValues rangeValues, float[] float1d, Histogram hist, double bzero, double bscale,
                          int naxis1, int naxis2, int bitpix, double blank_value) {
        double slow = 0.0;
        switch (rangeValues.getLowerWhich()) {
            case RangeValues.ABSOLUTE:
                slow = (rangeValues.getLowerValue() - bzero) /bscale;
                break;
            case RangeValues.PERCENTAGE:
                slow = hist.get_pct(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.SIGMA:
                slow = hist.get_sigma(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.ZSCALE:
                Zscale.ZscaleRetval zscale_retval = getZscaleValue(float1d, naxis1, naxis2, bitpix, blank_value, rangeValues);
                slow = zscale_retval.getZ1();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getLowerWhich()");
        }
        return slow;
    }
}
