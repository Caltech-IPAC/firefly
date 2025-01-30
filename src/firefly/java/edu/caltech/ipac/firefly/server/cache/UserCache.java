/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;
import redis.clients.jedis.Jedis;

import static edu.caltech.ipac.firefly.server.RequestOwner.USER_KEY_EXPIRY;

/**
 * Date: Jul 21, 2008
 *
 * @author loi
 * @version $Id: UserCache.java,v 1.5 2009/03/23 23:55:16 loi Exp $
 */
public class UserCache<T> extends DistribMapCache<T> {

    public static <T> Cache<T> getInstance(){
        return new UserCache<>();
    }

    private UserCache() {
        super(ServerContext.getRequestOwner().getUserKey(), USER_KEY_EXPIRY);
    }

    public static boolean exists(StringKey userKey) {
        try(Jedis redis = RedisService.getConnection()) {
            return redis.exists(userKey.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
        return false;
    }
}
