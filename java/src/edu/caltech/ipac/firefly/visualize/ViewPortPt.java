package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.Pt;

/**
 */
public class ViewPortPt extends Pt {

    public ViewPortPt() { this(0,0); }

   public ViewPortPt(int x, int y) {
       super(x,y);
   }

    public int getIX() { return (int)getX(); }
    public int getIY() { return (int)getY(); }

   public String toString() {
       return "x : " + getIX() + "   y:" + getIY();
   }


    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o instanceof ViewPortPt) {
            ViewPortPt p= (ViewPortPt)o;
            if (getIX()== p.getIX() && getIY()== p.getIY()) {
                retval= true;
            } // end if
        }
        return retval;
    }

}
