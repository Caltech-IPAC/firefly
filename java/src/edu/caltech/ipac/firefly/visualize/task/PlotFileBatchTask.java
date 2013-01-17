package edu.caltech.ipac.firefly.visualize.task;

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
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:25:00 PM
 */


/**
 * Experimental !!!!!!!!!!!!!
 * @author Trey Roby
*/
public class PlotFileBatchTask extends ServerTask<WebPlotResult[]> implements PlotFileTask {
    private final PlotFileTaskHelper _helper[];
    private final WebPlotRequest _reqAry[];

    public static PlotFileBatchTask plot(WebPlotRequest reqAry[],
                                    String message,
                                    boolean removeOldPlot,
                                    boolean addToHistory,
                                    AsyncCallback<WebPlot> notify[],
                                    MiniPlotWidget mpw[]) {

        PlotFileBatchTask task= new PlotFileBatchTask(reqAry, message,
                                            removeOldPlot, addToHistory,
                                            notify, mpw);
        task.start();
        return task;
    }

    PlotFileBatchTask(WebPlotRequest reqAry[],
                      String message,
                      boolean removeOldPlot,
                      boolean addToHistory,
                      AsyncCallback<WebPlot> notify[],
                      MiniPlotWidget mpw[]) {
        super(mpw[0].getLayoutPanel(), message, true);
        super.setMaskingDelaySec(1);
        _helper= new PlotFileTaskHelper [reqAry.length];
        _reqAry= reqAry;
        for(int i=0; (i<_helper.length); i++) {
            _helper[i]= new PlotFileTaskHelper(reqAry[i],null,null,false,removeOldPlot,addToHistory,notify[i],mpw[i],this);
        }
    }


    public WebPlotRequest getRequest() { return null; }
    public WebPlotRequest getRequest(Band band) { return null; }
    public boolean isThreeColor() { return false; }

    public void onFailure(Throwable e) {
        for(PlotFileTaskHelper  h : _helper) h.handleFailure(e);
    }


    @Override
    public void onSuccess(WebPlotResult result[]) {
        if (result!=null) {
            for(int i=0; (i<_helper.length); i++) {
                _helper[i].handleSuccess(result[i]);
            }
        }
    }

    @Override
    public void doTask(final AsyncCallback<WebPlotResult[]> passAlong) {
        for(PlotFileTaskHelper  h : _helper) {
            h.getMiniPlotWidget().prePlotTask();
        }
        PlotServiceAsync pserv= PlotService.App.getInstance();
//        pserv.getWebPlotBatch(_reqAry,passAlong);
    }



    /**
     * change the default cancel behavior so server can clean up
     */
    public void cancel() {
        if (!isFinish()) {
            super.cancel();
            for(PlotFileTaskHelper  h : _helper) h.cancel();
            unMask();
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
