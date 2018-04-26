/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.background.*;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.packagedata.BackgroundInfoCacher;
import edu.caltech.ipac.firefly.server.packagedata.PackageMaster;
import edu.caltech.ipac.firefly.server.packagedata.PackagedEmail;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.util.DownloadScript;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: roby
 * Date: Aug 23, 2010
 * Time: 12:24:18 PM
 */


/**
 * @author Trey Roby
 */
public class BackgroundEnv {

    public enum DownloadProgress { STARTING, WORKING, DONE, UNKNOWN, FAIL}

    private static final String BG_USER_PREFIX = "BG_USER";

    private static final String _hostname= FileUtil.getHostname();

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.DOWNLOAD_LOGGER);

    private static final String BASE_SERVLET = "servlet/Download?"+ AnyFileDownload.LOG_PARAM +"=true&" +
                                                 AnyFileDownload.FILE_PARAM +"=";
    private static final String RET_FILE = "&"+AnyFileDownload.RETURN_PARAM+"=";
    private static final Map<String,Integer> _unknownWaitingReports= Collections.synchronizedMap(new HashMap<String,Integer>());


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    //====================================================================
    // user's level background jobs related methods.
    //====================================================================
    public static List<String> getUserBackgroundInfoKeys() {
        String userKey = ServerContext.getRequestOwner().getUserKey();
        StringKey cacheKey = new StringKey(BG_USER_PREFIX, userKey);
        List<String> rval = (List<String>) getCache().get(cacheKey);
        return rval == null ? new ArrayList<>() : rval;
    }

    public static List<BackgroundInfoCacher> getUserBackgroundInfo() {
        List<BackgroundInfoCacher> rval = getUserBackgroundInfoKeys().stream()
                                    .map( s -> new BackgroundInfoCacher(s))
                                    .collect(Collectors.toList());
        return rval;
    }

    public static void addUserBackgroundInfo(BackgroundStatus bgStat) {
        String bgId = bgStat.getID();
        List<String> bgInfos = getUserBackgroundInfoKeys();
        if (!bgInfos.contains(bgId)) {
            bgInfos.add(bgId);
            updateUserBackgroundInfo(bgInfos);
            BackgroundInfoCacher.fireBackgroundJobAdd(bgStat);
        }
    }

    public static void removeUserBackgroundInfo(String bgId) {
        List<String> bgInfos = getUserBackgroundInfoKeys();
        boolean updated = bgInfos.remove(bgId);
        if (updated) {
            updateUserBackgroundInfo(bgInfos);
            FluxAction rmJob = new FluxAction("background.bgJobRemove");
            rmJob.setValue(bgId, "id");
            ServerEventManager.fireAction(rmJob, ServerEvent.Scope.USER);
        }
    }

    static void updateUserBackgroundInfo(List<String> bgIds) {
        String userKey = ServerContext.getRequestOwner().getUserKey();
        StringKey cacheKey = new StringKey(BG_USER_PREFIX, userKey);
        getCache().put(cacheKey, bgIds);
    }
    //====================================================================

    public static boolean cleanup(String id) { return true; }

    public static boolean cancel(String id) {
        new BackgroundInfoCacher(id).cancel();
        return true;
    }

    public static boolean remove(String id) {
        removeUserBackgroundInfo(id);
        new BackgroundInfoCacher(id).cancel();
        return true;
    }

    public static void setAttribute(String id, JobAttributes attribute) {
        BackgroundInfoCacher infoCacher= new BackgroundInfoCacher(id);
        BackgroundStatus bgStat= infoCacher.getStatus();
        bgStat.addAttribute(attribute);
        infoCacher.setStatus(bgStat);
    }

    public static void setAttribute(List<String> idList, JobAttributes attribute) {
        for(String id : idList) setAttribute(id,attribute);
    }

    public static void setEmail(String id, String email) {
        BackgroundInfoCacher infoCacher= new BackgroundInfoCacher(id);
        if (!infoCacher.isCanceled()) infoCacher.setEmailAddress(email);
    }


    public static void setEmail(List<String> idList, String email) {
        for(String id : idList) setEmail(id, email);
    }


    public static String getEmail(String id) {
        String email= null;
        BackgroundInfoCacher infoCacher= new BackgroundInfoCacher(id);
        if (!infoCacher.isCanceled()) email= infoCacher.getEmailAddress();
        return email;
    }

    public static void resendEmail(String id, String email) {
        if (id!=null) {
            BackgroundInfoCacher pi= new BackgroundInfoCacher(id);
            BackgroundStatus bgStat= pi.getStatus();
            if (bgStat!=null && bgStat.hasAttribute(JobAttributes.CanSendEmail)) {
                if (StringUtils.isEmpty(email)) {
                    email= pi.getEmailAddress();
                }
                else {
                    pi.setEmailAddress(email);
                }
                if (!StringUtils.isEmpty(email)) {
                    if (bgStat.getBackgroundType()== BackgroundStatus.BgType.PACKAGE) { // TODO: remove this if when we can support email in a more general way
                        PackagedEmail.send(email, pi);
                    }
                }
            }
        }
    }

    public static void resendEmail(List<String> idList, String email) {
        for(String id : idList) resendEmail(id, email);
    }

    public static void clearPushEntry(String id, int idx) {
        BackgroundInfoCacher pi= new BackgroundInfoCacher(id);
        BackgroundStatus bgStat= pi.getStatus();
        if (bgStat!=null) {
            bgStat.removeParam(BackgroundStatus.PUSH_DATA_BASE+idx );
            bgStat.removeParam(BackgroundStatus.PUSH_TYPE_BASE+idx );
            pi.setStatus(bgStat);
        }
    }

    public static void reportUserAction(String channel, String desc, String data) {
        ServerEvent userAction = new ServerEvent(Name.REPORT_USER_ACTION, ServerEvent.Scope.CHANNEL, data);
        ServerEventManager.fireEvent(userAction);

//
//
//
//       //
//        BackgroundInfoCacher pi= new BackgroundInfoCacher(channel);
//        BackgroundStatus bgStat= pi.getStatus();
//        if (bgStat!=null) {
//            if (bgStat.getRequestedCnt()>0) {
//                bgStat.addResponseData(desc, data);
//                pi.setStatus(bgStat);
//            }
//        }
    }

    public static ScriptRet createDownloadScript(String id,
                                              String fName,
                                              String dataSource,
                                              List<ScriptAttributes> attributes) {
        ScriptRet retval= null;
        if (id!=null) {
            BackgroundInfoCacher pi= new BackgroundInfoCacher(id);
            BackgroundStatus bgStat= pi.getStatus();
            if (bgStat!=null && bgStat.hasAttribute(JobAttributes.DownloadScript)) {
                if (!StringUtils.isEmpty(id)) {
                    retval= buildScript(bgStat, fName, dataSource, attributes);
                }
            }
        }
        return retval;
    }


    private static ScriptRet buildScript(BackgroundStatus bgStat,
                                         String fName,
                                         String dataSource,
                                         List<ScriptAttributes> attributes) {
        ScriptRet retval= null;
        if (bgStat.getBackgroundType()== BackgroundStatus.BgType.PACKAGE) {// TODO: remove this if when we can support downloads in a more general way
            List<URL> urlList= new ArrayList<URL>(bgStat.getPackageCount());
            for(PackageProgress part : bgStat.getPartProgressList()) {
                try {
                    urlList.add(new URL(part.getURL()));
                } catch (MalformedURLException e) {
                    _log.warn("Bad url for download script: " + part.getURL() + "Background ID: " + bgStat.getID());
                }
            }
            if (fName.length() < 3) {
                fName = StringUtils.pad(3, fName, StringUtils.Align.LEFT, '_');
            }
            if (urlList.size()>0) {
                String  ext = attributes.contains(ScriptAttributes.URLsOnly) ? ".txt" : ".sh";
                try {
                    File outFile = File.createTempFile(fName, ext, ServerContext.getStageWorkDir());
                    String retFile= fName+ext;
                    DownloadScript.composeDownloadScript(outFile, dataSource, urlList, attributes);
                    String fStr= ServerContext.replaceWithPrefix(outFile);
                    try {
                        fStr = URLEncoder.encode(fStr, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // if it fails, then use the original
                    }

                    if (fStr!= null) {
                        retval=  new ScriptRet(BASE_SERVLET  + fStr + RET_FILE + retFile, outFile);
                        _log.info("download script built, returning: " + retval.getFile(),
                                  "Background ID: " + bgStat.getID());
                        _statsLog.stats("create_script", "fname", retval.getFile());
                    }
                } catch (IOException e) {
                    _log.warn(e,"Could not create temp file",
                              "Background ID: " + bgStat.getID(),
                              "file root: "  + fName,
                              "ext: "+ ext);
                    retval= null;
                }
            }
            else {
                _log.warn("Could not build a download script list, urlList length==0",
                          "Background ID: " + bgStat.getID());
            }
        }
        return retval;

    }


    /**
     * Check the download progress of a individual file
     * @param fileKey the key of the file to check
     * @return the enum with the status
     */
    public static DownloadProgress getDownloadProgress(String fileKey) {
        Cache cache= AnyFileDownload.getCache(); // use the any file download choice of cache
        StringKey key= new StringKey(fileKey);
        DownloadProgress retval= DownloadProgress.UNKNOWN;
        if (cache.isCached(key)) {
            retval= (DownloadProgress)cache.get(key);
            if (retval== DownloadProgress.FAIL) {
                // once a fail is returned then put the unknown state,
                // this will keep retries from showing fail before an actual fail
                // note- most of the time a fail is when the user just canceled the download
                cache.put(key, DownloadProgress.UNKNOWN);

            }
        }
        return retval;
    }

    public static BackgroundStatus backgroundProcess(int waitMills, BackgroundProcessor processor, BackgroundStatus.BgType type) {
        String bid= processor.getBID();
        runBackgroundThread(waitMills, processor);
        Logger.briefDebug("Background thread returned");
        BackgroundStatus bgStat= processor.getBackgroundStatus();
        if (bgStat==null) {
            bgStat = processor.getPiCacher().getStatus();
            bgStat = bgStat == null ? new BackgroundStatus(bid, BackgroundState.WAITING, type) : bgStat;
        }
        if (!bgStat.isDone()) {
            // it's not done within the same request.. add it to background and enable email notification.
            bgStat.addAttribute(JobAttributes.CanSendEmail);
        }
        processor.getPiCacher().setStatus(bgStat);
        Logger.briefDebug("Background report returned");
        return bgStat;
    }






    public static String makeBackgroundID() {
        return "bid"+ (System.currentTimeMillis() % 100000000000L) + "_" + _hostname; // unique for 3 years
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static void runBackgroundThread(int waitMills,
                                            final BackgroundProcessor processor) {
        String name= "background-processor-" + processor.getBID();
        Thread thread= new Thread(processor, name);

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                threadCompletedWithException(processor.getBID(), t, e);
            }
        });

        thread.setDaemon(true);
        thread.start();
        
        try {
            thread.join(waitMills);
        } catch (InterruptedException e) {
            threadCompletedWithException(processor.getBID(),thread,e);
        }
    }


    private static synchronized BackgroundStatus threadCompletedWithException(String bid, Thread t, Throwable e) {
        BackgroundStatus bgStat= null;
        try {
            _log.error(e,
                       "Background ID: " + bid,
                       "Thread: "+ t.getName(),
                       "Thread completed with exception",
                       "Exception: " + e.toString(),
                       "Traceback follows");

            bgStat = new BackgroundStatus(bid, BackgroundState.FAIL);
            bgStat.addMessage(e.getMessage());
            setReportToCache(bid,bgStat);
        } catch (Throwable e1) {
            _log.error(e1,
                       "WARNING! WARNING!!! DANGER!!! DANGER!!!",
                       "Thread: "+ t.getName(),
                       "Error in Exception recovery, Traceback follows");
        }
        return bgStat;
    }

    private static void setReportToCache(String bid, BackgroundStatus bgStat) {
        BackgroundInfoCacher cacher= new BackgroundInfoCacher(bid);
        cacher.setStatus(bgStat);
    }

    public static Cache getCache() {
        return CacheManager.getCache(Cache.TYPE_PERM_SMALL);
    }




    public static BackgroundStatus getStatus(String id, boolean polling) {
        BackgroundStatus status= null;
        Cache cache= getCache();

        if (cache.isCached(new StringKey(id))) {
            BackgroundInfoCacher pi= new BackgroundInfoCacher(id);

            status= pi.isCanceled() ? new BackgroundStatus(id,BackgroundState.CANCELED) : pi.getStatus();

            if (status==null) {
                status= new BackgroundStatus(id,BackgroundState.WAITING);
                _log.info("Creating a temporary waiting report, estimate has yet not completed.",
                          "Background ID: "+ id);
            }

            if (status.isDone()) {
                PackageMaster.logPIDDebug(status,"Report Status");
            }
            else {
                _log.briefInfo("Report Status " + id +": " +status.getState());
            }
        }
        if (status==null) status= createUncachedStatus(id, polling);
        return status;
    }

    @Deprecated
    public static void addIDToPushCriteria(String id) {
        // TODO: need to investigate...  do nothing for now.
//        ServerEventManager.addSessionExtraEventTarget(new EventTarget.BackgroundID(id));
    }

    private static BackgroundStatus createUncachedStatus(String id, boolean polling) {
        BackgroundStatus status;
        int cnt= 0;

        if (polling) {
            if (_unknownWaitingReports.containsKey(id)) {
                cnt= _unknownWaitingReports.get(id);
            }
            cnt++;
            _unknownWaitingReports.put(id, cnt);
            if (cnt>3 || !polling) {
                status= new BackgroundStatus(id,BackgroundState.FAIL);
                _log.warn("Creating a failed report because this key could not be found after 3 or more checks",
                          "Background ID: "+ id);
            }
            else {
                status= new BackgroundStatus(id,BackgroundState.WAITING);
                status.addAttribute(JobAttributes.Unknown);
                _log.warn("Creating a waiting report for a key that is not in the cache",
                          "Background ID: " + id);
            }
        }
        else {
            status= new BackgroundStatus(id,BackgroundState.FAIL);
            _log.warn("Creating a failed report because key is not found", "Background ID: "+ id);
        }
        return status;
    }


//======================================================================
//----------------------- Public Inner Classes -------------------------
//======================================================================

    public interface Worker {
        BackgroundStatus work(BackgroundProcessor processor) throws Exception;
    }

    public static class ScriptRet {
        final private String _servlet;
        final private File   _file;

        public ScriptRet(String servlet, File file) {
            _servlet= servlet;
            _file= file;
        }

        public String getServlet() { return _servlet;}
        public File getFile() { return _file;}
    }

    /**
     * Run in the background to query for package data and start the packaging
     */
    public static class BackgroundProcessor implements Runnable {
        private final Worker _worker;
        private final String _bid;
        private final String _baseFileName;
        private final String _title;
        private final String _dataTag;
        private final String _email;
        private final String _dataSource;
        private final RequestOwner _requestOwner;
        private final BackgroundInfoCacher piCacher;
        private volatile BackgroundStatus _bgStat= null;

        public BackgroundProcessor(Worker worker,
                                   String baseFileName,
                                   String title,
                                   String dataTag,
                                   String email,
                                   String dataSource,
                                   RequestOwner requestOwner) {
            this(worker,baseFileName,title,dataTag,email,dataSource,requestOwner,null,null);
        }

        public BackgroundProcessor(Worker worker,
                                   String title,
                                   String dataTag,
                                   String dataSource,
                                   RequestOwner requestOwner,
                                   String bid,
                                   ServerEvent.EventTarget target) {
            this(worker,null,title,dataTag,null,dataSource,requestOwner,bid,target);
        }

        public BackgroundProcessor(Worker worker,
                                   String baseFileName,
                                   String title,
                                   String dataTag,
                                   String email,
                                   String dataSource,
                                   RequestOwner requestOwner,
                                   String bid,
                                   ServerEvent.EventTarget evTarget) {
            _bid = bid!=null ? bid : makeBackgroundID();
            _worker= worker;
            _baseFileName= baseFileName;
            _title= title;
            _dataTag = dataTag;
            _email= email;
            _dataSource= dataSource;
            _requestOwner= requestOwner;
            piCacher= new BackgroundInfoCacher(_bid, _email, _baseFileName, _title, _dataTag, evTarget); // force a cache entry here
        }


        public void run() {
            BackgroundStatus bgStat;
            try {
                bgStat= _worker.work(this);
            } catch (Exception e) {
                bgStat= threadCompletedWithException(_bid, Thread.currentThread(), e);
            }
            synchronized (this) {
                _bgStat= bgStat;
                piCacher.setStatus(bgStat);
            }
        }

        public synchronized BackgroundStatus getBackgroundStatus() { return _bgStat; }
        public String getBID() { return _bid; }

        public String getEmail() { return _email; }
        public String getTitle() { return _title; }
        public String getBaseFileName() { return _baseFileName; }
        public String getDataSource() { return _dataSource; }
        public RequestOwner getRequestOwner() { return _requestOwner; }
        public BackgroundInfoCacher getPiCacher () { return piCacher; }
    }
}

