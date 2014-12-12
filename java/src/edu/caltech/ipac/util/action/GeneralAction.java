package edu.caltech.ipac.util.action;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.OSInfo;
import edu.caltech.ipac.util.SingletonUndoManager;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.PropDbFieldDefSource;
import edu.caltech.ipac.util.html.HtmlDocumentEntry;

import javax.swing.*;
import javax.swing.undo.UndoableEdit;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A class to further extend the capabilities of the Action interface.
 * This class adds more properties to Action and will attempt to initialize
 * this properties from a property database.  It also adds get and set methods
 * for these properties. <p>
 * 
 * Properties are parsed from the property database when the ActionCommand
 * property is set.  It might be set in the construtor or it could be set
 * later when <code>setActionCommand</code> is call.  Subclasses of 
 * GeneralAction should not assume the the properties have been parse
 * until the ActionCommand is set. The <code>newCommandNameSet()</code>
 * method is called when the action command is set and the properties are
 * parsed.
 * 
 * So far the following properites are supported:
 * <ul>
 * <li>ActionConst.MNEMONIC
 * <li>Action.SHORT_DESCRIPTION
 * <li>Action.NAME
 * <li>ActionConst.ACCELERATOR
 * <li>ActionConst.RADIO_VALUE
 * <li>ActionConst.ITEMS
 * <li>ActionConst.SELECTED
 * <li>ActionConst.ERROR_DESCRIPTION
 * </ul>
 * In a property file they should be defined as:
 * <ul>
 * <li> <em>ActionCommand</em>.Mnemonic=<em>char</em>
 * <li> <em>ActionCommand</em>.ShortDescription=<em>string</em>
 * <li> <em>ActionCommand</em>.Name=<em>string</em>
 * <li> <em>ActionCommand</em>.Accelerator=<em>acceleratorDesc</em>
 * <li> <em>ActionCommand</em>.RadioValue=<em>selectedCommandString</em>
 * <li> <em>ActionCommand</em>.Items=<em>optionList</em>
 * <li> <em>ActionCommand</em>.Selected=<em>boolean</em>
 * <li> <em>ActionCommand</em>.ErrorDescription=<em>string</em>
 * </ul>
 * 
 * @see ActionConst
 * @see javax.swing.AbstractAction
 * @see javax.swing.Action
 * 
 * @author Trey Roby
 * @version $Id: GeneralAction.java,v 1.11 2010/09/28 17:58:39 roby Exp $
 *
 */
public abstract class GeneralAction extends AbstractAction {
    private final static ClassProperties _prop =
                                  new ClassProperties(GeneralAction.class);
    private final static String NOT_FOUND = "&lt;Not Found&gt;";
    private static int _extraKey= Event.CTRL_MASK;

    private Properties _alternatePdb= null;
    private ClassProperties _classProps= null;
    private boolean    _propertiesAdded= false;
    private List<String> _requestedLocations= null;
    private FieldDefSource _fds= null;


    private static final boolean DELAY_LOAD=  _prop.getSelected("DelayLoad");

  //private static final int SHORTCUT_KEY=
  //                  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

//========================================================================
//---------------- static initializer --------------------
//========================================================================
   static {
      if (OSInfo.isPlatform(OSInfo.MAC)) {
          _extraKey= Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
      }
      else {
          _extraKey= Event.CTRL_MASK;
      }

   }

//========================================================================
//---------------- Constructors & misc public methods --------------------
//========================================================================



    /**
     * Create a Action with no name.  When you use this constructor the class
     * will not look up properites in the property database.  To make the
     * property lookup occure you must then use the setActionCommand method.
     */
    public GeneralAction()  {
        this((String)null);
    }

    /**
     * Create a Action with a command string.
     * @param command the command to reference this action by
     */
    public GeneralAction(String command)  {
        this(command,null);
    }

    public GeneralAction(String command, Properties alternatePdb)  {
        super();
        _alternatePdb= alternatePdb;
        if (command!=null) setActionCommand(command);
    }

    public GeneralAction(FieldDefSource fds)  {
        super();
        _fds= fds;
        _alternatePdb= (fds instanceof PropDbFieldDefSource) ? ((PropDbFieldDefSource)fds).getPDB()  : null;
        if (fds.getName()!=null) setActionCommand(fds.getName());
    }




    public GeneralAction(ClassProperties classProps)  {
        super();
        _classProps= classProps;
        if (classProps!=null) {
            _alternatePdb= _classProps.getAlternatePdb();
            setActionCommand(_classProps.getBaseNoLoad());
        }
    }


    /**
     * When a subclass does an undoable action it can create an
     * UndoableEdit object and add it to the undo manager with this
     * convenience method.
     * @param undo the object that will handle undoing the action
     */
    public void postUndo(UndoableEdit undo) {
        SingletonUndoManager.getInstance().addEdit(undo);
    }

    public void actionPerformed(ActionEvent ev) {
        // change support comes from super class
        if (changeSupport!=null) {
            changeSupport.firePropertyChange ( "actionPerformed", null, ev);
        }
        else {
            System.out.println("GeneralAction: changeSupport is null");
        }
    }

    public ClassProperties getBuilderClassProperties() { return _classProps; }


    public void addRequestedLocation(String requestedLocation) {
        if (_requestedLocations==null) {
            _requestedLocations= new ArrayList<String>(1);
        }
        _requestedLocations.add(requestedLocation);
    }
    
    public List<String> getRequestedLocations() {
        return _requestedLocations;
    }


//========================================================================
//------------------------ CommandName Property --------------------------
//========================================================================

   /**
    * Set the name of the command.  When a new command name is set the
    * properties for the command are loaded from the property database.
    * @param command the name of the new command
    */
   public final void setActionCommand(String command) {
      if (command != null) {
         String oldCommand= (String)super.getValue(ActionConst.ACTION_COMMAND);
         if (oldCommand == null || !oldCommand.equals(command)) {
             _propertiesAdded= false;
             putValue(ActionConst.ACTION_COMMAND,command);
             if (!DELAY_LOAD) addProperties();
         }
      }
      newCommandNameSet();
   }

   /**
    * Get the Action command.
    * @return String the Action command string
    */
   public final String getActionCommand() {
       return (String)super.getValue(ActionConst.ACTION_COMMAND);
   } 

   public final void setAlternatePdb(Properties pdb) {
       _alternatePdb= pdb; 
   }

    public final Properties getAlternatePdb() {
        return _alternatePdb;
    }
//========================================================================
//------------------------ Mnemonic, ShortDesc Property ------------------
//========================================================================

    /**
     * Set the mnemonic associated with this action.
     * @param keycodeString the mnemonic character
     */
    public final void setMnemonic(String keycodeString) {
        addProperties();
        KeyStroke ks= KeyStroke.getKeyStroke(keycodeString);
        if (ks!=null) setMnemonic(ks.getKeyCode());
    }
   /**
    * Set the mnemonic associated with this action.
    * @param keycode the mnemonic character
    */
   public final void setMnemonic(int keycode) {
      addProperties();
      putValue(ActionConst.MNEMONIC, new Integer(keycode) );
   }

   public final Integer getMnemonic() {
       addProperties();
       return (Integer)super.getValue(ActionConst.MNEMONIC);
   }

   /**
    * Set the short description associated with this action.  The short 
    * description is most often used as the tool tip.
    * @param value the short description string
    */
   public final void setShortDesc(String value) {
      addProperties();
      if (value != null) putValue(Action.SHORT_DESCRIPTION, value);
   } 

   /**
    * Get the short description associated with this action.  The short 
    * description is most often used as the tool tip.
    * @return String the short description string
    */
   public final String getShortDesc() {
       addProperties();
       return (String)super.getValue(Action.SHORT_DESCRIPTION);
   }

    /**
     * Set a addition to the tool tip that disscribes why a actions is
     * disabled.  Should only be visible when the action is disabled.
     * @param value the string discribing why an action is disabled
     */
    public final void setHowToEnableTip(String value) {
        addProperties();
        if (value != null) putValue(ActionConst.HOW_TO_ENABLE_TIP,  value);
    }

    /**
     * Get a addition to the tool tip that disscribes why a actions is
     * disabled.  Should only be visible when the action is disabled.
     * @return the string discribing why an action is disabled
     */
    public final String getHowToEnableTip() {
        addProperties();
        return (String)super.getValue(ActionConst.HOW_TO_ENABLE_TIP);
    }


//========================================================================
//------------------------ Name Property ---------------------------------
//========================================================================

   /**
    * Set the name associated with this action.  The name 
    * is most often used as the text of the button.
    * @param value the name
    */
   public final void setName(String value) {
      addProperties();
      if (value != null) putValue(Action.NAME, value);
   } 

   /**
    * Get the name associated with this action.  The name 
    * is most often used as the text of the button.
    * @return String the name
    * @see RadioAction
    */
   public final String getName() {
       addProperties();
       return (String)super.getValue(NAME);
   } 

//========================================================================
//------------------------ RadioValue Property --------------------------
//========================================================================

   /**
    * Get the a current radio button that is set.  This
    * method used with a RadioAction to get the current radio button.
    * @return String the current radio button
    * @see RadioAction
    */
   public String getRadioValue() {
       addProperties();
       return (String)super.getValue(ActionConst.RADIO_VALUE);
   } 

   /**
    * Set the a radio button assoicated with the command to true.  This
    * method used with a RadioAction to change the current radio button.
    * @param command the current radio button
    * @see RadioAction
    */
   public void setRadioValue(String command) {
       addProperties();
       if (command != null) putValue(ActionConst.RADIO_VALUE, command);
   } 

//========================================================================
//------------------------ Items Property ------------------------------
//========================================================================

   /**
    * Set a list of options that could be used for creating a set of 
    * radio buttons.
    * The RadioAction will create a set of RadioActionElements with this list.
    * @param options an array of the current values of the radio button
    * @see RadioAction
    * @see RadioActionElement
    */
   public final void setItems(String options[]) {
      addProperties();
      if (options != null) putValue(ActionConst.ITEMS, options);
   } 

   /**
    * Get a list of options that could be used for creating a set of 
    * radio buttons.
    * The RadioAction will create a set of RadioActionElements with this list.
    * @return String the current radio button values
    * @see RadioAction
    * @see RadioActionElement
    */
   public final String [] getItems() {
      addProperties();
      return (String [])super.getValue(ActionConst.ITEMS);
   } 

//========================================================================
//------------------------ Selected Property ------------------------------
//========================================================================

   /**
    * Set a toggle button or check box button to true.
    * @param set value for the toggle button or check box
    * @see ToggleAction
    */
   public void setSelected(boolean set) {
      addProperties();
      setSelected(Boolean.valueOf(set));
   } 

   /**
    * Set a toggle button or check box button to true.
    * @param set value for the toggle button or check box
    * @see ToggleAction
    */
   public void setSelected(Boolean set) {
      addProperties();
      putValue(ActionConst.SELECTED, set );
   } 

   /**
    * Get the value of a toggle button or check box button.
    * @return boolean value of the toggle button or check box
    * @see ToggleAction
    */
   public boolean getSelected() {
      addProperties();
      Boolean retval= (Boolean)super.getValue(ActionConst.SELECTED);
      return (retval == null) ? false : retval;
   } 



//========================================================================
//------------------------ IntValue Property ------------------------------
//========================================================================
   /**
    * Set the integer values associated with the action
    * @param v  the value
    * @see RadioActionElement
    */
   public final void setIntValue(int v) {
      addProperties();
      setIntValue(new Integer(v));
   } 

   /**
    * Set the integer values associated with the action
    * @param v the value
    * @see RadioActionElement
    */
   public final void setIntValue(Integer v) {
      addProperties();
      putValue(ActionConst.INT_VALUE, v);
   }  

   /**
    * Get the integer value associated with the action
    * @return the value
    * @see RadioActionElement
    */
   public final int getIntValue() {
      addProperties();
      Integer v= (Integer)super.getValue(ActionConst.INT_VALUE);
      return (v == null) ? ActionConst.INT_NULL : v;
   } 

//========================================================================
//------------------------ Accelerator Property ------------------------------
//========================================================================

   /**
    * Set the accelator that can be used for a button in a pulldown menu.
    * @param value a accelerator description. If is specified as
    * cntl-<em>char</em> or alt-<em>char</em>. e.g. "cntl-x", "alt-y", etc.
    * It will be converted into a javax.swing.KeyStroke
    * @see javax.swing.KeyStroke
    */
   public final void setAccelerator(String value) {
       addProperties();
       StringTokenizer st = new StringTokenizer(value, "-");
       if (st.countTokens() == 2) { // this is the old way
             String mod= st.nextToken();
             String key= st.nextToken();
             if (  key.length() == 1 ) {
                    key= key.toUpperCase();
                    int mod_key= -1;
                    if      (mod.equalsIgnoreCase("ctrl")) {
                           mod_key= _extraKey;
                           //mod_key= Event.CTRL_MASK;
                           
                    }
                    else if (mod.equalsIgnoreCase("meta")) {
                           mod_key= Event.META_MASK;
                    }
                    else if (mod.equalsIgnoreCase("alt")) {
                           mod_key= Event.ALT_MASK;
                    }
                    setAccelerator( KeyStroke.getKeyStroke(
                                       (int)key.charAt(0), mod_key ) ); 
             } // ned if
       } // end if
       else { // this way parses using the swing routines - a better way
            setAccelerator( KeyStroke.getKeyStroke(value) );
       }
   }

   /**
    * Set the accelator that can be used for a button in a pulldown menu.
    * @param keystroke the accelerator keystroke
    * @see javax.swing.KeyStroke
    */
   public final void setAccelerator(KeyStroke keystroke) {
       addProperties();
       if (keystroke != null) putValue(ActionConst.ACCELERATOR, keystroke);
   }


    public final KeyStroke getAccelerator() {
        return (KeyStroke)super.getValue(ActionConst.ACCELERATOR);
    }

//========================================================================
//------------------------ Icon Property ---------------------------------
//========================================================================

    public final Object getValue(String key) {
        addProperties();
        return super.getValue(key);
    }
   /**
    * Set the icon that can be used for a button.  This method uses a resource
    * to find and create the icon. A resource in this case would
    * be a relative or absolute url string.  If it is relative
    * the package name is prepended as a directory heirarchy on the resource.
    * (this is done my Class.getResource). <p>
    * Therefore if I have a package "mypackage.stuff" and I passed a resource
    * "gifs/a.gif" then the method would look for a gif file in
    * <pre>
    *         mypackage/stuff/gifs/a.gif
    * </pre>
    * @param resource the resource of this icon.
    * @return true, if the resoruce was foudn and the set was successful
    */
   public final boolean setSmallIcon(String resource) {
       addProperties();
       boolean retval= false;
       URL icon_url;
       try {
           icon_url= getClass().getResource(resource);
       } catch (NullPointerException e) {
           icon_url= null;
       }
       if (icon_url != null) {
            Icon icon= new ImageIcon(icon_url, resource);
            setSmallIcon(icon);
            retval= true;
       }
       else {
            icon_url= ClassLoader.getSystemResource(resource);
            if (icon_url != null) {
               Icon icon= new ImageIcon(icon_url, resource);
               setSmallIcon(icon);
               retval= true;
            }
       }
       return retval;
   }

   /**
    * Set the icon that can be used for a button.
    * @param icon the icon to set
    */
   public final void setSmallIcon(Icon icon) {
       addProperties();
       putValue( Action.SMALL_ICON, icon);
   }

   public final Icon getSmallIcon() {
      return (Icon)super.getValue(Action.SMALL_ICON);
   }

//========================================================================
//------------------------ ErrorDescription Property ---------------------
//========================================================================

   /**
    * Set the error description
    * @param value the description
    */
   public final void setErrorDescription(String value) {
       addProperties();
       putValue(ActionConst.ERROR_DESCRIPTION, value);
   }

   /**
    * Return the error description
    * @return the description
    */
   public final String getErrorDescription() {
       addProperties();
       return (String)super.getValue(ActionConst.ERROR_DESCRIPTION);
   }

//========================================================================
//------------------------ Other Methods ---------------------------------
//========================================================================

   /**
    * Collect info for HTML document
    * @param entry the entry
    */
   public void document(HtmlDocumentEntry entry) {
       addProperties();
       //List items = makeList(2);
       List<String> items= new ArrayList<String>(2);

       String description = ((getErrorDescription() != null) ? 
                                        getErrorDescription() : NOT_FOUND);
       if (description.equals(NOT_FOUND))
          description = ((getName() != null) ? getName() : 
                                  NOT_FOUND);

       if (getItems() != null) {
          String[] itemsList = getItems();
          String item = "";
          for(String itemElement : itemsList) {
            item += itemElement + " ";
          }

          items.add(_prop.getName("validValues") + " " + item); 
          items.add(_prop.getName("defaultValue") + " " + 
                          getRadioValue(itemsList, getRadioValue())); 
          entry.insertEntry(description, items);
       }
       else if (getRadioValue() != null) {
          items.add(_prop.getName("validValues") + " " + NOT_FOUND); 
          items.add(_prop.getName("defaultValue") + " " + getRadioValue()); 
          entry.insertEntry(description, items);
       }
       else if (!description.equals(NOT_FOUND)) {
          items.add(NOT_FOUND); 
          entry.insertEntry(description, items);
       }
   }

//========================================================================
//------------------------ Protected Methods -----------------------------
//========================================================================
    protected void addProperties() {

        if(!_propertiesAdded) {
            _propertiesAdded= true;
            if (_fds==null) {
                addPropertiesFromPDB();
            }
            else {
                addFieldDefSource();

            }

        }
    }


    protected void addFieldDefSource() {

        setName(_fds.getTitle());


        if (_fds.getMnemonic()!=null) setMnemonic(_fds.getMnemonic());
        if (_fds.getShortDesc()!=null)  setShortDesc(_fds.getShortDesc());
        if (_fds.getHowToEnableTip()!=null) setHowToEnableTip(_fds.getHowToEnableTip());

        if (_fds.getAccelerator()!=null) setAccelerator(_fds.getAccelerator());

        if (_fds.getDefaultValue()!=null) putValue(ActionConst.RADIO_VALUE, _fds.getDefaultValue());

        if(_fds.getItems()!=null) setItems(_fds.getItems());

        String selected=getPreference(_fds.getName()+"."+ActionConst.SELECTED);
        if(selected==null) selected= _fds.isSelected();
        if(selected!=null)  putValue(ActionConst.SELECTED, Boolean.valueOf(selected));

        if(_fds.getItemIntValue(null)!=null) setIntValue(new Integer(_fds.getItemIntValue(null)));

        if (_fds.getIcon()!=null) setSmallIcon(_fds.getIcon());
        if (_fds.getErrMsg()!=null) setErrorDescription(_fds.getErrMsg());

    }

   protected void addPropertiesFromPDB() {
       String value;

       String command= (String)super.getValue(ActionConst.ACTION_COMMAND);

       value=getProp(command, Action.NAME);

       // the following is to optimize loading properties.
       // if we were created with a ClassProperties object then try
       // try to get ActionName with out the properties files beging loaded
       // if the is not name then load the property file.
       // The properties that this method is looking for is sometimes loaded
       // places other than then action property file.  If an action is
       // using a ClassProperties object is is often to provide additional
       // propertie beyond the basic ones.
       if(_classProps!=null && value==null) {
           _classProps.load();
           value=getProp(command, Action.NAME);
       }
       setName(value);


       value=getProp(command, ActionConst.MNEMONIC);
       if(value!=null) setMnemonic(value);

       value=getProp(command, Action.SHORT_DESCRIPTION);
       setShortDesc(value);

       value=getProp(command, ActionConst.HOW_TO_ENABLE_TIP);
       setHowToEnableTip(value);

       value=getProp(command, ActionConst.ACCELERATOR);
       if(value!=null) setAccelerator(value);

       // use the put value because the method may be overriddeen
       value=getPreference(command+"."+ActionConst.RADIO_VALUE);
       if(value!=null)  putValue(ActionConst.RADIO_VALUE, value);

       value=getProp(command, ActionConst.ITEMS);
       if(value!=null) setItems(StringUtil.strToStrings(value));

       // use the put value because the method may be overriddeen
       value=getPreference(command+"."+ActionConst.SELECTED);
       if(value!=null)  putValue(ActionConst.SELECTED, Boolean.valueOf(value));

       value=getProp(command+"."+ActionConst.INT_VALUE);
       if(value!=null) setIntValue(new Integer(value));

       value=getProp(command, Action.SMALL_ICON);
       if(value!=null) setSmallIcon(value);

       value=getProp(command, ActionConst.ERROR_DESCRIPTION);
       if(value!=null) setErrorDescription(value);
   }

   /**
    * This method is call when the action command name is set.  The action does
    * not parse all the properties from the property database until the
    * command property is set.  If the command property is ever changed
    * the properties will be reparseed and this method will be call.
    * If the GeneralAction constructor is used that takes the command
    * as a parameter then this method is called from the constructor.
    */
   protected void newCommandNameSet() {}

//========================================================================
//------------------------ Private Methods -------------------------------
//========================================================================

   private String getProp( String key) {
      return AppProperties.getProperty(key, null, _alternatePdb);
   }

   private String getProp( String base, String prop) {
       return Prop.getPlatformProp(base,prop,null,_alternatePdb);
//      String value= null;
//
//      if (OSInfo.isPlatform(OSInfo.MAC)) {
//           value= getProp( base + ActionConst.MAC_PROP + prop);
//      }
//      else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
//           value= getProp( base + ActionConst.WINDOWS_PROP + prop);
//      }
//      else if (OSInfo.isPlatform(OSInfo.ANY_UNIX)) {
//           value= getProp( base + ActionConst.UNIX_PROP + prop);
//      }
//
//      if (value==null) {
//          value= getProp(base +"."+ prop);
//      }
//
//      return value;
   }

   private String getPreference( String key) {
      return AppProperties.getPreference(key, null, _alternatePdb);
   }

   private String getRadioValue(String[] items, String def) {
      for (String item : items) {
         if (item.equals(def)) return getRadioValue();
      }
      return NOT_FOUND;
   }

    public boolean isUsingFieldDefSource() { return _fds!=null; }
    public FieldDefSource getFieldDefSource() { return _fds; }
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
