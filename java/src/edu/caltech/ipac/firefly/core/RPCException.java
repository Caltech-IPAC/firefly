/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.util.StringUtils;

/**
 * Date: May 14, 2008
 * $Id: RPCException.java,v 1.8 2009/11/10 18:47:41 roby Exp $
 */
public class RPCException extends Exception {

    private static String SPACE="&nbsp;&nbsp;&nbsp;";
    String serviceClass;
    String serviceOperation;
    String description;
    String endUserMsg= null;
    private String lineReport = null;

    public RPCException() {
        super("RPCException");
        setServiceClass("Unknown");
        setServiceOperation("Unknown");
        setDescription("An exception occurred during the processing of remote procedure call");
    }

    public RPCException(String serviceName, String operationName, String message, String description) {
        super(message);
        setServiceClass(serviceName);
        setServiceOperation(operationName);
        setDescription(description);
    }

    public RPCException(Throwable cause, String serviceName, String operationName, String message, String description) {
        super(message, cause);
        if (cause != null) lineReport = makeCauseReport();
        setServiceClass(serviceName);
        setServiceOperation(operationName);
        setDescription(description);
    }

    public String getDescription() {
            return description;
    }

    public void setEndUserMsg(String m) {
        this.endUserMsg= m;
    }

    public String getEndUserMsg() {
        return this.endUserMsg;
    }


    public void setDescription(String description) {
            this.description = description;
    }

    public String getServiceClass() {
            return serviceClass;
    }

    public void setServiceClass(String serviceClass) {
            this.serviceClass = serviceClass;
    }

    public String getServiceOperation() {
            return serviceOperation;
    }

    public void setServiceOperation(String serviceOperation) {
            this.serviceOperation = serviceOperation;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    public String toStringBrief() {
        String str = "";
        if (endUserMsg!=null) {
            str += "User Message: " + this.getEndUserMsg()+ "\n";
        }
        str += "Message: " + this.getMessage() + "\n";
        str += "Description: " + this.getDescription() + "\n";
        return str;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    public String toString() {
        String str = "Server operation has failed\n\n";
        str += this.getDescription()+"\n\n";
        if (endUserMsg!=null) {
            str += "User Message: " + this.getEndUserMsg()+ "\n";
        }
        str += "Message: " + this.getMessage() + "\n";
        str += "Service: " + this.getServiceClass() + "\n";
        str += "Operation: " + this.getServiceOperation() + "\n";
        return str;
    }

    /**
     * Returns a html string representation of the object.
     * @return a string representation of the object.
     */
    public String toHtmlString() {
        // getCause returns null at GWT client
        String str= "";
        if (endUserMsg!=null) {
            str += "<b>" + this.getEndUserMsg()+ "</b><br><br>";
            str += this.getMessage() + "<br>";
        }
        else {
            str += "<b>" + this.getMessage() + "</b><br>";
        }
        str += "Error: "+this.getDescription() + "<br>";
        str += "Service: " + this.getServiceClass() + "<br>";
        str += "Operation: " + this.getServiceOperation()+"<br>";
        str += (lineReport==null?"":lineReport+"");
        return str;
    }

    private String makeCauseReport() {
            String retval= "";
            for (Throwable t= getCause(); (t!=null); t= t.getCause()) {
                retval+= makeCauseElement(t);
            }
        return retval;
    }


    private static String makeCauseElement(Throwable t) {
        StackTraceElement eAry[]= t.getStackTrace();
        String s = "";
        if ((eAry != null) && (eAry.length > 0))
        {
            s = "<br><br><i>Caused By:</i><br>";
            StackTraceElement ste= eAry[0];
            String cName= ste.getClassName();
            if (!StringUtils.isEmpty(t.getMessage())) {
                s+= SPACE+ t.getMessage() + "<br>";
            }
            s+= SPACE+ "Exception: " + t.getClass().getName()          + "<br>" +
                SPACE+ "Class:  " + cName                              + "<br>" +
                SPACE+ "Method: " + ste.getMethodName()                + "<br>" +
                SPACE+ "File:   " + ste.getFileName()                  + "<br>" +
                SPACE+ "Line:   " + ste.getLineNumber();
        }
        return s;

    }
}
