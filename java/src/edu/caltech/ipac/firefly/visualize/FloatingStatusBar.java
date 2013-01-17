package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.PopupPane;
import edu.caltech.ipac.firefly.ui.PopupType;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jun 1, 2012
 * Time: 2:56:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class FloatingStatusBar {
    private final PopupPane popup;
    private final PlotWidgetGroup group;
    private boolean dropClosed= true;
    private HandlerRegistration hreg= null;
    private HTML status = new HTML();
    private boolean showing= false;

    public FloatingStatusBar(PlotWidgetGroup group, Widget alignWidget) {
        this.group= group;
        IconCreator _ic= IconCreator.Creator.getInstance();
        HorizontalPanel panel= new HorizontalPanel();
        popup= new PopupPane("",panel, PopupType.LOW_PROFILE,false,false,false, PopupPane.HeaderType.NONE);
        updateAlignWidget(alignWidget);
        popup.setDoRegionChangeHide(false);
        panel.setSpacing(3);
        panel.add(status);

        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_CLOSE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                DeferredCommand.add(new Command() {
                    public void execute() {
                        dropClosed= true;
                        if (showing) popup.show();
                    }
                });
            }
        });

        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_OPEN, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                DeferredCommand.add(new Command() {
                    public void execute() {
                        dropClosed= false;
                        if (showing) popup.hide();
                    }
                });
            }
        });
    }

    public void updateAlignWidget(Widget w) {
        if (hreg!=null) {
            hreg.removeHandler();
            hreg= null;
        }
        popup.alignTo(w, PopupPane.Align.BOTTOM_LEFT, 0, 0);
        hreg= w.addDomHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                if (dropClosed) popup.show();
            }
        },MouseOverEvent.getType() );
    }

    public void updateStatus(String msg) {
        status.setHTML(msg);
        if (!popup.isVisible()) popup.show();
    }

    public void show() {
        showing= true;
        popup.show();
    }

    public void hide() {
        showing= false;
        popup.hide();
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