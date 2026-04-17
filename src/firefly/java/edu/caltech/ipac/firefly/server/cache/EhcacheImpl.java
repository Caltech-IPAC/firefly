/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import org.ehcache.expiry.ExpiryPolicy;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This is an implementation of Cache using Ehcache 3.
 *
 * Date: Jul 17, 2008
 *
 * @author loi
 * @version $Id: EhcacheImpl.java,v 1.8 2009/12/16 21:43:25 loi Exp $
 */
public class EhcacheImpl<T> implements Cache<T> {

    /**
     * Wrapper for cache values that carry a per-entry time-to-live duration.
     * Used by {@link #put(CacheKey, Object, int)} to support per-entry TTL via
     * the {@link ExpiryPolicy} in {@link EhcacheProvider}.
     */
    record TtlValue<T>(T value, Duration ttl) implements Serializable {}

    /**
     * Returns an ExpiryPolicy where TtlValue entries use their embedded TTL,
     * and all other entries use {@code defaultExpiry} (null = keep current expiry).
     */
    static ExpiryPolicy<String, Object> makeExpiryPolicy(Duration defaultExpiry) {
        return new ExpiryPolicy<>() {
            public Duration getExpiryForCreation(String key, Object value) {
                return value instanceof TtlValue<?> tv ? tv.ttl() : defaultExpiry;
            }
            public Duration getExpiryForAccess(String key, Supplier<? extends Object> value) {
                return value.get() instanceof TtlValue<?> ? null : defaultExpiry;
            }
            public Duration getExpiryForUpdate(String key, Supplier<? extends Object> old, Object value) {
                return value instanceof TtlValue<?> tv ? tv.ttl() : defaultExpiry;
            }
        };
    }

    private final org.ehcache.Cache<String, Object> cache;
    private transient Predicate<T> getValidator;

    public EhcacheImpl(org.ehcache.Cache<String, Object> cache) {
        this.cache = cache;
    }

    public Cache<T> validateOnGet(Predicate<T> validator) {
        getValidator = validator;
        return this;
    }

    public void put(CacheKey key, T value) {
        String k = key.getUniqueString();
        if (value == null) cache.remove(k);
        else cache.put(k, value);
    }

    public void put(CacheKey key, T value, int lifespanInSecs) {
        String k = key.getUniqueString();
        if (value == null) cache.remove(k);
        else cache.put(k, new TtlValue<>(value, Duration.ofSeconds(lifespanInSecs)));
    }

    public void remove(CacheKey key) {
        cache.remove(key.getUniqueString());
    }

    @SuppressWarnings("unchecked")
    public T get(CacheKey key) {
        String k = key.getUniqueString();
        Object stored = cache.get(k);
        T v = stored instanceof TtlValue<?> tv ? (T) tv.value() : (T) stored;
        if (v != null && getValidator != null && !getValidator.test(v)) {
            cache.remove(k);
            return null;
        }
        return v;
    }

    public boolean isCached(CacheKey key) {
        return cache.containsKey(key.getUniqueString());
    }

    public List<StringKey> getKeys() {
        List<StringKey> keys = new ArrayList<>();
        cache.forEach(entry -> keys.add(new StringKey(entry.getKey())));
        return keys;
    }

    public long getSize() {
        long[] count = {0};
        cache.forEach(e -> count[0]++);
        return count[0];
    }
}
