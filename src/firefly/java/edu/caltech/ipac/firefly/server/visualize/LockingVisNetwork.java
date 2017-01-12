/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.download.CacheHelper;
import edu.caltech.ipac.util.download.DownloadEvent;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.AnyUrlGetter;
import edu.caltech.ipac.visualize.net.AnyUrlParams;
import edu.caltech.ipac.visualize.net.VisNetwork;
import edu.caltech.ipac.util.download.FileData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
/**
 * User: roby
 * Date: Feb 26, 2010
 * Time: 10:43:21 AM
 */


/**
 * This class will download files via VisNetwork.  However it also locks so that two of the same request do not happen
 * at the same time. This way a file will not overwrite itself during download.
 *
 * @author Trey Roby
 */
public class LockingVisNetwork {

    private static final Map<BaseNetParams, Object> _activeRequest =
            Collections.synchronizedMap(new HashMap<BaseNetParams, Object>());

    public static File getImage(BaseNetParams params) throws FailedRequestException, SecurityException {
        FileInfo fi = lockingRetrieve(params, true);
        return new File(fi.getInternalFilename());
    }

    public static FileInfo getFitsFile(BaseNetParams params) throws FailedRequestException {
        return lockingRetrieve(params, false);
    }

    public static FileInfo getFitsFile(URL url) throws FailedRequestException {
        AnyUrlParams p= new AnyUrlParams(url);
        p.setLocalFileExtensions(Collections.singletonList(FileUtil.FITS));
        return getFitsFile(p);
    }


//======================================================================
//----------------------- Private Methods ------------------------------
//======================================================================

    private static FileInfo lockingRetrieve(BaseNetParams params, boolean unzip)
            throws FailedRequestException, SecurityException {
        FileInfo retval = null;
        try {
            Object lockKey;
            synchronized (_activeRequest) {
                lockKey= _activeRequest.get(params);
                if (lockKey==null) {
                    lockKey= new Object();
                    _activeRequest.put(params,lockKey);
                }
            }
            synchronized (lockKey) {
                DownloadListener dl = null;
                if (params.getStatusKey() != null) { // todo: the download listener has very specific behavior
                                                     // todo: it could be generalized by passing a DownloadListener
                    dl = new DownloadProgress(params.getStatusKey(), params.getPlotid());
                }
                FileData fd;
                if (params instanceof AnyUrlParams) {
                    fd = retrieveURL((AnyUrlParams)params, dl);
                }
                else {
                    fd = VisNetwork.getImage(params, dl);
                }
                File fitsFile = fd.getFile();
                if (unzip) fitsFile = unzip(fitsFile);
                retval = new FileInfo(fitsFile.getPath(), fd.getSuggestedExternalName(), fitsFile.length(), fd.getResponseCode());
            }
        } finally {
            if (params != null) _activeRequest.remove(params);
        }
        return retval;
    }

    private static File unzip(File f) throws FailedRequestException {
        File retval = f;
        if (FileUtil.getExtension(f).equalsIgnoreCase(FileUtil.GZ)) {
            try {
                if (!FileUtil.computeUnzipFileName(f).canRead()) {
                    retval = FileUtil.gUnzipFile(f);
                }
            } catch (IOException e) {
                throw new FailedRequestException("Could not unzip file", "Unzipping failed", e);
            }
        }
        return retval;
    }



    //======================================
    //======================================
    //======================================

    /**
     * Retrieve a file from URL and cache it.  If the URL is a gz file then uncompress it and return the uncompress version.
     * @param params the configuration about the retrieve request
     * @param dl a Download listener, only used in server mode
     * @return a FileData of file returned from this URL.
     * @throws FailedRequestException when request fails
     */
    private static FileData retrieveURL(AnyUrlParams params, DownloadListener dl)
            throws FailedRequestException {
        FileData retval= CacheHelper.getFileData(params);
        File fileName= (retval==null) ? CacheHelper.makeFile(params.getFileDir(), params.getUniqueString()) : retval.getFile();

//        if (retval==null && params.isCompressedFileName()) {  // if we are requesting a gz file then check to see if we cached the unzipped version
//            retval=CacheHelper.getFileData(params.getUncompressedKey());
//            if (retval==null && fileName.canWrite()) fileName.delete(); // this file should not be in the cache in the this case
//        }

        if (retval == null || params.getCheckForNewer())  {          // if not in cache or is in cache & we want to see if there is a newer version
            FileData fd= AnyUrlGetter.lowlevelGetUrlToFile(params,fileName,false,dl);

            CacheKey saveKey= params;
            if (fd.isDownloaded() || retval==null) {
                retval= fd;
                CacheHelper.putFile(saveKey,fd);
            }
        }
        return retval;
    }



    //======================================
    //======================================
    //======================================



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
