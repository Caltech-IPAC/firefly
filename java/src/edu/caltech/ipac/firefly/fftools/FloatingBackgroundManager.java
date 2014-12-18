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
        super("Background", Application.getInstance().getBackgroundManager().getButton(), PopupType.STANDARD,
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

