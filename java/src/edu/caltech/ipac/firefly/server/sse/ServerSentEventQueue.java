package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 4:04 PM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import net.zschech.gwt.comet.server.CometServletResponse;
import net.zschech.gwt.comet.server.CometSession;

import java.io.IOException;
import java.util.LinkedList;

/**
 * @author Trey Roby
 */
public class ServerSentEventQueue implements Runnable {
    private final LinkedList<ServerSentEvent> evQueue = new LinkedList<ServerSentEvent>();

    private final CometServletResponse cometResponse;
    private Thread thread;
    private CometSession cometSession;
    private final EventMatchCriteria criteria;
    private final ServerSentEvent heartbeatEvent;
    private long lastSentTime= System.currentTimeMillis();
    private long ONE_MINUTE= 1000*60;




    public ServerSentEventQueue(CometServletResponse cometResponse, EventMatchCriteria criteria) {

        this.cometResponse = cometResponse;
        this.cometSession= cometResponse.getSession();
        this.criteria= criteria;
        if (criteria.getFirstTarget().equals(EventTarget.ALL)) {
            this.heartbeatEvent= null;
        }
        else {
            this.heartbeatEvent= new ServerSentEvent(Name.HEART_BEAT, criteria.getFirstTarget(), new EventData("ALL"));
        }
        thread= new Thread(this);
        thread.setDaemon(true);
        thread.start();


    }

    CometSession getCometSession() { return cometSession; }
    CometServletResponse getCometResponse() { return cometResponse; }
    EventMatchCriteria getCriteria() { return criteria; }

    private synchronized ServerSentEvent getEvent() {
        ServerSentEvent retval= null;
        try {
            if (evQueue.isEmpty()) wait(5000);
            if (!evQueue.isEmpty()) retval= evQueue.pollFirst();
            if (retval!=null && retval.isExpired()) retval= null;
        } catch (InterruptedException e) {
            retval= null;
        }
        return retval;
    }


    public synchronized void putEvent(ServerSentEvent ev) {
        if (criteria.matches(ev.getEvTarget())) {
            evQueue.add(ev);
            notifyAll();
        }
    }


    public void run() {
        while (thread!=null) {
            ServerSentEvent ev= getEvent();
            if (ev!=null) {
                    String message= "Event: "+ ev.getName() + "=====BEGIN:"+ ev.getEvData().getData().toString();
                    Logger.briefInfo("Sending: " + message);
                    sendEventToClient(message);
                    lastSentTime= System.currentTimeMillis();
            }
            else {
                if (lastSentTime+ONE_MINUTE < System.currentTimeMillis()) {
                    String message= "Event: "+ Name.HEART_BEAT.getName();
                    Logger.briefInfo("Sending: heartbeat");
                    sendEventToClient(message);
                    lastSentTime= System.currentTimeMillis();
                }
            }
        }
        ServerEventManager.removeEventQueue(this);
    }

    private void sendEventToClient(String message) {
        try {
            cometResponse.write(message);
        } catch (IOException e) {
            thread= null;
            Logger.briefInfo("comet send fail: "+e.toString());
        }

    }

    public void shutdown() {
        if (thread!=null) {
            Thread t= thread;
            thread= null;
            t.interrupt();
        }
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
