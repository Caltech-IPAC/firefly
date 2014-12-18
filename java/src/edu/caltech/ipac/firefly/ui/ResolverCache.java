package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 5/27/11
 * Time: 2:05 PM
 */


import edu.caltech.ipac.firefly.data.EphPair;
import edu.caltech.ipac.firefly.util.BrowserCache;
import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
class ResolverCache {
    private static final long MONTH_IN_SECS= 60L * 60L  * 24L * 30L ;   //  1 hour * 24 hours * 30 days = 1 month

    private final static String SPLIT_TOKEN= "--Ary--";
    private static final boolean ENABLE_CACHE= true;

    public static boolean isMovingCached(String name) {
        List<EphPair> list= getMoving(name);
        return ENABLE_CACHE && list!=null && list.size()>0;
    }

    public static boolean isCached(String name, Resolver resolver) {
        return (ENABLE_CACHE && get(name,resolver)!=null);
    }

    public static ResolvedWorldPt get(String name, Resolver resolver) {
        ResolvedWorldPt retval= null;
        if (name!=null && resolver!=null) {
            String key= getResolveKey(name,resolver);
            if (BrowserCache.isCached(key)) {
                retval= ResolvedWorldPt.parse(BrowserCache.get(key));
            }
        }
        return retval;
    }

    public static List<EphPair> getMoving(String key ) {
        List<EphPair> retval= null;
        if (BrowserCache.isCached(key)) {
            String v= BrowserCache.get(key);
            if (v!=null) {
                String sAry[]= v.split(SPLIT_TOKEN);
                if (sAry.length>0) {
                    retval= new ArrayList<EphPair>(sAry.length);
                    for(String s : sAry) {
                        EphPair p= EphPair.parse(s);
                        if (p!=null) retval.add(p);
                    }
                }
            }
        }
        return retval;
    }



    /**
     * Cached a ResolvedWorldPt.
     * @param wp the resolved point.
     */
    public static void put(ResolvedWorldPt wp){ put(wp,wp.getResolver()); }


    /**
     * @param key the resolved point.
     * @param idList the naif id's that match
     */
    public static void putMoving(String key, List<EphPair> idList){
        if (key!=null && idList!=null && idList.size()>0) {
            StringBuilder sb= new StringBuilder(30);
            for(int i= 0; (i<idList.size()); i++) {
                sb.append(idList.get(i));
                if (i<idList.size()-1)  sb.append(SPLIT_TOKEN);

            }
            BrowserCache.put(key,sb.toString(),MONTH_IN_SECS);
        }
    }

    /**
     * Cached a ResolvedWorldPt.
     * @param wp the resolved point.
     * @param specifiedResolver the resolver specified to resolve this point.  It might be different than the Resolver returned
     *                          in the ResolvedWorldPt, since a specified resolver could be NedThenSimbad and the actual
     *                          resolved point would be Simbad.
     */
    public static void put(ResolvedWorldPt wp, Resolver specifiedResolver){
        if (specifiedResolver!=null && wp!=null && wp.getObjName()!=null) {
            String key= getResolveKey(wp.getObjName(), specifiedResolver);
            BrowserCache.put(key,wp.toString(),MONTH_IN_SECS);
        }
    }


    private static String getResolveKey(String name, Resolver resolvedBy) {
        return name.trim() +"--" + resolvedBy;
    }

}

