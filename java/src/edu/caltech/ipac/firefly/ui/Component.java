package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * Date: Mar 2, 2009
 *
 * @author loi
 * @version $Id: Component.java,v 1.15 2010/05/27 22:11:45 loi Exp $
 */
public class Component extends Composite implements VisibleListener {

    public static final Name ON_SHOW     = new Name("Component.show",
                                                    "When the component becomes visible.");
    public static final Name ON_HIDE     = new Name("Component.hide",
                                                    "When the component becomes invisible");
    public static final Name ON_INIT     = new Name("Component.init",
                                                    "When the component is initialized");
    public static final Name ON_ATTACH     = new Name("Component.attach",
                                                    "When the component is attach to the DOM");
    public static final Name ON_DETACH     = new Name("Component.detach",
                                                    "When the component is detach from the DOM");
    public static final Name ON_LOAD     = new Name("Component.load",
                                                    "After the component is attach and loaded");
    public static final Name ON_UNLOAD     = new Name("Component.unload",
                                                    "When the component is unloaded");

    private MaskPane maskPane;
    private boolean isInit = true;
    private WebEventManager eventManager = new WebEventManager();

    protected Component() {
    }

    public Component(Widget wrappedWidget) {
        initWidget(wrappedWidget);
    }

    public WebEventManager getEventManager() {
        return eventManager;
    }

    public boolean isInit() {
        return isInit;
    }

    protected void setInit(boolean init) {
        isInit = init;
        if (init) {
            onInit();
        }
    }

    public void unmask() {
        if (maskPane != null) {
            maskPane.hide();
        }
    }

    public void mask(String msg) {
        mask(msg, 0);
    }

    public void mask(String msg, int delay) {
        mask(msg, delay, true);
    }

    public void mask(String msg, int delay, boolean onlyMaskWhenUncovered) {
        if (maskPane == null) {
            maskPane = GwtUtil.mask(msg, this, onlyMaskWhenUncovered);
        } else {
            maskPane.show(delay);
        }
    }

//====================================================================
//
//====================================================================

    @Override
    protected void onAttach() {
        try {
            super.onAttach();
            getEventManager().fireEvent(new WebEvent(this, ON_ATTACH));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDetach() {
        try {
            super.onDetach();
            getEventManager().fireEvent(new WebEvent(this, ON_DETACH));
        } catch(Exception ex) {
            // do nothing...
        }
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        getEventManager().fireEvent(new WebEvent(this, ON_LOAD));
        DeferredCommand.addCommand(new Command(){
                public void execute() {
                    if (GwtUtil.isOnDisplay(Component.this)) {
                        if (isInit()) {
                            onShow();
                        } else {
                            // due to async init, this init() could return false.
                            // in that case, add a listener to execute this request once it is initialized.
                            eventManager.addListener(ON_INIT, new WebEventListener(){
                                    public void eventNotify(WebEvent ev) {
                                        eventManager.removeListener(ON_INIT, this);
                                        onShow();
                                    }
                                });
                        }
                    }
                }
            });
    }

    @Override
    protected void onUnload() {
        super.onUnload();
        getEventManager().fireEvent(new WebEvent(this, ON_UNLOAD));
        onHide();
    }

    @Override
    public void setVisible(boolean visible) {
        boolean origVisible = isVisible();
        super.setVisible(visible);
        this.getWidget().setVisible(visible);
        if (!origVisible && visible) {
            onShow();
        } else if (origVisible && !visible) {
            onHide();
        }
    }

    public void onInit() {
        getEventManager().fireEvent(new WebEvent(this, ON_INIT));
    }

    public void onShow() {
        getEventManager().fireEvent(new WebEvent(this, ON_SHOW));
    }

    public void onHide() {
        getEventManager().fireEvent(new WebEvent(this, ON_HIDE));
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
