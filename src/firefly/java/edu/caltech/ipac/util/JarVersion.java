/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipException;


/**
 * A class to retrieve version information from a jar file. <br>
 * It looks in a jar file for a version file or manifest entries.  
 * The manifest is the prefered way to do it.  The property file style is
 * being phased out.
 * <p>
 * The version property file is 
 * a property file that contains version information about the jar file.  For 
 * example- if a jar file is named <code>myfile.jar</code> this class will look 
 * for a jar file named myfileVersion.prop in the root directory inside 
 * the jar file. <p>
 * The properties that are searched for have the following format: 
 * <em>JarFileName</em>.JarVersion.<em>type</em></br>
 * For Example- If the major version number of myfile.jar is 5. The 
 * property file (myfileVersion.prop) would 
 * define the major Version property to be 
 * <code>myfile.JarVersion.Major=5</code> <br>
 * You may define the following properties:
 * <ul>
 * <li><em>JarFileName</em><b>.JarVersion.Major=</b><em>version number</em>
 * <li><em>JarFileName</em><b>.JarVersion.Minor=</b><em>version number</em>
 * <li><em>JarFileName</em><b>.JarVersion.Revision=</b><em>revision number</em>
 * <li><em>JarFileName</em><b>.JarVersion.Type=</b><em>type description</em>
 * </ul>
 * <p>
 * The manifest style uses the the following format: 
 * <em>JarFileName-type</em></br>
 * For Example- If the major version number of myfile.jar is 5. The 
 * manifest entry would look like:
 * <code>myfile-MajorVersion: 5</code> <br>
 * You may define the following manifest entries:
 * <ul>
 * <li><em>JarFileName</em><b>-MajorVersion: </b><em>version number</em>
 * <li><em>JarFileName</em><b>-MinorVersion: </b><em>version number</em>
 * <li><em>JarFileName</em><b>-Revision: </b><em>revision number</em>
 * <li><em>JarFileName</em><b>-Type: </b><em>type description</em>
 * <li><em>JarFileName</em><b>-BuildDate: </b><em>date string</em>
 * </ul>
 * <p>
 * <p>
 * The known types are Beta, Final, Development. However, you may specify any
 * string for a type.
 *
 * @author Trey Roby
 */
public class JarVersion {

   public final static String PROP_V_MAJOR       = ".JarVersion.Major";
   public final static String PROP_V_MINOR       = ".JarVersion.Minor";
   public final static String PROP_V_REVISION    = ".JarVersion.Revision";
   public final static String PROP_V_TYPE        = ".JarVersion.Type";
   public final static String PROP_DATE          = ".JarVersion.BuildDate";
   public final static String DISPLAYED_NAME     = ".displayed.Name";

   public final static String MANIFEST_V_MAJOR   = "MajorVersion";
   public final static String MANIFEST_V_MINOR   = "MinorVersion";
   public final static String MANIFEST_V_REVISION= "Revision";
   public final static String MANIFEST_V_TYPE    = "Type";
   public final static String MANIFEST_V_HOST    = "Hostname";
   public final static String MANIFEST_V_USER    = "Username";
   public final static String MANIFEST_DATE      = "BuildDate";
   public final static String MANIFEST_DISPLAYED_NAME = "DisplayedName";
   public final static String MANIFEST_AU_TARGET_DIR  = "AutoUpdateTargetDir";
   public final static String MANIFEST_AU_ATTRIBUTES  = "AutoUpdateAttributes";
   public static final String MANIFEST_EXTRA_JARS= "AutoUpdateExtraJars";

   public final static String BETA_TYPE       =  "Beta";
   public final static String FINAL_TYPE      =  "Final";
   public final static String DEVELOPMENT_TYPE=  "Development";

   public final static String NOTES           =  "Notes-";
   public final static String TICKETS         =  "Tickets-";
   public final static String LINE            =  "-Line-";

   public final static String CRITICAL_ATTRIBUTE =  "CRITICAL";
   public static final String META_ATTRIBUTE = "META";
   public static final String JRE_ATTRIBUTE= "JRE";
   public static final String EXPAND_ATTRIBUTE= "EXPAND";

   private final static String IMP_VERSION    = "Implementation-Version";

   private int        _majorVersion= -1;
   private int        _minorVersion= 0;
   private int        _revision    = 0;
   private String     _type        = FINAL_TYPE;
   private String     _dateStr     = "unknown";
   private Properties _pdb         = new Properties();
   private String     _tickets     = null;
   private List<String> _notesList   = null;
   private String     _builderHost = null;
   private String     _builderUser = null;
   private String     _displayedName= null;
   private String     _extraJars[]= null;

   private String     _updatetargetInstallDir= null;
   private String     _updateAttributes= null;
   private final boolean _versionFromManifest;

   /**
    * Look for the jar file, open it, and determine the version.  Either do
    * this by pulling the property file out and reading it or by looking at
    * the manifest.  If there is a property file then then it overrides the 
    * manifest.
    * @param theJarFile jar file to open
    * @throws FileNotFoundException if the jar file is not found or 
    *                               the for the property file inside the
    *                               jar file was not found.
    */
   public JarVersion(File theJarFile) throws FileNotFoundException {
       String      fullName;
       JarFile     jar= null;
       boolean     found= false;
       _versionFromManifest= true;
       try {
           jar= new JarFile(theJarFile);
           fullName= theJarFile.getName();
           int     extIndex= fullName.indexOf(".jar"); 
           String  baseName= fullName.substring(0,extIndex);
           _displayedName= theJarFile.getName();

           Manifest   manifest= jar.getManifest();

           if (manifest != null) {
               Attributes att= manifest.getAttributes(baseName);
               if (att != null) {
                     found= findVersionByManifest(att);
                     if (found) findReleaseNotes(att);
               }
           }
           if (!found&&(manifest!=null)) found= findAnyVersion(manifest);

           if (found) {
               if (_majorVersion<0) {
                  throw new FileNotFoundException(
                           "Version found but in wrong format"); 
               }
           }
           else {
               throw new 
                    FileNotFoundException("Version information not found"); 
           }
       } catch (FileNotFoundException fnfe) {
           throw fnfe;
       } catch (ZipException ze) {
           FileNotFoundException fnfe=
                          new FileNotFoundException(ze.getMessage());
           fnfe.initCause(ze);
           throw fnfe;
       } catch (IOException ioe) {
           System.err.println("JarVersion: " + ioe);
           System.err.println("jar file: " + theJarFile);
       } finally {
           try {
              if (jar != null) jar.close();
           } catch (IOException ioe) {/*ignore*/ }
       }
   }

   /**
    * make a JarVersion object from a property file
    * @param propStream the stream to the property file
    * @param baseName the property name
    * Pass a property file stream to supply the version
    * @throws IOException if the is an problem reading
    */
   public JarVersion(InputStream propStream, String baseName) 
                                                  throws IOException {
       _displayedName= baseName;
       _versionFromManifest= false;
       if (_displayedName==null) _displayedName= "";
        AppProperties.addPropertiesFromStream(propStream,_pdb);
        findVersionByProperties(baseName);
   }

   private void findVersionByProperties(String root) {
      _majorVersion= AppProperties.getIntProperty(
                                      root+PROP_V_MAJOR, _majorVersion, _pdb);
      _minorVersion= AppProperties.getIntProperty(
                                      root+PROP_V_MINOR, _minorVersion, _pdb);
      _revision    = AppProperties.getIntProperty(
                                      root+PROP_V_REVISION, _revision, _pdb);
      _type        = AppProperties.getProperty(
                                      root+PROP_V_TYPE,     _type, _pdb);
      _dateStr     = AppProperties.getProperty(root+PROP_DATE, _dateStr, _pdb);
      _displayedName= AppProperties.getProperty(root+DISPLAYED_NAME,
                                                _displayedName, _pdb);
   }


   private boolean findVersionByManifest(Attributes att) {
       String str;
       boolean found=false;

       //boolean containsBasicKeys=;
       //boolean containsOldStyleKeys=containsOldStyleKeys(att, root);

       //if(containsBasicKeys || containsOldStyleKeys) {
       if(containsKeys(att)) {
           //String prefix=  containsBasicKeys ? "" : root+"-";

           str=att.getValue(MANIFEST_V_MAJOR);
           _majorVersion=getNum(str, _majorVersion);

           str=att.getValue(MANIFEST_V_MINOR);
           _minorVersion=getNum(str, _minorVersion);

           str=att.getValue(MANIFEST_V_REVISION);
           _revision=getNum(str, _revision);

           if (att.containsKey(new Attributes.Name(MANIFEST_V_TYPE))) {
               _type=att.getValue(MANIFEST_V_TYPE);
           }

           if (att.containsKey(new Attributes.Name(MANIFEST_DATE))) {
              _dateStr=att.getValue(MANIFEST_DATE);
           }


           _builderHost=att.getValue(MANIFEST_V_HOST);
           _builderUser=att.getValue(MANIFEST_V_USER);


           _updatetargetInstallDir= att.getValue(MANIFEST_AU_TARGET_DIR);
           _updateAttributes      = att.getValue(MANIFEST_AU_ATTRIBUTES);

           if (att.containsKey(new Attributes.Name(MANIFEST_DISPLAYED_NAME))) {
               _displayedName= att.getValue(MANIFEST_DISPLAYED_NAME);
           }
           
           _extraJars= null;
           if (att.containsKey(new Attributes.Name(MANIFEST_EXTRA_JARS))) {
               String s= att.getValue(MANIFEST_EXTRA_JARS);
               String sAry[]= s.split(" ");
               List<String> list= new ArrayList<String>(sAry.length);
               for(String tst : sAry) {
                   if (tst.endsWith(".jar")) list.add(tst);
               }
               if (list.size()>0) {
                   _extraJars= list.toArray(new String[list.size()]);
               }
           }
           found=true;
       } // end if 
       return found;
   }

    private boolean containsKeys(Attributes att) {
        return   att.containsKey(new Attributes.Name(MANIFEST_V_MAJOR))    &&
                 att.containsKey(new Attributes.Name(MANIFEST_V_MINOR))    &&
                 att.containsKey(new Attributes.Name(MANIFEST_V_REVISION));
    }


   private void findReleaseNotes(Attributes att) {

      _notesList   = new ArrayList<String>(5);
      _tickets= att.getValue(TICKETS+ _majorVersion +"-"+ _minorVersion);
      boolean found= true;
      Attributes.Name key;
      for(int i= 0; (found); i++)  {
         key=  new Attributes.Name(NOTES+ _majorVersion +"-"+ 
                                      _minorVersion + LINE + i);
         found= att.containsKey(key);
         if (found) {
               _notesList.add(att.getValue(key));
         }
      }
      if (_notesList.size() == 0)_notesList= null;
   }

   private boolean findAnyVersion(Manifest manifest) {
      Attributes att= manifest.getMainAttributes();
      boolean retval= false;
      if (att.containsKey(new Attributes.Name(IMP_VERSION)) ) {
             String fullVersion= att.getValue(IMP_VERSION);
             parseToVersion(fullVersion);
             retval= true;
      }
      else {
             Map entries= manifest.getEntries();
             Set keySet= entries.keySet();
             Iterator i= keySet.iterator();
             String key;
             while (i.hasNext()) {
                key= (String)i.next(); 
                att= manifest.getAttributes(key);
                if (att.containsKey(new Attributes.Name(IMP_VERSION)) ) {
                   String fullVersion= att.getValue(IMP_VERSION);
                   parseToVersion(fullVersion);
                   retval= true;
                }
             }
      }
      return retval;
   }

   private void parseToVersion(String fullVersion) {
      _majorVersion= 0;
      _minorVersion= 0;
      _revision    = 0;
      StringTokenizer st = new StringTokenizer(fullVersion, ".");
      try {
          _majorVersion= Integer.parseInt(st.nextToken());
          _minorVersion= Integer.parseInt(st.nextToken());
          _revision= Integer.parseInt(st.nextToken());
      } catch (NoSuchElementException e) {
            // do nothing
      } catch (NumberFormatException e) {
            // do nothing
      }
   }

   private int getNum(String val, int def) {
      int retval;
      try {
          if (val != null) retval= Integer.parseInt(val);
          else             retval= def;
      } catch (NumberFormatException e) {
          retval= def;
      }
      return retval;
   }


   /**
    * The Major version of the jar file.
    * return int the major version number
    * @return the major version
    */
   public int    getMajorVersion() { return _majorVersion; }

   /**
    * The Minor version of the jar file.
    * return int the minor version number
    * @return the minor version
    */
   public int    getMinorVersion() { return _minorVersion; }

   /**
    * The revision of the jar file.
    * return int the revision version number
    * @return the revision version
    */
   public int    getRevision() { return _revision; }

   /**
    * The status type of the jar file  (Final, Beta, Development)
    * return String the status type string
    * @return the type string
    */
   public String getType() { return _type; }

   /**
    * Return the build data of this Jar file
    * @return the build date as a string
    */
   public String getBuildDate() { return _dateStr; }

   public boolean isVersionFromManifest() { return _versionFromManifest; }

   public String getAutoUpdateTargetInstallDir() {
       return _updatetargetInstallDir;
   }

    public File getAutoUpdateTargetInstallDirFile() {
        File retval= null;
        if (_updatetargetInstallDir!=null) {
            String pathParts[]= _updatetargetInstallDir.split("[\\/\\\\]");
            StringBuffer sb= new StringBuffer(_updatetargetInstallDir.length()+5);
            for(int i=0; (i<pathParts.length); i++) {
                sb.append(pathParts[i]);
                if (i<pathParts.length-1) sb.append(File.separator);
            }
            retval= new File(sb.toString());
        }
        return retval;
    }



   public String getAutoUpdateAttributes() { return _updateAttributes; }


   public boolean containsAttribute(String testAtt) {
       boolean retval= false;
       if (_updateAttributes!=null) {
           String att[]= _updateAttributes.split(" ");
           if (testAtt!=null) {
               for(String a: att) {
                   if (a.equalsIgnoreCase(testAtt)) {
                       retval= true;
                       break;
                   }
               }
           }
       }
       return retval;
   }




    @Deprecated
   public boolean getAutoUpdateExpandJarFlag() {
        return containsAttribute(EXPAND_ATTRIBUTE);
    }

   public String getTickets() { return _tickets; }

   public String getBuilderHost() { return _builderHost; }
   public String getBuilderUser() { return _builderUser; }

   public String[] getReleaseNotes() { 
        String[] retval= null;
        if (_notesList!=null) {
           retval= _notesList.toArray(new String[0]);
        }
        return retval;
   }

    public String getDisplayedName() { return _displayedName; }

    public boolean getHasExtraJars() {
       return _extraJars!=null;
    }
    
    public String[] getExtraJars() {
        return _extraJars;
    }

    /**
     * Return true is this JarVersion instance is a newer version than the
     * one passed in.
     * @param jv another JarVersion instance
     * @return true if this version is newer, false otherwise
     */
    public boolean isNewerVersion(JarVersion jv) {
        boolean retval=false;
        if(_majorVersion>jv._majorVersion) {
            retval=true;
        }
        else if(_majorVersion==jv._majorVersion) {
            if(_minorVersion>jv._minorVersion) {
                retval=true;
            }
            else if(_minorVersion==jv._minorVersion) {
                if(_revision>jv._revision) {
                    retval=true;
                }
            }
        }
        return retval;
    }

   /**
    * String description of version
    * @return the version as a string
    */
   public String getVersionString() {
      return getVersionString(false);
   }

   public String getVersionString(boolean forceBuiltBy) {
      String retval;
      if (_revision == 0) {
         retval= _majorVersion +"."+ _minorVersion
                 +"  "+ _type;
      }
      else {
         retval= _majorVersion +"."+ _minorVersion
                 +"."+ _revision +"  "+ _type;
      }
      if ( (_type.equalsIgnoreCase(DEVELOPMENT_TYPE) || forceBuiltBy) &&
            _builderUser!=null && 
            _builderHost!=null ) {
              retval+= "- Built by: " + _builderUser +"@"+ _builderHost;
      }
      return retval;

   }

   /**
    * String description of the class.
    */
   public String toString() {
      return getVersionString() + "     date: " + _dateStr;
   }

    private static void doVerbose(String fname, JarVersion jv) {
        String outName= "Version: ";
        if (fname!=null) outName= fname + ": ";

        System.out.println(outName + jv.getVersionString(true));
        System.out.println("  Date: " + jv.getBuildDate() );

        String att= jv.getAutoUpdateAttributes();
        att= (att==null) ? "<none>" : att;
        System.out.println("  AutoUpdate attributes: " + att);
        System.out.println("  AutoUpdate expand flag: " +
                           jv.containsAttribute(EXPAND_ATTRIBUTE));
        System.out.println("  Description : " +jv.getDisplayedName());
        if (jv._updatetargetInstallDir!=null) {
            System.out.println("  Target dir : " +
                 jv.getAutoUpdateTargetInstallDirFile().getPath());
        }
        if (jv.getHasExtraJars()) {
            System.out.println("  Extra Jars : " +Arrays.toString(jv.getExtraJars()));
        }

        String t= jv.getTickets();
        String rl[]= jv.getReleaseNotes();
        if (t!=null) System.out.println("  Tickets: " + t);
        if (rl!=null) {
            System.out.println("  Release Notes:");
            for(String s : rl)  System.out.println( "    " + s);
        }
        System.out.println();
    }

    private static void doBrief(String fname, JarVersion jv) {
        String outName= "Version: ";
        if (fname!=null) outName= fname + ": ";
        System.out.println(outName +
                           jv.getVersionString(true) +
                           "     date: " + jv.getBuildDate() );
    }

   public static void main(String args[]) {
       /*  for debugging:*/ //try {System.in.read(); } catch (IOException e) {}
       JarVersion jv;
       boolean verbose= false;
       boolean doingFile= false;
       String  name= null;
       int start= -1;

       boolean done= false;

       for(int i=0; (i<args.length && !done); i++) {
           if (args[i].charAt(0) == '-') {
                 if (args[i].equals("-v")) {
                       verbose= true;
                 }
                 else if (args[i].equals("-file")) {
                       doingFile= true;
                 }
                 else if (args[i].equals("-name")) {
                       if (i+1 < args.length) { 
                          name= args[++i];
                       }
                       else {
                          System.out.println("The -name argument requires " +
                                             "a name as the next paremeter.");
                          System.exit(0);
                       }
                 }
           }
           else {
              start= i;
              done= true;
           }
       } // end loop

       if (!done)  start= args.length;

       if (doingFile) {
            if (name != null) {
                  try {
                      InputStream in= new FileInputStream (new 
                                                File(args[start]));
                      jv= new JarVersion(in, name);
                      if (verbose)  doVerbose(null, jv);
                      else          doBrief(null, jv);
                  } catch (IOException ioe) {
                       System.err.println("JarVersion: " + ioe);
                  }
            }
       }
       else {
          for (int i= start; (i<args.length); i++) {
               try {
                 jv= new JarVersion(new File(args[i]));
                 if (verbose)  doVerbose(args[i], jv);
                 else          doBrief(args[i], jv);
               } catch (FileNotFoundException e) {
                 System.out.println(args[i] + ": " + "no version information");
               } // end catch
          } // end loop
       } // else
   }
}

