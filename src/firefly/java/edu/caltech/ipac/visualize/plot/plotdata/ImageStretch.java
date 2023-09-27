/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;

import nom.tam.fits.Header;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.Arrays;

import static edu.caltech.ipac.visualize.plot.plotdata.ImageStretchUtil.*;

/**
 * @author Trey Roby
 */
public class ImageStretch {

    private static boolean isHuePreserving(RangeValues rv) {
        return rv!=null ? rv.rgbPreserveHue() : false;
    }


    public static void stretchPixels8Bit(RangeValues rangeValues,
                                         float[] float1d,
                                         byte[] pixelData,
                                         ImageHeader iH,
                                         Histogram hist,
                                         int startPixel,
                                         int lastPixel,
                                         int startLine,
                                         int lastLine ) {
        double slow = getSlow(rangeValues, float1d, hist, iH.bzero, iH.bscale, iH.naxis1, iH.naxis2, iH.blank_value);
        double shigh = getShigh(rangeValues, float1d, hist, iH.bzero, iH.bscale, iH.naxis1, iH.naxis2, iH.blank_value);
        stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine, iH.naxis1, hist,
                (byte)255, float1d, pixelData, rangeValues,slow,shigh);
    }

    public static void stretchPixels3Color(RangeValues rangeValuesAry[],
                                           float[][] float1dAry,
                                           byte[][] pixelDataAry,
                                           ImageHeader[] imageHeaderAry,
                                           Histogram[] histAry,
                                           RGBIntensity rgbIntensity,
                                           int startPixel,
                                           int lastPixel,
                                           int startLine,
                                           int lastLine ) {

        if (float1dAry.length!=3 || imageHeaderAry.length!=3 || histAry.length!=3) {
            throw new IllegalArgumentException("float1dAry, imageHeaderAry, histAry must be exactly 3 elements, some can be null ");
        }

        if (isHuePreserving(rangeValuesAry[0])) {
            stretchPixelsHuePreserving(startPixel, lastPixel, startLine, lastLine, imageHeaderAry, histAry,
                    rgbIntensity, float1dAry, pixelDataAry, rangeValuesAry);
        }
        else {
            for(int i=0; (i<float1dAry.length); i++) {
                if (float1dAry[i]!=null) {
                    ImageHeader iH= imageHeaderAry[i];
                    double slow = getSlow(rangeValuesAry[i], float1dAry[i], histAry[i], iH.bzero, iH.bscale, iH.naxis1, iH.naxis2, iH.blank_value);
                    double shigh = getShigh(rangeValuesAry[i], float1dAry[i], histAry[i], iH.bzero, iH.bscale, iH.naxis1, iH.naxis2, iH.blank_value);
                    stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine,iH.naxis1, histAry[i],
                            (byte)0, float1dAry[i], pixelDataAry[i], rangeValuesAry[i],slow,shigh);
                }
                else {
                    Arrays.fill(pixelDataAry[i], (byte)0);
                }
            }
        }

    }

    private static void stretchPixelsHuePreserving(int startPixel,
                                                  int lastPixel,
                                                  int startLine,
                                                  int lastLine,
                                                  ImageHeader[] imageHeaderAry,
                                                  Histogram[] histAry,
                                                  RGBIntensity rgbIntensity,
                                                  float[][]float1dAry, byte[][] pixelDataAry, RangeValues[] rangeValuesAry) {

        for (int i = 0; (i < float1dAry.length); i++) {
            if (float1dAry[i] == null || imageHeaderAry[i] == null || histAry[i] == null) {
                throw new IllegalArgumentException("3 bands are required for hue preserving stretch.");
            }
        }
        if (imageHeaderAry[0].naxis1 != imageHeaderAry[1].naxis1 || imageHeaderAry[1].naxis1 != imageHeaderAry[2].naxis1 ||
                imageHeaderAry[0].naxis2 != imageHeaderAry[1].naxis2 || imageHeaderAry[1].naxis2 != imageHeaderAry[2].naxis2) {
            throw new IllegalArgumentException("naxis1 and naxis2 must match. " +
                    "r: (" + imageHeaderAry[0].naxis1 + "," + imageHeaderAry[0].naxis2 + ") " +
                    "g: (" + imageHeaderAry[1].naxis1 + "," + imageHeaderAry[1].naxis2 + ") " +
                    "b: (" + imageHeaderAry[2].naxis1 + "," + imageHeaderAry[2].naxis2 + ")");
        }

        double blankPxValAry[] = new double[3];
        double [] slowAry = new double[3];
        for(int i=0; i<3; i++) {
            ImageHeader iH= imageHeaderAry[i];
            blankPxValAry[i]= iH.blank_value;
            slowAry[i] = getSlow(rangeValuesAry[i], float1dAry[i], histAry[i], iH.bzero, iH.bscale, iH.naxis1, iH.naxis2, iH.blank_value);
            slowAry[i] = getScaled(slowAry[i], imageHeaderAry[i], rangeValuesAry[i]);
        }

        // recreate an array of intensities (the part that will be used)
        int naxis1 = imageHeaderAry[0].naxis1;
        float [] intensity = new float[float1dAry[0].length];
        Arrays.fill(intensity, Float.NaN);
        int pixelCount = 0;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {
                
                if (float1dAry[0][index] != blankPxValAry[0] && float1dAry[1][index] != blankPxValAry[1] && float1dAry[2][index] != blankPxValAry[1]) {
                    intensity[index] = RGBIntensity.computeIntensity(index, float1dAry, imageHeaderAry, slowAry, rangeValuesAry);
                }
                pixelCount++;
            }
        }

        RangeValues rv = rangeValuesAry[0];

        // stretch an array of intensities
        byte[] pixelData = new byte[pixelCount];

        // should we use z-scale to calculate intensity slow and shigh values
        boolean useZ = rangeValuesAry[0].getLowerWhich()==RangeValues.ZSCALE || !Double.isFinite(rangeValuesAry[0].getAsinhStretch());
        double slow = useZ ? rgbIntensity.getIntensityLow() : rgbIntensity.getIntensityDataLow(); // lower range for intensity
        double stretch = useZ ? rgbIntensity.getIntensityHigh()-rgbIntensity.getIntensityLow() : rv.getAsinhStretch();

        if (!useZ) {
            double intensityRange = rgbIntensity.getIntensityDataHigh()-slow;
            if (stretch > intensityRange && intensityRange > 0) {
                stretch = intensityRange;
            } else if (stretch < 1e-10) {
                stretch = 1e-10;
            }
        }

        double shigh = slow + stretch; // upper range for intensity

        // for three color we use 0 as blank pixel value
        stretchPixelsUsingAsinh( startPixel, lastPixel,startLine,lastLine, naxis1,
                rgbIntensity.getIntensityDataLow(), rgbIntensity.getIntensityDataHigh(),
                (byte)0, intensity, pixelData, rv, slow, shigh);
        for (RangeValues anRV : rangeValuesAry) {
            anRV.setAsinhQValue(rv.getAsinhQValue());
            anRV.setAsinhStretch(stretch);
        }

        // fill pixel data for each band
        pixelCount = 0;
        float [] rgb = new float[3];
        short pixmax = 255;
        float maxv; // max value
        double flux;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {
                maxv = 0;
                for (int c=0; c<3; c++) {
                    flux = getScaled(float1dAry[c][index], imageHeaderAry[c], rangeValuesAry[c])-slowAry[c];
                    if (flux < 0 || Double.isNaN(flux)) {
                        rgb[c] = 0;
                    } else {
                        rgb[c] = (0xFF&pixelData[pixelCount])*((float)flux)/intensity[index];
                        if (rgb[c]>maxv) {
                            maxv = rgb[c];
                        }
                    }
                }
                if (maxv > pixmax) {
                    rgb[0] = pixmax * rgb[0] / maxv;
                    rgb[1] = pixmax * rgb[1] / maxv;
                    rgb[2] = pixmax * rgb[2] / maxv;
                }
                pixelDataAry[0][pixelCount] = (byte)rgb[0];
                pixelDataAry[1][pixelCount] = (byte)rgb[1];
                pixelDataAry[2][pixelCount] = (byte)rgb[2];
                pixelCount++;
            }
        }
    }

    /**
     * A pixel is a cell or small rectangle which stores the information the computer can handle. A discrete pixels make the map.
     * Each pixel store a value which represents the color of the map.
     * Byte image is the pixel having a value in the range of [0, 255].  One byte has 8 bit.  The stretch algorithm is able to convert
     * some invisible pixel value to become recognizable.
     * There are several stretch algorithm: liner, log, log-log etc.  Using those technique to calculate new pixel values.  For example:
     * Suppose you have a certain image in which the values range from 55 to 103. When this map is stretched linearly to output range 0 to
     * 255: the minimum input value 55 is brought to output value 0, and maximum input value 103 is brought to output value 255, and all
     * other values in between change accordingly (using the same formula). As 0 is by default displayed in black, and 255 in white, the
     * contrast will be better when the image is displayed.
     *
     * @param startPixel (tile info) start pixel in each line
     * @param lastPixel (tile info) end pixel in each line
     * @param startLine (tile info) start line
     * @param lastLine (tile info) end line
     * @param blank_pixel_value blank pixel value
     */
    private static void stretchPixelsByBand(int startPixel,
                                           int lastPixel,
                                           int startLine,
                                           int lastLine,
                                           int naxis1,
                                           Histogram hist,
                                           byte blank_pixel_value,
                                           float[] float1dArray,
                                           byte[] pixeldata,
                                           RangeValues rangeValues,
                                           double slow,
                                           double shigh) {



        /*
         * This loop will go through all the pixels and assign them new values based on the
         * stretch algorithm
         */
        if (rangeValues.getStretchAlgorithm()==RangeValues.STRETCH_ASINH) {
            stretchPixelsUsingAsinh( startPixel, lastPixel,startLine,lastLine, naxis1, hist.getDNMin(), hist.getDNMax(),
                    blank_pixel_value, float1dArray, pixeldata, rangeValues,slow,shigh);
        }
        else {
            stretchPixelsUsingOtherAlgorithms(startPixel, lastPixel, startLine, lastLine,naxis1, hist,
                    blank_pixel_value,float1dArray, pixeldata, rangeValues, slow, shigh);
        }
    }



    /**
     * Displayed flux value
     * @param raw_dn raw value
     * @param header image header
     * @return flux value to display
     */
    public static double getFluxPalomar(double  raw_dn, double blank_value, Header header) {
        int bitpix = header.getIntValue("BITPIX");
        if ((bitpix > 0 && raw_dn == blank_value) || (Double.isNaN(raw_dn))) {
            return Double.NaN;
        }
        double exptime = header.getDoubleValue(ImageHeader.EXPTIME, 0.0);
        double imagezpt = header.getDoubleValue(ImageHeader.IMAGEZPT, 0.0);
        double airmass = header.getDoubleValue(ImageHeader.AIRMASS, 0.0);
        double extinct = header.getDoubleValue(ImageHeader.EXTINCT, 0.0);
        return  -2.5 * .43429 * Math.log(raw_dn / exptime) + imagezpt + extinct * airmass;
        /* .43429 changes from natural log to common log */
    }





    public static double getFluxStandard(double  raw_dn, double blank_value, double bscale, double bzero, int bitpix){
        if ((bitpix > 0 && raw_dn == blank_value) || (Double.isNaN(raw_dn))) {
            return Double.NaN;
        }
        return raw_dn * bscale + bzero;
    }

    private static void stretchPixelsUsingOtherAlgorithms(int startPixel,
                                                         int lastPixel,
                                                         int startLine,
                                                         int lastLine,
                                                         int naxis1,
                                                         Histogram hist,
                                                         byte blank_pixel_value,
                                                         float[] float1dArray,
                                                         byte[] pixeldata,
                                                         RangeValues rangeValues,
                                                         double slow,
                                                         double shigh){

        double sdiff = slow == shigh ? 1.0 : shigh - slow;
        double gamma=rangeValues.getGammaValue();
        int pixelCount = 0;

        int stretchAlgorithm= rangeValues.getStretchAlgorithm();
        double[] dtbl= switch (stretchAlgorithm) { // lookup table only used with certain algorithms
            case RangeValues.STRETCH_LOG, RangeValues.STRETCH_LOGLOG ->  getLogDtbl(sdiff, slow, rangeValues);
            case RangeValues.STRETCH_EQUAL ->  hist.getTblArray();
            case RangeValues.STRETCH_SQUARED, RangeValues.STRETCH_SQRT ->  getSquaredDbl(sdiff, slow, rangeValues);
            default -> new double[0];
        };

        interface StretchVal { byte getStretchValue(float val); }
        StretchVal sv= switch (stretchAlgorithm) {
            case RangeValues.STRETCH_LINEAR -> (val) -> getLinearStretchedPixelValue(val,slow,sdiff);
            case RangeValues.STRETCH_POWERLAW_GAMMA -> (val) -> getPowerLawGammaStretchedPixelValue(val, gamma, slow, shigh);
            default -> (val) -> getNoneLinerStretchedPixelValue(val,dtbl,sdiff);
        };
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {
                pixeldata[pixelCount]= Double.isNaN(float1dArray[index]) ?
                        blank_pixel_value : sv.getStretchValue(float1dArray[index]);
                pixelCount++;
            }
        }
    }

    /**
     * The algorithm accepts positive Q, which should be controlled by a slider.
     * The mapping from flux to color value is 255 * 0.1 * asinh(Q*(x-xMin)/(xMax-xMin)) / asinh(0.1*Q)
     * Below xMin, the color will be 0; above xMax, the equation has to be applied and then clipped to 244, (255 is reserved)
     *
     * The parametrization using Q is explained in the footnote on page 3 of https://arxiv.org/pdf/astro-ph/0312483.pdf
     * The algorithm is based on asinh stretch algorithm used in
     *     https://github.com/astropy/astropy/blob/master/astropy/visualization/lupton_rgb.py
     *
     * Lupton’s formulation assumes that xMax is far below the bright features in the image.
     * He wants to see the features above xMax.
     *
     * If we know the brightest data value and upper and lower range values,
     * we can get the default Q from the following equation:
     *    0.1 * asinh(Q*(xDataMax-xMin)/(xMax-xMin)) / asinh(0.1*Q) = 1
     *
     * @param flux pixel value
     * @param maxFlux upper range value
     * @param minFlux lower range value
     * @param qvalue Q parameter for asinh stretch algorithm
     * @return mapped color value from 0 to 244
     */
    private static double  getASinhStretchedPixelValue(double flux, double maxFlux, double minFlux, double qvalue)  {
        if (flux <= minFlux) { return 0d; }
        double color = 255 * 0.1 * asinh(qvalue*(flux - minFlux) / (maxFlux - minFlux)) / asinh(0.1 * qvalue);
        return Math.min(color, 254d);

    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1));
    }

    public static final int[] shiftPosAry= new int[] {64,32,16,8,4,2,1};
    public static final int[] shiftNegAry= new int[] {-64,-32,-16,-8,-4,-2,-1};

    /**
     * find the correct value from the lookup table
     */
    private static byte getNoneLinerStretchedPixelValue(double dRunVal, double[] dtbl, double sdiff) {
        int[] ary= sdiff>0 ? shiftPosAry : shiftNegAry;
        int pixval = 128;
        for(int delta : ary) {
            if (dtbl[pixval] < dRunVal) pixval += delta;
            else pixval -= delta;
        }
        if (dtbl[pixval] >= dRunVal) pixval -= 1;
        return (byte)pixval;
    }

    private static byte getLinearStretchedPixelValue(double val, double slow, double sdiff) {
        var dRenVal= ((val - slow) * 254) / sdiff;
        if (dRenVal < 0) return 0;
        else if (dRenVal > 254) return (byte) 254;
        else return (byte) dRenVal;
    }

    private static double[] getLogDtbl(double sdiff, double slow, RangeValues rangeValues) {

        double[] dtbl = new double[256];
        for (int j = 0; j < 255; ++j) {

            double atbl = Math.pow(10., j / 254.0);
            if (rangeValues.getStretchAlgorithm() ==
                    RangeValues.STRETCH_LOGLOG) {
                atbl = Math.pow(10., (atbl - 1.0) / 9.0);
            }
            dtbl[j] = (atbl - 1.) / 9. * sdiff + slow;


        }
        dtbl[255] = Double.MAX_VALUE;
        return dtbl;
    }

    private static void stretchPixelsUsingAsinh(int startPixel,
                                                int lastPixel,
                                                int startLine,
                                                int lastLine,
                                                int naxis1,
                                                double dnmin,
                                                double dnmax,
                                                byte blank_pixel_value,
                                                float[] float1dArray,
                                                byte[] pixeldata,
                                                RangeValues rangeValues,
                                                double slow,
                                                double shigh){


        double qvalue = rangeValues.getAsinhQValue();

        if (qvalue < 1e-10) {
            qvalue = 0.1;
        } else if (qvalue > 1e10) {
            qvalue = 1e10;
        }

        // using raw_dn for flux
        double maxFlux = shigh;
        double minFlux = slow;
        if (Double.isNaN(minFlux) || Double.isInfinite((minFlux))){
            minFlux = dnmin;
        }

        if ( Double.isNaN(maxFlux) || Double.isInfinite((maxFlux)) ) {
            maxFlux = dnmax;
        }

        if ( !Double.isFinite(qvalue) ) {
            qvalue = getDefaultAsinhQ(minFlux, maxFlux, dnmax);
            rangeValues.setAsinhQValue(qvalue);
        }

        int pixelCount = 0;
        double flux;

        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;
            for (int index = start_index; index <= last_index; index++) {
                flux = float1dArray[index];
                if (Double.isNaN(flux)) { // if original pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {
                    pixeldata[pixelCount] = (byte) getASinhStretchedPixelValue(flux, maxFlux, minFlux, qvalue);
                }
                pixelCount++;
            }
        }
    }

    /**
     * Find default Q from asinh(Q*(xDataMax-xMin)/(xMax-xMin)) = 10 * asinh(0.1*Q)
     * @param minFlux lower range value
     * @param maxFlux upper range value
     * @param dataMaxFlux maximum data value
     * @return Q value, which would allow to use full color range
     */
    private static double getDefaultAsinhQ(double minFlux, double maxFlux, double dataMaxFlux) {
        double bestQ = 0.1;
        double step = 0.1;
        double minDiff = Double.MAX_VALUE;
        double fact = (dataMaxFlux-minFlux)/(maxFlux-minFlux);
        double diff;
        // max default Q is 12 - which corresponds to fact=1000
        // (points 1000 times brighter than maxFlux will saturate)
        for (double q=0.1; q<=12d; q=q+step) {
            diff = Math.abs(asinh(fact*q)-10*asinh(0.1*q));
            if ( diff < minDiff) {
                minDiff = diff;
                bestQ = q;
            }
        }
        return Math.round(10*bestQ)/10.0; // round to 1 decimal digit
    }

    /**
     * fill the 256 element table with values for a squared stretch
     *
     */
    private static double[] getSquaredDbl(double sdiff, double slow,RangeValues rangeValues) {
        double[] dtbl = new double[256];

        for (int j = 0; j < 255; ++j) {
            if ( rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED){
                dtbl[j] = Math.sqrt(sdiff * sdiff / 254 * j) + slow;
            }
            else {
                double dd = Math.sqrt(sdiff) / 254 * j;
                dtbl[j] = dd * dd + slow;
            }
        }
        dtbl[255] = Double.MAX_VALUE;
        return dtbl;
    }

    private static byte getPowerLawGammaStretchedPixelValue(double x, double gamma, double zp, double mp){
        if (x <= zp) { return 0; }
        if (x >= mp) { return (byte)254; }
        double  rd =  x-zp;
        double  nsd = Math.pow(rd, 1.0 / gamma)/ Math.pow(mp - zp, 1.0 / gamma);
        double pixValue = 255*nsd;
        return (byte)pixValue;
    }

    /**
     * Return an array where each element corresponds to an element of
     * the histogram, and the value in each element is the screen pixel
     * value which would result from an image pixel which falls into that
     * histogram bin.
     *
     * @return array of byte (4096 elements)
     */
    public static byte[] getHistColors(Histogram hist, RangeValues rangeValues, float[] float1d,
                                       double bzero, double bscale, int naxis1, int naxis2,
                                       int bitpix, double blank_value) {

        //calling stretch_pixel to calculate pixeldata, pixelhist
        byte[] pixeldata = new byte[4096];


        float[] hist_bin_values = new float[4096];
        for (int i = 0; i < 4096; i++) {
            hist_bin_values[i] = (float) hist.getDNfromBin(i);
        }

        double slow = getSlow(rangeValues, float1d, hist, bzero, bscale, naxis1, naxis2, blank_value);
        double shigh = getShigh(rangeValues, float1d, hist, bzero, bscale, naxis1, naxis2, blank_value);

        int start_pixel = 0;
        int last_pixel = 4095;
        int start_line = 0;
        int last_line = 0;
        byte blank_pixel_value = 0;

        stretchPixelsByBand(start_pixel, last_pixel,
                start_line, last_line, 1, hist,
                blank_pixel_value, hist_bin_values,
                pixeldata,  rangeValues, slow, shigh);

        return pixeldata;
    }
    
    /**
     * add a new stretch method to do the mask plot
     * @param startPixel (tile info) start pixel in each line
     * @param lastPixel (tile info) end pixel in each line
     * @param startLine (tile info) start line
     * @param lastLine (tile info) end line
     * @param naxis1 number of pixels in a line
     * @param blank_pixel_value blank pixel value
     * @param float1dArray array of raw values
     * @param pixeldata array to populate
     */
    public static void stretchPixelsForMask(int startPixel,
                                            int lastPixel,
                                            int startLine,
                                            int lastLine,
                                            int naxis1,
                                            byte blank_pixel_value,
                                            float[] float1dArray,
                                            byte[] pixeldata,
                                            int[] pixelhist,
                                            ImageMask[] lsstMasks) {


        int pixelCount = 0;
        ImageMask combinedMask = ImageMask.combineWithAnd(lsstMasks);  //mask=33, index=0 and 6 are set

        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {

                if (Double.isNaN(float1dArray[index])) { //original pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {
                    /*
                     The IndexColorModel is designed in the way that each pixel[index] contains the color in
                     lsstMasks[index].  In pixel index0, it stores the lsstMasks[0]'s color. Thus, assign
                     pixelData[pixelCount]=index of the lsstMasks, this pixel is going to be plotted using the
                     color stored there.  The color model is indexed.  For 8 bit image, it has 256 maximum colors.
                     For detail, see the indexColorModel defined in ImageData.java.
                     */
                    int maskPixel= (int)float1dArray[index];
                    if (combinedMask.isSet(maskPixel )) {
                        for (int i = 0; i < lsstMasks.length; i++) {
                            if (lsstMasks[i].isSet(maskPixel)) {
                                pixeldata[pixelCount] = (byte) i;
                                break;
                            }
                        }
                    }
                    else {

                        /*
                        The transparent color is stored at pixel[lsstMasks.length].  The pixelData[pixelCount]=(byte) lsstMasks.length,
                        this pixel will be transparent.
                         */
                        pixeldata[pixelCount]= (byte) lsstMasks.length;
                    }

                    pixelhist[pixeldata[pixelCount] & 0xff]++;
                }
                pixelCount++;

            }
        }


    }
}
