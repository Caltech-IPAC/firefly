/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.AnyUrlParams;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;


/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: URLFileInfoProcessor.java,v 1.6 2012/12/10 19:02:11 roby Exp $
 */
abstract public class URLFileInfoProcessor extends BaseFileInfoProcessor {

    protected FileInfo loadData(ServerRequest sr) throws IOException, DataAccessException {
        FileInfo retval= null;
        try {
            URL url= getURL(sr);
            if (url==null) throw new MalformedURLException("computed url is null");

            AnyUrlParams params = new AnyUrlParams(url);
            if (identityAware()) {
                Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();
                if (cookies != null && cookies.size() > 0) {
                    for (String key : cookies.keySet()) {
                        params.addCookie(key, cookies.get(key));
                    }
                }
            }
            retval= LockingVisNetwork.getFitsFile(params);
            if (retval.getResponseCode()>=500) {
                File f= new File(retval.getInternalFilename());
                if (f.length()<800) {
                    String fileData= FileUtil.readFile(f);
                    _logger.warn("Could not retrieve URL, status: "+ retval.getResponseCode(),
                                 "response: "+ fileData);
                    f.delete();
                    throw new DataAccessException("Could not retrieve file: "+ fileData);
                }
                else {
                    _logger.warn("Could not retrieve URL, status: "+ retval.getResponseCode());
                    throw new DataAccessException("Could not retrieve file");
                }
            }
            _logger.info("retrieving URL:" + url.toString());
        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");

        }
        return retval;
    }

    protected boolean identityAware() {
        return false;
    }

    public abstract URL getURL(ServerRequest sr) throws MalformedURLException;
}
