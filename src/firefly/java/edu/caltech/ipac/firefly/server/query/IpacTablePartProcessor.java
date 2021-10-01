/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;


import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.PrivateCache;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.IpacTableDef;
import edu.caltech.ipac.table.IpacTableUtil;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.IpacTableReader;
import edu.caltech.ipac.table.io.IpacTableWriter;
import edu.caltech.ipac.table.query.DataGroupQueryStatement;
import edu.caltech.ipac.table.query.FilterHanlder;
import edu.caltech.ipac.table.query.InvalidStatementException;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.apache.commons.httpclient.HttpStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.data.TableServerRequest.FILTERS;
import static edu.caltech.ipac.firefly.data.TableServerRequest.FIXED_LENGTH;
import static edu.caltech.ipac.firefly.data.TableServerRequest.INCL_COLUMNS;
import static edu.caltech.ipac.firefly.data.TableServerRequest.META_INFO;
import static edu.caltech.ipac.firefly.data.TableServerRequest.PAGE_SIZE;
import static edu.caltech.ipac.firefly.data.TableServerRequest.SORT_INFO;
import static edu.caltech.ipac.firefly.data.TableServerRequest.START_IDX;
import static edu.caltech.ipac.firefly.data.TableServerRequest.TBL_ID;


/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: IpacTablePartProcessor.java,v 1.33 2012/10/23 18:37:22 loi Exp $
 */
abstract public class IpacTablePartProcessor implements SearchProcessor<DataGroupPart>, SearchProcessor.CanGetDataFile, SearchProcessor.CanFetchDataGroup {

    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    //public static long logCounter = 0;
    private static final List<String> PAGE_PARAMS = Arrays.asList(PAGE_SIZE, START_IDX);
    private static final String SYS_PARAMS = "|" + StringUtils.toString(new String[]{FILTERS,SORT_INFO,PAGE_SIZE,START_IDX,INCL_COLUMNS,FIXED_LENGTH,META_INFO,TBL_ID}, "|") + "|";



    private static final Map<StringKey, Object> _activeRequests =
                Collections.synchronizedMap(new HashMap<>());

    boolean doLogging = true;

    protected void setDoLogging(boolean flg) {
        doLogging = flg;
    }

    protected static IOException makeException(Exception e, String reason) {
        IOException eio = new IOException(reason);
        eio.initCause(e);
        return eio;
    }

    public boolean isSecurityAware() {
        return false;
    }

    public void downloadFile(URL url, File outFile) throws IOException, EndUserException {
        try {
            HttpServiceInput inputs = HttpServiceInput.createWithCredential(url.toString());
            Map<String,String> headers= new HashMap<>();
            if (inputs.getHeaders()!=null) headers.putAll(inputs.getHeaders());
            headers.put("Accept", "*/*");
            URLDownload.getDataToFile(url, outFile,null, headers);
        } catch (FailedRequestException e) {
            if (e.getResponseCode() > -1) {
                throw new EndUserException("Search Failed: " + HttpStatus.getStatusText(e.getResponseCode()), e.getDetailMessage(), e);
            } else {
                throw makeException(e, "Query Failed - network error.");
            }
        }
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
    }

    /**
     * Default behavior is to read file, created by getDataGroupFile
     *
     * @param sr - server request
     * @return data group part
     * @throws DataAccessException when unable to obtain data
     */
    public DataGroupPart getData(ServerRequest sr) throws DataAccessException {
        File dgFile = null;
        try {
            TableServerRequest request = (TableServerRequest) sr;
            Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);
            // get unique key without page info
            StringKey key = new StringKey(this.getClass().getName(), getDataKey(request));

            try {
                Object lockKey;
                boolean lockKeyCreator = false;
                synchronized (_activeRequests) {
                    lockKey = _activeRequests.get(key);
                    if (lockKey == null) {
                        lockKey = new Object();
                        _activeRequests.put(key, lockKey);
                        lockKeyCreator = true;
                    }
                }
                synchronized (lockKey) {
                    if (!lockKeyCreator) {
                        dgFile = validateFile((File) cache.get(key));
                    }
                    if (dgFile == null) {
                        dgFile = getDataFile(request);

                        cache.put(key, dgFile);
                    }
                }

            } finally {
                _activeRequests.remove(key);
            }


            DataGroupPart page;
            // get the page requested
            if (dgFile == null || !dgFile.exists() || dgFile.length() == 0) {
                DataGroup dg = new DataGroup("No result found", new DataType[0]);
                dg.addAttribute(DataGroupPart.LOADING_STATUS, DataGroupPart.State.COMPLETED.name());
                page = new DataGroupPart(dg, 0, 0);
            } else {
                try {
                    dgFile = postProcessData(dgFile, request);
                    page = TableUtil.getData(dgFile, request.getStartIndex(), request.getPageSize());
                    ensureTableMeta(page, request, dgFile);  // inspect/edit meta info needed by client.
                } catch (Exception e) {
                    LOGGER.error(e, "Fail to parse ipac table file: " + dgFile);
                    throw e;
                }
            }

            onComplete(request, page);

            return page;
        } catch (DataAccessException dae) {
            throw dae;
        } catch (Exception e) {
            LOGGER.error(e, "Error while processing request:" + StringUtils.truncate(sr, 512));
            String message = e.getMessage();
            if (message == null || message.length() < 5) message = "Unexpected error";
            throw new DataAccessException(message, e);
        }
    }

    private void ensureTableMeta(DataGroupPart page, TableServerRequest request, File dgFile) {
        // make sure there's a status line so
        TableMeta meta = page.getData().getTableMeta();
        String status = meta.getAttribute(DataGroupPart.LOADING_STATUS);
        if (StringUtils.isEmpty(status)) {
            meta.setAttribute(DataGroupPart.LOADING_STATUS, DataGroupPart.State.COMPLETED.name());
        }
        meta.setAttribute(TableServerRequest.TBL_FILE_PATH, ServerContext.replaceWithPrefix(dgFile));  // set table's meta tblFilePath to the file it came from.
        if (!StringUtils.isEmpty(request.getTblTitle())) {
            page.getData().setTitle(request.getTblTitle());  // set the datagroup's title to the request title.
        }
    }

    protected File postProcessData(File dgFile, TableServerRequest request) throws Exception {
        return dgFile;
    }

    public boolean doCache() {
        return true;
    }

    public boolean doLogging() {
        return doLogging;
    }

    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
    }

    /**
     * return the unique ID for the original data set of this request.  This means parameters related
     * to paging, filtering, sorting, decimating, etc or ignored.
     * @param request
     * @return
     */
    public String getUniqueID(ServerRequest request) {
        // parameters to get original data (before filter, sort, etc.)
        List<Param> srvParams = new ArrayList<>();
        for (Param p : request.getParams()) {
             if (!SYS_PARAMS.contains("|" + p.getName() + "|")) {
                 srvParams.add(p);
             }
        }
        return createUniqueId(request.getRequestId(), srvParams);
    }

    /**
     * return the unique ID of this request ignoring only the paging parameters.
     * This is used to identify a unique data set returned to the client.
     * @param request
     * @return
     */
    public String getDataKey(ServerRequest request) {

        List<Param> srvParams = new ArrayList<>();
        for (Param p : request.getParams()) {
            if (!PAGE_PARAMS.contains(p.getName())) {
                srvParams.add(p);
            }
        }
        return createUniqueId(request.getRequestId(), srvParams);
    }

    private String createUniqueId(String reqId, List<Param> params) {
        String uid = reqId + "-";
        if ( isSecurityAware() &&
                ServerContext.getRequestOwner().isAuthUser() ) {
            uid = uid + ServerContext.getRequestOwner().getUserKey();
        }

        // sort by parameter name
        Collections.sort(params, (p1, p2) -> p1.getName().compareTo(p2.getName()));

        for (Param p : params) {
            uid += "|" + p.toString();
        }

        return uid;
    }

    public FileInfo writeData(OutputStream out, ServerRequest sr, TableUtil.Format format) throws DataAccessException {
        try {
            TableServerRequest request = (TableServerRequest) sr;

            File inf = getDataFile(request);
            if (inf != null && inf.canRead()) {
                int rows = IpacTableUtil.getMetaInfo(inf).getRowCount();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out),
                        IpacTableUtil.FILE_IO_BUFFER_SIZE);
                BufferedReader reader = new BufferedReader(new FileReader(inf),
                        IpacTableUtil.FILE_IO_BUFFER_SIZE);

                prepareAttributes(rows, writer, sr);
                String s = reader.readLine();
                while (s != null) {
                    if (!(s.startsWith("\\col.") ||
                            s.startsWith("\\Loading"))) {           // ignore ALL system-use headers
                        if (s.startsWith("|") && getOutputColumnsMap() != null)
                            for (String key : getOutputColumnsMap().keySet()) {
                                s = s.replaceAll(key, getOutputColumnsMap().get(key));
                            }
                        writer.write(s);
                        writer.newLine();
                    }
                    s = reader.readLine();
                }
                writer.flush();
                return new FileInfo(inf);
            } else {
                throw new DataAccessException("Data not accessible.  Check server log for errors.");
            }
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
    }

    public File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {
        LOGGER.warn("<< slow getDataFile called." + this.getClass().getSimpleName());

        Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);

        // if decimation or sorting is requested, you cannot background writing the file to speed up response time.
        boolean noBgWrite = DecimationProcessor.getDecimateInfo(request) != null || request.getSortInfo() != null;

        int oriPageSize = request.getPageSize();
        if (noBgWrite) {
            request.setPageSize(Integer.MAX_VALUE);
        }

        // go get original data
        File resultsFile = getBaseDataFile(request);      // caching already done..

        if (resultsFile == null || !resultsFile.canRead()) return null;

        // from here on.. we use resultsFile as the cache key.
        // if the source file changes, we ignore previously cached temp files
        StringKey key = new StringKey(resultsFile.getPath());

        // do filtering
        CollectionUtil.Filter<DataObject>[] filters = QueryUtil.convertToDataFilter(request.getFilters());
        if (filters != null && filters.length > 0) {
            key = key.appendToKey((Object[]) filters);
            File filterFile = validateFile((File) cache.get(key));
            if (filterFile == null) {
                filterFile = File.createTempFile(getFilePrefix(request), ".tbl", QueryUtil.getTempDir(request));
                doFilter(filterFile, resultsFile, filters, request);
                cache.put(key, filterFile);
            }
            resultsFile = filterFile;
        }

        // do sorting...
        SortInfo sortInfo = request.getSortInfo();
        if (sortInfo != null) {
            key = key.appendToKey(sortInfo);
            File sortedFile = validateFile((File) cache.get(key));
            if (sortedFile == null) {
                sortedFile = File.createTempFile(getFilePrefix(request), ".tbl", QueryUtil.getTempDir(request));
                doSort(resultsFile, sortedFile, sortInfo, request);
                cache.put(key, sortedFile);
            }
            resultsFile = sortedFile;
        }

        // do decimation
        DecimateInfo decimateInfo = DecimationProcessor.getDecimateInfo(request);
        if (decimateInfo != null) {
            key = key.appendToKey(decimateInfo);
            File deciFile = validateFile((File) cache.get(key));
            if (deciFile == null) {
                // only read in the required columns
                String xColExpr = decimateInfo.getxColumnName();
                String yColExpr = decimateInfo.getyColumnName();
                String [] requestedCols = new String[]{xColExpr, yColExpr};

                DataGroup dg = IpacTableReader.read(resultsFile, requestedCols);

                deciFile = File.createTempFile(getFilePrefix(request), ".tbl", QueryUtil.getTempDir(request));
                DataGroup retval = QueryUtil.doDecimation(dg, decimateInfo);
                IpacTableWriter.save(deciFile, retval);
                cache.put(key, deciFile);
            }
            resultsFile = deciFile;
        }

        // return only the columns requested, ignore when decimation is requested
        String ic = request.getParam(TableServerRequest.INCL_COLUMNS);
        if (decimateInfo == null && !StringUtils.isEmpty(ic) && !ic.equals("ALL")) {
            key = key.appendToKey(ic);
            File subFile = validateFile((File) cache.get(key));
            if (subFile == null) {
                subFile = File.createTempFile(getFilePrefix(request), ".tbl", QueryUtil.getTempDir(request));
                String sql = "select col " + ic + " from " + resultsFile.getAbsolutePath() + " into " + subFile.getAbsolutePath() + " with complete_header";
                try {
                    DataGroupQueryStatement.parseStatement(sql).executeInline();
                } catch (InvalidStatementException e) {
                    throw new DataAccessException("InvalidStatementException", e);
                }
                cache.put(key, subFile);
            }
            resultsFile = subFile;
        }

        if (noBgWrite) {
            request.setPageSize(oriPageSize);
        }

        return resultsFile;
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {}

    public void prepareAttributes(int rows, BufferedWriter writer, ServerRequest sr) throws IOException {
    }

    public Map<String, String> getOutputColumnsMap() {
        return null;
    }

    protected void doSort(File inFile, File outFile, SortInfo sortInfo, TableServerRequest request) throws IOException {
        LOGGER.warn("<< very slow doSort called." + this.getClass().getSimpleName());
        // do sorting...
        StopWatch timer = StopWatch.getInstance();
        timer.start("read");
        int pageSize = request.getPageSize();
        DataGroup dg = IpacTableReader.read(inFile);
        // if this file does not contain ROW_IDX, add it.
        if (!dg.containsKey(DataGroup.ROW_IDX)) {
            dg.addDataDefinition(DataGroup.makeRowIdx());
            dg.addAttribute("col." + DataGroup.ROW_IDX + ".Visibility", "hidden");
        }
        timer.printLog("read");
        timer.start("sort");
        QueryUtil.doSort(dg, sortInfo);
        timer.printLog("sort");
        timer.start("write");
        IpacTableWriter.save(outFile, dg);
        timer.printLog("write");
    }


//====================================================================
//
//====================================================================

    protected Cache getCache() {
        return new PrivateCache(ServerContext.getRequestOwner().getUserKey(), CacheManager.getCache(Cache.TYPE_PERM_FILE));
    }

    /**
     * NOTE:  Because this method is added afterward, most subclasses are fetching and saving IPAC table in loadDataFile
     *        Both methods should be overridden when dealing with non-IPAC table format so that data is not lost.
     * @param req
     * @return
     * @throws DataAccessException
     */
    public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException {
        try {
            return IpacTableReader.read(loadDataFile(req));
        } catch (IOException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    /**
     * subclass provide how the data are collected
     *
     * @param request table request
     * @return file with the data
     * @throws java.io.IOException
     * @throws edu.caltech.ipac.firefly.server.query.DataAccessException
     *
     */
    abstract protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException;


    /**
     * This is the proper implementation of loadDataFile if fetchDataGroup were overridden.
     * @param request
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    protected File loadDataFileImpl(TableServerRequest request) throws IOException, DataAccessException {
        DataGroup dg = fetchDataGroup(request);
        File outFile = createFile(request);
        IpacTableWriter.save(outFile, dg);
        return outFile;
    }

    private File validateFile(File inf) {
        if (inf != null) {
            if (!inf.canRead()) {
                LOGGER.warn("File returned from cache, but is not accessible:" + inf.getAbsolutePath());
                inf = null;
            }
        }
        return inf;
    }

    /**
     * return the file containing data before filter and sort.
     *
     * @param request table request
     * @return file with the base data (before filter, sort, etc.)
     * @throws IOException
     * @throws DataAccessException
     */
    private File getBaseDataFile(TableServerRequest request) throws IOException, DataAccessException {

        StringKey key = new StringKey(IpacTablePartProcessor.class.getName(), getUniqueID(request));

        Cache cache = getCache();
        File cfile = validateFile((File) cache.get(key));

        boolean isFromCache = true;
        if (cfile == null) {
            cfile = loadDataFile(request);

            if (doCache()) {
                cache.put(key, cfile);
            }
            isFromCache = false;
        }

        if (isInitLoad(request)) {
            // maintain counters for application monitoring
            Counters.getInstance().incrementSearch("Total Searches");
            if (isFromCache) {
                Counters.getInstance().incrementSearch("From Cache");
            }
            Counters.getInstance().incrementSearch(request.getRequestId());


            // do stats logging when appropriate
            if (doLogging()) {
                int rowCount = 0;
                long fileSize = 0;
                if (cfile != null) {
                    try {
                        IpacTableDef meta = IpacTableUtil.getMetaInfo(cfile);
                        if (meta.getStatus() == DataGroupPart.State.INPROGRESS) {
                            fileSize = (meta.getRowCount() * meta.getLineWidth()) + meta.getRowStartOffset();
                        } else {
                            fileSize = cfile.length();
                        }
                        rowCount = meta.getRowCount();
                    } catch (IOException iox) {
                        throw new IOException("File:" + cfile, iox);
                    }
                }

                SearchProcessor.logStats(request.getRequestId(), rowCount, fileSize, isFromCache, getDescResolver().getDesc(request));
            }
        }

        return cfile;
    }

    private boolean isInitLoad(TableServerRequest req) {
        List<String> filters = req.getFilters();
        return req.getPageSize() > 0 && req.getStartIndex() == 0 &&
                (filters == null || filters.size() == 0) && req.getSortInfo() == null;
    }

    protected void doFilter(File outFile, File source, CollectionUtil.Filter<DataObject>[] filters, TableServerRequest request) throws IOException {
        LOGGER.warn("<< very slow doFilter called." + this.getClass().getSimpleName());
        StopWatch timer = StopWatch.getInstance();
        timer.start("filter");
        IpacTableWriter.asyncSave(new FilterHanlder(outFile, source, filters, request));
        timer.printLog("filter");
    }

    /*
     * @return prefix for a file, where query results are going to be stored
     */
    protected String getFilePrefix(TableServerRequest request) {
        return request.getRequestId();
    }

    protected File createFile(TableServerRequest request) throws IOException {
        return createFile(request, ".tbl");
    }

    protected File createFile(TableServerRequest request, String fileExt) throws IOException {
        return File.createTempFile(getFilePrefix(request), fileExt, QueryUtil.getTempDir(request));
    }

}
