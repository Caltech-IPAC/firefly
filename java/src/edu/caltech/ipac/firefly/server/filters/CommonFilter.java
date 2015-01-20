/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.filters;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.CacheManager;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
    public static final String WEBAPP_CONFIG_DIR = "webapp-confi-dir";
    public static final String APP_NAME = "app.name";
    private static final Logger.LoggerImpl logger =  Logger.getLogger();
    boolean isInit = false;


    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            System.setProperty(WEBAPP_CONFIG_DIR, filterConfig.getServletContext().getRealPath("WEB-INF/config"));
            String appName = filterConfig.getServletContext().getServletContextName();
            System.setProperty(APP_NAME, appName);
            if (!isInit) {
                ServerContext.init(); // just a way to initializes ServerContext
                isInit = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            setupRequestOwner(httpReq, (HttpServletResponse)response);
        }
        filterChain.doFilter( request, response );
        // clean up ThreadLocal instances.
        ServerContext.clearRequestOwner();
        StopWatch.clear();
    }

    public void destroy() {
        ((EhcacheProvider)CacheManager.getCacheProvider()).shutdown();
    }

    private void setupRequestOwner(HttpServletRequest request, HttpServletResponse response) {

        String sessId = request.getRequestedSessionId();
        if (StringUtils.isEmpty(sessId)) {
            // create a session if one if not in the request.
            // this is needed for session stickiness.
            sessId = request.getSession().getId();
        }

        RequestOwner owner = ServerContext.getRequestOwner();   // establish a new one.
        owner.setHttpRequest(request);
        owner.setHttpResponse(response);

    }

}
