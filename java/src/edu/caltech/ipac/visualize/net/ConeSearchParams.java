package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.NetParams;

import java.util.Locale;


public class ConeSearchParams implements NetParams {
    private double _ra;
    private double _dec;
    private double _radius;


    public ConeSearchParams(double ra, double dec) {
        this(ra, dec, 0.05);
    }

    public ConeSearchParams(double ra, double dec, double radius) {
        _ra = ra;
        _dec = dec;
        _radius = radius;
    }


    public double  getRaJ2000()     { return _ra; }
    public double  getDecJ2000()    { return _dec; }
    public double  getRadius() {return _radius; }


    public String  getRaJ2000String()     {
        return String.format(Locale.US,"%8.6f",_ra);
    }
    public String  getDecJ2000String()    {
        return String.format(Locale.US,"%8.6f",_dec);
    }

    public String toString() {
        return String.format(Locale.US,"RA=%.6f&DEC=%.6f&SR=%.3f",_ra,_dec,_radius);
    }

    public String getUniqueString() {
        return "ConeSearchParams-"+toString();
    }
}
