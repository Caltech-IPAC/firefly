/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.ProgressStat;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.download.DownloadEvent;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.FileCacheHelper;
import edu.caltech.ipac.util.download.ResponseMessage;
import edu.caltech.ipac.util.download.RetrieveUtil;
import edu.caltech.ipac.util.download.RetrieveUtil.ServiceCaller;
import edu.caltech.ipac.util.download.UriRef;
import edu.caltech.ipac.util.download.UriRefParams;
import edu.caltech.ipac.visualize.net.ImageServiceParams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

;

/**
 * This class will download files.  However, it also locks so that two of the same request do not happen
 * at the same time. This way a file will not overwrite itself during download.
 * The close does the following: locks, notifies client, caches and downloads any UriRef
 *
 * @author Trey Roby
 */
public class LockingRetrieve {

    private static final Map<BaseNetParams, LockEntry> activeRequest = Collections.synchronizedMap(new HashMap<>());
    private static final Map<BaseNetParams, DownloadProgress> activeListeners = Collections.synchronizedMap(new HashMap<>());

    public static FileInfo serviceWithCacheMsg(ImageServiceParams params, ServiceCaller svcCaller) throws FailedRequestException {
        return lockingRetrieve(params, () -> retrieveImageService(params,svcCaller));
    }

    /**
     * download file with locking, caching, and messaging
     * @param uri - accepts a URL, String, S3Ref, or UriRef
     **/
    public static FileInfo downloadWithCacheMsg(Object uri) throws FailedRequestException {
        return downloadWithCacheMsg(uri,null);
    }

    /**
     * download a uri with locking, caching, and messaging
     * @param uri - accepts a URL, String, S3Ref, or UriRef
     **/
    public static FileInfo downloadWithCacheMsg(Object uri, File downloadDir) throws FailedRequestException {
        UriRef ref= UriRef.make(uri);
        if (ref == null) throw new FailedRequestException("uri is not valid much be URL, String, S3Ref, or UriRef");
        var params= new UriRefParams(ref,downloadDir);
        params.setMaxSizeToDownload(VisContext.FITS_MAX_SIZE);
        params.setExpectStaticFile(true);
        params.setStatusKey(uri.toString());
        return downloadWithCacheMsg(params);
    }

    /** download a uri with locking, caching, and messaging */
    public static FileInfo downloadWithCacheMsg(UriRefParams params) throws FailedRequestException {
        return lockingRetrieve(params, () -> RetrieveUtil.downloadCaching(params, makeDownloadProgressOrFind(params)));
    }

    public static FileInfo lockingRetrieve(BaseNetParams params, Callable<FileInfo> getter) throws FailedRequestException {
        LockEntry lockEntry= getLockEntry(params);
        try {
            synchronized (lockEntry) {
                return getter.call();
            }
        } catch (Exception e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        } finally {
            releaseLockEntry(params, lockEntry);
        }
    }


    private static LockEntry getLockEntry(BaseNetParams params) {
        synchronized (activeRequest) {
            LockEntry entry = activeRequest.computeIfAbsent(params, k -> new LockEntry());
            entry.refCount++;
            return entry;
        }
    }

    /** Atomically decrement the refcount, and remove the map entry only if this was the last holder. */
    private static void releaseLockEntry(BaseNetParams params, LockEntry entry) {
        synchronized (activeRequest) {
            entry.refCount--;
            if (entry.refCount <= 0) activeRequest.remove(params, entry);
        }
    }



    public static boolean isActiveRequest(BaseNetParams params) { return activeRequest.containsKey(params); }
    public static boolean isActiveRequest(String urlStr) { return isActiveRequest(new UriRefParams(urlStr)); }

    public static DownloadListener makeDownloadProgressOrFind(UriRefParams params) { // todo: generalize beyond just plotId
        if (params==null || params.getStatusKey()== null) return null;
        synchronized (activeListeners) {
            DownloadProgress dl = activeListeners.get(params);
            if (dl!=null) {
                dl.addEntry(params);
            }
            else {
                dl= new DownloadProgress(params) {
                    public void downloadDone() { activeListeners.remove(params); }
                };
                activeListeners.put(params, dl);
            }
            return dl;
        }
    }

//======================================================================
//----------------------- Private Methods ------------------------------
//======================================================================

    private static FileInfo retrieveImageService(ImageServiceParams params, ServiceCaller svcCaller) throws IOException, FailedRequestException {
        FileInfo fileInfo= FileCacheHelper.getFileInfo(params);
        if (fileInfo == null)  {
            fileInfo= svcCaller.retrieve(params, FileCacheHelper.makeFile(params.getUniqueString()+"."+FileUtil.FITS));
            FileCacheHelper.putFileInfo(params,fileInfo);
        }
        return fileInfo;
    }

    private static abstract class DownloadProgress implements DownloadListener {
        private record ListenerEntry(String key, String id) {}
        private final List<ListenerEntry> entryList = new ArrayList<>();

        DownloadProgress(BaseNetParams params) { addEntry(params); }

        synchronized void addEntry(BaseNetParams params) {
            if (!params.getNotify()) return;
            entryList.add(new ListenerEntry(params.getStatusKey(),params.getId()));
        }

        /**
         * call the listeners for each plot id. We don't care about synchronization when reading the list since the next
         * call will fix it. Also, we don't want to sync when reading because that will be done very often
         * @param ev
         */
        public void dataDownloading(DownloadEvent ev) {
            if (entryList.isEmpty()) return;
            long current= ev.getCurrent();
            long max= ev.getMax();
            var offStr= (max > 0 && current<max) ? " of " + FileUtil.getSizeAsString(max,true) : "";
            String messStr = "Retrieved " + FileUtil.getSizeAsString(current,true) + offStr;
            entryList.forEach(entry->
                    PlotServUtils.updateProgress(entry.key,entry.id, ProgressStat.PType.DOWNLOADING, messStr) );
        }
    }

    private static final class LockEntry {
            int refCount = 0;
    }
}