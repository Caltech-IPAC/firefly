package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.download.NetParams;

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

