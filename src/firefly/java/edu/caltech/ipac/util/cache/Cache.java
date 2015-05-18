/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.cache;

import java.util.List;

/**
 * Date: Jul 7, 2008
 *
 * @author loi
 * @version $Id: Cache.java,v 1.4 2009/06/23 18:57:17 loi Exp $
 */
public interface Cache {
    /**
     * This is a list of cache types each implementing cache may support.  The type
     * is described using 2 words; the first is lengh of time it may idle, and the
     * second is the type of data.
     */
    public static final String TYPE_PERM_SMALL = "PERM_SMALL";
    public static final String TYPE_PERM_LARGE = "PERM_LARGE";
    public static final String TYPE_PERM_FILE  = "PERM_FILE";
    public static final String TYPE_TEMP_FILE  = "TEMP_FILE";
    public static final String TYPE_VISUALIZE  = "VISUALIZE";
    public static final String TYPE_VIS_SHARED_MEM = "VIS_SHARED_MEM";

    /**
     * This is used to save User's session information.  It is backed
     * by UserKey.  UserKey may have longer lifespan than
     * the session data.  Currently, UserKey last for 2 weeks,
     * while session only last for 30min.
     */
    static final String TYPE_HTTP_SESSION = "HTTP_SESSION";


    void put(CacheKey key, Object value);
    void put(CacheKey key, Object value, int lifespanInSecs);
    Object get(CacheKey key);
    boolean isCached(CacheKey key);
    int getSize();

    /**
     * returns a list of keys in this cache as string.
     * @return
     */
    List<String> getKeys();


    interface Provider {
        Cache getCache(String type);
    }
}
