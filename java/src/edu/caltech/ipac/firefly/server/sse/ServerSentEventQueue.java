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
