/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * NOTE: We're using spring jdbc v2.x.  API changes dramatically in later versions.
 * For v2.x API docs, https://docs.spring.io/spring/docs/2.5.5/javadoc-api/
 *
 * Things to be aware of:
 *
 * - This processor caches its results based on search parameters plus session ID.
 *   To fetch new results, simply quit your browser so a new session ID can be assigned
 *
 * - There are 3 tables generated for each DataGroup; DATA, DATA_DD, and DATA_META.
 *   DATA contains the data, DD contains the column's definitions, and META contains the data's meta information
 *
 * - ROW_IDX and ROW_NUM are added to every DATA table.  use DataGroup.ROW_IDX and DataGroup.ROW_NUM when referencing it.
 *   ROW_IDX is the original row index of the data, starting from 0
 *   ROW_NUM is the natural order of the table, starting from 0
 *   these are used in row-based operations, ie. highlighting, selecting, filtering.
 *
 * - When an operation that changes the results of a table, like filter or sort, a new set of tables
 *   will be created; DATA_[hash_id], DATA_[hash_id]_DD, and DATA_[hash_id]_META.
 *   This is done for a couple of reasons:
 *   1. paging is much faster, since it does not need to re-run the query to get at the results.
 *   2. storing the original ROW_IDX of DATA in relation to the results.
 *   [hash_id] is an MD5 hex of the filter/sort parameters.
 *
 * - Database columns when not quoted are usually stored as uppercase.  Columns used in an SQL statement when not quoted
 *   are converted to uppercase in order to match the name of the table's columns.  With that said:
 *   - DataGroup columns are stored as uppercase
 *   - DD table is used to convert it back to original casing
 *   - Every database has its list reserved words.  To reference a column with the same name, enclose it with double-quotes(").
 *
 */
abstract public class EmbeddedDbProcessor implements SearchProcessor<DataGroupPart>, CanGetDataFile {
    private static final Map<String, ReentrantLock> activeRequests = new HashMap<>();
    private static final ReentrantLock lockChecker = new ReentrantLock();


    /**
     * Fetches the data from the given search request, then save it into a database
     * The database should contains at least 3 named tables: DATA, DD, and META
     * DATA table contains the data
     * DD table contains definition of the columns, including name, label, format, etc
     * META table contains meta information taken from the table.
     *
     * @param req  search request
     * @throws DataAccessException
     */
    abstract public FileInfo createDbFile(TableServerRequest req) throws DataAccessException;


    public DataGroupPart getData(ServerRequest request) throws DataAccessException {
        TableServerRequest treq = (TableServerRequest) request;

        String unigueReqID = this.getUniqueID(request);

        lockChecker.lock();
        ReentrantLock lock = null;
        try {
            lock = activeRequests.get(unigueReqID);
            if (lock == null) {
                lock = new ReentrantLock();
                activeRequests.put(unigueReqID, lock);
            }
        } finally {
            lockChecker.unlock();
        }

        // make sure multiple requests for the same data waits for the first one to create before acessing.
        lock.lock();
        try {
            boolean dbFileCreated = false;
            File dbFile = EmbeddedDbUtil.getDbFile(treq);
            if (dbFile == null || !dbFile.canRead()) {
                StopWatch.getInstance().start("createDbFile: " + request.getRequestId());
                FileInfo dbFileInfo = createDbFile(treq);
                dbFile = dbFileInfo.getFile();
                EmbeddedDbUtil.setDbMetaInfo(treq, DbAdapter.getAdapter(treq), dbFile);
                dbFileCreated = true;
                StopWatch.getInstance().stop("createDbFile: " + request.getRequestId()).printLog("createDbFile: " + request.getRequestId());
            }

            StopWatch.getInstance().start("getDataset: " + request.getRequestId());
            DataGroupPart results = getDataset(treq, dbFile);
            StopWatch.getInstance().stop("getDataset: " + request.getRequestId()).printLog("getDataset: " + request.getRequestId());

            if (doLogging() && dbFileCreated) {
                // no reliable way of capturing cached searches count
                SearchProcessor.logStats(treq.getRequestId(), results.getRowCount(), 0, false, getDescResolver().getDesc(treq));
            }

            return  results;
        } finally {
            activeRequests.remove(unigueReqID);
            lock.unlock();
        }
    }

    public File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {
        request.cloneRequest();
        request.setPageSize(Integer.MAX_VALUE);
        DataGroupPart results = getData(request);
        File ipacTable = createTempFile(request, ".tbl");
        IpacTableWriter.save(ipacTable, results.getData());
        return ipacTable;
    }

    public FileInfo writeData(OutputStream out, ServerRequest request) throws DataAccessException {
        try {
            TableServerRequest treq = (TableServerRequest) request;
            DataGroupPart page = getData(request);
            IpacTableWriter.save(out, page.getData(), true);

            // this is not accurate information if used to determine exactly what was written to output stream.
            // dbFile is the database file which contains the whole search results.  What get written to the output
            // stream is based on the given request.
            File dbFile = EmbeddedDbUtil.getDbFile(treq);
            return new FileInfo(dbFile);
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
    }

    public ServerRequest inspectRequest(ServerRequest request) {
        return SearchProcessor.inspectRequestDef(request);
    }

    public String getUniqueID(ServerRequest request) {
        return EmbeddedDbUtil.getUniqueID((TableServerRequest) request);
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        SearchProcessor.prepareTableMetaDef(defaults, columns, request);
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.StatsLogResolver();
    }

    protected File createTempFile(TableServerRequest request, String fileExt) throws IOException {
        return File.createTempFile(request.getRequestId(), fileExt, ServerContext.getTempWorkDir());
    }

    public boolean doCache() {return false;}
    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {}
    public boolean doLogging() {return true;}

    protected DataGroupPart getDataset(TableServerRequest treq, File dbFile) throws DataAccessException {

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);

        try {
            String tblName = EmbeddedDbUtil.setupDatasetTable(treq);

            // select a page from the dataset table
            String pageSql = getPageSql(dbAdapter, treq);
            DataGroupPart page = EmbeddedDbUtil.getResults(treq, pageSql, tblName);

            // fetch total row count for the query.. datagroup may contain partial results(paging)
            String cntSql = getCountSql(dbAdapter, treq);
            int rowCnt = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile)).queryForInt(cntSql);

            page.setRowCount(rowCnt);
            page.getTableDef().setAttribute(TableServerRequest.DATASET_ID, EmbeddedDbUtil.getDatasetID(treq));
            if (!StringUtils.isEmpty(treq.getTblTitle())) {
                page.getData().setTitle(treq.getTblTitle());  // set the datagroup's title to the request title.
            }
            return page;
        } catch (Exception ex) {
            throw new DataAccessException(ex);
        }
    }

//====================================================================
//
//====================================================================


    private String getCountSql(DbAdapter dbAdapter, TableServerRequest treq) {
        String fromSql = dbAdapter.fromPart(treq);
        String wherePart = dbAdapter.wherePart(treq);
        return String.format("select count(*) %s %s", fromSql, wherePart);
    }

    private String getPageSql(DbAdapter dbAdapter, TableServerRequest treq) {
        String selectPart = dbAdapter.selectPart(treq);
        String fromPart = dbAdapter.fromPart(treq);
        String pagingPart = dbAdapter.pagingPart(treq);

        return String.format("%s %s %s", selectPart, fromPart, pagingPart);
    }

}

