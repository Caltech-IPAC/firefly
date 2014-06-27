package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Date: 6/13/14
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
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxConnectionsPerHost(hostConfig, 50);
        params.setConnectionTimeout(5000);
        params.setSoTimeout(0);   // this is the default.. but, setting it explicitly to be sure
        connectionManager.setParams(params);
        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfig);
    }

    public static void init() {}

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
            return status >= 200 && status < 300;
        } catch (Exception e) {
            LOG.error(e, "Unable to connect to:" + method.toString());
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return false;
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
