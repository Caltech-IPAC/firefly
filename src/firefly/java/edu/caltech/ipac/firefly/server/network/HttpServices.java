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
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.apache.commons.httpclient.params.HttpMethodParams.USER_AGENT;


/**
 * Date: 6/13/14
 *
 * @author loi
 * @version $Id: $
 */
public class HttpServices {
    public static final int BUFFER_SIZE = FileUtil.BUFFER_SIZE;    // 64k
    private static HttpClient httpClient;
    private static final Logger.LoggerImpl LOG = Logger.getLogger();

    static {
        HostConfiguration hostConfig = new HostConfiguration();
        try {
            hostConfig.setHost(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {e.printStackTrace();}
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager(){
            public void releaseConnection(HttpConnection conn) {
                try {
                    if (conn != null) conn.close();
                } catch (Exception ex) {/* do nothing */}
                super.releaseConnection(conn);
            }
        };
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxConnectionsPerHost(hostConfig, 5);
        params.setStaleCheckingEnabled(true);
        params.setConnectionTimeout(5000);
        params.setSoTimeout(0);   // this is the default.. but, setting it explicitly to be sure
        connectionManager.setParams(params);
        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfig);
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
    public static int getData(String url, File results) {
        return getData(url, results);
    }

    public static int getData(String url, OutputStream results) {
        return getData(url, results, null);
    }

    public static int getData(String url, File results, HttpServiceInput input) {
        try {
            OutputStream os = results == null ? null : new FileOutputStream(results);
            return getData(url, os, input);
        } catch (FileNotFoundException e) {
            return 400;
        }
    }

    /**
     * For convenience, this function will return 400-bad-request if url is malformed or any IO related exceptions.
     * Exceptions will be written into the results stream when possible.
     * if params are given as input, it will replace any queryString provided in the url.  So, use one of the other.
     * params given as input will be automatically encoded with UTF-8.
     * @param url
     * @param results
     * @param input  if params are given, it will replace any queryString provided in the url.  So, use one of the other.
     * @return
     */
    public static int getData(String url, OutputStream results, HttpServiceInput input) {
        try {
            return executeMethod(new GetMethod(url), input, results);
        } catch (IOException e) {
            LOG.error(e);
            try {
                FileUtil.writeStringToStream(e.getMessage(), results);
            } catch (Exception ne) {};    // do nothing
            return 400;
        }
    }

//====================================================================
//  POST convenience functions
//====================================================================
    public static int postData(String url, File results, HttpServiceInput input) {
        try {
            OutputStream os = results == null ? null : new FileOutputStream(results);
            return postData(url, os, input);
        } catch (FileNotFoundException e) {
            return 400;
        }
    }

    public static int postData(String url, OutputStream results, HttpServiceInput input) {
        try {
            return executeMethod(new PostMethod(url), input, results);
        } catch (IOException e) {
            try {
                FileUtil.writeStringToStream(e.getMessage(), results);
            } catch (Exception ne) {};    // do nothing
            return 400;
        }
    }

//====================================================================
// low level functions
//====================================================================

    public static boolean executeMethod(HttpMethod method) {
        return executeMethod(method, (String) null, null);
    }

    public static boolean executeMethod(HttpMethod method, String userId, String password) {
        return executeMethod(method, userId, password, null);
    }

    /**
     * Execute the given HTTP method with the given parameters.
     * @param method    the function or method to perform
     * @param cookies   optional, sent with request if present.
     * @return  true is the request was successfully received, understood, and accepted (code 2xx).
     */
    @Deprecated
    public static boolean executeMethod(HttpMethod method, String userId, String password, Map<String, String> cookies) {
        try {

            LOG.info("HttpServices URL:" + method.toString());

            handleAuth(httpClient, method, userId, password);

            handleCookies(method, cookies);

            int status = httpClient.executeMethod(method);
            boolean isSuccess =  status >= 200 && status < 300;
            if (!isSuccess) {
                LOG.error("HTTP request failed with status:" + status + "\n" + getDetailDesc(method, null));
            }
            return isSuccess;
        } catch (Exception e) {
            LOG.error(e, "Unable to connect to:" + method.toString());
        }
        return false;
    }

    /**
     * Executes the given method with the given input.  If results is given,
     * @param method
     * @param input
     * @param results
     * @return
     * @throws IOException
     */
    public static int executeMethod(HttpMethod method, HttpServiceInput input, OutputStream results) throws IOException {
        try {
            input = input == null ? new HttpServiceInput() : input;
            LOG.info("HttpServices URL:" + method.getURI().toString());

            method.setRequestHeader("User-Agent", USER_AGENT);
            method.setRequestHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            if (method instanceof GetMethod) {
                method.setFollowRedirects(true);    // post are not allowed to follow redirect
            }

            handleAuth(httpClient, method, input.getUserId(), input.getPasswd());

            handleCookies(method, input.getCookies());

            handleHeaders(method, input.getHeaders());

            handleParams(method, input.getParams(), input.getFiles());

            int status = httpClient.executeMethod(method);

            handleResults(method, results);

            boolean isSuccess =  status >= 200 && status < 300;
            if (!isSuccess) {
                LOG.error("HTTP request failed with status:" + status + "\n" + getDetailDesc(method, input));
            }
            return status;
        } finally {
            if (results != null) {
                method.releaseConnection();
            }
        }
    }





//====================================================================
//  Util helper functions
//====================================================================

    public static void handleResults(HttpMethod method, OutputStream results) {
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

//====================================================================
//  Private functions
//====================================================================

    private static void handleAuth(HttpClient client, HttpMethod method, String userId, String password) {
        if (!StringUtils.isEmpty(userId)) {
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
            if (StringUtils.isEmpty(method.getQueryString())) {
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


}
