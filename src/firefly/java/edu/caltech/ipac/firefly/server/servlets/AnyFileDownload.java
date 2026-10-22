/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.LockingRetrieve;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.FormatUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.util.download.UriRef;
import edu.caltech.ipac.util.download.UriRefParams;
import edu.caltech.ipac.visualize.net.URLParms;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpMethod;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: AnyFileDownload.java,v 1.12 2012/12/19 22:36:08 roby Exp $
 */
public class AnyFileDownload extends BaseHttpServlet {

    private static final Logger.LoggerImpl _log= Logger.getLogger();
    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.DOWNLOAD_LOGGER);


    public static final String EXT_URL  = "externalURL"; // required for downloading something through the firefly server
    public static final String STREAM_URL = "streamURL"; // relay a remote file to the browser as it arrives, keeping no copy on the server
    public static final String HIPS_PARAM  = "hipsUrl"; // required for HiPS
    public static final String ALWAYS_USE_CACHED = "alwaysUseCached"; // true if use the cached without checking
    public static final String FILE_PARAM  = "file"; // required for other request
    public static final String RETURN_PARAM= "return"; // a name for the file, if empty or USE_SERVER_NAME the use file name on server
    public static final String LOG_PARAM   = "log"; // if true, log status
    public static final String DOWNLOAD_TOKEN = "ffDownloadToken"; // parameter sent by the client to be notified when the download starts


    public static final String USE_SERVER_NAME = "USE_SERVER_NAME";
    private static final String HIPS_CACHE_CONTROL= "must-revalidate, max-age=1800";
    private static final String SHORT_CACHE_CONTROL = "must-revalidate, max-age=300";
    private static final String STATIC_CACHE_CONTROL= "max-age=86400";

    private static final String BASE_URL = "servlet/Download?"+ LOG_PARAM +"=true&" + FILE_PARAM +"=";
    private static final String RET_FILE = "&"+RETURN_PARAM+"=";
    private static final String DOWNLOAD_COOKIE_PREFIX = "ffdl_";
    static final String STREAM_READ_TIMEOUT = "download.stream.read.timeout.secs"; // seconds of silence from the remote server before a streamURL download fails

    public static String getDownloadURL(File file, String suggestedFileName) {
        return getDownloadURL(file, suggestedFileName, ServerContext.getRequestOwner().getBaseUrl());
    }

    public static String getDownloadURL(File file, String suggestedFileName, String baseUrl) {
        if (file==null) return null;

        String fname= URLParms.encode(ServerContext.replaceWithPrefix(file));
        if (fname!= null) {
            String retFile = isEmpty(suggestedFileName) ? "" : RET_FILE + URLParms.encode(suggestedFileName);
            return baseUrl + BASE_URL + fname + retFile;
        } else {
            return null;
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        if (req.getParameterMap().containsKey(HIPS_PARAM)) {
            handleHiPSRequest(req,res,true);
        }
        else if (req.getParameterMap().containsKey(EXT_URL)) {
            handleExternalURLRequest(req,res,true);
        }
        else if (req.getParameterMap().containsKey(STREAM_URL)) {
            handleStreamURLRequest(req,res,true);
        }
        else {
            handleInternalFileRequest(req,res,true);
        }

    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (req.getParameterMap().containsKey(HIPS_PARAM)) {
            handleHiPSRequest(req,res,false);
        }
        else if (req.getParameterMap().containsKey(EXT_URL)) {
            handleExternalURLRequest(req,res,false);
        }
        else if (req.getParameterMap().containsKey(STREAM_URL)) {
            handleStreamURLRequest(req,res,false);
        }
        else {
            handleInternalFileRequest(req,res,false);
        }
    }

    private void handleInternalFileRequest(HttpServletRequest req, HttpServletResponse res, boolean fullBody)
            throws IOException {
        SrvParam sp= new SrvParam(req.getParameterMap());
        String fname= sp.getRequired(FILE_PARAM);
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
            res.addHeader("Cache-Control", STATIC_CACHE_CONTROL);
            if (fullBody) {
                String local= sp.getOptional(RETURN_PARAM);
                sendFileToClient(req,res,downloadFile, local);
            }
            else {
                res.setStatus(200);
            }
        }

    }

    private void handleHiPSRequest(HttpServletRequest req, HttpServletResponse res, boolean fullBody)
            throws IOException {
        SrvParam sp= new SrvParam(req.getParameterMap());
        String hips= sp.getRequired(HIPS_PARAM);
        boolean alwaysUseCached= sp.getOptionalBoolean(ALWAYS_USE_CACHED,false);
        FileInfo fi= HiPSRetrieve.retrieveHiPSData(hips, alwaysUseCached);

        if (fi.getFile()==null) {
            if (fi.getResponseCode()==204) {
                res.addHeader("Cache-Control", SHORT_CACHE_CONTROL);
                res.addDateHeader("Last-Modified", System.currentTimeMillis());
            }
            res.sendError(fi.getResponseCode(), fi.getResponseCodeMsg());
            return;
        }

        res.addHeader("Cache-Control", HIPS_CACHE_CONTROL);
        res.addDateHeader("Last-Modified", System.currentTimeMillis());
        if (fi.getResponseCode()==304 && req.getDateHeader("If-Modified-Since")> fi.getFile().lastModified()) {
            res.setStatus(304);
            return;
        }
        if (fullBody) {
            sendFileToClient(req,res,fi.getFile(),null);
        }
        else {
            res.setStatus(200);
        }
    }

    private void handleExternalURLRequest(HttpServletRequest req, HttpServletResponse res, boolean fullBody)
            throws IOException {
        SrvParam sp= new SrvParam(req.getParameterMap());
        String urlStr= sp.getRequired(EXT_URL);
        URL url= new URL(urlStr);
        FileInfo fi;
        try {
            UriRefParams params= new UriRefParams(UriRef.make(url), ServerContext.getUploadDir());
            params.setExpectStaticFile(true);
            fi = LockingRetrieve.downloadWithCacheMsg(url, ServerContext.getUploadDir());
        } catch (FailedRequestException e) {
            res.sendError(404, e.toString());
            return;
        }

        if (fi.getFile()==null) {
            if (fi.getResponseCode()==204) {
                res.addHeader("Cache-Control", SHORT_CACHE_CONTROL);
                res.addDateHeader("Last-Modified", System.currentTimeMillis());
            }
            res.sendError(fi.getResponseCode(), fi.getResponseCodeMsg());
            return;
        }

        res.addHeader("Cache-Control", HIPS_CACHE_CONTROL);
        res.addDateHeader("Last-Modified", System.currentTimeMillis());
        if (fi.getResponseCode()==304 && req.getDateHeader("If-Modified-Since")> fi.getFile().lastModified()) {
            res.setStatus(304);
            return;
        }

        String fileName= "download-"+System.currentTimeMillis()+".tmp";
        if (fi.getExternalName()!=null && fi.getExternalName().length()>2) {
            if (url.getFile().length()>2) fileName= new File(url.getFile()).getName();
            String ext = FileUtil.getExtension(fileName);
            if (ext.length() > 4 || ext.length() < 3 || fileName.indexOf('&') > -1 || fileName.indexOf('=') > -1) {
                String ftest = fi.getExternalName();
                if (ftest.startsWith("\"")) ftest = ftest.substring(1);
                if (ftest.endsWith("\"")) ftest = ftest.substring(0, ftest.length() - 1);
                fileName = ftest;
            }
        }

        if (fullBody) {
            String local= sp.getOptional(RETURN_PARAM);
            String retFileStr= (local!=null) ? local : fileName;
            sendFileToClient(req,res,fi.getFile(),retFileStr);
        }
        else {
            res.setStatus(200);
        }
    }


    /**
     * The Firefly server fetches the remote URL and passes its bytes to the browser as they arrive,
     * so a large file starts saving right away instead of after the server has a full copy.
     * HttpServiceInput adds the user's login token for the hosts the SsoAdapter allows, also after a redirect.
     * The browser's cookies are not forwarded.  Nothing is cached or kept on the server, and the bytes are not decoded.
     */
    private void handleStreamURLRequest(HttpServletRequest req, HttpServletResponse res, boolean fullBody)
            throws IOException {
        SrvParam sp = new SrvParam(req.getParameterMap());
        String urlStr = sp.getRequired(STREAM_URL);
        if (!urlStr.matches("(?i)https?://.+")) {
            sendTextError(res, HttpServletResponse.SC_BAD_REQUEST, "Only http and https URLs can be downloaded: " + urlStr);
            return;
        }

        StreamRelay relay = new StreamRelay(res, sp.getOptional(RETURN_PARAM), sp.getOptional(DOWNLOAD_TOKEN), fullBody);
        // Without a read timeout, a remote server that goes silent holds this Firefly server thread forever, even after
        // the browser cancels, because the relay sees a cancel only when it writes.  A job's result is already computed,
        // so 5 minutes of silence means the remote server has stalled.
        int readTimeout = AppProperties.getIntProperty(STREAM_READ_TIMEOUT, 300) * 1000;
        HttpServices.Status status = HttpServices.getData(new HttpServiceInput(urlStr).setReadTimeout(readTimeout), relay);

        if (relay.readError != null) {
            // The remote server failed after it started sending.  Before the response is committed, the browser
            // can still get an error page.  After that, the exception makes Tomcat drop the connection,
            // so the browser marks the download failed instead of saving a truncated file as complete.
            if (!res.isCommitted()) {
                res.reset();
                sendTextError(res, HttpServletResponse.SC_BAD_GATEWAY, "Download failed while reading " + urlStr + ": " + relay.readError.getMessage());
                return;
            }
            throw new IOException("Download failed while reading " + urlStr, relay.readError);
        }
        if (relay.writeError != null) {
            _log.info("Stream download stopped by the browser after " + relay.bytes + " bytes: " + urlStr);
        } else if (relay.started) {
            _log.info("Stream download -- URL: " + urlStr + ", bytes: " + relay.bytes);
        } else {
            // the remote server never returned the file; the client's download() shows this text in a popup
            String reason = status.getStatusCode() + (isEmpty(status.getErrMsg()) ? "" : " " + status.getErrMsg());
            sendTextError(res, HttpServletResponse.SC_BAD_GATEWAY, "Download failed for " + urlStr + ": " + reason);
        }
    }

    private static void sendTextError(HttpServletResponse res, int status, String msg) throws IOException {
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        res.setStatus(status);
        res.setContentType("text/plain;charset=utf-8");
        res.setContentLength(body.length);
        res.getOutputStream().write(body);
    }

    /**
     * Copies the remote server's response to the browser.  HttpServices calls it only for a 2xx response,
     * after following any redirects.
     */
    static class StreamRelay implements HttpServices.Handler {
        private final HttpServletResponse res;
        private final String returnName;
        private final String downloadToken;
        private final boolean fullBody;
        boolean started;
        long bytes;
        IOException readError;      // the remote server failed
        IOException writeError;     // the browser went away

        StreamRelay(HttpServletResponse res, String returnName, String downloadToken, boolean fullBody) {
            this.res = res;
            this.returnName = returnName;
            this.downloadToken = downloadToken;
            this.fullBody = fullBody;
        }

        public HttpServices.Status handleResponse(HttpMethod method) {
            String type = HttpServices.getResHeader(method, "Content-Type", null);
            String length = HttpServices.getResHeader(method, "Content-Length", null);
            String encoding = HttpServices.getResHeader(method, "Content-Encoding", null);
            String disposition = HttpServices.getResHeader(method, "Content-Disposition", null);

            res.setStatus(HttpServletResponse.SC_OK);
            if (type != null) res.setContentType(type);
            if (length != null) res.setHeader("Content-Length", length);
            if (encoding != null) res.setHeader("Content-Encoding", encoding);
            res.setHeader("Content-Disposition", URLDownload.makeContentDisposition(fileName(method, type, disposition)));
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Cache-Control", "private, no-store");
            sendDownloadStartedCookie(downloadToken, res);
            started = true;

            if (!fullBody) {
                method.abort();
                return HttpServices.Status.ok();
            }

            // The stream is not closed here: when the copy stops early, closing it would make HttpClient read the
            // rest of the remote file.  abort() drops the connection instead, and HttpServices releases it.
            byte[] buffer = new byte[HttpServices.BUFFER_SIZE];
            try {
                InputStream in = method.getResponseBodyAsStream();      // raw bytes; HttpServices.getResponseBodyAsStream would gunzip
                if (in == null) throw new IOException("the remote server sent no response body");   // HttpClient gives null when a chunked body never starts
                OutputStream out = res.getOutputStream();
                while (true) {
                    int n;
                    try {
                        n = in.read(buffer);
                    } catch (IOException e) {
                        readError = e;
                        break;
                    }
                    if (n == -1) break;
                    try {
                        out.write(buffer, 0, n);
                    } catch (IOException e) {
                        writeError = e;
                        break;
                    }
                    bytes += n;
                }
            } catch (IOException e) {
                if (readError == null && writeError == null) readError = e;     // getResponseBodyAsStream() or getOutputStream() failed
            }
            // HttpClient 3 ends the stream quietly when the remote server closes before its Content-Length,
            // so the relay compares the bytes it sent with the Content-Length it passed to the browser
            long expected = StringUtils.getLong(length, -1);
            if (readError == null && writeError == null && bytes < expected) {
                readError = new IOException("the remote server closed the connection after " + bytes + " of " + expected + " bytes");
            }
            if (readError != null || writeError != null) method.abort();
            return HttpServices.Status.ok();
        }

        /**
         * The remote server's file name if it gives one.  Otherwise the client's return name, or the file name
         * in the final URL, with an extension from the content type or from that URL.
         */
        private String fileName(HttpMethod method, String type, String disposition) {
            String name = isEmpty(disposition) ? null : URLDownload.getSuggestedFileName(disposition);
            if (!isEmpty(name)) return name;

            String urlName = "";
            try {
                urlName = URLDownload.sanitizeFilename(new File(method.getURI().getPath()).getName());
            } catch (Exception ignored) {}
            if (isEmpty(returnName) && !FileUtil.getExtension(urlName).isEmpty()) return urlName;
            name = !isEmpty(returnName) ? returnName : !isEmpty(urlName) ? urlName : "download";

            String mime = type == null ? "" : type.split(";")[0].trim();
            String ext = FormatUtil.Format.fromMime(mime).fileExt();
            if (ext == null) {
                String urlExt = FileUtil.getExtension(urlName);
                if (urlExt.matches("[a-z0-9]{1,5}")) ext = "." + urlExt;
                else if (mime.matches("(?i)(text|application)/xml")) ext = ".xml";     // e.g. the MAST and HEASARC TAP services send VOTables as text/xml
                else ext = "";
            }
            return name.toLowerCase().endsWith(ext) ? name : name + ext;
        }
    }


    /** Let the client know the download has started by setting a short-lived cookie named after its download token. */
    public static void sendDownloadStartedCookie(String token, HttpServletResponse res) {
        if (isEmpty(token) || !token.matches("[A-Za-z0-9_-]{1,64}")) return;
        Cookie cookie = new Cookie(DOWNLOAD_COOKIE_PREFIX + token, "1");
        cookie.setPath("/");
        cookie.setMaxAge(60);
        res.addCookie(cookie);
    }

    private void sendFileToClient(HttpServletRequest req, HttpServletResponse res, File f, String local) throws IOException {
        SrvParam sp= new SrvParam(req.getParameterMap());
        boolean log= sp.getOptionalBoolean(LOG_PARAM,false);

        String mType= getServletContext().getMimeType(f.getName());
        if (mType!=null) res.setContentType(mType);

        res.addHeader("Content-Length", f.length()+"");

        String retFileStr= (local!=null) ? local : f.getName();
        if (retFileStr.equals(USE_SERVER_NAME)) retFileStr= f.getName();
        if (!isEmpty(retFileStr)) {
            res.addHeader("Content-Disposition", URLDownload.makeContentDisposition(retFileStr));
        }
        sendDownloadStartedCookie(sp.getOptional(DOWNLOAD_TOKEN), res);



        try {
            FileUtil.writeFileToStream(f, res.getOutputStream());
            if (log) logActivity(f);
        } catch (IOException e) {
            throw e;
        }

    }






    private static void logActivity(File f) {
        String logStr= "File download -- File: " + f.getPath()+
                ", size: " +  FileUtil.getSizeAsString(f.length()) +
                ", bytes: " + f.length();
        _log.info(logStr);
        _statsLog.stats("file", "size(MB)", (double)f.length()/StringUtils.MEG,
                                         "u", FileUtil.getSizeAsString(f.length()), "file", f.getPath());
    }

    public static Cache getCache() { return CacheManager.getUserCache(); }

}
