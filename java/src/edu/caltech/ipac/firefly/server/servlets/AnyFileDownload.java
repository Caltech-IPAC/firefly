package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.rpc.SearchServices;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.SrvParam;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
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

        File downloadFile= VisContext.convertToFile(fname);
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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
