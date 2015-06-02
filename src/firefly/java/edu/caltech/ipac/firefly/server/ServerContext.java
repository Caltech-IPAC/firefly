/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.filters.CommonFilter;
import edu.caltech.ipac.firefly.server.query.SearchProcessorFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheManager;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A static server-centric class used hold server related information.
 *
 * Date: Jul 25, 2008
 * @author loi
 * @version $Id: ServerContext.java,v 1.26 2012/10/19 23:02:33 tatianag Exp $
 */
public class ServerContext {
    public static final String CACHE_DIR_PREFIX       = "${cache-dir}";
    public static final String UPLOAD_DIR_PREFIX      = "${upload-dir}";
    public static final String USER_IMAGES_DIR_PREFIX = "${user-images-dir}";
    public static final String USER_BASE_DIR_PREFIX   = "${user-base-dir}";
    public static final String SEARCH_PATH_PREFIX     = "${search-path}";
    public static final String STAGE_PATH_PREFIX      = "${stage}";
    public static final String PERM_FILES_PATH_PREFIX = "${perm-files}";
    public static final String IRSA_ROOT_PATH_PREFIX =  "${irsa-root-dir}";
    public static final String TEMP_FILES_PATH_PREFIX = "${temp-files}";
    public static final String WEBAPP_ROOT =            "${webapp-root}";
    private static final String VIS_DIR_STR= "visualize";
    private static final String CACHE_DIR_STR= "fits-cache";
    private static final String UPLOAD_DIR_STR= "fits-upload";
    private static final String USERS_BASE_DIR_STR= "users";
    public static final String PFX_START= "${";
    private static final int PFX_START_LEN= PFX_START.length();
    public static final String PFX_END= "}";
    private static final int PFX_TOTAL_CHAR= 3;
    public static final boolean FITS_SECURITY = AppProperties.getBooleanProperty("visualize.fits.Security", true);
    public static final String VIS_SEARCH_PATH= "visualize.fits.search.path";
    public final static Map<File, Long> _visSessionDirs= new ConcurrentHashMap<File, Long>(617);
    public static boolean DEBUG_MODE;
    public static final String CONFIG_DIR = "server_config_dir";
    public static final String CACHEMANAGER_DISABLED_PROP = "CacheManager.disabled";
    private static final String WORK_DIR_PROP = "work.directory";


    private static RequestOwnerThreadLocal owner = new RequestOwnerThreadLocal();
    private static String appName;
    private static File workingDir;
    private static File appConfigDir;
    private static File webappConfigDir;
    private static File[] visSearchPath = null;


    private volatile static String PERM_FILE_PATH_STR;
    private volatile static String TEMP_FILE_PATH_STR;
    private volatile static String STAGE_FILE_PATH_STR;
    private volatile static String VIS_UPLOAD_PATH_STR;
    private volatile static String CACHE_PATH_STR;
    private volatile static String IRSA_ROOT_PATH_STR;



    private static final Logger.LoggerImpl log= Logger.getLogger();

    public static void init() {
        
        configInit();

        initVisSearchPath();

        PERM_FILE_PATH_STR = getPermWorkDir().getPath();
        TEMP_FILE_PATH_STR = getTempWorkDir().getPath();
        STAGE_FILE_PATH_STR = getStageWorkDir().getPath();
        VIS_UPLOAD_PATH_STR = getVisUploadDir().getPath();
        CACHE_PATH_STR = getVisCacheDir().getPath();
        IRSA_ROOT_PATH_STR = getIrsaRoot().getPath();

        VisContext.init();

        // initialize search processors
        SearchProcessorFactory.init();
    }

    public static void configInit() {
        // load configurational properties...
        Assert.setServerMode(true);
        String configDirname = System.getProperty(CONFIG_DIR, System.getenv(CONFIG_DIR));
        String webappConfigDirname = System.getProperty(CommonFilter.WEBAPP_CONFIG_DIR);
        appName = AppProperties.getProperty(CommonFilter.APP_NAME, System.getProperty(CommonFilter.APP_NAME));
        configDirname = configDirname + "/" + appName;

        if (StringUtils.isEmpty(appName)) {
            String errmsg = CommonFilter.APP_NAME + " is not setup correctly.  System will not function properly";
            throw new RuntimeException(errmsg);
        };

        // load properties from WEBAPP_CONFIG_DIR.
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
        CacheManager.setDisabled(AppProperties.getBooleanProperty(CACHEMANAGER_DISABLED_PROP, false));

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


    static private void initVisSearchPath() {
        String path= AppProperties.getProperty(VIS_SEARCH_PATH, null);
        if (StringUtils.isEmpty(path)) {
            ServerContext.visSearchPath = new File[0];
            log.warn("There is no property " + VIS_SEARCH_PATH+ " defined.",
                      "This property is for security and is required to access fits files off the local server disk.",
                      "When defined this property is a list of top level directories where local fits files can be loaded from.",
                      VIS_SEARCH_PATH + " is defined in the app.prop file in the config directory specified at startup time.",
                      "Example: ",
                      "    "+VIS_SEARCH_PATH+"=/my/directory:/another/dir:/a/b/c:/local/fits/files");
        }
        else {
            path= StringUtils.crunch(path);
            String sep= System.getProperty("path.separator");
            String pathAry[]= path.split(sep);
            ServerContext.visSearchPath = new File[pathAry.length];
            for(int i=0; (i<pathAry.length); i++) {
                ServerContext.visSearchPath[i]= new File(pathAry[i]);
            }
            List<String> logList= new ArrayList<String>(10);
            logList.add(VIS_SEARCH_PATH + " loaded: ");
            logList.addAll(Arrays.asList(pathAry));
            logList.add("All local loaded FITS files must resides in these directories or sub-directories");
            log.info(logList.toArray(new String[logList.size()]));
        }

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

    public static File getWebappConfigDir() {
        return webappConfigDir;
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

    //====================================================================
    //  Factory methods for RequestAgent
    //====================================================================
    public static final RequestAgent getHttpRequestAgent(HttpServletRequest request, HttpServletResponse response) {
        // this is an abstraction point.  this class can be loaded from configuration.
        return new RequestAgent.HTTP(request, response);
    }

    public static final RequestAgent getWsRequestAgent(HandshakeRequest request, HandshakeResponse response) {
        // this is an abstraction point.  this class can be loaded from configuration.
        if (request instanceof HttpServletRequest) {
            return new RequestAgent.HTTP((HttpServletRequest) request, (HttpServletResponse) response);
        } else {
            return null;
        }
    }

    //====================================================================
    //
    //====================================================================


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

    public static File convertToFile(String in) {
        return convertToFile(in,false);
    }

    public static File convertToFile(String in, boolean matchOutsideOfPath) {
        File retval= null;
        if (in!=null) {
            int prefixEnd= in.indexOf(PFX_END);
            if (in.startsWith(PFX_START) && prefixEnd>0) {
                String prefix= in.substring(0,prefixEnd+1);
                int subStart= in.charAt(prefixEnd+1)==File.separatorChar ?
                                       prefixEnd+2 : prefixEnd+1 ;
                String relFile= in.substring(subStart);

                if (prefix.equals(CACHE_DIR_PREFIX)) {
                    retval= new File(getVisCacheDir(), relFile);
                }
                else if (prefix.equals(UPLOAD_DIR_PREFIX)) {
                    retval= new File(getVisUploadDir(), relFile);
                }
                else if (prefix.equals(USER_IMAGES_DIR_PREFIX)) {
                    retval= new File(getVisSessionDir(), relFile);
                }
                else if (prefix.equals(USER_BASE_DIR_PREFIX)) {
                    retval= new File(getUsersBaseDir(), relFile);
                }
                else if (prefix.equals(STAGE_PATH_PREFIX)) {
                    retval= new File(getStageWorkDir(), relFile);
                }
                else if (prefix.equals(PERM_FILES_PATH_PREFIX)) {
                    retval= new File(getPermWorkDir(), relFile);
                }
                else if (prefix.equals(TEMP_FILES_PATH_PREFIX)) {
                    retval= new File(getTempWorkDir(), relFile);
                }
                else if (prefix.equals(IRSA_ROOT_PATH_PREFIX)) {
                    retval= new File(getIrsaRoot(), relFile);
                }
                else if (prefix.equals(WEBAPP_ROOT)) {
                    String rp= getRequestOwner().getRequestAgent().getRealPath(relFile);
                    retval= new File(rp);
                }
                else if (prefix.equals(SEARCH_PATH_PREFIX)) {
                    retval= findFileInPath(relFile);
                }
                else if (!StringUtils.isEmpty(prefix)) {
                    retval=findFile(prefix,relFile);
                }
            }
            else if (in.charAt(0)==File.separatorChar) {
                retval= validateFileInPath(in,!matchOutsideOfPath);
            }
            else {
                retval= new File(getVisUploadDir(), in);
            }
        }
        return retval;
    }

    public static File findFile(String prefix, String relFile) {
        File retval= null;
        if (prefix.startsWith(PFX_START) && prefix.endsWith(PFX_END) && prefix.length()>PFX_TOTAL_CHAR) {
            String prop= prefix.substring(PFX_START_LEN,prefix.length()-1);
            String root= AppProperties.getProperty(prop);
            if (!StringUtils.isEmpty(root)) {
                File dir= new File(root);
                if (dir.canRead()) retval= new File(dir, relFile);
            }
        }
        return retval;
    }

    private static File findFileInPath(String relativeStr) {
        File foundFile= null;
        if (visSearchPath.length>0) {
            File f;
            for(File froot : visSearchPath) {
                f= new File(froot,relativeStr);
                if (f.canRead()) {
                    foundFile= f;
                    break;
                }
            }
        }
        return foundFile;
    }

    public static boolean isFileInPath(File f) { return validateFileInPath(f.getPath(), true)!=null; }

    public static File validateFileInPath(String fileStr, boolean useSecurity) {
        useSecurity= useSecurity && FITS_SECURITY;
        File foundFile= null;
        if (visSearchPath.length>0) {
            for(File fRoot : visSearchPath) {
                if (fileStr.startsWith(fRoot.getPath())) {
                    foundFile= new File(fileStr);
                    break;
                }
            }
            if (foundFile==null) {
                if (fileStr.startsWith(getVisCacheDir().getPath()) ||
                    fileStr.startsWith(getVisUploadDir().getPath()) ||
                    fileStr.startsWith(getVisSessionDir().getPath()) ||
                    fileStr.startsWith(getUsersBaseDir().getPath())   ||
                    fileStr.startsWith(getPermWorkDir().getPath())   ||
                    fileStr.startsWith(getTempWorkDir().getPath())   ||
                    fileStr.startsWith(getStageWorkDir().getPath()) )  {
                    foundFile= new File(fileStr);
                }
            }
        }
        if (foundFile==null && !useSecurity) {
            File f= new File(fileStr);
            foundFile= f.canRead() ? f : null;
        }
        return foundFile;
    }

    public static File getVisWorkingDir() {
        File dir= getWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the working dir");
        File visDir= new File(dir,VIS_DIR_STR);
        if (!visDir.exists()) makeDirs(visDir);
        return visDir;
    }

    public static File getVisCacheDir() {
        File dir= getVisWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the vis cache dir");
        File cacheDir= new File(dir,CACHE_DIR_STR);
        if (!cacheDir.exists()) makeDirs(cacheDir);
        return cacheDir;
    }

    public static File getVisUploadDir() {
        File dir= getVisWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the vis cache dir");
        File cacheDir= new File(dir,UPLOAD_DIR_STR);
        if (!cacheDir.exists()) makeDirs(cacheDir);
        return cacheDir;
    }

    public static File getUsersBaseDir() {
        File dir= getVisWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the vis users base dir");
        File userBaseDir= new File(dir, USERS_BASE_DIR_STR);
        if (!userBaseDir.exists()) makeDirs(userBaseDir);
        return userBaseDir;
    }

    public static File getVisSessionDir() {
        File dir= getUsersBaseDir();
        Assert.argTst(dir.canWrite(), "can't write to the users image working dir");
        RequestOwner owner= getRequestOwner();
        String dirStr= owner.getUserKey();
        File userDir= new File(dir, dirStr);
        if (!userDir.exists()) {
            makeDirs(userDir);
            _visSessionDirs.put(userDir,System.currentTimeMillis());
        }
        return userDir;
    }

    private static void makeDirs(File dir) {
        boolean success= dir.mkdirs();
        if (!success || !dir.exists()) {
            Logger.getLogger().error("failed to create " +dir.getPath());
        }
    }

    public static String replaceWithPrefix(File f) {
        if (f==null) return null;
        String retval= null;
        String path= f.getPath();
        String userImageDir= getVisSessionDir().getPath();

        String userBaseDir= getUsersBaseDir().getPath();
        if (path.startsWith(CACHE_PATH_STR)) {
            retval= replacePrefix(path, CACHE_PATH_STR, CACHE_DIR_PREFIX);
        }
        else if (path.startsWith(userImageDir)) {
            retval= replacePrefix(path,userImageDir, USER_IMAGES_DIR_PREFIX);
        }
        else if (path.startsWith(VIS_UPLOAD_PATH_STR)) {
            retval= replacePrefix(path, VIS_UPLOAD_PATH_STR, UPLOAD_DIR_PREFIX);
        }
        else if (path.startsWith(PERM_FILE_PATH_STR)) {
            retval= replacePrefix(path, PERM_FILE_PATH_STR, PERM_FILES_PATH_PREFIX);
        }
        else if (path.startsWith(TEMP_FILE_PATH_STR)) {
            retval= replacePrefix(path, TEMP_FILE_PATH_STR, TEMP_FILES_PATH_PREFIX);
        }
        else if (path.startsWith(STAGE_FILE_PATH_STR)) {
            retval= replacePrefix(path, STAGE_FILE_PATH_STR, STAGE_PATH_PREFIX);
        }
        else if (path.startsWith(IRSA_ROOT_PATH_STR)) {
            retval= replacePrefix(path, IRSA_ROOT_PATH_STR, IRSA_ROOT_PATH_PREFIX);
        }
        else if (path.startsWith(userBaseDir)) {
            retval= replacePrefix(path,userBaseDir, USER_BASE_DIR_PREFIX);
        }
        else if (!FITS_SECURITY && path.charAt(0)==File.separatorChar) {
            retval= path;
        }

        else { // search path
            String pathDir;
            for(File searchF : visSearchPath) {
                pathDir= searchF.getPath();
                if (path.startsWith(pathDir)) {
                    retval= replacePrefix(path,pathDir, SEARCH_PATH_PREFIX);
                    break;
                }
            }
        }
        return retval;
    }

    public static String replaceWithPrefix(File f, String propertyName) {
        String retval= null;
        String pValue= AppProperties.getProperty(propertyName);
        if (pValue!=null) retval= replaceWithPrefix(f,propertyName,pValue);
        return retval;

    }

    public static String replaceWithPrefix(File f, String propertyName, String propertyValue ) {
        return replacePrefix(f.getPath(),propertyValue, PFX_START +propertyName+ PFX_END);
    }

    public static String replaceWithUsersBaseDirPrefix(File f) {
        String path= f.getPath();
        String userBaseDir= getUsersBaseDir().getPath();
        String retval= null;
        if (path.startsWith(userBaseDir)) {
            retval= replacePrefix(path,userBaseDir, USER_BASE_DIR_PREFIX);
        }
        return retval;
    }

    private static String replacePrefix(String path,
                                        String oldDir,
                                        String newPrefix) {
        String retval;
        String strip= path.substring(oldDir.length());
        if (strip.startsWith("/")) {
            retval= newPrefix + strip;
        }
        else {
            retval= newPrefix + "/" + strip;
        }
        return retval;

    }

    public static boolean isInUploadDir(File f) {
        return (f!=null &&f.getPath().startsWith(VIS_UPLOAD_PATH_STR));
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
            return new RequestOwner(null, new Date());
        }

        @Override
        protected RequestOwner childValue(RequestOwner requestOwner) {
            try {
                return (RequestOwner) requestOwner.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }


}
