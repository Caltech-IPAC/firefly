package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;


/**
 * Date: Mar 26, 2008
 *
 * @author loi
 * @version $Id: RequestBuilderServerTask.java,v 1.1 2012/02/06 18:31:46 roby Exp $
 */
public abstract class RequestBuilderServerTask {

    private static final String MASK_MSG = "Loading...";

    protected enum State {START, WORKING, CANCELED, SUCCESS, FAIL, TIMEOUT, BACKGROUNDED}
    private State _state;
    private final Widget widget;
    private String msg;
    private RequestCallback _activeCallback= null;
    private final boolean _cancelable;
    private MaskPane maskPane;
    private boolean isAutoMask = true;
    private int maskingDelaySec= 0;
    private Timer _timer= null;
//    private static int taskRunningCnt= 0;
//    private long startTime;
//    private long midDelta= 0;
//    private long endDelta= 0;
//    private static List<RequestBuilderServerTask> runningList= new ArrayList<RequestBuilderServerTask>(30);

    public RequestBuilderServerTask() {
        this(null, MASK_MSG,false);
    }

    public RequestBuilderServerTask(Widget widget,
                                    String msg,
                                    boolean cancelable) {
        this.widget = widget;
        this.msg = msg == null ? "" : msg;
        _state= State.START;
        _cancelable= cancelable;
    }

    public void setMaskingDelaySec(int sec) {
        maskingDelaySec= sec;
    }

    public Widget getWidget() { return widget; }


    public void start() {
        start(new Callback());
    }

    void start(RequestCallback callback) {
//        taskRunningCnt++;
//        showDebugTask();
//        runningList.add(this);
        if (_state!= State.START) {
            throw new IllegalStateException("Server task must be in the START state to call start(). "+
                                            "Current state: "+ _state+
                                            " You can return to the start state using reset()");
        }
        _state= State.WORKING;
        if(isAutoMask) {
            mask();
        }
        _activeCallback= callback;
        try {
//            startTime= System.currentTimeMillis();
            doTask(callback);
        } catch (RequestException e) {
            doFailure(e);
        }
    }

    public String getMsg() {
        return msg;
    }


    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setAutoMask(boolean autoMask) {
        isAutoMask = autoMask;
    }

    public boolean isFinish() {
        return ( _state== State.SUCCESS || _state== State.CANCELED ||
                 _state== State.FAIL || _state== State.TIMEOUT );
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
        if (_state== State.WORKING) {
            throw new IllegalStateException("Cannot reset when in the WORKING state, "+
                                            "You must cancel or finish first");
        }
        _state= State.START;
    }

    public abstract void onSuccess(Response response);

    public abstract void doTask(RequestCallback passAlong) throws RequestException;

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

    protected  void doSuccess(Response response) {
//        taskRunningCnt--;
//        runningList.remove(this);
//        showDebugTask();
//        midDelta= System.currentTimeMillis()-startTime;
        if (_state == State.CANCELED) return;
        unMask();
        _state= State.SUCCESS;
        onSuccess(response);
//        endDelta= System.currentTimeMillis()-startTime;
    }

    public void mask() {
        if (widget!=null) {
            if (maskingDelaySec==0) {
                setMaskWidget();
            }
            else {
                _timer= new Timer() {
                    public void run() { setMaskWidget(); }
                };
                _timer.schedule(maskingDelaySec*1000);
            }
        }
    }


    private void setMaskWidget() {
        if (widget!=null) {
            ClickHandler cancelClick= null;
            if (_cancelable) {
                cancelClick= new ClickHandler () {
                    public void onClick(ClickEvent ev ) { cancel(true); }
                };

            }
            DefaultWorkingWidget working= new DefaultWorkingWidget(cancelClick);
            working.setText(msg);
            maskPane = new MaskPane(widget, working);
            maskPane.show();
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
//        for(RequestBuilderServerTask t : runningList) {
//          s+=t.toString() +", mid:"+ t.midDelta + ", end:"+t.endDelta+ ", " +"<br>";
//        }
//        GwtUtil.showDebugMsg(s,true);
//    }

//====================================================================
//
//====================================================================

    private class Callback  implements RequestCallback {

        public void onResponseReceived(Request request, Response response) {
            if (this==_activeCallback) doSuccess(response);
        }

        public void onError(Request request, Throwable throwable) {
            if (this==_activeCallback) doFailure(throwable);
        }
    }


}
