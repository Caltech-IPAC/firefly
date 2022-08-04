package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: 5/16/17
 *
 * @author loi
 * @version $Id: $
 */
public interface SsoAdapter {

    String SSO_FRAMEWORK_NAME = "sso.framework.name";
    String SSO_FRAMEWORK_ADAPTER = "sso.framework.adapter";

    // common properties keys used by SsoAdapter implementers
    String LOGIN_URL         = "sso.login.url";
    String LOGOUT_URL        = "sso.logout.url";
    String REQ_AUTH_HOSTS    = "sso.req.auth.hosts";


    enum SsoFramework {
        josso(JOSSOAdapter.class),
        oidc(OidcAdapter.class),
        mod_auth_openidc(AuthOpenidcMod.class),
        PAT(PersonalAccessToken.class);

        Class clz;
        SsoFramework(Class clz) { this.clz= clz;}
        SsoAdapter newAdapter() {
            try {
                return (SsoAdapter)clz.newInstance();
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * If your framework uses flow that redirect a user to an identity provider to authenticate,
     * this method will be used to accept the callback and create an auth token from the response.
     */
    default Token resolveAuthToken(HttpServletRequest req) {return null;}

    /**
     * Using the old token, return a refreshed token
     * @param old the old token
     */
    default Token refreshAuthToken(Token old) {return null;}

    default boolean logout() {return false; }

    default UserInfo login(String name, String passwd) {return null;}

    Token getAuthToken();

    UserInfo getUserInfo();

    default void clearAuthInfo() {};

    default String getRequestedUrl(HttpServletRequest req) {return null;}

    default String getLoginUrl(String backTo) {return null;}

    default String getLogoutUrl(String backTo) {return null;}

    default String getProfileUrl(String backTo) {return null;}
    /**
     * set the require info to identify the current logged in user
     * when making an http request to external services.
     * @param inputs
     */
    default void setAuthCredential(HttpServiceInput inputs) {};

//====================================================================
// convenience factory methods
//====================================================================

    static boolean requireAuthCredential(String reqUrl, String... reqAuthHosts) {
        if (reqUrl == null) return true;

        if (!isEmpty(reqUrl)) {
            try {
                String reqHost = new URL(ServerContext.resolveUrl(reqUrl)).getHost().toLowerCase();
                String host = ServerContext.getRequestOwner().getHostUrl();
                if (reqHost.equals(host.toLowerCase())) {
                    return true;
                } else if(reqAuthHosts != null) {
                    for (String domain : reqAuthHosts) {
                        if (domain != null && reqHost.endsWith(domain.trim().toLowerCase())) {
                            return true;
                        }
                    }
                }
            } catch (MalformedURLException e) {
                // ignore..
            }
        }
        return false;
    }

    static SsoAdapter getAdapter() {
        String byClz = AppProperties.getProperty(SSO_FRAMEWORK_ADAPTER, "");
        String byName = AppProperties.getProperty(SSO_FRAMEWORK_NAME, "");
        try {
            if (!isEmpty(byClz)) {
                return AdapterClassResovler.getAdapter(byClz);
            } else if (!isEmpty(byName)) {
                return SsoFramework.valueOf(byName).newAdapter();
            }
        } catch (Exception e) {
            Logger.getLogger().error(e, String.format("Cannot resolve adapter for: byClz: %s  byName: %s", byClz, byName));
        }
        return null;
    }

    class AdapterClassResovler {
        private static Map<String, Class> LOADED_ADAPTERS;      // to avoid repeatedly loading the same class

        static SsoAdapter getAdapter(String className) throws Exception {
            if (LOADED_ADAPTERS == null) {
                LOADED_ADAPTERS = new HashMap<>();
            }
            Class aClass = LOADED_ADAPTERS.get(className);
            if (aClass == null) {
                Logger.getLogger().debug("Loading new SSO_FRAMEWORK_ADAPTER = " + className);
                aClass = SsoAdapter.class.getClassLoader().loadClass(className);
                LOADED_ADAPTERS.put(className, aClass);
            }
            return (SsoAdapter) aClass.newInstance();
        }
    }

    class Token implements Serializable {
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
