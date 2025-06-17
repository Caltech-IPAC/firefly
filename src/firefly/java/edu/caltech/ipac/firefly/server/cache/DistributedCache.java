/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

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
    public static final int DEF_TTL = AppProperties.getIntProperty("dist.cache.ttl.hours", 14*24) * 60 * 60;   // default to 14 days in seconds
    static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String BASE64 = "BASE64::";
    private transient Predicate<T> getValidator;

    private Serializer serializer;

    public DistributedCache() {
        this(new JavaSerializer());
    }
    public DistributedCache(Serializer serializer) {
        this.serializer = serializer;
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
            try(Jedis redis = RedisService.getConnection()) {
                if (redis != null) {
                    if (value == null) {
                        del(redis, keystr);
                    } else {
                        if (lifespanInSecs > 0) {
                            setex(redis, keystr, serializer.serialize(value), lifespanInSecs);
                        } else {
                            set(redis, keystr, serializer.serialize(value));
                        }
                    }
                }
            } catch (Exception ex) { LOG.error(ex); }
    }

    public void remove(CacheKey key) {
        try(Jedis redis = RedisService.getConnection()) {
            del(redis, key.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
    }

    public T get(CacheKey key) {
        try(Jedis redis = RedisService.getConnection()) {
            T v = (T) serializer.deserialize( get(redis, key.getUniqueString()) );
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
        try(Jedis redis = RedisService.getConnection()) {
            return exists(redis, key.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
        return false;
    }

    public List<StringKey> getKeys() {
        try(Jedis redis = RedisService.getConnection()) {
            return keys(redis).stream().map(StringKey::new).toList();
        } catch (Exception ex) { LOG.error(ex); }
        return null;
    }

    public int getSize() {
        try(Jedis redis = RedisService.getConnection()) {
            return size(redis);
        } catch (Exception ex) { LOG.error(ex); }
        return -1;
    }

//====================================================================
// Implementation of redis string;  override for map, list, and set.
//====================================================================

    String  get(Jedis redis, String key) {
        return redis.get(key);
    }

    void del(Jedis redis, String key) {
        redis.del(key);
    }

    void set(Jedis redis, String key, String value) {
        redis.set(key, value);
    }

    void setex(Jedis redis, String key, String value, long lifespanInSecs) {
        redis.setex(key, lifespanInSecs, value);
    }

    @Nonnull
    List<String> keys(Jedis redis) {
        var keys = redis.keys("*");
        return keys == null ? List.of() : keys.stream().toList();
    }

    boolean exists(Jedis redis, String key) {
        return redis.exists(key);
    }

    int size(Jedis redis) {
        return Math.toIntExact(redis.dbSize());
    }

//====================================================================
//  Utility functions
//====================================================================

    public interface Serializer {
        String serialize(Object object);
        Object deserialize(String s) throws Exception;
    }

    public static class JavaSerializer implements Serializer {

        public String serialize(Object object) {
            if (object == null) return null;
            if (object instanceof String v) {
                return v;
            } else {
                return BASE64 + Util.serialize(object);
            }
        }

        public Object deserialize(String s) throws Exception {
            if (s == null) return null;
            return !s.startsWith(BASE64) ? s : Util.deserialize(s.substring(BASE64.length()));
        }
    }

}
