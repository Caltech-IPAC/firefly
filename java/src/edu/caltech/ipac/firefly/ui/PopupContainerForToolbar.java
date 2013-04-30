package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;



/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * @author Trey Roby
 */
public class PopupContainerForToolbar implements  PopoutContainer {


    public static final int DEF_MIN_WIDTH=   270;
    public static final int DEF_MIN_HEIGHT=  150;
    private static final boolean _forceIE6Layout = BrowserUtil.isBrowser(Browser.IE,6);


    protected PopoutWidget _popout;
    protected boolean _showing= false;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public PopupContainerForToolbar() {


        WebEventManager.getAppEvManager().addListener(
                Name.DROPDOWN_CLOSE,
                new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (_showing) {
                            dropDownCloseExecuted();
                        }
                    }
                });
        WebEventManager.getAppEvManager().addListener(
                Name.DROPDOWN_OPEN,
                new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (_showing) {
                            _showing= false;
                            toggleExpand();
                        }
                    }
                });

    }

    public void setPopoutWidget(PopoutWidget popout) { _popout= popout; }

    public void show() {
        Toolbar toolbar= Application.getInstance().getToolBar();
        toolbar.setContent(_popout.getToplevelExpandRoot(), false);
        toolbar.setCloseText(getDropDownCloseButtonText());
        toolbar.setAnimationEnabled(false);
        _showing= true;
    }

    protected void toggleExpand() {
        _popout.toggleExpand();
    }

    public void hide() {
        _showing= false;
        Application.getInstance().getToolBar().close(false);
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                Toolbar toolbar= Application.getInstance().getToolBar();
                toolbar.setAnimationEnabled(false);
                toggleExpand();
                toolbar.setAnimationEnabled(true);
            }
        });
    }

    public void setTitle(String title) {
        Application.getInstance().getToolBar().setTitle(title);
    }

    public void setTitle(Widget title) {
        Application.getInstance().getToolBar().setTitle(title);
    }

    public Dimension getAvailableSize() {
        Dimension dim = Application.getInstance().getToolBar().getDropDownSize();
       return  new Dimension(dim.getWidth(), dim.getHeight()-20);
    }

    public boolean isExpanded() { return true; }


    public Panel getHeaderBar() { return Application.getInstance().getToolBar().getHeaderButtons(); }

    protected void dropDownCloseExecuted() { hide(); }
    protected void dropDownOpenExecuted() {
       GwtUtil.showDebugMsg("dropDownOpenExecuted");
    }
    protected String getDropDownCloseButtonText() { return "Collapse";  }

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
