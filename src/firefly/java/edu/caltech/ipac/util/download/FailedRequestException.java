/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

/**
 * This exception is thrown when a value is out of range.
 * @author Trey Roby
 * @version $Id: FailedRequestException.java,v 1.4 2011/05/17 23:48:33 roby Exp $
 */
public class FailedRequestException extends Exception {

    static public final String SERVICE_FAILED=  "Service Failed";
    private String     _detailMessage;

    /**
     * Create a new FailedRequestException Exception.
     * @param userMessage the error message.
     */
    public FailedRequestException(String userMessage) {
        this(userMessage, null);
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
     * Create a new FailedRequestException Exception.
     * @param mess the error message.
     * @param detailMessage the user message
     * @param e the original Exception
     */
    public FailedRequestException(String    mess, 
                                  String    detailMessage, 
                                  Throwable e) {
        
        this(mess, detailMessage, false, e);
    }

    /**
     * Create a new FailedRequestException Exception.
     * @param userMessage the error message.
     * @param detailMessage the user message
     * @param userMessageHtml html flag 
     * @param e the original Exception
     */
    public FailedRequestException(String     userMessage, 
                                  String     detailMessage,
                                  boolean    userMessageHtml,
                                  Throwable  e) {
        super(userMessage,e);
        _detailMessage      = detailMessage;
    }

    public String     getUserMessage()       { return getMessage(); }
    public String     getDetailMessage()     { return _detailMessage; }

}
