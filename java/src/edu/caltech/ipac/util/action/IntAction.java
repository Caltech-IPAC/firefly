package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.PropDbFieldDefSource;
import edu.caltech.ipac.util.html.HtmlDocumentEntry;

import java.beans.PropertyVetoException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A class that validates a integer against a set of test.
 *
 * @author Trey Roby
 */
public class IntAction extends TextAction {
    private final static String PROP  = "IntAction";
    private final static ClassProperties _prop =
                             new ClassProperties(PROP, IntAction.class);
    private static final String DOT    = ".".intern();
    private int             _max       = Integer.MAX_VALUE;
    private int             _min       = Integer.MIN_VALUE;
    private int             _def       = 0;
    private boolean         _mayDoNull = false;
    private Integer         _lastValue;
    private String          _lastValueStr;
    private NumberFormat    _nf        = NumberFormat.getInstance();
    private static String _defaultPattern= null;
    private final static String NOT_FOUND = "&lt;Not Found&gt;";

    public IntAction(String propName,
                     InputContainer ic,
                     Properties alternatePdb) {
        this(new PropDbFieldDefSource(propName,alternatePdb), ic);
    }


    public IntAction(FieldDefSource fds, InputContainer ic) {
        super(fds, ic);
        if (fds!=null) initProperties(fds);
        init();
    }


    public IntAction(FieldDefSource fds) { this(fds,null);}

    public IntAction(String propName, InputContainer ic) {
        this(new PropDbFieldDefSource(propName),ic);
    }

    public IntAction(String propName) { this(new PropDbFieldDefSource(propName), null); }

    public static void setDefaultPattern(String pattern) {
        _defaultPattern= pattern;
    }

    public static String getDefaultPattern() {
        return _defaultPattern;
    }

    private void init() {
        if (getDefaultPattern()!=null) {
            ((DecimalFormat)_nf).applyPattern(getDefaultPattern());
        }
        setValueDontValidate(_def);
    }

     /**
      * Get some properties from the property database. Start by getting
      * properties from the super method and then add more to it.<br>
      * These are the properties search for here:
      * <ul>
      * <li>Default - the default value for this action
      * <li>Min - the minimum value for this action
      * <li>Max - the maximum value for this action
      * <li>NullAllowed - this action may have a null value. Otherwise this 
      *                   action always has a value. False by default.
      * </ul>
      */
//    protected void getProperties(String propName) {
//      super.getProperties(propName);
//      _def=    AppProperties.getIntPreference(
//                    propName +DOT+ ActionConst.DEFAULT , _def, getPdb());
//      _min=    AppProperties.getIntProperty(
//                    propName +DOT+ ActionConst.MIN , _min, getPdb() );
//      _max=    AppProperties.getIntProperty(
//                    propName +DOT+ ActionConst.MAX , _max , getPdb());
//      _mayDoNull= AppProperties.getBooleanProperty(
//                    propName +DOT+ ActionConst.NULL_ALLOWED, _mayDoNull , getPdb());
//      if (_mayDoNull) _def= ActionConst.INT_NULL;
//
//       /*
//        * 1. Get the validation type from a property if it exist
//        *        and set it on the field
//        * 2. If there is no property then set the validation
//        *        only if the min and max are equal (typically both are 0).
//        * 3. Otherwise don't set the validation at all.
//        */
//      int vtype= getValidationType(propName);
//      if (vtype >= 0 ) {
//            setValidationType( vtype );
//      }
//      else if (_min == _max) {
//            setValidationType(ActionConst.NO_VALIDATION);
//      }
//      if (vtype == ActionConst.LIST_VALIDATION)  {
//         Integer validValues[];
//         String items= AppProperties.getProperty(
//                                propName +DOT+ ActionConst.ITEMS,
//                                null, getPdb());
//
//         if (items != null) {
//              validValues= StringUtil.strToIntegers(items);
//              setValidList(validValues);
//              int selected= AppProperties.getIntProperty(
//                    propName +DOT+ ActionConst.DEFAULT,
//                    ActionConst.INT_NULL, getPdb());
//              if (selected != ActionConst.INT_NULL) {
//                    setEquiliventItem(selected );
//              } // end if selected
//         } // end if items
//      } // end if vtype
//    }

    protected void initProperties(FieldDefSource fds) {
        super.initProperties(fds);
         _def=    AppProperties.getIntPreference(
                 fds.getName() +DOT+ ActionConst.DEFAULT , ActionConst.INT_NULL, getPdb());
         if (_def==ActionConst.INT_NULL) {
             _def= StringUtils.getInt(fds.getDefaultValue(),0);
         }
        _min= StringUtils.getInt(fds.getMinValue(),_min);
        _max= StringUtils.getInt(fds.getMaxValue(),_max);
        _mayDoNull= StringUtils.getBoolean(fds.isNullAllow(),_mayDoNull);
        if (_mayDoNull) _def= ActionConst.INT_NULL;

        /*
        * 1. Get the validation type from a property if it exist
        *        and set it on the field
        * 2. If there is no property then set the validation
        *        only if the min and max are equal (typically both are 0).
        * 3. Otherwise don't set the validation at all.
        */
        int vtype= getValidationType(fds);
        if (vtype >= 0 ) {
            setValidationType( vtype );
        }
        else if (_min == _max) {
            setValidationType(ActionConst.NO_VALIDATION);
        }
        if (vtype == ActionConst.LIST_VALIDATION)  {

            String items[]= fds.getItems();

            if (items != null) {

                Integer validValues[]= new Integer[items.length];
                for(int i=0; (i<validValues.length);i++) validValues[i]= StringUtils.getInt(items[i]);

                setValidList(validValues);
                int selected= StringUtils.getInt(fds.getDefaultValue(),ActionConst.INT_NULL);
                if (selected != ActionConst.INT_NULL) {
                    setEquiliventItem(selected );
                } // end if selected
            } // end if items
        } // end if vtype
    }

    public Object clone() {
        IntAction ia= (IntAction)clone();
        if (getDefaultPattern()!=null) {
            ((DecimalFormat)ia._nf).applyPattern(getDefaultPattern());
        }
        return ia;
    }


    /**
     * Set the value of the IntAction and validate by doing range
     * checking.
     * @param v the new value
     */
    public void setValue(int v){// should throw a value out of range excep.
        if (goodValue(v))  setValueDontValidate(v);
    }

    /**
     * Set to the default value or null
     */
    protected void setToDefault() {
         setValueDontValidate(_mayDoNull ? ActionConst.INT_NULL  : _def);
    }

    /**
     * Set the value of the IntTextField but don't do any validation.
     * @param v the new value
     */
    public void setValueDontValidate(int v) {
         Integer newValue= v;
         String newValueStr;

         if (_mayDoNull && isNull(v)) {
             newValueStr= "";
         }
         else {
             newValueStr=v + "";
         }

         _inputContainer.setCurrentString(newValueStr);

         if (getValidationType()== ActionConst.LIST_VALIDATION &&
             _validValues != null &&
             _validValues.length > 0) {
             setEquiliventItem( v );
         }

         _propChange.firePropertyChange ( VALUE_STR, _lastValueStr,
                                          newValueStr);
         _propChange.firePropertyChange ( VALUE, _lastValue, newValue);

         _lastValue   = newValue;
         _lastValueStr= newValueStr;
    }

    protected boolean isNull(int v) { return v == ActionConst.INT_NULL; }

    public int getValue() {
         int v;
         if (getValidationType()== ActionConst.LIST_VALIDATION) {
            v= (Integer)getSelectedItem();
         }
         else {
            v= getValueFromString(_inputContainer.getCurrentString());
         }
         return v;
    }

    public boolean isValueNull() { return isNull(getValue()); }

    /**
     * Get the value of the IntTextField
     * @param ts the string to pull the value from
     * @return the value
     */
    private int getValueFromString(String ts) {
        int v;
        try {
           if (ts==null || ts.length() == 0) {
               v= ActionConst.INT_NULL;
           }
           else {
               Number num= _nf.parse(ts);
               v= num.intValue();
           }
        } catch (ParseException ex) {
               v= _mayDoNull ? ActionConst.INT_NULL : _def;
        }
        return v;
    }

    public void acceptField() throws OutOfRangeException {
         try {
             int v= getValue();
             validate(v);
             setValueDontValidate(v);
         } catch (OutOfRangeException e) {
             setToDefault();
             throw new OutOfRangeException( e.getMessage(), e.isWarningOnly(),
                                            _lastValueStr);
         }
    }

    public void validate(int v) throws OutOfRangeException {
           if (!goodValue(v)) {
               throw new OutOfRangeException(getUserErrorString());
           }
           try {
               _vetoChange.fireVetoableChange("value", _lastValue,
                                              new Integer(v));
           } catch (PropertyVetoException e) {
               throw new OutOfRangeException(e.getMessage());
           }
    }

    public boolean goodValue(int v) {
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
                   if (v >= _min) retval= true;
           }
           else if (vtype == ActionConst.MAX_VALIDATION) {
                   if (v <= _max) retval= true;
           }
           else if (vtype == ActionConst.RANGE_VALIDATION) {
                   if ((v <= _max) && (v >= _min)) retval= true;
           }
           else if (vtype == ActionConst.LIST_VALIDATION) {
                retval= false;
                Number validValues[]= getValidList();
                if (validValues != null) {
                    for(int i=0; (i<validValues.length && !retval); i++) {
                        if (validValues[i].intValue() == v) retval= true;
                    }
                }
           }
           return retval;
    }




       /**
        * Set the minimum value for validation.
        * @param min the minimum value this field can contain.
        */
       public void setMin(int min)     { _min= min; }
       /**
        * Get the minimum value for validation.
        * @return the minimum value for range validation
        */
       public int  getMin()            { return _min; }

       /**
        * Get the minimum value for validation as a formatted String.
        */
       public String getMinString()      { return _nf.format(_min); }

       public String getValueAsString(Number n) {
          return _nf.format(n.intValue());
       }

       public String getValueAsString() {
          return _nf.format(getValue());
       }

       /**
        * Set the maximum value for validation.
        * @param max the maximum value this field can contain.
        */
       public void setMax(int max)     { _max= max; }
       /**
        * Get the minimum value for validation.
        * @return the maximum value for range validation
        */
       public int  getMax()            { return _max; }

       /**
        * Get the maximum value for validation as a formatted String.
        */
       public String getMaxString()      { return _nf.format(_max); }

       /**
        * Set the default value for validation.
        * @param def the default value the is put in the field when the
        *      user enters one that is out of range
        */
       public void setDefault(int def) { _def= def; }
       /**
        * get the default value
        * @return the default value
        */
       public int  getDefault()        { return _def; }

       public String getDefaultString()      { return _nf.format(_def); }

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
        *  Collect info for HTML document
        * @param entry the HtmlDocumentEntry entry
        */
       public void document(HtmlDocumentEntry  entry) {
          List<String> items = new ArrayList<String>(3);
          String description = ((getErrorDescription() != "") ?
                                getErrorDescription() : NOT_FOUND);
          if (description.equals(NOT_FOUND))
            description = ((getDescription() != "") ? getDescription() :
                           NOT_FOUND);

          if ((getMax() != Integer.MAX_VALUE) && (getMin() != Integer.MIN_VALUE)) {
             items.add(_prop.getName("minValue") + " " + getMinString());
             items.add(_prop.getName("maxValue") + " " + getMaxString());
             items.add(_prop.getName("defaultValue") + " " +
                       getDefaultString(getMax(), getMin(), getDefault()));
             entry.insertEntry(description, items);
          }
          else if (getValidList().length > 0)  {
             items.add(_prop.getName("validValues") + " " + getValidValuesString());
             items.add(_prop.getName("defaultValue") + " " +
                       getDefaultString(getValidList(), getDefault()));
             entry.insertEntry(description, items);
          }
          else if (!description.equals(NOT_FOUND)) {
             items.add(NOT_FOUND);
             entry.insertEntry(description, items);
          }
       }



    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof IntAction) {
            IntAction ia= (IntAction)other;
            if (super.equals(other) &&
                _max       == ia._max &&
                _min       == ia._min &&
                _def       == ia._def &&
                _mayDoNull == ia._mayDoNull &&
                getValue() == ia.getValue()) {
                retval= true;
            }
        }
        return retval;
    }




       private String getDefaultString(int max, int min, int def) {
          return (((def <= max) && (def >= min)) ? getDefaultString() :
                  NOT_FOUND);
       }

       private String getDefaultString(Number[] values, int def) {
           for(Number v : values) {
             if (v.intValue() == def) return getDefaultString();
           }
          return NOT_FOUND;
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
