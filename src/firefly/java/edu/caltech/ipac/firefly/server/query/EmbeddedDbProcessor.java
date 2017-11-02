/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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
 * - All column names must be enclosed in double-quotes(") to avoid reserved keywords clashes.
 *   This applies to inputs used by the database component, ie.  INCL_COLUMNS, FILTERS, SORT_INFO, etc
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
    abstract public FileInfo ingestDataIntoDb(TableServerRequest req, File dbFile) throws DataAccessException;

    /**
     * returns the database file for the given request.
     * This implementation returns a file based on sessionId + search parameters
     * @param treq
     * @return
     */
    public File getDbFile(TableServerRequest treq) {
        String fname = String.format("%s_%s.%s", treq.getRequestId(), DigestUtils.md5Hex(getUniqueID(treq)), DbAdapter.getAdapter(treq).getName());
        return new File(ServerContext.getTempWorkDir(), fname);
    }

    /**
     * create a new database file base on the given request.
     * if this is overridden, make sure to override getDbFile as well.
     * @param treq
     * @return
     * @throws DataAccessException
     */
    public File createDbFile(TableServerRequest treq) throws DataAccessException {
        try {
            File dbFile = getDbFile(treq);
            DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
            EmbeddedDbUtil.createDbFile(dbFile, dbAdapter);
            return dbFile;
        } catch (IOException e) {
            throw new DataAccessException("Unable to create database file.");
        }
    }

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
            File dbFile = getDbFile(treq);
            if (!dbFile.exists()) {
                StopWatch.getInstance().start("createDbFile: " + request.getRequestId());
                DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
                dbFile = createDbFile(treq);
                FileInfo dbFileInfo = ingestDataIntoDb(treq, dbFile);
                dbFile = dbFileInfo.getFile();
                EmbeddedDbUtil.setDbMetaInfo(treq, dbAdapter, dbFile);
                dbFileCreated = true;
                StopWatch.getInstance().stop("createDbFile: " + request.getRequestId()).printLog("createDbFile: " + request.getRequestId());
            }

            StopWatch.getInstance().start("getDataset: " + request.getRequestId());
            DataGroupPart results = getResultSet(treq, dbFile);
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
            File dbFile = getDbFile(treq);
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

    /**
     * returns the table name for this request. 
     */
    @NotNull
    public String getResultSetID(TableServerRequest treq) {
        String id = StringUtils.toString(treq.getResultSetParam(), "|");
        return StringUtils.isEmpty(id) ? "data" : "data_" + DigestUtils.md5Hex(id);
    }

    protected DataGroupPart getResultSet(TableServerRequest treq, File dbFile) throws DataAccessException {

        String resultSetID = getResultSetID(treq);

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);

        try {
            JdbcFactory.getSimpleTemplate(dbInstance).queryForInt(String.format("select count(*) from %s", resultSetID));
        } catch (Exception e) {
            // does not exists.. create table from orignal 'data' table
            List<String> cols = StringUtils.isEmpty(treq.getInclColumns()) ? getColumnNames(dbInstance, "DATA")
                    : StringUtils.asList(treq.getInclColumns(), ",");
            String wherePart = dbAdapter.wherePart(treq);
            String orderBy = dbAdapter.orderByPart(treq);

            cols = cols.stream().filter((s) -> {
                s = s.replaceFirst("^\"(.+)\"$", "$1");
                return !(s.equals(DataGroup.ROW_IDX) || s.equals(DataGroup.ROW_NUM));
            }).collect(Collectors.toList());   // remove this cols because it will be automatically added

            // copy data
            String datasetSql = String.format("select %s, %s from data %s %s", StringUtils.toString(cols), DataGroup.ROW_IDX, wherePart, orderBy);
            String datasetSqlWithIdx = String.format("select b.*, (ROWNUM-1) as %s from (%s) as b", DataGroup.ROW_NUM, datasetSql);
            String sql = dbAdapter.createTableFromSelect(resultSetID, datasetSqlWithIdx);
            JdbcFactory.getSimpleTemplate(dbInstance).update(sql);

            // copy dd
            String ddSql = "select * from data_dd";
            ddSql = dbAdapter.createTableFromSelect(resultSetID + "_dd", ddSql);
            JdbcFactory.getSimpleTemplate(dbInstance).update(ddSql);

            // copy meta
            String metaSql = "select * from data_meta";
            metaSql = dbAdapter.createTableFromSelect(resultSetID + "_meta", metaSql);
            JdbcFactory.getSimpleTemplate(dbInstance).update(metaSql);

        }
        return EmbeddedDbUtil.getResultForTable(treq, dbFile, resultSetID);
    }

    public boolean isSecurityAware() { return false; }

//====================================================================
//
//====================================================================

    private static List<String> getColumnNames(DbInstance dbInstance, String forTable) {
        List<String> cols = JdbcFactory.getSimpleTemplate(dbInstance).query(String.format("select cname from %s_DD", forTable), (rs, i) -> "\"" + rs.getString(1) + "\"");
        return cols;
    }
    
}

