/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: FileGroupsProcessor.java,v 1.8 2012/06/21 18:23:53 loi Exp $
 */
abstract public class FileGroupsProcessor implements SearchProcessor<List<FileGroup>> {

    public static final Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);
    public static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    public static long logCounter = 0;

    public List<FileGroup> getData(ServerRequest sr) throws DataAccessException {
        try {
            DownloadRequest request= (DownloadRequest)sr;
            List<FileGroup> fileGroups = null;
            StringKey key = new StringKey(FileGroupsProcessor.class.getName(), getUniqueID(request));
            Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);
            fileGroups = (List<FileGroup>) cache.get(key);

            if (fileGroups == null || isStaled(fileGroups)) {
                fileGroups = loadData(request);
                if (doCache()) {
                    cache.put(key, fileGroups);
                }
            }
            onComplete(request, fileGroups);
            return fileGroups;
        } catch (Exception e) {
            LOGGER.error(e, "Error while processing request:" + StringUtils.truncate(sr, 512));
            throw new DataAccessException("Request failed due to unexpected exception: ", e);
        }

    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
    }

    private boolean isStaled(List<FileGroup> fileGroups) {
        if (fileGroups == null) return true;

        for(FileGroup fg : fileGroups) {
            for(int i = 0; i < fg.getSize(); i++) {
                FileInfo f = fg.getFileInfo(i);
                String fname = f.getInternalFilename();
                if ( !StringUtils.isEmpty(fname) && !isUrl(fname) && !new File(fname).canRead() ) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    public ServerRequest inspectRequest(ServerRequest request) {
        return request;
    }

    public String getUniqueID(ServerRequest request) {
        return request.getRequestId() + "-" + StringUtils.toString(request.getParams());
    }

    public void writeData(OutputStream out, ServerRequest request) throws DataAccessException {
        /* instead of returning the results as an object, write it into a file..
            may not make sense here.  do nothing for now. */
    }

    public boolean doCache() {
        /* may not make sense to cache here.. defaults to false */
        return false;
    }

    public void onComplete(ServerRequest request, List<FileGroup> results) throws DataAccessException {
    }

    public boolean doLogging() {
        return false;
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        /* this only applies to table-based results... do nothing here */
    }

    abstract public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException;




//====================================================================
//
//====================================================================
    /**
    static String colDesc = "remote-ip   search-type   rows-returned   file-size(KB) cache/db  response-time(sec) params";
    static String fmt = "%-17s  %-15s  %7d  %10.1f  %10s  %10.3f  -- params:(%s)";
    private void logStats(String searchType, int rows, long fileSize, boolean fromCached, Object... params) {

        if (logCounter%300 == 0) {
            SEARCH_LOGGER.stats("SEARCH-LOG-COLUMNS-DESC: " + colDesc);
        }
        logCounter++;

        RequestOwner ro = ServerContext.getRequestOwner();
        String isCached = fromCached ? "from-cache" : "from-db";
        String msg = String.format(fmt, searchType.toString(), ro.getRemoteIP(),
                    rows, fileSize/1024.0, isCached,
                    (System.currentTimeMillis() - ro.getStartTime().getTime())/1000.0,
                    CollectionUtil.toString(params,",")
                );

        SEARCH_LOGGER.stats(msg);
    }
    **/

}

