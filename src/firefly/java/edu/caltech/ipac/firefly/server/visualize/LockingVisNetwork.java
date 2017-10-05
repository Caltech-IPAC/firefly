/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.util.ClientLog;
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
import edu.caltech.ipac.visualize.net.AtlasImageGetter;
import edu.caltech.ipac.visualize.net.DssImageGetter;
import edu.caltech.ipac.visualize.net.DssImageParams;
import edu.caltech.ipac.visualize.net.IbeImageGetter;
import edu.caltech.ipac.visualize.net.ImageServiceParams;
import edu.caltech.ipac.visualize.net.IrsaImageGetter;
import edu.caltech.ipac.visualize.net.IrsaImageParams;
import edu.caltech.ipac.visualize.net.SloanDssImageGetter;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class will download files via VisNetwork.  However it also locks so that two of the same request do not happen
 * at the same time. This way a file will not overwrite itself during download.
 *
 * @author Trey Roby
 */
public class LockingVisNetwork {

    private static final Map<BaseNetParams, Object> _activeRequest =
            Collections.synchronizedMap(new HashMap<BaseNetParams, Object>());


    public static FileInfo retrieve(BaseNetParams params) throws FailedRequestException {
        return lockingRetrieve(params, false);
    }

    public static FileInfo retrieve(URL url) throws FailedRequestException {
        AnyUrlParams p= new AnyUrlParams(url);
        p.setLocalFileExtensions(Collections.singletonList(FileUtil.FITS));
        return retrieve(p);
    }


//======================================================================
//----------------------- Private Methods ------------------------------
//======================================================================

    private static FileInfo lockingRetrieve(BaseNetParams params, boolean unzip) throws FailedRequestException {
        Objects.requireNonNull(params);
        FileInfo retval;
        try {
            Object lockKey= _activeRequest.computeIfAbsent(params, k -> new Object());
            synchronized (lockKey) {
                DownloadListener dl = null;
                if (params.getStatusKey() != null) { // todo: the download listener has very specific behavior
                                                     // todo: it could be generalized by passing a DownloadListener
                    dl = new DownloadProgress(params.getStatusKey(), params.getPlotid());
                }
                FileInfo fd= fullFillRequest(params, dl);

                if (unzip) retval= new FileInfo(unzip(fd.getFile()),fd.getExternalName(),fd.getResponseCode(), fd.getResponseCodeMsg());
                else       retval= fd;

            }
        } catch (IOException | SecurityException e) {
            throw ResponseMessage.simplifyNetworkCallException(e);
        } finally {
            _activeRequest.remove(params);
        }
        return retval;
    }

    private static File unzip(File f) throws IOException, FailedRequestException {
        File retval = f;
        if (FileUtil.getExtension(f).equalsIgnoreCase(FileUtil.GZ)) {
            if (!FileUtil.computeUnzipFileName(f).canRead()) {
                retval = FileUtil.gUnzipFile(f);
            }
        }
        return retval;
    }

    private static FileInfo fullFillRequest(NetParams params, DownloadListener dl) throws IOException, FailedRequestException {
        if (params instanceof AnyUrlParams) {
            return retrieveURL((AnyUrlParams)params, dl);
        }
        else if (params instanceof ImageServiceParams) {
            File f= CacheHelper.getFile(params);
            if (f == null)  {
                f= callService((ImageServiceParams)params);
                CacheHelper.putFile(params,f);
            }
            return new FileInfo(f);
        }
        else {
            throw new FailedRequestException("Unrecognized Param Type");
        }
    }


    private static File callService(ImageServiceParams params) throws  IOException, FailedRequestException {
        File f= CacheHelper.makeFitsFile(params);
        ImageServiceParams.ImageSourceTypes type= params.getType();

        switch (type ) {

            case ISSA:
            case IRIS:
            case MSX:
                IrsaImageGetter.lowlevelGetIrsaImage((IrsaImageParams) params, f);
                break;
            case TWOMASS:
            case TWOMASS6:
                f= IbeImageGetter.lowlevelGetIbeImage(params);
                break;
            case WISE:
                f= IbeImageGetter.lowlevelGetIbeImage(params);
                break;
            case DSS:
                DssImageGetter.lowlevelGetDssImage((DssImageParams)params, f);
                break;
            case SDSS:
                SloanDssImageGetter.lowlevelGetSloanDssImage((SloanDssImageParams)params, f);
                break;
            case ATLAS:
                f= AtlasImageGetter.lowlevelGetIbe2Image(params);
                break;
        }
        return f;

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
        FileInfo fileInfo= CacheHelper.getFileData(params);
        if (fileInfo!=null && !params.getCheckForNewer()) return fileInfo;

        try {
            File fileName= (fileInfo==null) ? CacheHelper.makeFile(params.getFileDir(), params.getUniqueString()) : fileInfo.getFile();
            fileInfo= URLDownload.getDataToFile(params.getURL(), fileName, params.getCookies(), null,
                                                dl, false,true, params.getMaxSizeToDownload());
            if (fileInfo.getResponseCode()==200) CacheHelper.putFile(params,fileInfo);
            return fileInfo;
        } catch (Exception e) {
            ClientLog.warning(e.toString());
            throw ResponseMessage.simplifyNetworkCallException(e);
        }
    }



    private static class DownloadProgress implements DownloadListener {

        private final String _key;
        private final String _plotId;

        DownloadProgress(String key, String plotId) {
            _key = key;
            _plotId = plotId;
        }

        public void dataDownloading(DownloadEvent ev) {
            if (_key != null) {
                String offStr = "";
                if (ev.getMax() > 0) {
                    offStr = " of " + FileUtil.getSizeAsString(ev.getMax());
                }
                String messStr = "Retrieved " + FileUtil.getSizeAsString(ev.getCurrent()) + offStr;
                PlotServUtils.updatePlotCreateProgress(_key, _plotId, ProgressStat.PType.DOWNLOADING, messStr);
            }
        }

        public void beginDownload(DownloadEvent ev) {/*not used*/ }

        public void downloadCompleted(DownloadEvent ev) {/*not used*/ }
    }

}
