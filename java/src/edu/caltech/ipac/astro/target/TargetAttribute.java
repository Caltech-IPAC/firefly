package edu.caltech.ipac.astro.target;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

public abstract class TargetAttribute implements Attribute,
                                                 Cloneable, 
                                                 Serializable {

    private final String name;
    

    public TargetAttribute(String name) { this.name = name;}

    public String getName() {return name; }

    public abstract Object clone();

    public boolean equals(Object o) {
       boolean retval= false;
       if (o==this) {
          retval= true;
       }
       else if (o!=null && o instanceof TargetAttribute) {
          TargetAttribute a= (TargetAttribute)o;
          retval= ComparisonUtil.equals(name, a.name);
       }
       return retval;
    }
}
