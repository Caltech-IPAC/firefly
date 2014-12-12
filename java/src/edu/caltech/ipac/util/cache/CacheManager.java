package edu.caltech.ipac.util.cache;

import java.util.List;

/**
 * Date: Jul 2, 2008
 *
 * @author loi
 * @version $Id: CacheManager.java,v 1.6 2009/06/23 18:57:17 loi Exp $
 */
public class CacheManager {

    private static final String DEF_TYPE = Cache.TYPE_PERM_SMALL;
    private static final String DEF_PROVIDER = "edu.caltech.ipac.firefly.server.cache.EhcacheProvider";  //  ie... "edu.caltech.ipac.firefly.server.cache.EhcacheProvider";

    private static  Cache.Provider cacheProvider;
    private static boolean isDisabled = false;

    public static Cache getCache() {
        return getCache(DEF_TYPE);
    }

    public static Cache getCache(String type) {
        if (!isDisabled) {
            try {
                return getCacheProvider().getCache(type);
            } catch (Exception e){
                System.err.println("Unable to get Cache type:" + type + " returning EmptyCache.");
            }
        }
        return new EmptyCache();
    }

    public static Cache getSharedCache(String type) {
        if (!isDisabled) {
            try {
                return getCacheProvider().getSharedCache(type);
            } catch (Exception e){
                System.err.println("Unable to get SharedCache type:" + type + " returning EmptyCache.");
            }
        }
        return new EmptyCache();
    }

    public static Cache.Provider getCacheProvider() {
        if (cacheProvider == null) {
            cacheProvider = newInstanceOf(DEF_PROVIDER);
        }

        return cacheProvider;
    }

    public static boolean setCacheProvider(String cacheProviderClassName) {
        setCacheProvider(newInstanceOf(cacheProviderClassName));
        return cacheProvider != null;
    }

    public static void setCacheProvider(Cache.Provider cacheProvider) {
        CacheManager.cacheProvider = cacheProvider;
    }

    public static boolean isDisabled() {
        return isDisabled;
    }

    public static void setDisabled(boolean disabled) {
        isDisabled = disabled;
    }

//====================================================================
//  helper functions
//====================================================================
    private static Cache.Provider newInstanceOf(String className) {
        if (className != null && className.length() > 0) {
            try {
                Class<Cache.Provider> cc = (Class<Cache.Provider>) Cache.Provider.class.forName(className);
                return cc.newInstance();
            } catch (Exception e) {
                System.out.println("Can't create Cache.Provider:" + className + "\nThis could be a configuration error.");
                e.printStackTrace();
            }
        }
        return null;
    }

//====================================================================
//  inner classes
//====================================================================
    /**
     * An empty implementation of Cache.  This is a simple method to
     * disable caching by providing a permenantly empty cache.
     */
    private static class EmptyCache implements Cache {

        public void put(CacheKey key, Object value) {}

        public void put(CacheKey key, Object value, int lifespanInSecs) {}

        public Object get(CacheKey key) {
            return null;
        }

        public boolean isCached(CacheKey key) {
            return false;
        }

        public int getSize() {
            return 0;
        }

        public List<String> getKeys() {
            return null;
        }
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
