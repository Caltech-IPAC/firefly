package edu.caltech.ipac.targetgui.net;

import edu.caltech.ipac.client.net.NetParams;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.target.Ephemeris;
import edu.caltech.ipac.target.StandardEphemeris;
import edu.caltech.ipac.target.NonStandardEphemeris;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

public class HorizonsFileParams implements NetParams {



    public final static String XSP_EXT= "xsp";
    public final static String BSP_EXT= "bsp";
    public final static SimpleDateFormat _dateFormat=
                                 new SimpleDateFormat("dd-MMM-yyyy");

    private final String _id;
    private final Date   _beginDate;
    private final Date   _endDate;
    private final String _fileType;
    private String _epoch;
    private String _t;
    private String _e;
    private String _q;
    private String _i;
    private String _littleOmega;
    private String _bigOmega;
    private boolean _standard= true;




    /**
     * @param ephem     Ephemeris for this request
     * @param beginDate the begin date to build the file for
     * @param endDate   the begin date to build the file for
     * @param fileType  must be xsp (text) or bsp (binary)
     */
    public HorizonsFileParams(Ephemeris ephem,
                              Date      beginDate,
                              Date      endDate,
                              String    fileType) {
        Assert.argTst((fileType.equals(XSP_EXT) || fileType.equals(BSP_EXT)),
                      "fileType must be xsp or bsp");

        if (ephem instanceof StandardEphemeris) {
            _id= ((StandardEphemeris)ephem).getNaifID() + "";
        }
        else if (ephem instanceof NonStandardEphemeris) {
            _standard= false;
            _id= "123"; // dummy value
            NonStandardEphemeris nonSt= (NonStandardEphemeris)ephem;
            _epoch= nonSt.getEpoch();
            _t    = nonSt.getT();
            _e    = nonSt.getE() + "";
            _q    = nonSt.getQ() + "";
            _i    = nonSt.getI() + "";
            _littleOmega = nonSt.getLittleOmega() + "";
            _bigOmega    = nonSt.getBigOmega() + "";
        }
        else {
            _id= "";
            Assert.argTst(false, "Ephemeris must be either a "+
                             "StandardEphemeris or a NonStandardEphemeris");
        }
        _beginDate=beginDate;
        _endDate=endDate;
        _fileType=fileType;
	_dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public String getNaifID() { return _id; }
    public String getFileType() { return _fileType; }
    public Date   getBeginDate() { return _beginDate; }
    public Date   getEndDate() { return _endDate; }
    public String getBeginDateStr() { return _dateFormat.format(_beginDate); }
    public String getEndDateStr()   { return _dateFormat.format(_endDate); }

    public String getEpoch() { return _epoch; }
    public String getT() { return _t; }
    public String getE() { return _e; }
    public String getQ() { return _q; }
    public String getI() { return _i; }
    public String getLittleOmega() { return _littleOmega; }
    public String getBigOmega()    { return _bigOmega; }

    public boolean isStandard() { return _standard; }

    public String getUniqueString() {
        return "HORIZONS_"+_id+"-"+getBeginDateStr()+"-"+
               getEndDateStr()+"."+_fileType;
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
