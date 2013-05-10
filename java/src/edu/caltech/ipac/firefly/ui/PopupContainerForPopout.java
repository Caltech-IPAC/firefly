package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;

/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * @author Trey Roby
 */
public class PopupContainerForPopout implements  PopoutContainer {


    public static final int DEF_MIN_WIDTH=   270;
    public static final int DEF_MIN_HEIGHT=  150;
    private static final boolean _forceIE6Layout = BrowserUtil.isBrowser(Browser.IE,6);


    private PopupPane _expandPopout;
    private PopoutWidget _popout;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public PopupContainerForPopout() { }

    public void setPopoutWidget(PopoutWidget popout) {
        _popout= popout;
        _expandPopout= new PopupPane(null,null,PopupType.STANDARD, false,false) {
            protected void onClose() {
                DeferredCommand.addCommand(new Command() { public void execute() { _popout.toggleExpand();  } });
            }
        };

        _expandPopout.setWidget(popout.getToplevelExpandRoot());
    }

    public PopupPane getPopupPane() { return _expandPopout;  }

    public void show() {
        Widget regW  = findPopoutRegionWidget();
        _expandPopout.alignTo(regW, PopupPane.Align.TOP_LEFT);
        _expandPopout.show();
    }

    public void hide() {
        _expandPopout.hide();
    }

    public void setTitle(String title) {
        _expandPopout.setHeader(title);
    }

    public void setTitle(Widget title) {
        _expandPopout.setHeader("Widget not support for title");
    }

    public Dimension getAvailableSize() {
        Widget regW  = findPopoutRegionWidget();
        int w= Math.min(Window.getClientWidth(),  regW.getOffsetWidth());
        int h= Math.min(Window.getClientHeight(), regW.getOffsetHeight());
        return new Dimension(w,h);
    }

    public boolean isExpanded() { return _expandPopout.isVisible(); }

    public static Widget findPopoutRegionWidget() {
        Widget retval;
        Region r= Application.getInstance().getLayoutManager().getRegion(LayoutManager.RESULT_REGION);
        if (r==null || r.getDisplay().getOffsetWidth()==0) {
            r= Application.getInstance().getLayoutManager().getRegion(LayoutManager.CONTENT_REGION);
        }
        if (r==null || r.getDisplay().getOffsetWidth()==0) {
            retval= RootPanel.get();
        }
        else {
            retval= r.getDisplay();
        }
        return retval;

    }

    public Panel getHeaderBar() { return null; }

    public boolean isCloseShowing() { return true; }
    public boolean isViewControlShowing() { return true; }
    public boolean isImageSelectionShowing() { return true; }
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
