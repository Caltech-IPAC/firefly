/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.serialization.Serializer;
import io.lettuce.core.api.StatefulRedisConnection;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

import static edu.caltech.ipac.util.serialization.Serializer.*;

/**
 * This class provides an implementation of a distributed cache using Redis.
 * <p>
 * While Redis supports various data structures such as lists, sets, and maps,
 * this implementation is designed to focus solely on string-based storage.
 * In some cases, the stored strings may represent JSON-formatted data to encapsulate
 * more complex data structures.
 * <p>
 * For storage of plain objects beyond simple strings, this implementation
 * uses Base64 encoding. Objects are serialized into Base64-encoded strings
 * for storage and are deserialized back into objects when retrieved from the cache.
 * This approach ensures compatibility with Redis's string data type while
 * maintaining flexibility for handling diverse data types.
 * <p>
 * Date: Nov 18, 2024
 * @author loi
 * @version $Id: EhcacheImpl.java,v 1.8 2009/12/16 21:43:25 loi Exp $
 */
public class DistributedCache<T> implements Cache<T> {
    public static final int DEF_TTL = AppProperties.getIntProperty("dist.cache.ttl.hours", 7*24) * 60 * 60;   // default to 7 days in seconds
    static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String BASE64 = "BASE64::";
    private transient Predicate<T> getValidator;

    private final ValueSerializer<T> serializer;      // required; default is DefaultImpl

    public DistributedCache() {
        this(new DefaultImpl<>());
    }
    public DistributedCache(ValueSerializer<T> serializer) {
        this.serializer = serializer;
    }


    byte[] serialize(Object value) {
        return serializer.serialize(value);
    }

    T deserialize(byte[] value) throws Exception {
        return serializer.deserialize(value);
    }

    public Cache<T> validateOnGet(Predicate<T> validator) {
        getValidator = validator;
        return this;
    }

    public void put(CacheKey key, Object value) {
        put(key, value, DEF_TTL);
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
            String keystr = key.getUniqueString();
            try {
                var redis = RedisService.mainConn();
                if (value == null) {
                    del(redis, keystr);
                } else {
                    if (lifespanInSecs > 0) {
                        setex(redis, keystr, serialize(value), lifespanInSecs);
                    } else {
                        set(redis, keystr, serialize(value));
                    }
                }
            } catch (Exception ex) { LOG.error(ex); }
    }

    public void remove(CacheKey key) {
        try {
            var redis = RedisService.mainConn();
            del(redis, key.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
    }

    public T get(CacheKey key) {
        try {
            var redis = RedisService.mainConn();
            T v = deserialize( get(redis, key.getUniqueString()) );
            if (v != null && getValidator != null && !getValidator.test(v)) {
                del(redis, key.getUniqueString());
                return null;
            } else {
                return v;
            }
        } catch (Exception ex) {
            remove(key);
            LOG.warn("Encountered %s while retrieving key=%s; removing entry from cache.".formatted(ex.getClass().getName(), key) );
        }
        return null;
    }

    public boolean isCached(CacheKey key) {
        try {
            var redis = RedisService.mainConn();
            return exists(redis, key.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
        return false;
    }

    @Nonnull
    public List<StringKey> getKeys() {
        try {
            return keys(RedisService.scanConn()).stream().map(StringKey::new).toList();
        } catch (Exception ex) { LOG.error(ex); }
        return List.of();
    }

    public long getSize() {
        try {
            return size(RedisService.mainConn());
        } catch (Exception ex) { LOG.error(ex); }
        return -1;
    }

//====================================================================
// Implementation of redis string;  override for map, list, and set.
//====================================================================

    byte[] get(StatefulRedisConnection<String, byte[]> redis, String key) {
        return redis.sync().get(key);
    }

    void del(StatefulRedisConnection<String, byte[]> redis, String key) {
        redis.sync().del(key);
    }

    void set(StatefulRedisConnection<String, byte[]> redis, String key, byte[] value) {
        redis.sync().set(key, value);
    }

    void setex(StatefulRedisConnection<String, byte[]> redis, String key, byte[] value, long lifespanInSecs) {
        redis.sync().setex(key, lifespanInSecs, value);
    }

    @Nonnull
    List<String> keys(StatefulRedisConnection<String, byte[]> redis) {
        var keys = redis.sync().keys("*");
        return keys == null ? List.of() : keys.stream().toList();
    }

    boolean exists(StatefulRedisConnection<String, byte[]> redis, String key) {
        return redis.sync().exists(key) > 0;
    }

    long size(StatefulRedisConnection<String, byte[]> redis) {
        return redis.sync().dbsize();
    }

//====================================================================
//  Utility functions
//====================================================================

    public interface ValueSerializer<T> {
        byte[] serialize(Object object);
        T deserialize(byte[] s) throws Exception;
    }

    /**
     * Default implementation of the Serializer.  It serializes objects to Base64 strings
     * using java serialization.  When the object is a String, it returns the string directly.
     */
    public static class DefaultImpl<T> implements ValueSerializer<T> {

        public byte[] serialize(Object object) {
            if (object == null) return null;
            return Serializer.toTypedMessagePack(object);
        }

        public T deserialize(byte[] raw) throws Exception {
            if (raw == null) return null;
            // prior to MsgPack, JobInfo was stored as JSON string
            // for the time being, we will add special case here
            // it may be removed in the future
            if (looksLikeJson(raw)) {
                try {
                    return (T) Serializer.fromJson(toUtf8(raw), JobInfo.class);
                } catch (Exception ignored) {}
            }
            var val = Serializer.fromTypedMessagePack(raw);
            if (val instanceof String s) {
                if (s.startsWith(BASE64)) {
                    return (T) Util.deserialize(s.substring(BASE64.length()));
                } else {
                    return (T) s;
                }
            } else {
                return (T) val;
            }
        }
    }
}
