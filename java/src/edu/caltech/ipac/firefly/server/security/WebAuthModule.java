package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Date: Apr 1, 2010
 *
 * @author loi
 * @version $Id: WebAuthModule.java,v 1.12 2012/09/08 00:50:26 loi Exp $
 */
public class WebAuthModule {
    public static String AUTH_KEY = "JOSSO_SESSIONID";
    public static String TO_BE_DELETE = "-";

    public static UserInfo getUserInfo(String authToken) {
        return StringUtils.isEmpty(authToken) ? null :
                JOSSOAdapter.getUserInfo(authToken);
    }

    //    public static void logout(String token, HttpServletRequest req, HttpServletResponse resp) {
//        RequestOwner ro = ServerContext.getRequestOwner();
//        ro.setAuthKey(null);
//        updateAuthKey(null, resp);
//        updateUserKey(ro.getUserKey(), UserInfo.GUEST, req, resp);
//        if (token != null) {
//            JOSSOAdapter.logout(token);
//        }
//    }
//
    public static String getToken(HttpServletRequest req) {
        return getValFromCookie(AUTH_KEY, req);
    }

    public static Cookie getAuthCookie(HttpServletRequest req) {
        return getCookie(AUTH_KEY, req);
    }

    public static void updateAuthKey(String authKey, HttpServletResponse response) {
        Cookie c = new Cookie(AUTH_KEY, authKey);
        c.setMaxAge(authKey == null ? 0 : 60 * 60 * 24 * 14);
        c.setValue(authKey);
        c.setPath("/");
        response.addCookie(c);

    }

    public static void removeAuthKey(HttpServletRequest req, HttpServletResponse response) {
        Cookie c = getAuthCookie(req);
        if (c != null) {
            c.setMaxAge(0);
            c.setValue(TO_BE_DELETE);
            c.setDomain(".ipac.caltech.edu");
            c.setPath("/");
            response.addCookie(c);
        }
    }

    public static Cookie getCookie(String key, HttpServletRequest req) {
        if (req == null) return null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals(key) && (c.getValue() == null || !c.getValue().equals(TO_BE_DELETE))) {
                    return c;
                }
            }
        }
        return null;
    }

    public static String getValFromCookie(String key, HttpServletRequest request) {
        Cookie c = getCookie(key, request);
        return c == null ? null : c.getValue();
    }
}
