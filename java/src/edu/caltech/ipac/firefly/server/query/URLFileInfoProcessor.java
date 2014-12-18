package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.util.DataType;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: URLFileInfoProcessor.java,v 1.6 2012/12/10 19:02:11 roby Exp $
 */
abstract public class URLFileInfoProcessor implements SearchProcessor<FileInfo> {
    public static final Logger.LoggerImpl _logger = Logger.getLogger();


    public FileInfo getData(ServerRequest sr) throws DataAccessException {
        FileInfo retval= null;
        try {
            URL url= getURL(sr);
            if (url==null) throw new MalformedURLException("computed url is null");
            retval= LockingVisNetwork.getFitsFile(url);
            _logger.info("retrieving URL:" + url.toString());
        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");

        }
        return retval;
    }

    public QueryDescResolver getDescResolver() {
        return new QueryDescResolver.DescBySearchResolver(new SearchDescResolver());
    }

    public ServerRequest inspectRequest(ServerRequest request) {
        return request;
    }

    public abstract URL getURL(ServerRequest sr) throws MalformedURLException;

    // ---------- following now used -------------------------
    public String getUniqueID(ServerRequest request) { return null; }
    public void writeData(OutputStream out, ServerRequest request) throws DataAccessException { }
    public boolean doCache() { return false;/* does not apply.. do nothing */ }
    public void onComplete(ServerRequest request, FileInfo results) throws DataAccessException { }
    public boolean doLogging() { return false; }
    public void prepareTableMeta(TableMeta defaults, List<DataType> columns, ServerRequest request) { }


}
