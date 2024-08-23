/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.security.JOSSOAdapter;
import edu.caltech.ipac.util.FileUtil;
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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
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

    public static int getDataViaUrl(URL url, File results) throws FileNotFoundException {
        return getDataViaUrl(url, new BufferedOutputStream(new FileOutputStream(results), FileUtil.BUFFER_SIZE));
    }

    public static int getDataViaUrl(URL url, OutputStream results) {
        GetMethod getter = null;
        try {
            getter = new GetMethod(url.toURI().toString());
            executeMethod(getter);
            readBody(results, getter.getResponseBodyAsStream());
            return getter.getStatusLine().getStatusCode();
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            if (getter != null) {
                getter.releaseConnection();
            }
        }
        return 500;
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
        String url = method.toString();
        try {
            if (!StringUtils.isEmpty(userId)) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userId, password);
                httpClient.getState().setCredentials(AuthScope.ANY, credentials);
            } else {
                // check to see if the userId and password is in the url
                userId = URLDownload.getUserFromUrl(url);
                if (userId != null) {
                    password = URLDownload.getPasswordFromUrl(url);
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userId, password);
                    httpClient.getState().setCredentials(AuthScope.ANY, credentials);
                }
            }

            // quick and dirty fix to pass along credential to IRSA backend services
            if (JOSSOAdapter.requireAuthCredential(url)) {
                String auth = ServerContext.getRequestOwner().getRequestAgent().getHeader("Authorization");
                if (auth != null && auth.startsWith("Basic")) {
                    method.setRequestHeader("Authorization", auth);
                }
                Map<String, String> ids = ServerContext.getRequestOwner().getIdentityCookies();
                if (ids != null) {
                    if (cookies == null) {
                        cookies = ids;
                    } else {
                        cookies.putAll(ids);
                    }
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
            LOG.error(e, "Unable to connect to:" + url);
        }
        return false;
    }

    private static String getDesc(HttpMethodParams params) {
        return null;
    }


    static void readBody(OutputStream os, InputStream body) {
        BufferedInputStream bis = new BufferedInputStream(body);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        try {
            int b;
            while ((b = bis.read()) != -1) {
                bos.write(b);
            }

            bos.flush();

        } catch (IOException e) {
            LOG.error(e, "Error while reading response body");
        }
    }

}
