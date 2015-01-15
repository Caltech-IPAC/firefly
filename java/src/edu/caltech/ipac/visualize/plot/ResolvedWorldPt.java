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

    private String _objName;
    private Resolver _resolver;

   public ResolvedWorldPt() { this(0,0, null, Resolver.NONE); }

    public ResolvedWorldPt(WorldPt wp) { this(wp.getLon(),wp.getLat(), wp.getCoordSys(), null, Resolver.NONE); }
    public ResolvedWorldPt(WorldPt wp, String objName, Resolver resolver) {
        this(wp.getLon(),wp.getLat(), wp.getCoordSys(), objName, resolver);
    }

   public ResolvedWorldPt(double lon, double lat, String objName, Resolver resolver) {
       this(lon, lat, CoordinateSys.EQ_J2000, objName, resolver);
   }
   public ResolvedWorldPt(double lon,
                          double lat,
                          CoordinateSys coordSys,
                          String objName,
                          Resolver resolver) {
       super(lon,lat,coordSys);
       _objName= objName;
       _resolver= resolver;
   }

    public static ResolvedWorldPt makePt(WorldPt wp, String objName, Resolver resolver) {
        return wp==null ? null : new ResolvedWorldPt(wp,objName,resolver);
    }

    public static ResolvedWorldPt makePt(WorldPt wp) { return wp==null ? null : new ResolvedWorldPt(wp); }

    public Resolver getResolver() { return _resolver;}
    public String getObjName() { return _objName;}

    public boolean equals(Object o) {
        boolean retval= super.equals(o);
        if (retval) {
            retval= false;
            if (o instanceof ResolvedWorldPt) {
                ResolvedWorldPt p= (ResolvedWorldPt)o;
                retval= p._resolver.equals(_resolver) && ComparisonUtil.equals(p._objName, _objName);
            }
        }
        return retval;
    }

    public String toString() {
        String retval;
        if ((_resolver==Resolver.UNKNOWN || _resolver==Resolver.NONE) && _objName==null) {
            retval= super.toString();
        }
        else {
            retval= super.toString()+";"+_objName+";"+_resolver;
        }
        return retval;
    }

    public static ResolvedWorldPt parse(String serString) {
        if (serString==null) return null;
        String sAry[]= serString.split(";");
        if (sAry.length==3 || sAry.length==2) {
            WorldPt wp= WorldPt.parse(serString);
            if (wp!=null) {
                return new ResolvedWorldPt(wp);
            }
        }
        else  if (sAry.length==5 || sAry.length==4)  {
            WorldPt wp= WorldPt.stringAryToWorldPt(sAry);
            if (wp!=null) {
                Resolver resolver= sAry.length==5 ? Resolver.parse(sAry[4]) : Resolver.UNKNOWN;
                return new ResolvedWorldPt(wp, StringUtils.checkNull(sAry[3]),resolver);
            }
        }
        return null;
    }

}
