/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.network;

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
import org.apache.http.HttpHeaders;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static org.apache.commons.httpclient.params.HttpMethodParams.USER_AGENT;


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
            HttpMethod method = executeMethod(new GetMethod(url), input, handler);
            return Status.getStatus(method);
        } catch (IOException e) {
            LOG.error(e);
            return new Status(400, e.getMessage());
        }
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
            HttpMethod method = executeMethod(new PostMethod(url), input, handler);
            return Status.getStatus(method);
        } catch (IOException e) {
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
        executeMethod(method, input, defaultHandler(results));
        return Status.getStatus(method);
    }

    public static Status executeMethod(HttpMethod method, HttpServiceInput input, OutputStream results) throws IOException {
        executeMethod(method, input, defaultHandler(results));
        return Status.getStatus(method);
    }

    /**
     * Executes the given method with the given input.  If results is given,
     * @param method HTTPMethod
     * @param input object holding request headers and cookies
     * @param handler response handler
     * @return HttpMethod
     */
    public static HttpMethod executeMethod(HttpMethod method, HttpServiceInput input, Handler handler) throws IOException {
        try {
            input = input == null ? new HttpServiceInput() : input;
            LOG.info("HttpServices URL:" + method.getURI().toString());

            method.setRequestHeader("Connection", "close");            // request server to NOT keep-alive.. we don't plan to reuse this connection.
            method.setRequestHeader("User-Agent", USER_AGENT);
            method.setRequestHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            if (method instanceof GetMethod) {
                method.setFollowRedirects(input.isFollowRedirect());    // post are not allowed to follow redirect
            }
            HttpClient httpClient = newHttpClient();

            handleAuth(httpClient, method, input.getUserId(), input.getPasswd());

            handleCookies(method, input.getCookies());

            handleHeaders(method, input.getHeaders());

            handleParams(method, input.getParams(), input.getFiles());

            int status = httpClient.executeMethod(method);
            if ( ( !isOk(method) || (isRedirected(method) && input.isFollowRedirect())) ) {
                // logs bad requests
                LOG.error("HTTP request failed with status:" + status + "\n" + getDetailDesc(method, input));
            }

            if (handler != null) {
                handler.handleResponse(method);
            }

            return method;

        } finally {
            LOG.trace("--> HttpServices", getDetailDesc(method, input));

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

        public boolean isError() { return statusCode < 200 || statusCode >= 300; }
        public String getErrMsg() { return errMsg; }
        public int getStatusCode() { return statusCode;}

        public static Status getStatus(HttpMethod method) {
            return new Status(method.getStatusCode(), method.getStatusText());
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

        public void handleResponse(HttpMethod method) {
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
                LOG.error(e, "Error while reading response body");
            } finally {
                FileUtil.silentClose(bis);
                FileUtil.silentClose(bos);
            }
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

    private static String getDetailDesc(HttpMethod method, HttpServiceInput input) {

        try {
            String desc = "\tmethod: "+method.getName() +
                    "\n\tstatus: "+method.getStatusText()+
                    "\n\turl: " + method.getURI() +
                    input.getDesc() +
                    "\n\tREQUEST HEADERS: " + CollectionUtil.toString(method.getRequestHeaders()).replaceAll("\\r|\\n", "") +
                    "\n\tRESPONSE HEADERS: " + CollectionUtil.toString(method.getResponseHeaders()).replaceAll("\\r|\\n", "");

            if (method.getName().equals("GET")) {
                String curl = "curl -v";
                for(Header h : method.getRequestHeaders())  curl += " -H '" + h.toString().trim() + "'";
                curl += " '" + method.getURI() + "'";
                desc += "\n\tCURL CMD: " + curl;
            }
            return desc;
        } catch (Exception e) {
            return "Details not available.  Exception occurs while trying to get the details.";
        }
    }



    public interface Handler {
        void handleResponse(HttpMethod method);
    }
}
