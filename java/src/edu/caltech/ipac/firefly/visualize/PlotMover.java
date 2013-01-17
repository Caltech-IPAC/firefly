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
    private WebMouseReadout _readout= null;
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

    public void disableMouseReadoutOnMove(WebMouseReadout readout) {
        _readout= readout;
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void begin(ScreenPt spt) {
//        if (_readout!=null) _readout.setEnabled(false);
        _originalMouseX = convertToAbsoluteX(spt.getIX());
        _originalMouseY = convertToAbsoluteY(spt.getIY());


        ScreenPt pt= _pv.getScrollScreenPos();

        _originalScrollX = pt.getIX();
        _originalScrollY = pt.getIY();
    }

//    static int cnt= 0;

    private void drag(ScreenPt spt, boolean endDrag) {
//        if (_readout!=null) _readout.setEnabled(false);
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

        if (endDrag) {
            if (_readout!=null) _readout.setEnabled(true);
        }
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
