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
import edu.caltech.ipac.util.download.FileCacheHelper;
import edu.caltech.ipac.util.download.DownloadEvent;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
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

    private static final Map<BaseNetParams, Object> activeRequest = Collections.synchronizedMap(new HashMap<>());
    private static final Map<BaseNetParams, DownloadProgress> activeListeners = Collections.synchronizedMap(new HashMap<>());

    public static FileInfo serviceWithCacheMsg(ImageServiceParams params, ServiceCaller svcCaller) throws FailedRequestException {
        return lockingRetrieve(params, () -> retrieveImageService(params,svcCaller));
    }

    /**
     * download file with locking, cacheing, and messaging
     * @param uri - accepts a URL, String, S3Ref, or UriRef
     **/
    public static FileInfo downloadWithCacheMsg(Object uri) throws FailedRequestException {
        return downloadWithCacheMsg(uri,null);
    }

    /**
     * download file with locking, cacheing, and messaging
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

    /** download file with locking, cacheing, and messaging */
    public static FileInfo downloadWithCacheMsg(UriRefParams params) throws FailedRequestException {
        return lockingRetrieve(params, () -> RetrieveUtil.downloadCaching(params, makeDownloadProgressOrFind(params)));
    }

//======================================================================
//----------------------- Private Methods ------------------------------
//======================================================================

    private static FileInfo lockingRetrieve(BaseNetParams params, Callable<FileInfo> getter) throws FailedRequestException {
        try {
            Object lockKey= activeRequest.computeIfAbsent(params, k -> new Object());
            synchronized (lockKey) {
                return getter.call();
            }
        } catch (Exception e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        } finally {
            activeRequest.remove(params);
        }
    }

    private static DownloadProgress makeDownloadProgressOrFind(UriRefParams params) { // todo: generalize beyond just plotId
        if (params==null || params.getStatusKey()== null) return null;
        DownloadProgress dl;

        if (activeListeners.containsKey(params)) {
            dl = activeListeners.get(params);
            dl.addPlotId(params.getPlotId());
        }
        else {
            dl= new DownloadProgress(params.getStatusKey(), params.getPlotId());
            activeListeners.put(params, dl);
        }
        return dl;
    }


    private static FileInfo retrieveImageService(ImageServiceParams params, ServiceCaller svcCaller) throws IOException, FailedRequestException {
        FileInfo fileInfo= FileCacheHelper.getFileInfo(params);
        if (fileInfo == null)  {
            fileInfo= svcCaller.retrieve(params, FileCacheHelper.makeFile(params.getUniqueString()+"."+FileUtil.FITS));
            FileCacheHelper.putFileInfo(params,fileInfo);
        }
        return fileInfo;
    }

    private static class DownloadProgress implements DownloadListener {
        private final String key;
        private final List<String> plotIdList= new ArrayList<>();

        DownloadProgress(String key, String plotId) {
            this.key = key;
            plotIdList.add(plotId);
        }

        void addPlotId(String plotId) {plotIdList.add(plotId);}

        public void dataDownloading(DownloadEvent ev) {
            if (key == null) return;
            String offStr = "";
            long current= ev.getCurrent();
            long max= ev.getMax();
            if (max > 0 && current<max) {
                offStr = " of " + FileUtil.getSizeAsString(max,true);
            }
            String messStr = "Retrieved " + FileUtil.getSizeAsString(current,true) + offStr;
            for(var plotId : plotIdList) {
                PlotServUtils.updateProgress(key,plotId, ProgressStat.PType.DOWNLOADING, messStr);
            }
        }
    }
}