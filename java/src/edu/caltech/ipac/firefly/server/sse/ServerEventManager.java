package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 1:06 PM
 */


import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import net.zschech.gwt.comet.server.CometServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ServerEventManager {

    private static final boolean USE_CACHE_EV_CONTAINER= false;
    private static final EventsContainer eventsContainer= USE_CACHE_EV_CONTAINER ? new CacheEventsContainer():
                                                                                   new SimpleEventsContainer();

    private static final List<ServerSentEventQueue> evQueueList= new ArrayList<ServerSentEventQueue>(500);
    private static final EventSenderThread evSenderThread = new EventSenderThread();


    public static void fireEvent(ServerSentEvent sev) {
        eventsContainer.add(sev);
    }

    public static synchronized ServerSentEventQueue addEventQueueForClient(CometServletResponse cometResponse, EventMatchCriteria criteria) {
        ServerSentEventQueue retval;
        List<ServerSentEventQueue> delList= new ArrayList<ServerSentEventQueue>(100);
//        try {
//            for(ServerSentEventQueue queue : evQueueList) {
//                if (!queue.getCometSession().isValid()) {
//                    Logger.briefInfo("Found existing session by invalid: removing");
//                    delList.add(queue);
//                }
//                else if (queue.getCometResponse().isTerminated()) {
//                    Logger.briefInfo("Found existing session by terminated: removing");
//                    delList.add(queue);
//                }
//                else if (criteria.equals(queue.getCriteria())) {
//                    Logger.briefInfo("Found existing session by match: removing");
//                    delList.add(queue);
//                }
//            }
//
//        } catch (IllegalStateException e) {
//            Logger.briefInfo("session not accessible" );
//        }
//        for(ServerSentEventQueue queue : delList)  evQueueList.remove(queue);

        Logger.briefInfo("create new Queue for: "+ criteria );
        retval= new ServerSentEventQueue(cometResponse,criteria);
        evQueueList.add(retval);
        return retval;
    }




    static void queueEventForFiringToClient(ServerSentEvent ev) {
        List<ServerSentEventQueue> list;
        synchronized (evQueueList) {
            list= new ArrayList<ServerSentEventQueue>(evQueueList);
        }
        boolean found= false;
        for(ServerSentEventQueue queue : list) {
            if (queue.getCriteria().matches(ev.getEvTarget())) {
                queue.putEvent(ev);
                found= true;
            }
        }
        if (found) evSenderThread.wake();
    }

    private static synchronized void removeEventQueue(ServerSentEventQueue queue) {
        synchronized (ServerEventManager.class) {
            evQueueList.remove(queue);
        }
    }

///==============================================
///==============================================
///==============================================


    private static class EventSenderThread implements Runnable {
        private volatile Thread thread;
        private final long ONE_MINUTE= 1000*60;

        private EventSenderThread() {
            thread= new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            while (thread!=null) {
                synchronized (this) {
                    List<ServerSentEventQueue> list= new ArrayList<ServerSentEventQueue>(evQueueList);
                    for(ServerSentEventQueue queue : list) {
                        try {
                            for(ServerSentEvent ev= queue.getEvent(); (ev!=null); ev=queue.getEvent()) {
//                                String message= "Event: "+ ev.getName() + "=====BEGIN:"+ ev.getEvData().getData().toString();
                                String message= ev.getSerializedClientString();
                                Logger.briefInfo("Sending: " + message);
                                queue.sendEventToClient(message);
                            }
                            if (queue.getLastSentTime()+ONE_MINUTE < System.currentTimeMillis()) {
                                String message= Name.HEART_BEAT.getName();
                                Logger.briefInfo("Sending: heartbeat");
                                queue.sendEventToClient(message);
                            }
                        } catch (IOException e) {
                            removeEventQueue(queue);
//                            Logger.briefInfo("comet send fail, removing queue: "+e.toString());
                            Logger.getLogger().error(e,"comet send fail, removing queue: "+e.toString());
                        }
                    }
                    try {
                        if (isAllEmpty()) wait(5000);
                    } catch (InterruptedException e) {
                        // continue
                    }
                }
            }
        }

        private boolean isAllEmpty() {
            boolean retval= true;
            for(ServerSentEventQueue queue : evQueueList) {
                retval= queue.isEmpty();
                if (!retval) break;
            }
            return retval;
        }

        synchronized void wake() {
            notifyAll();
        }

        void shutdown() {
            thread= null;
            notifyAll();
        }

    }

    public interface EventsContainer {
        public void add(ServerSentEvent sev);
    }

    private static class SimpleEventsContainer implements EventsContainer {
        public void add(ServerSentEvent sev) {
            ServerEventManager.queueEventForFiringToClient(sev);
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
