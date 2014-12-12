package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.client.net.NetParams;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

public class HorizonsIspyParams implements NetParams {

    public final static String SIRTF= "-79";
    public final static SimpleDateFormat _dateFormat=
                                 new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    public final static SimpleDateFormat _dateFormatFile=
                   new SimpleDateFormat("dd-MMM-yyyy-HH-mm-ss");

    private Date   _epochDate;
    private double _raJ2000;
    private double _decJ2000;
    private int    _radiusInArcsec;
    private String _observer;

    public HorizonsIspyParams(Date   epochDate,
                              double raJ2000,
                              double decJ2000,
                              int    radiusInArcsec,
                              String observer) {
        _epochDate=epochDate;
        _raJ2000=raJ2000;
        _decJ2000=decJ2000;
        _radiusInArcsec=radiusInArcsec;
        _observer=observer;
	_dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	_dateFormatFile.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public int    getRadiusInArcsec(){ return _radiusInArcsec; }
    public double getRaJ2000()       { return _raJ2000; }
    public double getDecJ2000()      { return _decJ2000; }
    public Date   getEpochDate()     { return _epochDate; }
    public String getEpochinDateStr(){ return _dateFormat.format(_epochDate); }
    public String getObserver()      { return _observer;}

    public String getUniqueString() {
        return "HORIZONS_ISPY-"+_observer+"-"+
                  _dateFormatFile.format(_epochDate)+
                 "-"+_raJ2000+"-"
                 +_decJ2000+"-"+_radiusInArcsec;
    }

    public String toString() {
        return getUniqueString();
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
