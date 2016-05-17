/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

import java.io.Serializable;


/**
 * Base class for all points
 */

@JsExport
@JsType
public class Pt implements Serializable {
   private double _x;
   private double _y;


   public Pt() {this(0,0);}


   public Pt(double x, double y) {
      _x= x;
      _y= y;
   }

   public double getX() { return _x; }
   public double getY() { return _y; }

   public boolean equals(Object o) {
      boolean retval= false;
      if (o instanceof Pt) {
         Pt p= (Pt)o;
         if (getClass() == p.getClass() &&
             _x         == p._x         &&
             _y         == p._y) {
                 retval= true;
         } // end if
      }
      return retval;
   }

    public String toString() {
        return getX()+";"+getY();
    }


    public static Pt parse(String serString) {
        if (serString==null) return null;
        double x;
        double y;
        Pt pt;
        try {
            String sAry[]= serString.split(";");
            if (sAry.length!=2) return null;

            x = parseDouble(sAry[0]);
            y = parseDouble(sAry[1]);
            pt= new Pt(x,y);
        } catch (NumberFormatException e) {
            pt= null;
        }

        return pt;
    }

    public String serialize() { return toString(); }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    //these methods should not be used except in special circumstances
    protected void setX(double x) { _x= x; }
    protected void setY(double y) { _y= y; }

    public static double parseDouble(String s) throws NumberFormatException {
        return s.equals("NaN") ? Double.NaN : Double.parseDouble(s);
    }

}
