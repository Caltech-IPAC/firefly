package edu.caltech.ipac.astro.target;


import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.astro.conv.LonLat;
import edu.caltech.ipac.astro.conv.RaDecPM;

import java.text.NumberFormat;

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


   static {
      nf.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
      nf.setMinimumFractionDigits(MAX_FRACTION_DIGITS);
      }

//   public static String convertLonToString(double        lon,
//                                           CoordinateSys coordSystem )
//                                                 throws CoordException {
//       return convertLonToString(lon,coordSystem.isEquatorial());
//   }

   public static String convertLonToString(double lon, boolean isEquatorial )
                   throws CoordException {
       int convertionType= isEquatorial ? 2 : 0;
       return CoordUtil.dd2sex(lon, false, isEquatorial, 5);
    }

//   public static String convertLatToString(double        lat,
//                                           CoordinateSys coordSystem )
//                                                  throws CoordException {
//       return convertLatToString(lat,coordSystem.isEquatorial());
//   }

    public static String convertLatToString(double lat, boolean isEquatorial)
                                                  throws CoordException {
        return CoordUtil.dd2sex(lat, true, isEquatorial, 5);
    }

   public static double convertStringToLon(String        hms,
                                      CoordinateSys coordSystem )
                                                  throws CoordException {
       boolean eq= coordSystem.isEquatorial();
       return CoordUtil.sex2dd(hms,false, eq);
   }

   public static double convertStringToLat(String        dms,
                                           CoordinateSys coordSystem )
                                                  throws CoordException {
       boolean eq= coordSystem.isEquatorial();
       return CoordUtil.sex2dd(dms,true, eq);
   }

    public static String convertLonToString(double lon, edu.caltech.ipac.visualize.plot.CoordinateSys coordSystem) throws CoordException {
        return convertLonToString(lon,coordSystem.isEquatorial());
    }
    public static double convertStringToLon(String        hms,
                                            edu.caltech.ipac.visualize.plot.CoordinateSys coordSystem ) throws CoordException {
        boolean eq= coordSystem.isEquatorial();
        return CoordUtil.sex2dd(hms,false, eq);
    }

    public static String convertLatToString(double        lat,
                                            edu.caltech.ipac.visualize.plot.CoordinateSys coordSystem )
            throws CoordException {
        return convertLatToString(lat,coordSystem.isEquatorial());
    }

    public static double convertStringToLat(String        dms,
                                            edu.caltech.ipac.visualize.plot.CoordinateSys coordSystem ) throws CoordException {
        boolean eq= coordSystem.isEquatorial();
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
