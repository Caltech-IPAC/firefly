/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Date: Sep 15, 2008
 *
 * @author loi
 * @version $Id: SaveAsIpacTable.java,v 1.1 2010/11/03 20:49:36 loi Exp $
 * @deprecated incorporated into SearchServices.  see edu.caltech.ipac.firefly.server.servlets.SearchServices.doTableSave()
 */
public class SaveAsIpacTable  extends BaseHttpServlet {
    public static final Logger.LoggerImpl DL_LOGGER = Logger.getLogger(Logger.DOWNLOAD_LOGGER);

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String reqStr = req.getParameter(Request.class.getName());
        reqStr = reqStr == null ? req.getParameter("Request") : reqStr;
        TableServerRequest request = QueryUtil.convertToServerRequest(reqStr);

        if (request == null) throw new IllegalArgumentException("Invalid request");

        String fileName = req.getParameter("file_name");

        fileName = StringUtils.isEmpty(fileName) ? request.getRequestId() : fileName;
        res.setHeader("Content-Disposition", "attachment; filename=" + fileName + (fileName.endsWith(".tbl")?"":".tbl"));
        SearchManager am = new SearchManager();
        try {
            am.save(res.getOutputStream(), request);
        } finally {
            FileInfo fi = am.getFileInfo(request);
            long length = 0;
            if (fi != null) {
                length = fi.getSizeInBytes();
            }
            logStats(length, "fileName", fi.getExternalName());
            // maintain counters for applicaiton monitoring
            Counters.getInstance().increment(Counters.Category.Download, "SaveAsIpacTable");
            Counters.getInstance().incrementKB(Counters.Category.Download, "SaveAsIpacTable (KB)", length/1024);
        }
    }

    protected void logStats(long fileSize, Object... params) {
        DL_LOGGER.stats("SaveAsIpacTable", "fsize(MB)", (double) fileSize / StringUtils.MEG,
                "params", CollectionUtil.toString(params, ","));
    }


}
