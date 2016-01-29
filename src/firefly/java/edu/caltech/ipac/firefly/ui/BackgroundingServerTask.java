/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.background.BackgroundActivation;
import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.CanCancel;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.ui.background.UIBackgroundUtil;
/**
 * User: roby
 * Date: Dec 18, 2009
 * Time: 2:44:59 PM
 */


/**
 * @author Trey Roby
 */
@Deprecated
public abstract class BackgroundingServerTask<R> extends ServerTask<R> {


    private static final String CLIENT_ID = "BackgroundingServerTask-";

    private final BackgroundActivation _bActivate;
    private final int _delaySeconds;
    private static int _cnt= 0;
    private MonitorItem _monItem;
    private R _result= null;
    private final String _taskID= CLIENT_ID + (_cnt++);
    private Timer _timer;

    private final String _activateText;
    private final String _activateTip;
    private final String _title;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public BackgroundingServerTask(Widget widget,
                                   String title,
                                   String workingMsg,
                                   String activateText,
                                   String activateTip,
                                   boolean cancelable,
                                   int delaySeconds) {
        super(widget,workingMsg,cancelable);
        _bActivate= new Activation();
        _title= title;
        _delaySeconds= delaySeconds;
        _activateText= activateText;
        _activateTip= activateTip;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    @Override
    public void start() {
        _timer= new Timer() {
            public void run() {
                _cnt++;
                BackgroundingServerTask.this.unMask();
                TaskCanceler canceler= new TaskCanceler(_monItem,_taskID,BackgroundingServerTask.this);
//                _monItem= new MonitorItem(_title, _bActivate);
                _monItem= new MonitorItem(_title, BackgroundUIHint.QUERY);
                _monItem.setCanceller(canceler);
                _monItem.setStatus(new BackgroundStatus(_taskID,BackgroundState.WAITING));
                setState(State.BACKGROUNDED);
                PopupUtil.showMinimalMsg(getMaskWidget(),"Backgrounding...", 2, PopupPane.Align.CENTER, 150);
            }
        };
        _timer.schedule(_delaySeconds*1000);
        super.start();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void doFailure(Throwable caught) {
        if (getState()!=State.BACKGROUNDED) {
            _timer.cancel();
            super.doFailure(caught);

        }
        else {
            super.doFailure(caught);
            _monItem.setStatus(new BackgroundStatus(_taskID, BackgroundState.FAIL));
        }
    }

    @Override
    protected void doSuccess(R result) {
        if (getState()!=State.BACKGROUNDED) {
            _timer.cancel();
            super.doSuccess(result);
        }
        else {
            BackgroundStatus bgStat= new BackgroundStatus(_taskID, BackgroundState.SUCCESS);
            _monItem.setStatus(bgStat);
            _result= result;
        }
    }

//======================================================================
//------------------ Inner Classes -----------------------
//======================================================================


    public static class TaskCanceler implements CanCancel {

        private final MonitorItem _monItem;
        private final String _taskID;
        private final ServerTask _task;

        public TaskCanceler(MonitorItem monItem, String taskID, ServerTask task) {
            _monItem= monItem;
            _taskID= taskID;
            _task= task;
        }

        public void cancelTask() {
            _task.cancel();
            _monItem.setStatus(new BackgroundStatus(_taskID,BackgroundState.CANCELED));
        }
    }

    public class Activation implements BackgroundActivation   {
        public Widget buildActivationUI(MonitorItem monItem, int idx, boolean markAlreadyActivated) {
            return UIBackgroundUtil.buildActivationUI(_activateText, _activateTip, monItem,
                                                      idx,this, markAlreadyActivated);
        }

        public void activate(MonitorItem monItem, int idx, boolean byAutoActivation) {
            setState(State.SUCCESS);
            BackgroundingServerTask.this.onSuccess(_result);
            monItem.setActivated(true);
        }

    }

}

