/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;

/**
 */
public class TargetFixedSingle implements Serializable {

    private final String name;
    private final PositionJ2000 position;

    public TargetFixedSingle( String name, PositionJ2000 position) {
        this.name= name;
        this.position = position;
    }


    public TargetFixedSingle(String name, String raStr, String decStr, ProperMotion pm, CoordinateSys coordSys, float epoch) throws CoordException {
        this(name, new PositionJ2000(raStr,decStr,pm,coordSys,epoch));
    }


    public TargetFixedSingle( String name, WorldPt wpt) {
        this.name= name;
        this.position= new PositionJ2000(wpt.getLon(), wpt.getLat());
    }


    public String getName() { return name; }

    /**
     * Returns 'coords' value -- 
     * target coordinates (for user reference) (max 32 chars)
     */
    public String getCoords() { return (position !=null) ? position.getCoordAtString() : ""; }


    public String convertLonToString() { return position.convertLonToString(); }
    public String convertLatToString() { return position.convertLatToString(); }


    public WorldPt getWorldPt() { return new WorldPt(position.getRa(), position.getDec(), position.getCoordSystem()); }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o instanceof TargetFixedSingle) {
          TargetFixedSingle t= (TargetFixedSingle)o;
          if (super.equals(t)) {
              retval= ComparisonUtil.equals(position, t.position) && ComparisonUtil.equals(name,t.name);
          }
       }
       return retval;
    }
}
