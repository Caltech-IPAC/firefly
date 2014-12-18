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
    private final WebEventListener openListener;
    private final WebEventListener closeListener;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public PopupContainerForToolbar() {


        closeListener=  new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (_showing) {
                    dropDownCloseExecuted();
                }
            }
        };

        openListener= new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (_showing) {
                    _showing= false;
                    collapse();
                }
            }
        };


        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_CLOSE,closeListener);
        WebEventManager.getAppEvManager().addListener(Name.DROPDOWN_OPEN,openListener);

    }

    public void freeResources() {
        WebEventManager.getAppEvManager().removeListener(Name.DROPDOWN_CLOSE,closeListener);
        WebEventManager.getAppEvManager().removeListener(Name.DROPDOWN_OPEN,openListener);
    }

    public void setPopoutWidget(PopoutWidget popout) { _popout= popout; }

    public void show() {
        Toolbar toolbar= Application.getInstance().getToolBar();
        toolbar.getDropdown().setContent(_popout.getToplevelExpandRoot(), false);
        toolbar.getDropdown().setCloseText(getDropDownCloseButtonText());
        toolbar.getDropdown().setAnimationEnabled(false);
        toolbar.getDropdown().setCloseButtonEnabled(isCloseShowing());
        _showing= true;
    }

    protected void expand() {
        _popout.forceExpand();
    }
    protected void collapse() {
        _popout.forceCollapse();
    }

    public void hide() {
        _showing= false;
        Application.getInstance().getToolBar().getDropdown().close(false);
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                Toolbar toolbar= Application.getInstance().getToolBar();
                toolbar.getDropdown().setAnimationEnabled(false);
                collapse();
                _popout.forceCollapse();
                toolbar.getDropdown().setAnimationEnabled(true);
            }
        });
    }

    public void hideOnlyDisplay() {
        _showing= false;
        Application.getInstance().getToolBar().getDropdown().close(false);
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                Toolbar toolbar= Application.getInstance().getToolBar();
                toolbar.getDropdown().setAnimationEnabled(false);
                collapse();
                toolbar.getDropdown().setAnimationEnabled(true);
            }
        });
    }

    public void setTitle(String title) {
        Application.getInstance().getToolBar().setTitle(title);
    }

    public void setTitle(Widget title) {
        Application.getInstance().getToolBar().getDropdown().setTitle(title);
    }

    public Dimension getAvailableSize() {
        Dimension dim = Application.getInstance().getToolBar().getDropdown().getDropDownSize();
       return  new Dimension(dim.getWidth(), dim.getHeight()-20);
    }

    public boolean isExpanded() { return _showing; }


    public Panel getHeaderBar() { return Application.getInstance().getToolBar().getDropdown().getHeaderButtons(); }

    protected void dropDownCloseExecuted() { hide(); }
    protected void dropDownOpenExecuted() {
       GwtUtil.showDebugMsg("dropDownOpenExecuted");
    }
    protected String getDropDownCloseButtonText() { return "Collapse";  }


    public boolean isCloseShowing() { return true; }
    public boolean isViewControlShowing() { return true; }
    public boolean isImageSelectionShowing() { return true; }
}
