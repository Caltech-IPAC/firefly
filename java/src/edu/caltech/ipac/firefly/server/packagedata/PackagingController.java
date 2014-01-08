package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: roby
 * Date: Sep 25, 2008
 * Time: 4:00:16 PM
 */


/**
 * @author Trey Roby
 */
public class PackagingController {

    public static final long LARGE_PACKAGE = 750 * FileUtil.MEG;
    public static final int ALL = -1;

    private static final int MAX_THREADS = 5;
    private static final int MAX_LARGE_THREADS = 3;
    private static final int WARNING_QUEUE_SIZE = AppProperties.getIntProperty("download.warning.queue.size", 30);
    private static final String STARTED_LARGE = "------- Started (large): ";
    private static final String STARTED_SMALL = "------- Started (small): ";


    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final Logger.LoggerImpl _statsLog = Logger.getLogger(Logger.DOWNLOAD_LOGGER);
    private static final PackagingController _instance = new PackagingController();
    private volatile int _activeThreads = 0;
    private volatile int _activeLargeThreads = 0;
    private volatile int _queueHighWater = 0;
    private volatile long _totalPackage = 0;
    private volatile long _totalImmediatePackage = 0;
    private final List<PackagerItem> _packagerList = new LinkedList<PackagerItem>();

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public static PackagingController getInstance() {
        return _instance;
    }


//    public int getActiveThreads() { return _activeThreads;}

    public synchronized void queue(Packager packager,
                                   RequestOwner requestOwner) {

        _packagerList.add(new PackagerItem(packager, requestOwner));
        int len = _packagerList.size();
        _queueHighWater = Math.max(_queueHighWater, len);
        startNewThreads();
    }

    public PackagedReport doImmediatePackaging(Packager packager,
                                               RequestOwner requestOwner) {
        PackagerItem pi = new PackagerItem(packager, requestOwner);
        PackagedReport report = null;
        try {
            pi.markStartTime();
            report = doPackaging(packager);
            _totalImmediatePackage++;
        } finally {
            updateStatistics(pi, false, requestOwner);
        }
        return report;
    }

    public boolean isQueueLong() {
        return (getQueueSize() > WARNING_QUEUE_SIZE);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private synchronized void startNewThreads() {
        List<String> logList = new ArrayList<String>(12);
        String desc;
        int started = 0;
        int largeActive = countLargeActive();
        int active = 0;
        for (PackagerItem pi : _packagerList) {
            if (!pi.isRunning()) {
                if ((pi.isLarge() && largeActive < MAX_LARGE_THREADS) || !pi.isLarge()) {
                    desc = startThread(pi);
                    logList.add(pi.isLarge() ? STARTED_LARGE : STARTED_SMALL + desc);
                    started++;
                    if (pi.isLarge() && largeActive < MAX_LARGE_THREADS) largeActive++;
                }
            }
            if (pi.isRunning()) {
                active++;
                if (active > MAX_THREADS - 1) break;
            }
        }
        _activeThreads = active;
        _activeLargeThreads = largeActive;

        logStartStatus(logList, started);
    }


    public List<String> getStatus() {
        ArrayList<String> statuses = new ArrayList<String>();
        if (_totalPackage>0) {
            QueueStats stats = new QueueStats(_packagerList);

            statuses.add("  - Total large package (" + StringUtils.getSizeAsString(LARGE_PACKAGE, true) + ") threads: " +
                                 _activeLargeThreads + " of " + MAX_LARGE_THREADS + " allowed");
            statuses.add("  - Total running threads:                  " +
                                 _activeThreads + " of " + MAX_THREADS + " allowed");
            statuses.add("  - Longest current wait time:      " + stats.getLongestWaitStr());
            statuses.add("  - Total packaged since beginning: " + _totalPackage);
            statuses.add("  - Total packaged in background:   " + (_totalPackage - _totalImmediatePackage));
            statuses.add("  - Total immediate packaged:       " + _totalImmediatePackage);
            statuses.add("  - Queue high water mark:          " + _queueHighWater);
            statuses.add("  - Total large waiting:            " + (stats.getTotalLarge() - _activeLargeThreads));
            statuses.add("  - Queue size:                     " + getQueueSize());
        }
        else {
            statuses.add("  - Not Active");
        }
        return statuses;
    }

    private void logStartStatus(List<String> logList, int started) {

        if (started == 0) {
            if (_activeThreads >= MAX_THREADS) {
                logList.add(0, "Max packagers already running, 0 new threads started");
            } else if (_packagerList.size() == 0) {
                logList.add(0, "queue empty, 0 new threads started");
            } else {
                logList.add(0, "0 new threads started");
            }
        } else {
            logList.add(0, started + " new" +
                    (started == 1 ? " thread" : " threads") + " started");
        }

        logList.add(0, "PackagingController: Queue Status Report");
        logList.addAll(getStatus());

        _log.info(logList.toArray(new String[logList.size()]));
    }

    public int getQueueSize() {
        return (_packagerList.size() - _activeThreads);
    }

    private int countLargeActive() {
        int large = 0;
        for (PackagerItem pi : _packagerList) {
            if (pi.isRunning()) {
                if (pi.isLarge()) large++;
            } else {
                break;
            }
        }
        return large;
    }

//    private int countLarge() {
//        int large= 0;
//        for(PackagerItem pi : _packagerList) {
//           if (pi.isLarge()) large++;
//        }
//        return large;
//    }

    private synchronized String startThread(final PackagerItem pi) {
        PackagingThread tc = new PackagingThread(pi);
        String name = "Packager-" + pi.getID();
        Thread thread = new Thread(tc, name);
        String desc = "thread: " + name;

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                threadCompletedWithException(pi, t, e);
            }
        });

        thread.setDaemon(true);
        pi.setRunning(true);
        pi.setThread(thread);
        pi.markStartTime();
        thread.start();
        return desc;
    }


    private PackagedReport doPackaging(Packager packager) {
        return doPackaging(packager, ALL);
    }


    private PackagedReport doPackaging(Packager packager, int idx) {
        if (idx == ALL) {
            packager.packageAll();
        } else {
            packager.packageElement(idx);
        }

        PackageInfo info = packager.getPackageInfo();
        PackagedReport report = getReport(packager);

        if (!info.isCanceled()) {
            if (report.isDone()) {
                try {
                    String email = info.getEmailAddress();
                    if (email != null) PackagedEmail.send(email, info, report);

                } catch (IllegalPackageStateException e) {
                    _log.warn(e, "Could not get email");
                }
            }
        } else {
            report = makeFailReport(packager.getID());
        }
        return report;

    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private synchronized void threadCompletedWithException(PackagerItem pi,
                                                           Thread t,
                                                           Throwable e) {


        try {
            String id = pi.getID();
            _log.error(e, "Package ID: " + id,
                       "Thread: " + t.getName(),
                       "Thread aborted because of exception: " + e.toString(),
                       "Traceback follows");
            PackageInfo packageInfo = pi.getPackageInfo();
            PackagedReport report = (PackagedReport) getReport(pi.getPackager()).cloneWithState(BackgroundState.FAIL);

            report.addMessage("Contact SSC: " + e.toString());
            if (!packageInfo.isCanceled()) {
                packageInfo.setReport(report);
            } else {
                _log.warn("Package ID: " + id,
                          "Thread: " + t.getName(),
                          "PackageInfo is already canceled in Packager");
            }
            threadCompleted(pi);
        } catch (Throwable e1) {
            _log.error(e1,
                       "WARNING! WARNING!!! DANGER!!! DANGER!!!",
                       "Thread: " + t.getName(),
                       "Error in Exception recovery, Queue is locking up, Server should be restarted, Traceback follows");
        }
    }


    public PackagedReport getReport(Packager packager) {
        PackagedReport report = null;
        PackageInfo info = packager.getPackageInfo();
        if (!info.isCanceled()) {
            try {
                report = (PackagedReport) info.getReport();
            } catch (IllegalPackageStateException e) {
                _log.warn(e, "could not retrieve report, making failed report");
            }
        }
        if (report == null) {
            report = makeFailReport(packager.getID());
        }
        return report;

    }

    private static PackagedReport makeFailReport(String id) {
        PackagedBundle pb[] = {new PackagedBundle(0, 0, 0, 0)};
        pb[0].fail();
        PackagedReport report = new PackagedReport(id, pb, 0, BackgroundState.FAIL);
        report.addMessage("report is canceled or failed");
        return report;
    }

    private synchronized void threadCompleted(PackagerItem pi) {

        pi.setRunning(false);
        pi.setThread(null);

        if (_packagerList.contains(pi)) {
            _packagerList.remove(pi);
        } else {
            _log.warn("could not find packager after it finished running, " +
                              "Queue might be stuck");
        }
        updateStatistics(pi, true, pi.getRequestOwner());
        startNewThreads();
    }

    private void updateStatistics(PackagerItem pi,
                                  boolean queued,
                                  RequestOwner requestOwner) {
        _totalPackage++;
        PackagedReport report = getReport(pi.getPackager());
        long procMS = pi.getMillsSincePackagingStart();
        long waitMS = pi.getMillsSinceSubmited();
        long zipTotal = report.getTotalSizeInByte();
        String id = pi.getID();

        long waitSec = waitMS / 1000;
        if (waitMS % 1000 >= 500) waitSec++;

        String type = queued ? "queued" : "immediate";

        String details = " zTime= " + UTCTimeUtil.getHMSFromMills(procMS) +
                ", wTime= " + UTCTimeUtil.getHMS(waitSec) +
                ", size= " + StringUtils.getSizeAsString(zipTotal, true);
        if (pi.getPackager().isOneFile())  details+=", One file not zipped";

        _log.info("Package ID: " + id,
                  "Packaging completed: " + details);
        _statsLog.stats("package", "type", type, "zipT(sec)", procMS / 1000.0,
                        "waitT(sec)", waitMS / 1000.0, "size(MB)", (double) zipTotal / StringUtils.MEG, details);
    }


// =====================================================================
// -------------------- Inner Classes ----------------------------------
// =====================================================================


    private class PackagingThread implements Runnable {

        private PackagerItem _pi;

        PackagingThread(PackagerItem pi) {
            _pi = pi;
        }

        public void run() {
            Packager packager = _pi.getPackager();
            Thread thread = Thread.currentThread();
            PackageMaster.logPIDDebug(_pi.getID(),
                                      "Packager thread: " + thread.getName() + " is beginning");
            doPackaging(packager, ALL);
            PackageMaster.logPIDDebug(_pi.getID(),
                                      "Packager thread: " + thread.getName() + "  has completed");
            threadCompleted(_pi);
        }

    }


    private static class QueueStats {
        private final int _total;
        private final int _totalLarge;
        private final int _totalSmall;
        private final long _longestWait;

        public QueueStats(List<PackagerItem> packagerList) {
            int large = 0;
            int small = 0;
            long longWait = 0;
            for (PackagerItem pi : packagerList) {
                if (pi.isLarge()) large++;
                else small++;
                longWait = Math.max(longWait, pi.getMillsSinceSubmited());
            }

            _totalLarge = large;
            _totalSmall = small;
            _longestWait = longWait;
            _total = packagerList.size();
        }

        public int getTotal() {
            return _total;
        }

        public int getTotalLarge() {
            return _totalLarge;
        }

        public int getTotalSmall() {
            return _totalSmall;
        }

        public long getLongestWait() {
            return _longestWait;
        }

        public String getLongestWaitStr() {
            return UTCTimeUtil.getHMSFromMills(_longestWait);
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
