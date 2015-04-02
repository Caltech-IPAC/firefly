/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;

/**
 * @author tatianag
 * $Id: MinMax.java,v 1.7 2012/10/11 20:47:12 tatianag Exp $
 */
public class MinMax {

    double min;
    double max;
    // sometimes we need not only the range but also a reference value in this range
    double reference;

    public MinMax(double min, double max) {
        this.min = min;
        this.max = max;
        this.reference = (min+max)/2;
    }

    public MinMax(double min, double max, double reference) {
        this(min, max);
        if (isIn(reference)) {
            this.reference = reference;
        } else {
            throw new IllegalArgumentException("Reference is not within min/max range");
        }
    }

    public double getMin() { return this.min; }
    public double getMax() { return this.max; }
    public double getReference() {return this.reference; }
    public boolean isIn(double v) {
        return (v>=min && v<=max);
    }


    public static MinMax getRoundedMinMax(MinMax minMax, int numSigDigits) {
        return getRoundedMinMax(minMax.getMin(), minMax.getMax(), numSigDigits);
    }

    // round to given number of significant digits
    public static MinMax getRoundedMinMax(double min, double max, int numSigDigits) {
        double range = max - min;
        double sdScale = Math.pow(10, numSigDigits);
        double scale = (range == 0d) ? 1 : Math.pow(10, Math.floor(Math.log10(Math.abs(range)))+1);
        double newMax = scale*Math.ceil(max*sdScale/scale)/sdScale;
        double newMin = scale*Math.floor(min*sdScale/scale)/sdScale;
        return new MinMax(newMin, newMax);
    }

    public static String getFormatString(MinMax minMax, int numSigDigits) {
        double min = minMax.getMin();
        double max = minMax.getMax();
        if (min == Double.NEGATIVE_INFINITY) { min = 0; }
        if (max == Double.POSITIVE_INFINITY) { max = 0; }
        double range = max-min;
        if (range == 0) {
            if (min != 0) { range = min; }
            else { return "0"; }
        }
        String format;
        int firstSigDigitPos = (int)Math.floor(Math.log10(Math.abs(range)))+1;
        if (firstSigDigitPos>=numSigDigits) {
            format = "0";
        } else {
            format = "0.";
            int numDecPlaces;
            // find how many places after the decimal
            if (firstSigDigitPos <= 0) {
                numDecPlaces = Math.abs(firstSigDigitPos)+numSigDigits;
            } else {
                numDecPlaces = Math.abs(numSigDigits-firstSigDigitPos);
            }
            for (int n=0; n<numDecPlaces; n++) {
                format += "0";
            }

        }
        return format;
    }

    public static MinMax ensureNonZeroRange(MinMax minMax) {
        double min = minMax.getMin();
        double max = minMax.getMax();
        if (min==max) {
            if (min == 0d ) {
                return new MinMax(-1d,1d);
            } else {
                double range = (Math.abs(min))/2;
                return  new MinMax(min-range, max+range);
            }
        } else {
            return minMax;
        }
    }

}
