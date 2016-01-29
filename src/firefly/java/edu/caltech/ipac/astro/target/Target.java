/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * *******************************************************************
 * BASE class for target information.
 */
public abstract class Target implements Cloneable, Serializable {

    /**
     * Target Name (max 32 chars) @serial
     */
    private String name;
    private Map<String,TargetAttribute> targetAttributes = new HashMap<String,TargetAttribute>(4);
    private Map<Location,List<LocationAttribute>> locationAttributes = new HashMap<Location,List<LocationAttribute>>(4);


    /**
     * Default constructor
     */
    public Target() { this(""); }

    public Target(String name) { 
         this.name = name;
    }


    /**
     * Returns 'name' value -- target name (max 32 chars)
     */
    public String getName() { return name; }

    /**
     * Sets 'name' value. (target name (max 32 chars))
     */
    public void setName( String name ) { this.name = name; }


//======================================================================
//----------------------- Abstract Methods -----------------------------
//======================================================================

    /**
     * Returns 'type' value -- 
     * target type: "Fixed Single", 
     * "Fixed Cluster-Offsets", "Fixed Cluster-Positions" (max 32 chars)
     */
    public abstract String getType();

    /**
     * Returns 'coords' value -- target coordinates 
     * (for user reference) (max 32 chars)
     */
    public abstract String getCoords();

    /**
     * Return the coordinate system plus the equinox
     * @return String the coordinate system plus the equinox
     */
    public abstract String getCoordSysDescription();


    public abstract Object clone();

//======================================================================
//----------------------- End Abstract Methods -------------------------
//======================================================================


    /**
     * Compare this target to another target.  In this method target can
     * be equals even if it has different attributes
     * @param o the object to compare to
     * @return true is equals, false if not
     */
    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof Target) {
          Target t= (Target)o;
          retval=  ComparisonUtil.equals(name,t.name);
       }
       return retval;
    }


}
