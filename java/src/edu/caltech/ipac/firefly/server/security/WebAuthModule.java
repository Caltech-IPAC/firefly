package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Date: Apr 1, 2010
 *
 * @author loi
 * @version $Id: WebAuthModule.java,v 1.12 2012/09/08 00:50:26 loi Exp $
 */
public class WebAuthModule {
    public static String USER_KEY = "usrkey";
    private static String AUTH_KEY = "JOSSO_SESSIONID";

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
        c.setMaxAge(authKey == null ? 0 : 60*60*24*14);
        c.setValue(authKey);
        c.setPath("/");
        response.addCookie(c);

    }

    public static void removeAuthKey(HttpServletRequest req, HttpServletResponse response) {
        Cookie c = getAuthCookie(req);
        if (c != null) {
            c.setMaxAge(0);
            c.setValue("-");
            response.addCookie(c);
        }
    }

    public static void updateUserKey(String userKey, String userName,
                                     HttpServletRequest req, HttpServletResponse response) {
        if (req == null | response == null) return;

        String nVal = userKey + "/" + userName;
        String cVal = getValFromCookie(USER_KEY, req);
        if (!nVal.equals(String.valueOf(cVal))) {
            Cookie cookie = new Cookie(USER_KEY, userKey + "/" + userName);
            cookie.setMaxAge(3600*24*7*4);      // to live for four weeks
            cookie.setPath("/"); // to make it available to all subpasses within base URL
            response.addCookie(cookie);
        }

    }

    public static Cookie getCookie(String key, HttpServletRequest req) {
        if (req == null) return null;
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals(key)) {
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

    public static String newUserKey() {
        Cache cache = CacheManager.getCache(Cache.TYPE_PERM_SMALL);
        int tries = 0;
        String userKey;
        do {
            userKey = UUID.randomUUID().toString();
            tries++;
        } while (tries > 5 || cache.isCached(new StringKey(userKey)));


        return userKey;
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
