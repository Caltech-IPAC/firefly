package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.core.JossoUtil;
import edu.caltech.ipac.firefly.server.util.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Date: Jun 14, 2010
 *
 * @author loi
 * @version $Id: JossoCheckFilter.java,v 1.4 2011/03/02 02:18:20 loi Exp $
 */
public class JossoCheckFilter implements Filter {
    private static final Logger.LoggerImpl logger =  Logger.getLogger();

    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;

            Cookie c = WebAuthModule.getAuthCookie(req);
            if (c != null) {
                // already authenticated.. allow to pass
                filterChain.doFilter(req, resp);
            } else {
                // check user auth..
                String qstr = req.getQueryString() == null ? "" : "?" + req.getQueryString();
                resp.sendRedirect(JossoUtil.makeAuthCheckUrl(req.getRequestURL().toString() + qstr));
            }
        }
    }

    public void destroy() {}

}
