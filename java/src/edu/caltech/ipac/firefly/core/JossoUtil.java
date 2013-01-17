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

    public static void init(String jossoServerUrl, String contextPath, String userProfileUrl) {
        JossoUtil.contextPath = contextPath;
        JossoUtil.jossoServerUrl = jossoServerUrl;
        JossoUtil.userProfileUrl = userProfileUrl;
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
