package edu.caltech.ipac.client;




import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.JarVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/*
 * This class will search for the application version by looking for a property
 * file with the name based on the appication name
 * It will serach the classpath for a file name <i>app-name</i>-app-version.prop
 */
public class ApplicationVersion {

   public static final String FILE_BASE= "-app-version.prop";

   private static ApplicationVersion _installedVersion= null;

   private  String     _displayedName= null;
   private  String     _appName= null;
   private  File       _jarDir = null;  // if null use classpath
   private  JarVersion _jv     = null;


   public static ApplicationVersion getInstalledApplicationVersion() {
       if (_installedVersion==null) {
                _installedVersion= new ApplicationVersion(null);
       }
       return _installedVersion;
   }

   public ApplicationVersion(String appName) {
       if (appName!=null) setAppName(appName);
   }

   public ApplicationVersion(String appName, File jarDir) {
       _jarDir= jarDir;
       if (appName!=null) setAppName(appName);
   }

   public  boolean isAppNameSet() { return _appName!=null; }
   public  boolean isVersionSet() { return _jv     !=null; }


   public  void setAppName(String appName) {
      _appName= appName;
      _displayedName= appName;
      determineVersion();
   }

   public String getAppName() { return _appName; }

   public String getDisplayedName() { return _displayedName; }
   /**
    * The Major version of the jar file.
    * return int the major version number
    */
   public  int    getMajorVersion() { 
       return (_jv!=null) ? _jv.getMajorVersion() : 0;
    }

   /**
    * The Minor version of the jar file.
    * return int the minor version number
    */
   public  int    getMinorVersion() { 
      return (_jv!=null) ? _jv.getMinorVersion() : 0; 
   }

   /**
    * The revision of the jar file.
    * return int the revision version number
    */
   public  int    getRevision() { 
      return (_jv!=null) ? _jv.getRevision() : 0; 
   }

   /**
    * The status type of the jar file  (Final, Beta, Development)
    * return String the status type string
    */
   public  String getType() { 
      return (_jv!=null) ? _jv.getType() : "none"; 
   }


   public String getBuildDate() { 
      return (_jv!=null) ? _jv.getBuildDate() : "unknown";
   }

   public  String getVersionString() {
      return (_jv!=null) ? _jv.getVersionString() : "";
   }


   private void determineVersion() {
      String propFile= _appName+FILE_BASE;
      JarFile  jar= null;


      InputStream in= null;
      if (_jarDir==null) { //if jar directory is null then use classpath
          in= ClassLoader.getSystemResourceAsStream(propFile);
      }
      else {  // else look at all the jars in this directory for the file
          JarEntry entry;
          File     jarFiles[]= FileUtil.listJarFiles(_jarDir);
          if (jarFiles!=null && jarFiles.length>0) {
               for(int i=0; (i<jarFiles.length && in==null); i++) { 
                     try {
                        jar   = new JarFile(jarFiles[i]);
                        entry = jar.getJarEntry(propFile);
                        if (entry!=null) {
                            in=  jar.getInputStream(entry);
                        }
                        else {
                            jar.close();
                        }
                      } catch (IOException e) {}
               }// end loop
          } // end if
      } // end else



      _jv= null;
      if (in!=null) {
         try {
             String baseName= _appName + ".application";
            _jv= new JarVersion(in, baseName);
            _displayedName= _jv.getDisplayedName();
             if (_displayedName==null || baseName.equals(_displayedName))  {
                 _displayedName= _appName;
             }
        } catch( IOException e ) {
            _jv= null;
            ClientLog.warning("No verison information found for - " + propFile, 
                               e.toString());
        }
        try {
           if (jar!= null) jar.close();
        } catch (IOException e) {}
      }
      else {
          ClientLog.warning("Could not find the version file- " + propFile);
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
