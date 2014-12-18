package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

public abstract class LocationAttribute implements Attribute,
                                                   Cloneable, 
                                                   Serializable {

    private final Location location;
    

    public LocationAttribute(Location location) { this.location = location;}

    public Location getLocation() {return location; }

    public abstract Object clone();


    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof LocationAttribute) {
          LocationAttribute a= (LocationAttribute)o;
          retval= ComparisonUtil.equals(location, a.location);
       }
       return retval;
    }

}



