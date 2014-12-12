package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.FormatFloat;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.PropDbFieldDefSource;
import edu.caltech.ipac.util.html.HtmlDocumentEntry;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * A class that validates a float against a set of test.
 *
 * @see TextAction
 * @see ActionConst
 * @see java.text.NumberFormat
 *
 * @author Trey Roby
 */
public class FloatAction extends TextAction {
    private final int MAX_PRECISION = 6; //IEEE 754
    //private final int MIN_PRECISION = 3; //engineering notation
    private final static String PROP  = "FloatAction";
    private final static ClassProperties _prop =
                                 new ClassProperties(PROP, FloatAction.class);
    private static final String EXP_PATTERN= "0.###E0";
    private static final String DOT =         ".".intern();
    private static final float        DEF_DEFAULT= 0.0F;
    private float        _max       = Float.MAX_VALUE;
    private float        _min       = Float.MIN_VALUE;
    private float        _def       = DEF_DEFAULT;
    private NumberFormat _nf        = NumberFormat.getInstance();  //I18N compliant
    private NumberFormat _nfExp     = NumberFormat.getInstance();
    private boolean      _mayDoNull = false;
    private boolean      _expAllowed= true;
    private boolean      _alwaysDisplayExp= false;
    private String       _pattern   = null;
    private int          _precision = 2;
    private Float        _lastValue;
    private String       _lastValueStr;
    private float        _incrementValue= 0.0F;
    private static String _defaultPattern= null;
    private final static String NOT_FOUND = "&lt;Not Found&gt;";

    /**
     * Construct a float action base on a property name.
     */
    public FloatAction(String propName,
                       InputContainer ic,
                       Properties alternatePdb) {
        this(new PropDbFieldDefSource(propName, alternatePdb), ic);
    }


    public FloatAction(String propName) { this(new PropDbFieldDefSource(propName), null); }

    public FloatAction(FieldDefSource fds) { this(fds,null); }

    public FloatAction(FieldDefSource fds, InputContainer ic) {
        super(fds,ic);
        if (fds != null)  initProperties(fds);
        init();
    }

    private void init() {
        if (getDefaultPattern()!=null) {
            ((DecimalFormat)_nf).applyPattern(getDefaultPattern());
        }
        ((DecimalFormat)_nfExp).applyPattern(EXP_PATTERN);
        if (_pattern == null) setPrecision(_precision);
        else                ((DecimalFormat)_nf).applyPattern(_pattern);
        setValueDontValidate(_def);
    }


    public static void setDefaultPattern(String pattern) {
        _defaultPattern= pattern;
    }

    public static String getDefaultPattern() {
        return _defaultPattern;
    }


     /**
      * Get some properties from the property database. Start by getting
      * properties from the super method and then add more to it.<br>
      * These are the properties search for here:
      * <ul>
      * <li>Default - the default value for this action
      * <li>Min - the minimum value for this action
      * <li>Max - the maximum value for this action
      * <li>Precision - how many decimal places this action can have.
      * <li>Pattern - overrides precision. What the number looks like to the
      *               user.  The pattern is directly passed to the NumberFormat
      *               class to control how this number is displayed.
      * <li>NullAllowed - this action may have a null value. Otherwise this 
      *                   action always has a value. False by default.
      * <li>ScientificAllowed -  The use may enter scientific notation.
      *                          True by default.
      * </ul>
      */
    protected void initProperties(FieldDefSource fds) {
         super.initProperties(fds);
         String defProp= fds.getName() +DOT+ ActionConst.DEFAULT ;
         String defStr=AppProperties.getPreference(defProp,null, getPdb()); // get a glimps of how it is formated

         if (defStr==null || Float.isNaN(StringUtils.getFloat(defStr))) defStr= fds.getDefaultValue();
         _def= StringUtils.getFloat(defStr,DEF_DEFAULT );


         _min= StringUtils.getFloat(fds.getMinValue(),_min);
         _max= StringUtils.getFloat(fds.getMaxValue(),_max);
         _precision= StringUtils.getInt(fds.getPrecision(),_precision);
         _pattern= fds.getPattern();
         _mayDoNull= StringUtils.getBoolean(fds.isNullAllow(),_mayDoNull);
         _expAllowed= StringUtils.getBoolean(fds.getSciNotation(),_expAllowed);
         _incrementValue=   StringUtils.getFloat(fds.getIncrement(),0.0F);
         if (_incrementValue> 0.0) setupIncrementValidator();
         if (_mayDoNull) _def= ActionConst.FLOAT_NULL;

         if (defStr!=null && _expAllowed) { //if the default is an exponent the force that as the format
             try {
                 Float.parseFloat(defStr); // just make sure it is a valid string
                 _alwaysDisplayExp=
                         (defStr.indexOf('e') > -1 || defStr.indexOf('E') > -1 );
             } catch (NumberFormatException e) {
                 // do nothing, the string was not valid and was are using defaults
             }
         }


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
              FormatFloat validValues[]= StringUtil.strToFormatFloats(items,_nf);
              setValidList(validValues);
              float selected= StringUtils.getFloat(fds.getDefaultValue(),ActionConst.FLOAT_NULL);
              if (selected != ActionConst.FLOAT_NULL) {
                  setEquiliventItem(new FormatFloat(selected,_nf) );
              } // end if selected

          }
      } // end if vtype
    }

    public Object clone() {
       FloatAction fa= (FloatAction)clone();
       fa._nf= NumberFormat.getInstance();
       if (getDefaultPattern()!=null) {
           ((DecimalFormat)fa._nf).applyPattern(getDefaultPattern());
       }
       if (_pattern == null) {
            fa.setPrecision(_precision);
       }
       else {
          fa._pattern= _pattern;
          ((DecimalFormat)_nf).applyPattern(_pattern);
       }
       return fa;
    }

    /**
     * Set the precision, in other words, the number of decimal places of this float.
     * @param p number of decimal places
     */
    public void setPrecision(int p) {
       _precision= p;
       _nf.setMaximumFractionDigits(p);
       _nf.setMinimumFractionDigits(p);
       setScientificNotationPrecision(p); 
    }

    public void setScientificNotationPrecision(int p) {
        //TLau 09/29/2010: format scientific notation with user-defined precision digits
        if (p>_nfExp.getMaximumFractionDigits()+1) {
            _nfExp.setMaximumFractionDigits(p-2);
        }
        if (_nfExp.getMaximumFractionDigits() > MAX_PRECISION) {
            _nfExp.setMaximumFractionDigits(MAX_PRECISION);
        }
    }
    /**
     * Set the value of the FloatAction and validate by doing range
     * checking.
     * @param v the new value
     */
    public void setValue(float v){// should throw a value out of range excep.
        if (goodValue(v))  setValueDontValidate(v);
    }

    /**
     * Set to the default value or null
     */
    public void setToDefault() {
         setValueDontValidate(_mayDoNull ? Float.NaN  : _def);
    }

    /**
     * Set the value of the FloatAction but don't do any validation.
     * @param v the new value
     */
    public void setValueDontValidate(float v) {
         Float  newValue= v;
         String newValueStr;

         if (_mayDoNull && isNull(v)) {
             newValueStr= "";
         }
         else {
             if ((_alwaysDisplayExp || _inputContainer.getUserEnteredExponent())
                 && _expAllowed) {
                 String tmp= String.format(Locale.US,"%."+_precision+"f", v);     // do this two lines to control precision
                 float newV= Float.parseFloat(tmp);  // should never have exception
                 newValueStr= _nfExp.format(newV);
                 //TLau 09/29/2010: format string with user-defined precision
                 //NOTE: _nf.format() requires Double; thus don't rely on Float-Double casting!
                 tmp = newValue.toString();
                 newValueStr = _nfExp.format(Double.parseDouble(tmp));
             }
             else {
                 newValueStr= _nf.format(v);
                //TLau 09/29/2010: NOTE: _nf.format() requires Double; thus don't rely on Float-Double casting!
                newValueStr= _nf.format(Double.parseDouble(newValue.toString()));
             }
         }

         _inputContainer.setCurrentString(newValueStr);

         if (getValidationType()== ActionConst.LIST_VALIDATION &&
             _validValues != null && _validValues.length > 0) {
                    setEquiliventItem( new FormatFloat(v,_nf) );
         }

        if (_lastValueStr != null && newValueStr != null && _lastValueStr.equals(newValueStr) && _lastValue!=newValue) {
            _propChange.firePropertyChange ( VALUE_STR, _lastValueStr+"0",
                                          newValueStr);
         _propChange.firePropertyChange ( VALUE, _lastValue, newValue);
        } else {
         _propChange.firePropertyChange ( VALUE_STR, _lastValueStr,
                                          newValueStr);
         _propChange.firePropertyChange ( VALUE, _lastValue, newValue);
        }

         _lastValue   = newValue;
         _lastValueStr= newValueStr;
    }

    protected boolean isNull(float v) { return Float.isNaN(v); }


    public float getValue() {
         return getValueFromString(_inputContainer.getCurrentString());
    }

    public boolean isValueNull() { return isNull(getValue()); }

    /**
     * Get the value of the FloatAction.
     * @param ts the string to pull the value from
     * @return the value
     */
    private float getValueFromString(String ts) {
        float v;
        if (getValidationType()== ActionConst.LIST_VALIDATION) {
            v= ((FormatFloat)getSelectedItem()).floatValue();
        }
        else {
           try {
              if (ts==null || ts.length() == 0) {
                  v= ActionConst.FLOAT_NULL;
              }  // end if
              else {
                  if ( ts.charAt(0) == '+' ) ts= ts.substring(1);
                  ts= ts.toUpperCase();
                  Number num= _nf.parse(ts);
                  v= num.floatValue();
              } // end else
           } catch (ParseException ex) {
                  v= _mayDoNull ? ActionConst.FLOAT_NULL  : _def;
           } // end catch
        } // end else
        return v;
    }

    public void acceptField() throws OutOfRangeException {
         try {
             float v= getValue();
             validate(v);
             setValueDontValidate(v);
         } catch (OutOfRangeException e) {
             setToDefault();
             throw new OutOfRangeException( e.getMessage(), e.isWarningOnly(),
                                            _lastValueStr);
         }
    }

    public void validate(float v) throws OutOfRangeException {
           if (!goodValue(v)) {
              throw new OutOfRangeException(getUserErrorString());
           }
           try {
              _vetoChange.fireVetoableChange("value", _lastValue, new Float(v));
           } catch (PropertyVetoException e) {
              throw new OutOfRangeException(e.getMessage());
           }
    }

    private boolean goodValue(float v) {
           boolean retval= false;
           int vtype= getValidationType();
           if (_mayDoNull && isNull(v)) {
                   retval= true;
           }
           else if (!_mayDoNull && isNull(v)) {
               retval = false;
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
                        if (validValues[i].floatValue() == v) retval= true;
                    }
                }
           }
           return retval;
    }


    private void setupIncrementValidator() {
        this.addVetoableChangeListener(new VetoableChangeListener() {
            public void vetoableChange(PropertyChangeEvent ev)
                           throws PropertyVetoException {
                float value=(Float) ev.getNewValue();
                int workingInc=(int)(_incrementValue*
                                     Math.pow(10.0, _precision));
                int workingValue=(int) (value*
                                        Math.pow(10.0, _precision));
                if((workingValue % workingInc)!=0) {
                    String err=getErrorDescription()+" "+
                               _prop.getError("increment")+
                               _nf.format(_incrementValue);
                    throw new PropertyVetoException(err, ev);
                }
            }

        });

    }

       /**
        * Set the minimum value for validation.
        * @param min the minimum value this field can contain.

        */
       public void setMin(float min)     { _min= min; }
       /**
        * Get the minimum value for validation.
        * @return the minimum value for range validation
        */
       public float  getMin()            { return _min; }

       /**
        * Get the minimum value for validation as a formatted String.
        */
       public String getMinString()      {
           if ((_min>0) && (_min>100000 || _min<.0000001)) {
               return  _nfExp.format(_min);
           }
           else if ((_min<0) && (_min<-100000 || _min>.0000001)) {
               return  _nfExp.format(_min);
           }
           else {
               return  _nf.format(_min);
           }
       }

       public String getValueAsString(Number n) {
          return _nf.format(n.floatValue());
       }

       public String getValueAsString() {
          return _nf.format(getValue());
       }

       /**
        * Set the maximum value for validation.
        * @param max the maximum value this field can contain.
        */
       public void setMax(float max)     { _max= max; }
       /**
        * Get the maximum value for validation.
        * @return the maximum value for range validation
        */
       public float  getMax()            { return _max; }

       /**
        * Get the maximum value for validation as a formatted String.
        */
       public String getMaxString()      {
           if (_max>100000 || _max<.0000001) {
               return  _nfExp.format(_max);
           }
           else {
               return  _nf.format(_max);
           }
       }

       /**
        * Set the default value for validation.
        * @param def the default value the is put in the field when the
        *      user enters one that is out of range
        */
       public void setDefault(float def) { _def= def; }
       /**
        * get the default value
        * @return the default value
        */
       public float  getDefault()        { return _def; }

       public String getDefaultString()      { return _nf.format(_def); }

       /**
        * set Enabled/Disabled float field having null values. When a null
        * value is enabled the field may be empty.
        * @param b Enabled/Disabled doing nulls
        */
       public void setNullAllowed(boolean b) { _mayDoNull= b; }

       /**
        * get Enabled/Disabled float field having null values. When a null
        * value is enabled the field may be empty.
        * @return boolean Enabled/Disabled doing nulls
        */
       public boolean isNullAllowed() { return _mayDoNull; }

       /**
        *  Collect info for HTML document
        * @param entry the HtmlDocumentEntry  entry
        */
       public void document(HtmlDocumentEntry  entry) {
          List<String> items = new ArrayList<String>(4);
          String description = ((getErrorDescription() != "") ?
                                getErrorDescription() : NOT_FOUND);
          if (description.equals(NOT_FOUND))
             description = ((getDescription() != "") ? getDescription() :
                            NOT_FOUND);

          if ((getMax()!=Float.MAX_VALUE) && (getMin()!=Float.MIN_VALUE)) {
             items.add(_prop.getName("minValue") + " "  + getMinString());
             items.add(_prop.getName("maxValue") + " "  + getMaxString());
             items.add(_prop.getName("defaultValue") + " "  +
                       getDefaultString(getMax(), getMin(), getDefault()));
             items.add(_prop.getName("Precision") + " "  +
                       String.valueOf(_precision));
             entry.insertEntry(description, items);
         }
         else if (getValidList() != null && getValidList().length > 0)  {
             items.add(_prop.getName("validValues") + " " +
                       getValidValuesString());
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
        else if (other!=null && other instanceof FloatAction) {
            FloatAction ia= (FloatAction)other;
            if (super.equals(other) &&
                //_max       == ia._max &&
                //_min       == ia._min &&
                //_def       == ia._def &&
                ComparisonUtil.equals(_max,ia._max) &&
                ComparisonUtil.equals(_min,ia._min) &&
                ComparisonUtil.equals(_def, ia._def) &&                                                                   
                _precision == ia._precision &&
                _mayDoNull == ia._mayDoNull &&
                ComparisonUtil.equals(getValue(),ia.getValue())) {
                retval= true;
            }
        }
        return retval;
    }



       private String getDefaultString(float max, float min, float def) {
           return (((def <= max) && (def >= min)) ? getDefaultString() :
                   NOT_FOUND);
        }

        private String getDefaultString(Number[] values, float def) {
            for(Number v : values) {
                if (v.floatValue() == def) return getDefaultString();
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
