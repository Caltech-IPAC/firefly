package edu.caltech.ipac.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A group of Fix Single target thought of as one Target
 *
 */
public class TargetGroup extends    Fixed
                         implements Cloneable, 
                                    Serializable {
    /**
     * target positions @serial
     */
   private TargetFixedSingle targets[];

   public TargetGroup() {
      this(null,null);
   }

    /**
     * Initialization constructor
     */
    public TargetGroup( String name, TargetFixedSingle[] targets ) {
        super.setName( name );
        setTargets(targets);
    }

    /**
     * Return the target type
     */
    public String getType() {
        return "Target Group";
    }

    /**
     * Return the coordinates for the target
     */
    public String getCoords() {
        String retval= "";
        if (targets !=null &&
            targets.length>0 &&
            targets[0]!=null &&
            targets[0].getPosition()!=null) {
            UserPosition up= targets[0].getPosition().getUserEnteredPosition();
            retval= up.getUserLonStr() + "," + up.getUserLatStr();
        }
        return retval;
    }

    /**
     * Return the position for the target
     */
    public PositionJ2000 getPosition() {
        PositionJ2000  retval= null;
        if (targets !=null &&
            targets.length>0 &&
            targets[0]!=null &&
            targets[0].getPosition()!=null) {
            retval= targets[0].getPosition();
        }
        return retval;
    }

    /**
     * Return the position for the target
     */
    public TargetFixedSingle[] getTargets() {
        return targets;
    }

    /**
     * Set the position for the target
     */
    public void setTargets( TargetFixedSingle targets[]) {
        this.targets = new TargetFixedSingle[targets.length];
        System.arraycopy(targets,0, this.targets,0,targets.length);
    }

    /**
     * Returns target equinox entered by user
     */
    public String getCoordSysDescription() {
        String retval= null;

        if (targets !=null &&
            targets.length>0 &&
            targets[0]!=null &&
            targets[0].getPosition()!=null) {
            UserPosition up= targets[0].getPosition().getUserEnteredPosition();
            up.getCoordSystem().toString();
        }
        return retval;
    }

    public Iterator locationIterator() {
        List<Position> l= new ArrayList<Position>(targets.length);
        for(int i=0; (i< targets.length); i++) l.add(targets[i].getPosition());
        return l.iterator();
    }


    /**
     * Implementation of the cloneable interface
     */
    public Object clone() {
        TargetGroup t= new TargetGroup(getName(), targets);
        TargetUtil.cloneAllAttributes(this,t);
        return t;
    }

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof TargetGroup) {
          TargetGroup t= (TargetGroup)o;
          if (super.equals(t)) {
              retval= ComparisonUtil.equals(targets, t.targets);
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
