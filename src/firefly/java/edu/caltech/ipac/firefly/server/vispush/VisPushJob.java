/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.vispush;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 1:04 PM
 */


import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.BackgroundInfoCacher;
import edu.caltech.ipac.firefly.server.query.BackgroundEnv;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.server.events.ServerEventQueue;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.concurrent.TimeUnit;

/**
 * @author Trey Roby
 */
public class VisPushJob {

    public static boolean pushFits(WebPlotRequest wpr, ServerEvent.EventTarget evtTarget) {
        ServerEvent sevt = new ServerEvent(Name.PUSH_WEB_PLOT_REQUEST, evtTarget,
                    ServerEvent.DataType.STRING, wpr.toString(), ServerContext.getRequestOwner().getEventConnID());
        ServerEventManager.fireEvent(sevt);
        return true;
    }

    public static boolean pushRegion(String fileName, ServerEvent.EventTarget evtTarget) {
        ServerEvent sevt = new ServerEvent(Name.PUSH_REGION_FILE, evtTarget,
                ServerEvent.DataType.STRING, fileName, ServerContext.getRequestOwner().getEventConnID());
        ServerEventManager.fireEvent(sevt);
        return true;
    }

    public static boolean pushExtension(String sreqId,
                                        String plotId,
                                        String extType,
                                        String title,
                                        String image,
                                        String toolTip,
                                        ServerEvent.EventTarget evtTarget) {
        ServerRequest r= new ServerRequest(sreqId);
        r.setParam(ServerParams.EXT_TYPE, extType);
        r.setParam(ServerParams.TITLE, title);
        r.setParam(ServerParams.PLOT_ID, plotId);
        if (image!= null) r.setParam(ServerParams.IMAGE, image);
        if (toolTip!= null) r.setParam(ServerParams.IMAGE, toolTip);

        ServerEvent sevt = new ServerEvent(Name.PUSH_FITS_COMMAND_EXT, evtTarget,
                ServerEvent.DataType.STRING, r.toString(), ServerContext.getRequestOwner().getEventConnID());
        ServerEventManager.fireEvent(sevt);
        return true;
    }

    public static boolean pushTable(String fileName, ServerEvent.EventTarget evtTarget) {
        ServerEvent sevt = new ServerEvent(Name.PUSH_TABLE_FILE, evtTarget,
                ServerEvent.DataType.STRING, fileName, ServerContext.getRequestOwner().getEventConnID());
        ServerEventManager.fireEvent(sevt);
        return true;
    }

}
