package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 */
public class TargetFixedSingle extends    Fixed
                               implements Cloneable, Serializable {
    // class variable definitions //

    /**
     * observation position
     * @serial
     */
    private PositionJ2000 position;

    public TargetFixedSingle() {
       this(null,null);
    }

    public TargetFixedSingle( String name, PositionJ2000 position) {
        super(name);
        this.position = position;
    }



    /**
     * Returns 'type' value -- target type: "Fixed Single" (max 32 chars)
     */
    public String getType() {
        return "Fixed Single";
    }

    /**
     * Returns 'coords' value -- 
     * target coordinates (for user reference) (max 32 chars)
     */
    public String getCoords() {
        String retval= "";
        if (position !=null) {
           retval = position.getUserEnteredPosition().getUserLonStr()+","+
                    position.getUserEnteredPosition().getUserLatStr();
        }
        return retval;
    }

    /**
     * Get the position of the target
     */
    public PositionJ2000 getPosition() {
        return position;
    }

    /**
     * Set the position of the target
     */
    public void setPosition( PositionJ2000 position ) {
        this.position = position;
    }

    /**
     * Returns target equinox entered by user
     */
    public String getCoordSysDescription() {
        String retval= null;
        if (position !=null) {
           retval=
              position.getUserEnteredPosition().getCoordSystem().toString();
        }
        return retval;
    }

    
    public Iterator locationIterator() {
        List<Location> l= new ArrayList<Location>(1);
        l.add(position);
        return l.iterator();
    }


    /**
     * Implementation of the cloneable interface
     */
    public Object clone() {
        TargetFixedSingle t= new TargetFixedSingle(getName(), position);
//        TargetUtil.cloneAllAttributes(this,t);
        return t;
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof TargetFixedSingle) {
          TargetFixedSingle t= (TargetFixedSingle)o;
          if (super.equals(t)) {
              retval= ComparisonUtil.equals(position, t.position);
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
