package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.Pt;

/**
 */
public final class ScreenPt extends Pt {

    public ScreenPt() { this(0,0); }

    public ScreenPt(int x, int y) { super(x,y); }
    private ScreenPt(Pt p) { this((int)p.getX(),(int)p.getY()); }

    public int getIX() { return (int)getX(); }
    public int getIY() { return (int)getY(); }

    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o instanceof ScreenPt) {
            ScreenPt p= (ScreenPt)o;
            if (getIX()== p.getIX() && getIY()== p.getIY()) {
                retval= true;
            } // end if
        }
        return retval;
    }

    public static ScreenPt parse(String serString) {
        Pt p= Pt.parse(serString);
        return p==null ? null : new ScreenPt(p);
    }

}
