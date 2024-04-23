/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class CorsFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {}

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if ( request instanceof HttpServletRequest req &&
             response instanceof HttpServletResponse res) {

            enableCors(req, res);
            if (req.getMethod().equals("OPTIONS")) {
                res.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
        }

        filterChain.doFilter( request, response );
    }

    public void destroy() {}


    public static void enableCors(HttpServletRequest req, HttpServletResponse resp) {
        if (req.getHeader("Origin") != null) {
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
            resp.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
            resp.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            resp.setHeader("Access-Control-Max-Age", "86400");      // cache for 1 day
        }
    }
}
