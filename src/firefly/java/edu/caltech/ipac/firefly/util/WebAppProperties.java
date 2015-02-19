/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.MapPropertyLoader;

import java.util.HashMap;
import java.util.Map;


/**
 * A utility class for working with properties.  Reads in properity files,
 * and manages four level of perperties.
 * <ul>
 * <li>Application Properties- These are the properties that application
 *                             uses but the user cannot set.  Most of
 *                             the properties are application properties.
 * <li>Class Properties- These are also application properties and work just
 *                       like them but when are overridden by any equivilent
 *                       application properties.
 * <li>User Properties- These are properties that the user is allows to set
 *                      in a property file.
 * <li>Preference Properties- These are properties that are set for the
 *                             user from the application and saved to a
 *                             perference property file when they change.
 *                             (backing store concept).
 *
 * </ul>
 * When retrieving a property there is only two levels:
 * properties & prerferences.  There are a set of getXXXProperty routines and
 * and set of getXXXPreferece routines.  The getXXXProperty routines search
 * only System (jre defined), then Application then Class then properties
 * in that order.  The getXXXPreferece routines search Preference Properties
 * and User properties and then if not result if found they continue the search
 * the same as the getXXXProperty routines.
 * The class also a many get methods for various types of properties.
 *
 * @author Trey Roby
 * @version $Id: WebAppProperties.java,v 1.7 2012/05/16 01:39:04 loi Exp $
 *
 */
public class WebAppProperties {


//=========================================================================
//-------------- private static Property database varibles ----------------
//=========================================================================

    /**
     * all the application level set properties are kept here.
     */
    private Map<String,String> _mainProperties=new HashMap<String,String>(103);
    private Map<String,String> _preferencesProperties=new HashMap<String,String>(31);


//=========================================================================
//------------------------ other static  varibles -------------------------
//=========================================================================

    private final PropertyChangeSupport _propChange=new
                                   PropertyChangeSupport(WebAppProperties.class);


//=========================================================================
//------------------------- Float properties ------------------------------
//=========================================================================


    public WebAppProperties(String allPropertiesString) {
        load(allPropertiesString);
    }

    public void load(String allPropertiesString) { load(null,allPropertiesString); }
    public void load(Map<String,String> db, PropFile textRes) { load(db,textRes.get().getText()); }
    public void load(PropFile textRes) { load(null,textRes); }

    public void load(Map<String,String> db, String allPropertiesString) {
        MapPropertyLoader.load(db==null? _mainProperties : db ,allPropertiesString);
    }


  public float getFloatPreference(String key, float def) {
      return getFloatProp(key,def,true,null);
  }
  public float getFloatPreference(String     key,
                                  float      def,
                                  Map additionalPDB) {
      return getFloatProp(key,def,true,additionalPDB);
  }


  public float getFloatProperty(String key, float def) {
      return getFloatProp(key,def,false,null);
  }
  public float getFloatProperty(String key,
                                float  def,
                                Map additionalPDB) {
      return getFloatProp(key,def,false,additionalPDB);
  }

  /**
   * return as a float the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a float
   * @param isPref this is a preference
   * @param additionalPDB this property database is search instead
   *
   * @return float the value of the requested property or the default value
   */
  private float getFloatProp(String     key,
                             float      def,
                             boolean    isPref,
                             Map additionalPDB) {
      String val=    getProp(key,null,isPref,additionalPDB);
      float  retval;
      try {
          if (val != null) retval= Float.parseFloat(val);
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }
//=========================================================================
//------------------------- Double properties ------------------------------
//=========================================================================

  public double getDoublePreference(String key, double def) {
      return getDoubleProp(key,def,true,null);
  }
  public double getDoublePreference(String     key,
                                    double     def,
                                    Map additionalPDB) {
      return getDoubleProp(key,def,true,additionalPDB);
  }

  public double getDoubleProperty(String key, double def) {
      return getDoubleProp(key,def,false,null);
  }


  public double getDoubleProperty(String     key,
                                  double     def,
                                  Map additionalPDB) {
      return getDoubleProp(key,def,false,additionalPDB);
  }

  /**
   * return as a double the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a double
   * @param isPref this is a preference
   * @param additionalPDB this property database is search instead
   *
   * @return double the value of the requested property or the default value
   */
  private double getDoubleProp(String     key,
                               double     def,
                               boolean    isPref,
                               Map additionalPDB) {
      String val   =    getProp(key,null,isPref,additionalPDB);
      double retval;
      try {
          if (val != null) retval= Double.parseDouble(val);
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }

//=========================================================================
//------------------------- Long properties --------------------------------
//=========================================================================

  public long getLongPreference(String key, long def) {
      return getLongProp(key,def,true,null);
  }
  public long getLongPreference(String     key,
                                long       def,
                                Map additionalPDB) {
      return getLongProp(key,def,true,additionalPDB);
  }

  public long getLongProperty(String key, long def) {
      return getLongProp(key,def,false,null);
  }

  public long getLongProperty(String     key,
                              long       def,
                              Map additionalPDB) {
      return getLongProp(key,def,false,additionalPDB);
  }

  /**
   * return as a long the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed long a long
   * @param isPref this is a preference
   * @param additionalPDB  search the user property database also.  If the
   *        user property exist then it takes precedence.
   *
   * @return long the value of the requested property or the default value
   */
  private long getLongProp(String     key,
                           long       def,
                           boolean    isPref,
                           Map additionalPDB) {
      String val=    getProp(key,null,isPref,additionalPDB);
      long    retval;
      try {
          if (val != null) retval= Long.parseLong(val);
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }

//=========================================================================
//------------------------- Int properties --------------------------------
//=========================================================================

  public int getIntPreference(String key, int def) {
      return getIntProp(key,def,true,null);
  }

  public int getIntPreference(String key, int def, Map additionalPDB) {
      return getIntProp(key,def,true,additionalPDB);
  }

  public int getIntProperty(String key, int def) {
      return getIntProp(key,def,false,null);
  }

  public int getIntProperty(String key, int def, Map additionalPDB){
      return getIntProp(key,def,false,additionalPDB);
  }

  /**
   * return as a int the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a int
   * @param isPref this is a preference
   * @param additionalPDB this property database is search instead
   *
   * @return int the value of the requested property or the default value
   */
  private int getIntProp(String     key,
                         int        def,
                         boolean    isPref,
                         Map additionalPDB) {
      String val=    getProp(key,null,isPref,additionalPDB);
      int    retval;
      try {
          if (val != null) retval= Integer.parseInt(val);
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }

//=========================================================================
//------------------------- Boolean properties -------------------------------
//=========================================================================
  public boolean getBooleanPreference(String key, boolean def) {
      return getBooleanProp(key,def,true,null);
  }

  public boolean getBooleanPreference(String     key,
                                      boolean    def,
                                      Map additionalPDB) {
      return getBooleanProp(key,def,true,additionalPDB);
  }

  public boolean getBooleanProperty(String key) {
      return getBooleanProp(key,false,false,null);
  }

  public boolean getBooleanProperty(String key, boolean def) {
      return getBooleanProp(key,def,false,null);
  }

  public boolean getBooleanProperty(String     key,
                                    boolean    def,
                                    Map additionalPDB) {
      return getBooleanProp(key,def,false,additionalPDB);
  }

  /**
   * return as a boolean the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a boolean
   * @param isPref this is a preference
   * @param additionalPDB this property database is search instead
   *
   * @return int the value of the requested property or the default value
   */
  private boolean getBooleanProp(String     key,
                                 boolean    def,
                                 boolean    isPref,
                                 Map additionalPDB) {
      String  val=    getProp(key,null,isPref,additionalPDB);
      boolean retval;
      if (val != null) retval= Boolean.valueOf(val);
      else             retval= def;
      return retval;
  }


//=========================================================================
//------------------------- String properties -----------------------------
//=========================================================================
  public String getPreference(String key) {
      return getProp(key,null,true,null);
  }

  public String getPreference(String key, String def) {
      return getProp(key,def,true,null);
  }

  public String getPreference(String     key,
                              String     def,
                              Map additionalPDB) {
      return getProp(key,def,true,additionalPDB);
  }


  public String getProperty(String key) {
      return getProp(key,null,false,null);
  }

  public String getProperty(String key, String def) {
      return getProp(key,def,false,null);
  }

  public String getProperty(String     key,
                            String     def,
                            Map additionalPDB) {
      return getProp(key,def,false,additionalPDB);
  }


    String getProp(String     key,
                   String     def,
                   boolean    isPref,
                   Map additionalPDB) {

        String retval= def;
        // if isPref is set then:
        //    - search the additionalPDB database
        //    - search the preference database
        //    - the priority of which database for isPref is as
        //        follows from highest to lowest:
        //        1. perfs
        //        2. additionalPDB
        //        3. application (system, mainProperties, classProperties)
        if (isPref && _preferencesProperties.containsKey(key)) {
            retval= (String)_preferencesProperties.get(key);
        }
        else if (additionalPDB!=null && _preferencesProperties.containsKey(key)) {
            retval= (String)additionalPDB.get(key);
        }
        else {
            retval= get(key, def,_mainProperties);
        }

        return retval;
    }



  private String get(String     key,
                     String     def,
                     Map        pdb) {
      String retval= def;
      if (pdb.containsKey(key)) {
          retval= (String)pdb.get(key);
      }
      return retval;
  }

    public void setProperty(String key, String value) {
        String oldValue= getProperty(key, null);
        if (oldValue==null || !ComparisonUtil.equals(oldValue,value) ) {
            _mainProperties.put(key, value); //TODO - set on server
            _propChange.firePropertyChange(key,oldValue, value);
        }
    }


//=========================================================================
//--------------------- Preference Methods --------------------------------
//=========================================================================

  public void setPreference(String key, String value) {
      setPref(key,value);
  }

  private void setPref(String key, String value) {
      String oldValue= getPreference(key, null);
      if (oldValue==null || !ComparisonUtil.equals(oldValue,value) ) {
          _preferencesProperties.put(key, value); //TODO - set on server
          _propChange.firePropertyChange(key,oldValue, value);
      }
  }


//=====================================================================
//----------- add / remove property Change listener methods -----------
//=====================================================================

    /**
     * Add a property changed listener. Because this is a listener on a
     * static class it uses week references for is property change listener list.
     * You must not add a listener this is an annonymous inner class here.
     * The listener will be garbage collected immediatly.  You must add a listener
     * that is being pointed to by another object.
     * @param l listener
     */
    public void addPropertyChangeListener (PropertyChangeListener l) {
       _propChange.addPropertyChangeListener (l);
    }

    /**
     * Remove a property changed listener.
     * @param l  the listener
     */
    public void removePropertyChangeListener(PropertyChangeListener l){
       _propChange.removePropertyChangeListener (l);
    }

}

