/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.DownloadEvent;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.NetParams;
import edu.caltech.ipac.util.download.ResponseMessage;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.net.AnyUrlParams;
import edu.caltech.ipac.visualize.net.ImageServiceParams;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class will download files via VisNetwork.  However, it also locks so that two of the same request do not happen
 * at the same time. This way a file will not overwrite itself during download.
 *
 * @author Trey Roby
 */
public class LockingVisNetwork {

    private static final Map<BaseNetParams, Object> _activeRequest = Collections.synchronizedMap(new HashMap<>());

    public static FileInfo retrieveURL(AnyUrlParams params) throws FailedRequestException {
        return lockingRetrieve(params, null);
    }

    public static FileInfo retrieve(ImageServiceParams params, ServiceCaller svcCaller) throws FailedRequestException {
        return lockingRetrieve(params, svcCaller);
    }

    public static FileInfo retrieveURL(URL url) throws FailedRequestException {
        AnyUrlParams p= new AnyUrlParams(url, url.toString(),null);
        p.setLocalFileExtensions(Collections.singletonList(FileUtil.FITS));
        return retrieveURL(p);
    }

    public static FileInfo retrieve(ServiceCaller srvCaller, String lockKey) throws FailedRequestException {
        ServiceCallerParams p= new ServiceCallerParams(srvCaller, lockKey);
        return lockingRetrieve(p, srvCaller);
    }


//======================================================================
//----------------------- Private Methods ------------------------------
//======================================================================

    private static FileInfo lockingRetrieve(BaseNetParams params, ServiceCaller svcCaller) throws FailedRequestException {
        Objects.requireNonNull(params);
        confirmParamsType(params);
        try {
            Object lockKey= _activeRequest.computeIfAbsent(params, k -> new Object());
            synchronized (lockKey) {
                return (params instanceof AnyUrlParams urlP) ?
                    retrieveURL( urlP, makeDownloadProgress(params) ) :
                        (params instanceof ImageServiceParams isParam) ?
                                retrieveService(isParam, svcCaller) :
                                retrieveServiceCaller((ServiceCallerParams) params);
            }
        } catch (IOException | SecurityException e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        } finally {
            _activeRequest.remove(params);
        }
    }

    private static DownloadProgress makeDownloadProgress(BaseNetParams params) { // todo: generalize beyond just plotId
        if (params.getStatusKey()== null) return null;
        return new DownloadProgress(params.getStatusKey(), params.getPlotId());
    }

    private static void confirmParamsType(NetParams params) throws FailedRequestException {
        if (!(params instanceof ImageServiceParams) && !(params instanceof AnyUrlParams) && !(params instanceof ServiceCallerParams)) {
            throw new FailedRequestException("Unrecognized Param Type");
        }
    }

    private static FileInfo retrieveService(ImageServiceParams params, ServiceCaller svcCaller) throws IOException, FailedRequestException {
        FileInfo fileInfo= CacheHelper.getFileInfo(params);
        if (fileInfo == null)  {
            fileInfo= svcCaller.retrieve(params,CacheHelper.makeFitsFile(params));
            CacheHelper.putFileInfo(params,fileInfo);
        }
        return fileInfo;
    }

    private static FileInfo retrieveServiceCaller(ServiceCallerParams params) throws IOException, FailedRequestException {
        return params.getSvcCaller().retrieve(null,null);
    }

    //======================================
    //======================================
    //======================================

    /**
     * Retrieve a file from URL and cache it.  If the URL is a gz file then uncompress it and return the uncompress version.
     * @param params the configuration about the retrieve request
     * @param dl a Download listener, only used in server mode
     * @return a FileInfo of file returned from this URL.
     * @throws FailedRequestException when request fails
     */
    private static FileInfo retrieveURL(AnyUrlParams params, DownloadListener dl) throws FailedRequestException {
        FileInfo fileInfo= CacheHelper.getFileInfo(params);
        if (fileInfo!=null && !params.getCheckForNewer()) return fileInfo;

        try {
            File fileName= (fileInfo==null) ? CacheHelper.makeFile(params.getFileDir(), params.getUniqueString()) : fileInfo.getFile();
            var ops= URLDownload.Options.listenerOp(params.getMaxSizeToDownload(), dl);
            fileInfo= URLDownload.getDataToFile(params.getURL(), fileName, params.getCookies(), params.getHeaders(), ops);
            if (fileInfo.getResponseCode()==200) CacheHelper.putFileInfo(params,fileInfo);
            return fileInfo;
        } catch (Exception e) {
            Logger.warn(e.toString());
            throw ResponseMessage.simplifyNetworkCallException(e);
        }
    }

    public interface ServiceCaller {
        FileInfo retrieve(CanCallService p, File suggestedFile) throws  IOException, FailedRequestException;
    }

    public interface CanCallService {};


    private static class DownloadProgress implements DownloadListener {

        private final String _key;
        private final String _plotId;

        DownloadProgress(String key, String plotId) {
            _key = key;
            _plotId = plotId;
        }

        public void dataDownloading(DownloadEvent ev) {
            if (_key == null) return;
            String offStr = "";
            long current= ev.getCurrent();
            long max= ev.getMax();
            if (max > 0 && current<max) {
                offStr = " of " + FileUtil.getSizeAsString(ev.getMax(),true);
            }
            String messStr = "Retrieved " + FileUtil.getSizeAsString(current,true) + offStr;
            PlotServUtils.updateProgress(_key, _plotId, ProgressStat.PType.DOWNLOADING, messStr);
        }
    }

    private static class ServiceCallerParams extends BaseNetParams implements CanCallService {

        private final String lockKey;
        private final ServiceCaller svcCaller;

        public ServiceCallerParams (ServiceCaller svcCaller, String lockKey) {
            super("","");
            this.lockKey= lockKey;
            this.svcCaller= svcCaller;
        }

        public ServiceCaller getSvcCaller() {return svcCaller;}

        @Override
        public String getUniqueString() { return lockKey; }
    }
}
