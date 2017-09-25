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
     * @return the database file with sizeInBytes representing to the number of rows.
     * @throws DataAccessException
     */
    abstract public FileInfo createDbFile(TableServerRequest req) throws DataAccessException;


    public DataGroupPart getData(ServerRequest request) throws DataAccessException {
        TableServerRequest treq = (TableServerRequest) request;

        String unigueReqID = this.getUniqueID(request);


//        lockChecker.lock();
        ReentrantLock lock = null;
        try {
            lock = activeRequests.get(unigueReqID);
            if (lock == null) {
                lock = new ReentrantLock();
                activeRequests.put(unigueReqID, lock);
            }
        } finally {
//            lockChecker.unlock();
        }


        // make sure multiple requests for the same data waits for the first one to create before acessing.
        lock.lock();
        try {
            File dbFile = EmbeddedDbUtil.getDbFile(treq);
            if (dbFile == null || !dbFile.canRead()) {
                StopWatch.getInstance().start("createDbFile: " + request.getRequestId());
                FileInfo dbFileInfo = createDbFile(treq);
                dbFile = dbFileInfo.getFile();
                EmbeddedDbUtil.setDbMetaInfo(treq, DbAdapter.getAdapter(treq), dbFile);

                if (doLogging()) {
                    SearchProcessor.logStats(treq.getRequestId(), (int)dbFileInfo.getSizeInBytes(), 0, false, getDescResolver().getDesc(treq));
                }
                StopWatch.getInstance().stop("createDbFile: " + request.getRequestId()).printLog("createDbFile: " + request.getRequestId());
            }
            StopWatch.getInstance().start("getDataset: " + request.getRequestId());
            DataGroupPart results = getDataset(treq, dbFile);
            StopWatch.getInstance().stop("getDataset: " + request.getRequestId()).printLog("getDataset: " + request.getRequestId());
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
        return SearchProcessor.getUniqueIDDef((TableServerRequest) request);
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        SearchProcessor.prepareTableMetaDef(defaults, columns, request);
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
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
            EmbeddedDbUtil.setupDatasetTable(treq);

            // select a page from the dataset table
            String pageSql = getPageSql(dbAdapter, treq);
            DataGroupPart page = EmbeddedDbUtil.getResults(treq, pageSql);

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

