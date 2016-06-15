/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.FileUtil;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * $Id: SearchServicesImpl.java,v 1.14 2012/10/03 22:18:11 loi Exp $
 */

@MultipartConfig
public class JsonSearchServices extends BaseHttpServlet {

    @Override
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        TableServerRequest tsr = QueryUtil.convertToServerRequest(req);
        try {
            String jsonStr = new SearchManager().handleJsonRequest(tsr);
            res.setContentType("application/json");
            FileUtil.writeStringToStream(jsonStr, res.getOutputStream());

        } catch (Throwable e) {
            throw createException(e, tsr);
        }
    }

    protected EndUserException createException(Throwable e, TableServerRequest tsr) {
        e.printStackTrace();

        for (Throwable t = e.getCause(); (t != null); t = t.getCause()) {
            if (t instanceof EndUserException) {
                return (EndUserException) t;
            }
        }
        return new EndUserException(String.format("The call failed on the server. request=%s", tsr.toString()), e.getMessage());
    }

}
