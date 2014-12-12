package edu.caltech.ipac.util.action;


import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.dd.PropDbFieldDefSource;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


public class DateTimeAction extends TextAction {

    private final TimeZone  GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    private Date            _max             ;
    private Date            _min             ;
    private Date            _def             ;
    private String          _maxString       ;
    private String          _minString       ;
    private String          _defString       ;
    private Date            _lastValue;
    private String          _lastValueStr;

    private boolean         _mayDoNull       = false;

    private static final String FORMAT_ERROR =
                   "The format is wrong ";
    private String _formatError = null;
    private String _standardFormatString;
    private String _acceptedFormatStrings [];

    private DateFormat  _standardFormat;
    private DateFormat _acceptedFormats [];
    private static final DateFormat _localizedAcceptedFormats []= {
                           DateFormat.getDateInstance(DateFormat.LONG),
                           DateFormat.getDateInstance(DateFormat.MEDIUM),
                           DateFormat.getDateInstance(DateFormat.SHORT)
                        };

    private Calendar  _cal = new GregorianCalendar(GMT_TIME_ZONE);

    public DateTimeAction(String propName, String standardFormatString,
                          String acceptedFormatStrings[],
                          InputContainer inputContainer) {
       super(propName, inputContainer);
       _standardFormatString = standardFormatString;
       _acceptedFormatStrings = acceptedFormatStrings;
       getProperties(propName);
       setValueDontValidate(_def);
    }

    public DateTimeAction(String propName, String standardFormatString,
                          String acceptedFormatStrings[]) {
        this(propName, standardFormatString, acceptedFormatStrings, null);
    }

    public DateTimeAction(String propName) {
       this(propName, null, null, null);
    }


    public DateTimeAction() {
       this(null);
       setValueDontValidate(_def);
    }


    protected void getProperties(String propName) {
      super.initProperties(new PropDbFieldDefSource(propName));
      _defString=    AppProperties.getProperty(
                    propName +"."+ ActionConst.DEFAULT , _defString);
      _minString=    AppProperties.getProperty(
                    propName +"."+ ActionConst.MIN , _minString );
      _maxString=    AppProperties.getProperty(
                    propName +"."+ ActionConst.MAX , _maxString );
      _mayDoNull= AppProperties.getBooleanProperty(
                    propName +"."+ ActionConst.NULL_ALLOWED, _mayDoNull );
      if (_mayDoNull) _defString= "";  // ?????

   /* we need to get the standard format and accepted formats 
      from property file
   */
      _standardFormatString =    AppProperties.getProperty(
                    propName +".dateTimeFormatStandard",
                    _standardFormatString );
      String strings =    AppProperties.getProperty(
                    propName +".dateTimeFormatAccepted");
      if (strings != null)
         _acceptedFormatStrings = StringUtil.strToStrings(strings);

      defineStandardFormat();
      defineAcceptedFormats((strings==null));

//  Here we need to change the string min/max to Date min/max   XW ?????
      //System.out.println("minString:    " + _minString);
      //System.out.println("maxString:    " + _maxString);
      //System.out.println("defString:    " + _defString);
      //System.out.println("mayDoNUll:    " + _mayDoNull);
      _min = stringToDate(_minString);
      _max = stringToDate(_maxString);
      _def = stringToDate(_defString);

       /*
        * 1. Get the validation type from a property if it exist
        *        and set it on the field
        * 2. If there is no property then set the validation 
        *        only if the min and max are equal (typically both are 0).
        * 3. Otherwise don't set the validation at all.
        */
      int vtype= getValidationType(new PropDbFieldDefSource(propName));
      if (vtype >= 0 ) {
            setValidationType( vtype );
      }
      //else if (_min == null || _max == null) {
      //      setValidationType(ActionConst.NO_VALIDATION);
      //}
      //else if (_min.equals(_max)) {
      else if ( ComparisonUtil.equals(_min,_max) ) {
            setValidationType(ActionConst.NO_VALIDATION);
      }

    }

    public Object clone() {
       DateTimeAction ia= (DateTimeAction)clone();
       return ia;
    }


    /**
     * Set the value of the DateTimeAction and validate by doing range
     * checking.
     */
    public void setValue(Date v){// should throw a value out of range excep.
        //System.out.println("In setValue(Date) : " + v);
        if (goodValue(v))  setValueDontValidate(v);
    }

    /**
     * Set the value of the DateTimeAction and validate by doing range
     * checking.
     */
    public void setValue(String s){// should throw a value out of range excep.
                                   // and possibly WrongDateTimeFormatException
        Date d = stringToDate(s);
        if (goodValue(d))  setValueDontValidate(d);
    }

    /**
     * Set to the default value or null
     */
    protected void setToDefault() {
         setValueDontValidate(_mayDoNull ? null: _def);
    }

    /**
     * Set the value of the DateTimeTextField but don't do any validation.
     */
    public void setValueDontValidate(Date v) {
         Date   newValue= v;
         String newValueStr= "";

         if (isNull(v)) {
              newValueStr= "";
         }
         else {
              newValueStr= dateToString(v);
         }

         _inputContainer.setCurrentString(newValueStr);

         _propChange.firePropertyChange ( "valueString", _lastValueStr,
                                          newValueStr);
         _propChange.firePropertyChange ( "value", _lastValue, newValue);

         _lastValue   = newValue;
         _lastValueStr= newValueStr;
         _inputContainer.setCurrentString(newValueStr);
    }

    //public void setValueDontValidate(String s) {
    //    String old= _inputContainer.getCurrentString();
    //    if (s == null)
    //       _inputContainer.setCurrentString("");
//	else
    //       _inputContainer.setCurrentString(s );
//
    //    _propChange.firePropertyChange ( "valueString", old, 
    //                                   _inputContainer.getCurrentString() );
    //}

    protected boolean isNull(Date v) { return v == null	; }

    /**
     * Get the Date value of the DateTimeTextField in Date object
     */
    private Date getValueFromString(String ts) {
        Date v = null;
        if (ts==null || ts.length() == 0) {
            v= null;
        }
        else {
            v = stringToDate(ts);
        }

        if (v == null && ! _mayDoNull)
            v = _def;
        return v;
    }
    /**
     * Get the value of the DateTimeTextField in Date object
     */
    public Date getValue() {
         Date v;
         v= getValueFromString(_inputContainer.getCurrentString());
         return v;
    }

   public Date getDate() {
        return getValue();
    }

    public int getYear() {
        int year = -1;
        Date date = getDate();
        if (date != null) {
           _cal.setTime(date);
           year = _cal.get(Calendar.YEAR);
           }
         return year;
    }

    public int getDayOfYear() {
        int day = -1;
        Date date = getDate();
        if (date != null) {
           _cal.setTime(date);
           day = _cal.get(Calendar.DAY_OF_YEAR);
           }
         return day;
    }

    public int getHourOfDay() {
        int hour = 0;
        Date date = getDate();
        if (date != null) {
           _cal.setTime(date);
           hour = _cal.get(Calendar.HOUR_OF_DAY);
           }
         return hour;
    }

    public int getMinute() {
        int min = 0;
        Date date = getDate();
        if (date != null) {
           _cal.setTime(date);
           min = _cal.get(Calendar.MINUTE);
           }
         return min;
    }

    public int getSecond() {
        int sec = 0;
        Date date = getDate();
        if (date != null) {
           _cal.setTime(date);
           sec = _cal.get(Calendar.SECOND);
           }
         return sec;
    }

   /**
       get the hour, min, sec and then
       convert them into decimal time of Day 
       formula (hour+min/60 + sec/3600)/24 
     */

    public float getFractionalDay() {
       float h = (float)getHourOfDay();
       float m = (float)getMinute();
       float s = (float)getSecond();

       return (h + m/60.0F + s/3600.0F)/24.0F;
       }



    public void acceptField() throws OutOfRangeException {
         String s= getValueString();
         try {
           Date date= validate(s);
           //if (s != null && !StringUtil.isSpaces(s)) {
           //     ss = formatDateString(s);
           //}
           setValueDontValidate(date);
         } catch (OutOfRangeException e) {
             setToDefault();
             throw e;
         } catch (WrongDateTimeFormatException e) {
             setToDefault();
             OutOfRangeException e1= new OutOfRangeException(
                                                   e.getMessage(), false,
                                                   _lastValueStr);
             throw e1;
         }
    }

    public Date validate(Date v) throws OutOfRangeException {
           if (!goodValue(v)) {
               throw new OutOfRangeException(getUserErrorString());
           }
           return v;
    }

    public Date validate(String s) throws OutOfRangeException,
                                          WrongDateTimeFormatException{
           if ( (s==null || s.trim().length() == 0) && _mayDoNull)
              return null;
           Date v = stringToDate(s);
           if (v == null)
              throw new WrongDateTimeFormatException(
                  "input format is not accepted :" + s);
           if (!goodValue(v)) {
               throw new OutOfRangeException(getUserErrorString());
           }
           return v;
    }

    protected String getUserErrorString() {
       String errString= null;
       if (_formatError != null)
          errString = super.getErrorDescription() + _formatError;
       else
          errString = super.getUserErrorString();

       return errString;
    }

    public boolean goodValue(String s) {
        boolean retval = false;
        Date date;
        _formatError = null;
        if (_mayDoNull && (s == null || StringUtil.isSpaces(s))) {
           retval= true;
        }
        else if (!_mayDoNull && (s == null || StringUtil.isSpaces(s))) {
           _formatError = " should not be empty";
           retval = false;
        }
        else if (s != null && !StringUtil.isSpaces(s)) {
           date = stringToDate(s);
           if (date == null) {
              _formatError = FORMAT_ERROR +s;
              retval = false;
              }
           else {
              retval = goodValue(date);
           }
        }

        return retval;
    }

    public boolean goodValue(Date v) {
           boolean retval= false;
           int vtype= getValidationType();
           if (_mayDoNull && isNull(v)) {
                   retval= true;
           }
           else if (!_mayDoNull && isNull(v)) {
                   retval= false;
           }
           else if (vtype == ActionConst.NO_VALIDATION) {
                   retval= true;
           }
           else if (vtype == ActionConst.MIN_VALIDATION) {
                   if (v.compareTo(_min) >= 0) retval= true;
           }
           else if (vtype == ActionConst.MAX_VALIDATION) {
                   if (v.compareTo(_max) <= 0) retval= true;
           }
           else if (vtype == ActionConst.RANGE_VALIDATION) {
                   if (v.compareTo(_min) >= 0 && v.compareTo(_max) <= 0)
                      retval= true;
           }
           return retval;
    }




       /**
        * Set the minimum value for validation.
        * @param min the minimum value this field can contain.
        */
       public void setMin(Date min)     {
           _min= min;
           _minString = dateToString(min);
           }
       /**
        * Get the minimum value for validation.
        */
       public Date  getMin()            { return _min; }

       /**
        * Set the minimum value for validation.
        * @param minString the minimum value this field can contain.
        */
       public void setMin(String minString)     {
          Date d = stringToDate(minString);
          if (d != null) {
              setMin(d);
              }
           }

       /**
        * Get the minimum value for validation as a formatted String.
        */
       public String getMinString()      { return _minString; }

       public String getValueAsString(Date v) {
          return dateToString(v);
       }

       /**
        THis is defined only to satisfy the declaration in TextAction.java 
        so this class is not abstract.
        */
       public String getValueAsString(Number v) {
          return "";
       }

       /**
        * Set the maximum value for validation.
        * @param max the maximum value this field can contain.
        */
       public void setMax(Date max)     {
           _max= max;
           _maxString = dateToString(_max);
           }
       /**
        * Get the maximum value for validation.
        */
       public Date  getMax()            { return _max; }

       /**
        * Set the maximum value for validation.
        * @param s the maximum value this field can contain.
        */
       public void setMax(String s)     {
           Date d = stringToDate(s);
           if (d != null)
              setMax(d);
           }
       /**
        * Get the maximum value for validation as a formatted String.
        */
       public String getMaxString()      { return _maxString; }

       /**
        * Set the default value for validation.
        * @param def the default value the is put in the field when the
        *      user enters one that is out of range
        */
       public void setDefault(Date def) {
           _def= def;
           _defString = dateToString(_def);
           }
       /**
        * Get the default value for validation.
        */
       public Date  getDefault()        { return _def; }
       /**
        * Set the default value for validation.
        * @param s the default value the is put in the field when the
        *      user enters one that is out of range
        */
       public void setDefault(String s) {
           Date d = stringToDate(s);
           if (d!= null)
              setDefault(d);
           }
       public String  getDefaultString()        { return _defString; }

       /**
        * set Enabled/Disabled int field having null values. When a null
        * value is enabled the field may be empty.
        * @param b Enabled/Disabled doing nulls
        */
       public void setNullAllowed(boolean b) { _mayDoNull= b; }

       /**
        * get Enabled/Disabled int field having null values. When a null
        * value is enabled the field may be empty.
        * @return boolean Enabled/Disabled doing nulls
        */
       public boolean isNullAllowed() { return _mayDoNull; }




       /**
        *  if input Date is null, return null
        *  otherwise return a string representing the date
        */
       public String dateToString(Date v) {
          String retval = null;
          if (v != null) {
             StringBuffer append = new StringBuffer();
             FieldPosition fpos = new FieldPosition(0);
             StringBuffer sb = new StringBuffer();
             sb = _standardFormat.format(v, append, fpos);
             retval = sb.toString();
             }

              /*
               if (v != null)
                  System.out.println("XW DateTostringe  input: " +
                  v + "   output: " +retval);
               */
          return retval;
       }

       /**
        *  if the input string is not right format, return null
        *  otherwise return a Date object representing the string
        */
       public Date stringToDate(String strIn) {
           Date         date = null;
           int           loc = 0;
           int        length = _acceptedFormats.length;
           String          v = StringUtil.crunch(strIn);
           ParsePosition pos = new ParsePosition(0);

           if (v==null || v.length() == 0) return null;
           while (loc<length && date == null) {
              date = _acceptedFormats[loc].parse(v, pos);
              loc++;
              }

          return date;
       }

    protected DateFormat getStandardFormat() {
        return _standardFormat;
    }

    protected DateFormat[] getAcceptedFormats() {
        return _acceptedFormats;
    }
      /*
      This one only used when the String is a valid date/time string
      */
//      private String formatDateString(String s) {
//         Date d = stringToDate(s);
//         return dateToString(d);
//      }

    // ------------        private methods  ----------------------/
    private void defineStandardFormat() {
     _standardFormat = new SimpleDateFormat(_standardFormatString);
     _standardFormat.setLenient(false);
     _standardFormat.setTimeZone(GMT_TIME_ZONE);
    }

    private void defineAcceptedFormats(boolean usingDefaults) {
        int length = _acceptedFormatStrings.length;
        int adjust = 0;
        boolean isDate= (this instanceof DateAction);
        if (usingDefaults && isDate) {
            adjust=_localizedAcceptedFormats.length+1;
            _acceptedFormats =  new SimpleDateFormat[length+adjust];
            _acceptedFormats[0]= _standardFormat;
            _acceptedFormats[0].setLenient(true);
            _acceptedFormats[0].setTimeZone(GMT_TIME_ZONE);
            for (int i=0; i<_localizedAcceptedFormats.length; i++) {
                _acceptedFormats[i+1] = _localizedAcceptedFormats[i];
                _acceptedFormats[i+1].setLenient(false);
                _acceptedFormats[i+1].setTimeZone(GMT_TIME_ZONE);
            }
        }
        else {
            _acceptedFormats =  new SimpleDateFormat[length];
        }

        for (int i=adjust; i<length+adjust; i++) {
            _acceptedFormats[i] = new SimpleDateFormat(
                                   _acceptedFormatStrings[i-adjust], Locale.US);
            _acceptedFormats[i].setLenient(false);
            _acceptedFormats[i].setTimeZone(GMT_TIME_ZONE);
        }

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
