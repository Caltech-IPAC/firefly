/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.download.NetParams;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HorizonsFileParams implements NetParams {
    public final static String XSP_EXT= "xsp";
    public final static String BSP_EXT= "bsp";
    public final static SimpleDateFormat _dateFormat=
                                 new SimpleDateFormat("dd-MMM-yyyy");

    private final String id;
    private final Date beginDate;
    private final Date endDate;
    private final String fileType;
    private final String epoch;
    private final String t;
    private final String e;
    private final String q;
    private final String i;
    private final String littleOmega;
    private final String bigOmega;
    private final boolean standard;


    public HorizonsFileParams(String epoch, String t, String e, String q, String i, String littleOmega,
                              String bigOmega, Date beginDate, Date endDate, String fileType) {
        this(false,"",epoch,t,e,q,i,littleOmega,bigOmega,beginDate,endDate,fileType);
    }

    public HorizonsFileParams(String    id,
                              Date      beginDate,
                              Date      endDate,
                              String    fileType) {
        this(true,id,"", "","","","","","",beginDate,endDate,fileType);
    }

    private HorizonsFileParams(boolean standard, String id, String epoch, String t, String e, String q, String i, String littleOmega,
                              String bigOmega, Date beginDate, Date endDate, String fileType) {
        Assert.argTst((fileType.equals(XSP_EXT) || fileType.equals(BSP_EXT)),
                "fileType must be xsp or bsp");
        this.standard = standard;
        this.id = id;
        this.epoch = epoch;
        this.t = t;
        this.e = e;
        this.q = q;
        this.i = i;
        this.littleOmega = littleOmega;
        this.bigOmega = bigOmega;
        this.beginDate =beginDate;
        this.endDate =endDate;
        this.fileType =fileType;
        _dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public String getNaifID() { return id; }
    public String getFileType() { return fileType; }
    public Date   getBeginDate() { return beginDate; }
    public Date   getEndDate() { return endDate; }
    public String getBeginDateStr() { return _dateFormat.format(beginDate); }
    public String getEndDateStr()   { return _dateFormat.format(endDate); }

    public String getEpoch() { return epoch; }
    public String getT() { return t; }
    public String getE() { return e; }
    public String getQ() { return q; }
    public String getI() { return i; }
    public String getLittleOmega() { return littleOmega; }
    public String getBigOmega()    { return bigOmega; }

    public boolean isStandard() { return standard; }

    public String getUniqueString() {
        return "HORIZONS_"+ id +"-"+getBeginDateStr()+"-"+
               getEndDateStr()+"."+ fileType;
    }

    public String toString() {
        return getUniqueString();
    }
}

