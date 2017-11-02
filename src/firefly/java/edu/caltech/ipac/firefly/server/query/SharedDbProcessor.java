/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.util.SortedSet;


/**
 * This is a base class for processors in which query results are stored as a table
 * within the same database.
 */
public abstract class SharedDbProcessor extends EmbeddedDbProcessor {

    public abstract DataGroup fetchData(TableServerRequest treq) throws DataAccessException;

    @Override
    /**
     * All results from this processor will be saved in the same database.  It's also based on sessionID so that
     * it can be easily cleared.
     */
    public File getDbFile(TableServerRequest treq) {
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        String fname = String.format("%s_%s.%s", treq.getRequestId(), ServerContext.getRequestOwner().getRequestAgent().getSessId(), dbAdapter.getName());
        return new File(ServerContext.getTempWorkDir(), fname);
    }

    public FileInfo ingestDataIntoDb(TableServerRequest treq, File dbFile) throws DataAccessException {
        // nothing to do here.
        return new FileInfo(dbFile);
    }

    @Override
    protected DataGroupPart getResultSet(TableServerRequest treq, File dbFile) throws DataAccessException {
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance =  dbAdapter.getDbInstance(dbFile);
        SortedSet<Param> params = treq.getSearchParams();
        params.addAll(treq.getResultSetParam());
        String tblName = "data_" + DigestUtils.md5Hex(StringUtils.toString(params, "|"));

        String tblExists = String.format("select count(*) from %s", tblName);
        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(tblExists);
        } catch (Exception e) {
            // DD for this catalog does not exists.. fetch data and populate
            DataGroup data = fetchData(treq);
            EmbeddedDbUtil.ingestDataGroup(dbFile, data, dbAdapter, tblName);
        }
        return EmbeddedDbUtil.getResultForTable(treq, dbFile, tblName);
    }
}

