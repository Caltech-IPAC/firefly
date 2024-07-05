package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.TreeSet;

import static edu.caltech.ipac.firefly.data.TableServerRequest.FILTERS;
import static edu.caltech.ipac.firefly.data.TableServerRequest.SQL_FILTER;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * This is a base class for processors which perform a task on the original table
 * and then save the results into the original table's database.
 */
public abstract class TableFunctionProcessor extends EmbeddedDbProcessor {

    @Override
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        // this method will not be called because ingestDataIntoDb is overridden.
        // this processor operate on the results(db) of an existing request.
        // it needs the original dbFile and dbAdapter.  A new abstract method is created for this.  see #fetchData()
        return null;
    }

    /**
     * results will be saved as a new set of tables(data, dd, meta) with the returned prefix.
     * @return
     */
    abstract protected String getResultSetTablePrefix();
    abstract protected DataGroup fetchData(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException;

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
     * original database no longer available.. recreate it.
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

        String resTblName = getResultSetTable(treq);

        if (!dbAdapter.hasTable(resTblName)) {
            // does not exists.. fetch data and populate
            dbAdapter.ingestData(() -> fetchData(treq, dbAdapter), resTblName);
        }
        return dbAdapter.execRequestQuery(treq, resTblName);
    }

    /**
     * returns the table name of the resultset.  the same request should return the same table name so that it
     * does not need to be recreated.
     * @param treq
     * @return
     * @throws DataAccessException
     */
    protected String getResultSetTable(TableServerRequest treq) throws DataAccessException {
        TableServerRequest sreq = QueryUtil.getSearchRequest(treq);
        TreeSet<Param> params = new TreeSet<>();
        if (sreq.getFilters() != null && sreq.getFilters().size() > 0) {
            params.add(new Param(FILTERS, TableServerRequest.toFilterStr(sreq.getFilters())));
        }
        if (!isEmpty(sreq.getSqlFilter())) {
            params.add(new Param(SQL_FILTER, sreq.getSqlFilter()));
        }
        params.addAll(treq.getSearchParams());
        String id = StringUtils.toString(params, "|");
        String rst = String.format("%s_data_%s", getResultSetTablePrefix(), DigestUtils.md5Hex(id));
        return rst.toUpperCase();
    }

    protected EmbeddedDbProcessor getSearchProcessor(TableServerRequest searchReq) throws DataAccessException {
        return (EmbeddedDbProcessor) SearchManager.getProcessor(searchReq.getRequestId());
    }


}











