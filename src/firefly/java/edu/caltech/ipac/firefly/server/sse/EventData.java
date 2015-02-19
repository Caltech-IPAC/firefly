/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 11:48 AM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundStatus;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class EventData implements Serializable {
    enum DataType { BackgroundStatus, URL, }


    private DataType dataType;
    private String serializedData;

    public EventData(BackgroundStatus s) {
        this.dataType = DataType.BackgroundStatus;
        this.serializedData = s.serialize();
    }

    public EventData(String urlString) {
        this.dataType = DataType.URL;
        this.serializedData = urlString;
    }

    public DataType getDataType() { return dataType; }

    public BackgroundStatus getBackgroundStatus() {
        BackgroundStatus s= null;
        if (dataType== DataType.BackgroundStatus) s= BackgroundStatus.parse(serializedData);
        return s;

    }

    public String getURL() {
        return  (dataType==DataType.URL) ? serializedData : null;
    }

    public Object getData() {
        Object retval= null;
        switch (dataType) {
            case BackgroundStatus:
                retval= getBackgroundStatus().serialize();
                break;
            case URL:
                retval= getURL();
                break;
        }
        return retval;
    }


}

