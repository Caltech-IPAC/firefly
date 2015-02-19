/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.SrvParam;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: FFToolsStandaloneService.java,v 1.5 2012/12/19 22:36:08 roby Exp $
 */
public class FFToolsStandaloneService extends BaseHttpServlet {

    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    public static final boolean DEBUG= true;
    private final static String SPACE4=  "    ";
    private final static String SPACE6=  "      ";
    private final static String SPACE8 = "        ";
    private final static String SPACE10= "          ";

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        SrvParam sp= new SrvParam(req.getParameterMap());
        String cmd = sp.getRequired(ServerParams.COMMAND);
        ServletOutputStream out = res.getOutputStream();

        if (cmd.equals(ServerParams.PLOT_EXTERNAL)) {
            String reqStr = sp.getRequired(ServerParams.REQUEST);
            try {
                reqStr= URLDecoder.decode(reqStr, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // do nothing, just leave reqStr
            }
            WebPlotRequest wpr= WebPlotRequest.parse(reqStr);
            if (wpr!=null) {
                res.setContentType("text/html");
                writeViewerFile(req, out);
                writeViewerScript(out,wpr);
                writeDocEnd(out);
            }
        }
        out.close();
    }


    public static void writeViewerFile(HttpServletRequest req, ServletOutputStream out) throws IOException {
        String rp= req.getSession().getServletContext().getRealPath("/standalone_root.html");
        File f= new File(rp);
        FileUtil.writeFileToStream(f,out);
    }

    public static void writeViewerScript(ServletOutputStream out,
                                         WebPlotRequest wpr) throws IOException{


        writeScriptStart(out);

        out.println(SPACE8 +"var ex= firefly.getExpandViewer();");
        out.println(SPACE8 +"ex.setWindowClose(true);");
        out.println(SPACE8 +"ex.setFullControl(true);");
        out.println(SPACE8 +"ex.plot({");
        int i= 0;
        for(Param p : wpr.getParams()) {
            out.print(SPACE10 +"\"" + p.getName()+"\" : \"" + p.getValue() +"\"" );
            i++;
            if (i<wpr.getParams().size()) {
                 out.println(",");
            }
        }
        out.println(SPACE4 +"});");

        writeScriptEnd(out);
    }

    public static void writeScriptStart(ServletOutputStream out) throws IOException {
        out.println("<script type=\"text/javascript\">");
        out.println("    {");
        out.println("      onFireflyLoaded= function() {");

    }

    public static void writeScriptEnd(ServletOutputStream out) throws IOException {
        out.println("    }");
        out.println("  }");
        out.println("</script>");
    }
    public static void writeDocEnd(ServletOutputStream out) throws IOException {
        FileUtil.writeStringToStream("\n</body>\n</html>",out);
    }
}

