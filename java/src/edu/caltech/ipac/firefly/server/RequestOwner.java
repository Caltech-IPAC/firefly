package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.security.WebAuthModule;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class provides information associated with a request. The information will be lost once the request is finish.
 * <p/>
 * It is important you DO NOT reference this in a way that prevent GC from freeing it.  Plus, the information is only
 * valid for that one request, therefore, it should not be used elsewhere.
 * <p/>
 * Date: Jul 9, 2008
 *
 * @author loi
 * @version $Id: RequestOwner.java,v 1.24 2012/10/23 05:39:52 loi Exp $
 */
public class RequestOwner implements Cloneable {

    public static String USER_KEY = "usrkey";
    private static final String[] ID_COOKIE_NAMES = new String[]{WebAuthModule.AUTH_KEY, "ISIS"};
    private static boolean ignoreAuth = AppProperties.getBooleanProperty("ignore.auth", false);
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Date startTime;
    private File workingDir;
    private String host;
    private String referrer;
    private String baseUrl;
    private String remoteIP;
    private String protocol;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    // ------ these are lazy-load variables.. make sure you access it via getter. --------
    private String userKey;
    private String authKey;
    private Map<String, Cookie> cookies;

    private WorkspaceManager wsManager;

    //------------------------------------------------------------------------------------
    private transient UserInfo userInfo;


    public RequestOwner(String userKey, Date startTime) {
        this.userKey = userKey;
        this.startTime = startTime;
    }

    public void setHttpRequest(HttpServletRequest request) {
        this.request = request;

        host = request.getHeader("host");
        protocol = request.getProtocol();
        referrer = request.getHeader("Referer");
        baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), request.getContextPath()) + "/";
        remoteIP = request.getHeader("X-Forwarded-For");
        if (StringUtils.isEmpty(remoteIP)) {
            remoteIP = request.getRemoteAddr();
        }
    }

    public void setHttpResponse(HttpServletResponse response) {
        this.response = response;
    }

    public WorkspaceManager getWsManager() {
        if (wsManager == null) {
            getUserInfo();
            if (userInfo.isGuestUser()) {
                wsManager = new WorkspaceManager(getUserKey());
            } else {
                wsManager = new WorkspaceManager(userInfo.getLoginName(), getIdentityCookies());
            }
        }
        return wsManager;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public String getUserKey() {
        if (userKey == null) {
            String userKeyAndName = WebAuthModule.getValFromCookie(USER_KEY, request);
            userKey = userKeyAndName == null ? null :
                    userKeyAndName.split("/", 2)[0];

            if (userKey == null) {
                userKey = newUserKey();
                updateUserKey("Guest");
            }
            Logger.briefInfo("establishing userKey: " + userKey);
        }
        return userKey;
    }

    public String getAuthKey() {
        if (authKey == null) {
            authKey = WebAuthModule.getToken(request);
        }
        return authKey;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Map<String, Cookie> getCookieMap() {
        if (cookies == null) {
            if (request != null) {
                cookies = new HashMap<String, Cookie>();
                if (request.getCookies() != null) {
                    for (Cookie c : request.getCookies()) {
                        cookies.put(c.getName(), c);
                    }
                }
            }
        }
        return cookies;
    }

    public Cookie[] getCookies() {
        return getCookieMap().values().toArray(new Cookie[0]);
    }

    public File getWorkingDir() {
        return workingDir == null ? ServerContext.getWorkingDir() : workingDir;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public void sendRedirect(String url) {
        try {
            response.sendRedirect(url);
        } catch (IOException e) {
            LOG.error(e, "Unable to redirect to:" + url);
        }
    }

    public Map<String, String> getIdentityCookies() {
        HashMap<String, String> idCookies = new HashMap<String, String>();
        for (String s : ID_COOKIE_NAMES) {
            Cookie c = getCookieMap().get(s);
            if (c != null && !StringUtils.isEmpty(c.getValue())) {
                idCookies.put(s, c.getValue());
            }
        }
        return idCookies.size() == 0 ? null : idCookies;
    }

    public boolean isAuthUser() {
        return !StringUtils.isEmpty(getAuthKey());
    }

    // should only use this as a way to bypass the web-based access.
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public UserInfo getUserInfo() {
        if (userInfo == null) {
            if (isAuthUser() && !ignoreAuth) {
                userInfo = WebAuthModule.getUserInfo(getAuthKey());
                if (userInfo == null) {
                    WebAuthModule.removeAuthKey(request, response);
                } else {
                    updateUserKey(userInfo.getLoginName());
                }
            }

            if (userInfo == null) {
                Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
                userInfo = (UserInfo) cache.get(new StringKey(getUserKey()));
                if (userInfo == null) {
                    userInfo = UserInfo.newGuestUser();
                    cache.put(new StringKey(getUserKey()), userInfo);
                }
            }
        }
        return userInfo;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestOwner ro = new RequestOwner(getUserKey(), startTime);
        ro.request = request;
        ro.response = response;
        ro.workingDir = workingDir;
        ro.attributes = (HashMap<String, Object>) attributes.clone();
        ro.remoteIP = getRemoteIP();
        ro.authKey = getAuthKey();
        ro.userInfo = userInfo;
        ro.referrer = referrer;
        ro.host = host;
        ro.baseUrl = baseUrl;
        ro.protocol = protocol;
        ro.wsManager = wsManager;
        ro.cookies = getCookieMap();
        return ro;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * return host url including protocol
     */
    public String getHostUrl() {
        return (getProtocol().startsWith("HTTPS") ? "https" : "http") + "://" + getHost();
    }

    public boolean isCrossSite() {
        return (referrer != null && !referrer.startsWith(host));
    }

    public String getHost() {
        return host;
    }

//====================================================================
//
//====================================================================

    private String newUserKey() {
        int tries = 0;
        String userKey;
        do {
            userKey = UUID.randomUUID().toString();
            if (tries++ > 1000) {
                throw new RuntimeException("Unable to generate a new userKey after 1000 tries.");
            }
        } while (UserCache.exists(new StringKey(userKey)));
        UserCache.create(new StringKey(userKey));
        return userKey;
    }

    private void updateUserKey(String userName) {
        if (request == null || response == null) return;

        String nVal = userKey + "/" + userName;
        String cVal = WebAuthModule.getValFromCookie(USER_KEY, request);
        if (!nVal.equals(String.valueOf(cVal))) {
            Cookie cookie = new Cookie(USER_KEY, userKey + "/" + userName);
            cookie.setMaxAge(3600 * 24 * 7 * 2);      // to live for two weeks
            cookie.setPath("/"); // to make it available to all subpasses within base URL
            response.addCookie(cookie);
        }
    }
}
