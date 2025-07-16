/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.core.RedisService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    long ttl;

    public DistribMapCache(String mapKey) {
        this(mapKey, DEF_TTL);
    }

    public DistribMapCache(String mapKey, long ttl) {
        this(mapKey, ttl, new DefaultImpl<>());
    }

    public DistribMapCache(String mapKey, long ttl, Serializer<T> serializer) {
        super(serializer);
        this.mapKey = mapKey;
        this.ttl = ttl;
    }

    public List<T> getValuesFor(ScanParams scanParams) {
        List<T> result = new ArrayList<>();
        try (Jedis jedis = RedisService.getConnection()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            do {
                ScanResult<Map.Entry<String, String>> scanResult = jedis.hscan(mapKey, cursor, scanParams);
                for (Map.Entry<String, String> entry : scanResult.getResult()) {
                    result.add(deserialize(entry.getValue()));
                }
                cursor = scanResult.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return result;
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
        redis.hset(mapKey, key, value);
        if (ttl > 0) {
            redis.expire(mapKey, ttl);  // renew ttl on each update
        } else if (redis.ttl(mapKey) > 0) {
            redis.persist(mapKey);      // remove ttl if it was set (only needed for correction)
        }
    }

    void setex(Jedis redis, String key, String value, long ttl) {
        set(redis, key, value); // ttl is managed at the map level, not individual keys
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

