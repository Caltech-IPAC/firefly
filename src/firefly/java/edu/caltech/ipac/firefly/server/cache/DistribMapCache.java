/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import redis.clients.jedis.Jedis;
import java.util.ArrayList;
import java.util.List;

/**
 * Like {@link DistributedCache} but specifically designed for managing Redis Maps.
 * <p>
 * Date: Nov 18, 2024
 * <p>
 * @author loi
 * @version $Id: EhcacheImpl.java,v 1.8 2009/12/16 21:43:25 loi Exp $
 */
public class DistribMapCache<T> extends DistributedCache<T> {
    String mapKey;
    long lifespanInSecs;

    public DistribMapCache(String mapKey) {
        this(mapKey, 0);
    }

    public DistribMapCache(String mapKey, long lifespanInSecs) {
        this(mapKey, lifespanInSecs, new JavaSerializer());
    }

    public DistribMapCache(String mapKey, long lifespanInSecs, Serializer serializer) {
        super(serializer);
        this.mapKey = mapKey;
        this.lifespanInSecs = lifespanInSecs;
    }

//====================================================================
//  override for Redis Map implementation
//====================================================================

    String get(Jedis redis, String key) {
        return redis.hget(mapKey, key);
    }

    void del(Jedis redis, String key) {
        redis.hdel(mapKey, key);
    }

    void set(Jedis redis, String key, String value) {
        boolean exists = redis.exists(mapKey);
        redis.hset(mapKey, key, value);
        if (!exists && lifespanInSecs > 0) {
            redis.expire(mapKey, lifespanInSecs);      // set only when creating a new map; setting here instead of hset to accommodate older version of redis.
        }
    }

    void setex(Jedis redis, String key, String value, long lifespanInSecs) {
        throw new IllegalArgumentException("Cannot set expiry on individual key.  Do it as  Map");
    }

    List<String> keys(Jedis redis) {
        return new ArrayList<>(redis.hkeys(mapKey));
    }

    boolean exists(Jedis redis, String key) {
        return redis.hexists(mapKey, key);
    }

    int size(Jedis redis) {
        return (int) redis.hlen(mapKey);
    }
}

