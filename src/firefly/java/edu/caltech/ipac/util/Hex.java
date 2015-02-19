/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

/**
 *  Provides conversion from/to Hexadecimal
 *
 *  @author G. Turek
 */
public class Hex
{
  /**
   *  Converts a string of hex digits into a byte array of those digits
   *  @param s String of hex digits
   *  @return byte array digits 
   */
  static public byte[] toByteArr(String s)
  {
    byte[] number = new byte[s.length()/2];
    int i;
    for (i=0; i < s.length(); i+=2)
    {
      int j = Integer.parseInt(s.substring(i,i+2), 16);
      number[i/2] = (byte)(j & 0x000000ff);
    }
    return number;
  }

  /**
   *  Converts byte array to #Hex
   *  @param b byte array
   *  @return String hex digits
   */
  static public String toHexF(byte[] b)
  {
    StringBuffer s = new StringBuffer("");
    int i;

    if (b==null) return "<null>";

    int len = b.length;

    for (i=0; i<len; i++)
    {
      s.append(" " + toHex(b[i]));
      if      (i%16 == 15) s.append("\n");
      else if (i% 8 ==  7) s.append(" ");
      else if (i% 4 ==  3) s.append(" ");
    }
    if (i%16 != 0) s.append("\n");

    return s.toString();
  }

  /**
   *  Converts short array to #Hex
   *  @param b short array
   *  @return String hex digits
   */
  static public String toHexF(short[] b)
  {
    StringBuffer s = new StringBuffer("");
    int i;

    if (b==null) return "<null>";

    int len = b.length;

    for (i=0; i<len; i++)
    {
      s.append(" " + toHex(b[i]));
      if      (i%16 ==  7) s.append("\n");
      else if (i% 4 ==  3) s.append(" ");
    }
    if (i%8 != 0) s.append("\n");

    return s.toString();
  }

  /**
   *  Converts int array to #Hex
   *  @param b int array
   *  @return String hex digits
   */
  static public String toHexF(int[] b)
  {
    StringBuffer s = new StringBuffer("");
    int i;

    if (b==null) return "<null>";

    int len = b.length;

    for (i=0; i<len; i++)
    {
      s.append(" " + toHex(b[i]));
      if (i%4 == 3) s.append("\n");
    }
    if (i%4 != 0) s.append("\n");
    return s.toString();
  }

  /**
   *  Converts int array to Hex
   *  @param b int array
   *  @return String hex digits
   */
  static public String toHex(int[] b)
  {
    if (b==null) return "";
    int len = b.length;
    StringBuffer s = new StringBuffer("");
    int i;
    for (i=0; i<len; i++) s.append(toHex(b[i]));
    return s.toString();
  }

  /**
   *  Converts short array to Hex
   *  @param b short array
   *  @return String hex digits
   */
  static public String toHex(short[] b)
  {
    if (b==null) return "";
    int len = b.length;
    StringBuffer s = new StringBuffer("");
    int i;
    for (i=0; i<len; i++) s.append(toHex(b[i]));
    return s.toString();
  }

  /**
   *  Converts byte array to Hex
   *  @param b byte array
   *  @return String hex digits
   */
  static public String toHex(byte[] b)
  {
    if (b==null) return "";
    int len = b.length;
    StringBuffer s = new StringBuffer("");
    int i;
    for (i=0; i<len; i++) s.append(toHex(b[i]));
    return s.toString();
  }

  /**
   *  Converts byte to Hex
   *  @param b byte
   *  @return String hex digits
   */
  static public String toHex(byte b)
  {
    Integer I = new Integer((((int)b) << 24) >>> 24);
    int i = I.intValue();

    if (i < (byte)16) return "0"+Integer.toString(i, 16);
    else return Integer.toString(i, 16);
  }

  /**
   *  Converts short to Hex
   *  @param i short
   *  @return String hex digits
   */
  static public String toHex(short i)
  {
    byte b[] = new byte[2];
    b[0] = (byte)((i & 0xff00) >>>  8);
    b[1] = (byte)((i & 0x00ff)       );

    return toHex(b[0])+toHex(b[1]);
  }

  /**
   *  Converts integer to Hex
   *  @param i integer
   *  @return String hex digits
   */
  static public String toHex(int i)
  {
    byte b[] = new byte[4];
    b[0] = (byte)((i & 0xff000000) >>> 24);
    b[1] = (byte)((i & 0x00ff0000) >>> 16);
    b[2] = (byte)((i & 0x0000ff00) >>>  8);
    b[3] = (byte)((i & 0x000000ff)       );

    return toHex(b[0])+toHex(b[1])+toHex(b[2])+toHex(b[3]);
  }

  /**
   *  Returns long equivalent to String representation of Hex
   *  @param s String of hex digits
   *  @return long equivalent
   */
  static public long fromHex(String s)
  {
    long val = 0;
    long base = 16;
    for (int i = 0; i<s.length(); i++)
    {
      int cntr = s.length()-1-i;
      char c = s.charAt(cntr);
      int ic = (int)c;
      long mult = (long)Math.pow((double)base,(double)i);
      switch (ic)
      {
         case '0': case '1': case '2': case '3': case '4':
         case '5': case '6': case '7': case '8': case '9':
          val += (long)Character.getNumericValue(c) * mult;
          break;
         case 'a': case 'A':
          val += 10 * mult;
          break;
         case 'b': case 'B':
          val += 11 * mult;
          break;
         case 'c': case 'C':
          val += 12 * mult;
          break;
         case 'd': case 'D':
          val += 13 * mult;
          break;
         case 'e': case 'E':
          val += 14 * mult;
          break;
         case 'f': case 'F':
          val += 15 * mult;
          break;
         default:
          throw new NumberFormatException("Expected hexadecimal digit.");
      }
    }
    return val;
  }  

  /**
   *  Prints hex representation of byte array to stdout
   *  @param b byte array
   */
  static public void printHex(byte[] b)
  {
    System.out.print(toHexF(b));
  }

  /**
   *  Prints hex representation of short array to stdout
   *  @param b short array
   */
  static public void printHex(short[] b)  
  {
    System.out.print(toHexF(b));
  }

  /**
   *  Prints hex representation of int array to stdout
   *  @param b int array
   */
  static public void printHex(int[] b)    
  {
    System.out.print(toHexF(b));
  }
}


