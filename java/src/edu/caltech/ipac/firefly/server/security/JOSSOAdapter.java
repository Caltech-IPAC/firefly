package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
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

import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException;

/**
 * Date: Mar 30, 2010
 *
 * @author loi
 * @version $Id: JOSSOAdapter.java,v 1.14 2012/10/10 21:54:45 loi Exp $
 */
public class JOSSOAdapter {
    private static final String SSO_SERVICES_URL = AppProperties.getProperty("sso.server.url",
                                                    "http://***REMOVED***/account/");
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    private static final String REQUESTER = "JOSSOAdapter";


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

    /**
     * returns the number of seconds before this session expires.  0 if session is not valid, or it's already expires.
     * @param token
     * @return
     */
    public static long checkSession(String token) {

        try {
            SSOSessionManager man = getIdSessLoc().getSSOSessionManagerSoap();
            SessionResponseType sessRes = man.getSession(new SessionRequestType(REQUESTER, token));
            SSOSessionType session = sessRes.getSSOSession();
            long msecLeft = session == null ? 0 :
                            session.getMaxInactiveInterval() - ((System.currentTimeMillis() - session.getLastAccessTime())/1000);
            return msecLeft;
        } catch (NoSuchSessionErrorType noSuchSessionErrorType) {
            LOGGER.briefDebug("invalid auth token:" + token);
        } catch (SSOSessionErrorType ssoSessionErrorType) {
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
    public static RoleList getRoles(String token) {

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

    public static UserInfo getUserInfo(String token) {
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

    public static String resolveAuthToken(String assertionKey) {
        if (assertionKey == null) return null;
        
        try {
            SSOIdentityProvider idProv = getIdProvLoc().getSSOIdentityProviderSoap();
            ResolveAuthenticationAssertionResponseType tokenReq = idProv.resolveAuthenticationAssertion(
                                        new ResolveAuthenticationAssertionRequestType(REQUESTER, assertionKey));
            if (tokenReq != null) {
                return tokenReq.getSsoSessionId();
            }
        } catch (AssertionNotValidErrorType ex) {
            LOGGER.briefDebug("invalid assertion token:" + assertionKey);
        } catch (SSOIdentityProviderErrorType ex) {
            LOGGER.briefDebug("invalid assertion token:" + assertionKey);
        } catch (Exception e) {
            LOGGER.error(e, "Error while resolving auth token using assertKey:" + assertionKey);
        }
        return null;
    }

    public static boolean logout(String token) {
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

    public static UserInfo login(String name, String passwd) {
        String token = createSession(name, passwd);
        UserInfo user = getUserInfo(token);
        return user;
    }

    public static String createSession(String name, String passwd) {
        try {
            SSOIdentityProvider idProv = getIdProvLoc().getSSOIdentityProviderSoap();
            AssertIdentityWithSimpleAuthenticationResponseType rval = idProv.assertIdentityWithSimpleAuthentication(
                    new AssertIdentityWithSimpleAuthenticationRequestType(REQUESTER, "josso", name, passwd));
            String assertId = rval.getAssertionId();
            String token = resolveAuthToken(assertId);
            return token;
        } catch (RemoteException e) {
            LOGGER.error(e);
        } catch (ServiceException e) {
            LOGGER.error(e);
        }
        return null;
    }

    public static void main(String[] args) {

        if (args.length > 1) {
            String token = createSession(args[0], args[1]);
            System.out.println("token/checkSession:" + token + "/" + checkSession(token));

            if (token != null) {
                long startTime = System.currentTimeMillis();
                UserInfo user = getUserInfo(token);
                RoleList roles = getRoles(token);
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


                    boolean isLogout = logout(token);
                    System.out.println("Logout successful:" + isLogout);
                    System.out.println("token/checkSession:" + token + "/" + checkSession(token));
                }
            }
        }

    }

}
