/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import redis.clients.jedis.Jedis;

import java.util.List;

import static edu.caltech.ipac.firefly.core.Util.deserialize;
import static edu.caltech.ipac.firefly.core.Util.serialize;

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
public class DistributedCache implements Cache {
    static final Logger.LoggerImpl LOG = Logger.getLogger();
    private static final String BASE64 = "BASE64::";

    public void put(CacheKey key, Object value) {
        put(key, value, 0);
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
            String keystr = key.getUniqueString();
            try(Jedis redis = RedisService.getConnection()) {
                if (redis != null) {
                    if (value == null) {
                        del(redis, keystr);
                    } else {
                        if (lifespanInSecs > 0) {
                            setex(redis, keystr, v2set(value), lifespanInSecs);
                        } else {
                            set(redis, keystr, v2set(value));
                        }
                    }
                }
            } catch (Exception ex) { LOG.error(ex); }
    }

    public Object get(CacheKey key) {
        try(Jedis redis = RedisService.getConnection()) {
            return v2get( get(redis, key.getUniqueString()) );
        } catch (Exception ex) { LOG.error(ex); }
        return null;
    }

    public boolean isCached(CacheKey key) {
        try(Jedis redis = RedisService.getConnection()) {
            return exists(redis, key.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
        return false;
    }

    public List<String> getKeys() {
        try(Jedis redis = RedisService.getConnection()) {
            return keys(redis);
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

    List<String> keys(Jedis redis) {
        return redis.keys("*").stream().toList();
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

    static String v2set(Object object) {
        if (object instanceof String v) {
            return v;
        } else {
            return BASE64 + serialize(object);
        }
    }

    static Object v2get(Object object) {
        if (object instanceof String v) {
            return v.startsWith(BASE64) ? deserialize(v.substring(BASE64.length())) : v;
        } else {
            return object;      // this should not happen.
        }
    }

}
