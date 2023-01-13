/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.table.io.DsvTableIO;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.DbInstance;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.firefly.core.background.Job;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.apache.commons.csv.CSVFormat;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.data.table.MetaConst.HIGHLIGHTED_ROW;
import static edu.caltech.ipac.firefly.data.table.MetaConst.HIGHLIGHTED_ROW_BY_ROWIDX;
import static edu.caltech.ipac.firefly.server.ServerContext.SHORT_TASK_EXEC;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.MAIN_DB_TBL;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.NULL_TOKEN;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.execRequestQuery;
import static edu.caltech.ipac.util.StringUtils.*;

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
abstract public class EmbeddedDbProcessor implements SearchProcessor<DataGroupPart>, SearchProcessor.CanGetDataFile,
                                                     SearchProcessor.CanFetchDataGroup, Job.Worker {
    private static final Map<String, ReentrantLock> activeRequests = new HashMap<>();
    private static final ReentrantLock lockChecker = new ReentrantLock();
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static final int MAX_COL_ENUM_COUNT = AppProperties.getIntProperty("max.col.enum.count", 32);
    private Job job;

    public void setJob(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
    }

    public String getLabel() { return getJob().getParams().getTableServerRequest().getTblTitle(); }

    /**
     * Fetches the data for the given search request.  This method should perform a fetch for fresh
     * data.  Caching should not be performed here.
     * @param req
     * @return
     * @throws DataAccessException
     */
    abstract public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException;

    /**
     * returns the database file for the given request.
     * This implementation returns a file based on sessionId + search parameters
     * @param treq
     * @return
     */
    public File getDbFile(TableServerRequest treq) {
        String fname = String.format("%s_%s.%s", treq.getRequestId(), DigestUtils.md5Hex(getUniqueID(treq)), DbAdapter.getAdapter(treq).getName());
        return new File(QueryUtil.getTempDir(treq), fname);
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
            jobExecIf(v -> v.progress(10, "fetching data..."));
            if (!dbFile.exists()) {
                StopWatch.getInstance().start("createDbFile: " + treq.getRequestId());
                dbFile = createDbFromRequest(treq);
                dbFileCreated = true;
                StopWatch.getInstance().stop("createDbFile: " + treq.getRequestId()).printLog("createDbFile: " + treq.getRequestId());
            }

            StopWatch.getInstance().start("getDataset: " + request.getRequestId());
            DataGroupPart results;
            try {
                results = getResultSet(treq, dbFile);
                jobExecIf(v -> v.progress(90, "generating results..."));
            } catch (Exception e) {
                // table data exists.. but, bad grammar when querying for the resultset.
                // should return table meta info + error message
                // limit 0 does not work with oracle-like syntax
                DataGroup dg = EmbeddedDbUtil.execQuery(DbAdapter.getAdapter(treq), dbFile, "select * from data where ROWNUM < 1", MAIN_DB_TBL);
                results = EmbeddedDbUtil.toDataGroupPart(dg, treq);
                String error = retrieveMsgFromError(e, treq);
                results.setErrorMsg(error);
                jobExecIf(v -> v.setError(500, error));
            }
            StopWatch.getInstance().stop("getDataset: " + request.getRequestId()).printLog("getDataset: " + request.getRequestId());

            // ensure all meta are collected and set accordingly
            TableUtil.consumeColumnMeta(results.getData(), treq);

            int totalRows =  results.getRowCount();
            if (dbFileCreated) {
                if (doLogging()) {
                    SearchProcessor.logStats(treq.getRequestId(), totalRows, 0, false, getDescResolver().getDesc(treq));
                }
                // check for values that can be enumerated..
                if (totalRows < 5000) {
                    enumeratedValuesCheck(dbFile, results, treq);
                } else {
                    enumeratedValuesCheckBG(dbFile, results, treq);        // when it's more than 5000 rows, send it by background so it doesn't slow down response time.
                }
            }

            results.getData().getTableMeta().setAttribute(DataGroupPart.LOADING_STATUS, DataGroupPart.State.COMPLETED.name());

            jobExecIf(v -> v.getJobInfo().setSummary(String.format("%,d rows found", totalRows)));

            return results;
        } finally {
            activeRequests.remove(unigueReqID);
            lock.unlock();
        }
    }

    private File createDbFromRequest(TableServerRequest treq) throws DataAccessException {
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

    /**
     * Fetches the data for the given search request, then save it into a database
     * The database should contains at least 3 named tables: DATA, DD, and META
     * DATA table contains the data
     * DD table contains definition of the columns, including name, label, format, etc
     * META table contains meta information taken from the table.
     *
     * @param req  search request
     * @throws DataAccessException
     */
    protected FileInfo ingestDataIntoDb(TableServerRequest req, File dbFile) throws DataAccessException {

        DbAdapter dbAdapter = DbAdapter.getAdapter(req);

        StopWatch.getInstance().start("fetchDataGroup: " + req.getRequestId());
        DataGroup dg = fetchDataGroup(req);

        StopWatch.getInstance().stop("fetchDataGroup: " + req.getRequestId()).printLog("fetchDataGroup: " + req.getRequestId());

        if (dg == null) throw new DataAccessException("Failed to retrieve data");

        jobExecIf(v -> v.progress(70, dg.size() + " rows of data found"));

        prepareTableMeta(dg.getTableMeta(), Arrays.asList(dg.getDataDefinitions()), req);
        TableUtil.consumeColumnMeta(dg, null);      // META-INFO in the request should only be pass-along and not persist.

        StopWatch.getInstance().start("ingestDataIntoDb: " + req.getRequestId());
        FileInfo finfo = EmbeddedDbUtil.ingestDataGroup(dbFile, dg, dbAdapter, MAIN_DB_TBL);
        StopWatch.getInstance().stop("ingestDataIntoDb: " + req.getRequestId()).printLog("ingestDataIntoDb: " + req.getRequestId());
        return finfo;
    }

    public File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {
        request.cloneRequest();
        request.setPageSize(Integer.MAX_VALUE);
        DataGroupPart results = getData(request);
        File ipacTable = createTempFile(request, ".tbl");
        IpacTableWriter.save(ipacTable, results.getData());
        return ipacTable;
    }

    public FileInfo writeData(OutputStream out, ServerRequest request, TableUtil.Format format) throws DataAccessException {
        try {
            TableServerRequest treq = (TableServerRequest) request;
            DataGroupPart page = getData(request);

            switch(format) {
                case CSV:
                    DsvTableIO.write(new OutputStreamWriter(out), page.getData(), CSVFormat.DEFAULT);
                    break;
                case TSV:
                    DsvTableIO.write(new OutputStreamWriter(out), page.getData(), CSVFormat.TDF);
                    break;
                case VO_TABLE_TABLEDATA:
                case VO_TABLE_BINARY:
                case VO_TABLE_BINARY2:
                case VO_TABLE_FITS:
                    VoTableWriter.save(out, page.getData(), format);
                    break;
                default:
                    IpacTableWriter.save(out, page.getData());
            }
                       // this is not accurate information if used to determine exactly what was written to output stream.
            // dbFile is the database file which contains the whole search results.  What get written to the output
            // stream is based on the given request.
            File dbFile = getDbFile(treq);
            return new FileInfo(dbFile);
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
    }

    public String getUniqueID(ServerRequest request) {
        return EmbeddedDbUtil.getUniqueID((TableServerRequest) request);
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        // This is part of the older api.  In the new API, you should update these info directly in fetchDataGroup().
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.StatsLogResolver();
    }

    protected File createTempFile(TableServerRequest request, String fileExt) throws IOException {
        return File.createTempFile(request.getRequestId(), fileExt, QueryUtil.getTempDir(request));
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
        return MAIN_DB_TBL + (isEmpty(id) ? "" : "_" + DigestUtils.md5Hex(id));
    }

    private static List<String> ignoreCols = Arrays.asList(DataGroup.ROW_IDX, DataGroup.ROW_NUM, "\"" + DataGroup.ROW_IDX + "\"", "\"" + DataGroup.ROW_NUM + "\"");
    protected DataGroupPart getResultSet(TableServerRequest treq, File dbFile) throws DataAccessException {

        String rowIdx = "\"" + DataGroup.ROW_IDX + "\"";
        String rowNum = "\"" + DataGroup.ROW_NUM + "\"";

        String resultSetID = getResultSetID(treq);

        DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
        DbInstance dbInstance = dbAdapter.getDbInstance(dbFile);

        if (!EmbeddedDbUtil.hasTable(treq, dbFile, resultSetID)) {
            // does not exists.. create table from original 'data' table
            List<String> cols = isEmpty(treq.getInclColumns()) ? dbAdapter.getColumnNames(dbInstance, MAIN_DB_TBL, "\"")
                                : StringUtils.asList(treq.getInclColumns(), ",");
            cols = cols.stream().filter((s) -> !ignoreCols.contains(s)).collect(Collectors.toList());   // remove rowIdx and rowNum because it will be automatically added

            String selectPart = (cols.size() == 0 ? "" : StringUtils.toString(cols) + ", " )+ DataGroup.ROW_IDX;
            String wherePart = dbAdapter.wherePart(treq);
            String orderBy = dbAdapter.orderByPart(treq);

            // copy data
            String datasetSql = String.format("select %s from data %s %s", selectPart, wherePart, orderBy);
            String datasetSqlWithIdx = String.format("select b.*, (ROWNUM-1) as %s from (%s) as b", DataGroup.ROW_NUM, datasetSql);
            String sql = dbAdapter.createTableFromSelect(resultSetID, datasetSqlWithIdx);
            JdbcFactory.getSimpleTemplate(dbInstance).update(sql);

            // copy dd
            List<String> cnames = dbAdapter.getColumnNames(dbInstance, resultSetID, "'");
            String ddSql = String.format("select * from data_dd where cname in (%s)", StringUtils.toString(cnames));
            ddSql = dbAdapter.createTableFromSelect(resultSetID + "_dd", ddSql);
            JdbcFactory.getSimpleTemplate(dbInstance).update(ddSql);

            // copy meta
            String metaSql = "select * from data_meta";
            metaSql = dbAdapter.createTableFromSelect(resultSetID + "_meta", metaSql);
            try {
                JdbcFactory.getSimpleTemplate(dbInstance).update(metaSql);
            } catch (Exception mx) {/*ignore table may not exists*/}

            // copy aux
            String auxSql = "select * from data_aux";
            auxSql = dbAdapter.createTableFromSelect(resultSetID + "_aux", auxSql);
            try {
                JdbcFactory.getSimpleTemplate(dbInstance).update(auxSql);
            } catch (Exception ax) {/*ignore table may not exists*/}
        }

        // resultSetID is a table created sort and filter in consideration.  no need to re-apply.
        TableServerRequest nreq = (TableServerRequest) treq.cloneRequest();
        nreq.setFilters(null);
        nreq.setSqlFilter(null);
        nreq.setSortInfo(null);

        if (isEmpty(treq.getInclColumns())) {
            nreq.setInclColumns();
        } else {
            List<String> requestedCols = StringUtils.asList(treq.getInclColumns(), ",");
            List<String> cols = dbAdapter.getColumnNames(dbInstance, resultSetID, "\"");

            // only return these columns if requested
            if (!requestedCols.contains(rowIdx)) cols.remove(rowIdx);
            if (!requestedCols.contains(rowNum)) cols.remove(rowNum);

            nreq.setInclColumns(cols.toArray(new String[0]));
        }

        // handle highlightedRow request if present
        // meta can be HIGHLIGHTED_ROW or HIGHLIGHTED_ROW_BY_ROWIDX
        // if this row exists in the new table, fetch the page where this row is at, and set highlightedRow to it.
        // Otherwise, return as requested.
        int highlightedRow = handleHighlighedRowRequest(treq, nreq, dbFile, resultSetID);

        DataGroupPart page = execRequestQuery(nreq, dbFile, resultSetID);
        page.getData().setHighlightedRow(highlightedRow);

        // handle selectInfo
        // selectInfo is sent to the server as Request.META_INFO.selectInfo
        // it will be moved into TableModel.selectInfo
        SelectionInfo selectInfo = getSelectInfoForThisResultSet(treq, dbAdapter, dbFile, resultSetID, page.getRowCount());
        treq.setSelectInfo(selectInfo);

        // save information needed to recreated this resultset
        treq.setMeta(TableMeta.RESULTSET_REQ, makeResultSetReqStr(treq));
        treq.setMeta(TableMeta.RESULTSET_ID, resultSetID);

        return page;
    }

    // use HIGHLIGHTED_ROW_BY_ROWIDX if exists then check HIGHLIGHTED_ROW
    private int handleHighlighedRowRequest(TableServerRequest treq, TableServerRequest nreq, File dbFile, String resultSetID) {
        int highlightedRow = StringUtils.getInt(treq.getOption(HIGHLIGHTED_ROW), -1);
        int hlRowByRowIdx = StringUtils.getInt(treq.getOption(HIGHLIGHTED_ROW_BY_ROWIDX), -1);
        if (hlRowByRowIdx >= 0) {
            DbAdapter dbAdapter = DbAdapter.getAdapter(nreq);
            try {
                highlightedRow =  JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                        .queryForInt(String.format("select row_num from %s where ROW_IDX = %d", resultSetID, hlRowByRowIdx));
            } catch (Exception e) {
                // row does not exists in new resultset.. reset highlightedRow.
                highlightedRow = 0;
            }
        }
        if (highlightedRow >= 0) {
            // based on highlightedRow, update startIdx
            int currentPage = (int) (Math.floor(highlightedRow / nreq.getPageSize()) + 1);
            int startIdx = (currentPage-1) * nreq.getPageSize();
            nreq.setStartIndex(startIdx);
            treq.setStartIndex(startIdx);
        }
        return highlightedRow;
    }

    private String makeResultSetReqStr(TableServerRequest treq) {
        // only keep state one deep.
        TableServerRequest savedRequest = (TableServerRequest) treq.cloneRequest();
        Map<String, String> meta = savedRequest.getMeta();
        if (meta != null) {
            meta.remove(TableMeta.RESULTSET_REQ);
            meta.remove(TableMeta.RESULTSET_ID);
        }
        savedRequest.setSelectInfo(null);
        return JsonTableUtil.toJsonTableRequest(savedRequest).toString();
    }

    public boolean isSecurityAware() { return false; }

    public void addNewColumn(TableServerRequest tsr, DataType dtype, String expression) {
        File dbFile = getDbFile(tsr);
        DbAdapter dbAdapter = DbAdapter.getAdapter(tsr);
        EmbeddedDbUtil.addNewColumn(dbFile, dbAdapter, dtype, expression);
    }

//====================================================================
//
//====================================================================

    private String ensurePrevResultSetIfExists(TableServerRequest treq, File dbFile, String prevResultSetID) {

        if (!EmbeddedDbUtil.hasTable(treq, dbFile, prevResultSetID)) {
            // does not exists.. create table from original 'data' table
            prevResultSetID = MAIN_DB_TBL;      // if fail to create the previous resultset, use data.
            String resultSetRequest = treq.getMeta().get(TableMeta.RESULTSET_REQ);
            if (!isEmpty(resultSetRequest)) {
                try {
                    TableServerRequest req = QueryUtil.convertToServerRequest(resultSetRequest);
                    DataGroupPart page = getResultSet(req, dbFile);
                    prevResultSetID = page.getData().getAttribute(TableMeta.RESULTSET_ID);
                } catch (DataAccessException e1) {
                    // problem recreating previous
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

        String prevResultSetID = treq.getMeta(TableMeta.RESULTSET_ID);             // the previous resultset ID
        prevResultSetID = isEmpty(prevResultSetID) ? MAIN_DB_TBL : prevResultSetID;

        if ( selectInfo.getSelectedCount() > 0 && !String.valueOf(prevResultSetID).equals(String.valueOf(forTable)) ) {
            // there were row(s) selected from previous resultset.. make sure selectInfo is remapped to new resultset
            prevResultSetID = ensurePrevResultSetIfExists(treq, dbFile, prevResultSetID);

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

            TableServerRequest prevReq = QueryUtil.convertToServerRequest(treq.getMeta().get(TableMeta.RESULTSET_REQ));
            if (treq.getInclColumns() != null && !areEqual(treq.getInclColumns(), prevReq.getInclColumns())) {
                possibleErrors.add(treq.getInclColumns());
            }
            List<String> diff = CollectionUtil.diff(treq.getFilters(), prevReq.getFilters(), false);
            if (diff != null && diff.size() > 0) {
                possibleErrors.addAll(diff);
            }
            if (treq.getSortInfo() != null && !treq.getSortInfo().equals(prevReq.getSortInfo())) {
                possibleErrors.add(treq.getSortInfo().toString());
            }

            if (ex.getRootCause() != null ) {
                String msg = ex.getRootCause().getMessage();
                if (msg != null) {
                    if (msg.toLowerCase().contains("data exception:")) {
                        return msg;
                    }

                    if (msg.toLowerCase().contains(" cast")) {
                        return "Data type mismatch: \n" + StringUtils.toString(possibleErrors, "\n");
                    }

                    if (msg.toLowerCase().contains("unexpected token:")) {
                        return CollectionUtil.get(msg.split("required:"), 0, "").trim() +
                                "\n" + StringUtils.toString(possibleErrors, "\n");
                    }
                }
            }

            return "Invalid statement: \n" + StringUtils.toString(possibleErrors, "\n");
        }

        return e.getMessage();
    }

    private static void enumeratedValuesCheckBG(File dbFile, DataGroupPart results, TableServerRequest treq) {
        RequestOwner owner = ServerContext.getRequestOwner();
        ServerEvent.EventTarget target = new ServerEvent.EventTarget(ServerEvent.Scope.SELF, owner.getEventConnID(),
                                                                        owner.getEventChannel(), owner.getUserKey());
        SHORT_TASK_EXEC.submit(() -> {
            enumeratedValuesCheck(dbFile, results, treq);
            DataGroup updates = new DataGroup(null, results.getData().getDataDefinitions());
            updates.getTableMeta().setTblId(results.getData().getTableMeta().getTblId());
            JSONObject changes = JsonTableUtil.toJsonDataGroup(updates);
            changes.remove("totalRows");        //changes contains only the columns with 0 rows.. we don't want to update totalRows

            FluxAction action = new FluxAction(FluxAction.TBL_UPDATE, changes);
            ServerEventManager.fireAction(action, target);
        });
    }

    private static void enumeratedValuesCheck(File dbFile, DataGroupPart results, TableServerRequest treq) {
        try {
            StopWatch.getInstance().start("enumeratedValuesCheck: " + treq.getRequestId());
            DbAdapter dbAdapter = DbAdapter.getAdapter(treq);
            String cols = Arrays.stream(results.getData().getDataDefinitions())
                    .filter(dt -> maybeEnums(dt))
                    .map(dt -> String.format("count(distinct \"%s\") as \"%s\"", dt.getKeyName(), dt.getKeyName()))
                    .collect(Collectors.joining(", "));

            List<Map<String, Object>> rs = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                    .queryForList(String.format("SELECT %s FROM data where rownum < 500", cols));
            rs.get(0).forEach( (cname,v) -> {
                Long count = (Long) v ;
                if (count > 0 && count <= MAX_COL_ENUM_COUNT) {
                    List<Map<String, Object>> vals = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                            .queryForList(String.format("SELECT distinct \"%s\" FROM data order by 1", cname));

                    if (vals.size() <= MAX_COL_ENUM_COUNT) {
                        String enumVals = vals.stream()
                                .map(m -> m.get(cname) == null ? NULL_TOKEN : m.get(cname).toString())  // list of map to list of string(colname)
                                .collect(Collectors.joining(","));                          // combine the names into comma separated string.
                        results.getData().getDataDefintion(cname).setEnumVals(enumVals);
                        // update dd table
                        JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance(dbFile))
                                .update("UPDATE data_dd SET enumVals = ? WHERE cname = ?", enumVals, cname);
                    }
                }
            });
            StopWatch.getInstance().stop("enumeratedValuesCheck: " + treq.getRequestId()).printLog("enumeratedValuesCheck: " + treq.getRequestId());
        } catch (Exception ex) {
            // do nothing.. ok to ignore errors.
        }
    }

    private static List<Class> onlyCheckTypes = Arrays.asList(String.class, Integer.class, Long.class, Character.class, Boolean.class, Short.class, Byte.class);
    private static List<String> excludeColNames = Arrays.asList(DataGroup.ROW_IDX, DataGroup.ROW_NUM);
    private static boolean maybeEnums(DataType dt) {
        return onlyCheckTypes.contains(dt.getDataType()) && !excludeColNames.contains(dt.getKeyName());

    }

    /**
     * execute the given task if job is still in executing phase.  if job is aborted, throw exception to stop the process.
     * @param f
     */
    protected void jobExecIf(Consumer<Job> f) throws DataAccessException {
        Job job = getJob();
        if (job != null) {
            JobInfo.Phase phase = job.getJobInfo().getPhase();
            if (phase == JobInfo.Phase.EXECUTING) f.accept(job);
            if (phase == JobInfo.Phase.ABORTED) throw new DataAccessException.Aborted();
        }
    }


}

