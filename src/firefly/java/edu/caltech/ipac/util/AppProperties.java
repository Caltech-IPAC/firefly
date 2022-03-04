/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;


/**
 * A utility class for working with properties.  Reads in property files,
 * and manages four level of properties.
 * <ul>
 * <li>Application Properties- These are the properties that application
 *                             uses but the user cannot set.  Most of
 *                             the properties are application properties.
 * <li>Class Properties- These are also application properties and work just
 *                       like them but when are overridden by any equivalent
 *                       application properties.
 * <li>User Properties- These are properties that the user is allows to set
 *                      in a property file.
 *
 * </ul>
 * When retrieving a property there is only two levels:
 * properties & preferences.  There are a set of getXXXProperty routines.
 * The getXXXProperty routines search
 * only System (jre defined), then Application then Class then properties
 * in that order.
 * The class also a many get methods for various types of properties.
 *
 * @author Trey Roby
 * @version $Id: AppProperties.java,v 1.28 2012/12/14 21:04:02 roby Exp $
 *
 */
public class AppProperties {


//=========================================================================
//-------------- private static Property database variables ----------------
//=========================================================================

    /**
     * all the application level set properties are kept here.
     */
    private final static Properties _mainProperties=new Properties();





//=========================================================================
//------------------------- Float properties ------------------------------
//=========================================================================


  public static float getFloatProperty(String key, float def) { return getFloatProp(key,def,null); }

  /**
   * return as a float the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a float
   * @param overridePDB this property database is search instead
   *
   * @return float the value of the requested property or the default value
   */
  private static float getFloatProp(String key, float def, Properties overridePDB) {
      String val=    getProperty(key,null, overridePDB);
      float  retval;
      try {
          if (val != null) retval= Float.parseFloat(val.trim());
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }
//=========================================================================
//------------------------- Double properties ------------------------------
//=========================================================================


  /**
   * return as a double the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a double
   * @param overridePDB this property database is search instead
   *
   * @return double the value of the requested property or the default value
   */
  private static double getDoubleProp(String     key,
                                      double     def,
                                      Properties overridePDB) {
      String val   =    getProperty(key,null, overridePDB);
      double retval;
      try {
          if (val != null) retval= Double.parseDouble(val.trim());
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }

//=========================================================================
//------------------------- Long properties --------------------------------
//=========================================================================


  public static long getLongProperty(String key, long def) { return getLongProp(key,def,null); }

  /**
   * return as a long the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed long a long
   * @param overridePDB  search the user property database also.  If the
   *        user property exist then it takes precedence.
   *
   * @return long the value of the requested property or the default value
   */
  private static long getLongProp(String key, long def, Properties overridePDB) {
      String val=    getProperty(key,null, overridePDB);
      long    retval;
      try {
          if (val != null) retval= Long.parseLong(val.trim());
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
  }

//=========================================================================
//------------------------- Int properties --------------------------------
//=========================================================================


  public static int getIntProperty(String key, int def) { return getIntProperty(key,def,null); }


  /**
   * return as a int the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a int
   * @param overridePDB this property database is search instead
   *
   * @return int the value of the requested property or the default value
   */
  public static int getIntProperty(String key, int def, Properties overridePDB) {
      String val=    getProperty(key,null, overridePDB);
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

  public static boolean getBooleanProperty(String key) { return getBooleanProperty(key,false); }


  /**
   * return as a boolean the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a boolean
   *
   * @return int the value of the requested property or the default value
   */
  public static boolean getBooleanProperty(String key, boolean def) {
      String  val=    getProperty(key,null, null);
      boolean retval;
      if (val != null) retval= Boolean.parseBoolean(val);
      else             retval= def;
      return retval;
  }


//=========================================================================
//------------------------- String properties -----------------------------
//=========================================================================


  public static String getProperty(String key) {
      return getProperty(key,null, null);
  }

  public static String getProperty(String key, String def) {
      return getProperty(key,def, null);
  }

  public static String getProperty(String key, String def, Properties overridePDB) {
      // NOTE use of == on String instead of .equal is intentional
      //
      // Applications property search order
      //    - search the system database
      //    - search the mainProperties database
      //    - the priority for application properties
      //        follows from highest to lowest:
      //        1. system
      //        2. mainProperties

      String retval= def;
      if (overridePDB==null) {
          try {
               retval= System.getProperty(key, def);
          } catch (Exception e) { /*do nothing*/ }
          if (retval==def) retval= _mainProperties.getProperty(key, def);
      }
      else {
          retval= overridePDB.getProperty(key, def);
      }
      return retval;
  }




    public static void addApplicationProperties(File f, boolean ignoreFileNotFound)
                                                       throws IOException {
        addApplicationProperties(f,ignoreFileNotFound,null);
    }

    public static void addApplicationProperties(File       f,
                                                boolean    ignoreFileNotFound,
                                                Properties loadToPDB)
            throws IOException {
        if (_mainProperties==null) {
            return;  // if _mainProperties is null then most probably we are using a server (tomcat) that has
            // done some sort of static unload during shutdown, in any normal mode of
            // operation _mainProperties is never null
        }
        try {
            // need to add notLoaded support
            InputStream fs= new FileInputStream( f );
            if(loadToPDB==null) loadToPDB=_mainProperties;
            addPropertiesFromStream(fs, loadToPDB);
        } catch (FileNotFoundException e) {
            if (!ignoreFileNotFound) throw e;
        }
    }


    /**
     * Load a property database from a input stream with synchronization.
     * Close the stream when done.
     * @param propStream the input stream where to get the properties
     * @param pdb the property data base to load to
     * @throws IOException if it could not open the stream
     */
  public static synchronized void addPropertiesFromStream(
                                             InputStream propStream,
                                             Properties  pdb)
                                          throws IOException {
        Assert.argTst(pdb, "pdb must not be null");
        BufferedInputStream bis= new BufferedInputStream(propStream, 2048);
        try {
            AppProperties.loadSubstitutionValues(bis, pdb);
        } finally {
            FileUtil.silentClose(bis);
        }
  }

    /**
     * I don't understand what the purpose of thie method is.  It appears to load the properties and then copy
     * then all into the target pdb.  Not sure why this is necessary but I am leaving it for now.
     * I suspect the method use to do more in the spot days and it hss be cleaned up a couple of times.
     * @param bis
     * @param pdb
     * @throws IOException
     */
    private static void loadSubstitutionValues(BufferedInputStream bis, Properties pdb) throws IOException {
        Properties tempPDB = new Properties();
        tempPDB.load(bis);
        String key;
        String value;
        String newValue;

        for (Map.Entry<Object, Object> entry : tempPDB.entrySet()) {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
            newValue = value;
            pdb.setProperty(key, newValue);
        }
    }




//=====================================================================
//----------- Private / Protected Methods -----------------------------
//=====================================================================



    /**
     * Returns an array of property values from the Properties. If the property is empty an empty array is returned. If
     * the property is not defined return null.
     *
     * @param property Property to query
     * @param sep      separator between values
     * @return Property values.
     */
    public static String[] getArrayProperties(final String property, final String sep) {
        final String value = getProperty(property);
        String[] values = null;

        if (value != null) {
            if (value.length() == 0) {
                values = new String[0];
            } else {
                values = value.split(sep);
            }
        }

        // may be null
        return values;
    }

    /**
     * Returns an array of property values from the properties. If the property is empty an empty array is returned. If
     * the property is not defined return default.
     *
     * @param property Property to query
     * @param sep      separator between values
     * @param def      default list of properties
     * @return Property values.
     */
    public static String[] getArrayProperties(final String property, final String sep, final String def) {
        final String value = getProperty(property, def);
        String[] values = null;

        if (value != null) {
            if (value.length() == 0) {
                values = new String[0];
            } else {
                values = value.split(sep);
            }
        }

        // may be null
        return values;
    }

    /**
     * Returns an array of property values from the Properties, values list is defined by spaces.
     * <p>
     * Note that this method assumes that the property values are separated by one or more spaces. Multiple spaces in
     * between property values are equivalent to a single space. A property value corresponding to one or more spaces is
     * not possible.
     * </p>
     *
     * @param property Property to query
     * @return Property values.
     */
    public static String[] getArrayProperties(final String property) {
        return getArrayProperties(property, "\\s+");
    }

    public static void setProperty(String key, String value) {
           if (_mainProperties==null) {
               return;  // if _mainProperties is null then most probably we are using a server (tomcat) that has
               // done some sort of static unload during shutdown, in any normal mode of
               // operation _mainProperties is never null
           }
           _mainProperties.setProperty(key, value);
       }

    public static boolean loadClassPropertiesFromFileToPdb(File file, Properties loadToPDB) {
         boolean loaded= false;
         Assert.argTst(file, "resource must not be null");
         try {
             InputStream fs= new FileInputStream( file );
             addPropertiesFromStream(fs, loadToPDB);
             loaded= true;
         } catch (FileNotFoundException e) {
             System.out.println("AppProperties: Could not find file: " +
                                file.getPath());
         } catch (IOException e) {
             String errStr= "AppProperties: Class properties not found: ";
             System.out.println(errStr + file.getPath());
             System.out.println("AppProperties: exception: " + e);
         }
         return loaded;
     }

}
