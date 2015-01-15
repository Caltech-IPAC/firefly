/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task;
/**
 * User: roby
 * Date: 2/3/12
 * Time: 2:55 PM
 */


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

/**
 * @author Trey Roby
 */

public class PlotFileTask extends ServerTask<WebPlotResult> {
    private final PlotFileTaskHelper _helper;

    private static int _keyCnt= 0;
    private static final String KEY_ROOT= "progress-";
    private ProgressTimer _timer= new ProgressTimer();
    private String _messageRoot;
    private final String _progressKey;

    public static PlotFileTask plot(WebPlotRequest request1,
                                    WebPlotRequest request2,
                                    WebPlotRequest request3,
                                    boolean threeColor,
                                    String message,
                                    boolean removeOldPlot,
                                    boolean addToHistory,
                                    AsyncCallback<WebPlot> notify,
                                    MiniPlotWidget mpw) {

        PlotFileTask task= new PlotFileTask(request1, request2, request3,
                                                  threeColor, message,
                                                  removeOldPlot, addToHistory,
                                                  notify, mpw);
        task.start();
        return task;
    }

    PlotFileTask(WebPlotRequest request1,
                    WebPlotRequest request2,
                    WebPlotRequest request3,
                    boolean threeColor,
                    String message,
                    boolean removeOldPlot,
                    boolean addToHistory,
                    AsyncCallback<WebPlot> notify,
                    MiniPlotWidget mpw) {
        super(mpw.getPanelToMask(), message, true);
        super.setMaskingDelaySec(1);
        _messageRoot= message;
        _keyCnt++;
        _progressKey= KEY_ROOT+"-"+_keyCnt +"-"+System.currentTimeMillis();
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



    private class ProgressTimer extends Timer {
        @Override
        public void run() {
            if (!isFinish()) {
                VisTask.getInstance().checkPlotProgress(_progressKey, new AsyncCallback<WebPlotResult>() {
                    public void onFailure(Throwable caught) { }

                    public void onSuccess(WebPlotResult result) {
                        if (result.isSuccess()) {
                            String progress = result.getStringResult(WebPlotResult.STRING);
                            setMsg(_messageRoot + ": " + progress);
                            schedule(2000);
                        }
                    }
                });
            }
        }
    }
}


