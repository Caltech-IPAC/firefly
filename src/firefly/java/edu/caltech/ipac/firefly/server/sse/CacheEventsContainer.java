/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
import net.sf.ehcache.event.NotificationScope;

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
                ehC.getCacheEventNotificationService().registerListener(new LoggingEventListener(), NotificationScope.ALL);
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

