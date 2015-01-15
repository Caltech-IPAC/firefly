/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

/**
 */
public final class ViewPortPtMutable extends ViewPortPt {

    public ViewPortPtMutable() { this(0,0); }

   public ViewPortPtMutable(int x, int y) {
       super(x,y);
   }

    public void setX(double x) { super.setX(x); }
    public void setY(double y) { super.setY(y); }

}
