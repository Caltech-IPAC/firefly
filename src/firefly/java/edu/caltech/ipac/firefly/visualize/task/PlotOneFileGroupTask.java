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


import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Trey Roby
 */

public class PlotOneFileGroupTask extends ServerTask<WebPlotResult[]> {

    private static int _keyCnt= 0;
    private static final String KEY_ROOT= "multi-plot-progress-";
    private static final String GROUP_KEY_ROOT= "multi-plot-progress-";
    private ProgressTimer _timer= new ProgressTimer();
    private String _messageRoot;
    private String progressKeyRoot= KEY_ROOT+_keyCnt;
    private final List<WebPlotRequest> requestList;
    private Map<String,TaskInfo> taskMap= new HashMap<String, TaskInfo>(29);
    private final String _groupProgressKey= GROUP_KEY_ROOT+_keyCnt;

    public static PlotOneFileGroupTask plot(Element maskElement,
                                            List<WebPlotRequest> requestList,
                                            List<MiniPlotWidget> mpwList,
                                            AsyncCallback<WebPlot> notify) {

        PlotOneFileGroupTask task= new PlotOneFileGroupTask(maskElement, requestList,mpwList, notify);
        task.start();
        return task;
    }

    PlotOneFileGroupTask(Element maskElement,
                         List<WebPlotRequest> requestList,
                         List<MiniPlotWidget> mpwList,
                         AsyncCallback<WebPlot> notify) {
        super(maskElement, "working", true);
        super.setMaskingDelaySec(0);
        _messageRoot= "working";
        _keyCnt++;
        this.requestList= requestList;


        for(int i=0; (i<requestList.size()); i++) {
            String key= progressKeyRoot+i;
            MiniPlotWidget mpw= mpwList.get(i);
            WebPlotRequest r= requestList.get(i);
            r.setProgressKey(key);
            PlotFileTaskHelper help= new PlotFileTaskHelper(r, null,null,  false, true, false, notify,mpw,this);
            taskMap.put(key,new TaskInfo(help,mpw,r)); }
    }

    public void onFailure(Throwable e) {
        PopupUtil.showError("Plot fail", "group plot failed");
        GwtUtil.getClientLogger().log(Level.WARNING, "plot group failed", e);
//        _helper.handleFailure(e);
    }

    @Override
    public void onSuccess(WebPlotResult resultAry[]) {

        for(WebPlotResult result :resultAry) {
            if (result.isSuccess()) {
                CreatorResults cr= (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                WebPlotInitializer wpInit=cr.getInitializers()[0];
                String key= wpInit.getPlotState().getWebPlotRequest(Band.NO_BAND).getProgressKey();
                TaskInfo taskInfo= taskMap.get(key);
                taskInfo.helper.handleSuccess(result);
            }
            else {
                TaskInfo taskInfo= taskMap.get(result.getProgressKey());
                if (taskInfo!=null) {
                    taskInfo.helper.handleSuccess(result);
                }
            }
        }

    }

    @Override
    public void doTask(final AsyncCallback<WebPlotResult[]> passAlong) {
        for(TaskInfo taskInfo : taskMap.values()) {
            taskInfo.helper.getMiniPlotWidget().prePlotTask();
        }
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.getOneFileGroup(requestList, _groupProgressKey, passAlong);

        _timer.schedule(3000);
    }

    private class ProgressTimer extends Timer {
        @Override
        public void run() {
            if (!isFinish()) {
                VisTask.getInstance().checkPlotProgress(progressKeyRoot, new AsyncCallback<WebPlotResult>() {
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


    private class TaskInfo {
        PlotFileTaskHelper helper;
        MiniPlotWidget     mpw;
        WebPlotRequest     request;

        private TaskInfo(PlotFileTaskHelper helper, MiniPlotWidget mpw, WebPlotRequest request) {
            this.helper = helper;
            this.mpw = mpw;
            this.request = request;
        }
    }
}


