package edu.caltech.ipac.target;


import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.astro.conv.LonLat;
import edu.caltech.ipac.astro.conv.RaDecPM;

import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

/**
 * a util class for target
 * @author Xiuqin Wu
 */
public class TargetUtil {

   private static final int MAX_FRACTION_DIGITS = 9;
   private static final double DtoR = Math.PI/180.0;
   private static final double RtoD = 180.0/Math.PI;
   private static final double EPS  = 1.e-12;

   private static NumberFormat nf = NumberFormat.getInstance();

    public static enum sortFirst { RA, DEC }

   static {
      nf.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
      nf.setMinimumFractionDigits(MAX_FRACTION_DIGITS);
      }

   public static String convertLonToString(double        lon,
                                           CoordinateSys coordSystem )
                                                 throws CoordException {
       return convertLonToString(lon,coordSystem.isEquatorial());
   }

   public static String convertLonToString(double lon, boolean isEquatorial )
                   throws CoordException {
       int convertionType= isEquatorial ? 2 : 0;
       //return new Coord(lon, false, isEquatorial).c_ddsex(convertionType);
       return CoordUtil.dd2sex(lon, false, isEquatorial, 5);
    }

   public static String convertLatToString(double        lat,
                                           CoordinateSys coordSystem )
                                                  throws CoordException {
       return convertLatToString(lat,coordSystem.isEquatorial());
   }

    public static String convertLatToString(double lat, boolean isEquatorial)
                                                  throws CoordException {
        int convertionType= isEquatorial  ? 1 : 0;
//        return new Coord(lat, true, isEquatorial).c_ddsex(convertionType);
        return CoordUtil.dd2sex(lat, true, isEquatorial, 5);
    }

   public static double convertStringToLon(String        hms,
                                      CoordinateSys coordSystem )
                                                  throws CoordException {
       boolean eq= coordSystem.isEquatorial();
       //return new Coord(hms, false, eq).c_coord();
       return CoordUtil.sex2dd(hms,false, eq);
   }

   public static double convertStringToLat(String        dms,
                                           CoordinateSys coordSystem )
                                                  throws CoordException {
       boolean eq= coordSystem.isEquatorial();
       //return new Coord(dms, true, eq).c_coord();
       return CoordUtil.sex2dd(dms,true, eq);
   }



   /**
    * convert a Position from one coordSystem to another
    * @param inP the input position
    * @param coordSystem the CoordinateSys of the new position
    *              been set as disired output system.
    * @return a new Position converted to the CoordinateSys
    *           that you requested
    */

   public static Position convertTo(Position      inP,
                                    CoordinateSys coordSystem) {

      Position outP= null;

      if (inP.getCoordSystem()==coordSystem) {
         if (coordSystem==CoordinateSys.EQ_J2000) {
             return correctJ2000Epoch(inP);
         }
         else {
            return inP;
         }
      }


      double in_year;
      int    in_sys = 0;

      double out_year;
      int    out_sys;

      in_year = inP.getCoordSystem().getEquinoxYear();
      out_year= coordSystem.getEquinoxYear();


      in_sys = inP.getCoordSystem().getJsys();
      out_sys= coordSystem.getJsys();

      // note change - epoch is always 2000 unless specificly 1950
      // compare to old code below- is this correct????
      float  outEpoch= Position.EPOCH2000;
      if (out_year ==  1950.0F) outEpoch= Position.EPOCH1950;
/*
 *                                **** old code ****
 *    if (out_year == 2000.0 ) {
 *       outP.setEpoch(2000.0F);
 *       }
 *    if (out_year == 1950.0 ) {
 *       outP.setEpoch(1950.0F);
 *       }
 */



      ProperMotion inPm= inP.getProperMotion();


      // Equatorial with Proper Motion, it only handles B1950 <=> J2000
      // all other combinations are treated as the input does not have proper
      // motions
      RaDecPM retPM = null;
      if ((in_sys == CoordConv.EQUATORIAL_B ||
           in_sys == CoordConv.EQUATORIAL_J) && inPm != null ) {

         if (in_sys == CoordConv.EQUATORIAL_B &&
            out_sys == CoordConv.EQUATORIAL_J &&
             in_year == 1950.0 && out_year == 2000.0) {
                       retPM= CoordConv.doConvPM(true,
                                         inP.getLon(), inP.getLat(),
                                         inPm.getLonPm(), inPm.getLatPm() );
            }

         if (in_sys == CoordConv.EQUATORIAL_J &&
            out_sys == CoordConv.EQUATORIAL_B &&
             in_year == 2000.0 && out_year == 1950.0) {
                       retPM= CoordConv.doConvPM(false,
                                         inP.getLon(), inP.getLat(),
                                         inPm.getLonPm(), inPm.getLatPm() );
            }
      } // end if convert with proper motion

       ProperMotion outPm= null;
       if(retPM!=null) {
           outPm=new ProperMotion((float) retPM.getRaPM(),
                                  (float) retPM.getDecPM());
           outP=new Position(retPM.getRa(), retPM.getDec(),
                             outPm, coordSystem, outEpoch);
       }
       else {  // no proper motion conversion
           // galactic coordinate conversion requires tobs to be 1950
           // if EQ_J2000 then proper motion is 0,0
           double tobs= (in_sys==CoordConv.GALACTIC) ?
                                Position.EPOCH1950: inP.getEpoch();
           LonLat ret=CoordConv.doConv(in_sys, in_year,
                                       inP.getLon(), inP.getLat(),
                                       out_sys, out_year, tobs);
           if (coordSystem.equals(CoordinateSys.EQ_J2000)) {
               outPm= PositionJ2000.DEFAULT_PM;
           }
           outP=new Position(ret.getLon(), ret.getLat(), outPm,
                             coordSystem, outEpoch);
       }


      return outP;



   }


   static void cloneAllAttributes(Target fromTarget, Target toTarget) {
      Iterator locIter= fromTarget.locationAttributeIterator();
      Iterator tarIter= fromTarget.targetAttributeIterator();

      LocationAttribute la;
      List              list;
      while(locIter.hasNext()) {
         list= (List)locIter.next();
         for(Iterator i= list.iterator(); (i.hasNext()); ) {
               la= (LocationAttribute)i.next();
               la= (LocationAttribute)la.clone();
               toTarget.addLocationAttribute(la);
         }
      }

      TargetAttribute ta;
      while(tarIter.hasNext()) {
         ta= (TargetAttribute)tarIter.next();
         ta= (TargetAttribute)ta.clone();
         toTarget.addTargetAttribute(ta);
      }

   }

   /**
    *  compute the distance between two positions (lon1, lat1)
    *  and (lon2, lat2), the lon and lat are in decimal degrees.
    *  the unit of the distance is degree
    */
   public static double computeDistance(double lon1, double lat1,
                                        double lon2, double lat2) {
      double cosine;
      double lon1Radians, lon2Radians;
      double lat1Radians, lat2Radians;
      lon1Radians  = lon1 * DtoR;
      lat1Radians  = lat1 * DtoR;
      lon2Radians  = lon2 * DtoR;
      lat2Radians  = lat2 * DtoR;
      cosine =
         Math.cos(lat1Radians)*Math.cos(lat2Radians)*
         Math.cos(lon1Radians-lon2Radians)
         + Math.sin(lat1Radians)*Math.sin(lat2Radians);

      if (Math.abs(cosine) > 1.0)
         cosine = cosine/Math.abs(cosine);
      return RtoD*Math.acos(cosine);
   }

   /**
    * Find this distance between two positions.  If the postions do not have
    * the same coordinate system and epoch then convert then both to J2000
    * and then compute the distance. The distance is returned in degrees.
    *
    * @param p1 the first postion
    * @param p2 the second position
    * @return this distance in degrees
    */
   public static double computeDistance(Position p1, Position p2) {
      double retval;
      if (p1.getCoordSystem().equals(p2.getCoordSystem()) &&
          p1.getEpoch()==p2.getEpoch()) {
          retval= computeDistance(p1.getLon(), p1.getLat(),
                                  p2.getLon(), p2.getLat());
      }
      else {
         PositionJ2000 cp1= new PositionJ2000(p1);
         PositionJ2000 cp2= new PositionJ2000(p2);
         retval= computeDistance(cp1.getLon(), cp1.getLat(),
                                 cp2.getLon(), cp2.getLat());
      }
      return retval;
   }

   /**
    *  compute the distance between two fixed single target
    *  the unit of the distance is degree
    */
   public static double computeDistance(TargetFixedSingle t1,
                                        TargetFixedSingle t2) {
      return computeDistance(t1.getPosition(), t2.getPosition());
   }

   /**
    *  compute the maximum distance between all
    *  the positions in a TargetMulti target.
    *  the unit of the distance is degree
    */
   public static double computeMaxDistance(TargetMulti t) {
      PositionJ2000 [] ps = t.getPositionAry();
      int number = ps.length;
      double max = 0.0;
      double dist;

      for (int i = 0; i< number-1; i++) {
         for (int j=i+1; j<number; j++) {
            dist = computeDistance(ps[i], ps[j]);
            if (dist > max) max = dist;
            }
         }
      return max;
   }

   /**
    *  compute the distance between each position in
    *  a TargetMulti target and the position calculated using given offset,
    *  return the maximum.
    *  offsets RA and DEC are in arcsec.
    *  the unit of the distance is degree
    *
    *  assuming that the offset given is in the same coordinate system
    *  as the positions in target
    */
   public static double computeMaxDistance(TargetMulti t, Offset of) {
      PositionJ2000 [] ps = t.getPositionAry();
      Position newP = getNewPosition(ps[0], of);

      int         number = ps.length;
      double      dist = 0.0;
      double      max = 0.0;

      for (int i=0; i<number; i++) {
         dist = computeDistance(newP, ps[i]);
         if (max < dist) max = dist;
         }
      return max;
   }

   /**
    *  compute the distance between each position in
    *  a TargetFixedCluster target and the position calculated using
    *  given offset,
    *  return the maximum.
    *  offsets RA and DEC are in arcsec.
    *  the unit of the distance is degree
    *
    *  asuming the offset given is in the same coordinate system
    *  as the position in target. also the offset in target is in
    *  the same coordinate system as the position
    */
   public static double computeMaxDistance(TargetFixedCluster t, Offset of) {
      TargetMulti  tgtM = toTargetMulti(t);
      return computeMaxDistance(tgtM, of);
   }

   public static double computeMaxDistance(Target t, Offset of) {
      double dist = 0.0;
      if (!t.isFixed())
         throw new IllegalArgumentException(
                        "only fixed target is supported");
      if (t instanceof TargetFixedCluster)
         dist = computeMaxDistance((TargetFixedCluster)t, of);
      else if (t instanceof TargetMulti)
         dist = computeMaxDistance((TargetMulti)t, of);
      else {
         Position p = ((TargetFixedSingle)t).getPosition();
         dist = computeDistance(p, getNewPosition(p, of));
         }

      return dist;
   }

   public static double computeMaxDistance(TargetFixedCluster t,
                                           PositionJ2000 p) {
      TargetMulti tm = toTargetMulti(t);
      return computeMaxDistance(tm, p);

   }
   public static double computeMaxDistance(TargetMulti t, PositionJ2000 p) {
      PositionJ2000 [] ps = t.getPositionAry();

      int         number = ps.length;
      double      dist = 0.0;
      double      max = 0.0;

      for (int i=0; i<number; i++) {
         dist = computeDistance(p.getLon(),p.getLat(),
                                ps[i].getLon(), ps[i].getLat());
         if (max < dist) max = dist;
         }
      return max;
   }
   public static double computeMaxDistance(Target t, PositionJ2000 p) {
      double dist = 0.0;
      if (!t.isFixed())
         throw new IllegalArgumentException(
                        "only fixed target is supported");
      if (t instanceof TargetFixedCluster)
         dist = computeMaxDistance((TargetFixedCluster)t, p);
      else if (t instanceof TargetMulti)
         dist = computeMaxDistance((TargetMulti)t, p);
      else {
         Position tp = ((TargetFixedSingle)t).getPosition();
         dist = computeDistance( p.getLon(),  p.getLat(),
                                 tp.getLon(), tp.getLat());
         }
      return dist;
   }

   /**
    * compare base position for two targets,
    * for sorting in target list and aor list  br>
    * if (t1 < t2)  return -1; <br>
    * else if (t1 > t2) return 1; <br>
    * else return 0;  (this does not mean equal, use equals method
    * if you want to see if two targets are exactly the same)<br>
    * <br>
    * <ol>
    * Definitions as following: <br>
    *  <li> Fixed target is smaller than non-fixed targets;
    *  <li> TargetAny is smaller than moving target
    *  <li> Fixed Single, fixed cluster with offsets and fixed cluster with
    *       multiple positions, only the position(first position
    *       for multiple positions) are compared.
    *  <li> for fixed targets, we take into account the RA/Lon, Dec/Lat
    *  <li> for RA/Lon,from small to large:    0.00  ===> 360.0
    *  <li> for Dec/Lat, -rom small to large: 90.00  ===>  90.00
    *  <li> moving target with standard ephemeris is smaller than
    *     with non-standard ephemeris
    *  <li> for moving tragets with standard ephemeris,
    *       naifID is treated as integer comparison
    *  <li> for non-standard ephemeris, they are considered the same
    * </ol>
    */
   public static int compareBasePosition(Target t1, Target t2) {
       return compareBasePosition(t1, t2, sortFirst.RA);
   }

    public static int compareBasePosition(Target t1, Target t2, sortFirst sort) {
      int retval = -1;
      Position p1, p2;

      if (t1.isFixed()) {
         if (t2.isFixed()) {
            p1 = ((Fixed)t1).getPosition();
            p2 = ((Fixed)t2).getPosition();
             if (sort.equals(sortFirst.DEC)) {
                retval = compareLatLon(p1,p2);
             } else if (sort.equals(sortFirst.RA)) {
                retval = compareLonLat(p1, p2);
             }
            }
         else {
            retval = -1;
            }
         }
      else {   // t1 is TargetAny or moving target
         if (t2.isFixed()) {
            retval = 1;
            }
         else {  // both are moving or TargetAny
            if (t1 instanceof TargetAny && t2 instanceof TargetAny)
               retval = 0;
            else if (t1 instanceof TargetAny)
               retval = -1;
            else if (t2 instanceof TargetAny)
               retval = 1;
            else  if (((Moving)t1).isStandard()) { // both are moving target
               if (((Moving)t2).isStandard()) {
                  Ephemeris eph1 = ((Moving)t1).getEphemeris();
                  Ephemeris eph2 = ((Moving)t2).getEphemeris();
                  retval = compareEphID((StandardEphemeris)eph1,
                                        (StandardEphemeris)eph2);
                  }
               else {
                  retval = -1;
                  }
               }
            else { // t1 is non-standard
               if (((Moving)t2).isStandard())
                  retval = 1;
               else
                  retval = 0;
               }
            }
         }
      return retval;
   }

    /**
     * Compare by Latitute first then by lattitude then by longitude
     * @param p1
     * @param p2
     * @return int
     */
    private static int compareLatLon(Position p1, Position p2) {
        int retval = 0;
      double latD1 = p1.getLat();
      double latD2 = p2.getLat();
      double lonD1 = p1.getLon();
      double lonD2 = p2.getLon();

     if (latD1 < latD2) {
         retval = -1;
      }
      else if (latD1 > latD2) {
         retval = 1;
      }
      else {
         if (lonD1 < lonD2)      retval = -1;
         else if (lonD1 > lonD2) retval = 1;
         else                    retval = 0;
      }
      return retval;
    }

    /**
     * Compare Longitude first then Lattitude
     * @param p1
     * @param p2
     * @return int
     */
   private static int compareLonLat(Position p1, Position p2) {
      int retval = 0;
      double lonD1 = p1.getLon();
      double lonD2 = p2.getLon();
      double latD1 = p1.getLat();
      double latD2 = p2.getLat();

     if (lonD1 < lonD2) {
         retval = -1;
      }
      else if (lonD1 > lonD2) {
         retval = 1;
      }
      else {
         if (latD1 < latD2)      retval = -1;
         else if (latD1 > latD2) retval = 1;
         else                    retval = 0;
      }
      return retval;
   }

   private static int compareEphID(StandardEphemeris eph1,
                                   StandardEphemeris eph2) {
      int retval;
      int id1 = eph1.getNaifID();
      int id2 = eph2.getNaifID();

      if (id1 < id2)      retval = -1;
      else if (id1 > id2) retval = 1;
      else                retval = 0;

      return retval;
   }


  /** Compute position angle
    * @param ra0 the equatorial RA in degrees of the first object
    * @param dec0 the equatorial DEC in degrees of the first object
    * @param ra the equatorial RA in degrees of the second object
    * @param dec the equatorial DEC in degrees of the second object
    * @return position angle in degrees between the two objects
    */
    static public double getPositionAngle(double ra0, double dec0,
        double ra, double dec)
    {
        double alf,alf0,del,del0;
        double sd,sd0,cd,cd0,cosda,cosd,sind,sinpa,cospa;
        double dist;
        double pa;

        alf = ra * DtoR;
        alf0= ra0 * DtoR;
        del = dec * DtoR;
        del0= dec0 * DtoR;

        sd0= Math.sin(del0);
        sd = Math.sin(del);
        cd0= Math.cos(del0);
        cd = Math.cos(del);
        cosda= Math.cos(alf-alf0);
        cosd=sd0*sd+cd0*cd*cosda;
        dist= Math.acos(cosd);
        pa=0.0;
        if(dist > 0.0000004) {
            sind= Math.sin(dist);
            cospa=(sd*cd0 - cd*sd0*cosda)/sind;
            if(cospa>1.0)cospa=1.0;
            if (cospa < -1.0) cospa= -1.0;
            sinpa=cd * Math.sin(alf-alf0)/sind;
            pa=Math.acos(cospa) * RtoD;
            if(sinpa < 0.0) pa = 360.0-(pa);
        }
        dist *= RtoD;
        if(dec0 == 90.) pa = 180.0;
        if(dec0 == -90.) pa = 0.0;

        return(pa);
    }

   /**
    * Convert the passed ofset to a PositionJ2000 if posible.  If the
    * Offset contains a non-null Location that is an instance of
    * Position then call getNewPosition and return a new position that
    * combines the Position and the offset.
    * @param of the offset to try to convert to a PositionJ2000
    * @return return a PositonJ2000 if the convertion is position or a
    *         null if the convertion is not posible
    */
   public static PositionJ2000 convertToPosition(Offset of) {
      PositionJ2000 retval= null;
      Location l= of.getLocation();
      if (l!=null && l instanceof Position) {
         Position p= (Position)l;
         retval= getNewPosition(p,of);
      }
      return retval;
   }
   /**
    * @param p the base postion to start from
    * @param of - the offset in the same coordinate system as the input position
    * @return a new position with offset respective to input position.
    * the new position is in the same coordinate system as the input position.
    * the new psoition has the same proper motion as the input position
    * the new position has J2000 values populated.
    */
   public static PositionJ2000 getNewPosition(Position p, Offset of) {
      PositionJ2000 pos1 = new PositionJ2000(p);
      PositionJ2000 pos2 = PositionUtil.calculatePosition(pos1, of);
      return pos2;
   }

   private static TargetMulti toTargetMulti(TargetFixedCluster t) {
      PositionJ2000 p = t.getPosition();
      Offset []    ofs = t.getOffsets();
      int          number = ofs.length;
      PositionJ2000 []  newP = new PositionJ2000[number+1];

      for (int i=0; i<number; i++) {
         newP[i+1] = getNewPosition(p, ofs[i]);
         }
      newP[0] = p;
      return new TargetMulti(t.getName(), newP);
      }

   private static Position correctJ2000Epoch(Position inP) {
      Position outP;
      ProperMotion pm= inP.getProperMotion();
      if (inP.getCoordSystem()==CoordinateSys.EQ_J2000 && pm!=null &&
          inP.getEpoch()!=Position.EPOCH2000) {
         double correctedRa;
         double correctedDec;
         float  pmRa= pm.getLonPm();
         double tobs   = inP.getEpoch();
         double dec_r  = inP.getLat() * DtoR;
         double cos_dec= Math.cos(dec_r);
         float  pmDec= pm.getLatPm();

         if (Math.abs(cos_dec) > EPS) {
            correctedRa = inP.getLon() +
                          pmRa*(Position.EPOCH2000 - tobs)/(cos_dec*3600.0);
         }
         else {
            correctedRa = inP.getLon() +
                          pmRa*(Position.EPOCH2000  - tobs)/3600.0;
         }
         correctedDec = inP.getLat() +
                        pmDec*(Position.EPOCH2000 - tobs)/3600.0;

         outP= new Position( correctedRa, correctedDec, pm,
                             CoordinateSys.EQ_J2000, Position.EPOCH2000 );
      }
      else {
         outP= inP;
      }
      return outP;
   }

   public static void printPosition(String msg, Position p) {

      System.out.println(msg);
      System.out.println(p.toString());
      }

   //private static void printOffset(String msg, Offset of) {
   //   System.out.println(msg);
   //  System.out.println("user input: "+ of.getDeltaRaV() + ", " +
   //                                    of.getDeltaDecW());
   //}

     // coordName will be one of the following : Equaltorial, Ecliptic,
    // Galactic, equinox will be J2000 or B1950
    // the default will be Equatorial J2000
    public static CoordinateSys makeCoordSys(String coordName, String equinox) {
       CoordinateSys retval;
       if (coordName.equalsIgnoreCase(CoordinateSys.EQUATORIAL_NAME) ) {
          if (equinox.equalsIgnoreCase(CoordinateSys.B1950_DESC))
             retval = CoordinateSys.EQ_B1950;
          else
             retval = CoordinateSys.EQ_J2000;
       }
       else if (coordName.equalsIgnoreCase(CoordinateSys.ECLIPTIC_NAME)) {
          if (equinox.equalsIgnoreCase(CoordinateSys.B1950_DESC))
             retval = CoordinateSys.ECL_B1950;
          else
             retval = CoordinateSys.ECL_J2000;
       }
       else if (coordName.equalsIgnoreCase(CoordinateSys.GALACTIC_NAME)) {
          retval= CoordinateSys.GALACTIC ;
       }
       else
          retval= CoordinateSys.EQ_J2000 ;

       return retval;
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
