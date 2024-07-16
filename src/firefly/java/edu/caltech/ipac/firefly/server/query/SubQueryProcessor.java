package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;

import static edu.caltech.ipac.table.TableMeta.RESULTSET_ID;

/**
 * This processor perform a query on the underlying searchRequest and return the results
 * without saving any intermediate tables.
 */
@SearchProcessorImpl(id = "SubQueryProcessor")
public class SubQueryProcessor extends EmbeddedDbProcessor {

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        // this method will not be called because ingestDataIntoDb is overridden.
        // this processor operate on the results(db) of an existing request.
        // it needs the original dbFile and dbAdapter.  A new abstract method is created for this.  see #fetchData()
        return null;
    }

    @Override
    public DbAdapter getDbAdapter(TableServerRequest treq) {
        try {
            TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
            return getSearchProcessor(sreq).getDbAdapter(sreq);
        } catch (DataAccessException e) {
            // should not happen
            return super.getDbAdapter(treq);
        }
    }

    /**
     * only called when original database no longer available.. recreate it.
     */
    @Override
    public FileInfo ingestDataIntoDb(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        sreq.setPageSize(1);  // set to small number it's not used.
        new SearchManager().getDataGroup(sreq).getData();
        return new FileInfo(dbAdapter.getDbFile());
    }

    /**
     * generate stats for the given search request if not exists.  otherwise, return the stats
     */
    @Override
    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {

        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        sreq.setPageSize(1);  // set to small number it's not used.
        DataGroup baseResults = new SearchManager().getDataGroup(sreq).getData();
        String origTableName = baseResults.getTableMeta().getAttribute(RESULTSET_ID);

        return dbAdapter.execRequestQuery(treq, origTableName);
    }

    protected EmbeddedDbProcessor getSearchProcessor(TableServerRequest searchReq) throws DataAccessException {
        return (EmbeddedDbProcessor) SearchManager.getProcessor(searchReq.getRequestId());
    }


}











