/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.astro.net.Resolver;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

/**
 * This class contains a world point plus the way it was resolved
 */
public final class ResolvedWorldPt extends WorldPt {

    private final String objName;
    private final Resolver resolver;
    private final String objType;

    public ResolvedWorldPt(WorldPt wp, String objName, Resolver resolver, String objType) {
        this(wp.getLon(),wp.getLat(), wp.getCoordSys(), objName, resolver, objType);
    }

   public ResolvedWorldPt(double lon, double lat, String objName, Resolver resolver, String objType) {
       this(lon, lat, CoordinateSys.EQ_J2000, objName, resolver, objType);
   }
   public ResolvedWorldPt(double lon,
                          double lat,
                          CoordinateSys coordSys,
                          String objName,
                          Resolver resolver,
                          String objType) {
       super(lon,lat,coordSys);
       this.objName = objName;
       this.resolver = resolver;
       this.objType= objType;
   }

    public Resolver getResolver() { return resolver;}
    public String getObjName() { return objName;}
    public String getObjType() { return objType;}

    public boolean equals(Object o) {
        boolean retval= super.equals(o);
        if (retval) {
            retval= false;
            if (o instanceof ResolvedWorldPt p) {
                retval= p.resolver.equals(resolver) && ComparisonUtil.equals(p.objName, objName);
            }
        }
        return retval;
    }

    public String toString() {
        String retval;
        if ((resolver ==Resolver.UNKNOWN || resolver ==Resolver.NONE) && objName ==null && objType ==null) {
            retval= super.toString();
        }
        else {
            retval= super.toString()+";"+ objName +";"+ resolver;
            if (!StringUtils.isEmpty(objType)) retval+=";"+objType;
        }
        return retval;
    }

    public static ResolvedWorldPt parse(String serString) {
        if (serString==null) return null;
        String[] sAry= serString.split(";");
        if (sAry.length==3 || sAry.length==2) {
            WorldPt wp= WorldPt.parse(serString);
            if (wp==null) return null;
            return new ResolvedWorldPt(wp.getLon(),wp.getLat(), wp.getCoordSys(), null, Resolver.NONE, null);
        }
        else  if (sAry.length==5 || sAry.length==4 || sAry.length==6)  {
            WorldPt wp= WorldPt.stringAryToWorldPt(sAry);
            if (wp==null) return null;
            Resolver resolver= sAry.length==5 ? Resolver.parse(sAry[4]) : Resolver.UNKNOWN;
            String objType= sAry.length==6 ? StringUtils.checkNull(sAry[5]) : null;
            return new ResolvedWorldPt(wp, StringUtils.checkNull(sAry[3]),resolver,objType);
        }
        return null;
    }

}
