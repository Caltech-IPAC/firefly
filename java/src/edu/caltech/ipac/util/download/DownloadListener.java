package edu.caltech.ipac.util.download;

import java.util.EventListener;

/**
 * A listener that is called durring a download with status
 * @author Trey Roby
 */
public interface DownloadListener extends EventListener {
    public abstract void dataDownloading(DownloadEvent ev);
    public abstract void beginDownload(DownloadEvent ev);
    public abstract void downloadCompleted(DownloadEvent ev);
    public abstract void downloadAborted(DownloadEvent ev);
    public abstract void checkDataDownloading(DownloadEvent ev) throws VetoDownloadException;
}



