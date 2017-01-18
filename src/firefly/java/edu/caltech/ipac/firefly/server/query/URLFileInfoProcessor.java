/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.AnyUrlParams;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;


/**
 * Date: Mar 8, 2010
 *
 * @author loi
 * @version $Id: URLFileInfoProcessor.java,v 1.6 2012/12/10 19:02:11 roby Exp $
 */
abstract public class URLFileInfoProcessor extends BaseFileInfoProcessor {

    protected FileInfo loadDataTEMP(ServerRequest sr) throws IOException, DataAccessException {
        FileInfo retval= null;
        try {
            URL url= getURL(sr);
            if (url==null) throw new MalformedURLException("computed url is null");

            String progressKey= sr.getParam(WebPlotRequest.PROGRESS_KEY);
            String plotId= sr.getParam(WebPlotRequest.PLOT_ID);
            AnyUrlParams params = new AnyUrlParams(url,progressKey,plotId);
            if (!StringUtils.isEmpty(getFileExtension())) {
                params.setLocalFileExtensions(Arrays.asList(getFileExtension()));
            }
            if (identityAware()) {
                Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();
                if (cookies != null && cookies.size() > 0) {
                    for (String key : cookies.keySet()) {
                        params.addCookie(key, cookies.get(key));
                    }
                }
            }
            retval= LockingVisNetwork.retrieve(params);
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
        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");

        }
        return retval;
    }

    protected FileInfo loadData(ServerRequest sr) throws IOException, DataAccessException {
        URL url= getURL(sr);
        String progressKey= sr.getParam(WebPlotRequest.PROGRESS_KEY);
        String plotId= sr.getParam(WebPlotRequest.PLOT_ID);
        Map<String, String> identityCookies= null;

        if (identityAware()) {
            identityCookies = ServerContext.getRequestOwner().getIdentityCookies();
        }


        return retrieveViaURL(url,null,progressKey,plotId,getFileExtension(),identityCookies );

    }

    protected boolean identityAware() {
        return false;
    }

    public String getFileExtension()  { return ""; }

    public abstract URL getURL(ServerRequest sr) throws MalformedURLException;

    public static FileInfo retrieveViaURL(URL url, File dir) throws IOException, DataAccessException {
        return retrieveViaURL(url, dir, null, null, null, null);
    }

    public static FileInfo retrieveViaURL(URL url,
                                          File dir,
                                          String progressKey,
                                          String plotId,
                                          String fileExtension,
                                          Map<String, String> identityCookies)
                                                 throws IOException, DataAccessException {
        FileInfo retval= null;
        try {
            if (url==null) throw new MalformedURLException("computed url is null");

            AnyUrlParams params = new AnyUrlParams(url,progressKey,plotId);
            if (dir!=null) params.setFileDir(dir);
            if (!StringUtils.isEmpty(fileExtension)) {
                params.setLocalFileExtensions(Arrays.asList(fileExtension));
            }
            if (identityCookies != null && identityCookies.size() > 0) {
                for (String key : identityCookies.keySet()) {
                    params.addCookie(key, identityCookies.get(key));
                }
            }
            retval= LockingVisNetwork.retrieve(params);
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
        } catch (FailedRequestException e) {
            _logger.warn(e, "Could not retrieve URL");
        } catch (MalformedURLException e) {
            _logger.warn(e, "Could not compute URL");

        }
        return retval;

    }
}
