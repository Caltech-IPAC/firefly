package edu.caltech.ipac.frontpage.ui;
/**
 * User: roby
 * Date: 11/1/13
 * Time: 11:30 AM
 */


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.WebAssert;

/**
 * @author Trey Roby
 */
public class MorePullDown {

    public enum ShowType {Fixed, Centered}

    private static MorePullDown active= null;
    private Widget controlWidget;
    private Widget content;
    private PopupPanel pulldown= new PopupPanel();
    private HighlightLook highlightLook;
    private int offX= 0;
    private int offY= 0;
    private int maxWidth= -1;
    private final ShowType showType;



    public MorePullDown(Widget        controlWidget,
                        Widget        content,
                        HighlightLook highlightLook,
                        ShowType      showType) {
        this.controlWidget= controlWidget;
        this.content= content;
        this.highlightLook= highlightLook;
        this.showType= showType;
        init();
    }


    private void init() {
        pulldown.setStyleName("front-pulldown");
        pulldown.setWidget(content);
        content.addStyleName("centerLayoutPulldown");
        GwtUtil.setStyle(pulldown, "minWidth", "940px");

        pulldown.setAnimationEnabled(false);

        controlWidget.addDomHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                changeState();
            }
        }, ClickEvent.getType());

        Window.addResizeHandler(new ResizeHandler() {
            public void onResize(ResizeEvent event) {
                if (isShowing()) {
                    show();

                }
            }
        });
    }


    public Widget getWidget() {
        return pulldown;
    }

    private void changeState() {
        if (pulldown.isShowing()) {
            hide();
        }
        else {
            if (active!=null) active.hide();
            show();
            active= this;
        }

    }

    private void hide() {
        pulldown.hide();
        if (highlightLook!=null) highlightLook.disable();
    }

    public void setOffset(int offX, int offY) {
        this.offX= offX;
        this.offY= offY;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth= maxWidth;
    }



    private void show() {
        if (showType==ShowType.Centered) {
            showCentered();
        }
        else if (showType==ShowType.Fixed) {
            showFixed();
        }
        else {
            WebAssert.argTst(false, "not known showType");
        }
    }

    private void showCentered() {
        int cw= Window.getClientWidth();
        int width= cw-(20+offX);
        int xPos= 10+offX;
        if (maxWidth>-1 && width>maxWidth) {
            width= maxWidth;
            xPos= (cw-maxWidth)/2;
        }
        positionAndDisplay(xPos, width);

    }

    private void showFixed() {
        int cw= Window.getClientWidth();
        int width= cw-(20+offX);
        int xPos= 5+offX;
        if (maxWidth>-1 && width>maxWidth) {
            width= maxWidth;
        }
        positionAndDisplay(xPos, width);
    }


    private void positionAndDisplay(int xPos, int width) {
        pulldown.setWidth(width+"px");
        int y= controlWidget.getAbsoluteTop() + controlWidget.getOffsetHeight();

        pulldown.setPopupPosition(xPos, y+offY);

        pulldown.show();
        if (highlightLook!=null) highlightLook.enable();

    }


    private boolean isShowing() { return pulldown.isShowing();  }

    public static interface HighlightLook {
        public void enable();
        public void disable();
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
