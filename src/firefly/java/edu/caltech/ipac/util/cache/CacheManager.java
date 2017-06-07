/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.KeyBasedCache;

import java.util.List;

/**
 * Date: Jul 2, 2008
 *
 * @author loi
 * @version $Id: CacheManager.java,v 1.6 2009/06/23 18:57:17 loi Exp $
 */
public class CacheManager {

    private static final String DEF_TYPE = Cache.TYPE_PERM_SMALL;
    private static final String DEF_PROVIDER = "edu.caltech.ipac.firefly.server.cache.EhcacheProvider";  //  ie... "edu.caltech.ipac.firefly.server.cache.EhcacheProvider";

    private static  Cache.Provider cacheProvider;
    private static boolean isDisabled = false;

    public static Cache getCache() {
        return getCache(DEF_TYPE);
    }

    public static Cache getCache(String type) {
        if (!isDisabled) {
            try {
                return getCacheProvider().getCache(type);
            } catch (Exception e){
                System.err.println("Unable to get Cache type:" + type + " returning EmptyCache.");
            }
        }
        return new EmptyCache();
    }

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

    public static boolean isDisabled() {
        return isDisabled;
    }

    public static void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

    public static Cache getSessionCache() {
        return new KeyBasedCache(ServerContext.getRequestOwner().getRequestAgent().getSessId());
    }

    public static Cache getUserCache() {
        return new KeyBasedCache(ServerContext.getRequestOwner().getUserKey());
    }


    //====================================================================
//  helper functions
//====================================================================
    private static Cache.Provider newInstanceOf(String className) {
        if (className != null && className.length() > 0) {
            try {
                Class<Cache.Provider> cc = (Class<Cache.Provider>) Cache.Provider.class.forName(className);
                return cc.newInstance();
            } catch (Exception e) {
                System.out.println("Can't create Cache.Provider:" + className + "\nThis could be a configuration error.");
                e.printStackTrace();
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
    private static class EmptyCache implements Cache {

        public void put(CacheKey key, Object value) {}

        public void put(CacheKey key, Object value, int lifespanInSecs) {}

        public Object get(CacheKey key) {
            return null;
        }

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
