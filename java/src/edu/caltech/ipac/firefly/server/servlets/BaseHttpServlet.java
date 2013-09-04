package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.util.ComparisonUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Date: Oct 27, 2008
 *
 * @author loi
 * @version $Id: BaseHttpServlet.java,v 1.14 2011/05/02 19:25:24 roby Exp $
 */
public abstract class BaseHttpServlet extends HttpServlet {

    private static final String FAILURE_MSG_TMPL = "\nThe call failed on the server:\n%s" + "\n\nService: %s\n";

    private boolean allowAccess = false;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        VersionUtil.initVersion(config.getServletContext());  // can be called multiple times, only inits on the first call
        String allowFromValue = config.getInitParameter("AllowFrom");
        String serveltContextName = config.getServletContext().getServletContextName();
        if (allowFromValue!=null) {
            if (allowFromValue.contains(",")) {
                for (String value: allowFromValue.split(",")) {
                    if (ComparisonUtil.equals(value, serveltContextName)) {
                        allowAccess = true;
                        break;
                    }
                }
            } else {
                allowAccess = ComparisonUtil.equals(allowFromValue, serveltContextName);
            }
        } else {
            allowAccess = true; // if AllowFrom is not defined in web.xml <servlet>'s <init-param> tag, set it to true.
        }

    }

    /**
     * Sends the message in a custom format.
     * status_code::msg::value
     *
     * @param res
     * @param status
     * @param msg
     * @param value
     */
    protected void sendReturnMsg(HttpServletResponse res, int status, String msg, String value) throws IOException {

        String retstr = status + "::" + (msg == null ? "": msg.replace("\n", "<br>")) + "::" + (value == null ? "" : value);
        res.setStatus(status);
        res.setContentLength(retstr.length());
        res.setContentType("text/html");
        res.setCharacterEncoding("utf-8");
        res.getOutputStream().print(retstr);

    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        enableCors(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doService(httpServletRequest, httpServletResponse);
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        doService(httpServletRequest, httpServletResponse);
    }

    protected void doService(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
            StopWatch.getInstance().start(getClass().getSimpleName());
            enableCors(req, res);
            if (allowAccess)
                processRequest(req, res);
            else
                sendReturnMsg(res, 404, "File Not Found", "The requested file was not found on the IRSA website.");
        } catch (Throwable e) {
            handleException(req, res, e);
        } finally {
            StopWatch.getInstance().printLog(getClass().getSimpleName());
        }
    }

    public static void enableCors(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
        resp.setHeader("Access-Control-Max-Age", "86400");      // cache for 1 day
    }

    abstract protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception;

//====================================================================
//
//====================================================================

    private void handleException(HttpServletRequest req, HttpServletResponse response, Throwable e) throws ServletException {

        e.printStackTrace();
        String msg = String.format(FAILURE_MSG_TMPL, e.getMessage(), getClass().getName());
        throw new ServletException(msg, e);
        
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
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/
