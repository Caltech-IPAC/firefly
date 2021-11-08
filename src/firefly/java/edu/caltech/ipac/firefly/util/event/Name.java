/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util.event;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

/**
 * User: roby
 * Date: Dec 20, 2007
 * Time: 2:59:22 PM
 */


/**
 * @author Trey Roby
 */
public class Name implements Serializable {

    public static final Name EVT_CONN_EST =  new Name("EVT_CONN_EST",
                                             "Event connection established.  Along with this event, you can expect connID and channel in the event's data. ie. {connID: val, channel: val}");
    public static final Name REPORT_USER_ACTION     =  new Name("REPORT_USER_ACTION", "report a user response");
    public static final Name ACTION                 =  new Name("FLUX_ACTION", "an action message.");
    public static final Name PING                   = new Name("PING", "keepalive ping");


    private final String _name;
    private final String _desc;

    public Name(String name, String desc) {
        _name= name;
        _desc= desc;
    }

    public Name() {
        this("unknown", "bad name.");
    }

    public String toString() { return "EventName: "+ _name + " - " + _desc;}

    @Override
    public int hashCode() {
        return _name == null ? 0 : _name.hashCode();
    }

    public String getName() {
        return _name;
    }

    public String getDesc() { return _desc; }
    
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof Name) {
            Name en= (Name)other;
            retval= ComparisonUtil.equals(_name, en._name);
        }
        return retval;
    }

    /**
     * Should make Name into a string.
     * @param name
     * @return
     */
    public static Name parse(String name) {
        return new Name(name, "unknown");
    }
}

