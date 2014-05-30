package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 11:48 AM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundReport;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class EventData implements Serializable {
    enum DataType { BackgroundReport, URL, }


    private DataType dataType;
    private String serializedData;

    public EventData(BackgroundReport r) {
        this.dataType = DataType.BackgroundReport;
        this.serializedData = r.serialize();
    }

    public EventData(String urlString) {
        this.dataType = DataType.URL;
        this.serializedData = urlString;
    }

    public DataType getDataType() { return dataType; }

    public BackgroundReport getBackgroundReport() {
        BackgroundReport r= null;
        if (dataType==DataType.BackgroundReport) r= BackgroundReport.parse(serializedData);
        return r;

    }

    public String getURL() {
        return  (dataType==DataType.URL) ? serializedData : null;
    }

    public Object getData() {
        Object retval= null;
        switch (dataType) {
            case BackgroundReport:
                retval= getBackgroundReport().serialize();
                break;
            case URL:
                retval= getURL();
                break;
        }
        return retval;
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
