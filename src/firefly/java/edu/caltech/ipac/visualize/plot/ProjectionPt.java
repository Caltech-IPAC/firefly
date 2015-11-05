/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;


import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

/**
 * A ProjectionPt has its origin (0,0) at the CENTER of the lower left pixel
 * This point is offset from ImagePt by 1/2 pixel
 */
@JsExport
@JsType
public final class ProjectionPt extends Pt {
   public ProjectionPt() { this(0,0); }
   public ProjectionPt(double x, double y) { super(x,y); }

   public double getFsamp() { return getX(); }
   public double getFline() { return getY(); }

   public String toString() {
       return "x : " + getX() + "   y :" + getY();
   }
}

