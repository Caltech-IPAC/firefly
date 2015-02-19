/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

/**
 * Date: Jun 8, 2009
 *
 * @author loi
 * @version $Id: DataAccessException.java,v 1.2 2009/06/26 21:31:37 loi Exp $
 */
public class DataAccessException extends Exception {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(Throwable e) {
        super(e);
    }
}
