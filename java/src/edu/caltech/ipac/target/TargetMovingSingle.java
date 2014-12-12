package edu.caltech.ipac.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Target Moving Single
 * A single moving target.
 */
public class TargetMovingSingle extends    Moving
                                implements Cloneable, 
                                           Serializable {
    /**
     *  Ephemeris for this moving target
     *        @see Ephemeris
     *        @see StandardEphemeris
     *        @see edu.caltech.ipac.target.NonStandardEphemeris
     */
    private Ephemeris ephemeris;


    /**
     * Initialization constructor
     */
    public TargetMovingSingle( String name, Ephemeris ephemeris ) {
        super(name);
        this.ephemeris = ephemeris;
    }

    /**
     * This property is true if the ephemeris is Non-Standard
     */
    public boolean isNonStandard() {
        return ( ephemeris instanceof NonStandardEphemeris );
    }

    /**
     * This property is true if the ephmeris is Standard
     */
    public boolean isStandard() {
        return  ( ephemeris instanceof StandardEphemeris);
    }


    public int getNaifID() {
       int naifID;
       if (isStandard()) {
	   StandardEphemeris eph= (StandardEphemeris) ephemeris;
	   naifID = eph.getNaifID();
       } else {
	   NonStandardEphemeris eph= (NonStandardEphemeris) ephemeris;
	   naifID= eph.getNaifID();
       }
       return naifID;
    }

    /**
     * Get the ephemeris
     *  @see Ephemeris
     *  @see StandardEphemeris
     *  @see NonStandardEphemeris
     */
    public Ephemeris getEphemeris() {
        return ephemeris;
    }

    /**
     * Set the ephemeris
     *  @see Ephemeris
     *  @see StandardEphemeris
     *  @see NonStandardEphemeris
     */
    public void setEphemeris( Ephemeris ephemeris ) {
        this.ephemeris = ephemeris;
    }

    /**
     * Returns empty string (no target equinox entered by user)
     */
    public String getCoordSysDescription() {
        return "";
    }

    /**
     * Returns the type of target
     */
    public String getType() {
        return "Moving Single";
    }

    /**
     * Get the coordinates for this target. The coordinates is either the
     *  NIAF ID of the target or "Non-Standard Ephemeris".
     *  @see #isStandard
     *  @see #isNonStandard
     *  @see Ephemeris
     *  @see StandardEphemeris
     *  @see NonStandardEphemeris
     */
    public String getCoords() {
        if ( isStandard() ) {
            StandardEphemeris std = ( StandardEphemeris ) ephemeris;
            return std.getNaifID() + "";
        }
        else {
            return "Non-Standard Ephemeris";
        }
    }

    public Iterator locationIterator() {
        List<Location> l= new ArrayList<Location>(1);
        l.add(ephemeris);
        return l.iterator();
    }

    /**
     * Implementation of the cloneable interface
     */
    public Object clone() {
        TargetMovingSingle t= new TargetMovingSingle(getName(), ephemeris);
        TargetUtil.cloneAllAttributes(this,t);
        return t;
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof TargetMovingSingle) {
          TargetMovingSingle t= (TargetMovingSingle)o;
          if (super.equals(t)) {
              retval= ComparisonUtil.equals(ephemeris, t.ephemeris);
          }
       }
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
