/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.Pt;

/**
 */
public final class OffsetScreenPt extends Pt {

    public OffsetScreenPt() { this(0,0); }

    public OffsetScreenPt(int x, int y) { super(x,y); }
    private OffsetScreenPt(Pt p) { this((int)p.getX(),(int)p.getY()); }

    public int getIX() { return (int)getX(); }
    public int getIY() { return (int)getY(); }

    public boolean equals(Object o) {
        boolean retval= false;
        if (o==this) {
            retval= true;
        }
        else if (o instanceof OffsetScreenPt) {
            OffsetScreenPt p= (OffsetScreenPt)o;
            if (getIX()== p.getIX() && getIY()== p.getIY()) {
                retval= true;
            } // end if
        }
        return retval;
    }

    public static OffsetScreenPt parse(String serString) {
        Pt p= Pt.parse(serString);
        return p==null ? null : new OffsetScreenPt(p);
    }

}
