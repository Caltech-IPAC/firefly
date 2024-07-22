/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import com.sun.management.OperatingSystemMXBean;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheManager;
import nom.tam.fits.FitsFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A static server-centric class used hold server related information.
 *
 * Date: Jul 25, 2008
 * @author loi
 * @version $Id: ServerContext.java,v 1.26 2012/10/19 23:02:33 tatianag Exp $
 */
public class ServerContext {
    public static final ExecutorService SHORT_TASK_EXEC = Executors.newCachedThreadPool();        // an expandable thread pools.. for short tasks.
    public static final ScheduledExecutorService SCHEDULE_TASK_EXEC = Executors.newSingleThreadScheduledExecutor();


    private static final String HIPS_DIR_PREFIX        = "${hips-dir}";
    private static final String CACHE_DIR_PREFIX       = "${cache-dir}";
    private static final String UPLOAD_DIR_PREFIX      = "${upload-dir}";
    private static final String USER_IMAGES_DIR_PREFIX = "${user-images-dir}";
    private static final String USER_BASE_DIR_PREFIX   = "${user-base-dir}";
    private static final String SEARCH_PATH_PREFIX     = "${search-path}";
    private static final String STAGE_PATH_PREFIX      = "${stage}";
    private static final String PERM_FILES_PATH_PREFIX = "${perm-files}";
    private static final String IRSA_ROOT_PATH_PREFIX  =  "${irsa-root-dir}";
    private static final String TEMP_FILES_PATH_PREFIX = "${temp-files}";
    private static final String WEBAPP_ROOT =            "${webapp-root}";
    private static final String VIS_DIR_STR= "visualize";
    private static final String CACHE_DIR_STR= "fits-cache";
    private static final String UPLOAD_DIR_STR= "upload";
    private static final String USERS_BASE_DIR_STR= "users";
    private static final String PFX_START= "${";
    private static final int PFX_START_LEN= PFX_START.length();
    private static final String PFX_END= "}";
    private static final int PFX_TOTAL_CHAR= 3;
    private static boolean FITS_SECURITY;
    private static final Map<File, Long> _visSessionDirs= new ConcurrentHashMap<>(617);
    private static final String CONFIG_DIR = "server_config_dir";
    private static final String WORK_DIR_PROP = "work.directory";
    private static final String SHARED_WORK_DIR_PROP= "shared.work.directory";
    public static final String VIS_SEARCH_PATH= "visualize.fits.search.path";
    public static final String STATS_LOG_DIR= "stats.log.dir";


    private static RequestOwnerThreadLocal owner = new RequestOwnerThreadLocal();
    private static String webappConfigPath;
    private static String contextName;      // synonymous to appName.. during build, we set display-name to app_name
    private static String contextPath;
    private static File workingDir;
    private static File sharedWorkingDir= null;
    private static File appConfigDir;
    private static File webappConfigDir;
    private static File[] visSearchPath = null;
    private static boolean isInit = false;

    private volatile static String HIPS_FILE_PATH_STR;
    private volatile static String PERM_FILE_PATH_STR;
    private volatile static String TEMP_FILE_PATH_STR;
    private volatile static String STAGE_FILE_PATH_STR;
    private volatile static String VIS_UPLOAD_PATH_STR;
    private volatile static String CACHE_PATH_STR;
    private volatile static String IRSA_ROOT_PATH_STR;

    public static final String ACCESS_TEST_EXT= "access.test";


    private static final Logger.LoggerImpl log= Logger.getLogger();

    public static void init(String contextPath, String contextName, String webappConfigPath) {
        if (!isInit) {
            isInit = true;
            ServerContext.contextPath = contextPath;
            ServerContext.webappConfigPath = webappConfigPath;
            ServerContext.contextName = contextName;

            configInit();

            initVisSearchPath();

            PERM_FILE_PATH_STR = getPermWorkDir().getPath();
            TEMP_FILE_PATH_STR = getTempWorkDir().getPath();
            STAGE_FILE_PATH_STR = getStageWorkDir().getPath();
            VIS_UPLOAD_PATH_STR = getUploadDir().getPath();
            CACHE_PATH_STR = getVisCacheDir().getPath();
            IRSA_ROOT_PATH_STR = getIrsaRoot().getPath();
            HIPS_FILE_PATH_STR = getHiPSDir().getPath();

            VisContext.init();

            // initialize search processors
            SearchProcessorFactory.init();

            // alerts monitoring
            if (AppProperties.getBooleanProperty("alerts.monitorAlerts",true)) AlertsMonitor.startMonitor();

            // init fits read global settting
            FitsFactory.setAllowTerminalJunk(true);
            FitsFactory.setUseHierarch(true); // this is now the default as of 1.16
            FitsFactory.setLongStringsEnabled(true);
        }
    }

    public static void configInit() {
        // load configurational properties...
        Assert.setServerMode(true);
        String configDirname = System.getProperty(CONFIG_DIR, System.getenv(CONFIG_DIR));
        configDirname = configDirname == null ? null : configDirname + "/" + contextName;

        if (StringUtils.isEmpty(contextName)) {
            String errmsg = " is not setup correctly.  System will not function properly";
            throw new RuntimeException(errmsg);
        };

        // load properties from WEBAPP_CONFIG_DIR.
        if (loadProperties(webappConfigPath)) {
            webappConfigDir = new File(webappConfigPath);
        }

        // load properties from CONFIG_DIR
        if (loadProperties(configDirname)) {
            appConfigDir = new File(configDirname);
        }

        if (webappConfigDir == null && appConfigDir == null) {
            String errmsg = CONFIG_DIR + " is not setup correctly.  System will not function properly";
            throw new RuntimeException(errmsg);
        }

        // setup ClientLog and Assert to use firefly logging.
        Assert.setLogger(new AssertLogger());

        // Must be done after property init
        FITS_SECURITY = AppProperties.getBooleanProperty("visualize.fits.Security", true);

        // use EhCache for caching.
        CacheManager.setCacheProvider(EhcacheProvider.class.getName());

        // setup working area
        File workingDirFile = setupWorkDir("Setting up " + WORK_DIR_PROP,
                                AppProperties.getProperty(WORK_DIR_PROP),
                                new File(System.getProperty("java.io.tmpdir"),"workarea"));
        if (workingDirFile != null) {
            setWorkingDir(new File(workingDirFile, contextName));
        }

        // setup shared working area
        File sharedWorkingDirFile = setupWorkDir("Setting up " + SHARED_WORK_DIR_PROP,
                AppProperties.getProperty(SHARED_WORK_DIR_PROP), workingDirFile);
        if (sharedWorkingDirFile != null) {
            setSharedWorkingDir(new File(sharedWorkingDirFile, contextName));
            try {
                File f= new File(getSharedWorkingDir(), FileUtil.getHostname()+"."+ACCESS_TEST_EXT);
                if (f.canWrite()) f.delete();
                f.createNewFile();
                f.deleteOnExit();
            } catch (IOException e) {
                Logger.error("Could not create a test file in "+ getSharedWorkingDir().toString());
            }
        }

        Logger.info("",
                "CACHE_PROVIDER : " + EhcacheProvider.class.getName(),
                "WORK_DIR       : " + getWorkingDir(),
                "Available Cores: " + getAvailableCores() );
    }

    private static File setupWorkDir(String desc, String fromProp, File altDir) {
        log.info(desc);
        if (!StringUtils.isEmpty(fromProp)) {
            log.info(desc +  "; Using path from property: " + fromProp);
            File workDir = new File(fromProp);
            initDir(workDir);
            if (workDir.canWrite()) {
                return workDir;
            }
            else {
                log.info(desc + " failed. " + " Unable to write to directory: " + workDir.getPath());
            }
        }

        if (altDir == null) {
            log.error(desc + " failed. " + " no alternate directory is given");
            return null;
        }

        // using altDir
        log.info(desc +  "; Using alternate path: " + altDir.getPath());
        initDir(altDir);
        if (altDir.canWrite()) {
            return altDir;
        } else {
            log.error(desc + " failed. " + " Unable to write to alternate directory: " + altDir.getPath());
            return null;
        }
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
            List<String> logList= new ArrayList<>(10);
            logList.add(VIS_SEARCH_PATH + " loaded: ");
            logList.addAll(Arrays.asList(pathAry));
            logList.add("All local loaded FITS files must resides in these directories or sub-directories");
            log.info(logList.toArray(new String[logList.size()]));
        }

    }




    /**
     * return the configure file from the designated configuration directory.
     * @param fname  a relative path file name from the config directory
     * @return the config file
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
            File[] props = dir.listFiles( (theDir, name) -> name.endsWith(".prop") || name.endsWith(".properties") );
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
        return contextName;
    }
    public static String getContextName() {
        return contextName;
    }
    public static String getContextPath() {
        return contextPath;
    }

    public static void clearRequestOwner() {
        if (owner != null) owner.remove();
    }

    public static RequestOwner getRequestOwner() {
         return owner.get();
    }

    public static File getWorkingDir() {
        return initDir(workingDir);
    }


    /**
     * Don't use this call, it does not guarantee a safe dir
     * @return File the original requested SharedWorkingDirRequested, may not be a good directory
     */
    public static File getSharedWorkingDirRequested() {
        String swd = AppProperties.getProperty(SHARED_WORK_DIR_PROP);
        return swd == null ? null : new File(swd);
    }

    public static File getSharedWorkingDir() {
        return initDir(sharedWorkingDir);
    }

    public static void setWorkingDir(File workDir) {
        workingDir = workDir;
    }

    public static void setSharedWorkingDir(File sharedWorkDir) {
        sharedWorkingDir = sharedWorkDir;
    }

    public static File getHiPSDir() {
        File dir = new File(getWorkingDir(), "HiPS");
        return initDir(dir);
    }

    public static File getPermWorkDir() {
        File dir = new File(getWorkingDir(), "perm_files");
        return initDir(dir);
    }

    public static File getExternalPermWorkDir() {
        File dir = new File(getPermWorkDir(), "external");
        return initDir(dir);
    }

    public static File getTempWorkDir() {
        File dir = new File(getWorkingDir(), "temp_files");
        return initDir(dir);
    }


    public static File getExternalTempWorkDir() {
        File dir = new File(getTempWorkDir(), "external");
        return initDir(dir);
    }


    public static File getStageWorkDir() {
        File dir = new File(getSharedWorkingDir(), "stage");
        return initDir(dir);
    }

    //====================================================================
    //  Factory methods for RequestAgent
    //====================================================================
    public static RequestAgent getHttpRequestAgent(HttpServletRequest request, HttpServletResponse response) {
        // this is an abstraction point.  this class can be loaded from configuration.
        return new RequestAgent.HTTP(request, response);
    }

    public static RequestAgent getWsRequestAgent(HandshakeRequest request, HandshakeResponse response) {
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


    private static File initDir(File dir) {
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getIrsaRoot() {
        File dir = new File(getWorkingDir(), "irsa-root");
        return initDir(dir);
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
                    retval= new File(getUploadDir(), relFile);
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
                else if (prefix.equals(HIPS_DIR_PREFIX)) {
                    retval= new File(getHiPSDir(), relFile);
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
                retval= new File(getUploadDir(), in);
            }
        }
        return retval;
    }

    public static int getParallelProcessingCoreCnt() {
        int cores= getAvailableCores();
        int coreCnt;
        if      (cores<4)  coreCnt= 1;
        else if (cores<=5) coreCnt= cores-1;
        else if (cores<16) coreCnt= cores-2;
        else if (cores<22) coreCnt= cores-4;
        else               coreCnt= cores-6;
        return coreCnt;
    }

    private static int getAvailableCores() {
        int availableCores= AppProperties.getIntProperty("server.cores", 0);
        if (availableCores==0) availableCores= Runtime.getRuntime().availableProcessors();
        return availableCores;
    }


    public static Object convertFilePaths(Object json) {

        // look for unresolved path prefixes in
        // org.json.simple.JSONObject, org.json.simple.JSONArray, java.lang.String
        // otherwise return unchanged object
        if (json instanceof String) {
            if (((String) json).startsWith(ServerContext.PFX_START)) {
                File f = convertToFile((String) json, false);
                if (f != null && f.exists()) {
                    return f.getAbsolutePath();
                } else {
                    return json;
                }
            } else {
                return json;
            }
        } else if (json instanceof JSONObject) {
            JSONObject obj = (JSONObject) json;
            for (Object key : obj.keySet()) {
                Object newval = convertFilePaths(obj.get(key));
                if (newval != null) {
                    obj.put(key, newval);
                }
            }
            return obj;
        } else if (json instanceof JSONArray) {
            JSONArray arr = (JSONArray) json;
            for (int i=0; i< arr.size(); i++) {
                Object newval = convertFilePaths(arr.get(i));
                if (newval != null) {
                    arr.set(i, newval);
                }
            }
            return arr;
        } else {
            return json;
        }
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
                    fileStr.startsWith(getUploadDir().getPath()) ||
                    fileStr.startsWith(getVisSessionDir().getPath()) ||
                    fileStr.startsWith(getUsersBaseDir().getPath())   ||
                    fileStr.startsWith(getPermWorkDir().getPath())   ||
                    fileStr.startsWith(getHiPSDir().getPath())   ||
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

    private static File getVisWorkingDir() {
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

    public static File getUploadDir() {
        File dir= getSharedWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to cache dir");
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
            Logger.getLogger().error("failed to create " + dir.getPath());
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
        else if (path.startsWith(HIPS_FILE_PATH_STR)) {
            retval= replacePrefix(path, HIPS_FILE_PATH_STR, HIPS_DIR_PREFIX);
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

    public static Object replaceWithPrefixes(Object json) {

        // look for unresolved path prefixes in
        // org.json.simple.JSONObject, org.json.simple.JSONArray, java.lang.String
        // otherwise return unchanged object
        if (json instanceof String && ((String)json).contains(File.separator)) {
            File f = new File((String) json);
            if (f.exists()) {
                String encodedPath = replaceWithPrefix(f);
                if (encodedPath != null) {
                    return encodedPath;
                } else {
                    return json;
                }
            } else {
                return json;
            }
        } else if (json instanceof JSONObject) {
            JSONObject obj = (JSONObject) json;
            for (Object key : obj.keySet()) {
                Object newval = replaceWithPrefixes(obj.get(key));
                if (newval != null) {
                    obj.put(key, newval);
                }
            }
            return obj;
        } else if (json instanceof JSONArray) {
            JSONArray arr = (JSONArray) json;
            for (int i=0; i< arr.size(); i++) {
                Object newval = replaceWithPrefixes(arr.get(i));
                if (newval != null) {
                    arr.set(i, newval);
                }
            }
            return arr;
        } else {
            return json;
        }
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

    /**
     * This function attempt to convert a relative URL into an absolute URL.
     * If an absolute url is given, it'll return the url as is.
     * Otherwise, it will return an absolute URL based on the given url.
     * If the given url starts with '/', then it's relative to the host, otherwise
     * it's relative to the deployed context.
     * @param url - full or "relative" url string
     * @return an absolute url
     */
    public static String resolveUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("null URL is passed to resolveUrl");
        }
        if (url.toLowerCase().startsWith("http")) return url;
        try {
            if (url.startsWith("/")) {
                // it's a URI path..
                return ServerContext.getRequestOwner().getHostUrl() + url;
            } else {
                // assume it's a URL relative to the app's context
                return ServerContext.getRequestOwner().getBaseUrl() + url;
            }
        } catch (Exception e) {
            throw new IllegalStateException("host url can not be derived");
        }
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


    @WebListener
    public static class ContextListener implements ServletContextListener {
        public static final String WEBAPP_CONFIG_LOC = "/WEB-INF/config";

        public void contextInitialized(ServletContextEvent servletContextEvent) {
            try {
                System.out.println("contextInitialized...");
                ServletContext cntx = servletContextEvent.getServletContext();
                ServerContext.init(cntx.getContextPath(), cntx.getServletContextName(), cntx.getRealPath(WEBAPP_CONFIG_LOC));
                VersionUtil.initVersion(cntx);  // can be called multiple times, only inits on the first call
                SCHEDULE_TASK_EXEC.scheduleAtFixedRate(
                        () -> DbMonitor.cleanup(false),
                        DbMonitor.CLEANUP_INTVL,
                        DbMonitor.CLEANUP_INTVL,
                        TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public void contextDestroyed(ServletContextEvent servletContextEvent) {
            try {
                System.out.println("contextDestroyed...");
                DbMonitor.cleanup(true, false);
                ((EhcacheProvider)CacheManager.getCacheProvider()).shutdown();
                try {
                    SHORT_TASK_EXEC.shutdownNow();
                    SHORT_TASK_EXEC.awaitTermination(5, TimeUnit.SECONDS);
                    SCHEDULE_TASK_EXEC.shutdownNow();
                    SCHEDULE_TASK_EXEC.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static Info getSeverInfo() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long pMem =  osBean.getTotalMemorySize();
        Runtime rt= Runtime.getRuntime();
        long totMem= rt.totalMemory();
        long freeMem= rt.freeMemory();
        long maxMem= rt.maxMemory();
        return new Info(FileUtil.getHostname(), pMem, FileUtil.getIPString(), maxMem, totMem, freeMem);
    }
    public record Info(String host, long pMemory, String ip, long jvmMax, long jvmTotal, long jvmFree) {}
}
