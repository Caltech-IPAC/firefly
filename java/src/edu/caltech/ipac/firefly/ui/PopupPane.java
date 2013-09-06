package edu.caltech.ipac.firefly.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HumanInputEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.gwtclone.GwtPopupPanelFirefly;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.CssAnimation;
import edu.caltech.ipac.firefly.util.Platform;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;


/**
 * Date: Jun 11, 2008
 *
 * @author loi
 * @version $Id: PopupPane.java,v 1.77 2012/10/24 18:35:15 roby Exp $
 */
public class PopupPane implements HasCloseHandlers<PopupPane> {

    private static final String UP_POPUP_POINTER = "images/up-pointer.gif";
    private static final String LEFT_DOWN_POPUP_POINTER = "images/left-down-pointer.gif";
    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();
    private static final int DROP_DOWN_TIME=300;

    private enum PointerDir {NONE, NORTH, SOUTH_WEST}
    public enum HeaderType {TOP,SIDE,NONE}

    public static enum Align  {TOP_LEFT, BOTTOM_LEFT, TOP_RIGHT, BOTTOM_RIGHT,
                               TOP_LEFT_POPUP_BOTTOM, TOP_LEFT_POPUP_RIGHT,
                               BOTTOM_CENTER,TOP_RIGHT_OR_LEFT,
                               BOTTOM_CENTER_POPUP_BOTTOM,TOP_RIGHT_POPUP_RIGHT,
                               AUTO_POINTER, TOP_CENTER, CENTER, DISABLE,
                               VISIBLE_BOTTOM}
    private enum PopupRegion { NW_CORNER, NE_CORNER, SE_CORNER, SW_CORNER, TITLE_BAR, NONE}

    private static final int HOR_PTR_IMAGE_OFFSET= -6;
//    private static final int VER_PTR_IMAGE_OFFSET= -4;

    private boolean visible;
    private boolean _hiding= false;
    private GwtPopupPanelFirefly popup;
    private String header;
    private SimplePanel content;
    private Widget headerWidget;
    private Timer timer;
    private static int cnt= 0;


    // align info
    private HandlerManager hMan= new HandlerManager(this);
    private Widget alignWidget;
    private Align alignAt= Align.CENTER;
    private int xOffset;
    private int yOffset;
    private int _reqPosX;
    private int _reqPosY;
    private HTML titleLabel = new HTML();
    private boolean _doAlign= true;
    private boolean _autoAlign = true;
    private boolean _rolldownCSSAnimation= false;
    private StyleElement _styleElement= null;

    private DecoratorPanel _decoratorPanel= new DecoratorPanel();
    private Panel _mainPanel;

    private static final int MARGIN= 10;

    private int _xDiff;
    private int _yDiff;
    private int _originalX;
    private int _originalY;
    private int _originalMouseX;
    private int _originalMouseY;
    private PopupRegion _resizeCorner= PopupRegion.NONE;
    private boolean _resizing= false;
    private boolean _moving= false;
    private boolean firstAlign = true;
    private boolean doRegionChangeHide= true;
    private boolean animationEnabled= false;
    private Widget _expandW;
    private final PopupType _ptype;

    private final PopupPanel _maskPanel;
    private final boolean _masking;
    private final boolean _pointerPopup;
    private Widget _hideOnResizeWidget = null;
    private Widget _maskWidget = RootPanel.get();
    private Image _pointerIm;
    private PointerDir _currPtDir= PointerDir.NONE;
    private int optimalHeight= 0;
    private int optimalWidth= 0;
    private int minWidth= 50;
    private int minHeight= 50;
    private final HeaderType _headerType;
    private RegChangeListen regChange= null;
    private HandlerRegistration resizeReg;
    private HandlerRegistration _preventEventRemove= null;
    private int _animationDuration =DROP_DOWN_TIME;

    private final String _anStyleName;


    public PopupPane(String header) { this(header, null); }

    public PopupPane(String header, Widget content) {
        this(header, content, PopupType.STANDARD,false,false);
    }

    public PopupPane(String header,
                     Widget content,
                     boolean modal,
                     boolean autoHide) {
        this(header,content, PopupType.STANDARD, modal, autoHide);
    }

    public PopupPane(String header,
                     Widget content,
                     PopupType ptype,
                     boolean modal,
                     boolean autoHide) {
        this(header,content, ptype, false, modal, autoHide, HeaderType.TOP);
    }

    public PopupPane(String header,
                     Widget content,
                     PopupType ptype,
                     boolean pointerPopup,
                     boolean modal,
                     boolean autoHide,
                     HeaderType headerType) {
        cnt++;
        this.header = header;
        _ptype= isStyleImplemented(ptype) ? ptype : PopupType.STANDARD;
        _pointerPopup= pointerPopup;
        _pointerIm= null;
        _headerType= headerType;
        _anStyleName = "ppDrop"+cnt;

        init(modal,autoHide, headerType);
        if (content!=null) setWidget(content);




        popup.setAnimationEnabled(true);
        _masking= modal;

        if (Application.getInstance().getDefZIndex()>0) {
            GwtUtil.setStyle(popup,"zIndex", Application.getInstance().getDefZIndex()+"");
        }

        if (_masking) {
            _maskPanel = new PopupPanel(false, false);
            _maskPanel.setAnimationEnabled(false);
            _maskPanel.setStyleName("firefly-mask-ui");
            addZIndexStyle(popup,"onTopDialog" );
            _maskPanel.setWidget(new Label()); // needed for style to take effect
        }
        else {
            _maskPanel = null;
        }

        popup.addCloseHandler(new CloseHandler<GwtPopupPanelFirefly>() {
            public void onClose(CloseEvent<GwtPopupPanelFirefly> ev) {
                CloseEvent.fire(PopupPane.this, PopupPane.this, ev.isAutoClosed());
            }
        });
    }

    /**
     * Don't use this method, there are betters ways, ask trey
     * Mostly this method exist to work around a firefox 2 on the mac bug
     * @param popup the popup
     * @param styleStr the style to use, must be on of the ones below
     */
    static void addZIndexStyle(PopupPanel popup, String styleStr) {
        if ((BrowserUtil.isBrowser(Browser.FIREFOX,2) ||
                BrowserUtil.isBrowser(Browser.SEAMONKEY,1)) &&
                BrowserUtil.isPlatform(Platform.MAC) ) {
            int zIndex= 100;
            if (styleStr.equals("onTopDialog"))            zIndex= 101;
            else if (styleStr.equals("onTopDialogModal"))  zIndex= 102;
            DOM.setIntStyleAttribute( popup.getElement(), "zIndex", zIndex);
        }
        else {
            popup.addStyleName(styleStr);
        }
    }

    static void addZIndexStyle(GwtPopupPanelFirefly popup, String styleStr) {
        if ((BrowserUtil.isBrowser(Browser.FIREFOX,2) ||
                BrowserUtil.isBrowser(Browser.SEAMONKEY,1)) &&
                BrowserUtil.isPlatform(Platform.MAC) ) {
            int zIndex= 100;
            if (styleStr.equals("onTopDialog"))            zIndex= 101;
            else if (styleStr.equals("onTopDialogModal"))  zIndex= 102;
            DOM.setIntStyleAttribute( popup.getElement(), "zIndex", zIndex);
        }
        else {
            popup.addStyleName(styleStr);
        }
    }

    /**
     * Set the content widget.  This is the widget that the popup will wrap around.
     * If the content widget implements RequiresResize then this will be a resizable popup.
     * @param content the widget that the popup will wrap around
     */
    public void setWidget(Widget content) {
        this.content.setWidget(content);
        if (content instanceof RequiresResize)  setResizableArea(content);
    }

    public Widget getWidget() {
        return this.content.getWidget();
    }

    /**
     * set the minimum width of the content (set by setWidget).
     * This value only applies if the content implements RequiresResize.
     * @param minWidth the minimum width of the content
     */
    public void setContentMinWidth(int minWidth) { this.minWidth= minWidth;  }

    /**
     * set the minimum height of the content (set by setWidget).
     * This value only applies if the content implements RequiresResize.
     * @param minHeight the minimum height of the content
     */
    public void setContentMinHeight(int minHeight) { this.minHeight= minHeight;  }


    public void setDefaultSize(int w, int h) {
        content.getWidget().setSize(w+"px", h+"px");
    }

    public void setHeader(String title) {
        setTitleLabel(title);
    }

    public void useHighestZIndexLevel() {
        popup.removeStyleName("onTopDialog");
        addZIndexStyle(popup, "onTopDialogModal");
    }

    public boolean isVisible() { return visible; }


    public void hide() {
        if (visible && !_hiding) {
            _hiding= true;
            onPreClose();
            hidePopup();
            onClose();
            _hiding= false;
        }
    }

    public void hidePopup() {
        if (timer!=null) timer.cancel();
        if (isVisible()) {
            hideInternalPopup();
            if (_maskPanel!=null) _maskPanel.hide();
            visible = false;
        }
        visible = false;

        clearRegionChangeListener();
        if (resizeReg!=null) resizeReg.removeHandler();
        regChange= null;
        resizeReg= null;

    }


    private void hideInternalPopup() {
        if (animationEnabled &&_rolldownCSSAnimation && visible && popup.getAnimateDown() && popup.isVisible()) {
            animationDropdownCSS(false);
        }
        else {
            popup.hide();
        }
    }

    public void setAnimationEnabled(boolean animation) {
        animationEnabled= animation;
        popup.setAnimationEnabled(!_rolldownCSSAnimation  && animation);
    }

    public void setAnimateDown(boolean animateDown) { popup.setAnimateDown(animateDown); }
    public boolean isAnimationEnabled() { return animationEnabled; }

    public void setRolldownAnimation(boolean rolldown) {
        if (rolldown && BrowserUtil.canSupportAnimation()) {
//        if (false) {
            popup.setAnimationEnabled(false);
            _rolldownCSSAnimation= true;
            if (BrowserUtil.canSupportAnimation()) initCssAnimationElement();

        }
        else {
            popup.setAnimationType(rolldown ? GwtPopupPanelFirefly.AnimationType.ROLL_DOWN : GwtPopupPanelFirefly.AnimationType.CENTER);
            _rolldownCSSAnimation= false;
        }

    }


    private void initCssAnimationElement() {
        if (_styleElement==null) {
            _styleElement= Document.get().createStyleElement();
            _styleElement.setInnerText("");
            _styleElement.setType("text/css");
            Document.get().getBody().appendChild(_styleElement);
        }
    }

    private void setupCssAnimation(String name, boolean show) {
        if (show) {
            popup.setVisible(false);
            popup.show();
            sizeHeader();
        }
        int height= popup.getOffsetHeight();
        int sy= -1*(height+10);
        int ey= popup.getPopupTop();
        if (!show) {
            ey= -1*(height+10);
            sy= popup.getPopupTop();
        }
        if (show) {
            popup.hide();
            popup.setVisible(true);
        }

        String css= "";
        if (BrowserUtil.isBrowser(Browser.SAFARI) || BrowserUtil.isBrowser(Browser.CHROME)) {
            css="@-webkit-keyframes "+name+" { \n" +
                    "0%   { top:"+sy+"px;} \n" +
                    "100% { top:"+ey+"px; } \n"+
                    "}";
        }
        else if (BrowserUtil.isBrowser(Browser.FIREFOX)) {
            css= "@-moz-keyframes "+name+" { \n" +
                    "0%   { top:"+sy+"px;} \n" +
                    "100% { top:"+ey+"px; } \n"+
                    "}";

        }
        else if (BrowserUtil.isBrowser(Browser.OPERA)) {
            css= "@-o-keyframes "+name+" { \n" +
                    "0%   { top:"+sy+"px;} \n" +
                    "100% { top:"+ey+"px; } \n"+
                    "}";
        }
        else {
            css= "@keyframes "+name+" { \n" +
                    "0%   { top:"+sy+"px;} \n" +
                    "100% { top:"+ey+"px; } \n"+
                    "}";
        }
        _styleElement.setInnerText(css);
    }


    private void animationDropdownCSS(final boolean show) {
        setAnimationStyle("none");
        Timer t= new Timer() {
            @Override
            public void run() {
                int mills= _animationDuration;
                if (!show) mills-=1;
                setupCssAnimation(_anStyleName, show);
                setAnimationStyle(_anStyleName + " " + (mills + 35) + "ms linear 1 normal");
                if (show) popup.show();
                Timer t= new Timer() {
                    @Override
                    public void run() {
                        if (!show && !visible) popup.hide();
                    }
                };
                t.schedule(_animationDuration+20);
            }
        };
        t.schedule( 10);
    }


    private void setAnimationStyle(String s) {
        CssAnimation.setAnimationStyle(popup, s);
    }

    public void setAnimationDurration(int durration) {
        _animationDuration = durration;
        popup.setAnimationDuration(durration);
    }

    public boolean isPopupShowing() {
        return popup.isShowing();
    }

    public Widget getPopupPanel() { return popup; }

    public void setCloseable(boolean closeable) {
        headerWidget.setVisible(closeable);
    }

    public boolean isCloseable() { return headerWidget.isVisible(); }

    public void alignToCenter() { alignTo(null, Align.CENTER, 0, 0); }


    public void alignTo(Widget widget, Align alignAt) {
        if (_pointerPopup) {
            alignTo(widget, alignAt, 0, 0);
        }
        else {
            alignTo(widget, alignAt, 3, 3);
        }
    }

    public void alignTo(Widget widget, Align alignAt, final int xOffset, final int yOffset) {
        alignWidget = widget;
        this.alignAt = alignAt;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        if (_masking && (widget!=null && _maskWidget==RootPanel.get()) ) {
            _maskWidget = GwtUtil.getTopParent(widget);
        }
    }

    public void setAutoAlign(boolean autoAlign) { _autoAlign = autoAlign; }


    public void setHideOnResizeWidget(Widget w) { _hideOnResizeWidget= w; }


    public void setResizableArea(Widget expandW) {
        if (_expandW==null && expandW!=null) popup.addStyleName("ExpandableDialogBox");
        _expandW= expandW;
    }

    protected void onPreClose() { /* do nothing - for overriding*/ }
    protected void onClose() { /* do nothing - for overriding*/ }


    private void setCursorShape(MouseMoveEvent ev) {
        int x= getAbsoluteX(ev);
        int y= getAbsoluteY(ev);
        PopupRegion r= findRegion(x,y);
        if (r==PopupRegion.SE_CORNER) {
            GwtUtil.setStyle(popup, "cursor", "se-resize");
        }
        else if (r==PopupRegion.SW_CORNER) {
            GwtUtil.setStyle(popup, "cursor", "sw-resize");
        }
        else {
            GwtUtil.setStyle(popup, "cursor", "auto");
        }
    }

    private void humanStart(HumanInputEvent ev) {
        int x= getAbsoluteX(ev);
        int y= getAbsoluteY(ev);
        PopupRegion r= findRegion(x,y);

        addPreventEvent();
        if (r== PopupRegion.TITLE_BAR) {
            if (_hideOnResizeWidget!=null) {
                GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "hidden");
            }
            ev.preventDefault();
            beginMove(x,y);

        } if (_expandW!=null) {
            if (r== PopupRegion.SE_CORNER || r== PopupRegion.SW_CORNER) {
                if (_hideOnResizeWidget!=null) {
                    GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "hidden");
                }
                ev.preventDefault();
                beginResize(r);
            }
        }
    }

    private void humanMove(HumanInputEvent ev) {
        int x= getAbsoluteX(ev);
        int y= getAbsoluteY(ev);
        if (_moving) {
//            DOM.eventPreventDefault(ev.getNativeEvent());
            ev.preventDefault();
            doMove(x,y);
        }
        else if (_resizing) {
//            DOM.eventPreventDefault(ev);
            ev.preventDefault();
            moveResize(x,y);
        }
    }

    private void humanStop(HumanInputEvent ev) {
        removePreventEvent();
        if (_moving) {
            if (_hideOnResizeWidget!=null) {
                GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "visible");
            }
            ev.preventDefault();
//            DOM.eventPreventDefault(ev);
            endMove();

        }
        else if (_resizing) {
            if (_hideOnResizeWidget!=null) {
                GwtUtil.setStyle(_hideOnResizeWidget, "visibility", "visible");
            }
            ev.preventDefault();
//            DOM.eventPreventDefault(ev);
            endResize();
        }
    }


    private int getAbsoluteX(HumanInputEvent ev) {
        if (ev instanceof MouseEvent) {
            return  ((MouseEvent)ev).getClientX()+Window.getScrollLeft();
        }
        if (ev instanceof TouchEvent) {
            JsArray<Touch> tAry= ((TouchEvent)ev).getTargetTouches();
            return  tAry.get(0).getClientX()+Window.getScrollLeft();
        }
        return 0;
    }


    private int getAbsoluteY(HumanInputEvent ev) {
        if (ev instanceof MouseEvent) {
            return  ((MouseEvent)ev).getClientY()+ Window.getScrollTop();
        }
        if (ev instanceof TouchEvent) {
            JsArray<Touch> tAry= ((TouchEvent)ev).getTargetTouches();
            return tAry.get(0).getClientY() + Window.getScrollTop();
        }
        return 0;
    }

    /**
     * resize the popup when the browser area is smaller then the popup size
     */
    private void adjustResizable() {
        if (_expandW!=null && optimalWidth>0 && optimalHeight>0) {
            int width= content.getOffsetWidth();
            int height= content.getOffsetHeight();

            if (width> Window.getClientWidth()) {
                width= Window.getClientWidth()-20;
            }
            else if (width<optimalWidth) {
                width= Math.min(optimalWidth, Window.getClientWidth()-20);
            }

            if (height> Window.getClientHeight()) {
                height= Window.getClientHeight()-35;
            }
            else if (height<optimalHeight) {
                height= Math.min(optimalHeight, Window.getClientHeight()-35);
            }

            if (width<minWidth) {
                width = minWidth;
                _doAlign= false; //turn off the automatic align, the dialog will jump around when users scrolls
            }
            if (height<minHeight) {
                height= minHeight;
                _doAlign= false; //turn off the automatic align, the dialog will jump around when users scrolls
            }

            if (height!=content.getOffsetHeight() || width!=content.getOffsetWidth()) {
                content.getWidget().setSize(width+"px", height+"px");
                ((RequiresResize)_expandW).onResize();
            }

        }
    }


    /**
     * Determine where the place the popup. This is a huge and central method to PopupPane - it needs come cleaning up
     * @param xoffset offset x added to alignment
     * @param yoffset offset y added to alignment
     */
    private void alignPopup(final int xoffset, final int yoffset) {
        int x, y;

        if (firstAlign) {
            optimalWidth= content.getOffsetWidth();
            optimalHeight= content.getOffsetHeight();
            DeferredCommand.addCommand(new Command() {
                public void execute() { adjustResizable(); }
            });
        }

        firstAlign = false;
        locateMask();

        PointerDir dir= PointerDir.NONE;

        if (alignAt==Align.DISABLE) {
            if (_pointerPopup) alignPointer(PointerDir.NORTH);
            return;
        }




        Widget alignWidget= this.alignWidget;
        if (alignWidget==null)  alignWidget= RootPanel.get();

        int popupHeight= popup.getOffsetHeight();
        int popupWidth= popup.getOffsetWidth();
        int rootHeight= RootPanel.get().getOffsetHeight();
        int rootWidth= RootPanel.get().getOffsetWidth();

        int alignX= alignWidget.getAbsoluteLeft();
        int alignY= alignWidget.getAbsoluteTop();
        int alignWidth= alignWidget.getOffsetWidth();
        int alignHeight= alignWidget.getOffsetHeight();


        if (alignAt==Align.BOTTOM_LEFT) {
            x = alignX+xoffset;
            y = alignY+alignHeight+yoffset;

        } else if (alignAt==Align.BOTTOM_RIGHT) {
            x = alignX+alignWidth+xoffset;
            y = alignY+alignHeight+yoffset;

        } else if (alignAt==Align.AUTO_POINTER) {
            dir= PointerDir.NORTH;
            definePointerDirection(dir);
            x = alignX+(alignWidth/2)- (popupWidth >> 1);
            if (_pointerPopup) x+= HOR_PTR_IMAGE_OFFSET;
            y = alignY+alignHeight+yoffset;
            if (y+popupHeight>Window.getClientHeight()) {
                dir= PointerDir.SOUTH_WEST;
                definePointerDirection(dir);
                x = alignX+alignWidth+xoffset;
                if (_pointerPopup) x+= HOR_PTR_IMAGE_OFFSET;
                y = (alignY-popupHeight+5)-yoffset;

            }
        } else if (alignAt==Align.BOTTOM_CENTER) {
            x = alignX+(alignWidth/2)- (popupWidth >> 1);
            if (_pointerPopup) x+= HOR_PTR_IMAGE_OFFSET;
            y = alignY+alignHeight+yoffset;
        } else if (alignAt==Align.VISIBLE_BOTTOM) { //todo
            rootHeight= Window.getClientHeight();
            x= Window.getScrollLeft()+xoffset;
            y= Window.getScrollTop()+Window.getClientHeight()-popupHeight +yoffset;

        } else if (alignAt==Align.BOTTOM_CENTER_POPUP_BOTTOM) {
            x = alignX+(alignWidth/2)- (popupWidth >> 1);
            if (_pointerPopup) x+= HOR_PTR_IMAGE_OFFSET;
            y = alignY+alignHeight -popupHeight +yoffset;

        } else if (alignAt==Align.TOP_CENTER) {
            x = alignX+(alignWidth/2)- (popupWidth >> 1);
            if (_pointerPopup) x+= HOR_PTR_IMAGE_OFFSET;
            y = (alignY-popupHeight)-yoffset;

        } else if (alignAt==Align.TOP_RIGHT) {
            x = alignX+alignWidth+xoffset-popupWidth;
            y = alignY+yoffset;

        } else if (alignAt==Align.TOP_RIGHT_POPUP_RIGHT) {
            x = alignX+alignWidth+xoffset;
            y = alignY+yoffset;

        } else if (alignAt==Align.CENTER) {
            if (alignWidget==null) {
                int left = (Window.getClientWidth() - popupWidth) >> 1;
                int top = (Window.getClientHeight() - popupHeight) >> 1;
                x= Window.getScrollLeft() + left;
                y= Window.getScrollTop() + top;
            }
            else {
                int width= content.getOffsetWidth();
                int height= content.getOffsetHeight();
                x = alignX + alignWidth/2;
                x = x - width/2 + xoffset;
                y = alignY + alignHeight/2;
                y = y - height/2 + yoffset;
            }
            if (y<0) y=10;
            if (x<0) x=10;
        } else if (alignAt==Align.TOP_LEFT_POPUP_RIGHT) {
            x = alignX+xoffset-popupWidth;
            if (x<0) x= 0;
            y = alignY+yoffset;
        } else if (alignAt==Align.TOP_LEFT_POPUP_BOTTOM) {
            x = alignX+xoffset;
            if (x<0) x= 0;
            y = alignY+yoffset-popupHeight;
        } else if (alignAt== Align.TOP_RIGHT_OR_LEFT){
            if(Window.getClientWidth() < (alignWidth+popupWidth)){
                x = Window.getClientWidth() - ((xoffset+35) + popupWidth);
                if (x<0) x= 0;
                y = alignY+yoffset;
            } else {
                x = alignX+alignWidth+xoffset;
                y = alignY+yoffset;
            }

        } else {

            // default to TOP_LEFT
            x = 1+xoffset;
            y = 1+yoffset;
        }


        //----
        // Now adjust the popup to fit in the viewable area
        //----


        if (x+popupWidth > rootWidth) {
            x= x - ((x+popupWidth) - rootWidth); // move if over by the amount it is off the side
        }

        if (y+popupHeight > rootHeight && rootHeight>10) { //if bottom of dialog is off the bottom, if root height is near 0 then don't adjust
            y= y - ((y+popupHeight) - rootHeight); // move if up by the amount it is off the bottom
        }

        if (y<Window.getScrollTop()) y=Window.getScrollTop()+1;   // if it is off the top set it to the top +1
        if (x<Window.getScrollLeft()) x=Window.getScrollLeft()+1;// if it is off the left set it to the left +1





        if (_doAlign) _doAlign= _autoAlign;



        alignPointer(dir);

        if (popup.isVisible()) {
            if (popup.getAbsoluteLeft() != x ||
                popup.getAbsoluteTop() != y) {
                popup.setPopupPosition(x, y);
            }
        }

    }


    public void setPopupPosition(int xPos, int yPos) {
        _reqPosX= xPos;
        _reqPosY= yPos;
        popup.setPopupPosition(xPos, yPos);
    }

    public void show(int autoCloseSecs) {
        show();
        if (autoCloseSecs>0) {
            Timer t= new Timer() {
                public void run() { hide();}
            };
            t.schedule(autoCloseSecs * 1200);
        }

    }

    public void show() {
        showInternalPopup();
        _doAlign= true;
        if (timer == null) {
            timer = new Timer(){
                public void run() {
                    if (!canAlign()) {
                        hidePopup();
                    } else if (isVisible() && _doAlign) {
                        alignPopup(xOffset, yOffset);
                        if (alignAt!=Align.DISABLE)  schedule(500);
                    }
                }
            };
        }
        timer.run();


        initRegionChangeListener();
        if (resizeReg==null) {
            resizeReg= Window.addResizeHandler(new PopupResize());
        }


    }

    public void internalResized() {
        sizeHeader();
    }

    private void sizeHeader() {
        if (_headerType==HeaderType.SIDE  ) { // the following is a total hack, but I can't make it work on webkit
            DeferredCommand.addCommand(new Command() {
                public void execute() {
//                    String computedHeight= GwtUtil.getComputedStyle(headerWidget.getParent().getElement(), "height");
                    String computedHeight= GwtUtil.getComputedStyle(content.getElement(), "height");
                    if (!StringUtils.isEmpty(computedHeight)) {
                        headerWidget.setHeight(computedHeight);
                    }
                }
            });
        }
    }

    private void showInternalPopup() {
        if (animationEnabled &&_rolldownCSSAnimation && !visible) {
            visible= true;
            animationDropdownCSS(true);
        }
        else {
            popup.show();
            visible= true;
            sizeHeader();
        }
    }

    public void resetAutoAlign() {
        _doAlign= true;
        if (visible) show();
    }



    public void showOrHide() {
        if (visible) hide();
        else show();
    }

    public void setDoRegionChangeHide(boolean doRegionChangeHide) {
        this.doRegionChangeHide= doRegionChangeHide;
        if (doRegionChangeHide) {
            initRegionChangeListener();
        }
        else {
            clearRegionChangeListener();
        }
    }

    public boolean isDoRegionChangeHide() {  return doRegionChangeHide; }

    private void initRegionChangeListener() {
        if (visible && regChange==null && doRegionChangeHide) {
            regChange= new RegChangeListen();
            WebEventManager.getAppEvManager().addListener(Name.REGION_CHANGE, regChange);
        }
    }

    private void clearRegionChangeListener() {
        if (regChange!=null) WebEventManager.getAppEvManager().removeListener(Name.REGION_CHANGE, regChange);
    }

    /**
     * Enable/disable locating the dialog on browser or dialog resize
     * Must be called after visible or will have no effect
     * @param autoLocate enable/disable auto locating
     */
    public void setAutoLocate(boolean autoLocate) {
        if (visible) _doAlign= autoLocate;
    }

    private boolean canAlign() {
        return (alignAt==Align.DISABLE ||
               (alignWidget==null && alignAt==Align.CENTER) ||
               (alignWidget==null && alignAt==Align.VISIBLE_BOTTOM) ||
               (alignWidget != null &&
               (alignWidget.getOffsetHeight() + alignWidget.getOffsetWidth() > 0)));
    }

    public HandlerRegistration addCloseHandler(CloseHandler<PopupPane> h) {
        return hMan.addHandler(CloseEvent.getType(),h);
    }

    public void fireEvent(GwtEvent<?> ev) { hMan.fireEvent(ev); }

    private void init(boolean modal,
                     boolean autoHide,
                     HeaderType headerType) {

        content = new SimplePanel();
        if (headerType==HeaderType.TOP) {
            _mainPanel =  new VerticalPanel();
            headerWidget = makeTopHeader(header);
            _mainPanel.add(headerWidget);
            Label sep = new Label();
            sep.setHeight("2px");
            _mainPanel.add(sep);
        }
        else if (headerType==HeaderType.SIDE) {
            HorizontalPanel hp = new HorizontalPanel();
            headerWidget = makeSideHeader();
            hp.add(headerWidget);
            hp.setCellHeight(headerWidget, "100%");
            hp.addStyleName("tmp1-style");
            Label sep = new Label();
            sep.setWidth("2px");
            hp.add(sep);
            _mainPanel = hp;
        }
        else {
            _mainPanel =  new VerticalPanel();
        }
        _mainPanel.add(content);
        _mainPanel.addStyleName("main-panel");
        _mainPanel.addStyleName(_ffCss.popupBackground());
        _mainPanel.addStyleName(_ffCss.standardBorder());

        popup = createPopup(modal,autoHide);
        definePopupStyle(modal);
    }


    public static boolean isMaskable() { return true; }

    private void definePointerDirection(PointerDir dir) {
        if (_currPtDir!=dir) {
            Panel exteriorPanel= new HorizontalPanel();
            DOM.setStyleAttribute(_mainPanel.getElement(), "padding", "3px");
            switch (dir) {
                case NORTH:

                    String topOff= "-4px";
                    if (BrowserUtil.isBrowser(Browser.FIREFOX)) topOff= "-5px";
                    DOM.setStyleAttribute(_mainPanel.getElement(), "marginTop", topOff);
                    exteriorPanel= new VerticalPanel();
                    _pointerIm= new Image(UP_POPUP_POINTER);
                    exteriorPanel.add(_pointerIm);
                    exteriorPanel.add(_decoratorPanel);
                    break;
                case SOUTH_WEST:
                    String leftOff= "-2px";
                    if (BrowserUtil.isBrowser(Browser.FIREFOX))leftOff= "-1px";
                    DOM.setStyleAttribute(_mainPanel.getElement(), "marginLeft", leftOff);
                    exteriorPanel= new HorizontalPanel();
                    _pointerIm= new Image(LEFT_DOWN_POPUP_POINTER);
                    exteriorPanel.add(_pointerIm);
                    exteriorPanel.add(_decoratorPanel);
                    break;
                default :
                    assert false;
                    break;
            }
            if (dir!=PointerDir.NONE) {
                _decoratorPanel.setStylePrimaryName("firefly-popup-pointer");
                popup.setWidget(exteriorPanel);
            }
        }
    }


    private void definePopupStyle(boolean modal) {
        if (_ptype==PopupType.STANDARD) {
            popup.setStyleName("NO-STYLE");
            _decoratorPanel.setWidget(_mainPanel);
            _mainPanel.addStyleName("vertical-panel");
            if (_pointerPopup) {
                definePointerDirection(PointerDir.NORTH);
                _decoratorPanel.setStylePrimaryName("firefly-popup-pointer");
            }
            else {
                popup.setWidget(_decoratorPanel);
                _mainPanel.setStylePrimaryName("firefly-popup");
                if (BrowserUtil.getSupportsShadows()) {
                    _decoratorPanel.setStylePrimaryName("shadow");
                }
                else {
                    _decoratorPanel.setStylePrimaryName("firefly-popup-normal");
                }
            }
        }
        else if (_ptype==PopupType.LOW_PROFILE) {
            popup.setStyleName("shadow");
            popup.setWidget(_mainPanel);
            _mainPanel.setStylePrimaryName("firefly-popup");
            _mainPanel.addStyleName("vertical-panel");
        }
        else if (_ptype==PopupType.NONE) {
            popup.setWidget(_mainPanel);
            _mainPanel.setStylePrimaryName("firefly-popup");
        }
        popup.addStyleName(_ffCss.globalSettings());

        addZIndexStyle(popup, modal ? "onTopDialogModal" : "onTopDialog");
    }


    protected Widget makeCloseButton(boolean onTop) {
        Image close = new Image(GWT.getModuleBaseURL() +"images/blue_delete_10x10.gif"){
                        {this.addClickHandler(new ClickHandler(){
                            public void onClick(ClickEvent ev) {
                                onCloseButtonClick(ev);
                                hide();
                            }
                        });}
                    };
        close.setStyleName("popup-header");
        if (!onTop) GwtUtil.setStyle(close, "margin", "0 0 0 2px");
        return close;
    }

    protected void onCloseButtonClick(ClickEvent ev) {}

    protected Widget makeTopHeader(String title) {
        setTitleLabel(title);
        titleLabel.setWidth("100%");
        DOM.setStyleAttribute(titleLabel.getElement(),"cursor", "default");

        Widget close = makeCloseButton(true);

        DockLayoutPanel header = new DockLayoutPanel(Style.Unit.PX);

        header.setStyleName("title-bar");
        header.addStyleName(_ffCss.titleBgColor());
        header.addStyleName(_ffCss.titleColor());
        titleLabel.addStyleName("title-label");

        header.addEast(close, 12);
        header.add(titleLabel);
        header.setHeight("14px");
        header.setWidth("100%");
        return header;
    }


    protected Widget makeSideHeader() {

        Widget close = makeCloseButton(false);

        SimplePanel panel= new SimplePanel();
        panel.addStyleName(_ffCss.titleBgColor());
        panel.addStyleName(_ffCss.titleColor());
        panel.setHeight("100%");


        VerticalPanel header = new VerticalPanel();
        titleLabel.addStyleName("title-label");
        titleLabel.setHeight("100%");
        DOM.setStyleAttribute(titleLabel.getElement(), "cursor", "default");

        GwtUtil.setStyle(header, "paddingTop", "3px");

        titleLabel.setHTML("&nbsp;");

        header.addStyleName(_ffCss.titleBgColor());
        header.addStyleName(_ffCss.titleColor());

        header.add(close);
        header.add(titleLabel);
        header.setCellHeight(titleLabel, "100%");
        header.setWidth("15px");
        header.setHeight("100%");

        panel.setWidget(header);
        return panel;
    }

//    protected Widget makeSideHeader_DOCK_LAYOUT() {
//
//        Widget close = makeCloseButton(false);
//
//        DockLayoutPanel header = new DockLayoutPanel(Style.Unit.PX);
//        titleLabel.addStyleName("title-label");
//        titleLabel.setHeight("100%");
//        DOM.setStyleAttribute(titleLabel.getElement(),"cursor", "default");
//
//        header.setStyleName("title-bar");
//        header.addStyleName(_ffCss.titleBgColor());
//        header.addStyleName(_ffCss.titleColor());
//
//        header.addNorth(close, 12);
//        header.add(titleLabel);
//        header.setWidth("14px");
//        header.setHeight("100%");
//        return header;
//    }

    protected void setTitleLabel(String title) {
        if (_headerType==HeaderType.TOP) {
            title = title == null ? "" : title;
            titleLabel.setHTML(title);
            GwtUtil.setStyle(titleLabel,"padding", "2px 0 0 3px");
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void beginMove(int x, int y) {
        _moving= true;
        _doAlign= false;
        DOM.releaseCapture(popup.getElement());
        DOM.setCapture(popup.getElement());
        _originalX= popup.getAbsoluteLeft();
        _originalY= popup.getAbsoluteTop();
        _originalMouseX= x;
        _originalMouseY= y;
    }


    private void doMove(int x, int y) {
        if (x<0) x= 0;
        if (y<0) y= 0;
        if (_moving) {
            int xDiff= x-_originalMouseX;
            int yDiff= y-_originalMouseY;
            int newX=_originalX+xDiff;
            int newY=_originalY+yDiff;
            setPopupPosition(newX,newY);
        }
    }

    private void endMove() {
        if (_moving) {
            DOM.releaseCapture(popup.getElement());
            _moving= false;
        }
    }


    private void beginResize(PopupRegion resizeCorner) {
        if (_expandW!=null) {
            DOM.releaseCapture(popup.getElement());
            DOM.setCapture(popup.getElement());
            _resizeCorner= resizeCorner;
            _resizing= true;
            _doAlign= false;

            Element e= _expandW.getElement();
            _xDiff= popup.getOffsetWidth()- e.getOffsetWidth();
            _yDiff= popup.getOffsetHeight()- e.getOffsetHeight();
            _originalX= popup.getAbsoluteLeft();
            _originalY= popup.getAbsoluteTop();

        }
    }


    private void moveResize(int x, int y) {
        if (_expandW!=null && _resizing) {
            int newW;
            int newH;
            Element e= _expandW.getElement();
            int beginWidgetH= e.getOffsetHeight();
            int beginWidgetW= e.getOffsetWidth();
            int beginW= popup.getOffsetWidth();

            if (_resizeCorner== PopupRegion.SE_CORNER) {
                newW= x- popup.getAbsoluteLeft() - _xDiff;
                newH= y- popup.getAbsoluteTop() - _yDiff;

                if (newW<minWidth) newW= beginWidgetW;
                if (newH<minHeight) newH= beginWidgetH;

                performResize(_expandW, newW,newH);
            }
            else if (_resizeCorner== PopupRegion.SW_CORNER) {
                newW= e.getOffsetWidth() + (popup.getAbsoluteLeft() - x);
                newH= y- popup.getAbsoluteTop() - _yDiff;

                if (newW<minWidth) newW= beginWidgetW;
                if (newH<minHeight) newH= beginWidgetH;

                performResize(_expandW, newW,newH);

                if (beginW!=popup.getOffsetWidth()) setPopupPosition(x, _originalY);

          }


        }

    }


    private void performResize(Widget w, int width, int height) {
        if (w instanceof RequiresResize) {
            w.setPixelSize(width,height);
            ((RequiresResize)w).onResize();
        }
        else {
            WebAssert.argTst(false, "This should not happen, w must be an instance of RequiresResize");
        }

    }



    private void endResize() {
        if (_expandW!=null) {
            DOM.releaseCapture(popup.getElement());
            if (_expandW!=null && _resizing && (_resizeCorner== PopupRegion.SE_CORNER || _resizeCorner== PopupRegion.SW_CORNER)) {
                _resizeCorner= PopupRegion.NONE;
                _resizing= false;

            }
        }
    }




    private PopupRegion findRegion(int mx, int my) {

        PopupRegion retval= PopupRegion.NONE;

        int x= popup.getAbsoluteLeft();
        int y= popup.getAbsoluteTop();
        int w= popup.getOffsetWidth();
        int h= popup.getOffsetHeight();


        CLoc nwCorner= new CLoc(x,y,true,true);
        CLoc neCorner= new CLoc(x+w,y, false, true);
        CLoc swCorner= new CLoc(x,y+h, true, false);
        CLoc seCorner= new CLoc(x+w,y+h, false, false);


        if (neCorner.inRange(mx,my))      retval= PopupRegion.NE_CORNER;
        else if (nwCorner.inRange(mx,my)) retval= PopupRegion.NW_CORNER;
        else if (seCorner.inRange(mx,my)) retval= PopupRegion.SE_CORNER;
        else if (swCorner.inRange(mx,my)) retval= PopupRegion.SW_CORNER;

        if (retval== PopupRegion.NONE) { // look for the title bar
            w= titleLabel.getOffsetWidth();
            h= titleLabel.getOffsetHeight();
            x= titleLabel.getAbsoluteLeft();
            y= titleLabel.getAbsoluteTop();
            if (mx>=x && mx<=x+w  && my>=y && my<=y+h) {
                retval= PopupRegion.TITLE_BAR;
            }

        }

        return retval;
    }


    private GwtPopupPanelFirefly createPopup(boolean modal, boolean autoHide) {


        GwtPopupPanelFirefly pp =  new GwtPopupPanelFirefly(autoHide, modal);


        pp.addDomHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent ev) {
                setCursorShape(ev);
            }
        }, MouseMoveEvent.getType());


        pp.addDomHandler(new MouseDownHandler() {
            public void onMouseDown(MouseDownEvent ev) { humanStart(ev); }
        }, MouseDownEvent.getType());

        pp.addDomHandler(new MouseUpHandler() {
            public void onMouseUp(MouseUpEvent ev) { humanStop(ev); }
        }, MouseUpEvent.getType());

        pp.addDomHandler(new MouseMoveHandler() {
            public void onMouseMove(MouseMoveEvent ev) { humanMove(ev); }
        }, MouseMoveEvent.getType());

        pp.addDomHandler(new TouchStartHandler() {
            public void onTouchStart(TouchStartEvent ev) {
                if (ev.getTargetTouches().length()==1) humanStart(ev);
            }
        }, TouchStartEvent.getType());

        pp.addDomHandler(new TouchMoveHandler() {
            public void onTouchMove(TouchMoveEvent ev) {
                humanMove(ev);
            }
        }, TouchMoveEvent.getType());

        pp.addDomHandler(new TouchEndHandler() {
            public void onTouchEnd(TouchEndEvent ev) {
                humanStop(ev);
            }
        }, TouchEndEvent.getType());

        return pp;
    }


    private void alignPointer(PointerDir dir) {
        if (_pointerPopup && dir!=PointerDir.NONE) {


            switch (dir) {
                case NORTH:
                    int imWidth=  (_pointerIm.getWidth()==0) ? 1 : _pointerIm.getWidth();
                    DOM.setStyleAttribute(_pointerIm.getElement(), "paddingLeft", (popup.getOffsetWidth()/2- imWidth/2)+"px");
                    break;
                case SOUTH_WEST:
                    int imHeight=  (_pointerIm.getHeight()==0) ? 1 : _pointerIm.getHeight();
                    DOM.setStyleAttribute(_pointerIm.getElement(), "paddingTop", (_mainPanel.getOffsetHeight()- (imHeight) )+"px");
                    break;
            }

        }
    }

    private void locateMask() {
        if (!_masking || popup==null || !isMaskable()) {
            return;
        }
        final int w= _maskWidget.getOffsetWidth();
        final int h= _maskWidget.getOffsetHeight();
        int left= _maskWidget.getAbsoluteLeft();
        int top= _maskWidget.getAbsoluteTop();
        if (w>0 || h>0 && (left>0 || top>0) ) {
            _maskPanel.setPopupPosition(_maskWidget.getAbsoluteLeft(),
                                        _maskWidget.getAbsoluteTop());
            _maskPanel.setWidth(w+"px");
            _maskPanel.setHeight(h+"px");
            _maskPanel.show();
        }
    }





    private boolean isStyleImplemented(PopupType pType) {
        boolean retval=  true;
        if (pType==PopupType.LOW_PROFILE && !BrowserUtil.getSupportsCSS3()) {
            retval= false;
        }
        return retval;

    }

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

    private static class CLoc {
        private final int _x;
        private final int _y;
        private final boolean _xIncrease;
        private final boolean _yIncrease;


        public CLoc(int x, int y, boolean xIncrease, boolean yIncrease ) {
            _x= x;
            _y= y;
            _xIncrease= xIncrease;
            _yIncrease= yIncrease;
        }

        public boolean inRange(int mx, int my) {
            boolean inX;
            boolean inY;

            if (_xIncrease) inX= (mx<=_x+MARGIN && mx>=_x);
            else            inX= (mx>=_x-MARGIN && mx<=_x);

            if (_yIncrease) inY= (my<=_y+MARGIN && my>=_y);
            else            inY= (my>=_y-MARGIN && my<=_y);

            return inX && inY;
        }
    }


    public class RegChangeListen implements WebEventListener {
            public void eventNotify(WebEvent ev) { hide(); }
    }

    public class PopupResize implements ResizeHandler {
        public void onResize(ResizeEvent event) {
            if (_expandW!=null) {
                adjustResizable();
            }
        }
    }

    void addPreventEvent() {
        if (_preventEventRemove==null) {
            _preventEventRemove= Event.addNativePreviewHandler(new PreventEventPreview());
        }
    }


    void removePreventEvent() {
        if (_preventEventRemove!=null) {
            _preventEventRemove.removeHandler();
            _preventEventRemove= null;
        }
    }

    private static class PreventEventPreview implements Event.NativePreviewHandler {
        public void onPreviewNativeEvent(Event.NativePreviewEvent ev) {
            switch (ev.getTypeInt()) {
                case Event.ONMOUSEMOVE:
                case Event.ONMOUSEDOWN:
                case Event.ONTOUCHSTART:
                case Event.ONTOUCHMOVE:
                case Event.ONTOUCHEND:
                    ev.getNativeEvent().preventDefault();
                    break;
            }
        }
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
