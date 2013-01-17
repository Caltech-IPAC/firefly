package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.Iterator;

/**
 * Date: Jun 20, 2008
 *
 * @author loi
 * @version $Id: CollapsiblePanel.java,v 1.24 2011/09/28 02:18:47 loi Exp $
 */
public class CollapsiblePanel extends Composite implements HasWidgets {

    private DisclosurePanel disclosurePanel;
    private WebEventManager eventManager = new WebEventManager();

    public static final Name PANEL_OPENED = new Name("Panel.Opened", "When a panel is opened.");
    public static final Name PANEL_CLOSED = new Name("Panel.Closed", "When a panel is closed.");

    private String panelName;


    public CollapsiblePanel(String title) {
        this(title, null, true);
    }

    public CollapsiblePanel(String title, Widget content, boolean isOpen) {
        if (title == null) {
            this.disclosurePanel = new DisclosurePanel();
        } else {
            this.disclosurePanel = new DisclosurePanel(title);
        }
        disclosurePanel.setOpen(isOpen);

        if (content != null)  {
            disclosurePanel.setContent(content);
        }

        initWidget(this.disclosurePanel);

        disclosurePanel.addCloseHandler(new CloseHandler<DisclosurePanel>() {
            public void onClose(CloseEvent<DisclosurePanel> ev) {
                handleCallback();
                eventManager.fireEvent(new WebEvent<CollapsiblePanel>(CollapsiblePanel.this, PANEL_CLOSED, null));
            }
        });

        disclosurePanel.addOpenHandler(new OpenHandler<DisclosurePanel>() {
            public void onOpen(OpenEvent<DisclosurePanel> ev) {
                handleCallback();
                eventManager.fireEvent(new WebEvent<CollapsiblePanel>(CollapsiblePanel.this, PANEL_OPENED, null));
            }
        });

        disclosurePanel.setAnimationEnabled(true);
    }


    public WebEventManager getEventManager() {
        return eventManager;
    }

    public String getPanelName() {
        if (StringUtils.isEmpty(panelName)) {
            return "";
        } else {
            return panelName;
        }
    }
    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }


    protected DisclosurePanel getDisclosurePanel() {
        return disclosurePanel;
    }

    public void setAnimationEnabled(boolean enabled) {
        disclosurePanel.setAnimationEnabled(enabled);
    }

    public Widget getContent() {
        return disclosurePanel.getContent();
    }

    public void setContent(Widget content) {
        this.disclosurePanel.setContent(content);
    }

    protected void onClose() {
    }

    protected void onOpen() {
    }

    public boolean isCollapsed() {
        return !disclosurePanel.isOpen();
    }

    public void collapse() {
        if (disclosurePanel.isOpen()) {
            disclosurePanel.setOpen(false);
        }
    }

    public void expand() {
        if (!disclosurePanel.isOpen()) {
            DeferredCommand.addCommand(new Command(){
                        public void execute() {
                            disclosurePanel.setOpen(true);
                        }
                    });
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        Application.getInstance().resize();
    }

    //====================================================================
//
//====================================================================

    public void add(Widget w) {
        disclosurePanel.add(w);
    }

    public void clear() {
        disclosurePanel.clear();
    }

    public Iterator<Widget> iterator() {
        return disclosurePanel.iterator();
    }

    public boolean remove(Widget w) {
        return disclosurePanel.remove(w);
    }

//====================================================================
//
//====================================================================
    private void handleCallback() {
        if (disclosurePanel.isAnimationEnabled()) {
            if (disclosurePanel.getContent() != null && disclosurePanel.getContent().getParent() != null) {
                Timer t = new Timer(){
                    int tries = 0;
                    public void run() {
                            tries++;
                            String h = DOM.getStyleAttribute(disclosurePanel.getContent().getParent().getElement(), "height");
                            if (tries > 20 || h == null || h.equals("auto") || h.equals("")) {
                                postAction();
//                        GWT.log("#tries: " + tries, null);
                                cancel();
                            }
                        }
                };
                t.scheduleRepeating(250);
                return;
            }
        }
        postAction();
    }

    private void postAction() {
        if (CollapsiblePanel.this.isCollapsed()) {
            onClose();
        } else {
            onOpen();
        }
        Application.getInstance().resize();
    }

//====================================================================
//
//====================================================================

    public static interface AnimationCallback {
        void onComplete();
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
