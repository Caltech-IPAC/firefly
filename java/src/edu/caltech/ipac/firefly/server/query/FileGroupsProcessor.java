package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
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
            StringKey key = new StringKey(FileInfoProcessor.class.getName(), getUniqueID(request));
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
            LOGGER.error(e, "Error while processing request:" + sr);
            throw new DataAccessException("Request failed due to unexpected exception: ", e);
        }

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
        /* may not make sense to cahe here.. defaults to false */
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
