package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.util.TreeSet;

import static edu.caltech.ipac.firefly.data.TableServerRequest.FILTERS;

/**
 * This is a base class for processors which perform a task on the original table
 * and then save the results into the original table's database.
 */
public abstract class TableFunctionProcessor extends EmbeddedDbProcessor {
    public static final String SEARCH_REQUEST = "searchRequest";

    /**
     * results will be saved as a new set of tables(data, dd, meta) with the returned prefix.
     * @return
     */
    abstract protected String getResultSetTablePrefix();
    abstract protected DataGroup fetchData(TableServerRequest treq, File dbFile, DbAdapter dbAdapter) throws DataAccessException;

    @Override
    public File getDbFile(TableServerRequest treq) {
        try {
            TableServerRequest sreq = getSearchRequest(treq);
            return getSearchProcessor(sreq).getDbFile(sreq);
        } catch (DataAccessException e) {
            // should not happen
            return super.getDbFile(treq);
        }
    }

    @Override
    public File createDbFile(TableServerRequest treq) throws DataAccessException {
        try {
            TableServerRequest sreq = getSearchRequest(treq);
            return getSearchProcessor(sreq).createDbFile(sreq);
        } catch (DataAccessException e) {
            // should not happen
            return super.createDbFile(treq);
        }
    }

    /**
     * original database no longer available.. recreate it.
     */
    @Override
    public FileInfo ingestDataIntoDb(TableServerRequest treq, File dbFile) throws DataAccessException {
        TableServerRequest sreq = getSearchRequest(treq);
        sreq.setPageSize(1);  // set to small number it's not used.
        new SearchManager().getDataGroup(sreq).getData();
        return new FileInfo(dbFile);
    }

    /**
     * generate stats for the given search request if not exists.  otherwise, return the stats
     */
    @Override
    protected DataGroupPart getResultSet(TableServerRequest treq, File dbFile) throws DataAccessException {

        String resTblName = getResultSetTable(treq);

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance =  dbAdapter.getDbInstance(dbFile);
        String tblExists = String.format("select count(*) from %s", resTblName);
        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(tblExists);
        } catch (Exception e) {
            // does not exists.. fetch data and populate
            DataGroup data = fetchData(treq, dbFile, dbAdapter);
            EmbeddedDbUtil.createDataTbl(dbFile, data, dbAdapter, resTblName);
            EmbeddedDbUtil.createDDTbl(dbFile, data, dbAdapter, resTblName);
            EmbeddedDbUtil.createMetaTbl(dbFile, data, dbAdapter, resTblName);
        }
        return EmbeddedDbUtil.execRequestQuery(treq, dbFile, resTblName);
    }

    protected TableServerRequest getSearchRequest(TableServerRequest treq) throws DataAccessException {
        String searchRequestJson = treq.getParam(SEARCH_REQUEST);
        if (searchRequestJson == null) {
            throw new DataAccessException("Action failed: " + SEARCH_REQUEST + " is missing");
        }
        TableServerRequest sreq = QueryUtil.convertToServerRequest(searchRequestJson);
        if (sreq.getRequestId() == null) {
            throw new DataAccessException("Action failed: " + SEARCH_REQUEST + " must contain " + ServerParams.ID);
        }
        return sreq;
    }

    /**
     * returns the table name of the resultset.  the same request should return the same table name so that it
     * does not need to be recreated.
     * @param treq
     * @return
     * @throws DataAccessException
     */
    protected String getResultSetTable(TableServerRequest treq) throws DataAccessException {
        TableServerRequest sreq = getSearchRequest(treq);
        TreeSet<Param> params = new TreeSet<>();
        if (sreq.getFilters() != null && sreq.getFilters().size() > 0) {
            params.add(new Param(FILTERS, TableServerRequest.toFilterStr(sreq.getFilters())));
        }
        params.addAll(treq.getSearchParams());
        String id = StringUtils.toString(params, "|");
        return String.format("%s_data_%s", getResultSetTablePrefix(), DigestUtils.md5Hex(id));
    }

    protected EmbeddedDbProcessor getSearchProcessor(TableServerRequest searchReq) throws DataAccessException {
        return (EmbeddedDbProcessor) new SearchManager().getProcessor(searchReq.getRequestId());
    }


}











