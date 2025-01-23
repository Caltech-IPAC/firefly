/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.DistribMapCache;
import edu.caltech.ipac.firefly.server.cache.DistributedCache;
import edu.caltech.ipac.firefly.server.cache.LocalMapCache;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;

import java.util.List;

import static edu.caltech.ipac.firefly.server.cache.EhcacheProvider.*;

/**
 * Date: Jul 2, 2008
 *
 * @author loi
 * @version $Id: CacheManager.java,v 1.6 2009/06/23 18:57:17 loi Exp $
 */
public class CacheManager {

    private static final String DEF_PROVIDER = "edu.caltech.ipac.firefly.server.cache.EhcacheProvider";  //  ie... "edu.caltech.ipac.firefly.server.cache.EhcacheProvider";

    private static Cache.Provider cacheProvider;
    private static final Logger.LoggerImpl LOG = Logger.getLogger();


    /**
     * Same as {@code getLocal}.
     * @return A Cache that represents the local cache, backed by both memory and disk overflow.
     */
    public static <T> Cache<T> getCache() {
        return getLocal();
    }

    /**
     * Returns a local cache backed by Ehcache. The cache uses both in-memory storage and overflow to disk.
     * @return A Cache that represents the local cache, backed by both memory and disk overflow.
     */
    public static <T> Cache<T> getLocal() {
        return getCacheProvider().getCache(PERM_SMALL);
    }

    /**
     * A cache specifically designed for image visualization purposes.
     * @return A Cache optimized for storing and accessing image data.
     */
    public static <T> Cache<T> getVisMemCache() {
        return getCacheProvider().getCache(VIS_SHARED_MEM);
    }

    /**
     * Returns a distributed cache, used for sharing cache data across
     * different instances in a distributed environment.
     * @return A Cache instance for distributed environments.
     */
    public static <T> Cache<T> getDistributed() {
        return new DistributedCache<T>();
    }

    /**
     * Returns a local cache specifically designed for storing data during
     * a single session. This cache is for temporary storage
     * of data that is specific to a user's session.
     * @return A Cache instance for session-specific data storage.
     */
    public static <T> Cache<T> getSessionCache() {
        return getLocalMap(ServerContext.getRequestOwner().getRequestAgent().getSessId());
    }

    /**
     * Returns a distributed cache designed specifically for storing user-related information.
     * This cache is intended to live longer than a typical session cache and is backed by a long-lived user key
     * stored in a cookie, which allows user data to persist across multiple sessions.
     * @return A distributed Cache instance for storing user-related information with persistence across sessions.
     */
    public static <T> Cache<T> getUserCache() {
        return UserCache.getInstance();
    }

    /**
     * Returns a local cache for storing data mapped to a unique key.
     * @param mapKey The unique key associated with the cache.
     * @return A Cache instance specifically for the data associated with the provided map key.
     */
    public static <T> Cache<T> getLocalMap(String mapKey) {
        return new LocalMapCache<>(mapKey);
    }

    /**
     * Returns a distributed cache designed for storing data mapped to a unique key.
     * @param mapKey The unique key used to identify and access the data in the distributed cache.
     * @return A Cache instance specifically for the data associated with the provided map key in the distributed environment.
     */
    public static <T> Cache<T> getDistributedMap(String mapKey) {
        return new DistribMapCache<>(mapKey);
    }

//====================================================================
//
//====================================================================

    public static Cache.Provider getCacheProvider() {
        if (cacheProvider == null) {
            cacheProvider = newInstanceOf(DEF_PROVIDER);
        }

        return cacheProvider;
    }

    public static boolean setCacheProvider(String cacheProviderClassName) {
        setCacheProvider(newInstanceOf(cacheProviderClassName));
        return cacheProvider != null;
    }

    public static void setCacheProvider(Cache.Provider cacheProvider) {
        CacheManager.cacheProvider = cacheProvider;
    }

//====================================================================
//  helper functions
//====================================================================

    private static Cache.Provider newInstanceOf(String className) {
        if (className != null && !className.isEmpty()) {
            try {
                Class<Cache.Provider> cc = (Class<Cache.Provider>) Cache.Provider.class.forName(className);
                return cc.newInstance();
            } catch (Exception e) {
                LOG.error(e, "Can't create Cache.Provider:" + className + "\nThis could be a configuration error.");
            }
        }
        return null;
    }

//====================================================================
//  inner classes
//====================================================================
    /**
     * An empty implementation of Cache.  This is a simple method to
     * disable caching by providing a permanently empty cache.
     */
    public static class EmptyCache implements Cache {

        public void put(CacheKey key, Object value) {}

        public void put(CacheKey key, Object value, int lifespanInSecs) {}

        public Object get(CacheKey key) {
            return null;
        }

        public void remove(CacheKey key) {}

        public boolean isCached(CacheKey key) {
            return false;
        }

        public int getSize() {
            return 0;
        }

        public List<String> getKeys() {
            return null;
        }
    }
}
