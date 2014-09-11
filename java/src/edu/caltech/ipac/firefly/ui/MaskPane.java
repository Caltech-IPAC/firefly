package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author tatianag
 * @version $Id: MaskPane.java,v 1.15 2010/03/16 22:20:18 loi Exp $
 */
public class MaskPane {

    public enum MaskHint {OnDialog, OnComponent }


    private PopupPanel maskPanel;
    private PopupPanel popup;
    private Widget     maskWidget;
    private Widget     popupWidget;

    private MaskTimer _maskTimer;
    private HandlerRegistration _blistRemover= null;
    private final MaskHint _hint;
    private boolean showing= true;


    /**
     *
     * @param maskWidget  widget to mask
     * @param popupWidget widget to popup
     */
    public MaskPane(Widget maskWidget,
                    Widget popupWidget) {

        this(maskWidget,popupWidget,MaskHint.OnComponent);
    }


    /**
     *
     * @param maskWidget  widget to mask
     * @param popupWidget widget to popup
     * @param hint type of widget this mask is on
     */
    public MaskPane(Widget maskWidget,
                    Widget popupWidget,
                    MaskHint hint) {

        _maskTimer = new MaskTimer();
        _hint= hint;

        this.maskWidget = maskWidget;
        this.popupWidget = popupWidget;


        maskPanel = new PopupPanel(false, false);
        popup = null;
        
    }

    public void show() {
        show(0);
    }

    public void show(int delay) {

        if (popup==null) {
            maskPanel.setAnimationEnabled(false);
//            maskPanel.setStyleName("firefly-mask-panel");
            maskPanel.setStyleName("firefly-mask-ui");
            maskPanel.setWidget(new Label()); // needed for style to take effect

            popup= new PopupPanel(false,false);
            popup.setWidget(popupWidget);
            popup.setStyleName("maskingPopup");
            PopupPane.addZIndexStyle(popup,"onTopDialogModal");

            if (_hint==MaskHint.OnComponent) {
                PopupPane.addZIndexStyle(maskPanel,"onTop");
//                maskPanel.addStyleName("onTop");
            }
            else if (_hint==MaskHint.OnDialog) {
//                maskPanel.addStyleName("onTopDialog");
                PopupPane.addZIndexStyle(maskPanel,"onTopDialog");
            }
            else {
//                maskPanel.addStyleName("onTop");
                PopupPane.addZIndexStyle(maskPanel,"onTop");
            }

            _blistRemover= Window.addResizeHandler(new BrowserHandler());
            showing= true;
        }
        _maskTimer.starts(delay);
    }

    public boolean isShowing() { return showing;  }

    public void hide() {
        if (popup!=null) {
            _maskTimer.cancel();
            popup.hide();
            maskPanel.hide();
            if (_blistRemover!=null) {
                _blistRemover.removeHandler();
                _blistRemover= null;
            }
            popup=null;
        }
        showing= false;
    }

    private boolean locateMask() {
        if (popup==null) return false;

        if (GwtUtil.isOnDisplay(maskWidget)) {
            final int w= maskWidget.getOffsetWidth();
            final int h= maskWidget.getOffsetHeight();

            //int pwidth= popup.getOffsetWidth();
            //int pheight= popup.getOffsetHeight();
            popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
                public void setPosition(int offsetWidth, int offsetHeight) {
                    int left = maskWidget.getAbsoluteLeft()+w/2 - offsetWidth/2;
                    int top = maskWidget.getAbsoluteTop() +h/2 - offsetHeight/2;
                    popup.setPopupPosition(left, top);
                }
            });
            //popup.setPopupPosition(maskWidget.getAbsoluteLeft()+w/2 - pwidth/2,
            //                       maskWidget.getAbsoluteTop() +h/2 - pheight/2);
            maskPanel.setPopupPosition(maskWidget.getAbsoluteLeft(),
                                       maskWidget.getAbsoluteTop());
            maskPanel.setWidth(w+"px");
            maskPanel.setHeight(h+"px");
//            popup.show();
            if (PopupPane.isMaskable())  maskPanel.show();
            return true;
        } else {
            popup.hide();
            maskPanel.hide();
            return false;
        }
    }


    private class BrowserHandler  implements ResizeHandler {
        public void onResize(ResizeEvent event) {
            locateMask();
        }
    }


    private class MaskTimer extends Timer {
        private long starts = 0;
        private long maxIdle = 3*60*1000;  // 3 mins max
        private int pollingTime = 300;

        private MaskTimer() {}

        private MaskTimer(long maxIdle) {
            this.maxIdle = maxIdle;
        }

        public void starts(int delayMillis) {
            starts = System.currentTimeMillis();
            schedule(delayMillis+1);
        }

        /**
         * run for as long as needed while the maskWidget is visible.
         * cancel the timer if the maskWidget is not visible for longer than
         * maxActive variable.
         */
        public void run() {
            long ctime = System.currentTimeMillis();
            if ( popup!=null && ((ctime - starts) < maxIdle) )  {
                if ( locateMask() ) {
                    starts = System.currentTimeMillis();
                }
                schedule(pollingTime);
            }
        }

//        public void setupCall( ) { }
    }




}
