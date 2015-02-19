/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.conv;

import com.google.gwt.i18n.client.NumberFormat;
import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;

/**
 *  This class provides conversion of user's 
 *  coordinate string to a double
 *  Translated from Rick Ebert's coord.c
 *
 *  April 10, 2001 
 *  Copied from the original Coord.java, modified to provide the convenient
 *  static methods to do validation, formatting, and conversion from decimal
 *  degree to HMS/DMS string and vice versa.  -xiuqin
 *  @author Booth Hartley, G.Turek, Xiuqin Wu
 */
public class CoordUtil
{
  private static final int DEFAULT_PRECISION = 8;
  private static final int MAX_PRECISION = 8;
  private static final int FORM_DECIMAL_DEGREE = 0;  // 12.34d
  private static final int FORM_DMS = 1;             // 12d34m23.4s
  private static final int FORM_HMS = 2;             // 12h34m23.4s

  private static final String LAT_OUT_RANGE =
       "Latitude is out of range [-90.0, +90.0]";
  private static final String LON_OUT_RANGE =
       "Longitude is out of range [0.0, 360.0)";
  private static final String LON_TOO_BIG =
       "Longitude is too big (>=360.0)";
  private static final String LON_NEGATIVE =
       "Longitude can not be negative";
  private static final String RA_TOO_BIG =
       "RA is too big (>=24 hours)";
  private static final String INVALID_STRING = "Invalid Input";
  private static final String HMS_FOR_LAT =
       "HMS notation not valid for latitude";
  private static final String SEX_FOR_NONE_EQU =
       "Sexagesimal for non-Equatorial coordinate";
  private static final String MIN_SEC_TOO_BIG =
       "Greater than 60 minutes or seconds";
  private static final String INVALID_SEPARATOR = "Invalid input";
  private static final String EMPTY_STRING = "Empty input";



    public static String convertLonToString(double lon, CoordinateSys coordSystem) throws CoordException {
        return convertLonToString(lon,coordSystem.isEquatorial());
    }

    public static String convertLonToString(double lon, boolean isEquatorial )
                                  throws CoordException {
        return dd2sex(lon, false, isEquatorial, 5);
    }

    public static String convertLatToString(double        lat,
                                            CoordinateSys coordSystem )
                                  throws CoordException {
        return convertLatToString(lat,coordSystem.isEquatorial());
    }

    public static String convertLatToString(double lat, boolean isEquatorial)
                                  throws CoordException {
        return dd2sex(lat, true, isEquatorial, 5);
    }

    public static double convertStringToLon(String        hms,
                                            CoordinateSys coordSystem )
                                  throws CoordException {
        boolean eq= coordSystem.isEquatorial();
        return sex2dd(hms,false, eq);
    }

    public static double convertStringToLon(String hms, boolean isEquatorial)
            throws CoordException {
        return sex2dd(hms,false, isEquatorial);
    }

    public static boolean validLon(String hms, CoordinateSys coordSystem) {
        boolean retval;
        try {
            convertStringToLon(hms,coordSystem);
            retval= true;
        } catch (CoordException e) {
            retval= false;
        }
        return retval;
    }

    public static double convertStringToLat(String        dms,
                                            CoordinateSys coordSystem )
                                  throws CoordException {
        boolean eq= coordSystem.isEquatorial();
        return sex2dd(dms,true, eq);
    }

    public static double convertStringToLat(String        dms, boolean isEquatorial)
            throws CoordException {
        return sex2dd(dms,true, isEquatorial);
    }


  public static void validate(String coordstr, boolean islat, boolean isequ)
                               throws CoordException
  {
      double d = sex2dd(coordstr, islat, isequ);
  }
  public static String format(String coordstr, boolean islat, boolean isequ)
                               throws CoordException
  {
     double d = sex2dd(coordstr, islat, isequ);
     return dd2sex(d, islat, isequ);
  }
  /**
   * @param coordstr
   * @param islat  boolean, true if the coordstr is latitude
   * @param isequ  boolean, true if the coordstr is in equatorial system
   * @param precision  for DMS, precision = 4+ number of 
   *                            decimal digits you want in seconds
   *                       for HMS, precision = 3+ number of 
   *                            decimal digits you want in seconds
   */
  public static String format(String coordstr, boolean islat,
                              boolean isequ, int precision) throws CoordException
  {
     double d = sex2dd(coordstr, islat, isequ);
     return dd2sex(d, islat, isequ, precision);
  }
  /**
   * @param coordstr
   * @param islat true if the coordstr is latitude
   * @param isequ true if the coordstr is in equatorial system
   */
  public static double sex2dd(String coordstr, boolean islat, boolean isequ)
                               throws CoordException
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
    int form;
    boolean isdec;

    //System.out.println("Coord:  now in c_coord");
    //System.out.println("Coord: coord = " + coordstr);
    //System.out.println("Coord: islat = " + islat);
    //System.out.println("Coord: isequ = " + isequ);

    r = null;
    parts = 0;
    degrees = -1;

    p = coordstr.trim();
    if (p.length() == 0) throw new CoordException(EMPTY_STRING);

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
    p = p.trim();  // allow space between sign and nbr
    if (p.length() == 0) throw new CoordException(INVALID_STRING);

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
          if (pointseen == 1) throw new CoordException(INVALID_STRING);
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

      if (numseen == 0) throw new CoordException(INVALID_STRING);

      //convert it and save it
      //System.out.println("RBH i = "+i + " numseen = "+numseen);
      strpart[i] = r.substring(0, numseen+pointseen);
      part[i] = (Double.valueOf(strpart[i])).doubleValue();
      //System.out.println("RBH i = "+i+" strpart[i] = "+strpart[i] + " part[i] = " + part[i] );

      r = p;  // now deal with the separator
      p = p.trim();
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
          throw new CoordException(INVALID_STRING);
        }
      }
      point[i] = pointseen;
      parts++;
      if (p.length() > 0)
      p = p.substring(1);
      p = p.trim();
    } // outer-for loop ends here

    // when we get here we've cracked as many as 3 parts
    // if there's anything left, the whole string is junk
    p = p.trim();
    if (p.length() > 0) throw new CoordException(INVALID_STRING);

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
      else throw new CoordException(INVALID_SEPARATOR);
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
      else throw new CoordException(INVALID_SEPARATOR);
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

      } else throw new CoordException(INVALID_SEPARATOR);

    } else throw new CoordException(INVALID_STRING);  // parts == 0

    if (degrees == -1)
    {
      if (islat) degrees = 1; // input is a latitude
      else if (isequ) degrees = 0;  // input is equatorial longitude (RA)
      else degrees = 1;  // all else is DMS
    }

    // No HMS for latitudes
    if (degrees == 0)  // not in degree format
    {
      if (islat)
         throw new CoordException(HMS_FOR_LAT);
    }

    // No sexigesimal input for non-equatorial systems
    if (!isequ && !isdec)
       throw new CoordException(SEX_FOR_NONE_EQU);

    // now modularize the input and convert to a double

    //System.out.println("part[0] = "+part[0]+"  part[1] = "+part[1]+ " part[2] ="+part[2]);
    if (parts > 1)
    {
      for (i = parts - 1; i >= 1; i--)
      {
        if (part[i] >= 60.0)
        {
          throw new CoordException(MIN_SEC_TOO_BIG);
          //part[i-1] += Math.floor(part[i] / 60.0);
          //part[i] = part[i] % 60.0;
        }
      }
    }
    //System.out.println("part[0] = "+part[0]+"  part[1] = "+part[1]+ "  part[2] ="+part[2]);

    if (degrees != 0) modangle = 360.0;    // in degree
    else modangle = 24.0;   // Hours RA

    for (i = 0, angle = 0.0, base = 1.0; i < parts; i++, base *= 60.0)
       angle += part[i] / base;


    // the input might be  xxm[xx[.xx]s] or just xx[.xx]s or xx[.xx]m 
    // so we apply appropriate weighting - whether it's min arc or time
    // we worry about further on.

    if (sep[0] == 'm' || sep[0] == '\'') angle /= 60.0;
    else if (sep[0] == 's' || sep[0] == '\"') angle /= 3600.0;

    angle *= sign;

    if (islat)
    {
      if (angle > 90.0 || angle < -90.0)
          throw new CoordException(LAT_OUT_RANGE);
    }
    else // input is longitude
    {
      if (angle >= modangle)
      {
          if (degrees != 0)
             throw new CoordException(LON_TOO_BIG);
          else
             throw new CoordException(RA_TOO_BIG);
          //angle = angle % modangle;
      }
      else if (angle < 0.0)
      {
          throw new CoordException(LON_NEGATIVE);
          //angle = (angle % modangle) + modangle;
      }
    }

    // make angle degrees if it isn't already and needs to be, 
    // from hours to degrees.
    // we've already disallowed 'hours' as a valid form for latitudes

    if (degrees == 0) angle *= 15.0;

    // canonical string
    /*
    if (isequ)
    {
      if (islat) form = 1;
      else form = 2;
    }
    else form = 0;
  

    dangle = angle;
    dangle_set = true;

    buf = c_ddsex(form);

   */
    //System.out.println("RBH coordstr [" + coordstr + "]");

    //System.out.println("Coord.java: buf = " + buf);
    //System.out.println("Coord.java: isdec = " + isdec);
    //System.out.println("Coord: dangle = " + dangle);

    return angle;
  }

  public static void validate(double dangle, boolean islat)
                              throws CoordException
  {
     if (islat) {
        if (dangle > 90.0 || dangle < -90.0)
           throw new CoordException(LAT_OUT_RANGE);
        }
     else {
        if (dangle >= 360.0 || dangle < 0.0)
           throw new CoordException(LON_OUT_RANGE);
        }
  }

  /**
   * Converts decimal angle to string format, using default precision
   * @param dangle decimal angle
   * @param islat  true if the coordstr is latitude
   * @param isequ  true if the coordstr is in equatorial system
   *
   * @return String representation of angle 
   */

  public static String dd2sex(double dangle, boolean islat,
                              boolean isequ) throws CoordException
  {
     return dd2sex(dangle, islat, isequ, DEFAULT_PRECISION);
  }
  /**
   * Converts decimal angle to string format
   * @param dangle decimal angle
   * @param islat true if the coordstr is latitude
   * @param isequ true if the coordstr is in equatorial system
   * @param precision for DMS, precision = 4+ number of 
   *                            decimal digits you want in seconds
   *                       for HMS, precision = 3+ number of 
   *                            decimal digits you want in seconds

   *  @return String representation of angle 
   */
  public static String dd2sex(double dangle, boolean islat,
                              boolean isequ, int precision)
                              throws CoordException
  {
    int c_precision ;
    int form;
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
    NumberFormat df;
    String fmtstr;
    int i;
    String buf;

    if (precision <= 0) c_precision = 0;
    else if (precision <= MAX_PRECISION) c_precision = precision;
    else c_precision = DEFAULT_PRECISION ;

    if ((dangle < 0.0) && islat) isign = -1;
    if ((dangle < 0.0) && !islat) dangle = (dangle % 360.0) + 360.0;
    if ((dangle >= 360.0) && !islat) dangle = dangle % 360.0;

    // sign for Latitude/Dec  '+' or '-'
    if (isign == 1)
    {
      if (islat) signstr = "+";
      else signstr = "";

    }
    else signstr = "-";

    if (isequ)
    {
      if (islat) form = FORM_DMS;
      else form = FORM_HMS;
    }
    else form = FORM_DECIMAL_DEGREE;

    if (form == FORM_HMS) dangle /= 15.0;  // convert degree to hours
    if ((form == FORM_DMS) || (form == FORM_HMS))
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

      if (form == FORM_DMS)
      {
        // sexigesimal degrees 
        circ = 360;
        chd = 'd';
        switch (c_precision)
        {
          case 0:
            d = 1; break;

          case 1: case 2:
            d = 2; break;

          case 3: case 4:
            d = 3; break;

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
            d = 2; break;

          case 2: case 3:
            d = 3; break;

          default:
            d = c_precision; break;    // 3+p-3
        }
        drfs = c_precision - 3;   // the digits after seconds 
      }
      switch (d)
      {
        case 1:   // only degree
          rhd = hd + ((m >= 30) ? 1 : 0);
          if (rhd >= circ) rhd -= circ;
          buf = signstr;
          df = NumberFormat.getFormat("00");
          buf += df.format(rhd);
          buf += "d";
        break;

        case 2:   // degree + minutes
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
          df = NumberFormat.getFormat("00");
          buf += df.format(rm);
          buf += cm;
        break;

        case 3:   // degree + minutes + seconds
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
          df = NumberFormat.getFormat("00");
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
          df = NumberFormat.getFormat("00");
          buf += df.format(rm);
          buf += cm;
          buf += df.format(rs);
          buf += ".";
          for (i = 0, fmtstr = ""; i < drfs; i++) fmtstr += "0";
          //System.out.println("RBH fmtstr = " + fmtstr);
          df = NumberFormat.getFormat(fmtstr);
          buf += df.format(rfs);
          buf += cs;
        break;
      }
    }
    else // form == FORM_DECIMAL_DEGREE or otherwise unrecognized
    {
      if (islat && dangle >= 0) buf = "+";
      else buf = "";
      for (i = 0, fmtstr = "0."; i < c_precision; i++) fmtstr += "0";
      df = NumberFormat.getFormat(fmtstr);
//      NumberFormat nf = NumberFormat.getInstance(Locale.US);
//      df= (DecimalFormat)nf;
//       df.setMaximumFractionDigits(c_precision);
      buf += df.format(dangle);
      buf += "d";
    }

    return buf;
  }

  private static char c_separator(String str)
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

}



