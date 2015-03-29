/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
    /**
     * all the class level set properties are kept here.
     */
    private final static Properties _classProperties=new Properties();



//=========================================================================
//------------------------ other static  variables -------------------------
//=========================================================================

    private final static List<URL>      _loadedResources=new LinkedList<URL>();



//=========================================================================
//------------------------- Float properties ------------------------------
//=========================================================================


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

        //NOTE preference support has been removed
//        String retval= def;
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
//        if (isPref) {
//            retval= _preferencesProperties.getProperty(key, def);
//            if (retval==def) retval= _userProperties.getProperty(key, def);
//        }
        return getAppProp(key,def,overridePDB);
//
//        return retval;
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
   * Load this classes class specific properties.  If this method is
   * class multiple times it will only load the class properties the first
   * time.  The properties are loaded into the application property database.
   *
   * The file this method will attempt to load is built on the class name, in
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
       Assert.argTst(objClass, "obj Class must not be null");
       String shortName= ServerStringUtil.getShortClassName(objClass);
       String fname= "resources/" + shortName + ".prop";
       return loadClassPropertiesToPdb(objClass, fname, pdb, forceLoad);
  }



  /**
   * Load this classes class specific properties.  If this method is
   * called multiple times it will only load the class properties the first
   * time.  The properties are loaded into the application property database.
   *
   * Unlike other "loader" methods in AppProperties this class does not
   * throw and exception is simple return a boolean indicating whether the
   * properties where loaded.  That is class properties are sort of optional.
   * When you are developing you might not yet have the property file there
   * yet but still want to got through the running process.  The class swill log
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
        Assert.argTst(pdb, "pdb must not be null");
        BufferedInputStream bis= new BufferedInputStream(propStream, 2048);
        try {
            AppProperties.loadSubstitutionValues(bis, pdb);
        } finally {
            FileUtil.silentClose(bis);
        }
  }

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
//----------- add / remove property Change listener methods -----------
//=====================================================================


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

