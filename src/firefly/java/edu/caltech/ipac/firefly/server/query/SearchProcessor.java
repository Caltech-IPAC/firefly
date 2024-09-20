/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.table.TableUtil;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static edu.caltech.ipac.firefly.data.TableServerRequest.FF_SESSION_ID;
import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: SearchProcessor.java,v 1.3 2012/06/21 18:23:53 loi Exp $
 */
public interface SearchProcessor<Type> {

    String getUniqueID(ServerRequest request);
    Type getData(ServerRequest request) throws DataAccessException;
    default FileInfo writeData(OutputStream out, ServerRequest request, TableUtil.Format format, TableUtil.Mode mode) throws DataAccessException {
        return null;
    };
    boolean doCache();
    void onComplete(ServerRequest request, Type results) throws DataAccessException;
    boolean doLogging();
    void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request);
    QueryDescResolver getDescResolver();

//====================================================================
//  default implementations
//====================================================================

    Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);

    /**
     * return the unique ID for the original data set of this request.  This means parameters related
     * to paging, filtering, sorting, decimating, etc are ignored.
     * @param request
     * @return
     */
    static String getUniqueIDDef(TableServerRequest request) {
        RequestOwner ro = ServerContext.getRequestOwner();
        SortedSet<Param> params = request.getSearchParams();
        applyIfNotEmpty(request.getParam(FF_SESSION_ID),   v -> params.add(new Param(FF_SESSION_ID, v)));
        if (ro.isAuthUser()) {
            params.add( new Param("userID", ro.getUserInfo().getLoginName()));
        }
        return StringUtils.toString(params, "|");
    }

    static void logStats(String searchType, int rows, long fileSize, boolean fromCached, Object... params) {
        String isCached = fromCached ? "cache" : "db";
        SEARCH_LOGGER.stats(searchType, "rows", rows, "fsize(MB)", (double) fileSize / StringUtils.MEG,
                "from", isCached, "params", CollectionUtil.toString(params, ","));
    }


//====================================================================
// public interfaces
//====================================================================

    interface CanFetchDataGroup {

        /**
         * Fetches the data for the given search request.  This method should perform a fetch for fresh
         * data.  Caching should not be performed here.
         *
         * @param req Table request object
         * @return
         * @throws DataAccessException
         */
        public DataGroup fetchDataGroup(TableServerRequest req) throws DataAccessException;
    }

    /**
     * Date: 9/13/17
     *
     */
    interface CanGetDataFile {
        File getDataFile(TableServerRequest request) throws IpacTableException, IOException, DataAccessException;
    }

    class SynchronizedAccess {
        private final ConcurrentHashMap<String, ReentrantLock> activeRequests = new ConcurrentHashMap<>();

        /**
         * Acquires a lock associated with the given ID. If the lock does not already exist, it is created.
         *
         * @param id the identifier for the lock
         * @return a {@code Runnable} that, when executed, releases the lock and removes it from the active requests
         */
        public Runnable lock(String id) {
            ReentrantLock lock = activeRequests.computeIfAbsent(id, k -> new ReentrantLock());
            Logger.getLogger().trace("waiting %s: %s\n".formatted(id, lock));
            lock.lock();
            Logger.getLogger().trace("got lock %s: %s\n".formatted(id, lock));
            return () -> {
                try {
                    lock.unlock();              // Ensure lock is released even if an exception occurs
                } finally {
                    if (!lock.isLocked()) activeRequests.remove(id);  // Remove the lock from activeRequests if no threads are using it
                    Logger.getLogger().trace("unlock %s: %s\n".formatted(id, lock));
                }
            };
        }
    }
}
