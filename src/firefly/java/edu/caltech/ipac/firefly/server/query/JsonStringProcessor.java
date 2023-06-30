/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

import static edu.caltech.ipac.firefly.data.TableServerRequest.FF_SESSION_ID;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


/**
 * Base class for a SearchProcessor that returns a JSON string.
 * It provides default implementation for SearchProcessor interface
 * as well as these features:
 * - duplicate requests lock/wait mechanism
 * - caching
 * - logging
 */
abstract public class JsonStringProcessor implements SearchProcessor<String> {
    private static final Map<String, ReentrantLock> activeRequests = new HashMap<>();
    private static final ReentrantLock lockChecker = new ReentrantLock();
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    /**
     * Fetches the data for the given request.  This method should perform a fetch for fresh
     * data.  Caching should not be performed here.
     * @param req
     * @return
     * @throws DataAccessException
     */
    abstract public String fetchData(ServerRequest req) throws DataAccessException;

    public String getData(ServerRequest request) throws DataAccessException {

        if (!doCache()) {
            String results = fetchData(request);
            doLogging(request, results);
            return results;
        }

        // when caching is supported, use lock/wait to avoid multiple duplicated requests

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
            String results = getCachedData(request);
            if (results == null) {
                results = fetchData(request);
                cacheData(request, results);
            }
            doLogging(request, results);
            return results;
        } finally {
            activeRequests.remove(unigueReqID);
            lock.unlock();
        }
    }

    private void doLogging(ServerRequest request, String results) {
        if (doLogging()) {
            SearchProcessor.logStats(request.getRequestId(), 0, results.length(), false, getDesc(request));
        }
    }

//====================================================================
//  Simple cache implementation
//====================================================================

    protected void cacheData(ServerRequest request, String results) {
        if (!isEmpty(results)) {
            File jsonFile = null;
            try {
                jsonFile = File.createTempFile("tmp-", ".json", QueryUtil.getTempDir());
                FileUtil.writeStringToFile(jsonFile, results);
                CacheManager.getCache(Cache.TYPE_TEMP_FILE)
                        .put(new StringKey(getUniqueID(request)), jsonFile);
            } catch (IOException e) {
                LOGGER.error("Cannot create temp file: " + e.getMessage());
            }
        }
    }

    protected String getCachedData(ServerRequest request) {
        Cache cache = CacheManager.getCache(Cache.TYPE_TEMP_FILE);
        File jsonFile = (File)cache.get(new StringKey(getUniqueID(request)));
        if (jsonFile != null) {
            try {
                return FileUtil.readFile(jsonFile);
            } catch (IOException e) {
                LOGGER.error("Cannot read file: " + jsonFile.getPath());
            }
        }
        return null;
    }

//====================================================================
//  Default implementation of SearchProcessor
//====================================================================

    public String getUniqueID(ServerRequest request) {
        RequestOwner ro = ServerContext.getRequestOwner();
        TreeSet<Param> params = new TreeSet<>(request.getParams());
        applyIfNotEmpty(request.getParam(FF_SESSION_ID),   v -> params.add(new Param(FF_SESSION_ID, v)));
        if (ro.isAuthUser()) {
            params.add( new Param("userID", ro.getUserInfo().getLoginName()));
        }
        return StringUtils.toString(params, "|");
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver() {
            public String getTitle(ServerRequest req) { return "not used"; }
            public String getDesc(ServerRequest req) { return getUniqueID(req);}
        };
    }

    public String getDesc(ServerRequest req) {
        return getDescResolver().getDesc(req);
    }

    public boolean doCache() { return false; }

    public void onComplete(ServerRequest request, String results) throws DataAccessException {}

    public boolean doLogging() { return false; }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {}

}

