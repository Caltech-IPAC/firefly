package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.BrowserInfo;
import edu.caltech.ipac.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: CommandService.java,v 1.8 2012/08/22 20:31:43 roby Exp $
 */
public class CommandService extends BaseHttpServlet {

    private ServerCommandAccess commandAccess = new ServerCommandAccess();
    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);
    public static final boolean DEBUG= true;

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        long start = System.currentTimeMillis();
        String callback = req.getParameter("callback");
        String doJsonPStr = req.getParameter(ServerParams.DO_JSONP);


        boolean doJsonp = Boolean.parseBoolean(doJsonPStr);

        Map<String, String[]> map = req.getParameterMap();

        String jsonData;
        try {
            if (DEBUG) {
                List<String> sList= new ArrayList<String>();
                for(Map.Entry<String,String[]> entry : map.entrySet()) {
                    sList.add(""+ entry.getKey()+ " : "+ entry.getValue()[0]);
                }
                Logger.debug(sList.toArray(new String[sList.size()]));
            }
            String result = commandAccess.doCommand(map);

            if (commandAccess.getCanCreateJson(map)) {
                jsonData = result;
            } else {
                jsonData = "[{" +
                        "\"success\" :  \"" + true + "\"," +
                        "\"data\" :  \"" + StringUtils.escapeQuotes(result) + "\"" +
                        "}]";
            }

            StringBuilder sb= new StringBuilder(jsonData.length()+10);
            for(char c : jsonData.toCharArray()) {
                if (!Character.isISOControl(c) && c>31 && c<127 ) {
                    sb.append(c);
                }
            }

            jsonData= sb.toString();

//            res.setContentType("text/plain");
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder(500);
            sb.append("[{");
            sb.append("\"success\" :  \"").append(false).append("\",");
            int cnt = 0;
            for (Throwable t = e; (t != null); t = t.getCause()) {
                sb.append("\"e").append(cnt).append("\" :  \"").append(t.toString()).append("\"");
                if (e.getCause() != null) sb.append(", ");
                cnt++;
            }
            sb.append("}]");
            jsonData = sb.toString();
        }

        String retval;
        if (doJsonp && callback != null) {
            retval = callback + "(" + jsonData + ");";
        } else {
            retval = jsonData;
        }

        BrowserInfo b= new BrowserInfo(req.getHeader("user-agent"));
        if ((b.isIE() && b.getMajorVersion()<=9) || !doJsonp) {
            res.setContentType("text/html");
        }
        else {
            res.setContentType("application/javascript");
        }
        res.setContentLength(retval.length());
        ServletOutputStream out = res.getOutputStream();
        out.write(retval.getBytes());
        out.close();
        Logger.briefDebug("CommandService time took: " + (System.currentTimeMillis() - start));
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
