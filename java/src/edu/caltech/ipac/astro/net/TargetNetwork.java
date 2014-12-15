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
import java.awt.Window;

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
                pos= getNedPosition( new NedParams(objName),null).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(),objName,Resolver.NED);
                break;
            case Simbad :
                pos= getSimbadPosition( new SimbadParams(objName),null).getPosition();
                retval= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
                break;
            case SimbadThenNed :
                retval= getSimbadThenNed(objName);
                break;
            case NedThenSimbad :
                retval= getNedThenSimbad(objName);
                break;
            case PTF :
                pos= getPtfPosition(new PTFParams(objName), null).getPosition();
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
            PositionJ2000 pos= getNedPosition( new NedParams(objName),null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.NED);
        } catch (FailedRequestException e) {
            wp= null;
        }
        if (wp==null)  {
            PositionJ2000 pos= getSimbadPosition( new SimbadParams(objName),null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
        }
        return wp;
    }

    public static ResolvedWorldPt getSimbadThenNed(String objName) throws FailedRequestException {
        ResolvedWorldPt wp= null;
        try {
            PositionJ2000 pos= getSimbadPosition(new SimbadParams(objName), null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.Simbad);
        } catch (FailedRequestException e) {
            wp= null;
        }
        if (wp==null) {
            PositionJ2000 pos= getNedPosition(new NedParams(objName), null).getPosition();
            if (pos!=null) wp= new ResolvedWorldPt(pos.getLon(), pos.getLat(), objName, Resolver.NED);
        }
        return wp;
    }

   public static NedAttribute getNedPosition(NedParams params, Window w)
                                               throws FailedRequestException {
      NedAttribute na= (NedAttribute) CacheHelper.getObj(params);
      if (na==null)  {          // if not in cache
          PositionJ2000 pos= NedNameResolver.getPositionVOTable(params.getName());
          na= new NedAttribute(pos);
         CacheHelper.putObj(params,na);
      }
      return na;
   }

   public static SimbadAttribute getSimbadPosition(SimbadParams params,
                                                   Window       w)
                                               throws FailedRequestException {
       SimbadAttribute sa= (SimbadAttribute)CacheHelper.getObj(params);
       if (sa == null)  {          // if not in cache
           sa = SimbadNameResolver.lowlevelNameResolver(params.getName());
           CacheHelper.putObj(params,sa);
       }
       return sa;
   }

    public static PTFAttribute getPtfPosition(PTFParams params,
                                              Window       w) throws FailedRequestException {
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
