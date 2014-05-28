package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
import edu.caltech.ipac.firefly.server.RequestOwner;
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

    public BackgroundReport packageData(DownloadRequest req,
                                        SearchProcessor<List<FileGroup>> processor) {
        PackagingWorker worker= new PackagingWorker(processor,req);
        BackgroundEnv.BackgroundProcessor backProcess=
                new BackgroundEnv.BackgroundProcessor( worker,  req.getBaseFileName(),
                                                       req.getTitle(),req.getEmail(),
                                                       req.getDataSource(),
                                                       ServerContext.getRequestOwner());
        BackgroundReport rep= BackgroundEnv.backgroundProcess(WAIT_MILLS,backProcess);
        checkForLongQueue(rep);
        return rep;

    }

    private void checkForLongQueue(BackgroundReport rep) {
        PackagingController pControl= PackagingController.getInstance();
        if (!rep.isDone() && !rep.isFail() &&
                pControl.isQueueLong() ||
                _fileCounterTask.get() > WARNING_FILE_LIST_SIZE ) {
            rep.addAttribute(BackgroundReport.JobAttributes.LongQueue);
        }

    }

    public static void logPIDDebug(BackgroundReport report,
                                   String... s) {
        List<String> sList= new ArrayList<String>(s.length+15);
        sList.addAll(Arrays.asList(s));
        if (report instanceof PackagedReport) {
            sList.addAll(Arrays.asList(((PackagedReport)report).toStringAry()));
        }
        else {
            sList.add(report.toString());
        }
        logPIDDebug(report.getID(),sList.toArray(new String[sList.size()]));
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


    private static PackagedReport doPackage(String packageID,
                                            String dataSource,
                                            List<FileGroup> fgList,
                                            String baseFileName,
                                            String title,
                                            String email,
                                            long   maxBundleBytes,
                                            RequestOwner requestOwner) {

        PackageInfoCacher pi= new PackageInfoCacher(BackgroundEnv.getCache(),packageID, email, baseFileName, title);
        Packager packager= new Packager(packageID, fgList, dataSource, pi, maxBundleBytes);
        PackagedReport report;
        PackagingController pControl= PackagingController.getInstance();

        if (mayPackageImmediately(fgList)) {
            report= pControl.doImmediatePackaging(packager,requestOwner);
            logPIDDebug(report,"package immediately completed");
        }
        else {
            report= packager.estimate();
            pControl.queue(packager,requestOwner);
            logPIDDebug(report,"package queued in background");
        }

        return report;
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

         public BackgroundReport work(BackgroundEnv.BackgroundProcessor p)  throws Exception {
             BackgroundReport retval;
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
                 }
                 retval= doPackage(p.getBID(), p.getDataSource(),
                                   result, baseFileName,
                                   p.getTitle(), p.getEmail(),
                                   getMaxBundleSize(_request),
                                   p.getRequestOwner() );
             } catch (ClassCastException e) {
                 retval= BackgroundReport.createFailReport(p.getBID(),
                                                      "Invalid processor mapping.  Return value is not of type List<FileGroup>." );
                 _log.error(e, "Invalid processor mapping.  Return value is not of type List<FileGroup>.");

             }
             return retval;
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
