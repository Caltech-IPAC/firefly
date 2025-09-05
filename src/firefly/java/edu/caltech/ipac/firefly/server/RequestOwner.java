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

import javax.servlet.http.Cookie;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotEmpty;
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
    public static int USER_KEY_EXPIRY = AppProperties.getIntProperty("userkey.expiry", 3600 * 24 * 365);         // 1 year
    public static final String SET_USERINFO_ACTION = "app_data.setUserInfo";
    private static final boolean IGNORE_AUTH = AppProperties.getBooleanProperty("ignore.auth", false);
    private Date startTime;
    private String eventChannel;
    private String eventConnID;
    private String usrKey;
    //------------------------------------------------------------------------------------
    private transient RequestAgent requestAgent;
    private transient SsoAdapter ssoAdapter;
    private transient UserInfo userInfo;

    RequestOwner(){}

    public void init(RequestAgent ra) {
        if (ra == null) {
            LOG.warn("RequestOwner.init was passed a null RequestAgent");
        }
        this.requestAgent = ra;
        startTime = new Date();
        if (ra != null) {
            setWsConnInfo(ra.getHeader("FF-connID"), ra.getHeader("FF-channel"));
        }
        ssoAdapter = SsoAdapter.getAdapter();       // ensure ssoAdapter is initialized
        id(ra);     // ensure usrKey and userInfo are initialized
    }

    public RequestAgent getRequestAgent() { return requestAgent; }
    public SsoAdapter getSsoAdapter() { return ssoAdapter; }
    public String getRemoteIP() {
        return requestAgent.getRemoteIP();
    }
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Normally, this is not used, unless for testing or similar cases where you need to change the default behavior.
     * @param userKey a user key string
     */
    public void setUserKey(String userKey) {
        usrKey = userKey;
    }
    public String getUserKey() {
        return usrKey;
    }
    public void extendUserKeyExpiry() {
        ifNotEmpty(getUserKeyFromClient()).apply(this::updateClientUserKey);
    }

    public boolean isAuthUser() { return getAuthUser() != null; }
    public UserInfo getUserInfo() { return userInfo; }

    public void setWsConnInfo(String connID, String channel) {
        eventConnID = connID;
        eventChannel = channel;
    }
    public String getEventChannel() { return eventChannel; }
    public String getEventConnID() { return eventConnID; }

    /**
     * return host url including protocol
     */
    public String getHostUrl() {
        return ifNotNull(requestAgent).get(a -> a.getHostUrl());
    }
    public String getBaseUrl() {
        return ifNotNull(requestAgent).get(a -> a.getBaseUrl());
    }

    public Map<String, String> getCookieMap() {
        if (requestAgent == null) return Collections.emptyMap();
        Map<String, String> cmap = new HashMap<>();
        requestAgent.getCookies().forEach((name, cookie) -> {
            if (cookie != null && cookie.getValue() != null) {
                cmap.put(name, cookie.getValue());
            }
        });
        return cmap;
    }

    /**
     * Copy the source RequestOwner to the target RequestOwner.
     * @param source the source RequestOwner
     * @param target the target RequestOwner
     * @return the target RequestOwner
     */
    private RequestOwner copy(RequestOwner source, RequestOwner target) {
        target.requestAgent = source.requestAgent;
        target.startTime = source.startTime;
        target.userInfo = source.userInfo;
        target.usrKey = source.usrKey;
        target.ssoAdapter = source.ssoAdapter;
        target.eventChannel = source.eventChannel;
        target.eventConnID = source.eventConnID;
        return target;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return copy(this, new RequestOwner());
    }

    public void setTo(RequestOwner ro) {
        copy(ro, this);
    }

//====================================================================
//
//====================================================================

    /**
     * A userKey uniquely identifies the user.  It is managed by the server and sent to the client as a cookie.
     * If the user is authenticated, the userKey is derived from the user's login name(email).
     * If the user is not authenticated and no key exists, a new one is generated.
     * This function ensure usrKey and userInfo are initialized.
     */
    private void id(RequestAgent ra) {
        // exceptional case where ra is null, such as in unit tests.
        if (ra == null) {
            userInfo = UserInfo.newGuestUser();
            usrKey = newUserKey();
            LOG.info("id(): RequestAgent is null, this is not expected in production.");
            return;
        }

        userInfo = getAuthUser();
        if (userInfo != null) {         // authenticated
            usrKey = userKeyFrom(userInfo.getLoginName());
        } else {                        // guest user
            userInfo = UserInfo.newGuestUser();
            usrKey = getUserKeyFromClient();
            if (isEmpty(usrKey)) {
                usrKey = newUserKey();
                updateClientUserKey(usrKey);
            }
        }
        if (eventConnID != null) {
            notifyClient(userInfo);
        }
        LOG.trace("User: " + userInfo,
                " ConnId: " + eventConnID,          // when lowLevelDoFetch is used directly, ConnId and Channel is null.  may need to reevaluate.
                " Channel: " + eventChannel,
                " URL: " + ra.getRequestUrl(),
                " RemoteIP: " + ra.getRemoteIP(),
                " Referrer: " + ra.getHeader("Referer"));
    }

    /**
     *  @return the authenticated user info, or null if not authenticated.
     */
    private UserInfo getAuthUser() {
        if (IGNORE_AUTH) return null;
        return ifNotNull(getSsoAdapter()).get(SsoAdapter::getUserInfo);
    }

    private String getUserKeyFromClient() {
        String userKey = requestAgent == null ? null : requestAgent.getCookieVal(USER_KEY);
        return isEmpty(userKey) ? null : userKey;
    }

    private String newUserKey() {
        for (int tries = 0; tries < 1000; tries++) {
            String userKey = UUID.randomUUID().toString();
            if (!UserCache.exists(new StringKey(userKey))) {
                return userKey;
            }
        }
        throw new RuntimeException("Unable to generate a new userKey after 1000 tries.");
    }

    private String userKeyFrom(String val) {
        return UUID.nameUUIDFromBytes(String.valueOf(val).getBytes(UTF_8)).toString();
    }

    private void notifyClient(UserInfo userInfo) {
        // send UserInfo to client
        FluxAction action = new FluxAction(SET_USERINFO_ACTION)
            .setValue(userInfo.getLoginName(), "loginName")
            .setValue(userInfo.getFirstName(), "firstName")
            .setValue(userInfo.getLastName(), "lastName")
            .setValue(userInfo.getInstitute(), "institute");

        if (getSsoAdapter() != null) {
            action.setValue(getSsoAdapter().getLoginUrl(""), "login_url");
            action.setValue(getSsoAdapter().getLogoutUrl(""), "logout_url");
        }

        ServerEventManager.fireAction(action);
    }

    private void updateClientUserKey(String userKey) {
        if (requestAgent == null) return;
        Cookie cookie = new Cookie(USER_KEY, userKey);
        cookie.setMaxAge(USER_KEY_EXPIRY);
        cookie.setPath(requestAgent.getContextPath());
        requestAgent.sendCookie(cookie);
    }

}
