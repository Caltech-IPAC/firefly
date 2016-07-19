/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.InvalidStatementException;
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.DecimateInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.WspaceMeta;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.firefly.server.cache.PrivateCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupFilter;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.server.util.ipactable.TableDef;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.expr.Expression;
import org.apache.commons.httpclient.HttpStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: IpacTablePartProcessor.java,v 1.33 2012/10/23 18:37:22 loi Exp $
 */
abstract public class IpacTablePartProcessor implements SearchProcessor<DataGroupPart> {

    public static final boolean useWorkspace = AppProperties.getBooleanProperty("useWorkspace", false);

    public static final Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    //public static long logCounter = 0;
    public static final String SYS_PARAMS = TableServerRequest.SYS_PARAMS;
    public static final List<String> PAGE_PARAMS = Arrays.asList(TableServerRequest.PAGE_SIZE, TableServerRequest.START_IDX);


    private static final Map<StringKey, Object> _activeRequests =
                Collections.synchronizedMap(new HashMap<>());

    public boolean isSecurityAware() {
        return false;
    }

    public void downloadFile(URL url, File outFile) throws IOException, EndUserException {
        URLConnection conn = null;
        try {
            Map<String, String> cookies = isSecurityAware() ? ServerContext.getRequestOwner().getIdentityCookies() : null;
            conn = URLDownload.makeConnection(url, cookies);
            conn.setRequestProperty("Accept", "*/*");
            URLDownload.getDataToFile(conn, outFile);

        } catch (MalformedURLException e) {
            LOGGER.error(e, "Bad URL");
            throw makeException(e, "Query Failed - bad url.");

        } catch (FailedRequestException e) {
            LOGGER.error(e, e.toString());
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int respCode = httpConn.getResponseCode();
                String desc = respCode == 200 ? e.getMessage() : HttpStatus.getStatusText(respCode);
                throw new EndUserException("Search Failed: " + desc, e.getDetailMessage(), e);
            } else {
                throw makeException(e, "Query Failed - network error.");
            }
        } catch (IOException e) {
            if (conn != null && conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                int respCode = httpConn.getResponseCode();
                String desc = respCode == 200 ? e.getMessage() : HttpStatus.getStatusText(respCode);
                throw new EndUserException("Search Failed: " + desc, e.getMessage(), e);
            } else {
                throw makeException(e, "Query Failed - network error.");
            }
        }
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
    }

    protected static IOException makeException(Exception e, String reason) {
        IOException eio = new IOException(reason);
        eio.initCause(e);
        return eio;
    }

    public ServerRequest inspectRequest(ServerRequest request) {
        TableServerRequest req = (TableServerRequest) request;
        String doPadding = req.getMeta("padResults");
        if (Boolean.parseBoolean(doPadding)) {
            // if we need to pad the results, change the request.
            req = (TableServerRequest) req.cloneRequest();
            int start = Math.max(req.getStartIndex() - 50, 0);
            req.setStartIndex(start);
            req.setPageSize(req.getPageSize() + 100);
            ((TableServerRequest)request).setStartIndex(start);   // the original request needs to be modify as well.
            return req;
        } else {
            return request;
        }
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
                TableDef def = new TableDef();
                def.setStatus(DataGroupPart.State.COMPLETED);
                page = new DataGroupPart(def, new DataGroup("No result found", new DataType[0]), 0, 0);
            } else {
                try {
                    dgFile = postProcessData(dgFile, request);
                    page = IpacTableParser.getData(dgFile, request.getStartIndex(), request.getPageSize());
                    page.getTableDef().ensureStatus();      // make sure there's a status line so
                    page.getTableDef().setAttribute(TableServerRequest.TBL_FILE_PATH, ServerContext.replaceWithPrefix(dgFile));  // set table's meta tblFilePath to the file it came from.
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
            throw new DataAccessException("Unexpected error", e);
        }
//        finally {
//            if (!doCache()) {
// do not delete file even if it's not to be cached.  download still relies on it.
//                if (dgFile != null) {
//                    dgFile.delete();
//                }
//            }
//        }

    }

    protected File postProcessData(File dgFile, TableServerRequest request) throws Exception {
        return dgFile;
    }

    public boolean doCache() {
        return true;
    }

    public boolean doLogging() {
        return true;
    }

    public void onComplete(ServerRequest request, DataGroupPart results) throws DataAccessException {
    }

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

    // unique key without page info
    public String getDataKey(ServerRequest request) {

        List<Param> srvParams = new ArrayList<>();
        for (Param p : request.getParams()) {
            if (!SYS_PARAMS.contains("|" + p.getName() + "|") &&
                !PAGE_PARAMS.contains(p.getName())) {
                srvParams.add(p);
            }
        }
        return createUniqueId(request.getRequestId(), srvParams);
    }

    private String createUniqueId(String reqId, List<Param> params) {
        String uid = reqId + "-";
        if ( useWorkspace || (isSecurityAware() &&
                ServerContext.getRequestOwner().isAuthUser()) ) {
            uid = uid + ServerContext.getRequestOwner().getUserKey();
        }

        // sort by parameter name
        Collections.sort(params, (p1, p2) -> p1.getName().compareTo(p2.getName()));

        for (Param p : params) {
            uid += "|" + p.toString();
        }

        return uid;
    }

    public void writeData(OutputStream out, ServerRequest sr) throws DataAccessException {
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
            } else {
                throw new DataAccessException("Data not accessible.  Check server log for errors.");
            }
        } catch (Exception e) {
            throw new DataAccessException(e);
        }
    }

    public File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException {

        Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);

        // if decimation or sorting is requested, you cannot background writing the file to speed up response time.
        boolean noBgWrite = request.getDecimateInfo() != null || request.getSortInfo() != null;

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
                filterFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
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
                sortedFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
                doSort(resultsFile, sortedFile, sortInfo, request);
                cache.put(key, sortedFile);
            }
            resultsFile = sortedFile;
        }

        // do decimation
        DecimateInfo decimateInfo = request.getDecimateInfo();
        if (decimateInfo != null) {
            key = key.appendToKey(decimateInfo);
            File deciFile = validateFile((File) cache.get(key));
            if (deciFile == null) {
                // only read in the required columns
                Expression xColExpr = new Expression(decimateInfo.getxColumnName(), null);
                Expression yColExpr = new Expression(decimateInfo.getyColumnName(), null);
                List<String> requestedCols = new ArrayList<>();
                if (xColExpr.isValid() && yColExpr.isValid()) {
                    requestedCols.addAll(xColExpr.getParsedVariables());
                    requestedCols.addAll(yColExpr.getParsedVariables());
                }
                DataGroup dg = DataGroupReader.read(resultsFile, requestedCols.toArray(new String[requestedCols.size()]));

                deciFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
                DataGroup retval = QueryUtil.doDecimation(dg, decimateInfo);
                DataGroupWriter.write(deciFile, retval, Integer.MAX_VALUE, request.getMeta());
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
                subFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
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

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {

        if (defaults != null && request instanceof TableServerRequest) {
            TableServerRequest tsreq = (TableServerRequest) request;
            if (tsreq.getMeta() != null && tsreq.getMeta().size() > 0) {
                for (String key : tsreq.getMeta().keySet()) {
                    defaults.setAttribute(key, tsreq.getMeta(key));
                }
            }
        }
    }

    public void prepareAttributes(int rows, BufferedWriter writer, ServerRequest sr) throws IOException {
    }

    public Map<String, String> getOutputColumnsMap() {
        return null;
    }

    protected void doSort(File inFile, File outFile, SortInfo sortInfo, TableServerRequest request) throws IOException {
        // do sorting...
        StopWatch timer = StopWatch.getInstance();
        timer.start("read");
        int pageSize = request.getPageSize();
        DataGroup dg = DataGroupReader.read(inFile, true, false, true);
        // if this file does not contain ROWID, add it.
        if (!dg.containsKey(DataGroup.ROWID_NAME)) {
            dg.addDataDefinition(DataGroup.ROWID);
            dg.addAttribute("col." + DataGroup.ROWID_NAME + ".Visibility", "hidden");
        }
        timer.printLog("read");
        timer.start("sort");
        QueryUtil.doSort(dg, sortInfo);
        timer.printLog("sort");
        timer.start("write");
        DataGroupWriter.write(outFile, dg, pageSize, request.getMeta());
        timer.printLog("write");
    }

    protected Cache getCache() {
        return new PrivateCache(ServerContext.getRequestOwner().getUserKey(), CacheManager.getCache(Cache.TYPE_PERM_FILE));
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


//====================================================================
//
//====================================================================

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
                if (useWorkspace) {
                    WorkspaceManager ws = ServerContext.getRequestOwner().getWsManager();
                    WspaceMeta meta = ws.newMeta(cfile, WspaceMeta.TYPE, request.getRequestId());
                    meta.setProperty(WspaceMeta.DESC, getDescResolver().getDesc(request));
                    ws.setMeta(meta);
                }
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
                        TableDef meta = IpacTableUtil.getMetaInfo(cfile);
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

                logStats(request.getRequestId(), rowCount, fileSize, isFromCache, getDescResolver().getDesc(request));
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
        StopWatch timer = StopWatch.getInstance();
        timer.start("filter");
        DataGroupFilter.filter(outFile, source, filters, request.getPageSize(), request.getMeta());
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
        File file;
        if (doCache()) {
            if (useWorkspace) {
                file = ServerContext.getRequestOwner().getWsManager().createFile(
                        getWspaceSaveDirectory(), getFilePrefix(request), fileExt);
            } else {
                file = File.createTempFile(getFilePrefix(request), fileExt, ServerContext.getPermWorkDir());
            }
        } else {
            file = File.createTempFile(getFilePrefix(request), fileExt, ServerContext.getTempWorkDir());
        }
        return file;
    }

    /**
     * this is where your results should be saved.  It's default to WspaceMeta.IMAGESET.
     * @return path to the workspace directory
     */
    protected String getWspaceSaveDirectory() {
        return "/" + WorkspaceManager.SEARCH_DIR + "/" + WspaceMeta.IMAGESET;

    }

    private void logStats(String searchType, int rows, long fileSize, boolean fromCached, Object... params) {
        String isCached = fromCached ? "cache" : "db";
        SEARCH_LOGGER.stats(searchType, "rows", rows, "fsize(MB)", (double) fileSize / StringUtils.MEG,
                "from", isCached, "params", CollectionUtil.toString(params, ","));
    }

    protected static void writeLine(BufferedWriter writer, String text) throws IOException {
        writer.write(text);
        writer.newLine();
    }

    protected static File convertToIpacTable(File tblFile, TableServerRequest request) throws IOException, DataAccessException{
        // if the file is not in IPAC table format - convert
        DataGroupReader.Format format = DataGroupReader.guessFormat(tblFile);
        boolean isFixedLength = request.getBooleanParam(TableServerRequest.FIXED_LENGTH, true);
        if (format == DataGroupReader.Format.IPACTABLE && isFixedLength) {
            // file is already in ipac table format
            return tblFile;
        } else {
            if ( format != DataGroupReader.Format.UNKNOWN) {
                // format is unknown.. convert it into ipac table format
                DataGroup dg = DataGroupReader.readAnyFormat(tblFile);
                File convertedFile; //= createFile(request, ".tbl");
                if (format == DataGroupReader.Format.IPACTABLE) {
                    convertedFile = FileUtil.createUniqueFileFromFile(tblFile);
                } else {
                    convertedFile = FileUtil.modifyFile(tblFile, "tbl");
                    if (convertedFile.exists())
                        convertedFile = FileUtil.createUniqueFileFromFile(convertedFile);
                }

                DataGroupWriter.write(convertedFile, dg, 0);
                return convertedFile;
            } else {
                throw new DataAccessException("Source file has an unknown format:" + ServerContext.replaceWithPrefix(tblFile));
            }
        }
    }

}

