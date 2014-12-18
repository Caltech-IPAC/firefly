package edu.caltech.ipac.firefly.server.packagedata;
/**
 * User: roby
 * Date: 5/5/11
 * Time: 1:31 PM
 */


/**
 * @author Trey Roby
 */
public class IllegalPackageStateException extends Exception {

    IllegalPackageStateException(String msg) { super(msg); }

    IllegalPackageStateException(String msg, Throwable t) { super(msg,t); }
}

