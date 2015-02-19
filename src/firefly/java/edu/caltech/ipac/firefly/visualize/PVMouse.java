/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 7/18/11
 * Time: 11:46 AM
 */


import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusPanel;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
* @author Trey Roby
*/
class PVMouse implements MouseDownHandler,
                         MouseUpHandler,
                         MouseMoveHandler,
                         MouseOverHandler,
                         MouseOutHandler,
                         TouchStartHandler,
                         TouchMoveHandler,
                         TouchEndHandler,
                         ClickHandler {

    private static final boolean DEBUG = false;
    private boolean _mouseDown= false;
    private WebPlotView _webPlotView;
    private HandlerRegistration _preventEventRemove= null;
    private Stack<WebPlotView.MouseInfo> _exclusiveMouse= new Stack<WebPlotView.MouseInfo>();
    private List<WebPlotView.MouseInfo> _persistentMouse= new ArrayList<WebPlotView.MouseInfo>(3);
    private FocusPanel _mouseMoveArea;

    PVMouse(WebPlotView webPlotView,
            FocusPanel mouseMoveArea) {
        _webPlotView = webPlotView;
        _mouseMoveArea= mouseMoveArea;

        _mouseMoveArea.addMouseDownHandler(this);
        _mouseMoveArea.addMouseUpHandler(this);
        _mouseMoveArea.addMouseMoveHandler(this);
        _mouseMoveArea.addMouseOverHandler(this);
        _mouseMoveArea.addMouseOutHandler(this);
        _mouseMoveArea.addClickHandler(this);
        _mouseMoveArea.addDomHandler(this, TouchStartEvent.getType());
        _mouseMoveArea.addDomHandler(this, TouchMoveEvent.getType());
        _mouseMoveArea.addDomHandler(this, TouchEndEvent.getType());
    }




    public void onMouseOut(MouseOutEvent ev) {
        if (!_mouseDown) {
            removePreventEvent();
            DOM.releaseCapture(_mouseMoveArea.getElement());
        }
        boolean enabledOthers= true;
        if (_exclusiveMouse.size()>0) {
//              MouseInfo mi= _exclusiveMouse.peek();
            WebPlotView.MouseInfo mi;
            boolean enabledExclusive= true;
            int len= _exclusiveMouse.size();
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onMouseOut(_webPlotView);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }
        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onMouseOut(_webPlotView);
            }
        }
    }
    public void onMouseMove(MouseMoveEvent ev) {
        if (DEBUG) {
            GwtUtil.showDebugMsg("mouse move:" +
                                         " - xy: " +ev.getX()+","+ev.getY() +
                                         " -  c: " +ev.getClientX()+","+ev.getClientY() +
                                         " -  p: " +ev.getScreenX()+","+ev.getScreenY() +
                                         " -  r: " +ev.getRelativeX(_mouseMoveArea.getElement())+","+ev.getRelativeY(_mouseMoveArea.getElement()));
        }
        onMouseMove(makeScreenPt(ev));
    }

    public void onMouseMove(ScreenPt spt) {
        boolean enabledOthers= true;

        if (_exclusiveMouse.size()>0) {
//              MouseInfo mi= _exclusiveMouse.peek();
            WebPlotView.MouseInfo mi;

            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onMouseMove(_webPlotView, spt);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }


        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onMouseMove(_webPlotView, spt);
            }
        }
    }

    public void onMouseUp(MouseUpEvent ev) {
        ScreenPt spt= makeScreenPt(ev);
        removePreventEvent();
        DOM.releaseCapture(_mouseMoveArea.getElement());
        boolean enabledOthers= true;
        if (_exclusiveMouse.size()>0) {
//              MouseInfo mi= _exclusiveMouse.peek();

            WebPlotView.MouseInfo mi;

            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onMouseUp(_webPlotView, spt);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }



        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onMouseUp(_webPlotView, spt);
            }
        }
        _mouseDown= false;
        _webPlotView.disableTextSelect(false);
    }

    public void onClick(ClickEvent ev) {
        ScreenPt spt= makeScreenPt(ev);
        ev.preventDefault();
        _webPlotView.enableFocus();
        boolean enabledOthers= true;

        if (_exclusiveMouse.size()>0) {

            WebPlotView.MouseInfo mi;
            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onClick (_webPlotView, spt);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }



        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onClick(_webPlotView, spt);
            }
        }
//        _webPlotView.disableTextSelect(true);
        _webPlotView.fixScrollPosition();
    }

    public void onMouseDown(MouseDownEvent ev) {
        ScreenPt spt= makeScreenPt(ev);
        addPreventEvent();
        _webPlotView.enableFocus();
        DOM.releaseCapture(_mouseMoveArea.getElement());
        DOM.setCapture(_mouseMoveArea.getElement());
        boolean enabledOthers= true;
        if (_exclusiveMouse.size()>0) {

            WebPlotView.MouseInfo mi;
            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onMouseDown(_webPlotView, spt, ev);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }



        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onMouseDown(_webPlotView, spt, ev);
            }
        }
        _mouseDown= true;
        _webPlotView.disableTextSelect(true);
        _webPlotView.fixScrollPosition();
    }

    public void onMouseOver(MouseOverEvent ev) {
        ScreenPt spt= makeScreenPt(ev);
        boolean enabledOthers= true;
        if (_exclusiveMouse.size()>0) {

            WebPlotView.MouseInfo mi;
            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onMouseOver(_webPlotView, spt);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }


        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onMouseOver(_webPlotView, spt);
            }
        }
        _webPlotView.fixScrollPosition();
    }

    public void onTouchStart(TouchStartEvent ev) {
//        if (ev.getTouches().length()>1) return;
        boolean exclusive= _exclusiveMouse.size()>0;
        Touch t= ev.getTargetTouches().get(0);
        if (DEBUG) {
            GwtUtil.showDebugMsg("start, touches:" + ev.getTargetTouches().length() +
                                         " -  c: " +t.getClientX()+","+t.getClientY() +
                                         " -  p: " +t.getPageX()+","+t.getPageY() +
                                         " -  s: " +t.getScreenX()+","+t.getScreenY() +
                                         " -  r: " +t.getRelativeX(_mouseMoveArea.getElement())+","+t.getRelativeY(_mouseMoveArea.getElement())+
                                         " - pos: " +_mouseMoveArea.getAbsoluteLeft()+","+_mouseMoveArea.getAbsoluteTop()+"," +
                                         " - scroll: " +_mouseMoveArea.getElement().getScrollLeft()+","+
                                         _mouseMoveArea.getElement().getScrollTop()+"," +
                                         " - absScroll: " +_mouseMoveArea.getElement().getOwnerDocument().getScrollLeft()+","+
                                         _mouseMoveArea.getElement().getOwnerDocument().getScrollTop());
        }
        ScreenPt spt= makeScreenPt(ev);
        if (!exclusive) addPreventEvent();
        _webPlotView.enableFocus();
        if (!exclusive) {
            DOM.releaseCapture(_mouseMoveArea.getElement());
            DOM.setCapture(_mouseMoveArea.getElement());
        }
        boolean enabledOthers= true;
        if (exclusive) {
            WebPlotView.MouseInfo mi;
            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onTouchStart(_webPlotView, spt, ev);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }
        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onTouchStart(_webPlotView, spt, ev);
            }
        }
        _mouseDown= true;
        _webPlotView.disableTextSelect(true);
        _webPlotView.fixScrollPosition();
        ev.preventDefault();
    }

    public void onTouchMove(TouchMoveEvent ev) {
//        if (ev.getTouches().length()>1) return;
        Touch t= ev.getTargetTouches().get(0);
        if (DEBUG) {
            GwtUtil.showDebugMsg("move, touches:" + ev.getTargetTouches().length() +
                                         " -  c: " +t.getClientX()+","+t.getClientY() +
                                         " -  p: " +t.getPageX()+","+t.getPageY() +
                                         " -  s: " +t.getScreenX()+","+t.getScreenY() +
                                         " -  r: " +t.getRelativeX(_mouseMoveArea.getElement())+","+t.getRelativeY(_mouseMoveArea.getElement())+
                                         " - pos: " +_mouseMoveArea.getAbsoluteLeft()+","+_mouseMoveArea.getAbsoluteTop()+"," +
                                         " - scroll: " +_mouseMoveArea.getElement().getScrollLeft()+","+
                                         _mouseMoveArea.getElement().getScrollTop()+"," +
                                         " - absScroll: " +_mouseMoveArea.getElement().getOwnerDocument().getScrollLeft()+","+
                                         _mouseMoveArea.getElement().getOwnerDocument().getScrollTop());
        }
        ScreenPt spt= makeScreenPt(ev);
        boolean enabledOthers= true;
        if (_exclusiveMouse.size()>0) {
//              MouseInfo mi= _exclusiveMouse.peek();
            WebPlotView.MouseInfo mi;

            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onTouchMove(_webPlotView, spt, ev);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }


        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onTouchMove(_webPlotView, spt, ev);
            }
        }
        ev.preventDefault();
    }

    public void onTouchEnd(TouchEndEvent ev) {
//        if (ev.getTouches().length()>1) return;
        removePreventEvent();
        DOM.releaseCapture(_mouseMoveArea.getElement());
        boolean enabledOthers= true;
        if (_exclusiveMouse.size()>0) {
//              MouseInfo mi= _exclusiveMouse.peek();

            WebPlotView.MouseInfo mi;

            int len= _exclusiveMouse.size();
            boolean enabledExclusive= true;
            for(int i= 1; ((len-i)>=0 && enabledExclusive); i++) {
                mi= _exclusiveMouse.get(len-i);
                if (mi.isEnabled()) mi.getHandler().onTouchEnd(_webPlotView);
                enabledExclusive= mi.getEnableAllExclusive();
                enabledOthers= mi.getEnableAllPersistent();
            }



        }
        if (enabledOthers) {
            for(WebPlotView.MouseInfo info : _persistentMouse) {
                if (info.isEnabled()) info.getHandler().onTouchEnd(_webPlotView);
            }
        }
        _mouseDown= false;
        _webPlotView.disableTextSelect(false);
        ev.preventDefault();
    }



    void addPersistentMouseInfo(WebPlotView.MouseInfo info) { _persistentMouse.add(info); }
    void removePersistentMouseInfo(WebPlotView.MouseInfo info) {
        if (_persistentMouse.contains(info)) _persistentMouse.remove(info);
    }
    void grabMouse(WebPlotView.MouseInfo info) { _exclusiveMouse.push(info); }
    void releaseMouse(WebPlotView.MouseInfo info) {
        if (_exclusiveMouse!=null && _exclusiveMouse.contains(info)) _exclusiveMouse.remove(info);
    }



    void freeResources() {
        if (_exclusiveMouse!=null) _exclusiveMouse.clear();
        if (_persistentMouse!=null) _persistentMouse.clear();
    }

    private ScreenPt makeScreenPt(MouseEvent ev) {
        return new ScreenPt(ev.getX(),ev.getY());
    }
    private ScreenPt makeScreenPt(TouchEvent ev) {
        Touch t= (Touch)ev.getTouches().get(0);
        return new ScreenPt(t.getClientX() - _mouseMoveArea.getAbsoluteLeft(), t.getClientY()- _mouseMoveArea.getAbsoluteTop());
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
//            GwtUtil.showDebugMsg((cnt++)+", " +DOM.eventGetTypeString(ev));
            switch (ev.getTypeInt()) {
                case Event.ONMOUSEMOVE:
                case Event.ONMOUSEDOWN:
//                    DOM.eventPreventDefault(ev.getNativeEvent());
//                    ev.cancel();
                    ev.getNativeEvent().preventDefault();
                    break;
            }
        }
    }
}

