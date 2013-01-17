package edu.caltech.ipac.firefly.visualize.task.rpc;
/**
 * User: roby
 * Date: 10/4/11
 * Time: 5:00 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
* @author Trey Roby
*/
public class StretchTaskRPC extends ServerTask<WebPlotResult> {

    private WebPlot plot;
    private StretchData[] stretchData;

    public static void stretch(WebPlot plot, StretchData[] stretchData) {
        new StretchTaskRPC(plot,stretchData).start();
    }

    public StretchTaskRPC(WebPlot plot, StretchData[] stretchData) {
        super(plot.getPlotView().getMaskWidget(), "stretching...", true);
        super.setMaskingDelaySec(1);
        this.stretchData= stretchData;
        this.plot= plot;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            plot.setPlotState((PlotState)result.getResult(WebPlotResult.PLOT_STATE));
            plot.refreshWidget((PlotImages)result.getResult(WebPlotResult.PLOT_IMAGES));
            plot.getPlotView().getMiniPlotWidget().forcePlotPrefUpdate();
        }
        else {
            PopupUtil.showError("Server Error", "Changing Stretch Failed: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.recomputeStretch(plot.getPlotState(), stretchData, passAlong);
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
