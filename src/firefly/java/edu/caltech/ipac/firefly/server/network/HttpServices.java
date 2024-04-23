/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.network;

import com.google.common.net.HttpHeaders;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.CollectionUtil;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.cookie.CookiePolicy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;


/**
 * Date: 6/13/14
 *
 * @author loi
 * @version $Id: $
 */
public class HttpServices {
    public static final int BUFFER_SIZE = FileUtil.BUFFER_SIZE;    // 64k
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    private static HttpClient newHttpClient() {
        HttpClient httpClient = new HttpClient();
        HttpConnectionManagerParams params = httpClient.getHttpConnectionManager().getParams();
        params.setConnectionTimeout(5000);
        params.setSoTimeout(0);     // this is the default.. but, setting it explicitly to be sure
        return httpClient;
    }

//====================================================================
//  GET convenience functions
//====================================================================

    /**
     * For convenience, this function will return 400-bad-request if url is malformed or results is a bad File.
     * @param url  the resource url to call
     * @param results  the file to same the results in.
     * @return Status
     */
    public static Status getData(String url, File results) {
        return getData(new HttpServiceInput(url), results);
    }
    public static Status getData(HttpServiceInput input, File results) {
        try {
            return getData(input, defaultHandler(results));
        } catch (FileNotFoundException e) {
            return new Status(400, e.getMessage());
        }
    }

    public static Status getData(String url, OutputStream results) {
        return getData(new HttpServiceInput(url), results);
    }
    public static Status getData(HttpServiceInput input, OutputStream results) {
        return getData(input, defaultHandler(results));
    }

    /**
     * For convenience, this function will return 400-bad-request if url is malformed or any IO related exceptions.
     * Exceptions will be written into the results stream when possible.
     * if params are given as input, it will replace any queryString provided in the url.  So, use one of the other.
     * params given as input will be automatically encoded with UTF-8.
     * @param input  if params are given, it will replace any queryString provided in the url.  So, use one of the other.
     * @param handler  how to handle the response/results.  If null, do nothing.
     * @return Status
     */
    public static Status getData(HttpServiceInput input, Handler handler) {
        try {
            String url = input.getRequestUrl();
            return executeMethod(new GetMethod(url), input, handler);
        } catch (Exception e) {
            LOG.error(e);
            return new Status(400, e.getMessage());
        }
    }

    public static HttpServices.Status getWithAuth(String url, HttpServices.Handler handler) {
        return getWithAuth(new HttpServiceInput(url), 3, handler);
    }

    public static HttpServices.Status getWithAuth(HttpServiceInput input, HttpServices.Handler handler) {
        return getWithAuth(input, 3, handler);
    }

    /**
     * Similar to #getData(), but this function will handle credentials if necessary
     * and ensure that redirects are re-evaluated accordingly
     * @param input   request input
     * @param maxFollow  the maximum number of redirect to follow.  3 if using one of the overloaded functions.
     * @param handler   a handler to call upon successful fetch
     * @return the status of this fetch
     */
    public static HttpServices.Status getWithAuth(HttpServiceInput input, int maxFollow, HttpServices.Handler handler) {
        input.applyCredential()
                .setFollowRedirect(false);
        return HttpServices.getData(input, (method -> {
            try {
                if (HttpServices.isOk(method)) {
                    return handler.handleResponse(method);
                }
                if (HttpServices.isRedirected(method)) {
                    String location = HttpServices.getResHeader(method, "Location", null);
                    if (location != null) {
                        if (maxFollow > 0) {
                            return getWithAuth(new HttpServiceInput(location), maxFollow-1, handler);
                        } else {
                            return new HttpServices.Status(421, "Request redirected without a location header");
                        }
                    } else {
                        return new HttpServices.Status(421, "ERR_TOO_MANY_REDIRECTS");
                    }
                }
                return HttpServices.Status.getStatus(method);
            } catch (Exception e) {
                return new HttpServices.Status(500, "Error retrieving content from " + input.getRequestUrl() +": " + e.getMessage());
            }
        }));
    }

//====================================================================
//  POST convenience functions
//====================================================================
    public static Status postData(String url, File results) {
        return postData(new HttpServiceInput(url), results);
    }

    public static Status postData(HttpServiceInput input, File results) {
        try {
            return postData(input, defaultHandler(results));
        } catch (FileNotFoundException e) {
            return new Status(400, e.getMessage());
        }
    }

    public static Status postData(String url, OutputStream results) {
        return postData(new HttpServiceInput(url), results);
    }

    public static Status postData(HttpServiceInput input, OutputStream results) {
        return postData(input, defaultHandler(results));
    }

    public static Status postData(HttpServiceInput input) {
        return postData(input, (Handler) null);
    }

    public static Status postData(String url, Handler handler) {
        return postData(new HttpServiceInput(url), handler);
    }

    public static Status postData(HttpServiceInput input, Handler handler) {
        try {
            String url = input.getRequestUrl();
            if (isEmpty(url))  throw new FileNotFoundException("Missing URL parameter");
            input.setFollowRedirect(false);                                                 // post are not allowed to follow redirect
            return executeMethod(new PostMethod(url), input, handler);
        } catch (Exception e) {
            return new Status(400, e.getMessage());
        }
    }

//====================================================================
// low level functions
//====================================================================

    public static Status executeMethod(HttpMethod method) throws IOException {
        return executeMethod(method, null, (OutputStream) null);
    }

    public static Status executeMethod(HttpMethod method, HttpServiceInput input) throws IOException {
        return executeMethod(method, input, (OutputStream) null);
    }

    public static Status executeMethod(HttpMethod method, HttpServiceInput input, File results) throws IOException {
        return executeMethod(method, input, defaultHandler(results));
    }

    public static Status executeMethod(HttpMethod method, HttpServiceInput input, OutputStream results) throws IOException {
        return executeMethod(method, input, defaultHandler(results));
    }

    /**
     * Executes the given method with the given input.  If results is given,
     * @param method HTTPMethod
     * @param input object holding request headers and cookies
     * @param handler response handler
     * @return HttpMethod
     */
    public static Status executeMethod(HttpMethod method, HttpServiceInput input, Handler handler) throws IOException {
        Status status = null;
        try {
            input = input == null ? new HttpServiceInput() : input;

            method.setRequestHeader("Connection", "close");            // request server to NOT keep-alive.. we don't plan to reuse this connection.
            method.setRequestHeader("User-Agent", VersionUtil.getUserAgentString());
            method.setRequestHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            if (method instanceof GetMethod) {
                method.setFollowRedirects(input.isFollowRedirect());    // post are not allowed to follow redirect
            }

            HttpClient httpClient = newHttpClient();

            handleAuth(httpClient, method, input.getUserId(), input.getPasswd());

            handleCookies(method, input.getCookies());

            handleHeaders(method, input.getHeaders());

            handleParams(method, input.getParams(), input.getFiles());

            logRequestStart(method, input);

            httpClient.executeMethod(method);
            if (handler != null) {
                status = handler.handleResponse(method);
            }

            return status == null ? Status.getStatus(method) : status;
        } finally {
            logRequestEnd(method, input, status);

            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * Convenience method to be used by custom handlers to get the response body
     * of the HTTP method as an InputStream. Handles content encoding.
     * @param method HttpMethod
     * @return InputStream representation of the response body
     * @throws IOException on error
     */
    public static InputStream getResponseBodyAsStream(HttpMethod method) throws IOException {
        Header encoding = method.getResponseHeader("Content-Encoding");
        InputStream is = method.getResponseBodyAsStream();
        if (is != null && encoding != null && encoding.getValue().contains("gzip")) {
            return new GZIPInputStream(is);
        } else {
            return is;
        }
    }

    /**
     * Convenience method to be used by custom handlers to get the response body
     * of the HTTP method as a String. Handles content encoding.
     * @param method HttpMethod
     * @return String representation of the response body
     * @throws IOException on error
     */
    public static String getResponseBodyAsString(HttpMethod method) throws IOException {
        InputStream is = getResponseBodyAsStream(method);
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        } else {
            return method.getResponseBodyAsString();
        }
    }


//====================================================================
//  Util helper functions
//====================================================================

    public static Handler defaultHandler(File source) throws FileNotFoundException {
        return source == null ? null : new OutputStreamHandler(source);
    }

    public static Handler defaultHandler(OutputStream source) {
        return source == null ? null : new OutputStreamHandler(source);
    }

    public static class Status {
        private String errMsg;
        private int statusCode;

        public Status(int statusCode, String errMsg) {
            this.errMsg = errMsg;
            this.statusCode = statusCode;
        }

        public boolean isOk() { return statusCode >= 200 && statusCode < 300; }
        public boolean isRedirected() { return statusCode >= 300 && statusCode < 400; }
        public boolean isError() { return !isOk(); }
        public String getErrMsg() { return errMsg; }
        public int getStatusCode() { return statusCode;}

        public Exception getException() {
            return isError() ? new HttpException(this) : null;
        }

        public static Status getStatus(HttpMethod method) {
            return new Status(method.getStatusCode(), method.getStatusText());
        }
        public static Status ok() {return new Status(200, null);};

        static class HttpException extends Exception {
            public HttpException(Status status) {
                super(status.getStatusCode() + " - " + status.getErrMsg());
            }
        }
    }

    public static class OutputStreamHandler implements Handler {
        private OutputStream results;

        public OutputStreamHandler(File results) throws FileNotFoundException {
            this.results = new FileOutputStream(results);
        }

        public OutputStreamHandler(OutputStream results) {
            this.results = results;
        }

        public Status handleResponse(HttpMethod method) {
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                bis = new BufferedInputStream(getResponseBodyAsStream(method));
                bos = new BufferedOutputStream(results);
                int b;
                while ((b = bis.read()) != -1) {
                    bos.write(b);
                }
            } catch (IOException e) {
                return new Status(400, String.format("Error while reading response body: %s", e.getMessage()));
            } finally {
                FileUtil.silentClose(bis);
                FileUtil.silentClose(bos);
            }
            return new Status(method.getStatusCode(), method.getStatusText());
        }
    }

    public static boolean isOk(HttpMethod method) {
        int status = method.getStatusCode();
        return status >= 200 && status < 300;
    }

    public static boolean isRedirected(HttpMethod method) {
        int status = method.getStatusCode();
        return status >= 300 && status < 400;
    }

    public static String getReqHeader(HttpMethod method, String key, String def) {
        Header header = method.getRequestHeader(key);
        if (header == null || header.getValue() == null) return def;
        return header.getValue().trim();
    }

    public static String getResHeader(HttpMethod method, String key, String def) {
        Header header = method.getResponseHeader(key);
        if (header == null || header.getValue() == null) return def;
        return header.getValue().trim();
    }

//====================================================================
//  Private functions
//====================================================================

    private static void handleAuth(HttpClient client, HttpMethod method, String userId, String password) {
        if (!isEmpty(userId)) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userId, password);
            client.getState().setCredentials(AuthScope.ANY, credentials);
        } else {
            // check to see if the userId and password is in the url
            userId = URLDownload.getUserFromUrl(method.toString());
            if (userId != null) {
                password = URLDownload.getPasswordFromUrl(method.toString());
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userId, password);
                client.getState().setCredentials(AuthScope.ANY, credentials);
            }
        }
    }

    private static void handleCookies(HttpMethod method, Map<String,String> cookies) {
        if (cookies != null && cookies.size() > 0) {
            String cookieStr = cookies.entrySet().stream()
                    .map((e) -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(";"));
            method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
            method.setRequestHeader("Cookie", cookieStr);
        }
    }

    private static void handleHeaders(HttpMethod method, Map<String,String> headers) {
        if (headers != null) {
            headers.entrySet().stream()
                    .forEach( (e) -> method.setRequestHeader(e.getKey(), e.getValue()));
        }
    }

    private static void handleParams(HttpMethod method, Map<String,String> params, Map<String,File> files) throws FileNotFoundException {
        if (method instanceof PostMethod) {
            PostMethod postMethod = (PostMethod) method;
            if (files != null) {
                // this is a multipart request
                List<Part> parts = new ArrayList<>();
                if (params != null) {
                    for(String key : params.keySet()) {
                        parts.add(new StringPart(key, params.get(key)));
                    }
                }
                for(String key : files.keySet()) {
                    parts.add(new FilePart(key, files.get(key)));
                }
                postMethod.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[0]), postMethod.getParams()));

            } else {
                if (params != null) {
                    params.entrySet().stream()
                            .forEach( (e) -> postMethod.addParameter(e.getKey(), e.getValue()) );
                }
            }
        } else {
            if (isEmpty(method.getQueryString())) {
                if (params != null && params.size() > 0) {
                    List<NameValuePair> args = new ArrayList<>();
                    params.entrySet().stream()
                            .forEach( (e) -> args.add(new NameValuePair(e.getKey(), e.getValue())) );
                    method.setQueryString(args.toArray(new NameValuePair[0]));
                }
            }
        }
    }

    private static void logRequestStart(HttpMethod method, HttpServiceInput input) {
        try {
            LOG.info("HttpServices URL:" + method.getURI().toString());
            if (method.getClass().isAssignableFrom(PostMethod.class)) {
                if (input.getParams() != null) LOG.info("-->  POST params:" + input.getParams());
                if (input.getFiles() != null)  LOG.info("-->  POST files :" + input.getFiles());
            }
        } catch (Exception ignore){}
    }

    private static void logRequestEnd(HttpMethod method, HttpServiceInput input, Status status) {

        try {
            if (status == null) status = Status.getStatus(method);

            if(status.isOk()) {
                LOG.info("--> done URL: " + method.getURI().toString());
                LOG.trace("--> trace: ", getDetailDesc(method, input, status));
            } else if (status.isRedirected() && input.isFollowRedirect()) {
                LOG.error("--> Failed to follow redirect with status:" + status + "\n" + getDetailDesc(method, input, status));
            } else {
                LOG.error("--> Failed with status:" + status + "\n" + getDetailDesc(method, input, status));
            }
        } catch (Exception ignore){}
    }

    private static String getDetailDesc(HttpMethod method, HttpServiceInput input, Status status) {

        try {
            String desc = "\tmethod: "+method.getName() +
                    "\n\tstatus: " + status.getStatusCode() + "-" + status.getErrMsg() +
                    "\n\turl: " + method.getURI() +
                    input.getDesc() +
                    "\n\tREQUEST HEADERS: " + CollectionUtil.toString(method.getRequestHeaders()).replaceAll("\\r|\\n", "") +
                    "\n\tRESPONSE HEADERS: " + CollectionUtil.toString(method.getResponseHeaders()).replaceAll("\\r|\\n", "");

            final StringBuilder curl = new StringBuilder("curl -v");
            for(Header h : method.getRequestHeaders())  curl.append(String.format(" -H '%s'", h.toString().trim()));

            applyIfNotEmpty(input.getParams(),
                    (p) -> p.forEach((k,v) -> curl.append(String.format(" -F '%s=%s'", k,v))));
            applyIfNotEmpty(input.getFiles(),
                    (f) -> f.forEach((k,v) -> curl.append(String.format(" -F '%s=@%s'", k,v))));

            curl.append(String.format(" '%s'", input.getRequestUrl()));
            desc += "\n\tCURL CMD: " + curl;

            return desc;
        } catch (Exception e) {
            return "Details not available.  Exception occurs while trying to get the details.";
        }
    }



    public interface Handler {
        Status handleResponse(HttpMethod method);
    }
}
