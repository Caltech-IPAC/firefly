package edu.caltech.ipac.util;


import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.action.Prop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * this utility class does all sorts of activities releated to installing
 * edu.caltech.ipac common based application.
 * It will install all the start up files as well as return information about
 * the installation.
 * Installation are currently supported on linux, sun win, mac, and any generic unix
 * where the jre is supplied externally
 */

public class Installer {

   public static final String DEFAULT_META_JAR = "meta.jar";

   private static final String LINUX            = "linux";
   private static final String SUN              = "sun";
   private static final String WIN              = "windows";
   private static final String MAC              = "mac";

   private static final String PLATFORM         = "platform";

   private static final String GENERIC_UNIX     = "genericUnix";
   private static final String JRE_DIR_PROP     = "installer.jre.dir";
//   private static final String JRE_USE_JAVAHOME_PROP =
//                                      "installer.jre.useJavaHome";
   private static final String META_JAR_PROP =  "installer.meta.jar";
   private static final String MAC_APP_PROP        = "installer.mac.app";
   private static final String JRE_DIR= System.getProperty("java.home");

   private static final String MAC_APP= System.getProperty(MAC_APP_PROP,null);
   private static final String PATH_SEPARATOR= System.getProperty("path.separator");

   private static final String TCSH= "#!/bin/tcsh -f";
   private static final String CSH=  "#!/bin/csh -f";


    public static final String MAIN_CLASS_VARIBLE= "MAIN_CLASS";
    public static final String EXE_JAR_VARIBLE= "EXE_JAR";
    public static final String JRE_JAR_VARIBLE= "jreJar";
    public static final String REQUIRE_JRE_VARIBLE= "require_jre_param";
    public static final String MEM_VARIBLE= "MEM";
    public static final String DISPLAY_NAME_VARIBLE= "displayName";

    public static final String REQUIRE_JRE_DEFAULT= "false";
    public static final String MEM_DEFAULT=  "-Xms12m -Xmx256m";




   private static final Properties _installerProps = new Properties();

    //these next varibles are set on the first call that uses them
   private static String _rootDir= null;
   private static List<String> _trueClassPathList= null;

   private Installer() { }

   public static void main( String[] args ) {
       // try {System.in.read(); } catch (IOException e) {} // for debugging
       File expandDir=checkExpandAnyJars(args);
       if(doHelp(args)) {
           System.out.println("This programs works in several modes:");
           System.out.println("-help  or -h                - this message");
           System.out.println("");
           System.out.println("Classlist from file mode:");
           System.out.println("   -exeJarClasspath or -ecp <jarfile>  - show the classpath defined by the jar file");
           System.out.println("       addtional parameters: -ignoreErrors : ignore any errors");
           System.out.println("                             -pretty");
           System.out.println("");
           System.out.println("Classpath from dir mode:");
           System.out.println("     -makeDirToClassPath <dir>   - make a classpath string from all the");
           System.out.println("                                   jar in this directory ");
           System.out.println("                                   and any jar that are defined in their manifest");
           System.out.println("Expand jar mode:");
           System.out.println("   -expandAnyJars <dir>         - expand any jars marked for expansion in directory ");
           System.out.println("");
           System.out.println("Extra jar mode:");
           System.out.println("   -extraJars or -ej         - find any extra jars defined by jar in this directory");
           System.out.println("");
           System.out.println("no parameters - make an installation");
           System.exit(0);
       }
       else if(doExtraJarMode(args)) {
           File f= new File(System.getProperty("user.dir"));
           boolean ignoreErrors= ignoreErrors(args);
           getExtraJars(f, true, !ignoreErrors);
//           for(String entry : ej )  {
//               System.out.println(entry);
//           }

       }
       else if(doGetClassPathMode(args)) {
           File jar= doGetClassPathJar(args);
           boolean ignoreErrors= ignoreErrors(args);
           String cp[]= null;
           if (jar!=null) cp= getClassPathFromExeJar(jar, ignoreErrors);
           if(cp!=null) {
               if (pretty(args)) {
                   for(String entry : cp )  System.out.println(entry);
               }
               else {
                   for(String entry : cp )  System.out.print(entry+" ");
               }
           }
           else {
               System.out.println("");
           }
       }
       else if(makeClassPathFromDirMode(args)) {
           File f= new File(System.getProperty("user.dir"));
           String cp= makeClassPathFromAllJarsInDir(f);
           if (pretty(args)) {
               String cpAry[]= cp.split(PATH_SEPARATOR);
               for(String entry : cpAry )  {
                   System.out.println(entry);
               }
           }
           else {
               System.out.println(cp);
           }
       }
       else if(expandDir!=null) {
           expandMarkedJars(expandDir);
       }
       else {
           File root=deriveInstallationRootFile();
//           if(jreDir==null) {
//               boolean useJavaHome= AppProperties.getBooleanProperty(
//                                             JRE_USE_JAVAHOME_PROP, false);
//               if(useJavaHome) jreDir=JRE_DIR;
//           }
           try {
               doInstall(root);
           } catch (Exception e) {
               System.out.println("Error in install");
               System.out.println("      "+e.toString());
               e.printStackTrace();
               System.exit(22);
           }
       }
   }


   static public void installApp(File root) throws IOException {
       doInstall(root);
   }

   public static String getAutoUpdateDownloadDir() {
       String downloadDir;
       if (OSInfo.isPlatform(OSInfo.MAC)) {
           downloadDir= "Resources/download";
       }
       else {
           downloadDir= "download";
       }
       return downloadDir;
   }

   public static String getAutoUpdateUpdateDir() {
       String updateDir;
       if (OSInfo.isPlatform(OSInfo.MAC)) {
           updateDir= "Resources/update";
       }
       else {
           updateDir= "update";
       }
       return updateDir;
   }

   public static String getInstallationLibDir() {
       String updateDir;
       if (OSInfo.isPlatform(OSInfo.MAC)) {
           updateDir= "Resources/Java";
       }
       else {
           updateDir= "lib";
       }
       return updateDir;
   }

    public static String getSysExtensionDir() {
        String updateDir;
        if (OSInfo.isPlatform(OSInfo.MAC))  updateDir= "Resources/extensions";
        else                                updateDir= "extensions";
        return updateDir;
    }

    public static String getUserExtensionDir() { return "extensions"; }


    public static String getInstallationPlatformDir() {
        String pDir;
        if (OSInfo.isPlatform(OSInfo.MAC)) {
            pDir= "Resources/" + PLATFORM + File.separator + MAC;
        }
        else if (OSInfo.isPlatform(OSInfo.LINUX)) {
            pDir= PLATFORM + File.separator + LINUX;
        }
        else if (OSInfo.isPlatform(OSInfo.SUN)) {
            pDir= PLATFORM + File.separator + SUN;
        }
        else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
            pDir= PLATFORM + File.separator + WIN;
        }
        else {
            pDir= PLATFORM + File.separator + "unknown";
        }
        return pDir;

    }

   public static File getInstallationLibDirFile() {
        return new File(deriveInstallationRootFile(),
                        getInstallationLibDir() );
   }

   public static File getAutoUpdateUpdateDirFile() {
        return new File(deriveInstallationRootFile(),
                        getAutoUpdateUpdateDir() );
   }

   public static File deriveInstallationRootFile() {
        return new File(deriveInstallationRoot());
   }

    public static File getInstallationPlatformDirFile() {
        return new File(deriveInstallationRootFile(),
                        getInstallationPlatformDir() );
    }

    public static File getSysExtensionDirFile() {
        return new File(deriveInstallationRootFile(),getSysExtensionDir());
    }


    public static String deriveInstallationRoot() {
        if (_rootDir==null) {
            URL url= getThisClassURL();
            String urlStr= url.toString();
            int start= urlStr.indexOf('/');
            int end= urlStr.lastIndexOf('!');
            String fileStr;
            if (start < end) {
                fileStr= urlStr.substring(start, end);
                File jarFile= new File(fileStr);
                File jarsDir= jarFile.getParentFile();
                File tmpDir;
                if (OSInfo.isPlatform(OSInfo.MAC)) {
                    tmpDir= jarsDir.getParentFile().getParentFile();
                }
                else {
                    tmpDir= jarsDir.getParentFile();
                }

                String cleanValue;
                String preClean= tmpDir.getPath();


                // There is the issues with the plus sign (+) and decode
                // decode automaticly turns the plus to a blank.  That is not
                // good if there is a plus sign in the path.  However, decode
                // is still nice to use.  The next section of code uses docode
                // when there is no plus sign and uses the old way when there is.
                // There old way just looks for spaces a %20 and turns them into
                // spaces.
                if (preClean.indexOf('+')<0) {
                    try {
                        cleanValue= URLDecoder.decode(preClean, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        System.out.println("Installer.deriveInstallationRoot: " + e);
                        cleanValue= tmpDir.getPath().replaceAll("%20", " ");
                    }
                }
                else {
                    cleanValue= tmpDir.getPath().replaceAll("%20", " ");
                }


                File rootDirFile= new File(cleanValue);
                if (rootDirFile.exists()) {
                    _rootDir= rootDirFile.getPath();
                }
                System.out.println("Installer: (" + jarFile.getName()+
                                   ") rootDir: " + _rootDir);
            }
            else {
                _rootDir= System.getProperty("user.dir");
                System.out.println("Installer: Not a standard installation.");
                System.out.println("           "+ getThisClassFileName() +
                                   " is not in a jar file.");
                System.out.println("Installer: rootDir: " + _rootDir);
            }
        }
        return _rootDir;
   }

   public static File findDirWithJars() {
       URL url= getThisClassURL();
       String urlStr= url.toString();
       int start= urlStr.indexOf('/');
       int end= urlStr.lastIndexOf('!');
       String fileStr= urlStr.substring(start, end);
       File jarsDir= new File(fileStr);

       String cleanValue;
       try {
           cleanValue= URLDecoder.decode(jarsDir.getPath(), "UTF-8");
       } catch (UnsupportedEncodingException e) {
           System.out.println("Installer.findDirWithJars: " + e);
           cleanValue= jarsDir.getPath().replaceAll("%20", " ");
       }

       jarsDir= new File(cleanValue);
       jarsDir= jarsDir.getParentFile();
       File retval= null;
       if (jarsDir.exists()) retval= jarsDir;

       return retval;
   }


    public static void installOnAlternateMacLocation(File   root,
                                                     String appName)
                                                           throws IOException {
        if(OSInfo.isPlatform(OSInfo.MAC)) {
            MetaJarGroup jarGroup=null;
            try {
                jarGroup= new MetaJarGroup();
                String apps[]=getValidAppList(jarGroup);
                installOnMac(apps, jarGroup, root, appName);
            } finally {
                if(jarGroup!=null) jarGroup.close();
            }
        }
   }


    public static Process launchApp(File installdir, String appname, String parameters[])
                         throws IOException {
        ProcessBuilder procBuilder;
        Process process= null;
        List<String> params= new ArrayList<String>(5);

        if (OSInfo.isPlatform(OSInfo.MAC)) {
            File exe= new File(installdir.getParent());
            params.add("/usr/bin/open");
            params.add("-a");
            params.add(exe.getPath());
            if (parameters!=null) Collections.addAll(params,parameters);
            procBuilder = new ProcessBuilder(params);
            procBuilder.directory(null);
            process= procBuilder.start();

        }
        else if (OSInfo.isPlatform(OSInfo.ANY_UNIX)) {
            File exe= new File(installdir,appname);
            params.add(exe.getPath());
            if (parameters!=null) Collections.addAll(params,parameters);
            procBuilder = new ProcessBuilder(params);
            procBuilder.directory(null);
            process= procBuilder.start();
        }
        else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
            System.out.println("here");
            System.out.println("installdir:"+installdir.getPath());
            File exe= new File(installdir,appname);
//            params.add(appname+ ".exe");
            params.add(exe.getPath());
            if (parameters!=null) Collections.addAll(params,parameters);
            for (String s : params) System.out.println(s);
            procBuilder = new ProcessBuilder(params);
            procBuilder.directory(installdir);
            process= procBuilder.start();
        }
        for (String s : params) System.out.println(s);
        return process;
    }


    public static File[] getTrueClassPathAsFileAry() {
        List<String> strList= getTrueClassPathAsList();
        File fAry[]= new File[strList.size()];
        for(int i= 0; (i<strList.size()); i++) {
            fAry[i]= new File(strList.get(i));
        }
        return fAry;
    }

    public static String getTrueClassPath() {
        return makeClassPathString(getTrueClassPathAsList());
    }

    public static List<String> getTrueClassPathAsList() {
        if (_trueClassPathList==null) {
            String          classPath = System.getProperty("java.class.path");
            String cpAry[]= classPath.split(PATH_SEPARATOR);
            List<String> cpList;
            String entry;
            _trueClassPathList= new ArrayList<String>(25);
            for(String jarStr : cpAry) {
                cpList = getClassPathFromJar(new File(jarStr), true);
                for(Iterator<String> j= cpList.iterator();(j.hasNext());) {
                    entry= j.next();
                    if (!_trueClassPathList.contains(entry)) {
                        _trueClassPathList.add(entry);
                    }
                }
            }
        }
        return _trueClassPathList;
    }

    /**
     *  make a classpath using the jars in this directory and any jars
     * that they specify in their manifest
     * @param dir the directory to make the classpath from
     * @return a String with a classpath
     */
    public static String makeClassPathFromAllJarsInDir(File dir) {
        File jarFiles[]= FileUtil.listJarFiles(dir);
        List<String> masterList= new ArrayList<String>(25);
        if (jarFiles!=null) {
            List<String> cpList;
            String entry;

            for(File f: jarFiles) {
                cpList = getClassPathFromJar(f, false);
                for(Iterator<String> j= cpList.iterator();(j.hasNext());) {
                    entry= j.next();
                    if (!masterList.contains(entry)) masterList.add(entry);
                }
            }
        }
        return makeClassPathString(masterList);
    }

    public static void expandInstallationJar(JarVersion jv,
                                             File f,
                                             File rootDir,
                                             File libPackageDir) throws IOException {
        expandInstallationJar(jv,f,rootDir,libPackageDir,null);
    }


    public static void expandInstallationJar(JarVersion jv,
                                             File f,
                                             File rootDir,
                                             File libPackageDir,
                                             JarExpander.ExpandStatus  status) throws IOException {
        File jarTargetDir= jv.getAutoUpdateTargetInstallDirFile();
        File outDir= libPackageDir;
        if (jarTargetDir!=null && !jarTargetDir.isAbsolute()) {
            outDir= new File(rootDir,jarTargetDir.getPath());
        }
        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
               throw new IOException("could not create directory: " +
                                     outDir.getPath());
            }
        }
        // create place holder file
        String baseName= FileUtil.getBase(f);
        String propFileStr= f.getName() + ".prop";
        File propFile= new File(libPackageDir, propFileStr);
        FileWriter fw= new FileWriter(propFile);
        try {
            fw.write( String.format("%s= %d%n",
                                    baseName+JarVersion.PROP_V_MAJOR ,
                                    jv.getMajorVersion()));
            fw.write( String.format("%s= %d%n",
                                    baseName+JarVersion.PROP_V_MINOR ,
                                    jv.getMinorVersion()));
            fw.write( String.format("%s= %d%n",
                                    baseName+JarVersion.PROP_V_REVISION,
                                    jv.getRevision()));
            fw.write( String.format("%s= %s%n",
                                    baseName+JarVersion.PROP_V_TYPE,
                                    jv.getType()));
        } finally {
            fw.close();
        }
        JarExpander.expand(f, outDir,false, true,status);
        f.deleteOnExit();
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static boolean doGetClassPathMode(String args[]) {
        boolean found= false;
        for(int i=0; (i<args.length&&!found); i++) {
            found= ("-exeJarClasspath".equals(args[i]) ||
                   "-ecp".equals(args[i]) );
        }
        return found;
    }

    private static boolean doExtraJarMode(String args[]) {
        boolean found= false;
        for(int i=0; (i<args.length&&!found); i++) {
            found= ("-extraJars".equals(args[i]) ||
                    "-ej".equals(args[i]) );
        }
        return found;
    }

    private static boolean doHelp(String args[]) {
        boolean found= false;
        for(int i=0; (i<args.length&&!found); i++) {
            found= ("-help".equals(args[i]) ||
                    "-h".equals(args[i]) );
        }
        return found;
    }

    private static boolean ignoreErrors(String args[]) {
        boolean found= false;
        for(int i=0; (i<args.length&&!found); i++) {
            found= "-ignoreErrors".equals(args[i]);
        }
        return found;
    }

    private static boolean pretty(String args[]) {
        boolean found= false;
        for(int i=0; (i<args.length&&!found); i++) {
            found= "-pretty".equals(args[i]);
        }
        return found;
    }

    private static boolean makeClassPathFromDirMode(String args[]) {
        boolean found= false;
        for(int i=0; (i<args.length&&!found); i++) {
            found= "-makeDirToClassPath".equals(args[i]);
        }
        return found;
    }

    private static File checkExpandAnyJars(String args[]) {
        File f;
        boolean found= false;
        File retval= null;
        for(int i=0; (i<args.length&&!found); i++) {
            found= "-expandAnyJars".equals(args[i]);
            if (found && args.length>i+1) {
                f= new File(args[i+1]);
                if (f.canRead()) {
                    if (deriveInstallationRootFile().canWrite()) {
                        retval= f;
                    }
                }
            }
        }
        return retval;
    }

    private static File doGetClassPathJar(String args[]) {
        boolean found= false;
        File retval= null;
        for(int i=0; (i<args.length&&!found); i++) {
            found= ("-exeJarClasspath".equals(args[i]) ||
                    "-ecp".equals(args[i]) );
            if (found && (i+1<args.length)) {
                retval= new File(args[i+1]);
            }
        }
        return retval;
    }



    static private void doInstall(File   root) throws IOException {
        MetaJarGroup jarGroup= null;
        try {
            jarGroup= new MetaJarGroup();
            String apps[]= getValidAppList(jarGroup);
            if (apps.length == 0) throw new IOException("No Apps");
            if (!OSInfo.isPlatform(OSInfo.MAC)) {
                if (OSInfo.isPlatform(OSInfo.LINUX)) {
                    installOnLinux(apps, jarGroup,root);
                }
                else if (OSInfo.isPlatform(OSInfo.SUN)) {
                    installOnSun(apps, jarGroup,root);
                }
                else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
                    installOnWin(apps, jarGroup,root);
                }
                else if (OSInfo.isPlatform(OSInfo.UNKNOWN_UNIX)) {
                    installOnGenericUnix(apps, jarGroup,root);
                }
                else {
                    Assert.tst(false);
                }
            }
            else {
                installOnMac(apps, jarGroup, root, MAC_APP);
            }
        } finally {
            if (jarGroup!=null) jarGroup.close();
        }
    }

    private static String[] getExtraJars(File dir, boolean log, boolean showErrors) {
        List<String> extraJars= new ArrayList<String>(5);
        JarVersion jv;
        String cp= makeClassPathFromAllJarsInDir(dir);
        String cpAry[]= cp.split(PATH_SEPARATOR);
        File jar;


        for(String jStr : cpAry) {
            jar= new File(jStr);
            try {
                jv= new JarVersion(jar);
                if (jv.getHasExtraJars()) {
                    String jAry[]= jv.getExtraJars();
                    if (log) System.out.println("Extra Jars (defined by "+jar.getName() + ")");
                    for(String j : jAry) {
                        extraJars.add(j);
                        if (log) System.out.println("     " + j);
                    }
                }
            } catch (FileNotFoundException e) {
                if (showErrors) System.out.println(jStr +": " + e.getMessage());
            }
        }
        return extraJars.toArray(new String[extraJars.size()]);
    }

    private static String[] getClassPathFromExeJar(File infile) {
        return getClassPathFromExeJar(infile,false);
    }

    private static String[] getClassPathFromExeJar(File infile,
                                                   boolean ignoreErrors) {
        List<String> cpList = getClassPathFromJar(infile, !ignoreErrors);
        return cpList.toArray(new String[cpList.size()]);
    }

   static private String [] getValidAppList(MetaJarGroup jarGroup)
                                                 throws IOException {
       String plist;
       String apps[];
       String retapps[];
       JarGroupEntry entry   = jarGroup.getJarEntry("applist.prop");
       InputStream in   = jarGroup.getInputStream(entry);
       _installerProps.load( in );

       String appString= _installerProps.getProperty( "loader.applications.Name");
       if (appString == null || appString.length() == 0)
          throw new IOException("Nothing found for loader.boot.classpath");
       apps= StringUtil.strToStrings(appString);

       int idx;
       int numApps= 0;
       String platform= getOSProp();
       for(int i=0; (i<apps.length); i++) {
           plist= _installerProps.getProperty( "loader." + apps[i] + ".platforms.Name");
           idx= plist.indexOf(platform);
           if (idx > -1) {
              numApps++;
           }
           else {
              apps[i]= null;
           }
       }
       retapps= new String[numApps];
       int j=0;
       for(String app : apps) {
          if (app != null) {
            retapps[j++]= app;
          }
       }
       return retapps;
   }


   static private void installOnMac(String  apps[],
                                    MetaJarGroup jarGroup,
                                    File    root,
                                    String  macApp) throws IOException {
       System.out.println("Installer installOnMac, apps: "+ Arrays.toString(apps));
       System.out.println("Installer installOnMac, macApp: "+ macApp);
      if (macApp!=null) {
         boolean found= false;
         for(int i=0; (i<apps.length && !found); i++) {
               found= apps[i].equals(macApp);
         }

         if (found) {
             System.out.println("Installer installOnMac plist icns");
             String plist= "mac-" + macApp + "-Info.plist";
             JarGroupEntry entry   = jarGroup.getJarEntry(plist);
             if (entry!=null) {
                 extractTextFile(entry, new File(root, "Info.plist"), jarGroup );
             }
             else {
                 writeMacFallback(macApp, root, jarGroup);

             }

            try { // icns file is optional
                System.out.println("Installer installOnMac extracting icns");
              File macRes= new File(root, "Resources");
              extractFile("mac-" + macApp + ".icns",
                          new File(macRes, macApp + ".icns"), jarGroup );
            } catch (IOException e) {
                System.out.println("Installer: not installed: " +
                                   macApp + ".icns");
            }
         }
      } // end if
   }


    private static void writeMacFallback(String app, File root, MetaJarGroup jarGroup)
                                                   throws IOException {
        CharArrayWriter charWriter= new CharArrayWriter(2000);
        BufferedWriter writer= new BufferedWriter(charWriter,100);
        String fallbackFile= "mac-FALLBACK-Info.plist";
        LineNumberReader in=  getLineNumberReader(fallbackFile, jarGroup, true);
        writeTextFile(in,writer);
        writer.close();
        String plistStr= charWriter.toString();
        plistStr= checkAndWriteMacVarible(plistStr, "%%%APP-NAME%%%", app, // to the first APP-NAME
                                          DISPLAY_NAME_VARIBLE, app);
        plistStr= plistStr.replaceAll("%%%APP-NAME%%%", app);              // do the rest of the APP-NAME
        plistStr= checkAndWriteMacVarible(plistStr, "%%%VMOptions%%%", app,
                                          MEM_VARIBLE, MEM_DEFAULT);
        plistStr= checkAndWriteMacVarible(plistStr, "%%%MAIN_CLASS%%%", app,
                                          MAIN_CLASS_VARIBLE, null);
        plistStr= checkAndWriteMacVarible(plistStr, "%%%EXE_JAR%%%", app,
                                          EXE_JAR_VARIBLE, null);
        LineNumberReader reader= new LineNumberReader(
                             new CharArrayReader(plistStr.toCharArray()));
        File plistFile= new File(root, "Info.plist");
        BufferedWriter out=  new BufferedWriter(new FileWriter(plistFile), 2000);
        writeTextFile(reader, out);
        out.close();
        System.out.println("Installer: Writing: " + plistFile.getPath());
    }



    static private String checkAndWriteMacVarible(String inString,
                                                 String holderStr,
                                                 String  app,
                                                 String  name,
                                                 String  fallbackValue) {
        String retval= inString;
        String rootProp= "loader."+app;
        String value= Prop.getPlatformProp(rootProp,
                                           name +"." + ActionConst.VALUE,
                                           fallbackValue,
                                           true, _installerProps);
        System.out.println("checkAndWriteMacVarible replacing: "+holderStr +" with " + value );
        if (value!=null) {
            retval= inString.replace(holderStr, value);
        }
        return retval;
    }




   static private void installOnLinux(String  apps[],
                                      MetaJarGroup jarGroup,
                                      File    root) throws IOException {
      installOnAnyUnix("linux", apps, jarGroup, root, null);
   }

   static private void installOnSun(String  apps[],
                                    MetaJarGroup jarGroup,
                                    File    root) throws IOException {
      installOnAnyUnix("sun", apps, jarGroup, root, null);
   }

   static private void installOnGenericUnix(String  apps[],
                                            MetaJarGroup jarGroup,
                                            File    root) throws IOException {
      String jreDir=System.getProperty(JRE_DIR_PROP, JRE_DIR);
      installOnAnyUnix("genericUnix", apps, jarGroup, root, jreDir);
   }


   static private void installOnAnyUnix(String  prefix,
                                        String  apps[],
                                        MetaJarGroup jarGroup,
                                        File    root,
                                        String  jreDir) throws IOException {
       BufferedWriter out;
       LineNumberReader    in;
       File script;

       for(String app : apps) {
           script= new File(root, app);
//	  out  = new PrintWriter(
//                          new BufferedWriter(new FileWriter(script), 4096));
           out=  new BufferedWriter(new FileWriter(script), 4096);

           writeHeader(out, app);

           writeln(out,"set APP_ROOT = " + "\"" + root.getPath() + "\"" );
           if (jreDir != null) {
               writeln(out,"set JAVA = " + "\"" + jreDir + "/bin/java"  + "\"" );
           }
           in=  getLineNumberReader( prefix + "-" + app + ".params.csh",
                                     jarGroup, false);
           if (in!=null)  writeTextFile(in, out);
           else           writeUnixFallbackParams(out, app);
           in=  getLineNumberReader( app + ".unix-common.csh",
                                     jarGroup, false);
           if (in!=null)  {
               writeTextFile(in, out);
           }
           else {
               in=  getLineNumberReader( "FALLBACK.unix-common.csh",
                                         jarGroup, true);
               writeTextFile(in, out);
           }
           FileUtil.silentClose(out);
           System.out.println("Installer: Writing: " + script.getPath());
           String chmodCmd= "chmod +x " + script.getPath();
           Runtime.getRuntime().exec( chmodCmd );
       }
   }


   static private void writeUnixFallbackParams(BufferedWriter out,
                                               String app)  throws IOException {
       writeln(out,"# %%% END installation %%%");
       writeln(out,"#");
       checkAndWriteUnixVarible(out, app, EXE_JAR_VARIBLE);
       checkAndWriteUnixVarible(out, app, JRE_JAR_VARIBLE);
       checkAndWriteUnixVarible(out, app, REQUIRE_JRE_VARIBLE,
                                          REQUIRE_JRE_DEFAULT, false);
       checkAndWriteUnixVarible(out, app, MEM_VARIBLE, MEM_DEFAULT, true);

       writeln(out,"#");
       writeln(out,"# ---- Begin platform specific parameters");
       writeln(out,"#");
       writeln(out,"set APP_DEFS = ( \"-Dloader.restart.prog=$0\"    \\");
       writeln(out,"                  -DAssert.exitOnFail=false     \\");
       writeln(out,"                  -DautoUpdate.enabled=true)");
       writeln(out,"#");
       writeln(out,"#");
       writeln(out,"# ---- End platform specific parameters");

   }

    static private void checkAndWriteUnixVarible(BufferedWriter out,
                                                 String app,
                                                 String name)
                                                   throws IOException {
        checkAndWriteUnixVarible(out,app,name,null, false);
    }

    static private void checkAndWriteUnixVarible(BufferedWriter out,
                                                 String  app,
                                                 String  name,
                                                 String  fallbackValue,
                                                 boolean surround)
                                                         throws IOException {
        String rootProp= "loader."+app;
        String value= Prop.getPlatformProp(rootProp,
                                           name +"." + ActionConst.VALUE,
                                           fallbackValue,
                                           true, _installerProps);
        if (value!=null) {
            if (surround) value= "("+value+")";
            writeln(out,"set " + name +" = " + value);
        }
    }

   static private void writeHeader(BufferedWriter out, String appName)
                                                      throws IOException {
       boolean tcshExist= new File("/bin/tcsh").canRead();
       String shell= tcshExist ? TCSH : CSH;
       writeln(out,shell);
       writeln(out,"#");
       writeln(out,"#  ----------- "+ OSInfo.getRecognizedPlatform() +
                   " version of the " + appName+ " start script");
       writeln(out,"#");
   }

   static private void writeln(BufferedWriter bw, String s) throws IOException {
       bw.write(s);
       bw.newLine();
   }

   static private void installOnWin(String  apps[],
                                    MetaJarGroup jarGroup,
                                    File    root) throws IOException {

       if (OSInfo.isPlatform(OSInfo.ANY_WIN_NT)) {
           extractFile("win-restart.exe", new File(root, "restart.exe"), jarGroup );
           extractFile("win-restart.bat", new File(root, "restart.bat"), jarGroup );
           extractFilePrependString("win-restart.cfg", root.getPath(),
                                    new File(root, "restart.cfg"), jarGroup );
       }
       for(String app : apps) {
           String exeFile= "win-" + app + ".exe";
           String cfgFile= "win-" + app + ".cfg";
           if (jarGroup.getJarEntry(exeFile)!=null) {
               extractFile(exeFile, new File(root, app  +".exe"), jarGroup  );
           }
           else {
               extractFile("win-FALLBACK.exe",
                           new File(root, app  +".exe"), jarGroup  );
           }
           if (jarGroup.getJarEntry(cfgFile)!=null) {
               extractFilePrependString(cfgFile, root.getPath(),
                                        new File(root, app  +".cfg"), jarGroup  );
           }
           else {
               writeWinFallbackCfg(app, root);
           }
       }
   }



    static private void writeWinFallbackCfg(String app, File root)
                                                          throws IOException {

        String cfgStr= "";
        cfgStr= checkAndWriteWinVarible(cfgStr, app, MEM_VARIBLE, MEM_DEFAULT,false);
        cfgStr= checkAndWriteWinVarible(cfgStr, app, JRE_JAR_VARIBLE, null, true);
        cfgStr+= " -DautoUpdate.enabled=true -DAssert.exitOnFail=false " +
                  "-Dloader.restart.prog=" + app + " -jar lib\\";
        cfgStr= checkAndWriteWinVarible(cfgStr, app, EXE_JAR_VARIBLE, null, false);
        cfgStr+= " -log";
        File cfgFile= new File(root, app+".cfg");
        BufferedWriter out  = new BufferedWriter(new FileWriter(cfgFile), 4096);
        writeln(out, root.getPath());
        writeln(out, "jre\\bin\\javaw.exe");
        writeln(out, cfgStr);
        out.close();
        System.out.println("Installer: Writing: " + cfgFile.getPath());
    }


    static private String checkAndWriteWinVarible(String inString,
                                                  String  app,
                                                  String  name,
                                                  String  fallbackValue,
                                                  boolean space) {
        String retval= inString;
        if (inString==null) inString= "";
        String rootProp= "loader."+app;
        String value= Prop.getPlatformProp(rootProp,
                                           name +"." + ActionConst.VALUE,
                                           fallbackValue,
                                           true, _installerProps);
        if (value!=null) {
            retval= inString + (space ? " " : "")  + value;
        }
        return retval;
    }

   static private void extractFilePrependString(String   jaredFileName,
                                                String   prependString,
                                                File     outFile,
                                                MetaJarGroup jarGroup)
                                                      throws IOException {
      System.out.println("Installer: Writing: " + outFile.getPath());
      LineNumberReader in=  getLineNumberReader(jaredFileName, jarGroup, true);
      BufferedWriter out  = new BufferedWriter(new FileWriter(outFile), 4096);
      out.write(prependString);
      out.newLine();
      writeTextFile(in, out);
      out.close();
   }

    static private void extractTextFile(String   jaredFileName,
                                        File     outFile,
                                        MetaJarGroup jarGroup)
                                           throws IOException {
        System.out.println("Installer: Writing: " + outFile.getPath());
        LineNumberReader in=  getLineNumberReader(jaredFileName, jarGroup, true);
        BufferedWriter out  = new BufferedWriter(new FileWriter(outFile), 4096);
        writeTextFile(in, out);
        out.close();
    }

    static private void extractTextFile(JarGroupEntry   jaredEntry,
                                        File     outFile,
                                        MetaJarGroup jarGroup)
                                  throws IOException {
        System.out.println("Installer: Writing: " + outFile.getPath());
        LineNumberReader in=  getLineNumberReader(jaredEntry, jarGroup);
        BufferedWriter out  = new BufferedWriter(new FileWriter(outFile), 4096);
        writeTextFile(in, out);
        out.close();
    }



   static private void extractFile(String   jaredFileName,
                                   File     outFile,
                                   MetaJarGroup jarGroup) throws IOException {
      System.out.println("Installer: Writing: " + outFile.getPath());
      DataInputStream      in;
      BufferedOutputStream out;
      in= getDataInputStream(jaredFileName, jarGroup, false);
      if (in==null) {
           throw new IOException(jaredFileName + " not found in " +
                                 jarGroup.getName());
      }

      out= new BufferedOutputStream(new FileOutputStream(outFile), 4096);
      try {
         while(true) {
            out.write(in.readByte());
         }
      } catch (EOFException e) {
           // do nothing but stop
      } finally {
          FileUtil.silentClose(in);
          FileUtil.silentClose(out);
      }
   }


   static private void writeTextFile(LineNumberReader  in, BufferedWriter out)
                   throws IOException {
        try {
            String str= "";
            while(str!=null) {
                str= in.readLine();
                if (str!=null) {
                       writeln(out,str);
                }
            }
        } finally {
            if (in!=null) in.close();
        }
   }



   static private DataInputStream getDataInputStream(String  entryStr,
                                                     MetaJarGroup jarGroup,
                                                     boolean  warn)
                                                        throws IOException {
      Assert.tst(entryStr!=null);
      DataInputStream retval= null;
      JarGroupEntry entry   = jarGroup.getJarEntry(entryStr);
      if (entry!=null) {
         retval= new DataInputStream(new BufferedInputStream(
                                        jarGroup.getInputStream(entry)));
      }
      else {
          if (warn) {
              System.out.println("Installer: could not find file: " + entryStr +
                                 " in " + jarGroup.getName());
          }
      }
      return retval;
   }

    static private LineNumberReader getLineNumberReader(String  entryStr,
                                                        MetaJarGroup jarGroup,
                                                        boolean  warn)
                                                throws IOException {
        Assert.tst(entryStr!=null);
        LineNumberReader retval= null;
        JarGroupEntry entry   = jarGroup.getJarEntry(entryStr);
        if (entry!=null) {
            retval= getLineNumberReader(entry, jarGroup);
        }
        else {
            if (warn) {
                System.out.println("Installer: could not find file: " + entryStr +
                                   " in " + jarGroup.getName());
            }
        }
        return retval;
    }



    static private LineNumberReader getLineNumberReader(JarGroupEntry entry,
                                                        MetaJarGroup jarGroup)
                                  throws IOException {
        LineNumberReader retval= null;
        if (entry!=null) {
            retval= new LineNumberReader(new InputStreamReader(
                                          jarGroup.getInputStream(entry)));
        }
        return retval;
    }



    static public String getOSDirName()  {
        return getOSProp();
    }


   static public String getOSProp()  {
       String retval= null;
       if      (OSInfo.isPlatform(OSInfo.SUN)) {
          retval= SUN;
       }
       else if (OSInfo.isPlatform(OSInfo.LINUX)) {
          retval= LINUX;
       }
       else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
          retval= WIN;
       }
       else if (OSInfo.isPlatform(OSInfo.MAC)) {
          retval= MAC;
       }
       else if (OSInfo.isPlatform(OSInfo.UNKNOWN_UNIX)) {
          retval= GENERIC_UNIX;
       }
       else {
          Assert.tst(false);
       }
       return retval;
   }

    private static String makeClassPathString(List<String> jarList) {
        StringBuffer retClassPath= new StringBuffer(200);
        String entry;
        for(Iterator<String> j= jarList.iterator();(j.hasNext());) {
            entry= j.next();
            retClassPath.append(entry);
            if (j.hasNext()) retClassPath.append(PATH_SEPARATOR);
        }
        return retClassPath.toString();
    }

    protected static String getClassPathFromManifest(File f,
                                                     boolean logJarsNotFound) {
        String retval= null;
        String fsep  = System.getProperty("file.separator");

        try {
            JarFile jar= new JarFile(f);
            String dir= f.getParent();
            Manifest   manifest= jar.getManifest();
            if(manifest!=null) { // some jars do not contain manifest
                Attributes att= manifest.getMainAttributes();
                Attributes.Name cpName= new Attributes.Name("Class-Path");
                if (att.containsKey(cpName) ) {
                    String cp= att.getValue(cpName);
                    String rStr;
                    if (dir!=null) rStr= dir+fsep;
                    else           rStr= "";
                    String cpAry[]= cp.split(" ");
                    StringBuffer sb= new StringBuffer(200);
                    for(int i=0; (i<cpAry.length); i++) {
                        sb.append(rStr);
                        sb.append(cpAry[i]);
                        if (i<cpAry.length-1) sb.append(PATH_SEPARATOR);
                    }
                    retval= sb.toString();
                }
            }
        } catch (IOException e) {
            if (logJarsNotFound) {
                String err;
                if (e.getMessage().indexOf("cannot find")>0) {
                   err= "Not Found.";
                }
                else {
                    err= e.getMessage();
                }
                System.out.println("Installer: getClassPathFromManifest: "+
                                   f.getName()+ ": " + err);

            }
        }
        return retval;
    }






    private static List<String> getClassPathFromJar(File f,
                                                    boolean logJarNotFound) {
        return  getClassPathFromJar(f, new ArrayList<String>(5),
                                    logJarNotFound);
    }

    private static List<String> getClassPathFromJar(File f,
                                                    List<String> retList,
                                                    boolean logJarNotFound) {
        if (!retList.contains(f.getPath())) retList.add(f.getPath());
        String cp= f.isDirectory() ? null : getClassPathFromManifest(
                                                    f,logJarNotFound);
        if (cp!=null) {
            String cpAry[]= cp.split(PATH_SEPARATOR);
            for(String cStr : cpAry) {
                if (!retList.contains(cStr)) {
                    retList.add(cStr);
                    getClassPathFromJar( new File(cStr), retList,
                                         logJarNotFound);
                } // end if !contains
            }
        }
        return retList;
    }

    private static URL getThisClassURL() {
        return ClassLoader.getSystemResource(getThisClassFileName());
    }

    private static String getThisClassFileName() {
        String cName= Installer.class.getName();
        return cName.replace(".", "/") + ".class";
    }


    private static void expandMarkedJars(File dir) {
        File fAry[]= FileUtil.listJarFiles(dir);
        JarVersion jv;
        for(File f: fAry) {
            try {
                jv= new JarVersion(f);
                if (jv.containsAttribute(JarVersion.EXPAND_ATTRIBUTE)) {
                    File expandDir= jv.getAutoUpdateTargetInstallDirFile();
                    if (expandDir==null) expandDir= dir;
                    System.out.println("Installer: Expanding jar: " +
                                       f.getName()+
                                      " to " + expandDir.getPath());
                    Installer.expandInstallationJar(jv,f,
                                 Installer.deriveInstallationRootFile(),dir);
                }

            } catch (FileNotFoundException ignore) {
                // if it is not there don't do anything
            } catch (IOException e) {
                System.out.println("Installer: Expanding jar failed: " +
                                   f.getName());
            }
        }
    }


    private static class MetaJarGroup {
        private final List<JarFile> _jarGroup= new ArrayList<JarFile>(3);

        public MetaJarGroup() {
            File fAry[]= FileUtil.listJarFiles(getInstallationLibDirFile());
            File fallBackMeta= findFallbackMeta(fAry);
            JarVersion jv;
            System.out.println("Installer: MetaJarGroup lastToSearch: "+fallBackMeta);
            for(File f : fAry) {
                if (!f.equals(fallBackMeta)) {
                    try {
                        jv= new JarVersion(f);
                        if (jv.containsAttribute(JarVersion.META_ATTRIBUTE)) {
                            _jarGroup.add(new JarFile(f));
                            System.out.println("Installer: MetaJarGroup found: "+f);
                        }
                    } catch (IOException ignore) {
                        // if there is an exception we just don't add it
                    }
                } // end if
            } // end foreach
            try {
                if (fallBackMeta!=null) {
                    _jarGroup.add(new JarFile(fallBackMeta));
                }
            } catch (IOException ignore) {
                // if there is an exception we just don't add it
            }
        }

        private File findFallbackMeta(File fAry[]) {
            String metaJar= AppProperties.getProperty(META_JAR_PROP,
                                                      DEFAULT_META_JAR);
            File retval= null;
            for(File f : fAry) {
               if  (f.getName().equalsIgnoreCase(metaJar)) {
                   retval= f;
                   break;
               }
            }
            return retval;
        }


        JarGroupEntry getJarEntry(String entryStr) {

            JarGroupEntry retval= null;
            JarEntry entry;
            for(JarFile jar : _jarGroup) {
                entry= jar.getJarEntry(entryStr);
                if (entry!=null) {
                    retval= new JarGroupEntry(jar,entry);
                    System.out.println("Installer: getJarEntry: jar: "+jar.getName()+
                                       " entryStr: " +entryStr);
                    break;
                }
            }
            if (retval==null) {
                System.out.println("Installer: : getJarEntry: failed to find: " +entryStr);
            }
            return retval;
        }

        InputStream getInputStream(JarGroupEntry entry) throws IOException {
            InputStream in= null;
            if (entry!=null) {
                in   = entry.getJar().getInputStream(entry.getEntry());
            }
            return in;
        }

        void close() {
            for(JarFile jar : _jarGroup)  FileUtil.silentClose(jar);
        }

        String getName() {
            StringBuffer sb= new StringBuffer(100);
            for(int i= 0; (i<_jarGroup.size()); i++) {
                sb.append(_jarGroup.get(i).getName());
                if      (i<_jarGroup.size()-2) sb.append(", ");
                else if (i==_jarGroup.size()-2) sb.append(", or ");
            }
            return sb.toString();
        }
    }

    private static class JarGroupEntry {
        private final JarFile _jar;
        private final JarEntry _entry;
        JarGroupEntry (JarFile jar, JarEntry entry) {
            _jar= jar;
            _entry= entry;
        }
        JarFile getJar() { return _jar; }
        JarEntry getEntry() { return _entry; }
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
