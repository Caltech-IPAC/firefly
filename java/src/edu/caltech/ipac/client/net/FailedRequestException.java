package edu.caltech.ipac.client.net;

import edu.caltech.ipac.util.StringUtil;
import edu.caltech.ipac.util.ThrowableUtil;

/**
 * This exception is thrown when a value is out of range.
 * @author Trey Roby
 * @version $Id: FailedRequestException.java,v 1.4 2011/05/17 23:48:33 roby Exp $
 */
public class FailedRequestException extends Exception {

    static public final String USER_CANCELED=   "The User Canceled the Request";
    static public final String NETWORK_DOWN=    "Network down";
    static public final String SERVER_DOWN=     "Server Unavailable";
    static public final String SERVER_TIMEOUT=  "Server Timed out";
    static public final String SERVICE_FAILED=  "Service Failed";
                                                 

    private boolean    _isHtml =false;
    private String     _detailMessage;
    private boolean    _simple= false;
    private Object     _extraInformation;
    private boolean    _userShouldSeeHint= true;
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
        _isHtml           =userMessageHtml;
    }

    public String     getUserMessage()       { return getMessage(); }
    public String     getDetailMessage()     { return _detailMessage; }
    public boolean    isHtmlMessage()        { return _isHtml;            }
    public Object     getExtraInformation()  { return _extraInformation;  }
    
    public boolean    isCauseUserCancel()    {
        return USER_CANCELED.equals(getMessage());
    }

    /**
     * This property is only a hint from the thrower to whoever is 
     * catching this exception.
     * It is true if this exception contains a user message that the user
     * should see.  False is a sugestion not to show the user the 
     * the messages from this exception.  The default is true.
     * @return  true if use should see the message otherwise false
     */
    public boolean    getUserShouldSeeHint()    { return _userShouldSeeHint;  }

    /**
     * This property is only a hint from the thrower to whoever is 
     * catching this exception.
     * Set to true if this exception contains a user message that the user
     * should see.  False is a sugestion not to show the user the 
     * the messages from this exception.  The default is true.
     * @param userShouldSeeHint true if use should see the message,
     *                          otherwise false
     */
    public void setUserShouldSeeHint(boolean userShouldSeeHint) { 
        _userShouldSeeHint= userShouldSeeHint;
    }

    public void setExtraInformation(Object extra) {
          _extraInformation= extra;
    }


    public String toString(){
        StackTraceElement eAry[]= getStackTrace();
        StackTraceElement ste= eAry[0];
        String cName= StringUtil.getShortClassName(ste.getClassName());

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
        if (_extraInformation != null) 
              extra= NL + NL +
                   "========---------- Extra Information ----------========" 
                   + NL  + _extraInformation;
        return super.toString() + detail + where + (_simple? "" : makeCausedBy()) + extra;
    }

    public void setSimpleToString(boolean simple) {
        _simple= simple;
    }

    private String makeCausedBy() {
       String retval= "";
       String shortName;
       for (Throwable t= getCause(); (t!=null); t= t.getCause()) {
           shortName= StringUtil.getShortClassName(t.getClass().getName());
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
	    String cName= StringUtil.getShortClassName(ste.getClassName());
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


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
