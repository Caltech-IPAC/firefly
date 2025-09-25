/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.core.JossoUtil;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.util.StringUtils;
import org.apache.axis.client.Stub;
import org.apache.axis.transport.http.HTTPConstants;
import org.josso.gateway.ws._1_2.protocol.AssertIdentityWithSimpleAuthenticationRequestType;
import org.josso.gateway.ws._1_2.protocol.AssertIdentityWithSimpleAuthenticationResponseType;
import org.josso.gateway.ws._1_2.protocol.AssertionNotValidErrorType;
import org.josso.gateway.ws._1_2.protocol.FindRolesBySSOSessionIdRequestType;
import org.josso.gateway.ws._1_2.protocol.FindRolesBySSOSessionIdResponseType;
import org.josso.gateway.ws._1_2.protocol.FindUserInSessionRequestType;
import org.josso.gateway.ws._1_2.protocol.FindUserInSessionResponseType;
import org.josso.gateway.ws._1_2.protocol.GlobalSignoffRequestType;
import org.josso.gateway.ws._1_2.protocol.InvalidSessionErrorType;
import org.josso.gateway.ws._1_2.protocol.NoSuchSessionErrorType;
import org.josso.gateway.ws._1_2.protocol.ResolveAuthenticationAssertionRequestType;
import org.josso.gateway.ws._1_2.protocol.ResolveAuthenticationAssertionResponseType;
import org.josso.gateway.ws._1_2.protocol.SSOIdentityProviderErrorType;
import org.josso.gateway.ws._1_2.protocol.SSONameValuePairType;
import org.josso.gateway.ws._1_2.protocol.SSORoleType;
import org.josso.gateway.ws._1_2.protocol.SSOSessionErrorType;
import org.josso.gateway.ws._1_2.protocol.SSOSessionType;
import org.josso.gateway.ws._1_2.protocol.SSOUserType;
import org.josso.gateway.ws._1_2.protocol.SessionRequestType;
import org.josso.gateway.ws._1_2.protocol.SessionResponseType;
import org.josso.gateway.ws._1_2.wsdl.SSOIdentityManager;
import org.josso.gateway.ws._1_2.wsdl.SSOIdentityManagerWSLocator;
import org.josso.gateway.ws._1_2.wsdl.SSOIdentityProvider;
import org.josso.gateway.ws._1_2.wsdl.SSOIdentityProviderWSLocator;
import org.josso.gateway.ws._1_2.wsdl.SSOSessionManager;
import org.josso.gateway.ws._1_2.wsdl.SSOSessionManagerWSLocator;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.rpc.ServiceException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.util.StringUtils.isEmpty;

/**
 * Date: Mar 30, 2010
 *
 * @author loi
 * @version $Id: JOSSOAdapter.java,v 1.14 2012/10/10 21:54:45 loi Exp $
 */
public class JOSSOAdapter implements SsoAdapter {
    private static final String SSO_SERVICES_URL = AppProperties.getProperty("sso.server.url", "https://irsa.ipac.caltech.edu/account/");
    private static final String SSO_PROFILE_URL = AppProperties.getProperty("sso.user.profile.url");
    private static final boolean SEND_USER_ID = AppProperties.getBooleanProperty("sso.send.user.id", false);
    private static final int EXPIRY_TIME = AppProperties.getIntProperty("sso.cache.expiry", 60*5) * 1000;        // cache UserInfo to avoid excessive calls to JOSSO.  Default to 5 minutes.
    private static final String[] REQ_AUTH_HOSTS = AppProperties.getProperty("sso.req.auth.hosts", "ipac.caltech.edu").split(",");
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static final String REQUESTER = "JOSSOAdapter";
    private static final String JOSSO_ASSERT_ID = "josso_assertion_id";

    private static String AUTH_KEY = "JOSSO_SESSIONID";
    private static final String[] ID_COOKIE_NAMES = new String[]{AUTH_KEY, "ISIS"};
    public static String TO_BE_DELETE = "-";
    private static final Map<String, CachedUserInfo> cache = new ConcurrentHashMap<>();
    private static volatile long lastCleanup = 0;
    private static String ssoServicesUrl = null;


    public JOSSOAdapter() {
        JossoUtil.init(SSO_SERVICES_URL, ServerContext.getContextPath(), SSO_PROFILE_URL);
    }

    /**
     * returns the number of seconds before this session expires.  0 if session is not valid, or it's already expires.
     * @param token
     * @return
     */
    private long checkSession(String token) {

        try {
            SSOSessionManager man = getIdSess();
            SessionResponseType sessRes = man.getSession(new SessionRequestType(REQUESTER, token));
            SSOSessionType session = sessRes.getSSOSession();
            long msecLeft = session == null ? 0 :
                            session.getMaxInactiveInterval() - ((System.currentTimeMillis() - session.getLastAccessTime())/1000);
            return msecLeft;
        } catch (NoSuchSessionErrorType | SSOSessionErrorType noSuchSessionErrorType) {
            LOGGER.debug("invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while accessing roles using token:" + token);
        }
        return 0;
    }

    /**
     * return all the roles for a user authenticated with this token.
     * @param token
     * @return
     */
    private RoleList getRoles(String token) {

        RoleList roles = new RoleList();
        try {
            SSOIdentityManager man = getIdMan();
            FindRolesBySSOSessionIdResponseType roleWrap = man.findRolesBySSOSessionId(
                                new FindRolesBySSOSessionIdRequestType(REQUESTER, token));
            SSORoleType[] roleTypes = roleWrap.getRoles();

            if (roleTypes != null) {
                for (SSORoleType r : roleTypes) {
                    RoleList.RoleEntry role = RoleList.RoleEntry.parse(r.getName());
                    if (role != null) {
                        roles.add(role);
                    }
                }
            }
        } catch (InvalidSessionErrorType ex) {
            LOGGER.debug("invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while accessing roles using token:" + token);
        }
        return roles;
    }

    private UserInfo getUserInfo(String token) {

        if (isEmpty(token)) return null;

        long now = System.currentTimeMillis();
        if (now - lastCleanup > 60 * 60 * 1000) { // cleanup every 1 hour
            cache.entrySet().removeIf(e -> e.getValue().isExpired(now));
            lastCleanup = now;
        }

        CachedUserInfo cachedUserInfo = cache.get(token);
        if (cachedUserInfo != null && !cachedUserInfo.isExpired(now)) {
            return cachedUserInfo.userInfo;
        }

        try {

            SSOIdentityManager man = getIdMan();

            FindUserInSessionResponseType userWrap = man.findUserInSession(
                    new FindUserInSessionRequestType(REQUESTER, token));
            SSOUserType user = userWrap.getSSOUser();

            UserInfo userInfo = new UserInfo();
            userInfo.setLoginName(user.getName());
            userInfo.setRoles(getRoles(token));

            SSONameValuePairType[] props = user.getProperties();
            if (props !=null) {
                for (SSONameValuePairType pp : props) {
                    userInfo.setProperty(pp.getName(), pp.getValue());
                }
            }
            cache.put(token, new CachedUserInfo(userInfo, now));
            return userInfo;

        } catch (InvalidSessionErrorType ex) {
            LOGGER.debug("invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while accessing roles using token:" + token);
        }
        return null;
    }

    public Token refreshAuthToken(Token old) {
        getUserInfo(old.getId());   // this will refresh access time for josso
        return old;
    }

    public Token resolveAuthToken(HttpServletRequest req) {
        return resolveAuthToken(req.getParameter(JOSSO_ASSERT_ID));
    }

    private Token resolveAuthToken(String assertionKey) {

        if (assertionKey == null) return null;
        
        try {
            SSOIdentityProvider idProv = getIdProv();
            ResolveAuthenticationAssertionResponseType tokenReq = idProv.resolveAuthenticationAssertion(
                                        new ResolveAuthenticationAssertionRequestType(REQUESTER, assertionKey));
            Token token = tokenReq == null ? null : new Token(tokenReq.getSsoSessionId());
            updateAuthInfo(token);
            return token;
        } catch (AssertionNotValidErrorType | SSOIdentityProviderErrorType ex) {
            LOGGER.debug("invalid assertion token:" + assertionKey);
        } catch (Exception e) {
            LOGGER.error(e, "Error while resolving auth token using assertKey:" + assertionKey);
        }
        return null;
    }

    public boolean logout() {
        return logout(getAuthTokenId());
    }

    private boolean logout(String token) {
        try {
            if (!isEmpty(token)) {
                SSOIdentityProvider idProv = getIdProv();
                idProv.globalSignoff(new GlobalSignoffRequestType(REQUESTER, token));
                return true;
            }
        } catch (SSOIdentityProviderErrorType ssoIdentityProviderErrorType) {
            LOGGER.debug("logout failed... most likey invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while logging out using token:" + token);
        }
        return false;
    }

    public UserInfo login(String name, String passwd) {
        String token = createSession(name, passwd);
        UserInfo user = getUserInfo(token);
        return user;
    }

    private String createSession(String name, String passwd) {
        try {
            SSOIdentityProvider idProv = getIdProv();
            AssertIdentityWithSimpleAuthenticationResponseType rval = idProv.assertIdentityWithSimpleAuthentication(
                    new AssertIdentityWithSimpleAuthenticationRequestType(REQUESTER, "josso", name, passwd));
            String assertId = rval.getAssertionId();
            Token token = resolveAuthToken(assertId);
            return token == null ? null : token.getId();
        } catch (RemoteException | ServiceException e) {
            LOGGER.error(e);
        }
        return null;
    }

    public String getRequestedUrl(HttpServletRequest req) {
        String backTo = req.getParameter(JossoUtil.VERIFIER_BACK_TO);
        if (isEmpty(backTo)) {
            String path = req.getRequestURL().toString();
            backTo = path.substring(0, path.indexOf(req.getContextPath()));
            String qstr = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            backTo = backTo + "/" + req.getContextPath() + qstr;

        } else {
            backTo = Base64.decode(backTo);
        }
        return backTo;
    }

    public String getLoginUrl(String backTo) {
//        return JossoUtil.makeAuthCheckUrl(backTo);
        return JossoUtil.makeLoginUrl(backTo);
    }

    public String getProfileUrl(String backTo) {
        return SSO_PROFILE_URL;
    }

    public void setAuthCredential(HttpServiceInput inputs) {
        RequestAgent http = ServerContext.getRequestOwner().getRequestAgent();
        if(http!=null){
            if (SsoAdapter.requireAuthCredential(inputs.getRequestUrl(), REQ_AUTH_HOSTS)) {
                applyIfNotEmpty(http.getHeader("Authorization"), (v) -> inputs.setHeader("Authorization", v));      // pass along authorization header; this includes more than just basic-auth. should this in mind.
                if (SEND_USER_ID) {
                    UserInfo uInfo = ServerContext.getRequestOwner().getUserInfo();
                    String userId = uInfo.isGuestUser() ? ServerContext.getRequestOwner().getUserKey() : uInfo.getLoginName();
                    inputs.setHeader("X-Remote-User", userId);        // will remove this header later
                    inputs.setHeader("X-User-Id", userId);
                }
                for (String name : ID_COOKIE_NAMES) {
                    String value = http.getCookieVal(name);
                    if (!isEmpty(value)) {
                        inputs.setCookie(name, value);
                    }
                }
            }
        }
    }

    /**
     * @return the authenticated user info, or null if not authenticated.
     */
    public UserInfo getUserInfo() {
        String authToken = getAuthTokenId();
        if (isEmpty(authToken)) return null;

        UserInfo userInfo = getUserInfo(authToken);
        if (userInfo == null) clearAuthInfo();
        return userInfo;
    }

    public Token getAuthToken() {
        Cookie authCookie = ServerContext.getRequestOwner().getRequestAgent().getCookie(AUTH_KEY);
        if (!isEmpty(authCookie)) {
            Token token = new Token(getAuthTokenId());
            token.setExpiresOn(System.currentTimeMillis() + authCookie.getMaxAge() * 1000);
            return token;
        } else {
            return null;
        }
    }

    private String getAuthTokenId() {
        return ServerContext.getRequestOwner().getRequestAgent().getCookieVal(AUTH_KEY);
    }

    public void clearAuthInfo() {
        if (getAuthTokenId() != null) {
            Cookie c = new Cookie(AUTH_KEY, "");
            c.setMaxAge(0);
            c.setValue(TO_BE_DELETE);
            c.setDomain("ipac.caltech.edu");
            c.setPath("/");
            ServerContext.getRequestOwner().getRequestAgent().sendCookie(c);
        }
    }


//====================================================================
//
//====================================================================

    private void updateAuthInfo(Token authToken) {
        RequestAgent agent = ServerContext.getRequestOwner().getRequestAgent();
        if (agent != null) {
            String authTokenId = authToken == null ? null : authToken.getId();
            Cookie c = new Cookie(AUTH_KEY, authTokenId);
            c.setMaxAge(authToken == null ? 0 : 60 * 60 * 24 * 14);
            c.setValue(authTokenId);
            c.setPath("/");
            agent.sendCookie(c);
        }
    }

    private static String getSsoServicesUrl() {
        if (ssoServicesUrl == null) {
            ssoServicesUrl = SSO_SERVICES_URL.startsWith("http")
                    ? SSO_SERVICES_URL
                    : ServerContext.getRequestOwner().getHostUrl() + SSO_SERVICES_URL;
        }
        return ssoServicesUrl;
    }

    private static void checkSoapAuth(Remote remote) {
        if (remote instanceof Stub stub) {
            HttpServiceInput input = new HttpServiceInput(getSsoServicesUrl());
            Map<String, String> headers = input.getRequestHeaders();
            if (!headers.isEmpty()) stub._setProperty(HTTPConstants.REQUEST_HEADERS, input.getRequestHeaders() );
        }
    }

    private static SSOIdentityManager getIdMan() throws ServiceException {
        String endpoint = getSsoServicesUrl() + "services/SSOIdentityManagerSoap";
        SSOIdentityManagerWSLocator idManLoc = new SSOIdentityManagerWSLocator();
        idManLoc.setSSOIdentityManagerSoapEndpointAddress(endpoint);
        LOGGER.debug("JOSSO IdentityManager endpoint:" + endpoint);

        SSOIdentityManager man = idManLoc.getSSOIdentityManagerSoap();
        checkSoapAuth(man);
        return man;
    }

    private static SSOIdentityProvider getIdProv() throws ServiceException {
        String endpoint = getSsoServicesUrl() + "services/SSOIdentityProviderSoap";
        SSOIdentityProviderWSLocator idProvLoc = new SSOIdentityProviderWSLocator();
        idProvLoc.setSSOIdentityProviderSoapEndpointAddress(endpoint);
        LOGGER.debug("JOSSO IdentityProvider endpoint:" + endpoint);

        SSOIdentityProvider prov = idProvLoc.getSSOIdentityProviderSoap();
        checkSoapAuth(prov);
        return prov;
    }

    private static SSOSessionManager getIdSess() throws ServiceException {
        String endpoint = getSsoServicesUrl() + "services/SSOSessionManagerSoap";
        SSOSessionManagerWSLocator idSessLoc = new SSOSessionManagerWSLocator();
        idSessLoc.setSSOSessionManagerSoapEndpointAddress(endpoint);
        LOGGER.debug("JOSSO SessionManager endpoint:" + endpoint);

        SSOSessionManager man = idSessLoc.getSSOSessionManagerSoap();
        checkSoapAuth(man);
        return man;
    }

    private static class CachedUserInfo {
        final UserInfo userInfo;
        final long timestamp;
        CachedUserInfo(UserInfo userInfo, long timestamp) {
            this.userInfo = userInfo;
            this.timestamp = timestamp;
        }
        boolean isExpired(long now) {
            return now - timestamp > EXPIRY_TIME;
        }
    }

//====================================================================
//  main for testing only
//====================================================================

    public static void main(String[] args) {

        if (args.length > 1) {
            JOSSOAdapter adapter = new JOSSOAdapter();
            String token = adapter.createSession(args[0], args[1]);
            System.out.println("token/checkSession:" + token + "/" + adapter.checkSession(token));

            if (token != null) {
                long startTime = System.currentTimeMillis();
                UserInfo user = adapter.getUserInfo(token);
                RoleList roles = adapter.getRoles(token);
                System.out.println("elapsed time(ms):" + (System.currentTimeMillis() - startTime));
                if (user != null) {
                    System.out.println("Login Name:" + user.getLoginName());
                    System.out.println("First Name:" + user.getFirstName());
                    System.out.println("Last Name:" + user.getLastName());
                    System.out.println("Email:" + user.getEmail());
                    System.out.println("Roles:" + roles.size());
                    System.out.println(StringUtils.toString(roles, "\n"));

                    System.out.println("Test cases:");
                    System.out.println("hasAccess SPITZER:1148: = " + roles.hasAccess("SPITZER:1148:"));
                    System.out.println("hasAccess SPITZER:40018:READ = " + roles.hasAccess("SPITZER:40018:READ"));
                    System.out.println("hasAccess SPITZER:40018:DOWNLOAD = " + roles.hasAccess("SPITZER:40018:DOWNLOAD"));
                    System.out.println("hasAccess SPITZER:: = " + roles.hasAccess("SPITZER::"));

                    System.out.println("hasAccess (79):(1148): = " + roles.hasAccess("(79):(1148):"));
                    System.out.println("hasAccess (79)::READ = " + roles.hasAccess("(79)::"));


                    System.out.println("hasAccess WISE:: = " + roles.hasAccess("WISE::"));
                    System.out.println("hasAccess USER:: = " + roles.hasAccess("USER::"));
                    System.out.println("hasAccess :: = " + roles.hasAccess("::"));


                    boolean isLogout = adapter.logout(token);
                    System.out.println("Logout successful:" + isLogout);
                    System.out.println("token/checkSession:" + token + "/" + adapter.checkSession(token));
                }
            }
        }

    }

}
