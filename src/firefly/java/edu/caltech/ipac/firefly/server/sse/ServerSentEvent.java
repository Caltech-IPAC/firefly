/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/18/14
 * Time: 2:35 PM
 */


import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.event.Name;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class ServerSentEvent implements Serializable {

//    private static final long DEFAULT_EXPIRE_OFFSET= 60*60*1000; // 1 hour
    private static final long DEFAULT_EXPIRE_OFFSET= 10*1000; // 10 sec
    private Name name;
    private EventTarget evTarget;
    private EventData evData;
    private long expires;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ServerSentEvent(Name name, EventTarget evTarget, EventData evData) {
        this(name,evTarget,evData,System.currentTimeMillis()+DEFAULT_EXPIRE_OFFSET);
    }


    public ServerSentEvent(Name name, EventTarget evTarget, EventData evData, long expires) {
        this.name = name;
        this.evTarget = evTarget;
        this.evData = evData;
        this.expires = expires;
    }

    public Name getName() {
        return name;
    }

    public EventTarget getEvTarget() {
        return evTarget;
    }

    public EventData getEvData() {
        return evData;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis()>expires);
    }

    public String getSerializedClientString() {
        return name.getName() + Constants.SSE_SPLIT_TOKEN+ evData.getData().toString();
    }
}

