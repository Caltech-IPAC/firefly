package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 9/7/12
 * Time: 3:21 PM
 */


import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.background.BackgroundMonitor;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * @author Trey Roby
 */
public class FloatingBackgroundManager extends PopupPane {

    public enum Position {TOP_RIGHT, UNDER_TOOLBAR}

    public FloatingBackgroundManager(Position position) {
        super("Background", Application.getInstance().getBackgroundManager(), PopupType.STANDARD,
              false,false,false,HeaderType.NONE);
        RootPanel w= RootPanel.get();
        if (position==Position.TOP_RIGHT) alignTo(w, Align.TOP_RIGHT);
        else                              alignTo(w, Align.TOP_LEFT,0, 84);

        WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_UPDATE, new WebEventListener<MonitorItem>() {
            public void eventNotify(WebEvent ev) {
                checkVisible();
            }
        });
        WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_REMOVED, new WebEventListener<MonitorItem>() {
            public void eventNotify(WebEvent ev) {
                checkVisible();
            }
        });

        WebEventManager.getAppEvManager().addListener(Name.BG_MANAGER_PRE_ANIMATE, new WebEventListener<MonitorItem>() {
            public void eventNotify(WebEvent ev) {
                show();
            }
        });
    }


    private void checkVisible() {
        BackgroundMonitor bm= Application.getInstance().getBackgroundMonitor();
        if (bm.getCount()>0) {
            show();
        }
        else {
            hide();
        }
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
