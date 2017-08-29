/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.security.JOSSOAdapter;
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

    private Map<String, String> cookies;
    private String protocol;
    private String requestUrl;
    private String baseUrl;
    private String remoteIP;

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Map<String, String> getCookies() {
        if (cookies == null) {
            cookies = extractCookies();
        }
        return cookies;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    public void sendCookie(Cookie cookie) {}

    public String getCookie(String name) {
        return getCookies().get(name);
    }

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

    protected Map<String, String> extractCookies() {
        return new HashMap<String, String>(0);
    }


    //====================================================================
    //  Authentication section
    //====================================================================
    public String getAuthKey() { return null; }
    public String getAuthToken() { return null;}
    public UserInfo getUserInfo() { return null; }
    public void clearAuthInfo() {}
    public Map<String, String> getIdentities() { return null; }
    public void updateAuthInfo(String authToken) {}


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
            String scheme = getHeader("X-Forwarded-Proto", request.getHeader("Referer"));
            scheme = StringUtils.isEmpty(scheme) ? request.getScheme().toLowerCase() : scheme;
            scheme = scheme.toLowerCase().startsWith("https") ? "https" : "http";

            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String serverPortDesc = serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort;

            String baseUrl = String.format("%s://%s%s%s/", scheme, serverName, serverPortDesc, request.getContextPath());
            String requestUrl = String.format("%s://%s%s%s", scheme, serverName, serverPortDesc, request.getRequestURI());

            setRemoteIP(remoteIP);
            setProtocol(scheme);
            setRequestUrl(requestUrl);
            setBaseUrl(baseUrl);
        }

        @Override
        protected Map<String, String> extractCookies() {
            HashMap<String, String> cookies = new HashMap<String, String>();
            if (request != null) {
                if (request.getCookies() != null) {
                    for (javax.servlet.http.Cookie c : request.getCookies()) {
                        cookies.put(c.getName(), c.getValue());
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

        @Override
        public Map<String, String> getIdentities() {
            HashMap<String, String> idCookies = new HashMap<String, String>();
            for (String name : ID_COOKIE_NAMES) {
                String value = getCookie(name);
                if (!StringUtils.isEmpty(value)) {
                    idCookies.put(name, value);
                }
            }
            return idCookies.size() == 0 ? null : idCookies;
        }

        @Override
        public UserInfo getUserInfo() {
            String authToken = getAuthToken();
            return StringUtils.isEmpty(authToken) ? null :
                    JOSSOAdapter.getUserInfo(authToken);
        }

        @Override
        public String getAuthToken() {
            return getCookie(AUTH_KEY);
        }

        @Override
        public void updateAuthInfo(String authToken) {
            Cookie c = new Cookie(AUTH_KEY, authToken);
            c.setMaxAge(authToken == null ? 0 : 60 * 60 * 24 * 14);
            c.setValue(authToken);
            c.setPath("/");
            sendCookie(c);

        }

        @Override
        public void clearAuthInfo() {
            if (getAuthToken() != null) {
                Cookie c = new Cookie(AUTH_KEY, "");
                c.setMaxAge(0);
                c.setValue(TO_BE_DELETE);
                c.setDomain(".ipac.caltech.edu");
                c.setPath("/");
                sendCookie(c);
            }
        }
    }
}
