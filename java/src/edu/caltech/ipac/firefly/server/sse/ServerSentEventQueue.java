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
    private final EventMatchCriteria criteria;
    private long lastSentTime= System.currentTimeMillis();




    public ServerSentEventQueue(CometServletResponse cometResponse, EventMatchCriteria criteria) {

        this.cometResponse = cometResponse;
        this.criteria= criteria;
    }

    long getLastSentTime() { return lastSentTime; }
    public void setLastSentTime(long lastSentTime) { this.lastSentTime = lastSentTime; }

    public boolean isEmpty() { return evQueue.isEmpty(); }



    CometServletResponse getCometResponse() { return cometResponse; }
    EventMatchCriteria getCriteria() { return criteria; }

    synchronized ServerSentEvent getEvent() {
        ServerSentEvent retval= null;
        if (!evQueue.isEmpty()) retval= evQueue.pollFirst();
        if (retval!=null && retval.isExpired()) retval= null;
        return retval;
    }


    public synchronized void putEvent(ServerSentEvent ev) {
        if (criteria.matches(ev.getEvTarget())) {
            evQueue.add(ev);
        }
    }

    void sendEventToClient(String message) throws IOException {
        cometResponse.write(message);
        setLastSentTime(System.currentTimeMillis());
    }

}

