package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.PropDbFieldDefSource;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This class is the base class for the FloatAction, DoubleAction, and 
 * IntAction.  All these actions do validation of a value within a range or
 * validation of a 1 of many values.  This class provides all the elements 
 * common to floats, ints, and doubles.  Through these classes contain methods
 * to configure them; the typical use is to define properties in a property
 * file that is loaded in the property database.  At initialization this class
 * searches the property database for specific properties and the configures
 * itself.  All properties are build from the property passed to the 
 * constructor.
 * 
 * These are the types of validation that can be done:
 * <ul>
 * <li>range - the value must be between a min and max inclusive.
 * <li>min   - the value must greater than a minimum value
 * <li>max   - the value must lass than a maximum value
 * <li>list  - the value must be a specific value for a list of possible values.
 * <li>none  - no validation, any value is valid.
 * </ul>
 *
 * @see FloatAction
 * @see IntAction
 * @see DoubleAction
 * @see ActionConst
 * 
 * @author Trey Roby
 */
public abstract class TextAction implements ComboBoxModel {


    public static final String VALUE = "value";
    public static final String VALUE_STR = "valueString";

    protected PropertyChangeSupport _propChange=
                                new PropertyChangeSupport(this);
    protected VetoableChangeSupport _vetoChange=
                                new VetoableChangeSupport(this);
    private   String _propName       = null;
    private   String _tip            = null;
    private   String _enableTip      = null;
    private   String _desc           = "";
    private   String _errorDesc      = "";
    private   String _errorDescUnits = "";
    protected Number _validValues[]  = null;
    private   Number _selected       = null;
    private   int    _validationType = ActionConst.RANGE_VALIDATION;
    private   List<ListDataListener>   _listDataList   =
                                      new ArrayList<ListDataListener>(3);
    private Properties  _alternatePdb;
    protected InputContainer  _inputContainer;

    protected static final String DEF_MIN_ERR_STRING  =
                                 "Value must be greater than or equal to ";
    protected static final String DEF_MAX_ERR_STRING  =
                                 "Value must be less than or equal to ";
    protected static final String DEF_RANGE_ERR_STRING=
                                 "Value must be between ";
    private static final String DEF_START_LIST_ERR_STRING=
                                 "Value must be one of the following";
    private final String MIN_ERR_STRING       = DEF_MIN_ERR_STRING;
    private final String MAX_ERR_STRING       = DEF_MAX_ERR_STRING;
    private final String RANGE_ERR_STRING     = DEF_RANGE_ERR_STRING;
    private final String START_LIST_ERR_STRING= DEF_START_LIST_ERR_STRING;

    public TextAction(String propName,
                      InputContainer ic,
                      Properties alternatePdb) {
        this(new PropDbFieldDefSource(propName,alternatePdb),ic);
    }

    public TextAction(String propName, InputContainer inputContainer) {
        this(propName,inputContainer,null);
    }


    public TextAction(FieldDefSource fds, InputContainer ic) {
        if (fds!=null) _propName= fds.getName();
        _alternatePdb= (fds instanceof PropDbFieldDefSource) ? ((PropDbFieldDefSource)fds).getPDB()  : null;
        _inputContainer= (ic == null) ? new SimpleInputContainer() : ic;
    }



    /**
     * return the current value as a string.
     */
    public String getValueString() {
         return _inputContainer.getCurrentString();
    }

     /*
      * Get some properties from the property database.<br>
      * These are the properties search for here.
      * <ul>
      * <li>Name - the use used for the label.
      * <li>ErrorDescription - The name for the action used in error messages.
      *                        If the property is not found then Name is used
      *                        in error messages.
      * <li>ShortDescrption - used for the tooltip
      * </ul>
      */
//    protected void getProperties(String propName) {
//      _desc=  AppProperties.getProperty( propName +"."+ Action.NAME, "",_alternatePdb);
//      _errorDesc=  AppProperties.getProperty(
//                        propName +"."+ ActionConst.ERROR_DESCRIPTION, _desc, _alternatePdb);
//      _errorDescUnits=  AppProperties.getProperty(
//                        propName +"."+ ActionConst.ERROR_DESCRIPTION_UNITS, "", _alternatePdb);
//      _tip =  AppProperties.getProperty(
//                       propName +"."+ Action.SHORT_DESCRIPTION, "", _alternatePdb);
//      _enableTip =  AppProperties.getProperty(
//                        propName +"."+ ActionConst.HOW_TO_ENABLE_TIP, null, _alternatePdb);
//    }


    protected void initProperties(FieldDefSource fds) {
        _desc=  StringUtils.getVal(fds.getTitle(), "");
        _errorDesc=  StringUtils.getVal(fds.getErrMsg(), _desc);
        _errorDescUnits=  StringUtils.getVal(fds.getErrorDescUnits(), "");
        _tip =  StringUtils.getVal(fds.getShortDesc(), "");
        _enableTip =  fds.getHowToEnableTip();
    }




    /**
     * Get the integer validation type constant from the property database.
     */
    protected int getValidationType(FieldDefSource source) {
          int retval= -99;
        String vtype= source.getValidationType();
//          String vtype= AppProperties.getProperty(
//                                 propName +"."+ ActionConst.VALIDATION,null, _alternatePdb );

          if (vtype != null) {
             if      (vtype.equalsIgnoreCase(ActionConst.RANGE_VALIDATION_STR))
                              retval= ActionConst.RANGE_VALIDATION;
             else if (vtype.equalsIgnoreCase(ActionConst.MIN_VALIDATION_STR))
                              retval= ActionConst.MIN_VALIDATION;
             else if (vtype.equalsIgnoreCase(ActionConst.MAX_VALIDATION_STR))
                              retval= ActionConst.MAX_VALIDATION;
             else if (vtype.equalsIgnoreCase(ActionConst.LIST_VALIDATION_STR))
                              retval= ActionConst.LIST_VALIDATION;
             else if (vtype.equalsIgnoreCase(ActionConst.NO_VALIDATION_STR))
                              retval= ActionConst.NO_VALIDATION;
          }
          return retval;
    }

    public Properties getPdb() { return _alternatePdb; }

    /**
     * return an array of numbers that are valid.
     */
   public Number [] getValidList() {
         return _validValues;
   }

    /**
     * set an array of numbers that are valid.
     */
   public void setValidList(Number validValues[]) {
         _validValues= validValues;
         if (_validValues != null && _validValues.length > 0)
                 _selected= _validValues[0];
   }

    public abstract void acceptField() throws OutOfRangeException;
    /**
     * return the mininum valid number as a string.
     */
    public abstract String   getMinString();
    /**
     * return the maximum valid number as a string.
     */
    public abstract String   getMaxString();
    /**
     * return the number as a string correctly formatted.
     */
    public abstract String   getValueAsString(Number n);

    /**
     * A Text description of this field used for Errors.
     */
    public void setErrorDescription(String s) { _errorDesc= s; }

    /**
     * Get the text description of this field used for Errors.
     */
    public String getErrorDescription() { return _errorDesc; }

    /**
     * A Text description of this field used for Errors.
     */
    public void setErrorDescriptionUnits(String s) { _errorDescUnits= s; }

    /**
     * Get the text description of this field used for Errors.
     */
    public String getErrorDescriptionUnits() { return _errorDescUnits; }

    /**
     * A Text description of this field.
     */
    public void setDescription(String s) { _desc= s; }

    /**
     * Get the text description of this field.
     */
    public String getDescription() { return _desc; }
    public String getPropName() { return _propName; }
    /**
     * Set the validation Method.
     * Validation may be done in one of several ways.
     * <ul>
     * <li>ActionConst.NO_VALIDATION- validation is turned off.
     * <li>ActionConst.MIN_VALIDATION- value must be greater than or 
     *                      equal than a minimum
     * <li>ActionConst.MAX_VALIDATION- value must be less 
                                     than or equal than a maximum
     * <li>ActionConst.RANGE_VALIDATION- value must be in a range
     * <li>ActionConst.LIST_VALIDATION- must be one of several values
     * </ul>
     * @param type one of the above constants
     *
     */
    public void setValidationType(int type) {
           if  (type == ActionConst.NO_VALIDATION    ||
                type == ActionConst.MIN_VALIDATION   ||
                type == ActionConst.MAX_VALIDATION   ||
                type == ActionConst.RANGE_VALIDATION ||
                type == ActionConst.LIST_VALIDATION )
                         _validationType= type;
           else
              System.out.println(
                "setValidationType: wrong type passed- type= "+ type);
    }


    /**
     * Get the validation Method.
     * Validation may be done in one of several ways.
     * <ul>
     * <li>ActionConst.NO_VALIDATION- validation is turned off.
     * <li>ActionConst.MIN_VALIDATION- value must be greater than or 
     *                      equal than a minimum
     * <li>ActionConst.MAX_VALIDATION- value must be less 
                                     than or equal than a maximum
     * <li>ActionConst.RANGE_VALIDATION- value must be in a range
     * <li>ActionConst.LIST_VALIDATION- must be one of several values
     * </ul>
     * @return int on of the above constants
     *
     */
    public int  getValidationType() { return _validationType; }

    /**
     * Get the string to describe this error
     */
    protected String getUserErrorString() {
        String errString= null;
        int vtype= _validationType;
        if      (vtype == ActionConst.MIN_VALIDATION)
                errString= _errorDesc + " " + MIN_ERR_STRING + getMinString() +
                           " " + _errorDescUnits;
        else if (vtype == ActionConst.MAX_VALIDATION)
                errString= _errorDesc + " " + MAX_ERR_STRING + getMaxString() +
                           " " + _errorDescUnits;
        else if (vtype == ActionConst.RANGE_VALIDATION)
                errString= _errorDesc + " " + RANGE_ERR_STRING +
                           getMinString() +" and "+ getMaxString() +
                           " " + _errorDescUnits;
        else if (vtype == ActionConst.LIST_VALIDATION) {
                StringBuffer outstr= new StringBuffer(100);
                outstr.append(_errorDesc);
                outstr.append(" ");
                outstr.append(START_LIST_ERR_STRING);
                if (!("".equals(_errorDescUnits))) {
                    outstr.append( " (" );
                    outstr.append(_errorDescUnits);
                    outstr.append( ")" );
                }
                outstr.append( ": " );
                outstr.append( getValidValuesString() );
                errString= outstr.toString();
        }
        else if (vtype == ActionConst.NO_VALIDATION) {
            errString="You must enter a value"; 
        }
        else
                Assert.tst(false,
                           "Bad option passed to showValidationError.");
        return errString;
    }

    public String getValidValuesString() {
       StringBuffer outstr= new StringBuffer(100);
       for(int i=0; (i<_validValues.length); i++) {
           outstr.append(" ");
           outstr.append(getValueAsString(_validValues[i]));
       }
       return outstr.toString();
    }


    /**
     * Get the Tool tip
     */
    public String getTip() { return _tip; }

    /**
     * Get the How to Enable tool tip
     */
    public String getHowToEnableTip() { return _enableTip; }


    /**
     * Set the Tool tip
     */
    public void setTip(String t) { _tip= t; }

    /**
     * Set the How to Enable tool tip
     */
    public void setHowToEnableTip(String t) { _enableTip= t; }

    public Object getSelectedItem() {
         return _selected;
    }

    public void setSelectedItem(Object anItem) {
         int i;
         for(i= 0; (i<_validValues.length); i++) {
                 if (anItem == _validValues[i]) break;
         }
         if (i>=_validValues.length) i=0;
         _selected= _validValues[i];
         ListDataEvent ev= new ListDataEvent(this,
                                             ListDataEvent.CONTENTS_CHANGED, -1, -1);
        List<ListDataListener> aClone = new ArrayList<ListDataListener>(_listDataList);
         for(ListDataListener listener: aClone) {
              listener.contentsChanged(ev);
         }
    }

    public void setEquiliventItem(Number anItem) {
         boolean found= false;
         int i;
         for(i=0; (i < _validValues.length  && !found); i++ ) {
              if (anItem.equals(_validValues[i]))
                     found= true;
         }
         if (found) setSelectedItem(_validValues[i-1]);
    }

    public Object getElementAt(int i) {
        return _validValues[i];
    }

    public int getSize() {
        int retval= 0;
        if (_validValues != null) retval= _validValues.length;
        return retval;
    }

    public void addListDataListener(ListDataListener l) {
      _listDataList.add(l);
    }
    public void removeListDataListener(ListDataListener l) {
      _listDataList.remove(l);
    }



    /**
     * Add a property changed listener.
     * @param p  the listener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       _propChange.addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p  the listener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       _propChange.removePropertyChangeListener (p);
    }

    /**
     * Remove all a property changed listeners.
     */
    public void removeAllPropertyChangeListeners() {
          // just replace the property change support object to an empty one.
       _propChange= new PropertyChangeSupport(this);
    }

    /**
     * Add a vetoable change listener.
     * @param p  the listener
     */
    public void addVetoableChangeListener (VetoableChangeListener p) {
       _vetoChange.addVetoableChangeListener (p);
    }

    /**
     * Remove a vetoable change listener.
     * @param p  the listener
     */
    public void removeVetoableChangeListener (VetoableChangeListener p) {
       _vetoChange.removeVetoableChangeListener (p);
    }


    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof TextAction) {
            TextAction ta= (TextAction)other;
            if (ComparisonUtil.equals(_propName, ta._propName ) &&
                ComparisonUtil.equals(_tip, ta._tip ) &&
                ComparisonUtil.equals(_enableTip, ta._enableTip ) &&
                ComparisonUtil.equals(_desc, ta._desc ) &&
                ComparisonUtil.equals(_errorDesc, ta._errorDesc ) &&
                ComparisonUtil.equals(_errorDescUnits, ta._errorDescUnits ) &&
                ComparisonUtil.equals(_selected, ta._selected) &&
                Arrays.equals(_validValues,ta._validValues) &&
                _validationType == ta._validationType) {
                retval= true;
            }
        }
        return retval;
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
