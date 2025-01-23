/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.List;
import java.util.function.Predicate;

/**
 * This is an implementation of Cache using Ehcache.
 *
 * Date: Jul 17, 2008
 *
 * @author loi
 * @version $Id: EhcacheImpl.java,v 1.8 2009/12/16 21:43:25 loi Exp $
 */
public class EhcacheImpl<T> implements Cache<T> {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    Ehcache cache;
    private transient Predicate<T> getValidator;

    public Cache<T> validateOnGet(Predicate<T> validator) {
        getValidator = validator;
        return this;
    }

    public EhcacheImpl(Ehcache cache) {
        this.cache = cache;
    }

    public void put(CacheKey key, T value) {
        String keystr = key.getUniqueString();
        if (value == null) {
            cache.remove(keystr);
        } else {
            cache.put(new Element(keystr, value));
        }
    }

    public void put(CacheKey key, T value, int lifespanInSecs) {
        String keystr = key.getUniqueString();
        if (value == null) {
            cache.remove(keystr);
        } else {
            Element el = new Element(keystr, value);
            el.setTimeToLive(lifespanInSecs);
            cache.put(el);
        }
    }

    public void remove(CacheKey key) {
        cache.remove(key.getUniqueString());
    }

    public T get(CacheKey key) {
        Element el = cache.get(key.getUniqueString());
        T v = el == null ? null : (T) el.getValue();
        if (v != null && getValidator != null && !getValidator.test(v)) {
            cache.remove(key.getUniqueString());
            return null;
        } else {
            return v;
        }
    }

    public boolean isCached(CacheKey key) {
        return cache.isKeyInCache(key.getUniqueString());
    }

    public List<StringKey> getKeys() {
        return cache.getKeys().stream().map(k -> new StringKey(k.toString())).toList();
    }

    public int getSize() {
        return cache.getSize();
    }

    public Ehcache getEHcache() {
        return cache;
    }
}
