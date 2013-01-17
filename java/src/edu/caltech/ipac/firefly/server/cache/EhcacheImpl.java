package edu.caltech.ipac.firefly.server.cache;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.List;

/**
 * This is an implementation of Cache using Ehcache.
 *
 * Date: Jul 17, 2008
 *
 * @author loi
 * @version $Id: EhcacheImpl.java,v 1.8 2009/12/16 21:43:25 loi Exp $
 */
public class EhcacheImpl implements Cache {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    Ehcache cache;

    public EhcacheImpl(Ehcache cache) {
        this.cache = cache;
    }

    public void put(CacheKey key, Object value) {
//        logger.briefDebug("cache pre-put:" + key + " = " + StringUtils.toString(value));
        String keystr = key.getUniqueString();
        if (value == null) {
            cache.remove(keystr);
        } else {
            cache.put(new Element(keystr, value));
        }
//        logger.briefDebug("cache aft-put:" + key + " = " + StringUtils.toString(value));
    }

    public void put(CacheKey key, Object value, int lifespanInSecs) {
//        logger.briefDebug("cache pre-put:" + key +  " = " + StringUtils.toString(value) +
//                          " lifespanInSecs:" + lifespanInSecs);

        if (!cache.isEternal()) {
            throw new UnsupportedOperationException("Currently, we do not support cached object" +
                    " with idle time expiry and lifespan expiry at the same time.");
        }

        String keystr = key.getUniqueString();
        if (value == null) {
            cache.remove(keystr);
        } else {
            Element el = new Element(keystr, value);
            el.setTimeToLive(lifespanInSecs);
            cache.put(el);
        }
//        logger.briefDebug("cache aft-put:" + key +  " = " + StringUtils.toString(value) +
//                          " lifespanInSecs:" + lifespanInSecs);
    }

    public Object get(CacheKey key) {
        Element el = cache.get(key.getUniqueString());
        return el == null ? null : el.getValue();
    }

    public boolean isCached(CacheKey key) {
        return cache.isKeyInCache(key.getUniqueString());
    }

    public List<String> getKeys() {
        return cache.getKeys();
    }

    public int getSize() {
        return cache.getSize();
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
