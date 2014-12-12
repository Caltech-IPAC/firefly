package edu.caltech.ipac.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The AppLoader class performs two functions: <ol> <li>Move any .jar files found in the "update" 
 *  directory to the "lib"
 * directory</li> <li>Located in the directory specified by the <b>loader.root.dir</b> property</li>
 * <li>(Optional) <b>loader.debug</b> enables debugging print statements </ol> <br>Usage:
 * <br>-Dloader.root.dir= &lt root dir &gt -Dloader.prop= &lt properties &gt file &lt args &gt OR
 * <br>-Dloader.boot.class= &lt boot class &gt -Dloader.root.dir= &lt root dir &gt -Dloader.jre.dir= &lt jre dir &gt &lt args &gt
 * <br> The arguments &lt args &gt are passed to the AppLoader main will be forwarded to the boot class main. <br> <BR>
 * Copyright (C) 1999 California Institute of Technology. All rights reserved.<BR>
 * US Government Sponsorship under NASA contract NAS7-918 is acknowledged. <BR>
 * @author <a href="mailto:jchavez@ipac.caltech.edu?subject=Java Docs">Joe Chavez</a>
 * @version $Id: AppLoader.java,v 1.17 2007/11/30 22:38:01 roby Exp $
 */
public class AppLoader {

    //private static final String JRE_VALUE= "JRE";
    //private static final String NONE_VALUE= "NONE";


    private boolean _jreRelative  = false;
    private String  _jreDir       = null;
    private boolean _debug        = false;
    private String  _rootDir;
    private File    _updatePackageDir;
    private File    _libPackageDir;
    private String  _mainClass;
    private String  _inClasspath;
    private String  _jreParams[];
    private String  _jreJar;
    private String  _metaJar;
    private String  _restartProg;
    private String  _appDefine[];
    private int     _sleepTime= -1;
    private String  _logfile;
    private static String  _jvmcmd;

//    private String  _jarAttributes;
//    private File  _jarTargetDir;
//    private boolean _jarExpandFlag= false;

    static {
        if (OSInfo.isPlatform(OSInfo.ANY_UNIX_OR_MAC)) {
            _jvmcmd = "java";
        }
        else {
            _jvmcmd = "javaw";
        }
    }

    private AppLoader() {
        this(null,null,null,null,null,
             null,null,false,false,-1, null);
    }

    public static AppLoader makeSimpleAppLoader(String  rootDir,
                                                String  mainClass,
                                                String  jreDir,
                                                String  jreParams,
                                                String  appDefine[],
                                                boolean debug) {
       return new AppLoader(rootDir, mainClass, jreDir, jreParams,
                            null, null, appDefine, false, debug,
                            0, null);
    }

    public AppLoader(String  rootDir,
                     String  mainClass,
                     String  jreDir,
                     String  jreParams,
                     String  jreJar,
                     String  metaJar,
                     String  appDefine[],
                     boolean jreRelative,
                     boolean debug,
                     int     sleepTime,
                     String  logfile) {


        defineDirs(rootDir);

        String propFileName    = System.getProperty( "loader.prop", 
                                                      "appload.prop" );
        Properties loaderProps = null;
        InputStream in         = this.getClass().getResourceAsStream( 
                                                        "/" + propFileName );
        try {
            if ( in == null ) {
                in = new FileInputStream( _rootDir + File.separator + 
                                            propFileName );
            }
            loaderProps = new Properties( System.getProperties() );
            loaderProps.load( in );
        }
        catch( IOException e ) {
            //printMessage( "I/O error while loading properties from " +
            //                propFileName + " file." );
            //e.printStackTrace( System.err );
            loaderProps = null;
        }
        if ( loaderProps == null ) {
            printMessage( "Properties file not found will " + 
                           "use System properties" );
            loaderProps = new Properties( System.getProperties() );
        }
        // if debug mode is on, then echo out system properties and
        // set the jvm to "java" to allow printMessage to be seen in window

        setProperties(loaderProps, mainClass, jreDir,
                      jreParams, 
                      jreJar, metaJar, appDefine, jreRelative, debug, 
                      sleepTime, logfile);



        boolean missingArgs = false;

        if ( _rootDir.equals( "" ) ) {
            missingArgs = true;
        }    
        if ( _jreDir.equals( "" ) ) {
            //missingArgs = true;
        }
        else {
            if ( _jreRelative ) {
                _jreDir       = _rootDir + File.separator + _jreDir;
            }
        }
        if (_logfile != null && _logfile.length() > 1) {
              try {
                 File out= new File(_logfile);
                 PrintStream st= 
                        new PrintStream(new FileOutputStream(out, true), true);
                 System.setErr(st);
                 System.setOut(st);
                 //printMessage( "BEGIN: JVM System Properties -- TEMP" );
                 //System.getProperties().list( System.out );
                 //printMessage( "END: JVM System Properties -- TEMP" );
              } catch (IOException e) {
                  System.out.println("Could not redirect output: " + e);
              }
        }

        if ( _debug ) {
            Properties sysProp = System.getProperties();
            printMessage( "BEGIN: JVM System Properties" );
            sysProp.list( System.out );
            printMessage( "END: JVM System Properties" );
        }

        // print usage
        if ( missingArgs ) {
            printMessage( "loader.boot.class property not specified!" );
            printMessage( "Usage: " );
            printMessage( "\t-Dloader.root.dir=<root dir> " + 
                          "-Dloader.prop=<properties file> <args>" );
            printMessage( "OR" );
            printMessage( "\t-Dloader.boot.class=<boot class> " +
                          "-Dloader.root.dir=<root dir>" +
                          "-Dloader.jre.dir=<jre dir> <args>" );
            printMessage( "\tloader.prop defaults to apploader.prop" );
            printMessage( "\tThe <args> list will be passed to the boot " + 
                          "class main() method." );
            System.exit(0);
        }
        else {
            printProperties();
        }
    }


    private void setProperties(Properties p,
                               String     mainClass,
                               String     jreDir,
                               String     jreParams,
                               String     jreJar,
                               String     metaJar,
                               String     appDefine[],
                               boolean    jreRelative,
                               boolean    debug,
                               int        sleepTime,
                               String     logfile) {


        String tmpStr;
        if (mainClass==null) _mainClass= p.getProperty( 
                                                   "loader.boot.class", "" );
        else                 _mainClass= mainClass;

        _inClasspath   = p.getProperty( "loader.boot.classpath", "" );
        _restartProg   = p.getProperty( "loader.restart.prog", null );

        if (jreDir==null) _jreDir= p.getProperty( "loader.jre.dir", "" );
        else              _jreDir= jreDir;

        if (jreParams==null) tmpStr= p.getProperty( 
                                          "loader.jre.parameters", "" );
        else                 tmpStr= jreParams;
        _jreParams= tmpStr.split(" ");

        if (jreJar==null) _jreJar= p.getProperty( 
                                          "loader.jre.jarName", null );
        else              _jreJar= jreJar;

        if (metaJar==null) _metaJar= p.getProperty( 
                                          "loader.meta.jarName", null );
        else               _metaJar= metaJar;

        if (logfile==null) _logfile= p.getProperty( "loader.logfile", null );
        else               _logfile= logfile;

        if (appDefine==null) {
            tmpStr= p.getProperty( "loader.app.define", "" );
            _appDefine= tmpStr.split(" ");
        }
        else {
            _appDefine= appDefine;
        }
       

	if (!jreRelative) _jreRelative= Boolean.valueOf(
                             p.getProperty(
                                    "loader.jre.pathRelative")).booleanValue();
        else              _jreRelative= true;

        if (!debug) _debug= Boolean.valueOf( 
                             p.getProperty("loader.debug")).booleanValue();
        else                 _debug= true;

        if (sleepTime==-1) _sleepTime= Integer.valueOf( 
                            p.getProperty( 
                                    "loader.sleepTime", "0" )).intValue();
        else              _sleepTime= sleepTime;
    }


    private void printProperties() {
        printDebugMessage( "loader.boot.class       = " + _mainClass );
        printDebugMessage( "loader.boot.classpath   = " + _inClasspath );
        printDebugMessage( "loader.root.dir         = " + _rootDir );
        printDebugMessage( "loader.jre.dir          = " + _jreDir );
        printDebugMessage( "loader.jre.parameters (array)  = " );
        printDebugMessage( _jreParams );

        printDebugMessage( "loader.jre.pathRelative = " + _jreRelative );
        printDebugMessage( "loader.jre.jarName      = " + _jreJar );
        printDebugMessage( "loader.app.define (array) = ");
        printDebugMessage( _appDefine );
        printDebugMessage( "loader.sleepTime        = " + _sleepTime );
        printDebugMessage( "loader.logfile          = " + _logfile );
        printDebugMessage( "os.name                 = " + 
                                              System.getProperty("os.name"));
    }

    /**
     * Print a message to System.out with the class name.
     */
    void printMessage( String msg ) {
        System.out.println( "AppLoader: " + msg);
    }
    void printDebugMessage( String msg ) {
        if (_debug) printMessage(msg);
    }

    void printDebugMessage( String msg[] ) {
        if (_debug) {
           for(int i=0; (i<msg.length); i++) {
                printMessage("   ["+i+"]="+msg[i]);
           }
        }
    }


    public void defineDirs(String rootDir) {
       if (rootDir== null) {
          _rootDir= System.getProperty( "loader.root.dir", "." );
       }
       else {
          _rootDir= rootDir;
       }

       if (OSInfo.isPlatform(OSInfo.MAC)) { 
          _updatePackageDir = new File( _rootDir  + "/Resources/update" );
          _libPackageDir    = new File( _rootDir  + "/Resources/Java" );
       }
       else { // any other platform
          _updatePackageDir = new File( _rootDir + File.separator + "update" );
          _libPackageDir    = new File( _rootDir + File.separator + "lib" );
       }
    }

    /**
     * Load the application.
     * @param args This is passed to the application that is loaded.
     */
    public void loadApp( String args[],  String overrideJarFiles[] ) {
       loadApp(args, overrideJarFiles, null);
    }

    /**
     * Load the application.
     * @param args This is passed to the application that is loaded.
     */
    public void loadApp( String args[],  
                         String overrideJarFiles[],
                         File   overrideJarDir ) {

        File libDir= null;

        if (overrideJarDir == null) {
            libDir= _libPackageDir;
        }
        else {
            libDir= overrideJarDir;
        }

        List<String> cmdList= new ArrayList<String>(20);
        // loop though the list of files
        String libJarsPath = "";
        if (overrideJarFiles == null) {
            File classPathFileList[] = FileUtil.listJarFiles(libDir);
                   // If class path not found then print message and exit
            if ( classPathFileList == null ) {
                printMessage( "No application jar files found in " +
                              "lib directory, exiting..." );
                return;
            }
            for ( int i = 0; i < classPathFileList.length; i++ ) {
               libJarsPath += libDir + File.separator +
                              classPathFileList[i].getName() + 
                              File.pathSeparator;
            }
        }
        else {
           for ( int i = 0; i < overrideJarFiles.length; i++ ) {
               libJarsPath += libDir + File.separator + 
                              overrideJarFiles[i] + File.pathSeparator;
           }
        }
        printDebugMessage( "classpath = " + libJarsPath );
        String jvm;
        //String jvmDefine;
        // The full command string to the JVM including classpath, 
        // main class and arguments
        if ( OSInfo.isPlatform(OSInfo.ANY_WINDOWS) ) {
            String parens= "\"";
            jvm       = parens + _jreDir + File.separator + "bin" + 
                        File.separator + _jvmcmd + parens;

        }
        else {
            jvm       = _jreDir + File.separator + "bin" + 
                        File.separator + _jvmcmd;
        }
        String cp = _inClasspath + File.pathSeparator + libJarsPath;

        cmdList.add(jvm);
        addToList(cmdList,_jreParams);
        addToList(cmdList,"-cp");
        addToList(cmdList,cp);
        addToList(cmdList, _appDefine);
        addToList(cmdList, "-Dloader.jre.jarName=",  _jreJar );
        addToList(cmdList, "-Dloader.meta.jarName=", _metaJar);
        addToList(cmdList, "-Dloader.boot.class=",   _mainClass);
        addToList(cmdList, "-Dloader.root.dir=",     _rootDir );
        addToList(cmdList, _mainClass);
        addToList(cmdList, args);

        String cmdString[]= cmdList.toArray(new String[cmdList.size()]);
        printDebugMessage( "cmdString = ");
        printDebugMessage( cmdString );

        try {
            // Execute the command and continue, 
            // we could wait for and exit status
            // but there's really no need
            Runtime.getRuntime().exec( cmdString );
        } catch( IOException e ) {
            printMessage( "Exception during execution of process load\n" );
            e.printStackTrace( System.out );
        }
    }

    private void addToList(List<String> l, String s[]) {
        if (s != null) {
            for(int i=0; (i<s.length); addToList(l, s[i++]));
        }
    }

    private void addToList(List<String> l, String d, String v) {
        if (d!=null && v!=null) {
            addToList(l, d + v);
        }
    }
   

    private void addToList(List<String> l, String s) {
        if (s.length() == 0) return;
        String parens= "\"";
        String sAry[]= s.split(" ");
        if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
            if (sAry.length>1) {
                sAry[0]= parens + sAry[0];
                sAry[sAry.length-1]= sAry[sAry.length-1] + parens ;
            }
            for(String str : sAry) l.add(str);
        }
        else  {
            l.add(s);
        }
    }

    private void updateJars() {
        sleepMaybe();
        makeLoaderDirs();
        File updateFileList[] = FileUtil.listJarFiles(_updatePackageDir);

        boolean metaJarfound= false;
        if ( updateFileList != null  && updateFileList.length > 0) {
            boolean meta;
            for(File updateFile: updateFileList) {// loop though the files
                meta= processFile(updateFile);
                if (meta) metaJarfound= true;
            }
            if (metaJarfound) installApp();
        }
        else {
          printDebugMessage( "no class updates necessary" );
        }
        if (_restartProg != null) restart();
    }

    /**
     * Do some thing with the jar file.
     * If it is the jre the expand it in the update directory.
     * If it is a normal jar file then move it to the lib directory
     * If it has the expand attribute then move it to the lib direcory and
     * then expand/delete it.
     * Move this file over to the lib directory
     * @param updateFile the jar file to process
     * @return return true is the jar has the meta attribut / false otherwise
     */
    private boolean processFile(File updateFile) {
        JarVersion jv= checkJarInfo(updateFile);
        boolean isMeta= false;
        if (isJreJar(jv,updateFile)) {
            expandJre(updateFile);
        }
        else {
            File newFile= moveFile(updateFile);
            if (isMetaJar(jv, newFile)) {
                isMeta= true;
            }
            else if (jv.containsAttribute(JarVersion.EXPAND_ATTRIBUTE)) {
                try {
                    Installer.expandInstallationJar(jv, newFile,
                                    new File(_rootDir), _libPackageDir);
                } catch (IOException e) {
                    System.out.println("Could not expand " +
                                       updateFile+ "\n" + e);
                }
            }
        }
        return isMeta;
    }


    private File moveFile(File fileToMove) {
        boolean success;
        File targetFile = new File( _libPackageDir, fileToMove.getName() );
        printDebugMessage( "rename to save file: " + targetFile.getPath() );
        File savFile= new File( _libPackageDir, 
                           fileToMove.getName() + ".sav" );
        if (savFile.exists()) savFile.delete();
        success= targetFile.renameTo( new File( _libPackageDir,
                           fileToMove.getName() + ".sav" ) );
        if (!success) printDebugMessage( "Move/rename failed");
        printDebugMessage( "Moving: " + fileToMove.getPath() +
                           " to "     + targetFile.getPath() );
        success= fileToMove.renameTo( targetFile );
        if (!success) printDebugMessage( "Move/rename failed");

        return targetFile;
    }

    private void installApp() {
        try {
            Installer.installApp(new File(_rootDir));
        } catch (IOException e) {
            System.out.println("Could not expand and install:" + e);
        }
    }

    private boolean isMetaJar(JarVersion jv, File file) {
        boolean retval=false;
        if(file!=null) {
            retval= jv.containsAttribute(JarVersion.META_ATTRIBUTE);
            if(!retval) {
                retval=(file.getName().equals(_metaJar));
            }
        }
        return retval;
    }

    private boolean isJreJar(JarVersion jv, File file) {
        boolean retval=false;
        if(file!=null) {
            retval= jv.containsAttribute(JarVersion.JRE_ATTRIBUTE);
            if(!retval) {
                retval=(file.getName().equals(_jreJar));
            }
        }
        return retval;
    }

    private JarVersion checkJarInfo(File file) {
        String jarAttributes;
        boolean jarExpandFlag;
        JarVersion jv= null;
        File jarTargetDir;
        try {
            jv= new JarVersion(file);
            jarAttributes= jv.getAutoUpdateAttributes();
            jarExpandFlag= jv.containsAttribute(JarVersion.EXPAND_ATTRIBUTE);
            jarTargetDir= jv.getAutoUpdateTargetInstallDirFile();
            printDebugMessage( file.getName() + " : " +
                               "Attributes: " + jarAttributes +
                               ", Expand Flag: "+ jarExpandFlag+
                               ", jarTargetDir: "+
               ((jarTargetDir==null) ? "<none>" : jarTargetDir.getPath()) );
        } catch (FileNotFoundException e) {
            printMessage( "this should neve happen in checkJarInfo: " +
                          e.getMessage());
        }
        return jv;
    }

    private void sleepMaybe() {
        try {
           if (_sleepTime > 0) {
             printDebugMessage("updateJars: sleeping " +_sleepTime+ 
                               " seconds." );
             Thread.sleep(_sleepTime * 1000);  // number of seconds seconds
             printDebugMessage("updateJars: awake" );
           }
        } catch (InterruptedException e) {
            // just wake up
        }
    }

    private void makeLoaderDirs() {
        if ( !_updatePackageDir.exists()) _updatePackageDir.mkdirs();
        if ( !_libPackageDir.exists())    _libPackageDir.mkdirs();
    }

    private boolean expandJre(File newJreFile) {
        boolean success= false;
        try {
           if ( !_updatePackageDir.exists()) _updatePackageDir.mkdirs();
           JarExpander.expand(newJreFile, _updatePackageDir, false);
           newJreFile.delete();
           success= true;
        } catch (IOException e) {
           File badJre= new File(_updatePackageDir, "jre");
           if (badJre.exists())  {
              badJre.renameTo( 
                 new File(_updatePackageDir, "bad-jre"));
           }
        }
        return success;
    }

    private void restart() {
        try {
           String originalDir= System.getProperty("user.dir");
           System.setProperty("user.dir", _rootDir);
        
           if (OSInfo.isPlatform(OSInfo.ANY_UNIX)) {
              Runtime.getRuntime().exec( _restartProg);
              printDebugMessage( "restart using: " + _restartProg);
           }
           else if (OSInfo.isPlatform(OSInfo.ANY_WIN_95_98_ME)) {
              File newJre= new File( ".\\update\\jre" );
              if (newJre.exists()) {
                  new MoveJre(_rootDir, "jre", _logfile, _restartProg);
              }
              else {
                 Runtime.getRuntime().exec( _restartProg);
                 printDebugMessage( "restart using: " + _restartProg);
              }
           }
           else if (OSInfo.isPlatform(OSInfo.ANY_WIN_NT)) {
               if (OSInfo.isPlatform(OSInfo.WIN_VISTA)) {
//                   _restartProg= _rootDir +"\\"+_restartProg;
                   _restartProg= ".\\"+_restartProg;
               }
               File newJre= new File( ".\\update\\jre" );
               if (newJre.exists()) {
                   String exe= ".\\restart.exe "+ _restartProg;
                   Runtime.getRuntime().exec( exe);
               }
               else {
                   Runtime.getRuntime().exec( _restartProg);
                   printDebugMessage( "restart using: " + _restartProg);
               }
           }
           else if (OSInfo.isPlatform(OSInfo.MAC)) {  // mac
               String exe[]= new String[2];
               File f= new File(_rootDir);
               f= f.getParentFile();
               exe[0]= "/usr/bin/open";
               exe[1]= f.getPath();
               Runtime.getRuntime().exec( exe);
               printDebugMessage( "restart using: " + exe[0] +" "+exe[1]);
           }
           else {
              printDebugMessage( "should never be here");
              Assert.tst(false);
           }
           System.setProperty("user.dir", originalDir);
        } catch( IOException e ) {
            System.out.println("restart: restarting program failed");
            System.out.println("restart: " + e);
            System.out.println("restart: user.dir=" + _rootDir);
            System.out.println("restart: _restartProg=" + _restartProg);
            System.out.println("restart: Stack Trace Follows: ");
           e.printStackTrace();
        }
    }


    public static void updateJarsInSeparateProcess(String rootDir,
                                                   String jreDir,
                                                   String jreJar,
                                                   String metaJar,
                                                   int    sleepTime,
                                                   String restartProg,
                                                   String logFile) {

       List<String> defineList= new ArrayList<String>(20);
       defineList.add(
           "-Dloader.boot.class=edu.caltech.ipac.util.AppLoader");

        if (jreDir != null) {
             defineList.add("-Dloader.jre.dir=" + jreDir );
        }

        if (logFile != null) {
             defineList.add("-Dloader.logfile="+ logFile );
        }

        if (restartProg != null) {
             defineList.add("-Dloader.restart.prog="+ restartProg );
        }

        if (sleepTime > 0) {
             defineList.add("-Dloader.sleepTime="+sleepTime);
        }

        String s= System.getProperty("installer.mac.app",null);
        if (s!=null) defineList.add("-Dinstaller.mac.app="+s);


        defineList.add("-Dloader.debug=true");

        String defines[]= defineList.toArray(new String[defineList.size()]);

        AppLoader loader= new AppLoader(
                              rootDir,  
                              "edu.caltech.ipac.util.AppLoader",
                              jreDir,
                              null,
                              jreJar, 
                              metaJar, 
                              defines,
                              false,
                              true,
                              0,
                              null);

         String params[]= {"-AppLoader:updateJarsOnly"};
         String jarsStr[]= {"loader.jar"};
         loader.loadApp(params, jarsStr);
    }

    /**
     * Program entry point.
     * @param args main type args
     */
    public static void main( String[] args ) {
        boolean onlyUpdateJars= false;
        for(String arg : args) {
             if (arg.equals("-AppLoader:updateJarsOnly")) {
                 onlyUpdateJars= true;
                 break;
             }
        }
        AppLoader appLoader= new AppLoader();
        try {
            if (onlyUpdateJars) {
               appLoader.updateJars();
            }
            else {
               appLoader.updateJars();
               appLoader.loadApp( args, null );
            }
        }
        catch( Exception e ) {
            e.printStackTrace( System.out );
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
