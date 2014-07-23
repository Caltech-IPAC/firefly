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
