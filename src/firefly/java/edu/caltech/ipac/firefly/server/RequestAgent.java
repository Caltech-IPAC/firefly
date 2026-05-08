/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    public static final String SERVER_HOST = AppProperties.getProperty("server.host");

    private Map<String, Cookie> cookies;
    private String requestUrl;      // the request url
    private String baseUrl;         // the url up to the app's path
    private String hostUrl;         // the url up to the host name including port
    private String host;
    private String remoteIP;
    private String sessId;
    private String contextPath;
    private String servletPath;

    public RequestAgent() {}

    public RequestAgent(Map<String, Cookie> cookies, String host, String requestUrl, String baseUrl, String remoteIP, String sessId, String contextPath) {
        this.cookies = cookies;
        this.requestUrl = requestUrl;
        this.baseUrl = baseUrl;
        this.remoteIP = remoteIP;
        this.sessId = sessId;
        this.contextPath = contextPath;
        this.host = host;
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

    public String getHost() { return host;}

    void setHost(String host) { this.host = host;}

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

    public static final class HTTP extends RequestAgent {
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

            OriginInfo origin = resolveOrigin(request);

            String hostUrl = String.format("%s://%s%s", origin.proto, origin.host, origin.port);
            String baseUrl = hostUrl + origin.path;
            baseUrl = baseUrl.endsWith("/") ? baseUrl :  baseUrl + "/";

            String requestUrl = getHeader("X-Original-URI");
            if (requestUrl == null) {
                String queryStr = request.getQueryString() == null ? "" : "?" + request.getQueryString();
                String path = request.getRequestURI().replace(request.getContextPath(), origin.path);
                requestUrl = path + queryStr;
            }
            requestUrl = requestUrl.startsWith(origin.proto + "://") ? requestUrl : hostUrl + requestUrl;

            setBaseUrl(baseUrl);
            setHostUrl(hostUrl);
            setHost(origin.host);
            setRequestUrl(requestUrl);
            setContextPath(request.getContextPath());
            setRemoteIP(origin.remoteIP());
            setSessId(request.getSession(true).getId());
            setServletPath(request.getServletPath());

            realPath = request.getServletContext().getRealPath("/");
        }

        private record OriginInfo(String host, String port, String proto, String path, String remoteIP) {}

        // Trusts the 'server.host' property/env over request headers, which can be spoofed.
        private OriginInfo resolveOrigin(HttpServletRequest request) {
            String raw = !StringUtils.isEmpty(SERVER_HOST) ? SERVER_HOST
                    : getHeader("X-Forwarded-Host", getHeader("X-Forwarded-Server", getHeader("host", request.getServerName())));
            // X-Forwarded-Host may be a comma-separated list when behind multiple proxies; take the first entry
            raw = raw.split(",")[0].trim();
            // host may contain a port, e.g. hostname:8080
            String[] parts = raw.split(":");
            String host = parts.length == 2 && parts[1].matches("\\d+") ? parts[0] : raw;
            String port  = getHeader("X-Forwarded-Port", parts.length == 2 ? parts[1] : String.valueOf(request.getServerPort()));
            String proto = getHeader("X-Forwarded-Proto", port.equals("443") ? "https" : "http");
            port = port.matches("443|80") ? "" : ":" + port;
            String path = getHeader("X-Forwarded-Path", getHeader("X-Forwarded-Prefix", "") + request.getContextPath());
            // X-Forwarded-For is a comma-separated list of IPs; the leftmost is the original client
            String remoteIP = getHeader("x-original-forwarded-for", getHeader("X-Forwarded-For", request.getRemoteAddr()));
            remoteIP = remoteIP.split(",")[0].trim();

            return new OriginInfo(host, port, proto, path, remoteIP);
        }

        @Override
        protected Map<String, Cookie> extractCookies() {
            return cookies;
        }

        @Override
        public String getRealPath(String relPath) {
            if (realPath == null) return null;
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
