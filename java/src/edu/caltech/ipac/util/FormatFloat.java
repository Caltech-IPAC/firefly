package edu.caltech.ipac.util;

import java.text.NumberFormat;

/**
 * A float object that allows you to control the toString output using
 * a NumberFormat classes.  This is very useful in options menus.
 * @see java.text.NumberFormat
 * @author Trey Roby
 */
public class FormatFloat extends Number {
   private Float        _f;
   private NumberFormat _nf;

   /**
    * Create a new FormatFloat class.
    * @param f the float values
    * @param nf the NumberFormat class will control how
    * the float is outputted using the toString
    */
   public FormatFloat(float f, NumberFormat nf) {
      _f= new Float(f);
      _nf= nf;
      Assert.tst(nf);
   }

   /**
    * Create a new FormatFloat class.
    * @param s a string representing a float value
    * @param nf the NumberFormat class will control how
    * the float is outputted using the toString
    */
   public FormatFloat(String s, NumberFormat nf) throws NumberFormatException {
      this(Float.parseFloat(s), nf);
   }
   /**
    * return this float as a stirng
    * @return  String a representation of the float
    */
   public String toString() {
      return _nf.format(_f.floatValue());
   }
   /**
    * Compare this object against a Float or a FormatFloat object.
    * Any other type of object will return a false
    * @param obj any object but only Float or FormatFloat will work.
    * @return boolean equal or not equal
    */
   public boolean equals(Object obj) { 
       boolean retval= false;
       if (obj instanceof Float)
             retval= _f.equals(obj);
       else if (obj instanceof FormatFloat) {
             //retval= (_f.floatValue() == ((FormatFloat)obj).floatValue());
             retval= toString().equals(obj.toString());
       }
       return retval;
  }
 
  public NumberFormat getNumberFormat() { return _nf; }

  public byte   byteValue()  { return _f.byteValue(); }
  public double doubleValue(){ return _f.doubleValue(); }
  public float  floatValue() { return _f.floatValue(); }
  public int    intValue()   { return _f.intValue(); }
  public long   longValue()  { return _f.longValue(); }
  public short  shortValue() { return _f.shortValue(); }
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
