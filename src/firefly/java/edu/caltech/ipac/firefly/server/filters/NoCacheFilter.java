/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Date: Jul 9, 2008
 *
 * @author loi
 * @version $Id: NoCacheFilter.java,v 1.2 2012/09/17 22:59:39 loi Exp $
 */
public class NoCacheFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setDateHeader("Expires", 0);
            res.setDateHeader("Last-Modified", new Date().getTime());
            res.setHeader("Cache-Control", "no-store, no-cache, max-age=0");
            res.setHeader("Pragma", "no-cache");
        }
        filterChain.doFilter( request, response );
    }

    public void destroy() {
    }

}

