/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.VersionUtil;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.util.ComparisonUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
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
    protected static void sendReturnMsg(HttpServletResponse res, int status, String msg, String value) throws IOException {

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
            if (allowAccess) {
                String codId = req.getParameter(GwtUtil.COD_ID);
                if (codId != null) {
                    Cookie c = new Cookie(GwtUtil.COD_ID, codId);
                    c.setPath("/");
                    res.addCookie(c);
                }
                processRequest(req, res);

            } else {
                sendReturnMsg(res, 404, "File Not Found", "The requested file was not found on the IRSA website.");
            }
        } catch (Throwable e) {
            handleException(req, res, e);
        } finally {
            StopWatch.getInstance().printLog(getClass().getSimpleName());
        }
    }

    public static void enableCors(HttpServletRequest req, HttpServletResponse resp) {
        if (req.getHeader("Origin") != null) {
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
            resp.setHeader("Access-Control-Allow-Headers", req.getHeader("Access-Control-Request-Headers"));
            resp.setHeader("Access-Control-Max-Age", "86400");      // cache for 1 day
        }
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

