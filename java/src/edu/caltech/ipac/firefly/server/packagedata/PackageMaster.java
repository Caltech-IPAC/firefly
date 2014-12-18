package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.JobAttributes;
import edu.caltech.ipac.firefly.core.background.PackageProgress;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
/**
 * User: roby
 * Date: Sep 24, 2008
 * Time: 1:17:59 PM
 */

/**
 * @author Trey Roby
 */
public class PackageMaster  {

    public static final long ASYNC_SIZE= AppProperties.getLongProperty("download.async.bytesize", 50*FileUtil.MEG);
    public final static int WARNING_FILE_LIST_SIZE = AppProperties.getIntProperty("download.warning.fileListTask.size", 15);
    public final static int HEAVY_LOAD_CNT = (int)(WARNING_FILE_LIST_SIZE * 0.66F);
    private static long DEFAULT_MAX_BUNDLE_BYTES = AppProperties.getLongProperty("download.bundle.maxbytes", 107374182l);

    public static final int WAIT_MILLS= 3000;
    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final AtomicLong _fileCounterTask= new AtomicLong(0) ;

    public BackgroundStatus packageData(DownloadRequest req,
                                        SearchProcessor<List<FileGroup>> processor) {

        PackagingWorker worker= new PackagingWorker(processor,req);
        BackgroundEnv.BackgroundProcessor backProcess=
                new BackgroundEnv.BackgroundProcessor( worker,  req.getBaseFileName(),
                                                       req.getTitle(),req.getEmail(),
                                                       req.getDataSource(),
                                                       ServerContext.getRequestOwner());
        BackgroundStatus bgStat= BackgroundEnv.backgroundProcess(WAIT_MILLS,backProcess);
        checkForLongQueue(bgStat);
        return bgStat;

    }

    private void checkForLongQueue(BackgroundStatus bgStat) {
        PackagingController pControl= PackagingController.getInstance();
        if (bgStat.isActive() && pControl.isQueueLong() ||
                _fileCounterTask.get() > WARNING_FILE_LIST_SIZE ) {
            bgStat.addAttribute(JobAttributes.LongQueue);
        }
    }

    public static void logPIDDebug(BackgroundStatus bgStat, String... s) {
        List<String> sList= new ArrayList<String>(s.length+15);
        sList.addAll(Arrays.asList(s));
        if (bgStat.getBackgroundType()== BackgroundStatus.BgType.PACKAGE) {
            sList.addAll(createPackageStatusReport(bgStat));
        }
        else {
            sList.add(bgStat.toString());
        }
        logPIDDebug(bgStat.getID(),sList.toArray(new String[sList.size()]));
    }



    private static List<String> createPackageStatusReport(BackgroundStatus bg) {
        ArrayList<String> retval = new ArrayList<String>(20);

        if (bg.getBackgroundType()!= BackgroundStatus.BgType.PACKAGE) return retval;

        if (isOnePackagedFile(bg)) {
            retval.add("Package Report: " + bg.getID() + ", 1 File, state: " + bg.getState() + ", sizeInBytes: " + bg.getTotalSizeInBytes());
        }
        else {
            int messCnt = bg.getNumMessages();
            retval.add("Package Report: "+bg.getID() +", state: "+ bg.getState()+ ", msg cnt: " + messCnt+
                               ", sizeInBytes: " + bg.getTotalSizeInBytes() );
            retval.add("PackagedBundle cnt: " + bg.getPackageCount());
            StringBuilder bundleStr;
            PackagedBundle b;
            int i= 0;
            for (PackageProgress p : bg.getPartProgressList()) {
                bundleStr = new StringBuilder(100);
                bundleStr.append("            #");
                bundleStr.append(i).append(" - ");
                bundleStr.append("   files: ").append(p.getTotalFiles());
                retval.add(bundleStr.toString());
                i++;
            }
        }

        return retval;
    }


    public static boolean isOnePackagedFile(BackgroundStatus bg) {
        boolean retval= false;
        if ((bg.getPackageCount())==1) {
            retval= bg.getPartProgress(0).getTotalFiles()==1;
        }
        return retval;
    }




    public static void logPIDDebug(String packageID, String... s) {
        logPID(packageID,false,s);
    }

    public static void logPIDWarn(String packageID, String... s) {
        logPID(packageID,true,s);
    }


    private static void logPID(String packageID,
                               boolean warn,
                               String... s) {
        String outAry[]= new String[s.length+1];
        outAry[0]= "Package ID: " + packageID;
        System.arraycopy(s,0,outAry,1,s.length);
        if (warn) _log.warn(outAry);
        else      _log.debug(outAry);
    }


    public static long getMaxBundleSize(DownloadRequest req) {
        long maxBundle= PackageMaster.DEFAULT_MAX_BUNDLE_BYTES;
        if (req!=null && req.containsParam(DownloadRequest.MAX_BUNDLE_SIZE)) {
            maxBundle= req.getMaxBundleSize();
        }
        return maxBundle;
    }


    //======================================================================
//------------------ Package Methods -----------------------------------
//======================================================================


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private static BackgroundStatus doPackage(BackgroundEnv.BackgroundProcessor p,
                                              List<FileGroup> fgList,
                                              long maxBundleBytes) {

        BackgroundStatus bgStat;
        BackgroundInfoCacher piCacher= p.getPiCacher();
        Packager packager= new Packager(p.getBID(), fgList, p.getDataSource(), piCacher, maxBundleBytes);
        PackagingController pControl= PackagingController.getInstance();

        if (mayPackageImmediately(fgList)) {
            bgStat= pControl.doImmediatePackaging(packager,p.getRequestOwner());
            logPIDDebug(bgStat,"package immediately completed");
        }
        else {
            bgStat= packager.estimate();
            pControl.queue(packager,p.getRequestOwner());
            logPIDDebug(bgStat,"package queued in background");
        }

        return bgStat;
    }


    /**
     * determine whether is do immediate packaging or to do it in the background.
     * @param fgs list of file groups
     * @return true if we can package right now, false to package in the background
     */
    private static boolean mayPackageImmediately(List<FileGroup> fgs) {
        long totalSize = 0;
        for (FileGroup fg : fgs) {
            totalSize += fg.getSizeInBytes();
        }
        return (totalSize < ASYNC_SIZE);
    }


// =====================================================================
// -------------------- Interfaces -------------------------------------
// =====================================================================

//    public interface FileGroupCreator {
        /**
         * This method should compute the FileGroup list the first time it is called.
         * However, it may be call
         * repeatiatly so it should compute the list and save it so future calls to not
         * require a another database hit or computation.
         * @return a list of FileGroup
         * @throws Exception this method may throw any exception
         */
//        List<FileGroup> makeFileGroup() throws Exception;
//
//    }


        // =====================================================================
        // -------------------- Inner Classes -------------------------------------
        // =====================================================================

     private static class PackagingWorker implements BackgroundEnv.Worker {
         private final SearchProcessor<List<FileGroup>> _processor;
         private final DownloadRequest _request;

         public PackagingWorker(SearchProcessor<List<FileGroup>> processor,
                                DownloadRequest request) {
             _processor= processor;
             _request= request;
         }

         public BackgroundStatus work(BackgroundEnv.BackgroundProcessor p)  throws Exception {
             BackgroundStatus retval;
             try {
                 long cnt= _fileCounterTask.getAndIncrement();
                 if (cnt >= HEAVY_LOAD_CNT) {
                     _log.warn("Heavy load warning: There are currently " +cnt+ " threads running to compute package file list.",
                               "File list are computed before the package request is queued");
                 }
                 List<FileGroup> result= _processor.getData(_request);

                 _fileCounterTask.getAndDecrement();
                 String baseFileName = p.getBaseFileName();
                 if (!_request.getBaseFileName().equals(baseFileName)) {
                     baseFileName = _request.getBaseFileName(); // change baseFileName if a FileGroupsProcessor updated it.
                     p.getPiCacher().setBaseFileName(baseFileName);
                 }
                 retval= doPackage(p, result, getMaxBundleSize(_request));
             } catch (ClassCastException e) {
                 retval= new BackgroundStatus(p.getBID(), BackgroundState.FAIL);
                 retval.addMessage("Invalid processor mapping.  Return value is not of type List<FileGroup>.");
                 _log.error(e, "Invalid processor mapping.  Return value is not of type List<FileGroup>.");

             }
             return retval;
         }
     }

}
