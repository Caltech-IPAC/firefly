/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;


import java.io.Serializable;

/**
 * LZ 6/11/15
 * Modified in order to run the new stretch algorithm STRETCH_ASINH and power law gamma
 *
 * 6/26/15
 * Add ZP and WP codes
 */

public class RangeValues implements Cloneable, Serializable {


//    public static final String PERCENTAGE_STR = "Percent";
//    public static final String ABSOLUTE_STR   = "Absolute";
//    public static final String SIGMA_STR      = "Sigma";

    public static final int PERCENTAGE = 88;
//    public static final int MAXMIN     = 89;  // obsolete
    public static final int ABSOLUTE   = 90;
    public static final int ZSCALE     = 91;
    public static final int SIGMA      = 92;

    private static final double ASINH_Q =  Double.NaN; // if NaN, Q will be estimated based on range;
    private static final double GAMMA=2.0;

//    public static final String LINEAR_STR= "Linear";
//    public static final String LOG_STR= "Log";
//    public static final String LOGLOG_STR= "LogLog";
//    public static final String EQUAL_STR= "Equal";
//    public static final String SQUARED_STR= "Squared";
//    public static final String SQRT_STR= "Sqrt";
//
//    public static final String ASINH_STR= "Asinh";
//    public static final String POWERLAW_GAMMA_STR= "powerlaw_gamma";


    public static final int STRETCH_LINEAR= 44;
    public static final int STRETCH_LOG   = 45;
    public static final int STRETCH_LOGLOG= 46;
    public static final int STRETCH_EQUAL = 47;
    public static final int STRETCH_SQUARED = 48;
    public static final int STRETCH_SQRT    = 49;
    public static final int STRETCH_ASINH   = 50;
    public static final int STRETCH_POWERLAW_GAMMA   = 51;

    private static final short RGB_PRESERVE_HUE_DEFAULT = 0; // use 0 for false, 1 for true


    private int    _lowerWhich;
    private double _lowerValue;
    private int    _upperWhich;
    private double _upperValue;
    private double _asinhQValue;
    private double _gammaOrStretch; // gamma for Power Law Gamma stretch or stretch value for hue preserving RGB
    private int    _algorithm= STRETCH_LINEAR;
    private short  _rgbPreserveHue;
    private int    _zscale_contrast;
    private int    _zscale_samples; /* desired number of pixels in sample */
    private int    _zscale_samples_per_line; /* optimal number of pixels per line */
    private double _bias;
    private double _contrast;

    public RangeValues() {
        this( PERCENTAGE, 1.0, PERCENTAGE, 99.0, ASINH_Q, GAMMA, STRETCH_LINEAR, 25, 600, 120, RGB_PRESERVE_HUE_DEFAULT);
    }

    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        double asinhQValue,
                        double gammaOrStretch,
                        int    algorithm,
                        int    zscale_contrast,
                        int    zscale_samples,
                        int    zscale_samples_per_line,
                        short  rgbPreserveHue) {
        this( lowerWhich, lowerValue, upperWhich, upperValue, asinhQValue, gammaOrStretch, algorithm,
                zscale_contrast, zscale_samples, zscale_samples_per_line, rgbPreserveHue,
                0.5, 1.0);
    }


    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        double asinhQValue,
                        double gammaOrStretch,
                        int    algorithm,
                        int    zscale_contrast,
                        int    zscale_samples,
                        int    zscale_samples_per_line,
                        short  rgbPreserveHue,
                        double bias,
                        double contrast) {

        _lowerWhich= lowerWhich;
        _lowerValue= lowerValue;
        _upperWhich= upperWhich;
        _upperValue= upperValue;
        _algorithm = (rgbPreserveHue > 0) ? STRETCH_ASINH : algorithm;
        _asinhQValue = asinhQValue;
        _gammaOrStretch=gammaOrStretch;
        _zscale_contrast = zscale_contrast;
        _zscale_samples = zscale_samples;
        _zscale_samples_per_line = zscale_samples_per_line;
        _rgbPreserveHue = rgbPreserveHue;
        _bias = bias;
        _contrast = contrast;
    }

    public double getAsinhQValue() { return _asinhQValue; }
    public void setAsinhQValue(double val) { _asinhQValue = val; }
    public double getGammaOrStretch() { return _gammaOrStretch; }
    public void setGammaOrStretch(double val) { _gammaOrStretch = val; }

    public int    getLowerWhich() { return _lowerWhich; }
    public double getLowerValue() { return _lowerValue; }

    public int    getUpperWhich() { return _upperWhich; }
    public double getUpperValue() { return _upperValue; }

    public int getZscaleContrast() { return _zscale_contrast; }
    public int getZscaleSamples() { return _zscale_samples; }
    public int getZscaleSamplesPerLine() { return _zscale_samples_per_line; }

    public int getStretchAlgorithm() { return _algorithm; }

    public boolean rgbPreserveHue() { return _rgbPreserveHue != 0; }

    public byte computeBiasAndContrast(byte data) {
        short value = data>=0?data:(short)(2*(Byte.MAX_VALUE+1)+data);
        short offset = (short)(Byte.MAX_VALUE*(_bias-0.5)*-4);
        short shift = (short)(Byte.MAX_VALUE*(1-_contrast));

        value = (short)( offset+(value*_contrast)+shift );
        if (value>(Byte.MAX_VALUE*2)) value = Byte.MAX_VALUE*2;
        if (value<0) value = 0;

        return (byte) value;
    }

    public Object clone() {
        return new RangeValues( _lowerWhich, _lowerValue, _upperWhich,
		_upperValue, _asinhQValue, _gammaOrStretch, _algorithm,
		_zscale_contrast, _zscale_samples, _zscale_samples_per_line, _rgbPreserveHue, _bias, _contrast );
    }

    public String toString() { return serialize(); }

    public static RangeValues parse(String sIn) {
        if (isEmpty(sIn)) return null;

        try {
            String s[]= sIn.split(",");
            int i= 0;
            int    lowerWhich=              Integer.parseInt(s[i++]);
            double lowerValue=              parseDouble(s[i++]);
            int    upperWhich=              Integer.parseInt(s[i++]);
            double upperValue=              parseDouble(s[i++]);
            double asinhQValue=             parseDouble(s[i++]);
            double gammaOrStretch=              parseDouble(s[i++]);
            int    algorithm=               Integer.parseInt(s[i++]);
            int    zscale_contrast=         Integer.parseInt(s[i++]);
            int    zscale_samples=          Integer.parseInt(s[i++]);
            int    zscale_samples_per_line= Integer.parseInt(s[i++]);
            short  rgbPreserveHue=          s.length > 10 ? Short.parseShort(s[i]) : RGB_PRESERVE_HUE_DEFAULT;


            return new RangeValues(lowerWhich,
                    lowerValue,
                    upperWhich,
                    upperValue,
                    asinhQValue,
                    gammaOrStretch,
                    algorithm,
                    zscale_contrast,
                    zscale_samples,
                    zscale_samples_per_line,
                    rgbPreserveHue);

        } catch (Exception e) {
            return null;
        }

    }


    public String serialize() {

        return getLowerWhich()+","+
                getLowerValue()+","+
                getUpperWhich()+","+
                getUpperValue()+","+
                getAsinhQValue()+","+
                getGammaOrStretch()+","+
                getStretchAlgorithm()+","+
                getZscaleContrast()+","+
                getZscaleSamples()+","+
                getZscaleSamplesPerLine()+","+
                _rgbPreserveHue;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static double parseDouble(String s) throws NumberFormatException {
        return s.equals("NaN") ? Double.NaN : Double.parseDouble(s);
    }
}
