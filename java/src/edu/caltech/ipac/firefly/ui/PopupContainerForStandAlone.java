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
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.panels.BackButton;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;


/**
 * User: roby
 * Date: May 19, 2008
 * Time: 2:35:43 PM
 */


/**
 * @author Trey Roby
 */
public class PopupContainerForStandAlone implements  PopoutContainer {

    private static final int HEIGHT_OFFSET= 133;
    private static final int TOOLBAR_HEIGHT= 40;

    private final PopupPanel _main = new PopupPanel();
    private final PopupPanel _topBackground = new PopupPanel();
    private final DockLayoutPanel _layout= new DockLayoutPanel(Style.Unit.PX);
    private final DockLayoutPanel headerBar= new DockLayoutPanel(Style.Unit.PX);
    private final HorizontalPanel headerLeft= new HorizontalPanel();
    private final BackButton _close = new BackButton("Close");
    private final  SimplePanel titleBar = new SimplePanel();
    private PopoutWidget _popout;
    private final boolean fullControl;
    private boolean _closeBrowserWindow= false;

    private boolean _showing= false;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public PopupContainerForStandAlone(boolean fullControl) {

        this.fullControl= fullControl;
        headerBar.setWidth("100%");

        if (fullControl) {
            headerBar.addEast(Application.getInstance().getBackgroundManager(),200);
            AllPlots.getInstance().setToolPopLeftOffset(100);
        }
        else {

        }
        headerBar.add(headerLeft);


        if (false) {
            RootPanel.get().add(_close);
            _close.addStyleName("close-button-position");

        }
        else {
            headerLeft.add(_close);
        }


        _topBackground.setStyleName("NO_STYLE");
        _topBackground.addStyleName("onTop");
        _topBackground.setAnimationEnabled(false);

        GwtUtil.setStyles(_topBackground, "backgroundColor", "gray",
                                          "opacity", ".4",
                                          "position", "fixed",
                                          "left", "0px",
                                          "top", "0px",
                                          "height", Window.getClientHeight()+"px",
                                          "width", "100%");

        _main.setStyleName("standalone-expand");
        _main.addStyleName("onTopDialog");
        int zIndex= Application.getInstance().getDefZIndex();
        if (zIndex>0) {
            GwtUtil.setStyle(_main, "zIndex", zIndex+"");
            GwtUtil.setStyle(_topBackground, "zIndex", (zIndex-1)+"");
        }
        _main.setWidget(_layout);
        _layout.setSize("100%", "100%");

        headerLeft.add(GwtUtil.getFiller(10, 1));
        headerLeft.setStyleName("header");
//        GwtUtil.setStyles(headerBar, "paddingLeft", "0px",
//                                     "paddingTop", "5px");
        GwtUtil.setStyle(headerLeft, "padding", "5px 5px 0 0 ");
        headerLeft.add(GwtUtil.getFiller(30, 1));
        headerLeft.add(titleBar);
        titleBar.setStyleName("title-bar");
        headerLeft.setCellHorizontalAlignment(titleBar, HasHorizontalAlignment.ALIGN_LEFT);
        headerLeft.setCellWidth(titleBar, "100%");


        _close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                hide();
            }
        });
        GwtUtil.setStyle(_close, "marginLeft", "20px");


        WebEventManager.getAppEvManager().addListener(
                Name.DROPDOWN_CLOSE,
                new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        if (_showing) {
                            if (_closeBrowserWindow) {
                                doCloseBrowserWindow();
                            }
                            else {
                                hide();
                            }
                        }
                    }
                });

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                if (_showing) {
                    computePosition(false);
                    ensureSize();
                }
            }
        });

        Window.addWindowScrollHandler(new Window.ScrollHandler() {
            public void onWindowScroll(Window.ScrollEvent event) {
                if (BrowserUtil.isIE()) {
                    computePosition(false);
                    ensureSize();
                }
            }
        });

    }

    public void setPopoutWidget(PopoutWidget popout) { _popout= popout; }

    public void setCloseBrowserWindow(boolean close) { _closeBrowserWindow= close; }

    public void show() {

        GwtUtil.setStyle(_topBackground, "height", RootPanel.get().getOffsetHeight()+"px");
        if (!fullControl)  _topBackground.show();
        _layout.clear();
        _layout.addNorth(headerBar, TOOLBAR_HEIGHT);
        _layout.add(_popout.getToplevelExpandRoot());


        _main.setAnimationEnabled(true);
        computePosition(true);
        _main.show();

        _showing= true;

        _close.setDesc(fullControl? "Close" : "Collapse");
        computePosition(false);
    }

    public void hide() {

        if (_showing) {
            _topBackground.hide();
            _showing= false;
            _main.hide();
            _popout.toggleExpand();
            if (_closeBrowserWindow) {
                doCloseBrowserWindow();
            }
        }
    }

    public void setTitle(String title) {
        Label l = new Label(title);
        GwtUtil.setStyles(l, "fontSize", "13pt", "paddingTop", "7px");
        if (GwtUtil.isHidden(_close.getElement())) {
            GwtUtil.setStyle(l, "paddingLeft", "60px");
        }
        setTitle(l);
    }


    public void setTitle(Widget title) {
        titleBar.setWidget(title);
    }

    public Dimension getAvailableSize() {
        int w= Window.getClientWidth();
        int h= Window.getClientHeight();
        return  new Dimension(w-20,h-(HEIGHT_OFFSET+20+TOOLBAR_HEIGHT));
    }

    public boolean isExpanded() { return true; }


    public Panel getHeaderBar() { return headerLeft; }



    public void computePosition(boolean offset) {
        if (_popout.isExpanded()) {
            if (BrowserUtil.isOldIE() || offset) {
                GwtUtil.setStyle(_main, "position", "absolute");
                GwtUtil.setStyle(_topBackground, "position", "absolute");
                int x= Window.getScrollLeft();
                int y= Window.getScrollTop();
                _main.setPopupPosition(x+5,y+HEIGHT_OFFSET);
            }
            else {
                GwtUtil.setStyle(_main, "position", "fixed");
                GwtUtil.setStyle(_topBackground, "position", "fixed");
                GwtUtil.setStyles(_main, "left", "5px",
                                         "top", HEIGHT_OFFSET + "px");

            }
            int w= Window.getClientWidth();
            int h= Window.getClientHeight();
            _main.setPixelSize(w-20, h - (HEIGHT_OFFSET+20));
            _popout.onResize();
            _layout.onResize();
        }
    }


    private void ensureSize() {
        Widget tlExpRoot= _popout.getToplevelExpandRoot();
        if (_popout.isExpanded() && GwtUtil.isOnDisplay(tlExpRoot)) {
            _topBackground.setHeight(Window.getClientHeight()+"px");
            Dimension dim= getAvailableSize();
            tlExpRoot.setPixelSize(dim.getWidth(), dim.getHeight());
            if (tlExpRoot instanceof RequiresResize) {
                ((RequiresResize)tlExpRoot).onResize();
            }
        }
    }

    private static native void doCloseBrowserWindow()    /*-{
        $wnd.close();
    }-*/;
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
