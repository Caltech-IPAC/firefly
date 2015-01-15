/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 */
public class TargetFixedSingle extends    Target
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
