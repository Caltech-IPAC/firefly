/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.io.Serializable;


/**
 * Base class for all points
 */

public class Pt implements Serializable {
   private final double x;
   private final double y;

   public Pt(double x, double y) {
      this.x = x;
      this.y = y;
   }

   public double getX() { return x; }
   public double getY() { return y; }

   public boolean equals(Object o) {
      boolean retval= false;
      if (o instanceof Pt) {
         Pt p= (Pt)o;
         if (getClass() == p.getClass() &&
             x == p.x &&
             y == p.y) {
                 retval= true;
         } // end if
      }
      return retval;
   }

    public String toString() { return getX()+";"+getY(); }

    public static Pt parse(String serString) {
        if (serString==null) return null;
        double x;
        double y;
        Pt pt;
        try {
            String[] sAry= serString.split(";");
            if (sAry.length!=2) return null;

            x = parseDouble(sAry[0]);
            y = parseDouble(sAry[1]);
            pt= new Pt(x,y);
        } catch (NumberFormatException e) {
            pt= null;
        }

        return pt;
    }

    @Override
    public int hashCode() { return toString().hashCode(); }

    public static double parseDouble(String s) throws NumberFormatException {
        return s.equals("NaN") ? Double.NaN : Double.parseDouble(s);
    }

}
