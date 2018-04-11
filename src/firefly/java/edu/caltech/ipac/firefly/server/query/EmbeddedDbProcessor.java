/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.JsonTableUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.execRequestQuery;

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
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();


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
        return new File(getTempDir(), fname);
    }

    protected File getTempDir() {
        String sessId = ServerContext.getRequestOwner().getRequestAgent().getSessId();
        File tempDir = new File(ServerContext.getTempWorkDir(), sessId.substring(0, 3));
        if (!tempDir.exists()) tempDir.mkdirs();
        return tempDir;
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

        // make sure multiple requests for the same data waits for the first one to create before accessing.
        lock.lock();
        try {
            boolean dbFileCreated = false;
            File dbFile = getDbFile(treq);
            if (!dbFile.exists() || !EmbeddedDbUtil.hasTable(treq, dbFile, "DATA")) {
                StopWatch.getInstance().start("createDbFile: " + treq.getRequestId());
                dbFile = populateDataTable(treq);
                dbFileCreated = true;
                StopWatch.getInstance().stop("createDbFile: " + treq.getRequestId()).printLog("createDbFile: " + treq.getRequestId());
            }

            StopWatch.getInstance().start("getDataset: " + request.getRequestId());
            DataGroupPart results;
            try {
                results = getResultSet(treq, dbFile);
            } catch (Exception e) {
                // table data exists.. but, bad grammar when querying for the resultset.
                // should return table meta info + error message
                DataGroup dg = EmbeddedDbUtil.execQuery(DbAdapter.getAdapter(treq), dbFile, "select * from data limit 0", "data");
                results = EmbeddedDbUtil.toDataGroupPart(dg, treq);
                results.setErrorMsg(retrieveMsgFromError(e, treq));
            }
            StopWatch.getInstance().stop("getDataset: " + request.getRequestId()).printLog("getDataset: " + request.getRequestId());

            if (doLogging() && dbFileCreated) {
                // no reliable way of capturing cached searches count
                SearchProcessor.logStats(treq.getRequestId(), results.getRowCount(), 0, false, getDescResolver().getDesc(treq));
            }

            return results;
        } finally {
            activeRequests.remove(unigueReqID);
            lock.unlock();
        }
    }

    private File populateDataTable(TableServerRequest treq) throws DataAccessException {
        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        File dbFile = createDbFile(treq);
        try {
            FileInfo dbFileInfo = ingestDataIntoDb(treq, dbFile);
            dbFile = dbFileInfo.getFile();
        } catch (Exception e) {
            dbFile.delete();
            if (e instanceof DataAccessException) {
                throw e;
            } else {
                throw new DataAccessException(retrieveMsgFromError(e, treq), e);
            }
        }
        EmbeddedDbUtil.setDbMetaInfo(treq, dbAdapter, dbFile);
        return dbFile;
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

        if (!EmbeddedDbUtil.hasTable(treq, dbFile, resultSetID)) {
            // does not exists.. create table from original 'data' table
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

        // resultSetID is a table created with sort and filter in consideration.  no need to re-apply.
        TableServerRequest nreq = (TableServerRequest) treq.cloneRequest();
        nreq.setFilters(null);
        nreq.setSortInfo(null);
        nreq.setInclColumns(new String[0]);

        DataGroupPart page = execRequestQuery(nreq, dbFile, resultSetID);

        // save information needed to recreated this resultset
        page.getTableDef().setAttribute(TableServerRequest.RESULTSET_REQ, makeResultSetReqStr(treq));
        page.getTableDef().setAttribute(TableServerRequest.RESULTSET_ID, resultSetID);

        // handle selectInfo
        // selectInfo is sent to the server as Request.META_INFO.selectInfo
        // it will be moved into TableModel.selectInfo
        SelectionInfo selectInfo = getSelectInfoForThisResultSet(treq, dbAdapter, dbFile, resultSetID, page.getRowCount());
        page.getTableDef().setSelectInfo(selectInfo);
        treq.setSelectInfo(null);

        return page;
    }

    private String makeResultSetReqStr(TableServerRequest treq) {
        // only keep state one deep.
        TableServerRequest savedRequest = (TableServerRequest) treq.cloneRequest();
        savedRequest.getMeta().remove(TableServerRequest.RESULTSET_REQ);
        savedRequest.getMeta().remove(TableServerRequest.RESULTSET_ID);
        savedRequest.setSelectInfo(null);
        return JsonTableUtil.toJsonTableRequest(savedRequest).toString();
    }

    public boolean isSecurityAware() { return false; }

//====================================================================
//
//====================================================================

    private static List<String> getColumnNames(DbInstance dbInstance, String forTable) {
        List<String> cols = JdbcFactory.getSimpleTemplate(dbInstance).query(String.format("select cname from %s_DD", forTable), (rs, i) -> "\"" + rs.getString(1) + "\"");
        return cols;
    }

    private String ensurePrevResultSetIfExists(TableServerRequest treq, File dbFile) {

        String prevResultSetID = treq.getMeta().get(TableServerRequest.RESULTSET_ID);
        if (!StringUtils.isEmpty(prevResultSetID)) {
            if (!EmbeddedDbUtil.hasTable(treq, dbFile, prevResultSetID)) {
                // does not exists.. create table from original 'data' table
                String resultSetRequest = treq.getMeta().get(TableServerRequest.RESULTSET_REQ);
                if (!StringUtils.isEmpty(resultSetRequest)) {
                    try {
                        TableServerRequest req = QueryUtil.convertToServerRequest(resultSetRequest);
                        DataGroupPart page = getResultSet(req, dbFile);
                        prevResultSetID = page.getTableDef().getAttribute(TableServerRequest.RESULTSET_ID).getValue();
                    } catch (DataAccessException e1) {
                        // can ignore for now.
                    }
                    return prevResultSetID;
                }
            }
        }
        return prevResultSetID;
    }

    private SelectionInfo getSelectInfoForThisResultSet(TableServerRequest treq, DbAdapter dbAdapter, File dbFile, String forTable, int rowCnt) {

        SelectionInfo selectInfo = treq.getSelectInfo();
        if (selectInfo == null) {
            selectInfo = new SelectionInfo(false, null, rowCnt);
        }

        String prevResultSetID = treq.getMeta(TableServerRequest.RESULTSET_ID);             // the previous resultset ID
        prevResultSetID = StringUtils.isEmpty(prevResultSetID) ? "data" : prevResultSetID;

        if ( selectInfo.getSelectedCount() > 0 && !String.valueOf(prevResultSetID).equals(String.valueOf(forTable)) ) {
            // there were row(s) selected from previous resultset.. make sure selectInfo is remapped to new resultset
            prevResultSetID = ensurePrevResultSetIfExists(treq, dbFile);

            String rowNums = StringUtils.toString(selectInfo.getSelected());
            SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile));
            String origRowIds = String.format("Select ROW_IDX from %s where ROW_NUM in (%s)", prevResultSetID, rowNums);

            List<Integer> newRowNums = new ArrayList<>();
            try {
                newRowNums = jdbc.query(String.format("Select ROW_NUM from %s where ROW_IDX in (%s)", forTable, origRowIds), (rs, idx) -> rs.getInt(1));
            } catch (Exception ex) {
                // unable to collect previous select info.  we'll treat it
            }
            selectInfo = newRowNums.size() == rowCnt ? new SelectionInfo(true, null, rowCnt) : new SelectionInfo(false, newRowNums, rowCnt);
        }
        selectInfo.setRowCount(rowCnt);
        return selectInfo;
    }

    private static String retrieveMsgFromError(Exception e, TableServerRequest treq) {

        if (e instanceof BadSqlGrammarException) {
            // object not found condition
            BadSqlGrammarException ex = (BadSqlGrammarException) e;
            String msg = ex.getRootCause().getMessage();
            if (msg.toLowerCase().contains("object not found")) {
                String name = CollectionUtil.get(msg.split(":"), 1, "").trim();
                String sql = ex.getSql() == null ? "" : ex.getSql();
                if (sql.matches(String.format(".*from\\s+%s.*", name))) {
                    return "Table not found: " + name;
                } else {
                    return "Column not found: " + name;
                }
            } else if (msg.toLowerCase().contains("object name already exists")) {
                String name = CollectionUtil.get(msg.split(":"), 1, "").trim();
                return "Duplicate table or column name: " + name;
            }
        }

        if (e instanceof NestedRuntimeException) {
            List<String> possibleErrors = new ArrayList<>();
            NestedRuntimeException ex = (NestedRuntimeException) e;
            String msg = ex.getRootCause().getMessage();
            TableServerRequest prevReq = QueryUtil.convertToServerRequest(treq.getMeta().get(TableServerRequest.RESULTSET_REQ));
            if (treq.getInclColumns() != null && !StringUtils.areEqual(treq.getInclColumns(), prevReq.getInclColumns())) {
                possibleErrors.add(treq.getInclColumns());
            }
            List<String> diff = CollectionUtil.diff(treq.getFilters(), prevReq.getFilters(), false);
            if (diff != null && diff.size() > 0) {
                possibleErrors.addAll(diff);
            }
            if (treq.getSortInfo() != null && !treq.getSortInfo().equals(prevReq.getSortInfo())) {
                possibleErrors.add(treq.getSortInfo().toString());
            }

            if (msg.toLowerCase().contains(" cast")) {
                return "Data type mismatch: \n" + StringUtils.toString(possibleErrors, "\n");
            }

            if ( msg.toLowerCase().contains("unexpected token:")) {
                return CollectionUtil.get(msg.split("required:"), 0, "").trim() +
                        "\n" + StringUtils.toString(possibleErrors, "\n");
            }
            return "Invalid statement: \n" + StringUtils.toString(possibleErrors, "\n");
        }

        return e.getMessage();
    }
}

