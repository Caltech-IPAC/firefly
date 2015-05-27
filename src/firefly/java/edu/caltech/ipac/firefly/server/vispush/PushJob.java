/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.vispush;
/**
 * User: roby
 * Date: 1/27/15
 * Time: 1:04 PM
 */


import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.events.ServerEventManager;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

/**
 * @author Trey Roby
 */
public class PushJob {


    private static void fireEvent(String data, Name evName) {
        ServerEvent sevt = new ServerEvent(evName,
                new ServerEvent.EventTarget(ServerEvent.Scope.CHANNEL),
                ServerEvent.DataType.STRING, data,
                ServerContext.getRequestOwner().getEventConnID());
        ServerEventManager.fireEvent(sevt);

    }

    public static boolean pushFits(WebPlotRequest wpr) {
        fireEvent(wpr.toString(),Name.PUSH_WEB_PLOT_REQUEST);
        return true;
    }

    public static boolean pushExtension(String sreqId,
                                        String plotId,
                                        String extType,
                                        String title,
                                        String image,
                                        String toolTip) {
        ServerRequest r= new ServerRequest(sreqId);
        r.setParam(ServerParams.EXT_TYPE, extType);
        r.setParam(ServerParams.TITLE, title);
        r.setParam(ServerParams.PLOT_ID, plotId);
        if (image!= null) r.setParam(ServerParams.IMAGE, image);
        if (toolTip!= null) r.setParam(ServerParams.IMAGE, toolTip);
        fireEvent(r.toString(),Name.PUSH_FITS_COMMAND_EXT);
        return true;
    }

    public static boolean pushTable(String fileName) {
        fireEvent(fileName,Name.PUSH_TABLE_FILE);
        return true;
    }

    //================================
    //========== Region Stuff
    //================================

    public static boolean pushRegionFile(String fileName, String id) {
        ServerRequest r= new ServerRequest(id);
        r.setParam(ServerParams.FILE,fileName);
        fireEvent(r.toString(),Name.PUSH_REGION_FILE);
        return true;
    }

    public static boolean pushRemoveRegionFile(String id) {
        fireEvent(id,Name.PUSH_REMOVE_REGION_FILE);
        return true;
    }


    public static boolean pushRegionData(String title, String id, String data) {
        ServerRequest r= new ServerRequest(id);
        r.setParam(ServerParams.TITLE, title);
        r.setParam(ServerParams.DS9_REGION_DATA, data);
        fireEvent(r.toString(),Name.PUSH_REGION_DATA);
        return true;
    }

    public static boolean pushRemoveRegionData(String id, String data) {
        ServerRequest r= new ServerRequest(id);
        r.setParam(ServerParams.DS9_REGION_DATA, data);
        fireEvent(r.toString(),Name.REMOVE_REGION_DATA);
        return true;
    }

    //================================
    //========== End Region Stuff
    //================================

}
