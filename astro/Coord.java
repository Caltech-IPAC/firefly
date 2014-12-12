package edu.caltech.ipac.astro;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *  This class provides conversion of user's 
 *  coordinate string to a double
 *  Translated from Rick Ebert's coord.c
 *
 *  @author Booth Hartley, G.Turek
 */
public class Coord
{
  private final int DEFAULT_PRECISION = 8;
  private final int MAX_PRECISION = 8;
  private int c_precision = DEFAULT_PRECISION;
  private double dangle;
  private boolean dangle_set = false;

  private String buf, coordstr;
  private boolean islat, isequ;
  private boolean isdec;
  private int form;

  /**
   *  Constructor
   *  @param coordstr string representation of coordinate
   *  @param islat latitude flag (true or false)
   *  @param isequ
   */
  public Coord(String coordstr, boolean islat, boolean isequ)
  {
    this.coordstr = coordstr;
    this.islat = islat;
    this.isequ = isequ;
  }

  /**
   *  Constructor
   *  @param dangle angle in degrees
   *  @param islat latitude flag (true or false)
   *  @param isequ equatorial flag (true or false)
   */
  public Coord(double dangle, boolean islat, boolean isequ)
  {
    this.dangle = dangle;
    dangle_set = true;
    this.islat = islat;
    // add "d" at the end to signal it is degree,  XW 1/11/00
    this.coordstr = Double.toString(dangle)+"d";
    this.isequ = isequ;
  }


  public double c_coord() throws CoordException
  {
    int sign;
    String p, r;
    int pointseen, i, done, cntdigits, numseen, parts;
    int degrees;
    char sep[] = new char[3];
    double part[] = new double[3];
    double modangle, angle, base;
    int point[] = new int[3];
    String strpart[] = new String[3];

    //System.out.println("Coord:  now in c_coord");
    //System.out.println("Coord: coord = " + coordstr);
    //System.out.println("Coord: islat = " + islat);
    //System.out.println("Coord: isequ = " + isequ);

    r = null;
    parts = 0;
    degrees = -1;

    p = coordstr;
    p = skipwhite(p);
    if (p.length() == 0) throw new CoordException("Invalid input");

    sign = 1;  // assume positive
    if (p.charAt(0) == '-')
    {
      sign = -1;
      p = p.substring(1);
    }
    else if (p.charAt(0) == '+')
    {
      sign = 1;
      p = p.substring(1);
    }
    p = skipwhite(p);  // allow space between sign and nbr
    if (p.length() == 0) throw new CoordException("Invalid input");

    for (i = 0; i < 3 && (p.length() > 0) ; i++)
    {
      pointseen = 0;
      done = 0;
      cntdigits = 0;
      numseen = 0;
      r = null;

      while ((p.length() > 0) && done == 0)
      {
        if (p.charAt(0) == '.')
        {
          if (pointseen == 1) throw new CoordException("Invalid input");
          else
          {
            pointseen = 1;
            if (r == null) r = p;  //Mark this place - number starts here
          }
        }
        else if (Character.isDigit(p.charAt(0)))
        {
          if (pointseen == 0) cntdigits++;
          if (r == null) r = p;  //Mark this place - number starts here
          numseen++;
        }
        else done = 1;

        if (done == 0) p = p.substring(1);
      }  // end of inner-while

      if (numseen == 0) throw new CoordException("Invalid input");

      //convert it and save it
      //System.out.println("RBH i = "+i + " numseen = "+numseen);
      strpart[i] = r.substring(0, numseen+pointseen);
      part[i] = (Double.valueOf(strpart[i])).doubleValue();
      //System.out.println("RBH i = "+i+" strpart[i] = "+strpart[i] + " part[i] = " + part[i] );

      r = p;  // now deal with the separator
      p = skipwhite(p);
      sep[i] = c_separator(p);
      if (sep[i] == '\0')
      {
        // this character is NOT a separator
        if (r.charAt(0) == ' ')
        {
          sep[i] = ' ';
          p = r;
        }
        else
        {
          throw new CoordException("Invalid input");
        }
      }
      point[i] = pointseen;
      parts++;
      if (p.length() > 0)
      p = p.substring(1);
      p = skipwhite(p);
    } // outer-for loop ends here

    // when we get here we've cracked as many as 3 parts
    // if there's anything left, the whole string is junk
    p = skipwhite(p);
    if (p.length() > 0) throw new CoordException("Invalid input");

    // Another way to be junk is to have a decimal point in any but
    // the 'last' part

    if ((parts==3 && (point[0] == 1 || point[1] == 1 )) || (parts==2 && point[0] == 1)) throw new CoordException("Invalid input");

    // and another way to be junk is to use inconsistent or out 
    // of order separators
    // Here we also crack whether the input was ""decimal"" or not

    if (parts == 3)
    {
      isdec = false;
      if (sep[0] == 'h' && sep[1] == 'm' && sep[2] == 's') degrees = 0;
      else if (sep[0] == 'h' && sep[1] == 'm' && sep[2] == ' ') degrees = 0;
      else if (sep[0] == 'd' && sep[1] == 'm' && sep[2] == 's') degrees = 1;
      else if (sep[0] == 'd' && sep[1] == 'm' && sep[2] == ' ') degrees = 1;
      else if (sep[0] == ':' && sep[1] == ':' && sep[2] == ' ') {}
      else if (sep[0] == ' ' && sep[1] == ' ' && sep[2] == ' ') {}
      else if (sep[0] == 'd' && sep[1] == '\'' && sep[2] == '\"') degrees = 1;
      else if (sep[0] == 'd' && sep[1] == '\'' && sep[2] == ' ') degrees = 1;
      else throw new CoordException("Invalid input");
    }
    else if (parts == 2)
    {
      isdec = false;
      if (sep[0] == 'h' && sep[1] == 'm') degrees = 0;
      else if (sep[0] == 'h' && sep[1] == ' ') degrees = 0;
      else if (sep[0] == 'd' && sep[1] == 'm') degrees = 1;
      else if (sep[0] == 'd' && sep[1] == ' ') degrees = 1;
      else if (sep[0] == 'm' && sep[1] == 's') {}
      else if (sep[0] == 'm' && sep[1] == ' ') {}
      else if (sep[0] == ':' && sep[1] == ' ') {}
      else if (sep[0] == ' ' && sep[1] == ' ') {}
      else if (sep[0] == 'd' && sep[1] == '\'') degrees = 1;
      else if (sep[0] == '\'' && sep[1] == '\"') degrees = 1;
      else if (sep[0] == '\'' && sep[1] == ' ') degrees = 1;
      else throw new CoordException("Invalid input");
    }
    else if (parts == 1)
    {
      if (sep[0] == 'h') {
        degrees = 0;
        isdec = false;

      } else if (sep[0] == 'd') {
        degrees = 1;
        isdec = true;

      } else if (sep[0] == ' ') {

        if (isequ && !islat) isdec = false;
        else isdec = true;

      } else if (sep[0] == 'm') {
        isdec = false;

      } else if (sep[0] == 's') {
        isdec = false;

      } else if (sep[0] == '\'') {
        degrees = 1;
        isdec = false;

      } else if (sep[0] == '\"') {
        degrees = 1;
        isdec = false;

      } else throw new CoordException("Invalid input");

    } else throw new CoordException("Invalid input");  // parts == 0

    if (degrees == -1)
    {
      if (islat) degrees = 1; // input is a latitude
      else if (isequ) degrees = 0;  // input is equatorial longitude (RA)
      else degrees = 1;  // all else is DMS
    }

    // No HMS for latitudes
    if (degrees == 0)
    {
      if (islat) throw new CoordException("HMS notation not valid for latitude");
    }

    // No sexigesimal input for non-equatorial systems
    if (!isequ && !isdec) throw new CoordException("Sexagesimal for non-Equatorial coordinate");

    // now modularize the input and convert to a double

    //System.out.println("part[0] = "+part[0]+"  part[1] = "+part[1]+ " part[2] ="+part[2]);
    if (parts > 1)
    {
      for (i = parts - 1; i >= 1; i--)
      {
        if (part[i] >= 60.0)
        {
          throw new CoordException("Greater than 60 minutes or seconds");
          //part[i-1] += Math.floor(part[i] / 60.0);
          //part[i] = part[i] % 60.0;
        }
      }
    }
    //System.out.println("part[0] = "+part[0]+"  part[1] = "+part[1]+ "  part[2] ="+part[2]);

    if (degrees != 0) modangle = 360.0;
    else modangle = 24.0;   // Hours RA

    for (i = 0, angle = 0.0, base = 1.0; i < parts; i++, base *= 60.0) angle += part[i] / base;


    // the input might be  xxm[xx[.xx]s] or just xx[.xx]s or xx[.xx]m 
    // so we apply appropriate weighting - whether it's min arc or time
    // we worry about further on.

    if (sep[0] == 'm' || sep[0] == '\'') angle /= 60.0;
    else if (sep[0] == 's' || sep[0] == '\"') angle /= 3600.0;

    angle *= sign;

    if (islat)
    {
      if (angle > 90.0 || angle < -90.0) throw new CoordException("Latitude >+90d or <-90d");
    }
    else
    {
      // input is longitude
      if (angle >= modangle)
      {
          throw new CoordException("Longitude too large");
          //angle = angle % modangle;
      }
      else if (angle < 0.0)
      {
          throw new CoordException("Negative longitude");
          //angle = (angle % modangle) + modangle;
      }
    }

    // make angle degrees if it isn't already and needs to be
    // we've already disallowed 'hours' as a valid form for latitudes

    if (degrees == 0) angle *= 15.0;

    // canonical string
    if (isequ)
    {
      if (islat) form = 1;
      else form = 2;
    }
    else form = 0;


    dangle = angle;
    dangle_set = true;

    buf = c_ddsex(form);

    //System.out.println("RBH coordstr [" + coordstr + "]");

    //System.out.println("Coord.java: buf = " + buf);
    //System.out.println("Coord.java: isdec = " + isdec);
    //System.out.println("Coord: dangle = " + dangle);

    return dangle;
  }

  /**
   *  Returns the sexigesimal form of a string representation of
   *  coordinates in any other form 
   *  @return String sexigesimal representation of coordinates 
   */
  public String getCanonicalString() throws CoordException
  {
    if (buf == null) c_coord();
    return buf;
  }

  /**
   *  Returns indication of whether input coordinates were in
   *  decimal or hexidecimal
   *  @return boolean
   */
  public boolean is_decimal() throws CoordException
  {
    if (buf == null) c_coord();
    return isdec;
  }

  /**
   *  Converts decimal angle to string format
   *  @param form output format: 0 to decimal degrees, 1 dms, 2 hms
   *  @return String representation of angle 
   */
  public String c_ddsex(int form) throws CoordException
  {
    double tmp, dfs;
    int ofs;
    int hd, m, s;
    int rhd, rm, rs, rfs;
    int drfs;
    int d;
    int circ;
    char chd;
    char cm = 'm';
    char cs = 's';
    int isign = 1;
    String signstr;
    DecimalFormat df;
    String fmtstr;
    int i;
    double dangle;

    if (!dangle_set) c_coord();
    dangle = this.dangle;

    if ((dangle < 0.0) && islat) isign = -1;
    if ((dangle < 0.0) && !islat) dangle = (dangle % 360.0) + 360.0;
    if ((dangle >= 360.0) && !islat) dangle = dangle % 360.0;

    if (isign == 1)
    {
      if (islat) signstr = "+";
      else signstr = "";

    } else signstr = "-";

    if (form == 2) dangle /= 15.0;
    if ((form == 1) || (form == 2))
    {
      tmp = Math.abs(dangle);
      hd = (int) tmp;
      tmp -= hd;
      tmp *= 60.0;
      m = (int) tmp;
      tmp -= m;
      tmp *= 60.0;
      s = (int) tmp;
      tmp -= s;
      dfs = tmp;

      if (form == 1)
      {
        // sexigesimal degrees 
        circ = 360;
        chd = 'd';
        switch (c_precision)
        {
          case 0:
            d = 1;
          break;

          case 1: case 2:
            d = 2;
          break;

          case 3: case 4:
            d = 3;
          break;

          default:
            d = c_precision - 1;    // 3 + precision + 4
          break;
        }
        drfs = c_precision - 4;
      }
      else
      {
        // sexigesimal hours min sec
        circ = 24;
        chd = 'h';
        switch (c_precision)
        {
          case 0: case 1:
            d = 2;
          break;

          case 2: case 3:
            d = 3;
          break;

          default:
            d = c_precision;
          break;    // 3+p-3
        }
        drfs = c_precision - 3;
      }
      switch (d)
      {
        case 1:
          rhd = hd + ((m >= 30) ? 1 : 0);
          if (rhd >= circ) rhd -= circ;
          buf = signstr;
          df = new DecimalFormat("00");
          buf += df.format(rhd);
          buf += "d";
        break;

        case 2:
          rm = m + ((s >= 30) ? 1 : 0);
          rhd = hd;
          if (rm >= 60)
          {
            rm -= 60;
            rhd++;
          }
          if (rhd >= circ) rhd -= circ;
          buf = signstr;
          buf += rhd;
          buf += chd;
          df = new DecimalFormat("00");
          buf += df.format(rm);
          buf += cm;
        break;

        case 3:
          rs = s + ((dfs >= 0.5)?1:0);
          rm = m;
          rhd = hd;
          if (rs >= 60)
          {
            rs -= 60;
            rm++;
          }
          if (rm >= 60)
          {
            rm -= 60;
            rhd++;
          }
          if (rhd >= circ) rhd -= circ;
          buf = signstr;
          buf += rhd;
          buf += chd;
          df = new DecimalFormat("00");
          buf += df.format(rm);
          buf += cm;
          buf += df.format(rs);
          buf += cs;
        break;

        default:
          rs = s;
          rm = m;
          rhd = hd;
          tmp = Math.pow(10, drfs);
          ofs = (int) tmp;
          rfs = (int) Math.round(dfs * ofs);
          if (rfs >= ofs)
          {
            rfs -= ofs;
            rs++;
          }
          if (rs >= 60)
          {
            rs -= 60;
            rm++;
          }
          if (rm >= 60)
          {
            rm -= 60;
            rhd++;
          }
          if (rhd >= circ) rhd -= circ;
          buf = signstr;
          buf += rhd;
          buf += chd;
          df = new DecimalFormat("00");
          buf += df.format(rm);
          buf += cm;
          buf += df.format(rs);
          buf += ".";
          for (i = 0, fmtstr = ""; i < drfs; i++) fmtstr += "0";
          //System.out.println("RBH fmtstr = " + fmtstr);
          df = new DecimalFormat(fmtstr);
          buf += df.format(rfs);
          buf += cs;
        break;
      }
    }
    else
    {
      // form == 0 or otherwise unrecognized
      if (islat && dangle >= 0) buf = "+";
      else buf = "";
      for (i = 0, fmtstr = "0."; i < c_precision; i++) fmtstr += "0";
      //df = new DecimalFormat(fmtstr);
      NumberFormat nf = NumberFormat.getInstance(Locale.US);
      df= (DecimalFormat)nf;
      buf += df.format(dangle);
      buf += "d";
    }

    return buf;
  }

  /**
   *  Set precision
   *  @param p integer precision
   *  @return precision
   */
  public int c_setprecision(int p)
  {
    if (p <= 0) c_precision = 0;
    else if (p <= MAX_PRECISION) c_precision = p;
    else c_precision = DEFAULT_PRECISION ;
    return c_precision;
  }

  private char c_separator(String str)
  {
    if (str.length() == 0) return ' ';
    switch (str.charAt(0))
    {
      case 'h': case 'H': return 'h';
      case 'd': case 'D': return 'd';
      case 'm': case 'M': return 'm';
      case 's': case 'S': return 's';
      case ':': return ':';
      case '\'': case '\"': return str.charAt(0);
      case ' ': case '\t': default: return '\0';
    }
  }

  private String skipwhite(String str)
  {
    int i;
    for (i = 0; i<str.length(); i++)
    {
      if (str.charAt(i) !=' ') break;
    }
    return (str.substring(i));
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
