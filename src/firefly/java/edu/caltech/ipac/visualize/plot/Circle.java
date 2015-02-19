/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

 /* This class contains a circle */
public class Circle implements Serializable {
   private WorldPt center;
   private double radius;
    
   public Circle() {  }
   public Circle(WorldPt _center, double _radius) 
   {
       center = _center;
       radius = _radius;
   } 


   public WorldPt getCenter() { return center; }
   public double getRadius() { return radius; }

     public String toString() {
         return  "ra: " +center.getLon() +
                 ", dec: " +center.getLat() +
                 ", radius: " +radius;
     }

     @Override
     public boolean equals(Object other) {
         boolean retval= false;
         if (other==this) {
             retval= true;
         }
         else if (other!=null && other instanceof Circle) {
             Circle r= (Circle)other;
             if (radius==r.radius && ComparisonUtil.equals(center,r.center)) {
                 retval= true;
             }
         }
         return retval;
     }


}
