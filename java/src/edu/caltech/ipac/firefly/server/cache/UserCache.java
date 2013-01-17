package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Date: Jul 21, 2008
 *
 * @author loi
 * @version $Id: UserCache.java,v 1.5 2009/03/23 23:55:16 loi Exp $
 */
public class UserCache implements Cache {

    private Cache cache;
    private StringKey sessId;

    public static final Cache getInstance(){
        return new UserCache();
    }

    @Deprecated
    public static final Cache getInstance(String type) {
        if (type.equals(Cache.TYPE_PERM_FILE) || type.equals("PERM_LARGE") || type.equals("PERM_SMALL")) {
            throw new UnsupportedOperationException("Object in user cache should have expire on idle time requirement. " +
                    "Use one of the SHORT type instead.");
        }

        return new UserCache();
    }

    private UserCache() {
        sessId = new StringKey(ServerContext.getRequestOwner().getSessionId());
        cache = CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
    }

    public StringKey getSessionId() {
        return sessId;
    }

    public void put(CacheKey key, Object value) {
        Map<CacheKey, Object> map = getSessionMap();
        map.put(key, value);
        cache.put(sessId, map);
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
        throw new UnsupportedOperationException(
                "This cache is used to store session related information.  This operation is not supported.");
    }

    public Object get(CacheKey key) {
        return getSessionMap().get(key);
    }

    public boolean isCached(CacheKey key) {
        return getSessionMap().containsKey(key);
    }

    public List<String> getKeys() {
        Set<CacheKey> keys = getSessionMap().keySet();
        ArrayList<String> list = new ArrayList<String>(keys.size());
        for(CacheKey ck : keys) {
            list.add(ck.getUniqueString());
        }
        return list;
    }

    public int getSize() {
        return getSessionMap().size();
    }

//====================================================================
//
//====================================================================

    private Map<CacheKey, Object> getSessionMap() {
        Cache cache = CacheManager.getCache(Cache.TYPE_HTTP_SESSION);
        Map<CacheKey, Object> map = (Map<CacheKey, Object>) cache.get(sessId);
        if (map == null) {
            map = new HashMap<CacheKey, Object>();
        }
        return map;
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
