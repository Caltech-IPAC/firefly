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
