package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

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

    private void begin(ScreenPt spt) {
        _originalMouseX = convertToAbsoluteX(spt.getIX());
        _originalMouseY = convertToAbsoluteY(spt.getIY());


        ScreenPt pt= _pv.getScrollScreenPos();

        _originalScrollX = pt.getIX();
        _originalScrollY = pt.getIY();
    }

//    static int cnt= 0;

    private void drag(ScreenPt spt, boolean endDrag) {
        int x = convertToAbsoluteX(spt.getIX());
        int y = convertToAbsoluteY(spt.getIY());
        int xdiff= x- _originalMouseX;
        int ydiff= y- _originalMouseY;
        int newX= _originalScrollX -xdiff;
        int newY= _originalScrollY -ydiff;

//        GwtUtil.showDebugMsg(
//                "  cnt= "+ (cnt++)+
//                ", x= "+ x+
//                ", y= "+ y+
//                ", _originalMouseX= "+ _originalMouseX +
//                ", _originalMouseY= "+ _originalMouseY +
//                ", xdiff= "+ xdiff+
//                ", ydiff= "+ ydiff+
//                ", newX= "+ newX+
//                ", newY= "+ newY
//        );

        if (newX<0) newX= 0;
        if (newY<0) newY= 0;

        _pv.getMiniPlotWidget().getGroup().setDragging(!endDrag);
        _pv.setScrollXY(new ScreenPt(newX,newY), !endDrag);

    }


    private int convertToAbsoluteY(int y) {
        return _pv.getMouseMove().getAbsoluteTop()+y;
    }

    private int convertToAbsoluteX(int x) {
        return _pv.getMouseMove().getAbsoluteLeft()+x;
    }


// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

    private class Mouse extends WebPlotView.DefMouseAll  {

        @Override
        public void onMouseDown(WebPlotView pv, ScreenPt spt, MouseDownEvent ev) {
            _mouseDown= true;
            _mouseInfo.setEnableAllPersistent(true);
            begin(spt);
        }

        @Override
        public void onMouseMove(WebPlotView pv, ScreenPt spt) {
            if (_mouseDown) {
                drag(spt,false);
                _mouseInfo.setEnableAllPersistent(true);
            }
        }

        @Override
        public void onMouseUp(WebPlotView pv, ScreenPt spt) {
            if (_mouseDown) {
                _mouseDown= false;
                drag(spt,true);
            }
            DeferredCommand.addCommand(_enabler);
        }
    }


    private class Enabler implements Command {
        public void execute() {
            _mouseInfo.setEnableAllPersistent(true);
        }
    }

}

