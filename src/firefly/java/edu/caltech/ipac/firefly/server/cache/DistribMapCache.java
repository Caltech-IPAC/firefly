/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.core.RedisService;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;

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

    public DistribMapCache(String mapKey, long ttl, ValueSerializer<T> serializer) {
        super(serializer);
        this.mapKey = mapKey;
        this.ttl = ttl;
    }

    public List<T> getValuesFor(ScanArgs scanArgs) {
        List<T> result = new ArrayList<>();
        try {
            var redis = RedisService.scanConn().sync();  // use your dedicated scan connection
            ScanCursor cursor = ScanCursor.INITIAL;

            do {
                MapScanCursor<String, byte[]> scanResult = redis.hscan(mapKey, cursor, scanArgs);
                for (Map.Entry<String, byte[]> entry : scanResult.getMap().entrySet()) {
                    result.add(deserialize(entry.getValue()));
                }
                cursor = scanResult;   // advance cursor
            } while (!cursor.isFinished());

        } catch (Exception e) {
            LOG.error(e, "Error scanning Redis hash {}: {}", mapKey, e.getMessage());
        }

        return result;
    }

//====================================================================
//  override for Redis Map implementation
//====================================================================

    byte[] get(StatefulRedisConnection<String, byte[]>  redis, String key) {
        return redis.sync().hget(mapKey, key);
    }

    void del(StatefulRedisConnection<String, byte[]>  redis, String key) {
        redis.sync().hdel(mapKey, key);
    }

    void set(StatefulRedisConnection<String, byte[]>  redis, String key, byte[] value) {
        var sync = redis.sync();
        sync.hset(mapKey, key, value);
        if (ttl > 0) {
            sync.expire(mapKey, ttl);  // renew ttl on each update
        } else if (sync.ttl(mapKey) > 0) {
            sync.persist(mapKey);      // remove ttl if it was set (only needed for correction)
        }
    }

    void setex(StatefulRedisConnection<String, byte[]>  redis, String key, byte[] value, long ttl) {
        set(redis, key, value); // ttl is managed at the map level, not individual keys
    }

    List<String> keys(StatefulRedisConnection<String, byte[]>  redis) {
        return new ArrayList<>(redis.sync().hkeys(mapKey));
    }

    boolean exists(StatefulRedisConnection<String, byte[]>  redis, String key) {
        return redis.sync().hexists(mapKey, key);
    }

    long size(StatefulRedisConnection<String, byte[]>  redis) {
        return redis.sync().hlen(mapKey);
    }

}

