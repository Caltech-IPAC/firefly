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

/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
*
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
