/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

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
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MaskPlotView;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
 * @author Trey Roby
 */

public class PlotMaskTask extends ServerTask<WebPlotResult> {

    private static int _keyCnt= 0;
    private static final String KEY_ROOT= "mask-progress-";
    private ProgressTimer _timer= new ProgressTimer();
    private final String _progressKey;
    private MaskPlotView mpv;
    private WebPlotRequest request;
    private AsyncCallback<WebPlot> notify;

    public static PlotMaskTask plot(WebPlotRequest request,
                                    MaskPlotView mpv,
                                    AsyncCallback<WebPlot> notify) {

        PlotMaskTask task= new PlotMaskTask(request, notify, mpv);
        task.start();
        return task;
    }

    PlotMaskTask(WebPlotRequest request,
                 AsyncCallback<WebPlot> notify,
                 MaskPlotView mpv) {
        super(mpv, "Plotting Mask", true);
        super.setMaskingDelaySec(1);
        _keyCnt++;
        _progressKey= KEY_ROOT+"-"+_keyCnt +"-"+System.currentTimeMillis();
        this.mpv= mpv;
        this.request= request;
        this.notify= notify;
        request.setProgressKey(_progressKey);
    }

    public void onFailure(Throwable e) {
        mpv.clear();
        String extra = "";
        if (e!=null && e.getCause() != null) {
            extra = e.getCause().toString();

        }
//        Window.alert("Plot Failed: Server Error: "+  extra);
        PopupUtil.showError("Server Error with Mask", "Mask Failed: Server Error: " + extra);
    }

    public WebPlotRequest getRequest() { return request; }

    @Override
    public void onSuccess(WebPlotResult result) {
        try {
            if (result.isSuccess()) {
                CreatorResults cr= (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                WebPlotInitializer wpInit= cr.getInitializers()[0];
                mpv.setMaskPlot(new WebPlot(wpInit,true));
                if (notify != null) notify.onSuccess(null);
            } else {
                showFailure(result);
            }
        } catch (Exception e) {
            PopupUtil.showError("Plot Failed", e.getMessage(), "WebPlot exception: " + e);
        }
    }


    private void showFailure(WebPlotResult result) {
        String uMsg = result.getUserFailReason();
        String dMsg = result.getDetailFailReason();
        String title = request.getTitle() == null ? "" : request.getTitle() + ": ";
        PopupUtil.showError( title + " Plot Failed- " + uMsg, dMsg);
        if (notify != null) notify.onFailure(null);
    }

    @Override
    public void doTask(final AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pServ= PlotService.App.getInstance();
        pServ.getWebPlot(request, passAlong);
        _timer.schedule(7000);
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
                            setMsg("Mask: " + progress);
                            schedule(2000);
                        }
                    }
                });
            }
        }
    }
}


