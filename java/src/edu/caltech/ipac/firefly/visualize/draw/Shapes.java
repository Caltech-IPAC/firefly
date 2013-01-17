package edu.caltech.ipac.firefly.visualize.draw;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
/**
 * User: roby
 * Date: Nov 10, 2009
 * Time: 5:08:08 PM
 */


/**
 * @author Trey Roby
 */
public class Shapes implements Iterable<Shape> {

    private final Shape[] _sAry;

    public Shapes() { _sAry= new Shape[] {};}

    public Shapes(Shape[] sAry) { _sAry= sAry;}

    public Shapes(Shape s) {
        if (s==null) {
            _sAry= new Shape[] {};
        }
        else {
            _sAry= new Shape[] {s};
        }
    }

    public Shapes(List<Shape> sList) {
        _sAry= sList.toArray(new Shape[sList.size()]);
    }

    public Shapes concat(Shapes other) {
        Shapes retval= this;
        if (other!=null) {
            Shape[] newAry = new Shape[_sAry.length + other._sAry.length];
            System.arraycopy(_sAry,0,newAry,0,_sAry.length);
            System.arraycopy(other._sAry,0,newAry,_sAry.length,other._sAry.length);
            retval= new Shapes(newAry);
        }
        return retval;
    }

    public Shapes concat(Shape shape) {
        Shapes retval= this;
        if (shape!=null) {
            Shape[] newAry = new Shape[_sAry.length + 1];
            System.arraycopy(_sAry,0,newAry,0,_sAry.length);
            newAry[_sAry.length]= shape;
            retval= new Shapes(newAry);
        }
        return retval;
    }

    public Iterator<Shape> iterator() {
        return Arrays.asList(_sAry).iterator();
    }

    Shape[] getShapes() { return _sAry; }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
