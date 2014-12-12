package edu.caltech.ipac.target;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;

/**
 * This subclass of Position add three things to Postion
 *  <ul>
 *  <li>The Position is always in J2000
 *  <li>The Epoch is always 2000
 *  <li>It carries the original position that is was constructed with.
 *
 */
public final class PositionJ2000 extends Position {

    public final static ProperMotion DEFAULT_PM= new ProperMotion(0,0);

    private final UserPosition userEnteredPos;

    /**
     * @param ra ra in degrees
     * @param dec dec in degrees
     */
    public PositionJ2000( double ra, double dec) {
        this(ra,dec,DEFAULT_PM);
    }

    /**
     * @param ra ra in degrees
     * @param dec dec in degrees
     * @param pm the proper motion, null if position contains no proper motion
     */
    public PositionJ2000( double        ra,
                          double        dec,
                          ProperMotion  pm) {
        super(ra,dec, pm==null? DEFAULT_PM : pm,
              CoordinateSys.EQ_J2000, EPOCH2000);
        Assert.argTst(pm!=null, "Proper Motion should not be null with J2000");
        userEnteredPos = null;
    }



    /**
     * Construct a Position based on a position in another coordinate system.
     * Do the converation to a J2000. Save this positon for access later.
     * @param userEnteredPos the positon in another coordinate system
     */
    public PositionJ2000(UserPosition userEnteredPos) {
       super( TargetUtil.convertTo(userEnteredPos, CoordinateSys.EQ_J2000) );
       this.userEnteredPos = userEnteredPos;
    }

    /**
     * Construct a Position based on a position in another coordinate system.
     * Do the converation to a J2000. Save this positon for access later.
     * @param pos the positon in another coordinate system
     */
    public PositionJ2000(Position pos) {
       super( TargetUtil.convertTo(pos, CoordinateSys.EQ_J2000) );
       UserPosition up= null;
       try {
           up= new UserPosition(pos.convertLonToString(),
                                pos.convertLatToString(),
                                pos.getProperMotion(), pos.getCoordSystem(),
                                pos.getEpoch());
       } catch (CoordException e) { }
       userEnteredPos = up;
    }


    /**
     * Get the Position that this PositionJ2000 was constructed with.  If it
     * was not constructed with a UserPosition or a Position with form another
     * coordinate system then return a UserPosition that is in J2000
     * @return UserPosition the user position that was used to construct this
     *                      PositionJ2000 or a generated UserPosition
     */
    public UserPosition getUserEnteredPosition() {
       UserPosition retval= userEnteredPos;
       if (retval==null) {
           try {
               retval= new UserPosition( getLon()+"d", getLat()+"d",
                                         getProperMotion(),
                                         CoordinateSys.EQ_J2000, EPOCH2000);
           } catch (CoordException e) {
               Assert.tst(false,
                 "It should not be posible to get to this point in the code.");
               retval= null;
           }
       }
       return retval;
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


    public Object clone() {
       PositionJ2000 retval= null;
       if ( userEnteredPos ==null) {
           retval= new PositionJ2000(getLon(), getLat(),  getProperMotion());
       }
       else {
           retval= new PositionJ2000(userEnteredPos);
       }
       return retval;
    }


    

    public boolean equals(Object o) {
       boolean retval= false;

       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof PositionJ2000) {
          retval= super.equals(o);
          if (retval) {
                PositionJ2000 p= (PositionJ2000)o;
                retval= ComparisonUtil.equals( getUserEnteredPosition(),
                                                p.getUserEnteredPosition());
          }

       }
       return retval;
    };

    public String toString() {
       return "J2000 Position: \n" + super.toString() + 
              "\nOriginal User Postion: \n" + 
              getUserEnteredPosition().toString();

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
