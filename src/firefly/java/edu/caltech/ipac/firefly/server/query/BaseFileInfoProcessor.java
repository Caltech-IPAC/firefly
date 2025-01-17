/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.table.TableMeta;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import java.io.IOException;
import java.util.List;

import static edu.caltech.ipac.util.cache.Cache.fileInfoCheck;


/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: URLFileInfoProcessor.java,v 1.6 2012/12/10 19:02:11 roby Exp $
 */
public abstract class BaseFileInfoProcessor implements SearchProcessor<FileInfo> {
    public static final Logger.LoggerImpl _logger = Logger.getLogger();


    public FileInfo getData(ServerRequest request) throws DataAccessException {
        try {
            FileInfo fi = null;
            Cache<FileInfo> cache = getCache(request);
            StringKey key = new StringKey(getClass().getName(), getUniqueID(request));
            if (doCache()) {
                fi = cache != null ? cache.get(key) : null;
            }
            if (fi == null) {
                fi = loadData(request);
                if (fi!=null && doCache() && cache!=null) {
                    cache.put(key,fi);
                }
            }
            onComplete(request, fi);
            return fi;
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new DataAccessException("Request failed due to unexpected exception: ", e);
        }
    }

    public Cache<FileInfo> getCache(ServerRequest request) {
        return CacheManager.<FileInfo>getLocal().validateOnGet(fileInfoCheck);
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
    }

    public String getUniqueID(ServerRequest request) {
        String id = ServerContext.getRequestOwner().isAuthUser() ? ServerContext.getRequestOwner().getUserInfo().getLoginName() : "";
        return request.getRequestId() + "-" + id + "-" + StringUtils.toString(request.getParams());
    }

    public boolean doCache() {
        return false;
    }

    public void onComplete(ServerRequest request, FileInfo results) throws DataAccessException {
    }

    public boolean doLogging() {
        return false;
    }

    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) {
        /* this only applies to table-based results... do nothing here */
    }

    abstract protected FileInfo loadData(ServerRequest sr) throws IOException, DataAccessException;



}
