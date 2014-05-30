package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 1:06 PM
 */


import edu.caltech.ipac.firefly.server.cache.EhcacheImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.zschech.gwt.comet.server.CometServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ServerEventManager {

    private static final String EVENT_SENDING_CACHE= Cache.TYPE_PERM_SMALL;
    private static final Cache cache= CacheManager.getCache(EVENT_SENDING_CACHE);
    private final static List<ServerSentEventQueue> evQueueList= new ArrayList<ServerSentEventQueue>(500);

    static {
        initCacheListener();
    }



    public static synchronized ServerSentEventQueue addEventQueueForClient(CometServletResponse cometResponse, EventMatchCriteria criteria) {
        ServerSentEventQueue retval= null;
        List<ServerSentEventQueue> delList= new ArrayList<ServerSentEventQueue>(100);
        try {
            for(ServerSentEventQueue queue : evQueueList) {
                if (!queue.getCometSession().isValid()) {
                    Logger.briefInfo("Found existing session by invalid: removing" );
                    queue.shutdown();
                    delList.add(queue);
                }
                else if (queue.getCometResponse().isTerminated()) {
                    Logger.briefInfo("Found existing session by terminated: removing" );
                    queue.shutdown();
                    delList.add(queue);
                }
                else if (criteria.equals(queue.getCriteria())) {
                    Logger.briefInfo("Found existing session by match: removing" );
                    queue.shutdown();
                    delList.add(queue);
                }
            }

        } catch (IllegalStateException e) {
            Logger.briefInfo("session not accessible" );
        }
        for(ServerSentEventQueue es : delList) {
            evQueueList.remove(es);
        }
        Logger.briefInfo("create new Queue for: "+ criteria );
        retval= new ServerSentEventQueue(cometResponse,criteria);
        evQueueList.add(retval);
        return retval;
    }

    public static void fireEvent(ServerSentEvent sev) {
        if (cache!=null) {
            String key= "EventKey-"+System.currentTimeMillis() + Math.random();
            cache.put(new StringKey(key),sev);
        }
    }

    public static synchronized void removeEventQueue(ServerSentEventQueue queue) {
        synchronized (ServerEventManager.class) {
            evQueueList.remove(queue);
        }
    }

    private static void initCacheListener() {
        if (cache!=null) {
            if (cache instanceof EhcacheImpl) {
                Ehcache ehC= ((EhcacheImpl)cache).getEHcache();
                ehC.getCacheEventNotificationService().registerListener(new LoggingEventListener());
            }
        }

    }



    private static void notifyEvent(ServerSentEvent ev) {
        List<ServerSentEventQueue> list;
        synchronized (evQueueList) {
            list= new ArrayList<ServerSentEventQueue>(evQueueList);
        }
        for(ServerSentEventQueue queue : list) {
            queue.putEvent(ev);
        }
    }

    static class LoggingEventListener implements CacheEventListener {
        private static final Logger.LoggerImpl logger = Logger.getLogger();


        public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {
            if (element.getObjectValue() instanceof ServerSentEvent) {
                notifyEvent((ServerSentEvent)element.getObjectValue());
            }
        }

        public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {
            if (element.getObjectValue() instanceof ServerSentEvent) {
                notifyEvent((ServerSentEvent)element.getObjectValue());
            }
        }

        public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException { }
        public void notifyElementExpired(Ehcache ehcache, Element element) { }
        public void notifyElementEvicted(Ehcache ehcache, Element element) { }
        public void notifyRemoveAll(Ehcache ehcache) { }
        public void dispose() { }
        public Object clone() throws CloneNotSupportedException { return super.clone(); }

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
