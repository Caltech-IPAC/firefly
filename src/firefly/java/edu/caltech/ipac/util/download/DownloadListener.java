/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import java.util.EventListener;

/**
 * A listener that is called during a download with status
 * @author Trey Roby
 */
public interface DownloadListener extends EventListener {
    void dataDownloading(DownloadEvent ev);
    default void beginDownload(DownloadEvent ev) {};
    default void downloadCompleted(DownloadEvent ev) {};
}



