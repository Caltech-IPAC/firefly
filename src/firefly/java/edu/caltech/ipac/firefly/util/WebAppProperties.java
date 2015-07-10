/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util;


import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.MapPropertyLoader;

import java.util.HashMap;
import java.util.Map;


/**
 * A utility class for working with properties.  Reads in property files,
 * and manages four level of properties.
 * <ul>
 * <li>Application Properties- These are the properties that application
 *                             uses but the user cannot set.  Most of
 *                             the properties are application properties.
 * <li>Preference Properties- These are properties that are set for the
 *                             user from the application and saved to a
 *                             preference property file when they change.
 *                             (backing store concept).
 *
 * </ul>
 * When retrieving a property there is only two levels:
 * properties & preferences.  There are a set of getXXXProperty routines and
 * and set of getXXXPreference routines.  The getXXXProperty routines search
 * only System (jre defined), then Application then Class then properties
 * in that order.  The getXXXPreference routines search Preference Properties
 * and User properties and then if not result if found they continue the search
 * the same as the getXXXProperty routines.
 * The class also a many get methods for various types of properties.
 *
 * @author Trey Roby
 *
 */
public class WebAppProperties {


//=========================================================================
//-------------- private static Property database variables ---------------
//=========================================================================

    /**
     * all the application level set properties are kept here.
     */
    private Map<String,String> _mainProperties=new HashMap<String,String>(103);
    private Map<String,String> _preferencesProperties=new HashMap<String,String>(31);


//=========================================================================
//------------------------- Float properties ------------------------------
//=========================================================================


    public WebAppProperties(String allPropertiesString) { load(allPropertiesString); }

    public void load(String allPropertiesString) { load(null,allPropertiesString); }
    public void load(Map<String,String> db, PropFile textRes) { load(db,textRes.get().getText()); }
    public void load(PropFile textRes) { load(null,textRes); }

    public void load(Map<String,String> db, String allPropertiesString) {
        MapPropertyLoader.load(db==null? _mainProperties : db ,allPropertiesString);
    }

    public float getFloatProperty(String key, float  def) {
        return getFloatProp(key,def,false);
    }

  /**
   * return as a float the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a float
   * @param isPref this is a preference
   *
   * @return float the value of the requested property or the default value
   */
  private float getFloatProp(String     key,
                             float      def,
                             boolean    isPref) {
      String val=    getProp(key,null,isPref);
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

  public double getDoubleProperty(String key, double def) {
      return getDoubleProp(key,def,false);
  }


  /**
   * return as a double the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a double
   * @param isPref this is a preference
   *
   * @return double the value of the requested property or the default value
   */
  private double getDoubleProp(String key, double def, boolean isPref) {
      String val= getProp(key,null,isPref);
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

  public long getLongProperty(String key, long def) {
      return getLongProp(key,def,false);
  }

  /**
   * return as a long the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed long a long
   * @param isPref this is a preference
   *
   * @return long the value of the requested property or the default value
   */
  private long getLongProp(String     key,
                           long       def,
                           boolean    isPref) {
      String val=    getProp(key,null,isPref);
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

  public int getIntProperty(String key, int def) { return getIntProp(key,def,false); }

  /**
   * return as a int the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a int
   * @param isPref this is a preference
   *
   * @return int the value of the requested property or the default value
   */
  private int getIntProp(String key, int def, boolean isPref) {
      String val=    getProp(key,null,isPref);
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
  public boolean getBooleanProperty(String key) {
      return getBooleanProp(key,false,false);
  }

  public boolean getBooleanProperty(String key, boolean def) {
      return getBooleanProp(key,def,false);
  }

  public boolean getBooleanProperty(String key, boolean def, boolean isPref) {
        return getBooleanProp(key,def,isPref);
    }

    /**
   * return as a boolean the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a boolean
   * @param isPref this is a preference
   *
   * @return int the value of the requested property or the default value
   */
  private boolean getBooleanProp(String     key,
                                 boolean    def,
                                 boolean    isPref) {
      String  val=    getProp(key,null,isPref);
      boolean retval;
      if (val != null) retval= Boolean.valueOf(val);
      else             retval= def;
      return retval;
  }


//=========================================================================
//------------------------- String properties -----------------------------
//=========================================================================

    public String getPreference(String key, String def) { return getProp(key,def,true); }

    public String getProperty(String key) { return getProp(key,null,false); }

    public String getProperty(String key, String def) { return getProp(key,def,false); }


    String getProp(String key, String def, boolean isPref) {

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
            retval= _preferencesProperties.get(key);
        }
        else {
            retval= get(key, def,_mainProperties);
        }

        return retval;
    }

    private String get(String key, String def, Map pdb) {
        String retval= def;
        if (pdb.containsKey(key)) {
            retval= (String)pdb.get(key);
        }
        return retval;
    }

    public void setProperty(String key, String value) {
        String oldValue= getProperty(key, null);
        if (oldValue==null || !ComparisonUtil.equals(oldValue,value) ) {
            _mainProperties.put(key, value);
        }
    }


//=========================================================================
//--------------------- Preference Methods --------------------------------
//=========================================================================

    public void setPreference(String key, String value) { setPref(key,value); }

    private void setPref(String key, String value) {
        String oldValue= getPreference(key, null);
        if (oldValue==null || !ComparisonUtil.equals(oldValue,value) ) {
            _preferencesProperties.put(key, value);
        }
    }

}
