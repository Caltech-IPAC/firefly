/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.ServerContext;

import java.io.File;
import java.io.FileNotFoundException;

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

    public DataAccessException(String message, Throwable cause) { super(message, cause); }

    public DataAccessException(Throwable e) { super(e); }

    public static class Aborted extends DataAccessException {
        public Aborted() {
            super("Aborted");
        }
    }

    public static class FileNotFound extends DataAccessException {
        public FileNotFound(String msg, File f) {
            super(msg, new FileNotFoundException(makeMsg(f)));
        }

        static String makeMsg(File f) {
            if (f != null && !f.exists() && f.getAbsolutePath().contains(ServerContext.getWorkingDir().getPath())) {
                return "Temporary files are regularly deleted to free up space. It's possible that they were removed due to inactivity.";
            } else if (f != null && !f.canRead()){
                return "The file lacks read access, possibly due to misconfiguration. Please reach out to our support team for assistance.";
            }
            return "This could be caused by various reasons, including incorrect file location, permissions, or other factors.";
        }
    }
}


