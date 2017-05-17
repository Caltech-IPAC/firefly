/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.util.StringUtils;

/**
 * Date: Jun 15, 2010
 *
 * @author loi
 * @version $Id: JossoUtil.java,v 1.7 2012/07/16 23:30:10 loi Exp $
 */
public class JossoUtil {

    public static final String LOGIN_URL = "signon/login.do";
    public static final String LOGOUT_URL = "signon/logout.do";
    public static final String BACK_TO_KEY = "josso_back_to";
    public static String VERIFIER_BACK_TO = "verifier_back_to";
    public static String VERIFY_URL = "/josso_verify";
    private static String jossoServerUrl;
    private static String contextPath;
    private static String userProfileUrl;
    private static boolean isInit = false;

    public static void init(String jossoServerUrl, String contextPath, String userProfileUrl) {
        if (!isInit) {
            isInit = true;
            JossoUtil.contextPath = contextPath;
            JossoUtil.jossoServerUrl = jossoServerUrl;
            JossoUtil.userProfileUrl = userProfileUrl;
        }
    }

    public static String makeLoginUrl(String backTo) {
//        return jossoServerUrl + "signon/login.do" + "?josso_back_to=" + makeVerifyUrl(backTo);
        String backToUrl = StringUtils.isEmpty(backTo) ? "" : "?" + BACK_TO_KEY + "=" + backTo;
        return jossoServerUrl + LOGIN_URL + backToUrl;
    }

    public static String makeLogOutUrl(String backTo) {
//        return jossoServerUrl + "signon/logout.do" + "?josso_back_to=" + makeVerifyUrl(backTo);
        String backToUrl = StringUtils.isEmpty(backTo) ? "" : "?" + BACK_TO_KEY + "=" + backTo;
        return jossoServerUrl + LOGOUT_URL + backToUrl;
    }

    public static String makeAuthCheckUrl(String backTo) {
        return jossoServerUrl + LOGIN_URL + "?josso_cmd=login_optional" + "&" + BACK_TO_KEY + "=" + makeVerifyUrl(backTo);
    }

    public static String makeUserProfileUrl(String backTo) {
        String backToUrl = StringUtils.isEmpty(backTo) ? "" : BACK_TO_KEY + "=" + backTo;
        return userProfileUrl + "&" + backToUrl;
    }

    public static String makeVerifyUrl(String backTo) {
        int idx = backTo.indexOf(contextPath);
        if (idx >= 0) {
            String base = backTo.substring(0, idx);
            return base + contextPath + VERIFY_URL + "?" + VERIFIER_BACK_TO + "=" + Base64.encode(backTo);
        }
        return null;
    }


}
