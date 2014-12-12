package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.StringUtil;

import java.util.Date;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


/**
 * A subclass of DateTimeAction that suports entering Time values and 
 * checking.  Only some acceptible values may be entered in this field <p>
 *
 * @author Xiuqin Wu
 * @see DateTimeAction
 */
public class TimeAction extends DateTimeAction {

       private static final String _acceptedStrings [] =
                      {"HH:mm:ss",
                       "H:mm:ss",
                       "HH mm ss",
                       "H mm ss",
                       "HH:mm",
                       "H:mm",
                       "HH mm",
                       "H mm",
                       "HH",
                       "H", };

       private static final String _standardString = "HH:mm:ss";
       private static final String PROP = "TimeAction";

       public TimeAction(String propName, InputContainer inputContainer) {
            super(propName, _standardString, _acceptedStrings, inputContainer);
       }

       public TimeAction(String propName) {
            this(propName, null);
       }

       /**
         This is to overwrite the same method in DateTimeAction.
         It is basically allow any time to be a valid input
         Definitely needs more work here    -- Xiuqin Wu
       */
       public boolean goodValue(Date v) {

           boolean retval= true;
           int vtype= getValidationType();
           //System.out.println("goodValue isNull: " + isNull(v));
           if (isNullAllowed() && isNull(v)) {
                   retval= true;
           }
           else if (!isNullAllowed() && isNull(v)) {
                   retval= false;
           }

           return retval;

       }


    /**
     *  if the input string is not right format, return null
     *  otherwise return a Date object representing the string
     */
    public Date stringToDate(String strIn) {
        Date         date = null;
        int           loc = 0;
        DateFormat af[]= getAcceptedFormats();
        DateFormat sf= getStandardFormat();
        int        length = af.length;
        String          v = StringUtil.crunch(strIn);
        ParsePosition pos = new ParsePosition(0);

        if(v==null || v.length()==0) return null;

        date=sf.parse(v, pos);
        boolean doParse;
        if(date==null) {
            while(loc<length && date==null) {
                doParse= true;
                date=null;
                if (af[loc] instanceof SimpleDateFormat &&
                    ((SimpleDateFormat)af[loc]).toPattern().length()!=v.length() ) {
                    doParse=false;
                }
                if (doParse) date=af[loc].parse(v, pos);
                loc++;
            }
        }

        return date;
    }


    private class PatternMatch {
        public String  _pattern;
        public boolean _lengthMatchRequired;
        public PatternMatch(String pattern, boolean lengthMatchRequired) {
            _pattern= pattern;
            _lengthMatchRequired= lengthMatchRequired;
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
