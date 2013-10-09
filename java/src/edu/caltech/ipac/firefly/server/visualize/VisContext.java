package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.client.net.CacheHelper;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.Cleanupable;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.visualize.plot.ImagePlot;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
/**
 * User: roby
 * Date: Jul 29, 2008
 * Time: 3:36:31 PM
 */


/**
 * @author Trey Roby
 */
public class VisContext {

    public static final String PLOT_ABORTED= "Plot aborted by client";
    public static final String VIS_SEARCH_PATH= "visualize.fits.search.path";
    public static final long    FITS_MAX_SIZE = AppProperties.getLongProperty("visualize.fits.MaxSizeInBytes",
                                                                              (long)(FileUtil.GIG*2));
    public static final long USER_ALLOWED_SIZE_MB=AppProperties.getLongProperty("visualize.fits.UserAllowedSizeMB",
                                                                                 700);
    public static final boolean OPTIMIZE_FOR_SPEED=AppProperties.getBooleanProperty("visualize.fits.OptimizeForSpeed",true);
    private static final String VIS_DIR_STR= "visualize";
    private static final String CACHE_DIR_STR= "fits-cache";
    private static final String UPLOAD_DIR_STR= "fits-upload";
    private static final String USERS_BASE_DIR_STR= "users";
    private static final String PFX_START= "${";
    private static final String PFX_END= "}";
    private static final int PFX_TOTAL_CHAR= 3;
    private static final int PFX_START_LEN= PFX_START.length();
    private static final boolean FITS_SECURITY = AppProperties.getBooleanProperty("visualize.fits.Security", true);

    public static final String CACHE_DIR_PREFIX       = "${cache-dir}";
    public static final String UPLOAD_DIR_PREFIX      = "${upload-dir}";
    public static final String USER_IMAGES_DIR_PREFIX = "${user-images-dir}";
    public static final String USER_BASE_DIR_PREFIX   = "${user-base-dir}";
    public static final String SEARCH_PATH_PREFIX     = "${search-path}";
    public static final String STAGE_PATH_PREFIX      = "${stage}";
    public static final String PERM_FILES_PATH_PREFIX = "${perm-files}";
    public static final String TEMP_FILES_PATH_PREFIX = "${temp-files}";

    private static final Logger.LoggerImpl _log= Logger.getLogger();

    private static boolean _initialized= false;
    private volatile static File _visSearchPath[]= null;

    private volatile static String _permFileDirStr = ServerContext.getPermWorkDir().getPath();
    private volatile static String _tempFileDirStr = ServerContext.getTempWorkDir().getPath();
    private volatile static String _stageFileDirStr = ServerContext.getStageWorkDir().getPath();
    private volatile static String _cacheDirStr = getVisCacheDir().getPath();
    private volatile static String _vUploadDirStr = getVisUploadDir().getPath();

    private final static Map<File, Long> _visSessionDirs= new HashMap<File, Long>(617);
    private final static AtomicLong _lastDirCheck= new AtomicLong(0) ;
    private final static long EXPIRE_DAYS= 3;
    private final static long EXPIRE_DIR_DELTA = 1000 * 60 * 60 * 24 * EXPIRE_DAYS;
    private final static long CHECK_DIR_DELTA = 1000 * 60 * 60 * 12; // 12 hours
    private final static long MAX_AVAILABLE_K;

    /**
     * This cache key will be different for each user.  Even though it is static it compute a unique key
     * per session id.
     */
    private static final CacheKey PER_SESSION_CTX_MAP_ID = new CacheKey() {
        public String getUniqueString() {
            String id= ServerContext.getRequestOwner().getSessionId();
            return  "VisContext-OnePerUser-"+id;}
    };


    static {
        long maxK= (Runtime.getRuntime().maxMemory()) / FileUtil.K;
        long maxMB= maxK/1024;
        if (maxMB< 2000) {
            MAX_AVAILABLE_K= (long)(maxK*.1);
        }
        else if (maxMB< 5000) {
            MAX_AVAILABLE_K= (long)(maxK*.2);
        }
        else {
            MAX_AVAILABLE_K= (long)(maxK*.33);
        }




        init();
    }



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================



//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static File getWorkingFitsFile(PlotState state, Band band) {
        return convertToFile(state.getWorkingFitsFileStr(band));
    }
    public static void setWorkingFitsFile(PlotState state, File f, Band band) {
        state.setWorkingFitsFileStr(replaceWithPrefix(f), band);
    }

    public static File getOriginalFile(PlotState state, Band band) {
        return convertToFile(state.getOriginalFitsFileStr(band));
    }

    public static void setOriginalFitsFile(PlotState state, File f, Band band) {
        state.setOriginalFitsFileStr(replaceWithPrefix(f), band);
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
                    retval= new File(ServerContext.getStageWorkDir(), relFile);
                }
                else if (prefix.equals(PERM_FILES_PATH_PREFIX)) {
                    retval= new File(ServerContext.getPermWorkDir(), relFile);
                }
                else if (prefix.equals(TEMP_FILES_PATH_PREFIX)) {
                    retval= new File(ServerContext.getTempWorkDir(), relFile);
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

    public static boolean isInUploadDir(File f) {
        return (f!=null &&f.getPath().startsWith(_vUploadDirStr));
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

    public static String replaceWithPrefix(String pfx, File f) {
        return pfx + "/" + f.getName();
    }

    public static String replaceWithPrefix(File f) {
        if (f==null) return null;
        String retval= null;
        String path= f.getPath();
        String userImageDir= getVisSessionDir().getPath();

        String userBaseDir= getUsersBaseDir().getPath();
        if (path.startsWith(_cacheDirStr)) {
            retval= replacePrefix(path, _cacheDirStr,CACHE_DIR_PREFIX);
        }
        else if (path.startsWith(userImageDir)) {
            retval= replacePrefix(path,userImageDir,USER_IMAGES_DIR_PREFIX);
        }
        else if (path.startsWith(_vUploadDirStr)) {
            retval= replacePrefix(path, _vUploadDirStr,UPLOAD_DIR_PREFIX);
        }
        else if (path.startsWith(_permFileDirStr)) {
            retval= replacePrefix(path, _permFileDirStr,PERM_FILES_PATH_PREFIX);
        }
        else if (path.startsWith(_tempFileDirStr)) {
            retval= replacePrefix(path, _tempFileDirStr,TEMP_FILES_PATH_PREFIX);
        }
        else if (path.startsWith(_stageFileDirStr)) {
            retval= replacePrefix(path, _stageFileDirStr,STAGE_PATH_PREFIX);
        }
        else if (path.startsWith(userBaseDir)) {
            retval= replacePrefix(path,userBaseDir,USER_BASE_DIR_PREFIX);
        }
        else if (!FITS_SECURITY && path.charAt(0)==File.separatorChar) {
            retval= path;
        }

        else { // search path
            String pathDir;
            for(File searchF : _visSearchPath) {
                pathDir= searchF.getPath();
                if (path.startsWith(pathDir)) {
                    retval= replacePrefix(path,pathDir,SEARCH_PATH_PREFIX);
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
        return replacePrefix(f.getPath(),propertyValue,PFX_START+propertyName+PFX_END);
    }



    public static String replaceWithUsersBaseDirPrefix(File f) {
        String path= f.getPath();
        String userBaseDir= getUsersBaseDir().getPath();
        String retval= null;
        if (path.startsWith(userBaseDir)) {
            retval= replacePrefix(path,userBaseDir,USER_BASE_DIR_PREFIX);
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



    private static File findFileInPath(String relativeStr) {
        File foundFile= null;
        if (_visSearchPath.length>0) {
            File f;
            for(File froot : _visSearchPath) {
                f= new File(froot,relativeStr);
                if (f.canRead()) {
                    foundFile= f;
                    break;
                }
            }
        }
        return foundFile;
    }

    public static boolean isFileInPath(File f) { return validateFileInPath(f.getPath(),true)!=null; }

    private static File validateFileInPath(String fileStr, boolean useSecurity) {
        useSecurity= useSecurity && FITS_SECURITY;
        File foundFile= null;
        if (_visSearchPath.length>0) {
            for(File fRoot : _visSearchPath) {
                if (fileStr.startsWith(fRoot.getPath())) {
                    foundFile= new File(fileStr);
                    break;
                }
            }
            if (foundFile==null) {
                if (fileStr.startsWith(getVisCacheDir().getPath()) ||
                    fileStr.startsWith(getVisUploadDir().getPath()) ||
                    fileStr.startsWith(getVisSessionDir().getPath()) ||
                    fileStr.startsWith(getUsersBaseDir().getPath())  ) {
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
        File dir= ServerContext.getWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the working dir");
        File visDir= new File(dir,VIS_DIR_STR);
        if (!visDir.exists()) mkdirs(visDir);
        return visDir;
    }

    public static File getVisCacheDir() {
        File dir= getVisWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the vis cache dir");
        File cacheDir= new File(dir,CACHE_DIR_STR);
        if (!cacheDir.exists()) mkdirs(cacheDir);
        return cacheDir;
    }

    public static File getVisUploadDir() {
        File dir= getVisWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the vis cache dir");
        File cacheDir= new File(dir,UPLOAD_DIR_STR);
        if (!cacheDir.exists()) mkdirs(cacheDir);
        return cacheDir;
    }

//    public static File[] getVisSearchPath() { return _visSearchPath; }

    public static File getUsersBaseDir() {
        File dir= getVisWorkingDir();
        Assert.argTst(dir.canWrite(), "can't write to the vis users base dir");
        File userBaseDir= new File(dir, USERS_BASE_DIR_STR);
        if (!userBaseDir.exists()) mkdirs(userBaseDir);
        return userBaseDir;
    }


    public static File getVisSessionDir() {
        File dir= getUsersBaseDir();
        Assert.argTst(dir.canWrite(), "can't write to the users image working dir");
        RequestOwner owner= ServerContext.getRequestOwner();
//        String userID= (owner.getUserKey()==null) ? "" : owner.getUserKey()+"-";
        String dirStr= owner.getSessionId();
        File userDir= new File(dir, dirStr);
        if (!userDir.exists()) {
            mkdirs(userDir);
            _visSessionDirs.put(userDir,System.currentTimeMillis());
        }
        return userDir;
    }

    public static void mkdirs(File dir) {
        boolean success= dir.mkdirs();
        if (!success && dir.exists()) {
            success= true;
        }
        Assert.argTst(success, "failed to create " +dir.getPath());
    }

    public static void shouldContinue(PlotClientCtx ctx, ImagePlot plot) throws FailedRequestException {
        shouldContinue(ctx.getKey(),plot);
    }

    public static void shouldContinue(String ctxStr) throws FailedRequestException {
        shouldContinue(ctxStr,null);
    }

    public static void shouldContinue(String ctxStr, ImagePlot plot) throws FailedRequestException {
        if (ctxStr!=null && getPlotCtx(ctxStr)==null) {
            if (plot!=null) plot.freeResources();
            throw new FailedRequestException(PLOT_ABORTED,PLOT_ABORTED);
        }
    }

    public static void purgeOtherPlots(PlotState state) {
        if (OPTIMIZE_FOR_SPEED) {
//            purgeOthersOptimizeForSpeedByUser(state);
            purgeOthersOptimizeForSpeedBySystem(state);
        }
        else {
            purgeOtherOptimizeForMemory(state);
        }
    }

    private static void purgeOtherOptimizeForMemory(PlotState state) {
        PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
        if (ctx!=null) {
            String excludeKey= ctx.getKey();
            synchronized (VisContext.class) {
                try {
                    for(Map.Entry<String,PlotClientCtx> entry : getMap().entrySet()) {
                        if (!entry.getKey().equals(excludeKey)) {
                            entry.getValue().freeResources(PlotClientCtx.Free.YOUNG);
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    // just abort the purging - another thread is updating the map
                }
            }
        }
    }


    /**
     * Memory clean up attempts to keeps as much in memory as possible
     * @param excludeState
     */
    private static void purgeOthersOptimizeForSpeedBySystem(PlotState excludeState) {
        PlotClientCtx excludeCtx= VisContext.getPlotCtx(excludeState.getContextString());
        String excludeKey= excludeCtx!=null ? excludeCtx.getKey() : null;
        synchronized (VisContext.class) {
            long totalInUseK= 0;
            long totalCnt= 0;
            long startTotal= 0;
            List<PlotClientCtx> allInUseCtx= new ArrayList<PlotClientCtx>(500);
            Cache cache= getCache();
            List<String> keys= cache.getKeys();
            boolean freed;
            for(String key: keys) {
                Object o= cache.get(new StringKey(key));
                if (o instanceof UserCtx) {
                    Map<String,PlotClientCtx> map= ((UserCtx)o).getMap();
                    PlotClientCtx ctx;
                    for(Map.Entry<String,PlotClientCtx> entry : map.entrySet()) {
                        ctx= entry.getValue();
                        if (!ctx.getKey().equals(excludeKey)) {
                            if (ctx.getPlot()!=null) {  // if we are using memory
                                freed= entry.getValue().freeResources(PlotClientCtx.Free.OLD);
                                if (freed)  {
                                    totalCnt++;
                                }
                                else {
                                    totalInUseK+= ctx.getDataSizeK();
                                    allInUseCtx.add(ctx);
                                }
                                startTotal+= ctx.getDataSizeK();
                            }
                        }
                    }
                }
            }
            String aggressiveDesc= "";
            if (totalInUseK>MAX_AVAILABLE_K) {
                long purgeDownToK= (long)(MAX_AVAILABLE_K*.80);
                Collections.sort(allInUseCtx, new Comparator<PlotClientCtx>() {
                    public int compare(PlotClientCtx c1, PlotClientCtx c2) {
                        return -1 * ComparisonUtil.doCompare(c1.getAccessTime(), c2.getAccessTime());
                    }
                });

                for(PlotClientCtx ctx : allInUseCtx) {
                    if (!ctx.getKey().equals(excludeKey)) {
                        freed= ctx.freeResources(PlotClientCtx.Free.YOUNG);
                        if (freed) {
                            totalInUseK-= ctx.getDataSizeK();
                            totalCnt++;
                            if (totalInUseK<purgeDownToK) break;
                        }
                    }
                }

                if (totalInUseK>MAX_AVAILABLE_K*1.2) {
                    aggressiveDesc= ", aggressive";
                    for(PlotClientCtx ctx : allInUseCtx) {
                        if (!ctx.getKey().equals(excludeKey)) {
                            freed= ctx.freeResources(PlotClientCtx.Free.VERY_YOUNG);
                            if (freed) {
                                totalInUseK-= ctx.getDataSizeK();
                                totalCnt++;
                                if (totalInUseK<purgeDownToK) break;
                            }
                        }
                    }
                }
                if (totalInUseK>MAX_AVAILABLE_K*1.2) {
                    aggressiveDesc= ", very aggressive";
                    for(PlotClientCtx ctx : allInUseCtx) {
                        if (!ctx.getKey().equals(excludeKey)) {
                            freed= ctx.freeResources(PlotClientCtx.Free.INFANT);
                            if (freed) {
                                totalInUseK-= ctx.getDataSizeK();
                                totalCnt++;
                                if (totalInUseK<purgeDownToK) break;
                            }
                        }
                    }
                }


                if (totalCnt==0) {
                    Logger.debug("Free resources : no candidates to free, " +
                                         "current memory exceeds target, current/target (MB): "+
                                         totalInUseK/1024+ " / "+ MAX_AVAILABLE_K/1024);

                }
            }
            if (totalCnt>0) {
                String exceeds= totalInUseK>MAX_AVAILABLE_K ? ", in use memory still exceeds target" : "";
                Logger.debug("Free resources : start/end/target (MB): "+ startTotal/1024 +
                                     " / "+ totalInUseK/1024+
                                     " / "+ MAX_AVAILABLE_K/1024+
                                     ", plots freed: "+totalCnt + exceeds+ aggressiveDesc );
            }
        }
    }


    /**
     */
//    public static long purgeUser(String excludeKey, Map<String,PlotClientCtx> map, long maxAvailable) {
//        synchronized (VisContext.class) {
//            try {
//                PlotClientCtx testCtx;
//                boolean freed;
//                for(Map.Entry<String,PlotClientCtx> entry : map.entrySet()) {
//                    testCtx= entry.getValue();
//                    if (!testCtx.getKey().equals(excludeKey)) {
//                        if (testCtx.getPlot()!=null) {  // if we are using memory
//                            long dataSizeK= testCtx.getDataSizeK();
//                            if (dataSizeK>maxAvailable) {
//                                freed= testCtx.freeResources(false);
//                                if (!freed)  maxAvailable-= dataSizeK;
//                            }
//                            else {
//                                freed= entry.getValue().freeResourcesInactive();
//                                if (!freed)  maxAvailable-= dataSizeK;
//                            }
//                        }
//                    }
//                }
//            } catch (ConcurrentModificationException e) {
//                // just abort the purging - another thread is updating the map
//            }
//        }
//        return maxAvailable;
//    }






    /**
     * Memory clean up attempts to keeps as much in memory as possible
     * @param state
     */
    private static void purgeOthersOptimizeForSpeedByUser(PlotState state) {
        PlotClientCtx ctx= VisContext.getPlotCtx(state.getContextString());
        if (ctx!=null) {
            String excludeKey= ctx.getKey();
            synchronized (VisContext.class) {
                try {
                    long cnt= 0;
                    PlotClientCtx testCtx;
                    boolean freed;
                    for(Map.Entry<String,PlotClientCtx> entry : getMap().entrySet()) {
                        testCtx= entry.getValue();
                        if (!testCtx.getKey().equals(excludeKey)) {
                            if (testCtx.getPlot()!=null) {  // if we are using memory
                                if (cnt>USER_ALLOWED_SIZE_MB) {
                                    freed= testCtx.freeResources(PlotClientCtx.Free.YOUNG);
                                    if (!freed) cnt+= testCtx.getDataSizeMB();
                                }
                                else {
                                    freed= entry.getValue().freeResources(PlotClientCtx.Free.OLD);
                                    if (!freed) cnt+= testCtx.getDataSizeMB();
                                }
                            }
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    // just abort the purging - another thread is updating the map
                }
            }
        }
    }

    private static long getTotalMBUsedByUser() {
        long totalMB= 0;
        for(Map.Entry<String,PlotClientCtx> entry : getMap().entrySet()) {
            totalMB+= entry.getValue().getDataSizeMB();
        }
        return totalMB;
    }


    public static PlotClientCtx getPlotCtx(String ctxStr) {
        return getMap().get(ctxStr);
    }

    public static void putPlotCtx(PlotClientCtx ctx) {
        if (ctx!=null) {
            synchronized (VisContext.class) {
                getMap().put(ctx.getKey(),ctx);
            }
        }
    }

    public static void deletePlotCtx(PlotClientCtx ctx) {
        if (ctx!=null) {
            String key= ctx.getKey();
            ctx.deleteCtx();
            synchronized (VisContext.class) {
                Map<String, PlotClientCtx> map= getMap();
                if (map.containsKey(key)) map.remove(key);
            }
            _log.info("deletePlotCtx: Deleted plot context: " + key);
        }
    }

    /**
     * There is one map per user context.
     * @return the map of plot of PlotClientCtx
     */
    private static Map<String,PlotClientCtx> getMap() {

        Cache cache= getCache();
        UserCtx userCtx;
        boolean created= false;
        synchronized (VisContext.class) {
            userCtx= (UserCtx)cache.get(PER_SESSION_CTX_MAP_ID);
            if (userCtx==null) {
                created= true;
                userCtx= new UserCtx();
                cache.put(PER_SESSION_CTX_MAP_ID,userCtx);
            }
        }
        if (created) {
            _log.info("New session or cache was cleared: Creating new UserCtx",
                      "key: " + PER_SESSION_CTX_MAP_ID.getUniqueString());
            updateActiveUsersStatus(cache, true);
        }
        return userCtx.getMap();
    }

    private static Cache getCache() { return CacheManager.getCache(Cache.TYPE_VISUALIZE); }

    private static void updateActiveUsersStatus(Cache cache, boolean addOne) {
        int activeCtx= 0;
        List<String> cacheKeys= cache.getKeys();
        for(String key : cacheKeys) {
            UserCtx userCtx= (UserCtx)cache.get(new StringKey(key));
            for(PlotClientCtx ctx : userCtx.getMap().values()) {
                if (ctx.getPlot()!=null) {
                    activeCtx++;
                    break;
                }
            }
        }
        if (addOne) activeCtx++;
        Counters.getInstance().updateValue("Vis Current Status","Active Context",activeCtx);
        Counters.getInstance().updateValue("Vis Current Status","Total Context",cacheKeys.size());
    }


    static private void initVisSearchPath() {
        String path= AppProperties.getProperty(VIS_SEARCH_PATH, null);
        if (StringUtil.isEmpty(path)) {
            _visSearchPath= new File[0];
            _log.warn("There is no property " + VIS_SEARCH_PATH+ " defined.",
                      "This property is for security and is required to access fits files off the local server disk.",
                      "When defined this property is a list of top level directories where local fits files can be loaded from.",
                      VIS_SEARCH_PATH + " is defined in the app.prop file in the config directory specified at startup time.",
                      "Example: ",
                      "    "+VIS_SEARCH_PATH+"=/my/directory:/another/dir:/a/b/c:/local/fits/files");
        }
        else {
            path= StringUtil.crunch(path);
            String sep= System.getProperty("path.separator");
            String pathAry[]= path.split(sep);
            _visSearchPath= new File[pathAry.length];
            for(int i=0; (i<pathAry.length); i++) {
                _visSearchPath[i]= new File(pathAry[i]);
            }
            List<String> logList= new ArrayList<String>(10);
            logList.add(VIS_SEARCH_PATH + " loaded: ");
            logList.addAll(Arrays.asList(pathAry));
            logList.add("All local loaded FITS files must resides in these directories or sub-directories");
            _log.info(logList.toArray(new String[logList.size()]));
        }

    }

    static public void init() {
        if (!_initialized) {
            System.setProperty("java.awt.headless", "true");

            _log.info("Working dir: "+ServerContext.getWorkingDir().getPath());

            File cacheDir= getVisCacheDir();

            Cache objCache= CacheManager.getCache(Cache.TYPE_PERM_LARGE);
            Cache fileCache=  CacheManager.getCache(Cache.TYPE_PERM_FILE);

            CacheHelper.setServerMode(true);
            CacheHelper.setFileCache(fileCache);
            CacheHelper.setObjectCache(objCache);
            CacheHelper.setCacheDir(cacheDir);
            CacheHelper.setSupportsLifespan(true);

            initVisSearchPath();
            _initialized= true;


            Counters c= Counters.getInstance();
            c.initKey(Counters.Category.Visualization, "New Plots");
            c.initKey(Counters.Category.Visualization, "New 3 Color Plots");
            c.initKey(Counters.Category.Visualization, "3 Color Band");
            c.initKey(Counters.Category.Visualization, "Revalidate");
            c.initKey(Counters.Category.Visualization, "Zoom");
            c.initKey(Counters.Category.Visualization, "Crop");
            c.initKey(Counters.Category.Visualization, "Flip");
            c.initKey(Counters.Category.Visualization, "Rotate");
            c.initKey(Counters.Category.Visualization, "Color change");
            c.initKey(Counters.Category.Visualization, "Stretch change");
            c.initKey(Counters.Category.Visualization, "Fits header");
            c.initKey(Counters.Category.Visualization, "Region read");
            c.initKey(Counters.Category.Visualization, "Region save");
            c.initKey(Counters.Category.Visualization, "Area Stat");
            c.initKey(Counters.Category.Visualization, "Total Read", Counters.Unit.KB,0);
        }

    }

    private static void cleanupOldDirs() {
        long now= System.currentTimeMillis();
        long lastDelta= now-_lastDirCheck.get();
        if (lastDelta>CHECK_DIR_DELTA) {
            for(Map.Entry<File,Long> entry : _visSessionDirs.entrySet()) {
                if (now - entry.getValue() > EXPIRE_DIR_DELTA) {
                    FileUtil.deleteDirectory(entry.getKey());
                    _log.briefInfo("Removed " + EXPIRE_DAYS + " day old directory: " + entry.getKey().getPath());
                }
            }
            _lastDirCheck.set(System.currentTimeMillis());
        }
    }


// =====================================================================
// -------------------- Inner classes Methods --------------------------------
// =====================================================================

    public static class UserCtx implements Serializable, Cleanupable {
        private final Map<String,PlotClientCtx> _map= new HashMap<String,PlotClientCtx>(217);

        Map<String,PlotClientCtx> getMap() { return _map; }

        public void cleanup() {
            PlotClientCtx ctx;
            Logger.briefDebug("UserCtx.cleanup, Entries:"+_map.size() );
            cleanupOldDirs();
            for(Map.Entry<String,PlotClientCtx> entry : getMap().entrySet()) {
                    ctx= entry.getValue();
                    ctx.freeResources(PlotClientCtx.Free.OLD);
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
