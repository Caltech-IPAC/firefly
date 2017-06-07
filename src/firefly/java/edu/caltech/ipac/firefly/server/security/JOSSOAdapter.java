/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.core.JossoUtil;
import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.util.StringUtils;
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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: Mar 30, 2010
 *
 * @author loi
 * @version $Id: JOSSOAdapter.java,v 1.14 2012/10/10 21:54:45 loi Exp $
 */
public class JOSSOAdapter implements SsoAdapter {
    private static final String SSO_SERVICES_URL = AppProperties.getProperty("sso.server.url", "https://irsa.ipac.caltech.edu/account/");
    private static final String SSO_PROFILE_URL = AppProperties.getProperty("sso.user.profile.url");
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static final String REQUESTER = "JOSSOAdapter";
    private static final String JOSSO_ASSERT_ID = "josso_assertion_id";

    private static String AUTH_KEY = "JOSSO_SESSIONID";
    private static final String[] ID_COOKIE_NAMES = new String[]{AUTH_KEY, "ISIS"};
    public static String TO_BE_DELETE = "-";

    public JOSSOAdapter() {
        JossoUtil.init(SSO_SERVICES_URL, ServerContext.getContextPath(), SSO_PROFILE_URL);
    }

    /**
     * returns the number of seconds before this session expires.  0 if session is not valid, or it's already expires.
     * @param token
     * @return
     */
    public long checkSession(String token) {

        try {
            SSOSessionManager man = getIdSessLoc().getSSOSessionManagerSoap();
            SessionResponseType sessRes = man.getSession(new SessionRequestType(REQUESTER, token));
            SSOSessionType session = sessRes.getSSOSession();
            long msecLeft = session == null ? 0 :
                            session.getMaxInactiveInterval() - ((System.currentTimeMillis() - session.getLastAccessTime())/1000);
            return msecLeft;
        } catch (NoSuchSessionErrorType | SSOSessionErrorType noSuchSessionErrorType) {
            LOGGER.briefDebug("invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while accessing roles using token:" + token);
        }
        return 0;
    }

    /**
     * return all of the roles for a user authenticated with this token.
     * @param token
     * @return
     */
    public RoleList getRoles(String token) {

        RoleList roles = new RoleList();
        try {
            SSOIdentityManager man = getIdManLoc().getSSOIdentityManagerSoap();
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
            LOGGER.briefDebug("invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while accessing roles using token:" + token);
        }
        return roles;
    }

    private UserInfo getUserInfo(String token) {
        try {

            SSOIdentityManager man = getIdManLoc().getSSOIdentityManagerSoap();

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

            return userInfo;

        } catch (InvalidSessionErrorType ex) {
            LOGGER.briefDebug("invalid auth token:" + token);
        } catch (Exception e) {
            LOGGER.error(e, "Error while accessing roles using token:" + token);
        }
        return null;
    }

    public Token refreshAuthToken(Token old) {
        getUserInfo(old.getId());   // this will refresh access time for josso
        return old;
    }

    public Token resolveAuthToken(String assertionKey) {
        if (assertionKey == null) return null;
        
        try {
            SSOIdentityProvider idProv = getIdProvLoc().getSSOIdentityProviderSoap();
            ResolveAuthenticationAssertionResponseType tokenReq = idProv.resolveAuthenticationAssertion(
                                        new ResolveAuthenticationAssertionRequestType(REQUESTER, assertionKey));
            Token token = tokenReq == null ? null : new Token(tokenReq.getSsoSessionId());
            updateAuthInfo(token);
            return token;
        } catch (AssertionNotValidErrorType | SSOIdentityProviderErrorType ex) {
            LOGGER.briefDebug("invalid assertion token:" + assertionKey);
        } catch (Exception e) {
            LOGGER.error(e, "Error while resolving auth token using assertKey:" + assertionKey);
        }
        return null;
    }

    public boolean logout(String token) {
        try {
            if (!StringUtils.isEmpty(token)) {
                SSOIdentityProvider idProv = getIdProvLoc().getSSOIdentityProviderSoap();
                idProv.globalSignoff(new GlobalSignoffRequestType(REQUESTER, token));
                return true;
            }
        } catch (SSOIdentityProviderErrorType ssoIdentityProviderErrorType) {
            LOGGER.briefDebug("logout failed... most likey invalid auth token:" + token);
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

    public String createSession(String name, String passwd) {
        try {
            SSOIdentityProvider idProv = getIdProvLoc().getSSOIdentityProviderSoap();
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

    public String getAssertKey() {
        return JOSSO_ASSERT_ID;
    }

    public String getRequestedUrl(HttpServletRequest req) {
        String backTo = req.getParameter(JossoUtil.VERIFIER_BACK_TO);
        if (StringUtils.isEmpty(backTo)) {
            String path = req.getRequestURL().toString();
            backTo = path.substring(0, path.indexOf(req.getContextPath()));
            String qstr = req.getQueryString() == null ? "" : "?" + req.getQueryString();
            backTo = backTo + "/" + req.getContextPath() + qstr;

        } else {
            backTo = Base64.decode(backTo);
        }
        return backTo;
    }

    public String makeAuthCheckUrl(String backTo) {
        return JossoUtil.makeAuthCheckUrl(backTo);
    }

    public Map<String, String> getIdentities() {
        HashMap<String, String> idCookies = new HashMap<String, String>();
        RequestAgent http = ServerContext.getRequestOwner().getRequestAgent();
        for (String name : ID_COOKIE_NAMES) {
            String value = http.getCookieVal(name);
            if (!StringUtils.isEmpty(value)) {
                idCookies.put(name, value);
            }
        }
        return idCookies.size() == 0 ? null : idCookies;
    }

    public UserInfo getUserInfo() {
        String authToken = getAuthTokenId();
        return StringUtils.isEmpty(authToken) ? null : getUserInfo(authToken);
    }

    public Token getAuthToken() {
        Cookie authCookie = ServerContext.getRequestOwner().getRequestAgent().getCookie(AUTH_KEY);
        Token token = new Token(getAuthTokenId());
        token.setExpiresOn(System.currentTimeMillis() + authCookie.getMaxAge() * 1000);
        return token;
    }

    public String getAuthTokenId() {
        return ServerContext.getRequestOwner().getRequestAgent().getCookieVal(AUTH_KEY);
    }

    public void clearAuthInfo() {
        if (getAuthTokenId() != null) {
            Cookie c = new Cookie(AUTH_KEY, "");
            c.setMaxAge(0);
            c.setValue(TO_BE_DELETE);
            c.setDomain(".ipac.caltech.edu");
            c.setPath("/");
            ServerContext.getRequestOwner().getRequestAgent().sendCookie(c);
        }
    }


//====================================================================
//
//====================================================================

    private void updateAuthInfo(Token authToken) {
        String authTokenId = authToken == null ? null : authToken.getId();
        Cookie c = new Cookie(AUTH_KEY, authTokenId);
        c.setMaxAge(authToken == null ? 0 : 60 * 60 * 24 * 14);
        c.setValue(authTokenId);
        c.setPath("/");
        ServerContext.getRequestOwner().getRequestAgent().sendCookie(c);
    }

    private static SSOIdentityManagerWSLocator getIdManLoc() {
        String ssoServicesUrl = SSO_SERVICES_URL;
        if (!SSO_SERVICES_URL.startsWith("http")) {
            ssoServicesUrl = ServerContext.getRequestOwner().getHostUrl() + SSO_SERVICES_URL;
        }
        SSOIdentityManagerWSLocator idManLoc = new SSOIdentityManagerWSLocator();
        idManLoc.setSSOIdentityManagerSoapEndpointAddress(ssoServicesUrl + "services/SSOIdentityManagerSoap");
        return idManLoc;
    }

    private static SSOIdentityProviderWSLocator getIdProvLoc() {
        String ssoServicesUrl = SSO_SERVICES_URL;
        if (!SSO_SERVICES_URL.startsWith("http")) {
            ssoServicesUrl = ServerContext.getRequestOwner().getHostUrl() + SSO_SERVICES_URL;
        }
        SSOIdentityProviderWSLocator idProvLoc = new SSOIdentityProviderWSLocator();
        idProvLoc.setSSOIdentityProviderSoapEndpointAddress(ssoServicesUrl + "services/SSOIdentityProviderSoap");
        return idProvLoc;
    }

    private static SSOSessionManagerWSLocator getIdSessLoc() {
        String ssoServicesUrl = SSO_SERVICES_URL;
        if (!SSO_SERVICES_URL.startsWith("http")) {
            ssoServicesUrl = ServerContext.getRequestOwner().getHostUrl() + SSO_SERVICES_URL;
        }
        SSOSessionManagerWSLocator idSessLoc = new SSOSessionManagerWSLocator();
        idSessLoc.setSSOSessionManagerSoapEndpointAddress(ssoServicesUrl + "services/SSOSessionManagerSoap");
        return idSessLoc;
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
