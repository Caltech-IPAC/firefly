/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 1:06 PM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import net.zschech.gwt.comet.server.CometServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ServerEventManager {

    private static final boolean USE_CACHE_EV_CONTAINER= true;
    private static final EventsContainer eventsContainer= USE_CACHE_EV_CONTAINER ? new CacheEventsContainer():
                                                                                   new SimpleEventsContainer();
    private static final StringKey EV_TARGETS_KEY= new StringKey("ADDITIONAL_EVENT_TARGETS");

    private static final List<ServerSentEventQueue> evQueueList= new ArrayList<ServerSentEventQueue>(500);
    private static final EventSenderThread evSenderThread = new EventSenderThread();


    public static void fireEvent(ServerSentEvent sev) {
        eventsContainer.add(sev);
    }

    public static synchronized ServerSentEventQueue addEventQueueForClient(CometServletResponse cometResponse,
                                                                           String userKey,
                                                                           EventMatchCriteria criteria) {
        ServerSentEventQueue retval;
        Logger.briefInfo("create new Queue for: "+ criteria );
        retval= new ServerSentEventQueue(cometResponse,userKey, criteria);

        Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION );
        List <EventTarget> list= (List)cache.get(EV_TARGETS_KEY);
        if (list!=null) {
            for(EventTarget t : list) {
                criteria.addMatchTarget(t);
            }
        }
        evQueueList.add(retval);
        return retval;
    }


    public static synchronized ServerSentEventQueue addEventQueueForClient(ServerSentEventQueue queue) {
        Logger.briefInfo("create new Queue for: "+ queue.getCriteria() );
        evQueueList.add(queue);
        return queue;
    }




    public static synchronized List<ServerSentEventQueue> getEvQueueList() {
        return Collections.unmodifiableList(new ArrayList<ServerSentEventQueue>(evQueueList));
    }

    public static void addSessionExtraEventTarget(EventTarget tgt) {
        Cache cache= CacheManager.getCache(Cache.TYPE_HTTP_SESSION );
        List <EventTarget> list= (List)cache.get(EV_TARGETS_KEY);
        if (list==null) list= new ArrayList<EventTarget>(5);
        list.add(tgt);
        cache.put(EV_TARGETS_KEY,list);
        String sID= ServerContext.getRequestOwner().getUserKey();
        List<ServerSentEventQueue> qList= ServerEventManager.getEvQueueList();
        for(ServerSentEventQueue q : qList) {
            if (q.getUserKey().equals(sID)) {
                q.getCriteria().addMatchTarget(tgt);
            }
        }
    }


    static void queueEventForFiringToClient(ServerSentEvent ev) {
        List<ServerSentEventQueue> list;
System.out.print(">>>> incoming event for que: " + evQueueList.hashCode());
        synchronized (evQueueList) {
System.out.println("  : " + ev.getSerializedClientString());
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

    public static synchronized void removeEventQueue(ServerSentEventQueue queue) {
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
                                queue.sendEventToDestination(message);
                            }
                            if (queue.getLastSentTime()+ONE_MINUTE < System.currentTimeMillis()) {
                                String message= Name.HEART_BEAT.getName();
                                Logger.briefInfo("Sending: heartbeat");
                                queue.sendEventToDestination(message);
                            }
                        } catch (IOException e) {
                            removeEventQueue(queue);
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



    // OLD CODE SAVED FOR REFERENCE
    // OLD CODE SAVED FOR REFERENCE
    // OLD CODE SAVED FOR REFERENCE
//    public static synchronized ServerSentEventQueue addEventQueueForClient(CometServletResponse cometResponse,
//                                                                           String userKey,
//                                                                           EventMatchCriteria criteria) {
//        ServerSentEventQueue retval;
//        List<ServerSentEventQueue> delList= new ArrayList<ServerSentEventQueue>(100);
////        try {
////            for(ServerSentEventQueue queue : evQueueList) {
////                if (!queue.getCometSession().isValid()) {
////                    Logger.briefInfo("Found existing session by invalid: removing");
////                    delList.add(queue);
////                }
////                else if (queue.getCometResponse().isTerminated()) {
////                    Logger.briefInfo("Found existing session by terminated: removing");
////                    delList.add(queue);
////                }
////                else if (criteria.equals(queue.getCriteria())) {
////                    Logger.briefInfo("Found existing session by match: removing");
////                    delList.add(queue);
////                }
////            }
////
////        } catch (IllegalStateException e) {
////            Logger.briefInfo("session not accessible" );
////        }
////        for(ServerSentEventQueue queue : delList)  evQueueList.remove(queue);
//
//        Logger.briefInfo("create new Queue for: "+ criteria );
//        retval= new ServerSentEventQueue(cometResponse,userKey, criteria);
//        evQueueList.add(retval);
//        return retval;
//    }
//


}

