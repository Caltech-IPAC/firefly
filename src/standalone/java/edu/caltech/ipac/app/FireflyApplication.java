package edu.caltech.ipac.app;

import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class FireflyApplication {

    private static final File pwd = new File(System.getProperty("user.dir"));
    private static final File ffDir = new File(System.getProperty("user.home"), ".firefly");
    private static final File installDir = new File(pwd, "../..").getAbsoluteFile();
    private static final File tomcatDir = new File(ffDir,"server");
    private static final File tomcatTmp = new File(tomcatDir, "temp");
    private static final File tomcatLogs = new File(tomcatDir, "logs");
    private static final File applicationRoot = new File(installDir, "application");
    private static final File applicationDir = new File(applicationRoot, "current");
    private static final File cleanupScript= new File(applicationDir,"standalone_cleanup.sh");
    private static final File installScript= new File(applicationDir,"install.sh");
    private static final File configFile = new File(ffDir, "config.json");
    private static final File dockIconFile= new File(applicationDir,"fireflyDockIcon.png");
    private static final File applicationLogFile= new File(tomcatLogs, "application.log");
    private static final File fireflyWarDir= new File(applicationDir, "firefly-war");
    private static final File versionTagPropFile= new File(applicationDir, "version.tag");
    private static final File versionTextOutFile= new File(ffDir, "version.txt");
    private static final String compressibleMimeType= String.join(",", Arrays.asList(
            "text/html", "text/plain", "text/css", "text/javascript",
            "application/javascript", "application/json", "application/xml",
            "text/xml", "application/x-votable+xml", "application/x-yaml", "application/ld+json",
            "image/svg+xml", "text/csv", "application/xhtml+xml",
            "application/rss+xml", "application/atom+xml", "application/x-font-ttf",
            "font/otf", "font/woff", "font/woff2",
            "application/octet-stream"
    ));
    private static final boolean useLogFile= true;
    private static final int DEFAULT_PORT= 8888;
    private static String fireflyVersion;
    private static String javaVersion;
    private static boolean updateAvailable=  false;
    private static PrintStream terminalOut= System.out;
    private static boolean initComplete= false;
    private static JLabel aboutLabel= null; // only used in desktop mode
    private static MenuItem aboutItem = null; // only used in desktop mode
    private static boolean firstUpdateCheck= true;



    public static void start() throws LifecycleException, URISyntaxException, IOException, InterruptedException {
        fireflyVersion= saveVersion();
        javaVersion = System.getProperty("java.version");
        boolean useDesktop= AppProperties.getBooleanProperty("runAsDesktopApplication", false);
        ensureFireflyDir();
        var port= getPort();
        File readyTextOutFile= new File(ffDir, "ready-"+port+".txt");
//        File pidTextOutFile= new File(ffDir, "pid-"+port+".txt");
        File pidTextOutFile= new File(ffDir, "pid.txt");

        if (useDesktop) SwingUtilities.invokeLater(() -> initAboutLabel(port));


        if (useLogFile) setupLogger();

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(tomcatDir.getAbsolutePath());
        tomcat.setPort(port);


        boolean tomcatStarted = false;
        if (!isRunning(port)) {
            var ignore= readyTextOutFile.delete();
            if (useDesktop) setupUI(tomcat, port, pidTextOutFile, readyTextOutFile);
            tomcat.addUser("admin", "admin");
            tomcat.addWebapp("/firefly", fireflyWarDir.getAbsolutePath());
            terminalOut.println("Firefly server starting (is takes a few seconds)...");
            Connector connector= tomcat.getConnector();
            connector.setPort(port);
            connector.setProperty("compression", "on");
            connector.setProperty("useSendfile", "false");
            connector.setProperty("compressibleMimeType", compressibleMimeType);
            tomcat.start();
            savePid(pidTextOutFile);
            tomcatStarted = true;
        }

        if (!tomcatStarted) {
            terminalOut.println("Firefly is already running");
            openBrowser(port,true);
            fireflyReadyMessage(port, null);
            System.exit(0);
        }


        initComplete= true;
        if (useDesktop) {
            hideSplash();
            SwingUtilities.invokeLater(() -> updateAboutLabel(port));
            openBrowser(port,true);
        }
        fireflyReadyMessage(port, readyTextOutFile);
        Thread.sleep(5 * 1000); // 5 seconds
        updateAvailable= doAutoUpdateCheck();
        doWorkAreaCleanup();
        while (tomcat.getServer().getState().isAvailable()) {
            Thread.sleep(3600 * 1000); // 1 hour
            if (!updateAvailable) updateAvailable= doAutoUpdateCheck();
            doWorkAreaCleanup();
        }

    }

    public static boolean doAutoUpdateCheck() {
        boolean updateAvailable= false;
        try {
            var result= URLDownload.getDataFromURL(new URI("https://api.github.com/repos/Caltech-IPAC/firefly/releases/latest").toURL(),null,null);
            var obj= (JSONObject) new JSONParser().parse(result.getResultAsString());
            var availableVersion= (String) obj.get("name");
            var newVerAvailable= isNewVersionAvailable(fireflyVersion,availableVersion);

            String urlStr= null;
            var assetsAry= (JSONArray)obj.get("assets");
            if (newVerAvailable && assetsAry!=null && !assetsAry.isEmpty()) {
                for(Object entry: assetsAry){
                    JSONObject asset= (JSONObject)entry;
                    if (StringUtils.areEqual((String)asset.get("name"),"standalone.zip")) {
                        urlStr= (String)asset.get("url");
                    }
                }
            }
//            the following code can be uncommented  for testing auto-update
//            String overrideUrlStr= null;
//            overrideUrlStr= "/Users/roby/dev/firefly/build/dist/standalone.zip";
//            if (overrideUrlStr!=null) urlStr= overrideUrlStr;
            updateAvailable= urlStr!=null;
            if (updateAvailable) doUpdateInstall(urlStr);

            String updateMsg= updateAvailable ? ", Update available (relaunch Firefly to finish update)" : "";

            String msg= "**** Update Check: Current Version: "+fireflyVersion
                    + ", Available version: "+ availableVersion
                    + ", Java Version: "+ javaVersion + updateMsg;

            if (firstUpdateCheck) terminalOut.println(msg);
            System.out.println(msg);
            firstUpdateCheck= false;

        } catch (FailedRequestException | MalformedURLException | URISyntaxException | ParseException e) {
            System.out.println(e.toString());
        }
        return updateAvailable;
    }

    public static boolean isNewVersionAvailable(String currVer, String availableVer) {
        if (currVer==null) currVer= "0,0.0";
        if (availableVer==null) availableVer= "0,0.0";
        var cVer= currVer.split("\\.");
        var nVer= availableVer.split("\\.");
        if (cVer.length!=3 || nVer.length!=3) return false;
        var curr= Arrays.stream(cVer).map((s) -> StringUtils.getInt(s,0)).toList();
        var next= Arrays.stream(nVer).map((s) -> StringUtils.getInt(s,0)).toList();
        return (next.get(0)>curr.get(0) || next.get(1)>curr.get(1) || next.get(2)>curr.get(2));
    }


    public static void savePid(File pidTextOutFile) {
        FileUtil.writeStringToFile(pidTextOutFile,ProcessHandle.current().pid()+"");
    }

    public static String saveVersion() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(versionTagPropFile));
            VersionUtil.ingestVersion(props);
            var vInfo= VersionUtil.getVersionInfo();
            var fVerList= vInfo.stream().filter(kv -> kv.getKey().equals("Firefly Version")).toList();
            if (fVerList.size()==1) {
                var vStr= fVerList.getFirst().getValue();

                String major="0";
                String minor="0";
                String rev="0";
                var phase1= vStr.split("-");
                var realVStr= phase1[0];
                var parts= realVStr.split("\\.");
                if (parts.length>1) {
                    major= parts[0];
                    minor= parts[1];
                    if (parts.length>2) rev= parts[2];
                }
                var version= major+"."+minor+"."+rev;
                FileUtil.writeStringToFile(versionTextOutFile, version);
                return version;
            }
        } catch (IOException e) {
            System.out.println("failed to get version: " + e.toString());
        }
        return null;
    }

    public static void doUpdateInstall(String packageUrl) {
        ProcessBuilder pb = new ProcessBuilder(installScript.getAbsolutePath(),
                "-url", packageUrl, "-asUpdate",
                "-installDir", installDir.getAbsolutePath() );
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) System.out.println("auto update job failed with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void setupLogger() throws IOException {
        Logger logger= Logger.getLogger("");
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }


        // setup logger
        Handler fileHandler = new FileHandler(applicationLogFile.getAbsolutePath(), true);
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);

        // set system out for stuff that logger misses
        System.setOut(new PrintStream(new FileOutputStream(applicationLogFile,true)));


        String terminalDevice = System.getProperty("os.name").toLowerCase().contains("win")
                ? "CON" : "/dev/tty";
        boolean helpToLog= AppProperties.getBooleanProperty("userHelpToLog", false);
        terminalOut = helpToLog ? System.out : new PrintStream(new FileOutputStream(terminalDevice));
    }

    public static void doWorkAreaCleanup() {
        ProcessBuilder pb = new ProcessBuilder(cleanupScript.getAbsolutePath()," --once");
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) System.out.println("clean up job failed with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void openBrowser(int port, boolean doSleep) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                if (doSleep) Thread.sleep(500);
                Desktop.getDesktop().browse(new URI(makeUrlString(port)));
            } catch (URISyntaxException | InterruptedException | IOException ignore) {
                System.out.println("Could not open browser");
            }
        }
    }

    public static String makeUrlString(int port) { return "http://localhost:"+port+"/firefly/";}

    public static void ensureFireflyDir() {
        confirmDirOrExit(ffDir);
        confirmDirOrExit(tomcatDir);
        confirmDirOrExit(tomcatTmp);
        confirmDirOrExit(tomcatLogs);
    }

    public static int getPort() {
        int portProp= AppProperties.getIntProperty("firefly.port",0);
        if (portProp!=0) return portProp;
        try {
            if (!configFile.canRead()) return DEFAULT_PORT;
            String pStr= FileUtil.readFile(configFile);
            if (pStr==null) return DEFAULT_PORT;
            var obj= (JSONObject) new JSONParser().parse(pStr);
            var ports= (JSONObject)obj.get("ports");
            if (ports==null) return DEFAULT_PORT;
            Long port= (Long)ports.get("firefly");
            if (port==null) return DEFAULT_PORT;
            return port.intValue();
        } catch (IOException | NumberFormatException | ParseException e) {
            return DEFAULT_PORT;
        }
    }

    private static void confirmDirOrExit(File dir) {
        boolean exists = true;
        if (!dir.exists()) {
            exists = dir.mkdir();
        }
        if (!exists || !dir.canWrite()) {
            System.out.println("Can't write to " + dir.getAbsolutePath() + " directory");
            System.exit(0);
        }
    }

    private static void setupUI(Tomcat tomcat, int port, File pidTextOutFile, File readyTextOutFile) {
        System.setProperty("apple.awt.UIElement", "true");
//      setupDock(port);
        setupTray(tomcat, port, pidTextOutFile, readyTextOutFile);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                   try {
                       if (tomcat.getServer().getState().isAvailable()) {
                           System.out.println("Shutting down Firefly server...");
                           tomcat.stop();
                           tomcat.destroy();
                           var ignore= pidTextOutFile.delete();
                       }
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
                   finally {
                       Runtime.getRuntime().halt(0);
                   }
               }));
    }

// Keep this around- we might want to reenable the dock, todo - what does linux do with this code?
//    public static void setupDock(int port) {
//       //System.setProperty("apple.awt.UIElement", "false"); <<- this property should be set to false on the java command line
//       System.setProperty("apple.laf.useScreenMenuBar", "true");
//       System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Firefly");
//        if (Desktop.isDesktopSupported()) {
//            Desktop desktop = Desktop.getDesktop();
//            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
//                desktop.setAboutHandler(e -> showAboutDialog(port) );
//            }
//        }
//
//    }

    public static void stopFireflyServer(Tomcat tomcat, File pidTextOutFile, File readyTextOutFile) {
        try {
            if (tomcat.getServer().getState().isAvailable()) {
                System.out.println("Shutting down Firefly server...");
                tomcat.stop();
                tomcat.destroy();
                var ignore= pidTextOutFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            Runtime.getRuntime().halt(0);
        }
    }

    public static void initAboutLabel(int port) {
        aboutLabel= new JLabel();
        aboutLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(makeUrlString(port)));
                } catch (Exception ignore) { }
            }
        });
    }

    public static void updateAboutLabel(int port) {
        if (aboutLabel==null) return;
        String outstr= String.format("<html>Firefly Version: %s<br>Java Version: %s<br>",
                fireflyVersion, javaVersion);
        outstr+= String.format("To load Firefly: <a href=\"%s\">%s</a>",makeUrlString(port), makeUrlString(port));
        if (updateAvailable)  outstr+= "<br><br>"+"Update available (relaunch Firefly to finish update)";
        if (!initComplete)outstr+= "<br><br>"+"Server Initializing...";
        aboutLabel.setText(outstr);
        aboutLabel.setToolTipText(outstr);
        if (aboutItem!=null) {
            aboutItem.setLabel(initComplete ? "About Firefly" : "About Firefly (initializing...)");
        }
    }

    public static void showAboutDialog(int port, JFrame frame) {
        if (aboutLabel==null) return;
        SwingUtilities.invokeLater(() -> {

            updateAboutLabel(port);

            JDialog aboutDialog = new JDialog(frame, "About Firefly", true);
            aboutDialog.setLayout(new BorderLayout());

            aboutLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            aboutDialog.add(aboutLabel, BorderLayout.CENTER);

            aboutDialog.pack();
            aboutDialog.setSize(450, 150);
            aboutDialog.setLocationRelativeTo(null); // Center on screen
            aboutDialog.setAlwaysOnTop(true);
            aboutDialog.setVisible(true);
        });
    }



    public static boolean isRunning(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return false; // Port is available
        } catch (IOException e) {
            return true; // Port is in use
        }
    }

    public static void hideSplash() {
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) splash.close();
    }

    public static void setupTray(Tomcat tomcat, int port, File pidTextOutFile, File readyTextOutFile) {
        System.setProperty("apple.awt.enableTemplateImages", "false");
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported on this platform.");
            return;
        }
        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(dockIconFile.getAbsolutePath());

        var dummyAnchor = new JFrame();
        dummyAnchor.setType(Window.Type.UTILITY);
        dummyAnchor.setUndecorated(true);
        dummyAnchor.setSize(1, 1);
        dummyAnchor.setLocationRelativeTo(null);


        // Create a popup menu for the icon
        PopupMenu popup = new PopupMenu();
        MenuItem exitItem = new MenuItem("Shutdown Firefly Server");
        aboutItem = new MenuItem("About Firefly (initializing...)");
        MenuItem openInBrowser = new MenuItem("Open in Browser: " + makeUrlString(port));
        popup.add(openInBrowser);
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(exitItem);
        aboutItem.addActionListener(e -> showAboutDialog(port, dummyAnchor) );
        TrayIcon trayIcon = new TrayIcon(image, "Firefly Server", popup);
        trayIcon.setImageAutoSize(true); // Automatically scale the image
        openInBrowser.addActionListener(e -> openBrowser(port, false));
        exitItem.addActionListener(e -> stopFireflyServer(tomcat, pidTextOutFile, readyTextOutFile));

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("TrayIcon could not be added.");
        }
    }


    public static void fireflyReadyMessage(int port, File readyTextOutFile) throws IOException {
        terminalOut.println("\n---------------------------------");
        terminalOut.println("Firefly ready: use URL: " + makeUrlString(port));
        terminalOut.println("---------------------------------\n");
        if (readyTextOutFile!=null) FileUtil.writeStringToFile(readyTextOutFile,"TRUE");
    }

    public static void main(String[] args) {
        try {
            FireflyApplication.start();
        } catch (Exception e) {
            terminalOut.println("Error starting Firefly Application: " + e.getMessage());
            e.printStackTrace();
        }
        Runtime.getRuntime().halt(0);
    }
}

