/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.visualize.VisServerOps;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotResultParser;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: PlotFileService.java,v 1.3 2012/03/12 18:04:41 roby Exp $
 */
public class PlotFileService extends BaseHttpServlet {


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        String red= req.getParameter("red");
        String green= req.getParameter("green");
        String blue= req.getParameter("blue");
        String noband= req.getParameter("noband");

        WebPlotRequest rReq= null;
        WebPlotRequest bReq= null;
        WebPlotRequest gReq= null;
        WebPlotRequest nobandReq= null;
        boolean threeColor;

        if (noband!=null) {
            nobandReq= WebPlotRequest.parse(noband);
            threeColor= false;
        }
        else {
            rReq= WebPlotRequest.parse(red);
            gReq= WebPlotRequest.parse(green);
            bReq= WebPlotRequest.parse(blue);
            threeColor= true;
        }


        try {
            WebPlotResult result;
            if (threeColor) {
                 result= VisServerOps.create3ColorPlot(rReq,gReq,bReq);
            }
            else {
                 result= VisServerOps.createPlot(nobandReq);
            }
            String resultStr= WebPlotResultParser.createJS(result);

            res.setContentType("text/plain");
            res.setContentLength(resultStr.length());
            ServletOutputStream out= res.getOutputStream();
            out.write(resultStr.getBytes());
            out.close();
        } catch (IOException e) {
            throw new ServletException(e.toString(),e);
        }
    }

}

