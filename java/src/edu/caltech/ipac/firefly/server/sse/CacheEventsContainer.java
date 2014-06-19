package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 6/17/14
 * Time: 12:00 PM
 */


import edu.caltech.ipac.firefly.server.cache.EhcacheImpl;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/**
 * @author Trey Roby
 */
public class CacheEventsContainer implements ServerEventManager.EventsContainer {

    private static final String EVENT_SENDING_CACHE= Cache.TYPE_PERM_SMALL;
    private static final Cache cache= CacheManager.getCache(EVENT_SENDING_CACHE);

    public CacheEventsContainer() {
        initCacheListener();

    }

    public void add(ServerSentEvent sev) {
        if (cache!=null) {
            String key= "EventKey-"+System.currentTimeMillis() + Math.random();
            cache.put(new StringKey(key),sev);
        }
    }


    private void initCacheListener() {
        if (cache!=null) {
            if (cache instanceof EhcacheImpl) {
                Ehcache ehC= ((EhcacheImpl)cache).getEHcache();
                ehC.getCacheEventNotificationService().registerListener(new LoggingEventListener());
            }
        }

    }

    private class LoggingEventListener implements CacheEventListener {
        public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException {
            if (element.getObjectValue() instanceof ServerSentEvent) {
                ServerEventManager.queueEventForFiringToClient((ServerSentEvent) element.getObjectValue());
            }
        }

        public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException {
            if (element.getObjectValue() instanceof ServerSentEvent) {
                ServerEventManager.queueEventForFiringToClient((ServerSentEvent) element.getObjectValue());
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
