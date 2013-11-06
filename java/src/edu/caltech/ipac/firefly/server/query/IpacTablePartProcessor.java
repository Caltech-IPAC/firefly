package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.DataGroupQueryStatement;
import edu.caltech.ipac.astro.InvalidStatementException;
import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.SortInfo;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupFilter;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupWriter;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.IpacTableUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataGroupQuery;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: IpacTablePartProcessor.java,v 1.33 2012/10/23 18:37:22 loi Exp $
 */
abstract public class IpacTablePartProcessor implements SearchProcessor<DataGroupPart> {

    public static final Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    public static long logCounter = 0;
    public static final List<String> SYS_PARAMS = Arrays.asList(TableServerRequest.INCL_COLUMNS, TableServerRequest.FILTERS, TableServerRequest.PAGE_SIZE,
                                                                TableServerRequest.SORT_INFO, TableServerRequest.START_IDX);

    public ServerRequest inspectRequest(ServerRequest request) {
        return request;
    }

    /**
     * Default behavior is to read file, created by getDataGroupFile
     * @param sr
     * @return
     * @throws Exception
     */
    public DataGroupPart getData(ServerRequest sr) throws DataAccessException {
        File dgFile = null;
        try {
            TableServerRequest request= (TableServerRequest)sr;
            dgFile = getDataFile(request);

            DataGroupPart page = null;
            // get the page requested
            if (dgFile == null || !dgFile.exists() || dgFile.length() == 0) {
                DataGroupPart.TableDef def = new DataGroupPart.TableDef();
                def.setStatus(DataGroupPart.State.COMPLETED);
                page = new DataGroupPart(def, new DataGroup("No result found", new DataType[0]), 0, 0);
            } else {
                try {
                    page = IpacTableParser.getData(dgFile, request.getStartIndex(), request.getPageSize());
                } catch(Exception e) {
                    LOGGER.error(e, "Fail to parse ipac table file: " + dgFile);
                    throw e;
                }
            }
            onComplete(page);

            return page;
        } catch (Exception e) {
            LOGGER.error(e, "Error while processing request: " + sr.toString());
            throw new DataAccessException("Unexpected error", e);
        } finally {
            if (!doCache()) {
                if (dgFile != null) {
                    dgFile.delete();
                }
            }
        }

    }

    public boolean doCache() {
        return true;
    }

    public boolean doLogging() {
        return true;
    }

    public void onComplete(DataGroupPart results) throws DataAccessException {}

    public String getUniqueID(ServerRequest request) {

        String uid = request.getRequestId() + "-";
        for (Param p : request.getParams()) {
            if (!SYS_PARAMS.contains(p.getName())) {
                uid += "|" + p.toString();
            }
        }
        return uid;
    }

    public void writeData(OutputStream out, ServerRequest sr) throws DataAccessException {
        try {
            TableServerRequest request= (TableServerRequest)sr;

            File inf = getDataFile(request);
            if (inf != null && inf.canRead()) {
                int rows = IpacTableParser.getMetaInfo(inf).getRowCount();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out),
                                            IpacTableUtil.FILE_IO_BUFFER_SIZE);
                BufferedReader reader = new BufferedReader(new FileReader(inf),
                                            IpacTableUtil.FILE_IO_BUFFER_SIZE);

                prepareAttributes(rows, writer, sr);
                String s = reader.readLine();
                while (s != null) {
                    if ( !(s.startsWith("\\col.") ||
                           s.startsWith("\\Loading")) ) {           // ignore ALL system-use headers
                        if (s.startsWith("|") && getOutputColumnsMap()!=null)
                            for (String key: getOutputColumnsMap().keySet()) {
                                s=s.replaceAll(key, getOutputColumnsMap().get(key));
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
        StringKey basekey = new StringKey(IpacTablePartProcessor.class.getName(), getUniqueID(request));
        StringKey filterkey = new StringKey(basekey);
        StringKey key = new StringKey(basekey);
//        List<CollectionUtil.Filter<DataObject>> rowIdFilters = null;

        DataGroupQuery.DataFilter[] filters = QueryUtil.convertToDataFilter(request.getFilters());
        if (filters != null && filters.length > 0) {
            filterkey.appendToKey((Object[])filters);
            key.appendToKey((Object[])filters);
        }


        SortInfo sortInfo = request.getSortInfo();
        if ( sortInfo != null) {
            key.appendToKey(sortInfo);
        }

        Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);
        File dgFile = (File) cache.get(key);

        if (dgFile != null) {
            if (!dgFile.canRead()) {
                LOGGER.warn("File returned from cache, but is not accessible:" + dgFile.getAbsolutePath());
                dgFile = null;
            }
        }

        if (dgFile == null) {

            // do filtering
            if (filters != null && filters.length > 0) {
                dgFile = (File) cache.get(filterkey);
                if (dgFile == null) {
                    // not in cache... go get data
                    dgFile = getBaseDataFile(request);
                    if (dgFile == null) {
                        return null;
                    }
                    File source = dgFile;
                    dgFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
//                    if (request.getSortInfo() != null) {
//                        // if you need sorting afterward, then you have to apply the RowIdFilter after sorting
//                        ArrayList<CollectionUtil.Filter<DataObject>> filtersList = new ArrayList<CollectionUtil.Filter<DataObject>>();
//                        for (DataGroupQuery.DataFilter dt :filters) filtersList.add(dt);
//                        rowIdFilters =  CollectionUtil.splitUp(filtersList);
//                        if (rowIdFilters.size() > 0) {
//                            filters = new DataGroupQuery.DataFilter[filtersList.size()];
//                            for(int i = 0; i < filtersList.size(); i++) {
//                                filters[i] = (DataGroupQuery.DataFilter) filtersList.get(i);
//                            }
//                        }
//                    }

                    doFilter(dgFile, source, filters, request);
                    if (doCache()) {
                        cache.put(filterkey, dgFile);
                    }
                }
            }

            if (dgFile == null) {
                // go get data
                dgFile = getBaseDataFile(request);
            }

            if ( dgFile != null && sortInfo != null) {
                // do sorting...
                File inf = dgFile;
                dgFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
                doSort(inf, dgFile, sortInfo, request.getPageSize());
                if (doCache()) {
                    cache.put(key, dgFile);
                }
            }
        }

//        if (rowIdFilters != null && rowIdFilters.size() > 0) {
//            File source = dgFile;
//            dgFile = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
//            doFilter(dgFile, source, rowIdFilters.toArray(new CollectionUtil.Filter[rowIdFilters.size()]), request);
//        }
//
        // return only the columns requested
        String ic = request.getParam(TableServerRequest.INCL_COLUMNS);
        if (dgFile != null && !StringUtils.isEmpty(ic)) {

            if (!StringUtils.isEmpty(ic) && !ic.equals("ALL")) {
                File newf = File.createTempFile(getFilePrefix(request), ".tbl", ServerContext.getTempWorkDir());
                String sql = "select col " + ic + " from " + dgFile.getAbsolutePath() + " into " + newf.getAbsolutePath() + " with complete_header";
                try {
                    DataGroupQueryStatement.parseStatement(sql).execute();
                } catch (InvalidStatementException e) {
                    throw new DataAccessException("InvalidStatementException", e);
                }
                dgFile = newf;
            }
        }

        return dgFile;
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
    }

    public void prepareAttributes(int rows, BufferedWriter writer, ServerRequest sr) throws IOException{
    }

    public Map<String, String> getOutputColumnsMap() { return null; }

    protected void doSort(File inFile, File outFile, SortInfo sortInfo, int pageSize) throws IOException {
        // do sorting...
        StopWatch timer = StopWatch.getInstance();
        timer.start("read");
        DataGroup dg = DataGroupReader.read(inFile);
        // if this file does not contain ROWID, add it.
        if (!dg.containsKey(DataGroup.ROWID_NAME)) {
            dg.addDataDefinition(DataGroup.ROWID);
            dg.addAttributes(new DataGroup.Attribute("col." + DataGroup.ROWID_NAME + ".Visibility", "hidden"));
        }
        timer.printLog("read");
        timer.start("sort");
        QueryUtil.doSort(dg, sortInfo);
        timer.printLog("sort");
        timer.start("write");
        DataGroupWriter.write(outFile, dg, pageSize);
        timer.printLog("write");
    }


    protected Cache getCache() {
        return CacheManager.getCache(Cache.TYPE_PERM_FILE);
    }

    /**
     * subclass provide how the data are collected
     * @param request
     * @return
     * @throws java.io.IOException
     * @throws edu.caltech.ipac.firefly.server.query.DataAccessException
     */
    abstract protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException;


//====================================================================
//
//====================================================================

    /**
     *  return the file containing data before filter and sort.
     * @param request
     * @return
     * @throws IOException
     * @throws DataAccessException
     */
    private File getBaseDataFile(TableServerRequest request) throws IOException, DataAccessException {

        StringKey key = new StringKey(IpacTablePartProcessor.class.getName() + ":base-dataset", getUniqueID(request));

        Cache cache = getCache();
        File cfile = (File) cache.get(key);

        if (cfile != null) {
            if (!cfile.canRead()) {
                LOGGER.warn("File returned from cache, but is not accessible:" + cfile.getAbsolutePath());
                cfile = null;
            }
        }


        boolean isFromCache = true;
        if (cfile == null) {
            cfile = loadDataFile(request);

            if (doCache()) {
                cache.put(key, cfile);
            }
            isFromCache = false;
        }

        if (isInitLoad(request)) {
            // maintain counters for applicaiton monitoring
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
                        DataGroupPart.TableDef meta = IpacTableParser.getMetaInfo(cfile);
                        if (meta.getStatus() == DataGroupPart.State.INPROGRESS) {
                            fileSize = (meta.getRowCount() * meta.getLineWidth()) + meta.getRowStartOffset();
                        } else {
                            fileSize = cfile.length();
                        }
                        rowCount = meta.getRowCount();
                    } catch(IOException iox) {
                        throw new IOException("File:" + cfile, iox);
                    }
                }

                logStats(request.getRequestId(), rowCount, fileSize, isFromCache, request.getParams().toArray());
            }
        }

        return cfile;
    }

    private boolean isInitLoad(TableServerRequest req) {
        List<String> filters = req.getFilters();
        return req.getPageSize() > 0 && req.getStartIndex() == 0 &&
                (req.getFilters() == null || filters.size() == 0) && req.getSortInfo() == null;
    }

    protected void doFilter(File outFile, File source, CollectionUtil.Filter<DataObject>[] filters, TableServerRequest request) throws IOException {
        StopWatch timer = StopWatch.getInstance();
        // if you need to sort the file, you CANNOT background it.  must complete filtering, before sorting.
        int fetchSize = request.getSortInfo() == null ? request.getPageSize() : Integer.MAX_VALUE;
        timer.start("filter");
        DataGroupFilter.filter(outFile, source, filters, fetchSize);
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
        File workDir = doCache() ? ServerContext.getPermWorkDir() : ServerContext.getTempWorkDir();
        return File.createTempFile(getFilePrefix(request), fileExt, workDir);
    }

    private void logStats(String searchType, int rows, long fileSize, boolean fromCached, Object... params) {
        String isCached = fromCached ? "cache" : "db";
        SEARCH_LOGGER.stats(searchType, "rows", rows, "fsize(MB)", (double)fileSize/StringUtils.MEG,
                "from", isCached, "params", CollectionUtil.toString(params,","));
    }

    protected static void writeLine (BufferedWriter writer, String text) throws IOException {
        writer.write(text);
        writer.newLine();
    }

}

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
