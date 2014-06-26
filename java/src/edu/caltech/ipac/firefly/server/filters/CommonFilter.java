package edu.caltech.ipac.firefly.server.filters;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.EhcacheProvider;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
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
        System.setProperty(WEBAPP_CONFIG_DIR, filterConfig.getServletContext().getRealPath("WEB-INF/config"));
        String appName = filterConfig.getServletContext().getServletContextName();
        System.setProperty(APP_NAME, appName);
        if (!isInit) {
            ServerContext.init(); // just a way to initializes ServerContext
            isInit = true;
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

        RequestOwner owner = ServerContext.getRequestOwner();   // establish a new one.
        owner.setHttpRequest(request);
        owner.setHttpResponse(response);

    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/