package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.filters.CommonFilter;
import edu.caltech.ipac.firefly.server.query.SearchProcessorFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheManager;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Date;

/**
 * A static server-centric class used hold server related information.
 *
 * Date: Jul 25, 2008
 * @author loi
 * @version $Id: ServerContext.java,v 1.26 2012/10/19 23:02:33 tatianag Exp $
 */
public class ServerContext {
    public static boolean DEBUG_MODE;
    public static final String CONFIG_DIR = "server_config_dir";
    public static final String CACHEMANAGER_DISABLED_PROP = "CacheManager.disabled";
    private static final String WORK_DIR_PROP = "work.directory";

    private static RequestOwnerThreadLocal owner = new RequestOwnerThreadLocal();
    private static String appName;
    private static File workingDir;
    private static File appConfigDir;
    private static File webappConfigDir;



    public static void init() {
        
        configInit();

        VisContext.init();

        // initialize search processors
        SearchProcessorFactory.init();
    }

    public static void configInit() {
        // load configurational properties...
        Assert.setServerMode(true);
        String configDirname = System.getProperty(CONFIG_DIR, System.getenv(CONFIG_DIR));
        String webappConfigDirname = System.getProperty(CommonFilter.WEBAPP_CONFIG_DIR);
        appName = AppProperties.getPreference(CommonFilter.APP_NAME, System.getProperty(CommonFilter.APP_NAME));
        configDirname = configDirname + "/" + appName;

        if (StringUtils.isEmpty(appName)) {
            String errmsg = CommonFilter.APP_NAME + " is not setup correctly.  System will not function properly";
            throw new RuntimeException(errmsg);
        };

        // load properties from WEBAPP_CONFIG_DIR.
            // in some OS.. # is given as %23.
            webappConfigDirname =  webappConfigDirname.replaceAll("%23", "#");
        if (loadProperties(webappConfigDirname)) {
            webappConfigDir = new File(webappConfigDirname);
        }

        // load properties from CONFIG_DIR
        if (loadProperties(configDirname)) {
            appConfigDir = new File(configDirname);
        }

        if (webappConfigDir == null && appConfigDir == null) {
            String errmsg = CONFIG_DIR + " is not setup correctly.  System will not function properly";
            throw new RuntimeException(errmsg);
        }

        // initializes log4j
        File cfg = getConfigFile("log4j.properties");
        if (cfg.canRead()) {
            System.out.println("Initializing Log4J using file:" + cfg.getAbsolutePath());
            PropertyConfigurator.configureAndWatch(cfg.getAbsolutePath());
        }

        // setup ClientLog and Assert to use firefly logging.
        Assert.setLogger(new AssertLogger());
        ClientLog.setLogger(new Logger.ClientLogImpl());


        // load resource files
        String resdir = AppProperties.getProperty("web.properties.dir", "/resources");
        boolean loadAllClientProperties= AppProperties.getBooleanProperty("properties.loadAllClientProperties",true);
        WebPropertyLoader.loadAllProperties(resdir, loadAllClientProperties);

        // disable caching is it's a preference
        CacheManager.setDisabled(AppProperties.getBooleanPreference(CACHEMANAGER_DISABLED_PROP, false));

        // use EhCache for caching.
        CacheManager.setCacheProvider(EhcacheProvider.class.getName());

        // setup working area
        String workDirRoot = AppProperties.getProperty(WORK_DIR_PROP);
        File f = StringUtils.isEmpty(workDirRoot) ? null : new File(workDirRoot);
        if (f == null || !f.canWrite()) {
            f = new File(System.getProperty("java.io.tmpdir"),"workarea");
        }
        setWorkingDir(new File(f, appName));

        DEBUG_MODE = AppProperties.getBooleanProperty("debug.mode", false);

        Logger.info("CACHE_PROVIDER = " + EhcacheProvider.class.getName(),
                "WORK_DIR = " + ServerContext.getWorkingDir(),
                "DEBUG_MODE = " + DEBUG_MODE);
    }

    /**
     * return the configure file from the designated configuration directory.
     * @param fname  a relative path file name from the config directory
     * @return
     */
    public static File getConfigFile(String fname) {
        
        File f = null;
        if (appConfigDir != null) {
            f = new File(appConfigDir, fname) ;
        }
        if (f == null || !f.canRead()) {
            f = new File(webappConfigDir, fname);
        }
        if (f == null || !f.canRead()) {
            return null;
        } else {
            return f;
        }
    }

    public static long getConfigLastModTime() {
        long appTime = appConfigDir == null ? 0 : appConfigDir.lastModified();
        long webTime = webappConfigDir == null ? 0 : webappConfigDir.lastModified();
        return Math.max(appTime, webTime);
    }


    private static boolean loadProperties(String dirName) {
        if (dirName == null) return false;

        boolean hasProps = false;
        File dir = new File(dirName);
        if (dir.canRead()) {
            File[] props = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".prop") || name.endsWith(".properties");
                }
            });
            if (props != null) {
                for (File f : props) {
                    try {
                        AppProperties.addApplicationProperties(f, true);
                        hasProps = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return hasProps;
    }

    public static String getAppName() {
        return appName;
    }

    public static void clearRequestOwner() {
        if (owner != null) owner.remove();
    }

    public static RequestOwner getRequestOwner() {
        return owner.get();
    }

    public static File getWorkingDir() {
        initDir(workingDir);
        return workingDir;
    }

    public static void setWorkingDir(File workDir) {
        workingDir = workDir;
    }

//    public static File getAppConfigDir() {
//        initDir(appConfigDir);
//        return appConfigDir;
//    }
//
    public static File getPermWorkDir() {
        File dir = new File(getWorkingDir(), "perm_files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getTempWorkDir() {
        File dir = new File(getWorkingDir(), "temp_files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File getStageWorkDir() {
        File dir = new File(getWorkingDir(), "stage");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static void initDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static File getIrsaRoot() {
        File dir = new File(getWorkingDir(), "irsa-root");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }



    private static class AssertLogger implements Assert.Logger {
        private final Logger.LoggerImpl _log= Logger.getLogger();
        public void log(String... messages) { _log.warn(messages); }
    }

    /**
     * this ThreadLocal will attempt to make a snapshot of the parent's value when
     * a child thread is created instead of relying on the parent's value.
     */
    static class RequestOwnerThreadLocal extends InheritableThreadLocal<RequestOwner> {
        @Override
        protected RequestOwner initialValue() {
            return new RequestOwner(String.valueOf(hashCode()), null, new Date());
        }

        @Override
        protected RequestOwner childValue(RequestOwner requestOwner) {
            RequestOwner ro = super.childValue(requestOwner);
            try {
                return (RequestOwner) ro.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
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