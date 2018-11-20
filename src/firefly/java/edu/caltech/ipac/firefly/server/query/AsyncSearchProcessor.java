/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;

import java.util.concurrent.TimeUnit;

/**
 * Date: Sept 19, 2018
 *
 * @author loi
 * @version $Id: SearchProcessor.java,v 1.3 2012/06/21 18:23:53 loi Exp $
 */
public abstract class AsyncSearchProcessor extends EmbeddedDbProcessor  {
    abstract AsyncJob submitRequest(ServerRequest request) throws DataAccessException;

//====================================================================
//  default implementations
//====================================================================

    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        AsyncJob asyncJob = submitRequest(req);
        int cnt = 0;
        try {
            while (true) {
                cnt++;
                AsyncJob.Phase phase = asyncJob.getPhase();
                switch (phase) {
                    case COMPLETED:
                        return asyncJob.getDataGroup();
                    case ERROR:
                        throw new DataAccessException(asyncJob.getErrorMsg());
                    case ABORTED:
                        throw new DataAccessException("Query aborted");
                    case PENDING:
                    case EXECUTING:
                    case QUEUED:
                    default:
                    {
                        int wait = cnt < 10 ? 500 : cnt < 20 ? 1000 : 2000;
                        TimeUnit.MILLISECONDS.sleep(wait);
                    }
                }
            }
        } catch (InterruptedException e) {
            throw new DataAccessException("Query aborted");
        }
    }
}
