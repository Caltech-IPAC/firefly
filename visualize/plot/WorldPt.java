package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.StringUtils;

/**
 * This class contains a world point plus the coordinate system 
 * that the point is in.
 */
public class WorldPt extends Pt {
   private CoordinateSys _coordSys;
    
   public WorldPt() { this(0,0); }
   public WorldPt(double lon, double lat) {
       this(lon,lat,CoordinateSys.EQ_J2000);
   }
   public WorldPt(WorldPt wp) { this(wp.getLon(),wp.getLat(),wp.getCoordSys()); }
   public WorldPt(double lon, double lat, CoordinateSys coordSys) {
       super(lon,lat);
       _coordSys= coordSys;
   }


    public double getLon() { return getX(); }
    public double getLat() { return getY(); }

    public CoordinateSys  getCoordSys()   { return _coordSys;}

    public boolean equals(Object o) {
        boolean retval= super.equals(o);
        if (retval) {
            retval= false;
            if (o instanceof WorldPt) {
                WorldPt p= (WorldPt)o;
                retval= p._coordSys.equals(_coordSys);
            }
        }
        return retval;
    }

    public String toString() {
        return getX()+";"+getY()+";"+_coordSys.toString();
    }

    @Override
    public String serialize() {
        return toString();
    }

    protected static WorldPt stringAryToWorldPt(String wpParts[]) {
        WorldPt retval= null;
        if (wpParts.length>=3) {
            double lon= StringUtils.parseDouble(wpParts[0]);
            double lat= StringUtils.parseDouble(wpParts[1]);
            CoordinateSys coordSys= CoordinateSys.parse(wpParts[2]) ;
            if (!Double.isNaN(lon) && !Double.isNaN(lat) && coordSys!=null) {
                retval= new WorldPt(lon,lat,coordSys);
            }
        }
        if (wpParts.length==2) {
            double lon= StringUtils.parseDouble(wpParts[0]);
            double lat= StringUtils.parseDouble(wpParts[1]);
            if (!Double.isNaN(lon) && !Double.isNaN(lat)) {
                retval= new WorldPt(lon,lat);
            }

        }
        return retval;

    }

    public static WorldPt parse(String serString) {
        if (serString==null) return null;
        String sAry[]= serString.split(";");
        if (sAry.length<2 || sAry.length>5) return null;
        return stringAryToWorldPt(sAry);
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
