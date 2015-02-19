/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
//----------------------- Target Attributes ----------------------------
//======================================================================


    public void addTargetAttribute(TargetAttribute a) {
        targetAttributes.put(a.getName(),a);
    }

    public void removeTargetAttribute(TargetAttribute a) {
       targetAttributes.remove(a.getName());
    }

    public void cleaAllTargetAttributes() {
       targetAttributes.clear();
    }

    public TargetAttribute getTargetAttribute(String name) {
        return (TargetAttribute) targetAttributes.get(name);
    }

    public Iterator targetAttributeIterator() {
        return targetAttributes.values().iterator();
    }

//======================================================================
//----------------------- Location Attributes --------------------------
//======================================================================


    public void addLocationAttribute(LocationAttribute a) {
        List<LocationAttribute> list =  locationAttributes.get(a.getLocation());
        if(list==null) list = new ArrayList<LocationAttribute>(1);
        boolean found= false;
        for(int i = 0; i<list.size() && !found; i++) {
            LocationAttribute attribute =  list.get(i);
            if(attribute.getClass()==a.getClass()) {
                list.set(i, a);
                found= true;
            }
        }
        if (!found) list.add(a);
        locationAttributes.put(a.getLocation(), list);
    }

    public void removeLocationAttribute(LocationAttribute a) {
       List<LocationAttribute> list= locationAttributes.get(a.getLocation());
       if (list!=null) {
           list.remove(a);
       }
    }

    public void clearAllLocationAttributes() {
       locationAttributes.clear();
    }

    public LocationAttribute[] getLocationAttribute(Location l) {
        LocationAttribute lAry[]= null;
        List<LocationAttribute> list= locationAttributes.get(l);
        if (list!=null) {
            lAry= list.toArray(new LocationAttribute[list.size()]);
        }
        return lAry;
    }


    public LocationAttribute getLocationAttribute(Location l, Class c) {
        LocationAttribute la, retval= null;
        List<LocationAttribute> list= locationAttributes.get(l);
        if(list!=null) {
            for(Iterator i = list.iterator(); (i.hasNext() && retval==null);) {
                la = (LocationAttribute) i.next();
                if(la.getClass().equals(c)) retval = la;
            }
        }
        return retval;
    }


    public Iterator locationAttributeIterator() {
        return locationAttributes.values().iterator();
    }

//======================================================================
//----------------------- Abstract Methods -----------------------------
//======================================================================

    public abstract Iterator locationIterator();

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
     * Return the coordinage system plus the equinox
     * @return String the coordinage system plus the equinox
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

    /**
     * Compare this target to another target including the attriubutes.
     * The comparison includes the attriubutes therefore the
     * two targets must have the exact  same attributes.
     * This method has not been fully tested. Please test it and use with caution.
     * @param t the target to compare to
     * @return true is equals, false if not
     */
    public boolean equalsIncludingAttributes(Target t) {
        boolean retval= false;
        if (t==this) {
          retval= true;
        }
        else if (t!=null && equals(t)) {
           boolean ret1 = ComparisonUtil.equals(targetAttributes, t.targetAttributes);
           boolean ret2 = ComparisonUtil.equals(locationAttributes, t.locationAttributes);
          retval= ret1 && ret2;
        }
        return retval;
    }

   /**
    * Compare this target to another target including only the TargetAttriubute.
    * The comparison includes the TargetAttriubute therefore the
    * two targets must have the exact  same TargetAttribute.
    * @param t the target to compare to
    * @return true is equals, false if not
    */
   public boolean equalsIncludingTargetAttributes(Target t) {
      boolean retval= false;
      if (t==this) {
         retval= true;
      }
      else if (t!=null && equals(t)) {
         retval = ComparisonUtil.equals(targetAttributes, t.targetAttributes);
      }
      return retval;
   }

}
