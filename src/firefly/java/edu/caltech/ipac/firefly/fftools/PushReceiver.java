/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 4:24 PM
 */


import edu.caltech.ipac.firefly.core.ClientEventQueue;
import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.Ext;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ui.DS9RegionLoadDialog;
import edu.caltech.ipac.util.StringUtils;

import java.util.Arrays;
import java.util.logging.Level;

/**
 * @author Trey Roby
 */
public class PushReceiver implements WebEventListener {
    public enum ExtType { AREA_SELECT, LINE_SELECT, POINT, NONE }
    public static final String TABLE_SEARCH_PROC_ID = "IpacTableFromSource";
    public final ExternalPlotController plotController;

    private static final String IMAGE_CMD_PLOT_ID= "ImagePushPlotID";
    private static int idCnt= 0;

    public PushReceiver(ExternalPlotController plotController) {
        this.plotController= plotController;
        WebEventManager.getAppEvManager().addListener(this);
    }

    public void eventNotify(WebEvent ev) {
        Name name = ev.getName();
        String data = String.valueOf(ev.getData());

        GwtUtil.getClientLogger().log(Level.INFO, "name//data: " + name.getName() + " // " + data);
        if (name.equals(Name.PUSH_WEB_PLOT_REQUEST)) {
            WebPlotRequest wpr= WebPlotRequest.parse(data);
            String id;
            if (wpr.getPlotId()!=null) {
                id= wpr.getPlotId();
            } else {
                id=IMAGE_CMD_PLOT_ID + idCnt;
                idCnt++;
            }
            wpr.setPlotId(id);
            prepareRequest(wpr);
        } else if (name.equals(Name.PUSH_REGION_FILE)) {
            DS9RegionLoadDialog.loadRegFile(data,null);
        } else if (name == Name.PUSH_FITS_COMMAND_EXT) {
            Ext.Extension ext= parsePlotCmdExtension(data);
            Ext.ExtensionInterface exI= Ext.makeExtensionInterface();
            exI.fireExtAdd(ext);
        } else if (name.equals(Name.PUSH_TABLE_FILE)) {
            loadTable(data);
        } else if (name.equals(Name.PUSH_FITS_COMMAND_EXT)) {

        }

        // TODO: LLY- remove later.. just test code.
        else {
            if (name.equals(Name.WINDOW_RESIZE)) {
                if (!ev.getSource().equals(ClientEventQueue.class)) {
                    ServerEvent sevt = new ServerEvent(Name.WINDOW_RESIZE,
                            ServerEvent.Scope.CHANNEL, ServerEvent.DataType.STRING, data);
                    ClientEventQueue.sendEvent(sevt);
                }
            }
            // not an event this receiver cares for...
        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private static Ext.Extension parsePlotCmdExtension(String in) {
        ServerRequest req= ServerRequest.parse(in, new ServerRequest());

        return Ext.makeExtension(req.getRequestId(),
                req.getParam(ServerParams.PLOT_ID),
                StringUtils.getEnum(req.getParam(ServerParams.EXT_TYPE), ExtType.NONE).toString(),
                req.getParam(ServerParams.IMAGE),
                req.getParam(ServerParams.TITLE),
                req.getParam(ServerParams.TOOL_TIP));
    }


    private void prepareRequest(ServerRequest req) { deferredPlot(req); }

    private void deferredPlot(ServerRequest req) {
        WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);

        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) && req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }

        plotController.update(wpReq);
    }

    protected void loadTable(final String fileName) {

        final TableServerRequest req = new TableServerRequest(TABLE_SEARCH_PROC_ID);
        req.setStartIndex(0);
        req.setPageSize(100);
        req.setParam("source", fileName);
        String title= findTitle(req);
        SearchAdmin.getInstance().submitSearch(req, title);
    }

    private static String findTitle(TableServerRequest req) {
        String title= "Loaded Table";
        if (req.containsParam(ServerParams.TITLE)) {
            title= req.getParam(ServerParams.TITLE);
        }
        else if (req.containsParam(ServerParams.SOURCE)) { // find another way to make a title
            req.setParam(ServerParams.SOURCE, FFToolEnv.modifyURLToFull(req.getParam(ServerParams.SOURCE)));
            String url = req.getParam(ServerParams.SOURCE);
            int idx = url.lastIndexOf('/');
            if (idx<0) idx = url.lastIndexOf('\\');
            if (idx > 1) {
                title = url.substring(idx+1);
            } else {
                title = url;
            }
        }
        return title;

    }

    public interface ExternalPlotController {
        void update(WebPlotRequest wpr);
    }
}
