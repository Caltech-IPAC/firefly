/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.visualize.plot.Pt;

/**
 */
public final class OffsetScreenPt extends Pt {

    public OffsetScreenPt(int x, int y) { super(x,y); }

    public boolean equals(Object o) {
        if (o==this) {
            return true;
        }
        else if (o instanceof OffsetScreenPt p) {
            return (int) getX() == (int) p.getX() && (int) getY() == (int) p.getY();
        }
        return false;
    }

    public static OffsetScreenPt parse(String serString) {
        Pt p= Pt.parse(serString);
        return p==null ? null : new OffsetScreenPt((int)p.getX(),(int)p.getY());
    }
}