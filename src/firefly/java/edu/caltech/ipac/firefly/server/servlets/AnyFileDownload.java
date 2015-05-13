/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.SrvParam;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: AnyFileDownload.java,v 1.12 2012/12/19 22:36:08 roby Exp $
 */
public class AnyFileDownload extends BaseHttpServlet {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.DOWNLOAD_LOGGER);


    public static final String FILE_PARAM  = "file";
    public static final String RETURN_PARAM= "return";
    public static final String LOG_PARAM   = "log";
    public static final String TRACK_PARAM = "track";
    public static final String USE_SERVER_NAME = "USE_SERVER_NAME";


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        SrvParam sp= new SrvParam(req.getParameterMap());
        String fname= sp.getRequired(FILE_PARAM);
        String local= sp.getOptional(RETURN_PARAM);
        boolean log= sp.getOptionalBoolean(LOG_PARAM,false);
        boolean track= sp.getOptionalBoolean(TRACK_PARAM,false);

        File downloadFile= ServerContext.convertToFile(fname);
        if (downloadFile==null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND,"Could not convert input to a file" );
            _log.warn("Cannot convert file: "+ fname + " to valid path, returning 404");
        }
        else if (!downloadFile.canRead()) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND,"File was not found" );
            _log.warn("Cannot read file: "+ downloadFile.getPath(),
                      "fname: "+fname, " returning 404");
        }
        else {
            res.addHeader("Cache-Control", "max-age=86400");
            String retFile= (local!=null) ? local : downloadFile.getName();
            if (retFile.equals(USE_SERVER_NAME)) retFile= downloadFile.getName();
            insertResponseHeaders(res, downloadFile,retFile);
            if (track) trackProgress(req, SearchServices.DownloadProgress.WORKING);
            try {
                FileUtil.writeFileToStream(downloadFile, res.getOutputStream());
                if (track) trackProgress(req, SearchServices.DownloadProgress.DONE);
                if (log) logActivity(downloadFile);
            } catch (IOException e) {
                if (track) trackProgress(req, SearchServices.DownloadProgress.FAIL);
                throw e;
            }
        }
    }


    public void insertResponseHeaders(HttpServletResponse res, File f, String retFileStr) {

        String mType= getServletContext().getMimeType(f.getName());
        if (mType!=null) res.setContentType(mType);

        long fileLength= f.length();
        if(fileLength <= Integer.MAX_VALUE) res.setContentLength((int)fileLength);
        else                                res.addHeader("Content-Length", fileLength+"");

        if (!StringUtils.isEmpty(retFileStr)) {
            res.addHeader("Content-Disposition", "attachment; filename="+retFileStr);
        }
    }

    private static void logActivity(File f) {
        String logStr= "File download -- File: " + f.getPath()+
                ", size: " +  FileUtil.getSizeAsString(f.length()) +
                ", bytes: " + f.length();
        _log.briefInfo(logStr);
        _statsLog.stats("file", "size(MB)", (double)f.length()/StringUtils.MEG,
                                         "u", FileUtil.getSizeAsString(f.length()), "file", f.getPath());
    }

    private static void trackProgress(HttpServletRequest req, SearchServices.DownloadProgress progress) {
        StringKey statusKey= new StringKey(req.getQueryString());
        getCache().put(statusKey, progress);
    }

    public static Cache getCache() { return UserCache.getInstance(); }
}
