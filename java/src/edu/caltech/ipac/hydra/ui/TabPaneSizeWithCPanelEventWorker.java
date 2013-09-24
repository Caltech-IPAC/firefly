package edu.caltech.ipac.hydra.ui;

import com.google.gwt.user.client.DOM;
import edu.caltech.ipac.firefly.ui.ActiveTabPane;
import edu.caltech.ipac.firefly.ui.FormHub;
import edu.caltech.ipac.firefly.ui.creator.eventworker.BaseFormEventWorker;
import edu.caltech.ipac.firefly.ui.panels.CollapsiblePanel;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;


/*
 * this event worker does not fully function properly - needs more error handling and event state logic
 * probably needs to be integrated with TabPaneSizeEventWorker
 */
public class TabPaneSizeWithCPanelEventWorker extends BaseFormEventWorker {

    private static final String TAB_PANE_NAME = "tabPaneName";

    private String tabPaneName = "";
    Map<String, String> sizeMap = new HashMap<String, String>();

    TabPaneSizeWithCPanelEventWorker(Map<String, String> params) {
        if (params.containsKey(TAB_PANE_NAME)) {
            this.tabPaneName = params.get(TAB_PANE_NAME);
            params.remove(TAB_PANE_NAME);
        }

        sizeMap = params;
    }

    public void bind(FormHub hub) {
        if (hub != null) {
            addHub(hub);

            hub.getEventManager().addListener(FormHub.ON_PANEL_OPENED, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    CollapsiblePanel cp = (CollapsiblePanel) ev.getSource();
                    String pName = cp.getPanelName();
                    if (!StringUtils.isEmpty(pName) && sizeMap.containsKey(pName)) {
                        String sizes = sizeMap.get(pName);
                        String[] sizeArr = sizes.split(",");
                        int dHeight = 0;
                        int dWidth = 0;
                        for (String size : sizeArr) {
                            String[] dimen = size.split("=");
                            if (dimen.length == 2) {
                                if (dimen[0].equalsIgnoreCase("deltaHeight")) {
                                    dHeight = Integer.parseInt(dimen[1]);
                                } else if (dimen[0].equalsIgnoreCase("deltaWidth")) {
                                    dWidth = Integer.parseInt(dimen[1]);
                                }
                            }
                        }

                        TabPane tp;
                        ActiveTabPane atp = getActiveTabPane(tabPaneName);
                        if (atp != null) {
                            tp = atp.getTabPane();

                        } else {
                            tp = getTabPane(tabPaneName);
                        }

                        if (tp != null) {
                            //int h = tp.getOffsetHeight();
                            int h = DOM.getIntStyleAttribute(tp.getElement(), "height");
                            int w = DOM.getIntStyleAttribute(tp.getElement(), "width");

                            tp.setPixelSize(w + dWidth, h + dHeight);
                            tp.forceLayout();
                        }
                    }
                }
            });

            hub.getEventManager().addListener(FormHub.ON_PANEL_CLOSED, new WebEventListener() {
                public void eventNotify(WebEvent ev) {
                    CollapsiblePanel cp = (CollapsiblePanel) ev.getSource();
                    String pName = cp.getPanelName();
                    if (!StringUtils.isEmpty(pName) && sizeMap.containsKey(pName)) {
                        String sizes = sizeMap.get(pName);
                        String[] sizeArr = sizes.split(",");
                        int dHeight = 0;
                        int dWidth = 0;
                        for (String size : sizeArr) {
                            String[] dimen = size.split("=");
                            if (dimen.length == 2) {
                                if (dimen[0].equalsIgnoreCase("deltaHeight")) {
                                    dHeight = Integer.parseInt(dimen[1]);
                                } else if (dimen[0].equalsIgnoreCase("deltaWidth")) {
                                    dWidth = Integer.parseInt(dimen[1]);
                                }
                            }
                        }

                        TabPane tp;
                        ActiveTabPane atp = getActiveTabPane(tabPaneName);
                        if (atp != null) {
                            tp = atp.getTabPane();

                        } else {
                            tp = getTabPane(tabPaneName);
                        }

                        if (tp != null) {
                            //int h = tp.getOffsetHeight();
                            int h = DOM.getIntStyleAttribute(tp.getElement(), "height");
                            int w = DOM.getIntStyleAttribute(tp.getElement(), "width");

                            tp.setPixelSize(w - dWidth, h - dHeight);
                            tp.forceLayout();
                        }
                    }
                }
            });
        }
    }

    public void bind(EventHub hub) {
        // n/a
    }
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
