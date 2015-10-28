/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.ui.GwtUtil;

import java.util.logging.Level;


/**
 * User: roby
 * Date: May 5, 2009
 * Time: 1:36:05 PM
 */


/**
 * @author Trey Roby
 */
public class PlotMover {

    private static final String MOUSE_HELP= "Click and move to move plot";
    private final WebPlotView _pv;
    private boolean _mouseDown= false;
    private int _originalMouseX;
    private int _originalMouseY;
    private int _originalScrollX;
    private int _originalScrollY;
    private int _lastX;
    private int _lastY;
    private DragTimer dragTimer= new DragTimer();

    WebPlotView.MouseInfo _mouseInfo= new WebPlotView.MouseInfo(
                                  new Mouse(), MOUSE_HELP);
    private final Enabler _enabler= new Enabler();

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public PlotMover(WebPlotView pv) {
        _pv= pv;
        _pv.grabMouse(_mouseInfo);
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================



//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void begin(ScreenPt spt, MouseDownEvent ev) {
//        _originalMouseX = convertToAbsoluteX(spt.getIX());
//        _originalMouseY = convertToAbsoluteY(spt.getIY());

        _originalMouseX = ev.getScreenX();
        _originalMouseY = ev.getScreenY();

        ScreenPt pt= _pv.getScrollScreenPos();

        _originalScrollX = pt.getIX();
        _originalScrollY = pt.getIY();
        _lastX= _originalMouseX;
        _lastY= _originalMouseY;
    }

    static int cnt= 0;

    private void drag(ScreenPt spt, int x, int y, boolean endDrag) {
//        int x = convertToAbsoluteX(spt.getIX());
//        int y = convertToAbsoluteY(spt.getIY());
//        int x = _lastX;
//        int y = _lastY;
//        if (ev!=null) {
//            x = ev.getScreenX();
//            y = ev.getScreenY();
//        }

        int xdiff= x- _originalMouseX;
        int ydiff= y- _originalMouseY;
        int newX= _originalScrollX -xdiff;
        int newY= _originalScrollY -ydiff;

//        GwtUtil.getClientLogger().log(Level.INFO,
//                "  cnt= " + (cnt++) +
//                        ", x= " + x +
//                        ", y= " + y +
//                        ", _originalMouseX= " + _originalMouseX +
//                        ", _originalMouseY= " + _originalMouseY +
//                        ", xdiff= " + xdiff +
//                        ", ydiff= " + ydiff +
//                        ", newX= " + newX +
//                        ", newY= " + newY
//        );

        if (newX<0) newX= 0;
        if (newY<0) newY= 0;

        _pv.getMiniPlotWidget().getGroup().setDragging(!endDrag);
        _pv.setScrollXY(new ScreenPt(newX, newY), !endDrag);
        _lastX= x;
        _lastY= y;
//        GwtUtil.getClientLogger().log(Level.INFO, "PlotMover: newX="+ newX+"  newY="+newY);
    }


    private int convertToAbsoluteY(int y) {
        return _pv.getAbsoluteTop()+y;
    }

    private int convertToAbsoluteX(int x) {
        GwtUtil.getClientLogger().log(Level.INFO, "x= "+x+", AbLeft="+_pv.getAbsoluteLeft()+ ", return="+ (_pv.getAbsoluteLeft()+x));
        return _pv.getAbsoluteLeft()+x;
    }


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

    private class Mouse extends WebPlotView.DefMouseAll  {

        @Override
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
            _mouseDown= true;
            _mouseInfo.setEnableAllPersistent(true);
            begin(spt,ev);
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt, MouseMoveEvent ev) {
            if (_mouseDown) {
                dragTimer.cancel();
                dragTimer.setupCall(spt,ev.getScreenX(), ev.getScreenY());
                _lastX= ev.getScreenX();
                _lastY= ev.getScreenY();
                dragTimer.schedule(10);
//                drag(spt,false);
//                _mouseInfo.setEnableAllPersistent(true);
            }
        }

        @Override
        public void onMouseUp(WebPlotView pv, ScreenPt spt) {
            if (_mouseDown) {
                dragTimer.cancel();
                _mouseDown= false;
                drag(spt,_lastX,_lastY, true);
            }
            DeferredCommand.addCommand(_enabler);
        }
    }


    private class Enabler implements Command {
        public void execute() {
            _mouseInfo.setEnableAllPersistent(true);
        }
    }


    private class DragTimer extends Timer {
        private ScreenPt spt;
        private int x;
        private int y;
        @Override
        public void run() {
            drag(spt,x,y,false);
            _mouseInfo.setEnableAllPersistent(true);
        }

        private void setupCall(ScreenPt spt, int x, int y) {
            this.spt= spt;
            this.x= x;
            this.y= y;
        }
    }
}

