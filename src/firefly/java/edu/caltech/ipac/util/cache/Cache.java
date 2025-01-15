/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import java.util.List;

/**
 * Date: Jul 7, 2008
 *
 * @author loi
 * @version $Id: Cache.java,v 1.4 2009/06/23 18:57:17 loi Exp $
 */
public interface Cache {

    void put(CacheKey key, Object value);
    void put(CacheKey key, Object value, int lifespanInSecs);
    Object get(CacheKey key);
    boolean isCached(CacheKey key);
    int getSize();

    /**
     * returns a list of keys in this cache as string.
     * @return
     */
    List<String> getKeys();


    interface Provider {
        Cache getCache(String type);
    }
}
