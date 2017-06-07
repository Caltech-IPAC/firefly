package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import org.josso.gateway.ws._1_2.protocol.*;
import org.josso.gateway.ws._1_2.wsdl.SSOIdentityManager;
import org.josso.gateway.ws._1_2.wsdl.SSOIdentityProvider;
import org.josso.gateway.ws._1_2.wsdl.SSOSessionManager;

import javax.servlet.http.HttpServletRequest;
import javax.xml.rpc.ServiceException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 5/16/17
 *
 * @author loi
 * @version $Id: $
 */
public interface SsoAdapter {

    String SSO_FRAMEWORK_NAME = "sso.framework.name";
    String JOSSO = "josso";
    String OPENID_CONNECT = "oidc";

    /**
     * returns the number of seconds before this session expires.  0 if session is not valid, or it's already expires.
     * @param token
     * @return
     */
    long checkSession(String token);

    /**
     * return all of the roles for a user authenticated with this token.
     * @param token
     * @return
     */
    RoleList getRoles(String token);

    Token resolveAuthToken(String assertionKey);

    Token refreshAuthToken(Token old);

    boolean logout(String token);

    UserInfo login(String name, String passwd);

    String createSession(String name, String passwd);

    String getAssertKey();

    Token getAuthToken();

    String getAuthTokenId();

    UserInfo getUserInfo();

    void clearAuthInfo();

    Map<String, String> getIdentities();

    String getRequestedUrl(HttpServletRequest req);

    String makeAuthCheckUrl(String backTo);

//====================================================================
// convenience factory methods
//====================================================================

    static SsoAdapter getAdapter() {
        String ssoFrameworkName = AppProperties.getProperty(SSO_FRAMEWORK_NAME, "");
        switch (ssoFrameworkName) {
            case JOSSO:
                return new JOSSOAdapter();
            case OPENID_CONNECT:
                return new OidcAdapter();
            default:
                return new JOSSOAdapter();
        }
    }

    public static class Token implements Serializable {
        private String id;
        private long expiresOn = Long.MAX_VALUE;
        private Map<String, String> others = new HashMap<>();
        private Map<String, String> claims;

        public Token(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public long getExpiresOn() {
            return expiresOn;
        }

        public void setExpiresOn(long expiresOn) {
            this.expiresOn = expiresOn;
        }

        public void set(String key, String value) {
            others.put(key, value);
        }
        public String get(String key) {
            return others.get(key);
        }

        public Map<String, String> getClaims() {
            return claims;
        }

        public void setClaims(Map<String, String> claims) {
            this.claims = claims;
        }
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
