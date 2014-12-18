package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.util.download.BaseNetParams;
import edu.caltech.ipac.util.download.DownloadEvent;
import edu.caltech.ipac.util.download.DownloadListener;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.VetoDownloadException;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.net.AnyFitsParams;
import edu.caltech.ipac.visualize.net.VisNetwork;

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
        return getFitsFile(new AnyFitsParams(url));
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
                if (params.getStatusKey() != null) dl = new DownloadProgress(params.getStatusKey());
                edu.caltech.ipac.util.download.FileData fd[] = VisNetwork.getImage(params, dl);
                File fitsFile = fd[0].getFile();
                if (unzip) fitsFile = unzip(fitsFile);
                retval = new FileInfo(fitsFile.getPath(), fd[0].getSugestedExternalName(), fitsFile.length());
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

    private static class DownloadProgress implements DownloadListener {

        private final String _key;

        DownloadProgress(String key) {
            _key = key;
        }

        public void dataDownloading(DownloadEvent ev) {
            if (_key != null) {
                String offStr = "";
                if (ev.getMax() > 0) {
                    offStr = " of " + FileUtil.getSizeAsString(ev.getMax());
                }
                String messStr = "Downloaded " + FileUtil.getSizeAsString(ev.getCurrent()) + offStr;
                PlotServUtils.updateProgress(_key, ProgressStat.PType.DOWNLOADING, messStr);
            }
        }

        public void beginDownload(DownloadEvent ev) {/*not used*/ }

        public void downloadCompleted(DownloadEvent ev) {/*not used*/ }

        public void downloadAborted(DownloadEvent ev) {/*not used*/ }

        public void checkDataDownloading(DownloadEvent ev) throws VetoDownloadException {/*not used*/ }
    }

}
