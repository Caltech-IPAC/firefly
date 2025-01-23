/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
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
public class LocalMapCache<T> implements Cache<T> {

    private final Cache<Map<CacheKey, T>> cache;
    private final StringKey uniqueKey;

    public LocalMapCache(String key) {
        uniqueKey = new StringKey(key);
        cache = CacheManager.getLocal();
    }

    public StringKey getUserKey() {
        return uniqueKey;
    }

    public void put(CacheKey key, T value) {
        if (key == null) {
            throw  new NullPointerException("key must not be null.");
        }
        var map = getMappedData();
        map.put(key, value);
        cache.put(uniqueKey, map);
    }

    public void remove(CacheKey key) {
        if (key == null) {
            throw  new NullPointerException("key must not be null.");
        }
        var map = getMappedData();
        map.remove(key);
        cache.put(uniqueKey, map);
    }

    public void put(CacheKey key, T value, int lifespanInSecs) {
        throw new UnsupportedOperationException(
                "This cache is used to store session related information.  This operation is not supported.");
    }

    public T get(CacheKey key) {
        return key == null ? null : getMappedData().get(key);
    }

    public boolean isCached(CacheKey key) {
        return key != null && getMappedData().containsKey(key);
    }

    public List<CacheKey> getKeys() {
        Set<CacheKey> keys = getMappedData().keySet();
        return new ArrayList<>(keys);
    }

    public int getSize() {
        return getMappedData().size();
    }

//====================================================================
//
//====================================================================

    private Map<CacheKey, T> getMappedData() {
        Map<CacheKey, T> map = cache.get(uniqueKey);
        if (map == null) {
            map = new ConcurrentHashMap<>(100);
        }
        return map;
    }
}
