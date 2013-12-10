package edu.caltech.ipac.firefly.server;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
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

/**
 * This class provides information associated with a request.
 * The information will be lost once the request is finish.
 *
 * It is important you DO NOT reference this in a way that prevent
 * GC from freeing it.  Plus, the information is only valid for
 * that one request, therefore, it should not be used elsewhere.
 *
 * Date: Jul 9, 2008
 *
 * @author loi
 * @version $Id: RequestOwner.java,v 1.24 2012/10/23 05:39:52 loi Exp $
 */
public class RequestOwner implements Cloneable {

    private static boolean ignoreAuth = AppProperties.getBooleanProperty("ignore.auth", false);
    private static final Logger.LoggerImpl LOG = Logger.getLogger();
    private HttpServletRequest request;
    private HttpServletResponse response;
    private String sessionId;
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

    //------------------------------------------------------------------------------------
    private transient UserInfo userInfo;


    public RequestOwner(String sessionId, String userId, Date startTime) {
        this.sessionId = sessionId;
        this.userKey = userId;
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

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public HttpServletRequest getRequest() { return request; }

    public String getUserKey() {
        if (userKey == null) {
            String userKeyAndName = WebAuthModule.getValFromCookie(WebAuthModule.USER_KEY, request);
            userKey = userKeyAndName == null ? null :
                        userKeyAndName.split("/", 2)[0];

            if (userKey == null) {
                userKey = WebAuthModule.newUserKey();
            }
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

    public Cookie[] getCookies() {
        return request!=null ? request.getCookies() : null;
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
            WebAuthModule.updateUserKey(getUserKey(), userInfo.getLoginName(), request, response);
        }
        return userInfo;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestOwner ro = new RequestOwner(sessionId, userKey, startTime);
        ro.request = request;
        ro.response = response;
        ro.workingDir = workingDir;
        ro.attributes = (HashMap<String, Object>) attributes.clone();
        ro.userKey = getUserKey();
        ro.remoteIP = getRemoteIP();
        ro.authKey = getAuthKey();
        ro.userInfo = userInfo;
        ro.referrer = referrer;
        ro.host = host;
        ro.baseUrl = baseUrl;
        ro.protocol = protocol;
        return  ro;
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
     *  return host url including protocol
     */
    public String getHostUrl() {
        return (getProtocol().startsWith("HTTPS") ? "https" : "http") + "://" + getHost();
    }
    
    public boolean isCrossSite() {
        return (referrer!=null && !referrer.startsWith(host));
    }

    public String getHost() {
        return host;
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
