package edu.caltech.ipac.client;

import edu.caltech.ipac.client.net.HostPort;
import edu.caltech.ipac.client.net.HttpPing;
import edu.caltech.ipac.client.net.NetworkManager;
import edu.caltech.ipac.gui.CommandTable;
import edu.caltech.ipac.gui.FontDefaults;
import edu.caltech.ipac.gui.IconFactory;
import edu.caltech.ipac.gui.OptionPaneWrap;
import edu.caltech.ipac.gui.SwingSupport;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.action.Prop;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Date: Nov 1, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class ClientStartup {

    private static final String DUMP_FILE_NAME     = "dump.prop";
    private static final String  START_WARNING= "startWarning";
    private final JFrame _f;
    private final Class _mainClass;
    private JLabel  _startWarningLabel= null;
//    private enum SysUIEventType {OPEN_APP, OPEN_FILE, PREF, PRINT_FILE, REOPEN_APP}
    private File _dropFile= null;
//    private static transient List<WeakReference<SystemUIEventListener>> _listeners =
//                                  new ArrayList<WeakReference<SystemUIEventListener>>();
    private static final String APP_POSITION= ".position";
    private static final String APP_PREFERRED_SIZE=".preferredSize";

//============================================================================
//---------------------------- Constructors ----------------------------------
//============================================================================


    public ClientStartup (String appName,
                          String dirBase,
                          JFrame f,
                          Class  mainClass) {
        _f= f;
        _mainClass= mainClass;

        // two singletons need to be initialized
        // all at the very beginnig of the application
        // This initializes the application name & the base directory
        Platform.getInstance().setDirectoryBase(dirBase);
        ApplicationVersion.getInstalledApplicationVersion().setAppName(appName);
        Platform.getInstance().saveAppInstallationDir(appName);

        try {
            // use reflections to do the following line so we do not
            // have to be dependent on the package
            //TargetUIControl.getInstance().setJFrame(rootJFrame);
            Class targetUIControlClass= Class.forName(
                           "edu.caltech.ipac.targetgui.TargetUIControl");
            Method getInstance= targetUIControlClass.getMethod("getInstance");
            Method setJFrame= targetUIControlClass.getMethod("setJFrame",
                                                             JFrame.class);
            Object noArgs[]= {};
            Object targetUIControlObj= getInstance.invoke(
                                          targetUIControlClass, noArgs );
            setJFrame.invoke( targetUIControlObj, _f );
        } catch (Exception e) {
            ClientLog.message(
                           "could not access singleton class TargetUIControl",
                           "planner.jar is probably not in classpath");
        }

        /*
         * -- add this main class path to icon factory
         */
        IconFactory.getInstance().addResource(mainClass);
    }

//============================================================================
//---------------------------- Public Methods -------------------------
//============================================================================
    public void loadProperties(String propFileName,
                               String userPrefFileName) throws IOException {
        File userdir= Platform.getInstance().getWorkingDirectory();
        AppProperties.addApplicationProperties(_mainClass, "resources/"+
                                                           propFileName);


        loadUserProperties(userdir, propFileName, userPrefFileName);


        // --now that properties are loaded set the tip delay if specified

        ApplicationVersion av=
                 ApplicationVersion.getInstalledApplicationVersion();
        String base= av.getAppName();
        ToolTipManager tman= ToolTipManager.sharedInstance();
        int tipDelay = AppProperties.getIntPreference(
                                      base+ ".TipDelay", 7000);
        tman.setDismissDelay(tipDelay);



        /*
        * -- set font defaults for all widgets
        */
        Platform pf= Platform.getInstance();
        FontDefaults.scaleDefaults(pf.getFontDelta());

    }

    public void makeAppVisible(final boolean checkPlatformWarnings,
                               final boolean startBackgroundBuilding) {
       makeAppVisible(checkPlatformWarnings,startBackgroundBuilding,
                      new Point(40,40),null);
    }


    public void makeAppVisible(final boolean checkPlatformWarnings,
                               final boolean startBackgroundBuilding,
                               final Point   location) {
        makeAppVisible(checkPlatformWarnings,startBackgroundBuilding,
                       location,null);

    }



    public void makeAppVisible(final boolean checkPlatformWarnings,
                               final boolean startBackgroundBuilding,
                               final Point   location,
                               final String  warningProp) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                makeVisibleLater(_f,
                                 checkPlatformWarnings,
                                 startBackgroundBuilding,
                                 location,
                                 warningProp);
            }
        });
    }


    public void addDefaultPortWatcher() throws IOException {
        ApplicationVersion av=
             ApplicationVersion.getInstalledApplicationVersion();
        String base= av.getAppName();
        new PortWatcher( new DefaultReceiveDataFactory(), base, true);

//        CommandTable com= CommandTable.getInstance();
//        QuitAction qa= (QuitAction)com.findCommand("quit");
//        qa.addQuitListener(portWatcher);
    }


    /**
     * Parse the command line options and perform whatever action is required.
     * @param args the array of args passed the the main method
     */
    public void parseOptions(String args[]) {
        File workingDir= Platform.getInstance().getWorkingDirectory();
        File dumpProps= null;


        for(int i= 0; (i<args.length); i++) {
            File f= new File(args[i]);
            if (f.canRead()) {
                _dropFile= f;
            }
            if (args[i].equals("-log")) {
                ApplicationVersion av=
                             ApplicationVersion.getInstalledApplicationVersion();
                String base= av.getAppName();
                if (base==null) base= "unknowName";
                LogFileNamer.setLogFileRoot(base);
                String lname= LogFileNamer.redirectIO(workingDir);
                if (lname!=null) {
                    System.setProperty("client.logfile.Name", lname);
                }
                else {
                    ClientLog.warning(
                             "Could not set property client.logfile.Name",
                             "LogFileNamer.redirectIO() returned a null",
                             "workingDir= " + workingDir,
                             "base= ", base);
                }
            } // end if log
            else if (args[i].equals("-properties") ||
                     args[i].equals("-prop")       ||
                     args[i].equals("-p")       ) {
                if (i<args.length-1) {
                    try {
                        i++;
                        AppProperties.addUserProperties( new File(args[i]),false);
                    } catch (IOException e) {
                        ClientLog.warning(
                             "Could not find comand line property file: " + args[i]);
                    }
                }
            } // end if properties
            else if (args[i].equals("-dump")  ||
                     args[i].equals("-d")     ) {
                dumpProps= new File(workingDir, DUMP_FILE_NAME);
            } // end if dump
            else if (args[i].equals("-help")  ||
                     args[i].equals("-h")     ) {
                showHelp();
                System.exit(0);
            }
        } // end loop
        CommandTable com= CommandTable.getInstance();
        if (dumpProps != null) com.builderDumpPropertiesWhenDone(dumpProps);

        List<String> outList= new ArrayList<String>(args.length+1);
        outList.add("--- Arguments ---");
        for(String s : args) outList.add(s);
        ClientLog.message(outList);
    }


    /**
     * -- configure network
     * @param hp the machine to ping
     */
    public void configureSimplePinger(HostPort hp) {
        NetworkManager     nman= NetworkManager.getInstance();
        nman.setPinger( new HttpPing());
        nman.setPingTarget(hp);
    }


    /**
     * These gifs override the standard JOptionPane icons.
     * @param icons the set of icons
     */
    public void loadJOptionPaneGifs(OptionPaneIcons icons) {
        UIManager.put("OptionPane.errorIcon",       icons.getErrorIcon());
        UIManager.put("OptionPane.informationIcon", icons.getInfoIcon());
        UIManager.put("OptionPane.warningIcon",     icons.getWarningIcon());
        UIManager.put("OptionPane.questionIcon",    icons.getQuestionIcon());
    }

//=========================================================================
//------------ App Position and Preferred Size ----------------------------
//=========================================================================

    public static Point getAppPositionPreference(String root) {
        Point position = new Point(100,40); //default position
        String prefValue = AppProperties.getPreference(root+APP_POSITION);
        if (prefValue != null) {
            int x;
            int y;
            Dimension dim = Platform.getScreenSize();
            try {
                int i = prefValue.indexOf(",");
                x = Integer.parseInt(prefValue.substring(0,i));
                y = Integer.parseInt(prefValue.substring(i+1));
                if (y < dim.height && x < dim.width) {
                    position.setLocation(x,y);
                }
            } catch (Exception e) {
                position.setLocation(100,40);
            }
        }
        return position;
    }

    public static Dimension getAppPreferredSizePreference(String root) {
        ApplicationVersion av=
                 ApplicationVersion.getInstalledApplicationVersion();
        String base= av.getAppName();
        int minWidth = AppProperties.getIntPreference(
                                      base+ ".mainWindow.min.Width", 400);
        int minHeight = AppProperties.getIntPreference(
                                      base+ ".mainWindow.min.Height", 300);
        Dimension preferredSize = new Dimension(850, 600); //default size
        String prefValue = AppProperties.getPreference(root+APP_PREFERRED_SIZE);
        if (prefValue != null) {
            int width;
            int height;
           try {
                int i = prefValue.indexOf(",");
                width = Integer.parseInt(prefValue.substring(0,i));
                height = Integer.parseInt(prefValue.substring(i+1));
           } catch (Exception e) {
                width = preferredSize.width;
                height = preferredSize.height;
           }
           if (width < minWidth) width = minWidth;
           if (height < minHeight) height = minHeight;
           preferredSize.setSize(width, height);
        }
        return preferredSize;
    }

    public static void setAppPositionPreference(String root, int x, int y) {
        AppProperties.setPreference(root+APP_POSITION, x+","+y);
    }

    public static void setAppPreferredSizePreference(String root, int width, int height) {
        AppProperties.setPreference(root+APP_PREFERRED_SIZE, width + "," + height);
    }
//============================================================================
//---------------------------- Private / Protected Methods -------------------
//============================================================================

    private static void loadUserProperties(File   userdir,
                                           String userPropFileName,
                                           String userPrefFileName) {
        File f= null;
        try {
            f=new File(userdir, userPropFileName);
            AppProperties.addUserProperties(f, true);
        } catch (IOException e) {
            ClientLog.warning("Could not load user property file: "+f.getPath(),
                              e.toString());
        }
        try {
            f=new File(userdir, userPrefFileName);
            AppProperties.setAndLoadPreferenceFile(f);
        } catch (IOException e) {
            ClientLog.warning("Error loading preference file: "+f.getPath(),
                              e.toString());
        }
    }


    private void makeVisibleLater(JFrame f,
                                  boolean checkPlatformWarnings,
                                  boolean startBackgroundBuilding,
                                  Point   location,
                                  final String  warningProp) {
        f.pack();
        f.setLocation(location);
        Platform.getInstance().interfaceToMac();
        f.setState(Frame.NORMAL);
        f.setVisible(true);

        if (_dropFile!=null) fireOpenFile(_dropFile);



        if (checkPlatformWarnings) {
            String warnProp= Platform.getInstance().getBuggyWarningProperty();
            if (warnProp != null) {
                OptionPaneWrap.showWarning( f,
                                            AppProperties.getProperty(warnProp, "warning!"),
                                            "Warning!");
            }
        }
        if (warningProp!=null) showStartupWarning(warningProp);
        if (startBackgroundBuilding) CommandTable.getInstance().startBuilding(); // start the builder
    }


    public void showStartupWarning(String propRoot) {
        String startWarning= Prop.getPlatformProp(propRoot,
                                                  START_WARNING +"."+ Action.NAME,
                                                  null);
        if (startWarning!=null) {
            _startWarningLabel= new JLabel(startWarning);
            Color fg= SwingSupport.getColorFromProp(
                           propRoot+"."+START_WARNING+".foreground",null);
            Color bg= SwingSupport.getColorFromProp(
                           propRoot+"."+START_WARNING+".background",null);
            Font font= SwingSupport.getFontFromProp(
                                     propRoot +"."+ START_WARNING, null);
            if (font!=null) _startWarningLabel.setFont(font);
            if (fg!=null) _startWarningLabel.setForeground(fg);
            if (bg!=null) {
                _startWarningLabel.setOpaque(true);
                _startWarningLabel.setBackground(bg);
            }
            Runnable run= new Runnable() {
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        OptionPaneWrap.showWarning( _f, _startWarningLabel);
                    } catch (InterruptedException ignore) {  }

                } };
            new Thread(run).start();
        }
    }

    /**
     * Show the help message about command line options.
     */
    private static void showHelp() {
        System.out.println("\nCommand line parameters:");
        System.out.println("  -log        : redirect all IO " +
                           "to a log file in the spot directory." );
        System.out.println("  -properties : the next parameter is a " +
                           "properties file" );
        System.out.println("  -dump       : write all user properties\n" +
                           "                to a properties file in the " +
                           "working directory- " + DUMP_FILE_NAME);
    }


    private void fireOpenFile(File f) {
        if (f!=null) {
            ClientEvent ev= new ClientEvent(this,ClientEvent.REQUEST_OPEN_FILE,_f,f);
            ClientEventManager.fireClientEvent(ev);
        }
    }




//============================================================================
//---------------------------- Factory Methods -------------------------------
//============================================================================


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