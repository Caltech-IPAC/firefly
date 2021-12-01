/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.data.FileInfo;

/**
 * This exception is thrown when a value is out of range.
 * @author Trey Roby
 * @version $Id: FailedRequestException.java,v 1.4 2011/05/17 23:48:33 roby Exp $
 */
public class FailedRequestException extends Exception {
    private final String detailMessage;
    private int responseCode= -1;
    private FileInfo fileInfo= null;

    /**
     * Create a new FailedRequestException Exception.
     * @param userMessage the error message.
     */
    public FailedRequestException(String userMessage) {
        this(userMessage, "", null);
    }

    /**
     * Create a new FailedRequestException Exception.
     * @param userMessage the error message.
     * @param detailMessage the user message
     * @param responseCode the http(s) response code if it exist otherwise -1
     */
    public FailedRequestException(String userMessage, String detailMessage, int responseCode) {
        this(userMessage, detailMessage, null);
        this.responseCode= responseCode;
    }

    public FailedRequestException(String userMessage, String detailMessage, int responseCode, FileInfo fi) {
        this(userMessage, detailMessage, null);
        this.responseCode= responseCode;
        this.fileInfo= fi;
    }

    /**
     * Create a new FailedRequestException Exception.
     * @param mess the error message.
     * @param detailMessage the user message
     */
    public FailedRequestException(String mess, String detailMessage) {
        this(mess, detailMessage, null);
    }

    /**
     * Create a new FailedRequestException Exception. The detail message will be the cause message
     * @param mess the error message.
     * @param e the original Exception
     */
    public FailedRequestException(String mess, Throwable e) {
        this(mess,e!=null ? e.getMessage() : "", e);
    }

    /**
     * Create a new FailedRequestException Exception. The detail message will be the cause message
     * @param mess the error message.
     * @param e the original Exception
     * @param responseCode the http(s) response code if it exist otherwise -1
     */
    public FailedRequestException(String mess, Throwable e, int responseCode) {
        this(mess,e);
        this.responseCode= responseCode;
    }

    /**
     * Create a new FailedRequestException Exception.
     * @param mess the error message.
     * @param detailMessage the user message
     * @param e the original Exception
     */
    public FailedRequestException(String mess, String detailMessage, Throwable e) {
        super(mess,e);
        this.detailMessage = detailMessage;
    }

    public String getUserMessage()       { return getMessage(); }
    public String getDetailMessage()     { return detailMessage; }
    public int    getResponseCode()      { return responseCode; }
    public FileInfo getFileInfo()        { return fileInfo; }
}
