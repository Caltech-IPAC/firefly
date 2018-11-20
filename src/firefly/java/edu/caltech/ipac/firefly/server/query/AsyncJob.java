/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.table.DataGroup;

/**
 * Date: 9/19/18
 *
 * @author loi
 * @version $Id: $
 */
public interface AsyncJob {
    enum Phase {PENDING, QUEUED, EXECUTING, COMPLETED, ABORTED, ERROR, UNKNOWN}

    DataGroup getDataGroup() throws DataAccessException;
    boolean cancel();
    Phase getPhase() throws DataAccessException;
    String getErrorMsg() throws DataAccessException;

    /**
     * @return how long should the job run before giving up.
     */
    long getTimeout();
}
