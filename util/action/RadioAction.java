package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.ItemFieldDefSource;
import edu.caltech.ipac.util.html.HtmlDocumentEntry;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * An Action for creating a set of Radio buttons.  This class stretches the
 * definition of action a little.  It is used to create a set of radio buttons
 * that are that are all in the same ButtonGroup.  The action defines all the
 * buttons that are in the group.  Therefore the whole groups of buttons 
 * can be thought of as one entity.  This action uses another Action subclass-
 * RadioActionElement.  Each RadioActionElement defines the individual Radio
 * buttons. <p>
 * This class must be subclassed to be used.
 * There are at least two ways to use it:
 * <ol>
 * <li>Subclass RadioAction and in the constructor define each
 *     RadioActionElement.
 * <li>Subclass RadioAction and in the constructor call useOptionsProperty.
 *     useOptionsProperty is tell RadioActionElement to the the GeneralAction
 *     defined <em>Options</em> property with will define all of the
 *     RadioActionElements from the property file.
 * </ol>
 * @see javax.swing.ButtonGroup
 * @see RadioActionElement
 * @see GeneralAction
 *
 * @author Trey Roby
 */
public class RadioAction extends    GeneralAction
                         implements RadioListener,
                                    ComboBoxModel,
                                    Iterable<RadioActionElement> {

    private static final String START_ERROR=
             "Value must be one of the following: ";

    protected List<RadioActionElement>     _radioActionList=
                           new ArrayList<RadioActionElement>(3);
    private   List<ListDataListener>     _listDataList   =
                                     new ArrayList<ListDataListener>(3);
    private   final boolean _saveStateAsPreference;

    /**
     * Create a new RadioActtion.
     * @param command the command name
     */
    public RadioAction(String command) {
        this(command,false,null);
    }

    public RadioAction(String command, boolean saveStateAsPreference) {
        this(command,saveStateAsPreference,null);
    }

    /**
     * Create a new RadioActtion.
     * @param command the command name
     */
    public RadioAction(String command,
                       boolean saveStateAsPreference,
                       Properties alternatePdb) {
        super(command,alternatePdb);
        useItemsProperty();
        _saveStateAsPreference= saveStateAsPreference;
    }


    /**
     * Create a new RadioActtion.
     * @param fds all the setting of the action
     */
    public RadioAction(FieldDefSource fds,
                       boolean saveStateAsPreference) {
        super(fds);
        useItemsProperty();
        _saveStateAsPreference= saveStateAsPreference;
    }


    public RadioAction(FieldDefSource fds) {
        this (fds,false);
    }

    /**
     * RadioAction will not used the Option property in GeneralAction by
     * default.  If you wish to use that property then call this routine.
     * @see RadioActionElement
     */
    public void useItemsProperty() {
           String items[]= getItems();
           if (items != null) {
               if (getRadioValue() == null) setRadioValue(items[0]);
               for (String item : items) addRadioString(item);
           }
    }

    /**
     * Create a new RadioActionElement with the passed command and add it to
     * the RadioAction.
     * @param command the command of the new RadioActionElement
     * @return RadioActionElement
     * @see RadioActionElement
     */
    public RadioActionElement addRadioString(String command) {

        if (isUsingFieldDefSource()) {
            FieldDefSource itemFds= new ItemFieldDefSource( command, getFieldDefSource());
            return new RadioActionElement( itemFds, this);
        }
        else {
            return new RadioActionElement(
                    getActionCommand() + "." + command, this, getAlternatePdb());
        }
    }


    /**
     * Get all the RadioActionElements associated with this RadioAction.
     * @return Iterator a Iterator through a list of RadioAcitonElements
     * @see RadioActionElement
     */
    public Iterator elementIterator() { return _radioActionList.iterator(); }

    public Iterator<RadioActionElement> iterator() { return _radioActionList.iterator(); }
    /**
     * Get the RadioActionElement at the i<em>th</em> position
     * @param i the position of the element
     * @return RadioActionElement the Element asked for.
     * @see RadioActionElement
     */
    public RadioActionElement getRadioActionElement(int i) {
           return _radioActionList.get(i);
    }

    /**
     * Called anytime a button is changed.
     * @param ev the event
     */
    public void actionPerformed(ActionEvent ev) {}


    /**
     * Called anytime a button changed. Must be defined in a subclass.
     * @param ev the event describing what happened.
     */
    public void radioChange(RadioEvent ev) {}

    /**
     * Enables of disables all the radio buttons associated with this action.
     * @param enable value to enable or disable this action
     */
    public void setEnabled(boolean enable) {
       Action a;
       for(Iterator i= _radioActionList.iterator(); (i.hasNext()); ) {
            a= (Action)i.next();
            a.setEnabled(enable);
        }
        super.setEnabled(enable);
    }


    public int setSelectedInt(int v) {
         boolean found= false;
         RadioActionElement action= null;
         Iterator i;
         for(i= _radioActionList.iterator(); (i.hasNext() && !found);){
              action= (RadioActionElement)i.next();
              if (action.getIntValue() == v) found= true;
         }
         if (found) {
            setSelectedItem(action);
         }
         else {
            System.out.println(
            "RadioAction.setSelectedInt: no item with this intValue: " +v);
         }

//---
       RadioActionElement rae= (RadioActionElement)getSelectedItem();
       return rae.getIntValue();
    }


    public int getSelectedInt() {
       RadioActionElement rae= (RadioActionElement)getSelectedItem();
       return rae.getIntValue();
    }

    public String getSelectedCommand() {
       RadioActionElement rae= (RadioActionElement)getSelectedItem();
       return rae.getRadioActionString();
    }

    /**
     * return the string the users sees
     * @return return the ReadioActionElement's Action.NAME
     */
    public String getSelectedCommandAsUserString() {
        RadioActionElement rae= (RadioActionElement)getSelectedItem();
        return rae.getName();
    }

    /**
     *  This method sets the radio value by passing it the string that the
     *  user actually sees not the command.  In otherwords the Action.NAME 
     *  of the command is passed.
     * @param anItem set to this value
     */
    public void setEquiliventItem(String anItem) {
        if (anItem==null) return;
        RadioActionElement foundAction= null;
        for(RadioActionElement action : _radioActionList) {
            if (anItem.equals(action.getName()) || anItem.equals(action.getActionCommand())) {
                foundAction= action;
                break;
            }
        }
        if (foundAction!=null) {
            setSelectedItem(foundAction);
        }
        else {
            System.out.println(
                    "RadioAction.setEquiliventItem: no equivalent item to : " + anItem);
        }
    }

    /**
     *  This method sets the radio value by passing it the string that the
     *  user actually sees not the command.  In otherwords the Action.NAME 
     *  of the command is passed.
     * @param anItem get to this value
     * @return the command string
     */
    public String getCommandName(String anItem) {
         boolean found= false;
         RadioActionElement action;
         Iterator i;
         String retval= null;
         for(i= _radioActionList.iterator(); (i.hasNext() && !found);){
              action= (RadioActionElement)i.next();
              if (action.getRadioActionString().equals(anItem)) {
                     found= true;
                     retval= action.toString();
              }
         }
         if (!found) {
            System.out.println(
            "RadioAction.getCommandName: no command for item : "+anItem);
         }
         return retval;
    }


   /**
    * Set the a radio button assoicated with the command to true.  This
    * method used with a RadioAction to change the current radio button.
    * @param command the current radio button
    * @see RadioAction
    */
   public void setRadioValue(String command) {
       changeRadio(null, command);
   }



//=========================================================================
//----------------------- Public Validation Methods -----------------------
//=========================================================================

    /**
     * Validate that a string is equal to one of the set of valid strings.  
     * If the string is not in the set then throw an exception.
     * @param v the string to validate
     * @throws OutOfRangeException if the string is not in the set of strings
     *                             then throw this exception.
     */
    public void validate(String v) throws OutOfRangeException {
       String             command;
       boolean            found = false;
       Iterator           i;
       RadioActionElement action;
       if (v != null) {
          for(i= _radioActionList.iterator(); (i.hasNext() && !found);) {
               action= (RadioActionElement )i.next();
               command= action.getRadioActionString();
               if (v.equals(command)) found= true;
          } // end loop
       } // end if v!= null
       if (!found) {
             String name= getName();
             StringBuffer outstr= new StringBuffer(100);
             if (name != null) {
                   outstr.append(name);
                   outstr.append(": ");
             }
             outstr.append(START_ERROR);
             for(i= _radioActionList.iterator(); (i.hasNext());) {
                action= (RadioActionElement)i.next();
                outstr.append(" ");
                outstr.append(action.getRadioActionString());
             }
             throw new OutOfRangeException(outstr.toString());
       } // end if !found
    }

    /**
     * Validate that a int is equal to one of the set of valid ints.  
     * If the int is not in the set then throw an exception.
     * @param v the string to validate
     * @throws OutOfRangeException if the int is not in the set of int
     *                             then throw this exception.
     */
    public void validate(int v) throws OutOfRangeException {
       int                iconst;
       boolean            found = false;
       Iterator           i;
       RadioActionElement action;
       for(i= _radioActionList.iterator(); (i.hasNext() && !found);) {
            action= (RadioActionElement )i.next();
            iconst= action.getIntValue();
            if (v == iconst) found= true;
        }
        if (!found) {
             String name= getName();
             StringBuffer outstr= new StringBuffer(100);
             if (name != null) {
                   outstr.append(name);
                   outstr.append(": ");
             }
             outstr.append(START_ERROR);
             for(i= _radioActionList.iterator(); (i.hasNext());) {
                action= (RadioActionElement)i.next();
                outstr.append(" ");
                outstr.append(action.getRadioActionString());
             }
             throw new OutOfRangeException(outstr.toString());
        }
    }

   public void document(HtmlDocumentEntry entry) {
       super.document(entry);
    }


//=========================================================================
//----------------- Methods from ComboBoxModel interface ------------------
//=========================================================================


    public void addListDataListener(ListDataListener l) {
      _listDataList.add(l);
    }
    public void removeListDataListener(ListDataListener l) {
      _listDataList.remove(l);
    }


    public Object getSelectedItem() {
         Object retval= null;
         Iterator i;
         RadioActionElement action;
         String current= getRadioValue();
         for(i= _radioActionList.iterator(); (i.hasNext() && retval==null);){
             action= (RadioActionElement)i.next();
             if (current.equals(action.getRadioActionString()) ) {
                retval= action;
             }
         }
         if (retval==null) {
             Assert.stop(
                  "The current value is not one of the list of values: " +
                  toString());
         }
         return retval;
    }

    public void setSelectedItem(Object anItem) {
         RadioActionElement action= (RadioActionElement)anItem;
         setRadioValue(action.getRadioActionString());
    }

    /**
     * Get the number of RadioActionElements
     * @return int  how many elements
     * @see RadioActionElement
     */
    public int getSize() {
        return _radioActionList.size();
    }

    public Object getElementAt(int i) {
        return _radioActionList.get(i);
    }

    public String toString() {
        StringBuffer sb= new StringBuffer(100);
        for(RadioActionElement action: _radioActionList) {
            sb.append('"');
            sb.append(action.getRadioActionString());
            sb.append('"');
            sb.append(", ");
        }
        sb.append("Current Value: ");
        sb.append('"');
        sb.append(getRadioValue());
        sb.append('"');

        sb.append( "\ncommand= ");
        sb.append(getActionCommand());
        sb.append( "\nname= ");
        sb.append(getName());
        return sb.toString();
    }



    @Override
    public boolean equals(Object other) {
        boolean retval= false;
        if (other==this) {
            retval= true;
        }
        else if (other!=null && other instanceof RadioAction) {
            RadioAction ia= (RadioAction)other;
            if (ComparisonUtil.equals(_radioActionList, ia._radioActionList) &&
                ComparisonUtil.equals(getRadioValue(), ia.getRadioValue()) ) {
                retval= true;
            }
        }
        return retval;
    }


//========================================================================
//------------------------ Private / Protected Methods -------------------
//========================================================================

    /**
     * Add a RadioActionElement to the the RadioAction. <em>Do not call this 
     * command directly</em>.  Creating a RadioActionElement will call this
     * method.
     * @param a the RadioActionElement element to add
     * @see RadioActionElement
     */
    protected void addRadioElement(RadioActionElement a) {
          _radioActionList.add(a);
    }

    protected static List newList() { return new Vector(3,2); }

    protected void changeRadio(ActionEvent e, String radioActionString) {
       super.setRadioValue(radioActionString);
       savePreference();
       if (_listDataList != null) {
          ListDataEvent ev= new ListDataEvent(this,
                                              ListDataEvent.CONTENTS_CHANGED, -1, -1);
          //for(Iterator iter= _listDataList.iterator(); (iter.hasNext());) {
           List<ListDataListener> newlist;
           synchronized (this) {
               newlist = new ArrayList<ListDataListener>(_listDataList);
           }
           for(ListDataListener listener: newlist) {
               listener.contentsChanged(ev);
           }
          AbstractButton b= (e == null) ? null : (AbstractButton)e.getSource();
          actionPerformed(e);
          radioChange( new RadioEvent(this, b) );
       }
    }


    private void savePreference() {
        if (_saveStateAsPreference) {
            AppProperties.setPreference(
                           getActionCommand()+ "."+ActionConst.RADIO_VALUE,
                           getRadioValue() );
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
