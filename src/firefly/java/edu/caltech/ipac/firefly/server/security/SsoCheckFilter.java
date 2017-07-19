/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.data.userdata.RoleList;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.filters.CommonFilter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Date: Jun 14, 2010
 *
 * @author loi
 * @version $Id: JossoCheckFilter.java,v 1.4 2011/03/02 02:18:20 loi Exp $
 */
public class SsoCheckFilter implements Filter {
    private static final String REQUEST_WITH = "X-Requested-With";
    private static final String AJAX_REQUEST = "XMLHttpRequest";
    private static final Logger.LoggerImpl logger =  Logger.getLogger();
    private static final String EXCLUDE_PATTERN = "exclude-pattern";
    private static final String ALLOW = "allow";
    private static final String DENY = "deny";
    private static boolean authRequired;
    private static Pattern excludePattern;
    private static Pattern alwaysExcludePattern = Pattern.compile("/oidc/verify.*|.*firefly");  // ignores webwebsocket and verify (callback) entry url.
    private static List<String> allowAccess;

    public void init(FilterConfig filterConfig) throws ServletException {

        try {
            ServletContext cntx = filterConfig.getServletContext();
            ServerContext.init(cntx.getContextPath(), cntx.getServletContextName(), cntx.getRealPath(CommonFilter.WEBAPP_CONFIG_LOC));

            String excludes = filterConfig.getInitParameter(EXCLUDE_PATTERN);
            if (!StringUtils.isEmpty(excludes)) {
                excludePattern = Pattern.compile(excludes);
            }
            authRequired = AppProperties.getBooleanProperty("sso.auth.required", true);

            String allow = filterConfig.getInitParameter(ALLOW);
            if (!StringUtils.isEmpty(allow)) {
                allowAccess = StringUtils.asList(allow, ",");
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;

            if (!isExcluded(req)) {
                SsoAdapter.Token authToken = SsoAdapter.getAdapter().getAuthToken();

                if (authToken != null && authToken.getExpiresOn() < System.currentTimeMillis()) {
                    // token has expires on our end's.. should refresh it.
                    authToken = SsoAdapter.getAdapter().refreshAuthToken(authToken);
                }
                if (authRequired) {
                    if (authToken == null) {
                        if (getHeader(req, REQUEST_WITH, "").equals(AJAX_REQUEST)) {
                            // has not authenticated or session has expired..
                            // but, since ajax request will not respond to redirect.  we'll respond with a 401-Unauthorized
                            res.setHeader("WWW-Authenticate", "custom realm=\"apps\"");
                            res.sendError(401);
                            return;
                        } else {
                            // check user auth..
                            String qstr = req.getQueryString() == null ? "" : "?" + req.getQueryString();
                            res.sendRedirect(SsoAdapter.getAdapter().makeAuthCheckUrl(
                                    ServerContext.getRequestOwner().getRequestAgent().getRequestUrl() + qstr));
                            // after auth check, request will resume at SsoVerifyServlet.  no need to process this request.
                            return;
                        }
                    }
                    // is authenticated.. now check to see if user is allowed access
                    if (authRequired && allowAccess != null && allowAccess.size() > 0) {
                        UserInfo userInfo = SsoAdapter.getAdapter().getUserInfo();
                        RoleList roles = userInfo.getRoles();
                        boolean hasAccess = false;
                        for(RoleList.RoleEntry re : roles) {
                            for(String r : allowAccess) {
                                if (re.hasAccess(null, r, null)) {  // for now.. we'll only validate group name
                                    hasAccess = true;
                                    break;
                                }
                            }
                        }
                        if (!hasAccess) {
                            // has not authenticated or session has expired..
                            // but, since ajax request will not respond to redirect.  we'll respond with a 401-Unauthorized
                            res.setHeader("WWW-Authenticate", "custom realm=\"apps\"");
                            res.sendError(401);
                            return;
                        }
                    }
                }

            }

            // uri is either excluded or user is already authenticated.. allow to pass
            filterChain.doFilter(req, res);
        }
    }

    public void destroy() {

    }

    private boolean isExcluded(HttpServletRequest req) {
        String reqUri = req.getRequestURI().replaceFirst(req.getContextPath(), "");
        boolean isExcluded = alwaysExcludePattern.matcher(reqUri).matches();
        if (!isExcluded && excludePattern != null) {
            isExcluded = excludePattern.matcher(reqUri).matches();
        }
        return isExcluded;
    }

    private static String getHeader(HttpServletRequest req, String key, String def) {
        String val = req.getHeader(key);
        return StringUtils.isEmpty(val) ? def : val;
    }
}
