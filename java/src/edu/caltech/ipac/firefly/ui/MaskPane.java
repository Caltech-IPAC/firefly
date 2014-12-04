package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.ui.panels.ToolbarDropdown;

/**
 * @author tatianag
 * @version $Id: MaskPane.java,v 1.15 2010/03/16 22:20:18 loi Exp $
 */
public class MaskPane {

    public enum MaskHint {OnDialog, OnComponent }


    private PopupPanel maskPanel;
    private PopupPanel popup;
    private Widget     maskWidget= null;
    private Element     maskElement;
    private Widget     popupWidget;
    private MaskTimer maskTimer;
    private HandlerRegistration blistRemover = null;
    private final MaskHint _hint;
    private boolean showing= true;
    private boolean waitingToMask= true;


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
     * @param maskElement  widget to mask
     * @param popupWidget widget to popup
     */
    public MaskPane(Element maskElement,
                    Widget popupWidget) {

        this(maskElement,popupWidget,MaskHint.OnComponent);
    }

    public MaskPane(Widget maskWidget,
                    Widget popupWidget,
                    MaskHint hint) {
        this(maskWidget.getElement(),popupWidget,hint);
        this.maskWidget= maskWidget;
    }


    /**
     *
     * @param maskElement  widget to mask
     * @param popupWidget widget to popup
     * @param hint type of widget this mask is on
     */
    public MaskPane(Element maskElement,
                    Widget popupWidget,
                    MaskHint hint) {

        maskTimer = new MaskTimer();
        _hint= hint;

        this.maskElement = maskElement;
        this.popupWidget = popupWidget;


        maskPanel = new PopupPanel(false, false);
        popup = null;
        
    }

    public void show() {
        show(0);
    }


    public void showWhenUncovered() {
        waitingToMask= true;
        final Application app= Application.getInstance();
        final ToolbarDropdown dropdown= (app.getToolBar()!=null) ? app.getToolBar().getDropdown() : null;
        if (dropdown==null) {
            show();
        }
        else if (GwtUtil.isParentOf(maskElement, dropdown.getElement())) {
            show();
        }
        else {
            Timer t= new Timer() {
                @Override
                public void run() {
                    if (waitingToMask) {
                        if (dropdown.isOpen()) hide();
                        else                   show();
                        if (!isShowing()) schedule(500);
                    }
                }
            };
            t.schedule(100);
        }
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
                maskPanel.addStyleName("onTop");
            }
            else if (_hint==MaskHint.OnDialog) {
                maskPanel.addStyleName("onTopDialog");
            }
            else {
                maskPanel.addStyleName("onTop");
            }

            blistRemover = Window.addResizeHandler(new BrowserHandler());
            showing= true;
        }
        maskTimer.starts(delay);
    }

    public boolean isShowing() { return showing;  }

    public void hide() {
        doHide();
        waitingToMask= false;
    }

    private void doHide() {
        if (popup!=null) {
            maskTimer.cancel();
            popup.hide();
            maskPanel.hide();
            if (blistRemover !=null) {
                blistRemover.removeHandler();
                blistRemover = null;
            }
            popup=null;
        }
        showing= false;
    }

    private boolean locateMask() {
        if (popup==null) return false;

        if (GwtUtil.isOnDisplay(maskElement)) {
            final int w= maskElement.getOffsetWidth();
            final int h= maskElement.getOffsetHeight();

            //int pwidth= popup.getOffsetWidth();
            //int pheight= popup.getOffsetHeight();
            popup.setPopupPositionAndShow(new PopupPanel.PositionCallback() {
                public void setPosition(int offsetWidth, int offsetHeight) {
                    int left = maskElement.getAbsoluteLeft()+w/2 - offsetWidth/2;
                    int top = maskElement.getAbsoluteTop() +h/2 - offsetHeight/2;
                    popup.setPopupPosition(left, top);
                }
            });
            //popup.setPopupPosition(maskWidget.getAbsoluteLeft()+w/2 - pwidth/2,
            //                       maskWidget.getAbsoluteTop() +h/2 - pheight/2);
            maskPanel.setPopupPosition(maskElement.getAbsoluteLeft(),
                                       maskElement.getAbsoluteTop());
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
