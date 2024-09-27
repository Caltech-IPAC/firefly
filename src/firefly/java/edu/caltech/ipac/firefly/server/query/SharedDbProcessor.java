/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.util.SortedSet;


/**
 * This is a base class for processors in which query results are stored as a table
 * within the same database.
 */
public abstract class SharedDbProcessor extends EmbeddedDbProcessor {

    @Override
    /**
     * All results from this processor will be saved in the same database.  It's also based on sessionID so that
     * it can be easily cleared.
     */
    public DbAdapter getDbAdapter(TableServerRequest treq) {
        String sessId = ServerContext.getRequestOwner().getRequestAgent().getSessId();
        return DbAdapter.getAdapter(treq, (ext) ->
                new File(QueryUtil.getTempDir(treq), "%s_%s.%s".formatted(treq.getRequestId(), sessId, ext))
        );
    }


    public FileInfo ingestDataIntoDb(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        // nothing to do here.
        return new FileInfo(dbAdapter.getDbFile());
    }

    @Override
    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        SortedSet<Param> params = treq.getSearchParams();
        params.addAll(treq.getResultSetParam());
        String tblName = dbAdapter.getDataTable() + "_" + DigestUtils.md5Hex(StringUtils.toString(params, "|"));

        if (!dbAdapter.hasTable(tblName)) {
            // Data for this request does not exist.. fetch data and ingest
            dbAdapter.ingestData(() -> fetchDataGroup(treq), tblName);
        }
        return dbAdapter.execRequestQuery(treq, tblName);
    }
}

