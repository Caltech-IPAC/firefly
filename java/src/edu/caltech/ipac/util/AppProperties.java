package edu.caltech.ipac.util;


import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * A utility class for working with properties.  Reads in properity files,
 * and manages four level of properties.
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
 * @version $Id: AppProperties.java,v 1.28 2012/12/14 21:04:02 roby Exp $
 *
 */
public class AppProperties {

    public static final String DEVELOPER_MODE_PROP= "ipac.developer";
    public static final boolean DEVELOPER_MODE;

//=========================================================================
//-------------- private static Property database varibles ----------------
//=========================================================================

    /**
     * all the application level set properties are kept here.
     */
    private final static Properties _mainProperties=new Properties();
    /**
     * all the class level set properties are kept here.
     */
    private final static Properties _classProperties=new Properties();

    /**
     * user properties will hold only properties that the end user sets in
     * a user property file.  We do not write them back out as they change
     * as we do preferences.
     */
    private final static Properties _userProperties=new Properties();
    /**
     * preference properties will hold properties that the application
     * sets.  It may be initialized from a preference property file.
     * It is posible (though not recommended) that the user might
     * edit this preference property file.
     */
    private final static Properties _preferencesProperties=new Properties();

    private static Map<String,String> _substitutionValues= null;



    private static boolean serverMode= false;

    //--------------------- Note --------------------
    // the getXXXXProperty methods will query the _mainProperties,
    // _classProperties and System properties only
    // the getXXXXPreference methods will query the _userProperties,
    // _preferencesProperties  and if not found query _mainProperties,
    // _classProperties, and System properties



//=========================================================================
//------------------------ other static  variables -------------------------
//=========================================================================

    private static File                  _preferencesFile=null;
    private final static List<URL>      _loadedResources=new LinkedList<URL>();
    private final static WeakPropertyChangeSupport _propChange=new
                                   WeakPropertyChangeSupport();
    private static Properties  _record= null;

    static {
        boolean dMode= false;
        try {
            String v= System.getProperty(DEVELOPER_MODE_PROP);
            if (v!=null && v.equalsIgnoreCase("true")) {
                dMode= true;
                _record= new Properties();
            }
        } catch (AccessControlException ace) {
            dMode= false;
        } catch (Exception e) {
            dMode= false;
            e.printStackTrace();
        }
        DEVELOPER_MODE= dMode;
    }

    /**
     * when running in server mode certain client side features are disabled.
     * @param mode
     */
    public static void setServerMode(boolean mode) { serverMode= mode; }


//=========================================================================
//------------------------- Float properties ------------------------------
//=========================================================================
  public static float getFloatPreference(String key, float def) {
      return getFloatProp(key,def,true,null);
  }
  public static float getFloatPreference(String     key,
                                         float      def,
                                         Properties overridePDB) {
      return getFloatProp(key,def,true,overridePDB);
  }


  public static float getFloatProperty(String key, float def) {
      return getFloatProp(key,def,false,null);
  }
  public static float getFloatProperty(String     key,
                                       float      def,
                                       Properties overridePDB) {
      return getFloatProp(key,def,false,overridePDB);
  }

  /**
   * return as a float the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a float
   * @param isPref this is a preference
   * @param overridePDB this property database is search instead
   *
   * @return float the value of the requested property or the default value
   */
  private static float getFloatProp(String     key,
                                    float      def,
                                    boolean    isPref,
                                    Properties overridePDB) {
      String val=    getProp(key,null,isPref,overridePDB);
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

  public static double getDoublePreference(String key, double def) {
      return getDoubleProp(key,def,true,null);
  }
  public static double getDoublePreference(String     key,
                                           double     def,
                                           Properties overridePDB) {
      return getDoubleProp(key,def,true,overridePDB);
  }

  public static double getDoubleProperty(String key, double def) {
      return getDoubleProp(key,def,false,null);
  }


  public static double getDoubleProperty(String     key,
                                         double     def,
                                         Properties overridePDB) {
      return getDoubleProp(key,def,false,overridePDB);
  }

  /**
   * return as a double the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a double
   * @param isPref this is a preference
   * @param overridePDB this property database is search instead
   *
   * @return double the value of the requested property or the default value
   */
  private static double getDoubleProp(String     key,
                                      double     def,
                                      boolean    isPref,
                                      Properties overridePDB) {
      String val   =    getProp(key,null,isPref,overridePDB);
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

  public static long getLongPreference(String key, long def) {
      return getLongProp(key,def,true,null);
  }
  public static long getLongPreference(String     key,
                                       long       def,
                                       Properties overridePDB) {
      return getLongProp(key,def,true,overridePDB);
  }

  public static long getLongProperty(String key, long def) {
      return getLongProp(key,def,false,null);
  }

  public static long getLongProperty(String     key,
                                     long       def,
                                     Properties overridePDB) {
      return getLongProp(key,def,false,overridePDB);
  }

  /**
   * return as a long the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed long a long
   * @param isPref this is a preference
   * @param overridePDB  search the user property database also.  If the
   *        user property exist then it takes precedence.
   *
   * @return long the value of the requested property or the default value
   */
  private static long getLongProp(String     key,
                                  long       def,
                                  boolean    isPref,
                                  Properties overridePDB) {
      String val=    getProp(key,null,isPref,overridePDB);
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

  public static int getIntPreference(String key, int def) {
      return getIntProp(key,def,true,null);
  }

  public static int getIntPreference(String key, int def, Properties overPDB) {
      return getIntProp(key,def,true,overPDB);
  }

  public static int getIntProperty(String key, int def) {
      return getIntProp(key,def,false,null);
  }

  public static int getIntProperty(String key, int def, Properties overPDB){
      return getIntProp(key,def,false,overPDB);
  }

  /**
   * return as a int the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a int
   * @param isPref this is a preference
   * @param overridePDB this property database is search instead
   *
   * @return int the value of the requested property or the default value
   */
  private static int getIntProp(String     key,
                                int        def,
                                boolean    isPref,
                                Properties overridePDB) {
      String val=    getProp(key,null,isPref,overridePDB);
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
  public static boolean getBooleanPreference(String key, boolean def) {
      return getBooleanProp(key,def,true,null);
  }

  public static boolean getBooleanPreference(String     key,
                                             boolean    def,
                                             Properties overridePDB) {
      return getBooleanProp(key,def,true,overridePDB);
  }

  public static boolean getBooleanProperty(String key) {
      return getBooleanProp(key,false,false,null);
  }

  public static boolean getBooleanProperty(String key, boolean def) {
      return getBooleanProp(key,def,false,null);
  }

  public static boolean getBooleanProperty(String     key,
                                           boolean    def,
                                           Properties overridePDB) {
      return getBooleanProp(key,def,false,overridePDB);
  }

  /**
   * return as a boolean the requested property value.
   * @param key the request property
   * @param def the default value to return if the property does not
   *               exist or cannot be parsed into a boolean
   * @param isPref this is a preference
   * @param overridePDB this property database is search instead
   *
   * @return int the value of the requested property or the default value
   */
  private static boolean getBooleanProp(String     key,
                                        boolean    def,
                                        boolean    isPref,
                                        Properties overridePDB) {
      String  val=    getProp(key,null,isPref,overridePDB);
      boolean retval;
      if (val != null) retval= Boolean.parseBoolean(val);
      else             retval= def;
      return retval;
  }


//=========================================================================
//------------------------- String properties -----------------------------
//=========================================================================
  public static String getPreference(String key) {
      return getProp(key,null,true,null);
  }

  public static String getPreference(String key, String def) {
      return getProp(key,def,true,null);
  }

  public static String getPreference(String     key,
                                     String     def,
                                     Properties overridePDB) {
      return getProp(key,def,true,overridePDB);
  }


  public static String getProperty(String key) {
      return getProp(key,null,false,null);
  }

  public static String getProperty(String key, String def) {
      return getProp(key,def,false,null);
  }

  public static String getProperty(String     key,
                                   String     def,
                                   Properties overridePDB) {
      return getProp(key,def,false,overridePDB);
  }


    public static String getProp(String     key,
                                 String     def,
                                 boolean    isPref,
                                 Properties overridePDB) {

        String retval= def;
        // NOTE use of == on String instead of .equal is intentional
        //
        // if isPref is set then:
        //    - search the user database
        //    - search the preference database
        //    - the priority of which database for isPref is as
        //        follows from highest to lowest:
        //        1. perfs
        //        2. user
        //        3. application (system, mainProperties, classProperties)
        if (isPref) {
            retval= _preferencesProperties.getProperty(key, def);
            if (retval==def) retval= _userProperties.getProperty(key, def);
        }
        if (retval==def) retval= getAppProp(key,def,overridePDB);

        if (_record!=null && retval!=null) _record.put(key,retval);
        return retval;
    }



  private static String getAppProp(String     key,
                                   String     def,
                                   Properties overridePDB) {
      // NOTE use of == on String instead of .equal is intentional
      //
      // Applications property search order
      //    - search the system database
      //    - search the mainProperties database
      //    - search the classProperties database
      //    - the priority for application properties
      //        follows from highest to lowest:
      //        1. system
      //        2. mainProperties
      //        3. classProperties

      String retval= def;
      if (overridePDB==null) {
          try {
               retval= System.getProperty(key, def);
          } catch (AccessControlException ace) { /*do nothing*/ }
          if (retval==def) retval= _mainProperties.getProperty(key, def);
          if (retval==def) retval= _classProperties.getProperty(key, def);
      }
      else {
          retval= overridePDB.getProperty(key, def);
      }
      return retval;
  }


//=========================================================================
//--------------------- Preference Methods --------------------------------
//=========================================================================

  public static void setPreference(String key, String value) {
      setPref(key,value,true);
  }

  /**
   * Don't call this method unless you really know why you are doing it!
   * Set a perference but don't dump all the perference to a file as
   * we normally do. This method is designed to be called from one place-
   * that is when a preference is set in another process and we have been
   * informed.  In that case, the preference is already persisted.
   * @param key the name of the prerference
   * @param value the value of the prerference
   */
  public static void setPreferenceWithNoPersistence(String key, String value) {
      setPref(key,value,false);
  }

  private static void setPref(String key, String value, boolean dump) {
      String oldValue= getPreference(key, null);
      if (oldValue==null || !ComparisonUtil.equals(oldValue,value) ) {
          _preferencesProperties.setProperty(key, value);
          _userProperties.setProperty(key, value);
          if (dump) dumpPreferences();
          _propChange.firePropertyChange(AppProperties.class, key,oldValue, value);
      }
  }

    /**
     * rewrite the current perference file
     */
  public static void dumpPreferences() {
     try {
          if (_preferencesFile != null) {
             FileOutputStream fs= new FileOutputStream( _preferencesFile );
             _preferencesProperties.store(fs,
                                          "-------- User Preferences - Automaticly Generated -"+
                                          " Do not Edit -------- ");
             fs.close();
          }
     }
     catch (IOException e) {
          System.out.println("AppProperties.dumpPreference: " + e);
     }
  }

    public static void setAndLoadPreferenceFile(File f) throws IOException {
       _preferencesFile= f;
       if (f.exists()) addPreferences(f);
   }

    public static void addPreferences(InputStream fs) throws IOException {
        if (_preferencesProperties==null) {
            return;  // if _preferencesProperties is null then most probably we are using a server (tomcat) that has
            // done some sort of static unload during shutdown, in any normal mode of
            // operation _preferencesProperties is never null
        }
        addPropertiesFromStream(fs, _preferencesProperties);
        _userProperties.putAll(_preferencesProperties);
    }

    public static void addPreferences(File f) throws IOException {
        addPreferences( new FileInputStream( f ));
    }

//=========================================================================
//-------------------------------------------------------------------------
//=========================================================================

  public static void dumpUserProperties(File f) throws IOException {
      FileOutputStream fs= null;
      try {
         fs= new FileOutputStream( f );
         _userProperties.store(fs, "-------- Current User properties -------- ");
         System.out.println("User properties written to: " + f);
      } finally {
         if (fs!=null) fs.close();
      }
  }

  public static void addUserProperties(File f, boolean ignoreFileNotFound)
                                                         throws IOException {
      if (_userProperties==null) {
          return;  // if _userProperties is null then most probably we are using a server (tomcat) that has
                   // done some sort of static unload during shutdown, in any normal mode of
                   // operation _userProperties is never null
      }
      try {
         InputStream fs= new FileInputStream( f );
         addPropertiesFromStream(fs, _userProperties);
      } catch (FileNotFoundException e) {
          if (!ignoreFileNotFound) throw e;
      }
  }

  public static void addApplicationProperties(Class c, String resource)
                                                          throws IOException {

      if (_mainProperties==null) {
          return;  // if _mainProperties is null then most probably we are using a server (tomcat) that has
                   // done some sort of static unload during shutdown, in any normal mode of
                   // operation _mainProperties is never null
      }
      Assert.argTst(c, "objClass must not be null");
      Assert.argTst(resource, "resource must not be null");
      URL url=c.getResource(resource);
      if(url==null) {
          throw new FileNotFoundException("resource: "+resource+
                                          " could not be found");
      }
      if(notLoaded(url)) {
          addPropertiesFromStream(url.openStream(), _mainProperties);
          synchronized(_loadedResources) {
              _loadedResources.add(url);
          }
      }
  }

    public static void addApplicationProperties(String resource)
            throws IOException {

        if (_mainProperties==null) {
            return;  // if _mainProperties is null then most probably we are using a server (tomcat) that has
            // done some sort of static unload during shutdown, in any normal mode of
            // operation _mainProperties is never null
        }
        ClassLoader cl= AppProperties.class.getClassLoader();
        URL url= (cl==null) ? ClassLoader.getSystemResource(resource) :
                 cl.getResource(resource);
        if(url==null) {
            throw new FileNotFoundException("resource: "+resource+
                                                    " could not be found");
        }
        if(notLoaded(url)) {
            InputStream prop_stream=url.openStream();
            synchronized(_loadedResources) {
                _loadedResources.add(url);
            }
            addPropertiesFromStream(prop_stream, _mainProperties);
        }
    }


    public static void addApplicationProperties(File    f,
                                                boolean ignoreFileNotFound)
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

    public static void setProperty(String key, String value) {
        if (_mainProperties==null) {
            return;  // if _mainProperties is null then most probably we are using a server (tomcat) that has
            // done some sort of static unload during shutdown, in any normal mode of
            // operation _mainProperties is never null
        }
        _mainProperties.setProperty(key, value);
    }


    public static void addApplicationProperties(InputStream propStream,
                                                Properties loadToPDB)
            throws IOException {

        if (_mainProperties==null) {
            return;  // if _mainProperties is null then most probably we are using a server (tomcat) that has
            // done some sort of static unload during shutdown, in any normal mode of
            // operation _mainProperties is never null
        }
        if(loadToPDB==null) loadToPDB=_mainProperties;
        addPropertiesFromStream(propStream, loadToPDB);
    }




  /**
   * Load this classes class specific properties.  If this methoed is
   * called multiple times it will only load the class properties the first
   * time.  The properties are loaded into the application property database.
   *
   * Unlike other "loader" methods in AppProperties this class does not
   * throw and exception is simple return a boolean indicating whether the
   * properties where loaded.  That is class properties are sort of optional.
   * When you are developing you might not yet have the property file there
   * yet but still want to got through the running process.  The clas swill log
   * when a file is not found but will not create an Exception.
   *
   * @param  objClass the class that is loading the properties
   * @param  resource the name of the file to find in the class path
                              (it is probably in a jar file)
   * @return boolean is it loaded successfully
   */
  public static boolean loadClassProperties(Class objClass, String resource) {
      if (_classProperties==null) {
          return false; // if _classProperties is null then most probably we are using a server (tomcat) that has
                        // done some sort of static unload during shutdown, in any normal mode of
                        // operation _classProperties is never null
      }
      return loadClassPropertiesToPdb(objClass,resource,_classProperties,false);
  }


  public static boolean loadClassProperties(Class objClass) {
      if (_classProperties==null) {
          return false; // if _classProperties is null then most probably we are using a server (tomcat) that has
                        // done some sort of static unload during shutdown, in any normal mode of
                        // operation _classProperties is never null
      }
      return loadClassPropertiesToPdb(objClass,_classProperties, false);
  }

  /**
   * Load this classes class specific properties.  If this methoed is
   * class multiple times it will only load the class properties the first
   * time.  The properties are loaded into the application property database.
   *
   * The file this method will attempt to load is built on the classname, in
   * a "resources" subdirectory of the packages directory and will have an
   * an extension of ".prop". If the class name is "Abc" then the file name
   * built is "resources/Abc.prop".
   *
   * Unlike other "loader" methods in AppProperties this class does not
   * throw and exception is simple return a boolean indicating whether the
   * properties where loaded.  That is class properties are sort of optional.
   * When you are developing you might not yet have the property file there
   * yet but still want to got through the running process.  The method will log
   * when a file is not found but will not create an Exception.
   * @param  objClass the class that is loading the properties
   * @param  pdb load the properties to this property data base, if pdb is null
   *             then load to standard data base
   * @param forceLoad force this property file to load even if it is already loaded
   * @return boolean is it loaded successfully
   */
  public static boolean loadClassPropertiesToPdb(Class      objClass,
                                                 Properties pdb,
                                                 boolean forceLoad) {
       Assert.argTst(objClass, "objClas must not be null");
       String shortName= StringUtil.getShortClassName(objClass);
       String fname= "resources/" + shortName + ".prop";
       return loadClassPropertiesToPdb(objClass, fname, pdb, forceLoad);
  }



  /**
   * Load this classes class specific properties.  If this methoed is
   * called multiple times it will only load the class properties the first
   * time.  The properties are loaded into the application property database.
   *
   * Unlike other "loader" methods in AppProperties this class does not
   * throw and exception is simple return a boolean indicating whether the
   * properties where loaded.  That is class properties are sort of optional.
   * When you are developing you might not yet have the property file there
   * yet but still want to got through the running process.  The clas swill log
   * when a file is not found but will not create an Exception.
   * @param  objClass the class that is loading the properties
   * @param  resource the name of the file to find in the class path
                              (it is probably in a jar file)
   * @param  loadToPDB if this is not null then load this property database,
                        otherwise load to the main properties database
   * @param forceLoad force this property file to load even if it is already loaded
   * @return boolean is it loaded successfully
   */
  public static boolean loadClassPropertiesToPdb(Class     objClass,
                                                 String     resource,
                                                 Properties loadToPDB,
                                                 boolean    forceLoad) {
      if (_classProperties==null) {
          return false; // if _classProperties is null then most probably we are using a server (tomcat) that has
                        // done some sort of static unload during shutdown, in any normal mode of
                        // operation _classProperties is never null
      }
      boolean loaded= false;
      Assert.argTst(objClass, "objClass must not be null");
      Assert.argTst(resource, "resource must not be null");
      try {
          URL url= objClass.getResource(resource);
          if (url==null) {
              throw new FileNotFoundException(
                             "resource: " + resource + " could not be found");
          }
          if(forceLoad || notLoaded(url)) {
              if(loadToPDB==null) loadToPDB=_classProperties;
              addPropertiesFromStream(url.openStream(), loadToPDB);
              if (!forceLoad) {
                  synchronized(_loadedResources) {
                      _loadedResources.add(url);
                  }
              }
          }
          loaded= true;
      } catch (FileNotFoundException e) {
          System.out.println("AppProperties: Could not find resource: " +
                             resource);
      } catch (IOException e) {
          String errStr=
             "AppProperties: Class properties not found: ";
          System.out.println(errStr + resource);
          System.out.println("AppProperties: exception: " + e);
      }
      return loaded;
  }

    public static boolean loadClassPropertiesFromFileToPdb(File       file,
                                                           Properties loadToPDB) {
        boolean loaded= false;
        Assert.argTst(file, "resource must not be null");
        try {
            InputStream fs= new FileInputStream( file );
            if(loadToPDB==null) loadToPDB=_classProperties;
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
        // todo remove above
        Assert.argTst(pdb, "pdb must not be null");
        BufferedInputStream bis= new BufferedInputStream(propStream, 2048);
        try {
            if (_substitutionValues==null)  {
                pdb.load(bis);
            }
            else {
                AppProperties.loadSubstitionValues(bis, pdb);
                // TODO: tina changes
            }
        } finally {
            FileUtil.silentClose(bis);
        }
  }

    private static void loadSubstitionValues(BufferedInputStream bis, Properties pdb) throws IOException {
        Properties tempPDB = new Properties();
        tempPDB.load(bis);
        String key;
        String value;
        String newValue;

        for (Map.Entry<Object, Object> entry : tempPDB.entrySet()) {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
            newValue = value;

            if (key.endsWith(".Name") || key.endsWith(".ShortDescription") || key.endsWith(".Title") ||
                    key.endsWith(".Error")) {
                for (Map.Entry<String, String> subEntry : _substitutionValues.entrySet()) {
                    if (value.contains(subEntry.getKey()) && !(value.startsWith("<"))) {
                        newValue = value.replace(subEntry.getKey(), subEntry.getValue());
                    } else if (value.contains(subEntry.getKey()) && (value.startsWith("<"))) {
                        String [] htmlStartSplit = value.split("<");
                        if (htmlStartSplit.length > 1) {
                            StringBuffer newValueBuffer = new StringBuffer();
                            for (String a : htmlStartSplit) {
                                if (a.length() > 0) {
                                    String [] htmlFinalSplit = a.split(">",2);
                                    assert(htmlFinalSplit.length == 2);
                                    newValueBuffer.append("<" );
                                    newValueBuffer.append(htmlFinalSplit[0]);
                                    newValueBuffer.append(">");
                                    if (htmlFinalSplit[1].contains(subEntry.getKey())) {
                                        newValueBuffer.append(htmlFinalSplit[1].replace(
                                                subEntry.getKey(), subEntry.getValue()));
                                    }
                                    else {
                                        newValueBuffer.append(htmlFinalSplit[1]);
                                    }
                                }
                            }
                            newValue = newValueBuffer.toString();
                        }
                    } else {
                        //do nothing
                    }
                }
            }
            pdb.setProperty(key, newValue);
        }
    }

    /**
     * This is only called when you need to do language substitution from
     * American englist to real english
     * @param newValues value substitution map
     */
  public static void setSubstitutionValues (Map<String,String> newValues) {
      _substitutionValues = newValues;
  }
//=====================================================================
//----------- add / remove property Change listener methods -----------
//=====================================================================

    /**
     * Add a property changed listener. Because this is a listener on a
     * static class it uses week references for is property change listener list.
     * You must not add a listener if it is an anonymous inner class.
     * The listener will be garbage collected immediately.  You must add a listener
     * that is being pointed to by another object.
     * @param l listener
     */
    public static void addPropertyChangeListener (PropertyChangeListener l) {
        if (!serverMode) { // this listener is used for notifying when a preferences changes, this is only necessary in client mode
            _propChange.addPropertyChangeListener (l);
        }
    }

    /**
     * Remove a property changed listener.
     * @param l  the listener
     */
    public static void removePropertyChangeListener(PropertyChangeListener l){
       _propChange.removePropertyChangeListener (l);
    }


    public static void writeSizes() {
        System.out.printf("main= %d%nclass= %d%nuser= %d%npref= %d%n",
                          _mainProperties.size(),
                          _classProperties.size(),
                          _userProperties.size(),
                          _preferencesProperties.size());
    }

    public static void reportUnsedProperties() {
        if (_record!=null) {
            writeUnusedProperties("Main",  _mainProperties);
            writeUnusedProperties("Class",  _classProperties);
            writeUnusedProperties("Preferences", _preferencesProperties);
        }
        else {
            System.out.println("To use this method you must first set " +
                               DEVELOPER_MODE_PROP+"=true on the java command line");
            System.out.println("This property should not be set "+
                               "in production use.");
        }
    }

    private static void writeUnusedProperties(String prefix, Properties props) {
        Enumeration keys= props.propertyNames();
        String name;
        List<String> outList= new ArrayList<String>(1000);
        System.out.printf(prefix+ " properties has %d lines%n",
                          props.size());
        while(keys.hasMoreElements()) {
            name= (String)keys.nextElement();
            if (!_record.containsKey(name)) {
                outList.add(prefix +" Unused: "+name+"="+
                            props.getProperty(name));
            }
        }
        Collections.sort(outList);
        for(String s: outList) System.out.println(s);

    }

    public static List<String> getAllProperties() {
        List<String> outList= new ArrayList<String>(1000);
        Enumeration keys= _mainProperties.propertyNames();
        String name;
        while(keys.hasMoreElements()) {
            name= (String)keys.nextElement();
            outList.add(name+"="+_mainProperties.getProperty(name));
        }
        keys= _classProperties.propertyNames();
        while(keys.hasMoreElements()) {
            name= (String)keys.nextElement();
            outList.add(name+"="+_classProperties.getProperty(name));
        }
        Collections.sort(outList);
        return outList;
    }

    public static HashMap<String,String> convertClassPropertiesToMap() {
        HashMap<String,String> map= new HashMap<String,String>(_classProperties.size()+45);
        for(Map.Entry entry : _classProperties.entrySet()) {
            map.put((String)entry.getKey(),(String)entry.getValue());
        }
        return map;
    }

    public static HashMap<String,String> convertMainPropertiesToMap() {
        HashMap<String,String> map= new HashMap<String,String>(_mainProperties.size()+45);
        for(Map.Entry entry : _mainProperties.entrySet()) {
            map.put((String)entry.getKey(),(String)entry.getValue());
        }
        return map;
    }

    public static String convertMainPropertiesToString() {
        return convertPropertiesToString(_mainProperties);
    }

    public static String convertPropertiesToString(Properties pdb) {
        ByteArrayOutputStream out= new ByteArrayOutputStream(pdb.size()*80);
        String retval;
        try {
            pdb.store(out, "");
            retval= out.toString();
        } catch (IOException e) {
            retval= null;
        }
        return retval;
    }

//=====================================================================
//----------- Private / Protected Methods -----------------------------
//=====================================================================

    /**
     * check to see if a URL has every been loaded.  If it has never been
     * loaded then return a true.
     * @param url this url we are checking to see if we have loaded
     * @return true if the file has never been loaded
     */
    private static boolean notLoaded(URL url) {
        boolean found= false;
        synchronized (_loadedResources) {
            Iterator i= _loadedResources.iterator();
            while(i.hasNext() && !found) {
                found= url.sameFile( (URL)i.next() );
            }
        }
        return (!found);
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
