/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

/**
 * This class contains a world point plus the coordinate system 
 * that the point is in.
 */
public class WorldPt extends Pt {
   private final CoordinateSys _coordSys;
    
   public WorldPt(double lon, double lat) {
       this(lon,lat,CoordinateSys.EQ_J2000);
   }
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
            if (o instanceof WorldPt p) {
                retval= p._coordSys.equals(_coordSys);
            }
        }
        return retval;
    }

    public String toString() {
        return getX()+";"+getY()+";"+_coordSys.toString();
    }


    protected static WorldPt stringAryToWorldPt(String wpParts[]) {
        WorldPt retval= null;
        if (wpParts.length>=3) {
            double lon= parseDouble(wpParts[0]);
            double lat= parseDouble(wpParts[1]);
            CoordinateSys coordSys= CoordinateSys.parse(wpParts[2]) ;
            if (!Double.isNaN(lon) && !Double.isNaN(lat) && coordSys!=null) {
                retval= new WorldPt(lon,lat,coordSys);
            }
        }
        if (wpParts.length==2) {
            double lon= parseDouble(wpParts[0]);
            double lat= parseDouble(wpParts[1]);
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
