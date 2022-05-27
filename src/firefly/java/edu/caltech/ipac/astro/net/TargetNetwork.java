/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import static edu.caltech.ipac.astro.net.Resolver.NONE;
import static edu.caltech.ipac.astro.net.Resolver.UNKNOWN;

/*
 * This is the class that all the Name resolving related network request
 * go through. It provides caching for previous successful calls
 */
public class TargetNetwork {

    public final static int TWO_MONTHS= 60 * 86400;
    private final static Cache objCache= CacheManager.getCache(Cache.TYPE_PERM_SMALL);

    public static ResolvedWorldPt resolveToWorldPt(String objName, Resolver resolver) {
        if (resolver==null || resolver==UNKNOWN || resolver==NONE) {
            throw new IllegalArgumentException("resolver must be NED, Simbad, SimbadThenNed, NedThenSimbad, or PTF");
        }
        ResolvedWorldPt wp;
        for(Resolver r : resolver.getConcertResolvers()) {
            switch (r) {
                case NED : wp= resolve(objName, r, NedNameResolver::resolveName); break;
                case Simbad : wp=  resolve(objName, r, SimbadNameResolver::resolveName); break;
                case PTF: wp= resolve(objName, r, PTFNameResolver::resolveName); break;
                default: wp= null;
            }
            if (wp!=null) return wp;
        }
        return null;
    }

    public static HorizonsEphPairs.HorizonsResults[] getEphInfo(String nameOrId) throws FailedRequestException {
        HorizonsEphPairs.HorizonsResults[] res;
        HorizonsParams params= new HorizonsParams(nameOrId);
        res= (HorizonsEphPairs.HorizonsResults[])objCache.get(params);
        if (res==null) {
            res= HorizonsEphPairs.lowlevelGetEphInfo(nameOrId);
            if (res.length>0) objCache.put(params,res,TWO_MONTHS);
        }
        return res;
    }

    private interface ResolveCaller { ResolveResult callNetworkResolver(String objName) throws FailedRequestException; }

    private static ResolvedWorldPt resolve(String objName, Resolver resolver, ResolveCaller r) {
        try {
            ResolverParams params= new ResolverParams(objName,resolver);
            ResolveResult a= (ResolveResult) objCache.get(params);
            if (a == null)  {          // if not in cache
                a = r.callNetworkResolver(objName);
                objCache.put(params,a, TWO_MONTHS);
            }
            return (a!=null) ? a.getWorldPt() : null;
        } catch (FailedRequestException e) {
            return null;
        }
    }
}