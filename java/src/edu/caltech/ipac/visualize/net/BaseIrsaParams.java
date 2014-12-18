package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.BaseNetParams;

import java.util.Locale;


public abstract class BaseIrsaParams extends BaseNetParams {
    private double _ra;
    private double _dec;


    public BaseIrsaParams() { }

    public void setRaJ2000(double ra)     { _ra=  ra;}
    public void setDecJ2000(double dec)   { _dec= dec;}

    public double  getRaJ2000()     { return _ra; }
    public double  getDecJ2000()    { return _dec;}

    public String  getRaJ2000String()     {
        return String.format(Locale.US,"%8.6f",_ra);
    }
    public String  getDecJ2000String()    {
        return String.format(Locale.US,"%8.6f",_dec);
    }

    public String getIrsaObjectString() {
        return String.format(Locale.US,"%8.6fd %8.6fd eq j2000",_ra,_dec);
    }

    public String toString() {
        return String.format(Locale.US,"%8.6f%8.6f",_ra,_dec);
    }
}
