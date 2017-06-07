/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.filters.CommonFilter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    private static boolean authRequired;
    private static Pattern excludePattern;
    private static Pattern alwaysExcludePattern = Pattern.compile("/oidc/verify.*|.*firefly");  // ignores webwebsocket and verify (callback) entry url.

    public void init(FilterConfig filterConfig) throws ServletException {

        try {
            ServletContext cntx = filterConfig.getServletContext();
            ServerContext.init(cntx.getContextPath(), cntx.getServletContextName(), cntx.getRealPath(CommonFilter.WEBAPP_CONFIG_LOC));

            String excludes = filterConfig.getInitParameter(EXCLUDE_PATTERN);
            if (!StringUtils.isEmpty(excludes)) {
                excludePattern = Pattern.compile(excludes);
            }
            authRequired = AppProperties.getBooleanProperty("sso.auth.required", true);
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
                if (authToken == null && authRequired) {
                    if (getHeader(req, REQUEST_WITH, "").equals(AJAX_REQUEST)) {
                        // has not authenticated or session has expired..
                        // but, since ajax request will not respond to redirect.  we'll respond with a 401-Unauthorized
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
