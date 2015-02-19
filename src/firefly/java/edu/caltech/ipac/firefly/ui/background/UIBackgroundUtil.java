/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.background;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.BackgroundActivation;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * User: roby
 * Date: Dec 18, 2009
 * Time: 4:23:35 PM
 */


/**
 * @author Trey Roby
 */
public class UIBackgroundUtil {

    public static final String RETRIEVED_ICON= GWT.getModuleBaseURL()+ "images/blue_check-on_10x10.gif";

    public static Widget buildActivationUI(String text,
                                           String tip,
                                           final MonitorItem monItem,
                                           final int idx,
                                           final BackgroundActivation bActivate,
                                           boolean markAlreadyActivated) {

        final Image icon= new Image(RETRIEVED_ICON);
        icon.setVisible(markAlreadyActivated);

        Widget button= GwtUtil.makeLinkButton(text, tip,
                                              new ClickHandler() {
                                                  public void onClick(ClickEvent event) {
                                                      bActivate.activate(monItem,idx, false);
                                                      icon.setVisible(true);
                                                  }
                                              });

        WebEventListener autoActListener= new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (ev.getSource()==monItem && ev.getName()==Name.MONITOR_ITEM_UPDATE) {
                    if (monItem.isDone() && monItem.isActivated(idx)) {
                        icon.setVisible(true);
                        WebEventManager.getAppEvManager().removeListener(Name.MONITOR_ITEM_UPDATE,
                                                                         monItem,this);
                    }

                }
            }
        };


        WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_UPDATE,monItem,autoActListener);



        FlexTable fp= new FlexTable();
        HTMLTable.CellFormatter formatter= fp.getCellFormatter();

        fp.setWidget(0,0,button);
        formatter.setWidth(0,0,"100px");

        fp.setWidget(0,4,icon);
        formatter.setWidth(0,4,"20px");
        formatter.setHorizontalAlignment(0,4, HasHorizontalAlignment.ALIGN_RIGHT);
        return fp;
    }

}

