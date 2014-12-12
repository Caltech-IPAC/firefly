package edu.caltech.ipac.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Multiple target class representation.
 *
 */
public class TargetMulti extends    Fixed
                         implements Cloneable, 
                                    Serializable {
    /**
     * target positions @serial
     */
    private PositionJ2000 position[];

   public TargetMulti() {
      this(null,null);
   }

    /**
     * Initialization constructor
     */
    public TargetMulti( String name, PositionJ2000[] position ) {
        super.setName( name );
        this.position = position;
    }

    /**
     * Return the target type
     */
    public String getType() {
        return "Fixed Cluster-Positions";
    }

    /**
     * Return the coordinates for the target
     */
    public String getCoords() {
        String retval= "";
        if (position !=null && position.length>0 && position[0]!=null) {
            retval= position[0].getUserEnteredPosition().getUserLonStr() +
                    "," +
                    position[0].getUserEnteredPosition().getUserLatStr();
        }
        return retval;
    }

    /**
     * Return the position for the target
     */
    public PositionJ2000 getPosition() {
        PositionJ2000  retval= null;
        if (position !=null && position.length>0 && position[0]!=null) {
            retval= position[0];
        }
        return retval;
    }

    /**
     * Return the position for the target
     */
    public PositionJ2000[] getPositionAry() {
        return position;
    }

    /**
     * Set the position for the target
     */
    public void setPositionAry( PositionJ2000[] position ) {
        this.position = position;
    }

    /**
     * Returns target equinox entered by user
     */
    public String getCoordSysDescription() {
        String retval= null;
        if (position !=null && position.length>0 && position[0]!=null) {
           retval=
             position[0].getUserEnteredPosition().getCoordSystem().toString();

        }
        return retval;
    }

    public Iterator locationIterator() {
        List<Location> l= new ArrayList<Location>(position.length);
        for(int i=0; (i< position.length); i++) l.add(position[i]);
        return l.iterator();
    }


    /**
     * Implementation of the cloneable interface
     */
    public Object clone() {
        TargetMulti t= new TargetMulti(getName(), position);
        TargetUtil.cloneAllAttributes(this,t);
        return t;
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof TargetMulti) {
          TargetMulti t= (TargetMulti)o;
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
