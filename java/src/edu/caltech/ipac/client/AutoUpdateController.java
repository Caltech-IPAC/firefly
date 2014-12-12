package edu.caltech.ipac.client;

import static edu.caltech.ipac.client.AutoUpdateEvent.Status.*;
import static edu.caltech.ipac.client.AutoUpdateUI.UpdateAction.DONT_UPDATE;
import static edu.caltech.ipac.client.AutoUpdateUI.UpdateAction.UPDATE_NOW;
import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.util.AppLoader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.Installer;
import edu.caltech.ipac.util.JarVersion;
import edu.caltech.ipac.util.OSInfo;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.action.GeneralAction;
import edu.caltech.ipac.util.action.Prop;
import edu.caltech.ipac.util.software.PackageUpdate;
import edu.caltech.ipac.util.software.PackageUpdateException;
import edu.caltech.ipac.util.software.PackageUpdateServices;
import edu.caltech.ipac.util.software.SoftwarePackage;
import static edu.caltech.ipac.util.software.SoftwarePackage.UPDATE_STATUS_PACKAGE_UPDATED;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The class the updates the application in the background. This class is very
 * complex.  It controls the whole process of an auto-update. It run primarily as
 * a background thread.
 *
 * @see edu.caltech.ipac.client.AutoUpdateUI
 * @see edu.caltech.ipac.client.AutoUpdateAction
 * @see edu.caltech.ipac.client.LocalPackageDesc
 * @see edu.caltech.ipac.util.software.SoftwarePackage
 * @see edu.caltech.ipac.util.software.PackageUpdate
 *
 * @author Trey Roby
 * @version $Id: AutoUpdateController.java,v 1.34 2008/04/08 20:13:55 roby Exp $
 *
 */
public class AutoUpdateController implements Runnable, ClientEventListener {
//
    private enum AccessType { YES, NO, VistaNo }
    private static final String LOADER_JAR="loader.jar";
    private static final String SERVLET_PROP="AutoUpdateAction.servlet";
    private static final String LOADER_UPDATE_PROP=
                                  "AutoUpdateAction.loaderNeedsUpdating."+ActionConst.SELECTED;
    private static final String MAC_ADDAPPS_PROP=".mac.additionalApps.";
    private static final String THIS_PF_JAR_NAME= ".thisPropertyFile.jarName";
    private static final String THIS_PF_JAR_PATH= ".thisPropertyFile.jarPath";

    private static final String JRE_JAR_PROP= "loader.jre.jarName";

    private static final int PAD_SIZE=40;
    private static final String DOT=".";

    private static final int PASS_ONE= 0;
    private static final int PASS_TWO= 1;
    private static final int PASS_LENGTH= 2;

    private Thread  _thread=null;
    private boolean _recoveryUpdate=false;
    private boolean _updatingJRE=false;
    private boolean _doUpdatesOnQuit=false;
    private boolean _doRestart=false;
    private boolean _doSleep=true;

    private final List<SoftwarePackage>    _serverPackageList=
                                         new LinkedList<SoftwarePackage>();
    private final List<AutoUpdateListener>    _listeners=
                                 new ArrayList<AutoUpdateListener>(2);

    private List<LocalPackageDesc> _managedJars;
    private AutoUpdateUI  _updateUI;
    private PackageUpdate _pkUp;
    private final String        _baseProp;
    private final AutoUpdateAction _action=new AutoUpdateAction(this);
    private final String        _workerString;
    private String        _propertyDefinitionJar= null;
    private String        _propertyDefinitionJarPath= null;


    /**
     * these strings are based are indexed by status numbers in SoftwarePackage
     */
    private static final String UPDATE_STATUS_STR_ARY[]= {
                           "Update status not checked",
                           "Update is available",
                           "NEW VERSION! Downloaded to client.",
                           "No update found on server",
                           "Error on update",
                           "Older or same version on Server",
                   };


    public AutoUpdateController(JFrame       f,
                                String       appName,
                                String       propBaseName) {
        this(f,appName,propBaseName,null,null);
    }


    public AutoUpdateController(JFrame      f,
                                String      appName,
                                String      propBaseName,
                                Object      quitAction,    // only a place holder, not used
                                String      workerString) {
        LocalPackageDesc propPackages[]= makePackageFromProp(propBaseName, null);
        _baseProp= propBaseName;
        _workerString= workerString;
//        _quitAction=quitAction;
        init(f,appName,propPackages);

    }

     public AutoUpdateController(JFrame            f,
                                 String            appName,
                                 LocalPackageDesc  localPackages[]) {
         _baseProp= null;
         _workerString= null;
//         _quitAction=quitAction;
         init(f,appName,localPackages);
     }



    public GeneralAction getAction() { return _action; }

    /**
     * Is the auto update in progress
     *
     * @return true if we in any of the steps of evaluating, downloading,
     *         or talking to the user about an auto-update, false if that task is
     *         completed
     */
    public boolean isActive() { return (_thread!=null); }

//======================================================================
//------------------- Method from Runnable interface  ------------------
//======================================================================
    

   public void run() {
      try {
         if (_doSleep) {
              fireListeners(SLEEPING);
              ClientLog.message("sleeping...");
                // if we just updated don't sleep long get on with the
                // final phase, if this is a normal run then we are not in
                // as big of hurry
              boolean doLoaderUpdate= AppProperties.getBooleanPreference(
                            LOADER_UPDATE_PROP,true);
              int sleepSecs= doLoaderUpdate ? 1 : 4;
              TimeUnit.SECONDS.sleep(sleepSecs);
              fireListeners(AWAKE);
         }
         awakeReport();
      } catch (InterruptedException e) {
          ClientLog.warning("Should never have a InterruptedException here");
          e.printStackTrace();
      }
      AccessType accessType= hasAccess();

      if (accessType != AccessType.NO) {

          String s= "";

          if (accessType==AccessType.YES) doPostUpdate();

          if (accessType==AccessType.VistaNo) {
              ClientLog.message("On Vista with no access. ",
                                "Checking then will tell the user what is going on.");
          }

         if (_action.getSelected())  {
             update(accessType);
         }
         else  {
             s= " No update was attempted- user turned option off.";
         }

         ClientLog.message("Completed." + s);
      }
      else {
         _updateUI.warnUserOfNoAccess();
         _action.setSelected(false);
      }
      _thread= null;
   }

//=======================================================================
//-------------- Public Listener add/remove Methods ---------------------
//=======================================================================
    /**
     * Add a listener that is called when there updates to the
     * AutoUpdateController
     *
     * @param l the listener
     */
    public synchronized void addAutoUpdateListener(AutoUpdateListener l) {
        _listeners.add(l);
    }

    /**
     * Remove a listener that is called when there updates to the
     * AutoUpdateController
     *
     * @param l the listener
     */
    public synchronized void removeAutoUpdateListener(AutoUpdateListener l) {
        _listeners.remove(l);
    }


//=======================================================================
//-------------- Method from QuitListener Interface ----------------------
//=======================================================================

   // public void quit(QuitEvent ev) {


    public void eventNotify(ClientEvent ev) {
        Assert.tst(ev.getName()==ClientEvent.QUITTING);
        if(_doUpdatesOnQuit) {
            startJarMovingProcess();
        }
        else {
            // if we started to download an update but did not ever ask the
            // the user then we need to clean up
            File fAry[]= FileUtil.listJarFiles(
                                          Installer.getAutoUpdateUpdateDirFile());
            if (fAry!=null) {
                for(File f: fAry) f.deleteOnExit();
            }
        }
    }

//======================================================================
//----------------------- Package Methods ------------------------------
//======================================================================

    void startThread(boolean doSleep) {
        _doSleep=doSleep;
        if(_thread==null) {
            _thread=new Thread(this, "AutoUpdate Thread");
            _thread.start();
        }
    }

//======================================================================
//----------------------- Private / Protected Methods ------------------
//======================================================================
 
   private void init(JFrame            f, 
                     String            appName,
                     LocalPackageDesc  localPackages[]) {
       _managedJars= makeCompletePackageListFromClassPath(localPackages);
       _updateUI=new AutoUpdateUI(f, appName);
//       _quitAction.addQuitListener(this);
       ClientEventManager.addClientEventListener(this, ClientEvent.QUITTING);
       _propertyDefinitionJar=AppProperties.getProperty(
                                      _baseProp+THIS_PF_JAR_NAME, null);
       _propertyDefinitionJarPath=AppProperties.getProperty(
                                      _baseProp+THIS_PF_JAR_PATH, null);
   }


    /**
     * When we wake up form sleeping report all the jars we are managing.
     * This will be a very long log file entry
     */
   private void awakeReport() {
      String       desc;
      List<String> out= new ArrayList<String>(_managedJars.size()+2);
      out.add("Awake: Managing the following packages:");
      out.add("======================================");
      for(LocalPackageDesc  localPackage : _managedJars) {
         desc= localPackage.getUserDescription();
         if (desc==null) desc= "";
         out.add(StringUtil.pad(localPackage.getJarName(), PAD_SIZE) + desc);
      }
      ClientLog.message(out);
   }


    /**
     * This method does work on the run after an update
     */
    private synchronized void doPostUpdate() {
        // doPostUpdate expands any jars that need expanding and check
        // to see if we are in a post update mode
        // We know when we have just done an AutoUpdate because the
        // LOADER_UPDATE_PROP is true
        // when true: we are in post update processing mode and
        // two things can happen:
        //  1.  we need to update loader.jar, we do this be pull loader.jar
        //      out of what ever jar file is was packaged in
        //  2.  if we are on a mac we need to update any other applications
        //      that we are resposible for

        expandJars(); // check this everytime

        boolean doingPostUpdate = AppProperties.getBooleanPreference(
                       LOADER_UPDATE_PROP,true);

        File loaderJar=new File(
                       Installer.getInstallationLibDirFile(), LOADER_JAR);

        ClientLog.brief("Checking for PostUpdate: " + doingPostUpdate);
        if (doingPostUpdate || !loaderJar.exists()) {
            updateLoaderJar();
            AppProperties.setPreference(LOADER_UPDATE_PROP, false+"");
        }

        if (doingPostUpdate)  macAdditionalAppUpdate();
    }

    /**
     * go through all the jar file in the jars installation directory
     * ("Java" on mac or "lib" on all others).  If a jar is not in the
     * classpath then check its JarVersion information to see if the
     * expand flag is set.  If so then expand the jar.
     */
    private void expandJars() {
        File libDir= Installer.getInstallationLibDirFile();
        File jarFiles[]= FileUtil.listJarFiles(libDir);
        File cpAry[]= Installer.getTrueClassPathAsFileAry();
        List<File> jarNotInCP= new ArrayList<File>(5);
        boolean found;
        if (jarFiles!=null) {
            for(File f: jarFiles) {
                found= false;
                for(File cpFile: cpAry) {
                    if (f.equals(cpFile)) {
                        found= true;
                        break;
                    }
                }
                if (!found) jarNotInCP.add(f);
            }
        }
        JarVersion jv= null;
        for(File f: jarNotInCP) {
            AutoUpdateUI.ExpandFeedBack expandFeedback= null;
            try {
                jv= new JarVersion(f);
                if (jv.containsAttribute(JarVersion.EXPAND_ATTRIBUTE)) {
                    File expandDir= jv.getAutoUpdateTargetInstallDirFile();
                    if (expandDir==null) expandDir= libDir;
                    ClientLog.message(true,"Expanding jar: " + f.getName(),
                                      "To: " + expandDir.getPath(),
                                      jv.toString());
                    expandFeedback=  _updateUI.startExpandingMessage();
                    File rootDir= Installer.deriveInstallationRootFile();
                    Installer.expandInstallationJar(jv,f, rootDir,libDir,
                                       expandFeedback.getExpandingStatus());
                    ClientLog.message(true,"Expandsion successful");
                    fireListeners(EXPANDED_JAR,rootDir);
                }
            } catch (FileNotFoundException ignore) {
                // if it is not found don't do any thing
            } catch (IOException e) {
                String v= (jv!=null) ? jv.toString() : "";
                ClientLog.warning("Expanding jar failed: " + f.getName(), v);
            } finally {
                if (expandFeedback!=null) expandFeedback.endExpandingMessage();
            }
        }
    }


   /**
    * If we did an auto-update on the last run then we need to pull loader.jar
    * out and put it in the lib directory.  loader.jar is the jar file
    * that is used when we do an auto-update and have to move jars in a separate
    * process.
    */
   private synchronized void updateLoaderJar() {
       URL url=ClassLoader.getSystemResource(LOADER_JAR);
       DataInputStream in;
       BufferedOutputStream out;
       File f=new File(Installer.getInstallationLibDirFile(), LOADER_JAR);
       if (f.exists()) {
           File moveFile= new File(Installer.getInstallationLibDirFile(),
                                   LOADER_JAR + ".sav");
           f.renameTo(moveFile);
       }
       try {
           URLConnection conn=url.openConnection();
           in=new DataInputStream(new BufferedInputStream(conn.getInputStream()));
           out=new BufferedOutputStream(new FileOutputStream(f), 4096);
           try {
               while(true) out.write(in.readByte());
           } catch (EOFException e) {
               FileUtil.silentClose(in);
               FileUtil.silentClose(out);
           }
           ClientLog.message("updated "+LOADER_JAR);
       } catch (IOException ioe) {
           ClientLog.warning(true, ioe.toString(),
                             "trying to extract "+ LOADER_JAR +" out of "+
                             url.toString());
       }
   }

    /**
     *
     */
    private void macAdditionalAppUpdate() {
        if(_baseProp==null) return;
        if(!OSInfo.isPlatform(OSInfo.MAC)) return;
        String otherApps[];
        String value=AppProperties.getProperty(_baseProp+MAC_ADDAPPS_PROP+
                                               Action.NAME);
        if(value==null) return;

        otherApps=StringUtil.strToStrings(value);
        File fromDir=Installer.getInstallationLibDirFile();
        File toDir;
        File toJar;
        File fromJar;
        File toRoot;
        String jarName;
        File f=Installer.deriveInstallationRootFile();
        File appDirectory=f.getParentFile().getParentFile();
        for(String otherApp : otherApps) {
            ClientLog.message("Updating: "+otherApp);
            toRoot=new File(appDirectory, otherApp+".app/Contents");
            toDir=new File(toRoot,  "Resources/Java");
            value=AppProperties.getProperty(_baseProp+MAC_ADDAPPS_PROP+
                                            otherApp+".jars."+Action.NAME);
            if(value!=null) {
                String tokens[]=StringUtil.strToStrings(value);
                Iterator j=_managedJars.iterator();
                LocalPackageDesc localPackage;
                while(j.hasNext()) {
                    localPackage=(LocalPackageDesc) j.next();
                    jarName=localPackage.getJarName();
                    if(inList(tokens, jarName)) {
                        toJar=new File(toDir, jarName);
                        fromJar=new File(fromDir, jarName);
                        copyFileOnVersion(fromJar, toJar);
                    } // end if
                } // end while
            } // end if value
            copyFileOnVersion(new File(fromDir, LOADER_JAR),
                              new File(toDir, LOADER_JAR));
            try {
                copyFileOnVersion(new File(fromDir, Installer.DEFAULT_META_JAR),
                                  new File(toDir, Installer.DEFAULT_META_JAR));
                Installer.installOnAlternateMacLocation(toRoot,otherApp);
            } catch (Exception e) {
                ClientLog.warning(Installer.DEFAULT_META_JAR+
                                  " installation Failed: ", e.toString());
                e.printStackTrace();
            }
        } // end for
    }

    private void copyFileOnVersion(File from, File to) {
        JarVersion fromVer= null;
        JarVersion toVer;
        boolean proceed;
        try {
            fromVer= new JarVersion(from);
            try {
                toVer= new JarVersion(to);
                proceed= fromVer.isNewerVersion(toVer);
            } catch (FileNotFoundException e) {
                proceed= true;
            }
        } catch (FileNotFoundException e) {
            proceed= false;
        }

        if (proceed) {
            copyFile(from,to);
            if (fromVer.containsAttribute(JarVersion.EXPAND_ATTRIBUTE)) {
                try {
                    File rootDir= OSInfo.isPlatform(OSInfo.MAC) ?
                                      to.getParentFile().getParentFile() :
                                      to.getParentFile();
                    Installer.expandInstallationJar(fromVer,from,rootDir,to);
                    ClientLog.brief("Expanded: "+to.getPath()+
                                    " => "+rootDir);
                } catch (IOException e) {
                    ClientLog.warning("Could not expand: "+
                                      to.getParentFile(),
                                      e.getMessage());

                }
            }
        }
    }



    private void copyFile(File from, File to) {
        try {
            FileUtil.copyFile(from, to);
            ClientLog.brief("Copied: "+from.getPath()+
                            " => "+to.getPath());
        } catch (IOException e) {
            ClientLog.warning("Copy Failed: ",
                              from.getPath()+" => "+
                              to.getPath(), e.toString());
        } // end catch
    }

    private boolean inList(String tokens[], String jarName) {
        boolean retval= false;

        if (jarName.equals(LOADER_JAR) ||
            jarName.equals(Installer.DEFAULT_META_JAR)) {
            retval= false;
        }
        else if (tokens[0].equals("*")) {
            retval= true;
        }
        else {
            for(int i=0; (i<tokens.length && !retval); i++) {
                if(jarName.equals(tokens[i]+".jar")) {
                    retval=true;
                }
            }
        }
        return retval;
    }


    /**
     * Start the process of looking for an update.  An update can come from
     * two places.  It can come over the network (the normal case) or it
     * may already be in the update dir (the failed on last run case).
     * @param accessType which accessType
     */
   private synchronized void update(AccessType accessType) {
         fireListeners(CHECKING);
         HostPort hp= NetworkManager.getInstance().getServer(
                                            NetworkManager.AUTO_UPDATE_SERVER);

         String server = "http://" + hp.getHost() +":"+ hp.getPort();
         if(_workerString!=null) server+=_workerString;

         _pkUp= makePackageUpdate(server);

         File jarFiles[]= FileUtil.listJarFiles(
                              Installer.getAutoUpdateUpdateDirFile());


              // this following if determines if the last run of the program 
              // attempted to do an auto-update and was unsuccessful.
              // if it was unsuccessful then try to pick up where we left off.
              // otherwise the normal case it to query the server to
              // see if it has a new version

         if ( jarFiles != null  && jarFiles.length > 0) {
             _recoveryUpdate= true;  
             fireListeners(FOUND_UPDATES);
             SwingUtilities.invokeLater( new UserDecision() );
         }
         else {  // normal case
             _recoveryUpdate= false;  
             queryServerForUpdate(accessType);
         }

         if (getUpdatedCount() == 0 && !_recoveryUpdate) {
                fireListeners(DONE_NO_UPDATES);
         }
   }

   /**
    * Query the server over the network to see if any new versions of jar
    * files are available
    * @param accessType which accessType
    */
   private synchronized void queryServerForUpdate(AccessType accessType) {
      try {
         ClientLog.message("Starting", "Service Available: "+
                                       _pkUp.isUpdateServiceAvailable() );
         if (_pkUp.isUpdateServiceAvailable()) {
	    Enumeration     serverPackages= _pkUp.getPackageList();
            _serverPackageList.clear();
            while(serverPackages.hasMoreElements()) {
                _serverPackageList.add((SoftwarePackage)serverPackages.nextElement());
            }
            checkAllPackages();
            if (getUpdatedCount() > 0) {
                fireListeners(FOUND_UPDATES);
                tellUser(accessType);
            }
         } // end if
      } catch (PackageUpdateException pue) {
         ClientLog.warning(true, pue.toString());
      }
   }

    /**
     * This is a work around in 1.5 to create a typesafe array
     * The compiler does not allow you to create a typesafe array using
     * generics.  I am sure this will be fixed in 1.6 and this method should go
     * away
     */
//   private static <T> T[] createArray(T... items) { return items; }


    /**
     * This is the key method to check the version of each jar and download new
     * ones if necessary.  This method requires three passes to do a complete job.
     * <ol>
     * <li> Check the versions of each jar and download new version if necessary
     * <li> The newly downloaded jars may specify other new jars in their manifest.
     *      If any new jars are specified then download these
     * <li> Check there version of the JRE and download a new one if necessary
     * </ol>
     */
   private void checkAllPackages() {
              // ----------------------------------------
              // 1. Go throught our list of jar files
              //    this happens in two phases, the original list
              //    and then the list that comes from the updated jar
              //    The list for the second pass is built after the first
              //    pass jars are downloaded
              // ----------------------------------------



                               // java 1.5 does does not allows for arrays
                               // of generic list, so suppress the warning
       @SuppressWarnings(value = {"unchecked"})
       List<LocalPackageDesc> packageListArray[]= new List[PASS_LENGTH];
                              // keep above two lines together

       packageListArray[PASS_ONE]=_managedJars;
       packageListArray[PASS_TWO]= null;

      for(int j=PASS_ONE; j<PASS_LENGTH; j++) { // loop 2 times
          if (j==PASS_TWO)  prepareForPass2(packageListArray);
          performPackagePass(packageListArray[j]);
      }
              // ----------------------------------------
              // 2. now do the jre pass
              // ----------------------------------------
      performJrePass();
   }

    private void performJrePass() {
        try {
            String  jreJarStr= System.getProperty( JRE_JAR_PROP);
            ClientLog.message(JRE_JAR_PROP+": " + jreJarStr);
            if (jreJarStr != null) {
                File updateFile= new File(jreJarStr);
                SoftwarePackage pkg=  makeJRESoftwarePackage(updateFile);
                if (pkg!=null) {
                    _pkUp.updatePackage(pkg);  // do  the download, if a new version
                    if (pkg.getUpdateStatus() == UPDATE_STATUS_PACKAGE_UPDATED) {
                        _updatingJRE= true;
                    }
                    SoftwarePackage serverPkg=findServerPackageForJreJar(
                                                  _serverPackageList);
                    String pStr= pkg.getMajorVersion()+DOT+
                                 pkg.getMinorVersion()+DOT+
                                 pkg.getRevsion();
                    logUpdateStatus(
                       pStr +" for jre: " + System.getProperty("java.version"),
                       updateFile.getName(), serverPkg ,pkg, null,
                           "Note:              " +
                           "Local version determined by java.version property");
                }
            }
        } catch (PackageUpdateException pue) {
            ClientLog.warning(true, pue.toString());
        }
    }

    private void performPackagePass(List<LocalPackageDesc> packageList) {

        JarVersion jv;
        SoftwarePackage pkg;
        File updateFile;
        for(LocalPackageDesc localPackage : packageList) {
            try {
                jv=localPackage.getJarVersion();
                updateFile=new File(localPackage.getJarName());
                pkg=makeSoftwarePackage(jv, updateFile);
                _pkUp.updatePackage(pkg);  // do  the download, if a new version
                checkForUpdate(pkg, localPackage, updateFile);
                logUpdateStatus(localPackage, pkg);
            } catch (PackageUpdateException pue) {
                ClientLog.warning(true, pue.toString());
            }
        } // end loop
    }

    private void prepareForPass2(List<LocalPackageDesc> packageListArray[]) {
        List<LocalPackageDesc> tmpList;
        List<LocalPackageDesc> finalList= new ArrayList<LocalPackageDesc>(70);

        for(LocalPackageDesc localPackage : packageListArray[PASS_ONE]) {
            tmpList= checkForAdditionalPackages( localPackage);
            if (tmpList!=null) {
                finalList.addAll(tmpList);
                break;
            }
        }
        finalList= addAdditionalPackagesFromJars( finalList);
        finalList= purgeDuplicates(finalList);
        packageListArray[PASS_TWO]= finalList;
    }


   private List<LocalPackageDesc> checkForAdditionalPackages(
                                           LocalPackageDesc localPackage) {

       List<LocalPackageDesc>  retval= null;
       if (localPackage!=null          &&
           localPackage.isDownloaded() &&
           localPackage.getJarName().equals(_propertyDefinitionJar) &&
           _propertyDefinitionJarPath != null ) {
               retval= createAdditionalPackageListByProp();
       }
       return retval;
   }

    private void checkForUpdate( SoftwarePackage  pkg,
                                 LocalPackageDesc localPackage,
                                 File             updateFile) {
        if(pkg.getUpdateStatus()==UPDATE_STATUS_PACKAGE_UPDATED){
            SoftwarePackage serverPkg=findServerPackageForJar(
                                          _serverPackageList, updateFile.getName());
            localPackage.setServerPackage(serverPkg);
            localPackage.setDownloaded(true);
        } // end if
    }

    private LocalPackageDesc[] makePackageFromProp(String     pName,
                                                   Properties pdb) {
        LocalPackageDesc lpd[]=null;
        String searchProp= pName+"."+Action.NAME;
        String plateformValue= Prop.getPlatformProp(pName,Action.NAME,
                                                    null,false,null);
        String value=AppProperties.getProperty(searchProp, "", pdb);
        if (plateformValue!=null && plateformValue.length()>0) {
            value+= " " + plateformValue;
            value= value.trim();
        }
        if(value.length()>0) {
            String tokens[]=StringUtil.strToStrings(value);
            lpd=new LocalPackageDesc[tokens.length];
            boolean critical;
            String base;
            String fname;
            String desc;
            String src= "Properties file (" + searchProp  +" or " +
                        Prop.documentPlatformProp(pName,Action.NAME)+ ")";
            for(int i=0; (i<lpd.length); i++) {
                fname=tokens[i]+".jar";
                base=pName+"."+tokens[i];
                critical=Prop.getSelected(base+".critical");
                desc=AppProperties.getProperty(base+"."+Action.NAME, fname, pdb);
                lpd[i]=new LocalPackageDesc(fname, critical, desc, src);
            }
        }
        return lpd;
    }

    private List<LocalPackageDesc> addAdditionalPackagesFromJars(
                                 List<LocalPackageDesc> packagesList) {

        LocalPackageDesc localPackages[]=  packagesList.toArray(
                                 new LocalPackageDesc[packagesList.size()]);
        File updateDir= Installer.getAutoUpdateUpdateDirFile();
        return makeCompletePackageListFromDir( localPackages, updateDir);
    }

    private List<LocalPackageDesc> createAdditionalPackageListByProp() {
        List<LocalPackageDesc> additionalPackages= null;
        File updateDir= Installer.getAutoUpdateUpdateDirFile();
        File jarPath= new File(updateDir,_propertyDefinitionJar);
        JarFile jar= null;
        try {
            jar= new JarFile(jarPath);
            JarEntry entry= jar.getJarEntry(_propertyDefinitionJarPath);
            if (entry!=null) {
                InputStream in   = jar.getInputStream(entry);
                Properties pdb = new Properties();
                pdb.load( new BufferedInputStream(in) );
                LocalPackageDesc lpd[]= makePackageFromProp(_baseProp,pdb);
                additionalPackages= purgeDuplicates(lpd);
            }
        } catch (IOException e) {
            additionalPackages= null;
        }
        finally {
            FileUtil.silentClose(jar);
        }
        return additionalPackages;
    }

    private List<LocalPackageDesc> purgeDuplicates(
                                  List<LocalPackageDesc> newPackages) {
        return purgeDuplicates(newPackages.toArray(
                                  new LocalPackageDesc[newPackages.size()]));
    }

    /**
     * check the pasted list against the master list and return a new list
     * of packages that are not in the master list.
     * 
     * @param newPackages the list to search
     * @return a new list of packages that are not duplicated in the master list
     */
    private List<LocalPackageDesc> purgeDuplicates(LocalPackageDesc newPackages[]) {
        List<LocalPackageDesc>  purgedList= new ArrayList<LocalPackageDesc>(3);
        boolean found;
        if (newPackages!=null) {
            for(LocalPackageDesc newPackage : newPackages) {
                found= false;
                for(LocalPackageDesc  localPackage : _managedJars) {
                    if (newPackage.getJarName().equals( localPackage.getJarName())) {
                        found= true;
                        break;
                    }
                }
                if (!found) purgedList.add(newPackage);
            }
        }
        return purgedList;
    }

    private static List<LocalPackageDesc>  makeCompletePackageListFromClassPath(
                                  LocalPackageDesc localPackages[]) {
        String newClassPath= Installer.getTrueClassPath();
        return makeCompletePackageList(localPackages, newClassPath);
    }


    private static List<LocalPackageDesc>  makeCompletePackageListFromDir(
                                  LocalPackageDesc localPackages[],
                                  File             dir) {
        String newClassPath= Installer.makeClassPathFromAllJarsInDir(dir);
        return makeCompletePackageList(localPackages, newClassPath);
    }


   /**
    * Combine all the jar files that the user passed in with the jar files
    * in the classpath. After that look at each jar file for any JarVersion
    * manifest entry of extra jars and add this to the new combined/unique list.
    * @param localPackages jars that are already known
    * @param newClassPath a classpath string each file divided by "path.separator"
    * @return a list of LocalPackageDesc
    */
   private static List<LocalPackageDesc>  makeCompletePackageList(
                                           LocalPackageDesc localPackages[],
                                           String           newClassPath) {

       List<LocalPackageDesc> retDesc  = new ArrayList<LocalPackageDesc>(20);
       File              jarFile;
       boolean           inList;
       String            sep      = System.getProperty("path.separator");
       StringTokenizer   st       = new StringTokenizer(newClassPath, sep);

       // initialize the return value
       if (localPackages != null) {
           for(LocalPackageDesc localPackage : localPackages) {
               retDesc.add(localPackage);
           }
       }

       // add all the jars that are unique in the class path
       while(st.hasMoreTokens()) {  // loop thru classpath
           jarFile= new File(st.nextToken());
           if (!jarFile.isDirectory()) {
               inList= isInList(jarFile.getName(), retDesc);
               if (!inList) {
                   retDesc.add( new LocalPackageDesc(jarFile, "Classpath") );
               }
           }
       }


       // search all the jars to the extra jar attribute add those jars
       List<LocalPackageDesc> extraPackages= new ArrayList<LocalPackageDesc>(5);
       JarVersion jv;
       String srcStr;
       for(LocalPackageDesc p : retDesc) {
           jv= p.getJarVersion();
           if (jv!=null && jv.getHasExtraJars()) {
               String jAry[]= jv.getExtraJars();
               for(String j : jAry) {
                   if (!isInList(j,retDesc)) {
                       srcStr= "Extra Jars (defined by "+p.getJarName() + ")" ;
                       extraPackages.add(new LocalPackageDesc(j,srcStr));
                   }
               }
           }
       }

       retDesc.addAll(extraPackages);



       return retDesc;
   }

    /**
     * determine if the passed file name string is in the list of LocalPackageDesc
     * @param fStr a string of a file name (no path information)
     * @param  checkList this list of LocalPackageDesc to check against
     * @return true if it is in the list, false if it is not
     */
   private static boolean isInList(String fStr, List<LocalPackageDesc> checkList) {
       boolean retval= false;
       for(LocalPackageDesc p :  checkList) {
           if (fStr.equals(p.getJarName())) {
               retval= true;
               break;
           }
       }
       return retval;
   }

    /**
     * Tell user that the update is ready
     * @param accessType which accessTypey
     */
   private void tellUser(AccessType accessType){
        if (accessType==AccessType.YES) {
            if (isCriticalUpdate() ) {
                SwingUtilities.invokeLater(new UserDecision() );
            }
            else {
                SwingUtilities.invokeLater(new UserInfo() );
                _doRestart      = false;
                _doUpdatesOnQuit= true;
            }
        } else if (accessType==AccessType.VistaNo) {
            _updateUI.tellUserVistaAccessProblem();
            abortUpdate();
        }
        else {
            Assert.tst("No other AccessType should by here");
        }
   }




   private void logUpdateStatus(LocalPackageDesc desc, SoftwarePackage pkg) {
       JarVersion jv= desc.getJarVersion();
       String identDesc= null;
       if (jv==null) {
           identDesc=  "Note:              " +
                       "This jar does not yet exist on client";
       }
       else if (!jv.isVersionFromManifest()) {
           identDesc=  "Note:              " +
                       "Version determined by marker property file: " +
                       desc.getJarName() + ".prop";
       }
       logUpdateStatus(makeVersionStr(jv), desc.getJarName(),
                         desc.getServerPackage(), pkg, desc.getSource(),
                         identDesc);
   }

   private void logUpdateStatus(String          localVerStr,
                                String          jarName,
                                SoftwarePackage serverPkg,
                                SoftwarePackage localPkg,
                                String          source,
                                String          identDesc) {
                                
      String strStat= "unknown reason";
      String oldVer= "Local version:     " + localVerStr;
      String newVer= null;
      int updateStat= localPkg.getUpdateStatus();
      if (updateStat < UPDATE_STATUS_STR_ARY.length) {
           strStat= UPDATE_STATUS_STR_ARY[updateStat];
      }

      if (updateStat==UPDATE_STATUS_PACKAGE_UPDATED) {
         newVer= "Available version: " + makeVersionStr(serverPkg); 
      }
      if (source!=null) {
          source= "Source:            " + source;
      }
       ClientLog.message(
             "Jar:               " + jarName,
             "Update status:     "+ strStat + "  (" +updateStat + ")",
             oldVer, newVer,
             identDesc, source);
   }

    /**
     * make a displayable string for the version of this jar file
     * @param jv the JarVersion object to make the string from, if null
     *           then return string "new"
     * @return a user readable string
     */
   private String makeVersionStr(JarVersion jv) {
      String retval;
      if (jv==null) {
         retval= "new";
      }
      else {
         retval=  jv.getMajorVersion() + "." +
                  jv.getMinorVersion() + "." +
                  jv.getRevision()     + " " +
                  jv.getType();
      }
      return retval;
   }

    /**
     * make a displayable string for the version the server's software package
     * object
     * @param pkg the server SoftwarePackage object to make the string from
     * @return a user readable string
     */
   private String makeVersionStr(SoftwarePackage pkg) {
      return pkg.getMajorVersion() + "." +
             pkg.getMinorVersion() + "." +
             pkg.getRevsion();
   }

   private SoftwarePackage makeSoftwarePackage(JarVersion jv, File updateFile) {
      SoftwarePackage sp;
      if (jv==null) {
          sp = new SoftwarePackage( 0, 0, 0, "new", updateFile);
      }
      else {
          sp= new SoftwarePackage(jv.getMajorVersion(), jv.getMinorVersion(),
                                  jv.getRevision(),     jv.getType(),
                                  updateFile );
      }
      return sp;
   }

    private static SoftwarePackage makeJRESoftwarePackage(File updateFile) {
        String jreV=System.getProperty("java.version");
        SoftwarePackage retval=null;
        String verAry[]=jreV.split("\\.");
        if(verAry.length>=3) {
            try {
                String revAry[]=verAry[2].split("_");
                int minor;
                int revision;
                int major=Integer.parseInt(verAry[1]);
                if(revAry.length>=2) {
                    minor=Integer.parseInt(revAry[0]);
                    try {
                        revision=Integer.parseInt(revAry[1]);
                    } catch (NumberFormatException e) {
                        revision=0;
                    }
                }
                else {
                    minor=Integer.parseInt(verAry[2]);
                    revision=0;
                }

                retval=new SoftwarePackage(major, minor, revision,
                                           JarVersion.FINAL_TYPE, updateFile);
            } catch (NumberFormatException e) {
                ClientLog.warning("Could not parse jre version String: "+jreV);
            }
        } // end if
        return retval;

    }

    protected void fireListeners(AutoUpdateEvent.Status stat) {
        fireListeners(stat,null);
    }


  protected void fireListeners(AutoUpdateEvent.Status stat, File dir) {
     List<AutoUpdateListener> newlist;
     AutoUpdateEvent ev= new AutoUpdateEvent(this,stat,dir);
     synchronized (this) {
         newlist = new ArrayList<AutoUpdateListener>(_listeners);
     }
        
     for(AutoUpdateListener listener: newlist) {
         listener.update(ev);
     }
  }

    /**
     * User has access to the installation directory to do an auto-update
     * @return true if I have access to the directory otherwise false
     */

    private AccessType hasAccess() {
        boolean canAccess     = false;
        File    rootDir    = Installer.deriveInstallationRootFile();
        File    libDir     = Installer.getInstallationLibDirFile();
        File    updateDir  = Installer.getAutoUpdateUpdateDirFile();
        File    downloadDir= new File(Installer.getAutoUpdateDownloadDir());
        if (rootDir.canWrite()) {
            canAccess= true;
            if (libDir.exists()) {
                canAccess= (libDir.canWrite());
            }
            if (updateDir.exists()) {
                canAccess= (canAccess && updateDir.canWrite());
            }
            if (downloadDir.exists()) {
                canAccess= (canAccess && downloadDir.canWrite());
            }
        }
        AccessType retval= canAccess ? AccessType.YES : AccessType.NO;

        if (retval==AccessType.YES && OSInfo.isPlatform(OSInfo.WIN_VISTA)) {
            File testDir= new File(updateDir, "test-for-access");
            testDir.delete();
            testDir.mkdirs();
            String testPath= testDir.getPath();
            testPath= testPath.substring(3);
            String userHome= System.getProperty("user.home");
            String badResults= userHome + "\\AppData\\Local\\VirtualStore\\" +
                               testPath;
            File badFile = new File(badResults);
            if (badFile.exists()) {
                retval= AccessType.VistaNo;
                badFile.delete();
            }
        }
        return retval;
    }

//============================


   private void startJarMovingProcess() {
      String fname= System.getProperty("client.logfile.Name");
          
      String restartProg= null;
      if (_doRestart) restartProg= System.getProperty("loader.restart.prog");
      fname= fname+"-apploader";
      ClientLog.message("logfile name= " + fname);
      String jreDir = System.getProperty("java.home");
      String rootDir= Installer.deriveInstallationRoot();
      AppLoader.updateJarsInSeparateProcess(rootDir, jreDir, null,
                       Installer.DEFAULT_META_JAR,  2, restartProg, fname);
      AppProperties.setPreference(LOADER_UPDATE_PROP, true+"");
   }



    private static SoftwarePackage findServerPackageForJreJar(
                                    List<SoftwarePackage> serverPackageList) {
        String  jreJarStr= System.getProperty(JRE_JAR_PROP);
        return findServerPackageForJar(serverPackageList, jreJarStr);
    }

    private static SoftwarePackage findServerPackageForJar(
                                      List<SoftwarePackage>   serverPackageList,
                                      String jarName) {
      SoftwarePackage sPkg = null;
      boolean         found= false;
      Iterator<SoftwarePackage> j;
      for(j= serverPackageList.iterator(); j.hasNext() && !found; ) {
            sPkg= j.next();
            if (sPkg.getPackageFile().getName().equals(jarName) ) {
                 found= true; 
            }
      }
      if (!found) sPkg= null;
      return sPkg;
   }


   private int getUpdatedCount() {
      int               retval= 0;
      for(LocalPackageDesc  localPackage: _managedJars) {
         if (localPackage.isDownloaded()) retval++;
      }
      if (_updatingJRE) retval++;
      return retval;
   }

   private boolean isCriticalUpdate() {
      Iterator<LocalPackageDesc> i= _managedJars.iterator();
      LocalPackageDesc  localPackage;
      boolean           found= (_recoveryUpdate || _updatingJRE);
      while(i.hasNext() && !found) {
         localPackage= i.next();
         found=  (localPackage.isDownloaded() && localPackage.isCritical());
      }
      return found;
   }

   private void abortUpdate() {
      _doRestart      = false;
      _doUpdatesOnQuit= false;
      ClientLog.message("User aborted AutoUpdate.",
                        "Now deleting updated jars and unselecting the action");
      fireListeners(DONE_NO_UPDATES);
      File jarFiles[]= FileUtil.listJarFiles(
                                Installer.getAutoUpdateUpdateDirFile() );
      if (jarFiles!=null && jarFiles.length>0) {
           boolean success;
           for(File jar : jarFiles) {
                success= jar.delete();
                if (success) ClientLog.brief("deleting- " + jar.getName());
                else         ClientLog.warning("delete failed- "+ jar.getPath());
           }
      }
      if (_updateUI.askUserWantsDisabled()) {
          _action.setSelected(false);
      }
   }


   private void prepareQuitForUpdate() {
      int aliveCnt= AppInterface.getAliveCount("AutoUpdateController");
      if (aliveCnt > 1) {
         AutoUpdateUI.UpdateAction answer= UPDATE_NOW;
         for(int i=0; (aliveCnt>1 && answer==UPDATE_NOW); ) {
             answer  = _updateUI.showProgRunningQuitQuestion( (i>1) );
             aliveCnt= AppInterface.getAliveCount("AutoUpdateController");
         }
         if (answer==DONT_UPDATE) {
              abortUpdate();
         }
         else if (answer==UPDATE_NOW) {
              Assert.tst(aliveCnt==1);
              doQuit();
         }
         else {
              Assert.tst(false);
         }
      }
      else {
         doQuit();
      }


   }

    private void doQuit() {
         fireListeners(DONE_WITH_UPDATES);
         _updateUI.showFinalQuitInfo();
         _doRestart      = true;
         _doUpdatesOnQuit= true;
//         _quitAction.quitWithStatus(1);
        ClientEventManager.fireClientEvent(
                                      new ClientEvent(this,ClientEvent.REQUEST_QUIT,1));
    }


    private void updateLater() {
       _doRestart      = false;
       _doUpdatesOnQuit= true;
       fireListeners(DONE_WITH_UPDATES);
       ClientLog.message("AutoUpdate set up for later when users exits.");
    }

    private static PackageUpdate makePackageUpdate(String serverURL) {

        String servlet=AppProperties.getProperty(SERVLET_PROP, null);
        // the following property is used in PackageUpdate, MoveJre, AppLoader
        // need to be defined before PackageUpdate
        String iroot= Installer.deriveInstallationRoot();
        System.setProperty("loader.root.dir", iroot);
        String updateDir  = Installer.getAutoUpdateUpdateDir();
        String downloadDir= Installer.getAutoUpdateDownloadDir();

        ClientLog.message("UpdateServer:      " + serverURL,
                          "servlet:           " +
                          (servlet==null ? PackageUpdateServices.SERVLET_PACKAGE_UPDATE : servlet),
                          "Installation root: " + iroot,
                          "Download Dir:      " + updateDir,
                          "Update Dir:        " + downloadDir);
        
        return new PackageUpdate(serverURL, servlet, downloadDir, updateDir);
    }

//======================================================================
//----------------------- Private Innter Classes -----------------------
//======================================================================

   private class UserDecision implements Runnable {
      public void run() {
         AutoUpdateUI.UpdateAction decision= _updateUI.askUpdateQuestion(
                                                   _managedJars,
                                                   _recoveryUpdate,
                                                   _updatingJRE );
         switch (decision) {
              case UPDATE_NOW  : prepareQuitForUpdate(); break;
              case UPDATE_LATER: updateLater();          break;
              case DONT_UPDATE : abortUpdate();          break;
              default:                        Assert.tst(false);      break;
         }
      }
   }// end class UserDecision 


   private class UserInfo implements Runnable {
      public void run() {
         AutoUpdateUI.UpdateAction decision=
                          _updateUI.askInformOfMinorUpdate( _managedJars);
         switch (decision) {
              case UPDATE_NOW  : prepareQuitForUpdate(); break;
              case UPDATE_LATER: updateLater();          break;
              default:  Assert.tst(false);               break;
         }
      }
   }// end class UserDecision 
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
