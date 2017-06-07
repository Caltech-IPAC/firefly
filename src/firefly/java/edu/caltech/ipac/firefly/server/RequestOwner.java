/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.Cookie;
import java.io.File;
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

    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    public static String USER_KEY = "usrkey";
    public static final String SET_USERINFO_ACTION = "app_data.setUserInfo";
//    private static final String[] ID_COOKIE_NAMES = new String[]{WebAuthModule.AUTH_KEY, "ISIS"};
    private static boolean ignoreAuth = AppProperties.getBooleanProperty("ignore.auth", false);
    private RequestAgent requestAgent;
    private Date startTime;
    private File workingDir;
    private String host;
    private String referrer;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private String eventChannel;
    private String eventConnID;
    // ------ these are lazy-load variables.. make sure you access it via getter. --------
    private String userKey;
    private WorkspaceManager wsManager;

    //------------------------------------------------------------------------------------
    private transient UserInfo userInfo;


    public RequestOwner(String userKey, Date startTime) {
        this.userKey = userKey;
        this.startTime = startTime;
    }

    public RequestAgent getRequestAgent() {
        return requestAgent;
    }

    public void setRequestAgent(RequestAgent requestAgent) {
        this.requestAgent = requestAgent;
        if (requestAgent != null) {
            host = requestAgent.getHeader("host");
            referrer = requestAgent.getHeader("Referer");
            setWsConnInfo(requestAgent.getHeader("FF-connID"), requestAgent.getHeader("FF-channel"));
        }
    }

    public void setWsConnInfo(String connID, String channel) {
        eventConnID = connID;
        eventChannel = channel;
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

    public String getUserKey() {
        if (userKey == null) {
            String userKeyAndName = requestAgent == null ? null : requestAgent.getCookieVal(USER_KEY);
            userKey = userKeyAndName == null ? null :
                    userKeyAndName.split("/", 2)[0];

            if (userKey == null) {
                userKey = newUserKey();
                updateUserKey(new UserInfo("Guest", ""));
            }
        }
        return userKey;
    }

    public String getRemoteIP() {
        return requestAgent.getRemoteIP();
    }

    public Date getStartTime() {
        return startTime;
    }

    public Map<String, String> getCookieMap() {
        Map<String, Cookie> cookies = requestAgent.getCookies();
        Map<String, String> cmap = new HashMap<>(cookies.size());
        for(Cookie c : cookies.values()) {
            String v = c == null ? null : c.getValue();
            if (v != null) {
                cmap.put(c.getName(), c.getValue());
            }
        }
        return cmap;
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
        requestAgent.sendRedirect(url);
    }

    public Map<String, String> getIdentityCookies() {
        return SsoAdapter.getAdapter().getIdentities();
    }

    public boolean isAuthUser() {
        return !StringUtils.isEmpty(SsoAdapter.getAdapter().getAuthTokenId());
    }

    public UserInfo getUserInfo() {
        if (userInfo == null) {
            if (isAuthUser() && !ignoreAuth) {
                SsoAdapter sso = SsoAdapter.getAdapter();
                userInfo = sso.getUserInfo();
                if (userInfo == null) {
                    sso.clearAuthInfo();
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
            updateUserKey(userInfo);
        }
        return userInfo;
    }

    // should only use this as a way to bypass the web-based access.
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestOwner ro = new RequestOwner(getUserKey(), startTime);
        ro.setRequestAgent(requestAgent);
        ro.workingDir = workingDir;
        ro.attributes = (HashMap<String, Object>) attributes.clone();
        ro.userInfo = userInfo;
        ro.referrer = referrer;
        ro.host = host;
        ro.wsManager = wsManager;
        ro.userKey = userKey;
        ro.eventChannel = eventChannel;
        ro.eventConnID = eventConnID;

        return ro;
    }

    public String getProtocol() {
        return requestAgent.getProtocol();
    }

    public String getReferrer() {
        return referrer;
    }

    public String getBaseUrl() {
        return requestAgent.getBaseUrl();
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

    private void updateUserKey(UserInfo userInfo) {
        // send UserInfo to client
        FluxAction action = new FluxAction(SET_USERINFO_ACTION);
        action.setValue(userInfo.getLoginName(), "loginName");
        action.setValue(userInfo.getFirstName(), "firstName");
        action.setValue(userInfo.getLastName(), "lastName");
        action.setValue(userInfo.getInstitute(), "institute");
        action.setValue(SsoAdapter.getAdapter().makeAuthCheckUrl(""), "login_url");
        ServerEventManager.fireAction(action);

        String userName = userInfo.getName();
        String nVal = userKey + "/" + userName;
        if (requestAgent != null) {
            String cVal = requestAgent.getCookieVal(USER_KEY, "");
            if (!nVal.equals(cVal)) {
                Cookie cookie = new Cookie(USER_KEY, userKey + "/" + userName);
                cookie.setMaxAge(3600 * 24 * 7 * 2);      // to live for two weeks
                cookie.setPath("/"); // to make it available to all subpasses within base URL
                requestAgent.sendCookie(cookie);
            }
        }
    }

    public String getEventChannel() {
        return eventChannel;
    }

    public String getEventConnID() { return eventConnID; }
}
