/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.*;

import java.util.Arrays;

/**
 * @author Trey Roby
 */
public class ImageStretch {

    private static boolean isHuePreserving(RangeValues rv) {
        return rv.rgbPreserveHue();
    }


    public static void stretchPixels8Bit(RangeValues rangeValues,
                                         float[] float1d,
                                         byte[] pixelData,
                                         ImageHeader imageHeader,
                                         Histogram hist,
                                         int startPixel,
                                         int lastPixel,
                                         int startLine,
                                         int lastLine ) {
        double slow = ImageStretch.getSlow(rangeValues, float1d, imageHeader, hist);
        double shigh = ImageStretch.getShigh(rangeValues, float1d, imageHeader, hist);
        stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine, imageHeader.naxis1, hist,
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
                    rgbIntensity, (byte)0, float1dAry, pixelDataAry, rangeValuesAry);
        }
        else {
            for(int i=0; (i<float1dAry.length); i++) {
                if (float1dAry[i]!=null) {
                    double slow = ImageStretch.getSlow(rangeValuesAry[i], float1dAry[i], imageHeaderAry[i], histAry[i]);
                    double shigh = ImageStretch.getShigh(rangeValuesAry[i], float1dAry[i], imageHeaderAry[i], histAry[i]);
                    stretchPixelsByBand(startPixel, lastPixel, startLine, lastLine,imageHeaderAry[i].naxis1, histAry[i],
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
                                                  byte blank_pixel_value,
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
        // use zscale to compute lower value for each band and upper value for intensity
        RangeValues rv = rangeValuesAry[0];

        double blankPxValAry[] = new double[3];
        double [] slowAry = new double[3];
        for(int i=0; i<3; i++) {
            blankPxValAry[i]= imageHeaderAry[i].blank_value;
            slowAry[i] = getSlow(rv, float1dAry[i], imageHeaderAry[i], histAry[i]);
            slowAry[i] = getFlux(slowAry[i], imageHeaderAry[i]);
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
                    intensity[index] = RGBIntensity.computeIntensity(index, float1dAry, imageHeaderAry, slowAry);
                }
                pixelCount++;
            }
        }

        // stretch an array of intensities
        byte[] pixelData = new byte[pixelCount];

        boolean useZ = rangeValuesAry[0].getLowerWhich()==RangeValues.ZSCALE;
        double slow = useZ ? rgbIntensity.getIntensityLow() : rgbIntensity.getIntensityDataLow(); // lower range for intensity
        double stretch = useZ ? rgbIntensity.getIntensityHigh()-rgbIntensity.getIntensityLow() : rv.getGammaOrStretch();
        double shigh = slow + stretch; // upper range for intensity

        stretchPixelsUsingAsinh( startPixel, lastPixel,startLine,lastLine, naxis1,
                rgbIntensity.getIntensityDataLow(), rgbIntensity.getIntensityDataHigh(),
                blank_pixel_value, intensity, pixelData, rangeValuesAry[0], slow, shigh);
        for (RangeValues anRV : rangeValuesAry) {
            anRV.setAsinhQValue(rv.getAsinhQValue());
            if (useZ) anRV.setGammaOrStretch(stretch);
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
                    flux = getFlux(float1dAry[c][index], imageHeaderAry[c])-slowAry[c];
                    if (flux < 0) {
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
    public static void stretchPixelsByBand(int startPixel,
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
     *
     * @param raw_dn
     * @return
     */
    public static double getFlux(double  raw_dn, ImageHeader imageHeader){
        if ((raw_dn == imageHeader.blank_value) || (Double.isNaN(raw_dn))) {
            //throw new PixelValueException("No flux available");
            return Double.NaN;

        }

        if (imageHeader.origin.startsWith("Palomar Transient Factory")) {
            return  -2.5 * .43429 * Math.log(raw_dn / imageHeader.exptime) +
                    imageHeader.imagezpt +
                    imageHeader.extinct * imageHeader.airmass;
            /* .43429 changes from natural log to common log */
        } else {
            return raw_dn * imageHeader.bscale + imageHeader.bzero;
        }

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

        double[] dtbl = new double[256];
        if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_LOG
                || rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_LOGLOG) {
            dtbl = getLogDtbl(sdiff, slow, rangeValues);
        }
        else if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_EQUAL) {
            dtbl = hist.getTblArray();
        }
        else if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQUARED){
            dtbl = getSquaredDbl(sdiff, slow, rangeValues);
        }
        else if( rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_SQRT) {
            dtbl = getSquaredDbl(sdiff, slow, rangeValues);
        }
        int deltasav = sdiff > 0 ? 64 : -64;

        double gamma=rangeValues.getGammaOrStretch();
        int pixelCount = 0;
        for (int line = startLine; line <= lastLine; line++) {
            int start_index = line * naxis1 + startPixel;
            int last_index = line * naxis1 + lastPixel;

            for (int index = start_index; index <= last_index; index++) {

                if (Double.isNaN(float1dArray[index])) { //original pixel value is NaN, assign it to blank
                    pixeldata[pixelCount] = blank_pixel_value;
                } else {   // stretch each pixel
                    if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_LINEAR) {

                        double dRunval = ((float1dArray[index] - slow) * 254 / sdiff);
                        pixeldata[pixelCount] = getLinearStretchedPixelValue(dRunval);

                    } else if (rangeValues.getStretchAlgorithm() == RangeValues.STRETCH_POWERLAW_GAMMA) {

                        pixeldata[pixelCount] = (byte) getPowerLawGammaStretchedPixelValue(float1dArray[index], gamma, slow, shigh);
                    } else {

                        pixeldata[pixelCount] = (byte) getNoneLinerStretchedPixelValue(float1dArray[index], dtbl, deltasav);
                    }
                    pixeldata[pixelCount] = rangeValues.computeBiasAndContrast(pixeldata[pixelCount]);
                }
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
     * @param flux
     * @param maxFlux
     * @param minFlux
     * @param qvalue
     * @return
     */
    private static double  getASinhStretchedPixelValue(double flux, double maxFlux, double minFlux, double qvalue)  {

        if (flux <= minFlux) { return 0d; }

        double color = 255 * 0.1 * asinh(qvalue*(flux - minFlux) / (maxFlux - minFlux)) / asinh(0.1 * qvalue);

        return (color > 254d) ? 254d : color;

    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1));
    }

    private static int getNoneLinerStretchedPixelValue(double dRunVal, double[] dtbl, int delta) {

        int pixval = 128;

        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;

        delta >>= 1;
        if (dtbl[pixval] < dRunVal)
            pixval += delta;
        else
            pixval -= delta;
        delta >>= 1;
        if (dtbl[pixval] >= dRunVal)
            pixval -= 1;
        return pixval;
    }

    private static byte getLinearStretchedPixelValue(double dRenVal) {

        if (dRenVal < 0)
            return 0;
        else if (dRenVal > 254)
            return (byte) 254;
        else
            return (byte) dRenVal;
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
     * @param minFlux
     * @param maxFlux
     * @param dataMaxFlux
     * @return
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

    private static double getPowerLawGammaStretchedPixelValue(double x, double gamma, double zp, double mp){
        if (x <= zp) { return 0d; }
        if (x >= mp) { return 254d; }
        double  rd =  x-zp;
        double  nsd = Math.pow(rd, 1.0 / gamma)/ Math.pow(mp - zp, 1.0 / gamma);
        double pixValue = 255*nsd;

        return pixValue;

    }

    /**
     * Return an array where each element corresponds to an element of
     * the histogram, and the value in each element is the screen pixel
     * value which would result from an image pixel which falls into that
     * histogram bin.
     *
     * @return array of byte (4096 elements)
     */
    public static byte[] getHistColors(Histogram hist, RangeValues rangeValues, float[] float1d, ImageHeader imageHeader) {

        //calling stretch_pixel to calculate pixeldata, pixelhist
        byte[] pixeldata = new byte[4096];


        float[] hist_bin_values = new float[4096];
        for (int i = 0; i < 4096; i++) {
            hist_bin_values[i] = (float) hist.getDNfromBin(i);
        }

        double slow = getSlow(rangeValues, float1d, imageHeader,hist);
        double shigh = getShigh(rangeValues, float1d, imageHeader, hist);

        int start_pixel = 0;
        int last_pixel = 4095;
        int start_line = 0;
        int last_line = 0;
        int naxis1 = 1;
        byte blank_pixel_value = 0;

        stretchPixelsByBand(start_pixel, last_pixel,
                start_line, last_line, naxis1, hist,
                blank_pixel_value, hist_bin_values,
                pixeldata,  rangeValues, slow, shigh);

        return pixeldata;
    }

    public static double getShigh(RangeValues rangeValues, float[] float1d, ImageHeader imageHeader, Histogram hist) {
        double shigh = 0.0;
        switch (rangeValues.getUpperWhich()) {
            case RangeValues.ABSOLUTE:
                shigh = (rangeValues.getUpperValue() - imageHeader.bzero) / imageHeader.bscale;
                break;
            case RangeValues.PERCENTAGE:
                shigh = hist.get_pct(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.SIGMA:
                shigh = hist.get_sigma(rangeValues.getUpperValue(), true);
                break;
            case RangeValues.ZSCALE:
                Zscale.ZscaleRetval zscale_retval = getZscaleValue(float1d, imageHeader, rangeValues);
                shigh = zscale_retval.getZ2();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getUpperWhich()");
        }
        return shigh;
    }

    public static Zscale.ZscaleRetval getZscaleValue(float[] float1d, ImageHeader imageHeader, RangeValues rangeValues) {

        double contrast = rangeValues.getZscaleContrast();
        int optSize = rangeValues.getZscaleSamples();

        int lenStdline = rangeValues.getZscaleSamplesPerLine();

        Zscale.ZscaleRetval zscaleRetval = Zscale.cdl_zscale(float1d,
                imageHeader.naxis1, imageHeader.naxis2,
                imageHeader.bitpix, contrast / 100.0, optSize, lenStdline,
                imageHeader.blank_value );

        return zscaleRetval;
    }

    public static double getSlow(RangeValues rangeValues, float[] float1d, ImageHeader imageHeader, Histogram hist) {
        double slow = 0.0;
        switch (rangeValues.getLowerWhich()) {
            case RangeValues.ABSOLUTE:
                slow = (rangeValues.getLowerValue() - imageHeader.bzero) /imageHeader.bscale;
                break;
            case RangeValues.PERCENTAGE:
                slow = hist.get_pct(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.SIGMA:
                slow = hist.get_sigma(rangeValues.getLowerValue(), false);
                break;
            case RangeValues.ZSCALE:

                Zscale.ZscaleRetval zscale_retval = getZscaleValue(float1d, imageHeader, rangeValues);
                slow = zscale_retval.getZ1();
                break;
            default:
                Assert.tst(false, "illegal rangeValues.getLowerWhich()");
        }
        return slow;
    }
    
//    /**
//     * This sigma value is calculated using the whole image data.
//     * sigma = SQRT [  ( sum (xi-x_average)^2 )/n
//     *   where x_average is the mean value of the x array
//     *   n is the total number of the element of x array
//     * @return
//     */
//    public static double computeSigma(float[] float1d, ImageHeader imageHeader) {
//
//        //get none zero and finite flux values
//        double [] validData = getNoneZeroValidReadoutArray(float1d, imageHeader);
//        /*
//         When the index.length>25, the IDL atv uses sky to computer sigma. However the sky.pro uses many other
//         numerical receipt methods such as value_local, fitting etc. Here we uses stddev instead.
//       */
//        if (validData.length>5 ){
//            return getStdDev( validData);
//        }
//        else {
//            return  1.0;
//
//        }
//    }
//
//    /**
//     * Process the image fluxes to exclude the 0.0 and NaN and infinity values
//     * @return
//     */
//    private static double[] getNoneZeroValidReadoutArray(float[] float1d, ImageHeader imageHeader){
//        ArrayList<Double> list= new ArrayList<>();
//
//        for (int i=0; i<float1d.length; i++){
//            if (!Double.isNaN(float1d[i]) && !Double.isInfinite(float1d[i]) && float1d[1]!=0.0){
//                list.add( getFlux(float1d[i], imageHeader ) );
//            }
//        }
//        double[] arr = new double[list.size()];
//        for(int i = 0; i < list.size(); i++) {
//            arr[i] = list.get(i);
//        }
//        return arr;
//    }
//
//    /**
//     * Calculate variance and then standard deviation
//     * @param data
//     * @return
//     */
//    private static double getStdDev(double[] data) {
//
//        int size = data.length;
//        double mean = getMean(data);
//        double temp = 0.0f;
//        for(double a :data)
//            temp += (mean - a) * (mean - a);
//
//        return Math.sqrt(temp/size);
//    }
//
//    /**
//     * Calculate the mean flux value
//     * @param data
//     * @return
//     */
//    private static double getMean(double [] data ) {
//
//        int size = data.length;
//        float sum = 0.0f;
//        for(double a : data)
//            sum += a;
//        return sum/size;
//    }

    /**
     * add a new stretch method to do the mask plot
     * @param startPixel
     * @param lastPixel
     * @param startLine
     * @param lastLine
     * @param naxis1
     * @param blank_pixel_value
     * @param float1dArray
     * @param pixeldata
     * @param pixelhist
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
