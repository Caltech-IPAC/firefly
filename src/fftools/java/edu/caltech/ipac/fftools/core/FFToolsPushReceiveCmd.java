/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.fftools.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestCmd;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.HashMap;
import java.util.Map;

public class FFToolsPushReceiveCmd extends RequestCmd {

    public static final String COMMAND = "Loader";
    private final StandaloneUI aloneUI;
    private static final String IMAGE_CMD_PLOT_ID = "ImagePushPlotID";
    private static int idCnt = 0;
    private Map<String,PushReceiver> receivers= new HashMap<String, PushReceiver>(5);

    public FFToolsPushReceiveCmd(StandaloneUI aloneUI) {
        super(COMMAND, "Fits Viewer", "Fits Viewer", true);
        this.aloneUI = aloneUI;
    }

    protected void doExecute(final Request req, AsyncCallback<String> callback) {
        String bid = req.getParam("BID");
        if (!receivers.containsKey(bid)) {
            MonitorItem monItem = new MonitorItem(null, "i don't know", BackgroundUIHint.NONE);
            monItem.setWatchable(false);
            monItem.setStatus(new BackgroundStatus(bid, BackgroundState.STARTING));
            Application.getInstance().getBackgroundMonitor().addItem(monItem);
            PushReceiver dpR= new PushReceiver(monItem,aloneUI);
            receivers.put(bid,dpR);
        }

        if (!aloneUI.hasResults()) {
            HTML message= new HTML("Waiting for data");
            GwtUtil.setStyles(message,  "width","100%",
                                        "fontSize", "16pt",
                                        "textAlign", "center");
            Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION).setDisplay(message);
        }

    }

}
