/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.util.ServerStringUtil;
import edu.caltech.ipac.util.ThrowableUtil;

/**
 * This exception is thrown when a value is out of range.
 * @author Trey Roby
 * @version $Id: FailedRequestException.java,v 1.4 2011/05/17 23:48:33 roby Exp $
 */
public class FailedRequestException extends Exception {

    static public final String SERVICE_FAILED=  "Service Failed";
                                                 

    private String     _detailMessage;
    private boolean    _simple= false;
    private String     _constructedThread= Thread.currentThread().getName();

    private static final String NL= "\n";

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
        
        this(mess, detailMessage, isHtml(mess), e);
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

    public String toString(){
        StackTraceElement eAry[]= getStackTrace();
        StackTraceElement ste= eAry[0];
        String cName= ServerStringUtil.getShortClassName(ste.getClassName());

        String detail= "";
        String extra= "";

        String where = NL + NL +
                   "========---------- Location -----------========="  +
                        makeLineReport(this) + NL +
                       "Thread: " + _constructedThread;
        if (_detailMessage != null) 
              detail= NL + NL +
                   "========---------- Detailed Message -----------=========" 
                    + NL +_detailMessage;
        return super.toString() + detail + where + (_simple? "" : makeCausedBy()) + extra;
    }

    public void setSimpleToString(boolean simple) {
        _simple= simple;
    }

    private String makeCausedBy() {
       String retval= "";
       String shortName;
       for (Throwable t= getCause(); (t!=null); t= t.getCause()) {
           shortName= ServerStringUtil.getShortClassName(t.getClass().getName());
           retval+= NL + NL + 
                "========---------- Caused By " + shortName + 
                " ----------========" + NL +
                ThrowableUtil.getStackTraceAsString(t) +NL+
                makeLineReport(t);
       }
       return retval;
    }


    private String makeLineReport(Throwable t) {
	String where = "";
        StackTraceElement eAry[]= t.getStackTrace();
	if ((eAry != null) && (eAry.length > 0))
	{
	    StackTraceElement ste= eAry[0];
	    String cName= ServerStringUtil.getShortClassName(ste.getClassName());
	    where = NL +
                       "Class:  " + cName                              + NL +
                       "Method: " + ste.getMethodName()                + NL +
                       "File:   " + ste.getFileName()                  + NL +
                       "Line:   " + ste.getLineNumber();
	}

        return where;
    }

    private static boolean isHtml(String s) {
        boolean retval= false;
        if (s!=null && s.length()>6 && s.substring(0,6).equals("<html>")) {
                 retval= true;   
        }
        return retval;
    }
}


