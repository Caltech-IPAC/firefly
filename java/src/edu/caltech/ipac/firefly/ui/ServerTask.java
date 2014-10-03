package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;


/**
 * Date: Mar 26, 2008
 *
 * @author loi
 * @version $Id: ServerTask.java,v 1.28 2012/12/10 19:02:11 roby Exp $
 */
public abstract class ServerTask<R> {

    private static final String MASK_MSG = "Loading...";

    protected enum State {START, WORKING, CANCELED, SUCCESS, FAIL, TIMEOUT, BACKGROUNDED}
    private State _state;
    private final Widget widget;
    private String msg;
    private AsyncCallback<R> _activeCallback= null;
    private final boolean _cancelable;
    private final boolean _checkForDropdown;
    private MaskPane maskPane;
    private boolean isAutoMask = true;
    private int maskingDelaySec= 0;
    private Timer _timer= null;
    DefaultWorkingWidget working= null;
//    private static int taskRunningCnt= 0;
//    private static List<ServerTask> runningList= new ArrayList<ServerTask>(30);
//    private long startTime;
//    private long midDelta= 0;
//    private long endDelta= 0;

    public ServerTask() {
        this(null, MASK_MSG,false);
    }

    public ServerTask(Widget widget,
                      String msg,
                      boolean cancelable) {
        this(widget, msg, cancelable,true);
    }

    public ServerTask(Widget widget,
                      String msg,
                      boolean cancelable,
                      boolean checkForDropdown) {
        this.widget = widget;
        this.msg = msg == null ? "" : msg;
        _state= State.START;
        _cancelable= cancelable;
        _checkForDropdown= checkForDropdown;
    }

    public void setMaskingDelaySec(int sec) {
        maskingDelaySec= sec;
    }

    public Widget getWidget() { return widget; }


    public void start() {
        start(new Callback());
    }

    void start(AsyncCallback<R> callback) {
//        taskRunningCnt++;
//        showDebugTask();
//        runningList.add(this);
        if (_state!=State.START) {
            throw new IllegalStateException("Server task must be in the START state to call start(). "+
                                            "Current state: "+ _state+
                                            " You can return to the start state using reset()");
        }
        _state= State.WORKING;
        if(isAutoMask) {
            mask();
        }
        _activeCallback= callback;
        doTask(callback);
    }

    public String getMsg() {
        return msg;
    }


    public void setMsg(String msg) {
        this.msg = msg;
        if (working!=null) working.setText(msg);
    }

    public void setAutoMask(boolean autoMask) {
        isAutoMask = autoMask;
    }

    public boolean isFinish() {
        return ( _state==State.SUCCESS || _state==State.CANCELED ||
                 _state==State.FAIL || _state==State.TIMEOUT );
    }

    void forceStateChange(State state) {
        _state= state;
    }

    public void cancel() {
       cancel(false);
    }

    public void cancel(boolean userCancelled) {
        if (!isFinish()) {
            _state= State.CANCELED;
            _activeCallback= null;
            unMask();
            onCancel(userCancelled);
        }
    }

    public void reset() {
        if (_state==State.WORKING) {
            throw new IllegalStateException("Cannot reset when in the WORKING state, "+
                                            "You must cancel or finish first");
        }
        _state= State.START;
    }

    public abstract void onSuccess(R result);

    public abstract void doTask(AsyncCallback<R> passAlong);

//====================================================================
//
//====================================================================

    protected void setState(State state) { _state= state; }

    protected State getState() {  return _state; }


    protected void onCancel(boolean byUser) {
    }

    protected void onFailure(Throwable caught) {
        PopupUtil.showSevereError(caught);
    }

    protected  void doFailure(Throwable caught) {
//        taskRunningCnt--;
//        runningList.remove(this);
//        showDebugTask();
        if (_state == State.CANCELED) return;
        if (isAutoMask) unMask();
        _state= State.FAIL;
        onFailure(caught);
    }

    protected  void doSuccess(R result) {
//        taskRunningCnt--;
//        runningList.remove(this);
//        midDelta= System.currentTimeMillis()-startTime;
//        showDebugTask();
        if (_state == State.CANCELED) return;
        unMask();
        _state= State.SUCCESS;
        onSuccess(result);
//        endDelta= System.currentTimeMillis()-startTime;
    }

    public void mask() {
        if (widget!=null) {
            if (maskingDelaySec==0) {
                displayMaskWidget();
            }
            else {
                _timer= new Timer() {
                    public void run() { displayMaskWidget(); }
                };
                _timer.schedule(maskingDelaySec*1000);
            }
        }
    }


    private void displayMaskWidget() {
        if (widget!=null) {
            ClickHandler cancelClick= null;
            if (_cancelable) {
                cancelClick= new ClickHandler () {
                    public void onClick(ClickEvent ev ) { cancel(true); }
                };

            }
            working= new DefaultWorkingWidget(cancelClick);
            working.setText(msg);
            maskPane = new MaskPane(widget, working);
//            final Application app= Application.getInstance();
            if (_checkForDropdown) {
                maskPane.showWhenUncovered();
//                Timer t= new Timer() {
//                    @Override
//                    public void run() {
//                        if (maskPane!=null) {
//                            boolean ddOpen= false;
//                            if (app.getToolBar()!=null)  ddOpen= app.getToolBar().getDropdown().isOpen();
//                            if (!ddOpen)  maskPane.show();
//                            if (!maskPane.isShowing()) schedule(1000);
//                        }
//                    }
//                };
//                t.schedule(100);
            }
            else {
                maskPane.show();
            }
        }
    }


    public void unMask() {
        if (widget!=null) {
            if (_timer!=null) {
                _timer.cancel();
                _timer= null;
            }
            unMaskWidget();
        }
    }


    private void unMaskWidget() {
        if (widget != null && maskPane != null) {
            maskPane.hide();
            maskPane = null;
        }
    }

//    private static void showDebugTask() {
//        String s=  "task running: " + taskRunningCnt +"<br>";
//        for(ServerTask t : runningList) {
//            s+=t.toString() +", mid:"+ t.midDelta + ", end:"+t.endDelta+ ", " +"<br>";
//        }
//        GwtUtil.showDebugMsg(s,true);
//    }

//====================================================================
//
//====================================================================
    private class Callback  implements AsyncCallback<R> {

        public void onFailure(Throwable throwable) {
            if (this==_activeCallback) doFailure(throwable);
        }

        public void onSuccess(R result) {
            if (this==_activeCallback) doSuccess(result);
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
