/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: Jul 21, 2008
 *
 * @author loi
 * @version $Id: UserCache.java,v 1.5 2009/03/23 23:55:16 loi Exp $
 */
public class UserCache implements Cache {

    private Cache cache;
    private StringKey userKey;

    public static final Cache getInstance(){
        return new UserCache();
    }

    private UserCache() {
        userKey = new StringKey(ServerContext.getRequestOwner().getUserKey());
        cache = CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
    }

    public StringKey getUserKey() {
        return userKey;
    }

    public void put(CacheKey key, Object value) {
        Map<CacheKey, Object> map = getSessionMap();
        map.put(key, value);
        cache.put(userKey, map);
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
        throw new UnsupportedOperationException(
                "This cache is used to store session related information.  This operation is not supported.");
    }

    public Object get(CacheKey key) {
        return getSessionMap().get(key);
    }

    public boolean isCached(CacheKey key) {
        return getSessionMap().containsKey(key);
    }

    public List<String> getKeys() {
        Set<CacheKey> keys = getSessionMap().keySet();
        ArrayList<String> list = new ArrayList<String>(keys.size());
        for(CacheKey ck : keys) {
            list.add(ck.getUniqueString());
        }
        return list;
    }

    public int getSize() {
        return getSessionMap().size();
    }

    public static boolean exists(StringKey userKey) {
        Cache cache = CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
        return cache.isCached(userKey);
    }

    public static void create(StringKey userKey) {
        Cache cache = CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
        cache.put(userKey, null);
    }
//====================================================================
//
//====================================================================

    private Map<CacheKey, Object> getSessionMap() {
        Map<CacheKey, Object> map = (Map<CacheKey, Object>) cache.get(userKey);
        if (map == null) {
            map = new ConcurrentHashMap<CacheKey, Object>(100);
        }
        return map;
    }
}
