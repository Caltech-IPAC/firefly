package edu.caltech.ipac.firefly.server.rpc;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import edu.caltech.ipac.firefly.core.RPCException;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.server.util.VersionUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author tatianag
 *         Date: May 19, 2008
 *         Time: 2:39:00 PM
 */
public class BaseRemoteService extends RemoteServiceServlet {
    private static final Logger.LoggerImpl logger = Logger.getLogger();

    private static final String FAILURE_MSG_TMPL = "<p align='center'><div><b>The call failed on the server:</b><br>%s" +
                                                    "<br><br><i>Class: %s <br>Method: %s <br>Line: %d </i></div></p>";


    static boolean isInit = false;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        if (!isInit) {
            new ServerContext(); // just a way to initializes ServerContext
            log("rpc initialized.");
            isInit = true;
            VersionUtil.initVersion(config.getServletContext());
        }
    }


    @Override
    public String processCall(String payload) throws SerializationException {
        this.getThreadLocalResponse().addHeader("Access-Control-Allow-Origin", "*");
        String s = super.processCall(payload);
        return s;
    }

    @Override
    protected void doUnexpectedFailure(Throwable e) {
        RPCRequest req = (RPCRequest) ServerContext.getRequestOwner().getAttribute("rpcRequest");
        if (req == null) {
            super.doUnexpectedFailure(e);
            return;
        }
        try {
            RPC.encodeResponseForFailure(req.getMethod(), e, req.getSerializationPolicy());
        } catch (Exception ex) {
            ex.printStackTrace();
            super.doUnexpectedFailure(ex);
        }
    }

    @Override
    protected void onBeforeRequestDeserialized(String serializedRequest) {
        RPCRequest req = RPC.decodeRequest(serializedRequest, this.getClass(), this);
        ServerContext.getRequestOwner().setAttribute("rpcRequest", req);
        StopWatch.getInstance().start(makeReqKey(req));
    }

    @Override
    protected void onAfterResponseSerialized(String serializedResponse) {
        RPCRequest req = (RPCRequest) ServerContext.getRequestOwner().getAttribute("rpcRequest");
        String id = makeReqKey(req);
        StopWatch.getInstance().printLog(id);
        logger.briefDebug("processing " + id + "; " + serializedResponse.length() + " characters sent.");
    }


    protected RPCException createRPCException(Throwable e) {
        e.printStackTrace();
        RPCRequest req = (RPCRequest) ServerContext.getRequestOwner().getAttribute("rpcRequest");
        return new RPCException (e, getClass().getSimpleName(), req.getMethod().getName(), "The call failed on the server", e.getMessage());
    }

//====================================================================
//
//====================================================================
    private String makeReqKey(RPCRequest req) {
        return this.getClass().getSimpleName() +"." +
                        (req == null ? "unknown" : req.getMethod().getName());

    }

}