package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;
import edu.caltech.ipac.firefly.core.GeneralCommand;
/**
 * User: roby
 * Date: May 28, 2008
 * Time: 4:10:29 PM
 */


/**                                                                                       
 * @author Trey Roby
 */
public class FireFlyMenuBar extends MenuBar {

    private static TipTimer _tipTimer= new TipTimer();
    private int _mX= 0;
    private int _mY= 0;
    static int tipCnt= 0;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public FireFlyMenuBar() {
        this(false);
    }

    public FireFlyMenuBar(boolean vertical) {
        super(vertical);
        sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT
                   | Event.ONFOCUS |  Event.ONKEYDOWN |
                     Event.ONMOUSEMOVE);
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================




    static int cnum= 0;

    public void onBrowserEvent(final Event ev) {
        if (DOM.eventGetType(ev) != Event.ONMOUSEMOVE) {
            super.onBrowserEvent(ev);
        }

        DeferredCommand.addCommand(new Command() {
            public void execute() {
                dealWithEvent(ev);
            }
        });
    }


    public void dealWithEvent(Event ev) {

        int evType = DOM.eventGetType(ev);
//        _tipTimer._debugText.setText("event="+DOM.eventGetTypeString(ev) + " from: "+
//                                     DOM.eventGetFromElement(ev) +
//                                     " Selected Item= " +getSelectedItem() +
//                                     "   "+cnum++);
        _mX= DOM.eventGetClientX(ev);
        _mY= DOM.eventGetClientY(ev);
        switch (evType) {

            case Event.ONMOUSEOVER:
            case Event.ONMOUSEMOVE:
                if (getSelectedItem()!=null) {
                    String tip= getTip(getSelectedItem().getCommand());
                    if (getSelectedItem()!=null & tip!=null) getSelectedItem().setTitle(tip);
                    if (tip!=null) {
//                        _tipTimer.setup("tip: "+tip + ", mx= " + _mX+ ", mY= " + _mY, _mX, _mY);
                        _tipTimer.setup(tip, _mX, _mY);
                        _tipTimer.schedule(3000);
                    }
                    else {
                        _tipTimer.deferredHide();
                    }
                }
                else {
                }
                break;

            case Event.ONCLICK:
            case Event.ONMOUSEOUT:
            default:
                _tipTimer.deferredHide();
                break;
        }
    }

    private String getTip(Command command) {
        String tip= null;
        if (command instanceof GeneralCommand) {
            GeneralCommand fireflyCommand =
                               (GeneralCommand)command;
            tip= fireflyCommand.getShortDesc();
        }
        return tip;
    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

private static class TipTimer extends Timer {
    String _tip;
    private PopupPanel _ppToolTip= new PopupPanel(false,false);
    private Label _label= new Label();
    private int _x= 0;
    private int _y= 0;
    private boolean _showOnSchedule;


//    public PopupPanel _debugOut= new PopupPanel(false,false);
//    public Label _debugText= new Label();

    public TipTimer() {
        _ppToolTip.setWidget(_label);
        _ppToolTip.addStyleName("menuBarTipZIndex");
        _ppToolTip.setAnimationEnabled(false);


//        _debugOut.setWidget(_debugText);
//        _debugOut.setPopupPosition(510,10);
//        _debugOut.show();
    }

    public void run() {
        if (_showOnSchedule) {
            _label.setText(_tip);
            _ppToolTip.setPopupPosition(_x+5,_y+5);
//        _ppToolTip.setPopupPosition(100,100);
            _ppToolTip.show();
            _tipTimer.schedule(8000);
            _showOnSchedule= false;
        }
        else {
            _ppToolTip.hide();
        }
    }

    public void setup(String tip, int x, int y) {
        _tip= tip;
        _x= x;
        _y= y;
        _showOnSchedule= true;
        if (_ppToolTip.isVisible()) {
            _label.setText(_tip);
            _ppToolTip.setPopupPosition(_x+5,_y+5);
        }
    }

    public boolean isVisible() {
        return _ppToolTip.isVisible();
    }

    public void deferredHide() {
        cancel();
        _tipTimer.schedule(100);
        _showOnSchedule= false;
    }

    public void reset() {
        cancel();
        _ppToolTip.hide();
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
