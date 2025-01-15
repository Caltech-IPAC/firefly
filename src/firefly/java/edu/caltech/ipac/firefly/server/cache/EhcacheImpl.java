/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.List;

/**
 * This is an implementation of Cache using Ehcache.
 *
 * Date: Jul 17, 2008
 *
 * @author loi
 * @version $Id: EhcacheImpl.java,v 1.8 2009/12/16 21:43:25 loi Exp $
 */
public class EhcacheImpl implements Cache {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    Ehcache cache;

    public EhcacheImpl(Ehcache cache) {
        this.cache = cache;
    }

    public void put(CacheKey key, Object value) {
        String keystr = key.getUniqueString();
        if (value == null) {
            cache.remove(keystr);
        } else {
            cache.put(new Element(keystr, value));
        }
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
        String keystr = key.getUniqueString();
        if (value == null) {
            cache.remove(keystr);
        } else {
            Element el = new Element(keystr, value);
            el.setTimeToLive(lifespanInSecs);
            cache.put(el);
        }
    }

    public Object get(CacheKey key) {
        Element el = cache.get(key.getUniqueString());
        return el == null ? null : el.getValue();
    }

    public boolean isCached(CacheKey key) {
        return cache.isKeyInCache(key.getUniqueString());
    }

    public List<String> getKeys() {
        return cache.getKeys();
    }

    public int getSize() {
        return cache.getSize();
    }

    public Ehcache getEHcache() {
        return cache;
    }
}
