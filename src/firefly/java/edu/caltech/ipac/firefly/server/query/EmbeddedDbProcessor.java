/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.db.DuckDbReadable;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.table.io.RegionTableWriter;
import edu.caltech.ipac.table.io.VoTableWriter;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.SelectionInfo;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.db.spring.JdbcFactory;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.JsonTableUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.firefly.core.background.Job;
import edu.caltech.ipac.firefly.core.background.JobInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static edu.caltech.ipac.firefly.data.table.MetaConst.HIGHLIGHTED_ROW;
import static edu.caltech.ipac.firefly.data.table.MetaConst.HIGHLIGHTED_ROW_BY_ROWIDX;
import static edu.caltech.ipac.firefly.server.db.DbAdapter.*;
import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.*;
import static edu.caltech.ipac.table.DataGroup.ROW_IDX;
import static edu.caltech.ipac.table.DataGroup.ROW_NUM;
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
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static final SynchronizedAccess GET_DATA_CHECKER = new SynchronizedAccess();
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
     * returns the DbAdapter for the given request.
     * This implementation returns a file based on sessionId + search parameters
     * @param treq
     * @return
     */
    public DbAdapter getDbAdapter(TableServerRequest treq) {
        String reqId = treq.getRequestId();
        String hash = DigestUtils.md5Hex(getUniqueID(treq));
        DbFileCreator dbFileCreator = (ext) -> new File(QueryUtil.getTempDir(treq), "%s_%s.%s".formatted(reqId, hash, ext));
        return DbAdapter.getAdapter(treq, dbFileCreator);
    }

    public DataGroupPart getData(ServerRequest request) throws DataAccessException {
        TableServerRequest treq = (TableServerRequest) request;

        // make sure multiple requests for the same data waits for the first one to create before accessing.
        String uniqueID = this.getUniqueID(request);
        var release = GET_DATA_CHECKER.lock(uniqueID);
        try {
            var dbAdapter = getDbAdapter(treq);
            jobExecIf(v -> v.progress(10, "fetching data..."));
            if (!dbAdapter.hasTable(dbAdapter.getDataTable())) {
                StopWatch.getInstance().start("createDbFile: " + treq.getRequestId());
                createDbFromRequest(treq, dbAdapter);
                StopWatch.getInstance().stop("createDbFile: " + treq.getRequestId()).printLog("createDbFile: " + treq.getRequestId());
            }

            StopWatch.getInstance().start("getDataset: " + request.getRequestId());
            DataGroupPart results;
            try {
                results = getResultSet(treq, dbAdapter);
                jobExecIf(v -> v.progress(90, "generating results..."));
            } catch (Exception e) {
                // table data exists; but, bad grammar when querying for the resultset.
                // should return table meta info + error message
                // limit 0 does not work with oracle-like syntax
                DataGroup dg = dbAdapter.getHeaders(dbAdapter.getDataTable());
                results = EmbeddedDbUtil.toDataGroupPart(dg, treq);
                String error = dbAdapter.handleSqlExp("", e).getCause().getMessage(); // get the message describing the cause of the exception.
                results.setErrorMsg(error);
                jobExecIf(v -> v.setError(500, error));
            }
            StopWatch.getInstance().stop("getDataset: " + request.getRequestId()).printLog("getDataset: " + request.getRequestId());

            // ensure all meta are collected and set accordingly
            TableUtil.consumeColumnMeta(results.getData(), treq);

            int totalRows = results.getRowCount();

            results.getData().getTableMeta().setAttribute(DataGroupPart.LOADING_STATUS, DataGroupPart.State.COMPLETED.name());

            jobExecIf(v -> v.getJobInfo().setSummary(String.format("%,d rows found", totalRows)));

            return results;
        }catch (Exception e) {
            logger.error(e);
            throw e;
        } finally {
            release.run();
        }
    }

    protected void createDbFromRequest(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {
        try {
            FileInfo dbFileInfo = ingestDataIntoDb(treq, dbAdapter);
            if (dbAdapter.hasTable(dbAdapter.getDataTable())) {
                int totalRows = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance()).queryForInt("Select count(*) from " + dbAdapter.getDataTable());
                var headers = dbAdapter.getHeaders(dbAdapter.getDataTable());
                if (doLogging()) {
                    SearchProcessor.logStats(treq.getRequestId(), totalRows, 0, false, getDescResolver().getDesc(treq));
                }

                // check for values that can be enumerated
                if (totalRows < 5000) {
                    enumeratedValuesCheck(dbAdapter, new DataGroupPart(headers, 0, totalRows), treq);
                } else {
                    enumeratedValuesCheckBG(dbAdapter, new DataGroupPart(headers, 0, totalRows), treq);        // when it's more than 5000 rows, send it by background so it doesn't slow down response time.
                }
            }
        } catch (Exception e) {
            dbAdapter.close(true);
            throw dbAdapter.handleSqlExp("", e);
        }
        EmbeddedDbUtil.setDbMetaInfo(treq, dbAdapter);
    }

    /**
     * Fetches the data for the given search request, then save it into a database
     * The database should contains at least 3 named tables: DATA, DD, and META
     * DATA table contains the data
     * DD table contains definition of the columns, including name, label, format, etc
     * META table contains meta information taken from the table.
     *
     * @param req       search request
     * @param dbAdapter
     * @throws DataAccessException
     */
    protected FileInfo ingestDataIntoDb(TableServerRequest req, DbAdapter dbAdapter) throws DataAccessException {

        try {
            dbAdapter.initDbFile();
            StopWatch.getInstance().start("ingestDataIntoDb: " + req.getRequestId());

            // dataSupplier is passed in.  the adapter decides if fetch is needed.
            FileInfo finfo = dbAdapter.ingestData(makeDgSupplier(req, () -> fetchDataGroup(req)), dbAdapter.getDataTable());

            StopWatch.getInstance().stop("ingestDataIntoDb: " + req.getRequestId()).printLog("ingestDataIntoDb: " + req.getRequestId());
            return finfo;
        } catch (IOException ex) {
            logger.error(ex,"Failed to ingest data into the database:" + req.getRequestId());
            throw new DataAccessException(ex);
        }
    }

    protected DataGroupSupplier makeDgSupplier(TableServerRequest req, DataGroupSupplier getter) throws DataAccessException {
        return () -> {
            StopWatch.getInstance().start("fetchDataGroup: " + req.getRequestId());
            DataGroup dg = getter.get();
            StopWatch.getInstance().stop("fetchDataGroup: " + req.getRequestId()).printLog("fetchDataGroup: " + req.getRequestId());
            if (dg == null) throw new DataAccessException("Failed to retrieve data");

            jobExecIf(v -> v.progress(70, dg.size() + " rows of data found"));

            dg.addMetaFrom(collectMeta(req));
            prepareTableMeta(dg.getTableMeta(), Arrays.asList(dg.getDataDefinitions()), req);
            TableUtil.consumeColumnMeta(dg, null);      // META-INFO in the request should only be pass-along and not persist.

            return dg;
        };
    }

    /**
     * @param request   the table request
     * @return  Additional meta info to add to the DataGroup before it's being ingested into the database
     */
    protected DataGroup collectMeta(TableServerRequest request) {
        return null;
    }

    public File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {
        TableServerRequest cr = (TableServerRequest) request.cloneRequest();
        cr.setPageSize(Integer.MAX_VALUE);
        DataGroupPart results = getData(cr);
        File ipacTable = createTempFile(cr, ".tbl");
        IpacTableWriter.save(ipacTable, results.getData());
        return ipacTable;
    }

    public FileInfo writeData(OutputStream out, ServerRequest request, TableUtil.Format format, TableUtil.Mode mode) throws DataAccessException {
        try {
            TableServerRequest treq = (TableServerRequest) request;
            var dbAdapter =  getDbAdapter(treq);
            if (mode.equals(TableUtil.Mode.original)) {
                treq = (TableServerRequest) treq.cloneRequest();
                treq.keepBaseParamOnly();
                DataGroup table = dbAdapter.getHeaders(dbAdapter.getDataTable());  // just the headers.
                String[] cols = Arrays.stream(table.getDataDefinitions())
                                .filter(c -> c.getDerivedFrom() == null                                     // remove derived columns
                                             && !CollectionUtil.exists(c.getKeyName(), ROW_IDX, ROW_NUM))   // remove system added columns
                                .map(dt -> "\"" + dt.getKeyName() + "\"")
                                .toArray(String[]::new);
                treq.setInclColumns(cols);
            }
            switch(format) {
                case CSV:
                    new DuckDbReadable.Csv(dbAdapter.getDbFile()).export(treq, out);
                    return new FileInfo(dbAdapter.getDbFile());
                case TSV:
                    new DuckDbReadable.Tsv(dbAdapter.getDbFile()).export(treq, out);
                    return new FileInfo(dbAdapter.getDbFile());
                case PARQUET:
                    new DuckDbReadable.Parquet(dbAdapter.getDbFile()).export(treq, out);
                    return new FileInfo(dbAdapter.getDbFile());
            }

            DataGroupPart page = getData(treq);
            switch(format) {
                case REGION:
                    RegionTableWriter.write(new OutputStreamWriter(out), page.getData(), request.getParam("center_cols"));
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
            return new FileInfo(dbAdapter.getDbFile());
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
    }

    public String getUniqueID(ServerRequest request) {
        return EmbeddedDbUtil.getUniqueID((TableServerRequest) request);
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        // This is part of the older api.  Opportunity for SearchProcessor to add additonal TaIn the new API, you should update these info directly in fetchDataGroup().
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.StatsLogResolver();
    }

    protected File createTempFile(TableServerRequest request, String fileExt) throws IOException {
        return File.createTempFile(request.getRequestId(), fileExt, QueryUtil.getTempDir(request));
    }

    /**
     * In this implementation, this flag is not used.  It will only fetch new data when local data(database) does not exist.
     * The frequency and amount of caching depend on #getUniqueID and cleanup schedule.
     */
    public boolean doCache() {return false;}
    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {}
    public boolean doLogging() {return true;}

    /**
     * returns the table name for this request. 
     */
    @NotNull
    public String getResultSetID(TableServerRequest treq) {
        String id = StringUtils.toString(treq.getResultSetParam(), "|");
        String rsid =  MAIN_DATA_TBL + (isEmpty(id) ? "" : "_" + DigestUtils.md5Hex(id));
        return rsid.toUpperCase();
    }

    protected DataGroupPart getResultSet(TableServerRequest treq, DbAdapter dbAdapter) throws DataAccessException {

        String rowIdx = "\"" + DataGroup.ROW_IDX + "\"";
        String rowNum = "\"" + DataGroup.ROW_NUM + "\"";

        String resultSetID = getResultSetID(treq);

        if (!dbAdapter.hasTable(resultSetID)) {
            // does not exist; create table from original 'data' table
            dbAdapter.createTempResults(treq, resultSetID);
        }

        // resultSetID is a table created with sort and filter in consideration.  no need to re-apply.
        TableServerRequest nreq = (TableServerRequest) treq.cloneRequest();
        nreq.setFilters(null);
        nreq.setSqlFilter(null);
        nreq.setSortInfo(null);
        nreq.setInclColumns();

//        if (isEmpty(treq.getInclColumns())) {
//            nreq.setInclColumns();
//        } else {
//            List<String> requestedCols = StringUtils.asList(treq.getInclColumns(), ",");
////            List<String> cols = dbAdapter.getColumnNames(resultSetID, "\"");
//
//            // only return these columns if requested
//            if (!requestedCols.contains(rowIdx)) requestedCols.remove(rowIdx);
//            if (!requestedCols.contains(rowNum)) requestedCols.remove(rowNum);
//
//            nreq.setInclColumns(requestedCols.toArray(new String[0]));
//        }

        // handle highlightedRow request if present
        // meta can be HIGHLIGHTED_ROW or HIGHLIGHTED_ROW_BY_ROWIDX
        // if this row exists in the new table, fetch the page where this row is at, and set highlightedRow to it.
        // Otherwise, return as requested.
        int highlightedRow = handleHighlighedRowRequest(treq, nreq, dbAdapter, resultSetID);

        DataGroupPart page = dbAdapter.execRequestQuery(nreq, resultSetID);
        page.getData().setHighlightedRow(highlightedRow);

        // handle selectInfo
        // selectInfo is sent to the server as Request.META_INFO.selectInfo
        // it will be moved into TableModel.selectInfo
        SelectionInfo selectInfo = getSelectInfoForThisResultSet(treq, dbAdapter, resultSetID, page.getRowCount());
        treq.setSelectInfo(selectInfo);

        // save information needed to recreate this resultset
        treq.setMeta(TableMeta.RESULTSET_REQ, makeResultSetReqStr(treq));
        treq.setMeta(TableMeta.RESULTSET_ID, resultSetID);

        return page;
    }

    // use HIGHLIGHTED_ROW_BY_ROWIDX if exists then check HIGHLIGHTED_ROW
    private int handleHighlighedRowRequest(TableServerRequest treq, TableServerRequest nreq, DbAdapter dbAdapter, String resultSetID) {
        int highlightedRow = StringUtils.getInt(treq.getOption(HIGHLIGHTED_ROW), -1);
        int hlRowByRowIdx = StringUtils.getInt(treq.getOption(HIGHLIGHTED_ROW_BY_ROWIDX), -1);
        if (hlRowByRowIdx >= 0) {
            try {
                highlightedRow =  JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance())
                        .queryForInt(String.format("select row_num from %s where ROW_IDX = %d", resultSetID, hlRowByRowIdx));
            } catch (Exception e) {
                // row does not exist in new resultset; reset highlightedRow.
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

    public void addOrUpdateColumn(TableServerRequest tsr, DataType dtype, String expression, String editColName, String preset) throws DataAccessException {
        DbAdapter dbAdapter = getDbAdapter(tsr);
        // ensure resultSetID table exists
        TableServerRequest cr = (TableServerRequest) tsr.cloneRequest();
        cr.setPageSize(1);
        getResultSet(cr, dbAdapter);

        String resultSetID = getResultSetID(tsr);
        SelectionInfo si = tsr.getSelectInfo();
        if (isEmpty(editColName)) {
            EmbeddedDbUtil.addColumn(dbAdapter, dtype, expression, preset, resultSetID, si);
        } else {
            EmbeddedDbUtil.updateColumn(dbAdapter, dtype, expression, editColName, preset, resultSetID, si);
        }
    }

    public void deleteColumn(TableServerRequest tsr, String cname) {
        DbAdapter dbAdapter= getDbAdapter(tsr);
        EmbeddedDbUtil.deleteColumn(dbAdapter, cname);
    }

//====================================================================
//
//====================================================================

    private String ensurePrevResultSetIfExists(TableServerRequest treq, DbAdapter dbAdapter, String prevResultSetID) {
        if (!dbAdapter.hasTable(prevResultSetID)) {
            // does not exist; create table from original 'data' table
            prevResultSetID = dbAdapter.getDataTable();      // if fail to create the previous resultset, use data.
            String resultSetRequest = treq.getMeta().get(TableMeta.RESULTSET_REQ);
            if (!isEmpty(resultSetRequest)) {
                try {
                    TableServerRequest req = QueryUtil.convertToServerRequest(resultSetRequest);
                    DataGroupPart page = getResultSet(req, dbAdapter);
                    prevResultSetID = page.getData().getAttribute(TableMeta.RESULTSET_ID);
                } catch (DataAccessException e1) {
                    // problem recreating previous
                }
            }
        }
        return prevResultSetID;
    }

    private SelectionInfo getSelectInfoForThisResultSet(TableServerRequest treq, DbAdapter dbAdapter, String forTable, int rowCnt) {

        SelectionInfo selectInfo = treq.getSelectInfo();
        if (selectInfo == null) {
            selectInfo = new SelectionInfo(false, null, rowCnt);
        }

        String prevResultSetID = treq.getMeta(TableMeta.RESULTSET_ID);             // the previous resultset ID
        prevResultSetID = isEmpty(prevResultSetID) ? dbAdapter.getDataTable() : prevResultSetID;

        if ( selectInfo.getSelectedCount() > 0 && !String.valueOf(prevResultSetID).equals(String.valueOf(forTable)) ) {
            // there were row(s) selected from previous resultset; make sure selectInfo is remapped to new resultset
            prevResultSetID = ensurePrevResultSetIfExists(treq, dbAdapter, prevResultSetID);

            String rowNums = StringUtils.toString(selectInfo.getSelected());
            SimpleJdbcTemplate jdbc = JdbcFactory.getSimpleTemplate(dbAdapter.getDbInstance());
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

    private static String retrieveMsgFromError(Exception e, TableServerRequest treq, DbAdapter dbAdapter) {

        if (e.getCause() instanceof SQLException) {
            var x = dbAdapter.handleSqlExp("Invalid statement", e);
            return "%s: %s".formatted(x.getMessage(), x.getCause().getMessage());
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
                        return "Data type mismatch: " + StringUtils.toString(possibleErrors, "\n");
                    }

                    if (msg.toLowerCase().contains("unexpected token:")) {
                        return CollectionUtil.get(msg.split("required:"), 0, "").trim() +
                                ":" + StringUtils.toString(possibleErrors, "\n");
                    }
                }
            }

            return "Invalid statement: " + StringUtils.toString(possibleErrors, "\n");
        }
        String[] errorCause = ServCommand.getErrorCause(e);
        return errorCause[0] + (errorCause[1] != null ? ": " + errorCause[1] : "");
        // TODO: ServCommand returns separate strings for 'error' and 'cause'.  This is then converted to JSON and passed along as javascript Error.
        // This function however will only return a single string.  It's too much work to separate them in this current task.
        // I will use the format 'error:cause' as a way to transport these messages.
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

