/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.HttpResultInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;

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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;


public class URLDownload {
    private static final int BUFFER_SIZE = FileUtil.BUFFER_SIZE;
    private static final Logger.LoggerImpl _log = Logger.getLogger();

//=====================================================================
//----------- Public static methods -----------------------------------
//=====================================================================

    public static String getUserFromUrl(String url) { return getUserInfoPart(url,0); }
    public static String getPasswordFromUrl(String url) { return getUserInfoPart(url,1); }

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

    public static String getSugestedFileName(URLConnection conn) {
        if (conn == null) return null;
        String disposition = conn.getHeaderField("Content-disposition");
        if (disposition == null) return null;
        String[] strs = disposition.split(";");
        if (strs.length != 2) return null;
        String[] fname = strs[1].split("=");
        if (fname[0].toLowerCase().contains("filename")) return fname[1];
        return null;
    }

    private static int getResponseCode(URLConnection conn) {
        if (conn==null) return -1;
        try {
            return (conn instanceof HttpURLConnection) ? ((HttpURLConnection)conn).getResponseCode() : 200;
        } catch (IOException e) {
            return -1;
        }
    }

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
            addCookiesToConnection(conn, cookies);
            String[] userInfo = getUserInfo(url);
            if (userInfo != null) {
                String authStringEnc = Base64.encode(userInfo[0] + ":" + userInfo[1]);
                conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
            }
            if (requestHeaders != null && requestHeaders.size() > 0) {
                for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            return conn;
        } catch (IOException e) {
            logError(url,null,e);
            throw e;
        }
    }

    public static ConnCtx makeConnectionCtx(URL url,
                                              Map<String, String> cookies,
                                              Map<String, String> requestHeaders) throws IOException {
          URLConnection conn= makeConnection(url,cookies,requestHeaders);
          return new ConnCtx(url, cookies, requestHeaders, conn);
      }

      private static class ConnCtx {
          public final URL url;
          public final Map<String, String> cookies;
          public final Map<String, String> requestHeaders;
          public final URLConnection conn;
          public ConnCtx( URL url, Map<String, String> cookies, Map<String, String> requestHeaders, URLConnection conn) {
              this.url= url;
              this.cookies= cookies;
              this.requestHeaders= requestHeaders;
              this.conn= conn;
          }
      }

    private static void addCookiesToConnection(URLConnection conn, Map<String, String> cookies) {
        if (!(conn instanceof HttpURLConnection) || cookies == null) return;
        StringBuilder sb = new StringBuilder(200);
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (sb.length() > 0) conn.setRequestProperty("Cookie", sb.toString());
    }

//================================================================================
//------------------ Public getDataFromURL ---------------------------------------
//================================================================================

    /**
     * @param url - the url to download
     * @param postData - a string of the data to post, may be null
     * @param cookies   a map of cookies as name value pairs, may be null
     * @param requestHeaders a map of header as name value pairs, may be null
     * @return the results are in the HttpResultInfo object, call getData() or getResultAsString()
     * @throws FailedRequestException if it fails
     * @throws IOException if it fails
     */
    public static HttpResultInfo getDataFromURL(URL url,
                                                Map<String, String> postData,
                                                Map<String, String> cookies,
                                                Map<String, String> requestHeaders) throws FailedRequestException {
        URLConnection conn= null;
        try {
            conn= makeConnection(url,cookies,requestHeaders);
            pushPostData(conn, postData);

            logHeader(postData, conn, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            netCopy(makeAnyInStream(conn, false), out, conn, 0, null);
            byte[] results = out.toByteArray();
            logCompletedDownload(conn.getURL(), results.length);
            return new HttpResultInfo(results,getResponseCode(conn),conn.getContentType(),getSugestedFileName(conn));
        } catch (IOException e) {
            logError(url, postData, e);
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
            return getDataToFile(makeConnection(url, cookies, requestHeader), outfile, dl, true, true, 0L,timeoutInSec,postData);
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
        return getDataToFile(url, outfile, null, null, null, true, true, 0);
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
        return getDataToFile(url,outfile,cookies,requestHeaders,null,true,true,0);
    }

    /**
     * @param url                  the url to get data from
     * @param outfile              The name of the file to write the data to.
     * @param dl                   listen for progress and cancel if necessary, may be null
     * @param cookies              a map of cookies as name value pairs, may be null
     * @param requestHeaders       a map of header name value pairs, may be null
     * @param onlyIfModified       get the file only if there is a newer version, normally true
     * @param uncompress           if this data appears to be compressed then uncompress it first, normally true
     * @param maxFileSize          maximum that can be downloaded, 0 means don't check, normally 0
     * @return an array of FileInfo objects
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFile(URL url,
                                         File outfile,
                                         Map<String, String> cookies,
                                         Map<String, String> requestHeaders,
                                         DownloadListener dl,
                                         boolean onlyIfModified,
                                         boolean uncompress,
                                         long maxFileSize) throws FailedRequestException {
        try {
            return getDataToFile(makeConnection(url, cookies, requestHeaders),
                                 outfile, dl, onlyIfModified, uncompress, maxFileSize, 0, null);
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
     * @param dl                   listen for progress and cancel if necessary, may be null
     * @param onlyIfModified       get the file only if there is a newer version, normally true
     * @param uncompress           if this data appears to be compressed then uncompress it first, normally true
     * @param maxFileSize          maximum that can be downloaded, 0 means don't check, normally 0
     * @param timeoutInSec         timeout in seconds, 0 is use default, normally 0
     * @param postData             If non-null then send as post data
     * @return an array of FileInfo objects
     * @throws FailedRequestException Any Network Error with simple message, cause will probably be IOException
     */
    public static FileInfo getDataToFile(URLConnection conn,
                                         File outfile,
                                         DownloadListener dl,
                                         boolean onlyIfModified,
                                         boolean uncompress,
                                         long maxFileSize,
                                         int timeoutInSec,
                                         Map<String,String> postData) throws FailedRequestException {
        try {
            FileInfo outFileData;
            Map<String,List<String>> sendHeaders= null;
            long start = System.currentTimeMillis();
            try {
                if (timeoutInSec>0) {
                    conn.setConnectTimeout(timeoutInSec * 1000);//Sets a specified timeout value, in milliseconds
                    conn.setReadTimeout(timeoutInSec * 1000);
                }
                if (conn instanceof HttpURLConnection) {
                    pushPostData(conn,postData);
                    sendHeaders= conn.getRequestProperties();
                    if (onlyIfModified) {
                        outFileData = checkAlreadyDownloaded(conn, outfile);
                        if (outFileData != null) return outFileData;
                    }
                }
                else if (postData!=null) {
                    doPostDataException(conn,postData);
                }
            } catch (IllegalStateException e) {
                // if I get this exception then the connection was already open and I can't set any more headers
                // If that happens then just go ahead with the download
            }
            //------
            //---From here on the server should be responding
            //------
            logHeader(postData, conn, sendHeaders);
            validFileSize(conn, maxFileSize);
            netCopy(makeAnyInStream(conn,uncompress), makeOutStream(outfile), conn, maxFileSize, dl);
            long elapse = System.currentTimeMillis() - start;
            int responseCode= getResponseCode(conn);
            outFileData = new FileInfo(outfile, getSugestedFileName(conn), responseCode,
                    ResponseMessage.getHttpResponseMessage(responseCode), conn.getContentType());
            logDownload(outFileData, conn.getURL().toString(), elapse );

            if (responseCode>=300 && responseCode<400) {
                throw new FailedRequestException(ResponseMessage.getHttpResponseMessage(responseCode),
                                                 "Response Code: "+responseCode, responseCode);
            }
            return outFileData;
        } catch (IOException e) {
            logError(conn.getURL(), null, e);
            throw new FailedRequestException(ResponseMessage.getNetworkCallFailureMessage(e),e, getResponseCode(conn));
        }
    }

    private static String postDataToString(Map<String,String> postData) {
        StringBuilder sBuff= new StringBuilder();
        if (postData.size()==1 && postData.get("")!=null) {
            return postData.get("");
        }
        for(Map.Entry<String,String> entry : postData.entrySet()) {
            if (sBuff.length()>0) sBuff.append("&");
            sBuff.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sBuff.toString();
    }

    private static void pushPostData(URLConnection conn, Map<String,String> postData) throws IOException {
        if (!(conn instanceof HttpURLConnection) || postData==null) return;
        String postStr= postDataToString(postData);
        ((HttpURLConnection)conn).setRequestMethod("POST");
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


    private static void validFileSize(URLConnection conn, long maxFileSize) throws FailedRequestException {
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
    private static FileInfo checkAlreadyDownloaded(URLConnection urlConn, File outfile) throws IOException {
        FileInfo retval = null;
        try {
            if (outfile != null && outfile.canRead() && outfile.length() > 0) {
                urlConn.setIfModifiedSince(outfile.lastModified());
                if (getResponseCode(urlConn) == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    _log.briefInfo(outfile.getName() + ": Not downloading, already have current version");
                    retval = new FileInfo(outfile, getSugestedFileName(urlConn), HttpURLConnection.HTTP_NOT_MODIFIED,
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


    public static void netCopy(DataInputStream in,
                               OutputStream out,
                               URLConnection conn,
                               long maxSize,
                               DownloadListener dl) throws FailedRequestException, IOException {
        try {
            Downloader downloader = new Downloader(in, out, conn.getContentLength());
            downloader.setMaxDownloadSize(maxSize);
            downloader.setDownloadListener(dl);
            downloader.download();
        } finally {
            FileUtil.silentClose(in);
            FileUtil.silentClose(out);
        }
    }


    private static void logDownload(FileInfo retFile, String urlStr, long elapse) {
        if (retFile == null) return;
        String timeStr = (elapse>0) ? ", time: "+UTCTimeUtil.getHMSFromMills(elapse) : "";
        List<String> outList = new ArrayList<>(2);
        outList.add(String.format("Download Complete: %s : %d bytes%s",
                retFile.getFile().getName(), retFile.getFile().length(), timeStr));
        outList.add(urlStr);
        _log.info(outList.toArray(new String[0]));
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

    public static void logHeader(URLConnection conn) { logHeader(null, conn, null); }

    private static void logHeader(Map<String,String> postData, URLConnection conn, Map<String,List<String>> sendHeaders) {
        StringBuffer workBuff;
        try {
            Set<Map.Entry<String,List<String>>> hSet = getResponseCode(conn)==-1 ? null : conn.getHeaderFields().entrySet();
            List<String> outStr= new ArrayList<>(40);
            String key;
            if (conn.getURL() != null) {
                outStr.add("----------Sending");
                outStr.add( conn.getURL().toString());
                if (sendHeaders!=null) {
                    for(Map.Entry<String,List<String>> se: sendHeaders.entrySet()) {
                        workBuff = new StringBuffer(100);
                        key= (se.getKey() == null) ? "<none>" : se.getKey();
                        workBuff.append(StringUtils.pad(20,key));
                        workBuff.append(": ");
                        if (key.equalsIgnoreCase("cookie")) {
                            List<String> cValList= se.getValue();
                            int lenTotal=0;
                            for(String s : cValList) lenTotal+= ((s==null) ? 0 : s.length());
                            workBuff.append("<not shown");
                            if (lenTotal>0) workBuff.append(", length: ").append(lenTotal);
                            workBuff.append(">");
                        }
                        else {
                            workBuff.append(se.getValue());
                        }
                        outStr.add(workBuff.toString());
                    }

                }
            }
            if (postData != null) {
                outStr.add(StringUtils.pad(20,"Post Data ") + ": " + postDataToString(postData));
            }
            if (conn instanceof HttpURLConnection) {
                outStr.add("----------Received Headers, response status code: " + getResponseCode(conn));
            }
            else {
                outStr.add("----------Received Headers");
            }
            if (hSet!=null) {
                List<String> values;
                for (Map.Entry<String, List<String>> e : hSet) {
                    workBuff = new StringBuffer(100);
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


    private static void logCompletedDownload(URL url, long size) {
        _log.info(String.format("Download Complete- %d bytes", size), url != null ? url.toString() : null);
    }

    private static void doPostDataException(URLConnection conn, Map<String,String> postData) throws FailedRequestException {
        FailedRequestException fe = new FailedRequestException("Can only do post with http(s): " + conn.getURL().toString());
        logError(conn.getURL(), postData, fe);
        throw fe;
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

    private static DataInputStream makeAnyInStream(URLConnection conn, boolean uncompress) throws IOException {
        String contentType = conn.getContentType();
        if (conn.getContentEncoding() != null) return makeEncodedInStream(conn);
        else if (uncompress && contentType != null && contentType.toLowerCase().endsWith("gzip")) return makeGZipInStream(conn);
        else return makeDataInStream(conn);
    }

    private static DataInputStream makeEncodedInStream(URLConnection conn) throws IOException {
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

    private static DataInputStream makeDataInStream(URLConnection conn) throws IOException {
        if (conn instanceof HttpURLConnection && getResponseCode(conn)==-1) {
            throw new IOException("Http Response Code is -1, invalid http protocol, " +
                                          "probably no status line in response headers");
        }
        else {
            try {
                return new DataInputStream(makeInStream(conn));
            }
            catch (IOException e) {
                if (!(conn instanceof HttpURLConnection)) throw e;
                return new DataInputStream(makeErrStream((HttpURLConnection) conn));
            }
        }
    }

    private static InputStream makeInStream(URLConnection conn) throws IOException {
        return new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
    }

    private static InputStream makeErrStream(HttpURLConnection conn) {
        return new BufferedInputStream(conn.getErrorStream(), BUFFER_SIZE);
    }
}