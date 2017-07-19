/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.network.HttpServices;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Date: May 15, 2017
 *
 * @author loi
 * @version $Id: JOSSOAdapter.java,v 1.14 2012/10/10 21:54:45 loi Exp $
 */
public class OidcAdapter implements SsoAdapter {

    private static final String ASSERT_KEY = "code";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String ID_TOKEN = "id_token";
    private static final String EXPIRES_IN = "expires_in";

    private static final String IDP_NAME = "idp_name";
    private static final String IDP = "idp";
    private static final String AFFILIATION = "affiliation";
    private static final String FIRST_NAME = "given_name";
    private static final String LAST_NAME = "family_name";
    private static final String EMAIL = "email";

    private static final String AUTH_TOKEN_KEY = "auth_token_key";
    private static final String USER_INFO_KEY = "user_info_key";
    private static final String REQUEST_URL = "requested_url";

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static String ssoServerUrl = AppProperties.getProperty("sso.server.url");
    private static String ssoProfileUrl = AppProperties.getProperty("sso.user.profile.url");
    private static String oidcClientId = AppProperties.getProperty("oidc_client_id");
    private static String oidcClientSecret = AppProperties.getProperty("oidc_client_secret");

    private static final String LOGIN_URL = ssoServerUrl + "/authorize";
    private static final String TOKEN_URL = ssoServerUrl + "/oauth2/token";
    private static final String USER_INFO_URL = ssoServerUrl + "/oauth2/userinfo";
    private static final String VERIFY_URL = "oidc/verify";
    private static final String SCOPE = "openid profile email org.cilogon.userinfo edu.uiuc.ncsa.myproxy.getcert";
    private static final String ROLE_CLAIM = "isMemberOf";


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
        return null;
    }

    public Token resolveAuthToken(String assertionKey) {

        HttpServiceInput input = new HttpServiceInput()
                .setParam("client_id", oidcClientId)
                .setParam("client_secret", oidcClientSecret)
                .setParam("grant_type", "authorization_code")
                .setParam("code", assertionKey)
                .setParam("redirect_uri", makeVerifyUrl())
                .setParam("scope", SCOPE);

        Token token = getToken(TOKEN_URL, input);
        UserInfo userInfo = token == null ? null : getUserInfo(token.getId());

        CacheManager.getSessionCache().put(new StringKey(AUTH_TOKEN_KEY), token);
        CacheManager.getSessionCache().put(new StringKey(USER_INFO_KEY), userInfo);
        return token;
    }

    public Token refreshAuthToken(Token old) {
        String refreshToken = old.get(REFRESH_TOKEN);
        HttpServiceInput input = new HttpServiceInput()
                                .setParam("client_id", oidcClientId)
                                .setParam("client_secret", oidcClientSecret)
                                .setParam("grant_type", "refresh_token")
                                .setParam("refresh_token", refreshToken)
                                .setParam("scope", SCOPE);
        Token token = getToken(TOKEN_URL, input);
        if (token != null) {
            token.setExpiresOn(getNextCheckTime());
            token.set(ID_TOKEN, old.get(ID_TOKEN));
            token.setClaims(old.getClaims());
        }
        CacheManager.getSessionCache().put(new StringKey(AUTH_TOKEN_KEY), token);
        return token;
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
        return ASSERT_KEY;
    }

    public Token getAuthToken() {
        return (Token) CacheManager.getSessionCache().get(new StringKey(AUTH_TOKEN_KEY));
    }

    public String getAuthTokenId() {
        Token token = getAuthToken();
        return token == null ? null : token.getId();
    }

    public UserInfo getUserInfo() {
        UserInfo userInfo = (UserInfo) CacheManager.getSessionCache().get(new StringKey(USER_INFO_KEY));
        return userInfo;
    }

    public void clearAuthInfo() {
        CacheManager.getSessionCache().put(new StringKey(AUTH_TOKEN_KEY), null);
        CacheManager.getSessionCache().put(new StringKey(USER_INFO_KEY), null);
    }

    public Map<String, String> getIdentities() {
        HashMap<String, String> identities = new HashMap<>();
        identities.put(ACCESS_TOKEN, getAuthTokenId());
        return identities;
    }

    public String getRequestedUrl(HttpServletRequest req) {
        String requestedUrl = (String) CacheManager.getSessionCache().get(new StringKey(REQUEST_URL));
        requestedUrl = requestedUrl == null ? ServerContext.getRequestOwner().getRequestAgent().getBaseUrl() : requestedUrl;
        return requestedUrl;
    }

    public String makeAuthCheckUrl(String requestedUrl) {
        CacheManager.getSessionCache().put(new StringKey(REQUEST_URL), requestedUrl);
        return String.format("%s?response_type=code&client_id=%s&redirect_uri=%s&scope=%s&skin=LSST",
                LOGIN_URL, oidcClientId, encode(makeVerifyUrl()), encode(SCOPE));
    }


//====================================================================
//  private methods
//====================================================================

    private Token getToken(String url, HttpServiceInput input) {
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        if (HttpServices.postData(url, results, input) != 200) return null;

        Token token = null;
        try {
            LOGGER.briefDebug("auth token:" + results);

            JSONParser parser = new JSONParser();
            JSONObject ans = (JSONObject) parser.parse(results.toString());

            String accessToken = getString(ans, ACCESS_TOKEN, "");
            String idToken = getString(ans, ID_TOKEN, "");
            String refreshToken = getString(ans, REFRESH_TOKEN, "");
            int expiresIn = getInt(ans, EXPIRES_IN, 0);
            token = new Token(accessToken);
            token.set(ID_TOKEN, idToken);
            token.set(REFRESH_TOKEN, refreshToken);
            token.set(EXPIRES_IN, String.valueOf(expiresIn));
            token.setExpiresOn(getNextCheckTime());
            if (!StringUtils.isEmpty(idToken)) {
                token.setClaims(getClaims(idToken));
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return token;
    }

    private UserInfo getUserInfo(String accessToken) {
        String url = String.format("%s?access_token=%s", USER_INFO_URL, accessToken);
        ByteArrayOutputStream results = new ByteArrayOutputStream();

        if (HttpServices.getData(url, results) != 200) return null;
        try {
            LOGGER.briefDebug("user_info:" + results);
            JSONParser parser = new JSONParser();
            JSONObject ans = (JSONObject) parser.parse(results.toString());
            String email = getString(ans, EMAIL, "");

            UserInfo userInfo = new UserInfo(email, null);
            userInfo.setEmail(email);
            userInfo.setLastName(getString(ans, LAST_NAME, ""));
            userInfo.setFirstName(getString(ans, FIRST_NAME, ""));
            userInfo.setInstitute(getString(ans, AFFILIATION, ""));
            userInfo.setProperty(IDP_NAME, getString(ans, IDP_NAME, ""));
            userInfo.setProperty(IDP, getString(ans, IDP, ""));
            userInfo.setRoles(getRoleList(getList(ans, ROLE_CLAIM)));
            return userInfo;
        } catch (ParseException e) {
            LOGGER.error(e);
            return null;
        }
    }

    /**
     * @return a long time for when this identity should be re-validated.
     */
    private static long getNextCheckTime() {
        return System.currentTimeMillis() + 1000*60*5;   // re-check every 5 mins.
    }

    private static String makeVerifyUrl() {
        return   ServerContext.getRequestOwner().getRequestAgent().getBaseUrl() + VERIFY_URL;
    }

    @NotNull
    private static List<String> getList(JSONObject obj, String key) {
        return toList(obj.get(key));
    }

    private static List<String> toList(Object obj) {
        if (obj != null && obj instanceof JSONArray) {
            JSONArray ary = (JSONArray) obj;
            return Arrays.stream(ary.toArray()).map(String::valueOf)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private static String getString(JSONObject obj, String key, String def) {
        Object val = obj.get(key);
        String str = (val == null) ? null : String.valueOf(val);
        return StringUtils.isEmpty(str) ? def : str;
    }

    private static int getInt(JSONObject obj, String key, int def) {
        Object val = obj.get(key);
        if (val == null) {
            return def;
        } else if (val instanceof Number) {
                return ((Number)val).intValue();
        } else {
            try {
                return Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException ex) {
                return def;
            }
        }
    }

    private String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    @NotNull
    private Map<String, String> getClaims(String idTokenString) {
        Map<String, String> claims = new HashMap<>();
        try {
            JWT jwt = JWTParser.parse(idTokenString);
            if (jwt instanceof SignedJWT) {
                SignedJWT jwsObject = (SignedJWT)jwt;
                // continue with signature verification...
            } else if (jwt instanceof EncryptedJWT) {
                EncryptedJWT jweObject = (EncryptedJWT)jwt;
                // continue with decryption...
            }
            if (validateClaims(jwt.getJWTClaimsSet())) {
                Map<String, Object> claimset = jwt.getJWTClaimsSet().getClaims();
                for (String key : claimset.keySet()) {
                    if (key.equals(ROLE_CLAIM)) {
                        claims.put(key, StringUtils.toString(toList(claimset.get(key))));
                    } else {
                        claims.put(key, String.valueOf(claimset.get(key)));
                    }
                }
            }
        } catch (java.text.ParseException e) {
            // Invalid JWT encoding
            LOGGER.error(e);
        }
        return claims;
    }

    private boolean validateClaims(JWTClaimsSet claimsSet) {
        return StringUtils.areEqual(claimsSet.getIssuer(), ssoServerUrl.replaceFirst("/$", ""))
                && claimsSet.getAudience().contains(oidcClientId)
                && claimsSet.getExpirationTime().getTime() > System.currentTimeMillis()
                && claimsSet.getIssueTime().getTime() < System.currentTimeMillis();
    }

    @NotNull
    private static RoleList getRoleList(List<String> isMemberOf) {
        RoleList roleList = new RoleList();
        for (String r : isMemberOf) {
            String cn=null, ou=null, dc=null;
            for(String s : r.split(",")) {
                String[] kv = s.split("=");
                if (kv.length > 1) {
                    if (kv[0].equalsIgnoreCase("cn")) {
                        cn = cn == null ? kv[1] : cn;
                    } else if (kv[0].equalsIgnoreCase("ou")) {
                        ou = ou == null ? kv[1] : ou;
                    } else if (kv[0].equalsIgnoreCase("dc")) {
                        dc = dc == null ? kv[1] : dc;
                    }
                }
            }
            if (dc != null && cn != null) {
                roleList.add(new RoleList.RoleEntry(dc, dc.hashCode(), cn, cn.hashCode(), ""));
            }
        }

        return roleList;
    }

}
