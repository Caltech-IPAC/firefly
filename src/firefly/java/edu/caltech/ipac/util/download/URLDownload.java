/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.HttpResultInfo;
import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static edu.caltech.ipac.firefly.server.network.HttpServices.sanitizeHeader;


public class URLDownload {
    private static final int BUFFER_SIZE = FileUtil.BUFFER_SIZE;
    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final int MAX_REDIRECT= 2;

    static {
        boolean DISABLE_SSL_VERIFICATION = false;
        if (DISABLE_SSL_VERIFICATION) disableSSLCertificateChecking();
    }


    public static String getUserFromUrl(String url) { return getUserInfoPart(url,0); }
    public static String getPasswordFromUrl(String url) { return getUserInfoPart(url,1); }

    private static void disableSSLCertificateChecking() { // use for testing
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1) { }
                    public void checkServerTrusted(X509Certificate[] arg0, String arg1) { }
                } };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            _log.error(e);
        }
    }


    private static String getUserInfoPart(String url,int idx) {
        try {
            String[] userInfo = getUserInfo(new URL(url));
            return userInfo == null ? null : userInfo[idx];
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String[] getUserInfo(URL url) {
        String auth = url.getAuthority();
        if (auth != null && auth.contains("@")) {
            String[] parts = auth.split("@", 2);
            String[] userInfo = parts[0].split(":", 2);
            if (userInfo.length == 2) {
                return userInfo;
            }
        }
        return null;
    }

    public static String getSuggestedFileName(URLConnection conn) {
        if (conn == null) return null;
        String disposition = conn.getHeaderField("Content-disposition");
        if (disposition == null) return null;
        return getSuggestedFileName(disposition);
    }

    private static int codeFromException(Exception e) {
        return switch (e) {
            case SSLException ignored -> 495;
            case SocketTimeoutException ignored -> 408;
            case UnknownHostException ignored -> 404;
            default -> 500;
        };
    }

    private static HttpResultInfo exceptionToResponse(Exception e, Map<String,String> sendHeaders) {
        var r= new HttpResultInfo(codeFromException(e),ResponseMessage.getNetworkCallFailureMessage(e));
        r.setSendHeaders(sendHeaders);
        return r;
    }

    private static FileInfo exceptionToFileInfo(Exception e) {
        return new FileInfo(codeFromException(e),ResponseMessage.getNetworkCallFailureMessage(e));
    }

    public static String getSuggestedFileName(String disposition) {
        if (disposition == null) return null;
        String[] strs = disposition.split(";");
        if (strs.length != 2) return null;
        String[] fname = strs[1].split("=");
        if (fname[0].toLowerCase().contains("filename")) {
            return sanitizeFilename(fname[1]);
        }
        return null;
    }

    public static String getFileNameFromUrl(URL url) {
        if (url == null) return null;
        String urlPath = url.getPath();
        String suggestedFileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
        return sanitizeFilename(suggestedFileName);
    }

    public static Map<String, String> buildReqHeaders(URL url, Map<String, String> requestHeaders, Options ops) {
        if (requestHeaders== null) requestHeaders= Collections.emptyMap();
        Map<String, String> h = new HashMap<>(requestHeaders);
        if (ops==null || ops.useCredentials) {
            var inputs= new HttpServiceInput(url.toString());
            var credentials= inputs.getHeaders();
            if (credentials!=null && !credentials.isEmpty()) {
                if (!credentials.keySet().stream().allMatch(h::containsKey)) h.putAll(credentials);
            }
        }
        return h;
    }

    public static String sanitizeFilename(String fName) {
        if (StringUtils.isEmpty(fName)) return "";
        //trim leading/trailing whitespace and quotes
        fName = fName.trim().replaceAll("^[\"']+|[\"']+$", "");
        //replace unwanted characters
        fName = fName.replaceAll("[^a-zA-Z0-9._-]", "_");
        //remove leading/trailing underscores
        fName = fName.replaceAll("^_+", "").replaceAll("_+$", "");
        return fName;
    }

    private static String sendHeadersToCompactStr(Map<String,List<String>> sendHeaders) {

        if (sendHeaders.isEmpty()) return "";
        var outStr= new StringBuilder();
        for(Map.Entry<String,List<String>> se: sendHeaders.entrySet()) {
            if (!outStr.isEmpty()) outStr.append(", ");
            StringBuilder workBuff = new StringBuilder(100);
            var key= (se.getKey() == null) ? "<none>" : se.getKey();
            workBuff.append(key);
            workBuff.append(": ");
            if (key.equalsIgnoreCase("cookie")) {
                try {
                    List<String> cValList= se.getValue();
                    int lenTotal= cValList.stream().map( (s) -> s==null ? 0 : s.length()).reduce(0, Integer::sum);
                    String names= cValList.stream()
                            .reduce("", (all,t) -> all+ Arrays.stream(t.split(";"))
                                    .map( (s) -> s.split("=")[0])
                                    .reduce("", (allV,tv) -> allV+tv+","));
                    workBuff.append("<cookie names: ").append(names);
                    if (lenTotal>0) workBuff.append(" length: ").append(lenTotal);
                    workBuff.append(">");
                }
                catch (Exception e) {
                    workBuff.append("<not shown>");
                }
            }
            else {
                workBuff.append(sanitizeHeader(se.getKey(), String.valueOf(se.getValue())));
            }
            outStr.append(workBuff.toString());
        }
        return outStr.toString();
    }

    private static int getResponseCode(HttpURLConnection conn) {
        if (conn==null) return -1;
        try {
            return conn.getResponseCode();
        } catch (SocketTimeoutException e) {
            return 408;
        } catch (UnknownHostException e) {
            return 404;
        } catch (IOException e) {
            return -1;
        }
    }

    private static URL makeURL(String urlStr) {
        return Util.Try.it(() -> new URI(urlStr).toURL()).getOrElse((URL)null);
    }

    private static URL urlFromLocation(HttpURLConnection conn) { return makeURL(conn.getHeaderField("Location")); }

    /**
     * Create a URLConnection and add cookies and headers. Log and error on failure.
     * This method is not typically used outside of URLDownload. Don't use this method unless you have good reason.
     * You should be able to use the download methods that take a url directly.
     * @param url the url
     * @param cookies  map of cookies
     * @param requestHeaders map of headers
     * @return the connection
     * @throws IOException - if the connection fails
     */
    public static URLConnection makeConnection(URL url,
                                               Map<String, String> cookies,
                                               Map<String, String> requestHeaders) throws IOException {
        try {
            URLConnection conn = url.openConnection();
            if (conn instanceof HttpURLConnection httpConn) {
                httpConn.setRequestProperty("User-Agent", VersionUtil.getUserAgentString());
                httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
                if (cookies != null) {
                    var filteredCookies= new HashMap<>(cookies);
                    filteredCookies.remove("JSESSIONID");
                    filteredCookies.remove(RequestOwner.USER_KEY);
                    if (!filteredCookies.isEmpty()) addCookiesToConnection(httpConn, filteredCookies);
                }
                String[] userInfo = getUserInfo(url);
                if (userInfo != null) {
                    String authStringEnc = Base64.encode(userInfo[0] + ":" + userInfo[1]);
                    httpConn.setRequestProperty("Authorization", "Basic " + authStringEnc);
                }
            }
            if (requestHeaders != null && !requestHeaders.isEmpty()) {
                for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            return conn;
        } catch (IOException e) {
            logError(url,null,e);
            throw e;
        }
    }

    public static HttpURLConnection makeURLConnection(URL url,
                                                      Map<String, String> cookies,
                                                      Map<String, String> requestHeaders) throws IOException {

        var conn= makeConnection(url, cookies, requestHeaders);
        if (conn==null) throw new IOException("HTTP connection not be created");
        if (conn instanceof HttpURLConnection httpConn) return httpConn;
        throw new IOException("HTTP connection not be created, could not be cast to HttpURLConnection");
    }

    private static void addCookiesToConnection(HttpURLConnection conn, Map<String, String> cookies) {
        if (cookies == null) return;
        StringBuilder sb = new StringBuilder(200);
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (!sb.isEmpty()) conn.setRequestProperty("Cookie", sb.toString());
    }

//================================================================================
//------------------ Public getDataFromURL ---------------------------------------
//================================================================================

    public static HttpResultInfo getDataFromURL(URL url,
                                                Map<String, String> postData,
                                                Map<String, String> requestHeaders) throws FailedRequestException {
        return getDataFromURL(url,postData,null,requestHeaders, null, Options.def());
    }

    /**
     * @param url - the url to download
     * @param postData - a string of the data to post, may be null
     * @param cookies   a map of cookies as name value pairs, may be null
     * @param requestHeaders a map of header as name value pairs, may be null
     * @param  outByteBuffer write output to byte buffer, if used then HttpResultInfo.getResult() will return null
     * @return the results are in the HttpResultInfo object, call getData() or getResultAsString() or use outByteBuffer
     * @throws FailedRequestException if it fails
     */
    public static HttpResultInfo getDataFromURL(URL url,
                                                Map<String, String> postData,
                                                Map<String, String> cookies,
                                                Map<String, String> requestHeaders,
                                                ByteBuffer outByteBuffer,
                                                Options ops) throws FailedRequestException {
        HttpURLConnection conn= null;
        try {
            StopWatch.Tracker tracker = new StopWatch.Tracker("Download", null);
            tracker.starts();
            var h= buildReqHeaders(url, requestHeaders, ops);
            conn= makeURLConnection(url,cookies,h);
            Map<String,List<String>> reqProp= conn.getRequestProperties();
            pushPostData(conn, postData);

            byte[] results = null;

            DataInputStream in= makeAnyInStream(conn, false);
            long conLen= conn.getContentLength();
            if (outByteBuffer!=null) {
                Downloader.download(in, outByteBuffer, conLen, ops.maxFileSize, ops.dl);
            }
            else {
                ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
                Downloader.download(in, out, conLen, 0, ops.dl);
                results = out.toByteArray();
            }
            var responseCode= getResponseCode(conn);
            Set<Map.Entry<String,List<String>>> hSet = responseCode==-1 ? Collections.emptySet() : conn.getHeaderFields().entrySet();
            var result= new HttpResultInfo(results,getResponseCode(conn),null, conn.getContentType(), getSuggestedFileName(conn));
            result.setSendHeaders(h);
            for (Map.Entry<String, List<String>> e : hSet) {
                result.putAttribute(e.getKey()!=null ? e.getKey() : "<none>",combineValues(e.getValue()));
            }
            tracker.stops();
            double dlSeconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
            if (responseCode>300) logHeader(url.toString(), postData, conn, reqProp);
            if (!ops.logErrorsOnly) logSuccess(result,url,dlSeconds,reqProp, postData);
            return result;
        } catch (SSLException | SocketTimeoutException | UnknownHostException e) {
            logError(url, postData, e);
            return exceptionToResponse(e,requestHeaders);
        } catch (IOException e) {
            logError(url, postData, e);
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e), e, getResponseCode(conn));
        }
    }

    public static HttpResultInfo getHeader(URL url,
                                           Map<String, String> cookies,
                                           Map<String, String> requestHeaders,
                                           int timeoutInSec ) throws FailedRequestException {
        Map <String,String> h= null;
        try {
            h= buildReqHeaders(url,requestHeaders,null);
            HttpURLConnection conn= makeURLConnection(url,cookies,h);
            return getHeaderFromConnection(conn,timeoutInSec,MAX_REDIRECT,cookies,h);
        } catch (SSLException | SocketTimeoutException | UnknownHostException e) {
            return exceptionToResponse(e,h);
        } catch (IOException e) {
            logError(url, null, e);
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e), e, -1);
        }
    }

    private static HttpResultInfo getHeaderFromConnection(HttpURLConnection conn,
                                                          int timeoutInSec,
                                                          int redirectCnt,
                                                          Map<String, String> cookies,
                                                          Map<String, String> requestHeaders) throws FailedRequestException {
        try {
            if (timeoutInSec>0) {
                conn.setConnectTimeout(timeoutInSec * 1000);
                conn.setReadTimeout(timeoutInSec * 1000 + 3000);
            }
            conn.setRequestMethod("HEAD");


            conn.connect();
            Set<Map.Entry<String,List<String>>> hSet = getResponseCode(conn)==-1 ? Collections.emptySet() : conn.getHeaderFields().entrySet();
            HttpResultInfo result= new HttpResultInfo(null,getResponseCode(conn),null, conn.getContentType(), getSuggestedFileName(conn));
            result.setSendHeaders(requestHeaders);

            for (var e : hSet) {
                result.putAttribute(e.getKey()!=null ? e.getKey() : "<none>",combineValues(e.getValue()));
            }

            conn.disconnect();
            result.putAttribute("Location", conn.getURL().toString());
            if (redirectCnt<MAX_REDIRECT) result.setRedirected(true);
            var responseCode= getResponseCode(conn);
            if (responseCode >= 300 && responseCode < 400) {
                if (redirectCnt > 0 && Arrays.asList(301,302,303,307,308).contains(responseCode)) {
                    HttpURLConnection newConn = makeURLConnection(urlFromLocation(conn), cookies, requestHeaders);
                    return getHeaderFromConnection(newConn, 2, redirectCnt-1, cookies, requestHeaders);
                }
                result.putAttribute("Location", conn.getHeaderField("Location"));
                throw new FailedRequestException(ResponseMessage.getHttpResponseMessage(responseCode),
                        "Response Code: " + responseCode, responseCode);
            }
            return result;
        } catch (SSLException | SocketTimeoutException | UnknownHostException e) {
            logError(conn.getURL(), null , e);
            return exceptionToResponse(e,requestHeaders);
        } catch (IOException e) {
            logError(conn.getURL(), null, e);
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e), e, getResponseCode(conn));
        }
    }

//======================================================================
//------------------ Public getDataToFile using a URL  -----------------
//======================================================================

    /**
     * @param url      the url to get data from
     * @param postData string of data to post
     * @param outfile  write the url data to this file
     * @param dl       listen for progress and cancel if necessary
     * @param timeoutInSec timeout in seconds
     * @return a FileInfo
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFileUsingPost(URL url, Map<String,String> postData,
                                                  Map<String, String> cookies, Map<String, String> requestHeader,
                                                  File outfile, DownloadListener dl,
                                                  int timeoutInSec) throws FailedRequestException {
        try {
            Options ops= new Options(true, true, 0L, false, false, timeoutInSec, dl, false, false);
            return getDataToFile(makeURLConnection(url, cookies, requestHeader), outfile, ops, postData,0);
        } catch (IOException e) {
            logError(url, postData, e);
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e), e);
        }
    }

    /**
     * Download data from the URL to the file. If this data appears to be compressed then uncompress it first
     *
     * @param url     the url to get data from
     * @param outfile The name of the file to write the data to. uncompress it first
     * @return an array of FileInfo objects
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFile(URL url, File outfile) throws FailedRequestException {
        return getDataToFile(url, outfile, null, null, Options.def());
    }

    /**
     * @param url                  the url to get data from
     * @param outfile              The name of the file to write the data to.
     * @param cookies              a map of cookies as name value pairs, may be null
     * @param requestHeaders       a map of header name value pairs, may be null
     * @return an array of FileInfo objects
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFile(URL url,
                                         File outfile,
                                         Map<String, String> cookies,
                                         Map<String, String> requestHeaders) throws FailedRequestException {
        return getDataToFile(url,outfile,cookies,requestHeaders, Options.def());
    }

    /**
     * @param url                  the url to get data from
     * @param outfile              The name of the file to write the data to.
     * @param cookies              a map of cookies as name value pairs, may be null
     * @param requestHeaders       a map of header name value pairs, may be null
     * @param ops                  download options
     * @return an array of FileInfo objects
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFile(URL url,
                                         File outfile,
                                         Map<String, String> cookies,
                                         Map<String, String> requestHeaders,
                                         Options ops) throws FailedRequestException {
        try {
            var h= buildReqHeaders(url,requestHeaders,ops);
            return getDataToFile(makeURLConnection(url, cookies, h), outfile, ops, null, ops.allowRedirect?MAX_REDIRECT:0);
        } catch (IOException e) {
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e), e);
        }
    }

//================================================================================
//------------------ Public getDataToFile using a URLConnection  -----------------
//================================================================================
    /**
     * @param conn                 the URLConnection
     * @param outfile              The name of the file to write the data to.
     * @param ops                  download options
     * @param postData             If non-null then send as post data
     * @return an array of FileInfo objects
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFile(HttpURLConnection conn,
                                         File outfile,
                                         Options ops,
                                         Map<String,String> postData,
                                         int redirectCnt) throws FailedRequestException {

        try {
            StopWatch.Tracker tracker = new StopWatch.Tracker("Download", null);
            tracker.starts();
            String originalUrl= conn.getURL().toString();
            FileInfo outFileData;
            Map<String, List<String>> reqProp = conn.getRequestProperties();
            Map<String, List<String>> sendHeaders = null;
            long start = System.currentTimeMillis();
            try {
                if (ops.timeoutInSec > 0) {
                    conn.setConnectTimeout(ops.timeoutInSec * 1000);//Sets a specified timeout value, in milliseconds
                    conn.setReadTimeout(ops.timeoutInSec * 1000);
                }
                pushPostData(conn, postData);
                sendHeaders = conn.getRequestProperties();
                if (ops.onlyIfModified) {
                    outFileData = checkAlreadyDownloaded(conn, outfile);
                    if (outFileData != null) return outFileData;
                    if (getResponseCode(conn) == 408) {
                        throw new FailedRequestException("Timeout", "Timeout", 408);
                    }
                }
            } catch (IllegalStateException e) {
                // if I get this exception then the connection was already open and I can't set any more headers
                // If that happens then just go ahead with the download
            }
            //------
            //---From here on the server should be responding
            //------
            conn.connect();
            validFileSize(conn, ops.maxFileSize);
            Downloader.download(makeAnyInStream(conn, ops.uncompress), outfile, conn.getContentLength(), ops.maxFileSize, ops.dl);
            long elapse = System.currentTimeMillis() - start;
            int responseCode = getResponseCode(conn);
            outFileData = new FileInfo(outfile, getSuggestedFileName(conn), responseCode,
                    ResponseMessage.getHttpResponseMessage(responseCode), conn.getContentType());
            Set<Map.Entry<String,List<String>>> hSet = responseCode==-1 ? Collections.emptySet() : conn.getHeaderFields().entrySet();
            for (Map.Entry<String, List<String>> e : hSet) {
                outFileData.putAttribute(e.getKey()!=null ? e.getKey() : "<none>",combineValues(e.getValue()));
            }
            if (conn.getContentEncoding() != null)
                outFileData.putAttribute("content-encoding", conn.getContentEncoding());
//            if (responseCode>=300) logDownload(outFileData, conn.getURL().toString(), elapse);

            if (responseCode >= 300 && responseCode < 400) {
                if (redirectCnt > 0 && Arrays.asList(301,302,303,307,308).contains(responseCode)) {
                    return redirect(conn, outfile, reqProp, ops, redirectCnt - 1);
                }
                outFileData.putAttribute("Location", conn.getHeaderField("Location"));
                throw new FailedRequestException(ResponseMessage.getHttpResponseMessage(responseCode),
                        "Response Code: " + responseCode, responseCode, outFileData);
            }
            tracker.stops();
            double dlSeconds = tracker.getElapsedTime(StopWatch.Unit.SECONDS);
            if (responseCode>300) logHeader(originalUrl, postData, conn, sendHeaders);
            if (!ops.logErrorsOnly && responseCode<300) logSuccess(outFileData,outfile,conn.getURL(),dlSeconds,sendHeaders);
            return outFileData;
        } catch (SSLException | SocketTimeoutException | UnknownHostException e) {
            return exceptionToFileInfo(e);
        } catch (IOException e) {
            logError(conn.getURL(), null, e);
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e),e, getResponseCode(conn));
        }
    }


    private static FileInfo redirect(HttpURLConnection conn,
                                     File outfile,
                                     Map<String,List<String>> reqProp,
                                     Options ops,
                                     int redirectCnt) throws FailedRequestException, IOException {

        var ignore= outfile.delete();
        String urlStr= conn.getHeaderField("Location");
        HttpURLConnection newConn= makeURLConnection(makeURL(urlStr), null, null);
        for(Map.Entry<String,List<String>> entry : reqProp.entrySet()) {
            for(String s : entry.getValue()) newConn.setRequestProperty(entry.getKey(), s);
        }
        if (conn.getHeaderField("Set-Cookie")!=null) {
            newConn.setRequestProperty("Cookie", conn.getHeaderField("Set-Cookie"));
        }
        newConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        return getDataToFile(newConn, outfile, ops, null, redirectCnt);
    }

    private static String postDataToString(Map<String,String> postData) {
        StringBuilder sBuff= new StringBuilder();
        if (postData.size()==1 && postData.get("")!=null) {
            return postData.get("");
        }
        for(Map.Entry<String,String> entry : postData.entrySet()) {
            if (!sBuff.isEmpty()) sBuff.append("&");
            sBuff.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sBuff.toString();
    }

    private static void pushPostData(HttpURLConnection conn, Map<String,String> postData) throws IOException {
        if (postData==null) return;
        String postStr= postDataToString(postData);
        conn.setRequestMethod("POST");
        if (conn.getRequestProperty("Content-Type")==null) {
            conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
        }
        conn.setRequestProperty( "Content-Length", String.valueOf(postStr.length()));
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        OutputStreamWriter wr = new OutputStreamWriter(os);
        wr.write(postStr);
        wr.flush();
        wr.close();
    }


    private static void validFileSize(HttpURLConnection conn, long maxFileSize) throws FailedRequestException {
        long contLen = conn.getContentLength();
        if (maxFileSize > 0 && contLen > 0 && contLen > maxFileSize) {
            throw new FailedRequestException(
                    "File too big to download, " + FileUtil.getSizeAsString(contLen) +
                            ", Max: " + FileUtil.getSizeAsString(maxFileSize),
                    "URL content length header reports content size greater then max size passed as parameter. " +
                            "Content length:  " + contLen + ", maxFileSize: " + maxFileSize, getResponseCode(conn));
        }
    }


    /**
     * Check if the file already has been downloaded. This method needs to be call at a very precise time in the life of
     * the URL.  If must be called before any data is retrieve. It will set a header and then retrieve the response. No
     * headers can be set after this method and no data or headers can be retrieved before this method.
     *
     * @param urlConn the connection
     * @param outfile target file download filename
     * @return the FileInfo if the file exist and is not out of date, otherwise null
     * @throws IOException if something goes wrong
     */
    private static FileInfo checkAlreadyDownloaded(HttpURLConnection urlConn, File outfile) throws IOException {
        FileInfo retval = null;
        try {
            if (outfile != null && outfile.canRead() && outfile.length() > 0) {
                urlConn.setIfModifiedSince(outfile.lastModified());
                if (getResponseCode(urlConn) == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    String urlStr= urlConn.getURL().toString();
                    _log.info(outfile.getName() + ": Not downloading, already have current version, from "+urlStr);
                    retval = new FileInfo(outfile, getSuggestedFileName(urlConn), HttpURLConnection.HTTP_NOT_MODIFIED,
                                     ResponseMessage.getHttpResponseMessage(HttpURLConnection.HTTP_NOT_MODIFIED));
                    retval.putAttribute(FileInfo.FILE_DOWNLOADED,false+"");
                }
            }
        } catch (IllegalStateException e) {
            // if I get this exception then the connection was already open and I can't set any more headers
            // If that happens then just go ahead with the download
        }
        return retval;
    }


    private static void logError(URL url, Map<String,String> postData, Exception e) {
        List<String> strList = new ArrayList<>(6);
        strList.add("----------Network Error-----------");
        if (url != null) {
            strList.add("----------Sending");
            strList.add(url.toString());
        }
        if (postData != null) {
            strList.add(StringUtils.pad(20, "Post Data ") + ": " + postDataToString(postData));
        }
        if (e != null) {
            strList.add(StringUtils.pad(20,"----------Exception "));
            strList.add(e.toString());
        }
        _log.warn(strList.toArray(new String[0]));
    }

    private static void logHeader(String originalUrl,  Map<String,String> postData, HttpURLConnection conn, Map<String,List<String>> sendHeaders) {
        StringBuilder workBuff;
        try {
            String verb= "";
            verb= conn.getRequestMethod();
            Set<Map.Entry<String,List<String>>> hSet= Collections.emptySet();
            hSet = getResponseCode(conn)==-1 ? null : conn.getHeaderFields().entrySet();
            List<String> outStr= new ArrayList<>(40);
            String key;
            if (conn.getURL() != null) {
                outStr.add("----------Sending " + verb);
                outStr.add( conn.getURL().toString());
                if (originalUrl!=null && !conn.getURL().toString().equals(originalUrl)) {
                    outStr.add( StringUtils.pad(20, "Original URL")+": "+originalUrl);
                }
                if (sendHeaders!=null) {
                    for(Map.Entry<String,List<String>> se: sendHeaders.entrySet()) {
                        workBuff = new StringBuilder(100);
                        key= (se.getKey() == null) ? "<none>" : se.getKey();
                        workBuff.append(StringUtils.pad(20,key));
                        workBuff.append(": ");
                        if (key.equalsIgnoreCase("cookie")) {
                            try {
                                List<String> cValList= se.getValue();
                                int lenTotal= cValList.stream().map( (s) -> s==null ? 0 : s.length()).reduce(0, Integer::sum);
                                String names= cValList.stream()
                                        .reduce("", (all,t) -> all+ Arrays.stream(t.split(";"))
                                                .map( (s) -> s.split("=")[0])
                                                .reduce("", (allV,tv) -> allV+tv+","));
                                workBuff.append("<cookie names: ").append(names);
                                if (lenTotal>0) workBuff.append(" length: ").append(lenTotal);
                                workBuff.append(">");
                            }
                            catch (Exception e) {
                                workBuff.append("<not shown>");
                            }
                        }
                        else {
                            workBuff.append(sanitizeHeader(se.getKey(), String.valueOf(se.getValue())));
                        }
                        outStr.add(workBuff.toString());
                    }

                }
            }
            if (postData != null) {
                outStr.add(StringUtils.pad(20,"Post Data ") + ": " + postDataToString(postData));
            }
            outStr.add("----------Received Headers, response status code: " + getResponseCode(conn));
            if (hSet!=null) {
                List<String> values;
                for (Map.Entry<String, List<String>> e : hSet) {
                    workBuff = new StringBuilder(100);
                    key = e.getKey();
                    if (key == null) key = "<none>";
                    workBuff.append(StringUtils.pad(20, key));
                    workBuff.append(": ");
                    values = e.getValue();
                    Iterator<String> valIter;
                    int m;
                    for (m = 0, valIter = values.iterator(); (valIter.hasNext()); m++) {
                        if (m > 0) workBuff.append("; ");
                        workBuff.append(valIter.next());
                    }
                    outStr.add(workBuff.toString());
                }
            }
            else {
                outStr.add("No headers or status received, invalid http response, using work around");
            }
            _log.info(outStr.toArray(new String[0]));
        } catch (Exception e) {
            _log.info(e.getMessage() + ":" + " url=" + (conn.getURL()!=null ? conn.getURL().toString() : "none"));
        }
    }


    private static void logSuccess(FileInfo fileInfo, File outfile, URL url,  double dSeconds, Map<String,List<String>> sendHeaders) {
        String formatedSize= FileUtil.getSizeAsString(fileInfo.getSizeInBytes());
        String lastMod= fileInfo.getAttribute("Last-Modified")!=null ? ", Last-Modified: " +fileInfo.getAttribute("Last-Modified") : "";
        _log.info(
                String.format( "DOWNLOAD (%.1f sec, %s, response: %d): Content-Type: %s, Content-Length: %s%s",
                        dSeconds, formatedSize, fileInfo.getResponseCode(), fileInfo.getContentType(), fileInfo.getSizeInBytes(), lastMod),
                "url:  "+ url.toString(),
                "file: "+ outfile.toPath(),
                "send headers: "+sendHeadersToCompactStr(sendHeaders),
                "more response headers: "+otherHeadersToStr(fileInfo)
        );
    }

    private static void logSuccess(HttpResultInfo r, URL url, double dSeconds, Map<String,List<String>> sendHeaders, Map<String, String> postData) {
        String formatedSize= FileUtil.getSizeAsString(r.getContentLength());
        String lastMod= r.getAttribute("Last-Modified")!=null ? ", Last-Modified: " +r.getAttribute("Last-Modified") : "";
        String postStr= (postData==null || postData.isEmpty()) ? "" :  "\n        Post Data :" +  postDataToString(postData);
        String send= "send headers: "+sendHeadersToCompactStr(sendHeaders)  + postStr;

        _log.info(
                String.format( "DOWNLOAD to memory (%.1f sec, %s, response: %d): Content-Type: %s, Content-Length: %s%s",
                        dSeconds, formatedSize, r.getResponseCode(), r.getContentType(), r.getContentLength(), lastMod),
                "url:  "+ url.toString(),
                send,
                "more response headers: "+otherHeadersToStr(r)
        );
    }


    private static String otherHeadersToStr(FileInfo r) {
        StringBuilder out = new StringBuilder();
        int cnt = 0;
        for (var a : r.getAttributeMap().entrySet()) {
            var k = a.getKey();
            ;
            if (!r.isReservedKey(k) && !k.equalsIgnoreCase("<none>") &&
                    !k.equalsIgnoreCase("content-type") && !k.equalsIgnoreCase("content-length") &&
                    !k.equalsIgnoreCase("Last-Modified")) {
                if (cnt > 0) out.append(", ");
                out.append(String.format("%s: %s", k, a.getValue()));
                cnt++;
            }
        }
        return out.toString();
    }
    
    private static String otherHeadersToStr(HttpResultInfo r) {
        StringBuilder out= new StringBuilder();
        int cnt=0;
        for(var a : r.getAttributes().entrySet()) {
            var k = a.getKey();;
            if (r.isReservedKey(k) && !k.equalsIgnoreCase("<none>") &&
                    !k.equalsIgnoreCase("content-type") && !k.equalsIgnoreCase("content-length") &&
                    !k.equalsIgnoreCase("Last-Modified") ) {
                if (cnt>0) out.append(", ");
                out.append(String.format("%s: %s", k, a.getValue()));
                cnt++;
            }
        }
        return out.toString();
    }

//======================================================================
//------------------ Private in/out Stream Methods ---------------------
//======================================================================


    private static OutputStream makeOutStream(File f) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(f), BUFFER_SIZE);
    }

    private static DataInputStream makeGZipInStream(URLConnection conn) throws IOException {
        return new DataInputStream(new GZIPInputStream(conn.getInputStream(), BUFFER_SIZE));
    }

    private static DataInputStream makeAnyInStream(HttpURLConnection conn, boolean uncompress) throws IOException {
        String contentType = conn.getContentType();
        if (conn.getContentEncoding() != null) return makeEncodedInStream(conn);
        else if (uncompress && contentType != null && contentType.toLowerCase().endsWith("gzip")) return makeGZipInStream(conn);
        else return makeDataInStream(conn);
    }

    private static DataInputStream makeEncodedInStream(HttpURLConnection conn) throws IOException {
        String encodeType = conn.getContentEncoding();
        if (encodeType == null) return null;
        if (encodeType.toLowerCase().endsWith("gzip")) {
            return makeGZipInStream(conn);
        } else if (encodeType.toLowerCase().endsWith("deflate")) {
            return  new DataInputStream(new InflaterInputStream(makeInStream(conn)));
        } else {
            _log.warn("unrecognized Content-encoding: " + encodeType, "cannot uncompress");
            return  makeDataInStream(conn);
        }
    }

    private static DataInputStream makeDataInStream(HttpURLConnection conn) throws IOException {
        if (getResponseCode(conn)==-1) {
            _log.warn("Http Response Code is -1, invalid http protocol, " +
                                                      "probably no status line in response headers- trying anyway");
            return new DataInputStream(makeInStream(conn));
        }
        else {
            try {
                return new DataInputStream(makeInStream(conn));
            }
            catch (IOException e) {
                return new DataInputStream(makeErrStream(conn));
            }
        }
    }

    private static InputStream makeInStream(URLConnection conn) throws IOException {
        return new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
    }

    private static InputStream makeErrStream(HttpURLConnection conn) {
        return new BufferedInputStream(conn.getErrorStream(), BUFFER_SIZE);
    }

    private static String combineValues(List<String> values) {
        StringBuilder workBuff = new StringBuilder(100);
        Iterator<String> valIter;
        int m;
        for (m = 0, valIter = values.iterator(); (valIter.hasNext()); m++) {
            if (m > 0) workBuff.append("; ");
            workBuff.append(valIter.next());
        }
        return workBuff.toString();
    }

    public static class Options {
        private boolean onlyIfModified;
        private boolean uncompress;
        private long maxFileSize;
        private boolean allowRedirect;
        private boolean useCredentials;
        private int timeoutInSec;
        private DownloadListener dl;
        private boolean logErrorsOnly;
        private boolean expectStaticFile;

        public Options(boolean onlyIfModified, boolean uncompress, long maxFileSize, boolean allowRedirect,
                       boolean useCredentials, int timeoutInSec, DownloadListener dl,
                       boolean logErrorsOnly, boolean expectStaticFile) {
            this.onlyIfModified= onlyIfModified;
            this.uncompress= uncompress;
            this.maxFileSize= maxFileSize;
            this.allowRedirect= allowRedirect;
            this.useCredentials= useCredentials;
            this.timeoutInSec= timeoutInSec;
            this.dl= dl;
            this.logErrorsOnly= logErrorsOnly;
            this.expectStaticFile= expectStaticFile;
        }

        /**
         * convenience function
         * set no size limit,
         * sets true: onlyIfModified, uncompress, use credentials, allowRedirect
         * @return Options
         */
        public static Options def() {return new Options(true, true, 0, true, true, 0, null, false, false);}



        /**
         * convenience function
         * set no size limit,
         * sets true: uncompress, allowRedirect, use credentials
         * @param onlyIfModified - check for file modification
         * @param timeoutInSec - timeout in seconds, 0 use the default timeout
         * @return Options
         */
        public static Options modifiedAndTimeoutOp(boolean onlyIfModified, int timeoutInSec) {
            return new Options(onlyIfModified, true, 0, true, true, timeoutInSec, null, false, false);
        }

        public void setOnlyIfModified(boolean onlyIfModified) { this.onlyIfModified = onlyIfModified; }
        public void setUncompress(boolean uncompress) { this.uncompress = uncompress; }
        public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
        public void setAllowRedirect(boolean allowRedirect) { this.allowRedirect = allowRedirect; }
        public void setUseCredentials(boolean useCredentials) { this.useCredentials = useCredentials; }
        public void setTimeoutInSec(int timeoutInSec) { this.timeoutInSec = timeoutInSec; }
        public void setDl(DownloadListener dl) { this.dl = dl; }
        public void setLogErrorsOnly(boolean logErrorsOnly) { this.logErrorsOnly = logErrorsOnly; }
        public void setExpectStaticFile(boolean expectStaticFile) { this.expectStaticFile = expectStaticFile; }

        public DownloadListener dl() { return dl; }
        public boolean onlyIfModified() { return onlyIfModified; }
        public boolean expectStaticFile() { return expectStaticFile; }
        public long maxFileSize() { return maxFileSize; }
    }

}