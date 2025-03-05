/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.events.FluxAction;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.security.SsoAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.ws.WorkspaceFactory;
import edu.caltech.ipac.firefly.server.ws.WsCredentials;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.cache.StringKey;

import javax.annotation.Nonnull;
import javax.servlet.http.Cookie;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

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
    public static StringKey USER_INFO_KEY = new StringKey("UserInfo");
    public static int USER_KEY_EXPIRY = AppProperties.getIntProperty("userkey.expiry", 3600 * 24 * 7 * 2);         // 2 weeks
    public static final String SET_USERINFO_ACTION = "app_data.setUserInfo";
    private static boolean ignoreAuth = AppProperties.getBooleanProperty("ignore.auth", false);
    private RequestAgent requestAgent;
    private Date startTime;
    private File workingDir;
    private HashMap<String, Object> attributes = new HashMap<String, Object>();
    private String eventChannel;
    private String eventConnID;
    // ------ these are lazy-load variables.. make sure you access it via getter. --------
    private WorkspaceManager wsManager;

    //------------------------------------------------------------------------------------
    private transient UserInfo userInfo;
    private transient SsoAdapter ssoAdapter;



    public RequestOwner(Date startTime) {
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
        getUserInfo().setUserKey(userKey);
    }

    public String getUserKey() {
        return getUserInfo().getUserKey();
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
        return ifNotNull(getSsoAdapter()).get(SsoAdapter::getAuthToken) != null;
    }

    /**
     * userKey is used to uniquely identify the user.  It is generated by the server and sent to the client as a cookie.
     * If the user is authenticated, the userKey is generated from the user's login name(email).
     * If the user is not authenticated, a guest user will be returned.
     * @return the UserInfo associated with this request.
     */
    @Nonnull
    public UserInfo getUserInfo() {
        if (userInfo == null) {
            userInfo = getAuthUser();
            if (userInfo == null) {
                String userKey = ifNotNull(getUserKeyFromClient()).get(newUserKey());
                UserCache<UserInfo> userInfoCache = UserCache.getInstance(userKey);
                userInfo = userInfoCache.get(USER_INFO_KEY);
                if (userInfo == null) {
                    userInfo = UserInfo.newGuestUser();
                    userInfo.setUserKey(userKey);
                    userInfoCache.put(USER_INFO_KEY, userInfo);
                }
                syncUserKey(userInfo);
            }
            notifyClient(userInfo);
        }
        return userInfo;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestOwner ro = new RequestOwner(startTime);
        ro.setRequestAgent(requestAgent);
        ro.workingDir = workingDir;
        ro.attributes = (HashMap<String, Object>) attributes.clone();
        ro.userInfo = userInfo;
        ro.wsManager = wsManager;
        ro.eventChannel = eventChannel;
        ro.eventConnID = eventConnID;

        return ro;
    }

    public void setTo(RequestOwner ro) {
        requestAgent = ro.requestAgent;
        startTime = ro.startTime;
        workingDir = ro.workingDir;
        attributes = ro.attributes;
        userInfo = ro.userInfo;
        eventChannel = ro.eventChannel;
        eventConnID = ro.eventConnID;
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

    /**
     *  @return the authenticated user info, or null if not authenticated.
     */
    private UserInfo getAuthUser() {
        if (ignoreAuth || !isAuthUser()) return null;
        UserInfo userInfo = getSsoAdapter().getUserInfo();
        if (userInfo == null) {
            getSsoAdapter().clearAuthInfo();
        } else {
            userInfo.setUserKey(userKeyFrom(userInfo.getLoginName()));
        }
        return userInfo;
    }

    private String getUserKeyFromClient() {
        String userKey = requestAgent == null ? null : requestAgent.getCookieVal(USER_KEY);
        return isEmpty(userKey) ? null : userKey;
    }

    private String newUserKey() {
        int tries = 0;
        String userKey;
        do {
            userKey = UUID.randomUUID().toString();
            if (tries++ > 1000) {
                throw new RuntimeException("Unable to generate a new userKey after 1000 tries.");
            }
        } while (UserCache.exists(new StringKey(userKey)));
        return userKey;
    }

    private String userKeyFrom(String val) {
        return UUID.nameUUIDFromBytes(String.valueOf(val).getBytes(UTF_8)).toString();
    }

    private void notifyClient(UserInfo userInfo) {
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

    private void syncUserKey(UserInfo userInfo) {
        if (requestAgent == null) return;
        if (!userInfo.getUserKey().equals(String.valueOf(getUserKeyFromClient()))) {
            Cookie cookie = new Cookie(USER_KEY, userInfo.getUserKey());
            cookie.setMaxAge(USER_KEY_EXPIRY);      // to live for two weeks
            cookie.setPath(requestAgent.getContextPath());
            requestAgent.sendCookie(cookie);
        }
    }

    public String getEventChannel() { return eventChannel; }

    public String getEventConnID() { return eventConnID; }
}
