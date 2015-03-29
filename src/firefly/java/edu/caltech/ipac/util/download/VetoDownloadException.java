/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

/**
 * This exception is thrown when a download is stop.
 * @author Trey Roby
 */
public class VetoDownloadException extends Exception {

    /**
     * Create a new VetoDownload Exception.
     * @param mess the error message.
     */
    public VetoDownloadException(String mess) { super(mess);  }
}


