/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 4/20/15
 * This class acts as an agent for the underlaying request.  A request can initiate from different sources.
 * In this case, it can be HTTP, WS, or POJO.
 *
 * It also handle authentication related functions
 *
 * @author loi
 * @version $Id: $
 */
public class RequestAgent {

    private Map<String, Cookie> cookies;
    private String protocol;
    private String requestUrl;
    private String baseUrl;
    private String remoteIP;
    private String sessId;

    public RequestAgent() {}

    public RequestAgent(Map<String, Cookie> cookies, String protocol, String requestUrl, String baseUrl, String remoteIP, String sessId) {
        this.cookies = cookies;
        this.protocol = protocol;
        this.requestUrl = requestUrl;
        this.baseUrl = baseUrl;
        this.remoteIP = remoteIP;
        this.sessId = sessId;
    }

    public void setCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    public Map<String, Cookie> getCookies() {
        if (cookies == null) {
            cookies = extractCookies();
        }
        return cookies;
    }

    public String getSessId() {
        return sessId;
    }

    public void setSessId(String sessId) {
        this.sessId = sessId;
    }

    public String getProtocol() {
        return protocol;
    }

    void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public Cookie getCookie(String name) { return getCookies().get(name);}

    public String getCookieVal(String name) { return getCookieVal(name, null); }

    public String getCookieVal(String name, String def) {
        Cookie c = getCookie(name);
        String val = c == null ? def : c.getValue();
        return val == null ? def : val;
    }

    public void sendCookie(Cookie cookie) {}

    public String getRealPath(String relPath) {
        return null;
    }

    public String getHeader(String name) {
        return getHeader(name, null);
    }

    public String getHeader(String name, String def) {
        return null;
    }

    public void sendRedirect(String url) {}

    protected Map<String, Cookie> extractCookies() {
        return new HashMap<>(0);
    }


    //====================================================================
    //  Authentication section
    //====================================================================
    public String getAuthKey() { return null; }


//====================================================================
//  RequestAgent implementations...
//====================================================================

    public static class HTTP extends RequestAgent {
        public static String AUTH_KEY = "JOSSO_SESSIONID";
        public static String TO_BE_DELETE = "-";
        private static final Logger.LoggerImpl LOG = Logger.getLogger();
        private HttpServletRequest request;
        private HttpServletResponse response;
        private static final String[] ID_COOKIE_NAMES = new String[]{AUTH_KEY, "ISIS"};

        public HTTP(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;

            String remoteIP = getHeader("X-Forwarded-For", request.getRemoteAddr());
            String scheme = getHeader("X-Forwarded-Proto", request.getScheme());
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String serverPortDesc = serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort;

            String baseUrl = String.format("%s://%s%s%s/", scheme, serverName, serverPortDesc, request.getContextPath());
            String requestUrl = String.format("%s://%s%s%s", scheme, serverName, serverPortDesc, request.getRequestURI());

            setRemoteIP(remoteIP);
            setProtocol(scheme);
            setRequestUrl(requestUrl);
            setBaseUrl(baseUrl);
            setSessId(request.getSession(true).getId());
        }

        @Override
        protected Map<String, Cookie> extractCookies() {
            HashMap<String, Cookie> cookies = new HashMap<>();
            if (request != null) {
                if (request.getCookies() != null) {
                    for (javax.servlet.http.Cookie c : request.getCookies()) {
                        cookies.put(c.getName(), c);
                    }
                }
            }
            return cookies;
        }

        @Override
        public String getRealPath(String relPath) {
            return response != null ? request.getRealPath(relPath) : null;
        }

        @Override
        public void sendCookie(Cookie cookie) {
            if (response != null) {
                response.addCookie(cookie);
            }
        }

        @Override
        public String getHeader(String name, String def) {
            String retval = request != null ? request.getHeader(name) : null;
            return StringUtils.isEmpty(retval) ? def : retval;
        }

        @Override
        public void sendRedirect(String url) {
            try {
                response.sendRedirect(url);
            } catch (IOException e) {
                LOG.error(e, "Unable to redirect to:" + url);
            }
        }

        //====================================================================
        //  Authentication section
        //====================================================================

        @Override
        public String getAuthKey() {
            return AUTH_KEY;
        }

    }
}
