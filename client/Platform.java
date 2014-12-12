package edu.caltech.ipac.client;

import ch.randelshofer.quaqua.QuaquaManager;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.Installer;
import edu.caltech.ipac.util.OSInfo;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.StringTokenizer;

/*
 *
 * Platform is a singleton class unlike most singleton classes is
 * configurable by the constructor.
 * You typically access a singleton by getInstance.  This is true here
 * but it is also possible to call a constructor first to initialize the class.
 * If getInstance is called before the constructor then the default constructor
 * is called.  You may not call the constructor after the first getInstance is
 * called.
 */
public class Platform {

   // Look and Feel
   private static final String MSWIN_LF=
              "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
   private static final String MOTIF_LF=
              "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
   private static final String METAL_LF=
              "javax.swing.plaf.metal.MetalLookAndFeel";
   public static final String MAC_LF=
                                 "apple.laf.AquaLookAndFeel";
   public static final String MAC_QUAQUA_LF=
                                  "ch.randelshofer.quaqua.QuaquaLookAndFeel";
    public static final String INSTALL_ROOT_NAME="-installroot.dat";

   static private Platform _theInstance= null;
   private File    _workingDir;
   private File    _aliveDir= null;
   private boolean _useSmallIcon= true;
   private String  _warningProp       = null;
   private String  _lf;
   private int     _fontDelta         = 0;
   private int     _largeTitleFontSize= 18;
   private int     _smallTitleFontSize= 11;
   private boolean _smallScreen       = false;
   private boolean _threadSleepProblem= false;
   private boolean _animatedGifs      = true;
   private String  _dirBase;
   private String  _conversionDir;
   private final int  _minorVersion;
   private static boolean _gui= true;

   private Platform() {
        configure();
        _theInstance= this;
        String versionStr= System.getProperty("java.version");
        StringTokenizer st= new StringTokenizer(versionStr, ".");
        int majorVersion= Integer.parseInt(st.nextToken());
        _minorVersion= Integer.parseInt(st.nextToken());
       System.out.println("minor= " + _minorVersion);
        if (_gui) setLookAndFeelIfNecessary();
   }


    public static void setGUI(boolean gui) { _gui= gui; }


   public void setLargeTitleFontSizeBase(int size) {
        _largeTitleFontSize = size;
   }
   public void setSmallTitleFontSizeBase(int size) {
        _smallTitleFontSize = size;
   }

   public void setConversionDir(String conversionDir) {
      _conversionDir= conversionDir;
   }

   public void setDirectoryBase(String dirBase) {
      //_envVarible= overrideEnvVarible;
      _dirBase= dirBase;
      recomputeWorkingDir();
   }

    public boolean isVersionAtLeast(int v) {
        return (_minorVersion>=v);
    }

    /**
     * Return the only instance of Platform
     */
   public static Platform getInstance() {
        if (_theInstance == null) _theInstance= new Platform();
        return _theInstance;
   }

   public boolean useSmallIcon() { return _useSmallIcon; }

   public File getWorkingDirectory() {
        return _workingDir;
   }

   public File getAliveDirectory() {
        if (_aliveDir == null) {
           _aliveDir= new File(_workingDir, "alive");
           if (!_aliveDir.exists()) _aliveDir.mkdir();
           Assert.tst(_aliveDir.canWrite());
        }
        return _aliveDir;
   }

   public int getFontDelta()      { return _fontDelta; }
   public boolean isSmallScreen() { return _smallScreen; }

   public void setLookAndFeel() {
      try {
         UIManager.setLookAndFeel(_lf);
         ClientLog.message("Look And Feel:" + _lf);
      }
      catch (Exception e) {
         ClientLog.message("could not setup look and feel.", e.toString());
      }
   }

   public void setLookAndFeelIfNecessary() {
       if (OSInfo.isPlatform(OSInfo.MAC) ||  // mac
           OSInfo.isPlatform(OSInfo.ANY_WINDOWS) ) {
          try {
              if (OSInfo.isPlatform(OSInfo.MAC)) {
                  try {
                      setMacLookAndFeel();
                  } catch (NoClassDefFoundError e) {
                      ClientLog.warning("could not set mac look and feel");

                  }
              }
              else {
                  UIManager.setLookAndFeel(_lf);
              }
             ClientLog.message("Look And Feel:" + _lf);
          }
          catch (Exception e) {
             ClientLog.message("could not setup look and feel.", e.toString());
          }
      }
   }

   public void printLookAndFeelList() {
       UIManager.LookAndFeelInfo lfary[]= UIManager.getInstalledLookAndFeels();
       System.out.println("Available Look and Feel List:");
       System.out.println("Current:" + UIManager.getLookAndFeel() );
       for(int i=0; (i< lfary.length); i++) {
          System.out.println("   " + lfary[i].getName() + "  --  " +
                                     lfary[i].getClassName() );
       }
   }

    public void saveAppInstallationDir(String appname) {
        File f= new File(getWorkingDirectory(),appname+INSTALL_ROOT_NAME);
        PrintWriter pw= null;
        try {
            pw= new PrintWriter(f);
            pw.println(Installer.deriveInstallationRoot());
//            ClientLog.message("wrote to: "+ f.getPath());
        } catch (FileNotFoundException e) {
            ClientLog.warning("Could not save installtion directory in file:" +f.getPath());
            // do nothing
        } finally {
            FileUtil.silentClose(pw);
        }
    }


    public File findAppInstallationDir(String appname) {
        File f= new File(getWorkingDirectory(),appname+INSTALL_ROOT_NAME);
        BufferedReader br= null;
        File retval= null;
        try {
            br= new BufferedReader(new FileReader(f));
            String results= br.readLine();
            retval= new File(results);
            if (!retval.exists()) retval= null;
        } catch (IOException e) {
            ClientLog.warning("Could not save installtion directory in file:" +f.getPath());
            // do nothing
        } finally {
            FileUtil.silentClose(br);
        }
        return retval;
    }





   public int getLargeTitleFontSize() {
        return _largeTitleFontSize + _fontDelta;
   }

   public int getSmallTitleFontSize() {
        return _smallTitleFontSize + (_fontDelta < 0 ? -1 : _fontDelta);
   }

   public Font smartDeriveFont(Font f, float size) {
        Font retval= f;
        if (size < 9.0F) {
            retval= f.deriveFont(9.0F);
        }
        else {
            retval= f.deriveFont(size);
        }
        return retval;
   }

   /**
    * Set the working directory for the application.  Most times this routine
    * should never be called.  It is computed automaticly.  However, at times
    * when the class is unable to compute a working directory then it would
    * be appropriate to set one directory.
    * @param f the directory file
    */
   protected void setWorkingDirectory(File f) {
        _workingDir= f;
   }

   protected void configure() {
       if (_gui) {
           Dimension dim= getScreenSize();
           ClientLog.brief("Screen Size: " + dim.width + "x" + dim.height);
           if      (dim.height < 1024 && dim.height >= 768) {
               _fontDelta= -1;
           } // end if
           else if (dim.height < 768) {
               _fontDelta= -3;
               _smallScreen= true;
           } // end else
       }

       if (OSInfo.isPlatform(OSInfo.SUN)) {
           ClientLog.brief("Platform: SunOS");
           _useSmallIcon= false;
           _animatedGifs= true;
           _lf= MOTIF_LF;
       }
       else if (OSInfo.isPlatform(OSInfo.LINUX)) {
           ClientLog.brief("Platform: Linux");
           _useSmallIcon= false;
           _lf= MOTIF_LF;
           _threadSleepProblem= false;
       }
       else if (OSInfo.isPlatform(OSInfo.ANY_WINDOWS)) {
           ClientLog.brief("Platform: Windows");
           _useSmallIcon= true;
           _lf= MSWIN_LF;
       }
       else if (OSInfo.isPlatform(OSInfo.MAC)) {
           ClientLog.brief("Platform: Macintosh");
           _useSmallIcon= false;             // use until we know better
           //_warningProp= "Platform.MacWarning.Name";
           if (_fontDelta ==-1) { // if are on a small screen go event smaller
               _fontDelta= -2;
           }
           System.setProperty( "Quaqua.tabLayoutPolicy","wrap");
//             _lf= MAC_LF;
           _lf= MAC_QUAQUA_LF;
//             _lf= METAL_LF;
       }
       else if (OSInfo.isPlatform(OSInfo.UNKNOWN_UNIX)) {
           ClientLog.brief("Platform: is unknown and " +
                           " not supported, however I am guessing a flavor of unix");
           _useSmallIcon= false;
           _lf= MOTIF_LF;
       }
       else { // guess - the guess some unsupported unix
           Assert.tst(false);
       }
       recomputeWorkingDir();
   }

   private void recomputeWorkingDir() {
      _workingDir= null;
      File oldDirFile= null;
      if (_dirBase != null) {
          if (_conversionDir!=null) {
              String oldDir= computerDirName(_conversionDir);
              oldDirFile= new File(oldDir);
              if (!(oldDirFile.exists() && oldDirFile.canWrite())) {
                  oldDirFile= null;
              }
          }
          String dir= computerDirName(_dirBase);
          if (oldDirFile!=null) {
              File newDirFile= new File(dir);
              oldDirFile.renameTo(newDirFile);
          }
          _workingDir= determineWorkingDir(dir);
      }
   }



   private String computerDirName(String base) {
      String dir= null;
      String userHome= System.getProperty("user.home");
      switch (OSInfo.getBaseOStype()) {
          case OSInfo.BASED_ON_NT:
                 if(OSInfo.isPlatform(OSInfo.WIN_VISTA))
                 {
                     dir= userHome + "\\" + "AppData\\Roaming\\" + base;
                 }
                 else
                 {
                     dir= userHome + "\\" + "Application Data\\" + base;
                 }
                 break;
          case OSInfo.BASED_ON_DOS:  dir= "c:\\" + base;
                 break;
          case OSInfo.BASED_ON_UNIX: dir= userHome + "/." + base;
                 break;
          default:                Assert.tst(false); break;
      }
       ClientLog.message(true,"dir= "+ dir,
                         "userHome= " + userHome);
      return dir;
   }

   protected File determineWorkingDir(String inDir) {
        File dir= null;
        if (inDir != null) {
             dir= new File(inDir);
             boolean success= prepareDir(dir);
             if (!success) dir= null;
        }

       ClientLog.message(true,"inDir= "+ inDir,
                         "dir= " + dir);
        return dir;
   }


   private boolean prepareDir(File f) {
        boolean retval=false;
        if (f != null) {
           if ( f.exists() && f.canWrite() ) {
                if ( f.isDirectory() ) {
                    retval= true;
                    ClientLog.message("Working dir: " + f.getAbsolutePath() );
                }
           }
           else {
               try {
                  retval= f.mkdir();
                  ClientLog.message("Creating Working dir: " +
                                        f.getAbsolutePath() );
               } catch (java.lang.SecurityException e) { }
           }
        } // end if f != null
       ClientLog.message(true,"retval = "+ retval,
                         "exist= " + f.exists(),
                         "canWrite= " + f.canWrite());
        return retval;
   }

   public boolean supportsAnimatedGifs()    { return _animatedGifs; }
   public boolean isThreadedSleepProblem()  { return _threadSleepProblem; }
   public String  getBuggyWarningProperty() { return _warningProp; }



   public void interfaceToMac() {
       if(OSInfo.isPlatform(OSInfo.MAC)) {
           ClientLog.brief("MAC: Setting up interface to MAC");
           try {
               Class interfaceClass=Class.forName(
                              "edu.caltech.ipac.client.InterfaceToMac");
               interfaceClass.getConstructor().newInstance();
           } catch (Exception e) {
               ClientLog.warning(true,
                                 "We are running on a mac and cannot access "+
                                 "the InterfaceToMac class");
               e.printStackTrace();
           }
       }
   }

    private void setMacLookAndFeel() {
        if(OSInfo.isPlatform(OSInfo.MAC)) {
            ClientLog.message("MAC: Settting quaqua look and feel");
            try {
//                Class lfMan=Class.forName( "ch.randelshofer.quaqua.QuaquaManager");
//                Method meth= lfMan.getMethod("getLookAndFeel",(Class[])null);
//                LookAndFeel lAndF= (LookAndFeel)meth.invoke(null,(Object[])null);
//                UIManager.setLookAndFeel(lAndF);

                UIManager.setLookAndFeel(QuaquaManager.getLookAndFeel());


            } catch (UnsupportedLookAndFeelException e) {
                ClientLog.warning(true,
                                  "We are running on a mac and cannot access "+
                                  "the quaqua look and feel");
                e.printStackTrace();
                try {
                    UIManager.setLookAndFeel(_lf);
                } catch (Exception e1) {
                    ClientLog.warning("could not setup look and feel.", e1.toString());
                }
            }
        }

    }

    public static Dimension getScreenSize() {
        GraphicsEnvironment graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDev = graphicsEnv.getDefaultScreenDevice();
        DisplayMode mode = screenDev.getDisplayMode();
        return new Dimension(mode.getWidth(), mode.getHeight());
    }


   public String toString() {
     String newline= "\n";
     try {
        newline= System.getProperty("line.separator");
     } catch (Exception e) {}
     Toolkit tk= Toolkit.getDefaultToolkit();
     Dimension dim= tk.getScreenSize();
     int       res= tk.getScreenResolution();

     Locale locale= Locale.getDefault();
     return
        "OS Name:              "   + OSInfo.getOSName()          + newline +
        "Recognized OS Name:   "   + OSInfo.getRecognizedPlatform()
                                                                 + newline +
        "Locale:               "  + locale.toString()          + newline +
        "use small icon:       "  + useSmallIcon()            + newline +
        "working dir:          "  + getWorkingDirectory()        + newline +
        "font delta:           "  + getFontDelta()               + newline +
        "isSmallScreen:        "  + isSmallScreen()              + newline +
        "largeTitleFontSize:   "  + getLargeTitleFontSize()      + newline +
        "smallTitleFontSize:   "  + getSmallTitleFontSize()      + newline +
        "supportsAnimatedGifs: "  + supportsAnimatedGifs()       + newline +
        "threadedSleepProblem: "  + isThreadedSleepProblem()     + newline +
        "buggyPlatformWarning: "  + getBuggyWarningProperty()    + newline +
        "Screen Size:          "  + dim.width + "x" + dim.height + newline +
        "Resolution:           "  + res;
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
