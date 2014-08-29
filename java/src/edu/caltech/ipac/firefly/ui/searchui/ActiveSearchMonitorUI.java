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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
