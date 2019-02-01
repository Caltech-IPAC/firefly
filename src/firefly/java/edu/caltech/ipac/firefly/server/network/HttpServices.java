/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.*;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
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
     * @return
     */
    public static Status getData(String url, File results) {
        return getData(url, results, null);
    }

    public static Status getData(String url, OutputStream results) {
        return getData(url, results, null);
    }

    public static Status getData(String url, OutputStream results, HttpServiceInput input) {
        return getData(url, input, defaultHandler(results));
    }

    /**
     * For convenience, this function will return 400-bad-request if url is malformed or any IO related exceptions.
     */
    public static Status getData(String url, File results, HttpServiceInput input) {
        try {
            return getData(url, input, defaultHandler(results));
        } catch (FileNotFoundException e) {
            return new Status(400, e.getMessage());
        }
    }

    /**
     * For convenience, this function will return 400-bad-request if url is malformed or any IO related exceptions.
     * Exceptions will be written into the results stream when possible.
     * if params are given as input, it will replace any queryString provided in the url.  So, use one of the other.
     * params given as input will be automatically encoded with UTF-8.
     * @param url
     * @param input  if params are given, it will replace any queryString provided in the url.  So, use one of the other.
     * @param handler  how to handle the response/results.  If null, do nothing.
     * @return
     */
    public static Status getData(String url, HttpServiceInput input, Handler handler) {
        try {
            url = isEmpty(url) && input != null ? input.getRequestUrl() : url;
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
    public static Status postData(String url, File results, HttpServiceInput input) {
        try {
            return postData(url, input, defaultHandler(results));
        } catch (FileNotFoundException e) {
            return new Status(400, e.getMessage());
        }
    }

    public static Status postData(String url, OutputStream results, HttpServiceInput input) {
        return postData(url, input, defaultHandler(results));
    }

    public static Status postData(String url, HttpServiceInput input, Handler handler) {
        try {
            url = isEmpty(url) && input != null ? input.getRequestUrl() : url;
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
     * @param method
     * @param input
     * @param handler
     * @return
     */
    public static HttpMethod executeMethod(HttpMethod method, HttpServiceInput input, Handler handler) throws IOException {
        try {
            input = input == null ? new HttpServiceInput() : input;
            LOG.info("HttpServices URL:" + method.getURI().toString());

            method.setRequestHeader("User-Agent", USER_AGENT);
            method.setRequestHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            if (method instanceof GetMethod) {
                method.setFollowRedirects(true);    // post are not allowed to follow redirect
            }
            HttpClient httpClient = newHttpClient();

            handleAuth(httpClient, method, input.getUserId(), input.getPasswd());

            handleCookies(method, input.getCookies());

            handleHeaders(method, input.getHeaders());

            handleParams(method, input.getParams(), input.getFiles());

            int status = httpClient.executeMethod(method);
            if (status < 200 || status >= 300) {
                // logs bad requests
                LOG.error("HTTP request failed with status:" + status + "\n" + getDetailDesc(method, input));
            }

            if (handler != null) {
                handler.handleResponse(method);
            }

            return method;

        } finally {
            if (method != null) {
                method.releaseConnection();
            }
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
                String encoding = getResHeader(method, "Content-Encoding");
                if (encoding.contains("gzip")) {
                    bis = new BufferedInputStream(new GZIPInputStream(method.getResponseBodyAsStream()));
                } else {
                    bis = new BufferedInputStream(method.getResponseBodyAsStream());
                }

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

    private static String getResHeader(HttpMethod method, String key) {
        Header header = method.getResponseHeader(key);
        return header == null ? "" : header.getValue();
    }

    private static String getDetailDesc(HttpMethod method, HttpServiceInput input) throws URIException {
        String desc = "url: " + method.getURI() +
                "\nquery_str: " + method.getQueryString() +
                "\ninput: " + (input== null ? "" : input.getDesc()) +
                "\nREQUEST HEADERS: " + CollectionUtil.toString(method.getRequestHeaders()).replaceAll("\\r|\\n", "") +
                "\nRESPONSE HEADERS: " + CollectionUtil.toString(method.getResponseHeaders()).replaceAll("\\r|\\n", "");

        return desc;
    }



    public interface Handler {
        void handleResponse(HttpMethod method);
    }
}
