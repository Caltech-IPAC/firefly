/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;

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
    private String requestUrl;      // the request url
    private String baseUrl;         // the url up to the the app's path
    private String hostUrl;         // the url up to the host name including port
    private String remoteIP;
    private String sessId;
    private String contextPath;
    private String servletPath;

    public RequestAgent() {}

    public RequestAgent(Map<String, Cookie> cookies, String hostUrl, String requestUrl, String baseUrl, String remoteIP, String sessId, String contextPath) {
        this.cookies = cookies;
        this.requestUrl = requestUrl;
        this.baseUrl = baseUrl;
        this.remoteIP = remoteIP;
        this.sessId = sessId;
        this.contextPath = contextPath;
    }

    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
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

    void setSessId(String sessId) {
        this.sessId = sessId;
    }

    public String getContextPath() { return contextPath; }
    void setContextPath(String contextPath) { this.contextPath = contextPath; };

    public String getHostUrl() { return hostUrl;}

    void setHostUrl(String hostUrl) { this.hostUrl = hostUrl;}

    public String getRequestUrl() {
        return requestUrl;
    }

    void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getBaseUrl() { return baseUrl; }

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
        private static final String AUTH_KEY = "JOSSO_SESSIONID";
        private static final Logger.LoggerImpl LOG = Logger.getLogger();
        private final HashMap<String, String> headers = new HashMap<>();      // key stored as lowercase;
        private final HashMap<String, Cookie> cookies = new HashMap<>();
        private final HttpServletResponse response;
        private final String realPath;



        public HTTP(HttpServletRequest request, HttpServletResponse response) {
            this.response = response;

            Collections.list(request.getHeaderNames()).forEach(h -> {
                headers.put(h.toLowerCase(), request.getHeader(h));
            });
            applyIfNotEmpty(request.getCookies(), v -> {
                Arrays.stream(v).forEach(c -> cookies.put(c.getName(), c));
            });

            // getting the base url including the application path is a bit tricky when behind reverse proxy(ies)
            String proto = getHeader("X-Forwarded-Proto", request.getScheme());
            String host  = getHeader("X-Forwarded-Host", getHeader("X-Forwarded-Server", request.getServerName()));
            String port  = getHeader("X-Forwarded-Port", String.valueOf(request.getServerPort()));
            port = port.matches("443|80") ? "" : ":" + port;
            String proxiedPath = getHeader("X-Forwarded-Path", getHeader("X-Forwarded-Prefix", "") + request.getContextPath());

            String hostUrl = String.format("%s://%s%s", proto, host, port);
            String baseUrl = hostUrl + proxiedPath;
            baseUrl = baseUrl.endsWith("/") ? baseUrl :  baseUrl + "/";

            String requestUrl =  getHeader("X-Original-URI");
            if (requestUrl == null) {
                String queryStr = request.getQueryString() == null ? "" : "?" + request.getQueryString();
                String path = request.getRequestURI();
                if (!proxiedPath.equals(request.getContextPath())) {
                    path = path.replace(request.getContextPath(), proxiedPath);
                }
                requestUrl = path + queryStr;
            }
            requestUrl = requestUrl.startsWith(proto) ? requestUrl : hostUrl + requestUrl;

            String remoteIP = getHeader("x-original-forwarded-for", getHeader("X-Forwarded-For", request.getRemoteAddr()));

            setBaseUrl(baseUrl);
            setHostUrl(hostUrl);
            setRequestUrl(requestUrl);
            setContextPath(request.getContextPath());
            setRemoteIP(remoteIP);
            setSessId(request.getSession(true).getId());
            setServletPath(request.getServletPath());

            realPath = request.getServletContext().getRealPath("/");
        }

        @Override
        protected Map<String, Cookie> extractCookies() {
            return cookies;
        }

        @Override
        public String getRealPath(String relPath) {
            return new File(realPath, relPath).getAbsolutePath();
        }

        @Override
        public void sendCookie(Cookie cookie) {
            if (response != null) {
                response.addCookie(cookie);
            }
        }

        @Override
        public String getHeader(String name, String def) {
            String retval = name == null ? null : headers.get(name.toLowerCase());
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
