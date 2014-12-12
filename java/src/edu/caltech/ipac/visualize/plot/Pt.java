package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;


/**
 * Base class for all points
 */
public class Pt implements Serializable, HandSerialize {
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

            x = StringUtils.parseDouble(sAry[0]);
            y = StringUtils.parseDouble(sAry[1]);
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
