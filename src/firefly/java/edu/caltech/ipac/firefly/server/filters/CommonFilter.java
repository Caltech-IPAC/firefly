/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.filters;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.StopWatch;

import edu.caltech.ipac.util.AppProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;


/**
 * Date: Jul 9, 2008
 *
 * @author loi
 * @version $Id: CommonFilter.java,v 1.39 2012/09/07 18:04:02 loi Exp $
 */
public class CommonFilter implements Filter {

    private static final boolean ALLOW_CROSS_ORIGIN = AppProperties.getBooleanProperty("allow.cross.origin", true);

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpReq) {
            try {
                ServerContext.clearRequestOwner();
                setupRequestOwner(httpReq, (HttpServletResponse)response);

                filterChain.doFilter( request, response );
            } finally {
                if (ALLOW_CROSS_ORIGIN) {
                    applyCookieAttributes((HttpServletResponse)response, Set.of("JSESSIONID", "usrkey"));
                }
                // clean up ThreadLocal instances.
                StopWatch.clear();
            }
        }
    }

    public void destroy() {}

    public static void setupRequestOwner(HttpServletRequest request, HttpServletResponse response) {
        // Initialize the RequestOwner for the current request.
        ServerContext.getRequestOwner().init(ServerContext.getHttpRequestAgent(request, response));
    }

    public static void applyCookieAttributes(HttpServletResponse response, Set<String> cookieNames) {
        Collection<String> headers = response.getHeaders("Set-Cookie");
        boolean first = true;
        for (String header : headers) {
            String modified = header;
            String lowerCase = header.toLowerCase();
            boolean matches = cookieNames.stream()
                    .anyMatch(name -> lowerCase.contains(name.toLowerCase()));
            if (matches) {
                if (!lowerCase.contains("samesite")) {
                    modified = modified + "; SameSite=None";
                }
                if (!lowerCase.contains("secure")) {
                    modified = modified + "; Secure";
                }
                if (!lowerCase.contains("httponly")) {
                    modified = modified + "; HttpOnly";
                }
            }
            if (first) {
                response.setHeader("Set-Cookie", modified);
                first = false;
            } else {
                response.addHeader("Set-Cookie", modified);
            }
        }
    }

}
