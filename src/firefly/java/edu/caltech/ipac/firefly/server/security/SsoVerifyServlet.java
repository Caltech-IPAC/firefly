/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.security;

import edu.caltech.ipac.firefly.server.RequestOwner;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.filters.CommonFilter;
import edu.caltech.ipac.firefly.server.servlets.BaseHttpServlet;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Date: Jun 14, 2010
 *
 * @author loi
 * @version $Id: JossoVerifyFilter.java,v 1.8 2011/06/30 23:53:06 roby Exp $
 */
public class SsoVerifyServlet extends BaseHttpServlet {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        SsoAdapter ssoAdapter = ServerContext.getRequestOwner().getSsoAdapter();
        if (ssoAdapter != null) {
            SsoAdapter.Token token = ssoAdapter.resolveAuthToken(req);
            if (token != null) {
                logger.debug("Verifying user with verId=" + token.getId() + "  ==> returned auth token:" + token);

                String backTo = ssoAdapter.getRequestedUrl(req);
                if (StringUtils.isEmpty(backTo)) {
                    backTo = ServerContext.getRequestOwner().getRequestAgent().getBaseUrl();
                }
                res.sendRedirect(backTo);
            }
        }
        logger.debug("Unable to resolve AuthToken");
        res.sendError(401);
    }
}

