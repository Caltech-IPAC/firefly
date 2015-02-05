/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/20/14
 * Time: 9:23 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import net.zschech.gwt.comet.server.CometServlet;
import net.zschech.gwt.comet.server.CometServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author Trey Roby
 */
public class EventSenderServlet extends CometServlet {

    @Override
    protected void doComet(CometServletResponse cometResponse) throws ServletException, IOException {
        String winId= cometResponse.getRequest().getParameter("winId");
        String sID= ServerContext.getRequestOwner().getUserKey();
        EventMatchCriteria criteria= EventMatchCriteria.makeSessionCriteria(sID,winId);
        ServerEventManager.addEventQueueForClient(cometResponse, sID, criteria);
        Logger.briefInfo("doComet, request owner: " + ServerContext.getRequestOwner().getUserKey()+ ", winId= "+winId);
    }


    @Override
    public void cometTerminated(CometServletResponse cometResponse, boolean serverInitiated) {
        String winId= cometResponse.getRequest().getParameter("winId");
        Logger.briefInfo("cometTerminated, request owner: "+ServerContext.getRequestOwner().getUserKey()+ ", winId= "+winId);
    }
}

