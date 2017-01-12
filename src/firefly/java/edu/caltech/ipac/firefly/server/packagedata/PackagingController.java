/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
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
    private final List<PackagerItem> _packagerList = new LinkedList<PackagerItem>();
    private volatile int _activeThreads = 0;
    private volatile int _activeLargeThreads = 0;
    private volatile int _queueHighWater = 0;
    private volatile long _totalPackage = 0;
    private volatile long _totalImmediatePackage = 0;

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

    private static BackgroundStatus makeFailStatus(String id) {
        BackgroundStatus retval= new BackgroundStatus(id, BackgroundState.FAIL, BackgroundStatus.BgType.PACKAGE);
        retval.addAttribute(JobAttributes.Zipped);
        retval.addMessage("report is canceled or failed");
        return retval;
    }

    public synchronized void queue(Packager packager,
                                   RequestOwner requestOwner) {

        _packagerList.add(new PackagerItem(packager, requestOwner));
        int len = _packagerList.size();
        _queueHighWater = Math.max(_queueHighWater, len);
        startNewThreads();
    }

    public BackgroundStatus doImmediatePackaging(Packager packager,
                                               RequestOwner requestOwner) {
        PackagerItem pi = new PackagerItem(packager, requestOwner);
        BackgroundStatus bgStat = null;
        try {
            pi.markStartTime();
            bgStat = doPackaging(packager);
            _totalImmediatePackage++;
        } finally {
            updateStatistics(pi, false);
        }
        return bgStat;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    public boolean isQueueLong() {
        return (getQueueSize() > WARNING_QUEUE_SIZE);
    }

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

//    private int countLarge() {
//        int large= 0;
//        for(PackagerItem pi : _packagerList) {
//           if (pi.isLarge()) large++;
//        }
//        return large;
//    }

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

    private BackgroundStatus doPackaging(Packager packager) {
        return doPackaging(packager, ALL);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private BackgroundStatus doPackaging(Packager packager, int idx) {
        if (idx == ALL) {
            packager.packageAll();
        } else {
            packager.packageElement(idx);
        }

        BackgroundInfoCacher info = packager.getBackgroundInfoCacher();
        BackgroundStatus bgStat = getStatus(packager);

        if (!info.isCanceled()) {
            if (bgStat.isDone() && bgStat.hasAttribute(JobAttributes.CanSendEmail)) {
                String email = info.getEmailAddress();
                if (email != null) PackagedEmail.send(email, info, bgStat);
            }
        } else {
            bgStat = makeFailStatus(packager.getID());
        }
        return bgStat;

    }

    private synchronized void threadCompletedWithException(PackagerItem pi,
                                                           Thread t,
                                                           Throwable e) {


        try {
            String id = pi.getID();
            _log.error(e, "Package ID: " + id,
                       "Thread: " + t.getName(),
                       "Thread aborted because of exception: " + e.toString(),
                       "Traceback follows");
            BackgroundInfoCacher backgroundInfoCacher = pi.getPackager().getBackgroundInfoCacher();
            BackgroundStatus bgStat = getStatus(pi.getPackager()).cloneWithState(BackgroundState.FAIL);

            bgStat.addMessage("Contact SSC: " + e.toString());
            if (!backgroundInfoCacher.isCanceled()) {
                backgroundInfoCacher.setStatus(bgStat);
            } else {
                _log.warn("Package ID: " + id,
                          "Thread: " + t.getName(),
                          "BackgroundInfo is already canceled in Packager");
            }
            threadCompleted(pi);
        } catch (Throwable e1) {
            _log.error(e1,
                       "WARNING! WARNING!!! DANGER!!! DANGER!!!",
                       "Thread: " + t.getName(),
                       "Error in Exception recovery, Queue is locking up, Server should be restarted, Traceback follows");
        }
    }

    public BackgroundStatus getStatus(Packager packager) {
        BackgroundStatus bgStat = null;
        BackgroundInfo info = packager.getPackageInfo();
        if (!info.isCanceled()) bgStat = info.getStatus();
        if (bgStat == null)     bgStat= makeFailStatus(packager.getID());
        return bgStat;

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
        updateStatistics(pi, true);
        startNewThreads();
    }

    private void updateStatistics(PackagerItem pi, boolean queued) {
        _totalPackage++;
        BackgroundStatus bgStat = getStatus(pi.getPackager());
        long procMS = pi.getMillsSincePackagingStart();
        long waitMS = pi.getMillsSinceSubmited();
        long zipTotal = bgStat.getTotalSizeInBytes();
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
}

