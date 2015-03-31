/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 4:04 PM
 */


import net.zschech.gwt.comet.server.CometServletResponse;

import java.io.IOException;
import java.util.LinkedList;

/**
 * @author Trey Roby
 */
public class ServerSentEventQueue {
    private final LinkedList<ServerSentEvent> evQueue = new LinkedList<ServerSentEvent>();

    private final CometServletResponse cometResponse;
    private final Sender sender;
    private final EventMatchCriteria criteria;
    private final String userKey;
    private long lastSentTime= System.currentTimeMillis();


    public ServerSentEventQueue(String userKey, EventMatchCriteria criteria,Sender sender) {
        this.cometResponse = null;
        this.criteria= criteria;
        this.userKey= userKey;
        this.sender= sender;
    }


    public ServerSentEventQueue(CometServletResponse cometResponse,
                                String userKey,
                                EventMatchCriteria criteria) {
        this.cometResponse = cometResponse;
        this.criteria= criteria;
        this.userKey= userKey;
        this.sender= null;
    }

    long getLastSentTime() { return lastSentTime; }
    public void setLastSentTime(long lastSentTime) { this.lastSentTime = lastSentTime; }

    public boolean isEmpty() { return evQueue.isEmpty(); }



    public EventMatchCriteria getCriteria() { return criteria; }

    synchronized ServerSentEvent getEvent() {
        ServerSentEvent retval= null;
        if (!evQueue.isEmpty()) retval= evQueue.pollFirst();
        if (retval!=null && retval.isExpired()) retval= null;
        return retval;
    }

    public String getUserKey() {
        return userKey;
    }

    public synchronized void putEvent(ServerSentEvent ev) {
        if (criteria.matches(ev.getEvTarget())) {
            evQueue.add(ev);
        }
    }

    void sendEventToDestination(String message) throws IOException {
        if (cometResponse!=null) {
            cometResponse.write(message);
            setLastSentTime(System.currentTimeMillis());
        }
        else if (sender!=null){
           sender.send(message);
        }
    }

    public static interface Sender {
        public void send(String message);
    }
}

