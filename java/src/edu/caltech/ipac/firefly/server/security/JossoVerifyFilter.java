package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.util.Base64;
import edu.caltech.ipac.firefly.core.JossoUtil;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.StringUtils;

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
 * Date: Jun 14, 2010
 *
 * @author loi
 * @version $Id: JossoVerifyFilter.java,v 1.8 2011/06/30 23:53:06 roby Exp $
 */
public class JossoVerifyFilter implements Filter {
    public static final String VERIFY_TOKEN = "AUTH_VERIFIED";
    private static final String JOSSO_ASSERT_ID = "josso_assertion_id";
    private static final Logger.LoggerImpl logger = Logger.getLogger();
    private static FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        JossoVerifyFilter.filterConfig = filterConfig;
        JossoUtil.init(
                AppProperties.getProperty("sso.server.url"),
                filterConfig.getServletContext().getContextPath(),
                AppProperties.getProperty("sso.user.profile.url")
        );
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;

            String id = req.getParameter(JOSSO_ASSERT_ID);
            String token = JOSSOAdapter.resolveAuthToken(id);
            WebAuthModule.updateAuthKey(token, resp);
            logger.briefInfo("Verifying user with verId=" + id + "  ==> returned auth token:" + token);

//            createVerifyCookie(resp);

            String backTo = req.getParameter(JossoUtil.VERIFIER_BACK_TO);
            if (StringUtils.isEmpty(backTo)) {
                String path = req.getRequestURL().toString();
                backTo = path.substring(0, path.indexOf(req.getContextPath()));
                String qstr = req.getQueryString() == null ? "" : "?" + req.getQueryString();
                backTo = backTo + "/" + req.getContextPath() + qstr;

            } else {
                backTo = Base64.decode(backTo);
            }

            resp.sendRedirect(backTo);
        }
    }

//    public static Cookie getVerifyCookie(HttpServletRequest req) {
//        return WebAuthModule.getCookie(VERIFY_TOKEN, req);
//    }
//
//    public static void removeVerifyCookie(HttpServletRequest req, HttpServletResponse resp) {
//        Cookie c = getVerifyCookie(req);
//        c.setValue("false");
//        c.setMaxAge(0);
//        c.setPath(filterConfig.getServletContext().getContextPath());
//        resp.addCookie(c);
//    }
//
    public void destroy() {
    }
//
//    private void createVerifyCookie(HttpServletResponse resp) {
//        Cookie c = new Cookie(VERIFY_TOKEN, "true");
//        c.setMaxAge(-1);
//        c.setPath(filterConfig.getServletContext().getContextPath());
//        resp.addCookie(c);
//    }
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
