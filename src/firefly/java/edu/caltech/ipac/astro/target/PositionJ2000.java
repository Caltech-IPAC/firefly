/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

/**
 * This subclass of Position add three things to Postion
 *  <ul>
 *  <li>The Position is always in J2000
 *  <li>The Epoch is always 2000
 *  <li>It carries the original position that is was constructed with.
 *
 */
public final class PositionJ2000 extends Position {

    private final static ProperMotion DEFAULT_PM= new ProperMotion(0,0);

    private String userRaStr= null;
    private String userDecStr= null;

    /**
     * @param ra ra in degrees
     * @param dec dec in degrees
     */
    public PositionJ2000( double ra, double dec) {
        super(ra,dec,DEFAULT_PM, CoordinateSys.EQ_J2000, EPOCH2000);
    }


    public PositionJ2000(String raStr, String decStr, ProperMotion pm, CoordinateSys coordSys, float epoch) throws CoordException {
        super( TargetUtil.convertTo( new Position(
                TargetUtil.convertStringToLon(raStr, coordSys),
                TargetUtil.convertStringToLat(decStr, coordSys),
                pm, coordSys, epoch), CoordinateSys.EQ_J2000));
        this.userRaStr= raStr;
        this.userDecStr= raStr;
    }

    public String getCoordAtString() {
        if (userRaStr!=null && userDecStr!=null) {
            return userRaStr+","+userDecStr;
        }
        else {
            return getLon()+"d"+","+getLat()+"d";
        }

    }

    /**
     * RA of the position in degrees.
     * @return the ra
     */
    public double getRa()  { return getLon(); }
    /**
     * Dec of the position in degrees.
     * @return the dec
     */
    public double getDec() { return getLat(); }


    public boolean equals(Object o) {
       boolean retval= false;

       if (o==this) {
          retval= true;
       }
       else if (o instanceof PositionJ2000) {
           retval= super.equals(o);
           if (retval) {
               PositionJ2000 p= (PositionJ2000)o;
               return (ComparisonUtil.equals(userRaStr,p.userDecStr) && ComparisonUtil.equals(userDecStr,p.userDecStr));
           }
       }
       return retval;
    };

    public String toString() {
       return "J2000 Position: " + super.toString();

    }
}
