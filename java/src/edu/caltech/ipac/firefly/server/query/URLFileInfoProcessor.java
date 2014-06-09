package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.client.net.FailedRequestException;
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
