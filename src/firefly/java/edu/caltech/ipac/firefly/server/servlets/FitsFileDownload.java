/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.SrvParam;
import edu.caltech.ipac.firefly.server.visualize.VisContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: FitsFileDownload.java,v 1.9 2012/12/19 22:36:08 roby Exp $
 */
@Deprecated
public class FitsFileDownload extends BaseHttpServlet {


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        SrvParam sp= new SrvParam(req.getParameterMap());
        String fname= sp.getRequired("file");
        File downloadFile= VisContext.convertToFile(fname);
        if (downloadFile==null) {
            throw new ServletException("File does not exist");
        }
        else {
            long fileLength= downloadFile.length();
            if(fileLength <= Integer.MAX_VALUE) res.setContentLength((int)fileLength);
            else                                res.addHeader("Content-Length", fileLength+"");
            res.addHeader("Content-Type", "image/x-fits");
            res.addHeader("Content-Disposition",
                          "attachment; filename="+downloadFile.getName());
            PlotServUtils.writeFileToStream(downloadFile,res.getOutputStream());
        }
    }
}
