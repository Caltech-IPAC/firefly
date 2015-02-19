/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.searchui;
/**
 * User: roby
 * Date: 6/20/14
 * Time: 10:58 AM
 */


import com.google.gwt.user.client.ui.FlowPanel;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ActiveSearchMonitorUI {


    private final FlowPanel mainPanel= new FlowPanel();
    private List<ServerRequest> submittedReqList = new ArrayList<ServerRequest>(10);


    public ActiveSearchMonitorUI(final FuseSearchPanel searchPanel) {

        WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_CREATE, new WebEventListener<MonitorItem>() {
            public void eventNotify(WebEvent<MonitorItem> ev) {
                MonitorItem item= ev.getData();
                if (item.getUIHint()== BackgroundUIHint.RAW_DATA_SET) {
                    new SearchItemMonitorUI(ActiveSearchMonitorUI.this, searchPanel, item);
                    ServerRequest insertReq= new ServerRequest();
                    insertReq.setParams(item.getRequest().getParams());
                    submittedReqList.add(insertReq);
                }
            }
        });

        GwtUtil.setStyles(mainPanel, "whiteSpace", "nowrap",
                                     "overflow", "auto");
    }

    public boolean isADuplicate(ServerRequest r) {
        ServerRequest testReq= new ServerRequest();
        testReq.setParams(r.getParams());
        return (submittedReqList.contains(testReq));
    }

    public void clear() {
        submittedReqList.clear();
        mainPanel.clear();
    }

    public FlowPanel getWidget() { return mainPanel; }


}

