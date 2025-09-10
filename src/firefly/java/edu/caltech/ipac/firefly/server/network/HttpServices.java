/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.network;

import com.google.common.net.HttpHeaders;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.KeyVal;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
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
import java.util.function.BiFunction;
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
    public static final int BUFFER_SIZE = (int) (8*FileUtil.K);    // optimal buffer size
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    /* Takes the header name and value, and returns a sanitized value if needed. */
    private static final List<BiFunction<String, String, String>> headerSanitizers = List.of(
            (k, v) -> v.replaceAll("[\\r\\n]", ""),        // remove any new line characters from value
            new AuthHeader()
    );

    private static HttpClient newHttpClient() {
        HttpClient httpClient = new HttpClient();
        HttpConnectionManagerParams params = httpClient.getHttpConnectionManager().getParams();
        params.setConnectionTimeout(5000);
        params.setSoTimeout(0);     // this is the default. but, setting it explicitly to be sure
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
    public static Status getData(String url, Handler handler) {
        return getData(new HttpServiceInput(url), handler);
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

    public static Status getData(HttpServiceInput input, Handler handler) {
        return getData(input, 3, handler);
    }

    /**
     * Convenience function with the following behavior:
     * - Returns HTTP 400 (Bad Request) if the URL is malformed or an I/O error occurs.
     * - When possible, exception details are returned in Status's error message with error code.
     * - If `params` are provided, they replace any query string in the URL (use one or the other).
     * - Input parameters are automatically UTF-8 encoded.
     * - Handles redirects and re-evaluates credentials as needed.
     * @param input  if params are given, it will replace any queryString provided in the url.  So, use one of the other.
     * @param maxFollow  if followRedirect is true, the maximum number of redirect to follow.
     * @param handler  how to handle the response/results.  If null, do nothing.
     * @return Status
     */
    public static Status getData(HttpServiceInput input, int maxFollow, Handler handler ) {
        try {
            String url = input.getRequestUrl();
            if (!input.isFollowRedirect()) {
                return executeMethod(new GetMethod(url), input, handler);
            }
            // Need to manually handle redirect to ensure credentials are applied properly
            input.setFollowRedirect(false);
            return executeMethod(new GetMethod(url), input, new RedirectHandler(input, handler, maxFollow));
        } catch (Exception e) {
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
        return postData(input, 3, handler);
    }

    public static Status postData(HttpServiceInput input, int maxFollow, Handler handler) {
        try {
            String url = input.getRequestUrl();
            if (isEmpty(url))  throw new FileNotFoundException("Missing URL parameter");
            if (!input.isFollowRedirect()) {
                return executeMethod(new PostMethod(url), input, handler);
            }
            // Need to manually handle redirect to ensure credentials are applied properly
            input.setFollowRedirect(false);                 // httpclient 3.x, post are not allowed to follow redirect; but, we will do it manually.
            return executeMethod(new PostMethod(url), input, new RedirectHandler(input, handler, maxFollow));
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

            method.setRequestHeader("Connection", "close");            // request server to NOT keep-alive. we don't plan to reuse this connection.
            method.setRequestHeader("User-Agent", VersionUtil.getUserAgentString());
            method.setRequestHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            if (method instanceof GetMethod) {
                method.setFollowRedirects(input.isFollowRedirect());    // httpclient 3.x, post are not allowed to follow redirect
            }

            HttpClient httpClient = newHttpClient();

            handleAuth(httpClient, method, input.getUserId(), input.getPasswd());

            handleCookies(method, input.getCookies());

            handleHeaders(method, input.getHeaders());

            handleParams(method, input.getParamPairs(), input.getFiles());

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
            try (InputStream in = getResponseBodyAsStream(method);
                 OutputStream out = results) {

                byte[] buffer = new byte[BUFFER_SIZE]; // 8 KB buffer
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                return new Status(400, String.format("Error while reading response body: %s", e.getMessage()));
            }
            return new Status(method.getStatusCode(), method.getStatusText());
        }
    }


    /**
     * Handles redirects by following the Location header up to maxFollow times.
     * It will also re-apply credentials as needed.
     * For safety, it will change Method to GET when following redirect.  In the future when necessary, it will check for 307 & 308 status to keep the same method.
     */
    public static class RedirectHandler implements Handler {
        private final HttpServiceInput input;
        private final Handler handler;
        private final int maxFollow;

        public RedirectHandler(HttpServiceInput input, Handler handler, int maxFollow) {
            this.input = input;
            this.handler = handler;
            this.maxFollow = maxFollow;
        }

        public Status handleResponse(HttpMethod method) {
            try {
                if (HttpServices.isOk(method)) {
                    return handler.handleResponse(method);
                }
                if (HttpServices.isRedirected(method)) {
                    String location = HttpServices.getResHeader(method, "Location", null);
                    if (location != null) {
                        if (maxFollow > 0) {
                            return getData(new HttpServiceInput(location), maxFollow-1, handler);       // follow redirect is default to true.
                        } else {
                            return new Status(421, "ERR_TOO_MANY_REDIRECTS");
                        }
                    } else {
                        return new Status(421, "Request redirected without a location header");
                    }
                }
                return Status.getStatus(method);
            } catch (Exception e) {
                return new Status(500, "Error retrieving content from " + input.getRequestUrl() +": " + e.getMessage());
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

    private static void handleParams(HttpMethod method, List<KeyVal<String, String>> params, Map<String,File> files) throws FileNotFoundException {
        if (method instanceof PostMethod) {
            PostMethod postMethod = (PostMethod) method;
            if (files != null) {
                // this is a multipart request
                List<Part> parts = new ArrayList<>();
                if (params != null) {
                    params.forEach((p) -> parts.add(new StringPart(p.getKey(), p.getValue())));
                }
                for(String key : files.keySet()) {
                    parts.add(new FilePart(key, files.get(key)));
                }
                postMethod.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[0]), postMethod.getParams()));

            } else {
                if (params != null) {
                    params.forEach((p) -> postMethod.addParameter(p.getKey(), p.getValue()));
                }
            }
        } else {
            if (isEmpty(method.getQueryString())) {
                if (params != null && !params.isEmpty()) {
                    NameValuePair[] args = params.stream()
                            .map((p) -> new NameValuePair(p.getKey(), p.getValue()))
                            .toArray(NameValuePair[]::new);
                    method.setQueryString(args);
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
            } else if (status.isRedirected()) {
                if (input.isFollowRedirect()) {
                    LOG.error("--> Failed to follow redirect with status:" + status + "\n" + getDetailDesc(method, input, status));
                }
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
                    "\n\tREQUEST HEADERS: " + sanitizeHeaders(method.getRequestHeaders()) +
                    "\n\tRESPONSE HEADERS: " + sanitizeHeaders(method.getResponseHeaders());

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

    /**
     * Sanitize the header value for logging purposes.
     * @param name      name of the header
     * @param value     value of the header
     * @return  the sanitized value of the header
     */
    public static String sanitizeHeader(String name, String value) {
        String rval = value;
        for (BiFunction<String, String, String> sanitizer : headerSanitizers) {
            rval = sanitizer.apply(name, rval);
        }
        return rval;
    }

    public static String sanitizeHeaders(Header[] headers) {
        if (headers == null || headers.length == 0) return "";
        return Arrays.stream(headers)
                .map(h -> "%s: %s".formatted(h.getName(), sanitizeHeader(h.getName(), h.getValue())))
                .collect(Collectors.joining(", "));
    }

    private static class AuthHeader implements BiFunction<String, String, String> {
        // // Authorization: <scheme> <credentials>
        public String apply(String key, String value) {
            if (key == null || value == null) return value;
            if (key.equalsIgnoreCase("Authorization")) {
                int sep = value.indexOf(' ');
                if (sep > 0) {
                    String scheme = value.substring(0, sep);
                    return scheme + " [redacted]";
                }
            }
            return value;
        }
    }

    public interface Handler {
        Status handleResponse(HttpMethod method);
    }
}
