package edu.caltech.ipac.firefly.server.servlets;

import com.google.gwt.logging.server.RemoteLoggingServiceImpl;
import com.google.gwt.user.client.rpc.SerializationException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Date: Feb 16, 2011
 *
 * @author loi
 * @version $Id: AnyFileUpload.java,v 1.3 2011/10/11 21:44:39 roby Exp $
 */
public class FireflyRemoteLogging extends RemoteLoggingServiceImpl {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String fName= config.getServletContext().getRealPath(getInitParameter("gwtSymbolMapDir"));
        setSymbolMapsDirectory(fName);
    }

    @Override
    public String processCall(String payload) throws SerializationException {
        BaseHttpServlet.enableCors(this.getThreadLocalRequest(), this.getThreadLocalResponse());
        return super.processCall(payload);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BaseHttpServlet.enableCors(req, resp);
        super.doOptions(req, resp);
    }
}

