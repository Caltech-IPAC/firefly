package edu.caltech.ipac.target.validate;

import edu.caltech.ipac.util.Assert;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.target.Position;
import edu.caltech.ipac.target.Offset;
import edu.caltech.ipac.target.TargetFixedSingle;
import edu.caltech.ipac.target.TargetFixedCluster;
import edu.caltech.ipac.target.TargetMovingSingle;
import edu.caltech.ipac.target.TargetMovingCluster;
import edu.caltech.ipac.target.TargetMulti;
import edu.caltech.ipac.target.Ephemeris;
import edu.caltech.ipac.target.StandardEphemeris;
import edu.caltech.ipac.target.NonStandardEphemeris;
import edu.caltech.ipac.util.action.FloatAction;
import edu.caltech.ipac.util.action.DoubleAction;
import edu.caltech.ipac.util.action.OutOfRangeException;
import edu.caltech.ipac.util.action.Prop;
import edu.caltech.ipac.target.TargetUtil;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.ProperMotion;
import edu.caltech.ipac.target.CoordinateSys;


public class TargetValidator {
  private FloatAction lonPm;
  private FloatAction latPm;
  private FloatAction epoch;
  private DoubleAction offsetRa;
  private DoubleAction offsetDec;
  //private FloatAction _movingEpoch;
  //private DoubleAction _t;
  private DoubleAction i;
  private DoubleAction e;
  private DoubleAction q;
  private DoubleAction littleOmega;
  private DoubleAction bigOmega;

  private static TargetValidator instance = null;
  //private static final String INVALID_COORDSYS = "Invalid Coordinate System: ";
  //private static final String WRONG_TARGET =
  //           "The target type is not valid with an MIPS photometry AOT";

  static {
     ValidatePropertyLoader.loadTargetProperties();
     }
  /*
  */
  public TargetValidator() {
     lonPm = new FloatAction("PositionPanel.pmRa");
     latPm = new FloatAction("PositionPanel.pmDec");
     epoch = new FloatAction("PositionPanel.epoch");
     offsetRa = new DoubleAction("Offsets.ra");
     offsetDec = new DoubleAction("Offsets.dec");
     //_movingEpoch= new FloatAction("NonStandardEphemeris.epoch");
     //_t          = new DoubleAction("NonStandardEphemeris.t");
     i = new DoubleAction("NonStandardEphemeris.i");
     e = new DoubleAction("NonStandardEphemeris.e");
     q = new DoubleAction("NonStandardEphemeris.q");
     littleOmega = new DoubleAction("NonStandardEphemeris.littleOmega");
     bigOmega = new DoubleAction("NonStandardEphemeris.bigOmega");
  }


  public static TargetValidator getTargetValidator() {
       if (instance == null) instance = new TargetValidator();
       return instance;
  }

  public void validate(Target target) throws InvalidTargetException {
     PositionJ2000 posn, posns[];
     Offset [] offsets;
     Ephemeris eph;
     String name = target.getName().trim();
     if (name.length() == 0)
        throw new InvalidTargetException(
            AppProperties.getProperty("Target.name.Error"));
     if (target instanceof TargetFixedSingle){
        posn = ((TargetFixedSingle)target).getPosition();
	positionValidate(posn);
	}
     else if (target instanceof TargetFixedCluster) {
        posn = ((TargetFixedCluster)target).getPosition();
        offsets = ((TargetFixedCluster)target).getOffsets();
	//String   s = posn.getCoordSystem();
	//String equ = posn.getEquinoxYear();
	//String errString = AppProperties.getProperty("FixedCluster.noJ2000.Error");
	//if (!isEquatorial(s) || !isJ2000(equ))
	//   throw new InvalidTargetException(target.getName() + ": "
	//                                    + errString);
	positionValidate(posn);
	offsetsValidate(offsets);
	}
     else if (target instanceof TargetMulti) {
        posns = ((TargetMulti)target).getPositionAry();
	for (int i=0; i<posns.length; i++)
	   positionValidate(posns[i]);
	double dist = TargetUtil.computeMaxDistance((TargetMulti)target);
	double maxDist = Prop.getDoubleValue("FixedMulti.maxDistance");
	//System.out.println("Multi position Target Distance = " +dist);
	//System.out.println("Right MAX Distance = " +maxDist);
	if (dist > maxDist) { 
       	   String err =
	      AppProperties.getProperty("FixedMulti.maxDistance.Error");
	   throw new InvalidTargetException(err);
	   }
	}
     else if (target instanceof TargetMovingSingle){
        eph = ((TargetMovingSingle)target).getEphemeris();
        ephValidate(eph);
	}
     else if (target instanceof TargetMovingCluster) {
        eph = ((TargetMovingCluster)target).getEphemeris();
        ephValidate(eph);
        offsets = ((TargetMovingCluster)target).getOffsets();
        offsetsValidate(offsets);
	}

  }

  public void positionValidate(PositionJ2000 posn) throws InvalidTargetException {
     try {
        //validateSystem(posn);
        if (posn.getProperMotion()!=null) {
          ProperMotion pm= posn.getProperMotion();
          lonPm.validate(pm.getLonPm());
	      latPm.validate(pm.getLatPm());
        }
        if (posn.getCoordSystem() == CoordinateSys.EQ_B1950) {
	       epoch.validate(posn.getEpoch() );
	    }
	} catch (OutOfRangeException e) {
         throw new InvalidTargetException(e.getMessage());
    }
  }

   public void ephValidate(Ephemeris eph)
                  throws InvalidTargetException {

      if(eph instanceof NonStandardEphemeris) {
         NonStandardEphemeris noStdEph = (NonStandardEphemeris) eph;
         try {
            i.validate(noStdEph.getI());
            e.validate(noStdEph.getE());
            q.validate(noStdEph.getQ());
            littleOmega.validate(noStdEph.getLittleOmega());
            bigOmega.validate(noStdEph.getBigOmega());
         } catch (OutOfRangeException e) {
            throw new InvalidTargetException(e.getMessage());
         }
      }
   }

   public void offsetsValidate(Offset offsets[])
                                             throws InvalidTargetException {
      int length = offsets.length;

      try {
         for(int i = 0; i<length; i++) {
            offsetRa.validate(offsets[i].getDeltaRaV());
            offsetRa.validate(offsets[i].getDeltaDecW());
         }
      } catch (OutOfRangeException e) {
         throw new InvalidTargetException(e.getMessage());
      }
   }

  /**
   if the coordinate system is equatorial/ecliptic, 
      then the equinox has to be J2000 or B1950. Otherwise equinox is invalid.
   if the coordinat esystem is galactic, then the equinox has to be null.
   if 
   */
//  private void validateSystem(Position posn) throws
//                               InvalidTargetException{
//     String   s = posn.getCoordSystem();
//     String equ = posn.getEquinoxYear();
//     String [] coordStr = new String[3];
//
//     boolean validCS = false;
//     coordStr[0] = CoordSystemEquinox.EQUATORIAL;
//     coordStr[1]= CoordSystemEquinox.GALACTIC;
//     coordStr[2] = CoordSystemEquinox.ECLIPTIC;
//
//     for (int i=0; i<3; i++) {
//	if (s.substring(0, 2).equalsIgnoreCase(coordStr[i].substring(0,2))) {
//	   s = coordStr[i];
//	   validCS = true;
//	   }
//	}
//     if (!validCS)
//	throw new InvalidTargetException(INVALID_COORDSYS +s);
//     if (isGalactic(s)) {
//	if (equ != null && !StringUtil.isSpaces(equ))
//	    throw new InvalidTargetException(INVALID_COORDSYS +s+ " "+equ);
//	}
//     else {
//	if (!equ.equalsIgnoreCase(CoordSystemEquinox.B1950) &&
//	    !equ.equalsIgnoreCase(CoordSystemEquinox.J2000) )
//	    throw new InvalidTargetException(INVALID_COORDSYS +s+ " "+equ);
//	}
//     posn.setCoordSystem(s);
//  }

  /* factory methods to make properly initialized targets */

  public Position makePosition() {
       Assert.tst(false, "no longer used");
       return null;
  }

  public Position [] makePosition(int size) {
     Assert.tst(false, "no longer used");
     return null;
     //Position [] p =  new Position[size];
     //for (int i=0; i<size; i++) {
	//p[i] =  new Position();
	//p[i].setEpoch(epoch.getDefault());
  }

  public Offset makeOffset() {
     Offset  offset = new Offset(null,
                                 offsetRa.getValue(),
                                 offsetDec.getValue());
     return offset;
  }
  public Offset[] makeOffset(int size) {
     Offset [] offsets = new Offset[size];
     for (int i=0; i<size; i++) {
        offsets[i] = makeOffset();
        }
     return offsets;
  }

  public StandardEphemeris makeStandardEphemeris() {
     Assert.tst("should not use");
     return null;
     //StandardEphemeris eph = new StandardEphemeris();
     //return eph;
  }

  public  NonStandardEphemeris makeNonStandardEphemeris() {
     NonStandardEphemeris eph = new NonStandardEphemeris(null, null,
                              e.getValue(), q.getValue(), i.getValue(),
                              littleOmega.getValue(), bigOmega.getValue());
     return eph;
  }

  public TargetFixedSingle makeTargetFixedSingle() {
     TargetFixedSingle tgt = new TargetFixedSingle();
     return tgt;
  }

  public TargetFixedCluster makeTargetFixedCluster() {
     TargetFixedCluster tgt = new TargetFixedCluster();
      return tgt;
   }


  public TargetMulti makeTargetMulti(int size) {
     TargetMulti tgt = new TargetMulti();
     return tgt;
  }

  public TargetMovingSingle makeTargetMovingSingleStandard() {
     Assert.tst("should not use");
     return null;
     //TargetMovingSingle tgt = new TargetMovingSingle();
     //StandardEphemeris eph = makeStandardEphemeris();
     //tgt.setEphemeris(eph);
     //return tgt;
  }

  public TargetMovingSingle makeTargetMovingSingleNonStandard() {
     Assert.tst("should not use");
     return null;
     //TargetMovingSingle tgt = new TargetMovingSingle();
     //NonStandardEphemeris eph = makeNonStandardEphemeris();
     //tgt.setEphemeris(eph);
     //return tgt;
  }
  public TargetMovingCluster makeTargetMovingClusterStandard() {
     TargetMovingCluster tgt = new TargetMovingCluster();
     return tgt;
  }
  public TargetMovingCluster makeTargetMovingClusterNonStandard() {
     TargetMovingCluster tgt = new TargetMovingCluster();
     return tgt;
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
