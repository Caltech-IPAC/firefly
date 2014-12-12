package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.client.net.NetParams;

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
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
