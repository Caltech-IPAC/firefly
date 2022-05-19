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

public final class RangeValues implements Cloneable, Serializable {

    public static final int PERCENTAGE = 88;
    //public static final int MAXMIN     = 89;  // obsolete
    public static final int ABSOLUTE   = 90;
    public static final int ZSCALE     = 91;
    public static final int SIGMA      = 92;

    private static final double ASINH_Q =  Double.NaN; // if NaN, Q will be estimated based on range;
    private static final double GAMMA=2.0;

    // used for hue-preserving rgb only
    private static final double ASINH_STRETCH_PARAM = Double.NaN; // if NaN, use zscale
    private static final double SCALING_K = 1;

    public static final int STRETCH_LINEAR= 44;
    public static final int STRETCH_LOG   = 45;
    public static final int STRETCH_LOGLOG= 46;
    public static final int STRETCH_EQUAL = 47;
    public static final int STRETCH_SQUARED = 48;
    public static final int STRETCH_SQRT    = 49;
    public static final int STRETCH_ASINH   = 50;
    public static final int STRETCH_POWERLAW_GAMMA   = 51;

    private static final short RGB_PRESERVE_HUE_DEFAULT = 0; // use 0 for false, 1 for true


    private final int    _lowerWhich;
    private final double _lowerValue;
    private final int    _upperWhich;
    private final double _upperValue;
    private double _asinhQValue;
    private final double _gammaValue;
    private final int    _algorithm;
    private final short  _rgbPreserveHue;
    private double _asinhStretch; // used with hue-preserving rgb only
    private final double _scalingK; /* scaling factor for flux, used with hue-preserving rgb only */
    private final int    _zscale_contrast;
    private final int    _zscale_samples; /* desired number of pixels in sample */
    private final int    _zscale_samples_per_line; /* optimal number of pixels per line */
    private final double _bias;
    private final double _contrast;

    public RangeValues() {
        this( PERCENTAGE, 1.0, PERCENTAGE, 99.0, ASINH_Q, GAMMA, STRETCH_LINEAR, 25, 600, 120, RGB_PRESERVE_HUE_DEFAULT, ASINH_STRETCH_PARAM, SCALING_K);
    }

    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        double asinhQValue,
                        double gammaValue,
                        int    algorithm,
                        int    zscale_contrast,
                        int    zscale_samples,
                        int    zscale_samples_per_line,
                        short  rgbPreserveHue,
                        double asinhStretch,
                        double scalingK) {
        this( lowerWhich, lowerValue, upperWhich, upperValue, asinhQValue, gammaValue, algorithm,
                zscale_contrast, zscale_samples, zscale_samples_per_line, rgbPreserveHue, asinhStretch, scalingK,
                0.5, 1.0);
    }


    public RangeValues( int    lowerWhich,
                        double lowerValue,
                        int    upperWhich,
                        double upperValue,
                        double asinhQValue,
                        double gammaValue,
                        int    algorithm,
                        int    zscale_contrast,
                        int    zscale_samples,
                        int    zscale_samples_per_line,
                        short  rgbPreserveHue,
                        double asinhStretch,
                        double scalingK,
                        double bias,
                        double contrast) {

        _lowerWhich= lowerWhich;
        _lowerValue= lowerValue;
        _upperWhich= upperWhich;
        _upperValue= upperValue;
        _algorithm = (rgbPreserveHue > 0) ? STRETCH_ASINH : algorithm;
        _asinhQValue = asinhQValue;
        _gammaValue=gammaValue;
        _zscale_contrast = zscale_contrast;
        _zscale_samples = zscale_samples;
        _zscale_samples_per_line = zscale_samples_per_line;
        _rgbPreserveHue = rgbPreserveHue;
        _asinhStretch = asinhStretch;
        _scalingK = scalingK;
        _bias = bias;
        _contrast = contrast;
    }

    public double getAsinhQValue() { return _asinhQValue; }
    public void setAsinhQValue(double val) { _asinhQValue = val; }
    public double getGammaValue() { return _gammaValue; }
    public double getAsinhStretch() { return _asinhStretch; }
    public void setAsinhStretch(double val) { _asinhStretch = val; }
    public double getScalingK() { return _scalingK; }

    public int    getLowerWhich() { return _lowerWhich; }
    public double getLowerValue() { return _lowerValue; }

    public int    getUpperWhich() { return _upperWhich; }
    public double getUpperValue() { return _upperValue; }

    public int getZscaleContrast() { return _zscale_contrast; }
    public int getZscaleSamples() { return _zscale_samples; }
    public int getZscaleSamplesPerLine() { return _zscale_samples_per_line; }

    public int getStretchAlgorithm() { return _algorithm; }

    public boolean rgbPreserveHue() { return _rgbPreserveHue != 0; }

    public Object clone() {
        return new RangeValues( _lowerWhich, _lowerValue, _upperWhich,
		_upperValue, _asinhQValue, _gammaValue, _algorithm,
		_zscale_contrast, _zscale_samples, _zscale_samples_per_line, _rgbPreserveHue, _asinhStretch, _scalingK, _bias, _contrast );
    }

    public String toString() { return serialize(); }

    public static RangeValues parse(String sIn) {
        if (isEmpty(sIn)) return null;

        try {
            String[] s= sIn.split(",");
            int i= 0;
            int    lowerWhich=              Integer.parseInt(s[i++]);
            double lowerValue=              parseDouble(s[i++]);
            int    upperWhich=              Integer.parseInt(s[i++]);
            double upperValue=              parseDouble(s[i++]);
            double asinhQValue=             parseDouble(s[i++]);
            double gammaValue=              parseDouble(s[i++]);
            int    algorithm=               Integer.parseInt(s[i++]);
            int    zscale_contrast=         Integer.parseInt(s[i++]);
            int    zscale_samples=          Integer.parseInt(s[i++]);
            int    zscale_samples_per_line= Integer.parseInt(s[i++]);
            short  rgbPreserveHue=          s.length > 10 ? Short.parseShort(s[i++]) : RGB_PRESERVE_HUE_DEFAULT;
            double asinhStretch=            s.length > 10 ? parseDouble(s[i++]) : ASINH_STRETCH_PARAM;
            double scalingK=                s.length > 10 ? parseDouble(s[i]) : SCALING_K;

            return new RangeValues(lowerWhich,
                    lowerValue,
                    upperWhich,
                    upperValue,
                    asinhQValue,
                    gammaValue,
                    algorithm,
                    zscale_contrast,
                    zscale_samples,
                    zscale_samples_per_line,
                    rgbPreserveHue,
                    asinhStretch,
                    scalingK);

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
                getGammaValue()+","+
                getStretchAlgorithm()+","+
                getZscaleContrast()+","+
                getZscaleSamples()+","+
                getZscaleSamplesPerLine()+","+
                _rgbPreserveHue+","+
                getAsinhStretch()+","+
                getScalingK();
    }

    private static boolean isEmpty(String s) { return s == null || s.trim().length() == 0; }

    public static double parseDouble(String s) throws NumberFormatException {
        return s.equals("NaN") ? Double.NaN : Double.parseDouble(s);
    }
}
