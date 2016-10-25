/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.SrvParam;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Feb 11, 2007
 *
 * @author Trey Roby
 * @version $Id: CommandService.java,v 1.8 2012/08/22 20:31:43 roby Exp $
 */
public class CommandService extends BaseHttpServlet {
    public static final boolean DEBUG= true;
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();


    private static final Logger.LoggerImpl _statsLog= Logger.getLogger(Logger.VIS_LOGGER);

    public static void sendError(HttpServletRequest req, HttpServletResponse res, Map<String, String> errors) {
        sendError(req, res, errors, 500);
    }

    public static void sendError(HttpServletRequest req, HttpServletResponse res, Map<String, String> errors, int statusCode) {
        JSONObject rval = new JSONObject();
        JSONObject error = new JSONObject(errors);
        rval.put("success", false);
        rval.put("error", error);
        String msg = rval.toJSONString();
        res.setStatus(statusCode);
        res.setContentLength(msg.length());
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        try {
            res.getOutputStream().print(msg);
        } catch (IOException e) {
            LOGGER.error("Unable to send error response to client.", e.getMessage());
        }

    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        try {
            long start = System.currentTimeMillis();
            SrvParam sp = new SrvParam(req.getParameterMap());

            if (DEBUG) {
                List<String> sList= new ArrayList<>();
                for(Map.Entry<String,String[]> entry : sp.getParamMap().entrySet()) {
                    sList.add(""+ entry.getKey()+ " : "+ entry.getValue()[0]);
                }
                Logger.debug(sList.toArray(new String[sList.size()]));
            }

            String cmd = sp.getCommandKey();
            ServerCommandAccess.HttpCommand command = ServerCommandAccess.getCommand(cmd);
            command.processRequest(req, res, sp);
            Logger.briefDebug(String.format("CommandService: %s successfully completed in: %dms", cmd, System.currentTimeMillis() - start));
        } catch (Exception ex) {
            LOGGER.error(ex);
            HashMap<String, String> errors = new HashMap<>();
            int cnt = 1;
            for (Throwable t = ex.getCause(); (t != null); t = t.getCause()) {
                errors.put("cause" + cnt, t.toString());
                cnt++;
            }
            sendError(req, res, errors);
        }

    }

}

