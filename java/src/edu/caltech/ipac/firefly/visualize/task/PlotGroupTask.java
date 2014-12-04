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

public class PlotGroupTask extends ServerTask<WebPlotResult[]> {

    private static int _keyCnt= 0;
    private static final String KEY_ROOT= "multi-plot-progress-";
    private static final String GROUP_KEY_ROOT= "multi-plot-progress-";
    private ProgressTimer _timer= new ProgressTimer();
    private String _messageRoot;
    private String progressKeyRoot= KEY_ROOT+_keyCnt;
    private final List<WebPlotRequest> requestList;
    private Map<String,TaskInfo> taskMap= new HashMap<String, TaskInfo>(29);
    private final String _groupProgressKey= GROUP_KEY_ROOT+_keyCnt;

    public static PlotGroupTask plot(Element maskElement,
                                     List<WebPlotRequest> requestList,
                                     List<MiniPlotWidget> mpwList,
                                     AsyncCallback<WebPlot> notify) {

        PlotGroupTask task= new PlotGroupTask(maskElement, requestList,mpwList, notify);
        task.start();
        return task;
    }

    PlotGroupTask(Element maskElement,
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
            taskMap.put(key,new TaskInfo(help,mpw,r));
        }
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
        pserv.getWebPlotGroup(requestList, _groupProgressKey, passAlong);

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
