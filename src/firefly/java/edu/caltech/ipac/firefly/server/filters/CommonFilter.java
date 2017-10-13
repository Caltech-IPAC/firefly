/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.filters;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.cache.CacheManager;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Date: Jul 9, 2008
 *
 * @author loi
 * @version $Id: CommonFilter.java,v 1.39 2012/09/07 18:04:02 loi Exp $
 */
public class CommonFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        ServerContext.clearRequestOwner();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            setupRequestOwner(httpReq, (HttpServletResponse)response);
        }
        filterChain.doFilter( request, response );
        // clean up ThreadLocal instances.
        StopWatch.clear();
    }

    public void destroy() {
        ((EhcacheProvider)CacheManager.getCacheProvider()).shutdown();
    }

    public static void setupRequestOwner(HttpServletRequest request, HttpServletResponse response) {

        RequestOwner owner = ServerContext.getRequestOwner();   // establish a new one.
        owner.setRequestAgent(ServerContext.getHttpRequestAgent(request, response));
        owner.getUserKey();

    }

}
