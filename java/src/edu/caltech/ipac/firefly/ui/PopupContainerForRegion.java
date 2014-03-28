package edu.caltech.ipac.firefly.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.panels.BackButton;
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
public class PopupContainerForRegion implements  PopoutContainer {

    private static final int TOOLBAR_HEIGHT= 70;

    private static final int TOP_OFFSET= 75;
    protected boolean _showing= false;


    private final DockLayoutPanel _layout= new DockLayoutPanel(Style.Unit.PX);
    private final DockLayoutPanel headerBar= new DockLayoutPanel(Style.Unit.PX);
    private final HorizontalPanel headerLeft= new HorizontalPanel();
    private final BackButton _close = new BackButton("Close");
    private final SimplePanel titleBar = new SimplePanel();
    private PopoutWidget _popout;




//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public PopupContainerForRegion() {

        headerBar.setWidth("100%");
        headerBar.add(headerLeft);

        headerLeft.add(_close);

        _layout.setStyleName("standalone-expand");
        _layout.setSize("100%", "100%");

        headerLeft.add(GwtUtil.getFiller(10, 1));
        headerLeft.setStyleName("header");
        GwtUtil.setStyle(headerLeft, "padding", "5px 5px 0 0 ");
        headerLeft.add(GwtUtil.getFiller(30, 1));
        headerLeft.add(titleBar);
        titleBar.setStyleName("title-bar");
        headerLeft.setCellHorizontalAlignment(titleBar, HasHorizontalAlignment.ALIGN_LEFT);
        headerLeft.setCellWidth(titleBar, "100%");


        _close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) { hide(); }
        });
        GwtUtil.setStyle(_close, "marginLeft", "20px");


        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                if (_showing) {
                    ensureSize();
                }
            }
        });

        Window.addWindowScrollHandler(new Window.ScrollHandler() {
            public void onWindowScroll(Window.ScrollEvent event) {
                if (BrowserUtil.isIE()) {
                    ensureSize();
                }
            }
        });

    }


    private void ensureSize() {
        if (_popout.isExpanded() && GwtUtil.isOnDisplay(_layout)) {
            Dimension dim= getAvailableSize();
            Widget tlExpRoot= _popout.getToplevelExpandRoot();
            tlExpRoot.setPixelSize(dim.getWidth(), dim.getHeight());
            _layout.onResize();
        }
    }



    public void freeResources() {
    }

    public void setPopoutWidget(PopoutWidget popout) { _popout= popout; }



    public void show() {

        LayoutManager lm= Application.getInstance().getLayoutManager();
        _layout.clear();
        lm.getRegion(LayoutManager.POPOUT_REGION).setDisplay(_layout);
//        lm.getRegion(LayoutManager.POPOUT_REGION).show();
        _layout.addNorth(headerBar, TOOLBAR_HEIGHT);
        _layout.add(_popout.getToplevelExpandRoot());

        _showing= true;

        _close.setDesc("Collapse");
    }

    public void hide() {

        if (_showing) {
            LayoutManager lm= Application.getInstance().getLayoutManager();
            lm.getRegion(LayoutManager.POPOUT_REGION).hide();
            _showing= false;
            _popout.toggleExpand();
        }
    }



    public void setTitle(final String title) {
        Label l = new Label(title);
        GwtUtil.setStyles(l, "fontSize", "13pt", "paddingTop", "7px");
        if (GwtUtil.isHidden(_close.getElement())) {
            GwtUtil.setStyle(l, "paddingLeft", "60px");
        }
        setTitle(l);
    }


    public void setTitle(final Widget title) {
        titleBar.clear();
        titleBar.setWidget(title);
    }

    public Dimension getAvailableSize() {
        int w= _layout.getOffsetWidth()- 10;
        int h= _layout.getOffsetHeight() - TOP_OFFSET;
        return  new Dimension(w,h);
    }

    public boolean isExpanded() { return _showing; }


    public Panel getHeaderBar() { return headerLeft; }
//    public Panel getHeaderBar() { return null; }

    protected void dropDownCloseExecuted() { hide(); }
    protected void dropDownOpenExecuted() {
    }
    protected String getDropDownCloseButtonText() { return "Collapse";  }


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
