/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.jdbc.exceptions;

/**
 * Base class for runtime exceptions that wrap a root cause.
 */
public class NestedRuntimeException extends RuntimeException {

    public NestedRuntimeException(String msg) {
        super(msg);
    }

    public NestedRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /** Returns the deepest cause in the exception chain. */
    public Throwable getRootCause() {
        Throwable c = getCause();
        while (c != null && c.getCause() != null) c = c.getCause();
        return c;
    }

    public Throwable getMostSpecificCause() {
        Throwable root = getRootCause();
        return root != null ? root : this;
    }
}
