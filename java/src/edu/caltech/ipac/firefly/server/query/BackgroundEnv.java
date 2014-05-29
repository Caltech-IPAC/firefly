package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.background.BackgroundPart;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.IllegalPackageStateException;
import edu.caltech.ipac.firefly.server.packagedata.PackageInfoCacher;
import edu.caltech.ipac.firefly.server.packagedata.PackageMaster;
import edu.caltech.ipac.firefly.server.packagedata.PackagedEmail;
import edu.caltech.ipac.firefly.server.servlets.AnyFileDownload;
import edu.caltech.ipac.firefly.server.sse.EventTarget;
import edu.caltech.ipac.firefly.server.util.DownloadScript;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtil;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Aug 23, 2010
 * Time: 12:24:18 PM
 */


/**
 * @author Trey Roby
 */
public class BackgroundEnv {

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


    public static boolean cleanup(String id) { return true; }

    public static boolean cancel(String id) {
        try {
            new PackageInfoCacher(id).cancel();
        } catch (IllegalPackageStateException e) {
            _log.info("background id, "+id+" not in cache");
        }
        return true;
    }

    public static void setAttribute(String id, BackgroundReport.JobAttributes attribute) {
        try {
            PackageInfoCacher pi= new PackageInfoCacher(id);
            BackgroundReport report= pi.getReport();
            report.addAttribute(attribute);
            pi.setReport(report);
        } catch (IllegalPackageStateException e) {
            _log.info("setAttribute failed, background id, "+id+" not in cache");
        }
    }

    public static void setAttribute(List<String> idList, BackgroundReport.JobAttributes attribute) {
        for(String id : idList) setAttribute(id,attribute);
    }

    public static void setEmail(String id, String email) {
        try {
            PackageInfoCacher pi= new PackageInfoCacher(id);
            if (!pi.isCanceled()) pi.setEmailAddress(email);
        } catch (IllegalPackageStateException e) {
            _log.info("setEmail failed, background id, "+id+" not in cache");
        }
    }


    public static void setEmail(List<String> idList, String email) {
        for(String id : idList) setEmail(id, email);
    }


    public static String getEmail(String id) {
        String email= null;
        try {
            PackageInfoCacher pi= new PackageInfoCacher(id);
            if (!pi.isCanceled()) email= pi.getEmailAddress();
        } catch (IllegalPackageStateException e) {
            _log.info("getEmail failed, background id, "+id+" not in cache");
        }
        return email;
    }

    public static void resendEmail(String id, String email) {
        try {
            if (id!=null) {
                PackageInfoCacher pi= new PackageInfoCacher(id);
                BackgroundReport report= pi.getReport();
                if (report!=null && report.hasAttribute(BackgroundReport.JobAttributes.CanSendEmail)) {
                    if (StringUtil.isEmpty(email)) {
                        email= pi.getEmailAddress();
                    }
                    else {
                        pi.setEmailAddress(email);
                    }
                    if (!StringUtil.isEmpty(email)) {
                        if (report instanceof PackagedReport) { // TODO: remove this if when we can support email in a more general way
                            PackagedEmail.send(email, pi);
                        }
                    }
                }
            }
        } catch (IllegalPackageStateException e) {
            _log.info("resendEmail failed, background id, "+id+" not in cache");
        }
    }

    public static void resendEmail(List<String> idList, String email) {
        for(String id : idList) resendEmail(id, email);
    }

    public static ScriptRet createDownloadScript(String id,
                                              String fName,
                                              String dataSource,
                                              List<BackgroundReport.ScriptAttributes> attributes) {
        ScriptRet retval= null;
        try {
            if (id!=null) {
                PackageInfoCacher pi= new PackageInfoCacher(id);
                BackgroundReport report= pi.getReport();
                if (report!=null && report.hasAttribute(BackgroundReport.JobAttributes.DownloadScript)) {
                    if (!StringUtils.isEmpty(id)) {
                        retval= buildScript(report, fName, dataSource, attributes);
                    }
                }
            }
        } catch (IllegalPackageStateException e) {
            _log.info("createDownloadScript failed, background id, "+id+" not in cache");
        }
        return retval;


    }


    private static ScriptRet buildScript(BackgroundReport report,
                                         String fName,
                                         String dataSource,
                                         List<BackgroundReport.ScriptAttributes> attributes) {
        ScriptRet retval= null;
        if (report instanceof PackagedReport) {// TODO: remove this if when we can support downloads in a more general way
            PackagedReport rep= (PackagedReport)report;
            List<URL> urlList= new ArrayList<URL>(rep.getPartCount());
            for(BackgroundPart part : rep) {
                try {
                    urlList.add( new URL(((PackagedBundle)part).getUrl()));
                } catch (MalformedURLException e) {
                    _log.warn("Bad url for download script: "+ ((PackagedBundle)part).getUrl() +
                              "Background ID: " + report.getID());
                }
            }
            if (urlList.size()>0) {
                String  ext = attributes.contains(BackgroundReport.ScriptAttributes.URLsOnly) ? ".txt" : ".sh";
                try {
                    File outFile = File.createTempFile(fName, ext, ServerContext.getStageWorkDir());
                    String retFile= fName+ext;
                    DownloadScript.composeDownloadScript(outFile, dataSource, urlList, attributes);
                    String fStr= VisContext.replaceWithPrefix(outFile);
                    try {
                        fStr = URLEncoder.encode(fStr, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // if it fails, then use the original
                    }

                    if (fStr!= null) {
                        retval=  new ScriptRet(BASE_SERVLET  + fStr + RET_FILE + retFile, outFile);
                        _log.info("download script built, returning: " + retval.getFile(),
                                  "Background ID: " + report.getID());
                        _statsLog.stats("create_script", "fname", retval.getFile());
                    }
                } catch (IOException e) {
                    _log.warn(e,"Could not create temp file",
                              "Background ID: " + report.getID(),
                              "file root: "  + fName,
                              "ext: "+ ext);
                    retval= null;
                }
            }
            else {
                _log.warn("Could not build a download script list, urlList length==0",
                          "Background ID: " + report.getID());
            }
        }
        return retval;

    }


    /**
     * Check the download progress of a individual file
     * @param fileKey the key of the file to check
     * @return the enum with the status
     */
    public static SearchServices.DownloadProgress getDownloadProgress(String fileKey) {
        Cache cache= AnyFileDownload.getCache(); // use the any file download choice of cache
        StringKey key= new StringKey(fileKey);
        SearchServices.DownloadProgress retval= SearchServices.DownloadProgress.UNKNOWN;
        if (cache.isCached(key)) {
            retval= (SearchServices.DownloadProgress)cache.get(key);
            if (retval== SearchServices.DownloadProgress.FAIL) {
                // once a fail is returned then put the unknown state,
                // this will keep retries from showing fail before an actual fail
                // note- most of the time a fail is when the user just canceled the download
                cache.put(key, SearchServices.DownloadProgress.UNKNOWN);

            }
        }
        return retval;
    }


    public static BackgroundReport backgroundProcess(int waitMills, BackgroundProcessor processor) {
        String bid= processor.getBID();
        runBackgroundThread(waitMills, processor);
        Logger.briefDebug("Background thread returned");
        BackgroundReport report= processor.getBackgroundReport();
        if (report==null) {
            try {
                report= BackgroundReport.createWaitingReport(bid);
                processor.getPiCacher().setReport(report);
            } catch (IllegalPackageStateException e) {
                report= null;
                _log.warn(e, "could not create a report");
            }
        }
        Logger.briefDebug("Background report returned");
        return report;
    }






    public static String makeBackgroundID() {
        return "bid__"+ (System.currentTimeMillis() % 100000000000L) + "__" + _hostname; // unique for 3 years
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static void runBackgroundThread(int waitMills,
                                            final BackgroundEnv.BackgroundProcessor processor) {
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


    private static synchronized BackgroundReport threadCompletedWithException(String bid,
                                                                              Thread t,
                                                                              Throwable e) {
        BackgroundReport report= null;
        try {
            _log.error(e,
                       "Background ID: " + bid,
                       "Thread: "+ t.getName(),
                       "Thread abort during FileGroup creation",
                       "Exception: " + e.toString(),
                       "Traceback follows");

            report = BackgroundReport.createFailReport(bid,
                                                       "Packaging Failed with exception: " +
                                                               e.getMessage());
            setReportToCache(bid,report);
        } catch (Throwable e1) {
            _log.error(e1,
                       "WARNING! WARNING!!! DANGER!!! DANGER!!!",
                       "Thread: "+ t.getName(),
                       "Error in Exception recovery, Traceback follows");
        }
        return report;
    }

    private static void setReportToCache(String bid,
                                        BackgroundReport report) {
        try {
            Cache cache= getCache();
            PackageInfoCacher pi= new PackageInfoCacher(bid);
            pi.setReport(report);
        } catch (IllegalPackageStateException e) {
            _log.warn(e,"Could not set report");
        }
    }

    public static Cache getCache() {
        return CacheManager.getCache(Cache.TYPE_PERM_SMALL);
    }




    public static BackgroundReport getStatus(String id) {
        BackgroundReport report= null;
        Cache cache= getCache();

        if (cache.isCached(new StringKey(id))) {
            try {
                PackageInfoCacher pi= new PackageInfoCacher(id);

                report= pi.isCanceled() ? BackgroundReport.createCanceledReport(id) : pi.getReport();

                if (report==null) {
                    report= BackgroundReport.createWaitingReport(id);
                    _log.info("Creating a temporary waiting report, estimate has yet not completed.",
                              "Background ID: "+ id);
                }

                if (report.isDone()) {
                    PackageMaster.logPIDDebug(report,"Report Status");
                }
                else {
                    String s= (report instanceof PackagedReport) ?
                              ((PackagedReport)report).toBriefBundleString() :
                              report.toString();
                    _log.briefInfo("Report Status " + id +": " +s);
                }
            } catch (IllegalPackageStateException e) {
                _log.warn(e,"could not get report");
            }
        }
        if (report==null) {
            report= createUncachedReport(id);
        }
        return report;
    }

    private static BackgroundReport createUncachedReport(String id) {
        BackgroundReport report;
        int cnt= 0;
        if (_unknownWaitingReports.containsKey(id)) {
            cnt= _unknownWaitingReports.get(id);
        }
        cnt++;

        _unknownWaitingReports.put(id, cnt);

        if (cnt>3) {
            report= BackgroundReport.createFailReport(id,"Lost packaging");
            _log.warn("Creating a failed report because this key could not be found after 3 or more checks",
                      "Background ID: "+ id);
        }
        else {
            report= BackgroundReport.createWaitingReport(id);
            report.addAttribute(BackgroundReport.JobAttributes.Unknown);
            _log.warn("Creating a waiting report for a key that is not in the cache",
                      "Background ID: " + id);
        }
        return report;
    }





//======================================================================
//----------------------- Public Inner Classes -------------------------
//======================================================================

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
        private volatile BackgroundReport _report= null;
        private final Worker _worker;
        private final String _bid;
        private final String _baseFileName;
        private final String _title;
        private final String _email;
        private final String _dataSource;
        private final RequestOwner _requestOwner;
        private final PackageInfoCacher piCacher;

        public BackgroundProcessor(Worker worker,
                                   String baseFileName,
                                   String title,
                                   String email,
                                   String dataSource,
                                   RequestOwner requestOwner) {
            _bid = makeBackgroundID();
            _worker= worker;
            _baseFileName= baseFileName;
            _title= title;
            _email= email;
            _dataSource= dataSource;
            _requestOwner= requestOwner;
            EventTarget target= new EventTarget.Session(requestOwner.getSessionId());
            piCacher= new PackageInfoCacher(_bid, _email, _baseFileName, _title, target); // force a cache entry here
        }


        public void run() {
            BackgroundReport report;
            try {
                report= _worker.work(this);
            } catch (Exception e) {
                report= threadCompletedWithException(_bid, Thread.currentThread(), e);
            }
            synchronized (this) {
                _report= report;
                try {
                    piCacher.setReport(_report);
                } catch (IllegalPackageStateException e) {
                    _log.warn(e, "could set a report, should never happen");
                }
            }
        }

        public synchronized BackgroundReport getBackgroundReport() { return _report; }
        public String getBID() { return _bid; }

        public String getEmail() { return _email; }
        public String getTitle() { return _title; }
        public String getBaseFileName() { return _baseFileName; }
        public String getDataSource() { return _dataSource; }
        public RequestOwner getRequestOwner() { return _requestOwner; }
        public PackageInfoCacher getPiCacher () { return piCacher; }
    }

    public interface Worker {
        BackgroundReport work(BackgroundProcessor processor) throws Exception;
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
