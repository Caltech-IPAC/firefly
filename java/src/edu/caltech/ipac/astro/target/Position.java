package edu.caltech.ipac.astro.target;


import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.Assert;

import java.io.Serializable;

class Position implements Location, Serializable, Cloneable {

    public static final float EPOCH2000= 2000.0F;
    public static final float EPOCH1950= 1950.0F;
    /**
     * lon in degrees
     */
    private final double lon;

    /**
     * lat in degrees
     */
    private final double lat;

    /**
     */
    private final ProperMotion pm;



    private final CoordinateSys coordSystem;

    /**
     * epoch of position when observed, year of observation, i.e. 1950.0
     */
    private final float epoch;


    private transient String computedLonStr = null;
    private transient String computedLatStr = null;


    /**
     * @param lon lon in degrees
     * @param lat lon in degrees
     * @param coordSystem must be one of the predefined constansts in
     *                    CoordinateSys
     * @throws IllegalArgumentException
     */
    public Position( double        lon,
                     double        lat,
                     CoordinateSys coordSystem)
                                     throws IllegalArgumentException {

           // the following line is about as clear as mud, what it is doing
           // is passing an epoch of 1950 when the coordinate system is
           // b1950, an epoch of 2000 when J2000 and epoch to 2000 in
           // all other cases.  In all other cases the epoch is meaningless
           // so it is just a placeholder
        this(lon,lat,null,coordSystem,
          coordSystem.equals(CoordinateSys.EQ_B1950) ? EPOCH1950 : EPOCH2000);
    }



    /**
     * a copy constructor.  Make a positon just like the one passed in
     * @param pos the position
     */
    public Position(Position pos) {
         this (pos.getLon(), pos.getLat(), pos.getProperMotion(),
               pos.getCoordSystem(), pos.getEpoch());
    }

    /**
     * @param lon lon in degrees
     * @param lat lon in degrees
     * @param pm the proper motion, null if position contains no proper motion
     * @param coordSystem must be one of the predefined constansts in
     *                    CoordinateSys
     * @param epoch the epoch of oberservation (date when observed)
     * @throws IllegalArgumentException
     */
    public Position( double        lon,
                     double        lat, 
                     ProperMotion  pm,
                     CoordinateSys coordSystem,
                     float         epoch) throws IllegalArgumentException {
        Assert.argTst((lon >= 0.0D && lon <= 360.0D),
           "a lon of " + lon + " is not in the range of 0 to 360");
        Assert.argTst((lat >= -90.0D && lat <= 90.0D),
           "a lat of " + lat + " is not in the range of -90 to +90");
        if (!coordSystem.isEquatorial()) {
                   Assert.argTst((pm==null),
                               "Proper motion is only allowed"+
                               " on Equatorial coordinate systems");
        }
        if (pm==null && coordSystem.equals(CoordinateSys.EQ_J2000)) {
            pm= new ProperMotion(0F,0F);
        }
        this.lon = lon;
        this.lat = lat;
        this.pm = pm;
        this.coordSystem = coordSystem;
        this.epoch = epoch;
    }





    /**
     * get the Lon of the Position in degrees
     * @return double the long
     */
    public double getLon() { return lon; }

    /**
     * get the Lat of the Position in degrees
     * @return the lat
     */
    public double getLat() { return lat; }


    public ProperMotion getProperMotion() { return pm; }

    /**
     *  get epoch of obersation
     * @return the epoch, year when observed
     */
    public float getEpoch() { return epoch; }

    /**
     * 
     */
    public CoordinateSys getCoordSystem() { return coordSystem; }

    /**
     * Implementation of the Cloneable method
     */
    public Object clone() { return new Position(this); }



    public String convertLonToString() {
       if (computedLonStr ==null) {
           try {
              computedLonStr = CoordUtil.dd2sex(lon, false, coordSystem.isEquatorial(), 7);
              //computedLonStr = TargetUtil.convertLonToString(lon,
                                                            // coordSystem);
           } catch (CoordException e) {
              computedLonStr = "";
           }
       }
       return computedLonStr;
    }

    public String convertLatToString() {
       if (computedLatStr ==null) {
           try {
              computedLatStr = CoordUtil.dd2sex(lat, true, coordSystem.isEquatorial(), 7);
                      //TargetUtil.convertLatToString(lat,
                                                            // coordSystem);
           } catch (CoordException e) {
              computedLatStr = "";
           }
       }
       return computedLatStr;
    }

    public boolean equals(Object o) {
       boolean retval= false;

       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof Position) {
           Position p= (Position)o;
           retval= ComparisonUtil.equals(lon,p.lon,.000001) &&
                   ComparisonUtil.equals(lat,p.lat,.000001) &&
                   ComparisonUtil.equals(pm,p.pm) &&
                   ComparisonUtil.equals(coordSystem,p.coordSystem) &&
                   epoch == p.epoch;
       }
       return retval;
    }

    public String toString() {
       String pmStr= "Proper Motion: None";
       if (pm !=null) {
             pmStr= "Proper Motion: Lon Pm: " + pm.getLonPm() +
                                    ", Lat Pm: " + pm.getLatPm();
       }
       String outstr= "Lon: " + lon + ", Lat: " + lat +"\n"+
               "computed Lon: " + convertLonToString() +
               ",  computed Lat: " + convertLatToString() +"\n"+
               pmStr + "\n" +
               "Coordinate System: " +
               coordSystem.toString() + "\n" +
               "Epoch: " + epoch;
       return outstr;
    }
}
