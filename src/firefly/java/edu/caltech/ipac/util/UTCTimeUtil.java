/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*****************************************************************************
 * Copyright (C) 1999 California Institute of Technology. All rights reserved
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged
 ****************************************************************************/
package edu.caltech.ipac.util;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Static class parses a string of format yyyy-dddThh:mm:ss.sss
 * to a Date and viceversa
 * Checks that that a UTC date falls between given bounds
 * Get methods to get sub quantities (year, day, etc) out of the string
 *
 * @author G. Turek             
 * @version $Id: UTCTimeUtil.java,v 1.7 2009/03/23 18:54:08 roby Exp $
 */
public class UTCTimeUtil implements Cloneable
{

  /**
   *  Checks that a given date falls within bounds by checking
   *  if year falls between given years
   *  @param actual actual date
   *  @param beg begin date
   *  @param end end date
   *  @return true if within bounds
   */
  public static boolean checkBounds(String actual, String beg, String end)
  {
		Date utcActual = UTCTimeUtil.getDate(actual);
		Date utcBeg = UTCTimeUtil.getDate(beg);
		Date utcEnd = UTCTimeUtil.getDate(end);

		if (utcActual.before(utcBeg) || utcActual.after(utcEnd)) {
			return false;
		} else return true;
  }

  /**
   *  Get year
   *  @return year
   */
  public static int getYear(String date)
  {
    return Integer.parseInt(date.substring(0,4));
  }

  /**
   *  Get day number
   *  @return day number within given year
   */
  public static int getDay(String date)
  {
    return Integer.parseInt(date.substring(5,8));
  }

  /**
   *  Get hour
   *  @return hour
   */
  public static int getHour(String date)
  {
    return Integer.parseInt(date.substring(9,11));
  }

  /**
   *  Get minute
   *  @return minute
   */
  public static int getMinute(String date)
  {
    return Integer.parseInt(date.substring(12,14));
  }

  /**
   *  Get second
   *  @return second
   */
  public static double getSecond(String date)
  {
    return Double.parseDouble(date.substring(15));
  }

  /**
   *  Convert UTC string to Date object (GMT)
   *  @param utc string
   *  @return Date object
   */
  public static Date getDate(String utc)
  {
    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.YEAR,Integer.parseInt(utc.substring(0,4)));
    cal.set(Calendar.DAY_OF_YEAR,Integer.parseInt(utc.substring(5,8)));
    cal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(utc.substring(9,11)));
    cal.set(Calendar.MINUTE,Integer.parseInt(utc.substring(12,14)));
    cal.set(Calendar.SECOND,Integer.parseInt(utc.substring(15,17)));
    cal.set(Calendar.MILLISECOND,Integer.parseInt(utc.substring(18)));
    return cal.getTime();
  }

  /**
   *  Get a Date object in yyyy-dddThh:mm:ss.sss format (GMT)
   *  @param date
   *  @return UTC 
   */
  public static String getUTCString(Date date)
  {
    NumberFormat i3 = NumberFormat.getInstance();
    NumberFormat i2 = NumberFormat.getInstance();
    i3.setMinimumIntegerDigits(3);
    i2.setMinimumIntegerDigits(2);
    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    cal.setTime(date);
    String sdate = cal.get(Calendar.YEAR)
                + "-" + i3.format(cal.get(Calendar.DAY_OF_YEAR))
                + "T" + i2.format(cal.get(Calendar.HOUR_OF_DAY))
                + ":" + i2.format(cal.get(Calendar.MINUTE))
                + ":" + i2.format(cal.get(Calendar.SECOND)) 
                + "." + i3.format(cal.get(Calendar.MILLISECOND));
    return sdate;
  }

  /**
   *  Get current Date (GMT)
   *  @return current Date object 
   */
  public static Date getCurrentDate()
  {
    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    return cal.getTime();
  }
  
  /**
   *  Get current date in yyyy-dddThh:mm:ss.sss format (GMT)
   *  @return current date string
   */
  public static String getCurrentUTCString()
  {
    NumberFormat i3 = NumberFormat.getInstance();
    NumberFormat i2 = NumberFormat.getInstance();
    i3.setMinimumIntegerDigits(3);
    i2.setMinimumIntegerDigits(2);
    Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    String date = cal.get(Calendar.YEAR)
                + "-" + i3.format(cal.get(Calendar.DAY_OF_YEAR))
                + "T" + i2.format(cal.get(Calendar.HOUR_OF_DAY))
                + ":" + i2.format(cal.get(Calendar.MINUTE))
                + ":" + i2.format(cal.get(Calendar.SECOND))
                + "." + i3.format(cal.get(Calendar.MILLISECOND));
    return date;
  }

  /**
   *  @return the number of milliseconds since January 1, 1970, 00:00:00 GMT
   */
  public static long getTime(String utc)
  {
    Date d = UTCTimeUtil.getDate(utc); 
    return d.getTime();
  }

  /**
   *  Get a Date object in yyyy-dddThh:mm:ss.sss format (GMT)
   *  @param msec of milliseconds since January 1, 1970, 00:00:00 GMT
   *  @return UTC 
   */
  public static String getUTCString(long msec)
  {
    Date d = new Date(msec); 
    return UTCTimeUtil.getUTCString(d);
  }


    /**
     *  Get a String in HH:MM:SS format for a given total seconds (float)
     *  total seconds = hour*3600 + minute*60 +seconds,  Michael Nguyen
     *  @param totalSecond of seconds
     *  @return String
     */
    public static String getHMS(int totalSecond) {
        return getHMS((long)totalSecond);
    }



  /**
   *  Get a String in HH:MM:SS format for a given total seconds (float)
   *  total seconds = hour*3600 + minute*60 +seconds,  Michael Nguyen
   *  @param totalSecond of seconds 
   *  @return String 
   */
  public static String getHMS(long totalSecond)
  {
    String time = "00:00:00";
    DecimalFormat decimalFormat = new DecimalFormat("00");

    if (totalSecond > 0) 
    {
      long hour = totalSecond / 3600;
      if (hour > 0) time = decimalFormat.format(hour);
      else time = "00";
      time += ":";
      long minute = (totalSecond - (hour*3600)) / 60;
      if (minute > 0) time += decimalFormat.format(minute);
      else time += "00";
      time += ":";
      long second = totalSecond - (hour*3600) - (minute*60);
      if (second > 0) time += decimalFormat.format(second);
      else time += "00";
      return time;
    }
    else return time;

  }

    /**
     *  Get a String in HH:MM:SS format for a given total seconds (float)
     *  total seconds = hour*3600 + minute*60 +seconds,  Michael Nguyen
     *  @param totalSecond of seconds
     *  @return String
     */
    public static String getDHMS(long totalSecond)
    {
        String time = "00:00:00";

        if (totalSecond > 0)
        {
            String dayStr = "";
            long days = totalSecond / (3600*24);
            if (days > 0) {
                dayStr= days + " days ";
                totalSecond-= (3600*24);
            }

            DecimalFormat decimalFormat = new DecimalFormat("00");
            long hour = totalSecond / 3600;
            if (hour > 0) time = decimalFormat.format(hour);
            else time = "00";
            time += ":";
            long minute = (totalSecond - (hour*3600)) / 60;
            if (minute > 0) time += decimalFormat.format(minute);
            else time += "00";
            time += ":";
            long second = totalSecond - (hour*3600) - (minute*60);
            if (second > 0) time += decimalFormat.format(second);
            else time += "00";
            return dayStr + time;
        }
        else {
            return time;
        }

    }




    public static String getHMSFromMills(long millSec)
    {
        String retval;
        int decimal= (int)(millSec % 1000);
        int seconds= (int)(millSec / 1000);
        if (seconds>0 ) {
            if (seconds < 60) {
                retval= seconds + "." + decimal + " sec";
            }
            else {
                String outStr= getHMS(seconds);
                retval= outStr + "." + decimal;
            }
        }
        else {
            retval= decimal + " ms";
        }
        return retval;
    }

    public static String getDHMSFromMills(long millSec)
    {
        String retval;
        int decimal= (int)(millSec % 1000);
        int seconds= (int)(millSec / 1000);
        if (seconds>0 ) {
            if (seconds < 60) {
                retval= seconds + "." + decimal + " sec";
            }
            else {
                String outStr= getHMS(seconds);
                retval= outStr + "." + decimal;
            }
        }
        else {
            retval= decimal + " ms";
        }
        return retval;
    }

}


