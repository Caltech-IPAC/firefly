/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.firefly.core.background.JobInfo;

import java.util.concurrent.TimeUnit;

/**
 * Date: Sept 19, 2018
 *
 * @author loi
 * @version $Id: SearchProcessor.java,v 1.3 2012/06/21 18:23:53 loi Exp $
 */
public abstract class AsyncSearchProcessor extends EmbeddedDbProcessor  {
    private AsyncJob asyncJob;

    abstract AsyncJob submitRequest(TableServerRequest request) throws DataAccessException;


//====================================================================
//  default implementations
//====================================================================

    public void onAbort() {
        if (asyncJob != null) {
            asyncJob.cancel();
            Logger.getLogger().debug("AsyncJob cancelled: " + asyncJob.getBaseJobUrl());
            asyncJob = null;
        }
    }

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        asyncJob = submitRequest(req);
        int cnt = 0;
        try {
            while (true) {
                cnt++;
                AsyncJob.Phase phase = asyncJob.getPhase();
                switch (phase) {
                    case COMPLETED:
                        return asyncJob.getDataGroup();
                    case ERROR:
                    case UNKNOWN: {
                        String error = asyncJob.getErrorMsg();
                        if (getJob() != null) getJob().setError(500, error);
                        throw new DataAccessException(error);
                    }
                    case ABORTED:
                        if (getJob() != null) getJob().setPhase(JobInfo.PHASE.ABORTED);
                        throw new DataAccessException.Aborted();
                    case PENDING:
                    case EXECUTING:
                    case QUEUED:
                    default:
                    {
                        int wait = cnt < 3 ? 500 : cnt < 20 ? 1000 : 2000;
                        TimeUnit.MILLISECONDS.sleep(wait);
                    }
                }
                jobIf(v -> v=null);         // check job phase.. exit loop if aborted.
            }
        } catch (InterruptedException e) {
            onAbort();
            throw new DataAccessException.Aborted();
        }
    }
}
