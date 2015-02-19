/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.astro.target.NedAttribute;
import edu.caltech.ipac.astro.target.PTFAttribute;
import edu.caltech.ipac.astro.target.PositionJ2000;
import edu.caltech.ipac.astro.target.SimbadAttribute;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ResolvedWorldPt;

import javax.swing.JFrame;

/*
 * This is the new class that all the Target related network request 
 * go through.  It is a static class that uses 
 * client.net.NetCache to determine if it
 * can get a request from the cache or it has to go to the network.
 * There is one public method here for each network request the 
 * the visualization package does.  When we add a new type of 
 * request we will add new methods here.
 */
public class TargetNetwork {


    public final static int TWO_MONTHS= 60 * 86400;

    public static ResolvedWorldPt resolveToWorldPt(String objName, Resolver resolver) throws FailedRequestException {
        ResolvedWorldPt retval;
        PositionJ2000 pos;
        switch (resolver) {
            case NED :
                pos= getNedPosition( new NedParams(objName)).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(),objName,Resolver.NED);
                break;
            case Simbad :
                pos= getSimbadPosition( new SimbadParams(objName)).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
                break;
            case SimbadThenNed :
                retval= getSimbadThenNed(objName);
                break;
            case NedThenSimbad :
                retval= getNedThenSimbad(objName);
                break;
            case PTF :
                pos= getPtfPosition(new PTFParams(objName)).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.PTF);
                break;
            case Smart :
                retval= null;
                break;
            case NONE :
                throw new FailedRequestException("Cannot resolved: resolver set to NONE");
            default:
                retval= null;
                Assert.argTst(false, "resolver must be NED, Simbad, or NONE");
                break;
        }
        return retval;
    }


    public static ResolvedWorldPt getNedThenSimbad(String objName) throws FailedRequestException {
        ResolvedWorldPt wp= null;
        try {
            PositionJ2000 pos= getNedPosition( new NedParams(objName)).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.NED);
        } catch (FailedRequestException e) {
            wp= null;
        }
        if (wp==null)  {
            PositionJ2000 pos= getSimbadPosition( new SimbadParams(objName)).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
        }
        return wp;
    }

    public static ResolvedWorldPt getSimbadThenNed(String objName) throws FailedRequestException {
        ResolvedWorldPt wp= null;
        try {
            PositionJ2000 pos= getSimbadPosition(new SimbadParams(objName)).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
        } catch (FailedRequestException e) {
            wp= null;
        }
        if (wp==null) {
            PositionJ2000 pos= getNedPosition(new NedParams(objName)).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.NED);
        }
        return wp;
    }

   public static NedAttribute getNedPosition(NedParams params)
                                               throws FailedRequestException {
      NedAttribute na= (NedAttribute) CacheHelper.getObj(params);
      if (na==null)  {          // if not in cache
          PositionJ2000 pos= NedNameResolver.getPositionVOTable(params.getName());
          na= new NedAttribute(pos);
         CacheHelper.putObj(params,na);
      }
      return na;
   }

   public static SimbadAttribute getSimbadPosition(SimbadParams params)
                                               throws FailedRequestException {
       SimbadAttribute sa= (SimbadAttribute)CacheHelper.getObj(params);
       if (sa == null)  {          // if not in cache
           sa = SimbadNameResolver.lowlevelNameResolver(params.getName());
           CacheHelper.putObj(params,sa);
       }
       return sa;
   }

    public static PTFAttribute getPtfPosition(PTFParams params) throws FailedRequestException {
        PTFAttribute pa= (PTFAttribute)CacheHelper.getObj(params);
        if (pa == null)  {          // if not in cache
            pa = PTFNameResolver.lowlevelNameResolver(params.getName());
            CacheHelper.putObj(params,pa);
        }
        return pa;
    }

   public static HorizonsEphPairs.HorizonsResults[]
                   getEphIDInfo(String name, boolean showError, JFrame f)
                                 throws FailedRequestException{
       return getEphInfo(name,showError,f);
   }

    public static HorizonsEphPairs.HorizonsResults[]
                getEphNameInfo(String id, boolean showError, JFrame f) throws FailedRequestException {
        return getEphInfo(id,showError, f);
    }


    private static HorizonsEphPairs.HorizonsResults[]
                   getEphInfo(String nameOrId, boolean showError, JFrame f)
                                     throws FailedRequestException {
        HorizonsEphPairs.HorizonsResults res[];
        HorizonsParams params= new HorizonsParams(nameOrId);
        res= (HorizonsEphPairs.HorizonsResults[])CacheHelper.getObj(params);
        if (res==null) {
            res= HorizonsEphPairs.lowlevelGetEphInfo(nameOrId);

            CacheHelper.putObj(params,res,TWO_MONTHS);
        }
        return res;
    }




}
