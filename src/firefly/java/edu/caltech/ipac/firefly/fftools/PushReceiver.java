/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 4:24 PM
 */


import edu.caltech.ipac.firefly.core.SearchAdmin;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.Ext;
import edu.caltech.ipac.firefly.visualize.RegionLoader;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class PushReceiver implements WebEventListener {
    public enum ExtType { AREA_SELECT, LINE_SELECT, POINT, NONE }
    public static final String TABLE_SEARCH_PROC_ID = "IpacTableFromSource";
    private static final String IMAGE_CMD_PLOT_ID= "ImagePushPlotID";
    private static int idCnt= 0;

    public final ExternalPlotController plotController;


    public PushReceiver(ExternalPlotController plotController) {
        this.plotController= plotController;
        WebEventManager.getAppEvManager().addListener(this);
    }

    public void eventNotify(WebEvent ev) {
        Name name = ev.getName();
        String data = String.valueOf(ev.getData());

        if (name.equals(Name.PUSH_WEB_PLOT_REQUEST)) {
            prepareRequest(data);
        } else if (name.equals(Name.PUSH_REGION_DATA)) {
            loadRegionData(data);
        } else if (name.equals(Name.REMOVE_REGION_DATA)) {
            removeRegionData(data);
        } else if (name.equals(Name.PUSH_REGION_FILE)) {
            loadRegionFile(data);
        } else if (name.equals(Name.PUSH_REMOVE_REGION_FILE)) {
            removeRegionFile(data);
        } else if (name.equals(Name.PUSH_FITS_COMMAND_EXT)) {
            addPlotCmdExtension(data);
        } else if (name.equals(Name.PUSH_TABLE_FILE)) {
            loadTable(data);
        } else if (name.equals(Name.PUSH_XYPLOT_FILE)) {
            loadXYPlot(data);
        }


//        // TODO: LLY- remove later.. just test code.
//        else {
//            if (name.equals(Name.WINDOW_RESIZE)) {
//                if (!ev.getSource().equals(ClientEventQueue.class)) {
//                    ServerEvent sevt = new ServerEvent(Name.WINDOW_RESIZE,
//                            ServerEvent.Scope.CHANNEL, ServerEvent.DataType.STRING, data);
//                    ClientEventQueue.sendEvent(sevt);
//                }
//            }
//            // not an event this receiver cares for...
//        }
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private static void addPlotCmdExtension(String in) {
        ServerRequest req= ServerRequest.parse(in, new ServerRequest());
        Ext.ExtensionInterface exI= Ext.makeExtensionInterface();

        Ext.Extension ext= Ext.makeExtension(
                req.getRequestId(),
                req.getParam(ServerParams.PLOT_ID),
                StringUtils.getEnum(req.getParam(ServerParams.EXT_TYPE), ExtType.NONE).toString(),
                req.getParam(ServerParams.IMAGE),
                req.getParam(ServerParams.TITLE),
                req.getParam(ServerParams.TOOL_TIP));
        exI.fireExtAdd(ext);
    }


    private void prepareRequest(String data) {
        WebPlotRequest wpr= WebPlotRequest.parse(data);
        String id;
        if (wpr.getPlotId()!=null) {
            id= wpr.getPlotId();
        } else {
            id=IMAGE_CMD_PLOT_ID + idCnt;
            idCnt++;
        }
        wpr.setPlotId(id);
        deferredPlot(wpr);
    }

    private void deferredPlot(ServerRequest req) {
        WebPlotRequest wpReq= WebPlotRequest.makeRequest(req);

        if (req.containsParam(CommonParams.RESOLVE_PROCESSOR) && req.containsParam(CommonParams.CACHE_KEY)) {
            wpReq.setParam(TableServerRequest.ID_KEY, "MultiMissionFileRetrieve");
            wpReq.setRequestType(RequestType.PROCESSOR);
        }

        plotController.update(wpReq);
    }

    private void loadTable(final String fileName) {

        final TableServerRequest req = new TableServerRequest(TABLE_SEARCH_PROC_ID);
        req.setStartIndex(0);
        req.setPageSize(100);
        req.setParam("source", fileName);
        String title= findTitle(req);
        SearchAdmin.getInstance().submitSearch(req, title);
    }

    private void loadXYPlot(final String data) {

        ServerRequest sreq = ServerRequest.parse(data, new ServerRequest());
        final Map<String,String> params = new HashMap<String,String>();
        for (Param p : sreq.getParams()) {
            params.put (p.getName(), p.getValue());
        }
        plotController.addXYPlot(params);
    }


    private void loadRegionFile(final String in) {
        ServerRequest req= ServerRequest.parse(in, new ServerRequest());
        String id= req.getRequestId();
        String regFile= req.getParam(ServerParams.FILE);
//        String title= req.getParam(ServerParams.TITLE);
        RegionLoader.loadRegFile(regFile, id, null);
    }

    private void removeRegionFile(final String id) {
        RegionLoader.removeRegion(id);
    }


    private void loadRegionData(final String in) {
        ServerRequest req= ServerRequest.parse(in, new ServerRequest());
        String id= req.getRequestId();
        String regData= req.getParam(ServerParams.DS9_REGION_DATA);
        String title= req.getParam(ServerParams.TITLE);
        RegionLoader.loadRegion(title,regData,null,id);
    }
    private void removeRegionData(final String in) {
        ServerRequest req= ServerRequest.parse(in, new ServerRequest());
        String id= req.getRequestId();
        String regData= req.getParam(ServerParams.DS9_REGION_DATA);
        RegionLoader.removeFromRegion(regData,id);
    }




    private static String findTitle(TableServerRequest req) {
        String title= "Loaded Table";
        if (req.containsParam(ServerParams.TITLE)) {
            title= req.getParam(ServerParams.TITLE);
        }
        else if (req.containsParam(ServerParams.SOURCE)) { // find another way to make a title
            String val = req.getParam(ServerParams.SOURCE);
            if ( !(val == null || val.startsWith("$") || val.startsWith("/")) ) {
                req.setParam(ServerParams.SOURCE, FFToolEnv.modifyURLToFull(val));
            }
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
        void addXYPlot(Map<String,String> params);
    }
}
