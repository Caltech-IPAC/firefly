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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
