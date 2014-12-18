package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.List;

/**
 *  this is a cache created from a public cache with a private key.
 *  all objects stored in this cache will have the private key appended to it.
 *  access to this cache require the knowledge of the private key.
 *  this is useful when storing user's specific information without session constraint.
 *  the expiration or eviction policy is based on the backed cache, and not based on session.
 *  the private key in this case is the user's key.
 *
 * @author loi
 * @version $Id: UserCache.java,v 1.5 2009/03/23 23:55:16 loi Exp $
 */
public class PrivateCache implements Cache {

    private Cache cache;
    private StringKey privateKey;

    public PrivateCache(String privateKey, Cache cache) {
        this.cache = cache;
        this.privateKey = new StringKey(privateKey);
    }

    public void put(CacheKey key, Object value) {
        cache.put(getPrivateKey(key), value);
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
        cache.put(getPrivateKey(key), value);
    }

    public Object get(CacheKey key) {
        return cache.get(getPrivateKey(key));
    }

    public boolean isCached(CacheKey key) {
        return cache.isCached(getPrivateKey(key));
    }

    public List<String> getKeys() {
        throw new UnsupportedOperationException(
                "For performance reason, this operation is not supported.");
    }

    public int getSize() {
        throw new UnsupportedOperationException(
                "For performance reason, this operation is not supported.");
    }

    private StringKey getPrivateKey(CacheKey key) {
        return new StringKey(privateKey).appendToKey(key);
    }
}
