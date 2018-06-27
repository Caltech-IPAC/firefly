/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;
/**
 * User: roby
 * Date: 7/13/18
 * Time: 11:53 AM
 */


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.Zscale;

import java.util.ArrayList;

/**
 * @author Trey Roby
 */
public class ImageStretch {



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
     * @param startPixel
     * @param lastPixel
     * @param startLine
     * @param lastLine
     * @param blank_pixel_value
     */
    public static void stretchPixels(int startPixel,
                                     int lastPixel,
                                     int startLine,
                                     int lastLine,
                                     int naxis1,
                                     ImageHeader imageHeader,
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
            stretchPixelsUsingAsin( startPixel, lastPixel,startLine,lastLine, naxis1, imageHeader,
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

    public static void stretchPixelsUsingOtherAlgorithms(int startPixel,
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

        double gamma=rangeValues.getGammaValue();
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
                        pixeldata[pixelCount] = getLinearStrectchedPixelValue(dRunval);

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

    private static double[] getMinMaxData(float[] float1d){
        double min=Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i=0; i<float1d.length; i++){
            if (float1d[i]<min){
                min=float1d[i];
            }
            if (float1d[i]>max){
                max = float1d[i];
            }
        }
        double[] ret = {min, max};
        return ret;
    }

    /**
     * The asinh stretch algorithm is defined in the paper by Robert Lupton et al at "The Astronomical Journal 118: 1406-1410, 1999 Sept"
     * In the paper:
     *    magnitude = m_0-2.5log(b) - a* asinh (x/2b)  = mu_0  -a * asinh (x/2b), where mu_0 =m_0 - 2.5 * ln(b), a =2.5ln(e) = 1.08574,
     *    m_o=2.5log(flux_0), flux_0 is the flux of an object with magnitude 0.0.
     *    b is an arbitrary "softening" which determines the flux level as which the liner behavior is set in, and x is the flux,
     *
     *
     *   Since  mu_0 and a are constant, we can use just:
     *     mu =  asinh( flux/(2.0*b)) ) = asin(x/beta); where beta=2*b;
     * @param beta
     * @return
     */
    private static double  getASinhStretchedPixelValue(double flux, double maxFlux, double minFlux, double beta)  {
        if (flux <= minFlux) { return 0d; }
        if (flux >= maxFlux) { return 254d; }

        /*
         Since the data range is from minFlux to maxFlux, we can shift the data to  the range [0 - (maxFlux-minFlux)].
         Thus,
                   flux_new = flux-minFlux,
                   minFlux_new = 0
                   maxFlux_new = maxFlux - minFlux
         min = asinh( abs(minFlux_new) -square(minFlux_new*minFlux_new+1) ) =0
         max = (max-min) = asinh( maxFlux_new/beta) = asinh( (maxFlux-minFlux)/beta)
         diff = max - min = max
         */
        double asinhMagnitude =  asinh( (flux-minFlux) / beta); //beta = 2*b

        //normalize to 0 - 255:  (nCorlor-1 )*(x - Min)/(Max - Min), 8 bit nCorlor=256
        //this formula is referred from IDL function: BYTSCL
        double diff =   asinh ( (maxFlux-minFlux)/beta );
        return  255* asinhMagnitude/ diff ;

    }

    private static double asinh(double x) {

        double y  = Math.log( Math.abs(x ) + Math.sqrt(x * x + 1));
        y = x<0? -y:y;

        return y;

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

    private static byte getLinearStrectchedPixelValue(double dRenVal) {

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

    private static void stretchPixelsUsingAsin(int startPixel,
                                                 int lastPixel,
                                                 int startLine,
                                                 int lastLine,
                                                 int naxis1,
                                                 ImageHeader imageHeader,
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

          double maxFlux = getFlux(shigh, imageHeader);
          double minFlux = getFlux(slow, imageHeader);
          if (Double.isNaN(minFlux) || Double.isInfinite((minFlux))){
              double[] minMax=getMinMaxData(float1dArray);
              minFlux = getFlux(minMax[0],imageHeader);
          }

          if ( Double.isNaN(maxFlux) || Double.isInfinite((maxFlux)) ) {
              double[] minMax=getMinMaxData(float1dArray);
              minFlux = getFlux(minMax[1], imageHeader);
          }
          int pixelCount = 0;
          double flux;

          for (int line = startLine; line <= lastLine; line++) {
              int start_index = line * naxis1 + startPixel;
              int last_index = line * naxis1 + lastPixel;
              for (int index = start_index; index <= last_index; index++) {
                  flux = getFlux(float1dArray[index], imageHeader);
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

        stretchPixels(start_pixel, last_pixel,
                start_line, last_line, naxis1,imageHeader, hist,
                blank_pixel_value, hist_bin_values,
                pixeldata,  rangeValues,slow,shigh);

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

    private static Zscale.ZscaleRetval getZscaleValue(float[] float1d, ImageHeader imageHeader, RangeValues rangeValues) {

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

    /**
     * This sigma value is calculated using the whole image data.
     * sigma = SQRT [  ( sum (xi-x_average)^2 )/n
     *   where x_average is the mean value of the x array
     *   n is the total number of the element of x array
     * @return
     */
    public static double computeSigma(float[] float1d, ImageHeader imageHeader) {

        //get none zero and finite flux values
        double [] validData = getNoneZeroValidReadoutArray(float1d, imageHeader);
        /*
         When the index.length>25, the IDL atv uses sky to computer sigma. However the sky.pro uses many other
         numerical receipt methods such as value_local, fitting etc. Here we uses stddev instead.
       */
        if (validData.length>5 ){
            return getStdDev( validData);
        }
        else {
            return  1.0;

        }
    }

    /**
     * Process the image fluxes to exclude the 0.0 and NaN and infinity values
     * @return
     */
    private static double[] getNoneZeroValidReadoutArray(float[] float1d, ImageHeader imageHeader){
        ArrayList<Double> list= new ArrayList<>();

        for (int i=0; i<float1d.length; i++){
            if (!Double.isNaN(float1d[i]) && !Double.isInfinite(float1d[i]) && float1d[1]!=0.0){
                list.add( getFlux(float1d[i], imageHeader ) );
            }
        }
        double[] arr = new double[list.size()];
        for(int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /**
     * Calculate variance and then standard deviation
     * @param data
     * @return
     */
    private static double getStdDev(double[] data) {

        int size = data.length;
        double mean = getMean(data);
        double temp = 0.0f;
        for(double a :data)
            temp += (mean - a) * (mean - a);

        return Math.sqrt(temp/size);
    }

    /**
     * Calculate the mean flux value
     * @param data
     * @return
     */
    private static double getMean(double [] data ) {

        int size = data.length;
        float sum = 0.0f;
        for(double a : data)
            sum += a;
        return sum/size;
    }

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
