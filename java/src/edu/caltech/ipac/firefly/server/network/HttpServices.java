package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Date: 6/13/14
 * this class does NOT release the connection.  it is the responsible of the caller to release
 * it when the response is completely consumed.
 *
 * @author loi
 * @version $Id: $
 */
public class HttpServices {
    public static final int BUFFER_SIZE = 4096 * 16;    // 64k
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

    public static boolean executeMethod(HttpMethod method) {
        return executeMethod(method, null, null);
    }

    public static boolean executeMethod(HttpMethod method, Map<String, String> cookies) {
        return executeMethod(method, null, null, cookies);
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
    public static boolean executeMethod(HttpMethod method, String userId, String password, Map<String, String> cookies) {
        try {
            if (!StringUtils.isEmpty(userId)) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userId, password);
                httpClient.getState().setCredentials(AuthScope.ANY, credentials);
            } else {
                // check to see if the userId and password is in the url
                userId = URLDownload.getUserFromUrl(method.toString());
                if (userId != null) {
                    password = URLDownload.getPasswordFromUrl(method.toString());
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userId, password);
                    httpClient.getState().setCredentials(AuthScope.ANY, credentials);
                }
            }

            if (cookies != null) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : cookies.entrySet()) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                }
                if (sb.length() > 0) {
                    method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
                    method.setRequestHeader("Cookie", sb.toString());
                }
            }

            int status = httpClient.executeMethod(method);
            boolean isSuccess =  status >= 200 && status < 300;
            String reqDesc = "URL:" + method.getURI();
            if (isSuccess) {
                LOG.info(reqDesc);
            } else {
                reqDesc = reqDesc +
                        "\nREQUEST HEADERS: " + CollectionUtil.toString(method.getRequestHeaders()).replaceAll("\\r|\\n", "") +
                        "\nPARAMETERS:\n " + getDesc(method.getParams()) +
                        "\nRESPONSE HEADERS: " + CollectionUtil.toString(method.getResponseHeaders()).replaceAll("\\r|\\n", "");

                LOG.error("HTTP request failed with status:" + status + "\n" + reqDesc);
            }
            return isSuccess;
        } catch (Exception e) {
            LOG.error(e, "Unable to connect to:" + method.toString());
        }
        return false;
    }

    private static String getDesc(HttpMethodParams params) {
        return null;
    }


}
