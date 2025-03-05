/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.core.RedisService;
import edu.caltech.ipac.firefly.server.cache.DistribMapCache;
import edu.caltech.ipac.firefly.server.util.Logger;
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

    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    public static <T> UserCache<T> getInstance() {
        return getInstance(ServerContext.getRequestOwner().getUserKey());
    }

    /**
     * This is only used for initializing the user cache when ServerContext.getRequestOwner().getUserKey() is not available.
     * @param usrKey a unique key for the user.
     * @return a UserCache instance for the given user key.
     */
    static <T> UserCache<T> getInstance(String usrKey){
        return new UserCache<>(usrKey);
    }

    private UserCache(String userKey) {
        super(userKey, USER_KEY_EXPIRY);
    }

    public static boolean exists(StringKey userKey) {
        try(Jedis redis = RedisService.getConnection()) {
            return redis.exists(userKey.getUniqueString());
        } catch (Exception ex) { LOG.error(ex); }
        return false;
    }
}
