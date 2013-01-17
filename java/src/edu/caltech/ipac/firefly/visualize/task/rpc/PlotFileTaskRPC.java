package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.task.PlotFileTask;
import edu.caltech.ipac.firefly.visualize.task.PlotFileTaskHelper;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:25:00 PM
 */


/**
 * @author Trey Roby
*/
public class PlotFileTaskRPC extends ServerTask<WebPlotResult> implements PlotFileTask {
    private final PlotFileTaskHelper _helper;

    private static int _keyCnt= 0;
    private static final String KEY_ROOT= "progress-";
    private ProgressTimer _timer= new ProgressTimer();
    private String _messageRoot;
    private String _progressKey= KEY_ROOT+_keyCnt;

    public static PlotFileTaskRPC plot(WebPlotRequest request1,
                                    WebPlotRequest request2,
                                    WebPlotRequest request3,
                                    boolean threeColor,
                                    String message,
                                    boolean removeOldPlot,
                                    boolean addToHistory,
                                    AsyncCallback<WebPlot> notify,
                                    MiniPlotWidget mpw) {

        PlotFileTaskRPC task= new PlotFileTaskRPC(request1, request2, request3,
                                                  threeColor, message,
                                                  removeOldPlot, addToHistory,
                                                  notify, mpw);
        task.start();
        return task;
    }

    PlotFileTaskRPC(WebPlotRequest request1,
                    WebPlotRequest request2,
                    WebPlotRequest request3,
                    boolean threeColor,
                    String message,
                    boolean removeOldPlot,
                    boolean addToHistory,
                    AsyncCallback<WebPlot> notify,
                    MiniPlotWidget mpw) {
        super(mpw.getLayoutPanel(), message, true);
        super.setMaskingDelaySec(1);
        _messageRoot= message;
        _keyCnt++;
        if (request1!=null) request1.setProgressKey(_progressKey);
        if (request2!=null) request2.setProgressKey(_progressKey);
        if (request3!=null) request3.setProgressKey(_progressKey);

        _helper= new PlotFileTaskHelper(request1,request2,request3,threeColor,removeOldPlot,addToHistory,notify,mpw,this);

    }

    public void onFailure(Throwable e) {
        _helper.handleFailure(e);
    }

    public WebPlotRequest getRequest() { return _helper.getRequest(); }
    public WebPlotRequest getRequest(Band band) { return _helper.getRequest(band); }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result!=null) _helper.handleSuccess(result);
    }

    @Override
    public void doTask(final AsyncCallback<WebPlotResult> passAlong) {
        _helper.getMiniPlotWidget().prePlotTask();
        PlotServiceAsync pserv= PlotService.App.getInstance();
        if (_helper.isThreeColor()) {
            pserv.getWebPlot(_helper.getRequest(Band.RED),
                             _helper.getRequest(Band.GREEN),
                             _helper.getRequest(Band.BLUE), passAlong);
        }
        else {
            pserv.getWebPlot(_helper.getRequest(), passAlong);
        }
        _timer.schedule(7000);
    }


    public boolean isThreeColor() { return _helper.isThreeColor(); }

    public void updateProgress(String status) {
        setMsg(_messageRoot + ": " + status);
    }


    /**
     * change the default cancel behavior so server can clean up
     */
    public void cancel() {
        if (!isFinish()) {
            super.cancel();
            _helper.cancel();
            unMask();
        }
    }

    private class ProgressTimer extends Timer {
        @Override
        public void run() {
            if (!isFinish()) {
                VisTask.getInstance().checkPlotProgress(_progressKey, new AsyncCallback<WebPlotResult>() {
                    public void onFailure(Throwable caught) { }

                    public void onSuccess(WebPlotResult result) {
                        if (result.isSuccess()) {
                            String progress = result.getStringResult(WebPlotResult.STRING);
                            updateProgress(progress);
                            schedule(2000);
                        }
                    }
                });
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
