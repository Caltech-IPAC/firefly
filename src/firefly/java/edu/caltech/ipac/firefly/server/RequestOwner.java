/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WorkspaceFactory;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.util.AppProperties;
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
    private static boolean ignoreAuth = AppProperties.getBooleanProperty("ignore.auth", false);
    private RequestAgent requestAgent;
    private Date startTime;
    private File workingDir;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private String eventChannel;
    private String eventConnID;
    // ------ these are lazy-load variables.. make sure you access it via getter. --------
    private String userKey;
    private WorkspaceManager wsManager;

    //------------------------------------------------------------------------------------
    private transient UserInfo userInfo;
    private transient SsoAdapter ssoAdapter;


    public RequestOwner(String userKey, Date startTime) {
        this.userKey = userKey;
        this.startTime = startTime;
    }

    public RequestAgent getRequestAgent() {
        return requestAgent;
    }

    public SsoAdapter getSsoAdapter() {
        if (ssoAdapter == null) {
            ssoAdapter = SsoAdapter.getAdapter();
        }
        return ssoAdapter;
    }

    public void setRequestAgent(RequestAgent requestAgent) {
        this.requestAgent = requestAgent;
        if (requestAgent != null) {
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
                wsManager = WorkspaceFactory.getWorkspaceHandler().withCredentials(new WsCredentials(getUserKey()));
            } else {
                wsManager = WorkspaceFactory.getWorkspaceHandler().withCredentials(new WsCredentials(userInfo.getLoginName()));
            }
        }
        return wsManager;
    }

    /**
     * Normally, this is not used, unless for testing or similar cases where you need to change the default behavior.
     * @param userKey a user key string
     */
    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getUserKey() {
        if (userKey == null) {
            userKey = requestAgent == null ? null : requestAgent.getCookieVal(USER_KEY);
            userKey = userKey == null || userKey.contains(" ") ? null : userKey;

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
        Map<String, String> cmap = null;
        if(requestAgent!=null) {
            Map<String, Cookie> cookies = requestAgent.getCookies();
            cmap = new HashMap<>(cookies.size());
            for (Cookie c : cookies.values()) {
                String v = c == null ? null : c.getValue();
                if (v != null) {
                    cmap.put(c.getName(), c.getValue());
                }
            }
        }
        return cmap;
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

    public boolean isAuthUser() {
        return getSsoAdapter() != null && getSsoAdapter().getAuthToken() != null;
    }

    public UserInfo getUserInfo() {
        if (userInfo == null) {
            if (isAuthUser() && !ignoreAuth) {
                userInfo = ssoAdapter.getUserInfo();
                if (userInfo == null) {
                    ssoAdapter.clearAuthInfo();
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
        ro.wsManager = wsManager;
        ro.userKey = userKey;
        ro.eventChannel = eventChannel;
        ro.eventConnID = eventConnID;

        return ro;
    }

    public void setTo(RequestOwner ro) {
        requestAgent = ro.requestAgent;
        startTime = ro.startTime;
        workingDir = ro.workingDir;
        attributes = ro.attributes;
        eventChannel = ro.eventChannel;
        eventConnID = ro.eventConnID;
        userKey = ro.userKey;
        wsManager = ro.wsManager;
    }

    public String getBaseUrl() {
        return requestAgent.getBaseUrl();
    }

    /**
     * return host url including protocol
     */
    public String getHostUrl() {
        return requestAgent.getHostUrl();
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
        if (requestAgent != null) {
            String cVal = requestAgent.getCookieVal(USER_KEY, "");
            if (!userKey.equals(cVal)) {
                Cookie cookie = new Cookie(USER_KEY, userKey);
                cookie.setMaxAge(3600 * 24 * 7 * 2);      // to live for two weeks
                cookie.setPath(requestAgent.getContextPath());
                requestAgent.sendCookie(cookie);
            }
        }

        // send UserInfo to client
        FluxAction action = new FluxAction(SET_USERINFO_ACTION);
        action.setValue(userInfo.getLoginName(), "loginName");
        action.setValue(userInfo.getFirstName(), "firstName");
        action.setValue(userInfo.getLastName(), "lastName");
        action.setValue(userInfo.getInstitute(), "institute");
        if (getSsoAdapter() != null) {
            action.setValue(getSsoAdapter().getLoginUrl(""), "login_url");
            action.setValue(getSsoAdapter().getLogoutUrl(""), "logout_url");
        }
        ServerEventManager.fireAction(action);
    }

    public String getEventChannel() {
        return eventChannel;
    }

    public String getEventConnID() { return eventConnID; }
}
