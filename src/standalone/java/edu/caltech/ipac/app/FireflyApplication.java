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
    private final static File pidTextOutFile= new File(ffDir, "pid.txt");
    private final static File fireflyPortTextOutFile= new File(ffDir, "port.txt");
    private static final boolean useLogFile= true;
    private static final int DEFAULT_FIREFLY_PORT = 8888;
    private static String fireflyVersion;
    private static String javaVersion;
    private static boolean updateAvailable=  false;
    private static PrintStream terminalOut= System.out;
    private static boolean initComplete= false;
    private static JLabel aboutLabel= null; // only used in desktop mode
    private static MenuItem aboutItem = null; // only used in desktop mode
    private static boolean firstUpdateCheck= true;

    public record Ports(int fireflyPort, int redisPort) {}


    public static void start() throws LifecycleException, URISyntaxException, IOException, InterruptedException {
        fireflyVersion= saveVersion();
        javaVersion = System.getProperty("java.version");
        boolean useDesktop= AppProperties.getBooleanProperty("runAsDesktopApplication", false);
        ensureFireflyDir();
        var ports= getPorts();
        var fireflyPort= ports.fireflyPort;
        File readyTextOutFile= new File(ffDir, "ready-"+fireflyPort+".txt");

        if (useDesktop) SwingUtilities.invokeLater(() -> initAboutLabel(fireflyPort));


        if (useLogFile) setupLogger();

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(tomcatDir.getAbsolutePath());
        tomcat.setPort(fireflyPort);


        boolean tomcatStarted = false;
        if (!isRunning(fireflyPort)) {
            var ignore= readyTextOutFile.delete();
            if (useDesktop) setupUI(tomcat, ports, readyTextOutFile);
            tomcat.addUser("admin", "admin");
            tomcat.addWebapp("/firefly", fireflyWarDir.getAbsolutePath());
            terminalOut.println("Firefly server starting (is takes a few seconds)...");
            Connector connector= tomcat.getConnector();
            connector.setPort(fireflyPort);
            connector.setProperty("compression", "on");
            connector.setProperty("useSendfile", "false");
            connector.setProperty("compressibleMimeType", compressibleMimeType);
            tomcat.start();
            saveInfo(fireflyPort);
            tomcatStarted = true;
        }

        if (!tomcatStarted) {
            terminalOut.println("Firefly is already running");
            openBrowser(fireflyPort,true);
            fireflyReadyMessage(fireflyPort, null);
            System.exit(0);
        }


        initComplete= true;
        if (useDesktop) {
            hideSplash();
            SwingUtilities.invokeLater(() -> updateAboutLabel(ports));
            openBrowser(fireflyPort,true);
        }
        fireflyReadyMessage(fireflyPort, readyTextOutFile);
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
                        urlStr= (String)asset.get("browser_download_url");
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
        if (currVer==null) currVer= "0.0.0";
        if (availableVer==null) availableVer= "0.0.0";
        var cVer= currVer.split("\\.");
        var nVer= availableVer.split("\\.");
        if (cVer.length!=3 || nVer.length!=3) return false;
        var curr= Arrays.stream(cVer).map((s) -> StringUtils.getInt(s,0)).toList();
        var next= Arrays.stream(nVer).map((s) -> StringUtils.getInt(s,0)).toList();
        if (next.get(0)>curr.get(0)) return true;
        else if (next.get(0).equals(curr.get(0))) {
            if (next.get(1)>curr.get(1)) return true;
            else if (next.get(1).equals(curr.get(1))) {
                return next.get(2)>curr.get(2);
            }
        }
        return false;
    }


    public static void saveInfo(int fireflyPort) {
        FileUtil.writeStringToFile(pidTextOutFile,ProcessHandle.current().pid()+"");
        FileUtil.writeStringToFile(fireflyPortTextOutFile,fireflyPort+"");
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
        pb.redirectErrorStream(true);
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
        pb.redirectErrorStream(true);
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

    public static Ports getPorts() {
        int unknownRedisPort=0;
        int runningPort= AppProperties.getIntProperty("firefly.port",0);
        var defaultPorts= new Ports(runningPort!=0 ? runningPort : DEFAULT_FIREFLY_PORT, unknownRedisPort);
        try {
            if (!configFile.canRead()) return defaultPorts;
            String pStr= FileUtil.readFile(configFile);
            if (pStr==null) return defaultPorts;
            var obj= (JSONObject) new JSONParser().parse(pStr);
            var ports= (JSONObject)obj.get("ports");
            if (ports==null) return defaultPorts;
            int fireflyPort;
            if (runningPort!=0) {
                fireflyPort= runningPort;
            }
            else {
                Long fireflyPortJson= (Long)ports.get("firefly");
                fireflyPort= (fireflyPortJson!=null) ? fireflyPortJson.intValue() : 0;
            }
            if (fireflyPort==0) return defaultPorts;
            Long redisPortJson= (Long)ports.get("redis");
            int redisPort= (redisPortJson!=null)  ? redisPortJson.intValue() : unknownRedisPort;
            return new Ports(fireflyPort,redisPort);
        } catch (IOException | NumberFormatException | ParseException e) {
            return defaultPorts;
        }
    }

    private static void confirmDirOrExit(File dir) {
        boolean exists = true;
        if (!dir.exists()) {
            exists = dir.mkdir();
        }
        if (!exists || !dir.canWrite()) {
            System.out.println("Can't write to " + dir.getAbsolutePath() + " directory");
            System.exit(1);
        }
    }

    private static void setupUI(Tomcat tomcat, Ports ports, File readyTextOutFile) {
        System.setProperty("apple.awt.UIElement", "true");
//      setupDock(port);
        setupTray(tomcat, ports, readyTextOutFile);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                   try {
                       if (tomcat.getServer().getState().isAvailable()) {
                           System.out.println("Shutting down Firefly server...");
                           tomcat.stop();
                           tomcat.destroy();
                           var ignore= pidTextOutFile.delete();
                           ignore= fireflyPortTextOutFile.delete();
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

    public static void stopFireflyServer(Tomcat tomcat, File readyTextOutFile) {
        try {
            if (tomcat.getServer().getState().isAvailable()) {
                System.out.println("Shutting down Firefly server...");
                tomcat.stop();
                tomcat.destroy();
                var ignore= pidTextOutFile.delete();
                ignore= fireflyPortTextOutFile.delete();
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

    public static void updateAboutLabel(Ports ports) {
        if (aboutLabel==null) return;
        var workDir= tomcatDir.getPath() +"/workarea";
        var fireflyPort= ports.fireflyPort;
        var portStr= String.format("Ports: Firefly %d, Redis %d, debug (if started with -d) 5005", ports.fireflyPort, ports.redisPort);
        String outstr= String.format(
                "<html>Firefly Version: %s<br>Java Version: %s<br>%s<br>pid: %s<br>Work dir: %s<br>Log dir: %s<br><br>",
                fireflyVersion, javaVersion,portStr,ProcessHandle.current().pid()+"",workDir,tomcatLogs.getPath());
        outstr+= String.format("To load Firefly: <a href=\"%s\">%s</a>",makeUrlString(fireflyPort), makeUrlString(fireflyPort));
        if (updateAvailable)  outstr+= "<br><br>"+"Update available (relaunch Firefly to finish update)";
        if (!initComplete)outstr+= "<br><br>"+"Server Initializing...";
        aboutLabel.setText(outstr);
        aboutLabel.setToolTipText(outstr);
        if (aboutItem!=null) {
            aboutItem.setLabel(initComplete ? "About Firefly" : "About Firefly (initializing...)");
        }
    }

    public static void showAboutDialog(Ports ports, JFrame frame) {
        if (aboutLabel==null) return;
        SwingUtilities.invokeLater(() -> {

            updateAboutLabel(ports);

            JDialog aboutDialog = new JDialog(frame, "About Firefly", true);
            aboutDialog.setLayout(new BorderLayout());

            aboutLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            aboutDialog.add(aboutLabel, BorderLayout.CENTER);

            aboutDialog.pack();
            aboutDialog.setSize(550, 200);
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

    public static void setupTray(Tomcat tomcat, Ports ports, File readyTextOutFile) {
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
        MenuItem openInBrowser = new MenuItem("Open in Browser: " + makeUrlString(ports.fireflyPort));
        popup.add(openInBrowser);
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(exitItem);
        aboutItem.addActionListener(e -> showAboutDialog(ports, dummyAnchor) );
        TrayIcon trayIcon = new TrayIcon(image, "Firefly Server", popup);
        trayIcon.setImageAutoSize(true); // Automatically scale the image
        openInBrowser.addActionListener(e -> openBrowser(ports.fireflyPort, false));
        exitItem.addActionListener(e -> stopFireflyServer(tomcat, readyTextOutFile));

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
            Runtime.getRuntime().halt(1);
        }
        Runtime.getRuntime().halt(0);
    }
}

