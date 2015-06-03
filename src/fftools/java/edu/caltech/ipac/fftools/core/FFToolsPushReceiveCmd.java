/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.fftools.core;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.data.fuse.PlotData;
import edu.caltech.ipac.firefly.fftools.PushReceiver;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.Ext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.util.HashMap;
import java.util.Map;

public class FFToolsPushReceiveCmd extends RequestCmd {

    public static final String COMMAND = "Loader";
    private final StandaloneUI aloneUI;
    private Map<String,PushReceiver> receivers= new HashMap<String, PushReceiver>(5);

    public FFToolsPushReceiveCmd(StandaloneUI aloneUI) {
        super(COMMAND, "Fits Viewer", "Fits Viewer", true);
        this.aloneUI = aloneUI;
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {
        String bid = req.getParam("channelID");
            Ext.ExtensionInterface exI= Ext.makeExtensionInterfaceWithListener(this, getStoreCBForJs());
            exI.fireChannelActivate(bid);
        if (!aloneUI.hasResults()) {
            HTML message= new HTML("Waiting for data");
            GwtUtil.setStyles(message,  "width","100%",
                                        "fontSize", "16pt",
                                        "textAlign", "center");
            Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).setDisplay(message);
        }
    }

    public static void storeCB(Object o) {
        ((FFToolsPushReceiveCmd)o).setupBackgroundChannel();
    }

    public static native JavaScriptObject getStoreCBForJs() /*-{
        return $entry(@edu.caltech.ipac.fftools.core.FFToolsPushReceiveCmd::storeCB(*));
    }-*/;


    private void setupBackgroundChannel() {
        Ext.ExtensionInterface exI= Ext.makeExtensionInterface();
        String channel= exI.getRemoteChannel();
//        if (channel!=null && !receivers.containsKey(channel)) {  //TODO: LLY - need to figure this out.
        if (!receivers.containsKey(channel)) {

            PushReceiver.ExternalPlotController plotController= new AppPlotController(aloneUI);
            PushReceiver dpR= new PushReceiver(plotController);
            receivers.put(channel,dpR);
        }
    }

    public static class AppPlotController implements PushReceiver.ExternalPlotController {

        private StandaloneUI aloneUI;
        public AppPlotController(StandaloneUI aloneUI) {
            this.aloneUI= aloneUI;
        }

        public void update(WebPlotRequest wpr) {
            aloneUI.getMultiViewer().forceExpand();
            PlotData dynData= ConverterStore.get(ConverterStore.DYNAMIC).getPlotData();
            dynData.setID(wpr.getPlotId(),wpr);
        }

        public void addXYPlot(Map<String, String> params) {
            aloneUI.newXYPlot(params);
        }
    }
}
