package edu.caltech.ipac.firefly.core;

/**
 * @author tatianag
 * @version $Id: NotLoggedInException.java,v 1.2 2012/09/13 22:15:11 loi Exp $
 */
public class NotLoggedInException extends RPCException {

    public NotLoggedInException() {
        this("unknown", "unknown", "Operation requires user to be logged in", "Please, login");
    }

    public NotLoggedInException(String serviceName, String operationName, String message, String description) {
        super(serviceName, operationName, message, description);
    }
}
