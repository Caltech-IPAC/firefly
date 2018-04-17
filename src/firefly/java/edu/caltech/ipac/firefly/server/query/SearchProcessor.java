/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.StringUtils;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/**
 * Date: Jun 5, 2009
 *
 * @author loi
 * @version $Id: SearchProcessor.java,v 1.3 2012/06/21 18:23:53 loi Exp $
 */
public interface SearchProcessor<Type> {

    ServerRequest inspectRequest(ServerRequest request);
    String getUniqueID(ServerRequest request);
    Type getData(ServerRequest request) throws DataAccessException;
    FileInfo writeData(OutputStream out, ServerRequest request) throws DataAccessException;
    boolean doCache();
    void onComplete(ServerRequest request, Type results) throws DataAccessException;
    boolean doLogging();
    void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request);
    QueryDescResolver getDescResolver();

//====================================================================
//  default implementations
//====================================================================

    Logger.LoggerImpl SEARCH_LOGGER = Logger.getLogger(Logger.SEARCH_LOGGER);

    static ServerRequest inspectRequestDef(ServerRequest request) {
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
     * return the unique ID for the original data set of this request.  This means parameters related
     * to paging, filtering, sorting, decimating, etc are ignored.
     * @param request
     * @return
     */
    static String getUniqueIDDef(TableServerRequest request) {
        SortedSet<Param> params = request.getSearchParams();
        params.add(new Param("sessID", ServerContext.getRequestOwner().getRequestAgent().getSessId()));
        return StringUtils.toString(params, "|");
    }

    static void prepareTableMetaDef(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        if (defaults != null && request instanceof TableServerRequest) {
            TableServerRequest tsreq = (TableServerRequest) request;
            if (tsreq.getMeta() != null && tsreq.getMeta().size() > 0) {
                for (String key : tsreq.getMeta().keySet()) {
                    defaults.setAttribute(key, tsreq.getMeta(key));
                }
            }
        }
    }

    static void logStats(String searchType, int rows, long fileSize, boolean fromCached, Object... params) {
        String isCached = fromCached ? "cache" : "db";
        SEARCH_LOGGER.stats(searchType, "rows", rows, "fsize(MB)", (double) fileSize / StringUtils.MEG,
                "from", isCached, "params", CollectionUtil.toString(params, ","));
    }

}
