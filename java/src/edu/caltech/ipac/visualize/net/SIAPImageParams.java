/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.NetParams;

import java.util.Locale;


public class SIAPImageParams implements NetParams {
    private double _ra;
    private double _dec;
    private double _size;


    public SIAPImageParams(double ra, double dec) {
        this(ra, dec, 0.05);
    }

    public SIAPImageParams(double ra, double dec, double size) {
        _ra = ra;
        _dec = dec;
        _size = size;
    }


    public double  getRaJ2000()     { return _ra; }
    public double  getDecJ2000()    { return _dec; }
    public double  getWidth() {return _size; }


    public String  getRaJ2000String()     {
        return String.format(Locale.US,"%8.6f",_ra);
    }
    public String  getDecJ2000String()    {
        return String.format(Locale.US,"%8.6f",_dec);
    }

    public String getSIAPObjectString() {
        return String.format(Locale.US,"POS=%8.6fd,%8.6fd",_ra,_dec);
    }

    public String toString() {
        return String.format(Locale.US,"POS=%.6f,%.6f&SIZE=%.3f",_ra,_dec,_size);
    }

    public String getUniqueString() {
        return "SIAPImageParams-"+toString();
    }
}
