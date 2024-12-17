/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.db.DbAdapter;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Check application health by performing a simple task, then return HTTP code 200 if succeed.
 *
 * optional parameters:
 *  mem=<size-in-mb>    : amount of memory to allocate, between 1-1024 MB; default to 5
 *
 * Date: Oct 12, 2020
 *
 * @author loi
 * @version $Id: ServerStatus.java,v 1.1 2009/06/04 00:12:42 loi Exp $
 */
public class HealthCheck extends BaseHttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        byte[] alloc = null;
        try {
            // memory check
            int mem = Math.max(Math.min(StringUtils.getInt(req.getParameter("mem"), 5), 1024), 1);
            alloc = new byte[mem * 1024 * 1024];
            // database check
            DbAdapter.getAdapter().execQuery("select 1", null);
        } catch (OutOfMemoryError oome) {
            Logger.error(oome, "Encountered OutOfMemory during memory check");
            throw oome;
        } finally {
            Runtime rt= Runtime.getRuntime();
            Logger.debug(String.format("HealthCheck(Used/Max/Allocated): %s/%s/%s",
                    FileUtil.getSizeAsString(rt.totalMemory() - rt.freeMemory()),
                    FileUtil.getSizeAsString(rt.maxMemory()), alloc == null ? "null" : FileUtil.getSizeAsString(alloc.length)));
        }
    }
}
