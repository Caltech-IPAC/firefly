/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * This class support authentication in which mod_auth_openidc is used
 * at the proxy level.
 *
 * @author loi
 * @version $Id: JOSSOAdapter.java,v 1.14 2012/10/10 21:54:45 loi Exp $
 */
public class AuthOpenidcMod implements SsoAdapter {

    private static final String REMOTE_USER = "x-forwarded-user";
    private static final String ACCESS_TOKEN = "oidc_access_token";
    private static final String EXPIRES_IN = "oidc_access_token_expires";

    private static final String IDP_NAME = "oidc_claim_idp_name";
    private static final String IDP = "oidc_claim_idp";
    private static final String AFFILIATION = "oidc_claim_affiliation";
    private static final String FIRST_NAME = "oidc_claim_given_name";
    private static final String LAST_NAME = "oidc_claim_family_name";
    private static final String EMAIL = "oidc_claim_email";

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final String ROLE_CLAIM = "oidc_claim_ismemberof";


    /**
     * returns the number of seconds before this session expires.  0 if session is not valid, or it's already expires.
     * @param tokenId
     * @return
     */
    public long checkSession(String tokenId) {
        return 0;
    }

    /**
     * return all of the roles for a user authenticated with this token.
     * @param token
     * @return
     */
    public RoleList getRoles(String token) {
        return getRoleList();
    }

    public Token resolveAuthToken(String assertionKey) {
        return null;
    }

    public Token refreshAuthToken(Token old) {
        return null;
    }

    public boolean logout(String token) {
        clearAuthInfo();
        return true;
    }

    public UserInfo login(String name, String passwd) {
        return null;
    }

    public String createSession(String name, String passwd) {
        return null;
    }

    public String getAssertKey() {
        return null;
    }

    public Token getAuthToken() {
        return getToken();
    }

    public String getAuthTokenId() {
        Token token = getAuthToken();
        return token == null ? null : token.getId();
    }

    public UserInfo getUserInfo() {
        RequestAgent ra = ServerContext.getRequestOwner().getRequestAgent();
        try {
            String email = getString(ra, EMAIL, "");

            UserInfo userInfo = new UserInfo(email, null);
            userInfo.setEmail(email);
            userInfo.setLastName(getString(ra, LAST_NAME, ""));
            userInfo.setFirstName(getString(ra, FIRST_NAME, ""));
            userInfo.setInstitute(getString(ra, AFFILIATION, ""));
            userInfo.setProperty(IDP_NAME, getString(ra, IDP_NAME, ""));
            userInfo.setProperty(IDP, getString(ra, IDP, ""));
            userInfo.setRoles(getRoleList());
            return userInfo;
        } catch (Exception e) {
            LOGGER.error(e);
            return null;
        }
    }

    public void clearAuthInfo() {
    }

    @NotNull
    public Map<String, String> getIdentities() {
        HashMap<String, String> identities = new HashMap<>();
        String remoteUser = ServerContext.getRequestOwner().getRequestAgent().getHeader(REMOTE_USER);
        if (!StringUtils.isEmpty(remoteUser)) {
            identities.put(REMOTE_USER, remoteUser);
        }
        return identities;
    }

    public String getRequestedUrl(HttpServletRequest req) {
        return null;
    }

    public String makeAuthCheckUrl(String requestedUrl) {
        return null;
    }


//====================================================================
//  private methods
//====================================================================

    private Token getToken() {
        Token token = null;
        try {
            RequestAgent ra = ServerContext.getRequestOwner().getRequestAgent();
            String remoteUser = getString(ra, REMOTE_USER, "");
            int expiresIn = getInt(ra, EXPIRES_IN, 0);
            token = new Token(remoteUser);
            token.set(EXPIRES_IN, String.valueOf(expiresIn));
            token.setExpiresOn(expiresIn);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return token;
    }

    private static String getString(RequestAgent ra, String key, String def) {
        return ra.getHeader(key, def);
    }

    private static int getInt(RequestAgent ra, String key, int def) {
        String val = ra.getHeader(key);
        if (val == null) {
            return def;
        } else {
            try {
                return Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException ex) {
                return def;
            }
        }
    }

    /**
     * Convert the isMemberOf string to a RoleList.  This is specific to CILogon.
     * The values look like it came from an LDAP service. ["cn=val,ou=val,dc=val1,dc=val2,dc=val3","cn=val,ou=val,dc=val1,dc=val2,dc=val3"]
     * @return
     */
    @NotNull
    private static RoleList getRoleList() {
        RequestAgent ra = ServerContext.getRequestOwner().getRequestAgent();
        String[] isMemberOf = getString(ra, ROLE_CLAIM, "").split(",");

        RoleList roleList = new RoleList();
        String cuser = "";
        for (String r : isMemberOf) {
            String[] keyval = r.split("=", 2);
            String cn=null, dc="";

            if (keyval[0].equals("cn")) {
                cn = keyval.length > 1 ? keyval[1] : "";
            } else if (keyval[0].equals("dc")) {
                dc = keyval.length > 1 ? keyval[1] : "";
            }
            if (!StringUtils.isEmpty(cn) && !cn.equals(cuser)) {
                cuser = cn;
                roleList.add(new RoleList.RoleEntry(dc, dc.hashCode(), cn, cn.hashCode(), ""));
            }
        }

        return roleList;
    }

}
